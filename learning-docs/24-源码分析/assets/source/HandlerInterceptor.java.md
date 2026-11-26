<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HandlerInterceptor.java -->
<!-- 正文引用：[[源码:HandlerInterceptor.java#L起-L止]] ；代码块头部：(src: HandlerInterceptor.java#L起-L止) -->

# HandlerInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HandlerInterceptor.java` |
| 行数 | 158 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.426Z |

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
   23 | import org.springframework.web.method.HandlerMethod;
   24 | 
   25 | /**
   26 |  * Workflow interface that allows for customized handler execution chains.
   27 |  * Applications can register any number of existing or custom interceptors
   28 |  * for certain groups of handlers, to add common preprocessing behavior
   29 |  * without needing to modify each handler implementation.
   30 |  *
   31 |  * <p>A HandlerInterceptor gets called before the appropriate HandlerAdapter
   32 |  * triggers the execution of the handler itself. This mechanism can be used
   33 |  * for a large field of preprocessing aspects, or common handler behavior
   34 |  * like locale changes. Its main purpose is to allow for factoring
   35 |  * out repetitive handler code.
   36 |  *
   37 |  * <p>In an asynchronous processing scenario, the handler may be executed in a
   38 |  * separate thread while the main thread exits without rendering or invoking the
   39 |  * {@code postHandle} and {@code afterCompletion} callbacks. When concurrent
   40 |  * handler execution completes, the request is dispatched back in order to
   41 |  * proceed with rendering the model and all methods of this contract are invoked
   42 |  * again. For further options and details see
   43 |  * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
   44 |  *
   45 |  * <p>Typically an interceptor chain is defined per HandlerMapping bean,
   46 |  * sharing its granularity. To be able to apply a certain interceptor chain
   47 |  * to a group of handlers, one needs to map the desired handlers via one
   48 |  * HandlerMapping bean. The interceptors themselves are defined as beans
   49 |  * in the application context, referenced by the mapping bean definition
   50 |  * via its "interceptors" property (in XML: a &lt;list&gt; of &lt;ref&gt;).
   51 |  *
   52 |  * <p>HandlerInterceptor is basically similar to a Servlet Filter, but in
   53 |  * contrast to the latter it just allows custom pre-processing with the option
   54 |  * of prohibiting the execution of the handler itself, and custom post-processing.
   55 |  * Filters are more powerful, for example they allow for exchanging the request
   56 |  * and response objects that are handed down the chain. Note that a filter
   57 |  * gets configured in web.xml, a HandlerInterceptor in the application context.
   58 |  *
   59 |  * <p>As a basic guideline, fine-grained handler-related preprocessing tasks are
   60 |  * candidates for HandlerInterceptor implementations, especially factored-out
   61 |  * common handler code and authorization checks. On the other hand, a Filter
   62 |  * is well-suited for request content and view content handling, like multipart
   63 |  * forms and GZIP compression. This typically shows when one needs to map the
   64 |  * filter to certain content types (for example, images), or to all requests.
   65 |  *
   66 |  * <p><strong>Note:</strong> Interceptors are not ideally suited as a security
   67 |  * layer due to the potential for a mismatch with annotated controller path matching.
   68 |  * Generally, we recommend using Spring Security, or alternatively a similar
   69 |  * approach integrated with the Servlet filter chain, and applied as early as
   70 |  * possible.
   71 |  *
   72 |  * @author Juergen Hoeller
   73 |  * @since 20.06.2003
   74 |  * @see HandlerExecutionChain#getInterceptors
   75 |  * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
   76 |  * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
   77 |  * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
   78 |  * @see jakarta.servlet.Filter
   79 |  */
   80 | public interface HandlerInterceptor {
   81 | 
   82 | 	/**
   83 | 	 * Interception point before the execution of a handler. Called after
   84 | 	 * HandlerMapping determined an appropriate handler object, but before
   85 | 	 * HandlerAdapter invokes the handler.
   86 | 	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
   87 | 	 * of any number of interceptors, with the handler itself at the end.
   88 | 	 * With this method, each interceptor can decide to abort the execution chain,
   89 | 	 * typically sending an HTTP error or writing a custom response.
   90 | 	 * <p><strong>Note:</strong> special considerations apply for asynchronous
   91 | 	 * request processing. For more details see
   92 | 	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
   93 | 	 * <p>The default implementation returns {@code true}.
   94 | 	 * @param request current HTTP request
   95 | 	 * @param response current HTTP response
   96 | 	 * @param handler chosen handler to execute, for type and/or instance evaluation
   97 | 	 * @return {@code true} if the execution chain should proceed with the
   98 | 	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
   99 | 	 * that this interceptor has already dealt with the response itself.
  100 | 	 * @throws Exception in case of errors
  101 | 	 */
  102 | 	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
  103 | 			throws Exception {
  104 | 
  105 | 		return true;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Interception point after successful execution of a handler.
  110 | 	 * Called after HandlerAdapter actually invoked the handler, but before the
  111 | 	 * DispatcherServlet renders the view. Can expose additional model objects
  112 | 	 * to the view via the given ModelAndView.
  113 | 	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
  114 | 	 * of any number of interceptors, with the handler itself at the end.
  115 | 	 * With this method, each interceptor can post-process an execution,
  116 | 	 * getting applied in inverse order of the execution chain.
  117 | 	 * <p><strong>Note:</strong> special considerations apply for asynchronous
  118 | 	 * request processing. For more details see
  119 | 	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
  120 | 	 * <p>The default implementation is empty.
  121 | 	 * @param request current HTTP request
  122 | 	 * @param response current HTTP response
  123 | 	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
  124 | 	 * execution, for type and/or instance examination
  125 | 	 * @param modelAndView the {@code ModelAndView} that the handler returned
  126 | 	 * (can also be {@code null})
  127 | 	 * @throws Exception in case of errors
  128 | 	 */
  129 | 	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
  130 | 			@Nullable ModelAndView modelAndView) throws Exception {
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Callback after completion of request processing, that is, after rendering
  135 | 	 * the view. Will be called on any outcome of handler execution, thus allows
  136 | 	 * for proper resource cleanup.
  137 | 	 * <p>Note: Will only be called if this interceptor's {@code preHandle}
  138 | 	 * method has successfully completed and returned {@code true}!
  139 | 	 * <p>As with the {@code postHandle} method, the method will be invoked on each
  140 | 	 * interceptor in the chain in reverse order, so the first interceptor will be
  141 | 	 * the last to be invoked.
  142 | 	 * <p><strong>Note:</strong> special considerations apply for asynchronous
  143 | 	 * request processing. For more details see
  144 | 	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
  145 | 	 * <p>The default implementation is empty.
  146 | 	 * @param request current HTTP request
  147 | 	 * @param response current HTTP response
  148 | 	 * @param handler the handler (or {@link HandlerMethod}) that started asynchronous
  149 | 	 * execution, for type and/or instance examination
  150 | 	 * @param ex any exception thrown on handler execution, if any; this does not
  151 | 	 * include exceptions that have been handled through an exception resolver
  152 | 	 * @throws Exception in case of errors
  153 | 	 */
  154 | 	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
  155 | 			@Nullable Exception ex) throws Exception {
  156 | 	}
  157 | 
  158 | }
```
