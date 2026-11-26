<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DecisionOptions.java -->
<!-- 正文引用：[[源码:DecisionOptions.java#L起-L止]] ；代码块头部：(src: DecisionOptions.java#L起-L止) -->

# DecisionOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DecisionOptions.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.518Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.ai.model.ModelOptions;
   22 | 
   23 | /**
   24 |  * Portable options for decision models. Options are immutable: {@link #mutate()} returns
   25 |  * a builder with their values, and {@link Builder#combineWith(Builder)} overrides them
   26 |  * with the non-null values of another builder.
   27 |  *
   28 |  * @author Thomas Vitale
   29 |  * @since 2.1.0
   30 |  */
   31 | public interface DecisionOptions extends ModelOptions {
   32 | 
   33 | 	/**
   34 | 	 * The name of the model to use.
   35 | 	 */
   36 | 	@Nullable String getModel();
   37 | 
   38 | 	/**
   39 | 	 * A builder initialized with these options.
   40 | 	 */
   41 | 	DecisionOptions.Builder<?> mutate();
   42 | 
   43 | 	static DecisionOptions.Builder<?> builder() {
   44 | 		return new DefaultDecisionOptions.Builder<>();
   45 | 
   46 | 	}
   47 | 
   48 | 	interface Builder<B extends Builder<B>> extends Cloneable {
   49 | 
   50 | 		B clone();
   51 | 
   52 | 		B model(@Nullable String model);
   53 | 
   54 | 		DecisionOptions build();
   55 | 
   56 | 		/**
   57 | 		 * Override the values set so far with the non-null values of the given builder,
   58 | 		 * whichever implementation it is.
   59 | 		 */
   60 | 		B combineWith(DecisionOptions.Builder<?> other);
   61 | 
   62 | 	}
   63 | 
   64 | }
```
