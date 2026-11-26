<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-data-jpa-test:4.2.0 | path: org/springframework/boot/data/jpa/test/autoconfigure/DataJpaTest.java -->
<!-- 正文引用：[[源码:DataJpaTest.java#L起-L止]] ；代码块头部：(src: DataJpaTest.java#L起-L止) -->

# DataJpaTest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-data-jpa-test:4.2.0` |
| 路径 | `org/springframework/boot/data/jpa/test/autoconfigure/DataJpaTest.java` |
| 行数 | 135 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-data-jpa-test/4.2.0/spring-boot-data-jpa-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:38:15.611Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.data.jpa.test.autoconfigure;
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
   18 | import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc;
   19 | import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
   20 | import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
   21 | import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
   22 | import org.springframework.boot.test.context.PropertyMapping;
   23 | import org.springframework.boot.test.context.SpringBootTest;
   24 | import org.springframework.boot.test.context.filter.annotation.TypeExcludeFilters;
   25 | import org.springframework.context.annotation.ComponentScan.Filter;
   26 | import org.springframework.core.annotation.AliasFor;
   27 | import org.springframework.core.env.Environment;
   28 | import org.springframework.data.repository.config.BootstrapMode;
   29 | import org.springframework.test.context.BootstrapWith;
   30 | import org.springframework.test.context.junit.jupiter.SpringExtension;
   31 | import org.springframework.transaction.annotation.Transactional;
   32 | 
   33 | /**
   34 |  * Annotation for a JPA test that focuses <strong>only</strong> on JPA components.
   35 |  * <p>
   36 |  * Using this annotation only enables auto-configuration that is relevant to Data JPA
   37 |  * tests. Similarly, component scanning is limited to JPA repositories and entities
   38 |  * ({@code @Entity}).
   39 |  * <p>
   40 |  * By default, tests annotated with {@code @DataJpaTest} are transactional and roll back
   41 |  * at the end of each test. They also use an embedded in-memory database (replacing any
   42 |  * explicit or usually auto-configured DataSource). The
   43 |  * {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} annotation can be used to
   44 |  * override these settings.
   45 |  * <p>
   46 |  * SQL queries are logged by default by setting the {@code spring.jpa.show-sql} property
   47 |  * to {@code true}. This can be disabled using the {@link DataJpaTest#showSql() showSql}
   48 |  * attribute.
   49 |  * <p>
   50 |  * If you are looking to load your full application configuration, but use an embedded
   51 |  * database, you should consider {@link SpringBootTest @SpringBootTest} combined with
   52 |  * {@link AutoConfigureTestDatabase @AutoConfigureTestDatabase} rather than this
   53 |  * annotation.
   54 |  * <p>
   55 |  * When using JUnit 4, this annotation should be used in combination with
   56 |  * {@code @RunWith(SpringRunner.class)}.
   57 |  *
   58 |  * @author Phillip Webb
   59 |  * @author Artsiom Yudovin
   60 |  * @author Scott Frederick
   61 |  * @since 4.0.0
   62 |  * @see AutoConfigureDataJpa
   63 |  * @see AutoConfigureTestDatabase
   64 |  * @see AutoConfigureTestEntityManager
   65 |  */
   66 | @Target(ElementType.TYPE)
   67 | @Retention(RetentionPolicy.RUNTIME)
   68 | @Documented
   69 | @Inherited
   70 | @BootstrapWith(DataJpaTestContextBootstrapper.class)
   71 | @ExtendWith(SpringExtension.class)
   72 | @OverrideAutoConfiguration(enabled = false)
   73 | @TypeExcludeFilters(DataJpaTypeExcludeFilter.class)
   74 | @Transactional
   75 | @AutoConfigureDataJpa
   76 | @AutoConfigureJdbc
   77 | @AutoConfigureTestDatabase
   78 | @AutoConfigureTestEntityManager
   79 | @ImportAutoConfiguration
   80 | public @interface DataJpaTest {
   81 | 
   82 | 	/**
   83 | 	 * Properties in form {@literal key=value} that should be added to the Spring
   84 | 	 * {@link Environment} before the test runs.
   85 | 	 * @return the properties to add
   86 | 	 */
   87 | 	String[] properties() default {};
   88 | 
   89 | 	/**
   90 | 	 * If SQL output should be logged.
   91 | 	 * @return if SQL is logged
   92 | 	 */
   93 | 	@PropertyMapping("spring.jpa.show-sql")
   94 | 	boolean showSql() default true;
   95 | 
   96 | 	/**
   97 | 	 * The {@link BootstrapMode} for the test repository support. Defaults to
   98 | 	 * {@link BootstrapMode#DEFAULT}.
   99 | 	 * @return the {@link BootstrapMode} to use for testing the repository
  100 | 	 */
  101 | 	@PropertyMapping("spring.data.jpa.repositories.bootstrap-mode")
  102 | 	BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;
  103 | 
  104 | 	/**
  105 | 	 * Determines if default filtering should be used with
  106 | 	 * {@link SpringBootApplication @SpringBootApplication}. By default no beans are
  107 | 	 * included.
  108 | 	 * @see #includeFilters()
  109 | 	 * @see #excludeFilters()
  110 | 	 * @return if default filters should be used
  111 | 	 */
  112 | 	boolean useDefaultFilters() default true;
  113 | 
  114 | 	/**
  115 | 	 * A set of include filters which can be used to add otherwise filtered beans to the
  116 | 	 * application context.
  117 | 	 * @return include filters to apply
  118 | 	 */
  119 | 	Filter[] includeFilters() default {};
  120 | 
  121 | 	/**
  122 | 	 * A set of exclude filters which can be used to filter beans that would otherwise be
  123 | 	 * added to the application context.
  124 | 	 * @return exclude filters to apply
  125 | 	 */
  126 | 	Filter[] excludeFilters() default {};
  127 | 
  128 | 	/**
  129 | 	 * Auto-configuration exclusions that should be applied for this test.
  130 | 	 * @return auto-configuration exclusions to apply
  131 | 	 */
  132 | 	@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
  133 | 	Class<?>[] excludeAutoConfiguration() default {};
  134 | 
  135 | }
```
