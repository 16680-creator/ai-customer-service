# TCP 协议实战

> 对应项目：`scripts/loadtest/k6/sse-chat.js`（20 VU 挂长连接压测）、`deploy/redis/redis.conf:34-35`（`tcp-keepalive 300`/`timeout 0`）、
> `deploy/mysql/master.cnf:24-26`（`max-connections=500`/`wait-timeout=28800`）、
> `ai-cs-chat/.../service/impl/ChatServiceImpl.java:156`（SSE 5 分钟超时）。
> **划界声明**：[04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md) 讲 SSE/WS 应用层用法，[01-Java基础/07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) §四 讲粘包的三种 Decoder；本篇讲**传输层**——握手挥手、TIME_WAIT、连接队列、keepalive、Nagle、窗口与拥塞控制，以及"连接为什么会被中间设备悄悄断掉"。

---

## 一、从压测现象说起

[08-测试/05-性能压测实战-k6与JMeter](../08-测试/05-性能压测实战-k6与JMeter.md) §压测检查清单里写了一条："SSE 的连接堆积：流式会话压完确认连接真的关了（TIME_WAIT/服务端连接数），长连接泄漏会把下一轮压测污染成'慢在连接建立'"。

用项目自带的 `scripts/loadtest/k6/sse-chat.js`（`ramping-vus` 阶梯拉到 20 VU，每 VU 持有一条 SSE 流）压完一轮，压测机上大概率看到：

```bash
ss -tan state time-wait | wc -l        # 数千计 TIME_WAIT
ss -tan state close-wait | wc -l       # 若持续增长 → 对端关了你没关（代码泄漏）
```

为什么压测机是 TIME_WAIT 大户、为什么这些连接要等 60 秒才消失、SSE 长连接放着不管为什么会被断——下面按协议生命周期逐段讲。

---

## 二、三次握手：逐包看

以 `curl -N POST /api/chat/stream/sse` 打向网关 8080 为例：

```
客户端                          网关 8080（listen 队列）
  │ ── SYN, seq=x ────────────────▶│  客户端进入 SYN_SENT
  │ ◀─ SYN+ACK, seq=y, ack=x+1 ── │  服务端进入 SYN_RCVD
  │ ── ACK, ack=y+1 ──────────────▶│  双方 ESTABLISHED
```

| 设计点 | 理由 |
|---|---|
| 为什么三次 | 双方都要确认"我的发送与接收能力正常"；两次无法防止**历史重复 SYN** 旧连接复活（RFC 793 的经典理由） |
| seq 初始值随机（ISN） | 防止上一条同四元组连接的旧报文串进新连接 |
| 握手成本 | 1 个 RTT（同机房 ~1ms）——所以连接池/长连接的价值就是省这个 RTT + 内核建连接开销 |

**抓包验证**（可复制）：

```bash
tcpdump -i any -nn port 8080 -w /tmp/gw.pcap
# Wireshark 打开，过滤器 tcp.flags.syn==1，逐包对照上图
```

---

## 三、四次挥手：谁进 TIME_WAIT

```
A（主动关闭方）                         B（被动关闭方）
  │ ── FIN, seq=u ──────────────────────▶│  A: FIN_WAIT_1 → B: CLOSE_WAIT
  │ ◀─ ACK, ack=u+1 ──────────────────── │  A: FIN_WAIT_2
  │ ◀─ FIN, seq=w, ack=u+1 ───────────── │  B 半关完，发自己的 FIN
  │ ── ACK, ack=w+1 ────────────────────▶│  A: TIME_WAIT（等 2MSL）→ CLOSED
  │                                      │  B: 收到 ACK 即 CLOSED
```

**两个高频排障点**：

