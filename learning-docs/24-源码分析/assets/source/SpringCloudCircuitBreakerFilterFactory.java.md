<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/SpringCloudCircuitBreakerFilterFactory.java -->
<!-- 正文引用：[[源码:SpringCloudCircuitBreakerFilterFactory.java#L起-L止]] ；代码块头部：(src: SpringCloudCircuitBreakerFilterFactory.java#L起-L止) -->

# SpringCloudCircuitBreakerFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/SpringCloudCircuitBreakerFilterFactory.java` |
| 行数 | 255 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T08:49:13.841Z |

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
   19 | import java.net.URI;
   20 | import java.util.HashSet;
   21 | import java.util.List;
   22 | import java.util.Set;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | import reactor.core.publisher.Mono;
   27 | 
   28 | import org.springframework.beans.factory.ObjectProvider;
   29 | import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
   30 | import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
   31 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   32 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   33 | import org.springframework.cloud.gateway.support.HasRouteId;
   34 | import org.springframework.cloud.gateway.support.HttpStatusHolder;
   35 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   36 | import org.springframework.http.HttpStatus;
   37 | import org.springframework.http.HttpStatusCode;
   38 | import org.springframework.http.server.reactive.ServerHttpRequest;
   39 | import org.springframework.util.StringUtils;
   40 | import org.springframework.web.client.HttpStatusCodeException;
   41 | import org.springframework.web.reactive.DispatcherHandler;
   42 | import org.springframework.web.server.ServerWebExchange;
   43 | import org.springframework.web.util.UriComponentsBuilder;
   44 | 
   45 | import static java.util.Collections.singletonList;
   46 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   47 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR;
   48 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   49 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.containsEncodedParts;
   50 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.handle;
   51 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.reset;
   52 | 
   53 | /**
   54 |  * @author Ryan Baxter
   55 |  */
   56 | public abstract class SpringCloudCircuitBreakerFilterFactory
   57 | 		extends AbstractGatewayFilterFactory<SpringCloudCircuitBreakerFilterFactory.Config> {
   58 | 
   59 | 	/** CircuitBreaker component name. */
   60 | 	public static final String NAME = "CircuitBreaker";
   61 | 
   62 | 	private ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory;
   63 | 
   64 | 	private @Nullable ReactiveCircuitBreaker cb;
   65 | 
   66 | 	private final ObjectProvider<DispatcherHandler> dispatcherHandlerProvider;
   67 | 
   68 | 	// do not use this dispatcherHandler directly, use getDispatcherHandler() instead.
   69 | 	private volatile @Nullable DispatcherHandler dispatcherHandler;
   70 | 
   71 | 	public SpringCloudCircuitBreakerFilterFactory(ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory,
   72 | 			ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
   73 | 		super(Config.class);
   74 | 		this.reactiveCircuitBreakerFactory = reactiveCircuitBreakerFactory;
   75 | 		this.dispatcherHandlerProvider = dispatcherHandlerProvider;
   76 | 	}
   77 | 
   78 | 	private @Nullable DispatcherHandler getDispatcherHandler() {
   79 | 		if (dispatcherHandler == null) {
   80 | 			dispatcherHandler = dispatcherHandlerProvider.getIfAvailable();
   81 | 		}
   82 | 
   83 | 		return dispatcherHandler;
   84 | 	}
   85 | 
   86 | 	@Override
   87 | 	public List<String> shortcutFieldOrder() {
   88 | 		return singletonList(NAME_KEY);
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public GatewayFilter apply(Config config) {
   93 | 		if (config.getFallbackUri() != null) {
   94 | 			enableBodyCaching(config.getRouteId());
   95 | 		}
   96 | 		ReactiveCircuitBreaker cb = reactiveCircuitBreakerFactory.create(config.getId());
   97 | 		Set<HttpStatus> statuses = config.getStatusCodes()
   98 | 			.stream()
   99 | 			.map(HttpStatusHolder::parse)
  100 | 			.filter(statusHolder -> statusHolder.getHttpStatus() != null)
  101 | 			.map(HttpStatusHolder::getHttpStatus)
  102 | 			.collect(Collectors.toSet());
  103 | 
  104 | 		return new GatewayFilter() {
  105 | 			@Override
  106 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  107 | 				return cb.run(chain.filter(exchange).doOnSuccess(v -> {
  108 | 					if (statuses.contains(exchange.getResponse().getStatusCode())) {
  109 | 						HttpStatusCode status = java.util.Objects.requireNonNull(exchange.getResponse().getStatusCode(),
  110 | 								"statusCode must not be null");
  111 | 						throw new CircuitBreakerStatusCodeException(status);
  112 | 					}
  113 | 				}), t -> {
  114 | 					if (config.getFallbackUri() == null) {
  115 | 						return Mono.error(t);
  116 | 					}
  117 | 
  118 | 					exchange.getResponse().setStatusCode(null);
  119 | 
  120 | 					// TODO: copied from RouteToRequestUrlFilter
  121 | 					URI uri = exchange.getRequest().getURI();
  122 | 					// TODO: assume always?
  123 | 					boolean encoded = containsEncodedParts(uri);
  124 | 
  125 | 					String expandedFallbackUri = ServerWebExchangeUtils.expand(exchange,
  126 | 							config.getFallbackUri().getPath());
  127 | 					String fullFallbackUri = String.format("%s:%s", config.getFallbackUri().getScheme(),
  128 | 							expandedFallbackUri);
  129 | 					URI requestUrl = UriComponentsBuilder.fromUri(uri)
  130 | 						.host(null)
  131 | 						.port(null)
  132 | 						.uri(URI.create(fullFallbackUri))
  133 | 						.scheme(null)
  134 | 						.build(encoded)
  135 | 						.toUri();
  136 | 
  137 | 					exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
  138 | 					addExceptionDetails(t, exchange);
  139 | 
  140 | 					// Reset the exchange
  141 | 					reset(exchange);
  142 | 
  143 | 					ServerHttpRequest request = exchange.getRequest().mutate().uri(requestUrl).build();
  144 | 					return handle(getDispatcherHandler(), exchange.mutate().request(request).build());
  145 | 				}).onErrorResume(t -> handleErrorWithoutFallback(t, config.isResumeWithoutError()));
  146 | 			}
  147 | 
  148 | 			@Override
  149 | 			public String toString() {
  150 | 				String name = config.getName();
  151 | 				URI fallbackUri = config.getFallbackUri();
  152 | 				return filterToStringCreator(SpringCloudCircuitBreakerFilterFactory.this)
  153 | 					.append("name", name != null ? name : "")
  154 | 					.append("fallback", fallbackUri != null ? fallbackUri : "")
  155 | 					.toString();
  156 | 			}
  157 | 		};
  158 | 	}
  159 | 
  160 | 	protected abstract Mono<Void> handleErrorWithoutFallback(Throwable t, boolean resumeWithoutError);
  161 | 
  162 | 	private void addExceptionDetails(Throwable t, ServerWebExchange exchange) {
  163 | 		if (t != null) {
  164 | 			exchange.getAttributes().put(CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR, t);
  165 | 		}
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public String name() {
  170 | 		return NAME;
  171 | 	}
  172 | 
  173 | 	public static class Config implements HasRouteId {
  174 | 
  175 | 		private @Nullable String name;
  176 | 
  177 | 		private @Nullable URI fallbackUri;
  178 | 
  179 | 		private @Nullable String routeId;
  180 | 
  181 | 		private Set<String> statusCodes = new HashSet<>();
  182 | 
  183 | 		private boolean resumeWithoutError = false;
  184 | 
  185 | 		@Override
  186 | 		public void setRouteId(String routeId) {
  187 | 			this.routeId = routeId;
  188 | 		}
  189 | 
  190 | 		public @Nullable String getRouteId() {
  191 | 			return routeId;
  192 | 		}
  193 | 
  194 | 		public @Nullable URI getFallbackUri() {
  195 | 			return fallbackUri;
  196 | 		}
  197 | 
  198 | 		public Config setFallbackUri(URI fallbackUri) {
  199 | 			this.fallbackUri = fallbackUri;
  200 | 			return this;
  201 | 		}
  202 | 
  203 | 		public Config setFallbackUri(String fallbackUri) {
  204 | 			return setFallbackUri(URI.create(fallbackUri));
  205 | 		}
  206 | 
  207 | 		public @Nullable String getName() {
  208 | 			return name;
  209 | 		}
  210 | 
  211 | 		public Config setName(String name) {
  212 | 			this.name = name;
  213 | 			return this;
  214 | 		}
  215 | 
  216 | 		public @Nullable String getId() {
  217 | 			if (!StringUtils.hasText(name) && StringUtils.hasText(routeId)) {
  218 | 				return routeId;
  219 | 			}
  220 | 			return name;
  221 | 		}
  222 | 
  223 | 		public Set<String> getStatusCodes() {
  224 | 			return statusCodes;
  225 | 		}
  226 | 
  227 | 		public Config setStatusCodes(Set<String> statusCodes) {
  228 | 			this.statusCodes = statusCodes;
  229 | 			return this;
  230 | 		}
  231 | 
  232 | 		public Config addStatusCode(String statusCode) {
  233 | 			this.statusCodes.add(statusCode);
  234 | 			return this;
  235 | 		}
  236 | 
  237 | 		public boolean isResumeWithoutError() {
  238 | 			return resumeWithoutError;
  239 | 		}
  240 | 
  241 | 		public void setResumeWithoutError(boolean resumeWithoutError) {
  242 | 			this.resumeWithoutError = resumeWithoutError;
  243 | 		}
  244 | 
  245 | 	}
  246 | 
  247 | 	public class CircuitBreakerStatusCodeException extends HttpStatusCodeException {
  248 | 
  249 | 		public CircuitBreakerStatusCodeException(HttpStatusCode statusCode) {
  250 | 			super(statusCode);
  251 | 		}
  252 | 
  253 | 	}
  254 | 
  255 | }
```
