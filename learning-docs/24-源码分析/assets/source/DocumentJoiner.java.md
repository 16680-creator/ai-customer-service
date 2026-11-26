<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/retrieval/join/DocumentJoiner.java -->
<!-- 正文引用：[[源码:DocumentJoiner.java#L起-L止]] ；代码块头部：(src: DocumentJoiner.java#L起-L止) -->

# DocumentJoiner.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/retrieval/join/DocumentJoiner.java` |
| 行数 | 48 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.302Z |

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
   17 | package org.springframework.ai.rag.retrieval.join;
   18 | 
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.function.Function;
   22 | 
   23 | import org.springframework.ai.document.Document;
   24 | import org.springframework.ai.rag.Query;
   25 | 
   26 | /**
   27 |  * A component for combining documents retrieved based on multiple queries and from
   28 |  * multiple data sources into a single collection of documents. As part of the joining
   29 |  * process, it can also handle duplicate documents and reciprocal ranking strategies.
   30 |  *
   31 |  * @author Thomas Vitale
   32 |  * @since 1.0.0
   33 |  */
   34 | public interface DocumentJoiner extends Function<Map<Query, List<List<Document>>>, List<Document>> {
   35 | 
   36 | 	/**
   37 | 	 * Joins documents retrieved across multiple queries and daa sources.
   38 | 	 * @param documentsForQuery a map of queries and the corresponding list of documents
   39 | 	 * retrieved
   40 | 	 * @return a single collection of documents
   41 | 	 */
   42 | 	List<Document> join(Map<Query, List<List<Document>>> documentsForQuery);
   43 | 
   44 | 	default List<Document> apply(Map<Query, List<List<Document>>> documentsForQuery) {
   45 | 		return join(documentsForQuery);
   46 | 	}
   47 | 
   48 | }
```
