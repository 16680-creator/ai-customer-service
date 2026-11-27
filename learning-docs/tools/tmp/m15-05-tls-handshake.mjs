// 15-计算机基础 / 05-HTTP与TLS —— C 级实测脚本 C：TLS 握手在本地能看到什么、看不到什么
//
// 干什么：用本机自签证书（m15-05-tls-cert.py 生成，已提交在 tools/tmp/ 下）起一个 TLS 服务端，
//   然后用 node 客户端分别做四组观测：
//     1) TLS 1.3 完整握手 vs 恢复（resumption）——看 getProtocol()/isSessionReused()
//     2) TLS 1.2 完整握手 vs 恢复——同上，对比协议版本与套件差异
//     3) SNI：带 servername 连接 vs 用 IP 连接（不带 servername），看服务端收到什么
//     4) mTLS：服务端 requestCert:true 时，客户端不带证书 / 带证书 的两种结果
//
// 为什么这么设计：
//   "TLS 省不掉首次握手"这句话必须能被观测到，而不是只靠背。能直接观测到的事实有：
//     - 协议版本与密码套件（握手谈成了什么）
//     - isSessionReused()：第二次连接到底有没有重新做完整握手
//     - 服务端 servername：SNI 是不是真的传上去了（决定多域名站点拿到哪张证书）
//     - requestCert 前后握手结果不同：mTLS 到底在哪一侧失败
//   注意：本机 loopback 没有真实网络往返，1-RTT 与 2-RTT 的时间差会被噪声淹没，
//   所以下面的耗时只作参照，不做结论。往返数的账用 m15-05-tls-flights.mjs 算。
//
// 边界：只绑 127.0.0.1 临时端口，不联网；证书 30 天有效期、仅本机实验用。
//
// 运行：
//   python .\m15-05-tls-cert.py     # 首次：生成 m15-05-localhost-cert.pem / -key.pem
//   node .\m15-05-tls-handshake.mjs

import tls from 'node:tls';
import fs from 'node:fs';

const CERT = 'm15-05-localhost-cert.pem';
const KEY = 'm15-05-localhost-key.pem';
const cert = fs.readFileSync(new URL(`./${CERT}`, import.meta.url));
const key = fs.readFileSync(new URL(`./${KEY}`, import.meta.url));

const ms = (t0) => Number(process.hrtime.bigint() - t0) / 1e6;

function startServer(opts = {}) {
  const state = { sni: [], newSession: 0, resumeSession: 0, secureConn: 0, clientErrors: [] };
  const server = tls.createServer(
    { cert, key, ALPNProtocols: ['h2', 'http/1.1'], ...opts },
    (socket) => {
      // 握手已经完成才会走到这里；回一个字节让客户端读到数据后自然收尾
      socket.on('data', (d) => socket.write(d));
      socket.on('end', () => socket.end());
    },
  );
  server.on('secureConnection', (s) => {
    state.secureConn += 1;
    state.sni.push(s.servername ?? '(空)');
  });
  server.on('newSession', () => { state.newSession += 1; });
  // 注意：resumeSession 的第二个参数是回调，必须调用它，否则握手会一直挂着
  // （实测见 m15-05-tls-resume-trap.mjs：不调用 cb 时客户端等到超时，服务端最后报 ERR_TLS_HANDSHAKE_TIMEOUT）
  server.on('resumeSession', (_id, cb) => { state.resumeSession += 1; cb(null, null); });
  server.on('tlsClientError', (err) => { state.clientErrors.push(err.code || err.message); });
  return { server, state };
}

