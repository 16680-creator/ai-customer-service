<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/ServiceInstanceListSupplierBuilder.java -->
<!-- 正文引用：[[源码:ServiceInstanceListSupplierBuilder.java#L起-L止]] ；代码块头部：(src: ServiceInstanceListSupplierBuilder.java#L起-L止) -->

# ServiceInstanceListSupplierBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/ServiceInstanceListSupplierBuilder.java` |
| 行数 | 469 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.285Z |

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
   17 | package org.springframework.cloud.loadbalancer.core;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | import java.util.function.BiFunction;
   23 | import java.util.function.Function;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | import reactor.core.publisher.Mono;
   29 | 
   30 | import org.springframework.beans.factory.ObjectProvider;
   31 | import org.springframework.cloud.client.ServiceInstance;
   32 | import org.springframework.cloud.client.discovery.DiscoveryClient;
   33 | import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
   34 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   35 | import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
   36 | import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
   37 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   38 | import org.springframework.context.ConfigurableApplicationContext;
   39 | import org.springframework.core.env.PropertyResolver;
   40 | import org.springframework.http.HttpStatus;
   41 | import org.springframework.util.Assert;
   42 | import org.springframework.util.StringUtils;
   43 | import org.springframework.web.client.RestClient;
   44 | import org.springframework.web.client.RestTemplate;
   45 | import org.springframework.web.reactive.function.client.WebClient;
   46 | import org.springframework.web.util.UriComponentsBuilder;
   47 | 
   48 | /**
   49 |  * A Builder for creating a {@link ServiceInstanceListSupplier} hierarchy to be used in
   50 |  * {@link ReactorLoadBalancer} configuration.
   51 |  *
   52 |  * @author Spencer Gibb
   53 |  * @author Olga Maciaszek-Sharma
   54 |  * @author Zhiguo Chen
   55 |  * @author Sabyasachi Bhattacharya
   56 |  * @author Zhuozhi Ji
   57 |  */
   58 | public final class ServiceInstanceListSupplierBuilder {
   59 | 
   60 | 	private static final Log LOG = LogFactory.getLog(ServiceInstanceListSupplierBuilder.class);
   61 | 
   62 | 	private @Nullable Creator baseCreator;
   63 | 
   64 | 	private final List<DelegateCreator> creators = new ArrayList<>();
   65 | 
   66 | 	ServiceInstanceListSupplierBuilder() {
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Sets a blocking {@link DiscoveryClient}-based
   71 | 	 * {@link DiscoveryClientServiceInstanceListSupplier} as a base
   72 | 	 * {@link ServiceInstanceListSupplier} in the hierarchy.
   73 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
   74 | 	 */
   75 | 	public ServiceInstanceListSupplierBuilder withBlockingDiscoveryClient() {
   76 | 		if (baseCreator != null && LOG.isWarnEnabled()) {
   77 | 			LOG.warn("Overriding a previously set baseCreator with a blocking DiscoveryClient baseCreator.");
   78 | 		}
   79 | 		this.baseCreator = context -> {
   80 | 			DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
   81 | 
   82 | 			return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
   83 | 		};
   84 | 		return this;
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Sets a {@link ReactiveDiscoveryClient}-based
   89 | 	 * {@link DiscoveryClientServiceInstanceListSupplier} as a base
   90 | 	 * {@link ServiceInstanceListSupplier} in the hierarchy.
   91 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
   92 | 	 */
   93 | 	public ServiceInstanceListSupplierBuilder withDiscoveryClient() {
   94 | 		if (baseCreator != null && LOG.isWarnEnabled()) {
   95 | 			LOG.warn("Overriding a previously set baseCreator with a ReactiveDiscoveryClient baseCreator.");
   96 | 		}
   97 | 		this.baseCreator = context -> {
   98 | 			ReactiveDiscoveryClient discoveryClient = context.getBean(ReactiveDiscoveryClient.class);
   99 | 
  100 | 			return new DiscoveryClientServiceInstanceListSupplier(discoveryClient, context.getEnvironment());
  101 | 		};
  102 | 		return this;
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Sets a user-provided {@link ServiceInstanceListSupplier} as a base
  107 | 	 * {@link ServiceInstanceListSupplier} in the hierarchy.
  108 | 	 * @param supplier a user-provided {@link ServiceInstanceListSupplier} instance
  109 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  110 | 	 */
  111 | 	public ServiceInstanceListSupplierBuilder withBase(ServiceInstanceListSupplier supplier) {
  112 | 		this.baseCreator = context -> supplier;
  113 | 		return this;
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Adds a {@link WeightedServiceInstanceListSupplier} to the
  118 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  119 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  120 | 	 */
  121 | 	public ServiceInstanceListSupplierBuilder withWeighted() {
  122 | 		DelegateCreator creator = (context, delegate) -> {
  123 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory = context
  124 | 				.getBean(LoadBalancerClientFactory.class);
  125 | 			return new WeightedServiceInstanceListSupplier(delegate, loadBalancerClientFactory);
  126 | 		};
  127 | 		this.creators.add(creator);
  128 | 		return this;
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Adds a {@link WeightedServiceInstanceListSupplier} that uses user-provided
  133 | 	 * {@link WeightFunction} instance to the {@link ServiceInstanceListSupplier}
  134 | 	 * hierarchy.
  135 | 	 * @param weightFunction a user-provided {@link WeightFunction} instance
  136 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  137 | 	 */
  138 | 	public ServiceInstanceListSupplierBuilder withWeighted(WeightFunction weightFunction) {
  139 | 		DelegateCreator creator = (context, delegate) -> {
  140 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory = context
  141 | 				.getBean(LoadBalancerClientFactory.class);
  142 | 			return new WeightedServiceInstanceListSupplier(delegate, weightFunction, loadBalancerClientFactory);
  143 | 		};
  144 | 		this.creators.add(creator);
  145 | 		return this;
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} to the
  150 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  151 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  152 | 	 */
  153 | 	public ServiceInstanceListSupplierBuilder withHealthChecks() {
  154 | 		DelegateCreator creator = (context, delegate) -> {
  155 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory = context
  156 | 				.getBean(LoadBalancerClientFactory.class);
  157 | 			WebClient.Builder webClient = context.getBean(WebClient.Builder.class);
  158 | 			return healthCheckServiceInstanceListSupplier(webClient.build(), delegate, loadBalancerClientFactory);
  159 | 		};
  160 | 		this.creators.add(creator);
  161 | 		return this;
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
  166 | 	 * {@link WebClient} instance to the {@link ServiceInstanceListSupplier} hierarchy.
  167 | 	 * @param webClient a user-provided {@link WebClient} instance
  168 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  169 | 	 */
  170 | 	public ServiceInstanceListSupplierBuilder withHealthChecks(WebClient webClient) {
  171 | 		DelegateCreator creator = (context, delegate) -> {
  172 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  173 | 			return healthCheckServiceInstanceListSupplier(webClient, delegate, loadBalancerClientFactory);
  174 | 		};
  175 | 		this.creators.add(creator);
  176 | 		return this;
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * Adds a {@link SameInstancePreferenceServiceInstanceListSupplier} to the
  181 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  182 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  183 | 	 */
  184 | 	public ServiceInstanceListSupplierBuilder withSameInstancePreference() {
  185 | 		DelegateCreator creator = (context, delegate) -> {
  186 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  187 | 			return new SameInstancePreferenceServiceInstanceListSupplier(delegate, loadBalancerClientFactory);
  188 | 		};
  189 | 		this.creators.add(creator);
  190 | 		return this;
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
  195 | 	 * {@link RestTemplate} instance to the {@link ServiceInstanceListSupplier} hierarchy.
  196 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  197 | 	 */
  198 | 	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks() {
  199 | 		DelegateCreator creator = (context, delegate) -> {
  200 | 			RestTemplate restTemplate = context.getBean(RestTemplate.class);
  201 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  202 | 			return blockingHealthCheckServiceInstanceListSupplier(restTemplate, delegate, loadBalancerClientFactory);
  203 | 		};
  204 | 		this.creators.add(creator);
  205 | 		return this;
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
  210 | 	 * {@link RestClient} instance to the {@link ServiceInstanceListSupplier} hierarchy.
  211 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  212 | 	 */
  213 | 	public ServiceInstanceListSupplierBuilder withBlockingRestClientHealthChecks() {
  214 | 		DelegateCreator creator = (context, delegate) -> {
  215 | 			RestClient restClient = context.getBean(RestClient.class);
  216 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  217 | 			return blockingHealthCheckServiceInstanceListSupplier(restClient, delegate, loadBalancerClientFactory);
  218 | 		};
  219 | 		this.creators.add(creator);
  220 | 		return this;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
  225 | 	 * {@link RestTemplate} instance to the {@link ServiceInstanceListSupplier} hierarchy.
  226 | 	 * @param restTemplate a user-provided {@link RestTemplate} instance
  227 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  228 | 	 */
  229 | 	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks(RestTemplate restTemplate) {
  230 | 		DelegateCreator creator = (context, delegate) -> {
  231 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  232 | 			return blockingHealthCheckServiceInstanceListSupplier(restTemplate, delegate, loadBalancerClientFactory);
  233 | 		};
  234 | 		this.creators.add(creator);
  235 | 		return this;
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Adds a {@link HealthCheckServiceInstanceListSupplier} that uses user-provided
  240 | 	 * {@link RestClient} instance to the {@link ServiceInstanceListSupplier} hierarchy.
  241 | 	 * @param restClient a user-provided {@link RestClient} instance
  242 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  243 | 	 */
  244 | 	public ServiceInstanceListSupplierBuilder withBlockingHealthChecks(RestClient restClient) {
  245 | 		DelegateCreator creator = (context, delegate) -> {
  246 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  247 | 			return blockingHealthCheckServiceInstanceListSupplier(restClient, delegate, loadBalancerClientFactory);
  248 | 		};
  249 | 		this.creators.add(creator);
  250 | 		return this;
  251 | 	}
  252 | 
  253 | 	/**
  254 | 	 * Adds a {@link ZonePreferenceServiceInstanceListSupplier} to the
  255 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  256 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  257 | 	 */
  258 | 	public ServiceInstanceListSupplierBuilder withZonePreference() {
  259 | 		DelegateCreator creator = (context, delegate) -> {
  260 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  261 | 			LoadBalancerZoneConfig zoneConfig = context.getBean(LoadBalancerZoneConfig.class);
  262 | 			return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory);
  263 | 		};
  264 | 		this.creators.add(creator);
  265 | 		return this;
  266 | 	}
  267 | 
  268 | 	/**
  269 | 	 * Adds a {@link ZonePreferenceServiceInstanceListSupplier} to the
  270 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  271 | 	 * @param zoneName desired zone for zone preference
  272 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  273 | 	 */
  274 | 	public ServiceInstanceListSupplierBuilder withZonePreference(String zoneName) {
  275 | 		DelegateCreator creator = (context, delegate) -> {
  276 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  277 | 			LoadBalancerZoneConfig zoneConfig = new LoadBalancerZoneConfig(zoneName);
  278 | 			return new ZonePreferenceServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory);
  279 | 		};
  280 | 		this.creators.add(creator);
  281 | 		return this;
  282 | 	}
  283 | 
  284 | 	/**
  285 | 	 * Adds a {@link RequestBasedStickySessionServiceInstanceListSupplier} to the
  286 | 	 * {@link ServiceInstanceListSupplier} hierarchy.
  287 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  288 | 	 */
  289 | 	public ServiceInstanceListSupplierBuilder withRequestBasedStickySession() {
  290 | 		DelegateCreator creator = (context, delegate) -> {
  291 | 			LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
  292 | 			return new RequestBasedStickySessionServiceInstanceListSupplier(delegate, loadBalancerClientFactory);
  293 | 		};
  294 | 		this.creators.add(creator);
  295 | 		return this;
  296 | 	}
  297 | 
  298 | 	/**
  299 | 	 * If {@link LoadBalancerCacheManager} is available in the context, adds a
  300 | 	 * {@link CachingServiceInstanceListSupplier} instance to the
  301 | 	 * {@link ServiceInstanceListSupplier} hierarchy to provide a caching mechanism for
  302 | 	 * service instances. Uses {@link ObjectProvider} to lazily resolve
  303 | 	 * {@link LoadBalancerCacheManager}.
  304 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  305 | 	 */
  306 | 	public ServiceInstanceListSupplierBuilder withCaching() {
  307 | 		DelegateCreator creator = (context, delegate) -> {
  308 | 			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
  309 | 				.getBeanProvider(LoadBalancerCacheManager.class);
  310 | 			if (cacheManagerProvider.getIfAvailable() != null) {
  311 | 				return new CachingServiceInstanceListSupplier(delegate, cacheManagerProvider.getIfAvailable());
  312 | 			}
  313 | 			if (LOG.isWarnEnabled()) {
  314 | 				LOG.warn("LoadBalancerCacheManager not available, returning delegate without caching.");
  315 | 			}
  316 | 			return delegate;
  317 | 		};
  318 | 		creators.add(creator);
  319 | 		return this;
  320 | 	}
  321 | 
  322 | 	public ServiceInstanceListSupplierBuilder withRetryAwareness() {
  323 | 		DelegateCreator creator = (context, delegate) -> new RetryAwareServiceInstanceListSupplier(delegate);
  324 | 		creators.add(creator);
  325 | 		return this;
  326 | 	}
  327 | 
  328 | 	public ServiceInstanceListSupplierBuilder withHints() {
  329 | 		DelegateCreator creator = (context, delegate) -> {
  330 | 			LoadBalancerClientFactory factory = context.getBean(LoadBalancerClientFactory.class);
  331 | 			return new HintBasedServiceInstanceListSupplier(delegate, factory);
  332 | 		};
  333 | 		creators.add(creator);
  334 | 		return this;
  335 | 	}
  336 | 
  337 | 	public ServiceInstanceListSupplierBuilder withSubset() {
  338 | 		DelegateCreator creator = (context, delegate) -> {
  339 | 			PropertyResolver resolver = context.getBean(PropertyResolver.class);
  340 | 			LoadBalancerClientFactory factory = context.getBean(LoadBalancerClientFactory.class);
  341 | 			return new SubsetServiceInstanceListSupplier(delegate, resolver, factory);
  342 | 		};
  343 | 		creators.add(creator);
  344 | 		return this;
  345 | 	}
  346 | 
  347 | 	public ServiceInstanceListSupplierBuilder withReactiveApiVersioning() {
  348 | 		DelegateCreator creator = (context, delegate) -> {
  349 | 			LoadBalancerClientFactory factory = context.getBean(LoadBalancerClientFactory.class);
  350 | 			return new ReactiveApiVersionServiceInstanceListSupplier(delegate, factory);
  351 | 		};
  352 | 		creators.add(creator);
  353 | 		return this;
  354 | 	}
  355 | 
  356 | 	public ServiceInstanceListSupplierBuilder withBlockingApiVersioning() {
  357 | 		DelegateCreator creator = (context, delegate) -> {
  358 | 			LoadBalancerClientFactory factory = context.getBean(LoadBalancerClientFactory.class);
  359 | 			return new BlockingApiVersionServiceInstanceListSupplier(delegate, factory);
  360 | 		};
  361 | 		creators.add(creator);
  362 | 		return this;
  363 | 	}
  364 | 
  365 | 	/**
  366 | 	 * Support {@link ServiceInstanceListSupplierBuilder} can be added to the expansion
  367 | 	 * implementation of {@link ServiceInstanceListSupplier} by this method.
  368 | 	 * @param delegateCreator a {@link DelegateCreator} object
  369 | 	 * @return the {@link ServiceInstanceListSupplierBuilder} object
  370 | 	 */
  371 | 	public ServiceInstanceListSupplierBuilder with(@Nullable DelegateCreator delegateCreator) {
  372 | 		if (delegateCreator != null) {
  373 | 			creators.add(delegateCreator);
  374 | 		}
  375 | 		return this;
  376 | 	}
  377 | 
  378 | 	/**
  379 | 	 * Builds the {@link ServiceInstanceListSupplier} hierarchy.
  380 | 	 * @param context application context
  381 | 	 * @return a {@link ServiceInstanceListSupplier} instance on top of the delegate
  382 | 	 * hierarchy
  383 | 	 */
  384 | 	public ServiceInstanceListSupplier build(ConfigurableApplicationContext context) {
  385 | 		Assert.notNull(baseCreator, "A baseCreator must not be null");
  386 | 
  387 | 		ServiceInstanceListSupplier supplier = baseCreator.apply(context);
  388 | 
  389 | 		for (DelegateCreator creator : creators) {
  390 | 			supplier = creator.apply(context, supplier);
  391 | 		}
  392 | 
  393 | 		return supplier;
  394 | 	}
  395 | 
  396 | 	private ServiceInstanceListSupplier healthCheckServiceInstanceListSupplier(WebClient webClient,
  397 | 			ServiceInstanceListSupplier delegate,
  398 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
  399 | 		return new HealthCheckServiceInstanceListSupplier(delegate, loadBalancerClientFactory,
  400 | 				(serviceInstance, healthCheckPath) -> webClient.get()
  401 | 					.uri(UriComponentsBuilder.fromUriString(getUri(serviceInstance, healthCheckPath)).build().toUri())
  402 | 					.retrieve()
  403 | 					.toBodilessEntity()
  404 | 					.map(response -> HttpStatus.OK.equals(response.getStatusCode()))
  405 | 					.onErrorReturn(false));
  406 | 	}
  407 | 
  408 | 	private ServiceInstanceListSupplier blockingHealthCheckServiceInstanceListSupplier(RestTemplate restTemplate,
  409 | 			ServiceInstanceListSupplier delegate,
  410 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
  411 | 		return new HealthCheckServiceInstanceListSupplier(delegate, loadBalancerClientFactory,
  412 | 				(serviceInstance, healthCheckPath) -> Mono.defer(() -> {
  413 | 					URI uri = UriComponentsBuilder.fromUriString(getUri(serviceInstance, healthCheckPath))
  414 | 						.build()
  415 | 						.toUri();
  416 | 					try {
  417 | 						return Mono
  418 | 							.just(HttpStatus.OK.equals(restTemplate.getForEntity(uri, Void.class).getStatusCode()));
  419 | 					}
  420 | 					catch (Exception ignored) {
  421 | 						return Mono.just(false);
  422 | 					}
  423 | 				}));
  424 | 	}
  425 | 
  426 | 	private ServiceInstanceListSupplier blockingHealthCheckServiceInstanceListSupplier(RestClient restClient,
  427 | 			ServiceInstanceListSupplier delegate, LoadBalancerClientFactory loadBalancerClientFactory) {
  428 | 		return new HealthCheckServiceInstanceListSupplier(delegate, loadBalancerClientFactory,
  429 | 				(serviceInstance, healthCheckPath) -> Mono.defer(() -> {
  430 | 					URI uri = UriComponentsBuilder.fromUriString(getUri(serviceInstance, healthCheckPath))
  431 | 						.build()
  432 | 						.toUri();
  433 | 					try {
  434 | 						return Mono.just(HttpStatus.OK
  435 | 							.equals(restClient.get().uri(uri).retrieve().toBodilessEntity().getStatusCode()));
  436 | 					}
  437 | 					catch (Exception ignored) {
  438 | 						return Mono.just(false);
  439 | 					}
  440 | 				}));
  441 | 	}
  442 | 
  443 | 	static String getUri(ServiceInstance serviceInstance, String healthCheckPath) {
  444 | 		if (StringUtils.hasText(healthCheckPath)) {
  445 | 			String path = healthCheckPath.startsWith("/") ? healthCheckPath : "/" + healthCheckPath;
  446 | 			return serviceInstance.getUri().toString() + path;
  447 | 		}
  448 | 		return serviceInstance.getUri().toString();
  449 | 	}
  450 | 
  451 | 	/**
  452 | 	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided
  453 | 	 * {@link ConfigurableApplicationContext}.
  454 | 	 */
  455 | 	public interface Creator extends Function<ConfigurableApplicationContext, ServiceInstanceListSupplier> {
  456 | 
  457 | 	}
  458 | 
  459 | 	/**
  460 | 	 * Allows creating a {@link ServiceInstanceListSupplier} instance based on provided
  461 | 	 * {@link ConfigurableApplicationContext} and another
  462 | 	 * {@link ServiceInstanceListSupplier} instance that will be used as a delegate.
  463 | 	 */
  464 | 	public interface DelegateCreator extends
  465 | 			BiFunction<ConfigurableApplicationContext, ServiceInstanceListSupplier, ServiceInstanceListSupplier> {
  466 | 
  467 | 	}
  468 | 
  469 | }
```
