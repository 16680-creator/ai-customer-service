<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc-test:4.2.0 | path: org/springframework/boot/webmvc/test/autoconfigure/WebMvcTestContextBootstrapper.java -->
<!-- 正文引用：[[源码:WebMvcTestContextBootstrapper.java#L起-L止]] ；代码块头部：(src: WebMvcTestContextBootstrapper.java#L起-L止) -->

# WebMvcTestContextBootstrapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc-test:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/test/autoconfigure/WebMvcTestContextBootstrapper.java` |
| 行数 | 27 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc-test/4.2.0/spring-boot-webmvc-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:19:16.245Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.test.autoconfigure;
    6 | 
    7 | import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
    8 | import org.springframework.test.context.MergedContextConfiguration;
    9 | import org.springframework.test.context.TestContextBootstrapper;
   10 | import org.springframework.test.context.web.WebMergedContextConfiguration;
   11 | 
   12 | /**
   13 |  * {@link TestContextBootstrapper} for {@link WebMvcTest @WebMvcTest} support.
   14 |  *
   15 |  * @author Phillip Webb
   16 |  * @author Artsiom Yudovin
   17 |  * @author Lorenzo Dee
   18 |  */
   19 | class WebMvcTestContextBootstrapper extends TestSliceTestContextBootstrapper<WebMvcTest> {
   20 | 
   21 | 	@Override
   22 | 	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
   23 | 		MergedContextConfiguration processedMergedConfiguration = super.processMergedContextConfiguration(mergedConfig);
   24 | 		return new WebMergedContextConfiguration(processedMergedConfiguration, determineResourceBasePath(mergedConfig));
   25 | 	}
   26 | 
   27 | }
```
