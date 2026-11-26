<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/servlet/context/ServletWebServerApplicationContextFactory.java -->
<!-- 正文引用：[[源码:ServletWebServerApplicationContextFactory.java#L起-L止]] ；代码块头部：(src: ServletWebServerApplicationContextFactory.java#L起-L止) -->

# ServletWebServerApplicationContextFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/servlet/context/ServletWebServerApplicationContextFactory.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.656Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.servlet.context;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.aot.AotDetector;
   10 | import org.springframework.boot.ApplicationContextFactory;
   11 | import org.springframework.boot.WebApplicationType;
   12 | import org.springframework.boot.web.context.servlet.ApplicationServletEnvironment;
   13 | import org.springframework.context.ConfigurableApplicationContext;
   14 | import org.springframework.core.env.ConfigurableEnvironment;
   15 | 
   16 | /**
   17 |  * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
   18 |  * {@link AnnotationConfigServletWebServerApplicationContext} and
   19 |  * {@link ServletWebServerApplicationContext}.
   20 |  *
   21 |  * @author Phillip Webb
   22 |  * @author Andy Wilkinson
   23 |  */
   24 | class ServletWebServerApplicationContextFactory implements ApplicationContextFactory {
   25 | 
   26 | 	@Override
   27 | 	public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
   28 | 			@Nullable WebApplicationType webApplicationType) {
   29 | 		return (webApplicationType != WebApplicationType.SERVLET) ? null : ApplicationServletEnvironment.class;
   30 | 	}
   31 | 
   32 | 	@Override
   33 | 	public @Nullable ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
   34 | 		return (webApplicationType != WebApplicationType.SERVLET) ? null : new ApplicationServletEnvironment();
   35 | 	}
   36 | 
   37 | 	@Override
   38 | 	public @Nullable ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
   39 | 		return (webApplicationType != WebApplicationType.SERVLET) ? null : createContext();
   40 | 	}
   41 | 
   42 | 	private ConfigurableApplicationContext createContext() {
   43 | 		if (!AotDetector.useGeneratedArtifacts()) {
   44 | 			return new AnnotationConfigServletWebServerApplicationContext();
   45 | 		}
   46 | 		return new ServletWebServerApplicationContext();
   47 | 	}
   48 | 
   49 | }
```
