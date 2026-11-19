package model;

import exception.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Represents one product and its requested quantity in a shopping cart. */
public final class CartItem {
    private final Product product;
    private int quantity;

    /** Creates a cart line with a product and a strictly positive quantity. */
    public CartItem(Product product, int quantity) throws InvalidInputException {
        if (product == null) {
            throw new InvalidInputException("A cart item must have a product.");
        }
        this.product = product;
        setQuantity(quantity);
    }

    /** Returns the product associated with this cart line. */
    public Product getProduct() {
        return product;
    }

    /** Returns the number of units requested. */
    public int getQuantity() {
        return quantity;
    }

    /** Updates the quantity, rejecting zero or negative values. */
    public void setQuantity(int quantity) throws InvalidInputException {
        if (quantity <= 0) {
            throw new InvalidInputException("Quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    /** Calculates the line amount using two decimal places and HALF_UP rounding. */
    public BigDecimal getLineAmount() {
        return product.getPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
