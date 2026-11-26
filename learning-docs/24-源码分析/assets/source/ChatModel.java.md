<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/model/ChatModel.java -->
<!-- 正文引用：[[源码:ChatModel.java#L起-L止]] ；代码块头部：(src: ChatModel.java#L起-L止) -->

# ChatModel.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/model/ChatModel.java` |
| 行数 | 68 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:03.908Z |

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
   17 | package org.springframework.ai.chat.model;
   18 | 
   19 | import java.util.Arrays;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | import reactor.core.publisher.Flux;
   23 | 
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.ai.chat.messages.UserMessage;
   26 | import org.springframework.ai.chat.prompt.ChatOptions;
   27 | import org.springframework.ai.chat.prompt.Prompt;
   28 | import org.springframework.ai.model.Model;
   29 | 
   30 | public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {
   31 | 
   32 | 	default @Nullable String call(String message) {
   33 | 		Prompt prompt = new Prompt(new UserMessage(message));
   34 | 		Generation generation = call(prompt).getResult();
   35 | 		return (generation != null) ? generation.getOutput().getText() : "";
   36 | 	}
   37 | 
   38 | 	default @Nullable String call(Message... messages) {
   39 | 		Prompt prompt = new Prompt(Arrays.asList(messages));
   40 | 		Generation generation = call(prompt).getResult();
   41 | 		return (generation != null) ? generation.getOutput().getText() : "";
   42 | 	}
   43 | 
   44 | 	@Override
   45 | 	ChatResponse call(Prompt prompt);
   46 | 
   47 | 	/**
   48 | 	 * Gets the chat options for this model.
   49 | 	 * @return the chat options
   50 | 	 * @since 2.0.0
   51 | 	 */
   52 | 	default ChatOptions getOptions() {
   53 | 		return ChatOptions.builder().build();
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * @deprecated use {@link #getOptions()} instead.
   58 | 	 */
   59 | 	@Deprecated(forRemoval = true)
   60 | 	default ChatOptions getDefaultOptions() {
   61 | 		return getOptions();
   62 | 	}
   63 | 
   64 | 	default Flux<ChatResponse> stream(Prompt prompt) {
   65 | 		throw new UnsupportedOperationException("streaming is not supported");
   66 | 	}
   67 | 
   68 | }
```
