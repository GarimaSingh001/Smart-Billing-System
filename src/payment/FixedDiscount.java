package payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Applies a fixed discount without exceeding the subtotal. */
public final class FixedDiscount implements Discount {
    private final BigDecimal value;

    /** Creates a non-negative fixed discount rounded to two decimal places. */
    public FixedDiscount(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Fixed discount must be non-negative.");
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the smaller of the configured discount and the rounded subtotal. */
    @Override
    public BigDecimal apply(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Subtotal must be non-negative.");
        }
        return value.min(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
