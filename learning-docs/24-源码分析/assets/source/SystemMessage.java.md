<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/SystemMessage.java -->
<!-- 正文引用：[[源码:SystemMessage.java#L起-L止]] ；代码块头部：(src: SystemMessage.java#L起-L止) -->

# SystemMessage.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/SystemMessage.java` |
| 行数 | 107 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:27.898Z |

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
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.core.io.Resource;
   25 | import org.springframework.util.StringUtils;
   26 | 
   27 | /**
   28 |  * A message of the type 'system' passed as input. The system message gives high level
   29 |  * instructions for the conversation. This role typically provides high-level instructions
   30 |  * for the conversation. For example, you might use a system message to instruct the
   31 |  * generative to behave like a certain character or to provide answers in a specific
   32 |  * format.
   33 |  */
   34 | public class SystemMessage extends AbstractMessage {
   35 | 
   36 | 	public SystemMessage(@Nullable String textContent) {
   37 | 		this(textContent, Map.of());
   38 | 	}
   39 | 
   40 | 	public SystemMessage(Resource resource) {
   41 | 		this(MessageUtils.readResource(resource), Map.of());
   42 | 	}
   43 | 
   44 | 	private SystemMessage(@Nullable String textContent, Map<String, Object> metadata) {
   45 | 		super(MessageType.SYSTEM, textContent, metadata);
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public String toString() {
   50 | 		return "SystemMessage{" + "textContent='" + getText() + '\'' + ", messageType=" + this.messageType
   51 | 				+ ", metadata=" + this.metadata + '}';
   52 | 	}
   53 | 
   54 | 	public SystemMessage copy() {
   55 | 		return new SystemMessage(getText(), Map.copyOf(this.metadata));
   56 | 	}
   57 | 
   58 | 	public Builder mutate() {
   59 | 		Builder builder = new Builder();
   60 | 		String text = getText();
   61 | 		if (text != null) {
   62 | 			builder.text(text);
   63 | 		}
   64 | 		builder.metadata(this.metadata);
   65 | 		return builder;
   66 | 	}
   67 | 
   68 | 	public static Builder builder() {
   69 | 		return new Builder();
   70 | 	}
   71 | 
   72 | 	public static final class Builder {
   73 | 
   74 | 		private @Nullable String textContent;
   75 | 
   76 | 		private @Nullable Resource resource;
   77 | 
   78 | 		private Map<String, Object> metadata = new HashMap<>();
   79 | 
   80 | 		public Builder text(String textContent) {
   81 | 			this.textContent = textContent;
   82 | 			return this;
   83 | 		}
   84 | 
   85 | 		public Builder text(Resource resource) {
   86 | 			this.resource = resource;
   87 | 			return this;
   88 | 		}
   89 | 
   90 | 		public Builder metadata(Map<String, Object> metadata) {
   91 | 			this.metadata = metadata;
   92 | 			return this;
   93 | 		}
   94 | 
   95 | 		public SystemMessage build() {
   96 | 			if (StringUtils.hasText(this.textContent) && this.resource != null) {
   97 | 				throw new IllegalArgumentException("textContent and resource cannot be set at the same time");
   98 | 			}
   99 | 			else if (this.resource != null) {
  100 | 				this.textContent = MessageUtils.readResource(this.resource);
  101 | 			}
  102 | 			return new SystemMessage(this.textContent, this.metadata);
  103 | 		}
  104 | 
  105 | 	}
  106 | 
  107 | }
```
