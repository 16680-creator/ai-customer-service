// learning-docs/15-计算机基础/07-字符编码与时区.md 的 C 级实测脚本（node，无第三方依赖）。
// 用法：node learning-docs\tools\tmp\ch07-bom-scan.mjs
// 作用：1) 打印示例字符的码点与 UTF-8 字节，验证"变长编码"；
//       2) 扫描仓库内文件是否带 UTF-8 BOM（EF BB BF），统计 .ps1 与 .md 的分布。
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

console.log('[1] 示例字符：码点 → UTF-8 字节');
for (const ch of ['A', '订', '单', '😊']) {
  const cp = [...ch][0].codePointAt(0);
  const hex = [...Buffer.from(ch, 'utf8')].map((b) => b.toString(16).toUpperCase().padStart(2, '0')).join(' ');
  console.log(`    ${ch}  U+${cp.toString(16).toUpperCase().padEnd(6)} utf8 = ${hex.padEnd(12)} 字节数 = ${Buffer.from(ch, 'utf8').length}`);
}
console.log('    "订单😊" 的 js length（UTF-16 码元数）=', '订单😊'.length,
  '，按码点展开后 =', [...'订单😊'].length);
console.log('    BOM 的 UTF-8 字节 =', [...Buffer.from('\uFEFF', 'utf8')].map((b) => b.toString(16).toUpperCase().padStart(2, '0')).join(' '));

console.log('[2] 仓库内 UTF-8 BOM 分布（跳过 .git / node_modules / target / .venv / .idea）');
const SKIP = /^(\.git|node_modules|target|\.venv|\.idea)$/;
const EXT = /\.(md|yml|yaml|json|java|sql|ps1)$/;
let seen = 0;
const bom = [];
const ps1 = [];
const walk = (dir) => {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) {
      if (!SKIP.test(e.name)) walk(p);
      continue;
    }
    if (!EXT.test(e.name)) continue;
    seen++;
    const buf = readFileSync(p);
    const has = buf.length > 2 && buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf;
    if (has) bom.push(p.replaceAll('\\', '/'));
    if (/\.ps1$/.test(e.name)) ps1.push([p.replaceAll('\\', '/'), has]);
  }
};
walk('.');
console.log(`    扫描文件数 = ${seen}，带 BOM = ${bom.length}，其中 .md = ${bom.filter((x) => x.endsWith('.md')).length}`);
console.log(`    .ps1 共 ${ps1.length} 个，带 BOM = ${ps1.filter((x) => x[1]).length} 个`);
for (const [p, has] of ps1) console.log(`      ${has ? 'BOM ' : 'noBOM'}  ${p}`);
