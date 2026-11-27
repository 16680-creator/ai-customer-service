// learning-docs/tools/tmp/m15-06-container-res-limits.mjs
// 配套文档：learning-docs/15-计算机基础/06-容器隔离原理-namespace与cgroup.md
// 目的：把「容器被 cgroup 限住」这件事在本仓 + 本机上算清楚，分四段：
//   §1 deploy/k8s/**.yaml 里每个容器组的 requests/limits 现状表（B 级真值）
//   §2 合计账：整套清单的 requests/limits 总量
//   §3 deploy/docker-compose/*.yml 里资源限制字段的命中数（B 级真值，结论：0）
//   §4 JVM 堆外内存账本推演：用本机 java -XX:+PrintFlagsFinal 的实测常量，
//      算「512Mi limit + 256m 堆」到底还剩多少给 metaspace/direct/线程栈
//   §5 cgroup v2 的 cpu.max / cpu.weight 换算与 CFS 限流推演
//
// 只读仓库文件 + 纯算术，不联网、不起服务、不写文件。
// 一切「推演」都是算术，不是实测；正文引用时必须标注为推演。
import { readdirSync, readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const K8S = join(ROOT, 'deploy', 'k8s');
const COMPOSE = join(ROOT, 'deploy', 'docker-compose');

const MEM_TO_MI = { Ki: 1 / 1024, Mi: 1, Gi: 1024, Ti: 1024 * 1024, K: 1 / 1024, M: 1, G: 1024, T: 1024 * 1024 };
const toMi = (raw) => {
  const m = /^"?([\d.]+)([KMGT]i?|)"?$/.exec(raw.trim());
  if (!m) return NaN;
  return Number(m[1]) * (MEM_TO_MI[m[2] || 'Mi'] ?? 1);
};
const toMilliCpu = (raw) => {
  const s = raw.replace(/"/g, '').trim();
  return s.endsWith('m') ? Number(s.slice(0, -1)) : Number(s) * 1000;
};
const xmxMi = (txt) => {
  const m = /-Xmx(\d+)([mMgG])/.exec(txt);
  if (!m) return null;
  return Number(m[1]) * (/[gG]/.test(m[2]) ? 1024 : 1);
};
const ind = (line) => line.length - line.trimStart().length;
const pad = (s, n) => String(s).padEnd(n);
const rpad = (s, n) => String(s).padStart(n);

// ---------- §1 + §2：k8s 清单解析（逐行状态机，避开行尾中文注释与 volumeClaimTemplates 干扰）----------
const k8sFiles = [
  ...readdirSync(K8S).filter((f) => f.endsWith('.yaml')).map((f) => join(K8S, f)),
  ...readdirSync(join(K8S, 'services')).filter((f) => f.endsWith('.yaml')).map((f) => join(K8S, 'services', f)),
].sort();

function resourceGroups(file) {
  const out = [];
  let resIndent = -1;
  let mode = null;
  let cur = null;
  for (const line of readFileSync(file, 'utf8').split('\n')) {
    const bare = line.replace(/\s+#.*$/, '');
    const t = bare.trim();
    if (!t) continue;
    const indent = ind(bare);
    if (resIndent >= 0 && indent <= resIndent && !/^(requests|limits):/.test(t)) {
      if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
      cur = null;
      resIndent = -1;
      mode = null;
    }
    if (/^resources:\s*$/.test(t)) {
      if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
      cur = { reqCpuM: null, reqMemMi: null, limCpuM: null, limMemMi: null };
      resIndent = indent;
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
    if (kv[1] === 'cpu') cur[mode + 'CpuM'] = toMilliCpu(kv[2]);
    if (kv[1] === 'memory') cur[mode + 'MemMi'] = toMi(kv[2]);
    // storage: 属于 volumeClaimTemplates，与本篇无关，忽略
  }
  if (cur && (cur.reqMemMi || cur.limMemMi)) out.push(cur);
  return out;
}

const rows = [];
for (const file of k8sFiles) {
  const rel = file.slice(K8S.length + 1).replaceAll('\\', '/');
  const src = readFileSync(file, 'utf8');
  const xmx = xmxMi(src);
  resourceGroups(file).forEach((g, i) => rows.push({ file: rel, xmx: i === 0 ? xmx : null, ...g }));
}

console.log('=== §1 deploy/k8s 每个容器组的资源限制现状（真值来自 deploy/k8s/**.yaml）===');
console.log(`${pad('清单文件', 34)}${rpad('req.cpu', 9)}${rpad('lim.cpu', 9)}${rpad('req.mem', 10)}${rpad('lim.mem', 10)}${rpad('-Xmx', 7)}${rpad('堆/limit', 9)}`);
for (const r of rows) {
  const ratio = r.xmx && r.limMemMi ? `${((r.xmx / r.limMemMi) * 100).toFixed(0)}%` : '-';
  console.log(
    `${pad(r.file, 34)}${rpad(r.reqCpuM ?? '-', 9)}${rpad(r.limCpuM ?? '-', 9)}${rpad((r.reqMemMi ?? '-') + 'Mi', 10)}${rpad((r.limMemMi ?? '-') + 'Mi', 10)}${rpad(r.xmx ? r.xmx + 'M' : '-', 7)}${rpad(ratio, 9)}`
  );
}

const sum = (k) => rows.reduce((a, r) => a + (r[k] || 0), 0);
const jvmRows = rows.filter((r) => r.xmx != null);
console.log(
  `\n合计（${rows.length} 个容器组 / ${k8sFiles.length} 个清单文件）：` +
    `requests.cpu=${sum('reqCpuM')}m  limits.cpu=${sum('limCpuM')}m  ` +
    `requests.memory=${sum('reqMemMi')}Mi  limits.memory=${sum('limMemMi')}Mi`
);
console.log(
  `其中带 -Xmx 的 JVM 容器组 ${jvmRows.length} 个：堆合计 ${jvmRows.reduce((a, r) => a + r.xmx, 0)}MB，` +
    `它们 limits.memory 合计 ${jvmRows.reduce((a, r) => a + r.limMemMi, 0)}Mi；未写 -Xmx 的容器组 ${rows.length - jvmRows.length} 个`
);

// ---------- §3：compose 资源限制扫描（本仓 7 份 compose 全都没有 cgroup 级限制）----------
console.log('\n=== §3 compose 资源限制字段命中数（deploy/docker-compose/*.yml + 仓库根 docker-compose.yml）===');
const LIMIT_FIELDS = /^\s*(mem_limit|memswap_limit|cpus|cpu_shares|cpu_quota|pids_limit|stop_grace_period|deploy|cpuset|blkio_config)\s*:/;
const composeFiles = [
  ...readdirSync(COMPOSE).filter((n) => n.endsWith('.yml')).sort().map((n) => join(COMPOSE, n)),
  join(ROOT, 'docker-compose.yml'),
];
let totalServices = 0;
let totalHits = 0;
for (const path of composeFiles) {
  const f = path === join(ROOT, 'docker-compose.yml') ? 'docker-compose.yml（仓库根）' : path.slice(COMPOSE.length + 1);
  const lines = readFileSync(path, 'utf8').split('\n');
  const hits = lines.filter((l) => LIMIT_FIELDS.test(l));
  // 只数 services: 块下的二级键（缩进恰好 2 个空格），避免把 volumes/networks 里的键算进去
  let inServices = false;
  let services = 0;
  for (const l of lines) {
    if (/^services:\s*$/.test(l)) { inServices = true; continue; }
    if (!inServices) continue;
    if (/^\S/.test(l)) { inServices = false; continue; }
    if (/^ {2}[A-Za-z0-9_.-]+:\s*$/.test(l)) services++;
  }
  totalServices += services;
  totalHits += hits.length;
  console.log(`${pad(f, 40)} services 项=${rpad(services, 3)}  资源限制类字段命中=${hits.length}`);
  for (const h of hits) console.log(`    ! ${h.trim()}`);
}
console.log(`合计：${composeFiles.length} 份 compose，${totalServices} 个 service，资源限制类字段命中 ${totalHits} 个`);

// ---------- §4：JVM 堆外账本推演（常量全部来自本机 java -XX:+PrintFlagsFinal 实测）----------
const CODE_CACHE_MI = 251658240 / 1024 / 1024; // ReservedCodeCacheSize 实测 251658240 字节 = 240MiB
const STACK_MI = 1; // ThreadStackSize 实测打印 0 = 走平台默认；x64 默认 1MiB 属 JDK 文档值，不是本机打印值
console.log('\n=== §4 JVM 堆外内存账本推演（常量：ReservedCodeCacheSize=240MiB 实测；线程栈按 x64 默认 1MiB 估）===');
console.log('说明：这是「最坏情况上界」算术，不是实测。code cache 240MiB 是上限，实际占用取决于 JIT 编译量。');
console.log(`${pad('limit', 10)}${rpad('-Xmx', 8)}${rpad('线程数', 8)}${rpad('codeCache', 12)}${rpad('线程栈', 10)}${rpad('留给堆外', 12)}判读`);
const CASES = [
  { limit: 512, xmx: 256, threads: 30 }, // 本项目 6 个服务在 k8s 里的配置
  { limit: 512, xmx: 256, threads: 60 }, // 本机实测一个 Spring 进程 60~90 线程
  { limit: 1024, xmx: 512, threads: 30 }, // ai-chat-service 的配置
  { limit: 1024, xmx: 1024, threads: 30 }, // 常见错误：Xmx 直接写满 limit
];
for (const c of CASES) {
  const stack = c.threads * STACK_MI;
  const left = c.limit - c.xmx - CODE_CACHE_MI - stack;
  const verdict = left < 0 ? '账本已为负，metaspace 一涨就 OOMKilled' : left < 128 ? '只剩不到 128MiB 给 metaspace+direct' : '尚有余量';
  console.log(
    `${pad(c.limit + 'Mi', 10)}${rpad(c.xmx + 'M', 8)}${rpad(c.threads, 8)}${rpad(CODE_CACHE_MI + 'MiB', 12)}${rpad(stack + 'MiB', 10)}${rpad(left + 'MiB', 12)}${verdict}`
  );
}
console.log(`附带：MaxDirectMemorySize 实测打印 0，表示未设上限、默认取最大堆大小 → 堆外还能再被 direct buffer 吃掉约一个 -Xmx。`);
console.log(`附带：MaxMetaspaceSize 实测打印 18446744073709551615（无上限）→ metaspace 泄漏不受堆限制，直接算进容器 RSS。`);

// ---------- §5：cgroup v2 cpu.max / cpu.weight 换算 + CFS 限流推演 ----------
const weightOf = (shares) => Math.round(1 + ((shares - 2) * 9999) / 262142); // 公式出自 cgroup v2 Documentation/admin-guide/cgroup-v2.rst
console.log('\n=== §5 k8s 的 cpu 值 → cgroup 文件内容（换算，不是实测）===');
console.log('A 列：limits.cpu 落 cpu.max（v1 是 cfs_quota_us/cfs_period_us），period 固定 100000us=100ms');
console.log(`${pad('limits.cpu', 12)}${pad('cpu.max', 22)}含义`);
for (const m of [100, 250, 500, 1000, 2000]) {
  const quota = m * 100; // m 毫核 × 100ms = m/1000 * 100000us = m*100us
  console.log(`${pad(m + 'm', 12)}${pad(`"${quota} 100000"`, 22)}每 100ms 最多用 ${(quota / 1000).toFixed(1)}ms CPU 时间`);
}
console.log('\nB 列：requests.cpu 落 cpu.shares（v1）/ cpu.weight（v2），只在争抢时决定配比');
console.log(`${pad('requests.cpu', 14)}${pad('cpu.shares', 12)}cpu.weight（v2 换算）`);
for (const m of [100, 200, 250, 500]) {
  const shares = m * 1024; // k8s: requests.cpu × 1024
  console.log(`${pad(m + 'm', 14)}${pad(shares, 12)}${weightOf(shares)}`);
}
console.log('注意：limits.cpu 是硬上限（超了就被 throttle），requests.cpu 只在节点争抢时决定配比——空闲节点上 requests 不限制任何东西。');

console.log('\n=== §5b CFS 限流推演：limit=500m（每 100ms 只有 50ms 配额）===');
for (const workers of [1, 2, 4]) {
  const perWorker = 500 / workers;
  console.log(`  ${workers} 个线程同时满跑 → 合计仍只有 500m 配额，每线程等效约 ${perWorker}m，其余时间在 throttled 队列里等下一个 100ms 周期`);
}
console.log('  判读：throttling 不是把 CPU 变慢，而是「按 100ms 周期发放配额」，超发就被掐断到下一周期，所以尾延迟会突然出现台阶。');
