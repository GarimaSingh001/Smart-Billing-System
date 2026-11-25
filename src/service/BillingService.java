package service;

import exception.InsufficientStockException;
import exception.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Bill;
import model.Cart;
import model.CartItem;
import model.Product;
import payment.Discount;
import payment.Payment;

/** Single-process checkout over the shared inventory products. */
public final class BillingService {
    private final Collection<Product> products;
    private final util.Inventory inventory;
    private int lastInvoiceNumber;
    private repository.BillRepository billRepository;

    /** Uses shared Product objects and the highest invoice number loaded from disk. */
    public BillingService(Collection<Product> products, int lastInvoiceNumber) {
        if (products == null || lastInvoiceNumber < 0) {
            throw new IllegalArgumentException("Invalid inventory or invoice counter.");
        }
        this.products = products;
        this.inventory = null;
        this.lastInvoiceNumber = lastInvoiceNumber;
    }

    /** Uses the team's inventory directly, including products added after construction. */
    public BillingService(util.Inventory inventory, int lastInvoiceNumber) {
        if (inventory == null || lastInvoiceNumber < 0) {
            throw new IllegalArgumentException("Invalid inventory or invoice counter.");
        }
        this.inventory = inventory;
        this.products = null;
        this.lastInvoiceNumber = lastInvoiceNumber;
    }

    /** Saves completed invoices before confirming checkout to the caller. */
    public BillingService(Collection<Product> products,
                          int lastInvoiceNumber, repository.BillRepository billRepository) {
        this(products, lastInvoiceNumber);
        this.billRepository = java.util.Objects.requireNonNull(billRepository);
    }

    /** Saves invoices while reading current products from the team's inventory. */
    public BillingService(util.Inventory inventory, int lastInvoiceNumber, repository.BillRepository billRepository) {
        this(inventory, lastInvoiceNumber);
        this.billRepository = java.util.Objects.requireNonNull(billRepository);
    }

    /** Validates every line, builds the invoice, then accepts payment and reduces stock. */
    public synchronized Bill checkout(Cart cart, String cashierName, String customerId,
                                     Discount discount, Payment payment)
            throws InvalidInputException, InsufficientStockException {
        if (cart == null || cart.isEmpty() || discount == null || payment == null) {
            throw new InvalidInputException("A non-empty cart, discount and payment are required.");
        }
        if (lastInvoiceNumber == Integer.MAX_VALUE) {
            throw new InvalidInputException("Invoice number limit reached.");
        }
        List<CartItem> lines = new ArrayList<>();
        List<Integer> before = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = find(item.getProduct().getProductId());
            if (item.getQuantity() > product.getQuantity()) {
                throw new InsufficientStockException(product.getProductId(), item.getQuantity(), product.getQuantity());
            }
            lines.add(new CartItem(product, item.getQuantity()));
            before.add(product.getQuantity());
        }
        Bill bill;
        try {
            BigDecimal subtotal = new BigDecimal("0.00");
            for (CartItem line : lines) { subtotal = subtotal.add(line.getLineAmount()); }
            BigDecimal reduction = discount.apply(subtotal);
            if (reduction == null || reduction.signum() < 0 || reduction.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("Discount must be between zero and subtotal.");
            }
            reduction = reduction.setScale(2, RoundingMode.HALF_UP);
            BigDecimal ratio = subtotal.signum() == 0 ? BigDecimal.ZERO
                    : reduction.divide(subtotal, 16, RoundingMode.HALF_UP);
            BigDecimal tax = calculateTax(lines, ratio);
            bill = new Bill(String.format(java.util.Locale.ROOT, "SM-%04d", lastInvoiceNumber + 1),
                    LocalDateTime.now(), cashierName, customerId, lines, subtotal, reduction,
                    tax, subtotal.subtract(reduction).add(tax), payment.getMode());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidInputException("Cannot build invoice: " + e.getMessage());
        }
        if (!payment.pay(bill.getTotal())) { throw new InvalidInputException("Payment failed. Stock is unchanged."); }
        try {
            for (CartItem line : lines) {
                Product p = line.getProduct();
                p.setQuantity(p.getQuantity() - line.getQuantity());
            }
            if (billRepository != null) {
                List<Bill> saved = new ArrayList<>(billRepository.loadAll());
                if (!billRepository.getWarnings().isEmpty()) {
                    throw new java.io.IOException("Invoice history contains malformed rows; repair it before checkout.");
                }
                for (Bill existing : saved) {
                    if (existing.getInvoiceNo().equals(bill.getInvoiceNo())) {
                        throw new java.io.IOException("Invoice number already exists; reload invoice history.");
                    }
                }
                saved.add(bill);
                billRepository.saveAll(saved);
            }
        } catch (java.io.IOException e) {
            for (int i = 0; i < lines.size(); i++) { lines.get(i).getProduct().setQuantity(before.get(i)); }
            throw new InvalidInputException("Invoice was not saved; stock restored. " + e.getMessage());
        } catch (RuntimeException e) {
            for (int i = 0; i < lines.size(); i++) { lines.get(i).getProduct().setQuantity(before.get(i)); }
            throw e;
        }
        lastInvoiceNumber++;
        cart.clear();
        return bill;
    }

    private Product find(String id) throws InvalidInputException {
        Collection<Product> currentProducts = inventory == null ? products : inventory.getAll();
        for (Product p : currentProducts) {
            if (p.getProductId().equalsIgnoreCase(id)) { return p; }
        }
        throw new InvalidInputException("Product no longer exists: " + id);
    }
    /** Accepts a discount fraction (0.10 means 10%) and rounds the summed tax once. */
    public static BigDecimal calculateTax(List<CartItem> items, BigDecimal discountRatio) {
        if (items == null || discountRatio == null || discountRatio.signum() < 0
                || discountRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Discount ratio must be between 0 and 1.");
        }
        BigDecimal tax = BigDecimal.ZERO;
        for (CartItem item : items) {
            if (item == null) { throw new IllegalArgumentException("Missing cart line."); }
            BigDecimal discounted = item.getLineAmount().multiply(BigDecimal.ONE.subtract(discountRatio));
            tax = tax.add(discounted.multiply(item.getProduct().getCategory()
                    .getDefaultTaxRate()).movePointLeft(2));
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }
}
