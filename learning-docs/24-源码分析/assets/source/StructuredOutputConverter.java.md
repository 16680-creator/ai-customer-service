<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/converter/StructuredOutputConverter.java -->
<!-- 正文引用：[[源码:StructuredOutputConverter.java#L起-L止]] ；代码块头部：(src: StructuredOutputConverter.java#L起-L止) -->

# StructuredOutputConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/converter/StructuredOutputConverter.java` |
| 行数 | 48 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.699Z |

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
   17 | package org.springframework.ai.converter;
   18 | 
   19 | import org.springframework.core.convert.converter.Converter;
   20 | 
   21 | /**
   22 |  * Converts the (raw) LLM output into a structured responses of type. The
   23 |  * {@link FormatProvider#getFormat()} method should provide the LLM prompt description of
   24 |  * the desired format.
   25 |  *
   26 |  * @param <T> Specifies the desired response type.
   27 |  * @author Mark Pollack
   28 |  * @author Christian Tzolov
   29 |  * @author Filip Hrisafov
   30 |  */
   31 | public interface StructuredOutputConverter<T> extends Converter<String, T>, FormatProvider {
   32 | 
   33 | 	/**
   34 | 	 * Constant for when there is no JSON schema available.
   35 | 	 */
   36 | 	String NO_JSON_SCHEMA = "";
   37 | 
   38 | 	/**
   39 | 	 * Returns the JSON schema for the structured output of an LLM call.
   40 | 	 * @return the JSON schema or {@link StructuredOutputConverter#NO_JSON_SCHEMA} if not
   41 | 	 * available
   42 | 	 * @since 2.0.0
   43 | 	 */
   44 | 	default String getJsonSchema() {
   45 | 		return NO_JSON_SCHEMA;
   46 | 	}
   47 | 
   48 | }
```
