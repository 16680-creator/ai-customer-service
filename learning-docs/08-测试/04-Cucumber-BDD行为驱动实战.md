# 04-Cucumber BDD 行为驱动实战

> 2026-09 落地记录：本项目在 `ai-cs-chat`（7 个安全护栏 feature，31 个场景）与 `ai-cs-gateway`（2 个网关 feature，3 个场景）用 Cucumber 7.15.0 + JUnit Platform Suite 做行为驱动测试，但 [08-测试] 目录此前没有一篇 BDD 文档，本篇补齐。BDD 方法论综述另见 [05-AI集成/03-AI编码方法论/04-BDD行为驱动开发](../05-AI集成/03-AI编码方法论/04-BDD行为驱动开发.md)——那篇讲"理念"，本篇讲"本项目怎么落地"。

---

## 一、BDD 在测试金字塔里的位置

BDD（行为驱动开发）= 用**业务可读的自然语言**写验收标准，再让这段文字**可执行**。与单元测试的分工：

| 维度 | JUnit 单元测试 | Cucumber BDD |
|---|---|---|
| 表达方式 | Java 代码 | Gherkin（Given/When/Then） |
| 读者 | 开发者 | 开发 + 产品 + 测试都能看懂 |
| 粒度 | 方法级 | 业务行为级（一个完整场景） |
| 变更代价 | 低 | 稍高（步骤定义要维护） |
| 适合的场景 | 算法、工具类、边界值 | **规则类语义**：什么该拦、什么该放 |

判断标准很简单：**如果验收标准天然是一句"当…则…"的规则，用 BDD**。本项目挑中的正是两块规则最密集的领域：AI 安全护栏（注入该不该拦、PII 该不该脱敏）和网关身份（伪造头该不该剥掉）。这些规则的验收文档本身就是测试——产品经理能逐行 review `.feature` 文件。

## 二、本项目 BDD 落地全景

9 个 feature 文件、34 个场景，全部中文书写：

**ai-cs-chat**（`src/test/resources/features/security/`，AI 安全护栏）：

| 文件 | Feature | 场景数 |
|---|---|---|
| `01_prompt_injection.feature` | Prompt 注入检测（输入 Guardrail） | 7（忽略指令/索要原文/英文越狱/角色扮演/分割拼接/超长输入/正常放行） |
| `02_tool_authorization.feature` | 工具调用授权（资源级鉴权） | 4 |
| `03_pii_masking.feature` | PII 识别与脱敏 | 6 |
| `04_content_safety.feature` | 内容安全（输入/输出双向审核） | 5 |
| `05_rag_acl.feature` | RAG 数据防泄漏 | 3 |
| `06_sql_safety.feature` | SQL 安全（NL2SQL，含 1 个 Scenario Outline） | 7 |
| `07_audit_trail.feature` | 安全审计留痕 | 3 |

**ai-cs-gateway**（`src/test/resources/features/gateway/`）：

| 文件 | Feature | 场景数 |
|---|---|---|
| `01_rate_limit.feature` | 网关限流 | 2 |
| `02_trusted_identity.feature` | 身份可信透传 | 1 |

实现文档（这些规则的来源）：[05-AI集成/01-SpringAI框架集成/06-AI安全网关与Guardrails实现文档](../05-AI集成/01-SpringAI框架集成/06-AI安全网关与Guardrails实现文档.md)。

## 三、技术装配：三个依赖 + 一个 surefire 配置

`ai-cs-chat` 与 `ai-cs-gateway` 的 `pom.xml` 完全一致的三个 test 依赖：

```xml
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.15.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-suite</artifactId>
    <version>1.10.2</version>
    <scope>test</scope>
</dependency>
```

surefire 需要显式放行 Suite（否则默认按 `*Test` 命名扫描，`*Suite.java` 会被漏掉）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Suite.java</include>
        </includes>
    </configuration>
</plugin>
```

理解这套装配的关键：Cucumber 不再有自己的 runner（Cucumber 5 之后的老 `@CucumberOptions` 写法已过时），而是作为一个 **JUnit Platform 测试引擎**存在。`junit-platform-suite` 的 `@Suite` 类只是"告诉 JUnit 平台：到 `features/security` 目录下跑 cucumber 引擎"。

## 四、从零读懂一个 feature

最小的一个（`ai-cs-gateway/src/test/resources/features/gateway/02_trusted_identity.feature`，全文）：

```gherkin
Feature: 02 身份可信透传
  作为系统
  我想要在网关透传身份时移除客户端伪造的身份头
  以便下游只信任 JWT 解析出的真实用户身份

  Scenario: 伪造身份头不生效
    Given 客户端请求头携带伪造的 "X-User-Id: 999"
    And 网关从 JWT 解析出真实 userId=1
    When 网关透传身份头给下游
    Then 下游收到的 X-User-Id 仅为 "1"
    And 伪造的 "999" 被移除
