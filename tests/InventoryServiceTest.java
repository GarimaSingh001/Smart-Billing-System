import exception.InvalidInputException;
import exception.ProductNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import model.Category;
import model.Product;
import service.InventoryService;
import service.ProductService;
import util.Inventory;
import util.ProductComparators;

/** Main-method checks for catalogue CRUD, stock rules and deterministic sorting. */
public class InventoryServiceTest {
    private static int checks;

    /** Runs the service tests with no external dependencies or post-lecture-31 syntax. */
    public static void main(String[] args) throws Exception {
        Inventory inventory = new Inventory();
        ProductService catalogue = new ProductService(inventory);
        InventoryService stock = new InventoryService(inventory);
        Product milk = new Product("p1", "Milk", Category.DAIRY, new BigDecimal("20"), 5, 2);
        Product apple = new Product("P2", "apple", Category.GROCERY, new BigDecimal("10"), 2, 2);
        Product another = new Product("P3", "Apple", Category.GROCERY, new BigDecimal("10.00"), 0, 1);
        catalogue.addProduct(milk); catalogue.addProduct(apple); catalogue.addProduct(another);
        check(catalogue.searchById(" P1 ") == milk, "ID lookup ignores case and space");
        check(catalogue.searchByName(" APP ").size() == 2, "Partial name search");
        check(catalogue.searchByName("not present").isEmpty(), "Missing name returns empty results");
        rejects(new Attempt() { public void run() throws Exception { catalogue.searchByName(" "); } });
        rejects(new Attempt() { public void run() throws Exception { catalogue.addProduct(null); } });
        rejects(new Attempt() { public void run() throws Exception {
            catalogue.addProduct(new Product("P1", "Duplicate", Category.DAIRY, BigDecimal.ONE, 1, 1));
        } });
        check(inventory.getAll().size() == 3 && inventory.findById("p1") == milk, "Duplicate cannot replace product");
        catalogue.updateProduct("P1", new BigDecimal("25.555"), 4);
        check(milk.getPrice().equals(new BigDecimal("25.56")) && milk.getQuantity() == 4, "Rounded update keeps shared object");
        rejects(new Attempt() { public void run() throws Exception { catalogue.updateProduct("P1", BigDecimal.ONE, -1); } });
        check(milk.getPrice().equals(new BigDecimal("25.56")) && milk.getQuantity() == 4, "Invalid update is all-or-nothing");
        rejects(new Attempt() { public void run() throws Exception { catalogue.updateProduct("P1", new BigDecimal("0.001"), 4); } });
        rejects(new Attempt() { public void run() throws Exception { catalogue.updateProduct("P1", null, 4); } });
        check(catalogue.listSorted(ProductComparators.BY_NAME).get(0) == apple, "Name order and ID tie-break");
        check(catalogue.listSorted(ProductComparators.BY_PRICE).get(0) == apple, "Price order and ID tie-break");
        check(catalogue.listSorted(ProductComparators.BY_STOCK).get(0) == another, "Stock order");
        List<Product> independent = catalogue.listSorted(ProductComparators.BY_NAME);
        independent.clear(); check(inventory.getAll().size() == 3, "List changes cannot remove inventory");
        rejects(new Attempt() { public void run() throws Exception { catalogue.listSorted(null); } });
        check(stock.getLowStockProducts().size() == 2 && stock.getLowStockProducts().get(0) == another, "Low stock includes threshold equality");
        check(stock.isAvailable("p1", 4), "Exact stock available");
        check(!stock.isAvailable("P1", 5), "Above stock unavailable");
        rejects(new Attempt() { public void run() throws Exception { stock.isAvailable("P1", 0); } });
        rejects(new Attempt() { public void run() throws Exception { stock.receiveStock("P1", 0); } });
        rejects(new Attempt() { public void run() throws Exception { stock.receiveStock("P1", -1); } });
        stock.receiveStock("P1", 3); check(milk.getQuantity() == 7, "Received stock visible in catalogue");
        rejects(new Attempt() { public void run() throws Exception { stock.receiveStock("P1", Integer.MAX_VALUE); } });
        check(milk.getQuantity() == 7, "Overflow leaves stock unchanged");
        Product huge = new Product("P4", "Huge", Category.GROCERY, BigDecimal.ONE, Integer.MAX_VALUE, 0);
        check(ProductComparators.BY_STOCK.compare(another, huge) < 0, "Comparator handles max stock");
        catalogue.deleteProduct(" p1 "); check(!inventory.exists("P1"), "Deletion removes shared entry");
        rejects(new Attempt() { public void run() throws Exception { catalogue.deleteProduct("P1"); } });
        rejects(new Attempt() { public void run() throws Exception { catalogue.searchById("missing"); } });
        rejects(new Attempt() { public void run() throws Exception { stock.receiveStock("missing", 1); } });
        System.out.println("PASS: " + checks + " product and inventory service checks.");
    }
    private interface Attempt { void run() throws Exception; }
    private static void rejects(Attempt attempt) throws Exception {
        try { attempt.run(); }
        catch (InvalidInputException | ProductNotFoundException expected) { checks++; return; }
        throw new AssertionError("Expected invalid operation to be rejected");
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
}
