<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/DiscoveryClientServiceInstanceListSupplier.java -->
<!-- 正文引用：[[源码:DiscoveryClientServiceInstanceListSupplier.java#L起-L止]] ；代码块头部：(src: DiscoveryClientServiceInstanceListSupplier.java#L起-L止) -->

# DiscoveryClientServiceInstanceListSupplier.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/DiscoveryClientServiceInstanceListSupplier.java` |
| 行数 | 123 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.465Z |

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
   17 | package org.springframework.cloud.loadbalancer.core;
   18 | 
   19 | import java.time.Duration;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import reactor.core.publisher.Flux;
   26 | import reactor.core.publisher.Mono;
   27 | import reactor.core.scheduler.Schedulers;
   28 | 
   29 | import org.springframework.boot.convert.DurationStyle;
   30 | import org.springframework.cloud.client.ServiceInstance;
   31 | import org.springframework.cloud.client.discovery.DiscoveryClient;
   32 | import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
   33 | import org.springframework.core.env.Environment;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;
   37 | 
   38 | /**
   39 |  * A discovery-client-based {@link ServiceInstanceListSupplier} implementation.
   40 |  *
   41 |  * @author Spencer Gibb
   42 |  * @author Olga Maciaszek-Sharma
   43 |  * @author Tim Ysewyn
   44 |  * @author Rod Catter
   45 |  * @since 2.2.0
   46 |  */
   47 | public class DiscoveryClientServiceInstanceListSupplier implements ServiceInstanceListSupplier {
   48 | 
   49 | 	/**
   50 | 	 * Property that establishes the timeout for calls to service discovery.
   51 | 	 */
   52 | 	public static final String SERVICE_DISCOVERY_TIMEOUT = "spring.cloud.loadbalancer.service-discovery.timeout";
   53 | 
   54 | 	private static final Log LOG = LogFactory.getLog(DiscoveryClientServiceInstanceListSupplier.class);
   55 | 
   56 | 	private Duration timeout = Duration.ofSeconds(30);
   57 | 
   58 | 	private final String serviceId;
   59 | 
   60 | 	private final Flux<List<ServiceInstance>> serviceInstances;
   61 | 
   62 | 	public DiscoveryClientServiceInstanceListSupplier(DiscoveryClient delegate, Environment environment) {
   63 | 		String property = environment.getProperty(PROPERTY_NAME);
   64 | 		Assert.hasText(property, "'serviceId' must not be empty");
   65 | 		this.serviceId = property;
   66 | 		resolveTimeout(environment);
   67 | 		this.serviceInstances = Flux.defer(() -> Mono.fromCallable(() -> delegate.getInstances(serviceId)))
   68 | 			.timeout(timeout, Flux.defer(() -> {
   69 | 				logTimeout();
   70 | 				return Flux.just(new ArrayList<>());
   71 | 			}), Schedulers.boundedElastic())
   72 | 			.onErrorResume(error -> {
   73 | 				logException(error);
   74 | 				return Flux.just(new ArrayList<>());
   75 | 			});
   76 | 	}
   77 | 
   78 | 	public DiscoveryClientServiceInstanceListSupplier(ReactiveDiscoveryClient delegate, Environment environment) {
   79 | 		String property = environment.getProperty(PROPERTY_NAME);
   80 | 		Assert.hasText(property, "'serviceId' must not be empty");
   81 | 		this.serviceId = property;
   82 | 		resolveTimeout(environment);
   83 | 		this.serviceInstances = Flux
   84 | 			.defer(() -> delegate.getInstances(serviceId).collectList().flux().timeout(timeout, Flux.defer(() -> {
   85 | 				logTimeout();
   86 | 				return Flux.just(new ArrayList<>());
   87 | 			})).onErrorResume(error -> {
   88 | 				logException(error);
   89 | 				return Flux.just(new ArrayList<>());
   90 | 			}));
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public String getServiceId() {
   95 | 		return serviceId;
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public Flux<List<ServiceInstance>> get() {
  100 | 		return serviceInstances;
  101 | 	}
  102 | 
  103 | 	private void resolveTimeout(Environment environment) {
  104 | 		String providedTimeout = environment.getProperty(SERVICE_DISCOVERY_TIMEOUT);
  105 | 		if (providedTimeout != null) {
  106 | 			timeout = DurationStyle.detectAndParse(providedTimeout);
  107 | 		}
  108 | 	}
  109 | 
  110 | 	private void logTimeout() {
  111 | 		if (LOG.isDebugEnabled()) {
  112 | 			LOG.debug(String.format("Timeout occurred while retrieving instances for service %s."
  113 | 					+ "The instances could not be retrieved during %s", serviceId, timeout));
  114 | 		}
  115 | 	}
  116 | 
  117 | 	private void logException(Throwable error) {
  118 | 		if (LOG.isErrorEnabled()) {
  119 | 			LOG.error(String.format("Exception occurred while retrieving instances for service %s", serviceId), error);
  120 | 		}
  121 | 	}
  122 | 
  123 | }
```
