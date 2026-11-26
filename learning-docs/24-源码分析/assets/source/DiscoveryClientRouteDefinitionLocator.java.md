<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/discovery/DiscoveryClientRouteDefinitionLocator.java -->
<!-- 正文引用：[[源码:DiscoveryClientRouteDefinitionLocator.java#L起-L止]] ；代码块头部：(src: DiscoveryClientRouteDefinitionLocator.java#L起-L止) -->

# DiscoveryClientRouteDefinitionLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/discovery/DiscoveryClientRouteDefinitionLocator.java` |
| 行数 | 228 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:49.058Z |

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
   17 | package org.springframework.cloud.gateway.discovery;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | import java.util.Locale;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | import java.util.function.Predicate;
   26 | 
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | import reactor.core.publisher.Flux;
   31 | 
   32 | import org.springframework.cloud.client.ServiceInstance;
   33 | import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
   34 | import org.springframework.cloud.gateway.filter.FilterDefinition;
   35 | import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
   36 | import org.springframework.cloud.gateway.route.RouteDefinition;
   37 | import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
   38 | import org.springframework.core.style.ToStringCreator;
   39 | import org.springframework.expression.EvaluationException;
   40 | import org.springframework.expression.Expression;
   41 | import org.springframework.expression.ParseException;
   42 | import org.springframework.expression.spel.standard.SpelExpressionParser;
   43 | import org.springframework.expression.spel.support.SimpleEvaluationContext;
   44 | import org.springframework.util.StringUtils;
   45 | 
   46 | /**
   47 |  * TODO: change to RouteLocator? use java dsl
   48 |  *
   49 |  * @author Spencer Gibb
   50 |  */
   51 | public class DiscoveryClientRouteDefinitionLocator implements RouteDefinitionLocator {
   52 | 
   53 | 	private static final Log log = LogFactory.getLog(DiscoveryClientRouteDefinitionLocator.class);
   54 | 
   55 | 	private final DiscoveryLocatorProperties properties;
   56 | 
   57 | 	private final String routeIdPrefix;
   58 | 
   59 | 	private final SimpleEvaluationContext evalCtxt;
   60 | 
   61 | 	private @Nullable Flux<List<ServiceInstance>> serviceInstances;
   62 | 
   63 | 	public DiscoveryClientRouteDefinitionLocator(ReactiveDiscoveryClient discoveryClient,
   64 | 			DiscoveryLocatorProperties properties) {
   65 | 		this(discoveryClient.getClass().getSimpleName(), properties);
   66 | 		serviceInstances = discoveryClient.getServices()
   67 | 			.flatMap(service -> discoveryClient.getInstances(service).collectList());
   68 | 	}
   69 | 
   70 | 	private DiscoveryClientRouteDefinitionLocator(String discoveryClientName, DiscoveryLocatorProperties properties) {
   71 | 		this.properties = properties;
   72 | 		if (StringUtils.hasText(properties.getRouteIdPrefix())) {
   73 | 			routeIdPrefix = properties.getRouteIdPrefix();
   74 | 		}
   75 | 		else {
   76 | 			routeIdPrefix = discoveryClientName + "_";
   77 | 		}
   78 | 		evalCtxt = SimpleEvaluationContext.forReadOnlyDataBinding().build();
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public Flux<RouteDefinition> getRouteDefinitions() {
   83 | 
   84 | 		SpelExpressionParser parser = new SpelExpressionParser();
   85 | 		Expression includeExpr = parser.parseExpression(properties.getIncludeExpression());
   86 | 		Expression urlExpr = parser.parseExpression(properties.getUrlExpression());
   87 | 
   88 | 		Predicate<ServiceInstance> includePredicate;
   89 | 		if (properties.getIncludeExpression() == null || "true".equalsIgnoreCase(properties.getIncludeExpression())) {
   90 | 			includePredicate = instance -> true;
   91 | 		}
   92 | 		else {
   93 | 			includePredicate = instance -> {
   94 | 				Boolean include = includeExpr.getValue(evalCtxt, instance, Boolean.class);
   95 | 				if (include == null) {
   96 | 					return false;
   97 | 				}
   98 | 				return include;
   99 | 			};
  100 | 		}
  101 | 		Objects.requireNonNull(serviceInstances, "serviceInstances must be set");
  102 | 		return serviceInstances.filter(instances -> !instances.isEmpty())
  103 | 			.flatMap(Flux::fromIterable)
  104 | 			.filter(includePredicate)
  105 | 			.collectMap(instance -> instance.getServiceId())
  106 | 			// remove duplicates
  107 | 			.flatMapMany(map -> Flux.fromIterable(map.values()))
  108 | 			.map(instance -> {
  109 | 				RouteDefinition routeDefinition = buildRouteDefinition(urlExpr, instance);
  110 | 
  111 | 				final ServiceInstance instanceForEval = new DelegatingServiceInstance(instance, properties);
  112 | 
  113 | 				for (PredicateDefinition original : this.properties.getPredicates()) {
  114 | 					PredicateDefinition predicate = new PredicateDefinition();
  115 | 					if (original.getName() != null) {
  116 | 						predicate.setName(original.getName());
  117 | 					}
  118 | 					for (Map.Entry<String, String> entry : original.getArgs().entrySet()) {
  119 | 						String value = getValueFromExpr(evalCtxt, parser, instanceForEval, entry);
  120 | 						if (value != null) {
  121 | 							predicate.addArg(entry.getKey(), value);
  122 | 						}
  123 | 					}
  124 | 					routeDefinition.getPredicates().add(predicate);
  125 | 				}
  126 | 
  127 | 				for (FilterDefinition original : this.properties.getFilters()) {
  128 | 					FilterDefinition filter = new FilterDefinition();
  129 | 					if (original.getName() != null) {
  130 | 						filter.setName(original.getName());
  131 | 					}
  132 | 					for (Map.Entry<String, String> entry : original.getArgs().entrySet()) {
  133 | 						String value = getValueFromExpr(evalCtxt, parser, instanceForEval, entry);
  134 | 						if (value != null) {
  135 | 							filter.addArg(entry.getKey(), value);
  136 | 						}
  137 | 					}
  138 | 					routeDefinition.getFilters().add(filter);
  139 | 				}
  140 | 
  141 | 				return routeDefinition;
  142 | 			});
  143 | 	}
  144 | 
  145 | 	protected RouteDefinition buildRouteDefinition(Expression urlExpr, ServiceInstance serviceInstance) {
  146 | 		String serviceId = serviceInstance.getServiceId();
  147 | 		RouteDefinition routeDefinition = new RouteDefinition();
  148 | 		routeDefinition.setId(this.routeIdPrefix + serviceId);
  149 | 		String uri = urlExpr.getValue(this.evalCtxt, serviceInstance, String.class);
  150 | 		if (uri != null) {
  151 | 			routeDefinition.setUri(URI.create(uri));
  152 | 		}
  153 | 		// add instance metadata
  154 | 		routeDefinition.setMetadata(new LinkedHashMap<>(serviceInstance.getMetadata()));
  155 | 		return routeDefinition;
  156 | 	}
  157 | 
  158 | 	private @Nullable String getValueFromExpr(SimpleEvaluationContext evalCtxt, SpelExpressionParser parser,
  159 | 			ServiceInstance instance, Map.Entry<String, String> entry) {
  160 | 		try {
  161 | 			Expression valueExpr = parser.parseExpression(entry.getValue());
  162 | 			return valueExpr.getValue(evalCtxt, instance, String.class);
  163 | 		}
  164 | 		catch (ParseException | EvaluationException e) {
  165 | 			if (log.isDebugEnabled()) {
  166 | 				log.debug("Unable to parse " + entry.getValue(), e);
  167 | 			}
  168 | 			throw e;
  169 | 		}
  170 | 	}
  171 | 
  172 | 	private static class DelegatingServiceInstance implements ServiceInstance {
  173 | 
  174 | 		final ServiceInstance delegate;
  175 | 
  176 | 		private final DiscoveryLocatorProperties properties;
  177 | 
  178 | 		private DelegatingServiceInstance(ServiceInstance delegate, DiscoveryLocatorProperties properties) {
  179 | 			this.delegate = delegate;
  180 | 			this.properties = properties;
  181 | 		}
  182 | 
  183 | 		@Override
  184 | 		public String getServiceId() {
  185 | 			if (properties.isLowerCaseServiceId()) {
  186 | 				return delegate.getServiceId().toLowerCase(Locale.ROOT);
  187 | 			}
  188 | 			return delegate.getServiceId();
  189 | 		}
  190 | 
  191 | 		@Override
  192 | 		public String getHost() {
  193 | 			return delegate.getHost();
  194 | 		}
  195 | 
  196 | 		@Override
  197 | 		public int getPort() {
  198 | 			return delegate.getPort();
  199 | 		}
  200 | 
  201 | 		@Override
  202 | 		public boolean isSecure() {
  203 | 			return delegate.isSecure();
  204 | 		}
  205 | 
  206 | 		@Override
  207 | 		public URI getUri() {
  208 | 			return delegate.getUri();
  209 | 		}
  210 | 
  211 | 		@Override
  212 | 		public @Nullable Map<String, String> getMetadata() {
  213 | 			return delegate.getMetadata();
  214 | 		}
  215 | 
  216 | 		@Override
  217 | 		public @Nullable String getScheme() {
  218 | 			return delegate.getScheme();
  219 | 		}
  220 | 
  221 | 		@Override
  222 | 		public String toString() {
  223 | 			return new ToStringCreator(this).append("delegate", delegate).append("properties", properties).toString();
  224 | 		}
  225 | 
  226 | 	}
  227 | 
  228 | }
```
