<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/ReactorServiceInstanceLoadBalancer.java -->
<!-- 正文引用：[[源码:ReactorServiceInstanceLoadBalancer.java#L起-L止]] ；代码块头部：(src: ReactorServiceInstanceLoadBalancer.java#L起-L止) -->

# ReactorServiceInstanceLoadBalancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/ReactorServiceInstanceLoadBalancer.java` |
| 行数 | 30 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:36:10.808Z |

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
   19 | import org.springframework.cloud.client.ServiceInstance;
   20 | 
   21 | /**
   22 |  * A marker interface for {@link ReactorLoadBalancer} that allows selecting
   23 |  * {@link ServiceInstance} objects.
   24 |  *
   25 |  * @author Olga Maciaszek-Sharma
   26 |  * @since 2.2.0
   27 |  */
   28 | public interface ReactorServiceInstanceLoadBalancer extends ReactorLoadBalancer<ServiceInstance> {
   29 | 
   30 | }
```
