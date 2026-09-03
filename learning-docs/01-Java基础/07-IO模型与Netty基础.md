# I/O 模型与 Netty 基础

> 来源：[00-学习路线总览/04-Java基础补全开发计划](../00-学习路线总览/04-Java基础补全开发计划.md) P5。
> 对应项目：chat 的 SSE 流式对话（`text/event-stream`）、notify 的 WebSocket/STOMP、
> 网关（Spring Cloud Gateway = WebFlux + Netty）与业务服务（Servlet + Tomcat）的 I/O 模型差异。
> 用法层见 [04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md)，本篇讲底层。

---

## 一、三种 I/O 模型

### 1.1 BIO：一个连接一个线程

```
客户端连接 → accept() 阻塞等待 → new Thread(read 阻塞 + write 阻塞)
1000 个连接 = 1000 个线程 = 线程栈内存爆炸 + 内核调度开销
```

问题：阻塞在 `read()` 上的线程什么都干不了。适合连接数少的场景，不适合长连接。

### 1.2 NIO：一个 Selector 管成百上千连接

三件套：**Channel**（双向通道，替代单向流）、**Buffer**（数据容器）、
**Selector**（多路复用器，一个线程监听多个 Channel 的事件）。

```java
// NIO echo server（完整可运行，学习示例）
public class NioEchoServer {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(9999));
        server.configureBlocking(false);                      // 必须非阻塞才能注册 Selector
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();                                 // 阻塞直到有就绪事件（内核 epoll）
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();                                   // 手动移除，否则重复处理
                if (key.isAcceptable()) {
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    int n = client.read(buf);
                    if (n == -1) { key.cancel(); client.close(); continue; }
                    buf.flip();                                // 切换读模式（limit=position, position=0）
                    client.write(buf);                         // echo 回去
                }
            }
        }
    }
}
```

> Buffer 的 flip/clear 指针操作是 NIO 出了名的难用——这也是 Netty ByteBuf
> （双指针 readerIndex/writerIndex）出现的直接原因。

### 1.3 select / poll / epoll（多路复用的内核演进）

| | select | poll | epoll |
|---|--------|------|-------|
| fd 上限 | 1024 | 无 | 无 |
| 就绪查找 | O(n) 全量遍历 | O(n) | **O(1)**：内核回调放入就绪链表 |
| fd 拷贝 | 每次调用全量拷入内核 | 同 | epoll_ctl 注册一次 |

Linux 下 NIO 的 `Selector` 底层就是 **epoll**（Mac 是 kqueue）。
Java 的 AIO（Proactor）在 Linux 上是 epoll 模拟、无性能优势，**Netty 已放弃 AIO
transport**——面试提这点能证明真的理解。

---

## 二、Reactor 模式（Netty 的骨架）

```
单线程 Reactor：accept + read/write + 业务 全在一个线程（Redis 6 前的单线程模型）
多线程 Reactor：一个 Reactor 负责 accept + I/O，业务丢给 worker 线程池
主从 Reactor：主 Reactor 只管 accept，从 Reactor（线程池）管 I/O —— Netty 的 boss/worker
```

Netty 默认：`bossGroup`（通常 1 个 EventLoop）处理连接建立，
`workerGroup`（= CPU 核数）处理已建连接的读写；**一个 Channel 绑定一个 EventLoop
直到销毁**——同一连接的所有事件始终在同一线程执行，handler 无需加锁（无锁串行化）。

---

## 三、零拷贝（与中间件直接相关）

```
传统 read+write：磁盘 → 内核缓冲 → 用户缓冲 → Socket 缓冲 → 网卡   （4 次拷贝 + 4 次上下文切换）
sendfile：      磁盘 → 内核缓冲 → 网卡（带校验和）                （2 次拷贝，无用户态往返）
mmap：          磁盘映射进用户空间，省"内核→用户"那次拷贝
```

| 技术 | 用的零拷贝 | 本项目对应 |
|------|-----------|-----------|
| Kafka | sendfile | （对比学习） |
| **RocketMQ** | mmap（MappedFile） | `ai-cs-mq` 的消息存储层 |
| ES/Lucene | mmap | ai-cs-search |
| Java API | `FileChannel.transferTo()`（sendfile）、`MappedByteBuffer`（mmap） | — |
| Netty | `CompositeByteBuf`、`FileRegion`（包装 transferTo）、堆外 DirectBuffer | 网关转发 |

---

## 四、Netty 速览：为什么所有客户端都基于它

- **EventLoop**：`NioEventLoop = Selector + 任务队列`，run 循环里"轮询 I/O 事件 + 执行任务"
- **Pipeline**：责任链。入站事件从头到尾（解码 → 业务 handler），出站从尾到头
  （编码）；`ctx.fireChannelRead(msg)` 手动传递，忘了传消息就"断链"——排查 Netty
  问题先查这个
