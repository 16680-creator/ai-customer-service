# Spring 与微服务（02 模块）

> 本专题是 `learning-docs` 的**第二个模块**，覆盖本项目全部 Spring 技术栈：Spring Boot 3.2.5（本体+自动装配）、Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0（Nacos/Gateway/Seata/Sentinel）、Spring AI 1.1.4、Spring Security、Spring StateMachine、STOMP。
> 写法与全库一致：**每篇都锚定项目真实源码/配置**（给出 `文件:行号`），未落地主题显式标注；不写"通用教程"。
>
> 2026-09 补全：本模块此前 14 篇偏"使用 + 落地记录"，**缺 Spring Framework 本体原理（IoC/AOP/事务/MVC）**——本批新增 15~18 四篇 + 本 README，缺口分析见 [00-学习路线总览/05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md)。

---

## 一、文档地图

### A. 入门与架构（01~05）

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [01-SpringBoot核心原理](01-SpringBoot核心原理.md) | 注解、分层架构、依赖注入三种方式、自动配置简化版、配置文件优先级 | **新手第一篇**：先跑通"一个注解起服务" | ★ |
| [02-SpringCloud微服务架构](02-SpringCloud微服务架构.md) | 单体 vs 微服务、Nacos 注册中心、Gateway、OpenFeign、Sentinel、配置中心 | 微服务全景图 | ★★ |
| [03-Nacos注册与配置中心](03-Nacos注册与配置中心.md) | 服务注册发现、配置管理、动态刷新 | 服务发现的地基 | ★★ |
| [04-SpringCloudGateway网关](04-SpringCloudGateway网关.md) | 路由断言、WebFlux 过滤器、鉴权过滤器、CORS、限流 | 唯一入口的设计与实现 | ★★★ |
| [05-SpringDoc接口文档](05-SpringDoc接口文档.md) | OpenAPI 3 注解、Swagger UI、网关聚合 | 接口即文档 | ★ |

### B. 落地组件与一致性（06~14）

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [06-Seata分布式事务AT模式](06-Seata分布式事务AT模式.md) | 全局事务、undo_log 前后镜像、TC/TM/RM | **跨服务一致性核心** | ★★★★ |
| [07-服务调用统一与SeataXID传播](07-服务调用统一与SeataXID传播.md) | RestTemplate/Feign 统一、XID 透传、正确性缺陷修复 | 分布式事务的"连通性" | ★★★ |
| [08-Feign熔断与降级语义](08-Feign熔断与降级语义.md) | 超时/重试/降级、Reactor 边界 | 让远程调用"死得优雅" | ★★★ |
| [09-SpringCache与事务领域事件](09-SpringCache与事务领域事件.md) | `@Cacheable` 分层 TTL、`@TransactionalEventListener` 四 phase | **声明式缓存 + 进程内事件** | ★★★ |
| [10-自定义Starter与自动装配](10-自定义Starter与自动装配.md) | `AutoConfiguration.imports`、条件注解、`ApplicationContextRunner` 测试 | 把 common 变成 Starter | ★★★ |
| [11-STOMP实时通知与用户目的地](11-STOMP实时通知与用户目的地.md) | `@EnableWebSocketMessageBroker`、CONNECT 鉴权、user destination | 实时推送的正确姿势 | ★★★ |
| [12-订单状态机治理](12-订单状态机治理.md) | States/Events/Guards/Actions、为什么不用内存持久化 | 状态流转的集中治理 | ★★★★ |
| [13-支付渠道集成与回调一致性](13-支付渠道集成与回调一致性.md) | 策略+工厂四渠道、回调验签、事务消息、对账 | 支付全链路 | ★★★★ |
| [14-分布式幂等设计](14-分布式幂等设计.md) | `@Idempotent` 组件（AOP + Redis SET NX）、三层幂等 | 有代码、有测试的框架化实例 | ★★★★ |

### C. Spring Framework 本体原理（15~17，2026-09 新增）

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [15-SpringAOP与声明式事务原理](15-SpringAOP与声明式事务原理.md) | 代理模型、五种通知、JDK vs CGLIB、`@Transactional` 原理与失效七查、注解叠加顺序 | **面试重灾区**：解释一切"注解为什么不生效" | ★★★★ |
| [16-IoC容器与Bean生命周期](16-IoC容器与Bean生命周期.md) | `refresh()` 六阶段、Bean 八步生命周期、构造器注入、三级缓存、自动装配条件四件套、`@ConfigurationProperties` | 容器视角的"谁 new 的对象" | ★★★★ |
| [17-SpringMVC请求全链路与参数校验](17-SpringMVC请求全链路与参数校验.md) | 九站链路、四取数注解、`@Valid` 链路、Filter/Interceptor/AOP 对比、SSE 特殊路径 | 一次请求的完整旅程 | ★★★ |

### D. 验收出口（18）

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [18-Spring面试专练与追问链](18-Spring面试专练与追问链.md) | 四层答题模板、6 组高频题、4 条追问链、考前速览卡 | 串联 01~17 的面试答卷 | ★★★ |

## 二、与其他模块的分工边界（查重声明）

