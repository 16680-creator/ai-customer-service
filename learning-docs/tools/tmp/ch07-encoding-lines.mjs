// learning-docs/15-计算机基础/07-字符编码与时区.md 的 C 级实测脚本（node，无第三方依赖）。
// 用法：node learning-docs\tools\tmp\ch07-encoding-lines.mjs <文件路径>
// 作用：对同一份文件，分别用 UTF-8 与 GBK 解码，打印字节数/换行数/字符数/首行，
//       并统计「0x0A 被当作 GBK 双字节字符尾字节吃掉」的字节对数，解释行号漂移的由来。
import { readFileSync } from 'node:fs';

const file = process.argv[2];
if (!file) {
  console.error('用法: node ch07-encoding-lines.mjs <文件路径>');
  process.exit(2);
}

const buf = readFileSync(file);
let lf = 0;
let cr = 0;
for (const b of buf) {
  if (b === 0x0a) lf++;
  if (b === 0x0d) cr++;
}

const dec = (enc) => new TextDecoder(enc).decode(buf);
const countLines = (s) => s.split('\n').length; // 段数 = 换行数 + 1
const firstLine = (s) => s.split('\n')[0].slice(0, 48);

console.log(`文件            = ${file}`);
console.log(`字节数          = ${buf.length}`);
console.log(`字节层 0x0A 数  = ${lf}`);
console.log(`字节层 0x0D 数  = ${cr}`);

for (const enc of ['utf-8', 'gbk']) {
  const s = dec(enc);
  console.log(`[${enc}] 段数 = ${countLines(s)}，字符数 = ${s.length}`);
  console.log(`[${enc}] 首行 = ${firstLine(s)}`);
}

// GBK 前导字节范围 0x81-0xFE；若它后面紧跟 0x0A，宽松解码器（如 .NET CP936）会把 0x0A 当作尾字节吞掉。
let adjacent = 0;
for (let i = 0; i < buf.length - 1; i++) {
  if (buf[i] >= 0x81 && buf[i] <= 0xfe && buf[i + 1] === 0x0a) adjacent++;
}
console.log(`GBK 前导字节紧跟 0x0A 的字节对数（上界）= ${adjacent}`);

// 模拟「宽松双字节解码器」：见 0x81-0xFE 就吃两个字节，不校验尾字节是否合法。
let visibleLf = 0;
let swallowed = 0;
for (let i = 0; i < buf.length; ) {
  const b = buf[i];
  if (b >= 0x81 && b <= 0xfe && i + 1 < buf.length) {
    if (buf[i + 1] === 0x0a) swallowed++;
    i += 2;
  } else {
    if (b === 0x0a) visibleLf++;
    i += 1;
  }
}
console.log(`宽松双字节模拟解码：可见换行 = ${visibleLf}，被吞换行 = ${swallowed}`);
