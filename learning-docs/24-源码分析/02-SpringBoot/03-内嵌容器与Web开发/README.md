# 03 · 内嵌容器与 Web 开发

> 版本基线：**Spring Boot 4.2.0** / **Spring Framework 7.1.0**
> 前置：`02-SpringBoot/01-启动原理`（`SpringApplication#run` 全流程）、`02-SpringBoot/02-自动配置`（自动配置全链路与条件注解）

## 学习目标

学完本目录，你应该能够：

1. 说清 **「Web 应用启动时 Tomcat 是什么时候被创建、什么时候开始监听端口」** —— 它发生在 `refreshContext` → `onRefresh` → `createWebServer`，而不是在 `main` 方法里。
2. 读懂 `ServletWebServerApplicationContext` → `ServletWebServerFactory` → `TomcatServletWebServerFactory` → `TomcatWebServer` 四层调用链，并能对着源码指出每一步做了什么（创建 `Tomcat`、创建 `Context`、注册初始化器、`start()`）。
3. 解释 **Boot 4 的模块化拆分**：为什么 `TomcatServletWebServerFactory` 不在 `spring-boot` jar 里，而在 `spring-boot-tomcat`；`ServletWebServerApplicationContext` 在 `spring-boot-web-server`。并知道 Boot 3 → Boot 4 的包路径迁移对照。
4. 说清 `ServletContextInitializer` 是怎么被收集的（`ServletContextInitializerBeans`）、`DispatcherServlet` 是怎么变成一个 `ServletRegistrationBean` 的，以及"多个 Servlet 时路径前缀"规则。
5. 读懂 `WebMvcAutoConfiguration`：静态资源处理、消息转换器定制、`WebMvcRegistrations` 三个扩展点，以及 **`@EnableWebMvc` 为什么会让自动配置整体失效**。
6. 会用 `spring.mvc.*` 属性做常用配置，并能定位"我配了属性但不生效"的原因。

## 文章顺序

```mermaid
flowchart LR
    A["02-01 自动配置全链路<br/>（前置）"] --> B["03-01 内嵌容器启动<br/>Tomcat 与 ServletWeb 容器"]
    B --> C["03-02 WebMvc 自动配置<br/>与 DispatcherServlet 注册"]
    C --> D["04 配置与属性绑定<br/>（下一目录）"]
```

**读图说明**：

- 左边 `02-01 自动配置全链路` 属于上一个目录，这里只作为前置知识：内嵌容器之所以能被"自动"创建，靠的就是 `ServletWebServerFactoryAutoConfiguration` 一类的自动配置类。
- `03-01` 讲 **容器本体**：怎么 new 出一个 Tomcat、把它塞进 Spring 容器里、什么时候 `start()`。
- `03-02` 讲 **容器里的 Spring MVC**：`DispatcherServlet` 怎么注册进去、MVC 的自动配置做了哪些事。两篇的衔接点是 `ServletContextInitializerBeans` —— 03-01 讲它怎么被调用，03-02 讲它里面装了什么。
- 箭头是**建议顺序**，不是硬依赖；但 03-02 里"为什么 `DispatcherServlet` 在 `ServletRegistrationBean` 里"需要 03-01 的 `DelegatingServletContextInitializer` 知识。

## 文章清单

| 文件 | 主题 | 关键源码 |
| --- | --- | --- |
| `01-内嵌容器启动-tomcat与servletweb容器.md` | `onRefresh` → `createWebServer` → `TomcatServletWebServerFactory#getWebServer` → `TomcatWebServer#start`；`ServletContextInitializerBeans`；端口/上下文路径来源；优雅停机 | `ServletWebServerApplicationContext`、`TomcatServletWebServerFactory`、`TomcatWebServer`、`GracefulShutdown`、`ServerProperties` |
| `02-webmvc自动配置与dispatcherservlet注册.md` | `WebMvcAutoConfiguration`、`DispatcherServletAutoConfiguration`、`WebMvcProperties`、`WebMvcRegistrations`、静态资源、消息转换器、`@EnableWebMvc` 的副作用 | `WebMvcAutoConfiguration`、`DispatcherServletAutoConfiguration`、`WebMvcProperties`、`WebMvcRegistrations` |

## 检查点

完成本目录后，请自查：

- [ ] 能画出"`run()` → `refresh()` → `onRefresh()` → `createWebServer()` → `TomcatWebServer#start()`"的时序图，并说出每一步的大致行号所在的方法。
- [ ] 能回答：`TomcatWebServer` 构造时就已经 `tomcat.start()` 了，为什么端口还没开始对外服务？（提示：`bindOnInit=false` + 连接器被移除）
- [ ] 能说出 `ServletWebServerApplicationContext` 在 Boot 3 和 Boot 4 分别属于哪个 artifact、哪个包。
- [ ] 能解释 `server.port=-1`、`server.servlet.context-path=/api` 分别在源码哪里被读取。
- [ ] 能写出一个 `WebMvcConfigurer` 添加自定义静态资源目录，并说出它与 `spring.web.resources.static-locations` 的优先级关系。
- [ ] 能解释 `@EnableWebMvc` 为什么"一加就回到 Spring MVC 裸奔状态"，以及判断它的那个条件注解在哪一行。
