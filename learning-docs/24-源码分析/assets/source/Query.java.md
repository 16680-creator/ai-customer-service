<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/Query.java -->
<!-- 正文引用：[[源码:Query.java#L起-L止]] ；代码块头部：(src: Query.java#L起-L止) -->

# Query.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/Query.java` |
| 行数 | 96 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:18.648Z |

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
   17 | package org.springframework.ai.rag;
   18 | 
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Represents a query in the context of a Retrieval Augmented Generation (RAG) flow.
   29 |  *
   30 |  * @param text the text of the query
   31 |  * @param history the messages in the conversation history
   32 |  * @param context the context of the query
   33 |  * @author Thomas Vitale
   34 |  * @since 1.0.0
   35 |  */
   36 | public record Query(String text, List<Message> history, Map<String, Object> context) {
   37 | 
   38 | 	public Query {
   39 | 		Assert.hasText(text, "text cannot be null or empty");
   40 | 		Assert.notNull(history, "history cannot be null");
   41 | 		Assert.noNullElements(history, "history elements cannot be null");
   42 | 		Assert.notNull(context, "context cannot be null");
   43 | 		Assert.noNullElements(context.keySet(), "context keys cannot be null");
   44 | 	}
   45 | 
   46 | 	public Query(String text) {
   47 | 		this(text, List.of(), Map.of());
   48 | 	}
   49 | 
   50 | 	public Builder mutate() {
   51 | 		return new Builder().text(this.text).history(this.history).context(this.context);
   52 | 	}
   53 | 
   54 | 	public static Builder builder() {
   55 | 		return new Builder();
   56 | 	}
   57 | 
   58 | 	public static final class Builder {
   59 | 
   60 | 		private @Nullable String text;
   61 | 
   62 | 		private List<Message> history = List.of();
   63 | 
   64 | 		private Map<String, Object> context = Map.of();
   65 | 
   66 | 		private Builder() {
   67 | 		}
   68 | 
   69 | 		public Builder text(String text) {
   70 | 			this.text = text;
   71 | 			return this;
   72 | 		}
   73 | 
   74 | 		public Builder history(List<Message> history) {
   75 | 			this.history = history;
   76 | 			return this;
   77 | 		}
   78 | 
   79 | 		public Builder history(Message... history) {
   80 | 			this.history = List.of(history);
   81 | 			return this;
   82 | 		}
   83 | 
   84 | 		public Builder context(Map<String, Object> context) {
   85 | 			this.context = context;
   86 | 			return this;
   87 | 		}
   88 | 
   89 | 		public Query build() {
   90 | 			Assert.hasText(this.text, "text cannot be null or empty");
   91 | 			return new Query(this.text, this.history, this.context);
   92 | 		}
   93 | 
   94 | 	}
   95 | 
   96 | }
```
