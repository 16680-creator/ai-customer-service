<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/NacosDiscoveryClient.java -->
<!-- 正文引用：[[源码:NacosDiscoveryClient.java#L起-L止]] ；代码块头部：(src: NacosDiscoveryClient.java#L起-L止) -->

# NacosDiscoveryClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/NacosDiscoveryClient.java` |
| 行数 | 92 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.720Z |

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
   21 | import java.util.Optional;
   22 | 
   23 | import org.slf4j.Logger;
   24 | import org.slf4j.LoggerFactory;
   25 | 
   26 | import org.springframework.beans.factory.annotation.Value;
   27 | import org.springframework.cloud.client.ServiceInstance;
   28 | import org.springframework.cloud.client.discovery.DiscoveryClient;
   29 | 
   30 | /**
   31 |  * @author xiaojing
   32 |  * @author renhaojun
   33 |  * @author echooymxq
   34 |  * @author freeman
   35 |  */
   36 | public class NacosDiscoveryClient implements DiscoveryClient {
   37 | 
   38 | 	private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryClient.class);
   39 | 
   40 | 	/**
   41 | 	 * Nacos Discovery Client Description.
   42 | 	 */
   43 | 	public static final String DESCRIPTION = "Spring Cloud Nacos Discovery Client";
   44 | 
   45 | 	private NacosServiceDiscovery serviceDiscovery;
   46 | 
   47 | 	@Value("${spring.cloud.nacos.discovery.failure-tolerance-enabled:false}")
   48 | 	private boolean failureToleranceEnabled;
   49 | 
   50 | 	public NacosDiscoveryClient(NacosServiceDiscovery nacosServiceDiscovery) {
   51 | 		this.serviceDiscovery = nacosServiceDiscovery;
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public String description() {
   56 | 		return DESCRIPTION;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public List<ServiceInstance> getInstances(String serviceId) {
   61 | 		try {
   62 | 			return Optional.of(serviceDiscovery.getInstances(serviceId))
   63 | 					.map(instances -> {
   64 | 						ServiceCache.setInstances(serviceId, instances);
   65 | 						return instances;
   66 | 					}).get();
   67 | 		}
   68 | 		catch (Exception e) {
   69 | 			if (failureToleranceEnabled) {
   70 | 				return ServiceCache.getInstances(serviceId);
   71 | 			}
   72 | 			throw new RuntimeException(
   73 | 					"Can not get hosts from nacos server. serviceId: " + serviceId, e);
   74 | 		}
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	public List<String> getServices() {
   79 | 		try {
   80 | 			return Optional.of(serviceDiscovery.getServices()).map(services -> {
   81 | 				ServiceCache.setServiceIds(services);
   82 | 				return services;
   83 | 			}).get();
   84 | 		}
   85 | 		catch (Exception e) {
   86 | 			log.error("get service name from nacos server failed.", e);
   87 | 			return failureToleranceEnabled ? ServiceCache.getServiceIds()
   88 | 					: Collections.emptyList();
   89 | 		}
   90 | 	}
   91 | 
   92 | }
```
