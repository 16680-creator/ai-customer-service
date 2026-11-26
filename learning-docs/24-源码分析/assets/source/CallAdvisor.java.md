<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/api/CallAdvisor.java -->
<!-- 正文引用：[[源码:CallAdvisor.java#L起-L止]] ；代码块头部：(src: CallAdvisor.java#L起-L止) -->

# CallAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/api/CallAdvisor.java` |
| 行数 | 34 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.940Z |

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
   19 | import org.springframework.ai.chat.client.ChatClientRequest;
   20 | import org.springframework.ai.chat.client.ChatClientResponse;
   21 | 
   22 | /**
   23 |  * Advisor for execution flows ultimately resulting in a call to an AI model
   24 |  *
   25 |  * @author Christian Tzolov
   26 |  * @author Dariusz Jedrzejczyk
   27 |  * @author Thomas Vitale
   28 |  * @since 1.0.0
   29 |  */
   30 | public interface CallAdvisor extends Advisor {
   31 | 
   32 | 	ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain);
   33 | 
   34 | }
```
