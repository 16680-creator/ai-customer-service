<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc-test:4.2.0 | path: org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.java -->
<!-- 正文引用：[[源码:AutoConfigureMockMvc.java#L起-L止]] ；代码块头部：(src: AutoConfigureMockMvc.java#L起-L止) -->

# AutoConfigureMockMvc.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc-test:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.java` |
| 行数 | 102 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc-test/4.2.0/spring-boot-webmvc-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.630Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.test.autoconfigure;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Inherited;
   10 | import java.lang.annotation.Retention;
   11 | import java.lang.annotation.RetentionPolicy;
   12 | import java.lang.annotation.Target;
   13 | 
   14 | import org.htmlunit.WebClient;
   15 | import org.openqa.selenium.WebDriver;
   16 | 
   17 | import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
   18 | import org.springframework.boot.test.context.PropertyMapping;
   19 | import org.springframework.boot.test.context.PropertyMapping.Skip;
   20 | import org.springframework.test.web.servlet.MockMvc;
   21 | import org.springframework.test.web.servlet.MvcResult;
   22 | import org.springframework.test.web.servlet.assertj.MockMvcTester;
   23 | 
   24 | /**
   25 |  * Annotation that can be applied to a test class to enable and configure
   26 |  * auto-configuration of {@link MockMvc}. If AssertJ is available a {@link MockMvcTester}
   27 |  * is auto-configured as well.
   28 |  *
   29 |  * @author Phillip Webb
   30 |  * @since 4.0.0
   31 |  * @see MockMvcAutoConfiguration
   32 |  * @see SpringBootMockMvcBuilderCustomizer
   33 |  */
   34 | @Target({ ElementType.TYPE, ElementType.METHOD })
   35 | @Retention(RetentionPolicy.RUNTIME)
   36 | @Documented
   37 | @Inherited
   38 | @ImportAutoConfiguration
   39 | @PropertyMapping("spring.test.mockmvc")
   40 | public @interface AutoConfigureMockMvc {
   41 | 
   42 | 	/**
   43 | 	 * If filters from the application context should be registered with MockMVC. Defaults
   44 | 	 * to {@code true}.
   45 | 	 * @return if filters should be added
   46 | 	 */
   47 | 	boolean addFilters() default true;
   48 | 
   49 | 	/**
   50 | 	 * How {@link MvcResult} information should be printed after each MockMVC invocation.
   51 | 	 * @return how information is printed
   52 | 	 */
   53 | 	@PropertyMapping(skip = Skip.ON_DEFAULT_VALUE)
   54 | 	MockMvcPrint print() default MockMvcPrint.DEFAULT;
   55 | 
   56 | 	/**
   57 | 	 * If {@link MvcResult} information should be printed only if the test fails.
   58 | 	 * @return {@code true} if printing only occurs on failure
   59 | 	 */
   60 | 	boolean printOnlyOnFailure() default true;
   61 | 
   62 | 	/**
   63 | 	 * Settings for the integration between MockMVC and HtmlUnit.
   64 | 	 * @return the HtmlUnit settings
   65 | 	 */
   66 | 	@PropertyMapping("htmlunit")
   67 | 	HtmlUnit htmlUnit() default @HtmlUnit;
   68 | 
   69 | 	/**
   70 | 	 * HtmlUnit settings.
   71 | 	 */
   72 | 	@Target({ ElementType.TYPE, ElementType.METHOD })
   73 | 	@Retention(RetentionPolicy.RUNTIME)
   74 | 	@Documented
   75 | 	@Inherited
   76 | 	@interface HtmlUnit {
   77 | 
   78 | 		/**
   79 | 		 * The URL that should be used when expanding relative paths.
   80 | 		 * @return the URL used to expand relative paths
   81 | 		 */
   82 | 		String url() default "http://localhost";
   83 | 
   84 | 		/**
   85 | 		 * If a {@link WebClient} should be auto-configured when HtmlUnit is on the
   86 | 		 * classpath. Defaults to {@code true}.
   87 | 		 * @return if a {@link WebClient} is auto-configured
   88 | 		 */
   89 | 		@PropertyMapping("webclient.enabled")
   90 | 		boolean webClient() default true;
   91 | 
   92 | 		/**
   93 | 		 * If a {@link WebDriver} should be auto-configured when Selenium is on the
   94 | 		 * classpath. Defaults to {@code true}.
   95 | 		 * @return if a {@link WebDriver} is auto-configured
   96 | 		 */
   97 | 		@PropertyMapping("webdriver.enabled")
   98 | 		boolean webDriver() default true;
   99 | 
  100 | 	}
  101 | 
  102 | }
```
