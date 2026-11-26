<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/generation/augmentation/ContextualQueryAugmenter.java -->
<!-- 正文引用：[[源码:ContextualQueryAugmenter.java#L起-L止]] ；代码块头部：(src: ContextualQueryAugmenter.java#L起-L止) -->

# ContextualQueryAugmenter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/generation/augmentation/ContextualQueryAugmenter.java` |
| 行数 | 176 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:48.024Z |

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
   20 | import java.util.Map;
   21 | import java.util.function.Function;
   22 | import java.util.stream.Collectors;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.ai.chat.prompt.PromptTemplate;
   29 | import org.springframework.ai.document.Document;
   30 | import org.springframework.ai.rag.Query;
   31 | import org.springframework.ai.rag.util.PromptAssert;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * Augments the user query with contextual data from the content of the provided
   36 |  * documents.
   37 |  *
   38 |  * <p>
   39 |  * Example usage: <pre>{@code
   40 |  * QueryAugmenter augmenter = ContextualQueryAugmenter.builder()
   41 |  *    .allowEmptyContext(false)
   42 |  *    .build();
   43 |  * Query augmentedQuery = augmenter.augment(query, documents);
   44 |  * }</pre>
   45 |  *
   46 |  * @author Thomas Vitale
   47 |  * @since 1.0.0
   48 |  */
   49 | public final class ContextualQueryAugmenter implements QueryAugmenter {
   50 | 
   51 | 	private static final Log logger = LogFactory.getLog(ContextualQueryAugmenter.class);
   52 | 
   53 | 	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
   54 | 			Context information is below.
   55 | 
   56 | 			---------------------
   57 | 			{context}
   58 | 			---------------------
   59 | 
   60 | 			Given the context information and no prior knowledge, answer the query.
   61 | 
   62 | 			Follow these rules:
   63 | 
   64 | 			1. If the answer is not in the context, just say that you don't know.
   65 | 			2. Avoid statements like "Based on the context..." or "The provided information...".
   66 | 
   67 | 			Query: {query}
   68 | 
   69 | 			Answer:
   70 | 			""");
   71 | 
   72 | 	private static final PromptTemplate DEFAULT_EMPTY_CONTEXT_PROMPT_TEMPLATE = new PromptTemplate("""
   73 | 			The user query is outside your knowledge base.
   74 | 			Politely inform the user that you can't answer it.
   75 | 			""");
   76 | 
   77 | 	private static final boolean DEFAULT_ALLOW_EMPTY_CONTEXT = false;
   78 | 
   79 | 	/**
   80 | 	 * Default document formatter that just joins document text with newlines
   81 | 	 */
   82 | 	private static final Function<List<Document>, String> DEFAULT_DOCUMENT_FORMATTER = documents -> documents.stream()
   83 | 		.map(Document::getText)
   84 | 		.collect(Collectors.joining(System.lineSeparator()));
   85 | 
   86 | 	private final PromptTemplate promptTemplate;
   87 | 
   88 | 	private final PromptTemplate emptyContextPromptTemplate;
   89 | 
   90 | 	private final boolean allowEmptyContext;
   91 | 
   92 | 	private final Function<List<Document>, String> documentFormatter;
   93 | 
   94 | 	public ContextualQueryAugmenter(@Nullable PromptTemplate promptTemplate,
   95 | 			@Nullable PromptTemplate emptyContextPromptTemplate, @Nullable Boolean allowEmptyContext,
   96 | 			@Nullable Function<List<Document>, String> documentFormatter) {
   97 | 		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
   98 | 		this.emptyContextPromptTemplate = emptyContextPromptTemplate != null ? emptyContextPromptTemplate
   99 | 				: DEFAULT_EMPTY_CONTEXT_PROMPT_TEMPLATE;
  100 | 		this.allowEmptyContext = allowEmptyContext != null ? allowEmptyContext : DEFAULT_ALLOW_EMPTY_CONTEXT;
  101 | 		this.documentFormatter = documentFormatter != null ? documentFormatter : DEFAULT_DOCUMENT_FORMATTER;
  102 | 		PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "query", "context");
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public Query augment(Query query, List<Document> documents) {
  107 | 		Assert.notNull(query, "query cannot be null");
  108 | 		Assert.notNull(documents, "documents cannot be null");
  109 | 
  110 | 		logger.debug("Augmenting query with contextual data");
  111 | 
  112 | 		if (documents.isEmpty()) {
  113 | 			return augmentQueryWhenEmptyContext(query);
  114 | 		}
  115 | 
  116 | 		// 1. Collect content from documents.
  117 | 		String documentContext = this.documentFormatter.apply(documents);
  118 | 
  119 | 		// 2. Define prompt parameters.
  120 | 		Map<String, Object> promptParameters = Map.of("query", query.text(), "context", documentContext);
  121 | 
  122 | 		// 3. Augment user prompt with document context.
  123 | 		return new Query(this.promptTemplate.render(promptParameters));
  124 | 	}
  125 | 
  126 | 	private Query augmentQueryWhenEmptyContext(Query query) {
  127 | 		if (this.allowEmptyContext) {
  128 | 			logger.debug("Empty context is allowed. Returning the original query.");
  129 | 			return query;
  130 | 		}
  131 | 		logger.debug("Empty context is not allowed. Returning a specific query for empty context.");
  132 | 		return new Query(this.emptyContextPromptTemplate.render());
  133 | 	}
  134 | 
  135 | 	public static Builder builder() {
  136 | 		return new Builder();
  137 | 	}
  138 | 
  139 | 	public static final class Builder {
  140 | 
  141 | 		private @Nullable PromptTemplate promptTemplate;
  142 | 
  143 | 		private @Nullable PromptTemplate emptyContextPromptTemplate;
  144 | 
  145 | 		private @Nullable Boolean allowEmptyContext;
  146 | 
  147 | 		private @Nullable Function<List<Document>, String> documentFormatter;
  148 | 
  149 | 		public Builder promptTemplate(PromptTemplate promptTemplate) {
  150 | 			this.promptTemplate = promptTemplate;
  151 | 			return this;
  152 | 		}
  153 | 
  154 | 		public Builder emptyContextPromptTemplate(PromptTemplate emptyContextPromptTemplate) {
  155 | 			this.emptyContextPromptTemplate = emptyContextPromptTemplate;
  156 | 			return this;
  157 | 		}
  158 | 
  159 | 		public Builder allowEmptyContext(Boolean allowEmptyContext) {
  160 | 			this.allowEmptyContext = allowEmptyContext;
  161 | 			return this;
  162 | 		}
  163 | 
  164 | 		public Builder documentFormatter(Function<List<Document>, String> documentFormatter) {
  165 | 			this.documentFormatter = documentFormatter;
  166 | 			return this;
  167 | 		}
  168 | 
  169 | 		public ContextualQueryAugmenter build() {
  170 | 			return new ContextualQueryAugmenter(this.promptTemplate, this.emptyContextPromptTemplate,
  171 | 					this.allowEmptyContext, this.documentFormatter);
  172 | 		}
  173 | 
  174 | 	}
  175 | 
  176 | }
```
