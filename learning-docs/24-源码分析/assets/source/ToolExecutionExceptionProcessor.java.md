<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/execution/ToolExecutionExceptionProcessor.java -->
<!-- 正文引用：[[源码:ToolExecutionExceptionProcessor.java#L起-L止]] ；代码块头部：(src: ToolExecutionExceptionProcessor.java#L起-L止) -->

# ToolExecutionExceptionProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/execution/ToolExecutionExceptionProcessor.java` |
| 行数 | 36 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.920Z |

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
   19 | /**
   20 |  * A functional interface to process a {@link ToolExecutionException} by either converting
   21 |  * the error message to a String that can be sent back to the AI model or throwing an
   22 |  * exception to be handled by the caller.
   23 |  *
   24 |  * @author Thomas Vitale
   25 |  * @since 1.0.0
   26 |  */
   27 | @FunctionalInterface
   28 | public interface ToolExecutionExceptionProcessor {
   29 | 
   30 | 	/**
   31 | 	 * Convert an exception thrown by a tool to a String that can be sent back to the AI
   32 | 	 * model or throw an exception to be handled by the caller.
   33 | 	 */
   34 | 	String process(ToolExecutionException exception);
   35 | 
   36 | }
```