| 现象 | 含义 | 项目对应 |
|---|---|---|
| 服务端大量 **CLOSE_WAIT** | 客户端已 FIN，**你的代码一直没调 close**（fd 泄漏） | SSE 场景若漏掉 `emitter.onCompletion/onTimeout` 里的清理，网关连接 fd 会堆积；`ChatServiceImpl.java:646` 的 `flux.subscribe` 在 onComplete/onError 里都有 `emitter.complete()` 收尾 |
| 压测机大量 **TIME_WAIT** | 压测机是**主动关闭方**（HTTP 客户端发 FIN） | k6 每次请求新建连接时的正常残留，见 §四 |

---

## 四、TIME_WAIT：为什么是 2MSL、怎么调

### 4.1 为什么要等 2MSL

MSL（最大报文生存期，Linux 取 30s）是 IP 报文在网络中存活的上限。2MSL = 60s 的两个理由：

1. **最后一个 ACK 可能丢**：主动方发出最后 ACK 若丢失，被动方会重传 FIN——主动方必须留在 TIME_WAIT 重发 ACK。等 2MSL = "FIN 重传 + ACK 传输"两段生存期都覆盖；
2. **让旧连接的迷路报文全部消亡**：保证本四元组的旧报文过期，不会污染下一个同四元组的新连接。

### 4.2 调优参数与适用条件

```bash
# 压测机（临时生效）
sysctl -w net.ipv4.tcp_tw_reuse=1        # ⭐ 只对"主动发起连接"方向生效，且依赖 tcp_timestamps（默认开）
sysctl -w net.ipv4.tcp_max_tw_buckets=262144   # TIME_WAIT 总量上限，超出直接 RST（默认 262144？以 sysctl -a 为准）
```

| 手段 | 作用 | 前提/代价 |
|---|---|---|
| `tcp_tw_reuse` | 1MSL 后可复用 TIME_WAIT 端口发起新连接 | 仅客户端/出方向；需对端时间戳支持 |
| `SO_REUSEADDR` | 监听端口绑定不再受 TIME_WAIT 干扰 | 只影响 bind，不减少 TIME_WAIT |
| 连接池 / HTTP keep-alive | **根治**：不频繁建断连接 | 客户端配置问题，不是内核问题 |
| 四元组扩容 | 客户端多绑临时端口（`ip_local_port_range`） | 掩盖问题 |

**正确姿势（也是本项目思路）**：压测脚本与业务客户端都用 keep-alive 复用连接（k6 与 axios 默认复用），TIME_WAIT 自然稀少；只有"每请求新建连接"的压测脚本才需要碰 `tcp_tw_reuse`。

---

## 五、半连接队列与全连接队列

```
         SYN →  [半连接队列 SYN_RCVD]  → ACK →  [全连接队列 ESTABLISHED]  → accept() 取走
                tcp_max_syn_backlog                min(backlog, somaxconn)
```

| 队列 | 满了会怎样 | 排查 |
|---|---|---|
| 半连接队列 | 新 SYN 被丢弃/忽略（防 SYN Flood 时代会开 syncookies） | `netstat -s | grep -i listen`（SYNs to LISTEN sockets dropped） |
| 全连接队列 | 服务端 **accept 不够快**，完成握手的连接被丢，客户端表现为偶发超时/重传 | `ss -lnt` 看 `Recv-Q/Send-Q`（listen 状态下 Recv-Q ≈ 当前全连接队列积压） |

**项目关联**：Tomcat 的 `acceptCount`（默认 100）就是全连接队列长度；SSE 建流风暴时（§01 的 60s 阻塞段）如果线程全忙，accept 还在进行但请求排队——"连接建立成功却迟迟没响应"要往这两个队列上查。

---

## 六、keepalive 与长连接保活：连接为什么会被"悄悄断掉"

### 6.1 TCP keepalive 是内核层的探活

默认参数极保守：空闲 **7200s** 后才发探测，间隔 **75s** × 9 次。所以"默认 keepalive"对业务长连接形同虚设，必须调小：

