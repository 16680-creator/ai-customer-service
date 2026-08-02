# Maven 多模块管理

> 本项目是一个典型的 Maven 多模块工程，父 POM 统一管理 10 个子模块的版本和依赖。
> 对应项目文件：根目录 `pom.xml`

---

## 一、什么是多模块工程？

```
ai-customer-service/          ← 父工程（packaging: pom）
├── pom.xml                   ← 父 POM：统一版本、统一依赖管理
├── ai-cs-common/             ← 公共模块（工具类、统一返回体）
├── ai-cs-gateway/            ← 网关服务
├── ai-cs-user/               ← 用户服务
├── ai-cs-chat/               ← AI 对话服务
├── ai-cs-knowledge/          ← 知识库服务
├── ai-cs-order/              ← 订单服务
├── ai-cs-product/            ← 商品服务
├── ai-cs-search/             ← 搜索服务
├── ai-cs-message/            ← 消息服务
└── ai-cs-notify/             ← 通知服务
```

**为什么要拆分模块？**
- 各服务独立部署（微服务）
- 公共代码复用（ai-cs-common）
- 依赖版本统一，避免冲突
- 团队可以并行开发不同服务

---

## 二、父 POM 详解

打开项目根目录的 `pom.xml`，核心结构如下：

### 2.1 基本信息

```xml
<groupId>com.aics</groupId>                    <!-- 组织标识 -->
<artifactId>ai-customer-service</artifactId>   <!-- 项目名 -->
<version>1.0.0-SNAPSHOT</version>              <!-- 版本号 -->
<packaging>pom</packaging>                     <!-- ⚠️ 关键：pom 表示这是父工程，不产出 jar -->
```

### 2.2 声明子模块

```xml
<modules>
    <module>ai-cs-common</module>
    <module>ai-cs-gateway</module>
    <module>ai-cs-user</module>
    <module>ai-cs-knowledge</module>
    <module>ai-cs-chat</module>
    <module>ai-cs-search</module>
    <module>ai-cs-message</module>
    <module>ai-cs-notify</module>
    <module>ai-cs-order</module>
    <module>ai-cs-product</module>
</modules>
```

### 2.3 统一版本属性（properties）

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.5</spring-boot.version>
    <spring-cloud.version>2023.0.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
    <spring-ai.version>1.0.0</spring-ai.version>
    <mybatis-plus.version>3.5.6</mybatis-plus.version>
    <elasticsearch.version>8.12.2</elasticsearch.version>
    <rocketmq-spring.version>2.3.0</rocketmq-spring.version>
    <lombok.version>1.18.30</lombok.version>
    <!-- ... 更多版本 -->
</properties>
```

**好处**：升级版本只需改一处，所有子模块自动生效。

### 2.4 依赖管理（dependencyManagement）

```xml
<dependencyManagement>
    <dependencies>
        <!-- BOM 导入：一次性管理 Spring Boot 所有依赖的版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>  <!-- ⚠️ import 表示导入这个 BOM 的所有版本定义 -->
        </dependency>

        <!-- 自定义依赖版本 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**核心理解**：
- `dependencyManagement` ≠ `dependencies`
- 它只是**声明版本**，子模块不写版本号就能用
- 子模块如果不引用，这个依赖不会被打包进去

---

## 三、子模块 POM 详解

以 `ai-cs-chat/pom.xml` 为例：

```xml
<!-- 继承父 POM -->
<parent>
    <groupId>com.aics</groupId>
    <artifactId>ai-customer-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<artifactId>ai-cs-chat</artifactId>
<packaging>jar</packaging>  <!-- 子模块产出 jar -->

<dependencies>
    <!-- 不需要写版本号！从父 POM 的 dependencyManagement 继承 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 引用内部公共模块 -->
    <dependency>
        <groupId>com.aics</groupId>
        <artifactId>ai-cs-common</artifactId>
    </dependency>

    <!-- Spring AI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

---

## 四、模块依赖关系图

```
                    ai-cs-common（公共模块）
                   /    |    |    \     \
                  /     |    |     \     \
          gateway  user  chat  order  product ...
            |
            └── 所有服务都依赖 common
