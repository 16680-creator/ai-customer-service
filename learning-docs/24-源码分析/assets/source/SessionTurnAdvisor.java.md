<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session-advisor:2.1.0 | path: org/springframework/ai/session/advisor/SessionTurnAdvisor.java -->
<!-- 正文引用：[[源码:SessionTurnAdvisor.java#L起-L止]] ；代码块头部：(src: SessionTurnAdvisor.java#L起-L止) -->

# SessionTurnAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session-advisor:2.1.0` |
| 路径 | `org/springframework/ai/session/advisor/SessionTurnAdvisor.java` |
| 行数 | 563 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session-advisor/2.1.0/spring-ai-session-advisor-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T17:09:29.338Z |

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
   17 | package org.springframework.ai.session.advisor;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.HashMap;
   22 | import java.util.HashSet;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Objects;
   26 | import java.util.Set;
   27 | import java.util.concurrent.ConcurrentHashMap;
   28 | import java.util.concurrent.atomic.AtomicReference;
   29 | 
   30 | import io.micrometer.context.ContextSnapshot;
   31 | import io.micrometer.context.ContextSnapshotFactory;
   32 | import org.jspecify.annotations.Nullable;
   33 | import org.slf4j.Logger;
   34 | import org.slf4j.LoggerFactory;
   35 | import reactor.core.publisher.Flux;
   36 | import reactor.core.publisher.Mono;
   37 | import reactor.core.scheduler.Scheduler;
   38 | 
   39 | import org.springframework.ai.chat.client.ChatClientRequest;
   40 | import org.springframework.ai.chat.client.ChatClientResponse;
   41 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   42 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   43 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   44 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   45 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   46 | import org.springframework.ai.chat.messages.AssistantMessage;
   47 | import org.springframework.ai.chat.messages.MessageType;
   48 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   49 | import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
   50 | import org.springframework.ai.chat.model.ChatResponse;
   51 | import org.springframework.ai.chat.model.Generation;
   52 | import org.springframework.ai.chat.prompt.ChatOptions;
   53 | import org.springframework.ai.model.tool.ToolCallLimitExceededException;
   54 | import org.springframework.ai.model.tool.ToolCallingChatOptions;
   55 | import org.springframework.ai.model.tool.ToolExecutionResult;
   56 | import org.springframework.ai.session.EventFilter;
   57 | import org.springframework.ai.session.SessionEvent;
   58 | import org.springframework.ai.session.SessionService;
   59 | import org.springframework.ai.session.compaction.CompactionStrategy;
   60 | import org.springframework.ai.session.compaction.CompactionTrigger;
   61 | import org.springframework.core.Ordered;
   62 | import org.springframework.util.Assert;
   63 | 
   64 | /**
   65 |  * Runs each {@code ChatClient} call as one turn of the session. A turn is one user
   66 |  * message and everything up to the next: the tool rounds and the reply, the unit the chat
   67 |  * memory's turn-boundary eviction works with too. The advisor sits outside the
   68 |  * tool-calling loop (order {@link #DEFAULT_ORDER}), so it sees the request before the
   69 |  * first round and the turn's final response after the last one, however the loop ended;
   70 |  * what happens once per turn is its, what happens once per model round is the
   71 |  * {@link SessionMemoryAdvisor}'s, inside the loop.
   72 |  * <ol>
   73 |  * <li>Finds the session or creates it on first use, with the metadata from
   74 |  * {@link SessionMemoryAdvisor#SESSION_METADATA_CONTEXT_KEY}, such as the agent
   75 |  * attributes.</li>
   76 |  * <li>With {@link Builder#sessionIdInToolContext(boolean)}, puts the session id into the
   77 |  * tool context of the call, so that a tool that reads the session, the session recall
   78 |  * tools for example, finds it without the caller passing it a second time. Off by
   79 |  * default, because the tool context goes to every tool of the call.</li>
   80 |  * <li>Before a new turn, answers the tool calls the conversation ends with when they have
   81 |  * no results, with {@link #UNANSWERED_TOOL_RESULT}: a tool call that waited for a
   82 |  * person's confirmation and never got one, or a loop that failed after the reply was
   83 |  * stored. The log then keeps the call with a result, so the model sees what it asked for
   84 |  * and the memory advisor has nothing to drop from the prompt. A request that brings
   85 |  * results for those calls, as the resume of a confirmation does, is left alone. The
   86 |  * advisor cannot tell an abandoned call from one whose tool still runs in another call on
   87 |  * the same session, so a session takes one call at a time.</li>
   88 |  * <li>After a loop that ended on a {@code returnDirect} tool or a breached tool call
   89 |  * limit, stores the reply's tool results, which no model round stored.</li>
   90 |  * <li>Compacts the session once the call has returned, if a
   91 |  * {@link Builder#compactionTrigger trigger} fires: before the reply is returned, or after
   92 |  * it on the scheduler with {@link Builder#asyncCompaction(boolean)}. Compaction is
   93 |  * best-effort: a failure is logged and the reply returned, and it is retried after the
   94 |  * next turn. It appends a boundary with an optimistic compare-and-swap, so of two
   95 |  * concurrent compactions only the first lands; a compaction that a new turn's writes
   96 |  * overtake keeps them in the view and lands with the same summary.</li>
   97 |  * </ol>
   98 |  * On its own the advisor stores no conversation: it needs the memory advisor, and
   99 |  * {@link SessionAdvisors} builds the two together at their orders. The memory advisor
  100 |  * alone keeps the history, without compaction.
  101 |  *
  102 |  * @author Christian Tzolov
  103 |  * @since 2.1.0
  104 |  */
  105 | public final class SessionTurnAdvisor implements CallAdvisor, StreamAdvisor {
  106 | 
  107 | 	/**
  108 | 	 * Outside the tool-calling advisor ({@code HIGHEST_PRECEDENCE + 300}), so the loop
  109 | 	 * runs inside the turn.
  110 | 	 */
  111 | 	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
  112 | 
  113 | 	/** The result a tool call gets in the log when the loop ended before it ran. */
  114 | 	public static final String NOT_RUN_TOOL_RESULT = "The tool call did not run: the tool call limit was reached.";
  115 | 
  116 | 	/**
  117 | 	 * The result a tool call gets in the log when a new turn starts and the call still
  118 | 	 * has none: the turn that made it paused for a confirmation that never came, or
  119 | 	 * failed before its results were stored. It says what the log knows and no more: the
  120 | 	 * tool may have run.
  121 | 	 */
  122 | 	public static final String UNANSWERED_TOOL_RESULT = "No result was recorded for this tool call.";
  123 | 
  124 | 	/**
  125 | 	 * How often the version-checked write of {@link #UNANSWERED_TOOL_RESULT} is tried
  126 | 	 * when other appends, a compaction for example, keep moving the log.
  127 | 	 */
  128 | 	private static final int UNANSWERED_WRITE_ATTEMPTS = 3;
  129 | 
  130 | 	private static final Logger logger = LoggerFactory.getLogger(SessionTurnAdvisor.class);
  131 | 
  132 | 	/**
  133 | 	 * Carries the caller's context, such as the security context, to the session calls of
  134 | 	 * a stream and to an async compaction: the thread locals of a blocking caller, the
  135 | 	 * Reactor context of a reactive one, the latter winning where both hold a key.
  136 | 	 */
  137 | 	private static final ContextSnapshotFactory CONTEXT_SNAPSHOTS = ContextSnapshotFactory.builder().build();
  138 | 
  139 | 	private final SessionService sessionService;
  140 | 
  141 | 	private final int order;
  142 | 
  143 | 	private final Scheduler scheduler;
  144 | 
  145 | 	@Nullable private final CompactionTrigger compactionTrigger;
  146 | 
  147 | 	@Nullable private final CompactionStrategy compactionStrategy;
  148 | 
  149 | 	private final boolean asyncCompaction;
  150 | 
  151 | 	private final boolean sessionIdInToolContext;
  152 | 
  153 | 	/**
  154 | 	 * The sessions an asynchronous compaction of this advisor runs on, so it runs once
  155 | 	 * each.
  156 | 	 */
  157 | 	private final Set<String> compacting = ConcurrentHashMap.newKeySet();
  158 | 
  159 | 	private SessionTurnAdvisor(Builder builder) {
  160 | 		this.sessionService = builder.sessionService;
  161 | 		this.order = builder.order;
  162 | 		this.scheduler = builder.scheduler;
  163 | 		this.compactionTrigger = builder.compactionTrigger;
  164 | 		this.compactionStrategy = builder.compactionStrategy;
  165 | 		this.asyncCompaction = builder.asyncCompaction;
  166 | 		this.sessionIdInToolContext = builder.sessionIdInToolContext;
  167 | 	}
  168 | 
  169 | 	public static Builder builder(SessionService sessionService) {
  170 | 		return new Builder(sessionService);
  171 | 	}
  172 | 
  173 | 	@Override
  174 | 	public String getName() {
  175 | 		return getClass().getSimpleName();
  176 | 	}
  177 | 
  178 | 	@Override
  179 | 	public int getOrder() {
  180 | 		return this.order;
  181 | 	}
  182 | 
  183 | 	@Override
  184 | 	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
  185 | 		Assert.notNull(request, "request must not be null");
  186 | 		Assert.notNull(chain, "chain must not be null");
  187 | 		String sessionId = SessionContexts.sessionId(request.context());
  188 | 		this.sessionService.findOrCreate(SessionContexts.createRequest(sessionId, request.context()));
  189 | 		answerUnansweredCalls(sessionId, request);
  190 | 		ChatClientResponse response = chain.nextCall(withSessionIdInToolContext(request, sessionId));
  191 | 		complete(sessionId, response.chatResponse());
  192 | 		return response;
  193 | 	}
  194 | 
  195 | 	@Override
  196 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
  197 | 		Assert.notNull(request, "request must not be null");
  198 | 		Assert.notNull(chain, "chain must not be null");
  199 | 		String sessionId = SessionContexts.sessionId(request.context());
  200 | 		// The session calls run on the scheduler, never on the model's thread, with the
  201 | 		// caller's context (the security context, the current observation) restored
  202 | 		// around them and around the subscription of the rest of the chain: the thread
  203 | 		// locals of a blocking caller, and what a reactive caller carries in the Reactor
  204 | 		// context
  205 | 		return Flux.deferContextual(contextView -> {
  206 | 			ContextSnapshot context = CONTEXT_SNAPSHOTS.captureAll(contextView);
  207 | 			return Mono.just(request).publishOn(this.scheduler).flatMapMany(r -> {
  208 | 				// A returnDirect result or a breached limit ends the stream on a chunk
  209 | 				// of its own
  210 | 				AtomicReference<@Nullable ChatResponse> unanswered = new AtomicReference<>();
  211 | 				Flux<ChatClientResponse> rest = chain.nextStream(withSessionIdInToolContext(r, sessionId))
  212 | 					.doOnNext(response -> {
  213 | 						if (endsWithoutModel(response.chatResponse())) {
  214 | 							unanswered.set(response.chatResponse());
  215 | 						}
  216 | 					})
  217 | 					.publishOn(this.scheduler)
  218 | 					.doOnComplete(context.wrap(() -> complete(sessionId, unanswered.get())));
  219 | 				return Flux.from(subscriber -> {
  220 | 					try (ContextSnapshot.Scope ignored = context.setThreadLocals()) {
  221 | 						this.sessionService.findOrCreate(SessionContexts.createRequest(sessionId, r.context()));
  222 | 						answerUnansweredCalls(sessionId, r);
  223 | 						rest.subscribe(subscriber);
  224 | 					}
  225 | 				});
  226 | 			});
  227 | 		});
  228 | 	}
  229 | 
  230 | 	/**
  231 | 	 * The request with the session id in the tool context of its options, under
  232 | 	 * {@link SessionMemoryAdvisor#SESSION_ID_CONTEXT_KEY}, when the advisor is set to put
  233 | 	 * it there. The tool-calling loop runs inside this advisor and takes the tool context
  234 | 	 * from the request it gets, so the tools of the call see the id. A value the caller
  235 | 	 * set under the key stands, the other entries are kept, and options that take no tool
  236 | 	 * context are left alone.
  237 | 	 */
  238 | 	private ChatClientRequest withSessionIdInToolContext(ChatClientRequest request, String sessionId) {
  239 | 		if (!this.sessionIdInToolContext
  240 | 				|| !(request.prompt().getOptions() instanceof ToolCallingChatOptions options)) {
  241 | 			return request;
  242 | 		}
  243 | 		Map<String, Object> toolContext = options.getToolContext();
  244 | 		if (toolContext != null && toolContext.containsKey(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY)) {
  245 | 			return request;
  246 | 		}
  247 | 		Map<String, Object> withSessionId = (toolContext != null) ? new HashMap<>(toolContext) : new HashMap<>();
  248 | 		withSessionId.put(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId);
  249 | 		ChatOptions withToolContext = options.mutate().toolContext(withSessionId).build();
  250 | 		return request.mutate().prompt(request.prompt().mutate().chatOptions(withToolContext).build()).build();
  251 | 	}
  252 | 
  253 | 	/** Stores the results of a loop that ended without a model round, then compacts. */
  254 | 	private void complete(String sessionId, @Nullable ChatResponse response) {
  255 | 		if (response != null && endsWithoutModel(response)) {
  256 | 			storeResults(sessionId, response);
  257 | 		}
  258 | 		compactIfTriggered(sessionId);
  259 | 	}
  260 | 
  261 | 	/**
  262 | 	 * Whether the loop ended on a reply's tool calls without a model round after them: a
  263 | 	 * {@code returnDirect} result or a breached tool call limit. No round stored the
  264 | 	 * results then.
  265 | 	 */
  266 | 	private static boolean endsWithoutModel(@Nullable ChatResponse response) {
  267 | 		return response != null && response.getResults()
  268 | 			.stream()
  269 | 			.map(Generation::getMetadata)
  270 | 			.anyMatch(metadata -> ToolExecutionResult.FINISH_REASON.equals(metadata.getFinishReason())
  271 | 					|| ToolCallLimitExceededException.FINISH_REASON.equals(metadata.getFinishReason()));
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Answers the tool calls the conversation ends with when they have no results, before
  276 | 	 * a new turn adds to the log: the newest message is a reply with tool calls, and the
  277 | 	 * request brings no result for any of them. Every call gets
  278 | 	 * {@link #UNANSWERED_TOOL_RESULT}. The append is version-checked, so it lands only
  279 | 	 * while the reply is still the newest message. When the log moved, the reply is read
  280 | 	 * again: if another writer stored results, a resume on another instance for example,
  281 | 	 * nothing is written; if it was an unrelated append, a compaction for example, the
  282 | 	 * write is tried again, a few times. It has to land now: once the new turn's message
  283 | 	 * follows the reply, no later turn finds it. Best-effort all the same: a failure is
  284 | 	 * logged, and the memory advisor keeps the unanswered call out of the prompt.
  285 | 	 */
  286 | 	private void answerUnansweredCalls(String sessionId, ChatClientRequest request) {
  287 | 		try {
  288 | 			// One read for a turn that finds nothing open, which is nearly every turn
  289 | 			SessionEvent open = newestConversationEvent(sessionId);
  290 | 			if (open == null || !open.hasToolCalls()) {
  291 | 				return;
  292 | 			}
  293 | 			for (int attempt = 0; attempt < UNANSWERED_WRITE_ATTEMPTS; attempt++) {
  294 | 				// The version first, then the reply again: the append must not land
  295 | 				// after results another writer stored in between
  296 | 				long version = this.sessionService.getEventVersion(sessionId);
  297 | 				open = newestConversationEvent(sessionId);
  298 | 				if (open == null || !open.hasToolCalls()) {
  299 | 					return;
  300 | 				}
  301 | 				List<AssistantMessage.ToolCall> calls = ((AssistantMessage) open.getMessage()).getToolCalls();
  302 | 				if (answersAny(request, calls)) {
  303 | 					return;
  304 | 				}
  305 | 				SessionEvent answers = answersOf(open, "-unanswered",
  306 | 						Collections.nCopies(calls.size(), UNANSWERED_TOOL_RESULT));
  307 | 				if (this.sessionService.appendEvents(sessionId, List.of(answers), version)) {
  308 | 					logger.debug("Answered {} tool call(s) of session [{}] that had no results", calls.size(),
  309 | 							sessionId);
  310 | 					return;
  311 | 				}
  312 | 			}
  313 | 			logger.warn("Could not answer the unanswered tool calls of session [{}]: the log kept changing", sessionId);
  314 | 		}
  315 | 		catch (RuntimeException ex) {
  316 | 			logger.warn("Could not answer the unanswered tool calls of session [{}]: {}", sessionId, ex.getMessage());
  317 | 		}
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * The event that answers the reply's tool calls, one answer per call in the order of
  322 | 	 * the calls, with an id that derives from the reply's.
  323 | 	 */
  324 | 	private static SessionEvent answersOf(SessionEvent reply, String idSuffix, List<String> answers) {
  325 | 		List<AssistantMessage.ToolCall> calls = ((AssistantMessage) reply.getMessage()).getToolCalls();
  326 | 		List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
  327 | 		for (int i = 0; i < calls.size(); i++) {
  328 | 			responses.add(new ToolResponseMessage.ToolResponse(calls.get(i).id(), calls.get(i).name(), answers.get(i)));
  329 | 		}
  330 | 		return SessionEvent.builder()
  331 | 			.id(reply.getId() + idSuffix)
  332 | 			.sessionId(reply.getSessionId())
  333 | 			.message(ToolResponseMessage.builder().responses(responses).build())
  334 | 			.build();
  335 | 	}
  336 | 
  337 | 	/** Whether the request carries a tool result for one of the calls. */
  338 | 	private static boolean answersAny(ChatClientRequest request, List<AssistantMessage.ToolCall> calls) {
  339 | 		Set<String> ids = new HashSet<>();
  340 | 		calls.forEach(call -> ids.add(call.id()));
  341 | 		return request.prompt()
  342 | 			.getInstructions()
  343 | 			.stream()
  344 | 			.filter(ToolResponseMessage.class::isInstance)
  345 | 			.map(ToolResponseMessage.class::cast)
  346 | 			.flatMap(results -> results.getResponses().stream())
  347 | 			.anyMatch(result -> ids.contains(result.id()));
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * Answers the stored reply's tool calls in the log after a loop that ended without a
  352 | 	 * model round: with the results that came back directly, or, for a breached limit,
  353 | 	 * with the results of the calls that ran, the breach for the call that broke it and a
  354 | 	 * note for the calls after it. The results come in the order of the calls. Once: the
  355 | 	 * id derives from the reply's.
  356 | 	 */
  357 | 	private void storeResults(String sessionId, ChatResponse response) {
  358 | 		try {
  359 | 			SessionEvent stored = newestConversationEvent(sessionId);
  360 | 			if (stored == null || !stored.hasToolCalls()) {
  361 | 				return;
  362 | 			}
  363 | 			int calls = ((AssistantMessage) stored.getMessage()).getToolCalls().size();
  364 | 			this.sessionService.appendEvent(answersOf(stored, "-results", answers(response, calls)));
  365 | 		}
  366 | 		catch (RuntimeException ex) {
  367 | 			logger.warn("Could not store the tool results of session [{}]: {}", sessionId, ex.getMessage());
  368 | 		}
  369 | 	}
  370 | 
  371 | 	/** One answer per call, in the order of the calls. */
  372 | 	private static List<String> answers(ChatResponse response, int calls) {
  373 | 		List<String> answers = new ArrayList<>(calls);
  374 | 		for (Generation generation : response.getResults()) {
  375 | 			ChatGenerationMetadata metadata = generation.getMetadata();
  376 | 			if (ToolExecutionResult.FINISH_REASON.equals(metadata.getFinishReason())) {
  377 | 				answers.add(Objects.requireNonNullElse(generation.getOutput().getText(), ""));
  378 | 			}
  379 | 			else if (ToolCallLimitExceededException.FINISH_REASON.equals(metadata.getFinishReason())) {
  380 | 				if (metadata
  381 | 					.get(ToolCallLimitExceededException.METADATA_PARTIAL_TOOL_RESPONSES) instanceof List<?> ran) {
  382 | 					ran.stream()
  383 | 						.filter(ToolResponseMessage.ToolResponse.class::isInstance)
  384 | 						.map(result -> ((ToolResponseMessage.ToolResponse) result).responseData())
  385 | 						.forEach(answers::add);
  386 | 				}
  387 | 				answers.add(Objects.requireNonNullElse(generation.getOutput().getText(), ""));
  388 | 			}
  389 | 		}
  390 | 		while (answers.size() < calls) {
  391 | 			answers.add(NOT_RUN_TOOL_RESULT);
  392 | 		}
  393 | 		return answers.subList(0, calls);
  394 | 	}
  395 | 
  396 | 	/**
  397 | 	 * Compacts the session if the trigger fires: now, or on the scheduler when compaction
  398 | 	 * is asynchronous and none runs on the session yet.
  399 | 	 */
  400 | 	private void compactIfTriggered(String sessionId) {
  401 | 		if (this.compactionTrigger == null || this.compactionStrategy == null) {
  402 | 			return;
  403 | 		}
  404 | 		if (!this.asyncCompaction) {
  405 | 			compact(sessionId, this.compactionTrigger, this.compactionStrategy);
  406 | 		}
  407 | 		else if (this.compacting.add(sessionId)) {
  408 | 			CompactionTrigger trigger = this.compactionTrigger;
  409 | 			CompactionStrategy strategy = this.compactionStrategy;
  410 | 			// The caller's thread-local context goes with it: an authorizing
  411 | 			// SessionService
  412 | 			// takes the caller from the security context, and tracing keeps its parent
  413 | 			Runnable task = CONTEXT_SNAPSHOTS.captureAll().wrap(() -> {
  414 | 				try {
  415 | 					compact(sessionId, trigger, strategy);
  416 | 				}
  417 | 				finally {
  418 | 					this.compacting.remove(sessionId);
  419 | 				}
  420 | 			});
  421 | 			try {
  422 | 				this.scheduler.schedule(task);
  423 | 			}
  424 | 			catch (RuntimeException ex) {
  425 | 				this.compacting.remove(sessionId);
  426 | 				logger.warn("Could not schedule the compaction of session [{}]; it will be retried after the next "
  427 | 						+ "turn", sessionId, ex);
  428 | 			}
  429 | 		}
  430 | 	}
  431 | 
  432 | 	/** Whether an asynchronous compaction of this advisor runs on the session. */
  433 | 	boolean isCompacting(String sessionId) {
  434 | 		return this.compacting.contains(sessionId);
  435 | 	}
  436 | 
  437 | 	/** Compacts the session if the trigger fires; a failure is logged, not thrown. */
  438 | 	private void compact(String sessionId, CompactionTrigger trigger, CompactionStrategy strategy) {
  439 | 		try {
  440 | 			this.sessionService.compact(sessionId, trigger, strategy);
  441 | 		}
  442 | 		catch (RuntimeException ex) {
  443 | 			logger.warn("Compaction failed for session [{}]; the event log is unchanged and compaction will be "
  444 | 					+ "retried after the next turn", sessionId, ex);
  445 | 		}
  446 | 	}
  447 | 
  448 | 	/**
  449 | 	 * The newest message of the active view that is not a system message, or
  450 | 	 * {@code null}.
  451 | 	 */
  452 | 	private @Nullable SessionEvent newestConversationEvent(String sessionId) {
  453 | 		List<SessionEvent> events = this.sessionService.getEvents(sessionId,
  454 | 				EventFilter.active().toBuilder().lastN(1).build());
  455 | 		for (int i = events.size() - 1; i >= 0; i--) {
  456 | 			if (events.get(i).hasMessage() && events.get(i).getMessageType() != MessageType.SYSTEM) {
  457 | 				return events.get(i);
  458 | 			}
  459 | 		}
  460 | 		return null;
  461 | 	}
  462 | 
  463 | 	public static final class Builder {
  464 | 
  465 | 		private final SessionService sessionService;
  466 | 
  467 | 		private int order = DEFAULT_ORDER;
  468 | 
  469 | 		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
  470 | 
  471 | 		@Nullable private CompactionTrigger compactionTrigger;
  472 | 
  473 | 		@Nullable private CompactionStrategy compactionStrategy;
  474 | 
  475 | 		private boolean asyncCompaction;
  476 | 
  477 | 		private boolean sessionIdInToolContext;
  478 | 
  479 | 		private Builder(SessionService sessionService) {
  480 | 			Assert.notNull(sessionService, "sessionService must not be null");
  481 | 			this.sessionService = sessionService;
  482 | 		}
  483 | 
  484 | 		/**
  485 | 		 * The order; {@link #DEFAULT_ORDER} by default. Keep it before the tool-calling
  486 | 		 * advisor, so the loop runs inside the turn.
  487 | 		 */
  488 | 		public Builder order(int order) {
  489 | 			this.order = order;
  490 | 			return this;
  491 | 		}
  492 | 
  493 | 		/** The scheduler the session writes of a streamed call run on. */
  494 | 		public Builder scheduler(Scheduler scheduler) {
  495 | 			Assert.notNull(scheduler, "scheduler must not be null");
  496 | 			this.scheduler = scheduler;
  497 | 			return this;
  498 | 		}
  499 | 
  500 | 		/**
  501 | 		 * When to compact once a turn has ended; set together with
  502 | 		 * {@link #compactionStrategy}.
  503 | 		 */
  504 | 		public Builder compactionTrigger(CompactionTrigger trigger) {
  505 | 			Assert.notNull(trigger, "trigger must not be null");
  506 | 			this.compactionTrigger = trigger;
  507 | 			return this;
  508 | 		}
  509 | 
  510 | 		/** How to compact; set together with {@link #compactionTrigger}. */
  511 | 		public Builder compactionStrategy(CompactionStrategy strategy) {
  512 | 			Assert.notNull(strategy, "strategy must not be null");
  513 | 			this.compactionStrategy = strategy;
  514 | 			return this;
  515 | 		}
  516 | 
  517 | 		/**
  518 | 		 * Whether compaction runs after the call returns, on the {@link #scheduler}, so a
  519 | 		 * summarizing strategy adds no model call to the reply's latency; {@code false}
  520 | 		 * by default. A {@code call()} runs it with the caller's thread-local context,
  521 | 		 * the security context for example, captured with Micrometer's context
  522 | 		 * propagation; a {@code stream()} ends on the scheduler already. One compaction
  523 | 		 * per session runs at a time from this advisor; a new turn that writes to the
  524 | 		 * session meanwhile stays in the view, the compaction lands rebased onto its
  525 | 		 * events with the same summary. Only another compaction's boundary, or turns that
  526 | 		 * keep overtaking it, make it stand down until the next turn.
  527 | 		 */
  528 | 		public Builder asyncCompaction(boolean asyncCompaction) {
  529 | 			this.asyncCompaction = asyncCompaction;
  530 | 			return this;
  531 | 		}
  532 | 
  533 | 		/**
  534 | 		 * Whether the advisor puts the session id into the tool context of the call,
  535 | 		 * under {@link SessionMemoryAdvisor#SESSION_ID_CONTEXT_KEY}; {@code false} by
  536 | 		 * default. A tool that reads the session, such as
  537 | 		 * {@link org.springframework.ai.session.tool.SessionRecallTools}, then finds the
  538 | 		 * id without the caller passing it with {@code toolContext(..)} too. A value the
  539 | 		 * caller sets under the key stands.
  540 | 		 * <p>
  541 | 		 * The tool context goes to every tool of the call, so turning this on has three
  542 | 		 * effects beyond the session tools. Every tool that takes a {@code ToolContext}
  543 | 		 * sees the session id. The tools of an MCP server get it as request metadata,
  544 | 		 * unless a {@code ToolContextToMcpMetaConverter} leaves it out. And a tool that
  545 | 		 * takes a {@code ToolContext} is no longer refused when the call passes no tool
  546 | 		 * context: it runs with one that holds the session id only.
  547 | 		 */
  548 | 		public Builder sessionIdInToolContext(boolean sessionIdInToolContext) {
  549 | 			this.sessionIdInToolContext = sessionIdInToolContext;
  550 | 			return this;
  551 | 		}
  552 | 
  553 | 		public SessionTurnAdvisor build() {
  554 | 			if ((this.compactionTrigger == null) != (this.compactionStrategy == null)) {
  555 | 				throw new IllegalArgumentException(
  556 | 						"compactionTrigger and compactionStrategy must be set together — set both or neither");
  557 | 			}
  558 | 			return new SessionTurnAdvisor(this);
  559 | 		}
  560 | 
  561 | 	}
  562 | 
  563 | }
```
