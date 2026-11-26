<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/LoadBalancerRequest.java -->
<!-- 正文引用：[[源码:LoadBalancerRequest.java#L起-L止]] ；代码块头部：(src: LoadBalancerRequest.java#L起-L止) -->

# LoadBalancerRequest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/LoadBalancerRequest.java` |
| 行数 | 32 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:35:56.456Z |

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
   22 |  * Simple interface used by LoadBalancerClient to apply metrics or pre and post actions
   23 |  * around load balancer requests.
   24 |  *
   25 |  * @param <T> type of the response
   26 |  * @author Spencer Gibb
   27 |  */
   28 | public interface LoadBalancerRequest<T> {
   29 | 
   30 | 	T apply(ServiceInstance instance) throws Exception;
   31 | 
   32 | }
```
