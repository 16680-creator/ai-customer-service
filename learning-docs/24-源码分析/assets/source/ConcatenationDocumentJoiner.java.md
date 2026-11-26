<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/retrieval/join/ConcatenationDocumentJoiner.java -->
<!-- 正文引用：[[源码:ConcatenationDocumentJoiner.java#L起-L止]] ；代码块头部：(src: ConcatenationDocumentJoiner.java#L起-L止) -->

# ConcatenationDocumentJoiner.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/retrieval/join/ConcatenationDocumentJoiner.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:19.664Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Comparator;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.function.Function;
   24 | import java.util.stream.Collectors;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | 
   29 | import org.springframework.ai.document.Document;
   30 | import org.springframework.ai.rag.Query;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Combines documents retrieved based on multiple queries and from multiple data sources
   35 |  * by concatenating them into a single collection of documents. In case of duplicate
   36 |  * documents, the first occurrence is kept. The score of each document is kept as is. The
   37 |  * result is a list of unique documents sorted by their score in descending order.
   38 |  *
   39 |  * @author Thomas Vitale
   40 |  * @since 1.0.0
   41 |  */
   42 | public class ConcatenationDocumentJoiner implements DocumentJoiner {
   43 | 
   44 | 	private static final Log logger = LogFactory.getLog(ConcatenationDocumentJoiner.class);
   45 | 
   46 | 	@Override
   47 | 	public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
   48 | 		Assert.notNull(documentsForQuery, "documentsForQuery cannot be null");
   49 | 		Assert.noNullElements(documentsForQuery.keySet(), "documentsForQuery cannot contain null keys");
   50 | 		Assert.noNullElements(documentsForQuery.values(), "documentsForQuery cannot contain null values");
   51 | 
   52 | 		logger.debug("Joining documents by concatenation");
   53 | 
   54 | 		return new ArrayList<>(documentsForQuery.values()
   55 | 			.stream()
   56 | 			.flatMap(List::stream)
   57 | 			.flatMap(List::stream)
   58 | 			.collect(Collectors.toMap(Document::getId, Function.identity(), (existing, duplicate) -> existing))
   59 | 			.values()
   60 | 			.stream()
   61 | 			.sorted(Comparator.comparingDouble((Document doc) -> doc.getScore() != null ? doc.getScore() : 0.0)
   62 | 				.reversed())
   63 | 			.toList());
   64 | 	}
   65 | 
   66 | }
```
