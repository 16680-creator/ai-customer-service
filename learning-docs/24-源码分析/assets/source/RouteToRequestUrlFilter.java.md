<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/RouteToRequestUrlFilter.java -->
<!-- 正文引用：[[源码:RouteToRequestUrlFilter.java#L起-L止]] ；代码块头部：(src: RouteToRequestUrlFilter.java#L起-L止) -->

# RouteToRequestUrlFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/RouteToRequestUrlFilter.java` |
| 行数 | 98 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:47.592Z |

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
   19 | import java.net.URI;
   20 | import java.util.regex.Pattern;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import reactor.core.publisher.Mono;
   25 | 
   26 | import org.springframework.cloud.gateway.route.Route;
   27 | import org.springframework.core.Ordered;
   28 | import org.springframework.web.server.ServerWebExchange;
   29 | import org.springframework.web.util.UriComponentsBuilder;
   30 | 
   31 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   32 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
   33 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;
   34 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.containsEncodedParts;
   35 | 
   36 | /**
   37 |  * @author Spencer Gibb
   38 |  */
   39 | public class RouteToRequestUrlFilter implements GlobalFilter, Ordered {
   40 | 
   41 | 	/**
   42 | 	 * Order of Route to URL.
   43 | 	 */
   44 | 	public static final int ROUTE_TO_URL_FILTER_ORDER = 10000;
   45 | 
   46 | 	private static final Log log = LogFactory.getLog(RouteToRequestUrlFilter.class);
   47 | 
   48 | 	private static final String SCHEME_REGEX = "[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*";
   49 | 	static final Pattern schemePattern = Pattern.compile(SCHEME_REGEX);
   50 | 
   51 | 	/* for testing */
   52 | 	static boolean hasAnotherScheme(URI uri) {
   53 | 		return schemePattern.matcher(uri.getSchemeSpecificPart()).matches() && uri.getHost() == null
   54 | 				&& uri.getRawPath() == null;
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public int getOrder() {
   59 | 		return ROUTE_TO_URL_FILTER_ORDER;
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   64 | 		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
   65 | 		if (route == null) {
   66 | 			return chain.filter(exchange);
   67 | 		}
   68 | 		log.trace("RouteToRequestUrlFilter start");
   69 | 		URI uri = exchange.getRequest().getURI();
   70 | 		boolean encoded = containsEncodedParts(uri);
   71 | 		URI routeUri = route.getUri();
   72 | 
   73 | 		if (hasAnotherScheme(routeUri)) {
   74 | 			// this is a special url, save scheme to special attribute
   75 | 			// replace routeUri with schemeSpecificPart
   76 | 			exchange.getAttributes().put(GATEWAY_SCHEME_PREFIX_ATTR, routeUri.getScheme());
   77 | 			routeUri = URI.create(routeUri.getSchemeSpecificPart());
   78 | 		}
   79 | 
   80 | 		if ("lb".equalsIgnoreCase(routeUri.getScheme()) && routeUri.getHost() == null) {
   81 | 			// Load balanced URIs should always have a host. If the host is null it is
   82 | 			// most likely because the host name was invalid (for example included an
   83 | 			// underscore)
   84 | 			throw new IllegalStateException("Invalid host: " + routeUri.toString());
   85 | 		}
   86 | 
   87 | 		URI mergedUrl = UriComponentsBuilder.fromUri(uri)
   88 | 			// .uri(routeUri)
   89 | 			.scheme(routeUri.getScheme())
   90 | 			.host(routeUri.getHost())
   91 | 			.port(routeUri.getPort())
   92 | 			.build(encoded)
   93 | 			.toUri();
   94 | 		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, mergedUrl);
   95 | 		return chain.filter(exchange);
   96 | 	}
   97 | 
   98 | }
```
