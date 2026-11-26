<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HandlerExceptionResolver.java -->
<!-- 正文引用：[[源码:HandlerExceptionResolver.java#L起-L止]] ；代码块头部：(src: HandlerExceptionResolver.java#L起-L止) -->

# HandlerExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HandlerExceptionResolver.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.618Z |

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
   17 | package org.springframework.web.servlet;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpServletResponse;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | /**
   24 |  * Interface to be implemented by objects that can resolve exceptions thrown during
   25 |  * handler mapping or execution, in the typical case to error views. Implementors are
   26 |  * typically registered as beans in the application context.
   27 |  *
   28 |  * <p>Error views are analogous to JSP error pages but can be used with any kind of
   29 |  * exception including any checked exception, with potentially fine-grained mappings for
   30 |  * specific handlers.
   31 |  *
   32 |  * @author Juergen Hoeller
   33 |  * @since 22.11.2003
   34 |  */
   35 | public interface HandlerExceptionResolver {
   36 | 
   37 | 	/**
   38 | 	 * Try to resolve the given exception that got thrown during handler execution,
   39 | 	 * returning a {@link ModelAndView} that represents a specific error page if appropriate.
   40 | 	 * <p>The returned {@code ModelAndView} may be {@linkplain ModelAndView#isEmpty() empty}
   41 | 	 * to indicate that the exception has been resolved successfully but that no view
   42 | 	 * should be rendered, for instance by setting a status code.
   43 | 	 * @param request current HTTP request
   44 | 	 * @param response current HTTP response
   45 | 	 * @param handler the executed handler, or {@code null} if none chosen at the
   46 | 	 * time of the exception (for example, if multipart resolution failed)
   47 | 	 * @param ex the exception that got thrown during handler execution
   48 | 	 * @return a corresponding {@code ModelAndView} to forward to,
   49 | 	 * or {@code null} for default processing in the resolution chain
   50 | 	 */
   51 | 	@Nullable ModelAndView resolveException(
   52 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);
   53 | 
   54 | }
```
