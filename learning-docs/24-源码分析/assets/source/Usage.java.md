<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/metadata/Usage.java -->
<!-- 正文引用：[[源码:Usage.java#L起-L止]] ；代码块头部：(src: Usage.java#L起-L止) -->

# Usage.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/metadata/Usage.java` |
| 行数 | 94 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.599Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Abstract Data Type (ADT) encapsulating metadata on the usage of an AI provider's API
   23 |  * per AI request.
   24 |  *
   25 |  * @author John Blum
   26 |  * @author Ilayaperumal Gopinathan
   27 |  * @since 0.7.0
   28 |  */
   29 | public interface Usage {
   30 | 
   31 | 	/**
   32 | 	 * Returns the number of tokens used in the {@literal prompt} of the AI request.
   33 | 	 * @return an {@link Integer} with the number of tokens used in the {@literal prompt}
   34 | 	 * of the AI request.
   35 | 	 * @see #getCompletionTokens()
   36 | 	 */
   37 | 	Integer getPromptTokens();
   38 | 
   39 | 	/**
   40 | 	 * Returns the number of tokens returned in the {@literal generation (aka completion)}
   41 | 	 * of the AI's response.
   42 | 	 * @return an {@link Integer} with the number of tokens returned in the
   43 | 	 * {@literal generation (aka completion)} of the AI's response.
   44 | 	 * @see #getPromptTokens()
   45 | 	 */
   46 | 	Integer getCompletionTokens();
   47 | 
   48 | 	/**
   49 | 	 * Return the total number of tokens from both the {@literal prompt} of an AI request
   50 | 	 * and {@literal generation} of the AI's response.
   51 | 	 * @return the total number of tokens from both the {@literal prompt} of an AI request
   52 | 	 * and {@literal generation} of the AI's response.
   53 | 	 * @see #getPromptTokens()
   54 | 	 * @see #getCompletionTokens()
   55 | 	 */
   56 | 	default Integer getTotalTokens() {
   57 | 		Integer promptTokens = getPromptTokens();
   58 | 		promptTokens = promptTokens != null ? promptTokens : 0;
   59 | 		Integer completionTokens = getCompletionTokens();
   60 | 		completionTokens = completionTokens != null ? completionTokens : 0;
   61 | 		return promptTokens + completionTokens;
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Return the usage data from the underlying model API response.
   66 | 	 * @return the object of type inferred by the API response.
   67 | 	 */
   68 | 	@Nullable Object getNativeUsage();
   69 | 
   70 | 	/**
   71 | 	 * Returns the number of input tokens read from the prompt cache, if the provider
   72 | 	 * supports prompt caching. Cached tokens are tokens that were previously processed
   73 | 	 * and stored by the provider, reducing cost and latency for repeated prompt prefixes.
   74 | 	 * @return the number of cached input tokens read, or {@code null} if the provider
   75 | 	 * does not support prompt caching or no cache hit occurred.
   76 | 	 * @since 2.0.0
   77 | 	 */
   78 | 	default @Nullable Long getCacheReadInputTokens() {
   79 | 		return null;
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Returns the number of input tokens written to the prompt cache, if the provider
   84 | 	 * supports prompt caching. Cache writes occur when new prompt content is cached for
   85 | 	 * the first time.
   86 | 	 * @return the number of input tokens written to cache, or {@code null} if the
   87 | 	 * provider does not support prompt caching or no cache write occurred.
   88 | 	 * @since 2.0.0
   89 | 	 */
   90 | 	default @Nullable Long getCacheWriteInputTokens() {
   91 | 		return null;
   92 | 	}
   93 | 
   94 | }
```
