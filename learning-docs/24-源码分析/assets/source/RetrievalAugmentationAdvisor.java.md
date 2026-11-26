<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java -->
<!-- 正文引用：[[源码:RetrievalAugmentationAdvisor.java#L起-L止]] ；代码块头部：(src: RetrievalAugmentationAdvisor.java#L起-L止) -->

# RetrievalAugmentationAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java` |
| 行数 | 297 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:47.710Z |

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
   17 | package org.springframework.ai.rag.advisor;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.HashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | import java.util.concurrent.CompletableFuture;
   25 | import java.util.stream.Collectors;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | import reactor.core.scheduler.Scheduler;
   29 | 
   30 | import org.springframework.ai.chat.client.ChatClientRequest;
   31 | import org.springframework.ai.chat.client.ChatClientResponse;
   32 | import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
   33 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   34 | import org.springframework.ai.chat.model.ChatResponse;
   35 | import org.springframework.ai.document.Document;
   36 | import org.springframework.ai.rag.Query;
   37 | import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
   38 | import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
   39 | import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
   40 | import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
   41 | import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
   42 | import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
   43 | import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
   44 | import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
   45 | import org.springframework.core.task.TaskExecutor;
   46 | import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
   47 | import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
   48 | import org.springframework.util.Assert;
   49 | 
   50 | /**
   51 |  * Advisor that implements common Retrieval Augmented Generation (RAG) flows using the
   52 |  * building blocks defined in the {@link org.springframework.ai.rag} package and following
   53 |  * the Modular RAG Architecture.
   54 |  *
   55 |  * @author Christian Tzolov
   56 |  * @author Thomas Vitale
   57 |  * @since 1.0.0
   58 |  * @see <a href="http://export.arxiv.org/abs/2407.21059">arXiv:2407.21059</a>
   59 |  * @see <a href="https://export.arxiv.org/abs/2312.10997">arXiv:2312.10997</a>
   60 |  * @see <a href="https://export.arxiv.org/abs/2410.20878">arXiv:2410.20878</a>
   61 |  */
   62 | public final class RetrievalAugmentationAdvisor implements BaseAdvisor {
   63 | 
   64 | 	public static final String DOCUMENT_CONTEXT = "rag_document_context";
   65 | 
   66 | 	private final List<QueryTransformer> queryTransformers;
   67 | 
   68 | 	private final @Nullable QueryExpander queryExpander;
   69 | 
   70 | 	private final DocumentRetriever documentRetriever;
   71 | 
   72 | 	private final DocumentJoiner documentJoiner;
   73 | 
   74 | 	private final List<DocumentPostProcessor> documentPostProcessors;
   75 | 
   76 | 	private final QueryAugmenter queryAugmenter;
   77 | 
   78 | 	private final TaskExecutor taskExecutor;
   79 | 
   80 | 	private final Scheduler scheduler;
   81 | 
   82 | 	private final int order;
   83 | 
   84 | 	private RetrievalAugmentationAdvisor(@Nullable List<QueryTransformer> queryTransformers,
   85 | 			@Nullable QueryExpander queryExpander, DocumentRetriever documentRetriever,
   86 | 			@Nullable DocumentJoiner documentJoiner, @Nullable List<DocumentPostProcessor> documentPostProcessors,
   87 | 			@Nullable QueryAugmenter queryAugmenter, @Nullable TaskExecutor taskExecutor, @Nullable Scheduler scheduler,
   88 | 			@Nullable Integer order) {
   89 | 		Assert.notNull(documentRetriever, "documentRetriever cannot be null");
   90 | 		Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
   91 | 		this.queryTransformers = queryTransformers != null ? queryTransformers : List.of();
   92 | 		this.queryExpander = queryExpander;
   93 | 		this.documentRetriever = documentRetriever;
   94 | 		this.documentJoiner = documentJoiner != null ? documentJoiner : new ConcatenationDocumentJoiner();
   95 | 		this.documentPostProcessors = documentPostProcessors != null ? documentPostProcessors : List.of();
   96 | 		this.queryAugmenter = queryAugmenter != null ? queryAugmenter : ContextualQueryAugmenter.builder().build();
   97 | 		this.taskExecutor = taskExecutor != null ? taskExecutor : buildDefaultTaskExecutor();
   98 | 		this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
   99 | 		this.order = order != null ? order : 0;
  100 | 	}
  101 | 
  102 | 	public static Builder builder() {
  103 | 		return new Builder();
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public ChatClientRequest before(ChatClientRequest chatClientRequest, @Nullable AdvisorChain advisorChain) {
  108 | 		Map<String, Object> context = new HashMap<>(chatClientRequest.context());
  109 | 
  110 | 		// 0. Create a query from the user text, parameters, and conversation history.
  111 | 		String text = chatClientRequest.prompt().getUserMessage().getText();
  112 | 		Query originalQuery = Query.builder()
  113 | 			.text(Objects.requireNonNullElse(text, ""))
  114 | 			.history(chatClientRequest.prompt().getInstructions())
  115 | 			.context(context)
  116 | 			.build();
  117 | 
  118 | 		// 1. Transform original user query based on a chain of query transformers.
  119 | 		Query transformedQuery = originalQuery;
  120 | 		for (var queryTransformer : this.queryTransformers) {
  121 | 			transformedQuery = queryTransformer.apply(transformedQuery);
  122 | 		}
  123 | 
  124 | 		// 2. Expand query into one or multiple queries.
  125 | 		List<Query> expandedQueries = this.queryExpander != null ? this.queryExpander.expand(transformedQuery)
  126 | 				: List.of(transformedQuery);
  127 | 
  128 | 		// 3. Get similar documents for each query.
  129 | 		Map<Query, List<List<Document>>> documentsForQuery = expandedQueries.stream()
  130 | 			.map(query -> CompletableFuture.supplyAsync(() -> getDocumentsForQuery(query), this.taskExecutor))
  131 | 			.toList()
  132 | 			.stream()
  133 | 			.map(CompletableFuture::join)
  134 | 			.collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));
  135 | 
  136 | 		// 4. Combine documents retrieved based on multiple queries and from multiple data
  137 | 		// sources.
  138 | 		List<Document> documents = this.documentJoiner.join(documentsForQuery);
  139 | 
  140 | 		// 5. Post-process the documents.
  141 | 		for (var documentPostProcessor : this.documentPostProcessors) {
  142 | 			documents = documentPostProcessor.process(originalQuery, documents);
  143 | 		}
  144 | 		context.put(DOCUMENT_CONTEXT, documents);
  145 | 
  146 | 		// 6. Augment user query with the document contextual data.
  147 | 		Query augmentedQuery = this.queryAugmenter.augment(originalQuery, documents);
  148 | 
  149 | 		// 7. Update ChatClientRequest with augmented prompt.
  150 | 		return chatClientRequest.mutate()
  151 | 			.prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
  152 | 			.context(context)
  153 | 			.build();
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Processes a single query by routing it to document retrievers and collecting
  158 | 	 * documents.
  159 | 	 */
  160 | 	private Map.Entry<Query, List<Document>> getDocumentsForQuery(Query query) {
  161 | 		List<Document> documents = this.documentRetriever.retrieve(query);
  162 | 		return Map.entry(query, documents);
  163 | 	}
  164 | 
  165 | 	@Override
  166 | 	public ChatClientResponse after(ChatClientResponse chatClientResponse, @Nullable AdvisorChain advisorChain) {
  167 | 		ChatResponse.Builder chatResponseBuilder;
  168 | 		if (chatClientResponse.chatResponse() == null) {
  169 | 			chatResponseBuilder = ChatResponse.builder();
  170 | 		}
  171 | 		else {
  172 | 			chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
  173 | 		}
  174 | 		Object ctx = chatClientResponse.context().get(DOCUMENT_CONTEXT);
  175 | 		if (ctx != null) {
  176 | 			chatResponseBuilder.metadata(DOCUMENT_CONTEXT, ctx);
  177 | 		}
  178 | 		return ChatClientResponse.builder()
  179 | 			.chatResponse(chatResponseBuilder.build())
  180 | 			.context(chatClientResponse.context())
  181 | 			.build();
  182 | 	}
  183 | 
  184 | 	@Override
  185 | 	public Scheduler getScheduler() {
  186 | 		return this.scheduler;
  187 | 	}
  188 | 
  189 | 	@Override
  190 | 	public int getOrder() {
  191 | 		return this.order;
  192 | 	}
  193 | 
  194 | 	private static TaskExecutor buildDefaultTaskExecutor() {
  195 | 		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
  196 | 		taskExecutor.setThreadNamePrefix("ai-advisor-");
  197 | 		taskExecutor.setCorePoolSize(4);
  198 | 		taskExecutor.setMaxPoolSize(16);
  199 | 		taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
  200 | 		taskExecutor.initialize();
  201 | 		return taskExecutor;
  202 | 	}
  203 | 
  204 | 	public static final class Builder {
  205 | 
  206 | 		private @Nullable List<QueryTransformer> queryTransformers;
  207 | 
  208 | 		private @Nullable QueryExpander queryExpander;
  209 | 
  210 | 		private @Nullable DocumentRetriever documentRetriever;
  211 | 
  212 | 		private @Nullable DocumentJoiner documentJoiner;
  213 | 
  214 | 		private @Nullable List<DocumentPostProcessor> documentPostProcessors;
  215 | 
  216 | 		private @Nullable QueryAugmenter queryAugmenter;
  217 | 
  218 | 		private @Nullable TaskExecutor taskExecutor;
  219 | 
  220 | 		private @Nullable Scheduler scheduler;
  221 | 
  222 | 		private @Nullable Integer order;
  223 | 
  224 | 		private Builder() {
  225 | 		}
  226 | 
  227 | 		public Builder queryTransformers(List<QueryTransformer> queryTransformers) {
  228 | 			Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
  229 | 			this.queryTransformers = queryTransformers;
  230 | 			return this;
  231 | 		}
  232 | 
  233 | 		public Builder queryTransformers(QueryTransformer... queryTransformers) {
  234 | 			Assert.notNull(queryTransformers, "queryTransformers cannot be null");
  235 | 			Assert.noNullElements(queryTransformers, "queryTransformers cannot contain null elements");
  236 | 			this.queryTransformers = Arrays.asList(queryTransformers);
  237 | 			return this;
  238 | 		}
  239 | 
  240 | 		public Builder queryExpander(QueryExpander queryExpander) {
  241 | 			this.queryExpander = queryExpander;
  242 | 			return this;
  243 | 		}
  244 | 
  245 | 		public Builder documentRetriever(DocumentRetriever documentRetriever) {
  246 | 			this.documentRetriever = documentRetriever;
  247 | 			return this;
  248 | 		}
  249 | 
  250 | 		public Builder documentJoiner(DocumentJoiner documentJoiner) {
  251 | 			this.documentJoiner = documentJoiner;
  252 | 			return this;
  253 | 		}
  254 | 
  255 | 		public Builder documentPostProcessors(List<DocumentPostProcessor> documentPostProcessors) {
  256 | 			Assert.noNullElements(documentPostProcessors, "documentPostProcessors cannot contain null elements");
  257 | 			this.documentPostProcessors = documentPostProcessors;
  258 | 			return this;
  259 | 		}
  260 | 
  261 | 		public Builder documentPostProcessors(DocumentPostProcessor... documentPostProcessors) {
  262 | 			Assert.notNull(documentPostProcessors, "documentPostProcessors cannot be null");
  263 | 			Assert.noNullElements(documentPostProcessors, "documentPostProcessors cannot contain null elements");
  264 | 			this.documentPostProcessors = Arrays.asList(documentPostProcessors);
  265 | 			return this;
  266 | 		}
  267 | 
  268 | 		public Builder queryAugmenter(QueryAugmenter queryAugmenter) {
  269 | 			this.queryAugmenter = queryAugmenter;
  270 | 			return this;
  271 | 		}
  272 | 
  273 | 		public Builder taskExecutor(TaskExecutor taskExecutor) {
  274 | 			this.taskExecutor = taskExecutor;
  275 | 			return this;
  276 | 		}
  277 | 
  278 | 		public Builder scheduler(Scheduler scheduler) {
  279 | 			this.scheduler = scheduler;
  280 | 			return this;
  281 | 		}
  282 | 
  283 | 		public Builder order(Integer order) {
  284 | 			this.order = order;
  285 | 			return this;
  286 | 		}
  287 | 
  288 | 		public RetrievalAugmentationAdvisor build() {
  289 | 			Assert.state(this.documentRetriever != null, "documentRetriever cannot be null");
  290 | 			return new RetrievalAugmentationAdvisor(this.queryTransformers, this.queryExpander, this.documentRetriever,
  291 | 					this.documentJoiner, this.documentPostProcessors, this.queryAugmenter, this.taskExecutor,
  292 | 					this.scheduler, this.order);
  293 | 		}
  294 | 
  295 | 	}
  296 | 
  297 | }
```
