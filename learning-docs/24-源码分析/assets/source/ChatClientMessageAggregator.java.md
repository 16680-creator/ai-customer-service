<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/ChatClientMessageAggregator.java -->
<!-- 正文引用：[[源码:ChatClientMessageAggregator.java#L起-L止]] ；代码块头部：(src: ChatClientMessageAggregator.java#L起-L止) -->

# ChatClientMessageAggregator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/ChatClientMessageAggregator.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.521Z |

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
   17 | package org.springframework.ai.chat.client;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | import java.util.concurrent.atomic.AtomicReference;
   22 | import java.util.function.Consumer;
   23 | 
   24 | import reactor.core.publisher.Flux;
   25 | 
   26 | import org.springframework.ai.chat.model.MessageAggregator;
   27 | 
   28 | /**
   29 |  * Helper that for streaming chat responses, aggregate the chat response messages into a
   30 |  * single AssistantMessage. Job is performed in parallel to the chat response processing.
   31 |  *
   32 |  * @author Christian Tzolov
   33 |  * @author Alexandros Pappas
   34 |  * @author Thomas Vitale
   35 |  * @since 1.0.0
   36 |  */
   37 | public class ChatClientMessageAggregator {
   38 | 
   39 | 	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1350
   40 | 	public Flux<ChatClientResponse> aggregateChatClientResponse(Flux<ChatClientResponse> chatClientResponses,
   41 | 			Consumer<ChatClientResponse> aggregationHandler) {
   42 | 
   43 | 		AtomicReference<Map<String, Object>> context = new AtomicReference<>(new HashMap<>());
   44 | 
   45 | 		return new MessageAggregator().aggregate(chatClientResponses.mapNotNull(chatClientResponse -> {
   46 | 			context.get().putAll(chatClientResponse.context());
   47 | 			return chatClientResponse.chatResponse();
   48 | 		}), aggregatedChatResponse -> {
   49 | 			ChatClientResponse aggregatedChatClientResponse = ChatClientResponse.builder()
   50 | 				.chatResponse(aggregatedChatResponse)
   51 | 				.context(context.get())
   52 | 				.build();
   53 | 			aggregationHandler.accept(aggregatedChatClientResponse);
   54 | 		}).map(chatResponse -> ChatClientResponse.builder().chatResponse(chatResponse).context(context.get()).build());
   55 | 	}
   56 | 
   57 | }
```
