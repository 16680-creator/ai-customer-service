<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallDecisions.java -->
<!-- 正文引用：[[源码:ToolCallDecisions.java#L起-L止]] ；代码块头部：(src: ToolCallDecisions.java#L起-L止) -->

# ToolCallDecisions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallDecisions.java` |
| 行数 | 149 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:26.197Z |

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
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.messages.AssistantMessage;
   25 | import org.springframework.ai.chat.model.ChatResponse;
   26 | import org.springframework.util.Assert;
   27 | import org.springframework.util.StringUtils;
   28 | 
   29 | /**
   30 |  * The decisions a person made on the tool calls of an {@link AssistantMessage}, for a
   31 |  * {@link ToolCallingManager} to carry them out.
   32 |  * <p>
   33 |  * A manager pauses a batch when one of its calls needs confirmation (see
   34 |  * {@link ToolCallConfirmationPolicy}), by throwing
   35 |  * {@link ToolCallConfirmationRequiredException} before any call runs. A tool-calling loop
   36 |  * then returns the reply to the caller, listing the keys of the calls that wait for a
   37 |  * decision under {@link #AWAITING_DECISION_METADATA_KEY} in the response metadata
   38 |  * ({@link #awaitingDecision(ChatResponse)}). The caller shows them to a person and runs
   39 |  * the reply's calls with the decisions through
   40 |  * {@link ToolCallingManager#executeToolCalls(org.springframework.ai.chat.prompt.Prompt, ChatResponse, ToolCallDecisions)}:
   41 |  * the allowed calls run, a denied call answers the model with its message. The tool
   42 |  * results then go to the model as part of the next request.
   43 |  * <p>
   44 |  * Every tool call of the reply needs a decision, and an allowed call runs only with a
   45 |  * tool the prompt carries. The reply the calls come from must be the one the model gave,
   46 |  * read from storage the end user cannot change: whoever can edit it can run any tool with
   47 |  * any arguments.
   48 |  * <p>
   49 |  * Decisions name the calls by their keys ({@link #toolCallKeys(AssistantMessage)}): a
   50 |  * call's id, or its position in the reply when the model gave it no id, or the same id as
   51 |  * another call. The reply itself is never changed.
   52 |  *
   53 |  * @param decisions one decision per tool call of the reply, with distinct keys
   54 |  * @author Christian Tzolov
   55 |  * @since 2.1.0
   56 |  */
   57 | public record ToolCallDecisions(List<ToolCallDecision> decisions) {
   58 | 
   59 | 	/**
   60 | 	 * The prefix of the key of a tool call that has no id of its own, followed by the
   61 | 	 * call's position in the reply, starting at {@code 0}.
   62 | 	 */
   63 | 	public static final String POSITION_KEY_PREFIX = "#";
   64 | 
   65 | 	/**
   66 | 	 * The response metadata key under which a paused loop lists the keys of the tool
   67 | 	 * calls that wait for a decision, as a {@code List<String>}. When streaming, the key
   68 | 	 * is on the last chunk only; the aggregated response does not carry it.
   69 | 	 */
   70 | 	public static final String AWAITING_DECISION_METADATA_KEY = "spring.ai.tool-calls-awaiting-decision";
   71 | 
   72 | 	public ToolCallDecisions {
   73 | 		Assert.notNull(decisions, "decisions must not be null");
   74 | 		Assert.noNullElements(decisions, "decisions must not contain null elements");
   75 | 		Assert.isTrue(decisions.stream().map(ToolCallDecision::toolCallId).distinct().count() == decisions.size(),
   76 | 				"decisions must be about distinct tool calls");
   77 | 		decisions = List.copyOf(decisions);
   78 | 	}
   79 | 
   80 | 	/** The decisions, one per tool call. */
   81 | 	public static ToolCallDecisions of(ToolCallDecision... decisions) {
   82 | 		return new ToolCallDecisions(List.of(decisions));
   83 | 	}
   84 | 
   85 | 	/** The decision on the tool call with the key, or {@code null} when there is none. */
   86 | 	public @Nullable ToolCallDecision decisionOn(String toolCallId) {
   87 | 		for (ToolCallDecision decision : this.decisions) {
   88 | 			if (decision.toolCallId().equals(toolCallId)) {
   89 | 				return decision;
   90 | 			}
   91 | 		}
   92 | 		return null;
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * The keys of the tool calls of the message, in the order the model asked for them: a
   97 | 	 * call's id when it has one that no other call of the message shares, otherwise
   98 | 	 * {@link #POSITION_KEY_PREFIX} followed by the call's position, such as {@code #1}.
   99 | 	 * Decisions name the calls by these keys. Some model providers return tool calls
  100 | 	 * without ids; their calls are named by position, and the message keeps its calls as
  101 | 	 * the model gave them.
  102 | 	 * @param message the reply with the tool calls
  103 | 	 * @return one key per tool call, all distinct
  104 | 	 */
  105 | 	public static List<String> toolCallKeys(AssistantMessage message) {
  106 | 		Assert.notNull(message, "message must not be null");
  107 | 		List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
  108 | 		List<String> keys = new ArrayList<>(toolCalls.size());
  109 | 		for (int i = 0; i < toolCalls.size(); i++) {
  110 | 			String id = toolCalls.get(i).id();
  111 | 			keys.add(isOwnId(id, toolCalls) ? id : POSITION_KEY_PREFIX + i);
  112 | 		}
  113 | 		return keys;
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Whether the id names one call only. An id that looks like a position key does not
  118 | 	 * count, so that it can never stand for another call.
  119 | 	 */
  120 | 	private static boolean isOwnId(@Nullable String id, List<AssistantMessage.ToolCall> toolCalls) {
  121 | 		return StringUtils.hasText(id) && !id.startsWith(POSITION_KEY_PREFIX)
  122 | 				&& toolCalls.stream().filter(toolCall -> id.equals(toolCall.id())).count() == 1;
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * The keys of the tool calls a paused loop waits on (see
  127 | 	 * {@link #toolCallKeys(AssistantMessage)}), or an empty list when the response is not
  128 | 	 * a paused one. When streaming, ask the last chunk; the aggregated response does not
  129 | 	 * carry the keys.
  130 | 	 * <p>
  131 | 	 * Only a response tells a paused reply apart: read it with {@code chatResponse()} or
  132 | 	 * {@code chatClientResponse()}. The text that {@code content()} returns is the paused
  133 | 	 * reply's text, often empty, and a streamed {@code content()} simply ends.
  134 | 	 */
  135 | 	@SuppressWarnings("unchecked")
  136 | 	public static List<String> awaitingDecision(@Nullable ChatResponse chatResponse) {
  137 | 		if (chatResponse == null) {
  138 | 			return List.of();
  139 | 		}
  140 | 		Object ids = chatResponse.getMetadata().get(AWAITING_DECISION_METADATA_KEY);
  141 | 		return (ids instanceof List<?> list) ? (List<String>) list : List.of();
  142 | 	}
  143 | 
  144 | 	/** Whether the response is a paused one, with tool calls waiting for a decision. */
  145 | 	public static boolean isAwaitingDecision(@Nullable ChatResponse chatResponse) {
  146 | 		return !awaitingDecision(chatResponse).isEmpty();
  147 | 	}
  148 | 
  149 | }
```
