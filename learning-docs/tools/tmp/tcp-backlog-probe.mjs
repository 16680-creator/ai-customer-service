// tcp-backlog-probe.mjs —— 演示 listen(backlog) / accept 队列的真实行为
// 用法：node tcp-backlog-probe.mjs <backlog> <clients> <blockMs>
//   backlog  传给 server.listen({ backlog }) 的值
//   clients  同时发起的连接数（由 worker 线程发起，主线程此时被阻塞，无法 accept）
//   blockMs  主线程忙等毫秒数（模拟"应用没来得及 accept"）
// 只监听 127.0.0.1，全程 < 3 秒。
import net from 'node:net';
import { Worker } from 'node:worker_threads';

const BACKLOG = Number(process.argv[2] ?? 1);
const CLIENTS = Number(process.argv[3] ?? 8);
const BLOCK_MS = Number(process.argv[4] ?? 800);
const WAIT_MS = 600; // 客户端等事件的上限

const workerSrc = `
const { parentPort, workerData } = require('node:worker_threads');
const net = require('node:net');
const { port, clients, waitMs } = workerData;

const result = [];
const socks = [];
for (let i = 0; i < clients; i++) {
  const s = net.connect({ host: '127.0.0.1', port });
  const rec = { i, state: 'pending', err: null, localPort: null };
  result.push(rec);
  s.on('connect', () => {
    rec.state = 'connected';
    rec.localPort = s.localPort;
  });
  s.on('error', (e) => {
    rec.state = 'error';
    rec.err = e.code || e.message;
  });
  socks.push(s);
}
setTimeout(() => {
  for (const s of socks) s.destroy();
  parentPort.postMessage(result);
}, waitMs);
`;

const server = net.createServer((sock) => {
  accepted++;
  sock.destroy();
});
let accepted = 0;

const t0 = Date.now();
server.listen({ host: '127.0.0.1', port: 0, backlog: BACKLOG }, () => {
  const port = server.address().port;
  const worker = new Worker(workerSrc, {
    eval: true,
    workerData: { port, clients: CLIENTS, waitMs: WAIT_MS },
  });
  // 关键：主线程同步忙等，事件循环被占住，libuv 无法调用 accept()
  const until = Date.now() + BLOCK_MS;
  while (Date.now() < until) {
    /* busy wait: 故意不 accept */
  }
  worker.on('message', (rows) => {
    const byState = rows.reduce((m, r) => ((m[r.state] = (m[r.state] || 0) + 1), m), {});
    const errCodes = rows
      .filter((r) => r.err)
      .reduce((m, r) => ((m[r.err] = (m[r.err] || 0) + 1), m), {});
    console.log(
      `backlog=${BACKLOG} clients=${CLIENTS} blockMs=${BLOCK_MS} port=${port} elapsedMs=${Date.now() - t0}`
    );
    console.log('kernel 侧结果（worker 视角）：', JSON.stringify(byState));
    console.log('拒绝的错误码分布：', JSON.stringify(errCodes));
    for (const r of rows) {
      console.log(`  client#${r.i} ${r.state}${r.err ? ' ' + r.err : ''}${r.localPort ? ' localPort=' + r.localPort : ''}`);
    }
    // 让出事件循环一拍，等 accept 回调把已排队连接取走
    setTimeout(() => {
      console.log(`应用侧 accepted=${accepted}（主线程恢复后 accept 到的连接数）`);
      server.close();
    }, 150);
  });
});
