<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/metadata/ChatResponseMetadata.java -->
<!-- 正文引用：[[源码:ChatResponseMetadata.java#L起-L止]] ；代码块头部：(src: ChatResponseMetadata.java#L起-L止) -->

# ChatResponseMetadata.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/metadata/ChatResponseMetadata.java` |
| 行数 | 181 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.517Z |

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
   17 | package org.springframework.ai.chat.metadata;
   18 | 
   19 | import java.util.Map;
   20 | import java.util.Objects;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.ai.model.AbstractResponseMetadata;
   27 | import org.springframework.ai.model.ResponseMetadata;
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * Models common AI provider metadata returned in an AI response.
   32 |  *
   33 |  * @author John Blum
   34 |  * @author Thomas Vitale
   35 |  * @author Mark Pollack
   36 |  * @author Alexandros Pappas
   37 |  * @since 1.0.0
   38 |  */
   39 | public class ChatResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {
   40 | 
   41 | 	private static final Log logger = LogFactory.getLog(ChatResponseMetadata.class);
   42 | 
   43 | 	private String id = ""; // Set to blank to preserve backward compat with previous
   44 | 
   45 | 	// interface default methods
   46 | 
   47 | 	private String model = "";
   48 | 
   49 | 	private RateLimit rateLimit = new EmptyRateLimit();
   50 | 
   51 | 	private Usage usage = new EmptyUsage();
   52 | 
   53 | 	private PromptMetadata promptMetadata = PromptMetadata.empty();
   54 | 
   55 | 	public static Builder builder() {
   56 | 		return new Builder();
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * A unique identifier for the chat completion operation.
   61 | 	 * @return unique operation identifier.
   62 | 	 */
   63 | 	public String getId() {
   64 | 		return this.id;
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * The model that handled the request.
   69 | 	 * @return the model that handled the request.
   70 | 	 */
   71 | 	public String getModel() {
   72 | 		return this.model;
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Returns AI provider specific metadata on rate limits.
   77 | 	 * @return AI provider specific metadata on rate limits.
   78 | 	 * @see RateLimit
   79 | 	 */
   80 | 	public RateLimit getRateLimit() {
   81 | 		return this.rateLimit;
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * Returns AI provider specific metadata on API usage.
   86 | 	 * @return AI provider specific metadata on API usage.
   87 | 	 * @see Usage
   88 | 	 */
   89 | 	public Usage getUsage() {
   90 | 		return this.usage;
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Returns the prompt metadata gathered by the AI during request processing.
   95 | 	 * @return the prompt metadata.
   96 | 	 */
   97 | 	public PromptMetadata getPromptMetadata() {
   98 | 		return this.promptMetadata;
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public boolean equals(@Nullable Object o) {
  103 | 		if (this == o) {
  104 | 			return true;
  105 | 		}
  106 | 		if (!(o instanceof ChatResponseMetadata that)) {
  107 | 			return false;
  108 | 		}
  109 | 		return Objects.equals(this.id, that.id) && Objects.equals(this.model, that.model)
  110 | 				&& Objects.equals(this.rateLimit, that.rateLimit) && Objects.equals(this.usage, that.usage)
  111 | 				&& Objects.equals(this.promptMetadata, that.promptMetadata);
  112 | 	}
  113 | 
  114 | 	@Override
  115 | 	public int hashCode() {
  116 | 		return Objects.hash(this.id, this.model, this.rateLimit, this.usage, this.promptMetadata);
  117 | 	}
  118 | 
  119 | 	@Override
  120 | 	public String toString() {
  121 | 		return AI_METADATA_STRING.formatted(getId(), getUsage(), getRateLimit());
  122 | 	}
  123 | 
  124 | 	public static final class Builder {
  125 | 
  126 | 		private final ChatResponseMetadata chatResponseMetadata;
  127 | 
  128 | 		public Builder() {
  129 | 			this.chatResponseMetadata = new ChatResponseMetadata();
  130 | 		}
  131 | 
  132 | 		public Builder metadata(Map<String, Object> mapToCopy) {
  133 | 			this.chatResponseMetadata.map.putAll(mapToCopy);
  134 | 			return this;
  135 | 		}
  136 | 
  137 | 		public Builder keyValue(String key, @Nullable Object value) {
  138 | 			Assert.notNull(key, "Key must not be null"); // Defensive check
  139 | 			if (value != null) {
  140 | 				this.chatResponseMetadata.map.put(key, value);
  141 | 			}
  142 | 			else {
  143 | 				if (logger.isDebugEnabled()) {
  144 | 					logger.debug("Ignore null value for key [" + key + "]");
  145 | 				}
  146 | 			}
  147 | 			return this;
  148 | 		}
  149 | 
  150 | 		public Builder id(String id) {
  151 | 			this.chatResponseMetadata.id = id;
  152 | 			return this;
  153 | 		}
  154 | 
  155 | 		public Builder model(String model) {
  156 | 			this.chatResponseMetadata.model = model;
  157 | 			return this;
  158 | 		}
  159 | 
  160 | 		public Builder rateLimit(RateLimit rateLimit) {
  161 | 			this.chatResponseMetadata.rateLimit = rateLimit;
  162 | 			return this;
  163 | 		}
  164 | 
  165 | 		public Builder usage(Usage usage) {
  166 | 			this.chatResponseMetadata.usage = usage;
  167 | 			return this;
  168 | 		}
  169 | 
  170 | 		public Builder promptMetadata(PromptMetadata promptMetadata) {
  171 | 			this.chatResponseMetadata.promptMetadata = promptMetadata;
  172 | 			return this;
  173 | 		}
  174 | 
  175 | 		public ChatResponseMetadata build() {
  176 | 			return this.chatResponseMetadata;
  177 | 		}
  178 | 
  179 | 	}
  180 | 
  181 | }
```
