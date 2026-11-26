<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/metrics/ApplicationStartup.java -->
<!-- 正文引用：[[源码:ApplicationStartup.java#L起-L止]] ；代码块头部：(src: ApplicationStartup.java#L起-L止) -->

# ApplicationStartup.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/metrics/ApplicationStartup.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:20:54.637Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.core.metrics;
   18 | 
   19 | /**
   20 |  * Instruments the application startup phase using {@link StartupStep steps}.
   21 |  * <p>The core container and its infrastructure components can use the {@code ApplicationStartup}
   22 |  * to mark steps during the application startup and collect data about the execution context
   23 |  * or their processing time.
   24 |  *
   25 |  * @author Brian Clozel
   26 |  * @since 5.3
   27 |  */
   28 | public interface ApplicationStartup {
   29 | 
   30 | 	/**
   31 | 	 * Default "no op" {@code ApplicationStartup} implementation.
   32 | 	 * <p>This variant is designed for minimal overhead and does not record data.
   33 | 	 */
   34 | 	ApplicationStartup DEFAULT = new DefaultApplicationStartup();
   35 | 
   36 | 	/**
   37 | 	 * Create a new step and marks its beginning.
   38 | 	 * <p>A step name describes the current action or phase. This technical
   39 | 	 * name should be "." namespaced and can be reused to describe other instances of
   40 | 	 * the same step during application startup.
   41 | 	 * @param name the step name
   42 | 	 */
   43 | 	StartupStep start(String name);
   44 | 
   45 | }
```
