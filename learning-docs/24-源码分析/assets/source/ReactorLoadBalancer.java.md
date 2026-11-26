<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/ReactorLoadBalancer.java -->
<!-- 正文引用：[[源码:ReactorLoadBalancer.java#L起-L止]] ；代码块头部：(src: ReactorLoadBalancer.java#L起-L止) -->

# ReactorLoadBalancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/ReactorLoadBalancer.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.030Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.loadbalancer.core;
   18 | 
   19 | import reactor.core.publisher.Mono;
   20 | 
   21 | import org.springframework.cloud.client.loadbalancer.Request;
   22 | import org.springframework.cloud.client.loadbalancer.Response;
   23 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   24 | 
   25 | /**
   26 |  * A Reactor based implementation of {@link ReactiveLoadBalancer}.
   27 |  *
   28 |  * @param <T> - type of the response
   29 |  * @author Spencer Gibb
   30 |  */
   31 | public interface ReactorLoadBalancer<T> extends ReactiveLoadBalancer<T> {
   32 | 
   33 | 	/**
   34 | 	 * Choose the next server based on the load balancing algorithm.
   35 | 	 * @param request - an input request
   36 | 	 * @return - mono of response
   37 | 	 */
   38 | 	@SuppressWarnings("rawtypes")
   39 | 	Mono<Response<T>> choose(Request request);
   40 | 
   41 | 	default Mono<Response<T>> choose() {
   42 | 		return choose(REQUEST);
   43 | 	}
   44 | 
   45 | }
```
