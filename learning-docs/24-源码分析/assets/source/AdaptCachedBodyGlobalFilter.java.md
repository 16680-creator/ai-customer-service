<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/AdaptCachedBodyGlobalFilter.java -->
<!-- 正文引用：[[源码:AdaptCachedBodyGlobalFilter.java#L起-L止]] ；代码块头部：(src: AdaptCachedBodyGlobalFilter.java#L起-L止) -->

# AdaptCachedBodyGlobalFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/AdaptCachedBodyGlobalFilter.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:50.446Z |

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
   19 | import java.util.concurrent.ConcurrentHashMap;
   20 | import java.util.concurrent.ConcurrentMap;
   21 | 
   22 | import reactor.core.publisher.Mono;
   23 | 
   24 | import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
   25 | import org.springframework.cloud.gateway.route.Route;
   26 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   27 | import org.springframework.context.ApplicationListener;
   28 | import org.springframework.core.Ordered;
   29 | import org.springframework.core.io.buffer.DataBuffer;
   30 | import org.springframework.http.server.reactive.ServerHttpRequest;
   31 | import org.springframework.web.server.ServerWebExchange;
   32 | 
   33 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
   34 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;
   35 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
   36 | 
   37 | public class AdaptCachedBodyGlobalFilter implements GlobalFilter, Ordered, ApplicationListener<EnableBodyCachingEvent> {
   38 | 
   39 | 	private ConcurrentMap<String, Boolean> routesToCache = new ConcurrentHashMap<>();
   40 | 
   41 | 	@Override
   42 | 	public void onApplicationEvent(EnableBodyCachingEvent event) {
   43 | 		this.routesToCache.putIfAbsent(event.getRouteId(), true);
   44 | 	}
   45 | 
   46 | 	@Override
   47 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   48 | 		// the cached ServerHttpRequest is used when the ServerWebExchange can not be
   49 | 		// mutated, for example, during a predicate where the body is read, but still
   50 | 		// needs to be cached.
   51 | 		ServerHttpRequest cachedRequest = exchange.getAttribute(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
   52 | 		if (cachedRequest != null) {
   53 | 			exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
   54 | 			return chain.filter(exchange.mutate().request(cachedRequest).build());
   55 | 		}
   56 | 
   57 | 		//
   58 | 		DataBuffer body = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
   59 | 		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
   60 | 
   61 | 		if (body != null || route == null || !this.routesToCache.containsKey(route.getId())) {
   62 | 			return chain.filter(exchange);
   63 | 		}
   64 | 
   65 | 		return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
   66 | 			// don't mutate and build if same request object
   67 | 			if (serverHttpRequest == exchange.getRequest()) {
   68 | 				return chain.filter(exchange);
   69 | 			}
   70 | 			return chain.filter(exchange.mutate().request(serverHttpRequest).build());
   71 | 		});
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public int getOrder() {
   76 | 		return Ordered.HIGHEST_PRECEDENCE + 1000;
   77 | 	}
   78 | 
   79 | }
```
