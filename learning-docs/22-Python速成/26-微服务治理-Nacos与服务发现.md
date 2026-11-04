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
# ---- 本段讲：封装 Nacos 服务注册、注销与发现，并以单例方式提供客户端 ----
# NacosClient 相当于 Python 服务接入 Spring Cloud Alibaba Nacos 的底层入口，需要显式完成 Java 框架自动完成的治理动作。
# NacosServiceRegistry 集中保存服务名、本机地址和端口，对标 @EnableDiscoveryClient 启用后的注册发现能力。
# get_registry 延迟创建全局注册器，作用类似由 Spring 容器托管的单例 Bean，避免重复建立 Nacos 客户端。

# app/core/nacos_client.py
# 使用 SDK 客户端直接调用 Nacos 命名服务 API；Python 没有注解代理，需要自行持有该对象。
from nacos import NacosClient
# socket 不用于业务通信，而是借助 UDP 套接字判断当前进程对外通信时采用的本机 IP。
import socket
# 统一读取 Nacos 地址、命名空间、服务名和应用端口，类似 Spring Boot 注入 application.yml 配置。
from app.config import get_settings

# 返回值标注为字符串；此函数负责确定注册到 Nacos 的实例 IP，而不是固定写 localhost。
def get_local_ip() -> str:
    """获取本机 IP（对标 Spring Cloud 的 InetUtils）"""
    # 创建 IPv4 UDP 套接字；UDP connect 不会在这里发送业务数据，只让操作系统选择出站网卡。
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    # 无论地址探测成功还是抛出异常，都必须关闭套接字，语义接近 Java 的 try-finally 资源清理。
    try:
        # 设置一个外部目标，使多网卡、容器环境下可获得实际出站 IP；这里不会进行 HTTP 请求。
        s.connect(("8.8.8.8", 80))
        # getsockname 返回本地 (IP, 端口)，取下标 0 作为向 Nacos 注册的实例地址。
        ip = s.getsockname()[0]
    finally:
        # 立即释放临时 socket，避免每次构造注册器时泄漏系统句柄。
        s.close()
    # 将探测结果交给注册器保存，后续注册、注销必须使用同一个 IP。
    return ip


class NacosServiceRegistry:
    """
    Nacos 服务注册与发现（对标 Spring Cloud 的 DiscoveryClient）
    
    Java 等价:
    @EnableDiscoveryClient
    public class Application {}
    """
    
    # 构造过程对应 Spring Bean 初始化：一次性准备注册中心连接信息和当前实例身份。
    def __init__(self):
        # 取得应用配置对象，类似 @ConfigurationProperties Bean，而不是在各方法内散落读取环境变量。
        settings = get_settings()
        # 创建 Nacos SDK 客户端；server_addresses 可包含集群节点，namespace 用于隔离不同环境。
        self.client = NacosClient(
            # 指定 Nacos 服务端地址，对应 spring.cloud.nacos.server-addr。
            server_addresses=settings.nacos_hosts,
            # 指定命名空间，对应 spring.cloud.nacos.discovery.namespace，防止测试与生产服务互相发现。
            namespace=settings.nacos_namespace,
            # 认证信息用于登录 Nacos；示例固定值仅便于教学。
            username="nacos",      # 生产环境从配置读取
            password="nacos",
        )
        # service_name 是 Nacos 中的逻辑服务名，对标 spring.application.name，也是调用方发现实例时使用的键。
        self.service_name = settings.service_name
        # 保存实际出站 IP，保证 add/remove 操作指向同一实例。
        self.ip = get_local_ip()
        # 保存 FastAPI 监听端口，与 IP 共同组成可被下游访问的实例地址。
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
        # 调用命名服务注册当前实例；这一步是 Python 对 @EnableDiscoveryClient 自动注册行为的显式实现。
        self.client.add_naming_instance(
            # 同名实例会归入同一个服务，消费者按该名称获取候选地址。
            service_name=self.service_name,
            # 注册可被其他服务访问的主机地址，不能误用仅容器内部不可达的回环地址。
            ip=self.ip,
            # 注册 FastAPI 实际监听端口，消费者会据此拼接 HTTP 基础 URL。
            port=self.port,
            # 集群名可用于机房或地域分组；DEFAULT 表示使用默认集群。
            cluster_name="DEFAULT",
            # 权重供加权负载均衡使用，1.0 表示普通流量份额。
            weight=1.0,
            # metadata 是随实例返回的扩展标签，可用于版本路由、框架识别或灰度发布。
            metadata={
                "version": "1.0.0",
                "framework": "fastapi",
                "language": "python",
            },
        )
        # 注册完成后输出实例坐标，便于核对 Nacos 控制台中的服务名、IP 与端口。
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
        # 使用注册时完全相同的服务名、IP、端口删除实例，对应 @PreDestroy 阶段的优雅下线。
        self.client.remove_naming_instance(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
        )
        # 注销后消费者的下一次服务发现将不再选到该实例，从而避免请求发往已停止进程。
        print(f"❌ 服务已从 Nacos 注销: {self.service_name}")
    
    def discover_service(self, service_name: str) -> list[dict]:
        """
        服务发现（获取所有健康实例）
        
        对标:
        @Autowired
        DiscoveryClient discoveryClient;
        
        List<ServiceInstance> instances = discoveryClient.getInstances("ai-cs-product");
        """
        # 按服务名查询实例列表，对标 DiscoveryClient.getInstances(serviceName)。
        instances = self.client.list_naming_instance(
            service_name=service_name,
            # 让 Nacos 先过滤不健康实例，避免负载均衡把请求发给心跳超时或检查失败的节点。
            healthy_only=True,
        )
        
        # SDK 返回包含 hosts 等字段的响应；此处转换成调用层只关心的统一字典列表。
        return [
            {
                # IP 与端口用于构造下游 HTTP 地址。
                "ip": inst["ip"],
                "port": inst["port"],
                # 老实例没有 weight 时按 1.0 处理，供后续加权策略读取。
                "weight": inst.get("weight", 1.0),
                # 元数据缺失时返回空字典，调用方可据此实现版本或标签路由。
                "metadata": inst.get("metadata", {}),
            }
            # 遍历健康实例；列表推导式相当于 Java Stream.map(...).toList()。
            for inst in instances["hosts"]
        ]


