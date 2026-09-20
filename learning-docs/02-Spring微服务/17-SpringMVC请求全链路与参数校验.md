# 17-Spring MVC 请求全链路与参数校验：从零开始理解「一个请求的九站旅程」

> 前置阅读：
> - [02-Spring微服务/01-SpringBoot核心原理](01-SpringBoot核心原理.md)——第一节那张 5 行的"请求处理流程"，本篇把它展开成完整版本；
> - [02-Spring微服务/04-SpringCloudGateway网关](04-SpringCloudGateway网关.md)——请求进入微服务**之前**在网关经历的 WebFlux 过滤器链（与本篇的 Servlet 过滤链是两个体系）；
> - [02-Spring微服务/15-SpringAOP与声明式事务原理](15-SpringAOP与声明式事务原理.md)——本篇第三节要做"Filter vs Interceptor vs AOP"的三方对比；
> - [09-安全与设计模式/01-JWT鉴权与异常处理](../09-安全与设计模式/01-JWT鉴权与异常处理.md)——统一返回体与异常处理的使用层。
>
> 本篇回答三个问题：
> 1. 一个 HTTP 请求从 Tomcat 接住到返回 JSON，中间**经过了几站**？每站谁负责？
> 2. `@RequestHeader` / `@RequestParam` / `@PathVariable` / `@RequestBody` 到底是从哪里"取"数据的？
> 3. 校验失败时 `@Valid` 抛的异常，是怎么变成前端看到的 `{"code":400,"message":"数量至少为1"}` 的？

---

## ⚡ 30 秒速记卡（先背这个，再往下看推导）

```text
① 九站：Filter → DispatcherServlet → 查表(HandlerMapping) → 解参+校验(HandlerAdapter) → Controller → 序列化 → 异常出口
② 取数四口：@RequestHeader 头 / @RequestParam 查询串 / @PathVariable 路径 / @RequestBody 请求体
③ 校验失败发生在"进方法之前"——方法体一行都不会执行
④ 分工口诀：Filter 管进门(容器) / Interceptor 管接口(MVC) / AOP 管方法(Spring)——Filter 里的异常 @ControllerAdvice 接不住
⑤ 出问题先查"现象 → 出错的站"映射表（第五节），再翻日志
```

> 下面每一步推导都在解释这五条。读完再回来默写一遍，能全对就算过关。

---

## 一、先看全景：请求的九站旅程

用项目里"加入购物车"这个真实接口做主线：`POST /cart/add`（经网关转发到 `ai-cs-order`）。

```text
浏览器/前端
   │  POST /cart/add   Body: {"productId":1001,"quantity":2}   Header: Authorization / X-User-Id
   ▼
┌─ [网关 ai-cs-gateway] AuthFilter（WebFlux GlobalFilter，另一条链，见 02-04）──────────┐
│   校验 JWT → 清洗伪造头 → 透传 X-User-Id / X-User-Name / X-User-Roles                │
└────────────────────────────────────────────────────────────────────────────────────┘
   │  转发到 ai-cs-order:8087
   ▼
【站1】Tomcat 线程池接住连接（一个请求 = 一条 Tomcat 工作线程）
   ▼
【站2】Filter 链（Servlet 规范最外层）        ← 项目实体：HeaderAuthenticationFilter
   ▼
【站3】DispatcherServlet（前端控制器，总调度）
   ▼
【站4】HandlerMapping 查表：/cart/add → CartController#addToCart(...)
   ▼
【站5】HandlerAdapter 调参：把 HTTP 数据"翻译"成方法参数（@RequestHeader/@RequestBody…）
   ├──【站6】参数校验：@Valid 触发，失败直接抛异常（方法体还没执行）
   ▼
【站7】Controller 方法执行 → Service → Mapper
   ▼
【站8】返回值处理：Result 对象 → HttpMessageConverter（Jackson）→ JSON
   ▼
【站9】若中途抛异常 → HandlerExceptionResolver → @RestControllerAdvice（GlobalExceptionHandler）
   ▼
响应回去（原路返回：还有站的"下半场"要执行：postHandle、afterCompletion、Filter 后半段）
```

**先记这句总纲**：

> **Filter 在门口（不管你是谁）；DispatcherServlet 是总台；HandlerMapping 查号；HandlerAdapter 带着翻译干活；
> 参数校验发生在"进入方法之前"；异常有统一出口。**

