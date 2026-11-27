// learning-docs/15-计算机基础/07-字符编码与时区.md 的 C 级实测脚本（node，无第三方依赖）。
// 用法：node learning-docs\tools\tmp\ch07-tz-intl.mjs
// 作用：打印本机 node 的时区来源，并演示「同一时刻 → 不同本地日期」「LocalDateTime 字符串排序 ≠ 绝对时刻排序」
//      「按不同时区归日导致统计口径不一致」三件事。
const fmtIn = (tz, instant) =>
  new Intl.DateTimeFormat('sv-SE', {
    timeZone: tz,
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(instant);

console.log('[1] 本机 node 的时区来源');
console.log('    Intl.DateTimeFormat().resolvedOptions().timeZone =', Intl.DateTimeFormat().resolvedOptions().timeZone);
console.log('    process.env.TZ                                   =', process.env.TZ);
console.log('    process.versions.icu                             =', process.versions.icu);
console.log('    new Date().toString()                            =', new Date().toString());

console.log('[2] 同一时刻在四个时区的本地读法');
const instant = new Date('2026-11-27T17:30:00Z');
for (const tz of ['Asia/Shanghai', 'UTC', 'America/New_York', 'Europe/Berlin']) {
  console.log(`    ${tz.padEnd(18)} → ${fmtIn(tz, instant)}`);
}

console.log('[3] 「同一天」的定义随口径变化：按 UTC 归日 vs 按东八区归日');
const samples = [
  '2026-11-27T15:59:00Z',
  '2026-11-27T16:30:00Z',
  '2026-11-27T23:59:00Z',
  '2026-11-28T00:30:00Z',
];
const bucket = (tzName) => {
  const m = new Map();
  for (const s of samples) {
    const day = fmtIn(tzName, new Date(s)).slice(0, 10);
    m.set(day, (m.get(day) ?? 0) + 1);
  }
  return [...m.entries()].sort();
};
console.log('    按 UTC 归日      =', JSON.stringify(bucket('UTC')));
console.log('    按 Asia/Shanghai =', JSON.stringify(bucket('Asia/Shanghai')));

console.log('[4] LocalDateTime 字符串排序 ≠ 绝对时刻排序（两条记录来自不同时区写入方）');
const records = [
  { id: 'A', local: '2026-11-27T09:00', tz: 'Asia/Shanghai' },
  { id: 'B', local: '2026-11-27T08:00', tz: 'America/New_York' },
];
const byLocal = [...records].sort((x, y) => x.local.localeCompare(y.local)).map((r) => r.id);
const withInstant = records.map((r) => {
  // 把「本地时间 + 该写入方的时区」还原成绝对时刻：用 Intl 求该时区的偏移，再算 UTC 毫秒
  const probe = new Date(`${r.local}:00Z`);
  const localAtProbe = fmtIn(r.tz, probe); // 同一毫秒在该时区的读法
  const offsetMs = Date.parse(`${localAtProbe.replace(' ', 'T')}:00Z`) - probe.getTime();
  return { id: r.id, instant: new Date(Date.parse(`${r.local}:00Z`) - offsetMs) };
});
const byInstant = [...withInstant].sort((x, y) => x.instant - y.instant).map((r) => r.id);
for (const r of withInstant) console.log(`    ${r.id}: local=${records.find((x) => x.id === r.id).local} (${records.find((x) => x.id === r.id).tz}) → ${r.instant.toISOString()}`);
console.log('    按 local 字符串排序 =', byLocal.join(' , '));
console.log('    按绝对时刻排序     =', byInstant.join(' , '));
console.log('    两种口径是否一致   =', byLocal.join() === byInstant.join() ? '一致' : '不一致（排序结果被时区翻转）');
