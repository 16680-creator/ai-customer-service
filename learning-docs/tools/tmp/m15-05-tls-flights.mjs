// 15-计算机基础 / 05-HTTP与TLS —— C 级实测脚本 D：握手往返数的账（纯计算模型）
//
// 干什么：把"一次请求要花几个 RTT"这件事写成可复算的算式，输入只有两个：
//   RTT（毫秒）与请求个数 N。输出是不同连接/协议策略下的总耗时账。
//
// 为什么用模型而不是实测：
//   本机 loopback 的 RTT 只有零点几毫秒，1-RTT 与 2-RTT 的差别会被调度噪声淹没
//   （见 m15-05-tls-handshake.mjs 的耗时列，全是个位数毫秒）。而"握手要几个往返"是
//   协议规定的事实，不是测量值，写清楚假设就能算准。所以这里只算，不谎称实测。
//
// 假设（都是公开协议事实，不是本项目实测）：
//   TCP 三次握手            = 1 RTT
//   TLS 1.2 完整握手        = 2 RTT（ClientHello/ServerHello+Certificate/Finished）
//   TLS 1.3 完整握手        = 1 RTT（ServerHello 之后服务端就能发加密扩展）
//   TLS 1.2 会话恢复        = 1 RTT
//   TLS 1.3 会话恢复(PSK)   = 1 RTT；0-RTT 早数据可把首请求的应用数据塞进第一趟
//   HTTP/1.1 请求-响应      = 1 RTT（不含服务端处理时间 S）
//   服务端处理时间 S        = 0（只看传输账，把计算时间单独加）
//
// 运行：node .\m15-05-tls-flights.mjs

const RTT_LIST = [0.5, 20, 50, 150];   // ms：本机 loopback / 同城 / 跨城 / 跨国
const N = 10;                          // 连续请求个数

const strategies = [
  { name: '每次新连接 + TLS1.2 完整握手', flightsPerRequest: () => 1 + 2 + 1, warmup: 0 },
  { name: '每次新连接 + TLS1.3 完整握手', flightsPerRequest: () => 1 + 1 + 1, warmup: 0 },
  { name: 'keep-alive 复用 + 仅首请求握手(1.2)', flightsPerRequest: () => 1, warmup: 1 + 2 + 1 },
  { name: 'keep-alive 复用 + 仅首请求握手(1.3)', flightsPerRequest: () => 1, warmup: 1 + 1 + 1 },
  { name: 'keep-alive 复用 + TLS1.3 0-RTT 早数据', flightsPerRequest: () => 1, warmup: 1, note: '首请求与握手同趟发出' },
];

function pad(s, n) { return String(s).padEnd(n); }
function padL(s, n) { return String(s).padStart(n); }

console.log(`模型参数：N=${N} 个连续请求，服务端处理时间记为 0，只算网络往返`);
console.log(`（RTT 一列依次是：本机 loopback / 同城 / 跨城 / 跨国，均为假设值，非实测）\n`);

for (const RTT of RTT_LIST) {
  console.log(`--- 假设 RTT = ${RTT} ms ---`);
  console.log(`${pad('策略', 40)}${padL('总往返数', 10)}${padL('总耗时(ms)', 12)}${padL('相对最优', 10)}`);
  const rows = strategies.map((s) => {
    const flights = s.warmup + s.flightsPerRequest() * N;
    const ms = flights * RTT;
    return { name: s.name, flights, ms };
  });
  const best = Math.min(...rows.map((r) => r.ms));
  for (const r of rows) {
    console.log(`${pad(r.name, 40)}${padL(r.flights, 10)}${padL(r.ms.toFixed(1), 12)}${padL((r.ms / best).toFixed(2) + 'x', 10)}`);
  }
  console.log();
}

// 关键结论 1：握手只付一次，就把"N 次握手"摊薄成"1 次"
console.log('=== 结论 1：keep-alive 省下的是"每请求 N 次握手"这件事 ===');
for (const RTT of RTT_LIST) {
  const noReuse = (1 + 2 + 1) * N * RTT;          // 每请求都重做 TCP+TLS1.2
  const reuse = (1 + 2 + 1) * RTT + N * 1 * RTT;  // 只首请求握手
  console.log(`RTT=${padL(RTT, 5)}ms  不复用=${padL(noReuse.toFixed(1), 9)}ms  复用=${padL(reuse.toFixed(1), 8)}ms  省 ${(noReuse - reuse).toFixed(1)}ms`);
}

// 关键结论 2：TLS1.2 -> 1.3 省的是"那次握手里的 1 个往返"，与 RTT 成正比
console.log('\n=== 结论 2：TLS1.2 -> 1.3 只省 1 个往返，但每次新建连接都要省一次 ===');
for (const RTT of RTT_LIST) {
  const perConn = RTT * 1;
  console.log(`RTT=${padL(RTT, 5)}ms  每次新建连接省 ${perConn.toFixed(1)}ms；${N} 次新建连接共省 ${(perConn * N).toFixed(1)}ms`);
}

// 关键结论 3：握手成本与"慢请求"无关——慢请求的墙钟由服务端处理时间决定
console.log('\n=== 结论 3：一个固定耗时 S 的接口，握手只能占很小一部分 ===');
const S = 1000;
for (const RTT of RTT_LIST) {
  const handshake = (1 + 2 + 1) * RTT;
  console.log(`RTT=${padL(RTT, 5)}ms  S=${S}ms  单次请求墙钟≈${(S + handshake + RTT).toFixed(1)}ms  其中握手占 ${((handshake / (S + handshake + RTT)) * 100).toFixed(2)}%`);
}

console.log('\n=== 结论 4：流水线式（HTTP/2 多路复用）把 N 个慢请求压到 1 个服务端处理时长 ===');
console.log('HTTP/1.1 单连接串行 3×1000ms 的实测见 m15-05-h2-multiplex.mjs（本机实测 3058ms vs 1191ms）；');
console.log('理论上多路复用只多付 1 个 RTT，而不是 3 个 RTT 的排队。');
