<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/resolution/ToolCallbackResolver.java -->
<!-- 正文引用：[[源码:ToolCallbackResolver.java#L起-L止]] ；代码块头部：(src: ToolCallbackResolver.java#L起-L止) -->

# ToolCallbackResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/resolution/ToolCallbackResolver.java` |
| 行数 | 36 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.171Z |

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
   17 | package org.springframework.ai.tool.resolution;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.ai.tool.ToolCallback;
   22 | 
   23 | /**
   24 |  * A resolver for {@link ToolCallback} instances.
   25 |  *
   26 |  * @author Thomas Vitale
   27 |  * @since 1.0.0
   28 |  */
   29 | public interface ToolCallbackResolver {
   30 | 
   31 | 	/**
   32 | 	 * Resolve the {@link ToolCallback} for the given tool name.
   33 | 	 */
   34 | 	@Nullable ToolCallback resolve(String toolName);
   35 | 
   36 | }
```
