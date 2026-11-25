# Syllabus scope: lectures 1-31

Updated 25 November 2026 following the team's supplied course-plan screenshots.
The user explicitly retained CSV persistence. This supersedes the earlier use of
lecture 43-44 lambdas and streams throughout the source and tests.

| Lectures | Concepts demonstrated | Examples |
|---|---|---|
| 1-5 | Classes, constructors, private fields, inheritance, interfaces and polymorphism | User/Admin/Cashier, Payment and Discount implementations, Bill |
| 6-9 | Arrays, strings, formatting, StringBuilder, wrapper classes | Menu tables, CSV fields, invoice output, nullable Integer input |
| 10-12 | Checked/unchecked exceptions, throw/throws, try/catch/finally | InvalidInputException, stock exceptions, input and repository handling |
| 13-15 | Validation, console programs, debugging and test cases | Main and CashierMenu, main-method tests, rejected input cases |
| 16-22 | Lists, maps, sets and collection traversal | Cart ArrayList, Inventory HashMap, account lockout HashSet |
| 23-25 | Comparable, Comparator and sorting | User ordering, ProductComparators, reports, explicit Comparator implementations |
| 26-29 | Thread, Runnable, start/sleep/join/interrupt, daemon threads and states | SessionMonitor and thread lifecycle tests |
| 30-31 | Shared data and synchronized methods/blocks | AuthService, SessionMonitor, synchronized test counters |

## Replacements made

- Streams became ordinary for loops and explicit accumulation/search.
- Lambdas and method references became ordinary interface implementations.
- Supplier-based billing constructors became constructors accepting the real
  Inventory or a shared Collection of products. Inventory is consulted on every
  checkout, including products added later.
- Comparator callbacks are explicit objects implementing compare.
- Atomic variables became synchronized fields/counters.
- Optional results became documented nullable objects, with all callers updated.
- Arrow switch expressions and pattern-matching instanceof became ordinary
  switches, instanceof checks and casts. Inferred var declarations were removed.
- Tests use the same restricted syntax; no assertions were removed to pass.

No wait/notify/notifyAll (lectures 32-33), JDBC/SQL (34-42), or lambda/Stream API
(43-44) is used. No new threads or artificial classes were added for marks.

## Supporting libraries retained

BigDecimal/RoundingMode preserve accurate money; date/time classes label invoices;
CSV file APIs preserve the user-approved storage plan. SHA-256 password hashing
and existing invoice text encoding are library calls, not newly taught algorithms.
The existing CSV format is retained so saved invoices remain readable. These
supporting APIs are not claimed as syllabus topics explicitly listed in the images.
Java 21 remains the toolchain; that does not require using later syllabus concepts.

## Verification and integration limits

Run `tests/check-syllabus.ps1` to scan every Java source and test for excluded
constructs. Run `tests/run-billing.ps1` for the scan, compilation and available tests.
The scan is a conservative syntax check, not a guarantee of a teacher's grading.

Pulled teammate commit 10f4aef: Inventory, ProductValidator,
ProductNotFoundException and inventory tests. Billing now has real Inventory
integration coverage, including additions after service construction.
Also pulled merge 263ff33 during the push: corrected sample CSV data and removal
of the duplicate InputHelper class. The suite was rerun after reconciliation.

ProductService, InventoryService and ProductComparators are now implemented.
Main, AdminMenu and CashierMenu compile with the real services, with no exclusions
or stubs. `tests/run-menu-smoke.ps1` starts separate application processes to test
cashier sale, saved invoice retrieval after restart, and admin product/stock/report
operations in temporary CSV files. It does not modify the project's data files.

Existing prototype limits remain: UPI is simulated; inventory/invoice file writes
do not form a crash-atomic transaction. Tests and CSV data
for retained features were preserved; obsolete coupon tests were removed with
the optional coupon/comparison classes on 25 November. Old generated build folders and obsolete demo-support copies can
be removed; source, team history and user data must remain.
