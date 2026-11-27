# 一次性审计脚本：把「有生产无消费 / 有消费无生产」变成可复现输出
# 用法（仓库根）：& .\learning-docs\tools\tmp\cdc-chain-audit.ps1
Set-Location (Resolve-Path "$PSScriptRoot\..\..\..")

$java = Get-ChildItem -Recurse -Include *.java -File |
        Where-Object { $_.FullName -notmatch '\\target\\|\\src\\test\\' }

# ---------- 1) 消费端：@RocketMQMessageListener(topic=..., consumerGroup=...) ----------
$consumers = [ordered]@{}
foreach ($f in $java) {
    $text = Get-Content $f.FullName -Raw -Encoding UTF8
    foreach ($m in [regex]::Matches($text, '@RocketMQMessageListener\s*\(([^)]*)\)', 'Singleline')) {
        $body  = $m.Groups[1].Value -replace '\s+', ' '
        $topic = ([regex]::Match($body, 'topic\s*=\s*("[^"]*"|[\w.]+)')).Groups[1].Value.Trim('"')
        $group = ([regex]::Match($body, 'consumerGroup\s*=\s*("[^"]*"|[\w.]+)')).Groups[1].Value.Trim('"')
        $line  = ($text.Substring(0, $m.Index) -split "`n").Count
        if ($topic -eq 'WorkOrderProducer.TOPIC') { $topic = 'work-order-topic' }
        if (-not $consumers.Contains($topic)) {
            $consumers[$topic] = "$($f.Name):$line [$group]"
        }
    }
}

# ---------- 2) 生产端：常量 TOPIC 定义 + 发送点（含 MessagePublisher 抽象层） ----------
$sendRe = '\.(syncSend|convertAndSend|sendMessageInTransaction|asyncSend|sendOneWay|sendDelayed|send)\('
$producers = [ordered]@{}
foreach ($f in $java) {
    $text  = Get-Content $f.FullName -Raw -Encoding UTF8
    $lines = $text -split "`n"
    $constTopic = $null
    $cm = [regex]::Match($text, 'static\s+final\s+String\s+TOPIC\s*=\s*"([^"]+)"')
    if ($cm.Success) { $constTopic = $cm.Groups[1].Value }
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -notmatch $sendRe) { continue }
        $window = $lines[$i] + ' ' + $lines[[Math]::Min($i + 1, $lines.Count - 1)]
        $lit    = [regex]::Match($window, '"([a-zA-Z0-9\-]+-topic)"')
        $usesConst = $window -match '(^|[\s(,])TOPIC\b'
        $topic  = if ($lit.Success) { $lit.Groups[1].Value } elseif ($usesConst) { $constTopic } else { $null }
        if (-not $topic) { continue }
        if (-not $producers.Contains($topic)) { $producers[$topic] = "$($f.Name):$($i + 1)" }
    }
}

# ---------- 3) 闭环矩阵 ----------
Write-Output ("{0,-32} {1,-30} {2}" -f 'topic', 'producer(send site)', 'consumer(listener)')
Write-Output ('-' * 104)
foreach ($t in (($consumers.Keys + $producers.Keys) | Sort-Object -Unique)) {
    $p = if ($producers.Contains($t)) { $producers[$t] }
         elseif ($t -like '%DLQ%*') { '(broker auto)' } else { '(NONE)' }
    $c = if ($consumers.Contains($t)) { $consumers[$t] } else { '(NONE)' }
    Write-Output ("{0,-32} {1,-30} {2}" -f $t, $p, $c)
}
$pwc = ($producers.Keys | Where-Object { -not $consumers.Contains($_) }) -join ', '
$cwp = ($consumers.Keys | Where-Object { -not $producers.Contains($_) -and $_ -notlike '%DLQ%*' }) -join ', '
$dlq = ($consumers.Keys | Where-Object { $_ -like '%DLQ%*' }) -join ', '
Write-Output ''
Write-Output ("produce-without-consume = [{0}]" -f $pwc)
Write-Output ("consume-without-produce = [{0}]" -f $cwp)
Write-Output ("broker-generated-consumers = [{0}]" -f $dlq)
