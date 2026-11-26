<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/WeightCalculatorWebFilter.java -->
<!-- 正文引用：[[源码:WeightCalculatorWebFilter.java#L起-L止]] ；代码块头部：(src: WeightCalculatorWebFilter.java#L起-L止) -->

# WeightCalculatorWebFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/WeightCalculatorWebFilter.java` |
| 行数 | 321 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:19.183Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | import java.util.concurrent.ConcurrentHashMap;
   25 | import java.util.concurrent.ThreadLocalRandom;
   26 | import java.util.concurrent.atomic.AtomicBoolean;
   27 | import java.util.concurrent.atomic.AtomicInteger;
   28 | import java.util.function.Function;
   29 | import java.util.function.Supplier;
   30 | 
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | import org.jspecify.annotations.Nullable;
   34 | import reactor.core.publisher.Mono;
   35 | 
   36 | import org.springframework.beans.factory.ObjectProvider;
   37 | import org.springframework.cloud.gateway.event.PredicateArgsEvent;
   38 | import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
   39 | import org.springframework.cloud.gateway.event.RouteDeletedEvent;
   40 | import org.springframework.cloud.gateway.event.WeightDefinedEvent;
   41 | import org.springframework.cloud.gateway.route.RouteLocator;
   42 | import org.springframework.cloud.gateway.support.ConfigurationService;
   43 | import org.springframework.cloud.gateway.support.WeightConfig;
   44 | import org.springframework.context.ApplicationEvent;
   45 | import org.springframework.context.event.SmartApplicationListener;
   46 | import org.springframework.core.Ordered;
   47 | import org.springframework.core.log.LogMessage;
   48 | import org.springframework.core.style.ToStringCreator;
   49 | import org.springframework.web.server.ServerWebExchange;
   50 | import org.springframework.web.server.WebFilter;
   51 | import org.springframework.web.server.WebFilterChain;
   52 | 
   53 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.WEIGHT_ATTR;
   54 | 
   55 | /**
   56 |  * @author Spencer Gibb
   57 |  * @author Alexey Nakidkin
   58 |  */
   59 | public class WeightCalculatorWebFilter implements WebFilter, Ordered, SmartApplicationListener {
   60 | 
   61 | 	/**
   62 | 	 * Order of Weight Calculator Web filter.
   63 | 	 */
   64 | 	public static final int WEIGHT_CALC_FILTER_ORDER = 10001;
   65 | 
   66 | 	private static final Log log = LogFactory.getLog(WeightCalculatorWebFilter.class);
   67 | 
   68 | 	private final ObjectProvider<RouteLocator> routeLocator;
   69 | 
   70 | 	private final ConfigurationService configurationService;
   71 | 
   72 | 	private Function<ServerWebExchange, Double> randomFunction = exchange -> ThreadLocalRandom.current().nextDouble();
   73 | 
   74 | 	private int order = WEIGHT_CALC_FILTER_ORDER;
   75 | 
   76 | 	private Map<String, GroupWeightConfig> groupWeights = new ConcurrentHashMap<>();
   77 | 
   78 | 	private final AtomicBoolean routeLocatorInitialized = new AtomicBoolean();
   79 | 
   80 | 	public WeightCalculatorWebFilter(ObjectProvider<RouteLocator> routeLocator,
   81 | 			ConfigurationService configurationService) {
   82 | 		this.routeLocator = routeLocator;
   83 | 		this.configurationService = configurationService;
   84 | 	}
   85 | 
   86 | 	/* for testing */
   87 | 	static Map<String, String> getWeights(ServerWebExchange exchange) {
   88 | 		Map<String, String> weights = exchange.getAttribute(WEIGHT_ATTR);
   89 | 
   90 | 		if (weights == null) {
   91 | 			weights = new ConcurrentHashMap<>();
   92 | 			exchange.getAttributes().put(WEIGHT_ATTR, weights);
   93 | 		}
   94 | 		return weights;
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public int getOrder() {
   99 | 		return order;
  100 | 	}
  101 | 
  102 | 	public void setOrder(int order) {
  103 | 		this.order = order;
  104 | 	}
  105 | 
  106 | 	public void setRandomSupplier(Supplier<Double> randomSupplier) {
  107 | 		Objects.requireNonNull(randomSupplier, "randomSupplier may not be null");
  108 | 		this.randomFunction = exchange -> randomSupplier.get();
  109 | 	}
  110 | 
  111 | 	public void setRandomFunction(Function<ServerWebExchange, Double> randomFunction) {
  112 | 		Objects.requireNonNull(randomFunction, "randomFunction may not be null");
  113 | 		this.randomFunction = randomFunction;
  114 | 	}
  115 | 
  116 | 	@Override
  117 | 	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
  118 | 		// from config file
  119 | 		return PredicateArgsEvent.class.isAssignableFrom(eventType) ||
  120 | 		// from java dsl
  121 | 				WeightDefinedEvent.class.isAssignableFrom(eventType) ||
  122 | 				// from actuator or custom call
  123 | 				RouteDeletedEvent.class.isAssignableFrom(eventType) ||
  124 | 				// force initialization
  125 | 				RefreshRoutesEvent.class.isAssignableFrom(eventType);
  126 | 	}
  127 | 
  128 | 	@Override
  129 | 	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
  130 | 		return true;
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public void onApplicationEvent(ApplicationEvent event) {
  135 | 		if (event instanceof PredicateArgsEvent) {
  136 | 			handle((PredicateArgsEvent) event);
  137 | 		}
  138 | 		else if (event instanceof WeightDefinedEvent) {
  139 | 			addWeightConfig(((WeightDefinedEvent) event).getWeightConfig());
  140 | 		}
  141 | 		else if (event instanceof RouteDeletedEvent) {
  142 | 			removeWeightConfig(((RouteDeletedEvent) event).getRouteId());
  143 | 		}
  144 | 		else if (event instanceof RefreshRoutesEvent && routeLocator != null) {
  145 | 			// forces initialization
  146 | 			if (routeLocatorInitialized.compareAndSet(false, true)) {
  147 | 				// on first time, block so that app fails to start if there are errors in
  148 | 				// routes
  149 | 				// see gh-1574
  150 | 				routeLocator.ifAvailable(locator -> locator.getRoutes().blockLast());
  151 | 			}
  152 | 			else {
  153 | 				// this preserves previous behaviour on refresh, this could likely go away
  154 | 				routeLocator.ifAvailable(locator -> locator.getRoutes().subscribe());
  155 | 			}
  156 | 		}
  157 | 
  158 | 	}
  159 | 
  160 | 	public void handle(PredicateArgsEvent event) {
  161 | 		Map<String, Object> args = event.getArgs();
  162 | 
  163 | 		if (args.isEmpty() || !hasRelevantKey(args)) {
  164 | 			return;
  165 | 		}
  166 | 
  167 | 		WeightConfig config = new WeightConfig(event.getRouteId());
  168 | 
  169 | 		this.configurationService.with(config).name(WeightConfig.CONFIG_PREFIX).normalizedProperties(args).bind();
  170 | 
  171 | 		addWeightConfig(config);
  172 | 	}
  173 | 
  174 | 	private boolean hasRelevantKey(Map<String, Object> args) {
  175 | 		return args.keySet().stream().anyMatch(key -> key.startsWith(WeightConfig.CONFIG_PREFIX + "."));
  176 | 	}
  177 | 
  178 | 	/* for testing */ void addWeightConfig(WeightConfig weightConfig) {
  179 | 		String group = weightConfig.getGroup();
  180 | 		GroupWeightConfig config;
  181 | 		// only create new GroupWeightConfig rather than modify
  182 | 		// and put at end of calculations. This avoids concurency problems
  183 | 		// later during filter execution.
  184 | 		if (groupWeights.containsKey(group)) {
  185 | 			config = new GroupWeightConfig(groupWeights.get(group));
  186 | 		}
  187 | 		else {
  188 | 			config = new GroupWeightConfig(group);
  189 | 		}
  190 | 
  191 | 		config.weights.put(weightConfig.getRouteId(), weightConfig.getWeight());
  192 | 
  193 | 		// recalculate
  194 | 
  195 | 		// normalize weights
  196 | 		int weightsSum = 0;
  197 | 
  198 | 		for (Integer weight : config.weights.values()) {
  199 | 			weightsSum += weight;
  200 | 		}
  201 | 
  202 | 		final AtomicInteger index = new AtomicInteger(0);
  203 | 		for (Map.Entry<String, Integer> entry : config.weights.entrySet()) {
  204 | 			String routeId = entry.getKey();
  205 | 			Integer weight = entry.getValue();
  206 | 			Double nomalizedWeight = weight / (double) weightsSum;
  207 | 			config.normalizedWeights.put(routeId, nomalizedWeight);
  208 | 
  209 | 			// recalculate rangeIndexes
  210 | 			config.rangeIndexes.put(index.getAndIncrement(), routeId);
  211 | 		}
  212 | 
  213 | 		// TODO: calculate ranges
  214 | 		config.ranges.clear();
  215 | 
  216 | 		config.ranges.add(0.0);
  217 | 
  218 | 		List<Double> values = new ArrayList<>(config.normalizedWeights.values());
  219 | 		for (int i = 0; i < values.size(); i++) {
  220 | 			Double currentWeight = values.get(i);
  221 | 			Double previousRange = config.ranges.get(i);
  222 | 			Double range = previousRange + currentWeight;
  223 | 			config.ranges.add(range);
  224 | 		}
  225 | 
  226 | 		if (log.isTraceEnabled()) {
  227 | 			log.trace("Recalculated group weight config " + config);
  228 | 		}
  229 | 		// only update after all calculations
  230 | 		groupWeights.put(group, config);
  231 | 	}
  232 | 
  233 | 	private void removeWeightConfig(String routeId) {
  234 | 		log.trace(LogMessage.format("Removing weight config for route %s", routeId));
  235 | 		groupWeights.forEach((group, weightConfig) -> {
  236 | 			if (weightConfig.normalizedWeights.containsKey(routeId)) {
  237 | 				weightConfig.normalizedWeights.remove(routeId);
  238 | 				weightConfig.weights.remove(routeId);
  239 | 			}
  240 | 		});
  241 | 	}
  242 | 
  243 | 	/* for testing */ Map<String, GroupWeightConfig> getGroupWeights() {
  244 | 		return groupWeights;
  245 | 	}
  246 | 
  247 | 	@Override
  248 | 	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
  249 | 		Map<String, String> weights = getWeights(exchange);
  250 | 
  251 | 		for (String group : groupWeights.keySet()) {
  252 | 			GroupWeightConfig config = groupWeights.get(group);
  253 | 
  254 | 			if (config == null) {
  255 | 				if (log.isDebugEnabled()) {
  256 | 					log.debug("No GroupWeightConfig found for group: " + group);
  257 | 				}
  258 | 				continue; // nothing we can do, but this is odd
  259 | 			}
  260 | 
  261 | 			// Usually, multiple threads accessing the same random object will have some
  262 | 			// performance problems, so we can use ThreadLocalRandom by default
  263 | 			double r = randomFunction.apply(exchange);
  264 | 
  265 | 			List<Double> ranges = config.ranges;
  266 | 
  267 | 			if (log.isTraceEnabled()) {
  268 | 				log.trace("Weight for group: " + group + ", ranges: " + ranges + ", r: " + r);
  269 | 			}
  270 | 
  271 | 			for (int i = 0; i < ranges.size() - 1; i++) {
  272 | 				if (r >= ranges.get(i) && r < ranges.get(i + 1)) {
  273 | 					String routeId = config.rangeIndexes.get(i);
  274 | 					weights.put(group, routeId);
  275 | 					break;
  276 | 				}
  277 | 			}
  278 | 		}
  279 | 
  280 | 		if (log.isTraceEnabled()) {
  281 | 			log.trace("Weights attr: " + weights);
  282 | 		}
  283 | 
  284 | 		return chain.filter(exchange);
  285 | 	}
  286 | 
  287 | 	/* for testing */ static class GroupWeightConfig {
  288 | 
  289 | 		String group;
  290 | 
  291 | 		LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();
  292 | 
  293 | 		LinkedHashMap<String, Double> normalizedWeights = new LinkedHashMap<>();
  294 | 
  295 | 		LinkedHashMap<Integer, String> rangeIndexes = new LinkedHashMap<>();
  296 | 
  297 | 		List<Double> ranges = new ArrayList<>();
  298 | 
  299 | 		GroupWeightConfig(String group) {
  300 | 			this.group = group;
  301 | 		}
  302 | 
  303 | 		GroupWeightConfig(GroupWeightConfig other) {
  304 | 			this.group = other.group;
  305 | 			this.weights = new LinkedHashMap<>(other.weights);
  306 | 			this.normalizedWeights = new LinkedHashMap<>(other.normalizedWeights);
  307 | 			this.rangeIndexes = new LinkedHashMap<>(other.rangeIndexes);
  308 | 		}
  309 | 
  310 | 		@Override
  311 | 		public String toString() {
  312 | 			return new ToStringCreator(this).append("group", group)
  313 | 				.append("weights", weights)
  314 | 				.append("normalizedWeights", normalizedWeights)
  315 | 				.append("rangeIndexes", rangeIndexes)
  316 | 				.toString();
  317 | 		}
  318 | 
  319 | 	}
  320 | 
  321 | }
```
