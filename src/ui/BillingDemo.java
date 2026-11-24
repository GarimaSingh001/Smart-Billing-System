package ui;

import exception.InvalidInputException;
import exception.InsufficientStockException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import model.Bill;
import model.Cart;
import model.Category;
import model.Product;
import payment.CashPayment;
import payment.Discount;
import repository.BillRepository;
import repository.ProductRepository;
import service.BillingService;

/** Standalone Member C rehearsal using real models and repositories, without login. */
public final class BillingDemo {
    private BillingDemo() { }

    /** Starts the demo; an optional argument chooses its separate data directory. */
    public static void main(String[] args) {
        Path directory = Path.of(args.length == 0 ? "demo-data" : args[0]);
        try (Scanner scanner = new Scanner(System.in)) {
            run(new ConsoleInput(scanner, null), directory);
        } catch (ConsoleInput.InputClosedException e) { System.out.println("Demo closed."); }
        catch (IOException e) { System.out.println("[ERROR] Demo storage unavailable: " + e.getMessage()); }
    }

    /** Runs cash sales with sample products and coupons, saving invoices and updated stock. */
    public static void run(ConsoleInput input, Path directory) throws IOException {
        System.out.println("SUPERMART BILLING REHEARSAL - sample products and coupons, no login");
        System.out.println("Demo files: " + directory.toAbsolutePath());
        ProductRepository productRepo = new ProductRepository(directory.resolve("products.csv").toString());
        BillRepository billRepo = new BillRepository(directory.resolve("bills.csv").toString());
        if (Files.notExists(productRepo.getFile())) {
            productRepo.saveAll(List.of(new Product("P1", "Milk pack", Category.DAIRY,
                    new BigDecimal("500"), 10, 2), new Product("P2", "Rice pack", Category.GROCERY,
                    new BigDecimal("100"), 20, 3)));
        }
        List<Product> products = new ArrayList<>(productRepo.loadAll());
        int lastInvoice = billRepo.getLastInvoiceNumber();
        if (!productRepo.getWarnings().isEmpty() || !billRepo.getWarnings().isEmpty()) {
            throw new IOException("Malformed demo data; repair the files before another sale.");
        }
        BillingService billing = new BillingService(() -> products, lastInvoice, billRepo);
        while (true) {
            for (Product p : products) {
                System.out.println(p.getProductId() + " | " + p.getName() + " | Rs " + p.getPrice()
                        + " | stock " + p.getQuantity());
            }
            if (input.readInt("1 New sale, 2 Exit: ", 1, 2) == 2) { return; }
            Cart cart = new Cart();
            while (true) {
                String id = input.readLine("Product ID (blank = finish cart): ");
                if (id.isEmpty()) { break; }
                Product product = products.stream().filter(p -> p.getProductId().equalsIgnoreCase(id)).findFirst().orElse(null);
                if (product == null) { System.out.println("[ERROR] Unknown product."); continue; }
                try {
                    cart.addItem(product, input.readPositiveInt("Quantity: "));
                    System.out.println("Subtotal: Rs " + cart.getSubtotal());
                } catch (InvalidInputException e) { System.out.println("[ERROR] " + e.getMessage()); }
            }
            if (cart.isEmpty()) { System.out.println("Empty cart cancelled."); continue; }
            System.out.println("Demo coupons: SAVE10 (10%), LESS50 (Rs 50).");
            Discount discount = CouponMenu.choose(input, CouponMenu.demoCatalogue());
            CashPayment cash = new CashPayment(input.readMoney("Cash tendered: "));
            Bill bill;
            try { bill = billing.checkout(cart, "demo-cashier", "WALK-IN", discount, cash); }
            catch (InvalidInputException | InsufficientStockException e) {
                System.out.println("[ERROR] " + e.getMessage() + " Sale cancelled.");
                continue;
            }
            InvoicePrinter.print(bill);
            System.out.println("Change: Rs " + cash.getChange());
            try { productRepo.saveAll(products); }
            catch (IOException e) {
                throw new IOException("Invoice " + bill.getInvoiceNo()
                        + " is saved but stock could not be saved. Reconcile stock before retrying.", e);
            }
            System.out.println("Invoice and updated stock saved.");
        }
    }
}
