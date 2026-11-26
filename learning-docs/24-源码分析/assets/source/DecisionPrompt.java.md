<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DecisionPrompt.java -->
<!-- 正文引用：[[源码:DecisionPrompt.java#L起-L止]] ；代码块头部：(src: DecisionPrompt.java#L起-L止) -->

# DecisionPrompt.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DecisionPrompt.java` |
| 行数 | 182 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:07.718Z |

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
   19 | import java.util.Collections;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.chat.messages.Message;
   28 | import org.springframework.ai.chat.messages.part.MessagePart;
   29 | import org.springframework.ai.decision.input.DecisionInput;
   30 | import org.springframework.ai.decision.question.DecisionQuestion;
   31 | import org.springframework.ai.model.ModelRequest;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * The request to a {@link DecisionModel}: an input, the named questions to answer about
   36 |  * it, and optional {@link DecisionOptions}. The questions keep the order they were added
   37 |  * in, and the answers come back in the same order.
   38 |  *
   39 |  * @author Thomas Vitale
   40 |  * @author Christian Tzolov
   41 |  * @since 2.1.0
   42 |  */
   43 | public final class DecisionPrompt implements ModelRequest<DecisionInput> {
   44 | 
   45 | 	private final DecisionInput input;
   46 | 
   47 | 	private final Map<String, DecisionQuestion> questions;
   48 | 
   49 | 	private final @Nullable DecisionOptions options;
   50 | 
   51 | 	private DecisionPrompt(DecisionInput input, Map<String, DecisionQuestion> questions,
   52 | 			@Nullable DecisionOptions options) {
   53 | 		Assert.notNull(input, "input cannot be null");
   54 | 		Assert.notEmpty(questions, "questions cannot be null or empty");
   55 | 		this.input = input;
   56 | 		this.questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
   57 | 		this.options = options;
   58 | 	}
   59 | 
   60 | 	public static Builder builder() {
   61 | 		return new Builder();
   62 | 	}
   63 | 
   64 | 	public Builder mutate() {
   65 | 		return new Builder(this);
   66 | 	}
   67 | 
   68 | 	/**
   69 | 	 * The input to decide about.
   70 | 	 */
   71 | 	@Override
   72 | 	public DecisionInput getInstructions() {
   73 | 		return this.input;
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * The questions to answer, keyed by name, in the order they were added.
   78 | 	 */
   79 | 	public Map<String, DecisionQuestion> getQuestions() {
   80 | 		return this.questions;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public @Nullable DecisionOptions getOptions() {
   85 | 		return this.options;
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public boolean equals(@Nullable Object o) {
   90 | 		if (this == o) {
   91 | 			return true;
   92 | 		}
   93 | 		if (!(o instanceof DecisionPrompt that)) {
   94 | 			return false;
   95 | 		}
   96 | 		return this.input.equals(that.input) && this.questions.equals(that.questions)
   97 | 				&& Objects.equals(this.options, that.options);
   98 | 	}
   99 | 
  100 | 	@Override
  101 | 	public int hashCode() {
  102 | 		return Objects.hash(this.input, this.questions, this.options);
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public String toString() {
  107 | 		return "DecisionPrompt{input=" + this.input + ", questions=" + this.questions.keySet() + ", options="
  108 | 				+ this.options + "}";
  109 | 	}
  110 | 
  111 | 	public static final class Builder {
  112 | 
  113 | 		private @Nullable DecisionInput input;
  114 | 
  115 | 		private final Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
  116 | 
  117 | 		private @Nullable DecisionOptions options;
  118 | 
  119 | 		private Builder() {
  120 | 		}
  121 | 
  122 | 		private Builder(DecisionPrompt prompt) {
  123 | 			this.input = prompt.input;
  124 | 			this.questions.putAll(prompt.questions);
  125 | 			this.options = prompt.options;
  126 | 		}
  127 | 
  128 | 		public Builder input(DecisionInput input) {
  129 | 			this.input = input;
  130 | 			return this;
  131 | 		}
  132 | 
  133 | 		public Builder text(String text) {
  134 | 			return input(DecisionInput.text(text));
  135 | 		}
  136 | 
  137 | 		public Builder content(List<? extends MessagePart> parts) {
  138 | 			return input(DecisionInput.content(parts));
  139 | 		}
  140 | 
  141 | 		public Builder content(MessagePart... parts) {
  142 | 			return input(DecisionInput.content(parts));
  143 | 		}
  144 | 
  145 | 		public Builder fields(Map<String, @Nullable Object> fields) {
  146 | 			return input(DecisionInput.fields(fields));
  147 | 		}
  148 | 
  149 | 		public Builder conversation(List<? extends Message> messages) {
  150 | 			return input(DecisionInput.conversation(messages));
  151 | 		}
  152 | 
  153 | 		/**
  154 | 		 * Add a question. Its name must be unique within the prompt.
  155 | 		 */
  156 | 		public Builder question(String name, DecisionQuestion question) {
  157 | 			Assert.hasText(name, "question name cannot be null or empty");
  158 | 			Assert.notNull(question, "question cannot be null");
  159 | 			Assert.isTrue(!this.questions.containsKey(name), () -> "duplicate question '" + name + "'");
  160 | 			this.questions.put(name, question);
  161 | 			return this;
  162 | 		}
  163 | 
  164 | 		public Builder questions(Map<String, ? extends DecisionQuestion> questions) {
  165 | 			questions.forEach(this::question);
  166 | 			return this;
  167 | 		}
  168 | 
  169 | 		public Builder options(@Nullable DecisionOptions options) {
  170 | 			this.options = options;
  171 | 			return this;
  172 | 		}
  173 | 
  174 | 		public DecisionPrompt build() {
  175 | 			Assert.state(this.input != null, "input cannot be null");
  176 | 			Assert.state(!this.questions.isEmpty(), "at least one question is required");
  177 | 			return new DecisionPrompt(this.input, this.questions, this.options);
  178 | 		}
  179 | 
  180 | 	}
  181 | 
  182 | }
```