- **ByteBuf**：双指针（读写分离，无 flip）、引用计数（`refCnt`/`release`，漏 release
  就是堆外内存泄漏）、池化分配器（PooledByteBufAllocator，jemalloc 思路）
- **粘包/拆包**：TCP 是字节流没有消息边界，解决方案三件套——定长
  （`FixedLengthFrameDecoder`）、分隔符（`DelimiterBasedFrameDecoder`）、
  长度域（`LengthFieldBasedFrameDecoder`，最常用）；HTTP/SSE/STOMP 各有现成编解码器

> 本项目的连接全景：Redis（Lettuce=Netty）、ES（原生 java client=Netty/异步）、
> RocketMQ 客户端（Netty）、网关（Netty）。**你每天都在用 Netty，只是没意识到。**

---

## 五、HTTP 层：keep-alive、chunked、SSE（项目现场）

### 5.1 SSE 协议格式（chat 流式对话的线上事实）

```
HTTP/1.1 200 OK
Content-Type: text/event-stream          ← 关键头
Cache-Control: no-cache
Connection: keep-alive

event: token                             ← 事件类型（可省，默认 message）
data: {"delta":"你"}                     ← 每帧数据

id: 42                                   ← 断线重连时的 Last-Event-ID
retry: 3000                              ← 建议重连间隔(ms)

data: {"delta":"好"}                     ← 空行分隔每个事件
```

- 单向（服务器→客户端）、纯文本、基于 HTTP——浏览器端 `EventSource` 自动重连；
  复杂度远低于 WebSocket。**本项目 chat 用它流式输出 LLM token**
- Servlet 栈用 `SseEmitter`（异步 Servlet），注意异步上下文与超时配置；
  网关（Netty）透传时要确认禁用响应缓冲，否则流被攒成一坨

### 5.2 两种技术栈的 I/O 模型差异（本项目架构事实）

```
前端 ──HTTP──→ ai-cs-gateway（Spring Cloud Gateway = WebFlux + Netty，Reactor 主从）
                    │ 路由
                    ├──→ chat（spring-boot-starter-web = Servlet + Tomcat NIO）
                    │      ↑ SSE 长响应
                    └──→ notify（WebSocket/STOMP）
```

| | 网关 WebFlux | 业务服务 Servlet |
|---|-------------|------------------|
| 容器 | Netty | Tomcat（NIO，默认 200 工作线程） |
| 模型 | 少量 EventLoop + 响应式链 | 请求占用一个工作线程直到响应完成 |
| 风险 | 阻塞调用会卡死 EventLoop（**必须**异步/弹性调度） | 200 线程易被慢请求（LLM 2~10s）耗尽 |

> 这正是 chat 里线程池隔离（[04-并发编程进阶 §5](./04-并发编程进阶.md)）与
> 03-微服务治理计划"网关阻塞调用治理"的底层原因——
> I/O 模型决定了并发容量的天花板。

---

## 六、Netty echo 三行版（对照第二节的原生 NIO）

```java
public class NettyEchoServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);       // 主 Reactor：accept
        EventLoopGroup worker = new NioEventLoopGroup();      // 从 Reactor：I/O
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                         @Override
                         public void channelRead(ChannelHandlerContext ctx, Object msg) {
                             ctx.writeAndFlush(msg);          // ByteBuf 直接回写
                         }
                     });
                 }
             });
            b.bind(9999).sync().channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully(); worker.shutdownGracefully();
        }
    }
}
```

对比原生 NIO：没有手动 Selector 轮询、没有 flip、没有手动 remove key——
这些都被 EventLoop + Pipeline 封装了。

---

## 七、高频面试题（含参考答案）

**Q1：BIO/NIO/AIO 的区别？**
A：BIO 同步阻塞，一个连接占一个线程，read/write 都阻塞；NIO 同步非阻塞，
Selector（epoll）单线程监听多连接的就绪事件，就绪后才读写；AIO 异步，内核完成
I/O 后回调通知（Proactor），但 Linux 实现不成熟，Netty 已移除 AIO 支持。
"同步/异步"看谁负责把数据从内核拷到用户空间：应用自己做=NIO，内核做完通知=AIO。

**Q2：epoll 相比 select/poll 好在哪？**
A：select 有 1024 fd 上限且每次调用把 fd 集合从用户态拷入内核、返回后要 O(n)
遍历找就绪；poll 去掉上限但仍是 O(n) 遍历 + 每次拷贝。epoll 用 epoll_ctl 把 fd
注册进内核红黑树（只注册一次），就绪事件由回调放进就绪链表，epoll_wait 只返回
就绪的 fd——O(1) 检索、无重复拷贝。连接多但活跃少时优势最大（长连接服务）。

