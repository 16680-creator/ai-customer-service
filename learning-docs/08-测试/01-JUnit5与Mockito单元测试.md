# JUnit 5 + Mockito 单元测试

> 本项目 `ai-cs-order` 和 `ai-cs-product` 模块有完整的单元测试，使用 TDD 方式开发。
> 对应项目文件：`ai-cs-order/src/test/`、`ai-cs-product/src/test/`

---

## 一、为什么需要测试？

```
没有测试：
  改了一行代码 → 不确定有没有破坏其他功能 → 心惊胆战上线 → 出 bug

有测试：
  改了一行代码 → 跑一下测试 → 全绿 → 放心上线
```

### 测试金字塔

```
        /  E2E 测试  \        ← 少量（慢、贵）
       / 集成测试     \       ← 适量
      /  单元测试      \      ← 大量（快、便宜）
     ─────────────────────
```

本项目的测试分布：
- 单元测试：`CartServiceTest`、`OrderServiceTest`、`ProductServiceTest`
- 控制器测试：`CartControllerTest`、`OrderControllerTest`
- 覆盖率要求：行覆盖 ≥ 40%，分支覆盖 ≥ 30%（JaCoCo 配置）

---

## 二、JUnit 5 基础

### 2.1 测试类结构

```java
// ai-cs-order/src/test/java/com/aics/order/service/CartServiceTest.java
@ExtendWith(MockitoExtension.class)   // 启用 Mockito
class CartServiceTest {

    @Mock                              // 模拟依赖
    private CartItemMapper cartItemMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks                       // 自动注入 Mock 到被测对象
    private CartServiceImpl cartService;

    private CartItem sampleCartItem;   // 测试数据

    @BeforeEach                        // 每个测试方法前执行
    void setUp() {
        sampleCartItem = new CartItem();
        sampleCartItem.setId(1L);
        sampleCartItem.setUserId(100L);
        sampleCartItem.setProductId(1001L);
        sampleCartItem.setProductName("无线蓝牙耳机");
        sampleCartItem.setProductPrice(new BigDecimal("199.00"));
        sampleCartItem.setQuantity(2);
        sampleCartItem.setSelected(true);
    }

    @Test                              // 标记为测试方法
    @DisplayName("获取购物车列表 - 正常返回")  // 测试描述
    void getCartList_shouldReturnItems() {
        // Given（准备）
        when(cartItemMapper.selectList(any())).thenReturn(Arrays.asList(sampleCartItem));

        // When（执行）
        CartVO result = cartService.getCartList(100L);

        // Then（验证）
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("398.00"), result.getTotalAmount());
    }
}
```

### 2.2 核心注解

| 注解 | 作用 |
|------|------|
| `@Test` | 标记测试方法 |
| `@DisplayName` | 给测试起名字（报告更清晰） |
| `@BeforeEach` | 每个测试前执行（初始化数据） |
| `@AfterEach` | 每个测试后执行（清理） |
| `@BeforeAll` | 所有测试前执行一次（static） |
| `@Disabled` | 跳过这个测试 |
| `@ParameterizedTest` | 参数化测试（一组数据跑多次） |

### 2.3 断言方法

```java
import static org.junit.jupiter.api.Assertions.*;

// 基本断言
assertEquals(expected, actual);         // 相等
assertNotEquals(a, b);                  // 不等
assertTrue(condition);                  // 为真
assertFalse(condition);                 // 为假
assertNull(obj);                        // 为空
assertNotNull(obj);                     // 非空

// 异常断言
assertThrows(BusinessException.class, () -> {
    cartService.updateQuantity(100L, 1L, 999);
});

// 不抛异常
assertDoesNotThrow(() -> cartService.deleteCartItem(100L, 1L));

// 组合断言
assertAll("购物车",
    () -> assertEquals(1, result.getItems().size()),
    () -> assertEquals(new BigDecimal("398.00"), result.getTotalAmount()),
    () -> assertEquals(1, result.getSelectedCount())
);
```

---

## 三、Mockito 模拟框架

### 3.1 为什么需要 Mock？

```
CartServiceImpl 依赖：
  ├── CartItemMapper（数据库）    → 测试时不想连真数据库
  ├── StringRedisTemplate（Redis）→ 测试时不想连真 Redis
  └── ProductFeignClient（远程）  → 测试时不想调真服务

Mock = 创建假的依赖，控制它的返回值
```

### 3.2 核心用法

```java
// ===== 打桩（Stubbing）：告诉 Mock "当被这样调用时，返回这个" =====

// 当调用 selectList(任何参数) 时，返回预设数据
when(cartItemMapper.selectList(any())).thenReturn(List.of(sampleCartItem));

// 当调用 selectById(1L) 时，返回 sampleCartItem
when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);

// 当调用 Redis 获取库存时，返回 "10"
when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
when(valueOperations.get("stock:1001")).thenReturn("10");

// 模拟抛异常
when(cartItemMapper.selectById(999L)).thenThrow(new BusinessException("不存在"));


// ===== 验证（Verify）：检查方法是否被调用过 =====

// 验证 updateById 被调用了 1 次
verify(cartItemMapper).updateById(any());

// 验证被调用了 2 次
verify(cartItemMapper, times(2)).selectById(any());

// 验证从未被调用
verify(cartItemMapper, never()).deleteById(any());

// 验证参数（捕获实际传入的值）
verify(cartItemMapper).updateById(argThat(item -> item.getQuantity() == 3));
```

