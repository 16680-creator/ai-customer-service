<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/OrderedGatewayFilter.java -->
<!-- 正文引用：[[源码:OrderedGatewayFilter.java#L起-L止]] ；代码块头部：(src: OrderedGatewayFilter.java#L起-L止) -->

# OrderedGatewayFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/OrderedGatewayFilter.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:46.419Z |

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
   21 | import org.springframework.core.Ordered;
   22 | import org.springframework.web.server.ServerWebExchange;
   23 | 
   24 | /**
   25 |  * @author Spencer Gibb
   26 |  */
   27 | public class OrderedGatewayFilter implements GatewayFilter, Ordered {
   28 | 
   29 | 	private final GatewayFilter delegate;
   30 | 
   31 | 	private final int order;
   32 | 
   33 | 	public OrderedGatewayFilter(GatewayFilter delegate, int order) {
   34 | 		this.delegate = delegate;
   35 | 		this.order = order;
   36 | 	}
   37 | 
   38 | 	public GatewayFilter getDelegate() {
   39 | 		return delegate;
   40 | 	}
   41 | 
   42 | 	@Override
   43 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   44 | 		return this.delegate.filter(exchange, chain);
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public int getOrder() {
   49 | 		return this.order;
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public String toString() {
   54 | 		return new StringBuilder("[").append(delegate).append(", order = ").append(order).append("]").toString();
   55 | 	}
   56 | 
   57 | }
```
