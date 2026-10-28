#Requires -Version 5.1
<#
.SYNOPSIS
    AI 智能客服平台 —— 一键部署自检（部署前环境检查 + 部署后验收）

.DESCRIPTION
    覆盖交付现场最常被问的两件事：环境能不能装、装完到底通没通。
    分 7 层检查，全程只读，不启动/停止/修改任何服务，可安全用于客户环境：

      [0] 前置依赖      JDK / Maven / Node / Docker Engine / 容器编排文件
      [1] 容器运行态    docker compose 各容器 Status 与 Health
      [2] 端口连通性    全部中间件与微服务端口
      [3] 中间件协议态  Nacos / ES / MinIO / Chroma / Neo4j 真实接口探活
      [4] 微服务健康    11 个业务服务 /actuator/health
      [5] 注册与路由    Nacos 服务注册 + 配置项 + 网关 + 前端
      [6] 汇总          控制台结果表 + Markdown 验收报告（可作交付证据）

    退出码：0 = 无 FAIL（WARN/SKIP 允许）；1 = 存在 FAIL。可直接接入 CI/CD 门禁。

.PARAMETER BaseHost
    被检环境地址，默认 127.0.0.1（本机单机版）。检查远程环境时传 IP 或域名。

.PARAMETER TimeoutSec
    单项探测超时秒数，默认 3。跨机房或高延迟环境建议调到 8。

.PARAMETER SkipDeps
    跳过第 0 层（本地已确认工具链时用）。

.PARAMETER SkipInfra
    跳过第 1、2、3 层（只验收业务服务时用）。

.PARAMETER SkipServices
    跳过第 4、5 层（只做部署前环境体检时用）。

.PARAMETER ReportDir
    Markdown 报告输出目录，相对路径基于仓库根目录。默认 reports/fde。

.PARAMETER NoReport
    只输出到控制台，不写报告文件。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File deploy/scripts/check-local.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File deploy/scripts/check-local.ps1 -BaseHost 192.168.56.10 -TimeoutSec 8

.EXAMPLE
    # 只做部署前体检，不检查业务服务（服务还没启动时用）
    powershell -ExecutionPolicy Bypass -File deploy/scripts/check-local.ps1 -SkipServices
#>
[CmdletBinding()]
param(
    [string]$BaseHost = '127.0.0.1',
    [int]$TimeoutSec = 3,
    [switch]$SkipDeps,
    [switch]$SkipInfra,
    [switch]$SkipServices,
    [string]$ReportDir = 'reports/fde',
    [switch]$NoReport
)

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

# ============================================================
# 常量：与 docker-compose.yml / 各模块 application.yml 保持一致
# ============================================================
$Services = @(
    @{ Port = 8080; Code = 'ai-cs-gateway';   Name = 'API 网关' }
    @{ Port = 8081; Code = 'ai-cs-user';      Name = '用户服务' }
    @{ Port = 8082; Code = 'ai-cs-knowledge'; Name = '知识库服务' }
    @{ Port = 8083; Code = 'ai-cs-chat';      Name = 'AI 对话服务' }
    @{ Port = 8084; Code = 'ai-cs-search';    Name = '搜索服务' }
    @{ Port = 8085; Code = 'ai-cs-message';   Name = '消息服务' }
    @{ Port = 8086; Code = 'ai-cs-notify';    Name = '通知服务' }
    @{ Port = 8087; Code = 'ai-cs-order';     Name = '订单服务' }
    @{ Port = 8088; Code = 'ai-cs-product';   Name = '商品服务' }
    @{ Port = 8089; Code = 'ai-cs-pay';       Name = '支付服务' }
    @{ Port = 8090; Code = 'ai-cs-mq';        Name = 'MQ 服务' }
)

$Containers = @(
    @{ Name = 'aics-mysql';              Label = 'MySQL' }
    @{ Name = 'aics-redis';              Label = 'Redis' }
    @{ Name = 'aics-nacos';              Label = 'Nacos' }
    @{ Name = 'aics-elasticsearch';      Label = 'Elasticsearch' }
    @{ Name = 'aics-rocketmq-namesrv';   Label = 'RocketMQ Namesrv' }
    @{ Name = 'aics-rocketmq-broker';    Label = 'RocketMQ Broker' }
    @{ Name = 'minio';                   Label = 'MinIO' }
    @{ Name = 'aics-mongodb';            Label = 'MongoDB' }
    @{ Name = 'aics-chroma';             Label = 'Chroma 向量库' }
    @{ Name = 'aics-neo4j';              Label = 'Neo4j 图数据库' }
    @{ Name = 'aics-xxl-job-admin';      Label = 'XXL-Job 调度台' }
    @{ Name = 'aics-sentinel-dashboard'; Label = 'Sentinel 控制台' }
)

