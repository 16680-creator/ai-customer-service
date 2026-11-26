<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/DefaultToolCallingChatOptions.java -->
<!-- 正文引用：[[源码:DefaultToolCallingChatOptions.java#L起-L止]] ；代码块头部：(src: DefaultToolCallingChatOptions.java#L起-L止) -->

# DefaultToolCallingChatOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/DefaultToolCallingChatOptions.java` |
| 行数 | 207 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:25.441Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.HashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.chat.prompt.ChatOptions;
   28 | import org.springframework.ai.chat.prompt.DefaultChatOptions;
   29 | import org.springframework.ai.chat.prompt.DefaultChatOptionsBuilder;
   30 | import org.springframework.ai.tool.ToolCallback;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Default implementation of {@link ToolCallingChatOptions}.
   35 |  *
   36 |  * @author Thomas Vitale
   37 |  * @author Sebastien Deleuze
   38 |  * @author Christian Tzolov
   39 |  * @since 1.0.0
   40 |  */
   41 | public class DefaultToolCallingChatOptions extends DefaultChatOptions implements ToolCallingChatOptions {
   42 | 
   43 | 	private final @Nullable List<ToolCallback> toolCallbacks;
   44 | 
   45 | 	private final @Nullable Map<String, Object> toolContext;
   46 | 
   47 | 	protected DefaultToolCallingChatOptions(@Nullable List<ToolCallback> toolCallbacks,
   48 | 			@Nullable Map<String, Object> toolContext, @Nullable String model, @Nullable Double frequencyPenalty,
   49 | 			@Nullable Integer maxTokens, @Nullable Double presencePenalty, @Nullable List<String> stopSequences,
   50 | 			@Nullable Double temperature, @Nullable Integer topK, @Nullable Double topP) {
   51 | 		super(model, frequencyPenalty, maxTokens, presencePenalty, stopSequences, temperature, topK, topP);
   52 | 		this.toolCallbacks = (toolCallbacks != null ? List.copyOf(toolCallbacks) : null);
   53 | 		this.toolContext = (toolContext != null ? Map.copyOf(toolContext) : null);
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public @Nullable List<ToolCallback> getToolCallbacks() {
   58 | 		return this.toolCallbacks;
   59 | 	}
   60 | 
   61 | 	@Override
   62 | 	public @Nullable Map<String, Object> getToolContext() {
   63 | 		return this.toolContext;
   64 | 	}
   65 | 
   66 | 	@Override
   67 | 	public ToolCallingChatOptions.Builder<?> mutate() {
   68 | 		return DefaultToolCallingChatOptions.builder()
   69 | 			.model(getModel())
   70 | 			.frequencyPenalty(getFrequencyPenalty())
   71 | 			.maxTokens(getMaxTokens())
   72 | 			.presencePenalty(getPresencePenalty())
   73 | 			.stopSequences(getStopSequences())
   74 | 			.temperature(getTemperature())
   75 | 			.topK(getTopK())
   76 | 			.topP(getTopP())
   77 | 			.toolCallbacks(getToolCallbacks())
   78 | 			.toolContext(getToolContext());
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public boolean equals(@Nullable Object o) {
   83 | 		if (this == o) {
   84 | 			return true;
   85 | 		}
   86 | 		if (o == null || getClass() != o.getClass()) {
   87 | 			return false;
   88 | 		}
   89 | 		if (!super.equals(o)) {
   90 | 			return false;
   91 | 		}
   92 | 		DefaultToolCallingChatOptions that = (DefaultToolCallingChatOptions) o;
   93 | 		return Objects.equals(this.toolCallbacks, that.toolCallbacks)
   94 | 				&& Objects.equals(this.toolContext, that.toolContext);
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public int hashCode() {
   99 | 		return Objects.hash(super.hashCode(), this.toolCallbacks, this.toolContext);
  100 | 	}
  101 | 
  102 | 	public static Builder<?> builder() {
  103 | 		return new Builder<>();
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Default implementation of {@link ToolCallingChatOptions.Builder}.
  108 | 	 */
  109 | 	public static class Builder<B extends Builder<B>> extends DefaultChatOptionsBuilder<B>
  110 | 			implements ToolCallingChatOptions.Builder<B> {
  111 | 
  112 | 		protected @Nullable List<ToolCallback> toolCallbacks;
  113 | 
  114 | 		protected @Nullable Map<String, Object> toolContext;
  115 | 
  116 | 		@Override
  117 | 		public B clone() {
  118 | 			B copy = super.clone();
  119 | 			copy.toolCallbacks = this.toolCallbacks == null ? null : new ArrayList<>(this.toolCallbacks);
  120 | 			copy.toolContext = this.toolContext == null ? null : new HashMap<>(this.toolContext);
  121 | 			return copy;
  122 | 		}
  123 | 
  124 | 		@Override
  125 | 		public B toolCallbacks(@Nullable List<ToolCallback> toolCallbacks) {
  126 | 			if (toolCallbacks != null) {
  127 | 				this.toolCallbacks = new ArrayList<>(toolCallbacks);
  128 | 			}
  129 | 			else {
  130 | 				this.toolCallbacks = null;
  131 | 			}
  132 | 			return self();
  133 | 		}
  134 | 
  135 | 		@Override
  136 | 		public B toolCallbacks(ToolCallback... toolCallbacks) {
  137 | 			Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
  138 | 			if (this.toolCallbacks == null) {
  139 | 				this.toolCallbacks = new ArrayList<>();
  140 | 			}
  141 | 			this.toolCallbacks.addAll(List.of(toolCallbacks));
  142 | 			return self();
  143 | 		}
  144 | 
  145 | 		@Override
  146 | 		public B toolContext(@Nullable Map<String, Object> context) {
  147 | 			if (context != null) {
  148 | 				if (this.toolContext == null) {
  149 | 					this.toolContext = new HashMap<>();
  150 | 				}
  151 | 				this.toolContext.putAll(context);
  152 | 			}
  153 | 			else {
  154 | 				this.toolContext = null;
  155 | 			}
  156 | 			return self();
  157 | 		}
  158 | 
  159 | 		@Override
  160 | 		public B toolContext(String key, Object value) {
  161 | 			Assert.hasText(key, "key cannot be null");
  162 | 			Assert.notNull(value, "value cannot be null");
  163 | 			if (this.toolContext == null) {
  164 | 				this.toolContext = new HashMap<>();
  165 | 			}
  166 | 			this.toolContext.put(key, value);
  167 | 			return self();
  168 | 		}
  169 | 
  170 | 		@Override
  171 | 		public ToolCallingChatOptions build() {
  172 | 			return new DefaultToolCallingChatOptions(this.toolCallbacks, this.toolContext, this.model,
  173 | 					this.frequencyPenalty, this.maxTokens, this.presencePenalty, this.stopSequences, this.temperature,
  174 | 					this.topK, this.topP);
  175 | 		}
  176 | 
  177 | 		@Override
  178 | 		public B combineWith(ChatOptions.Builder<?> other) {
  179 | 			super.combineWith(other);
  180 | 			if (other instanceof Builder<?> that) {
  181 | 				if (that.toolCallbacks != null) {
  182 | 					if (this.toolCallbacks == null) {
  183 | 						this.toolCallbacks = new ArrayList<>(that.toolCallbacks);
  184 | 					}
  185 | 					else {
  186 | 						List<ToolCallback> merged = new ArrayList<>(this.toolCallbacks);
  187 | 						merged.addAll(that.toolCallbacks);
  188 | 						this.toolCallbacks = merged;
  189 | 					}
  190 | 				}
  191 | 				if (that.toolContext != null) {
  192 | 					if (this.toolContext == null) {
  193 | 						this.toolContext = new HashMap<>(that.toolContext);
  194 | 					}
  195 | 					else {
  196 | 						Map<String, Object> merged = new HashMap<>(this.toolContext);
  197 | 						merged.putAll(that.toolContext);
  198 | 						this.toolContext = merged;
  199 | 					}
  200 | 				}
  201 | 			}
  202 | 			return self();
  203 | 		}
  204 | 
  205 | 	}
  206 | 
  207 | }
```
