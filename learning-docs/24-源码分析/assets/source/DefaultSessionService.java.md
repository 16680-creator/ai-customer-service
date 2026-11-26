<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session:2.1.0 | path: org/springframework/ai/session/DefaultSessionService.java -->
<!-- 正文引用：[[源码:DefaultSessionService.java#L起-L止]] ；代码块头部：(src: DefaultSessionService.java#L起-L止) -->

# DefaultSessionService.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session:2.1.0` |
| 路径 | `org/springframework/ai/session/DefaultSessionService.java` |
| 行数 | 320 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session/2.1.0/spring-ai-session-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:06.532Z |

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
   17 | package org.springframework.ai.session;
   18 | 
   19 | import java.time.Duration;
   20 | import java.time.Instant;
   21 | import java.util.ArrayList;
   22 | import java.util.HashSet;
   23 | import java.util.List;
   24 | import java.util.Objects;
   25 | import java.util.Set;
   26 | import java.util.UUID;
   27 | 
   28 | import org.jspecify.annotations.Nullable;
   29 | import org.slf4j.Logger;
   30 | import org.slf4j.LoggerFactory;
   31 | 
   32 | import org.springframework.ai.chat.messages.MessageType;
   33 | import org.springframework.ai.session.compaction.CompactionRequest;
   34 | import org.springframework.ai.session.compaction.CompactionResult;
   35 | import org.springframework.ai.session.compaction.CompactionStrategy;
   36 | import org.springframework.ai.session.compaction.CompactionTrigger;
   37 | import org.springframework.util.Assert;
   38 | 
   39 | /**
   40 |  * The {@link SessionService} on a {@link SessionRepository}.
   41 |  *
   42 |  * @author Christian Tzolov
   43 |  * @since 2.1.0
   44 |  */
   45 | public final class DefaultSessionService implements SessionService {
   46 | 
   47 | 	private static final Logger logger = LoggerFactory.getLogger(DefaultSessionService.class);
   48 | 
   49 | 	/**
   50 | 	 * How many times a compaction that a concurrent append overtook is rebased onto the
   51 | 	 * new events and written again before it is given up.
   52 | 	 */
   53 | 	static final int MAX_REBASES = 2;
   54 | 
   55 | 	/** The newest compaction boundary of a session. */
   56 | 	private static final EventFilter NEWEST_BOUNDARY = EventFilter.builder()
   57 | 		.eventTypes(Set.of(EventType.COMPACTION))
   58 | 		.lastN(1)
   59 | 		.build();
   60 | 
   61 | 	private final Duration defaultTimeToLive;
   62 | 
   63 | 	private final SessionRepository sessionRepository;
   64 | 
   65 | 	private final boolean allowSystemMessages;
   66 | 
   67 | 	private DefaultSessionService(SessionRepository sessionRepository, Duration defaultTimeToLive,
   68 | 			boolean allowSystemMessages) {
   69 | 		Assert.notNull(sessionRepository, "sessionRepository must not be null");
   70 | 		Assert.notNull(defaultTimeToLive, "defaultTimeToLive must not be null");
   71 | 		this.sessionRepository = sessionRepository;
   72 | 		this.defaultTimeToLive = defaultTimeToLive;
   73 | 		this.allowSystemMessages = allowSystemMessages;
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public Session create(CreateSessionRequest request) {
   78 | 		Assert.notNull(request, "request must not be null");
   79 | 
   80 | 		Instant now = Instant.now();
   81 | 		Instant expiresAt = (request.timeToLive() != null) ? now.plus(request.timeToLive())
   82 | 				: now.plus(this.defaultTimeToLive);
   83 | 
   84 | 		String sessionId = (request.id() != null && !request.id().isBlank()) ? request.id()
   85 | 				: UUID.randomUUID().toString();
   86 | 
   87 | 		Session session = Session.builder()
   88 | 			.id(sessionId)
   89 | 			.createdAt(now)
   90 | 			.expiresAt(expiresAt)
   91 | 			.metadata(request.metadata())
   92 | 			.build();
   93 | 
   94 | 		// Insert-only and atomic: never silently overwrite an existing session (an upsert
   95 | 		// would reassign its owner and TTL while keeping its event log), even when two
   96 | 		// callers create the same id concurrently.
   97 | 		if (!this.sessionRepository.saveIfAbsent(session)) {
   98 | 			throw new SessionAlreadyExistsException(sessionId);
   99 | 		}
  100 | 		return session;
  101 | 	}
  102 | 
  103 | 	@Override
  104 | 	@Nullable public Session findById(String sessionId) {
  105 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  106 | 
  107 | 		return this.sessionRepository.findById(sessionId);
  108 | 	}
  109 | 
  110 | 	@Override
  111 | 	public List<Session> findByMetadata(String name, String value) {
  112 | 		Assert.hasText(name, "name must not be null or empty");
  113 | 		Assert.notNull(value, "value must not be null");
  114 | 		return this.sessionRepository.findByMetadata(name, value);
  115 | 	}
  116 | 
  117 | 	private Session requireSession(String sessionId) {
  118 | 		Session session = this.sessionRepository.findById(sessionId);
  119 | 		if (session == null) {
  120 | 			throw new IllegalArgumentException("Session not found: " + sessionId);
  121 | 		}
  122 | 		return session;
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	public void delete(String sessionId) {
  127 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  128 | 		this.sessionRepository.delete(sessionId);
  129 | 	}
  130 | 
  131 | 	@Override
  132 | 	public int deleteExpiredSessions(Instant before) {
  133 | 		Assert.notNull(before, "before must not be null");
  134 | 		return this.sessionRepository.deleteExpiredSessions(before);
  135 | 	}
  136 | 
  137 | 	@Override
  138 | 	public void appendEvent(SessionEvent event) {
  139 | 		checkSystemMessage(event);
  140 | 		this.sessionRepository.appendEvent(event);
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public boolean appendEvents(String sessionId, List<SessionEvent> events, long expectedVersion) {
  145 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  146 | 		Assert.notEmpty(events, "events must not be null or empty");
  147 | 		events.forEach(this::checkSystemMessage);
  148 | 		return this.sessionRepository.appendEvents(sessionId, events, expectedVersion);
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	public long getEventVersion(String sessionId) {
  153 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  154 | 		return this.sessionRepository.getEventVersion(sessionId);
  155 | 	}
  156 | 
  157 | 	private void checkSystemMessage(SessionEvent event) {
  158 | 		Assert.notNull(event, "event must not be null");
  159 | 		if (!this.allowSystemMessages && event.hasMessage() && event.getMessageType() == MessageType.SYSTEM) {
  160 | 			throw new IllegalArgumentException("Storing a SystemMessage in session '" + event.getSessionId()
  161 | 					+ "' is disabled. System prompts are configuration: supply them on every request "
  162 | 					+ "(e.g. ChatClient defaultSystem/.system, or your own prompt builder) and keep per-session "
  163 | 					+ "settings in Session.metadata. To store system messages anyway, use "
  164 | 					+ "DefaultSessionService.builder().allowSystemMessages(true) or set "
  165 | 					+ "spring.ai.session.allow-system-messages=true. See the \"System Messages\" reference page.");
  166 | 		}
  167 | 	}
  168 | 
  169 | 	@Override
  170 | 	public List<SessionEvent> getEvents(String sessionId, EventFilter filter) {
  171 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  172 | 		Assert.notNull(filter, "filter must not be null");
  173 | 		return this.sessionRepository.findEvents(sessionId, filter);
  174 | 	}
  175 | 
  176 | 	@Override
  177 | 	public List<SessionEvent> searchEvents(String sessionId, EventQuery query) {
  178 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  179 | 		Assert.notNull(query, "query must not be null");
  180 | 		return this.sessionRepository.searchEvents(sessionId, query);
  181 | 	}
  182 | 
  183 | 	@Override
  184 | 	public CompactionResult compact(String sessionId, CompactionTrigger trigger, CompactionStrategy strategy) {
  185 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  186 | 		Assert.notNull(trigger, "trigger must not be null");
  187 | 		Assert.notNull(strategy, "strategy must not be null");
  188 | 
  189 | 		Session session = requireSession(sessionId);
  190 | 
  191 | 		// The version is read before the boundary and the view, so the version passed to
  192 | 		// the version-checked write is at most the one the view was read at, and the
  193 | 		// boundary read says which view that was: a write that fails on the version
  194 | 		// then tells an append apart from another compaction.
  195 | 		long version = this.sessionRepository.getEventVersion(session.id());
  196 | 		String boundary = newestBoundaryId(session.id());
  197 | 
  198 | 		// The active view only: the events an earlier compaction covered are kept for
  199 | 		// recall and are not summarized again.
  200 | 		List<SessionEvent> view = this.sessionRepository.findEvents(session.id(), EventFilter.active());
  201 | 
  202 | 		CompactionRequest request = CompactionRequest.of(session, view);
  203 | 
  204 | 		if (!trigger.shouldCompact(request)) {
  205 | 			return CompactionResult.unchanged();
  206 | 		}
  207 | 
  208 | 		CompactionResult result = strategy.compact(request);
  209 | 		if (result.isUnchanged()) {
  210 | 			return result;
  211 | 		}
  212 | 
  213 | 		// Record the decision as an appended boundary followed by its summary events. The
  214 | 		// log is never changed: the boundary names, by id, what the view is from now on.
  215 | 		Set<String> seen = new HashSet<>();
  216 | 		view.forEach(event -> seen.add(event.getId()));
  217 | 		for (int rebases = 0;; rebases++) {
  218 | 			if (this.sessionRepository.appendEvents(session.id(), result.toEvents(session.id()), version)) {
  219 | 				return result;
  220 | 			}
  221 | 			// A concurrent writer changed the log. Another compaction's boundary makes
  222 | 			// this result stale, its view is gone. An append does not: the summary still
  223 | 			// stands in for what it covered, and the events appended since join the
  224 | 			// preserved tail verbatim, so the result is rebased onto them and written
  225 | 			// again, without a second summary. Bounded, so a busy session cannot hold
  226 | 			// the compaction up for long.
  227 | 			if (rebases == MAX_REBASES) {
  228 | 				logger.debug("Compaction of session [{}] overtaken {} times; giving up until the next pass",
  229 | 						session.id(), rebases + 1);
  230 | 				return CompactionResult.unchanged();
  231 | 			}
  232 | 			version = this.sessionRepository.getEventVersion(session.id());
  233 | 			if (!Objects.equals(boundary, newestBoundaryId(session.id()))) {
  234 | 				return CompactionResult.unchanged();
  235 | 			}
  236 | 			List<SessionEvent> since = new ArrayList<>();
  237 | 			for (SessionEvent event : this.sessionRepository.findEvents(session.id(), EventFilter.active())) {
  238 | 				if (seen.add(event.getId())) {
  239 | 					since.add(event);
  240 | 				}
  241 | 			}
  242 | 			result = rebase(result, since);
  243 | 		}
  244 | 	}
  245 | 
  246 | 	private @Nullable String newestBoundaryId(String sessionId) {
  247 | 		List<SessionEvent> boundaries = this.sessionRepository.findEvents(sessionId, NEWEST_BOUNDARY);
  248 | 		return boundaries.isEmpty() ? null : boundaries.get(boundaries.size() - 1).getId();
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * The result with the events appended since its view was read preserved after its
  253 | 	 * tail. A stored system message among them is preserved like the rest, not pinned:
  254 | 	 * the next compaction pins it and covers the one before.
  255 | 	 */
  256 | 	private static CompactionResult rebase(CompactionResult result, List<SessionEvent> since) {
  257 | 		if (since.isEmpty()) {
  258 | 			return result;
  259 | 		}
  260 | 		List<SessionEvent> preserved = new ArrayList<>(result.preservedEvents());
  261 | 		preserved.addAll(since);
  262 | 		return new CompactionResult(result.coveredEvents(), result.pinnedEvents(), preserved, result.summary(),
  263 | 				result.tokensEstimatedSaved(), result.strategy());
  264 | 	}
  265 | 
  266 | 	public static Builder builder() {
  267 | 		return new Builder();
  268 | 	}
  269 | 
  270 | 	public static class Builder {
  271 | 
  272 | 		@Nullable private SessionRepository sessionRepository;
  273 | 
  274 | 		private Duration defaultTimeToLive = Duration.ofDays(60);
  275 | 
  276 | 		private boolean allowSystemMessages = false;
  277 | 
  278 | 		/** The store; required. */
  279 | 		public Builder sessionRepository(SessionRepository sessionRepository) {
  280 | 			Assert.notNull(sessionRepository, "sessionRepository must not be null");
  281 | 			this.sessionRepository = sessionRepository;
  282 | 			return this;
  283 | 		}
  284 | 
  285 | 		/**
  286 | 		 * Whether {@link #appendEvent(SessionEvent)} and
  287 | 		 * {@link #appendEvents(String, List, long)} may store a
  288 | 		 * {@link org.springframework.ai.chat.messages.SystemMessage}; {@code false} by
  289 | 		 * default. A system prompt is configuration, best supplied on every request, so
  290 | 		 * storing one is rejected with an {@link IllegalArgumentException} that says how
  291 | 		 * to enable it. When enabled, the latest stored system message is the session's
  292 | 		 * system prompt, which compaction pins; see
  293 | 		 * {@link org.springframework.ai.session.compaction.WindowCompactionStrategy}.
  294 | 		 */
  295 | 		public Builder allowSystemMessages(boolean allowSystemMessages) {
  296 | 			this.allowSystemMessages = allowSystemMessages;
  297 | 			return this;
  298 | 		}
  299 | 
  300 | 		/** The time to live of a session created without one; 60 days by default. */
  301 | 		public Builder defaultTimeToLive(Duration defaultTimeToLive) {
  302 | 			Assert.notNull(defaultTimeToLive, "defaultTimeToLive must not be null");
  303 | 			this.defaultTimeToLive = defaultTimeToLive;
  304 | 			return this;
  305 | 		}
  306 | 
  307 | 		/**
  308 | 		 * Builds the service.
  309 | 		 * @throws IllegalStateException if no repository was set
  310 | 		 */
  311 | 		public DefaultSessionService build() {
  312 | 			if (this.sessionRepository == null) {
  313 | 				throw new IllegalStateException("sessionRepository must be set");
  314 | 			}
  315 | 			return new DefaultSessionService(this.sessionRepository, this.defaultTimeToLive, this.allowSystemMessages);
  316 | 		}
  317 | 
  318 | 	}
  319 | 
  320 | }
```