```bash
# 容器/进程可按 socket 覆盖（应用层设置），或系统级：
sysctl -w net.ipv4.tcp_keepalive_time=300 net.ipv4.tcp_keepalive_intvl=30 net.ipv4.tcp_keepalive_probes=3
```

### 6.2 项目现场：三层超时/保活的分工

| 层 | 配置 | 现值 | 出处 | 作用 |
|---|---|---|---|---|
| 内核 TCP keepalive | `tcp-keepalive` | **300s** | `redis.conf:34` | Redis 对空闲客户端连接发探测，清掉半死连接 |
| 应用层空闲剔除 | `timeout` | **0（不剔除）** | `redis.conf:35` | Redis 不主动踢空闲客户端，交给 keepalive |
| 服务端连接生命周期 | `wait-timeout` | **28800s（8h）** | `master.cnf:25` | MySQL 主动断开空闲 8h 的连接——连接池不配 keepalive/校验的话，第二天早上的第一个请求就是 "connection reset" |
| 应用层 SSE 超时 | `SSE_EMITTER_TIMEOUT` | **5min** | `ChatServiceImpl.java:156` | emitter 超时后服务端主动 complete（发 FIN 的应用层动因） |

### 6.3 "中间设备断长连接"的完整解释

SSE 对话流 5 分钟内若模型长时间不出 token（RAG 检索慢、模型排队），链路上只有 TCP 层在"沉默"。而客户端与网关之间往往隔着 NAT/LVS/Nginx/云 LB——**每台设备都对空闲连接有自己的老化时间（常见 60~300s）**，超时后静默删除会话表项：

```
客户端 ── NAT/LB（空闲 60s 清表）── 网关 8080 ── chat
问题：设备悄悄清表，两端都不知道；下一个包到达时设备直接丢弃/RST
     → 前端表现：SSE 流"卡死后无报错断开"，EventSource 静默重连
解法：让链路上"有包在流动"——应用层心跳（SSE 注释行/WS ping-pong）
     或把 TCP keepalive 调到小于所有中间设备的最短空闲超时（本项目 Redis 的 300s 就是这个思路）
```

**结论**：应用层超时（5min）设得比中间设备空闲超时短，或加心跳——两选一，否则用户会看到"随机断流"。这就是"传输层为什么连接会被中间设备断掉"的完整答案。

---

## 七、Nagle 与延迟确认：40ms 卡顿的经典配方

| 机制 | 行为 | 初衷 |
|---|---|---|
| **Nagle** | 有未确认的小包时，后续小包攒着，攒成 MSS 或收到 ACK 才发 | 减少网络里的小包（telnet 一个键一包的时代） |
| **延迟确认（Delayed ACK）** | 接收方 ACK 不立即发，等 ~40ms 或捎带数据一起发 | 减少纯 ACK 包 |

两个"省包"机制相遇：发送方等 ACK 才发下一小包，接收方等数据才发 ACK → **SSE 逐 token 小包场景可能出现 40ms 一跳的锯齿延迟**。

**项目现状（诚实说明）**：SSE 流式服务端对此的解法是 `TCP_NODELAY`（关 Nagle）。Spring Boot 的 Tomcat 连接默认 `tcpNoDelay=true`，网关（Reactor Netty）默认同样开启——项目**没有显式配置过任何 socket option（grep 确认无 childOption/socket 配置）**，靠的是框架默认值。若自建 Netty 服务器，`childOption(ChannelOption.TCP_NODELAY, true)` 必须手动加，这是 [07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) 没展开、而传输层必须知道的点。

---

## 八、滑动窗口与拥塞控制

### 8.1 两个窗口，各管一头

| 窗口 | 谁控制 | 含义 |
|---|---|---|
| **接收窗口 rwnd** | 接收方通告（TCP 头里的 window 字段） | "我缓冲区还能装多少"——**流控**，别把接收方灌死 |
| **拥塞窗口 cwnd** | 发送方自己维护 | "网络大概还能吃多少"——**拥塞控制**，别把网络灌死 |
| 实际发送量 | `min(rwnd, cwnd)` | 两者取小 |

