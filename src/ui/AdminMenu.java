package ui;

import exception.InvalidInputException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import model.Category;
import model.Product;
import model.Role;
import model.User;
import repository.BillRepository;
import service.AuditLog;
import service.AuthService;
import service.InventoryService;
import service.ProductService;
import service.SalesReport;
import util.ProductComparators;
import util.SessionMonitor;

/** Menu for the store administrator; every option only calls a service. */
public class AdminMenu extends MenuHandler {

    private static final String[][] MAIN_OPTIONS = {
        {"Manage Products", "Add, view, search, update, delete"},
        {"Manage Inventory", "Receive stock, low-stock report"},
        {"View Sales Reports", "Totals, best sellers, sales per day"},
        {"Manage Users", "Accounts, passwords, locks, audit log"},
        {"Logout", "Save and return to login"}
    };
    private static final String[][] PRODUCT_OPTIONS = {
        {"Add product", "Create a new product"},
        {"View all products", "Sorted by name, price or stock"},
        {"Search products", "By ID or by name"},
        {"Update product", "Change price or quantity"},
        {"Delete product", "Remove a product"},
        {"Back", ""}
    };
    private static final String[][] INVENTORY_OPTIONS = {
        {"Receive stock", "Add delivered quantity"},
        {"Low-stock report", "Products at or below reorder level"},
        {"Check availability", "Is a quantity in stock?"},
        {"Back", ""}
    };
    private static final String[][] USER_OPTIONS = {
        {"List users", "All accounts and lock status"},
        {"Add user", "Create admin or cashier"},
        {"Delete user", "Remove an account"},
        {"Reset password", "Set a new password for a user"},
        {"Unlock account", "After 3 failed logins"},
        {"Change my password", "For the logged-in admin"},
        {"View audit log", "Latest security events"},
        {"Back", ""}
    };

    private final AuthService authService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final BillRepository billRepository;
    private final AuditLog auditLog;

    /** Creates the admin menu with the services it needs. */
    public AdminMenu(Scanner scanner, User user, SessionMonitor monitor, AuthService authService,
                     ProductService productService, InventoryService inventoryService,
                     BillRepository billRepository, AuditLog auditLog) {
        super(scanner, user, monitor);
        this.authService = authService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.billRepository = billRepository;
        this.auditLog = auditLog;
    }

    /** Shows the main admin menu until the admin logs out. */
    @Override
    public void show() {
        System.out.println("\nWelcome, " + currentUser.getUsername() + ". Access: " + currentUser.getAccessSummary());
        boolean running = true;
        while (running) {
            printMenu(currentUser.getMenuTitle(), MAIN_OPTIONS);
            switch (readChoice(1, MAIN_OPTIONS.length)) {
                case 1: manageProducts(); break;
                case 2: manageInventory(); break;
                case 3: execute(new MenuAction() { public void run() throws Exception { viewSalesReports(); } }); break;
                case 4: manageUsers(); break;
                default: {
                    auditLog.record(currentUser.getUsername(), "Logout");
                    running = false;
                break;
                }
            }
        }
    }

    // ---------------------------------------------------------------- products

    private void manageProducts() {
        enter("Manage Products");
        boolean back = false;
        while (!back) {
            printMenu("Manage Products", PRODUCT_OPTIONS);
            switch (readChoice(1, PRODUCT_OPTIONS.length)) {
                case 1: execute(new MenuAction() { public void run() throws Exception { addProduct(); } }); break;
                case 2: execute(new MenuAction() { public void run() throws Exception { viewAllProducts(); } }); break;
                case 3: execute(new MenuAction() { public void run() throws Exception { searchProducts(); } }); break;
                case 4: execute(new MenuAction() { public void run() throws Exception { updateProduct(); } }); break;
                case 5: execute(new MenuAction() { public void run() throws Exception { deleteProduct(); } }); break;
                default: back = true; break;
            }
        }
        leave();
    }

    private void addProduct() throws Exception {
        String id = input.readNonBlank("Product ID: ").toUpperCase();
        String name = input.readNonBlank("Product name: ");
        Category category = chooseCategory();
        BigDecimal price = input.readMoney("Price (Rs): ");
        int quantity = input.readInt("Opening quantity: ", 0, 1_000_000);
        int reorderLevel = input.readInt("Reorder level: ", 0, 1_000_000);
        productService.addProduct(new Product(id, name, category, price, quantity, reorderLevel));
        auditLog.record(currentUser.getUsername(), "Product added: " + id);
        success("Product " + id + " added.");
    }

