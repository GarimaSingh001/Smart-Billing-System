$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot)
try {
    # The three console entry files still require Member B's inventory services.
    $sources = Get-ChildItem src -Recurse -Filter '*.java' |
        Where-Object { $_.Name -notin @('Main.java', 'AdminMenu.java', 'CashierMenu.java') } |
        ForEach-Object { $_.FullName }
    $tests = Get-ChildItem tests -Filter '*.java' | ForEach-Object { $_.FullName }
    & javac --release 21 -d out/billing $sources $tests
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    foreach ($test in @('BillingDemoTest', 'CouponTest', 'BillingTest', 'CartItemTest', 'RepositoryTest', 'DiscountTest', 'PaymentTest', 'InsufficientStockExceptionTest', 'test.RunAllTests', 'test.ProductPart1Test')) {
        & java -cp out/billing $test
        if ($LASTEXITCODE -ne 0) { throw "Failed: $test" }
    }
} finally {
    Pop-Location
}