# 全局单例
# 初始值为 None 表示尚未构造；联合类型相当于 Java 中可为空的 NacosServiceRegistry 引用。
_registry: NacosServiceRegistry | None = None

# 对外统一提供注册器，避免各业务客户端分别创建连接和保存不同实例信息。
def get_registry() -> NacosServiceRegistry:
    # 声明这里修改模块级变量，而不是创建同名局部变量。
    global _registry
    # 首次调用才创建对象，效果类似 Spring 默认单例 Bean 的延迟初始化。
    if _registry is None:
        _registry = NacosServiceRegistry()
    # 后续调用始终返回同一个注册器。
    return _registry
```

### 在 FastAPI 启动时注册

```python
# ---- 本段讲：把 Nacos 注册与注销绑定到 FastAPI 应用生命周期 ----
# startup/shutdown 回调分别对应 Spring Bean 的 @PostConstruct 与 @PreDestroy，用于上线注册和优雅下线。
# 新版 FastAPI 也可用 lifespan 上下文表达同一过程：yield 前初始化，yield 后释放；这里展示事件钩子写法。
# 两个阶段复用单例注册器，确保注销的服务名、IP、端口与启动时注册的信息一致。

# app/main.py
# 获取由模块统一管理的注册器，类似从 Spring 容器注入 DiscoveryClient 封装 Bean。
from app.core.nacos_client import get_registry

# 将函数登记为应用启动事件；装饰器作用类似框架扫描生命周期注解。
@app.on_event("startup")
async def on_startup():
    """
    应用启动时注册到 Nacos
    
    对标 Spring Boot:
    @PostConstruct
    public void onStartup() { ... }
    """
    # 取得单例注册器，避免生命周期函数各自创建不同客户端。
    registry = get_registry()
    # 等待注册流程完成后再结束启动阶段，对标 @PostConstruct 完成后 Spring Boot 才进入就绪状态。
    await registry.register()


# 将函数登记为应用关闭事件；若改用 lifespan，此部分位于 yield 之后的清理阶段。
@app.on_event("shutdown")
async def on_shutdown():
    """
    应用关闭时注销
    
    对标:
    @PreDestroy
    public void onShutdown() { ... }
    """
    # 取回启动阶段使用的同一注册器，其中保留了当前实例身份。
    registry = get_registry()
    # 等待 Nacos 删除实例后再退出进程，对标 @PreDestroy 中完成资源清理与服务摘除。
    await registry.deregister()
