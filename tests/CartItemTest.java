import exception.InvalidInputException;
import java.math.BigDecimal;
import model.CartItem;
import model.Product;

/** Standalone checks using the shared Product model. */
public class CartItemTest {
    /** Verifies money calculations, quantity updates and invalid input handling. */
    public static void main(String[] args) throws InvalidInputException {
        Product product = product(new BigDecimal("19.99"));
        CartItem item = new CartItem(product, 3);
        check(item.getProduct() == product, "Product reference retained");
        check(item.getQuantity() == 3, "Initial quantity");
        check(item.getLineAmount().equals(new BigDecimal("59.97")), "19.99 x 3 = 59.97");
        System.out.println("19.99 x 3 = " + item.getLineAmount());
        item.setQuantity(2);
        check(item.getLineAmount().equals(new BigDecimal("39.98")), "Updated amount");
        for (int invalid : new int[] {0, -1}) {
            try {
                item.setQuantity(invalid);
                throw new AssertionError("Accepted invalid quantity: " + invalid);
            } catch (InvalidInputException expected) {
                check(item.getQuantity() == 2, "Rejected update leaves quantity unchanged");
            }
            try {
                new CartItem(product, invalid);
                throw new AssertionError("Constructor accepted invalid quantity");
            } catch (InvalidInputException expected) {
                // Expected: invalid cart lines cannot be created.
            }
        }
        try {
            new CartItem(null, 1);
            throw new AssertionError("Accepted missing product");
        } catch (InvalidInputException expected) {
            // Expected: every cart line needs a product.
        }
        check(new CartItem(product(new BigDecimal("0.10")), 3)
                .getLineAmount().equals(new BigDecimal("0.30")), "Exact decimal arithmetic");
        check(new CartItem(product(new BigDecimal("1.005")), 1)
                .getLineAmount().equals(new BigDecimal("1.01")), "HALF_UP rounding");
        System.out.println("PASS: totals, rounding, updates and invalid inputs verified.");
    }

    private static Product product(BigDecimal price) {
        return new Product("P1", "Test product", model.Category.GROCERY, price, 10, 1);
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
