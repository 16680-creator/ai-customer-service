# 手写迷你框架示例（mini-frameworks）

这里是四篇「手写迷你实现」教程的**配套可运行代码**。四个目录各放一个 Java 文件，每个文件都是
「单文件 + 零第三方依赖 + `javac` 一把编译 + `main` 里自带断言」：

| 目录 | 文件 | 类全名 | 演示什么 | `main` 打印的 PASS 行数 |
| --- | --- | --- | --- | --- |
| `ioc/` | `MiniIoC.java` | `mini.ioc.MiniIoC` | 按类型构造器注入、单例身份、依赖缺失/歧义/循环依赖报错 | 4 |
| `aop/` | `MiniAop.java` | `mini.aop.MiniAop` | JDK 动态代理 + 拦截器链顺序 + 已检查异常原样抛出 | 3 |
| `mvc/` | `MiniMvc.java` | `mini.mvc.MiniMvc` | 路径模板路由、参数绑定、404/405/400/200 分水岭 | 7 |
| `boot/` | `MiniBoot.java` | `mini.boot.MiniBoot` | 显式候选清单 + `@ConditionalOnClass` + `@ConditionalOnMissingBean` 退让 | 3 |

> 「断言」就是 `main` 方法里 `if (!条件) throw new AssertionError(...)` 这样的检查。
> 全部通过时进程**正常退出，退出码 0**；任意一条不成立，进程会抛异常并打印 `AssertionError`。

```mermaid
flowchart LR
    A["MiniIoC<br/>怎么把对象造出来"] --> B["MiniAop<br/>造出来之后怎么加行为"]
    B --> C["MiniMvc<br/>行为怎么对外接请求"]
    C --> D["MiniBoot<br/>这些约定怎么自动生效"]
```

**读图说明**：四个示例按「容器 → 代理 → 分发 → 自动配置」的顺序层层叠加，正好对应真实 Spring
应用启动时发生的事：先把 bean 造出来（IoC），需要横切逻辑时套一层代理（AOP），把方法暴露成 URL
（MVC），最后用条件装配把上面这些约定自动化（Boot）。看代码时建议按这个顺序看，每一步只依赖前一步
的概念，不依赖后一步。

## 一、前置条件

- **JDK 17**。本文所有命令都在 Windows PowerShell 下实测过；如果你的 `java` / `javac` 不在 `PATH` 里，
  用绝对路径调用即可（下文命令就是这么写的）。
- 工作目录固定为仓库中的 `learning-docs/24-源码分析`（因为编译产物要落到它下面的 `.source-cache/`，该目录已被
  `.gitignore` 忽略，不会污染仓库）。

先把这两个变量设好（一次性，当前 PowerShell 窗口有效）：

```powershell
# 换成你自己机器上的 JDK 17 路径
$javac = "D:\DevTools\jdk\jdk17.0.19\bin\javac.exe"
$java  = "D:\DevTools\jdk\jdk17.0.19\bin\java.exe"
```

## 二、通用编译与运行方式

每个示例都是**两条命令**：一条 `javac` 编译，一条 `java` 运行。

```powershell
& $javac -encoding UTF-8 -d .source-cache/mini-check/<子目录> examples/mini-frameworks/<子目录>/<文件名>.java
& $java  -cp .source-cache/mini-check/<子目录> mini.<子目录>.<类名>
```

把 `<子目录>`、`<文件名>`、`<类名>` 换成下表中的值：

| `<子目录>` | `<文件名>` | `<类名>` |
| --- | --- | --- |
| `ioc` | `MiniIoC.java` | `MiniIoC` |
| `aop` | `MiniAop.java` | `MiniAop` |
| `mvc` | `MiniMvc.java` | `MiniMvc` |
| `boot` | `MiniBoot.java` | `MiniBoot` |

两个参数为什么必须这么写（新手最容易在这里卡住）：

- **`-d .source-cache/mini-check/<子目录>`**：告诉 `javac` 把 `.class` 放到哪个目录。因为源文件第一行有
  `package mini.ioc;` 这样的包声明，`javac` 会按包名生成 `mini/ioc/MiniIoC.class`，所以**不加 `-d`
  会把 `.class` 生成在源码旁边，把仓库弄乱**。
- **`-cp .source-cache/mini-check/<子目录>`**：告诉 `java` 去哪儿找类。这个路径是**包的根目录**
  （它的下面才是 `mini/`），所以运行时用的是包名 `mini.ioc.MiniIoC`，而不是文件路径
  `examples/mini-frameworks/ioc/MiniIoC.java`。

