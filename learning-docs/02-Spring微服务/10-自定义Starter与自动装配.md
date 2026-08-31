# 10-自定义 Starter 与自动装配：从零开始理解 Spring Boot 扩展机制

> 本文面向第一次接触 Spring Boot Starter 和自动装配的读者。
> 示例来自本项目：`ai-cs-common` 从“每个服务都扫描 `com.aics.common` 包”迁为
> Spring Boot 3 标准自动装配。
>
> 当前实现：common 自动提供统一异常处理、MinIO 客户端与文件服务、默认本地 Embedding 模型；
> 业务服务只扫描自身包，不再通过跨模块组件扫描获取 common Bean。

---

## 一、先说结论：Starter 到底是什么

很多初学者会把 Starter、自动装配、`@Configuration` 混在一起。先记住下面这张表：

| 名词 | 它解决什么 | 例子 |
|------|------------|------|
| Spring Boot Starter | 帮你打包一组常用依赖，减少手写 dependency | `spring-boot-starter-web` |
| 自动装配 AutoConfiguration | 根据依赖和配置，自动创建 Bean | Web starter 自动创建 MVC 相关 Bean |
| `@Configuration` | 普通 Spring 配置类，需要被组件扫描或显式 `@Import` | 业务模块的 `ProductCacheConfig` |
| 自定义 Starter | 你自己发布的“依赖聚合 + 自动装配”能力 | 本项目的 `ai-cs-common` 当前承担这个角色 |

一句话：

```text
Starter 负责“把依赖带进来”
AutoConfiguration 负责“根据条件自动创建 Bean”
```

官方示例：

```xml
<!-- 一个依赖，带来 web、json、tomcat、validation 等依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

引入后，你没有手动 `new DispatcherServlet()`，但 Web 应用已经能启动。
这就是 Starter 加自动装配带来的开发体验。

---

## 二、为什么项目需要自定义自动装配

### 2.1 改造前：跨模块组件扫描

以前多个服务的启动类写成：

```java
@SpringBootApplication(scanBasePackages = {"com.aics.order", "com.aics.common"})
public class OrderApplication {
}
```

这样 order 服务除了扫描自己的 Controller、Service、Mapper，还会扫描 common 包中的：

- `@RestControllerAdvice`
- `@Service`
- `@Component`
- `@ConfigurationProperties`
- `@Configuration`

短期能工作，但随着公共模块变大，会有几个问题。

### 2.2 问题一：每个服务都要记住公共包名

如果新服务只写：

```java
@SpringBootApplication
public class NewServiceApplication {
}
```

它默认只扫描启动类所在包及其子包，例如 `com.aics.newservice`。
`com.aics.common` 是兄弟包，不在扫描范围内：

```text
com.aics
├── common
└── newservice
    └── NewServiceApplication
```

结果：`GlobalExceptionHandler`、`FileStorageService` 等 Bean 可能缺失。

### 2.3 问题二：公共模块新增组件会隐式影响所有服务

假设有人在 common 中新增：

```java
@Component
public class SomeExperimentalComponent {
}
```

只要服务扫描了 `com.aics.common`，它就会被装进去。开发者可能不知道：

- 这个 Bean 是否会访问外部服务
- 是否会覆盖某个默认 Bean
- 是否会增加启动时间
- 是否会触发循环依赖

这叫做**隐式 Bean 图扩张**：服务的启动行为被公共模块的扫描结果悄悄改变。

### 2.4 问题三：难以按条件装配和覆盖

公共模块通常希望表达这种规则：

```text
如果项目引了 MinIO SDK，才提供 MinioClient
如果业务自己定义了 MinioClient，就使用业务自己的
如果是 Servlet Web 服务，才提供 MVC 异常处理器
如果配置 provider=local，才提供本地 HashEmbeddingModel
```

单纯 `@Component` 扫描无法清晰表达这些条件；自动装配的条件注解则是为此设计的。

---

## 三、本项目改造后的结构

现在各服务只扫描自己的包：

```java
@SpringBootApplication(scanBasePackages = {"com.aics.order"})
public class OrderApplication {
}
```

common 的能力通过 Spring Boot 自动装配进入 ApplicationContext：

```text
ai-cs-order 引用 ai-cs-common
            │
            ▼
