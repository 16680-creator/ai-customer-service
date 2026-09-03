# 计算机基础

> 本专题是 `learning-docs` 的 **第十五个模块**（`15-计算机基础`），讲 `01`~`14` 里"框架底下"的操作系统与计算机网络层：**不是通用教科书，每一篇都落回 `ai-customer-service` 的真实文件与真实现象**。
> 前置阅读：[00-学习路线总览](../00-学习路线总览/README.md)、[01-Java基础/03-JVM内存模型与GC实战](../01-Java基础/03-JVM内存模型与GC实战.md)、[01-Java基础/07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md)

---

## 一、为什么要在框架之下单独学 OS 与网络

`01`~`14` 教你"怎么用框架"，本模块回答"**框架底下 OS 与网络层为什么这样**"。同样是调参数，懂底层的人问的是"这个参数动了内核的哪个机制"：

| 场景 | 只懂框架 | 懂 OS/网络之后 |
|---|---|---|
| SSE 对话 20 人并发就报警 | 知道加机器 | 知道 Tomcat 默认 200 工作线程、`futureFlux.get()` 阻塞在哪、上下文切换一次要几微秒 |
| 容器被杀 exit 137 | 会重启容器 | 知道是 cgroup limit 触发 OOM Killer、`-Xmx512m` 和 1Gi limit 的差值去哪了 |
| 压测后 `netstat` 一堆 TIME_WAIT | 会紧张 | 知道 2MSL 的由来、`tcp_tw_reuse` 的前提、为什么压测机是 TIME_WAIT 大户 |
| 消息里 emoji 变 `?` | 会说"编码问题" | 知道 MySQL `utf8` 是 3 字节残血版、UTF-16 代理对 `length()==2` |
| Redis 挂了数据丢了多少 | 会背"everysec" | 知道 AOF `appendfsync everysec` 的页缓存语义、最坏丢 2 秒的窗口怎么算 |

一句话：**框架的默认值都是对某个底层机制的取舍**。本模块就是把这些取舍的"底牌"翻开。

---

## 二、知识地图

```
                            计算机基础（OS × 网络 × 硬件）
                                      │
        ┌─────────────────┬───────────┴──────────┬──────────────────┐
        │                 │                      │                  │
   【并发与调度】      【内存】                【I/O 与文件】       【网络协议】
        │                 │                      │                  │
   进程/线程/协程      虚拟内存/分页/TLB        inode/页缓存        TCP 握手挥手
   上下文切换成本      page fault              fsync 刷盘语义      TIME_WAIT/队列
   CFS 调度器         RSS vs VSS/swap         select/poll/epoll   keepalive/Nagle
   线程数 vs 核数     cgroup/OOMKilled        LT/ET 触发          滑动窗口/拥塞控制
        │                 │                      │                  │
        │            【隔离与容器】            【HTTP 与 TLS】      【硬件层】
        │                 │                      │                  │
        │            namespace 六种            keep-alive          缓存行/MESI
        │            cgroup v1/v2              HTTP/2 多路复用     伪共享/store buffer
        │            overlayfs 分层            SSE 分帧/WS 握手    内存屏障/volatile
        │            JVM 容器感知              TLS 1.2 vs 1.3      CAS/ABA
        │
   【编码与时间】
   UTF-8 变长/UTF-16 代码点
   utf8mb4/collation
   时区/serverTimezone/TZ
```

---

## 三、文档清单与学习路线

### 第一阶段：并发与资源（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 01 | [进程线程与调度](./01-进程线程与调度.md) | 进程/线程/协程、上下文切换成本（数值级）、CFS、线程数与核数、SSE 长连接挂线程分析 | ⭐⭐ |
| 02 | [内存管理与虚拟内存](./02-内存管理与虚拟内存.md) | 虚拟内存/分页/TLB、RSS vs VSS、swap、OOMKilled、容器下 JVM 行为、项目内存配置盘点 | ⭐⭐ |

