<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallLimitBehavior.java -->
<!-- 正文引用：[[源码:ToolCallLimitBehavior.java#L起-L止]] ；代码块头部：(src: ToolCallLimitBehavior.java#L起-L止) -->

# ToolCallLimitBehavior.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallLimitBehavior.java` |
| 行数 | 42 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.564Z |

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
   17 | package org.springframework.ai.model.tool;
   18 | 
   19 | /**
   20 |  * Determines what {@link DefaultToolCallingManager} does when a configured tool call
   21 |  * limit (per tool name or total per turn) is exceeded.
   22 |  *
   23 |  * @author Christian Tzolov
   24 |  * @since 2.0.1
   25 |  */
   26 | public enum ToolCallLimitBehavior {
   27 | 
   28 | 	/**
   29 | 	 * Throw a {@link ToolCallLimitExceededException} carrying the tool calls executed so
   30 | 	 * far, aborting the current batch of tool calls immediately.
   31 | 	 */
   32 | 	THROW,
   33 | 
   34 | 	/**
   35 | 	 * Skip invoking the tool callback and instead return a
   36 | 	 * {@link org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse}
   37 | 	 * explaining that the limit was reached, letting the model see the rejection and
   38 | 	 * decide how to proceed.
   39 | 	 */
   40 | 	RETURN_ERROR_RESPONSE
   41 | 
   42 | }
```