**Q3：什么是零拷贝？RocketMQ/Kafka 各用什么？**
A：传统文件发送要 4 次拷贝（含 2 次用户态往返）+ 4 次上下文切换。零拷贝让数据
不经过用户空间：sendfile 只在内核态 2 次拷贝（Kafka 用它）；mmap 把文件映射进
用户地址空间，省一次拷贝（RocketMQ 的 MappedFile 用它）。Java 对应
`FileChannel.transferTo` 与 `MappedByteBuffer`。

**Q4：Netty 的线程模型？为什么 handler 不用加锁？**
A：主从 Reactor：boss EventLoop 处理 accept，worker EventLoop 处理已建连接 I/O。
一个 Channel 的生命周期绑定在一个 EventLoop 线程上，该 Channel 的所有事件
串行执行——单连接内天然无并发，handler 无需同步；跨连接才需要考虑并发。

**Q5：TCP 粘包拆包是什么？怎么解决？**
A：TCP 是字节流，没有"消息"边界，一次 read 可能读到半条或多条消息（Nagle 合包、
MSS 分段都会造成）。解决：应用层定义边界——定长帧、分隔符（如 Redis 的 \r\n）、
长度域头（最通用）。Netty 提供对应三个 Decoder。SSE/HTTP 用空行分隔事件帧。

**Q6：SSE 和 WebSocket 怎么选？**
A：SSE：单向推送、纯 HTTP、文本、浏览器 EventSource 自动重连、实现与运维成本
低——适合 LLM 流式输出、通知推送。WebSocket：双向、二进制、需 Upgrade 握手
与独立子协议（本项目 notify 用 STOMP）、需要心跳与网关透传配置。只需要
"服务器说客户端听"就选 SSE，双向交互（协同编辑、对话上屏+打断）才上 WebSocket。

**Q7：Spring Cloud Gateway 为什么用 Netty 而不是 Tomcat？**
A：网关是典型 I/O 密集 + 长连接场景（SSE/WebSocket 透传），Netty 的 EventLoop
模型用少量线程支撑大量连接；Tomcat 每请求一线程，网关转发虽快，但下游慢响应
（LLM 2~10s）会占满 200 工作线程。WebFlux + Netty 让"等待下游"不占线程。
代价：整条链路不能阻塞（这也是网关 filter 里禁同步 IO 的原因）。

**Q8：ByteBuf 相比 NIO ByteBuffer 好在哪？**
A：① 读写双指针（readerIndex/writerIndex），不需要 flip/clear 这种指针舞蹈；
② 容量自动扩展；③ 池化分配减少 GC；④ 引用计数管理堆外内存（漏 release 泄漏，
异常路径也要 release，惯用 try/finally 或 SimpleChannelInboundHandler 自动释放）；
⑤ CompositeByteBuf 聚合多缓冲免拷贝。

**Q9：Netty 空轮询 bug 是什么？（了解即可）**
A：JDK epoll 的历史 bug：Selector 意外唤醒且 selectedKeys 为空，循环全速空转
CPU 100%。Netty 的对策：检测到512 次空轮询就重建 Selector 并迁移 Channel。
展示你了解 Netty 与 JDK 的恩怨即可，重点是排查 CPU 100% 时知道有这一型。

**Q10：一次 HTTP 请求经过本项目链路，I/O 模型发生了几次变化？**
A：前端 → 网关（Netty EventLoop）→ 业务服务（Tomcat NIO 工作线程）→
下游（Feign/RestTemplate，占着工作线程等响应）→ Redis/ES/MQ（Netty 客户端）。
能沿链路说出"哪一段是事件驱动、哪一段占线程阻塞"，等于把架构瓶颈讲清楚了。

---

## 八、学习检查清单

- [ ] 能说清 BIO/NIO/AIO 与 select/poll/epoll 两组概念（同步异步 × 阻塞非阻塞）
- [ ] 现场写出原生 NIO echo server 并解释 flip 的作用
- [ ] 画出 Reactor 三形态，说出 Netty boss/worker 与"Channel 绑定 EventLoop"
- [ ] 记住零拷贝两个 API 与 RocketMQ(mmap)/Kafka(sendfile) 的对应
- [ ] 能默写 SSE 帧格式并解释本项目为什么选 SSE 而不是 WebSocket

## 九、动手实践（对照 04 计划 P5 任务）

1. 跑通本文 NioEchoServer，用 `telnet 127.0.0.1 9999` 验证回显
2. `curl -N` 打 chat 的流式接口，逐帧对照第五节 SSE 格式解读
3. （可选）用 Netty 重写 echo server，体会 EventLoop/Pipeline 封装了什么

---

## 下一步

→ [08-语言级细节与序列化](./08-语言级细节与序列化.md)
