# =============================================================================
# AI Customer Service Platform - One-Click Start (Windows / PowerShell)
#
# Portable: repo can live anywhere. JDK8 / JDK17 / Maven are resolved in this
# order:  tools\env.ps1 overrides  ->  environment variables  ->  auto-detect
# (common install dirs)  ->  PATH.  Edit tools\env.ps1 on a new machine.
#
# Flow:  Infra(Nacos/RocketMQ) -> Publish Nacos Config -> Build -> Backend -> Frontend
#
# Usage (from repo root or anywhere):
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1                      # full flow
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -SkipBuild           # skip Maven build
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -InfraOnly           # infra + config only
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -SkipInfra           # backend + frontend only
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -Service ai-cs-user  # start one backend service
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -NoFrontend          # no Vite dev server
#
# Idempotent: components already listening on their ports are skipped.
# All backend/frontend processes run hidden, logs under tools\logs\.
# Stop everything with:  powershell -ExecutionPolicy Bypass -File .\tools\stop-all.ps1
# =============================================================================

param(
    [switch]$SkipBuild,      # skip `mvn clean install`
    [switch]$InfraOnly,      # start infra + publish configs, then exit
    [switch]$SkipInfra,      # assume Nacos/RocketMQ already running
    [switch]$NoFrontend,     # do not start the Vite dev server
    [string]$Service = ""    # start only one backend service, e.g. ai-cs-user
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------- base paths
$tools    = $PSScriptRoot
$root     = (Resolve-Path "$tools\..").Path
$logDir   = "$tools\logs"
$nacosBin = "$tools\nacos\bin"
$mqHome   = "$tools\rocketmq"
$mqBin    = "$mqHome\bin"
$cfgDir   = "$tools\nacos-config"
$feDir    = "$root\ai-cs-frontend"

# ------------------------------------------ machine-specific overrides
# Values here are used as PREFERRED paths; the script falls back to
# env vars (JAVA8_HOME / JAVA17_HOME / MAVEN_HOME) and auto-detection.
$JAVA8_HOME  = ""      # e.g. "D:\DevTools\jdk\jdk8u492"
$JAVA17_HOME = ""      # e.g. "D:\DevTools\jdk\jdk17.0.19"
$MAVEN_HOME  = ""      # e.g. "D:\DevTools\maven\maven3.9.16"
$DB_PASSWORD = "Yxw172707"   # remote MySQL password (${DB_PASSWORD} placeholders)

# Optional: source tools\env.ps1 if present (recommended on other machines)
$envFile = "$tools\env.ps1"
if (Test-Path -LiteralPath $envFile) { . $envFile }

# ------------------------------------------------------------------ helpers
function Test-PortListening([int]$port) {
    foreach ($addr in @([System.Net.IPAddress]::Loopback, [System.Net.IPAddress]::IPv6Loopback)) {
        $family = [System.Net.Sockets.AddressFamily]::InterNetwork
        if ($addr.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetworkV6) {
            $family = [System.Net.Sockets.AddressFamily]::InterNetworkV6
        }
        $c = New-Object System.Net.Sockets.TcpClient($family)
        try {
            $t = $c.ConnectAsync($addr, $port)
            if ($t.Wait(1200) -and $c.Connected) { return $true }
        } catch { }
        finally { $c.Dispose() }
    }
    return $false
}

function Wait-Port([int]$port, [int]$timeoutSec, [string]$name) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening $port) {
            Write-Host "  [OK]   $name ready on $port"
            return $true
        }
        Start-Sleep -Seconds 3
    }
    Write-Warning "  [WARN] $name not ready on $port after ${timeoutSec}s"
    return $false
}

function Test-JdkMajor([string]$jdkHome, [int]$major) {
    try {
        $v = (& cmd /c ('"' + $jdkHome + '\bin\java.exe" -version 2>&1') | Out-String)
        if ($major -eq 8) { return $v -match 'version "1\.8|version "8' }
        return $v -match ('version "' + $major)
    } catch { return $false }
}

