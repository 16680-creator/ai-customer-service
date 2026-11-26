<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/HandlerMethodReturnValueHandler.java -->
<!-- 正文引用：[[源码:HandlerMethodReturnValueHandler.java#L起-L止]] ；代码块头部：(src: HandlerMethodReturnValueHandler.java#L起-L止) -->

# HandlerMethodReturnValueHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/HandlerMethodReturnValueHandler.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:38.113Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.method.support;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.core.MethodParameter;
   22 | import org.springframework.web.context.request.NativeWebRequest;
   23 | 
   24 | /**
   25 |  * Strategy interface to handle the value returned from the invocation of a
   26 |  * handler method.
   27 |  *
   28 |  * @author Arjen Poutsma
   29 |  * @since 3.1
   30 |  * @see HandlerMethodArgumentResolver
   31 |  */
   32 | public interface HandlerMethodReturnValueHandler {
   33 | 
   34 | 	/**
   35 | 	 * Whether the given {@linkplain MethodParameter method return type} is
   36 | 	 * supported by this handler.
   37 | 	 * @param returnType the method return type to check
   38 | 	 * @return {@code true} if this handler supports the supplied return type;
   39 | 	 * {@code false} otherwise
   40 | 	 */
   41 | 	boolean supportsReturnType(MethodParameter returnType);
   42 | 
   43 | 	/**
   44 | 	 * Handle the given return value by adding attributes to the model and
   45 | 	 * setting a view or setting the
   46 | 	 * {@link ModelAndViewContainer#setRequestHandled} flag to {@code true}
   47 | 	 * to indicate the response has been handled directly.
   48 | 	 * @param returnValue the value returned from the handler method
   49 | 	 * @param returnType the type of the return value. This type must have
   50 | 	 * previously been passed to {@link #supportsReturnType} which must
   51 | 	 * have returned {@code true}.
   52 | 	 * @param mavContainer the ModelAndViewContainer for the current request
   53 | 	 * @param webRequest the current request
   54 | 	 * @throws Exception if the return value handling results in an error
   55 | 	 */
   56 | 	void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
   57 | 			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;
   58 | 
   59 | }
```
