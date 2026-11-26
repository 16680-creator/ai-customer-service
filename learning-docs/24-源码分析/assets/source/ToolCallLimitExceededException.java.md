<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallLimitExceededException.java -->
<!-- 正文引用：[[源码:ToolCallLimitExceededException.java#L起-L止]] ；代码块头部：(src: ToolCallLimitExceededException.java#L起-L止) -->

# ToolCallLimitExceededException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallLimitExceededException.java` |
| 行数 | 140 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.646Z |

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
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.messages.AssistantMessage;
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   26 | import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
   27 | import org.springframework.ai.chat.model.Generation;
   28 | 
   29 | /**
   30 |  * Thrown by {@link DefaultToolCallingManager} when a configured tool call limit is
   31 |  * exceeded and {@link ToolCallLimitBehavior#THROW} is in effect.
   32 |  * <p>
   33 |  * Carries the {@link ToolExecutionResult} assembled from whichever tool calls in the
   34 |  * current batch already executed successfully before the limit was hit, so a caller can
   35 |  * still produce a coherent response instead of discarding completed work.
   36 |  *
   37 |  * @author Christian Tzolov
   38 |  * @since 2.0.1
   39 |  */
   40 | public class ToolCallLimitExceededException extends RuntimeException {
   41 | 
   42 | 	/**
   43 | 	 * {@link Generation#getMetadata()} finish reason set on the {@link Generation} built
   44 | 	 * by {@link #buildGeneration()}, distinguishing a forcibly aborted turn from a
   45 | 	 * regular {@link ToolExecutionResult#FINISH_REASON returnDirect} completion.
   46 | 	 */
   47 | 	public static final String FINISH_REASON = "toolCallLimitExceeded";
   48 | 
   49 | 	/**
   50 | 	 * {@link ChatGenerationMetadata} key under which the
   51 | 	 * {@link ToolResponseMessage.ToolResponse}s that completed successfully in the
   52 | 	 * current batch before the limit was hit are attached to the {@link Generation} built
   53 | 	 * by {@link #buildGeneration()}. Keeping them in metadata - rather than as separate,
   54 | 	 * sibling {@link Generation}s - avoids mixing successful tool output with the error
   55 | 	 * message as if they were equally valid alternative answers.
   56 | 	 */
   57 | 	public static final String METADATA_PARTIAL_TOOL_RESPONSES = "partialToolResponses";
   58 | 
   59 | 	private final @Nullable String toolName;
   60 | 
   61 | 	private final int limit;
   62 | 
   63 | 	private final ToolExecutionResult partialToolExecutionResult;
   64 | 
   65 | 	/**
   66 | 	 * @param toolName the name of the tool whose per-tool limit was exceeded, or
   67 | 	 * {@code null} when the total per-turn limit was exceeded instead
   68 | 	 * @param limit the configured limit that was exceeded
   69 | 	 * @param partialToolExecutionResult the result assembled from the tool calls already
   70 | 	 * executed in the current batch, including a synthesized error response for the call
   71 | 	 * that exceeded the limit
   72 | 	 */
   73 | 	public ToolCallLimitExceededException(@Nullable String toolName, int limit,
   74 | 			ToolExecutionResult partialToolExecutionResult) {
   75 | 		super(buildMessage(toolName, limit));
   76 | 		this.toolName = toolName;
   77 | 		this.limit = limit;
   78 | 		this.partialToolExecutionResult = partialToolExecutionResult;
   79 | 	}
   80 | 
   81 | 	private static String buildMessage(@Nullable String toolName, int limit) {
   82 | 		return toolName != null ? "Tool call limit (%d) exceeded for tool '%s'".formatted(limit, toolName)
   83 | 				: "Total tool call limit (%d) exceeded for this turn".formatted(limit);
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * The name of the tool whose per-tool limit was exceeded, or {@code null} when the
   88 | 	 * total per-turn limit was exceeded instead.
   89 | 	 */
   90 | 	public @Nullable String getToolName() {
   91 | 		return this.toolName;
   92 | 	}
   93 | 
   94 | 	/**
   95 | 	 * The configured limit that was exceeded.
   96 | 	 */
   97 | 	public int getLimit() {
   98 | 		return this.limit;
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * The result assembled from the tool calls already executed in the current batch,
  103 | 	 * including a synthesized error response for the call that exceeded the limit.
  104 | 	 */
  105 | 	public ToolExecutionResult getPartialToolExecutionResult() {
  106 | 		return this.partialToolExecutionResult;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Builds a single {@link Generation} representing this exception, so that a limit
  111 | 	 * breach is never mistaken for one of several equally valid answers.
  112 | 	 * <p>
  113 | 	 * The {@link Generation}'s output is the breach message synthesized by
  114 | 	 * {@link DefaultToolCallingManager} for the call that exceeded the limit. Any tool
  115 | 	 * responses that completed successfully earlier in the same batch are attached under
  116 | 	 * {@value #METADATA_PARTIAL_TOOL_RESPONSES} instead of being emitted as separate,
  117 | 	 * sibling generations, so completed work isn't discarded but also isn't confused with
  118 | 	 * the error itself.
  119 | 	 */
  120 | 	public Generation buildGeneration() {
  121 | 		String breachMessage = getMessage();
  122 | 		List<ToolResponseMessage.ToolResponse> partialResponses = List.of();
  123 | 
  124 | 		List<Message> history = this.partialToolExecutionResult.conversationHistory();
  125 | 		if (!history.isEmpty() && history.get(history.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
  126 | 			List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
  127 | 			if (!responses.isEmpty()) {
  128 | 				breachMessage = responses.get(responses.size() - 1).responseData();
  129 | 				partialResponses = responses.subList(0, responses.size() - 1);
  130 | 			}
  131 | 		}
  132 | 
  133 | 		ChatGenerationMetadata metadata = ChatGenerationMetadata.builder()
  134 | 			.finishReason(FINISH_REASON)
  135 | 			.metadata(METADATA_PARTIAL_TOOL_RESPONSES, partialResponses)
  136 | 			.build();
  137 | 		return new Generation(new AssistantMessage(breachMessage), metadata);
  138 | 	}
  139 | 
  140 | }
```
