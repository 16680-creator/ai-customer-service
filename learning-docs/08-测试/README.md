# 08-测试

> 会写 `assert` 只是开始。真正的价值在于：测试快、稳定、能定位问题，让重构无忧。

---

## 📚 章节导航

| 章节 | 核心内容 | 难度 | 适用场景 |
|------|---------|------|---------|
| 单元测试 | JUnit 5 / Mockito / 测试分层 | ⭐⭐ | 业务逻辑验证 |
| 集成测试 | MockMvc / TestContainers / H2 | ⭐⭐⭐ | 跨层协作验证 |
| 性能测试 | JMH / Gatling / 压测方法论 | ⭐⭐⭐ | 性能基线建立 |
| 测试覆盖率 | JaCoCo / 门禁 / 有效性评估 | ⭐⭐ | 质量门禁 |
| TDD 实战 | Red-Green-Refactor / 练习 | ⭐⭐⭐⭐ | 代码质量提升 |

---

## 🎯 测试金字塔

```
        E2E 测试（少）
       /          \
      /  UI 自动化  \
     /--------------\
    /  接口集成测试  \
   /------------------\
  /    单元测试（多）   \
 /----------------------\
```

**比例建议**：单元：集成：E2E = 70:20:10

---

## 💡 核心原则

### 1. FIRST 原则
- **Fast** — 毫秒级（单测）、秒级（集成测试）
- **Independent** — 每个测试独立，顺序无关
- **Repeatable** — 任何环境都能复现
- **Self-Validating** — 断言清晰，自动通过/失败
- **Timely** — 代码写完立即补测试（TDD 更好）

### 2. 测试命名三段式
```java
// 结构：方法名_场景_预期结果
@Test
void createOrder_whenOutOfStock_thenThrowException() {
    // given
    // when
    // then
}
```

### 3. Given-When-Then 模式
```java
@Test
void test() {
    // Given: 准备数据和环境
    Order order = new Order("NO001", 100);
    
    // When: 执行被测试方法
    OrderService service = new OrderService();
    Result result = service.submit(order);
    
    // Then: 断言结果
    assertThat(result.isSuccess()).isTrue();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
}
```

---

## 🔧 实战场景

### 场景 1：Mock 外部依赖

**问题**：单元测试不应依赖真实的数据库 / Redis / HTTP 调用

**解法**（Mockito）：
```java
@Test
void getUserInfo_shouldCallRemoteApi() {
    // Mock 外部服务
    UserClient client = Mockito.mock(UserClient.class);
    when(client.getUser(1)).thenReturn(new User(1, "Alice"));
    
    UserService service = new UserService(client);
    User user = service.getUserInfo(1);
    
    // 验证
    assertThat(user.getName()).isEqualTo("Alice");
    verify(client, times(1)).getUser(1);
}
```

### 场景 2：集成测试（MockMvc）

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    
    @Autowired
    private MockMvc mvc;
    
    @Test
    void createOrder_shouldReturn201() throws Exception {
        mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": 1, \"quantity\": 2}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.orderNo").exists());
    }
}
```

### 场景 3：数据库集成测试（H2 / TestContainers）

```java
@DataJpaTest  // 自动配置 H2 内存数据库
class OrderRepositoryTest {
    
    @Autowired
    private OrderRepository repo;
    
    @Test
    void findByStatus_shouldReturnMatchingOrders() {
        // Given
        Order o1 = new Order("NO001", OrderStatus.PENDING);
        Order o2 = new Order("NO002", OrderStatus.PAID);
        repo.saveAll(List.of(o1, o2));
        
        // When
        List<Order> pending = repo.findByStatus(OrderStatus.PENDING);
        
        // Then
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getOrderNo()).isEqualTo("NO001");
    }
}
```

---

## 📊 测试覆盖率

### JaCoCo 配置

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>  <!-- 60% 门禁 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

运行：
```bash
mvn clean verify

# 查看报告
open target/site/jacoco/index.html
```

### 100% 覆盖率 ≠ 高质量

**覆盖了但没测到**：
```java
// ❌ 覆盖率 100%，但没有实际断言
@Test
void test() {
    service.processOrder(order);  // 行被执行，但不知道对不对
}

// ✅ 覆盖率 100%，且有意义的断言
@Test
void test() {
    Result result = service.processOrder(order);
    assertThat(result.isSuccess()).isTrue();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
}
```

---

## ⚡ 常见问题

### Q1：测试跑得太慢？
**诊断**：
```bash
mvn test -Dtest=OrderServiceTest | grep "Time elapsed"
# OrderServiceTest.test1  Time elapsed: 3.234 s
```

**优化**：
- 用 `@MockBean` 替代真实 Spring 容器
- 用 H2 替代真实数据库
- 拆分测试类（`@WebMvcTest` / `@DataJpaTest`）

### Q2：测试偶尔失败（Flaky Test）？
**常见原因**：
- 依赖执行顺序
- 多线程竞态条件
- 时间依赖（`Thread.sleep`）
- 共享可变状态

**修复**：
- 每个测试独立准备数据
- 用 `CountDownLatch` 同步多线程
- 用 `@BeforeEach` / `@AfterEach` 清理状态

### Q3：如何测试异步方法？
```java
@Test
void asyncMethod_shouldComplete() throws Exception {
    CompletableFuture<String> future = service.asyncProcess();
    
    // ❌ 不要这样（会立即返回，还没执行完）
    assertThat(future.isDone()).isTrue();
    
    // ✅ 等待完成
    String result = future.get(5, TimeUnit.SECONDS);
    assertThat(result).isEqualTo("done");
}
```

---

## 📈 测试检查清单

```
□ 所有 Service 方法有单元测试
□ 所有 Controller 端点有集成测试
□ 覆盖率 ≥ 60%（JaCoCo 门禁）
□ 测试命名清晰（方法名_场景_预期）
□ 无 Flaky 测试（CI 跑 3 次全绿）
□ 测试速度 < 10s（单元测试套件）
□ 异常分支都测过
□ 边界值都测过
□ 并发场景测过（如有）
□ Mock 外部依赖（不访问真实网络/DB）
```

---

## 🔗 与其他模块关联

```
08-测试
  ├── ← 02-Spring微服务（MockMvc / @SpringBootTest）
  ├── ← 03-数据库与ORM（@DataJpaTest / H2）
  ├── → 12-性能工程（JMH 基准测试）
  └── → 22-Python速成/18 篇（pytest 对标 JUnit）
```

---

## 📚 推荐资源

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [22-Python速成/18 篇 测试进阶](../22-Python速成/18-测试进阶-pytest与可测性设计.md)
- [GOOS Book - Growing Object-Oriented Software, Guided by Tests](http://www.growing-object-oriented-software.com/)

---

## 💡 TDD 实战练习

### 练习 1：字符串计算器（30 min）
实现一个字符串计算器，支持加法：
```java
// 红（测试先行）
@Test
void add_emptyString_returnsZero() {
    assertThat(calculator.add("")).isEqualTo(0);
}

// 绿（最小实现）
public int add(String numbers) {
    if (numbers.isEmpty()) return 0;
    // ...
}

// 重构（优化）
```

### 练习 2：订单状态机（1 hour）
用 TDD 实现订单状态流转，确保只能按规则流转。

---

**下一步**：从最简单的单元测试开始，逐步补齐项目中的测试覆盖率。
