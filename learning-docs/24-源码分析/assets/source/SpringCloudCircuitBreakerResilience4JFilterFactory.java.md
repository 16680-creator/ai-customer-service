<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/SpringCloudCircuitBreakerResilience4JFilterFactory.java -->
<!-- 正文引用：[[源码:SpringCloudCircuitBreakerResilience4JFilterFactory.java#L起-L止]] ；代码块头部：(src: SpringCloudCircuitBreakerResilience4JFilterFactory.java#L起-L止) -->

# SpringCloudCircuitBreakerResilience4JFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/SpringCloudCircuitBreakerResilience4JFilterFactory.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.698Z |

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
   17 | package org.springframework.cloud.gateway.filter.factory;
   18 | 
   19 | import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
   20 | import reactor.core.publisher.Mono;
   21 | 
   22 | import org.springframework.beans.factory.ObjectProvider;
   23 | import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
   24 | import org.springframework.cloud.gateway.support.ServiceUnavailableException;
   25 | import org.springframework.http.HttpStatus;
   26 | import org.springframework.web.reactive.DispatcherHandler;
   27 | import org.springframework.web.server.ResponseStatusException;
   28 | 
   29 | /**
   30 |  * @author Ryan Baxter
   31 |  */
   32 | public class SpringCloudCircuitBreakerResilience4JFilterFactory extends SpringCloudCircuitBreakerFilterFactory {
   33 | 
   34 | 	public SpringCloudCircuitBreakerResilience4JFilterFactory(
   35 | 			ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory,
   36 | 			ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
   37 | 		super(reactiveCircuitBreakerFactory, dispatcherHandlerProvider);
   38 | 	}
   39 | 
   40 | 	@Override
   41 | 	protected Mono<Void> handleErrorWithoutFallback(Throwable t, boolean resumeWithoutError) {
   42 | 		if (java.util.concurrent.TimeoutException.class.isInstance(t)) {
   43 | 			return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, t.getMessage(), t));
   44 | 		}
   45 | 		if (CallNotPermittedException.class.isInstance(t)) {
   46 | 			return Mono.error(new ServiceUnavailableException());
   47 | 		}
   48 | 		if (resumeWithoutError) {
   49 | 			return Mono.empty();
   50 | 		}
   51 | 		return Mono.error(t);
   52 | 	}
   53 | 
   54 | }
```
