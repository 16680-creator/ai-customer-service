<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/ServletContextInitializer.java -->
<!-- 正文引用：[[源码:ServletContextInitializer.java#L起-L止]] ；代码块头部：(src: ServletContextInitializer.java#L起-L止) -->

# ServletContextInitializer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/ServletContextInitializer.java` |
| 行数 | 43 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.095Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet;
    6 | 
    7 | import jakarta.servlet.ServletContainerInitializer;
    8 | import jakarta.servlet.ServletContext;
    9 | import jakarta.servlet.ServletException;
   10 | 
   11 | import org.springframework.web.SpringServletContainerInitializer;
   12 | import org.springframework.web.WebApplicationInitializer;
   13 | 
   14 | /**
   15 |  * Interface used to configure a Servlet 3.0+ {@link ServletContext context}
   16 |  * programmatically. Unlike {@link WebApplicationInitializer}, classes that implement this
   17 |  * interface (and do not implement {@link WebApplicationInitializer}) will <b>not</b> be
   18 |  * detected by {@link SpringServletContainerInitializer} and hence will not be
   19 |  * automatically bootstrapped by the Servlet container.
   20 |  * <p>
   21 |  * This interface is designed to act in a similar way to
   22 |  * {@link ServletContainerInitializer}, but have a lifecycle that's managed by Spring and
   23 |  * not the Servlet container.
   24 |  * <p>
   25 |  * For configuration examples see {@link WebApplicationInitializer}.
   26 |  *
   27 |  * @author Phillip Webb
   28 |  * @since 4.0.0
   29 |  * @see WebApplicationInitializer
   30 |  */
   31 | @FunctionalInterface
   32 | public interface ServletContextInitializer {
   33 | 
   34 | 	/**
   35 | 	 * Configure the given {@link ServletContext} with any servlets, filters, listeners
   36 | 	 * context-params and attributes necessary for initialization.
   37 | 	 * @param servletContext the {@code ServletContext} to initialize
   38 | 	 * @throws ServletException if any call against the given {@code ServletContext}
   39 | 	 * throws a {@code ServletException}
   40 | 	 */
   41 | 	void onStartup(ServletContext servletContext) throws ServletException;
   42 | 
   43 | }
```
