// list-jar.mjs —— 列出 Maven sources jar 内的类路径（供 agent 找到要 fetch 的 path）
//
// 用法：
//   node tools/list-jar.mjs org.springframework:spring-aop:7.1.0 org/springframework/aop/framework/
//   node tools/list-jar.mjs org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0 ".java$"
//
// 说明：sources.jar 复用 .source-cache 缓存（与 fetch-source.mjs 同目录），第二次起离线可用。
// 输出每行一个 jar 内路径，按字母序。
import { createWriteStream, existsSync, mkdirSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '..');
const CACHE = join(ROOT, '.source-cache');

const MIRRORS = ['https://repo1.maven.org/maven2', 'https://maven.aliyun.com/repository/public'];

const [gav, pattern] = process.argv.slice(2);
if (!gav) {
  console.error('用法: node tools/list-jar.mjs group:artifact:version [正则或路径片段]');
  process.exit(2);
}
const [group, artifact, version] = gav.split(':');

async function downloadJar() {
  const dest = join(CACHE, ...group.split('.'), artifact, version, `${artifact}-${version}-sources.jar`);
  if (existsSync(dest)) return dest;
  mkdirSync(dirname(dest), { recursive: true });
  const rel = `${group.replace(/\./g, '/')}/${artifact}/${version}/${artifact}-${version}-sources.jar`;
  for (const m of MIRRORS) {
    try {
      const res = await fetch(`${m}/${rel}`);
      if (!res.ok) continue;
      await pipeline(Readable.fromWeb(res.body), createWriteStream(dest));
      return dest;
    } catch {
      /* 试下一个镜像 */
    }
  }
  throw new Error(`sources jar 下载失败: ${gav}`);
}

/** 极简 ZIP 中央目录解析（只取文件名列表） */
function listEntries(buf) {
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 66000); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('不是合法 ZIP');
  const count = buf.readUInt16LE(eocd + 10);
  let off = buf.readUInt32LE(eocd + 16);
  const names = [];
  for (let i = 0; i < count; i++) {
    if (buf.readUInt32LE(off) !== 0x02014b50) break;
    const nameLen = buf.readUInt16LE(off + 28);
    const extraLen = buf.readUInt16LE(off + 30);
    const commentLen = buf.readUInt16LE(off + 32);
    names.push(buf.toString('utf8', off + 46, off + 46 + nameLen));
    off += 46 + nameLen + extraLen + commentLen;
  }
  return names;
}

const jar = await downloadJar();
const buf = await readFile(jar);
let names = listEntries(buf);
if (pattern) {
  let re;
  try { re = new RegExp(pattern); } catch { re = new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')); }
  names = names.filter((n) => re.test(n));
}
names.sort();
console.log(names.join('\n'));
console.error(`[list-jar] ${gav} 共 ${names.length} 个匹配条目`);
