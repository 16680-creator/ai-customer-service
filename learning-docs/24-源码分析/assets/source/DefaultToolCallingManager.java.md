<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/DefaultToolCallingManager.java -->
<!-- 正文引用：[[源码:DefaultToolCallingManager.java#L起-L止]] ；代码块头部：(src: DefaultToolCallingManager.java#L起-L止) -->

# DefaultToolCallingManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/DefaultToolCallingManager.java` |
| 行数 | 668 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.968Z |

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
   20 | import java.util.HashMap;
   21 | import java.util.HashSet;
   22 | import java.util.LinkedHashSet;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Objects;
   26 | import java.util.Optional;
   27 | import java.util.Set;
   28 | 
   29 | import io.micrometer.observation.Observation;
   30 | import io.micrometer.observation.ObservationRegistry;
   31 | import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
   32 | import org.apache.commons.logging.Log;
   33 | import org.apache.commons.logging.LogFactory;
   34 | import org.jspecify.annotations.Nullable;
   35 | 
   36 | import org.springframework.ai.chat.messages.AssistantMessage;
   37 | import org.springframework.ai.chat.messages.Message;
   38 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   39 | import org.springframework.ai.chat.model.ChatResponse;
   40 | import org.springframework.ai.chat.model.Generation;
   41 | import org.springframework.ai.chat.model.ToolContext;
   42 | import org.springframework.ai.chat.prompt.Prompt;
   43 | import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
   44 | import org.springframework.ai.tool.ToolCallback;
   45 | import org.springframework.ai.tool.definition.ToolDefinition;
   46 | import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
   47 | import org.springframework.ai.tool.execution.ToolExecutionException;
   48 | import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
   49 | import org.springframework.ai.tool.metadata.ToolMetadata;
   50 | import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
   51 | import org.springframework.ai.tool.observation.ToolCallingObservationContext;
   52 | import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
   53 | import org.springframework.ai.tool.observation.ToolCallingObservationDocumentation;
   54 | import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
   55 | import org.springframework.ai.tool.resolution.ToolCallbackResolver;
   56 | import org.springframework.util.Assert;
   57 | import org.springframework.util.CollectionUtils;
   58 | import org.springframework.util.StringUtils;
   59 | 
   60 | /**
   61 |  * Default implementation of {@link ToolCallingManager}.
   62 |  *
   63 |  * @author Thomas Vitale
   64 |  * @author chabinhwang
   65 |  * @author Christian Tzolov
   66 |  * @author Dimitar Proynov
   67 |  * @since 1.0.0
   68 |  */
   69 | public final class DefaultToolCallingManager implements ToolCallingManager {
   70 | 
   71 | 	private static final Log logger = LogFactory.getLog(DefaultToolCallingManager.class);
   72 | 
   73 | 	// @formatter:off
   74 | 
   75 | 	private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY
   76 | 			= ObservationRegistry.NOOP;
   77 | 
   78 | 	private static final ToolCallingObservationConvention DEFAULT_OBSERVATION_CONVENTION
   79 | 			= new DefaultToolCallingObservationConvention();
   80 | 
   81 | 	private static final ToolCallbackResolver DEFAULT_TOOL_CALLBACK_RESOLVER
   82 | 			= new DelegatingToolCallbackResolver(List.of());
   83 | 
   84 | 	private static final ToolExecutionExceptionProcessor DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR
   85 | 			= DefaultToolExecutionExceptionProcessor.builder().build();
   86 | 
   87 | 	private static final boolean DEFAULT_RESOLUTION_FALLBACK_ENABLED = false;
   88 | 
   89 | 	private static final String POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_START = "LLM may have adapted the tool name '";
   90 | 	private static final String POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_END
   91 | 			= "', especially if the name was truncated due to length limits. If this is the case, you can customize the prefixing and processing logic using McpToolNamePrefixGenerator";
   92 | 
   93 | 	// @formatter:on
   94 | 
   95 | 	/**
   96 | 	 * Default cap applied to every tool name unless overridden via
   97 | 	 * {@link Builder#maxCallsPerTool(String, int)}, exempted via
   98 | 	 * {@link Builder#excludeToolFromLimit(String)}, or disabled entirely via
   99 | 	 * {@link Builder#unlimitedCallsPerTool()}. Generous enough for legitimate iterative
  100 | 	 * tool use (pagination, retries) while still catching a runaway loop long before it
  101 | 	 * becomes expensive.
  102 | 	 */
  103 | 	public static final int DEFAULT_MAX_CALLS_PER_TOOL = 40;
  104 | 
  105 | 	/**
  106 | 	 * Default cap on the total number of tool calls, across all tools combined, allowed
  107 | 	 * within a turn, unless disabled entirely via
  108 | 	 * {@link Builder#unlimitedTotalToolCalls()}.
  109 | 	 */
  110 | 	public static final int DEFAULT_MAX_TOTAL_TOOL_CALLS = 150;
  111 | 
  112 | 	private final ObservationRegistry observationRegistry;
  113 | 
  114 | 	private final ToolCallbackResolver toolCallbackResolver;
  115 | 
  116 | 	private final ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;
  117 | 
  118 | 	private final ToolCallLimits toolCallLimits;
  119 | 
  120 | 	/**
  121 | 	 * Whether a call to a tool that is not part of the request's tool callbacks may still
  122 | 	 * be executed by resolving it through the {@link ToolCallbackResolver}. When
  123 | 	 * {@code false} (the default), the tools attached to the request are the only tools
  124 | 	 * that can be executed.
  125 | 	 */
  126 | 	private final boolean resolutionFallbackEnabled;
  127 | 
  128 | 	/** Which tool calls need a person's confirmation before they run. */
  129 | 	private final ToolCallConfirmationPolicy toolCallConfirmationPolicy;
  130 | 
  131 | 	private ToolCallingObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;
  132 | 
  133 | 	public DefaultToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
  134 | 			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
  135 | 		this(observationRegistry, toolCallbackResolver, toolExecutionExceptionProcessor,
  136 | 				new ToolCallLimits(DEFAULT_MAX_CALLS_PER_TOOL, Map.of(), Set.of(), DEFAULT_MAX_TOTAL_TOOL_CALLS,
  137 | 						ToolCallLimitBehavior.THROW),
  138 | 				DEFAULT_RESOLUTION_FALLBACK_ENABLED);
  139 | 	}
  140 | 
  141 | 	DefaultToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
  142 | 			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor, ToolCallLimits toolCallLimits) {
  143 | 		this(observationRegistry, toolCallbackResolver, toolExecutionExceptionProcessor, toolCallLimits,
  144 | 				DEFAULT_RESOLUTION_FALLBACK_ENABLED);
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Create a {@link DefaultToolCallingManager}.
  149 | 	 * @param observationRegistry the observation registry
  150 | 	 * @param toolCallbackResolver the resolver consulted for name-based tool resolution
  151 | 	 * @param toolExecutionExceptionProcessor the processor for tool execution exceptions
  152 | 	 * @param toolCallLimits the limits applied to tool calls within a turn
  153 | 	 * @param resolutionFallbackEnabled whether tools that are not part of the request's
  154 | 	 * tool callbacks may be resolved and executed via the {@code toolCallbackResolver};
  155 | 	 * defaults to {@code false} so that the request's tool callbacks bound execution
  156 | 	 * @since 2.0.1
  157 | 	 */
  158 | 	DefaultToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
  159 | 			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor, ToolCallLimits toolCallLimits,
  160 | 			boolean resolutionFallbackEnabled) {
  161 | 		this(observationRegistry, toolCallbackResolver, toolExecutionExceptionProcessor, toolCallLimits,
  162 | 				resolutionFallbackEnabled, ToolCallConfirmationPolicy.fromToolMetadata());
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * Create a {@link DefaultToolCallingManager}.
  167 | 	 * @param observationRegistry the observation registry
  168 | 	 * @param toolCallbackResolver the resolver consulted for name-based tool resolution
  169 | 	 * @param toolExecutionExceptionProcessor the processor for tool execution exceptions
  170 | 	 * @param toolCallLimits the limits applied to tool calls within a turn
  171 | 	 * @param resolutionFallbackEnabled whether tools that are not part of the request's
  172 | 	 * tool callbacks may be resolved and executed via the {@code toolCallbackResolver}
  173 | 	 * @param toolCallConfirmationPolicy which tool calls need a person's confirmation
  174 | 	 * before they run; see {@link ToolCallDecisions}
  175 | 	 * @since 2.1.0
  176 | 	 */
  177 | 	DefaultToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
  178 | 			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor, ToolCallLimits toolCallLimits,
  179 | 			boolean resolutionFallbackEnabled, ToolCallConfirmationPolicy toolCallConfirmationPolicy) {
  180 | 		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
  181 | 		Assert.notNull(toolCallbackResolver, "toolCallbackResolver cannot be null");
  182 | 		Assert.notNull(toolExecutionExceptionProcessor, "toolCallExceptionConverter cannot be null");
  183 | 		Assert.notNull(toolCallLimits, "toolCallLimits cannot be null");
  184 | 		Assert.notNull(toolCallConfirmationPolicy, "toolCallConfirmationPolicy cannot be null");
  185 | 
  186 | 		this.observationRegistry = observationRegistry;
  187 | 		this.toolCallbackResolver = toolCallbackResolver;
  188 | 		this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
  189 | 		this.toolCallLimits = toolCallLimits;
  190 | 		this.resolutionFallbackEnabled = resolutionFallbackEnabled;
  191 | 		this.toolCallConfirmationPolicy = toolCallConfirmationPolicy;
  192 | 	}
  193 | 
  194 | 	@Override
  195 | 	public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
  196 | 		Assert.notNull(chatOptions, "chatOptions cannot be null");
  197 | 
  198 | 		List<ToolCallback> toolCallbacks = new ArrayList<>(
  199 | 				!CollectionUtils.isEmpty(chatOptions.getToolCallbacks()) ? chatOptions.getToolCallbacks() : List.of());
  200 | 
  201 | 		return toolCallbacks.stream().map(ToolCallback::getToolDefinition).toList();
  202 | 	}
  203 | 
  204 | 	@Override
  205 | 	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
  206 | 		return this.doExecuteToolCalls(prompt, chatResponse, null);
  207 | 	}
  208 | 
  209 | 	@Override
  210 | 	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse, ToolCallDecisions decisions) {
  211 | 		Assert.notNull(decisions, "decisions cannot be null");
  212 | 		return this.doExecuteToolCalls(prompt, chatResponse, decisions);
  213 | 	}
  214 | 
  215 | 	private ToolExecutionResult doExecuteToolCalls(Prompt prompt, ChatResponse chatResponse,
  216 | 			@Nullable ToolCallDecisions decisions) {
  217 | 		Assert.notNull(prompt, "prompt cannot be null");
  218 | 		Assert.notNull(chatResponse, "chatResponse cannot be null");
  219 | 
  220 | 		Optional<Generation> toolCallGeneration = chatResponse.getResults()
  221 | 			.stream()
  222 | 			.filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
  223 | 			.findFirst();
  224 | 
  225 | 		if (toolCallGeneration.isEmpty()) {
  226 | 			throw new IllegalStateException("No tool call requested by the chat model");
  227 | 		}
  228 | 
  229 | 		AssistantMessage assistantMessage = toolCallGeneration.get().getOutput();
  230 | 
  231 | 		ToolContext toolContext = buildToolContext(prompt, assistantMessage);
  232 | 
  233 | 		InternalToolExecutionResult internalToolExecutionResult = executeToolCall(prompt, assistantMessage, toolContext,
  234 | 				decisions);
  235 | 
  236 | 		// A resumed batch may come with the conversation up to and including the reply;
  237 | 		// the history then holds the reply once
  238 | 		List<Message> previousMessages = prompt.getInstructions();
  239 | 		if (endsWith(previousMessages, assistantMessage)) {
  240 | 			previousMessages = previousMessages.subList(0, previousMessages.size() - 1);
  241 | 		}
  242 | 		List<Message> conversationHistory = buildConversationHistoryAfterToolExecution(previousMessages,
  243 | 				assistantMessage, internalToolExecutionResult.toolResponseMessage());
  244 | 
  245 | 		return ToolExecutionResult.builder()
  246 | 			.conversationHistory(conversationHistory)
  247 | 			.returnDirect(internalToolExecutionResult.returnDirect())
  248 | 			.build();
  249 | 	}
  250 | 
  251 | 	private static ToolContext buildToolContext(Prompt prompt, AssistantMessage assistantMessage) {
  252 | 		Map<String, Object> toolContextMap = Map.of();
  253 | 
  254 | 		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
  255 | 				&& !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {
  256 | 			toolContextMap = new HashMap<>(toolCallingChatOptions.getToolContext());
  257 | 		}
  258 | 
  259 | 		return new ToolContext(toolContextMap);
  260 | 	}
  261 | 
  262 | 	/**
  263 | 	 * Whether the messages end with a reply holding the same tool calls as the message.
  264 | 	 */
  265 | 	private static boolean endsWith(List<Message> messages, AssistantMessage assistantMessage) {
  266 | 		return !messages.isEmpty() && messages.get(messages.size() - 1) instanceof AssistantMessage last
  267 | 				&& last.getToolCalls().equals(assistantMessage.getToolCalls());
  268 | 	}
  269 | 
  270 | 	/**
  271 | 	 * Execute the tool call and return the response message.
  272 | 	 */
  273 | 	private InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
  274 | 			ToolContext toolContext, @Nullable ToolCallDecisions decisions) {
  275 | 		List<ToolCallback> toolCallbacks = List.of();
  276 | 		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
  277 | 			if (!CollectionUtils.isEmpty(toolCallingChatOptions.getToolCallbacks())) {
  278 | 				toolCallbacks = toolCallingChatOptions.getToolCallbacks();
  279 | 			}
  280 | 		}
  281 | 
  282 | 		// Before any call runs: a call that needs a person's confirmation pauses the
  283 | 		// whole batch, unless the decisions cover it
  284 | 		// Decisions name the calls by key: the id, or the position when there is none
  285 | 		List<String> toolCallKeys = ToolCallDecisions.toolCallKeys(assistantMessage);
  286 | 		if (decisions == null) {
  287 | 			List<String> awaiting = this.getAwaitingConfirmationIds(assistantMessage, toolCallKeys, toolCallbacks);
  288 | 			if (!awaiting.isEmpty()) {
  289 | 				throw new ToolCallConfirmationRequiredException(awaiting);
  290 | 			}
  291 | 		}
  292 | 		else {
  293 | 			checkDecisions(decisions, assistantMessage, toolCallKeys, toolCallbacks);
  294 | 		}
  295 | 
  296 | 		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
  297 | 
  298 | 		Boolean returnDirect = null;
  299 | 		boolean denied = false;
  300 | 
  301 | 		Map<String, Integer> toolCallCounts = ToolCallLimits.countPriorToolCalls(prompt.getInstructions());
  302 | 		int totalToolCallCount = toolCallCounts.values().stream().mapToInt(Integer::intValue).sum();
  303 | 
  304 | 		List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
  305 | 		for (int i = 0; i < toolCalls.size(); i++) {
  306 | 
  307 | 			AssistantMessage.ToolCall toolCall = toolCalls.get(i);
  308 | 			String toolName = toolCall.name();
  309 | 
  310 | 			ToolCallDecision decision = (decisions != null) ? decisions.decisionOn(toolCallKeys.get(i)) : null;
  311 | 			if (decision != null && !decision.allowed()) {
  312 | 				// A denied call does not run; the model receives the decision's message
  313 | 				toolResponses
  314 | 					.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, decision.denyMessage()));
  315 | 				denied = true;
  316 | 				continue;
  317 | 			}
  318 | 
  319 | 			if (logger.isDebugEnabled()) {
  320 | 				logger.debug("Executing tool call: " + toolCall.name());
  321 | 			}
  322 | 
  323 | 			totalToolCallCount++;
  324 | 			int toolCallCount = toolCallCounts.merge(toolName, 1, Integer::sum);
  325 | 
  326 | 			ToolCallLimits.Breach limitBreach = this.toolCallLimits.check(toolName, toolCallCount, totalToolCallCount);
  327 | 			if (limitBreach != null) {
  328 | 				toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, limitBreach.message()));
  329 | 
  330 | 				if (this.toolCallLimits.onLimitExceeded() == ToolCallLimitBehavior.THROW) {
  331 | 					ToolResponseMessage partialToolResponseMessage = ToolResponseMessage.builder()
  332 | 						.responses(toolResponses)
  333 | 						.build();
  334 | 					List<Message> partialConversationHistory = buildConversationHistoryAfterToolExecution(
  335 | 							prompt.getInstructions(), assistantMessage, partialToolResponseMessage);
  336 | 					ToolExecutionResult partialToolExecutionResult = ToolExecutionResult.builder()
  337 | 						.conversationHistory(partialConversationHistory)
  338 | 						.returnDirect(Objects.requireNonNullElse(returnDirect, false))
  339 | 						.build();
  340 | 					throw new ToolCallLimitExceededException(limitBreach.toolName(), limitBreach.limit(),
  341 | 							partialToolExecutionResult);
  342 | 				}
  343 | 
  344 | 				// ToolCallLimitBehavior.RETURN_ERROR_RESPONSE: skip invoking this tool
  345 | 				// call but keep processing the rest of the batch.
  346 | 				continue;
  347 | 			}
  348 | 
  349 | 			String toolInputArguments = toolCall.arguments();
  350 | 
  351 | 			// Handle the possible null parameter situation in streaming mode.
  352 | 			final String finalToolInputArguments;
  353 | 			if (!StringUtils.hasText(toolInputArguments)) {
  354 | 				if (logger.isWarnEnabled()) {
  355 | 					logger.warn("Tool call arguments are null or empty for tool: " + toolName
  356 | 							+ ". Using empty JSON object as default.");
  357 | 				}
  358 | 				finalToolInputArguments = "{}";
  359 | 			}
  360 | 			else {
  361 | 				finalToolInputArguments = toolInputArguments;
  362 | 			}
  363 | 
  364 | 			ToolCallback toolCallback = this.resolveToolCallback(toolCallbacks, toolName);
  365 | 
  366 | 			if (toolCallback == null) {
  367 | 				if (logger.isWarnEnabled()) {
  368 | 					logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_START + toolName
  369 | 							+ POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING_END);
  370 | 				}
  371 | 				throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
  372 | 			}
  373 | 
  374 | 			if (returnDirect == null) {
  375 | 				returnDirect = toolCallback.getToolMetadata().returnDirect();
  376 | 			}
  377 | 			else {
  378 | 				returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
  379 | 			}
  380 | 
  381 | 			// In streaming/reactive mode the parent observation is propagated through the
  382 | 			// Reactor context captured in ToolCallReactiveContextHolder. In blocking mode
  383 | 			// that holder is never populated, so fall back to the observation currently
  384 | 			// in scope on the calling thread to keep the observation hierarchy intact.
  385 | 			Observation parent = ToolCallReactiveContextHolder.getContext()
  386 | 				.getOrDefault(ObservationThreadLocalAccessor.KEY, this.observationRegistry.getCurrentObservation());
  387 | 
  388 | 			ToolCallingObservationContext observationContext = ToolCallingObservationContext.builder()
  389 | 				.toolDefinition(toolCallback.getToolDefinition())
  390 | 				.toolMetadata(toolCallback.getToolMetadata())
  391 | 				.toolCallId(toolCall.id())
  392 | 				.toolType(toolCall.type())
  393 | 				.toolCallArguments(finalToolInputArguments)
  394 | 				.build();
  395 | 
  396 | 			String toolCallResult = ToolCallingObservationDocumentation.TOOL_CALL
  397 | 				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
  398 | 						this.observationRegistry)
  399 | 				.parentObservation(parent)
  400 | 				.observe(() -> {
  401 | 					String toolResult;
  402 | 					try {
  403 | 						toolResult = toolCallback.call(finalToolInputArguments, toolContext);
  404 | 					}
  405 | 					catch (ToolExecutionException ex) {
  406 | 						toolResult = this.toolExecutionExceptionProcessor.process(ex);
  407 | 					}
  408 | 					observationContext.setToolCallResult(toolResult);
  409 | 					return toolResult;
  410 | 				});
  411 | 
  412 | 			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName,
  413 | 					toolCallResult != null ? toolCallResult : ""));
  414 | 		}
  415 | 
  416 | 		// Straight back to the caller only when every call ran and asked for it
  417 | 		return new InternalToolExecutionResult(ToolResponseMessage.builder().responses(toolResponses).build(),
  418 | 				Objects.requireNonNullElse(returnDirect, false) && !denied);
  419 | 	}
  420 | 
  421 | 	/**
  422 | 	 * The keys of the calls that need a person's confirmation before they run, in the
  423 | 	 * order the model asked for them. The policy sees the metadata of the tool the call
  424 | 	 * would run with, or {@code null} when the manager has none for it.
  425 | 	 */
  426 | 	private List<String> getAwaitingConfirmationIds(AssistantMessage assistantMessage, List<String> toolCallKeys,
  427 | 			List<ToolCallback> toolCallbacks) {
  428 | 		List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
  429 | 		List<String> awaiting = new ArrayList<>();
  430 | 		for (int i = 0; i < toolCalls.size(); i++) {
  431 | 			AssistantMessage.ToolCall toolCall = toolCalls.get(i);
  432 | 			ToolCallback toolCallback = this.resolveToolCallback(toolCallbacks, toolCall.name());
  433 | 			ToolMetadata toolMetadata = (toolCallback != null) ? toolCallback.getToolMetadata() : null;
  434 | 			if (this.toolCallConfirmationPolicy.requiresConfirmation(toolCall, toolMetadata)) {
  435 | 				awaiting.add(toolCallKeys.get(i));
  436 | 			}
  437 | 		}
  438 | 		return awaiting;
  439 | 	}
  440 | 
  441 | 	/**
  442 | 	 * Every call of the reply needs a decision, so the policy is never re-evaluated when
  443 | 	 * a paused batch resumes, and an allowed call runs only with a tool the prompt
  444 | 	 * carries, never with one resolved by name.
  445 | 	 */
  446 | 	private static void checkDecisions(ToolCallDecisions decisions, AssistantMessage assistantMessage,
  447 | 			List<String> toolCallKeys, List<ToolCallback> toolCallbacks) {
  448 | 		Set<String> assistantToolCallIds = new LinkedHashSet<>(toolCallKeys);
  449 | 		for (ToolCallDecision decision : decisions.decisions()) {
  450 | 			Assert.isTrue(assistantToolCallIds.contains(decision.toolCallId()), () -> "No tool call has the key '"
  451 | 					+ decision.toolCallId() + "'; the reply's tool call keys: " + assistantToolCallIds);
  452 | 		}
  453 | 		Set<String> missing = new LinkedHashSet<>(assistantToolCallIds);
  454 | 		decisions.decisions().forEach(decision -> missing.remove(decision.toolCallId()));
  455 | 		Assert.isTrue(missing.isEmpty(), () -> "Every tool call of the reply needs a decision; missing: " + missing);
  456 | 		List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
  457 | 		for (int i = 0; i < toolCalls.size(); i++) {
  458 | 			AssistantMessage.ToolCall toolCall = toolCalls.get(i);
  459 | 			ToolCallDecision decision = decisions.decisionOn(toolCallKeys.get(i));
  460 | 			if (decision != null && decision.allowed() && findToolCallback(toolCallbacks, toolCall.name()) == null) {
  461 | 				throw new IllegalArgumentException(
  462 | 						"The prompt does not carry the tool '" + toolCall.name() + "' of the allowed call '"
  463 | 								+ toolCallKeys.get(i) + "'; an allowed call runs only with a tool of the prompt");
  464 | 			}
  465 | 		}
  466 | 	}
  467 | 
  468 | 	/**
  469 | 	 * The prompt's callback for the tool or, when the fallback is enabled, the
  470 | 	 * resolver's.
  471 | 	 */
  472 | 	private @Nullable ToolCallback resolveToolCallback(List<ToolCallback> toolCallbacks, String toolName) {
  473 | 		ToolCallback toolCallback = findToolCallback(toolCallbacks, toolName);
  474 | 		if (toolCallback == null && this.resolutionFallbackEnabled) {
  475 | 			return this.toolCallbackResolver.resolve(toolName);
  476 | 		}
  477 | 		return toolCallback;
  478 | 	}
  479 | 
  480 | 	private static @Nullable ToolCallback findToolCallback(List<ToolCallback> toolCallbacks, String toolName) {
  481 | 		return toolCallbacks.stream()
  482 | 			.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
  483 | 			.findFirst()
  484 | 			.orElse(null);
  485 | 	}
  486 | 
  487 | 	private List<Message> buildConversationHistoryAfterToolExecution(List<Message> previousMessages,
  488 | 			AssistantMessage assistantMessage, ToolResponseMessage toolResponseMessage) {
  489 | 		List<Message> messages = new ArrayList<>(previousMessages);
  490 | 		messages.add(assistantMessage);
  491 | 		messages.add(toolResponseMessage);
  492 | 		return messages;
  493 | 	}
  494 | 
  495 | 	public void setObservationConvention(ToolCallingObservationConvention observationConvention) {
  496 | 		this.observationConvention = observationConvention;
  497 | 	}
  498 | 
  499 | 	public static Builder builder() {
  500 | 		return new Builder();
  501 | 	}
  502 | 
  503 | 	private record InternalToolExecutionResult(ToolResponseMessage toolResponseMessage, boolean returnDirect) {
  504 | 	}
  505 | 
  506 | 	public final static class Builder {
  507 | 
  508 | 		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;
  509 | 
  510 | 		private ToolCallbackResolver toolCallbackResolver = DEFAULT_TOOL_CALLBACK_RESOLVER;
  511 | 
  512 | 		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor = DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR;
  513 | 
  514 | 		private int defaultMaxCallsPerTool = DEFAULT_MAX_CALLS_PER_TOOL;
  515 | 
  516 | 		private final Map<String, Integer> maxCallsPerTool = new HashMap<>();
  517 | 
  518 | 		private final Set<String> toolsExcludedFromLimit = new HashSet<>();
  519 | 
  520 | 		private int maxTotalToolCalls = DEFAULT_MAX_TOTAL_TOOL_CALLS;
  521 | 
  522 | 		private ToolCallLimitBehavior onLimitExceeded = ToolCallLimitBehavior.THROW;
  523 | 
  524 | 		private boolean resolutionFallbackEnabled = DEFAULT_RESOLUTION_FALLBACK_ENABLED;
  525 | 
  526 | 		private ToolCallConfirmationPolicy toolCallConfirmationPolicy = ToolCallConfirmationPolicy.fromToolMetadata();
  527 | 
  528 | 		private Builder() {
  529 | 		}
  530 | 
  531 | 		public Builder observationRegistry(ObservationRegistry observationRegistry) {
  532 | 			this.observationRegistry = observationRegistry;
  533 | 			return this;
  534 | 		}
  535 | 
  536 | 		public Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
  537 | 			this.toolCallbackResolver = toolCallbackResolver;
  538 | 			return this;
  539 | 		}
  540 | 
  541 | 		public Builder toolExecutionExceptionProcessor(
  542 | 				ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
  543 | 			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
  544 | 			return this;
  545 | 		}
  546 | 
  547 | 		/**
  548 | 		 * Default maximum number of times any single tool can be called within a turn,
  549 | 		 * unless overridden for a specific tool name via
  550 | 		 * {@link #maxCallsPerTool(String, int)} or exempted via
  551 | 		 * {@link #excludeToolFromLimit(String)}. Defaults to
  552 | 		 * {@link DefaultToolCallingManager#DEFAULT_MAX_CALLS_PER_TOOL}; call
  553 | 		 * {@link #unlimitedCallsPerTool()} to disable this limit entirely.
  554 | 		 */
  555 | 		public Builder maxCallsPerTool(int maxCallsPerTool) {
  556 | 			Assert.isTrue(maxCallsPerTool > 0, "maxCallsPerTool must be greater than 0");
  557 | 			this.defaultMaxCallsPerTool = maxCallsPerTool;
  558 | 			return this;
  559 | 		}
  560 | 
  561 | 		/**
  562 | 		 * Disable the per-tool call limit entirely, undoing {@link #maxCallsPerTool(int)}
  563 | 		 * (or the {@link DefaultToolCallingManager#DEFAULT_MAX_CALLS_PER_TOOL} default),
  564 | 		 * so that, absent a specific {@link #maxCallsPerTool(String, int)} override, any
  565 | 		 * tool can be called an unlimited number of times.
  566 | 		 */
  567 | 		public Builder unlimitedCallsPerTool() {
  568 | 			this.defaultMaxCallsPerTool = ToolCallLimits.NO_LIMIT;
  569 | 			return this;
  570 | 		}
  571 | 
  572 | 		/**
  573 | 		 * Maximum number of times the named tool can be called within a turn, overriding
  574 | 		 * the default set via {@link #maxCallsPerTool(int)} for that tool only.
  575 | 		 */
  576 | 		public Builder maxCallsPerTool(String toolName, int maxCallsPerTool) {
  577 | 			Assert.hasText(toolName, "toolName cannot be null or empty");
  578 | 			Assert.isTrue(maxCallsPerTool > 0, "maxCallsPerTool must be greater than 0");
  579 | 			this.maxCallsPerTool.put(toolName, maxCallsPerTool);
  580 | 			this.toolsExcludedFromLimit.remove(toolName);
  581 | 			return this;
  582 | 		}
  583 | 
  584 | 		/**
  585 | 		 * Exempt the named tool from the per-tool call limit, so it can be called an
  586 | 		 * unlimited number of times regardless of {@link #maxCallsPerTool(int)} or a
  587 | 		 * prior {@link #maxCallsPerTool(String, int)} override for that tool. Calls to
  588 | 		 * this tool still count toward {@link #maxTotalToolCalls(int)}.
  589 | 		 */
  590 | 		public Builder excludeToolFromLimit(String toolName) {
  591 | 			Assert.hasText(toolName, "toolName cannot be null or empty");
  592 | 			this.toolsExcludedFromLimit.add(toolName);
  593 | 			this.maxCallsPerTool.remove(toolName);
  594 | 			return this;
  595 | 		}
  596 | 
  597 | 		/**
  598 | 		 * Maximum number of tool calls, across all tools combined, allowed within a turn.
  599 | 		 * Defaults to {@link DefaultToolCallingManager#DEFAULT_MAX_TOTAL_TOOL_CALLS};
  600 | 		 * call {@link #unlimitedTotalToolCalls()} to disable this limit entirely.
  601 | 		 */
  602 | 		public Builder maxTotalToolCalls(int maxTotalToolCalls) {
  603 | 			Assert.isTrue(maxTotalToolCalls > 0, "maxTotalToolCalls must be greater than 0");
  604 | 			this.maxTotalToolCalls = maxTotalToolCalls;
  605 | 			return this;
  606 | 		}
  607 | 
  608 | 		/**
  609 | 		 * Disable the total tool call limit entirely, undoing
  610 | 		 * {@link #maxTotalToolCalls(int)} (or the
  611 | 		 * {@link DefaultToolCallingManager#DEFAULT_MAX_TOTAL_TOOL_CALLS} default).
  612 | 		 */
  613 | 		public Builder unlimitedTotalToolCalls() {
  614 | 			this.maxTotalToolCalls = ToolCallLimits.NO_LIMIT;
  615 | 			return this;
  616 | 		}
  617 | 
  618 | 		/**
  619 | 		 * What to do when a configured limit is exceeded. Defaults to
  620 | 		 * {@link ToolCallLimitBehavior#THROW}.
  621 | 		 */
  622 | 		public Builder onLimitExceeded(ToolCallLimitBehavior onLimitExceeded) {
  623 | 			Assert.notNull(onLimitExceeded, "onLimitExceeded cannot be null");
  624 | 			this.onLimitExceeded = onLimitExceeded;
  625 | 			return this;
  626 | 		}
  627 | 
  628 | 		/**
  629 | 		 * Whether a call to a tool that is not part of the request's tool callbacks may
  630 | 		 * still be executed by resolving it through the {@link ToolCallbackResolver}.
  631 | 		 * Defaults to {@code false}, so that the tools attached to a request bound what
  632 | 		 * can be executed for that request.
  633 | 		 * @param resolutionFallbackEnabled {@code true} to allow name-based resolution of
  634 | 		 * tools absent from the request via the resolver
  635 | 		 * @since 2.0.1
  636 | 		 */
  637 | 		public Builder resolutionFallbackEnabled(boolean resolutionFallbackEnabled) {
  638 | 			this.resolutionFallbackEnabled = resolutionFallbackEnabled;
  639 | 			return this;
  640 | 		}
  641 | 
  642 | 		/**
  643 | 		 * Which tool calls need a person's confirmation before they run. The manager
  644 | 		 * refuses to run such a call without a decision for it, by throwing
  645 | 		 * {@link ToolCallConfirmationRequiredException} before any call of the batch
  646 | 		 * runs; see {@link ToolCallDecisions}. Defaults to
  647 | 		 * {@link ToolCallConfirmationPolicy#fromToolMetadata()}: the calls of tools
  648 | 		 * marked {@code requiresConfirmation}.
  649 | 		 * @param toolCallConfirmationPolicy the policy
  650 | 		 * @since 2.1.0
  651 | 		 */
  652 | 		public Builder toolCallConfirmationPolicy(ToolCallConfirmationPolicy toolCallConfirmationPolicy) {
  653 | 			Assert.notNull(toolCallConfirmationPolicy, "toolCallConfirmationPolicy cannot be null");
  654 | 			this.toolCallConfirmationPolicy = toolCallConfirmationPolicy;
  655 | 			return this;
  656 | 		}
  657 | 
  658 | 		public DefaultToolCallingManager build() {
  659 | 			ToolCallLimits toolCallLimits = new ToolCallLimits(this.defaultMaxCallsPerTool, this.maxCallsPerTool,
  660 | 					this.toolsExcludedFromLimit, this.maxTotalToolCalls, this.onLimitExceeded);
  661 | 			return new DefaultToolCallingManager(this.observationRegistry, this.toolCallbackResolver,
  662 | 					this.toolExecutionExceptionProcessor, toolCallLimits, this.resolutionFallbackEnabled,
  663 | 					this.toolCallConfirmationPolicy);
  664 | 		}
  665 | 
  666 | 	}
  667 | 
  668 | }
```
