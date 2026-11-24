package service;

import exception.InvalidInputException;
import exception.ProductNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import model.Product;
import util.Inventory;
import util.ProductComparators;
import util.ProductValidator;

/** Product catalogue operations used by the admin and cashier menus. */
public final class ProductService {
    private final Inventory inventory;

    /** Uses the same inventory instance as billing and stock management. */
    public ProductService(Inventory inventory) {
        if (inventory == null) { throw new IllegalArgumentException("Inventory is required."); }
        this.inventory = inventory;
    }

    /** Adds a valid product without replacing an existing product ID. */
    public void addProduct(Product product) throws InvalidInputException {
        ProductValidator.validate(product, inventory);
        inventory.addProduct(product);
    }

    /** Finds an ID without case or surrounding-space differences. */
    public Product searchById(String id) throws ProductNotFoundException {
        return inventory.findById(id);
    }

    /** Finds names containing non-blank search text and returns name-sorted matches. */
    public List<Product> searchByName(String text) throws InvalidInputException {
        if (text == null || text.isBlank()) { throw new InvalidInputException("Enter product search text."); }
        String wanted = text.trim().toLowerCase(Locale.ROOT);
        List<Product> matches = new ArrayList<>();
        for (Product product : inventory.getAll()) {
            if (product.getName().toLowerCase(Locale.ROOT).contains(wanted)) { matches.add(product); }
        }
        Collections.sort(matches, ProductComparators.BY_NAME);
        return matches;
    }

    /** Returns a sorted copy of the catalogue; changing the list does not change inventory. */
    public List<Product> listSorted(Comparator<Product> order) throws InvalidInputException {
        if (order == null) { throw new InvalidInputException("Choose a product sort order."); }
        List<Product> result = inventory.getAll();
        Collections.sort(result, order);
        return result;
    }

    /** Validates both fields before changing the existing shared product object. */
    public void updateProduct(String id, BigDecimal price, int quantity)
            throws ProductNotFoundException, InvalidInputException {
        Product product = inventory.findById(id);
        Product validated;
        try {
            validated = new Product(product.getProductId(), product.getName(), product.getCategory(),
                    price, quantity, product.getReorderLevel());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(e.getMessage());
        }
        product.setPrice(validated.getPrice());
        product.setQuantity(validated.getQuantity());
    }

    /** Removes a product, reporting unknown IDs instead of silently succeeding. */
    public void deleteProduct(String id) throws ProductNotFoundException {
        inventory.removeProduct(id);
    }
}
