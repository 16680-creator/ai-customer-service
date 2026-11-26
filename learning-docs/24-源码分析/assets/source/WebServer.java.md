<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/WebServer.java -->
<!-- 正文引用：[[源码:WebServer.java#L起-L止]] ；代码块头部：(src: WebServer.java#L起-L止) -->

# WebServer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/WebServer.java` |
| 行数 | 60 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:55.368Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server;
    6 | 
    7 | /**
    8 |  * Simple interface that represents a fully configured web server (for example Tomcat,
    9 |  * Jetty, Netty). Allows the server to be {@link #start() started} and {@link #stop()
   10 |  * stopped}.
   11 |  *
   12 |  * @author Phillip Webb
   13 |  * @author Dave Syer
   14 |  * @since 2.0.0
   15 |  */
   16 | public interface WebServer {
   17 | 
   18 | 	/**
   19 | 	 * Starts the web server. Calling this method on an already started server has no
   20 | 	 * effect.
   21 | 	 * @throws WebServerException if the server cannot be started
   22 | 	 */
   23 | 	void start() throws WebServerException;
   24 | 
   25 | 	/**
   26 | 	 * Stops the web server. Calling this method on an already stopped server has no
   27 | 	 * effect.
   28 | 	 * @throws WebServerException if the server cannot be stopped
   29 | 	 */
   30 | 	void stop() throws WebServerException;
   31 | 
   32 | 	/**
   33 | 	 * Return the port this server is listening on.
   34 | 	 * @return the port (or -1 if none)
   35 | 	 */
   36 | 	int getPort();
   37 | 
   38 | 	/**
   39 | 	 * Initiates a graceful shutdown of the web server. Handling of new requests is
   40 | 	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
   41 | 	 * attempt can be explicitly ended by invoking {@link #stop}. The default
   42 | 	 * implementation invokes the callback immediately with
   43 | 	 * {@link GracefulShutdownResult#IMMEDIATE}, i.e. no attempt is made at a graceful
   44 | 	 * shutdown.
   45 | 	 * @param callback the callback to invoke when the graceful shutdown completes
   46 | 	 * @since 2.3.0
   47 | 	 */
   48 | 	default void shutDownGracefully(GracefulShutdownCallback callback) {
   49 | 		callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Destroys the web server such that it cannot be started again.
   54 | 	 * @since 3.2.0
   55 | 	 */
   56 | 	default void destroy() {
   57 | 		stop();
   58 | 	}
   59 | 
   60 | }
```
