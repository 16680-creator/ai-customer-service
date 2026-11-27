#!/usr/bin/env node
/**
 * m16-03-argocd-lint.mjs — ArgoCD Application/AppProject 结构自检 + 仓库真实清单体检
 *
 * 用法：
 *   node learning-docs/tools/tmp/m16-03-argocd-lint.mjs                 # 常规自检
 *   node learning-docs/tools/tmp/m16-03-argocd-lint.mjs --drop-exclude  # 故意去掉 directory.exclude，演示双写报警
 *
 * 设计约束（对本机环境的让步，不是偷懒）：
 *   1. 零依赖：本机没装 js-yaml（require.resolve('js-yaml') 失败），所以自带一个
 *      只支持「映射 / 块序列 / 标量 / 行内列表」的极简 YAML 子集解析器。
 *      不支持锚点、别名、多文档、块标量 —— 遇到这些结构会显式报错，不会静默猜。
 *   2. 只读：不写任何文件、不连集群、不跑 kubectl/argocd（本机也没有）。
 *      因此本脚本只能证明「清单结构自洽」，不能证明「ArgoCD 真能同步成功」。
 *   3. 提案骨架镜像：deploy/argocd/ 在真实仓库里不存在，所以脚本把
 *      tools/tmp/m16-03-argocd/ 当作仓库根的镜像去解析路径，命中镜像的路径标 PROPOSAL-ONLY。
 */
import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { join, dirname, resolve, relative, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(HERE, '..', '..', '..'); // learning-docs/tools/tmp -> 仓库根
const MIRROR = join(HERE, 'm16-03-argocd'); // 提案骨架镜像（视作仓库根）
// 输出里给提案文件加前缀，避免被文档校验器当成「仓库内真实存在的交付物路径」
const MIRROR_LABEL = 'learning-docs/tools/tmp/m16-03-argocd/';
const DROP_EXCLUDE = process.argv.includes('--drop-exclude');

// ---------------------------------------------------------------- 极简 YAML
function stripComment(line) {
  let out = '';
  let quote = null;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (quote) {
      out += c;
      if (c === quote) quote = null;
      continue;
    }
    if (c === '"' || c === "'") { quote = c; out += c; continue; }
    if (c === '#' && (i === 0 || /\s/.test(line[i - 1]))) break;
    out += c;
  }
  return out.replace(/\s+$/, '');
}

function scalar(raw) {
  const s = raw.trim();
  if (s === '' || s === '~' || s === 'null') return null;
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) return s.slice(1, -1);
  if (s.startsWith('[') && s.endsWith(']')) {
    const inner = s.slice(1, -1).trim();
    return inner === '' ? [] : inner.split(',').map((x) => scalar(x));
  }
  if (s === 'true') return true;
  if (s === 'false') return false;
  if (/^-?\d+$/.test(s)) return Number(s);
  return s;
}

function prepare(text) {
  const lines = [];
  const raw = text.split(/\r?\n/);
  for (let n = 0; n < raw.length; n++) {
    if (raw[n].includes('\t')) throw new Error(`第 ${n + 1} 行含 TAB，YAML 不允许用 TAB 缩进`);
    const content = stripComment(raw[n]);
    if (content.trim() === '') continue;
    if (content.trim() === '---' || content.trim() === '...') continue;
    const indent = content.length - content.trimStart().length;
    lines.push({ indent, content: content.trim(), no: n + 1 });
  }
  return lines;
}

function parseMap(lines, i, indent) {
  const obj = {};
  while (i < lines.length && lines[i].indent === indent && !lines[i].content.startsWith('-')) {
    const l = lines[i];
    const c = l.content;
    const ci = c.indexOf(':');
    if (ci <= 0) throw new Error(`第 ${l.no} 行不是 key: value —— [${c}]`);
    const key = c.slice(0, ci).trim();
    const rest = c.slice(ci + 1).trim();
    if (rest !== '') { obj[key] = scalar(rest); i++; continue; }
    if (i + 1 < lines.length && lines[i + 1].indent > indent) {
      const r = parseNode(lines, i + 1, lines[i + 1].indent);
      obj[key] = r.value;
      i = r.next;
    } else { obj[key] = null; i++; }
  }
  return { value: obj, next: i };
}

