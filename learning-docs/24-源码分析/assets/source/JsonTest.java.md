<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test-autoconfigure:4.2.0 | path: org/springframework/boot/test/autoconfigure/json/JsonTest.java -->
<!-- 正文引用：[[源码:JsonTest.java#L起-L止]] ；代码块头部：(src: JsonTest.java#L起-L止) -->

# JsonTest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/test/autoconfigure/json/JsonTest.java` |
| 行数 | 108 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test-autoconfigure/4.2.0/spring-boot-test-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.439Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.autoconfigure.json;
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
   19 | import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
   20 | import org.springframework.boot.test.json.GsonTester;
   21 | import org.springframework.boot.test.json.JacksonTester;
   22 | import org.springframework.boot.test.json.JsonbTester;
   23 | import org.springframework.context.annotation.ComponentScan.Filter;
   24 | import org.springframework.core.annotation.AliasFor;
   25 | import org.springframework.core.env.Environment;
   26 | import org.springframework.test.context.BootstrapWith;
   27 | import org.springframework.test.context.junit.jupiter.SpringExtension;
   28 | 
   29 | /**
   30 |  * Annotation for a JSON test that focuses <strong>only</strong> on JSON serialization.
   31 |  * <p>
   32 |  * Using this annotation only enables auto-configuration that is relevant to JSON tests.
   33 |  * Similarly, component scanning is limited to beans annotated with:
   34 |  * <ul>
   35 |  * <li>{@code @JacksonComponent}</li>
   36 |  * </ul>
   37 |  * <p>
   38 |  * as well as beans that implement:
   39 |  * <ul>
   40 |  * <li>{@code JacksonModule}, if Jackson is available</li>
   41 |  * </ul>
   42 |  * <p>
   43 |  * By default, tests annotated with {@code JsonTest} will also initialize
   44 |  * {@link JacksonTester}, {@link JsonbTester} and {@link GsonTester} fields. More
   45 |  * fine-grained control can be provided through the
   46 |  * {@link AutoConfigureJsonTesters @AutoConfigureJsonTesters} annotation.
   47 |  * <p>
   48 |  * When using JUnit 4, this annotation should be used in combination with
   49 |  * {@code @RunWith(SpringRunner.class)}.
   50 |  *
   51 |  * @author Phillip Webb
   52 |  * @author Artsiom Yudovin
   53 |  * @see AutoConfigureJson
   54 |  * @see AutoConfigureJsonTesters
   55 |  * @since 1.4.0
   56 |  */
   57 | @Target(ElementType.TYPE)
   58 | @Retention(RetentionPolicy.RUNTIME)
   59 | @Documented
   60 | @Inherited
   61 | @BootstrapWith(JsonTestContextBootstrapper.class)
   62 | @ExtendWith(SpringExtension.class)
   63 | @OverrideAutoConfiguration(enabled = false)
   64 | @TypeExcludeFilters(JsonTypeExcludeFilter.class)
   65 | @AutoConfigureJsonTesters
   66 | @ImportAutoConfiguration
   67 | public @interface JsonTest {
   68 | 
   69 | 	/**
   70 | 	 * Properties in form {@literal key=value} that should be added to the Spring
   71 | 	 * {@link Environment} before the test runs.
   72 | 	 * @return the properties to add
   73 | 	 * @since 2.1.0
   74 | 	 */
   75 | 	String[] properties() default {};
   76 | 
   77 | 	/**
   78 | 	 * Determines if default filtering should be used with
   79 | 	 * {@link SpringBootApplication @SpringBootApplication}. By default, only
   80 | 	 * {@code @JacksonComponent} and {@code JacksonModule} beans are included.
   81 | 	 * @see #includeFilters()
   82 | 	 * @see #excludeFilters()
   83 | 	 * @return if default filters should be used
   84 | 	 */
   85 | 	boolean useDefaultFilters() default true;
   86 | 
   87 | 	/**
   88 | 	 * A set of include filters which can be used to add otherwise filtered beans to the
   89 | 	 * application context.
   90 | 	 * @return include filters to apply
   91 | 	 */
   92 | 	Filter[] includeFilters() default {};
   93 | 
   94 | 	/**
   95 | 	 * A set of exclude filters which can be used to filter beans that would otherwise be
   96 | 	 * added to the application context.
   97 | 	 * @return exclude filters to apply
   98 | 	 */
   99 | 	Filter[] excludeFilters() default {};
  100 | 
  101 | 	/**
  102 | 	 * Auto-configuration exclusions that should be applied for this test.
  103 | 	 * @return auto-configuration exclusions to apply
  104 | 	 */
  105 | 	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
  106 | 	Class<?>[] excludeAutoConfiguration() default {};
  107 | 
  108 | }
```
