<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/AdvisorUtils.java -->
<!-- 正文引用：[[源码:AdvisorUtils.java#L起-L止]] ；代码块头部：(src: AdvisorUtils.java#L起-L止) -->

# AdvisorUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/AdvisorUtils.java` |
| 行数 | 51 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.443Z |

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
   19 | import java.util.function.Predicate;
   20 | 
   21 | import org.springframework.ai.chat.client.ChatClientResponse;
   22 | import org.springframework.ai.chat.model.ChatResponse;
   23 | import org.springframework.util.StringUtils;
   24 | 
   25 | /**
   26 |  * Utilities to work with advisors.
   27 |  *
   28 |  * @author Christian Tzolov
   29 |  */
   30 | public final class AdvisorUtils {
   31 | 
   32 | 	private AdvisorUtils() {
   33 | 	}
   34 | 
   35 | 	/**
   36 | 	 * Checks whether the provided {@link ChatClientResponse} contains a
   37 | 	 * {@link ChatResponse} with at least one result having a non-empty finish reason in
   38 | 	 * its metadata.
   39 | 	 */
   40 | 	public static Predicate<ChatClientResponse> onFinishReason() {
   41 | 		return chatClientResponse -> {
   42 | 			ChatResponse chatResponse = chatClientResponse.chatResponse();
   43 | 			return chatResponse != null && chatResponse.getResults() != null
   44 | 					&& chatResponse.getResults()
   45 | 						.stream()
   46 | 						.anyMatch(result -> result != null && result.getMetadata() != null
   47 | 								&& StringUtils.hasText(result.getMetadata().getFinishReason()));
   48 | 		};
   49 | 	}
   50 | 
   51 | }
```
