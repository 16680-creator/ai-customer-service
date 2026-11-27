// 15-计算机基础 / 05-HTTP与TLS —— C 级实测脚本 B：队头阻塞与多路复用的真实差别
//
// 干什么：
//   用同一台机器、同一个"1000ms 才回"的处理函数，分别走两条路：
//     A) HTTP/1.1 + keepAlive：一条 TCP 连接上并发发 3 个请求
//     B) HTTP/2（h2c 明文，node 内置 node:http2）：一条 TCP 连接上并发开 3 个 stream
//   打印两次的真实墙钟时间、服务端 accept 的连接数、以及响应头差异。
//
// 为什么这么设计：
//   - HTTP/1.1 的一条连接上，请求是"顺序"的：前一个响应没写完，后一个响应就不能开始写
//     （协议层面叫"应用层队头阻塞"）。所以 3 个 1s 请求 = 3s 左右。
//   - HTTP/2 在同一条连接上开多条 stream，帧之间可以交错，所以同样是 1 条连接、同样是 3 个
//     1s 请求，墙钟只应该是 1s 左右。
//   - accept 计数必须打印：否则读者无法判断"是不是偷偷开了 3 条连接"。
//
// 边界：
//   - 只绑 127.0.0.1 临时端口，不联网。h2c 是明文 HTTP/2（不需要证书），好让实验不依赖 TLS。
//   - 本机 loopback 没有真实 RTT，所以绝对时间只能看"3s vs 1s"这个量级对比；
//     真实链路上还要叠加 RTT 与带宽，账见 m15-05-tls-flights.mjs。
//
// 运行：node .\m15-05-h2-multiplex.mjs

import http from 'node:http';
import http2 from 'node:http2';

const SLOW_MS = 1000;
const N = 3;

const handler = (delayMs) => (_req, res) => {
  setTimeout(() => {
    const body = JSON.stringify({ ok: true, delayMs });
    if (typeof res.writeHead === 'function') {
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(body),
      });
    }
    res.end(body);
  }, delayMs);
};

// ---------- A：HTTP/1.1 单连接并发 ----------
let h1Conns = 0;
const h1 = http.createServer(handler(SLOW_MS));
h1.on('connection', () => { h1Conns += 1; });
await new Promise((r) => h1.listen(0, '127.0.0.1', r));
const h1Port = h1.address().port;

const agent = new http.Agent({ keepAlive: true, maxSockets: 1, maxFreeSockets: 1 });
const h1Once = () => new Promise((resolve, reject) => {
  const req = http.request({ host: '127.0.0.1', port: h1Port, path: '/slow', agent }, (res) => {
    res.resume();
    res.on('end', () => resolve({ status: res.statusCode, proto: `HTTP/${res.httpVersion}`, headers: res.headers }));
  });
  req.on('error', reject);
  req.end();
});

let t = process.hrtime.bigint();
const h1Results = await Promise.all([h1Once(), h1Once(), h1Once()]);
let h1Ms = Number(process.hrtime.bigint() - t) / 1e6;
agent.destroy();
h1.close();

console.log('=== A）HTTP/1.1，keepAlive，1 条连接上并发 3 个 1000ms 请求 ===');
console.log(`墙钟=${h1Ms.toFixed(2)}ms  服务端 accept=${h1Conns} 条 TCP 连接`);
console.log(`首个响应 proto=${h1Results[0].proto} headers=${JSON.stringify(h1Results[0].headers)}`);

// ---------- B：HTTP/2（h2c）单连接并发 stream ----------
let h2Conns = 0;
const h2 = http2.createServer();
h2.on('connection', () => { h2Conns += 1; });
h2.on('stream', (stream, headers) => {
  setTimeout(() => {
    const body = JSON.stringify({ ok: true, delayMs: SLOW_MS, path: headers[':path'] });
    stream.respond({
      ':status': 200,
      'content-type': 'application/json; charset=utf-8',
      'content-length': Buffer.byteLength(body),
    });
    stream.end(body);
  }, SLOW_MS);
});
await new Promise((r) => h2.listen(0, '127.0.0.1', r));
const h2Port = h2.address().port;

const client = http2.connect(`http://127.0.0.1:${h2Port}`);
const h2Once = () => new Promise((resolve, reject) => {
  const req = client.request({ ':path': '/slow' });
  let status = null;
  let hdrs = null;
  req.on('response', (h) => { status = h[':status']; hdrs = h; });
  req.resume();
  req.on('end', () => resolve({ status, headers: hdrs }));
  req.on('error', reject);
});

t = process.hrtime.bigint();
const h2Results = await Promise.all([h2Once(), h2Once(), h2Once()]);
const h2Ms = Number(process.hrtime.bigint() - t) / 1e6;
client.close();
h2.close();

console.log('\n=== B）HTTP/2（h2c 明文），1 条连接上并发 3 个 stream，每个 1000ms ===');
console.log(`墙钟=${h2Ms.toFixed(2)}ms  服务端 accept=${h2Conns} 条 TCP 连接`);
const clean = { ...h2Results[0].headers };
console.log(`首个响应 :status=${h2Results[0].status} headers=${JSON.stringify(clean)}`);

console.log(`\n=== 对比 ===`);
console.log(`HTTP/1.1 单连接串行 = ${h1Ms.toFixed(0)}ms`);
console.log(`HTTP/2   单连接多路 = ${h2Ms.toFixed(0)}ms`);
console.log(`单连接上"3 个慢请求"被串行化多付的代价 = ${(h1Ms - h2Ms).toFixed(0)}ms（本机 loopback，无额外 RTT）`);
console.log('[done]');
