# 07-服务调用统一与 Seata XID 传播（Feign 迁移实录）

> 2026-08 落地记录：03-P1 缺陷修复。order 服务从手写 RestTemplate 全面迁移到 OpenFeign，
> 修复「product 扣库存分支游离在 Seata 全局事务外」的正确性缺陷。

## 一、缺陷是什么（为什么这是 P1）

改造前的调用链：

```java
// OrderServiceImpl.doCreateOrder —— @GlobalTransactional 开启全局事务
productStockClient.deductStock(productId, quantity);
//   ↓ ProductStockClient（旧实现）
restTemplate.put("http://ai-cs-product/product/{id}/stock/deduct?quantity=", null, id, qty);
```

三个事实叠加成一个隐蔽缺陷：

1. `RestTemplateConfig` 是裸 `new RestTemplate()`，**没有任何 Seata 拦截器**
2. seata 配置未开启 `seata.rest.template.enabled`（RestTemplate 传播 XID 需要显式开启）
3. 请求不带 `TX_XID` 头 → product 侧拿不到全局事务上下文 →
   扣库存的 `@Transactional` 是**独立本地事务**，提交后与全局事务无关

后果：全局事务二阶段回滚时，AT 补偿**覆盖不到 product 的扣减**。项目之所以"看起来正常"，
是因为 cancel 路径里的手工回补（`restoreStock`）一直在兜底——双机制并存掩盖了缺陷，
而且 `ProductStockClient` 的 javadoc 还写着旧机制「先扣后用、失败回补」，
与 06-Seata 文档「XID 自动传播」的说法漂移。

> 教训：**双保险机制会互相掩盖失效**。引入新机制后必须删除/降级旧机制，
> 并用「关掉旧机制依然正确」来验收新机制。

## 二、修复方案（三层传播条件）

XID 要真正生效，客户端传播、服务端绑定、数据源代理三者缺一不可：

```
[order 发起方]                [网络]                 [product 接收方]
@GlobalTransactional    →   TX_XID 请求头   →   SeataHandlerInterceptor 绑定 XID
RootContext.getXID()≠空      (Feign 拦截器)       RootContext.bind(xid)
        ↓                                               ↓
SeataFeignRequestInterceptor                  enable-auto-data-source-proxy
自动注入 XID（SCA seata starter 内置）          SQL 拦截生成 undo_log，注册分支事务
```

### 2.1 发起方：order 迁 Feign（传播自动生效）

`spring-cloud-starter-alibaba-seata` 的 jar 里带
`SeataFeignClientAutoConfiguration` + `SeataFeignRequestInterceptor`
（经 `META-INF/spring/...AutoConfiguration.imports` 注册）：
类路径上有 Feign 且 `seata.enabled=true` 时，自动把 Feign.Client 包上 XID 传播。

改动落点：

```
ai-cs-order/pom.xml            + spring-cloud-starter-openfeign
OrderApplication               + @EnableFeignClients(basePackages = "com.aics.order.client")
client/ProductClient.java      新增（deductStock / restoreStock / getProduct）
client/PayClient.java          新增（closeOrder，尽力而为语义在调用方 closePayChannel 收敛）
config/RestTemplateConfig.java 删除
client/ProductStockClient.java 删除
client/OrderPayClient.java     删除
```

迁移对照（RestTemplate → Feign）：

| 旧写法 | 新写法 |
|---|---|
| `restTemplate.put(url + "/{id}/stock/deduct?quantity={q}", null, id, q)` | `@PutMapping("/{id}/stock/deduct")` + `Result<Void> deductStock(@PathVariable, @RequestParam)` |
| `restTemplate.getForObject("http://ai-cs-product/product/{id}", ProductRemoteDTO.class, id)` | `@GetMapping("/{id}")` + `ProductRemoteDTO getProduct(@PathVariable("id") Long id)`，`@FeignClient(name = "ai-cs-product", path = "/product")` 承担服务名 + 基路径 |

### 2.2 接收方：product 补 seata-http 依赖

排查结论（解包 `seata-spring-boot-starter-1.7.1.jar` 验证）：
`spring.factories` 里的 `SeataHttpAutoConfiguration` 会注册
`JakartaSeataWebMvcConfigurer`（Boot 3 / jakarta 环境的 MVC 拦截器，从 `TX_XID` 头
绑定/解绑全局事务），但这些 `io.seata.integration.http.*` 类**不在 starter 包内**，
按 `@ConditionalOnClass` 装配——classpath 必须有 `seata-http`：

```
pom.xml（父）        dependencyManagement + io.seata:seata-http:${seata.version}
ai-cs-product/pom.xml + io.seata:seata-http（version 由父管理）
```

不加这个依赖，product 侧 MVC 拦截器根本不会注册——这是「引了 Seata 却没引 HTTP 集成」
的常见漏配点。

### 2.3 数据源代理

product 的 application.yml 已有 `enable-auto-data-source-proxy: true`（06-Seata 落地时配好），
XID 绑定后 SQL 自动被代理拦截生成 undo_log，无需改动。

## 三、为什么统一 Feign（面试可讲的架构论据）

1. **分布式事务正确性**：Seata 对 Feign 有开箱传播，对 RestTemplate 要手动开
   `seata.rest.template.enabled` 且容易被漏配——本次缺陷正是漏配实证
2. **超时/重试/降级**声明式集中配置（P2 的 fallbackFactory 挂在接口上，RestTemplate 做不到）
3. **契约显性化**：接口注解即 API 契约，路径漂移在编译期/契约测试期暴露
4. 负载均衡（LoadBalancer）、链路追踪注入（micrometer）都是自动的

## 四、验证与遗留

- ✅ `mvn -pl ai-cs-order verify`：74 个测试全绿，JaCoCo 覆盖率门禁通过
- ✅ product 引入 seata-http 后编译通过，`SeataHttpAutoConfiguration` 类路径条件满足
- ⏳ **端到端故障注入**（起 seata-server，product 扣库存抛异常，断言全局回滚且库存被
  AT 补偿、全程无手工回补参与）依赖 Docker/Testcontainers 环境，
  归入 01 计划 P3 落地时补（届时一并验证 `RootContext.getXID()` 在 product 侧非空）
- ✅ 文档漂移修正：OrderServiceImpl 注释原本写「Feign 调用」实际是 RestTemplate，
  现已名副其实；旧客户端类及其过时 javadoc 已删除

## 五、面试要点速记

- Seata XID 传播的完整链路：发起方 `@GlobalTransactional` → `SeataFeignRequestInterceptor`
  注入 `TX_XID` 头 → 下游 `SeataHandlerInterceptor` 绑定 `RootContext` → 数据源代理
  在 SQL 执行时按 XID 注册分支事务
- 「双机制并存掩盖缺陷」：手工回补兜底让 AT 失效看起来"没事"，验收新机制必须孤立验证
- RestTemplate vs Feign 的选型不只是风格问题，直接决定分布式事务能否正确传播
- 排查第三方 starter 的正确姿势：解包 jar 看 `spring.factories` / `AutoConfiguration.imports`
  与 `@ConditionalOnClass`，而不是只看文档
