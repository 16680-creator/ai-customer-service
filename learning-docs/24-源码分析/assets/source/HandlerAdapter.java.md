<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HandlerAdapter.java -->
<!-- 正文引用：[[源码:HandlerAdapter.java#L起-L止]] ；代码块头部：(src: HandlerAdapter.java#L起-L止) -->

# HandlerAdapter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HandlerAdapter.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.276Z |

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
   24 |  * MVC framework SPI, allowing parameterization of the core MVC workflow.
   25 |  *
   26 |  * <p>Interface that must be implemented for each handler type to handle a request.
   27 |  * This interface is used to allow the {@link DispatcherServlet} to be indefinitely
   28 |  * extensible. The {@code DispatcherServlet} accesses all installed handlers through
   29 |  * this interface, meaning that it does not contain code specific to any handler type.
   30 |  *
   31 |  * <p>Note that a handler can be of type {@code Object}. This is to enable
   32 |  * handlers from other frameworks to be integrated with this framework without
   33 |  * custom coding, as well as to allow for annotation-driven handler objects that
   34 |  * do not obey any specific Java interface.
   35 |  *
   36 |  * <p>This interface is not intended for application developers. It is available
   37 |  * to handlers who want to develop their own web workflow.
   38 |  *
   39 |  * <p>Note: {@code HandlerAdapter} implementors may implement the {@link
   40 |  * org.springframework.core.Ordered} interface to be able to specify a sorting
   41 |  * order (and thus a priority) for getting applied by the {@code DispatcherServlet}.
   42 |  * Non-Ordered instances get treated as the lowest priority.
   43 |  *
   44 |  * @author Rod Johnson
   45 |  * @author Juergen Hoeller
   46 |  * @see org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
   47 |  * @see org.springframework.web.servlet.handler.SimpleServletHandlerAdapter
   48 |  */
   49 | public interface HandlerAdapter {
   50 | 
   51 | 	/**
   52 | 	 * Given a handler instance, return whether this {@code HandlerAdapter}
   53 | 	 * can support it. Typical HandlerAdapters will base the decision on the handler
   54 | 	 * type. HandlerAdapters will usually only support one handler type each.
   55 | 	 * <p>A typical implementation:
   56 | 	 * <p>{@code
   57 | 	 * return (handler instanceof MyHandler);
   58 | 	 * }
   59 | 	 * @param handler the handler object to check
   60 | 	 * @return whether this object can use the given handler
   61 | 	 */
   62 | 	boolean supports(Object handler);
   63 | 
   64 | 	/**
   65 | 	 * Use the given handler to handle this request.
   66 | 	 * The workflow that is required may vary widely.
   67 | 	 * @param request current HTTP request
   68 | 	 * @param response current HTTP response
   69 | 	 * @param handler the handler to use. This object must have previously been passed
   70 | 	 * to the {@code supports} method of this interface, which must have
   71 | 	 * returned {@code true}.
   72 | 	 * @return a ModelAndView object with the name of the view and the required
   73 | 	 * model data, or {@code null} if the request has been handled directly
   74 | 	 * @throws Exception in case of errors
   75 | 	 */
   76 | 	@Nullable ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
   77 | 
   78 | }
```
