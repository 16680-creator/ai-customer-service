<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/ExceptionHandlerExceptionResolver.java -->
<!-- 正文引用：[[源码:ExceptionHandlerExceptionResolver.java#L起-L止]] ；代码块头部：(src: ExceptionHandlerExceptionResolver.java#L起-L止) -->

# ExceptionHandlerExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/ExceptionHandlerExceptionResolver.java` |
| 行数 | 714 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.599Z |

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
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.Proxy;
   21 | import java.util.ArrayList;
   22 | import java.util.Collections;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.concurrent.ConcurrentHashMap;
   27 | 
   28 | import jakarta.servlet.http.HttpServletRequest;
   29 | import jakarta.servlet.http.HttpServletResponse;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.aop.support.AopUtils;
   33 | import org.springframework.beans.factory.InitializingBean;
   34 | import org.springframework.context.ApplicationContext;
   35 | import org.springframework.context.ApplicationContextAware;
   36 | import org.springframework.http.HttpHeaders;
   37 | import org.springframework.http.HttpStatusCode;
   38 | import org.springframework.http.MediaType;
   39 | import org.springframework.http.converter.ByteArrayHttpMessageConverter;
   40 | import org.springframework.http.converter.FormHttpMessageConverter;
   41 | import org.springframework.http.converter.HttpMessageConverter;
   42 | import org.springframework.http.converter.HttpMessageConverters;
   43 | import org.springframework.http.converter.StringHttpMessageConverter;
   44 | import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
   45 | import org.springframework.ui.ModelMap;
   46 | import org.springframework.web.ErrorResponse;
   47 | import org.springframework.web.HttpMediaTypeNotAcceptableException;
   48 | import org.springframework.web.accept.ContentNegotiationManager;
   49 | import org.springframework.web.bind.annotation.ControllerAdvice;
   50 | import org.springframework.web.context.request.RequestAttributes;
   51 | import org.springframework.web.context.request.ServletWebRequest;
   52 | import org.springframework.web.filter.ServerHttpObservationFilter;
   53 | import org.springframework.web.method.ControllerAdviceBean;
   54 | import org.springframework.web.method.HandlerMethod;
   55 | import org.springframework.web.method.annotation.ExceptionHandlerMappingInfo;
   56 | import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
   57 | import org.springframework.web.method.annotation.MapMethodProcessor;
   58 | import org.springframework.web.method.annotation.ModelMethodProcessor;
   59 | import org.springframework.web.method.annotation.ProblemDetailExceptionHandler;
   60 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   61 | import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
   62 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   63 | import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
   64 | import org.springframework.web.method.support.ModelAndViewContainer;
   65 | import org.springframework.web.servlet.ErrorResponseViewResolver;
   66 | import org.springframework.web.servlet.HandlerMapping;
   67 | import org.springframework.web.servlet.ModelAndView;
   68 | import org.springframework.web.servlet.View;
   69 | import org.springframework.web.servlet.function.HandlerFunction;
   70 | import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
   71 | import org.springframework.web.servlet.mvc.support.RedirectAttributes;
   72 | import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
   73 | import org.springframework.web.servlet.support.RequestContextUtils;
   74 | import org.springframework.web.util.DisconnectedClientHelper;
   75 | 
   76 | /**
   77 |  * An {@link AbstractHandlerMethodExceptionResolver} that resolves exceptions
   78 |  * through {@code @ExceptionHandler} methods.
   79 |  *
   80 |  * <p>Support for custom argument and return value types can be added via
   81 |  * {@link #setCustomArgumentResolvers} and {@link #setCustomReturnValueHandlers}.
   82 |  * Or alternatively to re-configure all argument and return value types use
   83 |  * {@link #setArgumentResolvers} and {@link #setReturnValueHandlers(List)}.
   84 |  *
   85 |  * <p>As of 7.1, exceptions not handled by any {@code @ExceptionHandler} method
   86 |  * can be rendered as RFC 9457 problem details, see
   87 |  * {@link #setRenderUnhandledExceptionsAsProblemDetails(boolean)}.
   88 |  *
   89 |  * @author Rossen Stoyanchev
   90 |  * @author Juergen Hoeller
   91 |  * @author Sebastien Deleuze
   92 |  * @author Brian Clozel
   93 |  * @since 3.1
   94 |  */
   95 | public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver
   96 | 		implements ApplicationContextAware, InitializingBean {
   97 | 
   98 | 	/**
   99 | 	 * Log category to use for network failure after a client has gone away.
  100 | 	 * @see DisconnectedClientHelper
  101 | 	 */
  102 | 	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
  103 | 			"org.springframework.web.servlet.mvc.method.annotation.DisconnectedClient";
  104 | 
  105 | 	private static final DisconnectedClientHelper disconnectedClientHelper =
  106 | 			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);
  107 | 
  108 | 
  109 | 	private @Nullable List<HandlerMethodArgumentResolver> customArgumentResolvers;
  110 | 
  111 | 	private @Nullable HandlerMethodArgumentResolverComposite argumentResolvers;
  112 | 
  113 | 	private @Nullable List<HandlerMethodReturnValueHandler> customReturnValueHandlers;
  114 | 
  115 | 	private @Nullable HandlerMethodReturnValueHandlerComposite returnValueHandlers;
  116 | 
  117 | 	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
  118 | 
  119 | 	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();
  120 | 
  121 | 	private final List<Object> responseBodyAdvice = new ArrayList<>();
  122 | 
  123 | 	private final List<ErrorResponse.Interceptor> errorResponseInterceptors = new ArrayList<>();
  124 | 
  125 | 	private @Nullable ApplicationContext applicationContext;
  126 | 
  127 | 	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
  128 | 			new ConcurrentHashMap<>(64);
  129 | 
  130 | 	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
  131 | 			new LinkedHashMap<>();
  132 | 
  133 | 	private boolean renderUnhandledExceptionsAsProblemDetails;
  134 | 
  135 | 	private @Nullable ErrorResponseViewResolver errorResponseViewResolver;
  136 | 
  137 | 	private @Nullable ProblemDetailExceptionHandler problemDetailExceptionHandler;
  138 | 
  139 | 	private @Nullable Method problemDetailHandlerMethod;
  140 | 
  141 | 
  142 | 	/**
  143 | 	 * Provide resolvers for custom argument types. Custom resolvers are ordered
  144 | 	 * after built-in ones. To override the built-in support for argument
  145 | 	 * resolution use {@link #setArgumentResolvers} instead.
  146 | 	 */
  147 | 	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
  148 | 		this.customArgumentResolvers = argumentResolvers;
  149 | 	}
  150 | 
  151 | 	/**
  152 | 	 * Return the custom argument resolvers, or {@code null}.
  153 | 	 */
  154 | 	public @Nullable List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
  155 | 		return this.customArgumentResolvers;
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Configure the complete list of supported argument types thus overriding
  160 | 	 * the resolvers that would otherwise be configured by default.
  161 | 	 */
  162 | 	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
  163 | 		if (argumentResolvers == null) {
  164 | 			this.argumentResolvers = null;
  165 | 		}
  166 | 		else {
  167 | 			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
  168 | 			this.argumentResolvers.addResolvers(argumentResolvers);
  169 | 		}
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Return the configured argument resolvers, or possibly {@code null} if
  174 | 	 * not initialized yet via {@link #afterPropertiesSet()}.
  175 | 	 */
  176 | 	public @Nullable HandlerMethodArgumentResolverComposite getArgumentResolvers() {
  177 | 		return this.argumentResolvers;
  178 | 	}
  179 | 
  180 | 	/**
  181 | 	 * Provide handlers for custom return value types. Custom handlers are
  182 | 	 * ordered after built-in ones. To override the built-in support for
  183 | 	 * return value handling use {@link #setReturnValueHandlers}.
  184 | 	 */
  185 | 	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  186 | 		this.customReturnValueHandlers = returnValueHandlers;
  187 | 	}
  188 | 
  189 | 	/**
  190 | 	 * Return the custom return value handlers, or {@code null}.
  191 | 	 */
  192 | 	public @Nullable List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
  193 | 		return this.customReturnValueHandlers;
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Configure the complete list of supported return value types thus
  198 | 	 * overriding handlers that would otherwise be configured by default.
  199 | 	 */
  200 | 	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  201 | 		if (returnValueHandlers == null) {
  202 | 			this.returnValueHandlers = null;
  203 | 		}
  204 | 		else {
  205 | 			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
  206 | 			this.returnValueHandlers.addHandlers(returnValueHandlers);
  207 | 		}
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * Return the configured handlers, or possibly {@code null} if not
  212 | 	 * initialized yet via {@link #afterPropertiesSet()}.
  213 | 	 */
  214 | 	public @Nullable HandlerMethodReturnValueHandlerComposite getReturnValueHandlers() {
  215 | 		return this.returnValueHandlers;
  216 | 	}
  217 | 
  218 | 	/**
  219 | 	 * Set the message body converters to use.
  220 | 	 * <p>These converters are used to convert from and to HTTP requests and responses.
  221 | 	 */
  222 | 	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
  223 | 		this.messageConverters.clear();
  224 | 		this.messageConverters.addAll(messageConverters);
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Return the configured message body converters.
  229 | 	 */
  230 | 	public List<HttpMessageConverter<?>> getMessageConverters() {
  231 | 		return this.messageConverters;
  232 | 	}
  233 | 
  234 | 	/**
  235 | 	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
  236 | 	 * If not set, the default constructor is used.
  237 | 	 */
  238 | 	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
  239 | 		this.contentNegotiationManager = contentNegotiationManager;
  240 | 	}
  241 | 
  242 | 	/**
  243 | 	 * Return the configured {@link ContentNegotiationManager}.
  244 | 	 */
  245 | 	public ContentNegotiationManager getContentNegotiationManager() {
  246 | 		return this.contentNegotiationManager;
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Add one or more components to be invoked after the execution of a controller
  251 | 	 * method annotated with {@code @ResponseBody} or returning {@code ResponseEntity}
  252 | 	 * but before the body is written to the response with the selected
  253 | 	 * {@code HttpMessageConverter}.
  254 | 	 */
  255 | 	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
  256 | 		if (responseBodyAdvice != null) {
  257 | 			this.responseBodyAdvice.addAll(responseBodyAdvice);
  258 | 		}
  259 | 	}
  260 | 
  261 | 	/**
  262 | 	 * Configure a list of {@link ErrorResponse.Interceptor}'s to apply when
  263 | 	 * rendering an RFC 9457 {@link org.springframework.http.ProblemDetail}
  264 | 	 * error response.
  265 | 	 * @param interceptors the handlers to use
  266 | 	 * @since 6.2
  267 | 	 */
  268 | 	public void setErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
  269 | 		this.errorResponseInterceptors.clear();
  270 | 		this.errorResponseInterceptors.addAll(interceptors);
  271 | 	}
  272 | 
  273 | 	/**
  274 | 	 * Return the {@link #setErrorResponseInterceptors(List) configured}
  275 | 	 * {@link ErrorResponse.Interceptor}'s.
  276 | 	 * @since 6.2
  277 | 	 */
  278 | 	public List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
  279 | 		return this.errorResponseInterceptors;
  280 | 	}
  281 | 
  282 | 	/**
  283 | 	 * Whether to render exceptions that are not handled by any
  284 | 	 * {@code @ExceptionHandler} method as RFC 9457 problem details.
  285 | 	 * <p>When enabled, a {@link ProblemDetailExceptionHandler} is invoked after
  286 | 	 * local {@code @ExceptionHandler} methods and those declared in
  287 | 	 * {@link ControllerAdvice @ControllerAdvice} beans, and also when one of them
  288 | 	 * rethrows the exception or fails. The resulting problem detail is rendered
  289 | 	 * like any other {@code @ExceptionHandler} return value, with content
  290 | 	 * negotiation, {@link ErrorResponse.Interceptor}s and {@code MessageSource}
  291 | 	 * lookups through the {@code ApplicationContext}.
  292 | 	 * <p>Exceptions resulting in a 5xx server error are recorded as the error
  293 | 	 * of the current HTTP server observation, if any.
  294 | 	 * <p>The problem detail is not rendered if the response is already committed.
  295 | 	 * <p>By default this is set to {@code false}.
  296 | 	 * @param renderUnhandledExceptionsAsProblemDetails whether to render
  297 | 	 * unhandled exceptions as problem details
  298 | 	 * @since 7.1
  299 | 	 * @see ProblemDetailExceptionHandler
  300 | 	 */
  301 | 	public void setRenderUnhandledExceptionsAsProblemDetails(boolean renderUnhandledExceptionsAsProblemDetails) {
  302 | 		this.renderUnhandledExceptionsAsProblemDetails = renderUnhandledExceptionsAsProblemDetails;
  303 | 	}
  304 | 
  305 | 	/**
  306 | 	 * Whether exceptions not handled by any {@code @ExceptionHandler} method
  307 | 	 * are rendered as RFC 9457 problem details.
  308 | 	 * @since 7.1
  309 | 	 * @see #setRenderUnhandledExceptionsAsProblemDetails(boolean)
  310 | 	 */
  311 | 	public boolean isRenderUnhandledExceptionsAsProblemDetails() {
  312 | 		return this.renderUnhandledExceptionsAsProblemDetails;
  313 | 	}
  314 | 
  315 | 	/**
  316 | 	 * Configure an {@link ErrorResponseViewResolver} to render problem details
  317 | 	 * returned by {@code @ExceptionHandler} methods as views, typically HTML
  318 | 	 * error pages, when the client prefers HTML.
  319 | 	 * <p>This applies to {@code ProblemDetail}, {@code ErrorResponse} and
  320 | 	 * {@code ResponseEntity<ProblemDetail>} return values, including problem details
  321 | 	 * rendered for {@link #setRenderUnhandledExceptionsAsProblemDetails(boolean)
  322 | 	 * unhandled exceptions}. It is not applied to custom
  323 | 	 * {@link #setReturnValueHandlers(List) return value handlers}.
  324 | 	 * <p>By default, this is not set and problem details are always written
  325 | 	 * with message converters.
  326 | 	 * @param errorResponseViewResolver the resolver to use
  327 | 	 * @since 7.1
  328 | 	 */
  329 | 	public void setErrorResponseViewResolver(@Nullable ErrorResponseViewResolver errorResponseViewResolver) {
  330 | 		this.errorResponseViewResolver = errorResponseViewResolver;
  331 | 	}
  332 | 
  333 | 	/**
  334 | 	 * Return the {@link #setErrorResponseViewResolver configured}
  335 | 	 * {@link ErrorResponseViewResolver}, if any.
  336 | 	 * @since 7.1
  337 | 	 */
  338 | 	public @Nullable ErrorResponseViewResolver getErrorResponseViewResolver() {
  339 | 		return this.errorResponseViewResolver;
  340 | 	}
  341 | 
  342 | 	@Override
  343 | 	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
  344 | 		this.applicationContext = applicationContext;
  345 | 	}
  346 | 
  347 | 	public @Nullable ApplicationContext getApplicationContext() {
  348 | 		return this.applicationContext;
  349 | 	}
  350 | 
  351 | 
  352 | 	@Override
  353 | 	public void afterPropertiesSet() {
  354 | 		// Do this first, it may add ResponseBodyAdvice beans
  355 | 		initExceptionHandlerAdviceCache();
  356 | 		initMessageConverters();
  357 | 		initProblemDetailExceptionHandler();
  358 | 
  359 | 		if (this.argumentResolvers == null) {
  360 | 			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
  361 | 			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
  362 | 		}
  363 | 		if (this.returnValueHandlers == null) {
  364 | 			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
  365 | 			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
  366 | 		}
  367 | 	}
  368 | 
  369 | 	private void initMessageConverters() {
  370 | 		if (!this.messageConverters.isEmpty()) {
  371 | 			return;
  372 | 		}
  373 | 		this.messageConverters.add(new ByteArrayHttpMessageConverter());
  374 | 		this.messageConverters.add(new StringHttpMessageConverter());
  375 | 		this.messageConverters.add(new FormHttpMessageConverter());
  376 | 		this.messageConverters.add(new MultipartHttpMessageConverter(HttpMessageConverters.forServer()
  377 | 				.registerDefaults().build()));
  378 | 	}
  379 | 
  380 | 	private void initProblemDetailExceptionHandler() {
  381 | 		if (this.renderUnhandledExceptionsAsProblemDetails) {
  382 | 			this.problemDetailExceptionHandler = new ProblemDetailExceptionHandler(getApplicationContext());
  383 | 			this.problemDetailHandlerMethod = new ExceptionHandlerMethodResolver(ProblemDetailExceptionHandler.class)
  384 | 					.resolveMethodByExceptionType(Exception.class);
  385 | 		}
  386 | 		else {
  387 | 			this.problemDetailExceptionHandler = null;
  388 | 			this.problemDetailHandlerMethod = null;
  389 | 		}
  390 | 	}
  391 | 
  392 | 	private void initExceptionHandlerAdviceCache() {
  393 | 		if (getApplicationContext() == null) {
  394 | 			return;
  395 | 		}
  396 | 
  397 | 		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
  398 | 		for (ControllerAdviceBean adviceBean : adviceBeans) {
  399 | 			Class<?> beanType = adviceBean.getBeanType();
  400 | 			if (beanType == null) {
  401 | 				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
  402 | 			}
  403 | 			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
  404 | 			if (resolver.hasExceptionMappings()) {
  405 | 				this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
  406 | 			}
  407 | 			if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
  408 | 				this.responseBodyAdvice.add(adviceBean);
  409 | 			}
  410 | 		}
  411 | 
  412 | 		if (logger.isDebugEnabled()) {
  413 | 			int handlerSize = this.exceptionHandlerAdviceCache.size();
  414 | 			int adviceSize = this.responseBodyAdvice.size();
  415 | 			if (handlerSize == 0 && adviceSize == 0) {
  416 | 				logger.debug("ControllerAdvice beans: none");
  417 | 			}
  418 | 			else {
  419 | 				logger.debug("ControllerAdvice beans: " +
  420 | 						handlerSize + " @ExceptionHandler, " + adviceSize + " ResponseBodyAdvice");
  421 | 			}
  422 | 		}
  423 | 	}
  424 | 
  425 | 	/**
  426 | 	 * Return an unmodifiable Map with the {@link ControllerAdvice @ControllerAdvice}
  427 | 	 * beans discovered in the ApplicationContext. The returned map will be empty if
  428 | 	 * the method is invoked before the bean has been initialized via
  429 | 	 * {@link #afterPropertiesSet()}.
  430 | 	 */
  431 | 	public Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> getExceptionHandlerAdviceCache() {
  432 | 		return Collections.unmodifiableMap(this.exceptionHandlerAdviceCache);
  433 | 	}
  434 | 
  435 | 	/**
  436 | 	 * Return the list of argument resolvers to use including built-in resolvers
  437 | 	 * and custom resolvers provided via {@link #setCustomArgumentResolvers}.
  438 | 	 */
  439 | 	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
  440 | 		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
  441 | 
  442 | 		// Annotation-based argument resolution
  443 | 		resolvers.add(new SessionAttributeMethodArgumentResolver());
  444 | 		resolvers.add(new RequestAttributeMethodArgumentResolver());
  445 | 
  446 | 		// Type-based argument resolution
  447 | 		resolvers.add(new ServletRequestMethodArgumentResolver());
  448 | 		resolvers.add(new ServletResponseMethodArgumentResolver());
  449 | 		resolvers.add(new RedirectAttributesMethodArgumentResolver());
  450 | 		resolvers.add(new ModelMethodProcessor());
  451 | 
  452 | 		// Custom arguments
  453 | 		if (getCustomArgumentResolvers() != null) {
  454 | 			resolvers.addAll(getCustomArgumentResolvers());
  455 | 		}
  456 | 
  457 | 		// Catch-all
  458 | 		resolvers.add(new PrincipalMethodArgumentResolver());
  459 | 
  460 | 		return resolvers;
  461 | 	}
  462 | 
  463 | 	/**
  464 | 	 * Return the list of return value handlers to use including built-in and
  465 | 	 * custom handlers provided via {@link #setReturnValueHandlers}.
  466 | 	 */
  467 | 	protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
  468 | 		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
  469 | 
  470 | 		// Single-purpose return value types
  471 | 		handlers.add(new ModelAndViewMethodReturnValueHandler());
  472 | 		handlers.add(new ModelMethodProcessor());
  473 | 		handlers.add(new ViewMethodReturnValueHandler());
  474 | 		HttpEntityMethodProcessor httpEntityProcessor = new HttpEntityMethodProcessor(
  475 | 				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice,
  476 | 				this.errorResponseInterceptors);
  477 | 		httpEntityProcessor.setErrorResponseViewResolver(this.errorResponseViewResolver);
  478 | 		handlers.add(httpEntityProcessor);
  479 | 
  480 | 		// Annotation-based return value types
  481 | 		handlers.add(new ServletModelAttributeMethodProcessor(false));
  482 | 		RequestResponseBodyMethodProcessor responseBodyProcessor = new RequestResponseBodyMethodProcessor(
  483 | 				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice,
  484 | 				this.errorResponseInterceptors);
  485 | 		responseBodyProcessor.setErrorResponseViewResolver(this.errorResponseViewResolver);
  486 | 		handlers.add(responseBodyProcessor);
  487 | 
  488 | 		// Multi-purpose return value types
  489 | 		handlers.add(new ViewNameMethodReturnValueHandler());
  490 | 		handlers.add(new MapMethodProcessor());
  491 | 
  492 | 		// Custom return value types
  493 | 		if (getCustomReturnValueHandlers() != null) {
  494 | 			handlers.addAll(getCustomReturnValueHandlers());
  495 | 		}
  496 | 
  497 | 		// Catch-all
  498 | 		handlers.add(new ServletModelAttributeMethodProcessor(true));
  499 | 
  500 | 		return handlers;
  501 | 	}
  502 | 
  503 | 	@Override
  504 | 	protected boolean hasGlobalExceptionHandlers() {
  505 | 		return (!this.exceptionHandlerAdviceCache.isEmpty() || this.problemDetailExceptionHandler != null);
  506 | 	}
  507 | 
  508 | 	@Override
  509 | 	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
  510 | 		if (this.problemDetailExceptionHandler != null && !hasHandlerMappings()) {
  511 | 			return true;  // render problem details for any handler
  512 | 		}
  513 | 		else if ((handler instanceof ResourceHttpRequestHandler || handler instanceof HandlerFunction) &&
  514 | 				hasGlobalExceptionHandlers() && !hasHandlerMappings()) {
  515 | 			return true;  // apply to ResourceHttpRequestHandler and HandlerFunction by default
  516 | 		}
  517 | 		else {
  518 | 			return super.shouldApplyTo(request, handler);
  519 | 		}
  520 | 	}
  521 | 
  522 | 	/**
  523 | 	 * Find an {@code @ExceptionHandler} method and invoke it to handle the raised exception.
  524 | 	 */
  525 | 	@Override
  526 | 	protected @Nullable ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
  527 | 			HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception exception) {
  528 | 
  529 | 		ServletWebRequest webRequest = new ServletWebRequest(request, response);
  530 | 		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception, webRequest);
  531 | 
  532 | 		if (exceptionHandlerMethod == null) {
  533 | 			return null;
  534 | 		}
  535 | 		return invokeExceptionHandlerMethod(exceptionHandlerMethod, webRequest, handlerMethod, exception);
  536 | 	}
  537 | 
  538 | 	private @Nullable ModelAndView invokeExceptionHandlerMethod(ServletInvocableHandlerMethod exceptionHandlerMethod,
  539 | 			ServletWebRequest webRequest, @Nullable HandlerMethod handlerMethod, Exception exception) {
  540 | 
  541 | 		HttpServletRequest request = webRequest.getRequest();
  542 | 		boolean isProblemDetailHandler = isProblemDetailHandlerMethod(exceptionHandlerMethod);
  543 | 
  544 | 		if (this.argumentResolvers != null) {
  545 | 			exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
  546 | 		}
  547 | 		if (this.returnValueHandlers != null) {
  548 | 			exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
  549 | 		}
  550 | 
  551 | 		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
  552 | 
  553 | 		ArrayList<Throwable> exceptions = new ArrayList<>();
  554 | 		try {
  555 | 			if (logger.isDebugEnabled()) {
  556 | 				logger.debug("Using @ExceptionHandler " + exceptionHandlerMethod);
  557 | 			}
  558 | 			// Expose causes as provided arguments as well
  559 | 			Throwable exToExpose = exception;
  560 | 			while (exToExpose != null) {
  561 | 				exceptions.add(exToExpose);
  562 | 				Throwable cause = exToExpose.getCause();
  563 | 				exToExpose = (cause != exToExpose ? cause : null);
  564 | 			}
  565 | 			@Nullable Object[] arguments = new Object[exceptions.size() + 1];
  566 | 			exceptions.toArray(arguments);  // efficient arraycopy call in ArrayList
  567 | 			arguments[arguments.length - 1] = handlerMethod;
  568 | 			exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, arguments);
  569 | 		}
  570 | 		catch (Throwable invocationEx) {
  571 | 			if (disconnectedClientHelper.checkAndLogClientDisconnectedException(invocationEx)) {
  572 | 				if (isProblemDetailHandler) {
  573 | 					recordServerError(exception, webRequest);
  574 | 				}
  575 | 				return new ModelAndView();
  576 | 			}
  577 | 			// Any other than the original exception (or a cause) is unintended here,
  578 | 			// probably an accident (for example, failed assertion or the like).
  579 | 			if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
  580 | 				logger.warn("Failure in @ExceptionHandler " + exceptionHandlerMethod, invocationEx);
  581 | 			}
  582 | 			if (!isProblemDetailHandler) {
  583 | 				ServletInvocableHandlerMethod problemDetailHandlerMethod = getProblemDetailHandlerMethod(webRequest);
  584 | 				if (problemDetailHandlerMethod != null) {
  585 | 					// Producible media types are those of the handler method that did not handle the exception
  586 | 					request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
  587 | 					return invokeExceptionHandlerMethod(problemDetailHandlerMethod, webRequest, handlerMethod, exception);
  588 | 				}
  589 | 			}
  590 | 			// Continue with default processing of the original exception...
  591 | 			return null;
  592 | 		}
  593 | 
  594 | 		if (isProblemDetailHandler) {
  595 | 			recordServerError(exception, webRequest);
  596 | 		}
  597 | 
  598 | 		if (mavContainer.isRequestHandled()) {
  599 | 			return new ModelAndView();
  600 | 		}
  601 | 		else {
  602 | 			ModelMap model = mavContainer.getModel();
  603 | 			HttpStatusCode status = mavContainer.getStatus();
  604 | 			ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, status);
  605 | 			mav.setViewName(mavContainer.getViewName());
  606 | 			if (!mavContainer.isViewReference()) {
  607 | 				mav.setView((View) mavContainer.getView());
  608 | 			}
  609 | 			if (model instanceof RedirectAttributes redirectAttributes) {
  610 | 				Map<String, ?> flashAttributes = redirectAttributes.getFlashAttributes();
  611 | 				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
  612 | 			}
  613 | 			return mav;
  614 | 		}
  615 | 	}
  616 | 
  617 | 	private boolean isProblemDetailHandlerMethod(ServletInvocableHandlerMethod handlerMethod) {
  618 | 		return (this.problemDetailExceptionHandler != null &&
  619 | 				handlerMethod.getBean() == this.problemDetailExceptionHandler);
  620 | 	}
  621 | 
  622 | 	private void recordServerError(Exception exception, ServletWebRequest webRequest) {
  623 | 		HttpServletResponse response = webRequest.getResponse();
  624 | 		if (response != null && HttpStatusCode.valueOf(response.getStatus()).is5xxServerError()) {
  625 | 			ServerHttpObservationFilter.recordHandledException(webRequest.getRequest(), exception);
  626 | 		}
  627 | 	}
  628 | 
  629 | 	/**
  630 | 	 * Find an {@code @ExceptionHandler} method for the given exception. The default
  631 | 	 * implementation searches methods in the class hierarchy of the controller first
  632 | 	 * and if not found, it continues searching for additional {@code @ExceptionHandler}
  633 | 	 * methods assuming some {@linkplain ControllerAdvice @ControllerAdvice}
  634 | 	 * Spring-managed beans were detected. As a last resort, if
  635 | 	 * {@link #setRenderUnhandledExceptionsAsProblemDetails(boolean) enabled},
  636 | 	 * a {@link ProblemDetailExceptionHandler} method is returned.
  637 | 	 * @param handlerMethod the method where the exception was raised (may be {@code null})
  638 | 	 * @param exception the raised exception
  639 | 	 * @param webRequest the original web request that resulted in a handler error
  640 | 	 * @return a method to handle the exception, or {@code null} if none
  641 | 	 */
  642 | 	protected @Nullable ServletInvocableHandlerMethod getExceptionHandlerMethod(
  643 | 			@Nullable HandlerMethod handlerMethod, Exception exception, ServletWebRequest webRequest) {
  644 | 
  645 | 		List<MediaType> acceptedMediaTypes = List.of(MediaType.ALL);
  646 | 		try {
  647 | 			acceptedMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
  648 | 		}
  649 | 		catch (HttpMediaTypeNotAcceptableException mediaTypeExc) {
  650 | 			if (logger.isDebugEnabled()) {
  651 | 				logger.debug("Could not resolve accepted media types for @ExceptionHandler [" + webRequest.getHeader(HttpHeaders.ACCEPT) + "]", mediaTypeExc);
  652 | 			}
  653 | 		}
  654 | 
  655 | 		Class<?> handlerType = null;
  656 | 
  657 | 		if (handlerMethod != null) {
  658 | 			// Local exception handler methods on the controller class itself.
  659 | 			// To be invoked through the proxy, even in case of an interface-based proxy.
  660 | 			handlerType = handlerMethod.getBeanType();
  661 | 			ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.computeIfAbsent(
  662 | 					handlerType, ExceptionHandlerMethodResolver::new);
  663 | 
  664 | 			for (MediaType mediaType : acceptedMediaTypes) {
  665 | 				ExceptionHandlerMappingInfo mappingInfo = resolver.resolveExceptionMapping(exception, mediaType);
  666 | 				if (mappingInfo != null) {
  667 | 					if (!mappingInfo.getProducibleTypes().isEmpty()) {
  668 | 						webRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mappingInfo.getProducibleTypes(), RequestAttributes.SCOPE_REQUEST);
  669 | 					}
  670 | 					return new ServletInvocableHandlerMethod(handlerMethod.getBean(), mappingInfo.getHandlerMethod(), this.applicationContext);
  671 | 				}
  672 | 			}
  673 | 			// For advice applicability check below (involving base packages, assignable types
  674 | 			// and annotation presence), use target class instead of interface-based proxy.
  675 | 			if (Proxy.isProxyClass(handlerType)) {
  676 | 				handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
  677 | 			}
  678 | 		}
  679 | 
  680 | 		for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
  681 | 			ControllerAdviceBean advice = entry.getKey();
  682 | 			if (advice.isApplicableToBeanType(handlerType)) {
  683 | 				ExceptionHandlerMethodResolver resolver = entry.getValue();
  684 | 				for (MediaType mediaType : acceptedMediaTypes) {
  685 | 					ExceptionHandlerMappingInfo mappingInfo = resolver.resolveExceptionMapping(exception, mediaType);
  686 | 					if (mappingInfo != null) {
  687 | 						if (!mappingInfo.getProducibleTypes().isEmpty()) {
  688 | 							webRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mappingInfo.getProducibleTypes(), RequestAttributes.SCOPE_REQUEST);
  689 | 						}
  690 | 						return new ServletInvocableHandlerMethod(advice.resolveBean(), mappingInfo.getHandlerMethod(), this.applicationContext);
  691 | 					}
  692 | 				}
  693 | 			}
  694 | 		}
  695 | 
  696 | 		return getProblemDetailHandlerMethod(webRequest);
  697 | 	}
  698 | 
  699 | 	private @Nullable ServletInvocableHandlerMethod getProblemDetailHandlerMethod(ServletWebRequest webRequest) {
  700 | 		if (this.problemDetailExceptionHandler == null || this.problemDetailHandlerMethod == null) {
  701 | 			return null;
  702 | 		}
  703 | 		HttpServletResponse response = webRequest.getResponse();
  704 | 		if (response != null && response.isCommitted()) {
  705 | 			if (logger.isDebugEnabled()) {
  706 | 				logger.debug("Response already committed, not rendering a problem detail");
  707 | 			}
  708 | 			return null;
  709 | 		}
  710 | 		return new ServletInvocableHandlerMethod(
  711 | 				this.problemDetailExceptionHandler, this.problemDetailHandlerMethod, this.applicationContext);
  712 | 	}
  713 | 
  714 | }
```
