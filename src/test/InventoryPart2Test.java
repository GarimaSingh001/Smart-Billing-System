package test;

import exception.InsufficientStockException;
import exception.InvalidInputException;
import exception.ProductNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import model.Category;
import model.Product;
import util.Inventory;
import util.ProductValidator;

/** Unit tests for Part 2 classes: ProductNotFoundException, Inventory, and ProductValidator. */
public class InventoryPart2Test {

    private static int passed = 0;
    private static int failed = 0;

    /** Runs all Part 2 unit tests and outputs the results. */
    public static void main(String[] args) {
        System.out.println("Starting Member B - Part 2 Unit Tests (Inventory & Validator)...\n");

        testAddAndFindById();
        testExists();
        testGetAllReturnsCopy();
        testMissingIdThrowsNotFound();
        testReduceStockSuccess();
        testOversellThrowsInsufficientStock();
        testOversellLeavesStockUnchanged();
        testAddStockSuccess();
        testAddStockInvalidQuantity();
        testValidatorRejectsDuplicateId();
        testValidatorRejectsBlankIdAndName();
        testValidatorRejectsZeroAndNegativePrice();
        testValidatorRejectsNegativeQuantityAndReorderLevel();
        testValidatorAcceptsValidProduct();

        System.out.println("\n-------------------------------------------");
        System.out.printf("Test Summary: %d passed, %d failed%n", passed, failed);
        System.out.println("-------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testAddAndFindById() {
        System.out.println("--- Test 1: Add product and find by ID ---");
        Inventory inv = new Inventory();
        Product p1 = new Product("P001", "Basmati Rice 5kg", Category.GROCERY, new BigDecimal("520.00"), 40, 10);
        inv.addProduct(p1);

        try {
            Product found = inv.findById("P001");
            check("Found product matches added product", "P001".equals(found.getProductId()));
            check("Found product has correct name", "Basmati Rice 5kg".equals(found.getName()));
        } catch (ProductNotFoundException e) {
            fail("ProductNotFoundException unexpectedly thrown: " + e.getMessage());
        }
    }

    private static void testExists() {
        System.out.println("\n--- Test 2: exists() method ---");
        Inventory inv = new Inventory();
        inv.addProduct(new Product("P002", "Wheat Flour", Category.GROCERY, new BigDecimal("410.00"), 30, 10));

        check("exists() returns true for P002", inv.exists("P002"));
        check("exists() returns false for P999", !inv.exists("P999"));
        check("exists() returns false for null", !inv.exists(null));
        check("exists() returns false for blank", !inv.exists("   "));
    }

    private static void testGetAllReturnsCopy() {
        System.out.println("\n--- Test 3: getAll() returns an independent copy ---");
        Inventory inv = new Inventory();
        Product p1 = new Product("P001", "Item 1", Category.GROCERY, new BigDecimal("10.00"), 5, 2);
        inv.addProduct(p1);

        List<Product> list1 = inv.getAll();
        check("Initial list contains 1 item", list1.size() == 1);

        list1.clear();
        check("Clearing returned list does not affect inventory", inv.getAll().size() == 1);

        Product p2 = new Product("P002", "Item 2", Category.DAIRY, new BigDecimal("20.00"), 10, 3);
        list1.add(p2);
        check("Adding to returned list does not affect inventory", inv.getAll().size() == 1);
    }

    private static void testMissingIdThrowsNotFound() {
        System.out.println("\n--- Test 4: Missing product ID throws ProductNotFoundException ---");
        Inventory inv = new Inventory();
        try {
            inv.findById("NON_EXISTENT");
            fail("Expected ProductNotFoundException for NON_EXISTENT");
        } catch (ProductNotFoundException e) {
            check("ProductNotFoundException thrown with missing ID in message",
                    e.getMessage().contains("NON_EXISTENT"));
        }

        try {
            inv.findById(null);
            fail("Expected ProductNotFoundException for null ID");
        } catch (ProductNotFoundException e) {
            pass("ProductNotFoundException thrown for null ID");
        }
    }

    private static void testReduceStockSuccess() {
        System.out.println("\n--- Test 5: Reducing stock by valid amount ---");
        Inventory inv = new Inventory();
        Product p = new Product("P003", "Sugar 1kg", Category.GROCERY, new BigDecimal("48.00"), 50, 10);
        inv.addProduct(p);

        try {
            inv.reduceStock("P003", 20);
            check("Stock reduced from 50 to 30", p.getQuantity() == 30);
            inv.reduceStock("P003", 30);
            check("Stock reduced from 30 to 0", p.getQuantity() == 0);
        } catch (Exception e) {
            fail("Unexpected exception during valid reduceStock: " + e.getMessage());
        }
    }

    private static void testOversellThrowsInsufficientStock() {
        System.out.println("\n--- Test 6: Overselling throws InsufficientStockException ---");
        Inventory inv = new Inventory();
        Product p = new Product("P004", "Milk 1L", Category.DAIRY, new BigDecimal("66.00"), 15, 5);
        inv.addProduct(p);

        try {
            inv.reduceStock("P004", 16);
            fail("Expected InsufficientStockException when reducing 16 units from stock of 15");
        } catch (InsufficientStockException e) {
            check("InsufficientStockException message mentions requested (16) and available (15)",
                    e.getMessage().contains("16") && e.getMessage().contains("15"));
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName());
        }
    }

