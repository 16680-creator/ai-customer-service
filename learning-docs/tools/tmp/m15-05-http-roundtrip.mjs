// 15-计算机基础 / 05-HTTP与TLS —— C 级实测脚本 A：连接复用到底省什么、省多少
//
// 干什么：
//   1) 在本机 127.0.0.1 的临时端口起一个 node http 服务，暴露四条路由：
//        /ping        立即返回 JSON，但不设 Content-Length（看 node 退回 chunked）
//        /ping-fixed  立即返回同样的 JSON，显式设 Content-Length（看定长帧）
//        /slow        固定延迟 1000ms 后返回（模拟"首 token 慢"的模型服务）
//        /stream      不设 Content-Length，分 5 帧各隔 100ms 写出（模拟 SSE/chunked）
//   2) 用同一个 node 进程当客户端，分两组发请求：
//        冷连接组：每个请求新建 TCP 连接（agent: false）
//        复用组  ：keepAlive agent，多请求共用一条连接
//   3) 打印真实的响应头、耗时、以及"服务端一共 accept 了几条连接"
//
// 为什么这么设计：
//   - 只有服务端 accept 计数能证明"复用"真的发生；光看耗时会被 loopback 噪声骗过去。
//   - 只有把定长/分帧两条路由都打出来，才能看到 Content-Length 与 Transfer-Encoding: chunked
//     在真实响应头里的区别。
//
// 边界：
//   - 只绑 127.0.0.1 的临时端口，不监听外部网卡、不联网、不连任何中间件，跑完自动退出。
//   - 本机 loopback 的"建立连接"成本远低于真实跨机房 RTT，所以本脚本的数字只能用来
//     证明"谁在省什么"，不能当线上延迟引用。跨网段的 RTT 账用 m15-05-tls-flights.mjs 算。
//
// 运行：node .\m15-05-http-roundtrip.mjs

import http from 'node:http';

const SLOW_MS = 1000;
const FRAMES = 5;
const FRAME_GAP_MS = 100;

let connCount = 0;            // 服务端 accept 到的 TCP 连接总数
const reuseMark = new Set();  // 记录每条连接上跑了几个请求

const server = http.createServer((req, res) => {
  const url = req.url;

  if (url === '/ping') {
    const body = JSON.stringify({ ok: true, route: '/ping' });
    // 故意不设 Content-Length：看服务端只能退化成 Transfer-Encoding: chunked
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
    });
    res.end(body);
    return;
  }

  if (url === '/ping-fixed') {
    const body = JSON.stringify({ ok: true, route: '/ping-fixed' });
    // 显式声明长度：同一次 end 写出，帧头里就是 Content-Length，没有分块
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Content-Length': Buffer.byteLength(body),
      'Cache-Control': 'no-store',
    });
    res.end(body);
    return;
  }

  if (url === '/slow') {
    setTimeout(() => {
      const body = JSON.stringify({ ok: true, route: '/slow', delayMs: SLOW_MS });
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store',
      });
      res.end(body);
    }, SLOW_MS);
    return;
  }

  if (url === '/stream') {
    // 不写 Content-Length，先 flush 头 -> node 自动改用 chunked 编码
    res.writeHead(200, {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    });
    let i = 0;
    const timer = setInterval(() => {
      i += 1;
      res.write(`event: token\ndata: {"i":${i}}\n\n`);
      if (i >= FRAMES) {
        clearInterval(timer);
        res.end();
      }
    }, FRAME_GAP_MS);
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('not found');
});

server.on('connection', (socket) => {
  connCount += 1;
  const id = connCount;
  reuseMark.add(id);
  socket.on('close', () => reuseMark.delete(id));
});

function pickHeaders(h) {
  const keys = ['content-type', 'content-length', 'transfer-encoding', 'connection',
    'keep-alive', 'cache-control', 'date', 'etag'];
  const out = {};
  for (const k of keys) if (h[k] !== undefined) out[k] = h[k];
  return out;
}

function once(port, path, agentOpt) {
  return new Promise((resolve, reject) => {
    const t0 = process.hrtime.bigint();
    const req = http.request(
      { host: '127.0.0.1', port, path, method: 'GET', agent: agentOpt },
      (res) => {
        let bytes = 0;
        const chunks = [];
        res.on('data', (c) => { bytes += c.length; chunks.push(c); });
        res.on('end', () => {
          const t1 = process.hrtime.bigint();
          resolve({
            status: res.statusCode,
            headers: pickHeaders(res.headers),
            bytes,
            ms: Number(t1 - t0) / 1e6,
            localPort: req.socket.localPort,
            bodyHead: Buffer.concat(chunks).toString('utf8').slice(0, 60).replace(/\n/g, '\\n'),
          });
        });
      },
    );
    req.on('error', reject);
    req.end();
  });
}

const fmt = (x, d = 2) => x.toFixed(d);
const avg = (xs) => xs.reduce((a, b) => a + b, 0) / xs.length;

await new Promise((r) => server.listen(0, '127.0.0.1', r));
const port = server.address().port;
console.log(`[server] listening 127.0.0.1:${port}  (loopback only)`);

