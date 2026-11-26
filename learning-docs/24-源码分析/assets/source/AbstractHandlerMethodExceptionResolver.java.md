<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/AbstractHandlerMethodExceptionResolver.java -->
<!-- 正文引用：[[源码:AbstractHandlerMethodExceptionResolver.java#L起-L止]] ；代码块头部：(src: AbstractHandlerMethodExceptionResolver.java#L起-L止) -->

# AbstractHandlerMethodExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/AbstractHandlerMethodExceptionResolver.java` |
| 行数 | 92 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:10.664Z |

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
   17 | package org.springframework.web.servlet.handler;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpServletResponse;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.web.method.HandlerMethod;
   24 | import org.springframework.web.servlet.ModelAndView;
   25 | 
   26 | /**
   27 |  * Abstract base class for
   28 |  * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}
   29 |  * implementations that support handling exceptions from handlers of type {@link HandlerMethod}.
   30 |  *
   31 |  * @author Rossen Stoyanchev
   32 |  * @author Juergen Hoeller
   33 |  * @since 3.1
   34 |  */
   35 | public abstract class AbstractHandlerMethodExceptionResolver extends AbstractHandlerExceptionResolver {
   36 | 
   37 | 	/**
   38 | 	 * Checks if the handler is a {@link HandlerMethod} or the resolver has global exception
   39 | 	 * handlers and then delegates to the base class implementation of {@code #shouldApplyTo}
   40 | 	 * passing the bean of the {@code HandlerMethod} if necessary. Otherwise, returns {@code false}.
   41 | 	 * @see HandlerMethod
   42 | 	 * @see #hasGlobalExceptionHandlers()
   43 | 	 */
   44 | 	@Override
   45 | 	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
   46 | 		if (handler instanceof HandlerMethod handlerMethod) {
   47 | 			return super.shouldApplyTo(request, handlerMethod.getBean());
   48 | 		}
   49 | 		else if (handler == null || (hasGlobalExceptionHandlers() && hasHandlerMappings())) {
   50 | 			return super.shouldApplyTo(request, handler);
   51 | 		}
   52 | 		else {
   53 | 			return false;
   54 | 		}
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Whether this resolver has global exception handlers, for example, not declared in
   59 | 	 * the same class as the {@code HandlerMethod} that raised the exception and
   60 | 	 * therefore can apply to any handler.
   61 | 	 * @since 5.3
   62 | 	 */
   63 | 	protected boolean hasGlobalExceptionHandlers() {
   64 | 		return false;
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	protected final @Nullable ModelAndView doResolveException(
   69 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
   70 | 
   71 | 		HandlerMethod handlerMethod = (handler instanceof HandlerMethod hm ? hm : null);
   72 | 		return doResolveHandlerMethodException(request, response, handlerMethod, ex);
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Actually resolve the given exception that got thrown during on handler execution,
   77 | 	 * returning a ModelAndView that represents a specific error page if appropriate.
   78 | 	 * <p>May be overridden in subclasses, in order to apply specific exception checks.
   79 | 	 * Note that this template method will be invoked <i>after</i> checking whether this
   80 | 	 * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
   81 | 	 * with its actual exception handling.
   82 | 	 * @param request current HTTP request
   83 | 	 * @param response current HTTP response
   84 | 	 * @param handlerMethod the executed handler method, or {@code null} if none chosen at the time
   85 | 	 * of the exception (for example, if multipart resolution failed)
   86 | 	 * @param ex the exception that got thrown during handler execution
   87 | 	 * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
   88 | 	 */
   89 | 	protected abstract @Nullable ModelAndView doResolveHandlerMethodException(
   90 | 			HttpServletRequest request, HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception ex);
   91 | 
   92 | }
```
