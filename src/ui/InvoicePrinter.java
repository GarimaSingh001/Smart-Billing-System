package ui;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import model.Bill;
import model.CartItem;

/** Prints a bill as an aligned console invoice. */
public final class InvoicePrinter {
    private static final int WIDTH = 52;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private InvoicePrinter() {
    }

    /** Prints the invoice to the console. */
    public static void print(Bill bill) {
        System.out.print(format(bill));
    }

    /** Builds the invoice text (separate from print so it can be tested). */
    public static String format(Bill bill) {
        String line = "-".repeat(WIDTH);
        String rule = "=".repeat(WIDTH);
        StringBuilder sb = new StringBuilder();
        sb.append(rule).append('\n');
        sb.append(center("SUPERMART")).append('\n');
        sb.append(rule).append('\n');
        sb.append(String.format("Invoice : %s%n", bill.getInvoiceNo()));
        sb.append(String.format("Date    : %s%n", bill.getDateTime().format(DATE_FORMAT)));
        sb.append(String.format("Cashier : %s%n", bill.getCashierName()));
        sb.append(String.format("Customer: %s%n", bill.getCustomerId()));
        sb.append(line).append('\n');
        sb.append(String.format("%-22s %4s %10s %11s%n", "Product", "Qty", "Price", "Amount"));
        sb.append(line).append('\n');
        for (CartItem item : bill.getItems()) {
            String name = item.getProduct().getName();
            if (name.length() > 22) {
                name = name.substring(0, 21) + ".";
            }
            sb.append(String.format("%-22s %4d %10.2f %11.2f%n", name, item.getQuantity(),
                    item.getProduct().getPrice(), item.getLineAmount()));
        }
        sb.append(line).append('\n');
        sb.append(money("Subtotal", bill.getSubtotal()));
        sb.append(money("Discount", bill.getDiscount().negate()));
        sb.append(money("Tax", bill.getTax()));
        sb.append(money("TOTAL", bill.getTotal()));
        sb.append(line).append('\n');
        sb.append(String.format("Payment mode: %s%n", bill.getPaymentMode()));
        sb.append(center("Thank you for shopping with us!")).append('\n');
        sb.append(rule).append('\n');
        return sb.toString();
    }

    private static String money(String label, BigDecimal amount) {
        return String.format("%-30s %21.2f%n", label, amount);
    }

    private static String center(String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }
}
