<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/metadata/ChatGenerationMetadata.java -->
<!-- 正文引用：[[源码:ChatGenerationMetadata.java#L起-L止]] ；代码块头部：(src: ChatGenerationMetadata.java#L起-L止) -->

# ChatGenerationMetadata.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/metadata/ChatGenerationMetadata.java` |
| 行数 | 140 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:28.083Z |

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
   20 | import java.util.Map.Entry;
   21 | import java.util.Set;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.ai.model.ResultMetadata;
   26 | 
   27 | /**
   28 |  *
   29 |  * Represents the metadata associated with the generation of a chat response.
   30 |  *
   31 |  * @author John Blum
   32 |  * @author Christian Tzolov
   33 |  * @since 0.7.0
   34 |  */
   35 | public interface ChatGenerationMetadata extends ResultMetadata {
   36 | 
   37 | 	ChatGenerationMetadata NULL = builder().build();
   38 | 
   39 | 	/**
   40 | 	 * Get the reason this generation finished, as the provider names it. The names differ
   41 | 	 * between providers: {@code stop}, {@code end_turn} and {@code STOP} all say that the
   42 | 	 * model finished its answer. For a meaning that is the same for every provider, see
   43 | 	 * {@link #getFinishReasonCategory()}.
   44 | 	 * @return the raw finish reason of the provider, or {@code null} when it gave none
   45 | 	 */
   46 | 	@Nullable String getFinishReason();
   47 | 
   48 | 	/**
   49 | 	 * Return the normalized category of the finish reason, which is the same for every
   50 | 	 * provider, while {@link #getFinishReason()} keeps the name the provider uses.
   51 | 	 * <p>
   52 | 	 * A chat model classifies its own finish reasons and sets the category with
   53 | 	 * {@link Builder#finishReasonCategory(FinishReasonCategory)}. It sees the whole
   54 | 	 * provider response, so it can also classify a stop that the finish reason alone does
   55 | 	 * not tell, such as a refusal reported in another field.
   56 | 	 * <p>
   57 | 	 * This default implementation is the fallback for metadata that carries no category.
   58 | 	 * It classifies the raw string only: a {@code null} reason is
   59 | 	 * {@link FinishReasonCategory#UNSPECIFIED} and an unrecognized non-blank reason is
   60 | 	 * {@link FinishReasonCategory#OTHER}.
   61 | 	 * @return the categorized finish reason, never {@code null}
   62 | 	 * @since 2.1.0
   63 | 	 * @see FinishReasonCategory#categorize(String)
   64 | 	 */
   65 | 	default FinishReasonCategory getFinishReasonCategory() {
   66 | 		return FinishReasonCategory.categorize(getFinishReason());
   67 | 	}
   68 | 
   69 | 	Set<String> getContentFilters();
   70 | 
   71 | 	<T> @Nullable T get(String key);
   72 | 
   73 | 	boolean containsKey(String key);
   74 | 
   75 | 	<T> T getOrDefault(String key, T defaultObject);
   76 | 
   77 | 	Set<Entry<String, Object>> entrySet();
   78 | 
   79 | 	Set<String> keySet();
   80 | 
   81 | 	boolean isEmpty();
   82 | 
   83 | 	static Builder builder() {
   84 | 		return new DefaultChatGenerationMetadataBuilder();
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * @author Christian Tzolov
   89 | 	 * @since 1.0.0
   90 | 	 */
   91 | 	public interface Builder {
   92 | 
   93 | 		/**
   94 | 		 * Set the reason this generation finished, as the provider names it. See
   95 | 		 * {@link #finishReasonCategory(FinishReasonCategory)} for its normalized
   96 | 		 * category.
   97 | 		 */
   98 | 		Builder finishReason(@Nullable String finishReason);
   99 | 
  100 | 		/**
  101 | 		 * Set the normalized category of the finish reason. A chat model sets it next to
  102 | 		 * {@link #finishReason(String)}, as it knows what the names of its provider mean.
  103 | 		 * When it is not set, the category is derived from the raw finish reason with
  104 | 		 * {@link FinishReasonCategory#categorize(String)}.
  105 | 		 * @param finishReasonCategory the category, or {@code null} to derive it
  106 | 		 * @return this builder
  107 | 		 * @since 2.1.0
  108 | 		 */
  109 | 		default Builder finishReasonCategory(@Nullable FinishReasonCategory finishReasonCategory) {
  110 | 			return this;
  111 | 		}
  112 | 
  113 | 		/**
  114 | 		 * Add metadata to the Generation result.
  115 | 		 */
  116 | 		<T> Builder metadata(String key, T value);
  117 | 
  118 | 		/**
  119 | 		 * Add metadata to the Generation result.
  120 | 		 */
  121 | 		Builder metadata(Map<String, Object> metadata);
  122 | 
  123 | 		/**
  124 | 		 * Add content filter to the Generation result.
  125 | 		 */
  126 | 		Builder contentFilter(String contentFilter);
  127 | 
  128 | 		/**
  129 | 		 * Add content filters to the Generation result.
  130 | 		 */
  131 | 		Builder contentFilters(Set<String> contentFilters);
  132 | 
  133 | 		/**
  134 | 		 * Build the Generation metadata.
  135 | 		 */
  136 | 		ChatGenerationMetadata build();
  137 | 
  138 | 	}
  139 | 
  140 | }
```
