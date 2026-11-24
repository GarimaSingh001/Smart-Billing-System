package test;

import java.math.BigDecimal;
import model.Category;
import model.Product;

/** Tests for Category and Product classes (Member B - Part 1). */
public class ProductPart1Test {

    private static int passed = 0;
    private static int failed = 0;

    /** Runs all unit tests and reports results. */
    public static void main(String[] args) {
        System.out.println("Starting Member B - Part 1 Unit Tests (Product & Category)...\n");

        testProductCreationAndGetters();
        testSetPrice();
        testSetQuantity();
        testIsLowStock();
        testToCsv();
        testFromCsv();
        testInvalidInputsAndMalformedCsv();
        testCategoryTaxRates();

        System.out.println("\n-------------------------------------------");
        System.out.printf("Test Summary: %d passed, %d failed%n", passed, failed);
        System.out.println("-------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testProductCreationAndGetters() {
        System.out.println("--- Test 1 & 2: Creation and Getters ---");
        Product p = new Product("P001", "Basmati Rice 5kg", Category.GROCERY, new BigDecimal("520.00"), 40, 10);
        check("Product ID getter", "P001".equals(p.getProductId()));
        check("Product name getter", "Basmati Rice 5kg".equals(p.getName()));
        check("Product category getter", Category.GROCERY == p.getCategory());
        check("Product price getter", new BigDecimal("520.00").equals(p.getPrice()));
        check("Product quantity getter", p.getQuantity() == 40);
        check("Product reorderLevel getter", p.getReorderLevel() == 10);
    }

    private static void testSetPrice() {
        System.out.println("\n--- Test 3: setPrice() ---");
        Product p = new Product("P002", "Wheat Flour 10kg", Category.GROCERY, new BigDecimal("410.00"), 35, 10);
        p.setPrice(new BigDecimal("425.50"));
        check("Price updated to 425.50", new BigDecimal("425.50").equals(p.getPrice()));

        p.setPrice(new BigDecimal("430"));
        check("Price rounded and scaled to 2 decimals (430.00)", new BigDecimal("430.00").equals(p.getPrice()));
    }

    private static void testSetQuantity() {
        System.out.println("\n--- Test 4: setQuantity() ---");
        Product p = new Product("P003", "Sugar 1kg", Category.GROCERY, new BigDecimal("48.00"), 80, 20);
        p.setQuantity(65);
        check("Quantity updated to 65", p.getQuantity() == 65);

        p.setQuantity(0);
        check("Quantity updated to 0", p.getQuantity() == 0);
    }

    private static void testIsLowStock() {
        System.out.println("\n--- Test 5 & 6: isLowStock() ---");
        Product p = new Product("P004", "Toor Dal 1kg", Category.GROCERY, new BigDecimal("145.00"), 15, 15);
        check("isLowStock() returns true when quantity == reorderLevel", p.isLowStock());

        p.setQuantity(16);
        check("isLowStock() returns false when quantity > reorderLevel", !p.isLowStock());

        p.setQuantity(5);
        check("isLowStock() returns true when quantity < reorderLevel", p.isLowStock());
    }

    private static void testToCsv() {
        System.out.println("\n--- Test 7: toCsv() ---");
        Product p = new Product("P001", "Basmati Rice 5kg", Category.GROCERY, new BigDecimal("520.00"), 40, 10);
        String expected = "P001,Basmati Rice 5kg,GROCERY,520.00,40,10";
        check("toCsv() matches 6-column format", expected.equals(p.toCsv()));
    }

    private static void testFromCsv() {
        System.out.println("\n--- Test 8: fromCsv() ---");
        String line = "P010,Potato Chips 150g,SNACKS,40.00,70,20";
        Product p = Product.fromCsv(line);
        check("Parsed ID", "P010".equals(p.getProductId()));
        check("Parsed Name", "Potato Chips 150g".equals(p.getName()));
        check("Parsed Category", Category.SNACKS == p.getCategory());
        check("Parsed Price", new BigDecimal("40.00").equals(p.getPrice()));
        check("Parsed Quantity", p.getQuantity() == 70);
        check("Parsed ReorderLevel", p.getReorderLevel() == 20);
    }

    private static void testInvalidInputsAndMalformedCsv() {
        System.out.println("\n--- Test 9: Invalid Price and Malformed CSV Handling ---");
        // Invalid prices in constructor
        rejects("Reject null price in constructor",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "Item", Category.GROCERY, null, 10, 5);
            }
        });
        rejects("Reject negative price in constructor",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "Item", Category.GROCERY, new BigDecimal("-10.00"), 10, 5);
            }
        });
        rejects("Reject zero price in constructor",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "Item", Category.GROCERY, BigDecimal.ZERO, 10, 5);
            }
        });

        // Invalid prices in setter
        Product p = new Product("P001", "Item", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
        rejects("Reject null price in setPrice", new Runnable() {
            @Override
            public void run() {
                p.setPrice(null);
            }
        });
        rejects("Reject negative price in setPrice", new Runnable() {
            @Override
            public void run() {
                p.setPrice(new BigDecimal("-1.00"));
            }
        });
        rejects("Reject zero price in setPrice", new Runnable() {
            @Override
            public void run() {
                p.setPrice(BigDecimal.ZERO);
            }
        });

        // Invalid quantities
        rejects("Reject negative quantity in constructor",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "Item", Category.GROCERY, new BigDecimal("10.00"), -1, 5);
            }
        });
        rejects("Reject negative quantity in setQuantity", new Runnable() {
            @Override
            public void run() {
                p.setQuantity(-5);
            }
        });

        // Invalid IDs and names
        rejects("Reject null ID",
                new Runnable() {
            @Override
            public void run() {
                new Product(null, "Item", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });
        rejects("Reject blank ID",
                new Runnable() {
            @Override
            public void run() {
                new Product("   ", "Item", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });
        rejects("Reject comma in ID",
                new Runnable() {
            @Override
            public void run() {
                new Product("P,01", "Item", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });
        rejects("Reject null name",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", null, Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });
        rejects("Reject blank name",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "  ", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });
        rejects("Reject comma in name",
                new Runnable() {
            @Override
            public void run() {
                new Product("P001", "Item, A", Category.GROCERY, new BigDecimal("10.00"), 10, 5);
            }
        });

        // CSV parsing failures
        rejects("Reject null CSV line", new Runnable() {
            @Override
            public void run() {
                Product.fromCsv(null);
            }
        });
        rejects("Reject blank CSV line", new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("   ");
            }
        });
        rejects("Reject CSV with 5 columns",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,GROCERY,50.00,10");
            }
        });
        rejects("Reject CSV with 7 columns",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,GROCERY,50.00,10,5,EXTRA");
            }
        });
        rejects("Reject invalid price in CSV",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,GROCERY,abc,10,5");
            }
        });
        rejects("Reject invalid quantity in CSV",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,GROCERY,50.00,notanumber,5");
            }
        });
        rejects("Reject invalid reorder level in CSV",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,GROCERY,50.00,10,xyz");
            }
        });
        rejects("Reject unknown category in CSV",
                new Runnable() {
            @Override
            public void run() {
                Product.fromCsv("P001,Name,FURNITURE,50.00,10,5");
            }
        });
    }

    private static void testCategoryTaxRates() {
        System.out.println("\n--- Test 10: Category Sample Tax Rates ---");
        check("GROCERY tax rate is 0.00%", new BigDecimal("0.00").equals(Category.GROCERY.getDefaultTaxRate()));
        check("DAIRY tax rate is 5.00%", new BigDecimal("5.00").equals(Category.DAIRY.getDefaultTaxRate()));
        check("BEVERAGE tax rate is 12.00%", new BigDecimal("12.00").equals(Category.BEVERAGE.getDefaultTaxRate()));
        check("PERSONAL_CARE tax rate is 18.00%", new BigDecimal("18.00").equals(Category.PERSONAL_CARE.getDefaultTaxRate()));
        check("HOUSEHOLD tax rate is 18.00%", new BigDecimal("18.00").equals(Category.HOUSEHOLD.getDefaultTaxRate()));
        check("SNACKS tax rate is 5.00%", new BigDecimal("5.00").equals(Category.SNACKS.getDefaultTaxRate()));
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + label);
        } else {
            failed++;
            System.out.println("  FAIL: " + label);
        }
    }

    private static void rejects(String label, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL: " + label + " (did not throw exception)");
        } catch (IllegalArgumentException e) {
            passed++;
            System.out.println("  PASS: " + label);
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + label + " (threw unexpected " + e.getClass().getSimpleName() + ")");
        }
    }
}
