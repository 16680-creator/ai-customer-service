# =============================================================================
# Start infrastructure only: Nacos + RocketMQ(NameServer/Broker) + publish configs
#
# Usage: powershell -ExecutionPolicy Bypass -File .\tools\start-infra.ps1
# Idempotent: already-running components are skipped.
# =============================================================================

. "$PSScriptRoot\lib\common.ps1"

# ------------------------------------------------------------------ Nacos
Write-Host "===== [1/3] Infra: Nacos (JDK8) ====="
if (Test-PortListening 8848) {
    Write-Host "  [Skip] Nacos already running on 8848"
} else {
    # ".\" 前缀：NoDefaultCurrentDirectoryInExePath=1 时 cmd 不再从当前目录解析裸命令名
    Start-Process cmd -ArgumentList "/c", "set `"JAVA_HOME=$jdk8`" && cd /d $nacosBin && .\startup.cmd -m standalone" -WindowStyle Hidden
    Wait-Port 8848 120 "Nacos" | Out-Null
}

# ------------------------------------------------------------------ RocketMQ
Write-Host "===== [2/3] Infra: RocketMQ NameServer + Broker (JDK17) ====="
if (Test-PortListening 9876) {
    Write-Host "  [Skip] NameServer already running on 9876"
} else {
    Start-Process cmd -ArgumentList "/c", "set `"JAVA_HOME=$jdk17`" && set `"ROCKETMQ_HOME=$mqHome`" && cd /d $mqBin && .\mqnamesrv.cmd" -WindowStyle Hidden
    Wait-Port 9876 90 "NameServer" | Out-Null
}
if (Test-PortListening 10911) {
    Write-Host "  [Skip] Broker already running on 10911"
} elseif (Test-PortListening 9876) {
    Start-Sleep -Seconds 3
    Start-Process cmd -ArgumentList "/c", "set `"JAVA_HOME=$jdk17`" && set `"ROCKETMQ_HOME=$mqHome`" && cd /d $mqBin && .\mqbroker.cmd -n 127.0.0.1:9876 -c ../conf/broker-aics.conf" -WindowStyle Hidden
    Wait-Port 10911 90 "Broker" | Out-Null
} else {
    Write-Warning "  [WARN] NameServer not ready, Broker skipped"
}

# ------------------------------------------------------------- publish configs
Write-Host "===== [3/3] Publish Nacos configs (tenant=aics) ====="
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

Write-Host ""
Write-Host "===== Infra summary ====="
Write-Host ("  {0,-16} {1}  {2}" -f "nacos", 8848, $(if (Test-PortListening 8848) { "UP" } else { "DOWN" }))
Write-Host ("  {0,-16} {1}  {2}" -f "namesrv", 9876, $(if (Test-PortListening 9876) { "UP" } else { "DOWN" }))
Write-Host ("  {0,-16} {1}  {2}" -f "broker", 10911, $(if (Test-PortListening 10911) { "UP" } else { "DOWN" }))
Write-Host ""
Write-Host "  Nacos console: http://localhost:8848/nacos  (nacos/nacos)"
Write-Host "  Next: run .\tools\start-app.ps1 to start backend + frontend"
Write-Host ""