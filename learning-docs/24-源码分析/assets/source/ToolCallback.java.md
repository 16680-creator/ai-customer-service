<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/ToolCallback.java -->
<!-- 正文引用：[[源码:ToolCallback.java#L起-L止]] ；代码块头部：(src: ToolCallback.java#L起-L止) -->

# ToolCallback.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/ToolCallback.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.203Z |

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
   17 | package org.springframework.ai.tool;
   18 | 
   19 | import org.apache.commons.logging.Log;
   20 | import org.apache.commons.logging.LogFactory;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.model.ToolContext;
   24 | import org.springframework.ai.tool.definition.ToolDefinition;
   25 | import org.springframework.ai.tool.metadata.ToolMetadata;
   26 | 
   27 | /**
   28 |  * Represents a tool whose execution can be triggered by an AI model.
   29 |  *
   30 |  * @author Thomas Vitale
   31 |  * @since 1.0.0
   32 |  */
   33 | public interface ToolCallback {
   34 | 
   35 | 	Log logger = LogFactory.getLog(ToolCallback.class);
   36 | 
   37 | 	/**
   38 | 	 * Definition used by the AI model to determine when and how to call the tool.
   39 | 	 */
   40 | 	ToolDefinition getToolDefinition();
   41 | 
   42 | 	/**
   43 | 	 * Metadata providing additional information on how to handle the tool.
   44 | 	 */
   45 | 	default ToolMetadata getToolMetadata() {
   46 | 		return ToolMetadata.builder().build();
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Execute tool with the given input and return the result to send back to the AI
   51 | 	 * model.
   52 | 	 */
   53 | 	String call(String toolInput);
   54 | 
   55 | 	/**
   56 | 	 * Execute tool with the given input and context, and return the result to send back
   57 | 	 * to the AI model.
   58 | 	 */
   59 | 	default String call(String toolInput, @Nullable ToolContext toolContext) {
   60 | 		if (toolContext != null && !toolContext.getContext().isEmpty()) {
   61 | 			if (logger.isInfoEnabled()) {
   62 | 				logger.info("By default the tool context is not used,  "
   63 | 						+ "override the method 'call(String toolInput, ToolContext toolcontext)' to support the use of tool context."
   64 | 						+ "Review the ToolCallback implementation for " + getToolDefinition().name());
   65 | 			}
   66 | 		}
   67 | 		return call(toolInput);
   68 | 	}
   69 | 
   70 | }
```
