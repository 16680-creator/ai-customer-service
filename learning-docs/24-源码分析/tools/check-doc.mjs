#!/usr/bin/env node
/*
 * check-doc.mjs —— learning-docs 文档规范校验器（多 agent 产出的统一质检门槛）
 *
 * 校验项：
 *  1. 锚点有效性：正文 [[源码:文件名#L起-L止]] 必须能在 assets/source/ 中找到同名入库摘录，
 *     且行号区间必须落在摘录范围内、且该区间每一行都真实存在（防止"编造源码"与"行号漂移"）。
 *  2. 摘录一致性：正文 ```java 代码块标注 (src: 文件名#L起-L止) 时：
 *     - 区间必须合法（起 >= 1、起 <= 止、不越界、区间内原文不缺行）；
 *     - 代码块首行必须逐字符等于原文 L起（不做任何"就近纠偏"）；
 *     - 无省略号时，代码块行数必须等于 止-起+1，且逐行一致；
 *     - 有省略号时，省略号必须是**单独一行**，且整行只能是 `...`/`…`/`⋯`（可用 // 或块注释包裹）；
 *       省略号前后的内容必须在原文中连续匹配，且**最后一行必须落在声明的 L止 上**，
 *       即声明的结束行就是真正被引用的最后一行。
 *  3. 相对链接：Markdown 相对链接指向的文件必须存在（忽略 http/https/mailto/#）。
 *     代码块内部的文本不参与链接检查（示例代码里的 `[x](y)` 不是链接）。
 *  4. 规范检查：文件名 kebab-case；非 README 必须有「小结」「自测题」章节。
 *
 * 用法：
 *   node tools/check-doc.mjs                       # 校验全目录
 *   node tools/check-doc.mjs 01-Spring框架 02-SpringBoot   # 校验多个目录（全部生效，自动去重）
 *   node tools/check-doc.mjs --json                # 机器可读输出
 *
 * 退出码：0 = 无错误；1 = 有校验错误；2 = 参数/路径错误（不会给出"部分通过"的假象）
 */
import { readdir, readFile, stat } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, dirname, resolve, relative, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const DEFAULT_ROOT = resolve(HERE, '..');
const ROOT = DEFAULT_ROOT;
const SOURCE_DIR = join(DEFAULT_ROOT, 'assets', 'source');