```

逐层拆解：

- **Feature 头三行** = 用户故事格式（作为…我想要…以便…），这是给人看的验收声明。
- **Given**：前置状态（"伪造头已带上、真实身份已解析"）。
- **When**：被测动作（"网关透传"——只触发一件事）。
- **Then/And**：可观测断言（两步，各对应一个断言）。
- 引号里的 `"X-User-Id: 999"` 是参数：Gherkin 会把它作为 `{string}` 传给步骤方法，**文字即数据**。

注意 feature 里**没有出现任何类名、方法名、Mock 配置**——业务语言与实现解耦，这就是 BDD 的核心价值。实现换了一版（比如过滤器重写），feature 一行不用动。

## 五、步骤定义（glue）：Gherkin 与 Java 的桥

`ai-cs-gateway/src/test/java/com/aics/gateway/guardrail/GatewayGuardrailSteps.java` 的代表签名：

```java
public class GatewayGuardrailSteps {

    @Given("限流窗口为 {int} 秒内最多 {int} 次")
    public void window(int seconds, int requests) { ... }

    @When("同一用户已发起 {int} 次请求")
    public void fireRequests(int count) { ... }

    @Then("第 {int} 次请求被限流")
    public void assertLimited(int count) { ... }
}
```

`ai-cs-chat` 侧的 `SecurityGuardrailSteps`（`com.aics.chat.security.guardrail`）同理：

```java
@Given("用户输入 {string}")
public void userInput(String text) { ... }

@When("输入 Guardrail 检查该输入")
public void checkInput() { ... }

@Then("拦截原因为{string}")
public void assertReason(String reason) { ... }
```

三条经验：

1. **注解里的字符串必须与 feature 逐字匹配**（包括标点），启动时 Cucumber 做"胶水扫描"，匹配不上的步骤会在报告里提示 undefined。
2. **步骤方法要短**：`@When` 里调用被测对象一次，`@Then` 里断言，不要在步骤里写业务逻辑——逻辑在被测类里，步骤只是"遥控器"。
3. **每个 Scenario 独立**：Cucumber 对每个场景新建 steps 实例，状态不会跨场景泄漏；场景内多步之间靠 steps 类的字段传值。

## 六、Suite Runner：junit-platform-suite 写法

`ai-cs-chat/.../SecurityGuardSuite.java` 全文骨架：

```java
@Suite
@IncludeEngines("cucumber")                          // 用 cucumber 引擎执行
@SelectClasspathResource("features/security")        // feature 目录（test/resources 下）
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME,
                        value = "com.aics.chat.security.guardrail")  // 步骤类所在包
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME,
                        value = "pretty")            // 控制台逐场景输出
public class SecurityGuardSuite { }
```

gateway 侧的 `GatewayGuardSuite` 唯一区别是 `@SelectClasspathResource("features/gateway")` 与 glue 包名。

## 七、运行与验证

```bash
# 只跑网关 BDD（3 个场景）
mvn -pl ai-cs-gateway test

# 只跑 chat 安全护栏 BDD（31 个场景）
mvn -pl ai-cs-chat test

# 注意：chat 模块的 RAG 评估门禁在独立 profile，与 BDD 互不影响
mvn -pl ai-cs-chat verify -Peval    # 只跑 RagEvaluationTest
```

CI 中这两组 BDD 已随 `mvn test` 常规执行（Jenkinsfile 测试阶段），相当于每次构建都回归一遍安全规则。

## 八、动手练习

给 `01_rate_limit.feature` 增加一个场景（只动 feature，先让它 red，再去 `SlidingWindowRateLimiter`/`TokenBucketRateLimiter` 找行为依据让它 green）：

```gherkin
  Scenario: 窗口滑动后恢复放行
    Given 限流窗口为 1 秒内最多 5 次
    When 同一用户已发起 5 次请求
    And 等待窗口滑动 1 秒
    Then 再次请求被放行
```

做这个练习会逼你回答一个设计问题：步骤里"等待 1 秒"是 `Thread.sleep(1000)` 还是被测类注入可拨动的时钟？（后者是更好的答案，想想为什么。）

## 九、面试要点总结

> 本项目对规则密集的安全语义采用 Cucumber BDD：34 个 Gherkin 场景覆盖 AI 安全护栏（注入/工具授权/PII/内容安全/RAG ACL/SQL 安全/审计）与网关韧性（限流/身份透传），feature 用中文业务语言书写并与实现解耦；Cucumber 7.15 以 JUnit Platform 引擎接入，junit-platform-suite 的 @Suite 指定 feature 目录与 glue 包，surefire 显式 include *Suite.java，随 mvn test 进入 CI 常规回归。

```text
关键词：Given/When/Then = 前置/动作/断言 · glue 步骤类逐字匹配 · @Suite+@IncludeEngines("cucumber")
选型判断：规则语义用 BDD，算法边界用单测 · feature 是产品可读的活验收文档
```

## 学习检查清单

- [ ] 能不查资料写出一个最小可跑的 feature + Steps + Suite 三件套
- [ ] 能说出本项目 9 个 feature 分别保护哪条安全边界
- [ ] 能解释为什么 surefire 要 include `**/*Suite.java`
- [ ] 完成第八节动手练习并让它在 CI 通过
