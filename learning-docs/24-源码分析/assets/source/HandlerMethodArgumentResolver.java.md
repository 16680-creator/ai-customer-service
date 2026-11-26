<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/HandlerMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:HandlerMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: HandlerMethodArgumentResolver.java#L起-L止) -->

# HandlerMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/HandlerMethodArgumentResolver.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.958Z |

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
   22 | import org.springframework.web.bind.WebDataBinder;
   23 | import org.springframework.web.bind.support.WebDataBinderFactory;
   24 | import org.springframework.web.context.request.NativeWebRequest;
   25 | 
   26 | /**
   27 |  * Strategy interface for resolving method parameters into argument values in
   28 |  * the context of a given request.
   29 |  *
   30 |  * @author Arjen Poutsma
   31 |  * @since 3.1
   32 |  * @see HandlerMethodReturnValueHandler
   33 |  */
   34 | public interface HandlerMethodArgumentResolver {
   35 | 
   36 | 	/**
   37 | 	 * Whether the given {@linkplain MethodParameter method parameter} is
   38 | 	 * supported by this resolver.
   39 | 	 * @param parameter the method parameter to check
   40 | 	 * @return {@code true} if this resolver supports the supplied parameter;
   41 | 	 * {@code false} otherwise
   42 | 	 */
   43 | 	boolean supportsParameter(MethodParameter parameter);
   44 | 
   45 | 	/**
   46 | 	 * Resolves a method parameter into an argument value from a given request.
   47 | 	 * A {@link ModelAndViewContainer} provides access to the model for the
   48 | 	 * request. A {@link WebDataBinderFactory} provides a way to create
   49 | 	 * a {@link WebDataBinder} instance when needed for data binding and
   50 | 	 * type conversion purposes.
   51 | 	 * @param parameter the method parameter to resolve. This parameter must
   52 | 	 * have previously been passed to {@link #supportsParameter} which must
   53 | 	 * have returned {@code true}.
   54 | 	 * @param mavContainer the ModelAndViewContainer for the current request
   55 | 	 * @param webRequest the current request
   56 | 	 * @param binderFactory a factory for creating {@link WebDataBinder} instances
   57 | 	 * @return the resolved argument value, or {@code null} if not resolvable
   58 | 	 * @throws Exception in case of errors with the preparation of argument values
   59 | 	 */
   60 | 	@Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
   61 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception;
   62 | 
   63 | }
```
