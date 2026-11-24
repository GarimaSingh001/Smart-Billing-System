package model;

import exception.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Immutable invoice with defensive copies of product details and quantities. */
public final class Bill {
    private final String invoiceNo;
    private final LocalDateTime dateTime;
    private final String cashierName;
    private final String customerId;
    private final List<CartItem> items;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal tax;
    private final BigDecimal total;
    private final String paymentMode;

    /** Creates an invoice snapshot and verifies its line subtotal and final total. */
    public Bill(String invoiceNo, LocalDateTime dateTime, String cashierName, String customerId,
                List<CartItem> items, BigDecimal subtotal, BigDecimal discount,
                BigDecimal tax, BigDecimal total, String paymentMode) {
        this.invoiceNo = required(invoiceNo);
        this.dateTime = java.util.Objects.requireNonNull(dateTime, "Missing invoice date");
        this.cashierName = required(cashierName);
        this.customerId = required(customerId);
        this.paymentMode = required(paymentMode);
        if (items == null || items.isEmpty()) { throw new IllegalArgumentException("Invoice needs items."); }
        this.items = copy(items);
        this.subtotal = money(subtotal);
        this.discount = money(discount);
        this.tax = money(tax);
        this.total = money(total);
        BigDecimal lines = this.items.stream().map(CartItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lines.compareTo(this.subtotal) != 0 || this.discount.compareTo(this.subtotal) > 0
                || this.subtotal.subtract(this.discount).add(this.tax).compareTo(this.total) != 0) {
            throw new IllegalArgumentException("Invoice totals do not match its items.");
        }
    }

    /** Returns the invoice number. */
    public String getInvoiceNo() { return invoiceNo; }
    /** Returns the invoice date and time. */
    public LocalDateTime getDateTime() { return dateTime; }
    /** Returns the cashier name. */
    public String getCashierName() { return cashierName; }
    /** Returns the customer identifier. */
    public String getCustomerId() { return customerId; }
    /** Returns defensive copies, including the products, of all invoice lines. */
    public List<CartItem> getItems() { return copy(items); }
    /** Returns the subtotal. */
    public BigDecimal getSubtotal() { return subtotal; }
    /** Returns the discount amount. */
    public BigDecimal getDiscount() { return discount; }
    /** Returns the tax amount. */
    public BigDecimal getTax() { return tax; }
    /** Returns the total payable. */
    public BigDecimal getTotal() { return total; }
    /** Returns the payment mode. */
    public String getPaymentMode() { return paymentMode; }

    /** Serializes a versioned CSV row with Base64 UTF-8 text and item fields. */
    public String toCsv() {
        List<String> lines = new ArrayList<>();
        for (CartItem item : items) {
            lines.add(encode(item.getProduct().toCsv()) + ":" + item.getQuantity());
        }
        return String.join(",", "v1", encode(invoiceNo), dateTime.toString(), encode(cashierName),
                encode(customerId), subtotal.toPlainString(), discount.toPlainString(),
                tax.toPlainString(), total.toPlainString(), encode(paymentMode), String.join(";", lines));
    }

    /** Reconstructs an invoice and rejects malformed rows or inconsistent totals. */
    public static Bill fromCsv(String line) {
        if (line == null) { throw new IllegalArgumentException("Missing invoice row."); }
        String[] fields = line.split(",", -1);
        if (fields.length != 11 || !fields[0].equals("v1")) {
            throw new IllegalArgumentException("Unsupported invoice CSV format.");
        }
        List<CartItem> items = new ArrayList<>();
        try {
            for (String value : fields[10].split(";", -1)) {
                String[] parts = value.split(":", -1);
                if (parts.length != 2) { throw new IllegalArgumentException("Invalid invoice item."); }
                items.add(new CartItem(Product.fromCsv(decode(parts[0])), Integer.parseInt(parts[1])));
            }
        } catch (InvalidInputException e) {
            throw new IllegalArgumentException("Invalid invoice quantity.", e);
        }
        return new Bill(decode(fields[1]), LocalDateTime.parse(fields[2]), decode(fields[3]),
                decode(fields[4]), items, new BigDecimal(fields[5]), new BigDecimal(fields[6]),
                new BigDecimal(fields[7]), new BigDecimal(fields[8]), decode(fields[9]));
    }

    private static List<CartItem> copy(List<CartItem> source) {
        List<CartItem> result = new ArrayList<>();
        for (CartItem item : source) {
            Product p = item.getProduct();
            Product snapshot = new Product(p.getProductId(), p.getName(), p.getCategory(),
                    p.getPrice(), p.getQuantity(), p.getReorderLevel());
            try { result.add(new CartItem(snapshot, item.getQuantity())); }
            catch (InvalidInputException e) { throw new IllegalArgumentException("Invalid invoice item.", e); }
        }
        return List.copyOf(result);
    }
    private static String required(String value) {
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Missing invoice field."); }
        return value;
    }
    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) { throw new IllegalArgumentException("Invalid invoice amount."); }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
