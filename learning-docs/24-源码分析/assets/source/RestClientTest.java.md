<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-restclient-test:4.2.0 | path: org/springframework/boot/restclient/test/autoconfigure/RestClientTest.java -->
<!-- 正文引用：[[源码:RestClientTest.java#L起-L止]] ；代码块头部：(src: RestClientTest.java#L起-L止) -->

# RestClientTest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-restclient-test:4.2.0` |
| 路径 | `org/springframework/boot/restclient/test/autoconfigure/RestClientTest.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-restclient-test/4.2.0/spring-boot-restclient-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:24:14.480Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.restclient.test.autoconfigure;
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
   18 | import org.springframework.boot.restclient.RestTemplateBuilder;
   19 | import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
   20 | import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
   21 | import org.springframework.context.annotation.ComponentScan;
   22 | import org.springframework.core.annotation.AliasFor;
   23 | import org.springframework.core.env.Environment;
   24 | import org.springframework.test.context.BootstrapWith;
   25 | import org.springframework.test.context.junit.jupiter.SpringExtension;
   26 | import org.springframework.test.web.client.MockRestServiceServer;
   27 | import org.springframework.web.client.RestClient.Builder;
   28 | 
   29 | /**
   30 |  * Annotation for a Spring rest client test that focuses <strong>only</strong> on beans
   31 |  * that use {@link RestTemplateBuilder} or {@link Builder RestClient.Builder}.
   32 |  * <p>
   33 |  * Using this annotation only enables auto-configuration that is relevant to rest client
   34 |  * tests. Similarly, component scanning is limited to beans annotated with:
   35 |  * <ul>
   36 |  * <li>{@code @JacksonComponent}</li>
   37 |  * <li>{@code @JsonComponent}(deprecated)</li>
   38 |  * </ul>
   39 |  * <p>
   40 |  * as well as beans that implement:
   41 |  * <ul>
   42 |  * <li>{@code JacksonModule}, if Jackson is available</li>
   43 |  * <li>{@code Module}, if Jackson 2 is available (deprecated)</li>
   44 |  * </ul>
   45 |  * <p>
   46 |  * By default, tests annotated with {@code RestClientTest} will also auto-configure a
   47 |  * {@link MockRestServiceServer}. For more fine-grained control the
   48 |  * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer}
   49 |  * annotation can be used.
   50 |  * <p>
   51 |  * When using JUnit 4, this annotation should be used in combination with
   52 |  * {@code @RunWith(SpringRunner.class)}.
   53 |  *
   54 |  * @author Stephane Nicoll
   55 |  * @author Phillip Webb
   56 |  * @author Artsiom Yudovin
   57 |  * @since 4.0.0
   58 |  */
   59 | @Target(ElementType.TYPE)
   60 | @Retention(RetentionPolicy.RUNTIME)
   61 | @Documented
   62 | @Inherited
   63 | @BootstrapWith(RestClientTestContextBootstrapper.class)
   64 | @ExtendWith(SpringExtension.class)
   65 | @OverrideAutoConfiguration(enabled = false)
   66 | @TypeExcludeFilters(RestClientTypeExcludeFilter.class)
   67 | @AutoConfigureMockRestServiceServer
   68 | @AutoConfigureRestClient
   69 | @ImportAutoConfiguration
   70 | public @interface RestClientTest {
   71 | 
   72 | 	/**
   73 | 	 * Properties in form {@literal key=value} that should be added to the Spring
   74 | 	 * {@link Environment} before the test runs.
   75 | 	 * @return the properties to add
   76 | 	 */
   77 | 	String[] properties() default {};
   78 | 
   79 | 	/**
   80 | 	 * Specifies the components to test. This is an alias of {@link #components()} which
   81 | 	 * can be used for brevity if no other attributes are defined. See
   82 | 	 * {@link #components()} for details.
   83 | 	 * @see #components()
   84 | 	 * @return the components to test
   85 | 	 */
   86 | 	@AliasFor("components")
   87 | 	Class<?>[] value() default {};
   88 | 
   89 | 	/**
   90 | 	 * Specifies the components to test. May be left blank if components will be manually
   91 | 	 * imported or created directly.
   92 | 	 * @see #value()
   93 | 	 * @return the components to test
   94 | 	 */
   95 | 	@AliasFor("value")
   96 | 	Class<?>[] components() default {};
   97 | 
   98 | 	/**
   99 | 	 * Determines if default filtering should be used with
  100 | 	 * {@link SpringBootApplication @SpringBootApplication}. By default, only
  101 | 	 * {@code @JacksonComponent} and {@code JacksonModule} beans are included.
  102 | 	 * @see #includeFilters()
  103 | 	 * @see #excludeFilters()
  104 | 	 * @return if default filters should be used
  105 | 	 */
  106 | 	boolean useDefaultFilters() default true;
  107 | 
  108 | 	/**
  109 | 	 * A set of include filters which can be used to add otherwise filtered beans to the
  110 | 	 * application context.
  111 | 	 * @return include filters to apply
  112 | 	 */
  113 | 	ComponentScan.Filter[] includeFilters() default {};
  114 | 
  115 | 	/**
  116 | 	 * A set of exclude filters which can be used to filter beans that would otherwise be
  117 | 	 * added to the application context.
  118 | 	 * @return exclude filters to apply
  119 | 	 */
  120 | 	ComponentScan.Filter[] excludeFilters() default {};
  121 | 
  122 | 	/**
  123 | 	 * Auto-configuration exclusions that should be applied for this test.
  124 | 	 * @return auto-configuration exclusions to apply
  125 | 	 */
  126 | 	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
  127 | 	Class<?>[] excludeAutoConfiguration() default {};
  128 | 
  129 | }
```
