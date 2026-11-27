// helm16-render-sim.mjs —— 本机没有 helm，用 node 复现 Helm 渲染流水线里"与判读有关"的四件事：
//   1) values 层级覆盖（默认 → 环境 overlay → --set）谁赢
//   2) --set 的类型推断坑（数字 / 逗号 / 空值 / 引号）
//   3) 渲染产物丢注释（按现有 manifest 的注释密度实测）
//   4) 等价性验收：把 deploy/k8s/services/*.yaml 抽象成 values 再渲染回去，逐字段比对
// 明确边界：这不是 Helm，也不解析 Go template。第 1/2/4 步是我按 Helm 公开语义写的模拟器，
//          用来说明坑与验收方法；真实 helm template 的输出未本机实测。
// 运行：node learning-docs/tools/tmp/helm16-render-sim.mjs
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const ROOT = 'D:\\Projects\\Persion\\ai-customer-service';
const SVC_DIR = join(ROOT, 'deploy', 'k8s', 'services');

/* ---------------- 迷你 YAML 子集解析器（只认本项目清单用到的写法） ---------------- */
export function parseYaml(text) {
  const raw = text.split(/\r?\n/);
  const lines = [];
  for (const l of raw) {
    if (l.trim() === '' || l.trim() === '---') continue;
    if (l.trim().startsWith('#')) continue; // 整行注释：不参与结构解析（但它正是渲染时会丢的东西）
    lines.push(l);
  }
  let i = 0;
  const indentOf = (l) => l.match(/^ */)[0].length;

  const stripComment = (s) => {
    let out = '';
    let q = null;
    for (let k = 0; k < s.length; k++) {
      const c = s[k];
      if (q) { out += c; if (c === q) q = null; }
      else if (c === '"' || c === "'") { q = c; out += c; }
      else if (c === '#' && (k === 0 || s[k - 1] === ' ')) break;
      else out += c;
    }
    return out.trimEnd();
  };
  const scalar = (v) => {
    v = stripComment(v).trim();
    if (v === '') return null;
    if (/^-?\d+$/.test(v)) return Number(v);
    if (v === 'true' || v === 'false') return v === 'true';
    if (v === '{}') return {};
    if (v === '[]') return [];
    if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) return v.slice(1, -1);
    return v;
  };

  function parseBlock(indent) {
    if (i < lines.length && indentOf(lines[i]) === indent && lines[i].trim().startsWith('-')) return parseSeq(indent);
    return parseMap(indent);
  }
  function parseMap(indent) {
    const obj = {};
    while (i < lines.length) {
      const l = lines[i];
      const ind = indentOf(l);
      const t = l.trim();
      if (ind < indent) break;
      if (ind > indent) { i++; continue; }
      if (t.startsWith('- ')) break;
      const m = t.match(/^([^:]+):\s*(.*)$/);
      if (!m) { i++; continue; }
      const key = m[1].trim();
      const rest = m[2];
      if (stripComment(rest).trim() === '') {
        i++;
        obj[key] = i < lines.length && indentOf(lines[i]) > indent ? parseBlock(indentOf(lines[i])) : null;
      } else { obj[key] = scalar(rest); i++; }
    }
    return obj;
  }
  function parseSeq(indent) {
    const arr = [];
    while (i < lines.length) {
      const l = lines[i];
      if (indentOf(l) !== indent) break;
      const t = l.trim();
      if (!t.startsWith('-')) break;
      const rest = t.slice(1).trimStart();
      if (rest === '') { i++; arr.push(parseBlock(indentOf(lines[i]))); continue; }
      if (/^[^:]+:/.test(rest)) {
        // "- name: x" 这类行内起始的 map：把当前行改写成更深的缩进，交给 parseMap 继续吃
        const childIndent = indent + 2;
        lines[i] = ' '.repeat(childIndent) + rest;
        arr.push(parseMap(childIndent));
        continue;
      }
      arr.push(scalar(rest));
      i++;
    }
    return arr;
  }
  return parseBlock(indentOf(lines[0]));
}

