# Member C: cart and billing

## 19 November 2026

Implemented:
- `InsufficientStockException(String productId, int requested, int available)`:
  checked exception with a readable stock shortage message.
- `CartItem(Product product, int quantity)`: product reference, quantity getters
  and setter, positive-quantity validation, and line totals using BigDecimal
  with scale 2 and HALF_UP rounding.

Validation:
- Compiled with Java 21 compatibility and compiler warnings treated as errors.
- Stock exception test passed.
- CartItem checked locally for exact decimal totals, rounding, quantity updates,
  null product rejection, and zero/negative quantities. Rejected updates preserve
  the previous quantity.

Integration pending:
- Member B's `model.Product`, including `BigDecimal getPrice()`.
- Member D's checked `exception.InvalidInputException(String message)`.
- Callers must handle InvalidInputException from CartItem's constructor/setter.

The branch cannot compile as a complete application until these dependencies arrive.
Local CartItem checks used temporary stand-ins; those stand-ins and their
fixture-specific test are intentionally excluded from this repository. Add the
integration test using Member B's real Product constructor once it is available.

Run the independent stock exception test from the repository root:

```powershell
javac --release 21 -d out src/exception/InsufficientStockException.java tests/InsufficientStockExceptionTest.java
java -cp out InsufficientStockExceptionTest
```

Next: Bill contract and invoice persistence format with Member D. CartItem uses
the current Product price; finalized invoices will need stable item snapshots.
Cart, checkout, payment processing and stock reduction are not implemented yet.

## 20 November 2026

Added three production types in `payment`:
- Discount: shared interface; apply returns the discount value, not the final total.
- PercentageDiscount: validates a 0-100 percentage and calculates the discount.
- FixedDiscount: caps the discount at the subtotal so it cannot make a bill negative.

Money is normalized to two decimals using HALF_UP. Negative/null arguments and
percentages above 100 are rejected with IllegalArgumentException. These are
argument validation errors; the future UI must handle them with a readable message.
The team's checked input exception can be used at the input/service boundary.

DiscountTest has 22 checks for normal totals, zero, full discounts, caps,
fractional percentages, rounding and invalid arguments. It needs no teammate stubs.

```powershell
javac --release 21 -Xlint:all -Werror -d out src/payment/Discount.java src/payment/PercentageDiscount.java src/payment/FixedDiscount.java tests/DiscountTest.java
java -cp out DiscountTest
```

This completes the three discount types, not the whole billing module. Bill,
Cart, TaxCalculator, payments and checkout are still pending. The full repository
also still needs Product and InvalidInputException from teammates.

## 21 November 2026

Added Payment, CashPayment and UpiPayment in the payment package.

- Payment exposes pay(BigDecimal) and getMode(). Billing can hold either
  implementation through this interface (polymorphism).
- CashPayment compares tendered cash with the total and stores change only after
  success. Failed attempts reset change. Cash and totals use scale 2, HALF_UP.
- UpiPayment simulates success for a basic name@provider string with one @ and
  no internal whitespace. It does not verify an account or transfer money.
- Both reject null/negative totals by returning false. Zero totals are allowed
  for fully discounted bills. Invalid tendered cash throws IllegalArgumentException.
- These classes perform no console input, stock changes or invoice persistence.
  Checkout must inspect the boolean result before changing stock. Each checkout
  should receive a new payment object; these are not transaction records.

Run the independent payment checks:

```powershell
javac --release 21 -Xlint:all -Werror -d out src/payment/Payment.java src/payment/CashPayment.java src/payment/UpiPayment.java tests/PaymentTest.java
java -cp out PaymentTest
```

PaymentTest covers cash shortage, exact cash, change, rounding, reset after failure,
zero/invalid totals, malformed UPI IDs and calls through the Payment interface.
Bill, Cart, TaxCalculator and BillingService remain pending, along with integration
with the teammates' Product and InvalidInputException implementations.

## 24 November 2026

Implemented Cart, Bill, TaxCalculator and BillingService against the real team
Product, Category and InvalidInputException classes. CartItemTest now uses the
real Product constructor too. No temporary dependencies are needed.

Cart merges matching IDs, validates quantities, prevents integer overflow and
provides copies of its lines. Bill deep-copies products and quantities, validates
subtotal and total, and supports toCsv/fromCsv for Member D's BillRepository.
Invoice CSV v1 has 11 columns: version, Base64 invoice ID, ISO date-time, Base64
cashier, Base64 customer, subtotal, discount, tax, total, Base64 payment mode,
semicolon-separated items. Each item is Base64 Product.toCsv(), a colon, and quantity.
Base64 is encoding, not encryption. It keeps delimiters/newlines out of CSV fields.

TaxCalculator uses each category's sample percentage after proportional discount;
the discount argument is a fraction (0.10), while Category rates are percentages
(5.00). Tax is summed before rounding to cents.

BillingService looks up current products, validates all stock before modifying
any line, calculates totals, constructs a validated invoice, checks payment,
reduces stock and saves the invoice when configured with BillRepository. A failed
invoice save restores stock and keeps the cart. The two-argument constructors
support in-memory checks; Main uses the repository constructor and the cashier
menu no longer saves a duplicate invoice. Invoice numbers resume from saved bills.

Use `tests/run-billing.ps1` from PowerShell with JDK 21+ on PATH. This compiles the
available modules and runs billing, repository, payment, discount, authentication
and product tests. It deliberately excludes Main, AdminMenu and CashierMenu:
the full application cannot yet compile without Member B's Inventory,
InventoryService, ProductService and ProductComparators. These missing classes
are not replaced with stubs or claimed complete. The IntelliJ learning project
contains the tested modules and a Billing Test run configuration.

Current limits: checkout is a single-process prototype; inventory edits must not
run concurrently with checkout. Stock is persisted by the application's existing
logout/exit save, while invoices save immediately. Cross-file crash recovery is
not implemented. UPI remains simulated. Full menu integration and a restart demo
must be repeated after the missing inventory classes arrive. Coupon is optional.

RepositoryTest's old placeholder hashes were updated to real SHA-256 values, its
checked-exception declaration was corrected, and the second bill fixture's item
price was corrected to match its declared subtotal.

### Coupon support (24 November)

Added the optional Coupon model and CouponService. Coupon implements Discount,
delegating to PercentageDiscount or FixedDiscount, and rejects use while inactive.
CouponService uses a HashMap keyed by normalized codes, rejects duplicates and
unknown codes, and provides deactivation and sorted listing. CouponTest exercises
the catalogue and passes a coupon into the existing BillingService checkout.

Usage: register a Coupon with addCoupon, resolve it with findActive(code), then
pass that result as checkout's Discount argument. Codes are reusable and stored
only in memory. No coupon is automatically registered or applied. Coupon entry
in the cashier menu, persistence, expiry dates and usage limits are not implemented.
The existing missing inventory dependencies still block the full console build.
