<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallingManager.java -->
<!-- 正文引用：[[源码:ToolCallingManager.java#L起-L止]] ；代码块头部：(src: ToolCallingManager.java#L起-L止) -->

# ToolCallingManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallingManager.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.831Z |

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
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.ai.chat.model.ChatResponse;
   22 | import org.springframework.ai.chat.prompt.Prompt;
   23 | import org.springframework.ai.tool.definition.ToolDefinition;
   24 | 
   25 | /**
   26 |  * Service responsible for managing the tool calling process for a chat model.
   27 |  *
   28 |  * @author Thomas Vitale
   29 |  * @since 1.0.0
   30 |  */
   31 | public interface ToolCallingManager {
   32 | 
   33 | 	/**
   34 | 	 * Resolve the tool definitions from the model's tool calling options.
   35 | 	 */
   36 | 	List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions);
   37 | 
   38 | 	/**
   39 | 	 * Execute the tool calls requested by the model.
   40 | 	 * @throws ToolCallConfirmationRequiredException if a tool call needs a person's
   41 | 	 * confirmation before it runs; none of the calls ran. Resume with
   42 | 	 * {@link #executeToolCalls(Prompt, ChatResponse, ToolCallDecisions)}.
   43 | 	 */
   44 | 	ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);
   45 | 
   46 | 	/**
   47 | 	 * Execute the tool calls requested by the model as a person decided: an allowed call
   48 | 	 * runs, a denied call does not and the model receives the decision's message as its
   49 | 	 * result. Every tool call of the response needs a decision, and an allowed call runs
   50 | 	 * only with a tool the prompt carries. This is how a batch paused with a
   51 | 	 * {@link ToolCallConfirmationRequiredException} resumes; the response is the reply
   52 | 	 * the model gave, read from storage the end user cannot change.
   53 | 	 * <p>
   54 | 	 * The default throws {@link UnsupportedOperationException}.
   55 | 	 * @param prompt the conversation the model answered with the tool calls, carrying the
   56 | 	 * tools; it may end with the reply itself
   57 | 	 * @param chatResponse the reply with the tool calls
   58 | 	 * @param decisions one decision per tool call of the reply
   59 | 	 * @return the result, with the conversation history ending in the tool results
   60 | 	 * @throws IllegalArgumentException if a decision names no tool call of the reply, a
   61 | 	 * tool call has no decision, or an allowed call names a tool the prompt does not
   62 | 	 * carry
   63 | 	 * @throws ToolCallLimitExceededException if an allowed call breaks a limit; the
   64 | 	 * caller handles it, there is no loop to turn it into a reply
   65 | 	 * @since 2.1.0
   66 | 	 */
   67 | 	default ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse,
   68 | 			ToolCallDecisions decisions) {
   69 | 		throw new UnsupportedOperationException(getClass().getName() + " does not support tool call decisions");
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Create a default {@link ToolCallingManager} builder.
   74 | 	 */
   75 | 	static DefaultToolCallingManager.Builder builder() {
   76 | 		return DefaultToolCallingManager.builder();
   77 | 	}
   78 | 
   79 | }
```