| 主题 | 在哪里讲 | 本模块讲不讲 |
|---|---|---|
| JDK 动态代理 / CGLIB 的 **Java 语言层**原理 | [01-Java基础/06-反射动态代理与SPI](../01-Java基础/06-反射动态代理与SPI.md) | 15 篇只讲"Spring 如何用代理"（AOP 织入、事务失效） |
| MySQL 事务（MVCC/锁/三大日志） | [03-数据库与ORM/06](../03-数据库与ORM/06-事务进阶-MVCC实现与三大日志.md) | 15 篇只讲 **Spring 事务管理**（注解/AOP/传播），DB 原理链接过去 |
| Spring AI / RAG / 向量库 | [05-AI集成](../05-AI集成/01-SpringAI框架集成/01-SpringAI入门.md)（Spring AI + RAG + 向量库，51 篇） | 不讲；15~17 只引用 chat 的 AOP/拦截器/线程池锚点 |
| Spring Security 落地细节 | [09-安全与设计模式/03](../09-安全与设计模式/03-SpringSecurity微服务两层安全模型.md) | 18 篇只做面试口径，机制不重复 |
| JUnit5 / Mockito / Testcontainers / BDD | [08-测试/01](../08-测试/01-JUnit5与Mockito单元测试.md)~[05](../08-测试/05-性能压测实战-k6与JMeter.md) | 不讲；16 篇只讲"怎么测自动装配"（`ApplicationContextRunner`） |
| 线程池调优 / 背压 | [12-性能工程/07-线程池调优与背压](../12-性能工程/07-线程池调优与背压.md) | 16/18 篇只讲 Spring 侧（`ThreadPoolTaskExecutor` Bean、`@Scheduled` 单线程陷阱） |
| XXL-Job 分布式调度 | [07-运维部署/05-XXL-Job分布式调度](../07-运维部署/05-XXL-Job分布式调度.md) | 18 篇只做"`@Scheduled` vs XXL-Job"的选型对比 |
| 限流算法原理 | [11-数据结构与算法/10-限流算法](../11-数据结构与算法/10-限流算法-四大算法与网关实现.md) | 不讲；02/04 篇只讲配置与过滤器位置 |
| 微服务治理（熔断/降级/灰度/限流体系） | [00-学习路线总览/03-微服务治理补全开发计划](../00-学习路线总览/03-微服务治理补全开发计划.md) | 08 篇是治理计划的落地记录之一 |

## 三、本项目 Spring 资产清单

```text
依赖版本（根 pom.xml:39-42）
├── Spring Boot            3.2.5
├── Spring Cloud           2023.0.1
├── Spring Cloud Alibaba   2023.0.1.0
├── Spring AI              1.1.4
└── Spring StateMachine    4.0.0（statemachine.version）

ai-cs-common（可复用能力的落点）
├── autoconfigure/          5 个自动配置（imports 登记，16 篇锚点）
│   ├── CommonWebAutoConfiguration   GlobalExceptionHandler 兜底
│   ├── MinioAutoConfiguration       MinioClient + FileStorageService
│   ├── ai/embedding/EmbeddingAutoConfig   HashEmbeddingModel（可被 openai 覆盖）
│   ├── idempotent/                  @Idempotent 注解 + 切面 + 自动装配（14/15 篇锚点）
│   └── mq/                          按 aics.mq.type 切换 RocketMQ/Kafka（16 篇锚点）
├── exception/GlobalExceptionHandler  异常统一出口（17 篇锚点）
├── result/Result + ResultCode        统一返回体
├── idempotent/                       全仓唯一手写 @Aspect（15 篇教学样本）
└── storage / util / mq / enums       文件存储 / JwtUtil / 消息抽象 / 公共枚举

各服务的 Spring 使用点（节选）
├── ai-cs-gateway/.../filter/AuthFilter.java        WebFlux GlobalFilter + @PostConstruct 白名单
├── ai-cs-user/.../security/                        SecurityFilterChain + @EnableMethodSecurity + HeaderAuthenticationFilter
├── ai-cs-product/.../config/ProductCacheConfig.java @EnableCaching + 分 cacheName TTL
├── ai-cs-order/.../service/impl/OrderServiceImpl.java  @Transactional + @GlobalTransactional + 领域事件
├── ai-cs-order/.../statemachine/                   状态机配置与迁移矩阵
├── ai-cs-order/.../task/OrderTimeoutScheduler.java  @Scheduled 兜底扫描
├── ai-cs-chat/.../config/ObservabilityWebConfig.java WebMvcConfigurer + TraceInterceptor
├── ai-cs-chat/.../config/ObservabilityExecutorConfig.java  ThreadPoolTaskExecutor × 2
├── ai-cs-chat/.../config/SentinelFlowConfig.java    @PostConstruct 灌限流规则
└── ai-cs-notify/.../config/WebSocketConfig.java     STOMP + CONNECT 鉴权
```

## 四、学习路径

**新手（会写 CRUD）**：01 → 15 的前六节 → 17 → 03。目标：能解释"注解为什么生效/不生效"，把线上遇到的接口问题定位到链路某一站。

**进阶（要讲清为什么）**：16 → 15 → 09 → 10。顺序理由：先有"容器里有什么"（16），才讲得清"代理在哪儿切进去"（15），再看两个真实声明式能力（09 缓存/事件、10 自动装配）。

**架构（要扛住一致性）**：06 → 07 → 12 → 13 → 14。目标：能独立讲清一条"下单 → 支付 → 通知 → 幂等"的完整链路与每层保障。

**验收**：[18-Spring面试专练](18-Spring面试专练与追问链.md) 的四条追问链全部能接住第二轮追问，即认为本模块过关。

## 五、更新日志

- 2026-08：P1~P6 六阶段落地记录（09~12 篇为落地技术文档，见 [00-学习路线总览/01-Spring技术补全开发计划](../00-学习路线总览/01-Spring技术补全开发计划.md)）。
- 2026-09：第三批补 13/14 两篇（支付、幂等）。
- 2026-09：第十一批补 15~18 四篇 + 本 README——补 Spring Framework 本体原理（AOP/事务、IoC/生命周期、MVC 全链路）与面试专练。
