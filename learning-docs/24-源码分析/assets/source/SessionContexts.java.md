<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session-advisor:2.1.0 | path: org/springframework/ai/session/advisor/SessionContexts.java -->
<!-- 正文引用：[[源码:SessionContexts.java#L起-L止]] ；代码块头部：(src: SessionContexts.java#L起-L止) -->

# SessionContexts.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session-advisor:2.1.0` |
| 路径 | `org/springframework/ai/session/advisor/SessionContexts.java` |
| 行数 | 84 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session-advisor/2.1.0/spring-ai-session-advisor-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:58:22.201Z |

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
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.session.CreateSessionRequest;
   25 | import org.springframework.ai.session.SessionAttributes;
   26 | 
   27 | /**
   28 |  * What the session advisors read from the advisor context: the session id, and the
   29 |  * request that creates the session on first use. An advisor of your own that works on the
   30 |  * session of a call, around the session advisors or inside the tool-calling loop, reads
   31 |  * the same two here, so that it names and creates the session exactly as they do.
   32 |  *
   33 |  * @author Christian Tzolov
   34 |  * @since 2.1.0
   35 |  */
   36 | public final class SessionContexts {
   37 | 
   38 | 	private SessionContexts() {
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * The session id from {@link SessionMemoryAdvisor#SESSION_ID_CONTEXT_KEY}.
   43 | 	 * @throws IllegalStateException when it is missing, so that sessions are never shared
   44 | 	 * by accident
   45 | 	 */
   46 | 	public static String sessionId(Map<String, @Nullable Object> context) {
   47 | 		Object value = context.get(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY);
   48 | 		if (value instanceof String s && !s.isBlank()) {
   49 | 			return s;
   50 | 		}
   51 | 		throw new IllegalStateException("No session ID found in advisor context. Set SESSION_ID_CONTEXT_KEY on "
   52 | 				+ "every request: .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))");
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * The request the advisors pass to {@code SessionService.findOrCreate}: the id, and
   57 | 	 * the metadata from {@link SessionMemoryAdvisor#SESSION_METADATA_CONTEXT_KEY}, such
   58 | 	 * as the agent attributes ({@link SessionAttributes#AGENT_NAME}).
   59 | 	 * @throws IllegalArgumentException when the metadata in the context is not a map
   60 | 	 */
   61 | 	public static CreateSessionRequest createRequest(String sessionId, Map<String, @Nullable Object> context) {
   62 | 		return CreateSessionRequest.builder().id(sessionId).metadata(sessionMetadata(context)).build();
   63 | 	}
   64 | 
   65 | 	private static Map<String, Object> sessionMetadata(Map<String, @Nullable Object> context) {
   66 | 		Object value = context.get(SessionMemoryAdvisor.SESSION_METADATA_CONTEXT_KEY);
   67 | 		if (value == null) {
   68 | 			return Map.of();
   69 | 		}
   70 | 		if (!(value instanceof Map<?, ?> map)) {
   71 | 			throw new IllegalArgumentException(
   72 | 					"Advisor context value for '" + SessionMemoryAdvisor.SESSION_METADATA_CONTEXT_KEY
   73 | 							+ "' must be a Map but was " + value.getClass().getName());
   74 | 		}
   75 | 		Map<String, Object> metadata = new HashMap<>();
   76 | 		map.forEach((k, v) -> {
   77 | 			if (k != null && v != null) {
   78 | 				metadata.put(String.valueOf(k), v);
   79 | 			}
   80 | 		});
   81 | 		return metadata;
   82 | 	}
   83 | 
   84 | }
```
