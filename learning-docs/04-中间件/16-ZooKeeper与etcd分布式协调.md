# 16-ZooKeeper 与 etcd：从零开始理解分布式协调（认知拓展，工程未落地）

> **定位**：本仓的分布式协调用的是 **Redisson（Redis，AP 派）**（[04-中间件/07-Redisson分布式锁](07-Redisson分布式锁.md)），注册/配置是 **Nacos**（[02-Spring微服务/03](../02-Spring微服务/03-Nacos注册与配置中心.md)），而 **K8s 集群自己的"大脑数据"全存在 etcd**（[16-云原生与GitOps/02](../16-云原生与GitOps/02-K8s资源治理与弹性伸缩.md)）。ZooKeeper 与 etcd 是 CP 派协调的双雄——学它们是为了把"**选主、分布式锁、元数据存储、配置监听**"这四件事的完整坐标系建起来。工程不引入，只学。
> 前置阅读：[15-计算机基础/01-进程线程与调度](../15-计算机基础/01-进程线程与调度.md)（Raft/共识的计算机基础位）、[04-中间件/07](07-Redisson分布式锁.md)（AP 派锁）、[02-Spring微服务/03](../02-Spring微服务/03-Nacos注册与配置中心.md)。

---

## ⚡ 30 秒速记卡

```text
① 协调中间件 = 分布式系统的"公共记事本 + 排队叫号机"：存小量元数据 + 提供变更通知与互斥
② ZooKeeper：ZNode 树 + Watcher 一次性触发 + ZAB 共识（CP）；会话断 → 临时节点自动消失
③ etcd：KV + MVCC 多版本 + Watch 流 + Lease 租约（Raft 共识，CP）；K8s 的唯一存储
④ 临时节点/租约 = "心跳换存活"：掉线自动清理，这是选主与锁的关键机制
⑤ AP 锁（Redisson）性能高但极端下会双持；CP 锁（ZK/etcd）强一致但吞吐低——没有银弹，按业务选
```

---

## 一、它们到底"协调"什么？四个经典场景

| 场景 | 人话 | 谁在用 |
|---|---|---|
| **选主（Leader Election）** | 一堆副本里选出唯一"当班"的 | Kafka 老版 Controller、Solr、HDFS NameNode HA（ZK）；K8s controller-manager 的 Lease 抢占（etcd） |
| **分布式锁** | 全局唯一"占坑凭证" | 老派 Curator 锁（ZK）；K8s Lease 对象（etcd） |
| **元数据/配置存储** | 全集群共享的"小配置 + 事实状态" | K8s 全部资源对象（etcd）、Dubbo 老版注册中心（ZK） |
| **变更通知（Watch）** | 数据变了，订阅者秒级知道 | 配置中心鼻祖、服务发现 |

> **为什么这些场景需要 CP？** 因为"谁是主 / 锁归谁"这类答案**不允许出现两个真相**——宁可暂时不可用（选不出主），也不能脑裂。这与 Redis 缓存"宁可失效回源"的 AP 取舍正好是坐标系两端。

---

## 二、ZooKeeper：ZNode 树 + 临时节点 + Watcher

```text
/                     （根，类文件系统）
├── /aics/leader      临时节点：谁创建了它谁就是 leader，会话断→自动删除
├── /aics/locks/order-0001   临时顺序节点：锁排队
└── /aics/config      持久节点：配置数据（≤1MB，别放业务数据）
```

| 机制 | 说明 | 记忆点 |
|---|---|---|
| **ZNode** | 每个节点=路径+数据（默认 ≤1MB） | "小文件系统" |
| **临时节点（EPHEMERAL）** | 绑定客户端会话，**会话断→节点自动删** | 存活检测的本质：不是心跳改数据，而是"会话在，节点在" |
| **临时顺序节点** | 自动追加递增序号 | 排队叫号：序号最小者持锁，其余 Watch 前一个（**避免惊群**） |
| **Watcher** | 一次性触发：数据变更推通知，客户端收到后**重新注册** | 不是订阅流，是"响一次铃" |
| **ZAB 共识** | 类 Raft：过半写成功才算提交，读默认可能读到旧值（可 sync 强一致读） | CP 的代价：写入要过半数节点 |

**ZK 版分布式锁的正确姿势**（面试手写题常客）：

```text
1. 创建 /locks/order 的临时顺序子节点 → 得到 order-00000003
2. 判断自己是不是序号最小者：是 → 持锁；否 → Watch 比自己小的最近一个
3. 前一个被删（释放）→ Watch 触发 → 再判断 → 直到轮到自己
4. 释放 = 删除自己的节点；会话断开 = 自动释放（这就是比 Redisson"看门狗"更硬的保活）
```

---

## 三、etcd：Raft + MVCC + Watch 流 + Lease

| 机制 | 说明 | 与 K8s 的关系 |
|---|---|---|
| **KV + MVCC** | 每次 Put 产生新 revision（全局递增），可查历史版本 | `kubectl get -o yaml` 里的 `resourceVersion` |
| **Raft 共识** | Leader 选举 + 日志复制 + 过半提交 | etcd 集群 3/5 节点，写走 Leader |
| **Watch 流** | 按 revision 订阅**连续变更流**（不是一次性铃） | informer/list-watch 机制的地基（16-02 的"K8s 控制循环"靠它驱动） |
| **Lease 租约** | 客户端申请租约（TTL）+ 心跳续约；租约过期 → 挂在其上的 KV 自动删除 | K8s 的 `Node` 心跳、controller-manager 选主的 `Lease` 对象 |

