<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/api/Advisor.java -->
<!-- 正文引用：[[源码:Advisor.java#L起-L止]] ；代码块头部：(src: Advisor.java#L起-L止) -->

# Advisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/api/Advisor.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.844Z |

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
   19 | import org.springframework.core.Ordered;
   20 | 
   21 | /**
   22 |  * Parent advisor interface for all advisors.
   23 |  *
   24 |  * @author Christian Tzolov
   25 |  * @author Dariusz Jedrzejczyk
   26 |  * @since 1.0.0
   27 |  * @see CallAdvisor
   28 |  * @see StreamAdvisor
   29 |  * @see BaseAdvisor
   30 |  */
   31 | public interface Advisor extends Ordered {
   32 | 
   33 | 	/**
   34 | 	 * Default order for chat-memory advisors. Placed before (outside)
   35 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor#DEFAULT_ORDER}
   36 | 	 * so the memory advisor wraps the tool-call loop, and the {@code ToolCallingAdvisor}
   37 | 	 * manages its own intermediate conversation history.
   38 | 	 */
   39 | 	int DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
   40 | 
   41 | 	/**
   42 | 	 * Return the name of the advisor.
   43 | 	 * @return the advisor name.
   44 | 	 */
   45 | 	String getName();
   46 | 
   47 | }
```
