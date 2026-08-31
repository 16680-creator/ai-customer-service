# 03-Spring Security 微服务两层安全模型（01-P1 落地记录）

> 2026-08 落地：保留现有 JWT 登录，不重写认证；建立「网关认证 + 下游服务授权」
> 两层模型，user 服务作为首个试点。gateway 29、user 22 测试全绿。

## 一、为什么不是每个服务都重新校验 JWT

微服务安全分两层关注点：

```
外部请求 → Gateway AuthFilter（认证：JWT/API Key 真伪）
         → 清洗客户端伪造的 X-User-* 头
         → 注入可信身份 X-User-Id / X-User-Name / X-User-Roles
         → 下游 HeaderAuthenticationFilter（把可信头转 SecurityContext）
         → @PreAuthorize（授权：这个身份能否执行方法）
```

- 网关认证一次：避免 11 个服务重复 JWT 密钥/解析逻辑
- 服务授权各自负责：权限规则靠近业务方法，网关不需要知道每个接口的领域规则
- 代价/部署红线：客户端必须**不能直连服务端口**；K8s NetworkPolicy/防火墙只允许网关和
  内部服务访问下游。否则攻击者可伪造 X-User-Roles 绕过网关

## 二、网关可信角色透传

JWT payload 已有 `role` claim（UserServiceImpl 登录时写入）。AuthFilter 新增：

1. 所有路径（含白名单）先 remove 客户端传入的 `X-User-Roles`
2. JWT 合法后取 role，标准化为 `ROLE_ADMIN/ROLE_USER/ROLE_AGENT`
3. API Key 身份赋 `ROLE_SERVICE`
4. 再注入头转发

```java
String role = claims.getOrDefault("role", "USER").toString();
String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
request.mutate().header("X-User-Roles", normalized);
```

为什么要 `ROLE_` 前缀：Spring Security 的 `hasRole('ADMIN')` 内部自动比较
`ROLE_ADMIN`；如果直接透传 admin，永远授权失败。

## 三、下游 HeaderAuthenticationFilter

user 的 `HeaderAuthenticationFilter extends OncePerRequestFilter`：

- principal = userId（字符串），所以 SpEL 可用 `authentication.name`
- authorities = 逗号分隔 roles → SimpleGrantedAuthority
- details = username（审计日志可用）
- finally 必须 `SecurityContextHolder.clearContext()`：SecurityContext 默认 ThreadLocal，
  Tomcat 线程复用时不清会造成用户身份串线

本过滤器**不验 JWT**，只做协议适配；身份真实性依赖网关清洗 + 网络隔离。

## 四、SecurityFilterChain（无状态资源服务）

```java
http.csrf(csrf -> csrf.disable())
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/user/register", "/user/login", "/actuator/**", "/swagger-ui/**").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(headerFilter, UsernamePasswordAuthenticationFilter.class);
```

- `STATELESS`：不用 Session；每次请求身份来自网关头
- CSRF 关闭：CSRF 针对浏览器 Cookie 自动携带凭证；本项目 Authorization Bearer，不依赖 Cookie
- 401/403 的区别：未认证 → AuthenticationEntryPoint 401；已认证但权限不足 → AccessDeniedHandler 403
- 两个 handler 均返回项目统一 `Result` JSON（code/message/data/timestamp），前端无需特殊适配

## 五、方法级授权：本人或 ADMIN

```java
@PreAuthorize("authentication.name == #p0.toString() or hasRole('ADMIN')")
@GetMapping("/{id}")
Result<User> getUserById(@PathVariable("id") Long id)

@PreAuthorize("authentication.name == #p0.id.toString() or hasRole('ADMIN')")
@PutMapping
Result<Void> updateUser(@RequestBody User user)
```

这里用 `#p0` 而不是 `#id`：项目 Maven 编译未启用 `-parameters`，运行时反射拿不到参数名，
用 `#id` 会报 `Name for argument not specified`。参数索引 `#p0` 不依赖编译参数，跨环境稳定。

## 六、测试覆盖

| 测试 | 覆盖 |
|------|------|
| AuthFilterTest | 伪造身份头清洗、JWT role → ROLE_ADMIN 可信透传 |
| HeaderAuthenticationFilterTest | principal/多角色标准化/请求后 ThreadLocal 清理/匿名 |
| UserAuthorizationContractTest | `@PreAuthorize` 表达式契约 |
| UserSecurityIntegrationTest（MockMvc 真 FilterChain） | 无头 401、普通用户查他人 403、本人 200、ADMIN 200、统一 JSON |

验证：`mvn -pl ai-cs-gateway,ai-cs-user -am verify`，gateway 29 + user 22 测试全绿。

## 七、后续 Starter 化

当前 HeaderAuthenticationFilter/UserSecurityConfig 放在 user 试点模块，避免 Security 依赖经 common
传递后一次锁死全部服务。01-P4 自定义 Starter 阶段将提取为条件自动装配：

- classpath 有 SecurityFilterChain 才装配
- `aics.security.enabled=true` 才开启
- 各服务只需引 starter + 写方法级规则

## 八、面试要点速记

- 认证（你是谁）vs 授权（你能做什么）在微服务里的边界
- SecurityFilterChain 的顺序与 OncePerRequestFilter 放置点
- `@PreAuthorize` 由 MethodSecurityInterceptor/AOP 代理生效，直接 new Controller 不生效
- 401 vs 403；STATELESS / CSRF 关闭理由
- 网关可信头模型的安全前提：头清洗 + 内网服务不可公网直连
