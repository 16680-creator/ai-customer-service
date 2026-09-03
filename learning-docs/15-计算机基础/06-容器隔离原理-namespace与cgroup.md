# 容器隔离原理：namespace 与 cgroup

> 对应项目：`ai-cs-chat/Dockerfile`（多阶段构建模板，9 个服务同款）、`deploy/docker-compose/docker-compose-all.yml`、
> `deploy/k8s/services/ai-chat-service.yaml:80-86`（resources limits）。
> **划界声明**：[07-运维部署/01-Docker容器化](../07-运维部署/01-Docker容器化.md) 讲 Docker **用法**（image/container/Compose/volume、怎么 build 怎么跑）；本篇讲**隔离机制原理**——namespace 隔离了什么、cgroup 限住了什么、overlayfs 怎么分层、容器里的 JVM 到底看到多少资源。

---

## 一、先立结论：容器不是虚拟机，是"被圈起来的进程"

```
虚拟机：Hypervisor 虚拟出整套硬件 → 每个 VM 独立内核 → 隔离强、重、秒级启动
容器：  一个普通 Linux 进程 + namespace（骗它"世界只有我一个"）+ cgroup（限制它能用多少）
        → 共享宿主内核 → 启动毫秒级、开销趋近于零，但隔离强度弱于 VM
```

`docker run` 的内核动作等价于三件事：

1. `clone()` 时传入一组 `CLONE_NEW*` 标志 → 创建新 namespace；
2. 把进程写进 cgroup 目录（设置 CPU/内存限额）；
3. `pivot_root` 切到镜像解包出来的根文件系统（overlayfs 合并视图）。

理解了这句，本篇的所有细节都是这三个动作的展开。

---

## 二、六种 namespace：分别"骗"过了什么

| Namespace | 隔离内容 | 容器里的表现 | 验证命令 |
|---|---|---|---|
| **PID** | 进程编号空间 | 容器内自己的 `PID 1` 就是 java 进程，看不见宿主其他进程 | `ls /proc`；`docker exec aics-chat-service ps -ef` |
| **NET** | 网络栈：网卡/路由/iptables/端口 | 容器有独立 eth0；`8080` 是容器内端口，映射靠宿主 iptables/NAT | `docker exec aics-chat-service ip a` |
| **MNT** | 挂载点视图 | 容器看到的 `/` 是镜像根；volume 是额外挂载 | `cat /proc/mounts` |
| **UTS** | 主机名与域名 | `hostname` 显示容器 ID | `docker exec aics-mysql hostname` |
| **IPC** | System V IPC/POSIX 消息队列 | 容器间不能用共享内存通信 | `ipcs` |
| **USER** | 用户/组 ID 映射 | 容器内 root 可映射为宿主普通用户 | `id`（容器内 vs 宿主） |

> Linux 后来补了 **Cgroup**（隔离 cgroup 根视图）与 **Time**（5.6+，隔离时钟）两个 namespace，机制同上，面试提一句即可。
> **注意 PID namespace 的副作用**：容器里 `PID 1` 是 java，它没有 init 的僵尸进程回收能力——多线程应用在容器里产生僵尸子进程时要留意（Java 进程极少 fork 子进程，本项目不受影响，但这是经典面试题）。

### 2.1 namespace 不隔离什么（安全的另一半）

namespace 只负责"视图"，**不等于安全边界**。完整的容器安全还要叠加：

| 机制 | 管什么 | 与 namespace 的关系 |
|---|---|---|
| **Capabilities** | root 的特权拆成约 40 个能力位，容器默认丢弃大部分（如 CAP_SYS_ADMIN） | 容器内 root ≠ 宿主 root |
| **seccomp** | 系统调用白名单，Docker 默认 profile 拦掉几十个危险 syscall | 视图之外再加"行为"限制 |
| AppArmor/SELinux | 强制访问控制（文件/网络策略） | 纵深防御 |
| rootless 容器 | 整个 Docker daemon 跑在非 root 用户 + USER namespace 映射下 | 内核逃逸漏洞的缓解 |

