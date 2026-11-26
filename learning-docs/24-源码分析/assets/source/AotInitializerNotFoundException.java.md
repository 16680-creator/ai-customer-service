<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/AotInitializerNotFoundException.java -->
<!-- 正文引用：[[源码:AotInitializerNotFoundException.java#L起-L止]] ；代码块头部：(src: AotInitializerNotFoundException.java#L起-L止) -->

# AotInitializerNotFoundException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/AotInitializerNotFoundException.java` |
| 行数 | 27 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.764Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | /**
    8 |  * Exception thrown when the AOT initializer couldn't be found.
    9 |  *
   10 |  * @author Moritz Halbritter
   11 |  * @since 3.2.6
   12 |  */
   13 | public class AotInitializerNotFoundException extends RuntimeException {
   14 | 
   15 | 	private final Class<?> mainClass;
   16 | 
   17 | 	public AotInitializerNotFoundException(Class<?> mainClass, String initializerClassName) {
   18 | 		super("Startup with AOT mode enabled failed: AOT initializer %s could not be found"
   19 | 			.formatted(initializerClassName));
   20 | 		this.mainClass = mainClass;
   21 | 	}
   22 | 
   23 | 	public Class<?> getMainClass() {
   24 | 		return this.mainClass;
   25 | 	}
   26 | 
   27 | }
```
