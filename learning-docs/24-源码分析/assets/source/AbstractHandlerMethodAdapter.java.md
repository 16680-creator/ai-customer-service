<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter.java -->
<!-- 正文引用：[[源码:AbstractHandlerMethodAdapter.java#L起-L止]] ；代码块头部：(src: AbstractHandlerMethodAdapter.java#L起-L止) -->

# AbstractHandlerMethodAdapter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter.java` |
| 行数 | 102 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:11.282Z |

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
   17 | package org.springframework.web.servlet.mvc.method;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpServletResponse;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.core.Ordered;
   24 | import org.springframework.web.method.HandlerMethod;
   25 | import org.springframework.web.servlet.HandlerAdapter;
   26 | import org.springframework.web.servlet.ModelAndView;
   27 | import org.springframework.web.servlet.support.WebContentGenerator;
   28 | 
   29 | /**
   30 |  * Abstract base class for {@link HandlerAdapter} implementations that support
   31 |  * handlers of type {@link HandlerMethod}.
   32 |  *
   33 |  * @author Arjen Poutsma
   34 |  * @since 3.1
   35 |  */
   36 | public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {
   37 | 
   38 | 	private int order = Ordered.LOWEST_PRECEDENCE;
   39 | 
   40 | 
   41 | 	public AbstractHandlerMethodAdapter() {
   42 | 		// no restriction of HTTP methods by default
   43 | 		super(false);
   44 | 	}
   45 | 
   46 | 
   47 | 	/**
   48 | 	 * Specify the order value for this HandlerAdapter bean.
   49 | 	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   50 | 	 * @see org.springframework.core.Ordered#getOrder()
   51 | 	 */
   52 | 	public void setOrder(int order) {
   53 | 		this.order = order;
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public int getOrder() {
   58 | 		return this.order;
   59 | 	}
   60 | 
   61 | 
   62 | 	/**
   63 | 	 * This implementation expects the handler to be an {@link HandlerMethod}.
   64 | 	 * @param handler the handler instance to check
   65 | 	 * @return whether this adapter can adapt the given handler
   66 | 	 */
   67 | 	@Override
   68 | 	public final boolean supports(Object handler) {
   69 | 		return (handler instanceof HandlerMethod handlerMethod && supportsInternal(handlerMethod));
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Given a handler method, return whether this adapter can support it.
   74 | 	 * @param handlerMethod the handler method to check
   75 | 	 * @return whether this adapter can adapt the given method
   76 | 	 */
   77 | 	protected abstract boolean supportsInternal(HandlerMethod handlerMethod);
   78 | 
   79 | 	/**
   80 | 	 * This implementation expects the handler to be an {@link HandlerMethod}.
   81 | 	 */
   82 | 	@Override
   83 | 	public final @Nullable ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
   84 | 			throws Exception {
   85 | 
   86 | 		return handleInternal(request, response, (HandlerMethod) handler);
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Use the given handler method to handle the request.
   91 | 	 * @param request current HTTP request
   92 | 	 * @param response current HTTP response
   93 | 	 * @param handlerMethod handler method to use. This object must have previously been passed to the
   94 | 	 * {@link #supportsInternal(HandlerMethod)} this interface, which must have returned {@code true}.
   95 | 	 * @return a ModelAndView object with the name of the view and the required model data,
   96 | 	 * or {@code null} if the request has been handled directly
   97 | 	 * @throws Exception in case of errors
   98 | 	 */
   99 | 	protected abstract @Nullable ModelAndView handleInternal(HttpServletRequest request,
  100 | 			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;
  101 | 
  102 | }
```
