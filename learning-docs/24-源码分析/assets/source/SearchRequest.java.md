<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/SearchRequest.java -->
<!-- 正文引用：[[源码:SearchRequest.java#L起-L止]] ；代码块头部：(src: SearchRequest.java#L起-L止) -->

# SearchRequest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/SearchRequest.java` |
| 行数 | 298 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:46.607Z |

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
   19 | import java.util.Objects;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.document.Document;
   24 | import org.springframework.ai.vectorstore.filter.Filter;
   25 | import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
   26 | import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * Similarity search request. Use the {@link SearchRequest#builder()} to create the
   31 |  * instance of a {@link SearchRequest}.
   32 |  *
   33 |  * @author Christian Tzolov
   34 |  * @author Thomas Vitale
   35 |  * @author Ilayaperumal Gopinathan
   36 |  */
   37 | public class SearchRequest {
   38 | 
   39 | 	/**
   40 | 	 * Similarity threshold that accepts all search scores. A threshold value of 0.0 means
   41 | 	 * any similarity is accepted or disable the similarity threshold filtering. A
   42 | 	 * threshold value of 1.0 means an exact match is required.
   43 | 	 */
   44 | 	public static final double SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0;
   45 | 
   46 | 	/**
   47 | 	 * Default value for the top 'k' similar results to return.
   48 | 	 */
   49 | 	public static final int DEFAULT_TOP_K = 4;
   50 | 
   51 | 	/**
   52 | 	 * Default value is empty string.
   53 | 	 */
   54 | 	private String query = "";
   55 | 
   56 | 	private int topK = DEFAULT_TOP_K;
   57 | 
   58 | 	private double similarityThreshold = SIMILARITY_THRESHOLD_ACCEPT_ALL;
   59 | 
   60 | 	private Filter.@Nullable Expression filterExpression;
   61 | 
   62 | 	/**
   63 | 	 * Copy an existing {@link SearchRequest.Builder} instance.
   64 | 	 * @param originalSearchRequest {@link SearchRequest} instance to copy.
   65 | 	 * @return Returns new {@link SearchRequest.Builder} instance.
   66 | 	 */
   67 | 	public static Builder from(SearchRequest originalSearchRequest) {
   68 | 		return builder().query(originalSearchRequest.getQuery())
   69 | 			.topK(originalSearchRequest.getTopK())
   70 | 			.similarityThreshold(originalSearchRequest.getSimilarityThreshold())
   71 | 			.filterExpression(originalSearchRequest.getFilterExpression());
   72 | 	}
   73 | 
   74 | 	public SearchRequest() {
   75 | 	}
   76 | 
   77 | 	protected SearchRequest(SearchRequest original) {
   78 | 		this.query = original.query;
   79 | 		this.topK = original.topK;
   80 | 		this.similarityThreshold = original.similarityThreshold;
   81 | 		this.filterExpression = original.filterExpression;
   82 | 	}
   83 | 
   84 | 	public String getQuery() {
   85 | 		return this.query;
   86 | 	}
   87 | 
   88 | 	public int getTopK() {
   89 | 		return this.topK;
   90 | 	}
   91 | 
   92 | 	public double getSimilarityThreshold() {
   93 | 		return this.similarityThreshold;
   94 | 	}
   95 | 
   96 | 	public Filter.@Nullable Expression getFilterExpression() {
   97 | 		return this.filterExpression;
   98 | 	}
   99 | 
  100 | 	public boolean hasFilterExpression() {
  101 | 		return this.filterExpression != null;
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	public String toString() {
  106 | 		return "SearchRequest{" + "query='" + this.query + '\'' + ", topK=" + this.topK + ", similarityThreshold="
  107 | 				+ this.similarityThreshold + ", filterExpression=" + this.filterExpression + '}';
  108 | 	}
  109 | 
  110 | 	@Override
  111 | 	public boolean equals(@Nullable Object o) {
  112 | 		if (this == o) {
  113 | 			return true;
  114 | 		}
  115 | 		if (o == null || getClass() != o.getClass()) {
  116 | 			return false;
  117 | 		}
  118 | 		SearchRequest that = (SearchRequest) o;
  119 | 		return this.topK == that.topK && Double.compare(that.similarityThreshold, this.similarityThreshold) == 0
  120 | 				&& Objects.equals(this.query, that.query)
  121 | 				&& Objects.equals(this.filterExpression, that.filterExpression);
  122 | 	}
  123 | 
  124 | 	@Override
  125 | 	public int hashCode() {
  126 | 		return Objects.hash(this.query, this.topK, this.similarityThreshold, this.filterExpression);
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Builder for creating the SearchRequest instance.
  131 | 	 * @return the builder.
  132 | 	 */
  133 | 	public static Builder builder() {
  134 | 		return new Builder();
  135 | 	}
  136 | 
  137 | 	/**
  138 | 	 * SearchRequest Builder.
  139 | 	 */
  140 | 	public static final class Builder {
  141 | 
  142 | 		private final SearchRequest searchRequest = new SearchRequest();
  143 | 
  144 | 		/**
  145 | 		 * @param query Text to use for embedding similarity comparison.
  146 | 		 * @return this builder.
  147 | 		 */
  148 | 		public Builder query(String query) {
  149 | 			Assert.notNull(query, "Query can not be null.");
  150 | 			this.searchRequest.query = query;
  151 | 			return this;
  152 | 		}
  153 | 
  154 | 		/**
  155 | 		 * @param topK the top 'k' similar results to return.
  156 | 		 * @return this builder.
  157 | 		 */
  158 | 		public Builder topK(int topK) {
  159 | 			Assert.isTrue(topK >= 0, "TopK should be positive.");
  160 | 			this.searchRequest.topK = topK;
  161 | 			return this;
  162 | 		}
  163 | 
  164 | 		/**
  165 | 		 * Similarity threshold score to filter the search response by. Only documents
  166 | 		 * with similarity score equal or greater than the 'threshold' will be returned.
  167 | 		 * Note that this is a post-processing step performed on the client not the server
  168 | 		 * side. A threshold value of 0.0 means any similarity is accepted or disable the
  169 | 		 * similarity threshold filtering. A threshold value of 1.0 means an exact match
  170 | 		 * is required.
  171 | 		 * @param threshold The lower bound of the similarity score.
  172 | 		 * @return this builder.
  173 | 		 */
  174 | 		public Builder similarityThreshold(double threshold) {
  175 | 			Assert.isTrue(threshold >= 0 && threshold <= 1, "Similarity threshold must be in [0,1] range.");
  176 | 			this.searchRequest.similarityThreshold = threshold;
  177 | 			return this;
  178 | 		}
  179 | 
  180 | 		/**
  181 | 		 * Sets disables the similarity threshold by setting it to 0.0 - all results are
  182 | 		 * accepted.
  183 | 		 * @return this builder.
  184 | 		 */
  185 | 		public Builder similarityThresholdAll() {
  186 | 			this.searchRequest.similarityThreshold = 0.0;
  187 | 			return this;
  188 | 		}
  189 | 
  190 | 		/**
  191 | 		 * Retrieves documents by query embedding similarity and matching the filters.
  192 | 		 * Value of 'null' means that no metadata filters will be applied to the search.
  193 | 		 *
  194 | 		 * For example if the {@link Document#getMetadata()} schema is:
  195 | 		 *
  196 | 		 * <pre>{@code
  197 | 		 * &#123;
  198 | 		 * "country": <Text>,
  199 | 		 * "city": <Text>,
  200 | 		 * "year": <Number>,
  201 | 		 * "price": <Decimal>,
  202 | 		 * "isActive": <Boolean>
  203 | 		 * &#125;
  204 | 		 * }</pre>
  205 | 		 *
  206 | 		 * you can constrain the search result to only UK countries with isActive=true and
  207 | 		 * year equal or greater 2020. You can build this such metadata filter
  208 | 		 * programmatically like this:
  209 | 		 *
  210 | 		 * <pre>{@code
  211 | 		 * var exp = new Filter.Expression(AND,
  212 | 		 * 		new Expression(EQ, new Key("country"), new Value("UK")),
  213 | 		 * 		new Expression(AND,
  214 | 		 * 				new Expression(GTE, new Key("year"), new Value(2020)),
  215 | 		 * 				new Expression(EQ, new Key("isActive"), new Value(true))));
  216 | 		 * }</pre>
  217 | 		 *
  218 | 		 * The {@link Filter.Expression} is portable across all vector stores.<br/>
  219 | 		 *
  220 | 		 *
  221 | 		 * The {@link FilterExpressionBuilder} is a DSL creating expressions
  222 | 		 * programmatically:
  223 | 		 *
  224 | 		 * <pre>{@code
  225 | 		 * var b = new FilterExpressionBuilder();
  226 | 		 * var exp = b.and(
  227 | 		 * 		b.eq("country", "UK"),
  228 | 		 * 		b.and(
  229 | 		 * 			b.gte("year", 2020),
  230 | 		 * 			b.eq("isActive", true)));
  231 | 		 * }</pre>
  232 | 		 *
  233 | 		 * The {@link FilterExpressionTextParser} converts textual, SQL like filter
  234 | 		 * expression language into {@link Filter.Expression}:
  235 | 		 *
  236 | 		 * <pre>{@code
  237 | 		 * var parser = new FilterExpressionTextParser();
  238 | 		 * var exp = parser.parse("country == 'UK' && isActive == true && year >=2020");
  239 | 		 * }</pre>
  240 | 		 *
  241 | 		 * A parser instance caches what it parses without bound, so see
  242 | 		 * {@link FilterExpressionTextParser} before keeping one around.
  243 | 		 * @param expression {@link Filter.Expression} instance used to define the
  244 | 		 * metadata filter criteria. The 'null' value stands for no expression filters.
  245 | 		 * @return this builder.
  246 | 		 */
  247 | 		public Builder filterExpression(Filter.@Nullable Expression expression) {
  248 | 			this.searchRequest.filterExpression = expression;
  249 | 			return this;
  250 | 		}
  251 | 
  252 | 		/**
  253 | 		 * Document metadata filter expression. For example if your
  254 | 		 * {@link Document#getMetadata()} has a schema like:
  255 | 		 *
  256 | 		 * <pre>{@code
  257 | 		 * &#123;
  258 | 		 * "country": <Text>,
  259 | 		 * "city": <Text>,
  260 | 		 * "year": <Number>,
  261 | 		 * "price": <Decimal>,
  262 | 		 * "isActive": <Boolean>
  263 | 		 * &#125;
  264 | 		 * }</pre>
  265 | 		 *
  266 | 		 * then you can constrain the search result with metadata filter expressions like:
  267 | 		 *
  268 | 		 * <pre>{@code
  269 | 		 * country == 'UK' && year >= 2020 && isActive == true
  270 | 		 * Or
  271 | 		 * country == 'BG' && (city NOT IN ['Sofia', 'Plovdiv'] || price < 134.34)
  272 | 		 * }</pre>
  273 | 		 *
  274 | 		 * This ensures that the response contains only embeddings that match the
  275 | 		 * specified filer criteria. <br/>
  276 | 		 *
  277 | 		 * The declarative, SQL like, filter syntax is portable across all vector stores
  278 | 		 * supporting the filter search feature.<br/>
  279 | 		 *
  280 | 		 * The {@link FilterExpressionTextParser} is used to convert the text filter
  281 | 		 * expression into {@link Filter.Expression}.
  282 | 		 * @param textExpression declarative, portable, SQL like, metadata filter syntax.
  283 | 		 * The 'null' value stands for no expression filters.
  284 | 		 * @return this.builder
  285 | 		 */
  286 | 		public Builder filterExpression(@Nullable String textExpression) {
  287 | 			this.searchRequest.filterExpression = (textExpression != null)
  288 | 					? new FilterExpressionTextParser().parse(textExpression) : null;
  289 | 			return this;
  290 | 		}
  291 | 
  292 | 		public SearchRequest build() {
  293 | 			return this.searchRequest;
  294 | 		}
  295 | 
  296 | 	}
  297 | 
  298 | }
```
