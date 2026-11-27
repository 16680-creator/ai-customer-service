# One-shot audit: paper vs orchestration gap for every CDC component.
# Run from repo root:  & .\learning-docs\tools\tmp\cdc-coverage.ps1
Set-Location (Resolve-Path "$PSScriptRoot\..\..\..")

function Get-Hits([string]$path, [string[]]$include, [string]$pattern, [string]$exclude) {
    Get-ChildItem -Path $path -Recurse -Include $include -File -ErrorAction SilentlyContinue |
        Where-Object { -not $exclude -or $_.FullName -notmatch $exclude } |
        Select-String -Pattern $pattern -Encoding UTF8 |
        ForEach-Object { $_.Path.Replace((Get-Location).Path + '\', '') + ':' + $_.LineNumber }
}

$patterns = 'canal', 'binlog-format', 'binlog-row-image', 'expire-logs-days',
            'debezium', 'c-product-sync', 'ProductCdcConsumer'

# scan only ai-cs-* modules (skipping ai-cs-frontend keeps the run under a few seconds)
$javaFiles = foreach ($m in (Get-ChildItem -Directory -Filter 'ai-cs-*' | Where-Object { $_.Name -ne 'ai-cs-frontend' })) {
    Get-ChildItem $m.FullName -Recurse -Include *.java -File |
        Where-Object { $_.FullName -notmatch '\\target\\|\\src\\test\\' }
}

Write-Output ("{0,-20} {1,-7} {2,-6}  {3}" -f 'pattern', 'deploy', 'java', 'deploy hits')
Write-Output ('-' * 110)
foreach ($p in $patterns) {
    $d = @(Get-Hits 'deploy' @('*.yml', '*.yaml', '*.cnf', '*.sql', '*.properties', '*.xml') $p '')
    $j = @($javaFiles | Select-String -Pattern $p -Encoding UTF8)
    Write-Output ("{0,-20} {1,-7} {2,-6}  {3}" -f $p, $d.Count, $j.Count, (($d | Select-Object -First 6) -join ' | '))
}

Write-Output ''
Write-Output '--- orchestration files and canal hits inside each ---'
$compose = @('docker-compose.yml') + (Get-ChildItem 'deploy\docker-compose' -Filter *.yml | ForEach-Object { $_.FullName })
foreach ($f in $compose) {
    $hits = @(Select-String -Path $f -Pattern 'canal' -Encoding UTF8)
    Write-Output ("{0,-46} canal_hits={1}" -f $f.Replace((Get-Location).Path + '\', ''), $hits.Count)
}

Write-Output ''
Write-Output '--- service names declared under the services: block of each file ---'
$allSvc = @()
foreach ($f in $compose) {
    $lines = Get-Content $f -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if (-not $lines) { continue }
    $lines = $lines -split "`r?`n"
    $in = $false
    $names = @()
    foreach ($ln in $lines) {
        if ($ln -match '^services:\s*$') { $in = $true; continue }
        if ($in -and $ln -match '^[a-zA-Z]') { $in = $false }
        if ($in -and $ln -match '^  ([a-zA-Z0-9][a-zA-Z0-9_-]*):\s*$') { $names += $Matches[1] }
    }
    $allSvc += $names
    Write-Output ("{0,-46} services={1}" -f $f.Replace((Get-Location).Path + '\', ''), $names.Count)
}
$allSvc = $allSvc | Sort-Object -Unique
Write-Output ("distinct services total={0}" -f $allSvc.Count)
Write-Output ($allSvc -join ', ')
Write-Output ("services matching canal={0}" -f @($allSvc | Where-Object { $_ -match 'canal' }).Count)
