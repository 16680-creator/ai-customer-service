<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/definition/ToolDefinition.java -->
<!-- 正文引用：[[源码:ToolDefinition.java#L起-L止]] ；代码块头部：(src: ToolDefinition.java#L起-L止) -->

# ToolDefinition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/definition/ToolDefinition.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.480Z |

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
   17 | package org.springframework.ai.tool.definition;
   18 | 
   19 | /**
   20 |  * Definition used by the AI model to determine when and how to call the tool.
   21 |  *
   22 |  * @author Thomas Vitale
   23 |  * @since 1.0.0
   24 |  */
   25 | public interface ToolDefinition {
   26 | 
   27 | 	/**
   28 | 	 * The tool name. Unique within the tool set provided to a model.
   29 | 	 */
   30 | 	String name();
   31 | 
   32 | 	/**
   33 | 	 * The tool description, used by the AI model to determine what the tool does.
   34 | 	 */
   35 | 	String description();
   36 | 
   37 | 	/**
   38 | 	 * The schema of the parameters used to call the tool.
   39 | 	 */
   40 | 	String inputSchema();
   41 | 
   42 | 	/**
   43 | 	 * Create a default {@link ToolDefinition} builder.
   44 | 	 */
   45 | 	static DefaultToolDefinition.Builder builder() {
   46 | 		return DefaultToolDefinition.builder();
   47 | 	}
   48 | 
   49 | }
```
