<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/DispatcherServletRegistrationBean.java -->
<!-- 正文引用：[[源码:DispatcherServletRegistrationBean.java#L起-L止]] ；代码块头部：(src: DispatcherServletRegistrationBean.java#L起-L止) -->

# DispatcherServletRegistrationBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/DispatcherServletRegistrationBean.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.474Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import java.util.Collection;
    8 | 
    9 | import org.springframework.boot.web.servlet.ServletRegistrationBean;
   10 | import org.springframework.util.Assert;
   11 | import org.springframework.web.servlet.DispatcherServlet;
   12 | 
   13 | /**
   14 |  * {@link ServletRegistrationBean} for the auto-configured {@link DispatcherServlet}. Both
   15 |  * registers the servlet and exposes {@link DispatcherServletPath} information.
   16 |  *
   17 |  * @author Phillip Webb
   18 |  * @since 4.0.0
   19 |  */
   20 | public class DispatcherServletRegistrationBean extends ServletRegistrationBean<DispatcherServlet>
   21 | 		implements DispatcherServletPath {
   22 | 
   23 | 	private final String path;
   24 | 
   25 | 	/**
   26 | 	 * Create a new {@link DispatcherServletRegistrationBean} instance for the given
   27 | 	 * servlet and path.
   28 | 	 * @param servlet the dispatcher servlet
   29 | 	 * @param path the dispatcher servlet path
   30 | 	 */
   31 | 	public DispatcherServletRegistrationBean(DispatcherServlet servlet, String path) {
   32 | 		super(servlet);
   33 | 		Assert.notNull(path, "'path' must not be null");
   34 | 		this.path = path;
   35 | 		super.addUrlMappings(getServletUrlMapping());
   36 | 	}
   37 | 
   38 | 	@Override
   39 | 	public String getPath() {
   40 | 		return this.path;
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public void setUrlMappings(Collection<String> urlMappings) {
   45 | 		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public void addUrlMappings(String... urlMappings) {
   50 | 		throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
   51 | 	}
   52 | 
   53 | }
```
