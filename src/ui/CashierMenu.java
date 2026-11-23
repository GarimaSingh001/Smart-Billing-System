package ui;

import exception.InsufficientStockException;
import exception.InvalidInputException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import model.Bill;
import model.Cart;
import model.CartItem;
import model.Customer;
import model.Product;
import model.User;
import payment.CashPayment;
import payment.Discount;
import payment.FixedDiscount;
import payment.Payment;
import payment.PercentageDiscount;
import payment.UpiPayment;
import repository.BillRepository;
import service.AuditLog;
import service.BillingService;
import service.CustomerService;
import service.InventoryService;
import service.ProductService;
import util.SessionMonitor;

/** Menu for billing counter staff; every option only calls a service. */
public class CashierMenu extends MenuHandler {

    private static final String[][] MAIN_OPTIONS = {
        {"Create New Bill", "Scan items, discount, payment, invoice"},
        {"Search Product", "By ID or by name"},
        {"View Recent Bills", "Your latest bills"},
        {"Logout", "Return to login"}
    };
    private static final String[][] BILL_OPTIONS = {
        {"Add item", "Product ID and quantity"},
        {"Change quantity", "Edit a line in the cart"},
        {"Remove item", "Delete a line from the cart"},
        {"Checkout", "Discount, payment and invoice"},
        {"Cancel bill", "Discard this cart"}
    };
    private static final int RECENT_BILL_COUNT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final BillingService billingService;
    private final CustomerService customerService;
    private final BillRepository billRepository;
    private final AuditLog auditLog;

    /** Creates the cashier menu with the services it needs. */
    public CashierMenu(Scanner scanner, User user, SessionMonitor monitor, ProductService productService,
                       InventoryService inventoryService, BillingService billingService,
                       CustomerService customerService, BillRepository billRepository, AuditLog auditLog) {
        super(scanner, user, monitor);
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.billingService = billingService;
        this.customerService = customerService;
        this.billRepository = billRepository;
        this.auditLog = auditLog;
    }

    /** Shows the main cashier menu until the cashier logs out. */
    @Override
    public void show() {
        System.out.println("\nWelcome, " + currentUser.getUsername() + ". Access: " + currentUser.getAccessSummary());
        boolean running = true;
        while (running) {
            printMenu(currentUser.getMenuTitle(), MAIN_OPTIONS);
            switch (readChoice(1, MAIN_OPTIONS.length)) {
                case 1 -> createNewBill();
                case 2 -> execute(this::searchProduct);
                case 3 -> execute(this::viewRecentBills);
                default -> {
                    auditLog.record(currentUser.getUsername(), "Logout");
                    running = false;
                }
            }
        }
    }

    // ------------------------------------------------------------------ billing

    private void createNewBill() {
        Cart cart = new Cart();
        enter("New Bill");
        boolean finished = false;
        while (!finished) {
            printCart(cart);
            printMenu("New Bill", BILL_OPTIONS);
            switch (readChoice(1, BILL_OPTIONS.length)) {
                case 1 -> execute(() -> addItem(cart));
                case 2 -> execute(() -> changeQuantity(cart));
                case 3 -> execute(() -> removeItem(cart));
                case 4 -> {
                    boolean[] billed = {false};
                    execute(() -> billed[0] = checkout(cart));
                    finished = billed[0];
                }
                default -> {
                    if (cart.isEmpty() || confirm("Discard this bill?")) {
                        info("Bill cancelled. No stock was changed.");
                        finished = true;
                    }
                }
            }
        }
        leave();
    }

    private void addItem(Cart cart) throws Exception {
        Product product = productService.searchById(input.readNonBlank("Product ID: ").toUpperCase());
        int quantity = input.readPositiveInt("Quantity: ");
        requireStock(product, quantityInCart(cart, product.getProductId()) + quantity);
        cart.addItem(product, quantity);
        success(quantity + " x " + product.getName() + " added.");
    }

    private void changeQuantity(Cart cart) throws Exception {
        if (cart.isEmpty()) {
            throw new InvalidInputException("The cart is empty.");
        }
        String id = input.readNonBlank("Product ID in cart: ").toUpperCase();
        Product product = productService.searchById(id);
        int quantity = input.readPositiveInt("New quantity: ");
        requireStock(product, quantity);
        cart.changeQuantity(id, quantity);
        success("Quantity of " + product.getName() + " is now " + quantity + ".");
    }

    private void removeItem(Cart cart) throws Exception {
        if (cart.isEmpty()) {
            throw new InvalidInputException("The cart is empty.");
        }
        String id = input.readNonBlank("Product ID to remove: ").toUpperCase();
        cart.removeItem(id);
        success("Item " + id + " removed.");
    }

