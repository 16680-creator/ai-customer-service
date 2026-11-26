<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/contributor/HealthContributor.java -->
<!-- 正文引用：[[源码:HealthContributor.java#L起-L止]] ；代码块头部：(src: HealthContributor.java#L起-L止) -->

# HealthContributor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/contributor/HealthContributor.java` |
| 行数 | 18 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.869Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.contributor;
    6 | 
    7 | /**
    8 |  * Contributes health information, either directly ({@link HealthIndicator}) or via other
    9 |  * contributors ({@link CompositeHealthContributor}).
   10 |  *
   11 |  * @author Phillip Webb
   12 |  * @since 4.0.0
   13 |  * @see HealthIndicator
   14 |  * @see CompositeHealthContributor
   15 |  */
   16 | public sealed interface HealthContributor permits HealthIndicator, CompositeHealthContributor {
   17 | 
   18 | }
```
