<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session:2.1.0 | path: org/springframework/ai/session/SessionEvent.java -->
<!-- 正文引用：[[源码:SessionEvent.java#L起-L止]] ；代码块头部：(src: SessionEvent.java#L起-L止) -->

# SessionEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session:2.1.0` |
| 路径 | `org/springframework/ai/session/SessionEvent.java` |
| 行数 | 310 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session/2.1.0/spring-ai-session-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:06.286Z |

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
   20 | import java.util.HashMap;
   21 | import java.util.Map;
   22 | import java.util.Objects;
   23 | import java.util.UUID;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.chat.messages.AssistantMessage;
   28 | import org.springframework.ai.chat.messages.Message;
   29 | import org.springframework.ai.chat.messages.MessageType;
   30 | import org.springframework.util.Assert;
   31 | 
   32 | /**
   33 |  * An entry of a session's log. An event has an {@link EventType}: a
   34 |  * {@link EventType#MESSAGE} event wraps a Spring AI {@link Message} and adds what the
   35 |  * message intentionally lacks, identity, ordering and the session it belongs to; every
   36 |  * other type records runtime state, a compaction or an application's own event, in
   37 |  * {@link #getPayload()} instead of a message, and {@link #getMessage()} throws for it.
   38 |  * Message events are the conversation: they alone take part in prompt assembly and
   39 |  * compaction.
   40 |  * <p>
   41 |  * An event is never changed once appended. A compaction records what left the active view
   42 |  * in a {@link EventType#COMPACTION} event of its own, and a covered event stays in the
   43 |  * log, readable through {@link EventFilter#all()}. A {@link #isSynthetic() synthetic}
   44 |  * event is one a compaction wrote: the user message that opens a summary turn and the
   45 |  * assistant message that closes it.
   46 |  *
   47 |  * @author Christian Tzolov
   48 |  * @since 2.1.0
   49 |  */
   50 | public final class SessionEvent {
   51 | 
   52 | 	/** Metadata key of the synthetic flag, a {@link Boolean}. */
   53 | 	public static final String METADATA_SYNTHETIC = "synthetic";
   54 | 
   55 | 	/** Metadata key naming the strategy that wrote a synthetic event. */
   56 | 	public static final String METADATA_COMPACTION_SOURCE = "compactionSource";
   57 | 
   58 | 	/**
   59 | 	 * Metadata key of a synthetic event: the id of the {@link EventType#COMPACTION}
   60 | 	 * boundary it belongs to.
   61 | 	 */
   62 | 	public static final String METADATA_COMPACTION_ID = "compactionId";
   63 | 
   64 | 	private final String id;
   65 | 
   66 | 	private final String sessionId;
   67 | 
   68 | 	private final Instant timestamp;
   69 | 
   70 | 	private final EventType type;
   71 | 
   72 | 	@Nullable private final Message message;
   73 | 
   74 | 	private final Map<String, Object> payload;
   75 | 
   76 | 	private final Map<String, Object> metadata;
   77 | 
   78 | 	private SessionEvent(String id, String sessionId, Instant timestamp, EventType type, @Nullable Message message,
   79 | 			Map<String, Object> payload, Map<String, Object> metadata) {
   80 | 		this.id = id;
   81 | 		this.sessionId = sessionId;
   82 | 		this.timestamp = timestamp;
   83 | 		this.type = type;
   84 | 		this.message = message;
   85 | 		this.payload = Map.copyOf(payload);
   86 | 		this.metadata = Map.copyOf(metadata);
   87 | 	}
   88 | 
   89 | 	/** The identity of the event, unique across sessions. */
   90 | 	public String getId() {
   91 | 		return this.id;
   92 | 	}
   93 | 
   94 | 	/** The session the event belongs to. */
   95 | 	public String getSessionId() {
   96 | 		return this.sessionId;
   97 | 	}
   98 | 
   99 | 	/** When the event was recorded. A log is ordered by insertion, not by this. */
  100 | 	public Instant getTimestamp() {
  101 | 		return this.timestamp;
  102 | 	}
  103 | 
  104 | 	/** What the event records; {@link EventType#MESSAGE} for a conversation message. */
  105 | 	public EventType getType() {
  106 | 		return this.type;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Whether the event carries a message, so that {@link #getMessage()} can be called: a
  111 | 	 * {@link EventType#MESSAGE} event does; see {@link EventType#canCarryMessage()}.
  112 | 	 */
  113 | 	public boolean hasMessage() {
  114 | 		return this.message != null;
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * The wrapped Spring AI message.
  119 | 	 * @throws IllegalStateException if the event carries no message; see
  120 | 	 * {@link #hasMessage()}
  121 | 	 */
  122 | 	public Message getMessage() {
  123 | 		if (this.message == null) {
  124 | 			throw new IllegalStateException(
  125 | 					"Event '" + this.id + "' is a " + this.type + " event and carries no message");
  126 | 		}
  127 | 		return this.message;
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * The data of an event that is not a message, the ids a {@link EventType#COMPACTION}
  132 | 	 * boundary names for example; empty for a message event. Values must be
  133 | 	 * JSON-serializable.
  134 | 	 */
  135 | 	public Map<String, Object> getPayload() {
  136 | 		return this.payload;
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Framework flags such as {@link #METADATA_SYNTHETIC} and
  141 | 	 * {@link #METADATA_COMPACTION_SOURCE}, and whatever the application adds.
  142 | 	 */
  143 | 	public Map<String, Object> getMetadata() {
  144 | 		return this.metadata;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * The type of the wrapped message.
  149 | 	 * @throws IllegalStateException if the event carries no message; see
  150 | 	 * {@link #hasMessage()}
  151 | 	 */
  152 | 	public MessageType getMessageType() {
  153 | 		return getMessage().getMessageType();
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Whether a compaction wrote this event, a summary rather than something a
  158 | 	 * participant said.
  159 | 	 */
  160 | 	public boolean isSynthetic() {
  161 | 		return Boolean.parseBoolean(String.valueOf(this.metadata.get(METADATA_SYNTHETIC)));
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Whether the event starts a turn: a {@link MessageType#USER} message event. A turn
  166 | 	 * is a user message plus everything up to the next one, the assistant replies, the
  167 | 	 * tool calls and their results, so it is the unit no read or compaction cut may
  168 | 	 * split. A synthetic shadow prompt is a user message too, so a summary turn is never
  169 | 	 * separated from its summary. An event that is not a {@link EventType#MESSAGE} never
  170 | 	 * starts a turn.
  171 | 	 */
  172 | 	public boolean isTurnStart() {
  173 | 		return EventType.MESSAGE.equals(this.type) && this.message != null
  174 | 				&& this.message.getMessageType() == MessageType.USER;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Whether the event is an assistant message with tool calls; {@code false} for an
  179 | 	 * event without a message.
  180 | 	 */
  181 | 	public boolean hasToolCalls() {
  182 | 		return this.message instanceof AssistantMessage am && am.hasToolCalls();
  183 | 	}
  184 | 
  185 | 	@Override
  186 | 	public boolean equals(Object obj) {
  187 | 		if (this == obj) {
  188 | 			return true;
  189 | 		}
  190 | 		if (obj == null || getClass() != obj.getClass()) {
  191 | 			return false;
  192 | 		}
  193 | 		SessionEvent other = (SessionEvent) obj;
  194 | 		return this.id.equals(other.id) && this.sessionId.equals(other.sessionId);
  195 | 	}
  196 | 
  197 | 	@Override
  198 | 	public int hashCode() {
  199 | 		return Objects.hash(this.id, this.sessionId);
  200 | 	}
  201 | 
  202 | 	/** Returns a new {@link Builder}. */
  203 | 	public static Builder builder() {
  204 | 		return new Builder();
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Builds a {@link SessionEvent}. The id is a random UUID, the timestamp
  209 | 	 * {@link Instant#now()}, the payload and the metadata empty, unless set; the session
  210 | 	 * id is required. The type is {@link EventType#MESSAGE} when a message is set, which
  211 | 	 * that type requires and no other type allows.
  212 | 	 */
  213 | 	public static final class Builder {
  214 | 
  215 | 		private String id = UUID.randomUUID().toString();
  216 | 
  217 | 		private String sessionId = "";
  218 | 
  219 | 		private Instant timestamp = Instant.now();
  220 | 
  221 | 		@Nullable private EventType type;
  222 | 
  223 | 		@Nullable private Message message;
  224 | 
  225 | 		private Map<String, Object> payload = new HashMap<>();
  226 | 
  227 | 		private Map<String, Object> metadata = new HashMap<>();
  228 | 
  229 | 		private Builder() {
  230 | 		}
  231 | 
  232 | 		/** The event id; a random UUID by default. */
  233 | 		public Builder id(String id) {
  234 | 			this.id = id;
  235 | 			return this;
  236 | 		}
  237 | 
  238 | 		/** The session the event belongs to; required. */
  239 | 		public Builder sessionId(String sessionId) {
  240 | 			this.sessionId = sessionId;
  241 | 			return this;
  242 | 		}
  243 | 
  244 | 		/** When the event was recorded; {@link Instant#now()} by default. */
  245 | 		public Builder timestamp(Instant timestamp) {
  246 | 			this.timestamp = timestamp;
  247 | 			return this;
  248 | 		}
  249 | 
  250 | 		/**
  251 | 		 * What the event records; {@link EventType#MESSAGE} by default when a message is
  252 | 		 * set.
  253 | 		 */
  254 | 		public Builder type(EventType type) {
  255 | 			this.type = type;
  256 | 			return this;
  257 | 		}
  258 | 
  259 | 		/**
  260 | 		 * The message a {@link EventType#MESSAGE} event wraps; required for that type,
  261 | 		 * not allowed for any other.
  262 | 		 */
  263 | 		public Builder message(Message message) {
  264 | 			this.message = message;
  265 | 			return this;
  266 | 		}
  267 | 
  268 | 		/** Replaces the payload of an event that is not a message. */
  269 | 		public Builder payload(Map<String, Object> payload) {
  270 | 			this.payload = new HashMap<>(payload);
  271 | 			return this;
  272 | 		}
  273 | 
  274 | 		/** Adds one payload entry. */
  275 | 		public Builder payload(String key, Object value) {
  276 | 			this.payload.put(key, value);
  277 | 			return this;
  278 | 		}
  279 | 
  280 | 		/** Replaces the metadata. */
  281 | 		public Builder metadata(Map<String, Object> metadata) {
  282 | 			this.metadata = new HashMap<>(metadata);
  283 | 			return this;
  284 | 		}
  285 | 
  286 | 		/** Adds one metadata entry. */
  287 | 		public Builder metadata(String key, Object value) {
  288 | 			this.metadata.put(key, value);
  289 | 			return this;
  290 | 		}
  291 | 
  292 | 		/** Builds the event, checking that the type and the message agree. */
  293 | 		public SessionEvent build() {
  294 | 			Assert.hasText(this.id, "id must not be null or empty");
  295 | 			Assert.hasText(this.sessionId, "sessionId must not be null or empty");
  296 | 			Assert.notNull(this.timestamp, "timestamp must not be null");
  297 | 			EventType resolvedType = (this.type != null) ? this.type : EventType.MESSAGE;
  298 | 			if (EventType.MESSAGE.equals(resolvedType)) {
  299 | 				Assert.notNull(this.message, "message must not be null for a MESSAGE event");
  300 | 			}
  301 | 			else if (!resolvedType.canCarryMessage()) {
  302 | 				Assert.isNull(this.message, "a " + resolvedType + " event must not carry a message");
  303 | 			}
  304 | 			return new SessionEvent(this.id, this.sessionId, this.timestamp, resolvedType, this.message, this.payload,
  305 | 					this.metadata);
  306 | 		}
  307 | 
  308 | 	}
  309 | 
  310 | }
```
