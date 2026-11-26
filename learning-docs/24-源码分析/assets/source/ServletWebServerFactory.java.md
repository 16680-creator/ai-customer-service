<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/servlet/ServletWebServerFactory.java -->
<!-- 正文引用：[[源码:ServletWebServerFactory.java#L起-L止]] ；代码块头部：(src: ServletWebServerFactory.java#L起-L止) -->

# ServletWebServerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/servlet/ServletWebServerFactory.java` |
| 行数 | 33 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:54.495Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.servlet;
    6 | 
    7 | import org.springframework.boot.web.server.WebServer;
    8 | import org.springframework.boot.web.server.WebServerFactory;
    9 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   10 | 
   11 | /**
   12 |  * Factory interface that can be used to create a {@link WebServer}.
   13 |  *
   14 |  * @author Phillip Webb
   15 |  * @since 4.0.0
   16 |  * @see WebServer
   17 |  */
   18 | @FunctionalInterface
   19 | public interface ServletWebServerFactory extends WebServerFactory {
   20 | 
   21 | 	/**
   22 | 	 * Gets a new fully configured but paused {@link WebServer} instance. Clients should
   23 | 	 * not be able to connect to the returned server until {@link WebServer#start()} is
   24 | 	 * called (which happens when the {@code ApplicationContext} has been fully
   25 | 	 * refreshed).
   26 | 	 * @param initializers {@link ServletContextInitializer}s that should be applied as
   27 | 	 * the server starts
   28 | 	 * @return a fully configured and started {@link WebServer}
   29 | 	 * @see WebServer#stop()
   30 | 	 */
   31 | 	WebServer getWebServer(ServletContextInitializer... initializers);
   32 | 
   33 | }
```
