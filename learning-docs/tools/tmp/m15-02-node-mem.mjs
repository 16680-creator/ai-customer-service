// learning-docs/tools/tmp/m15-02-node-mem.mjs
// 目的：用 Node 自身把 process.memoryUsage() 的 rss / heapUsed / external / arrayBuffers
// 四者的关系量出来——尤其是「rss 为什么大于 heapUsed」，以及「申请了不等于驻留」。
// 纯内存读写 + 算术，不联网、不写文件。
import { setTimeout as sleep } from 'node:timers/promises';

const MB = 1024 * 1024;
const mb = (n) => (n / MB).toFixed(1).padStart(8);

function snap(stage) {
  const m = process.memoryUsage();
  console.log(
    `${stage.padEnd(30)} rss=${mb(m.rss)}MB heapTotal=${mb(m.heapTotal)}MB ` +
      `heapUsed=${mb(m.heapUsed)}MB external=${mb(m.external)}MB arrayBuffers=${mb(m.arrayBuffers)}MB`
  );
  return m;
}

const base = snap('0. 进程刚起来');

// ---- 阶段 1：纯 JS 对象 → 只涨 heapUsed ----
const objects = Array.from({ length: 1_000_000 }, (_, i) => ({ i, s: 'x' + (i % 97) }));
const s1 = snap('1. 100 万个对象（堆内）');
console.log(
  `   -> heapUsed 增加 ${((s1.heapUsed - base.heapUsed) / MB).toFixed(1)}MB，` +
    `rss 增加 ${((s1.rss - base.rss) / MB).toFixed(1)}MB（rss ≈ heap + 引擎/模块/栈，故 rss 一直更高）`
);

// ---- 阶段 2：申请 200MB ArrayBuffer，但一个字节都不碰 ----
const ab = new ArrayBuffer(200 * MB);
const s2 = snap('2. new ArrayBuffer(200MB) 未触碰');
console.log(
  `   -> arrayBuffers 增加 ${((s2.arrayBuffers - s1.arrayBuffers) / MB).toFixed(1)}MB，` +
    `rss 只增加 ${((s2.rss - s1.rss) / MB).toFixed(1)}MB：虚拟地址登记了，物理页还没分配`
);

// ---- 阶段 3：每 4KB（一页）写一个字节，把页"踩实" ----
const view = new Uint8Array(ab);
for (let off = 0; off < view.length; off += 4096) view[off] = 1;
const s3 = snap('3. 每 4KB 写 1 字节（共 51200 页）');
console.log(
  `   -> rss 从 ${((s2.rss) / MB).toFixed(1)}MB 涨到 ${((s3.rss) / MB).toFixed(1)}MB，` +
    `增加 ${((s3.rss - s2.rss) / MB).toFixed(1)}MB：缺页异常把 200MB 物理页一页页换进来了`
);

// ---- 阶段 4：把 ArrayBuffer 释放掉，看 rss 是否立刻回落 ----
objects.length = 0;
const s4 = snap('4. 丢掉引用后（GC 未强制）');
console.log(
  `   -> 只是"不再引用"，V8 还没回收；rss 不会因为一句 = null 就还给操作系统`
);

await sleep(200);
const s5 = snap('5. 200ms 后');
console.log(
  `   -> heapUsed ${((s4.heapUsed - s5.heapUsed) / MB).toFixed(1)}MB，` +
    `rss ${((s5.rss - s4.rss) / MB).toFixed(1)}MB：RSS 是"高水位"指标，不是"当前活对象"指标`
);

console.log('\n=== 结论算术（全部来自上面同一进程的真实读数）===');
console.log(`rss - heapUsed = ${((s3.rss - s3.heapUsed) / MB).toFixed(1)}MB  <- 差值来自：ArrayBuffer/堆外 + V8 自身 + 共享库 + 线程栈`);
console.log(`heapTotal - heapUsed = ${((s3.heapTotal - s3.heapUsed) / MB).toFixed(1)}MB  <- 堆内"已拿到但没用"的空洞（碎片/预留）`);
