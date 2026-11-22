package model;

import java.util.ArrayList;
import java.util.List;

/** A store customer with the ids of the bills linked to them. */
public class Customer {
    private final String customerId;
    private final String name;
    private final String phone;
    private final List<String> billIds;

    /** Creates a customer with no bills yet. */
    public Customer(String customerId, String name, String phone) {
        this(customerId, name, phone, new ArrayList<>());
    }

    /** Creates a customer with existing bill ids. */
    public Customer(String customerId, String name, String phone, List<String> billIds) {
        this.customerId = clean(customerId);
        this.name = clean(name);
        this.phone = clean(phone);
        this.billIds = new ArrayList<>(billIds);
    }

    /** Returns the customer id. */
    public String getCustomerId() {
        return customerId;
    }

    /** Returns the customer name. */
    public String getName() {
        return name;
    }

    /** Returns the phone number. */
    public String getPhone() {
        return phone;
    }

    /** Returns a copy of the linked bill ids. */
    public List<String> getBillIds() {
        return new ArrayList<>(billIds);
    }

    /** Links a bill id to this customer (ignores duplicates). */
    public void addBillId(String billId) {
        if (!billIds.contains(billId)) {
            billIds.add(billId);
        }
    }

    /** Converts to a CSV line: id,name,phone,billId;billId. */
    public String toCsv() {
        return customerId + "," + name + "," + phone + "," + String.join(";", billIds);
    }

    /** Parses a CSV line; throws IllegalArgumentException if malformed. */
    public static Customer fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 3 || p[0].isBlank() || p[1].isBlank()) {
            throw new IllegalArgumentException("expected id,name,phone[,billIds]");
        }
        List<String> ids = new ArrayList<>();
        if (p.length > 3 && !p[3].isBlank()) {
            for (String id : p[3].split(";")) {
                ids.add(id.trim());
            }
        }
        return new Customer(p[0], p[1], p[2], ids);
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace(",", " ").trim();
    }
}