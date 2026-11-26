<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jackson:4.2.0 | path: org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.java -->
<!-- 正文引用：[[源码:JsonMapperBuilderCustomizer.java#L起-L止]] ；代码块头部：(src: JsonMapperBuilderCustomizer.java#L起-L止) -->

# JsonMapperBuilderCustomizer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jackson:4.2.0` |
| 路径 | `org/springframework/boot/jackson/autoconfigure/JsonMapperBuilderCustomizer.java` |
| 行数 | 26 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jackson/4.2.0/spring-boot-jackson-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:16.698Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jackson.autoconfigure;
    6 | 
    7 | import tools.jackson.databind.json.JsonMapper;
    8 | 
    9 | /**
   10 |  * Callback interface that can be implemented by beans wishing to further customize the
   11 |  * {@link JsonMapper} through {@link tools.jackson.databind.json.JsonMapper.Builder} to
   12 |  * fine-tune its auto-configuration.
   13 |  *
   14 |  * @author Grzegorz Poznachowski
   15 |  * @since 4.0.0
   16 |  */
   17 | @FunctionalInterface
   18 | public interface JsonMapperBuilderCustomizer {
   19 | 
   20 | 	/**
   21 | 	 * Customize the JsonMapper.Builder.
   22 | 	 * @param jsonMapperBuilder the builder to customize
   23 | 	 */
   24 | 	void customize(JsonMapper.Builder jsonMapperBuilder);
   25 | 
   26 | }
```
