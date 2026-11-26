<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session:2.1.0 | path: org/springframework/ai/session/SessionService.java -->
<!-- 正文引用：[[源码:SessionService.java#L起-L止]] ；代码块头部：(src: SessionService.java#L起-L止) -->

# SessionService.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session:2.1.0` |
| 路径 | `org/springframework/ai/session/SessionService.java` |
| 行数 | 210 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session/2.1.0/spring-ai-session-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:05.890Z |

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
   19 | import java.time.Instant;
   20 | import java.util.List;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.ai.session.compaction.CompactionResult;
   26 | import org.springframework.ai.session.compaction.CompactionStrategy;
   27 | import org.springframework.ai.session.compaction.CompactionTrigger;
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * The sessions of an application and their conversations: creating and finding sessions,
   32 |  * appending, reading and searching their events, compacting them. {@link #appendEvent}
   33 |  * appends one event. {@link #appendEvents(String, List, long)} appends a batch only if
   34 |  * the log has not changed since the caller read its version. Compaction writes this way,
   35 |  * and so can any other code that builds events from the log.
   36 |  * {@link DefaultSessionService} implements it on a {@link SessionRepository}.
   37 |  *
   38 |  * @author Christian Tzolov
   39 |  * @since 2.1.0
   40 |  */
   41 | public interface SessionService {
   42 | 
   43 | 	// Sessions
   44 | 
   45 | 	/**
   46 | 	 * Creates a session with the request's id, a random UUID when it has none, its time
   47 | 	 * to live and its metadata.
   48 | 	 * @throws SessionAlreadyExistsException if a session with the requested id exists
   49 | 	 */
   50 | 	Session create(CreateSessionRequest request);
   51 | 
   52 | 	/**
   53 | 	 * The session with the request's id, created from the request when there is none.
   54 | 	 * Safe under concurrent first use: when another caller creates it first, the stored
   55 | 	 * session is returned, not this request's. The default finds, creates, and on a
   56 | 	 * {@link SessionAlreadyExistsException} finds again, through this service's own
   57 | 	 * {@link #findById} and {@link #create}.
   58 | 	 * @throws IllegalArgumentException if the request has no id, null or blank
   59 | 	 */
   60 | 	default Session findOrCreate(CreateSessionRequest request) {
   61 | 		Assert.notNull(request, "request must not be null");
   62 | 		String sessionId = request.id();
   63 | 		Assert.hasText(sessionId, "request must have an id");
   64 | 		Session session = findById(sessionId);
   65 | 		if (session != null) {
   66 | 			return session;
   67 | 		}
   68 | 		try {
   69 | 			return create(request);
   70 | 		}
   71 | 		catch (SessionAlreadyExistsException ex) {
   72 | 			Session existing = findById(sessionId);
   73 | 			if (existing == null) {
   74 | 				throw ex;
   75 | 			}
   76 | 			return existing;
   77 | 		}
   78 | 	}
   79 | 
   80 | 	@Nullable Session findById(String sessionId);
   81 | 
   82 | 	/**
   83 | 	 * The sessions whose metadata holds {@code value} under {@code name}, oldest first,
   84 | 	 * for the keys the store indexes; see {@link SessionRepository#findByMetadata}. The
   85 | 	 * sessions of a principal are {@code findByMetadata(SessionAttributes.PRINCIPAL_NAME,
   86 | 	 * name)}.
   87 | 	 */
   88 | 	List<Session> findByMetadata(String name, String value);
   89 | 
   90 | 	/** Deletes the session and its events. */
   91 | 	void delete(String sessionId);
   92 | 
   93 | 	/**
   94 | 	 * Deletes every session that expired before the given instant, with their events, and
   95 | 	 * returns how many. Nothing calls this on its own: call it from a scheduler, every
   96 | 	 * hour for example.
   97 | 	 */
   98 | 	int deleteExpiredSessions(Instant before);
   99 | 
  100 | 	// Events
  101 | 
  102 | 	/**
  103 | 	 * Appends an event to the session named by {@link SessionEvent#getSessionId()}.
  104 | 	 * @throws IllegalArgumentException if the event wraps a
  105 | 	 * {@link org.springframework.ai.chat.messages.SystemMessage} and the service does not
  106 | 	 * allow storing system messages; see
  107 | 	 * {@link DefaultSessionService.Builder#allowSystemMessages(boolean)}
  108 | 	 */
  109 | 	void appendEvent(SessionEvent event);
  110 | 
  111 | 	/**
  112 | 	 * Wraps the message in a {@link SessionEvent} and appends it; see
  113 | 	 * {@link #appendEvent(SessionEvent)}.
  114 | 	 */
  115 | 	default void appendMessage(String sessionId, Message message) {
  116 | 		appendEvent(SessionEvent.builder().sessionId(sessionId).message(message).build());
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Appends one or more events in order, atomically, only if the log's version still
  121 | 	 * equals {@code expectedVersion}. Otherwise nothing is written and {@code false} is
  122 | 	 * returned: another writer appended in between. On success the version advances once,
  123 | 	 * whatever the number of events. This is the compare-and-set of the log, as
  124 | 	 * {@link SessionRepository#appendEvents} defines it.
  125 | 	 * <p>
  126 | 	 * Read {@link #getEventVersion}, then read what the new events depend on, then call
  127 | 	 * this method. On {@code false}, read again and rebuild the events. Compaction writes
  128 | 	 * its boundary and summaries this way.
  129 | 	 * @param sessionId the session whose log is appended to; every event must belong to
  130 | 	 * it
  131 | 	 * @param events the events to append, oldest first; at least one, with ids that are
  132 | 	 * new to the log
  133 | 	 * @param expectedVersion the version read before the events were prepared
  134 | 	 * @return {@code true} when the events were appended, {@code false} when the log
  135 | 	 * moved on and nothing was written
  136 | 	 * @throws IllegalArgumentException if the session does not exist, the batch is empty,
  137 | 	 * an event belongs to another session, an id is already in the log or appears twice,
  138 | 	 * or an event holds a system message that {@link #appendEvent} would reject. The
  139 | 	 * system message check runs before the version check. The other checks may run only
  140 | 	 * when the version matches, so a stale version can return {@code false} for a batch
  141 | 	 * that would otherwise throw.
  142 | 	 */
  143 | 	boolean appendEvents(String sessionId, List<SessionEvent> events, long expectedVersion);
  144 | 
  145 | 	/**
  146 | 	 * The version of the session's log. It advances on every append of a new event and
  147 | 	 * once per successful {@link #appendEvents}. An {@link #appendEvent} of an id the log
  148 | 	 * already holds is a no-op and does not advance it. {@code 0} for a session without
  149 | 	 * events, or one that does not exist. Read it before the events that a
  150 | 	 * version-checked {@link #appendEvents} depends on.
  151 | 	 */
  152 | 	long getEventVersion(String sessionId);
  153 | 
  154 | 	/** The events the filter selects, oldest first; see {@link EventFilter}. */
  155 | 	List<SessionEvent> getEvents(String sessionId, EventFilter filter);
  156 | 
  157 | 	/** Every event of the log, oldest first. */
  158 | 	default List<SessionEvent> getEvents(String sessionId) {
  159 | 		return getEvents(sessionId, EventFilter.all());
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * The messages of the whole log, the events a compaction covered and the summaries
  164 | 	 * included: the full history, not a prompt. Use {@link #getActiveMessages(String)}
  165 | 	 * for the prompt.
  166 | 	 */
  167 | 	default List<Message> getMessages(String sessionId) {
  168 | 		return getEvents(sessionId).stream().filter(SessionEvent::hasMessage).map(SessionEvent::getMessage).toList();
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * The messages of the active view, the context window to send to the model: {@code
  173 | 	 * getEvents(sessionId, EventFilter.active())} as messages.
  174 | 	 */
  175 | 	default List<Message> getActiveMessages(String sessionId) {
  176 | 		return getEvents(sessionId, EventFilter.active()).stream().map(SessionEvent::getMessage).toList();
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * The message events of the session the query finds, oldest first, a page at a time:
  181 | 	 * a keyword search for recall. It covers the whole log, so it finds what a compaction
  182 | 	 * no longer sends to the model. See {@link EventQuery}. The default reads the log and
  183 | 	 * matches in memory; {@link DefaultSessionService} leaves the search to the
  184 | 	 * repository.
  185 | 	 */
  186 | 	default List<SessionEvent> searchEvents(String sessionId, EventQuery query) {
  187 | 		Assert.hasText(sessionId, "sessionId must not be null or empty");
  188 | 		Assert.notNull(query, "query must not be null");
  189 | 		return query.apply(getEvents(sessionId, EventFilter.all()));
  190 | 	}
  191 | 
  192 | 	// Compaction
  193 | 
  194 | 	/**
  195 | 	 * Evaluates the trigger on the active view and, when it fires, runs the strategy and
  196 | 	 * records its result: one {@link EventType#COMPACTION} boundary event followed by the
  197 | 	 * summary events, in one version-checked {@link #appendEvents}. The log is never
  198 | 	 * changed; the boundary defines the view from then on. An append that lands in
  199 | 	 * between does not waste the summary: the new events join the view verbatim and the
  200 | 	 * write is retried, a bounded number of times. Pass {@code request -> true} to
  201 | 	 * compact unconditionally.
  202 | 	 * @return the result as written, the events appended meanwhile added to its preserved
  203 | 	 * tail;
  204 | 	 * {@link org.springframework.ai.session.compaction.CompactionResult#isUnchanged()}
  205 | 	 * when nothing was written: the trigger did not fire, the strategy covered nothing,
  206 | 	 * another compaction landed first, or appends kept overtaking the write
  207 | 	 */
  208 | 	CompactionResult compact(String sessionId, CompactionTrigger trigger, CompactionStrategy strategy);
  209 | 
  210 | }
```
