<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/SpringBootApplication.java -->
<!-- 正文引用：[[源码:SpringBootApplication.java#L起-L止]] ；代码块头部：(src: SpringBootApplication.java#L起-L止) -->

# SpringBootApplication.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/SpringBootApplication.java` |
| 行数 | 142 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:07.149Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Inherited;
   10 | import java.lang.annotation.Retention;
   11 | import java.lang.annotation.RetentionPolicy;
   12 | import java.lang.annotation.Target;
   13 | 
   14 | import org.springframework.beans.factory.support.BeanNameGenerator;
   15 | import org.springframework.boot.SpringApplication;
   16 | import org.springframework.boot.SpringBootConfiguration;
   17 | import org.springframework.boot.context.TypeExcludeFilter;
   18 | import org.springframework.context.annotation.AnnotationBeanNameGenerator;
   19 | import org.springframework.context.annotation.Bean;
   20 | import org.springframework.context.annotation.ComponentScan;
   21 | import org.springframework.context.annotation.ComponentScan.Filter;
   22 | import org.springframework.context.annotation.Configuration;
   23 | import org.springframework.context.annotation.FilterType;
   24 | import org.springframework.core.annotation.AliasFor;
   25 | 
   26 | /**
   27 |  * Indicates a {@link Configuration configuration} class that declares one or more
   28 |  * {@link Bean @Bean} methods and also triggers {@link EnableAutoConfiguration
   29 |  * auto-configuration} and {@link ComponentScan component scanning}. This is a convenience
   30 |  * annotation that is equivalent to declaring {@code @SpringBootConfiguration},
   31 |  * {@code @EnableAutoConfiguration} and {@code @ComponentScan}.
   32 |  *
   33 |  * @author Phillip Webb
   34 |  * @author Stephane Nicoll
   35 |  * @author Andy Wilkinson
   36 |  * @since 1.2.0
   37 |  */
   38 | @Target(ElementType.TYPE)
   39 | @Retention(RetentionPolicy.RUNTIME)
   40 | @Documented
   41 | @Inherited
   42 | @SpringBootConfiguration
   43 | @EnableAutoConfiguration
   44 | @ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
   45 | 		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
   46 | public @interface SpringBootApplication {
   47 | 
   48 | 	/**
   49 | 	 * Exclude specific auto-configuration classes such that they will never be applied.
   50 | 	 * <p>
   51 | 	 * Since this annotation is parsed by loading class bytecode, it is safe to specify
   52 | 	 * classes here that may ultimately not be on the classpath, but only if this
   53 | 	 * annotation is directly on the affected component and <b>not</b> if this annotation
   54 | 	 * is used as a composed, meta-annotation. In order to use this annotation as a
   55 | 	 * meta-annotation, only use the {@link #excludeName} attribute.
   56 | 	 * @return the classes to exclude
   57 | 	 */
   58 | 	@AliasFor(annotation = EnableAutoConfiguration.class)
   59 | 	Class<?>[] exclude() default {};
   60 | 
   61 | 	/**
   62 | 	 * Exclude specific auto-configuration class names such that they will never be
   63 | 	 * applied.
   64 | 	 * @return the class names to exclude
   65 | 	 * @since 1.3.0
   66 | 	 */
   67 | 	@AliasFor(annotation = EnableAutoConfiguration.class)
   68 | 	String[] excludeName() default {};
   69 | 
   70 | 	/**
   71 | 	 * Base packages to scan for annotated components. Use {@link #scanBasePackageClasses}
   72 | 	 * for a type-safe alternative to String-based package names.
   73 | 	 * <p>
   74 | 	 * <strong>Note:</strong> this setting is an alias for
   75 | 	 * {@link ComponentScan @ComponentScan} only. It has no effect on {@code @Entity}
   76 | 	 * scanning or Spring Data repository scanning. For those you should add
   77 | 	 * {@code @EntityScan} and {@code @Enable...Repositories} annotations.
   78 | 	 * @return base packages to scan
   79 | 	 * @since 1.3.0
   80 | 	 */
   81 | 	@AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
   82 | 	String[] scanBasePackages() default {};
   83 | 
   84 | 	/**
   85 | 	 * Type-safe alternative to {@link #scanBasePackages} for specifying the packages to
   86 | 	 * scan for annotated components. The package of each class specified will be scanned.
   87 | 	 * <p>
   88 | 	 * Consider creating a special no-op marker class or interface in each package that
   89 | 	 * serves no purpose other than being referenced by this attribute.
   90 | 	 * <p>
   91 | 	 * <strong>Note:</strong> this setting is an alias for
   92 | 	 * {@link ComponentScan @ComponentScan} only. It has no effect on {@code @Entity}
   93 | 	 * scanning or Spring Data repository scanning. For those you should add
   94 | 	 * {@code @EntityScan} and {@code @Enable...Repositories} annotations.
   95 | 	 * @return base packages to scan
   96 | 	 * @since 1.3.0
   97 | 	 */
   98 | 	@AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
   99 | 	Class<?>[] scanBasePackageClasses() default {};
  100 | 
  101 | 	/**
  102 | 	 * The {@link BeanNameGenerator} class to be used for naming detected components
  103 | 	 * within the Spring container.
  104 | 	 * <p>
  105 | 	 * The default value of the {@link BeanNameGenerator} interface itself indicates that
  106 | 	 * the scanner used to process this {@code @SpringBootApplication} annotation should
  107 | 	 * use its inherited bean name generator, e.g. the default
  108 | 	 * {@link AnnotationBeanNameGenerator} or any custom instance supplied to the
  109 | 	 * application context at bootstrap time.
  110 | 	 * @return {@link BeanNameGenerator} to use
  111 | 	 * @see SpringApplication#setBeanNameGenerator(BeanNameGenerator)
  112 | 	 * @since 2.3.0
  113 | 	 */
  114 | 	@AliasFor(annotation = ComponentScan.class, attribute = "nameGenerator")
  115 | 	Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;
  116 | 
  117 | 	/**
  118 | 	 * Specify whether {@link Bean @Bean} methods should get proxied in order to enforce
  119 | 	 * bean lifecycle behavior, e.g. to return shared singleton bean instances even in
  120 | 	 * case of direct {@code @Bean} method calls in user code. This feature requires
  121 | 	 * method interception, implemented through a runtime-generated CGLIB subclass which
  122 | 	 * comes with limitations such as the configuration class and its methods not being
  123 | 	 * allowed to declare {@code final}.
  124 | 	 * <p>
  125 | 	 * The default is {@code true}, allowing for 'inter-bean references' within the
  126 | 	 * configuration class as well as for external calls to this configuration's
  127 | 	 * {@code @Bean} methods, e.g. from another configuration class. If this is not needed
  128 | 	 * since each of this particular configuration's {@code @Bean} methods is
  129 | 	 * self-contained and designed as a plain factory method for container use, switch
  130 | 	 * this flag to {@code false} in order to avoid CGLIB subclass processing.
  131 | 	 * <p>
  132 | 	 * Turning off bean method interception effectively processes {@code @Bean} methods
  133 | 	 * individually like when declared on non-{@code @Configuration} classes, a.k.a.
  134 | 	 * "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore behaviorally
  135 | 	 * equivalent to removing the {@code @Configuration} stereotype.
  136 | 	 * @since 2.2
  137 | 	 * @return whether to proxy {@code @Bean} methods
  138 | 	 */
  139 | 	@AliasFor(annotation = Configuration.class)
  140 | 	boolean proxyBeanMethods() default true;
  141 | 
  142 | }
```
