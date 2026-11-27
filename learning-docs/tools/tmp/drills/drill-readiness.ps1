# 演练准备度自检（只读、离线、2 秒内跑完）
# 用途：演练开始前 5 分钟先跑一遍，确认「四个前提」里哪些还没到位；
#       任一项 FAIL，演练只能降级为「只看现象、不做判据」的观察练习，不能写进验收记录。
# 用法：pwsh -File drills/drill-readiness.ps1      （在仓库根执行）
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
Set-Location $root

function Count-Hits {
    param([string[]]$Include, [string]$Pattern, [string]$Scope = '.')
    $files = Get-ChildItem -Recurse -File -Include $Include -Path $Scope |
        Where-Object { $_.FullName -notmatch 'node_modules|\\target\\|\\\.git\\|learning-docs' }
    return ($files | Select-String -Pattern $Pattern | Measure-Object).Count
}

$checks = @()

# 前提 1：稳态指标（没有指标就没有判据，只能靠人眼看日志）
$checks += [pscustomobject]@{
    前提   = '1 稳态指标'
    探测   = 'MeterRegistry / Timer.builder / Counter.builder 命中数'
    实测值 = (Count-Hits -Include '*.java' -Pattern 'MeterRegistry|Timer\.builder|Counter\.builder')
    门槛   = '≥1'
}

# 前提 2：注入能力（有工具才能注入延迟/断网/丢包，否则只能靠停服务或改配置）
$checks += [pscustomobject]@{
    前提   = '2 注入能力'
    探测   = 'chaos / ChaosBlade / toxiproxy 命中数'
    实测值 = (Count-Hits -Include '*.java','*.yml','*.yaml','*.ps1','*.sh','*.mjs' -Pattern 'chaos|ChaosBlade|toxiproxy')
    门槛   = '≥1'
}

# 前提 3：中止开关（演练中能一键回滚，否则停不下来）
$checks += [pscustomobject]@{
    前提   = '3 中止开关'
    探测   = '@RefreshScope 真注解行数（可热更配置的类）'
    实测值 = (Count-Hits -Include '*.java' -Pattern '^\s*@RefreshScope\s*$')
    门槛   = '≥1'
}

# 前提 4：判据资产（Prometheus 侧有没有规则化判据/告警）
$promYml = Join-Path $root 'deploy\observability\prometheus.yml'
$ruleHits = if (Test-Path $promYml) { (Select-String -Path $promYml -Pattern 'rule_files|alertmanager' | Measure-Object).Count } else { 0 }
$checks += [pscustomobject]@{
    前提   = '4 判据资产'
    探测   = 'deploy\observability\prometheus.yml 中 rule_files / alertmanager 命中数'
    实测值 = $ruleHits
    门槛   = '≥1'
}

# 前提 5：复盘沉淀的载体（有案例库才谈得上"沉淀"）
$caseDocs = (Get-ChildItem -File -Path 'docs' -Filter '2*.md' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '^2[89]-|^30-' } | Measure-Object).Count
$checks += [pscustomobject]@{
    前提   = '5 复盘载体'
    探测   = 'docs/28-*.md ~ docs/30-*.md 故障案例库文件数'
    实测值 = $caseDocs
    门槛   = '≥1'
}

$checks | ForEach-Object {
    $_ | Add-Member -NotePropertyName 结论 -NotePropertyValue $(if ($_.实测值 -ge [int]($_.门槛 -replace '[^0-9]','')) { 'PASS' } else { 'FAIL' }) -PassThru
} | Format-Table -AutoSize

$failed = ($checks | Where-Object { $_.结论 -eq 'FAIL' }).Count
Write-Host "汇总：$($checks.Count) 项前提，FAIL $failed 项 → $(if ($failed -eq 0) { '可做带判据的正式演练' } else { '只能做观察式演练，先补 FAIL 项' })"
