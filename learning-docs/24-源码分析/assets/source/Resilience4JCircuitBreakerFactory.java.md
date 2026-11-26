<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0 | path: org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JCircuitBreakerFactory.java -->
<!-- 正文引用：[[源码:Resilience4JCircuitBreakerFactory.java#L起-L止]] ；代码块头部：(src: Resilience4JCircuitBreakerFactory.java#L起-L止) -->

# Resilience4JCircuitBreakerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0` |
| 路径 | `org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JCircuitBreakerFactory.java` |
| 行数 | 219 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-circuitbreaker-resilience4j/5.1.0/spring-cloud-circuitbreaker-resilience4j-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.174Z |

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
   19 | import java.util.Collections;
   20 | import java.util.HashMap;
   21 | import java.util.Map;
   22 | import java.util.Optional;
   23 | import java.util.Set;
   24 | import java.util.concurrent.ConcurrentHashMap;
   25 | import java.util.concurrent.ExecutorService;
   26 | import java.util.concurrent.Executors;
   27 | import java.util.function.Function;
   28 | 
   29 | import io.github.resilience4j.circuitbreaker.CircuitBreaker;
   30 | import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
   31 | import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
   32 | import io.github.resilience4j.timelimiter.TimeLimiterConfig;
   33 | import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
   34 | import io.micrometer.observation.ObservationRegistry;
   35 | import org.apache.commons.logging.Log;
   36 | import org.apache.commons.logging.LogFactory;
   37 | import org.jspecify.annotations.Nullable;
   38 | 
   39 | import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
   40 | import org.springframework.cloud.client.circuitbreaker.Customizer;
   41 | import org.springframework.cloud.client.circuitbreaker.observation.ObservedCircuitBreaker;
   42 | import org.springframework.util.Assert;
   43 | 
   44 | /**
   45 |  * @author Ryan Baxter
   46 |  * @author Andrii Bohutskyi
   47 |  * @author 荒
   48 |  */
   49 | public class Resilience4JCircuitBreakerFactory extends
   50 | 		CircuitBreakerFactory<Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration, Resilience4JConfigBuilder> {
   51 | 
   52 | 	private static final Log LOG = LogFactory.getLog(Resilience4JCircuitBreakerFactory.class);
   53 | 
   54 | 	private @Nullable Resilience4jBulkheadProvider bulkheadProvider;
   55 | 
   56 | 	private Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration;
   57 | 
   58 | 	private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
   59 | 
   60 | 	private TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
   61 | 
   62 | 	private ExecutorService executorService = Executors.newCachedThreadPool();
   63 | 
   64 | 	private Function<String, ExecutorService> groupExecutorServiceFactory = group -> Executors.newCachedThreadPool();
   65 | 
   66 | 	private ConcurrentHashMap<String, ExecutorService> executorServices = new ConcurrentHashMap<>();
   67 | 
   68 | 	private Map<String, Customizer<CircuitBreaker>> circuitBreakerCustomizers = new HashMap<>();
   69 | 
   70 | 	private final Set<String> loggedTimeLimiterIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
   71 | 
   72 | 	private Resilience4JConfigurationProperties resilience4JConfigurationProperties;
   73 | 
   74 | 	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
   75 | 
   76 | 	public Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
   77 | 			TimeLimiterRegistry timeLimiterRegistry, Resilience4jBulkheadProvider bulkheadProvider) {
   78 | 		this(circuitBreakerRegistry, timeLimiterRegistry, bulkheadProvider, new Resilience4JConfigurationProperties());
   79 | 	}
   80 | 
   81 | 	public Resilience4JCircuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry,
   82 | 			TimeLimiterRegistry timeLimiterRegistry, @Nullable Resilience4jBulkheadProvider bulkheadProvider,
   83 | 			Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
   84 | 		this.circuitBreakerRegistry = circuitBreakerRegistry;
   85 | 		this.timeLimiterRegistry = timeLimiterRegistry;
   86 | 		this.bulkheadProvider = bulkheadProvider;
   87 | 		this.defaultConfiguration = id -> new Resilience4JConfigBuilder(id)
   88 | 			.circuitBreakerConfig(this.circuitBreakerRegistry.getDefaultConfig())
   89 | 			.timeLimiterConfig(this.timeLimiterRegistry.getDefaultConfig())
   90 | 			.build();
   91 | 		this.resilience4JConfigurationProperties = resilience4JConfigurationProperties;
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	protected Resilience4JConfigBuilder configBuilder(String id) {
   96 | 		return new Resilience4JConfigBuilder(id);
   97 | 	}
   98 | 
   99 | 	@Override
  100 | 	public void configureDefault(
  101 | 			Function<String, Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration> defaultConfiguration) {
  102 | 		this.defaultConfiguration = defaultConfiguration;
  103 | 	}
  104 | 
  105 | 	public void configureCircuitBreakerRegistry(CircuitBreakerRegistry registry) {
  106 | 		this.circuitBreakerRegistry = registry;
  107 | 	}
  108 | 
  109 | 	public CircuitBreakerRegistry getCircuitBreakerRegistry() {
  110 | 		return this.circuitBreakerRegistry;
  111 | 	}
  112 | 
  113 | 	public TimeLimiterRegistry getTimeLimiterRegistry() {
  114 | 		return this.timeLimiterRegistry;
  115 | 	}
  116 | 
  117 | 	public @Nullable Resilience4jBulkheadProvider getBulkheadProvider() {
  118 | 		return this.bulkheadProvider;
  119 | 	}
  120 | 
  121 | 	public void configureExecutorService(ExecutorService executorService) {
  122 | 		this.executorService = executorService;
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * configure GroupExecutorService.
  127 | 	 * @param groupFactory GroupExecutorService Factory
  128 | 	 */
  129 | 	public void configureGroupExecutorService(Function<String, ExecutorService> groupFactory) {
  130 | 		this.groupExecutorServiceFactory = groupFactory;
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public org.springframework.cloud.client.circuitbreaker.CircuitBreaker create(String id) {
  135 | 		Assert.hasText(id, "A CircuitBreaker must have an id.");
  136 | 		Resilience4JCircuitBreaker resilience4JCircuitBreaker = create(id, id, this.executorService);
  137 | 		return tryObservedCircuitBreaker(resilience4JCircuitBreaker);
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	public org.springframework.cloud.client.circuitbreaker.CircuitBreaker create(String id, String groupName) {
  142 | 		Assert.hasText(id, "A CircuitBreaker must have an id.");
  143 | 		Assert.hasText(groupName, "A CircuitBreaker must have a group name.");
  144 | 		final ExecutorService groupExecutorService = executorServices.computeIfAbsent(groupName,
  145 | 				groupExecutorServiceFactory);
  146 | 		Resilience4JCircuitBreaker resilience4JCircuitBreaker = create(id, groupName, groupExecutorService);
  147 | 		return tryObservedCircuitBreaker(resilience4JCircuitBreaker);
  148 | 	}
  149 | 
  150 | 	private org.springframework.cloud.client.circuitbreaker.CircuitBreaker tryObservedCircuitBreaker(
  151 | 			Resilience4JCircuitBreaker resilience4JCircuitBreaker) {
  152 | 		if (this.observationRegistry.isNoop()) {
  153 | 			return resilience4JCircuitBreaker;
  154 | 		}
  155 | 		return new ObservedCircuitBreaker(resilience4JCircuitBreaker, this.observationRegistry);
  156 | 	}
  157 | 
  158 | 	public void addCircuitBreakerCustomizer(Customizer<CircuitBreaker> customizer, String... ids) {
  159 | 		for (String id : ids) {
  160 | 			circuitBreakerCustomizers.put(id, customizer);
  161 | 		}
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Add support group/service config on CircuitBreaker.
  166 | 	 * <ul>
  167 | 	 * <li>method(id) config - on specific method or operation</li>
  168 | 	 * <li>Service(group) config - on specific application service or some operations</li>
  169 | 	 * <li>global default config</li>
  170 | 	 * </ul>
  171 | 	 * Descending priority from top to bottom.
  172 | 	 * <p/>
  173 | 	 * @param id operation or method name
  174 | 	 * @param groupName service group name
  175 | 	 * @return {@link Resilience4JCircuitBreaker}
  176 | 	 */
  177 | 	private Resilience4JCircuitBreaker create(String id, String groupName,
  178 | 			ExecutorService circuitBreakerExecutorService) {
  179 | 		Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration defaultConfig = getConfigurations()
  180 | 			.computeIfAbsent(id, defaultConfiguration);
  181 | 		CircuitBreakerConfig circuitBreakerConfig = this.circuitBreakerRegistry.getConfiguration(id)
  182 | 			.orElseGet(() -> this.circuitBreakerRegistry.getConfiguration(groupName)
  183 | 				.orElseGet(defaultConfig::getCircuitBreakerConfig));
  184 | 		TimeLimiterConfig timeLimiterConfig = this.timeLimiterRegistry.getConfiguration(id)
  185 | 			.orElseGet(() -> this.timeLimiterRegistry.getConfiguration(groupName).orElseGet(() -> {
  186 | 				TimeLimiterConfig defaultTimeLimiterConfig = defaultConfig.getTimeLimiterConfig();
  187 | 				if (loggedTimeLimiterIds.add(id)) {
  188 | 					boolean hasUserDefault = this.timeLimiterRegistry.getConfiguration("default").isPresent();
  189 | 					if (hasUserDefault) {
  190 | 						LOG.debug("No specific timeLimiterConfig found for '" + id
  191 | 								+ "' in time limiter registry, using default config: " + defaultTimeLimiterConfig);
  192 | 					}
  193 | 					else {
  194 | 						LOG.warn("No timeLimiterConfig found for '" + id + "' in time limiter registry, using "
  195 | 								+ defaultTimeLimiterConfig);
  196 | 					}
  197 | 				}
  198 | 				return defaultTimeLimiterConfig;
  199 | 			}));
  200 | 		if (resilience4JConfigurationProperties.isDisableThreadPool()) {
  201 | 			return new Resilience4JCircuitBreaker(id, groupName, circuitBreakerConfig, timeLimiterConfig,
  202 | 					circuitBreakerRegistry, timeLimiterRegistry, Optional.ofNullable(circuitBreakerCustomizers.get(id)),
  203 | 					bulkheadProvider);
  204 | 		}
  205 | 		else {
  206 | 			boolean isDisableTimeLimiter = ConfigurationPropertiesUtils
  207 | 				.isDisableTimeLimiter(this.resilience4JConfigurationProperties, id, groupName);
  208 | 			return new Resilience4JCircuitBreaker(id, groupName, circuitBreakerConfig, timeLimiterConfig,
  209 | 					circuitBreakerRegistry, timeLimiterRegistry, circuitBreakerExecutorService,
  210 | 					Optional.ofNullable(circuitBreakerCustomizers.get(id)), bulkheadProvider, isDisableTimeLimiter);
  211 | 		}
  212 | 
  213 | 	}
  214 | 
  215 | 	public void setObservationRegistry(ObservationRegistry observationRegistry) {
  216 | 		this.observationRegistry = observationRegistry;
  217 | 	}
  218 | 
  219 | }
```
