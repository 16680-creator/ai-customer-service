// m15-03-lt-et.mjs
// 目的：用最小模型演示 LT（水平触发 / level-triggered）与 ET（边缘触发 / edge-triggered）
//       在「读者没有把数据一次读干净」时的行为分叉 —— 也就是线上最常见的那类「连接还在、
//       数据不动」的假活。
//
// 【性质说明】纯内存状态机模拟，不是内核实测。模型里只有三个东西：
//   内核接收缓冲区（数组）、就绪判定、应用侧的 read(chunk)。
//   省略了：TCP 窗口/粘包、EPOLLONESHOT、EPOLLERR|EPOLLHUP 的优先级、阻塞 fd 上的 read 语义。
//
// 运行：node .\m15-03-lt-et.mjs

const CHUNK = 32; // 应用侧一次 read 最多取 32 字节（远小于内核缓冲区里的数据量）

function makeSocket(totalBytes) {
  // 内核接收缓冲区：一共 totalBytes 字节待读
  return { kernelBuf: totalBytes, readBytes: 0, eagainHits: 0 };
}

// 一次 read：缓冲区非空就取 CHUNK 字节；空则返回 EAGAIN
function readOnce(sock) {
  if (sock.kernelBuf <= 0) { sock.eagainHits++; return 0; }
  const n = Math.min(CHUNK, sock.kernelBuf);
  sock.kernelBuf -= n;
  sock.readBytes += n;
  return n;
}

// LT：只要缓冲区里还有数据，下一次 epoll_wait 就继续报告「可读」
function runLT(sock, maxWakeups) {
  let wakeups = 0, notified = 0;
  while (wakeups < maxWakeups) {
    wakeups++;
    if (sock.kernelBuf > 0) notified++;         // 水平触发：有数据就一直报
    else break;
    readOnce(sock);                             // 每次都只读一次，不循环
  }
  return { notified, wakeups };
}

// ET：只在「空 → 非空」这个状态跳变时报一次；之后不管缓冲区里剩多少都不再报
function runET(sock, maxWakeups, drainUntilEAGAIN) {
  let wakeups = 0, notified = 0;
  let lastStateEmpty = true;
  while (wakeups < maxWakeups) {
    wakeups++;
    const nowEmpty = sock.kernelBuf <= 0;
    if (lastStateEmpty && !nowEmpty) {           // 只在边缘上报
      notified++;
      if (drainUntilEAGAIN) {
        while (sock.kernelBuf > 0) readOnce(sock); // 正确写法：读到 EAGAIN 为止
      } else {
        readOnce(sock);                        // 错误写法：只读一次就回去等
      }
    }
    lastStateEmpty = sock.kernelBuf <= 0;
    if (sock.kernelBuf <= 0) break;
  }
  return { notified, wakeups };
}

const TOTAL = 100;
const MAX_WAKEUPS = 50;

const cases = [
  { name: 'LT  + 每次只 read 一次', fn: (s) => runLT(s, MAX_WAKEUPS) },
  { name: 'ET  + 每次只 read 一次（典型 bug）', fn: (s) => runET(s, MAX_WAKEUPS, false) },
  { name: 'ET  + read 到 EAGAIN（正确写法）', fn: (s) => runET(s, MAX_WAKEUPS, true) },
];

console.log(`内核接收缓冲区初始有 ${TOTAL} 字节待读，应用侧单次 read 上限 = ${CHUNK} 字节，最多唤醒 ${MAX_WAKEUPS} 次`);
console.log('');
console.log('场景                              | 收到可读通知次数 | 读出字节 | 残留字节 | 是否读干净');
console.log('----------------------------------|------------------|----------|----------|-----------');

for (const c of cases) {
  const sock = makeSocket(TOTAL);
  const r = c.fn(sock);
  console.log(
    `${c.name.padEnd(33)} | ${String(r.notified).padStart(16)} | ${String(sock.readBytes).padStart(8)} | ${String(sock.kernelBuf).padStart(8)} | ${sock.kernelBuf === 0 ? '是' : '否 '}`
  );
}

console.log('');
console.log('读法：');
console.log('  1) LT 下「只读一次」不会丢数据，只是多几次唤醒 —— 所以 LT 更安全，代价是同一份数据被反复通知；');
console.log('  2) ET 下「只读一次」= 剩下的 68 字节永久卡在内核缓冲区：连接没断、TCP 还活着、应用却再也不会被通知，');
console.log('     表现为前端 SSE 卡住不动、日志里没有任何异常，直到连接超时 —— 这是 ET 最典型的线上事故形态；');
console.log('  3) ET 的正确约定是：被通知后必须循环 read 直到返回 EAGAIN，否则下一次边缘可能永远不来。');
