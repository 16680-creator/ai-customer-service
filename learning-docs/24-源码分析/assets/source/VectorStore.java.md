<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/VectorStore.java -->
<!-- 正文引用：[[源码:VectorStore.java#L起-L止]] ；代码块头部：(src: VectorStore.java#L起-L止) -->

# VectorStore.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/VectorStore.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:46.425Z |

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
   20 | import java.util.Optional;
   21 | 
   22 | import io.micrometer.observation.ObservationRegistry;
   23 | 
   24 | import org.springframework.ai.document.Document;
   25 | import org.springframework.ai.document.DocumentWriter;
   26 | import org.springframework.ai.embedding.BatchingStrategy;
   27 | import org.springframework.ai.vectorstore.filter.Filter;
   28 | import org.springframework.ai.vectorstore.observation.DefaultVectorStoreObservationConvention;
   29 | import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
   30 | import org.springframework.util.Assert;
   31 | 
   32 | /**
   33 |  * The {@code VectorStore} interface defines the operations for managing and querying
   34 |  * documents in a vector database. It extends {@link DocumentWriter} to support document
   35 |  * writing operations. Vector databases are specialized for AI applications, performing
   36 |  * similarity searches based on vector representations of data rather than exact matches.
   37 |  * This interface allows for adding, deleting, and searching documents based on their
   38 |  * similarity to a given query.
   39 |  */
   40 | public interface VectorStore extends DocumentWriter, VectorStoreRetriever {
   41 | 
   42 | 	default String getName() {
   43 | 		return this.getClass().getSimpleName();
   44 | 	}
   45 | 
   46 | 	/**
   47 | 	 * Adds list of {@link Document}s to the vector store.
   48 | 	 * @param documents the list of documents to store. Throws an exception if the
   49 | 	 * underlying provider checks for duplicate IDs.
   50 | 	 */
   51 | 	void add(List<Document> documents);
   52 | 
   53 | 	@Override
   54 | 	default void accept(List<Document> documents) {
   55 | 		add(documents);
   56 | 	}
   57 | 
   58 | 	/**
   59 | 	 * Upserts a list of {@link EmbeddedDocument}s into the vector store using
   60 | 	 * caller-supplied embeddings, bypassing the store's own embedding model.
   61 | 	 * <p>
   62 | 	 * Write semantics are replace-by-id: if an entry's document id already exists, the
   63 | 	 * existing row is replaced; otherwise a new row is inserted. As a result, an
   64 | 	 * ingestion job that uses stable ids is safe to re-run — the same input yields the
   65 | 	 * same rows with no duplicates. This is in contrast to {@link #add(List)}, whose
   66 | 	 * behavior for a repeated id is left to the underlying provider.
   67 | 	 * <p>
   68 | 	 * An empty list does nothing.
   69 | 	 * <p>
   70 | 	 * If any entry fails, the operation throws. A backend may have written some entries
   71 | 	 * before failing, so callers should treat the batch outcome as unknown and retry the
   72 | 	 * whole batch, which replace-by-id makes safe.
   73 | 	 * <p>
   74 | 	 * The default implementation throws {@link UnsupportedOperationException}; each store
   75 | 	 * opts in independently.
   76 | 	 * @param entries the documents and their pre-computed embeddings to upsert
   77 | 	 * @throws UnsupportedOperationException if the store does not support upsert
   78 | 	 * @since 2.1.0
   79 | 	 */
   80 | 	default void upsert(List<EmbeddedDocument> entries) {
   81 | 		throw new UnsupportedOperationException(getName() + " does not support upsert");
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * Deletes documents from the vector store.
   86 | 	 * @param idList list of document ids for which documents will be removed.
   87 | 	 */
   88 | 	void delete(List<String> idList);
   89 | 
   90 | 	/**
   91 | 	 * Deletes documents from the vector store based on filter criteria.
   92 | 	 * @param filterExpression Filter expression to identify documents to delete
   93 | 	 * @throws IllegalStateException if the underlying delete causes an exception
   94 | 	 */
   95 | 	void delete(Filter.Expression filterExpression);
   96 | 
   97 | 	/**
   98 | 	 * Deletes documents from the vector store using a string filter expression. Converts
   99 | 	 * the string filter to an Expression object and delegates to
  100 | 	 * {@link #delete(Filter.Expression)}.
  101 | 	 * @param filterExpression String representation of the filter criteria
  102 | 	 * @throws IllegalArgumentException if the filter expression is null
  103 | 	 * @throws IllegalStateException if the underlying delete causes an exception
  104 | 	 */
  105 | 	default void delete(String filterExpression) {
  106 | 		SearchRequest searchRequest = SearchRequest.builder().filterExpression(filterExpression).build();
  107 | 		Filter.Expression textExpression = searchRequest.getFilterExpression();
  108 | 		Assert.notNull(textExpression, "Filter expression must not be null");
  109 | 		this.delete(textExpression);
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Returns the native client if available in this vector store implementation.
  114 | 	 *
  115 | 	 * Note on usage: 1. Returns empty Optional when no native client is available 2. Due
  116 | 	 * to Java type erasure, runtime type checking is not possible
  117 | 	 *
  118 | 	 * Example usage: When working with implementation with known native client:
  119 | 	 * Optional<NativeClientType> client = vectorStore.getNativeClient();
  120 | 	 *
  121 | 	 * Note: Using Optional<?> will return the native client if one exists, rather than an
  122 | 	 * empty Optional. For type safety, prefer using the specific client type.
  123 | 	 * @return Optional containing native client if available, empty Optional otherwise
  124 | 	 * @param <T> The type of the native client
  125 | 	 */
  126 | 	default <T> Optional<T> getNativeClient() {
  127 | 		return Optional.empty();
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Builder interface for creating VectorStore instances. Implements a fluent builder
  132 | 	 * pattern for configuring observation-related settings.
  133 | 	 *
  134 | 	 * @param <T> the concrete builder type, enabling method chaining with the correct
  135 | 	 * return type
  136 | 	 */
  137 | 	interface Builder<T extends Builder<T>> {
  138 | 
  139 | 		/**
  140 | 		 * Sets the registry for collecting observations and metrics. Defaults to
  141 | 		 * {@link ObservationRegistry#NOOP} if not specified.
  142 | 		 * @param observationRegistry the registry to use for observations
  143 | 		 * @return the builder instance for method chaining
  144 | 		 */
  145 | 		T observationRegistry(ObservationRegistry observationRegistry);
  146 | 
  147 | 		/**
  148 | 		 * Sets a custom convention for creating observations. If not specified,
  149 | 		 * {@link DefaultVectorStoreObservationConvention} will be used.
  150 | 		 * @param convention the custom observation convention to use
  151 | 		 * @return the builder instance for method chaining
  152 | 		 */
  153 | 		T customObservationConvention(VectorStoreObservationConvention convention);
  154 | 
  155 | 		/**
  156 | 		 * Sets the batching strategy.
  157 | 		 * @param batchingStrategy the strategy to use
  158 | 		 * @return the builder instance for method chaining
  159 | 		 */
  160 | 		T batchingStrategy(BatchingStrategy batchingStrategy);
  161 | 
  162 | 		/**
  163 | 		 * Builds and returns a new VectorStore instance with the configured settings.
  164 | 		 * @return a new VectorStore instance
  165 | 		 */
  166 | 		VectorStore build();
  167 | 
  168 | 	}
  169 | 
  170 | }
```
