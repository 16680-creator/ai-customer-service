<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/RemoveCachedBodyFilter.java -->
<!-- 正文引用：[[源码:RemoveCachedBodyFilter.java#L起-L止]] ；代码块头部：(src: RemoveCachedBodyFilter.java#L起-L止) -->

# RemoveCachedBodyFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/RemoveCachedBodyFilter.java` |
| 行数 | 37 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:51:23.088Z |

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
   21 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   22 | import org.springframework.core.Ordered;
   23 | import org.springframework.web.server.ServerWebExchange;
   24 | 
   25 | public class RemoveCachedBodyFilter implements GlobalFilter, Ordered {
   26 | 
   27 | 	@Override
   28 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   29 | 		return chain.filter(exchange).doFinally(s -> ServerWebExchangeUtils.clearCachedRequestBody(exchange));
   30 | 	}
   31 | 
   32 | 	@Override
   33 | 	public int getOrder() {
   34 | 		return HIGHEST_PRECEDENCE;
   35 | 	}
   36 | 
   37 | }
```