Spring Boot 启动时读取 ai-cs-common jar 内的 imports 文件
            │
            ▼
按条件导入 common 的 AutoConfiguration
            │
            ▼
创建 GlobalExceptionHandler / MinioClient / FileStorageService / EmbeddingModel 等 Bean
```

项目中的核心入口文件：

```text
ai-cs-common/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

文件内容：

```text
com.aics.common.autoconfigure.CommonWebAutoConfiguration
com.aics.common.autoconfigure.MinioAutoConfiguration
com.aics.common.ai.embedding.EmbeddingAutoConfig
```

每行都是一个自动配置类的全限定类名。

---

## 四、Spring Boot 3 自动装配完整加载过程

下面是一次服务启动时的大致过程：

```mermaid
flowchart TD
    A[启动 Application.main] --> B[@SpringBootApplication]
    B --> C[@EnableAutoConfiguration]
    C --> D[ImportCandidates 读取 classpath]
    D --> E[META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports]
    E --> F[导入 AutoConfiguration 类]
    F --> G{条件注解匹配?}
    G -->|是| H[注册 Bean 定义]
    G -->|否| I[跳过自动配置]
    H --> J[ApplicationContext 完成刷新]

    K[业务显式 Bean] --> L{@ConditionalOnMissingBean?}
    L -->|业务 Bean 已存在| M[自动配置让位]
    L -->|业务 Bean 不存在| H
```

用本项目的 `MinioAutoConfiguration` 举例：

```text
classpath 中是否有 MinioClient？
  ├── 没有 → 跳过 MinIO 自动配置
  └── 有
       ├── 业务自己有没有定义 MinioClient？
       │    ├── 有 → common 自动配置让位
       │    └── 没有 → common 根据 aics.minio.* 创建 MinioClient
       │
       └── 业务自己有没有定义 FileStorageService？
            ├── 有 → 自动配置让位
            └── 没有 → common 创建默认 FileStorageService
```

---

## 五、Boot 2 和 Boot 3 的区别

### 5.1 Boot 2：spring.factories

Spring Boot 2 中自定义自动装配一般写：

```text
META-INF/spring.factories
```

内容：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.demo.ExampleAutoConfiguration
```

问题：`spring.factories` 同时可用于很多 SPI 扩展点，文件会越来越大；读取时也更偏全量扫描。

### 5.2 Boot 3：AutoConfiguration.imports

Spring Boot 3 推荐使用：

```text
META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容更简单：

```text
com.example.demo.ExampleAutoConfiguration
```

本项目使用的就是 Boot 3 方式。

| 对比项 | Boot 2 常见方式 | Boot 3 推荐方式 |
|--------|----------------|----------------|
| 文件 | `META-INF/spring.factories` | `META-INF/spring/...AutoConfiguration.imports` |
| 内容 | `EnableAutoConfiguration=类名` | 每行一个类名 |
| 声明注解 | `@Configuration` | `@AutoConfiguration` |
| 读取方式 | 通用 SPI 文件 | 更明确的自动装配候选文件 |

---

## 六、本项目三个自动配置模块

### 6.1 CommonWebAutoConfiguration：统一异常处理

