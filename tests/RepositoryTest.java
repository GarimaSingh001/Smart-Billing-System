import exception.InvalidInputException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Bill;
import model.CartItem;
import model.Category;
import model.Customer;
import model.Product;
import model.User;
import repository.BillRepository;
import repository.CustomerRepository;
import repository.ProductRepository;
import repository.UserRepository;
import service.CustomerService;
import service.SalesReport;
import ui.InvoicePrinter;

/** Plain main-method tests for Member D's repositories, reports and customer service. */
public class RepositoryTest {
    private static int passed = 0;
    private static int failed = 0;

    /** Runs all tests and prints a summary. */
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("supermart-test");
        testSaveThenReloadProducts(dir);
        testSaveThenReloadUsersAndCustomers(dir);
        testMissingFileCreated(dir);
        testMalformedLineSkipped(dir);
        testBillsAndSalesReport(dir);
        testCustomerService();
        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSaveThenReloadProducts(Path dir) throws IOException {
        ProductRepository repo = new ProductRepository(dir.resolve("p.csv").toString());
        List<Product> list = new ArrayList<>();
        list.add(new Product("P1", "Rice", Category.GROCERY, new BigDecimal("50.00"), 10, 5));
        list.add(new Product("P2", "Milk", Category.DAIRY, new BigDecimal("30.00"), 3, 5));
        repo.saveAll(list);
        List<Product> back = repo.loadAll();
        check("products: save then reload gives same count", back.size() == 2);
        check("products: reload gives identical CSV", back.get(0).toCsv().equals(list.get(0).toCsv())
                && back.get(1).toCsv().equals(list.get(1).toCsv()));
    }

    private static void testSaveThenReloadUsersAndCustomers(Path dir) throws IOException {
        UserRepository ur = new UserRepository(dir.resolve("u.csv").toString());
        List<User> users = List.of(User.fromCsv("U1,admin,abc123,ADMIN"), User.fromCsv("U2,cashier,def456,CASHIER"));
        ur.saveAll(users);
        List<User> ub = ur.loadAll();
        check("users: reload identical", ub.size() == 2 && ub.get(1).toCsv().equals(users.get(1).toCsv()));

        CustomerRepository cr = new CustomerRepository(dir.resolve("c.csv").toString());
        Customer c = new Customer("C001", "Ravi", "9876543210");
        c.addBillId("SM-0001");
        c.addBillId("SM-0002");
        cr.saveAll(List.of(c));
        Customer back = cr.loadAll().get(0);
        check("customers: bill ids survive reload", back.getBillIds().equals(List.of("SM-0001", "SM-0002")));
    }

    private static void testMissingFileCreated(Path dir) throws IOException {
        Path file = dir.resolve("sub/never-existed.csv");
        ProductRepository repo = new ProductRepository(file.toString());
        check("missing file: loads as empty list", repo.loadAll().isEmpty());
        check("missing file: file now exists", Files.exists(file));
    }

    private static void testMalformedLineSkipped(Path dir) throws IOException {
        Path file = dir.resolve("bad.csv");
        Files.writeString(file, "P1,Rice,GROCERY,50.00,10,5\nTHIS IS GARBAGE\nP3,Tea,BEVERAGE,abc,1,1\nP4,Oil,GROCERY,120.00,8,3\n");
        ProductRepository repo = new ProductRepository(file.toString());
        List<Product> loaded = repo.loadAll();
        check("malformed: valid lines still loaded", loaded.size() == 2);
        check("malformed: two warnings recorded", repo.getWarnings().size() == 2);
    }

    private static void testBillsAndSalesReport(Path dir) throws IOException {
        BillRepository repo = new BillRepository(dir.resolve("bills.csv").toString());
        Product rice = new Product("P1", "Rice", Category.GROCERY, new BigDecimal("500.00"), 10, 5);
        Product oil = new Product("P2", "Oil", Category.GROCERY, new BigDecimal("100.00"), 10, 5);
        Bill b1 = new Bill("SM-0001", LocalDateTime.of(2026, 11, 20, 10, 30), "cashier", "WALK-IN",
                List.of(new CartItem(rice, 2)), new BigDecimal("1000.00"), new BigDecimal("100.00"),
                new BigDecimal("45.00"), new BigDecimal("945.00"), "CASH");
        Bill b2 = new Bill("SM-0002", LocalDateTime.of(2026, 11, 21, 11, 0), "cashier", "C001",
                List.of(new CartItem(oil, 2), new CartItem(rice, 1)), new BigDecimal("800.00"), new BigDecimal("0.00"),
                new BigDecimal("40.00"), new BigDecimal("840.00"), "UPI");
        repo.appendBill(b1);
        repo.appendBill(b2);
        List<Bill> back = repo.loadAll();
        check("bills: two bills reloaded", back.size() == 2);
        check("bills: last invoice number is 2", repo.getLastInvoiceNumber() == 2);

        SalesReport report = new SalesReport(back);
        BigDecimal expected = b1.getTotal().add(b2.getTotal());
        check("report: bill count 2", report.getBillCount() == 2);
        check("report: total equals sum of saved bills (1785.00)",
                report.getTotalSales().compareTo(expected) == 0 && report.getTotalSales().toPlainString().equals("1785.00"));
        check("report: best seller is Rice with 3 units", report.getBestSellingProducts(1).get(0).getKey().equals("Rice")
                && report.getBestSellingProducts(1).get(0).getValue() == 3);
        check("report: two days of sales", report.getSalesPerDay().size() == 2);
        check("report: low stock finds nothing here", report.getLowStockAlerts(List.of(rice)).isEmpty());
        System.out.println("\n--- sample invoice ---");
        InvoicePrinter.print(b1);
    }

    private static void testCustomerService() {
        CustomerService service = new CustomerService(new ArrayList<>());
        try {
            Customer c = service.addCustomer("Simran", "9812345678");
            check("customer: first id is C001", c.getCustomerId().equals("C001"));
            service.linkBill("C001", "SM-0009");
            check("customer: bill linked", c.getBillIds().contains("SM-0009"));
        } catch (InvalidInputException e) {
            check("customer: unexpected error " + e.getMessage(), false);
        }
        try {
            service.addCustomer("Other", "9812345678");
            check("customer: duplicate phone rejected", false);
        } catch (InvalidInputException e) {
            check("customer: duplicate phone rejected", true);
        }
        try {
            service.addCustomer("Bad", "12ab");
            check("customer: bad phone rejected", false);
        } catch (InvalidInputException e) {
            check("customer: bad phone rejected", true);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
