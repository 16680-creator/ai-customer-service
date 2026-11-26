# 源码阅读方法论：四步读码法 + 断点清单

> 一句话结论：读框架源码的核心不是"读完"，而是"跑起来、停在入口、把调用栈分层、带着问题跳读"。本目录两篇文章：一篇讲方法（四步读码法），一篇给工具（按框架速查的断点清单）。
> 阅读前置：建议先读完 [01-Spring框架](../../01-Spring框架/README.md) 的 IoC 与 MVC 两块，否则断点停下的地方会看不懂。
> 预计阅读：55 分钟   ｜   源码：spring-framework@7.1.0、spring-boot@4.2.0、mybatis@3.6.0、spring-security@7.2.0、spring-cloud@5.1.0

---

## 学习目标

1. 能独立为一个陌生框架找到"入口方法"，并用最小工程触发它。
2. 会在 IDE 里看调用栈，并能把它切成"容器层 / 代理层 / 业务层 / 持久层"。
3. 会写条件断点，避免在全局入口被上百次命中淹没。
4. 遇到问题（如事务失效、自动配置没生效、Mapper 找不到）时，知道第一个断点打在哪一行。

## 文章顺序

```mermaid
flowchart LR
    A["01 四步读码法<br/>方法：跑起来 → 入口断点 → 分层 → 跳读"] --> B["02 断点清单<br/>工具：按框架速查行号与看点"]
    B -.->|"练完再读"| A
```

> 读图说明：先读 01 建立"坐标系"概念，再用 02 当查表工具；每用一次清单解决一个问题，就回到 01 复盘自己走了哪几步。

| 顺序 | 文章 | 类型 | 行号已核对 |
| --- | --- | --- | --- |
| 1 | [01-四步读码法-从断点到调用栈](01-四步读码法-从断点到调用栈.md) | 方法论 | 六个入口：`refresh` L582、`doGetBean` L249、`doDispatch` L935、`TransactionInterceptor#invoke` L123、`selectList` L145、`run` L292 |
| 2 | [02-断点清单-按框架速查](02-断点清单-按框架速查.md) | 速查表 | Spring / Boot / MyBatis / Cloud / Security 共 50+ 位置 |

## 迷你实现 ↔ 真实源码：动手前的准备

读源码之前最好先亲手写一遍骨架，这样在调用栈里看到真实类时会有"似曾相识"的感觉。对应关系见 [01-手写迷你实现/README.md](../01-手写迷你实现/README.md)，例如：

| 迷你实现 | 真实入口（断点位置） |
| --- | --- |
| `MiniApplicationContext#getBean` | `AbstractBeanFactory#doGetBean` L249 |
| `MiniDispatcherServlet2#dispatch` | `DispatcherServlet#doDispatch` L935 |
| `MiniSpringApplication#run` | `SpringApplication#run` L292 |

## 运行方式

本目录不含可运行代码；需要的是"最小工程"。最快的方式：

```powershell
# 用 start.spring.io 生成一个最简 Web 工程（Boot 4.2.0），解开后在 IDEA 里打开
Invoke-WebRequest https://start.spring.io/starter.zip -OutFile demo.zip -Body @{ type = 'maven-project'; bootVersion = '4.2.0'; dependencies = 'web,mybatis,security' }
```

然后在 IDEA 中打开 `assets/source/` 对应的源码文件，按清单行号打断点。拉取基线版本源码可用 `node tools/fetch-source.mjs`。

## 检查点

- [ ] 能在 3 分钟内说出六个入口的方法名与所在类。
- [ ] 调试面板里能找到"目录页帧"并把栈切成 ≥3 层。
- [ ] 至少用过一次条件断点（beanName / URI / statement id）。
- [ ] 完成两篇的 §9 练习各 3 题以上。

## 小结

- 四步法：先跑起来 → 入口断点 → 调用栈分层 → 带着问题跳读。
- 六个入口的主线都只有 10~30 行，注释即阶段名。
- 断点清单是查表工具，行号对应本套 `assets/source/` 基线版本。

## 自测题

<details><summary>1. 四步读码法的第一步为什么是"先跑起来"？</summary>
断点只有在真实触发路径上才有意义；没有运行现场无法确认读的代码是否真的执行，也看不到变量的真实值。
</details>
<details><summary>2. Boot 启动时 `refresh` 断点为什么会命中两次？</summary>
bootstrap context 一次（Cloud 启用 bootstrap 时）+ 主 context 一次。
</details>
<details><summary>3. 排查"事务失效"第一个断点打在哪？</summary>
`TransactionInterceptor#invoke` L123；命中说明走了代理，未命中说明调用没经过代理（如 `this.xxx()`）。
</details>
<details><summary>4. 清单中的行号对应什么版本？</summary>
Spring 7.1.0 / Boot 4.2.0 / Cloud 5.1.0 / Security 7.2.0 / MyBatis 3.6.0，即 `assets/source/` 入库基线。
</details>
<details><summary>5. 全局入口（`getBean`、`doDispatch`）为什么要加条件断点？</summary>
它们在应用内命中成百上千次，不加条件会淹没目标调用。
</details>
<details><summary>6. 迷你实现与本目录的关系？</summary>
先写迷你实现建立骨架直觉，再用四步法在真实源码里验证；两者的入口一一对应（如 `dispatch` ↔ `doDispatch` L935）。
</details>
