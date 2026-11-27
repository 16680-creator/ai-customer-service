// 容量基线统计脚本（13-稳定性工程/02-容量规划与性能预算 的 C 级证据）
// 只读本仓文件，不做任何推测：统计 compose 与 k8s 的「容量账」
// 运行：cd D:\Projects\Persion\ai-customer-service ; node learning-docs\tools\tmp\cap-baseline.mjs
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const toMi = (v) => {
  const m = String(v).replace(/"/g, '').trim().match(/^(\d+(?:\.\d+)?)([GMK]i?)?$/i);
  if (!m) return 0;
  const n = Number(m[1]);
  const u = (m[2] || 'Mi').toLowerCase();
  if (u.startsWith('g')) return n * 1024;
  if (u.startsWith('k')) return n / 1024;
  return n;
};
const toMilliCpu = (v) => {
  const s = String(v).replace(/"/g, '').trim();
  return s.endsWith('m') ? Number(s.slice(0, -1)) : Number(s) * 1000;
};
const stripComment = (s) => s.replace(/\s+#.*$/, '').trim();

// ---------- 1. compose 账 ----------
console.log('== 1. docker compose 账 ==');
const composeFiles = readdirSync('deploy/docker-compose')
  .filter((f) => f.endsWith('.yml'))
  .sort()
  .map((f) => join('deploy/docker-compose', f));
if (existsSync('docker-compose.yml')) composeFiles.push('docker-compose.yml');

let cSvc = 0;
let cPorts = 0;
let cExposes = 0;
let cHeap = 0;
let heapCnt = 0;
const guardHits = { mem_limit: 0, cpus: 0 };

for (const file of composeFiles) {
  const text = readFileSync(file, 'utf8');
  const lines = text.split('\n');

  // 服务名：services: 块下两空格缩进的 key
  let inServices = false;
  const svc = [];
  for (const line of lines) {
    if (/^services:\s*$/.test(line)) { inServices = true; continue; }
    if (!inServices) continue;
    if (/^\S/.test(line)) break;
    const m = line.match(/^ {2}([A-Za-z0-9_.-]+):\s*$/);
    if (m) svc.push(m[1]);
  }

  // 端口：ports 块内逐条分类，host:container 是真映射，只有端口号的是 expose
  let ports = 0;
  let exposes = 0;
  let pi = -1;
  for (const raw of lines) {
    if (/^\s*ports:\s*$/.test(raw)) { pi = raw.length - raw.trimStart().length; continue; }
    if (pi < 0) continue;
    const ind = raw.length - raw.trimStart().length;
    const t = raw.trim();
    if (t === '' || t.startsWith('#')) continue;
    if (ind > pi && t.startsWith('-')) {
      const v = t.slice(1).replace(/\s+#.*$/, '').trim().replace(/^"|"$/g, '');
      if (/^\d+$/.test(v)) exposes++;
      else if (/:\d+(?:\/(tcp|udp))?$/.test(v)) ports++;
      continue;
    }
    if (ind <= pi) pi = -1;
  }

  // 堆上限：Xmx<n>m / JVM_XMX: <n>m
  const heap = [];
  for (const l of lines) {
    for (const m of l.matchAll(/Xmx(\d+)([mMgG])/g)) heap.push(Number(m[1]) * (m[2].toLowerCase() === 'g' ? 1024 : 1));
    const j = l.match(/JVM_XMX:\s*"?(\d+)([mMgG])/);
    if (j) heap.push(Number(j[1]) * (j[2].toLowerCase() === 'g' ? 1024 : 1));
  }
  const sum = heap.reduce((a, b) => a + b, 0);
  guardHits.mem_limit += (text.match(/^\s*mem_limit:/gm) || []).length;
  guardHits.cpus += (text.match(/^\s*cpus:/gm) || []).length;

  cSvc += svc.length;
  cPorts += ports;
  cExposes += exposes;
  cHeap += sum;
  heapCnt += heap.length;
  console.log(`${file}: services=${svc.length} ports=${ports} exposeOnly=${exposes} heapXmx=${sum}MiB rows=${heap.length}`);
}
console.log(`compose 合计: files=${composeFiles.length} services=${cSvc} ports=${cPorts} exposeOnly=${cExposes} heapXmx=${cHeap}MiB (${(cHeap / 1024).toFixed(2)}GiB) 堆行数=${heapCnt}`);
console.log(`资源限制守卫命中: mem_limit=${guardHits.mem_limit} cpus=${guardHits.cpus}`);

// ---------- 2. k8s 账 ----------
console.log('\n== 2. k8s 账（清单口径 vs 副本展开口径）==');
const k8sFiles = [];
for (const f of readdirSync('deploy/k8s').filter((x) => x.endsWith('.yaml')).sort()) k8sFiles.push(join('deploy/k8s', f));
for (const f of readdirSync('deploy/k8s/services').filter((x) => x.endsWith('.yaml')).sort()) k8sFiles.push(join('deploy/k8s/services', f));

const list = { reqCpu: 0, reqMem: 0, limCpu: 0, limMem: 0, pvc: 0 };
const scaled = { reqCpu: 0, reqMem: 0, limCpu: 0, limMem: 0, pvc: 0 };

for (const file of k8sFiles) {
  const docs = readFileSync(file, 'utf8').split(/^---\s*$/m);
  const acc = { reqCpu: 0, reqMem: 0, limCpu: 0, limMem: 0, pvc: 0 };
  const sacc = { reqCpu: 0, reqMem: 0, limCpu: 0, limMem: 0, pvc: 0 };
  const kinds = [];

  for (const doc of docs) {
    const kind = (doc.match(/^kind:\s*(\S+)/m) || [, ''])[1];
    if (!kind) continue;
    const repMatch = doc.match(/^\s*replicas:\s*(\d+)/m);
    const replicas = repMatch ? Number(repMatch[1]) : 1;
    const scalable = kind === 'Deployment' || kind === 'StatefulSet';
    kinds.push(scalable ? `${kind}×${replicas}` : kind);

    let mode = null;
    let modeIndent = -1;
    const d = { reqCpu: 0, reqMem: 0, limCpu: 0, limMem: 0, pvc: 0 };
    for (const raw of doc.split('\n')) {
      if (/^\s*#/.test(raw) || raw.trim() === '') continue;
      const indent = raw.length - raw.trimStart().length;
      if (mode && indent <= modeIndent) mode = null;
      const t = stripComment(raw);
      if (/^requests:\s*$/.test(t)) { mode = 'req'; modeIndent = indent; continue; }
      if (/^limits:\s*$/.test(t)) { mode = 'lim'; modeIndent = indent; continue; }
      if (!mode) continue;
      const kv = t.match(/^(cpu|memory|storage):\s*"?([^"\s]+)"?/);
      if (!kv) continue;
      const [, k, v] = kv;
      if (k === 'cpu') { if (mode === 'req') d.reqCpu += toMilliCpu(v); else d.limCpu += toMilliCpu(v); }
      else if (k === 'memory') { const mi = toMi(v); if (mode === 'req') d.reqMem += mi; else d.limMem += mi; }
      else d.pvc += toMi(v);
    }
    const mult = scalable ? replicas : 1;
    for (const k of Object.keys(acc)) {
      acc[k] += d[k];
      sacc[k] += d[k] * mult;
    }
  }
  for (const k of Object.keys(list)) {
    list[k] += acc[k];
    scaled[k] += sacc[k];
  }
  if (acc.reqCpu || acc.limCpu || acc.pvc) {
    console.log(`${file}: ${kinds.join(',')} req=${acc.reqCpu}m/${acc.reqMem}Mi lim=${acc.limCpu}m/${acc.limMem}Mi pvc=${acc.pvc}Mi | 展开 ${sacc.reqCpu}m/${sacc.reqMem}Mi lim=${sacc.limCpu}m/${sacc.limMem}Mi pvc=${sacc.pvc}Mi`);
  }
}
console.log(`清单口径: requests=${list.reqCpu}m/${list.reqMem}Mi limits=${list.limCpu}m/${list.limMem}Mi PVC=${(list.pvc / 1024).toFixed(0)}Gi`);
console.log(`副本展开: requests=${scaled.reqCpu}m/${scaled.reqMem}Mi limits=${scaled.limCpu}m/${scaled.limMem}Mi PVC=${(scaled.pvc / 1024).toFixed(0)}Gi`);