**记忆法**：namespace 防的是"看到"，capabilities/seccomp 防的是"做到"。面试被问"容器安全 = namespace 吗"，答出这层才完整。

**进入同一组 namespace 的方式**：`docker exec` 本质就是用 `setns()` 把新进程挂进目标容器已有 namespace——所以"进入容器"没有任何魔法，就是切换视图。

---

## 三、cgroup：限住了多少

namespace 管"看得见什么"，cgroup 管"用得了多少"。

### 3.1 v1 与 v2

| | cgroup v1 | cgroup v2（统一层级） |
|---|---|---|
| 结构 | 每种资源（cpu/memory/blkio/pids…）一棵独立树，`/sys/fs/cgroup/cpu`、`.../memory` | 单一棵树，`/sys/fs/cgroup/` 下每个目录是一个控制组 |
| 控制器 | 可分别挂在不同层级，配置分散 | 统一，`memory.max`、`cpu.max`、`pids.max` |
| 现状 | 老发行版/老 Docker | 新发行版与 K8s 1.25+ 默认 |

### 3.2 两个最常用的控制器

```
memory：memory.max（硬限，超了触发 OOM Killer） / memory.high（软限，超了节流回收）
        容器 RSS 是整个 cgroup 的口径 —— 这就是 [02-内存管理与虚拟内存] §4 OOMKilled 的判定依据
cpu：   cpu.max = "quota period"（v2）或 cpu.cfs_quota_us / cpu.cfs_period_us（v1）
        例：K8s limits cpu=1000m → quota=100000 period=100000 → 每 100ms 周期最多用 100ms CPU
        超额不是杀死，是【节流 throttled】：线程被暂停到下个周期 —— 表现为"没跑满却抖动"
```

```bash
# 在容器内看实际生效限额
cat /sys/fs/cgroup/cpu.max                  # v2："100000 100000" 即 1 核
cat /sys/fs/cgroup/memory.max               # v2；v1 为 memory/memory.limit_in_bytes
cat /sys/fs/cgroup/memory.events | head     # oom_kill 计数（v2，被杀过几次一目了然）
```

### 3.3 项目现场：两套部署的 cgroup 差异

| 部署 | CPU | 内存 | 证据 |
|---|---|---|---|
| docker-compose（单机） | **未设任何限制** | **未设任何限制**（无 `mem_limit`/`deploy.resources`，已 grep 确认） | `docker-compose-all.yml` 全文件 |
| K8s | `limits.cpu: 1000m` | `limits.memory: 1Gi`（requests 512Mi） | `deploy/k8s/services/ai-chat-service.yaml:80-86` |

同一个镜像，compose 下 JVM 能看到宿主全部资源，K8s 下被压在 1 核/1Gi 里——**行为差异直接来自 cgroup**，下节展开。

> **requests 与 limits 的分工**：limits 落成 cgroup 硬约束（memory.max / cpu.max）；requests 不产生任何内核机制，只参与**调度**——节点剩余可分配量不足 requests 的 Pod 不会被调度上去，且 CPU 抢占时按 requests 加权。一句话：requests 是"排座位用的申报值"，limits 是"真物理墙"。

---

## 四、overlayfs：镜像分层与写时复制

### 4.1 四个目录合成一个根

```
merged（容器运行时的 /）  =  lowerdir（多个只读镜像层，从上往下叠）
                          + upperdir（容器可写层，唯一可变）
                          + workdir（overlayfs 内部工作目录）

读：各层从上往下找，找到即返回（上层遮住下层同名文件）
写：文件在只读层 → 先整份【拷贝】到 upper 层再改 = Copy-on-Write
删：在 upper 层放一个 whiteout 标记文件 = "逻辑删除"，下层原文件还在
```

