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

### Runnable billing rehearsal (24 November)

Added ui.BillingDemo, ui.CouponMenu and BillingDemoTest. The demo runs with the
real shared classes and repositories without the unavailable inventory services.
It has no authentication and is explicitly a rehearsal, not the full application.
The cashier menu now includes a demo coupon option using the same CouponMenu;
that full menu still needs integration testing once Member B's classes arrive.

After `tests/run-billing.ps1`, run `java -cp out/billing ui.BillingDemo`.
In IntelliJ use the Billing Demo run configuration. Demo products and invoices
are saved under demo-data (ignored by Git), separate from the team's data folder.
An optional first command-line argument selects another demo directory.

For the worked example: choose 1, enter P1, quantity 2, blank to finish the cart,
enter SAVE10, tender 1000, then choose 2 to exit. On a fresh demo this prints total
945.00 and change 55.00, leaving stock 8. Rerunning reloads that stock and continues
invoice numbering. LESS50 is a second sample code. Blank coupon means no discount.
Sample product prices and category tax rates are demonstration values.

Nine interactive checks cover invalid input/code retry, sale, CSV reload, numbering,
overselling, insufficient cash, empty cart and inactive coupon cancellation.
All existing tests also passed. The demo saves stock after each successful sale;
invoice and stock are still separate file writes, not a crash-atomic transaction.
A stock-write error after invoice save stops the demo and requests reconciliation.

### What-if bill comparison (24 November)

Four functional classes add an explainable coupon recommendation before payment:
- BillQuote stores immutable subtotal, discount, tax and total estimates.
- PricingService supplies the same arithmetic to previews and BillingService.
- CouponAdvisor compares no coupon with each active coupon, sorted cheapest first.
- BillComparisonPrinter displays the breakdown and recommends the lowest total.

The Billing Demo now displays this automatically before coupon entry. The cashier
menu has the same call, pending full inventory integration. No coupon is applied
automatically or combined with another. Stock, cart and invoices are untouched by
comparison; checkout revalidates stock and current prices. A preview is an estimate,
not a reserved price or stock guarantee. Inactive coupons are omitted. Equal totals
favor no coupon first, then alphabetical code order. The 26 new tests include
mixed tax rates, invalid discounts, ties, snapshot stability and quote/checkout
agreement. Existing tests still pass.

Example: a 1000.00 dairy subtotal gives 1050.00 without a coupon, 997.50 with
LESS50 and 945.00 with SAVE10. This is a concrete demonstration of interfaces,
composition, BigDecimal, immutable data and Comparator, not an AI prediction.

### Syllabus correction: through lecture 31 only (24 November)

The current implementation supersedes earlier notes about lambdas, streams and
Supplier. Replaced them with ordinary loops and interface implementations in
production code AND tests. Atomic helpers now use synchronized fields. Optional,
pattern matching, var and arrow switches were simplified too. The latest teammate
Inventory code was pulled and connected directly to BillingService. CSV and exact
money calculations are retained. See lecture-31-scope.md for the complete map.
Only InventoryService, ProductService and ProductComparators remain missing from
the full menu build; the real Inventory and its tests are now included.

### Full menu integration completed (24 November)

Implemented the three previously pending classes at the user's request:
- ProductService: add, ID/name search, sorted listing, validated price/quantity
  update and delete. Updates preserve the shared product object and validate both
  fields before changing either.
- InventoryService: receive stock, check availability and list low-stock products.
- ProductComparators: explicit Comparator implementations for name, price and
  quantity, with ID tie-breaks and no lambdas or streams.

Inventory now supports deletion and consistent case-insensitive IDs. Stock receipt
rejects integer overflow. Product rejects prices that round to zero. These changes
have 31 new service checks; the prior tests and lecture-31 scan still pass.

The full application compiles with no stubs or excluded entry files. Main is now
available as the SuperMart run configuration in IntelliJ. `tests/run-menu-smoke.ps1`
verified cashier login, a SAVE10 sale of two P005 products (total 124.74, change
75.26 from 200.00), stock saved from 60 to 58, and invoice retrieval in a second
process. An admin run verified add/update, low stock, restock, availability, delete
and sales-report operations. All checks use temporary copied data and clean it up.

This supersedes earlier missing-service notes. Prototype limitations remain:
simulated UPI, sample category tax rates, and no crash-atomic transaction spanning
the product and invoice files. Normal logout/restart persistence was tested.

### Member C class reduction (25 November)

Removed seven optional production classes at the user's request:
Coupon, BillQuote, CouponService, CouponAdvisor, PricingService, CouponMenu and
BillComparisonPrinter. Earlier coupon and what-if comparison entries above are
historical and no longer describe the current application.

BillingService now owns the subtotal, discount and total calculation and continues
to use TaxCalculator. Cart validation, exact BigDecimal money, cash/UPI payment,
stock deduction, invoice persistence and failure handling remain. Both menus offer
no discount, percentage discount or fixed discount. CashierMenu only lost the
integration calls for the removed features; no teammate class was deleted.

Removed CouponTest and CouponAdvisorTest because their features were removed.
Updated demo and full-menu tests to exercise direct discounts, including invalid
percentage input and a fixed discount capped at the subtotal. The test runner also
includes the teammate's new ProductPart3Test from commit 05bb590.

### Member C reduced to 11 types (25 November)

Removed TaxCalculator as a separate class; its validated BigDecimal calculation
is now BillingService.calculateTax. BillingTest retains the mixed-category tax
assertion. Removed the duplicate standalone BillingDemo entry point and its
obsolete BillingDemoTest. Main and the full cashier/admin smoke test remain.
The earlier demo instructions above are historical. No teammate class was removed.

Member C: CartItem, Cart, Bill, BillingService, Payment, CashPayment, UpiPayment,
Discount, PercentageDiscount, FixedDiscount, InsufficientStockException.
