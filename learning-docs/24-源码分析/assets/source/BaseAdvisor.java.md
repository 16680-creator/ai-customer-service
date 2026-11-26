<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/api/BaseAdvisor.java -->
<!-- 正文引用：[[源码:BaseAdvisor.java#L起-L止]] ；代码块头部：(src: BaseAdvisor.java#L起-L止) -->

# BaseAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/api/BaseAdvisor.java` |
| 行数 | 98 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.096Z |

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
   17 | package org.springframework.ai.chat.client.advisor.api;
   18 | 
   19 | import reactor.core.publisher.Flux;
   20 | import reactor.core.publisher.Mono;
   21 | import reactor.core.scheduler.Scheduler;
   22 | import reactor.core.scheduler.Schedulers;
   23 | 
   24 | import org.springframework.ai.chat.client.ChatClientRequest;
   25 | import org.springframework.ai.chat.client.ChatClientResponse;
   26 | import org.springframework.ai.chat.client.advisor.AdvisorUtils;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * Base advisor that implements common aspects of the {@link CallAdvisor} and
   31 |  * {@link StreamAdvisor}, reducing the boilerplate code needed to implement an advisor.
   32 |  * <p>
   33 |  * It provides default implementations for the
   34 |  * {@link #adviseCall(ChatClientRequest, CallAdvisorChain)} and
   35 |  * {@link #adviseStream(ChatClientRequest, StreamAdvisorChain)} methods, delegating the
   36 |  * actual logic to the {@link #before(ChatClientRequest, AdvisorChain advisorChain)} and
   37 |  * {@link #after(ChatClientResponse, AdvisorChain advisorChain)} methods.
   38 |  *
   39 |  * @author Thomas Vitale
   40 |  * @since 1.0.0
   41 |  */
   42 | public interface BaseAdvisor extends CallAdvisor, StreamAdvisor {
   43 | 
   44 | 	Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
   45 | 
   46 | 	@Override
   47 | 	default ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
   48 | 		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
   49 | 		Assert.notNull(callAdvisorChain, "callAdvisorChain cannot be null");
   50 | 
   51 | 		ChatClientRequest processedChatClientRequest = before(chatClientRequest, callAdvisorChain);
   52 | 		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(processedChatClientRequest);
   53 | 		return after(chatClientResponse, callAdvisorChain);
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	default Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
   58 | 			StreamAdvisorChain streamAdvisorChain) {
   59 | 		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
   60 | 		Assert.notNull(streamAdvisorChain, "streamAdvisorChain cannot be null");
   61 | 		Assert.notNull(getScheduler(), "scheduler cannot be null");
   62 | 
   63 | 		Flux<ChatClientResponse> chatClientResponseFlux = Mono.just(chatClientRequest)
   64 | 			.publishOn(getScheduler())
   65 | 			.map(request -> this.before(request, streamAdvisorChain))
   66 | 			.flatMapMany(streamAdvisorChain::nextStream);
   67 | 
   68 | 		return chatClientResponseFlux.map(response -> {
   69 | 			if (AdvisorUtils.onFinishReason().test(response)) {
   70 | 				response = after(response, streamAdvisorChain);
   71 | 			}
   72 | 			return response;
   73 | 		}).onErrorResume(error -> Flux.error(new IllegalStateException("Stream processing failed", error)));
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	default String getName() {
   78 | 		return this.getClass().getSimpleName();
   79 | 	}
   80 | 
   81 | 	/**
   82 | 	 * Logic to be executed before the rest of the advisor chain is called.
   83 | 	 */
   84 | 	ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain);
   85 | 
   86 | 	/**
   87 | 	 * Logic to be executed after the rest of the advisor chain is called.
   88 | 	 */
   89 | 	ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain);
   90 | 
   91 | 	/**
   92 | 	 * Scheduler used for processing the advisor logic when streaming.
   93 | 	 */
   94 | 	default Scheduler getScheduler() {
   95 | 		return DEFAULT_SCHEDULER;
   96 | 	}
   97 | 
   98 | }
```
