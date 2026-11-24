import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import repository.BillRepository;
import repository.ProductRepository;
import ui.BillingDemo;
import ui.ConsoleInput;
import ui.CouponMenu;

/** Tests actual console input, invoice persistence and stock after restarting the demo. */
public class BillingDemoTest {
    /** Runs success, restart and rejected-sale scenarios in temporary files. */
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("supermart-demo-");
        run(directory, "1\nBAD\nP1\nzero\n2\n\nBADCODE\nsave10\n1000\n2\n");
        BillRepository bills = new BillRepository(directory.resolve("bills.csv").toString());
        ProductRepository products = new ProductRepository(directory.resolve("products.csv").toString());
        check(bills.loadAll().size() == 1, "One persisted bill");
        check(bills.loadAll().get(0).getTotal().equals(new BigDecimal("945.00")), "Coupon total");
        check(products.loadAll().get(0).getQuantity() == 8, "Persisted stock");
        run(directory, "1\nP1\n1\n\n\n1000\n2\n");
        check(bills.getLastInvoiceNumber() == 2, "Restart continues numbering");
        check(products.loadAll().get(0).getQuantity() == 7, "Restart uses saved stock");
        run(directory, "1\nP1\n99\n\n\n100000\n2\n");
        run(directory, "1\nP1\n1\n\n\n1\n2\n");
        check(bills.loadAll().size() == 2, "Failed sales create no invoices");
        check(products.loadAll().get(0).getQuantity() == 7, "Failed sales preserve stock");
        run(directory, "1\n\n2\n");
        check(bills.loadAll().size() == 2, "Empty cart creates no invoice");
        service.CouponService coupons = CouponMenu.demoCatalogue(); coupons.deactivate("SAVE10");
        try (Scanner scanner = new Scanner("SAVE10\n\n")) {
            payment.Discount discount = CouponMenu.choose(new ConsoleInput(scanner, null), coupons);
            check(discount.apply(BigDecimal.TEN).equals(new BigDecimal("0.00")), "Inactive coupon can be cancelled");
        }
        System.out.println("PASS: 9 interactive demo and restart checks.");
    }
    private static void run(Path directory, String commands) throws Exception {
        try (Scanner scanner = new Scanner(commands)) { BillingDemo.run(new ConsoleInput(scanner, null), directory); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
