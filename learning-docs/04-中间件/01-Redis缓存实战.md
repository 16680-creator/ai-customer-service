# Redis 缓存实战

> 本项目使用 **Redis 7** 做缓存、库存扣减、分布式锁、会话存储。
> 对应项目文件：`docker-compose.yml`（Redis 容器）、`ai-cs-order`（购物车库存校验）

---

## 一、Redis 在项目中的用途

```
┌─────────────────────────────────────────────────────┐
│                    Redis 7                            │
│                                                      │
│  用途 1：商品库存缓存                                  │
│  key: stock:{productId}  value: 库存数量              │
│  场景: 购物车修改数量时校验库存（CartServiceTest）       │
│                                                      │
│  用途 2：用户会话/Token 缓存                           │
│  key: token:{userId}     value: JWT Token            │
│  场景: 登录状态管理、Token 黑名单                      │
│                                                      │
│  用途 3：AI 对话上下文                                 │
│  key: chat:{sessionId}   value: 历史消息 JSON         │
│  场景: 多轮对话记忆                                   │
│                                                      │
│  用途 4：分布式锁                                     │
│  key: lock:order:{orderId}                           │
│  场景: 防止重复下单                                   │
│                                                      │
│  用途 5：接口限流                                     │
│  key: rate:{userId}:{api}  value: 请求计数            │
│  场景: Gateway 限流                                   │
└─────────────────────────────────────────────────────┘
```

---

## 二、Docker 部署

```yaml
# docker-compose.yml
redis:
  image: redis:7
  container_name: aics-redis
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data                              # 数据持久化
    - ./deploy/redis/redis.conf:/usr/local/etc/redis/redis.conf
  command: redis-server /usr/local/etc/redis/redis.conf
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### redis.conf 关键配置

```ini
# deploy/redis/redis.conf
bind 0.0.0.0              # 允许外部连接
protected-mode no         # 关闭保护模式（开发用）
port 6379

# 持久化
save 900 1                # 900秒内至少1次修改 → 触发 RDB 快照
save 300 10
appendonly yes            # 开启 AOF（更安全的持久化）
appendfsync everysec      # 每秒刷盘

# 内存
maxmemory 256mb           # 最大内存
maxmemory-policy allkeys-lru  # 内存满时淘汰最久未使用的 key
```

---

## 三、Spring Boot 集成

### 3.1 依赖

```xml
<!-- ai-cs-order/pom.xml 中已有 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 3.2 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0            # 默认 0 号库
    password:              # 无密码（开发环境）
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20     # 最大连接数
        max-idle: 10       # 最大空闲连接
        min-idle: 5        # 最小空闲连接
```

### 3.3 使用 StringRedisTemplate

```java
// 本项目购物车服务中的实际用法（参考 CartServiceTest）
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public CartVO updateQuantity(Long userId, Long itemId, int quantity) {
        // ...
        
        // 从 Redis 读取库存
        String stockStr = stringRedisTemplate.opsForValue().get("stock:" + productId);
        int stock = Integer.parseInt(stockStr);
        
        if (quantity > stock) {
            throw new BusinessException("库存不足，当前库存: " + stock);
        }
        
        // 更新数据库...
    }
}
```

---

## 四、Redis 五种数据结构

| 类型 | 命令 | 本项目用途 |
|------|------|-----------|
| **String** | GET/SET/INCR | 库存数量、Token、计数器 |
| **Hash** | HGET/HSET/HGETALL | 用户信息缓存、购物车 |
| **List** | LPUSH/RPUSH/LRANGE | 消息队列、最近浏览 |
| **Set** | SADD/SMEMBERS/SISMEMBER | 用户标签、共同好友 |
| **ZSet** | ZADD/ZRANGE/ZRANK | 排行榜、热度排序 |

### 实际示例

```java
// String：库存
redisTemplate.opsForValue().set("stock:1001", "100");
redisTemplate.opsForValue().decrement("stock:1001", 2);  // 扣减 2

// Hash：用户信息
redisTemplate.opsForHash().putAll("user:100", Map.of(
    "name", "张三",
    "level", "VIP",
    "points", "5000"
));

