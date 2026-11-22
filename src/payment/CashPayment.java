package payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Checks cash tendered and calculates change for the most recent attempt. */
public final class CashPayment implements Payment {
    private final BigDecimal cashTendered;
    private BigDecimal change = new BigDecimal("0.00");

    /** Creates a cash payment with non-negative tendered cash rounded to two decimals. */
    public CashPayment(BigDecimal cashTendered) {
        if (cashTendered == null || cashTendered.signum() < 0) {
            throw new IllegalArgumentException("Cash tendered must be non-negative.");
        }
        this.cashTendered = cashTendered.setScale(2, RoundingMode.HALF_UP);
    }

    /** Accepts sufficient cash, including a zero total, and resets change on failure. */
    @Override
    public boolean pay(BigDecimal amount) {
        change = new BigDecimal("0.00");
        if (amount == null || amount.signum() < 0) {
            return false;
        }
        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);
        if (cashTendered.compareTo(total) < 0) {
            return false;
        }
        change = cashTendered.subtract(total);
        return true;
    }

    /** Returns change after success, or zero before payment and after a failed attempt. */
    public BigDecimal getChange() {
        return change;
    }

    /** Returns the invoice payment label. */
    @Override
    public String getMode() {
        return "CASH";
    }
}