推论（都是面试高频）：

- **同 base 的镜像共享只读层**：9 个服务镜像的 `eclipse-temurin:17-jre-jammy` 底座层物理上只存一份，磁盘与拉取都省；
- **容器删文件不省磁盘**：COW + whiteout，只读层永远在；
- **数据必须放 volume**：写在容器可写层的数据随容器删除而消失——`docker-compose-all.yml:19` 把 `/var/lib/mysql` 挂卷正是为了绕开 overlayfs。

### 4.2 项目现场：多阶段构建 = 把"_builder 层"留在最终镜像外

```dockerfile
# ai-cs-chat/Dockerfile（22 行，全文）
FROM maven:3.9-eclipse-temurin-17 AS builder     # 阶段1：含 Maven + JDK 的重镜像（~几百 MB）
WORKDIR /build
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-chat/pom.xml ai-cs-chat/
RUN mvn dependency:go-offline -B -pl ai-cs-chat -am   # 先拉依赖，利用层缓存（pom 不变则不重拉）
COPY ai-cs-common/ ai-cs-common/
COPY ai-cs-chat/ ai-cs-chat/
RUN mvn package -pl ai-cs-chat -am -DskipTests -B

FROM eclipse-temurin:17-jre-jammy                # 阶段2：只要 JRE 的运行镜像
WORKDIR /app
COPY --from=builder /build/ai-cs-chat/target/*.jar app.jar   # 只拷产物 jar
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]  # 堆参数从环境变量注入（02 篇 §4.2）
```

从 overlayfs 视角解读：

| 设计 | 分层收益 |
|---|---|
| 两个 `FROM` | builder 阶段的 Maven/源码/依赖层**不进入**运行镜像的 lowerdir，最终镜像只剩 JRE + jar |
| 先 COPY pom 再 go-offline | 依赖层缓存：代码改动不触发依赖重拉（层缓存按指令+内容失效） |
| `COPY --from=builder` | 跨"阶段"拷文件——本质是从另一个镜像树里取文件 |

---

## 五、容器里 JVM 看到的 CPU/内存与宿主差异

### 5.1 内存：堆从哪算起

| 场景 | JVM 看到的"机器内存" | 默认堆（MaxRAMPercentage=25%） |
|---|---|---|
| 裸机/无 cgroup limit | 宿主物理内存 | 宿主内存 × 25%（32G 机器 → 8G 堆 ❌ 危险） |
| 有 cgroup memory limit（K8s 1Gi） | **limit 值 1Gi**（UseContainerSupport 默认开） | 256Mi（偏小） |
| 显式 `-Xmx`（项目现状） | 不参与计算 | 显式值（compose 512m / K8s 512m） |

**项目结论**：因为 compose 没有 memory limit（§3.3），若不显式传 `JAVA_OPTS`，JVM 会按宿主内存算出巨大默认堆——所以 `ENTRYPOINT` 的 `java ${JAVA_OPTS:-}`（`Dockerfile:21`）与 compose 每服务 `JAVA_OPTS: "-Xms256m -Xmx512m"` 是**必要的兜底**，不是可有可无的习惯（详见 [02-内存管理与虚拟内存](./02-内存管理与虚拟内存.md) §4）。

### 5.2 CPU：核数感知决定了一串线程数

`Runtime.getRuntime().availableProcessors()` 在容器里**不是宿主核数**，JDK 10+ 按优先级读：cgroup quota（取整）→ cpuset → cpu.shares → 宿主核数。它直接决定：

