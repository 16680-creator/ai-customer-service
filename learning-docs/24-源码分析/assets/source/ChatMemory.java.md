<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/memory/ChatMemory.java -->
<!-- 正文引用：[[源码:ChatMemory.java#L起-L止]] ；代码块头部：(src: ChatMemory.java#L起-L止) -->

# ChatMemory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/memory/ChatMemory.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:07.051Z |

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
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.ai.chat.messages.Message;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * The contract for storing and managing the memory of chat conversations.
   26 |  *
   27 |  * @author Christian Tzolov
   28 |  * @author Thomas Vitale
   29 |  * @since 1.0.0
   30 |  * @deprecated since 2.1.0, for removal in 3.0.0, in favor of session memory,
   31 |  * {@code org.springframework.ai.session.SessionService} in {@code spring-ai-session}; the
   32 |  * context key is {@code MemoryAdvisor.CONVERSATION_ID} in {@code spring-ai-client-chat}
   33 |  */
   34 | @Deprecated(since = "2.1.0", forRemoval = true)
   35 | public interface ChatMemory {
   36 | 
   37 | 	/**
   38 | 	 * The key to retrieve the chat memory conversation id from the context.
   39 | 	 */
   40 | 	String CONVERSATION_ID = "chat_memory_conversation_id";
   41 | 
   42 | 	/**
   43 | 	 * Save the specified message in the chat memory for the specified conversation.
   44 | 	 */
   45 | 	default void add(String conversationId, Message message) {
   46 | 		Assert.hasText(conversationId, "conversationId cannot be null or empty");
   47 | 		Assert.notNull(message, "message cannot be null");
   48 | 		this.add(conversationId, List.of(message));
   49 | 	}
   50 | 
   51 | 	/**
   52 | 	 * Save the specified messages in the chat memory for the specified conversation.
   53 | 	 */
   54 | 	void add(String conversationId, List<Message> messages);
   55 | 
   56 | 	/**
   57 | 	 * Get the messages in the chat memory for the specified conversation.
   58 | 	 */
   59 | 	List<Message> get(String conversationId);
   60 | 
   61 | 	/**
   62 | 	 * Clear the chat memory for the specified conversation.
   63 | 	 */
   64 | 	void clear(String conversationId);
   65 | 
   66 | }
```