$Ports = @(
    @{ Port = 3306;  Label = 'MySQL' }
    @{ Port = 6379;  Label = 'Redis' }
    @{ Port = 8848;  Label = 'Nacos HTTP' }
    @{ Port = 9200;  Label = 'Elasticsearch' }
    @{ Port = 9876;  Label = 'RocketMQ Namesrv' }
    @{ Port = 10911; Label = 'RocketMQ Broker' }
    @{ Port = 9000;  Label = 'MinIO API' }
    @{ Port = 27017; Label = 'MongoDB' }
    @{ Port = 8000;  Label = 'Chroma' }
    @{ Port = 7474;  Label = 'Neo4j HTTP' }
    @{ Port = 8099;  Label = 'XXL-Job Admin' }
    @{ Port = 8858;  Label = 'Sentinel Dashboard' }
)

$NacosNs = 'aics'

# ============================================================
# 结果收集
# ============================================================
$script:Results = New-Object System.Collections.ArrayList
$script:LayerTimes = [ordered]@{}
$script:OpenPorts = @{}
$script:DockerReady = $false
$script:DockerState = 'unknown'

function Add-Result {
    param(
        [string]$Layer,
        [string]$Name,
        [ValidateSet('PASS', 'FAIL', 'WARN', 'SKIP')][string]$Status,
        [string]$Detail = '',
        [string]$Hint = ''
    )
    [void]$script:Results.Add([pscustomobject]@{
            Layer  = $Layer
            Name   = $Name
            Status = $Status
            Detail = $Detail
            Hint   = $Hint
        })

    $color = 'DarkGray'
    if ($Status -eq 'PASS') { $color = 'Green' }
    if ($Status -eq 'FAIL') { $color = 'Red' }
    if ($Status -eq 'WARN') { $color = 'Yellow' }
    Write-Host ('  [{0,-4}] {1,-30} {2}' -f $Status, $Name, $Detail) -ForegroundColor $color
}

function Add-PortDown {
    # 端口未监听时，后续协议/健康探活必然失败。收敛为 SKIP 并指向已报出的端口失败项，
    # 避免同一个根因在多个层级被重复计数（交付现场最忌讳一堆重复的"失败"掩盖真正的根因）
    param([string]$Layer, [string]$Name, [int]$Port)
    Add-Result -Layer $Layer -Name $Name -Status 'SKIP' -Detail "端口 $Port 未监听，已在上层报失败"
}

function Write-Layer {
    param([string]$Title)
    Write-Host ''
    Write-Host "--- $Title ---" -ForegroundColor Cyan
}

# ============================================================
# 探测原语
# ============================================================
function Test-TcpPortBatch {
    <#
      批量并发探测端口，返回 hashtable：端口 -> $true/$false。

      为什么不逐个探：客户现场对"未监听端口"常表现为丢包等待而非立即拒绝，
      逐个探测的耗时等于 端口数 × 超时（实测本机 23 个端口约 46 秒）。
      这里一次性发起全部连接、共享同一个截止时间，总耗时约等于单次超时。
    #>
    param([string]$TargetHost, [int[]]$PortList, [int]$Timeout)

    $map = @{}
    $probes = @()
    foreach ($p in $PortList) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $iar = $client.BeginConnect($TargetHost, $p, $null, $null)
            $probes += [pscustomobject]@{ Port = $p; Client = $client; Async = $iar }
        }
        catch {
            $map[$p] = $false
            $client.Close()
        }
    }

    $deadline = (Get-Date).AddSeconds($Timeout)
    foreach ($probe in $probes) {
        $open = $false
        try {
            $remainMs = [int][math]::Max(0, ($deadline - (Get-Date)).TotalMilliseconds)
            if ($probe.Async.AsyncWaitHandle.WaitOne($remainMs, $false)) {
                $probe.Client.EndConnect($probe.Async)
                $open = $true
            }
        }
        catch {
            $open = $false
        }
        $map[$probe.Port] = $open
        $probe.Client.Close()
    }

    return $map
}

function Test-PortOpen {
    param([int]$Port)
    # 未探测过的端口按"未知"处理并照常执行后续检查，避免漏检
    if (-not $script:OpenPorts.ContainsKey($Port)) { return $true }
    return [bool]$script:OpenPorts[$Port]
}

function Get-HttpText {
    param([string]$Url, [int]$Timeout)
    try {
        # 不用 curl：PowerShell 里 curl 是 Invoke-WebRequest 的别名，行为与 Linux curl 不同
        $resp = Invoke-WebRequest -Uri $Url -TimeoutSec $Timeout -UseBasicParsing -ErrorAction Stop
        return $resp.Content
    }
    catch {
        return $null
    }
}

function Get-HttpJson {
    param([string]$Url, [int]$Timeout)
    try {
        return Invoke-RestMethod -Uri $Url -TimeoutSec $Timeout -UseBasicParsing -ErrorAction Stop
    }
    catch {
        return $null
    }
}