function parseSeq(lines, i, indent) {
  const arr = [];
  while (i < lines.length && lines[i].indent === indent && lines[i].content.startsWith('-')) {
    const rest = lines[i].content.replace(/^-\s*/, '');
    if (rest === '') {
      if (i + 1 < lines.length && lines[i + 1].indent > indent) {
        const r = parseNode(lines, i + 1, lines[i + 1].indent);
        arr.push(r.value);
        i = r.next;
      } else { arr.push(null); i++; }
      continue;
    }
    if (/^[^\s:][^:]*:(\s|$)/.test(rest)) {
      // "- key: value" → 把该行虚拟成 indent+2 的映射首行
      const sub = [{ indent: indent + 2, content: rest, no: lines[i].no }, ...lines.slice(i + 1)];
      const r = parseMap(sub, 0, indent + 2);
      arr.push(r.value);
      // sub[j] (j>=1) 对应 lines[i+j]：resume 下标直接平移
      i = i + r.next;
      continue;
    }
    arr.push(scalar(rest));
    i++;
  }
  return { value: arr, next: i };
}

function parseNode(lines, i, indent) {
  return lines[i].content.startsWith('-') ? parseSeq(lines, i, indent) : parseMap(lines, i, indent);
}

function parseYaml(text) {
  const lines = prepare(text);
  if (lines.length === 0) return null;
  const r = parseNode(lines, 0, lines[0].indent);
  if (r.next < lines.length) throw new Error(`第 ${lines[r.next].no} 行无法归入任何层级（缩进错乱）`);
  return r.value;
}

// ---------------------------------------------------------------- 工具
const fwd = (p) => p.replace(/\\/g, '/');
const relRepo = (abs) => fwd(relative(REPO_ROOT, abs));

function walkYaml(root) {
  const out = [];
  for (const name of readdirSync(root)) {
    const p = join(root, name);
    const st = statSync(p);
    if (st.isDirectory()) out.push(...walkYaml(p));
    else if (/\.ya?ml$/.test(name)) out.push(p);
  }
  return out;
}

