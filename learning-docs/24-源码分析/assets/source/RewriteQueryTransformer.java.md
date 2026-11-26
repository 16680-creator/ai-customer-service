<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/preretrieval/query/transformation/RewriteQueryTransformer.java -->
<!-- 正文引用：[[源码:RewriteQueryTransformer.java#L起-L止]] ；代码块头部：(src: RewriteQueryTransformer.java#L起-L止) -->

# RewriteQueryTransformer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/preretrieval/query/transformation/RewriteQueryTransformer.java` |
| 行数 | 134 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:19.252Z |

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
   17 | package org.springframework.ai.rag.preretrieval.query.transformation;
   18 | 
   19 | import org.apache.commons.logging.Log;
   20 | import org.apache.commons.logging.LogFactory;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.chat.client.ChatClient;
   24 | import org.springframework.ai.chat.prompt.PromptTemplate;
   25 | import org.springframework.ai.rag.Query;
   26 | import org.springframework.ai.rag.util.PromptAssert;
   27 | import org.springframework.util.Assert;
   28 | import org.springframework.util.StringUtils;
   29 | 
   30 | /**
   31 |  * Uses a large language model to rewrite a user query to provide better results when
   32 |  * querying a target system, such as a vector store or a web search engine.
   33 |  * <p>
   34 |  * This transformer is useful when the user query is verbose, ambiguous, or contains
   35 |  * irrelevant information that may affect the quality of the search results.
   36 |  *
   37 |  * @author Thomas Vitale
   38 |  * @since 1.0.0
   39 |  * @see <a href="https://arxiv.org/pdf/2305.14283">arXiv:2305.14283</a>
   40 |  */
   41 | public class RewriteQueryTransformer implements QueryTransformer {
   42 | 
   43 | 	private static final Log logger = LogFactory.getLog(RewriteQueryTransformer.class);
   44 | 
   45 | 	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
   46 | 			Given a user query, rewrite it to provide better results when querying a {target}.
   47 | 			Remove any irrelevant information, and ensure the query is concise and specific.
   48 | 
   49 | 			Original query:
   50 | 			{query}
   51 | 
   52 | 			Rewritten query:
   53 | 			""");
   54 | 
   55 | 	private static final String DEFAULT_TARGET = "vector store";
   56 | 
   57 | 	private final ChatClient chatClient;
   58 | 
   59 | 	private final PromptTemplate promptTemplate;
   60 | 
   61 | 	private final String targetSearchSystem;
   62 | 
   63 | 	public RewriteQueryTransformer(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate,
   64 | 			@Nullable String targetSearchSystem) {
   65 | 		Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
   66 | 
   67 | 		this.chatClient = chatClientBuilder.build();
   68 | 		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
   69 | 		this.targetSearchSystem = targetSearchSystem != null ? targetSearchSystem : DEFAULT_TARGET;
   70 | 
   71 | 		PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "target", "query");
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public Query transform(Query query) {
   76 | 		Assert.notNull(query, "query cannot be null");
   77 | 
   78 | 		if (logger.isDebugEnabled()) {
   79 | 			logger.debug("Rewriting query to optimize for querying a " + this.targetSearchSystem + ".");
   80 | 		}
   81 | 
   82 | 		var rewrittenQueryText = this.chatClient.prompt()
   83 | 			.user(user -> user.text(this.promptTemplate.getTemplate())
   84 | 				.param("target", this.targetSearchSystem)
   85 | 				.param("query", query.text()))
   86 | 			.call()
   87 | 			.content();
   88 | 
   89 | 		if (!StringUtils.hasText(rewrittenQueryText)) {
   90 | 			logger.warn("Query rewrite result is null/empty. Returning the input query unchanged.");
   91 | 			return query;
   92 | 		}
   93 | 
   94 | 		return query.mutate().text(rewrittenQueryText).build();
   95 | 	}
   96 | 
   97 | 	public static Builder builder() {
   98 | 		return new Builder();
   99 | 	}
  100 | 
  101 | 	public static final class Builder {
  102 | 
  103 | 		private ChatClient.@Nullable Builder chatClientBuilder;
  104 | 
  105 | 		private @Nullable PromptTemplate promptTemplate;
  106 | 
  107 | 		private @Nullable String targetSearchSystem;
  108 | 
  109 | 		private Builder() {
  110 | 		}
  111 | 
  112 | 		public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
  113 | 			this.chatClientBuilder = chatClientBuilder;
  114 | 			return this;
  115 | 		}
  116 | 
  117 | 		public Builder promptTemplate(PromptTemplate promptTemplate) {
  118 | 			this.promptTemplate = promptTemplate;
  119 | 			return this;
  120 | 		}
  121 | 
  122 | 		public Builder targetSearchSystem(String targetSearchSystem) {
  123 | 			this.targetSearchSystem = targetSearchSystem;
  124 | 			return this;
  125 | 		}
  126 | 
  127 | 		public RewriteQueryTransformer build() {
  128 | 			Assert.state(this.chatClientBuilder != null, "chatClientBuilder cannot be null");
  129 | 			return new RewriteQueryTransformer(this.chatClientBuilder, this.promptTemplate, this.targetSearchSystem);
  130 | 		}
  131 | 
  132 | 	}
  133 | 
  134 | }
```
