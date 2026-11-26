<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/ChatClientRequest.java -->
<!-- 正文引用：[[源码:ChatClientRequest.java#L起-L止]] ；代码块头部：(src: ChatClientRequest.java#L起-L止) -->

# ChatClientRequest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/ChatClientRequest.java` |
| 行数 | 90 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.666Z |

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
   24 | import org.springframework.ai.chat.prompt.Prompt;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Represents a request processed by a {@link ChatClient} that ultimately is used to build
   29 |  * a {@link Prompt} to be sent to an AI model.
   30 |  *
   31 |  * @param prompt The prompt to be sent to the AI model
   32 |  * @param context The contextual data through the execution chain
   33 |  * @author Thomas Vitale
   34 |  * @since 1.0.0
   35 |  */
   36 | public record ChatClientRequest(Prompt prompt, Map<String, @Nullable Object> context) {
   37 | 
   38 | 	public ChatClientRequest {
   39 | 		Assert.notNull(prompt, "prompt cannot be null");
   40 | 		Assert.notNull(context, "context cannot be null");
   41 | 		Assert.noNullElements(context.keySet(), "context keys cannot be null");
   42 | 	}
   43 | 
   44 | 	public ChatClientRequest copy() {
   45 | 		return new ChatClientRequest(this.prompt.copy(), new HashMap<>(this.context));
   46 | 	}
   47 | 
   48 | 	public Builder mutate() {
   49 | 		return new Builder().prompt(this.prompt.copy()).context(new HashMap<>(this.context));
   50 | 	}
   51 | 
   52 | 	public static Builder builder() {
   53 | 		return new Builder();
   54 | 	}
   55 | 
   56 | 	public static final class Builder {
   57 | 
   58 | 		private @Nullable Prompt prompt;
   59 | 
   60 | 		private final Map<String, @Nullable Object> context = new HashMap<>();
   61 | 
   62 | 		private Builder() {
   63 | 		}
   64 | 
   65 | 		public Builder prompt(Prompt prompt) {
   66 | 			Assert.notNull(prompt, "prompt cannot be null");
   67 | 			this.prompt = prompt;
   68 | 			return this;
   69 | 		}
   70 | 
   71 | 		public Builder context(Map<String, ? extends @Nullable Object> context) {
   72 | 			Assert.notNull(context, "context cannot be null");
   73 | 			this.context.putAll(context);
   74 | 			return this;
   75 | 		}
   76 | 
   77 | 		public Builder context(String key, @Nullable Object value) {
   78 | 			Assert.notNull(key, "key cannot be null");
   79 | 			this.context.put(key, value);
   80 | 			return this;
   81 | 		}
   82 | 
   83 | 		public ChatClientRequest build() {
   84 | 			Assert.state(this.prompt != null, "prompt cannot be null");
   85 | 			return new ChatClientRequest(this.prompt, this.context);
   86 | 		}
   87 | 
   88 | 	}
   89 | 
   90 | }
```
