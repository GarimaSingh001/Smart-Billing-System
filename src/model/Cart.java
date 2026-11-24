package model;

import exception.InvalidInputException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Holds cart lines; adding the same product ID increases its quantity. */
public final class Cart {
    private final List<CartItem> items = new ArrayList<>();

    /** Adds a positive quantity, checking overflow before changing an existing line. */
    public void addItem(Product product, int quantity) throws InvalidInputException {
        if (product == null || quantity <= 0) {
            throw new InvalidInputException("Select a product and a positive quantity.");
        }
        for (CartItem item : items) {
            if (item.getProduct().equals(product)) {
                long combined = (long) item.getQuantity() + quantity;
                if (combined > Integer.MAX_VALUE) {
                    throw new InvalidInputException("Quantity is too large.");
                }
                item.setQuantity((int) combined);
                return;
            }
        }
        items.add(new CartItem(product, quantity));
    }

    /** Removes a line or reports that the product is not in this cart. */
    public void removeItem(String productId) throws InvalidInputException {
        items.remove(find(productId));
    }

    /** Changes a line quantity using CartItem's validation. */
    public void changeQuantity(String productId, int quantity) throws InvalidInputException {
        find(productId).setQuantity(quantity);
    }

    /** Returns the sum of the current line amounts. */
    public BigDecimal getSubtotal() {
        BigDecimal subtotal = new BigDecimal("0.00");
        for (CartItem item : items) {
            subtotal = subtotal.add(item.getLineAmount());
        }
        return subtotal;
    }

    /** Returns copies of cart lines so callers cannot bypass cart quantity controls. */
    public List<CartItem> getItems() {
        List<CartItem> result = new ArrayList<>();
        for (CartItem item : items) {
            try {
                result.add(new CartItem(item.getProduct(), item.getQuantity()));
            } catch (InvalidInputException e) {
                throw new IllegalStateException("Invalid cart state", e);
            }
        }
        return List.copyOf(result);
    }

    /** Reports whether the cart has no lines. */
    public boolean isEmpty() { return items.isEmpty(); }

    /** Removes all lines. */
    public void clear() { items.clear(); }

    private CartItem find(String id) throws InvalidInputException {
        for (CartItem item : items) {
            if (item.getProduct().getProductId().equalsIgnoreCase(id)) { return item; }
        }
        throw new InvalidInputException("Product is not in the cart: " + id);
    }
}
