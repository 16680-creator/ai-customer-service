<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/model/ChatResponse.java -->
<!-- 正文引用：[[源码:ChatResponse.java#L起-L止]] ；代码块头部：(src: ChatResponse.java#L起-L止) -->

# ChatResponse.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/model/ChatResponse.java` |
| 行数 | 224 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.037Z |

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
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.Set;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
   27 | import org.springframework.ai.chat.metadata.ChatResponseMetadata;
   28 | import org.springframework.ai.chat.metadata.FinishReasonCategory;
   29 | import org.springframework.ai.model.ModelResponse;
   30 | import org.springframework.util.Assert;
   31 | import org.springframework.util.CollectionUtils;
   32 | 
   33 | /**
   34 |  * The chat completion (e.g. generation) response returned by an AI provider.
   35 |  *
   36 |  * @author Christian Tzolov
   37 |  * @author Mark Pollack
   38 |  * @author Soby Chacko
   39 |  * @author John Blum
   40 |  * @author Alexandros Pappas
   41 |  * @author Thomas Vitale
   42 |  */
   43 | public class ChatResponse implements ModelResponse<Generation> {
   44 | 
   45 | 	private final ChatResponseMetadata chatResponseMetadata;
   46 | 
   47 | 	/**
   48 | 	 * List of generated messages returned by the AI provider.
   49 | 	 */
   50 | 	private final List<Generation> generations;
   51 | 
   52 | 	/**
   53 | 	 * Construct a new {@link ChatResponse} instance without metadata.
   54 | 	 * @param generations the {@link List} of {@link Generation} returned by the AI
   55 | 	 * provider.
   56 | 	 */
   57 | 	public ChatResponse(List<Generation> generations) {
   58 | 		this(generations, new ChatResponseMetadata());
   59 | 	}
   60 | 
   61 | 	/**
   62 | 	 * Construct a new {@link ChatResponse} instance.
   63 | 	 * @param generations the {@link List} of {@link Generation} returned by the AI
   64 | 	 * provider.
   65 | 	 * @param chatResponseMetadata {@link ChatResponseMetadata} containing information
   66 | 	 * about the use of the AI provider's API.
   67 | 	 */
   68 | 	public ChatResponse(List<Generation> generations, ChatResponseMetadata chatResponseMetadata) {
   69 | 		Assert.notNull(generations, "'generations' must not be null");
   70 | 		this.chatResponseMetadata = Objects.requireNonNullElse(chatResponseMetadata, new ChatResponseMetadata());
   71 | 		this.generations = List.copyOf(generations);
   72 | 	}
   73 | 
   74 | 	public static Builder builder() {
   75 | 		return new Builder();
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * The {@link List} of {@link Generation generated outputs}.
   80 | 	 * <p>
   81 | 	 * It is a {@link List} of {@link List lists} because the Prompt could request
   82 | 	 * multiple output {@link Generation generations}.
   83 | 	 * @return the {@link List} of {@link Generation generated outputs}.
   84 | 	 */
   85 | 
   86 | 	@Override
   87 | 	public List<Generation> getResults() {
   88 | 		return this.generations;
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * @return Returns the first {@link Generation} in the generations list.
   93 | 	 */
   94 | 	public @Nullable Generation getResult() {
   95 | 		if (CollectionUtils.isEmpty(this.generations)) {
   96 | 			return null;
   97 | 		}
   98 | 		return this.generations.get(0);
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * @return Returns {@link ChatResponseMetadata} containing information about the use
  103 | 	 * of the AI provider's API.
  104 | 	 */
  105 | 	@Override
  106 | 	public ChatResponseMetadata getMetadata() {
  107 | 		return this.chatResponseMetadata;
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Whether the model has requested the execution of a tool.
  112 | 	 */
  113 | 	public boolean hasToolCalls() {
  114 | 		if (CollectionUtils.isEmpty(this.generations)) {
  115 | 			return false;
  116 | 		}
  117 | 		return this.generations.stream().anyMatch(generation -> generation.getOutput().hasToolCalls());
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Whether the model has finished with any of the given finish reasons. They are
  122 | 	 * compared, ignoring case, with the raw finish reason as the provider names it. For a
  123 | 	 * check that is the same for every provider, see
  124 | 	 * {@link #hasFinishReasonCategory(FinishReasonCategory)}.
  125 | 	 */
  126 | 	public boolean hasFinishReasons(Set<String> finishReasons) {
  127 | 		Assert.notNull(finishReasons, "finishReasons cannot be null");
  128 | 		if (CollectionUtils.isEmpty(this.generations)) {
  129 | 			return false;
  130 | 		}
  131 | 		return this.generations.stream().anyMatch(generation -> {
  132 | 			var finishReason = (generation.getMetadata().getFinishReason() != null)
  133 | 					? generation.getMetadata().getFinishReason() : "";
  134 | 			return finishReasons.stream().anyMatch(fr -> fr.equalsIgnoreCase(finishReason));
  135 | 		});
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Whether any generation finished for a reason of the given category, whatever name
  140 | 	 * the provider gives it. For example, {@link FinishReasonCategory#FILTERED} tells
  141 | 	 * that a safety filter or a refusal of the model stopped a generation.
  142 | 	 * @param category the category to look for
  143 | 	 * @return {@code true} when at least one generation finished in that category
  144 | 	 * @since 2.1.0
  145 | 	 * @see ChatGenerationMetadata#getFinishReasonCategory()
  146 | 	 */
  147 | 	public boolean hasFinishReasonCategory(FinishReasonCategory category) {
  148 | 		Assert.notNull(category, "category cannot be null");
  149 | 		if (CollectionUtils.isEmpty(this.generations)) {
  150 | 			return false;
  151 | 		}
  152 | 		return this.generations.stream()
  153 | 			.anyMatch(generation -> generation.getMetadata().getFinishReasonCategory() == category);
  154 | 	}
  155 | 
  156 | 	@Override
  157 | 	public String toString() {
  158 | 		return "ChatResponse [metadata=" + this.chatResponseMetadata + ", generations=" + this.generations + "]";
  159 | 	}
  160 | 
  161 | 	@Override
  162 | 	public boolean equals(@Nullable Object o) {
  163 | 		if (this == o) {
  164 | 			return true;
  165 | 		}
  166 | 		if (!(o instanceof ChatResponse that)) {
  167 | 			return false;
  168 | 		}
  169 | 		return Objects.equals(this.chatResponseMetadata, that.chatResponseMetadata)
  170 | 				&& Objects.equals(this.generations, that.generations);
  171 | 	}
  172 | 
  173 | 	@Override
  174 | 	public int hashCode() {
  175 | 		return Objects.hash(this.chatResponseMetadata, this.generations);
  176 | 	}
  177 | 
  178 | 	public static final class Builder {
  179 | 
  180 | 		private @Nullable List<Generation> generations;
  181 | 
  182 | 		private ChatResponseMetadata.Builder chatResponseMetadataBuilder;
  183 | 
  184 | 		private Builder() {
  185 | 			this.chatResponseMetadataBuilder = ChatResponseMetadata.builder();
  186 | 		}
  187 | 
  188 | 		public Builder from(ChatResponse other) {
  189 | 			this.generations = other.generations;
  190 | 			return this.metadata(other.chatResponseMetadata);
  191 | 		}
  192 | 
  193 | 		public Builder metadata(String key, Object value) {
  194 | 			this.chatResponseMetadataBuilder.keyValue(key, value);
  195 | 			return this;
  196 | 		}
  197 | 
  198 | 		public Builder metadata(ChatResponseMetadata other) {
  199 | 			this.chatResponseMetadataBuilder.model(other.getModel());
  200 | 			this.chatResponseMetadataBuilder.id(other.getId());
  201 | 			this.chatResponseMetadataBuilder.rateLimit(other.getRateLimit());
  202 | 			this.chatResponseMetadataBuilder.usage(other.getUsage());
  203 | 			this.chatResponseMetadataBuilder.promptMetadata(other.getPromptMetadata());
  204 | 			Set<Map.Entry<String, Object>> entries = other.entrySet();
  205 | 			for (Map.Entry<String, Object> entry : entries) {
  206 | 				this.chatResponseMetadataBuilder.keyValue(entry.getKey(), entry.getValue());
  207 | 			}
  208 | 			return this;
  209 | 		}
  210 | 
  211 | 		public Builder generations(List<Generation> generations) {
  212 | 			this.generations = generations;
  213 | 			return this;
  214 | 
  215 | 		}
  216 | 
  217 | 		public ChatResponse build() {
  218 | 			Assert.notNull(this.generations, "'generations' must not be null");
  219 | 			return new ChatResponse(this.generations, this.chatResponseMetadataBuilder.build());
  220 | 		}
  221 | 
  222 | 	}
  223 | 
  224 | }
```
