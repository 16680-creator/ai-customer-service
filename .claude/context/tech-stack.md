# 技术栈定义（Tech Stack）

> 本文件是项目技术栈的权威定义，所有功能开发必须基于此底座。

---

## 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Maven | 3.9+ | 构建工具 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 微服务增强（Nacos、Sentinel） |
| Spring Cloud Gateway | 4.1.x | API 网关 |
| Spring AI | 1.0.0 | AI 能力集成（Chat、RAG、Function Calling） |
| MyBatis-Plus | 3.5.6 | ORM 框架 |
| ShardingSphere | 5.5.0 | 分库分表（用户表已落地：user_db_0/1 × sys_user_0..3，后四位取模） |
| SpringDoc OpenAPI | 2.3.0 | API 文档 |
| Lombok | 1.18.30 | 代码简化 |
| MapStruct | 1.5.5.Final | 对象映射 |
| Hutool | 5.8.26 | 工具库 |
| JJWT | 0.12.5 | JWT 认证 |
| Guava | 33.0.0-jre | 基础工具 |

## 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 前端框架 |
| Vite | 8.x | 构建工具 |
| Element Plus | 2.14+ | UI 组件库 |
| Vue Router | 4.6+ | 路由管理 |
| Axios | 1.18+ | HTTP 客户端 |

## 中间件与基础设施

| 组件 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 业务数据持久化 |
| Redis | 7.x | 缓存、会话管理、分布式锁 |
| Elasticsearch | 8.12 | 全文搜索、知识库向量检索 |
| RocketMQ | 5.x（Spring 2.3.0） | 异步消息、事件驱动 |
| Nacos | 2.3.x | 服务注册发现、配置中心 |

## 部署与运维

| 技术 | 用途 |
|------|------|
| Docker | 容器化部署 |
| Docker Compose | 本地开发环境编排 |
| Kubernetes | 生产环境容器编排 |
| Jenkins | CI/CD 流水线 |

## 版本约束规则

1. 所有依赖版本由父 POM `dependencyManagement` 统一管控
2. 子模块禁止自行声明版本号（必须继承父 POM）
3. 引入新依赖需评估兼容性并更新本文件
4. Spring Boot / Spring Cloud / Spring Cloud Alibaba 版本必须保持官方兼容矩阵对齐
