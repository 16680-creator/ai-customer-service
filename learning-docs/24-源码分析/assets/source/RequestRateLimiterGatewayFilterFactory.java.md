<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/RequestRateLimiterGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:RequestRateLimiterGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: RequestRateLimiterGatewayFilterFactory.java#L起-L止) -->

# RequestRateLimiterGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/RequestRateLimiterGatewayFilterFactory.java` |
| 行数 | 239 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:36:10.216Z |

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
   19 | import java.util.Map;
   20 | import java.util.Objects;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | import reactor.core.publisher.Mono;
   24 | 
   25 | import org.springframework.boot.context.properties.ConfigurationProperties;
   26 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   27 | import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
   28 | import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
   29 | import org.springframework.cloud.gateway.route.Route;
   30 | import org.springframework.cloud.gateway.support.HasRouteId;
   31 | import org.springframework.cloud.gateway.support.HttpStatusHolder;
   32 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   33 | import org.springframework.http.HttpStatus;
   34 | import org.springframework.web.client.HttpClientErrorException;
   35 | 
   36 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;
   37 | 
   38 | /**
   39 |  * User Request Rate Limiter filter. See https://stripe.com/blog/rate-limiters and
   40 |  * https://gist.github.com/ptarjan/e38f45f2dfe601419ca3af937fff574d#file-1-check_request_rate_limiter-rb-L11-L34.
   41 |  */
   42 | @ConfigurationProperties("spring.cloud.gateway.server.webflux.filter.request-rate-limiter")
   43 | public class RequestRateLimiterGatewayFilterFactory
   44 | 		extends AbstractGatewayFilterFactory<RequestRateLimiterGatewayFilterFactory.Config> {
   45 | 
   46 | 	/**
   47 | 	 * Key-Resolver key.
   48 | 	 */
   49 | 	public static final String KEY_RESOLVER_KEY = "keyResolver";
   50 | 
   51 | 	private static final String EMPTY_KEY = "____EMPTY_KEY__";
   52 | 
   53 | 	private final RateLimiter defaultRateLimiter;
   54 | 
   55 | 	private final KeyResolver defaultKeyResolver;
   56 | 
   57 | 	/**
   58 | 	 * Switch to deny requests if the Key Resolver returns an empty key, defaults to true.
   59 | 	 */
   60 | 	private boolean denyEmptyKey = true;
   61 | 
   62 | 	/** HttpStatus to return when denyEmptyKey is true, defaults to FORBIDDEN. */
   63 | 	private String emptyKeyStatusCode = HttpStatus.FORBIDDEN.name();
   64 | 
   65 | 	/**
   66 | 	 * Switch to throw a {@link HttpClientErrorException} when the request is denied by
   67 | 	 * the RateLimiter, defaults to false.
   68 | 	 */
   69 | 	private boolean throwOnLimit = false;
   70 | 
   71 | 	public RequestRateLimiterGatewayFilterFactory(RateLimiter defaultRateLimiter, KeyResolver defaultKeyResolver) {
   72 | 		super(Config.class);
   73 | 		this.defaultRateLimiter = defaultRateLimiter;
   74 | 		this.defaultKeyResolver = defaultKeyResolver;
   75 | 	}
   76 | 
   77 | 	public KeyResolver getDefaultKeyResolver() {
   78 | 		return defaultKeyResolver;
   79 | 	}
   80 | 
   81 | 	public RateLimiter getDefaultRateLimiter() {
   82 | 		return defaultRateLimiter;
   83 | 	}
   84 | 
   85 | 	public boolean isDenyEmptyKey() {
   86 | 		return denyEmptyKey;
   87 | 	}
   88 | 
   89 | 	public void setDenyEmptyKey(boolean denyEmptyKey) {
   90 | 		this.denyEmptyKey = denyEmptyKey;
   91 | 	}
   92 | 
   93 | 	public String getEmptyKeyStatusCode() {
   94 | 		return emptyKeyStatusCode;
   95 | 	}
   96 | 
   97 | 	public void setEmptyKeyStatusCode(String emptyKeyStatusCode) {
   98 | 		this.emptyKeyStatusCode = emptyKeyStatusCode;
   99 | 	}
  100 | 
  101 | 	public boolean isThrowOnLimit() {
  102 | 		return throwOnLimit;
  103 | 	}
  104 | 
  105 | 	public void setThrowOnLimit(boolean throwOnLimit) {
  106 | 		this.throwOnLimit = throwOnLimit;
  107 | 	}
  108 | 
  109 | 	@SuppressWarnings("unchecked")
  110 | 	@Override
  111 | 	public GatewayFilter apply(Config config) {
  112 | 		KeyResolver resolver = getOrDefault(config.keyResolver, defaultKeyResolver);
  113 | 		RateLimiter<Object> limiter = getOrDefault(config.rateLimiter, defaultRateLimiter);
  114 | 		boolean denyEmpty = getOrDefault(config.denyEmptyKey, this.denyEmptyKey);
  115 | 		HttpStatusHolder emptyKeyStatus = HttpStatusHolder
  116 | 			.parse(getOrDefault(config.emptyKeyStatus, this.emptyKeyStatusCode));
  117 | 		boolean throwLimit = getOrDefault(config.throwOnLimit, this.throwOnLimit);
  118 | 
  119 | 		return (exchange, chain) -> resolver.resolve(exchange).defaultIfEmpty(EMPTY_KEY).flatMap(key -> {
  120 | 			if (EMPTY_KEY.equals(key)) {
  121 | 				if (denyEmpty) {
  122 | 					setResponseStatus(exchange, emptyKeyStatus);
  123 | 					return exchange.getResponse().setComplete();
  124 | 				}
  125 | 				return chain.filter(exchange);
  126 | 			}
  127 | 			String routeId = config.getRouteId();
  128 | 			if (routeId == null) {
  129 | 				Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
  130 | 				routeId = Objects.requireNonNull(route, "Route not found").getId();
  131 | 			}
  132 | 			return limiter.isAllowed(routeId, key).flatMap(response -> {
  133 | 
  134 | 				for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
  135 | 					exchange.getResponse().getHeaders().add(header.getKey(), header.getValue());
  136 | 				}
  137 | 
  138 | 				if (response.isAllowed()) {
  139 | 					return chain.filter(exchange);
  140 | 				}
  141 | 
  142 | 				if (throwLimit) {
  143 | 					return Mono.error(HttpClientErrorException.create(config.getStatusCode(), "Too Many Requests",
  144 | 							exchange.getResponse().getHeaders(), null, null));
  145 | 				}
  146 | 
  147 | 				setResponseStatus(exchange, config.getStatusCode());
  148 | 				return exchange.getResponse().setComplete();
  149 | 			});
  150 | 		});
  151 | 	}
  152 | 
  153 | 	private <T> T getOrDefault(@Nullable T configValue, T defaultValue) {
  154 | 		return (configValue != null) ? configValue : defaultValue;
  155 | 	}
  156 | 
  157 | 	public static class Config implements HasRouteId {
  158 | 
  159 | 		private @Nullable KeyResolver keyResolver;
  160 | 
  161 | 		private @Nullable RateLimiter rateLimiter;
  162 | 
  163 | 		private HttpStatus statusCode = HttpStatus.TOO_MANY_REQUESTS;
  164 | 
  165 | 		private @Nullable Boolean denyEmptyKey;
  166 | 
  167 | 		private @Nullable String emptyKeyStatus;
  168 | 
  169 | 		private @Nullable Boolean throwOnLimit;
  170 | 
  171 | 		private @Nullable String routeId;
  172 | 
  173 | 		public @Nullable KeyResolver getKeyResolver() {
  174 | 			return keyResolver;
  175 | 		}
  176 | 
  177 | 		public Config setKeyResolver(KeyResolver keyResolver) {
  178 | 			this.keyResolver = keyResolver;
  179 | 			return this;
  180 | 		}
  181 | 
  182 | 		public @Nullable RateLimiter getRateLimiter() {
  183 | 			return rateLimiter;
  184 | 		}
  185 | 
  186 | 		public Config setRateLimiter(RateLimiter rateLimiter) {
  187 | 			this.rateLimiter = rateLimiter;
  188 | 			return this;
  189 | 		}
  190 | 
  191 | 		public HttpStatus getStatusCode() {
  192 | 			return statusCode;
  193 | 		}
  194 | 
  195 | 		public Config setStatusCode(HttpStatus statusCode) {
  196 | 			this.statusCode = statusCode;
  197 | 			return this;
  198 | 		}
  199 | 
  200 | 		public @Nullable Boolean getDenyEmptyKey() {
  201 | 			return denyEmptyKey;
  202 | 		}
  203 | 
  204 | 		public Config setDenyEmptyKey(Boolean denyEmptyKey) {
  205 | 			this.denyEmptyKey = denyEmptyKey;
  206 | 			return this;
  207 | 		}
  208 | 
  209 | 		public @Nullable String getEmptyKeyStatus() {
  210 | 			return emptyKeyStatus;
  211 | 		}
  212 | 
  213 | 		public Config setEmptyKeyStatus(String emptyKeyStatus) {
  214 | 			this.emptyKeyStatus = emptyKeyStatus;
  215 | 			return this;
  216 | 		}
  217 | 
  218 | 		public @Nullable Boolean getThrowOnLimit() {
  219 | 			return throwOnLimit;
  220 | 		}
  221 | 
  222 | 		public Config setThrowOnLimit(Boolean throwOnLimit) {
  223 | 			this.throwOnLimit = throwOnLimit;
  224 | 			return this;
  225 | 		}
  226 | 
  227 | 		@Override
  228 | 		public void setRouteId(String routeId) {
  229 | 			this.routeId = routeId;
  230 | 		}
  231 | 
  232 | 		@Override
  233 | 		public @Nullable String getRouteId() {
  234 | 			return this.routeId;
  235 | 		}
  236 | 
  237 | 	}
  238 | 
  239 | }
```
