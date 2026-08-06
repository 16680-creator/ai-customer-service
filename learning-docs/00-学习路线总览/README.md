# AI 客服系统 - 全栈学习路线总览

> 本文档基于 `ai-customer-service` 项目的实际技术栈编写，帮助你系统性地掌握全栈 + AI + Java 开发所需的全部知识。

---

## 项目简介

这是一个 **AI 智能客服平台**，采用微服务架构，包含以下服务：

| 服务模块 | 端口 | 职责 |
|---------|------|------|
| ai-cs-gateway | 8080 | API 网关，统一入口、路由、鉴权 |
| ai-cs-user | 8081 | 用户管理、注册登录、JWT 鉴权 |
| ai-cs-knowledge | 8082 | 知识库管理、文档上传 |
| ai-cs-chat | 8083 | AI 对话、RAG 检索增强、Spring AI |
| ai-cs-message | 8084 | 站内消息、会话记录 |
| ai-cs-notify | 8085 | 通知推送 |
| ai-cs-search | 8086 | 全文搜索（Elasticsearch） |
| ai-cs-order | 8087 | 订单、购物车、支付、促销 |
| ai-cs-product | 8088 | 商品管理 |
| ai-cs-frontend | 5173 | Vue3 前端 |

---

## 技术栈全景图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│   Vue Router · Element Plus · Axios · Vite                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / SSE
┌──────────────────────────▼──────────────────────────────────┐
│                   API 网关 (Spring Cloud Gateway)             │
│   路由转发 · CORS · JWT 鉴权过滤器                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                      微服务集群                               │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │  User   │ │  Chat   │ │  Order  │ │ Product │  ...      │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │
│         Spring Boot 3.2 + Spring Cloud Alibaba              │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                      基础设施层                               │
│  MySQL 8.0 · Redis 7 · Elasticsearch 8.12                  │
│  RocketMQ 5.1 · Nacos 2.3 · MinIO                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 学习路线（建议顺序）

### 第一阶段：打地基（1-2 周）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 1 | [01-Java基础](../01-Java基础/) | Java 17 核心特性、Maven 多模块 | `pom.xml`、`ai-cs-common` |
| 2 | [02-Spring微服务](../02-Spring微服务/) | Spring Boot、Cloud、Nacos、Gateway、SpringDoc 接口文档 | 各服务 `application.yml` |

### 第二阶段：核心能力（2-3 周）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 3 | [03-数据库与ORM](../03-数据库与ORM/) | MySQL、MyBatis-Plus | `ai-cs-order`、`ai-cs-product` |
| 4 | [04-中间件](../04-中间件/) | Redis、RocketMQ、ES、MinIO、SSE、WebSocket | `docker-compose.yml`、`ai-cs-notify` |

### 第三阶段：AI 能力（1-2 周）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 5 | [05-AI集成](../05-AI集成/) | Spring AI、RAG、LLM 工程化 | `ai-cs-chat` |

### 第四阶段：前端（1-2 周）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 6 | [06-前端开发](../06-前端开发/) | Vue3、Element Plus、路由守卫、Axios 封装、前后端联调 | `ai-cs-frontend` |

### 第五阶段：工程化（1-2 周）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 7 | [07-运维部署](../07-运维部署/) | Docker、K8s、Jenkins CI/CD | `Dockerfile`、`Jenkinsfile`、`deploy/` |
| 8 | [08-测试](../08-测试/) | JUnit5、Mockito、TDD、覆盖率 | `ai-cs-order/src/test` |

### 第六阶段：进阶（持续）

| 序号 | 文件夹 | 内容 | 对应项目代码 |
|------|--------|------|-------------|
| 9 | [09-安全与设计模式](../09-安全与设计模式/) | JWT 鉴权、接口安全、限流、异常处理、设计模式 | `ai-cs-common`、`ai-cs-gateway` |

---

## 学习建议

1. **边学边看代码**：每个文档都标注了对应的项目文件路径，学完一个知识点就去对应代码里找
2. **先跑起来**：先用 `docker-compose up -d` 启动基础设施，再逐个启动微服务
3. **动手改**：尝试给现有服务加一个接口、写一个测试，比纯看文档有效 10 倍
4. **画架构图**：每学完一个模块，自己画一遍调用链路图
5. **记笔记**：遇到坑记下来，这些都是面试时的谈资

---

## 环境准备清单

- [ ] JDK 17+
- [ ] Maven 3.8+
- [ ] Node.js 18+ / npm 9+
- [ ] Docker Desktop（含 Docker Compose）
- [ ] IDEA（推荐）或 VS Code
- [ ] Git
- [ ] Postman / Apifox（接口调试）
- [ ] Navicat / DBeaver（数据库管理）

---

## 快速启动项目

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 等待所有服务健康（约 1-2 分钟）
docker-compose ps

# 3. 编译后端
mvn clean package -DskipTests

# 4. 启动网关（先启动这个）
cd ai-cs-gateway && mvn spring-boot:run

# 5. 启动 AI 对话服务
cd ai-cs-chat && mvn spring-boot:run

# 6. 启动前端
cd ai-cs-frontend && npm install && npm run dev

# 7. 访问
# 前端: http://localhost:5173
# 网关: http://localhost:8080
# Nacos: http://localhost:8848/nacos
```