### 第二阶段：I/O 与网络（1-2 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 03 | [文件系统与IO多路复用](./03-文件系统与IO多路复用.md) | inode/页缓存/fsync、buffered vs direct、epoll 内部机制与 LT/ET、MySQL 刷盘/主从、Redis 单线程 | ⭐⭐⭐ |
| 04 | [TCP协议实战](./04-TCP协议实战.md) | 握手挥手逐包、TIME_WAIT 2MSL 与调优、半/全连接队列、keepalive、Nagle、滑动窗口/拥塞控制、粘包 | ⭐⭐⭐ |
| 05 | [HTTP与TLS](./05-HTTP与TLS.md) | keep-alive、HTTP/2 多路复用与队头阻塞、QUIC 概览、SSE 分帧、WebSocket 握手、TLS 1.2 vs 1.3、网关 8080 透传 | ⭐⭐ |

### 第三阶段：隔离与环境（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 06 | [容器隔离原理-namespace与cgroup](./06-容器隔离原理-namespace与cgroup.md) | 六种 namespace、cgroup v1/v2、overlayfs、容器里 JVM 看到的资源、limits 对 GC/JIT 的影响 | ⭐⭐⭐ |
| 07 | [字符编码与时区](./07-字符编码与时区.md) | UTF-8 变长、UTF-16 代码点、emoji 截断坑、utf8mb4/collation、JDBC 编码与时区参数、容器 TZ | ⭐⭐ |

### 第四阶段：硬件层（3-5 天）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 08 | [CPU缓存与内存序](./08-CPU缓存与内存序.md) | 缓存行与 MESI、伪共享、store buffer 与内存屏障、volatile 的硬件根源、CAS/ABA、网关并发代码走读 | ⭐⭐⭐ |

---

## 四、本项目中的底层机制现场（核心特色）

> 学完原理后**回到这里对照真实文件看一遍**——每行都给了 file:line，禁止凭印象背诵。

| 底层机制 | 项目现场（真实文件） | 对应文档 |
|---|---|---|
| Servlet 线程模型 + 异步 Servlet | `ai-cs-chat/.../controller/ChatController.java:168` 返回 `SseEmitter` | [01](./01-进程线程与调度.md) |
| 异步线程阻塞等待 | `ChatServiceImpl.java:601` `futureFlux.get()`（TimeLimiter 60s 兜底） | [01](./01-进程线程与调度.md) |
| 线程复用与 ThreadLocal 清理 | `ChatController.java:88` "Tomcat 线程被复用" 注释 + `finally` 清理 | [01](./01-进程线程与调度.md) |
| 容器内存限制与 JVM | compose 各服务 `JAVA_OPTS: "-Xms256m -Xmx512m"`；K8s `ai-chat-service.yaml:80-86` limits 1Gi | [02](./02-内存管理与虚拟内存.md) |
| 中间件内存预算 | ES `ES_JAVA_OPTS: -Xms512m -Xmx512m`（compose:91）、MySQL `innodb-buffer-pool-size=256M`（mysql.cnf:4）、Redis `maxmemory 256mb`（redis.conf:24） | [02](./02-内存管理与虚拟内存.md) |
| WAL + fsync 语义 | `master.cnf:16` `innodb-flush-log-at-trx-commit=1`；Redis `appendfsync everysec`（redis.conf:16） | [03](./03-文件系统与IO多路复用.md) |
| 主从复制 I/O 链 | `slave.cnf:6-16` relay-log + replicate-do-db | [03](./03-文件系统与IO多路复用.md) |
| TCP keepalive 现场参数 | `redis.conf:34-35` `tcp-keepalive 300` / `timeout 0`；MySQL `wait-timeout=28800`（master.cnf:25） | [04](./04-TCP协议实战.md) |
| SSE 传输层生命周期 | `ChatServiceImpl.java:156` 5 分钟 emitter 超时 vs TCP keepalive 分工 | [04](./04-TCP协议实战.md) |
| 网关统一入口与 SSE 透传 | `ai-cs-gateway/.../RouteConfig.java:81-84` `/api/chat/**` 路由；`:176-178` 重试仅 GET | [05](./05-HTTP与TLS.md) |
| SSE 空行分帧（应用层解粘包） | `ai-cs-frontend/src/views/ChatView.vue:388` `buffer.indexOf('\n\n')` | [04](./04-TCP协议实战.md)、[05](./05-HTTP与TLS.md) |
| 容器隔离与镜像分层 | `ai-cs-chat/Dockerfile`（多阶段构建，builder 层不进运行镜像） | [06](./06-容器隔离原理-namespace与cgroup.md) |
| utf8mb4 全链配置 | `master.cnf:20-21`、`init.sql:283` chat_db 建库、JDBC URL `characterEncoding=utf-8&serverTimezone=Asia/Shanghai`（tools/nacos-config/ai-cs-message.yml:3） | [07](./07-字符编码与时区.md) |
| emoji/中文对话消息 | `init.sql:305-318` `chat_message.content TEXT`（用户输入含 emoji 是真实业务场景） | [07](./07-字符编码与时区.md) |
| 无锁计数（CAS） | `ai-cs-gateway/.../InstanceInFlightRegistry.java:40-48` `AtomicInteger.incrementAndGet/updateAndGet` | [08](./08-CPU缓存与内存序.md) |
| 混合并发策略 | `TokenBucketRateLimiter.java:25,39` `ConcurrentHashMap` + per-bucket `synchronized` | [08](./08-CPU缓存与内存序.md) |

