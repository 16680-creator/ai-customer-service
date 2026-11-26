<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/preretrieval/query/transformation/QueryTransformer.java -->
<!-- 正文引用：[[源码:QueryTransformer.java#L起-L止]] ；代码块头部：(src: QueryTransformer.java#L起-L止) -->

# QueryTransformer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/preretrieval/query/transformation/QueryTransformer.java` |
| 行数 | 44 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.002Z |

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
   17 | package org.springframework.ai.rag.preretrieval.query.transformation;
   18 | 
   19 | import java.util.function.Function;
   20 | 
   21 | import org.springframework.ai.rag.Query;
   22 | 
   23 | /**
   24 |  * A component for transforming the input query to make it more effective for retrieval
   25 |  * tasks, addressing challenges such as poorly formed queries, ambiguous terms, complex
   26 |  * vocabulary, or unsupported languages.
   27 |  *
   28 |  * @author Thomas Vitale
   29 |  * @since 1.0.0
   30 |  */
   31 | public interface QueryTransformer extends Function<Query, Query> {
   32 | 
   33 | 	/**
   34 | 	 * Transforms the given query according to the implemented strategy.
   35 | 	 * @param query The original query to transform
   36 | 	 * @return The transformed query
   37 | 	 */
   38 | 	Query transform(Query query);
   39 | 
   40 | 	default Query apply(Query query) {
   41 | 		return transform(query);
   42 | 	}
   43 | 
   44 | }
```
