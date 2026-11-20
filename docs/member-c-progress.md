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
