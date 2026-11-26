#!/usr/bin/env node
/*
 * fetch-source.mjs —— 抓取"指定版本的真源码"并生成可校验摘录入库文件。
 *
 * 为什么需要它：本目录要求所有代码摘录都能对回真实源码（行号一致），
 * 所以先机械抓取原文 -> 入库 assets/source/xxx.md -> 正文用
 *   [[源码:xxx.java#L120-L168]]          锚点引用
 *   ```java (src: xxx.java#L120-L168)    可校验代码块
 * 再由 tools/check-doc.mjs 做一致性校验。
 *
 * 两种来源（--from auto 默认：先 maven，失败再 github）：
 *   1) maven  —— 从 Maven Central 下载 <artifact>-<version>-sources.jar 并解压到本地缓存。
 *                不受 GitHub 限流影响，推荐。
 *        node tools/fetch-source.mjs --from maven \
 *          --gav org.springframework:spring-beans:7.1.0 \
 *          --path org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java \
 *          --out assets/source/AbstractAutowireCapableBeanFactory.java.md
 *        （--path 可省略：会用文件名在 jar 中唯一匹配）
 *   2) github —— GitHub Contents API（匿名 60 次/小时，可设 GITHUB_TOKEN）
 *        node tools/fetch-source.mjs --from github --repo spring-projects/spring-cloud \
 *          --tag v5.1.0 --path ... --out assets/source/xxx.java.md
 *
 * 其它选项：
 *   --name <显示名>     正文锚点使用的文件名（默认取 --path 的文件名）
 *   --jar-cache <dir>   jar 缓存目录（默认 docs/learning-docs/.source-cache，已 gitignore）
 *   --lines 120-260     只入库该区间（大文件建议只摘相关区间，但会限制锚点范围，慎用）
 */
import { mkdir, writeFile, readFile, access } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { dirname, join, resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { inflateRawSync } from 'node:zlib';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '..');
const DEFAULT_CACHE = join(ROOT, '.source-cache');

const MAVEN_BASES = [
  'https://repo1.maven.org/maven2',
  'https://maven.aliyun.com/repository/public',
];

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i++) {
    const k = argv[i];
    if (!k.startsWith('--')) continue;
    const key = k.slice(2);
    const val = argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[++i] : 'true';
    args[key] = val;
  }
  return args;
}

function bail(msg) {
  console.error(`[fetch-source] ERROR: ${msg}`);
  process.exit(1);
}

function splitLines(text) {
  const nl = text.includes('\r\n') ? '\r\n' : '\n';
  const lines = text.split(nl);
  if (lines.length && lines[lines.length - 1] === '') lines.pop();
  return lines;
}

/* ------------------------------- 极简 ZIP 读取 ------------------------------ */

async function readZipEntries(buf) {
  // 从尾部找 End Of Central Directory (0x06054b50)
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557 - 22); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('不是有效的 zip（找不到 EOCD）');
  const count = buf.readUInt16LE(eocd + 10);
  let off = buf.readUInt32LE(eocd + 16);
  const entries = new Map();
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(off) !== 0x02014b50) throw new Error('中央目录损坏');
    const method = buf.readUInt16LE(off + 10);
    const crc = buf.readUInt32LE(off + 16);
    const compSize = buf.readUInt32LE(off + 20);
    const size = buf.readUInt32LE(off + 24);
    const nameLen = buf.readUInt16LE(off + 28);
    const extraLen = buf.readUInt16LE(off + 30);
    const commentLen = buf.readUInt16LE(off + 32);
    const localOff = buf.readUInt32LE(off + 42);
    const name = buf.toString('utf8', off + 46, off + 46 + nameLen);
    entries.set(name, { method, crc, compSize, size, localOff });
    off += 46 + nameLen + extraLen + commentLen;
  }

  const read = (name) => {
    const e = entries.get(name);
    if (!e) return null;
    const lNameLen = buf.readUInt16LE(e.localOff + 26);
    const lExtraLen = buf.readUInt16LE(e.localOff + 28);
    const start = e.localOff + 30 + lNameLen + lExtraLen;
    const data = buf.subarray(start, start + e.compSize);
    const raw = e.method === 0 ? data : inflateRawSync(data);
    if (raw.length !== e.size) throw new Error(`解压长度不符：${name}`);
    return raw;
  };

  return { names: [...entries.keys()], read };
}

async function downloadTo(url, dest) {
  const res = await fetch(url, { redirect: 'follow' });
  if (!res.ok) throw new Error(`HTTP ${res.status} ${url}`);
  const ab = await res.arrayBuffer();
  await mkdir(dirname(dest), { recursive: true });
  await writeFile(dest, Buffer.from(ab));
  return dest.length;
}

