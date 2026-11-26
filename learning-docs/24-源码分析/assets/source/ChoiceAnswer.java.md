<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/answer/ChoiceAnswer.java -->
<!-- 正文引用：[[源码:ChoiceAnswer.java#L起-L止]] ；代码块头部：(src: ChoiceAnswer.java#L起-L止) -->

# ChoiceAnswer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/answer/ChoiceAnswer.java` |
| 行数 | 175 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.344Z |

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
   17 | package org.springframework.ai.decision.answer;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Comparator;
   21 | import java.util.LinkedHashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.ai.decision.question.ChoiceQuestion;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * The answer to a {@link ChoiceQuestion}: the selected option, the probability of each
   33 |  * option when the provider reports them, and the provider's confidence when it reports
   34 |  * one. Probabilities are what the model assigned to each option. Confidence is the
   35 |  * provider's own estimate and is not a probability of correctness.
   36 |  *
   37 |  * @author Thomas Vitale
   38 |  * @author Christian Tzolov
   39 |  * @since 2.1.0
   40 |  */
   41 | public final class ChoiceAnswer implements DecisionAnswer {
   42 | 
   43 | 	private final String value;
   44 | 
   45 | 	private final Map<String, Double> probabilities;
   46 | 
   47 | 	private final @Nullable Double confidence;
   48 | 
   49 | 	private ChoiceAnswer(String value, Map<String, Double> probabilities, @Nullable Double confidence) {
   50 | 		Assert.hasText(value, "value cannot be null or empty");
   51 | 		Assert.notNull(probabilities, "probabilities cannot be null");
   52 | 		this.value = value;
   53 | 		Map<String, Double> checked = new LinkedHashMap<>();
   54 | 		probabilities.forEach((option, probability) -> {
   55 | 			Assert.notNull(probability, () -> "the probability of '" + option + "' cannot be null");
   56 | 			Assert.isTrue(probability >= 0 && probability <= 1,
   57 | 					() -> "the probability of '" + option + "' must be between 0 and 1, but was " + probability);
   58 | 			checked.put(option, probability);
   59 | 		});
   60 | 		this.probabilities = Collections.unmodifiableMap(checked);
   61 | 		Assert.isTrue(confidence == null || (confidence >= 0 && confidence <= 1),
   62 | 				() -> "confidence must be between 0 and 1, but was " + confidence);
   63 | 		this.confidence = confidence;
   64 | 	}
   65 | 
   66 | 	public static Builder builder() {
   67 | 		return new Builder();
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * The selected option.
   72 | 	 */
   73 | 	public String getValue() {
   74 | 		return this.value;
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * The probability of the selected option, or {@code null} when not reported.
   79 | 	 */
   80 | 	public @Nullable Double getProbability() {
   81 | 		return this.probabilities.get(this.value);
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * The probability of the given option, or {@code null} when not reported.
   86 | 	 */
   87 | 	public @Nullable Double getProbability(String option) {
   88 | 		return this.probabilities.get(option);
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * The probability of each option, or an empty map when the provider doesn't report
   93 | 	 * them.
   94 | 	 */
   95 | 	public Map<String, Double> getProbabilities() {
   96 | 		return this.probabilities;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * The options from the most to the least likely, or an empty list when probabilities
  101 | 	 * weren't reported. Ties keep the reported order.
  102 | 	 */
  103 | 	public List<String> getRankedOptions() {
  104 | 		return this.probabilities.entrySet()
  105 | 			.stream()
  106 | 			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
  107 | 			.map(Map.Entry::getKey)
  108 | 			.toList();
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * The provider's confidence in the selected option, or {@code null} when the provider
  113 | 	 * doesn't report one.
  114 | 	 */
  115 | 	public @Nullable Double getConfidence() {
  116 | 		return this.confidence;
  117 | 	}
  118 | 
  119 | 	@Override
  120 | 	public boolean equals(@Nullable Object o) {
  121 | 		if (this == o) {
  122 | 			return true;
  123 | 		}
  124 | 		if (!(o instanceof ChoiceAnswer that)) {
  125 | 			return false;
  126 | 		}
  127 | 		return this.value.equals(that.value) && this.probabilities.equals(that.probabilities)
  128 | 				&& Objects.equals(this.confidence, that.confidence);
  129 | 	}
  130 | 
  131 | 	@Override
  132 | 	public int hashCode() {
  133 | 		return Objects.hash(this.value, this.probabilities, this.confidence);
  134 | 	}
  135 | 
  136 | 	@Override
  137 | 	public String toString() {
  138 | 		return "ChoiceAnswer{value='" + this.value + "', probabilities=" + this.probabilities + ", confidence="
  139 | 				+ this.confidence + "}";
  140 | 	}
  141 | 
  142 | 	public static final class Builder {
  143 | 
  144 | 		private @Nullable String value;
  145 | 
  146 | 		private Map<String, Double> probabilities = Map.of();
  147 | 
  148 | 		private @Nullable Double confidence;
  149 | 
  150 | 		private Builder() {
  151 | 		}
  152 | 
  153 | 		public Builder value(String value) {
  154 | 			this.value = value;
  155 | 			return this;
  156 | 		}
  157 | 
  158 | 		public Builder probabilities(Map<String, Double> probabilities) {
  159 | 			this.probabilities = probabilities;
  160 | 			return this;
  161 | 		}
  162 | 
  163 | 		public Builder confidence(@Nullable Double confidence) {
  164 | 			this.confidence = confidence;
  165 | 			return this;
  166 | 		}
  167 | 
  168 | 		public ChoiceAnswer build() {
  169 | 			Assert.state(this.value != null, "value cannot be null");
  170 | 			return new ChoiceAnswer(this.value, this.probabilities, this.confidence);
  171 | 		}
  172 | 
  173 | 	}
  174 | 
  175 | }
```
