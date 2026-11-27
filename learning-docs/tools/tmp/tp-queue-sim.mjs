// 线程池队列/拒绝策略对照仿真（离散时间步进，1ms 粒度）
// 复现"无界队列把过载变成延迟、有界队列+CallerRuns 把过载变成背压"
// 用法: node learning-docs/tools/tmp/tp-queue-sim.mjs
const CORE = 8;                 // 核心线程
const MAX = 8;                  // 最大线程（= 核心：典型 fixed pool）
const SERVICE_MS = 50;          // 每任务服务时长
const OFFERED_QPS = Number(process.argv[2] ?? 200); // 上游到达速率（可用命令行覆盖）
const HORIZON_MS = 60_000;      // 仿真 60 秒
const CALLERS = 200;            // 调用方线程数（类比 Tomcat maxThreads）

function run({ name, capacity, policy }) {
  const intervalMs = 1000 / OFFERED_QPS;
  let nextArrival = 0;
  const workerBusyUntil = new Array(CORE).fill(0); // 固定池：线程数 = core
  const queue = [];                                // 排队中的任务 { arrivalT }
  const callerBusyUntil = new Array(CALLERS).fill(0);
  const door = [];                                 // 已到达但没抢到调用方线程
  const latencies = [];
  let accepted = 0, rejected = 0, inlineRuns = 0, maxQueue = 0, maxDoor = 0;
  const snap = {};

  const startOnWorker = (slot, task, now) => {
    workerBusyUntil[slot] = now + SERVICE_MS;
    latencies.push(now + SERVICE_MS - task.arrivalT);
  };

  for (let now = 0; now <= HORIZON_MS; now++) {
    // 1) 空闲工作线程从队列取活
    for (let w = 0; w < workerBusyUntil.length; w++) {
      if (workerBusyUntil[w] <= now && queue.length > 0) {
        startOnWorker(w, queue.shift(), now);
      }
    }
    // 2) 新请求到达 -> 门口排队
    while (nextArrival <= now) {
      door.push({ arrivalT: nextArrival });
      nextArrival += intervalMs;
    }
    // 3) 空闲调用方取请求并提交给线程池
    for (let c = 0; c < callerBusyUntil.length && door.length > 0; c++) {
      if (callerBusyUntil[c] > now) continue;
      const task = door.shift();
      accepted++;
      const idle = workerBusyUntil.findIndex((until) => until <= now);
      if (idle >= 0) {
        startOnWorker(idle, task, now);          // 核心线程空闲，直接跑
      } else if (queue.length < capacity) {
        queue.push(task);                        // 核心满 -> 进队列（无界队列等于永远走这里）
      } else if (policy === 'caller-runs') {
        inlineRuns++;                            // 队列满 -> 调用方线程亲自执行
        callerBusyUntil[c] = now + SERVICE_MS;
        latencies.push(SERVICE_MS);
      } else {
        rejected++;                              // 队列满 -> AbortPolicy 抛 RejectedExecutionException
      }
    }
    maxQueue = Math.max(maxQueue, queue.length);
    maxDoor = Math.max(maxDoor, door.length);
    if (now === 10_000 || now === 30_000 || now === 60_000) snap[now] = queue.length;
  }

  latencies.sort((a, b) => a - b);
  const round2 = (x) => Math.round(x * 100) / 100;
  const q = (p) => latencies.length ? round2(latencies[Math.min(latencies.length - 1, Math.floor(p * latencies.length))]) : 0;
  return {
    策略: name,
    '排队@10s': snap[10_000],
    '排队@30s': snap[30_000],
    '排队@60s': snap[60_000],
    峰值排队: maxQueue,
    完成数: latencies.length,
    拒绝数: rejected,
    CallerRuns次数: inlineRuns,
    'p50(ms)': q(0.5),
    'p95(ms)': q(0.95),
    'p99(ms)': q(0.99),
    'max(ms)': round2(latencies[latencies.length - 1] ?? 0),
    '门口峰值': maxDoor,
  };
}

const cases = [
  { name: '无界队列+Abort', capacity: Number.MAX_SAFE_INTEGER, policy: 'abort' },
  { name: '有界1000+Abort', capacity: 1000, policy: 'abort' },
  { name: '有界1000+CallerRuns', capacity: 1000, policy: 'caller-runs' },
  { name: '有界200+CallerRuns', capacity: 200, policy: 'caller-runs' },
];

const supply = CORE * 1000 / SERVICE_MS;
const COLS = ['排队@10s', '排队@30s', '排队@60s', '峰值排队', '完成数', '拒绝数', 'CallerRuns次数', 'p50(ms)', 'p95(ms)', 'p99(ms)', 'max(ms)'];
console.log(`基线: core=${CORE} max=${MAX} 服务=${SERVICE_MS}ms 吞吐上限 ${supply}/s，到达 ${OFFERED_QPS}qps（为上限的 ${(OFFERED_QPS / supply * 100).toFixed(0)}%），仿真 ${HORIZON_MS / 1000}s，调用方线程 ${CALLERS}`);
console.table(cases.map((c) => {
  const row = run(c);
  const out = { 策略: row.策略 };
  for (const k of COLS) out[k] = row[k];
  return out;
}));
console.log('说明: 排队=队列内任务数(个)；延迟=提交到完成(ms)；完成数含 CallerRuns 内联执行的任务；门口峰值=已到达但无调用方线程可接的最大任务数');
