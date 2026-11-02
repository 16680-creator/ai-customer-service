# 01-Java基础

> 面向企业应用开发的 Java 高阶知识补全。不讲 HelloWorld，讲生产环境的每一个细节决策。

---

## 📚 章节导航

| 章节 | 核心内容 | 难度 | 适用场景 |
|------|---------|------|---------|
| [01-Java17核心特性](01-Java17核心特性.md) | Record / Sealed / Pattern Matching / Virtual Thread | ⭐⭐⭐ | 现代Java应用开发 |
| [02-Maven多模块管理](02-Maven多模块管理.md) | 依赖管理 / 多模块聚合 / BOM / 版本管理 | ⭐⭐ | 大型项目工程化 |
| [03-JVM内存模型与GC实战](03-JVM内存模型与GC实战.md) | 堆结构 / GC 算法 / 调优参数 / 故障排查 | ⭐⭐⭐⭐ | 生产环保 OOM 诊断 |
| [04-并发编程进阶](04-并发编程进阶.md) | ThreadPool / Lock / Atomic / ConcurrentHashMap | ⭐⭐⭐ | 高并发系统设计 |
| [05-集合框架与源码](05-集合框架与源码.md) | ArrayList / HashMap / TreeMap / 源码分析 | ⭐⭐⭐ | 性能瓶颈定位 |
| [06-反射动态代理与SPI](06-反射动态代理与SPI.md) | Reflection / Proxy / SPI / 类加载 | ⭐⭐⭐ | 框架开发必备 |
| [07-IO模型与Netty基础](07-IO模型与Netty基础.md) | NIO / BIO / Netty / 异步网络编程 | ⭐⭐⭐⭐ | 网关 / 中间件开发 |
| [08-语言级细节与序列化](08-语言级细节与序列化.md) | 浮点精度 / 序列化兼容 / ClassLoader / 性能 | ⭐⭐⭐ | 故障排查 / 优化 |

---

## 🎯 学习路线

### 快速路线（1周）
```
01 → 04 → 05 → 02
核心语言特性 → 并发 → 集合 → 工程化
```

### 深度路线（2周）
```
01 → 04 → 05 → 06 → 03 → 07 → 08 → 02
完整语言 → 并发基础 → 容器 → 反射代理 → GC调优 → 网络IO → 细节 → 工程化
```

### 专项提升（场景化）

**性能优化方向**
- 05 集合框架（数据结构选择）
- 03 GC 实战（JVM 调优）
- 04 并发编程（线程池配置）

**框架开发方向**
- 06 反射动态代理（AOP 基础）
- 07 IO 模型（网络框架）
- 02 Maven 多模块（项目架构）

**高可用方向**
- 03 GC 与内存（OOM 预防）
- 04 并发编程（死锁 / 竞态条件）
- 08 序列化（分布式通信）

---

## 💡 核心知识点速查

### Java 17 新增
- **Record** 替代 Lombok @Data（参考 01 篇）
- **Sealed Classes** 受限继承（参考 01 篇）
- **Pattern Matching** 模式匹配（参考 01 篇）
- **Virtual Thread** 虚拟线程（参考 04 篇）

### 容器性能对比
| 操作 | ArrayList | LinkedList | HashMap | ConcurrentHashMap |
|------|-----------|------------|---------|------------------|
| 随机访问 | O(1) | O(n) | O(1) | O(1) |
| 插入 | O(n) | O(1) | O(1)* | O(1)* |
| 删除 | O(n) | O(1) | O(1)* | O(1)* |
| 线程安全 | ❌ | ❌ | ❌ | ✅ |

### GC 调优三步
1. **监控** — 用 `jstat` 看 GC 频率与暂停时间
2. **分析** — 堆大小、年轻代占比、GC 算法选择
3. **验证** — 压测前后数据对比（参考 03 篇）

