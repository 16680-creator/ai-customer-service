# Spring Boot 核心原理

> 本项目所有微服务都基于 **Spring Boot 3.2.5** 构建。
> 理解 Spring Boot 的自动配置、分层架构、依赖注入是看懂整个项目的基础。

---

## 一、Spring Boot 是什么？

一句话：**Spring Boot = Spring 框架 + 自动配置 + 内嵌服务器 + Starter 依赖**

它解决的核心问题：
- 不用写一堆 XML 配置
- 不用单独部署 Tomcat
- 引入一个 Starter 就能用一整套功能

---

## 二、本项目的分层架构

以 `ai-cs-order`（订单服务）为例：

```
ai-cs-order/src/main/java/com/aics/order/
├── controller/          ← 控制层：接收 HTTP 请求
│   ├── CartController.java
│   ├── OrderController.java
│   └── CheckoutController.java
├── service/             ← 业务层：核心逻辑
│   ├── CartService.java         (接口)
│   ├── OrderService.java        (接口)
│   └── impl/
│       ├── CartServiceImpl.java (实现)
│       └── OrderServiceImpl.java(实现)
├── mapper/              ← 数据层：操作数据库
│   ├── CartItemMapper.java
│   └── OrderMapper.java
├── entity/              ← 实体类：对应数据库表
│   ├── CartItem.java
│   └── Order.java
├── vo/                  ← 视图对象：返回给前端的数据结构
│   └── CartVO.java
├── listener/            ← 消息监听器
│   └── OrderTimeoutListener.java
└── OrderApplication.java ← 启动类
```

### 请求处理流程

```
前端请求 → Gateway(8080) → OrderController(8087)
                                    ↓
                              OrderService（业务逻辑）
                                    ↓
                              OrderMapper（数据库操作）
                                    ↓
                              MySQL（数据存储）
```

---

## 三、核心注解详解

### 3.1 启动类

```java
// 每个微服务都有一个启动类（如 ai-cs-chat 的 ChatApplication.java）
@SpringBootApplication  // 这是一个组合注解，包含下面三个
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}

// @SpringBootApplication = 
//   @SpringBootConfiguration  → 标记为配置类
//   @EnableAutoConfiguration  → 开启自动配置（核心！）
//   @ComponentScan           → 扫描当前包及子包的 @Component
```

### 3.2 控制层注解

```java
@RestController  // = @Controller + @ResponseBody（返回值自动转 JSON）
@RequestMapping("/api/cart")  // 基础路径
public class CartController {

    @Autowired
    private CartService cartService;  // 依赖注入

    @GetMapping("/list")  // GET /api/cart/list?userId=100
    public Result<CartVO> getCart(@RequestParam Long userId) {
        return Result.success(cartService.getCartList(userId));
    }

    @PostMapping("/add")  // POST /api/cart/add
    public Result<CartVO> addToCart(@RequestBody @Valid AddCartRequest req) {
        return Result.success(cartService.addItem(req));
    }

    @PutMapping("/quantity")  // PUT /api/cart/quantity
    public Result<CartVO> updateQty(@RequestParam Long userId,
                                     @RequestParam Long itemId,
                                     @RequestParam int quantity) {
        return Result.success(cartService.updateQuantity(userId, itemId, quantity));
    }

    @DeleteMapping("/{itemId}")  // DELETE /api/cart/1
    public Result<Void> delete(@PathVariable Long itemId,
                               @RequestParam Long userId) {
        cartService.deleteCartItem(userId, itemId);
        return Result.success();
    }
}
```

### 3.3 服务层注解

```java
// 接口定义（面向接口编程）
public interface CartService {
    CartVO getCartList(Long userId);
    CartVO updateQuantity(Long userId, Long itemId, int quantity);
    void deleteCartItem(Long userId, Long itemId);
}

// 实现类
@Service  // 标记为 Spring 管理的 Bean
public class CartServiceImpl implements CartService {
    
    @Autowired
    private CartItemMapper cartItemMapper;  // 注入 Mapper
    
    @Autowired
    private StringRedisTemplate redisTemplate;  // 注入 Redis
    
    @Override
    @Transactional  // 开启事务（方法内数据库操作要么全成功，要么全回滚）
    public CartVO updateQuantity(Long userId, Long itemId, int quantity) {
        // 1. 参数校验
        if (quantity <= 0) throw new IllegalArgumentException("数量必须大于0");
        
        // 2. 查询购物车项
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("购物车项不存在");
        
        // 3. 检查库存（从 Redis 读取）
        String stock = redisTemplate.opsForValue().get("stock:" + item.getProductId());
        if (Integer.parseInt(stock) < quantity) {
            throw new BusinessException("库存不足");
        }
        
        // 4. 更新数据库
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
        
        return getCartList(userId);
    }
}
```

