package exception;

/** Signals that a requested product was not found in the inventory. */
public class ProductNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Creates a new exception referencing the missing product ID. */
    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
    }
}
