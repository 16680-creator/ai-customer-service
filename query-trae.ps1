$patterns = 'lf-cdn\.trae\.com\.cn|releases/stable/[0-9.]+|Trae-?Code_CN-Setup|updateUrl|downloadUrl|Setup-x64\.exe'
$roots = @(
  'D:\DevTools\Trae CN\logs',
  'D:\DevTools\Trae CN\resources\app',
  "$env:APPDATA\Trae CN\logs",
  "$env:APPDATA\Trae CN\CachedConfigurations",
  "$env:APPDATA\Trae CN\User\globalStorage",
  "$env:USERPROFILE\.trae-cn"
)

foreach ($r in $roots) {
  if (-not (Test-Path -LiteralPath $r)) { Write-Output ("SKIP (missing): {0}" -f $r); continue }
  Write-Output ("===== SEARCH: {0} =====" -f $r)
  Get-ChildItem -LiteralPath $r -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Length -lt 20MB } |
    Select-String -Pattern $patterns -ErrorAction SilentlyContinue |
    Select-Object -First 25 |
    ForEach-Object { "{0}:{1}: {2}" -f ($_.Path -replace [regex]::Escape($r), '.'), $_.LineNumber, ($_.Line.Trim() -replace '\s+', ' ') }
}
