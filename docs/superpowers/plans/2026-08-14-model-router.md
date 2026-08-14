# 多模型路由与模型降级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ai-cs-chat` 中实现 Nacos 驱动的多模型注册表、场景路由、同能力故障降级、配额降档和路由可观测性。

**Architecture:** 新增 `com.aics.chat.modelrouter` 包，提供 `ModelRouterProperties`、`ChatModelRegistry`、`ModelHealthRegistry`、`ModelRouter`、`RoutedChatClientFactory`。`ResilientAiService` 改为场景感知调用，按候选链逐个调用模型，每个模型使用独立 CircuitBreaker；流式在订阅前按健康状态选模型。Nacos 配置通过 `@RefreshScope` 热更新。

**Tech Stack:** Java 17、Spring Boot 3.2、Spring AI 1.1.4（OpenAI 兼容协议）、Resilience4j、Nacos Config、JUnit 5 + Mockito。

**Spec:** [docs/superpowers/specs/2026-08-14-model-router-design.md](../../docs/superpowers/specs/2026-08-14-model-router-design.md)

## Global Constraints

- 工作目录必须是 git worktree `D:\Projects\Persion\ai-customer-service\.worktrees\006-model-router`。
- Java 版本要求 JDK 17+；构建命令统一用 `mvn -pl ai-cs-chat -am ...`。
- 测试必须在实现代码之前或同一次提交中提交（TDD，宪法第 2-1 条）。
- 不得新增微服务依赖；所有改动集中在 `ai-cs-chat` 和 `ai-cs-message` 不涉及（本计划不修改 message 侧表结构）。
- 模型 API Key 只放在 Nacos/环境变量，不硬编码到 Java 代码。
- 默认场景路由：`intent/summary/rewrite/chart` 走 `siliconflow-qwen3-8b`；`chat/rag/agent/nl2sql/judge` 走 `deepseek-chat`；标准场景备选 `siliconflow-qwen3-32b`。
- 流式调用不重试、不中途切换；首 token 前按健康状态选模型。
- 每个模型独立 CircuitBreaker；禁止用单个 `chatService` 熔断器包住整个候选链。

---

### Task 1: 模型路由领域模型与配置校验

**Files:**
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelScenario.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelCapability.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelDefinition.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ScenarioRoute.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/QuotaRouteProperties.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelRouterProperties.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/RouteRequest.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/RouteDecision.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/RouteReason.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ModelRouterPropertiesTest.java`

**Interfaces:**
- Produces: `ModelScenario` enum, `ModelCapability` enum, `RouteRequest.builder()`, `RouteDecision.builder()`, `ModelRouterProperties.validate()`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelRouterPropertiesTest {

    @Test
    void validate_rejectsDuplicateModelId() {
        ModelRouterProperties props = new ModelRouterProperties();
        ModelDefinition a = model("m1", "deepseek");
        ModelDefinition b = model("m1", "siliconflow");
        props.setModels(List.of(a, b));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("duplicate model id: m1", ex.getMessage());
    }

    @Test
    void validate_rejectsScenarioWithUnknownPrimary() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("missing");
        route.setFallbacks(List.of());
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("unknown primary model for scenario CHAT: missing", ex.getMessage());
    }

    @Test
    void validate_rejectsScenarioWithUnknownFallback() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("m1");
        route.setFallbacks(List.of("missing"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, props::validate);
        assertEquals("unknown fallback model for scenario CHAT: missing", ex.getMessage());
    }

    @Test
    void validate_acceptsValidConfig() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek"), model("m2", "siliconflow")));
        ScenarioRoute route = new ScenarioRoute();
        route.setPrimary("m1");
        route.setFallbacks(List.of("m2"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, route)));
        props.validate();
        assertEquals(2, props.getModels().size());
    }

    private static ModelDefinition model(String id, String provider) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier("standard");
        return def;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelRouterPropertiesTest -q`
Expected: FAIL because `ModelRouterProperties` and related classes do not exist.

- [ ] **Step 3: Write minimal implementation**

`ModelScenario.java`:

```java
package com.aics.chat.modelrouter;

public enum ModelScenario {
    CHAT, RAG, SUMMARY, INTENT, AGENT, NL2SQL, REWRITE, CHART, JUDGE
}
```

`ModelCapability.java`:

```java
package com.aics.chat.modelrouter;

public enum ModelCapability {
    TOOL_CALLING, VISION
}
```

`ModelDefinition.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ModelDefinition {
    private String id;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private boolean enabled = true;
    private int priority = 0;
    private String tier;
    private Set<ModelCapability> capabilities = new HashSet<>();
    private int contextWindow = 32768;
    private long timeoutMs = 30000;
}
```

`ScenarioRoute.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ScenarioRoute {
    private String primary;
    private List<String> fallbacks = new ArrayList<>();
}
```

`QuotaRouteProperties.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotaRouteProperties {
    private boolean enabled = true;
    private String overLimitFallbackTier = "cheap";
}
```

`ModelRouterProperties.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "aics.model-router")
public class ModelRouterProperties {

    private boolean enabled = true;
    private List<ModelDefinition> models = new ArrayList<>();
    private Map<ModelScenario, ScenarioRoute> scenarios = new EnumMap<>(ModelScenario.class);
    private QuotaRouteProperties quota = new QuotaRouteProperties();

    public void validate() {
        Set<String> ids = new HashSet<>();
        for (ModelDefinition def : models) {
            if (!StringUtils.hasText(def.getId())) {
                throw new IllegalArgumentException("model id must not be blank");
            }
            if (!ids.add(def.getId())) {
                throw new IllegalArgumentException("duplicate model id: " + def.getId());
            }
            if (!StringUtils.hasText(def.getBaseUrl())
                    || !StringUtils.hasText(def.getApiKey())
                    || !StringUtils.hasText(def.getModel())) {
                throw new IllegalArgumentException("model " + def.getId() + " must configure base-url, api-key and model");
            }
        }
        for (Map.Entry<ModelScenario, ScenarioRoute> entry : scenarios.entrySet()) {
            ModelScenario scenario = entry.getKey();
            ScenarioRoute route = entry.getValue();
            if (route == null || !StringUtils.hasText(route.getPrimary())) {
                throw new IllegalArgumentException("scenario " + scenario + " must configure primary");
            }
            if (!ids.contains(route.getPrimary())) {
                throw new IllegalArgumentException("unknown primary model for scenario " + scenario + ": " + route.getPrimary());
            }
            for (String fallback : route.getFallbacks()) {
                if (!ids.contains(fallback)) {
                    throw new IllegalArgumentException("unknown fallback model for scenario " + scenario + ": " + fallback);
                }
            }
        }
    }
}
```

