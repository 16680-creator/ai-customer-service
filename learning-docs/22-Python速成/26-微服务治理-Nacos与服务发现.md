# 26 · 微服务治理：Nacos 与服务发现

> 让 Python 服务加入 Spring Cloud 生态。本篇对标 Spring Cloud Alibaba，实现 Nacos 注册发现、配置中心、服务调用、负载均衡。

---

## 一、Spring Cloud vs Python 微服务治理对比

| 能力 | Spring Cloud Alibaba | Python 生态 |
|------|---------------------|-------------|
| 服务注册发现 | Nacos + `@EnableDiscoveryClient` | `nacos-sdk-python` |
| 配置中心 | Nacos Config + `@RefreshScope` | 手动拉取 + 监听 |
| 负载均衡 | Ribbon / LoadBalancer | 自己实现或用 `aiod` |
| 服务调用 | Feign / RestTemplate | `httpx` + 服务发现 |
| 熔断降级 | Sentinel | `pybreaker` |
| 链路追踪 | Spring Cloud Sleuth | OpenTelemetry |

**核心差异**：
- Java 侧是框架自动化（注解驱动）
- Python 侧需要手动集成（代码驱动）

---

## 二、Nacos 服务注册与发现

### 在 ai-cs-py-recommend 中集成 Nacos

```python
# app/core/nacos_client.py
from nacos import NacosClient
import socket
from app.config import get_settings

def get_local_ip() -> str:
    """获取本机 IP（对标 Spring Cloud 的 InetUtils）"""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


class NacosServiceRegistry:
    """
    Nacos 服务注册与发现（对标 Spring Cloud 的 DiscoveryClient）
    
    Java 等价:
    @EnableDiscoveryClient
    public class Application {}
    """
    
    def __init__(self):
        settings = get_settings()
        self.client = NacosClient(
            server_addresses=settings.nacos_hosts,
            namespace=settings.nacos_namespace,
            username="nacos",      # 生产环境从配置读取
            password="nacos",
        )
        self.service_name = settings.service_name
        self.ip = get_local_ip()
        self.port = settings.app_port
    
    async def register(self):
        """
        注册服务实例
        
        对标:
        @PostConstruct
        public void register() {
            discoveryClient.register(...);
        }
        """
        self.client.add_naming_instance(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
            cluster_name="DEFAULT",
            weight=1.0,
            metadata={
                "version": "1.0.0",
                "framework": "fastapi",
                "language": "python",
            },
        )
        print(f"✅ 服务已注册到 Nacos: {self.service_name}@{self.ip}:{self.port}")
    
    async def deregister(self):
        """
        注销服务实例
        
        对标:
        @PreDestroy
        public void deregister() {
            discoveryClient.deregister(...);
        }
        """
        self.client.remove_naming_instance(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
        )
        print(f"❌ 服务已从 Nacos 注销: {self.service_name}")
    
    def discover_service(self, service_name: str) -> list[dict]:
        """
        服务发现（获取所有健康实例）
        
        对标:
        @Autowired
        DiscoveryClient discoveryClient;
        
        List<ServiceInstance> instances = discoveryClient.getInstances("ai-cs-product");
        """
        instances = self.client.list_naming_instance(
            service_name=service_name,
            healthy_only=True,
        )
        
        return [
            {
                "ip": inst["ip"],
                "port": inst["port"],
                "weight": inst.get("weight", 1.0),
                "metadata": inst.get("metadata", {}),
            }
            for inst in instances["hosts"]
        ]


# 全局单例
_registry: NacosServiceRegistry | None = None

def get_registry() -> NacosServiceRegistry:
    global _registry
    if _registry is None:
        _registry = NacosServiceRegistry()
    return _registry
```

### 在 FastAPI 启动时注册

```python
# app/main.py
from app.core.nacos_client import get_registry

@app.on_event("startup")
async def on_startup():
    """
    应用启动时注册到 Nacos
    
    对标 Spring Boot:
    @PostConstruct
    public void onStartup() { ... }
    """
    registry = get_registry()
    await registry.register()


@app.on_event("shutdown")
async def on_shutdown():
    """
    应用关闭时注销
    
    对标:
    @PreDestroy
    public void onShutdown() { ... }
    """
    registry = get_registry()
    await registry.deregister()
```

---

## 三、服务间调用：从服务名到真实地址

### 场景：Python 服务调用 Java 服务

