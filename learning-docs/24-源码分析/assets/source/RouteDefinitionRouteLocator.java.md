<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/route/RouteDefinitionRouteLocator.java -->
<!-- 正文引用：[[源码:RouteDefinitionRouteLocator.java#L起-L止]] ；代码块头部：(src: RouteDefinitionRouteLocator.java#L起-L止) -->

# RouteDefinitionRouteLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/route/RouteDefinitionRouteLocator.java` |
| 行数 | 237 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:46.881Z |

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
   17 | package org.springframework.cloud.gateway.route;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.HashMap;
   21 | import java.util.LinkedHashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import reactor.core.publisher.Flux;
   29 | 
   30 | import org.springframework.cloud.gateway.config.GatewayProperties;
   31 | import org.springframework.cloud.gateway.event.FilterArgsEvent;
   32 | import org.springframework.cloud.gateway.event.PredicateArgsEvent;
   33 | import org.springframework.cloud.gateway.filter.FilterDefinition;
   34 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   35 | import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
   36 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   37 | import org.springframework.cloud.gateway.handler.AsyncPredicate;
   38 | import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
   39 | import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
   40 | import org.springframework.cloud.gateway.support.ConfigurationService;
   41 | import org.springframework.cloud.gateway.support.HasRouteId;
   42 | import org.springframework.core.Ordered;
   43 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   44 | import org.springframework.util.CollectionUtils;
   45 | import org.springframework.web.server.ServerWebExchange;
   46 | 
   47 | /**
   48 |  * {@link RouteLocator} that loads routes from a {@link RouteDefinitionLocator}.
   49 |  *
   50 |  * @author Spencer Gibb
   51 |  */
   52 | public class RouteDefinitionRouteLocator implements RouteLocator {
   53 | 
   54 | 	/**
   55 | 	 * Default filters name.
   56 | 	 */
   57 | 	public static final String DEFAULT_FILTERS = "defaultFilters";
   58 | 
   59 | 	protected final Log logger = LogFactory.getLog(getClass());
   60 | 
   61 | 	private final RouteDefinitionLocator routeDefinitionLocator;
   62 | 
   63 | 	private final ConfigurationService configurationService;
   64 | 
   65 | 	private final Map<String, RoutePredicateFactory> predicates = new LinkedHashMap<>();
   66 | 
   67 | 	private final Map<String, GatewayFilterFactory> gatewayFilterFactories = new HashMap<>();
   68 | 
   69 | 	private final GatewayProperties gatewayProperties;
   70 | 
   71 | 	public RouteDefinitionRouteLocator(RouteDefinitionLocator routeDefinitionLocator,
   72 | 			List<RoutePredicateFactory> predicates, List<GatewayFilterFactory> gatewayFilterFactories,
   73 | 			GatewayProperties gatewayProperties, ConfigurationService configurationService) {
   74 | 		this.routeDefinitionLocator = routeDefinitionLocator;
   75 | 		this.configurationService = configurationService;
   76 | 		initFactories(predicates);
   77 | 		gatewayFilterFactories.forEach(factory -> this.gatewayFilterFactories.put(factory.name(), factory));
   78 | 		this.gatewayProperties = gatewayProperties;
   79 | 	}
   80 | 
   81 | 	private void initFactories(List<RoutePredicateFactory> predicates) {
   82 | 		predicates.forEach(factory -> {
   83 | 			String key = factory.name();
   84 | 			if (this.predicates.containsKey(key)) {
   85 | 				this.logger.warn("A RoutePredicateFactory named " + key + " already exists, class: "
   86 | 						+ this.predicates.get(key) + ". It will be overwritten.");
   87 | 			}
   88 | 			this.predicates.put(key, factory);
   89 | 			if (logger.isInfoEnabled()) {
   90 | 				logger.info("Loaded RoutePredicateFactory [" + key + "]");
   91 | 			}
   92 | 		});
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Filtering is done via {@link RouteDefinition} instead of {@link Route} to prevent
   97 | 	 * creating Route instances that will be discarded.
   98 | 	 */
   99 | 	@Override
  100 | 	public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
  101 | 		return getRoutes(this.routeDefinitionLocator.getRouteDefinitions()
  102 | 			.filter(routeDef -> RouteLocator.matchMetadata(routeDef.getMetadata(), metadata)));
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public Flux<Route> getRoutes() {
  107 | 		return getRoutes(this.routeDefinitionLocator.getRouteDefinitions());
  108 | 	}
  109 | 
  110 | 	private Flux<Route> getRoutes(Flux<RouteDefinition> routeDefinitions) {
  111 | 		Flux<Route> routes = routeDefinitions.map(this::convertToRoute);
  112 | 
  113 | 		if (!gatewayProperties.isFailOnRouteDefinitionError()) {
  114 | 			// instead of letting error bubble up, continue
  115 | 			routes = routes.onErrorContinue((error, obj) -> {
  116 | 				if (logger.isWarnEnabled()) {
  117 | 					logger.warn("RouteDefinition id " + ((RouteDefinition) obj).getId()
  118 | 							+ " will be ignored. Definition has invalid configs, " + error.getMessage());
  119 | 				}
  120 | 			});
  121 | 		}
  122 | 
  123 | 		return routes.map(route -> {
  124 | 			if (logger.isDebugEnabled()) {
  125 | 				logger.debug("RouteDefinition matched: " + route.getId());
  126 | 			}
  127 | 			return route;
  128 | 		});
  129 | 	}
  130 | 
  131 | 	private Route convertToRoute(RouteDefinition routeDefinition) {
  132 | 		AsyncPredicate<ServerWebExchange> predicate = combinePredicates(routeDefinition);
  133 | 		List<GatewayFilter> gatewayFilters = getFilters(routeDefinition);
  134 | 
  135 | 		return Route.async(routeDefinition).asyncPredicate(predicate).replaceFilters(gatewayFilters).build();
  136 | 	}
  137 | 
  138 | 	@SuppressWarnings("unchecked")
  139 | 	List<GatewayFilter> loadGatewayFilters(String id, List<FilterDefinition> filterDefinitions) {
  140 | 		ArrayList<GatewayFilter> ordered = new ArrayList<>(filterDefinitions.size());
  141 | 		for (int i = 0; i < filterDefinitions.size(); i++) {
  142 | 			FilterDefinition definition = filterDefinitions.get(i);
  143 | 			GatewayFilterFactory factory = this.gatewayFilterFactories.get(definition.getName());
  144 | 			if (factory == null) {
  145 | 				throw new IllegalArgumentException(
  146 | 						"Unable to find GatewayFilterFactory with name " + definition.getName());
  147 | 			}
  148 | 			if (logger.isDebugEnabled()) {
  149 | 				logger.debug("RouteDefinition " + id + " applying filter " + definition.getArgs() + " to "
  150 | 						+ definition.getName());
  151 | 			}
  152 | 
  153 | 			// @formatter:off
  154 | 			Object configuration = this.configurationService.with(factory)
  155 | 					.name(definition.getName())
  156 | 					.properties(definition.getArgs())
  157 | 					.eventFunction((bound, properties) -> new FilterArgsEvent(
  158 | 							// TODO: why explicit cast needed or java compile fails
  159 | 							RouteDefinitionRouteLocator.this, id, (Map<String, Object>) properties))
  160 | 					.bind();
  161 | 			// @formatter:on
  162 | 
  163 | 			// some filters require routeId
  164 | 			// TODO: is there a better place to apply this?
  165 | 			if (configuration instanceof HasRouteId hasRouteId) {
  166 | 				hasRouteId.setRouteId(id);
  167 | 			}
  168 | 
  169 | 			GatewayFilter gatewayFilter = factory.apply(configuration);
  170 | 			if (gatewayFilter instanceof Ordered) {
  171 | 				ordered.add(gatewayFilter);
  172 | 			}
  173 | 			else {
  174 | 				ordered.add(new OrderedGatewayFilter(gatewayFilter, i + 1));
  175 | 			}
  176 | 		}
  177 | 
  178 | 		return ordered;
  179 | 	}
  180 | 
  181 | 	private List<GatewayFilter> getFilters(RouteDefinition routeDefinition) {
  182 | 		List<GatewayFilter> filters = new ArrayList<>();
  183 | 		Objects.requireNonNull(routeDefinition.getId(), "Route id must be set");
  184 | 		// TODO: support option to apply defaults after route specific filters?
  185 | 		if (!this.gatewayProperties.getDefaultFilters().isEmpty()) {
  186 | 			filters.addAll(loadGatewayFilters(routeDefinition.getId(),
  187 | 					new ArrayList<>(this.gatewayProperties.getDefaultFilters())));
  188 | 		}
  189 | 
  190 | 		final List<FilterDefinition> definitionFilters = routeDefinition.getFilters();
  191 | 		if (!CollectionUtils.isEmpty(definitionFilters)) {
  192 | 			filters.addAll(loadGatewayFilters(routeDefinition.getId(), definitionFilters));
  193 | 		}
  194 | 
  195 | 		AnnotationAwareOrderComparator.sort(filters);
  196 | 		return filters;
  197 | 	}
  198 | 
  199 | 	private AsyncPredicate<ServerWebExchange> combinePredicates(RouteDefinition routeDefinition) {
  200 | 		List<PredicateDefinition> predicates = routeDefinition.getPredicates();
  201 | 		if (predicates == null || predicates.isEmpty()) {
  202 | 			// this is a very rare case, but possible, just match all
  203 | 			return AsyncPredicate.from(exchange -> true);
  204 | 		}
  205 | 
  206 | 		return predicates.stream()
  207 | 			.map(nextPredicate -> lookup(routeDefinition, nextPredicate))
  208 | 			.reduce(AsyncPredicate.from(exchange -> true), AsyncPredicate::and);
  209 | 	}
  210 | 
  211 | 	@SuppressWarnings("unchecked")
  212 | 	private AsyncPredicate<ServerWebExchange> lookup(RouteDefinition route, PredicateDefinition predicate) {
  213 | 		RoutePredicateFactory<Object> factory = this.predicates.get(predicate.getName());
  214 | 		if (factory == null) {
  215 | 			throw new IllegalArgumentException("Unable to find RoutePredicateFactory with name " + predicate.getName());
  216 | 		}
  217 | 		if (logger.isDebugEnabled()) {
  218 | 			logger.debug("RouteDefinition " + route.getId() + " applying " + predicate.getArgs() + " to "
  219 | 					+ predicate.getName());
  220 | 		}
  221 | 
  222 | 		// @formatter:off
  223 | 		Object config = this.configurationService.with(factory)
  224 | 				.name(predicate.getName())
  225 | 				.properties(predicate.getArgs())
  226 | 				.eventFunction((bound, properties) -> {
  227 | 					Objects.requireNonNull(route.getId(), "Route id must be set");
  228 | 					return new PredicateArgsEvent(RouteDefinitionRouteLocator.this, route.getId(), properties);
  229 | 						}
  230 | 				)
  231 | 				.bind();
  232 | 		// @formatter:on
  233 | 
  234 | 		return factory.applyAsync(config);
  235 | 	}
  236 | 
  237 | }
```