`RouteRequest.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RouteRequest {
    private final ModelScenario scenario;
    private final boolean quotaExceeded;
    private final Set<ModelCapability> requiredCapabilities;
}
```

`RouteReason.java`:

```java
package com.aics.chat.modelrouter;

public enum RouteReason {
    SCENARIO_DEFAULT,
    PRIMARY_UNAVAILABLE,
    QUOTA_DOWNGRADE,
    QUOTA_NO_CHEAPER_MODEL,
    NO_ELIGIBLE_MODEL
}
```

`RouteDecision.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteDecision {
    private final String selectedModelId;
    private final List<String> fallbackChain;
    private final RouteReason reason;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelRouterPropertiesTest -q`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/modelrouter ai-cs-chat/src/test/java/com/aics/chat/modelrouter
git commit -m "feat(model-router): add routing domain model and config validation"
```

---

### Task 2: ChatModelRegistry 与默认 ChatClient 装配

**Files:**
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelClientHolder.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatClientCustomizer.java`
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ChatModelRegistryTest.java`

**Interfaces:**
- Consumes: `ModelRouterProperties`, `ModelDefinition`, `ModelCapability`, `ToolCallbackProvider`, `QuestionAnswerAdvisor`.
- Produces: `ModelClientHolder(definition, chatModel, chatClient)`, `ChatModelRegistry.get(String)`, `ChatModelRegistry.contains(String)`, `ChatClientCustomizer.customize(ChatClient.Builder)`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChatModelRegistryTest {

    @Test
    void rebuild_registersEnabledModelsOnly() {
        ModelRouterProperties props = new ModelRouterProperties();
        ModelDefinition enabled = model("deepseek-chat", "deepseek", true);
        ModelDefinition disabled = model("siliconflow-qwen3-32b", "siliconflow", false);
        props.setModels(List.of(enabled, disabled));

        ChatClientCustomizer customizer = builder -> builder.defaultSystem("test system");
        ChatModelRegistry registry = new ChatModelRegistry(
                props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();

        assertTrue(registry.contains("deepseek-chat"));
        assertFalse(registry.contains("siliconflow-qwen3-32b"));
        ModelClientHolder holder = registry.get("deepseek-chat");
        assertNotNull(holder.getChatModel());
        assertNotNull(holder.getChatClient());
        assertNotNull(holder.getDefinition());
    }

    @Test
    void rebuild_appliesCustomizer() {
        ModelRouterProperties props = new ModelRouterProperties();
        props.setModels(List.of(model("m1", "deepseek", true)));
        ChatClientCustomizer customizer = builder -> builder.defaultSystem("custom system");
        ChatModelRegistry registry = new ChatModelRegistry(
                props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();

        ChatClient client = registry.get("m1").getChatClient();
        assertNotNull(client);
    }

    private static ModelDefinition model(String id, String provider, boolean enabled) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier("standard");
        def.setEnabled(enabled);
        return def;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ChatModelRegistryTest -q`
Expected: FAIL because `ChatModelRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

`ModelClientHolder.java`:

```java
package com.aics.chat.modelrouter;

import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;

@Getter
public class ModelClientHolder {
    private final ModelDefinition definition;
    private final OpenAiChatModel chatModel;
    private final ChatClient chatClient;

    public ModelClientHolder(ModelDefinition definition, OpenAiChatModel chatModel, ChatClient chatClient) {
        this.definition = definition;
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }
}
```

`ChatClientCustomizer.java`:

```java
package com.aics.chat.modelrouter;

import org.springframework.ai.chat.client.ChatClient;

@FunctionalInterface
public interface ChatClientCustomizer {
    ChatClient.Builder customize(ChatClient.Builder builder);
}
```

`ChatModelRegistry.java`:

```java
package com.aics.chat.modelrouter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RefreshScope
@RequiredArgsConstructor
public class ChatModelRegistry {

    private final ModelRouterProperties properties;
    private final ChatClientCustomizer chatClientCustomizer;
    private final ToolCallbackProvider toolCallbackProvider;
    private final QuestionAnswerAdvisor ragAdvisor;

    private volatile Map<String, ModelClientHolder> clients = Map.of();

    @PostConstruct
    void init() {
        rebuild();
    }

    public synchronized void rebuild() {
        properties.validate();
        Map<String, ModelClientHolder> next = new LinkedHashMap<>();
        for (ModelDefinition definition : properties.getModels()) {
            if (!definition.isEnabled()) {
                continue;
            }
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(definition.getBaseUrl())
                    .apiKey(definition.getApiKey())
                    .build();
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(definition.getModel()).build())
                    .build();
            ChatClient.Builder builder = chatClientCustomizer.customize(ChatClient.builder(chatModel));
            if (definition.getCapabilities().contains(ModelCapability.TOOL_CALLING)) {
                builder = builder.defaultToolCallbacks(toolCallbackProvider);
            }
            next.put(definition.getId(),
                    new ModelClientHolder(definition, chatModel, builder.build()));
        }
        this.clients = Map.copyOf(next);
    }

    public boolean contains(String modelId) {
        return clients.containsKey(modelId);
    }

    public ModelClientHolder get(String modelId) {
        ModelClientHolder holder = clients.get(modelId);
        if (holder == null) {
            throw new IllegalStateException("model not registered: " + modelId);
        }
        return holder;
    }
}
```

`SpringAiConfig.java` modifications:

- Remove the `chatClient(OpenAiChatModel, ToolCallbackProvider, QuestionAnswerAdvisor)` bean method.
- Keep `ragAdvisor(VectorStore)` and `toolCallbackProvider(...)`.
- Add this bean method:

```java
@Bean
public ChatClientCustomizer chatClientCustomizer(ToolCallbackProvider toolCallbackProvider,
                                                 QuestionAnswerAdvisor ragAdvisor) {
    return builder -> builder
            .defaultSystem("""
                    你是AI客服平台的智能助手，代表平台为用户提供专业、友好的服务。

                    重要规则：
                    1. 绝对不要透露你使用的底层模型名称、版本号或技术提供商信息
                    2. 如果用户询问你是什么模型、用什么技术构建，请回答：“我是AI客服平台的智能助手，专注于为您提供优质的服务体验”
                    3. 不要提及MiniMax、GPT、Claude、LLM等任何具体模型或技术名称
                    4. 保持专业形象，始终以帮助用户解决问题为首要目标

                    能力范围：
                    - 订单查询：当用户想查询订单信息时，使用 queryOrderByOrderId 或 queryOrdersByUserId 工具查询订单数据，并用清晰、结构化的方式呈现给用户
                    - 当前登录用户已由系统自动识别，无需向用户索要用户ID；用户询问“我的订单/订单列表”时直接调用 queryOrdersByUserId 查询
                      - 查询时可以通过订单号精确查询，也可以通过当前用户ID查询名下所有订单
                    - 查询结果要包含订单状态、商品信息、金额、物流等关键信息，用简洁易懂的格式展示
                    - 数据查询（智能问数）：当用户想了解平台数据（如订单统计、商品销量、用户数量、优惠券使用情况等），
                      使用 executeReadOnlyQuery 工具查询数据库。规则：
                      * 必须根据问题涉及的业务先选对 database（user=用户库、product=商品库、order=订单支付库、chat=对话消息库、knowledge=知识库）
                      * 组装合法 SELECT 语句，可带 WHERE / ORDER BY / GROUP BY / 聚合函数（COUNT/SUM/AVG）
                      * 日期字段用 create_time / pay_time 等，订单金额字段用 pay_amount，订单状态用 status 过滤
                      * 查询出数据后用自然语言向用户汇报结论，可附带表格或关键数字
                      * 若多次尝试仍失败（SQL 语法错误/表列名不存在），如实告知用户"暂时无法查询该数据"，不要编造数字

                    回答风格：简洁、准确、有亲和力，适当使用emoji增加友好感。

                    """ + DB_SCHEMA)
            .defaultAdvisors(ragAdvisor);
}
```

Add imports: `com.aics.chat.modelrouter.ChatClientCustomizer`. Remove unused `ChatClient` import if no longer referenced by other code in the file.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ChatModelRegistryTest -q`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/modelrouter ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java ai-cs-chat/src/test/java/com/aics/chat/modelrouter
git commit -m "feat(model-router): add ChatModelRegistry and default client wiring"
```

---

### Task 3: 按模型隔离的 ModelHealthRegistry

**Files:**
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelHealthRegistry.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ModelHealthRegistryTest.java`

