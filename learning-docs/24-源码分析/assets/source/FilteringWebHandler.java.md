<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/FilteringWebHandler.java -->
<!-- 正文引用：[[源码:FilteringWebHandler.java#L起-L止]] ；代码块头部：(src: FilteringWebHandler.java#L起-L止) -->

# FilteringWebHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/FilteringWebHandler.java` |
| 行数 | 191 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:45.145Z |

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
   17 | package org.springframework.cloud.gateway.handler;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.concurrent.ConcurrentHashMap;
   22 | import java.util.stream.Collectors;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import reactor.core.publisher.Mono;
   27 | 
   28 | import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
   29 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   30 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   31 | import org.springframework.cloud.gateway.filter.GlobalFilter;
   32 | import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
   33 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   34 | import org.springframework.cloud.gateway.route.Route;
   35 | import org.springframework.context.ApplicationListener;
   36 | import org.springframework.core.DecoratingProxy;
   37 | import org.springframework.core.Ordered;
   38 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   39 | import org.springframework.core.annotation.AnnotationUtils;
   40 | import org.springframework.core.annotation.Order;
   41 | import org.springframework.web.server.ServerWebExchange;
   42 | import org.springframework.web.server.WebHandler;
   43 | 
   44 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
   45 | 
   46 | /**
   47 |  * WebHandler that delegates to a chain of {@link GlobalFilter} instances and
   48 |  * {@link GatewayFilterFactory} instances then to the target {@link WebHandler}.
   49 |  *
   50 |  * @author Rossen Stoyanchev
   51 |  * @author Spencer Gibb
   52 |  * @author Yuxin Wang
   53 |  * @since 0.1
   54 |  */
   55 | public class FilteringWebHandler implements WebHandler, ApplicationListener<RefreshRoutesEvent> {
   56 | 
   57 | 	protected static final Log logger = LogFactory.getLog(FilteringWebHandler.class);
   58 | 
   59 | 	private final List<GatewayFilter> globalFilters;
   60 | 
   61 | 	private final ConcurrentHashMap<Route, List<GatewayFilter>> routeFilterMap = new ConcurrentHashMap();
   62 | 
   63 | 	private final boolean routeFilterCacheEnabled;
   64 | 
   65 | 	public FilteringWebHandler(List<GlobalFilter> globalFilters, boolean routeFilterCacheEnabled) {
   66 | 		this.globalFilters = loadFilters(globalFilters);
   67 | 		this.routeFilterCacheEnabled = routeFilterCacheEnabled;
   68 | 	}
   69 | 
   70 | 	/* for testing */ ConcurrentHashMap<Route, List<GatewayFilter>> getRouteFilterMap() {
   71 | 		return routeFilterMap;
   72 | 	}
   73 | 
   74 | 	private static List<GatewayFilter> loadFilters(List<GlobalFilter> filters) {
   75 | 		return filters.stream().map(filter -> {
   76 | 			GatewayFilterAdapter gatewayFilter = new GatewayFilterAdapter(filter);
   77 | 			if (filter instanceof Ordered ordered) {
   78 | 				int order = ordered.getOrder();
   79 | 				return new OrderedGatewayFilter(gatewayFilter, order);
   80 | 			}
   81 | 			else {
   82 | 				Order order = AnnotationUtils.findAnnotation(filter.getClass(), Order.class);
   83 | 				if (order != null) {
   84 | 					return new OrderedGatewayFilter(gatewayFilter, order.value());
   85 | 				}
   86 | 			}
   87 | 			return gatewayFilter;
   88 | 		}).collect(Collectors.toList());
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public void onApplicationEvent(RefreshRoutesEvent event) {
   93 | 		if (this.routeFilterCacheEnabled) {
   94 | 			routeFilterMap.clear();
   95 | 		}
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public Mono<Void> handle(ServerWebExchange exchange) {
  100 | 		Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
  101 | 		List<GatewayFilter> combined = getCombinedFilters(route);
  102 | 
  103 | 		if (logger.isDebugEnabled()) {
  104 | 			logger.debug("Sorted gatewayFilterFactories: " + combined);
  105 | 		}
  106 | 
  107 | 		return new DefaultGatewayFilterChain(combined).filter(exchange);
  108 | 	}
  109 | 
  110 | 	protected List<GatewayFilter> getCombinedFilters(Route route) {
  111 | 		if (this.routeFilterCacheEnabled) {
  112 | 			return routeFilterMap.computeIfAbsent(route, this::getAllFilters);
  113 | 		}
  114 | 		else {
  115 | 			return getAllFilters(route);
  116 | 		}
  117 | 	}
  118 | 
  119 | 	protected List<GatewayFilter> getAllFilters(Route route) {
  120 | 		List<GatewayFilter> gatewayFilters = route.getFilters();
  121 | 		List<GatewayFilter> combined = new ArrayList<>(this.globalFilters);
  122 | 		combined.addAll(gatewayFilters);
  123 | 		AnnotationAwareOrderComparator.sort(combined);
  124 | 		return combined;
  125 | 	}
  126 | 
  127 | 	private static class DefaultGatewayFilterChain implements GatewayFilterChain {
  128 | 
  129 | 		private final int index;
  130 | 
  131 | 		private final List<GatewayFilter> filters;
  132 | 
  133 | 		DefaultGatewayFilterChain(List<GatewayFilter> filters) {
  134 | 			this.filters = filters;
  135 | 			this.index = 0;
  136 | 		}
  137 | 
  138 | 		private DefaultGatewayFilterChain(DefaultGatewayFilterChain parent, int index) {
  139 | 			this.filters = parent.getFilters();
  140 | 			this.index = index;
  141 | 		}
  142 | 
  143 | 		public List<GatewayFilter> getFilters() {
  144 | 			return filters;
  145 | 		}
  146 | 
  147 | 		@Override
  148 | 		public Mono<Void> filter(ServerWebExchange exchange) {
  149 | 			return Mono.defer(() -> {
  150 | 				if (this.index < filters.size()) {
  151 | 					GatewayFilter filter = filters.get(this.index);
  152 | 					DefaultGatewayFilterChain chain = new DefaultGatewayFilterChain(this, this.index + 1);
  153 | 					return filter.filter(exchange, chain);
  154 | 				}
  155 | 				else {
  156 | 					return Mono.empty(); // complete
  157 | 				}
  158 | 			});
  159 | 		}
  160 | 
  161 | 	}
  162 | 
  163 | 	private static class GatewayFilterAdapter implements GatewayFilter, DecoratingProxy {
  164 | 
  165 | 		private final GlobalFilter delegate;
  166 | 
  167 | 		GatewayFilterAdapter(GlobalFilter delegate) {
  168 | 			this.delegate = delegate;
  169 | 		}
  170 | 
  171 | 		@Override
  172 | 		public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  173 | 			return this.delegate.filter(exchange, chain);
  174 | 		}
  175 | 
  176 | 		@Override
  177 | 		public String toString() {
  178 | 			final StringBuilder sb = new StringBuilder("GatewayFilterAdapter{");
  179 | 			sb.append("delegate=").append(delegate);
  180 | 			sb.append('}');
  181 | 			return sb.toString();
  182 | 		}
  183 | 
  184 | 		@Override
  185 | 		public Class<?> getDecoratedClass() {
  186 | 			return delegate.getClass();
  187 | 		}
  188 | 
  189 | 	}
  190 | 
  191 | }
```