---

## 二、逐站详解（全部对照项目代码）

### 2.1 站1：Tomcat 接连接

各服务都是"内置 Tomcat"的 Spring Boot 应用，`server.port: 8087`（order）。
一个请求进来，Tomcat 从**工作线程池**取一条线程跑后续所有逻辑——所以：

- 业务代码里做"阻塞操作"（调 LLM、查慢 SQL）会**占住这条线程**；
- `ThreadLocal`（事务连接、trace 上下文、SecurityContext）都以"这条线程"为边界，**线程复用要记得清理**（后面 `HeaderAuthenticationFilter` 的 `finally` 就是干这个的）。

> 对比知识：`ai-cs-chat` 的 SSE 接口一条连接能挂很久，靠的正是"把 Tomcat 线程还回去、结果异步推送"（见第四节）。

### 2.2 站2：Filter 链——最外层，和 Spring MVC 无关

**Servlet 规范的东西**（不是 Spring 发明的），可以拿到 `request/response`，但**不知道"最终会执行哪个方法"**。

项目里全仓唯一的 Filter（grep `extends OncePerRequestFilter` 仅 1 处）：

```java
// ai-cs-user/src/main/java/com/aics/user/security/HeaderAuthenticationFilter.java:25-56（节选）
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {   // ← "一个请求只跑一次"

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");        // ① 读网关透传的可信头
        if (userId != null && !userId.isBlank()) {
            String roles = request.getHeader("X-User-Roles");
            List<SimpleGrantedAuthority> authorities = ...;    // ② "ADMIN" → "ROLE_ADMIN"
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);  // ③ 放入 ThreadLocal
        }
        try {
            filterChain.doFilter(request, response);           // ④ 放行给后面的链
        } finally {
            SecurityContextHolder.clearContext();              // ⑤ 必须清理：线程池复用防身份串线
        }
    }
}
```

**四个学习点**：

| 点 | 说明 |
|---|---|
| `OncePerRequestFilter` | 同一请求即使被 forward/include 多次，也只执行一次（普通 Filter 可能跑多次） |
| **它不校验 JWT** | 认证在网关（`ai-cs-gateway` 的 `AuthFilter`）；服务端只信"网关清洗过的头"——**认证与授权分离**（见 [09-03](../09-安全与设计模式/03-SpringSecurity微服务两层安全模型.md)） |
| 放入 `SecurityContextHolder` | 底层也是 ThreadLocal，所以 `@PreAuthorize` 能读到当前用户 |
| `finally` 清理 | 防"下一条复用这条线程的请求"意外带着上一个人的身份 |

它如何接入 Security 过滤链（**顺序**很重要，必须排在用户名密码认证过滤器前面）：

```java
// ai-cs-user/src/main/java/com/aics/user/security/UserSecurityConfig.java:43
.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

### 2.3 站3：DispatcherServlet——"前端控制器"模式

所有请求都先到它，然后它**自己从不处理业务**，只做调度：

```text
DispatcherServlet.doDispatch() 的白话版：
  1. 问 HandlerMapping：这个 URL 归谁？           → 得到 CartController#addToCart + 一堆拦截器
  2. 问 HandlerAdapter：谁会调这个方法？           → 得到 RequestMappingHandlerAdapter
  3. 依次执行拦截器 preHandle（TraceInterceptor 在这层）
  4. 调 HandlerAdapter.handle()：解析参数 → 校验 → 反射调用 Controller 方法
  5. 拿到返回值 → 交给 HandlerMethodReturnValueHandler → 序列化成 JSON
  6. 途中任何异常 → 交给 HandlerExceptionResolver（GlobalExceptionHandler 在这层）
  7. 执行拦截器 postHandle / afterCompletion
```

> **为什么要有"前端控制器"这个设计**（面试可能问）：把"公共的调度逻辑"集中一处，避免每个 Servlet 重复写路由、参数解析、异常处理——本质仍是**横切逻辑集中化**的思想（和 AOP 同源）。

### 2.4 站4：HandlerMapping——URL 到方法的映射表

映射关系由注解声明：

```java
// ai-cs-order/src/main/java/com/aics/order/controller/CartController.java:28-47
@RestController                      // 组合注解：@Controller + @ResponseBody
@RequestMapping("/cart")             // 类级前缀
public class CartController {

