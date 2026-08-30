# 02-Testcontainers 与安全框架技术选型（评估记录）

> 2026-08：第二梯队两项的「选型评估 + 环境限制说明」，先记录结论再择机落地。

## 一、Testcontainers（集成测试基础设施）

### 是什么

单测里用 Docker 拉起**一次性真实容器**（MySQL/Redis/RocketMQ），测试结束自动销毁。
对比当前方案的差异：

| 维度 | 现状（H2 + embedded-redis + Mockito） | Testcontainers |
|---|---|---|
| SQL 方言兼容 | H2 与 MySQL 有差异（函数/锁行为） | 真实 MySQL 8 |
| 中间件行为 | embedded-redis 版本老、模拟不完整 | 真实 Redis 7 / RocketMQ |
| 环境依赖 | 零依赖（CI 友好） | **必须有 Docker** |

### 为什么暂缓

本机与 CI 环境当前**没有 Docker**（K8s 部署在远端虚拟机集群），Testcontainers 无法运行。
引入一个跑不起来的依赖是负资产。

### 落地清单（有 Docker 后 30 分钟可接入）

1. 父 POM：`org.testcontainers:testcontainers-bom:1.19.7`（import）+
   `org.testcontainers:mysql`、`org.testcontainers:junit-jupiter`（test scope）
2. 基类：

```java
@Testcontainers
@SpringBootTest
abstract class IntegrationTestBase {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withInitScript("schema-test.sql");
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }
}
```

3. 优先替换的测试：分库分表路由（真 MySQL 验证 ShardingSphere 落表）、Seata AT 全局回滚
   （真 undo_log 行为）、Redisson 锁（真 Redis 看门狗）。

## 二、Spring Security / Sa-Token（安全框架）

### 现状盘点

- 鉴权：网关 JWT 过滤器（JJWT）统一校验，`X-User-Id` 透传下游
- 密码：BCrypt 已是存储格式
- 授权：角色标识在 JWT payload（admin/agent/user），接口层按需校验

### 结论：暂缓引入，理由（面试可讲的架构取舍）

1. **重复建设**：Spring Security 的过滤器链与本项目的「网关统一鉴权」架构定位重叠，
   引入后出现两套认证入口（网关 Filter vs Security Filter Chain），边界反而模糊。
2. **收益有限**：微服务场景下 Security 主要价值在 OAuth2 资源服务器/方法级授权；
   本项目无第三方 OAuth 接入需求，方法级校验用现有角色检查可覆盖。
3. **迁移成本**：11 个服务的启动链路、异常处理（AuthenticationException → 统一响应）
   都要适配。

### 触发重新评估的条件

- 接入第三方登录（微信/OAuth2）→ 引入 spring-boot-oauth2-resource-server
- 接口数量膨胀到需要**声明式权限模型**（如 `@PreAuthorize("hasRole('admin')")` 铺开）→
  引入 Spring Security 方法级安全，网关只做认证、服务做授权（关注点拆分更清晰）

### Sa-Token 对比一句话

Sa-Token 上手快（注解 + 内存/Redis 会话），但 Spring Security 生态（OAuth2/OIDC 支持成熟度）
更强；本项目已深度绑定 JWT 无状态方案，二者切换都是重写，性价比都不高。
