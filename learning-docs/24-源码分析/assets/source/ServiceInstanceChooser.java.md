<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/ServiceInstanceChooser.java -->
<!-- 正文引用：[[源码:ServiceInstanceChooser.java#L起-L止]] ；代码块头部：(src: ServiceInstanceChooser.java#L起-L止) -->

# ServiceInstanceChooser.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/ServiceInstanceChooser.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:51.157Z |

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
   19 | import org.springframework.cloud.client.ServiceInstance;
   20 | 
   21 | /**
   22 |  * Implemented by classes which use a load balancer to choose a server to send a request
   23 |  * to.
   24 |  *
   25 |  * @author Ryan Baxter
   26 |  * @author Olga Maciaszek-Sharma
   27 |  */
   28 | public interface ServiceInstanceChooser {
   29 | 
   30 | 	/**
   31 | 	 * Chooses a ServiceInstance from the LoadBalancer for the specified service.
   32 | 	 * @param serviceId The service ID to look up the LoadBalancer.
   33 | 	 * @return A ServiceInstance that matches the serviceId.
   34 | 	 */
   35 | 	ServiceInstance choose(String serviceId);
   36 | 
   37 | 	/**
   38 | 	 * Chooses a ServiceInstance from the LoadBalancer for the specified service and
   39 | 	 * LoadBalancer request.
   40 | 	 * @param serviceId The service ID to look up the LoadBalancer.
   41 | 	 * @param request The request to pass on to the LoadBalancer
   42 | 	 * @param <T> The type of the request context.
   43 | 	 * @return A ServiceInstance that matches the serviceId.
   44 | 	 */
   45 | 	<T> ServiceInstance choose(String serviceId, Request<T> request);
   46 | 
   47 | }
```
