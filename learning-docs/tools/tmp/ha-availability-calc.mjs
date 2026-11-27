// 可用性串联/并联实算（教学参数，非本仓实测 SLO）
const pct = (x, d = 5) => (x * 100).toFixed(d) + '%';
const round = (x, n = 6) => Number(x.toFixed(n));

// 1) 串联：n 个组件，每个可用性 a
const series = (a, n) => Math.pow(a, n);

// 2) 并联：k 个副本，每个可用性 a，且必须有"切换/负载均衡"才算并联
const parallel = (a, k) => 1 - Math.pow(1 - a, k);

// 3) 年停机预算（分钟）
const YEAR_MIN = 365 * 24 * 60;

console.log('=== 表1 串联相乘：一条请求经过 n 个组件，每个 a ===');
console.log('n'.padStart(2), 'a=0.9990'.padStart(14), 'a=0.9995'.padStart(14), 'a=0.9999'.padStart(14));
for (let n = 1; n <= 7; n++) {
  console.log(
    String(n).padStart(2),
    pct(series(0.999, n)).padStart(14),
    pct(series(0.9995, n)).padStart(14),
    pct(series(0.9999, n)).padStart(14)
  );
}

console.log('\n=== 表2 年停机预算（分钟/年，按 525600 分钟口径）===');
for (const a of [0.99, 0.999, 0.9995, 0.9999, 0.99999]) {
  const down = (1 - a) * YEAR_MIN;
  console.log(pct(a).padStart(10), '年度不可用', round(down, 2).toString().padStart(10), '分钟 ≈', round(down / 60, 2).toString().padStart(8), '小时');
}

console.log('\n=== 表3 并联冗余：k 个副本（前提：有健康检查+切换）===');
for (const a of [0.99, 0.999]) {
  for (let k = 1; k <= 3; k++) {
    console.log('a=' + pct(a).padStart(9), 'k=' + k, '并联后', pct(parallel(a, k), 7).padStart(12));
  }
}

console.log('\n=== 表4 共享单点封顶：应用层 3 副本 + 1 个 MySQL 单点 ===');
const app = parallel(0.99, 3);
const chainWithSingleDb = app * 0.99;
const chainWithHaDb = app * parallel(0.99, 2);
console.log('应用层 3 副本并联 =', pct(app));
console.log('再串联 1 个 MySQL 单点(0.99) =', pct(chainWithSingleDb), ' <- 被下游单点封顶');
console.log('MySQL 换成 2 副本(0.9999) =', pct(chainWithHaDb));

console.log('\n=== 表5 本项目链路示意（参数为教学假设，不是本仓实测值）===');
const hop = { 'api-gateway': 0.9995, 'ai-cs-chat': 0.999, 'nacos(服务发现)': 0.999, 'mysql(单实例)': 0.999, redis: 0.999, chroma: 0.999, 'LLM 供应商 A': 0.99 };
let acc = 1;
for (const [k, v] of Object.entries(hop)) {
  acc *= v;
  console.log(k.padEnd(18), pct(v).padStart(10), '累计', pct(acc).padStart(12));
}
const base = acc;
// 只把 chat 提到 6 个 9，其余不动
const chatUp = acc / hop['ai-cs-chat'] * 0.999999;
// 只把 LLM 单供应商换成双供应商并联
const llmUp = acc / hop['LLM 供应商 A'] * parallel(0.99, 2);
console.log('\n基线链路 =', pct(base));
console.log('把 ai-cs-chat 做到 99.9999%（6 个 9）后 =', pct(chatUp), ' 提升', round((chatUp - base) * 100, 4), '个百分点');
console.log('把 LLM 换成双供应商路由后 =', pct(llmUp), ' 提升', round((llmUp - base) * 100, 4), '个百分点');

console.log('\n=== 表6 三台"独立 Redis 单机"为什么不算并联 ===');
console.log('若真有 sentinel 自动切换：3 副本 a=0.999 ->', pct(parallel(0.999, 3), 7));
const noFailover = 0.999; // 没有切换 = 客户端只连一个，可用性就是那一台
console.log('无切换/无复制（本仓现状）：可用性仍是单机 ->', pct(noFailover), '；数据侧冗余 = 0（各自独立数据集）');
