# 05-XXL-Job 分布式调度：定时任务的"集群化管理"

> 对应项目文件：`ai-cs-order/src/main/java/com/aics/order/task/`（3 个类）、`ai-cs-order/application.yml` 的 `aics.xxl-job` 配置、`deploy/mysql/xxl-job-init.sql`、`deploy/k8s/xxl-job.yaml`
> 业务背景：订单超时未支付要自动关单。这是个"永远要有人盯着"的活，本篇就从这个场景讲透分布式调度。

---

## 零、先定位：业务全景——订单超时关单的"三重保障"

```
用户下单（PENDING_PAY，15 分钟后过期）
   │
   ├─ 第一重【主路径】RocketMQ 延迟消息
   │    下单时发一条"15 分钟后投递"的消息 → 到点触发关单
   │    特点：精准、实时。风险：消息可能丢（broker 宕机等）
   │
   ├─ 第二重【本地兜底】@Scheduled 定时扫描（OrderTimeoutScheduler）
   │    每 5 分钟扫一遍 DB：status=PENDING_PAY 且 expireTime < now → 关单
   │    特点：不依赖任何外部组件，消息丢了也兜得住。
   │    风险：多实例部署时每个实例都会扫（重复执行）
   │
   └─ 第三重【集中调度】XXL-Job（本篇主角）
        admin 统一按 cron 派发 → 只派给一台执行器 → 同样跑这段扫描
        特点：看得见（执行日志看板）、控得住（改 cron 不重启、失败重试、告警）

三重保障执行的是【同一份业务逻辑 scanExpiredOrders()】，
靠订单状态机天然幂等：已关的订单再扫到也不会重复关 → 多通道并存无副作用
```

**为什么关单场景需要这么多保险？** 关单要回滚库存，漏关 = 库存被无效订单占死。金融/交易类系统对"必须执行"的任务都这么设计：主路径保实时，兜底路径保最终一致。

---

## 一、@Scheduled 的局限 vs XXL-Job 的解法

先看单机时代 `@Scheduled` 在集群环境下的尴尬：

```
order 服务部署 3 个实例（K8s replicas: 3）

  实例1 @Scheduled ──┐
  实例2 @Scheduled ──┼── 每 5 分钟，3 个实例同时扫库、同时关单
  实例3 @Scheduled ──┘    → 重复劳动、数据库压力×3、日志×3 难排查
                          → 想改扫描周期？改代码 + 重新发版 ×3
                          → 关单逻辑有 bug？看 3 份各自为政的日志
```

| 问题 | @Scheduled | XXL-Job |
|------|-----------|---------|
| 多实例重复执行 | ❌ 每个实例都跑（要自己加分布式锁） | ✅ 路由策略只派发到一台（FIRST/轮询/分片） |
| 失败感知 | ❌ 到日志里翻异常 | ✅ 执行日志看板 + 失败重试 + 告警邮件 |
| 动态调 cron | ❌ 改代码重启 | ✅ 控制台改，秒级生效 |
| 手动触发/补跑 | ❌ 只能等下一个周期 | ✅ 控制台"执行一次"按钮 |
| 任务依赖/编排 | ❌ | ✅ 子任务链 |

**本项目的并存策略**：不二选一——`@Scheduled` 保留为本地兜底（防 admin 单点挂掉后没人扫描），XXL-Job 作为集中调度主通道，两者跑同一份幂等逻辑。

---

## 二、核心概念与交互流程：admin 和 executor 是谁

XXL-Job 就两个角色，用"外卖平台"理解：

```
┌────────────────────────────────────────────────────────────┐
│  admin 调度中心（外卖平台）                                    │
│  独立部署的 web 应用（xxl-job-admin，含自己的 MySQL 库 xxl_job）│
│  职责：管任务定义（cron/路由策略）、到点派单、收结果、记日志      │
└──────────▲──────────────────────────────┬───────────────────┘
           │ ③ 回调：到点了，执行吧          │
           │    （admin → 执行器内嵌端口 9999）│
           │                                │ ① 注册：我在这
           │                                │    （appname=ai-cs-order + IP:9999）
┌──────────┴──────────────────────────────▼───────────────────┐
│  executor 执行器（骑手）—— 内嵌在业务服务里                     │
│  本项目 = ai-cs-order 服务里的 XxlJobSpringExecutor bean      │
│  职责：向 admin 报到、接单、调用 JobHandler、回报成败           │
└──────────────────────────────┬───────────────────────────────┘
                               │ ④ 执行
                     ┌─────────▼──────────┐
                     │ @XxlJob 注解的方法   │
                     │ orderTimeoutScanJob │
                     └────────────────────┘
```

一次完整调用的时序：

