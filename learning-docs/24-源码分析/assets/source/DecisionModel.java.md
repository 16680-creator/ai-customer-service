<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DecisionModel.java -->
<!-- 正文引用：[[源码:DecisionModel.java#L起-L止]] ；代码块头部：(src: DecisionModel.java#L起-L止) -->

# DecisionModel.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DecisionModel.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:07.477Z |

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
   19 | import java.util.Map;
   20 | 
   21 | import org.springframework.ai.decision.input.DecisionInput;
   22 | import org.springframework.ai.decision.question.DecisionQuestion;
   23 | import org.springframework.ai.model.Model;
   24 | 
   25 | /**
   26 |  * A model that answers typed questions about an input instead of generating text. Each
   27 |  * answer carries the probabilities the model assigned, so that the application decides
   28 |  * what to do with them.
   29 |  *
   30 |  * @author Thomas Vitale
   31 |  * @since 2.1.0
   32 |  */
   33 | @FunctionalInterface
   34 | public interface DecisionModel extends Model<DecisionPrompt, DecisionResponse> {
   35 | 
   36 | 	@Override
   37 | 	DecisionResponse call(DecisionPrompt prompt);
   38 | 
   39 | 	/**
   40 | 	 * Ask the given questions about a text, with the model's default options.
   41 | 	 */
   42 | 	default DecisionResponse call(String text, Map<String, ? extends DecisionQuestion> questions) {
   43 | 		return call(DecisionPrompt.builder().text(text).questions(questions).build());
   44 | 	}
   45 | 
   46 | 	/**
   47 | 	 * Ask the given questions about an input, with the model's default options.
   48 | 	 */
   49 | 	default DecisionResponse call(DecisionInput input, Map<String, ? extends DecisionQuestion> questions) {
   50 | 		return call(DecisionPrompt.builder().input(input).questions(questions).build());
   51 | 	}
   52 | 
   53 | }
```
