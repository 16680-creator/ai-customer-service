<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/ChatModelStreamAdvisor.java -->
<!-- 正文引用：[[源码:ChatModelStreamAdvisor.java#L起-L止]] ；代码块头部：(src: ChatModelStreamAdvisor.java#L起-L止) -->

# ChatModelStreamAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/ChatModelStreamAdvisor.java` |
| 行数 | 93 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.088Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | import reactor.core.publisher.Flux;
   21 | import reactor.core.scheduler.Schedulers;
   22 | 
   23 | import org.springframework.ai.chat.client.ChatClientRequest;
   24 | import org.springframework.ai.chat.client.ChatClientResponse;
   25 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   26 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   27 | import org.springframework.ai.chat.model.ChatModel;
   28 | import org.springframework.core.Ordered;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * A {@link StreamAdvisor} that uses a {@link ChatModel} to generate a streaming response.
   33 |  *
   34 |  * @author Thomas Vitale
   35 |  * @author Minju Kim
   36 |  * @since 1.0.0
   37 |  */
   38 | public final class ChatModelStreamAdvisor implements StreamAdvisor {
   39 | 
   40 | 	private final ChatModel chatModel;
   41 | 
   42 | 	private ChatModelStreamAdvisor(ChatModel chatModel) {
   43 | 		Assert.notNull(chatModel, "chatModel cannot be null");
   44 | 		this.chatModel = chatModel;
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
   49 | 			StreamAdvisorChain streamAdvisorChain) {
   50 | 		Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
   51 | 
   52 | 		return this.chatModel.stream(chatClientRequest.prompt())
   53 | 			.map(chatResponse -> ChatClientResponse.builder()
   54 | 				.chatResponse(chatResponse)
   55 | 				.context(chatClientRequest.context())
   56 | 				.build())
   57 | 			.publishOn(Schedulers.boundedElastic()); // TODO add option to disable
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	public String getName() {
   62 | 		return "stream";
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public int getOrder() {
   67 | 		return Ordered.LOWEST_PRECEDENCE;
   68 | 	}
   69 | 
   70 | 	public static Builder builder() {
   71 | 		return new Builder();
   72 | 	}
   73 | 
   74 | 	public static final class Builder {
   75 | 
   76 | 		private @Nullable ChatModel chatModel;
   77 | 
   78 | 		private Builder() {
   79 | 		}
   80 | 
   81 | 		public Builder chatModel(ChatModel chatModel) {
   82 | 			this.chatModel = chatModel;
   83 | 			return this;
   84 | 		}
   85 | 
   86 | 		public ChatModelStreamAdvisor build() {
   87 | 			Assert.state(this.chatModel != null, "chatModel cannot be null");
   88 | 			return new ChatModelStreamAdvisor(this.chatModel);
   89 | 		}
   90 | 
   91 | 	}
   92 | 
   93 | }
```
