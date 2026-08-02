# MyBatis-Plus 实战

> 本项目使用 **MyBatis-Plus 3.5.6** 作为 ORM 框架，简化数据库操作。
> 对应项目文件：`ai-cs-order/src/main/java/com/aics/order/mapper/`、`entity/`

---

## 一、MyBatis-Plus 是什么？

```
JDBC（原始）→ MyBatis（半自动）→ MyBatis-Plus（增强）

MyBatis-Plus = MyBatis + 通用 CRUD + 条件构造器 + 分页 + 代码生成
```

**核心理念**：单表 CRUD 不用写 SQL，复杂查询照样写 XML。

---

## 二、Entity 实体类

```java
// ai-cs-order/src/main/java/com/aics/order/entity/CartItem.java
@Data                          // Lombok：自动生成 getter/setter
@TableName("cart_item")        // 对应数据库表名
public class CartItem {

    @TableId(type = IdType.AUTO)  // 主键，自增
    private Long id;

    private Long userId;           // 自动映射为 user_id（驼峰→下划线）
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private Boolean selected;

    @TableField(fill = FieldFill.INSERT)  // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时填充
    private LocalDateTime updateTime;

    @TableLogic                  // 逻辑删除标记
    private Integer deleted;     // 0=正常, 1=已删除
}
```

### 注解说明

| 注解 | 作用 |
|------|------|
| `@TableName` | 指定表名（类名和表名不一致时） |
| `@TableId` | 标记主键，`IdType.AUTO` = 数据库自增 |
| `@TableField` | 字段配置（填充策略、是否忽略等） |
| `@TableLogic` | 逻辑删除（delete 变成 update deleted=1） |

---

## 三、Mapper 接口

```java
// ai-cs-order/src/main/java/com/aics/order/mapper/CartItemMapper.java
@Mapper  // 或者在启动类加 @MapperScan("com.aics.order.mapper")
public interface CartItemMapper extends BaseMapper<CartItem> {
    // 继承 BaseMapper 后，自动拥有以下方法（不用写 SQL！）：
    // insert(entity)
    // deleteById(id)
    // updateById(entity)
    // selectById(id)
    // selectList(wrapper)
    // selectPage(page, wrapper)
    // selectCount(wrapper)
    // ... 共 17 个基础方法
}
```

**就这么简单！** 一个接口继承 `BaseMapper<T>` 就能做基本 CRUD。

---

## 四、条件构造器（Wrapper）

### 4.1 QueryWrapper（查询）

```java
// 查询用户的购物车
List<CartItem> items = cartItemMapper.selectList(
    new QueryWrapper<CartItem>()
        .eq("user_id", userId)        // WHERE user_id = ?
        .eq("deleted", 0)             // AND deleted = 0
        .orderByDesc("create_time")   // ORDER BY create_time DESC
);

// 复杂条件
List<Order> orders = orderMapper.selectList(
    new QueryWrapper<Order>()
        .eq("user_id", userId)
        .ge("total_amount", 100)      // >= 100
        .in("status", 1, 2, 3)       // IN (1,2,3)
        .like("order_no", "2024")     // LIKE '%2024%'
        .between("create_time", start, end)  // BETWEEN
        .orderByDesc("create_time")
);
```

### 4.2 LambdaQueryWrapper（推荐！类型安全）

```java
// 用方法引用代替字符串列名（重构时不会漏改）
List<CartItem> items = cartItemMapper.selectList(
    new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)       // 编译期检查列名
        .eq(CartItem::getSelected, true)
        .orderByDesc(CartItem::getCreateTime)
);
```

### 4.3 UpdateWrapper（更新）

```java
// 批量更新状态
orderMapper.update(null,
    new LambdaUpdateWrapper<Order>()
        .eq(Order::getUserId, userId)
        .eq(Order::getStatus, 0)           // 待付款
        .set(Order::getStatus, 4)          // 改为已取消
        .set(Order::getUpdateTime, LocalDateTime.now())
);
```

---

## 五、分页查询

### 5.1 配置分页插件

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(
            new PaginationInnerInterceptor(DbType.MYSQL)  // 指定数据库类型
        );
        return interceptor;
    }
}
```

### 5.2 使用分页

```java
// Controller 接收分页参数
@GetMapping("/list")
public Result<Page<OrderVO>> listOrders(
        @RequestParam(defaultValue = "1") int current,   // 第几页
        @RequestParam(defaultValue = "10") int size,     // 每页几条
        @RequestParam Long userId) {
    
    Page<Order> page = orderMapper.selectPage(
        new Page<>(current, size),
        new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, userId)
            .orderByDesc(Order::getCreateTime)
    );
    
    // page.getRecords()  → 当前页数据
    // page.getTotal()    → 总记录数
    // page.getPages()    → 总页数
    
    return Result.success(convertToVO(page));
}
```

---

## 六、逻辑删除

配置后，所有 `delete` 操作自动变成 `UPDATE SET deleted=1`：

```yaml
# application.yml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted       # 全局逻辑删除字段
      logic-delete-value: 1             # 删除后的值
      logic-not-delete-value: 0         # 正常值
```

```java
// 调用 deleteById → 实际执行 UPDATE cart_item SET deleted=1 WHERE id=?
cartItemMapper.deleteById(1L);

// 查询时自动追加 WHERE deleted=0
cartItemMapper.selectList(wrapper);  // 自动过滤已删除数据
```

---

## 七、自动填充

```java
@Component
public class MetaObjectHandler implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

---

## 八、Service 层封装

MyBatis-Plus 还提供了 Service 层基类：

```java
// 接口
public interface CartItemService extends IService<CartItem> {
    CartVO getCartByUserId(Long userId);
}

// 实现
@Service
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem> 
    implements CartItemService {
    
    // 继承后自动拥有：
    // save(entity)        → insert
    // saveBatch(list)     → 批量插入
    // updateById(entity)  → 按 ID 更新
    // removeById(id)      → 删除
    // getById(id)         → 按 ID 查询
    // list(wrapper)       → 条件查询
    // page(page, wrapper) → 分页查询
    
    @Override
    public CartVO getCartByUserId(Long userId) {
        List<CartItem> items = this.list(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
        );
        // 组装 VO...
    }
}
```

---

## 九、配置总结

```yaml
# application.yml 中的 MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰（默认开启）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 控制台打印 SQL（开发用）
  global-config:
    db-config:
      id-type: auto                      # 全局主键策略：自增
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml  # XML 文件位置
```

---

## 十、动手练习

1. 在 `ai-cs-order` 中新建一个 Entity + Mapper
2. 用 `LambdaQueryWrapper` 写一个条件查询
3. 实现分页查询接口
4. 开启 SQL 日志，观察 MyBatis-Plus 生成的实际 SQL
5. 测试逻辑删除：调用 deleteById 后查看数据库（数据还在，deleted=1）

---

## 学习检查清单

- [ ] 理解 Entity 注解（@TableName、@TableId、@TableLogic）
- [ ] 熟练使用 LambdaQueryWrapper 构建查询
- [ ] 会配置和使用分页插件
- [ ] 理解逻辑删除的原理
- [ ] 理解自动填充（createTime、updateTime）
- [ ] 知道什么时候用 MP 内置方法，什么时候写自定义 SQL

---

## 下一步

→ [04-中间件/01-Redis缓存实战](../04-中间件/01-Redis缓存实战.md)
