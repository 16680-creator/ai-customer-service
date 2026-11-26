<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/HandlerMethodReturnValueHandlerComposite.java -->
<!-- 正文引用：[[源码:HandlerMethodReturnValueHandlerComposite.java#L起-L止]] ；代码块头部：(src: HandlerMethodReturnValueHandlerComposite.java#L起-L止) -->

# HandlerMethodReturnValueHandlerComposite.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/HandlerMethodReturnValueHandlerComposite.java` |
| 行数 | 123 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:19.928Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.core.MethodParameter;
   26 | import org.springframework.web.context.request.NativeWebRequest;
   27 | 
   28 | /**
   29 |  * Handles method return values by delegating to a list of registered
   30 |  * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
   31 |  *
   32 |  * @author Rossen Stoyanchev
   33 |  * @since 3.1
   34 |  */
   35 | public class HandlerMethodReturnValueHandlerComposite implements HandlerMethodReturnValueHandler {
   36 | 
   37 | 	private final List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
   38 | 
   39 | 
   40 | 	/**
   41 | 	 * Return a read-only list with the registered handlers, or an empty list.
   42 | 	 */
   43 | 	public List<HandlerMethodReturnValueHandler> getHandlers() {
   44 | 		return Collections.unmodifiableList(this.returnValueHandlers);
   45 | 	}
   46 | 
   47 | 	/**
   48 | 	 * Whether the given {@linkplain MethodParameter method return type} is supported by any registered
   49 | 	 * {@link HandlerMethodReturnValueHandler}.
   50 | 	 */
   51 | 	@Override
   52 | 	public boolean supportsReturnType(MethodParameter returnType) {
   53 | 		return getReturnValueHandler(returnType) != null;
   54 | 	}
   55 | 
   56 | 	private @Nullable HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
   57 | 		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
   58 | 			if (handler.supportsReturnType(returnType)) {
   59 | 				return handler;
   60 | 			}
   61 | 		}
   62 | 		return null;
   63 | 	}
   64 | 
   65 | 	/**
   66 | 	 * Iterate over registered {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} and invoke the one that supports it.
   67 | 	 * @throws IllegalStateException if no suitable {@link HandlerMethodReturnValueHandler} is found.
   68 | 	 */
   69 | 	@Override
   70 | 	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
   71 | 			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
   72 | 
   73 | 		HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
   74 | 		if (handler == null) {
   75 | 			throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
   76 | 		}
   77 | 		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
   78 | 	}
   79 | 
   80 | 	private @Nullable HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {
   81 | 		boolean isAsyncValue = isAsyncReturnValue(value, returnType);
   82 | 		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
   83 | 			if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
   84 | 				continue;
   85 | 			}
   86 | 			if (handler.supportsReturnType(returnType)) {
   87 | 				return handler;
   88 | 			}
   89 | 		}
   90 | 		return null;
   91 | 	}
   92 | 
   93 | 	private boolean isAsyncReturnValue(@Nullable Object value, MethodParameter returnType) {
   94 | 		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
   95 | 			if (handler instanceof AsyncHandlerMethodReturnValueHandler asyncHandler &&
   96 | 					asyncHandler.isAsyncReturnValue(value, returnType)) {
   97 | 				return true;
   98 | 			}
   99 | 		}
  100 | 		return false;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Add the given {@link HandlerMethodReturnValueHandler}.
  105 | 	 */
  106 | 	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler handler) {
  107 | 		this.returnValueHandlers.add(handler);
  108 | 		return this;
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Add the given {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
  113 | 	 */
  114 | 	public HandlerMethodReturnValueHandlerComposite addHandlers(
  115 | 			@Nullable List<? extends HandlerMethodReturnValueHandler> handlers) {
  116 | 
  117 | 		if (handlers != null) {
  118 | 			this.returnValueHandlers.addAll(handlers);
  119 | 		}
  120 | 		return this;
  121 | 	}
  122 | 
  123 | }
```