    private void viewAllProducts() throws InvalidInputException {
        System.out.println("Sort by: 1) Name  2) Price (low to high)  3) Stock (low to high)");
        Comparator<Product> order;
        switch (input.readInt("Sort option [1-3]: ", 1, 3)) {
            case 2: order = ProductComparators.BY_PRICE; break;
            case 3: order = ProductComparators.BY_STOCK; break;
            default: order = ProductComparators.BY_NAME; break;
        }
        printProducts(productService.listSorted(order));
    }

    private void searchProducts() throws Exception {
        System.out.println("Search by: 1) Product ID  2) Name contains text");
        if (input.readInt("Option [1-2]: ", 1, 2) == 1) {
            Product product = productService.searchById(input.readNonBlank("Product ID: ").toUpperCase());
            printProducts(List.of(product));
        } else {
            printProducts(productService.searchByName(input.readNonBlank("Text in name: ")));
        }
    }

    private void updateProduct() throws Exception {
        Product current = productService.searchById(input.readNonBlank("Product ID to update: ").toUpperCase());
        printProducts(List.of(current));
        BigDecimal price = input.readOptionalMoney("New price (blank = keep " + current.getPrice() + "): ");
        if (price == null) { price = current.getPrice(); }
        Integer quantity = input.readOptionalInt("New quantity (blank = keep " + current.getQuantity() + "): ",
                0, 1_000_000);
        if (quantity == null) { quantity = current.getQuantity(); }
        productService.updateProduct(current.getProductId(), price, quantity);
        auditLog.record(currentUser.getUsername(), "Product updated: " + current.getProductId());
        success("Product " + current.getProductId() + " updated.");
    }

    private void deleteProduct() throws Exception {
        String id = input.readNonBlank("Product ID to delete: ").toUpperCase();
        Product product = productService.searchById(id);
        if (confirm("Delete " + product.getName() + " (" + id + ")?")) {
            productService.deleteProduct(id);
            auditLog.record(currentUser.getUsername(), "Product deleted: " + id);
            success("Product " + id + " deleted.");
        } else {
            info("Deletion cancelled.");
        }
    }