function Get-JdkHome([int]$major, [string]$preferred) {
    $candidates = @()
    if ($preferred) { $candidates += $preferred }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    if ($major -eq 8 -and $env:JAVA8_HOME) { $candidates += $env:JAVA8_HOME }
    if ($major -eq 17 -and $env:JAVA17_HOME) { $candidates += $env:JAVA17_HOME }
    $base = @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium",
              "$env:LOCALAPPDATA\Programs\Eclipse Adoptium", "D:\DevTools\jdk", "C:\Java")
    foreach ($b in $base) {
        if (Test-Path -LiteralPath $b) {
            $candidates += (Get-ChildItem -LiteralPath $b -Directory -ErrorAction SilentlyContinue |
                            Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($cand in $candidates) {
        if ((Test-Path -LiteralPath "$cand\bin\java.exe") -and (Test-JdkMajor $cand $major)) {
            return $cand
        }
    }
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath) {
        $dir = Split-Path -Parent (Split-Path -Parent $onPath.Source)
        if (Test-JdkMajor $dir $major) { return $dir }
    }
    return ""
}

function Get-MavenHome([string]$preferred) {
    $candidates = @()
    if ($preferred) { $candidates += $preferred }
    if ($env:MAVEN_HOME) { $candidates += $env:MAVEN_HOME }
    if ($env:M2_HOME) { $candidates += $env:M2_HOME }
    foreach ($b in @("$env:ProgramFiles\Apache", "D:\DevTools\maven", "C:\Program Files", "C:\")) {
        if (Test-Path -LiteralPath $b) {
            $candidates += (Get-ChildItem -LiteralPath $b -Directory -ErrorAction SilentlyContinue |
                            Where-Object { $_.Name -match 'maven' } | Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($cand in $candidates) {
        if (Test-Path -LiteralPath "$cand\bin\mvn.cmd") { return $cand }
    }
    $onPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($onPath) { return (Split-Path -Parent (Split-Path -Parent $onPath.Source)) }
    return ""
}

function Test-FatJar([string]$jar) {
    if (-not (Test-Path -LiteralPath $jar)) { return $false }
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
        try {
            foreach ($e in $zip.Entries) {
                if ($e.FullName -like 'BOOT-INF/classes/*') { return $true }
            }
            return $false
        } finally { $zip.Dispose() }
    } catch { return $false }
}

function Start-Proc([string]$exe, [string[]]$args, [string]$name, [string]$workdir) {
    $out = "$logDir\$name.out.log"
    $err = "$logDir\$name.err.log"
    $p = Start-Process -FilePath $exe -ArgumentList $args -WorkingDirectory $workdir `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden -PassThru
    [IO.File]::WriteAllText("$logDir\$name.pid", "$($p.Id)")
    Write-Host "  [Start] $name PID=$($p.Id)  log=$out"
    Start-Sleep -Seconds 2
}

function Start-BackendService($svc) {
    $name = $svc.name
    $port = $svc.port
    if (Test-PortListening $port) {
        Write-Host "  [Skip] $name already running on $port"
        return
    }
    $jar = Get-ChildItem -Path "$root\$name\target" -Filter '*.jar' -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch 'sources|javadoc|original' } | Select-Object -First 1
    if ($jar -and (Test-FatJar $jar.FullName)) {
        # repackaged Spring Boot fat jar -> fast java -jar startup
        Start-Proc "$jdk17\bin\java.exe" @("-jar", $jar.FullName) $name $root
    } else {
        # thin jar / no jar -> Maven spring-boot:run
        Start-Proc "$maven\bin\mvn.cmd" @("-pl", $name, "spring-boot:run", "-DskipTests") $name $root
    }
}

# --------------------------------------------------------- resolve toolchain
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jdk8  = Get-JdkHome 8  $JAVA8_HOME
$jdk17 = Get-JdkHome 17 $JAVA17_HOME
$maven = Get-MavenHome $MAVEN_HOME
if (-not $jdk8)  { throw "JDK 8 not found. Set JAVA8_HOME in tools\env.ps1 (or install JDK 8)." }
if (-not $jdk17) { throw "JDK 17 not found. Set JAVA17_HOME in tools\env.ps1 (or install JDK 17)." }
if (-not $maven) { throw "Maven not found. Set MAVEN_HOME in tools\env.ps1 (or install Maven)." }
Write-Host "Toolchain: JDK8=$jdk8"
Write-Host "           JDK17=$jdk17"
Write-Host "           Maven=$maven"

$env:DB_PASSWORD = $DB_PASSWORD
$env:JAVA_HOME = $jdk17
$env:Path = "$jdk17\bin;$maven\bin;$env:Path"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# ------------------------------------------------------ service registry
# name -> port (dependency order, gateway last)
$services = @(
    @{ name = "ai-cs-user";      port = 8081 }
    @{ name = "ai-cs-knowledge"; port = 8082 }
    @{ name = "ai-cs-chat";      port = 8083 }
    @{ name = "ai-cs-search";    port = 8084 }
    @{ name = "ai-cs-message";   port = 8085 }
    @{ name = "ai-cs-notify";    port = 8086 }
    @{ name = "ai-cs-order";     port = 8087 }
    @{ name = "ai-cs-product";   port = 8088 }
    @{ name = "ai-cs-pay";       port = 8089 }
    @{ name = "ai-cs-mq";        port = 8090 }
    @{ name = "ai-cs-gateway";   port = 8080 }
)

# ============================================================================
# 1-3. Infra: Nacos + RocketMQ + publish configs
# ============================================================================
if ($SkipInfra) {
    Write-Host "===== [1/4] Infra skipped (-SkipInfra) ====="
} else {
    Write-Host "===== [1/6] Infra: Nacos (JDK8) ====="
    if (Test-PortListening 8848) {
        Write-Host "  [Skip] Nacos already running on 8848"
    } else {
        Start-Process cmd -ArgumentList "/c", "set JAVA_HOME=$jdk8 && cd /d $nacosBin && startup.cmd -m standalone" -WindowStyle Hidden
        Wait-Port 8848 120 "Nacos" | Out-Null
    }

    Write-Host "===== [2/6] Infra: RocketMQ NameServer + Broker (JDK17) ====="
    if (Test-PortListening 9876) {
        Write-Host "  [Skip] NameServer already running on 9876"
    } else {
        Start-Process cmd -ArgumentList "/c", "set JAVA_HOME=$jdk17 && set ROCKETMQ_HOME=$mqHome && cd /d $mqBin && mqnamesrv.cmd" -WindowStyle Hidden
        Wait-Port 9876 90 "NameServer" | Out-Null
    }
    if (Test-PortListening 10911) {
        Write-Host "  [Skip] Broker already running on 10911"
    } elseif (Test-PortListening 9876) {
        Start-Sleep -Seconds 3
        Start-Process cmd -ArgumentList "/c", "set JAVA_HOME=$jdk17 && set ROCKETMQ_HOME=$mqHome && cd /d $mqBin && mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true" -WindowStyle Hidden
        Wait-Port 10911 90 "Broker" | Out-Null
    } else {
        Write-Warning "  [WARN] NameServer not ready, Broker skipped"
    }

    Write-Host "===== [3/6] Publish Nacos configs (tenant=aics) ====="
    if (Test-PortListening 8848) {
        $ok = 0; $fail = 0
        Get-ChildItem -Path $cfgDir -Filter '*.yml' | ForEach-Object {
            # curl --data-urlencode "content@file" sends the file bytes raw (UTF-8 safe)
            $r = & curl.exe -s -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs" `
                --data-urlencode "dataId=$($_.Name)" `
                --data-urlencode "group=DEFAULT_GROUP" `
                --data-urlencode "tenant=aics" `
                --data-urlencode "type=yaml" `
                --data-urlencode "content@$($_.FullName)"
            if ("$r".Trim() -eq "true") { $ok++; Write-Host "  [OK]   $($_.Name)" }
            else { $fail++; Write-Warning "  [FAIL] $($_.Name) => $r" }
        }
        Write-Host "  Published $ok configs, $fail failed"
    } else {
        Write-Warning "  [WARN] Nacos not running, configs not published"
    }
}

if ($InfraOnly) {
    Write-Host "`n-InfraOnly done, backend services not started."
    exit 0
}

# ============================================================================
# 4. Build backend (optional)
# ============================================================================
if ($SkipBuild) {
    Write-Host "===== [4/6] Build skipped (-SkipBuild) ====="
} else {
    Write-Host "===== [4/6] Maven build (JDK17): mvn clean install -DskipTests ====="
    Push-Location $root
    try {
        & "$maven\bin\mvn.cmd" clean install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit=$LASTEXITCODE)" }
    } finally { Pop-Location }
    Write-Host "  Build OK"
}

# ============================================================================
# 5. Backend services
# ============================================================================
Write-Host "===== [5/6] Backend services ====="
if ($Service -ne "") {
    $svc = $services | Where-Object { $_.name -eq $Service }
    if (-not $svc) { throw "Unknown service: $Service (expected: $($services.name -join ', '))" }
    Start-BackendService $svc
    Wait-Port $svc.port 180 $svc.name | Out-Null
} else {
    foreach ($svc in $services) { Start-BackendService $svc }
    foreach ($svc in $services) { Wait-Port $svc.port 180 $svc.name | Out-Null }
}

# ============================================================================
# 6. Frontend (Vite dev server)
# ============================================================================
if ($NoFrontend) {
    Write-Host "===== [6/6] Frontend skipped (-NoFrontend) ====="
} else {
    Write-Host "===== [6/6] Frontend (Vite dev server) ====="
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

# ============================================================================
# Summary
# ============================================================================
Write-Host ""
Write-Host "===== Summary ====="
foreach ($svc in $services) {
    $state = if (Test-PortListening $svc.port) { "UP" } else { "DOWN" }
    Write-Host ("  {0,-16} {1}  {2}" -f $svc.name, $svc.port, $state)
}
Write-Host ("  {0,-16} {1}  {2}" -f "frontend", 5173, $(if (Test-PortListening 5173) { "UP" } else { "DOWN" }))
Write-Host ""
Write-Host "  Nacos:    http://localhost:8848/nacos  (nacos/nacos)"
Write-Host "  Gateway:  http://localhost:8080"
Write-Host "  Frontend: http://localhost:5173"
Write-Host "  Logs:     $logDir"
Write-Host ""