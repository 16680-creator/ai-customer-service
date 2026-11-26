<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc-test:4.2.0 | path: org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.java -->
<!-- 正文引用：[[源码:WebMvcTest.java#L起-L止]] ；代码块头部：(src: WebMvcTest.java#L起-L止) -->

# WebMvcTest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc-test:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.java` |
| 行数 | 157 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc-test/4.2.0/spring-boot-webmvc-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:19:16.113Z |

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
   14 | import org.junit.jupiter.api.extension.ExtendWith;
   15 | 
   16 | import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
   17 | import org.springframework.boot.autoconfigure.SpringBootApplication;
   18 | import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
   19 | import org.springframework.boot.test.context.SpringBootTest;
   20 | import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
   21 | import org.springframework.context.annotation.ComponentScan.Filter;
   22 | import org.springframework.context.annotation.Import;
   23 | import org.springframework.core.annotation.AliasFor;
   24 | import org.springframework.core.env.Environment;
   25 | import org.springframework.test.context.BootstrapWith;
   26 | import org.springframework.test.context.junit.jupiter.SpringExtension;
   27 | import org.springframework.test.web.servlet.MockMvc;
   28 | 
   29 | /**
   30 |  * Annotation that can be used for a Spring MVC test that focuses <strong>only</strong> on
   31 |  * Spring MVC components.
   32 |  * <p>
   33 |  * Using this annotation only enables auto-configuration that is relevant to MVC tests.
   34 |  * Similarly, component scanning is limited to beans annotated with:
   35 |  * <ul>
   36 |  * <li>{@code @Controller}</li>
   37 |  * <li>{@code @ControllerAdvice}</li>
   38 |  * <li>{@code @JacksonComponent}</li>
   39 |  * <li>{@code @JsonComponent} (Jackson 2, deprecated)</li>
   40 |  * </ul>
   41 |  * <p>
   42 |  * as well as beans that implement:
   43 |  * <ul>
   44 |  * <li>{@code Converter}</li>
   45 |  * <li>{@code DelegatingFilterProxyRegistrationBean}</li>
   46 |  * <li>{@code ErrorAttributes}</li>
   47 |  * <li>{@code Filter}</li>
   48 |  * <li>{@code FilterRegistrationBean}</li>
   49 |  * <li>{@code GenericConverter}</li>
   50 |  * <li>{@code HandlerInterceptor}</li>
   51 |  * <li>{@code HandlerMethodArgumentResolver}</li>
   52 |  * <li>{@code HttpMessageConverter}</li>
   53 |  * <li>{@code IDialect}, if Thymeleaf is available</li>
   54 |  * <li>{@code JacksonModule}, if Jackson is available</li>
   55 |  * <li>{@code Module} (deprecated), if Jackson 2 is available</li>
   56 |  * <li>{@code SecurityFilterChain}</li>
   57 |  * <li>{@code WebMvcConfigurer}</li>
   58 |  * <li>{@code WebMvcRegistrations}</li>
   59 |  * <li>{@code WebSecurityConfigurer}</li>
   60 |  * </ul>
   61 |  * <p>
   62 |  * By default, tests annotated with {@code @WebMvcTest} will also auto-configure Spring
   63 |  * Security and {@link MockMvc} (include support for HtmlUnit WebClient and Selenium
   64 |  * WebDriver). For more fine-grained control of MockMVC the
   65 |  * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} annotation can be used.
   66 |  * <p>
   67 |  * Typically {@code @WebMvcTest} is used in combination with
   68 |  * {@link org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean}
   69 |  * or {@link Import @Import} to create any collaborators required by your
   70 |  * {@code @Controller} beans.
   71 |  * <p>
   72 |  * If you are looking to load your full application configuration and use MockMVC, you
   73 |  * should consider {@link SpringBootTest @SpringBootTest} combined with
   74 |  * {@link AutoConfigureMockMvc @AutoConfigureMockMvc} rather than this annotation.
   75 |  * <p>
   76 |  * When using JUnit 4, this annotation should be used in combination with
   77 |  * {@code @RunWith(SpringRunner.class)}.
   78 |  *
   79 |  * @author Phillip Webb
   80 |  * @author Artsiom Yudovin
   81 |  * @see AutoConfigureWebMvc
   82 |  * @see AutoConfigureMockMvc
   83 |  * @since 4.0.0
   84 |  */
   85 | @Target(ElementType.TYPE)
   86 | @Retention(RetentionPolicy.RUNTIME)
   87 | @Documented
   88 | @Inherited
   89 | @BootstrapWith(WebMvcTestContextBootstrapper.class)
   90 | @ExtendWith(SpringExtension.class)
   91 | @OverrideAutoConfiguration(enabled = false)
   92 | @TypeExcludeFilters(WebMvcTypeExcludeFilter.class)
   93 | @AutoConfigureWebMvc
   94 | @AutoConfigureMockMvc
   95 | @ImportAutoConfiguration
   96 | public @interface WebMvcTest {
   97 | 
   98 | 	/**
   99 | 	 * Properties in form {@literal key=value} that should be added to the Spring
  100 | 	 * {@link Environment} before the test runs.
  101 | 	 * @return the properties to add
  102 | 	 */
  103 | 	String[] properties() default {};
  104 | 
  105 | 	/**
  106 | 	 * Specifies the controllers to test. This is an alias of {@link #controllers()} which
  107 | 	 * can be used for brevity if no other attributes are defined. See
  108 | 	 * {@link #controllers()} for details.
  109 | 	 * @see #controllers()
  110 | 	 * @return the controllers to test
  111 | 	 */
  112 | 	@AliasFor("controllers")
  113 | 	Class<?>[] value() default {};
  114 | 
  115 | 	/**
  116 | 	 * Specifies the controllers to test. May be left blank if all {@code @Controller}
  117 | 	 * beans should be added to the application context.
  118 | 	 * @see #value()
  119 | 	 * @return the controllers to test
  120 | 	 */
  121 | 	@AliasFor("value")
  122 | 	Class<?>[] controllers() default {};
  123 | 
  124 | 	/**
  125 | 	 * Determines if default filtering should be used with
  126 | 	 * {@link SpringBootApplication @SpringBootApplication}. By default only
  127 | 	 * {@code @Controller} (when no explicit {@link #controllers() controllers} are
  128 | 	 * defined), {@code @ControllerAdvice} and {@code WebMvcConfigurer} beans are
  129 | 	 * included.
  130 | 	 * @see #includeFilters()
  131 | 	 * @see #excludeFilters()
  132 | 	 * @return if default filters should be used
  133 | 	 */
  134 | 	boolean useDefaultFilters() default true;
  135 | 
  136 | 	/**
  137 | 	 * A set of include filters which can be used to add otherwise filtered beans to the
  138 | 	 * application context.
  139 | 	 * @return include filters to apply
  140 | 	 */
  141 | 	Filter[] includeFilters() default {};
  142 | 
  143 | 	/**
  144 | 	 * A set of exclude filters which can be used to filter beans that would otherwise be
  145 | 	 * added to the application context.
  146 | 	 * @return exclude filters to apply
  147 | 	 */
  148 | 	Filter[] excludeFilters() default {};
  149 | 
  150 | 	/**
  151 | 	 * Auto-configuration exclusions that should be applied for this test.
  152 | 	 * @return auto-configuration exclusions to apply
  153 | 	 */
  154 | 	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
  155 | 	Class<?>[] excludeAutoConfiguration() default {};
  156 | 
  157 | }
```
