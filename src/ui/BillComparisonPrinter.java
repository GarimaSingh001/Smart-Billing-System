package ui;

import java.util.List;
import model.BillQuote;

/** Displays a coupon comparison before the cashier selects a discount and takes payment. */
public final class BillComparisonPrinter {
    private BillComparisonPrinter() { }

    /** Prints sorted estimates and the cheapest option; does not select a coupon. */
    public static void print(List<BillQuote> quotes) { System.out.print(format(quotes)); }

    /** Formats the comparison for the console or test assertions. */
    public static String format(List<BillQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) { throw new IllegalArgumentException("At least one quote is required."); }
        StringBuilder result = new StringBuilder("\nWHAT-IF BILL COMPARISON\n");
        result.append(String.format("%-32s %12s %12s %12s%n", "Option", "Discount", "Tax", "Total"));
        for (BillQuote q : quotes) {
            result.append(String.format("%-32s %12s %12s %12s%n", q.getOption(), q.getDiscount(), q.getTax(), q.getTotal()));
        }
        BillQuote best = quotes.stream().min(java.util.Comparator.comparing(BillQuote::getTotal)).orElseThrow();
        result.append("Lowest total: ").append(best.getOption()).append(" - Rs ").append(best.getTotal()).append('\n');
        result.append("Estimate only: no coupon applied, payment taken or stock reserved. Checkout rechecks prices and stock.\n");
        return result.toString();
    }
}
