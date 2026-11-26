<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ratelimit/Bucket4jRateLimiter.java -->
<!-- 正文引用：[[源码:Bucket4jRateLimiter.java#L起-L止]] ；代码块头部：(src: Bucket4jRateLimiter.java#L起-L止) -->

# Bucket4jRateLimiter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ratelimit/Bucket4jRateLimiter.java` |
| 行数 | 267 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:50:10.767Z |

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
   17 | package org.springframework.cloud.gateway.filter.ratelimit;
   18 | 
   19 | import java.time.Duration;
   20 | import java.time.Instant;
   21 | import java.util.HashMap;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | import java.util.concurrent.CompletableFuture;
   25 | import java.util.function.Function;
   26 | import java.util.function.Supplier;
   27 | 
   28 | import io.github.bucket4j.Bandwidth;
   29 | import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderBuildStage;
   30 | import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderRefillStage;
   31 | import io.github.bucket4j.BucketConfiguration;
   32 | import io.github.bucket4j.ConsumptionProbe;
   33 | import io.github.bucket4j.distributed.AsyncBucketProxy;
   34 | import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
   35 | import org.apache.commons.logging.Log;
   36 | import org.apache.commons.logging.LogFactory;
   37 | import org.jspecify.annotations.Nullable;
   38 | import reactor.core.publisher.Mono;
   39 | 
   40 | import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
   41 | import org.springframework.cloud.gateway.support.ConfigurationService;
   42 | import org.springframework.core.style.ToStringCreator;
   43 | 
   44 | public class Bucket4jRateLimiter extends AbstractRateLimiter<Bucket4jRateLimiter.Config> {
   45 | 
   46 | 	/**
   47 | 	 * Default Header Name.
   48 | 	 */
   49 | 	public static final String DEFAULT_HEADER_NAME = "X-RateLimit-Remaining";
   50 | 
   51 | 	/**
   52 | 	 * Redis Rate Limiter property name.
   53 | 	 */
   54 | 	public static final String CONFIGURATION_PROPERTY_NAME = "bucket4j-rate-limiter";
   55 | 
   56 | 	private final Log log = LogFactory.getLog(getClass());
   57 | 
   58 | 	private final AsyncProxyManager<String> proxyManager;
   59 | 
   60 | 	private Config defaultConfig = new Config();
   61 | 
   62 | 	public Bucket4jRateLimiter(AsyncProxyManager<String> proxyManager, ConfigurationService configurationService) {
   63 | 		super(Config.class, CONFIGURATION_PROPERTY_NAME, configurationService);
   64 | 		this.proxyManager = proxyManager;
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	public Mono<Response> isAllowed(String routeId, String id) {
   69 | 		Config routeConfig = loadRouteConfiguration(routeId);
   70 | 
   71 | 		AsyncBucketProxy bucket = proxyManager.builder().build(id, routeConfig.getConfigurationSupplier());
   72 | 		CompletableFuture<ConsumptionProbe> bucketFuture = bucket
   73 | 			.tryConsumeAndReturnRemaining(routeConfig.getRequestedTokens());
   74 | 		return Mono.fromFuture(bucketFuture).onErrorResume(throwable -> {
   75 | 			if (log.isDebugEnabled()) {
   76 | 				log.debug("Error calling Bucket4J rate limiter", throwable);
   77 | 			}
   78 | 			return Mono.just(ConsumptionProbe.rejected(-1, -1, -1));
   79 | 		}).map(consumptionProbe -> {
   80 | 			boolean allowed = consumptionProbe.isConsumed();
   81 | 			long remainingTokens = consumptionProbe.getRemainingTokens();
   82 | 			Response response = new Response(allowed, getHeaders(routeConfig, remainingTokens));
   83 | 
   84 | 			if (log.isDebugEnabled()) {
   85 | 				log.debug("response: " + response);
   86 | 			}
   87 | 			return response;
   88 | 		});
   89 | 	}
   90 | 
   91 | 	protected Config loadRouteConfiguration(String routeId) {
   92 | 		Config routeConfig = getConfig().getOrDefault(routeId, defaultConfig);
   93 | 
   94 | 		if (routeConfig == null) {
   95 | 			routeConfig = getConfig().get(RouteDefinitionRouteLocator.DEFAULT_FILTERS);
   96 | 		}
   97 | 
   98 | 		if (routeConfig == null) {
   99 | 			throw new IllegalArgumentException("No Configuration found for route " + routeId + " or defaultFilters");
  100 | 		}
  101 | 		return routeConfig;
  102 | 	}
  103 | 
  104 | 	public Map<String, String> getHeaders(Config config, Long tokensLeft) {
  105 | 		Map<String, String> headers = new HashMap<>();
  106 | 		// TODO: configurable isIncludeHeaders?
  107 | 		// if (isIncludeHeaders()) {
  108 | 		headers.put(config.getHeaderName(), tokensLeft.toString());
  109 | 		// }
  110 | 		return headers;
  111 | 	}
  112 | 
  113 | 	public static class Config {
  114 | 
  115 | 		private static final Function<Config, BucketConfiguration> DEFAULT_CONFIGURATION_BUILDER = config -> {
  116 | 			BandwidthBuilderRefillStage bandwidth = Bandwidth.builder().capacity(config.getCapacity());
  117 | 
  118 | 			long refillTokens = config.getRefillTokens() == null ? config.getCapacity() : config.getRefillTokens();
  119 | 
  120 | 			BandwidthBuilderBuildStage refill = switch (config.getRefillStyle()) {
  121 | 				case GREEDY -> bandwidth.refillGreedy(refillTokens, config.getRefillPeriod());
  122 | 				case INTERVALLY -> bandwidth.refillIntervally(refillTokens, config.getRefillPeriod());
  123 | 				case INTERVALLY_ALIGNED -> bandwidth.refillIntervallyAligned(refillTokens, config.getRefillPeriod(),
  124 | 						config.getTimeOfFirstRefill());
  125 | 			};
  126 | 
  127 | 			return BucketConfiguration.builder().addLimit(refill.build()).build();
  128 | 		};
  129 | 
  130 | 		long capacity;
  131 | 
  132 | 		Function<Config, BucketConfiguration> configurationBuilder = DEFAULT_CONFIGURATION_BUILDER;
  133 | 
  134 | 		protected @Nullable Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier;
  135 | 
  136 | 		String headerName = DEFAULT_HEADER_NAME;
  137 | 
  138 | 		protected @Nullable Duration refillPeriod;
  139 | 
  140 | 		RefillStyle refillStyle = RefillStyle.GREEDY;
  141 | 
  142 | 		protected @Nullable Long refillTokens;
  143 | 
  144 | 		long requestedTokens = 1;
  145 | 
  146 | 		// for RefillStyle.INTERVALLY_ALIGNED
  147 | 		protected @Nullable Instant timeOfFirstRefill;
  148 | 
  149 | 		public long getCapacity() {
  150 | 			return capacity;
  151 | 		}
  152 | 
  153 | 		public Config setCapacity(long capacity) {
  154 | 			this.capacity = capacity;
  155 | 			return this;
  156 | 		}
  157 | 
  158 | 		public Function<Config, BucketConfiguration> getConfigurationBuilder() {
  159 | 			return configurationBuilder;
  160 | 		}
  161 | 
  162 | 		public void setConfigurationBuilder(Function<Config, BucketConfiguration> configurationBuilder) {
  163 | 			Objects.requireNonNull(configurationBuilder, "configurationBuilder may not be null");
  164 | 			this.configurationBuilder = configurationBuilder;
  165 | 		}
  166 | 
  167 | 		public Supplier<CompletableFuture<BucketConfiguration>> getConfigurationSupplier() {
  168 | 			if (configurationSupplier != null) {
  169 | 				return configurationSupplier;
  170 | 			}
  171 | 			return () -> CompletableFuture.completedFuture(getConfigurationBuilder().apply(this));
  172 | 		}
  173 | 
  174 | 		public void setConfigurationSupplier(Function<Config, BucketConfiguration> configurationBuilder) {
  175 | 			Objects.requireNonNull(configurationBuilder, "configurationBuilder may not be null");
  176 | 			this.configurationBuilder = configurationBuilder;
  177 | 		}
  178 | 
  179 | 		public String getHeaderName() {
  180 | 			return headerName;
  181 | 		}
  182 | 
  183 | 		public Config setHeaderName(String headerName) {
  184 | 			Objects.requireNonNull(headerName, "headerName may not be null");
  185 | 			this.headerName = headerName;
  186 | 			return this;
  187 | 		}
  188 | 
  189 | 		public @Nullable Duration getRefillPeriod() {
  190 | 			return refillPeriod;
  191 | 		}
  192 | 
  193 | 		public Config setRefillPeriod(Duration refillPeriod) {
  194 | 			this.refillPeriod = refillPeriod;
  195 | 			return this;
  196 | 		}
  197 | 
  198 | 		public RefillStyle getRefillStyle() {
  199 | 			return refillStyle;
  200 | 		}
  201 | 
  202 | 		public Config setRefillStyle(RefillStyle refillStyle) {
  203 | 			this.refillStyle = refillStyle;
  204 | 			return this;
  205 | 		}
  206 | 
  207 | 		public @Nullable Long getRefillTokens() {
  208 | 			return refillTokens;
  209 | 		}
  210 | 
  211 | 		public Config setRefillTokens(Long refillTokens) {
  212 | 			this.refillTokens = refillTokens;
  213 | 			return this;
  214 | 		}
  215 | 
  216 | 		public long getRequestedTokens() {
  217 | 			return requestedTokens;
  218 | 		}
  219 | 
  220 | 		public Config setRequestedTokens(long requestedTokens) {
  221 | 			this.requestedTokens = requestedTokens;
  222 | 			return this;
  223 | 		}
  224 | 
  225 | 		public @Nullable Instant getTimeOfFirstRefill() {
  226 | 			return timeOfFirstRefill;
  227 | 		}
  228 | 
  229 | 		public Config setTimeOfFirstRefill(Instant timeOfFirstRefill) {
  230 | 			this.timeOfFirstRefill = timeOfFirstRefill;
  231 | 			return this;
  232 | 		}
  233 | 
  234 | 		public String toString() {
  235 | 			return new ToStringCreator(this).append("capacity", capacity)
  236 | 				.append("headerName", headerName)
  237 | 				.append("refillPeriod", refillPeriod)
  238 | 				.append("refillStyle", refillStyle)
  239 | 				.append("refillTokens", refillTokens)
  240 | 				.append("requestedTokens", requestedTokens)
  241 | 				.append("timeOfFirstRefill", timeOfFirstRefill)
  242 | 				.toString();
  243 | 		}
  244 | 
  245 | 	}
  246 | 
  247 | 	public enum RefillStyle {
  248 | 
  249 | 		/**
  250 | 		 * Greedy tries to add the tokens to the bucket as soon as possible.
  251 | 		 */
  252 | 		GREEDY,
  253 | 
  254 | 		/**
  255 | 		 * Intervally, in opposite to greedy, waits until the whole refillPeriod has
  256 | 		 * elapsed before refilling tokens.
  257 | 		 */
  258 | 		INTERVALLY,
  259 | 
  260 | 		/**
  261 | 		 * IntervallyAligned, like Intervally, but with a specified first refill time.
  262 | 		 */
  263 | 		INTERVALLY_ALIGNED;
  264 | 
  265 | 	}
  266 | 
  267 | }
```
