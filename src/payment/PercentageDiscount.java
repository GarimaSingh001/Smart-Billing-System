package payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Applies a percentage between zero and one hundred to a subtotal. */
public final class PercentageDiscount implements Discount {
    private final BigDecimal rate;

    /** Creates a percentage discount; 10 means ten percent. */
    public PercentageDiscount(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        this.rate = rate;
    }

    /** Returns the discount value using HALF_UP rounding on a two-decimal subtotal. */
    @Override
    public BigDecimal apply(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Subtotal must be non-negative.");
        }
        BigDecimal subtotal = amount.setScale(2, RoundingMode.HALF_UP);
        return subtotal.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
}
