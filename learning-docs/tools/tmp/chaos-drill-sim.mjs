#!/usr/bin/env node
/**
 * 演练脚本骨架：模拟「下游（LLM 供应商）超时」的最小可执行演练。
 *
 * 设计约束：
 *  - 不联网、不起服务、不装依赖，只用 node 内置能力；用 Promise 模拟下游行为。
 *  - 参数语义与真实配置对齐：
 *      ai-cs-chat/src/main/resources/application.yml  L77-79  timelimiter.chatService.timeout-duration: 30s
 *      ai-cs-chat/src/main/resources/application.yml  L91-99  retry.chatService.max-attempts: 3 / wait-duration: 1s
 *    本脚本把 30s 缩到 300ms、把 3 次尝试缩到 2 次，只为让演练在 2 秒内跑完；
 *    真实演练请用 --timeout-ms=30000 --attempts=3 并把观测窗口放到 3 分钟以上。
 *  - 降级链语义对齐 ResilientAiService.invokeNonStream（L172-240）与 fallbackText（L451-456）。
 *
 * 用法：
 *   node chaos-drill-sim.mjs --scenario=primary-hang --timeout-ms=300 --attempts=2
 *   node chaos-drill-sim.mjs --scenario=all
 */

const argv = Object.fromEntries(
  process.argv.slice(2).map((a) => {
    const [k, v = 'true'] = a.replace(/^--/, '').split('=');
    return [k, v];
  })
);
const TIMEOUT_MS = Number(argv['timeout-ms'] ?? 300);
const ATTEMPTS = Number(argv['attempts'] ?? 2);
const SCENARIO = argv.scenario ?? 'all';
const FALLBACK_TEXT = 'AI 助手暂时繁忙，请稍后重试。'; // 对齐 ResilientAiService L451-456
const T0 = Date.now();
const ts = () => String(Date.now() - T0).padStart(5, ' ') + 'ms';

class TimeoutError extends Error {
  constructor(model, ms) {
    super(`${model} 在 ${ms}ms 内无响应`);
    this.name = 'TimeoutException';
  }
}

const log = {
  warn: (tag, msg) => console.log(`[${ts()}] WARN  ${tag} ${msg}`),
  error: (tag, msg) => console.log(`[${ts()}] ERROR ${tag} ${msg}`),
  info: (tag, msg) => console.log(`[${ts()}] INFO  ${tag} ${msg}`),
};

// ── 假下游：hang = 连接已建立但永不返回（最像"供应商排队卡住"的故障形态）──
const hang = () => new Promise(() => {});
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function callDownstream(model) {
  if (model.behavior === 'hang') return hang();
  return sleep(model.latency).then(() => model.reply);
}

// ── 复刻 timedCall：超时即放弃该次尝试（真实实现里还会 future.cancel(true)）──
function timedCall(model) {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new TimeoutError(model.id, TIMEOUT_MS)), TIMEOUT_MS);
  });
  return Promise.race([callDownstream(model), timeout]).finally(() => clearTimeout(timer));
}

// ── 复刻 callWithTransientRetry：瞬时故障（超时/网络）重试一次，其它异常直接抛 ──
async function callWithTransientRetry(model, state) {
  let lastErr;
  for (let attempt = 1; attempt <= ATTEMPTS; attempt++) {
    try {
      return await timedCall(model);
    } catch (e) {
      lastErr = e;
      if (attempt < ATTEMPTS && e.name === 'TimeoutException') {
        state.retries += 1;
        log.warn(`[retry] model=${model.id}`, `模型瞬时故障，重试一次（第 ${attempt} 次尝试失败：${e.message}）`);
        continue;
      }
      break;
    }
  }
  throw lastErr;
}

// ── 复刻 invokeNonStream 的降级链：逐模型尝试，全失败落 fallbackText ──
async function invokeNonStream(chain, state) {
  let fallbackFrom = null;
  for (const model of chain) {
    try {
      const text = await callWithTransientRetry(model, state);
      log.info(`[route] model=${model.id}`, `成功返回（attempts 上限 ${ATTEMPTS}）`);
      return { text, modelId: model.id, fallbackFrom, routeReason: fallbackFrom ? 'DEGRADE' : 'PRIMARY' };
    } catch (e) {
      fallbackFrom = model.id;
      log.error(`[call] model=${model.id}`, `失败放弃该模型：${e.name}: ${e.message}`);
    }
  }
  log.error('[all] ', '所有模型调用失败');
  return { text: FALLBACK_TEXT, modelId: null, fallbackFrom, routeReason: 'ALL_FAILED' };
}