```

---

## 三、服务间调用：从服务名到真实地址

### 场景：Python 服务调用 Java 服务

```python
# ---- 本段讲：通过 Nacos 发现 Java 商品服务，并用 httpx 完成客户端负载均衡调用 ----
# ProductServiceClient 对标 @FeignClient：先用 DiscoveryClient 思路解析服务名，再向选中的真实实例发 HTTP 请求。
# random.choice 是最简客户端 LoadBalancer，仅在 Nacos 已过滤出的健康实例之间随机分流。
# httpx.AsyncClient 复用连接池并异步等待网络响应，避免每次请求重新建连或阻塞 FastAPI 事件循环。

# app/services/product_service.py
# httpx 提供异步 HTTP 客户端，角色类似 Feign 底层 HTTP Client 或 WebClient。
import httpx
# random 用于从健康实例列表随机选取一个节点，实现最小版本的客户端负载均衡。
import random
# 获取封装后的 Nacos DiscoveryClient 能力，把逻辑服务名解析为真实 IP 和端口。
from app.core.nacos_client import get_registry

# 将发现、选址和 HTTP 调用封装到一个客户端类中，业务代码无需感知 Nacos 返回结构。
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
    
    # 构造客户端时固定依赖的逻辑服务名并复用 HTTP 连接池，类似创建一个 Feign 代理 Bean。
    def __init__(self):
        # 保存注册中心封装，后续每次调用都可获取最新健康实例。
        self.registry = get_registry()
        # 逻辑服务名对应 Java @FeignClient(name = "ai-cs-product")，不是 DNS 主机名或固定 IP。
        self.service_name = "ai-cs-product"
        # 5 秒超时防止下游长时间无响应；单例 AsyncClient 可复用 keep-alive 连接。
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
        # 用逻辑服务名向 Nacos 查询，并且 discover_service 已通过 healthy_only 排除不健康节点。
        instances = self.registry.discover_service(self.service_name)
        # 空列表表示当前没有可路由节点；显式失败比 random.choice 抛出难懂的下标错误更清楚。
        if not instances:
            raise RuntimeError(f"服务 {self.service_name} 无可用实例")
        
        # 2. 负载均衡（简单随机，对标 Ribbon 的 RandomRule）
        # 从候选实例中随机选择一个，承担 Spring Cloud LoadBalancer 根据 ServiceInstance 选址的职责。
        instance = random.choice(instances)
        # 将选中实例的 IP 和端口转换为 HTTP 基础地址；服务名到物理地址的解析至此完成。
        base_url = f"http://{instance['ip']}:{instance['port']}"
        
        # 3. 发起调用
        # 拼接商品资源路径和方法参数，效果对应 Feign 接口上的 @GetMapping 与 @PathVariable。
        url = f"{base_url}/api/products/{product_id}"
        # await 非阻塞等待下游响应，事件循环可在此期间继续处理其他请求。
        response = await self._client.get(url)
        
        # 约定只有 200 表示成功；其他状态码转换为当前客户端可理解的业务调用异常。
        if response.status_code != 200:
            raise RuntimeError(f"商品服务调用失败: {response.status_code}")
        
        # 将 JSON 响应体解析为 Python 字典，类似 Jackson 把响应反序列化为 Result<ProductDto>。
        data = response.json()
        # 取统一响应包装的 data 字段，返回值对应 Java Result<T>.getData()。
        return data["data"]  # 解包 Result<T>


# 使用
# 创建服务客户端；实际项目通常把它放入依赖注入容器并在应用关闭时释放 AsyncClient。
client = ProductServiceClient()
# 异步调用商品 101，内部依次执行发现、选址和 HTTP GET。
product = await client.get_product(101)
# 打印已经解包的商品数据，而不是 httpx Response 或统一 Result 外壳。
print(product)  # {'productId': 101, 'name': 'xxx', ...}
```

### 负载均衡策略（对标 Ribbon）

```python
# ---- 本段讲：实现随机、轮询和加权随机三种客户端负载均衡策略 ----
# LoadBalancer 接收 DiscoveryClient 返回的实例列表，承担 Spring Cloud LoadBalancer/Ribbon 的选址职责。
# 三个静态方法不保存对象状态；轮询游标由调用者传入，随机与加权随机则直接从当前候选集选节点。
# 策略只决定“调用哪个实例”，实际 HTTP 请求仍由 httpx 等客户端执行。