| 派生量 | 公式/行为 | 对项目的意义 |
|---|---|---|
| **GC 线程数** | ParallelGCThreads ≈ 核数；G1 并发标记/并行回收线程随之缩放 | K8s `1000m` limit → JVM 认为 1 核 → GC 线程极少；压测发现 GC 排队，先查这个而不是盲目调参 |
| **JIT 编译线程** | C1/C2 编译器线程数 ≈ 核数 | 低限额下热点代码编译变慢，启动后前几分钟 JMH/压测数字偏低是正常现象 |
| **ForkJoinPool.commonPool** | 并行度 = 核数 - 1（≥1） | `ResilientAiService.java:255` 的 supplyAsync 实际并行度被 limit 收紧（详见 [01-进程线程与调度](./01-进程线程与调度.md) §六） |
| 显式覆盖 | `-XX:ActiveProcessorCount=n`、`-XX:ParallelGCThreads=n` | 想脱离 cgroup 感知时的手动旋钮 |

**验证命令**（可复制）：

```bash
docker exec aics-chat-service sh -c \
  "java -XX:+PrintFlagsFinal -version 2>/dev/null | grep -E 'ActiveProcessorCount|MaxHeapSize' ; \
   java -Xlog:gc -version 2>&1 | head -3"
# 或在应用里打印：
jshell -q <<< 'System.out.println(Runtime.getRuntime().availableProcessors());'
```

> 补一句诚实说明：ES/RocketMQ 等镜像自带容器感知（ES 明确按容器 cgroup 配置），项目里 ES 用 `ES_JAVA_OPTS: "-Xms512m -Xmx512m"`（`docker-compose-all.yml:91`）显式圈堆，原理与 JVM 服务一致。

---

## 六、常用命令速查

```bash
# namespace 视角
docker exec aics-chat-service ps -ef        # 容器内进程视图（PID namespace）
ls /proc/<java_pid>/ns/                     # 宿主看该进程属于哪些 namespace（inode 相同 = 同组）

# cgroup 实况
docker exec aics-chat-service cat /sys/fs/cgroup/memory.max        # v2 内存硬限
docker exec aics-chat-service cat /sys/fs/cgroup/cpu.max           # v2 CPU 配额
cat /sys/fs/cgroup/memory.events | grep oom                        # v2 OOM 击杀计数

# overlayfs 实况（宿主）
docker inspect aics-chat-service --format '{{json .GraphDriver.Data}}'   # LowerDir/UpperDir/MergedDir
docker history ai-cs-chat:latest                                          # 每层多大、哪条指令产生

# 镜像层占用
docker system df -v | head -20
```

---

## 七、面试高频问答

**Q1：容器和虚拟机的区别？容器靠什么实现隔离？**
A：VM 通过 Hypervisor 虚拟硬件、每个 VM 独立内核，隔离强但重；容器是共享宿主内核的普通进程，靠 namespace 隔离视图（PID/NET/MNT/UTS/IPC/USER）、cgroup 限制资源用量、overlayfs 提供分层根文件系统。隔离强度弱于 VM（内核逃逸类漏洞是共担风险），换来毫秒级启动与近零开销。

**Q2：六种 namespace 分别隔离什么？**
A：PID（进程编号，容器内独立 PID 1）、NET（网卡/路由/iptables/端口）、MNT（挂载点视图）、UTS（主机名）、IPC（共享内存与信号量）、USER（uid/gid 映射，容器内 root 可映射宿主普通用户）。另外还有 Cgroup 和 Time 两个较新的 namespace。

**Q3：K8s 的 requests/limits 落到内核是什么机制？CPU 超限会怎样？**
A：limits 写进 Pod 内进程所在 cgroup：memory → memory.max（超限触发 OOM Killer，容器 exit 137）；cpu → CFS 带宽控制 cpu.max（quota/period），超限不是杀死而是**节流**——线程被挂起到下个 100ms 周期，表现为吞吐抖动。requests 影响调度权重，不是内核机制。

**Q4：overlayfs 的分层原理？为什么容器删文件不省磁盘？**
A：merged = 只读镜像层（lowerdir）+ 可写层（upperdir）合成。读文件自上而下找；修改只读层文件先整体拷到 upper 层再改（COW）；删除是在 upper 层放 whiteout 标记，下层文件仍在——所以删文件不释放空间，数据要持久化必须放 volume。