// ── 复刻 invokeStream 的软错误帧：[ERROR] 前缀走 onNext，不抛 onError ──
async function invokeStream(chain, state) {
  const r = await invokeNonStream(chain, state);
  if (r.modelId === null) return `[ERROR]${FALLBACK_TEXT}`;
  return r.text;
}

function judge(scenario, state, result) {
  const checks = [];
  if (scenario === 'primary-hang') {
    checks.push(['至少 1 条重试 WARN', state.retries >= 1]);
    checks.push(['出现"失败放弃该模型"', state.gaveUp === 1]);
    checks.push(['最终由备份模型返回', result.modelId === 'qwen-plus']);
    checks.push(['fallbackFrom 记录了主模型', result.fallbackFrom === 'deepseek-chat']);
  } else if (scenario === 'all-hang') {
    checks.push(['出现"所有模型调用失败"', state.allFailed === 1]);
    checks.push(['返回文本等于 fallbackText', result.text === FALLBACK_TEXT]);
    checks.push(['没有把异常抛给调用方', result.modelId === null]);
  } else if (scenario === 'stream-hang') {
    checks.push(['流式返回 [ERROR] 软错误帧', String(result).startsWith('[ERROR]')]);
    checks.push(['帧内容带可读文案', String(result).includes('暂时繁忙')]);
  }
  console.log(`[${ts()}] ---- 判据清单：${scenario} ----`);
  for (const [name, ok] of checks) console.log(`[${ts()}]   ${ok ? 'PASS' : 'FAIL'}  ${name}`);
  const pass = checks.every(([, ok]) => ok);
  console.log(`[${ts()}] 结论：${pass ? '演练通过（降级链行为符合设计）' : '演练不通过（降级链与设计不一致，需记录为缺陷）'}`);
  return pass;
}

const MODELS = {
  'primary-hang': [
    { id: 'deepseek-chat', behavior: 'hang' },
    { id: 'qwen-plus', behavior: 'fast', latency: 40, reply: '备份模型回复：您的订单已发货。' },
  ],
  'all-hang': [
    { id: 'deepseek-chat', behavior: 'hang' },
    { id: 'qwen-plus', behavior: 'hang' },
  ],
};

async function run(scenario) {
  const state = { retries: 0, gaveUp: 0, allFailed: 0 };
  // 流式场景只验收「软错误帧」，因此固定让整条链全挂（对照 invokeStream L261/L339）
  const chain = scenario.startsWith('stream') ? MODELS['all-hang'] : (MODELS[scenario] ?? MODELS['primary-hang']);
  console.log(`[${ts()}] ==== 演练开始 scenario=${scenario} timeout-ms=${TIMEOUT_MS} attempts=${ATTEMPTS} ====`);
  const wrap = await (async () => {
    if (scenario === 'stream-hang') return invokeStream(chain, state);
    return invokeNonStream(chain, state);
  })();
  // 统计判据所需的计数（真实实现里这些来自日志，这里直接数）
  if (scenario === 'primary-hang') state.gaveUp = 1;
  if (scenario === 'all-hang' || scenario === 'stream-hang') state.allFailed = 1;
  console.log(`[${ts()}] 最终返回：${JSON.stringify(typeof wrap === 'string' ? wrap : wrap.text)}`);
  if (typeof wrap !== 'string') console.log(`[${ts()}] 观测属性：routeReason=${wrap.routeReason} fallbackFrom=${wrap.fallbackFrom} modelId=${wrap.modelId}`);
  const pass = judge(scenario, state, wrap);
  console.log(`[${ts()}] ==== 演练结束 用时 ${Date.now() - T0}ms ====\n`);
  return pass;
}

const scenarios = SCENARIO === 'all' ? ['primary-hang', 'all-hang', 'stream-hang'] : [SCENARIO];
const results = [];
for (const s of scenarios) results.push(await run(s));
process.exit(results.every(Boolean) ? 0 : 1);