## 三、四个示例逐一说明

### 1. `ioc/` —— 迷你 IoC 容器：怎么把对象造出来

**演示什么**：把一组候选类型登记进 `Container`，然后 `getBean(类型)` 就能拿到实例，其中
构造器的每个参数都会回到容器里再要一次（这就是「按类型构造器注入」）。同时演示三类必须显式报错的
失败：依赖缺失（`No bean for ...`）、依赖歧义（`Multiple beans for ...`）、构造器循环依赖
（`Constructor cycle detected: ...`）。

**编译与运行**：

```powershell
& $javac -encoding UTF-8 -d .source-cache/mini-check/ioc examples/mini-frameworks/ioc/MiniIoC.java
& $java  -cp .source-cache/mini-check/ioc mini.ioc.MiniIoC
```

**真实预期输出**（实测，退出码 0）：

```text
PASS ioc: by-type constructor injection and singleton identity
PASS ioc: missing dependency rejected
PASS ioc: ambiguous dependency rejected
PASS ioc: constructor cycle rejected
```

**怎么看这四行**：第 1 行同时验证了「注入成功」和「单例身份」——`OrderService` 拿到的 `Repository`
与单独 `getBean(Repository.class)` 拿到的必须是**同一个对象**；第 2、3、4 行验证三种失败都被
`IllegalStateException` 明确报出来，而不是悄悄返回 `null` 或者栈溢出。

### 2. `aop/` —— 迷你 AOP：造出来之后怎么加行为

**演示什么**：用 JDK 动态代理给接口生成代理对象，代理里做两件事——先判断「目标方法的**实现类**方法上
有没有 `@Logged`」（这就是切点的静态判断），命中才把调用串进「拦截器链」。拦截器链用「下标 + 递归」
实现，所以是**嵌套**语义：第一个拦截器的 `before` 最先、`after` 最后。

**编译与运行**：

```powershell
& $javac -encoding UTF-8 -d .source-cache/mini-check/aop examples/mini-frameworks/aop/MiniAop.java
& $java  -cp .source-cache/mini-check/aop mini.aop.MiniAop
```

**真实预期输出**（实测，退出码 0）：

```text
PASS aop: [outer.before, inner.before, target, inner.after, outer.after]
PASS aop: same checked exception propagated; after advice ran in reverse order
PASS aop: unmatched method bypasses advice
```

**怎么看这三行**：第 1 行是成功路径的完整顺序（外 → 内 → 目标 → 内 → 外）；第 2 行验证目标方法抛出的
已检查异常 `BusinessException` 被**原样**抛给调用方（断言用的是 `ex == target.failure`，同一个对象），
且异常路径上 `after` 仍然按相反顺序执行了一次；第 3 行验证没有 `@Logged` 的方法完全绕过拦截器。

### 3. `mvc/` —— 迷你 MVC 分发器：行为怎么对外接请求

**演示什么**：启动时扫描控制器方法上的 `@Get("/orders/{id}")` 这类注解建路由表，请求进来后按
「静态段多的优先」选路由（所以 `/orders/all` 不会被 `/orders/{id}` 抢走），再按 `@Path` / `@Query`
绑定参数并转换类型，最后根据结果给出状态码：找不到路由 `404`、路径对了但动词不对 `405`、
参数绑定失败 `400`、正常 `200`。

**编译与运行**：

```powershell
& $javac -encoding UTF-8 -d .source-cache/mini-check/mvc examples/mini-frameworks/mvc/MiniMvc.java
& $java  -cp .source-cache/mini-check/mvc mini.mvc.MiniMvc
```

**真实预期输出**（实测，退出码 0）：

```text
PASS mvc: GET /orders/42 -> 200 order:42
PASS mvc: GET /orders/all -> 200 all-orders
PASS mvc: GET /missing -> 404 Not Found
PASS mvc: GET /orders/abc -> 400 Invalid number: id
PASS mvc: GET /hello -> 400 Missing parameter: name
PASS mvc: GET /hello?name=Li+Lei -> 200 hello:Li Lei
PASS mvc: POST /orders/42 -> 405 Method Not Allowed
```

**怎么看这七行**：第 2 行是最容易被写错的一行——如果路由只按注册顺序匹配，`/orders/all` 会先命中
`/orders/{id}`，然后因为 `"all"` 转不成 `long` 而返回 400；第 4 行就是那个 400 的对照实验；
第 6 行验证查询串里的 `+` 被解码成空格（`Li+Lei` → `Li Lei`），不写解码就会得到 `Li+Lei`。

