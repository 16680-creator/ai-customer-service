<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/ChatClientResponse.java -->
<!-- 正文引用：[[源码:ChatClientResponse.java#L起-L止]] ；代码块头部：(src: ChatClientResponse.java#L起-L止) -->

# ChatClientResponse.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/ChatClientResponse.java` |
| 行数 | 86 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.746Z |

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
   17 | package org.springframework.ai.chat.client;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.model.ChatResponse;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Represents a response returned by a {@link ChatClient}.
   29 |  *
   30 |  * @param chatResponse The response returned by the AI model
   31 |  * @param context The contextual data propagated through the execution chain
   32 |  * @author Thomas Vitale
   33 |  * @since 1.0.0
   34 |  */
   35 | public record ChatClientResponse(@Nullable ChatResponse chatResponse, Map<String, @Nullable Object> context) {
   36 | 
   37 | 	public ChatClientResponse {
   38 | 		Assert.notNull(context, "context cannot be null");
   39 | 		Assert.noNullElements(context.keySet(), "context keys cannot be null");
   40 | 	}
   41 | 
   42 | 	public ChatClientResponse copy() {
   43 | 		return new ChatClientResponse(this.chatResponse, new HashMap<>(this.context));
   44 | 	}
   45 | 
   46 | 	public Builder mutate() {
   47 | 		return new Builder().chatResponse(this.chatResponse).context(new HashMap<>(this.context));
   48 | 	}
   49 | 
   50 | 	public static Builder builder() {
   51 | 		return new Builder();
   52 | 	}
   53 | 
   54 | 	public static final class Builder {
   55 | 
   56 | 		private @Nullable ChatResponse chatResponse;
   57 | 
   58 | 		private final Map<String, @Nullable Object> context = new HashMap<>();
   59 | 
   60 | 		private Builder() {
   61 | 		}
   62 | 
   63 | 		public Builder chatResponse(@Nullable ChatResponse chatResponse) {
   64 | 			this.chatResponse = chatResponse;
   65 | 			return this;
   66 | 		}
   67 | 
   68 | 		public Builder context(Map<String, ? extends @Nullable Object> context) {
   69 | 			Assert.notNull(context, "context cannot be null");
   70 | 			this.context.putAll(context);
   71 | 			return this;
   72 | 		}
   73 | 
   74 | 		public Builder context(String key, @Nullable Object value) {
   75 | 			Assert.notNull(key, "key cannot be null");
   76 | 			this.context.put(key, value);
   77 | 			return this;
   78 | 		}
   79 | 
   80 | 		public ChatClientResponse build() {
   81 | 			return new ChatClientResponse(this.chatResponse, this.context);
   82 | 		}
   83 | 
   84 | 	}
   85 | 
   86 | }
```
