<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/question/YesNoQuestion.java -->
<!-- 正文引用：[[源码:YesNoQuestion.java#L起-L止]] ；代码块头部：(src: YesNoQuestion.java#L起-L止) -->

# YesNoQuestion.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/question/YesNoQuestion.java` |
| 行数 | 145 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:08.863Z |

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
   17 | package org.springframework.ai.decision.question;
   18 | 
   19 | import java.util.Objects;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.decision.answer.YesNoAnswer;
   24 | import org.springframework.util.Assert;
   25 | import org.springframework.util.StringUtils;
   26 | 
   27 | /**
   28 |  * A question with a yes or no answer, answered with the probability of "yes" as a
   29 |  * {@link YesNoAnswer}. The optional criteria describe when the answer is yes and when it
   30 |  * is no; a blank criterion counts as none. Providers whose API has a place for them send
   31 |  * them as such, the others add them to the question text.
   32 |  *
   33 |  * @author Thomas Vitale
   34 |  * @since 2.1.0
   35 |  */
   36 | public final class YesNoQuestion implements DecisionQuestion {
   37 | 
   38 | 	private final String text;
   39 | 
   40 | 	private final @Nullable String whenYes;
   41 | 
   42 | 	private final @Nullable String whenNo;
   43 | 
   44 | 	private YesNoQuestion(String text, @Nullable String whenYes, @Nullable String whenNo) {
   45 | 		Assert.hasText(text, "text cannot be null or empty");
   46 | 		this.text = text;
   47 | 		this.whenYes = StringUtils.hasText(whenYes) ? whenYes : null;
   48 | 		this.whenNo = StringUtils.hasText(whenNo) ? whenNo : null;
   49 | 	}
   50 | 
   51 | 	/**
   52 | 	 * A question without criteria.
   53 | 	 */
   54 | 	public static YesNoQuestion of(String text) {
   55 | 		return builder().text(text).build();
   56 | 	}
   57 | 
   58 | 	public static Builder builder() {
   59 | 		return new Builder();
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public String getText() {
   64 | 		return this.text;
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * When the answer is yes, or {@code null} when not specified or blank.
   69 | 	 */
   70 | 	public @Nullable String getWhenYes() {
   71 | 		return this.whenYes;
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * When the answer is no, or {@code null} when not specified or blank.
   76 | 	 */
   77 | 	public @Nullable String getWhenNo() {
   78 | 		return this.whenNo;
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public boolean equals(@Nullable Object o) {
   83 | 		if (this == o) {
   84 | 			return true;
   85 | 		}
   86 | 		if (!(o instanceof YesNoQuestion that)) {
   87 | 			return false;
   88 | 		}
   89 | 		return this.text.equals(that.text) && Objects.equals(this.whenYes, that.whenYes)
   90 | 				&& Objects.equals(this.whenNo, that.whenNo);
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public int hashCode() {
   95 | 		return Objects.hash(this.text, this.whenYes, this.whenNo);
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public String toString() {
  100 | 		return "YesNoQuestion{text='" + this.text + "', whenYes='" + this.whenYes + "', whenNo='" + this.whenNo + "'}";
  101 | 	}
  102 | 
  103 | 	public static final class Builder {
  104 | 
  105 | 		private @Nullable String text;
  106 | 
  107 | 		private @Nullable String whenYes;
  108 | 
  109 | 		private @Nullable String whenNo;
  110 | 
  111 | 		private Builder() {
  112 | 		}
  113 | 
  114 | 		/**
  115 | 		 * The question text.
  116 | 		 */
  117 | 		public Builder text(String text) {
  118 | 			this.text = text;
  119 | 			return this;
  120 | 		}
  121 | 
  122 | 		/**
  123 | 		 * Describe when the answer is yes. A blank description counts as none.
  124 | 		 */
  125 | 		public Builder whenYes(@Nullable String whenYes) {
  126 | 			this.whenYes = whenYes;
  127 | 			return this;
  128 | 		}
  129 | 
  130 | 		/**
  131 | 		 * Describe when the answer is no. A blank description counts as none.
  132 | 		 */
  133 | 		public Builder whenNo(@Nullable String whenNo) {
  134 | 			this.whenNo = whenNo;
  135 | 			return this;
  136 | 		}
  137 | 
  138 | 		public YesNoQuestion build() {
  139 | 			Assert.state(this.text != null, "text cannot be null");
  140 | 			return new YesNoQuestion(this.text, this.whenYes, this.whenNo);
  141 | 		}
  142 | 
  143 | 	}
  144 | 
  145 | }
```
