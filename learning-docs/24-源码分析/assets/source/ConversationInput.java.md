<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/input/ConversationInput.java -->
<!-- 正文引用：[[源码:ConversationInput.java#L起-L止]] ；代码块头部：(src: ConversationInput.java#L起-L止) -->

# ConversationInput.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/input/ConversationInput.java` |
| 行数 | 86 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.438Z |

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
   17 | package org.springframework.ai.decision.input;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.messages.AbstractMessage;
   24 | import org.springframework.ai.chat.messages.Message;
   25 | import org.springframework.ai.chat.messages.part.MessagePart;
   26 | import org.springframework.ai.chat.messages.part.TextPart;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * A {@link DecisionInput} made of a conversation, such as the one a chat model is about
   31 |  * to answer or has just answered. The messages are the same {@link Message} types a chat
   32 |  * model takes.
   33 |  *
   34 |  * @author Thomas Vitale
   35 |  * @since 2.1.0
   36 |  */
   37 | public final class ConversationInput implements DecisionInput {
   38 | 
   39 | 	private final List<Message> messages;
   40 | 
   41 | 	ConversationInput(List<? extends Message> messages) {
   42 | 		Assert.notEmpty(messages, "messages cannot be null or empty");
   43 | 		this.messages = List.copyOf(messages);
   44 | 	}
   45 | 
   46 | 	public List<Message> getMessages() {
   47 | 		return this.messages;
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * The parts of a message, in order, for a provider to map. A message that does not
   52 | 	 * keep parts has its text as a single text part, or no part when it has no text.
   53 | 	 * @param message the message
   54 | 	 * @return the parts of the message
   55 | 	 * @since 2.1.0
   56 | 	 */
   57 | 	public static List<MessagePart> parts(Message message) {
   58 | 		if (message instanceof AbstractMessage abstractMessage) {
   59 | 			return abstractMessage.getParts();
   60 | 		}
   61 | 		String text = message.getText();
   62 | 		return (text != null) ? List.of(TextPart.of(text)) : List.of();
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public boolean equals(@Nullable Object o) {
   67 | 		if (this == o) {
   68 | 			return true;
   69 | 		}
   70 | 		if (!(o instanceof ConversationInput that)) {
   71 | 			return false;
   72 | 		}
   73 | 		return this.messages.equals(that.messages);
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public int hashCode() {
   78 | 		return this.messages.hashCode();
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public String toString() {
   83 | 		return "ConversationInput{messages=" + this.messages.size() + "}";
   84 | 	}
   85 | 
   86 | }
```
