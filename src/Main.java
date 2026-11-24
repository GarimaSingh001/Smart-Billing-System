import exception.AuthenticationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import model.Customer;
import model.Product;
import model.User;
import repository.BillRepository;
import repository.CustomerRepository;
import repository.ProductRepository;
import repository.Repository;
import repository.UserRepository;
import service.AuditLog;
import service.AuthService;
import service.BillingService;
import service.CustomerService;
import service.InventoryService;
import service.ProductService;
import ui.AdminMenu;
import ui.CashierMenu;
import ui.ConsoleInput;
import ui.MenuHandler;
import util.Inventory;
import util.SessionMonitor;

/** Entry point: loads data, shows the login screen, opens the menu for the user's role and saves on exit. */
public class Main {

    private static final int IDLE_TIMEOUT_SECONDS = Integer.getInteger("supermart.idleSeconds", 600);

    private final Scanner scanner = new Scanner(System.in);
    private final ConsoleInput console = new ConsoleInput(scanner, null);
    private final AuditLog auditLog = new AuditLog(200);
    private final AtomicBoolean exitSaveDone = new AtomicBoolean(false);

    private final UserRepository userRepository = new UserRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final BillRepository billRepository = new BillRepository();

    private AuthService authService;
    private Inventory inventory;
    private ProductService productService;
    private InventoryService inventoryService;
    private BillingService billingService;
    private CustomerService customerService;

    /** Starts SuperMart. */
    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        // The hook also saves data when the window is closed or the user presses Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveOnExit, "save-on-exit"));
        try {
            loadData();
            loginLoop();
        } catch (ConsoleInput.InputClosedException e) {
            System.out.println("\n[INFO] Input closed. Shutting down.");
        } catch (IOException e) {
            System.out.println("[ERROR] Could not load invoice history: " + e.getMessage());
        } finally {
            saveOnExit();
        }
    }

    // ------------------------------------------------------------------ startup

    private void loadData() throws IOException {
        List<User> users = loadSafely("users", userRepository);
        if (users.isEmpty()) {
            users = AuthService.demoUsers();
            System.out.println("[INFO] No users found. Created demo accounts: admin/admin123 and cashier/cash123.");
        }
        List<Product> products = loadSafely("products", productRepository);
        List<Customer> customers = loadSafely("customers", customerRepository);

        authService = new AuthService(users, auditLog);
        inventory = new Inventory();
        for (Product product : products) {
            inventory.addProduct(product);
        }
        // Only this block depends on constructors owned by Members B, C and D.
        productService = new ProductService(inventory);
        inventoryService = new InventoryService(inventory);
        customerService = new CustomerService(customers);
        billingService = new BillingService(() -> inventory.getAll(), billRepository.getLastInvoiceNumber(), billRepository);
        System.out.printf("[INFO] Loaded %d user(s), %d product(s), %d customer(s).%n",
                users.size(), products.size(), customers.size());
    }

    private <T> List<T> loadSafely(String label, Repository<T> repository) {
        try {
            return new ArrayList<>(repository.loadAll());
        } catch (IOException | RuntimeException e) {
            System.out.println("[WARN] Could not load " + label + ": " + e.getMessage() + ". Starting empty.");
            return new ArrayList<>();
        }
    }

    // -------------------------------------------------------------------- login

    private void loginLoop() {
        while (true) {
            System.out.println("\n" + "=".repeat(64));
            System.out.println("            SUPERMART - Billing and Inventory System");
            System.out.println("=".repeat(64));
            System.out.println(" 1. Login");
            System.out.println(" 2. Exit");
            System.out.println("-".repeat(64));
            if (console.readInt("Enter choice [1-2]: ", 1, 2) == 2) {
                System.out.println("Goodbye!");
                return;
            }
            User user = authenticate();
            if (user != null) {
                runSession(user);
            }
        }
    }

    /** Gives the user up to MAX_FAILED_ATTEMPTS tries; returns null when the login was cancelled or failed. */
    private User authenticate() {
        for (int attempt = 1; attempt <= AuthService.MAX_FAILED_ATTEMPTS; attempt++) {
            String username = console.readLine("Username (blank to cancel): ");
            if (username.isEmpty()) {
                return null;
            }
            String password = console.readPassword("Password: ");
            try {
                return authService.login(username, password);
            } catch (AuthenticationException e) {
                System.out.println("[ERROR] " + e.getMessage());
                if (authService.isLocked(username)) {
                    return null;
                }
            }
        }
        System.out.println("[ERROR] Too many attempts. Returning to the main screen.");
        return null;
    }

    private void runSession(User user) {
        SessionMonitor monitor = new SessionMonitor(IDLE_TIMEOUT_SECONDS * 1000L, 1000L);
        monitor.start();
        try {
            MenuHandler menu = createMenu(user, monitor); // polymorphism: Admin or Cashier menu
            menu.show();
            System.out.println("[INFO] " + user.getUsername() + " logged out.");
        } catch (ConsoleInput.SessionExpiredException e) {
            auditLog.record(user.getUsername(), "Session expired");
            System.out.println("\n[WARN] Session expired after " + IDLE_TIMEOUT_SECONDS
                    + " seconds of inactivity. Please log in again.");
        } finally {
            monitor.stop();
            checkpoint();
        }
    }

    private MenuHandler createMenu(User user, SessionMonitor monitor) {
        return switch (user.getRole()) {
            case ADMIN -> new AdminMenu(scanner, user, monitor, authService, productService,
                    inventoryService, billRepository, auditLog);
            case CASHIER -> new CashierMenu(scanner, user, monitor, productService, inventoryService,
                    billingService, customerService, billRepository, auditLog);
        };
    }

    // ------------------------------------------------------------------ saving

    /** Saves quietly after every logout so little data is lost if the program is killed. */
    private void checkpoint() {
        saveAll(false);
    }

    private void saveOnExit() {
        if (exitSaveDone.compareAndSet(false, true)) {
            saveAll(true);
        }
    }

    private synchronized void saveAll(boolean announce) {
        if (authService == null) {
            return; // nothing was loaded, so there is nothing to overwrite
        }
        try {
            userRepository.saveAll(authService.getAllUsers());
            productRepository.saveAll(inventory.getAll());
            customerRepository.saveAll(customerService.getAllCustomers());
            if (announce) {
                System.out.println("[INFO] All data saved.");
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("[ERROR] Could not save data: " + e.getMessage());
        }
    }
}