> **ZK 临时节点 vs etcd Lease**：同一思想的两版实现——"**会话/租约在，数据在；断了，自动清**"。
> **ZK Watcher vs etcd Watch**：一次性铃 vs 连续流（etcd 更像你 Nacos gRPC 长连接推送的体验）。

---

## 四、ZK vs etcd vs Nacos：一张表收口

| 维度 | ZooKeeper | etcd | Nacos（本仓在用） |
|---|---|---|---|
| 共识 | ZAB（CP） | Raft（CP） | 持久实例 Raft（CP）/ 临时实例 Distro（**AP**） |
| 定位 | 通用协调 | **K8s 专用底座**+通用 | 注册中心 + 配置中心 |
| 数据模型 | ZNode 树 | 扁平 KV + MVCC | 服务/配置两域模型 |
| 通知 | Watcher（一次性） | Watch（revision 流） | UDP→gRPC 长连接推送 |
| 存活机制 | 会话+临时节点 | Lease 租约 | 心跳（1.x）/ 连接健康（2.x） |
| 典型用户 | Kafka 老版、HDFS、Dubbo 老版 | **K8s**、iTerm 类基础设施 | 本仓 11 个服务 |

> 记忆口径：**etcd 一定要会（K8s 的地基，16 模块多处引用）；ZK 要会原理与老系统迁移认知；Nacos 你已在用——把三者放到"CP/AP、推/拉、铃/流"三个轴上比较即可**。

---

## 五、AP 锁 vs CP 锁：把 Redisson 的账算清楚（高频追问）

| | Redisson（Redis，AP） | ZK/etcd 锁（CP） |
|---|---|---|
| 加锁速度 | 内存级，微秒~毫秒 | 过半共识写入，毫秒~十毫秒 |
| 极端风险 | 主从切换瞬间锁可能"双持"（看门狗续期也救不了已复制的丢失） | 拿到锁就是铁的；代价是 ZooKeeper 集群不可用时**无法加新锁** |
| 兜底 | 状态检查 + DB 唯一键终审（你 [02-Spring微服务/14](../02-Spring微服务/14-分布式幂等设计.md) 的三层幂等） | 无需兜底，但吞吐低 |
| 选型口径 | 高并发互斥"加速挡"，正确性交给 DB | 强正确性协调（选主、任务分片主控） |

> 本项目口径：**锁是性能优化（挡并发重复），权威在业务状态与唯一键**——所以选 Redisson 合理。如果哪天要做"调度器选主"这类"必须唯一"的活，再考虑 Lease 类方案（甚至直接用 K8s Lease 对象，零新增组件）。

---

## 六、动手实验（15 分钟）

```bash
# ZK：临时节点的"会话断即消失"
docker run -d --name zk -p 2181:2181 zookeeper:3.9
docker exec -it zk zkCli.sh
  create -e /aics/leader "node-1"      # -e 临时节点
  ls /aics
# 另开一个窗口 kill 客户端会话 → 回来看 /aics/leader 已自动消失

# etcd：租约 + watch 流
docker run -d --name etcd -p 2379:2379 \
  -e ALLOW_NONE_AUTHENTICATION=yes bitnami/etcd:3.5
etcdctl lease grant 10                 # 得到租约 ID
etcdctl put /aics/leader node-1 --lease=<ID>   # 绑定租约
etcdctl watch /aics/ --prefix          # 另一窗口 watch；10s 后看到 Delete 事件
```

---

## 七、面试要点总结

```text
关键词：
协调四场景：选主/锁/元数据/变更通知 · CP：宁可不可用不可脑裂
ZK：ZNode+临时节点(会话断自动删)+顺序节点排队(防惊群)+一次性 Watcher+ZAB
etcd：Raft+MVCC(revision)+Watch 流+Lease · K8s 唯一存储、resourceVersion/Lease 对象
AP/CP 锁账本：Redisson 快但极端双持→DB 终审；CP 锁硬但吞吐低
项目锚点：Redisson 看门狗（04-07）· Nacos Distro/Raft 双轨（02-03）· K8s Lease 选主可零组件替代
```

## 学习检查清单

- [ ] 能按"会话/租约在则数据在"讲清存活检测的本质
- [ ] 能手写 ZK 临时顺序节点锁的四步流程，并解释为什么 Watch 前一个而非全部
- [ ] 能对比 ZK Watcher 与 etcd Watch 的"铃 vs 流"
- [ ] 能把 Nacos 临时/持久实例映射到 AP/CP 两个象限
- [ ] 能为"调度器选主"给出一套零新增组件的方案（K8s Lease）

## 下一步

- [17-Caffeine 本地缓存与两级缓存](17-Caffeine本地缓存与两级缓存.md)：回到 AP 派的性能世界；
- [16-云原生与GitOps/02-K8s资源治理与弹性伸缩](../16-云原生与GitOps/02-K8s资源治理与弹性伸缩.md)：etcd 之上的控制循环。
