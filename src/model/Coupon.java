package model;

import java.math.BigDecimal;
import java.util.Locale;
import payment.Discount;

/** A reusable coupon that delegates its calculation to an existing discount. */
public final class Coupon implements Discount {
    private final String code;
    private final Discount discount;
    private boolean active;

    /** Creates a coupon with a normalized code and a percentage or fixed discount. */
    public Coupon(String code, boolean active, Discount discount) {
        if (code == null || !code.trim().matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("Coupon code must contain 1-32 letters, digits, underscores or hyphens.");
        }
        if (discount == null || discount instanceof Coupon) {
            throw new IllegalArgumentException("A coupon needs a discount and cannot wrap another coupon.");
        }
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.active = active;
        this.discount = discount;
    }

    /** Returns the normalized coupon code. */
    public String getCode() { return code; }

    /** Reports whether this coupon can currently be used. */
    public boolean isActive() { return active; }

    /** Enables or disables this coupon. */
    public void setActive(boolean active) { this.active = active; }

    /** Returns the wrapped discount strategy. */
    public Discount getDiscount() { return discount; }

    /** Returns the discount value, rejecting inactive coupons even after lookup. */
    @Override
    public BigDecimal apply(BigDecimal amount) {
        if (!active) { throw new IllegalArgumentException("Coupon is inactive: " + code); }
        return discount.apply(amount);
    }
}
