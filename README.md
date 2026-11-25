
# Smart-Billing-System

**Current syllabus limit: lectures 1-31 (Continuous Evaluation 2).** Java source
and tests use loops, explicit interface implementations and synchronized methods;
no lambdas, streams, method references, wait/notify or JDBC. CSV is retained by
agreement. See `docs/lecture-31-scope.md` for the concept map and supporting APIs.
Run `tests/check-syllabus.ps1` for the source scan.

**24 Nov integration status:** The full console application now compiles, including
ProductService, InventoryService and ProductComparators. Run `tests/run-billing.ps1`
in PowerShell for the syllabus scan, full compilation and unit/integration tests.
Then run `tests/run-menu-smoke.ps1` for real cashier/admin menu and restart checks
using isolated temporary CSV files. Start the application with
`java -cp out/billing Main`. See `docs/member-c-progress.md` for limits and details.

**25 Nov Member C scope:** 11 production types remain: CartItem, Cart, Bill,
BillingService, Payment, CashPayment, UpiPayment, Discount, PercentageDiscount,
FixedDiscount and InsufficientStockException. Tax calculation is now a method in
BillingService. The separate BillingDemo was removed; run Main for login, cart,
discounts, payment and invoice persistence. The three inventory helper classes
belong to the inventory area and are not counted as Member C types.

Java-based Supermarket POS and Billing System
# Smart-Billing-System
Java-based Supermarket POS and Billing System
=======
# SuperMart - Supermarket Billing and Inventory System

Console application in plain Java 21 (no frameworks, no database). Two roles, **Admin** and **Cashier**, work with products, stock, bills and reports. Data is stored in CSV files and survives a restart.

## Requirements
- JDK 21 (check with `javac -version`)

## Build and run
```bash
# from the project root (the folder that contains src/ and data/)
javac -d out $(find src -name "*.java")     # Windows PowerShell: javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out Main
```
The program reads and writes `data/*.csv` relative to the folder you start it from, so always start it from the project root.

Optional: idle logout time (default 600 s): `java -Dsupermart.idleSeconds=300 -cp out Main`

## Demo logins
| Role    | Username  | Password   |
|---------|-----------|------------|
| Admin   | `admin`   | `admin123` |
| Cashier | `cashier` | `cash123`  |

Passwords are stored only as SHA-256 hashes in `data/users.csv`. An account is locked after 3 wrong passwords (until an admin unlocks it or the program restarts).

## Run the tests
```bash
java -cp out test.RunAllTests          # all login/user/security tests
java -cp out test.AuthServiceTest      # or any single test class
```

## Project structure
```
src/
  Main.java            entry point: load data, login, role menu, save on exit
  model/               User, Admin, Cashier, Role, Product, Cart, Bill, ...
  service/             AuthService, AuditLog, ProductService, BillingService, ...
  payment/             Payment, CashPayment, UpiPayment, Discount, ...
  repository/          Repository<T> and the CSV repositories
  ui/                  MenuHandler, AdminMenu, CashierMenu, ConsoleInput, InvoicePrinter
  exception/           custom checked exceptions
  util/                PasswordUtil, SessionMonitor, Inventory, ...
  test/                main-method tests (no JUnit needed)
data/                  products.csv, users.csv, customers.csv, bills.csv
```

## Features and status
Update this table honestly before the evaluation: list a feature as Done only after you have tested it.

| Feature | Status |
|---|---|
| Login for two roles, SHA-256 password hashing | Done (tested) |
| Lock account after 3 failed attempts, admin unlock | Done (tested) |
| Role-based menus (Admin / Cashier), polymorphic menu selection | Done (tested) |
| Idle-session logout (background daemon thread) | Done (tested) |
| User management: add, delete, reset password, change own password | Done (tested) |
| Audit log of logins and admin actions | Done (tested) |
| Load all data on start, save on exit / logout / Ctrl+C | Done (tested with stub repositories; re-test with real ones) |
| Product, inventory, billing, reports, customers | Owned by Members B, C, D - fill in their status |

## Pending / future scope
- JDBC with MySQL instead of CSV files (planned, not implemented)
- Salted password hashing (bcrypt/PBKDF2) instead of plain SHA-256
- Lockout state kept only in memory for one run

## Milestone log
| Date | Milestone |
|---|---|
| 17 Nov 2026 | Repository, folder layout, skeleton pushed |
| 18 Nov 2026 | Role, User, Admin, Cashier pushed (frozen contract) |
| 19-20 Nov 2026 | PasswordUtil, AuthenticationException, AuthService |
| 21-22 Nov 2026 | MenuHandler, AdminMenu, CashierMenu, Main |
| 22 Nov 2026 | Tests for passwords, users, login, lockout, threads |
| 24 Nov 2026 | Full-team integration test, screenshots |

(Edit the dates to match your real commit history.)

## Team
| Member | Package |
|---|---|
| A | users, security, menus, Main |
| B | products and inventory |
| C | cart, billing, payments |
| D | customers, persistence, reports |