function Test-DockerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        $script:DockerState = 'cli-missing'
        return $false
    }
    $null = docker info --format '{{.ServerVersion}}' 2>$null
    if ($LASTEXITCODE -eq 0) {
        $script:DockerState = 'ready'
        return $true
    }
    $script:DockerState = 'daemon-down'
    return $false
}

function Get-ContainerState {
    param([string]$ContainerName)
    $raw = (docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $ContainerName 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($raw)) { return $null }
    $parts = $raw -split '\|'
    if ($parts.Count -lt 2) { return $null }
    return [pscustomobject]@{ Status = $parts[0]; Health = $parts[1] }
}

# ============================================================
# 开始
# ============================================================
$startedAt = Get-Date
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host '==========================================================' -ForegroundColor White
Write-Host ' AI 智能客服平台 —— 一键部署自检' -ForegroundColor White
Write-Host " 目标环境: $BaseHost" -ForegroundColor White
Write-Host " 检查时间: $($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White
Write-Host ' 说明: 本脚本只读，不会改动被检环境' -ForegroundColor DarkGray
Write-Host '==========================================================' -ForegroundColor White

# ------------------------------------------------------------
# [0] 前置依赖
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipDeps) {
    Write-Layer '[0] 前置依赖（已跳过）'
    Add-Result -Layer '前置依赖' -Name '依赖检查' -Status 'SKIP' -Detail '用户指定 -SkipDeps'
}
else {
    Write-Layer '[0] 前置依赖'

    # JDK
    $javaRaw = (& java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($javaRaw)) {
        Add-Result -Layer '前置依赖' -Name 'JDK' -Status 'FAIL' -Detail 'java 命令不可用' `
            -Hint '安装 JDK 17+ 并把 JAVA_HOME\bin 加入 PATH'
    }
    else {
        $m = [regex]::Match($javaRaw, 'version "(?<v>[0-9]+)')
        $major = 0
        if ($m.Success) { $major = [int]$m.Groups['v'].Value }
        if ($major -eq 1 -and $javaRaw -match 'version "1\.(?<v2>[0-9]+)') { $major = [int]$Matches['v2'] }
        if ($major -ge 17) {
            Add-Result -Layer '前置依赖' -Name 'JDK' -Status 'PASS' -Detail "Java $major"
        }
        else {
            Add-Result -Layer '前置依赖' -Name 'JDK' -Status 'FAIL' -Detail "Java $major（要求 17+）" `
                -Hint '本项目编译与运行需 JDK 17+，实测环境为 JDK 21'
        }
    }

    # JAVA_HOME 一致性（本项是真实高频坑：PATH 指向 17，JAVA_HOME 仍指向 8）
    $javaHome = $env:JAVA_HOME
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        Add-Result -Layer '前置依赖' -Name 'JAVA_HOME' -Status 'WARN' -Detail '未设置' `
            -Hint 'Maven 依赖 JAVA_HOME 定位 JDK，未设置时可能用错版本'
    }
    else {
        Add-Result -Layer '前置依赖' -Name 'JAVA_HOME' -Status 'PASS' -Detail $javaHome
    }

    # Maven
    $mvnRaw = (& mvn -v 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        Add-Result -Layer '前置依赖' -Name 'Maven' -Status 'WARN' -Detail 'mvn 命令不可用' `
            -Hint '若使用已构建产物部署可忽略；需现场编译则安装 Maven 3.8+'
    }
    else {
        $mv = [regex]::Match($mvnRaw, 'Apache Maven (?<v>[0-9\.]+)')
        if ($mv.Success) {
            Add-Result -Layer '前置依赖' -Name 'Maven' -Status 'PASS' -Detail ('Maven ' + $mv.Groups['v'].Value)
        }
        else {
            Add-Result -Layer '前置依赖' -Name 'Maven' -Status 'PASS' -Detail '可用'
        }
    }

    # Node（前端构建/开发用）
    $nodeRaw = (& node -v 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        Add-Result -Layer '前置依赖' -Name 'Node.js' -Status 'WARN' -Detail 'node 命令不可用' `
            -Hint '仅前端开发需要；后端部署可忽略'
    }
    else {
        Add-Result -Layer '前置依赖' -Name 'Node.js' -Status 'PASS' -Detail $nodeRaw
    }

    # Docker
    $script:DockerReady = Test-DockerReady
    if ($script:DockerReady) {
        $dv = (docker --version 2>$null | Out-String).Trim()
        Add-Result -Layer '前置依赖' -Name 'Docker Engine' -Status 'PASS' -Detail $dv
    }
    elseif ($script:DockerState -eq 'cli-missing') {
        Add-Result -Layer '前置依赖' -Name 'Docker Engine' -Status 'FAIL' -Detail '未安装 Docker CLI' `
            -Hint '安装 Docker Desktop（Windows）或 docker-ce（Linux）；容器与中间件层将无法检查'
    }
    else {
        Add-Result -Layer '前置依赖' -Name 'Docker Engine' -Status 'FAIL' -Detail 'Docker 守护进程未运行' `
            -Hint 'CLI 已安装但连不上守护进程：Windows 启动 Docker Desktop；Linux 执行 systemctl start docker'
    }

    # 编排文件
    $composeFile = Join-Path $repoRoot 'docker-compose.yml'
    if (Test-Path $composeFile) {
        Add-Result -Layer '前置依赖' -Name 'docker-compose.yml' -Status 'PASS' -Detail '存在'
    }
    else {
        Add-Result -Layer '前置依赖' -Name 'docker-compose.yml' -Status 'FAIL' -Detail "未找到 $composeFile" `
            -Hint '请在仓库根目录执行，或确认部署包完整性'
    }
}
$sw.Stop()
$script:LayerTimes['前置依赖'] = $sw.Elapsed.TotalSeconds

