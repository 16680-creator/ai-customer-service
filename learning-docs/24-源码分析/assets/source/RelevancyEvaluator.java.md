<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/evaluation/RelevancyEvaluator.java -->
<!-- 正文引用：[[源码:RelevancyEvaluator.java#L起-L止]] ；代码块头部：(src: RelevancyEvaluator.java#L起-L止) -->

# RelevancyEvaluator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/evaluation/RelevancyEvaluator.java` |
| 行数 | 122 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.598Z |

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
   17 | package org.springframework.ai.chat.evaluation;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.chat.client.ChatClient;
   25 | import org.springframework.ai.chat.prompt.PromptTemplate;
   26 | import org.springframework.ai.evaluation.EvaluationRequest;
   27 | import org.springframework.ai.evaluation.EvaluationResponse;
   28 | import org.springframework.ai.evaluation.Evaluator;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * Evaluates the relevancy of a response to a query based on the context provided.
   33 |  */
   34 | public class RelevancyEvaluator implements Evaluator {
   35 | 
   36 | 	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
   37 | 				Your task is to evaluate if the response for the query
   38 | 				is in line with the context information provided.
   39 | 
   40 | 				You have two options to answer. Either YES or NO.
   41 | 
   42 | 				Answer YES, if the response for the query
   43 | 				is in line with context information otherwise NO.
   44 | 
   45 | 				Query:
   46 | 				{query}
   47 | 
   48 | 				Response:
   49 | 				{response}
   50 | 
   51 | 				Context:
   52 | 				{context}
   53 | 
   54 | 				Answer:
   55 | 			""");
   56 | 
   57 | 	private final ChatClient.Builder chatClientBuilder;
   58 | 
   59 | 	private final PromptTemplate promptTemplate;
   60 | 
   61 | 	public RelevancyEvaluator(ChatClient.Builder chatClientBuilder) {
   62 | 		this(chatClientBuilder, null);
   63 | 	}
   64 | 
   65 | 	private RelevancyEvaluator(ChatClient.Builder chatClientBuilder, @Nullable PromptTemplate promptTemplate) {
   66 | 		Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
   67 | 		this.chatClientBuilder = chatClientBuilder;
   68 | 		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {
   73 | 		var response = evaluationRequest.getResponseContent();
   74 | 		var context = doGetSupportingData(evaluationRequest);
   75 | 
   76 | 		var userMessage = this.promptTemplate
   77 | 			.render(Map.of("query", evaluationRequest.getUserText(), "response", response, "context", context));
   78 | 
   79 | 		String evaluationResponse = this.chatClientBuilder.build().prompt().user(userMessage).call().content();
   80 | 
   81 | 		boolean passing = false;
   82 | 		float score = 0;
   83 | 		String normalizedResponse = (evaluationResponse != null) ? evaluationResponse.strip() : "";
   84 | 		if ("yes".equalsIgnoreCase(normalizedResponse)) {
   85 | 			passing = true;
   86 | 			score = 1;
   87 | 		}
   88 | 
   89 | 		return new EvaluationResponse(passing, score, "", Collections.emptyMap());
   90 | 	}
   91 | 
   92 | 	public static Builder builder() {
   93 | 		return new Builder();
   94 | 	}
   95 | 
   96 | 	public static final class Builder {
   97 | 
   98 | 		private ChatClient.@Nullable Builder chatClientBuilder;
   99 | 
  100 | 		private @Nullable PromptTemplate promptTemplate;
  101 | 
  102 | 		private Builder() {
  103 | 		}
  104 | 
  105 | 		public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
  106 | 			this.chatClientBuilder = chatClientBuilder;
  107 | 			return this;
  108 | 		}
  109 | 
  110 | 		public Builder promptTemplate(PromptTemplate promptTemplate) {
  111 | 			this.promptTemplate = promptTemplate;
  112 | 			return this;
  113 | 		}
  114 | 
  115 | 		public RelevancyEvaluator build() {
  116 | 			Assert.state(this.chatClientBuilder != null, "chatClientBuilder cannot be null");
  117 | 			return new RelevancyEvaluator(this.chatClientBuilder, this.promptTemplate);
  118 | 		}
  119 | 
  120 | 	}
  121 | 
  122 | }
```
