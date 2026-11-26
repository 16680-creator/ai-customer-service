<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/core/ServiceInstanceListSupplier.java -->
<!-- 正文引用：[[源码:ServiceInstanceListSupplier.java#L起-L止]] ；代码块头部：(src: ServiceInstanceListSupplier.java#L起-L止) -->

# ServiceInstanceListSupplier.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/core/ServiceInstanceListSupplier.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.194Z |

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
   20 | import java.util.function.Supplier;
   21 | 
   22 | import reactor.core.publisher.Flux;
   23 | 
   24 | import org.springframework.cloud.client.ServiceInstance;
   25 | import org.springframework.cloud.client.loadbalancer.Request;
   26 | 
   27 | /**
   28 |  * A {@link Supplier} of lists of {@link ServiceInstance} objects.
   29 |  *
   30 |  * @author Olga Maciaszek-Sharma
   31 |  * @since 2.2.0
   32 |  */
   33 | public interface ServiceInstanceListSupplier extends Supplier<Flux<List<ServiceInstance>>> {
   34 | 
   35 | 	String getServiceId();
   36 | 
   37 | 	default Flux<List<ServiceInstance>> get(Request request) {
   38 | 		return get();
   39 | 	}
   40 | 
   41 | 	static ServiceInstanceListSupplierBuilder builder() {
   42 | 		return new ServiceInstanceListSupplierBuilder();
   43 | 	}
   44 | 
   45 | }
```
