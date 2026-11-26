<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/circuitbreaker/CircuitBreaker.java -->
<!-- 正文引用：[[源码:CircuitBreaker.java#L起-L止]] ；代码块头部：(src: CircuitBreaker.java#L起-L止) -->

# CircuitBreaker.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/circuitbreaker/CircuitBreaker.java` |
| 行数 | 39 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:21.438Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.client.circuitbreaker;
   18 | 
   19 | import java.util.function.Function;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | /**
   25 |  * Spring Cloud circuit breaker.
   26 |  *
   27 |  * @author Ryan Baxter
   28 |  */
   29 | public interface CircuitBreaker {
   30 | 
   31 | 	default <T> T run(Supplier<T> toRun) {
   32 | 		return run(toRun, throwable -> {
   33 | 			throw new NoFallbackAvailableException("No fallback available.", throwable);
   34 | 		});
   35 | 	}
   36 | 
   37 | 	<T> T run(Supplier<T> toRun, Function<@Nullable Throwable, T> fallback);
   38 | 
   39 | }
```
