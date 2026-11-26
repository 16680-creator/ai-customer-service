<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DecisionResponse.java -->
<!-- 正文引用：[[源码:DecisionResponse.java#L起-L止]] ；代码块头部：(src: DecisionResponse.java#L起-L止) -->

# DecisionResponse.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DecisionResponse.java` |
| 行数 | 286 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:07.965Z |

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
   17 | package org.springframework.ai.decision;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.LinkedHashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.ai.decision.answer.ChoiceAnswer;
   29 | import org.springframework.ai.decision.answer.DecisionAnswer;
   30 | import org.springframework.ai.decision.answer.RefusalAnswer;
   31 | import org.springframework.ai.decision.answer.ScoreAnswer;
   32 | import org.springframework.ai.decision.answer.YesNoAnswer;
   33 | import org.springframework.ai.decision.question.ChoiceQuestion;
   34 | import org.springframework.ai.decision.question.DecisionQuestion;
   35 | import org.springframework.ai.decision.question.ScoreQuestion;
   36 | import org.springframework.ai.decision.question.YesNoQuestion;
   37 | import org.springframework.ai.model.ModelResponse;
   38 | import org.springframework.util.Assert;
   39 | 
   40 | /**
   41 |  * The response of a {@link DecisionModel}: one {@link DecisionResult} per question, in
   42 |  * the order the questions were asked, and the response metadata.
   43 |  * <p>
   44 |  * A response is built with {@link #builder(DecisionPrompt)}, which orders the answers by
   45 |  * the prompt's questions and checks that each one answers its question, so a response
   46 |  * always matches the prompt it answers.
   47 |  *
   48 |  * @author Thomas Vitale
   49 |  * @since 2.1.0
   50 |  */
   51 | public final class DecisionResponse implements ModelResponse<DecisionResult> {
   52 | 
   53 | 	private final List<DecisionResult> results;
   54 | 
   55 | 	private final Map<String, DecisionAnswer> answers;
   56 | 
   57 | 	private final DecisionResponseMetadata metadata;
   58 | 
   59 | 	private DecisionResponse(List<DecisionResult> results, DecisionResponseMetadata metadata) {
   60 | 		this.results = List.copyOf(results);
   61 | 		Map<String, DecisionAnswer> answers = new LinkedHashMap<>();
   62 | 		for (DecisionResult result : this.results) {
   63 | 			answers.put(result.getName(), result.getOutput());
   64 | 		}
   65 | 		this.answers = Collections.unmodifiableMap(answers);
   66 | 		this.metadata = metadata;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * A builder for the response to the given prompt. {@link Builder#build()} orders the
   71 | 	 * answers by the prompt's questions and checks that each question is answered by an
   72 | 	 * answer of the matching type.
   73 | 	 */
   74 | 	public static Builder builder(DecisionPrompt prompt) {
   75 | 		return new Builder(prompt);
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public List<DecisionResult> getResults() {
   80 | 		return this.results;
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * The first result, or {@code null} when there are none.
   85 | 	 */
   86 | 	@Override
   87 | 	public @Nullable DecisionResult getResult() {
   88 | 		return this.results.isEmpty() ? null : this.results.get(0);
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public DecisionResponseMetadata getMetadata() {
   93 | 		return this.metadata;
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * The answers, keyed by question name, in the order the questions were asked.
   98 | 	 */
   99 | 	public Map<String, DecisionAnswer> getAnswers() {
  100 | 		return this.answers;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Whether the question was answered, including by a refusal.
  105 | 	 */
  106 | 	public boolean hasAnswer(String name) {
  107 | 		return this.answers.containsKey(name);
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * The answer to the given question.
  112 | 	 */
  113 | 	public DecisionAnswer getAnswer(String name) {
  114 | 		DecisionAnswer answer = this.answers.get(name);
  115 | 		Assert.isTrue(answer != null, () -> "no answer for question '" + name + "'");
  116 | 		return answer;
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * The answer to the given question, as the given type. Fails with an
  121 | 	 * {@link IllegalStateException} when the provider refused to answer.
  122 | 	 */
  123 | 	public <A extends DecisionAnswer> A getAnswer(String name, Class<A> type) {
  124 | 		DecisionAnswer answer = getAnswer(name);
  125 | 		if (type.isInstance(answer)) {
  126 | 			return type.cast(answer);
  127 | 		}
  128 | 		if (answer instanceof RefusalAnswer) {
  129 | 			throw new IllegalStateException("the provider refused to answer question '" + name + "'");
  130 | 		}
  131 | 		throw new IllegalArgumentException("the answer to question '" + name + "' is a "
  132 | 				+ answer.getClass().getSimpleName() + ", not a " + type.getSimpleName());
  133 | 	}
  134 | 
  135 | 	public YesNoAnswer getYesNo(String name) {
  136 | 		return getAnswer(name, YesNoAnswer.class);
  137 | 	}
  138 | 
  139 | 	public ChoiceAnswer getChoice(String name) {
  140 | 		return getAnswer(name, ChoiceAnswer.class);
  141 | 	}
  142 | 
  143 | 	public ScoreAnswer getScore(String name) {
  144 | 		return getAnswer(name, ScoreAnswer.class);
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Whether the provider refused to answer the given question.
  149 | 	 */
  150 | 	public boolean isRefused(String name) {
  151 | 		return getAnswer(name) instanceof RefusalAnswer;
  152 | 	}
  153 | 
  154 | 	@Override
  155 | 	public boolean equals(@Nullable Object o) {
  156 | 		if (this == o) {
  157 | 			return true;
  158 | 		}
  159 | 		if (!(o instanceof DecisionResponse that)) {
  160 | 			return false;
  161 | 		}
  162 | 		return this.results.equals(that.results) && Objects.equals(this.metadata, that.metadata);
  163 | 	}
  164 | 
  165 | 	@Override
  166 | 	public int hashCode() {
  167 | 		return Objects.hash(this.results, this.metadata);
  168 | 	}
  169 | 
  170 | 	@Override
  171 | 	public String toString() {
  172 | 		return "DecisionResponse{results=" + this.results + ", metadata=" + this.metadata + "}";
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Builds a {@link DecisionResponse} that answers a {@link DecisionPrompt}.
  177 | 	 */
  178 | 	public static final class Builder {
  179 | 
  180 | 		private final DecisionPrompt prompt;
  181 | 
  182 | 		private final Map<String, DecisionAnswer> answers = new LinkedHashMap<>();
  183 | 
  184 | 		private final Map<String, DecisionResultMetadata> resultMetadata = new LinkedHashMap<>();
  185 | 
  186 | 		private DecisionResponseMetadata metadata = DecisionResponseMetadata.builder().build();
  187 | 
  188 | 		private Builder(DecisionPrompt prompt) {
  189 | 			Assert.notNull(prompt, "prompt cannot be null");
  190 | 			this.prompt = prompt;
  191 | 		}
  192 | 
  193 | 		/**
  194 | 		 * Add the answer to a question.
  195 | 		 */
  196 | 		public Builder answer(String name, DecisionAnswer answer) {
  197 | 			return answer(name, answer, DecisionResult.NO_METADATA);
  198 | 		}
  199 | 
  200 | 		/**
  201 | 		 * Add the answer to a question, with metadata about that answer.
  202 | 		 */
  203 | 		public Builder answer(String name, DecisionAnswer answer, DecisionResultMetadata metadata) {
  204 | 			Assert.hasText(name, "name cannot be null or empty");
  205 | 			Assert.notNull(answer, "answer cannot be null");
  206 | 			Assert.notNull(metadata, "metadata cannot be null");
  207 | 			Assert.isTrue(!this.answers.containsKey(name), () -> "duplicate answer for question '" + name + "'");
  208 | 			this.answers.put(name, answer);
  209 | 			this.resultMetadata.put(name, metadata);
  210 | 			return this;
  211 | 		}
  212 | 
  213 | 		public Builder answers(Map<String, ? extends DecisionAnswer> answers) {
  214 | 			answers.forEach(this::answer);
  215 | 			return this;
  216 | 		}
  217 | 
  218 | 		public Builder metadata(DecisionResponseMetadata metadata) {
  219 | 			Assert.notNull(metadata, "metadata cannot be null");
  220 | 			this.metadata = metadata;
  221 | 			return this;
  222 | 		}
  223 | 
  224 | 		/**
  225 | 		 * Build the response, with the results in the order of the prompt's questions.
  226 | 		 * Fails when a question has no answer, an answer has no question, or an answer
  227 | 		 * doesn't match its question: a wrong type, a choice outside the offered options,
  228 | 		 * or a score outside the levels. A refusal is accepted for any question.
  229 | 		 */
  230 | 		public DecisionResponse build() {
  231 | 			for (String name : this.answers.keySet()) {
  232 | 				Assert.state(this.prompt.getQuestions().containsKey(name),
  233 | 						() -> "the prompt has no question named '" + name + "'");
  234 | 			}
  235 | 			List<DecisionResult> results = new ArrayList<>();
  236 | 			for (Map.Entry<String, DecisionQuestion> entry : this.prompt.getQuestions().entrySet()) {
  237 | 				String name = entry.getKey();
  238 | 				DecisionAnswer answer = this.answers.get(name);
  239 | 				Assert.state(answer != null, () -> "missing answer for question '" + name + "'");
  240 | 				check(name, entry.getValue(), answer);
  241 | 				results.add(new DecisionResult(name, answer,
  242 | 						this.resultMetadata.getOrDefault(name, DecisionResult.NO_METADATA)));
  243 | 			}
  244 | 			return new DecisionResponse(results, this.metadata);
  245 | 		}
  246 | 
  247 | 		private static void check(String name, DecisionQuestion question, DecisionAnswer answer) {
  248 | 			if (answer instanceof RefusalAnswer) {
  249 | 				return;
  250 | 			}
  251 | 			if (question instanceof YesNoQuestion) {
  252 | 				expect(name, answer, YesNoAnswer.class);
  253 | 			}
  254 | 			else if (question instanceof ChoiceQuestion choiceQuestion) {
  255 | 				ChoiceAnswer choiceAnswer = expect(name, answer, ChoiceAnswer.class);
  256 | 				Assert.state(choiceQuestion.getOptions().containsKey(choiceAnswer.getValue()),
  257 | 						() -> "the answer to question '" + name + "' is '" + choiceAnswer.getValue()
  258 | 								+ "', which is not one of the offered options " + choiceQuestion.getOptionNames());
  259 | 				for (String option : choiceAnswer.getProbabilities().keySet()) {
  260 | 					Assert.state(choiceQuestion.getOptions().containsKey(option),
  261 | 							() -> "the answer to question '" + name + "' reports a probability for '" + option
  262 | 									+ "', which is not one of the offered options " + choiceQuestion.getOptionNames());
  263 | 				}
  264 | 			}
  265 | 			else if (question instanceof ScoreQuestion scoreQuestion) {
  266 | 				ScoreAnswer scoreAnswer = expect(name, answer, ScoreAnswer.class);
  267 | 				int maxLevel = scoreQuestion.getLevels().size() - 1;
  268 | 				Assert.state(scoreAnswer.getValue() <= maxLevel, () -> "the answer to question '" + name + "' is "
  269 | 						+ scoreAnswer.getValue() + ", which is above the highest level " + maxLevel);
  270 | 				Assert.state(
  271 | 						scoreAnswer.getProbabilities().isEmpty()
  272 | 								|| scoreAnswer.getProbabilities().size() == scoreQuestion.getLevels().size(),
  273 | 						() -> "the answer to question '" + name + "' reports " + scoreAnswer.getProbabilities().size()
  274 | 								+ " probabilities for " + scoreQuestion.getLevels().size() + " levels");
  275 | 			}
  276 | 		}
  277 | 
  278 | 		private static <A extends DecisionAnswer> A expect(String name, DecisionAnswer answer, Class<A> type) {
  279 | 			Assert.state(type.isInstance(answer), () -> "the answer to question '" + name + "' is a "
  280 | 					+ answer.getClass().getSimpleName() + ", but a " + type.getSimpleName() + " was expected");
  281 | 			return type.cast(answer);
  282 | 		}
  283 | 
  284 | 	}
  285 | 
  286 | }
```
