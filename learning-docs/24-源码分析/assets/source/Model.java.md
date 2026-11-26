<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/Model.java -->
<!-- 正文引用：[[源码:Model.java#L起-L止]] ；代码块头部：(src: Model.java#L起-L止) -->

# Model.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/Model.java` |
| 行数 | 40 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:27.831Z |

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
   17 | package org.springframework.ai.model;
   18 | 
   19 | /**
   20 |  * The Model interface provides a generic API for invoking AI models. It is designed to
   21 |  * handle the interaction with various types of AI models by abstracting the process of
   22 |  * sending requests and receiving responses. The interface uses Java generics to
   23 |  * accommodate different types of requests and responses, enhancing flexibility and
   24 |  * adaptability across different AI model implementations.
   25 |  *
   26 |  * @param <TReq> the generic type of the request to the AI model
   27 |  * @param <TRes> the generic type of the response from the AI model
   28 |  * @author Mark Pollack
   29 |  * @since 0.8.0
   30 |  */
   31 | public interface Model<TReq extends ModelRequest<?>, TRes extends ModelResponse<?>> {
   32 | 
   33 | 	/**
   34 | 	 * Executes a method call to the AI model.
   35 | 	 * @param request the request object to be sent to the AI model
   36 | 	 * @return the response from the AI model
   37 | 	 */
   38 | 	TRes call(TReq request);
   39 | 
   40 | }
```
