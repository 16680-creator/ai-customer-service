<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/api/MemoryAdvisor.java -->
<!-- 正文引用：[[源码:MemoryAdvisor.java#L起-L止]] ；代码块头部：(src: MemoryAdvisor.java#L起-L止) -->

# MemoryAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/api/MemoryAdvisor.java` |
| 行数 | 58 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:32:11.108Z |

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
   19 | import java.util.Map;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.util.Assert;
   24 | 
   25 | /**
   26 |  * An advisor that owns the memory lifecycle: it stores, retrieves and updates the
   27 |  * conversation a request belongs to, named by {@link #CONVERSATION_ID}.
   28 |  *
   29 |  * <p>
   30 |  * {@link org.springframework.ai.chat.client.DefaultChatClient} uses this marker to detect
   31 |  * whether a memory advisor is already present in the chain, and to avoid auto-registering
   32 |  * a duplicate when memory management is configured on the {@code ChatClient}.
   33 |  *
   34 |  * @author Christian Tzolov
   35 |  * @since 2.0.0
   36 |  * @see org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor
   37 |  */
   38 | public interface MemoryAdvisor extends Advisor {
   39 | 
   40 | 	/**
   41 | 	 * The advisor context key of the conversation a request belongs to. Memory advisors
   42 | 	 * read it to find the conversation or session, and the {@code ChatClient} observation
   43 | 	 * records it. Set with
   44 | 	 * {@code .advisors(a -> a.param(MemoryAdvisor.CONVERSATION_ID, conversationId))}.
   45 | 	 */
   46 | 	String CONVERSATION_ID = "chat_memory_conversation_id";
   47 | 
   48 | 	/**
   49 | 	 * Retrieve the conversation ID from the given context.
   50 | 	 */
   51 | 	default String getConversationId(Map<String, @Nullable Object> context) {
   52 | 		Assert.notNull(context, "context cannot be null");
   53 | 		Assert.noNullElements(context.keySet().toArray(), "context cannot contain null keys");
   54 | 		Assert.notNull(context.get(CONVERSATION_ID), "conversationId cannot be null");
   55 | 		return context.get(CONVERSATION_ID).toString();
   56 | 	}
   57 | 
   58 | }
```