---

## 五、速查表（背下来，排查问题从这里出发）

### 数量级速查

| 操作 | 量级 | 直觉换算 |
|---|---|---|
| CPU 缓存命中（L1/L2/L3） | ~1 / ~4 / ~15 ns | 比内存快 1~2 个数量级 |
| 内存访问 | ~100 ns | |
| 上下文切换（线程） | 1~10 μs | 1ms 里可切换几百次 |
| 一次 TCP 握手（同机房） | ~1 ms | RTT 主导 |
| MySQL 同机房 RTT | 0.5~1 ms | 连接池存在的根本原因 |
| 磁盘 fsync（SSD） | ~0.1~1 ms | 比内存写慢 1 万倍 |
| `Thread.sleep(1ms)` 实际 | ≥1~2 ms | 定时器精度 + 调度延迟 |

### 关键默认值（本项目相关）

| 组件 | 参数 | 现值 | 出处 |
|---|---|---|---|
| Tomcat | 工作线程数 | 200（项目未显式配置） | Spring Boot 默认 |
| MySQL | `innodb_flush_log_at_trx_commit` | 1 | `master.cnf:16` |
| MySQL | `max_connections` | 500 | `master.cnf:24` |
| Redis | `appendfsync` | everysec | `redis.conf:16` |
| Redis | `maxmemory` | 256mb + allkeys-lru | `redis.conf:24-25` |
| Redis | `tcp-keepalive` | 300 | `redis.conf:34` |
| 微服务 JVM | 堆 | -Xms256m -Xmx512m | compose 各服务 `JAVA_OPTS` |
| chat SSE | emitter 超时 | 5 min | `ChatServiceImpl.java:156` |
| chat SSE | LLM TimeLimiter | 60 s | `ai-cs-chat/application.yml:74-77` |
| MySQL 字符集 | utf8mb4 / unicode_ci | 全库统一 | `master.cnf:20-21` |

### 本项目端口/进程速查

| 组件 | 端口/标识 | 本模块相关文档出现处 |
|---|---|---|
| 网关 | 8080 | [05](./05-HTTP与TLS.md)、[04](./04-TCP协议实战.md) |
| ai-cs-chat | 8083 | [01](./01-进程线程与调度.md)、[02](./02-内存管理与虚拟内存.md) |
| ai-cs-py-chat | 8000 | [01](./01-进程线程与调度.md)（协程对照组） |
| MySQL | 3306 | [03](./03-文件系统与IO多路复用.md)、[07](./07-字符编码与时区.md) |
| Redis | 6379 | [03](./03-文件系统与IO多路复用.md)、[04](./04-TCP协议实战.md) |

### 排查命令速查

