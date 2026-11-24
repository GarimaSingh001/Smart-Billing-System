package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a supermarket product item in the inventory and billing system.
 */
public class Product {

    private String productId;
    private String name;
    private Category category;
    private BigDecimal price;
    private int quantity;
    private int reorderLevel;

    /** Creates a new product with the given details, validating required fields. */
    public Product(String productId, String name, Category category, BigDecimal price, int quantity, int reorderLevel) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank.");
        }
        if (productId.contains(",")) {
            throw new IllegalArgumentException("Product ID cannot contain commas.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank.");
        }
        if (name.contains(",")) {
            throw new IllegalArgumentException("Product name cannot contain commas.");
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        this.productId = productId.trim();
        this.name = name.trim();
        this.category = Objects.requireNonNull(category, "Category cannot be null.");
        this.reorderLevel = reorderLevel;
        setPrice(price);
        setQuantity(quantity);
    }

    /** Returns the unique identifier of the product. */
    public String getProductId() {
        return productId;
    }

    /** Returns the display name of the product. */
    public String getName() {
        return name;
    }

    /** Returns the category to which this product belongs. */
    public Category getCategory() {
        return category;
    }

    /** Returns the unit price of the product formatted to two decimal places. */
    public BigDecimal getPrice() {
        return price;
    }

    /** Returns the current quantity in stock. */
    public int getQuantity() {
        return quantity;
    }

    /** Returns the minimum stock threshold that triggers a reorder alert. */
    public int getReorderLevel() {
        return reorderLevel;
    }

    /** Updates the unit price, validating that it is strictly positive. */
    public void setPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        BigDecimal rounded = price.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0) {
            throw new IllegalArgumentException("Price must round to at least 0.01.");
        }
        this.price = rounded;
    }

    /** Updates the stock quantity, validating that it is non-negative. */
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    /**
     * Returns true when the current stock quantity is less than or equal to the
     * reorder level.
     */
    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    /** Serializes this product into a standard six-column CSV string. */
    public String toCsv() {
        return String.join(",",
                productId,
                name,
                category.name(),
                price.toPlainString(),
                String.valueOf(quantity),
                String.valueOf(reorderLevel));
    }

    /** Parses a CSV line and constructs a Product or PerishableProduct object. */
    public static Product fromCsv(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("CSV line cannot be null or blank.");
        }
        String[] parts = line.split(",", -1);
        if (parts.length != 6 && parts.length != 7) {
            throw new IllegalArgumentException("Expected 6 or 7 fields in product CSV line, but found " + parts.length);
        }
        String id = parts[0].trim();
        String name = parts[1].trim();
        Category category;
        try {
            category = Category.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown product category: " + parts[2], e);
        }
        BigDecimal price;
        try {
            price = new BigDecimal(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price number format: " + parts[3], e);
        }
        int quantity;
        try {
            quantity = Integer.parseInt(parts[4].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid quantity integer format: " + parts[4], e);
        }
        int reorderLevel;
        try {
            reorderLevel = Integer.parseInt(parts[5].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid reorder level integer format: " + parts[5], e);
        }
        if (parts.length == 7) {
            String expiryStr = parts[6].trim();
            if (expiryStr.isBlank()) {
                throw new IllegalArgumentException("Expiry date column cannot be blank in perishable product CSV.");
            }
            LocalDate expiryDate;
            try {
                expiryDate = LocalDate.parse(expiryStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid expiry date format: " + expiryStr, e);
            }
            return new PerishableProduct(id, name, category, price, quantity, reorderLevel, expiryDate);
        }
        return new Product(id, name, category, price, quantity, reorderLevel);
    }

    /** Compares this product with another by product ID. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product other = (Product) o;
        return productId.equalsIgnoreCase(other.productId);
    }

    /** Returns a hash code consistent with the product ID. */
    @Override
    public int hashCode() {
        return productId.toLowerCase(Locale.ROOT).hashCode();
    }

    /** Returns a string representation of the product for display. */
    @Override
    public String toString() {
        return productId + " - " + name + " [" + category + "] Rs " + price + " (Stock: " + quantity + ")";
    }
}
