#!/usr/bin/env node
/*
 * m16-02-k8s-res-audit.mjs —— K8s 资源治理盘点（只读 + 纯算术）
 *
 * 用途：把 deploy/k8s 下 20 个工作负载的 replicas / requests / limits / 探针
 *      逐行解析出来，算清三件事：
 *        1. 每个 Pod 的 requests 与 limits 分别是多少（调度账 / 生死账）
 *        2. 全量清单的 requests/limits 合计与超卖倍数
 *        3. 这些 Pod 全部同时调度需要多少 CPU/内存，几种典型节点各需几台
 *
 * 约束：不启动任何服务、不连集群、不写仓库文件。行号一律用 UTF-8 正确编码计数
 *      （等价于 [System.IO.File]::ReadAllLines），Windows 默认 GBK 读中文注释会少算行。
 *
 * 运行：
 *   node learning-docs/tools/tmp/m16-02-k8s-res-audit.mjs
 */
import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';

const ROOT = 'D:/Projects/Persion/ai-customer-service';
const K8S = join(ROOT, 'deploy', 'k8s');

async function walk(dir, out = []) {
  for (const e of await readdir(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) await walk(p, out);
    else if (e.name.toLowerCase().endsWith('.yaml')) out.push(p);
  }
  return out;
}

const indentOf = (s) => s.length - s.trimStart().length;
const stripComment = (s) => s.replace(/\s+#.*$/, '').trim();
const unquote = (s) => s.replace(/^["']|["']$/g, '').trim();

function splitDocs(text) {
  const lines = text.split(/\r?\n/);
  const docs = [];
  let cur = { start: 0, lines: [] };
  for (let i = 0; i < lines.length; i++) {
    if (/^---\s*$/.test(lines[i])) { docs.push(cur); cur = { start: i + 1, lines: [] }; }
    else cur.lines.push(lines[i]);
  }
  docs.push(cur);
  return docs.filter((d) => d.lines.some((l) => l.trim() && !l.trim().startsWith('#')));
}

const abs = (doc, idx) => doc.start + idx + 1; // 1-based 绝对行号

function topLevel(doc, key) {
  for (let i = 0; i < doc.lines.length; i++) {
    const m = doc.lines[i].match(new RegExp('^' + key + ':\\s*(\\S+)'));
    if (m) return { i, value: unquote(stripComment(m[1])) };
  }
  return null;
}

function metadataName(doc) {
  const mi = doc.lines.findIndex((l) => /^metadata:\s*$/.test(l));
  if (mi < 0) return null;
  for (let i = mi + 1; i < doc.lines.length; i++) {
    if (/^\S/.test(doc.lines[i]) && doc.lines[i].trim()) break;
    const m = doc.lines[i].match(/^\s{2}name:\s*(\S+)/);
    if (m) return { i, value: unquote(stripComment(m[1])) };
  }
  return null;
}

// ---- 解析所有 resources 块，区分「容器资源」与「PVC 存储申请」 ----
function parseResources(doc) {
  const blocks = [];
  for (let i = 0; i < doc.lines.length; i++) {
    const m = doc.lines[i].match(/^(\s*)resources:\s*(#.*)?$/);
    if (!m) continue;
    const base = m[1].length;
    const blk = { at: i, base, requests: {}, limits: {}, reqLines: {}, limLines: {} };
    let sect = null;
    for (let j = i + 1; j < doc.lines.length; j++) {
      const raw = doc.lines[j];
      if (!raw.trim() || raw.trim().startsWith('#')) continue;
      if (indentOf(raw) <= base) break;
      const t = stripComment(raw);
      const kv = t.match(/^([A-Za-z]+):\s*(.*)$/);
      if (!kv) continue;
      const key = kv[1], val = unquote(kv[2]);
      if (key === 'requests' || key === 'limits') { sect = key; continue; }
      if (!sect || !val) continue;
      blk[sect][key] = val;
      (sect === 'requests' ? blk.reqLines : blk.limLines)[key] = abs(doc, j);
    }
    blocks.push(blk);
  }
  return blocks;
}

function parseProbe(doc, name) {
  for (let i = 0; i < doc.lines.length; i++) {
    if (!new RegExp('^(\\s*)' + name + ':\\s*(#.*)?$').test(doc.lines[i])) continue;
    const base = indentOf(doc.lines[i]);
    const p = { at: i, kind: '?', target: '', initialDelaySeconds: null, periodSeconds: null, timeoutSeconds: null, failureThreshold: null };
    for (let j = i + 1; j < doc.lines.length; j++) {
      const raw = doc.lines[j];
      if (!raw.trim() || raw.trim().startsWith('#')) continue;
      if (indentOf(raw) <= base) break;
      const t = stripComment(raw);
      if (/^httpGet:/.test(t)) p.kind = 'httpGet';
      else if (/^tcpSocket:/.test(t)) p.kind = 'tcpSocket';
      else if (/^exec:/.test(t)) p.kind = 'exec';
      else if (/^path:/.test(t)) p.target += ' ' + t.slice(t.indexOf(':') + 1).trim();
      else if (/^port:/.test(t)) p.target += ':' + t.slice(t.indexOf(':') + 1).trim();
      else {
        const kv = t.match(/^([A-Za-z]+):\s*(\d+)/);
        if (kv && p[kv[1]] !== undefined) p[kv[1]] = Number(kv[2]);
      }
    }
    p.target = p.target.trim().replace(/^:/, '');
    return p;
  }
  return null;
}

// 找「决定堆大小」的那个环境变量：值里含 -Xmx（JAVA_OPTS / JAVA_OPT_EXT / ES_JAVA_OPTS）
// 或变量名本身就是 JVM_XMX（Nacos 官方镜像约定）。
function parseHeapEnv(doc) {
  for (let i = 0; i < doc.lines.length; i++) {
    const nm = doc.lines[i].match(/^\s*-\s*name:\s*([A-Za-z_][\w]*)/);
    if (!nm) continue;
    const envName = nm[1];
    // 只认「大写环境变量名」，否则会误匹配 ports 里的 - name: namesrv 之类
    if (!/^[A-Z][A-Z0-9_]*$/.test(envName)) continue;
    const base = indentOf(doc.lines[i]);
    for (let j = i + 1; j < doc.lines.length; j++) {
      const raw = doc.lines[j];
      if (!raw.trim() || raw.trim().startsWith('#')) continue;
      if (indentOf(raw) <= base + 2 && /^\s*-\s*name:/.test(raw)) break;
      const m = raw.match(/^\s*value:\s*(.+)$/);
      if (!m) continue;
      const val = unquote(stripComment(m[1]));
      if (/-Xmx/.test(val) || /^JVM_XMX$/.test(envName)) {
        return { at: abs(doc, j), env: envName, value: val };
      }
      break; // 该 env 的 value 已看完，不含堆设置
    }
  }
  return null;
}

// ---- 单位换算 ----
const cpuMilli = (v) => {
  if (v == null) return null;
  if (/^\d+m$/.test(v)) return Number(v.slice(0, -1));
  return Math.round(Number(v) * 1000);
};
const memMi = (v) => {
  if (v == null) return null;
  const m = v.match(/^([\d.]+)(Ki|Mi|Gi|Ti|k|M|G)?$/);
  if (!m) return NaN;
  const n = Number(m[1]);
  const unit = m[2] || '';
  const factor = { Ki: 1 / 1024, Mi: 1, Gi: 1024, Ti: 1024 * 1024, k: 1 / 1024, M: 1, G: 1024, '': 1 / (1024 * 1024) }[unit];
  return Math.round(n * factor);
};
const xmxMi = (val) => {
  if (!val) return null;
  const m = val.match(/-Xmx(\d+)([mMgG])/) || val.match(/^(\d+)([mMgG])$/);
  if (!m) return null;
  return m[2].toLowerCase() === 'm' ? Number(m[1]) : Number(m[1]) * 1024;
};

// ---- 主流程 ----
const files = (await walk(K8S)).sort();
const workloads = [];
const docKinds = {};

for (const f of files) {
  const text = await readFile(f, 'utf8');
  const docs = splitDocs(text);
  for (const doc of docs) {
    const kind = topLevel(doc, 'kind');
    if (!kind) continue;
    docKinds[kind.value] = (docKinds[kind.value] || 0) + 1;
    if (!/^(Deployment|StatefulSet|DaemonSet)$/.test(kind.value)) continue;

    const name = metadataName(doc);
    const ns = topLevel(doc, 'namespace');
    const reps = doc.lines.findIndex((l) => /^\s+replicas:\s*\d+/.test(l));
    const rblocks = parseResources(doc);
    const containerRes = rblocks.find((b) => !b.requests.storage && (b.requests.cpu || b.limits.cpu || b.requests.memory));
    const pvcRes = rblocks.filter((b) => b.requests.storage);
    const heapEnv = parseHeapEnv(doc);
    const imageLine = doc.lines.findIndex((l) => /^\s+image:\s*\S+/.test(l));

    // namespace 实际位置（可能写在 metadata 下任意缩进）
    let nsAt = null, nsVal = '(none)';
    for (let i = 0; i < doc.lines.length; i++) {
      const m = doc.lines[i].match(/^\s+namespace:\s*(\S+)/);
      if (m) { nsAt = abs(doc, i); nsVal = unquote(stripComment(m[1])); break; }
    }

    workloads.push({
      file: relative(ROOT, f).replace(/\\/g, '/'),
      kind: kind.value,
      name: name ? name.value : '(no name)',
      nameAt: name ? abs(doc, name.i) : null,
      ns: nsVal,
      nsAt,
      replicas: reps >= 0 ? Number(doc.lines[reps].match(/replicas:\s*(\d+)/)[1]) : null,
      replicasAt: reps >= 0 ? abs(doc, reps) : null,
      imageAt: imageLine >= 0 ? abs(doc, imageLine) : null,
      resAt: containerRes ? abs(doc, containerRes.at) : null,
      reqCpu: cpuMilli(containerRes?.requests.cpu),
      reqMem: memMi(containerRes?.requests.memory),
      limCpu: cpuMilli(containerRes?.limits.cpu),
      limMem: memMi(containerRes?.limits.memory),
      reqLines: containerRes ? containerRes.reqLines : {},
      limLines: containerRes ? containerRes.limLines : {},
      liveness: parseProbe(doc, 'livenessProbe'),
      readiness: parseProbe(doc, 'readinessProbe'),
      startup: parseProbe(doc, 'startupProbe'),
      heapEnv,
      pvc: pvcRes.map((b) => ({ storage: b.requests.storage, at: abs(doc, b.at) })),
    });
  }
}

const pad = (s, n) => String(s).padEnd(n);
const padL = (s, n) => String(s).padStart(n);

// ================= TABLE 1：逐工作负载 =================
console.log('## 表 1　20 个工作负载的 replicas / requests / limits / 探针（行号为 UTF-8 正确计数）\n');
console.log('| # | 工作负载 | kind | 文件:行(replicas) | 副本 | requests cpu/mem | limits cpu/mem | cpu 倍数 | mem 倍数 | liveness | readiness |');
console.log('|---|---------|------|------------------|-----|-----------------|---------------|---------|---------|----------|-----------|');
workloads.forEach((w, i) => {
  const cpuMul = w.reqCpu && w.limCpu ? (w.limCpu / w.reqCpu).toFixed(2) + '×' : '-';
  const memMul = w.reqMem && w.limMem ? (w.limMem / w.reqMem).toFixed(2) + '×' : '-';
  const lv = w.liveness ? `${w.liveness.kind}${w.liveness.target ? ' ' + w.liveness.target : ''} delay=${w.liveness.initialDelaySeconds}s` : '**无**';
  const rd = w.readiness ? `${w.readiness.kind}${w.readiness.target ? ' ' + w.readiness.target : ''} delay=${w.readiness.initialDelaySeconds}s` : '**无**';
  console.log(`| ${i + 1} | ${w.name} | ${w.kind} | ${w.file}:${w.replicasAt} | ${w.replicas} | ${w.reqCpu}m / ${w.reqMem}Mi | ${w.limCpu}m / ${w.limMem}Mi | ${cpuMul} | ${memMul} | ${lv} | ${rd} |`);
});

// ================= TABLE 2：字段出现次数 =================
const fieldCount = (re) => { let n = 0; for (const w of workloads) n += 0; return n; };
console.log('\n## 表 2　资源/弹性字段在 deploy/k8s 全部 20 个清单里的出现次数\n');
const rawAll = (await Promise.all(files.map((f) => readFile(f, 'utf8')))).join('\n');
const countRe = (re) => (rawAll.match(re) || []).length;
const rows = [
  ['kind: HorizontalPodAutoscaler', /kind:\s*HorizontalPodAutoscaler/g, 'HPA：水位自动扩缩'],
  ['kind: PodDisruptionBudget', /kind:\s*PodDisruptionBudget/g, 'PDB：自愿中断保护'],
  ['kind: ResourceQuota', /kind:\s*ResourceQuota/g, '命名空间总量配额'],
  ['kind: LimitRange', /kind:\s*LimitRange/g, '未写 resources 时的默认值注入'],
  ['kind: VerticalPodAutoscaler', /kind:\s*VerticalPodAutoscaler/g, 'VPA：按历史用量改 requests'],
  ['strategy:', /^\s*strategy:/gm, 'Deployment 更新策略（缺省=RollingUpdate）'],
  ['terminationGracePeriodSeconds', /terminationGracePeriodSeconds/g, '优雅停机窗口'],
  ['preStop', /preStop:/g, '摘流钩子'],
  ['startupProbe', /startupProbe:/g, '慢启动保护'],
  ['readinessProbe', /readinessProbe:/g, '就绪探测'],
  ['livenessProbe', /livenessProbe:/g, '存活探测'],
  ['resources:', /^\s*resources:/gm, '资源块（含 7 个 PVC 的 storage 申请）'],
  ['requests:', /^\s*requests:/gm, 'requests 子块'],
  ['limits:', /^\s*limits:/gm, 'limits 子块'],
  ['replicas:', /^\s*replicas:/gm, '副本数声明'],
  ['topologySpreadConstraints', /topologySpreadConstraints/g, '拓扑打散'],
  ['podAntiAffinity', /podAntiAffinity/g, '反亲和'],
  ['priorityClassName', /priorityClassName/g, '优先级抢占'],
];
console.log('| 字段 | 出现次数 | 含义 |');
console.log('|------|---------|------|');
for (const [k, re, desc] of rows) console.log(`| \`${k}\` | ${countRe(re)} | ${desc} |`);

const pvcCount = workloads.reduce((a, w) => a + w.pvc.length, 0);
console.log(`\n> resources: 出现 ${countRe(/^\s*resources:/gm)} 次 = 工作负载容器 ${workloads.length} 次 + volumeClaimTemplates 存储申请 ${pvcCount} 次；`);
console.log(`> 所以 requests: 也是 ${countRe(/^\s*requests:/gm)} 次，而 limits: 只有 ${countRe(/^\s*limits:/gm)} 次 —— 差出来的 ${countRe(/^\s*requests:/gm) - countRe(/^\s*limits:/gm)} 次正是 PVC 的 requests.storage（存储只有下限、没有上限）。`);

// ================= TABLE 3：合计与超卖 =================
const sum = (arr, k) => arr.reduce((a, w) => a + (w[k] || 0), 0);
const perPod = {
  reqCpu: sum(workloads, 'reqCpu'), reqMem: sum(workloads, 'reqMem'),
  limCpu: sum(workloads, 'limCpu'), limMem: sum(workloads, 'limMem'),
};
const weighted = {
  reqCpu: workloads.reduce((a, w) => a + w.reqCpu * w.replicas, 0),
  reqMem: workloads.reduce((a, w) => a + w.reqMem * w.replicas, 0),
  limCpu: workloads.reduce((a, w) => a + w.limCpu * w.replicas, 0),
  limMem: workloads.reduce((a, w) => a + w.limMem * w.replicas, 0),
  pods: workloads.reduce((a, w) => a + w.replicas, 0),
};
console.log('\n## 表 3　全量清单的资源合计（parse 自清单，非手算）\n');
console.log('| 口径 | 数量 | CPU | 内存 |');
console.log('|------|------|-----|------|');
console.log(`| 工作负载数 | ${workloads.length} | - | - |`);
console.log(`| Pod 数（replicas 求和） | ${weighted.pods} | - | - |`);
console.log(`| Σ requests（每工作负载算 1 份） | - | ${perPod.reqCpu}m | ${perPod.reqMem}Mi |`);
console.log(`| Σ limits（每工作负载算 1 份） | - | ${perPod.limCpu}m | ${perPod.limMem}Mi |`);
console.log(`| Σ requests × replicas | - | ${weighted.reqCpu}m | ${weighted.reqMem}Mi |`);
console.log(`| Σ limits × replicas | - | ${weighted.limCpu}m | ${weighted.limMem}Mi |`);
console.log(`| **超卖倍数 limits/requests** | - | **${(perPod.limCpu / perPod.reqCpu).toFixed(2)}×** | **${(perPod.limMem / perPod.reqMem).toFixed(2)}×** |`);

// ================= TABLE 4：QoS 分类 =================
console.log('\n## 表 4　每个工作负载的 QoS 档位（Guaranteed 需 requests==limits 且都写全）\n');
const qosOf = (w) => {
  if (w.reqCpu == null && w.reqMem == null && w.limCpu == null && w.limMem == null) return 'BestEffort';
  if (w.reqCpu != null && w.limCpu != null && w.reqMem != null && w.limMem != null && w.reqCpu === w.limCpu && w.reqMem === w.limMem) return 'Guaranteed';
  return 'Burstable';
};
const qosStat = { Guaranteed: 0, Burstable: 0, BestEffort: 0 };
workloads.forEach((w) => { w.qos = qosOf(w); qosStat[w.qos]++; });
console.log('| QoS | 工作负载数 | 含义 |');
console.log('|-----|-----------|------|');
console.log(`| Guaranteed | ${qosStat.Guaranteed} | requests==limits，最后被驱逐 |`);
console.log(`| Burstable | ${qosStat.Burstable} | 有 requests 但 requests<limits，中间层 |`);
console.log(`| BestEffort | ${qosStat.BestEffort} | 完全没写，最先被驱逐 |`);
console.log('\n明细：');
workloads.forEach((w) => console.log(`- ${w.name}: requests ${w.reqCpu}m/${w.reqMem}Mi vs limits ${w.limCpu}m/${w.limMem}Mi → **${w.qos}**`));

// ================= TABLE 5：堆与内存 limit 的关系 =================
const NON_JVM = new Set(['chroma', 'minio', 'mysql-master', 'mysql-slave', 'redis']);
console.log('\n## 表 5　JVM 堆（谁在决定堆大小）与容器内存 limit 的关系\n');
console.log('| 工作负载 | 堆设置（env 名 = 值） | 声明位置 | 内存 limit | 堆 / limit |');
console.log('|---------|---------------------|---------|-----------|-----------|');
for (const w of workloads) {
  const heapMi = xmxMi(w.heapEnv?.value);
  let heapCell;
  if (heapMi) heapCell = `${heapMi}Mi（${w.heapEnv.env}）`;
  else if (NON_JVM.has(w.name)) heapCell = '非 JVM（不适用）';
  else heapCell = '**未设 -Xmx**（JDK 默认 MaxRAMPercentage=25% 生效）';
  const ratio = heapMi ? ((heapMi / w.limMem) * 100).toFixed(0) + '%' : (NON_JVM.has(w.name) ? '-' : '约 25%（按 limit 折算）');
  const where = heapMi ? `${w.file}:${w.heapEnv.at}` : '-';
  console.log(`| ${w.name} | ${heapCell} | ${where} | ${w.limMem}Mi | ${ratio} |`);
}
console.log('\n> 注意 env 名不统一：业务服务用 `JAVA_OPTS`，RocketMQ 用 `JAVA_OPT_EXT`，Elasticsearch 用 `ES_JAVA_OPTS`，');
console.log('> Nacos 官方镜像用 `JVM_XMS` / `JVM_XMX`，xxl-job 走 Secret 里的 `PARAMS`，product-service 一个都没设。');

// ================= TABLE 8：探针细节 =================
const probeCell = (p) => {
  if (!p) return '**未配置**';
  const t = p.target ? ` ${p.target}` : '';
  return `${p.kind}${t}｜delay ${p.initialDelaySeconds}s／period ${p.periodSeconds}s／timeout ${p.timeoutSeconds ?? '默认1'}s／fail ${p.failureThreshold ?? '默认3'}`;
};
console.log('\n## 表 8　20 个工作负载的探针明细（timeoutSeconds / failureThreshold 没写就是默认值）\n');
console.log('| 工作负载 | livenessProbe | readinessProbe | startupProbe |');
console.log('|---------|---------------|----------------|--------------|');
for (const w of workloads) {
  console.log(`| ${w.name} | ${probeCell(w.liveness)} | ${probeCell(w.readiness)} | ${probeCell(w.startup)} |`);
}

// ================= TABLE 9：namespace 一致性 =================
console.log('\n## 表 9　工作负载声明在哪个 namespace（多 namespace 会让 ResourceQuota / PDB 分不到一起）\n');
const nsStat = {};
for (const w of workloads) nsStat[w.ns] = (nsStat[w.ns] || 0) + 1;
for (const [k, v] of Object.entries(nsStat)) console.log(`- \`namespace: ${k}\` —— ${v} 个工作负载`);
for (const w of workloads) if (w.ns !== 'ai-customer-service') console.log(`  - 例外：${w.name} → ${w.ns}（${w.file}:${w.nsAt}）`);

// ================= TABLE 6：节点容量情景算术 =================
console.log('\n## 表 6　「全部同时调度」需要多少核/内存 vs 几种典型节点能给多少\n');
console.log('假设：kubelet 预留后 CPU 可分配 = 标称值，内存可分配 = 标称值 × 0.9（典型预留），不跑其他负载。\n');
const profiles = [
  { label: '单节点 4C / 8Gi（常见个人学习机）', cpu: 4000, mem: 8192 },
  { label: '单节点 8C / 16Gi', cpu: 8000, mem: 16384 },
  { label: '3 节点 × 4C / 8Gi（每节点单独放一部分 Pod）', cpu: 12000, mem: 3 * 8192 },
  { label: '3 节点 × 8C / 32Gi', cpu: 24000, mem: 3 * 32768 },
];
console.log('| 集群形态 | 可分配 CPU | 可分配内存 | requests 占用 | limits 占用 | 装得下吗（按 requests） | CPU 超卖比 | 内存超卖比 |');
console.log('|---------|-----------|-----------|--------------|------------|----------------------|-----------|-----------|');
for (const p of profiles) {
  const allocMem = Math.floor(p.mem * 0.9);
  const ok = weighted.reqCpu <= p.cpu && weighted.reqMem <= allocMem;
  const cpuShort = weighted.reqCpu > p.cpu ? `CPU 少 ${weighted.reqCpu - p.cpu}m` : '';
  const memShort = weighted.reqMem > allocMem ? `内存少 ${weighted.reqMem - allocMem}Mi` : '';
  console.log(`| ${p.label} | ${p.cpu}m | ${allocMem}Mi | ${weighted.reqCpu}m / ${weighted.reqMem}Mi | ${weighted.limCpu}m / ${weighted.limMem}Mi | ${ok ? '能' : '**不能**（' + [cpuShort, memShort].filter(Boolean).join('，') + '）'} | ${(weighted.limCpu / p.cpu).toFixed(2)}× | ${(weighted.limMem / allocMem).toFixed(2)}× |`);
}

const memPerCore = (weighted.reqMem / 1024 / (weighted.reqCpu / 1000)).toFixed(2);
console.log(`\n关键算术：本次全量清单的内存取用 / 算力取用 = ${weighted.reqMem}Mi ÷ ${(weighted.reqCpu / 1000).toFixed(1)} 核 = **${memPerCore} GiB/核**。`);
console.log(`判读方法：把这一个数字和机型比。云端通用型机型给 2 GiB/核（如 4C/8Gi）时，本例需求 ${memPerCore} GiB/核 ≈ 机型比例，`);
console.log(`内存和 CPU 会同时被卡住（上表 4C/8Gi 就差 436Mi 内存，Pod 会 Pending）；给 4 GiB/核（如 2C/8Gi、8C/32Gi）时，先被卡住的变成 CPU。`);

console.log('\n## 表 7　若所有容器同时打到 limits（最坏情况）\n');
console.log(`| 项 | 数值 | 说明 |`);
console.log(`|----|------|------|`);
console.log(`| 峰值 CPU 需求 | ${weighted.limCpu}m = ${(weighted.limCpu / 1000).toFixed(1)} 核 | CPU 可压缩：超了只会被 CFS 限流（throttling），进程不死 |`);
console.log(`| 峰值内存需求 | ${weighted.limMem}Mi = ${(weighted.limMem / 1024).toFixed(1)} GiB | 内存不可压缩：节点内存真不够就是 OOMKill / 节点 NotReady |`);
console.log(`| 单 Pod 最容易踩内存上限的 | ai-chat-service（limit 1Gi，-Xmx512m）与 elasticsearch（limit 2Gi） | 堆只占一半左右，另外一半要留给元空间/线程栈/直接内存/页缓存 |`);

console.log('\n## 附：解析到的清单种类统计\n');
for (const [k, v] of Object.entries(docKinds).sort((a, b) => b[1] - a[1])) console.log(`- ${k}: ${v}`);
console.log(`\n共扫描 ${files.length} 个 yaml 文件、${Object.values(docKinds).reduce((a, b) => a + b, 0)} 个 YAML 文档、${workloads.length} 个工作负载。`);
console.log(`replicas=1 的工作负载 ${workloads.filter((w) => w.replicas === 1).length} 个，replicas=2 的 ${workloads.filter((w) => w.replicas === 2).length} 个。`);
