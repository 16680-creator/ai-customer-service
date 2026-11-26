<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationProperties.java -->
<!-- 正文引用：[[源码:ConfigurationProperties.java#L起-L止]] ；代码块头部：(src: ConfigurationProperties.java#L起-L止) -->

# ConfigurationProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationProperties.java` |
| 行数 | 75 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.054Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Retention;
   10 | import java.lang.annotation.RetentionPolicy;
   11 | import java.lang.annotation.Target;
   12 | 
   13 | import org.springframework.boot.context.properties.bind.ConstructorBinding;
   14 | import org.springframework.core.annotation.AliasFor;
   15 | import org.springframework.stereotype.Indexed;
   16 | 
   17 | /**
   18 |  * Annotation for externalized configuration. Add this to a class definition or a
   19 |  * {@code @Bean} method in a {@code @Configuration} class if you want to bind and validate
   20 |  * some external Properties (e.g. from a .properties file).
   21 |  * <p>
   22 |  * Binding is either performed by calling setters on the annotated class or, if
   23 |  * {@link ConstructorBinding @ConstructorBinding} is in use, by binding to the constructor
   24 |  * parameters.
   25 |  * <p>
   26 |  * Note that contrary to {@code @Value}, SpEL expressions are not evaluated since property
   27 |  * values are externalized.
   28 |  *
   29 |  * @author Dave Syer
   30 |  * @since 1.0.0
   31 |  * @see ConfigurationPropertiesScan
   32 |  * @see ConstructorBinding
   33 |  * @see ConfigurationPropertiesBindingPostProcessor
   34 |  * @see EnableConfigurationProperties
   35 |  */
   36 | @Target({ ElementType.TYPE, ElementType.METHOD })
   37 | @Retention(RetentionPolicy.RUNTIME)
   38 | @Documented
   39 | @Indexed
   40 | public @interface ConfigurationProperties {
   41 | 
   42 | 	/**
   43 | 	 * The prefix of the properties that are valid to bind to this object. Synonym for
   44 | 	 * {@link #prefix()}. A valid prefix is defined by one or more words separated with
   45 | 	 * dots (e.g. {@code "acme.system.feature"}).
   46 | 	 * @return the prefix of the properties to bind
   47 | 	 */
   48 | 	@AliasFor("prefix")
   49 | 	String value() default "";
   50 | 
   51 | 	/**
   52 | 	 * The prefix of the properties that are valid to bind to this object. Synonym for
   53 | 	 * {@link #value()}. A valid prefix is defined by one or more words separated with
   54 | 	 * dots (e.g. {@code "acme.system.feature"}).
   55 | 	 * @return the prefix of the properties to bind
   56 | 	 */
   57 | 	@AliasFor("value")
   58 | 	String prefix() default "";
   59 | 
   60 | 	/**
   61 | 	 * Flag to indicate that when binding to this object invalid fields should be ignored.
   62 | 	 * Invalid means invalid according to the binder that is used, and usually this means
   63 | 	 * fields of the wrong type (or that cannot be coerced into the correct type).
   64 | 	 * @return the flag value (default false)
   65 | 	 */
   66 | 	boolean ignoreInvalidFields() default false;
   67 | 
   68 | 	/**
   69 | 	 * Flag to indicate that when binding to this object unknown fields should be ignored.
   70 | 	 * An unknown field could be a sign of a mistake in the Properties.
   71 | 	 * @return the flag value (default true)
   72 | 	 */
   73 | 	boolean ignoreUnknownFields() default true;
   74 | 
   75 | }
```
