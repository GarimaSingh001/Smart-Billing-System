$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot
# Conservative source audit: these constructs belong after lecture 31 or were
# deliberately simplified. This is not a replacement for a teacher's review.
$pattern = '->|::|\.\s*(stream|parallelStream|codePoints)\s*\(|java\.util\.(function|stream|concurrent)|java\.sql|javax\.sql|@FunctionalInterface|\bOptional(?:Int|Long|Double)?\b|\bvar\s+\w+|\.\s*(wait|notify|notifyAll)\s*\('
$findings = @()
foreach ($file in (Get-ChildItem (Join-Path $project 'src'), (Join-Path $project 'tests') -Recurse -Filter '*.java')) {
    $findings += @(Select-String -LiteralPath $file.FullName -Pattern $pattern -CaseSensitive)
}
if ($findings.Count -gt 0) {
    $findings | ForEach-Object { Write-Output "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
    throw 'Found a construct outside the agreed lecture 1-31 source policy.'
}
Write-Output 'PASS: lecture 1-31 source scan (including tests).'
