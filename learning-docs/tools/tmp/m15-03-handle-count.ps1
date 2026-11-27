# m15-03-handle-count.ps1
# 目的：在同一进程内真实观测「一个打开的文件 = 一个句柄」，
#       证明句柄是独立于内存的第二种稀缺资源，且能被回收（不泄漏）。
#
# 【性质说明】只读打开同一个临时文件 2000 次，不联网、不改仓库文件。
#   句柄数取自 Get-Process .HandleCount（Windows 内核给本进程的句柄表大小），是真实计数。
#
# 运行：pwsh -File .\m15-03-handle-count.ps1

$f = "$env:TEMP\m15-03-handle.txt"
Set-Content -Path $f -Value "probe" -NoNewline

$before = (Get-Process -Id $PID).HandleCount
$h = [System.Collections.Generic.List[object]]::new()
for ($i = 0; $i -lt 2000; $i++) { $h.Add([System.IO.File]::OpenRead($f)) }
$mid = (Get-Process -Id $PID).HandleCount
$h | ForEach-Object { $_.Dispose() }
$h.Clear()
[System.GC]::Collect()
$after = (Get-Process -Id $PID).HandleCount

"before={0}  after_open_2000={1}  delta={2}  after_close={3}" -f $before, $mid, ($mid - $before), $after

# 读法：
#   1) delta 精确等于打开次数 → 「一条连接/一个文件」在内核里就是表里的一行，与字节数无关；
#   2) 关闭后回落到 before 附近（允许 ±几个的抖动，来自 GC/线程池）→ 没有泄漏；
#   3) Linux 侧同一件事的名字叫 fd，上限由 ulimit -n / fs.file-max 决定，撑爆报 EMFILE
#      "Too many open files"；本机是 Windows，没有该上限，所以这个坑只能在 Linux 容器里复现，
#      本文不做 Linux 本机实测。
