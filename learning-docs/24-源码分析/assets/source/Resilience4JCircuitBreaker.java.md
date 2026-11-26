<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0 | path: org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JCircuitBreaker.java -->
<!-- 正文引用：[[源码:Resilience4JCircuitBreaker.java#L起-L止]] ；代码块头部：(src: Resilience4JCircuitBreaker.java#L起-L止) -->

# Resilience4JCircuitBreaker.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0` |
| 路径 | `org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JCircuitBreaker.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-circuitbreaker-resilience4j/5.1.0/spring-cloud-circuitbreaker-resilience4j-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.067Z |

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
   17 | package org.springframework.cloud.circuitbreaker.resilience4j;
   18 | 
   19 | import java.util.Map;
   20 | import java.util.Optional;
   21 | import java.util.concurrent.Callable;
   22 | import java.util.concurrent.ExecutorService;
   23 | import java.util.concurrent.Future;
   24 | import java.util.function.Function;
   25 | import java.util.function.Supplier;
   26 | 
   27 | import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
   28 | import io.github.resilience4j.timelimiter.TimeLimiter;
   29 | import io.github.resilience4j.timelimiter.TimeLimiterConfig;
   30 | import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
   34 | import org.springframework.cloud.client.circuitbreaker.Customizer;
   35 | 
   36 | /**
   37 |  * @author Ryan Baxter
   38 |  * @author Andrii Bohutskyi
   39 |  * @author 荒
   40 |  * @author Renette Ros
   41 |  */
   42 | public class Resilience4JCircuitBreaker implements CircuitBreaker {
   43 | 
   44 | 	static final String CIRCUIT_BREAKER_GROUP_TAG = "group";
   45 | 
   46 | 	private final String id;
   47 | 
   48 | 	private final String groupName;
   49 | 
   50 | 	private final Map<String, String> tags;
   51 | 
   52 | 	private @Nullable Resilience4jBulkheadProvider bulkheadProvider;
   53 | 
   54 | 	private final io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig;
   55 | 
   56 | 	private final CircuitBreakerRegistry registry;
   57 | 
   58 | 	private final TimeLimiterRegistry timeLimiterRegistry;
   59 | 
   60 | 	private final TimeLimiterConfig timeLimiterConfig;
   61 | 
   62 | 	private final @Nullable ExecutorService executorService;
   63 | 
   64 | 	private final Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer;
   65 | 
   66 | 	private final boolean disableTimeLimiter;
   67 | 
   68 | 	public Resilience4JCircuitBreaker(String id, String groupName,
   69 | 			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
   70 | 			TimeLimiterConfig timeLimiterConfig, CircuitBreakerRegistry circuitBreakerRegistry,
   71 | 			TimeLimiterRegistry timeLimiterRegistry, @Nullable ExecutorService executorService,
   72 | 			Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer,
   73 | 			@Nullable Resilience4jBulkheadProvider bulkheadProvider, boolean disableTimeLimiter) {
   74 | 		this.id = id;
   75 | 		this.groupName = groupName;
   76 | 		this.circuitBreakerConfig = circuitBreakerConfig;
   77 | 		this.registry = circuitBreakerRegistry;
   78 | 		this.timeLimiterRegistry = timeLimiterRegistry;
   79 | 		this.timeLimiterConfig = timeLimiterConfig;
   80 | 		this.executorService = executorService;
   81 | 		this.circuitBreakerCustomizer = circuitBreakerCustomizer;
   82 | 		this.bulkheadProvider = bulkheadProvider;
   83 | 		this.disableTimeLimiter = disableTimeLimiter;
   84 | 		this.tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
   85 | 	}
   86 | 
   87 | 	public Resilience4JCircuitBreaker(String id, String groupName,
   88 | 			io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig,
   89 | 			TimeLimiterConfig timeLimiterConfig, CircuitBreakerRegistry circuitBreakerRegistry,
   90 | 			TimeLimiterRegistry timeLimiterRegistry,
   91 | 			Optional<Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker>> circuitBreakerCustomizer,
   92 | 			@Nullable Resilience4jBulkheadProvider bulkheadProvider) {
   93 | 		this(id, groupName, circuitBreakerConfig, timeLimiterConfig, circuitBreakerRegistry, timeLimiterRegistry, null,
   94 | 				circuitBreakerCustomizer, bulkheadProvider, false);
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public <T> T run(Supplier<T> toRun, Function<@Nullable Throwable, T> fallback) {
   99 | 		final Map<String, String> tags = Map.of(CIRCUIT_BREAKER_GROUP_TAG, this.groupName);
  100 | 		Optional<TimeLimiter> timeLimiter = loadTimeLimiter();
  101 | 		io.github.resilience4j.circuitbreaker.CircuitBreaker defaultCircuitBreaker = registry.circuitBreaker(this.id,
  102 | 				this.circuitBreakerConfig, tags);
  103 | 		circuitBreakerCustomizer.ifPresent(customizer -> customizer.customize(defaultCircuitBreaker));
  104 | 		if (bulkheadProvider != null) {
  105 | 
  106 | 			if (executorService != null) {
  107 | 				Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
  108 | 				/* conditionally wrap in time-limiter */
  109 | 				Callable<T> timeLimitedCall = timeLimiter
  110 | 					.map(tl -> TimeLimiter.decorateFutureSupplier(tl, futureSupplier))
  111 | 					.orElse(() -> futureSupplier.get().get());
  112 | 				Callable<T> bulkheadCall = bulkheadProvider.decorateCallable(this.groupName, tags, timeLimitedCall);
  113 | 				Callable<T> circuitBreakerCall = io.github.resilience4j.circuitbreaker.CircuitBreaker
  114 | 					.decorateCallable(defaultCircuitBreaker, bulkheadCall);
  115 | 				return getAndApplyFallback(circuitBreakerCall, fallback);
  116 | 			}
  117 | 			else {
  118 | 				Callable<T> bulkheadCall = bulkheadProvider.decorateCallable(this.groupName, tags, toRun::get);
  119 | 				Callable<T> circuitBreakerCall = io.github.resilience4j.circuitbreaker.CircuitBreaker
  120 | 					.decorateCallable(defaultCircuitBreaker, bulkheadCall);
  121 | 				return getAndApplyFallback(circuitBreakerCall, fallback);
  122 | 			}
  123 | 		}
  124 | 		else {
  125 | 			if (executorService != null) {
  126 | 				Supplier<Future<T>> futureSupplier = () -> executorService.submit(toRun::get);
  127 | 				/* conditionally wrap in time-limiter */
  128 | 				Callable<T> restrictedCall = timeLimiter
  129 | 					.map(tl -> TimeLimiter.decorateFutureSupplier(tl, futureSupplier))
  130 | 					.orElse(() -> futureSupplier.get().get());
  131 | 				Callable<T> callable = io.github.resilience4j.circuitbreaker.CircuitBreaker
  132 | 					.decorateCallable(defaultCircuitBreaker, restrictedCall);
  133 | 				return getAndApplyFallback(callable, fallback);
  134 | 			}
  135 | 			else {
  136 | 				Supplier<T> decorator = io.github.resilience4j.circuitbreaker.CircuitBreaker
  137 | 					.decorateSupplier(defaultCircuitBreaker, toRun);
  138 | 				return getAndApplyFallback(decorator, fallback);
  139 | 			}
  140 | 		}
  141 | 	}
  142 | 
  143 | 	private static <T> T getAndApplyFallback(Supplier<T> supplier, Function<@Nullable Throwable, T> fallback) {
  144 | 		try {
  145 | 			return supplier.get();
  146 | 		}
  147 | 		catch (Throwable t) {
  148 | 			return fallback.apply(t);
  149 | 		}
  150 | 	}
  151 | 
  152 | 	private static <T> T getAndApplyFallback(Callable<T> callable, Function<@Nullable Throwable, T> fallback) {
  153 | 		try {
  154 | 			return callable.call();
  155 | 		}
  156 | 		catch (Throwable t) {
  157 | 			return fallback.apply(t);
  158 | 		}
  159 | 	}
  160 | 
  161 | 	private Optional<TimeLimiter> loadTimeLimiter() {
  162 | 		if (disableTimeLimiter) {
  163 | 			return Optional.empty();
  164 | 		}
  165 | 		return Optional.of(this.timeLimiterRegistry.find(this.id)
  166 | 			.orElseGet(() -> this.timeLimiterRegistry.find(this.groupName)
  167 | 				.orElseGet(() -> this.timeLimiterRegistry.timeLimiter(this.id, this.timeLimiterConfig, this.tags))));
  168 | 	}
  169 | 
  170 | }
```
