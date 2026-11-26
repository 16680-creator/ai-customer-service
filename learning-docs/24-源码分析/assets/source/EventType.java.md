<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session:2.1.0 | path: org/springframework/ai/session/EventType.java -->
<!-- 正文引用：[[源码:EventType.java#L起-L止]] ；代码块头部：(src: EventType.java#L起-L止) -->

# EventType.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session:2.1.0` |
| 路径 | `org/springframework/ai/session/EventType.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session/2.1.0/spring-ai-session-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:06.761Z |

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
   19 | import java.util.Set;
   20 | import java.util.regex.Pattern;
   21 | 
   22 | import com.fasterxml.jackson.annotation.JsonCreator;
   23 | import com.fasterxml.jackson.annotation.JsonValue;
   24 | 
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * What a {@link SessionEvent} records. A type is its name: the well-known types are
   29 |  * constants here, and an application or an agent runtime defines its own with
   30 |  * {@link #of(String)}; every store keeps the name, and a filter by type works for any of
   31 |  * them. Only a {@link #MESSAGE} event takes part in prompt assembly and compaction, and
   32 |  * only a type that {@link #canCarryMessage() carries a message} wraps a Spring AI
   33 |  * message.
   34 |  *
   35 |  * @author Christian Tzolov
   36 |  * @since 2.1.0
   37 |  */
   38 | public final class EventType {
   39 | 
   40 | 	/** The longest name; a store sizes its column for it. */
   41 | 	public static final int MAX_NAME_LENGTH = 64;
   42 | 
   43 | 	private static final Pattern NAME = Pattern.compile("[A-Z][A-Z0-9_.]*");
   44 | 
   45 | 	/**
   46 | 	 * A conversation message; the event carries a
   47 | 	 * {@link org.springframework.ai.chat.messages.Message}.
   48 | 	 */
   49 | 	public static final EventType MESSAGE = of("MESSAGE");
   50 | 
   51 | 	/**
   52 | 	 * A compaction boundary: the payload names, by id, the events that make up the active
   53 | 	 * view from here on; see
   54 | 	 * {@link org.springframework.ai.session.compaction.CompactionBoundary}.
   55 | 	 */
   56 | 	public static final EventType COMPACTION = of("COMPACTION");
   57 | 
   58 | 	private static final Set<EventType> MESSAGE_BEARING = Set.of(MESSAGE);
   59 | 
   60 | 	private final String name;
   61 | 
   62 | 	private EventType(String name) {
   63 | 		this.name = name;
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * The event type with the given name: a well-known one, or an application's own. A
   68 | 	 * name is upper case, digits, underscores and dots, like {@code MY_APP.PROGRESS}, of
   69 | 	 * at most {@value #MAX_NAME_LENGTH} characters.
   70 | 	 * @throws IllegalArgumentException if the name is not of that form
   71 | 	 */
   72 | 	@JsonCreator
   73 | 	public static EventType of(String name) {
   74 | 		Assert.hasText(name, "name must not be null or empty");
   75 | 		Assert.isTrue(NAME.matcher(name).matches(),
   76 | 				() -> "Event type name '" + name + "' must match " + NAME.pattern());
   77 | 		Assert.isTrue(name.length() <= MAX_NAME_LENGTH,
   78 | 				() -> "Event type name '" + name + "' is longer than " + MAX_NAME_LENGTH + " characters");
   79 | 		return new EventType(name);
   80 | 	}
   81 | 
   82 | 	/** The name, as stores keep it and as JSON carries it. */
   83 | 	@JsonValue
   84 | 	public String name() {
   85 | 		return this.name;
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Whether an event of this type carries a Spring AI message: a {@link #MESSAGE} event
   90 | 	 * does.
   91 | 	 */
   92 | 	public boolean canCarryMessage() {
   93 | 		return MESSAGE_BEARING.contains(this);
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public boolean equals(Object other) {
   98 | 		return this == other || (other instanceof EventType type && this.name.equals(type.name));
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public int hashCode() {
  103 | 		return this.name.hashCode();
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public String toString() {
  108 | 		return this.name;
  109 | 	}
  110 | 
  111 | }
```
