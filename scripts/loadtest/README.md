# 性能压测脚本

配套学习文档：[learning-docs/08-测试/05-性能压测实战](../../learning-docs/08-测试/05-性能压测实战-k6与JMeter.md)（含工具选型、脚本逐段精读、基线报告模板）。

## 前置

1. 安装 k6：https://k6.io/docs/get-started/installation/ （Windows: `winget install k6 --source winget` 或 `choco install k6`）
2. 启动被测链路：`docker-compose up -d` 基础设施 + 对应微服务（或直接压 py-chat `python -m uvicorn app.main:app --port 8000`）

## 脚本

| 脚本 | 场景 | 关键观察 |
|---|---|---|
| `k6/gateway-rate-limit.js` | 30 req/s 恒定到达率打网关路由，超 replenish-rate=5/burst=10 | 429 比例、放行请求 p95 |
| `k6/sse-chat.js` | 阶梯加压至 20 VU 保持 SSE 流式会话 | 首 token 时延（http_req_waiting）、整流时长、失败率 |

## 运行

```bash
# 1. 网关限流（把 PATH 换成你想打的目标路由）
k6 run -e BASE=http://localhost:8080 -e PATH="/api/product/page?current=1&size=10" k6/gateway-rate-limit.js

# 2. SSE 流式（直连 py-chat 或走网关）
k6 run -e BASE=http://localhost:8000 k6/sse-chat.js

# 3. 压测时同步观测（另开终端）：Prometheus/Grafana 已由观测栈提供
#    docker compose -f deploy/docker-compose/docker-compose-observability.yml up -d
```

## 注意

- **别用本机既当压测机又当被测机出报告**：资源互相挤压，数据只可作相对比较；正式基线在独立环境跑。
- 限流压测会真实消耗 Redis 令牌桶计数与上游资源，**跑完确认无残留流量**。
- JMeter（登录/下单链路）按学习文档第五节用 GUI 搭建（5 分钟），仓库不提交手写 .jmx。