// List：AI 对话历史
redisTemplate.opsForList().rightPush("chat:session-1", messageJson);
List<String> history = redisTemplate.opsForList().range("chat:session-1", -10, -1); // 最近10条

// ZSet：搜索热词
redisTemplate.opsForZSet().incrementScore("hot:keywords", "蓝牙耳机", 1);
Set<String> top10 = redisTemplate.opsForZSet().reverseRange("hot:keywords", 0, 9);
```

---

## 五、缓存三大问题

### 5.1 缓存穿透（查不存在的数据）

```
请求: 查商品 ID=999999（不存在）
→ Redis 没有 → MySQL 也没有 → 每次都打到数据库

解决：缓存空值
```

```java
public ProductVO getProduct(Long id) {
    String key = "product:" + id;
    String cached = redisTemplate.opsForValue().get(key);
    
    if (cached != null) {
        if ("NULL".equals(cached)) return null;  // 缓存了空值
        return JSON.parseObject(cached, ProductVO.class);
    }
    
    ProductVO product = productMapper.selectById(id);
    if (product == null) {
        redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);  // 缓存空值 5 分钟
    } else {
        redisTemplate.opsForValue().set(key, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
    }
    return product;
}
```

### 5.2 缓存击穿（热点 key 过期）

```
热点商品缓存过期 → 1000 个并发同时查数据库 → 数据库压力暴增

解决：互斥锁（只让一个线程去查数据库）
```

```java
public ProductVO getProductWithLock(Long id) {
    String key = "product:" + id;
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) return JSON.parseObject(cached, ProductVO.class);
    
    // 获取分布式锁
    String lockKey = "lock:product:" + id;
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
    
    if (Boolean.TRUE.equals(locked)) {
        try {
            // 双重检查
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) return JSON.parseObject(cached, ProductVO.class);
            
            // 查数据库并回填缓存
            ProductVO product = productMapper.selectById(id);
            redisTemplate.opsForValue().set(key, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
            return product;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 没拿到锁，等一下重试
        Thread.sleep(50);
        return getProductWithLock(id);
    }
}
```

### 5.3 缓存雪崩（大量 key 同时过期）

```
解决：过期时间加随机值
```

```java
// 基础过期时间 + 随机 0~300 秒
int ttl = 1800 + ThreadLocalRandom.current().nextInt(300);
redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
```

---

## 六、分布式锁

```java
// 防止重复下单
public OrderVO createOrder(Long userId, CreateOrderRequest req) {
    String lockKey = "lock:order:create:" + userId;
    
    // SET NX EX（原子操作）
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, UUID.randomUUID().toString(), 30, TimeUnit.SECONDS);
    
    if (!Boolean.TRUE.equals(locked)) {
        throw new BusinessException("操作太频繁，请稍后重试");
    }
    
    try {
        // 业务逻辑...
        return doCreateOrder(userId, req);
    } finally {
        redisTemplate.delete(lockKey);
    }
}
```

---

## 七、动手练习

1. 连接 Redis：`docker exec -it aics-redis redis-cli`
2. 基本操作：`SET name "AI客服"` → `GET name` → `DEL name`
3. 设置过期：`SET token:100 "xxx" EX 3600` → `TTL token:100`
4. 在项目中找到 `StringRedisTemplate` 的使用位置，理解业务逻辑
5. 模拟库存扣减：`SET stock:1001 10` → `DECRBY stock:1001 2` → `GET stock:1001`

---

## 学习检查清单

- [ ] 理解 Redis 五种数据结构及适用场景
- [ ] 会在 Spring Boot 中使用 StringRedisTemplate
- [ ] 理解缓存穿透/击穿/雪崩及解决方案
- [ ] 理解分布式锁的实现原理（SET NX EX）
- [ ] 理解缓存一致性策略（先更新 DB 还是先删缓存）
- [ ] 知道 Redis 持久化方式（RDB vs AOF）

---

## 下一步

→ [02-RocketMQ消息队列](./02-RocketMQ消息队列.md)
