<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-resttestclient:4.2.0 | path: org/springframework/boot/resttestclient/autoconfigure/AutoConfigureRestTestClient.java -->
<!-- 正文引用：[[源码:AutoConfigureRestTestClient.java#L起-L止]] ；代码块头部：(src: AutoConfigureRestTestClient.java#L起-L止) -->

# AutoConfigureRestTestClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-resttestclient:4.2.0` |
| 路径 | `org/springframework/boot/resttestclient/autoconfigure/AutoConfigureRestTestClient.java` |
| 行数 | 30 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-resttestclient/4.2.0/spring-boot-resttestclient-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:24:29.060Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.resttestclient.autoconfigure;
    6 | 
    7 | import java.lang.annotation.Documented;
    8 | import java.lang.annotation.ElementType;
    9 | import java.lang.annotation.Inherited;
   10 | import java.lang.annotation.Retention;
   11 | import java.lang.annotation.RetentionPolicy;
   12 | import java.lang.annotation.Target;
   13 | 
   14 | import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
   15 | import org.springframework.test.web.servlet.client.RestTestClient;
   16 | 
   17 | /**
   18 |  * Annotation that can be applied to a test class to enable a {@link RestTestClient}.
   19 |  *
   20 |  * @author Stephane Nicoll
   21 |  * @since 4.0.0
   22 |  */
   23 | @Target({ ElementType.TYPE, ElementType.METHOD })
   24 | @Retention(RetentionPolicy.RUNTIME)
   25 | @Documented
   26 | @Inherited
   27 | @ImportAutoConfiguration
   28 | public @interface AutoConfigureRestTestClient {
   29 | 
   30 | }
```
