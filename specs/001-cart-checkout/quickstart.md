# 快速启动：电商购物车结算流程

**日期**: 2026-08-01

---

## 前置条件

1. JDK 17 已安装
2. Maven 3.9+ 已安装
3. MySQL 8.0 运行中（端口 3306）
4. Redis 7.x 运行中（端口 6379）
5. RocketMQ 5.x 运行中（NameServer 端口 9876）
6. Nacos 2.3 运行中（端口 8848）

## 数据库初始化

```bash
mysql -u root -p ai_customer_service < deploy/mysql/order-init.sql
```

## 启动服务

```bash
# 1. 启动基础设施（如使用 Docker Compose）
docker-compose up -d mysql redis rocketmq nacos

# 2. 编译项目
mvn clean install -DskipTests

# 3. 启动订单服务
mvn -pl ai-cs-order spring-boot:run

# 4. 启动网关
mvn -pl ai-cs-gateway spring-boot:run
```

## 验证

```bash
# 健康检查
curl http://localhost:8084/actuator/health

# 获取购物车（需携带 JWT Token）
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/order/cart/list

# Swagger UI
open http://localhost:8084/swagger-ui.html
```

## 关键配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| server.port | 8084 | 订单服务端口 |
| spring.datasource.url | jdbc:mysql://localhost:3306/ai_customer_service | 数据库连接 |
| spring.data.redis.host | localhost | Redis 地址 |
| rocketmq.name-server | localhost:9876 | RocketMQ NameServer |
| order.timeout-minutes | 30 | 订单超时时间（分钟） |

## 开发测试

```bash
# 运行单元测试
mvn -pl ai-cs-order test

# 运行集成测试（需要基础设施）
mvn -pl ai-cs-order verify -P integration-test
```
