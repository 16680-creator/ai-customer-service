<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/GatewayFilterChain.java -->
<!-- 正文引用：[[源码:GatewayFilterChain.java#L起-L止]] ；代码块头部：(src: GatewayFilterChain.java#L起-L止) -->

# GatewayFilterChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/GatewayFilterChain.java` |
| 行数 | 40 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:45.249Z |

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
   17 | package org.springframework.cloud.gateway.filter;
   18 | 
   19 | import reactor.core.publisher.Mono;
   20 | 
   21 | import org.springframework.web.server.ServerWebExchange;
   22 | 
   23 | /**
   24 |  * Contract to allow a {@link GatewayFilter} to delegate to the next in the chain.
   25 |  *
   26 |  * Copied from framework WebFilterChain
   27 |  *
   28 |  * @author Rossen Stoyanchev
   29 |  * @since 5.0
   30 |  */
   31 | public interface GatewayFilterChain {
   32 | 
   33 | 	/**
   34 | 	 * Delegate to the next {@code GatewayFilter} in the chain.
   35 | 	 * @param exchange the current server exchange
   36 | 	 * @return {@code Mono<Void>} to indicate when request handling is complete
   37 | 	 */
   38 | 	Mono<Void> filter(ServerWebExchange exchange);
   39 | 
   40 | }
```
