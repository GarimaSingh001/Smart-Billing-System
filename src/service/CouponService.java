package service;

import exception.InvalidInputException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import model.Coupon;

/** In-memory coupon catalogue for the single-process prototype. */
public final class CouponService {
    private final Map<String, Coupon> coupons = new HashMap<>();

    /** Registers a coupon, rejecting duplicate codes without replacing the original. */
    public void addCoupon(Coupon coupon) throws InvalidInputException {
        if (coupon == null) { throw new InvalidInputException("Coupon is required."); }
        if (coupons.putIfAbsent(coupon.getCode(), coupon) != null) {
            throw new InvalidInputException("Coupon code already exists: " + coupon.getCode());
        }
    }

    /** Resolves an active coupon by code for use as BillingService's Discount argument. */
    public Coupon findActive(String code) throws InvalidInputException {
        Coupon coupon = find(code);
        if (!coupon.isActive()) { throw new InvalidInputException("Coupon is inactive: " + coupon.getCode()); }
        return coupon;
    }

    /** Disables an existing coupon; previously returned references also become inactive. */
    public void deactivate(String code) throws InvalidInputException {
        find(code).setActive(false);
    }

    /** Returns an unmodifiable list of registered coupons sorted by code. */
    public List<Coupon> getAll() {
        return coupons.values().stream().sorted(Comparator.comparing(Coupon::getCode)).toList();
    }

    private Coupon find(String code) throws InvalidInputException {
        if (code == null || code.isBlank()) { throw new InvalidInputException("Enter a coupon code."); }
        Coupon coupon = coupons.get(code.trim().toUpperCase(Locale.ROOT));
        if (coupon == null) { throw new InvalidInputException("Unknown coupon code: " + code); }
        return coupon;
    }
}
