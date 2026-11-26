<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/WeightRoutePredicateFactory.java -->
<!-- 正文引用：[[源码:WeightRoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: WeightRoutePredicateFactory.java#L起-L止) -->

# WeightRoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/WeightRoutePredicateFactory.java` |
| 行数 | 130 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:36:10.564Z |

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
   17 | package org.springframework.cloud.gateway.handler.predicate;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.function.Predicate;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.cloud.gateway.event.WeightDefinedEvent;
   30 | import org.springframework.cloud.gateway.support.WeightConfig;
   31 | import org.springframework.context.ApplicationEventPublisher;
   32 | import org.springframework.context.ApplicationEventPublisherAware;
   33 | import org.springframework.web.server.ServerWebExchange;
   34 | 
   35 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR;
   36 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.WEIGHT_ATTR;
   37 | 
   38 | /**
   39 |  * @author Spencer Gibb
   40 |  */
   41 | // TODO: make this a generic Choose out of group predicate?
   42 | public class WeightRoutePredicateFactory extends AbstractRoutePredicateFactory<WeightConfig>
   43 | 		implements ApplicationEventPublisherAware {
   44 | 
   45 | 	/**
   46 | 	 * Weight config group key.
   47 | 	 */
   48 | 	public static final String GROUP_KEY = WeightConfig.CONFIG_PREFIX + ".group";
   49 | 
   50 | 	/**
   51 | 	 * Weight config weight key.
   52 | 	 */
   53 | 	public static final String WEIGHT_KEY = WeightConfig.CONFIG_PREFIX + ".weight";
   54 | 
   55 | 	private static final Log log = LogFactory.getLog(WeightRoutePredicateFactory.class);
   56 | 
   57 | 	private @Nullable ApplicationEventPublisher publisher;
   58 | 
   59 | 	public WeightRoutePredicateFactory() {
   60 | 		super(WeightConfig.class);
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
   65 | 		this.publisher = publisher;
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public List<String> shortcutFieldOrder() {
   70 | 		return Arrays.asList(GROUP_KEY, WEIGHT_KEY);
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public String shortcutFieldPrefix() {
   75 | 		return WeightConfig.CONFIG_PREFIX;
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public void beforeApply(WeightConfig config) {
   80 | 		if (publisher != null) {
   81 | 			publisher.publishEvent(new WeightDefinedEvent(this, config));
   82 | 		}
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public Predicate<ServerWebExchange> apply(WeightConfig config) {
   87 | 		return new GatewayPredicate() {
   88 | 			@Override
   89 | 			public boolean test(ServerWebExchange exchange) {
   90 | 				Map<String, String> weights = exchange.getAttributeOrDefault(WEIGHT_ATTR, Collections.emptyMap());
   91 | 
   92 | 				String routeId = exchange.getAttribute(GATEWAY_PREDICATE_ROUTE_ATTR);
   93 | 
   94 | 				if (routeId == null) {
   95 | 					return false;
   96 | 				}
   97 | 
   98 | 				// all calculations and comparison against random num happened in
   99 | 				// WeightCalculatorWebFilter
  100 | 				String group = config.getGroup();
  101 | 				if (weights.containsKey(group)) {
  102 | 
  103 | 					String chosenRoute = weights.get(group);
  104 | 					if (log.isTraceEnabled()) {
  105 | 						log.trace("in group weight: " + group + ", current route: " + routeId + ", chosen route: "
  106 | 								+ chosenRoute);
  107 | 					}
  108 | 
  109 | 					return routeId.equals(chosenRoute);
  110 | 				}
  111 | 				else if (log.isTraceEnabled()) {
  112 | 					log.trace("no weights found for group: " + group + ", current route: " + routeId);
  113 | 				}
  114 | 
  115 | 				return false;
  116 | 			}
  117 | 
  118 | 			@Override
  119 | 			public Object getConfig() {
  120 | 				return config;
  121 | 			}
  122 | 
  123 | 			@Override
  124 | 			public String toString() {
  125 | 				return String.format("Weight: %s %s", config.getGroup(), config.getWeight());
  126 | 			}
  127 | 		};
  128 | 	}
  129 | 
  130 | }
```