```
① ai-cs-order 启动（开关打开）→ 执行器向 admin 注册：appname=ai-cs-order, 地址=本机IP:9999
② admin 后台按任务配置的 cron（每 5 分钟）触发
③ admin 按路由策略选中一台执行器，HTTP 回调它的 9999 端口
④ 执行器调用 @XxlJob("orderTimeoutScanJob") 标注的方法
⑤ 方法跑完回报结果（XxlJobHelper.handleSuccess/handleFail）
⑥ admin 记录本次执行的日志、耗时、结果 → 控制台可查；失败可自动重试 + 告警
```

---

## 三、代码落点精读（3 个类，共不到 100 行）

```
ai-cs-order/src/main/java/com/aics/order/task/
├── XxlJobConfig.java            # 执行器装配（开关控制）
├── OrderTimeoutScanJob.java     # XXL-Job 入口（只做"调度入口"）
└── OrderTimeoutScheduler.java   # 真正的业务逻辑（双通道复用）
```

### 3.1 XxlJobConfig：一个值得学习的"开关式装配"

```java
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aics.xxl-job.enabled", havingValue = "true")  // ★ 关键注解
public class XxlJobConfig {

    @Value("${aics.xxl-job.admin-addresses}")
    private String adminAddresses;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);   // admin 地址
        executor.setAppname("ai-cs-order");           // 注册名（admin 后台按它分组）
        executor.setPort(executorPort);               // 内嵌 server 端口，admin 回调用
        executor.setAccessToken(accessToken);         // 通信令牌
        executor.setLogPath(...);  executor.setLogRetentionDays(7);
        return executor;
    }
}
```

**为什么要有开关？** 本地开发经常没有 admin，如果执行器无条件装配，要么启动报错、要么反复注册失败刷日志。`@ConditionalOnProperty` + 默认 `false`：**不开开关 = 退化成纯 @Scheduled 兜底，本地照常跑**。这个"可降级设计"比集成本身更值得学。

对应配置（`ai-cs-order/application.yml`）：

```yaml
aics:
  xxl-job:
    enabled: ${XXL_JOB_ENABLED:false}                      # 默认关，环境变量开
    admin-addresses: ${XXL_JOB_ADMIN:http://127.0.0.1:8099/xxl-job-admin}
    access-token: default_token
    executor:
      port: 9999
```

### 3.2 OrderTimeoutScanJob：入口薄、逻辑厚

```java
@XxlJob("orderTimeoutScanJob")                    // ← admin 控制台里填的 JobHandler 名
public void orderTimeoutScanJob() {
    log.info("XXL-Job: 执行订单超时扫描");
    orderTimeoutScheduler.scanExpiredOrders();    // ← 复用 @Scheduled 同一份逻辑
    XxlJobHelper.handleSuccess("订单超时扫描完成");  // ← 回报结果给 admin
}
```

### 3.3 OrderTimeoutScheduler：业务逻辑（两通道共享）

```java
@Scheduled(fixedDelay = 300000, initialDelay = 60000)   // 本地兜底：每 5 分钟
public void scanExpiredOrders() {
    // ① 查超时未支付订单：status = PENDING_PAY 且 expireTime < now
    List<Order> expired = orderMapper.selectList(...);
    // ② 逐单取消（内部回滚库存），单笔失败不影响其他订单
    for (Order order : expired) {
        try { orderService.cancelExpiredOrder(order.getOrderNo()); }
        catch (Exception e) { log.error(...); }         // 记录，下轮扫描自然重试
    }
}
```

**幂等怎么保证的？** `cancelExpiredOrder` 按订单状态机工作：只有 PENDING_PAY 才能流转到 CANCELLED。扫描重复跑、两通道同时跑，已关的单状态不对就不会被再次处理。**写定时任务先想幂等，是分布式调度的第一课。**

---

## 四、部署步骤：把 admin 跑起来并接上执行器

### 4.1 准备调度中心（二选一）

**方式 A：K8s 部署（推荐，和整套系统统一）**

```bash
# ① 建库：xxl_job 库 + 8 张表 + admin 账号
kubectl exec -it mysql-master-0 -n ai-customer-service -- \
  mysql -uroot -proot < deploy/mysql/xxl-job-init.sql   # 或在宿主机连库执行

# ② 准备 admin 连接参数（模板复制后填真实值）
cp deploy/k8s/middleware-secrets.example.yaml middleware-secrets.yaml
#   编辑 xxl-job-admin-config 这个 Secret：datasource 指向 mysql:3306/xxl_job + accessToken
kubectl apply -f middleware-secrets.yaml

# ③ 部署 admin（xuxueli/xxl-job-admin:2.4.1，参数从 Secret 注入）
kubectl apply -f deploy/k8s/xxl-job.yaml
```

**方式 B：本地 jar 跑**