代码逻辑：

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
```

逐行理解：

| 写法 | 含义 |
|------|------|
| `@AutoConfiguration` | 这是 Boot 自动装配候选类，不需要组件扫描 |
| `@ConditionalOnWebApplication(SERVLET)` | 只在 MVC/Servlet Web 应用中生效 |
| `@Bean` | 创建 `GlobalExceptionHandler` Bean |
| `@ConditionalOnMissingBean` | 如果业务服务已自定义异常处理器，common 默认实现不抢占 |

为什么限定 `SERVLET`：

- order、product、user 等服务使用 Spring MVC
- gateway 使用 Spring Cloud Gateway / WebFlux
- `GlobalExceptionHandler` 依赖 Servlet 相关类型，不应该误装到 Gateway

### 6.2 MinioAutoConfiguration：MinIO 文件能力

核心结构：

```java
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient minioClient(MinioProperties properties) { ... }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService fileStorageService(
            MinioClient minioClient,
            MinioProperties properties) { ... }
}
```

关键点：

#### `@ConditionalOnClass(MinioClient.class)`

只有 classpath 里有 MinIO SDK 才启用。它防止 common 在没有 MinIO 依赖的模块中报类找不到。

#### `@EnableConfigurationProperties(MinioProperties.class)`

以前 `MinioProperties` 写成：

```java
@Component
@ConfigurationProperties(prefix = "aics.minio")
```

这意味着它依赖包扫描。

现在改成：

```java
@ConfigurationProperties(prefix = "aics.minio")
public class MinioProperties { ... }
```

然后由自动配置显式注册：

```java
@EnableConfigurationProperties(MinioProperties.class)
```

好处：配置类从普通组件扫描中独立出来，归属更清晰，也更容易用
`ApplicationContextRunner` 测试绑定。

### 6.3 EmbeddingAutoConfig：默认本地向量模型

核心逻辑：

```java
@AutoConfiguration
@ConditionalOnProperty(
    name = "aics.ai.embedding.provider",
    havingValue = "local",
    matchIfMissing = true
)
public class EmbeddingAutoConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel hashEmbeddingModel() {
        return new HashEmbeddingModel();
    }
}
```

规则：

```text
aics.ai.embedding.provider=local 或未配置
  → 如果业务没有 EmbeddingModel → 提供 HashEmbeddingModel

aics.ai.embedding.provider=openai
  → 不提供本地模型

业务自己提供 EmbeddingModel
  → 无论 provider 如何，默认 HashEmbeddingModel 都让位
```

这让 common 能为本地学习环境提供零外部依赖的默认模型，同时不会覆盖 chat/knowledge
服务自己的 OpenAI 兼容 EmbeddingModel。

---

## 七、为什么 `@ConditionalOnMissingBean` 很重要

假设 common 直接写：

```java
@Bean
public MinioClient minioClient() { ... }
```

而业务服务又写：

```java
@Bean
public MinioClient enterpriseMinioClient() { ... }
```

启动时会出现两个 `MinioClient` Bean，注入时可能报：

```text
NoUniqueBeanDefinitionException
```

或者 common 的默认配置抢占业务特殊配置。

加上：

```java
@ConditionalOnMissingBean(MinioClient.class)
```

优先级就变成：

```text
业务服务显式声明 Bean
  > common 自动配置默认 Bean
  > 没有任何 Bean 时才创建默认实现
```

这是 Starter 最重要的设计原则之一：

> 自动配置应该提供合理默认值，但永远允许业务模块覆盖。

---

## 八、从组件扫描迁移到自动装配，项目改了什么

### 8.1 删除的旧结构

```text
ai-cs-common/storage/MinioConfig.java
```

旧 `MinioConfig` 是普通 `@Configuration`，需要 `com.aics.common` 被扫描才会生效。

### 8.2 改造的 common Bean

| 原结构 | 新结构 |
|--------|--------|
| `MinioConfig @Configuration` | `MinioAutoConfiguration @AutoConfiguration` |
| `MinioProperties @Component` | `@ConfigurationProperties` + `@EnableConfigurationProperties` |
| `FileStorageService @Service` | 自动配置里的 `@Bean` |
| `EmbeddingAutoConfig @Configuration` | `@AutoConfiguration` |
| `GlobalExceptionHandler` 靠扫描 | `CommonWebAutoConfiguration` 显式提供 |

### 8.3 移除 common 扫描的服务

下列启动类已移除 `"com.aics.common"`：

```text
ai-cs-chat
ai-cs-knowledge
ai-cs-mq
ai-cs-notify
ai-cs-order
ai-cs-pay
ai-cs-product
```

例如：

```java
// 改造前
@SpringBootApplication(scanBasePackages = {"com.aics.order", "com.aics.common"})