**Interfaces:**
- Consumes: Resilience4j `CircuitBreakerRegistry`.
- Produces: `CircuitBreaker breaker(String modelId)`, `boolean isAvailable(String modelId)`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.modelrouter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelHealthRegistryTest {

    private final ModelHealthRegistry registry = new ModelHealthRegistry();

    @Test
    void breaker_isIsolatedPerModelId() {
        CircuitBreaker a = registry.breaker("deepseek-chat");
        CircuitBreaker b = registry.breaker("siliconflow-qwen3-32b");
        assertNotSame(a, b);
        assertSame(a, registry.breaker("deepseek-chat"));
    }

    @Test
    void isAvailable_returnsFalseWhenBreakerOpen() {
        assertTrue(registry.isAvailable("deepseek-chat"));
        registry.breaker("deepseek-chat").transitionToOpenState();
        assertFalse(registry.isAvailable("deepseek-chat"));
    }

    @Test
    void isAvailable_returnsTrueWhenHalfOpen() {
        CircuitBreaker breaker = registry.breaker("deepseek-chat");
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        assertTrue(registry.isAvailable("deepseek-chat"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelHealthRegistryTest -q`
Expected: FAIL because `ModelHealthRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.aics.chat.modelrouter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelHealthRegistry {

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker breaker(String modelId) {
        return breakers.computeIfAbsent(modelId, id -> circuitBreakerRegistry.circuitBreaker(id, config()));
    }

    public boolean isAvailable(String modelId) {
        CircuitBreaker breaker = breakers.get(modelId);
        if (breaker == null) {
            return true;
        }
        return breaker.getState() == CircuitBreaker.State.CLOSED
                || breaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

    private CircuitBreakerConfig config() {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelHealthRegistryTest -q`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelHealthRegistry.java ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ModelHealthRegistryTest.java
git commit -m "feat(model-router): add per-model health registry"
```

---

### Task 4: ModelRouter 确定性路由

**Files:**
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelRouter.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ModelRouterTest.java`

**Interfaces:**
- Consumes: `ModelRouterProperties`, `ChatModelRegistry`, `ModelHealthRegistry`, `RouteRequest`, `RouteDecision`, `RouteReason`.
- Produces: `RouteDecision route(RouteRequest request)`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.modelrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class ModelRouterTest {

    private ModelRouterProperties props;
    private ChatModelRegistry registry;
    private ModelHealthRegistry health;
    private ModelRouter router;

    @BeforeEach
    void setUp() {
        props = new ModelRouterProperties();
        ModelDefinition primary = model("deepseek-chat", "deepseek", "standard", Set.of(ModelCapability.TOOL_CALLING), 100);
        ModelDefinition fallback = model("siliconflow-qwen3-32b", "siliconflow", "standard", Set.of(ModelCapability.TOOL_CALLING), 90);
        ModelDefinition cheap = model("siliconflow-qwen3-8b", "siliconflow", "cheap", Set.of(), 80);
        props.setModels(List.of(primary, fallback, cheap));

        ScenarioRoute chat = new ScenarioRoute();
        chat.setPrimary("deepseek-chat");
        chat.setFallbacks(List.of("siliconflow-qwen3-32b"));
        props.setScenarios(new EnumMap<>(Map.of(ModelScenario.CHAT, chat)));

        ChatClientCustomizer customizer = builder -> builder.defaultSystem("test");
        registry = new ChatModelRegistry(props, customizer, mock(ToolCallbackProvider.class), mock(QuestionAnswerAdvisor.class));
        registry.init();
        health = new ModelHealthRegistry();
        router = new ModelRouter(props, registry, health);
    }

    @Test
    void route_selectsHealthyPrimary() {
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertEquals("deepseek-chat", decision.getSelectedModelId());
        assertEquals(List.of("siliconflow-qwen3-32b"), decision.getFallbackChain());
        assertEquals(RouteReason.SCENARIO_DEFAULT, decision.getReason());
    }

    @Test
    void route_fallsBackWhenPrimaryCircuitOpen() {
        health.breaker("deepseek-chat").transitionToOpenState();
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertEquals("siliconflow-qwen3-32b", decision.getSelectedModelId());
        assertEquals(RouteReason.PRIMARY_UNAVAILABLE, decision.getReason());
    }

    @Test
    void route_quotaDowngradesToCheapTier() {
        props.getQuota().setEnabled(true);
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(true)
                .requiredCapabilities(Set.of())
                .build());
        assertEquals("siliconflow-qwen3-8b", decision.getSelectedModelId());
        assertEquals(RouteReason.QUOTA_DOWNGRADE, decision.getReason());
    }

    @Test
    void route_returnsNoEligibleWhenAllUnavailable() {
        health.breaker("deepseek-chat").transitionToOpenState();
        health.breaker("siliconflow-qwen3-32b").transitionToOpenState();
        RouteDecision decision = router.route(RouteRequest.builder()
                .scenario(ModelScenario.CHAT)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of(ModelCapability.TOOL_CALLING))
                .build());
        assertNull(decision.getSelectedModelId());
        assertEquals(RouteReason.NO_ELIGIBLE_MODEL, decision.getReason());
    }

    private static ModelDefinition model(String id, String provider, String tier,
                                         Set<ModelCapability> capabilities, int priority) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider(provider);
        def.setBaseUrl("https://example.test");
        def.setApiKey("test-key");
        def.setModel(id);
        def.setTier(tier);
        def.setCapabilities(capabilities);
        def.setPriority(priority);
        return def;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelRouterTest -q`
Expected: FAIL because `ModelRouter` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.aics.chat.modelrouter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ModelRouter {

    private final ModelRouterProperties properties;
    private final ChatModelRegistry registry;
    private final ModelHealthRegistry healthRegistry;

    public RouteDecision route(RouteRequest request) {
        ScenarioRoute scenarioRoute = properties.getScenarios().get(request.getScenario());
        if (scenarioRoute == null || !StringUtils.hasText(scenarioRoute.getPrimary())) {
            return noEligible();
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(scenarioRoute.getPrimary());
        if (scenarioRoute.getFallbacks() != null) {
            candidates.addAll(scenarioRoute.getFallbacks());
        }
        Set<ModelCapability> required = request.getRequiredCapabilities() == null
                ? Set.of() : request.getRequiredCapabilities();
        candidates.removeIf(id -> !registry.contains(id)
                || !healthRegistry.isAvailable(id)
                || !hasCapabilities(id, required));

        if (candidates.isEmpty()) {
            return noEligible();
        }

        if (request.isQuotaExceeded() && properties.getQuota().isEnabled()) {
            String cheap = cheapestEligible(candidates);
            if (cheap != null) {
                return decision(cheap, rest(candidates, cheap), RouteReason.QUOTA_DOWNGRADE);
            }
            return decision(candidates.get(0), rest(candidates, candidates.get(0)),
                    RouteReason.QUOTA_NO_CHEAPER_MODEL);
        }

        RouteReason reason = candidates.get(0).equals(scenarioRoute.getPrimary())
                ? RouteReason.SCENARIO_DEFAULT : RouteReason.PRIMARY_UNAVAILABLE;
        return decision(candidates.get(0), rest(candidates, candidates.get(0)), reason);
    }

    private boolean hasCapabilities(String modelId, Set<ModelCapability> required) {
        Set<ModelCapability> actual = registry.get(modelId).getDefinition().getCapabilities();
        return actual != null && actual.containsAll(required);
    }

    private String cheapestEligible(List<String> candidates) {
        String targetTier = properties.getQuota().getOverLimitFallbackTier();
        return candidates.stream()
                .filter(id -> targetTier.equals(registry.get(id).getDefinition().getTier()))
                .sorted(Comparator.comparingInt(id -> registry.get(id).getDefinition().getPriority()).reversed())
                .findFirst()
                .orElse(null);
    }

    private List<String> rest(List<String> candidates, String selected) {
        return candidates.stream().filter(id -> !id.equals(selected)).toList();
    }

    private RouteDecision decision(String selected, List<String> fallbackChain, RouteReason reason) {
        return RouteDecision.builder()
                .selectedModelId(selected)
                .fallbackChain(fallbackChain)
                .reason(reason)
                .build();
    }

    private RouteDecision noEligible() {
        return RouteDecision.builder()
                .selectedModelId(null)
                .fallbackChain(List.of())
                .reason(RouteReason.NO_ELIGIBLE_MODEL)
                .build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ModelRouterTest -q`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ModelRouter.java ai-cs-chat/src/test/java/com/aics/chat/modelrouter/ModelRouterTest.java
git commit -m "feat(model-router): add deterministic scenario router"
```

---

### Task 5: RoutedChatClientFactory 与直接 ChatClient 调用迁移

**Files:**
- Create: `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/RoutedChatClientFactory.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/rag/rewrite/QueryRewriteService.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/nl2sql/chart/ChartAnswerGenerator.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/rag/eval/LlmJudgeService.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/modelrouter/RoutedChatClientFactoryTest.java`
- Modify: `ai-cs-chat/src/test/java/com/aics/chat/rag/rewrite/QueryRewriteServiceTest.java`
- Modify: `ai-cs-chat/src/test/java/com/aics/chat/nl2sql/chart/ChartAnswerGeneratorTest.java`

**Interfaces:**
- Consumes: `ModelRouter`, `ChatModelRegistry`, `ChatClient`.
- Produces: `ChatClient chatClientFor(ModelScenario scenario)`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.modelrouter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutedChatClientFactoryTest {

    @Test
    void chatClientFor_returnsSelectedModelClient() {
        ModelRouter router = mock(ModelRouter.class);
        ChatModelRegistry registry = mock(ChatModelRegistry.class);
        ModelClientHolder holder = mock(ModelClientHolder.class);
        ChatClient client = mock(ChatClient.class);
        when(holder.getChatClient()).thenReturn(client);
        when(registry.get("deepseek-chat")).thenReturn(holder);
        when(router.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId("deepseek-chat")
                        .fallbackChain(List.of())
                        .reason(RouteReason.SCENARIO_DEFAULT)
                        .build());

        RoutedChatClientFactory factory = new RoutedChatClientFactory(router, registry);
        assertSame(client, factory.chatClientFor(ModelScenario.REWRITE));
    }

    @Test
    void chatClientFor_throwsWhenNoEligibleModel() {
        ModelRouter router = mock(ModelRouter.class);
        ChatModelRegistry registry = mock(ChatModelRegistry.class);
        when(router.route(org.mockito.ArgumentMatchers.any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId(null)
                        .fallbackChain(List.of())
                        .reason(RouteReason.NO_ELIGIBLE_MODEL)
                        .build());

        RoutedChatClientFactory factory = new RoutedChatClientFactory(router, registry);
        assertThrows(IllegalStateException.class, () -> factory.chatClientFor(ModelScenario.REWRITE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=RoutedChatClientFactoryTest -q`
Expected: FAIL because `RoutedChatClientFactory` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.aics.chat.modelrouter;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoutedChatClientFactory {

    private final ModelRouter modelRouter;
    private final ChatModelRegistry modelRegistry;

    public ChatClient chatClientFor(ModelScenario scenario) {
        RouteDecision decision = modelRouter.route(RouteRequest.builder()
                .scenario(scenario)
                .quotaExceeded(false)
                .requiredCapabilities(Set.of())
                .build());
        if (decision.getSelectedModelId() == null) {
            throw new IllegalStateException("no eligible model for scenario: " + scenario);
        }
        return modelRegistry.get(decision.getSelectedModelId()).getChatClient();
    }
}
```

`QueryRewriteService.java` changes:

- Replace `private final ChatClient chatClient;` with `private final RoutedChatClientFactory routedChatClientFactory;`
- Replace the LLM call block with:

```java
String content = routedChatClientFactory.chatClientFor(ModelScenario.REWRITE)
        .prompt()
        .system("你是检索查询优化专家，只输出指定 JSON。")
        .user(prompt)
        .call()
        .content();
```

`ChartAnswerGenerator.java` changes:

- Replace `private final ChatClient chatClient;` with `private final RoutedChatClientFactory routedChatClientFactory;`
- Replace the LLM call block with:

```java
String content = routedChatClientFactory.chatClientFor(ModelScenario.CHART)
        .prompt()
        .system("你是数据分析师，只输出结论。")
        .user(prompt)
        .call()
        .content();
```

`LlmJudgeService.java` changes:

- Replace `private final ChatClient chatClient;` with `private final RoutedChatClientFactory routedChatClientFactory;`
- Replace the LLM call block with:

```java
org.springframework.ai.chat.model.ChatResponse response = routedChatClientFactory.chatClientFor(ModelScenario.JUDGE)
        .prompt()
        .system("你是严谨的 RAG 质量评估员，只输出 1-5 的整数分数。")
        .user(prompt)
        .call()
        .chatResponse();
```

Update existing tests to mock `RoutedChatClientFactory` and return the existing mock `ChatClient`:

`QueryRewriteServiceTest.java`:

```java
private RoutedChatClientFactory routedChatClientFactory;
private ChatClient chatClient;

@BeforeEach
void setUp() {
    chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    routedChatClientFactory = mock(RoutedChatClientFactory.class);
    when(routedChatClientFactory.chatClientFor(ModelScenario.REWRITE)).thenReturn(chatClient);
    service = new QueryRewriteService(routedChatClientFactory, new ObjectMapper());
}
```

`ChartAnswerGeneratorTest.java`:

```java
private RoutedChatClientFactory routedChatClientFactory;
private ChatClient chatClient;

@BeforeEach
void setUp() {
    chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    routedChatClientFactory = mock(RoutedChatClientFactory.class);
    when(routedChatClientFactory.chatClientFor(ModelScenario.CHART)).thenReturn(chatClient);
    generator = new ChartAnswerGenerator(routedChatClientFactory);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl ai-cs-chat -am test -Dtest=RoutedChatClientFactoryTest,QueryRewriteServiceTest,ChartAnswerGeneratorTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/modelrouter/RoutedChatClientFactory.java ai-cs-chat/src/main/java/com/aics/chat/rag/rewrite/QueryRewriteService.java ai-cs-chat/src/main/java/com/aics/chat/nl2sql/chart/ChartAnswerGenerator.java ai-cs-chat/src/main/java/com/aics/chat/rag/eval/LlmJudgeService.java ai-cs-chat/src/test/java/com/aics/chat/modelrouter ai-cs-chat/src/test/java/com/aics/chat/rag/rewrite ai-cs-chat/src/test/java/com/aics/chat/nl2sql/chart
git commit -m "feat(model-router): route direct ChatClient consumers by scenario"
```

---

### Task 6: ResilientAiService 场景感知路由与降级

**Files:**
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ChatServiceImpl.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/agent/intent/IntentClassifierService.java`
- Test: `ai-cs-chat/src/test/java/com/aics/chat/service/impl/ResilientAiServiceRoutingTest.java`
- Modify: `ai-cs-chat/src/test/java/com/aics/chat/agent/intent/IntentClassifierServiceTest.java`

**Interfaces:**
- Consumes: `ModelRouter`, `ChatModelRegistry`, `ModelHealthRegistry`, `QuotaService`, `ModelRouterProperties`, `ObservationRegistry`, `ModelUsageRecorder`, `ChatUserContext`.
- Produces: `callChat(ModelScenario, List<Message>)`, `callRagChat(ModelScenario, String)`, `callSummary(ModelScenario, Prompt)`, `callSseStream(ModelScenario, List<Message>)`, `callSseRagStream(ModelScenario, String)`.

- [ ] **Step 1: Write the failing test**

```java
package com.aics.chat.service.impl;

import com.aics.chat.dto.QuotaCheckResult;
import com.aics.chat.modelrouter.*;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.ObservabilityProperties;
import com.aics.chat.observability.QuotaService;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientAiServiceRoutingTest {

    private ModelRouter modelRouter;
    private ChatModelRegistry registry;
    private ModelHealthRegistry health;
    private QuotaService quotaService;
    private ModelUsageRecorder usageRecorder;
    private ResilientAiService service;

    @BeforeEach
    void setUp() {
        modelRouter = mock(ModelRouter.class);
        registry = mock(ChatModelRegistry.class);
        health = new ModelHealthRegistry();
        quotaService = mock(QuotaService.class);
        usageRecorder = mock(ModelUsageRecorder.class);
        ModelRouterProperties props = new ModelRouterProperties();
        props.getQuota().setEnabled(false);
        service = new ResilientAiService(modelRouter, registry, health, quotaService, props,
                ObservationRegistry.create(), usageRecorder);
    }

    @Test
    void callChat_returnsFallbackWhenNoEligibleModel() throws Exception {
        when(modelRouter.route(any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId(null)
                        .fallbackChain(List.of())
                        .reason(RouteReason.NO_ELIGIBLE_MODEL)
                        .build());
        String result = service.callChat(ModelScenario.CHAT, List.of()).get();
        assertTrue(result.contains("繁忙"));
    }

    @Test
    void callChat_switchesToFallbackModel() throws Exception {
        ModelClientHolder primary = holder("deepseek-chat", mock(ChatClient.class));
        ModelClientHolder fallback = holder("siliconflow-qwen3-32b", mock(ChatClient.class));
        when(registry.get("deepseek-chat")).thenReturn(primary);
        when(registry.get("siliconflow-qwen3-32b")).thenReturn(fallback);
        when(modelRouter.route(any(RouteRequest.class)))
                .thenReturn(RouteDecision.builder()
                        .selectedModelId("deepseek-chat")
                        .fallbackChain(List.of("siliconflow-qwen3-32b"))
                        .reason(RouteReason.PRIMARY_UNAVAILABLE)
                        .build());
        when(primary.getChatClient().prompt().messages(any()).call().chatResponse())
                .thenThrow(new RuntimeException("primary down"));
        ChatResponse response = chatResponse("backup answer");
        when(fallback.getChatClient().prompt().messages(any()).call().chatResponse()).thenReturn(response);

        String result = service.callChat(ModelScenario.CHAT, List.of()).get();
        assertEquals("backup answer", result);
    }

    private static ModelClientHolder holder(String id, ChatClient client) {
        ModelDefinition def = new ModelDefinition();
        def.setId(id);
        def.setProvider("test");
        def.setBaseUrl("https://example.test");
        def.setApiKey("test");
        def.setModel(id);
        def.setTimeoutMs(1000);
        return new ModelClientHolder(def, null, client);
    }

    private static ChatResponse chatResponse(String text) {
        Generation generation = new Generation(new AssistantMessage(text));
        return ChatResponse.builder().generations(List.of(generation)).build();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ResilientAiServiceRoutingTest -q`
Expected: FAIL because the new method signatures do not exist.

- [ ] **Step 3: Replace ResilientAiService with routed implementation**

Replace the file content with the implementation below (keep package and existing fallback messages):

```java
package com.aics.chat.service.impl;

import com.aics.chat.modelrouter.ChatModelRegistry;
import com.aics.chat.modelrouter.ModelCapability;
import com.aics.chat.modelrouter.ModelClientHolder;
import com.aics.chat.modelrouter.ModelHealthRegistry;
import com.aics.chat.modelrouter.ModelRouter;
import com.aics.chat.modelrouter.ModelRouterProperties;
import com.aics.chat.modelrouter.ModelScenario;
import com.aics.chat.modelrouter.RouteDecision;
import com.aics.chat.modelrouter.RouteRequest;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.QuotaService;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import com.aics.chat.util.ChatUserContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientAiService {

    private static final String OBS_LLM = "chat.llm";

    private final ModelRouter modelRouter;
    private final ChatModelRegistry modelRegistry;
    private final ModelHealthRegistry healthRegistry;
    private final QuotaService quotaService;
    private final ModelRouterProperties routerProperties;
    private final ObservationRegistry observationRegistry;
    private final ModelUsageRecorder modelUsageRecorder;

    @FunctionalInterface
    private interface NonStreamCall {
        ChatResponse call(ModelClientHolder holder) throws Exception;
    }

    @FunctionalInterface
    private interface StreamCall {
        Flux<String> call(ModelClientHolder holder) throws Exception;
    }

    public CompletableFuture<String> callChat(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).call().chatResponse());
    }

    public CompletableFuture<String> callRagChat(ModelScenario scenario, String prompt) {
        return invokeNonStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).call().chatResponse());
    }

    public CompletableFuture<String> callSummary(ModelScenario scenario, Prompt prompt) {
        return invokeNonStream(scenario, holder -> holder.getChatModel().call(prompt));
    }

    public CompletableFuture<Flux<String>> callSseStream(ModelScenario scenario, List<org.springframework.ai.chat.messages.Message> messages) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().messages(messages).stream().content());
    }

    public CompletableFuture<Flux<String>> callSseRagStream(ModelScenario scenario, String prompt) {
        return invokeStream(scenario,
                holder -> holder.getChatClient().prompt().user(prompt).stream().content());
    }

    private CompletableFuture<String> invokeNonStream(ModelScenario scenario, NonStreamCall call) {
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                RouteDecision decision = route(scenario);
                if (decision.getSelectedModelId() == null) {
                    log.warn("模型路由无可用模型: scenario={}, reason={}", scenario, decision.getReason());
                    return fallbackText(scenario);
                }
                List<String> chain = new ArrayList<>();
                chain.add(decision.getSelectedModelId());
                chain.addAll(decision.getFallbackChain());

                String fallbackFrom = null;
                int attempt = 0;
                Throwable lastError = null;
                for (String modelId : chain) {
                    attempt++;
                    ModelClientHolder holder = modelRegistry.get(modelId);
                    Observation observation = startLlmObservation(scenario, holder, decision, fallbackFrom, attempt);
                    try {
                        ChatResponse response = callWithTransientRetry(holder, call);
                        String text = response == null || response.getResult() == null
                                ? "" : response.getResult().getOutput().getText();
                        Usage usage = response == null || response.getMetadata() == null
                                ? null : response.getMetadata().getUsage();
                        finishLlmObservation(observation, usage, null);
                        modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                holder.getDefinition().getModel(),
                                usage == null ? null : usage.getPromptTokens(),
                                usage == null ? null : usage.getCompletionTokens(),
                                "SUCCESS", null);
                        return text;
                    } catch (Exception e) {
                        lastError = e;
                        fallbackFrom = modelId;
                        finishLlmObservation(observation, null, e);
                        modelUsageRecorder.record(scenarioId(scenario), holder.getDefinition().getProvider(),
                                holder.getDefinition().getModel(), null, null, "FAILED", e.getMessage());
                    }
                }
                log.error("所有模型调用失败: scenario={}, lastError={}",
                        scenario, lastError == null ? null : lastError.getMessage());
                return fallbackText(scenario);
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    private CompletableFuture<Flux<String>> invokeStream(ModelScenario scenario, StreamCall call) {
        TraceContext captured = TraceContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                RouteDecision decision = route(scenario);
                if (decision.getSelectedModelId() == null) {
                    return Flux.just("[ERROR]AI 助手暂时繁忙，请稍后重试。");
                }
                ModelClientHolder holder = modelRegistry.get(decision.getSelectedModelId());
                try {
                    Flux<String> flux = call.call(holder);
                    AtomicReference<Usage> usageRef = new AtomicReference<>();
                    AtomicLong firstTokenMs = new AtomicLong(-1);
                    long start = System.currentTimeMillis();
                    return flux
                            .doOnNext(chunk -> {
                                if (firstTokenMs.get() < 0) {
                                    firstTokenMs.set(System.currentTimeMillis() - start);
                                }
                            })
                            .doFinally(signal -> {
                                Usage usage = usageRef.get();
                                Observation observation = startLlmObservation(
                                        scenario, holder, decision, null, 1);
                                if (usage != null) {
                                    observation.highCardinalityKeyValue("promptTokens",
                                            String.valueOf(usage.getPromptTokens()))
                                            .highCardinalityKeyValue("completionTokens",
                                                    String.valueOf(usage.getCompletionTokens()));
                                }
                                if (firstTokenMs.get() >= 0) {
                                    observation.highCardinalityKeyValue("firstTokenMs",
                                            String.valueOf(firstTokenMs.get()));
                                }
                                modelUsageRecorder.record(scenarioId(scenario),
                                        holder.getDefinition().getProvider(),
                                        holder.getDefinition().getModel(),
                                        usage == null ? null : usage.getPromptTokens(),
                                        usage == null ? null : usage.getCompletionTokens(),
                                        signal == SignalType.ON_ERROR ? "FAILED" : "SUCCESS",
                                        signal == SignalType.ON_ERROR ? "stream error" : null);
                                finishLlmObservation(observation, usage,
                                        signal == SignalType.ON_ERROR
                                                ? new RuntimeException("stream error") : null);
                            });
                } catch (Exception e) {
                    return Flux.just("[ERROR]AI 助手暂时繁忙，请稍后重试。");
                }
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    private ChatResponse callWithTransientRetry(ModelClientHolder holder, NonStreamCall call) throws Exception {
        CircuitBreaker breaker = healthRegistry.breaker(holder.getDefinition().getId());
        try {
            return timedCall(breaker, holder, call);
        } catch (ResourceAccessException | SocketTimeoutException | TimeoutException e) {
            log.warn("模型瞬时故障，重试一次: modelId={}, err={}",
                    holder.getDefinition().getId(), e.getMessage());
            return timedCall(breaker, holder, call);
        }
    }

    private ChatResponse timedCall(CircuitBreaker breaker, ModelClientHolder holder, NonStreamCall call) throws Exception {
        return breaker.executeCallable(() -> {
            CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return call.call(holder);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
            try {
                return future.get(holder.getDefinition().getTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw e;
            }
        });
    }

    private RouteDecision route(ModelScenario scenario) {
        boolean quotaExceeded = routerProperties.getQuota().isEnabled()
                && quotaService.check(ChatUserContext.getUserId(), scenarioId(scenario)).isExceeded();
        return modelRouter.route(RouteRequest.builder()
                .scenario(scenario)
                .quotaExceeded(quotaExceeded)
                .requiredCapabilities(requiredCapabilities(scenario))
                .build());
    }

    private Set<ModelCapability> requiredCapabilities(ModelScenario scenario) {
        return switch (scenario) {
            case CHAT, RAG, AGENT, NL2SQL -> EnumSet.of(ModelCapability.TOOL_CALLING);
            default -> EnumSet.noneOf(ModelCapability.class);
        };
    }

    private String scenarioId(ModelScenario scenario) {
        return scenario.name().toLowerCase(Locale.ROOT);
    }

    private String fallbackText(ModelScenario scenario) {
        if (scenario == ModelScenario.SUMMARY) {
            return "";
        }
        return "AI 助手暂时繁忙，请稍后重试。";
    }

    private Observation startLlmObservation(ModelScenario scenario, ModelClientHolder holder,
                                            RouteDecision decision, String fallbackFrom, int attempt) {
        return Observation.createNotStarted(OBS_LLM, observationRegistry)
                .lowCardinalityKeyValue("span.type", "LLM")
                .lowCardinalityKeyValue("provider", holder.getDefinition().getProvider())
                .lowCardinalityKeyValue("model", holder.getDefinition().getModel())
                .highCardinalityKeyValue("scenario", scenarioId(scenario))
                .highCardinalityKeyValue("modelId", holder.getDefinition().getId())
                .highCardinalityKeyValue("routeReason", decision.getReason().name())
                .highCardinalityKeyValue("fallbackFrom", fallbackFrom == null ? "" : fallbackFrom)
                .highCardinalityKeyValue("attempt", String.valueOf(attempt))
                .start();
    }

    private void finishLlmObservation(Observation observation, Usage usage, Throwable error) {
        try {
            if (usage != null) {
                observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                        .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
            }
            if (error != null) {
                observation.error(error);
            }
        } finally {
            observation.stop();
        }
    }
}
```

Update `ChatServiceImpl.java` call sites:

```java
String summary = resilientAiService.callSummary(ModelScenario.SUMMARY, new Prompt(...)).get();
String response = resilientAiService.callChat(ModelScenario.CHAT, history).get();
String response = resilientAiService.callRagChat(ModelScenario.RAG, ragPrompt).get();
futureFlux = resilientAiService.callSseRagStream(ModelScenario.RAG, ragPrompt);
futureFlux = resilientAiService.callSseStream(ModelScenario.CHAT, streamHistory);
```

Add import `com.aics.chat.modelrouter.ModelScenario`.

Update `IntentClassifierService.java`:

```java
String json = resilientAiService.callRagChat(ModelScenario.INTENT, buildPrompt(input))
        .get(10, TimeUnit.SECONDS);
```

Add import `com.aics.chat.modelrouter.ModelScenario`.

Update `IntentClassifierServiceTest.java` mocks:

```java
when(resilientAiService.callRagChat(org.mockito.ArgumentMatchers.eq(ModelScenario.INTENT), anyString()))
        .thenReturn(CompletableFuture.completedFuture(json));
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl ai-cs-chat -am test -Dtest=ResilientAiServiceRoutingTest,IntentClassifierServiceTest,ChatControllerRegressionTest -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java ai-cs-chat/src/main/java/com/aics/chat/service/impl/ChatServiceImpl.java ai-cs-chat/src/main/java/com/aics/chat/agent/intent/IntentClassifierService.java ai-cs-chat/src/test/java/com/aics/chat/service/impl ai-cs-chat/src/test/java/com/aics/chat/agent/intent
git commit -m "feat(model-router): route ResilientAiService by scenario with model fallback"
```

---

### Task 7: 路由可观测性字段

**Files:**
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/observability/TraceSpan.java`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/observability/TraceSpanObservationHandler.java`
- Modify: `ai-cs-chat/src/test/java/com/aics/chat/observability/TraceSpanObservationHandlerTest.java`

**Interfaces:**
- Consumes: Observation high-cardinality keys `modelId`, `routeReason`, `fallbackFrom`, `attempt` produced by `ResilientAiService`.
- Produces: `TraceSpan.modelId`, `TraceSpan.routeReason`, `TraceSpan.fallbackFrom`, `TraceSpan.attempt`.

- [ ] **Step 1: Write the failing test additions**

Add to `TraceSpanObservationHandlerTest.java`:

```java
@Test
@DisplayName("onStop 组装路由观测字段")
void onStop_assemblesRouteFields() {
    properties.setLogExport(false);
    ObservabilityProperties props = new ObservabilityProperties();
    props.setEnabled(true);
    props.setSampleRate(1.0);
    TraceContext ctx = TraceContextHolder.begin(props, 1L, "s1", "chat");
    ObservationRegistry registry = newRegistry();

    Observation.createNotStarted("chat.llm", registry)
            .lowCardinalityKeyValue("span.type", "LLM")
            .lowCardinalityKeyValue("provider", "deepseek")
            .lowCardinalityKeyValue("model", "deepseek-chat")
            .highCardinalityKeyValue("modelId", "deepseek-chat")
            .highCardinalityKeyValue("routeReason", "PRIMARY_UNAVAILABLE")
            .highCardinalityKeyValue("fallbackFrom", "deepseek-chat")
            .highCardinalityKeyValue("attempt", "2")
            .observe(() -> {
            });

    TraceSpan span = ctx.getSpans().get(0);
    assertEquals("deepseek-chat", span.getModelId());
    assertEquals("PRIMARY_UNAVAILABLE", span.getRouteReason());
    assertEquals("deepseek-chat", span.getFallbackFrom());
    assertEquals(2, span.getAttempt());
    TraceContextHolder.clear();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl ai-cs-chat -am test -Dtest=TraceSpanObservationHandlerTest -q`
Expected: FAIL because `TraceSpan` has no `getModelId/getRouteReason/getFallbackFrom/getAttempt`.

- [ ] **Step 3: Implement fields and handler mapping**

Add to `TraceSpan.java`:

```java
/** 路由选中的模型注册表 ID（如 deepseek-chat） */
private String modelId;

/** 路由原因（SCENARIO_DEFAULT / PRIMARY_UNAVAILABLE / QUOTA_DOWNGRADE / ...） */
private String routeReason;

/** 降级前一个模型 ID（首次调用为空） */
private String fallbackFrom;

/** 本次路由中的第几次尝试（从 1 开始） */
private Integer attempt;
```

In `TraceSpanObservationHandler.onStop`, after `span.setRetries(...)` add:

```java
span.setModelId(high(context, "modelId"));
span.setRouteReason(high(context, "routeReason"));
span.setFallbackFrom(high(context, "fallbackFrom"));
span.setAttempt(intOf(high(context, "attempt")));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl ai-cs-chat -am test -Dtest=TraceSpanObservationHandlerTest -q`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add ai-cs-chat/src/main/java/com/aics/chat/observability ai-cs-chat/src/test/java/com/aics/chat/observability/TraceSpanObservationHandlerTest.java
git commit -m "feat(model-router): record routing metadata in trace spans"
```

---

### Task 8: Nacos 配置、单价与文档

**Files:**
- Modify: `tools/nacos-config/ai-cs-chat.yml`
- Modify: `ai-cs-chat/src/main/java/com/aics/chat/observability/ModelUsageProperties.java` (only if pricing defaults are needed; the main pricing lives in Nacos)
- Modify: `docs/15-AI功能与技术缺口分析.md` 3.4 状态行
- Create: `docs/superpowers/plans/2026-08-14-model-router.md` is this plan; do not commit it in this task if already committed.

**Interfaces:**
- Produces: Nacos `aics.model-router` config used by `ModelRouterProperties`, and `aics.usage.pricing` entries for Qwen models.

- [ ] **Step 1: Add Nacos model-router config**

Append to `tools/nacos-config/ai-cs-chat.yml`:

```yaml
aics:
  model-router:
    enabled: true
    models:
      - id: deepseek-chat
        provider: deepseek
        base-url: https://api.deepseek.com
        api-key: ${DEEPSEEK_API_KEY}
        model: deepseek-chat
        enabled: true
        priority: 100
        tier: standard
        capabilities: [tool-calling]
        context-window: 65536
        timeout-ms: 30000
      - id: siliconflow-qwen3-32b
        provider: siliconflow
        base-url: https://api.siliconflow.cn
        api-key: ${SILICONFLOW_API_KEY}
        model: Qwen/Qwen3-32B
        enabled: true
        priority: 90
        tier: standard
        capabilities: [tool-calling]
        context-window: 32768
        timeout-ms: 30000
      - id: siliconflow-qwen3-8b
        provider: siliconflow
        base-url: https://api.siliconflow.cn
        api-key: ${SILICONFLOW_API_KEY}
        model: Qwen/Qwen3-8B
        enabled: true
        priority: 80
        tier: cheap
        capabilities: []
        context-window: 32768
        timeout-ms: 15000
    scenarios:
      chat:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      rag:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      agent:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      nl2sql:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      judge:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      summary:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      intent:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      rewrite:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      chart:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
    quota:
      enabled: true
      over-limit-fallback-tier: cheap
```

Add pricing entries under `aics.usage`:

```yaml
aics:
  usage:
    pricing:
      deepseek-chat:
        input: 2.0
        output: 8.0
      siliconflow-qwen3-32b:
        input: 1.0
        output: 2.0
      siliconflow-qwen3-8b:
        input: 0.2
        output: 0.6
```

The existing `spring.ai.openai.*` block can remain until the router config is verified in the target Nacos; after verification it should be removed.

- [ ] **Step 2: Update gap analysis status**

In `docs/15-AI功能与技术缺口分析.md`, change row:

```markdown
| 多模型路由与模型降级 | 缺失 | P1 | 单模型成本、能力和可用性受限 |
```

to:

```markdown
| 多模型路由与模型降级 | 已实现 | P1 | 多模型注册表 + 场景路由 + 同能力降级 + 配额降档，见 docs/superpowers/specs/2026-08-14-model-router-design.md |
```

- [ ] **Step 3: Run full chat test suite**

Run: `mvn -pl ai-cs-chat -am test -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add tools/nacos-config/ai-cs-chat.yml docs/15-AI功能与技术缺口分析.md
git commit -m "feat(model-router): add Nacos routing config and update gap status"
```

---

### Task 9: 全量回归与验收检查

**Files:**
- None (verification task).

- [ ] **Step 1: Run full chat module verification**

Run: `mvn -pl ai-cs-chat -am verify -q`
Expected: PASS, including JaCoCo coverage gates.

- [ ] **Step 2: Verify acceptance items**

- Primary model failure: temporarily set `deepseek-chat.base-url` to `http://127.0.0.1:9` in Nacos, call `/chat/send`; expect answer generated by `siliconflow-qwen3-32b` and trace contains `routeReason=PRIMARY_UNAVAILABLE`, `fallbackFrom=deepseek-chat`.
- Intent/summary routing: call intent endpoint and history-compressed chat; `model_usage` records `siliconflow-qwen3-8b`.
- Quota downgrade: set a small quota in `model_usage_quota`, exceed it; expect `modelId=siliconflow-qwen3-8b` and `routeReason=QUOTA_DOWNGRADE`.
- Streaming: open `deepseek-chat` circuit breaker via fault injection, call SSE; expect first token comes from `siliconflow-qwen3-32b`.

- [ ] **Step 3: Commit any verification fixes**

If any fix is needed, commit with a message describing the fix.

---

## Self-Review

1. **Spec coverage:** Task 1-4 cover config/registry/health/router; Task 5 covers direct consumers; Task 6 covers ResilientAiService and scenario plumbing; Task 7 covers trace fields; Task 8 covers Nacos/pricing/docs; Task 9 covers acceptance. No spec requirement is left without a task.
2. **Placeholder scan:** No TBD/TODO; every code step includes concrete files and code.
3. **Type consistency:** `ModelScenario` and `RouteDecision` are introduced in Task 1 and used consistently in Tasks 4-6; `ChatModelRegistry.get/contains` are introduced in Task 2 and used by Tasks 4-6; `RoutedChatClientFactory.chatClientFor` is introduced in Task 5 and used by the three migrated services.
