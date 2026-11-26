<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/annotation/Tool.java -->
<!-- 正文引用：[[源码:Tool.java#L起-L止]] ；代码块头部：(src: Tool.java#L起-L止) -->

# Tool.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/annotation/Tool.java` |
| 行数 | 82 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.063Z |

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
   17 | package org.springframework.ai.tool.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
   26 | import org.springframework.ai.tool.execution.ToolCallResultConverter;
   27 | 
   28 | /**
   29 |  * Marks a method as a tool in Spring AI.
   30 |  *
   31 |  * @author Thomas Vitale
   32 |  * @since 1.0.0
   33 |  */
   34 | @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
   35 | @Retention(RetentionPolicy.RUNTIME)
   36 | @Documented
   37 | public @interface Tool {
   38 | 
   39 | 	/**
   40 | 	 * The name of the tool. If not provided, the method name will be used.
   41 | 	 * <p>
   42 | 	 * For maximum compatibility across different LLMs, it is recommended to use only
   43 | 	 * alphanumeric characters, underscores, hyphens, and dots in tool names. Using spaces
   44 | 	 * or special characters may cause issues with some LLMs (e.g., OpenAI).
   45 | 	 * </p>
   46 | 	 * <p>
   47 | 	 * Examples of recommended names: "get_weather", "search-docs", "tool.v1"
   48 | 	 * </p>
   49 | 	 * <p>
   50 | 	 * Examples of names that may cause compatibility issues: "get weather" (contains
   51 | 	 * space), "tool()" (contains parentheses)
   52 | 	 * </p>
   53 | 	 */
   54 | 	String name() default "";
   55 | 
   56 | 	/**
   57 | 	 * The description of the tool. If not provided, the method name will be used.
   58 | 	 */
   59 | 	String description() default "";
   60 | 
   61 | 	/**
   62 | 	 * Whether the tool result should be returned directly or passed back to the model.
   63 | 	 */
   64 | 	boolean returnDirect() default false;
   65 | 
   66 | 	/**
   67 | 	 * Whether a call of the tool needs a person's confirmation before it runs.
   68 | 	 * {@link org.springframework.ai.model.tool.DefaultToolCallingManager} refuses to run
   69 | 	 * such a call without a decision, and the tool-calling loop pauses on it; see
   70 | 	 * {@link org.springframework.ai.model.tool.ToolCallDecisions}. A custom
   71 | 	 * {@link org.springframework.ai.model.tool.ToolCallingManager} has to check it
   72 | 	 * itself.
   73 | 	 * @since 2.1.0
   74 | 	 */
   75 | 	boolean requiresConfirmation() default false;
   76 | 
   77 | 	/**
   78 | 	 * The class to use to convert the tool call result to a String.
   79 | 	 */
   80 | 	Class<? extends ToolCallResultConverter> resultConverter() default DefaultToolCallResultConverter.class;
   81 | 
   82 | }
```