    private static void testOversellLeavesStockUnchanged() {
        System.out.println("\n--- Test 7: Oversell rejection leaves original stock unchanged ---");
        Inventory inv = new Inventory();
        Product p = new Product("P005", "Paneer 200g", Category.DAIRY, new BigDecimal("90.00"), 25, 5);
        inv.addProduct(p);

        try {
            inv.reduceStock("P005", 100);
        } catch (InsufficientStockException expected) {
            // Expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        check("Stock remained untouched at 25 after rejected oversell attempt", p.getQuantity() == 25);
    }

    private static void testAddStockSuccess() {
        System.out.println("\n--- Test 8: Adding valid stock increases quantity ---");
        Inventory inv = new Inventory();
        Product p = new Product("P006", "Butter 100g", Category.DAIRY, new BigDecimal("58.00"), 10, 5);
        inv.addProduct(p);

        try {
            inv.addStock("P006", 15);
            check("Stock increased from 10 to 25", p.getQuantity() == 25);
        } catch (Exception e) {
            fail("Unexpected exception during valid addStock: " + e.getMessage());
        }
    }

    private static void testAddStockInvalidQuantity() {
        System.out.println("\n--- Test 9: Adding zero or negative stock is rejected ---");
        Inventory inv = new Inventory();
        Product p = new Product("P007", "Juice 1L", Category.BEVERAGE, new BigDecimal("110.00"), 20, 5);
        inv.addProduct(p);

        try {
            inv.addStock("P007", 0);
            fail("Expected InvalidInputException for addStock with qty 0");
        } catch (InvalidInputException e) {
            pass("addStock with 0 throws InvalidInputException");
        } catch (Exception e) {
            fail("Unexpected exception type for qty 0: " + e.getClass().getSimpleName());
        }

        try {
            inv.addStock("P007", -5);
            fail("Expected InvalidInputException for addStock with negative qty");
        } catch (InvalidInputException e) {
            pass("addStock with negative qty throws InvalidInputException");
        } catch (Exception e) {
            fail("Unexpected exception type for negative qty: " + e.getClass().getSimpleName());
        }

        check("Stock remains 20 after rejected addStock operations", p.getQuantity() == 20);
    }

    private static void testValidatorRejectsDuplicateId() {
        System.out.println("\n--- Test 10: ProductValidator rejects duplicate ID ---");
        Inventory inv = new Inventory();
        inv.addProduct(new Product("P001", "Existing", Category.GROCERY, new BigDecimal("10.00"), 5, 2));

        Product duplicate = new Product("P001", "Duplicate ID Item", Category.DAIRY, new BigDecimal("15.00"), 3, 1);
        try {
            ProductValidator.validate(duplicate, inv);
            fail("Expected InvalidInputException for duplicate product ID");
        } catch (InvalidInputException e) {
            check("InvalidInputException message mentions duplicate ID", e.getMessage().contains("P001"));
        }
    }

    private static void testValidatorRejectsBlankIdAndName() {
        System.out.println("\n--- Test 11: ProductValidator rejects blank ID and name ---");
        Inventory inv = new Inventory();

        Product blankId = new Product("VALID_ID", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public String getProductId() {
                return "   ";
            }
        };
        try {
            ProductValidator.validate(blankId, inv);
            fail("Expected InvalidInputException for blank ID");
        } catch (InvalidInputException e) {
            pass("Blank product ID rejected by ProductValidator");
        }

        Product blankName = new Product("P008", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public String getName() {
                return "   ";
            }
        };
        try {
            ProductValidator.validate(blankName, inv);
            fail("Expected InvalidInputException for blank name");
        } catch (InvalidInputException e) {
            pass("Blank product name rejected by ProductValidator");
        }
    }

    private static void testValidatorRejectsZeroAndNegativePrice() {
        System.out.println("\n--- Test 12: ProductValidator rejects zero and negative price ---");
        Inventory inv = new Inventory();

        Product zeroPrice = new Product("P009", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public BigDecimal getPrice() {
                return BigDecimal.ZERO;
            }
        };
        try {
            ProductValidator.validate(zeroPrice, inv);
            fail("Expected InvalidInputException for zero price");
        } catch (InvalidInputException e) {
            pass("Zero price rejected by ProductValidator");
        }

        Product negativePrice = new Product("P010", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public BigDecimal getPrice() {
                return new BigDecimal("-10.00");
            }
        };
        try {
            ProductValidator.validate(negativePrice, inv);
            fail("Expected InvalidInputException for negative price");
        } catch (InvalidInputException e) {
            pass("Negative price rejected by ProductValidator");
        }

        Product nullPrice = new Product("P011", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public BigDecimal getPrice() {
                return null;
            }
        };
        try {
            ProductValidator.validate(nullPrice, inv);
            fail("Expected InvalidInputException for null price");
        } catch (InvalidInputException e) {
            pass("Null price rejected by ProductValidator");
        }
    }

    private static void testValidatorRejectsNegativeQuantityAndReorderLevel() {
        System.out.println("\n--- Test 13: ProductValidator rejects negative quantity and reorder level ---");
        Inventory inv = new Inventory();

        Product negQty = new Product("P012", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public int getQuantity() {
                return -1;
            }
        };
        try {
            ProductValidator.validate(negQty, inv);
            fail("Expected InvalidInputException for negative quantity");
        } catch (InvalidInputException e) {
            pass("Negative quantity rejected by ProductValidator");
        }

        Product negReorder = new Product("P013", "Valid Name", Category.GROCERY, new BigDecimal("10.00"), 5, 2) {
            @Override
            public int getReorderLevel() {
                return -5;
            }
        };
        try {
            ProductValidator.validate(negReorder, inv);
            fail("Expected InvalidInputException for negative reorder level");
        } catch (InvalidInputException e) {
            pass("Negative reorder level rejected by ProductValidator");
        }
    }

    private static void testValidatorAcceptsValidProduct() {
        System.out.println("\n--- Extra Check: ProductValidator accepts valid product ---");
        Inventory inv = new Inventory();
        Product valid = new Product("P014", "Valid Product", Category.SNACKS, new BigDecimal("35.00"), 20, 5);
        try {
            ProductValidator.validate(valid, inv);
            pass("Valid product passes ProductValidator");
        } catch (InvalidInputException e) {
            fail("Unexpected InvalidInputException on valid product: " + e.getMessage());
        }
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            pass(label);
        } else {
            fail(label);
        }
    }

    private static void pass(String label) {
        passed++;
        System.out.println("  PASS: " + label);
    }

    private static void fail(String label) {
        failed++;
        System.out.println("  FAIL: " + label);
    }
}