function globToRegex(glob) {
  const body = fwd(glob).replace(/[.+^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '[^/]*').replace(/\?/g, '[^/]');
  return new RegExp(`^${body}$`);
}

const errs = [];
const warns = [];
const infos = [];

// ---------------------------------------------------------------- 1. 提案骨架
const appsDir = join(MIRROR, 'deploy', 'argocd');
const mirrorFiles = existsSync(appsDir) ? walkYaml(appsDir) : [];
const docs = [];
for (const f of mirrorFiles) {
  let obj;
  try {
    obj = parseYaml(readFileSync(f, 'utf8'));
  } catch (e) {
    errs.push(`YAML 解析失败 ${relRepo(f)}: ${e.message}`);
    continue;
  }
  docs.push({ file: f, rel: MIRROR_LABEL + fwd(relative(MIRROR, f)), obj });
}

const projects = new Map();
const apps = [];
for (const d of docs) {
  const o = d.obj || {};
  if (o.kind === 'AppProject') projects.set(o.metadata?.name, { ...d, o });
  else if (o.kind === 'Application') apps.push({ ...d, o });
  else errs.push(`${d.rel}: kind=${o.kind} 既不是 Application 也不是 AppProject`);
}

console.log('='.repeat(78));
console.log(' 1. 提案骨架自检（镜像根 = learning-docs/tools/tmp/m16-03-argocd）');
console.log('='.repeat(78));
console.log(`扫描到 ${docs.length} 个 YAML：Application ${apps.length} 个 / AppProject ${projects.size} 个`);
console.log(`DROP_EXCLUDE 模式 = ${DROP_EXCLUDE ? '开（故意破坏 directory.exclude）' : '关'}`);

const seen = new Map();
const coverage = new Map(); // relRepo(file) -> [appName]

for (const a of apps) {
  const o = a.o;
  const spec = o.spec || {};
  const src = spec.source || {};
  const dst = spec.destination || {};
  const pol = spec.syncPolicy || {};
  const auto = pol.automated || null;
  const dirOpt = src.directory || {};
  let exclude = dirOpt.exclude ?? dirOpt.excludes ?? null;
  if (DROP_EXCLUDE && a.rel.includes('aics-platform')) exclude = null;
  const recurse = dirOpt.recurse === true;

  const need = [
    ['metadata.name', o.metadata?.name],
    ['metadata.namespace', o.metadata?.namespace],
    ['spec.project', spec.project],
    ['spec.source.repoURL', src.repoURL],
    ['spec.source.targetRevision', src.targetRevision],
    ['spec.source.path', src.path],
    ['spec.destination.server', dst.server],
    ['spec.destination.namespace', dst.namespace],
  ];
  const missing = need.filter(([, v]) => v === undefined || v === null || v === '').map(([k]) => k);
  if (missing.length) errs.push(`${o.metadata?.name || a.rel}: 缺必填字段 ${missing.join(', ')}`);

  const name = o.metadata?.name;
  if (seen.has(name)) errs.push(`Application 名字重复: ${name}（${a.rel} 与 ${seen.get(name)}，后者会覆盖前者）`);
  seen.set(name, a.rel);

  const proj = projects.get(spec.project);
  if (!proj) errs.push(`${name}: spec.project=${spec.project} 没有对应的 AppProject 定义`);
  else {
    const allowedRepo = (proj.o.spec?.sourceRepos || []).some((r) => r === '*' || r === src.repoURL);
    if (!allowedRepo) errs.push(`${name}: repoURL ${src.repoURL} 不在 AppProject ${spec.project} 的 sourceRepos 白名单里`);
    const allowedNs = (proj.o.spec?.destinations || []).some(
      (x) => (x.server === dst.server || x.server === '*') && (x.namespace === dst.namespace || x.namespace === '*'),
    );
    if (!allowedNs) errs.push(`${name}: destination ${dst.server}/${dst.namespace} 不在 AppProject ${spec.project} 的 destinations 白名单里`);
  }

  // path 解析：仓库根 → 镜像根
  let base = src.path ? join(REPO_ROOT, src.path) : null;
  let pathState = 'MISSING';
  if (base && existsSync(base)) pathState = 'REPO';
  else if (src.path && existsSync(join(MIRROR, src.path))) {
    base = join(MIRROR, src.path);
    pathState = 'PROPOSAL-ONLY';
  }
  if (pathState === 'MISSING') errs.push(`${name}: spec.source.path=${src.path} 在仓库与提案镜像里都不存在 → ArgoCD 只会报 path does not exist`);

  let covered = [];
  if (pathState !== 'MISSING') {
    const all = walkYaml(base);
    covered = recurse ? all : all.filter((f) => dirname(f) === base);
    if (exclude) {
      const globs = String(exclude).split(',').map((s) => s.trim()).filter(Boolean).map(globToRegex);
      covered = covered.filter((f) => {
        const r = fwd(relative(base, f));
        return !globs.some((g) => g.test(r));
      });
    }
  }
  for (const f of covered) {
    const k = relRepo(f).replace('learning-docs/tools/tmp/m16-03-argocd/', '');
    if (!coverage.has(k)) coverage.set(k, []);
    coverage.get(k).push(name);
  }

  const polText = auto
    ? `automated(prune=${auto.prune === true} selfHeal=${auto.selfHeal === true})`
    : 'manual（无 automated，只报 OutOfSync 不改集群）';
  console.log('');
  console.log(`[${name}]  ${a.rel}`);
  console.log(`   project : ${spec.project}   destination: ${dst.server} / ${dst.namespace}`);
  console.log(`   source  : ${src.targetRevision} @ ${src.path}  (recurse=${recurse}, exclude=${exclude === null ? '无' : JSON.stringify(exclude)})`);
  console.log(`   path    : ${pathState}   → 命中 ${covered.length} 个清单`);
  console.log(`   policy  : ${polText}   retry.limit=${pol.retry?.limit ?? '-'}`);

  if (auto && auto.prune === true && auto.selfHeal !== true) {
    warns.push(`${name}: prune=true 但 selfHeal=false —— git 里删掉的会被删，集群里手改的不回滚，这是最容易出事的组合`);
  }
  if (!auto) infos.push(`${name}: 无 automated → 只做漂移可视化（第一阶段就该这样起步）`);
  if (auto && auto.prune === true) {
    const opts = pol.syncOptions || [];
    if (!opts.includes('CreateNamespace=true')) {
      infos.push(`${name}: prune=true 且无 CreateNamespace=true —— 首次同步到新 namespace 会失败`);
    }
  }
}

// 双写检测
console.log('');
for (const [f, owners] of coverage) {
  if (owners.length > 1) {
    warns.push(`双写：${f} 同时被 [${owners.join(' + ')}] 管理 —— ArgoCD 会互相把对方的改动当成漂移，来回抖动`);
  }
}
if (![...coverage.values()].some((o) => o.length > 1)) {
  console.log(`覆盖矩阵：${coverage.size} 个清单，无重叠 ✓`);
}

// ---------------------------------------------------------------- 2. 真实清单体检
const k8sDir = join(REPO_ROOT, 'deploy', 'k8s');
const realFiles = existsSync(k8sDir) ? walkYaml(k8sDir) : [];
let totalLines = 0;
const images = [];
const nsCount = new Map();
const kindCount = new Map();
let strategy = 0;
let grace = 0;
let startup = 0;
let liveness = 0;
let readiness = 0;
for (const f of realFiles) {
  const t = readFileSync(f, 'utf8');
  const lines = t.split(/\r?\n/);
  totalLines += lines.length;
  for (const l of lines) {
    const im = /^\s*image:\s*(\S+)/.exec(l);
    if (im) images.push({ file: relRepo(f), image: im[1] });
    const ns = /^\s*namespace:\s*(\S+)/.exec(l);
    if (ns) nsCount.set(ns[1], (nsCount.get(ns[1]) || 0) + 1);
    const kd = /^kind:\s*(\S+)/.exec(l);
    if (kd) kindCount.set(kd[1], (kindCount.get(kd[1]) || 0) + 1);
    if (/^\s*strategy:\s*$/.test(l)) strategy++;
    if (/^\s*terminationGracePeriodSeconds:/.test(l)) grace++;
    if (/^\s*startupProbe:/.test(l)) startup++;
    if (/^\s*livenessProbe:/.test(l)) liveness++;
    if (/^\s*readinessProbe:/.test(l)) readiness++;
  }
}
const latest = images.filter((x) => /:latest$/.test(x.image));
const noTag = images.filter((x) => !/:[^/]+$/.test(x.image));
const pinned = images.filter((x) => !/:latest$/.test(x.image) && /:[^/]+$/.test(x.image));

console.log('');
console.log('='.repeat(78));
console.log(' 2. 仓库真实清单体检（deploy/k8s/**/*.yaml，纯文本扫描，不做语义解析）');
console.log('='.repeat(78));
console.log(`清单规模 : ${realFiles.length} 个文件 / ${totalLines} 行`);
console.log(`image 行 : ${images.length}  →  :latest ${latest.length} / 完全无 tag ${noTag.length} / 固定 tag ${pinned.length}`);
console.log(`namespace: ${[...nsCount].map(([k, v]) => `${k}=${v}`).join('  ')}`);
console.log(`kind     : ${[...kindCount].sort().map(([k, v]) => `${k}=${v}`).join('  ')}`);
console.log(`探针     : livenessProbe=${liveness} readinessProbe=${readiness} startupProbe=${startup}`);
console.log(`滚动策略 : strategy:=${strategy}  terminationGracePeriodSeconds=${grace}`);
const managed = new Set([...coverage.keys()]);
const realRel = realFiles.map(relRepo);
const unmanaged = realRel.filter((f) => !managed.has(f));
console.log(`Application 覆盖范围: 真实清单 ${realRel.filter((f) => managed.has(f)).length}/${realRel.length} 个；未被纳管 ${unmanaged.length} 个`);
for (const f of unmanaged) console.log(`   · 未纳管 ${f}`);
if (latest.length || noTag.length) {
  warns.push(`浮动 tag ${latest.length + noTag.length} 处：pull 模型下 git 无法唯一确定集群跑的是哪个镜像，回滚也无从重放`);
}

// ---------------------------------------------------------------- 汇总
console.log('');
console.log('='.repeat(78));
console.log(` 结论：errors=${errs.length}  warnings=${warns.length}  info=${infos.length}`);
console.log('='.repeat(78));
errs.forEach((e) => console.log(`  ERROR   ${e}`));
warns.forEach((w) => console.log(`  WARN    ${w}`));
infos.forEach((x) => console.log(`  INFO    ${x}`));
console.log(errs.length === 0 ? '  自检通过：清单结构自洽（≠ ArgoCD 能同步成功，本机无 argocd/kubectl，未实测）' : '  自检失败：先修 ERROR');
