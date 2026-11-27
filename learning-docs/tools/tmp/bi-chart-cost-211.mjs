// 06-BI看板与ECharts可视化 的 C 级实测脚本（第 211 号临时脚本）
// 只做算术与库内调用：不启动服务、不联网、不读数据库。
// 运行：cd D:\Projects\Persion\ai-customer-service\learning-docs && node tools\tmp\bi-chart-cost-211.mjs
import { createRequire } from "node:module";
import { gzipSync } from "node:zlib";

const require = createRequire("D:/Projects/Persion/ai-customer-service/ai-cs-frontend/package.json");
const echarts = require("echarts");

const median = (xs) => {
  const s = [...xs].sort((a, b) => a - b);
  return s[Math.floor(s.length / 2)];
};
const kb = (n) => (n / 1024).toFixed(1).padStart(7);

// ---------- 模拟数据：一年小时级时间序列（10 000 点） ----------
function makeSeries(n) {
  const xs = [];
  const ys = [];
  for (let i = 0; i < n; i++) {
    // 日周期 + 周周期 + 一个尖峰（第 7350 点，模拟一次故障/活动）
    const day = Math.sin((2 * Math.PI * i) / 24);
    const week = Math.sin((2 * Math.PI * i) / (24 * 7));
    let v = 500 + 120 * day + 60 * week + ((i * 37) % 11) - 5;
    if (i === 7350) v += 900; // 单点尖峰：降采样最容易吃掉它
    xs.push(i);
    ys.push(Math.round(v));
  }
  return { xs, ys };
}

function buildOption(xs, ys) {
  return {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: xs },
    yAxis: { type: "value" },
    series: [{ type: "line", data: ys, smooth: true }],
  };
}

// ---------- 降采样 A：桶均值 ----------
function bucketMean(xs, ys, target) {
  const size = xs.length / target;
  const ox = [];
  const oy = [];
  for (let b = 0; b < target; b++) {
    const from = Math.floor(b * size);
    const to = Math.floor((b + 1) * size);
    let sum = 0;
    for (let i = from; i < to; i++) sum += ys[i];
    ox.push(xs[Math.floor((from + to - 1) / 2)]);
    oy.push(sum / Math.max(1, to - from));
  }
  return { xs: ox, ys: oy };
}

// ---------- 降采样 B：LTTB（Largest-Triangle-Three-Buckets） ----------
function lttb(xs, ys, target) {
  const n = xs.length;
  if (target >= n || target <= 2) return { xs: [...xs], ys: [...ys] };
  const sampledX = [xs[0]];
  const sampledY = [ys[0]];
  const every = (n - 2) / (target - 2);
  let a = 0;
  for (let i = 0; i < target - 2; i++) {
    // 下一个桶的平均点
    let avgX = 0;
    let avgY = 0;
    let start = Math.floor((i + 1) * every) + 1;
    let end = Math.floor((i + 2) * every) + 1;
    if (end > n) end = n;
    const len = Math.max(1, end - start);
    for (let j = start; j < end; j++) {
      avgX += xs[j];
      avgY += ys[j];
    }
    avgX /= len;
    avgY /= len;
    // 当前桶里找面积最大的点
    const rangeStart = Math.floor(i * every) + 1;
    const rangeEnd = Math.floor((i + 1) * every) + 1;
    let maxArea = -1;
    let chosen = rangeStart;
    for (let j = rangeStart; j < rangeEnd && j < n; j++) {
      const area = Math.abs(
        (xs[a] - avgX) * (ys[j] - ys[a]) - (xs[a] - xs[j]) * (avgY - ys[a]),
      ) / 2;
      if (area > maxArea) {
        maxArea = area;
        chosen = j;
      }
    }
    sampledX.push(xs[chosen]);
    sampledY.push(ys[chosen]);
    a = chosen;
  }
  sampledX.push(xs[n - 1]);
  sampledY.push(ys[n - 1]);
  return { xs: sampledX, ys: sampledY };
}

