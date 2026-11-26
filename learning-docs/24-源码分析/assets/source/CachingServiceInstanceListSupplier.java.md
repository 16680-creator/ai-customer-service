<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/CachingServiceInstanceListSupplier.java -->
<!-- 正文引用：[[源码:CachingServiceInstanceListSupplier.java#L起-L止]] ；代码块头部：(src: CachingServiceInstanceListSupplier.java#L起-L止) -->

# CachingServiceInstanceListSupplier.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/CachingServiceInstanceListSupplier.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.376Z |

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
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import reactor.core.publisher.Flux;
   24 | 
   25 | import org.springframework.cache.Cache;
   26 | import org.springframework.cache.CacheManager;
   27 | import org.springframework.cloud.client.ServiceInstance;
   28 | 
   29 | /**
   30 |  * A {@link ServiceInstanceListSupplier} implementation that tries retrieving
   31 |  * {@link ServiceInstance} objects from cache; if none found, retrieves instances using
   32 |  * {@link DiscoveryClientServiceInstanceListSupplier}.
   33 |  *
   34 |  * @author Spencer Gibb
   35 |  * @author Olga Maciaszek-Sharma
   36 |  * @since 2.2.0
   37 |  */
   38 | public class CachingServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {
   39 | 
   40 | 	private static final Log log = LogFactory.getLog(CachingServiceInstanceListSupplier.class);
   41 | 
   42 | 	/**
   43 | 	 * Name of the service cache instance.
   44 | 	 */
   45 | 	public static final String SERVICE_INSTANCE_CACHE_NAME = CachingServiceInstanceListSupplier.class.getSimpleName()
   46 | 			+ "Cache";
   47 | 
   48 | 	private final Flux<List<ServiceInstance>> serviceInstances;
   49 | 
   50 | 	@SuppressWarnings("unchecked")
   51 | 	public CachingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, CacheManager cacheManager) {
   52 | 		super(delegate);
   53 | 		String serviceId = delegate.getServiceId();
   54 | 		this.serviceInstances = Flux.defer(() -> {
   55 | 			// TODO: configurable cache name
   56 | 			Cache cache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
   57 | 			if (cache == null) {
   58 | 				if (log.isErrorEnabled()) {
   59 | 					log.error("Unable to find cache: " + SERVICE_INSTANCE_CACHE_NAME);
   60 | 				}
   61 | 			}
   62 | 			else {
   63 | 				List<ServiceInstance> list = cache.get(serviceId, List.class);
   64 | 				if (list != null && !list.isEmpty()) {
   65 | 					return Flux.just(list);
   66 | 				}
   67 | 			}
   68 | 			return delegate.get().take(1).doOnNext(instances -> {
   69 | 				Cache writeCache = cacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
   70 | 				if (writeCache == null) {
   71 | 					if (log.isErrorEnabled()) {
   72 | 						log.error("Unable to find cache for writing: " + SERVICE_INSTANCE_CACHE_NAME);
   73 | 					}
   74 | 				}
   75 | 				else {
   76 | 					writeCache.put(serviceId, instances);
   77 | 				}
   78 | 			});
   79 | 		});
   80 | 	}
   81 | 
   82 | 	@Override
   83 | 	public Flux<List<ServiceInstance>> get() {
   84 | 		return serviceInstances;
   85 | 	}
   86 | 
   87 | }
```
