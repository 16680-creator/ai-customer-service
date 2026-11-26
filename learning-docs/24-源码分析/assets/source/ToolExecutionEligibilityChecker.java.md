<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolExecutionEligibilityChecker.java -->
<!-- 正文引用：[[源码:ToolExecutionEligibilityChecker.java#L起-L止]] ；代码块头部：(src: ToolExecutionEligibilityChecker.java#L起-L止) -->

# ToolExecutionEligibilityChecker.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolExecutionEligibilityChecker.java` |
| 行数 | 42 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:26.589Z |

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
   19 | import java.util.function.Function;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.model.ChatResponse;
   24 | 
   25 | /**
   26 |  * Interface for determining when tool execution should be performed based on model
   27 |  * responses.
   28 |  *
   29 |  * @author Christian Tzolov
   30 |  */
   31 | public interface ToolExecutionEligibilityChecker extends Function<ChatResponse, Boolean> {
   32 | 
   33 | 	/**
   34 | 	 * Determines if the response is a tool call message response.
   35 | 	 * @param chatResponse The response from the chat model call
   36 | 	 * @return true if the response is a tool call message response, false otherwise
   37 | 	 */
   38 | 	default boolean isToolCallResponse(@Nullable ChatResponse chatResponse) {
   39 | 		return chatResponse != null && apply(chatResponse);
   40 | 	}
   41 | 
   42 | }
```
