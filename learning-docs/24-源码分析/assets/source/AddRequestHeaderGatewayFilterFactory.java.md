<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/AddRequestHeaderGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:AddRequestHeaderGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: AddRequestHeaderGatewayFilterFactory.java#L起-L止) -->

# AddRequestHeaderGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/AddRequestHeaderGatewayFilterFactory.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.227Z |

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
   19 | import java.util.Objects;
   20 | 
   21 | import reactor.core.publisher.Mono;
   22 | 
   23 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   24 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   25 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   26 | import org.springframework.http.server.reactive.ServerHttpRequest;
   27 | import org.springframework.web.server.ServerWebExchange;
   28 | 
   29 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   30 | 
   31 | /**
   32 |  * @author Spencer Gibb
   33 |  */
   34 | public class AddRequestHeaderGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {
   35 | 
   36 | 	@Override
   37 | 	public GatewayFilter apply(NameValueConfig config) {
   38 | 		return new GatewayFilter() {
   39 | 			@Override
   40 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   41 | 				String name = Objects.requireNonNull(config.getName(), "name must not be null");
   42 | 				String rawValue = Objects.requireNonNull(config.getValue(), "value must not be null");
   43 | 				String value = ServerWebExchangeUtils.expand(exchange, rawValue);
   44 | 				ServerHttpRequest request = exchange.getRequest()
   45 | 					.mutate()
   46 | 					.headers(httpHeaders -> httpHeaders.add(name, value))
   47 | 					.build();
   48 | 
   49 | 				return chain.filter(exchange.mutate().request(request).build());
   50 | 			}
   51 | 
   52 | 			@Override
   53 | 			public String toString() {
   54 | 				String name = config.getName();
   55 | 				String value = config.getValue();
   56 | 				return filterToStringCreator(AddRequestHeaderGatewayFilterFactory.this)
   57 | 					.append(name != null ? name : "", value != null ? value : "")
   58 | 					.toString();
   59 | 			}
   60 | 		};
   61 | 	}
   62 | 
   63 | }
```
