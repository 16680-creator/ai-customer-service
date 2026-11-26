<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/discovery/DiscoveryClient.java -->
<!-- 正文引用：[[源码:DiscoveryClient.java#L起-L止]] ；代码块头部：(src: DiscoveryClient.java#L起-L止) -->

# DiscoveryClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/discovery/DiscoveryClient.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:50.789Z |

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
   17 | package org.springframework.cloud.client.discovery;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.cloud.client.ServiceInstance;
   22 | import org.springframework.core.Ordered;
   23 | 
   24 | /**
   25 |  * Represents read operations commonly available to discovery services such as Netflix
   26 |  * Eureka or consul.io.
   27 |  *
   28 |  * @author Spencer Gibb
   29 |  * @author Olga Maciaszek-Sharma
   30 |  * @author Chris Bono
   31 |  */
   32 | public interface DiscoveryClient extends Ordered {
   33 | 
   34 | 	/**
   35 | 	 * Default order of the discovery client.
   36 | 	 */
   37 | 	int DEFAULT_ORDER = 0;
   38 | 
   39 | 	/**
   40 | 	 * A human-readable description of the implementation, used in HealthIndicator.
   41 | 	 * @return The description.
   42 | 	 */
   43 | 	String description();
   44 | 
   45 | 	/**
   46 | 	 * Gets all ServiceInstances associated with a particular serviceId.
   47 | 	 * @param serviceId The serviceId to query.
   48 | 	 * @return A List of ServiceInstance.
   49 | 	 */
   50 | 	List<ServiceInstance> getInstances(String serviceId);
   51 | 
   52 | 	/**
   53 | 	 * @return All known service IDs.
   54 | 	 */
   55 | 	List<String> getServices();
   56 | 
   57 | 	/**
   58 | 	 * Can be used to verify the client is valid and able to make calls.
   59 | 	 * <p>
   60 | 	 * A successful invocation with no exception thrown implies the client is able to make
   61 | 	 * calls.
   62 | 	 * <p>
   63 | 	 * The default implementation simply calls {@link #getServices()} - client
   64 | 	 * implementations can override with a lighter weight operation if they choose to.
   65 | 	 */
   66 | 	default void probe() {
   67 | 		getServices();
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * Default implementation for getting order of discovery clients.
   72 | 	 * @return order
   73 | 	 */
   74 | 	@Override
   75 | 	default int getOrder() {
   76 | 		return DEFAULT_ORDER;
   77 | 	}
   78 | 
   79 | }
```
