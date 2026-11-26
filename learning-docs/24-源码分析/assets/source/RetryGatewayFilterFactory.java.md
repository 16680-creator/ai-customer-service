<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/RetryGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:RetryGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: RetryGatewayFilterFactory.java#L起-L止) -->

# RetryGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/RetryGatewayFilterFactory.java` |
| 行数 | 593 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:47.386Z |

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
   19 | import java.io.IOException;
   20 | import java.time.Duration;
   21 | import java.time.Instant;
   22 | import java.util.ArrayList;
   23 | import java.util.Arrays;
   24 | import java.util.List;
   25 | import java.util.Objects;
   26 | import java.util.concurrent.ThreadLocalRandom;
   27 | import java.util.function.Function;
   28 | import java.util.function.Predicate;
   29 | import java.util.function.Supplier;
   30 | 
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | import org.jspecify.annotations.Nullable;
   34 | import org.reactivestreams.Publisher;
   35 | import reactor.core.publisher.Flux;
   36 | import reactor.core.publisher.Mono;
   37 | import reactor.netty.Connection;
   38 | import reactor.util.function.Tuples;
   39 | import reactor.util.retry.Retry;
   40 | import reactor.util.retry.RetryBackoffSpec;
   41 | import reactor.util.retry.RetrySpec;
   42 | 
   43 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   44 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   45 | import org.springframework.cloud.gateway.support.HasRouteId;
   46 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   47 | import org.springframework.cloud.gateway.support.TimeoutException;
   48 | import org.springframework.core.style.ToStringCreator;
   49 | import org.springframework.http.HttpMethod;
   50 | import org.springframework.http.HttpStatus;
   51 | import org.springframework.http.HttpStatus.Series;
   52 | import org.springframework.http.HttpStatusCode;
   53 | import org.springframework.util.Assert;
   54 | import org.springframework.web.server.ServerWebExchange;
   55 | 
   56 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   57 | 
   58 | public class RetryGatewayFilterFactory extends AbstractGatewayFilterFactory<RetryGatewayFilterFactory.RetryConfig> {
   59 | 
   60 | 	/**
   61 | 	 * Retry iteration key.
   62 | 	 */
   63 | 	public static final String RETRY_ITERATION_KEY = "retry_iteration";
   64 | 
   65 | 	private static final Log log = LogFactory.getLog(RetryGatewayFilterFactory.class);
   66 | 
   67 | 	public RetryGatewayFilterFactory() {
   68 | 		super(RetryConfig.class);
   69 | 	}
   70 | 
   71 | 	private static <T> List<T> toList(T... items) {
   72 | 		return new ArrayList<>(Arrays.asList(items));
   73 | 	}
   74 | 
   75 | 	@Override
   76 | 	public List<String> shortcutFieldOrder() {
   77 | 		return Arrays.asList("retries", "statuses", "methods", "backoff.firstBackoff", "backoff.maxBackoff",
   78 | 				"backoff.factor", "backoff.basedOnPreviousValue", "jitter.randomFactor", "timeout");
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public GatewayFilter apply(RetryConfig retryConfig) {
   83 | 		retryConfig.validate();
   84 | 		enableBodyCaching(retryConfig.getRouteId());
   85 | 
   86 | 		boolean hasStatusCodeRepeat = !retryConfig.getStatuses().isEmpty() || !retryConfig.getSeries().isEmpty();
   87 | 		boolean hasExceptionRetry = !retryConfig.getExceptions().isEmpty();
   88 | 
   89 | 		GatewayFilter gatewayFilter = (exchange, chain) -> {
   90 | 			trace("Entering retry-filter");
   91 | 
   92 | 			// chain.filter returns a Mono<Void>
   93 | 			Publisher<Void> publisher = chain.filter(exchange)
   94 | 				// .log("retry-filter", Level.INFO)
   95 | 				.doOnSuccess(aVoid -> updateIteration(exchange))
   96 | 				.doOnError(throwable -> updateIteration(exchange));
   97 | 
   98 | 			if (hasExceptionRetry) {
   99 | 				// retryWhen returns a Mono<Void>
  100 | 				// retry needs to go before repeat
  101 | 				publisher = ((Mono<Void>) publisher).retryWhen(buildExceptionRetry(exchange, retryConfig));
  102 | 			}
  103 | 			if (hasStatusCodeRepeat) {
  104 | 				// repeatWhen returns a Flux<Void>
  105 | 				// so this needs to be last and the variable a Publisher<Void>
  106 | 				publisher = ((Mono<Void>) publisher).repeatWhen(buildStatusCodeRepeat(exchange, retryConfig));
  107 | 			}
  108 | 
  109 | 			return Mono.fromDirect(publisher);
  110 | 		};
  111 | 
  112 | 		return new GatewayFilter() {
  113 | 			@Override
  114 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  115 | 				return gatewayFilter.filter(exchange, chain);
  116 | 			}
  117 | 
  118 | 			@Override
  119 | 			public String toString() {
  120 | 				return filterToStringCreator(RetryGatewayFilterFactory.this).append("routeId", retryConfig.getRouteId())
  121 | 					.append("retries", retryConfig.getRetries())
  122 | 					.append("series", retryConfig.getSeries())
  123 | 					.append("statuses", retryConfig.getStatuses())
  124 | 					.append("methods", retryConfig.getMethods())
  125 | 					.append("exceptions", retryConfig.getExceptions())
  126 | 					.append("backoff", retryConfig.getBackoff())
  127 | 					.append("jitter", retryConfig.getJitter())
  128 | 					.append("timeout", retryConfig.getTimeout())
  129 | 					.toString();
  130 | 			}
  131 | 		};
  132 | 	}
  133 | 
  134 | 	private boolean isRetryableStatusCode(ServerWebExchange exchange, RetryConfig retryConfig) {
  135 | 		HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
  136 | 
  137 | 		boolean retryableStatusCode = retryConfig.getStatuses().contains(statusCode);
  138 | 
  139 | 		// null status code might mean a network exception?
  140 | 		if (!retryableStatusCode && statusCode != null) {
  141 | 			// try the series
  142 | 			retryableStatusCode = false;
  143 | 			for (int i = 0; i < retryConfig.getSeries().size(); i++) {
  144 | 				if (statusCode instanceof HttpStatus) {
  145 | 					HttpStatus httpStatus = (HttpStatus) statusCode;
  146 | 					if (httpStatus.series().equals(retryConfig.getSeries().get(i))) {
  147 | 						retryableStatusCode = true;
  148 | 						break;
  149 | 					}
  150 | 				}
  151 | 			}
  152 | 		}
  153 | 
  154 | 		final boolean finalRetryableStatusCode = retryableStatusCode;
  155 | 		trace("retryableStatusCode: %b, statusCode %s, configured statuses %s, configured series %s",
  156 | 				() -> finalRetryableStatusCode, () -> statusCode, retryConfig::getStatuses, retryConfig::getSeries);
  157 | 		return retryableStatusCode;
  158 | 	}
  159 | 
  160 | 	private boolean isRetryableMethod(ServerWebExchange exchange, RetryConfig retryConfig) {
  161 | 		HttpMethod httpMethod = exchange.getRequest().getMethod();
  162 | 		boolean retryableMethod = retryConfig.getMethods().contains(httpMethod);
  163 | 
  164 | 		trace("retryableMethod: %b, httpMethod %s, configured methods %s", () -> retryableMethod, () -> httpMethod,
  165 | 				retryConfig::getMethods);
  166 | 		return retryableMethod;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Checks elapsed-time-plus-{@code upcomingDelay} against {@code timeout}, rather than
  171 | 	 * just elapsed time, so we stop before waiting on a delay that would blow the budget
  172 | 	 * rather than only after it's already blown.
  173 | 	 */
  174 | 	private boolean withinTimeout(Instant start, @Nullable Duration timeout, Duration upcomingDelay) {
  175 | 		return timeout == null || Duration.between(start, Instant.now()).plus(upcomingDelay).compareTo(timeout) < 0;
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Exponential backoff for the given (1-based) iteration, capped at {@code maxBackoff}
  180 | 	 * when configured. Hand-rolled because there's no reactor-core backoff builder usable
  181 | 	 * with {@code repeatWhen}, which is what the status-code repeat path needs.
  182 | 	 */
  183 | 	private Duration computeBackoff(BackoffConfig backoff, long iteration) {
  184 | 		Duration next = backoff.getFirstBackoff().multipliedBy((long) Math.pow(backoff.getFactor(), iteration - 1));
  185 | 		Duration max = backoff.getMaxBackoff();
  186 | 		if (max != null && next.compareTo(max) > 0) {
  187 | 			return max;
  188 | 		}
  189 | 		return next;
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Randomizes a backoff duration by +/- {@code randomFactor}, for the same reason as
  194 | 	 * {@link #computeBackoff}: no reactor-core built-in covers this for
  195 | 	 * {@code repeatWhen}.
  196 | 	 */
  197 | 	private Duration applyJitter(Duration backoff, JitterConfig jitter) {
  198 | 		long jitterOffset = (long) (backoff.toMillis() * jitter.getRandomFactor());
  199 | 		long lowBound = Math.max(backoff.toMillis() - jitterOffset, 0);
  200 | 		long highBound = backoff.toMillis() + jitterOffset;
  201 | 		return Duration.ofMillis(ThreadLocalRandom.current().nextLong(lowBound, highBound + 1));
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * The backoff delay before the given iteration's attempt, or {@link Duration#ZERO} if
  206 | 	 * no backoff is configured. Callers should compute this once per iteration and reuse
  207 | 	 * it for both the {@link #withinTimeout} check and the actual delay, since jitter is
  208 | 	 * randomized and calling this twice would produce two different values.
  209 | 	 */
  210 | 	private Duration nextDelay(long iteration, RetryConfig retryConfig) {
  211 | 		BackoffConfig backoff = retryConfig.getBackoff();
  212 | 		if (backoff == null) {
  213 | 			return Duration.ZERO;
  214 | 		}
  215 | 		Duration delay = computeBackoff(backoff, iteration);
  216 | 		JitterConfig jitter = retryConfig.getJitter();
  217 | 		if (jitter != null) {
  218 | 			delay = applyJitter(delay, jitter);
  219 | 		}
  220 | 		return delay;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * The next 1-based iteration number, derived from {@link #RETRY_ITERATION_KEY} rather
  225 | 	 * than the value emitted by {@code repeatWhen}'s companion {@code Flux<Long>} (which
  226 | 	 * counts items emitted by the source and is always {@code 0} for a
  227 | 	 * {@code Mono<Void>}).
  228 | 	 */
  229 | 	private long nextIteration(ServerWebExchange exchange) {
  230 | 		Integer currentIteration = exchange.getAttribute(RETRY_ITERATION_KEY);
  231 | 		return (currentIteration == null ? 0 : currentIteration) + 1;
  232 | 	}
  233 | 
  234 | 	private Function<Flux<Long>, Publisher<Long>> buildStatusCodeRepeat(ServerWebExchange exchange,
  235 | 			RetryConfig retryConfig) {
  236 | 		Instant start = Instant.now();
  237 | 		// the upcoming delay is computed once per iteration (up front) so the same value
  238 | 		// can be used both to
  239 | 		// decide whether it would blow the configured timeout budget, and, if not, as the
  240 | 		// actual delay applied
  241 | 		return companion -> companion.map(ignored -> nextIteration(exchange))
  242 | 			.map(iteration -> Tuples.of(iteration, nextDelay(iteration, retryConfig)))
  243 | 			.takeWhile(tuple -> !exceedsMaxIterations(exchange, retryConfig)
  244 | 					&& isRetryableStatusCode(exchange, retryConfig) && isRetryableMethod(exchange, retryConfig)
  245 | 					&& withinTimeout(start, retryConfig.getTimeout(), tuple.getT2()))
  246 | 			.doOnNext(tuple -> reset(exchange))
  247 | 			.concatMap(tuple -> tuple.getT2().isZero() ? Mono.just(tuple.getT1())
  248 | 					: Mono.delay(tuple.getT2()).thenReturn(tuple.getT1()));
  249 | 	}
  250 | 
  251 | 	private Retry buildExceptionRetry(ServerWebExchange exchange, RetryConfig retryConfig) {
  252 | 		Instant start = Instant.now();
  253 | 		Predicate<Throwable> predicate = exception -> {
  254 | 			if (exceedsMaxIterations(exchange, retryConfig) || !withinTimeout(start, retryConfig.getTimeout(),
  255 | 					nextDelay(nextIteration(exchange), retryConfig))) {
  256 | 				return false;
  257 | 			}
  258 | 
  259 | 			for (Class<? extends Throwable> retryableClass : retryConfig.getExceptions()) {
  260 | 				if (retryableClass.isInstance(exception)
  261 | 						|| (exception != null && retryableClass.isInstance(exception.getCause()))) {
  262 | 					trace("exception or its cause is retryable %s, configured exceptions %s",
  263 | 							() -> getExceptionNameWithCause(exception), retryConfig::getExceptions);
  264 | 					return isRetryableMethod(exchange, retryConfig);
  265 | 				}
  266 | 			}
  267 | 			trace("exception or its cause is not retryable %s, configured exceptions %s",
  268 | 					() -> getExceptionNameWithCause(exception), retryConfig::getExceptions);
  269 | 			return false;
  270 | 		};
  271 | 
  272 | 		BackoffConfig backoff = retryConfig.getBackoff();
  273 | 		if (backoff != null) {
  274 | 			RetryBackoffSpec spec = Retry.backoff(retryConfig.getRetries(), backoff.getFirstBackoff())
  275 | 				.multiplier(backoff.getFactor());
  276 | 			if (backoff.getMaxBackoff() != null) {
  277 | 				spec = spec.maxBackoff(backoff.getMaxBackoff());
  278 | 			}
  279 | 			JitterConfig jitter = retryConfig.getJitter();
  280 | 			spec = spec.jitter(jitter != null ? jitter.getRandomFactor() : 0d);
  281 | 			return spec.filter(predicate)
  282 | 				.doBeforeRetry(signal -> reset(exchange))
  283 | 				.onRetryExhaustedThrow((retrySpec, signal) -> signal.failure());
  284 | 		}
  285 | 
  286 | 		RetrySpec spec = Retry.max(retryConfig.getRetries());
  287 | 		return spec.filter(predicate)
  288 | 			.doBeforeRetry(signal -> reset(exchange))
  289 | 			.onRetryExhaustedThrow((retrySpec, signal) -> signal.failure());
  290 | 	}
  291 | 
  292 | 	private String getExceptionNameWithCause(Throwable exception) {
  293 | 		if (exception != null) {
  294 | 			StringBuilder builder = new StringBuilder(exception.getClass().getName());
  295 | 			Throwable cause = exception.getCause();
  296 | 			if (cause != null) {
  297 | 				builder.append("{cause=").append(cause.getClass().getName()).append("}");
  298 | 			}
  299 | 			return builder.toString();
  300 | 		}
  301 | 		else {
  302 | 			return "null";
  303 | 		}
  304 | 	}
  305 | 
  306 | 	public boolean exceedsMaxIterations(ServerWebExchange exchange, RetryConfig retryConfig) {
  307 | 		Integer iteration = exchange.getAttribute(RETRY_ITERATION_KEY);
  308 | 
  309 | 		// TODO: deal with null iteration
  310 | 		boolean exceeds = iteration != null && iteration >= retryConfig.getRetries();
  311 | 		trace("exceedsMaxIterations %b, iteration %d, configured retries %d", () -> exceeds, () -> iteration,
  312 | 				retryConfig::getRetries);
  313 | 		return exceeds;
  314 | 	}
  315 | 
  316 | 	private void reset(ServerWebExchange exchange) {
  317 | 		Connection conn = exchange.getAttribute(ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR);
  318 | 		if (conn != null) {
  319 | 			trace("disposing response connection before next iteration");
  320 | 		}
  321 | 		ServerWebExchangeUtils.reset(exchange);
  322 | 	}
  323 | 
  324 | 	public GatewayFilter apply(@Nullable String routeId, @Nullable Function<Flux<Long>, ? extends Publisher<?>> repeat,
  325 | 			@Nullable Retry retry) {
  326 | 		enableBodyCaching(routeId);
  327 | 		return (exchange, chain) -> {
  328 | 			trace("Entering retry-filter");
  329 | 
  330 | 			// chain.filter returns a Mono<Void>
  331 | 			Publisher<Void> publisher = chain.filter(exchange)
  332 | 				// .log("retry-filter", Level.INFO)
  333 | 				.doOnSuccess(aVoid -> updateIteration(exchange))
  334 | 				.doOnError(throwable -> updateIteration(exchange));
  335 | 
  336 | 			if (retry != null) {
  337 | 				// retryWhen returns a Mono<Void>
  338 | 				// retry needs to go before repeat
  339 | 				publisher = ((Mono<Void>) publisher).retryWhen(retry);
  340 | 			}
  341 | 			if (repeat != null) {
  342 | 				// repeatWhen returns a Flux<Void>
  343 | 				// so this needs to be last and the variable a Publisher<Void>
  344 | 				publisher = ((Mono<Void>) publisher).repeatWhen(repeat);
  345 | 			}
  346 | 
  347 | 			return Mono.fromDirect(publisher);
  348 | 		};
  349 | 	}
  350 | 
  351 | 	private void updateIteration(ServerWebExchange exchange) {
  352 | 		int iteration = exchange.getAttributeOrDefault(RETRY_ITERATION_KEY, -1);
  353 | 		int newIteration = iteration + 1;
  354 | 		trace("setting new iteration in attr %d", () -> newIteration);
  355 | 		exchange.getAttributes().put(RETRY_ITERATION_KEY, newIteration);
  356 | 	}
  357 | 
  358 | 	@SafeVarargs
  359 | 	private final void trace(String message, Supplier<Object>... argSuppliers) {
  360 | 		if (log.isTraceEnabled()) {
  361 | 			Object[] args = new Object[argSuppliers.length];
  362 | 			int i = 0;
  363 | 			for (Supplier<Object> a : argSuppliers) {
  364 | 				args[i] = a.get();
  365 | 				++i;
  366 | 			}
  367 | 			log.trace(String.format(message, args));
  368 | 		}
  369 | 	}
  370 | 
  371 | 	@SuppressWarnings("unchecked")
  372 | 	public static class RetryConfig implements HasRouteId {
  373 | 
  374 | 		private @Nullable String routeId;
  375 | 
  376 | 		private int retries = 3;
  377 | 
  378 | 		private List<Series> series = toList(Series.SERVER_ERROR);
  379 | 
  380 | 		private List<HttpStatus> statuses = new ArrayList<>();
  381 | 
  382 | 		private List<HttpMethod> methods = toList(HttpMethod.GET);
  383 | 
  384 | 		private List<Class<? extends Throwable>> exceptions = toList(IOException.class, TimeoutException.class);
  385 | 
  386 | 		private @Nullable BackoffConfig backoff;
  387 | 
  388 | 		private @Nullable JitterConfig jitter;
  389 | 
  390 | 		private @Nullable Duration timeout;
  391 | 
  392 | 		public RetryConfig allMethods() {
  393 | 			return setMethods(HttpMethod.values());
  394 | 		}
  395 | 
  396 | 		public void validate() {
  397 | 			Assert.isTrue(this.retries > 0, "retries must be greater than 0");
  398 | 			Assert.isTrue(!this.series.isEmpty() || !this.statuses.isEmpty() || !this.exceptions.isEmpty(),
  399 | 					"series, status and exceptions may not all be empty");
  400 | 			Assert.notEmpty(this.methods, "methods may not be empty");
  401 | 			if (this.backoff != null) {
  402 | 				this.backoff.validate();
  403 | 			}
  404 | 			if (this.jitter != null) {
  405 | 				this.jitter.validate();
  406 | 			}
  407 | 			if (this.timeout != null) {
  408 | 				Assert.isTrue(!timeout.isNegative(), "timeout should be >= 0");
  409 | 			}
  410 | 		}
  411 | 
  412 | 		public @Nullable Duration getTimeout() {
  413 | 			return timeout;
  414 | 		}
  415 | 
  416 | 		public RetryConfig setTimeout(Duration timeout) {
  417 | 			this.timeout = timeout;
  418 | 			return this;
  419 | 		}
  420 | 
  421 | 		public @Nullable JitterConfig getJitter() {
  422 | 			return jitter;
  423 | 		}
  424 | 
  425 | 		public RetryConfig setJitter(JitterConfig jitter) {
  426 | 			this.jitter = jitter;
  427 | 			return this;
  428 | 		}
  429 | 
  430 | 		public RetryConfig setJitter(double randomFactor) {
  431 | 			this.jitter = new JitterConfig(randomFactor);
  432 | 			return this;
  433 | 		}
  434 | 
  435 | 		public @Nullable BackoffConfig getBackoff() {
  436 | 			return backoff;
  437 | 		}
  438 | 
  439 | 		public RetryConfig setBackoff(BackoffConfig backoff) {
  440 | 			this.backoff = backoff;
  441 | 			return this;
  442 | 		}
  443 | 
  444 | 		public RetryConfig setBackoff(Duration firstBackoff, Duration maxBackoff, int factor) {
  445 | 			this.backoff = new BackoffConfig(firstBackoff, maxBackoff, factor);
  446 | 			return this;
  447 | 		}
  448 | 
  449 | 		@Override
  450 | 		public void setRouteId(String routeId) {
  451 | 			this.routeId = routeId;
  452 | 		}
  453 | 
  454 | 		@Override
  455 | 		public @Nullable String getRouteId() {
  456 | 			return this.routeId;
  457 | 		}
  458 | 
  459 | 		public int getRetries() {
  460 | 			return retries;
  461 | 		}
  462 | 
  463 | 		public RetryConfig setRetries(int retries) {
  464 | 			this.retries = retries;
  465 | 			return this;
  466 | 		}
  467 | 
  468 | 		public List<Series> getSeries() {
  469 | 			return series;
  470 | 		}
  471 | 
  472 | 		public RetryConfig setSeries(Series... series) {
  473 | 			this.series = Arrays.asList(series);
  474 | 			return this;
  475 | 		}
  476 | 
  477 | 		public List<HttpStatus> getStatuses() {
  478 | 			return statuses;
  479 | 		}
  480 | 
  481 | 		public RetryConfig setStatuses(HttpStatus... statuses) {
  482 | 			this.statuses = Arrays.asList(statuses);
  483 | 			return this;
  484 | 		}
  485 | 
  486 | 		public List<HttpMethod> getMethods() {
  487 | 			return methods;
  488 | 		}
  489 | 
  490 | 		public RetryConfig setMethods(HttpMethod... methods) {
  491 | 			this.methods = Arrays.asList(methods);
  492 | 			return this;
  493 | 		}
  494 | 
  495 | 		public List<Class<? extends Throwable>> getExceptions() {
  496 | 			return exceptions;
  497 | 		}
  498 | 
  499 | 		public RetryConfig setExceptions(Class<? extends Throwable>... exceptions) {
  500 | 			this.exceptions = Arrays.asList(exceptions);
  501 | 			return this;
  502 | 		}
  503 | 
  504 | 	}
  505 | 
  506 | 	public static class BackoffConfig {
  507 | 
  508 | 		private Duration firstBackoff = Duration.ofMillis(5);
  509 | 
  510 | 		private @Nullable Duration maxBackoff;
  511 | 
  512 | 		private int factor = 2;
  513 | 
  514 | 		public BackoffConfig() {
  515 | 		}
  516 | 
  517 | 		public BackoffConfig(Duration firstBackoff, Duration maxBackoff, int factor) {
  518 | 			this.firstBackoff = firstBackoff;
  519 | 			this.maxBackoff = maxBackoff;
  520 | 			this.factor = factor;
  521 | 		}
  522 | 
  523 | 		public void validate() {
  524 | 			Objects.requireNonNull(this.firstBackoff, "firstBackoff must be present");
  525 | 		}
  526 | 
  527 | 		public Duration getFirstBackoff() {
  528 | 			return firstBackoff;
  529 | 		}
  530 | 
  531 | 		public void setFirstBackoff(Duration firstBackoff) {
  532 | 			this.firstBackoff = firstBackoff;
  533 | 		}
  534 | 
  535 | 		public @Nullable Duration getMaxBackoff() {
  536 | 			return maxBackoff;
  537 | 		}
  538 | 
  539 | 		public void setMaxBackoff(Duration maxBackoff) {
  540 | 			this.maxBackoff = maxBackoff;
  541 | 		}
  542 | 
  543 | 		public int getFactor() {
  544 | 			return factor;
  545 | 		}
  546 | 
  547 | 		public void setFactor(int factor) {
  548 | 			this.factor = factor;
  549 | 		}
  550 | 
  551 | 		@Override
  552 | 		public String toString() {
  553 | 			return new ToStringCreator(this).append("firstBackoff", firstBackoff)
  554 | 				.append("maxBackoff", maxBackoff)
  555 | 				.append("factor", factor)
  556 | 				.toString();
  557 | 		}
  558 | 
  559 | 	}
  560 | 
  561 | 	public static class JitterConfig {
  562 | 
  563 | 		private double randomFactor = 0.5;
  564 | 
  565 | 		public void validate() {
  566 | 			Assert.isTrue(randomFactor >= 0 && randomFactor <= 1,
  567 | 					"random factor must be between 0 and 1 (default 0.5)");
  568 | 		}
  569 | 
  570 | 		public JitterConfig() {
  571 | 		}
  572 | 
  573 | 		public JitterConfig(double randomFactor) {
  574 | 			this.randomFactor = randomFactor;
  575 | 		}
  576 | 
  577 | 		public double getRandomFactor() {
  578 | 			return randomFactor;
  579 | 		}
  580 | 
  581 | 		public void setRandomFactor(double randomFactor) {
  582 | 			this.randomFactor = randomFactor;
  583 | 		}
  584 | 
  585 | 		@Override
  586 | 		public String toString() {
  587 | 			return new ToStringCreator(this).append("randomFactor", randomFactor).toString();
  588 | 
  589 | 		}
  590 | 
  591 | 	}
  592 | 
  593 | }
```