// ---------- 误差：把降采样结果线性插值回原始 x，逐点比对 ----------
function reconstructError(xs, ys, sx, sy) {
  let i = 0;
  const errs = [];
  for (let k = 0; k < xs.length; k++) {
    while (i < sx.length - 2 && sx[i + 1] < xs[k]) i++;
    const x0 = sx[i];
    const x1 = sx[i + 1];
    const t = x1 === x0 ? 0 : (xs[k] - x0) / (x1 - x0);
    const approx = sy[i] + t * (sy[i + 1] - sy[i]);
    errs.push(ys[k] - approx);
  }
  const abs = errs.map(Math.abs);
  const mae = abs.reduce((a, b) => a + b, 0) / abs.length;
  const rmse = Math.sqrt(errs.reduce((a, b) => a + b * b, 0) / errs.length);
  const maxAbs = Math.max(...abs);
  return { mae, rmse, maxAbs };
}

// ---------- 1) 数据规模 vs option 负载 ----------
const N = 10000;
const { xs, ys } = makeSeries(N);
const mean500 = bucketMean(xs, ys, 500);
const lttb500 = lttb(xs, ys, 500);
const lttb2000 = lttb(xs, ys, 2000);

console.log("=== 1) 同一个折线图，不同数据量下的 option 负载（原始 JSON / gzip） ===");
const cases = [
  ["10 000 点（原始）", xs, ys],
  ["2 000 点（LTTB）", lttb2000.xs, lttb2000.ys],
  ["500 点（LTTB）", lttb500.xs, lttb500.ys],
  ["500 点（桶均值）", mean500.xs, mean500.ys],
];
const optionBytes = new Map();
for (const [name, cx, cy] of cases) {
  const json = JSON.stringify(buildOption(cx, cy));
  const raw = Buffer.byteLength(json, "utf8");
  const gz = gzipSync(Buffer.from(json), { level: 9 }).length;
  optionBytes.set(name, raw);
  console.log(`  ${name.padEnd(20)} option JSON=${kb(raw)}KB  gzip=${kb(gz)}KB  series.data 项数=${cy.length}`);
}
const drop =
  ((optionBytes.get("10 000 点（原始）") - optionBytes.get("500 点（LTTB）")) /
    optionBytes.get("10 000 点（原始）")) * 100;
console.log(`  → 10 000 点降到 500 点，option 体积下降 ${drop.toFixed(1)}%`);

// ---------- 2) setOption + 渲染耗时（echarts 5.6.0 SSR / SVG 渲染器） ----------
console.log("\n=== 2) setOption + 渲染耗时（node + echarts 5.6.0 SSR，SVG 渲染器；非浏览器 canvas） ===");
function timeOnce(cx, cy, reuseChart) {
  const chart = reuseChart || echarts.init(null, null, { renderer: "svg", ssr: true, width: 1200, height: 380 });
  const t0 = performance.now();
  chart.setOption(buildOption(cx, cy));
  chart.renderToSVGString();
  const dt = performance.now() - t0;
  return { dt, chart };
}
for (const [name, cx, cy] of cases) {
  const runs = [];
  let svgBytes = 0;
  for (let r = 0; r < 5; r++) {
    const { dt, chart } = timeOnce(cx, cy, null); // 每轮新实例：等于"先 dispose 再 init"的写法
    svgBytes = chart.renderToSVGString().length;
    runs.push(dt);
    chart.dispose();
  }
  console.log(`  ${name.padEnd(20)} 中位耗时=${median(runs).toFixed(1).padStart(7)}ms  最小=${Math.min(...runs).toFixed(1)}ms  最大=${Math.max(...runs).toFixed(1)}ms  svg≈${kb(svgBytes)}KB`);
}

