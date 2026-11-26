<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/memory/MessageWindowChatMemory.java -->
<!-- 正文引用：[[源码:MessageWindowChatMemory.java#L起-L止]] ；代码块头部：(src: MessageWindowChatMemory.java#L起-L止) -->

# MessageWindowChatMemory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/memory/MessageWindowChatMemory.java` |
| 行数 | 166 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:07.245Z |

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
   17 | package org.springframework.ai.chat.memory;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.HashSet;
   21 | import java.util.List;
   22 | import java.util.Set;
   23 | 
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.ai.chat.messages.MessageType;
   26 | import org.springframework.ai.chat.messages.SystemMessage;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * A chat memory implementation that maintains a message window of a specified size,
   31 |  * ensuring that the total number of messages does not exceed the specified limit. When
   32 |  * the number of messages exceeds the maximum size, older messages are evicted.
   33 |  * <p>
   34 |  * Messages of type {@link SystemMessage} are treated specially: if a new
   35 |  * {@link SystemMessage} is added, all previous {@link SystemMessage} instances are
   36 |  * removed from the memory. Also, if the total number of messages exceeds the limit, the
   37 |  * {@link SystemMessage} messages are preserved while evicting other types of messages.
   38 |  *
   39 |  * @author Thomas Vitale
   40 |  * @author Ilayaperumal Gopinathan
   41 |  * @since 1.0.0
   42 |  * @deprecated since 2.1.0, for removal in 3.0.0, in favor of session memory,
   43 |  * {@code org.springframework.ai.session.SessionService} in {@code spring-ai-session}; a
   44 |  * turn-aware window is {@code EventFilter.lastN}, and compaction replaces eviction
   45 |  */
   46 | @Deprecated(since = "2.1.0", forRemoval = true)
   47 | @SuppressWarnings("removal")
   48 | public final class MessageWindowChatMemory implements ChatMemory {
   49 | 
   50 | 	private static final int DEFAULT_MAX_MESSAGES = 20;
   51 | 
   52 | 	private final ChatMemoryRepository chatMemoryRepository;
   53 | 
   54 | 	private final int maxMessages;
   55 | 
   56 | 	private MessageWindowChatMemory(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
   57 | 		Assert.notNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
   58 | 		Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
   59 | 		this.chatMemoryRepository = chatMemoryRepository;
   60 | 		this.maxMessages = maxMessages;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public void add(String conversationId, List<Message> messages) {
   65 | 		Assert.hasText(conversationId, "conversationId cannot be null or empty");
   66 | 		Assert.notNull(messages, "messages cannot be null");
   67 | 		Assert.noNullElements(messages, "messages cannot contain null elements");
   68 | 
   69 | 		List<Message> memoryMessages = this.chatMemoryRepository.findByConversationId(conversationId);
   70 | 		List<Message> processedMessages = process(memoryMessages, messages);
   71 | 		this.chatMemoryRepository.saveAll(conversationId, processedMessages);
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public List<Message> get(String conversationId) {
   76 | 		Assert.hasText(conversationId, "conversationId cannot be null or empty");
   77 | 		return this.chatMemoryRepository.findByConversationId(conversationId);
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public void clear(String conversationId) {
   82 | 		Assert.hasText(conversationId, "conversationId cannot be null or empty");
   83 | 		this.chatMemoryRepository.deleteByConversationId(conversationId);
   84 | 	}
   85 | 
   86 | 	private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
   87 | 		List<Message> processedMessages = new ArrayList<>();
   88 | 
   89 | 		Set<Message> memoryMessagesSet = new HashSet<>(memoryMessages);
   90 | 		boolean hasNewSystemMessage = newMessages.stream()
   91 | 			.filter(SystemMessage.class::isInstance)
   92 | 			.anyMatch(message -> !memoryMessagesSet.contains(message));
   93 | 
   94 | 		memoryMessages.stream()
   95 | 			.filter(message -> !(hasNewSystemMessage && message instanceof SystemMessage))
   96 | 			.forEach(processedMessages::add);
   97 | 
   98 | 		processedMessages.addAll(newMessages);
   99 | 
  100 | 		if (processedMessages.size() <= this.maxMessages) {
  101 | 			return processedMessages;
  102 | 		}
  103 | 
  104 | 		// Collect the indices of non-system messages; SystemMessages are always
  105 | 		// preserved.
  106 | 		List<Integer> nonSystemIndices = new ArrayList<>();
  107 | 		for (int i = 0; i < processedMessages.size(); i++) {
  108 | 			if (!(processedMessages.get(i) instanceof SystemMessage)) {
  109 | 				nonSystemIndices.add(i);
  110 | 			}
  111 | 		}
  112 | 
  113 | 		// Raw cut: the number of non-system messages that must be removed to fit within
  114 | 		// maxMessages. This index into nonSystemIndices is where the kept window would
  115 | 		// start based on count alone.
  116 | 		int cutIndex = processedMessages.size() - this.maxMessages;
  117 | 
  118 | 		// Snap the cut forward to the nearest USER message so the kept window always
  119 | 		// starts at a complete turn. This prevents keeping an assistant reply or tool
  120 | 		// result without the user message that originated its turn.
  121 | 		while (cutIndex < nonSystemIndices.size()
  122 | 				&& processedMessages.get(nonSystemIndices.get(cutIndex)).getMessageType() != MessageType.USER) {
  123 | 			cutIndex++;
  124 | 		}
  125 | 		cutIndex = Math.min(cutIndex, nonSystemIndices.size());
  126 | 
  127 | 		Set<Integer> removeIndices = new HashSet<>(nonSystemIndices.subList(0, cutIndex));
  128 | 		List<Message> trimmedMessages = new ArrayList<>();
  129 | 		for (int i = 0; i < processedMessages.size(); i++) {
  130 | 			if (!removeIndices.contains(i)) {
  131 | 				trimmedMessages.add(processedMessages.get(i));
  132 | 			}
  133 | 		}
  134 | 		return trimmedMessages;
  135 | 	}
  136 | 
  137 | 	public static Builder builder() {
  138 | 		return new Builder();
  139 | 	}
  140 | 
  141 | 	public static final class Builder {
  142 | 
  143 | 		private ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
  144 | 
  145 | 		private int maxMessages = DEFAULT_MAX_MESSAGES;
  146 | 
  147 | 		private Builder() {
  148 | 		}
  149 | 
  150 | 		public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
  151 | 			this.chatMemoryRepository = chatMemoryRepository;
  152 | 			return this;
  153 | 		}
  154 | 
  155 | 		public Builder maxMessages(int maxMessages) {
  156 | 			this.maxMessages = maxMessages;
  157 | 			return this;
  158 | 		}
  159 | 
  160 | 		public MessageWindowChatMemory build() {
  161 | 			return new MessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
  162 | 		}
  163 | 
  164 | 	}
  165 | 
  166 | }
```
