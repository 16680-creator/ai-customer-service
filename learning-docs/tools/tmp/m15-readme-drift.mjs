// learning-docs/tools/tmp/m15-readme-drift.mjs
// 用途：给 15-计算机基础/README.md 第 4 节「字符编码：行号漂移实测」取证。
//       对同一份 UTF-8 文件，对比三种口径的行数：
//         1) 真实行数（按 0x0A 数）        —— node / UTF-8 读取看到的
//         2) 宽松双字节解码后的可见行数      —— PowerShell 5.1 用 ANSI(CP936) 读 UTF-8 时看到的
//         3) 风险行数（行尾字节落在 0x81~0xFE 的行） —— 这些行的换行符会被当成双字节字符的尾字节吞掉
// 跑法：node learning-docs\tools\tmp\m15-readme-drift.mjs <文件1> [文件2 ...]
import { readFileSync } from 'node:fs';

const files = process.argv.slice(2);
if (files.length === 0) {
  console.error('用法: node m15-readme-drift.mjs <文件...>');
  process.exit(2);
}

/** 宽松双字节解码（模拟 CP936 遇到 0x81~0xFE 就吃两个字节，不校验尾字节） */
function lenientDecode(buf) {
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
  return { visibleLf, swallowed };
}

console.log('文件'.padEnd(46) + '真实行数  风险行数  宽松解码行数  被吞换行  漂移');
console.log('-'.repeat(92));
for (const f of files) {
  const buf = readFileSync(f);
  const realLf = buf.reduce((n, b) => n + (b === 0x0a ? 1 : 0), 0);
  const realLines = realLf + 1;

  // 逐行扫：统计「行尾最后一个字节落在 0x81~0xFE」的行数（这些行的 0x0A 会被吞）
  let risky = 0;
  let start = 0;
  for (let i = 0; i < buf.length; i++) {
    if (buf[i] === 0x0a) {
      if (i > start) {
        const last = buf[i - 1];
        if (last !== 0x0d && last >= 0x81 && last <= 0xfe) risky++;
      }
      start = i + 1;
    }
  }

  const { visibleLf, swallowed } = lenientDecode(buf);
  const drift = realLines - (visibleLf + 1);
  console.log(
    f.padEnd(46) +
      String(realLines).padStart(6) +
      String(risky).padStart(10) +
      String(visibleLf + 1).padStart(14) +
      String(swallowed).padStart(10) +
      String(drift).padStart(8)
  );
}
console.log('-'.repeat(92));
console.log('读法：真实行数与宽松解码行数之差 = 你看到的行号会往上漂多少行；');
console.log('      风险行数 = 行尾是多字节字符（UTF-8 末字节落在 0x81~0xFE）的行，它们制造了吞换行。');
