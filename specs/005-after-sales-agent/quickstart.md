# 快速启动：智能客服 Agent 编排与人工转接

## 1. 初始化数据库

```bash
mysql -uroot -p < deploy/mysql/after-sales-agent-init.sql
# 或全量：mysql -uroot -p < deploy/mysql/all-init.sql
```

## 2. 启动微服务

```bash
mvn clean install -DskipTests
mvn -pl ai-cs-order spring-boot:run    # 8087（含售后命令）
mvn -pl ai-cs-product spring-boot:run  # 8088（含商品推荐）
mvn -pl ai-cs-message spring-boot:run  # 8085（含 Agent 轨迹）
mvn -pl ai-cs-notify spring-boot:run   # 8086（含转人工通知）
mvn -pl ai-cs-chat spring-boot:run     # 8083（Agent 编排入口）
```

## 3. 演示目标场景

```bash
# 1) 发起售后 + 推荐
curl -X POST http://localhost:8080/chat/agent \
  -H "X-User-Id: 1" -H "Content-Type: application/json" \
  -d '{"sessionId":1,"input":"我昨天买的耳机坏了，想换货，另外帮我看看有没有同价位降噪更好的"}'

# 2) 确认换货（使用返回的 runId 与 confirmationToken）
curl -X POST http://localhost:8080/chat/agent/confirm \
  -H "X-User-Id: 1" -H "Content-Type: application/json" \
  -d '{"runId":"<runId>","token":"<confirmationToken>","decision":"CONFIRM","sessionId":1}'

# 3) 审计回放
curl http://localhost:8080/chat/agent/runs/<runId> -H "X-User-Id: 1"
```

> 端口 8080 为网关；直接访问各服务端口（8083/8085/8086/8087/8088）亦可。
> 演示前需确保订单服务中存在当前用户（X-User-Id）的 PAID 订单与售后规则知识库种子数据（after-sales-agent-init.sql 已含）。

## 4. 测试

```bash
mvn -pl ai-cs-order,ai-cs-product,ai-cs-message,ai-cs-notify,ai-cs-chat verify
```

## 5. 配置（ai-cs-chat）

见 `specs/005-after-sales-agent/contracts/rest-api.md` 第 7 节（aics.agent.*）。