    private boolean checkout(Cart cart) throws Exception {
        if (cart.isEmpty()) {
            throw new InvalidInputException("Cannot check out an empty cart.");
        }
        String customerId = chooseCustomer();
        Discount discount = chooseDiscount();
        Payment payment = choosePayment();

        Bill bill = billingService.checkout(cart, currentUser.getUsername(), customerId, discount, payment);
        billRepository.appendBill(bill);
        if (!CustomerService.WALK_IN_ID.equals(customerId)) {
            customerService.linkBill(customerId, bill.getInvoiceNo());
        }
        auditLog.record(currentUser.getUsername(), "Bill " + bill.getInvoiceNo() + " total " + bill.getTotal());
        InvoicePrinter.print(bill);
        if (payment instanceof CashPayment cash && cash.getChange().signum() > 0) {
            System.out.println("Change to return: Rs " + cash.getChange().toPlainString());
        }
        cart.clear();
        return true;
    }

    private String chooseCustomer() throws Exception {
        String phone = input.readLine("Customer phone (blank = walk-in): ");
        if (phone.isEmpty()) {
            return CustomerService.WALK_IN_ID;
        }
        Customer customer = customerService.findByPhone(phone);
        if (customer == null) {
            if (!confirm("No customer with this phone. Register a new customer?")) {
                return CustomerService.WALK_IN_ID;
            }
            customer = customerService.addCustomer(input.readNonBlank("Customer name: "), phone);
            success("Customer " + customer.getName() + " registered.");
        } else {
            info("Customer: " + customer.getName());
        }
        return customer.getCustomerId();
    }

    private Discount chooseDiscount() {
        System.out.println("Discount: 1) None  2) Percentage  3) Fixed amount");
        return switch (input.readInt("Option [1-3]: ", 1, 3)) {
            case 2 -> new PercentageDiscount(input.readMoney("Discount percent: "));
            case 3 -> new FixedDiscount(input.readMoney("Discount amount (Rs): "));
            default -> new FixedDiscount(BigDecimal.ZERO.setScale(2));
        };
    }

    private Payment choosePayment() {
        System.out.println("Payment: 1) Cash  2) UPI");
        if (input.readInt("Option [1-2]: ", 1, 2) == 1) {
            return new CashPayment(input.readMoney("Cash tendered (Rs): "));
        }
        return new UpiPayment(input.readNonBlank("UPI ID (example name@bank): "));
    }

    private void requireStock(Product product, int wanted) throws InsufficientStockException {
        if (product.getQuantity() < wanted) {
            throw new InsufficientStockException(product.getProductId(), wanted, product.getQuantity());
        }
    }

    private int quantityInCart(Cart cart, String productId) {
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getProductId().equals(productId)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void printCart(Cart cart) {
        System.out.println();
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            rows.add(new String[] {item.getProduct().getProductId(), item.getProduct().getName(),
                item.getProduct().getPrice().toPlainString(), String.valueOf(item.getQuantity()),
                item.getLineAmount().toPlainString()});
        }
        printTable(new String[] {"ID", "Item", "Price", "Qty", "Amount"}, rows);
        System.out.println("Running subtotal: Rs " + cart.getSubtotal().toPlainString());
    }

    // -------------------------------------------------------- search and history

    private void searchProduct() throws Exception {
        System.out.println("Search by: 1) Product ID  2) Name contains text");
        List<Product> found;
        if (input.readInt("Option [1-2]: ", 1, 2) == 1) {
            found = List.of(productService.searchById(input.readNonBlank("Product ID: ").toUpperCase()));
        } else {
            found = productService.searchByName(input.readNonBlank("Text in name: "));
        }
        if (found.isEmpty()) {
            info("No products found.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (Product p : found) {
            rows.add(new String[] {p.getProductId(), p.getName(), p.getCategory().name(),
                p.getPrice().toPlainString(), String.valueOf(p.getQuantity())});
        }
        printTable(new String[] {"ID", "Name", "Category", "Price", "In stock"}, rows);
    }

    private void viewRecentBills() throws Exception {
        List<Bill> mine = new ArrayList<>();
        for (Bill bill : billRepository.loadAll()) {
            if (bill.getCashierName().equalsIgnoreCase(currentUser.getUsername())) {
                mine.add(bill);
            }
        }
        mine.sort(Comparator.comparing(Bill::getDateTime).reversed());
        if (mine.isEmpty()) {
            info("You have not issued any bills yet.");
            return;
        }
        List<Bill> recent = mine.subList(0, Math.min(RECENT_BILL_COUNT, mine.size()));
        List<String[]> rows = new ArrayList<>();
        for (Bill bill : recent) {
            rows.add(new String[] {bill.getInvoiceNo(), bill.getDateTime().format(DATE_FORMAT),
                bill.getCustomerId(), bill.getTotal().toPlainString(), bill.getPaymentMode()});
        }
        printTable(new String[] {"Invoice", "Date/time", "Customer", "Total", "Payment"}, rows);
        String invoiceNo = input.readLine("Invoice number to print (blank = skip): ");
        for (Bill bill : recent) {
            if (bill.getInvoiceNo().equalsIgnoreCase(invoiceNo)) {
                InvoicePrinter.print(bill);
                return;
            }
        }
        if (!invoiceNo.isEmpty()) {
            error("Invoice " + invoiceNo + " is not in your recent list.");
        }
    }
}
