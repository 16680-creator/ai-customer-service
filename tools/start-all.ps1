# =============================================================================
# AI 智能客服平台 - 一键启动脚本（Windows / PowerShell）
#
# 一键完成：基础设施(Nacos/RocketMQ) -> 发布Nacos配置 -> 编译后端 -> 启动微服务 -> 启动前端
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1          # 全流程
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -SkipBuild # 跳过编译(已编译过)
#   powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1 -InfraOnly # 只启基础设施+发布配置
#
# 说明：
#   - 本机无 Docker，基础设施(Nacos/RocketMQ) 使用 tools/ 下的原生二进制
#   - MySQL / Redis 为远程实例(123.60.31.79)，需保持可达
#   - 所有进程均在独立 cmd 窗口运行，关闭窗口即停止对应进程
#   - 停止：手工关闭各窗口 或 使用 logs/stop-all.ps1（按 PID 停止）
# =============================================================================

param(
    [switch]$SkipBuild,          # 跳过 Maven 编译
    [switch]$InfraOnly           # 只启动基础设施 + 发布配置，不启动服务
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------- 基础路径
$root   = "D:\Code\aiCode\Daily\ai-customer-service"
$jdk21  = "D:\Tools\IT\enviroment\jdk\jdk-21.0.11+10"    # 服务编译/运行 JDK
$jdk8   = "D:\Tools\IT\enviroment\jdk\jdk8"              # Nacos/RocketMQ 所需 JDK
$nacosBin = "$root\tools\nacos\nacos\bin"
$mqHome   = "$root\tools\rocketmq\rocketmq-all-5.1.4-bin-release"
$mqBin    = "$mqHome\bin"
$cfgDir   = "$root\tools\nacos-config"

# 远程数据库密码（ai-cs-*.yml 中 ${DB_PASSWORD} 的取值）
$env:DB_PASSWORD = "Yxw172707"

# 微服务列表（按依赖顺序启动，gateway 最后）
$services = @(
    "ai-cs-user",        # 8081 用户（登录/注册，缺它会 503）
    "ai-cs-knowledge",   # 8082 知识库
    "ai-cs-chat",        # 8083 AI 对话
    "ai-cs-message",     # 8085 消息
    "ai-cs-notify",      # 8086 通知
    "ai-cs-order",       # 8087 订单
    "ai-cs-product",     # 8088 商品（商城/图片检索，缺它会 503）
    "ai-cs-pay",         # 8089 支付（下单/收银台/回调，缺它支付失败）
    "ai-cs-mq",          # 8090 MQ 管理
    "ai-cs-gateway"      # 8080 网关
)

function Open-Window([string]$title, [string]$cmdline) {
    Write-Host "  [启动] $title"
    Start-Process cmd -ArgumentList "/k", $cmdline
}

function Wait-Port([int]$port, [int]$timeoutSec = 60, [string]$name) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $c = New-Object System.Net.Sockets.TcpClient
        $t = $c.ConnectAsync("127.0.0.1", $port)
        if ($t.Wait(1500) -and $c.Connected) { $c.Dispose(); Write-Host "  [OK] $name ($port) 就绪"; return $true }
        $c.Dispose()
        Start-Sleep -Seconds 3
    }
    Write-Warning "  [超时] $name ($port) 未就绪"
    return $false
}

# ============================================================================
# 1. 启动 Nacos
# ============================================================================
Write-Host "===== [1/6] 启动 Nacos 注册/配置中心 ====="
Open-Window "aics-nacos" "set JAVA_HOME=$jdk8 && cd /d $nacosBin && startup.cmd -m standalone"
if (-not (Wait-Port 8848 90 "Nacos")) { Write-Warning "Nacos 未就绪，继续尝试……" }

# ============================================================================
# 2. 启动 RocketMQ
# ============================================================================
Write-Host "===== [2/6] 启动 RocketMQ NameServer + Broker ====="
Open-Window "aics-mq-namesrv" "set JAVA_HOME=$jdk8 && set ROCKETMQ_HOME=$mqHome && cd /d $mqBin && mqnamesrv.cmd"
if (Wait-Port 9876 60 "RocketMQ NameServer") {
    Start-Sleep -Seconds 3
    Open-Window "aics-mq-broker" "set JAVA_HOME=$jdk8 && set ROCKETMQ_HOME=$mqHome && cd /d $mqBin && mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true"
    Wait-Port 10911 90 "RocketMQ Broker" | Out-Null
} else {
    Write-Warning "NameServer 未就绪，跳过 Broker"
}

# ============================================================================
# 3. 发布 Nacos 配置
# ============================================================================
Write-Host "===== [3/6] 发布 Nacos 配置（aics 命名空间）====="
$nacos = "http://127.0.0.1:8848"; $tenant = "aics"; $group = "DEFAULT_GROUP"
$ok = 0
Get-ChildItem -Path $cfgDir -Filter *.yml | ForEach-Object {
    $body = @{ dataId = $_.Name; group = $group; tenant = $tenant; type = "yaml"; content = (Get-Content -Raw -Path $_.FullName) }
    try {
        $r = Invoke-RestMethod -Uri "$nacos/nacos/v1/cs/configs" -Method Post -Body $body -TimeoutSec 15
        if ("$r" -eq "true") { $ok++ } else { Write-Warning "    $($_.Name) => $r" }
    } catch { Write-Warning "    $($_.Name) => ERROR: $($_.Exception.Message)" }
}
Write-Host "  已发布 $ok 个配置"

if ($InfraOnly) { Write-Host "已选择 -InfraOnly，跳过服务启动。"; exit 0 }

# ============================================================================
# 4. 编译后端（可跳过）
# ============================================================================
if ($SkipBuild) {
    Write-Host "===== [4/6] 跳过编译（-SkipBuild）====="
} else {
    Write-Host "===== [4/6] Maven 编译后端（JDK 21）====="
    $env:JAVA_HOME = $jdk21
    $env:Path = "$jdk21\bin;$env:Path"
    Push-Location $root
    try {
        mvn clean install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Maven 编译失败 (exit=$LASTEXITCODE)" }
    } finally { Pop-Location }
    Write-Host "  编译完成"
}

# ============================================================================
# 5. 启动后端微服务
# ============================================================================
Write-Host "===== [5/6] 启动后端微服务 ====="
foreach ($s in $services) {
    Open-Window "aics-$s" "set JAVA_HOME=$jdk21 && set Path=$jdk21\bin;%Path% && set DB_PASSWORD=$env:DB_PASSWORD && cd /d $root && mvn -pl $s spring-boot:run"
    Start-Sleep -Seconds 2
}

# ============================================================================
# 6. 启动前端
# ============================================================================
Write-Host "===== [6/6] 启动前端 ai-cs-frontend ====="
Open-Window "aics-frontend" "cd /d $root\ai-cs-frontend && npm run dev"

Write-Host ""
Write-Host "全部启动命令已发出，各进程在独立窗口运行："
Write-Host "  - Nacos:         http://localhost:8848/nacos (nacos/nacos)"
Write-Host "  - 网关:          http://localhost:8080"
Write-Host "  - 前端:          http://localhost:5173"
Write-Host "  - 后端服务日志:  各窗口实时输出"
Write-Host ""
