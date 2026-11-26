<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/context/WebServerGracefulShutdownLifecycle.java -->
<!-- 正文引用：[[源码:WebServerGracefulShutdownLifecycle.java#L起-L止]] ；代码块头部：(src: WebServerGracefulShutdownLifecycle.java#L起-L止) -->

# WebServerGracefulShutdownLifecycle.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/context/WebServerGracefulShutdownLifecycle.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:55.036Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.context;
    6 | 
    7 | import org.springframework.boot.web.server.WebServer;
    8 | import org.springframework.context.SmartLifecycle;
    9 | 
   10 | /**
   11 |  * {@link SmartLifecycle} to trigger {@link WebServer} graceful shutdown.
   12 |  *
   13 |  * @author Andy Wilkinson
   14 |  * @since 4.0.0
   15 |  */
   16 | public final class WebServerGracefulShutdownLifecycle implements SmartLifecycle {
   17 | 
   18 | 	private final WebServer webServer;
   19 | 
   20 | 	private volatile boolean running;
   21 | 
   22 | 	/**
   23 | 	 * Creates a new {@code WebServerGracefulShutdownLifecycle} that will gracefully shut
   24 | 	 * down the given {@code webServer}.
   25 | 	 * @param webServer web server to shut down gracefully
   26 | 	 */
   27 | 	public WebServerGracefulShutdownLifecycle(WebServer webServer) {
   28 | 		this.webServer = webServer;
   29 | 	}
   30 | 
   31 | 	@Override
   32 | 	public void start() {
   33 | 		this.running = true;
   34 | 	}
   35 | 
   36 | 	@Override
   37 | 	public void stop() {
   38 | 		throw new UnsupportedOperationException("Stop must not be invoked directly");
   39 | 	}
   40 | 
   41 | 	@Override
   42 | 	public void stop(Runnable callback) {
   43 | 		this.running = false;
   44 | 		this.webServer.shutDownGracefully((result) -> callback.run());
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public boolean isRunning() {
   49 | 		return this.running;
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public int getPhase() {
   54 | 		return WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE;
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public boolean isPauseable() {
   59 | 		return false;
   60 | 	}
   61 | 
   62 | }
```