**Q5：为什么 9 个镜像共用一个基础镜像能省空间？**
A：`eclipse-temurin:17-jre-jammy` 等相同的只读层在磁盘（及 registry）只存一份，镜像间按层内容哈希去重共享；这也是多阶段构建有价值的原因——builder 阶段的重层不进入运行镜像，最终镜像只叠"JRE + jar"。

**Q6：容器里 JVM 的默认堆是怎么算的？为什么还要显式 -Xmx？**
A：JDK 10+ 默认 UseContainerSupport，读 cgroup memory limit，默认堆 = limit × MaxRAMPercentage(25%)。但本项目 compose 部署没有任何 memory limit，JVM 退化为按宿主机内存算——32G 宿主上单服务默认 8G 堆，十几个服务必然挤爆宿主，所以 compose 里 `JAVA_OPTS: -Xms256m -Xmx512m` 是必要兜底。

**Q7：容器 CPU limit=1 核，对 GC 和 JIT 有什么影响？**
A：availableProcessors 按 cgroup quota 取整为 1 → GC 线程、JIT 编译线程、ForkJoinPool 并行度全部按 1 核缩放：GC 并行度低、热点编译慢、公共池并行度仅 1。压测出现"GC 排队/JIT 预热慢"时先查限额感知结果（ActiveProcessorCount 可显式覆盖）。

**Q8：`docker exec` 的原理是什么？**
A：在宿主上启动一个新进程，用 setns() 把它挂进目标容器已有的一组 namespace（PID/NET/MNT…），再 attach 标准输入输出——所以"进入容器"只是切换视图，并没有第二个虚拟环境。

**Q9：多阶段构建为什么能瘦身？除了瘦身还有什么收益？**
A：第一阶段用含 Maven/JDK 的重镜像编译，第二阶段只 `COPY --from=builder` 产物 jar 到轻量 JRE 镜像——builder 的层不进入最终镜像 lowerdir。附加收益：依赖层缓存策略（先 COPY pom 再 go-offline）让"改代码不重拉依赖"；运行镜像不含编译器与源码，攻击面更小。

---

## 八、动手练习

1. 分别在 compose 与 K8s（或 minikube）里起 chat 服务，容器内对比 `cat /sys/fs/cgroup/cpu.max`、`memory.max` 与 `java -XX:+PrintFlagsFinal -version | grep MaxHeapSize` 的输出，写一段话解释两个环境 JVM 行为差异（对照 §3.3 表格）。
2. 用 `docker history ai-cs-chat:latest` 逐层看镜像构成，标出哪些层来自多阶段构建的哪一阶段；估算"若不用多阶段构建"最终镜像会多出多少 MB。
3. 进入容器执行 `ls /proc/1/ns/`，与宿主上同 pid 的 `/proc/<pid>/ns/` 对比 inode；再 `docker exec` 一次，用 `readlink /proc/self/ns/pid` 验证 exec 进程与容器主进程同 namespace。
4. 在 chat 容器里压测触发 CPU 节流：`cat /sys/fs/cgroup/cpu.stat` 记录 `nr_throttled`，跑一轮 k6 SSE 压测后再看增长——结合 §5.2 解释"GC 线程少 + 节流"的双重抖动来源。
5. 改造练习：为 `docker-compose-all.yml` 的 9 个 Java 服务补一版资源限制方案（`mem_limit`/`cpus`），要求"limit ≥ RSS 实测 × 1.3 且 ≤ 宿主可分配"，把你的账本（每个服务的估算依据）写成 10 行注释附在 compose 草稿里。

---

> 上一篇：[05-HTTP与TLS](./05-HTTP与TLS.md) ｜ 下一篇：[07-字符编码与时区](./07-字符编码与时区.md)
