# =============================================================================
# Start application only: backend Java services + frontend (Vite)
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\tools\start-app.ps1                     # full (with build)
#   powershell -ExecutionPolicy Bypass -File .\tools\start-app.ps1 -SkipBuild          # use existing jars/classes
#   powershell -ExecutionPolicy Bypass -File .\tools\start-app.ps1 -NoFrontend         # backend only
#   powershell -ExecutionPolicy Bypass -File .\tools\start-app.ps1 -Service ai-cs-user # one backend service
#
# Requires Nacos/RocketMQ already running (run .\tools\start-infra.ps1 first).
# Idempotent: already-running services are skipped.
# =============================================================================

param(
    [switch]$SkipBuild,   # skip `mvn clean install`
    [switch]$NoFrontend,  # do not start the Vite dev server
    [string]$Service = "" # start only one backend service, e.g. ai-cs-user
)

. "$PSScriptRoot\lib\common.ps1"

# ------------------------------------------------------------------ build
if ($SkipBuild) {
    Write-Host "===== [1/3] Build skipped (-SkipBuild) ====="
} else {
    Write-Host "===== [1/3] Maven build (JDK17): mvn clean install -DskipTests ====="
    Push-Location $root
    try {
        & "$maven\bin\mvn.cmd" clean install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit=$LASTEXITCODE)" }
    } finally { Pop-Location }
    Write-Host "  Build OK"
}

# ------------------------------------------------------------------ backend
Write-Host "===== [2/3] Backend services ====="
if ($Service -ne "") {
    $svc = $services | Where-Object { $_.name -eq $Service }
    if (-not $svc) { throw "Unknown service: $Service (expected: $($services.name -join ', '))" }
    Start-BackendService $svc
    Wait-Port $svc.port 180 $svc.name | Out-Null
} else {
    foreach ($svc in $services) { Start-BackendService $svc }
    foreach ($svc in $services) { Wait-Port $svc.port 180 $svc.name | Out-Null }
}

# ------------------------------------------------------------------ frontend
if ($NoFrontend) {
    Write-Host "===== [3/3] Frontend skipped (-NoFrontend) ====="
} else {
    Write-Host "===== [3/3] Frontend (Vite dev server) ====="
    if (Test-PortListening 5173) {
        Write-Host "  [Skip] Frontend already running on 5173"
    } else {
        if (-not (Test-Path "$feDir\node_modules")) {
            Write-Host "  node_modules missing, running npm install..."
            Push-Location $feDir
            try { & npm install; if ($LASTEXITCODE -ne 0) { throw "npm install failed (exit=$LASTEXITCODE)" } }
            finally { Pop-Location }
        }
        Start-Proc "npm.cmd" @("run", "dev") "frontend" $feDir
        Wait-Port 5173 90 "Frontend" | Out-Null
    }
}

# ------------------------------------------------------------------ summary
Write-Host ""
Write-Host "===== Summary ====="
foreach ($svc in $services) {
    $state = if (Test-PortListening $svc.port) { "UP" } else { "DOWN" }
    Write-Host ("  {0,-16} {1}  {2}" -f $svc.name, $svc.port, $state)
}
Write-Host ("  {0,-16} {1}  {2}" -f "frontend", 5173, $(if (Test-PortListening 5173) { "UP" } else { "DOWN" }))
Write-Host ""
Write-Host "  Gateway:  http://localhost:8080"
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Logs:     $logDir"
Write-Host ""