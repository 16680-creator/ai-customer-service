<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/execution/ToolExecutionException.java -->
<!-- 正文引用：[[源码:ToolExecutionException.java#L起-L止]] ；代码块头部：(src: ToolExecutionException.java#L起-L止) -->

# ToolExecutionException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/execution/ToolExecutionException.java` |
| 行数 | 40 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.822Z |

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
   17 | package org.springframework.ai.tool.execution;
   18 | 
   19 | import org.springframework.ai.tool.definition.ToolDefinition;
   20 | 
   21 | /**
   22 |  * An exception thrown when a tool execution fails.
   23 |  *
   24 |  * @author Thomas Vitale
   25 |  * @since 1.0.0
   26 |  */
   27 | public class ToolExecutionException extends RuntimeException {
   28 | 
   29 | 	private final ToolDefinition toolDefinition;
   30 | 
   31 | 	public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
   32 | 		super(cause.getMessage(), cause);
   33 | 		this.toolDefinition = toolDefinition;
   34 | 	}
   35 | 
   36 | 	public ToolDefinition getToolDefinition() {
   37 | 		return this.toolDefinition;
   38 | 	}
   39 | 
   40 | }
```