function connect(port, opts = {}) {
  return new Promise((resolve) => {
    const t0 = process.hrtime.bigint();
    let settled = false;
    const socket = tls.connect(
      { host: '127.0.0.1', port, ca: cert, ALPNProtocols: ['h2', 'http/1.1'], ...opts },
      () => {
        settled = true;
        const info = {
          handshakeMs: ms(t0),
          protocol: socket.getProtocol(),
          cipher: socket.getCipher()?.name,
          alpn: socket.alpnProtocol,
          reused: socket.isSessionReused(),
          servername: socket.servername ?? '(空)',
          authorized: socket.authorized,
          authError: socket.authorizationError ?? null,
          session: socket.getSession(),
        };
        const peer = socket.getPeerCertificate(true);
        info.certSubject = peer.subject?.CN;
        info.certIssuer = peer.issuer?.CN;
        info.certValidTo = peer.valid_to;
        info.selfSigned = peer.subject?.CN === peer.issuer?.CN;
        info.chainLen = (() => {
          let n = 0;
          let c = peer;
          const seen = new Set();
          while (c && !seen.has(c.fingerprint256)) { seen.add(c.fingerprint256); n += 1; c = c.issuerCertificate; }
          return n;
        })();
        socket.end();
        resolve(info);
      },
    );
    socket.on('error', (err) => { settled = true; resolve({ error: err.code || err.message, handshakeMs: ms(t0) }); });
    // 兜底：握手卡住时不要让脚本永远挂着
    socket.on('timeout', () => { if (!settled) resolve({ error: 'TIMEOUT', handshakeMs: ms(t0) }); });
    socket.setTimeout(5000, () => { if (!settled) { resolve({ error: '握手 5s 未完成', handshakeMs: ms(t0) }); socket.destroy(); } });
  });
}

const show = (label, i) => {
  if (i.error) {
    console.log(`${label}  失败: ${i.error}  (${i.handshakeMs.toFixed(2)}ms)`);
    return;
  }
  console.log(
    `${label}\n    protocol=${i.protocol} cipher=${i.cipher} alpn=${i.alpn} sessionReused=${i.reused} ` +
    `servername=${i.servername} authorized=${i.authorized}\n` +
    `    cert: CN=${i.certSubject} issuer=${i.certIssuer} selfSigned=${i.selfSigned} chainLen=${i.chainLen} valid_to=${i.certValidTo}\n` +
    `    握手耗时(loopback，仅参照)=${i.handshakeMs.toFixed(2)}ms`,
  );
};

// ---------- 1 & 2：1.3 / 1.2 完整握手与恢复 ----------
const { server, state } = startServer();
await new Promise((r) => server.listen(0, '127.0.0.1', r));
const port = server.address().port;
console.log(`[server] tls 127.0.0.1:${port}  ALPN=[h2,http/1.1]  minVersion=默认`);

console.log('\n=== 观测 1：TLS 1.3 ===');
const a13 = await connect(port, { servername: 'localhost' });
show('[1.3 完整握手]', a13);
const b13 = await connect(port, { servername: 'localhost', session: a13.session });
show('[1.3 复用 session]', b13);

console.log('\n=== 观测 2：把版本钉死到 TLS 1.2 ===');
const a12 = await connect(port, { servername: 'localhost', maxVersion: 'TLSv1.2' });
show('[1.2 完整握手]', a12);
const b12 = await connect(port, { servername: 'localhost', maxVersion: 'TLSv1.2', session: a12.session });
show('[1.2 复用 session]', b12);

console.log('\n=== 观测 3：SNI 有没有传上去 ===');
const withSni = await connect(port, { servername: 'localhost' });
show('[客户端带 servername=localhost]', withSni);
const noSni = await connect(port, { servername: undefined });
show('[客户端只给 IP，不发 servername]', noSni);
console.log(`服务端 secureConnection 共 ${state.secureConn} 次，收到的 servername 序列 = [${state.sni.join(', ')}]`);

console.log('\n=== 观测 3 附：TLS1.2 的会话事件计数（node 只在 1.2 上emit newSession/resumeSession） ===');
console.log(`newSession=${state.newSession} resumeSession=${state.resumeSession}`);

server.close();

// ---------- 4：mTLS ----------
console.log('\n=== 观测 4：服务端 requestCert:true（要求客户端出示证书） ===');
const mtls = startServer({ requestCert: true, rejectUnauthorized: true });
await new Promise((r) => mtls.server.listen(0, '127.0.0.1', r));
const mtlsPort = mtls.server.address().port;

const noClientCert = await connect(mtlsPort, { servername: 'localhost' });
show('[客户端不带证书]', noClientCert);

const withClientCert = await connect(mtlsPort, { servername: 'localhost', cert, key });
show('[客户端带证书]', withClientCert);

console.log(`服务端侧记录到的 tlsClientError = [${mtls.state.clientErrors.join(', ')}]`);
mtls.server.close();
console.log('\n[done]');
