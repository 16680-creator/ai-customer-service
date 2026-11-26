<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/endpoint/annotation/Endpoint.java -->
<!-- 正文引用：[[源码:Endpoint.java#L起-L止]] ；代码块头部：(src: Endpoint.java#L起-L止) -->

# Endpoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/endpoint/annotation/Endpoint.java` |
| 行数 | 61 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.197Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.endpoint.annotation;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Retention;
   10 | import java.lang.annotation.RetentionPolicy;
   11 | import java.lang.annotation.Target;
   12 | 
   13 | import org.springframework.aot.hint.annotation.Reflective;
   14 | import org.springframework.boot.actuate.endpoint.Access;
   15 | import org.springframework.boot.actuate.endpoint.EndpointId;
   16 | 
   17 | /**
   18 |  * Identifies a type as being an actuator endpoint that provides information about the
   19 |  * running application. Endpoints can be exposed over a variety of technologies including
   20 |  * JMX and HTTP.
   21 |  * <p>
   22 |  * Most {@code @Endpoint} classes will declare one or more
   23 |  * {@link ReadOperation @ReadOperation}, {@link WriteOperation @WriteOperation},
   24 |  * {@link DeleteOperation @DeleteOperation} annotated methods which will be automatically
   25 |  * adapted to the exposing technology (JMX, Spring MVC, Spring WebFlux, Jersey etc.).
   26 |  * <p>
   27 |  * {@code @Endpoint} represents the lowest common denominator for endpoints and
   28 |  * intentionally limits the sorts of operation methods that may be defined in order to
   29 |  * support the broadest possible range of exposure technologies. If you need deeper
   30 |  * support for a specific technology you can either write an endpoint that is
   31 |  * {@link FilteredEndpoint filtered} to a certain technology, or provide
   32 |  * {@link EndpointExtension extension} for the broader endpoint.
   33 |  *
   34 |  * @author Andy Wilkinson
   35 |  * @author Phillip Webb
   36 |  * @since 2.0.0
   37 |  * @see EndpointExtension
   38 |  * @see FilteredEndpoint
   39 |  * @see EndpointDiscoverer
   40 |  */
   41 | @Target(ElementType.TYPE)
   42 | @Retention(RetentionPolicy.RUNTIME)
   43 | @Documented
   44 | @Reflective
   45 | public @interface Endpoint {
   46 | 
   47 | 	/**
   48 | 	 * The id of the endpoint (must follow {@link EndpointId} rules).
   49 | 	 * @return the id
   50 | 	 * @see EndpointId
   51 | 	 */
   52 | 	String id() default "";
   53 | 
   54 | 	/**
   55 | 	 * Level of access to the endpoint that is permitted by default.
   56 | 	 * @return the default level of access
   57 | 	 * @since 3.4.0
   58 | 	 */
   59 | 	Access defaultAccess() default Access.UNRESTRICTED;
   60 | 
   61 | }
```
