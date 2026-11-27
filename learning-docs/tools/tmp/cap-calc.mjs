// 容量换算链演示脚本（13-稳定性工程/02-容量规划与性能预算 的 C 级证据）
// 全部为【假设值】，脚本只做换算与偏差对比，不含任何实测吞吐/延迟
// 运行：cd D:\Projects\Persion\ai-customer-service ; node learning-docs\tools\tmp\cap-calc.mjs

// ===== 假设（改这里做你自己的推演）=====
const DAU = 10000;            // 日活
const PER_USER = 20;          // 人均日请求数
const PEAK_HOUR_SHARE = 0.12; // 峰值 1 小时占全天请求的比例（二八原则的量化版）
const PEAK_SECOND = 2.5;      // 峰秒系数：最忙 1 秒 / 峰值小时均值
const SLOTS = 100;            // 单实例并发槽位 = Tomcat 200 线程 × 50% 水位
// ========================================

const f = (x, n) => Number(x).toFixed(n);
const daily = DAU * PER_USER;
const avgQps = daily / 86400;
const peakHourQps = (daily * PEAK_HOUR_SHARE) / 3600;
const peakQps = peakHourQps * PEAK_SECOND;

console.log('== 1. 业务量 -> 请求量 ==');
console.log('日请求量        =', daily);
console.log('日均 QPS        =', f(avgQps, 2));
console.log('峰值小时 QPS    =', f(peakHourQps, 2));
console.log('峰值 QPS(峰秒)  =', f(peakQps, 2));

console.log('\n== 2. 线性外推（无排队模型）：N = ceil(QPS x T0 / 槽位) ==');
const plan = {};
for (const [name, T0] of [['普通接口', 0.08], ['AI 对话', 8]]) {
  const conc = peakQps * T0;                 // Little 定律：并发 = 到达率 x 停留时间
  const inst = Math.max(1, Math.ceil(conc / SLOTS));
  const util = (conc / (inst * SLOTS)) * 100;
  const instRho50 = Math.max(1, Math.ceil(conc / (SLOTS * 0.5)));
  plan[name] = { T0, conc, inst, util, instRho50 };
  console.log(
    name + ': 需要并发=' + f(conc, 2) + ' 实例=' + inst +
    ' 水位=' + f(util, 1) + '% | 按 50% 水位实例=' + instRho50 +
    ' 多出=' + f((instRho50 / inst - 1) * 100, 1) + '%'
  );
}

console.log('\n== 3. 排队放大：M/M/1 响应时间 = T0 / (1 - rho) ==');
for (const rho of [0.3, 0.5, 0.6, 0.7, 0.8, 0.9]) {
  console.log('rho=' + rho + ' 延迟放大=' + f(1 / (1 - rho), 2) + 'x');
}

console.log('\n== 4. 延迟预算反推水位：预算 B 下 rho <= 1 - T0/B ==');
for (const [name, T0] of [['AI 对话', 8], ['普通接口', 0.08]]) {
  for (const k of [1, 2, 3]) {
    const B = T0 * k;
    const rho = 1 - T0 / B;
    console.log(
      name + ' T0=' + T0 + 's 预算B=' + f(B, 2) + 's(' + k + 'xT0) -> rho上限=' + f(rho, 3) +
      (rho <= 0 ? '  [不可行：预算=均值，没有任何排队余量]' : ' 实例=' + Math.max(1, Math.ceil(plan[name].conc / (SLOTS * rho))))
    );
  }
}

console.log('\n== 5. 连接池 = 隐藏的 QPS 上限：cap = 池大小 / 单请求持连接时间 ==');
const POOL = 10;   // tools/nacos-config/ai-cs-*.yml 的 maximum-pool-size: 10
for (const hold of [0.005, 0.05, 0.5]) {
  const cap = POOL / hold;
  console.log('池=10 持连接=' + f(hold * 1000, 0) + 'ms -> 上限 ' + f(cap, 1) +
    ' QPS, 峰值 ' + f(peakQps, 2) + ' QPS 占用 ' + f((peakQps / cap) * 100, 1) + '%');
}

console.log('\n== 6. 小流量外推偏差（同一份算力，两套口径）==');
const n = plan['AI 对话'].inst;
console.log('线性口径: ' + n + ' 实例 x ' + SLOTS + ' 槽位 / 8s = ' + f((n * SLOTS) / 8, 2) + ' QPS');
console.log('守 16s 预算(50% 水位): ' + n + ' 实例 = ' + f((n * SLOTS * 0.5) / 8, 2) + ' QPS');
console.log('外推高估 = ' + f((n * SLOTS) / 8 / ((n * SLOTS * 0.5) / 8), 2) + 'x');
