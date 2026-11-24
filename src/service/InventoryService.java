package service;

import exception.InvalidInputException;
import exception.ProductNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Product;
import util.Inventory;
import util.ProductComparators;

/** Stock receiving, availability checks and low-stock reporting for the menus. */
public final class InventoryService {
    private final Inventory inventory;

    /** Uses the shared inventory so stock changes are visible during checkout. */
    public InventoryService(Inventory inventory) {
        if (inventory == null) { throw new IllegalArgumentException("Inventory is required."); }
        this.inventory = inventory;
    }

    /** Adds positive stock and rejects integer overflow before any mutation. */
    public void receiveStock(String id, int quantity) throws ProductNotFoundException, InvalidInputException {
        inventory.addStock(id, quantity);
    }

    /** Reports whether an existing product has the requested positive quantity. */
    public boolean isAvailable(String id, int quantity) throws ProductNotFoundException, InvalidInputException {
        if (quantity <= 0) { throw new InvalidInputException("Requested quantity must be positive."); }
        return inventory.findById(id).getQuantity() >= quantity;
    }

    /** Returns products at or below their reorder level, lowest stock first. */
    public List<Product> getLowStockProducts() {
        List<Product> result = new ArrayList<>();
        for (Product product : inventory.getAll()) {
            if (product.isLowStock()) { result.add(product); }
        }
        Collections.sort(result, ProductComparators.BY_STOCK);
        return result;
    }
}
