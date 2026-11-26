<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-tomcat:4.2.0 | path: org/springframework/boot/tomcat/GracefulShutdown.java -->
<!-- 正文引用：[[源码:GracefulShutdown.java#L起-L止]] ；代码块头部：(src: GracefulShutdown.java#L起-L止) -->

# GracefulShutdown.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-tomcat:4.2.0` |
| 路径 | `org/springframework/boot/tomcat/GracefulShutdown.java` |
| 行数 | 138 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-tomcat/4.2.0/spring-boot-tomcat-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.383Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.tomcat;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collections;
    9 | import java.util.List;
   10 | import java.util.concurrent.CountDownLatch;
   11 | 
   12 | import org.apache.catalina.Container;
   13 | import org.apache.catalina.Service;
   14 | import org.apache.catalina.connector.Connector;
   15 | import org.apache.catalina.core.StandardContext;
   16 | import org.apache.catalina.core.StandardWrapper;
   17 | import org.apache.catalina.startup.Tomcat;
   18 | import org.apache.commons.logging.Log;
   19 | import org.apache.commons.logging.LogFactory;
   20 | 
   21 | import org.springframework.boot.web.server.GracefulShutdownCallback;
   22 | import org.springframework.boot.web.server.GracefulShutdownResult;
   23 | 
   24 | /**
   25 |  * Handles Tomcat graceful shutdown.
   26 |  *
   27 |  * @author Andy Wilkinson
   28 |  */
   29 | final class GracefulShutdown {
   30 | 
   31 | 	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);
   32 | 
   33 | 	private final Tomcat tomcat;
   34 | 
   35 | 	private volatile boolean aborted;
   36 | 
   37 | 	GracefulShutdown(Tomcat tomcat) {
   38 | 		this.tomcat = tomcat;
   39 | 	}
   40 | 
   41 | 	void shutDownGracefully(GracefulShutdownCallback callback) {
   42 | 		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
   43 | 		CountDownLatch shutdownUnderway = new CountDownLatch(1);
   44 | 		new Thread(() -> doShutdown(callback, shutdownUnderway), "tomcat-shutdown").start();
   45 | 		try {
   46 | 			shutdownUnderway.await();
   47 | 		}
   48 | 		catch (InterruptedException ex) {
   49 | 			Thread.currentThread().interrupt();
   50 | 		}
   51 | 	}
   52 | 
   53 | 	private void doShutdown(GracefulShutdownCallback callback, CountDownLatch shutdownUnderway) {
   54 | 		try {
   55 | 			List<Connector> connectors = getConnectors();
   56 | 			connectors.forEach(this::close);
   57 | 			GracefulShutdownResult result = awaitInactiveOrAborted(shutdownUnderway);
   58 | 			if (result == GracefulShutdownResult.REQUESTS_ACTIVE) {
   59 | 				logger.info("Graceful shutdown aborted with one or more requests still active");
   60 | 			}
   61 | 			else {
   62 | 				logger.info("Graceful shutdown complete");
   63 | 			}
   64 | 			callback.shutdownComplete(result);
   65 | 		}
   66 | 		finally {
   67 | 			shutdownUnderway.countDown();
   68 | 		}
   69 | 	}
   70 | 
   71 | 	private List<Connector> getConnectors() {
   72 | 		List<Connector> connectors = new ArrayList<>();
   73 | 		for (Service service : this.tomcat.getServer().findServices()) {
   74 | 			Collections.addAll(connectors, service.findConnectors());
   75 | 		}
   76 | 		return connectors;
   77 | 	}
   78 | 
   79 | 	private void close(Connector connector) {
   80 | 		connector.pause();
   81 | 		connector.getProtocolHandler().closeServerSocketGraceful();
   82 | 	}
   83 | 
   84 | 	private GracefulShutdownResult awaitInactiveOrAborted(CountDownLatch shutdownUnderway) {
   85 | 		boolean active = isActive();
   86 | 		shutdownUnderway.countDown();
   87 | 		try {
   88 | 			while (!this.aborted && isActive()) {
   89 | 				Thread.sleep(50);
   90 | 			}
   91 | 			if (this.aborted && active) {
   92 | 				return GracefulShutdownResult.REQUESTS_ACTIVE;
   93 | 			}
   94 | 			return GracefulShutdownResult.IDLE;
   95 | 		}
   96 | 		catch (InterruptedException ex) {
   97 | 			Thread.currentThread().interrupt();
   98 | 			return GracefulShutdownResult.REQUESTS_ACTIVE;
   99 | 		}
  100 | 	}
  101 | 
  102 | 	private boolean isActive() {
  103 | 		for (Container host : this.tomcat.getEngine().findChildren()) {
  104 | 			for (Container context : host.findChildren()) {
  105 | 				if (isActive(context)) {
  106 | 					return true;
  107 | 				}
  108 | 			}
  109 | 		}
  110 | 		return false;
  111 | 	}
  112 | 
  113 | 	private boolean isActive(Container context) {
  114 | 		try {
  115 | 			if (((StandardContext) context).getInProgressAsyncCount() > 0) {
  116 | 				return true;
  117 | 			}
  118 | 			for (Container wrapper : context.findChildren()) {
  119 | 				if (((StandardWrapper) wrapper).getCountAllocated() > 0) {
  120 | 					return true;
  121 | 				}
  122 | 			}
  123 | 			return false;
  124 | 		}
  125 | 		catch (Exception ex) {
  126 | 			throw new RuntimeException(ex);
  127 | 		}
  128 | 	}
  129 | 
  130 | 	void abort() {
  131 | 		this.aborted = true;
  132 | 	}
  133 | 
  134 | 	void reset() {
  135 | 		this.aborted = false;
  136 | 	}
  137 | 
  138 | }
```
