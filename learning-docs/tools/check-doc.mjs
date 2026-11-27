#!/usr/bin/env node
/*
 * check-doc.mjs —— learning-docs 通用文档校验器（适配 _templates/写作规范.md v1）
 *
 * 校验项：
 *  1. 结构完整性：非 README 的正文必须有 0/1/3/4/6/7/8/9 节（2/5 缺失给警告），
 *     并且有 `## 小结` 与 `## 自测题`；自测题里 <details> 折叠答案 >= 10 个。
 *  2. 图表规范：每个 ```mermaid 块后面 15 行内必须有「读图说明」。
 *  3. 证据有效性：
 *     - A 级锚点 `[[源码:文件.java#L起-L止]]` 的文件名必须存在于
 *       24-源码分析/assets/source/ 或 learning-docs/assets/source/；
 *     - A 级代码块 `(src: 文件#L起-L止)` 同上；
 *     - B 级引用 `ai-cs-xxx/....java|yml|xml|sql|vue|ts|js` 必须是真实存在的文件路径。
 *  4. 链接有效性：Markdown 相对链接目标必须存在（http/https/mailto/# 跳过）。
 *  5. 代码块必须有语言标注。
 *  6. 篇幅：正文 < 300 行给警告；> 2000 行给警告。
 *  7. 文件名：新增文件不应含大写字母（已存在的大写文件名只警告，不乱改）。
 *
 * 用法：
 *   node tools/check-doc.mjs                    # 校验全库（跳过 24-源码分析/_templates/tools/assets）
 *   node tools/check-doc.mjs 01-Java基础 02-Spring微服务
 *   node tools/check-doc.mjs --json 01-Java基础
 *
 * 退出码：0 = 无错误；1 = 有错误；2 = 参数/路径错误
 */
import { readdir, readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join, dirname, resolve, relative, basename, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '..');
const PROJECT_ROOT = resolve(ROOT, '..');
const SOURCE_DIRS = [
  join(ROOT, '24-源码分析', 'assets', 'source'),
  join(ROOT, 'assets', 'source'),
].filter((d) => existsSync(d));

const SKIP_DIRS = new Set([
  'node_modules', '.git', '_templates', 'tools', 'assets',
  '24-源码分析', '.source-cache', 'target', '.idea',
]);

const REQUIRED_SECTIONS = [0, 1, 3, 4, 6, 7, 8, 9];
const OPTIONAL_SECTIONS = [2, 5];
const MIN_SELFTEST = 10;
const MIN_LINES = 300;
const MAX_LINES = 2000;