### 3.4 依赖注入的三种方式

```java
// 方式一：字段注入（本项目主要用这种，简单但不推荐）
@Autowired
private CartService cartService;

// 方式二：构造器注入（推荐！便于测试）
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final CartService cartService;
    
    // Lombok 的 @RequiredArgsConstructor 自动生成构造器
    @RequiredArgsConstructor
    public OrderServiceImpl(OrderMapper orderMapper, CartService cartService) {
        this.orderMapper = orderMapper;
        this.cartService = cartService;
    }
}

// 方式三：Setter 注入（可选依赖时用）
@Autowired(required = false)
public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
}
```

---

## 四、自动配置原理（面试高频）

### 4.1 什么是自动配置？

你引入了 `spring-boot-starter-web`，Spring Boot 就自动：
- 启动内嵌 Tomcat（端口 8080）
- 配置 JSON 序列化（Jackson）
- 配置异常处理
- 配置请求日志

你不需要写任何配置代码！

### 4.2 原理（简化版）

```
@SpringBootApplication
    └── @EnableAutoConfiguration
            └── 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
                    └── 里面列出了所有自动配置类
                            └── 每个配置类上有 @Conditional 条件注解
                                    └── 条件满足才生效
```

### 4.3 条件注解

```java
@ConditionalOnClass(DataSource.class)       // classpath 有这个类才生效
@ConditionalOnMissingBean(UserService.class) // 容器中没有这个 Bean 才创建
@ConditionalOnProperty("spring.redis.host")  // 配置文件有这个属性才生效
```

**这就是为什么**：你加了 Redis 依赖 + 配了 `spring.redis.host`，RedisTemplate 就自动可用了。

---

## 五、配置文件详解

本项目的 `application.yml`（以 ai-cs-chat 为例）：

```yaml
server:
  port: 8083                    # 服务端口

spring:
  application:
    name: ai-cs-chat            # 服务名（注册到 Nacos 用）
  cloud:
    nacos:
      discovery:
        enabled: false          # 本地开发关闭注册发现
      config:
        enabled: false          # 本地开发关闭配置中心
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:默认值}   # 优先读环境变量
      base-url: ${OPENAI_BASE_URL:https://api.minimaxi.com}
      chat:
        options:
          model: ${OPENAI_MODEL:MiniMax-M3}
          temperature: 0.7      # 生成随机性（0=确定，1=创意）
          max-tokens: 2048      # 最大回复长度

logging:
  level:
    com.aics.chat: debug        # 自己的代码打 debug 日志
    org.springframework.ai: info # 框架打 info 日志
```

### 配置优先级（从高到低）

```
命令行参数 > 环境变量 > application-{profile}.yml > application.yml > 默认值
```

### 多环境配置

```yaml
# application.yml（公共配置）
spring:
  application:
    name: ai-cs-order

# application-dev.yml（开发环境）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aics_order

# application-prod.yml（生产环境）
spring:
  datasource:
    url: jdbc:mysql://mysql-cluster:3306/aics_order
```

启动时指定：`java -jar app.jar --spring.profiles.active=prod`

---

## 六、统一返回体设计

本项目所有接口都返回 `Result<T>`（在 ai-cs-common 中定义）：

```json
// 成功
{
    "code": 200,
    "message": "操作成功",
    "data": { "id": 1, "name": "无线蓝牙耳机" },
    "timestamp": 1722585600000
}

// 失败
{
    "code": 500,
    "message": "库存不足，当前库存: 5",
    "data": null,
    "timestamp": 1722585600000
}
```

**为什么需要统一返回体？**
- 前端只需要判断 `code === 200` 就知道成功/失败
- 所有服务格式一致，前端不用适配不同格式
- 方便全局异常处理

---

## 七、动手练习

1. 启动 `ai-cs-chat` 服务，访问 `http://localhost:8083/swagger-ui.html` 查看 API 文档
2. 在 `application.yml` 中修改端口为 9999，重启验证
3. 新建一个 Controller，写一个 `/api/hello` 接口返回 `Result.success("Hello World")`
4. 故意抛出一个 `BusinessException`，观察全局异常处理器的返回

---

## 学习检查清单

- [ ] 理解 Controller → Service → Mapper 三层架构
- [ ] 理解 @SpringBootApplication 的组合含义
- [ ] 理解自动配置的触发条件（@Conditional）
- [ ] 会写 RESTful 接口（GET/POST/PUT/DELETE）
- [ ] 理解依赖注入的三种方式
- [ ] 理解 application.yml 的配置优先级
- [ ] 理解统一返回体 Result<T> 的设计目的

---

## 下一步

→ [02-SpringCloud微服务架构](./02-SpringCloud微服务架构.md)
