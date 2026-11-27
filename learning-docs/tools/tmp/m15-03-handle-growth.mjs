// m15-03-handle-growth.mjs —「观测口径选错会得出相反结论」的反面教材
//
// 目的：演示一个真实的踩坑：想数「进程到底开了多少个 fd/句柄」，
//       于是用 Node 自带的 process.report.getReport().libuv 去数 libuv 活动句柄表，
//       结果打开 2000 个文件后句柄总数纹丝不动（一直是 10）。
//       原因：fs 的同步打开句柄不进 libuv 的活动句柄表，那 10 个是 event loop 自身的
//       async/check/timer/idle/prepare/pipe/loop。
//
// 正确的观测口径见同目录 m15-03-handle-count.ps1（Get-Process .HandleCount，同进程内计数）。
//
// 【性质说明】只读打开同一个临时文件 N 次；不联网、不写仓库。
// 运行：node .\m15-03-handle-growth.mjs

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const target = path.join(os.tmpdir(), 'm15-03-probe.txt');
fs.writeFileSync(target, 'io-multiplexing probe\n');

function libuvTotal() {
  const report = process.report.getReport();
  const counts = {};
  for (const h of report.libuv) {
    const t = h.type || h.handleType || 'unknown';
    counts[t] = (counts[t] || 0) + 1;
  }
  const top = Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k}=${v}`).join('  ');
  return { total: report.libuv.length, top };
}

function show(tag) {
  const { total, top } = libuvTotal();
  console.log(`${tag.padEnd(24)} libuv 活动句柄表=${String(total).padStart(4)}  [${top}]`);
  return total;
}

console.log(`探测目标文件：${target}`);
console.log(`本进程 PID = ${process.pid}（可用 Get-Process -Id ${process.pid} 从外部核对真实句柄数）`);
console.log('');

const before = show('打开文件之前');
const N = 2000;
const handles = [];
for (let i = 0; i < N; i++) handles.push(fs.openSync(target, 'r'));
const after = show(`打开 ${N} 个文件之后`);

console.log('');
console.log(`libuv 活动句柄表净增 = ${after - before}；rss = ${(process.memoryUsage().rss / 1024 / 1024).toFixed(1)} MB`);
for (const fd of handles) fs.closeSync(fd);
const closed = show('全部关闭之后');
console.log(`净残留 = ${closed - before}`);
console.log('');
console.log('结论（负面结果，本身就是要讲的坑）：');
console.log('  1) 表是 0 增长，但内核里确实多了 2000 个句柄 —— 说明这个口径数不到它们；');
console.log('  2) 用错口径的后果是反向结论：会误判「没有 fd 压力/没有泄漏」；');
console.log('  3) 正确口径：Windows 用 Get-Process .HandleCount（见 m15-03-handle-count.ps1，');
console.log('     delta 精确等于打开次数）；Linux 用 ls /proc/<pid>/fd | wc -l 或 lsof -p <pid> | wc -l。');
console.log('  4) 内存里看不出这件事：句柄满和堆满（OOM）是两种完全不同的故障，日志关键词也不同。');
