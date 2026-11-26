<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/VectorStoreRetriever.java -->
<!-- 正文引用：[[源码:VectorStoreRetriever.java#L起-L止]] ；代码块头部：(src: VectorStoreRetriever.java#L起-L止) -->

# VectorStoreRetriever.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/VectorStoreRetriever.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:46.767Z |

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
   17 | package org.springframework.ai.vectorstore;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.ai.document.Document;
   22 | 
   23 | /**
   24 |  * A functional interface that provides read-only access to vector store retrieval
   25 |  * operations. This interface extracts only the document retrieval functionality from
   26 |  * {@link VectorStore}, ensuring that mutation operations (add, delete) are not exposed.
   27 |  *
   28 |  * <p>
   29 |  * This is useful when you want to provide retrieval-only access to a vector store,
   30 |  * following the principle of least privilege by not exposing write operations.
   31 |  *
   32 |  * @author Mark Pollack
   33 |  * @since 1.0.0
   34 |  */
   35 | @FunctionalInterface
   36 | public interface VectorStoreRetriever {
   37 | 
   38 | 	/**
   39 | 	 * Retrieves documents by query embedding similarity and metadata filters to retrieve
   40 | 	 * exactly the number of nearest-neighbor results that match the request criteria.
   41 | 	 * @param request Search request for set search parameters, such as the query text,
   42 | 	 * topK, similarity threshold and metadata filter expressions.
   43 | 	 * @return Returns documents that match the query request conditions.
   44 | 	 */
   45 | 	List<Document> similaritySearch(SearchRequest request);
   46 | 
   47 | 	/**
   48 | 	 * Retrieves documents by query embedding similarity using the default
   49 | 	 * {@link SearchRequest}'s search criteria.
   50 | 	 * @param query Text to use for embedding similarity comparison.
   51 | 	 * @return Returns a list of documents that have embeddings similar to the query text
   52 | 	 * embedding.
   53 | 	 */
   54 | 	default List<Document> similaritySearch(String query) {
   55 | 		return this.similaritySearch(SearchRequest.builder().query(query).build());
   56 | 	}
   57 | 
   58 | 	/**
   59 | 	 * Retrieves documents by similarity to a query embedding the caller has already
   60 | 	 * computed, instead of embedding query text with the store's own embedding model.
   61 | 	 * <p>
   62 | 	 * This is the search-side counterpart of {@link VectorStore#upsert(List)}: an
   63 | 	 * application that produces its own embeddings can embed the query the same way and
   64 | 	 * search with the result, so the store's embedding model is never involved. The query
   65 | 	 * embedding must come from the same model, with the same settings, as the stored
   66 | 	 * embeddings; otherwise the results are meaningless even though no error is raised.
   67 | 	 * <p>
   68 | 	 * The request supplies the rest of the search: {@code topK}, the similarity threshold
   69 | 	 * and the filter expression. Its query text, if any, is ignored.
   70 | 	 * <p>
   71 | 	 * The default implementation throws {@link UnsupportedOperationException}; each store
   72 | 	 * opts in independently.
   73 | 	 * @param queryEmbedding the query embedding; must not be null or empty, and must
   74 | 	 * contain only finite values
   75 | 	 * @param request the search parameters; its query text is ignored
   76 | 	 * @return the documents most similar to the query embedding that match the request
   77 | 	 * conditions
   78 | 	 * @throws UnsupportedOperationException if the store does not support searching with
   79 | 	 * a query embedding
   80 | 	 * @since 2.1.0
   81 | 	 */
   82 | 	default List<Document> similaritySearch(float[] queryEmbedding, SearchRequest request) {
   83 | 		throw new UnsupportedOperationException(
   84 | 				getClass().getSimpleName() + " does not support similarity search with a query embedding");
   85 | 	}
   86 | 
   87 | }
```
