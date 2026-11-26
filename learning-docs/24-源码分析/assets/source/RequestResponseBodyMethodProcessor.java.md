<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/RequestResponseBodyMethodProcessor.java -->
<!-- 正文引用：[[源码:RequestResponseBodyMethodProcessor.java#L起-L止]] ；代码块头部：(src: RequestResponseBodyMethodProcessor.java#L起-L止) -->

# RequestResponseBodyMethodProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/RequestResponseBodyMethodProcessor.java` |
| 行数 | 214 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.736Z |

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
   20 | import java.lang.reflect.Type;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.concurrent.ConcurrentHashMap;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.core.Conventions;
   28 | import org.springframework.core.MethodParameter;
   29 | import org.springframework.core.ResolvableType;
   30 | import org.springframework.core.annotation.AnnotatedElementUtils;
   31 | import org.springframework.http.HttpStatusCode;
   32 | import org.springframework.http.ProblemDetail;
   33 | import org.springframework.http.converter.HttpMessageConverter;
   34 | import org.springframework.http.converter.HttpMessageNotReadableException;
   35 | import org.springframework.http.converter.HttpMessageNotWritableException;
   36 | import org.springframework.http.server.ServletServerHttpRequest;
   37 | import org.springframework.http.server.ServletServerHttpResponse;
   38 | import org.springframework.validation.BindingResult;
   39 | import org.springframework.web.ErrorResponse;
   40 | import org.springframework.web.HttpMediaTypeNotAcceptableException;
   41 | import org.springframework.web.HttpMediaTypeNotSupportedException;
   42 | import org.springframework.web.accept.ContentNegotiationManager;
   43 | import org.springframework.web.bind.MethodArgumentNotValidException;
   44 | import org.springframework.web.bind.WebDataBinder;
   45 | import org.springframework.web.bind.annotation.RequestBody;
   46 | import org.springframework.web.bind.annotation.ResponseBody;
   47 | import org.springframework.web.bind.support.WebDataBinderFactory;
   48 | import org.springframework.web.context.request.NativeWebRequest;
   49 | import org.springframework.web.method.support.ModelAndViewContainer;
   50 | import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
   51 | 
   52 | /**
   53 |  * Resolves method arguments annotated with {@code @RequestBody} and handles return
   54 |  * values from methods annotated with {@code @ResponseBody} by reading and writing
   55 |  * to the body of the request or response with an {@link HttpMessageConverter}.
   56 |  *
   57 |  * <p>An {@code @RequestBody} method argument is also validated if it is annotated
   58 |  * with any
   59 |  * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
   60 |  * annotations that trigger validation}. In case of validation failure,
   61 |  * {@link MethodArgumentNotValidException} is raised and results in an HTTP 400
   62 |  * response status code if {@link DefaultHandlerExceptionResolver} is configured.
   63 |  *
   64 |  * @author Arjen Poutsma
   65 |  * @author Rossen Stoyanchev
   66 |  * @author Juergen Hoeller
   67 |  * @since 3.1
   68 |  */
   69 | public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {
   70 | 
   71 | 	private final Map<Class<?>, Boolean> responseBodyControllerCache = new ConcurrentHashMap<>();
   72 | 
   73 | 
   74 | 	/**
   75 | 	 * Basic constructor with converters only. Suitable for resolving
   76 | 	 * {@code @RequestBody}. For handling {@code @ResponseBody} consider also
   77 | 	 * providing a {@code ContentNegotiationManager}.
   78 | 	 */
   79 | 	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters) {
   80 | 		super(converters);
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Basic constructor with converters and {@code ContentNegotiationManager}.
   85 | 	 * Suitable for resolving {@code @RequestBody} and handling
   86 | 	 * {@code @ResponseBody} without {@code Request~} or
   87 | 	 * {@code ResponseBodyAdvice}.
   88 | 	 */
   89 | 	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
   90 | 			@Nullable ContentNegotiationManager manager) {
   91 | 
   92 | 		super(converters, manager);
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Complete constructor for resolving {@code @RequestBody} method arguments.
   97 | 	 * For handling {@code @ResponseBody} consider also providing a
   98 | 	 * {@code ContentNegotiationManager}.
   99 | 	 * @since 4.2
  100 | 	 */
  101 | 	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
  102 | 			@Nullable List<Object> requestResponseBodyAdvice) {
  103 | 
  104 | 		super(converters, null, requestResponseBodyAdvice);
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Variant of {@link #RequestResponseBodyMethodProcessor(List, List)}
  109 | 	 * with an additional {@link ContentNegotiationManager} argument, for return
  110 | 	 * value handling.
  111 | 	 */
  112 | 	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
  113 | 			@Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {
  114 | 
  115 | 		super(converters, manager, requestResponseBodyAdvice);
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Variant of{@link #RequestResponseBodyMethodProcessor(List, ContentNegotiationManager, List)}
  120 | 	 * with an additional {@link ErrorResponse.Interceptor} argument for return
  121 | 	 * value handling.
  122 | 	 * @since 6.2
  123 | 	 */
  124 | 	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
  125 | 			@Nullable ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice,
  126 | 			List<ErrorResponse.Interceptor> interceptors) {
  127 | 
  128 | 		super(converters, manager, requestResponseBodyAdvice, interceptors);
  129 | 	}
  130 | 
  131 | 
  132 | 	@Override
  133 | 	public boolean supportsParameter(MethodParameter parameter) {
  134 | 		return parameter.hasParameterAnnotation(RequestBody.class);
  135 | 	}
  136 | 
  137 | 	@Override
  138 | 	public boolean supportsReturnType(MethodParameter returnType) {
  139 | 		return (this.responseBodyControllerCache.computeIfAbsent(returnType.getContainingClass(),
  140 | 				clazz -> AnnotatedElementUtils.hasAnnotation(clazz, ResponseBody.class)) ||
  141 | 				returnType.hasMethodAnnotation(ResponseBody.class));
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Throws MethodArgumentNotValidException if validation fails.
  146 | 	 * @throws HttpMessageNotReadableException if {@link RequestBody#required()}
  147 | 	 * is {@code true} and there is no body content or if there is no suitable
  148 | 	 * converter to read the content with.
  149 | 	 */
  150 | 	@Override
  151 | 	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
  152 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
  153 | 
  154 | 		parameter = parameter.nestedIfOptional();
  155 | 		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
  156 | 
  157 | 		if (binderFactory != null) {
  158 | 			String name = Conventions.getVariableNameForParameter(parameter);
  159 | 			ResolvableType type = ResolvableType.forMethodParameter(parameter);
  160 | 			WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name, type);
  161 | 			if (arg != null) {
  162 | 				validateIfApplicable(binder, parameter);
  163 | 				if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
  164 | 					throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
  165 | 				}
  166 | 			}
  167 | 			if (mavContainer != null) {
  168 | 				mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
  169 | 			}
  170 | 		}
  171 | 
  172 | 		return adaptArgumentIfNecessary(arg, parameter);
  173 | 	}
  174 | 
  175 | 	@Override
  176 | 	protected @Nullable Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
  177 | 			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
  178 | 
  179 | 		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
  180 | 		Object arg = readWithMessageConverters(inputMessage, parameter, paramType);
  181 | 		if (arg == null && checkRequired(parameter)) {
  182 | 			throw new HttpMessageNotReadableException("Required request body is missing: " +
  183 | 					parameter.getExecutable().toGenericString(), inputMessage);
  184 | 		}
  185 | 		return arg;
  186 | 	}
  187 | 
  188 | 	protected boolean checkRequired(MethodParameter parameter) {
  189 | 		RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
  190 | 		return (requestBody != null && requestBody.required() && !parameter.isOptional());
  191 | 	}
  192 | 
  193 | 	@Override
  194 | 	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
  195 | 			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
  196 | 			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
  197 | 
  198 | 		mavContainer.setRequestHandled(true);
  199 | 		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
  200 | 		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
  201 | 
  202 | 		if (returnValue instanceof ProblemDetail detail) {
  203 | 			outputMessage.setStatusCode(HttpStatusCode.valueOf(detail.getStatus()));
  204 | 			invokeErrorResponseInterceptors(detail, null, inputMessage);
  205 | 			if (resolveErrorView(detail, null, HttpStatusCode.valueOf(detail.getStatus()), mavContainer, webRequest)) {
  206 | 				return;
  207 | 			}
  208 | 		}
  209 | 
  210 | 		// Try even with null return value. ResponseBodyAdvice could get involved.
  211 | 		writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
  212 | 	}
  213 | 
  214 | }
```
