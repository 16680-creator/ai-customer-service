<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ratelimit/RedisRateLimiter.java -->
<!-- 正文引用：[[源码:RedisRateLimiter.java#L起-L止]] ；代码块头部：(src: RedisRateLimiter.java#L起-L止) -->

# RedisRateLimiter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ratelimit/RedisRateLimiter.java` |
| 行数 | 379 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:36:10.346Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | import java.util.concurrent.atomic.AtomicBoolean;
   26 | 
   27 | import jakarta.validation.constraints.Min;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | import reactor.core.publisher.Flux;
   32 | import reactor.core.publisher.Mono;
   33 | 
   34 | import org.springframework.beans.BeansException;
   35 | import org.springframework.boot.context.properties.ConfigurationProperties;
   36 | import org.springframework.cloud.gateway.config.GatewayProperties;
   37 | import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
   38 | import org.springframework.cloud.gateway.support.ConfigurationService;
   39 | import org.springframework.context.ApplicationContext;
   40 | import org.springframework.context.ApplicationContextAware;
   41 | import org.springframework.core.style.ToStringCreator;
   42 | import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
   43 | import org.springframework.data.redis.core.script.RedisScript;
   44 | import org.springframework.util.Assert;
   45 | import org.springframework.validation.annotation.Validated;
   46 | 
   47 | /**
   48 |  * See https://stripe.com/blog/rate-limiters and
   49 |  * https://gist.github.com/ptarjan/e38f45f2dfe601419ca3af937fff574d#file-1-check_request_rate_limiter-rb-L11-L34.
   50 |  *
   51 |  * @author Spencer Gibb
   52 |  * @author Ronny Bräunlich
   53 |  * @author Denis Cutic
   54 |  * @author Andrey Muchnik
   55 |  */
   56 | @ConfigurationProperties(GatewayProperties.PREFIX + ".redis-rate-limiter")
   57 | public class RedisRateLimiter extends AbstractRateLimiter<RedisRateLimiter.Config> implements ApplicationContextAware {
   58 | 
   59 | 	/**
   60 | 	 * Redis Rate Limiter property name.
   61 | 	 */
   62 | 	public static final String CONFIGURATION_PROPERTY_NAME = "redis-rate-limiter";
   63 | 
   64 | 	/**
   65 | 	 * Redis Script name.
   66 | 	 */
   67 | 	public static final String REDIS_SCRIPT_NAME = "redisRequestRateLimiterScript";
   68 | 
   69 | 	/**
   70 | 	 * Remaining Rate Limit header name.
   71 | 	 */
   72 | 	public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
   73 | 
   74 | 	/**
   75 | 	 * Replenish Rate Limit header name.
   76 | 	 */
   77 | 	public static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
   78 | 
   79 | 	/**
   80 | 	 * Burst Capacity header name.
   81 | 	 */
   82 | 	public static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";
   83 | 
   84 | 	/**
   85 | 	 * Requested Tokens header name.
   86 | 	 */
   87 | 	public static final String REQUESTED_TOKENS_HEADER = "X-RateLimit-Requested-Tokens";
   88 | 
   89 | 	/**
   90 | 	 * Constant representing the maximum number that can be safely used in Redis scripts.
   91 | 	 * This is the largest number that can be represented accurately in Java's `long` type
   92 | 	 * and is equal to 2^53 - 1, which is the largest number that Lua can reliably handle.
   93 | 	 */
   94 | 	private static final Long REDIS_LUA_MAX_SAFE_INTEGER = 9007199254740991L;
   95 | 
   96 | 	private Log log = LogFactory.getLog(getClass());
   97 | 
   98 | 	private @Nullable ReactiveStringRedisTemplate redisTemplate;
   99 | 
  100 | 	private @Nullable RedisScript<List<Long>> script;
  101 | 
  102 | 	private AtomicBoolean initialized = new AtomicBoolean(false);
  103 | 
  104 | 	private @Nullable Config defaultConfig;
  105 | 
  106 | 	// configuration properties
  107 | 	/**
  108 | 	 * Whether or not to include headers containing rate limiter information, defaults to
  109 | 	 * true.
  110 | 	 */
  111 | 	private boolean includeHeaders = true;
  112 | 
  113 | 	/**
  114 | 	 * The name of the header that returns number of remaining requests during the current
  115 | 	 * second.
  116 | 	 */
  117 | 	private String remainingHeader = REMAINING_HEADER;
  118 | 
  119 | 	/** The name of the header that returns the replenish rate configuration. */
  120 | 	private String replenishRateHeader = REPLENISH_RATE_HEADER;
  121 | 
  122 | 	/** The name of the header that returns the burst capacity configuration. */
  123 | 	private String burstCapacityHeader = BURST_CAPACITY_HEADER;
  124 | 
  125 | 	/** The name of the header that returns the requested tokens configuration. */
  126 | 	private String requestedTokensHeader = REQUESTED_TOKENS_HEADER;
  127 | 
  128 | 	public RedisRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script,
  129 | 			ConfigurationService configurationService) {
  130 | 		super(Config.class, CONFIGURATION_PROPERTY_NAME, configurationService);
  131 | 		this.redisTemplate = redisTemplate;
  132 | 		this.script = script;
  133 | 		this.initialized.compareAndSet(false, true);
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * This creates an instance with default static configuration, useful in Java DSL.
  138 | 	 * @param defaultReplenishRate how many tokens per second in token-bucket algorithm.
  139 | 	 * @param defaultBurstCapacity how many tokens the bucket can hold in token-bucket
  140 | 	 * algorithm.
  141 | 	 */
  142 | 	public RedisRateLimiter(int defaultReplenishRate, long defaultBurstCapacity) {
  143 | 		super(Config.class, CONFIGURATION_PROPERTY_NAME, (ConfigurationService) null);
  144 | 		this.defaultConfig = new Config().setReplenishRate(defaultReplenishRate).setBurstCapacity(defaultBurstCapacity);
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * This creates an instance with default static configuration, useful in Java DSL.
  149 | 	 * @param defaultReplenishRate how many tokens per second in token-bucket algorithm.
  150 | 	 * @param defaultBurstCapacity how many tokens the bucket can hold in token-bucket
  151 | 	 * algorithm.
  152 | 	 * @param defaultRequestedTokens how many tokens are requested per request.
  153 | 	 */
  154 | 	public RedisRateLimiter(int defaultReplenishRate, long defaultBurstCapacity, int defaultRequestedTokens) {
  155 | 		this(defaultReplenishRate, defaultBurstCapacity);
  156 | 		Objects.requireNonNull(this.defaultConfig, "defaultConfig may not be null");
  157 | 		this.defaultConfig.setRequestedTokens(defaultRequestedTokens);
  158 | 	}
  159 | 
  160 | 	static List<String> getKeys(String id, String routeId) {
  161 | 		// use `{}` around keys to use Redis Key hash tags
  162 | 		// this allows for using redis cluster
  163 | 
  164 | 		// Make a unique key per user and route.
  165 | 		String prefix = "request_rate_limiter.{" + routeId + "." + id + "}.";
  166 | 
  167 | 		// You need two Redis keys for Token Bucket.
  168 | 		String tokenKey = prefix + "tokens";
  169 | 		String timestampKey = prefix + "timestamp";
  170 | 		return Arrays.asList(tokenKey, timestampKey);
  171 | 	}
  172 | 
  173 | 	public boolean isIncludeHeaders() {
  174 | 		return includeHeaders;
  175 | 	}
  176 | 
  177 | 	public void setIncludeHeaders(boolean includeHeaders) {
  178 | 		this.includeHeaders = includeHeaders;
  179 | 	}
  180 | 
  181 | 	public String getRemainingHeader() {
  182 | 		return remainingHeader;
  183 | 	}
  184 | 
  185 | 	public void setRemainingHeader(String remainingHeader) {
  186 | 		this.remainingHeader = remainingHeader;
  187 | 	}
  188 | 
  189 | 	public String getReplenishRateHeader() {
  190 | 		return replenishRateHeader;
  191 | 	}
  192 | 
  193 | 	public void setReplenishRateHeader(String replenishRateHeader) {
  194 | 		this.replenishRateHeader = replenishRateHeader;
  195 | 	}
  196 | 
  197 | 	public String getBurstCapacityHeader() {
  198 | 		return burstCapacityHeader;
  199 | 	}
  200 | 
  201 | 	public void setBurstCapacityHeader(String burstCapacityHeader) {
  202 | 		this.burstCapacityHeader = burstCapacityHeader;
  203 | 	}
  204 | 
  205 | 	public String getRequestedTokensHeader() {
  206 | 		return requestedTokensHeader;
  207 | 	}
  208 | 
  209 | 	public void setRequestedTokensHeader(String requestedTokensHeader) {
  210 | 		this.requestedTokensHeader = requestedTokensHeader;
  211 | 	}
  212 | 
  213 | 	/**
  214 | 	 * Used when setting default configuration in constructor.
  215 | 	 * @param context the ApplicationContext object to be used by this object
  216 | 	 * @throws BeansException if thrown by application context methods
  217 | 	 */
  218 | 	@Override
  219 | 	@SuppressWarnings("unchecked")
  220 | 	public void setApplicationContext(ApplicationContext context) throws BeansException {
  221 | 		if (initialized.compareAndSet(false, true)) {
  222 | 			if (this.redisTemplate == null) {
  223 | 				this.redisTemplate = context.getBean(ReactiveStringRedisTemplate.class);
  224 | 			}
  225 | 			this.script = context.getBean(REDIS_SCRIPT_NAME, RedisScript.class);
  226 | 			if (context.getBeanNamesForType(ConfigurationService.class).length > 0) {
  227 | 				setConfigurationService(context.getBean(ConfigurationService.class));
  228 | 			}
  229 | 		}
  230 | 	}
  231 | 
  232 | 	@SuppressWarnings("NullAway")
  233 | 	/* for testing */
  234 | 	Config getDefaultConfig() {
  235 | 		return defaultConfig;
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * This uses a basic token bucket algorithm and relies on the fact that Redis scripts
  240 | 	 * execute atomically. No other operations can run between fetching the count and
  241 | 	 * writing the new count.
  242 | 	 */
  243 | 	@Override
  244 | 	@SuppressWarnings("unchecked")
  245 | 	public Mono<Response> isAllowed(String routeId, String id) {
  246 | 		if (!this.initialized.get()) {
  247 | 			throw new IllegalStateException("RedisRateLimiter is not initialized");
  248 | 		}
  249 | 
  250 | 		Config routeConfig = loadConfiguration(routeId);
  251 | 
  252 | 		// How many requests per second do you want a user to be allowed to do?
  253 | 		int replenishRate = routeConfig.getReplenishRate();
  254 | 
  255 | 		// How much bursting do you want to allow?
  256 | 		long burstCapacity = routeConfig.getBurstCapacity();
  257 | 
  258 | 		// How many tokens are requested per request?
  259 | 		int requestedTokens = routeConfig.getRequestedTokens();
  260 | 
  261 | 		try {
  262 | 			List<String> keys = getKeys(id, routeId);
  263 | 
  264 | 			// The arguments to the LUA script. time() returns unixtime in seconds.
  265 | 			List<String> scriptArgs = Arrays.asList(replenishRate + "", burstCapacity + "", "", requestedTokens + "");
  266 | 			// allowed, tokens_left = redis.eval(SCRIPT, keys, args)
  267 | 			Objects.requireNonNull(this.redisTemplate, "redisTemplate may not be null");
  268 | 			Objects.requireNonNull(this.script, "script may not be null");
  269 | 			Flux<List<Long>> flux = this.redisTemplate.execute(this.script, keys, scriptArgs);
  270 | 			// .log("redisratelimiter", Level.FINER);
  271 | 			return flux.onErrorResume(throwable -> {
  272 | 				log.error("Error calling rate limiter lua", throwable);
  273 | 				return Flux.just(Arrays.asList(1L, -1L));
  274 | 			}).reduce(new ArrayList<Long>(), (longs, l) -> {
  275 | 				longs.addAll(l);
  276 | 				return longs;
  277 | 			}).map(results -> {
  278 | 				boolean allowed = results.get(0) == 1L;
  279 | 				Long tokensLeft = results.get(1);
  280 | 
  281 | 				Response response = new Response(allowed, getHeaders(routeConfig, tokensLeft));
  282 | 
  283 | 				if (log.isDebugEnabled()) {
  284 | 					log.debug("response: " + response);
  285 | 				}
  286 | 				return response;
  287 | 			});
  288 | 		}
  289 | 		catch (Exception e) {
  290 | 			/*
  291 | 			 * We don't want a hard dependency on Redis to allow traffic. Make sure to set
  292 | 			 * an alert so you know if this is happening too much. Stripe's observed
  293 | 			 * failure rate is 0.01%.
  294 | 			 */
  295 | 			log.error("Error determining if user allowed from redis", e);
  296 | 		}
  297 | 		return Mono.just(new Response(true, getHeaders(routeConfig, -1L)));
  298 | 	}
  299 | 
  300 | 	@SuppressWarnings("NullAway")
  301 | 	/* for testing */ Config loadConfiguration(String routeId) {
  302 | 		Config routeConfig = getConfig().getOrDefault(routeId, defaultConfig);
  303 | 
  304 | 		if (routeConfig == null) {
  305 | 			routeConfig = getConfig().get(RouteDefinitionRouteLocator.DEFAULT_FILTERS);
  306 | 		}
  307 | 
  308 | 		if (routeConfig == null) {
  309 | 			throw new IllegalArgumentException("No Configuration found for route " + routeId + " or defaultFilters");
  310 | 		}
  311 | 		return routeConfig;
  312 | 	}
  313 | 
  314 | 	public Map<String, String> getHeaders(Config config, Long tokensLeft) {
  315 | 		Map<String, String> headers = new HashMap<>();
  316 | 		if (isIncludeHeaders()) {
  317 | 			headers.put(this.remainingHeader, tokensLeft.toString());
  318 | 			headers.put(this.replenishRateHeader, String.valueOf(config.getReplenishRate()));
  319 | 			headers.put(this.burstCapacityHeader, String.valueOf(config.getBurstCapacity()));
  320 | 			headers.put(this.requestedTokensHeader, String.valueOf(config.getRequestedTokens()));
  321 | 		}
  322 | 		return headers;
  323 | 	}
  324 | 
  325 | 	@Validated
  326 | 	public static class Config {
  327 | 
  328 | 		@Min(1)
  329 | 		private int replenishRate;
  330 | 
  331 | 		@Min(0)
  332 | 		private long burstCapacity = 1;
  333 | 
  334 | 		@Min(1)
  335 | 		private int requestedTokens = 1;
  336 | 
  337 | 		public int getReplenishRate() {
  338 | 			return replenishRate;
  339 | 		}
  340 | 
  341 | 		public Config setReplenishRate(int replenishRate) {
  342 | 			this.replenishRate = replenishRate;
  343 | 			return this;
  344 | 		}
  345 | 
  346 | 		public long getBurstCapacity() {
  347 | 			return burstCapacity;
  348 | 		}
  349 | 
  350 | 		public Config setBurstCapacity(long burstCapacity) {
  351 | 			Assert.isTrue(burstCapacity >= this.replenishRate, "BurstCapacity(" + burstCapacity
  352 | 					+ ") must be greater than or equal than replenishRate(" + this.replenishRate + ")");
  353 | 			Assert.isTrue(burstCapacity <= REDIS_LUA_MAX_SAFE_INTEGER, "BurstCapacity(" + burstCapacity
  354 | 					+ ") must not exceed the maximum allowed value of " + REDIS_LUA_MAX_SAFE_INTEGER);
  355 | 			this.burstCapacity = burstCapacity;
  356 | 			return this;
  357 | 		}
  358 | 
  359 | 		public int getRequestedTokens() {
  360 | 			return requestedTokens;
  361 | 		}
  362 | 
  363 | 		public Config setRequestedTokens(int requestedTokens) {
  364 | 			this.requestedTokens = requestedTokens;
  365 | 			return this;
  366 | 		}
  367 | 
  368 | 		@Override
  369 | 		public String toString() {
  370 | 			return new ToStringCreator(this).append("replenishRate", replenishRate)
  371 | 				.append("burstCapacity", burstCapacity)
  372 | 				.append("requestedTokens", requestedTokens)
  373 | 				.toString();
  374 | 
  375 | 		}
  376 | 
  377 | 	}
  378 | 
  379 | }
```
