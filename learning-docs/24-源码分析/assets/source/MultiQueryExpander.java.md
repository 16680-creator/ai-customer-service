<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-rag:2.1.0 | path: org/springframework/ai/rag/preretrieval/query/expansion/MultiQueryExpander.java -->
<!-- 正文引用：[[源码:MultiQueryExpander.java#L起-L止]] ；代码块头部：(src: MultiQueryExpander.java#L起-L止) -->

# MultiQueryExpander.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-rag:2.1.0` |
| 路径 | `org/springframework/ai/rag/preretrieval/query/expansion/MultiQueryExpander.java` |
| 行数 | 184 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-rag/2.1.0/spring-ai-rag-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:19.458Z |

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
   17 | package org.springframework.ai.rag.preretrieval.query.expansion;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | import java.util.stream.Collectors;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.chat.client.ChatClient;
   28 | import org.springframework.ai.chat.prompt.PromptTemplate;
   29 | import org.springframework.ai.rag.Query;
   30 | import org.springframework.ai.rag.util.PromptAssert;
   31 | import org.springframework.util.Assert;
   32 | import org.springframework.util.CollectionUtils;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * Uses a large language model to expand a query into multiple semantically diverse
   37 |  * variations to capture different perspectives, useful for retrieving additional
   38 |  * contextual information and increasing the chances of finding relevant results.
   39 |  *
   40 |  * <p>
   41 |  * Example usage: <pre>{@code
   42 |  * MultiQueryExpander expander = MultiQueryExpander.builder()
   43 |  *    .chatClientBuilder(chatClientBuilder)
   44 |  *    .numberOfQueries(3)
   45 |  *    .build();
   46 |  * List<Query> queries = expander.expand(new Query("How to run a Spring Boot app?"));
   47 |  * }</pre>
   48 |  *
   49 |  * @author Thomas Vitale
   50 |  * @since 1.0.0
   51 |  */
   52 | public final class MultiQueryExpander implements QueryExpander {
   53 | 
   54 | 	private static final Log logger = LogFactory.getLog(MultiQueryExpander.class);
   55 | 
   56 | 	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
   57 | 			You are an expert at information retrieval and search optimization.
   58 | 			Your task is to generate {number} different versions of the given query.
   59 | 
   60 | 			Each variant must cover different perspectives or aspects of the topic,
   61 | 			while maintaining the core intent of the original query. The goal is to
   62 | 			expand the search space and improve the chances of finding relevant information.
   63 | 
   64 | 			Do not explain your choices or add any other text.
   65 | 			Provide the query variants separated by newlines.
   66 | 
   67 | 			Original query: {query}
   68 | 
   69 | 			Query variants:
   70 | 			""");
   71 | 
   72 | 	private static final Boolean DEFAULT_INCLUDE_ORIGINAL = true;
   73 | 
   74 | 	private static final Integer DEFAULT_NUMBER_OF_QUERIES = 3;
   75 | 
   76 | 	private final ChatClient chatClient;
   77 | 
   78 | 	private final PromptTemplate promptTemplate;
   79 | 
   80 | 	private final boolean includeOriginal;
   81 | 
   82 | 	private final int numberOfQueries;
   83 | 
   84 | 	public MultiQueryExpander(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate,
   85 | 			@Nullable Boolean includeOriginal, @Nullable Integer numberOfQueries) {
   86 | 		Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
   87 | 
   88 | 		this.chatClient = chatClientBuilder.build();
   89 | 		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
   90 | 		this.includeOriginal = includeOriginal != null ? includeOriginal : DEFAULT_INCLUDE_ORIGINAL;
   91 | 		this.numberOfQueries = numberOfQueries != null ? numberOfQueries : DEFAULT_NUMBER_OF_QUERIES;
   92 | 
   93 | 		PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "number", "query");
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public List<Query> expand(Query query) {
   98 | 		Assert.notNull(query, "query cannot be null");
   99 | 
  100 | 		if (logger.isDebugEnabled()) {
  101 | 			logger.debug("Generating " + this.numberOfQueries + " query variants");
  102 | 		}
  103 | 
  104 | 		var response = this.chatClient.prompt()
  105 | 			.user(user -> user.text(this.promptTemplate.getTemplate())
  106 | 				.param("number", this.numberOfQueries)
  107 | 				.param("query", query.text()))
  108 | 			.call()
  109 | 			.content();
  110 | 
  111 | 		if (response == null) {
  112 | 			logger.warn("Query expansion result is null. Returning the input query unchanged.");
  113 | 			return List.of(query);
  114 | 		}
  115 | 
  116 | 		var queryVariants = Arrays.asList(response.split("\n"));
  117 | 
  118 | 		if (CollectionUtils.isEmpty(queryVariants) || this.numberOfQueries != queryVariants.size()) {
  119 | 			if (logger.isWarnEnabled()) {
  120 | 				logger.warn("Query expansion result does not contain the requested " + this.numberOfQueries
  121 | 						+ " variants. Returning the input query unchanged.");
  122 | 			}
  123 | 			return List.of(query);
  124 | 		}
  125 | 
  126 | 		var queries = queryVariants.stream()
  127 | 			.filter(StringUtils::hasText)
  128 | 			.map(queryText -> query.mutate().text(queryText).build())
  129 | 			.collect(Collectors.toList());
  130 | 
  131 | 		if (this.includeOriginal) {
  132 | 			logger.debug("Including the original query in the result");
  133 | 			queries.add(0, query);
  134 | 		}
  135 | 
  136 | 		return queries;
  137 | 	}
  138 | 
  139 | 	public static Builder builder() {
  140 | 		return new Builder();
  141 | 	}
  142 | 
  143 | 	public static final class Builder {
  144 | 
  145 | 		private ChatClient.@Nullable Builder chatClientBuilder;
  146 | 
  147 | 		private @Nullable PromptTemplate promptTemplate;
  148 | 
  149 | 		private @Nullable Boolean includeOriginal;
  150 | 
  151 | 		private @Nullable Integer numberOfQueries;
  152 | 
  153 | 		private Builder() {
  154 | 		}
  155 | 
  156 | 		public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
  157 | 			this.chatClientBuilder = chatClientBuilder;
  158 | 			return this;
  159 | 		}
  160 | 
  161 | 		public Builder promptTemplate(PromptTemplate promptTemplate) {
  162 | 			this.promptTemplate = promptTemplate;
  163 | 			return this;
  164 | 		}
  165 | 
  166 | 		public Builder includeOriginal(Boolean includeOriginal) {
  167 | 			this.includeOriginal = includeOriginal;
  168 | 			return this;
  169 | 		}
  170 | 
  171 | 		public Builder numberOfQueries(Integer numberOfQueries) {
  172 | 			this.numberOfQueries = numberOfQueries;
  173 | 			return this;
  174 | 		}
  175 | 
  176 | 		public MultiQueryExpander build() {
  177 | 			Assert.state(this.chatClientBuilder != null, "chatClientBuilder cannot be null");
  178 | 			return new MultiQueryExpander(this.chatClientBuilder, this.promptTemplate, this.includeOriginal,
  179 | 					this.numberOfQueries);
  180 | 		}
  181 | 
  182 | 	}
  183 | 
  184 | }
```