// ---------- 观测 1：定长 vs 分帧 vs 流式，真实响应头长什么样 ----------
console.log('\n=== 观测 1a：/ping（立刻返回，没设 Content-Length） ===');
const pingCold = await once(port, '/ping', false);
console.log(`status=${pingCold.status} ms=${fmt(pingCold.ms)} bytes=${pingCold.bytes} socket.localPort=${pingCold.localPort}`);
console.log(`headers=${JSON.stringify(pingCold.headers)}`);
console.log(`body=${pingCold.bodyHead}`);

console.log('\n=== 观测 1b：/ping-fixed（立刻返回，显式 Content-Length） ===');
const pingFixed = await once(port, '/ping-fixed', false);
console.log(`status=${pingFixed.status} ms=${fmt(pingFixed.ms)} bytes=${pingFixed.bytes} socket.localPort=${pingFixed.localPort}`);
console.log(`headers=${JSON.stringify(pingFixed.headers)}`);
console.log(`body=${pingFixed.bodyHead}`);

console.log('\n=== 观测 1c：/stream（5 帧 × 100ms，无 Content-Length） ===');
const streamCold = await once(port, '/stream', false);
console.log(`status=${streamCold.status} ms=${fmt(streamCold.ms)} bytes=${streamCold.bytes} socket.localPort=${streamCold.localPort}`);
console.log(`headers=${JSON.stringify(streamCold.headers)}`);
console.log(`body=head ${streamCold.bodyHead}`);

// ---------- 观测 2：慢接口的耗时就是服务端处理时间，连接方式改不了它 ----------
console.log(`\n=== 观测 2：/slow（固定 ${SLOW_MS}ms），冷连接 3 次 ===`);
const slowTimes = [];
for (let i = 0; i < 3; i++) {
  const r = await once(port, '/slow', false);
  slowTimes.push(r.ms);
  console.log(`#${i + 1} ms=${fmt(r.ms)} bytes=${r.bytes} localPort=${r.localPort}`);
}
console.log(`平均=${fmt(avg(slowTimes))}ms  （≈ 服务端 ${SLOW_MS}ms + 极小的 loopback 往返）`);

// ---------- 观测 3：冷连接 vs 复用连接，用服务端 accept 计数作证 ----------
console.log('\n=== 观测 3：20 次 /ping，冷连接（每请求新建 TCP） ===');
connCount = 0;
const coldTimes = [];
for (let i = 0; i < 20; i++) {
  const r = await once(port, '/ping', false);
  coldTimes.push(r.ms);
}
const coldConns = connCount;
console.log(`20 个请求 -> 服务端 accept ${coldConns} 条 TCP 连接；耗时 平均=${fmt(avg(coldTimes))}ms 最小=${fmt(Math.min(...coldTimes))}ms 最大=${fmt(Math.max(...coldTimes))}ms`);

console.log('\n=== 观测 3：20 次 /ping，复用连接（keepAlive agent） ===');
connCount = 0;
const agent = new http.Agent({ keepAlive: true, maxSockets: 1, maxFreeSockets: 1 });
const warmTimes = [];
const portsSeen = new Set();
let warmHeaders = null;
for (let i = 0; i < 20; i++) {
  const r = await once(port, '/ping', agent);
  warmTimes.push(r.ms);
  portsSeen.add(r.localPort);
  if (i === 0) warmHeaders = r.headers;
}
const warmConns = connCount;
agent.destroy();
console.log(`首个请求的响应头=${JSON.stringify(warmHeaders)}   <- 注意 connection 与冷连接不一样`);
console.log(`20 个请求 -> 服务端 accept ${warmConns} 条 TCP 连接；客户端只用了 ${portsSeen.size} 个本地端口 [${[...portsSeen].join(', ')}]`);
console.log(`耗时 平均=${fmt(avg(warmTimes))}ms 最小=${fmt(Math.min(...warmTimes))}ms 最大=${fmt(Math.max(...warmTimes))}ms`);
console.log(`省下的绝对时间 = ${fmt(avg(coldTimes) - avg(warmTimes), 3)}ms/请求（loopback 建立连接本来就便宜，所以这个差值很小）`);

// ---------- 观测 4：同一条连接上的请求是"排队"的 ----------
console.log('\n=== 观测 4：一条 keepAlive 连接上并发 3 个 /slow 请求 ===');
connCount = 0;
const t0 = process.hrtime.bigint();
const agent2 = new http.Agent({ keepAlive: true, maxSockets: 1, maxFreeSockets: 1 });
await Promise.all([once(port, '/slow', agent2), once(port, '/slow', agent2), once(port, '/slow', agent2)]);
const t1 = process.hrtime.bigint();
agent2.destroy();
console.log(`3 个 ${SLOW_MS}ms 请求、单连接串行 -> 墙钟 ${fmt(Number(t1 - t0) / 1e6)}ms；accept ${connCount} 条连接`);

console.log('\n=== 观测 4：三条独立连接上并发同样 3 个 /slow 请求 ===');
connCount = 0;
const t2 = process.hrtime.bigint();
await Promise.all([once(port, '/slow', false), once(port, '/slow', false), once(port, '/slow', false)]);
const t3 = process.hrtime.bigint();
console.log(`3 个 ${SLOW_MS}ms 请求、三连接并行 -> 墙钟 ${fmt(Number(t3 - t2) / 1e6)}ms；accept ${connCount} 条连接`);

server.close();
console.log('\n[done] server closed');