### 4. `boot/` —— 迷你 Boot 自动配置：这些约定怎么自动生效

**演示什么**：`run(候选配置类[], 用户 bean...)` 依次做三件事——① 用户 bean **先**注册；② 逐个检查
配置类上的 `@ConditionalOnClass("全限定类名")`，类不在 classpath 上就**整个配置类跳过**（连实例化都
不做）；③ 条件满足的配置类，调用它的 `@Bean` 方法，但方法上若有 `@ConditionalOnMissingBean(类型)`
且容器里已经有该类型，就**退让**（跳过）。每一步的「接受 / 跳过」都记进 `Context.report`，
所以最后一行 PASS 会把整份决策日志打出来。

**编译与运行**：

```powershell
& $javac -encoding UTF-8 -d .source-cache/mini-check/boot examples/mini-frameworks/boot/MiniBoot.java
& $java  -cp .source-cache/mini-check/boot mini.boot.MiniBoot
```

**真实预期输出**（实测，退出码 0）：

```text
PASS boot: present class enables default bean; missing class skips configuration
PASS boot: user bean wins by identity; default factory was not called
PASS boot: [register user bean UserGreeting, accept class GreetingConfiguration, skip bean GreetingConfiguration.greeting, skip class MissingConfiguration]
```

**怎么看这三行**：第 3 行的日志就是答案——
`register user bean UserGreeting` 说明用户 bean 先入容器；`accept class GreetingConfiguration`
说明类条件满足；`skip bean GreetingConfiguration.greeting` 说明默认 bean 因为容器里已经有同类型 bean
而退让（`@ConditionalOnMissingBean` 生效）；`skip class MissingConfiguration` 说明类条件不满足，
且该配置类的构造器里故意写了 `throw new AssertionError(...)`——它**没有**被实例化，否则进程会直接失败。

## 四、和教程的对应关系

| 示例 | 对应教程 |
| --- | --- |
| `ioc/MiniIoC.java` | [01-手写miniioc容器.md](../../07-源码方法论与附录/01-手写迷你实现/01-手写miniioc容器.md) |
| `aop/MiniAop.java` | [02-手写miniaop代理.md](../../07-源码方法论与附录/01-手写迷你实现/02-手写miniaop代理.md) |
| `mvc/MiniMvc.java` | [03-手写minimvc分发器.md](../../07-源码方法论与附录/01-手写迷你实现/03-手写minimvc分发器.md) |
| `boot/MiniBoot.java` | [04-手写miniboot自动配置.md](../../07-源码方法论与附录/01-手写迷你实现/04-手写miniboot自动配置.md) |

教程目录的入口见 [01-手写迷你实现/README.md](../../07-源码方法论与附录/01-手写迷你实现/README.md)。

建议的阅读顺序：先读一篇教程，立刻编译运行对应的示例（上面的两条命令），看到 PASS 行之后再回去
读教程里对应的真实 Spring 源码摘录——此时你已经知道「那几十行 Spring 代码在解决什么具体问题」。

## 五、常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `错误: 找不到文件: examples/mini-frameworks/...` | 当前目录不是 `learning-docs/24-源码分析` | 先 `cd learning-docs/24-源码分析`，再执行命令 |
| `错误: 找不到或无法加载主类 mini.ioc.MiniIoC` | `-cp` 指错了层级 | `-cp` 要指向**包的根目录**（`.../ioc`），不是它的上级 |
| 中文注释在 `javac` 时报编码错误 | 源文件是 UTF-8，但平台默认编码不是 | 编译时务必带 `-encoding UTF-8` |
| 运行时打印 `AssertionError: ...` | 断言不成立（说明代码被改动过或 JDK 版本不对） | 对照本文「真实预期输出」逐行比对，用 JDK 17 |
| 仓库里多出一堆 `.class` 文件 | 编译时漏了 `-d` | 删掉它们，重新加上 `-d .source-cache/mini-check/<子目录>` |

## 小结

- 四个示例都是**单文件、零依赖**，用 `javac -encoding UTF-8 -d .source-cache/mini-check/<子目录>` 编译、
  `java -cp .source-cache/mini-check/<子目录> mini.<子目录>.<类名>` 运行。
- `-d` 决定 `.class` 落在哪（按包名建子目录），`-cp` 指向包根目录，运行时用**类全名**。
- 每个示例的自测断言都是可验证的承诺：全部通过时打印固定行数的 `PASS`，退出码 0。
- 四者按 IoC → AOP → MVC → Boot 叠起来，就是一次 Spring 应用启动的骨架。
