<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java -->
<!-- 正文引用：[[源码:MessageChatMemoryAdvisor.java#L起-L止]] ；代码块头部：(src: MessageChatMemoryAdvisor.java#L起-L止) -->

# MessageChatMemoryAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java` |
| 行数 | 211 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.417Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import reactor.core.publisher.Flux;
   23 | import reactor.core.publisher.Mono;
   24 | import reactor.core.scheduler.Scheduler;
   25 | 
   26 | import org.springframework.ai.chat.client.ChatClientMessageAggregator;
   27 | import org.springframework.ai.chat.client.ChatClientRequest;
   28 | import org.springframework.ai.chat.client.ChatClientResponse;
   29 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   30 | import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
   31 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   32 | import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
   33 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   34 | import org.springframework.ai.chat.memory.ChatMemory;
   35 | import org.springframework.ai.chat.messages.Message;
   36 | import org.springframework.ai.chat.messages.SystemMessage;
   37 | import org.springframework.util.Assert;
   38 | 
   39 | /**
   40 |  * Memory is retrieved added as a collection of messages to the prompt
   41 |  *
   42 |  * @author Christian Tzolov
   43 |  * @author Mark Pollack
   44 |  * @author Thomas Vitale
   45 |  * @since 1.0.0
   46 |  * @deprecated since 2.1.0, for removal in 3.0.0, in favor of {@code SessionAdvisors} and
   47 |  * {@code SessionMemoryAdvisor} in {@code spring-ai-session-advisor}
   48 |  */
   49 | @Deprecated(since = "2.1.0", forRemoval = true)
   50 | @SuppressWarnings("removal")
   51 | public final class MessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {
   52 | 
   53 | 	private final ChatMemory chatMemory;
   54 | 
   55 | 	private final int order;
   56 | 
   57 | 	private final Scheduler scheduler;
   58 | 
   59 | 	private MessageChatMemoryAdvisor(ChatMemory chatMemory, int order, Scheduler scheduler) {
   60 | 		Assert.notNull(chatMemory, "chatMemory cannot be null");
   61 | 		Assert.notNull(scheduler, "scheduler cannot be null");
   62 | 		this.chatMemory = chatMemory;
   63 | 		this.order = order;
   64 | 		this.scheduler = scheduler;
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	public int getOrder() {
   69 | 		return this.order;
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public Scheduler getScheduler() {
   74 | 		return this.scheduler;
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
   79 | 		String conversationId = getConversationId(chatClientRequest.context());
   80 | 
   81 | 		// 1. Retrieve the chat memory for the current conversation.
   82 | 		List<Message> memoryMessages = this.chatMemory.get(conversationId);
   83 | 
   84 | 		// 2. Advise the request messages list.
   85 | 		List<Message> promptMessages = chatClientRequest.prompt().getInstructions();
   86 | 		List<Message> processedMessages = new ArrayList<>();
   87 | 		if (!isMemoryAlreadyInPrompt(promptMessages, memoryMessages)) {
   88 | 			processedMessages.addAll(memoryMessages);
   89 | 		}
   90 | 		processedMessages.addAll(promptMessages);
   91 | 
   92 | 		// 2.1. Ensure system message, if present, appears first in the list.
   93 | 		for (int i = 0; i < processedMessages.size(); i++) {
   94 | 			if (processedMessages.get(i) instanceof SystemMessage) {
   95 | 				Message systemMessage = processedMessages.remove(i);
   96 | 				processedMessages.add(0, systemMessage);
   97 | 				break;
   98 | 			}
   99 | 		}
  100 | 
  101 | 		// 3. Create a new request with the advised messages.
  102 | 		ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
  103 | 			.prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
  104 | 			.build();
  105 | 
  106 | 		// 4. Add the new user message to the conversation memory.
  107 | 		Message userMessage = processedChatClientRequest.prompt().getLastUserOrToolResponseMessage();
  108 | 		this.chatMemory.add(conversationId, userMessage);
  109 | 
  110 | 		return processedChatClientRequest;
  111 | 	}
  112 | 
  113 | 	private static boolean isMemoryAlreadyInPrompt(List<Message> promptMessages, List<Message> memoryMessages) {
  114 | 		if (memoryMessages.isEmpty()) {
  115 | 			return true;
  116 | 		}
  117 | 		if (promptMessages.size() < memoryMessages.size()) {
  118 | 			return false;
  119 | 		}
  120 | 		for (int offset = 0; offset <= promptMessages.size() - memoryMessages.size(); offset++) {
  121 | 			if (startsWith(promptMessages, memoryMessages, offset)) {
  122 | 				return true;
  123 | 			}
  124 | 		}
  125 | 		return false;
  126 | 	}
  127 | 
  128 | 	private static boolean startsWith(List<Message> messages, List<Message> prefix, int offset) {
  129 | 		if (messages.size() - offset < prefix.size()) {
  130 | 			return false;
  131 | 		}
  132 | 		for (int i = 0; i < prefix.size(); i++) {
  133 | 			if (!messages.get(i + offset).equals(prefix.get(i))) {
  134 | 				return false;
  135 | 			}
  136 | 		}
  137 | 		return true;
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
  142 | 		List<Message> assistantMessages = new ArrayList<>();
  143 | 		if (chatClientResponse.chatResponse() != null) {
  144 | 			assistantMessages = chatClientResponse.chatResponse()
  145 | 				.getResults()
  146 | 				.stream()
  147 | 				.map(g -> (Message) g.getOutput())
  148 | 				.toList();
  149 | 		}
  150 | 		this.chatMemory.add(this.getConversationId(chatClientResponse.context()), assistantMessages);
  151 | 		return chatClientResponse;
  152 | 	}
  153 | 
  154 | 	@Override
  155 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
  156 | 			StreamAdvisorChain streamAdvisorChain) {
  157 | 		// Get the scheduler from BaseAdvisor
  158 | 		Scheduler scheduler = this.getScheduler();
  159 | 
  160 | 		// Process the request with the before method
  161 | 		return Mono.just(chatClientRequest)
  162 | 			.publishOn(scheduler)
  163 | 			.map(request -> this.before(request, streamAdvisorChain))
  164 | 			.flatMapMany(streamAdvisorChain::nextStream)
  165 | 			.transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
  166 | 					response -> this.after(response, streamAdvisorChain)));
  167 | 	}
  168 | 
  169 | 	public static Builder builder(ChatMemory chatMemory) {
  170 | 		return new Builder(chatMemory);
  171 | 	}
  172 | 
  173 | 	public static final class Builder {
  174 | 
  175 | 		private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
  176 | 
  177 | 		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
  178 | 
  179 | 		private final ChatMemory chatMemory;
  180 | 
  181 | 		private Builder(ChatMemory chatMemory) {
  182 | 			Assert.notNull(chatMemory, "chatMemory cannot be null");
  183 | 			this.chatMemory = chatMemory;
  184 | 		}
  185 | 
  186 | 		/**
  187 | 		 * Set the order.
  188 | 		 * @param order the order
  189 | 		 * @return the builder
  190 | 		 */
  191 | 		public Builder order(int order) {
  192 | 			this.order = order;
  193 | 			return this;
  194 | 		}
  195 | 
  196 | 		public Builder scheduler(Scheduler scheduler) {
  197 | 			this.scheduler = scheduler;
  198 | 			return this;
  199 | 		}
  200 | 
  201 | 		/**
  202 | 		 * Build the advisor.
  203 | 		 * @return the advisor
  204 | 		 */
  205 | 		public MessageChatMemoryAdvisor build() {
  206 | 			return new MessageChatMemoryAdvisor(this.chatMemory, this.order, this.scheduler);
  207 | 		}
  208 | 
  209 | 	}
  210 | 
  211 | }
```
