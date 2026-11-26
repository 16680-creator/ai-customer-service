<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/model/StreamingChatModel.java -->
<!-- 正文引用：[[源码:StreamingChatModel.java#L起-L止]] ；代码块头部：(src: StreamingChatModel.java#L起-L止) -->

# StreamingChatModel.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/model/StreamingChatModel.java` |
| 行数 | 51 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:03.970Z |

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
   20 | import java.util.Optional;
   21 | 
   22 | import reactor.core.publisher.Flux;
   23 | 
   24 | import org.springframework.ai.chat.messages.AssistantMessage;
   25 | import org.springframework.ai.chat.messages.Message;
   26 | import org.springframework.ai.chat.prompt.Prompt;
   27 | import org.springframework.ai.model.StreamingModel;
   28 | 
   29 | @FunctionalInterface
   30 | public interface StreamingChatModel extends StreamingModel<Prompt, ChatResponse> {
   31 | 
   32 | 	default Flux<String> stream(String message) {
   33 | 		Prompt prompt = new Prompt(message);
   34 | 		return stream(prompt).map(response -> Optional.ofNullable(response.getResult())
   35 | 			.map(Generation::getOutput)
   36 | 			.map(AssistantMessage::getText)
   37 | 			.orElse(""));
   38 | 	}
   39 | 
   40 | 	default Flux<String> stream(Message... messages) {
   41 | 		Prompt prompt = new Prompt(Arrays.asList(messages));
   42 | 		return stream(prompt).map(response -> Optional.ofNullable(response.getResult())
   43 | 			.map(Generation::getOutput)
   44 | 			.map(AssistantMessage::getText)
   45 | 			.orElse(""));
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	Flux<ChatResponse> stream(Prompt prompt);
   50 | 
   51 | }
```
