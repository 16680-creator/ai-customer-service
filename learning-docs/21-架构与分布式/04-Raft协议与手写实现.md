# 04 - Raft 协议与手写实现

> **目标**：能画图讲清 Raft 的选主与日志复制，能手写一个能跑通选主 + 复制的 mini-raft。
> **前提**：[03 分布式一致性理论](03-分布式一致性理论.md)。
> **纪律**：先跑通"选主 + 日志复制"，再加持久化/快照/成员变更；不要一上来写完整版。

---

## 一、为什么是 Raft

Paxos 理论强但难懂难实现。Raft 做了三件事：

1. **强 Leader**：所有写都走 Leader，简化复制；
2. **任期（Term）**：用单调递增的任期解决"旧 Leader"问题；
3. **可理解的子问题拆分**：选主、日志复制、安全性。

> etcd、Consul、TiKV、Nacos（部分）都用 Raft 或其变体。

---

## 二、角色与任期

```text
Follower ──(选举超时)──> Candidate ──(多数票)──> Leader
   ↑                                          │
   └──────────(发现更高任期/心跳)──────────────┘
```

| 角色 | 职责 |
|---|---|
| **Follower** | 被动接收心跳/日志；超时未收到心跳则发起选举 |
| **Candidate** | 拉票；拿到多数票成为 Leader |
| **Leader** | 处理写请求、复制日志、发心跳 |

**Term**：单调递增的整数。规则：

- 收到更高 term → 立刻转为 Follower；
- 每个 term 最多一个 Leader；
- 投票时 term 必须匹配。

---

## 三、选主

### 3.1 流程

```text
1. Follower 在 [electionTimeout, 2×electionTimeout] 内没收到心跳 → 转 Candidate
2. term++，投自己，向所有节点发 RequestVote(term, lastLogIndex, lastLogTerm)
3. 收到多数票 → 成为 Leader，立即发心跳
4. 收到更高 term 的请求 → 退回 Follower
```

### 3.2 投票规则（关键）

节点 A 投票给 B 的条件：

1. B 的 term ≥ A 的 term；
2. A 在这个 term 还没投过票；
3. **B 的日志至少和 A 一样新**（`lastLogTerm` 更大，或 term 相同但 `lastLogIndex` 更大）。

> 第 3 条是安全性核心：防止"日志落后的节点"当 Leader 覆盖已提交日志。

### 3.3 超时设置

- 心跳间隔：50~100ms；
- 选举超时：150~300ms，**随机化**（避免同时选举导致分裂票）。

---

## 四、日志复制

### 4.1 流程

```text
1. Client 发命令给 Leader
2. Leader 追加到本地日志（未提交）
3. 并行发 AppendEntries(prevLogIndex, prevLogTerm, entries, leaderCommit)
4. Follower 校验 prevLog 匹配 → 追加 → 回复成功
5. Leader 收到多数成功 → commitIndex 前移 → 应用到状态机 → 回复 Client
6. 下次心跳把 commitIndex 带给 Follower，Follower 也应用
```

### 4.2 关键索引

| 变量 | 含义 |
|---|---|
| `nextIndex[i]` | 下一个要发给 follower i 的日志位置 |
| `matchIndex[i]` | 已知 follower i 已复制的最高位置 |
| `commitIndex` | 已提交的最高位置（多数派已复制） |
| `lastApplied` | 已应用到状态机的位置 |

**冲突处理**：Follower 的 `prevLogTerm` 不匹配 → 拒绝；Leader 递减 `nextIndex` 重试，直到找到共同前缀。

### 4.3 提交规则（安全）

Leader 只能提交**当前 term** 的日志（通过多数派复制）。不能直接提交旧 term 的日志——否则可能违反一致性。

---

## 五、安全性三条规则

1. **选举限制**：只有日志足够新的节点才能当选（见 3.2）。
2. **日志匹配**：相同 index + term 的日志，内容一定相同，且之前的所有日志也相同。
3. **提交规则**：只能通过当前 term 的多数派复制来提交。

> 这三条保证 **已提交的日志不会被覆盖/丢失**。

---

## 六、持久化

每个节点必须持久化（先落盘再回复）：

| 字段 | 作用 |
|---|---|
| `currentTerm` | 防止重启后重复投票 |
| `votedFor` | 本 term 投给了谁 |
| `log[]` | 日志条目 |

**易失状态**（可重建）：`commitIndex`、`lastApplied`、`nextIndex`、`matchIndex`。

---

## 七、成员变更与快照

### 7.1 成员变更

不要一次性改多个节点（可能选出两个 Leader）。用**单节点变更**：一次只加/减一个，多数派在新旧配置间过渡。

### 7.2 快照

日志无限增长会爆盘。做法：

- 状态机定期做快照（含 `lastIncludedIndex/Term`）；
- 丢弃快照之前的日志；
- 落后太多的 Follower 用 `InstallSnapshot` 追赶。

---

## 八、手写路线（建议 4 周）

| 周 | 目标 | 验收 |
|---|---|---|
| 1 | 状态机 + RPC 骨架 + 选主 | 3 节点能选出 Leader，kill Leader 能重选 |
| 2 | 日志复制 + commitIndex | 客户端写能复制到多数派并应用 |
| 3 | 持久化 + 重启恢复 | 重启后 term/votedFor/log 不丢 |
| 4 | 快照 + 测试（分区/延迟） | 网络分区恢复后日志一致 |

