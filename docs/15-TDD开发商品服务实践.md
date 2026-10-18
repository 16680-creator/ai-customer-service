# TDD 开发实践：商品服务（ai-cs-product）

> 记录时间：2026-08-01  
> 开发模式：TDD（Test-Driven Development）  
> 分支：`feature/ai-chat-and-frontend`

---

## 一、TDD 流程概述

本次开发严格遵循 **Red → Green → Refactor** 三阶段循环：

```
┌─────────────────────────────────────────────────────────┐
│  Red（红灯）    → 先编写测试，此时测试必然失败            │
│  Green（绿灯）  → 编写最小实现代码，使测试全部通过         │
│  Refactor（重构）→ 优化代码结构，补充集成配置，回归验证     │
└─────────────────────────────────────────────────────────┘
```

---

## 二、详细开发过程

### 阶段 0：需求分析

分析现有代码中商品相关引用，确定 `ai-cs-product` 的职责边界：

- 订单服务（ai-cs-order）中 `CartItem`、`OrderItem` 均引用了 `productId`、`productName`、`productPrice`
- Redis 中通过 `stock:{productId}` 管理库存
- 商品服务需独立提供：商品 CRUD、库存扣减/恢复、分类管理

### 阶段 1：Red — 先写测试

**先于任何实现代码**，编写了两个测试类共 26 个测试用例：

| 测试类 | 用例数 | 覆盖场景 |
|--------|--------|----------|
| `ProductServiceTest` | 17 | 创建商品（成功/重名/分类不存在）、分页查询（正常/空）、详情（成功/不存在）、更新（成功/不存在）、删除（成功/不存在）、扣减库存（成功/不足/商品不存在）、恢复库存、创建分类、分类列表 |
| `ProductControllerTest` | 9 | 创建/列表/详情/更新/删除/扣库存/恢复库存/创建分类/分类列表 的 Controller 委托正确性 |

测试技术选型：
- `@ExtendWith(MockitoExtension.class)` — 纯 Mockito 单元测试
- `@Mock` + `@InjectMocks` — 依赖注入 mock
- 直接调用 Controller 方法（非 MockMvc），与 ai-cs-order 保持一致

### 阶段 2：Green — 最小实现

编写让测试通过的最小代码：

```
ai-cs-product/src/main/java/com/aics/product/
├── controller/ProductController.java       ← REST API（9 个端点）
├── dto/ProductCreateDTO.java               ← 创建请求（含 JSR-303 校验）
├── dto/ProductUpdateDTO.java               ← 更新请求
├── entity/Product.java                     ← 商品实体（MyBatis-Plus）
├── entity/ProductCategory.java             ← 分类实体
├── mapper/ProductMapper.java               ← 数据访问
├── mapper/ProductCategoryMapper.java
├── service/ProductService.java             ← 服务接口
├── service/impl/ProductServiceImpl.java    ← 服务实现（核心业务逻辑）
├── vo/ProductVO.java                       ← 返回视图对象
└── ProductApplication.java                 ← 启动类（port: 8088）
```

运行测试：
```bash
mvn -pl ai-cs-product -am test
```

结果：
```
Tests run: 9, Failures: 0, Errors: 0  -- ProductControllerTest
Tests run: 17, Failures: 0, Errors: 0 -- ProductServiceTest
Tests run: 26, Failures: 0, Errors: 0
BUILD SUCCESS
```

### 阶段 3：Refactor — 重构与集成

测试全绿后进行代码优化和集成配置补充：

- 补充数据库初始化 SQL（`deploy/mysql/product-init.sql`）
- 补充 Dockerfile（`ai-cs-product/Dockerfile`）
- 补充 K8s 部署配置（`deploy/k8s/services/product-service.yaml`）
- 注册网关路由（`/api/product/**` → `lb://ai-cs-product`）
- 新增商品模块错误码（8001~8005）
- 更新项目文档（CLAUDE.md 工程结构）
- 最终回归验证：26 个测试全部通过，无回归

---

## 三、涉及的文件清单

### 新建文件（ai-cs-product 模块）