### 并发 Bug 速查
| 现象 | 可能原因 | 验证方法 |
|------|---------|---------|
| 请求偶尔丢失 | 线程安全问题 / 竞态条件 | jstack 看线程状态 |
| CPU 100% | 忙轮询 / 死循环 / GC 压力 | jps / jstat / 火焰图 |
| OOM | 堆溢出 / 内存泄漏 | jmap dump + MAT 分析 |

---

## 📖 典型问题与解法

### Q1：如何优化 HashMap 性能？
**场景**：高并发读写导致 CPU 飙升

**解法**（参考 05 篇）：
1. 检查 loadFactor（默认 0.75 可能过高）
2. 初始化时指定容量避免扩容
3. 使用 ConcurrentHashMap 替代 synchronized map
4. 考虑 segment 级别的锁粒度

### Q2：GC Pause Time 超过 SLA？
**场景**：CMS/G1 Full GC 导致 1-2 秒卡顿

**解法**（参考 03 篇）：
1. 用 `-XX:+PrintGCDetails` 看具体原因
2. 调整年轻代大小减少 Full GC 频率
3. 考虑 ZGC / Shenandoah（低延迟 GC）
4. 压测验证（参考 12-性能工程）

### Q3：如何设计线程池大小？
**场景**：CPU 密集 vs IO 密集需要不同的配置

**解法**（参考 04 篇）：
- **CPU 密集**：`核心数 + 1`
- **IO 密集**：`核心数 × 2-4`
- **混合**：监控调整

---

## 🔧 实战练习

### 练习 1：容器性能测试（30 min）
编写基准测试对比 ArrayList vs LinkedList 的性能

```bash
# 运行
mvn clean test -Dtest=ArrayListBenchmark

# 对标 22-Python速成/17 篇的基准测试方法论
```

### 练习 2：GC 日志分析（45 min）
启用 GC 日志，用 jstat 监控，找出瓶颈

```bash
# 启动 JVM
java -XX:+PrintGCDetails -Xmx1g YourApp

# 监控 GC
jstat -gc -h10 <pid> 1000   # 每秒打印一次
```

### 练习 3：线程安全验证（1 hour）
用 JCStress / ThreadSanitizer 检测数据竞态条件

---

## 📊 与其他模块的关联

```
01-Java基础
  ├── → 02-Spring微服务（Spring 内部大量用到反射/动态代理）
  ├── → 03-数据库与ORM（JDBC / Mybatis 连接池线程模型）
  ├── → 04-中间件（Redis/RocketMQ 客户端并发）
  ├── → 07-运维部署（JVM 启动参数调优）
  └── → 12-性能工程（基准测试方法论）
```

---

## ⚡ 快速查询

### 遇到的问题直接查

- **"请求偶尔报错"** → 04 篇 线程安全
- **"服务内存慢增长"** → 03 篇 GC / 内存泄漏
- **"某个接口特别慢"** → 05 篇 容器选择 / 12 篇 基准测试
- **"Spring AOP 不生效"** → 06 篇 动态代理原理
- **"Netty 应用卡顿"** → 07 篇 NIO / EventLoop 模型
- **"JSON 序列化报错"** → 08 篇 序列化兼容性

---

## 📚 推荐阅读顺序

**第一遍（建立认知）**
```
01 Java 特性 → 02 Maven 工程化 → 04 并发基础 → 05 容器
```

**第二遍（深度理解）**
```
03 GC 调优 → 06 动态代理 → 07 网络 IO → 08 细节
```

**第三遍（实战验证）**
```
写基准测试 → GC 日志分析 → 线程竞态检测 → 性能优化
```

---

**相关学习资源：**
- [JCIP - Java Concurrency In Practice](https://jcip.net/)
- [深入理解 JVM](https://book.douban.com/subject/24722612/)
- [Effective Java](https://book.douban.com/subject/27047716/)
- [22-Python速成 的 Java 对照视角](../22-Python速成/README.md)
