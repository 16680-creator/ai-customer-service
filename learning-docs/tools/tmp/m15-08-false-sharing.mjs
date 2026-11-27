// m15-08-false-sharing.mjs —— 伪共享（false sharing）耗时对比实验
// 说明：这是真实多核计时（worker_threads 真跑在不同核上，SharedArrayBuffer 真被多个核同时改写），
//       但它只是对 CPU 缓存行争用的「模拟」：node 没有 Java 内存模型，这里也不验证 happens-before，
//       只测量「同一 64B 缓存行里的多个计数器」与「各自独占一条缓存行」两种布局的耗时差。
//       计时窗口用 ready/go 握手夹住，只算纯计算段，不含 worker 启动开销。
// 运行：node m15-08-false-sharing.mjs [每线程迭代次数] [轮数]
import { Worker, isMainThread, workerData, parentPort } from 'node:worker_threads';
import { availableParallelism } from 'node:os';

const ITER = Number(process.argv[2] ?? 100_000_000);
const ROUNDS = Number(process.argv[3] ?? 5);
const WORKERS = Number(process.env.M15_WORKERS ?? Math.min(4, availableParallelism()));

if (!isMainThread) {
  const { sab, index, iter, ctrl } = workerData;
  const view = new Int32Array(sab);
  const flag = new Int32Array(ctrl);
  Atomics.add(flag, 0, 1); // 报到：我准备好了
  while (Atomics.load(flag, 1) === 0) { /* 自旋等发令 */ }
  // 被测代码：对共享数组指定下标做「非原子」读改写，不加锁、不调 Atomics
  for (let k = 0; k < iter; k++) {
    view[index] = view[index] + 1;
  }
  parentPort.postMessage(view[index]);
} else {
  const oneRound = async (stride) => {
    const sab = new SharedArrayBuffer(64 * 8);
    const ctrl = new SharedArrayBuffer(64);
    const flag = new Int32Array(ctrl);
    const done = Array.from({ length: WORKERS }, (_, i) =>
      new Promise((resolve, reject) => {
        const w = new Worker(new URL(import.meta.url), {
          workerData: { sab, index: i * stride, iter: ITER, ctrl },
        });
        w.on('message', resolve);
        w.on('error', reject);
      }),
    );
    while (Atomics.load(flag, 0) < WORKERS) { /* 等全部报到 */ }
    const t0 = process.hrtime.bigint();
    Atomics.store(flag, 1, 1); // 发令开跑
    await Promise.all(done);
    return Number(process.hrtime.bigint() - t0) / 1e6;
  };

  const measure = async (stride) => {
    const times = [];
    for (let r = 0; r < ROUNDS; r++) times.push(await oneRound(stride));
    return times;
  };
  const median = (a) => [...a].sort((x, y) => x - y)[Math.floor(a.length / 2)];
  const f = (v) => v.toFixed(1);

  const sameLine = await measure(1); // 4 个 int32 相邻 → 落在同一条 64B 缓存行
  const spreadLine = await measure(16); // 每个下标相隔 16*4=64B → 各自独占一条缓存行

  console.log('workers         = ' + WORKERS);
  console.log('iterPerWorker   = ' + ITER);
  console.log('rounds          = ' + ROUNDS);
  console.log('sameLineMs      = [' + sameLine.map(f).join(', ') + ']  median=' + f(median(sameLine)));
  console.log('spreadLineMs    = [' + spreadLine.map(f).join(', ') + ']  median=' + f(median(spreadLine)));
  console.log('slowdown        = ' + (median(sameLine) / median(spreadLine)).toFixed(2) + 'x (sameLine / spreadLine)');
}
