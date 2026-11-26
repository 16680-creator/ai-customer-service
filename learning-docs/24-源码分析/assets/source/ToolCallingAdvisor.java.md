<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.java -->
<!-- 正文引用：[[源码:ToolCallingAdvisor.java#L起-L止]] ；代码块头部：(src: ToolCallingAdvisor.java#L起-L止) -->

# ToolCallingAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.java` |
| 行数 | 745 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:32:11.178Z |

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
   20 | import java.util.concurrent.atomic.AtomicReference;
   21 | 
   22 | import reactor.core.publisher.Flux;
   23 | import reactor.core.scheduler.Schedulers;
   24 | 
   25 | import org.springframework.ai.chat.client.ChatClientMessageAggregator;
   26 | import org.springframework.ai.chat.client.ChatClientRequest;
   27 | import org.springframework.ai.chat.client.ChatClientResponse;
   28 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   29 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   30 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   31 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   32 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   33 | import org.springframework.ai.chat.client.advisor.api.ToolAdvisor;
   34 | import org.springframework.ai.chat.messages.AssistantMessage;
   35 | import org.springframework.ai.chat.messages.Message;
   36 | import org.springframework.ai.chat.messages.part.MessagePart;
   37 | import org.springframework.ai.chat.messages.part.ToolCallPart;
   38 | import org.springframework.ai.chat.model.ChatResponse;
   39 | import org.springframework.ai.chat.model.Generation;
   40 | import org.springframework.ai.chat.prompt.ChatOptions;
   41 | import org.springframework.ai.chat.prompt.Prompt;
   42 | import org.springframework.ai.model.tool.ToolCallConfirmationRequiredException;
   43 | import org.springframework.ai.model.tool.ToolCallDecisions;
   44 | import org.springframework.ai.model.tool.ToolCallLimitExceededException;
   45 | import org.springframework.ai.model.tool.ToolCallingChatOptions;
   46 | import org.springframework.ai.model.tool.ToolCallingManager;
   47 | import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
   48 | import org.springframework.ai.model.tool.ToolExecutionResult;
   49 | import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
   50 | import org.springframework.core.Ordered;
   51 | import org.springframework.util.Assert;
   52 | 
   53 | /**
   54 |  * Recursive Advisor that disables the internal tool execution flow and instead implements
   55 |  * the tool calling loop as part of the advisor chain.
   56 |  * <p>
   57 |  * It uses the CallAdvisorChainUtil to implement looping advisor chain calls.
   58 |  * <p>
   59 |  * This enables intercepting the tool calling loop by the rest of the advisors next in the
   60 |  * chain.
   61 |  * <p>
   62 |  * When the prompt options do not implement
   63 |  * {@link org.springframework.ai.model.tool.ToolCallingChatOptions} (e.g. for models that
   64 |  * don't support tool calling), this advisor passes the request through to the next
   65 |  * advisor without modification.
   66 |  * <p>
   67 |  * <b>Tool calls that need confirmation.</b> When the model asks for a tool call that the
   68 |  * {@link ToolCallingManager}'s
   69 |  * {@link org.springframework.ai.model.tool.ToolCallConfirmationPolicy} says needs a
   70 |  * person's confirmation (by default, a tool marked {@code requiresConfirmation}), the
   71 |  * manager throws {@link ToolCallConfirmationRequiredException} and the loop
   72 |  * <em>pauses</em>: none of the reply's tool calls run, and the reply goes back to the
   73 |  * caller, listing the keys of the calls that wait for a decision
   74 |  * ({@link ToolCallDecisions#awaitingDecision}). A streamed reply pauses on a last chunk
   75 |  * with the tool calls and the keys. The loop keeps nothing. To resume, the application
   76 |  * runs the calls with the decisions through
   77 |  * {@link ToolCallingManager#executeToolCalls(Prompt, ChatResponse, ToolCallDecisions)}
   78 |  * and sends the conversation, with the paused reply and the tool results, as its next
   79 |  * request. A memory advisor outside the loop then stores the tool results like any other
   80 |  * message.
   81 |  *
   82 |  * @author Christian Tzolov
   83 |  * @author Jewoo Shin
   84 |  * @since 2.0.0
   85 |  */
   86 | public class ToolCallingAdvisor implements CallAdvisor, StreamAdvisor, ToolAdvisor {
   87 | 
   88 | 	private static final ChatClientMessageAggregator CHAT_CLIENT_MESSAGE_AGGREGATOR = new ChatClientMessageAggregator();
   89 | 
   90 | 	/**
   91 | 	 * Default advisor order. Higher than
   92 | 	 * {@link org.springframework.ai.chat.client.advisor.api.Advisor#DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}
   93 | 	 * (+200) so that memory advisors at their default order are placed outside this
   94 | 	 * advisor and do not participate in each tool-call iteration.
   95 | 	 */
   96 | 	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 300;
   97 | 
   98 | 	protected static final ToolExecutionEligibilityChecker DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER = chatResponse -> chatResponse != null
   99 | 			&& chatResponse.hasToolCalls();
  100 | 
  101 | 	protected final ToolCallingManager toolCallingManager;
  102 | 
  103 | 	private final ToolExecutionEligibilityChecker toolExecutionEligibilityChecker;
  104 | 
  105 | 	/**
  106 | 	 * Set the order close to {@link Ordered#LOWEST_PRECEDENCE} to ensure an advisor is
  107 | 	 * executed first in the chain (first for request processing, last for response
  108 | 	 * processing).
  109 | 	 * <p>
  110 | 	 * https://docs.spring.io/spring-ai/reference/api/advisors.html#_advisor_order
  111 | 	 */
  112 | 	private final int advisorOrder;
  113 | 
  114 | 	private final boolean conversationHistoryEnabled;
  115 | 
  116 | 	protected ToolCallingAdvisor(ToolCallingManager toolCallingManager,
  117 | 			ToolExecutionEligibilityChecker toolExecutionEligibilityChecker, int advisorOrder,
  118 | 			boolean conversationHistoryEnabled) {
  119 | 		Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
  120 | 		Assert.notNull(toolExecutionEligibilityChecker, "toolExecutionEligibilityChecker must not be null");
  121 | 		Assert.isTrue(advisorOrder > BaseAdvisor.HIGHEST_PRECEDENCE && advisorOrder < BaseAdvisor.LOWEST_PRECEDENCE,
  122 | 				"advisorOrder must be between HIGHEST_PRECEDENCE and LOWEST_PRECEDENCE");
  123 | 
  124 | 		this.toolCallingManager = toolCallingManager;
  125 | 		this.toolExecutionEligibilityChecker = toolExecutionEligibilityChecker;
  126 | 		this.advisorOrder = advisorOrder;
  127 | 		this.conversationHistoryEnabled = conversationHistoryEnabled;
  128 | 	}
  129 | 
  130 | 	@Override
  131 | 	public String getName() {
  132 | 		return "Tool Calling Advisor";
  133 | 	}
  134 | 
  135 | 	@Override
  136 | 	public int getOrder() {
  137 | 		return this.advisorOrder;
  138 | 	}
  139 | 
  140 | 	// -------------------------------------------------------------------------
  141 | 	// Call (non-streaming) implementation
  142 | 	// -------------------------------------------------------------------------
  143 | 	@Override
  144 | 	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
  145 | 		Assert.notNull(callAdvisorChain, "callAdvisorChain must not be null");
  146 | 		Assert.notNull(chatClientRequest, "chatClientRequest must not be null");
  147 | 
  148 | 		ChatOptions options = chatClientRequest.prompt().getOptions();
  149 | 		if (!(options instanceof ToolCallingChatOptions toolCallingChatOptions)) {
  150 | 			// No tool calling options - skip the tool calling advisor
  151 | 			return callAdvisorChain.nextCall(chatClientRequest);
  152 | 		}
  153 | 
  154 | 		chatClientRequest = this.doInitializeLoop(chatClientRequest, callAdvisorChain);
  155 | 
  156 | 		var instructions = chatClientRequest.prompt().getInstructions();
  157 | 		// Always the complete history for this turn, independent of what
  158 | 		// doGetNextInstructionsForToolCall trims `instructions` down to for forwarding
  159 | 		// to the rest of the chain (e.g. for a memory advisor nested inside the loop
  160 | 		// that doesn't itself de-duplicate). This is what the ToolCallingManager sees,
  161 | 		// so trimming the forwarded prompt never hides earlier rounds from its tool
  162 | 		// call limit counting.
  163 | 		var fullTurnHistory = instructions;
  164 | 
  165 | 		ChatClientResponse chatClientResponse = null;
  166 | 		UsageAccumulator usageAccumulator = new UsageAccumulator();
  167 | 
  168 | 		boolean isToolCall = false;
  169 | 
  170 | 		do {
  171 | 
  172 | 			// Before Call
  173 | 			var processedChatClientRequest = ChatClientRequest.builder()
  174 | 				.prompt(new Prompt(instructions, toolCallingChatOptions))
  175 | 				.context(chatClientRequest.context())
  176 | 				.build();
  177 | 
  178 | 			// Next Call
  179 | 			processedChatClientRequest = this.doBeforeCall(processedChatClientRequest, callAdvisorChain);
  180 | 
  181 | 			// Re-read the options doBeforeCall() may have mutated (e.g. a subclass
  182 | 			// injecting per-iteration tool callbacks) so executeToolCalls() below sees
  183 | 			// what was actually sent to the model, not the pre-doBeforeCall snapshot.
  184 | 			toolCallingChatOptions = (ToolCallingChatOptions) processedChatClientRequest.prompt().getOptions();
  185 | 			Assert.notNull(toolCallingChatOptions,
  186 | 					"redundant check that should never fail (doBeforeCall must not change the options type away from ToolCallingChatOptions), but here to help NullAway");
  187 | 
  188 | 			chatClientResponse = callAdvisorChain.copy(this).nextCall(processedChatClientRequest);
  189 | 
  190 | 			chatClientResponse = this.doAfterCall(chatClientResponse, callAdvisorChain);
  191 | 
  192 | 			// After Call
  193 | 			ChatResponse chatResponse = chatClientResponse.chatResponse();
  194 | 			usageAccumulator.addRoundResponse(chatResponse);
  195 | 			isToolCall = this.toolExecutionEligibilityChecker.isToolCallResponse(chatResponse);
  196 | 
  197 | 			if (isToolCall) {
  198 | 				Assert.notNull(chatResponse, "redundant check that should never fail, but here to help NullAway");
  199 | 
  200 | 				ToolExecutionResult toolExecutionResult;
  201 | 				try {
  202 | 					toolExecutionResult = this.toolCallingManager
  203 | 						.executeToolCalls(new Prompt(fullTurnHistory, toolCallingChatOptions), chatResponse);
  204 | 				}
  205 | 				catch (ToolCallLimitExceededException ex) {
  206 | 					// Return the breach as a single generation - directly to the
  207 | 					// application client instead of looping back to the LLM.
  208 | 					chatClientResponse = chatClientResponse.mutate()
  209 | 						.chatResponse(ChatResponse.builder()
  210 | 							.from(chatResponse)
  211 | 							.generations(List.of(ex.buildGeneration()))
  212 | 							.build())
  213 | 						.build();
  214 | 					break;
  215 | 				}
  216 | 				catch (ToolCallConfirmationRequiredException ex) {
  217 | 					// A call that needs a person's confirmation pauses the loop: none of
  218 | 					// the reply's calls ran, the reply goes back to the caller with the
  219 | 					// keys of the calls that wait for a decision.
  220 | 					chatClientResponse = paused(chatClientResponse, chatResponse, ex);
  221 | 					break;
  222 | 				}
  223 | 
  224 | 				fullTurnHistory = toolExecutionResult.conversationHistory();
  225 | 
  226 | 				if (toolExecutionResult.returnDirect()) {
  227 | 
  228 | 					// Return tool execution result directly to the application client
  229 | 					// instead of returning it to the LLM.
  230 | 					chatClientResponse = chatClientResponse.mutate()
  231 | 						.chatResponse(ChatResponse.builder()
  232 | 							.from(chatResponse)
  233 | 							.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
  234 | 							.build())
  235 | 						.build();
  236 | 					break;
  237 | 				}
  238 | 
  239 | 				instructions = this.doGetNextInstructionsForToolCall(processedChatClientRequest, chatClientResponse,
  240 | 						toolExecutionResult);
  241 | 			}
  242 | 
  243 | 		}
  244 | 		while (isToolCall); // loop until no tool calls are present
  245 | 
  246 | 		chatClientResponse = usageAccumulator.applyAccumulatedUsage(chatClientResponse);
  247 | 		return this.doFinalizeLoop(chatClientResponse, callAdvisorChain);
  248 | 	}
  249 | 
  250 | 	protected List<Message> doGetNextInstructionsForToolCall(ChatClientRequest chatClientRequest,
  251 | 			ChatClientResponse chatClientResponse, ToolExecutionResult toolExecutionResult) {
  252 | 
  253 | 		if (!this.conversationHistoryEnabled) {
  254 | 			List<Message> history = toolExecutionResult.conversationHistory();
  255 | 			if (history.isEmpty()) {
  256 | 				return history;
  257 | 			}
  258 | 			return List.of(chatClientRequest.prompt().getSystemMessage(), history.get(history.size() - 1));
  259 | 		}
  260 | 
  261 | 		return toolExecutionResult.conversationHistory();
  262 | 	}
  263 | 
  264 | 	protected ChatClientResponse doFinalizeLoop(ChatClientResponse chatClientResponse,
  265 | 			CallAdvisorChain callAdvisorChain) {
  266 | 		return chatClientResponse;
  267 | 	}
  268 | 
  269 | 	protected ChatClientRequest doInitializeLoop(ChatClientRequest chatClientRequest,
  270 | 			CallAdvisorChain callAdvisorChain) {
  271 | 		return chatClientRequest;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Hook method called before each call in the tool call loop. Subclasses can override
  276 | 	 * to customize pre-call behavior, e.g. to add per-round tool callbacks to the
  277 | 	 * options.
  278 | 	 * @param chatClientRequest the request about to be processed
  279 | 	 * @param callAdvisorChain the call advisor chain
  280 | 	 * @return the potentially modified request
  281 | 	 */
  282 | 	protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
  283 | 		return chatClientRequest;
  284 | 	}
  285 | 
  286 | 	protected ChatClientResponse doAfterCall(ChatClientResponse chatClientResponse, CallAdvisorChain callAdvisorChain) {
  287 | 		return chatClientResponse;
  288 | 	}
  289 | 
  290 | 	// -------------------------------------------------------------------------
  291 | 	// Streaming implementation
  292 | 	// -------------------------------------------------------------------------
  293 | 	@Override
  294 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
  295 | 			StreamAdvisorChain streamAdvisorChain) {
  296 | 		Assert.notNull(streamAdvisorChain, "streamAdvisorChain must not be null");
  297 | 		Assert.notNull(chatClientRequest, "chatClientRequest must not be null");
  298 | 
  299 | 		ChatOptions options = chatClientRequest.prompt().getOptions();
  300 | 		if (!(options instanceof ToolCallingChatOptions toolCallingChatOptions)) {
  301 | 			// No tool calling options - skip the tool calling advisor
  302 | 			return streamAdvisorChain.nextStream(chatClientRequest);
  303 | 		}
  304 | 
  305 | 		return Flux.defer(() -> {
  306 | 			ChatClientRequest initializedRequest = this.doInitializeLoopStream(chatClientRequest, streamAdvisorChain);
  307 | 			// Subscription-local accumulator so usage is not shared across subscriptions.
  308 | 			UsageAccumulator usageAccumulator = new UsageAccumulator();
  309 | 			List<Message> initialInstructions = initializedRequest.prompt().getInstructions();
  310 | 			return this.internalStream(streamAdvisorChain, initializedRequest, toolCallingChatOptions,
  311 | 					initialInstructions, initialInstructions, usageAccumulator);
  312 | 		});
  313 | 	}
  314 | 
  315 | 	private Flux<ChatClientResponse> internalStream(StreamAdvisorChain streamAdvisorChain,
  316 | 			ChatClientRequest originalRequest, ToolCallingChatOptions toolCallingChatOptions,
  317 | 			List<Message> instructions, List<Message> fullTurnHistory, UsageAccumulator usageAccumulator) {
  318 | 
  319 | 		return Flux.deferContextual(contextView -> {
  320 | 			// Build request with current instructions
  321 | 			var processedRequest = ChatClientRequest.builder()
  322 | 				.prompt(new Prompt(instructions, toolCallingChatOptions))
  323 | 				.context(originalRequest.context())
  324 | 				.build();
  325 | 
  326 | 			processedRequest = this.doBeforeStream(processedRequest, streamAdvisorChain);
  327 | 
  328 | 			// Get a copy of the chain excluding this advisor
  329 | 			StreamAdvisorChain chainCopy = streamAdvisorChain.copy(this);
  330 | 
  331 | 			final ChatClientRequest finalRequest = processedRequest;
  332 | 
  333 | 			Flux<ChatClientResponse> responseFlux = chainCopy.nextStream(processedRequest);
  334 | 
  335 | 			// Re-read the options doBeforeStream() may have mutated (e.g. a subclass
  336 | 			// injecting per-iteration tool callbacks) so executeToolCalls() below sees
  337 | 			// what was actually sent to the model, not the pre-doBeforeStream snapshot.
  338 | 			ToolCallingChatOptions processedOptions = (ToolCallingChatOptions) finalRequest.prompt().getOptions();
  339 | 			Assert.notNull(processedOptions,
  340 | 					"redundant check that should never fail (doBeforeStream must not change the options type away from ToolCallingChatOptions), but here to help NullAway");
  341 | 
  342 | 			return streamWithToolCallResponses(responseFlux, finalRequest, fullTurnHistory, streamAdvisorChain,
  343 | 					originalRequest, processedOptions, usageAccumulator);
  344 | 		});
  345 | 	}
  346 | 
  347 | 	private Flux<ChatClientResponse> streamWithToolCallResponses(Flux<ChatClientResponse> responseFlux,
  348 | 			ChatClientRequest finalRequest, List<Message> fullTurnHistory, StreamAdvisorChain streamAdvisorChain,
  349 | 			ChatClientRequest originalRequest, ToolCallingChatOptions optionsCopy, UsageAccumulator usageAccumulator) {
  350 | 
  351 | 		AtomicReference<ChatClientResponse> aggregatedResponseRef = new AtomicReference<>();
  352 | 		// Snapshot of the usage accumulated from previous rounds (before this round).
  353 | 		ChatResponse previousAccumulatedResponse = usageAccumulator.accumulatedResponse();
  354 | 
  355 | 		return CHAT_CLIENT_MESSAGE_AGGREGATOR.aggregateChatClientResponse(responseFlux, aggregatedResponseRef::set)
  356 | 			.map(chatClientResponse -> UsageAccumulator.applyPreviousAccumulatedUsageToChunk(chatClientResponse,
  357 | 					previousAccumulatedResponse))
  358 | 			.concatWith(Flux.defer(() -> this.handleToolCallRecursion(aggregatedResponseRef.get(), finalRequest,
  359 | 					fullTurnHistory, streamAdvisorChain, originalRequest, optionsCopy, usageAccumulator)))
  360 | 			.filter(ccr -> !this.toolExecutionEligibilityChecker.isToolCallResponse(ccr.chatResponse())
  361 | 					|| ToolCallDecisions.isAwaitingDecision(ccr.chatResponse()));
  362 | 	}
  363 | 
  364 | 	/**
  365 | 	 * Handles tool call detection and recursion after streaming completes. Returns empty
  366 | 	 * flux if no tool call, or recursive stream if tool call detected.
  367 | 	 */
  368 | 	private Flux<ChatClientResponse> handleToolCallRecursion(ChatClientResponse aggregatedResponse,
  369 | 			ChatClientRequest finalRequest, List<Message> fullTurnHistory, StreamAdvisorChain streamAdvisorChain,
  370 | 			ChatClientRequest originalRequest, ToolCallingChatOptions optionsCopy, UsageAccumulator usageAccumulator) {
  371 | 
  372 | 		if (aggregatedResponse == null) {
  373 | 			return Flux.empty();
  374 | 		}
  375 | 
  376 | 		aggregatedResponse = this.doAfterStream(aggregatedResponse, streamAdvisorChain);
  377 | 
  378 | 		ChatResponse chatResponse = aggregatedResponse.chatResponse();
  379 | 		ChatResponse accumulatedChatResponse = usageAccumulator.addRoundResponse(chatResponse);
  380 | 		boolean isToolCall = this.toolExecutionEligibilityChecker.isToolCallResponse(chatResponse);
  381 | 
  382 | 		if (!isToolCall) {
  383 | 			// No tool call: the final round's chunks have already been streamed. If that
  384 | 			// round carried no usage of its own, the usage accumulated from previous
  385 | 			// rounds was never stamped onto any emitted chunk, so emit a trailing
  386 | 			// usage-only response to keep the aggregated stream usage correct. Otherwise
  387 | 			// nothing more needs to be emitted.
  388 | 			Flux<ChatClientResponse> finalEmissions = UsageAccumulator
  389 | 				.emitFinalUsageCorrectionIfNecessary(aggregatedResponse, chatResponse, accumulatedChatResponse);
  390 | 			return this.doFinalizeLoopStream(finalEmissions, streamAdvisorChain);
  391 | 		}
  392 | 
  393 | 		Assert.notNull(chatResponse, "redundant check that should never fail, but here to help NullAway");
  394 | 
  395 | 		final ChatClientResponse finalAggregatedResponse = aggregatedResponse;
  396 | 
  397 | 		// Execute tool calls on bounded elastic scheduler (tool execution is blocking)
  398 | 		Flux<ChatClientResponse> toolCallFlux = Flux.deferContextual(ctx -> {
  399 | 			ToolExecutionResult toolExecutionResult;
  400 | 			try {
  401 | 				ToolCallReactiveContextHolder.setContext(ctx);
  402 | 				toolExecutionResult = this.toolCallingManager.executeToolCalls(new Prompt(fullTurnHistory, optionsCopy),
  403 | 						chatResponse);
  404 | 			}
  405 | 			catch (ToolCallLimitExceededException ex) {
  406 | 				// A configured tool call limit was hit. Return the breach as a single
  407 | 				// generation - rather than mixing it with any successful calls from
  408 | 				// the same batch as if they were equally valid alternative answers -
  409 | 				// directly to the application client instead of looping back to the
  410 | 				// LLM.
  411 | 				ChatClientResponse limitResponse = finalAggregatedResponse.mutate()
  412 | 					.chatResponse(ChatResponse.builder()
  413 | 						.from(chatResponse)
  414 | 						.generations(List.of(ex.buildGeneration()))
  415 | 						.build())
  416 | 					.build();
  417 | 				return Flux.just(usageAccumulator.applyAccumulatedUsage(limitResponse));
  418 | 			}
  419 | 			catch (ToolCallConfirmationRequiredException ex) {
  420 | 				// A call that needs a person's confirmation pauses the loop. The reply's
  421 | 				// text was streamed already; a last chunk carries its tool calls and the
  422 | 				// keys of the calls that wait for a decision.
  423 | 				ChatClientResponse pausedResponse = pausedChunk(finalAggregatedResponse, chatResponse, ex);
  424 | 				return this.doFinalizeLoopStream(Flux.just(usageAccumulator.applyAccumulatedUsage(pausedResponse)),
  425 | 						streamAdvisorChain);
  426 | 			}
  427 | 			finally {
  428 | 				ToolCallReactiveContextHolder.clearContext();
  429 | 			}
  430 | 
  431 | 			if (toolExecutionResult.returnDirect()) {
  432 | 				// Return tool execution result directly to the application client
  433 | 				ChatClientResponse directResponse = finalAggregatedResponse.mutate()
  434 | 					.chatResponse(ChatResponse.builder()
  435 | 						.from(chatResponse)
  436 | 						.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
  437 | 						.build())
  438 | 					.build();
  439 | 				return Flux.just(usageAccumulator.applyAccumulatedUsage(directResponse));
  440 | 			}
  441 | 			else {
  442 | 				// Recursive call with updated conversation history
  443 | 				List<Message> nextInstructions = this.doGetNextInstructionsForToolCallStream(finalRequest,
  444 | 						finalAggregatedResponse, toolExecutionResult);
  445 | 				return this.internalStream(streamAdvisorChain, originalRequest, optionsCopy, nextInstructions,
  446 | 						toolExecutionResult.conversationHistory(), usageAccumulator);
  447 | 			}
  448 | 		});
  449 | 		return toolCallFlux.subscribeOn(Schedulers.boundedElastic());
  450 | 	}
  451 | 
  452 | 	/**
  453 | 	 * Hook method called at the start of the streaming tool call loop. Subclasses can
  454 | 	 * override to customize initialization behavior.
  455 | 	 * @param chatClientRequest the initial request
  456 | 	 * @param streamAdvisorChain the stream advisor chain
  457 | 	 * @return the potentially modified request
  458 | 	 */
  459 | 	protected ChatClientRequest doInitializeLoopStream(ChatClientRequest chatClientRequest,
  460 | 			StreamAdvisorChain streamAdvisorChain) {
  461 | 		return chatClientRequest;
  462 | 	}
  463 | 
  464 | 	/**
  465 | 	 * Hook method called before each streaming call in the tool call loop. Subclasses can
  466 | 	 * override to customize pre-call behavior.
  467 | 	 * @param chatClientRequest the request about to be processed
  468 | 	 * @param streamAdvisorChain the stream advisor chain
  469 | 	 * @return the potentially modified request
  470 | 	 */
  471 | 	protected ChatClientRequest doBeforeStream(ChatClientRequest chatClientRequest,
  472 | 			StreamAdvisorChain streamAdvisorChain) {
  473 | 		return chatClientRequest;
  474 | 	}
  475 | 
  476 | 	/**
  477 | 	 * Hook method called after each streaming call in the tool call loop. Subclasses can
  478 | 	 * override to customize post-call behavior.
  479 | 	 * @param chatClientResponse the response from the call
  480 | 	 * @param streamAdvisorChain the stream advisor chain
  481 | 	 * @return the potentially modified response
  482 | 	 */
  483 | 	protected ChatClientResponse doAfterStream(ChatClientResponse chatClientResponse,
  484 | 			StreamAdvisorChain streamAdvisorChain) {
  485 | 		return chatClientResponse;
  486 | 	}
  487 | 
  488 | 	/**
  489 | 	 * Hook method called at the end of the streaming tool call loop to finalize the
  490 | 	 * response. Subclasses can override to customize finalization behavior.
  491 | 	 * @param chatClientResponseFlux the flux of collected response chunks to emit
  492 | 	 * @param streamAdvisorChain the stream advisor chain
  493 | 	 * @return the potentially modified flux of responses
  494 | 	 */
  495 | 	protected Flux<ChatClientResponse> doFinalizeLoopStream(Flux<ChatClientResponse> chatClientResponseFlux,
  496 | 			StreamAdvisorChain streamAdvisorChain) {
  497 | 		return chatClientResponseFlux;
  498 | 	}
  499 | 
  500 | 	/**
  501 | 	 * Hook method to determine the next instructions for a tool call iteration in
  502 | 	 * streaming mode. Subclasses can override to customize conversation history handling.
  503 | 	 * @param chatClientRequest the current request
  504 | 	 * @param chatClientResponse the current response
  505 | 	 * @param toolExecutionResult the result of tool execution
  506 | 	 * @return the list of messages to use as instructions for the next iteration
  507 | 	 */
  508 | 	protected List<Message> doGetNextInstructionsForToolCallStream(ChatClientRequest chatClientRequest,
  509 | 			ChatClientResponse chatClientResponse, ToolExecutionResult toolExecutionResult) {
  510 | 
  511 | 		if (!this.conversationHistoryEnabled) {
  512 | 			List<Message> history = toolExecutionResult.conversationHistory();
  513 | 			if (history.isEmpty()) {
  514 | 				return history;
  515 | 			}
  516 | 			return List.of(chatClientRequest.prompt().getSystemMessage(), history.get(history.size() - 1));
  517 | 		}
  518 | 
  519 | 		return toolExecutionResult.conversationHistory();
  520 | 	}
  521 | 
  522 | 	// -------------------------------------------------------------------------
  523 | 	// Tool calls that need confirmation: pause
  524 | 	// -------------------------------------------------------------------------
  525 | 
  526 | 	/**
  527 | 	 * The last chunk of a paused streamed reply: its tool calls and the keys of the calls
  528 | 	 * that wait for a decision. The reply's other parts were streamed already. The tool
  529 | 	 * call parts are the model's, so they keep their payloads (such as a thought
  530 | 	 * signature) and attributes, and the message keeps its metadata. The chunk keeps the
  531 | 	 * reply's generation metadata, so its finish reason still marks the end of the round
  532 | 	 * for the advisors outside the loop.
  533 | 	 */
  534 | 	private static ChatClientResponse pausedChunk(ChatClientResponse aggregated, ChatResponse chatResponse,
  535 | 			ToolCallConfirmationRequiredException ex) {
  536 | 		Generation toolCallGeneration = toolCallGeneration(chatResponse);
  537 | 		AssistantMessage reply = toolCallGeneration.getOutput();
  538 | 		List<MessagePart> toolCallParts = reply.getParts().stream().filter(ToolCallPart.class::isInstance).toList();
  539 | 		AssistantMessage toolCallsOnly = AssistantMessage.builder()
  540 | 			.parts(toolCallParts)
  541 | 			.properties(reply.getMetadata())
  542 | 			.build();
  543 | 		ChatResponse chunk = ChatResponse.builder()
  544 | 			.from(chatResponse)
  545 | 			.generations(List.of(new Generation(toolCallsOnly, toolCallGeneration.getMetadata())))
  546 | 			.metadata(ToolCallDecisions.AWAITING_DECISION_METADATA_KEY, ex.getToolCallIds())
  547 | 			.build();
  548 | 		return aggregated.mutate().chatResponse(chunk).build();
  549 | 	}
  550 | 
  551 | 	/**
  552 | 	 * The paused reply as the model gave it, with the keys of the tool calls that wait
  553 | 	 * for a decision.
  554 | 	 */
  555 | 	private static ChatClientResponse paused(ChatClientResponse response, ChatResponse chatResponse,
  556 | 			ToolCallConfirmationRequiredException ex) {
  557 | 		return response.mutate()
  558 | 			.chatResponse(ChatResponse.builder()
  559 | 				.from(chatResponse)
  560 | 				.metadata(ToolCallDecisions.AWAITING_DECISION_METADATA_KEY, ex.getToolCallIds())
  561 | 				.build())
  562 | 			.build();
  563 | 	}
  564 | 
  565 | 	/**
  566 | 	 * The generation whose tool calls the manager looked at: the first with tool calls.
  567 | 	 */
  568 | 	private static Generation toolCallGeneration(ChatResponse chatResponse) {
  569 | 		return chatResponse.getResults()
  570 | 			.stream()
  571 | 			.filter(generation -> generation.getOutput().hasToolCalls())
  572 | 			.findFirst()
  573 | 			.orElseThrow(() -> new IllegalStateException("No tool call requested by the chat model"));
  574 | 	}
  575 | 
  576 | 	/**
  577 | 	 * Creates a new Builder instance for constructing a ToolCallingAdvisor.
  578 | 	 * @return a new Builder instance
  579 | 	 */
  580 | 	public static Builder<?> builder() {
  581 | 		return new Builder<>();
  582 | 	}
  583 | 
  584 | 	/**
  585 | 	 * Builder for creating instances of ToolCallingAdvisor.
  586 | 	 * <p>
  587 | 	 * This builder uses the self-referential generic pattern to support extensibility.
  588 | 	 *
  589 | 	 * @param <T> the builder type, used for self-referential generics to support method
  590 | 	 * chaining in subclasses
  591 | 	 */
  592 | 	public static class Builder<T extends Builder<T>> {
  593 | 
  594 | 		private ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
  595 | 
  596 | 		private ToolExecutionEligibilityChecker toolExecutionEligibilityChecker = DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER;
  597 | 
  598 | 		private int advisorOrder = DEFAULT_ORDER;
  599 | 
  600 | 		private boolean conversationHistoryEnabled = true;
  601 | 
  602 | 		protected Builder() {
  603 | 		}
  604 | 
  605 | 		/**
  606 | 		 * Returns this builder cast to the appropriate type for method chaining.
  607 | 		 * Subclasses should override this method to return the correct type.
  608 | 		 * @return this builder instance
  609 | 		 */
  610 | 		@SuppressWarnings("unchecked")
  611 | 		protected T self() {
  612 | 			return (T) this;
  613 | 		}
  614 | 
  615 | 		/**
  616 | 		 * Sets the ToolCallingManager to be used by the advisor.
  617 | 		 * @param toolCallingManager the ToolCallingManager instance
  618 | 		 * @return this Builder instance for method chaining
  619 | 		 */
  620 | 		public T toolCallingManager(ToolCallingManager toolCallingManager) {
  621 | 			this.toolCallingManager = toolCallingManager;
  622 | 			return self();
  623 | 		}
  624 | 
  625 | 		/**
  626 | 		 * Sets the checker that determines whether a model response should trigger tool
  627 | 		 * execution. Defaults to
  628 | 		 * {@code chatResponse -> chatResponse != null && chatResponse.hasToolCalls()}.
  629 | 		 * Override to apply provider-specific stop-reason logic.
  630 | 		 * @param toolExecutionEligibilityChecker the checker
  631 | 		 * @return this Builder instance for method chaining
  632 | 		 */
  633 | 		public T toolExecutionEligibilityChecker(ToolExecutionEligibilityChecker toolExecutionEligibilityChecker) {
  634 | 			this.toolExecutionEligibilityChecker = toolExecutionEligibilityChecker;
  635 | 			return self();
  636 | 		}
  637 | 
  638 | 		/**
  639 | 		 * Sets the order of the advisor in the advisor chain.
  640 | 		 * @param advisorOrder the order value, must be between HIGHEST_PRECEDENCE and
  641 | 		 * LOWEST_PRECEDENCE
  642 | 		 * @return this Builder instance for method chaining
  643 | 		 */
  644 | 		public T advisorOrder(int advisorOrder) {
  645 | 			this.advisorOrder = advisorOrder;
  646 | 			return self();
  647 | 		}
  648 | 
  649 | 		/**
  650 | 		 * Sets whether internal conversation history is enabled. If false, you need a
  651 | 		 * memory advisor, a {@code MemoryAdvisor}, registered next in the chain.
  652 | 		 * @param conversationHistoryEnabled true to enable, false to disable
  653 | 		 * @return this Builder instance for method chaining
  654 | 		 */
  655 | 		public T conversationHistoryEnabled(boolean conversationHistoryEnabled) {
  656 | 			this.conversationHistoryEnabled = conversationHistoryEnabled;
  657 | 			return self();
  658 | 		}
  659 | 
  660 | 		/**
  661 | 		 * Disables internal conversation history. You need a memory advisor, a
  662 | 		 * {@code MemoryAdvisor}, registered next in the chain.
  663 | 		 * @return this Builder instance for method chaining
  664 | 		 */
  665 | 		public T disableInternalConversationHistory() {
  666 | 			this.conversationHistoryEnabled = false;
  667 | 			return self();
  668 | 		}
  669 | 
  670 | 		/**
  671 | 		 * Returns the configured ToolCallingManager.
  672 | 		 * @return the ToolCallingManager instance
  673 | 		 */
  674 | 		protected ToolCallingManager getToolCallingManager() {
  675 | 			return this.toolCallingManager;
  676 | 		}
  677 | 
  678 | 		/**
  679 | 		 * Returns the configured advisor order.
  680 | 		 * @return the advisor order value
  681 | 		 */
  682 | 		public int getAdvisorOrder() {
  683 | 			return this.advisorOrder;
  684 | 		}
  685 | 
  686 | 		/**
  687 | 		 * Returns the configured ToolExecutionEligibilityChecker.
  688 | 		 * @return the ToolExecutionEligibilityChecker instance
  689 | 		 */
  690 | 		public ToolExecutionEligibilityChecker getToolExecutionEligibilityChecker() {
  691 | 			return this.toolExecutionEligibilityChecker;
  692 | 		}
  693 | 
  694 | 		/**
  695 | 		 * Creates a shallow copy of this builder with all current settings. The copy can
  696 | 		 * be used as a template from which per-call overrides (e.g.
  697 | 		 * {@code conversationHistoryEnabled}) are applied without mutating the original.
  698 | 		 * <p>
  699 | 		 * Subclasses must override {@link #newCopy()} to return an instance of their own
  700 | 		 * type, and override this method to copy their additional fields into it.
  701 | 		 * @return a new {@link Builder} with the same configuration
  702 | 		 */
  703 | 		public Builder<?> copy() {
  704 | 			Builder<?> copy = newCopy();
  705 | 			copy.toolCallingManager = this.toolCallingManager;
  706 | 			copy.toolExecutionEligibilityChecker = this.toolExecutionEligibilityChecker;
  707 | 			copy.advisorOrder = this.advisorOrder;
  708 | 			copy.conversationHistoryEnabled = this.conversationHistoryEnabled;
  709 | 			return copy;
  710 | 		}
  711 | 
  712 | 		/**
  713 | 		 * Factory method called by {@link #copy()} to create the blank builder instance
  714 | 		 * that will receive the copied fields. Subclasses must override this to return an
  715 | 		 * instance of their own builder type so that {@link #copy()} preserves the full
  716 | 		 * subtype.
  717 | 		 * @return a new, unconfigured builder of the same concrete type
  718 | 		 */
  719 | 		protected Builder<?> newCopy() {
  720 | 			return new Builder<>();
  721 | 		}
  722 | 
  723 | 		/**
  724 | 		 * Returns whether internal conversation history is enabled.
  725 | 		 * @return true if internal conversation history is enabled, false if disabled
  726 | 		 */
  727 | 		protected boolean isConversationHistoryEnabled() {
  728 | 			return this.conversationHistoryEnabled;
  729 | 		}
  730 | 
  731 | 		/**
  732 | 		 * Builds and returns a new ToolCallingAdvisor instance with the configured
  733 | 		 * properties.
  734 | 		 * @return a new ToolCallingAdvisor instance
  735 | 		 * @throws IllegalArgumentException if toolCallingManager is null or advisorOrder
  736 | 		 * is out of valid range
  737 | 		 */
  738 | 		public ToolCallingAdvisor build() {
  739 | 			return new ToolCallingAdvisor(this.toolCallingManager, this.toolExecutionEligibilityChecker,
  740 | 					this.advisorOrder, this.conversationHistoryEnabled);
  741 | 		}
  742 | 
  743 | 	}
  744 | 
  745 | }
```
