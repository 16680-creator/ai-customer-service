// 本机取证脚本 2：为「先看分布、再看平均」提供真实样本
// 干什么：对同一个真实文件重复 200 次读+解析，记录每次耗时，输出 mean/p50/p95/p99/max
// 为什么用文件 IO：不联网、不起服务、30 秒内跑完，且天然带"冷热"两段与 OS 抖动
import { readFileSync } from 'node:fs';

const FILE = 'D:/Projects/Persion/ai-customer-service/tools/logs/ai-cs-order.out.log';
const N = 200;

const raw = [];
for (let i = 0; i < N; i++) {
  const t0 = process.hrtime.bigint();
  const text = readFileSync(FILE, 'utf8');
  // 模拟真实分析动作：按行切分并统计 WARN/ERROR
  let warn = 0, err = 0;
  for (const line of text.split('\n')) {
    if (line.includes(' WARN ')) warn++;
    else if (line.includes(' ERROR ')) err++;
  }
  const t1 = process.hrtime.bigint();
  raw.push(Number(t1 - t0) / 1e6);
  if (i === N - 1) console.log(`file=${FILE.split('/').pop()} bytes=${text.length} warn=${warn} error=${err}`);
}

const mean = raw.reduce((a, b) => a + b, 0) / raw.length;
const sorted = [...raw].sort((a, b) => a - b);
const q = (p) => sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];

console.log(`n=${N}  mean=${mean.toFixed(2)}ms  p50=${q(50).toFixed(2)}ms  p95=${q(95).toFixed(2)}ms  p99=${q(99).toFixed(2)}ms  max=${sorted[sorted.length - 1].toFixed(2)}ms`);
console.log(`first5=[${raw.slice(0, 5).map((x) => x.toFixed(2)).join(', ')}]`);
console.log(`last5 =[${raw.slice(-5).map((x) => x.toFixed(2)).join(', ')}]`);
console.log(`p95/p50=${(q(95) / q(50)).toFixed(2)}x  max/p50=${(sorted[sorted.length - 1] / q(50)).toFixed(2)}x`);
