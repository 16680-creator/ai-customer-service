# 10-Python服务

> Python 在企业应用中的定位：快速迭代、AI 应用、数据处理。本模块展示如何将 Python 服务集成到 Spring Cloud 微服务体系。

---

## 📚 章节导航

| 章节 | 核心内容 | 难度 | 适用场景 |
|------|---------|------|---------|
| [FastAPI 对话服务实战](01-FastAPI对话服务实战.md) | 基于 `ai-cs-py-chat` 的完整解析 | ⭐⭐ | AI 对话应用 |
| Python 与 Java 混合架构 | 服务调用、链路追踪、数据一致性 | ⭐⭐⭐ | 微服务整合 |
| 异步编程最佳实践 | asyncio / ASGI / 性能优化 | ⭐⭐⭐ | 高并发场景 |
| 数据处理服务 | Pandas / NumPy / 批量计算 | ⭐⭐ | 数据分析 |

---

## 🎯 学习路线

### 快速入门（2天）
```
01-FastAPI 实战 → 22-Python速成/11篇 → ai-cs-py-chat 源码
```

### 完整路线（1周）
```
Day 1-2: 22-Python速成 (02-11篇)
Day 3:   ai-cs-py-chat 源码分析
Day 4:   ai-cs-py-recommend 开发
Day 5:   与 Java 服务整合
Day 6-7: 可观测性 + 部署上线
```

---

## 💡 核心价值主张

### Python vs Java 的定位

| 维度 | Java (Spring Boot) | Python (FastAPI) |
|------|-------------------|------------------|
| **适用场景** | 核心业务、事务密集 | AI 应用、数据处理、快速迭代 |
| **开发速度** | 中等 | 快（代码量少 50%+） |
| **运行性能** | 高（JIT 优化） | 中等（纯 Python 慢 10-50 倍）|
| **并发模型** | 多线程 | 异步 I/O（单线程高并发） |
| **生态成熟度** | 企业级（Spring 全家桶） | 新兴（AI / 数据科学丰富） |
| **类型安全** | 编译期检查 | 可选（mypy） |

### 什么时候用 Python

✅ **推荐场景**
- AI 对话 / RAG / Embedding（OpenAI / LangChain）
- 数据处理 / 报表生成（Pandas / NumPy）
- 快速原型验证（MVP）
- 轻量级服务（API 网关后的内部服务）

❌ **不推荐场景**
- 核心支付 / 订单 / 库存（事务强一致性）
- CPU 密集计算（除非用 Cython / numpy）
- 超大规模流量（单机 QPS 不如 Java）

---

## 🔧 架构模式

### 模式 1：旁路模式（推荐）

```
用户请求
  ↓
Java 网关 (ai-cs-gateway)
  ├──→ Java 核心服务 (ai-cs-order / ai-cs-user)
  └──→ Python AI 服务 (ai-cs-py-chat)
         ↓
       返回结果
```

**优点**：
- Java 服务不依赖 Python（降级容易）
- Python 服务可独立扩缩容
- 故障隔离

### 模式 2：编排模式

```
用户请求
  ↓
Java 编排服务 (ai-cs-order)
  ├──→ 商品服务 (Java)
  ├──→ 推荐服务 (ai-cs-py-recommend)  ← Python
  └──→ AI 客服 (ai-cs-py-chat)        ← Python
         ↓
       组装结果
```

**适用**：
- Python 服务是必要环节（不可降级）
- 需要编排多个 Python 服务结果

---

## 📊 实战案例

### 案例 1：ai-cs-py-chat（AI 对话服务）

**技术栈**：
- FastAPI + Pydantic（路由 + 校验）
- AsyncOpenAI（LLM 调用）
- SSE 流式响应
- Prometheus 指标

**关键代码**（参考 01 篇）：
```python
# app/api/chat.py
@router.post("/chat/stream")
async def chat_stream(req: ChatRequest) -> StreamingResponse:
    async def event_gen():
        async for delta in chat_service.stream(req):
            yield f"data: {json.dumps({'delta': delta})}\n\n"
        yield "data: [DONE]\n\n"
    
    return StreamingResponse(
        event_gen(),
        media_type="text/event-stream"
    )
```

**部署**：
```bash
docker run -p 8000:8000 \
  -e LLM_API_KEY=xxx \
  ai-cs-py-chat
```

### 案例 2：ai-cs-py-recommend（推荐服务）

完整实现见 `ai-cs-py-recommend/` 目录

**功能**：
- 协同过滤推荐
- Redis 缓存（TTL 管理）
- SQLite 参数化查询
- 测试覆盖率 65%+

