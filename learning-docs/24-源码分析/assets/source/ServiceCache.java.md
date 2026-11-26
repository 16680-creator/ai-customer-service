<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/ServiceCache.java -->
<!-- 正文引用：[[源码:ServiceCache.java#L起-L止]] ；代码块头部：(src: ServiceCache.java#L起-L止) -->

# ServiceCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/ServiceCache.java` |
| 行数 | 107 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.191Z |

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
   17 | package com.alibaba.cloud.nacos.discovery;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Optional;
   23 | import java.util.concurrent.ConcurrentHashMap;
   24 | 
   25 | import com.alibaba.cloud.nacos.discovery.reactive.NacosReactiveDiscoveryClient;
   26 | 
   27 | import org.springframework.cloud.client.ServiceInstance;
   28 | 
   29 | /**
   30 |  * Service cache.
   31 |  * <p>
   32 |  * Cache serviceIds and corresponding instances in Nacos.
   33 |  * <p>
   34 |  * It's very useful to query services and instances on runtime, but it's not real-time,
   35 |  * depends on {@link NacosDiscoveryClient} or {@link NacosReactiveDiscoveryClient}
   36 |  * {@code getServices(), getInstances(..)} invoke.
   37 |  *
   38 |  * @author freeman
   39 |  * @since 2021.0.1.0
   40 |  */
   41 | public final class ServiceCache {
   42 | 
   43 | 	private ServiceCache() {
   44 | 	}
   45 | 
   46 | 	private static List<String> services = Collections.emptyList();
   47 | 
   48 | 	private static Map<String, List<ServiceInstance>> instancesMap = new ConcurrentHashMap<>();
   49 | 
   50 | 	/**
   51 | 	 * Set instances for specific service.
   52 | 	 * @param serviceId service id
   53 | 	 * @param instances service instances
   54 | 	 */
   55 | 	public static void setInstances(String serviceId, List<ServiceInstance> instances) {
   56 | 		instancesMap.put(serviceId, Collections.unmodifiableList(instances));
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Get instances for specific service.
   61 | 	 * @param serviceId service id
   62 | 	 * @return service instances
   63 | 	 */
   64 | 	public static List<ServiceInstance> getInstances(String serviceId) {
   65 | 		return Optional.ofNullable(instancesMap.get(serviceId))
   66 | 				.orElse(Collections.emptyList());
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Set all services.
   71 | 	 * @param serviceIds all services
   72 | 	 * @deprecated since 2021.0.1.1, use {@link #setServiceIds(List)} instead.
   73 | 	 */
   74 | 	@Deprecated
   75 | 	public static void set(List<String> serviceIds) {
   76 | 		services = Collections.unmodifiableList(serviceIds);
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Set all services.
   81 | 	 * @param serviceIds all services
   82 | 	 * @since 2021.0.1.1
   83 | 	 */
   84 | 	public static void setServiceIds(List<String> serviceIds) {
   85 | 		services = Collections.unmodifiableList(serviceIds);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Get all services.
   90 | 	 * @return all services
   91 | 	 * @deprecated since 2021.0.1.1, use {@link #getServiceIds()} instead.
   92 | 	 */
   93 | 	@Deprecated
   94 | 	public static List<String> get() {
   95 | 		return services;
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Get all services.
  100 | 	 * @return all services
  101 | 	 * @since 2021.0.1.1
  102 | 	 */
  103 | 	public static List<String> getServiceIds() {
  104 | 		return services;
  105 | 	}
  106 | 
  107 | }
```
