# 10-自定义 Starter 与自动装配（01-P4 落地记录）

> 2026-08 落地：将 ai-cs-common 从"所有服务跨包扫描 common"迁为 Spring Boot 3 标准自动装配。
> 业务服务只扫描自身包，common 的 Web/MinIO/Embedding 通过
> `AutoConfiguration.imports` 自动进入 Bean 图。

## 一、改造前的问题：公共模块依赖组件扫描

以前多服务启动类写：

```java
@SpringBootApplication(scanBasePackages = {"com.aics.order", "com.aics.common"})
```

问题：

- 每个服务都要记住 common 包名，漏掉即出现 Bean 缺失
- common 内新增一个 `@Component`，会隐式注入所有扫描 common 的服务，边界不可控
- 无法按 classpath/config 条件装配，也无法让业务模块轻易覆盖公共 Bean

## 二、Spring Boot 3 自动装配链路

```text
@SpringBootApplication
  → @EnableAutoConfiguration
  → 读取所有依赖 jar 的 META-INF/spring/
     org.springframework.boot.autoconfigure.AutoConfiguration.imports
  → 导入 @AutoConfiguration 类
  → @ConditionalOnClass / @ConditionalOnProperty / @ConditionalOnMissingBean 决定 Bean 图
```

本项目新增文件：

```text
ai-cs-common/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

内容（每行一个自动配置类）：

```text
com.aics.common.autoconfigure.CommonWebAutoConfiguration
com.aics.common.autoconfigure.MinioAutoConfiguration
com.aics.common.ai.embedding.EmbeddingAutoConfig
```

## 三、三个自动配置模块

| 自动配置 | 条件 | 产出 |
|----------|------|------|
| CommonWebAutoConfiguration | Servlet Web 应用 + 缺少同类 Bean | GlobalExceptionHandler |
| MinioAutoConfiguration | classpath 有 MinioClient | MinioProperties、MinioClient、FileStorageService |
| EmbeddingAutoConfig | `aics.ai.embedding.provider=local`（默认）+ 缺少 EmbeddingModel | HashEmbeddingModel |

关键注解：

- `@AutoConfiguration`：Boot 3 的自动配置声明（替代旧 `@Configuration` + `spring.factories`）
- `@ConditionalOnMissingBean`：业务模块自己声明同类型 Bean 时自动让位
- `@EnableConfigurationProperties(MinioProperties.class)`：配置绑定不再依赖 `@Component` 扫描
- `@ConditionalOnWebApplication(SERVLET)`：gateway WebFlux 不误装 Servlet `@RestControllerAdvice`

## 四、迁移结果

删除旧的 `MinioConfig`（功能并入 MinioAutoConfiguration），并将 `FileStorageService` 从
`@Service` 改为自动配置提供的 Bean；`MinioProperties` 去掉 `@Component`。

chat/knowledge/mq/notify/order/pay/product 七个启动类全部移除 `com.aics.common` 包扫描，
只扫描自身模块。例如：

```java
@SpringBootApplication(scanBasePackages = {"com.aics.order"})
```

这样 common 的公共能力仍存在，但来源变成显式的依赖级自动装配，而不是隐式组件扫描。

## 五、测试与验证

`CommonAutoConfigurationTest` 使用 `ApplicationContextRunner`：

- Servlet 上下文自动得到 GlobalExceptionHandler
- MinIO 配置绑定后创建 MinioClient
- 业务自定义 MinioClient 时自动配置让位（单 Bean 断言）
- embedding provider=local 得 HashEmbeddingModel；openai 时不装

验证结果：

```text
ai-cs-common: 10 tests passed
ai-cs-order:  81 tests passed
ai-cs-product: 54 tests passed
mvn ... test: BUILD SUCCESS
```

## 六、面试要点

- Boot 2：`META-INF/spring.factories`；Boot 3：`AutoConfiguration.imports`，减少全量扫描
- `@ConditionalOnMissingBean` 的覆盖优先级：业务显式 Bean > 自动配置默认 Bean
- 为什么 properties 不再 `@Component`：由 `@EnableConfigurationProperties` 管理，更显式、更可测试
- Starter 拆分边界：当前 common 直接承担 starter 角色；上规模后可拆
  `ai-cs-common-core`（纯 DTO/工具）+ `ai-cs-common-spring-boot-starter`（自动配置/依赖聚合）