```python
# app/services/product_service.py
import httpx
import random
from app.core.nacos_client import get_registry

class ProductServiceClient:
    """
    商品服务客户端（对标 Feign Client）
    
    Java 等价:
    @FeignClient(name = "ai-cs-product")
    public interface ProductServiceClient {
        @GetMapping("/api/products/{id}")
        ProductDto getProduct(@PathVariable int id);
    }
    """
    
    def __init__(self):
        self.registry = get_registry()
        self.service_name = "ai-cs-product"
        self._client = httpx.AsyncClient(timeout=5.0)
    
    async def get_product(self, product_id: int):
        """
        通过服务发现调用下游服务
        
        流程：
        1. 从 Nacos 获取 ai-cs-product 的所有实例
        2. 客户端负载均衡（随机选一个）
        3. 发起 HTTP 调用
        """
        # 1. 服务发现
        instances = self.registry.discover_service(self.service_name)
        if not instances:
            raise RuntimeError(f"服务 {self.service_name} 无可用实例")
        
        # 2. 负载均衡（简单随机，对标 Ribbon 的 RandomRule）
        instance = random.choice(instances)
        base_url = f"http://{instance['ip']}:{instance['port']}"
        
        # 3. 发起调用
        url = f"{base_url}/api/products/{product_id}"
        response = await self._client.get(url)
        
        if response.status_code != 200:
            raise RuntimeError(f"商品服务调用失败: {response.status_code}")
        
        data = response.json()
        return data["data"]  # 解包 Result<T>


# 使用
client = ProductServiceClient()
product = await client.get_product(101)
print(product)  # {'productId': 101, 'name': 'xxx', ...}
```

### 负载均衡策略（对标 Ribbon）

```python
# app/core/load_balancer.py
from typing import List
import random
import time

class LoadBalancer:
    """
    负载均衡器（对标 Ribbon）
    
    Java 等价:
    @LoadBalanced
    RestTemplate restTemplate;
    """
    
    @staticmethod
    def random(instances: List[dict]) -> dict:
        """随机（对标 RandomRule）"""
        return random.choice(instances)
    
    @staticmethod
    def round_robin(instances: List[dict], counter: dict) -> dict:
        """轮询（对标 RoundRobinRule）"""
        idx = counter.get("index", 0)
        counter["index"] = (idx + 1) % len(instances)
        return instances[idx]
    
    @staticmethod
    def weighted(instances: List[dict]) -> dict:
        """加权随机（对标 WeightedResponseTimeRule）"""
        weights = [inst.get("weight", 1.0) for inst in instances]
        return random.choices(instances, weights=weights)[0]
```

---

## 四、Nacos 配置中心

### 从 Nacos 拉取配置

```python
# app/core/nacos_config.py
from nacos import NacosClient
from app.config import get_settings

class NacosConfigClient:
    """
    Nacos 配置中心（对标 Spring Cloud Config）
    
    Java 等价:
    @Configuration
    @RefreshScope
    public class AppConfig {
        @Value("${app.feature.enabled}")
        private boolean featureEnabled;
    }
    """
    
    def __init__(self):
        settings = get_settings()
        self.client = NacosClient(
            server_addresses=settings.nacos_hosts,
            namespace=settings.nacos_namespace,
        )
        self.data_id = f"{settings.service_name}.yml"
        self.group = "DEFAULT_GROUP"
    
    def get_config(self) -> dict:
        """
        获取配置
        
        对标:
        @Value("${...}")
        """
        content = self.client.get_config(self.data_id, self.group)
        if content:
            import yaml
            return yaml.safe_load(content)
        return {}
    
    def listen_config(self, callback):
        """
        监听配置变更
        
        对标:
        @RefreshScope  # Spring Cloud 自动刷新
        """
        def listener(config_dict):
            print(f"📢 配置已更新: {config_dict}")
            callback(config_dict)
        
        self.client.add_config_watcher(self.data_id, self.group, listener)


# 使用
config_client = NacosConfigClient()

# 拉取一次
config = config_client.get_config()
print(config)  # {'feature_enabled': True, 'max_workers': 4}

# 监听变更
def on_config_change(new_config):
    # 更新应用配置
    app.state.max_workers = new_config.get("max_workers", 4)

config_client.listen_config(on_config_change)
```

---

## 五、健康检查与心跳

### Nacos 心跳机制

