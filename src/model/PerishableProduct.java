package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Represents a perishable product that has an expiration date. */
public class PerishableProduct extends Product {

    private final LocalDate expiryDate;

    /** Creates a new perishable product with the given details including expiry date. */
    public PerishableProduct(String productId, String name, Category category, BigDecimal price,
                            int quantity, int reorderLevel, LocalDate expiryDate) {
        super(productId, name, category, price, quantity, reorderLevel);
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date cannot be null.");
    }

    /** Returns the expiration date of this product. */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /** Returns true if the product has passed its expiration date. */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    /** Serializes this perishable product into a seven-column CSV string including the expiry date. */
    @Override
    public String toCsv() {
        return super.toCsv() + "," + expiryDate.toString();
    }

    /** Returns a string representation of the perishable product including its expiry details. */
    @Override
    public String toString() {
        return super.toString() + " [Expires: " + expiryDate + (isExpired() ? " (EXPIRED)" : "") + "]";
    }
}
