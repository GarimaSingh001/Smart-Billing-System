import java.math.BigDecimal;
import payment.CashPayment;
import payment.Payment;
import payment.UpiPayment;

/** Standalone checks for cash handling and simulated UPI payments. */
public class PaymentTest {
    private static int checks;

    /** Tests success, rejection, decimal rounding and interface-based payment calls. */
    public static void main(String[] args) {
        CashPayment cash = new CashPayment(new BigDecimal("1000"));
        money("0.00", cash.getChange());
        Payment method = cash;
        check(method.pay(new BigDecimal("945")), "Sufficient cash");
        money("55.00", cash.getChange());
        check("CASH".equals(method.getMode()), "Cash mode");
        check(!method.pay(new BigDecimal("1000.01")), "Insufficient cash rejected");
        money("0.00", cash.getChange());
        check(method.pay(new BigDecimal("1000")), "Exact cash");
        money("0.00", cash.getChange());
        check(!method.pay(null), "Null total rejected");
        check(!method.pay(new BigDecimal("-0.001")), "Negative total rejected before rounding");
        check(method.pay(BigDecimal.ZERO), "Fully discounted zero total accepted");
        money("1000.00", cash.getChange());
        check(!method.pay(null), "Failed attempt after success");
        money("0.00", cash.getChange());

        CashPayment coins = new CashPayment(new BigDecimal("0.30"));
        check(coins.pay(new BigDecimal("0.10")), "Exact decimal subtraction");
        money("0.20", coins.getChange());
        CashPayment rounded = new CashPayment(new BigDecimal("1.005"));
        check(rounded.pay(new BigDecimal("1.004")), "HALF_UP rounding");
        money("0.01", rounded.getChange());
        rejects(new Runnable() {
            @Override
            public void run() {
                new CashPayment(null);
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                new CashPayment(new BigDecimal("-0.001"));
            }
        });

        method = new UpiPayment(" shopper@demo ");
        check(method.pay(new BigDecimal("945.00")), "Simulated UPI success");
        check("UPI".equals(method.getMode()), "UPI mode");
        check(method.pay(BigDecimal.ZERO), "Zero UPI total accepted");
        check(!method.pay(null), "Null UPI total rejected");
        check(!method.pay(new BigDecimal("-1")), "Negative UPI total rejected");
        for (String id : new String[] {null, "", "shopper", "@demo", "shopper@", "a@@b", "a b@demo"}) {
            check(!new UpiPayment(id).pay(BigDecimal.ONE), "Malformed demo UPI ID rejected");
        }
        System.out.println("Cash: 1000.00 tendered for 945.00 gives 55.00 change.");
        System.out.println("UPI: simulated validation only; no money transferred.");
        System.out.println("PASS: " + checks + " payment checks.");
    }

    private static void money(String expected, BigDecimal actual) {
        check(new BigDecimal(expected).equals(actual), "Expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Expected invalid cash tendered to be rejected.");
    }
}
