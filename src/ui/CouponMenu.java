package ui;

import exception.InvalidInputException;
import java.math.BigDecimal;
import model.Coupon;
import payment.Discount;
import payment.FixedDiscount;
import payment.PercentageDiscount;
import service.CouponService;

/** Coupon entry shared by the cashier menu and the standalone rehearsal. */
public final class CouponMenu {
    private CouponMenu() { }

    /** Retries invalid codes; a blank response selects no discount. */
    public static Discount choose(ConsoleInput input, CouponService coupons) {
        while (true) {
            String code = input.readLine("Coupon code (blank = no discount): ");
            if (code.isEmpty()) { return new FixedDiscount(BigDecimal.ZERO); }
            try { return coupons.findActive(code); }
            catch (InvalidInputException e) { System.out.println("[ERROR] " + e.getMessage()); }
        }
    }

    /** Builds sample codes for demonstrations, not a persisted coupon catalogue. */
    public static CouponService demoCatalogue() {
        CouponService coupons = new CouponService();
        try {
            coupons.addCoupon(new Coupon("SAVE10", true, new PercentageDiscount(new BigDecimal("10"))));
            coupons.addCoupon(new Coupon("LESS50", true, new FixedDiscount(new BigDecimal("50"))));
        } catch (InvalidInputException e) { throw new IllegalStateException("Invalid demo configuration", e); }
        return coupons;
    }
}
