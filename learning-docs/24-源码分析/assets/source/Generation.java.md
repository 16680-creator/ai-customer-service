<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/model/Generation.java -->
<!-- 正文引用：[[源码:Generation.java#L起-L止]] ；代码块头部：(src: Generation.java#L起-L止) -->

# Generation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/model/Generation.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.120Z |

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
   19 | import java.util.Objects;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.messages.AssistantMessage;
   24 | import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
   25 | import org.springframework.ai.model.ModelResult;
   26 | 
   27 | /**
   28 |  * Represents a response returned by the AI.
   29 |  */
   30 | public class Generation implements ModelResult<AssistantMessage> {
   31 | 
   32 | 	private final AssistantMessage assistantMessage;
   33 | 
   34 | 	private ChatGenerationMetadata chatGenerationMetadata;
   35 | 
   36 | 	public Generation(AssistantMessage assistantMessage) {
   37 | 		this(assistantMessage, ChatGenerationMetadata.NULL);
   38 | 	}
   39 | 
   40 | 	public Generation(AssistantMessage assistantMessage, ChatGenerationMetadata chatGenerationMetadata) {
   41 | 		this.assistantMessage = assistantMessage;
   42 | 		this.chatGenerationMetadata = chatGenerationMetadata != null ? chatGenerationMetadata
   43 | 				: ChatGenerationMetadata.NULL;
   44 | 	}
   45 | 
   46 | 	@Override
   47 | 	public AssistantMessage getOutput() {
   48 | 		return this.assistantMessage;
   49 | 	}
   50 | 
   51 | 	@Override
   52 | 	public ChatGenerationMetadata getMetadata() {
   53 | 		return this.chatGenerationMetadata;
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public boolean equals(@Nullable Object o) {
   58 | 		if (this == o) {
   59 | 			return true;
   60 | 		}
   61 | 		if (!(o instanceof Generation that)) {
   62 | 			return false;
   63 | 		}
   64 | 		return Objects.equals(this.assistantMessage, that.assistantMessage)
   65 | 				&& Objects.equals(this.chatGenerationMetadata, that.chatGenerationMetadata);
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public int hashCode() {
   70 | 		return Objects.hash(this.assistantMessage, this.chatGenerationMetadata);
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public String toString() {
   75 | 		return "Generation[" + "assistantMessage=" + this.assistantMessage + ", chatGenerationMetadata="
   76 | 				+ this.chatGenerationMetadata + ']';
   77 | 	}
   78 | 
   79 | }
```