    private Category chooseCategory() {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.println("  " + (i + 1) + ") " + categories[i]);
        }
        return categories[input.readInt("Category [1-" + categories.length + "]: ", 1, categories.length) - 1];
    }

    private void printProducts(List<Product> products) {
        if (products.isEmpty()) {
            info("No products found.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (Product p : products) {
            rows.add(new String[] {p.getProductId(), p.getName(), p.getCategory().name(),
                p.getPrice().toPlainString(), String.valueOf(p.getQuantity()),
                String.valueOf(p.getReorderLevel()), p.isLowStock() ? "LOW" : "ok"});
        }
        printTable(new String[] {"ID", "Name", "Category", "Price", "Stock", "Reorder", "Status"}, rows);
    }

    // --------------------------------------------------------------- inventory

    private void manageInventory() {
        enter("Manage Inventory");
        boolean back = false;
        while (!back) {
            printMenu("Manage Inventory", INVENTORY_OPTIONS);
            switch (readChoice(1, INVENTORY_OPTIONS.length)) {
                case 1: execute(new MenuAction() { public void run() throws Exception { receiveStock(); } }); break;
                case 2: execute(new MenuAction() { public void run() throws Exception { printLowStock(); } }); break;
                case 3: execute(new MenuAction() { public void run() throws Exception { checkAvailability(); } }); break;
                default: back = true; break;
            }
        }
        leave();
    }

    private void receiveStock() throws Exception {
        String id = input.readNonBlank("Product ID: ").toUpperCase();
        int quantity = input.readInt("Quantity received: ", 1, 1_000_000);
        inventoryService.receiveStock(id, quantity);
        auditLog.record(currentUser.getUsername(), "Stock received: " + id + " x" + quantity);
        success("Added " + quantity + " units to " + id + ".");
    }

    private void printLowStock() {
        List<Product> low = inventoryService.getLowStockProducts();
        if (low.isEmpty()) {
            success("No product is at or below its reorder level.");
        } else {
            info(low.size() + " product(s) need re-ordering:");
            printProducts(low);
        }
    }

    private void checkAvailability() throws Exception {
        String id = input.readNonBlank("Product ID: ").toUpperCase();
        int quantity = input.readPositiveInt("Quantity needed: ");
        info(inventoryService.isAvailable(id, quantity)
                ? quantity + " x " + id + " is available."
                : "Not enough stock of " + id + " for " + quantity + " units.");
    }

    // ----------------------------------------------------------------- reports

    private void viewSalesReports() throws Exception {
        SalesReport report = new SalesReport(billRepository.loadAll());
        System.out.println("\n--- SALES REPORT ---");
        System.out.println("Bills issued : " + report.getBillCount());
        System.out.println("Total sales  : Rs " + report.getTotalSales().toPlainString());

        List<Map.Entry<String, Integer>> best = report.getBestSellingProducts(5);
        System.out.println("\nTop selling products (units):");
        if (best.isEmpty()) {
            System.out.println("  no sales yet");
        }
        int rank = 1;
        for (Map.Entry<String, Integer> entry : best) {
            System.out.printf("  %d. %s - %d units%n", rank++, entry.getKey(), entry.getValue());
        }

        System.out.println("\nSales per day:");
        Map<java.time.LocalDate, BigDecimal> perDay = new TreeMap<>(report.getSalesPerDay());
        if (perDay.isEmpty()) {
            System.out.println("  no sales yet");
        }
        for (Map.Entry<java.time.LocalDate, BigDecimal> entry : perDay.entrySet()) {
            System.out.printf("  %s  Rs %s%n", entry.getKey(), entry.getValue().toPlainString());
        }
        System.out.println("\nLow-stock alert:");
        printLowStock();
    }

    // ------------------------------------------------------------------- users

    private void manageUsers() {
        enter("Manage Users");
        boolean back = false;
        while (!back) {
            printMenu("Manage Users", USER_OPTIONS);
            switch (readChoice(1, USER_OPTIONS.length)) {
                case 1: execute(new MenuAction() { public void run() throws Exception { listUsers(); } }); break;
                case 2: execute(new MenuAction() { public void run() throws Exception { addUser(); } }); break;
                case 3: execute(new MenuAction() { public void run() throws Exception { deleteUser(); } }); break;
                case 4: execute(new MenuAction() { public void run() throws Exception { resetPassword(); } }); break;
                case 5: execute(new MenuAction() { public void run() throws Exception { unlockAccount(); } }); break;
                case 6: execute(new MenuAction() { public void run() throws Exception { changeOwnPassword(); } }); break;
                case 7: execute(new MenuAction() { public void run() throws Exception { showAuditLog(); } }); break;
                default: back = true; break;
            }
        }
        leave();
    }

    private void listUsers() {
        List<String[]> rows = new ArrayList<>();
        for (User user : authService.getAllUsers()) {
            rows.add(new String[] {user.getUserId(), user.getUsername(), user.getRole().getDisplayName(),
                authService.isLocked(user.getUsername()) ? "LOCKED" : "active"});
        }
        printTable(new String[] {"ID", "Username", "Role", "Status"}, rows);
    }

    private void addUser() throws Exception {
        String username = input.readNonBlank("New username: ");
        String password = input.readPassword("Password (min 6 chars): ");
        if (!password.equals(input.readPassword("Repeat password: "))) {
            error("Passwords do not match.");
            return;
        }
        System.out.println("  1) Cashier   2) Admin");
        Role role = input.readInt("Role [1-2]: ", 1, 2) == 2 ? Role.ADMIN : Role.CASHIER;
        User created = authService.createUser(username, password, role);
        success("Created " + created + " with id " + created.getUserId() + ".");
    }

    private void deleteUser() throws Exception {
        String username = input.readNonBlank("Username to delete: ");
        if (confirm("Delete user '" + username + "'?")) {
            authService.removeUser(username, currentUser);
            success("User deleted.");
        } else {
            info("Deletion cancelled.");
        }
    }

    private void resetPassword() throws Exception {
        String username = input.readNonBlank("Username: ");
        String password = input.readPassword("New password (min 6 chars): ");
        authService.resetPassword(username, password, currentUser);
        success("Password reset for " + username + ".");
    }

    private void unlockAccount() throws Exception {
        String username = input.readNonBlank("Username to unlock: ");
        authService.unlock(username, currentUser);
        success("Account " + username + " unlocked.");
    }

    private void changeOwnPassword() throws Exception {
        String oldPassword = input.readPassword("Current password: ");
        String newPassword = input.readPassword("New password (min 6 chars): ");
        if (!newPassword.equals(input.readPassword("Repeat new password: "))) {
            error("Passwords do not match.");
            return;
        }
        authService.changePassword(currentUser, oldPassword, newPassword);
        success("Your password was changed.");
    }

    private void showAuditLog() {
        List<String> entries = authService.getAuditLog().getRecent(20);
        if (entries.isEmpty()) {
            info("The audit log is empty.");
        }
        for (String entry : entries) { System.out.println(entry); }
    }
}