---

## 九、Java 代码骨架

### 9.1 节点状态

```java
public class RaftNode {
    enum Role { FOLLOWER, CANDIDATE, LEADER }

    private Role role = Role.FOLLOWER;
    private long currentTerm = 0;
    private Long votedFor = null;
    private final List<LogEntry> log = new ArrayList<>();
    private long commitIndex = 0;
    private long lastApplied = 0;

    private final Map<Long, Long> nextIndex = new HashMap<>();
    private final Map<Long, Long> matchIndex = new HashMap<>();

    private final RaftStorage storage;   // 持久化
    private final StateMachine stateMachine;
    private final RpcClient rpc;
}
```

### 9.2 选主（伪代码 → 可直接翻译）

```java
void onElectionTimeout() {
    role = Role.CANDIDATE;
    currentTerm++;
    votedFor = selfId;
    storage.save(currentTerm, votedFor, log);

    int votes = 1;
    for (Node peer : peers) {
        VoteResponse r = rpc.requestVote(peer,
            new VoteRequest(currentTerm, lastLogIndex(), lastLogTerm()));
        if (r.term() > currentTerm) { becomeFollower(r.term()); return; }
        if (r.voteGranted()) votes++;
    }
    if (votes > peers.size() / 2) becomeLeader();
}

void becomeLeader() {
    role = Role.LEADER;
    for (Node peer : peers) { nextIndex.put(peer.id(), log.size() + 1); matchIndex.put(peer.id(), 0L); }
    sendHeartbeat();
}
```

### 9.3 日志复制（Leader 侧）

```java
void appendEntries(Node peer) {
    long next = nextIndex.get(peer.id());
    long prevIndex = next - 1;
    long prevTerm = prevIndex == 0 ? 0 : log.get((int) prevIndex - 1).term();
    List<LogEntry> entries = log.subList((int) (next - 1), log.size());

    AppendResponse r = rpc.appendEntries(peer,
        new AppendRequest(currentTerm, prevIndex, prevTerm, entries, commitIndex));

    if (r.success()) {
        matchIndex.put(peer.id(), next + entries.size() - 1);
        advanceCommitIndex();
    } else {
        nextIndex.put(peer.id(), Math.max(1, next - 1));  // 回退重试
    }
}
```

### 9.4 提交与应用

```java
void advanceCommitIndex() {
    for (long n = log.size(); n > commitIndex; n--) {
        if (log.get((int) n - 1).term() != currentTerm) continue;
        int replicated = 1; // 自己
        for (Long m : matchIndex.values()) if (m >= n) replicated++;
        if (replicated > peers.size() / 2) { commitIndex = n; applyCommitted(); break; }
    }
}

void applyCommitted() {
    while (lastApplied < commitIndex) {
        stateMachine.apply(log.get((int) lastApplied));
        lastApplied++;
    }
}
```

---

## 十、测试方案（最关键）

用 `MockRpc` 模拟网络，做四类测试：

| 测试 | 做法 | 预期 |
|---|---|---|
| 正常选主 | 3 节点启动 | 1 个 Leader，2 个 Follower |
| Leader 宕机 | kill Leader | 剩余节点选出新 Leader |
| 网络分区 | 隔离 Leader | 多数派侧选新 Leader；恢复后旧 Leader 退位 |
| 日志一致性 | 并发写 + 随机宕机 | 所有存活节点日志前缀一致，已提交不丢 |

**可用工具**：JUnit + 内存 RPC；进阶用 `jepsen`（了解即可）。

---

## 十一、常见坑

| 坑 | 现象 | 解法 |
|---|---|---|
| 选举超时太短 | 频繁重选 | 随机化 + 大于心跳 3 倍 |
| 心跳与 AppendEntries 混用 | 日志无法及时复制 | 心跳带空 entries，也走 AppendEntries |
| 提交旧 term 日志 | 已提交日志被覆盖 | 只提交当前 term |
| 重启后 votedFor 丢失 | 一个 term 投两票 | 先持久化再回复 |
| 未处理 term 更高的请求 | 脑裂 | 任何请求 term 更高 → 退回 Follower |
| nextIndex 回退太慢 | 复制卡住 | 可用冲突 term 快速定位 |

---

## 十二、面试题

| 题 | 答法骨架 |
|---|---|
| Raft 怎么选主？ | 随机超时 → term++ → 拉票 → 多数派；日志新才能当选 |
| 为什么随机超时？ | 避免同时选举导致分裂票 |
| 日志怎么复制？ | AppendEntries 带 prevIndex/prevTerm；多数派确认后提交 |
| 怎么保证不丢已提交日志？ | 选举限制 + 日志匹配 + 只提交当前 term |
| 脑裂怎么避免？ | 多数派才能当选；term 更高就退位 |
| 快照解决什么？ | 日志无限增长；InstallSnapshot 追赶 |
| Raft 和 Paxos 区别？ | Raft 强 Leader、更易实现；Paxos 更通用但难懂 |

---

## 附：推荐实现顺序

```text
1. 单机状态机 + 日志（无网络）
2. 内存 RPC + 选主
3. 日志复制 + 提交
4. 持久化 + 重启
5. 快照
6. 成员变更
```

> 写到第 3 步就足够在面试里讲清 Raft；第 4~6 步是加分项。
