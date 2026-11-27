# learning-docs/tools/tmp/m15-02-sample-rss.ps1
# Purpose: sample the JVM process's RSS / Private / VSZ from OUTSIDE the JVM, then compare
#          with what the JVM reports about its own heap.
# Usage:   .\m15-02-sample-rss.ps1
# Meaning: WorkingSet64       = RSS  (pages actually resident)
#          PrivateMemorySize64 = private committed bytes (includes pages swapped out)
#          VirtualMemorySize64 = VSZ  (whole virtual address space, mostly reservation)
# Labels are ASCII on purpose: Windows PowerShell 5.1 reads BOM-less .ps1 as ANSI,
# so Chinese literals here would be mojibake in the quoted output.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Join-Path $here 'm15-02-heap-stages.out.txt'
$err = Join-Path $here 'm15-02-heap-stages.err.txt'

$proc = Start-Process -FilePath java `
    -ArgumentList '-Xmx512m', (Join-Path $here 'm15-02-heap-stages.java') `
    -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

$rows = @()
while (-not $proc.HasExited) {
    try {
        $proc.Refresh()
        $rows += [pscustomobject]@{
            t_s        = [math]::Round(((Get-Date) - $proc.StartTime).TotalSeconds, 1)
            RSS_MB     = [math]::Round($proc.WorkingSet64 / 1MB, 1)
            Private_MB = [math]::Round($proc.PrivateMemorySize64 / 1MB, 1)
            VSZ_MB     = [math]::Round($proc.VirtualMemorySize64 / 1MB, 1)
            Threads    = $proc.Threads.Count
        }
    } catch { }
    Start-Sleep -Milliseconds 400
}

'=== external sampling (first sample of each second) ==='
$rows | Group-Object { [math]::Floor($_.t_s) } | ForEach-Object { $_.Group[0] } |
    Format-Table -AutoSize | Out-String -Width 200
'=== peak of the samples ==='
"Peak_RSS_MB = {0}" -f ($rows | Measure-Object RSS_MB -Maximum).Maximum
"Peak_VSZ_MB = {0}" -f ($rows | Measure-Object VSZ_MB -Maximum).Maximum
'=== JVM self-report (stdout of the java process) ==='
Get-Content $out
'=== stderr ==='
Get-Content $err
