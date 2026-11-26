<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/servlet/context/WebServerStartStopLifecycle.java -->
<!-- 正文引用：[[源码:WebServerStartStopLifecycle.java#L起-L止]] ；代码块头部：(src: WebServerStartStopLifecycle.java#L起-L止) -->

# WebServerStartStopLifecycle.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/servlet/context/WebServerStartStopLifecycle.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:54.862Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.servlet.context;
    6 | 
    7 | import org.springframework.boot.web.server.WebServer;
    8 | import org.springframework.boot.web.server.context.WebServerApplicationContext;
    9 | import org.springframework.context.SmartLifecycle;
   10 | 
   11 | /**
   12 |  * {@link SmartLifecycle} to start and stop the {@link WebServer} in a
   13 |  * {@link ServletWebServerApplicationContext}.
   14 |  *
   15 |  * @author Andy Wilkinson
   16 |  */
   17 | class WebServerStartStopLifecycle implements SmartLifecycle {
   18 | 
   19 | 	private final ServletWebServerApplicationContext applicationContext;
   20 | 
   21 | 	private final WebServer webServer;
   22 | 
   23 | 	private volatile boolean running;
   24 | 
   25 | 	WebServerStartStopLifecycle(ServletWebServerApplicationContext applicationContext, WebServer webServer) {
   26 | 		this.applicationContext = applicationContext;
   27 | 		this.webServer = webServer;
   28 | 	}
   29 | 
   30 | 	@Override
   31 | 	public void start() {
   32 | 		this.webServer.start();
   33 | 		this.running = true;
   34 | 		this.applicationContext
   35 | 			.publishEvent(new ServletWebServerInitializedEvent(this.webServer, this.applicationContext));
   36 | 	}
   37 | 
   38 | 	@Override
   39 | 	public void stop() {
   40 | 		this.running = false;
   41 | 		this.webServer.stop();
   42 | 	}
   43 | 
   44 | 	@Override
   45 | 	public boolean isRunning() {
   46 | 		return this.running;
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public int getPhase() {
   51 | 		return WebServerApplicationContext.START_STOP_LIFECYCLE_PHASE;
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public boolean isPauseable() {
   56 | 		return false;
   57 | 	}
   58 | 
   59 | }
```
