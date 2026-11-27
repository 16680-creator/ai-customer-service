// learning-docs/tools/tmp/m15-cfs-sim.mjs
// 用途：按 Linux CFS 的权重表与 sched_slice 公式做纯算术模拟，给出"nice 如何变成 CPU 份额"的直觉。
// 注意：这是模拟（公式来自内核文档的重量级换算），不是本机内核实测——本机是 Windows，无 CFS。
// 跑法：node learning-docs\tools\tmp\m15-cfs-sim.mjs

// Linux kernel: prio_to_weight[]，nice -20..19 共 40 档，nice 0 的权重是 1024
const NICE0_WEIGHT = 1024;
const weight = (nice) => Math.round(NICE0_WEIGHT * Math.pow(1.25, -nice));
const SHARE = (n) => (weight(n) / 1024).toFixed(3); // 相对 nice 0 的 CPU 份额（单个任务）

console.log('=== 表1 nice → 权重 → 相对 nice 0 的 CPU 份额（单任务） ===');
console.log('nice   权重      相对份额   备注');
for (const n of [-20, -10, -5, 0, 5, 10, 19]) {
  const note = n === 0 ? '默认优先级' : n > 0 ? '让出 CPU' : '抢占 CPU';
  console.log(`${String(n).padStart(4)}  ${String(weight(n)).padStart(6)}   ${SHARE(n).padStart(7)}   ${note}`);
}

// 三个任务争一个核：nice 0 / nice 5 / nice 10，各自的长期 CPU 份额 = w_i / Σw
function shares(nices) {
  const sum = nices.reduce((a, n) => a + weight(n), 0);
  return nices.map((n) => weight(n) / sum);
}

console.log('\n=== 表2 三个任务争 1 个核（nice = 0 / 5 / 10） ===');
const nices = [0, 5, 10];
const s = shares(nices);
nices.forEach((n, i) => {
  console.log(`nice=${String(n).padStart(2)}  权重=${String(weight(n)).padStart(4)}  长期 CPU 份额=${(s[i] * 100).toFixed(1)}%`);
});

// vruntime 直觉：sched_latency=24ms（任务数<=8 时的默认周期），sched_slice_i = 周期 × w_i/Σw
const LATENCY_MS = 24;
console.log('\n=== 表3 一个调度周期 24ms 怎么切（就是表2 那三个任务） ===');
let acc = 0;
nices.forEach((n, i) => {
  const slice = LATENCY_MS * s[i];
  const start = acc;
  acc += slice;
  console.log(`nice=${String(n).padStart(2)}  时间片=${slice.toFixed(2)}ms  时间轴区间=[${start.toFixed(2)}, ${acc.toFixed(2)}]ms`);
});

// 关键点：跑完一个时间片后各任务 vruntime 增量相同（这就是"公平"的定义）
console.log('\n=== 表4 为什么这样是公平的：跑完自己的时间片后 vruntime 增量相同 ===');
nices.forEach((n, i) => {
  const delta = (LATENCY_MS * s[i]) * (1024 / weight(n)); // vruntime 增量 = 实际运行时间 × (NICE_0_WEIGHT / weight)
  console.log(`nice=${String(n).padStart(2)}  跑 ${(LATENCY_MS * s[i]).toFixed(2)}ms  →  vruntime 增量 ${delta.toFixed(4)}ms`);
});

// 交互式任务：睡 100ms 后只要求 2ms CPU，vruntime 仍最小，于是被优先调度
console.log('\n=== 表5 睡眠如何变成优先级：睡 100ms 的任务 vruntime 落后多少 ===');
const awake = [
  { name: 'CPU 密集 A(nice0)', slice: 8, vr: 0 },
  { name: 'CPU 密集 B(nice0)', slice: 8, vr: 0 },
  { name: '交互任务 C(nice0)', slice: 8, vr: -100 },
];
awake.sort((a, b) => a.vr - b.vr);
awake.forEach((t, i) => console.log(`${i + 1}. 被选中：${t.name}  vruntime=${t.vr}ms`));
console.log('说明：vruntime 是"欠账"的顺序号，负数 = 欠得最多 = 下一个上 CPU');