# ------------------------------------------------------------
# [1] 容器运行态
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipInfra) {
    Write-Layer '[1] 容器运行态（已跳过）'
}
else {
    Write-Layer '[1] 容器运行态（docker compose）'

    if (-not $script:DockerReady) {
        Add-Result -Layer '容器运行态' -Name '全部容器' -Status 'SKIP' -Detail 'Docker 不可用，跳过'
    }
    else {
        foreach ($c in $Containers) {
            $state = Get-ContainerState -ContainerName $c.Name
            if ($null -eq $state) {
                Add-Result -Layer '容器运行态' -Name $c.Label -Status 'FAIL' -Detail "容器 $($c.Name) 不存在" `
                    -Hint 'docker compose up -d（根目录 docker-compose.yml）'
                continue
            }
            if ($state.Status -ne 'running') {
                Add-Result -Layer '容器运行态' -Name $c.Label -Status 'FAIL' -Detail "Status=$($state.Status)" `
                    -Hint "docker logs --tail=100 $($c.Name)"
                continue
            }
            switch ($state.Health) {
                'healthy' {
                    Add-Result -Layer '容器运行态' -Name $c.Label -Status 'PASS' -Detail 'running / healthy'
                }
                'starting' {
                    Add-Result -Layer '容器运行态' -Name $c.Label -Status 'WARN' -Detail 'running / starting' `
                        -Hint '健康检查进行中，等待 30-60 秒后重跑本脚本'
                }
                'unhealthy' {
                    Add-Result -Layer '容器运行态' -Name $c.Label -Status 'FAIL' -Detail 'running / unhealthy' `
                        -Hint "docker logs --tail=200 $($c.Name)"
                }
                default {
                    Add-Result -Layer '容器运行态' -Name $c.Label -Status 'PASS' -Detail 'running / 无健康检查'
                }
            }
        }
    }
}
$sw.Stop()
$script:LayerTimes['容器运行态'] = $sw.Elapsed.TotalSeconds

