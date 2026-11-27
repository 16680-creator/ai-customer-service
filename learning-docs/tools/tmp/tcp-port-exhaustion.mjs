// tcp-port-exhaustion.mjs —— 纯算术：TIME_WAIT 与临时端口如何决定"连接速率上限"
// 用法：node tcp-port-exhaustion.mjs [临时端口数] [TIME_WAIT 秒数] [持续占用连接数]
// 默认值来自本机实测：netsh int ipv4 show dynamicport tcp -> 49152 / 16384
// 说明：这是上界估算（假设所有连接都指向同一个远端 IP:Port、且全部由本机主动关闭），
//       不是吞吐实测；真实系统里远端地址分散会放大上限，NAT 端口复用会缩小上限。
const ports = Number(process.argv[2] ?? 16384);
const twSeconds = Number(process.argv[3] ?? 120);
const steady = Number(process.argv[4] ?? 200);

// 同一 (远端IP, 远端端口) 下，本机可用四元组数量 = 临时端口数
const rateLimit = ports / twSeconds;
console.log(`临时端口数            = ${ports}`);
console.log(`TIME_WAIT 持续时间    = ${twSeconds}s`);
console.log(`持续占用（ESTABLISHED）= ${steady}`);
console.log(`-> 到同一目标的最大新建速率 ≈ ${ports}/${twSeconds} = ${rateLimit.toFixed(2)} 次/秒`);
console.log(`-> 剩余可用于短连接的端口 = ${ports - steady}，对应速率 ≈ ${((ports - steady) / twSeconds).toFixed(2)} 次/秒`);

// 反过来：给定 QPS，每个短连接平均占用端口多久，才不会被 TIME_WAIT 卡住
for (const qps of [50, 200, 1000, 5000]) {
  const need = qps * twSeconds; // 需要的四元组数量
  const verdict = need <= ports ? 'OK' : `不足（缺 ${need - ports}）`;
  console.log(`QPS=${String(qps).padStart(5)} 且每连接都主动关闭 -> 需要 ${need} 个四元组：${verdict}`);
}