# app/core/load_balancer.py
# List[dict] 描述实例集合的参数类型，便于 Java 开发者类比 List<ServiceInstance>。
from typing import List
# random 同时提供等概率 choice 与按权重 choices 两种抽样方式。
import random
# time 在本示例代码中尚未参与策略计算，保留是为了后续响应时间等策略扩展。
import time

# 将不同选址算法集中为静态方法，类似 Spring Cloud 的 ReactorServiceInstanceLoadBalancer 策略实现。
class LoadBalancer:
    """
    负载均衡器（对标 Ribbon）
    
    Java 等价:
    @LoadBalanced
    RestTemplate restTemplate;
    """
    
    # staticmethod 表示调用时无需创建 LoadBalancer 实例，相当于 Java 静态工具方法。
    @staticmethod
    # 输入健康实例列表并返回其中一个实例字典。
    def random(instances: List[dict]) -> dict:
        """随机（对标 RandomRule）"""
        # 每个实例等概率被选中，对标 Ribbon RandomRule。
        return random.choice(instances)
    
    @staticmethod
    # counter 是可变字典，用来跨调用保存下一次轮询位置，作用类似 AtomicInteger 游标。
    def round_robin(instances: List[dict], counter: dict) -> dict:
        """轮询（对标 RoundRobinRule）"""
        # 首次调用没有 index 时从第 0 个实例开始。
        idx = counter.get("index", 0)
        # 游标加一后对实例数取模，到末尾会回到 0，形成循环选择。
        counter["index"] = (idx + 1) % len(instances)
        # 返回本轮游标指向的实例；调用方再用其 IP、端口发请求。
        return instances[idx]
    
    @staticmethod
    # 按 Nacos 实例 weight 字段分配不同选择概率，返回值仍是单个实例。
    def weighted(instances: List[dict]) -> dict:
        """加权随机（对标 WeightedResponseTimeRule）"""
        # 将每个实例权重提取为并行数组；缺失权重按 1.0，类似默认 ServiceInstance 权重。
        weights = [inst.get("weight", 1.0) for inst in instances]
        # choices 返回列表，即使只抽一个也需取 [0]；权重越大的实例被选中概率越高。
        return random.choices(instances, weights=weights)[0]
```

---

## 四、Nacos 配置中心

### 从 Nacos 拉取配置

```python
# ---- 本段讲：从 Nacos 配置中心拉取 YAML，并监听变更后刷新应用状态 ----
# NacosConfigClient 手工实现 Spring Cloud Alibaba Config 的配置读取，get_config 对应属性注入前的远程配置加载。
# add_config_watcher 在配置变化时回调业务函数，对标 @RefreshScope 触发 Bean 配置刷新，但 Python 需明确更新目标对象。
# data_id 与 group 共同定位配置集，类似 Spring Cloud 根据应用名、环境和分组选择配置文件。

# app/core/nacos_config.py
# 同一个 NacosClient SDK 也提供配置中心 API，不仅能执行命名服务注册发现。
from nacos import NacosClient
# 获取 Nacos 地址、命名空间和当前服务名，作用类似读取 bootstrap/application 配置。
from app.config import get_settings

