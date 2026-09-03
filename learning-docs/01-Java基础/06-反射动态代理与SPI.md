# 反射、动态代理与 SPI

> 这一篇是"框架为什么能工作"的底层三件套。来源：
> [00-学习路线总览/04-Java基础补全开发计划](../00-学习路线总览/04-Java基础补全开发计划.md) P4。
> 对应项目：`ai-cs-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
> （自定义 Starter 已落地，见 [02-Spring微服务/10](../02-Spring微服务/10-自定义Starter与自动装配.md)）、
> MyBatis-Plus 的 Mapper 接口、父 POM `annotationProcessorPaths`（Lombok + MapStruct）。

---

## 一、类加载：从字节码到 Class 对象

### 1.1 五个阶段

```
加载 → 验证 → 准备 → 解析 → 初始化
```

| 阶段 | 干什么 | 面试考点 |
|------|--------|----------|
| 加载 | 读 .class 字节流 → 方法区类元数据 + 堆中 Class 对象 | Class 对象是反射入口 |
| 验证 | 字节码合法性、安全检查 | — |
| 准备 | 静态变量分配内存并赋**零值** | `static int a = 1` 此刻 a=0；`static final int a=1` 常量在此直接赋值 |
| 解析 | 符号引用 → 直接引用 | 可延迟（懒解析） |
| 初始化 | 执行 `<clinit>()`（静态变量赋值 + static 块） | **线程安全**（JVM 加锁）；`Class.forName` 触发，`ClassLoader.loadClass` 不触发 |

**触发初始化的六个时机**（主动引用）：new / getstatic / putstatic / invokestatic、
反射调用、初始化子类先初始化父类、main 所在类。数组定义、引用 final 常量、
通过子类引用父类静态字段都**不触发**（被动引用）。

### 1.2 双亲委派模型

```
应用程序类加载器 Application（classpath，你的业务类）
        ↑ 委派
平台类加载器 Platform（JDK 9+，原 Ext）
        ↑ 委派
启动类加载器 Bootstrap（C++，java.* 核心类）
```

**流程**：收到加载请求 → 先逐级向上委派 → 父加载器找不到才自己加载。
**为什么**：① 安全——你自己写个 `java.lang.String` 也不会被加载（核心类永远
由 Bootstrap 加载）；② 唯一性——同一个类只会被加载一次。

**何时打破**（面试必答三个场景）：
1. **SPI/JDBC**：`java.sql.DriverManager` 在 Bootstrap 里，但要加载 classpath 的
   厂商驱动（Bootstrap 看不见）→ 用 `Thread.currentThread().getContextClassLoader()`
   线程上下文类加载器**反向**委派
2. **Tomcat/Osgi 隔离**：WebappClassLoader 先自己加载 WEB-INF/classes，
   实现应用间类隔离与热部署
3. **热加载**（如 JRebel）：重新读字节码 + 新类加载器实例替换旧 Class

---

## 二、反射：Spring 的一切基础

```java
// 三种获取 Class 的方式（初始化行为不同！）
Class<?> c1 = Class.forName("com.aics.order.entity.CartItem"); // 触发初始化
Class<?> c2 = CartItem.class;                                  // 不触发
Class<?> c3 = cartItem.getClass();                             // 不触发（运行时实际类型）

// 常用操作
Constructor<?> ctor = c1.getDeclaredConstructor();   // getDeclared* 含私有；getXxx 只含 public
ctor.setAccessible(true);                            // 绕过 private 检查（SecurityManager 下受限）
Object obj = ctor.newInstance();

Method m = c1.getDeclaredMethod("setQuantity", int.class);
m.invoke(obj, 3);

Field f = c1.getDeclaredField("productPrice");
f.setAccessible(true);
```

**性能与替代**：反射调用无法被 JIT 正常内联，且有参数装箱/数组封装开销
（JVM 会生成 `GeneratedMethodAccessorN` 字节码优化，第 ~15 次调用后切换）。
现代框架逐步转向 **MethodHandle / VarHandle**（类型校验前移到创建时、
更接近字节码性能），但 Spring 的 `@Autowired`、MyBatis 的结果映射、
Jackson 的字段访问，核心机制仍然是反射。

**实践要点**：高频路径的 `Method/Field` 要**缓存**（放进 static final Map），
不要每次反射解析——这也是本项目各框架封装里常见的手法。

---

## 三、动态代理：JDK Proxy vs CGLIB

### 3.1 JDK 动态代理（基于接口）

```java
public class LogHandler implements InvocationHandler {
    private final Object target;
    public LogHandler(Object target) { this.target = target; }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args);   // 反射调用真实方法
        System.out.println(method.getName() + " 耗时 " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }
}

