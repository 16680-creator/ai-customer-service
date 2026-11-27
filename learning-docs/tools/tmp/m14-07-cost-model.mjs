#!/usr/bin/env node
/*
 * m14-07-cost-model.mjs —— LLM 用量计量与成本分析的算术验证脚本（14/07 篇 C 级证据）
 *
 * 干三件事：
 *   1. 用「假设的日流量分布」+ 仓库真实单价表，算两种归集（按模型 / 按场景）下的成本表；
 *   2. 模拟当前记账缺陷（流式 usage 恒 null、单价未配走 0 兜底、judge 完全不上报），
 *      算出「记账成本」与「真实成本」的差额与低估比例；
 *   3. 演示字符数估算 token 的两套口径在中文上的算术差。
 *
 * 单价来源：tools/nacos-config/ai-cs-chat.yml L118-L128（元/百万 token）
 * 注意：流量分布是假设值（本机无流量、不许启动服务），但算术全过程真实可复现。
 */
import { fileURLToPath } from 'node:url';

// ---------- 1. 真实单价表（tools/nacos-config/ai-cs-chat.yml L118-L128） ----------
const PRICING = {
  'deepseek-chat': { in: 2.0, out: 8.0 },
  'siliconflow-qwen3-32b': { in: 1.0, out: 2.0 },
  'siliconflow-qwen3-8b': { in: 0.2, out: 0.6 },
};
// ModelUsageProperties.defaultPricing 的初值是 0/0（ModelUsageProperties.java L46-L52）
const DEFAULT_PRICING = { in: 0, out: 0 };

const PER_MILLION = 1000000;

/** 复刻 ModelUsageRecorder.estimateCost（L127-L137）：先算再四舍五入到 6 位小数 */
function estimateCost(pricingKey, inTok, outTok) {
  const p = PRICING[pricingKey] ?? DEFAULT_PRICING;
  const inCost = (inTok / PER_MILLION) * p.in;
  const outCost = (outTok / PER_MILLION) * p.out;
  return Math.round((inCost + outCost) * 1e6) / 1e6; // DECIMAL(12,6) 精度
}

// ---------- 2. 假设的日流量分布 ----------
// mode: stream = 走 ResilientAiService.invokeStream（usage 恒 null）
//       nonstream = 走 invokeNonStream / VisionModelClient（有 usage）
// reported: true=会写 model_usage 行；false=完全不上报（judge 直连 chatClient.call()）
// priceHit: false 表示 pricingKey 查不到单价（视觉模型用展示名当 key）
const traffic = [
  { scenario: 'chat',    model: 'deepseek-chat',              calls: 8000, inTok: 1800, outTok: 260, mode: 'stream',    reported: true,  priceHit: true  },
  { scenario: 'rag',     model: 'deepseek-chat',              calls: 3000, inTok: 4200, outTok: 320, mode: 'stream',    reported: true,  priceHit: true  },
  { scenario: 'intent',  model: 'siliconflow-qwen3-8b',       calls: 8000, inTok: 420,  outTok: 24,  mode: 'nonstream', reported: true,  priceHit: true  },
  { scenario: 'rewrite', model: 'siliconflow-qwen3-8b',       calls: 2500, inTok: 700,  outTok: 90,  mode: 'nonstream', reported: true,  priceHit: true  },
  { scenario: 'summary', model: 'siliconflow-qwen3-8b',       calls: 900,  inTok: 2600, outTok: 180, mode: 'nonstream', reported: true,  priceHit: true  },
  { scenario: 'judge',   model: 'deepseek-chat',              calls: 300,  inTok: 1200, outTok: 60,  mode: 'nonstream', reported: false, priceHit: true  },
  { scenario: 'vision',  model: 'Qwen/Qwen3-VL-32B-Instruct', calls: 200, inTok: 1500, outTok: 120, mode: 'nonstream', reported: true,  priceHit: false },
];

