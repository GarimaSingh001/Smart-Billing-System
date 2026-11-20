package payment;

import java.math.BigDecimal;

/** Calculates the amount to subtract from a subtotal, not the final payable amount. */
public interface Discount {
    /** Returns a non-negative discount, rounded to two decimal places. */
    BigDecimal apply(BigDecimal amount);
}