```bash
# 线程与调度
top -H -p $(pidof java)          # 线程级 CPU（找出占核的线程）
pidstat -w -p $(pidof java) 1    # 每秒自愿/非自愿上下文切换

# 内存
ps -o pid,rss,vsz -p $(pidof java)   # RSS（实际物理内存）vs VSZ（虚拟地址空间）
docker stats                          # 容器视角内存（≈cgroup 计量）
dmesg | grep -i "killed process"      # OOM Killer 现场记录

# 网络与连接
ss -s                             # 连接总览（含 timewait 计数）
ss -tan state time-wait | wc -l   # TIME_WAIT 数量（压测后必看）
cat /proc/net/sockstat            # 半/全连接队列溢出会在这里体现

# 容器
docker inspect aics-chat-service | grep -i memory   # cgroup 限制
cat /sys/fs/cgroup/memory/memory.limit_in_bytes     # v1 限额实际值

# 编码与时区
docker exec aics-mysql mysql -uroot -proot -e "show variables like '%char%'; show variables like '%time_zone%';"
```

---

## 六、与既有模块的划界（防重复学习）

本模块刻意"只写增量"，凡是已有文档讲过的内容只给链接不重写。学习前先对齐边界：

| 主题 | 已讲的地方（不要在这里重学） | 本模块只讲什么 |
|---|---|---|
| BIO/NIO/AIO、Selector、Netty | [07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) | [03](./03-文件系统与IO多路复用.md)：inode/页缓存/fsync、epoll 内核内部与 LT/ET |
| JMM、happens-before、volatile 语义 | [04-并发编程进阶](../01-Java基础/04-并发编程进阶.md) | [08](./08-CPU缓存与内存序.md)：MESI/伪共享/store buffer 的硬件根源 |
| JVM 堆内分区、GC 算法 | [03-JVM内存模型与GC实战](../01-Java基础/03-JVM内存模型与GC实战.md) | [02](./02-内存管理与虚拟内存.md)：虚拟内存/RSS/swap/cgroup 下的 JVM |
| 容器 OOMKilled 现象 | 上述 GC 文档 Q9、[02-Kubernetes入门](../07-运维部署/02-Kubernetes入门.md) | [02](./02-内存管理与虚拟内存.md)：完整内存账本与排查命令 |
| Docker 用法（build/run/compose/volume） | [01-Docker容器化](../07-运维部署/01-Docker容器化.md) | [06](./06-容器隔离原理-namespace与cgroup.md)：namespace/cgroup/overlayfs 原理 |
| SSE/WS 用法与选型 | [05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md) | [04](./04-TCP协议实战.md)、[05](./05-HTTP与TLS.md)：传输层/协议层细节 |
| TCP 粘包的三种 Decoder | [07-IO模型与Netty基础](../01-Java基础/07-IO模型与Netty基础.md) §四 | [04](./04-TCP协议实战.md) §九：粘包的传输层根源 |
| MySQL/utf8mb4 用法 | [01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md) | [07](./07-字符编码与时区.md)：字节层、Java 层、连接层与截断坑 |
| Java 并发工具/线程池参数 | [04-并发编程进阶](../01-Java基础/04-并发编程进阶.md) | [01](./01-进程线程与调度.md)：调度器、上下文切换成本、线程数天花板 |
| 缓存淘汰、限流算法 | [11-数据结构与算法](../11-数据结构与算法/README.md) | [08](./08-CPU缓存与内存序.md)：这些实现背后的硬件行为 |

---

## 七、学习方法建议

1. **现象先行**：每篇开头都是项目的真实现象/真实配置，先看现象再学原理，最后回到"这个配置改了会怎样"。
2. **命令必须真跑**：本模块的命令在 Linux 容器/宿主机上跑一遍，比看十遍都有用。没有 Linux 环境，就在 Docker Desktop 里 `docker run -it ubuntu bash` 练。
3. **数值敏感**：OS 与网络是"数量级的学问"，1μs、1ms、1s 差三个数量级就是三种架构决策，记住量级比记住定义有用。
4. **划界阅读**：本模块与 `01-Java基础` 多处划界（IO/并发/JVM/OOMKilled），本模块只讲 OS/网络/硬件层，Java 层用链接回跳，不要重复学。
5. **诚实面对项目现状**：项目里没配的东西（如容器内存 limit、TLS）文档会如实标注"项目现状"，这本身就是学习点——知道"该配而没配"也是能力。

---

> 返回 [学习路线总览](../00-学习路线总览/README.md)