### 3.3 参数匹配器

```java
any()              // 任何参数
any(Long.class)    // 任何 Long 类型
eq(100L)           // 等于 100
isNull()           // 为 null
argThat(predicate) // 自定义匹配条件
```

---

## 四、本项目的测试示例解析

### 4.1 正常流程测试

```java
@Test
@DisplayName("修改数量 - 正常修改")
void updateQuantity_shouldUpdateSuccessfully() {
    // Given：准备 Mock 行为
    when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("stock:1001")).thenReturn("10");  // 库存 10
    when(cartItemMapper.updateById(any())).thenReturn(1);

    // When：执行被测方法
    CartVO result = cartService.updateQuantity(100L, 1L, 3);  // 改为 3 个

    // Then：验证结果
    assertNotNull(result);
    verify(cartItemMapper).updateById(argThat(item -> item.getQuantity() == 3));
}
```

### 4.2 异常场景测试

```java
@Test
@DisplayName("修改数量 - 超出库存应抛出异常")
void updateQuantity_exceedStock_shouldThrowException() {
    when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("stock:1001")).thenReturn("5");  // 库存只有 5

    // 验证抛出异常 + 异常消息
    BusinessException exception = assertThrows(BusinessException.class,
        () -> cartService.updateQuantity(100L, 1L, 10));  // 要 10 个

    assertTrue(exception.getMessage().contains("库存不足"));
}

@Test
@DisplayName("修改数量 - 数量为零或负数应抛出异常")
void updateQuantity_zeroOrNegative_shouldThrowException() {
    assertThrows(IllegalArgumentException.class,
        () -> cartService.updateQuantity(100L, 1L, 0));
    assertThrows(IllegalArgumentException.class,
        () -> cartService.updateQuantity(100L, 1L, -1));
}
```

### 4.3 权限校验测试

```java
@Test
@DisplayName("删除购物车商品 - 非本人购物车项应抛出异常")
void deleteCartItem_notOwner_shouldThrowException() {
    when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);  // userId=100

    // 用 userId=999 去删 userId=100 的购物车 → 应该报错
    assertThrows(BusinessException.class,
        () -> cartService.deleteCartItem(999L, 1L));
}
```

---

## 五、TDD 开发流程

```
Red → Green → Refactor（红 → 绿 → 重构）

1. Red：先写一个会失败的测试
2. Green：写最少的代码让测试通过
3. Refactor：重构代码，保持测试绿色
```

### 示例：开发"购物车选中"功能

```java
// Step 1: Red - 先写测试（此时方法还不存在）
@Test
@DisplayName("选中/取消选中购物车商品")
void toggleSelect_shouldUpdateStatus() {
    when(cartItemMapper.selectById(1L)).thenReturn(sampleCartItem);
    when(cartItemMapper.updateById(any())).thenReturn(1);

    CartVO result = cartService.toggleSelect(100L, 1L);

    verify(cartItemMapper).updateById(argThat(item -> !item.getSelected()));
}

// Step 2: Green - 实现方法让测试通过
public CartVO toggleSelect(Long userId, Long itemId) {
    CartItem item = cartItemMapper.selectById(itemId);
    item.setSelected(!item.getSelected());
    cartItemMapper.updateById(item);
    return getCartList(userId);
}

// Step 3: Refactor - 优化代码（加权限校验等），测试保持绿色
```

---

## 六、运行测试

```bash
# 运行所有测试
mvn test

# 运行某个模块的测试
mvn test -pl ai-cs-order

# 运行某个测试类
mvn test -pl ai-cs-order -Dtest=CartServiceTest

# 运行某个测试方法
mvn test -pl ai-cs-order -Dtest=CartServiceTest#updateQuantity_shouldUpdateSuccessfully

# 生成覆盖率报告（JaCoCo）
mvn verify -pl ai-cs-order
# 报告位置：ai-cs-order/target/site/jacoco/index.html
```

---

## 七、动手练习

1. 运行 `mvn test -pl ai-cs-order`，观察测试结果
2. 打开 JaCoCo 报告，看哪些代码没被覆盖
3. 给 `deleteCartItem` 写一个新测试：删除不存在的商品
4. 用 TDD 方式给 CartService 加一个"清空购物车"方法
5. 故意改坏业务代码，看测试是否能检测到

---

## 学习检查清单

- [ ] 理解测试金字塔（单元 > 集成 > E2E）
- [ ] 熟练使用 JUnit 5 注解和断言
- [ ] 理解 Mockito 的 Mock / Stub / Verify
- [ ] 理解 Given-When-Then 测试结构
- [ ] 会测试正常流程和异常场景
- [ ] 理解 TDD 的 Red-Green-Refactor
- [ ] 会运行测试和查看覆盖率报告

---

## 下一步

→ [09-安全与设计模式/01-JWT鉴权与异常处理](../09-安全与设计模式/01-JWT鉴权与异常处理.md)
