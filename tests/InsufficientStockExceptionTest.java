import exception.InsufficientStockException;

/** Runs a small standalone check without external test libraries. */
public class InsufficientStockExceptionTest {
    /** Checks that callers can catch the exception and read its stock details. */
    public static void main(String[] args) {
        try {
            throw new InsufficientStockException("P001", 5, 2);
        } catch (InsufficientStockException ex) {
            String expected = "Insufficient stock for product P001: requested 5, available 2.";
            if (!expected.equals(ex.getMessage())) {
                throw new AssertionError("Unexpected message: " + ex.getMessage());
            }
            System.out.println(ex.getMessage());
            System.out.println("PASS: exception caught and stock details verified.");
        }
    }
}
