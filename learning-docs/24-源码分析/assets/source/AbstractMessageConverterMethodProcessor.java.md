<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java -->
<!-- 正文引用：[[源码:AbstractMessageConverterMethodProcessor.java#L起-L止]] ；代码块头部：(src: AbstractMessageConverterMethodProcessor.java#L起-L止) -->

# AbstractMessageConverterMethodProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java` |
| 行数 | 694 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.316Z |

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
   21 | import java.util.ArrayList;
   22 | import java.util.Collections;
   23 | import java.util.HashSet;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Locale;
   27 | import java.util.Set;
   28 | 
   29 | import jakarta.servlet.ServletRequest;
   30 | import jakarta.servlet.http.HttpServletRequest;
   31 | import jakarta.servlet.http.HttpServletResponse;
   32 | import org.jspecify.annotations.Nullable;
   33 | 
   34 | import org.springframework.core.GenericTypeResolver;
   35 | import org.springframework.core.MethodParameter;
   36 | import org.springframework.core.ParameterizedTypeReference;
   37 | import org.springframework.core.ResolvableType;
   38 | import org.springframework.core.io.InputStreamResource;
   39 | import org.springframework.core.io.Resource;
   40 | import org.springframework.core.io.support.ResourceRegion;
   41 | import org.springframework.core.log.LogFormatUtils;
   42 | import org.springframework.http.HttpEntity;
   43 | import org.springframework.http.HttpHeaders;
   44 | import org.springframework.http.HttpOutputMessage;
   45 | import org.springframework.http.HttpRange;
   46 | import org.springframework.http.HttpRequest;
   47 | import org.springframework.http.HttpStatus;
   48 | import org.springframework.http.HttpStatusCode;
   49 | import org.springframework.http.MediaType;
   50 | import org.springframework.http.MediaTypeFactory;
   51 | import org.springframework.http.ProblemDetail;
   52 | import org.springframework.http.converter.GenericHttpMessageConverter;
   53 | import org.springframework.http.converter.HttpMessageConverter;
   54 | import org.springframework.http.converter.HttpMessageNotWritableException;
   55 | import org.springframework.http.converter.SmartHttpMessageConverter;
   56 | import org.springframework.http.server.ServletServerHttpRequest;
   57 | import org.springframework.http.server.ServletServerHttpResponse;
   58 | import org.springframework.util.Assert;
   59 | import org.springframework.util.CollectionUtils;
   60 | import org.springframework.util.MimeTypeUtils;
   61 | import org.springframework.util.StringUtils;
   62 | import org.springframework.web.ErrorResponse;
   63 | import org.springframework.web.HttpMediaTypeNotAcceptableException;
   64 | import org.springframework.web.accept.ContentNegotiationManager;
   65 | import org.springframework.web.context.request.NativeWebRequest;
   66 | import org.springframework.web.context.request.ServletWebRequest;
   67 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   68 | import org.springframework.web.method.support.ModelAndViewContainer;
   69 | import org.springframework.web.servlet.ErrorResponseViewResolver;
   70 | import org.springframework.web.servlet.HandlerMapping;
   71 | import org.springframework.web.servlet.ModelAndView;
   72 | import org.springframework.web.util.UrlPathHelper;
   73 | 
   74 | /**
   75 |  * Extends {@link AbstractMessageConverterMethodArgumentResolver} with the ability to handle method
   76 |  * return values by writing to the response with {@link HttpMessageConverter HttpMessageConverters}.
   77 |  *
   78 |  * @author Arjen Poutsma
   79 |  * @author Rossen Stoyanchev
   80 |  * @author Brian Clozel
   81 |  * @author Juergen Hoeller
   82 |  * @author Sebastien Deleuze
   83 |  * @since 3.1
   84 |  */
   85 | public abstract class AbstractMessageConverterMethodProcessor extends AbstractMessageConverterMethodArgumentResolver
   86 | 		implements HandlerMethodReturnValueHandler {
   87 | 
   88 | 	/* Extensions associated with the built-in message converters */
   89 | 	private static final Set<String> SAFE_EXTENSIONS = Set.of(
   90 | 			"txt", "text", "yml", "properties", "csv",
   91 | 			"json", "xml", "atom", "rss",
   92 | 			"png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp");
   93 | 
   94 | 	private static final Set<String> SAFE_MEDIA_BASE_TYPES =
   95 | 			Set.of("audio", "image", "video");
   96 | 
   97 | 	private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES =
   98 | 			List.of(MediaType.ALL, new MediaType("application"));
   99 | 
  100 | 	private static final List<MediaType> PROBLEM_MEDIA_TYPES =
  101 | 			List.of(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_PROBLEM_XML);
  102 | 
  103 | 	private static final Type RESOURCE_REGION_LIST_TYPE =
  104 | 			new ParameterizedTypeReference<List<ResourceRegion>>() {}.getType();
  105 | 
  106 | 
  107 | 	private final ContentNegotiationManager contentNegotiationManager;
  108 | 
  109 | 	private final List<ErrorResponse.Interceptor> errorResponseInterceptors = new ArrayList<>();
  110 | 
  111 | 	private final Set<String> safeExtensions = new HashSet<>();
  112 | 
  113 | 	private @Nullable ErrorResponseViewResolver errorResponseViewResolver;
  114 | 
  115 | 
  116 | 	/**
  117 | 	 * Construct with the provided list of converters only.
  118 | 	 */
  119 | 	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
  120 | 		this(converters, null, null);
  121 | 	}
  122 | 
  123 | 	/**
  124 | 	 * Construct with the provided list of converters and {@link ContentNegotiationManager}.
  125 | 	 */
  126 | 	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
  127 | 			@Nullable ContentNegotiationManager contentNegotiationManager) {
  128 | 
  129 | 		this(converters, contentNegotiationManager, null);
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Variant of {@link #AbstractMessageConverterMethodProcessor(List, ContentNegotiationManager)}
  134 | 	 * with an additional {@code requestResponseBodyAdvice} list for return value handling.
  135 | 	 */
  136 | 	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
  137 | 			@Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {
  138 | 
  139 | 		this(converters, manager, requestResponseBodyAdvice, Collections.emptyList());
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Variant of {@link #AbstractMessageConverterMethodProcessor(List, ContentNegotiationManager, List)}
  144 | 	 * with additional list of {@link ErrorResponse.Interceptor}s for return value handling.
  145 | 	 * @since 6.2
  146 | 	 */
  147 | 	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
  148 | 			@Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice,
  149 | 			List<ErrorResponse.Interceptor> interceptors) {
  150 | 
  151 | 		super(converters, requestResponseBodyAdvice);
  152 | 
  153 | 		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
  154 | 		this.safeExtensions.addAll(this.contentNegotiationManager.getAllFileExtensions());
  155 | 		this.safeExtensions.addAll(SAFE_EXTENSIONS);
  156 | 		this.errorResponseInterceptors.addAll(interceptors);
  157 | 	}
  158 | 
  159 | 
  160 | 	/**
  161 | 	 * Configure an {@link ErrorResponseViewResolver} to render problem details
  162 | 	 * as views when the client prefers HTML.
  163 | 	 * @param errorResponseViewResolver the resolver to use, or {@code null} to
  164 | 	 * always write problem details with message converters
  165 | 	 * @since 7.1
  166 | 	 */
  167 | 	public void setErrorResponseViewResolver(@Nullable ErrorResponseViewResolver errorResponseViewResolver) {
  168 | 		this.errorResponseViewResolver = errorResponseViewResolver;
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Return the {@link #setErrorResponseViewResolver configured}
  173 | 	 * {@link ErrorResponseViewResolver}, if any.
  174 | 	 * @since 7.1
  175 | 	 */
  176 | 	public @Nullable ErrorResponseViewResolver getErrorResponseViewResolver() {
  177 | 		return this.errorResponseViewResolver;
  178 | 	}
  179 | 
  180 | 
  181 | 	/**
  182 | 	 * Create a new {@link HttpOutputMessage} from the given {@link NativeWebRequest}.
  183 | 	 * @param webRequest the web request to create an output message from
  184 | 	 * @return the output message
  185 | 	 */
  186 | 	protected ServletServerHttpResponse createOutputMessage(NativeWebRequest webRequest) {
  187 | 		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
  188 | 		Assert.state(response != null, "No HttpServletResponse");
  189 | 		return new ServletServerHttpResponse(response);
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Invoke the configured {@link ErrorResponse.Interceptor}'s.
  194 | 	 * @since 6.2
  195 | 	 * @deprecated as of 7.1, in favor of
  196 | 	 * {@link #invokeErrorResponseInterceptors(ProblemDetail, ErrorResponse, HttpRequest)}
  197 | 	 */
  198 | 	@Deprecated(since = "7.1", forRemoval = true)
  199 | 	protected void invokeErrorResponseInterceptors(ProblemDetail detail, @Nullable ErrorResponse errorResponse) {
  200 | 		try {
  201 | 			for (ErrorResponse.Interceptor handler : this.errorResponseInterceptors) {
  202 | 				handler.handleError(detail, errorResponse);
  203 | 			}
  204 | 		}
  205 | 		catch (Throwable ex) {
  206 | 			// ignore
  207 | 		}
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * Invoke the configured {@link ErrorResponse.Interceptor}'s with the
  212 | 	 * current request.
  213 | 	 * @param detail the {@code ProblemDetail} to be rendered
  214 | 	 * @param errorResponse the {@code ErrorResponse}, or {@code null} if there isn't one
  215 | 	 * @param request the current request
  216 | 	 * @since 7.1
  217 | 	 * @see ErrorResponse.Interceptor#handleError(ProblemDetail, ErrorResponse, HttpRequest)
  218 | 	 */
  219 | 	protected void invokeErrorResponseInterceptors(
  220 | 			ProblemDetail detail, @Nullable ErrorResponse errorResponse, HttpRequest request) {
  221 | 
  222 | 		try {
  223 | 			for (ErrorResponse.Interceptor handler : this.errorResponseInterceptors) {
  224 | 				handler.handleError(detail, errorResponse, request);
  225 | 			}
  226 | 		}
  227 | 		catch (Throwable ex) {
  228 | 			// ignore
  229 | 		}
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Render the given problem detail as a view, if an
  234 | 	 * {@link #setErrorResponseViewResolver ErrorResponseViewResolver} is
  235 | 	 * configured, the client prefers HTML, and the resolver returns a view.
  236 | 	 * <p>In that case, the view, model and status are applied to the
  237 | 	 * {@code ModelAndViewContainer} and the response status is set.
  238 | 	 * The problem detail must not be written to the response.
  239 | 	 * @param problemDetail the problem detail to render
  240 | 	 * @param errorResponse the error response the problem detail is from, if any
  241 | 	 * @param status the status to use if the view does not declare one
  242 | 	 * @param mavContainer the container for the current request
  243 | 	 * @param webRequest the current request
  244 | 	 * @return {@code true} if the problem detail is rendered as a view,
  245 | 	 * {@code false} if it should be written with message converters
  246 | 	 * @since 7.1
  247 | 	 */
  248 | 	protected boolean resolveErrorView(ProblemDetail problemDetail, @Nullable ErrorResponse errorResponse,
  249 | 			HttpStatusCode status, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
  250 | 
  251 | 		ErrorResponseViewResolver viewResolver = this.errorResponseViewResolver;
  252 | 		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
  253 | 		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
  254 | 		if (viewResolver == null || request == null || response == null || !isHtmlPreferred(request)) {
  255 | 			return false;
  256 | 		}
  257 | 		ModelAndView mav = viewResolver.resolveErrorView(request, problemDetail, errorResponse);
  258 | 		if (mav == null) {
  259 | 			return false;
  260 | 		}
  261 | 		HttpStatusCode viewStatus = (mav.getStatus() != null ? mav.getStatus() : status);
  262 | 		mavContainer.setRequestHandled(false);
  263 | 		if (mav.isReference()) {
  264 | 			mavContainer.setViewName(mav.getViewName());
  265 | 		}
  266 | 		else {
  267 | 			mavContainer.setView(mav.getView());
  268 | 		}
  269 | 		mavContainer.addAllAttributes(mav.getModel());
  270 | 		mavContainer.setStatus(viewStatus);
  271 | 		response.setStatus(viewStatus.value());
  272 | 		return true;
  273 | 	}
  274 | 
  275 | 	@SuppressWarnings("unchecked")
  276 | 	private boolean isHtmlPreferred(HttpServletRequest request) {
  277 | 		Set<MediaType> producibleTypes =
  278 | 				(Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
  279 | 		if (!CollectionUtils.isEmpty(producibleTypes) &&
  280 | 				producibleTypes.stream().noneMatch(MediaType.TEXT_HTML::isCompatibleWith)) {
  281 | 			return false;
  282 | 		}
  283 | 		List<MediaType> acceptableTypes;
  284 | 		try {
  285 | 			acceptableTypes = getAcceptableMediaTypes(request);
  286 | 		}
  287 | 		catch (HttpMediaTypeNotAcceptableException ex) {
  288 | 			return false;
  289 | 		}
  290 | 		// Acceptable media types are sorted by quality and specificity
  291 | 		for (MediaType mediaType : acceptableTypes) {
  292 | 			if (mediaType.isConcrete()) {
  293 | 				return MediaType.TEXT_HTML.isCompatibleWith(mediaType);
  294 | 			}
  295 | 		}
  296 | 		return false;
  297 | 	}
  298 | 
  299 | 	/**
  300 | 	 * Writes the given return value to the given web request. Delegates to
  301 | 	 * {@link #writeWithMessageConverters(Object, MethodParameter, ServletServerHttpRequest, ServletServerHttpResponse)}
  302 | 	 */
  303 | 	protected <T> void writeWithMessageConverters(T value, MethodParameter returnType, NativeWebRequest webRequest)
  304 | 			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
  305 | 
  306 | 		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
  307 | 		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
  308 | 		writeWithMessageConverters(value, returnType, inputMessage, outputMessage);
  309 | 	}
  310 | 
  311 | 	/**
  312 | 	 * Write the given return type to the given output message.
  313 | 	 * @param value the value to write to the output message
  314 | 	 * @param returnType the type of the value
  315 | 	 * @param inputMessage the input messages, used to inspect the {@code Accept} header
  316 | 	 * @param outputMessage the output message to write to
  317 | 	 * @throws IOException thrown in case of I/O errors
  318 | 	 * @throws HttpMediaTypeNotAcceptableException thrown when the conditions indicated
  319 | 	 * by the {@code Accept} header on the request cannot be met by the message converters
  320 | 	 * @throws HttpMessageNotWritableException thrown if a given message cannot
  321 | 	 * be written by a converter, or if the content-type chosen by the server
  322 | 	 * has no compatible converter.
  323 | 	 */
  324 | 	@SuppressWarnings({"rawtypes", "unchecked", "NullAway"})
  325 | 	protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType,
  326 | 			ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
  327 | 			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
  328 | 
  329 | 		Object body;
  330 | 		Class<?> valueType;
  331 | 		Type targetType;
  332 | 
  333 | 		if (value instanceof CharSequence) {
  334 | 			body = value.toString();
  335 | 			valueType = String.class;
  336 | 			targetType = String.class;
  337 | 		}
  338 | 		else {
  339 | 			body = value;
  340 | 			valueType = getReturnValueType(body, returnType);
  341 | 			targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
  342 | 		}
  343 | 
  344 | 		if (isResourceType(value, returnType)) {
  345 | 			outputMessage.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
  346 | 			if (value != null && inputMessage.getHeaders().getFirst(HttpHeaders.RANGE) != null &&
  347 | 					outputMessage.getServletResponse().getStatus() == 200) {
  348 | 				Resource resource = (Resource) value;
  349 | 				try {
  350 | 					List<HttpRange> httpRanges =
  351 | 							HttpRange.resolveRanges(inputMessage.getHeaders(), outputMessage.getHeaders());
  352 | 					if (!httpRanges.isEmpty()) {
  353 | 						outputMessage.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());
  354 | 						body = HttpRange.toResourceRegions(httpRanges, resource);
  355 | 						valueType = body.getClass();
  356 | 						targetType = RESOURCE_REGION_LIST_TYPE;
  357 | 					}
  358 | 				}
  359 | 				catch (IllegalArgumentException ex) {
  360 | 					outputMessage.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
  361 | 					outputMessage.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
  362 | 				}
  363 | 			}
  364 | 		}
  365 | 
  366 | 		MediaType selectedMediaType = null;
  367 | 		MediaType contentType = outputMessage.getHeaders().getContentType();
  368 | 		boolean isContentTypePreset = contentType != null && contentType.isConcrete();
  369 | 		if (isContentTypePreset) {
  370 | 			if (logger.isDebugEnabled()) {
  371 | 				logger.debug("Found 'Content-Type:" + contentType + "' in response");
  372 | 			}
  373 | 			selectedMediaType = contentType;
  374 | 		}
  375 | 		else {
  376 | 			HttpServletRequest request = inputMessage.getServletRequest();
  377 | 			List<MediaType> acceptableTypes;
  378 | 			try {
  379 | 				acceptableTypes = getAcceptableMediaTypes(request);
  380 | 			}
  381 | 			catch (HttpMediaTypeNotAcceptableException ex) {
  382 | 				int series = outputMessage.getServletResponse().getStatus() / 100;
  383 | 				if (body == null || series == 4 || series == 5) {
  384 | 					if (logger.isDebugEnabled()) {
  385 | 						logger.debug("Ignoring error response content (if any). " + ex);
  386 | 					}
  387 | 					return;
  388 | 				}
  389 | 				throw ex;
  390 | 			}
  391 | 
  392 | 			List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);
  393 | 			if (body != null && producibleTypes.isEmpty()) {
  394 | 				throw new HttpMessageNotWritableException(
  395 | 						"No converter found for return value of type: " + valueType);
  396 | 			}
  397 | 
  398 | 			List<MediaType> compatibleMediaTypes = determineCompatibleMediaTypes(acceptableTypes, producibleTypes);
  399 | 
  400 | 			// For ProblemDetail, fall back on RFC 9457 format
  401 | 			if (compatibleMediaTypes.isEmpty() && ProblemDetail.class.isAssignableFrom(valueType)) {
  402 | 				compatibleMediaTypes = determineCompatibleMediaTypes(PROBLEM_MEDIA_TYPES, producibleTypes);
  403 | 			}
  404 | 
  405 | 			if (compatibleMediaTypes.isEmpty()) {
  406 | 				if (logger.isDebugEnabled()) {
  407 | 					logger.debug("No match for " + acceptableTypes + ", supported: " + producibleTypes);
  408 | 				}
  409 | 				if (body != null) {
  410 | 					throw new HttpMediaTypeNotAcceptableException(producibleTypes);
  411 | 				}
  412 | 				return;
  413 | 			}
  414 | 
  415 | 			MimeTypeUtils.sortBySpecificity(compatibleMediaTypes);
  416 | 
  417 | 			for (MediaType mediaType : compatibleMediaTypes) {
  418 | 				if (mediaType.isConcrete()) {
  419 | 					selectedMediaType = mediaType;
  420 | 					break;
  421 | 				}
  422 | 				else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
  423 | 					selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
  424 | 					break;
  425 | 				}
  426 | 			}
  427 | 
  428 | 			if (logger.isDebugEnabled()) {
  429 | 				logger.debug("Using '" + selectedMediaType + "', given " +
  430 | 						acceptableTypes + " and supported " + producibleTypes);
  431 | 			}
  432 | 		}
  433 | 
  434 | 		if (selectedMediaType != null) {
  435 | 			selectedMediaType = selectedMediaType.removeQualityValue();
  436 | 
  437 | 			ResolvableType targetResolvableType = null;
  438 | 			for (HttpMessageConverter converter : this.messageConverters) {
  439 | 				ConverterType converterTypeToUse = null;
  440 | 				if (converter instanceof GenericHttpMessageConverter genericConverter) {
  441 | 					if (genericConverter.canWrite(targetType, valueType, selectedMediaType)) {
  442 | 						converterTypeToUse = ConverterType.GENERIC;
  443 | 					}
  444 | 				}
  445 | 				else if (converter instanceof SmartHttpMessageConverter smartConverter) {
  446 | 					targetResolvableType = getNestedTypeIfNeeded(ResolvableType.forType(targetType));
  447 | 					if (smartConverter.canWrite(targetResolvableType, valueType, selectedMediaType)) {
  448 | 						converterTypeToUse = ConverterType.SMART;
  449 | 					}
  450 | 				}
  451 | 				else if (converter.canWrite(valueType, selectedMediaType)){
  452 | 					converterTypeToUse = ConverterType.BASE;
  453 | 				}
  454 | 				if (converterTypeToUse != null) {
  455 | 					body = getAdvice().beforeBodyWrite(body, returnType, selectedMediaType,
  456 | 							(Class<? extends HttpMessageConverter<?>>) converter.getClass(), inputMessage, outputMessage);
  457 | 					if (body != null) {
  458 | 						Object theBody = body;
  459 | 						LogFormatUtils.traceDebug(logger, traceOn ->
  460 | 								"Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");
  461 | 						addContentDispositionHeader(inputMessage, outputMessage);
  462 | 						switch (converterTypeToUse) {
  463 | 							case BASE -> converter.write(body, selectedMediaType, outputMessage);
  464 | 							case GENERIC -> ((GenericHttpMessageConverter) converter).write(body, targetType, selectedMediaType, outputMessage);
  465 | 							case SMART -> ((SmartHttpMessageConverter) converter).write(body, targetResolvableType,
  466 | 									selectedMediaType, outputMessage, getAdvice().determineWriteHints(body, returnType,
  467 | 											selectedMediaType, (Class<? extends HttpMessageConverter<?>>) converter.getClass()));
  468 | 						}
  469 | 					}
  470 | 					else {
  471 | 						if (logger.isDebugEnabled()) {
  472 | 							logger.debug("Nothing to write: null body");
  473 | 						}
  474 | 					}
  475 | 					return;
  476 | 				}
  477 | 			}
  478 | 		}
  479 | 
  480 | 		if (body != null) {
  481 | 			Set<MediaType> producibleMediaTypes =
  482 | 					(Set<MediaType>) inputMessage.getServletRequest()
  483 | 							.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
  484 | 
  485 | 			if (isContentTypePreset || !CollectionUtils.isEmpty(producibleMediaTypes)) {
  486 | 				throw new HttpMessageNotWritableException(
  487 | 						"No converter for [" + valueType + "] with preset Content-Type '" + contentType + "'");
  488 | 			}
  489 | 			throw new HttpMediaTypeNotAcceptableException(getSupportedMediaTypes(body.getClass()));
  490 | 		}
  491 | 	}
  492 | 
  493 | 	/**
  494 | 	 * Return the type of the value to be written to the response. Typically this is
  495 | 	 * a simple check via getClass on the value but if the value is null, then the
  496 | 	 * return type needs to be examined possibly including generic type determination
  497 | 	 * (for example, {@code ResponseEntity<T>}).
  498 | 	 */
  499 | 	protected Class<?> getReturnValueType(@Nullable Object value, MethodParameter returnType) {
  500 | 		return (value != null ? value.getClass() : returnType.getParameterType());
  501 | 	}
  502 | 
  503 | 	/**
  504 | 	 * Return whether the returned value or the declared return type extends {@link Resource}.
  505 | 	 */
  506 | 	protected boolean isResourceType(@Nullable Object value, MethodParameter returnType) {
  507 | 		Class<?> clazz = getReturnValueType(value, returnType);
  508 | 		return clazz != InputStreamResource.class && Resource.class.isAssignableFrom(clazz);
  509 | 	}
  510 | 
  511 | 	/**
  512 | 	 * Return the generic type of the {@code returnType} (or of the nested type
  513 | 	 * if it is an {@link HttpEntity}).
  514 | 	 */
  515 | 	private Type getGenericType(MethodParameter returnType) {
  516 | 		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
  517 | 			return ResolvableType.forType(returnType.getGenericParameterType()).getGeneric().getType();
  518 | 		}
  519 | 		else {
  520 | 			return returnType.getGenericParameterType();
  521 | 		}
  522 | 	}
  523 | 
  524 | 	/**
  525 | 	 * Returns the media types that can be produced.
  526 | 	 * @see #getProducibleMediaTypes(HttpServletRequest, Class, Type)
  527 | 	 */
  528 | 	@SuppressWarnings("unused")
  529 | 	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> valueClass) {
  530 | 		return getProducibleMediaTypes(request, valueClass, null);
  531 | 	}
  532 | 
  533 | 	/**
  534 | 	 * Returns the media types that can be produced. The resulting media types are:
  535 | 	 * <ul>
  536 | 	 * <li>The producible media types specified in the request mappings, or
  537 | 	 * <li>Media types of configured converters that can write the specific return value, or
  538 | 	 * <li>{@link MediaType#ALL}
  539 | 	 * </ul>
  540 | 	 * @since 4.2
  541 | 	 */
  542 | 	@SuppressWarnings("unchecked")
  543 | 	protected List<MediaType> getProducibleMediaTypes(
  544 | 			HttpServletRequest request, Class<?> valueClass, @Nullable Type targetType) {
  545 | 
  546 | 		Set<MediaType> mediaTypes =
  547 | 				(Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
  548 | 		if (!CollectionUtils.isEmpty(mediaTypes)) {
  549 | 			return new ArrayList<>(mediaTypes);
  550 | 		}
  551 | 		Set<MediaType> result = new LinkedHashSet<>();
  552 | 		for (HttpMessageConverter<?> converter : this.messageConverters) {
  553 | 			if (converter instanceof GenericHttpMessageConverter<?> genericConverter && targetType != null) {
  554 | 				if (genericConverter.canWrite(targetType, valueClass, null)) {
  555 | 					result.addAll(converter.getSupportedMediaTypes(valueClass));
  556 | 				}
  557 | 			}
  558 | 			else if (converter instanceof SmartHttpMessageConverter<?> smartConverter && targetType != null) {
  559 | 				ResolvableType resolvableType = ResolvableType.forType(targetType);
  560 | 				if (smartConverter.canWrite(resolvableType, valueClass, null)) {
  561 | 					result.addAll(converter.getSupportedMediaTypes(valueClass));
  562 | 				}
  563 | 			}
  564 | 			else if (converter.canWrite(valueClass, null)) {
  565 | 				result.addAll(converter.getSupportedMediaTypes(valueClass));
  566 | 			}
  567 | 		}
  568 | 		return (result.isEmpty() ? Collections.singletonList(MediaType.ALL) : new ArrayList<>(result));
  569 | 	}
  570 | 
  571 | 	private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request)
  572 | 			throws HttpMediaTypeNotAcceptableException {
  573 | 
  574 | 		return this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
  575 | 	}
  576 | 
  577 | 	private List<MediaType> determineCompatibleMediaTypes(
  578 | 			List<MediaType> acceptableTypes, List<MediaType> producibleTypes) {
  579 | 
  580 | 		Set<MediaType> compatibleTypes = new LinkedHashSet<>();
  581 | 		for (MediaType requestedType : acceptableTypes) {
  582 | 			for (MediaType producibleType : producibleTypes) {
  583 | 				if (requestedType.isCompatibleWith(producibleType)) {
  584 | 					compatibleTypes.add(getMostSpecificMediaType(requestedType, producibleType));
  585 | 				}
  586 | 			}
  587 | 		}
  588 | 		return new ArrayList<>(compatibleTypes);
  589 | 	}
  590 | 
  591 | 	/**
  592 | 	 * Return the more specific of the acceptable and the producible media types
  593 | 	 * with the q-value of the former.
  594 | 	 */
  595 | 	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
  596 | 		MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
  597 | 		if (acceptType.isLessSpecific(produceTypeToUse)) {
  598 | 			return produceTypeToUse;
  599 | 		}
  600 | 		else {
  601 | 			return acceptType;
  602 | 		}
  603 | 	}
  604 | 
  605 | 	/**
  606 | 	 * Check if the path has a file extension and whether the extension is either
  607 | 	 * on the list of {@link #SAFE_EXTENSIONS safe extensions} or explicitly
  608 | 	 * {@link ContentNegotiationManager#getAllFileExtensions() registered}.
  609 | 	 * If not, and the status is in the 2xx range, a 'Content-Disposition'
  610 | 	 * header with a safe attachment file name ("f.txt") is added to prevent
  611 | 	 * RFD exploits.
  612 | 	 */
  613 | 	private void addContentDispositionHeader(ServletServerHttpRequest request, ServletServerHttpResponse response) {
  614 | 		HttpHeaders headers = response.getHeaders();
  615 | 		if (headers.containsHeader(HttpHeaders.CONTENT_DISPOSITION)) {
  616 | 			return;
  617 | 		}
  618 | 
  619 | 		try {
  620 | 			int status = response.getServletResponse().getStatus();
  621 | 			if (status < 200 || (status > 299 && status < 400)) {
  622 | 				return;
  623 | 			}
  624 | 		}
  625 | 		catch (Throwable ex) {
  626 | 			// ignore
  627 | 		}
  628 | 
  629 | 		HttpServletRequest servletRequest = request.getServletRequest();
  630 | 		String requestUri = UrlPathHelper.rawPathInstance.getOriginatingRequestUri(servletRequest);
  631 | 
  632 | 		int index = requestUri.lastIndexOf('/') + 1;
  633 | 		String filename = requestUri.substring(index);
  634 | 		String pathParams = "";
  635 | 
  636 | 		index = filename.indexOf(';');
  637 | 		if (index != -1) {
  638 | 			pathParams = filename.substring(index);
  639 | 			filename = filename.substring(0, index);
  640 | 		}
  641 | 
  642 | 		filename = UrlPathHelper.defaultInstance.decodeRequestString(servletRequest, filename);
  643 | 		String ext = StringUtils.getFilenameExtension(filename);
  644 | 
  645 | 		pathParams = UrlPathHelper.defaultInstance.decodeRequestString(servletRequest, pathParams);
  646 | 		String extInPathParams = StringUtils.getFilenameExtension(pathParams);
  647 | 
  648 | 		if (!safeExtension(servletRequest, ext) || !safeExtension(servletRequest, extInPathParams)) {
  649 | 			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
  650 | 		}
  651 | 	}
  652 | 
  653 | 	@SuppressWarnings("unchecked")
  654 | 	private boolean safeExtension(HttpServletRequest request, @Nullable String extension) {
  655 | 		if (!StringUtils.hasText(extension)) {
  656 | 			return true;
  657 | 		}
  658 | 		extension = extension.toLowerCase(Locale.ROOT);
  659 | 		if (this.safeExtensions.contains(extension)) {
  660 | 			return true;
  661 | 		}
  662 | 		String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
  663 | 		if (pattern != null && pattern.endsWith("." + extension)) {
  664 | 			return true;
  665 | 		}
  666 | 		if (extension.equals("html")) {
  667 | 			String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
  668 | 			Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(name);
  669 | 			if (!CollectionUtils.isEmpty(mediaTypes) && mediaTypes.contains(MediaType.TEXT_HTML)) {
  670 | 				return true;
  671 | 			}
  672 | 		}
  673 | 		MediaType mediaType = resolveMediaType(request, extension);
  674 | 		return (mediaType != null && (safeMediaType(mediaType)));
  675 | 	}
  676 | 
  677 | 	private @Nullable MediaType resolveMediaType(ServletRequest request, String extension) {
  678 | 		MediaType result = null;
  679 | 		String rawMimeType = request.getServletContext().getMimeType("file." + extension);
  680 | 		if (StringUtils.hasText(rawMimeType)) {
  681 | 			result = MediaType.parseMediaType(rawMimeType);
  682 | 		}
  683 | 		if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
  684 | 			result = MediaTypeFactory.getMediaType("file." + extension).orElse(null);
  685 | 		}
  686 | 		return result;
  687 | 	}
  688 | 
  689 | 	private boolean safeMediaType(MediaType mediaType) {
  690 | 		return (SAFE_MEDIA_BASE_TYPES.contains(mediaType.getType()) ||
  691 | 				mediaType.getSubtype().endsWith("+xml"));
  692 | 	}
  693 | 
  694 | }
```
