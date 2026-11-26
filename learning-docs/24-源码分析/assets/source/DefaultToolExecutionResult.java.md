<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/DefaultToolExecutionResult.java -->
<!-- 正文引用：[[源码:DefaultToolExecutionResult.java#L起-L止]] ；代码块头部：(src: DefaultToolExecutionResult.java#L起-L止) -->

# DefaultToolExecutionResult.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/DefaultToolExecutionResult.java` |
| 行数 | 67 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.734Z |

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
   17 | package org.springframework.ai.model.tool;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.ai.chat.messages.Message;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * Default implementation of {@link ToolExecutionResult}.
   26 |  *
   27 |  * @author Thomas Vitale
   28 |  * @since 1.0.0
   29 |  */
   30 | public record DefaultToolExecutionResult(List<Message> conversationHistory,
   31 | 		boolean returnDirect) implements ToolExecutionResult {
   32 | 
   33 | 	public DefaultToolExecutionResult {
   34 | 		Assert.notNull(conversationHistory, "conversationHistory cannot be null");
   35 | 		Assert.noNullElements(conversationHistory, "conversationHistory cannot contain null elements");
   36 | 	}
   37 | 
   38 | 	public static Builder builder() {
   39 | 		return new Builder();
   40 | 	}
   41 | 
   42 | 	public static final class Builder {
   43 | 
   44 | 		private List<Message> conversationHistory = List.of();
   45 | 
   46 | 		private boolean returnDirect;
   47 | 
   48 | 		private Builder() {
   49 | 		}
   50 | 
   51 | 		public Builder conversationHistory(List<Message> conversationHistory) {
   52 | 			this.conversationHistory = conversationHistory;
   53 | 			return this;
   54 | 		}
   55 | 
   56 | 		public Builder returnDirect(boolean returnDirect) {
   57 | 			this.returnDirect = returnDirect;
   58 | 			return this;
   59 | 		}
   60 | 
   61 | 		public DefaultToolExecutionResult build() {
   62 | 			return new DefaultToolExecutionResult(this.conversationHistory, this.returnDirect);
   63 | 		}
   64 | 
   65 | 	}
   66 | 
   67 | }
```
