<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/question/ChoiceQuestion.java -->
<!-- 正文引用：[[源码:ChoiceQuestion.java#L起-L止]] ；代码块头部：(src: ChoiceQuestion.java#L起-L止) -->

# ChoiceQuestion.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/question/ChoiceQuestion.java` |
| 行数 | 165 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.256Z |

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
   19 | import java.util.Collections;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.decision.answer.ChoiceAnswer;
   28 | import org.springframework.util.Assert;
   29 | import org.springframework.util.StringUtils;
   30 | 
   31 | /**
   32 |  * A question answered by picking one of the offered options, as a {@link ChoiceAnswer}.
   33 |  * Each option has a name and, optionally, a description that tells the model when the
   34 |  * option applies; a blank description counts as none.
   35 |  *
   36 |  * @author Thomas Vitale
   37 |  * @since 2.1.0
   38 |  */
   39 | public final class ChoiceQuestion implements DecisionQuestion {
   40 | 
   41 | 	private final String text;
   42 | 
   43 | 	private final Map<String, @Nullable String> options;
   44 | 
   45 | 	private ChoiceQuestion(String text, Map<String, @Nullable String> options) {
   46 | 		Assert.hasText(text, "text cannot be null or empty");
   47 | 		Assert.notNull(options, "options cannot be null");
   48 | 		Assert.isTrue(options.size() >= 2, "a choice question needs at least 2 options");
   49 | 		options.keySet().forEach(option -> Assert.hasText(option, "option names cannot be null or empty"));
   50 | 		this.text = text;
   51 | 		this.options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
   52 | 	}
   53 | 
   54 | 	/**
   55 | 	 * A question whose options have no description.
   56 | 	 */
   57 | 	public static ChoiceQuestion of(String text, String... options) {
   58 | 		return builder().text(text).options(options).build();
   59 | 	}
   60 | 
   61 | 	public static Builder builder() {
   62 | 		return new Builder();
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public String getText() {
   67 | 		return this.text;
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * The options, name to description, in the order they were declared. A description is
   72 | 	 * {@code null} when the option has none or a blank one.
   73 | 	 */
   74 | 	public Map<String, @Nullable String> getOptions() {
   75 | 		return this.options;
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * The option names, in the order they were declared.
   80 | 	 */
   81 | 	public List<String> getOptionNames() {
   82 | 		return List.copyOf(this.options.keySet());
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public boolean equals(@Nullable Object o) {
   87 | 		if (this == o) {
   88 | 			return true;
   89 | 		}
   90 | 		if (!(o instanceof ChoiceQuestion that)) {
   91 | 			return false;
   92 | 		}
   93 | 		return this.text.equals(that.text) && this.options.equals(that.options);
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public int hashCode() {
   98 | 		return Objects.hash(this.text, this.options);
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public String toString() {
  103 | 		return "ChoiceQuestion{text='" + this.text + "', options=" + this.options + "}";
  104 | 	}
  105 | 
  106 | 	public static final class Builder {
  107 | 
  108 | 		private @Nullable String text;
  109 | 
  110 | 		private final Map<String, @Nullable String> options = new LinkedHashMap<>();
  111 | 
  112 | 		private Builder() {
  113 | 		}
  114 | 
  115 | 		/**
  116 | 		 * The question text.
  117 | 		 */
  118 | 		public Builder text(String text) {
  119 | 			this.text = text;
  120 | 			return this;
  121 | 		}
  122 | 
  123 | 		/**
  124 | 		 * Add an option.
  125 | 		 */
  126 | 		public Builder option(String name) {
  127 | 			return option(name, null);
  128 | 		}
  129 | 
  130 | 		/**
  131 | 		 * Add an option with a description. A blank description counts as none.
  132 | 		 */
  133 | 		public Builder option(String name, @Nullable String description) {
  134 | 			Assert.hasText(name, "option name cannot be null or empty");
  135 | 			Assert.isTrue(!this.options.containsKey(name), () -> "duplicate option '" + name + "'");
  136 | 			this.options.put(name, StringUtils.hasText(description) ? description : null);
  137 | 			return this;
  138 | 		}
  139 | 
  140 | 		/**
  141 | 		 * Add multiple options.
  142 | 		 */
  143 | 		public Builder options(Map<String, @Nullable String> options) {
  144 | 			options.forEach(this::option);
  145 | 			return this;
  146 | 		}
  147 | 
  148 | 		/**
  149 | 		 * Add options without a description.
  150 | 		 */
  151 | 		public Builder options(String... names) {
  152 | 			for (String name : names) {
  153 | 				option(name);
  154 | 			}
  155 | 			return this;
  156 | 		}
  157 | 
  158 | 		public ChoiceQuestion build() {
  159 | 			Assert.state(this.text != null, "text cannot be null");
  160 | 			return new ChoiceQuestion(this.text, this.options);
  161 | 		}
  162 | 
  163 | 	}
  164 | 
  165 | }
```
