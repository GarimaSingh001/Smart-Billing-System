package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import model.CartItem;

/** Calculates prototype category tax after a proportional discount. */
public final class TaxCalculator {
    /** Accepts a discount fraction (0.10 means 10%) and rounds the summed tax once. */
    public BigDecimal calculateTax(List<CartItem> items, BigDecimal discountRatio) {
        if (items == null || discountRatio == null || discountRatio.signum() < 0
                || discountRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Discount ratio must be between 0 and 1.");
        }
        BigDecimal tax = BigDecimal.ZERO;
        for (CartItem item : items) {
            if (item == null) { throw new IllegalArgumentException("Missing cart line."); }
            BigDecimal discounted = item.getLineAmount().multiply(BigDecimal.ONE.subtract(discountRatio));
            tax = tax.add(discounted.multiply(item.getProduct().getCategory()
                    .getDefaultTaxRate()).movePointLeft(2));
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }
}
