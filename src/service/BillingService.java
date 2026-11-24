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
    private final java.util.function.Supplier<? extends Collection<Product>> products;
    private final TaxCalculator taxes = new TaxCalculator();
    private int lastInvoiceNumber;
    private repository.BillRepository billRepository;

    /** Uses shared Product objects and the highest invoice number loaded from disk. */
    public BillingService(Collection<Product> products, int lastInvoiceNumber) {
        this(() -> products, lastInvoiceNumber);
        java.util.Objects.requireNonNull(products, "Missing inventory");
    }

    /** Queries current inventory on each checkout, including newly added products. */
    public BillingService(java.util.function.Supplier<? extends Collection<Product>> products, int lastInvoiceNumber) {
        if (products == null || lastInvoiceNumber < 0) {
            throw new IllegalArgumentException("Invalid inventory or invoice counter.");
        }
        this.products = products;
        this.lastInvoiceNumber = lastInvoiceNumber;
    }

    /** Saves completed invoices before confirming checkout to the caller. */
    public BillingService(java.util.function.Supplier<? extends Collection<Product>> products,
                          int lastInvoiceNumber, repository.BillRepository billRepository) {
        this(products, lastInvoiceNumber);
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
        BigDecimal subtotal = lines.stream().map(CartItem::getLineAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal reduction;
        Bill bill;
        try {
            reduction = discount.apply(subtotal);
            if (reduction == null || reduction.signum() < 0 || reduction.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException("Discount must be between zero and subtotal.");
            }
            reduction = reduction.setScale(2, RoundingMode.HALF_UP);
            BigDecimal ratio = subtotal.signum() == 0 ? BigDecimal.ZERO
                    : reduction.divide(subtotal, 16, RoundingMode.HALF_UP);
            BigDecimal tax = taxes.calculateTax(lines, ratio);
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
                if (saved.stream().anyMatch(existing -> existing.getInvoiceNo().equals(bill.getInvoiceNo()))) {
                    throw new java.io.IOException("Invoice number already exists; reload invoice history.");
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
        for (Product p : products.get()) {
            if (p.getProductId().equalsIgnoreCase(id)) { return p; }
        }
        throw new InvalidInputException("Product no longer exists: " + id);
    }
}
