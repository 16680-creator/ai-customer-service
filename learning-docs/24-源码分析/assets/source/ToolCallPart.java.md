<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/part/ToolCallPart.java -->
<!-- 正文引用：[[源码:ToolCallPart.java#L起-L止]] ；代码块头部：(src: ToolCallPart.java#L起-L止) -->

# ToolCallPart.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/part/ToolCallPart.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:05.309Z |

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
   17 | package org.springframework.ai.chat.messages.part;
   18 | 
   19 | import java.util.Map;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
   24 | import org.springframework.util.Assert;
   25 | 
   26 | /**
   27 |  * A tool call requested by the model.
   28 |  * <p>
   29 |  * The {@link #payload()} slot exists on tool calls because some providers attach their
   30 |  * reasoning token to the call rather than to a {@link ReasoningPart} (for example a
   31 |  * Gemini {@code thought_signature}).
   32 |  *
   33 |  * @param toolCall the tool call
   34 |  * @param payload provider data to replay with this part, or {@code null}
   35 |  * @param attributes free-form attributes, copied and unmodifiable
   36 |  * @author Christian Tzolov
   37 |  * @since 2.1.0
   38 |  */
   39 | public record ToolCallPart(ToolCall toolCall, @Nullable OpaquePayload payload,
   40 | 		Map<String, String> attributes) implements MessagePart {
   41 | 
   42 | 	public ToolCallPart {
   43 | 		Assert.notNull(toolCall, "toolCall must not be null");
   44 | 		Assert.notNull(attributes, "attributes must not be null");
   45 | 		attributes = Map.copyOf(attributes);
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public ToolCallPart withAttributes(Map<String, String> attributes) {
   50 | 		return new ToolCallPart(this.toolCall, this.payload, attributes);
   51 | 	}
   52 | 
   53 | 	/**
   54 | 	 * Creates a tool call part without payload or attributes.
   55 | 	 * @param toolCall the tool call
   56 | 	 * @return the part
   57 | 	 */
   58 | 	public static ToolCallPart of(ToolCall toolCall) {
   59 | 		return new ToolCallPart(toolCall, null, Map.of());
   60 | 	}
   61 | 
   62 | }
```