```python
# Nacos 客户端会自动发送心跳（默认 5 秒一次）
# 但可以自定义健康检查逻辑

class HealthChecker:
    """
    自定义健康检查（对标 Spring Boot Actuator）
    
    Java 等价:
    @Component
    public class CustomHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            // 检查数据库、Redis、下游服务
            return Health.up().build();
        }
    }
    """
    
    async def check_health(self) -> bool:
        """
        检查服务健康状态
        
        检查项：
        - 数据库连接
        - Redis 连接
        - 下游服务可达性
        """
        checks = {
            "database": await self._check_database(),
            "redis": await self._check_redis(),
            "downstream": await self._check_downstream(),
        }
        
        return all(checks.values())
    
    async def _check_database(self) -> bool:
        try:
            # 执行简单查询
            db.query("SELECT 1")
            return True
        except:
            return False
    
    async def _check_redis(self) -> bool:
        try:
            cache.set("health_check", "ok", ttl=10)
            return cache.get("health_check") == "ok"
        except:
            return False
    
    async def _check_downstream(self) -> bool:
        try:
            # 检查关键依赖是否可用
            instances = get_registry().discover_service("ai-cs-product")
            return len(instances) > 0
        except:
            return False
```

---

## 六、与 ai-customer-service 整合清单

### 在主项目中启用 Python 服务

```yaml
# docker-compose.yml 新增
ai-cs-py-recommend:
  build: ./ai-cs-py-recommend
  ports:
    - "8082:8082"
  environment:
    - NACOS_HOSTS=nacos:8848
    - NACOS_NAMESPACE=ai-customer-service
    - SERVICE_NAME=ai-cs-py-recommend
    - REDIS_URL=redis://redis:6379
  depends_on:
    - nacos
    - redis
```

### 验证服务注册

```bash
# 1. 启动 Nacos
docker-compose up nacos

# 2. 启动 Python 服务
cd ai-cs-py-recommend
python -m app.main

# 3. 查看 Nacos 控制台
open http://localhost:8848/nacos
# 用户名/密码: nacos/nacos
# 服务管理 → 服务列表 → 看到 ai-cs-py-recommend
```

### 从 Java 服务调用 Python 服务

```java
// ai-cs-order 中新增 Feign Client
@FeignClient(name = "ai-cs-py-recommend")
public interface RecommendServiceClient {
    @PostMapping("/api/recommend/")
    Result<RecommendResponse> getRecommendations(@RequestBody RecommendRequest req);
}

// 使用
@Autowired
RecommendServiceClient recommendClient;

public List<ProductDto> getRecommendations(int userId) {
    RecommendResponse resp = recommendClient.getRecommendations(
        new RecommendRequest(userId, 5)
    ).getData();
    return resp.getProducts();
}
```

---

## 七、故障排查

### 服务注册失败

**症状：** 启动后在 Nacos 控制台看不到服务

**排查步骤：**
```bash
# 1. 检查 Nacos 连通性
curl http://nacos:8848/nacos/v1/ns/operator/metrics

# 2. 查看 Python 服务日志
docker logs ai-cs-py-recommend

# 3. 验证网络
docker exec ai-cs-py-recommend ping nacos

# 4. 检查命名空间
# Nacos 控制台 → 命名空间 → 确认 ai-customer-service 存在
```

### 服务发现返回空列表

**原因：** 健康检查未通过

**修复：**
```python
# 确保 /api/recommend/health 端点可访问
@router.get("/health")
async def health():
    return {"status": "up"}
```

---

## 八、性能优化

### 缓存服务实例列表

```python
from functools import lru_cache
import time

class CachedServiceDiscovery:
    """
    缓存服务发现结果（减少 Nacos 查询）
    
    对标 Ribbon 的 ServerList 缓存
    """
    
    def __init__(self, ttl: int = 30):
        self.ttl = ttl
        self._cache = {}
    
    def get_instances(self, service_name: str) -> list[dict]:
        now = time.time()
        
        if service_name in self._cache:
            instances, cached_at = self._cache[service_name]
            if now - cached_at < self.ttl:
                return instances
        
        # 缓存过期，重新查询
        instances = get_registry().discover_service(service_name)
        self._cache[service_name] = (instances, now)
        
        return instances
```

---

## 九、动手验证

```bash
# 1. 启动 Nacos
docker-compose up nacos

# 2. 启动 ai-cs-py-recommend
cd ai-cs-py-recommend
python -m app.main

# 3. 验证服务注册
curl http://localhost:8082/api/recommend/health

# 4. 在 Nacos 控制台查看
open http://localhost:8848/nacos
# 服务管理 → ai-cs-py-recommend → 查看实例详情

# 5. 测试服务发现
python -c "
from app.core.nacos_client import get_registry
registry = get_registry()
instances = registry.discover_service('ai-cs-product')
print(instances)
"
```

---

**上一篇**：[25-可观测性实战-日志、监控、链路追踪](25-可观测性实战-日志、监控、链路追踪.md)　**下一篇**：完结
