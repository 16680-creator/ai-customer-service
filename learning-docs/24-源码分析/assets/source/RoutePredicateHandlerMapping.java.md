<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/RoutePredicateHandlerMapping.java -->
<!-- 正文引用：[[源码:RoutePredicateHandlerMapping.java#L起-L止]] ；代码块头部：(src: RoutePredicateHandlerMapping.java#L起-L止) -->

# RoutePredicateHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/RoutePredicateHandlerMapping.java` |
| 行数 | 195 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:45.039Z |

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
   19 | import java.util.function.Function;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | import reactor.core.publisher.Mono;
   23 | 
   24 | import org.springframework.cloud.gateway.config.GatewayProperties;
   25 | import org.springframework.cloud.gateway.config.GlobalCorsProperties;
   26 | import org.springframework.cloud.gateway.route.Route;
   27 | import org.springframework.cloud.gateway.route.RouteLocator;
   28 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   29 | import org.springframework.core.env.Environment;
   30 | import org.springframework.web.cors.CorsConfiguration;
   31 | import org.springframework.web.reactive.handler.AbstractHandlerMapping;
   32 | import org.springframework.web.server.ServerWebExchange;
   33 | 
   34 | import static org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping.ManagementPortType.DIFFERENT;
   35 | import static org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping.ManagementPortType.DISABLED;
   36 | import static org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping.ManagementPortType.SAME;
   37 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_HANDLER_MAPPER_ATTR;
   38 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR;
   39 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REACTOR_CONTEXT_ATTR;
   40 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
   41 | 
   42 | /**
   43 |  * @author Spencer Gibb
   44 |  */
   45 | public class RoutePredicateHandlerMapping extends AbstractHandlerMapping {
   46 | 
   47 | 	private final FilteringWebHandler webHandler;
   48 | 
   49 | 	private final RouteLocator routeLocator;
   50 | 
   51 | 	private final @Nullable Integer managementPort;
   52 | 
   53 | 	private final ManagementPortType managementPortType;
   54 | 
   55 | 	public RoutePredicateHandlerMapping(FilteringWebHandler webHandler, RouteLocator routeLocator,
   56 | 			GlobalCorsProperties globalCorsProperties, Environment environment) {
   57 | 		this.webHandler = webHandler;
   58 | 		this.routeLocator = routeLocator;
   59 | 
   60 | 		this.managementPort = getPortProperty(environment, "management.server.");
   61 | 		this.managementPortType = getManagementPortType(environment);
   62 | 		setOrder(environment.getProperty(GatewayProperties.PREFIX + ".handler-mapping.order", Integer.class, 1));
   63 | 		setCorsConfigurations(globalCorsProperties.getCorsConfigurations());
   64 | 	}
   65 | 
   66 | 	private ManagementPortType getManagementPortType(Environment environment) {
   67 | 		Integer serverPort = getPortProperty(environment, "server.");
   68 | 		if (this.managementPort != null && this.managementPort < 0) {
   69 | 			return DISABLED;
   70 | 		}
   71 | 		return ((this.managementPort == null || (serverPort == null && this.managementPort.equals(8080))
   72 | 				|| (this.managementPort != 0 && this.managementPort.equals(serverPort))) ? SAME : DIFFERENT);
   73 | 	}
   74 | 
   75 | 	private static @Nullable Integer getPortProperty(Environment environment, String prefix) {
   76 | 		return environment.getProperty(prefix + "port", Integer.class);
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
   81 | 		// don't handle requests on management port if set and different than server port
   82 | 		if (this.managementPortType == DIFFERENT && this.managementPort != null
   83 | 				&& exchange.getRequest().getLocalAddress() != null
   84 | 				&& exchange.getRequest().getLocalAddress().getPort() == this.managementPort) {
   85 | 			return Mono.empty();
   86 | 		}
   87 | 		exchange.getAttributes().put(GATEWAY_HANDLER_MAPPER_ATTR, getSimpleName());
   88 | 
   89 | 		return Mono.deferContextual(contextView -> {
   90 | 			exchange.getAttributes().put(GATEWAY_REACTOR_CONTEXT_ATTR, contextView);
   91 | 			return lookupRoute(exchange)
   92 | 				// .log("route-predicate-handler-mapping", Level.FINER) //name this
   93 | 				.map((Function<Route, ?>) r -> {
   94 | 					exchange.getAttributes().remove(GATEWAY_PREDICATE_ROUTE_ATTR);
   95 | 					if (logger.isDebugEnabled()) {
   96 | 						logger.debug("Mapping [" + getExchangeDesc(exchange) + "] to " + r);
   97 | 					}
   98 | 
   99 | 					exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, r);
  100 | 					return webHandler;
  101 | 				})
  102 | 				.switchIfEmpty(Mono.empty().then(Mono.fromRunnable(() -> {
  103 | 					exchange.getAttributes().remove(GATEWAY_PREDICATE_ROUTE_ATTR);
  104 | 					ServerWebExchangeUtils.clearCachedRequestBody(exchange);
  105 | 					if (logger.isTraceEnabled()) {
  106 | 						logger.trace("No RouteDefinition found for [" + getExchangeDesc(exchange) + "]");
  107 | 					}
  108 | 				})));
  109 | 		});
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	protected @Nullable CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
  114 | 		// TODO: support cors configuration via properties on a route see gh-229
  115 | 		// see RequestMappingHandlerMapping.initCorsConfiguration()
  116 | 		// also see
  117 | 		// https://github.com/spring-projects/spring-framework/blob/master/spring-web/src/test/java/org/springframework/web/cors/reactive/CorsWebFilterTests.java
  118 | 		return super.getCorsConfiguration(handler, exchange);
  119 | 	}
  120 | 
  121 | 	// TODO: get desc from factory?
  122 | 	private String getExchangeDesc(ServerWebExchange exchange) {
  123 | 		StringBuilder out = new StringBuilder();
  124 | 		out.append("Exchange: ");
  125 | 		out.append(exchange.getRequest().getMethod());
  126 | 		out.append(" ");
  127 | 		out.append(exchange.getRequest().getURI());
  128 | 		return out.toString();
  129 | 	}
  130 | 
  131 | 	protected Mono<Route> lookupRoute(ServerWebExchange exchange) {
  132 | 		return this.routeLocator.getRoutes().filterWhen(route -> {
  133 | 			// add the current route we are testing
  134 | 			exchange.getAttributes().put(GATEWAY_PREDICATE_ROUTE_ATTR, route.getId());
  135 | 			try {
  136 | 				return route.getPredicate().apply(exchange);
  137 | 			}
  138 | 			catch (Exception e) {
  139 | 				logger.error("Error applying predicate for route: " + route.getId(), e);
  140 | 			}
  141 | 			return Mono.just(false);
  142 | 		})
  143 | 			.next()
  144 | 			// TODO: error handling
  145 | 			.map(route -> {
  146 | 				if (logger.isDebugEnabled()) {
  147 | 					logger.debug("Route matched: " + route.getId());
  148 | 				}
  149 | 				validateRoute(route, exchange);
  150 | 				return route;
  151 | 			});
  152 | 
  153 | 		/*
  154 | 		 * TODO: trace logging if (logger.isTraceEnabled()) {
  155 | 		 * logger.trace("RouteDefinition did not match: " + routeDefinition.getId()); }
  156 | 		 */
  157 | 	}
  158 | 
  159 | 	/**
  160 | 	 * Validate the given handler against the current request.
  161 | 	 * <p>
  162 | 	 * The default implementation is empty. Can be overridden in subclasses, for example
  163 | 	 * to enforce specific preconditions expressed in URL mappings.
  164 | 	 * @param route the Route object to validate
  165 | 	 * @param exchange current exchange
  166 | 	 * @throws Exception if validation failed
  167 | 	 */
  168 | 	@SuppressWarnings("UnusedParameters")
  169 | 	protected void validateRoute(Route route, ServerWebExchange exchange) {
  170 | 	}
  171 | 
  172 | 	protected String getSimpleName() {
  173 | 		return "RoutePredicateHandlerMapping";
  174 | 	}
  175 | 
  176 | 	public enum ManagementPortType {
  177 | 
  178 | 		/**
  179 | 		 * The management port has been disabled.
  180 | 		 */
  181 | 		DISABLED,
  182 | 
  183 | 		/**
  184 | 		 * The management port is the same as the server port.
  185 | 		 */
  186 | 		SAME,
  187 | 
  188 | 		/**
  189 | 		 * The management port and server port are different.
  190 | 		 */
  191 | 		DIFFERENT;
  192 | 
  193 | 	}
  194 | 
  195 | }
```
