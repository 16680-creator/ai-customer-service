# 05-XXL-Job 分布式调度

> 2026-08 落地记录：order 服务接入 XXL-Job 执行器（订单超时扫描 JobHandler），
> 调度中心（admin）部署脚本与步骤就绪；执行器默认关闭可平滑启用。

## 一、@Scheduled 的局限与 XXL-Job 的解法

| 问题 | @Scheduled | XXL-Job |
|---|---|---|
| 多实例重复执行 | ❌ 每个实例都跑（需自己加分布式锁） | ✅ 路由策略（FIRST/轮询/分片）只派发到一台 |
| 失败感知 | ❌ 日志里翻异常 | ✅ 执行日志看板 + 失败重试 + 邮件告警 |
| 动态调整 cron | ❌ 改代码重启 | ✅ 控制台改，秒级生效 |
| 手动触发/补跑 | ❌ | ✅ 控制台一键执行一次 |

**并存策略（本项目）**：`@Scheduled` 保留为本地兜底（防 admin 单点），
XXL-Job 作为集中调度主通道，两者执行同一份**幂等**的扫描逻辑（按订单状态机天然防重）。

## 二、执行器侧代码落点（已完成）

```
ai-cs-order/
├── pom.xml                                # xxl-job-core 2.4.1
└── src/main/java/com/aics/order/task/
    ├── XxlJobConfig.java                  # @ConditionalOnProperty 开关，默认关闭
    ├── OrderTimeoutScanJob.java           # @XxlJob("orderTimeoutScanJob") 入口
    └── OrderTimeoutScheduler.java         # 业务逻辑（双通道复用）
```

配置（application.yml）：

```yaml
aics:
  xxl-job:
    enabled: ${XXL_JOB_ENABLED:false}      # 打开开关需要 admin 已就绪
    admin-addresses: ${XXL_JOB_ADMIN:http://127.0.0.1:8099/xxl-job-admin}
    access-token: default_token
    executor:
      port: 9999                           # 执行器内嵌 server 端口（admin 回调用）
```

## 三、调度中心（admin）部署步骤

1. **建库**：执行 `deploy/mysql/xxl-job-init.sql`（已建好 `xxl_job` 库 + 8 张表 + admin 账号）
2. **编译 admin**（官方源码不含可执行 jar）：

```bash
git clone -b 2.4.1 https://github.com/xuxueli/xxl-job.git
cd xxl-job
# 修改 xxl-job-admin/src/main/resources/application.properties：
#   spring.datasource.url=jdbc:mysql://123.60.31.79:3306/xxl_job...
#   server.port=8099
mvn clean package -DskipTests -pl xxl-job-admin -am
java -jar xxl-job-admin/target/xxl-job-admin-2.4.1.jar
```

3. **控制台**：http://localhost:8099/xxl-job-admin（admin / 123456）
4. **配置任务**：
   - 执行器管理 → 新增（AppName=`ai-cs-order`，自动注册）
   - 任务管理 → 新增（JobHandler=`orderTimeoutScanJob`，cron `0 */5 * * * ?`，
     路由策略 FIRST，阻塞策略丢弃后续调度）
5. **打开开关重启 order**：`XXL_JOB_ENABLED=true`

## 四、验证方式

- 单测：`XxlJobConfigTest`（ApplicationContextRunner 验证开关条件装配——默认关闭不装配，
  打开才创建 `xxlJobExecutor` bean）
- 集成验证：admin 启动后执行器列表出现 `ai-cs-order`（自动注册），
  控制台「执行一次」触发扫描，执行日志可在看板查看

## 五、面试高频

1. **避免重复执行的三个层次**：调度中心路由（架构层）、分布式锁（应用层）、业务幂等（数据层）——层层兜底。
2. **分片广播**：`shardIndex/shardTotal` 参数把任务拆给多实例并行（如按订单号 hash 分片扫描），与单机 FIRST 的区别。
3. **调度过期策略**：misfire 时「立即补偿一次」还是「忽略」，对超时关单类任务选 DO_NOTHING（下次扫描自然覆盖）。
