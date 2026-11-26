<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/metadata/ToolMetadata.java -->
<!-- 正文引用：[[源码:ToolMetadata.java#L起-L止]] ；代码块头部：(src: ToolMetadata.java#L起-L止) -->

# ToolMetadata.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/metadata/ToolMetadata.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.999Z |

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
   17 | package org.springframework.ai.tool.metadata;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | 
   21 | import org.springframework.ai.tool.support.ToolUtils;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * Metadata about a tool specification and execution.
   26 |  *
   27 |  * @author Thomas Vitale
   28 |  * @since 1.0.0
   29 |  */
   30 | public interface ToolMetadata {
   31 | 
   32 | 	/**
   33 | 	 * Whether the tool result should be returned directly or passed back to the model.
   34 | 	 */
   35 | 	default boolean returnDirect() {
   36 | 		return false;
   37 | 	}
   38 | 
   39 | 	/**
   40 | 	 * Whether a call of the tool needs a person's confirmation before it runs.
   41 | 	 * {@link org.springframework.ai.model.tool.DefaultToolCallingManager} refuses to run
   42 | 	 * such a call without a decision, and the tool-calling loop pauses on it; see
   43 | 	 * {@link org.springframework.ai.model.tool.ToolCallDecisions}. A custom
   44 | 	 * {@link org.springframework.ai.model.tool.ToolCallingManager} has to check it
   45 | 	 * itself.
   46 | 	 * @since 2.1.0
   47 | 	 */
   48 | 	default boolean requiresConfirmation() {
   49 | 		return false;
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Create a default {@link ToolMetadata} builder.
   54 | 	 */
   55 | 	static DefaultToolMetadata.Builder builder() {
   56 | 		return DefaultToolMetadata.builder();
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Create a default {@link ToolMetadata} instance from a {@link Method}.
   61 | 	 */
   62 | 	static ToolMetadata from(Method method) {
   63 | 		Assert.notNull(method, "method cannot be null");
   64 | 		return DefaultToolMetadata.builder()
   65 | 			.returnDirect(ToolUtils.getToolReturnDirect(method))
   66 | 			.requiresConfirmation(ToolUtils.getToolRequiresConfirmation(method))
   67 | 			.build();
   68 | 	}
   69 | 
   70 | }
```
