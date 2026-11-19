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
