<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session-advisor:2.1.0 | path: org/springframework/ai/session/advisor/SessionMemoryAdvisor.java -->
<!-- 正文引用：[[源码:SessionMemoryAdvisor.java#L起-L止]] ；代码块头部：(src: SessionMemoryAdvisor.java#L起-L止) -->

# SessionMemoryAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session-advisor:2.1.0` |
| 路径 | `org/springframework/ai/session/advisor/SessionMemoryAdvisor.java` |
| 行数 | 402 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session-advisor/2.1.0/spring-ai-session-advisor-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:58:21.888Z |

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
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Set;
   23 | import java.util.UUID;
   24 | import java.util.function.Supplier;
   25 | 
   26 | import io.micrometer.context.ContextSnapshot;
   27 | import io.micrometer.context.ContextSnapshotFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | import org.slf4j.Logger;
   30 | import org.slf4j.LoggerFactory;
   31 | import reactor.core.publisher.Flux;
   32 | import reactor.core.publisher.Mono;
   33 | import reactor.core.scheduler.Scheduler;
   34 | 
   35 | import org.springframework.ai.chat.client.ChatClientMessageAggregator;
   36 | import org.springframework.ai.chat.client.ChatClientRequest;
   37 | import org.springframework.ai.chat.client.ChatClientResponse;
   38 | import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
   39 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   40 | import org.springframework.ai.chat.client.advisor.api.MemoryAdvisor;
   41 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   42 | import org.springframework.ai.chat.messages.AssistantMessage;
   43 | import org.springframework.ai.chat.messages.Message;
   44 | import org.springframework.ai.chat.messages.MessageType;
   45 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   46 | import org.springframework.ai.chat.model.Generation;
   47 | import org.springframework.ai.session.EventFilter;
   48 | import org.springframework.ai.session.MessageFilter;
   49 | import org.springframework.ai.session.SessionAttributes;
   50 | import org.springframework.ai.session.SessionEvent;
   51 | import org.springframework.ai.session.SessionService;
   52 | import org.springframework.core.Ordered;
   53 | import org.springframework.util.Assert;
   54 | 
   55 | /**
   56 |  * Keeps a {@code ChatClient} conversation in a session, on each model call:
   57 |  * <ol>
   58 |  * <li>Reads the session's active view and prepends it to the prompt. Of the system
   59 |  * messages stored in the session only the latest one is used, the system prompt; system
   60 |  * messages go first, and an exact duplicate is sent once.</li>
   61 |  * <li>Stores the request's user message, if the {@link MessageFilter} accepts it; inside
   62 |  * a tool loop the trailing tool results, always.</li>
   63 |  * <li>After the model responds, stores the reply, if the filter accepts it; a reply with
   64 |  * tool calls, always. By default an empty assistant message, no text, no tool calls, no
   65 |  * media, is not stored.</li>
   66 |  * <li>Names the stored events on the request and response context
   67 |  * ({@link #REQUEST_EVENT_ID_CONTEXT_KEY}, {@link #RESPONSE_EVENT_IDS_CONTEXT_KEY}), so a
   68 |  * caller can correlate what it sent and received with the log.</li>
   69 |  * </ol>
   70 |  * The session comes from {@link #SESSION_ID_CONTEXT_KEY}, required on every request:
   71 |  * omitting it throws {@link IllegalStateException}, so sessions are never shared by
   72 |  * accident.
   73 |  * <p>
   74 |  * On its own the advisor keeps the history and nothing more: no compaction, and the tool
   75 |  * results of a loop that ends without a model round, a {@code returnDirect} tool, are not
   76 |  * stored. What happens once per turn, around the whole tool-calling loop, is the
   77 |  * {@link SessionTurnAdvisor}'s; {@link SessionAdvisors} builds the two together.
   78 |  * <p>
   79 |  * By default the advisor runs inside the tool-calling loop (order {@code
   80 |  * HIGHEST_PRECEDENCE + 1000}), once per round: from the second round on the loop's prompt
   81 |  * carries the turn so far, and only the history before it is prepended. A tool call or
   82 |  * tool result left without its counterpart is kept out of the prompt.
   83 |  *
   84 |  * @author Christian Tzolov
   85 |  * @since 2.1.0
   86 |  */
   87 | public final class SessionMemoryAdvisor implements BaseAdvisor, MemoryAdvisor {
   88 | 
   89 | 	private static final Logger logger = LoggerFactory.getLogger(SessionMemoryAdvisor.class);
   90 | 
   91 | 	/**
   92 | 	 * Carries the caller's context to the session calls of a stream: the thread locals of
   93 | 	 * a blocking caller, the Reactor context of a reactive one, the latter winning where
   94 | 	 * both hold a key.
   95 | 	 */
   96 | 	private static final ContextSnapshotFactory CONTEXT_SNAPSHOTS = ContextSnapshotFactory.builder().build();
   97 | 
   98 | 	/**
   99 | 	 * Above the order of the tool-calling loop, which is
  100 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor#DEFAULT_ORDER}
  101 | 	 * by default, so the memory advisor runs inside the loop, once per round. An advisor
  102 | 	 * that has to run in every round too takes an order above the loop's: below this one
  103 | 	 * to run around the memory advisor, above this one to run inside it.
  104 | 	 */
  105 | 	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;
  106 | 
  107 | 	/**
  108 | 	 * Context key of the session a request runs on;
  109 | 	 * {@link MemoryAdvisor#CONVERSATION_ID}, the conversation key of every memory
  110 | 	 * advisor. Set with {@code .advisors(a ->
  111 | 	 * a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))}.
  112 | 	 */
  113 | 	public static final String SESSION_ID_CONTEXT_KEY = MemoryAdvisor.CONVERSATION_ID;
  114 | 
  115 | 	/**
  116 | 	 * Context key for the metadata of a session the advisor creates on first use. The
  117 | 	 * value must be a {@code Map<String, Object>}; it is ignored for an existing session.
  118 | 	 * The agent attributes ({@link SessionAttributes#AGENT_NAME}) label the agent a
  119 | 	 * session runs; set them once for every call of a {@code ChatClient} with
  120 | 	 * {@code .defaultAdvisors(a -> a.param(SessionMemoryAdvisor.SESSION_METADATA_CONTEXT_KEY, Map.of(SessionAttributes.AGENT_NAME, "support")))}.
  121 | 	 * A request that passes its own value replaces that default as a whole, so it
  122 | 	 * includes the agent attributes too.
  123 | 	 */
  124 | 	public static final String SESSION_METADATA_CONTEXT_KEY = "chat_memory_session_metadata";
  125 | 
  126 | 	/**
  127 | 	 * Context key of a per-request {@link EventFilter}, merged over the advisor's (see
  128 | 	 * {@link EventFilter#merge(EventFilter)}). Set with {@code .advisors(a ->
  129 | 	 * a.param(SessionMemoryAdvisor.EVENT_FILTER_CONTEXT_KEY, EventFilter.lastN(10)))}.
  130 | 	 */
  131 | 	public static final String EVENT_FILTER_CONTEXT_KEY = "chat_memory_event_filter";
  132 | 
  133 | 	/**
  134 | 	 * Context key the advisor sets on the request it forwards, and so on the response:
  135 | 	 * the id of the event that stores the request's trailing user or tool message; absent
  136 | 	 * when none was stored. Lets a caller name the message it sent by the id the log
  137 | 	 * knows it by; inside a tool loop it is the current round's.
  138 | 	 */
  139 | 	public static final String REQUEST_EVENT_ID_CONTEXT_KEY = "chat_memory_request_event_id";
  140 | 
  141 | 	/**
  142 | 	 * Context key the advisor sets on the response: the ids of the events that store the
  143 | 	 * reply's assistant messages, in reply order, a {@code List<String>}; empty when none
  144 | 	 * was stored. Inside a tool loop it is the current round's. A streamed reply's chunks
  145 | 	 * do not carry it.
  146 | 	 */
  147 | 	public static final String RESPONSE_EVENT_IDS_CONTEXT_KEY = "chat_memory_response_event_ids";
  148 | 
  149 | 	private final SessionService sessionService;
  150 | 
  151 | 	private final int order;
  152 | 
  153 | 	private final Scheduler scheduler;
  154 | 
  155 | 	private final EventFilter eventFilter;
  156 | 
  157 | 	private final MessageFilter messageFilter;
  158 | 
  159 | 	private SessionMemoryAdvisor(Builder builder) {
  160 | 		this.sessionService = builder.sessionService;
  161 | 		this.order = builder.order;
  162 | 		this.scheduler = builder.scheduler;
  163 | 		this.eventFilter = builder.eventFilter;
  164 | 		this.messageFilter = builder.messageFilter;
  165 | 	}
  166 | 
  167 | 	@Override
  168 | 	public int getOrder() {
  169 | 		return this.order;
  170 | 	}
  171 | 
  172 | 	@Override
  173 | 	public Scheduler getScheduler() {
  174 | 		return this.scheduler;
  175 | 	}
  176 | 
  177 | 	@Override
  178 | 	public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
  179 | 
  180 | 		// 1. Resolve the session id (it must be in the context) and find or create the
  181 | 		// session. Whether the caller may use it is not decided here: an authorizing
  182 | 		// SessionService (see the security module) checks every read and append.
  183 | 		String sessionId = SessionContexts.sessionId(request.context());
  184 | 		this.sessionService.findOrCreate(SessionContexts.createRequest(sessionId, request.context()));
  185 | 
  186 | 		// 2. Read the history: always the active view, whatever the configured or
  187 | 		// per-request filter says; the events a compaction covered live on for search
  188 | 		// only
  189 | 		EventFilter eventFilter = resolveEventFilter(request.context()).merge(EventFilter.active());
  190 | 		List<SessionEvent> events = this.sessionService.getEvents(sessionId, eventFilter);
  191 | 		// A lastN window, or a messageTypes filter without SYSTEM, may leave the system
  192 | 		// prompt, which is configuration, outside the events read: look it up on its own
  193 | 		SessionEvent systemPrompt = PromptHistory.latestStoredSystemPrompt(events);
  194 | 		if (systemPrompt == null && !canReturnSystemPrompt(eventFilter)) {
  195 | 			systemPrompt = latestStoredSystemPrompt(sessionId);
  196 | 		}
  197 | 
  198 | 		// 3. The prompt: the history the request does not carry yet, valid for the model
  199 | 		List<Message> prompt = PromptHistory.of(events, (systemPrompt != null) ? systemPrompt.getMessage() : null)
  200 | 			.prompt(request.prompt().getInstructions(), sessionId);
  201 | 
  202 | 		// 4. Store the request's trailing user message, subject to the message filter, or
  203 | 		// tool results, always. Skipping only affects persistence, not the prompt. A
  204 | 		// request without one stores nothing.
  205 | 		Message userMessage = PromptHistory.lastUserOrToolResponse(request.prompt().getInstructions());
  206 | 		String requestEventId = null;
  207 | 		if (userMessage != null && shouldPersist(userMessage, sessionId)) {
  208 | 			requestEventId = UUID.randomUUID().toString();
  209 | 			this.sessionService.appendEvent(
  210 | 					SessionEvent.builder().id(requestEventId).sessionId(sessionId).message(userMessage).build());
  211 | 		}
  212 | 
  213 | 		// The forwarded request copies the context, so the id reaches the response too.
  214 | 		// When nothing was stored the key is left out, rather than set to null.
  215 | 		ChatClientRequest.Builder forwarded = request.mutate()
  216 | 			.prompt(request.prompt().mutate().messages(prompt).build());
  217 | 		if (requestEventId != null) {
  218 | 			forwarded.context(REQUEST_EVENT_ID_CONTEXT_KEY, requestEventId);
  219 | 		}
  220 | 		return forwarded.build();
  221 | 	}
  222 | 
  223 | 	@Override
  224 | 	public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
  225 | 		String sessionId = SessionContexts.sessionId(response.context());
  226 | 
  227 | 		// 1. Append the assistant message(s) produced by the model, subject to the
  228 | 		// configured message filter. By default excludes messages that carry no
  229 | 		// content — blank text, no tool calls, and no media.
  230 | 		List<String> responseEventIds = new ArrayList<>();
  231 | 		if (response.chatResponse() != null) {
  232 | 			response.chatResponse().getResults().stream().map(Generation::getOutput).forEach(reply -> {
  233 | 				if (!shouldPersist(reply, sessionId)) {
  234 | 					return;
  235 | 				}
  236 | 				String eventId = UUID.randomUUID().toString();
  237 | 				this.sessionService
  238 | 					.appendEvent(SessionEvent.builder().id(eventId).sessionId(sessionId).message(reply).build());
  239 | 				responseEventIds.add(eventId);
  240 | 			});
  241 | 		}
  242 | 		ChatClientResponse named = response.mutate()
  243 | 			.context(RESPONSE_EVENT_IDS_CONTEXT_KEY, List.copyOf(responseEventIds))
  244 | 			.build();
  245 | 
  246 | 		return named;
  247 | 	}
  248 | 
  249 | 	/** Whether a read with the filter returns every stored system message of the view. */
  250 | 	private static boolean canReturnSystemPrompt(EventFilter filter) {
  251 | 		Set<MessageType> types = filter.messageTypes();
  252 | 		return filter.window() instanceof EventFilter.Window.All
  253 | 				&& (types == null || types.isEmpty() || types.contains(MessageType.SYSTEM));
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Reads the latest stored (non-synthetic, active) system message of the session with
  258 | 	 * one pushed-down query, for a read whose filter may not return it.
  259 | 	 */
  260 | 	private @Nullable SessionEvent latestStoredSystemPrompt(String sessionId) {
  261 | 		EventFilter latestSystem = EventFilter.builder()
  262 | 			.messageTypes(Set.of(MessageType.SYSTEM))
  263 | 			.excludeSynthetic(true)
  264 | 			.activeView(true)
  265 | 			.lastN(1)
  266 | 			.build();
  267 | 		List<SessionEvent> found = this.sessionService.getEvents(sessionId, latestSystem);
  268 | 		return found.isEmpty() ? null : found.get(0);
  269 | 	}
  270 | 
  271 | 	@Override
  272 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
  273 | 		// The session reads and writes run on the scheduler, never on the model's thread,
  274 | 		// with the caller's context (the security context, the current observation)
  275 | 		// restored around them: the thread locals of a blocking caller, and what a
  276 | 		// reactive caller, under WebFlux for example, carries in the Reactor context
  277 | 		return Flux.deferContextual(contextView -> {
  278 | 			ContextSnapshot context = CONTEXT_SNAPSHOTS.captureAll(contextView);
  279 | 			return Mono.just(request)
  280 | 				.publishOn(this.scheduler)
  281 | 				.map(r -> withContext(context, () -> this.before(r, chain)))
  282 | 				.flatMapMany(chain::nextStream)
  283 | 				.publishOn(this.scheduler)
  284 | 				.transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
  285 | 						r -> withContext(context, () -> this.after(r, chain))));
  286 | 		});
  287 | 	}
  288 | 
  289 | 	private static <T> T withContext(ContextSnapshot context, Supplier<T> action) {
  290 | 		try (ContextSnapshot.Scope ignored = context.setThreadLocals()) {
  291 | 			return action.get();
  292 | 		}
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * Returns the advisor's configured filter, merged with the per-request filter from
  297 | 	 * {@link #EVENT_FILTER_CONTEXT_KEY} when present, so that request-level parameters
  298 | 	 * override the advisor defaults.
  299 | 	 */
  300 | 	private EventFilter resolveEventFilter(Map<String, @Nullable Object> context) {
  301 | 		Object requestFilterValue = context.get(EVENT_FILTER_CONTEXT_KEY);
  302 | 		if (requestFilterValue == null) {
  303 | 			return this.eventFilter;
  304 | 		}
  305 | 		if (!(requestFilterValue instanceof EventFilter requestEventFilter)) {
  306 | 			throw new IllegalArgumentException("Advisor context value for '" + EVENT_FILTER_CONTEXT_KEY
  307 | 					+ "' must be an EventFilter but was " + requestFilterValue.getClass().getName());
  308 | 		}
  309 | 		return this.eventFilter.merge(requestEventFilter);
  310 | 	}
  311 | 
  312 | 	/**
  313 | 	 * Whether the message is stored. A tool call and a tool result always are: either
  314 | 	 * half alone makes a prompt every provider rejects. The {@link MessageFilter} decides
  315 | 	 * for every other message.
  316 | 	 */
  317 | 	private boolean shouldPersist(Message message, String sessionId) {
  318 | 		if (message instanceof ToolResponseMessage
  319 | 				|| (message instanceof AssistantMessage assistant && assistant.hasToolCalls())) {
  320 | 			return true;
  321 | 		}
  322 | 		if (!this.messageFilter.shouldPersist(message)) {
  323 | 			logger.debug("Skipping [{}] message for session [{}] — rejected by the configured MessageFilter",
  324 | 					message.getMessageType(), sessionId);
  325 | 			return false;
  326 | 		}
  327 | 		return true;
  328 | 	}
  329 | 
  330 | 	public static Builder builder(SessionService sessionService) {
  331 | 		return new Builder(sessionService);
  332 | 	}
  333 | 
  334 | 	public static final class Builder {
  335 | 
  336 | 		private final SessionService sessionService;
  337 | 
  338 | 		// Deliberately higher (lower-precedence) than ToolCallingAdvisor's default order
  339 | 		// (HIGHEST_PRECEDENCE + 300): with ascending order sort, the lower value runs
  340 | 		// outer, so this places SessionMemoryAdvisor nested inside a default-configured
  341 | 		// ToolCallingAdvisor's tool-call loop rather than wrapping it -- before()/after()
  342 | 		// then run once per round, which PromptHistory's overlap with the loop's own
  343 | 		// prompt is built to tolerate.
  344 | 		private int order = DEFAULT_ORDER;
  345 | 
  346 | 		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
  347 | 
  348 | 		private EventFilter eventFilter = EventFilter.all();
  349 | 
  350 | 		private MessageFilter messageFilter = MessageFilter.skipEmptyMessages();
  351 | 
  352 | 		private Builder(SessionService sessionService) {
  353 | 			Assert.notNull(sessionService, "sessionService must not be null");
  354 | 			this.sessionService = sessionService;
  355 | 		}
  356 | 
  357 | 		public Builder order(int order) {
  358 | 			this.order = order;
  359 | 			return this;
  360 | 		}
  361 | 
  362 | 		public Builder scheduler(Scheduler scheduler) {
  363 | 			Assert.notNull(scheduler, "scheduler must not be null");
  364 | 			this.scheduler = scheduler;
  365 | 			return this;
  366 | 		}
  367 | 
  368 | 		/**
  369 | 		 * The filter of the history read into the prompt: always the active view, all of
  370 | 		 * it or its newest {@link EventFilter#lastN(int) N events}, optionally only some
  371 | 		 * {@link EventFilter#messageTypes() message types} or without synthetic events;
  372 | 		 * {@link EventFilter#all()} by default.
  373 | 		 */
  374 | 		public Builder eventFilter(EventFilter eventFilter) {
  375 | 			Assert.notNull(eventFilter, "eventFilter must not be null");
  376 | 			this.eventFilter = eventFilter;
  377 | 			return this;
  378 | 		}
  379 | 
  380 | 		/**
  381 | 		 * The filter of what is stored: the request's user message and the reply's
  382 | 		 * assistant messages; a rejected message is never stored and so never replayed,
  383 | 		 * while the outgoing prompt is unaffected. Tool calls and tool results are always
  384 | 		 * stored; keep them out of the prompt with the {@link EventFilter}'s {@code
  385 | 		 * messageTypes} instead. {@link MessageFilter#skipEmptyMessages()} by default:
  386 | 		 * some models reject an empty assistant message replayed as history, so compose a
  387 | 		 * filter of your own with it rather than replacing it, {@code
  388 | 		 * myFilter.and(MessageFilter.skipEmptyMessages())}.
  389 | 		 */
  390 | 		public Builder messageFilter(MessageFilter messageFilter) {
  391 | 			Assert.notNull(messageFilter, "messageFilter must not be null");
  392 | 			this.messageFilter = messageFilter;
  393 | 			return this;
  394 | 		}
  395 | 
  396 | 		public SessionMemoryAdvisor build() {
  397 | 			return new SessionMemoryAdvisor(this);
  398 | 		}
  399 | 
  400 | 	}
  401 | 
  402 | }
```
