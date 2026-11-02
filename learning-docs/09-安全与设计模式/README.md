# 09-安全与设计模式

> 设计模式是"重用"，安全设计是"避坑"。两者结合才能写出"既优雅又不会被黑"的代码。

---

## 📚 章节导航

| 章节 | 核心内容 | 难度 | 适用场景 |
|------|---------|------|---------|
| 创建型模式 | Singleton / Factory / Builder / Prototype | ⭐⭐ | 对象创建优化 |
| 结构型模式 | Proxy / Adapter / Facade / Decorator | ⭐⭐⭐ | 系统解耦 |
| 行为型模式 | Strategy / Observer / State / Template | ⭐⭐⭐ | 业务编排 |
| 企业级模式 | DDD / Event Sourcing / CQRS | ⭐⭐⭐⭐ | 大型系统 |
| 常见漏洞 | SQL 注入 / XSS / CSRF / 文件穿越 | ⭐⭐⭐ | 安全防御 |
| 密码与加密 | 密码存储 / SSL/TLS / 密钥管理 | ⭐⭐⭐ | 数据保护 |
| 授权与认证 | JWT / OAuth2 / 权限模型 | ⭐⭐⭐ | 用户管理 |

---

## 🎯 学习路线

### 快速入门（3 天）
```
创建型 → 结构型 → 行为型
```

### 安全优先（1 周）
```
常见漏洞 → 密码加密 → JWT 认证 → 授权模型
```

### 架构升级（2 周）
```
设计模式 → 企业级模式（DDD/CQRS/Event Sourcing）→ 安全设计综合
```

---

## 💡 核心设计模式

### 创建型：Singleton vs Factory

```java
// ❌ 多线程不安全的 Singleton
class Config {
    private static Config instance;
    private Config() {}
    
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();  // 竞态条件！
        }
        return instance;
    }
}

// ✅ 双检查锁 Singleton
class Config {
    private static volatile Config instance;
    private Config() {}
    
    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}

// ✅ 最简单的 Singleton（Java 推荐）
class Config {
    public static final Config INSTANCE = new Config();
    private Config() {}
}
```

### 结构型：Proxy 模式

```java
// 应用1：事务代理（Spring AOP 原理）
interface OrderService {
    void createOrder(Order order);
}

class OrderServiceProxy implements OrderService {
    private OrderService target;
    
    @Override
    public void createOrder(Order order) {
        try {
            beginTransaction();
            target.createOrder(order);
            commit();
        } catch (Exception e) {
            rollback();
            throw e;
        }
    }
}

// 应用2：动态代理（运行时生成代理类）
OrderService proxy = (OrderService) Proxy.newProxyInstance(
    OrderService.class.getClassLoader(),
    new Class[]{OrderService.class},
    new InvocationHandler() {
        @Override
        public Object invoke(Object o, Method method, Object[] args) {
            System.out.println("前处理");
            Object result = method.invoke(target, args);
            System.out.println("后处理");
            return result;
        }
    }
);
```

### 行为型：Strategy 模式

```java
// 应用：不同支付方式的策略

interface PaymentStrategy {
    PaymentResult pay(Order order, BigDecimal amount);
}

class AliPayStrategy implements PaymentStrategy {
    @Override
    public PaymentResult pay(Order order, BigDecimal amount) {
        // 调用支付宝 API
        return new PaymentResult(...);
    }
}

class WeChatPayStrategy implements PaymentStrategy {
    @Override
    public PaymentResult pay(Order order, BigDecimal amount) {
        // 调用微信 API
        return new PaymentResult(...);
    }
}

// 使用
class PaymentService {
    private Map<String, PaymentStrategy> strategies = Map.of(
        "alipay", new AliPayStrategy(),
        "wechat", new WeChatPayStrategy()
    );
    
    public PaymentResult processPayment(Order order, String channel) {
        PaymentStrategy strategy = strategies.get(channel);
        return strategy.pay(order, order.getAmount());
    }
}
```

---

## 🔒 安全设计

### 1. 常见漏洞与防御

| 漏洞 | 原理 | 防御 | 验证 |
|------|------|------|------|
| **SQL 注入** | 未净化用户输入 | 参数化查询 / 预编译 | 用 `?` 占位符 |
| **XSS** | 未转义 HTML / JS | 输出编码 / CSP | 浏览器检查 |
| **CSRF** | 跨站请求伪造 | Token + SameSite | Spring Security |
| **文件穿越** | `../../../etc/passwd` | 路径白名单 / 规范化 | 正则校验 |
| **命令注入** | 拼接系统命令 | 避免 `Runtime.exec` | 使用库替代 |

### 2. 密码存储（永远不存明文！）