const ANCHOR_RE = /\[\[源码[:：]([^\]\s#|]+)#L\d+(?:-L?\d+)?\]\]/g;
const SRC_MARK_RE = /\(src:\s*([^\s#()#]+)#L\d+(?:\s*[-–]\s*L?\d+)?\)/g;
const PROJECT_PATH_RE = /(?:^|[\s(（`'"])((?:ai-cs-[A-Za-z0-9-]+|deploy|scripts|docs)\/[A-Za-z0-9._\-/]+\.(?:java|ya?ml|xml|sql|vue|ts|js|properties|json|md|sh|ps1))(?::\d+|#L\d+(?:-L?\d+)?)?/g;
const LINK_RE = /\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;
const FENCE_OPEN_RE = /^(\s*)(`{3,}|~{3,})([^\s`]*)\s*(.*)$/;

function isMarkdown(p) {
  return p.toLowerCase().endsWith('.md');
}

async function walk(dir, out) {
  let entries;
  try {
    entries = await readdir(dir, { withFileTypes: true });
  } catch {
    return out;
  }
  for (const e of entries) {
    const full = join(dir, e.name);
    if (e.isDirectory()) {
      if (SKIP_DIRS.has(e.name)) continue;
      await walk(full, out);
    } else if (isMarkdown(e.name)) {
      out.push(full);
    }
  }
  return out;
}

/** 把文档切成「普通行」与「代码块内行」，方便跳过节内伪链接/伪锚点 */
function splitCodeFences(lines) {
  const blocks = [];
  let cur = null;
  const inCode = new Array(lines.length).fill(false);
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(FENCE_OPEN_RE);
    if (!cur && m) {
      cur = { start: i, lang: m[3] || '', info: m[4] || '', body: [] };
      inCode[i] = true;
      continue;
    }
    if (cur && /^\s*(`{3,}|~{3,})\s*$/.test(lines[i])) {
      cur.end = i;
      blocks.push(cur);
      inCode[i] = true;
      cur = null;
      continue;
    }
    if (cur) {
      inCode[i] = true;
      cur.body.push(lines[i]);
    }
  }
  if (cur) {
    cur.end = lines.length - 1;
    cur.unclosed = true;
    blocks.push(cur);
  }
  return { blocks, inCode };
}

function checkFile(file, text) {
  const errors = [];
  const warnings = [];
  const lines = text.split('\n');
  const rel = relative(ROOT, file).replaceAll('\\', '/');
  const name = basename(file);
  const isReadme = name.toLowerCase() === 'readme.md';
  const { blocks, inCode } = splitCodeFences(lines);

  // ---- 5. 代码块必须有语言标注 ----
  for (const b of blocks) {
    if (b.unclosed) {
      errors.push(`${rel}:${b.start + 1} 代码块未闭合`);
      continue;
    }
    if (!b.lang) {
      const msg = `${rel}:${b.start + 1} 代码块缺少语言标注（写 \`\`\`java / \`\`\`text 等）`;
      if (isReadme) warnings.push(msg);
      else errors.push(msg);
    }
  }

  // ---- 4. 相对链接（跳过代码块内文本） ----
  for (let i = 0; i < lines.length; i++) {
    if (inCode[i]) continue;
    LINK_RE.lastIndex = 0;
    let m;
    while ((m = LINK_RE.exec(lines[i])) !== null) {
      const target = m[1];
      if (/^(https?:|mailto:|tel:|#|data:)/i.test(target)) continue;
      if (target.startsWith('<')) continue;
      const clean = decodeURIComponent(target.split('#')[0].split('?')[0]);
      if (!clean) continue;
      const abs = resolve(dirname(file), clean);
      if (!existsSync(abs)) {
        errors.push(`${rel}:${i + 1} 相对链接失效 → ${target}`);
      }
    }
  }

  // ---- 3a. A 级锚点 ----
  for (let i = 0; i < lines.length; i++) {
    ANCHOR_RE.lastIndex = 0;
    let m;
    while ((m = ANCHOR_RE.exec(lines[i])) !== null) {
      const src = m[1];
      if (!SOURCE_DIRS.some((d) => existsSync(join(d, src)))) {
        errors.push(`${rel}:${i + 1} 源码锚点文件不存在于 assets/source → ${src}`);
      }
    }
  }

  // ---- 3b. 代码块 src 标注 ----
  for (const b of blocks) {
    SRC_MARK_RE.lastIndex = 0;
    const head = `${lines[b.start]} ${b.info}`;
    let m;
    while ((m = SRC_MARK_RE.exec(head)) !== null) {
      const src = m[1];
      if (!SOURCE_DIRS.some((d) => existsSync(join(d, src)))) {
        errors.push(`${rel}:${b.start + 1} 代码块 src 指向的文件不存在 → ${src}`);
      }
    }
  }

  // ---- 3c. B 级：本项目源码路径引用 ----
  for (let i = 0; i < lines.length; i++) {
    PROJECT_PATH_RE.lastIndex = 0;
    let m;
    while ((m = PROJECT_PATH_RE.exec(lines[i])) !== null) {
      const p = m[1];
      const abs = join(PROJECT_ROOT, p);
      if (!existsSync(abs)) {
        errors.push(`${rel}:${i + 1} 项目路径不存在 → ${p}`);
      }
    }
  }

  if (!isReadme) {
    // ---- 1. 12 节结构 ----
    const found = new Map();
    for (let i = 0; i < lines.length; i++) {
      if (inCode[i]) continue;
      const m = lines[i].match(/^##\s*(\d+)\s*[.、．]/);
      if (m) {
        const n = Number(m[1]);
        if (!found.has(n)) found.set(n, i + 1);
      }
    }
    for (const n of REQUIRED_SECTIONS) {
      if (!found.has(n)) errors.push(`${rel} 缺第 ${n} 节（"## ${n}. ..."）`);
    }
    for (const n of OPTIONAL_SECTIONS) {
      if (!found.has(n)) warnings.push(`${rel} 缺第 ${n} 节（"## ${n}. ..."），若刻意合并请在文首说明`);
    }

    // ---- 小结 / 自测题 ----
    const idxSummary = lines.findIndex((l, i) => !inCode[i] && /^##\s*小结/.test(l));
    const idxQuiz = lines.findIndex((l, i) => !inCode[i] && /^##\s*自测题/.test(l));
    if (idxSummary < 0) errors.push(`${rel} 缺 "## 小结"`);
    if (idxQuiz < 0) {
      errors.push(`${rel} 缺 "## 自测题"`);
    } else {
      const tail = lines.slice(idxQuiz).join('\n');
      const count = (tail.match(/<details>/g) || []).length;
      if (count < MIN_SELFTEST) {
        errors.push(`${rel} 自测题折叠答案只有 ${count} 个，要求 >= ${MIN_SELFTEST}`);
      }
    }
    if (idxSummary >= 0 && idxQuiz >= 0 && idxSummary > idxQuiz) {
      warnings.push(`${rel} "## 小结" 出现在 "## 自测题" 之后，建议放在前面`);
    }

    // ---- 2. Mermaid 读图说明 ----
    for (const b of blocks) {
      if (!/mermaid/i.test(b.lang)) continue;
      const from = b.end + 1;
      const to = Math.min(lines.length, from + 15);
      const tail = lines.slice(from, to).join('\n');
      if (!/读图说明/.test(tail)) {
        warnings.push(`${rel}:${b.start + 1} Mermaid 图后 15 行内没有「读图说明」`);
      }
    }

    // ---- 6. 篇幅 ----
    if (lines.length < MIN_LINES) {
      warnings.push(`${rel} 只有 ${lines.length} 行（< ${MIN_LINES}），确认是否讲透`);
    }
    if (lines.length > MAX_LINES) {
      warnings.push(`${rel} 有 ${lines.length} 行（> ${MAX_LINES}），考虑拆分`);
    }
  }

  // ---- 7. 文件名 ----
  if (/[A-Z]/.test(name)) {
    warnings.push(`${rel} 文件名含大写字母（已存在文件可保留，新增文件请用小写或中文）`);
  }

  return { rel, errors, warnings };
}

async function main() {
  const argv = process.argv.slice(2);
  const json = argv.includes('--json');
  const targets = argv.filter((a) => !a.startsWith('--'));

  if (targets.length === 0) {
    const all = await walk(ROOT, []);
    return report(await Promise.all(all.map(async (f) => checkFile(f, await readFile(f, 'utf8')))), json);
  }

  const files = [];
  for (const t of targets) {
    const abs = resolve(ROOT, t);
    if (!existsSync(abs)) {
      console.error(`✗ 路径不存在：${t}`);
      process.exit(2);
    }
    const list = await walk(abs, []);
    if (list.length === 0) {
      console.error(`✗ 目录下没有 Markdown：${t}`);
      process.exit(2);
    }
    files.push(...list);
  }
  const uniq = [...new Set(files)];
  return report(await Promise.all(uniq.map(async (f) => checkFile(f, await readFile(f, 'utf8')))), json);
}

function report(results, json) {
  const errors = results.flatMap((r) => r.errors);
  const warnings = results.flatMap((r) => r.warnings);

  if (json) {
    console.log(JSON.stringify({
      files: results.length,
      errors: errors.length,
      warnings: warnings.length,
      details: { errors, warnings },
    }, null, 2));
    process.exit(errors.length ? 1 : 0);
  }

  for (const r of results) {
    if (r.errors.length === 0 && r.warnings.length === 0) continue;
    console.log(`\n── ${r.rel}`);
    for (const e of r.errors) console.log(`  ✗ ${e}`);
    for (const w of r.warnings) console.log(`  ! ${w}`);
  }

  console.log('');
  console.log(`files=${results.length} errors=${errors.length} warnings=${warnings.length}`);
  process.exit(errors.length ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(2);
});
