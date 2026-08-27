# =============================================================================
# Shared helpers for start-infra.ps1 / start-app.ps1 (dot-sourced, not run directly)
#
# Loads tools\env.ps1 (machine-specific JDK/Maven/DB paths), resolves the
# toolchain, and defines port/proc helpers plus the service registry.
# =============================================================================

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------- base paths
$tools    = (Resolve-Path "$PSScriptRoot\..").Path
$root     = (Resolve-Path "$tools\..").Path
$logDir   = "$tools\logs"
$nacosBin = "$tools\nacos\bin"
$mqHome   = "$tools\rocketmq"
$mqBin    = "$mqHome\bin"
$cfgDir   = "$tools\nacos-config"
$feDir    = "$root\ai-cs-frontend"

# ------------------------------------------ machine-specific overrides
$JAVA8_HOME  = ""      # e.g. "D:\DevTools\jdk\jdk8u492"
$JAVA17_HOME = ""      # e.g. "D:\DevTools\jdk\jdk17.0.19"
$MAVEN_HOME  = ""      # e.g. "D:\DevTools\maven\maven3.9.16"
$DB_PASSWORD = "Yxw172707"

$envFile = "$tools\env.ps1"
if (Test-Path -LiteralPath $envFile) { . $envFile }
# 本机私密配置（LLM 密钥等），不入库；后加载可覆盖 env.ps1 中的非敏感默认值
$envLocalFile = "$tools\env.local.ps1"
if (Test-Path -LiteralPath $envLocalFile) { . $envLocalFile }

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

function Start-Proc([string]$exe, [string[]]$argList, [string]$name, [string]$workdir) {
    $out = "$logDir\$name.out.log"
    $err = "$logDir\$name.err.log"
    $p = Start-Process -FilePath $exe -ArgumentList $argList -WorkingDirectory $workdir `
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
        Start-Proc "$jdk17\bin\java.exe" @("-Dfile.encoding=UTF-8", "-jar", $jar.FullName) $name $root
    } else {
        Start-Proc "$maven\bin\mvn.cmd" @("-pl", $name, "spring-boot:run",
            "-Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8", "-DskipTests") $name $root
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
# LLM API keys：非空才覆盖，避免清掉机器级环境变量
if ($DEEPSEEK_API_KEY)   { $env:DEEPSEEK_API_KEY   = $DEEPSEEK_API_KEY }
if ($SILICONFLOW_API_KEY) { $env:SILICONFLOW_API_KEY = $SILICONFLOW_API_KEY }
$env:JAVA_HOME = $jdk17
$env:Path = "$jdk17\bin;$maven\bin;$env:Path"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# ------------------------------------------------------ service registry
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
