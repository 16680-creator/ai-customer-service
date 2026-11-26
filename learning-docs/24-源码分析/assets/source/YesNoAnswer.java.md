<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/answer/YesNoAnswer.java -->
<!-- 正文引用：[[源码:YesNoAnswer.java#L起-L止]] ；代码块头部：(src: YesNoAnswer.java#L起-L止) -->

# YesNoAnswer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/answer/YesNoAnswer.java` |
| 行数 | 110 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:09.215Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.ai.decision.question.YesNoQuestion;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * The answer to a {@link YesNoQuestion}: the probability that the answer is yes.
   26 |  *
   27 |  * @author Thomas Vitale
   28 |  * @since 2.1.0
   29 |  */
   30 | public final class YesNoAnswer implements DecisionAnswer {
   31 | 
   32 | 	/**
   33 | 	 * The threshold {@link #isYes()} applies.
   34 | 	 */
   35 | 	public static final double DEFAULT_THRESHOLD = 0.5;
   36 | 
   37 | 	private final double probability;
   38 | 
   39 | 	private YesNoAnswer(double probability) {
   40 | 		Assert.isTrue(probability >= 0 && probability <= 1,
   41 | 				() -> "probability must be between 0 and 1, but was " + probability);
   42 | 		this.probability = probability;
   43 | 	}
   44 | 
   45 | 	public static Builder builder() {
   46 | 		return new Builder();
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * The probability, between 0 and 1, that the answer is yes.
   51 | 	 */
   52 | 	public double getProbability() {
   53 | 		return this.probability;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Whether the answer is yes, that is, the probability is at least 0.5.
   58 | 	 */
   59 | 	public boolean isYes() {
   60 | 		return isYes(DEFAULT_THRESHOLD);
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Whether the probability is at least the given threshold.
   65 | 	 */
   66 | 	public boolean isYes(double threshold) {
   67 | 		return this.probability >= threshold;
   68 | 	}
   69 | 
   70 | 	@Override
   71 | 	public boolean equals(@Nullable Object o) {
   72 | 		if (this == o) {
   73 | 			return true;
   74 | 		}
   75 | 		if (!(o instanceof YesNoAnswer that)) {
   76 | 			return false;
   77 | 		}
   78 | 		return Double.compare(this.probability, that.probability) == 0;
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public int hashCode() {
   83 | 		return Double.hashCode(this.probability);
   84 | 	}
   85 | 
   86 | 	@Override
   87 | 	public String toString() {
   88 | 		return "YesNoAnswer{probability=" + this.probability + "}";
   89 | 	}
   90 | 
   91 | 	public static final class Builder {
   92 | 
   93 | 		private @Nullable Double probability;
   94 | 
   95 | 		private Builder() {
   96 | 		}
   97 | 
   98 | 		public Builder probability(double probability) {
   99 | 			this.probability = probability;
  100 | 			return this;
  101 | 		}
  102 | 
  103 | 		public YesNoAnswer build() {
  104 | 			Assert.state(this.probability != null, "probability cannot be null");
  105 | 			return new YesNoAnswer(this.probability);
  106 | 		}
  107 | 
  108 | 	}
  109 | 
  110 | }
```