    @PostMapping("/add")             // 方法级路径 → 完整路径 POST /cart/add
    public Result<CartVO> addToCart(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody CartAddDTO dto) {
        return Result.success("已加入购物车", cartService.addToCart(userId, dto.getProductId(), dto.getQuantity()));
    }
}
```

启动时，`RequestMappingHandlerMapping` 会扫描所有 `@RequestMapping` 方法，建立"**路径+方法 → 处理器方法**"的注册表（URL → 方法，不是 URL → 类）。
请求进来后按最精确匹配（Ant 风格/路径变量都能参与匹配）。

> 跨服务视角：网关路由 `/cart/**` → `lb://ai-cs-order`（路由规则见 `ai-cs-gateway` 配置与 [02-04](04-SpringCloudGateway网关.md)）。
> 网关只改"目的地"，**不改路径**，所以服务端声明的还是 `/cart/add`。

### 2.5 站5：参数绑定——四个"取数口"（重点）

HTTP 里的数据位置不同，Spring 用不同注解"对口取数"。这张表建议背：

| 注解 | 数据来自 | 项目实例 | 备注 |
|---|---|---|---|
| `@RequestHeader("X-User-Id")` | **请求头** | `CartController:40,46` | 网关透传身份的标准姿势 |
| `@RequestParam("sessionId")` | **URL 查询串** `?sessionId=x` | `ChatController:168`（SSE 接口） | `required=false` / `defaultValue="false"` 可配 |
| `@PathVariable("cartItemId")` | **路径占位符** `/cart/{cartItemId}` | `CartController:59-61` | RESTful 风格 |
| `@RequestBody` | **请求体 JSON** | `CartController:47` `CartAddDTO` | 由 Jackson 反序列化；`produces/consumes` 控制格式 |

**类型转换是自动的**：URL 上的一切天生是字符串，Spring 用 `ConversionService` 转成 `Long` / `Integer` / `BigDecimal` / `boolean` / `LocalDateTime`……
转不动就抛 `MethodArgumentTypeMismatchException`——**注意：本项目的 `GlobalExceptionHandler` 没有专门处理它**（它不是 `IllegalArgumentException` 的子类），会落入兜底 handler 变成 500"系统内部错误"。想统一成 400，补一个 `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` 即可（完整对照见第五节的排查映射表）。

```java
// ChatController 展示了"可选参数 + 默认值"的写法
@PostMapping(value = "/stream/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStreamSse(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                @RequestParam("message") @NotBlank String message,
                                @RequestParam(value = "knowledgeBase", required = false) String knowledgeBase,
                                @RequestParam(value = "hybrid", defaultValue = "false") boolean hybrid,
                                @RequestParam(value = "rewrite", defaultValue = "false") boolean rewrite) { ... }
```

> **易错点**：`@RequestParam` 不写名字时靠**参数名**绑定，而参数名要编译期 `-parameters` 才有。
> 本仓根 POM 已开启 `maven.compiler.parameters`（见 [02-Spring微服务/14 第 5.1 节](14-分布式幂等设计.md)）——
> 同一件事同时救了 SpEL 和参数绑定，面试可以当"一处配置两处受益"的例子讲。

### 2.6 站6：参数校验——`@Valid` 的完整链路

**校验声明在 DTO 上**（`jakarta.validation` 注解）：

```java
// ai-cs-order/src/main/java/com/aics/order/dto/CartAddDTO.java:14-28
@Data
@Schema(description = "加入购物车请求")
public class CartAddDTO implements Serializable {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "单次最多购买99件")
    private Integer quantity;
}
```

**触发校验的是方法参数上的 `@Valid`**：

```java
public Result<CartVO> addToCart(@RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody CartAddDTO dto) { ... }
```

校验失败会发生什么（**关键：方法体一行都不会执行**）：

```text
@Valid 校验不通过
   → 抛 MethodArgumentNotValidException（@RequestBody 场景的标准异常）
   → DispatcherServlet 捕获，交给 HandlerExceptionResolver
   → @RestControllerAdvice（GlobalExceptionHandler）里命中对应 @ExceptionHandler
   → 返回 Result.fail(400, "数量至少为1; 商品ID不能为空")   ← message 直接来自 DTO 注解
```

**三种校验异常别搞混**（面试高频）：

| 场景 | 抛出的异常 | 项目处理位置 |
|---|---|---|
| `@Valid @RequestBody DTO` 校验失败 | `MethodArgumentNotValidException` | `GlobalExceptionHandler:42-50` |
| 非 JSON 的**对象绑定**（表单/查询参数绑定到对象） | `BindException` | `GlobalExceptionHandler:55-63` |
| **方法级** `@Validated` + 参数上 `@NotBlank`（如 `ChatController` 的 SSE 参数） | `ConstraintViolationException` | `GlobalExceptionHandler:68-76` |

> **`@Valid` 与 `@Validated` 的区别（面试常考）**：
> - `@Valid` 是 **Jakarta 标准**注解，用在"级联校验/参数"；
> - `@Validated` 是 **Spring** 的注解，加 `@Validated` 在类上才会启用**方法级**校验（AOP 干的事，属于 15 篇的"代理"家族）。
> 项目里两种都有：DTO 上用 `@Valid`，Controller 方法参数（`@NotBlank`）靠**类级 `@Validated`**——
> grep 实证 12 个 Controller 类级标注（`ChatController:53`、`UserController:23`、`SearchController:25`…），
> 其中 `ChatFeedbackController:24-28` 的类注释专门写了这层分工：
> "`@Validated`（类级）激活方法参数上的约束校验；`@Valid @RequestBody` 触发对请求体嵌套对象的 Bean Validation"。

### 2.7 站7-8：Controller 执行与返回值序列化

```java
@RestController   // = @Controller + @ResponseBody
```

- `@Controller`：声明"我是一个处理器"；
- `@ResponseBody`：**返回值不当作页面名去渲染**，而是交给 `HttpMessageConverter` 写进响应体。

对象 → JSON 由 Jackson 完成（项目统一用 `Result<T>` 包裹）：

```java
public Result<CartVO> getCartList(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(cartService.getCartList(userId));
}
// 响应：{"code":200,"message":"操作成功","data":{...},"timestamp":1758...}
```

**统一返回体的价值**（对照 [09-01](../09-安全与设计模式/01-JWT鉴权与异常处理.md)）：前端只需判断 `code`；无论成功、业务失败还是参数错，**HTTP 结构永远一致**。

### 2.8 站9：异常出口——`@RestControllerAdvice`

```java
// ai-cs-common/src/main/java/com/aics/common/exception/GlobalExceptionHandler.java:25-26
@RestControllerAdvice      // = @ControllerAdvice + @ResponseBody：给所有 Controller 当"兜底出口"
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)      // 同时把 HTTP 状态码置为 400
    public Result<Void> handleMethodArgumentNotValidException(...) { ... }

    @ExceptionHandler(Exception.class)            // 兜底：任何未捕获异常 → 500"系统内部错误"
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] {}", request.getMethod(), request.getRequestURI(), e);  // 日志记全栈
        return Result.fail(ResultCode.INTERNAL_ERROR);   // 前端只看到"系统内部错误"，不暴露细节
    }
}
```

**异常 → 响应 对照表（全部来自该文件）**：

| 异常 | HTTP 状态 | 返回 message |
|---|---|---|
| `BusinessException` | 200（业务错误靠 code 区分，不置 4xx） | 业务自定义 |
| `MethodArgumentNotValidException` | 400 | 各字段 message 拼接 |
| `BindException` | 400 | 同上 |
| `ConstraintViolationException` | 400 | 同上 |
| `MissingServletRequestParameterException` | 400 | "缺少请求参数: xxx" |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不允许 |
| `NoHandlerFoundException` | 404 | 资源不存在 |
| `IllegalArgumentException` | 400 | 原始消息 |
| `Exception`（兜底） | 500 | "系统内部错误"（**详情只进日志**） |

> **两个"书上不写但线上必须知道"的点**：
> ① `NoHandlerFoundException`（404 想走异常链）需要在配置里打开：默认 Spring Boot 的 404 由容器直接处理、**不进** `@ControllerAdvice`。开关是 `spring.mvc.throw-exception-if-no-handler-found=true`，且要关掉静态资源默认处理（否则静态资源兜底会先接住）。本项目在 `GlobalExceptionHandler` 里写了这个 handler，属于"预留能力"。
> ② 兜底 `Exception` handler **一定不能把堆栈返回给前端**（信息泄露），项目做法：日志 `log.error` 全栈 + 响应只给固定文案。

---

## 三、Filter vs Interceptor vs AOP：三方对比（面试必考）

这三者都能"拦截请求"，但**位置、能力、归属**完全不同。项目里恰好各有一个真实实体，正好对照：

| 维度 | Filter | HandlerInterceptor | Spring AOP |
|---|---|---|---|
| **规范归属** | Servlet 容器（Tomcat） | Spring MVC | Spring AOP（代理） |
| **项目实体** | `HeaderAuthenticationFilter`（user 服务） | `TraceInterceptor`（chat 可观测性） | `IdempotentAspect`（common 幂等） |
| 能拿到什么 | `ServletRequest` / `Response`，**不知道目标方法** | `handler`（知道要执行哪个方法）、`ModelAndView` | 方法、参数、返回值、异常，**最全** |
| 怎么注册 | `@Component` 自动 / `FilterRegistrationBean` | `WebMvcConfigurer#addInterceptors` | `@Aspect` + `@Around`（切点决定范围） |
| 能否改参数 | 能（包一层 request） | 能（preHandle 阶段） | 能（改 `joinPoint.getArgs()`） |
| 典型用途 | 编码、跨域、安全过滤 | 鉴权上下文、trace、日志 | 事务、缓存、幂等、权限注解 |

**执行顺序（一个请求的完整"穿过"过程）**：

```text
Filter.preHandle（Before）
  │
  ├─ DispatcherServlet
  │    ├─ Interceptor.preHandle          ← TraceInterceptor：开始 trace
  │    │     └─ AOP 环绕通知.前          ← IdempotentAspect：占位
  │    │           └─ ★ Controller 方法执行 ★
  │    │     └─ AOP 环绕通知.后          ← 业务成功：保留占位
  │    ├─ Interceptor.postHandle         ← 视图渲染前（JSON 场景很少用）
  │    └─ Interceptor.afterCompletion    ← 无论成败都执行：TraceInterceptor 在这里落库 + 清理
  │
Filter.preHandle（After）→ 返回响应
```

**项目里的"为什么选它"注释（可以直接背去面试）**：

```java
// ai-cs-chat/src/main/java/com/aics/chat/config/ObservabilityWebConfig.java:15-22（类注释节选）
// 为什么用 MVC 拦截器而不是 Filter 或 AOP？
//  - 拦截器能拿到精确的 URL 路径匹配：addPathPatterns("/chat/**", "/agent/**") 精确圈定，
//    Filter 则要手写路径判断；
//  - 与 MVC 生命周期契合：preHandle 在 controller 前、postHandle 在渲染前触发，
//    天然适配"请求进入开始 trace、响应结束结束 trace"的语义；AOP 无法感知 URL 匹配。
```

**记忆口诀**：

> **Filter 管"进不进得来"（容器级）；Interceptor 管"哪个接口、要不要记录"（MVC 级）；AOP 管"哪个方法、事务缓存权限"（方法级）。**
> 范围：Filter 最大 → Interceptor 中 → AOP 最小（最贴业务方法）。

### 3.1 一个容易答错的细节

**异常从哪一层抛出，能不能被 `@RestControllerAdvice` 接住？**

| 抛出位置 | 能否被全局异常处理器接住 |
|---|---|
| Controller / Service / Interceptor / AOP | ✅ 能（都在 DispatcherServlet 的异常链里） |
| **Filter**（在 DispatcherServlet 之前） | ❌ 不能！Filter 里的异常不会进 `HandlerExceptionResolver`（项目 `HeaderAuthenticationFilter` 因此不做业务校验，异常一律交给后续链路） |

---

## 四、SSE：一条"不走寻常路"的请求

AI 客服的流式对话（`ai-cs-chat`）用的是 **SSE（Server-Sent Events）**，它的返回值和普通接口完全不同：

```java
// ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java:167-172
@PostMapping(value = "/stream/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStreamSse(@RequestParam("sessionId") @NotBlank(message = "会话ID不能为空") String sessionId,
                                @RequestParam("message") @NotBlank(message = "消息内容不能为空") String message,
                                @RequestParam(value = "knowledgeBase", required = false) String knowledgeBase,
                                ...) { ... }
```

**时序上与普通请求的差异**：

```text
普通请求：Tomcat 线程 ──► 方法返回 Result ──► 序列化 JSON ──► 响应结束，线程归还
SSE 请求：Tomcat 线程 ──► 方法返回 SseEmitter（"预约单"）──► ★线程立即归还线程池★
                                     │
                                     └── 后台（LLM 的 Flux 流）：每来一个 token，
                                         emitter.send(data) 推送一次，保持连接不断开，
                                         直到 emitter.complete() 才真正结束
```

由此推出三个"必须知道"：

1. **SSE 接口的业务方法要"快速返回 emitter"**，不能在里面阻塞等 LLM 全部生成完；
2. **ThreadLocal 上下文跨线程会丢**：所以项目在异步链路用 `TraceContextHolder.capture()/restore()` 显式传播（见 `TraceInterceptor` 类注释与 [05-AI集成/01/05](../05-AI集成/01-SpringAI框架集成/05-LLM可观测性评估与成本治理实现文档.md)）；
3. **拦截器 `afterCompletion` 的时机**：异步请求下，它在**异步处理完成（emitter 完成）后**才触发——这正是项目"trace 落库"能覆盖整段流式对话的原因（`TraceInterceptor:68-71` 注释明确写了"SSE 场景则在 emitter 完成后触发"）。

> SSE 与 WebSocket 的选型对比见 [04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md)。

---

## 五、这条链路上的 5 个高频坑

### 5.0 先对号入座：现象 → 大概率出错的站（排查映射表）

接口"不对劲"时，别急着翻业务代码，先按**现象**定位到站，再决定去哪翻日志：

| 前端看到的现象 | 大概率出错的站 | 定位线索 / 本项目现状 |
|---|---|---|
| 404 | **站4** HandlerMapping 没找到方法 | 路径拼错 / `@RequestMapping` 前缀对不上 / 类不在包扫描范围；要统一 `Result` 需开 `throw-exception-if-no-handler-found` |
| 400 "缺少请求参数: xxx" | **站5** 参数解析 | `MissingServletRequestParameterException`（有专门 handler） |
| 400 且 message 是 DTO 里的文案 | **站6** `@Valid` 校验 | `MethodArgumentNotValidException` / `ConstraintViolationException` |
| 405 "请求方法不允许" | **站4** | 用 GET 打了 `@PostMapping` 接口（有专门 handler） |
| 500"系统内部错误"但明明是参数问题 | **站5** 类型转换 / Content-Type | `MethodArgumentTypeMismatchException`、`HttpMediaTypeNotSupportedException`（本为 415）本项目**无专门 handler → 落兜底 500**，建议各补一个 handler |
| 401 / 403 | **站2** Security 过滤链 | `UserSecurityConfig` 的 `authenticationEntryPoint` / `accessDeniedHandler` |
| 请求一直卡住、超时 | **站7** 业务执行 | 慢 SQL、下游 Feign 阻塞、连接池耗尽；`jstack` 看 Tomcat 线程在等谁 |
| 收到的不是 `Result` 结构 | 异常发生在 DispatcherServlet **之前** | Filter 里抛的异常 `@ControllerAdvice` 接不住（见 3.1 节） |

> 这张表的用法：**先分类"谁的锅"（路由 / 参数 / 业务 / 安全），再决定去哪翻日志**——比从头 debug 快一个数量级。

1. **参数名丢失**：`@RequestParam String sessionId` 在没开 `-parameters` 的工程里会报"找不到参数名"。
   项目根 POM 已开启 `maven.compiler.parameters`；@RequestParam/@RequestHeader 显式写名字更保险（项目全程显式写名）。
2. **`@RequestBody` 的流只能读一次**：Filter/Interceptor 里如果读了 body，后续 Jackson 反序列化会拿到空。
   需要多次读取就包装 `ContentCachingRequestWrapper`（项目未做，因为没这个需求）。
3. **校验注解不生效**：DTO 上写了 `@NotNull` 但方法参数**忘了加 `@Valid`**——最常见的一种"静默失效"。
4. **404 不走异常**：想统一 `Result` 结构就要开 `throw-exception-if-no-handler-found`，否则前端拿到的是容器默认 404 页面/空体。
5. **Filter 里的 ThreadLocal 不清理**：Tomcat 线程会复用，**下个请求可能带着上个请求的身份**。
   项目 `HeaderAuthenticationFilter` 用 `finally { SecurityContextHolder.clearContext(); }` 锁死这个坑；`TraceInterceptor` 同理（`TraceContextHolder.clear()`）。

---

## 六、面试要点总结

> 一句话主线：**请求在 Servlet 容器里穿过 Filter → DispatcherServlet → HandlerMapping → HandlerAdapter（参数解析+校验）→ Controller → 返回值序列化；异常统一从 HandlerExceptionResolver 出口走 `@RestControllerAdvice`。**

```text
关键词：
九站链路 · DispatcherServlet（前端控制器不干业务）· HandlerMapping（URL→方法）· HandlerAdapter（参数解析）
四取数口：@RequestHeader / @RequestParam / @PathVariable / @RequestBody · ConversionService 类型转换
校验三异常：MethodArgumentNotValid（@RequestBody）/ BindException（对象绑定）/ ConstraintViolation（方法级 @Validated）
@Valid（标准）vs @Validated（Spring 方法级）· @RestController = @Controller + @ResponseBody
Filter（容器级，接不住 @ControllerAdvice）→ Interceptor（MVC 级，能拿 handler）→ AOP（方法级，能改参数返回值）
项目锚点：HeaderAuthenticationFilter（唯一 Filter，finally 清 ThreadLocal）· TraceInterceptor（唯一拦截器，afterCompletion 落库）· CartController+CartAddDTO（参数与校验）· GlobalExceptionHandler（异常出口）· ChatController SseEmitter（异步请求 afterCompletion 时机不同）
```

### 高频追问链

1. **问：一个请求进来，Spring MVC 内部发生了什么？**
   答：按九站说：Filter → DispatcherServlet → HandlerMapping 找方法 → HandlerAdapter 解析参数（含 @Valid 校验）→ 反射调 Controller → 返回值经 HttpMessageConverter 序列化 → 异常走 HandlerExceptionResolver。
2. **追问：Filter、Interceptor、AOP 有什么区别？**
   答：归属不同（Servlet / MVC / Spring AOP）、能力不同（拿不到方法 / 拿到 handler / 拿到方法参数返回值）、顺序不同（外→内）；项目各有一个实体，分别是 `HeaderAuthenticationFilter`、`TraceInterceptor`、`IdempotentAspect`。
3. **再追问：Filter 里抛异常，全局异常处理器能接住吗？**
   答：不能。DispatcherServlet 之外抛出的异常不经过 `HandlerExceptionResolver`；要统一处理得在 Filter 内部自己写响应（Security 的 `authenticationEntryPoint` 就是这么做的）。
4. **再追问：参数校验失败为什么没有进入 Controller 方法？**
   答：校验在 HandlerAdapter 参数解析阶段完成，失败抛 `MethodArgumentNotValidException`，方法体尚未执行。
5. **再追问：SSE 接口为什么返回值是 `SseEmitter` 而不是 String？**
   答：需要"先返回、后持续推送"，SseEmitter 让请求线程立即归还，后续 token 由后台线程推送；也导致 ThreadLocal 上下文需要显式跨线程传播。

---

## 学习检查清单

- [ ] 能按顺序画出九站旅程，并说出每站的一句话职责
- [ ] 能区分四个取数注解的数据来源，并各举一个项目用法
- [ ] 能说清 `@Valid` 触发校验的完整链路（从 DTO 注解到 GlobalExceptionHandler）
- [ ] 能背出三种校验异常各自对应的场景
- [ ] 能用一张表说出 Filter / Interceptor / AOP 的三点区别 + 项目实体
- [ ] 能解释"Filter 抛异常为什么接不住"和"404 为什么要开开关才走异常链"
- [ ] 能解释 SSE 接口的线程模型与 afterCompletion 时机差异

## 下一步

- [18-Spring 面试专练与追问链](18-Spring面试专练与追问链.md)：把 01~17 篇的知识点压缩成一份可直接背的面试答卷；
- [02-Spring微服务/README](README.md)：本模块 19 篇的完整地图与学习路径。
