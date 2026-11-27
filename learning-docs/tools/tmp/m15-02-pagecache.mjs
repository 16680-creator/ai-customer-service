// learning-docs/tools/tmp/m15-02-pagecache.mjs
// 目的：演示「文件 IO 快是因为页缓存」，以及「写文件不等于数据落盘」。
// 做法：造一个 128MB 文件 → 冷读一次、热读一次，各自计时；全程不调用 fsync。
// 说明：数字只代表本机这一刻，不能当基准（没有清 OS 缓存的能力，第二次必然是热的）。
import { writeFileSync, readFileSync, openSync, writeSync, closeSync, statSync, unlinkSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import os from 'node:os';

const HERE = dirname(fileURLToPath(import.meta.url));
const FILE = join(HERE, 'm15-02-pagecache.bin');
const SIZE_MB = 128;

const mb = (n) => (n / 1024 / 1024).toFixed(1);
const snap = () => {
  const m = process.memoryUsage();
  return `rss=${mb(m.rss)}MB heapUsed=${mb(m.heapUsed)}MB external=${mb(m.external)}MB`;
};

console.log('0. before   ' + snap());

let t = Date.now();
{
  const fd = openSync(FILE, 'w');
  const buf = Buffer.alloc(1024 * 1024);
  for (let i = 0; i < SIZE_MB; i++) {
    buf[i & 1023] = i & 255;
    writeSync(fd, buf);
  }
  closeSync(fd);
}
const writeMs = Date.now() - t;
console.log(`1. write ${SIZE_MB}MB  writeMs=${writeMs}  size=${mb(statSync(FILE).size)}MB  ${snap()}`);
console.log('   -> 写完立刻返回，数据只在页缓存里；writeSync 不保证落盘，落盘要 fsync');

t = Date.now();
const a = readFileSync(FILE);
const read1 = Date.now() - t;
console.log(`2. read#1   readMs=${read1} bytes=${a.length}  ${snap()}`);

t = Date.now();
const b = readFileSync(FILE);
const read2 = Date.now() - t;
console.log(`3. read#2   readMs=${read2} bytes=${b.length}  ${snap()}`);
console.log(`   -> 第二次读比第一次快 ${(read1 / Math.max(read2, 1)).toFixed(1)}x（同进程、缓存未清，只能看方向不看绝对量）`);

t = Date.now();
const fd = openSync(FILE, 'r');
closeSync(fd);
console.log(`4. open+close again ${Date.now() - t}ms  ${snap()}`);

console.log(`5. pageSize_report=${'see PowerShell [System.Environment]::SystemPageSize'} os.freemem=${mb(os.freemem())}MB os.totalmem=${mb(os.totalmem())}MB`);

unlinkSync(FILE);
console.log('6. cleaned up ' + (existsSync(FILE) ? 'FAILED' : 'ok') + '  ' + snap());