// ---------- 3) 复用实例 vs 每次重建（同一份 500 点数据反复更新） ----------
console.log("\n=== 3) 实例复用 vs 每次 dispose+init（500 点数据更新 20 次） ===");
{
  const runs = [];
  const chart = echarts.init(null, null, { renderer: "svg", ssr: true, width: 1200, height: 380 });
  for (let i = 0; i < 20; i++) {
    const t0 = performance.now();
    chart.setOption(buildOption(mean500.xs, mean500.ys));
    chart.renderToSVGString();
    runs.push(performance.now() - t0);
  }
  chart.dispose();
  console.log(`  复用同一实例：20 次更新平均=${(runs.reduce((a, b) => a + b, 0) / runs.length).toFixed(1)}ms  首次=${runs[0].toFixed(1)}ms`);
}
{
  const runs = [];
  for (let i = 0; i < 20; i++) {
    const t0 = performance.now();
    const chart = echarts.init(null, null, { renderer: "svg", ssr: true, width: 1200, height: 380 });
    chart.setOption(buildOption(mean500.xs, mean500.ys));
    chart.renderToSVGString();
    chart.dispose();
    runs.push(performance.now() - t0);
  }
  console.log(`  dispose+init   ：20 次更新平均=${(runs.reduce((a, b) => a + b, 0) / runs.length).toFixed(1)}ms  首次=${runs[0].toFixed(1)}ms`);
}

// ---------- 4) 降采样到 500 点，误差到底多大 ----------
console.log("\n=== 4) 降采样到 500 点：重建误差（把 500 点线性插值回 10 000 点逐点比对） ===");
const rawMax = Math.max(...ys);
const rawMin = Math.min(...ys);
const rawArea = ys.reduce((a, b) => a + b, 0);
for (const [name, s] of [["桶均值", mean500], ["LTTB", lttb500]]) {
  const { mae, rmse, maxAbs } = reconstructError(xs, ys, s.xs, s.ys);
  const sMax = Math.max(...s.ys);
  const sMin = Math.min(...s.ys);
  const sArea = s.ys.reduce((a, b) => a + b, 0);
  const areaErr = Math.abs(sArea / s.ys.length - rawArea / ys.length) / (rawArea / ys.length) * 100;
  console.log(`  ${name.padEnd(4)} MAE=${mae.toFixed(2)}  RMSE=${rmse.toFixed(2)}  单点最大误差=${maxAbs.toFixed(1)}`);
  console.log(`       峰值 ${sMax.toFixed(0)}（原始 ${rawMax}）  谷值 ${sMin.toFixed(0)}（原始 ${rawMin}）  均值偏差=${areaErr.toFixed(2)}%  点数=${s.ys.length}`);
}
console.log(`  原始序列：峰值=${rawMax}（第 ${ys.indexOf(rawMax)} 点），平均值=${(rawArea / ys.length).toFixed(2)}`);

// ---------- 5) 点密度算术：一堆常见"粒度"落在多少点上 ----------
console.log("\n=== 5) 时间范围 × 粒度 = 点数（决定该不该下采样） ===");
const rows = [
  ["1 小时", 60, "1 秒"],
  ["1 天", 24, "1 分钟"],
  ["1 天", 24 * 60, "1 秒"],
  ["7 天", 7 * 24, "1 小时"],
  ["30 天", 30 * 24, "1 小时"],
  ["30 天", 30 * 24 * 12, "5 分钟"],
  ["365 天", 365 * 24, "1 小时"],
  ["365 天", 365 * 24 * 60, "1 分钟"],
];
for (const [range, n, gran] of rows) {
  const flag = n > 5000 ? "→ 必须降采样/聚合下推" : n > 1000 ? "→ 建议 dataZoom" : "→ 可直接画";
  console.log(`  ${String(range).padEnd(8)} 按 ${String(gran).padEnd(5)} = ${String(n).padStart(8)} 点  ${flag}`);
}

// ---------- 6) 饼图扇区角度：几类之后就没法看了 ----------
console.log("\n=== 6) 饼图可读性：Zipf 长尾分布下最小扇区的圆心角（饼图 360°） ===");
for (const k of [3, 5, 8, 12, 20]) {
  const w = Array.from({ length: k }, (_, i) => 1 / (i + 1));
  const sum = w.reduce((a, b) => a + b, 0);
  const minAngle = (Math.min(...w) / sum) * 360;
  const maxAngle = (Math.max(...w) / sum) * 360;
  console.log(`  类别数=${String(k).padStart(2)}  最大扇区=${maxAngle.toFixed(1).padStart(5)}°  最小扇区=${minAngle.toFixed(2).padStart(5)}°  ${minAngle < 5 ? "→ 小于 5°，肉眼不可读" : ""}`);
}