const ANCHOR_RE = /\[\[源码:([^\]\s#|]+)#L(-?\d+)(?:-L?(-?\d+))?\]\]/g;
const LINK_RE = /\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;
/** 代码块起始行：三个及以上反引号或波浪号，后面可带语言与 (src: ...) 标注 */
const FENCE_OPEN_RE = /^(\s*)(`{3,}|~{3,})(.*)$/;
/** 代码块标注 (src: 文件#L起-L止)；也接受"来源/源码" */
const SRC_MARK_RE = /(?:src|来源|源码)\s*[:：]\s*([^\s#()（）]+)#L(-?\d+)\s*[-–~]\s*L?(-?\d+)/i;
/** 出现了 src 一类关键字但格式不可解析（例如 #L-1-L2） */
const SRC_HINT_RE = /(?:src|来源|源码)\s*[:：]/i;
/** 合法的"省略一行"：`...` / `…` / `⋯`，允许用行注释或块注释包裹，不允许跟别的内容 */
const MARKER_LINE_RE = /^\s*(?:(?:\/\/|\/\*)\s*(?:\.\.\.|…|⋯)\s*(?:\*\/)?|(?:\.\.\.|…|⋯))\s*$/;

// assets/source 是机器生成的"原文摘录库"，不参与正文规范检查（它本来就没有小结/自测题）
const PSEUDO_ANCHOR_NAMES = new Set(['X.java', 'x.java', 'example.java', 'YOUR_FILE.java', '<文件名>', '文件名']);

const SKIP_DIRS = new Set(['node_modules', '.git', '_templates', 'assets', '.source-cache']);

async function walk(dir, out = []) {
  for (const e of await readdir(dir, { withFileTypes: true })) {
    if (e.name.startsWith('.') || SKIP_DIRS.has(e.name)) continue;
    const p = join(dir, e.name);
    if (e.isDirectory()) await walk(p, out);
    else if (e.name.endsWith('.md')) out.push(p);
  }
  return out;
}

/** 解析 assets/source/*.md，返回 Map(fileName -> Map(行号 -> 文本)) */
async function loadSources(sourceDir = SOURCE_DIR) {
  const map = new Map();
  if (!existsSync(sourceDir)) return map;
  const entries = [];
  for (const e of await readdir(sourceDir, { withFileTypes: true })) {
    if (!e.isFile() || !e.name.endsWith('.md')) continue;
    const text = await readFile(join(sourceDir, e.name), 'utf8');
    const lines = new Map();
    for (const raw of text.split(/\r?\n/)) {
      const m = /^\s*(\d{1,6}) \| ?(.*)$/.exec(raw);
      if (m) lines.set(Number(m[1]), m[2]);
    }
    const disp = /^#\s+(.+?)\s*$/m.exec(text)?.[1]?.trim();
    const key = e.name.replace(/\.md$/, '');
    map.set(key, lines);
    entries.push({ key, disp, lines });
  }
  // 展示名（如 META-INF/spring/*.imports 这类同一个资源路径被多个 jar 各抓一份）只在全局唯一时才作为别名，
  // 否则会互相覆盖，导致"同名不同内容"的静默错配。歧义时必须用文件名引用。
  const dispCount = new Map();
  for (const { disp } of entries) {
    if (disp) dispCount.set(disp, (dispCount.get(disp) ?? 0) + 1);
  }
  for (const { key, disp, lines } of entries) {
    if (disp && dispCount.get(disp) === 1 && disp !== key) map.set(disp, lines);
  }
  return map;
}

/** 把 Markdown 切成"代码块之外的行" + "代码块列表"，一次扫描确定 fence 归属 */
function scanFences(text) {
  const lines = text.split(/\r?\n/);
  const blocks = [];
  const outside = [];
  let i = 0;
  while (i < lines.length) {
    const open = FENCE_OPEN_RE.exec(lines[i]);
    if (!open) {
      outside.push(lines[i]);
      i++;
      continue;
    }
    const indent = open[1];
    const fence = open[2];
    const info = open[3] ?? '';
    const ch = fence[0];
    const len = fence.length;
    const closeRe = new RegExp(`^\\s*\\${ch}{${len},}\\s*$`);
    const body = [];
    let j = i + 1;
    let closed = false;
    while (j < lines.length) {
      if (closeRe.test(lines[j])) {
        closed = true;
        j++;
        break;
      }
      body.push(lines[j]);
      j++;
    }
    blocks.push({ line: i + 1, indent, info, closed, body });
    // 代码块内容不进入 outside：链接与锚点都不检查这里面的文本
    i = j;
  }
  return { lines, blocks, outside };
}

/** 逐行核对一个片段；返回首个不一致原因 */
function verifySegment(src, segLines, pos) {
  for (let t = 0; t < segLines.length; t++) {
    const want = segLines[t].replace(/\s+$/, '');
    const got = src.get(pos + t);
    if (got === undefined) return { ok: false, why: `L${pos + t} 超出原文范围` };
    if (got.replace(/\s+$/, '') !== want) {
      return { ok: false, why: `@L${pos + t}\n      原文: ${got}\n      正文: ${want}` };
    }
  }
  return { ok: true };
}

/** 按省略号行切段 */
function splitSegments(bodyEff) {
  const segs = [[]];
  for (const l of bodyEff) {
    if (MARKER_LINE_RE.test(l)) segs.push([]);
    else segs[segs.length - 1].push(l);
  }
  return segs;
}

/**
 * 校验一个 Markdown 文本。
 * @returns {{file:string, errors:string[], warns:string[]}}
 */
export function checkMarkdown(text, options = {}) {
  const {
    file = resolve('chapter.md'),
    root = DEFAULT_ROOT,
    sources = new Map(),
    linkExists = (p) => existsSync(p),
  } = options;

  const errors = [];
  const warns = [];
  const relPath = relative(root, file).replace(/\\/g, '/');
  const { blocks, outside } = scanFences(text);
  const outsideText = outside.join('\n');

  // 1) 锚点
  for (const m of outsideText.matchAll(ANCHOR_RE)) {
    const [, srcName, s, e] = m;
    const src = sources.get(srcName) ?? sources.get(basename(srcName));
    if (!src) {
      const hint = PSEUDO_ANCHOR_NAMES.has(srcName) ? '（疑似示例/占位符被误写成锚点）' : '';
      errors.push(`${relPath}: 锚点 [[源码:${srcName}]] 在 assets/source 中没有入库摘录${hint}`);
      continue;
    }
    const start = Number(s);
    const end = e ? Number(e) : start;
    if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 1) {
      errors.push(`${relPath}: 锚点 [[源码:${srcName}#L${s}-L${e}]] 行号非法`);
      continue;
    }
    if (start > end) errors.push(`${relPath}: 锚点行号倒置 L${start}-L${end}`);
    const max = Math.max(...src.keys());
    const min = Math.min(...src.keys());
    if (end > max || start < min) {
      errors.push(`${relPath}: 锚点 [[源码:${srcName}#L${start}-L${end}]] 超出摘录范围（L${min}-L${max}）`);
      continue;
    }
    const missing = [];
    for (let n = start; n <= end; n++) if (!src.has(n)) missing.push(n);
    if (missing.length) {
      errors.push(`${relPath}: 锚点 [[源码:${srcName}#L${start}-L${end}]] 区间内原文缺失 ${missing.length} 行`);
    }
  }

  // 2) 代码块摘录一致性
  for (const block of blocks) {
    if (!block.closed) {
      errors.push(`${relPath}:${block.line} 代码块没有闭合`);
      continue;
    }
    const lang = (block.info.trim().split(/\s+/)[0] ?? '').toLowerCase();
    const isJava = lang === 'java';
    const bodyEff = block.body;

    if (!SRC_HINT_RE.test(block.info)) {
      if (isJava && bodyEff.length > 8) {
        warns.push(`${relPath}:${block.line} 超过 8 行的 java 代码块未标注 (src: 文件名#Lx-Ly)，无法追溯原文`);
      }
      continue;
    }

    const mark = SRC_MARK_RE.exec(block.info);
    if (!mark) {
      // 例如 ```text (src: 应用代码示例，非 Spring 源码)：这是"说明性标注"，不是可溯源的源码引用
      warns.push(`${relPath}:${block.line} 代码块 src 标注不是可解析的 (src: 文件名.java#L起-L止)，已跳过溯源校验`);
      continue;
    }
    const [, srcName, s, e] = mark;
    const src = sources.get(srcName) ?? sources.get(basename(srcName));
    if (!src) {
      errors.push(`${relPath}:${block.line} 代码块声明来源于 ${srcName}，但该文件未入库`);
      continue;
    }
    const start = Number(s);
    const end = Number(e);
    if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 1 || end < 1) {
      errors.push(`${relPath}:${block.line} 声明的行号非法 L${s}-L${e}`);
      continue;
    }
    if (start > end) {
      errors.push(`${relPath}:${block.line} 声明区间倒置 L${start}-L${end}`);
      continue;
    }
    const max = Math.max(...src.keys());
    const min = Math.min(...src.keys());
    if (end > max || start < min) {
      errors.push(`${relPath}:${block.line} 声明区间 L${start}-L${end} 超出原文范围（L${min}-L${max}）`);
      continue;
    }
    const missing = [];
    for (let n = start; n <= end; n++) if (!src.has(n)) missing.push(n);
    if (missing.length) {
      errors.push(
        `${relPath}:${block.line} 声明区间 L${start}-L${end} 内原文缺失 ${missing.length} 行（含被省略号跳过的行，也必须真实存在）`,
      );
      continue;
    }
    if (!bodyEff.length) {
      errors.push(`${relPath}:${block.line} 代码块为空，无法核对 L${start}-L${end}`);
      continue;
    }

    const segs = splitSegments(bodyEff);
    if (!segs[0].length) {
      errors.push(`${relPath}:${block.line} 代码块不能以省略号开头（首行必须是原文 L${start}）`);
      continue;
    }
    if (segs.slice(1).some((seg) => !seg.length)) {
      errors.push(`${relPath}:${block.line} 省略号后面缺少续接内容`);
      continue;
    }

    // 首段：必须从声明的 L起 开始逐行一致
    const r0 = verifySegment(src, segs[0], start);
    if (!r0.ok) {
      errors.push(`${relPath}:${block.line} 首行/首段与原文 L${start} 不一致 ${r0.why}`);
      continue;
    }
    let prevEnd = start + segs[0].length - 1;
    let broken = false;

    // 中间段：向后贪心找连续匹配（前后至少跳 1 行，否则省略号无意义）
    for (let si = 1; si < segs.length - 1 && segs.length > 2; si++) {
      const seg = segs[si];
      let found = -1;
      for (let c = prevEnd + 2; c <= max - seg.length + 1; c++) {
        if (verifySegment(src, seg, c).ok) {
          found = c;
          break;
        }
      }
      if (found < 0) {
        errors.push(`${relPath}:${block.line} 第 ${si} 个省略号之后的续接内容在原文中找不到连续匹配`);
        broken = true;
        break;
      }
      prevEnd = found + seg.length - 1;
    }
    if (broken) continue;

    // 末段：最后一行必须正好落在声明的 L止
    const last = segs[segs.length - 1];
    const pos = end - last.length + 1;
    if (segs.length > 1 && pos <= prevEnd + 1) {
      errors.push(
        `${relPath}:${block.line} 末段起始 L${pos} 与上一段结束 L${prevEnd} 不衔接（声明 L${start}-L${end}）`,
      );
      continue;
    }
    const rl = verifySegment(src, last, pos);
    if (!rl.ok) {
      errors.push(`${relPath}:${block.line} 末段未落在声明的 L${end} 上 ${rl.why}`);
    }
  }

  // 3) 相对链接（只看代码块之外）
  for (const m of outsideText.matchAll(LINK_RE)) {
    const target = m[1];
    if (/^(https?:|mailto:|#)/.test(target)) continue;
    const clean = decodeURIComponent(target.split('#')[0]);
    if (!clean) continue;
    if (!linkExists(resolve(dirname(file), clean))) {
      errors.push(`${relPath}: 链接失效 -> ${target}`);
    }
  }

  // 4) 规范
  const name = basename(file);
  if (name !== 'README.md' && !/^[a-z0-9\-\u4e00-\u9fa5]+\.md$/.test(name)) {
    warns.push(`${relPath}: 文件名不符合 kebab-case 规范`);
  }
  if (!relPath.includes('00-导读') && name !== 'README.md') {
    if (!/##\s*.*小结/.test(text)) warns.push(`${relPath}: 缺少「小结」章节`);
    if (!/##\s*.*自测题/.test(text)) warns.push(`${relPath}: 缺少「自测题」章节`);
  }
  return { file: relPath, errors, warns };
}

/**
 * 执行一次校验（供 CLI 与测试共用）。
 * @returns {Promise<{report:{files:number,errors:number,warns:number,results:Array}, exitCode:number}>}
 */
export async function runCheck(argv = process.argv.slice(2), options = {}) {
  const root = options.root ?? DEFAULT_ROOT;
  const sourceDir = options.sourceDir ?? join(root, 'assets', 'source');
  const out = options.out ?? ((s) => console.log(s));
  const jsonOut = argv.includes('--json');
  const unknown = argv.filter((a) => a.startsWith('--') && a !== '--json');
  let scopes = argv.filter((a) => !a.startsWith('--'));

  const fail = (message) => {
    const report = { files: 0, errors: 1, warns: 0, results: [{ file: '', errors: [message], warns: [] }] };
    if (jsonOut) out(JSON.stringify(report, null, 2));
    else out(`[check-doc] ${message}`);
    return { report, exitCode: 2 };
  };

  if (unknown.length) return fail(`未知参数: ${unknown.join(' ')}（只支持 --json）`);
  if (!scopes.length) scopes = ['.'];

  const files = new Set();
  for (const scope of scopes) {
    if (scope === '') return fail('scope 不能为空字符串');
    const target = resolve(root, scope);
    if (!existsSync(target)) return fail(`路径不存在: ${scope}`);
    const info = await stat(target);
    if (info.isDirectory()) {
      for (const f of await walk(target)) files.add(resolve(f));
    } else {
      if (!target.endsWith('.md')) return fail(`只校验 Markdown 文件: ${scope}`);
      files.add(target);
    }
  }

  const sources = await loadSources(sourceDir);
  const results = [];
  for (const f of [...files].sort()) {
    const text = await readFile(f, 'utf8');
    results.push(checkMarkdown(text, { file: f, root, sources }));
  }

  const errCount = results.reduce((n, r) => n + r.errors.length, 0);
  const warnCount = results.reduce((n, r) => n + r.warns.length, 0);
  const report = { files: results.length, errors: errCount, warns: warnCount, results };

  if (jsonOut) {
    out(JSON.stringify(report, null, 2));
  } else {
    for (const r of results) {
      if (!r.errors.length && !r.warns.length) continue;
      out(`\n=== ${r.file} ===`);
      for (const e of r.errors) out(`  [ERROR] ${e}`);
      for (const w of r.warns) out(`  [warn ] ${w}`);
    }
    out(`\n[check-doc] 文件 ${results.length} 个，错误 ${errCount}，警告 ${warnCount}`);
  }
  return { report, exitCode: errCount > 0 ? 1 : 0 };
}

const invokedDirectly = process.argv[1] && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url));
if (invokedDirectly) {
  const { exitCode } = await runCheck();
  process.exit(exitCode);
}
