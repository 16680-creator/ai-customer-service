// m15-03-select-vs-epoll.mjs
// 目的：量化「select/poll 式全量轮询」与「epoll 式就绪链表」在连接数增长时的算法差异。
//
// 【性质说明 · 必读】这是用 JS 数组遍历做的「算法级模拟」，不是内核 syscall 实测：
//   - 测的是「每个事件循环 tick 应用侧要碰多少个 fd」，不是 epoll_wait 与 select 的
//     系统调用耗时差；
//   - 「数据到达」这一侧的事件由预生成的调度表提供（等价于网卡/软中断把 fd 挂进就绪链表），
//     生成调度表的时间不计入两者，保证对比公平；
//   - 真实内核差异（一次 select 要 copy 一遍 fd 集合到内核、epoll_ctl 一次注册）本脚本不测。
// 结论只能用于解释「为什么 O(N) 轮询在 1 万连接时不可用」这个量级判断，不能当压测数据引用。
//
// 运行：node .\m15-03-select-vs-epoll.mjs

const TICKS = 200;            // 一个 tick ≈ 一次事件循环 / 一次 select 返回
const READY_RATIO = 0.01;     // 每个 tick 平均 1% 的连接有数据可读
const SIZES = [1000, 5000, 10000];

// 可复现的伪随机：线性同余，固定种子 → 每次跑输出一致
function rng(seed) {
  let s = seed >>> 0;
  return () => ((s = (Math.imul(s, 1664525) + 1013904223) >>> 0) / 4294967296);
}

// 预生成「到达调度表」：arrivals[t] = 本 tick 由网络侧变为可读的 fd 下标数组
function buildArrivals(n, ticks, ratio, seed) {
  const rand = rng(seed);
  const arrivals = [];
  let total = 0;
  for (let t = 0; t < ticks; t++) {
    const hit = [];
    for (let i = 0; i < n; i++) {
      if (rand() < ratio) hit.push(i);
    }
    total += hit.length;
    arrivals.push(hit);
  }
  return { arrivals, total };
}

// 模式 A：select/poll —— 每次唤醒都要把全部 n 个 fd 过一遍才知道谁就绪
function runPoll(n, arrivals) {
  const ready = new Uint8Array(n);
  let fdVisits = 0, handled = 0;
  const t0 = process.hrtime.bigint();
  for (const hit of arrivals) {
    for (let i = 0; i < n; i++) {
      fdVisits++;                       // ← 无论有没有数据，这 n 次检查都跑不掉
      if (ready[i]) { handled++; ready[i] = 0; }
    }
  }
  const t1 = process.hrtime.bigint();
  return { fdVisits, handled, ms: Number(t1 - t0) / 1e6 };
}

// 模式 B：epoll —— 有数据的一方把自己挂进就绪链表，应用只看就绪链表
function runEpoll(n, arrivals) {
  const queue = [];                     // 就绪链表
  let fdVisits = 0, handled = 0;
  const t0 = process.hrtime.bigint();
  for (const hit of arrivals) {
    for (const fd of hit) queue.push(fd);   // O(1) 挂链，由网络侧完成
    while (queue.length > 0) {
      const fd = queue.pop();
      fdVisits++;                        // 只碰就绪的 fd
      handled++;
      void fd;
    }
  }
  const t1 = process.hrtime.bigint();
  return { fdVisits, handled, ms: Number(t1 - t0) / 1e6 };
}

console.log(`模拟参数：TICKS=${TICKS}  READY_RATIO=${READY_RATIO}  每 tick 平均活跃连接数 = N*${READY_RATIO}`);
console.log('');
console.log('连接数N | 到达事件数 | select式fd检查次数 | epoll式fd检查次数 | select耗时ms | epoll耗时ms | 检查次数比');
console.log('--------|-----------|-------------------|-------------------|--------------|-------------|----------');

for (const n of SIZES) {
  const { arrivals, total } = buildArrivals(n, TICKS, READY_RATIO, 20261127);
  const a = runPoll(n, arrivals);
  const b = runEpoll(n, arrivals);
  const ratio = (a.fdVisits / b.fdVisits).toFixed(1);
  console.log(
    `${String(n).padStart(7)} | ${String(total).padStart(9)} | ${String(a.fdVisits).padStart(17)} | ${String(b.fdVisits).padStart(17)} | ${a.ms.toFixed(2).padStart(12)} | ${b.ms.toFixed(2).padStart(11)} | ${ratio.padStart(7)}x`
  );
}

console.log('');
console.log('读法：');
console.log('  1) select 式检查次数 = N * TICKS，与「有多少数据到达」完全无关 —— 连接空转也要付费；');
console.log('  2) epoll 式检查次数 = 到达事件数，与 N 无关 —— 1 万连接里只有 100 个活跃，就只碰 100 个；');
console.log('  3) 真实内核里 select 还受 FD_SETSIZE=1024 上限约束（一次最多表达 1024 个 fd），');
console.log('     poll 去掉了上限但仍是 O(N) 且每次调用要把数组整体拷进内核 —— 本脚本只模拟了 O(N) 这一面。');
