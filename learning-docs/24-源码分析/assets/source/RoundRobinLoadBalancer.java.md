<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/RoundRobinLoadBalancer.java -->
<!-- 正文引用：[[源码:RoundRobinLoadBalancer.java#L起-L止]] ；代码块头部：(src: RoundRobinLoadBalancer.java#L起-L止) -->

# RoundRobinLoadBalancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/RoundRobinLoadBalancer.java` |
| 行数 | 122 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.116Z |

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
   19 | import java.util.List;
   20 | import java.util.Random;
   21 | import java.util.concurrent.atomic.AtomicInteger;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import reactor.core.publisher.Mono;
   26 | 
   27 | import org.springframework.beans.factory.ObjectProvider;
   28 | import org.springframework.cloud.client.ServiceInstance;
   29 | import org.springframework.cloud.client.loadbalancer.DefaultResponse;
   30 | import org.springframework.cloud.client.loadbalancer.EmptyResponse;
   31 | import org.springframework.cloud.client.loadbalancer.Request;
   32 | import org.springframework.cloud.client.loadbalancer.Response;
   33 | import org.springframework.util.function.SingletonSupplier;
   34 | 
   35 | /**
   36 |  * A Round-Robin-based implementation of {@link ReactorServiceInstanceLoadBalancer}.
   37 |  *
   38 |  * @author Spencer Gibb
   39 |  * @author Olga Maciaszek-Sharma
   40 |  * @author Zhuozhi JI
   41 |  * @author Nan Chiu
   42 |  */
   43 | public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {
   44 | 
   45 | 	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);
   46 | 
   47 | 	final AtomicInteger position;
   48 | 
   49 | 	final String serviceId;
   50 | 
   51 | 	private final SingletonSupplier<ServiceInstanceListSupplier> serviceInstanceListSingletonSupplier;
   52 | 
   53 | 	/**
   54 | 	 * @param serviceInstanceListSupplierProvider a provider of
   55 | 	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
   56 | 	 * @param serviceId id of the service for which to choose an instance
   57 | 	 */
   58 | 	public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
   59 | 			String serviceId) {
   60 | 		this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * @param serviceInstanceListSupplierProvider a provider of
   65 | 	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
   66 | 	 * @param serviceId id of the service for which to choose an instance
   67 | 	 * @param seedPosition Round Robin element position marker
   68 | 	 */
   69 | 	public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
   70 | 			String serviceId, int seedPosition) {
   71 | 		this.serviceId = serviceId;
   72 | 		this.serviceInstanceListSingletonSupplier = SingletonSupplier
   73 | 			.of(() -> serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new));
   74 | 		this.position = new AtomicInteger(seedPosition);
   75 | 	}
   76 | 
   77 | 	@SuppressWarnings("rawtypes")
   78 | 	@Override
   79 | 	// see original
   80 | 	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
   81 | 	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
   82 | 	public Mono<Response<ServiceInstance>> choose(Request request) {
   83 | 		ServiceInstanceListSupplier supplier = serviceInstanceListSingletonSupplier.obtain();
   84 | 		return supplier.get(request)
   85 | 			.next()
   86 | 			.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
   87 | 	}
   88 | 
   89 | 	@SuppressWarnings("NullAway") // guarded by hasServer()
   90 | 	private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
   91 | 			List<ServiceInstance> serviceInstances) {
   92 | 		Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
   93 | 		if (supplier instanceof SelectedInstanceCallback callback && serviceInstanceResponse.hasServer()) {
   94 | 			callback.selectedServiceInstance(serviceInstanceResponse.getServer());
   95 | 		}
   96 | 		return serviceInstanceResponse;
   97 | 	}
   98 | 
   99 | 	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
  100 | 		if (instances.isEmpty()) {
  101 | 			if (log.isWarnEnabled()) {
  102 | 				log.warn("No servers available for service: " + serviceId);
  103 | 			}
  104 | 			return new EmptyResponse();
  105 | 		}
  106 | 
  107 | 		// Do not move position when there is only 1 instance, especially some suppliers
  108 | 		// have already filtered instances
  109 | 		if (instances.size() == 1) {
  110 | 			return new DefaultResponse(instances.get(0));
  111 | 		}
  112 | 
  113 | 		// Ignore the sign bit, this allows pos to loop sequentially from 0 to
  114 | 		// Integer.MAX_VALUE
  115 | 		int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
  116 | 
  117 | 		ServiceInstance instance = instances.get(pos % instances.size());
  118 | 
  119 | 		return new DefaultResponse(instance);
  120 | 	}
  121 | 
  122 | }
```