OrderService proxy = (OrderService) Proxy.newProxyInstance(
    loader, new Class[]{OrderService.class}, new LogHandler(new OrderServiceImpl()));
// JVM 运行时生成 $Proxy0 extends Proxy implements OrderService
```

限制：**只能代理接口的方法**（$Proxy0 已继承 Proxy，Java 单继承）。

### 3.2 CGLIB（基于子类）

- 用 ASM 生成**目标类的子类**，覆写方法插回调（`MethodInterceptor`）
- 限制：**final 类/final 方法无法代理**（不能被继承/覆写）；private 方法不拦截
- `FastClass` 机制：给方法建索引直接调用，避开反射

### 3.3 与 Spring 的关系（面试高频）

| 框架行为 | 用哪个代理 |
|----------|-----------|
| Spring AOP | Boot 2.x 起 `proxyTargetClass` 默认 true → **默认 CGLIB**；有接口且显式配置时 JDK Proxy |
| `@Transactional` | AOP 代理 → 这就是**同类自调用事务失效**的根因（this 不走代理） |
| MyBatis Mapper | **JDK Proxy**（MapperProxy implements InvocationHandler）——接口没有实现类，代理补位 |
| Feign 客户端 | 同 MyBatis：接口 + 动态代理，方法调用翻译成 HTTP 请求 |

> 一个闭环理解：**代理 = AOP 的实现手段**。状态机、事务、Security `@PreAuthorize`、
> 缓存注解，在本项目里全部是"注解 + 代理拦截"生效的——
> 看到注解不生效，第一反应查：①是不是同类自调用绕过了代理；②方法是不是 private/final。

---

## 四、SPI：从 META-INF/services 到 AutoConfiguration.imports

### 4.1 JDK 原生 SPI

```
约定：接口 FQCN 同名文件放进 META-INF/services/，内容为实现类 FQCN（可多个）
```

```java
// 加载方（框架）：
ServiceLoader<RuleProvider> loader = ServiceLoader.load(RuleProvider.class);
for (RuleProvider p : loader) { providers.add(p); }   // 反射实例化实现类（需无参构造）
```

- **JDBC 就是 SPI**：DriverManager（Bootstrap 加载）通过上下文类加载器 +
  ServiceLoader 加载各厂商驱动，驱动 jar 里都有
  `META-INF/services/java.sql.Driver`（MySQL 驱动 4.0 后免 `Class.forName` 的原因）
- 不足：一次性全量实例化、无按名取用、无依赖注入 → 所以有 Dubbo SPI 等增强

### 4.2 Spring 的演进（和本项目直接相关）

```
JDK SPI (META-INF/services)                    通用、原生
  ↓ 思想被借用
Spring spring.factories                        Boot ≤2.6 主力（2.7 起弃用）
  ↓ 拆分规范化
AutoConfiguration.imports                      Boot 3.0+，本项目在用 ✅
```

`ai-cs-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
里登记了 Web/MinIO/Embedding 三个自动配置类 → Spring Boot 启动时
`@EnableAutoConfiguration` 读取该文件 → 反射实例化配置类 → `@ConditionalOnXxx`
条件过滤 → Bean 进容器。

**一句话讲透**：自动装配 = "SPI 文件登记配置类 + 反射实例化 + 条件装配筛选"，
本项目的自定义 Starter 就是亲手把这条链路走了一遍（
[02-Spring微服务/10](../02-Spring微服务/10-自定义Starter与自动装配.md)）。

### 4.3 两条注解路线对比

| | 运行时注解 + 反射 | 编译期注解处理器 |
|---|---|---|
| 代表 | Spring `@Component`、Jackson | Lombok、MapStruct |
| 时机 | 运行时读注解、反射调用 | javac 编译期生成代码 |
| 代价 | 运行时开销、启动慢 | 编译期一次成本，运行时零开销 |
| 本项目 | `@PreAuthorize`、`@Cacheable` | 父 POM annotationProcessorPaths |

---

## 五、动手实践 1：20 行「迷你 MyBatis」（JDK Proxy + 反射）

