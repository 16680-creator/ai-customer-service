#!/usr/bin/env pwsh
<#
  m16-02-k8s-res-scan.ps1 —— 只用 pwsh 把 deploy/k8s 里的资源字段数清、数全（C 级证据）

  做四件事：
    1. 数「弹性/资源相关字段」在全部清单里各出现多少次（HPA / PDB / ResourceQuota / strategy / startupProbe …）
    2. 把每个 resources 块的 requests / limits 的 cpu、memory 加起来（区分容器资源与 PVC 的 requests.storage）
    3. 数 replicas 分布，按 replicas 展开算真实调度账（Σ requests × replicas）
    4. 算超卖倍数

  为什么还要一个 Node 版（m16-02-k8s-res-audit.mjs）：正文里的合计值必须能用两条互不相同的实现对上，
  两套解析结果一致才算真实数据。

  运行（本机只装了 Windows PowerShell 5.1，没有 pwsh 7）：
    powershell -NoProfile -File learning-docs\tools\tmp\m16-02-k8s-res-scan.ps1
  注意：本文件必须以 UTF-8 with BOM 保存，否则 PowerShell 5.1 会按 GBK 解析中文注释而报语法错。
#>

$ErrorActionPreference = 'Stop'
$Root = 'D:\Projects\Persion\ai-customer-service'
$K8sDir = Join-Path $Root 'deploy\k8s'

function ConvertTo-CpuMilli([string]$v) {
    if ($v -match '^(\d+)m$') { return [int]$Matches[1] }
    return [int]([double]$v * 1000)
}
function ConvertTo-MemMi([string]$v) {
    if ($v -match '^([\d.]+)(Ki|Mi|Gi|Ti)?$') {
        $n = [double]$Matches[1]
        switch ($Matches[2]) {
            'Ki' { return [int]($n / 1024) }
            'Mi' { return [int]$n }
            'Gi' { return [int]($n * 1024) }
            'Ti' { return [int]($n * 1024 * 1024) }
            default { return [int]($n / 1MB) }
        }
    }
    throw "cannot parse memory value: $v"
}

$files = Get-ChildItem -Path $K8sDir -Recurse -Filter *.yaml | Sort-Object FullName
$allText = ($files | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"
$totalLines = (($files | ForEach-Object { [System.IO.File]::ReadAllLines($_.FullName).Count }) | Measure-Object -Sum).Sum

Write-Host "Scan dir : $K8sDir"
Write-Host ("Files    : {0} yaml, {1} lines (UTF-8 correct count)" -f $files.Count, $totalLines)
Write-Host ''

Write-Host '=== 1) field occurrences (0 = not implemented) ==='
$fields = [ordered]@{
    'kind: HorizontalPodAutoscaler' = 'kind:\s*HorizontalPodAutoscaler'
    'kind: PodDisruptionBudget'     = 'kind:\s*PodDisruptionBudget'
    'kind: ResourceQuota'           = 'kind:\s*ResourceQuota'
    'kind: LimitRange'              = 'kind:\s*LimitRange'
    'strategy:'                     = '(?m)^\s*strategy:'
    'terminationGracePeriodSeconds' = 'terminationGracePeriodSeconds'
    'preStop'                       = 'preStop:'
    'startupProbe'                  = 'startupProbe:'
    'readinessProbe'                = 'readinessProbe:'
    'livenessProbe'                 = 'livenessProbe:'
    'resources:'                    = '(?m)^\s*resources:'
    'requests:'                     = '(?m)^\s*requests:'
    'limits:'                       = '(?m)^\s*limits:'
    'replicas:'                     = '(?m)^\s*replicas:'
}
foreach ($k in $fields.Keys) {
    $n = ([regex]::Matches($allText, $fields[$k])).Count
    Write-Host ("  {0,-32} {1}" -f $k, $n)
}

Write-Host ''
Write-Host '=== 2) sum of every resources block (container res vs PVC storage separated) ==='
$reqCpu = 0; $reqMem = 0; $limCpu = 0; $limMem = 0
$pvcCount = 0; $blockCount = 0; $replicaValues = @(); $curReplicas = 1
$rows = @()

foreach ($f in $files) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName)
    $curReplicas = 1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\s+replicas:\s*(\d+)') {
            $curReplicas = [int]$Matches[1]
            $replicaValues += $curReplicas
        }
        if ($lines[$i] -notmatch '^(\s*)resources:\s*(#.*)?$') { continue }
        $base = $Matches[1].Length
        $blockCount++
        $blk = @{ requests = @{}; limits = @{} }
        $sect = $null
        for ($j = $i + 1; $j -lt $lines.Count; $j++) {
            $raw = $lines[$j]
            if ($raw.Trim() -eq '' -or $raw.TrimStart().StartsWith('#')) { continue }
            $ind = $raw.Length - $raw.TrimStart().Length
            if ($ind -le $base) { break }
            $t = ($raw -replace '\s+#.*$', '').Trim()
            if ($t -match '^(requests|limits):') { $sect = $Matches[1]; continue }
            if ($null -eq $sect) { continue }
            if ($t -match '^(cpu|memory|storage):\s*"?([^"\s]+)"?') {
                $blk[$sect][$Matches[1]] = $Matches[2]
            }
        }
        if ($blk.requests.ContainsKey('storage')) { $pvcCount++; continue }
        $rc = ConvertTo-CpuMilli $blk.requests['cpu']
        $rm = ConvertTo-MemMi $blk.requests['memory']
        $lc = ConvertTo-CpuMilli $blk.limits['cpu']
        $lm = ConvertTo-MemMi $blk.limits['memory']
        $reqCpu += $rc; $reqMem += $rm; $limCpu += $lc; $limMem += $lm
        $rows += [pscustomobject]@{ ReqCpu = $rc; ReqMem = $rm; LimCpu = $lc; LimMem = $lm; Replicas = $curReplicas }
    }
}

