#!/usr/bin/env pwsh
<#
  m16-02-heap-headroom.ps1 —— 采样「-Xmx256m 的 JVM 整进程 RSS 是多少」

  做法：用 Start-Process 起 m16-02-heap-headroom.java（堆填到 85% 后挂 6 秒），
        每 250ms 读一次 Process.WorkingSet64（Windows 上即 RSS），取峰值。

  为什么要在文档里放这条：K8s 的 memory limit 管的是「整进程 RSS」，不是「Java 堆」。
        清单里写 -Xmx256m / memory 512Mi 时，堆只占上限的一半，另一半必须够放
        元空间、线程栈、直接内存（NIO/JDBC）、JIT 代码缓存、GC 结构。
        这条探针给的就是「堆之外至少还要多少」的量级。

  边界：Windows + 非容器化，无 cgroup 上限，量的是堆外开销量级，不是 Linux 容器实测。

  运行：powershell -NoProfile -File learning-docs\tools\tmp\m16-02-heap-headroom.ps1
#>

$ErrorActionPreference = 'Stop'
$Dir = 'D:\Projects\Persion\ai-customer-service\learning-docs\tools\tmp'
$Java = 'D:\DevTools\jdk\jdk17.0.19\bin\java.exe'
$Src = Join-Path $Dir 'm16-02-heap-headroom.java'
$Out = Join-Path $Dir 'm16-02-heap-headroom.out.txt'

if (-not (Test-Path $Java)) { throw "java not found: $Java" }
if (-not (Test-Path $Src)) { throw "source not found: $Src" }

Write-Host "java  : $Java"
Write-Host "flags : -Xms256m -Xmx256m"
Write-Host ''

$proc = Start-Process -FilePath $Java `
    -ArgumentList '-Xms256m', '-Xmx256m', $Src `
    -RedirectStandardOutput $Out -PassThru -WindowStyle Hidden

$peak = 0
$samples = 0
while (-not $proc.HasExited -and $samples -lt 60) {
    Start-Sleep -Milliseconds 250
    $proc.Refresh()
    if ($proc.HasExited) { break }
    $ws = $proc.WorkingSet64
    if ($ws -gt $peak) { $peak = $ws }
    $samples++
}
$proc.WaitForExit()

Write-Host '--- JVM self report ---'
Get-Content $Out -Encoding utf8 | ForEach-Object { Write-Host "  $_" }
Write-Host ''
Write-Host '--- RSS sampling ---'
Write-Host ("  samples              = {0}" -f $samples)
Write-Host ("  peak WorkingSet64    = {0:N1} MiB   (Windows 的 WorkingSet64 ≈ RSS)" -f ($peak / 1MB))
Write-Host ("  peak / 256Mi heap    = {0:N2}x" -f (($peak / 1MB) / 256))
Write-Host ''
Write-Host 'How to read it: peak RSS minus heap = the off-heap part that MUST fit under the container memory limit.'
Write-Host 'If (heap + overhead) > memory limit, the container gets OOMKilled (exit code 137) even though java never threw OOM.'
