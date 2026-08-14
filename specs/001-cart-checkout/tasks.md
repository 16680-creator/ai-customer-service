# 任务：电商购物车结算流程

**输入**: 来自 `specs/001-cart-checkout/` 的设计文档
**前置条件**: plan.md（必填）、spec.md（用户故事必填）、research.md、data-model.md、contracts/

**测试**: 采用 TDD 方式（宪法第2-1条），测试任务必选。每个用户故事先写测试并验证失败（Red），再实现使其通过（Green），再重构（Refactor）。

**组织方式**: 任务按用户故事分组，以支持每个故事的独立实施和测试。

## 格式：`[ID] [P?] [故事] 描述`

- **[P]**：可并行执行（不同文件，无依赖）
- **[故事]**：任务所属的用户故事（例如：US1、US2、US3）
- 描述中包含确切的文件路径

## 路径约定

- **后端服务**：`ai-cs-order/src/main/java/com/aics/order/`
- **测试代码**：`ai-cs-order/src/test/java/com/aics/order/`
- **前端页面**：`ai-cs-frontend/src/views/`
- **数据库SQL**：`deploy/mysql/`

---

## 阶段 1：初始化（共享基础设施）

**目的**：创建 ai-cs-order 微服务模块骨架

- [x] T001 创建 ai-cs-order 模块目录结构和 pom.xml（依赖 ai-cs-common、Spring Boot Starter Web、MyBatis-Plus、Redis、RocketMQ）
- [x] T002 [P] 创建启动类 ai-cs-order/src/main/java/com/aics/order/OrderApplication.java
- [x] T003 [P] 创建配置文件 ai-cs-order/src/main/resources/application.yml（端口8084、数据源、Redis、RocketMQ）
- [x] T004 [P] 在父 pom.xml 中添加 ai-cs-order 模块声明
- [x] T005 [P] 创建 Dockerfile ai-cs-order/Dockerfile

---

## 阶段 2：基础层（阻塞性前置条件）

**目的**：所有用户故事实施前必须完成的核心基础设施

**⚠️ 关键**：此阶段完成前，不得开始任何用户故事的工作

- [x] T006 创建数据库初始化 SQL deploy/mysql/order-init.sql（cart_item、orders、order_item、coupon、full_reduction_rule 五张表）
- [x] T007 [P] 创建订单状态枚举 ai-cs-order/src/main/java/com/aics/order/enums/OrderStatus.java（PENDING_PAY、PAID、CANCELLED）
- [x] T008 [P] 创建支付方式枚举 ai-cs-order/src/main/java/com/aics/order/enums/PaymentMethod.java（WECHAT、ALIPAY、BANK_CARD）
- [x] T009 [P] 创建优惠券状态枚举 ai-cs-order/src/main/java/com/aics/order/enums/CouponStatus.java（UNUSED、USED、EXPIRED）
- [x] T010 [P] 创建购物车项实体 ai-cs-order/src/main/java/com/aics/order/entity/CartItem.java
- [x] T011 [P] 创建订单实体 ai-cs-order/src/main/java/com/aics/order/entity/Order.java
- [x] T012 [P] 创建订单项实体 ai-cs-order/src/main/java/com/aics/order/entity/OrderItem.java
- [x] T013 [P] 创建优惠券实体 ai-cs-order/src/main/java/com/aics/order/entity/Coupon.java
- [x] T014 [P] 创建满减规则实体 ai-cs-order/src/main/java/com/aics/order/entity/FullReductionRule.java
- [x] T015 [P] 创建 CartItemMapper ai-cs-order/src/main/java/com/aics/order/mapper/CartItemMapper.java
- [x] T016 [P] 创建 OrderMapper ai-cs-order/src/main/java/com/aics/order/mapper/OrderMapper.java
- [x] T017 [P] 创建 OrderItemMapper ai-cs-order/src/main/java/com/aics/order/mapper/OrderItemMapper.java
- [x] T018 [P] 创建 CouponMapper ai-cs-order/src/main/java/com/aics/order/mapper/CouponMapper.java
- [x] T019 [P] 创建 FullReductionRuleMapper ai-cs-order/src/main/java/com/aics/order/mapper/FullReductionRuleMapper.java
- [x] T020 [P] 创建 DTO 类：CartUpdateDTO、CheckoutDTO、OrderCreateDTO ai-cs-order/src/main/java/com/aics/order/dto/
- [x] T021 [P] 创建 VO 类：CartVO、CheckoutConfirmVO、OrderVO ai-cs-order/src/main/java/com/aics/order/vo/
- [x] T022 [P] 创建业务对象 PriceCalcBO ai-cs-order/src/main/java/com/aics/order/bo/PriceCalcBO.java
- [x] T023 创建模块配置类 ai-cs-order/src/main/java/com/aics/order/config/OrderConfig.java（Redis、RocketMQ、MyBatis-Plus 配置）
- [x] T024 在 ai-cs-gateway 路由配置中添加 ai-cs-order 服务路由规则

**检查点**：基础层就绪 - 可以开始并行实施用户故事

