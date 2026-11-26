<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: META-INF/spring.factories -->
<!-- 正文引用：[[源码:spring.factories#L起-L止]] ；代码块头部：(src: spring.factories#L起-L止) -->

# spring.factories

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `META-INF/spring.factories` |
| 行数 | 6 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:07:22.153Z |

```java
    1 | # Environment Post Processors
    2 | org.springframework.boot.EnvironmentPostProcessor=\
    3 | org.springframework.cloud.client.HostInfoEnvironmentPostProcessor
    4 | # Failure Analyzers
    5 | org.springframework.boot.diagnostics.FailureAnalyzer=\
    6 | org.springframework.cloud.configuration.CompatibilityNotMetFailureAnalyzer
```
