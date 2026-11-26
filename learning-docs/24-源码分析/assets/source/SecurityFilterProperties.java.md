<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-security:4.2.0 | path: org/springframework/boot/security/autoconfigure/web/servlet/SecurityFilterProperties.java -->
<!-- 正文引用：[[源码:SecurityFilterProperties.java#L起-L止]] ；代码块头部：(src: SecurityFilterProperties.java#L起-L止) -->

# SecurityFilterProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-security:4.2.0` |
| 路径 | `org/springframework/boot/security/autoconfigure/web/servlet/SecurityFilterProperties.java` |
| 行数 | 65 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-security/4.2.0/spring-boot-security-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:37.042Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.security.autoconfigure.web.servlet;
    6 | 
    7 | import java.util.EnumSet;
    8 | import java.util.Set;
    9 | 
   10 | import org.springframework.boot.context.properties.ConfigurationProperties;
   11 | import org.springframework.boot.servlet.filter.OrderedFilter;
   12 | import org.springframework.boot.web.servlet.DispatcherType;
   13 | import org.springframework.core.Ordered;
   14 | 
   15 | /**
   16 |  * Configuration properties for Spring Security Filter.
   17 |  *
   18 |  * @author Phillip Webb
   19 |  * @since 4.0.0
   20 |  */
   21 | @ConfigurationProperties("spring.security.filter")
   22 | public class SecurityFilterProperties {
   23 | 
   24 | 	/**
   25 | 	 * Order applied to the {@code SecurityFilterChain} that is used to configure basic
   26 | 	 * authentication for application endpoints. Create your own
   27 | 	 * {@code SecurityFilterChain} if you want to add your own authentication for all or
   28 | 	 * some of those endpoints.
   29 | 	 */
   30 | 	public static final int BASIC_AUTH_ORDER = Ordered.LOWEST_PRECEDENCE - 5;
   31 | 
   32 | 	/**
   33 | 	 * Default order of Spring Security's Filter in the servlet container (i.e. amongst
   34 | 	 * other filters registered with the container). There is no connection between this
   35 | 	 * and the {@code @Order} on a {@code SecurityFilterChain}.
   36 | 	 */
   37 | 	public static final int DEFAULT_FILTER_ORDER = OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100;
   38 | 
   39 | 	/**
   40 | 	 * Security filter chain order for Servlet-based web applications.
   41 | 	 */
   42 | 	private int order = DEFAULT_FILTER_ORDER;
   43 | 
   44 | 	/**
   45 | 	 * Security filter chain dispatcher types for Servlet-based web applications.
   46 | 	 */
   47 | 	private Set<DispatcherType> dispatcherTypes = EnumSet.allOf(DispatcherType.class);
   48 | 
   49 | 	public int getOrder() {
   50 | 		return this.order;
   51 | 	}
   52 | 
   53 | 	public void setOrder(int order) {
   54 | 		this.order = order;
   55 | 	}
   56 | 
   57 | 	public Set<DispatcherType> getDispatcherTypes() {
   58 | 		return this.dispatcherTypes;
   59 | 	}
   60 | 
   61 | 	public void setDispatcherTypes(Set<DispatcherType> dispatcherTypes) {
   62 | 		this.dispatcherTypes = dispatcherTypes;
   63 | 	}
   64 | 
   65 | }
```
