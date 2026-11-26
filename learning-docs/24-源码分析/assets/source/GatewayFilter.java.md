<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/GatewayFilter.java -->
<!-- 正文引用：[[源码:GatewayFilter.java#L起-L止]] ；代码块头部：(src: GatewayFilter.java#L起-L止) -->

# GatewayFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/GatewayFilter.java` |
| 行数 | 55 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:46.252Z |

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
   21 | import org.springframework.cloud.gateway.support.ShortcutConfigurable;
   22 | import org.springframework.web.server.ServerWebExchange;
   23 | 
   24 | /**
   25 |  * Contract for interception-style, chained processing of Web requests that may be used to
   26 |  * implement cross-cutting, application-agnostic requirements such as security, timeouts,
   27 |  * and others. Specific to a Gateway
   28 |  *
   29 |  * Copied from WebFilter
   30 |  *
   31 |  * @author Rossen Stoyanchev
   32 |  * @since 5.0
   33 |  */
   34 | public interface GatewayFilter extends ShortcutConfigurable {
   35 | 
   36 | 	/**
   37 | 	 * Name key.
   38 | 	 */
   39 | 	String NAME_KEY = "name";
   40 | 
   41 | 	/**
   42 | 	 * Value key.
   43 | 	 */
   44 | 	String VALUE_KEY = "value";
   45 | 
   46 | 	/**
   47 | 	 * Process the Web request and (optionally) delegate to the next {@code WebFilter}
   48 | 	 * through the given {@link GatewayFilterChain}.
   49 | 	 * @param exchange the current server exchange
   50 | 	 * @param chain provides a way to delegate to the next filter
   51 | 	 * @return {@code Mono<Void>} to indicate when request processing is complete
   52 | 	 */
   53 | 	Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
   54 | 
   55 | }
```
