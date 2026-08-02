# JWT 鉴权与异常处理

> 本项目在 `ai-cs-common` 模块实现了统一的 JWT 鉴权和全局异常处理。
> 对应项目文件：`ai-cs-common/src/main/java/com/aics/common/`

---

## 一、认证与授权

```
认证（Authentication）：你是谁？  → 登录、验证身份
授权（Authorization）：你能做什么？ → 权限控制

本项目的方案：
  用户登录 → 服务端生成 JWT Token → 前端每次请求带上 Token → 网关验证
```

---

## 二、JWT 原理

### 2.1 JWT 结构

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAiLCJyb2xlIjoiYWRtaW4ifQ.abc123signature
|____Header____|  |____________Payload____________|  |____Signature____|

Header:    算法信息（HS256）
Payload:   数据（用户ID、角色、过期时间）
Signature: 签名（防篡改）
```

### 2.2 工作流程

```
1. 用户登录 → POST /api/user/login {username, password}
2. 服务端验证密码 → 生成 JWT Token → 返回给前端
3. 前端存储 Token（localStorage）
4. 后续请求 → Header: Authorization: Bearer <token>
5. 网关验证 Token → 解析用户信息 → 转发给下游服务
```

---

## 三、本项目的 JwtUtil 实现

```java
// ai-cs-common/src/main/java/com/aics/common/util/JwtUtil.java
public class JwtUtil {

    private static final String DEFAULT_SECRET = "aics-platform-jwt-secret-key-...";
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;  // 24小时

    /**
     * 生成 Token
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + DEFAULT_EXPIRATION);
        SecretKey key = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .subject(subject)          // 用户 ID
            .claims(claims)            // 自定义数据（角色等）
            .issuedAt(now)             // 签发时间
            .expiration(expiryDate)    // 过期时间
            .signWith(key)             // 签名
            .compact();
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期");
            return false;
        } catch (Exception e) {
            log.warn("JWT Token 无效");
            return false;
        }
    }

    /**
     * 判断是否即将过期（用于无感刷新）
     */
    public static boolean isTokenExpiringSoon(String token, String secret, long thresholdMillis) {
        Claims claims = parseToken(token, secret);
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return remaining < thresholdMillis;
    }
}
```

---

## 四、登录接口示例

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        // 1. 验证用户名密码
        User user = userService.verifyCredentials(req.getUsername(), req.getPassword());
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 2. 生成 Token
        String token = JwtUtil.generateToken(
            user.getId().toString(),
            Map.of("username", user.getUsername(), "role", user.getRole())
        );

        // 3. 返回
        return Result.success(Map.of(
            "token", token,
            "userId", user.getId(),
            "username", user.getUsername()
        ));
    }
}
```

---

## 五、统一返回体

```java
// ai-cs-common/src/main/java/com/aics/common/result/Result.java
@Data
public class Result<T> implements Serializable {
    private int code;        // 状态码
    private String message;  // 提示信息
    private T data;          // 响应数据
    private long timestamp;  // 时间戳

    // 成功
    public static <T> Result<T> success(T data) { ... }
    
    // 失败
    public static <T> Result<T> fail(String message) { ... }
    public static <T> Result<T> fail(ResultCode resultCode) { ... }
}

// 状态码枚举
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数错误");
}
```

---

## 六、全局异常处理

```java
// ai-cs-common/src/main/java/com/aics/common/exception/GlobalExceptionHandler.java
@RestControllerAdvice    // 全局拦截所有 Controller 的异常
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常（可预期的错误）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 未知异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

### 异常处理流程

```
Controller 抛出异常
       ↓
GlobalExceptionHandler 捕获
       ↓
根据异常类型选择处理方法
       ↓
返回统一的 Result JSON（而不是 500 错误页面）
```

---

## 七、安全最佳实践

| 实践 | 说明 |
|------|------|
| 密钥不要硬编码 | 从环境变量/配置中心读取 |
| Token 设置合理过期时间 | 本项目 24 小时 |
| HTTPS 传输 | 防止 Token 被截获 |
| 密码加密存储 | BCrypt，不要明文 |
| 接口限流 | 防止暴力破解 |
| 输入校验 | @Valid + 自定义校验 |
| SQL 注入防护 | 用 MyBatis-Plus 参数化查询 |
| XSS 防护 | 前端转义、CSP 头 |

---

## 八、设计模式（本项目中的体现）

| 模式 | 在项目中的体现 |
|------|--------------|
| 模板方法 | GlobalExceptionHandler 统一处理流程 |
| 策略模式 | 不同消息通知渠道（短信/邮件/推送） |
| 观察者模式 | RocketMQ 消息发布/订阅 |
| 责任链模式 | Gateway 过滤器链 |
| 建造者模式 | Jwts.builder()、QueryWrapper |
| 工厂模式 | Result.success() / Result.fail() |
| 单例模式 | Spring Bean 默认单例 |

---

## 九、动手练习

1. 用 JwtUtil 生成一个 Token，用 [jwt.io](https://jwt.io) 解码查看内容
2. 修改过期时间为 10 秒，验证过期后 validateToken 返回 false
3. 写一个接口，故意抛出 BusinessException，观察返回格式
4. 给登录接口加 @Valid 参数校验
5. 在 Gateway 过滤器中打印解析出的用户 ID

---

## 学习检查清单

- [ ] 理解 JWT 的三段结构和签名原理
- [ ] 会生成、解析、验证 Token
- [ ] 理解统一返回体 Result<T> 的设计
- [ ] 理解全局异常处理的工作流程
- [ ] 知道常见的安全最佳实践
- [ ] 能识别项目中使用的设计模式

---

## 恭喜完成全部学习文档！

回到 [学习路线总览](../00-学习路线总览/README.md) 查看完整路径。
