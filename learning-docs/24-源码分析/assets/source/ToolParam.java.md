<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/annotation/ToolParam.java -->
<!-- 正文引用：[[源码:ToolParam.java#L起-L止]] ；代码块头部：(src: ToolParam.java#L起-L止) -->

# ToolParam.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/annotation/ToolParam.java` |
| 行数 | 46 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.134Z |

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
   25 | /**
   26 |  * Marks a tool argument.
   27 |  *
   28 |  * @author Thomas Vitale
   29 |  * @since 1.0.0
   30 |  */
   31 | @Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
   32 | @Retention(RetentionPolicy.RUNTIME)
   33 | @Documented
   34 | public @interface ToolParam {
   35 | 
   36 | 	/**
   37 | 	 * Whether the tool argument is required.
   38 | 	 */
   39 | 	boolean required() default true;
   40 | 
   41 | 	/**
   42 | 	 * The description of the tool argument.
   43 | 	 */
   44 | 	String description() default "";
   45 | 
   46 | }
```
