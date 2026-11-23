package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Supermarket product categories with prototype default tax rates.
 * Note: Tax rates are sample values for demonstration and not legally verified GST rates.
 */
public enum Category {
    GROCERY(new BigDecimal("0.00")),
    DAIRY(new BigDecimal("5.00")),
    BEVERAGE(new BigDecimal("12.00")),
    PERSONAL_CARE(new BigDecimal("18.00")),
    HOUSEHOLD(new BigDecimal("18.00")),
    SNACKS(new BigDecimal("5.00"));

    private final BigDecimal defaultTaxRate;

    /**
     * Initializes the category with a prototype default tax rate percentage.
     */
    Category(BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate.setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the default sample tax rate as a percentage value (e.g. 5.00 for 5%). */
    public BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }
}
