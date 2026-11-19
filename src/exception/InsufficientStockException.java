package exception;

/** Signals that a requested sale quantity exceeds available stock. */
public class InsufficientStockException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates a readable error containing the product and stock quantities. */
    public InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for product " + productId
                + ": requested " + requested + ", available " + available + ".");
    }
}
