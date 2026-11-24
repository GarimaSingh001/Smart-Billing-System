package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import model.BillQuote;
import model.CartItem;
import payment.Discount;

/** Shared arithmetic for both previews and actual checkout to prevent calculation drift. */
public final class PricingService {
    private final TaxCalculator taxes = new TaxCalculator();

    /** Calculates a quote without changing cart, inventory, payments or files. */
    public BillQuote quote(List<CartItem> items, Discount discount, String option) {
        if (items == null || items.isEmpty() || discount == null) {
            throw new IllegalArgumentException("A non-empty cart and discount are required.");
        }
        BigDecimal subtotal = new BigDecimal("0.00");
        for (CartItem item : items) {
            if (item == null) { throw new IllegalArgumentException("Missing cart line."); }
            subtotal = subtotal.add(item.getLineAmount());
        }
        BigDecimal reduction = discount.apply(subtotal);
        if (reduction == null || reduction.signum() < 0 || reduction.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount must be between zero and subtotal.");
        }
        reduction = reduction.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratio = subtotal.signum() == 0 ? BigDecimal.ZERO
                : reduction.divide(subtotal, 16, RoundingMode.HALF_UP);
        return new BillQuote(option, subtotal, reduction, taxes.calculateTax(items, ratio));
    }
}
