<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallConfirmationRequiredException.java -->
<!-- 正文引用：[[源码:ToolCallConfirmationRequiredException.java#L起-L止]] ；代码块头部：(src: ToolCallConfirmationRequiredException.java#L起-L止) -->

# ToolCallConfirmationRequiredException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallConfirmationRequiredException.java` |
| 行数 | 61 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:25.817Z |

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
   21 | import org.springframework.util.Assert;
   22 | 
   23 | /**
   24 |  * Thrown by a {@link ToolCallingManager} when a tool call of the batch needs a person's
   25 |  * confirmation before it runs (see {@link ToolCallConfirmationPolicy}) and no
   26 |  * {@link ToolCallDecisions} were given. None of the batch's tool calls ran.
   27 |  * <p>
   28 |  * A tool-calling loop pauses on it: it returns the reply to the caller as the model gave
   29 |  * it, listing the keys of the calls that wait for a decision under
   30 |  * {@link ToolCallDecisions#AWAITING_DECISION_METADATA_KEY}. The caller collects the
   31 |  * decisions and runs the batch with
   32 |  * {@link ToolCallingManager#executeToolCalls(org.springframework.ai.chat.prompt.Prompt, org.springframework.ai.chat.model.ChatResponse, ToolCallDecisions)}.
   33 |  *
   34 |  * @author Christian Tzolov
   35 |  * @since 2.1.0
   36 |  */
   37 | public class ToolCallConfirmationRequiredException extends RuntimeException {
   38 | 
   39 | 	private final List<String> toolCallIds;
   40 | 
   41 | 	/**
   42 | 	 * Create the exception for the calls that wait for a decision.
   43 | 	 * @param toolCallIds the keys of the tool calls that need a confirmation (see
   44 | 	 * {@link ToolCallDecisions#toolCallKeys}), in the order the model asked for them
   45 | 	 */
   46 | 	public ToolCallConfirmationRequiredException(List<String> toolCallIds) {
   47 | 		super("Tool calls " + toolCallIds + " need a person's confirmation before they run");
   48 | 		Assert.notEmpty(toolCallIds, "toolCallIds must not be empty");
   49 | 		Assert.noNullElements(toolCallIds, "toolCallIds must not contain null elements");
   50 | 		this.toolCallIds = List.copyOf(toolCallIds);
   51 | 	}
   52 | 
   53 | 	/**
   54 | 	 * The keys of the tool calls that need a confirmation (see
   55 | 	 * {@link ToolCallDecisions#toolCallKeys}), in the order the model asked for them.
   56 | 	 */
   57 | 	public List<String> getToolCallIds() {
   58 | 		return this.toolCallIds;
   59 | 	}
   60 | 
   61 | }
```
