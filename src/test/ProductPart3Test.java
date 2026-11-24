package test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Category;
import model.PerishableProduct;
import model.Product;
import util.ProductComparators;

/** Unit tests for Part 3 classes: PerishableProduct, ProductComparators, and polymorphic CSV parsing. */
public class ProductPart3Test {

    private static int passed = 0;
    private static int failed = 0;

    /** Runs all Part 3 unit tests and outputs the results. */
    public static void main(String[] args) {
        System.out.println("Starting Member B - Part 3 Unit Tests (PerishableProduct & Comparators)...\n");

        testCreatePerishableAndGetExpiry();
        testIsExpiredPastDate();
        testIsExpiredFutureDate();
        testPerishableToCsvSevenColumns();
        testProductFromCsvPerishable();
        testProductFromCsvNormalProduct();
        testSortByName();
        testSortByPrice();
        testSortByStock();

        System.out.println("\n-------------------------------------------");
        System.out.printf("Test Summary: %d passed, %d failed%n", passed, failed);
        System.out.println("-------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testCreatePerishableAndGetExpiry() {
        System.out.println("--- Test 1 & 2: Create PerishableProduct & getExpiryDate ---");
        LocalDate date = LocalDate.now().plusDays(30);
        PerishableProduct p = new PerishableProduct("P005", "Milk 1L", Category.DAIRY,
                new BigDecimal("66.00"), 20, 5, date);

        check("PerishableProduct created and ID matches", "P005".equals(p.getProductId()));
        check("PerishableProduct is an instance of Product", p instanceof Product);
        check("getExpiryDate() returns expected date", date.equals(p.getExpiryDate()));
    }

    private static void testIsExpiredPastDate() {
        System.out.println("\n--- Test 3: isExpired() returns true for past date ---");
        LocalDate pastDate = LocalDate.now().minusDays(3);
        PerishableProduct expired = new PerishableProduct("P006", "Expired Paneer", Category.DAIRY,
                new BigDecimal("90.00"), 10, 5, pastDate);

        check("isExpired() is true for date 3 days ago", expired.isExpired());
    }

    private static void testIsExpiredFutureDate() {
        System.out.println("\n--- Test 4: isExpired() returns false for future date ---");
        LocalDate futureDate = LocalDate.now().plusDays(7);
        PerishableProduct fresh = new PerishableProduct("P007", "Fresh Butter", Category.DAIRY,
                new BigDecimal("58.00"), 15, 5, futureDate);

        check("isExpired() is false for date 7 days in future", !fresh.isExpired());
    }

    private static void testPerishableToCsvSevenColumns() {
        System.out.println("\n--- Test 5: PerishableProduct toCsv() produces 7 columns ---");
        LocalDate expiry = LocalDate.of(2026, 12, 15);
        PerishableProduct p = new PerishableProduct("P008", "Yogurt 500g", Category.DAIRY,
                new BigDecimal("45.00"), 25, 8, expiry);

        String csv = p.toCsv();
        String expected = "P008,Yogurt 500g,DAIRY,45.00,25,8,2026-12-15";
        check("toCsv() matches expected 7-column format", expected.equals(csv));
        check("toCsv() split has exactly 7 parts", csv.split(",", -1).length == 7);
    }

    private static void testProductFromCsvPerishable() {
        System.out.println("\n--- Test 6: Product.fromCsv() reconstructs PerishableProduct from 7 columns ---");
        String line = "P005,Full Cream Milk 1L,DAIRY,66.00,60,20,2026-12-05";
        Product product = Product.fromCsv(line);

        check("Constructed object is an instance of PerishableProduct", product instanceof PerishableProduct);
        PerishableProduct perishable = (PerishableProduct) product;
        check("Product ID is P005", "P005".equals(perishable.getProductId()));
        check("Product Name matches", "Full Cream Milk 1L".equals(perishable.getName()));
        check("Category is DAIRY", Category.DAIRY == perishable.getCategory());
        check("Price is 66.00", new BigDecimal("66.00").equals(perishable.getPrice()));
        check("Quantity is 60", perishable.getQuantity() == 60);
        check("Reorder level is 20", perishable.getReorderLevel() == 20);
        check("Expiry date is 2026-12-05", LocalDate.of(2026, 12, 5).equals(perishable.getExpiryDate()));
    }

    private static void testProductFromCsvNormalProduct() {
        System.out.println("\n--- Test 7: Product.fromCsv() reconstructs normal Product from 6 columns ---");
        String line = "P001,Basmati Rice 5kg,GROCERY,520.00,40,10";
        Product product = Product.fromCsv(line);

        check("Constructed object is regular Product", !(product instanceof PerishableProduct));
        check("Product ID is P001", "P001".equals(product.getProductId()));
        check("Category is GROCERY", Category.GROCERY == product.getCategory());
        check("Price is 520.00", new BigDecimal("520.00").equals(product.getPrice()));
        check("Quantity is 40", product.getQuantity() == 40);
        check("Reorder level is 10", product.getReorderLevel() == 10);
    }

    private static void testSortByName() {
        System.out.println("\n--- Test 8: Sorting by BY_NAME ---");
        List<Product> list = new ArrayList<>();
        list.add(new Product("P003", "Sugar 1kg", Category.GROCERY, new BigDecimal("48.00"), 80, 20));
        list.add(new Product("P001", "Basmati Rice 5kg", Category.GROCERY, new BigDecimal("520.00"), 40, 10));
        list.add(new Product("P002", "Almonds 500g", Category.SNACKS, new BigDecimal("450.00"), 15, 5));

        list.sort(ProductComparators.BY_NAME);

        check("1st after name sort: Almonds 500g", "Almonds 500g".equals(list.get(0).getName()));
        check("2nd after name sort: Basmati Rice 5kg", "Basmati Rice 5kg".equals(list.get(1).getName()));
        check("3rd after name sort: Sugar 1kg", "Sugar 1kg".equals(list.get(2).getName()));
    }

    private static void testSortByPrice() {
        System.out.println("\n--- Test 9: Sorting by BY_PRICE (low to high) ---");
        List<Product> list = new ArrayList<>();
        list.add(new Product("P001", "Rice", Category.GROCERY, new BigDecimal("520.00"), 40, 10));
        list.add(new Product("P002", "Salt", Category.GROCERY, new BigDecimal("25.00"), 100, 20));
        list.add(new Product("P003", "Dal", Category.GROCERY, new BigDecimal("145.00"), 50, 15));

        list.sort(ProductComparators.BY_PRICE);

        check("Lowest price: Salt (25.00)", new BigDecimal("25.00").equals(list.get(0).getPrice()));
        check("Middle price: Dal (145.00)", new BigDecimal("145.00").equals(list.get(1).getPrice()));
        check("Highest price: Rice (520.00)", new BigDecimal("520.00").equals(list.get(2).getPrice()));
    }

    private static void testSortByStock() {
        System.out.println("\n--- Test 10: Sorting by BY_STOCK (low to high) ---");
        List<Product> list = new ArrayList<>();
        list.add(new Product("P001", "Item A", Category.GROCERY, new BigDecimal("10.00"), 75, 10));
        list.add(new Product("P002", "Item B", Category.GROCERY, new BigDecimal("10.00"), 5, 10));
        list.add(new Product("P003", "Item C", Category.GROCERY, new BigDecimal("10.00"), 30, 10));

        list.sort(ProductComparators.BY_STOCK);

        check("Lowest stock: Item B (5)", list.get(0).getQuantity() == 5);
        check("Middle stock: Item C (30)", list.get(1).getQuantity() == 30);
        check("Highest stock: Item A (75)", list.get(2).getQuantity() == 75);
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
}