// ---------- 3. 逐行算「真实成本」与「记账成本」 ----------
const rows = traffic.map((t) => {
  const truePerCall = estimateCost(t.model, t.inTok, t.outTok);
  const trueCost = Math.round(truePerCall * t.calls * 1e6) / 1e6;

  // 记账侧：流式 usage=null → Recorder 把 null 折成 0（L91-L95），费用随之为 0
  // 视觉：单价查不到 → defaultPricing 0/0 → 费用 0（但 token 数记对了）
  // judge：直连 chatClient.call()，根本不经过 Recorder → 连行都没有
  let recInTok = t.inTok;
  let recOutTok = t.outTok;
  let recPerCall = truePerCall;
  if (t.mode === 'stream') {
    recInTok = 0;
    recOutTok = 0;
    recPerCall = estimateCost(t.model, 0, 0);
  } else if (!t.priceHit) {
    recPerCall = estimateCost(t.model, t.inTok, t.outTok); // 落到 defaultPricing
  }
  if (!t.reported) {
    recInTok = 0;
    recOutTok = 0;
    recPerCall = 0;
  }
  const recCost = Math.round(recPerCall * t.calls * 1e6) / 1e6;
  return {
    ...t,
    truePerCall,
    trueCost,
    recInTok,
    recOutTok,
    recPerCall,
    recCost,
    estimatedFlag: t.mode === 'stream' ? 1 : 0,
  };
});

const sum = (arr, f) => arr.reduce((a, x) => a + f(x), 0);
const fmt = (n) => n.toFixed(6);
const pct = (n) => (n * 100).toFixed(2) + '%';

const totalTrue = sum(rows, (r) => r.trueCost);
const totalRec = sum(rows, (r) => r.recCost);
const totalCalls = sum(rows, (r) => r.calls);

console.log('=== A. 逐场景明细（真实 vs 记账） ===');
console.log(
  ['scenario', 'model', 'calls', 'true/call', 'trueCost', 'recCost', 'est']
    .map((s) => String(s).padEnd(16)).join(' | ')
);
for (const r of rows) {
  console.log(
    [
      r.scenario,
      r.model,
      String(r.calls),
      fmt(r.truePerCall),
      fmt(r.trueCost),
      fmt(r.recCost),
      String(r.estimatedFlag),
    ]
      .map((s) => String(s).padEnd(16)).join(' | ')
  );
}
console.log(`合计: calls=${totalCalls} trueCost=${fmt(totalTrue)} recCost=${fmt(totalRec)}`);

console.log('\n=== B. 按「模型」归集（记账数据的口径） ===');
const byModel = new Map();
for (const r of rows) {
  const k = r.model;
  const cur = byModel.get(k) ?? { calls: 0, trueCost: 0, recCost: 0, inTok: 0, outTok: 0 };
  cur.calls += r.calls;
  cur.trueCost += r.trueCost;
  cur.recCost += r.recCost;
  cur.inTok += r.recInTok * r.calls;
  cur.outTok += r.recOutTok * r.calls;
  byModel.set(k, cur);
}
for (const [k, v] of [...byModel.entries()].sort((a, b) => b[1].trueCost - a[1].trueCost)) {
  const share = totalRec > 0 ? v.recCost / totalRec : 0;
  console.log(
    `${k.padEnd(30)} calls=${String(v.calls).padEnd(6)} recCost=${fmt(v.recCost).padEnd(12)} ` +
      `recShare=${pct(share).padEnd(8)} trueCost=${fmt(v.trueCost)}`
  );
}

console.log('\n=== C. 按「场景」归集（记账数据的口径） ===');
for (const v of [...byModelScenario(rows)].sort((a, b) => b.recCost - a.recCost)) {
  console.log(
    `${v.scenario.padEnd(10)} calls=${String(v.calls).padEnd(6)} recCost=${fmt(v.recCost).padEnd(12)} trueCost=${fmt(v.trueCost)}`
  );
}

console.log('\n=== D. 低估了多少 ===');
const gap = totalTrue - totalRec;
console.log(`真实成本合计 = ${fmt(totalTrue)} 元/日`);
console.log(`记账成本合计 = ${fmt(totalRec)} 元/日`);
console.log(`漏记金额     = ${fmt(gap)} 元/日`);
console.log(`低估比例     = ${pct(gap / totalTrue)}（记账只有真实的 ${pct(totalRec / totalTrue)}）`);