```

`ai-cs-common` 包含：
- `Result<T>` - 统一返回体
- `ResultCode` - 状态码枚举
- `BusinessException` - 业务异常
- `GlobalExceptionHandler` - 全局异常处理
- `JwtUtil` - JWT 工具类

---

## 五、常用 Maven 命令

### 5.1 构建相关

```bash
# 编译所有模块（在项目根目录执行）
mvn clean compile

# 打包所有模块（跳过测试）
mvn clean package -DskipTests

# 打包并安装到本地仓库（其他项目可以引用）
mvn clean install -DskipTests

# 只构建某个子模块（及其依赖的模块）
mvn clean package -pl ai-cs-chat -am
# -pl: 指定模块  -am: 同时构建它依赖的模块（ai-cs-common）

# 只构建某个模块（不构建依赖）
mvn clean package -pl ai-cs-order
```

### 5.2 依赖分析

```bash
# 查看依赖树（排查冲突）
mvn dependency:tree

# 查看某个模块的依赖树
mvn dependency:tree -pl ai-cs-order

# 分析未使用/未声明的依赖
mvn dependency:analyze
```

### 5.3 运行服务

```bash
# 方式一：Maven 插件直接运行
cd ai-cs-chat
mvn spring-boot:run

# 方式二：先打包再运行
mvn clean package -DskipTests
java -jar ai-cs-chat/target/ai-cs-chat-1.0.0-SNAPSHOT.jar

# 方式三：指定配置文件
java -jar ai-cs-chat.jar --spring.profiles.active=prod
```

---

## 六、BOM（Bill of Materials）是什么？

BOM 是一个特殊的 POM，它只定义版本号，不包含实际代码。

本项目导入了 4 个 BOM：

| BOM | 管理什么 |
|-----|---------|
| spring-boot-dependencies | Spring Boot 全家桶版本 |
| spring-cloud-dependencies | Spring Cloud 组件版本 |
| spring-cloud-alibaba-dependencies | Nacos、Sentinel 等版本 |
| spring-ai-bom | Spring AI 相关版本 |

**为什么需要 BOM？**
- Spring 生态有几百个 jar，版本必须配套
- BOM 帮你测试过哪些版本组合是兼容的
- 你只需要选一个 BOM 版本，其他自动对齐

---

## 七、常见问题

### Q1: 依赖冲突怎么办？

```bash
# 1. 先看依赖树
mvn dependency:tree -pl ai-cs-order | grep "冲突的包名"

# 2. 在父 POM 中排除
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### Q2: SNAPSHOT 和 RELEASE 的区别？

| 类型 | 含义 | 使用场景 |
|------|------|---------|
| 1.0.0-SNAPSHOT | 开发中，随时会变 | 开发阶段（本项目） |
| 1.0.0 | 正式发布，不可变 | 上线部署 |

### Q3: 为什么 Lombok 要在 compiler plugin 中配置？

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
</annotationProcessorPaths>
```

因为 Lombok 和 MapStruct 都是**编译期注解处理器**，需要在编译时生成代码。
如果同时用 Lombok + MapStruct，必须把 Lombok 放前面（先生成 getter，MapStruct 才能用）。

---

## 八、动手练习

1. 在根 POM 的 `<modules>` 中注释掉一个模块，执行 `mvn clean compile`，观察变化
2. 给 `ai-cs-chat` 添加一个新依赖（如 `spring-boot-starter-cache`），不写版本号，看能否编译通过
3. 执行 `mvn dependency:tree -pl ai-cs-order`，画出订单服务的依赖关系

---

## 学习检查清单

- [ ] 理解父 POM 和子 POM 的继承关系
- [ ] 理解 `dependencyManagement` vs `dependencies` 的区别
- [ ] 理解 BOM 的作用
- [ ] 会用 `-pl` 和 `-am` 单独构建模块
- [ ] 会排查依赖冲突
- [ ] 理解 annotationProcessorPaths 的配置原因

---

## 下一步

→ [02-Spring微服务/01-SpringBoot核心原理](../02-Spring微服务/01-SpringBoot核心原理.md)
