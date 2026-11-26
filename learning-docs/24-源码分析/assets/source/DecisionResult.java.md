<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DecisionResult.java -->
<!-- 正文引用：[[源码:DecisionResult.java#L起-L止]] ；代码块头部：(src: DecisionResult.java#L起-L止) -->

# DecisionResult.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DecisionResult.java` |
| 行数 | 94 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:08.424Z |

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
   19 | import java.util.Objects;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.ai.decision.answer.DecisionAnswer;
   24 | import org.springframework.ai.model.ModelResult;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * The answer to one question of a {@link DecisionPrompt}, named after the question.
   29 |  * Results are created by {@link DecisionResponse.Builder}, which puts them in the order
   30 |  * of the prompt's questions.
   31 |  *
   32 |  * @author Thomas Vitale
   33 |  * @since 2.1.0
   34 |  */
   35 | public final class DecisionResult implements ModelResult<DecisionAnswer> {
   36 | 
   37 | 	static final DecisionResultMetadata NO_METADATA = new DecisionResultMetadata() {
   38 | 
   39 | 	};
   40 | 
   41 | 	private final String name;
   42 | 
   43 | 	private final DecisionAnswer answer;
   44 | 
   45 | 	private final DecisionResultMetadata metadata;
   46 | 
   47 | 	DecisionResult(String name, DecisionAnswer answer, DecisionResultMetadata metadata) {
   48 | 		Assert.hasText(name, "name cannot be null or empty");
   49 | 		Assert.notNull(answer, "answer cannot be null");
   50 | 		Assert.notNull(metadata, "metadata cannot be null");
   51 | 		this.name = name;
   52 | 		this.answer = answer;
   53 | 		this.metadata = metadata;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * The name of the question this result answers.
   58 | 	 */
   59 | 	public String getName() {
   60 | 		return this.name;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public DecisionAnswer getOutput() {
   65 | 		return this.answer;
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public DecisionResultMetadata getMetadata() {
   70 | 		return this.metadata;
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public boolean equals(@Nullable Object o) {
   75 | 		if (this == o) {
   76 | 			return true;
   77 | 		}
   78 | 		if (!(o instanceof DecisionResult that)) {
   79 | 			return false;
   80 | 		}
   81 | 		return this.name.equals(that.name) && this.answer.equals(that.answer) && this.metadata.equals(that.metadata);
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public int hashCode() {
   86 | 		return Objects.hash(this.name, this.answer, this.metadata);
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	public String toString() {
   91 | 		return "DecisionResult{name='" + this.name + "', answer=" + this.answer + "}";
   92 | 	}
   93 | 
   94 | }
```