理解"MyBatis 为什么接口没有实现类也能跑"——先定义注解：

```java
@Retention(RetentionPolicy.RUNTIME)      // 运行时可读（编译期注解反射读不到）
@Target(ElementType.METHOD)
public @interface Sql {
    String value();                      // 简化：直接写 SQL
}
```

Mapper 接口与代理工厂：

```java
public interface UserMapper {
    @Sql("select * from t_user where id = ?")
    String selectById(Long id);
}

public class MiniMapperFactory {
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> mapperInterface) {
        return (T) Proxy.newProxyInstance(
            mapperInterface.getClassLoader(),
            new Class<?>[]{mapperInterface},
            (proxy, method, args) -> {
                Sql sql = method.getAnnotation(Sql.class);   // 反射读方法注解
                if (sql == null) throw new IllegalStateException("缺少 @Sql: " + method);
                // 真实 MyBatis 在这里：解析 SQL → 参数绑定 → 执行 → 结果集反射映射
                System.out.println("执行 SQL: " + sql.value() + " 参数: " + Arrays.toString(args));
                return "[模拟结果] " + method.getName();
            });
    }
}

// 使用（与 UserMapper userMapper = sqlSession.getMapper(UserMapper.class) 同构）
UserMapper mapper = MiniMapperFactory.create(UserMapper.class);
mapper.selectById(1L);   // 打印：执行 SQL: select * from t_user where id = ? 参数: [1]
```

这段代码覆盖了：**运行时注解 + 反射 + JDK 动态代理**三个考点，
和 MyBatis `MapperProxy`、Feign 客户端的工作方式同构。

---

## 六、动手实践 2：给 RuleProvider 做 SPI 化

chat 已有两个实现（本项目现场）：

```java
public interface RuleProvider { ... }                       // 策略接口
@org.springframework.stereotype.Component
public class StaticRuleProvider implements RuleProvider { ... }   // Spring 管理
```

对比三种装配方式（这是"选型思维"的练习）：

| 方式 | 写法 | 适用 |
|------|------|------|
| 现状：Spring 组件扫描 | `@Component` + 构造器注入 `List<RuleProvider>` | 实现都在本服务内 ✅ 本项目够用 |
| JDK SPI | `META-INF/services/com.aics.chat.agent.tool.RuleProvider` | 实现在外部 jar、由 jar 自带注册 |
| Spring Factories/imports | 配置类里 `@Bean` 列出实现 | 需要条件装配/依赖注入的扩展点 |

> 结论写进代码注释：单服务内的策略模式用 Spring 注入 List 即可；
> SPI 的真正价值是**跨 jar 的扩展点契约**（驱动、插件）。学习阶段可在
> common 建一个 ServiceLoader 示例验证机制，不必替换现有实现。

---

## 七、高频面试题（含参考答案）

**Q1：什么是双亲委派？为什么需要？什么时候打破？**
A：加载请求先逐级委派给父加载器，父加载器无法完成才自己加载。目的：核心类
（java.*）永远由 Bootstrap 加载，保证安全（自定义 java.lang.String 无效）与类的
唯一性（避免重复加载）。打破场景：SPI/JDBC 用线程上下文类加载器反向加载实现类；
Tomcat 为应用隔离与热部署让 WebappClassLoader 优先自加载；模块化/热替换框架。

**Q2：类加载的五个阶段？static 变量在哪个阶段赋值？**
A：加载→验证→准备→解析→初始化。准备阶段赋零值（int=0），初始化阶段执行
`<clinit>` 赋真实值（static 块 + 静态变量赋值语句合并）；`static final` 编译期常量
在准备阶段就赋值。`<clinit>` 由 JVM 保证线程安全（这就是"静态单例无并发问题"
的原因）。

**Q3：反射为什么慢？怎么优化？**
A：方法查找、安全检查、参数装箱、无法内联等运行时开销。优化：缓存 Method/Field
（static final）；`setAccessible(true)` 跳过检查；热点路径换 MethodHandle/VarHandle
或生成的字节码（LamdbaMetafactory）。JVM 对反射有 GeneratedMethodAccessor 优化，
但首次阈值（默认 15 次）前仍慢。

