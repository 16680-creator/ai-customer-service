<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/HttpEntityMethodProcessor.java -->
<!-- 正文引用：[[源码:HttpEntityMethodProcessor.java#L起-L止]] ；代码块头部：(src: HttpEntityMethodProcessor.java#L起-L止) -->

# HttpEntityMethodProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/HttpEntityMethodProcessor.java` |
| 行数 | 337 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.879Z |

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
   20 | import java.lang.reflect.ParameterizedType;
   21 | import java.lang.reflect.Type;
   22 | import java.util.ArrayList;
   23 | import java.util.Collections;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | 
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | import jakarta.servlet.http.HttpServletResponse;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.core.MethodParameter;
   32 | import org.springframework.core.ResolvableType;
   33 | import org.springframework.http.HttpEntity;
   34 | import org.springframework.http.HttpHeaders;
   35 | import org.springframework.http.HttpMethod;
   36 | import org.springframework.http.HttpStatus;
   37 | import org.springframework.http.HttpStatusCode;
   38 | import org.springframework.http.ProblemDetail;
   39 | import org.springframework.http.RequestEntity;
   40 | import org.springframework.http.ResponseEntity;
   41 | import org.springframework.http.converter.HttpMessageConverter;
   42 | import org.springframework.http.server.ServletServerHttpRequest;
   43 | import org.springframework.http.server.ServletServerHttpResponse;
   44 | import org.springframework.ui.ModelMap;
   45 | import org.springframework.util.Assert;
   46 | import org.springframework.util.CollectionUtils;
   47 | import org.springframework.util.StringUtils;
   48 | import org.springframework.web.ErrorResponse;
   49 | import org.springframework.web.HttpMediaTypeNotSupportedException;
   50 | import org.springframework.web.accept.ContentNegotiationManager;
   51 | import org.springframework.web.bind.support.WebDataBinderFactory;
   52 | import org.springframework.web.context.request.NativeWebRequest;
   53 | import org.springframework.web.context.request.ServletWebRequest;
   54 | import org.springframework.web.method.support.ModelAndViewContainer;
   55 | import org.springframework.web.servlet.mvc.support.RedirectAttributes;
   56 | import org.springframework.web.servlet.support.RequestContextUtils;
   57 | 
   58 | /**
   59 |  * Resolves {@link HttpEntity} and {@link RequestEntity} method argument values,
   60 |  * as well as return values of type {@link HttpEntity}, {@link ResponseEntity},
   61 |  * {@link ErrorResponse} and {@link ProblemDetail}.
   62 |  *
   63 |  * <p>An {@link HttpEntity} return type has a specific purpose. Therefore, this
   64 |  * handler should be configured ahead of handlers that support any return
   65 |  * value type annotated with {@code @ModelAttribute} or {@code @ResponseBody}
   66 |  * to ensure they don't take over.
   67 |  *
   68 |  * @author Arjen Poutsma
   69 |  * @author Rossen Stoyanchev
   70 |  * @author Brian Clozel
   71 |  * @author Sam Brannen
   72 |  * @since 3.1
   73 |  */
   74 | public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {
   75 | 
   76 | 	/**
   77 | 	 * Basic constructor with converters only. Suitable for resolving
   78 | 	 * {@code HttpEntity}. For handling {@code ResponseEntity} consider also
   79 | 	 * providing a {@code ContentNegotiationManager}.
   80 | 	 */
   81 | 	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters) {
   82 | 		super(converters);
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * Basic constructor with converters and {@code ContentNegotiationManager}.
   87 | 	 * Suitable for resolving {@code HttpEntity} and handling {@code ResponseEntity}
   88 | 	 * without {@code Request~} or {@code ResponseBodyAdvice}.
   89 | 	 */
   90 | 	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters, ContentNegotiationManager manager) {
   91 | 		super(converters, manager);
   92 | 	}
   93 | 
   94 | 	/**
   95 | 	 * Complete constructor for resolving {@code HttpEntity} method arguments.
   96 | 	 * For handling {@code ResponseEntity} consider also providing a
   97 | 	 * {@code ContentNegotiationManager}.
   98 | 	 * @since 4.2
   99 | 	 */
  100 | 	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
  101 | 			List<Object> requestResponseBodyAdvice) {
  102 | 
  103 | 		super(converters, null, requestResponseBodyAdvice);
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Variant of {@link #HttpEntityMethodProcessor(List, List)}
  108 | 	 * with an additional {@link ContentNegotiationManager} argument for return
  109 | 	 * value handling.
  110 | 	 */
  111 | 	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
  112 | 			@Nullable ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {
  113 | 
  114 | 		super(converters, manager, requestResponseBodyAdvice);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Variant of {@link #HttpEntityMethodProcessor(List, ContentNegotiationManager, List)}
  119 | 	 * with additional list of {@link ErrorResponse.Interceptor}s for return
  120 | 	 * value handling.
  121 | 	 * @since 6.2
  122 | 	 */
  123 | 	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
  124 | 			@Nullable ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice,
  125 | 			List<ErrorResponse.Interceptor> interceptors) {
  126 | 
  127 | 		super(converters, manager, requestResponseBodyAdvice, interceptors);
  128 | 	}
  129 | 
  130 | 
  131 | 	@Override
  132 | 	public boolean supportsParameter(MethodParameter parameter) {
  133 | 		return (HttpEntity.class == parameter.getParameterType() ||
  134 | 				RequestEntity.class == parameter.getParameterType());
  135 | 	}
  136 | 
  137 | 	@Override
  138 | 	public boolean supportsReturnType(MethodParameter returnType) {
  139 | 		Class<?> type = returnType.getParameterType();
  140 | 		return ((HttpEntity.class.isAssignableFrom(type) && !RequestEntity.class.isAssignableFrom(type)) ||
  141 | 				ErrorResponse.class.isAssignableFrom(type) || ProblemDetail.class.isAssignableFrom(type));
  142 | 	}
  143 | 
  144 | 	@Override
  145 | 	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
  146 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory)
  147 | 			throws IOException, HttpMediaTypeNotSupportedException {
  148 | 
  149 | 		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
  150 | 		Type paramType = getHttpEntityType(parameter);
  151 | 		if (paramType == null) {
  152 | 			throw new IllegalArgumentException("HttpEntity parameter '" + parameter.getParameterName() +
  153 | 					"' in method " + parameter.getMethod() + " is not parameterized");
  154 | 		}
  155 | 
  156 | 		Object body = readWithMessageConverters(inputMessage, parameter, paramType);
  157 | 		if (RequestEntity.class == parameter.getParameterType()) {
  158 | 			return new RequestEntity<>(body, inputMessage.getHeaders(),
  159 | 					inputMessage.getMethod(), inputMessage.getURI());
  160 | 		}
  161 | 		else {
  162 | 			return new HttpEntity<>(body, inputMessage.getHeaders());
  163 | 		}
  164 | 	}
  165 | 
  166 | 	private @Nullable Type getHttpEntityType(MethodParameter parameter) {
  167 | 		Assert.isAssignable(HttpEntity.class, parameter.getParameterType());
  168 | 		Type parameterType = parameter.getGenericParameterType();
  169 | 		if (parameterType instanceof ParameterizedType type) {
  170 | 			if (type.getActualTypeArguments().length != 1) {
  171 | 				throw new IllegalArgumentException("Expected single generic parameter on '" +
  172 | 						parameter.getParameterName() + "' in method " + parameter.getMethod());
  173 | 			}
  174 | 			return type.getActualTypeArguments()[0];
  175 | 		}
  176 | 		else if (parameterType instanceof Class) {
  177 | 			return Object.class;
  178 | 		}
  179 | 		else {
  180 | 			return null;
  181 | 		}
  182 | 	}
  183 | 
  184 | 	@Override
  185 | 	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
  186 | 			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
  187 | 
  188 | 		mavContainer.setRequestHandled(true);
  189 | 		if (returnValue == null) {
  190 | 			return;
  191 | 		}
  192 | 
  193 | 		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
  194 | 		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
  195 | 
  196 | 		HttpEntity<?> httpEntity;
  197 | 		if (returnValue instanceof ErrorResponse response) {
  198 | 			httpEntity = new ResponseEntity<>(response.getBody(), response.getHeaders(), response.getStatusCode());
  199 | 		}
  200 | 		else if (returnValue instanceof ProblemDetail detail) {
  201 | 			httpEntity = ResponseEntity.of(detail).build();
  202 | 		}
  203 | 		else {
  204 | 			Assert.isInstanceOf(HttpEntity.class, returnValue);
  205 | 			httpEntity = (HttpEntity<?>) returnValue;
  206 | 		}
  207 | 
  208 | 		if (httpEntity.getBody() instanceof ProblemDetail detail) {
  209 | 			if (logger.isWarnEnabled() && httpEntity instanceof ResponseEntity<?> responseEntity) {
  210 | 				if (responseEntity.getStatusCode().value() != detail.getStatus()) {
  211 | 					logger.warn(returnType.getExecutable().toGenericString() +
  212 | 							" returned ResponseEntity: " + responseEntity + ", but its status" +
  213 | 							" doesn't match the ProblemDetail status: " + detail.getStatus());
  214 | 				}
  215 | 			}
  216 | 			invokeErrorResponseInterceptors(
  217 | 					detail, (returnValue instanceof ErrorResponse response ? response : null), inputMessage);
  218 | 		}
  219 | 
  220 | 		HttpHeaders outputHeaders = outputMessage.getHeaders();
  221 | 		HttpHeaders entityHeaders = httpEntity.getHeaders();
  222 | 		if (!entityHeaders.isEmpty()) {
  223 | 			entityHeaders.forEach((key, value) -> {
  224 | 				if (HttpHeaders.VARY.equals(key) && outputHeaders.containsHeader(HttpHeaders.VARY)) {
  225 | 					List<String> values = getVaryRequestHeadersToAdd(outputHeaders, entityHeaders);
  226 | 					if (!values.isEmpty()) {
  227 | 						outputHeaders.addAll(HttpHeaders.VARY, values);
  228 | 					}
  229 | 				}
  230 | 				else {
  231 | 					outputHeaders.put(key, value);
  232 | 				}
  233 | 			});
  234 | 		}
  235 | 
  236 | 		if (httpEntity.getBody() instanceof ProblemDetail detail && !entityHeaders.containsHeader(HttpHeaders.CONTENT_TYPE)) {
  237 | 			HttpStatusCode status = (httpEntity instanceof ResponseEntity<?> responseEntity ?
  238 | 					responseEntity.getStatusCode() : HttpStatusCode.valueOf(detail.getStatus()));
  239 | 			ErrorResponse errorResponse = (returnValue instanceof ErrorResponse response ? response : null);
  240 | 			if (resolveErrorView(detail, errorResponse, status, mavContainer, webRequest)) {
  241 | 				return;
  242 | 			}
  243 | 		}
  244 | 
  245 | 		if (httpEntity instanceof ResponseEntity<?> responseEntity) {
  246 | 			HttpStatusCode returnStatus = responseEntity.getStatusCode();
  247 | 			outputMessage.getServletResponse().setStatus(returnStatus.value());
  248 | 			if (returnStatus.value() == HttpStatus.OK.value()) {
  249 | 				HttpMethod method = inputMessage.getMethod();
  250 | 				if ((HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)) &&
  251 | 						isResourceNotModified(inputMessage, outputMessage)) {
  252 | 					outputMessage.flush();
  253 | 					return;
  254 | 				}
  255 | 			}
  256 | 			else if (returnStatus.is3xxRedirection()) {
  257 | 				String location = outputHeaders.getFirst(HttpHeaders.LOCATION);
  258 | 				if (location != null) {
  259 | 					saveFlashAttributes(mavContainer, webRequest, location);
  260 | 				}
  261 | 			}
  262 | 		}
  263 | 
  264 | 		// Try even with null body. ResponseBodyAdvice could get involved.
  265 | 		writeWithMessageConverters(httpEntity.getBody(), returnType, inputMessage, outputMessage);
  266 | 
  267 | 		// Ensure headers are flushed even if no body was written.
  268 | 		outputMessage.flush();
  269 | 	}
  270 | 
  271 | 	private List<String> getVaryRequestHeadersToAdd(HttpHeaders responseHeaders, HttpHeaders entityHeaders) {
  272 | 		List<String> entityHeadersVary = entityHeaders.getVary();
  273 | 		List<String> vary = responseHeaders.get(HttpHeaders.VARY);
  274 | 		if (vary != null) {
  275 | 			List<String> result = new ArrayList<>(entityHeadersVary);
  276 | 			for (String header : vary) {
  277 | 				for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
  278 | 					if ("*".equals(existing)) {
  279 | 						return Collections.emptyList();
  280 | 					}
  281 | 					for (String value : entityHeadersVary) {
  282 | 						if (value.equalsIgnoreCase(existing)) {
  283 | 							result.remove(value);
  284 | 						}
  285 | 					}
  286 | 				}
  287 | 			}
  288 | 			return result;
  289 | 		}
  290 | 		return entityHeadersVary;
  291 | 	}
  292 | 
  293 | 	private boolean isResourceNotModified(ServletServerHttpRequest request, ServletServerHttpResponse response) {
  294 | 		ServletWebRequest servletWebRequest =
  295 | 				new ServletWebRequest(request.getServletRequest(), response.getServletResponse());
  296 | 		HttpHeaders responseHeaders = response.getHeaders();
  297 | 		String etag = responseHeaders.getETag();
  298 | 		long lastModifiedTimestamp = responseHeaders.getLastModified();
  299 | 		if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.HEAD) {
  300 | 			responseHeaders.remove(HttpHeaders.ETAG);
  301 | 			responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
  302 | 		}
  303 | 
  304 | 		return servletWebRequest.checkNotModified(etag, lastModifiedTimestamp);
  305 | 	}
  306 | 
  307 | 	private void saveFlashAttributes(ModelAndViewContainer mav, NativeWebRequest request, String location) {
  308 | 		mav.setRedirectModelScenario(true);
  309 | 		ModelMap model = mav.getModel();
  310 | 		if (model instanceof RedirectAttributes redirectAttributes) {
  311 | 			Map<String, ?> flashAttributes = redirectAttributes.getFlashAttributes();
  312 | 			if (!CollectionUtils.isEmpty(flashAttributes)) {
  313 | 				HttpServletRequest req = request.getNativeRequest(HttpServletRequest.class);
  314 | 				HttpServletResponse res = request.getNativeResponse(HttpServletResponse.class);
  315 | 				if (req != null) {
  316 | 					RequestContextUtils.getOutputFlashMap(req).putAll(flashAttributes);
  317 | 					if (res != null) {
  318 | 						RequestContextUtils.saveOutputFlashMap(location, req, res);
  319 | 					}
  320 | 				}
  321 | 			}
  322 | 		}
  323 | 	}
  324 | 
  325 | 	@Override
  326 | 	protected Class<?> getReturnValueType(@Nullable Object returnValue, MethodParameter returnType) {
  327 | 		if (returnValue != null) {
  328 | 			return returnValue.getClass();
  329 | 		}
  330 | 		else {
  331 | 			Type type = getHttpEntityType(returnType);
  332 | 			type = (type != null ? type : Object.class);
  333 | 			return ResolvableType.forMethodParameter(returnType, type).toClass();
  334 | 		}
  335 | 	}
  336 | 
  337 | }
```
