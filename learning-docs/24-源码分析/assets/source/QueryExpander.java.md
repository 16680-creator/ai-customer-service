<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/preretrieval/query/expansion/QueryExpander.java -->
<!-- 正文引用：[[源码:QueryExpander.java#L起-L止]] ；代码块头部：(src: QueryExpander.java#L起-L止) -->

# QueryExpander.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/preretrieval/query/expansion/QueryExpander.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.106Z |

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
   17 | package org.springframework.ai.rag.preretrieval.query.expansion;
   18 | 
   19 | import java.util.List;
   20 | import java.util.function.Function;
   21 | 
   22 | import org.springframework.ai.rag.Query;
   23 | 
   24 | /**
   25 |  * A component for expanding the input query into a list of queries, addressing challenges
   26 |  * such as poorly formed queries by providing alternative query formulations, or by
   27 |  * breaking down complex problems into simpler sub-queries.
   28 |  *
   29 |  * @author Thomas Vitale
   30 |  * @since 1.0.0
   31 |  */
   32 | public interface QueryExpander extends Function<Query, List<Query>> {
   33 | 
   34 | 	/**
   35 | 	 * Expands the given query into a list of queries.
   36 | 	 * @param query The original query to be expanded
   37 | 	 * @return A list of expanded queries
   38 | 	 */
   39 | 	List<Query> expand(Query query);
   40 | 
   41 | 	default List<Query> apply(Query query) {
   42 | 		return expand(query);
   43 | 	}
   44 | 
   45 | }
```
