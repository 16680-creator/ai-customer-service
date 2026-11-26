<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallConfirmationPolicy.java -->
<!-- 正文引用：[[源码:ToolCallConfirmationPolicy.java#L起-L止]] ；代码块头部：(src: ToolCallConfirmationPolicy.java#L起-L止) -->

# ToolCallConfirmationPolicy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallConfirmationPolicy.java` |
| 行数 | 75 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:25.604Z |

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
   19 | import java.util.Set;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.messages.AssistantMessage;
   24 | import org.springframework.ai.tool.metadata.ToolMetadata;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Decides which tool calls a person must confirm before they run.
   29 |  * {@link DefaultToolCallingManager} refuses to run such a call without a decision, and
   30 |  * the tool-calling loop pauses on it; see {@link ToolCallDecisions}. A custom
   31 |  * {@link ToolCallingManager} has to check it itself.
   32 |  *
   33 |  * @author Christian Tzolov
   34 |  * @since 2.1.0
   35 |  */
   36 | @FunctionalInterface
   37 | public interface ToolCallConfirmationPolicy {
   38 | 
   39 | 	/**
   40 | 	 * Whether the tool call needs a confirmation before it runs.
   41 | 	 * @param toolCall the call the model asked for
   42 | 	 * @param toolMetadata the metadata of the tool the call names, or {@code null} when
   43 | 	 * the manager finds no tool for the call
   44 | 	 */
   45 | 	boolean requiresConfirmation(AssistantMessage.ToolCall toolCall, @Nullable ToolMetadata toolMetadata);
   46 | 
   47 | 	/** A call needs confirmation when either policy says so. */
   48 | 	default ToolCallConfirmationPolicy or(ToolCallConfirmationPolicy other) {
   49 | 		Assert.notNull(other, "other must not be null");
   50 | 		return (toolCall, toolMetadata) -> requiresConfirmation(toolCall, toolMetadata)
   51 | 				|| other.requiresConfirmation(toolCall, toolMetadata);
   52 | 	}
   53 | 
   54 | 	/** No call needs confirmation. */
   55 | 	static ToolCallConfirmationPolicy none() {
   56 | 		return (toolCall, toolMetadata) -> false;
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * The calls of tools whose metadata says so
   61 | 	 * ({@link ToolMetadata#requiresConfirmation()},
   62 | 	 * {@code @Tool(requiresConfirmation = true)}). The default of
   63 | 	 * {@link DefaultToolCallingManager}.
   64 | 	 */
   65 | 	static ToolCallConfirmationPolicy fromToolMetadata() {
   66 | 		return (toolCall, toolMetadata) -> toolMetadata != null && toolMetadata.requiresConfirmation();
   67 | 	}
   68 | 
   69 | 	/** The calls of the named tools. */
   70 | 	static ToolCallConfirmationPolicy forTools(String... toolNames) {
   71 | 		Set<String> names = Set.of(toolNames);
   72 | 		return (toolCall, toolMetadata) -> names.contains(toolCall.name());
   73 | 	}
   74 | 
   75 | }
```
