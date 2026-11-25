$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot)
try {
    & (Join-Path $PSScriptRoot 'check-syllabus.ps1')
    $sources = Get-ChildItem src -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName }
    $tests = Get-ChildItem tests -Filter '*.java' | ForEach-Object { $_.FullName }
    & javac --release 21 -d out/billing $sources $tests
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    foreach ($test in @('InventoryServiceTest', 'BillingTest', 'CartItemTest', 'RepositoryTest', 'DiscountTest', 'PaymentTest', 'InsufficientStockExceptionTest', 'test.RunAllTests', 'test.ProductPart1Test', 'test.InventoryPart2Test', 'test.ProductPart3Test')) {
        & java -cp out/billing $test
        if ($LASTEXITCODE -ne 0) { throw "Failed: $test" }
    }
} finally {
    Pop-Location
}
