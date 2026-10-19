# DB password: loaded from tools/env.local.ps1 or DB_PASSWORD env var (no hardcoding)
$envLocal = Join-Path $PSScriptRoot 'env.local.ps1'
if (Test-Path $envLocal) { . $envLocal }
if (-not $env:DB_PASSWORD -and $DB_PASSWORD) { $env:DB_PASSWORD = $DB_PASSWORD }
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD is not set: set the DB_PASSWORD env var or configure tools/env.local.ps1' }
$root    = "D:\Projects\Persion\ai-customer-service"
$logDir  = "$root\tools\logs"

$services = @("ai-cs-knowledge", "ai-cs-search", "ai-cs-order", "ai-cs-product")
foreach ($s in $services) {
    $out = "$logDir\$s.springboot.out.log"
    $err = "$logDir\$s.springboot.err.log"
    $p = Start-Process -FilePath "mvn.cmd" `
        -ArgumentList "-pl", $s, "spring-boot:run", "-DskipTests" `
        -WorkingDirectory $root `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden -PassThru
    Write-Output ("{0} PID={1}" -f $s, $p.Id)
}