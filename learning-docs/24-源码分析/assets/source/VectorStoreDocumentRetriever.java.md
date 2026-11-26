<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/retrieval/search/VectorStoreDocumentRetriever.java -->
<!-- 正文引用：[[源码:VectorStoreDocumentRetriever.java#L起-L止]] ；代码块头部：(src: VectorStoreDocumentRetriever.java#L起-L止) -->

# VectorStoreDocumentRetriever.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/retrieval/search/VectorStoreDocumentRetriever.java` |
| 行数 | 176 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:47.857Z |

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
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.document.Document;
   25 | import org.springframework.ai.rag.Query;
   26 | import org.springframework.ai.vectorstore.SearchRequest;
   27 | import org.springframework.ai.vectorstore.VectorStore;
   28 | import org.springframework.ai.vectorstore.filter.Filter;
   29 | import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
   30 | import org.springframework.util.Assert;
   31 | import org.springframework.util.StringUtils;
   32 | 
   33 | /**
   34 |  * Retrieves documents from a vector store that are semantically similar to the input
   35 |  * query. It supports filtering based on metadata, similarity threshold, and top-k
   36 |  * results.
   37 |  *
   38 |  * <p>
   39 |  * Example usage: <pre>{@code
   40 |  * VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
   41 |  *     .vectorStore(vectorStore)
   42 |  *     .similarityThreshold(0.73)
   43 |  *     .topK(5)
   44 |  *     .filterExpression(filterExpression)
   45 |  *     .build();
   46 |  * List<Document> documents = retriever.retrieve(new Query("example query"));
   47 |  * }</pre>
   48 |  *
   49 |  * <p>
   50 |  * The {@link #FILTER_EXPRESSION} context key can be used to provide a filter expression
   51 |  * for a specific query. This key accepts either a string representation of a filter
   52 |  * expression or a {@link Filter.Expression} object directly.
   53 |  *
   54 |  * @author Thomas Vitale
   55 |  * @since 1.0.0
   56 |  */
   57 | public final class VectorStoreDocumentRetriever implements DocumentRetriever {
   58 | 
   59 | 	public static final String FILTER_EXPRESSION = "vector_store_filter_expression";
   60 | 
   61 | 	private final VectorStore vectorStore;
   62 | 
   63 | 	private final Double similarityThreshold;
   64 | 
   65 | 	private final Integer topK;
   66 | 
   67 | 	// Supplier to allow for lazy evaluation of the filter expression,
   68 | 	// which may depend on the execution content. For example, you may want to
   69 | 	// filter dynamically based on the current user's identity or tenant ID.
   70 | 	private final Supplier<Filter.Expression> filterExpression;
   71 | 
   72 | 	public VectorStoreDocumentRetriever(VectorStore vectorStore, @Nullable Double similarityThreshold,
   73 | 			@Nullable Integer topK, @Nullable Supplier<Filter.Expression> filterExpression) {
   74 | 		Assert.notNull(vectorStore, "vectorStore cannot be null");
   75 | 		Assert.isTrue(similarityThreshold == null || similarityThreshold >= 0.0,
   76 | 				"similarityThreshold must be equal to or greater than 0.0");
   77 | 		Assert.isTrue(topK == null || topK > 0, "topK must be greater than 0");
   78 | 		this.vectorStore = vectorStore;
   79 | 		this.similarityThreshold = similarityThreshold != null ? similarityThreshold
   80 | 				: SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL;
   81 | 		this.topK = topK != null ? topK : SearchRequest.DEFAULT_TOP_K;
   82 | 		this.filterExpression = filterExpression != null ? filterExpression : () -> null;
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public List<Document> retrieve(Query query) {
   87 | 		Assert.notNull(query, "query cannot be null");
   88 | 		var requestFilterExpression = computeRequestFilterExpression(query);
   89 | 		var searchRequest = SearchRequest.builder()
   90 | 			.query(query.text())
   91 | 			.filterExpression(requestFilterExpression)
   92 | 			.similarityThreshold(this.similarityThreshold)
   93 | 			.topK(this.topK)
   94 | 			.build();
   95 | 		return this.vectorStore.similaritySearch(searchRequest);
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Computes the filter expression to use for the current request.
  100 | 	 * <p>
  101 | 	 * The filter expression can be provided in the query context using the
  102 | 	 * {@link #FILTER_EXPRESSION} key. This key accepts either a string representation of
  103 | 	 * a filter expression or a {@link Filter.Expression} object directly.
  104 | 	 * <p>
  105 | 	 * If no filter expression is provided in the context, the default filter expression
  106 | 	 * configured for this retriever is used.
  107 | 	 * @param query the query containing potential context with filter expression
  108 | 	 * @return the filter expression to use for the request
  109 | 	 */
  110 | 	private Filter.Expression computeRequestFilterExpression(Query query) {
  111 | 		var contextFilterExpression = query.context().get(FILTER_EXPRESSION);
  112 | 		if (contextFilterExpression != null) {
  113 | 			if (contextFilterExpression instanceof Filter.Expression) {
  114 | 				return (Filter.Expression) contextFilterExpression;
  115 | 			}
  116 | 			else if (StringUtils.hasText(contextFilterExpression.toString())) {
  117 | 				return new FilterExpressionTextParser().parse(contextFilterExpression.toString());
  118 | 			}
  119 | 		}
  120 | 		return this.filterExpression.get();
  121 | 	}
  122 | 
  123 | 	public static Builder builder() {
  124 | 		return new Builder();
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Builder for {@link VectorStoreDocumentRetriever}.
  129 | 	 */
  130 | 	public static final class Builder {
  131 | 
  132 | 		private @Nullable VectorStore vectorStore;
  133 | 
  134 | 		private @Nullable Double similarityThreshold;
  135 | 
  136 | 		private @Nullable Integer topK;
  137 | 
  138 | 		private @Nullable Supplier<Filter.Expression> filterExpression;
  139 | 
  140 | 		private Builder() {
  141 | 		}
  142 | 
  143 | 		public Builder vectorStore(VectorStore vectorStore) {
  144 | 			this.vectorStore = vectorStore;
  145 | 			return this;
  146 | 		}
  147 | 
  148 | 		public Builder similarityThreshold(Double similarityThreshold) {
  149 | 			this.similarityThreshold = similarityThreshold;
  150 | 			return this;
  151 | 		}
  152 | 
  153 | 		public Builder topK(Integer topK) {
  154 | 			this.topK = topK;
  155 | 			return this;
  156 | 		}
  157 | 
  158 | 		public Builder filterExpression(Filter.Expression filterExpression) {
  159 | 			this.filterExpression = () -> filterExpression;
  160 | 			return this;
  161 | 		}
  162 | 
  163 | 		public Builder filterExpression(Supplier<Filter.Expression> filterExpression) {
  164 | 			this.filterExpression = filterExpression;
  165 | 			return this;
  166 | 		}
  167 | 
  168 | 		public VectorStoreDocumentRetriever build() {
  169 | 			Assert.state(this.vectorStore != null, "vectorStore cannot be null");
  170 | 			return new VectorStoreDocumentRetriever(this.vectorStore, this.similarityThreshold, this.topK,
  171 | 					this.filterExpression);
  172 | 		}
  173 | 
  174 | 	}
  175 | 
  176 | }
```
