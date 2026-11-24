package util;

import java.util.Comparator;
import model.Product;

/** Reusable ascending product orders with product-ID tie breaks. */
public final class ProductComparators {
    private ProductComparators() { }

    /** Orders names without case differences, then IDs for predictable display. */
    public static final Comparator<Product> BY_NAME = new Comparator<Product>() {
        @Override
        public int compare(Product first, Product second) {
            int result = first.getName().compareToIgnoreCase(second.getName());
            return result == 0 ? byId(first, second) : result;
        }
    };

    /** Orders exact decimal prices, then IDs. */
    public static final Comparator<Product> BY_PRICE = new Comparator<Product>() {
        @Override
        public int compare(Product first, Product second) {
            int result = first.getPrice().compareTo(second.getPrice());
            return result == 0 ? byId(first, second) : result;
        }
    };

    /** Orders stock from lowest to highest, then IDs. */
    public static final Comparator<Product> BY_STOCK = new Comparator<Product>() {
        @Override
        public int compare(Product first, Product second) {
            int result = Integer.compare(first.getQuantity(), second.getQuantity());
            return result == 0 ? byId(first, second) : result;
        }
    };

    private static int byId(Product first, Product second) {
        int result = first.getProductId().compareToIgnoreCase(second.getProductId());
        return result == 0 ? first.getProductId().compareTo(second.getProductId()) : result;
    }
}
