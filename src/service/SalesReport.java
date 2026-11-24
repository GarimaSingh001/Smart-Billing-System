package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.Bill;
import model.CartItem;
import model.Product;

/** Summaries over a list of saved bills. */
public class SalesReport {
    private final List<Bill> bills;

    /** Creates a report over the given bills. */
    public SalesReport(List<Bill> bills) {
        this.bills = new ArrayList<>(bills);
    }

    /** Returns the number of bills. */
    public int getBillCount() {
        return bills.size();
    }

    /** Returns the sum of all bill totals. */
    public BigDecimal getTotalSales() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bill b : bills) {
            sum = sum.add(b.getTotal());
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the top N products by units sold (name to units), highest first. */
    public List<Map.Entry<String, Integer>> getBestSellingProducts(int topN) {
        Map<String, Integer> units = new HashMap<>();
        for (Bill b : bills) {
            for (CartItem item : b.getItems()) {
                String name = item.getProduct().getName();
                Integer count = units.get(name);
                units.put(name, (count == null ? 0 : count) + item.getQuantity());
            }
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(units.entrySet());
        list.sort(new java.util.Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int byUnits = Integer.compare(b.getValue(), a.getValue());
                return byUnits != 0 ? byUnits : a.getKey().compareTo(b.getKey());
            }
        });
        return new ArrayList<>(list.subList(0, Math.min(Math.max(topN, 0), list.size())));
    }

    /** Returns total sales for each day, in date order. */
    public Map<LocalDate, BigDecimal> getSalesPerDay() {
        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (Bill b : bills) {
            LocalDate day = b.getDateTime().toLocalDate();
            BigDecimal previous = perDay.get(day);
            perDay.put(day, previous == null ? b.getTotal() : previous.add(b.getTotal()));
        }
        for (Map.Entry<LocalDate, BigDecimal> entry : perDay.entrySet()) {
            entry.setValue(entry.getValue().setScale(2, RoundingMode.HALF_UP));
        }
        return perDay;
    }

    /** Returns products at or below their reorder level. */
    public List<Product> getLowStockAlerts(List<Product> products) {
        List<Product> low = new ArrayList<>();
        for (Product p : products) {
            if (p.isLowStock()) {
                low.add(p);
            }
        }
        return low;
    }
}