| 文件路径 | 说明 |
|----------|------|
| `ai-cs-product/pom.xml` | 模块 POM（继承父 POM，含 JaCoCo） |
| `ai-cs-product/Dockerfile` | Docker 镜像构建 |
| `ai-cs-product/src/main/java/com/aics/product/ProductApplication.java` | 启动类 |
| `ai-cs-product/src/main/java/com/aics/product/controller/ProductController.java` | REST 控制器 |
| `ai-cs-product/src/main/java/com/aics/product/dto/ProductCreateDTO.java` | 创建 DTO |
| `ai-cs-product/src/main/java/com/aics/product/dto/ProductUpdateDTO.java` | 更新 DTO |
| `ai-cs-product/src/main/java/com/aics/product/entity/Product.java` | 商品实体 |
| `ai-cs-product/src/main/java/com/aics/product/entity/ProductCategory.java` | 分类实体 |
| `ai-cs-product/src/main/java/com/aics/product/mapper/ProductMapper.java` | 商品 Mapper |
| `ai-cs-product/src/main/java/com/aics/product/mapper/ProductCategoryMapper.java` | 分类 Mapper |
| `ai-cs-product/src/main/java/com/aics/product/service/ProductService.java` | 服务接口 |
| `ai-cs-product/src/main/java/com/aics/product/service/impl/ProductServiceImpl.java` | 服务实现 |
| `ai-cs-product/src/main/java/com/aics/product/vo/ProductVO.java` | 视图对象 |
| `ai-cs-product/src/main/resources/application.yml` | 服务配置 |
| `ai-cs-product/src/test/resources/application.yml` | 测试配置（禁用 Nacos/Redis） |
| `ai-cs-product/src/test/java/com/aics/product/service/ProductServiceTest.java` | 服务测试（17 例） |
| `ai-cs-product/src/test/java/com/aics/product/controller/ProductControllerTest.java` | 控制器测试（9 例） |

### 新建文件（部署 & 配置）

| 文件路径 | 说明 |
|----------|------|
| `deploy/mysql/product-init.sql` | 商品库建表 + 初始数据 |
| `deploy/k8s/services/product-service.yaml` | K8s Deployment + Service |

### 修改文件

| 文件路径 | 变更内容 |
|----------|----------|
| `pom.xml`（根） | `<modules>` 新增 `ai-cs-product` |
| `ai-cs-common/.../ResultCode.java` | 新增商品模块错误码 8001~8005 |
| `ai-cs-gateway/.../RouteConfig.java` | 新增 `/api/product/**` 路由 |
| `CLAUDE.md` | 工程结构 + 依赖方向更新 |

---

## 四、参考的已有文档/代码

开发过程中参考了以下项目内文档和代码模式：

| 参考来源 | 用途 |
|----------|------|
| `CLAUDE.md` — TDD 开发规范章节 | 确认 Red→Green→Refactor 流程、覆盖率门禁、测试分层策略 |
| `ai-cs-order/pom.xml` | 模块 POM 结构模板（依赖、JaCoCo 插件） |
| `ai-cs-order/src/main/resources/application.yml` | 服务配置模板（端口、数据源、Nacos、MyBatis-Plus） |
| `ai-cs-order/.../OrderApplication.java` | 启动类模式（@MapperScan + @EnableDiscoveryClient） |
| `ai-cs-order/.../CartServiceImpl.java` | 业务实现模式（Redis 库存校验、LambdaQueryWrapper） |
| `ai-cs-order/src/test/.../CartServiceTest.java` | 测试编写模式（Mockito + @DisplayName） |
| `ai-cs-order/src/test/.../OrderControllerTest.java` | Controller 测试模式（直接方法调用） |
| `ai-cs-order/src/test/resources/application.yml` | 测试配置模板（禁用 Nacos/排除自动配置） |
| `ai-cs-common/.../Result.java` | 统一返回体结构 |
| `ai-cs-common/.../ResultCode.java` | 错误码分段规则（商品模块 = 8xxx） |
| `ai-cs-gateway/.../RouteConfig.java` | 网关路由注册模式 |
| `deploy/mysql/init.sql` | 建表 SQL 风格参考 |
| `deploy/k8s/services/ai-chat-service.yaml` | K8s 部署 YAML 模板 |

---

## 五、关键技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| Controller 测试方式 | 直接调用方法（非 MockMvc） | @MapperScan 与 @WebMvcTest 不兼容；与 ai-cs-order 保持一致 |
| 库存管理 | DB + Redis 双写 | DB 保证持久性，Redis 提供高并发读性能 |
| 商品删除 | 逻辑删除（@TableLogic） | 保留历史数据，支持订单溯源 |
| ID 策略 | AUTO_INCREMENT | 商品量可控，无需分布式 ID |
| 测试隔离 | 纯 Mockito（无 Spring 上下文） | 速度快、无外部依赖、CI 友好 |

---

## 六、TDD 收益总结

1. **设计先行**：写测试时自然思考 API 契约（方法签名、返回结构、异常场景），避免过度设计
2. **即时反馈**：26 个测试 3 秒内跑完，任何回归立即暴露
3. **高覆盖率**：Service 层核心逻辑（CRUD + 库存 + 分类）全覆盖
4. **可重构信心**：有测试兜底，后续优化（如加缓存、加搜索）不怕破坏已有行为
5. **文档即测试**：`@DisplayName` 中文描述即活文档，新人可直接阅读测试理解业务
