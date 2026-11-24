import java.math.BigDecimal;
import payment.Discount;
import payment.FixedDiscount;
import payment.PercentageDiscount;

/** Standalone regression checks for both discount implementations. */
public class DiscountTest {
    private static int checks;

    /** Exercises discount amounts, rounding, boundaries and invalid inputs. */
    public static void main(String[] args) {
        Discount percentage = new PercentageDiscount(new BigDecimal("10"));
        expect("100.00", percentage.apply(new BigDecimal("1000")));
        expect("0.00", percentage.apply(BigDecimal.ZERO));
        expect("0.00", new PercentageDiscount(BigDecimal.ZERO).apply(new BigDecimal("100")));
        expect("19.99", new PercentageDiscount(new BigDecimal("100")).apply(new BigDecimal("19.99")));
        expect("0.01", percentage.apply(new BigDecimal("0.05")));
        expect("2.50", new PercentageDiscount(new BigDecimal("12.5")).apply(new BigDecimal("19.99")));
        expect("1.01", new PercentageDiscount(new BigDecimal("100")).apply(new BigDecimal("1.005")));

        Discount fixed = new FixedDiscount(new BigDecimal("50"));
        expect("50.00", fixed.apply(new BigDecimal("1000")));
        expect("20.00", fixed.apply(new BigDecimal("20")));
        expect("50.00", fixed.apply(new BigDecimal("50")));
        expect("0.00", fixed.apply(BigDecimal.ZERO));
        expect("0.00", new FixedDiscount(BigDecimal.ZERO).apply(new BigDecimal("10")));
        expect("1.01", new FixedDiscount(new BigDecimal("1.005")).apply(new BigDecimal("10")));

        rejects(new Runnable() {
            @Override
            public void run() {
                new PercentageDiscount(null);
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                new PercentageDiscount(new BigDecimal("-1"));
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                new PercentageDiscount(new BigDecimal("100.01"));
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                new FixedDiscount(null);
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                new FixedDiscount(new BigDecimal("-0.001"));
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                percentage.apply(null);
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                percentage.apply(new BigDecimal("-0.001"));
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                fixed.apply(null);
            }
        });
        rejects(new Runnable() {
            @Override
            public void run() {
                fixed.apply(new BigDecimal("-0.001"));
            }
        });

        System.out.println("10% discount on 1000.00 = " + percentage.apply(new BigDecimal("1000")));
        System.out.println("Fixed discount of 50.00 on 20.00 = " + fixed.apply(new BigDecimal("20")));
        System.out.println("PASS: " + checks + " discount checks.");
    }

    private static void expect(String expected, BigDecimal actual) {
        if (!new BigDecimal(expected).equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
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
        throw new AssertionError("Expected invalid input to be rejected.");
    }
}
