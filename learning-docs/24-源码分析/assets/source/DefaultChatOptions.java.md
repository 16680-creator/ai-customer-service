<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/prompt/DefaultChatOptions.java -->
<!-- 正文引用：[[源码:DefaultChatOptions.java#L起-L止]] ；代码块头部：(src: DefaultChatOptions.java#L起-L止) -->

# DefaultChatOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/prompt/DefaultChatOptions.java` |
| 行数 | 134 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.447Z |

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
   20 | import java.util.Objects;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | /**
   25 |  * Default implementation for the {@link ChatOptions}.
   26 |  */
   27 | public class DefaultChatOptions implements ChatOptions {
   28 | 
   29 | 	private final @Nullable String model;
   30 | 
   31 | 	private final @Nullable Double frequencyPenalty;
   32 | 
   33 | 	private final @Nullable Integer maxTokens;
   34 | 
   35 | 	private final @Nullable Double presencePenalty;
   36 | 
   37 | 	private final @Nullable List<String> stopSequences;
   38 | 
   39 | 	private final @Nullable Double temperature;
   40 | 
   41 | 	private final @Nullable Integer topK;
   42 | 
   43 | 	private final @Nullable Double topP;
   44 | 
   45 | 	protected DefaultChatOptions(@Nullable String model, @Nullable Double frequencyPenalty, @Nullable Integer maxTokens,
   46 | 			@Nullable Double presencePenalty, @Nullable List<String> stopSequences, @Nullable Double temperature,
   47 | 			@Nullable Integer topK, @Nullable Double topP) {
   48 | 		this.model = model;
   49 | 		this.frequencyPenalty = frequencyPenalty;
   50 | 		this.maxTokens = maxTokens;
   51 | 		this.presencePenalty = presencePenalty;
   52 | 		this.stopSequences = stopSequences != null ? List.copyOf(stopSequences) : null;
   53 | 		this.temperature = temperature;
   54 | 		this.topK = topK;
   55 | 		this.topP = topP;
   56 | 	}
   57 | 
   58 | 	@Override
   59 | 	public @Nullable String getModel() {
   60 | 		return this.model;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public @Nullable Double getFrequencyPenalty() {
   65 | 		return this.frequencyPenalty;
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public @Nullable Integer getMaxTokens() {
   70 | 		return this.maxTokens;
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public @Nullable Double getPresencePenalty() {
   75 | 		return this.presencePenalty;
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public @Nullable List<String> getStopSequences() {
   80 | 		return this.stopSequences;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public @Nullable Double getTemperature() {
   85 | 		return this.temperature;
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public @Nullable Integer getTopK() {
   90 | 		return this.topK;
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public @Nullable Double getTopP() {
   95 | 		return this.topP;
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public ChatOptions.Builder<?> mutate() {
  100 | 		return ChatOptions.builder()
  101 | 			.model(this.model)
  102 | 			.frequencyPenalty(this.frequencyPenalty)
  103 | 			.maxTokens(this.maxTokens)
  104 | 			.presencePenalty(this.presencePenalty)
  105 | 			.stopSequences(this.stopSequences)
  106 | 			.temperature(this.temperature)
  107 | 			.topK(this.topK)
  108 | 			.topP(this.topP);
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	public boolean equals(@Nullable Object o) {
  113 | 		if (this == o) {
  114 | 			return true;
  115 | 		}
  116 | 		if (o == null || getClass() != o.getClass()) {
  117 | 			return false;
  118 | 		}
  119 | 		DefaultChatOptions that = (DefaultChatOptions) o;
  120 | 		return Objects.equals(this.model, that.model) && Objects.equals(this.frequencyPenalty, that.frequencyPenalty)
  121 | 				&& Objects.equals(this.maxTokens, that.maxTokens)
  122 | 				&& Objects.equals(this.presencePenalty, that.presencePenalty)
  123 | 				&& Objects.equals(this.stopSequences, that.stopSequences)
  124 | 				&& Objects.equals(this.temperature, that.temperature) && Objects.equals(this.topK, that.topK)
  125 | 				&& Objects.equals(this.topP, that.topP);
  126 | 	}
  127 | 
  128 | 	@Override
  129 | 	public int hashCode() {
  130 | 		return Objects.hash(this.model, this.frequencyPenalty, this.maxTokens, this.presencePenalty, this.stopSequences,
  131 | 				this.temperature, this.topK, this.topP);
  132 | 	}
  133 | 
  134 | }
```