---

## 阶段 3：用户故事 1 - 购物车商品管理（优先级：P1）🎯 MVP

**目标**：用户可查看购物车、修改商品数量、删除商品，系统实时计算金额

**独立测试**：调用购物车 CRUD 接口，验证数量修改后金额正确、库存校验生效、删除后列表更新

### 用户故事 1 的测试（必选 - TDD Red 阶段）⚠️

> **注意：先编写这些测试，确保它们在实施前失败**

- [x] T025 [P] [US1] 编写 CartService 单元测试 ai-cs-order/src/test/java/com/aics/order/service/CartServiceTest.java（覆盖：获取列表、修改数量正常、修改数量超库存、修改数量为0/负数、删除商品）
- [x] T026 [P] [US1] 编写 CartController 接口测试 ai-cs-order/src/test/java/com/aics/order/controller/CartControllerTest.java（覆盖：GET /cart/list、PUT /cart/quantity、DELETE /cart/{id}、PUT /cart/select）

### 用户故事 1 的实施

- [x] T027 [US1] 实现 CartService 接口 ai-cs-order/src/main/java/com/aics/order/service/CartService.java
- [x] T028 [US1] 实现 CartServiceImpl ai-cs-order/src/main/java/com/aics/order/service/impl/CartServiceImpl.java（获取列表、修改数量含库存校验、删除、切换选中状态、金额计算）
- [x] T029 [US1] 实现 CartController ai-cs-order/src/main/java/com/aics/order/controller/CartController.java（GET /cart/list、PUT /cart/quantity、DELETE /cart/{id}、PUT /cart/select，SpringDoc 注解）
- [x] T030 [US1] 创建前端购物车页面 ai-cs-frontend/src/views/CartView.vue（商品列表、数量修改、删除、选中、合计金额展示）
- [x] T031 [US1] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：此时用户故事 1 应完全可用并可独立测试

---

## 阶段 4：用户故事 2 - 优惠计算与结算确认（优先级：P2）

**目标**：系统自动匹配满减规则（取最优）、支持优惠券选择、展示优惠明细和应付金额

**独立测试**：设置满减规则和优惠券，调用结算确认接口，验证优惠计算逻辑（满减取最优、优惠券门槛校验、叠加计算）

### 用户故事 2 的测试（必选 - TDD Red 阶段）⚠️

- [x] T032 [P] [US2] 编写 PromotionService 单元测试 ai-cs-order/src/test/java/com/aics/order/service/PromotionServiceTest.java（覆盖：单条满减命中、多条满减取最优、无满减命中、优惠券可用、优惠券不满足门槛、满减+优惠券叠加、优惠券过期）
- [x] T033 [P] [US2] 编写结算确认接口测试 ai-cs-order/src/test/java/com/aics/order/controller/CheckoutControllerTest.java（覆盖：POST /checkout/confirm 正常、未选商品、优惠券不可用列表）

### 用户故事 2 的实施

- [x] T034 [US2] 实现 PromotionService 接口 ai-cs-order/src/main/java/com/aics/order/service/PromotionService.java
- [x] T035 [US2] 实现 PromotionServiceImpl ai-cs-order/src/main/java/com/aics/order/service/impl/PromotionServiceImpl.java（满减规则匹配取最优、优惠券可用性校验、叠加计算引擎）
- [x] T036 [US2] 实现结算确认接口（在 CartController 或新建 CheckoutController 中）ai-cs-order/src/main/java/com/aics/order/controller/CartController.java（POST /checkout/confirm，返回优惠明细+可用优惠券列表+应付金额）
- [x] T037 [US2] 创建前端结算确认页面 ai-cs-frontend/src/views/CheckoutView.vue（商品清单、满减优惠展示、优惠券选择器、优惠明细、应付金额）
- [x] T038 [US2] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：此时用户故事 1 和 2 均应可独立运行

---

## 阶段 5：用户故事 3 - 支付方式选择与下单（优先级：P3）

**目标**：用户选择支付方式提交订单，系统生成订单、预扣库存、跳转支付，支持超时取消和支付失败重试

**独立测试**：调用下单接口验证订单生成、库存预扣、延迟消息发送；模拟支付回调验证状态变更；模拟超时验证自动取消

### 用户故事 3 的测试（必选 - TDD Red 阶段）⚠️

- [x] T039 [P] [US3] 编写 OrderService 单元测试 ai-cs-order/src/test/java/com/aics/order/service/OrderServiceTest.java（覆盖：正常下单、库存不足拒绝、订单号唯一性、超时取消、支付成功状态变更、支付回调幂等）
- [x] T040 [P] [US3] 编写 PaymentService 单元测试 ai-cs-order/src/test/java/com/aics/order/service/PaymentServiceTest.java（覆盖：三种支付方式策略选择、支付失败保持待支付、更换支付方式重试）
- [x] T041 [P] [US3] 编写 OrderController 接口测试 ai-cs-order/src/test/java/com/aics/order/controller/OrderControllerTest.java（覆盖：POST /create、GET /{orderNo}、PUT /{orderNo}/cancel、PUT /{orderNo}/retry-pay）

