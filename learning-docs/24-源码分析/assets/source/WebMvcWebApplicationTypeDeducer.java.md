<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/WebMvcWebApplicationTypeDeducer.java -->
<!-- 正文引用：[[源码:WebMvcWebApplicationTypeDeducer.java#L起-L止]] ；代码块头部：(src: WebMvcWebApplicationTypeDeducer.java#L起-L止) -->

# WebMvcWebApplicationTypeDeducer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/WebMvcWebApplicationTypeDeducer.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:22:48.945Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.aot.hint.RuntimeHints;
   10 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   11 | import org.springframework.aot.hint.TypeReference;
   12 | import org.springframework.boot.WebApplicationType;
   13 | import org.springframework.core.annotation.Order;
   14 | import org.springframework.util.ClassUtils;
   15 | 
   16 | /**
   17 |  * {@link WebApplicationType} deducer to ensure Spring MVC applications use
   18 |  * {@link WebApplicationType#SERVLET}.
   19 |  *
   20 |  * @author Phillip Webb
   21 |  */
   22 | @Order(10) // Ordered before WebFlux
   23 | class WebMvcWebApplicationTypeDeducer implements WebApplicationType.Deducer {
   24 | 
   25 | 	private static final String[] INDICATOR_CLASSES = { "jakarta.servlet.Servlet",
   26 | 			"org.springframework.web.servlet.DispatcherServlet",
   27 | 			"org.springframework.web.context.ConfigurableWebApplicationContext" };
   28 | 
   29 | 	@Override
   30 | 	public @Nullable WebApplicationType deduceWebApplicationType() {
   31 | 		// Guard in case the classic module is being used and dependencies are excluded
   32 | 		for (String indicatorClass : INDICATOR_CLASSES) {
   33 | 			if (!ClassUtils.isPresent(indicatorClass, null)) {
   34 | 				return null;
   35 | 			}
   36 | 		}
   37 | 		return WebApplicationType.SERVLET;
   38 | 	}
   39 | 
   40 | 	static class WebMvcWebApplicationTypeDeducerRuntimeHints implements RuntimeHintsRegistrar {
   41 | 
   42 | 		@Override
   43 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
   44 | 			for (String servletIndicatorClass : INDICATOR_CLASSES) {
   45 | 				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
   46 | 			}
   47 | 		}
   48 | 
   49 | 		private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
   50 | 			if (ClassUtils.isPresent(typeName, classLoader)) {
   51 | 				hints.reflection().registerType(TypeReference.of(typeName));
   52 | 			}
   53 | 		}
   54 | 
   55 | 	}
   56 | 
   57 | }
```
