package util;

import exception.InsufficientStockException;
import exception.InvalidInputException;
import exception.ProductNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Product;

/** In-memory inventory catalog storing and managing products by their unique IDs. */
public class Inventory {

    private final Map<String, Product> products;

    /** Creates an empty inventory catalog. */
    public Inventory() {
        this.products = new HashMap<>();
    }

    /** Adds or replaces a product in the inventory. */
    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        products.put(key(product.getProductId()), product);
    }

    /** Finds a product by its ID, throwing ProductNotFoundException if absent. */
    public Product findById(String id) throws ProductNotFoundException {
        if (id == null || id.isBlank()) {
            throw new ProductNotFoundException(id == null ? "null" : id.trim());
        }
        Product product = products.get(key(id));
        if (product == null) {
            throw new ProductNotFoundException(id.trim());
        }
        return product;
    }

    /** Returns true if a product with the specified ID exists in the inventory. */
    public boolean exists(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return products.containsKey(key(id));
    }

    /** Returns an independent copy of all products currently in the inventory. */
    public List<Product> getAll() {
        return new ArrayList<>(products.values());
    }

    /** Reduces the stock of a product, throwing an exception if quantity is invalid or stock is insufficient. */
    public void reduceStock(String id, int qty) throws InsufficientStockException, ProductNotFoundException {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity to reduce must be greater than zero, got: " + qty);
        }
        Product product = findById(id);
        int current = product.getQuantity();
        if (qty > current) {
            throw new InsufficientStockException(product.getProductId(), qty, current);
        }
        product.setQuantity(current - qty);
    }

    /** Adds stock to an existing product, throwing an exception if quantity is non-positive or product is missing. */
    public void addStock(String id, int qty) throws ProductNotFoundException, InvalidInputException {
        if (qty <= 0) {
            throw new InvalidInputException("Quantity to add must be greater than zero, got: " + qty);
        }
        Product product = findById(id);
        long total = (long) product.getQuantity() + qty;
        if (total > Integer.MAX_VALUE) {
            throw new InvalidInputException("Stock quantity exceeds the supported limit.");
        }
        product.setQuantity((int) total);
    }

    /** Removes an existing product using the same ID rules as lookup. */
    public void removeProduct(String id) throws ProductNotFoundException {
        Product product = findById(id);
        products.remove(key(product.getProductId()));
    }

    private String key(String id) {
        return id.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
