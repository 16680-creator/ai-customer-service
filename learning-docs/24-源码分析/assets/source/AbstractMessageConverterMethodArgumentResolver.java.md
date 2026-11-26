<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:AbstractMessageConverterMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: AbstractMessageConverterMethodArgumentResolver.java#L起-L止) -->

# AbstractMessageConverterMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodArgumentResolver.java` |
| 行数 | 431 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.252Z |

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
   20 | import java.io.InputStream;
   21 | import java.io.PushbackInputStream;
   22 | import java.lang.annotation.Annotation;
   23 | import java.lang.reflect.Type;
   24 | import java.util.ArrayList;
   25 | import java.util.Collection;
   26 | import java.util.Collections;
   27 | import java.util.LinkedHashSet;
   28 | import java.util.List;
   29 | import java.util.Optional;
   30 | import java.util.Set;
   31 | 
   32 | import jakarta.servlet.http.HttpServletRequest;
   33 | import org.apache.commons.logging.Log;
   34 | import org.apache.commons.logging.LogFactory;
   35 | import org.jspecify.annotations.Nullable;
   36 | 
   37 | import org.springframework.core.MethodParameter;
   38 | import org.springframework.core.ResolvableType;
   39 | import org.springframework.core.log.LogFormatUtils;
   40 | import org.springframework.http.HttpEntity;
   41 | import org.springframework.http.HttpHeaders;
   42 | import org.springframework.http.HttpInputMessage;
   43 | import org.springframework.http.HttpMethod;
   44 | import org.springframework.http.HttpOutputMessage;
   45 | import org.springframework.http.HttpRequest;
   46 | import org.springframework.http.InvalidMediaTypeException;
   47 | import org.springframework.http.MediaType;
   48 | import org.springframework.http.converter.GenericHttpMessageConverter;
   49 | import org.springframework.http.converter.HttpMessageConverter;
   50 | import org.springframework.http.converter.HttpMessageNotReadableException;
   51 | import org.springframework.http.converter.SmartHttpMessageConverter;
   52 | import org.springframework.http.server.ServletServerHttpRequest;
   53 | import org.springframework.util.Assert;
   54 | import org.springframework.util.MimeTypeUtils;
   55 | import org.springframework.validation.Errors;
   56 | import org.springframework.validation.annotation.ValidationAnnotationUtils;
   57 | import org.springframework.web.HttpMediaTypeNotSupportedException;
   58 | import org.springframework.web.bind.WebDataBinder;
   59 | import org.springframework.web.context.request.NativeWebRequest;
   60 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   61 | 
   62 | /**
   63 |  * A base class for resolving method argument values by reading from the body of
   64 |  * a request with {@link HttpMessageConverter HttpMessageConverters}.
   65 |  *
   66 |  * @author Arjen Poutsma
   67 |  * @author Rossen Stoyanchev
   68 |  * @author Juergen Hoeller
   69 |  * @author Sebastien Deleuze
   70 |  * @since 3.1
   71 |  */
   72 | public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {
   73 | 
   74 | 	protected enum ConverterType { BASE, GENERIC, SMART }
   75 | 
   76 | 
   77 | 	private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.QUERY);
   78 | 
   79 | 	private static final Object NO_VALUE = new Object();
   80 | 
   81 | 
   82 | 	protected final Log logger = LogFactory.getLog(getClass());
   83 | 
   84 | 	protected final List<HttpMessageConverter<?>> messageConverters;
   85 | 
   86 | 	private final RequestResponseBodyAdviceChain advice;
   87 | 
   88 | 
   89 | 	/**
   90 | 	 * Basic constructor with converters only.
   91 | 	 */
   92 | 	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
   93 | 		this(converters, null);
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
   98 | 	 * @since 4.2
   99 | 	 */
  100 | 	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters,
  101 | 			@Nullable List<Object> requestResponseBodyAdvice) {
  102 | 
  103 | 		Assert.notEmpty(converters, "'messageConverters' must not be empty");
  104 | 		this.messageConverters = converters;
  105 | 		this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Return the configured {@link RequestBodyAdvice} and
  110 | 	 * {@link RequestBodyAdvice} where each instance may be wrapped as a
  111 | 	 * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}.
  112 | 	 */
  113 | 	RequestResponseBodyAdviceChain getAdvice() {
  114 | 		return this.advice;
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Create the method argument value of the expected parameter type by
  119 | 	 * reading from the given request.
  120 | 	 * @param webRequest the current request
  121 | 	 * @param parameter the method parameter descriptor (may be {@code null})
  122 | 	 * @param paramType the type of the argument value to be created
  123 | 	 * @return the created method argument value
  124 | 	 * @throws IOException if the reading from the request fails
  125 | 	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
  126 | 	 */
  127 | 	protected @Nullable Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
  128 | 			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
  129 | 
  130 | 		HttpInputMessage inputMessage = createInputMessage(webRequest);
  131 | 		return readWithMessageConverters(inputMessage, parameter, paramType);
  132 | 	}
  133 | 
  134 | 	/**
  135 | 	 * Create the method argument value of the expected parameter type by reading
  136 | 	 * from the given HttpInputMessage.
  137 | 	 * @param <T> the expected type of the argument value to be created
  138 | 	 * @param inputMessage the HTTP input message representing the current request
  139 | 	 * @param parameter the method parameter descriptor
  140 | 	 * @param targetType the target type, not necessarily the same as the method
  141 | 	 * parameter type, for example, for {@code HttpEntity<String>}.
  142 | 	 * @return the created method argument value
  143 | 	 * @throws IOException if the reading from the request fails
  144 | 	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
  145 | 	 */
  146 | 	@SuppressWarnings({"rawtypes", "unchecked", "NullAway"})
  147 | 	protected <T> @Nullable Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,
  148 | 			Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
  149 | 
  150 | 		Class<?> contextClass = parameter.getContainingClass();
  151 | 		Class<T> targetClass = (targetType instanceof Class clazz ? clazz : null);
  152 | 		ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
  153 | 		if (targetClass == null) {
  154 | 			targetClass = (Class<T>) resolvableType.resolve();
  155 | 		}
  156 | 
  157 | 		MediaType contentType;
  158 | 		boolean noContentType = false;
  159 | 		try {
  160 | 			contentType = inputMessage.getHeaders().getContentType();
  161 | 		}
  162 | 		catch (InvalidMediaTypeException ex) {
  163 | 			throw new HttpMediaTypeNotSupportedException(
  164 | 					ex.getMessage(), getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
  165 | 		}
  166 | 		if (contentType == null) {
  167 | 			noContentType = true;
  168 | 			contentType = MediaType.APPLICATION_OCTET_STREAM;
  169 | 		}
  170 | 
  171 | 		HttpMethod httpMethod = (inputMessage instanceof HttpRequest httpRequest ? httpRequest.getMethod() : null);
  172 | 		Object body = NO_VALUE;
  173 | 
  174 | 		EmptyBodyCheckingHttpInputMessage message = null;
  175 | 		try {
  176 | 			ResolvableType targetResolvableType = null;
  177 | 			message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
  178 | 			for (HttpMessageConverter<?> converter : this.messageConverters) {
  179 | 				Class<? extends HttpMessageConverter<?>> converterClass = (Class<? extends HttpMessageConverter<?>>) converter.getClass();
  180 | 				ConverterType converterTypeToUse = null;
  181 | 				if (converter instanceof GenericHttpMessageConverter<?> genericConverter) {
  182 | 					if (genericConverter.canRead(targetType, contextClass, contentType)) {
  183 | 						converterTypeToUse = ConverterType.GENERIC;
  184 | 					}
  185 | 				}
  186 | 				else if (converter instanceof SmartHttpMessageConverter<?> smartConverter) {
  187 | 					if (targetResolvableType == null) {
  188 | 						targetResolvableType = getNestedTypeIfNeeded(resolvableType);
  189 | 					}
  190 | 					if (smartConverter.canRead(targetResolvableType, contentType)) {
  191 | 						converterTypeToUse = ConverterType.SMART;
  192 | 					}
  193 | 				}
  194 | 				else if (targetClass != null && converter.canRead(targetClass, contentType)) {
  195 | 					converterTypeToUse = ConverterType.BASE;
  196 | 				}
  197 | 				if (converterTypeToUse != null) {
  198 | 					if (message.hasBody()) {
  199 | 						HttpInputMessage msgToUse = this.advice.beforeBodyRead(message, parameter, targetType, converterClass);
  200 | 						body = switch (converterTypeToUse) {
  201 | 							case BASE -> ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse);
  202 | 							case GENERIC -> ((GenericHttpMessageConverter<?>) converter).read(targetType, contextClass, msgToUse);
  203 | 							case SMART -> ((SmartHttpMessageConverter<?>) converter).read(targetResolvableType, msgToUse,
  204 | 									this.advice.determineReadHints(parameter, targetType, (Class<SmartHttpMessageConverter<?>>) converterClass));
  205 | 						};
  206 | 						body = this.advice.afterBodyRead(body, msgToUse, parameter, targetType, converterClass);
  207 | 					}
  208 | 					else {
  209 | 						body = this.advice.handleEmptyBody(null, message, parameter, targetType, converterClass);
  210 | 					}
  211 | 					break;
  212 | 				}
  213 | 
  214 | 			}
  215 | 
  216 | 			if (body == NO_VALUE && noContentType && !message.hasBody()) {
  217 | 				body = this.advice.handleEmptyBody(
  218 | 						null, message, parameter, targetType, NoContentTypeHttpMessageConverter.class);
  219 | 			}
  220 | 		}
  221 | 		catch (IOException ex) {
  222 | 			throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
  223 | 		}
  224 | 		finally {
  225 | 			if (message != null && message.hasBody()) {
  226 | 				closeStreamIfNecessary(message.getBody());
  227 | 			}
  228 | 		}
  229 | 
  230 | 		if (body == NO_VALUE) {
  231 | 			if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) || (noContentType && !message.hasBody())) {
  232 | 				return null;
  233 | 			}
  234 | 			throw new HttpMediaTypeNotSupportedException(contentType,
  235 | 					getSupportedMediaTypes(targetClass != null ? targetClass : Object.class), httpMethod);
  236 | 		}
  237 | 
  238 | 		MediaType selectedContentType = contentType;
  239 | 		Object theBody = body;
  240 | 		LogFormatUtils.traceDebug(logger, traceOn -> {
  241 | 			String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
  242 | 			return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
  243 | 		});
  244 | 
  245 | 		return body;
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Return the generic type of the {@code returnType} (or of the nested type
  250 | 	 * if it is an {@link HttpEntity} or/and an {@link Optional}).
  251 | 	 */
  252 | 	protected ResolvableType getNestedTypeIfNeeded(ResolvableType type) {
  253 | 		ResolvableType genericType = type;
  254 | 		if (Optional.class.isAssignableFrom(genericType.toClass())) {
  255 | 			genericType = genericType.getNested(2);
  256 | 		}
  257 | 		if (HttpEntity.class.isAssignableFrom(genericType.toClass())) {
  258 | 			genericType = genericType.getNested(2);
  259 | 		}
  260 | 		return genericType;
  261 | 	}
  262 | 
  263 | 
  264 | 	/**
  265 | 	 * Create a new {@link HttpInputMessage} from the given {@link NativeWebRequest}.
  266 | 	 * @param webRequest the web request to create an input message from
  267 | 	 * @return the input message
  268 | 	 */
  269 | 	protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
  270 | 		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
  271 | 		Assert.state(servletRequest != null, "No HttpServletRequest");
  272 | 		return new ServletServerHttpRequest(servletRequest);
  273 | 	}
  274 | 
  275 | 	/**
  276 | 	 * Validate the binding target if applicable.
  277 | 	 * <p>The default implementation checks for {@code @jakarta.validation.Valid},
  278 | 	 * Spring's {@link org.springframework.validation.annotation.Validated},
  279 | 	 * and custom annotations whose name starts with "Valid".
  280 | 	 * @param binder the DataBinder to be used
  281 | 	 * @param parameter the method parameter descriptor
  282 | 	 * @since 4.1.5
  283 | 	 * @see #isBindExceptionRequired
  284 | 	 */
  285 | 	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
  286 | 		Annotation[] annotations = parameter.getParameterAnnotations();
  287 | 		for (Annotation ann : annotations) {
  288 | 			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
  289 | 			if (validationHints != null) {
  290 | 				binder.validate(validationHints);
  291 | 				break;
  292 | 			}
  293 | 		}
  294 | 	}
  295 | 
  296 | 	/**
  297 | 	 * Whether to raise a fatal bind exception on validation errors.
  298 | 	 * @param binder the data binder used to perform data binding
  299 | 	 * @param parameter the method parameter descriptor
  300 | 	 * @return {@code true} if the next method argument is not of type {@link Errors}
  301 | 	 * @since 4.1.5
  302 | 	 */
  303 | 	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
  304 | 		int i = parameter.getParameterIndex();
  305 | 		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
  306 | 		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
  307 | 		return !hasBindingResult;
  308 | 	}
  309 | 
  310 | 	/**
  311 | 	 * Return the media types supported by all provided message converters sorted
  312 | 	 * by specificity via {@link MimeTypeUtils#sortBySpecificity(List)}.
  313 | 	 * @since 5.3.4
  314 | 	 */
  315 | 	protected List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
  316 | 		Set<MediaType> mediaTypeSet = new LinkedHashSet<>();
  317 | 		for (HttpMessageConverter<?> converter : this.messageConverters) {
  318 | 			mediaTypeSet.addAll(converter.getSupportedMediaTypes(clazz));
  319 | 		}
  320 | 		List<MediaType> result = new ArrayList<>(mediaTypeSet);
  321 | 		MimeTypeUtils.sortBySpecificity(result);
  322 | 		return result;
  323 | 	}
  324 | 
  325 | 	/**
  326 | 	 * Adapt the given argument against the method parameter, if necessary.
  327 | 	 * @param arg the resolved argument
  328 | 	 * @param parameter the method parameter descriptor
  329 | 	 * @return the adapted argument, or the original resolved argument as-is
  330 | 	 * @since 4.3.5
  331 | 	 */
  332 | 	protected @Nullable Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
  333 | 		if (parameter.getParameterType() == Optional.class) {
  334 | 			if (arg == null || (arg instanceof Collection<?> collection && collection.isEmpty()) ||
  335 | 					(arg instanceof Object[] array && array.length == 0)) {
  336 | 				return Optional.empty();
  337 | 			}
  338 | 			else {
  339 | 				return Optional.of(arg);
  340 | 			}
  341 | 		}
  342 | 		return arg;
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Allow for closing the body stream if necessary,
  347 | 	 * for example, for part streams in a multipart request.
  348 | 	 */
  349 | 	void closeStreamIfNecessary(InputStream body) {
  350 | 		// No-op by default: A standard HttpInputMessage exposes the HTTP request stream
  351 | 		// (ServletRequest#getInputStream), with its lifecycle managed by the container.
  352 | 	}
  353 | 
  354 | 
  355 | 	private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {
  356 | 
  357 | 		private final HttpHeaders headers;
  358 | 
  359 | 		private final @Nullable InputStream body;
  360 | 
  361 | 		public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
  362 | 			this.headers = inputMessage.getHeaders();
  363 | 			InputStream inputStream = inputMessage.getBody();
  364 | 			if (inputStream.markSupported()) {
  365 | 				inputStream.mark(1);
  366 | 				this.body = (inputStream.read() != -1 ? inputStream : null);
  367 | 				inputStream.reset();
  368 | 			}
  369 | 			else {
  370 | 				PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
  371 | 				int b = pushbackInputStream.read();
  372 | 				if (b == -1) {
  373 | 					this.body = null;
  374 | 				}
  375 | 				else {
  376 | 					this.body = pushbackInputStream;
  377 | 					pushbackInputStream.unread(b);
  378 | 				}
  379 | 			}
  380 | 		}
  381 | 
  382 | 		@Override
  383 | 		public HttpHeaders getHeaders() {
  384 | 			return this.headers;
  385 | 		}
  386 | 
  387 | 		@Override
  388 | 		public InputStream getBody() {
  389 | 			return (this.body != null ? this.body : InputStream.nullInputStream());
  390 | 		}
  391 | 
  392 | 		public boolean hasBody() {
  393 | 			return (this.body != null);
  394 | 		}
  395 | 	}
  396 | 
  397 | 
  398 | 	/**
  399 | 	 * Placeholder HttpMessageConverter type to pass to RequestBodyAdvice if there
  400 | 	 * is no content-type and no content. In that case, we may not find a converter,
  401 | 	 * but RequestBodyAdvice have a chance to provide it via handleEmptyBody.
  402 | 	 */
  403 | 	private static class NoContentTypeHttpMessageConverter implements HttpMessageConverter<String> {
  404 | 
  405 | 		@Override
  406 | 		public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
  407 | 			return false;
  408 | 		}
  409 | 
  410 | 		@Override
  411 | 		public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
  412 | 			return false;
  413 | 		}
  414 | 
  415 | 		@Override
  416 | 		public List<MediaType> getSupportedMediaTypes() {
  417 | 			return Collections.emptyList();
  418 | 		}
  419 | 
  420 | 		@Override
  421 | 		public String read(Class<? extends String> clazz, HttpInputMessage inputMessage) {
  422 | 			throw new UnsupportedOperationException();
  423 | 		}
  424 | 
  425 | 		@Override
  426 | 		public void write(String s, @Nullable MediaType contentType, HttpOutputMessage outputMessage) {
  427 | 			throw new UnsupportedOperationException();
  428 | 		}
  429 | 	}
  430 | 
  431 | }
```
