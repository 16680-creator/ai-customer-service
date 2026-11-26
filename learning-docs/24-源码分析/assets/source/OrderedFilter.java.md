<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-servlet:4.2.0 | path: org/springframework/boot/servlet/filter/OrderedFilter.java -->
<!-- 正文引用：[[源码:OrderedFilter.java#L起-L止]] ；代码块头部：(src: OrderedFilter.java#L起-L止) -->

# OrderedFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-servlet:4.2.0` |
| 路径 | `org/springframework/boot/servlet/filter/OrderedFilter.java` |
| 行数 | 24 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-servlet/4.2.0/spring-boot-servlet-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:31:20.865Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.servlet.filter;
    6 | 
    7 | import jakarta.servlet.Filter;
    8 | 
    9 | import org.springframework.core.Ordered;
   10 | 
   11 | /**
   12 |  * An {@link Ordered} {@link jakarta.servlet.Filter}.
   13 |  *
   14 |  * @author Phillip Webb
   15 |  * @since 4.0.0
   16 |  */
   17 | public interface OrderedFilter extends Filter, Ordered {
   18 | 
   19 | 	/**
   20 | 	 * Filters that wrap the servlet request should be ordered less than or equal to this.
   21 | 	 */
   22 | 	int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;
   23 | 
   24 | }
```