# ------------------------------------------------------------
# [2] 端口连通性
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipInfra) {
    Write-Layer '[2] 端口连通性（已跳过）'
}
else {
    Write-Layer "[2] 端口连通性（$BaseHost）"

    # 一次性并发探测全部端口，结果缓存进 OpenPorts 供后续层复用，避免重复探测
    $portNumbers = @($Ports | ForEach-Object { $_.Port })
    $infraOpen = Test-TcpPortBatch -TargetHost $BaseHost -PortList $portNumbers -Timeout $TimeoutSec

    foreach ($p in $Ports) {
        $script:OpenPorts[$p.Port] = [bool]$infraOpen[$p.Port]
        if ($infraOpen[$p.Port]) {
            Add-Result -Layer '端口连通性' -Name "$($p.Label) :$($p.Port)" -Status 'PASS' -Detail 'TCP 可达'
        }
        else {
            Add-Result -Layer '端口连通性' -Name "$($p.Label) :$($p.Port)" -Status 'FAIL' -Detail 'TCP 不可达' `
                -Hint "本机部署检查容器是否启动；远程部署检查防火墙与安全组是否放通 $($p.Port)"
        }
    }
}
$sw.Stop()
$script:LayerTimes['端口连通性'] = $sw.Elapsed.TotalSeconds

# ------------------------------------------------------------
# [3] 中间件协议态
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipInfra) {
    Write-Layer '[3] 中间件协议态（已跳过）'
}
else {
    Write-Layer '[3] 中间件协议态（真实接口探活）'

    # 端口未监听的中间件不再走 HTTP 探活：端口层已给出明确的"TCP 不可达"结论，
    # 这里跳过可避免同一根因重复报错，也省掉每个端点一次无谓的超时等待
    # Nacos readiness
    $nacosReady = $null
    if (Test-PortOpen -Port 8848) {
        $nacosReady = Get-HttpText -Url "http://${BaseHost}:8848/nacos/v1/console/health/readiness" -Timeout $TimeoutSec
    }
    if ($null -ne $nacosReady -and $nacosReady -match 'ok') {
        Add-Result -Layer '中间件协议态' -Name 'Nacos 就绪' -Status 'PASS' -Detail 'readiness = ok'
    }
    elseif (Test-PortOpen -Port 8848) {
        Add-Result -Layer '中间件协议态' -Name 'Nacos 就绪' -Status 'FAIL' -Detail 'readiness 未返回 ok' `
            -Hint 'Nacos 依赖 MySQL，先确认 nacos_config 库已初始化；docker logs aics-nacos'
    }
    else {
        Add-PortDown -Layer '中间件协议态' -Name 'Nacos 就绪' -Port 8848
    }

    # Elasticsearch 集群健康
    $es = $null
    if (Test-PortOpen -Port 9200) {
        $es = Get-HttpJson -Url "http://${BaseHost}:9200/_cluster/health" -Timeout $TimeoutSec
    }
    if ($null -ne $es) {
        switch ($es.status) {
            'green' { Add-Result -Layer '中间件协议态' -Name 'Elasticsearch 集群' -Status 'PASS' -Detail 'green' }
            'yellow' { Add-Result -Layer '中间件协议态' -Name 'Elasticsearch 集群' -Status 'WARN' -Detail 'yellow（副本未分配，单节点正常）' }
            default { Add-Result -Layer '中间件协议态' -Name 'Elasticsearch 集群' -Status 'FAIL' -Detail "$($es.status)" -Hint '检查分片与磁盘水位' }
        }
    }
    elseif (Test-PortOpen -Port 9200) {
        Add-Result -Layer '中间件协议态' -Name 'Elasticsearch 集群' -Status 'FAIL' -Detail '无响应' `
            -Hint '单节点部署若内存不足会反复重启，检查 docker logs aics-elasticsearch'
    }
    else {
        Add-PortDown -Layer '中间件协议态' -Name 'Elasticsearch 集群' -Port 9200
    }

    # MinIO
    $minioLive = $null
    if (Test-PortOpen -Port 9000) {
        $minioLive = Get-HttpText -Url "http://${BaseHost}:9000/minio/health/live" -Timeout $TimeoutSec
    }
    if ($null -ne $minioLive) {
        Add-Result -Layer '中间件协议态' -Name 'MinIO 存活' -Status 'PASS' -Detail 'health/live 正常'
    }
    elseif (Test-PortOpen -Port 9000) {
        Add-Result -Layer '中间件协议态' -Name 'MinIO 存活' -Status 'FAIL' -Detail '无响应' `
            -Hint 'docker logs minio；注意容器名是 minio，不是 aics-minio'
    }
    else {
        Add-PortDown -Layer '中间件协议态' -Name 'MinIO 存活' -Port 9000
    }

    # Chroma：只支持 v2 API 的版本会拒绝 v1，因此先探 v2 再回落 v1
    $chroma = $null
    $chromaVer = 'v2'
    if (Test-PortOpen -Port 8000) {
        $chroma = Get-HttpText -Url "http://${BaseHost}:8000/api/v2/heartbeat" -Timeout $TimeoutSec
        if ($null -eq $chroma) {
            $chroma = Get-HttpText -Url "http://${BaseHost}:8000/api/v1/heartbeat" -Timeout $TimeoutSec
            $chromaVer = 'v1'
        }
    }
    if ($null -ne $chroma) {
        Add-Result -Layer '中间件协议态' -Name 'Chroma 向量库' -Status 'PASS' -Detail "heartbeat OK（$chromaVer）"
        if ($chromaVer -eq 'v1') {
            Add-Result -Layer '中间件协议态' -Name 'Chroma API 版本' -Status 'WARN' -Detail '仅探测到 v1' `
                -Hint '若远端升级为仅 v2，需 Spring AI 1.1.4+ 并配置 tenant-name / database-name'
        }
    }
    elseif (Test-PortOpen -Port 8000) {
        Add-Result -Layer '中间件协议态' -Name 'Chroma 向量库' -Status 'FAIL' -Detail 'heartbeat 无响应' `
            -Hint 'RAG 链路的硬依赖。确认容器 aics-chroma 运行且 8000 端口可达；远端实例需为 v2 API'
    }
    else {
        Add-PortDown -Layer '中间件协议态' -Name 'Chroma 向量库' -Port 8000
    }

    # Neo4j
    $neo = $null
    if (Test-PortOpen -Port 7474) {
        $neo = Get-HttpText -Url "http://${BaseHost}:7474" -Timeout $TimeoutSec
    }
    if ($null -ne $neo) {
        Add-Result -Layer '中间件协议态' -Name 'Neo4j' -Status 'PASS' -Detail 'HTTP 可达'
    }
    elseif (Test-PortOpen -Port 7474) {
        Add-Result -Layer '中间件协议态' -Name 'Neo4j' -Status 'WARN' -Detail 'HTTP 无响应' `
            -Hint '图数据库为可选能力，未使用时不影响主链路'
    }
    else {
        Add-PortDown -Layer '中间件协议态' -Name 'Neo4j' -Port 7474
    }

    # Redis / MySQL / RocketMQ 协议级探活：Docker 可用 且 端口确已监听时才执行
    $needContainerProbe = $script:DockerReady -and (
        (Test-PortOpen -Port 6379) -or (Test-PortOpen -Port 3306) -or (Test-PortOpen -Port 9876))
    if ($needContainerProbe) {
        $pong = (docker exec aics-redis redis-cli ping 2>$null | Out-String).Trim()
        if ($pong -eq 'PONG') {
            Add-Result -Layer '中间件协议态' -Name 'Redis PING' -Status 'PASS' -Detail 'PONG'
        }
        else {
            Add-Result -Layer '中间件协议态' -Name 'Redis PING' -Status 'FAIL' -Detail "返回 '$pong'" `
                -Hint 'Redis 不可用会同时影响网关分布式限流与对话缓存'
        }

        # MySQL 协议级 ping
        $null = docker exec aics-mysql mysqladmin ping -h localhost -uroot -proot --silent 2>$null
        if ($LASTEXITCODE -eq 0) {
            Add-Result -Layer '中间件协议态' -Name 'MySQL PING' -Status 'PASS' -Detail 'mysqld is alive'
        }
        else {
            Add-Result -Layer '中间件协议态' -Name 'MySQL PING' -Status 'FAIL' -Detail 'ping 失败' `
                -Hint 'docker logs aics-mysql；确认 root 密码与 compose 中一致'
        }

        # RocketMQ namesrv
        $null = docker exec aics-rocketmq-namesrv sh -c "echo probe | nc -w 2 localhost 9876" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Add-Result -Layer '中间件协议态' -Name 'RocketMQ Namesrv' -Status 'PASS' -Detail '9876 可写'
        }
        else {
            Add-Result -Layer '中间件协议态' -Name 'RocketMQ Namesrv' -Status 'FAIL' -Detail '端口不可写' `
                -Hint 'docker logs aics-rocketmq-namesrv'
        }
    }
    else {
        Add-Result -Layer '中间件协议态' -Name 'Redis / MySQL / MQ 协议探活' -Status 'SKIP' -Detail '需要 Docker 且 6379/3306/9876 已监听'
    }
}
$sw.Stop()
$script:LayerTimes['中间件协议态'] = $sw.Elapsed.TotalSeconds

# ------------------------------------------------------------
# [4] 微服务健康
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipServices) {
    Write-Layer '[4] 微服务健康（已跳过）'
}
else {
    Write-Layer "[4] 微服务健康（${BaseHost}:8080-8090 /actuator/health）"

    # 先并发判端口再探健康接口：端口未监听时"服务未启动"比"健康接口无响应"更能指向根因，
    # 同时省掉每个未启动服务一次 3 秒的 HTTP 超时等待
    $svcPorts = @($Services | ForEach-Object { $_.Port })
    $svcOpen = Test-TcpPortBatch -TargetHost $BaseHost -PortList $svcPorts -Timeout $TimeoutSec

    foreach ($s in $Services) {
        $script:OpenPorts[$s.Port] = [bool]$svcOpen[$s.Port]
        if (-not $svcOpen[$s.Port]) {
            Add-Result -Layer '微服务健康' -Name "$($s.Name) ($($s.Port))" -Status 'FAIL' -Detail '端口未监听（服务未启动）' `
                -Hint "启动命令：mvn -pl $($s.Code) spring-boot:run"
            continue
        }
        $url = "http://${BaseHost}:$($s.Port)/actuator/health"
        $body = Get-HttpJson -Url $url -Timeout $TimeoutSec
        if ($null -eq $body) {
            Add-Result -Layer '微服务健康' -Name "$($s.Name) ($($s.Port))" -Status 'FAIL' -Detail '端口已监听但健康接口无响应' `
                -Hint "服务可能仍在启动中（Java 应用冷启动需 30-60 秒），稍后重跑；仍失败则查启动日志"
            continue
        }
        if ($body.status -eq 'UP') {
            Add-Result -Layer '微服务健康' -Name "$($s.Name) ($($s.Port))" -Status 'PASS' -Detail 'UP'
        }
        else {
            Add-Result -Layer '微服务健康' -Name "$($s.Name) ($($s.Port))" -Status 'FAIL' -Detail "$($body.status)" `
                -Hint '服务起来了但依赖不健康，逐项看 detail 字段定位是哪个组件'
        }
    }
}
$sw.Stop()
$script:LayerTimes['微服务健康'] = $sw.Elapsed.TotalSeconds

# ------------------------------------------------------------
# [5] 注册与路由
# ------------------------------------------------------------
$sw = [System.Diagnostics.Stopwatch]::StartNew()
if ($SkipServices) {
    Write-Layer '[5] 注册与路由（已跳过）'
}
else {
    Write-Layer '[5] 注册与路由'

    $nacosUp = Test-PortOpen -Port 8848

    # Nacos 服务注册
    $svcList = $null
    if ($nacosUp) {
        $svcList = Get-HttpJson -Url "http://${BaseHost}:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=200&namespaceId=$NacosNs" -Timeout $TimeoutSec
    }
    if ($null -eq $svcList -and -not $nacosUp) {
        Add-PortDown -Layer '注册与路由' -Name 'Nacos 服务注册' -Port 8848
    }
    elseif ($null -eq $svcList) {
        Add-Result -Layer '注册与路由' -Name 'Nacos 服务列表' -Status 'FAIL' -Detail '查询失败' `
            -Hint "确认命名空间 $NacosNs 已创建（Nacos 控制台 → 命名空间）"
    }
    elseif ($svcList.doms -contains 'ai-cs-gateway') {
        Add-Result -Layer '注册与路由' -Name 'Nacos 服务注册' -Status 'PASS' -Detail "已注册 $($svcList.count) 个服务"
        $missing = @()
        foreach ($s in $Services) {
            if ($svcList.doms -notcontains $s.Code) { $missing += $s.Code }
        }
        if ($missing.Count -eq 0) {
            Add-Result -Layer '注册与路由' -Name '服务注册完整性' -Status 'PASS' -Detail '11/11 已注册'
        }
        else {
            Add-Result -Layer '注册与路由' -Name '服务注册完整性' -Status 'WARN' -Detail "未注册: $($missing -join ', ')" `
                -Hint '未启动的服务不会注册；若已启动仍缺失，检查 spring.cloud.nacos.discovery 配置与网络'
        }
    }
    else {
        Add-Result -Layer '注册与路由' -Name 'Nacos 服务注册' -Status 'FAIL' -Detail '未发现 ai-cs-gateway' `
            -Hint '业务服务尚未启动，或注册到了别的 namespace/group'
    }

    # Nacos 配置项
    $cfg = $null
    if ($nacosUp) {
        $cfg = Get-HttpJson -Url "http://${BaseHost}:8848/nacos/v1/cs/configs?search=accurate&dataId=&group=&pageNo=1&pageSize=100&tenant=$NacosNs" -Timeout $TimeoutSec
    }
    if ($null -eq $cfg -and -not $nacosUp) {
        Add-PortDown -Layer '注册与路由' -Name 'Nacos 配置项' -Port 8848
    }
    elseif ($null -eq $cfg) {
        Add-Result -Layer '注册与路由' -Name 'Nacos 配置项' -Status 'WARN' -Detail '查询失败' `
            -Hint '本地 application.yml 有兜底配置，缺 Nacos 配置仍可启动，但以 Nacos 为准'
    }
    elseif ([int]$cfg.totalCount -gt 0) {
        Add-Result -Layer '注册与路由' -Name 'Nacos 配置项' -Status 'PASS' -Detail "$($cfg.totalCount) 项"
    }
    else {
        Add-Result -Layer '注册与路由' -Name 'Nacos 配置项' -Status 'WARN' -Detail '命名空间下无配置' `
            -Hint '执行 deploy/nacos/publish-to-nacos.ps1 发布配置（如该脚本存在）'
    }

    # 网关入口（8080 已在第 4 层判定过，未监听则不再等待 HTTP 超时）
    $gw = $null
    if (Test-PortOpen -Port 8080) {
        $gw = Get-HttpJson -Url "http://${BaseHost}:8080/actuator/health" -Timeout $TimeoutSec
    }
    if ($null -ne $gw -and $gw.status -eq 'UP') {
        Add-Result -Layer '注册与路由' -Name '网关入口' -Status 'PASS' -Detail "http://${BaseHost}:8080"
    }
    elseif (Test-PortOpen -Port 8080) {
        Add-Result -Layer '注册与路由' -Name '网关入口' -Status 'FAIL' -Detail '端口已监听但健康接口不可用' `
            -Hint '网关是所有流量的唯一入口，必须先通；查看网关启动日志'
    }
    else {
        Add-PortDown -Layer '注册与路由' -Name '网关入口' -Port 8080
    }

    # 前端
    $fe = Get-HttpText -Url "http://${BaseHost}:5173" -Timeout $TimeoutSec
    if ($null -ne $fe) {
        Add-Result -Layer '注册与路由' -Name '前端页面' -Status 'PASS' -Detail "http://${BaseHost}:5173"
    }
    else {
        Add-Result -Layer '注册与路由' -Name '前端页面' -Status 'WARN' -Detail '5173 无响应' `
            -Hint 'cd ai-cs-frontend && npm install && npm run dev；生产环境走 Nginx 静态托管'
    }
}
$sw.Stop()
$script:LayerTimes['注册与路由'] = $sw.Elapsed.TotalSeconds

# ============================================================
# [6] 汇总
# ============================================================
$finishedAt = Get-Date
$passCount = @($script:Results | Where-Object { $_.Status -eq 'PASS' }).Count
$failCount = @($script:Results | Where-Object { $_.Status -eq 'FAIL' }).Count
$warnCount = @($script:Results | Where-Object { $_.Status -eq 'WARN' }).Count
$skipCount = @($script:Results | Where-Object { $_.Status -eq 'SKIP' }).Count

Write-Host ''
Write-Host '==========================================================' -ForegroundColor White
Write-Host (' 结果: PASS={0}  FAIL={1}  WARN={2}  SKIP={3}' -f $passCount, $failCount, $warnCount, $skipCount) -ForegroundColor White
Write-Host (' 耗时: {0:N1} 秒' -f ($finishedAt - $startedAt).TotalSeconds) -ForegroundColor White
if ($failCount -eq 0) {
    Write-Host ' 结论: 通过' -ForegroundColor Green
    Write-Host " 入口: http://${BaseHost}:5173  (网关 http://${BaseHost}:8080)" -ForegroundColor Green
}
else {
    Write-Host " 结论: 存在 $failCount 项失败，见下方待办" -ForegroundColor Red
}
Write-Host '==========================================================' -ForegroundColor White

# 待办清单
$failures = @($script:Results | Where-Object { $_.Status -eq 'FAIL' })
if ($failures.Count -gt 0) {
    Write-Host ''
    Write-Host ' 待办（按此顺序处理）:' -ForegroundColor Yellow
    $i = 1
    foreach ($f in $failures) {
        Write-Host ("  {0}. [{1}] {2} —— {3}" -f $i, $f.Layer, $f.Name, $f.Detail) -ForegroundColor Yellow
        if (-not [string]::IsNullOrWhiteSpace($f.Hint)) {
            Write-Host ("     -> {0}" -f $f.Hint) -ForegroundColor DarkYellow
        }
        $i++
    }
}

# ------------------------------------------------------------
# 写 Markdown 报告（交付证据）
# ------------------------------------------------------------
if (-not $NoReport) {
    if (-not [System.IO.Path]::IsPathRooted($ReportDir)) {
        $ReportDir = Join-Path $repoRoot $ReportDir
    }
    if (-not (Test-Path $ReportDir)) {
        $null = New-Item -ItemType Directory -Force -Path $ReportDir
    }
    $reportFile = Join-Path $ReportDir ('selfcheck-{0}.md' -f $startedAt.ToString('yyyyMMdd-HHmmss'))

    $md = New-Object System.Collections.ArrayList
    [void]$md.Add('# AI 智能客服平台 —— 部署自检报告')
    [void]$md.Add('')
    [void]$md.Add('| 项 | 内容 |')
    [void]$md.Add('|---|---|')
    [void]$md.Add("| 检查时间 | $($startedAt.ToString('yyyy-MM-dd HH:mm:ss')) |")
    [void]$md.Add("| 目标环境 | $BaseHost |")
    [void]$md.Add("| 执行主机 | $env:COMPUTERNAME |")
    [void]$md.Add("| 耗时 | $([math]::Round(($finishedAt - $startedAt).TotalSeconds, 1)) 秒 |")
    [void]$md.Add("| 结论 | **PASS=$passCount / FAIL=$failCount / WARN=$warnCount / SKIP=$skipCount** |")
    [void]$md.Add('')
    [void]$md.Add('## 分层耗时')
    [void]$md.Add('')
    [void]$md.Add('| 层级 | 耗时(秒) |')
    [void]$md.Add('|---|---|')
    foreach ($k in $script:LayerTimes.Keys) {
        [void]$md.Add("| $k | $([math]::Round($script:LayerTimes[$k], 1)) |")
    }
    [void]$md.Add('')
    [void]$md.Add('## 未通过项（待办）')
    [void]$md.Add('')
    if ($failures.Count -eq 0) {
        [void]$md.Add('无。')
    }
    else {
        [void]$md.Add('| # | 层级 | 检查项 | 现象 | 建议处置 |')
        [void]$md.Add('|---|---|---|---|---|')
        $i = 1
        foreach ($f in $failures) {
            [void]$md.Add("| $i | $($f.Layer) | $($f.Name) | $($f.Detail) | $($f.Hint) |")
            $i++
        }
    }
    [void]$md.Add('')
    [void]$md.Add('## 全量明细')
    [void]$md.Add('')
    [void]$md.Add('| 层级 | 检查项 | 结果 | 说明 |')
    [void]$md.Add('|---|---|---|---|')
    foreach ($r in $script:Results) {
        [void]$md.Add("| $($r.Layer) | $($r.Name) | $($r.Status) | $($r.Detail) |")
    }
    [void]$md.Add('')
    [void]$md.Add('---')
    [void]$md.Add('')
    [void]$md.Add('> 由 `deploy/scripts/check-local.ps1` 自动生成。脚本为只读检查，本报告可作为交付验收证据附件。')

    $md -join [Environment]::NewLine | Set-Content -Path $reportFile -Encoding UTF8
    Write-Host ''
    Write-Host " 报告已生成: $reportFile" -ForegroundColor Cyan
}

if ($failCount -gt 0) { exit 1 }
exit 0