### 8.2 拥塞控制四阶段

```
慢启动：cwnd 从 10 MSS 起步，每 RTT 翻倍（指数）
拥塞避免：cwnd ≥ ssthresh 后每 RTT +1 MSS（线性）
快重传：收到 3 个重复 ACK → 立即重传丢失段（不等超时）
快恢复：cwnd 减半回到拥塞避免（而非重置到 1 重新慢启动）
```

**对项目的影响**：

- SSE 首帧的 RTT 里包含慢启动爬坡——**短连接的每次新建都要重新爬坡**，长连接复用后 cwnd 已经爬好，这也是"压测脚本要复用连接"的第二个理由（第一个是 §四的 TIME_WAIT）；
- 网关到 LLM 的外网调用延迟大（RTT 高），大响应体（长回复）传输受"带宽 × RTT"限制——这就是长肥管道，BBR 类算法对外网链路更友好（Linux 4.9+ 可用，项目未改默认 cubic）。

---

## 九、粘包拆包：传输层根源与应用层定界

**TCP 是字节流，没有"消息"边界**。产生"粘/拆"的三个传输层原因：

| 原因 | 机制 |
|---|---|
| MSS 分段 | 发送缓冲区攒够一个 MSS（≈1460B）才成段发出 → 两条消息被并进一段 |
| Nagle 合包 | §七：小包攒批发送 → 多条消息粘连 |
| 接收时机 | 接收方 `read()` 的时机与发送方的分段无关 → 一次读到半条或多条 |

**定界必须在应用层做**，本项目的 SSE 就是活例子：

```
服务端：每条事件以空行结尾（data: {...}\n\n）
前端：ai-cs-frontend/src/views/ChatView.vue:386-394
      buffer += decoder.decode(value, { stream: true })
      while ((sep = buffer.indexOf('\n\n')) >= 0) { ... }   // 空行 = 事件边界
      for line in rawEvent.split('\n'): line.startsWith('data:')  // 取 data 行
```

`indexOf('\n\n')` 就是应用层"从字节流里切消息"的定界器——与 Netty `DelimiterBasedFrameDecoder` 同一思想（定长/分隔符/长度域三件套见 [07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) §四，不重复）。WebSocket 则由协议自带帧长度字段定界，所以"没有粘包问题"。

---

## 十、面试高频问答

**Q1：为什么握手三次、挥手四次？**
A：握手三次保证双方收发能力各被确认一次，且能挡住历史重复 SYN；挥手四次因为 TCP 全双工，两个方向的关闭各自独立（FIN/ACK 各一对），被动方可能还有数据要发，所以 ACK 和它的 FIN 不能合并。

**Q2：TIME_WAIT 为什么是 2MSL？过多怎么办？**
A：①保证最后一个 ACK 丢失后能响应被动方的 FIN 重传；②让本连接旧报文在网络中全部消亡，避免污染新连接。过多时先看角色：主动关闭方才有 TIME_WAIT——根治是连接池/keep-alive 复用连接；客户端方向可用 `tcp_tw_reuse`（依赖时间戳），服务端大可不必动它。

**Q3：CLOSE_WAIT 堆积说明什么？**
A：被动关闭方收到 FIN 后回了 ACK 但应用一直没调 close()——**代码泄漏 fd**，与内核参数无关。排查方向是异常分支漏 close：如 SSE 场景 emitter 的 onCompletion/onTimeout/finally 清理缺失，或 HTTP 客户端没复用也没关闭响应体。

**Q4：半连接队列和全连接队列的区别与排查？**
A：半连接队列收 SYN_RCVD 状态（满则丢 SYN，可开 syncookies 抗 SYN Flood）；全连接队列收完成握手待 accept 的连接（满则丢，客户端表现为超时重试）。`netstat -s | grep -i listen` 看丢弃计数，`ss -lnt` 的 Recv-Q 看积压；全连接溢出 = accept 速度跟不上。