**Q4：JDK 动态代理和 CGLIB 的区别？Spring AOP 默认用哪个？**
A：JDK Proxy 基于接口（生成的类继承 Proxy 实现 target 接口），无接口不能用；
CGLIB 生成子类（ASM 字节码），final 类/方法不可代理，private 不拦截。
Spring Boot 2.x 起默认 `proxyTargetClass=true` 即 CGLIB，统一避免"有无接口行为
不一致"的问题。

**Q5：为什么 @Transactional 同类自调用会失效？**
A：事务由 AOP 代理实现，`this.method()` 是对象内部调用，不经过代理对象的
拦截器链，事务通知自然不生效。解法：拆到另一个 Bean、或注入自身代理
（`AopContext.currentProxy()`，需 exposeProxy=true）。

**Q6：MyBatis 的 Mapper 为什么只要接口就能用？**
A：`sqlSession.getMapper()` 返回 `MapperProxy`（InvocationHandler）的 JDK 动态
代理；方法调用被转发到 MapperMethod，它解析方法上的注解/XML 找到 SQL，
参数经反射绑定后执行，结果集再反射映射到实体。本项目 chat 的 11 个 FeignClient
是同一机制：接口方法 → 代理 → HTTP 请求。

**Q7：SPI 是什么？JDBC 和它什么关系？**
A：Service Provider Interface，JDK 内置的服务发现：接口 + `META-INF/services/接口FQCN`
文件列出实现类，`ServiceLoader` 反射实例化。JDBC 4.0 起 DriverManager 用 SPI
自动发现驱动（驱动 jar 里有 `META-INF/services/java.sql.Driver`），所以不再需要
手动 `Class.forName("com.mysql...")`。DriverManager 在 Bootstrap 类加载器中无法
看见 classpath 的驱动，靠**线程上下文类加载器**打破双亲委派完成加载。

**Q8：Spring Boot 自动装配的原理？（结合本项目 Starter 说）**
A：`@SpringBootApplication` 里的 `@EnableAutoConfiguration` → 启动时读取所有 jar 的
`META-INF/spring/...AutoConfiguration.imports`（Boot 3.0 起替代 spring.factories）
→ 反射实例化这些配置类 → `@ConditionalOnClass/@ConditionalOnProperty/
@ConditionalOnMissingBean` 等条件筛选 → 生效的 `@Bean` 进容器。本项目
`ai-cs-common` 的 Web/MinIO/Embedding 三个自动配置类就是把这套流程亲手实现了一遍。

**Q9：编译期注解和运行时注解的区别？各自代表？**
A：编译期注解由注解处理器（JSR 269）在 javac 阶段生成/修改代码，运行零开销，
代表 Lombok（修改 AST）、MapStruct（生成 Impl）；运行时注解 `@Retention(RUNTIME)`
配合反射在运行时读取，灵活但有开销，代表 Spring 全家桶。判断依据：功能能否在
"知道全部类型信息"的编译期完成——能则编译期做（更安全更快）。

**Q10：Lombok 的 @Data 有什么坑？**
A：① `@EqualsAndHashCode` 默认不调用父类属性，继承场景要 `callSuper=true`；
② 和 JPA/MyBatis 实体混用时 equals/hashCode 基于可变字段会破坏 Set/Map 语义；
③ `@Builder` 不带全参构造会与 `@NoArgsConstructor` 冲突，字段默认值要
`@Builder.Default` 否则被置零；④ `@AllArgsConstructor` 顺序按字段声明，重构字段
顺序会悄悄改变构造器签名。

---

## 八、学习检查清单

- [ ] 说出类加载五阶段与 `<clinit>` 的线程安全性
- [ ] 背出双亲委派的定义、两个目的、三个打破场景
- [ ] 现场写出 20 行迷你 MyBatis（注解 + 反射 + JDK Proxy）
- [ ] 讲清自动装配链路：imports 文件 → 反射实例化 → 条件装配 → Bean
- [ ] 说得出 @Transactional 自调用失效与 Mapper 代理的原理

## 九、后续实践（对照 04 计划 P4 任务）

1. 把迷你 MyBatis 扩展成"注解 SQL + 参数绑定 + 打印执行计划"的独立示例类
2. （可选）在 common 建一个 ServiceLoader 扩展点验证 SPI，与 Spring 注入对比取舍
3. 用反射读一遍 common 的 imports 文件并画出"文件 → Bean"链路图

---

## 下一步

→ [07-IO模型与Netty基础](./07-IO模型与Netty基础.md)
