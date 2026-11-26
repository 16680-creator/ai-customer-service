<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/ServletInvocableHandlerMethod.java -->
<!-- 正文引用：[[源码:ServletInvocableHandlerMethod.java#L起-L止]] ；代码块头部：(src: ServletInvocableHandlerMethod.java#L起-L止) -->

# ServletInvocableHandlerMethod.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/ServletInvocableHandlerMethod.java` |
| 行数 | 327 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.153Z |

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
   17 | package org.springframework.web.servlet.mvc.method.annotation;
   18 | 
   19 | import java.io.IOException;
   20 | import java.lang.annotation.Annotation;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Type;
   23 | import java.util.concurrent.Callable;
   24 | 
   25 | import jakarta.servlet.ServletException;
   26 | import jakarta.servlet.http.HttpServletRequest;
   27 | import jakarta.servlet.http.HttpServletResponse;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.context.MessageSource;
   31 | import org.springframework.core.KotlinDetector;
   32 | import org.springframework.core.MethodParameter;
   33 | import org.springframework.core.ResolvableType;
   34 | import org.springframework.http.HttpHeaders;
   35 | import org.springframework.http.HttpStatusCode;
   36 | import org.springframework.util.Assert;
   37 | import org.springframework.util.ClassUtils;
   38 | import org.springframework.util.StringUtils;
   39 | import org.springframework.web.bind.annotation.ResponseBody;
   40 | import org.springframework.web.bind.annotation.ResponseStatus;
   41 | import org.springframework.web.context.request.ServletWebRequest;
   42 | import org.springframework.web.method.HandlerMethod;
   43 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   44 | import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
   45 | import org.springframework.web.method.support.InvocableHandlerMethod;
   46 | import org.springframework.web.method.support.ModelAndViewContainer;
   47 | import org.springframework.web.servlet.View;
   48 | import org.springframework.web.servlet.mvc.method.annotation.ReactiveTypeHandler.CollectedValuesList;
   49 | 
   50 | /**
   51 |  * Extends {@link InvocableHandlerMethod} with the ability to handle return
   52 |  * values through a registered {@link HandlerMethodReturnValueHandler} and
   53 |  * also supports setting the response status based on a method-level
   54 |  * {@code @ResponseStatus} annotation.
   55 |  *
   56 |  * <p>A {@code null} return value (including void) may be interpreted as the
   57 |  * end of request processing in combination with a {@code @ResponseStatus}
   58 |  * annotation, a not-modified check condition
   59 |  * (see {@link ServletWebRequest#checkNotModified(long)}), or
   60 |  * a method argument that provides access to the response stream.
   61 |  *
   62 |  * @author Rossen Stoyanchev
   63 |  * @author Juergen Hoeller
   64 |  * @since 3.1
   65 |  */
   66 | public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {
   67 | 
   68 | 	private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");
   69 | 
   70 | 	private @Nullable HandlerMethodReturnValueHandlerComposite returnValueHandlers;
   71 | 
   72 | 
   73 | 	/**
   74 | 	 * Creates an instance from the given handler and method.
   75 | 	 */
   76 | 	public ServletInvocableHandlerMethod(Object handler, Method method) {
   77 | 		super(handler, method);
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Variant of {@link #ServletInvocableHandlerMethod(Object, Method)} that
   82 | 	 * also accepts a {@link MessageSource}, for example, to resolve
   83 | 	 * {@code @ResponseStatus} messages with.
   84 | 	 * @since 5.3.10
   85 | 	 */
   86 | 	public ServletInvocableHandlerMethod(Object handler, Method method, @Nullable MessageSource messageSource) {
   87 | 		super(handler, method, messageSource);
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Create an instance from a {@code HandlerMethod}.
   92 | 	 */
   93 | 	public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
   94 | 		super(handlerMethod);
   95 | 	}
   96 | 
   97 | 
   98 | 	/**
   99 | 	 * Register {@link HandlerMethodReturnValueHandler} instances to use to
  100 | 	 * handle return values.
  101 | 	 */
  102 | 	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
  103 | 		this.returnValueHandlers = returnValueHandlers;
  104 | 	}
  105 | 
  106 | 
  107 | 	/**
  108 | 	 * Invoke the method and handle the return value through one of the
  109 | 	 * configured {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
  110 | 	 * @param webRequest the current request
  111 | 	 * @param mavContainer the ModelAndViewContainer for this request
  112 | 	 * @param providedArgs "given" arguments matched by type (not resolved)
  113 | 	 */
  114 | 	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
  115 | 			@Nullable Object... providedArgs) throws Exception {
  116 | 
  117 | 		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
  118 | 		setResponseStatus(webRequest);
  119 | 
  120 | 		if (returnValue == null) {
  121 | 			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
  122 | 				disableContentCachingIfNecessary(webRequest);
  123 | 				mavContainer.setRequestHandled(true);
  124 | 				return;
  125 | 			}
  126 | 		}
  127 | 		else if (StringUtils.hasText(getResponseStatusReason())) {
  128 | 			mavContainer.setRequestHandled(true);
  129 | 			return;
  130 | 		}
  131 | 
  132 | 		mavContainer.setRequestHandled(false);
  133 | 		Assert.state(this.returnValueHandlers != null, "No return value handlers");
  134 | 		try {
  135 | 			this.returnValueHandlers.handleReturnValue(
  136 | 					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
  137 | 		}
  138 | 		catch (Exception ex) {
  139 | 			if (logger.isTraceEnabled()) {
  140 | 				logger.trace(formatErrorForReturnValue(returnValue), ex);
  141 | 			}
  142 | 			throw ex;
  143 | 		}
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Set the response status according to the {@link ResponseStatus} annotation.
  148 | 	 */
  149 | 	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
  150 | 		HttpStatusCode status = getResponseStatus();
  151 | 		if (status == null) {
  152 | 			return;
  153 | 		}
  154 | 
  155 | 		HttpServletResponse response = webRequest.getResponse();
  156 | 		if (response != null) {
  157 | 			String reason = getResponseStatusReason();
  158 | 			if (StringUtils.hasText(reason)) {
  159 | 				response.sendError(status.value(), reason);
  160 | 			}
  161 | 			else {
  162 | 				response.setStatus(status.value());
  163 | 			}
  164 | 		}
  165 | 
  166 | 		// To be picked up by RedirectView
  167 | 		webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Does the given request qualify as "not modified"?
  172 | 	 * @see ServletWebRequest#checkNotModified(long)
  173 | 	 * @see ServletWebRequest#checkNotModified(String)
  174 | 	 */
  175 | 	private boolean isRequestNotModified(ServletWebRequest webRequest) {
  176 | 		return webRequest.isNotModified();
  177 | 	}
  178 | 
  179 | 	private void disableContentCachingIfNecessary(ServletWebRequest webRequest) {
  180 | 		if (isRequestNotModified(webRequest)) {
  181 | 			HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
  182 | 			Assert.notNull(response, "Expected HttpServletResponse");
  183 | 			if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
  184 | 				HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
  185 | 				Assert.notNull(request, "Expected HttpServletRequest");
  186 | 			}
  187 | 		}
  188 | 	}
  189 | 
  190 | 	private String formatErrorForReturnValue(@Nullable Object returnValue) {
  191 | 		return "Error handling return value=[" + returnValue + "]" +
  192 | 				(returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
  193 | 				" in " + toString();
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Create a nested ServletInvocableHandlerMethod subclass that returns the
  198 | 	 * given value (or raises an Exception if the value is one) rather than
  199 | 	 * actually invoking the controller method. This is useful when processing
  200 | 	 * async return values (for example, Callable, DeferredResult, CompletableFuture).
  201 | 	 */
  202 | 	ServletInvocableHandlerMethod wrapConcurrentResult(@Nullable Object result) {
  203 | 		return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
  204 | 	}
  205 | 
  206 | 
  207 | 	/**
  208 | 	 * A nested subclass of {@code ServletInvocableHandlerMethod} that uses a
  209 | 	 * simple {@link Callable} instead of the original controller as the handler in
  210 | 	 * order to return the fixed (concurrent) result value given to it. Effectively
  211 | 	 * "resumes" processing with the asynchronously produced return value.
  212 | 	 */
  213 | 	private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {
  214 | 
  215 | 		private final MethodParameter returnType;
  216 | 
  217 | 		public ConcurrentResultHandlerMethod(@Nullable Object result, ConcurrentResultMethodParameter returnType) {
  218 | 			super((Callable<Object>) () -> {
  219 | 				if (result instanceof Exception exception) {
  220 | 					throw exception;
  221 | 				}
  222 | 				else if (result instanceof Throwable throwable) {
  223 | 					throw new ServletException("Async processing failed: " + result, throwable);
  224 | 				}
  225 | 				return result;
  226 | 			}, CALLABLE_METHOD);
  227 | 
  228 | 			if (ServletInvocableHandlerMethod.this.returnValueHandlers != null) {
  229 | 				setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
  230 | 			}
  231 | 			this.returnType = returnType;
  232 | 		}
  233 | 
  234 | 		/**
  235 | 		 * Bridge to actual controller type-level annotations.
  236 | 		 */
  237 | 		@Override
  238 | 		public Class<?> getBeanType() {
  239 | 			return ServletInvocableHandlerMethod.this.getBeanType();
  240 | 		}
  241 | 
  242 | 		/**
  243 | 		 * Bridge to actual return value or generic type within the declared
  244 | 		 * async return type, for example, Foo instead of {@code DeferredResult<Foo>}.
  245 | 		 */
  246 | 		@Override
  247 | 		public MethodParameter getReturnValueType(@Nullable Object returnValue) {
  248 | 			return this.returnType;
  249 | 		}
  250 | 
  251 | 		/**
  252 | 		 * Bridge to controller method-level annotations.
  253 | 		 */
  254 | 		@Override
  255 | 		public <A extends Annotation> @Nullable A getMethodAnnotation(Class<A> annotationType) {
  256 | 			return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
  257 | 		}
  258 | 
  259 | 		/**
  260 | 		 * Bridge to controller method-level annotations.
  261 | 		 */
  262 | 		@Override
  263 | 		public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
  264 | 			return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
  265 | 		}
  266 | 	}
  267 | 
  268 | 
  269 | 	/**
  270 | 	 * MethodParameter subclass based on the actual return value type or if
  271 | 	 * that's null falling back on the generic type within the declared async
  272 | 	 * return type, for example, Foo instead of {@code DeferredResult<Foo>}.
  273 | 	 */
  274 | 	private class ConcurrentResultMethodParameter extends AnnotatedMethodParameter {
  275 | 
  276 | 		private final @Nullable Object returnValue;
  277 | 
  278 | 		private final ResolvableType returnType;
  279 | 
  280 | 		public ConcurrentResultMethodParameter(@Nullable Object returnValue) {
  281 | 			super(-1);
  282 | 			this.returnValue = returnValue;
  283 | 			this.returnType = (returnValue instanceof CollectedValuesList cvList ?
  284 | 					cvList.getReturnType() :
  285 | 					KotlinDetector.isSuspendingFunction(super.getMethod()) ?
  286 | 					ResolvableType.forMethodParameter(getReturnType()) :
  287 | 					ResolvableType.forType(super.getGenericParameterType()).getGeneric());
  288 | 		}
  289 | 
  290 | 		public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
  291 | 			super(original);
  292 | 			this.returnValue = original.returnValue;
  293 | 			this.returnType = original.returnType;
  294 | 		}
  295 | 
  296 | 		@Override
  297 | 		public Class<?> getParameterType() {
  298 | 			if (this.returnValue != null) {
  299 | 				return this.returnValue.getClass();
  300 | 			}
  301 | 			if (!ResolvableType.NONE.equals(this.returnType)) {
  302 | 				return this.returnType.toClass();
  303 | 			}
  304 | 			return super.getParameterType();
  305 | 		}
  306 | 
  307 | 		@Override
  308 | 		public Type getGenericParameterType() {
  309 | 			return this.returnType.getType();
  310 | 		}
  311 | 
  312 | 		@Override
  313 | 		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
  314 | 			// Ensure @ResponseBody-style handling for values collected from a reactive type
  315 | 			// even if actual return type is ResponseEntity<Flux<T>>
  316 | 			return (super.hasMethodAnnotation(annotationType) ||
  317 | 					(annotationType == ResponseBody.class &&
  318 | 							this.returnValue instanceof CollectedValuesList));
  319 | 		}
  320 | 
  321 | 		@Override
  322 | 		public ConcurrentResultMethodParameter clone() {
  323 | 			return new ConcurrentResultMethodParameter(this);
  324 | 		}
  325 | 	}
  326 | 
  327 | }
```
