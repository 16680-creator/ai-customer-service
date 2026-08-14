# 实施计划：电商购物车结算流程

**分支**: `001-cart-checkout` | **日期**: 2026-08-01 | **规格**: [spec.md](spec.md)
**输入**: 来自 `specs/001-cart-checkout/spec.md` 的功能规格

## 摘要

构建完整的电商购物车结算链路，涵盖三大用户故事：购物车商品管理（数量修改、删除、库存校验）、优惠计算与结算确认（满减自动取最优 + 优惠券选择 + 叠加计算）、支付方式选择与下单（微信/支付宝/银行卡、订单生成、支付跳转、超时取消、失败重试）。

技术方案：基于现有微服务架构，新建 `ai-cs-order` 模块承载购物车、订单、优惠计算核心逻辑，通过 Spring Cloud OpenFeign 调用商品服务获取商品/库存数据，使用 Redis 实现库存预扣和分布式锁防超卖，使用 RocketMQ 延迟消息实现订单超时自动取消。

## 技术上下文

**语言/版本**：Java 17
**主要依赖**：Spring Boot 3.2.5、Spring Cloud 2023.0.1、MyBatis-Plus 3.5.6、Spring AI 1.0.0（不涉及）
**存储**：MySQL 8.0（订单、购物车、优惠券持久化）、Redis 7.x（库存预扣、分布式锁、购物车缓存）
**测试**：JUnit 5 + Mockito（单元）、Spring Boot Test（集成）
**目标平台**：Linux 服务器（Docker 容器化）
**项目类型**：微服务（Web 服务）
**性能目标**：1000 并发结算、金额计算 < 1s、结算页加载 < 2s
**约束**：库存零超卖、支付回调幂等、30 分钟超时取消
**规模/范围**：新增 1 个微服务模块（ai-cs-order），约 30+ 个类文件

## 宪法检查

*门禁：必须在第 0 阶段研究前通过。第 1 阶段设计后重新检查。*

| 宪法条款 | 合规状态 | 说明 |
|----------|----------|------|
| 第2条 SDD流程 | ✅ 通过 | 按 specify → clarify → plan 顺序推进 |
| 第2-1条 TDD | ✅ 计划中 | tasks 阶段将为每个实现任务配置前置测试任务 |
| 第12条 文档规范 | ✅ 通过 | 中文文档、英文命名、SpringDoc API 注解 |
| 第13条 配置安全 | ✅ 通过 | 支付密钥通过环境变量/Nacos 注入，不入版本控制 |
| 第13-1条 SQL交付 | ✅ 计划中 | 将在 data-model.md 中定义表结构，SQL 输出到 deploy/mysql/ |
| 第13-2条 公共复用 | ✅ 通过 | 复用 ai-cs-common 的 Result/ResultCode/BusinessException/GlobalExceptionHandler |
| 第16条 模块架构 | ✅ 通过 | 新建 ai-cs-order 模块，依赖 ai-cs-common，通过 Feign 调用商品服务 |
| 第17条 技术优先 | ✅ 通过 | 优先使用 Spring Cloud/Boot 官方能力 |
| 第20条 编码规范 | ✅ 通过 | 构造器注入、Lombok、MapStruct、SLF4J |
| 第21条 领域模型 | ✅ 通过 | Entity/DTO/VO/BO 分层命名 |

**门禁结论**：全部通过，无违规项，可进入 Phase 0。

## 项目结构

### 文档（本功能）

```text
specs/001-cart-checkout/
├── plan.md              # 本文件
├── research.md          # 第 0 阶段：技术调研与决策
├── data-model.md        # 第 1 阶段：数据模型设计
├── quickstart.md        # 第 1 阶段：快速启动指南
├── contracts/           # 第 1 阶段：API 契约
│   └── rest-api.md      # REST API 契约定义
├── checklists/
│   └── requirements.md  # 规格质量检查清单
└── tasks.md             # 第 2 阶段输出（/speckit-tasks 生成）
```

### 源代码（仓库根目录）

```text
ai-cs-order/                          ← 新建微服务模块
├── src/main/java/com/aics/order/
│   ├── OrderApplication.java         ← 启动类
│   ├── controller/
│   │   ├── CartController.java       ← 购物车接口
│   │   ├── OrderController.java      ← 订单接口
│   │   └── PayCallbackController.java← 支付回调接口
│   ├── service/
│   │   ├── CartService.java          ← 购物车服务
│   │   ├── OrderService.java         ← 订单服务
│   │   ├── PromotionService.java     ← 优惠计算服务
│   │   ├── PaymentService.java       ← 支付服务
│   │   └── impl/
│   │       ├── CartServiceImpl.java
│   │       ├── OrderServiceImpl.java
│   │       ├── PromotionServiceImpl.java
│   │       └── PaymentServiceImpl.java
│   ├── entity/
│   │   ├── CartItem.java             ← 购物车项实体
│   │   ├── Order.java                ← 订单实体
│   │   ├── OrderItem.java            ← 订单项实体
│   │   ├── Coupon.java               ← 优惠券实体
│   │   └── FullReductionRule.java    ← 满减规则实体
│   ├── dto/
│   │   ├── CartUpdateDTO.java        ← 购物车修改请求
│   │   ├── CheckoutDTO.java          ← 结算请求
│   │   └── OrderCreateDTO.java       ← 下单请求
│   ├── vo/
│   │   ├── CartVO.java               ← 购物车展示
│   │   ├── CheckoutConfirmVO.java    ← 结算确认页展示
│   │   └── OrderVO.java              ← 订单展示
│   ├── bo/
│   │   └── PriceCalcBO.java          ← 价格计算业务对象
│   ├── mapper/
│   │   ├── CartItemMapper.java
│   │   ├── OrderMapper.java
│   │   ├── OrderItemMapper.java
│   │   ├── CouponMapper.java
│   │   └── FullReductionRuleMapper.java
│   ├── enums/
│   │   ├── OrderStatus.java          ← 订单状态枚举
│   │   ├── PaymentMethod.java        ← 支付方式枚举
│   │   └── CouponStatus.java         ← 优惠券状态枚举
│   ├── config/
│   │   └── OrderConfig.java          ← 模块配置
│   └── listener/
│       └── OrderTimeoutListener.java ← 超时取消消息监听
├── src/main/resources/
│   └── application.yml
├── src/test/java/com/aics/order/
│   ├── service/
│   │   ├── CartServiceTest.java
│   │   ├── OrderServiceTest.java
│   │   └── PromotionServiceTest.java
│   └── controller/
│       └── OrderControllerTest.java
├── Dockerfile
└── pom.xml

ai-cs-frontend/src/views/
├── CartView.vue                      ← 购物车页面
└── CheckoutView.vue                  ← 结算确认页面

deploy/mysql/
└── order-init.sql                    ← 订单模块建表 SQL
```

**结构决策**：采用微服务模块结构，新建 `ai-cs-order` 独立模块。购物车、订单、优惠计算、支付均归属该模块，通过 Feign 调用 `ai-cs-user`（用户信息）和商品服务（商品/库存数据）。前端新增 2 个页面组件。

## 复杂度追踪

> 无宪法违规项，无需填写。
