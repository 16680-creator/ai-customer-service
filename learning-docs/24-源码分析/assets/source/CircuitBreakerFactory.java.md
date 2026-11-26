<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/circuitbreaker/CircuitBreakerFactory.java -->
<!-- 正文引用：[[源码:CircuitBreakerFactory.java#L起-L止]] ；代码块头部：(src: CircuitBreakerFactory.java#L起-L止) -->

# CircuitBreakerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/circuitbreaker/CircuitBreakerFactory.java` |
| 行数 | 34 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:21.127Z |

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
   17 | package org.springframework.cloud.client.circuitbreaker;
   18 | 
   19 | /**
   20 |  * Creates circuit breakers based on the underlying implementation.
   21 |  *
   22 |  * @author Ryan Baxter
   23 |  * @author Andrii Bohutskyi
   24 |  */
   25 | public abstract class CircuitBreakerFactory<CONF, CONFB extends ConfigBuilder<CONF>>
   26 | 		extends AbstractCircuitBreakerFactory<CONF, CONFB> {
   27 | 
   28 | 	public abstract CircuitBreaker create(String id);
   29 | 
   30 | 	public CircuitBreaker create(String id, String groupName) {
   31 | 		return create(id);
   32 | 	}
   33 | 
   34 | }
```
