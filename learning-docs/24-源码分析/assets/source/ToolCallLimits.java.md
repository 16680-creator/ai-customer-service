<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallLimits.java -->
<!-- 正文引用：[[源码:ToolCallLimits.java#L起-L止]] ；代码块头部：(src: ToolCallLimits.java#L起-L止) -->

# ToolCallLimits.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallLimits.java` |
| 行数 | 135 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:26.404Z |

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
   19 | import java.util.HashMap;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Set;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.ai.chat.messages.Message;
   27 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   28 | import org.springframework.ai.chat.messages.UserMessage;
   29 | 
   30 | /**
   31 |  * Tool call limit configuration for {@link DefaultToolCallingManager}, and the logic to
   32 |  * evaluate it against the number of times a tool has been called.
   33 |  *
   34 |  * @author Christian Tzolov
   35 |  * @since 2.0.1
   36 |  */
   37 | final class ToolCallLimits {
   38 | 
   39 | 	/**
   40 | 	 * Sentinel value meaning that no limit is enforced.
   41 | 	 */
   42 | 	static final int NO_LIMIT = -1;
   43 | 
   44 | 	private final int defaultMaxCallsPerTool;
   45 | 
   46 | 	private final Map<String, Integer> maxCallsPerTool;
   47 | 
   48 | 	private final Set<String> toolsExcludedFromLimit;
   49 | 
   50 | 	private final int maxTotalToolCalls;
   51 | 
   52 | 	private final ToolCallLimitBehavior onLimitExceeded;
   53 | 
   54 | 	ToolCallLimits(int defaultMaxCallsPerTool, Map<String, Integer> maxCallsPerTool, Set<String> toolsExcludedFromLimit,
   55 | 			int maxTotalToolCalls, ToolCallLimitBehavior onLimitExceeded) {
   56 | 		this.defaultMaxCallsPerTool = defaultMaxCallsPerTool;
   57 | 		this.maxCallsPerTool = Map.copyOf(maxCallsPerTool);
   58 | 		this.toolsExcludedFromLimit = Set.copyOf(toolsExcludedFromLimit);
   59 | 		this.maxTotalToolCalls = maxTotalToolCalls;
   60 | 		this.onLimitExceeded = onLimitExceeded;
   61 | 	}
   62 | 
   63 | 	ToolCallLimitBehavior onLimitExceeded() {
   64 | 		return this.onLimitExceeded;
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Tally tool calls already executed earlier in this turn by scanning the
   69 | 	 * {@link ToolResponseMessage.ToolResponse} entries present in the current turn's
   70 | 	 * portion of the conversation history. The
   71 | 	 * {@link org.springframework.ai.chat.prompt.Prompt} passed into
   72 | 	 * {@link DefaultToolCallingManager#executeToolCalls} accumulates every prior round's
   73 | 	 * assistant and tool response messages (see {@code ToolCallingAdvisor}'s tool-calling
   74 | 	 * loop), so this reconstructs the running count without the manager itself holding
   75 | 	 * any per-turn state. Only messages from the current turn - the last
   76 | 	 * {@link UserMessage} onward, matching the Turn definition used by the
   77 | 	 * {@code spring-ai-session} project - are counted, so a long conversation replayed in
   78 | 	 * full by an external {@code ChatMemory} advisor does not inflate the count with tool
   79 | 	 * calls from earlier turns.
   80 | 	 */
   81 | 	static Map<String, Integer> countPriorToolCalls(List<Message> instructions) {
   82 | 		Map<String, Integer> counts = new HashMap<>();
   83 | 		for (Message message : currentTurnMessages(instructions)) {
   84 | 			if (message instanceof ToolResponseMessage toolResponseMessage) {
   85 | 				for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
   86 | 					counts.merge(response.name(), 1, Integer::sum);
   87 | 				}
   88 | 			}
   89 | 		}
   90 | 		return counts;
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * The current, still-open turn: the last {@link UserMessage} in {@code instructions}
   95 | 	 * plus everything after it. Falls back to the full list if no {@link UserMessage} is
   96 | 	 * present.
   97 | 	 */
   98 | 	private static List<Message> currentTurnMessages(List<Message> instructions) {
   99 | 		for (int i = instructions.size() - 1; i >= 0; i--) {
  100 | 			if (instructions.get(i) instanceof UserMessage) {
  101 | 				return instructions.subList(i, instructions.size());
  102 | 			}
  103 | 		}
  104 | 		return instructions;
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Checks the given call counts (inclusive of the call about to be made) against the
  109 | 	 * configured limits.
  110 | 	 * @return a {@link Breach} describing the limit that was hit, or {@code null} if the
  111 | 	 * call is within all configured limits.
  112 | 	 */
  113 | 	@Nullable Breach check(String toolName, int toolCallCount, int totalToolCallCount) {
  114 | 		if (!this.toolsExcludedFromLimit.contains(toolName)) {
  115 | 			int maxCallsForTool = this.maxCallsPerTool.getOrDefault(toolName, this.defaultMaxCallsPerTool);
  116 | 			if (maxCallsForTool != NO_LIMIT && toolCallCount > maxCallsForTool) {
  117 | 				return new Breach(toolName, maxCallsForTool,
  118 | 						"Tool call limit (%d) exceeded for tool '%s'. No further calls to this tool are allowed in this turn."
  119 | 							.formatted(maxCallsForTool, toolName));
  120 | 			}
  121 | 		}
  122 | 
  123 | 		if (this.maxTotalToolCalls != NO_LIMIT && totalToolCallCount > this.maxTotalToolCalls) {
  124 | 			return new Breach(null, this.maxTotalToolCalls,
  125 | 					"Total tool call limit (%d) exceeded for this turn. No further tool calls are allowed."
  126 | 						.formatted(this.maxTotalToolCalls));
  127 | 		}
  128 | 
  129 | 		return null;
  130 | 	}
  131 | 
  132 | 	record Breach(@Nullable String toolName, int limit, String message) {
  133 | 	}
  134 | 
  135 | }
```
