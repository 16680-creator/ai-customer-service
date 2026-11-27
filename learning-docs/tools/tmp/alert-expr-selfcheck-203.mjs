// 告警规则 expr 文本自检（本机无 promtool，只做静态文本层校验 + 阈值算术）
// 用法: node tools/tmp/alert-expr-selfcheck-203.mjs
// 局限: 不做 PromQL 语义求值、不连 Prometheus、不替代 promtool check rules。

const rules = [
  {
    name: 'AiCsServiceDown',
    kind: '症状 P0',
    expr: 'up{job="ai-cs-services"} == 0',
    forDur: '2m',
  },
  {
    name: 'AiCsHttp5xxBudgetBurn1h',
    kind: '预算 P1',
    // 与文档 §3.5 的 promql 块逐字一致（含 # 注释；校验前先剥离注释）
    expr: `
# AiCsHttp5xxBudgetBurn1h（P1）: 30 天 99% 预算下, 1 小时烧掉 2% 预算就通知
(
  sum(rate(http_server_requests_seconds_count{job="ai-cs-services",status=~"5.."}[5m]))   # 分子: 5xx 请求数
  / sum(rate(http_server_requests_seconds_count{job="ai-cs-services"}[5m]))               # 分母: 全部请求数
  > 14.4 * 0.01                                                                           # 14.4 倍速 x 1% 预算 = 错误率 14.4%
)
and                                                                                       # and: 短窗与长窗必须同时成立
(
  sum(rate(http_server_requests_seconds_count{job="ai-cs-services",status=~"5.."}[1h]))   # 长窗: 防尖峰骗过短窗
  / sum(rate(http_server_requests_seconds_count{job="ai-cs-services"}[1h]))
  > 14.4 * 0.01
)`,
    forDur: '5m',
  },
  {
    name: 'AiCsJvmHeapWillExhaust',
    kind: '趋势 P2',
    expr:
      'predict_linear(jvm_memory_used_bytes{job="ai-cs-services",area="heap"}[30m], 3600) ' +
      '> jvm_memory_max_bytes{job="ai-cs-services",area="heap"} * 0.95',
    forDur: '10m',
  },
  {
    name: 'AiCsChatCircuitBreakerOpen',
    kind: '原因(扩展)',
    expr: 'max by (instance) (resilience4j_circuitbreaker_state{job="ai-cs-services",state="open"}) == 1',
    forDur: '1m',
  },
];

const DUR_RE = /^\[(\d+)(ms|s|m|h|d|w|y)\]$/;
const unitSec = { ms: 0.001, s: 1, m: 60, h: 3600, d: 86400, w: 604800, y: 31536000 };

function balance(s, open, close) {
  let depth = 0;
  let min = 0;
  for (const ch of s) {
    if (ch === open) depth++;
    else if (ch === close) { depth--; min = Math.min(min, depth); }
  }
  return { depth, min };
}

function windows(s) {
  return [...s.matchAll(/\[[^\]]*\]/g)].map((m) => m[0]);
}

let bad = 0;
console.log('=== 1) expr 文本层自检 ===');
console.log('rule'.padEnd(28), 'parens'.padEnd(8), 'braces'.padEnd(8), 'windows'.padEnd(24), 'verdict');
const strip = (s) => s.replace(/#[^\n]*/g, '');

for (const r of rules) {
  const e = strip(r.expr);
  const p = balance(e, '(', ')');
  const b = balance(e, '{', '}');
  const ws = windows(e);
  // 只有含 rate()/increase()/预测函数的表达式才必须有 range 窗口
  const needsWin = /(rate|increase|avg_over_time|max_over_time|predict_linear)\(/.test(e);
  const wsOk = (needsWin ? ws.length > 0 : true) && ws.every((w) => DUR_RE.test(w));
  const ok = p.depth === 0 && p.min === 0 && b.depth === 0 && b.min === 0 && wsOk && !/\t/.test(r.expr);
  if (!ok) bad++;
  console.log(
    r.name.padEnd(28),
    String(p.depth).padEnd(8),
    String(b.depth).padEnd(8),
    (ws.length ? ws.join(',') : '-').padEnd(24),
    ok ? 'PASS' : 'FAIL'
  );
}

console.log('\n=== 2) 窗口换算（rate() 至少需要 2 个样本；scrape_interval=15s）===');
const scrape = 15;
for (const r of rules) {
  for (const w of windows(r.expr)) {
    const m = w.match(DUR_RE);
    const sec = Number(m[1]) * unitSec[m[2]];
    console.log(
      `${r.name} 窗口 ${w} = ${sec}s -> 预计样本数 ${Math.floor(sec / scrape)} 个` +
        (sec / scrape >= 2 ? '  OK(>=2)' : '  风险(<2，rate 出不了值)')
    );
  }
}

console.log('\n=== 3) 阈值算术（错误预算 burn rate，示例预算 1%）===');
const budget = 0.01;
const rows = [
  ['5m + 1h 双窗口(长窗 1h)', 14.4, 3600],
  ['30m + 6h 双窗口(长窗 6h)', 6, 21600],
  ['2h + 1d 双窗口(长窗 1d)', 3, 86400],
];
for (const [label, factor, longWinSec] of rows) {
  const errPct = (budget * factor * 100).toFixed(1);
  // 预算消耗 = 燃烧倍率 x (窗口时长 / 30天)，单位是总预算的百分比
  const consume = (factor * (longWinSec / (30 * 86400)) * 100).toFixed(2);
  console.log(
    `${label}: 阈值 ${factor} x ${budget} = ${(factor * budget).toFixed(4)} (即错误率 ${errPct}%)` +
      ` | 以该速率持续 ${longWinSec / 3600}h 消耗总预算 ${consume}%`
  );
}

console.log('\n=== 4) 通知放大算术（为何必须 grouping）===');
const targets = 11;
console.log(`prometheus.yml 里 static target = ${targets} 个；逐个发通知 = ${targets} 条/轮`);
console.log(`加 group_by: [alertname, severity] 后同一轮 = 1 条（压缩比 ${targets}:1）`);
const evalPerDay = 86400 / scrape;
console.log(`evaluation_interval=15s -> 每条规则每天评估 ${evalPerDay} 次`);
console.log(`若 repeat_interval=4h，则一条持续告警每天最多提醒 ${Math.ceil(24 / 4)} 次`);
console.log('准确率 = 真实故障触发数 / 总触发数（未接生产告警，只给公式不给数字）');

console.log('\n=== 5) 结果 ===');
console.log(bad === 0 ? `ALL PASS：${rules.length} 条规则文本层自检通过（语法仍需 promtool 校验）` : `${bad} 条 FAIL`);
process.exit(bad === 0 ? 0 : 1);
