package util;

import exception.InvalidInputException;
import model.Product;

/** Validates product state and uniqueness against an inventory catalog. */
public final class ProductValidator {

    private ProductValidator() {
        // Utility class: prevent instantiation
    }

    /** Validates that the product meets all domain constraints and that its ID is unique in inventory. */
    public static void validate(Product product, Inventory inventory) throws InvalidInputException {
        if (product == null) {
            throw new InvalidInputException("Product cannot be null.");
        }
        if (product.getProductId() == null || product.getProductId().isBlank()) {
            throw new InvalidInputException("Product ID cannot be null or blank.");
        }
        if (inventory != null && inventory.exists(product.getProductId())) {
            throw new InvalidInputException("Product with ID '" + product.getProductId() + "' already exists in inventory.");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new InvalidInputException("Product name cannot be null or blank.");
        }
        if (product.getCategory() == null) {
            throw new InvalidInputException("Product category cannot be null.");
        }
        if (product.getPrice() == null || product.getPrice().signum() <= 0) {
            throw new InvalidInputException("Product price must be greater than zero.");
        }
        if (product.getQuantity() < 0) {
            throw new InvalidInputException("Product quantity cannot be negative.");
        }
        if (product.getReorderLevel() < 0) {
            throw new InvalidInputException("Product reorder level cannot be negative.");
        }
    }
}
