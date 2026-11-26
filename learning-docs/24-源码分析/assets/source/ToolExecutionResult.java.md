<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolExecutionResult.java -->
<!-- 正文引用：[[源码:ToolExecutionResult.java#L起-L止]] ；代码块头部：(src: ToolExecutionResult.java#L起-L止) -->

# ToolExecutionResult.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolExecutionResult.java` |
| 行数 | 86 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:25.108Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import org.springframework.ai.chat.messages.AssistantMessage;
   23 | import org.springframework.ai.chat.messages.Message;
   24 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   25 | import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
   26 | import org.springframework.ai.chat.model.Generation;
   27 | 
   28 | /**
   29 |  * The result of a tool execution.
   30 |  *
   31 |  * @author Thomas Vitale
   32 |  * @since 1.0.0
   33 |  */
   34 | public interface ToolExecutionResult {
   35 | 
   36 | 	String FINISH_REASON = "returnDirect";
   37 | 
   38 | 	String METADATA_TOOL_ID = "toolId";
   39 | 
   40 | 	String METADATA_TOOL_NAME = "toolName";
   41 | 
   42 | 	/**
   43 | 	 * The history of messages exchanged during the conversation, including the tool
   44 | 	 * execution result.
   45 | 	 */
   46 | 	List<Message> conversationHistory();
   47 | 
   48 | 	/**
   49 | 	 * Whether the tool execution result should be returned directly or passed back to the
   50 | 	 * model.
   51 | 	 */
   52 | 	default boolean returnDirect() {
   53 | 		return false;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Create a default {@link ToolExecutionResult} builder.
   58 | 	 */
   59 | 	static DefaultToolExecutionResult.Builder builder() {
   60 | 		return DefaultToolExecutionResult.builder();
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Build a list of {@link Generation} from the tool execution result, useful for
   65 | 	 * sending the tool execution result to the client directly.
   66 | 	 */
   67 | 	static List<Generation> buildGenerations(ToolExecutionResult toolExecutionResult) {
   68 | 		List<Message> conversationHistory = toolExecutionResult.conversationHistory();
   69 | 		List<Generation> generations = new ArrayList<>();
   70 | 		if (conversationHistory
   71 | 			.get(conversationHistory.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
   72 | 			toolResponseMessage.getResponses().forEach(response -> {
   73 | 				AssistantMessage assistantMessage = new AssistantMessage(response.responseData());
   74 | 				Generation generation = new Generation(assistantMessage,
   75 | 						ChatGenerationMetadata.builder()
   76 | 							.metadata(METADATA_TOOL_ID, response.id())
   77 | 							.metadata(METADATA_TOOL_NAME, response.name())
   78 | 							.finishReason(FINISH_REASON)
   79 | 							.build());
   80 | 				generations.add(generation);
   81 | 			});
   82 | 		}
   83 | 		return generations;
   84 | 	}
   85 | 
   86 | }
```
