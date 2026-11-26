<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/retrieval/search/DocumentRetriever.java -->
<!-- 正文引用：[[源码:DocumentRetriever.java#L起-L止]] ；代码块头部：(src: DocumentRetriever.java#L起-L止) -->

# DocumentRetriever.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/retrieval/search/DocumentRetriever.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:19.023Z |

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
   17 | package org.springframework.ai.rag.retrieval.search;
   18 | 
   19 | import java.util.List;
   20 | import java.util.function.Function;
   21 | 
   22 | import org.springframework.ai.document.Document;
   23 | import org.springframework.ai.rag.Query;
   24 | 
   25 | /**
   26 |  * Component responsible for retrieving {@link Document}s from an underlying data source,
   27 |  * such as a search engine, a vector store, a database, or a knowledge graph.
   28 |  *
   29 |  * @author Christian Tzolov
   30 |  * @author Thomas Vitale
   31 |  * @since 1.0.0
   32 |  */
   33 | public interface DocumentRetriever extends Function<Query, List<Document>> {
   34 | 
   35 | 	/**
   36 | 	 * Retrieves relevant documents from an underlying data source based on the given
   37 | 	 * query.
   38 | 	 * @param query The query to use for retrieving documents
   39 | 	 * @return The list of relevant documents
   40 | 	 */
   41 | 	List<Document> retrieve(Query query);
   42 | 
   43 | 	default List<Document> apply(Query query) {
   44 | 		return retrieve(query);
   45 | 	}
   46 | 
   47 | }
```