async function getSourcesJar(gav, cacheDir) {
  const parts = gav.split(':');
  if (parts.length !== 3) bail(`--gav 需要 groupId:artifactId:version，收到 ${gav}`);
  const [g, a, v] = parts;
  const dir = join(cacheDir, ...g.split('.'), a, v);
  const jar = join(dir, `${a}-${v}-sources.jar`);
  if (existsSync(jar)) return jar;
  let lastErr;
  for (const base of MAVEN_BASES) {
    const url = `${base}/${g.replace(/\./g, '/')}/${a}/${v}/${a}-${v}-sources.jar`;
    try {
      await downloadTo(url, jar);
      console.error(`[fetch-source] downloaded ${url}`);
      return jar;
    } catch (e) {
      lastErr = e;
    }
  }
  bail(`sources jar 下载失败（${gav}）：${lastErr?.message}`);
}

async function fromMaven({ gav, path, cacheDir }) {
  const jar = await getSourcesJar(gav, cacheDir);
  const zip = await readZipEntries(await readFile(jar));
  const javaFiles = zip.names.filter((n) => n.endsWith('.java') || n.endsWith('.properties') || n.endsWith('.factories') || n.endsWith('.imports'));
  let entry = path;
  if (entry) {
    if (!zip.names.includes(entry)) bail(`jar 中不存在 ${entry}；同名候选：${javaFiles.filter((n) => n.endsWith('/' + basename(entry))).join(', ') || '无'}`);
  } else {
    bail('--from maven 需要 --path（jar 内相对路径）');
  }
  const text = zip.read(entry).toString('utf8');
  const [g, a, v] = gav.split(':');
  return { lines: splitLines(text), ref: `${g}:${a}:${v}`, htmlUrl: `${MAVEN_BASES[0]}/${g.replace(/\./g, '/')}/${a}/${v}/${a}-${v}-sources.jar` };
}

/* --------------------------------- GitHub --------------------------------- */

async function fromGithub({ repo, tag, path }) {
  const url = `https://api.github.com/repos/${repo}/contents/${path}?ref=${encodeURIComponent(tag)}`;
  const headers = { Accept: 'application/vnd.github+json', 'User-Agent': 'assetcentre-learning-docs' };
  if (process.env.GITHUB_TOKEN) headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
  const res = await fetch(url, { headers });
  if (!res.ok) bail(`GitHub HTTP ${res.status}（匿名限流 60 次/小时，建议改用 --from maven）：${repo}@${tag}:${path}`);
  const json = await res.json();
  if (Array.isArray(json)) bail(`路径是目录：${path}`);
  const text = Buffer.from(json.content.replace(/\s/g, ''), 'base64').toString('utf8');
  return { lines: splitLines(text), ref: `${repo}@${tag}`, htmlUrl: json.html_url };
}

/* --------------------------------- 渲染 ---------------------------------- */

function renderSourceMarkdown({ origin, path, name, lines, htmlUrl }) {
  const display = name || basename(path);
  return [
    '<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->',
    `<!-- origin: ${origin} | path: ${path} -->`,
    `<!-- 正文引用：[[源码:${display}#L起-L止]] ；代码块头部：(src: ${display}#L起-L止) -->`,
    '',
    `# ${display}`,
    '',
    '| 项 | 值 |',
    '| --- | --- |',
    `| 来源 | \`${origin}\` |`,
    `| 路径 | \`${path}\` |`,
    `| 行数 | ${lines.length} |`,
    `| 在线地址 | ${htmlUrl} |`,
    `| 抓取时间 | ${new Date().toISOString()} |`,
    '',
    '```java',
    ...lines.map((l, i) => `${String(i + 1).padStart(5, ' ')} | ${l}`),
    '```',
    '',
  ].join('\n');
}

/* --------------------------------- main ---------------------------------- */

async function main() {
  const a = parseArgs(process.argv);
  if (!a.out) bail('缺少 --out <assets/source/xxx.md>');
  const cacheDir = a['jar-cache'] || DEFAULT_CACHE;
  const from = a.from || 'auto';

  let result;
  if (from === 'maven' || (from === 'auto' && a.gav)) {
    if (!a.gav) bail('--from maven 需要 --gav');
    result = await fromMaven({ gav: a.gav, path: a.path, cacheDir });
  } else {
    if (!a.repo || !a.tag || !a.path) bail('--from github 需要 --repo --tag --path');
    result = await fromGithub({ repo: a.repo, tag: a.tag, path: a.path });
  }

  const origin = a.gav ? result.ref.split(':').join(':') : result.ref;
  const md = renderSourceMarkdown({ origin: a.gav ? origin : result.ref, path: a.path, name: a.name, lines: result.lines, htmlUrl: result.htmlUrl });
  await mkdir(dirname(a.out), { recursive: true });
  await writeFile(a.out, md, 'utf8');
  console.log(`[fetch-source] OK ${result.ref} ${a.path} -> ${a.out} (${result.lines.length} lines)`);
}

await main();
