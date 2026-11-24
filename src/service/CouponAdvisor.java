package service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.BillQuote;
import model.Cart;
import model.CartItem;
import model.Coupon;
import payment.FixedDiscount;

/** Compares each active coupon individually, without stacking or applying one automatically. */
public final class CouponAdvisor {
    private final PricingService pricing = new PricingService();

    /** Returns cheapest-first previews; stable ties favor no discount, then coupon code order. */
    public List<BillQuote> compare(Cart cart, CouponService catalogue) {
        if (cart == null || catalogue == null) { throw new IllegalArgumentException("Cart and catalogue are required."); }
        List<CartItem> items = cart.getItems();
        List<BillQuote> quotes = new ArrayList<>();
        quotes.add(pricing.quote(items, new FixedDiscount(BigDecimal.ZERO), "No coupon"));
        for (Coupon coupon : catalogue.getAll()) {
            if (coupon.isActive()) { quotes.add(pricing.quote(items, coupon, coupon.getCode())); }
        }
        quotes.sort(Comparator.comparing(BillQuote::getTotal));
        return List.copyOf(quotes);
    }
}
