<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/api/AdvisorChain.java -->
<!-- 正文引用：[[源码:AdvisorChain.java#L起-L止]] ；代码块头部：(src: AdvisorChain.java#L起-L止) -->

# AdvisorChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/api/AdvisorChain.java` |
| 行数 | 34 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.364Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
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
   17 | package org.springframework.ai.chat.client.advisor.api;
   18 | 
   19 | import io.micrometer.observation.ObservationRegistry;
   20 | 
   21 | /**
   22 |  * Defines the context for executing a chain of advisors as part of processing a chat
   23 |  * request.
   24 |  *
   25 |  * @author Thomas Vitale
   26 |  * @since 1.0.0
   27 |  */
   28 | public interface AdvisorChain {
   29 | 
   30 | 	default ObservationRegistry getObservationRegistry() {
   31 | 		return ObservationRegistry.NOOP;
   32 | 	}
   33 | 
   34 | }
```
