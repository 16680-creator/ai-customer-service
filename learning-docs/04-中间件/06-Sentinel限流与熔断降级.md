# 06-Sentinel 限流与熔断降级

> 2026-08 落地记录：chat 服务接入 Sentinel，AI 对话接口入口限流（WarmUp 预热 + QPS 流控）。

## 一、为什么需要 Sentinel（与 Resilience4j 的分工）

| 维度 | Sentinel（SCA） | Resilience4j（本网关/chat 原有） |
|---|---|---|
| 定位 | **入口**流控、热点参数、系统自适应保护 | 调**下游**时的熔断/超时/重试 |
| 隔离粒度 | QPS / 线程数 / 关联资源 | 循环断路器（错误率/慢调用比例） |
| 规则来源 | Dashboard / Nacos 数据源 / 代码注册 | 注解 + 配置文件 |
| 本项目分工 | 限「多少请求能进 AI 对话」 | 限「LLM 供应商抖动时怎么自保」 |

面试高频：**两者能并存吗？** 能——入口限流和出口熔断是两层不同的保护，本项目 chat 同时使用。

## 二、代码落点

```
ai-cs-chat/
├── pom.xml                                    # spring-cloud-starter-alibaba-sentinel（SCA BOM 管版本）
└── src/main/java/com/aics/chat/config/
    ├── SentinelRules.java                     # 资源名/阈值常量（单一来源）
    └── SentinelFlowConfig.java                # @PostConstruct 注册 FlowRule
ai-cs-chat/.../controller/ChatController.java  # @SentinelResource + blockHandler
```

核心三步：

1. **资源声明**：`@SentinelResource(value = "chat-send", blockHandler = "chatSendBlocked")`
2. **规则注册**：`FlowRuleManager.loadRules(List.of(sendRule, ragRule))`
   - `chat-send`：QPS ≤ 10，`CONTROL_BEHAVIOR_WARM_UP` 预热 10s（冷启动慢慢放量，保护 LLM 连接池）
   - `chat-rag`：QPS ≤ 5（检索 + 生成链路更重）
3. **限流响应**：blockHandler 方法签名 = 原方法签名 + 末尾 `BlockException`，返回
   `Result.fail(ResultCode.TOO_MANY_REQUESTS, ...)`（429 语义）而非 500

## 三、规则来源的三种模式（演进路线）

| 模式 | 特点 | 适用 |
|---|---|---|
| 代码注册（当前） | 随应用发布，重启即生效，无外部依赖 | 兜底规则 |
| Dashboard 推送 | 内存态，控制台实时调整，重启丢失 | 调试/演练 |
| Nacos 数据源 | 规则持久化 + 全局推送（`sentinel-datasource-nacos`） | 生产推荐 |

## 四、验证方式

- 单测：`SentinelFlowConfigTest`（规则数量/资源名/阈值/预热效果断言）
- 启动冒烟：服务日志出现 `Sentinel 流控规则已注册`；压测 `/chat/send` 超过 QPS 后返回 429 业务码

## 五、踩坑与要点

1. `@SentinelResource` 由 Sentinel 的 AOP（`SentinelResourceAspect`）织入，需引入 starter 自带切面；**资源名不要与 URL 拦截的资源重复**，否则限流统计分裂。
2. blockHandler 与原方法必须在同一个类里（或用 `blockHandlerClass` 指定外部类）。
3. Sentinel Dashboard 只是「看 + 推规则」，不是必需组件；无 Dashboard 时客户端照样限流。