const flatten = (obj, prefix = '', out = {}) => {
  if (obj === null || typeof obj !== 'object') { out[prefix] = obj; return out; }
  if (Array.isArray(obj)) obj.forEach((v, k) => flatten(v, `${prefix}[${k}]`, out));
  else for (const [k, v] of Object.entries(obj)) flatten(v, prefix ? `${prefix}.${k}` : k, out);
  return out;
};

/* ---------------- 第 1 步：values 层级覆盖 ---------------- */
function deepMerge(base, over) {
  if (over === null || typeof over !== 'object' || Array.isArray(over)) return over;
  const out = { ...base };
  for (const [k, v] of Object.entries(over)) {
    out[k] = base && typeof base[k] === 'object' && !Array.isArray(base[k]) && v && typeof v === 'object' && !Array.isArray(v)
      ? deepMerge(base[k], v) : v;
  }
  return out;
}

/* ---------------- 第 2 步：--set 解析（按 Helm strvals 的公开语义） ---------------- */
export function parseSet(str, opts = {}) {
  const eff = {};
  const types = {};
  for (const pair of splitTop(str)) {
    const eq = pair.indexOf('=');
    const key = (eq === -1 ? pair : pair.slice(0, eq)).trim();
    const rawVal = eq === -1 ? '' : pair.slice(eq + 1);
    if (key === '') throw new Error(`--set 项缺少键名: ${JSON.stringify(pair)}`);
    const { value, type } = opts.forceString ? { value: rawVal, type: 'string(强制)' } : inferType(rawVal);
    setPath(eff, key, value);
    types[key] = type;
  }
  return { eff, types };
}
function splitTop(str) { // 只按未被反斜杠转义的逗号切
  const out = [];
  let cur = '';
  for (let k = 0; k < str.length; k++) {
    if (str[k] === '\\' && k + 1 < str.length) { cur += str[k + 1]; k++; continue; }
    if (str[k] === ',') { out.push(cur); cur = ''; continue; }
    cur += str[k];
  }
  if (cur !== '') out.push(cur);
  return out;
}
export function inferType(v) {
  if (v === '') return { value: null, type: 'null' };
  if (v === 'null') return { value: null, type: 'null(键被置空)' };
  if (v === 'true' || v === 'false') return { value: v === 'true', type: 'boolean' };
  if (/^-?\d+$/.test(v)) return { value: Number(v), type: 'number' };
  if (/^-?\d+\.\d+$/.test(v)) return { value: Number(v), type: 'number(float)' };
  return { value: v, type: 'string' };
}
function setPath(obj, path, value) {
  const parts = path.split('.');
  let cur = obj;
  for (let k = 0; k < parts.length - 1; k++) {
    const p = parts[k];
    if (cur[p] === undefined || cur[p] === null || typeof cur[p] !== 'object') cur[p] = {};
    cur = cur[p];
  }
  cur[parts.at(-1)] = value;
}

