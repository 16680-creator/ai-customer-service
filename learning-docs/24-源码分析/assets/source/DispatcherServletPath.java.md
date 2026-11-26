<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/DispatcherServletPath.java -->
<!-- 正文引用：[[源码:DispatcherServletPath.java#L起-L止]] ；代码块头部：(src: DispatcherServletPath.java#L起-L止) -->

# DispatcherServletPath.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/DispatcherServletPath.java` |
| 行数 | 77 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.562Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import org.springframework.boot.web.servlet.ServletRegistrationBean;
    8 | import org.springframework.web.servlet.DispatcherServlet;
    9 | 
   10 | /**
   11 |  * Interface that can be used by auto-configurations that need path details for the
   12 |  * {@link DispatcherServletAutoConfiguration#DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME
   13 |  * default} {@link DispatcherServlet}.
   14 |  *
   15 |  * @author Madhura Bhave
   16 |  * @author Stephane Nicoll
   17 |  * @since 4.0.0
   18 |  */
   19 | @FunctionalInterface
   20 | public interface DispatcherServletPath {
   21 | 
   22 | 	/**
   23 | 	 * Returns the configured path of the dispatcher servlet.
   24 | 	 * @return the configured path
   25 | 	 */
   26 | 	String getPath();
   27 | 
   28 | 	/**
   29 | 	 * Return a form of the given path that's relative to the dispatcher servlet path.
   30 | 	 * @param path the path to make relative
   31 | 	 * @return the relative path
   32 | 	 */
   33 | 	default String getRelativePath(String path) {
   34 | 		String prefix = getPrefix();
   35 | 		if (!path.startsWith("/")) {
   36 | 			path = "/" + path;
   37 | 		}
   38 | 		return prefix + path;
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * Return a cleaned up version of the path that can be used as a prefix for URLs. The
   43 | 	 * resulting path will have path will not have a trailing slash.
   44 | 	 * @return the prefix
   45 | 	 * @see #getRelativePath(String)
   46 | 	 */
   47 | 	default String getPrefix() {
   48 | 		String result = getPath();
   49 | 		int index = result.indexOf('*');
   50 | 		if (index != -1) {
   51 | 			result = result.substring(0, index);
   52 | 		}
   53 | 		if (result.endsWith("/")) {
   54 | 			result = result.substring(0, result.length() - 1);
   55 | 		}
   56 | 		return result;
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Return a URL mapping pattern that can be used with a
   61 | 	 * {@link ServletRegistrationBean} to map the dispatcher servlet.
   62 | 	 * @return the path as a servlet URL mapping
   63 | 	 */
   64 | 	default String getServletUrlMapping() {
   65 | 		if (getPath().isEmpty() || getPath().equals("/")) {
   66 | 			return "/";
   67 | 		}
   68 | 		if (getPath().contains("*")) {
   69 | 			return getPath();
   70 | 		}
   71 | 		if (getPath().endsWith("/")) {
   72 | 			return getPath() + "*";
   73 | 		}
   74 | 		return getPath() + "/*";
   75 | 	}
   76 | 
   77 | }
```