### 用户故事 3 的实施

- [x] T042 [US3] 实现 PaymentStrategy 接口及三种实现 ai-cs-order/src/main/java/com/aics/order/service/PaymentService.java 和 impl/PaymentServiceImpl.java（策略模式：WechatPayStrategy、AlipayStrategy、BankCardStrategy）
- [x] T043 [US3] 实现 OrderService 接口 ai-cs-order/src/main/java/com/aics/order/service/OrderService.java
- [x] T044 [US3] 实现 OrderServiceImpl ai-cs-order/src/main/java/com/aics/order/service/impl/OrderServiceImpl.java（下单：库存Redis预扣+DB乐观锁、订单创建、优惠券核销、延迟消息发送；回调：幂等状态变更、库存确认扣减、购物车清除；取消：库存归还、优惠券退回）
- [x] T045 [US3] 实现订单超时监听器 ai-cs-order/src/main/java/com/aics/order/listener/OrderTimeoutListener.java（RocketMQ 消费者，检查订单状态，未支付则执行取消逻辑）
- [x] T046 [US3] 实现 OrderController ai-cs-order/src/main/java/com/aics/order/controller/OrderController.java（POST /create、GET /{orderNo}、PUT /{orderNo}/cancel、PUT /{orderNo}/retry-pay，SpringDoc 注解）
- [x] T047 [US3] 实现 PayCallbackController ai-cs-order/src/main/java/com/aics/order/controller/PayCallbackController.java（POST /pay/callback/{paymentMethod}，幂等处理）
- [x] T048 [US3] 运行本故事全部测试与覆盖率校验，验证 Red → Green → Refactor 完成

**检查点**：所有用户故事此时应均可独立运行

---

## 阶段 6：优化与跨切面关注点

**目的**：影响多个用户故事的改进

- [ ] T049 [P] 补充 Redis 库存预扣工具类单元测试（覆盖并发 DECR/INCR 场景）
- [ ] T050 [P] 为所有 Controller 补充 SpringDoc OpenAPI 注解（@Tag、@Operation、@Parameter、@Schema）
- [ ] T051 配置 Spring Boot Actuator 健康检查和指标暴露
- [ ] T052 [P] 更新 API 文档和部署文档
- [ ] T053 运行 quickstart.md 验证全流程可启动
- [ ] T054 运行全量测试确认覆盖率达到宪法第2-1条阈值（行 ≥ 40%、分支 ≥ 30%）

---

## 依赖与执行顺序

### 阶段依赖

- **初始化（阶段 1）**：无依赖 - 可立即开始
- **基础层（阶段 2）**：依赖初始化完成 - 阻塞所有用户故事
- **用户故事 1（阶段 3）**：依赖基础层完成
- **用户故事 2（阶段 4）**：依赖基础层完成（与 US1 无强依赖，可并行）
- **用户故事 3（阶段 5）**：依赖基础层 + US2（下单需要优惠计算能力）
- **优化（阶段 6）**：依赖所有用户故事完成

### 用户故事依赖

- **US1（P1）**：基础层完成后可开始 - 无其他故事依赖
- **US2（P2）**：基础层完成后可开始 - 无其他故事依赖（可与 US1 并行）
- **US3（P3）**：依赖 US2 的 PromotionService（下单时需计算优惠金额）

### 每个用户故事内部

- 测试（必选）必须先编写，并在实施前运行确认失败（Red 证据）
- 实体/Mapper 已在基础层完成
- Service 先于 Controller
- 后端先于前端页面
- 故事完成后再进入下一优先级

### 并行机会

- 所有标记 [P] 的初始化任务可并行执行
- 所有标记 [P] 的基础层任务可并行执行（阶段 2 内）
- US1 和 US2 可并行开发（无互相依赖）
- 某用户故事内所有标记 [P] 的测试可并行执行

---

## 实施策略

### MVP 优先（仅用户故事 1）

1. 完成阶段 1：初始化
2. 完成阶段 2：基础层（关键 - 阻塞所有故事）
3. 完成阶段 3：用户故事 1（购物车管理）
4. **停止并验证**：独立测试购物车 CRUD
5. 准备好后部署/演示

### 增量交付

1. 完成初始化 + 基础层 → 基础层就绪
2. 添加 US1（购物车） → 独立测试 → 部署/演示（MVP！）
3. 添加 US2（优惠计算） → 独立测试 → 部署/演示
4. 添加 US3（支付下单） → 独立测试 → 部署/演示
5. 每个故事增加价值且不破坏之前的故事

---

## 备注

- [P] 任务 = 不同文件，无依赖
- [故事] 标签将任务映射到特定用户故事以实现可追溯性
- 每个用户故事应可独立完成和测试
- 实施前必须运行测试并留存失败证据（Red）；实施后必须运行测试确认通过（Green）
- 每个任务或逻辑分组后提交
- 在任何检查点停止以独立验证故事
- 避免：模糊任务、同文件冲突、破坏独立性的跨故事依赖
