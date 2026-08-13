# =============================================================================
# AI Customer Service Platform - Stop Everything
#
# Kills: Nacos (8848/9848), RocketMQ (9876/10911),
#        backend services (8080-8090), frontend Vite (5173).
#
# Usage: powershell -ExecutionPolicy Bypass -File .\tools\stop-all.ps1
# =============================================================================

$ErrorActionPreference = "SilentlyContinue"
$tools = $PSScriptRoot
$logDir = "$tools\logs"

# Ports owned by this platform
$ports = @(8848, 9848, 9876, 10911, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090, 5173)

# 1. Kill whatever is listening on our ports (most reliable)
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $ports -contains $_.LocalPort } |
    ForEach-Object {
        $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host ("  [Stop] {0} (PID {1}, port {2})" -f $proc.ProcessName, $_.OwningProcess, $_.LocalPort)
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }

Start-Sleep -Seconds 2

# 2. Fallback: leftover java/node processes whose command line matches our components
Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -in @("java.exe", "node.exe") -and
        $_.CommandLine -match "ai-cs-(user|knowledge|chat|search|message|notify|order|product|pay|mq|gateway)|nacos.*(standalone|Nacos)|mqnamesrv|mqbroker|rocketmq|ai-cs-frontend.*vite"
    } |
    ForEach-Object {
        Write-Host ("  [Stop] leftover {0} (PID {1})" -f $_.Name, $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

# 3. Clean pid files written by start-all.ps1
Get-ChildItem -Path "$logDir" -Filter '*.pid' -ErrorAction SilentlyContinue | Remove-Item -Force

Write-Host "All platform services stopped."