// 改造后
@SpringBootApplication(scanBasePackages = {"com.aics.order"})
```

现在 common 能力的来源是 Maven 依赖里的自动装配入口，而不是“扫到了某个包”。

---

## 九、如何自己写一个新的自动配置

假设你想给 common 增加一个默认的业务审计器 `AuditService`。

### 第一步：写配置属性

```java
@ConfigurationProperties(prefix = "aics.audit")
public class AuditProperties {
    private boolean enabled = true;
    private String channel = "log";
}
```

### 第二步：写自动配置类

```java
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "aics.audit",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditService.class)
    public AuditService auditService(AuditProperties properties) {
        return new DefaultAuditService(properties);
    }
}
```

### 第三步：登记 imports

在文件末尾新增一行：

```text
com.aics.common.autoconfigure.AuditAutoConfiguration
```

### 第四步：写条件装配测试

至少覆盖：

1. 默认启用时 Bean 存在
2. `aics.audit.enabled=false` 时 Bean 不存在
3. 业务自己提供 `AuditService` 时默认 Bean 让位
4. 配置值正确绑定到 `AuditProperties`

---

## 十、如何测试自动装配：ApplicationContextRunner

不要为了测试一个自动配置就启动整个 Spring Boot 应用。使用：

```java
ApplicationContextRunner
```

本项目测试：

```text
ai-cs-common/src/test/java/
  com/aics/common/autoconfigure/CommonAutoConfigurationTest.java
```

示例：

```java
new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(MinioAutoConfiguration.class))
    .withPropertyValues(
        "aics.minio.endpoint=http://127.0.0.1:9000",
        "aics.minio.access-key=test",
        "aics.minio.secret-key=test1234"
    )
    .run(context -> {
        assertThat(context).hasSingleBean(MinioClient.class);
    });
```

当前测试覆盖：

| 场景 | 断言 |
|------|------|
| Servlet Web 上下文 | 自动得到 `GlobalExceptionHandler` |
| MinIO 默认配置 | 有 `MinioProperties` 与 `MinioClient` |
| 业务自定义 `MinioClient` | common 自动配置让位，不出现第二个 Bean |
| provider=local | 有 `HashEmbeddingModel` |
| provider=openai | 不装本地 HashEmbeddingModel |

这是比“启动一个完整服务看看是否报错”更快、更稳定的测试方式。

---

## 十一、如何调试自动装配为什么没有生效

### 11.1 开启 conditions debug report

启动参数：

```bash
--debug
```

或者配置：

```yaml
debug: true
```

Spring Boot 会在启动日志里打印 Condition Evaluation Report，能看到：

```text
MinioAutoConfiguration:
  Positive matches:
    - @ConditionalOnClass found required class 'io.minio.MinioClient'

  Negative matches:
    - @ConditionalOnMissingBean found existing bean 'enterpriseMinioClient'
```

### 11.2 Actuator conditions 端点

开启 Actuator 后可以查看：

```text
/actuator/conditions
```

它会展示自动配置匹配与不匹配原因。

### 11.3 常见排查顺序

```text
1. imports 文件路径和类名是否正确
2. 自动配置类是否使用 @AutoConfiguration
3. Maven 依赖是否真的在 classpath
4. @ConditionalOnClass 依赖类是否存在
5. @ConditionalOnProperty 配置键和值是否匹配
6. 是否已经有业务 Bean 导致 @ConditionalOnMissingBean 让位
7. 是否被 @SpringBootApplication(exclude=...) 排除了
```

---

## 十二、Starter 的两种模块组织方式

### 12.1 当前项目：common 同时承担 starter 角色

```text
ai-cs-common
├── Result / ResultCode / BusinessException
├── JwtUtil
├── 自动配置类
├── MinIO 文件服务
└── 默认 Embedding 模型
```

优点：模块少，学习项目简单。

缺点：纯 DTO/工具依赖 Spring Boot、MinIO、Spring AI，依赖面偏大。

### 12.2 生产常见拆法：core + starter

```text
ai-cs-common-core
├── DTO
├── Result
├── 异常
└── 纯 Java 工具