const streamCalls = sum(rows.filter((r) => r.mode === 'stream'), (r) => r.calls);
const streamTrue = sum(rows.filter((r) => r.mode === 'stream'), (r) => r.trueCost);
console.log(
  `流式调用占比 = ${pct(streamCalls / totalCalls)}（${streamCalls}/${totalCalls}），` +
    `但占真实成本 ${pct(streamTrue / totalTrue)}（${fmt(streamTrue)}/${fmt(totalTrue)}）`
);
const unmeteredTrue = sum(rows.filter((r) => !r.reported), (r) => r.trueCost);
const visionTrue = sum(rows.filter((r) => !r.priceHit), (r) => r.trueCost);
console.log(`judge（无行）  真实成本 = ${fmt(unmeteredTrue)}（${pct(unmeteredTrue / totalTrue)}）`);
console.log(`vision（0 单价）真实成本 = ${fmt(visionTrue)}（${pct(visionTrue / totalTrue)}）`);

const visionRow = rows.find((r) => !r.priceHit);
console.log(
  `vision（单价键 miss）token 记对了但金额恒 0：input=${visionRow.recInTok * visionRow.calls} ` +
    `output=${visionRow.recOutTok * visionRow.calls}，estimated_cost=0；价格表无此键 → 仓库内无法计价`
);

console.log('\n=== E. 月化与外推（只做乘法，不外推真实性） ===');
console.log(`按月 30 天：真实 ≈ ${fmt(totalTrue * 30)} 元，账面 ≈ ${fmt(totalRec * 30)} 元，漏 ≈ ${fmt(gap * 30)} 元`);

console.log('\n=== F. 敏感性：流式成本占比 s 与低估比例 ===');
for (const s of [0.3, 0.5, 0.7, 0.9]) {
  console.log(`s=${pct(s).padEnd(8)} → 账面 = 真实 × ${(1 - s).toFixed(2)} = ${pct(1 - s)}，低估 ${pct(s)}`);
}

console.log('\n=== G. 按「用户」归集（假设三个用户的场景配比不同） ===');
const byScenario = new Map(rows.map((r) => [r.scenario, r]));
const users = [
  { user: 'u-1001 重度对话', mix: { chat: 6000, rag: 2400, intent: 2000 } },
  { user: 'u-1002 一般使用', mix: { chat: 1500, rag: 400, intent: 4000, rewrite: 1200 } },
  { user: 'u-1003 内部评测', mix: { summary: 900, judge: 300, vision: 200 } },
];
for (const u of users) {
  let t = 0;
  let r = 0;
  for (const [sc, n] of Object.entries(u.mix)) {
    const row = byScenario.get(sc);
    t += n * row.truePerCall;
    r += n * row.recPerCall;
  }
  console.log(
    `${u.user.padEnd(20)} trueCost=${fmt(t).padEnd(12)} recCost=${fmt(r).padEnd(12)} ` +
      `可见率=${pct(t > 0 ? r / t : 0)}`
  );
}

console.log('\n=== H. 字符数估 token 的两套口径（中文） ===');
const sample = 'AI 助手暂时繁忙，请稍后重试。'; // ai-cs-chat/.../ResilientAiService.java L455 的兜底文案
const chars = [...sample].length;
console.log(`样本="${sample}" 字符数=${chars}`);
console.log(`口径① 字符数/4   → ${(chars / 4).toFixed(2)} token（OpenAI 英文经验的粗糙移植）`);
console.log(`口径② 字符数/1.5 → ${(chars / 1.5).toFixed(2)} token（按中文约 1.5 字/token 反推）`);
console.log(`两口径差 ${((chars / 1.5) / (chars / 4)).toFixed(2)} 倍；样本固定，差额只来自除数`);

function byModelScenario(list) {
  const m = new Map();
  for (const r of list) {
    m.set(r.scenario, { scenario: r.scenario, calls: r.calls, recCost: r.recCost, trueCost: r.trueCost });
  }
  return m.values();
}

export {};
void fileURLToPath;
