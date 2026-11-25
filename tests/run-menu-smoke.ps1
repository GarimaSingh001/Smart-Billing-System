$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
$classes = Join-Path $project 'out/billing'
if (!(Test-Path -LiteralPath (Join-Path $classes 'Main.class'))) { throw 'Run tests/run-billing.ps1 first.' }
$temporaryRoot = [System.IO.Path]::GetTempPath()
$runDirectory = Join-Path $temporaryRoot ('supermart-menu-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $runDirectory 'data') -Force | Out-Null
Copy-Item -Path (Join-Path $project 'data/*.csv') -Destination (Join-Path $runDirectory 'data')
# Only the isolated fixtures are changed; existing project CSV data is never touched.
$billFile = Join-Path $runDirectory 'data/bills.csv'
if (Test-Path -LiteralPath $billFile) { Remove-Item -LiteralPath $billFile }
Push-Location $runDirectory
try {
    $cashier = @('1','cashier','cash123','1','1','P005','2','4','','2','10','1','200','4','2')
    $sale = (($cashier -join "`n") | & java -cp $classes Main 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $sale.Contains('[ERROR]') -or !$sale.Contains('124.74') -or !$sale.Contains('75.26')) {
        throw "Cashier sale failed:`n$sale"
    }
    $productsFile = Join-Path $runDirectory 'data/products.csv'
    $milk = Get-Content -LiteralPath $productsFile | Where-Object { $_.StartsWith('P005,') }
    if (($milk -split ',')[4] -ne '58') { throw 'Cashier stock was not persisted.' }
    $bills = @(Get-Content -LiteralPath $billFile | Where-Object { $_ -and !$_.StartsWith('#') })
    if ($bills.Count -ne 1 -or ($bills[0] -split ',')[8] -ne '124.74') { throw 'Expected one saved invoice for 124.74.' }
    Write-Output 'PASS: cashier login, percentage discount, change, invoice save and persisted stock.'

    $history = @('1','cashier','cash123','3','SM-0001','4','2')
    $restart = (($history -join "`n") | & java -cp $classes Main 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $restart.Contains('[ERROR]') -or !$restart.Contains('SM-0001') -or !$restart.Contains('124.74')) {
        throw "Restart invoice lookup failed:`n$restart"
    }
    Write-Output 'PASS: a new application process reloads and prints the saved invoice.'

    $admin = @('1','admin','admin123','1','1','P999','Test Milk','2','10','3','2',
        '4','P999','12.50','2','6','2','2','1','P999','3','3','P999','5','4',
        '1','5','P999','y','6','3','5','2')
    $management = (($admin -join "`n") | & java -cp $classes Main 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $management.Contains('[ERROR]') -or !$management.Contains('P999') -or !$management.Contains('124.74')) {
        throw "Admin flow failed:`n$management"
    }
    if (Get-Content -LiteralPath $productsFile | Where-Object { $_.StartsWith('P999,') }) { throw 'Product deletion was not persisted.' }
    Write-Output 'PASS: admin add/update, low-stock report, receipt, availability, delete and sales report.'
} finally {
    Pop-Location
    $resolved = [System.IO.Path]::GetFullPath($runDirectory)
    $parent = [System.IO.Path]::GetFullPath($temporaryRoot).TrimEnd('\') + '\'
    if (!$resolved.StartsWith($parent, [System.StringComparison]::OrdinalIgnoreCase) -or
            !(Split-Path $resolved -Leaf).StartsWith('supermart-menu-')) { throw 'Unexpected cleanup path.' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
