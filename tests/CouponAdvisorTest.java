import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.*;
import payment.*;
import repository.BillRepository;
import service.*;
import ui.BillComparisonPrinter;
import ui.CouponMenu;

/** Verifies recommendation accuracy and read-only preview behavior. */
public class CouponAdvisorTest {
    private static int checks;

    /** Runs quote, checkout consistency and preview isolation checks. */
    public static void main(String[] args) throws Exception {
        Product milk = new Product("P1", "Milk", Category.DAIRY, new BigDecimal("500"), 10, 2);
        Cart cart = new Cart(); cart.addItem(milk, 2);
        CouponService coupons = CouponMenu.demoCatalogue();
        CouponAdvisor advisor = new CouponAdvisor();
        Path folder = Files.createTempDirectory("quote-test-");
        BillRepository repository = new BillRepository(folder.resolve("bills.csv").toString());
        repository.loadAll();
        String saved = Files.readString(repository.getFile());
        List<BillQuote> quotes = advisor.compare(cart, coupons);
        check(quotes.size() == 3, "All active options and baseline");
        check(quotes.get(0).getOption().equals("SAVE10"), "Cheapest coupon first");
        money("945.00", quotes.get(0).getTotal());
        money("997.50", quotes.get(1).getTotal());
        money("1050.00", quotes.get(2).getTotal());
        money("100.00", quotes.get(0).getDiscount());
        money("45.00", quotes.get(0).getTax());
        check(milk.getQuantity() == 10 && cart.getItems().get(0).getQuantity() == 2, "No stock/cart changes");
        check(saved.equals(Files.readString(repository.getFile())), "No invoice write");
        check(BillComparisonPrinter.format(quotes).contains("Lowest total: SAVE10 - Rs 945.00"), "Readable recommendation");
        try { quotes.clear(); throw new AssertionError("Mutable result"); }
        catch (UnsupportedOperationException expected) { checks++; }
        Bill bill = new BillingService(() -> List.of(milk), 0, repository).checkout(cart, "cashier", "C1",
                coupons.findActive("SAVE10"), new CashPayment(new BigDecimal("1000")));
        check(bill.getTotal().equals(quotes.get(0).getTotal()) && bill.getTax().equals(quotes.get(0).getTax()), "Quote matches checkout");
        check(bill.getInvoiceNo().equals("SM-0001"), "Preview does not consume invoice number");
        cart.addItem(milk, 1);
        coupons.deactivate("SAVE10");
        quotes = advisor.compare(cart, coupons);
        check(quotes.size() == 2 && quotes.get(0).getOption().equals("LESS50"), "Inactive coupons excluded");
        CouponService empty = new CouponService();
        check(advisor.compare(cart, empty).get(0).getOption().equals("No coupon"), "Empty catalogue baseline");
        empty.addCoupon(new Coupon("ZERO", true, new FixedDiscount(BigDecimal.ZERO)));
        check(advisor.compare(cart, empty).get(0).getOption().equals("No coupon"), "Equal prices favor no coupon");
        empty.addCoupon(new Coupon("FREE", true, new FixedDiscount(new BigDecimal("9999"))));
        money("0.00", advisor.compare(cart, empty).get(0).getTotal());
        Product rice = new Product("R1", "Rice", Category.GROCERY, new BigDecimal("100"), 5, 1);
        cart.addItem(rice, 1);
        PricingService pricing = new PricingService();
        BillQuote mixed = pricing.quote(cart.getItems(), new PercentageDiscount(new BigDecimal("10")), "Mixed");
        money("562.50", mixed.getTotal());
        rejects(() -> advisor.compare(new Cart(), coupons));
        rejects(() -> advisor.compare(null, coupons));
        rejects(() -> advisor.compare(cart, null));
        rejects(() -> pricing.quote(cart.getItems(), amount -> amount.add(BigDecimal.ONE), "Bad"));
        rejects(() -> pricing.quote(cart.getItems(), amount -> null, "Bad"));
        rejects(() -> pricing.quote(cart.getItems(), amount -> new BigDecimal("-1"), "Bad"));
        milk.setPrice(new BigDecimal("600"));
        money("562.50", mixed.getTotal());
        money("657.00", pricing.quote(cart.getItems(), new PercentageDiscount(new BigDecimal("10")), "Updated").getTotal());
        System.out.println("PASS: " + checks + " coupon comparison and pricing checks.");
    }
    private static void money(String expected, BigDecimal actual) { check(new BigDecimal(expected).equals(actual), expected + " != " + actual); }
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } checks++; }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected invalid preview rejection");
    }
}
