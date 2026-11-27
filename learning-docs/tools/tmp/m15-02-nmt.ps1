# learning-docs/tools/tmp/m15-02-nmt.ps1
# Purpose: let the JVM itself itemize its native memory (heap / class / code / thread / GC / internal),
#          so we can see how much of the process RSS is NOT the -Xmx heap.
# Usage:   .\m15-02-nmt.ps1
# Requires: -XX:NativeMemoryTracking=summary (NMT must be enabled at startup; you cannot attach it later).
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

$proc = Start-Process -FilePath java `
    -ArgumentList '-Xmx512m', '-XX:NativeMemoryTracking=summary', (Join-Path $here 'm15-02-heap-stages.java') `
    -RedirectStandardOutput (Join-Path $here 'm15-02-nmt.out.txt') `
    -RedirectStandardError (Join-Path $here 'm15-02-nmt.err.txt') `
    -PassThru -WindowStyle Hidden

# stage 2 (heap + direct) finishes around t=7s; sample then
Start-Sleep -Seconds 8
'=== process counters ==='
Get-Process -Id $proc.Id | Select-Object Id, @{n = 'RSS_MB'; e = { [math]::Round($_.WorkingSet64 / 1MB, 1) } },
    @{n = 'Private_MB'; e = { [math]::Round($_.PrivateMemorySize64 / 1MB, 1) } },
    @{n = 'VSZ_MB'; e = { [math]::Round($_.VirtualMemorySize64 / 1MB, 1) } }, Threads |
    Format-List | Out-String -Width 200

'=== jcmd <pid> VM.native_memory summary (JVM itemized native memory) ==='
& jcmd $proc.Id VM.native_memory summary
'=== jcmd <pid> GC.heap_info ==='
& jcmd $proc.Id GC.heap_info

$proc.WaitForExit()
