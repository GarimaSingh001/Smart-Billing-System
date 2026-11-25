import exception.InvalidInputException;
import exception.InsufficientStockException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.*;
import payment.*;
import repository.BillRepository;
import service.BillingService;


/** Tests cart, checkout, invoice snapshots and CSV reload with real team models. */
public class BillingTest {
    private static int checks;
    /** Runs billing integration checks in a temporary data directory. */
    public static void main(String[] args) throws Exception {
        Product milk = new Product("P1", "Milk", Category.DAIRY, new BigDecimal("500"), 10, 2);
        List<Product> products = new ArrayList<>(List.of(milk));
        BillingService service = new BillingService(products, 7);
        Cart cart = new Cart();
        cart.addItem(milk, 1); cart.addItem(milk, 1);
        check(cart.getItems().size() == 1 && cart.getItems().get(0).getQuantity() == 2, "Duplicate merge");
        expect("1000.00", cart.getSubtotal());
        cart.getItems().get(0).setQuantity(99);
        expect("1000.00", cart.getSubtotal());
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                cart.changeQuantity("P1", 0);
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                cart.removeItem("missing");
            }
        });
        expect("1000.00", cart.getSubtotal());
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.checkout(new Cart(), "cashier", "WALK-IN", none(), new CashPayment(BigDecimal.TEN));
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.checkout(cart, "cashier", "WALK-IN", none(), new CashPayment(BigDecimal.ONE));
            }
        });
        check(milk.getQuantity() == 10 && !cart.isEmpty(), "Failed cash preserves stock and cart");
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.checkout(cart, "cashier", "WALK-IN", none(), new UpiPayment("bad"));
            }
        });
        cart.changeQuantity("P1", 11);
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.checkout(cart, "cashier", "WALK-IN", none(), new UpiPayment("a@b"));
            }
        });
        check(milk.getQuantity() == 10, "Oversell leaves stock unchanged");
        cart.changeQuantity("P1", 2);
        Bill bill = service.checkout(cart, "Cashier, One\nDesk", "WALK-IN",
                new PercentageDiscount(new BigDecimal("10")), new CashPayment(new BigDecimal("1000")));
        expect("945.00", bill.getTotal()); expect("45.00", bill.getTax());
        check(bill.getInvoiceNo().equals("SM-0008"), "Failures do not consume invoice numbers");
        check(milk.getQuantity() == 8 && cart.isEmpty(), "Successful stock deduction and cart clear");
        milk.setPrice(new BigDecimal("600"));
        bill.getItems().get(0).getProduct().setPrice(BigDecimal.ONE);
        bill.getItems().get(0).setQuantity(1);
        expect("1000.00", bill.getItems().get(0).getLineAmount());
        String row = bill.toCsv();
        check(!row.contains("\n") && row.equals(Bill.fromCsv(row).toCsv()), "CSV round trip with special text");
        Path directory = Files.createTempDirectory("billing-test-");
        BillRepository repo = new BillRepository(directory.resolve("bills.csv").toString());
        repo.appendBill(bill);
        check(repo.getLastInvoiceNumber() == 8, "Persisted invoice counter");
        expect("945.00", repo.loadAll().get(0).getTotal());
        Files.writeString(repo.getFile(), "bad row\n", java.nio.file.StandardOpenOption.APPEND);
        check(repo.loadAll().size() == 1 && repo.getWarnings().size() == 1, "Malformed invoice skipped");
        BillingService restarted = new BillingService(products, repo.getLastInvoiceNumber());
        cart.addItem(milk, 1);
        Bill next = restarted.checkout(cart, "cashier", "C1", none(), new UpiPayment("a@b"));
        check(next.getInvoiceNo().equals("SM-0009"), "Invoice numbers continue after reload");
        Product soap = new Product("P2", "Soap", Category.PERSONAL_CARE, new BigDecimal("100"), 1, 0);
        products.add(soap);
        expect("43.20", BillingService.calculateTax(List.of(new CartItem(milk, 1), new CartItem(soap, 1)), new BigDecimal("0.1")));
        cart.addItem(milk, 1); cart.addItem(soap, 2);
        int old = milk.getQuantity();
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.checkout(cart, "cashier", "C1", none(), new UpiPayment("a@b"));
            }
        });
        check(milk.getQuantity() == old && soap.getQuantity() == 1, "All lines checked before deduction");
        cart.changeQuantity("P2", 1); cart.removeItem("P1");
        Bill free = service.checkout(cart, "cashier", "C1", new FixedDiscount(new BigDecimal("500")), new CashPayment(BigDecimal.ZERO));
        expect("0.00", free.getTotal()); expect("0.00", free.getTax());
        check(soap.getQuantity() == 0, "Freshly added product available to checkout");
        Cart overflow = new Cart(); overflow.addItem(milk, Integer.MAX_VALUE);
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                overflow.addItem(milk, 1);
            }
        });
        check(overflow.getItems().get(0).getQuantity() == Integer.MAX_VALUE, "Quantity overflow rejected");
        Path blockedFile = directory.resolve("blocked");
        Files.writeString(blockedFile, "not a directory");
        BillingService failingSave = new BillingService(products, 20,
                new BillRepository(blockedFile.resolve("bills.csv").toString()));
        Cart retry = new Cart(); retry.addItem(milk, 1);
        int beforeSave = milk.getQuantity();
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                failingSave.checkout(retry, "cashier", "C1", none(), new UpiPayment("a@b"));
            }
        });
        check(milk.getQuantity() == beforeSave && !retry.isEmpty(), "Save failure restores stock and keeps cart");
        BillRepository savedRepo = new BillRepository(directory.resolve("checkout.csv").toString());
        BillingService persistent = new BillingService(products, 0, savedRepo);
        Bill persisted = persistent.checkout(retry, "cashier", "C1", none(), new UpiPayment("a@b"));
        check(savedRepo.loadAll().get(0).toCsv().equals(persisted.toCsv()), "Checkout saves invoice before returning");
        util.Inventory inventory = new util.Inventory();
        BillingService sharedInventoryBilling = new BillingService(inventory, 0);
        Product addedLater = new Product("P3", "New tea", Category.BEVERAGE, new BigDecimal("100"), 5, 1);
        inventory.addProduct(addedLater);
        Cart sharedCart = new Cart();
        sharedCart.addItem(addedLater, 2);
        Bill sharedBill = sharedInventoryBilling.checkout(sharedCart, "cashier", "C1", none(), new UpiPayment("a@b"));
        expect("224.00", sharedBill.getTotal());
        check(inventory.findById("P3").getQuantity() == 3, "Live inventory includes products added after service creation");
        System.out.println("PASS: " + checks + " cart, billing, snapshot and persistence checks.");
    }
    private static Discount none() { return new FixedDiscount(BigDecimal.ZERO); }
    private static void expect(String amount, BigDecimal actual) { check(new BigDecimal(amount).equals(actual), amount + " != " + actual); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } checks++; }
    private interface Attempt { void run() throws Exception; }
    private static void rejects(Attempt attempt) throws Exception {
        try { attempt.run(); }
        catch (InvalidInputException | InsufficientStockException expected) { checks++; return; }
        throw new AssertionError("Expected rejection");
    }
}
