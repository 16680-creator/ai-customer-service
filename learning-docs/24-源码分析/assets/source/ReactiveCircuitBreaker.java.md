<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/circuitbreaker/ReactiveCircuitBreaker.java -->
<!-- 正文引用：[[源码:ReactiveCircuitBreaker.java#L起-L止]] ；代码块头部：(src: ReactiveCircuitBreaker.java#L起-L止) -->

# ReactiveCircuitBreaker.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/circuitbreaker/ReactiveCircuitBreaker.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:21.532Z |

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
   20 | 
   21 | import reactor.core.publisher.Flux;
   22 | import reactor.core.publisher.Mono;
   23 | 
   24 | /**
   25 |  * Spring Cloud reactive circuit breaker API.
   26 |  *
   27 |  * @author Ryan Baxter
   28 |  */
   29 | public interface ReactiveCircuitBreaker {
   30 | 
   31 | 	default <T> Mono<T> run(Mono<T> toRun) {
   32 | 		return run(toRun, throwable -> {
   33 | 			throw new NoFallbackAvailableException("No fallback available.", throwable);
   34 | 		});
   35 | 	}
   36 | 
   37 | 	<T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback);
   38 | 
   39 | 	default <T> Flux<T> run(Flux<T> toRun) {
   40 | 		return run(toRun, throwable -> {
   41 | 			throw new NoFallbackAvailableException("No fallback available.", throwable);
   42 | 		});
   43 | 	}
   44 | 
   45 | 	<T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback);
   46 | 
   47 | }
```
