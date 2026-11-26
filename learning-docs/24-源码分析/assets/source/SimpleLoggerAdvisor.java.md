<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/SimpleLoggerAdvisor.java -->
<!-- 正文引用：[[源码:SimpleLoggerAdvisor.java#L起-L止]] ；代码块头部：(src: SimpleLoggerAdvisor.java#L起-L止) -->

# SimpleLoggerAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/SimpleLoggerAdvisor.java` |
| 行数 | 156 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.588Z |

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
   19 | import java.util.function.Function;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | import reactor.core.publisher.Flux;
   25 | 
   26 | import org.springframework.ai.chat.client.ChatClientMessageAggregator;
   27 | import org.springframework.ai.chat.client.ChatClientRequest;
   28 | import org.springframework.ai.chat.client.ChatClientResponse;
   29 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   30 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   31 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   32 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   33 | import org.springframework.ai.chat.model.ChatResponse;
   34 | import org.springframework.ai.util.JacksonUtils;
   35 | 
   36 | /**
   37 |  * A simple logger advisor that logs the request and response messages.
   38 |  *
   39 |  * @author Christian Tzolov
   40 |  */
   41 | public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
   42 | 
   43 | 	public static final Function<@Nullable ChatClientRequest, String> DEFAULT_REQUEST_TO_STRING = chatClientRequest -> chatClientRequest != null
   44 | 			? chatClientRequest.toString() : "null";
   45 | 
   46 | 	public static final Function<@Nullable ChatResponse, String> DEFAULT_RESPONSE_TO_STRING = object -> object != null
   47 | 			? JacksonUtils.getDefaultJsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object) : "null";
   48 | 
   49 | 	private static final Log logger = LogFactory.getLog(SimpleLoggerAdvisor.class);
   50 | 
   51 | 	private final Function<@Nullable ChatClientRequest, String> requestToString;
   52 | 
   53 | 	private final Function<@Nullable ChatResponse, String> responseToString;
   54 | 
   55 | 	private final int order;
   56 | 
   57 | 	public SimpleLoggerAdvisor() {
   58 | 		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, 0);
   59 | 	}
   60 | 
   61 | 	public SimpleLoggerAdvisor(int order) {
   62 | 		this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, order);
   63 | 	}
   64 | 
   65 | 	public SimpleLoggerAdvisor(@Nullable Function<@Nullable ChatClientRequest, String> requestToString,
   66 | 			@Nullable Function<@Nullable ChatResponse, String> responseToString, int order) {
   67 | 		this.requestToString = requestToString != null ? requestToString : DEFAULT_REQUEST_TO_STRING;
   68 | 		this.responseToString = responseToString != null ? responseToString : DEFAULT_RESPONSE_TO_STRING;
   69 | 		this.order = order;
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
   74 | 		logRequest(chatClientRequest);
   75 | 
   76 | 		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
   77 | 
   78 | 		logResponse(chatClientResponse);
   79 | 
   80 | 		return chatClientResponse;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
   85 | 			StreamAdvisorChain streamAdvisorChain) {
   86 | 		logRequest(chatClientRequest);
   87 | 
   88 | 		Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
   89 | 
   90 | 		return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
   91 | 	}
   92 | 
   93 | 	protected void logRequest(ChatClientRequest request) {
   94 | 		if (logger.isDebugEnabled()) {
   95 | 			logger.debug("request: " + this.requestToString.apply(request));
   96 | 		}
   97 | 	}
   98 | 
   99 | 	protected void logResponse(ChatClientResponse chatClientResponse) {
  100 | 		if (logger.isDebugEnabled()) {
  101 | 			logger.debug("response: " + this.responseToString.apply(chatClientResponse.chatResponse()));
  102 | 		}
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public String getName() {
  107 | 		return this.getClass().getSimpleName();
  108 | 	}
  109 | 
  110 | 	@Override
  111 | 	public int getOrder() {
  112 | 		return this.order;
  113 | 	}
  114 | 
  115 | 	@Override
  116 | 	public String toString() {
  117 | 		return SimpleLoggerAdvisor.class.getSimpleName();
  118 | 	}
  119 | 
  120 | 	public static Builder builder() {
  121 | 		return new Builder();
  122 | 	}
  123 | 
  124 | 	public static final class Builder {
  125 | 
  126 | 		private @Nullable Function<@Nullable ChatClientRequest, String> requestToString;
  127 | 
  128 | 		private @Nullable Function<@Nullable ChatResponse, String> responseToString;
  129 | 
  130 | 		private int order = 0;
  131 | 
  132 | 		private Builder() {
  133 | 		}
  134 | 
  135 | 		public Builder requestToString(Function<@Nullable ChatClientRequest, String> requestToString) {
  136 | 			this.requestToString = requestToString;
  137 | 			return this;
  138 | 		}
  139 | 
  140 | 		public Builder responseToString(Function<@Nullable ChatResponse, String> responseToString) {
  141 | 			this.responseToString = responseToString;
  142 | 			return this;
  143 | 		}
  144 | 
  145 | 		public Builder order(int order) {
  146 | 			this.order = order;
  147 | 			return this;
  148 | 		}
  149 | 
  150 | 		public SimpleLoggerAdvisor build() {
  151 | 			return new SimpleLoggerAdvisor(this.requestToString, this.responseToString, this.order);
  152 | 		}
  153 | 
  154 | 	}
  155 | 
  156 | }
```
