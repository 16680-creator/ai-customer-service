<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/EnableAutoConfiguration.java -->
<!-- 正文引用：[[源码:EnableAutoConfiguration.java#L起-L止]] ；代码块头部：(src: EnableAutoConfiguration.java#L起-L止) -->

# EnableAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/EnableAutoConfiguration.java` |
| 行数 | 94 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:06.171Z |

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
   14 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   15 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   16 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   17 | import org.springframework.boot.context.annotation.ImportCandidates;
   18 | import org.springframework.context.annotation.Conditional;
   19 | import org.springframework.context.annotation.Configuration;
   20 | import org.springframework.context.annotation.Import;
   21 | 
   22 | /**
   23 |  * Enable auto-configuration of the Spring Application Context, attempting to guess and
   24 |  * configure beans that you are likely to need. Auto-configuration classes are usually
   25 |  * applied based on your classpath and what beans you have defined.
   26 |  * <p>
   27 |  * When using {@link SpringBootApplication @SpringBootApplication}, the auto-configuration
   28 |  * of the context is automatically enabled and adding this annotation has therefore no
   29 |  * additional effect.
   30 |  * <p>
   31 |  * Auto-configuration tries to be as intelligent as possible and will back-away as you
   32 |  * define more of your own configuration. You can always manually {@link #exclude()} any
   33 |  * configuration that you never want to apply (use {@link #excludeName()} if you don't
   34 |  * have access to them). You can also exclude them through the
   35 |  * {@code spring.autoconfigure.exclude} property. Auto-configuration is always applied
   36 |  * after user-defined beans have been registered.
   37 |  * <p>
   38 |  * The package of the class that is annotated with {@code @EnableAutoConfiguration},
   39 |  * usually through {@code @SpringBootApplication}, has specific significance and is often
   40 |  * used as a 'default'. For example, it will be used when scanning for {@code @Entity}
   41 |  * classes. It is generally recommended that you place {@code @EnableAutoConfiguration}
   42 |  * (if you're not using {@code @SpringBootApplication}) in a root package so that all
   43 |  * sub-packages and classes can be searched.
   44 |  * <p>
   45 |  * Auto-configuration classes are regular Spring {@link Configuration @Configuration}
   46 |  * beans. They are located using {@link ImportCandidates}. Generally auto-configuration
   47 |  * beans are {@link Conditional @Conditional} beans (most often using
   48 |  * {@link ConditionalOnClass @ConditionalOnClass} and
   49 |  * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
   50 |  *
   51 |  * @author Phillip Webb
   52 |  * @author Stephane Nicoll
   53 |  * @since 1.0.0
   54 |  * @see ConditionalOnBean
   55 |  * @see ConditionalOnMissingBean
   56 |  * @see ConditionalOnClass
   57 |  * @see AutoConfigureAfter
   58 |  * @see SpringBootApplication
   59 |  */
   60 | @Target(ElementType.TYPE)
   61 | @Retention(RetentionPolicy.RUNTIME)
   62 | @Documented
   63 | @Inherited
   64 | @AutoConfigurationPackage
   65 | @Import(AutoConfigurationImportSelector.class)
   66 | public @interface EnableAutoConfiguration {
   67 | 
   68 | 	/**
   69 | 	 * Environment property that can be used to override when auto-configuration is
   70 | 	 * enabled.
   71 | 	 */
   72 | 	String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";
   73 | 
   74 | 	/**
   75 | 	 * Exclude specific auto-configuration classes such that they will never be applied.
   76 | 	 * <p>
   77 | 	 * Since this annotation is parsed by loading class bytecode, it is safe to specify
   78 | 	 * classes here that may ultimately not be on the classpath, but only if this
   79 | 	 * annotation is directly on the affected component and <b>not</b> if this annotation
   80 | 	 * is used as a composed, meta-annotation. In order to use this annotation as a
   81 | 	 * meta-annotation, only use the {@link #excludeName()} attribute.
   82 | 	 * @return the classes to exclude
   83 | 	 */
   84 | 	Class<?>[] exclude() default {};
   85 | 
   86 | 	/**
   87 | 	 * Exclude specific auto-configuration class names such that they will never be
   88 | 	 * applied.
   89 | 	 * @return the class names to exclude
   90 | 	 * @since 1.3.0
   91 | 	 */
   92 | 	String[] excludeName() default {};
   93 | 
   94 | }
```
