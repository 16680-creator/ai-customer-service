<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session:2.1.0 | path: org/springframework/ai/session/Session.java -->
<!-- 正文引用：[[源码:Session.java#L起-L止]] ；代码块头部：(src: Session.java#L起-L止) -->

# Session.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session:2.1.0` |
| 路径 | `org/springframework/ai/session/Session.java` |
| 行数 | 136 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session/2.1.0/spring-ai-session-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:06.086Z |

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
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * One continuous conversation between a user and an agent: its identity, its expiry and
   29 |  * its {@link #metadata() metadata}. An immutable value; the events are stored separately
   30 |  * in the {@link SessionRepository} and read through the {@link SessionService}.
   31 |  * <p>
   32 |  * A session has no owner field. Who it belongs to is an attribute of its metadata, under
   33 |  * a well-known key such as {@link SessionAttributes#PRINCIPAL_NAME}, set by the
   34 |  * application and looked up with {@link SessionRepository#findByMetadata}.
   35 |  *
   36 |  * @author Christian Tzolov
   37 |  * @since 2.1.0
   38 |  */
   39 | public final class Session {
   40 | 
   41 | 	private final String id;
   42 | 
   43 | 	private final Instant createdAt;
   44 | 
   45 | 	@Nullable private final Instant expiresAt;
   46 | 
   47 | 	private final Map<String, Object> metadata;
   48 | 
   49 | 	private Session(String id, Instant createdAt, @Nullable Instant expiresAt, Map<String, Object> metadata) {
   50 | 		this.id = id;
   51 | 		this.createdAt = createdAt;
   52 | 		this.expiresAt = expiresAt;
   53 | 		this.metadata = Map.copyOf(metadata);
   54 | 	}
   55 | 
   56 | 	/** The session's identifier. */
   57 | 	public String id() {
   58 | 		return this.id;
   59 | 	}
   60 | 
   61 | 	/** When the session was created. */
   62 | 	public Instant createdAt() {
   63 | 		return this.createdAt;
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * When the session expires, or {@code null} for never. A session the service creates
   68 | 	 * always has an expiry, the request's time to live or the service's default; a
   69 | 	 * session built directly has none unless {@link Builder#expiresAt} sets one.
   70 | 	 */
   71 | 	@Nullable public Instant expiresAt() {
   72 | 		return this.expiresAt;
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * The metadata: the owner and agent attributes ({@link SessionAttributes}), tags, and
   77 | 	 * whatever the application adds.
   78 | 	 */
   79 | 	public Map<String, Object> metadata() {
   80 | 		return this.metadata;
   81 | 	}
   82 | 
   83 | 	public static Builder builder() {
   84 | 		return new Builder();
   85 | 	}
   86 | 
   87 | 	public static final class Builder {
   88 | 
   89 | 		private String id = "";
   90 | 
   91 | 		private Instant createdAt = Instant.now();
   92 | 
   93 | 		@Nullable private Instant expiresAt;
   94 | 
   95 | 		private Map<String, Object> metadata = new HashMap<>();
   96 | 
   97 | 		public Builder id(String id) {
   98 | 			this.id = id;
   99 | 			return this;
  100 | 		}
  101 | 
  102 | 		public Builder createdAt(Instant createdAt) {
  103 | 			this.createdAt = createdAt;
  104 | 			return this;
  105 | 		}
  106 | 
  107 | 		/**
  108 | 		 * Sets the expiry, or {@code null} for a session that never expires, the default.
  109 | 		 * The time to live is the service's policy, see
  110 | 		 * {@link DefaultSessionService.Builder#defaultTimeToLive}; a session is a value.
  111 | 		 */
  112 | 		public Builder expiresAt(@Nullable Instant expiresAt) {
  113 | 			this.expiresAt = expiresAt;
  114 | 			return this;
  115 | 		}
  116 | 
  117 | 		/** Adds every entry of the map, keeping the ones already set. */
  118 | 		public Builder metadata(Map<String, Object> metadata) {
  119 | 			this.metadata.putAll(metadata);
  120 | 			return this;
  121 | 		}
  122 | 
  123 | 		/** Adds one metadata entry. */
  124 | 		public Builder metadata(String key, Object value) {
  125 | 			this.metadata.put(key, value);
  126 | 			return this;
  127 | 		}
  128 | 
  129 | 		public Session build() {
  130 | 			Assert.hasText(this.id, "id must not be null or empty");
  131 | 			return new Session(this.id, this.createdAt, this.expiresAt, this.metadata);
  132 | 		}
  133 | 
  134 | 	}
  135 | 
  136 | }
```