```java
// ❌ 千万别这样
String password = "123456";  // 明文存库 = 送命！

// ✅ 用 bcrypt 加盐哈希
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("123456");
// 每次加盐随机，即使同一密码也产生不同哈希

// 验证密码
boolean isCorrect = encoder.matches("123456", hashedPassword);
```

### 3. JWT 认证（有状态 vs 无状态）

```java
// JWT 令牌结构
// Header.Payload.Signature
// eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiYWxpY2UifQ.xxxx

// 签发
@RestController
public class AuthController {
    
    private static final String SECRET = "your-secret-key-at-least-32-chars";
    
    @PostMapping("/login")
    public Map<String, String> login(String username, String password) {
        if (authenticate(username, password)) {
            String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))  // 1 小时
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
            return Map.of("token", token);
        }
        return Map.of("error", "Invalid credentials");
    }
}

// 验证（Spring Security 拦截器）
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 4. 权限模型（RBAC）

```java
// 角色 (Role)
@Entity
class Role {
    @Id
    Long id;
    String name;  // "ADMIN", "USER", "GUEST"
    @ManyToMany
    Set<Permission> permissions;
}

// 权限 (Permission)
@Entity
class Permission {
    @Id
    Long id;
    String name;  // "user:read", "user:write", "order:delete"
}

// 用户 (User)
@Entity
class User {
    @Id
    Long id;
    String username;
    @ManyToMany
    Set<Role> roles;
}

// 检查权限
@Service
public class AuthService {
    public boolean hasPermission(User user, String permission) {
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(p -> p.getName().equals(permission));
    }
}

// 使用 Spring Security 注解
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('order:create')")
    public Order createOrder(@RequestBody OrderRequest req) {
        // 只有有 "order:create" 权限的用户才能调用
        return orderService.create(req);
    }
}
```

---

## 📊 设计模式速查表

| 问题 | 推荐模式 | 关键词 |
|------|----------|--------|
| 对象创建太复杂 | Builder / Factory | 拆分创建逻辑 |
| 需要缓存对象 | Singleton / Flyweight | 单一实例 |
| 想在方法前后加逻辑 | Proxy / Decorator | 横切关注点 |
| 不同算法切换 | Strategy | 行为变化 |
| 需要通知多个对象 | Observer / Publish-Subscribe | 松耦合 |
| 复杂对象关联 | Facade | 隐藏复杂度 |
| 需要undo/redo | Command / Memento | 操作回溯 |

---

## 🔧 实战练习

### 练习 1：订单状态机（State 模式）

```java
// 状态接口
interface OrderState {
    void pay(Order order);
    void ship(Order order);
    void receive(Order order);
}

// 不同状态实现
class PendingState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("已支付");
        order.setState(new PaidState());
    }
    
    @Override
    public void ship(Order order) {
        throw new IllegalStateException("需要先支付");
    }
}

// 使用
Order order = new Order();
order.setState(new PendingState());
order.pay();        // 允许
order.receive();    // 异常！
```

### 练习 2：实现一个简单的认证与授权系统

要求：
- [ ] 用户登录返回 JWT Token
- [ ] 不同角色有不同权限
- [ ] API 端点根据权限拒绝 / 允许访问

---

## ⚡ 安全检查清单

```
认证 & 授权
□ 所有非公开端点都需要认证
□ JWT Token 有过期时间
□ 密码用 bcrypt 加盐存储
□ 每个操作都有权限检查

输入验证
□ 所有用户输入都验证（长度、格式、范围）
□ 参数化查询（防 SQL 注入）
□ 文件上传有类型和大小限制
□ 文件路径规范化（防穿越）

数据保护
□ 敏感数据加密（密码、支付信息）
□ HTTPS 必须开启（生产环境）
□ 日志不记录敏感信息
□ 数据库连接用 SSL

错误处理
□ 不泄露系统细节（500 返回通用错误）
□ 异常日志详细，但用户只看"稍后重试"
□ 敏感操作加审计日志

依赖管理
□ 定期更新依赖（特别是安全补丁）
□ 扫描漏洞（OWASP Dependency-Check）
□ 禁止使用已知有漏洞的版本
```

---

## 🔗 与其他模块关联

```
09-安全与设计模式
  ├── ← 02-Spring微服务（Spring Security / AOP）
  ├── ← 03-数据库与ORM（SQL 注入防御）
  ├── → 07-运维部署（HTTPS / 证书）
  └── → 22-Python速成/23 篇（安全编码）
```

---

## 📚 推荐资源

- [Gang of Four Design Patterns](https://en.wikipedia.org/wiki/Design_Patterns)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [NIST 密码学指南](https://csrc.nist.gov/publications/detail/sp/800-175b/final)
- [22-Python速成/23 篇 安全编码](../22-Python速成/23-安全编码与常见漏洞.md)

---

**下一步**：从最常用的 3 个模式（Singleton / Proxy / Strategy）开始实践，再补充安全检查清单。
