import exception.InvalidInputException;
import java.math.BigDecimal;
import java.util.List;
import model.Bill;
import model.Cart;
import model.Category;
import model.Coupon;
import model.Product;
import payment.CashPayment;
import payment.FixedDiscount;
import payment.PercentageDiscount;
import service.BillingService;
import service.CouponService;

/** Checks coupon validation and its use in the real checkout calculation. */
public class CouponTest {
    private static int checks;

    /** Runs coupon catalogue and checkout tests without temporary model stubs. */
    public static void main(String[] args) throws Exception {
        CouponService service = new CouponService();
        Coupon save10 = new Coupon(" save10 ", true, new PercentageDiscount(new BigDecimal("10")));
        service.addCoupon(save10);
        check(save10.getCode().equals("SAVE10"), "Normalized code");
        check(service.findActive(" SaVe10 ") == save10, "Case-insensitive lookup");
        money("100.00", save10.apply(new BigDecimal("1000")));
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.addCoupon(new Coupon("SAVE10", true, new FixedDiscount(BigDecimal.ONE)));
            }
        });
        check(service.findActive("SAVE10") == save10, "Duplicate preserves original");
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.addCoupon(null);
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.findActive(null);
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.findActive(" ");
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.findActive("missing");
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                new Coupon("bad code", true, new FixedDiscount(BigDecimal.ONE));
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                new Coupon(null, true, new FixedDiscount(BigDecimal.ONE));
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                new Coupon("X", true, null);
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                new Coupon("X", true, save10);
            }
        });

        Coupon fixed = new Coupon("LESS50", true, new FixedDiscount(new BigDecimal("50")));
        service.addCoupon(fixed);
        money("20.00", fixed.apply(new BigDecimal("20")));
        check(service.getAll().get(0).getCode().equals("LESS50"), "Sorted catalogue");
        try { service.getAll().clear(); throw new AssertionError("Mutable catalogue list"); }
        catch (UnsupportedOperationException expected) { checks++; }
        service.deactivate("save10");
        check(!save10.isActive(), "Deactivate updates existing reference");
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.findActive("SAVE10");
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                save10.apply(new BigDecimal("1000"));
            }
        });
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                service.deactivate("missing");
            }
        });
        save10.setActive(true);

        Product milk = new Product("P1", "Milk", Category.DAIRY, new BigDecimal("500"), 10, 2);
        Cart cart = new Cart(); cart.addItem(milk, 2);
        BillingService billing = new BillingService(List.of(milk), 0);
        Bill bill = billing.checkout(cart, "cashier", "WALK-IN", service.findActive("save10"),
                new CashPayment(new BigDecimal("1000")));
        money("100.00", bill.getDiscount()); money("45.00", bill.getTax()); money("945.00", bill.getTotal());
        check(milk.getQuantity() == 8, "Coupon checkout reduces stock exactly once");
        cart.addItem(milk, 1);
        Coupon selected = service.findActive("SAVE10");
        service.deactivate("SAVE10");
        rejects(new Attempt() {
            @Override
            public void run() throws Exception {
                billing.checkout(cart, "cashier", "WALK-IN", selected, new CashPayment(new BigDecimal("1000")));
            }
        });
        check(milk.getQuantity() == 8 && !cart.isEmpty(), "Inactive coupon leaves stock and cart untouched");
        System.out.println("PASS: " + checks + " coupon and checkout checks.");
    }

    private static void money(String expected, BigDecimal actual) {
        check(new BigDecimal(expected).equals(actual), "Expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
    private interface Attempt { void run() throws Exception; }
    private static void rejects(Attempt attempt) throws Exception {
        try { attempt.run(); }
        catch (InvalidInputException | IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected invalid coupon to be rejected");
    }
}
