<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/LoadBalancerProperties.java -->
<!-- 正文引用：[[源码:LoadBalancerProperties.java#L起-L止]] ；代码块头部：(src: LoadBalancerProperties.java#L起-L止) -->

# LoadBalancerProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/LoadBalancerProperties.java` |
| 行数 | 693 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:07:51.599Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.client.loadbalancer;
   18 | 
   19 | import java.io.IOException;
   20 | import java.time.Duration;
   21 | import java.util.Arrays;
   22 | import java.util.HashSet;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | import java.util.concurrent.TimeoutException;
   27 | 
   28 | import org.jspecify.annotations.Nullable;
   29 | import reactor.util.retry.RetryBackoffSpec;
   30 | 
   31 | import org.springframework.boot.context.properties.ConfigurationProperties;
   32 | import org.springframework.boot.context.properties.bind.Name;
   33 | import org.springframework.cloud.client.ServiceInstance;
   34 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   35 | import org.springframework.cloud.commons.util.IdUtils;
   36 | import org.springframework.core.env.PropertyResolver;
   37 | import org.springframework.http.HttpMethod;
   38 | import org.springframework.http.MediaType;
   39 | import org.springframework.util.LinkedCaseInsensitiveMap;
   40 | import org.springframework.web.client.RestTemplate;
   41 | 
   42 | /**
   43 |  * The base configuration bean for Spring Cloud LoadBalancer.
   44 |  *
   45 |  * See {@link LoadBalancerClientsProperties} for the {@link ConfigurationProperties}
   46 |  * annotation.
   47 |  *
   48 |  * @author Olga Maciaszek-Sharma
   49 |  * @author Gandhimathi Velusamy
   50 |  * @author Zhuozhi Ji
   51 |  * @since 2.2.1
   52 |  */
   53 | public class LoadBalancerProperties {
   54 | 
   55 | 	/**
   56 | 	 * Properties for <code>HealthCheckServiceInstanceListSupplier</code>.
   57 | 	 */
   58 | 	private HealthCheck healthCheck = new HealthCheck();
   59 | 
   60 | 	/**
   61 | 	 * Allows setting the value of <code>hint</code> that is passed on to the LoadBalancer
   62 | 	 * request and can subsequently be used in {@link ReactiveLoadBalancer}
   63 | 	 * implementations.
   64 | 	 */
   65 | 	private Map<String, String> hint = new LinkedCaseInsensitiveMap<>();
   66 | 
   67 | 	/**
   68 | 	 * Allows setting the name of the header used for passing the hint for hint-based
   69 | 	 * service instance filtering.
   70 | 	 */
   71 | 	private String hintHeaderName = "X-SC-LB-Hint";
   72 | 
   73 | 	/**
   74 | 	 * Properties for Spring-Retry and Reactor Retry support in Spring Cloud LoadBalancer.
   75 | 	 */
   76 | 	private Retry retry = new Retry();
   77 | 
   78 | 	/**
   79 | 	 * Properties for LoadBalancer sticky-session.
   80 | 	 */
   81 | 	private StickySession stickySession = new StickySession();
   82 | 
   83 | 	/**
   84 | 	 * If this flag is set to {@code true},
   85 | 	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
   86 | 	 * to call {@code delegate.get(request)} in classes assignable from
   87 | 	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
   88 | 	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
   89 | 	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
   90 | 	 * instance supplier hierarchy directly after the supplier performing instance
   91 | 	 * retrieval over the network, before any request-based filtering is done,
   92 | 	 * {@code true} by default.
   93 | 	 */
   94 | 	private boolean callGetWithRequestOnDelegates = true;
   95 | 
   96 | 	/**
   97 | 	 * Properties for
   98 | 	 * {@code org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier}.
   99 | 	 */
  100 | 	private Subset subset = new Subset();
  101 | 
  102 | 	/**
  103 | 	 * Enabling X-Forwarded Host and Proto Headers.
  104 | 	 */
  105 | 	private XForwarded xForwarded = new XForwarded();
  106 | 
  107 | 	/**
  108 | 	 * Properties for LoadBalancer metrics.
  109 | 	 */
  110 | 	private Stats stats = new Stats();
  111 | 
  112 | 	/**
  113 | 	 * Properties for API version-based load-balancing.
  114 | 	 */
  115 | 	private ApiVersion apiVersion = new ApiVersion();
  116 | 
  117 | 	public HealthCheck getHealthCheck() {
  118 | 		return healthCheck;
  119 | 	}
  120 | 
  121 | 	public void setHealthCheck(HealthCheck healthCheck) {
  122 | 		this.healthCheck = healthCheck;
  123 | 	}
  124 | 
  125 | 	public Map<String, String> getHint() {
  126 | 		return hint;
  127 | 	}
  128 | 
  129 | 	public void setHint(Map<String, String> hint) {
  130 | 		this.hint = hint;
  131 | 	}
  132 | 
  133 | 	public Retry getRetry() {
  134 | 		return retry;
  135 | 	}
  136 | 
  137 | 	public void setRetry(Retry retry) {
  138 | 		this.retry = retry;
  139 | 	}
  140 | 
  141 | 	public StickySession getStickySession() {
  142 | 		return stickySession;
  143 | 	}
  144 | 
  145 | 	public void setStickySession(StickySession stickySession) {
  146 | 		this.stickySession = stickySession;
  147 | 	}
  148 | 
  149 | 	public String getHintHeaderName() {
  150 | 		return hintHeaderName;
  151 | 	}
  152 | 
  153 | 	public void setHintHeaderName(String hintHeaderName) {
  154 | 		this.hintHeaderName = hintHeaderName;
  155 | 	}
  156 | 
  157 | 	public void setXForwarded(XForwarded xForwarded) {
  158 | 		this.xForwarded = xForwarded;
  159 | 	}
  160 | 
  161 | 	public XForwarded getXForwarded() {
  162 | 		return xForwarded;
  163 | 	}
  164 | 
  165 | 	public boolean isCallGetWithRequestOnDelegates() {
  166 | 		return callGetWithRequestOnDelegates;
  167 | 	}
  168 | 
  169 | 	public Subset getSubset() {
  170 | 		return subset;
  171 | 	}
  172 | 
  173 | 	public void setSubset(Subset subset) {
  174 | 		this.subset = subset;
  175 | 	}
  176 | 
  177 | 	public void setCallGetWithRequestOnDelegates(boolean callGetWithRequestOnDelegates) {
  178 | 		this.callGetWithRequestOnDelegates = callGetWithRequestOnDelegates;
  179 | 	}
  180 | 
  181 | 	public Stats getStats() {
  182 | 		return stats;
  183 | 	}
  184 | 
  185 | 	public void setStats(Stats stats) {
  186 | 		this.stats = stats;
  187 | 	}
  188 | 
  189 | 	public ApiVersion getApiVersion() {
  190 | 		return apiVersion;
  191 | 	}
  192 | 
  193 | 	public void setApiVersion(ApiVersion apiVersion) {
  194 | 		this.apiVersion = apiVersion;
  195 | 	}
  196 | 
  197 | 	public static class StickySession {
  198 | 
  199 | 		/**
  200 | 		 * The default name of the cookie holding the preferred instance id.
  201 | 		 */
  202 | 		public static final String DEFAULT_INSTANCE_ID_COOKIE_NAME = "sc-lb-instance-id";
  203 | 
  204 | 		/**
  205 | 		 * The name of the cookie holding the preferred instance id.
  206 | 		 */
  207 | 		private String instanceIdCookieName = DEFAULT_INSTANCE_ID_COOKIE_NAME;
  208 | 
  209 | 		/**
  210 | 		 * Indicates whether a cookie with the newly selected instance should be added by
  211 | 		 * SC LoadBalancer.
  212 | 		 */
  213 | 		private boolean addServiceInstanceCookie = false;
  214 | 
  215 | 		public String getInstanceIdCookieName() {
  216 | 			return instanceIdCookieName;
  217 | 		}
  218 | 
  219 | 		public void setInstanceIdCookieName(String instanceIdCookieName) {
  220 | 			this.instanceIdCookieName = instanceIdCookieName;
  221 | 		}
  222 | 
  223 | 		public boolean isAddServiceInstanceCookie() {
  224 | 			return addServiceInstanceCookie;
  225 | 		}
  226 | 
  227 | 		public void setAddServiceInstanceCookie(boolean addServiceInstanceCookie) {
  228 | 			this.addServiceInstanceCookie = addServiceInstanceCookie;
  229 | 		}
  230 | 
  231 | 	}
  232 | 
  233 | 	public static class XForwarded {
  234 | 
  235 | 		/**
  236 | 		 * To Enable X-Forwarded Headers.
  237 | 		 */
  238 | 		private boolean enabled = false;
  239 | 
  240 | 		public boolean isEnabled() {
  241 | 			return enabled;
  242 | 		}
  243 | 
  244 | 		public void setEnabled(boolean enabled) {
  245 | 			this.enabled = enabled;
  246 | 		}
  247 | 
  248 | 	}
  249 | 
  250 | 	public static class HealthCheck {
  251 | 
  252 | 		/**
  253 | 		 * Initial delay value for the HealthCheck scheduler.
  254 | 		 */
  255 | 		private Duration initialDelay = Duration.ZERO;
  256 | 
  257 | 		/**
  258 | 		 * Interval for rerunning the HealthCheck scheduler.
  259 | 		 */
  260 | 		private Duration interval = Duration.ofSeconds(25);
  261 | 
  262 | 		/**
  263 | 		 * Interval for refetching available service instances.
  264 | 		 */
  265 | 		private Duration refetchInstancesInterval = Duration.ofSeconds(25);
  266 | 
  267 | 		/**
  268 | 		 * Path at which the health-check request should be made. Can be set up per
  269 | 		 * <code>serviceId</code>. A <code>default</code> value can be set up as well. If
  270 | 		 * none is set up, <code>/actuator/health</code> will be used.
  271 | 		 */
  272 | 		private Map<String, String> path = new LinkedCaseInsensitiveMap<>();
  273 | 
  274 | 		/**
  275 | 		 * Port at which the health-check request should be made. If none is set, the port
  276 | 		 * under which the requested service is available at the service instance.
  277 | 		 */
  278 | 		private @Nullable Integer port;
  279 | 
  280 | 		/**
  281 | 		 * Indicates whether the instances should be refetched by the
  282 | 		 * <code>HealthCheckServiceInstanceListSupplier</code>. This can be used if the
  283 | 		 * instances can be updated and the underlying delegate does not provide an
  284 | 		 * ongoing flux.
  285 | 		 */
  286 | 		private boolean refetchInstances = false;
  287 | 
  288 | 		/**
  289 | 		 * Indicates whether health checks should keep repeating. It might be useful to
  290 | 		 * set it to <code>false</code> if periodically refetching the instances, as every
  291 | 		 * refetch will also trigger a healthcheck.
  292 | 		 */
  293 | 		private boolean repeatHealthCheck = true;
  294 | 
  295 | 		/**
  296 | 		 * Indicates whether the {@code healthCheckFlux} should emit on each alive
  297 | 		 * {@link ServiceInstance} that has been retrieved. If set to {@code false}, the
  298 | 		 * entire alive instances sequence is first collected into a list and only then
  299 | 		 * emitted.
  300 | 		 */
  301 | 		private boolean updateResultsList = true;
  302 | 
  303 | 		public boolean getRefetchInstances() {
  304 | 			return refetchInstances;
  305 | 		}
  306 | 
  307 | 		public void setRefetchInstances(boolean refetchInstances) {
  308 | 			this.refetchInstances = refetchInstances;
  309 | 		}
  310 | 
  311 | 		public boolean getRepeatHealthCheck() {
  312 | 			return repeatHealthCheck;
  313 | 		}
  314 | 
  315 | 		public void setRepeatHealthCheck(boolean repeatHealthCheck) {
  316 | 			this.repeatHealthCheck = repeatHealthCheck;
  317 | 		}
  318 | 
  319 | 		public Duration getInitialDelay() {
  320 | 			return initialDelay;
  321 | 		}
  322 | 
  323 | 		public void setInitialDelay(Duration initialDelay) {
  324 | 			this.initialDelay = initialDelay;
  325 | 		}
  326 | 
  327 | 		public Duration getRefetchInstancesInterval() {
  328 | 			return refetchInstancesInterval;
  329 | 		}
  330 | 
  331 | 		public void setRefetchInstancesInterval(Duration refetchInstancesInterval) {
  332 | 			this.refetchInstancesInterval = refetchInstancesInterval;
  333 | 		}
  334 | 
  335 | 		public Map<String, String> getPath() {
  336 | 			return path;
  337 | 		}
  338 | 
  339 | 		public void setPath(Map<String, String> path) {
  340 | 			this.path = path;
  341 | 		}
  342 | 
  343 | 		public Duration getInterval() {
  344 | 			return interval;
  345 | 		}
  346 | 
  347 | 		public void setInterval(Duration interval) {
  348 | 			this.interval = interval;
  349 | 		}
  350 | 
  351 | 		public @Nullable Integer getPort() {
  352 | 			return port;
  353 | 		}
  354 | 
  355 | 		public void setPort(@Nullable Integer port) {
  356 | 			this.port = port;
  357 | 		}
  358 | 
  359 | 		public boolean isUpdateResultsList() {
  360 | 			return updateResultsList;
  361 | 		}
  362 | 
  363 | 		public void setUpdateResultsList(boolean updateResultsList) {
  364 | 			this.updateResultsList = updateResultsList;
  365 | 		}
  366 | 
  367 | 	}
  368 | 
  369 | 	public static class Retry {
  370 | 
  371 | 		private boolean enabled = true;
  372 | 
  373 | 		/**
  374 | 		 * Indicates retries should be attempted on operations other than
  375 | 		 * {@link HttpMethod#GET} and {@link HttpMethod#QUERY}.
  376 | 		 */
  377 | 		private boolean retryOnAllOperations = false;
  378 | 
  379 | 		/**
  380 | 		 * Indicates retries should be attempted for all exceptions, not only those
  381 | 		 * specified in {@code retryableExceptions}.
  382 | 		 */
  383 | 		private boolean retryOnAllExceptions = false;
  384 | 
  385 | 		/**
  386 | 		 * Number of retries to be executed on the same <code>ServiceInstance</code>.
  387 | 		 */
  388 | 		private int maxRetriesOnSameServiceInstance = 0;
  389 | 
  390 | 		/**
  391 | 		 * Number of retries to be executed on the next <code>ServiceInstance</code>. A
  392 | 		 * <code>ServiceInstance</code> is chosen before each retry call.
  393 | 		 */
  394 | 		private int maxRetriesOnNextServiceInstance = 1;
  395 | 
  396 | 		/**
  397 | 		 * A {@link Set} of status codes that should trigger a retry.
  398 | 		 */
  399 | 		private Set<Integer> retryableStatusCodes = new HashSet<>();
  400 | 
  401 | 		/**
  402 | 		 * A {@link Set} of {@link Throwable} classes that should trigger a retry.
  403 | 		 */
  404 | 		private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>(
  405 | 				Arrays.asList(IOException.class, TimeoutException.class, RetryableStatusCodeException.class,
  406 | 						org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class));
  407 | 
  408 | 		/**
  409 | 		 * Properties for Reactor Retry backoffs in Spring Cloud LoadBalancer.
  410 | 		 */
  411 | 		private Backoff backoff = new Backoff();
  412 | 
  413 | 		/**
  414 | 		 * Returns true if the load balancer should retry failed requests.
  415 | 		 * @return True if the load balancer should retry failed requests; false
  416 | 		 * otherwise.
  417 | 		 */
  418 | 		public boolean isEnabled() {
  419 | 			return this.enabled;
  420 | 		}
  421 | 
  422 | 		/**
  423 | 		 * Sets whether the load balancer should retry failed requests.
  424 | 		 * @param enabled Whether the load balancer should retry failed requests.
  425 | 		 */
  426 | 		public void setEnabled(boolean enabled) {
  427 | 			this.enabled = enabled;
  428 | 		}
  429 | 
  430 | 		public boolean isRetryOnAllOperations() {
  431 | 			return retryOnAllOperations;
  432 | 		}
  433 | 
  434 | 		public void setRetryOnAllOperations(boolean retryOnAllOperations) {
  435 | 			this.retryOnAllOperations = retryOnAllOperations;
  436 | 		}
  437 | 
  438 | 		public int getMaxRetriesOnSameServiceInstance() {
  439 | 			return maxRetriesOnSameServiceInstance;
  440 | 		}
  441 | 
  442 | 		public void setMaxRetriesOnSameServiceInstance(int maxRetriesOnSameServiceInstance) {
  443 | 			this.maxRetriesOnSameServiceInstance = maxRetriesOnSameServiceInstance;
  444 | 		}
  445 | 
  446 | 		public int getMaxRetriesOnNextServiceInstance() {
  447 | 			return maxRetriesOnNextServiceInstance;
  448 | 		}
  449 | 
  450 | 		public void setMaxRetriesOnNextServiceInstance(int maxRetriesOnNextServiceInstance) {
  451 | 			this.maxRetriesOnNextServiceInstance = maxRetriesOnNextServiceInstance;
  452 | 		}
  453 | 
  454 | 		public Set<Integer> getRetryableStatusCodes() {
  455 | 			return retryableStatusCodes;
  456 | 		}
  457 | 
  458 | 		public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) {
  459 | 			this.retryableStatusCodes = retryableStatusCodes;
  460 | 		}
  461 | 
  462 | 		public Set<Class<? extends Throwable>> getRetryableExceptions() {
  463 | 			return retryableExceptions;
  464 | 		}
  465 | 
  466 | 		public void setRetryableExceptions(Set<Class<? extends Throwable>> retryableExceptions) {
  467 | 			retryableExceptions
  468 | 				.add(org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class);
  469 | 			this.retryableExceptions = retryableExceptions;
  470 | 		}
  471 | 
  472 | 		public Backoff getBackoff() {
  473 | 			return backoff;
  474 | 		}
  475 | 
  476 | 		public void setBackoff(Backoff backoff) {
  477 | 			this.backoff = backoff;
  478 | 		}
  479 | 
  480 | 		public boolean isRetryOnAllExceptions() {
  481 | 			return retryOnAllExceptions;
  482 | 		}
  483 | 
  484 | 		public void setRetryOnAllExceptions(boolean retryOnAllExceptions) {
  485 | 			this.retryOnAllExceptions = retryOnAllExceptions;
  486 | 		}
  487 | 
  488 | 		public static class Backoff {
  489 | 
  490 | 			/**
  491 | 			 * Indicates whether Reactor Retry backoffs should be applied.
  492 | 			 */
  493 | 			private boolean enabled = false;
  494 | 
  495 | 			/**
  496 | 			 * Used to set {@link RetryBackoffSpec#minBackoff}.
  497 | 			 */
  498 | 			private Duration minBackoff = Duration.ofMillis(5);
  499 | 
  500 | 			/**
  501 | 			 * Used to set {@link RetryBackoffSpec#maxBackoff}.
  502 | 			 */
  503 | 			private Duration maxBackoff = Duration.ofMillis(Long.MAX_VALUE);
  504 | 
  505 | 			/**
  506 | 			 * Used to set {@link RetryBackoffSpec#jitter}.
  507 | 			 */
  508 | 			private double jitter = 0.5d;
  509 | 
  510 | 			public Duration getMinBackoff() {
  511 | 				return minBackoff;
  512 | 			}
  513 | 
  514 | 			public void setMinBackoff(Duration minBackoff) {
  515 | 				this.minBackoff = minBackoff;
  516 | 			}
  517 | 
  518 | 			public Duration getMaxBackoff() {
  519 | 				return maxBackoff;
  520 | 			}
  521 | 
  522 | 			public void setMaxBackoff(Duration maxBackoff) {
  523 | 				this.maxBackoff = maxBackoff;
  524 | 			}
  525 | 
  526 | 			public double getJitter() {
  527 | 				return jitter;
  528 | 			}
  529 | 
  530 | 			public void setJitter(double jitter) {
  531 | 				this.jitter = jitter;
  532 | 			}
  533 | 
  534 | 			public boolean isEnabled() {
  535 | 				return enabled;
  536 | 			}
  537 | 
  538 | 			public void setEnabled(boolean enabled) {
  539 | 				this.enabled = enabled;
  540 | 			}
  541 | 
  542 | 		}
  543 | 
  544 | 	}
  545 | 
  546 | 	public static class Subset {
  547 | 
  548 | 		/**
  549 | 		 * Instance id of deterministic subsetting. If not set,
  550 | 		 * {@link IdUtils#getDefaultInstanceId(PropertyResolver)} will be used.
  551 | 		 */
  552 | 		private String instanceId = "";
  553 | 
  554 | 		/**
  555 | 		 * Max subset size of deterministic subsetting.
  556 | 		 */
  557 | 		private int size = 100;
  558 | 
  559 | 		public String getInstanceId() {
  560 | 			return instanceId;
  561 | 		}
  562 | 
  563 | 		public void setInstanceId(String instanceId) {
  564 | 			this.instanceId = instanceId;
  565 | 		}
  566 | 
  567 | 		public int getSize() {
  568 | 			return size;
  569 | 		}
  570 | 
  571 | 		public void setSize(int size) {
  572 | 			this.size = size;
  573 | 		}
  574 | 
  575 | 	}
  576 | 
  577 | 	public static class Stats {
  578 | 
  579 | 		/**
  580 | 		 * Indicates whether the {@code path} should be added to {@code uri} tag in
  581 | 		 * metrics. When {@link RestTemplate} is used to execute load-balanced requests
  582 | 		 * with high cardinality paths, setting it to {@code false} is recommended.
  583 | 		 */
  584 | 		private boolean includePath = true;
  585 | 
  586 | 		public boolean isIncludePath() {
  587 | 			return includePath;
  588 | 		}
  589 | 
  590 | 		public void setIncludePath(boolean includePath) {
  591 | 			this.includePath = includePath;
  592 | 		}
  593 | 
  594 | 	}
  595 | 
  596 | 	public static class ApiVersion {
  597 | 
  598 | 		/**
  599 | 		 * Indicates whether the API version is required with each request.
  600 | 		 */
  601 | 		private boolean required = false;
  602 | 
  603 | 		/**
  604 | 		 * Sets default version that should be used for each request.
  605 | 		 */
  606 | 		@Name("default")
  607 | 		private @Nullable String defaultVersion;
  608 | 
  609 | 		/**
  610 | 		 * Uses the HTTP header with the given name to obtain the version.
  611 | 		 */
  612 | 		private @Nullable String header;
  613 | 
  614 | 		/**
  615 | 		 * Uses the query parameter with the given name to obtain the version.
  616 | 		 */
  617 | 		private @Nullable String queryParameter;
  618 | 
  619 | 		/**
  620 | 		 * Uses the path segment at the given index to obtain the version.
  621 | 		 */
  622 | 		private @Nullable Integer pathSegment;
  623 | 
  624 | 		/**
  625 | 		 * Uses the media type parameter with the given name to obtain the version.
  626 | 		 */
  627 | 		private Map<MediaType, String> mediaTypeParameters = new LinkedHashMap<>();
  628 | 
  629 | 		/**
  630 | 		 * Indicates whether all the available instances should be returned if no
  631 | 		 * instances for the specified version are available.
  632 | 		 */
  633 | 		private boolean fallbackToAvailableInstances = false;
  634 | 
  635 | 		public boolean getRequired() {
  636 | 			return required;
  637 | 		}
  638 | 
  639 | 		public void setRequired(boolean required) {
  640 | 			this.required = required;
  641 | 		}
  642 | 
  643 | 		public @Nullable String getDefaultVersion() {
  644 | 			return defaultVersion;
  645 | 		}
  646 | 
  647 | 		public void setDefaultVersion(@Nullable String defaultVersion) {
  648 | 			this.defaultVersion = defaultVersion;
  649 | 		}
  650 | 
  651 | 		public @Nullable String getHeader() {
  652 | 			return header;
  653 | 		}
  654 | 
  655 | 		public void setHeader(@Nullable String header) {
  656 | 			this.header = header;
  657 | 		}
  658 | 
  659 | 		public @Nullable String getQueryParameter() {
  660 | 			return queryParameter;
  661 | 		}
  662 | 
  663 | 		public void setQueryParameter(@Nullable String queryParameter) {
  664 | 			this.queryParameter = queryParameter;
  665 | 		}
  666 | 
  667 | 		public @Nullable Integer getPathSegment() {
  668 | 			return pathSegment;
  669 | 		}
  670 | 
  671 | 		public void setPathSegment(@Nullable Integer pathSegment) {
  672 | 			this.pathSegment = pathSegment;
  673 | 		}
  674 | 
  675 | 		public Map<MediaType, String> getMediaTypeParameters() {
  676 | 			return mediaTypeParameters;
  677 | 		}
  678 | 
  679 | 		public void setMediaTypeParameters(Map<MediaType, String> mediaTypeParameters) {
  680 | 			this.mediaTypeParameters = mediaTypeParameters;
  681 | 		}
  682 | 
  683 | 		public boolean isFallbackToAvailableInstances() {
  684 | 			return fallbackToAvailableInstances;
  685 | 		}
  686 | 
  687 | 		public void setFallbackToAvailableInstances(boolean fallbackToAvailableInstances) {
  688 | 			this.fallbackToAvailableInstances = fallbackToAvailableInstances;
  689 | 		}
  690 | 
  691 | 	}
  692 | 
  693 | }
```
