<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/Message.java -->
<!-- 正文引用：[[源码:Message.java#L起-L止]] ；代码块头部：(src: Message.java#L起-L止) -->

# Message.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/Message.java` |
| 行数 | 38 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:05.021Z |

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
   17 | package org.springframework.ai.chat.messages;
   18 | 
   19 | import org.springframework.ai.content.Content;
   20 | import org.springframework.ai.content.Media;
   21 | 
   22 | /**
   23 |  * The Message interface represents a message that can be sent or received in a chat
   24 |  * application. Messages can have content, media attachments, properties, and message
   25 |  * types.
   26 |  *
   27 |  * @see Media
   28 |  * @see MessageType
   29 |  */
   30 | public interface Message extends Content {
   31 | 
   32 | 	/**
   33 | 	 * Get the message type.
   34 | 	 * @return the message type
   35 | 	 */
   36 | 	MessageType getMessageType();
   37 | 
   38 | }
```
