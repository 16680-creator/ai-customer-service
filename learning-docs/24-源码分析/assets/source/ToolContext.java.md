<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/model/ToolContext.java -->
<!-- 正文引用：[[源码:ToolContext.java#L起-L止]] ；代码块头部：(src: ToolContext.java#L起-L止) -->

# ToolContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/model/ToolContext.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.215Z |

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
   17 | package org.springframework.ai.chat.model;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | 
   22 | /**
   23 |  * Represents the context for tool execution in a function calling scenario.
   24 |  *
   25 |  * <p>
   26 |  * This class encapsulates a map of contextual information that can be passed to tools
   27 |  * (functions) when they are called. It provides an immutable view of the context to
   28 |  * ensure thread-safety and prevent modification after creation.
   29 |  * </p>
   30 |  *
   31 |  * <p>
   32 |  * The context is typically populated from the {@code toolContext} field of
   33 |  * {@code ToolCallingChatOptions} and is used in the function execution process.
   34 |  * </p>
   35 |  *
   36 |  * <p>
   37 |  * The context map can contain any information that is relevant to the tool execution.
   38 |  * </p>
   39 |  *
   40 |  * @author Christian Tzolov
   41 |  * @since 1.0.0
   42 |  */
   43 | public final class ToolContext {
   44 | 
   45 | 	private final Map<String, Object> context;
   46 | 
   47 | 	/**
   48 | 	 * Constructs a new ToolContext with the given context map.
   49 | 	 * @param context A map containing the tool context information. This map is wrapped
   50 | 	 * in an unmodifiable view to prevent changes.
   51 | 	 */
   52 | 	public ToolContext(Map<String, Object> context) {
   53 | 		this.context = Collections.unmodifiableMap(context);
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Returns the immutable context map.
   58 | 	 * @return An unmodifiable view of the context map.
   59 | 	 */
   60 | 	public Map<String, Object> getContext() {
   61 | 		return this.context;
   62 | 	}
   63 | 
   64 | }
```