ai-cs-common-spring-boot-autoconfigure
├── AutoConfiguration
├── Properties
└── 条件装配 Bean

ai-cs-common-spring-boot-starter
├── 依赖 common-core
└── 依赖 autoconfigure
```

业务服务只需：

```xml
<dependency>
    <groupId>com.aics</groupId>
    <artifactId>ai-cs-common-spring-boot-starter</artifactId>
</dependency>
```

这也是很多官方 Starter 的组织方式。

---

## 十三、常见错误

### Q1：我写了 `@AutoConfiguration`，为什么 Bean 没有创建？

最常见原因是忘记在 `AutoConfiguration.imports` 中登记类名。`@AutoConfiguration` 本身不会被
组件扫描自动发现。

### Q2：为什么不把自动配置类加 `@Component`？

自动配置不应该依赖包扫描；它应由 imports 文件精确导入。加 `@Component` 又回到了跨包扫描问题。

### Q3：为什么 `@ConfigurationProperties` 不再加 `@Component`？

因为它由 `@EnableConfigurationProperties` 显式注册，绑定行为更可控、更容易测试；
也不会要求业务扫描 common 包。

### Q4：`@ConditionalOnMissingBean` 会不会让 Bean 永远不创建？

不会。只有同类型 Bean 已存在时才让位。没有业务 Bean 时，默认 Bean 正常创建。

### Q5：自动装配顺序能控制吗？

可以使用：

```java
@AutoConfigureBefore(...)
@AutoConfigureAfter(...)
```

但不要滥用。大多数场景靠条件注解和依赖注入就够；只有多个自动配置确实有顺序依赖时才声明。

### Q6：Gateway 为什么不装 GlobalExceptionHandler？

Gateway 是 WebFlux，common 的 `CommonWebAutoConfiguration` 使用
`@ConditionalOnWebApplication(SERVLET)`，因此不会错误装入 Servlet MVC 的异常处理器。
Gateway 应使用自己的 WebFlux 错误处理方式。

---

## 十四、项目验证结果

自动装配改造后执行：

```text
ai-cs-common: 10 tests passed
ai-cs-order:  84 tests passed
ai-cs-product: 54 tests passed
mvn -DskipTests compile: BUILD SUCCESS
```

已验证：

- `AutoConfiguration.imports` 能正确加载 common 自动配置
- MinIO/Embedding 支持条件装配和业务 Bean 覆盖
- 7 个服务移除 common 包扫描后仍可编译与通过主要模块回归
- 自动装配改造未破坏 order/product 的业务测试

全 11 个服务的真实启动回归需要 Nacos、Redis、RocketMQ、MinIO 等基础设施环境，
应在部署验收阶段执行。

---

## 十五、面试要点总结

可以这样描述本项目：

> 我们最初通过 `scanBasePackages` 跨包扫描 common，但这种方式会让公共模块新增组件隐式影响
> 所有服务，也让配置绑定依赖扫描。后来将 common 改为 Spring Boot 3 自动装配：使用
> `META-INF/spring/...AutoConfiguration.imports` 登记 Web、MinIO、Embedding 三个自动配置类，
> 结合 `@ConditionalOnClass`、`@ConditionalOnProperty` 和 `@ConditionalOnMissingBean` 提供
> 默认能力并允许业务覆盖。服务启动类只扫描自身包，公共能力由 Maven 依赖中的自动装配入口获得。
> 我们还用 `ApplicationContextRunner` 覆盖了条件生效、条件失效和业务 Bean 覆盖场景。

需要记住的关键词：

```text
Starter
AutoConfiguration
AutoConfiguration.imports
ConditionalOnClass
ConditionalOnProperty
ConditionalOnMissingBean
EnableConfigurationProperties
ApplicationContextRunner
默认能力 + 业务覆盖
```
