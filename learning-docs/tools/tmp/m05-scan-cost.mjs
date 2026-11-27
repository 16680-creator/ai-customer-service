// learning-docs\tools\tmp\m05-scan-cost.mjs
// 目的：估算 OrderTimeoutScheduler 每 5 分钟「扫表 + 串行关单」在 100 万 / 1000 万行 orders 表的代价。
// 全部硬盘/网络参数均为显式假设（本机不许起 MySQL、不许联网，无法实测），输出里逐条打印假设。

const PAGE = 16384;          // InnoDB 页大小 16KB
const USABLE = 15200;        // 每页可用字节（扣页头/页目录，经验值）
const ASSUMPTIONS = {
  '表结构来源': 'deploy/mysql/order-init.sql#L24-L46（16 列，utf8mb4）',
  '行宽': '按 DDL 字段类型逐列累加，含 InnoDB 记录头 13B + 变长列长度表',
  'order_no 长度': 'yyyyMMddHHmmss+01+4 位序号 = 18 字符（见 OrderCreateTxService#L160-L164）',
  'SSD 顺序读': '500 MB/s',
  'SSD 随机读 IOPS': '8000（单行回表 = 1 次随机页读）',
  '订单增速': '本仓无真实订单量样本（demo-data.sql 仅 1 条订单），下面把 1 万/10 万/100 万 单/天 当作假设并列',
  '单笔关单耗时': 'DB 3 次 × 5ms + Feign (1 次 closePay + 商品数×1 次 restoreStock) × 20ms，商品数假设 2 → 65ms',
  '待支付占比': 'PENDING_PAY 行数 = 最近 30 分钟新单 + 积压（假设积压为 0 的正常态）',
  '调度周期': 'fixedDelay = 300000ms（OrderTimeoutScheduler#L29）→ 每轮预算 300s',
};

// ---- 1. 行宽估算 ----
const cols = [
  ['id', 8], ['order_no', 18 + 1], ['user_id', 8],
  ['total_amount', 5], ['discount_amount', 5], ['pay_amount', 5],
  ['full_reduction_amount', 5], ['coupon_amount', 5], ['coupon_id', 8],
  ['payment_method', 6 + 1], ['status', 11 + 1],
  ['pay_time', 5], ['cancel_time', 5], ['expire_time', 5],
  ['create_time', 5], ['update_time', 5],
];
const header = 5 + 6 + 1 + 1; // 记录头 + 事务/回滚指针 + 变长长度表 + 空值位图（近似）
const rowB = cols.reduce((s, [, b]) => s + b, 0) + header;
const idxExpireB = 5 + 8 + 6;   // DATETIME(5) + PK(8) + 二级索引记录头（近似）
const idxStatusB = 11 + 1 + 8 + 6;

const rowsPerPage = (rec) => Math.floor(USABLE / rec);
const pages = (rows, rec) => Math.ceil(rows / rowsPerPage(rec));
const mb = (b) => (b / 1024 / 1024).toFixed(1);
const s = (sec) => sec < 1 ? (sec * 1000).toFixed(0) + ' ms' : sec.toFixed(1) + ' s';

const orderSizes = [1_000_000, 10_000_000];
const dailyVolumes = [10_000, 100_000, 1_000_000];
const perOrderMs = 65;

console.log('=== 假设 ===');
for (const [k, v] of Object.entries(ASSUMPTIONS)) console.log(`- ${k}: ${v}`);
console.log(`- 推导行宽: ${rowB} B/行（列 ${cols.reduce((a, [, b]) => a + b, 0)} B + 记录头 ${header} B）`);
console.log(`- 聚集索引叶子: ${rowsPerPage(rowB)} 行/页；idx_expire_time: ${rowsPerPage(idxExpireB)} 条/页；idx_status: ${rowsPerPage(idxStatusB)} 条/页`);
console.log(`- 每轮时间预算: 300 s；单笔 ${perOrderMs} ms ⇒ 每轮最多处理 ${Math.floor(300000 / perOrderMs)} 单`);

console.log('\n=== 表体积与索引体积（估算） ===');
for (const n of orderSizes) {
  const cl = pages(n, rowB), ie = pages(n, idxExpireB), is = pages(n, idxStatusB);
  console.log(`${(n / 10000) + ' 万行'}: 聚集索引 ${cl} 页 ≈ ${mb(cl * PAGE)} MB | idx_expire_time ${ie} 页 ≈ ${mb(ie * PAGE)} MB | idx_status ${is} 页 ≈ ${mb(is * PAGE)} MB`);
}

