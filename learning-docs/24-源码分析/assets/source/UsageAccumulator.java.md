<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/UsageAccumulator.java -->
<!-- 正文引用：[[源码:UsageAccumulator.java#L起-L止]] ；代码块头部：(src: UsageAccumulator.java#L起-L止) -->

# UsageAccumulator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/UsageAccumulator.java` |
| 行数 | 177 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.187Z |

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
   17 | package org.springframework.ai.chat.client.advisor;
   18 | 
   19 | import java.util.List;
   20 | import java.util.Objects;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | import reactor.core.publisher.Flux;
   24 | 
   25 | import org.springframework.ai.chat.client.ChatClientResponse;
   26 | import org.springframework.ai.chat.metadata.Usage;
   27 | import org.springframework.ai.chat.model.ChatResponse;
   28 | import org.springframework.ai.support.UsageCalculator;
   29 | 
   30 | /**
   31 |  * Accumulates token {@link Usage} across the iterations of a recursive advisor (for
   32 |  * example a tool-calling loop or a structured-output validation retry loop), so that the
   33 |  * final response reports the cumulative usage of every model call rather than only the
   34 |  * last one.
   35 |  * <p>
   36 |  * An instance is stateful and not thread-safe: create one per non-streaming
   37 |  * {@code adviseCall} invocation, or one per stream subscription (inside a
   38 |  * {@link Flux#defer}) to keep the accumulated usage subscription-local.
   39 |  * <p>
   40 |  * The token arithmetic and {@link ChatResponse} metadata copying are delegated to
   41 |  * {@link UsageCalculator}; this class owns the recursive advisor loop state and the final
   42 |  * response stamping.
   43 |  * <p>
   44 |  * Typical non-streaming use:
   45 |  *
   46 |  * <pre>{@code
   47 |  * UsageAccumulator usage = new UsageAccumulator();
   48 |  * ChatClientResponse response;
   49 |  * do {
   50 |  *     response = chain.nextCall(request);
   51 |  *     usage.addRoundResponse(response.chatResponse());
   52 |  *     // ... decide whether to loop again ...
   53 |  * }
   54 |  * while (loopAgain);
   55 |  * return usage.applyAccumulatedUsage(response);
   56 |  * }</pre>
   57 |  *
   58 |  * @author Christian Tzolov
   59 |  * @author Jewoo Shin
   60 |  * @since 2.0.0
   61 |  */
   62 | public final class UsageAccumulator {
   63 | 
   64 | 	private @Nullable ChatResponse accumulated;
   65 | 
   66 | 	/**
   67 | 	 * Returns the cumulative usage folded in so far, or {@code null} if none. In a
   68 | 	 * streaming loop this is the total of the <em>previous</em> rounds, since it is read
   69 | 	 * before the in-flight round is {@link #addRoundResponse(ChatResponse) added}.
   70 | 	 * @return the accumulated chat response carrying the cumulative usage, or
   71 | 	 * {@code null}
   72 | 	 */
   73 | 	public @Nullable ChatResponse accumulatedResponse() {
   74 | 		return this.accumulated;
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Folds the usage of the given round response into the running total.
   79 | 	 * @param roundChatResponse the response produced by the current round, or
   80 | 	 * {@code null}
   81 | 	 * @return the updated cumulative total, or {@code null} if still empty
   82 | 	 */
   83 | 	public @Nullable ChatResponse addRoundResponse(@Nullable ChatResponse roundChatResponse) {
   84 | 		this.accumulated = UsageCalculator.accumulateResponseUsage(roundChatResponse, this.accumulated);
   85 | 		return this.accumulated;
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Stamps the accumulated total usage onto the given response. Used to finalize a
   90 | 	 * non-streaming loop or a streaming return-direct result. The response is returned
   91 | 	 * unchanged when there is nothing accumulated, or when its usage already equals the
   92 | 	 * accumulated total.
   93 | 	 * @param chatClientResponse the response to stamp the total onto
   94 | 	 * @return the response carrying the accumulated total usage
   95 | 	 */
   96 | 	public ChatClientResponse applyAccumulatedUsage(ChatClientResponse chatClientResponse) {
   97 | 		return stampTotalUsage(chatClientResponse, this.accumulated);
   98 | 	}
   99 | 
  100 | 	private static ChatClientResponse stampTotalUsage(ChatClientResponse chatClientResponse,
  101 | 			@Nullable ChatResponse accumulatedChatResponse) {
  102 | 		ChatResponse currentChatResponse = chatClientResponse.chatResponse();
  103 | 		if (currentChatResponse == null || accumulatedChatResponse == null) {
  104 | 			return chatClientResponse;
  105 | 		}
  106 | 		Usage accumulatedUsage = accumulatedChatResponse.getMetadata().getUsage();
  107 | 		if (UsageCalculator.isEmpty(accumulatedUsage)
  108 | 				|| Objects.equals(currentChatResponse.getMetadata().getUsage(), accumulatedUsage)) {
  109 | 			return chatClientResponse;
  110 | 		}
  111 | 		return chatClientResponse.mutate()
  112 | 			.chatResponse(UsageCalculator.withUsage(currentChatResponse, accumulatedUsage))
  113 | 			.build();
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Adds the previous rounds' accumulated usage onto a single streamed chunk that
  118 | 	 * already carries its own usage. Chunks without usage (or when there is no previous
  119 | 	 * accumulated response) are returned unchanged, so the previous rounds' usage is
  120 | 	 * reflected only on usage-bearing chunks.
  121 | 	 * @param chunk the streamed chunk
  122 | 	 * @param previousAccumulatedResponse the response carrying the accumulated usage of
  123 | 	 * previous rounds, or {@code null}
  124 | 	 * @return the chunk with the previous rounds' usage added to its usage, or unchanged
  125 | 	 */
  126 | 	static ChatClientResponse applyPreviousAccumulatedUsageToChunk(ChatClientResponse chunk,
  127 | 			@Nullable ChatResponse previousAccumulatedResponse) {
  128 | 		ChatResponse currentChatResponse = chunk.chatResponse();
  129 | 		if (currentChatResponse == null || previousAccumulatedResponse == null) {
  130 | 			return chunk;
  131 | 		}
  132 | 		Usage currentUsage = currentChatResponse.getMetadata().getUsage();
  133 | 		if (UsageCalculator.isEmpty(currentUsage)
  134 | 				|| UsageCalculator.isEmpty(previousAccumulatedResponse.getMetadata().getUsage())) {
  135 | 			return chunk;
  136 | 		}
  137 | 		Usage cumulativeUsage = UsageCalculator.getCumulativeUsage(currentUsage, previousAccumulatedResponse);
  138 | 		if (Objects.equals(currentUsage, cumulativeUsage)) {
  139 | 			return chunk;
  140 | 		}
  141 | 		return chunk.mutate().chatResponse(UsageCalculator.withUsage(currentChatResponse, cumulativeUsage)).build();
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Builds the trailing usage-only emission for a streaming loop whose final round
  146 | 	 * reported no usage of its own. In that case the accumulated total from previous
  147 | 	 * rounds was never stamped onto an emitted chunk, so a usage-only
  148 | 	 * {@link ChatClientResponse} (with empty generations, so no content is duplicated)
  149 | 	 * carrying the accumulated total is emitted. Returns an empty flux when no correction
  150 | 	 * is needed (the round reported its own usage, or there is nothing accumulated to
  151 | 	 * preserve).
  152 | 	 * @param aggregatedResponse the aggregated response of the final round
  153 | 	 * @param roundChatResponse the final round's own response (its own usage)
  154 | 	 * @param accumulatedChatResponse the response carrying the cumulative usage
  155 | 	 * @return a single usage-only response, or an empty flux when no correction is needed
  156 | 	 */
  157 | 	static Flux<ChatClientResponse> emitFinalUsageCorrectionIfNecessary(ChatClientResponse aggregatedResponse,
  158 | 			@Nullable ChatResponse roundChatResponse, @Nullable ChatResponse accumulatedChatResponse) {
  159 | 		if (accumulatedChatResponse == null) {
  160 | 			return Flux.empty();
  161 | 		}
  162 | 		Usage accumulatedUsage = accumulatedChatResponse.getMetadata().getUsage();
  163 | 		if (!UsageCalculator.isEmpty(getUsage(roundChatResponse)) || UsageCalculator.isEmpty(accumulatedUsage)) {
  164 | 			return Flux.empty();
  165 | 		}
  166 | 		ChatResponse usageOnlyChatResponse = ChatResponse.builder()
  167 | 			.from(accumulatedChatResponse)
  168 | 			.generations(List.of())
  169 | 			.build();
  170 | 		return Flux.just(aggregatedResponse.mutate().chatResponse(usageOnlyChatResponse).build());
  171 | 	}
  172 | 
  173 | 	private static @Nullable Usage getUsage(@Nullable ChatResponse chatResponse) {
  174 | 		return chatResponse != null ? chatResponse.getMetadata().getUsage() : null;
  175 | 	}
  176 | 
  177 | }
```
