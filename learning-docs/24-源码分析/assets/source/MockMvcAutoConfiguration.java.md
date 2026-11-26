<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc-test:4.2.0 | path: org/springframework/boot/webmvc/test/autoconfigure/MockMvcAutoConfiguration.java -->
<!-- 正文引用：[[源码:MockMvcAutoConfiguration.java#L起-L止]] ；代码块头部：(src: MockMvcAutoConfiguration.java#L起-L止) -->

# MockMvcAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc-test:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/test/autoconfigure/MockMvcAutoConfiguration.java` |
| 行数 | 48 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc-test/4.2.0/spring-boot-webmvc-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.722Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.test.autoconfigure;
    6 | 
    7 | import org.springframework.boot.autoconfigure.AutoConfiguration;
    8 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
    9 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   10 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   11 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   12 | import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
   13 | import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
   14 | import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
   15 | import org.springframework.context.annotation.Bean;
   16 | import org.springframework.context.annotation.Import;
   17 | import org.springframework.test.web.servlet.MockMvc;
   18 | import org.springframework.web.servlet.DispatcherServlet;
   19 | 
   20 | /**
   21 |  * Auto-configuration for {@link MockMvc}.
   22 |  *
   23 |  * @author Phillip Webb
   24 |  * @author Andy Wilkinson
   25 |  * @author Stephane Nicoll
   26 |  * @author Brian Clozel
   27 |  * @since 4.0.0
   28 |  * @see AutoConfigureWebMvc
   29 |  */
   30 | @AutoConfiguration(after = WebMvcAutoConfiguration.class)
   31 | @ConditionalOnWebApplication(type = Type.SERVLET)
   32 | @EnableConfigurationProperties(WebMvcProperties.class)
   33 | @Import({ MockMvcConfiguration.class, MockMvcTesterConfiguration.class })
   34 | public final class MockMvcAutoConfiguration {
   35 | 
   36 | 	@Bean
   37 | 	@ConditionalOnMissingBean
   38 | 	DispatcherServletPath dispatcherServletPath(WebMvcProperties webMvcProperties) {
   39 | 		return () -> webMvcProperties.getServlet().getPath();
   40 | 	}
   41 | 
   42 | 	@Bean
   43 | 	@ConditionalOnMissingBean
   44 | 	DispatcherServlet dispatcherServlet(MockMvc mockMvc) {
   45 | 		return mockMvc.getDispatcherServlet();
   46 | 	}
   47 | 
   48 | }
```