# 封装配置定位、首次拉取和持续监听，避免业务层直接操作 Nacos SDK。
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
    
    # 初始化时固定 Nacos 连接和配置集坐标，类似构造一个配置中心客户端 Bean。
    def __init__(self):
        # 读取环境相关设置，避免将服务地址和命名空间散落在业务代码中。
        settings = get_settings()
        # 创建面向指定 Nacos 集群和命名空间的客户端。
        self.client = NacosClient(
            # 对应 spring.cloud.nacos.config.server-addr。
            server_addresses=settings.nacos_hosts,
            # 对应配置中心 namespace，用于隔离开发、测试和生产配置。
            namespace=settings.nacos_namespace,
        )
        # 按“服务名.yml”构造 data_id，使每个微服务拥有独立配置集。
        self.data_id = f"{settings.service_name}.yml"
        # group 是 data_id 之外的第二层分类；这里使用 Nacos 默认分组。
        self.group = "DEFAULT_GROUP"
    
    def get_config(self) -> dict:
        """
        获取配置
        
        对标:
        @Value("${...}")
        """
        # 按 data_id 和 group 同步读取原始文本，作用类似 Spring 启动期从远程 PropertySource 拉配置。
        content = self.client.get_config(self.data_id, self.group)
        # Nacos 无内容时跳过解析，避免把空值交给 YAML 解析器。
        if content:
            # 仅在确有配置时加载 YAML 库，缩小模块初始化阶段的依赖工作。
            import yaml
            # safe_load 将 YAML 转成字典且不构造任意 Python 对象，对应安全的配置反序列化。
            return yaml.safe_load(content)
        # 统一返回空字典，调用方可通过 get(key, default) 使用本地默认值。
        return {}
    
    def listen_config(self, callback):
        """
        监听配置变更
        
        对标:
        @RefreshScope  # Spring Cloud 自动刷新
        """
        # 定义 SDK 收到推送后执行的内部适配函数，将 Nacos 回调转交给应用回调。
        def listener(config_dict):
            # 输出最新配置便于观察监听是否生效；生产环境通常使用结构化日志并注意敏感项脱敏。
            print(f"📢 配置已更新: {config_dict}")
            # 由业务 callback 决定更新哪些运行时状态，这是 Python 对 @RefreshScope 自动重建 Bean 的显式替代。
            callback(config_dict)
        
        # 注册配置监听器；之后 Nacos 配置变化会触发 listener，而不是每次请求主动轮询。
        self.client.add_config_watcher(self.data_id, self.group, listener)


# 使用
# 创建配置客户端并确定当前服务对应的 data_id 与 group。
config_client = NacosConfigClient()

# 拉取一次
# 启动时获取配置快照，类似 Spring Environment 完成远程属性源装载。
config = config_client.get_config()
# 查看解析后的字典；键名可由业务代码直接读取。
print(config)  # {'feature_enabled': True, 'max_workers': 4}

# 监听变更
# 定义运行时刷新动作；参数是监听器收到的新配置字典。
def on_config_change(new_config):
    # 更新应用配置
    # 写入 FastAPI app.state，使后续请求读取新值；缺少 max_workers 时回退到 4。
    app.state.max_workers = new_config.get("max_workers", 4)

# 将业务刷新函数交给 Nacos watcher，建立持续监听关系。
config_client.listen_config(on_config_change)
```

---

## 五、健康检查与心跳

### Nacos 心跳机制

```python
# ---- 本段讲：组合数据库、Redis 和下游发现结果，形成应用级健康判断 ----
# NacosClient 会为临时实例维持心跳，心跳超时后 Nacos 可将实例标记为不健康并从健康发现结果中排除。
# HealthChecker 对标 Spring Boot Actuator 的 HealthIndicator，补充进程存活之外的依赖可用性检查。
# check_health 按字典项从上到下逐项 await 后汇总布尔值；任一关键依赖失败，整体健康状态即为失败。

# Nacos 客户端会自动发送心跳（默认 5 秒一次）
# 但可以自定义健康检查逻辑

# 将多个基础设施检查集中为一个组件，便于健康端点统一调用。
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
        # 依次执行三个异步检查，并保留名称到结果的映射，类似 Actuator Health.Builder.withDetail。
        checks = {
            # 数据库能否执行最小查询。
            "database": await self._check_database(),
            # Redis 能否完成一次短生命周期读写。
            "redis": await self._check_redis(),
            # Nacos 中是否存在关键下游的健康实例。
            "downstream": await self._check_downstream(),
        }
        
        # all 仅当所有值都为 True 时返回 True，对标 Health.up；任一 False 对标 Health.down。
        return all(checks.values())
    
    # 私有方法只返回布尔值，让聚合层无需理解数据库异常类型。
    async def _check_database(self) -> bool:
        # 将连接或查询异常转换为“不健康”，避免健康端点自身因异常返回 500 堆栈。
        try:
            # 执行简单查询
            # SELECT 1 不读取业务表，用最小代价验证数据库连接和执行链路。
            db.query("SELECT 1")
            return True
        except:
            return False
    
    # 通过真实写入和读取同时验证 Redis 连接与基本命令执行能力。
    async def _check_redis(self) -> bool:
        try:
            # 设置 10 秒 TTL，避免健康检查键永久残留；随后读取并比较预期值。
            cache.set("health_check", "ok", ttl=10)
            return cache.get("health_check") == "ok"
        except:
            # 连接失败、超时或命令异常都统一视为 Redis 不健康。
            return False
    
    # 下游检查不直接发业务请求，而是确认注册中心至少返回一个健康商品服务实例。
    async def _check_downstream(self) -> bool:
        try:
            # 检查关键依赖是否可用
            # discover_service 已启用 healthy_only，结果语义对应 DiscoveryClient 中可路由的 ServiceInstance。
            instances = get_registry().discover_service("ai-cs-product")
            # 至少一个健康实例即可承接流量；空列表表示当前下游整体不可用。
            return len(instances) > 0
        except:
            # Nacos 查询异常同样意味着当前无法可靠定位下游，因此返回不健康。
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
# ---- 本段讲：暴露 FastAPI 健康端点，供人工、容器或注册中心探测 ----
# 路由作用对标 Spring Boot Actuator 的 /actuator/health，用 HTTP 响应表明进程可接收请求。
# @router.get 类似 Spring MVC 的 @GetMapping；async def 允许端点后续异步调用 HealthChecker。
# 当前示例固定返回 up，仅验证 Web 进程存活；若接入上段检查器，可进一步表达依赖就绪状态。

