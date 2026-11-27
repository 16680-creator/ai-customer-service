# helm16-field-census.ps1 —— 统计 deploy/k8s 下 20 份清单里"可变字段"的分布
# 用途：给「把裸 YAML 抽象成 1 个 chart」这件事提供参数化候选清单的真实数据。
# 说明：用缩进状态机逐行扫描（本机无 yaml 解析库，也没有 helm/kubectl），
#       只提取本篇讨论的字段，不做通用 YAML 解析。
# 运行：pwsh -File learning-docs\tools\tmp\helm16-field-census.ps1
$ErrorActionPreference = 'Stop'
$root = 'D:\Projects\Persion\ai-customer-service'
$files = Get-ChildItem (Join-Path $root 'deploy\k8s') -Recurse -File -Filter *.yaml | Sort-Object FullName

function Read-Lines([string]$p) { [System.IO.File]::ReadAllLines($p, [System.Text.Encoding]::UTF8) }

function Get-Indent([string]$l) {
    $m = [regex]::Match($l, '^( *)')
    return $m.Groups[1].Value.Length
}

$rows = @()
foreach ($f in $files) {
    $lines = Read-Lines $f.FullName
    $rel = $f.FullName.Replace($root + '\', '').Replace('\', '/')

    # ---- 逐行状态机 ----
    $kind = $null; $inEnv = $false; $envIndent = -1
    $images = @(); $replicas = @(); $ns = @(); $svcTypes = @(); $nodePorts = @()
    $cpuReq = @(); $memReq = @(); $cpuLim = @(); $memLim = @()
    $envCount = 0; $secretRef = 0; $literalEnv = 0
    $tcpProbe = 0; $httpProbe = 0; $probeKinds = @()
    $resourcesBlocks = 0
    $commentLines = 0
    $imagePullPolicy = 0
    $probeBlock = $null

    foreach ($l in $lines) {
        if ($l -match '^\s*#') { $commentLines++; continue }
        if ($l.Trim() -eq '' -or $l.Trim() -eq '---') { $inEnv = $false; $envIndent = -1; continue }
        $ind = Get-Indent $l
        $t = $l.Trim()

        if ($t -match '^kind:\s*(\S+)') { $kind = $Matches[1]; continue }
        if ($t -match '^image:\s*(\S+)') { $images += $Matches[1]; continue }
        if ($t -match '^imagePullPolicy:\s*(\S+)') { if ($Matches[1] -eq 'IfNotPresent') { $imagePullPolicy++ } ; continue }
        if ($t -match '^replicas:\s*(\d+)') { $replicas += [int]$Matches[1]; continue }
        if ($t -match '^namespace:\s*(\S+)') { $ns += $Matches[1]; continue }
        # 修 bug2：type: 只算 Service 的 spec.type，别把 Secret 的 type: Opaque 混进来
        if ($t -match '^type:\s*(\S+)') { if ($kind -eq 'Service') { $svcTypes += $Matches[1] }; continue }
        if ($t -match '^nodePort:\s*(\d+)') { $nodePorts += [int]$Matches[1]; continue }

        # 修 bug3：新的容器开始，requests/limits 归属必须重置
        if ($t -match '^(containers|- name:\s)' -or $t -match '^image:') { $probeBlock = $null }

        # 修 bug1：env: 常带尾注释（env:      # ---- 环境变量…），用 -match 而不是 -eq
        if ($t -match '^env:') { $inEnv = $true; $envIndent = $ind; continue }
        if ($inEnv) {
            if ($ind -le $envIndent) { $inEnv = $false; $envIndent = -1 }
            else {
                if ($t -match '^- name:') { $envCount++ }
                if ($t -match '^valueFrom:') { $secretRef++ }
                if ($t -match '^value:') { $literalEnv++ }
                continue
            }
        }

        # resources 段：requests / limits 下的 cpu / memory
        if ($t -match '^(requests|limits):') { $resourcesBlocks++; $probeBlock = $Matches[1]; continue }
        if ($t -match '^cpu:\s*"?([^"]+)"?') {
            $v = $Matches[1].Trim()
            if ($probeBlock -eq 'requests') { $cpuReq += $v } elseif ($probeBlock -eq 'limits') { $cpuLim += $v }
            continue
        }
        if ($t -match '^memory:\s*"?([^"]+)"?') {
            $v = $Matches[1].Trim()
            if ($probeBlock -eq 'requests') { $memReq += $v } elseif ($probeBlock -eq 'limits') { $memLim += $v }
            continue
        }
        if ($t -match '^(tcpSocket|httpGet):') {
            $probeKinds += $Matches[1]
            if ($Matches[1] -eq 'tcpSocket') { $tcpProbe++ } else { $httpProbe++ }
            continue
        }
    }

    $tag = @(); $reg = @()
    foreach ($i in $images) {
        if ($i -match '^(?<r>.+):(?<t>[^:/]+)$') { $tag += $Matches['t']; if ($Matches['r'] -match '\.') { $reg += 'yes' } else { $reg += 'no' } }
        else { $tag += '(none)'; $reg += 'no' }
    }

    $rows += [pscustomobject]@{
        file          = ($rel -replace '^deploy/k8s/', '')
        # 修 bug3：剥掉 kind: 行尾注释，否则 kinds 列会显示成 "Service   # 资源类型：…"
        kinds         = (($lines | Where-Object { $_ -match '^kind:\s*(\S+)' } | ForEach-Object { (($_ -replace '^kind:\s*', '') -replace '\s+#.*$', '').Trim() }) -join '+')
        image         = ($images -join ',')
        tag           = ($tag -join ',')
        registry      = ($reg -join ',')
        pullPolicy    = $imagePullPolicy
        replicas      = (($replicas | ForEach-Object { $_ }) -join ',')
        namespace     = (($ns | Select-Object -Unique) -join ',')
        svcType       = (($svcTypes | Select-Object -Unique) -join ',')
        nodePort      = (($nodePorts | ForEach-Object { $_ }) -join ',')
        cpuReq        = (($cpuReq | Select-Object -Unique) -join ',')
        memReq        = (($memReq | Select-Object -Unique) -join ',')
        cpuLim        = (($cpuLim | Select-Object -Unique) -join ',')
        memLim        = (($memLim | Select-Object -Unique) -join ',')
        envItems      = $envCount
        envSecretRef  = $secretRef
        probes        = "$tcpProbe tcp/$httpProbe http"
        commentLines  = $commentLines
        totalLines    = $lines.Count
    }
}

Write-Output '=== 表A：20 份清单逐份服役字段（- 表示该文件没有此类资源） ==='
$rows | Format-Table -AutoSize file, kinds, replicas, namespace, svcType, nodePort, cpuReq, memReq, cpuLim, memLim, envItems, envSecretRef, probes | Out-String -Width 300

Write-Output '=== 表A2：镜像与 tag ==='
$rows | Format-Table -AutoSize file, image, tag, registry, pullPolicy | Out-String -Width 300

Write-Output '=== 表B：字段聚合（覆盖文件数 / 不同取值数） ==='
$fields = 'image', 'tag', 'replicas', 'namespace', 'svcType', 'nodePort', 'cpuReq', 'memReq', 'cpuLim', 'memLim', 'envItems'
foreach ($f in $fields) {
    $vals = $rows | Where-Object { $_.$f -ne '' -and $_.$f -ne $null } | ForEach-Object { $_.$f }
    $distinct = $vals | Select-Object -Unique
    "{0,-10} 覆盖 {1,2} 个文件  不同取值 {2,2} 个  →  {3}" -f $f, $vals.Count, $distinct.Count, (($distinct | Select-Object -First 8) -join ' | ')
}

Write-Output ''
Write-Output '=== 表C：注释占比（渲染产物会全部丢失） ==='
$sumC = ($rows | Measure-Object commentLines -Sum).Sum
$sumT = ($rows | Measure-Object totalLines -Sum).Sum
"注释行合计 {0} 行 / 总行数 {1} 行 = {2:N1}%" -f $sumC, $sumT, (100.0 * $sumC / $sumT)
$rows | Where-Object { $_.commentLines -gt 0 } | Sort-Object commentLines -Descending |
    ForEach-Object { "  {0,-45} 注释 {1,3} / {2,3} 行" -f $_.file, $_.commentLines, $_.totalLines }