**Q5：TCP keepalive 和应用层心跳的区别？**
A：keepalive 在内核层，探活失败才断，不能感知应用假死（进程活着、线程卡死）；应用层心跳在协议里跑（SSE 注释帧/WS ping），能带上业务状态、间隔可控。中间设备的老化时间看的是"有没有包流动"，两者只要有一个在发就能续命。

**Q6：SSE 长连接为什么会被中间设备断掉？怎么防？**
A：NAT/LB/代理对空闲连接有老化时间（常见 60~300s），超时静默清表，两端无感知，下一个包被丢。防法：应用层定期心跳（或 SSE 周期注释行/下行 keepalive 帧），或把 TCP keepalive 调到小于最短设备超时（本项目 redis.conf:34 的 300s 就是该思路）；同时应用层超时要与心跳配套（ChatServiceImpl.java:156 的 5min）。

**Q7：Nagle 与延迟确认一起会造成什么问题？**
A：发送方等 ACK 攒包，接收方延迟 40ms 发 ACK——小包交互场景出现 40ms 锯齿延迟。解法 `TCP_NODELAY` 关 Nagle。Tomcat/Reactor Netty 默认已开；自建 Netty 要显式 `childOption(TCP_NODELAY, true)`。

**Q8：滑动窗口和拥塞窗口分别解决什么问题？**
A：rwnd 是流控，保护接收方缓冲区（TCP 头 window 字段通告）；cwnd 是拥塞控制，保护网络（慢启动指数爬坡→拥塞避免线性增长→快重传/快恢复应对丢包）。实际发送量 = min(rwnd, cwnd)。短连接每次都要重新慢启动，是长连接复用的隐性收益。

**Q9：TCP 为什么会粘包？SSE 怎么解决的？**
A：TCP 是字节流无消息边界，MSS 分段、Nagle 合包、读取时机共同造成"一次读多消息或半条"。定界靠应用层协议：SSE 用空行分隔事件（ChatView.vue:388 `indexOf('\n\n')`），WebSocket 帧头自带长度；Netty 提供定长/分隔符/长度域三种 Decoder。

---

## 十一、动手练习

1. 压一轮 `scripts/loadtest/k6/sse-chat.js`，压后立刻 `ss -tan state time-wait | wc -l` 与 `state close-wait` 计数；把 k6 换成每请求新建连接（`Connection: close`）再压一轮，对比两个数字并解释差异。
2. 用 `tcpdump -i any -nn port 8080 -w /tmp/sse.pcap` 抓一次完整 SSE 请求，在 Wireshark 里找出三次握手、`GET/POST`、每帧 data、四次挥手，标注状态机变迁点。
3. 用 `sysctl net.ipv4.tcp_keepalive_time` 查看本机默认值，写一个 5 分钟无输出的 TCP echo 实验：验证默认 keepalive（7200s）下连接"看着活着其实早被 NAT 清表"的现象，再加 300s 探测对比。
4. 读 `ChatServiceImpl.java:739-808`（onComplete/onError 分支），画出"客户端断开/服务端 complete/流异常"三条路径各自谁先发 FIN，判断每条路径结束后服务端是否留 TIME_WAIT。
5. 改造练习：给 `redis.conf` 估算空闲连接策略——若把 `timeout 300`（5 分钟踢空闲）与 `tcp-keepalive 300` 同开，说明两者各自的触发条件与先后顺序，写出你们小组选择哪个组合的理由（对照 [04-中间件/01-Redis缓存实战](../04-中间件/01-Redis缓存实战.md) 的连接池配置）。

---

> 上一篇：[03-文件系统与IO多路复用](./03-文件系统与IO多路复用.md) ｜ 下一篇：[05-HTTP与TLS](./05-HTTP与TLS.md)
