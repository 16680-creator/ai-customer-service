<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/WebMvcRegistrations.java -->
<!-- 正文引用：[[源码:WebMvcRegistrations.java#L起-L止]] ；代码块头部：(src: WebMvcRegistrations.java#L起-L止) -->

# WebMvcRegistrations.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/WebMvcRegistrations.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:02.041Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
   10 | import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
   11 | import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
   12 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
   13 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
   14 | 
   15 | /**
   16 |  * Interface to register key components of the {@link WebMvcConfigurationSupport} in place
   17 |  * of the default ones provided by Spring MVC.
   18 |  * <p>
   19 |  * All custom instances are later processed by Boot and Spring MVC configurations. To
   20 |  * participate in, and if desired, override that subsequent processing,
   21 |  * {@link WebMvcConfigurer} should be used.
   22 |  * <p>
   23 |  * A single instance of this component should be registered, otherwise making it
   24 |  * impossible to choose from redundant MVC components.
   25 |  *
   26 |  * @author Brian Clozel
   27 |  * @since 4.0.0
   28 |  * @see org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration.EnableWebMvcConfiguration
   29 |  */
   30 | public interface WebMvcRegistrations {
   31 | 
   32 | 	/**
   33 | 	 * Return the custom {@link RequestMappingHandlerMapping} that should be used and
   34 | 	 * processed by the MVC configuration.
   35 | 	 * @return the custom {@link RequestMappingHandlerMapping} instance
   36 | 	 */
   37 | 	default @Nullable RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
   38 | 		return null;
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * Return the custom {@link RequestMappingHandlerAdapter} that should be used and
   43 | 	 * processed by the MVC configuration.
   44 | 	 * @return the custom {@link RequestMappingHandlerAdapter} instance
   45 | 	 */
   46 | 	default @Nullable RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
   47 | 		return null;
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * Return the custom {@link ExceptionHandlerExceptionResolver} that should be used and
   52 | 	 * processed by the MVC configuration.
   53 | 	 * @return the custom {@link ExceptionHandlerExceptionResolver} instance
   54 | 	 */
   55 | 	default @Nullable ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
   56 | 		return null;
   57 | 	}
   58 | 
   59 | }
```
