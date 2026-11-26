<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/context/WebServerApplicationContext.java -->
<!-- 正文引用：[[源码:WebServerApplicationContext.java#L起-L止]] ；代码块头部：(src: WebServerApplicationContext.java#L起-L止) -->

# WebServerApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/context/WebServerApplicationContext.java` |
| 行数 | 77 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:55.559Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.context;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.boot.web.server.WebServer;
   10 | import org.springframework.context.ApplicationContext;
   11 | import org.springframework.context.SmartLifecycle;
   12 | import org.springframework.util.ObjectUtils;
   13 | 
   14 | /**
   15 |  * Interface to be implemented by {@link ApplicationContext application contexts} that
   16 |  * create and manage the lifecycle of an embedded {@link WebServer}.
   17 |  *
   18 |  * @author Phillip Webb
   19 |  * @since 4.0.0
   20 |  */
   21 | public interface WebServerApplicationContext extends ApplicationContext {
   22 | 
   23 | 	/**
   24 | 	 * {@link SmartLifecycle#getPhase() SmartLifecycle phase} in which graceful shutdown
   25 | 	 * of the web server is performed.
   26 | 	 * @since 4.0.0
   27 | 	 */
   28 | 	int GRACEFUL_SHUTDOWN_PHASE = SmartLifecycle.DEFAULT_PHASE - 1024;
   29 | 
   30 | 	/**
   31 | 	 * {@link SmartLifecycle#getPhase() SmartLifecycle phase} in which starting and
   32 | 	 * stopping of the web server is performed.
   33 | 	 */
   34 | 	int START_STOP_LIFECYCLE_PHASE = GRACEFUL_SHUTDOWN_PHASE - 1024;
   35 | 
   36 | 	/**
   37 | 	 * Returns the {@link WebServer} that was created by the context or {@code null} if
   38 | 	 * the server has not yet been created.
   39 | 	 * @return the web server
   40 | 	 */
   41 | 	@Nullable WebServer getWebServer();
   42 | 
   43 | 	/**
   44 | 	 * Returns the namespace of the web server application context or {@code null} if no
   45 | 	 * namespace has been set. Used for disambiguation when multiple web servers are
   46 | 	 * running in the same application (for example a management context running on a
   47 | 	 * different port).
   48 | 	 * @return the server namespace
   49 | 	 */
   50 | 	@Nullable String getServerNamespace();
   51 | 
   52 | 	/**
   53 | 	 * Returns {@code true} if the specified context is a
   54 | 	 * {@link WebServerApplicationContext} with a matching server namespace.
   55 | 	 * @param context the context to check
   56 | 	 * @param serverNamespace the server namespace to match against
   57 | 	 * @return {@code true} if the server namespace of the context matches
   58 | 	 */
   59 | 	static boolean hasServerNamespace(@Nullable ApplicationContext context, String serverNamespace) {
   60 | 		return (context instanceof WebServerApplicationContext webServerApplicationContext)
   61 | 				&& ObjectUtils.nullSafeEquals(webServerApplicationContext.getServerNamespace(), serverNamespace);
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Returns the server namespace if the specified context is a
   66 | 	 * {@link WebServerApplicationContext}.
   67 | 	 * @param context the context
   68 | 	 * @return the server namespace or {@code null} if the context is not a
   69 | 	 * {@link WebServerApplicationContext}
   70 | 	 */
   71 | 	static @Nullable String getServerNamespace(@Nullable ApplicationContext context) {
   72 | 		return (context instanceof WebServerApplicationContext configurableContext)
   73 | 				? configurableContext.getServerNamespace() : null;
   74 | 
   75 | 	}
   76 | 
   77 | }
```
