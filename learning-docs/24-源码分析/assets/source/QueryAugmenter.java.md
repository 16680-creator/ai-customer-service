<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/generation/augmentation/QueryAugmenter.java -->
<!-- 正文引用：[[源码:QueryAugmenter.java#L起-L止]] ；代码块头部：(src: QueryAugmenter.java#L起-L止) -->

# QueryAugmenter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/generation/augmentation/QueryAugmenter.java` |
| 行数 | 46 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.205Z |

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
   17 | package org.springframework.ai.rag.generation.augmentation;
   18 | 
   19 | import java.util.List;
   20 | import java.util.function.BiFunction;
   21 | 
   22 | import org.springframework.ai.document.Document;
   23 | import org.springframework.ai.rag.Query;
   24 | 
   25 | /**
   26 |  * A component for augmenting an input query with additional data, useful to provide a
   27 |  * large language model with the necessary context to answer the user query.
   28 |  *
   29 |  * @author Thomas Vitale
   30 |  * @since 1.0.0
   31 |  */
   32 | public interface QueryAugmenter extends BiFunction<Query, List<Document>, Query> {
   33 | 
   34 | 	/**
   35 | 	 * Augments the user query with contextual data.
   36 | 	 * @param query The user query to augment
   37 | 	 * @param documents The contextual data to use for augmentation
   38 | 	 * @return The augmented query
   39 | 	 */
   40 | 	Query augment(Query query, List<Document> documents);
   41 | 
   42 | 	default Query apply(Query query, List<Document> documents) {
   43 | 		return augment(query, documents);
   44 | 	}
   45 | 
   46 | }
```
