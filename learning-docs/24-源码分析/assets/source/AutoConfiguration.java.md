<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/AutoConfiguration.java -->
<!-- 正文引用：[[源码:AutoConfiguration.java#L起-L止]] ；代码块头部：(src: AutoConfiguration.java#L起-L止) -->

# AutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/AutoConfiguration.java` |
| 行数 | 113 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:06.857Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Retention;
   10 | import java.lang.annotation.RetentionPolicy;
   11 | import java.lang.annotation.Target;
   12 | 
   13 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   14 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   15 | import org.springframework.boot.context.annotation.ImportCandidates;
   16 | import org.springframework.context.annotation.AnnotationBeanNameGenerator;
   17 | import org.springframework.context.annotation.AnnotationConfigApplicationContext;
   18 | import org.springframework.context.annotation.Conditional;
   19 | import org.springframework.context.annotation.Configuration;
   20 | import org.springframework.core.annotation.AliasFor;
   21 | 
   22 | /**
   23 |  * Indicates that a class provides configuration that can be automatically applied by
   24 |  * Spring Boot. Auto-configuration classes are regular
   25 |  * {@link Configuration @Configuration} with the exception that
   26 |  * {@link Configuration#proxyBeanMethods() proxyBeanMethods} is always {@code false}. They
   27 |  * are located using {@link ImportCandidates}.
   28 |  * <p>
   29 |  * Generally, auto-configuration classes are top-level classes that are marked as
   30 |  * {@link Conditional @Conditional} (most often using
   31 |  * {@link ConditionalOnClass @ConditionalOnClass} and
   32 |  * {@link ConditionalOnMissingBean @ConditionalOnMissingBean} annotations).
   33 |  *
   34 |  * @author Moritz Halbritter
   35 |  * @see EnableAutoConfiguration
   36 |  * @see AutoConfigureBefore
   37 |  * @see AutoConfigureAfter
   38 |  * @see Conditional
   39 |  * @see ConditionalOnClass
   40 |  * @see ConditionalOnMissingBean
   41 |  * @since 2.7.0
   42 |  */
   43 | @Target(ElementType.TYPE)
   44 | @Retention(RetentionPolicy.RUNTIME)
   45 | @Documented
   46 | @Configuration(proxyBeanMethods = false)
   47 | @AutoConfigureBefore
   48 | @AutoConfigureAfter
   49 | public @interface AutoConfiguration {
   50 | 
   51 | 	/**
   52 | 	 * Explicitly specify the name of the Spring bean definition associated with the
   53 | 	 * {@code @AutoConfiguration} class. If left unspecified (the common case), a bean
   54 | 	 * name will be automatically generated.
   55 | 	 * <p>
   56 | 	 * The custom name applies only if the {@code @AutoConfiguration} class is picked up
   57 | 	 * through component scanning or supplied directly to an
   58 | 	 * {@link AnnotationConfigApplicationContext}. If the {@code @AutoConfiguration} class
   59 | 	 * is registered as a traditional XML bean definition, the name/id of the bean element
   60 | 	 * will take precedence.
   61 | 	 * @return the explicit component name, if any (or empty String otherwise)
   62 | 	 * @see AnnotationBeanNameGenerator
   63 | 	 */
   64 | 	@AliasFor(annotation = Configuration.class)
   65 | 	String value() default "";
   66 | 
   67 | 	/**
   68 | 	 * The auto-configuration classes that should have not yet been applied.
   69 | 	 * <p>
   70 | 	 * Since this annotation is parsed by loading class bytecode, it is safe to specify
   71 | 	 * classes here that may ultimately not be on the classpath, but only if this
   72 | 	 * annotation is directly on the affected component and <b>not</b> if this annotation
   73 | 	 * is used as a composed, meta-annotation. In order to use this annotation as a
   74 | 	 * meta-annotation, only use the {@link #beforeName} attribute.
   75 | 	 * @return the classes
   76 | 	 */
   77 | 	@AliasFor(annotation = AutoConfigureBefore.class, attribute = "value")
   78 | 	Class<?>[] before() default {};
   79 | 
   80 | 	/**
   81 | 	 * The names of the auto-configuration classes that should have not yet been applied.
   82 | 	 * In the unusual case that an auto-configuration class is not a top-level class, its
   83 | 	 * name should use {@code $} to separate it from its containing class, for example
   84 | 	 * {@code com.example.Outer$NestedAutoConfiguration}.
   85 | 	 * @return the class names
   86 | 	 */
   87 | 	@AliasFor(annotation = AutoConfigureBefore.class, attribute = "name")
   88 | 	String[] beforeName() default {};
   89 | 
   90 | 	/**
   91 | 	 * The auto-configuration classes that should have already been applied.
   92 | 	 * <p>
   93 | 	 * Since this annotation is parsed by loading class bytecode, it is safe to specify
   94 | 	 * classes here that may ultimately not be on the classpath, but only if this
   95 | 	 * annotation is directly on the affected component and <b>not</b> if this annotation
   96 | 	 * is used as a composed, meta-annotation. In order to use this annotation as a
   97 | 	 * meta-annotation, only use the {@link #afterName} attribute.
   98 | 	 * @return the classes
   99 | 	 */
  100 | 	@AliasFor(annotation = AutoConfigureAfter.class, attribute = "value")
  101 | 	Class<?>[] after() default {};
  102 | 
  103 | 	/**
  104 | 	 * The names of the auto-configuration classes that should have already been applied.
  105 | 	 * In the unusual case that an auto-configuration class is not a top-level class, its
  106 | 	 * class name should use {@code $} to separate it from its containing class, for
  107 | 	 * example {@code com.example.Outer$NestedAutoConfiguration}.
  108 | 	 * @return the class names
  109 | 	 */
  110 | 	@AliasFor(annotation = AutoConfigureAfter.class, attribute = "name")
  111 | 	String[] afterName() default {};
  112 | 
  113 | }
```
