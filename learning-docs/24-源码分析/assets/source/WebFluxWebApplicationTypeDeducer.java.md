<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webflux:4.2.0 | path: org/springframework/boot/webflux/WebFluxWebApplicationTypeDeducer.java -->
<!-- 正文引用：[[源码:WebFluxWebApplicationTypeDeducer.java#L起-L止]] ；代码块头部：(src: WebFluxWebApplicationTypeDeducer.java#L起-L止) -->

# WebFluxWebApplicationTypeDeducer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webflux:4.2.0` |
| 路径 | `org/springframework/boot/webflux/WebFluxWebApplicationTypeDeducer.java` |
| 行数 | 56 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webflux/4.2.0/spring-boot-webflux-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:22:49.010Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webflux;
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
   17 |  * {@link WebApplicationType} deducer to ensure Spring WebFlux applications use
   18 |  * {@link WebApplicationType#REACTIVE}.
   19 |  *
   20 |  * @author Phillip Webb
   21 |  */
   22 | @Order(20) // Ordered after MVC
   23 | class WebFluxWebApplicationTypeDeducer implements WebApplicationType.Deducer {
   24 | 
   25 | 	private static final String[] INDICATOR_CLASSES = { "reactor.core.publisher.Mono",
   26 | 			"org.springframework.web.reactive.DispatcherHandler" };
   27 | 
   28 | 	@Override
   29 | 	public @Nullable WebApplicationType deduceWebApplicationType() {
   30 | 		// Guard in case the classic module is being used and dependencies are excluded
   31 | 		for (String indicatorClass : INDICATOR_CLASSES) {
   32 | 			if (!ClassUtils.isPresent(indicatorClass, null)) {
   33 | 				return null;
   34 | 			}
   35 | 		}
   36 | 		return WebApplicationType.REACTIVE;
   37 | 	}
   38 | 
   39 | 	static class WebFluxWebApplicationTypeDeducerRuntimeHints implements RuntimeHintsRegistrar {
   40 | 
   41 | 		@Override
   42 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
   43 | 			for (String servletIndicatorClass : INDICATOR_CLASSES) {
   44 | 				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
   45 | 			}
   46 | 		}
   47 | 
   48 | 		private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
   49 | 			if (ClassUtils.isPresent(typeName, classLoader)) {
   50 | 				hints.reflection().registerType(TypeReference.of(typeName));
   51 | 			}
   52 | 		}
   53 | 
   54 | 	}
   55 | 
   56 | }
```
