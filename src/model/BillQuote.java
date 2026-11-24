package model;

import java.math.BigDecimal;

/** Immutable price preview, without an invoice number or payment side effects. */
public final class BillQuote {
    private final String option;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal tax;
    private final BigDecimal total;

    /** Creates a consistent two-decimal preview. */
    public BillQuote(String option, BigDecimal subtotal, BigDecimal discount, BigDecimal tax) {
        if (option == null || option.isBlank()) { throw new IllegalArgumentException("Option name is required."); }
        for (BigDecimal value : new BigDecimal[] {subtotal, discount, tax}) {
            if (value == null || value.signum() < 0 || value.scale() != 2) {
                throw new IllegalArgumentException("Quote amounts must be non-negative with two decimal places.");
            }
        }
        if (discount.compareTo(subtotal) > 0) { throw new IllegalArgumentException("Discount exceeds subtotal."); }
        this.option = option;
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = subtotal.subtract(discount).add(tax);
    }
    /** Returns the coupon code or no-discount label. */
    public String getOption() { return option; }
    /** Returns the subtotal. */
    public BigDecimal getSubtotal() { return subtotal; }
    /** Returns the discount value. */
    public BigDecimal getDiscount() { return discount; }
    /** Returns the discounted tax. */
    public BigDecimal getTax() { return tax; }
    /** Returns the final estimated total. */
    public BigDecimal getTotal() { return total; }
}
