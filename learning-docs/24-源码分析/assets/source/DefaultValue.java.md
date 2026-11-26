<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/DefaultValue.java -->
<!-- 正文引用：[[源码:DefaultValue.java#L起-L止]] ；代码块头部：(src: DefaultValue.java#L起-L止) -->

# DefaultValue.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/DefaultValue.java` |
| 行数 | 44 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.823Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Retention;
   10 | import java.lang.annotation.RetentionPolicy;
   11 | import java.lang.annotation.Target;
   12 | 
   13 | /**
   14 |  * Annotation that can be used to specify the default value when binding to an immutable
   15 |  * property. This annotation can also be used with nested properties to indicate that a
   16 |  * value should always be bound (rather than binding {@code null}). The value from this
   17 |  * annotation will only be used if the property is not found in the property sources used
   18 |  * by the {@link Binder}. For example, if the property is present in the
   19 |  * {@link org.springframework.core.env.Environment} when binding to
   20 |  * {@link org.springframework.boot.context.properties.ConfigurationProperties @ConfigurationProperties},
   21 |  * the default value for the property will not be used even if the property value is
   22 |  * empty.
   23 |  * <p>
   24 |  * Property placeholders in the default value are resolved using the {@link Binder}'s
   25 |  * {@link PlaceholdersResolver} before the value is converted to the property's type.
   26 |  *
   27 |  * @author Madhura Bhave
   28 |  * @author Pavel Anisimov
   29 |  * @author Wan bin yu
   30 |  * @since 2.2.0
   31 |  */
   32 | @Retention(RetentionPolicy.RUNTIME)
   33 | @Target({ ElementType.PARAMETER })
   34 | @Documented
   35 | public @interface DefaultValue {
   36 | 
   37 | 	/**
   38 | 	 * The default value of the property. Can be an array of values for collection or
   39 | 	 * array-based properties.
   40 | 	 * @return the default value of the property.
   41 | 	 */
   42 | 	String[] value() default {};
   43 | 
   44 | }
```