```bash
# 官方仓库不发布可执行 jar，需要自己编译
git clone -b 2.4.1 https://github.com/xuxueli/xxl-job.git && cd xxl-job
# 改 xxl-job-admin/src/main/resources/application.properties：
#   spring.datasource.url=jdbc:mysql://<你的MySQL>:3306/xxl_job...
#   server.port=8099
mvn clean package -DskipTests -pl xxl-job-admin -am
java -jar xxl-job-admin/target/xxl-job-admin-2.4.1.jar
```

### 4.2 控制台配置（http://localhost:8099/xxl-job-admin，admin/123456）

```
① 执行器管理 → 新增
   AppName: ai-cs-order          ← 必须和代码里 executor.setAppname() 一致
   注册方式: 自动注册              ← 执行器上线后自动出现在节点列表

② 任务管理 → 新增
   执行器:      ai-cs-order
   JobHandler:  orderTimeoutScanJob     ← 和 @XxlJob 注解值一致
   调度类型:     CRON → 0 */5 * * * ?    ← 每 5 分钟
   路由策略:     FIRST（固定第一台）       ← 多实例时不重复执行的关键
   阻塞处理策略:  丢弃后续调度              ← 上一轮没跑完就跳过本轮（防止堆积）
   任务参数:     留空

③ 启动任务
```

### 4.3 打开执行器开关

```bash
# 给 order 服务设置环境变量后重启
XXL_JOB_ENABLED=true
# admin 控制台"执行器管理"里应出现 ai-cs-order 的在线节点
```

---

## 五、验证

**单元层**：`XxlJobConfigTest` 用 `ApplicationContextRunner` 验证条件装配——开关默认关闭时容器里没有 `xxlJobExecutor` bean，打开才创建（学条件装配的好例子）。

**集成层**（三步，由表及里）：

```
① admin 控制台 → 执行器管理 → ai-cs-order 显示"在线"（注册成功）
② 控制台 → 任务管理 → 选中任务 → "执行一次" → 执行日志里看到
   "XXL-Job: 执行订单超时扫描"（调度链路通）
③ 造一笔过期订单（把 expire_time 改到过去）→ 执行一次 → 订单变 CANCELLED（业务生效）
```

---

## 六、面试高频

1. **避免重复执行的三个层次**：调度中心路由（架构层：只派一台）→ 分布式锁（应用层：抢到锁才跑）→ 业务幂等（数据层：重复执行也无副作用）。层层兜底，缺一层都可能出事。
2. **分片广播**：路由策略选 SHARDING_BROADCAST 时，所有执行器同时收到任务，各自拿到 `shardIndex/shardTotal` 参数——比如 10 万笔超时订单按订单号 hash 分给 3 个实例并行扫。FIRST 是"一台干"，分片是"一起干"。
3. **调度过期策略（misfire）**：admin 重启错过调度点时，选"立即补偿一次"还是"忽略"。超时关单类任务选 DO_NOTHING——下一轮 5 分钟扫描自然覆盖，补偿反而可能造成执行风暴。
4. **XXL-Job vs Quartz vs Spring Task**：@Scheduled 无中心（单机）；Quartz 有集群能力但要自己管表和故障转移，无界面；XXL-Job 自带控制台、分片、日志、告警，运维成本低，是国内主流。

---

## 动手练习

1. 本地只跑 order 服务（不开开关），观察日志里每 5 分钟出现"定时任务：扫描超时未支付订单"——这就是 @Scheduled 兜底在工作
2. 把系统时间视角换成数据视角：手工把一笔测试订单的 `expire_time` 改到 10 分钟前，等下一轮扫描，确认订单被关闭、库存被回滚
3. 按第四节把 admin 部署起来，打开 `XXL_JOB_ENABLED=true`，在控制台"执行一次"触发同样逻辑
4. 同时开两个 order 实例（本地不同端口），观察 @Scheduled 版两边都扫、XXL-Job 版只有路由选中的那台执行——亲手复现第一节的对比图
5. 思考题：如果把路由策略改成"轮询"，扫描任务的行为有什么变化？改成"分片广播"还需要改代码吗？（提示：`XxlJobHelper.getShardIndex()`）

---

## 学习检查清单

- [ ] 能画出订单超时关单"RocketMQ 延迟消息 + @Scheduled + XXL-Job"三重保障全景图
- [ ] 理解 admin / executor / JobHandler 三个概念及注册→调度→回调→回报的完整时序
- [ ] 理解 @ConditionalOnProperty 开关式装配的可降级设计（本地无 admin 也能跑）
- [ ] 知道幂等为什么是定时任务的前提，以及本项目靠订单状态机保证幂等
- [ ] 会在 admin 控制台配置执行器和任务（AppName、JobHandler、路由策略、阻塞策略）
- [ ] 理解路由策略 FIRST / 轮询 / 分片广播的区别和适用场景
- [ ] 说出避免任务重复执行的三个层次

---

## 下一步

→ [06-优雅停机与零丢失滚动更新](./06-优雅停机与零丢失滚动更新.md)（任务正在跑时实例被杀怎么办——运维篇收官）