Write-Host ("resources blocks: {0} total = {1} container blocks + {2} PVC storage requests" -f $blockCount, ($blockCount - $pvcCount), $pvcCount)
Write-Host ''
Write-Host '=== 3) totals (one Pod per workload) ==='
Write-Host ("  SUM requests.cpu    = {0}m" -f $reqCpu)
Write-Host ("  SUM requests.memory = {0}Mi" -f $reqMem)
Write-Host ("  SUM limits.cpu      = {0}m" -f $limCpu)
Write-Host ("  SUM limits.memory   = {0}Mi" -f $limMem)
Write-Host ("  overcommit cpu {0:N2}x / memory {1:N2}x" -f ($limCpu / $reqCpu), ($limMem / $reqMem))

Write-Host ''
Write-Host '=== 4) replicas distribution ==='
$replicaValues | Group-Object | Sort-Object Name | ForEach-Object {
    Write-Host ("  replicas={0} -> {1} manifests" -f $_.Name, $_.Count)
}
$pods = ($replicaValues | Measure-Object -Sum).Sum
Write-Host ("  pods after expanding replicas = {0}" -f $pods)

Write-Host ''
Write-Host '=== 5) real scheduling bill (requests x replicas) ==='
$wReqCpu = ($rows | ForEach-Object { $_.ReqCpu * $_.Replicas } | Measure-Object -Sum).Sum
$wReqMem = ($rows | ForEach-Object { $_.ReqMem * $_.Replicas } | Measure-Object -Sum).Sum
$wLimCpu = ($rows | ForEach-Object { $_.LimCpu * $_.Replicas } | Measure-Object -Sum).Sum
$wLimMem = ($rows | ForEach-Object { $_.LimMem * $_.Replicas } | Measure-Object -Sum).Sum
Write-Host ("  SUM(requests.cpu x replicas)    = {0}m" -f $wReqCpu)
Write-Host ("  SUM(requests.memory x replicas) = {0}Mi" -f $wReqMem)
Write-Host ("  SUM(limits.cpu x replicas)      = {0}m" -f $wLimCpu)
Write-Host ("  SUM(limits.memory x replicas)   = {0}Mi" -f $wLimMem)