# 确保 /api/recommend/health 端点可访问
# 将 GET /health 映射到下方处理函数；最终完整路径还会叠加 router 的 prefix。
@router.get("/health")
# 异步路由函数不接收参数，FastAPI 在请求到达时调用它。
async def health():
    # 返回字典后框架自动序列化为 JSON，与 Spring 返回 Map/DTO 的效果相同。
    return {"status": "up"}
```

---

## 八、性能优化

### 缓存服务实例列表

```python
# ---- 本段讲：给 Nacos 服务发现结果增加 TTL 缓存，减少频繁查询注册中心 ----
# CachedServiceDiscovery 对标 Ribbon/Spring Cloud LoadBalancer 的 ServerList 缓存，在短时间内复用同一批健康实例。
# 每个服务名单独保存实例列表和缓存时间；超过 ttl 才重新调用 DiscoveryClient 风格的 discover_service。
# 缓存降低 Nacos 压力，但会带来最长 ttl 秒的实例变更延迟，需要在实时性与开销之间权衡。

# lru_cache 在当前示例中未实际使用，保留用于说明也可借助标准库实现函数级缓存。
from functools import lru_cache
# time.time 提供秒级时间戳，用于判断缓存条目是否超过 TTL。
import time

# 封装带过期判断的服务发现读取层，调用方仍按服务名获取实例列表。
class CachedServiceDiscovery:
    """
    缓存服务发现结果（减少 Nacos 查询）
    
    对标 Ribbon 的 ServerList 缓存
    """
    
    # ttl 参数以秒为单位，默认允许实例列表复用 30 秒。
    def __init__(self, ttl: int = 30):
        # 保存有效期，类似 Spring Cloud LoadBalancer 缓存配置中的 ttl。
        self.ttl = ttl
        # 字典按 service_name 存储 (instances, cached_at) 元组。
        self._cache = {}
    
    # 返回指定逻辑服务名的实例列表，类型对应 List<ServiceInstance> 的简化字典形式。
    def get_instances(self, service_name: str) -> list[dict]:
        # 获取当前时间，后续与写入缓存时刻相减得到条目年龄。
        now = time.time()
        
        # 先按服务名查缓存；不同微服务的实例列表互不覆盖。
        if service_name in self._cache:
            # 解包缓存值与写入时间，类似从 Spring Cache 中读取带时间戳的包装对象。
            instances, cached_at = self._cache[service_name]
            # 条目年龄小于 TTL 时直接命中，不访问 Nacos；这会暂时保留期间上下线的旧视图。
            if now - cached_at < self.ttl:
                return instances
        
        # 缓存过期，重新查询
        # 未命中或已过期时向 Nacos 拉取最新健康实例，对标刷新 LoadBalancer 的 ServiceInstanceListSupplier。
        instances = get_registry().discover_service(service_name)
        # 将最新列表与本次查询时间一起覆盖写入，作为下一次 TTL 判断的基准。
        self._cache[service_name] = (instances, now)
        
        # 无论来自首次查询还是过期刷新，都向调用方返回当前缓存的新快照。
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
