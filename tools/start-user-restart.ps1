# DB password: loaded from tools/env.local.ps1 or DB_PASSWORD env var (no hardcoding)
$envLocal = Join-Path $PSScriptRoot 'env.local.ps1'
if (Test-Path $envLocal) { . $envLocal }
if (-not $env:DB_PASSWORD -and $DB_PASSWORD) { $env:DB_PASSWORD = $DB_PASSWORD }
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD is not set: set the DB_PASSWORD env var or configure tools/env.local.ps1' }
$root  = "D:\Projects\Persion\ai-customer-service"
$log   = "$root\tools\logs"

Start-Process -FilePath "mvn.cmd" `
    -ArgumentList "-pl", "ai-cs-user", "spring-boot:run", "-DskipTests" `
    -WorkingDirectory $root `
    -RedirectStandardOutput "$log\ai-cs-user.springboot.out.log" `
    -RedirectStandardError "$log\ai-cs-user.springboot.err.log" `
    -WindowStyle Hidden -PassThru | ForEach-Object { Write-Output "ai-cs-user PID=$($_.Id)" }