console.log('\n=== 执行计划 A：优化器选 idx_expire_time（expire_time < now() 是无下界范围） ===');
console.log('命中行数 ≈ 表中除最近 30 分钟以外【全部】订单；每行都要回表取 16 列判 status。');
for (const n of orderSizes) {
  const matched = n - Math.round(n * 0.001); // 最近 30min 新单占比假设 0.1%
  const idxPages = pages(n, idxExpireB);
  const seqSec = (idxPages * PAGE) / (500 * 1024 * 1024);
  const randSec = matched / 8000;
  const heapMB = mb(matched * 400); // List<Order> 对象按 400B/个（含引用与对象头，假设）
  console.log(`${(n / 10000) + ' 万行'}: 索引顺序读 ${idxPages} 页(${mb(idxPages * PAGE)} MB) ≈ ${s(seqSec)} + 回表 ${matched} 次随机页读 ≈ ${s(randSec)} ⇒ 合计 ${s(seqSec + randSec)}；同时驻留 List<Order> ≈ ${heapMB} MB`);
}

console.log('\n=== 执行计划 B：优化器选 idx_status（PENDING_PAY 选择性高时） ===');
for (const n of orderSizes) {
  for (const d of dailyVolumes) {
    const window = Math.round(d / 288);          // 5 分钟窗口新增
    const pending = window;                       // 正常态积压 ≈ 0
    const rows = pending;
    console.log(`${(n / 10000) + ' 万行 / 日增 ' + d}: 索引仅 ${pages(rows, idxStatusB)} 页，回表 ${rows} 次 ≈ ${s(rows / 8000)} ⇒ ${perOrderMs}ms/单串行处理 ${rows} 单需 ${s(rows * perOrderMs / 1000)}；每轮预算 300s ⇒ ${rows * perOrderMs / 1000 > 300 ? '★超预算，backlog 每轮净增 ' + Math.round(rows - Math.floor(300000 / perOrderMs)) + ' 单' : '在预算内'}`);
  }
}

console.log('\n=== 执行计划 C：无索引全表顺序扫（对照：顺序扫其实更快） ===');
for (const n of orderSizes) {
  const cl = pages(n, rowB);
  console.log(`${(n / 10000) + ' 万行'}: 顺序读 ${cl} 页 ≈ ${mb(cl * PAGE)} MB ⇒ ${s((cl * PAGE) / (500 * 1024 * 1024))}（顺序读便宜，随机回表贵）`);
}

console.log('\n=== 交叉点：多快就必须停止暴力扫表 ===');
for (const pm of [65, 150, 300]) {
  const cap = Math.floor(300000 / pm);
  console.log(`单笔 ${pm}ms ⇒ 每轮 ${cap} 单 ⇒ 临界日增 ≈ ${(cap * 288 / 10000).toFixed(1)} 万单/天（超过它 backlog 单调增长）`);
}

console.log('\n=== 场景 D：支付渠道抖一下，backlog 就再也追不回来（模拟，假设同前） ===');
{
  const daily = 1_000_000;              // 假设日增 100 万单
  const window = Math.round(daily / 288); // 每 5 分钟新增待支付单
  const perMs = 130;                    // 假设渠道抖动时单笔 130ms（Feign 变慢）
  const cap = Math.floor(300000 / perMs);
  let backlog = 0;
  console.log(`日增 ${daily} 单，每窗口到账 ${window} 单，单笔 ${perMs}ms ⇒ 每轮最多 ${cap} 单`);
  for (let round = 1; round <= 8; round++) {
    backlog += window;                  // 新到账
    const done = Math.min(cap, backlog); // 本轮能处理的上限（串行 + 300s 预算）
    backlog -= done;
    const need = (backlog * perMs / 1000).toFixed(0);
    console.log(`第 ${round} 轮: 到账 +${window}，处理 ${done} ⇒ 积压 ${backlog} 单，清空需 ${need}s（预算 300s）${backlog > window * 2 ? '  ← 已经追不回来了' : ''}`);
  }
  console.log(`结论：单笔耗时涨到 ${perMs}ms 时，处理能力 ${cap} 单 < 到账 ${window} 单，积压每轮净增 ${window - cap} 单，fixedDelay 不会跳过也不会报警。`);
}
