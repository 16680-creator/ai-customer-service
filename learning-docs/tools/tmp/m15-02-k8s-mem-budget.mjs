// learning-docs/tools/tmp/m15-02-k8s-mem-budget.mjs
// 目的：从 deploy/k8s 的真实清单里把每个容器的 requests/limits 抓出来，
// 汇总成「整套清单要多少内存/CPU」与「每个 JVM 服务的堆 : limit 余量」两张账。
// 只读仓库文件 + 纯算术，不联网、不写文件。
//
// 解析策略：逐行状态机而不是正则一把梭——因为本仓清单的 `resources:` / `requests:`
// 行尾常带中文注释（如 `requests:                     # 调度承诺值`），
// 以及 volumeClaimTemplates 里也有 `resources: -> requests: -> storage:`，
// 用单条正则同时匹配 cpu/memory 会漏文件（漏了 chroma / ai-chat / gateway / product / redis / neo4j 六个）。
import { readdirSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const K8S = join(HERE, '..', '..', '..', 'deploy', 'k8s');

const yamlFiles = [
  ...readdirSync(K8S).filter((f) => f.endsWith('.yaml')).map((f) => join(K8S, f)),
  ...readdirSync(join(K8S, 'services')).filter((f) => f.endsWith('.yaml')).map((f) => join(K8S, 'services', f)),
].sort();

const MEM_UNIT_MI = { Ki: 1 / 1024, Mi: 1, Gi: 1024, Ti: 1024 * 1024, K: 1 / 1024, M: 1, G: 1024, T: 1024 * 1024 };
const toMi = (raw) => {
  const m = /^"?([\d.]+)([KMGT]i?|)"?$/.exec(raw.trim());
  if (!m) return NaN;
  return Number(m[1]) * (MEM_UNIT_MI[m[2] || 'Mi'] ?? 1);
};
const toMilliCpu = (raw) => {
  const s = raw.replace(/"/g, '').trim();
  return s.endsWith('m') ? Number(s.slice(0, -1)) : Number(s) * 1000;
};
const xmxOf = (txt) => {
  const m = /-Xmx(\d+)([mMgG])/.exec(txt);
  if (!m) return null;
  return Number(m[1]) * (/[gG]/.test(m[2]) ? 1024 : 1);
};
const indentOf = (line) => line.length - line.trimStart().length;

function resourcesOf(file) {
  const lines = readFileSync(file, 'utf8').split('\n');
  const out = [];
  let resIndent = -1;
  let mode = null;
  let cur = null;
  for (const line of lines) {
    const bare = line.replace(/\s+#.*$/, '');
    const t = bare.trim();
    if (!t) continue;
    const ind = indentOf(bare);
    if (resIndent >= 0 && ind <= resIndent && !/^(requests|limits):/.test(t)) {
      if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
      cur = null;
      resIndent = -1;
      mode = null;
    }
    if (/^resources:\s*$/.test(t)) {
      if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
      cur = { file, reqCpuM: null, reqMemMi: null, limCpuM: null, limMemMi: null };
      resIndent = ind;
      mode = null;
      continue;
    }
    if (resIndent < 0) continue;
    if (/^(requests|limits):\s*$/.test(t)) {
      mode = t.startsWith('requests') ? 'req' : 'lim';
      continue;
    }
    const kv = /^(cpu|memory|storage):\s*(.+)$/.exec(t);
    if (!kv || !mode) continue;
    const [, key, val] = kv;
    if (key === 'cpu') cur[mode + 'CpuM'] = toMilliCpu(val);
    else if (key === 'memory') cur[mode + 'MemMi'] = toMi(val);
    // storage: 属于 volumeClaimTemplates，忽略
  }
  if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
  return out;
}

const rows = [];
for (const file of yamlFiles) {
  const rel = file.slice(K8S.length + 1).replaceAll('\\', '/');
  const src = readFileSync(file, 'utf8');
  const xmx = xmxOf(src);
  const groups = resourcesOf(file);
  groups.forEach((g, i) => rows.push({ ...g, file: rel, xmxMB: i === 0 ? xmx : null }));
}

const sum = (k) => rows.reduce((a, r) => a + (r[k] || 0), 0);
const pad = (s, n) => String(s).padEnd(n);

console.log('=== 每个容器组的 resources（真值来自 deploy/k8s/**.yaml）三列：req.mem / lim.mem / lim.cpu ===');
for (const r of rows) {
  console.log(
    `${pad(r.file, 32)} ${String(r.reqMemMi ?? '-').padStart(6)}Mi ${String(r.limMemMi ?? '-').padStart(7)}Mi ${String(r.limCpuM ?? '-').padStart(6)}m`
  );
}
console.log(
  `\n合计（${rows.length} 个容器组 / ${yamlFiles.length} 个清单文件）：` +
    `requests.memory=${sum('reqMemMi')}Mi  limits.memory=${sum('limMemMi')}Mi  ` +
    `requests.cpu=${sum('reqCpuM')}m  limits.cpu=${sum('limCpuM')}m`
);

console.log('\n=== 带 -Xmx 的工作负载：堆 vs limit 余量（余量 = limit - Xmx，要装 metaspace/线程栈/direct/code cache）===');
for (const r of rows) {
  if (r.xmxMB == null) continue;
  const margin = (r.limMemMi ?? 0) - r.xmxMB;
  console.log(
    `${pad(r.file, 32)} -Xmx=${String(r.xmxMB).padStart(4)}MB  limit=${String(r.limMemMi).padStart(5)}Mi  ` +
      `余量=${String(margin).padStart(5)}Mi${margin < 300 ? '   <-- 堆外只剩这么点' : ''}`
  );
}
const jvm = rows.filter((r) => r.xmxMB != null);
console.log(
  `\n结论算术：${jvm.length} 个 -Xmx 工作负载，堆合计 ${jvm.reduce((a, r) => a + r.xmxMB, 0)}MB，` +
    `它们 limit 合计 ${jvm.reduce((a, r) => a + r.limMemMi, 0)}Mi；` +
    `未写 -Xmx 的容器组 ${rows.length - jvm.length} 个。`
);
