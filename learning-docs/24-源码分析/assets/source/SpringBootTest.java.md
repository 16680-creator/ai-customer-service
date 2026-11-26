<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/SpringBootTest.java -->
<!-- 正文引用：[[源码:SpringBootTest.java#L起-L止]] ；代码块头部：(src: SpringBootTest.java#L起-L止) -->

# SpringBootTest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/SpringBootTest.java` |
| 行数 | 202 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:56.696Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context;
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
   16 | import org.springframework.boot.ApplicationArguments;
   17 | import org.springframework.boot.SpringApplication;
   18 | import org.springframework.boot.SpringBootConfiguration;
   19 | import org.springframework.boot.WebApplicationType;
   20 | import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
   21 | import org.springframework.context.ApplicationContext;
   22 | import org.springframework.context.annotation.Configuration;
   23 | import org.springframework.core.annotation.AliasFor;
   24 | import org.springframework.core.env.Environment;
   25 | import org.springframework.test.context.BootstrapWith;
   26 | import org.springframework.test.context.ContextConfiguration;
   27 | import org.springframework.test.context.ContextLoader;
   28 | import org.springframework.test.context.junit.jupiter.SpringExtension;
   29 | import org.springframework.web.context.WebApplicationContext;
   30 | 
   31 | /**
   32 |  * Annotation that can be specified on a test class that runs Spring Boot based tests.
   33 |  * Provides the following features over and above the regular <em>Spring TestContext
   34 |  * Framework</em>:
   35 |  * <ul>
   36 |  * <li>Uses {@link SpringBootContextLoader} as the default {@link ContextLoader} when no
   37 |  * specific {@link ContextConfiguration#loader() @ContextConfiguration(loader=...)} is
   38 |  * defined.</li>
   39 |  * <li>Automatically searches for a
   40 |  * {@link SpringBootConfiguration @SpringBootConfiguration} when nested
   41 |  * {@code @Configuration} is not used, and no explicit {@link #classes() classes} are
   42 |  * specified.</li>
   43 |  * <li>Allows custom {@link Environment} properties to be defined using the
   44 |  * {@link #properties() properties attribute}.</li>
   45 |  * <li>Allows application arguments to be defined using the {@link #args() args
   46 |  * attribute}.</li>
   47 |  * <li>Provides support for different {@link #webEnvironment() webEnvironment} modes,
   48 |  * including the ability to start a fully running web server listening on a
   49 |  * {@link WebEnvironment#DEFINED_PORT defined} or {@link WebEnvironment#RANDOM_PORT
   50 |  * random} port when {@code spring-boot-web-server} is on the classpath.</li>
   51 |  * </ul>
   52 |  *
   53 |  * @author Phillip Webb
   54 |  * @author Andy Wilkinson
   55 |  * @since 1.4.0
   56 |  * @see ContextConfiguration
   57 |  */
   58 | @Target(ElementType.TYPE)
   59 | @Retention(RetentionPolicy.RUNTIME)
   60 | @Documented
   61 | @Inherited
   62 | @BootstrapWith(SpringBootTestContextBootstrapper.class)
   63 | @ExtendWith(SpringExtension.class)
   64 | public @interface SpringBootTest {
   65 | 
   66 | 	/**
   67 | 	 * Alias for {@link #properties()}.
   68 | 	 * @return the properties to apply
   69 | 	 */
   70 | 	@AliasFor("properties")
   71 | 	String[] value() default {};
   72 | 
   73 | 	/**
   74 | 	 * Properties in form {@literal key=value} that should be added to the Spring
   75 | 	 * {@link Environment} before the test runs.
   76 | 	 * @return the properties to add
   77 | 	 */
   78 | 	@AliasFor("value")
   79 | 	String[] properties() default {};
   80 | 
   81 | 	/**
   82 | 	 * Application arguments that should be passed to the application under test.
   83 | 	 * @return the application arguments to pass to the application under test.
   84 | 	 * @see ApplicationArguments
   85 | 	 * @see SpringApplication#run(String...)
   86 | 	 * @since 2.2.0
   87 | 	 */
   88 | 	String[] args() default {};
   89 | 
   90 | 	/**
   91 | 	 * The <em>component classes</em> to use for loading an
   92 | 	 * {@link org.springframework.context.ApplicationContext ApplicationContext}. Can also
   93 | 	 * be specified using
   94 | 	 * {@link ContextConfiguration#classes() @ContextConfiguration(classes=...)}. If no
   95 | 	 * explicit classes are defined the test will look for nested
   96 | 	 * {@link Configuration @Configuration} classes, before falling back to a
   97 | 	 * {@link SpringBootConfiguration @SpringBootConfiguration} search.
   98 | 	 * @see ContextConfiguration#classes()
   99 | 	 * @return the component classes used to load the application context
  100 | 	 */
  101 | 	Class<?>[] classes() default {};
  102 | 
  103 | 	/**
  104 | 	 * The type of web environment to create when applicable. Defaults to
  105 | 	 * {@link WebEnvironment#MOCK}.
  106 | 	 * @return the type of web environment
  107 | 	 */
  108 | 	WebEnvironment webEnvironment() default WebEnvironment.MOCK;
  109 | 
  110 | 	/**
  111 | 	 * The type of main method usage to employ when creating the {@link SpringApplication}
  112 | 	 * under test.
  113 | 	 * @return the type of main method usage
  114 | 	 * @since 3.0.0
  115 | 	 */
  116 | 	UseMainMethod useMainMethod() default UseMainMethod.NEVER;
  117 | 
  118 | 	/**
  119 | 	 * An enumeration of web environment modes.
  120 | 	 */
  121 | 	enum WebEnvironment {
  122 | 
  123 | 		/**
  124 | 		 * Creates a {@link WebApplicationContext} with a mock servlet environment if
  125 | 		 * servlet APIs are on the classpath, a {@link ReactiveWebApplicationContext} if
  126 | 		 * Spring WebFlux is on the classpath or a regular {@link ApplicationContext}
  127 | 		 * otherwise.
  128 | 		 */
  129 | 		MOCK(false),
  130 | 
  131 | 		/**
  132 | 		 * Creates a web application context (reactive or servlet based) and sets a
  133 | 		 * {@code server.port=0} {@link Environment} property (which usually triggers
  134 | 		 * listening on a random port). Requires a dependency on
  135 | 		 * {@code spring-boot-web-server}. Often used in conjunction with a
  136 | 		 * {@code @LocalServerPort} injected field on the test.
  137 | 		 */
  138 | 		RANDOM_PORT(true),
  139 | 
  140 | 		/**
  141 | 		 * Creates a web application context (reactive or servlet based) without defining
  142 | 		 * any {@code server.port=0} {@link Environment} property. Requires a dependency
  143 | 		 * on {@code spring-boot-web-server}.
  144 | 		 */
  145 | 		DEFINED_PORT(true),
  146 | 
  147 | 		/**
  148 | 		 * Creates an {@link ApplicationContext} and sets
  149 | 		 * {@link SpringApplication#setWebApplicationType(WebApplicationType)} to
  150 | 		 * {@link WebApplicationType#NONE}.
  151 | 		 */
  152 | 		NONE(false);
  153 | 
  154 | 		private final boolean embedded;
  155 | 
  156 | 		WebEnvironment(boolean embedded) {
  157 | 			this.embedded = embedded;
  158 | 		}
  159 | 
  160 | 		/**
  161 | 		 * Return if the environment uses an embedded web server.
  162 | 		 * @return if an embedded web server is used
  163 | 		 */
  164 | 		public boolean isEmbedded() {
  165 | 			return this.embedded;
  166 | 		}
  167 | 
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Enumeration of how the main method of the
  172 | 	 * {@link SpringBootConfiguration @SpringBootConfiguration}-annotated class is used
  173 | 	 * when creating and running the {@link SpringApplication} under test.
  174 | 	 *
  175 | 	 * @since 3.0.0
  176 | 	 */
  177 | 	enum UseMainMethod {
  178 | 
  179 | 		/**
  180 | 		 * Always use the {@code main} method. A failure will occur if there is no
  181 | 		 * {@link SpringBootConfiguration @SpringBootConfiguration}-annotated class or
  182 | 		 * that class does not have a main method.
  183 | 		 */
  184 | 		ALWAYS,
  185 | 
  186 | 		/**
  187 | 		 * Never use the {@code main} method, creating a test-specific
  188 | 		 * {@link SpringApplication} instead.
  189 | 		 */
  190 | 		NEVER,
  191 | 
  192 | 		/**
  193 | 		 * Use the {@code main} method when it is available. If there is no
  194 | 		 * {@link SpringBootConfiguration @SpringBootConfiguration}-annotated class or
  195 | 		 * that class does not have a main method, a test-specific
  196 | 		 * {@link SpringApplication} will be used.
  197 | 		 */
  198 | 		WHEN_AVAILABLE
  199 | 
  200 | 	}
  201 | 
  202 | }
```
