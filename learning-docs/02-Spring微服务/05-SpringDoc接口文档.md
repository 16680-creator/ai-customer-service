# SpringDoc OpenAPI 接口文档

> 本项目使用 **SpringDoc OpenAPI** 自动生成 REST 接口文档，前端可据此联调，接口调试无需手写文档。
> 对应项目文件：各服务 `pom.xml`、`ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java`

---

## 一、什么是 OpenAPI？

```
传统方式：接口写好了，再手动维护一份文档（Word/Markdown）
   问题：接口一改，文档就过期

SpringDoc：基于注解 + 反射，运行时自动生成接口文档
   好处：文档永远和代码同步
```

OpenAPI 是一个描述 HTTP API 的标准格式（JSON/YAML）。SpringDoc 把 Spring MVC 的接口自动转换成 OpenAPI 格式，并提供 Swagger UI 可视化界面。

```
你的 Controller ──反射──▶ OpenAPI 描述(JSON) ──渲染──▶ Swagger UI 网页
```

---

## 二、引入依赖

```xml
<!-- 各服务 pom.xml 都引入了 SpringDoc -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

> 注意：如果是 WebFlux 网关（Gateway），应使用 `springdoc-openapi-starter-webflux-ui`。

---

## 三、注解使用（本项目实际代码）

```java
// ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// @Tag：给整个控制器分组命名
@Tag(name = "AI对话")
@RestController
@RequestMapping("/chat")
public class ChatController {

    // @Operation：描述单个接口
    @Operation(summary = "发送对话消息")
    @PostMapping("/send")
    public Result<String> chat(@RequestParam String sessionId,
                               @RequestParam String message) {
        return chatService.chat(sessionId, message);
    }

    @Operation(summary = "流式对话")
    @PostMapping("/stream")
    public Result<Map<String, Object>> chatStream(@RequestParam String sessionId,
                                                   @RequestParam String message) {
        return chatService.chatStream(sessionId, message);
    }
}
```

### 常用注解一览

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Tag` | 给 Controller 分组 | `@Tag(name = "AI对话")` |
| `@Operation` | 描述接口用途 | `@Operation(summary = "发送对话消息")` |
| `@Parameter` | 描述参数 | `@Parameter(description = "会话ID")` |
| `@Schema` | 描述 DTO 字段 | `@Schema(description = "订单号")` |
| `@ApiResponse` | 描述响应码 | `@ApiResponse(responseCode = "401", description = "未认证")` |

### DTO 字段描述

```java
public class OrderCreateDTO {

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "商品列表")
    private List<CartUpdateDTO> items;
}
```

---

## 四、访问接口文档

启动任意服务后，访问：

| 地址 | 说明 |
|------|------|
| `http://localhost:8083/swagger-ui.html` | Swagger UI 可视化界面 |
| `http://localhost:8083/v3/api-docs` | OpenAPI JSON 原始数据 |

在 Swagger UI 上可以：
1. 查看所有接口的分组、路径、参数、响应结构
2. **直接在线调试**（点击 Try it out → 填参数 → Execute）
3. 拷贝每个接口的 curl 命令

---

## 五、在网关访问各服务的文档

由于所有请求走网关（8080），可以直接通过网关路由访问各服务的文档：

```
# 网关路径需要 stripPrefix(1)，比如：
# /api/chat/v3/api-docs → 转发到 ai-cs-chat 的 /v3/api-docs
http://localhost:8080/api/chat/v3/api-docs
```

注意：接口文档路径已加入网关鉴权白名单（见 AuthFilter 的 WHITE_LIST）：
```java
private static final List<String> WHITE_LIST = List.of(
        "/doc.html",
        "/webjars/",
        "/v3/api-docs",
        "/swagger-resources"
);
```

---

## 六、自定义文档信息

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI客服系统 API")
                .version("1.0.0")
                .description("AI 智能客服平台微服务接口文档")
                .contact(new Contact().name("aics").email("aics@example.com")));
    }
}
```

---

## 七、动手练习

1. 启动 `ai-cs-chat`，访问 `http://localhost:8083/swagger-ui.html`，查看 AI 对话接口
2. 在 Swagger UI 上调试 `/chat/send` 接口
3. 给自己写的新接口加上 `@Operation` 和 `@Tag` 注解，刷新查看
4. 给 DTO 字段加 `@Schema` 描述

---

## 学习检查清单

- [ ] 理解 OpenAPI / SpringDoc / Swagger UI 三者关系
- [ ] 会引入 SpringDoc 依赖
- [ ] 会用 `@Tag` / `@Operation` / `@Schema` 注解
- [ ] 会访问 `swagger-ui.html` 和 `v3/api-docs`
- [ ] 会在 Swagger UI 上在线调试接口
- [ ] 理解网关鉴权白名单与文档的关系

---

## 下一步

→ [03-数据库与ORM/01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md)