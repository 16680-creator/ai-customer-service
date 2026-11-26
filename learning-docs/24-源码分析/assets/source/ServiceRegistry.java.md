<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/serviceregistry/ServiceRegistry.java -->
<!-- 正文引用：[[源码:ServiceRegistry.java#L起-L止]] ；代码块头部：(src: ServiceRegistry.java#L起-L止) -->

# ServiceRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/serviceregistry/ServiceRegistry.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:40.120Z |

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
   17 | package org.springframework.cloud.client.serviceregistry;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Contract to register and deregister instances with a Service Registry.
   23 |  *
   24 |  * @param <R> registration meta data
   25 |  * @author Spencer Gibb
   26 |  * @since 1.2.0
   27 |  */
   28 | public interface ServiceRegistry<R extends Registration> {
   29 | 
   30 | 	/**
   31 | 	 * Registers the registration. A registration typically has information about an
   32 | 	 * instance, such as its hostname and port.
   33 | 	 * @param registration registration meta data
   34 | 	 */
   35 | 	void register(@Nullable R registration);
   36 | 
   37 | 	/**
   38 | 	 * Deregisters the registration.
   39 | 	 * @param registration registration meta data
   40 | 	 */
   41 | 	void deregister(@Nullable R registration);
   42 | 
   43 | 	/**
   44 | 	 * Closes the ServiceRegistry. This is a lifecycle method.
   45 | 	 */
   46 | 	void close();
   47 | 
   48 | 	/**
   49 | 	 * Sets the status of the registration. The status values are determined by the
   50 | 	 * individual implementations.
   51 | 	 * @param registration The registration to update.
   52 | 	 * @param status The status to set.
   53 | 	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
   54 | 	 */
   55 | 	void setStatus(R registration, String status);
   56 | 
   57 | 	/**
   58 | 	 * Gets the status of a particular registration.
   59 | 	 * @param registration The registration to query.
   60 | 	 * @param <T> The type of the status.
   61 | 	 * @return The status of the registration.
   62 | 	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
   63 | 	 */
   64 | 	<T> T getStatus(R registration);
   65 | 
   66 | }
```
