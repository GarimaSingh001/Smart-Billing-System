package service;

import exception.InvalidInputException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Customer;

/** Adds, finds and links customers. The cashier can also choose Walk-in. */
public class CustomerService {
    /** Id used by bills that are not linked to a registered customer. */
    public static final String WALK_IN_ID = "WALK-IN";

    /** The anonymous customer used when nobody is registered. */
    public static final Customer WALK_IN = new Customer(WALK_IN_ID, "Walk-in", "-");

    private final List<Customer> customers;

    /** Creates the service around a list loaded from the repository. */
    public CustomerService(List<Customer> customers) {
        this.customers = new ArrayList<>(customers);
    }

    /** Adds a customer with a generated id; rejects blank names, bad phones and duplicates. */
    public Customer addCustomer(String name, String phone) throws InvalidInputException {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Customer name cannot be blank.");
        }
        if (phone == null || !phone.trim().matches("\\d{10}")) {
            throw new InvalidInputException("Phone must be exactly 10 digits.");
        }
        if (findByPhone(phone) != null) {
            throw new InvalidInputException("A customer with phone " + phone.trim() + " already exists.");
        }
        Customer customer = new Customer(nextId(), name, phone);
        customers.add(customer);
        return customer;
    }

    /** Finds a customer by phone number, or returns null when no customer matches. */
    public Customer findByPhone(String phone) {
        String wanted = phone == null ? "" : phone.trim();
        return customers.stream().filter(c -> c.getPhone().equals(wanted)).findFirst().orElse(null);
    }

    /** Finds a customer by id; WALK-IN is always found. */
    public Optional<Customer> findById(String customerId) {
        if (WALK_IN.getCustomerId().equals(customerId)) {
            return Optional.of(WALK_IN);
        }
        return customers.stream().filter(c -> c.getCustomerId().equals(customerId)).findFirst();
    }

    /** Links a bill to a customer; walk-in bills are not stored. */
    public void linkBill(String customerId, String billId) throws InvalidInputException {
        if (WALK_IN.getCustomerId().equals(customerId)) {
            return;
        }
        Customer customer = findById(customerId)
                .orElseThrow(() -> new InvalidInputException("Unknown customer id: " + customerId));
        customer.addBillId(billId);
    }

    /** Returns a copy of all registered customers (for saving). */
    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    /** Compatibility alias for callers using the shorter method name. */
    public List<Customer> getAll() {
        return getAllCustomers();
    }

    private String nextId() {
        int max = 0;
        for (Customer c : customers) {
            try {
                max = Math.max(max, Integer.parseInt(c.getCustomerId().substring(1)));
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                // ids that do not follow C001 are skipped
            }
        }
        return String.format("C%03d", max + 1);
    }
}
