<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/prompt/ChatOptions.java -->
<!-- 正文引用：[[源码:ChatOptions.java#L起-L止]] ；代码块头部：(src: ChatOptions.java#L起-L止) -->

# ChatOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/prompt/ChatOptions.java` |
| 行数 | 173 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.365Z |

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
   17 | package org.springframework.ai.chat.prompt;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.model.ModelOptions;
   24 | 
   25 | /**
   26 |  * {@link ModelOptions} representing the common options that are portable across different
   27 |  * chat models.
   28 |  */
   29 | public interface ChatOptions extends ModelOptions {
   30 | 
   31 | 	/**
   32 | 	 * Returns the model to use for the chat.
   33 | 	 * @return the model to use for the chat
   34 | 	 */
   35 | 	@Nullable String getModel();
   36 | 
   37 | 	/**
   38 | 	 * Returns the frequency penalty to use for the chat.
   39 | 	 * @return the frequency penalty to use for the chat
   40 | 	 */
   41 | 	@Nullable Double getFrequencyPenalty();
   42 | 
   43 | 	/**
   44 | 	 * Returns the maximum number of tokens to use for the chat.
   45 | 	 * @return the maximum number of tokens to use for the chat
   46 | 	 */
   47 | 	@Nullable Integer getMaxTokens();
   48 | 
   49 | 	/**
   50 | 	 * Returns the presence penalty to use for the chat.
   51 | 	 * @return the presence penalty to use for the chat
   52 | 	 */
   53 | 	@Nullable Double getPresencePenalty();
   54 | 
   55 | 	/**
   56 | 	 * Returns the stop sequences to use for the chat.
   57 | 	 * @return the stop sequences to use for the chat
   58 | 	 */
   59 | 	@Nullable List<String> getStopSequences();
   60 | 
   61 | 	/**
   62 | 	 * Returns the temperature to use for the chat.
   63 | 	 * @return the temperature to use for the chat
   64 | 	 */
   65 | 	@Nullable Double getTemperature();
   66 | 
   67 | 	/**
   68 | 	 * Returns the top K to use for the chat.
   69 | 	 * @return the top K to use for the chat
   70 | 	 */
   71 | 	@Nullable Integer getTopK();
   72 | 
   73 | 	/**
   74 | 	 * Returns the top P to use for the chat.
   75 | 	 * @return the top P to use for the chat
   76 | 	 */
   77 | 	@Nullable Double getTopP();
   78 | 
   79 | 	/**
   80 | 	 * Returns a new {@link Builder} initialized with the values of this
   81 | 	 * {@link ChatOptions}.
   82 | 	 *
   83 | 	 * Concrete ChatOptions classes must implement this and return the most concrete
   84 | 	 * builder implementation.
   85 | 	 */
   86 | 	ChatOptions.Builder<?> mutate();
   87 | 
   88 | 	/**
   89 | 	 * Creates a new {@link Builder} to create the default {@link ChatOptions}.
   90 | 	 * @return Returns a new {@link Builder}.
   91 | 	 */
   92 | 	static ChatOptions.Builder<?> builder() {
   93 | 		return new DefaultChatOptionsBuilder<>();
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Builder for creating {@link ChatOptions} instance.
   98 | 	 */
   99 | 	interface Builder<B extends Builder<B>> extends Cloneable {
  100 | 
  101 | 		B clone();
  102 | 
  103 | 		/**
  104 | 		 * Builds with the model to use for the chat.
  105 | 		 * @param model
  106 | 		 * @return the builder
  107 | 		 */
  108 | 		B model(@Nullable String model);
  109 | 
  110 | 		/**
  111 | 		 * Builds with the frequency penalty to use for the chat.
  112 | 		 * @param frequencyPenalty
  113 | 		 * @return the builder.
  114 | 		 */
  115 | 		B frequencyPenalty(@Nullable Double frequencyPenalty);
  116 | 
  117 | 		/**
  118 | 		 * Builds with the maximum number of tokens to use for the chat.
  119 | 		 * @param maxTokens
  120 | 		 * @return the builder.
  121 | 		 */
  122 | 		B maxTokens(@Nullable Integer maxTokens);
  123 | 
  124 | 		/**
  125 | 		 * Builds with the presence penalty to use for the chat.
  126 | 		 * @param presencePenalty
  127 | 		 * @return the builder.
  128 | 		 */
  129 | 		B presencePenalty(@Nullable Double presencePenalty);
  130 | 
  131 | 		/**
  132 | 		 * Builds with the stop sequences to use for the chat.
  133 | 		 * @param stopSequences
  134 | 		 * @return the builder.
  135 | 		 */
  136 | 		B stopSequences(@Nullable List<String> stopSequences);
  137 | 
  138 | 		/**
  139 | 		 * Builds with the temperature to use for the chat.
  140 | 		 * @param temperature
  141 | 		 * @return the builder.
  142 | 		 */
  143 | 		B temperature(@Nullable Double temperature);
  144 | 
  145 | 		/**
  146 | 		 * Builds with the top K to use for the chat.
  147 | 		 * @param topK
  148 | 		 * @return the builder.
  149 | 		 */
  150 | 		B topK(@Nullable Integer topK);
  151 | 
  152 | 		/**
  153 | 		 * Builds with the top P to use for the chat.
  154 | 		 * @param topP
  155 | 		 * @return the builder.
  156 | 		 */
  157 | 		B topP(@Nullable Double topP);
  158 | 
  159 | 		/**
  160 | 		 * Build the {@link ChatOptions}.
  161 | 		 * @return the Chat options.
  162 | 		 */
  163 | 		ChatOptions build();
  164 | 
  165 | 		/**
  166 | 		 * Mutate this builder by taking all {@code other}'s values that are non-null,
  167 | 		 * retaining {@code this} other values.
  168 | 		 */
  169 | 		B combineWith(ChatOptions.Builder<?> other);
  170 | 
  171 | 	}
  172 | 
  173 | }
```
