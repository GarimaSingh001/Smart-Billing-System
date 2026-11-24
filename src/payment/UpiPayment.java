package payment;

import java.math.BigDecimal;

/** Simulates UPI payment without contacting a bank or transferring money. */
public final class UpiPayment implements Payment {
    private final String upiId;

    /** Stores the demo UPI ID; malformed IDs cause pay to return false. */
    public UpiPayment(String upiId) {
        this.upiId = upiId == null ? "" : upiId.trim();
    }

    /** Simulates success for non-negative totals and a basic name@provider format. */
    @Override
    public boolean pay(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return false;
        }
        int at = upiId.indexOf('@');
        if (at <= 0 || at >= upiId.length() - 1 || at != upiId.lastIndexOf('@')) {
            return false;
        }
        for (int i = 0; i < upiId.length(); i++) {
            if (Character.isWhitespace(upiId.charAt(i))) { return false; }
        }
        return true;
    }

    /** Returns the invoice payment label; this prototype's UPI is simulated. */
    @Override
    public String getMode() {
        return "UPI";
    }
}
