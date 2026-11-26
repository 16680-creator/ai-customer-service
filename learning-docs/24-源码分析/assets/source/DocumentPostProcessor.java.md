<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/postretrieval/document/DocumentPostProcessor.java -->
<!-- 正文引用：[[源码:DocumentPostProcessor.java#L起-L止]] ；代码块头部：(src: DocumentPostProcessor.java#L起-L止) -->

# DocumentPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/postretrieval/document/DocumentPostProcessor.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.400Z |

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
   17 | package org.springframework.ai.rag.postretrieval.document;
   18 | 
   19 | import java.util.List;
   20 | import java.util.function.BiFunction;
   21 | 
   22 | import org.springframework.ai.document.Document;
   23 | import org.springframework.ai.rag.Query;
   24 | 
   25 | /**
   26 |  * A component for post-processing retrieved documents based on a query, addressing
   27 |  * challenges such as "lost-in-the-middle", context length restrictions from the model,
   28 |  * and the need to reduce noise and redundancy in the retrieved information.
   29 |  * <p>
   30 |  * For example, it could rank documents based on their relevance to the query, remove
   31 |  * irrelevant or redundant documents, or compress the content of each document to reduce
   32 |  * noise and redundancy.
   33 |  *
   34 |  * @author Thomas Vitale
   35 |  * @since 1.0.0
   36 |  */
   37 | public interface DocumentPostProcessor extends BiFunction<Query, List<Document>, List<Document>> {
   38 | 
   39 | 	List<Document> process(Query query, List<Document> documents);
   40 | 
   41 | 	default List<Document> apply(Query query, List<Document> documents) {
   42 | 		return process(query, documents);
   43 | 	}
   44 | 
   45 | }
```