/* ---------------- 第 3 步：把现有清单抽成 values ---------------- */
function loadServiceFiles() {
  return readdirSync(SVC_DIR).filter((f) => f.endsWith('.yaml')).sort().map((f) => {
    const p = join(SVC_DIR, f);
    const text = readFileSync(p, 'utf8');
    const lines = text.split(/\r?\n/);
    const docs = [];
    let buf = [];
    for (const l of lines) {
      if (l.trim() === '---') { if (buf.length) docs.push(buf.join('\n')); buf = []; }
      else buf.push(l);
    }
    if (buf.length) docs.push(buf.join('\n'));
    const parsed = docs.map((d) => parseYaml(d));
    const svc = parsed.find((d) => d?.kind === 'Service');
    const dep = parsed.find((d) => d?.kind === 'Deployment');
    const c = dep.spec.template.spec.containers[0];
    return {
      file: f,
      commentLines: lines.filter((l) => /^\s*#/.test(l)).length,
      totalLines: lines.filter((l) => l.trim() !== '').length,
      name: dep.metadata.name,
      namespace: dep.metadata.namespace,
      labels: dep.metadata.labels,
      selector: dep.spec.selector.matchLabels,
      podLabels: dep.spec.template.metadata.labels,
      svc: { type: svc.spec.type, ports: svc.spec.ports, selector: svc.spec.selector },
      deploy: {
        replicas: dep.spec.replicas,
        image: c.image,
        imagePullPolicy: c.imagePullPolicy ?? null,
        env: c.env ?? [],
        containerPorts: c.ports ?? [],
        resources: c.resources,
        livenessProbe: c.livenessProbe,
        readinessProbe: c.readinessProbe,
      },
    };
  });
}

/* ---------------- 第 4 步：渲染（替代 helm template 的最小实现） ---------------- */
const q = (v) => `"${String(v)}"`;
function renderService(v, opts = {}) {
  const quoteEnv = opts.quoteEnv !== false;
  const L = [];
  L.push('apiVersion: v1');
  L.push('kind: Service');
  L.push('metadata:');
  L.push(`  name: ${v.name}`);
  L.push(`  namespace: ${v.namespace}`);
  L.push('  labels:');
  for (const [k, val] of Object.entries(v.labels)) L.push(`    ${k}: ${q(val)}`);
  L.push('spec:');
  L.push(`  type: ${v.svc.type}`);
  L.push('  ports:');
  for (const p of v.svc.ports) {
    L.push(`    - name: ${p.name ?? '<no value>'}`);
    L.push(`      port: ${p.port}`);
    L.push(`      targetPort: ${p.targetPort}`);
    if (p.nodePort) L.push(`      nodePort: ${p.nodePort}`);
    if (p.protocol) L.push(`      protocol: ${p.protocol}`);
  }
  L.push('  selector:');
  for (const [k, val] of Object.entries(v.svc.selector)) L.push(`    ${k}: ${val}`);
  L.push('---');
  L.push('apiVersion: apps/v1');
  L.push('kind: Deployment');
  L.push('metadata:');
  L.push(`  name: ${v.name}`);
  L.push(`  namespace: ${v.namespace}`);
  L.push('  labels:');
  for (const [k, val] of Object.entries(v.labels)) L.push(`    ${k}: ${q(val)}`);
  L.push('spec:');
  L.push(`  replicas: ${v.deploy.replicas}`);
  L.push('  selector:');
  L.push('    matchLabels:');
  for (const [k, val] of Object.entries(v.selector)) L.push(`      ${k}: ${val}`);
  L.push('  template:');
  L.push('    metadata:');
  L.push('      labels:');
  for (const [k, val] of Object.entries(v.podLabels)) L.push(`        ${k}: ${val}`);
  L.push('    spec:');
  L.push('      containers:');
  L.push(`        - name: ${v.name}`);
  L.push(`          image: ${v.deploy.image}`);
  if (v.deploy.imagePullPolicy) L.push(`          imagePullPolicy: ${v.deploy.imagePullPolicy}`);
  if (v.deploy.env.length) {
    L.push('          env:');
    for (const e of v.deploy.env) {
      L.push(`            - name: ${e.name}`);
      if (e.valueFrom) {
        const ref = e.valueFrom.secretKeyRef;
        L.push('              valueFrom:');
        L.push('                secretKeyRef:');
        L.push(`                  name: ${ref.name}`);
        L.push(`                  key: ${ref.key}`);
        if (ref.optional !== undefined) L.push(`                  optional: ${ref.optional}`);
      } else if (quoteEnv) L.push(`              value: ${q(e.value)}`);
      else L.push(`              value: ${e.value}`);
    }
  }
  if (v.deploy.containerPorts.length) {
    L.push('          ports:');
    for (const p of v.deploy.containerPorts) {
      L.push(`            - name: ${p.name ?? '<no value>'}`);
      L.push(`              containerPort: ${p.containerPort}`);
    }
  }
  L.push('          resources:');
  L.push('            requests:');
  L.push(`              cpu: ${q(v.deploy.resources.requests.cpu)}`);
  L.push(`              memory: ${q(v.deploy.resources.requests.memory)}`);
  L.push('            limits:');
  L.push(`              cpu: ${q(v.deploy.resources.limits.cpu)}`);
  L.push(`              memory: ${q(v.deploy.resources.limits.memory)}`);
  for (const kind of ['livenessProbe', 'readinessProbe']) {
    const pr = v.deploy[kind];
    if (!pr) continue;
    L.push(`          ${kind}:`);
    if (pr.tcpSocket) { L.push('            tcpSocket:'); L.push(`              port: ${pr.tcpSocket.port}`); }
    if (pr.httpGet) {
      L.push('            httpGet:');
      L.push(`              path: ${pr.httpGet.path}`);
      L.push(`              port: ${pr.httpGet.port}`);
    }
    if (pr.initialDelaySeconds !== undefined) L.push(`            initialDelaySeconds: ${pr.initialDelaySeconds}`);
    if (pr.periodSeconds !== undefined) L.push(`            periodSeconds: ${pr.periodSeconds}`);
    if (pr.timeoutSeconds !== undefined) L.push(`            timeoutSeconds: ${pr.timeoutSeconds}`);
    if (pr.failureThreshold !== undefined) L.push(`            failureThreshold: ${pr.failureThreshold}`);
  }
  return L.join('\n');
}

/* ---------------- 主流程 ---------------- */
const out = [];
const say = (...a) => out.push(a.join(' '));

const files = loadServiceFiles();
say('=== D1：从 deploy/k8s/services/*.yaml 抽出的"可变字段"（这就是 values 的候选） ===');
say(['文件'.padEnd(30), '镜像tag'.padEnd(10), 'replicas'.padEnd(9), 'svcType'.padEnd(10), 'nodePort'.padEnd(9), 'env项', '注释/有效行'].join(''));
for (const f of files) {
  const tag = f.deploy.image.split(':').at(-1);
  say([
    f.file.padEnd(30), tag.padEnd(10), String(f.deploy.replicas).padEnd(9), String(f.svc.type).padEnd(10),
    String(f.svc.ports[0].nodePort ?? '-').padEnd(9), String(f.deploy.env.length).padEnd(6),
    `${f.commentLines}/${f.totalLines}`,
  ].join(''));
}

say('');
say('=== D2：values 层级覆盖（默认 < 环境 overlay < --set） ===');
const base = { image: { registry: '192.168.56.12:5000/aics', tag: 'latest' }, service: { replicas: 1, nodePort: null }, env: { JAVA_OPTS: '-Xms256m -Xmx256m', NACOS_ADDR: 'nacos:8848' } };
const prod = { image: { tag: 'v1.0.0' }, service: { replicas: 2 } };
const setOver = parseSet('service.replicas=3,env.JAVA_OPTS=-Xms512m\\ -Xmx512m,env.OPENAI_MODEL=');
const eff = deepMerge(deepMerge(base, prod), setOver.eff);
const fProd = flatten(prod);
const fSet = flatten(setOver.eff);
for (const [p, v] of Object.entries(flatten(eff))) {
  const src = fSet[p] !== undefined ? '--set 赢' : fProd[p] !== undefined ? 'values-prod.yaml 赢' : 'values.yaml 默认值';
  say(`${p.padEnd(28)} = ${JSON.stringify(v).padEnd(28)} ← ${src}`);
}
say('读法：replicas 三层都有（1 / 2 / 3），最终 3 —— --set 最外层赢；tag 只有 overlay 与默认值，overlay 赢；');
say('     env.OPENAI_MODEL 被 --set 设成 null —— 它不会"删掉这个键"，而是变成一个 null 值，模板里 {{ .Values.env.OPENAI_MODEL }} 渲染出 "<no value>" 或空，取决于写法。');

say('');
say('=== D3：--set 的类型推断与切分坑（每条给推断结果 + 后果） ===');
const cases = [
  ['--set', 'service.replicas=3', 'k8s 要 int，正好'],
  ['--set', 'service.nodePort=30080', 'k8s 要 int，正好'],
  ['--set', 'env.MYSQL_PORT=3306', 'env value 要字符串，塞进数字 → 提交被拒'],
  ['--set-string', 'env.MYSQL_PORT=3306', '强制字符串，安全'],
  ['--set', 'env.PASSWORD=0123', '前导 0 丢失，密码变 123'],
  ['--set', 'env.PASSWORD=null', '键被置空，等于删掉这个 env'],
  ['--set', 'env.JAVA_OPTS=-Xms256m -Xmx256m', '空格没转义 → shell 先切成两个参数'],
  ['--set', 'env.JAVA_OPTS=-Xms256m\\ -Xmx256m', '转义空格后是一个值'],
  ['--set', 'name=a,b', '键名里的逗号被当分隔符，多出一个 b 键'],
  ['--set', 'env.EMPTY=', '空值 → null，不是空字符串'],
];
for (const [flag, c, note] of cases) {
  let r;
  try {
    const { eff: p, types } = parseSet(c, { forceString: flag === '--set-string' });
    const vals = Object.entries(flatten(p));
    const tlist = Object.entries(types).map(([k, t]) => `${k}=${t}`);
    r = `值=${JSON.stringify(vals.map(([, v]) => v))} 类型=${tlist.join(' ')}`;
  } catch (e) { r = `解析失败: ${e.message}`; }
  say(`${(flag + ' ' + c).padEnd(40)} → ${r.padEnd(50)} // ${note}`);
}
say('读法：本模拟器把每条当成"已经过 shell 处理、原样交给 Helm 的字符串"——真实 shell 会先吃掉引号、按空格切词；');
say('     所以 env value 类一律要 --set-string 或写进 values.yaml，靠 --set 猜类型迟早出事；');

say('');
say('=== D4：渲染出的 api-gateway（注意：一条注释都不剩） ===');
const gw = files.find((f) => f.name === 'api-gateway');
const gwYaml = renderService(gw);
say(gwYaml);
say(`原文件注释行 ${gw.commentLines} 行 → 渲染产物注释行 ${gwYaml.split('\n').filter((l) => /^\s*#/.test(l)).length} 行`);

say('');
say('=== D5：等价性验收 —— 渲染回去逐字段比对（这是 helm template 的验收标准） ===');
function docsOf(text) {
  const out = [];
  let buf = [];
  for (const l of text.split(/\r?\n/)) {
    if (l.trim() === '---') { if (buf.join('').trim()) out.push(buf.join('\n')); buf = []; }
    else buf.push(l);
  }
  if (buf.join('').trim()) out.push(buf.join('\n'));
  return out.map((s) => parseYaml(s));
}
const resKey = (o) => `${o?.kind ?? '?'}/${o?.metadata?.name ?? '?'}`;
function compare(origText, renderedText) {
  const O = new Map(docsOf(origText).map((o) => [resKey(o), flatten(o)]));
  const R = new Map(docsOf(renderedText).map((o) => [resKey(o), flatten(o)]));
  const diffs = [];
  for (const [k, fo] of O) {
    if (!R.has(k)) { diffs.push([`${k} 整个资源`, '存在', '缺失', '没渲染出来']); continue; }
    const fr = R.get(k);
    for (const [p, ov] of Object.entries(fo)) {
      if (!(p in fr)) { diffs.push([`${k}.${p}`, JSON.stringify(ov), '缺失', '未渲染出来']); continue; }
      if (typeof ov !== typeof fr[p] || JSON.stringify(ov) !== JSON.stringify(fr[p])) {
        diffs.push([`${k}.${p}`, JSON.stringify(ov), JSON.stringify(fr[p]), `类型 ${typeof ov} vs ${typeof fr[p]}`]);
      }
    }
    for (const [p, rv] of Object.entries(fr)) if (!(p in fo)) diffs.push([`${k}.${p}`, '不存在', JSON.stringify(rv), '渲染多出来']);
  }
  for (const [k] of R) if (!O.has(k)) diffs.push([`${k} 整个资源`, '缺失', '多出', '多渲染了一个资源']);
  return diffs;
}
let totalQuoted = 0;
let totalNaive = 0;
for (const f of files) {
  const origText = readFileSync(join(SVC_DIR, f.file), 'utf8');
  const dq = compare(origText, renderService(f, { quoteEnv: true }));
  const dn = compare(origText, renderService(f, { quoteEnv: false }));
  totalQuoted += dq.length;
  totalNaive += dn.length;
  say(`${f.file.padEnd(30)} 带引号渲染 ${String(dq.length).padStart(3)} 处差异   不写引号渲染 ${String(dn.length).padStart(3)} 处差异`);
}
say(`合计：带引号 ${totalQuoted} 处，不带引号 ${totalNaive} 处`);
say('');
say('--- 不带引号时出现的差异样本（前 12 条） ---');
let shown = 0;
for (const f of files) {
  const origText = readFileSync(join(SVC_DIR, f.file), 'utf8');
  for (const d of compare(origText, renderService(f, { quoteEnv: false }))) {
    if (shown++ >= 12) break;
    say(`${f.file}  ${d[0]}  原文=${d[1]}  渲染=${d[2]}  (${d[3]})`);
  }
  if (shown >= 12) break;
}
say('');
say('读法：差异全落在 env 的 value 上 —— 原清单把端口/开关写成 "3306" 字符串，模板若用 {{ .Values... }} 直接插值就变成 3306 数字；');
say('     k8s 侧 env value 必须是字符串，类型不符会被 API Server 拒绝（或 client 端报 cannot unmarshal number into Go struct field）。');
say('     这就是"模板里必须写 | quote"的真实理由，也是"渲染结果必须可 review"的第一条检查项。');

say('');
say('=== D6：8 份服务清单的 schema 漂移（统一模板会强迫你在这里做决定） ===');
say(['文件'.padEnd(30), '镜像名'.padEnd(20), 'registry', 'pullPolicy', 'port.name', 'namespace'.padEnd(20), 'env字面值', 'envSecretRef'].join(' '));
for (const f of files) {
  const imgName = f.deploy.image.split('/').at(-1).split(':')[0];
  const hasReg = /:\d+\//.test(f.deploy.image) || /^[a-z0-9.-]+\.[a-z]{2,}\//.test(f.deploy.image);
  const literal = f.deploy.env.filter((e) => e.value !== undefined).length;
  const secret = f.deploy.env.filter((e) => e.valueFrom).length;
  say([
    f.file.padEnd(30), imgName.padEnd(20), (hasReg ? '有' : '无').padEnd(8),
    (f.deploy.imagePullPolicy ? '有' : '无').padEnd(10),
    (f.svc.ports[0].name ? '有' : '无').padEnd(9),
    String(f.namespace).padEnd(20), String(literal).padEnd(8), String(secret),
  ].join(' '));
}
const springStyle = files.filter((f) => f.deploy.env.some((e) => e.name.startsWith('SPRING_'))).map((f) => f.file);
const noReg = files.filter((f) => !(/:\d+\//.test(f.deploy.image) || /^[a-z0-9.-]+\.[a-z]{2,}\//.test(f.deploy.image))).map((f) => f.file);
const noPull = files.filter((f) => !f.deploy.imagePullPolicy).map((f) => f.file);
const oddNs = files.filter((f) => f.namespace !== 'ai-customer-service').map((f) => f.file);
const noPortName = files.filter((f) => !f.svc.ports[0].name).map((f) => f.file);
say('');
say(`用 SPRING_* 全名写 env 的文件：${springStyle.join(', ') || '无'}`);
say(`镜像没有私有仓库前缀的文件：${noReg.join(', ') || '无'}`);
say(`没写 imagePullPolicy 的文件：${noPull.join(', ') || '无'}`);
say(`namespace 不是 ai-customer-service 的文件：${oddNs.join(', ') || '无'}`);
say(`Service 端口没写 name 的文件：${noPortName.join(', ') || '无'}`);
say('读法：每一条都不是"模板写得好不好"，而是"参数化时必须做的决定"——');
say('     registry / pullPolicy / namespace / port.name / env 命名风格，这 5 个决定一旦进 values，');
say('     8 份清单就从"各写各的"变成"同一个 schema 的 8 组取值"，第 9 个服务接进来只要加一段 values。');

console.log(out.join('\n'));