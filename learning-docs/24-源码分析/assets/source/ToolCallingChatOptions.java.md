<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallingChatOptions.java -->
<!-- 正文引用：[[源码:ToolCallingChatOptions.java#L起-L止]] ；代码块头部：(src: ToolCallingChatOptions.java#L起-L止) -->

# ToolCallingChatOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallingChatOptions.java` |
| 行数 | 164 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:25.270Z |

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
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.prompt.ChatOptions;
   25 | import org.springframework.ai.tool.ToolCallback;
   26 | import org.springframework.ai.tool.support.ToolUtils;
   27 | import org.springframework.util.Assert;
   28 | import org.springframework.util.CollectionUtils;
   29 | 
   30 | /**
   31 |  * A set of options that can be used to configure the interaction with a chat model,
   32 |  * including tool calling.
   33 |  *
   34 |  * @author Thomas Vitale
   35 |  * @author Ilayaperumal Gopinathan
   36 |  * @author Christian Tzolov
   37 |  * @since 1.0.0
   38 |  */
   39 | public interface ToolCallingChatOptions extends ChatOptions {
   40 | 
   41 | 	/**
   42 | 	 * ToolCallbacks to be registered with the ChatModel.
   43 | 	 */
   44 | 	@Nullable List<ToolCallback> getToolCallbacks();
   45 | 
   46 | 	/**
   47 | 	 * Get the configured tool context.
   48 | 	 * @return the tool context map.
   49 | 	 */
   50 | 	@Nullable Map<String, Object> getToolContext();
   51 | 
   52 | 	/**
   53 | 	 * Returns a new {@link ToolCallingChatOptions.Builder} initialized with the values of
   54 | 	 * this {@link ToolCallingChatOptions}.
   55 | 	 *
   56 | 	 * Narrows the return type of {@link ChatOptions#mutate()} so generic tool calling
   57 | 	 * code can chain methods without casting.
   58 | 	 */
   59 | 	@Override
   60 | 	ToolCallingChatOptions.Builder<?> mutate();
   61 | 
   62 | 	/**
   63 | 	 * A builder to create a new {@link ToolCallingChatOptions} instance.
   64 | 	 */
   65 | 	static ToolCallingChatOptions.Builder<?> builder() {
   66 | 		return new DefaultToolCallingChatOptions.Builder<>();
   67 | 	}
   68 | 
   69 | 	static @Nullable List<ToolCallback> mergeToolCallbacks(@Nullable List<ToolCallback> runtimeToolCallbacks,
   70 | 			@Nullable List<ToolCallback> defaultToolCallbacks) {
   71 | 		if (CollectionUtils.isEmpty(runtimeToolCallbacks)) {
   72 | 			return defaultToolCallbacks != null ? List.copyOf(defaultToolCallbacks) : null;
   73 | 		}
   74 | 		return List.copyOf(runtimeToolCallbacks);
   75 | 	}
   76 | 
   77 | 	static @Nullable Map<String, Object> mergeToolContext(@Nullable Map<String, Object> runtimeToolContext,
   78 | 			@Nullable Map<String, Object> defaultToolContext) {
   79 | 		if (CollectionUtils.isEmpty(runtimeToolContext)) {
   80 | 			return defaultToolContext != null ? Map.copyOf(defaultToolContext) : null;
   81 | 		}
   82 | 		Assert.noNullElements(runtimeToolContext.keySet(), "runtimeToolContext keys cannot be null");
   83 | 		if (CollectionUtils.isEmpty(defaultToolContext)) {
   84 | 			return Map.copyOf(runtimeToolContext);
   85 | 		}
   86 | 		Assert.noNullElements(defaultToolContext.keySet(), "defaultToolContext keys cannot be null");
   87 | 		var mergedToolContext = new java.util.HashMap<>(defaultToolContext);
   88 | 		mergedToolContext.putAll(runtimeToolContext);
   89 | 		return Map.copyOf(mergedToolContext);
   90 | 	}
   91 | 
   92 | 	static void validateToolCallbacks(@Nullable List<ToolCallback> toolCallbacks) {
   93 | 		if (CollectionUtils.isEmpty(toolCallbacks)) {
   94 | 			return;
   95 | 		}
   96 | 		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
   97 | 		if (!duplicateToolNames.isEmpty()) {
   98 | 			throw new IllegalStateException("Multiple tools with the same name (%s) found in ToolCallingChatOptions"
   99 | 				.formatted(String.join(", ", duplicateToolNames)));
  100 | 		}
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * A builder to create a {@link ToolCallingChatOptions} instance.
  105 | 	 */
  106 | 	interface Builder<B extends Builder<B>> extends ChatOptions.Builder<B> {
  107 | 
  108 | 		/**
  109 | 		 * ToolCallbacks to be registered with the ChatModel.
  110 | 		 */
  111 | 		B toolCallbacks(@Nullable List<ToolCallback> toolCallbacks);
  112 | 
  113 | 		/**
  114 | 		 * ToolCallbacks to be registered with the ChatModel.
  115 | 		 */
  116 | 		B toolCallbacks(ToolCallback... toolCallbacks);
  117 | 
  118 | 		/**
  119 | 		 * Add a {@link Map} of context values into tool context.
  120 | 		 * @param context the map representing the tool context.
  121 | 		 * @return the {@link ToolCallingChatOptions} Builder.
  122 | 		 */
  123 | 		B toolContext(@Nullable Map<String, Object> context);
  124 | 
  125 | 		/**
  126 | 		 * Add a specific key/value pair to the tool context.
  127 | 		 * @param key the key to use.
  128 | 		 * @param value the corresponding value.
  129 | 		 * @return the {@link ToolCallingChatOptions} Builder.
  130 | 		 */
  131 | 		B toolContext(String key, Object value);
  132 | 
  133 | 		// ChatOptions.Builder methods
  134 | 
  135 | 		@Override
  136 | 		B model(@Nullable String model);
  137 | 
  138 | 		@Override
  139 | 		B frequencyPenalty(@Nullable Double frequencyPenalty);
  140 | 
  141 | 		@Override
  142 | 		B maxTokens(@Nullable Integer maxTokens);
  143 | 
  144 | 		@Override
  145 | 		B presencePenalty(@Nullable Double presencePenalty);
  146 | 
  147 | 		@Override
  148 | 		B stopSequences(@Nullable List<String> stopSequences);
  149 | 
  150 | 		@Override
  151 | 		B temperature(@Nullable Double temperature);
  152 | 
  153 | 		@Override
  154 | 		B topK(@Nullable Integer topK);
  155 | 
  156 | 		@Override
  157 | 		B topP(@Nullable Double topP);
  158 | 
  159 | 		@Override
  160 | 		ToolCallingChatOptions build();
  161 | 
  162 | 	}
  163 | 
  164 | }
```
