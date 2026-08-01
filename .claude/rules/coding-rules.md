# 编码规范（Coding Rules）

> 本文件定义项目编码规范，所有代码必须遵循。

---

## 通用规范

1. **字符集**：UTF-8 全局统一
2. **注释语言**：中文
3. **命名语言**：英文（变量、函数、类名）
4. **日志语言**：英文
5. **缩进**：4 空格（Java）/ 2 空格（Vue/JS）

## Java 编码规范

### 命名规范
- 类名：大驼峰（`ChatService`、`OrderQueryService`）
- 方法/变量：小驼峰（`getUserById`、`chatHistory`）
- 常量：全大写下划线（`MAX_RETRY_COUNT`）
- 包名：全小写（`com.aics.chat.service`）

### 分层规范
- Controller 层：参数校验、调用 Service、返回 `Result<T>`
- Service 层：业务逻辑编排
- Mapper 层：数据访问（MyBatis-Plus）
- 禁止 Controller 直接操作 Mapper

### 依赖注入
- 优先使用构造器注入（`@RequiredArgsConstructor`）
- 禁止字段注入（`@Autowired` 在字段上）

### 异常处理
- 业务异常统一使用 `BusinessException`
- 全局异常由 `GlobalExceptionHandler` 统一处理
- 禁止 `e.printStackTrace()`，必须使用 `log.error()`

### 日志规范
- 使用 SLF4J（`@Slf4j`）
- 禁止 `System.out.println()`
- 日志级别：ERROR（异常）> WARN（警告）> INFO（关键流程）> DEBUG（调试）

### 对象转换
- 使用 MapStruct 进行 DTO/VO/Entity 转换
- 禁止手动逐字段 get/set 复制（简单场景除外）

## 前端编码规范

### 命名规范
- 组件文件：大驼峰（`ChatView.vue`）
- 工具函数：小驼峰（`formatDate`）
- CSS 类名：kebab-case（`chat-container`）

### 组件规范
- 使用 Composition API（`<script setup>`）
- Props 必须定义类型
- 事件命名使用 kebab-case

## API 规范

- 统一响应结构：`Result<T>`（code + message + data）
- 统一错误码：`ResultCode` 枚举
- RESTful 风格：GET（查询）/ POST（创建）/ PUT（更新）/ DELETE（删除）
- 分页参数：`pageNum` + `pageSize`
- 所有接口必须有 SpringDoc 注解