**对标 Java**：
```java
// Java 版本 (ai-cs-order/service/RecommendService.java)
@Service
public class RecommendService {
    @Autowired
    private ProductRepository repo;
    
    public List<ProductDto> getRecommendations(int userId) {
        // 同样的推荐算法
    }
}

// Python 版本 (ai-cs-py-recommend/app/services/recommend_service.py)
class RecommendService:
    def __init__(self, repo: ProductRepository):
        self.repo = repo
    
    async def get_recommendations(self, user_id: int) -> list[ProductDto]:
        # 同样的推荐算法
```

---

## 🔗 与 Spring Cloud 整合

### 1. Nacos 服务注册

```python
# app/core/nacos_client.py
from nacos import NacosClient

client = NacosClient(
    server_addresses="nacos:8848",
    namespace="ai-customer-service"
)

# 注册服务
client.add_naming_instance(
    service_name="ai-cs-py-chat",
    ip="0.0.0.0",
    port=8000
)
```

### 2. Java 调用 Python（Feign）

```java
@FeignClient(name = "ai-cs-py-chat")
public interface ChatServiceClient {
    @PostMapping("/api/chat")
    Result<ChatResponse> chat(@RequestBody ChatRequest req);
}
```

### 3. OpenTelemetry 链路追踪

```python
# app/core/tracing.py
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor

FastAPIInstrumentor.instrument_app(app)

# 与 Java 侧 Spring Cloud Sleuth 共用 Jaeger
```

---

## ⚡ 性能优化

### 异步 I/O（核心优势）

```python
# ❌ 同步阻塞（所有请求排队）
def get_data():
    response = requests.get("https://api.example.com")  # 阻塞 1s
    return response.json()

# ✅ 异步并发（100 个请求 1s 完成）
async def get_data():
    async with httpx.AsyncClient() as client:
        response = await client.get("https://api.example.com")
        return response.json()

# 并发执行
results = await asyncio.gather(*[get_data() for _ in range(100)])
```

### Worker 配置（对标 Tomcat 线程池）

```bash
# Uvicorn 启动（4 个 worker）
uvicorn app.main:app \
  --workers 4 \
  --host 0.0.0.0 \
  --port 8000

# 推荐配置：worker 数 = CPU 核心数
```

---

## 📈 监控与告警

```python
# 与 Java 侧统一的 Prometheus 指标
from prometheus_client import Counter, Histogram

request_count = Counter("http_requests_total", "Total requests")
request_duration = Histogram("http_request_duration_seconds", "Request duration")

@app.middleware("http")
async def metrics_middleware(request, call_next):
    request_count.inc()
    start = time.perf_counter()
    response = await call_next(request)
    request_duration.observe(time.perf_counter() - start)
    return response
```

---

## 🔧 开发环境搭建

```bash
# 1. 克隆项目
git clone https://github.com/your-org/ai-customer-service.git
cd ai-customer-service/ai-cs-py-chat

# 2. 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 3. 安装依赖
pip install -r requirements.txt

# 4. 配置环境变量
cp .env.example .env
# 编辑 .env 填入 LLM_API_KEY

# 5. 启动服务
uvicorn app.main:app --reload

# 6. 访问
open http://localhost:8000/docs
```

---

## 📚 学习资源

### 内部文档
- [01-FastAPI对话服务实战](01-FastAPI对话服务实战.md) — 架构解析
- [22-Python速成](../22-Python速成/README.md) — 从零到生产的完整路径
- [ai-cs-py-recommend](../../ai-cs-py-recommend/) — 完整示例项目

### 外部资源
- [FastAPI 官方文档](https://fastapi.tiangolo.com/)
- [asyncio 文档](https://docs.python.org/3/library/asyncio.html)
- [OpenTelemetry Python](https://opentelemetry.io/docs/languages/python/)

---

## ✅ 检查清单

```
开发
□ FastAPI + Pydantic 校验
□ 异步 I/O（不用 requests / time.sleep）
□ 日志结构化（JSON 格式）
□ 测试覆盖率 ≥ 60%

部署
□ Docker 多阶段构建
□ 健康检查端点（/health）
□ Prometheus 指标暴露（/metrics）
□ Uvicorn worker 配置

整合
□ Nacos 服务注册
□ OpenTelemetry 链路追踪
□ 与 Java 服务 trace_id 一致
□ 统一可观测平台（Jaeger/Grafana）
```

---

**下一步**：
1. 学习 [22-Python速成](../22-Python速成/) 补齐语言基础
2. 研读 [ai-cs-py-chat 源码](../../ai-cs-py-chat/)
3. 实现 [ai-cs-py-recommend](../../ai-cs-py-recommend/) 并与 Java 服务整合
