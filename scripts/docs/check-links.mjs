#!/usr/bin/env node
// 学习文档库链接检查：校验所有 Markdown 相对链接指向的文件是否存在
// 用法: node scripts/docs/check-links.mjs [目录]   （默认 learning-docs）
import { readdir, readFile } from 'node:fs/promises';
import { existsSync, statSync } from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] ?? 'learning-docs');
if (!existsSync(root) || !statSync(root).isDirectory()) {
  console.error(`目录不存在: ${root}`);
  process.exit(2);
}

async function walk(dir) {
  const out = [];
  for (const e of await readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...(await walk(p)));
    else if (e.name.toLowerCase().endsWith('.md')) out.push(p);
  }
  return out;
}

const LINK_RE = /\[[^\]\n]*\]\(([^)\s]+)\)/g;

const files = await walk(root);
let checked = 0;
const broken = [];

for (const file of files) {
  const raw = await readFile(file, 'utf8');
  // 剔除代码块与行内代码，避免把示例文本当成链接
  const text = raw.replace(/```[\s\S]*?(?:```|$)/g, (m) => m.replace(/[^\n]/g, ' ')).replace(/`[^`\n]*`/g, (m) => m.replace(/[^\n]/g, ' '));
  let m;
  LINK_RE.lastIndex = 0;
  while ((m = LINK_RE.exec(text))) {
    const target = m[1];
    if (/^[a-z][a-z0-9+.-]*:/i.test(target) || target.startsWith('#')) continue;
    checked++;
    const [p] = target.split('#');
    let dec = p;
    try { dec = decodeURIComponent(p); } catch { /* 保留原样 */ }
    const resolved = path.resolve(path.dirname(file), dec);
    if (!existsSync(resolved)) {
      const line = text.slice(0, m.index).split('\n').length;
      broken.push({ file: path.relative(root, file), line, target });
    }
  }
}

console.log(`扫描目录: ${root}`);
console.log(`Markdown 文件: ${files.length}，相对链接: ${checked}，断链: ${broken.length}`);
for (const b of broken) console.log(`  ✗ ${b.file}:${b.line} -> ${b.target}`);
process.exit(broken.length ? 1 : 0);
