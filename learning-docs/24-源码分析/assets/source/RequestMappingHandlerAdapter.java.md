<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter.java -->
<!-- 正文引用：[[源码:RequestMappingHandlerAdapter.java#L起-L止]] ；代码块头部：(src: RequestMappingHandlerAdapter.java#L起-L止) -->

# RequestMappingHandlerAdapter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter.java` |
| 行数 | 1096 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.811Z |

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
   20 | import java.util.ArrayList;
   21 | import java.util.Collections;
   22 | import java.util.LinkedHashMap;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | import java.util.concurrent.Callable;
   27 | import java.util.concurrent.ConcurrentHashMap;
   28 | import java.util.function.Predicate;
   29 | 
   30 | import jakarta.servlet.http.HttpServletRequest;
   31 | import jakarta.servlet.http.HttpServletResponse;
   32 | import jakarta.servlet.http.HttpSession;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.beans.factory.BeanFactory;
   36 | import org.springframework.beans.factory.BeanFactoryAware;
   37 | import org.springframework.beans.factory.BeanFactoryUtils;
   38 | import org.springframework.beans.factory.InitializingBean;
   39 | import org.springframework.beans.factory.ListableBeanFactory;
   40 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   41 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   42 | import org.springframework.core.DefaultParameterNameDiscoverer;
   43 | import org.springframework.core.KotlinDetector;
   44 | import org.springframework.core.MethodIntrospector;
   45 | import org.springframework.core.MethodParameter;
   46 | import org.springframework.core.ParameterNameDiscoverer;
   47 | import org.springframework.core.ReactiveAdapterRegistry;
   48 | import org.springframework.core.annotation.AnnotatedElementUtils;
   49 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   50 | import org.springframework.core.log.LogFormatUtils;
   51 | import org.springframework.core.task.AsyncTaskExecutor;
   52 | import org.springframework.core.task.SimpleAsyncTaskExecutor;
   53 | import org.springframework.http.converter.ByteArrayHttpMessageConverter;
   54 | import org.springframework.http.converter.FormHttpMessageConverter;
   55 | import org.springframework.http.converter.HttpMessageConverter;
   56 | import org.springframework.http.converter.HttpMessageConverters;
   57 | import org.springframework.http.converter.StringHttpMessageConverter;
   58 | import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
   59 | import org.springframework.ui.ModelMap;
   60 | import org.springframework.util.Assert;
   61 | import org.springframework.util.ClassUtils;
   62 | import org.springframework.util.CollectionUtils;
   63 | import org.springframework.util.ReflectionUtils.MethodFilter;
   64 | import org.springframework.validation.method.MethodValidator;
   65 | import org.springframework.web.ErrorResponse;
   66 | import org.springframework.web.accept.ContentNegotiationManager;
   67 | import org.springframework.web.bind.annotation.InitBinder;
   68 | import org.springframework.web.bind.annotation.ModelAttribute;
   69 | import org.springframework.web.bind.annotation.RequestMapping;
   70 | import org.springframework.web.bind.support.DefaultDataBinderFactory;
   71 | import org.springframework.web.bind.support.DefaultSessionAttributeStore;
   72 | import org.springframework.web.bind.support.SessionAttributeStore;
   73 | import org.springframework.web.bind.support.WebBindingInitializer;
   74 | import org.springframework.web.bind.support.WebDataBinderFactory;
   75 | import org.springframework.web.context.request.NativeWebRequest;
   76 | import org.springframework.web.context.request.ServletWebRequest;
   77 | import org.springframework.web.context.request.async.AsyncWebRequest;
   78 | import org.springframework.web.context.request.async.CallableProcessingInterceptor;
   79 | import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
   80 | import org.springframework.web.context.request.async.WebAsyncManager;
   81 | import org.springframework.web.context.request.async.WebAsyncTask;
   82 | import org.springframework.web.context.request.async.WebAsyncUtils;
   83 | import org.springframework.web.method.ControllerAdviceBean;
   84 | import org.springframework.web.method.HandlerMethod;
   85 | import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver;
   86 | import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver;
   87 | import org.springframework.web.method.annotation.HandlerMethodValidator;
   88 | import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
   89 | import org.springframework.web.method.annotation.MapMethodProcessor;
   90 | import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
   91 | import org.springframework.web.method.annotation.ModelFactory;
   92 | import org.springframework.web.method.annotation.ModelMethodProcessor;
   93 | import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver;
   94 | import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
   95 | import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
   96 | import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
   97 | import org.springframework.web.method.annotation.SessionAttributesHandler;
   98 | import org.springframework.web.method.annotation.SessionStatusMethodArgumentResolver;
   99 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
  100 | import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
  101 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
  102 | import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
  103 | import org.springframework.web.method.support.InvocableHandlerMethod;
  104 | import org.springframework.web.method.support.ModelAndViewContainer;
  105 | import org.springframework.web.servlet.DispatcherServlet;
  106 | import org.springframework.web.servlet.LocaleResolver;
  107 | import org.springframework.web.servlet.ModelAndView;
  108 | import org.springframework.web.servlet.View;
  109 | import org.springframework.web.servlet.ViewResolver;
  110 | import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
  111 | import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
  112 | import org.springframework.web.servlet.mvc.support.RedirectAttributes;
  113 | import org.springframework.web.servlet.support.RequestContextUtils;
  114 | import org.springframework.web.util.WebUtils;
  115 | 
  116 | /**
  117 |  * Extension of {@link AbstractHandlerMethodAdapter} that supports
  118 |  * {@link RequestMapping @RequestMapping} annotated {@link HandlerMethod HandlerMethods}.
  119 |  *
  120 |  * <p>Support for custom argument and return value types can be added via
  121 |  * {@link #setCustomArgumentResolvers} and {@link #setCustomReturnValueHandlers},
  122 |  * or alternatively, to re-configure all argument and return value types,
  123 |  * use {@link #setArgumentResolvers} and {@link #setReturnValueHandlers}.
  124 |  *
  125 |  * @author Rossen Stoyanchev
  126 |  * @author Juergen Hoeller
  127 |  * @author Sebastien Deleuze
  128 |  * @since 3.1
  129 |  * @see HandlerMethodArgumentResolver
  130 |  * @see HandlerMethodReturnValueHandler
  131 |  */
  132 | public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
  133 | 		implements BeanFactoryAware, InitializingBean {
  134 | 
  135 | 	/**
  136 | 	 * MethodFilter that matches {@link InitBinder @InitBinder} methods.
  137 | 	 */
  138 | 	public static final MethodFilter INIT_BINDER_METHODS = method ->
  139 | 			AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);
  140 | 
  141 | 	/**
  142 | 	 * MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods.
  143 | 	 */
  144 | 	public static final MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
  145 | 			(!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) &&
  146 | 					AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class));
  147 | 
  148 | 	private static final boolean BEAN_VALIDATION_PRESENT =
  149 | 			ClassUtils.isPresent("jakarta.validation.Validator", HandlerMethod.class.getClassLoader());
  150 | 
  151 | 
  152 | 	private @Nullable List<HandlerMethodArgumentResolver> customArgumentResolvers;
  153 | 
  154 | 	private @Nullable HandlerMethodArgumentResolverComposite argumentResolvers;
  155 | 
  156 | 	private @Nullable HandlerMethodArgumentResolverComposite initBinderArgumentResolvers;
  157 | 
  158 | 	private @Nullable List<HandlerMethodReturnValueHandler> customReturnValueHandlers;
  159 | 
  160 | 	private @Nullable HandlerMethodReturnValueHandlerComposite returnValueHandlers;
  161 | 
  162 | 	private @Nullable List<ModelAndViewResolver> modelAndViewResolvers;
  163 | 
  164 | 	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();
  165 | 
  166 | 	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
  167 | 
  168 | 	private final List<Object> requestResponseBodyAdvice = new ArrayList<>();
  169 | 
  170 | 	private @Nullable WebBindingInitializer webBindingInitializer;
  171 | 
  172 | 	private final List<ErrorResponse.Interceptor> errorResponseInterceptors = new ArrayList<>();
  173 | 
  174 | 	private @Nullable MethodValidator methodValidator;
  175 | 
  176 | 	private AsyncTaskExecutor taskExecutor = new MvcSimpleAsyncTaskExecutor();
  177 | 
  178 | 	private @Nullable Long asyncRequestTimeout;
  179 | 
  180 | 	private CallableProcessingInterceptor[] callableInterceptors = new CallableProcessingInterceptor[0];
  181 | 
  182 | 	private DeferredResultProcessingInterceptor[] deferredResultInterceptors = new DeferredResultProcessingInterceptor[0];
  183 | 
  184 | 	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
  185 | 
  186 | 	private int cacheSecondsForSessionAttributeHandlers = 0;
  187 | 
  188 | 	private boolean synchronizeOnSession = false;
  189 | 
  190 | 	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();
  191 | 
  192 | 	private ParameterNameDiscoverer parameterNameDiscoverer = DefaultParameterNameDiscoverer.getSharedInstance();
  193 | 
  194 | 	private @Nullable ConfigurableBeanFactory beanFactory;
  195 | 
  196 | 	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);
  197 | 
  198 | 	private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);
  199 | 
  200 | 	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>();
  201 | 
  202 | 	private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);
  203 | 
  204 | 	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>();
  205 | 
  206 | 
  207 | 	/**
  208 | 	 * Provide resolvers for custom argument types. Custom resolvers are ordered
  209 | 	 * after built-in ones. To override the built-in support for argument
  210 | 	 * resolution use {@link #setArgumentResolvers} instead.
  211 | 	 */
  212 | 	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
  213 | 		this.customArgumentResolvers = argumentResolvers;
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Return the custom argument resolvers, or {@code null}.
  218 | 	 */
  219 | 	public @Nullable List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
  220 | 		return this.customArgumentResolvers;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Configure the complete list of supported argument types thus overriding
  225 | 	 * the resolvers that would otherwise be configured by default.
  226 | 	 */
  227 | 	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
  228 | 		if (argumentResolvers == null) {
  229 | 			this.argumentResolvers = null;
  230 | 		}
  231 | 		else {
  232 | 			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
  233 | 			this.argumentResolvers.addResolvers(argumentResolvers);
  234 | 		}
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Return the configured argument resolvers, or possibly {@code null} if
  239 | 	 * not initialized yet via {@link #afterPropertiesSet()}.
  240 | 	 */
  241 | 	public @Nullable List<HandlerMethodArgumentResolver> getArgumentResolvers() {
  242 | 		return (this.argumentResolvers != null ? this.argumentResolvers.getResolvers() : null);
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * Configure the supported argument types in {@code @InitBinder} methods.
  247 | 	 */
  248 | 	public void setInitBinderArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
  249 | 		if (argumentResolvers == null) {
  250 | 			this.initBinderArgumentResolvers = null;
  251 | 		}
  252 | 		else {
  253 | 			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
  254 | 			this.initBinderArgumentResolvers.addResolvers(argumentResolvers);
  255 | 		}
  256 | 	}
  257 | 
  258 | 	/**
  259 | 	 * Return the argument resolvers for {@code @InitBinder} methods, or possibly
  260 | 	 * {@code null} if not initialized yet via {@link #afterPropertiesSet()}.
  261 | 	 */
  262 | 	public @Nullable List<HandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
  263 | 		return (this.initBinderArgumentResolvers != null ? this.initBinderArgumentResolvers.getResolvers() : null);
  264 | 	}
  265 | 
  266 | 	/**
  267 | 	 * Provide handlers for custom return value types. Custom handlers are
  268 | 	 * ordered after built-in ones. To override the built-in support for
  269 | 	 * return value handling use {@link #setReturnValueHandlers}.
  270 | 	 */
  271 | 	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  272 | 		this.customReturnValueHandlers = returnValueHandlers;
  273 | 	}
  274 | 
  275 | 	/**
  276 | 	 * Return the custom return value handlers, or {@code null}.
  277 | 	 */
  278 | 	public @Nullable List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
  279 | 		return this.customReturnValueHandlers;
  280 | 	}
  281 | 
  282 | 	/**
  283 | 	 * Configure the complete list of supported return value types thus
  284 | 	 * overriding handlers that would otherwise be configured by default.
  285 | 	 */
  286 | 	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  287 | 		if (returnValueHandlers == null) {
  288 | 			this.returnValueHandlers = null;
  289 | 		}
  290 | 		else {
  291 | 			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
  292 | 			this.returnValueHandlers.addHandlers(returnValueHandlers);
  293 | 		}
  294 | 	}
  295 | 
  296 | 	/**
  297 | 	 * Return the configured handlers, or possibly {@code null} if not
  298 | 	 * initialized yet via {@link #afterPropertiesSet()}.
  299 | 	 */
  300 | 	public @Nullable List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
  301 | 		return (this.returnValueHandlers != null ? this.returnValueHandlers.getHandlers() : null);
  302 | 	}
  303 | 
  304 | 	/**
  305 | 	 * Provide custom {@link ModelAndViewResolver ModelAndViewResolvers}.
  306 | 	 * <p><strong>Note:</strong> This method is available for backwards
  307 | 	 * compatibility only. However, it is recommended to re-write a
  308 | 	 * {@code ModelAndViewResolver} as {@link HandlerMethodReturnValueHandler}.
  309 | 	 * An adapter between the two interfaces is not possible since the
  310 | 	 * {@link HandlerMethodReturnValueHandler#supportsReturnType} method
  311 | 	 * cannot be implemented. Hence {@code ModelAndViewResolver}s are limited
  312 | 	 * to always being invoked at the end after all other return value
  313 | 	 * handlers have been given a chance.
  314 | 	 * <p>A {@code HandlerMethodReturnValueHandler} provides better access to
  315 | 	 * the return type and controller method information and can be ordered
  316 | 	 * freely relative to other return value handlers.
  317 | 	 */
  318 | 	public void setModelAndViewResolvers(@Nullable List<ModelAndViewResolver> modelAndViewResolvers) {
  319 | 		this.modelAndViewResolvers = modelAndViewResolvers;
  320 | 	}
  321 | 
  322 | 	/**
  323 | 	 * Return the configured {@link ModelAndViewResolver ModelAndViewResolvers}, or {@code null}.
  324 | 	 */
  325 | 	public @Nullable List<ModelAndViewResolver> getModelAndViewResolvers() {
  326 | 		return this.modelAndViewResolvers;
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
  331 | 	 * If not set, the default constructor is used.
  332 | 	 */
  333 | 	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
  334 | 		this.contentNegotiationManager = contentNegotiationManager;
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Provide the converters to use in argument resolvers and return value
  339 | 	 * handlers that support reading and/or writing to the body of the
  340 | 	 * request and response.
  341 | 	 */
  342 | 	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
  343 | 		this.messageConverters.clear();
  344 | 		this.messageConverters.addAll(messageConverters);
  345 | 	}
  346 | 
  347 | 	/**
  348 | 	 * Return the configured message body converters.
  349 | 	 */
  350 | 	public List<HttpMessageConverter<?>> getMessageConverters() {
  351 | 		return this.messageConverters;
  352 | 	}
  353 | 
  354 | 	/**
  355 | 	 * Add one or more {@code RequestBodyAdvice} instances to intercept the
  356 | 	 * request before it is read and converted for {@code @RequestBody} and
  357 | 	 * {@code HttpEntity} method arguments.
  358 | 	 */
  359 | 	public void setRequestBodyAdvice(@Nullable List<RequestBodyAdvice> requestBodyAdvice) {
  360 | 		if (requestBodyAdvice != null) {
  361 | 			this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
  362 | 		}
  363 | 	}
  364 | 
  365 | 	/**
  366 | 	 * Add one or more {@code ResponseBodyAdvice} instances to intercept the
  367 | 	 * response before {@code @ResponseBody} or {@code ResponseEntity} return
  368 | 	 * values are written to the response body.
  369 | 	 */
  370 | 	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
  371 | 		if (responseBodyAdvice != null) {
  372 | 			this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
  373 | 		}
  374 | 	}
  375 | 
  376 | 	/**
  377 | 	 * Provide a WebBindingInitializer with "global" initialization to apply
  378 | 	 * to every DataBinder instance.
  379 | 	 */
  380 | 	public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
  381 | 		this.webBindingInitializer = webBindingInitializer;
  382 | 	}
  383 | 
  384 | 	/**
  385 | 	 * Return the configured WebBindingInitializer, or {@code null} if none.
  386 | 	 */
  387 | 	public @Nullable WebBindingInitializer getWebBindingInitializer() {
  388 | 		return this.webBindingInitializer;
  389 | 	}
  390 | 
  391 | 	/**
  392 | 	 * Configure a list of {@link ErrorResponse.Interceptor}'s to apply when
  393 | 	 * rendering an RFC 9457 {@link org.springframework.http.ProblemDetail}
  394 | 	 * error response.
  395 | 	 * @param interceptors the interceptors to use
  396 | 	 * @since 6.2
  397 | 	 */
  398 | 	public void setErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
  399 | 		this.errorResponseInterceptors.clear();
  400 | 		this.errorResponseInterceptors.addAll(interceptors);
  401 | 	}
  402 | 
  403 | 	/**
  404 | 	 * Return the {@link #setErrorResponseInterceptors(List) configured}
  405 | 	 * {@link ErrorResponse.Interceptor}'s.
  406 | 	 * @since 6.2
  407 | 	 */
  408 | 	public List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
  409 | 		return this.errorResponseInterceptors;
  410 | 	}
  411 | 
  412 | 	/**
  413 | 	 * Set the default {@link AsyncTaskExecutor} to use when a controller method
  414 | 	 * return a {@link Callable}. Controller methods can override this default on
  415 | 	 * a per-request basis by returning an {@link WebAsyncTask}.
  416 | 	 * <p>If your application has controllers with such return types, please
  417 | 	 * configure an {@link AsyncTaskExecutor} as the one used by default is not
  418 | 	 * suitable for production under load.
  419 | 	 */
  420 | 	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
  421 | 		this.taskExecutor = taskExecutor;
  422 | 	}
  423 | 
  424 | 	/**
  425 | 	 * Specify the amount of time, in milliseconds, before concurrent handling
  426 | 	 * should time out. In Servlet 3, the timeout begins after the main request
  427 | 	 * processing thread has exited and ends when the request is dispatched again
  428 | 	 * for further processing of the concurrently produced result.
  429 | 	 * <p>If this value is not set, the default timeout of the underlying
  430 | 	 * implementation is used.
  431 | 	 * @param timeout the timeout value in milliseconds
  432 | 	 */
  433 | 	public void setAsyncRequestTimeout(long timeout) {
  434 | 		this.asyncRequestTimeout = timeout;
  435 | 	}
  436 | 
  437 | 	/**
  438 | 	 * Configure {@code CallableProcessingInterceptor}'s to register on async requests.
  439 | 	 * @param interceptors the interceptors to register
  440 | 	 */
  441 | 	public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
  442 | 		this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[0]);
  443 | 	}
  444 | 
  445 | 	/**
  446 | 	 * Configure {@code DeferredResultProcessingInterceptor}'s to register on async requests.
  447 | 	 * @param interceptors the interceptors to register
  448 | 	 */
  449 | 	public void setDeferredResultInterceptors(List<DeferredResultProcessingInterceptor> interceptors) {
  450 | 		this.deferredResultInterceptors = interceptors.toArray(new DeferredResultProcessingInterceptor[0]);
  451 | 	}
  452 | 
  453 | 	/**
  454 | 	 * Configure the registry for reactive library types to be supported as
  455 | 	 * return values from controller methods.
  456 | 	 * @since 5.0.5
  457 | 	 */
  458 | 	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
  459 | 		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
  460 | 	}
  461 | 
  462 | 	/**
  463 | 	 * Return the configured reactive type registry of adapters.
  464 | 	 * @since 5.0
  465 | 	 */
  466 | 	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
  467 | 		return this.reactiveAdapterRegistry;
  468 | 	}
  469 | 
  470 | 	/**
  471 | 	 * Specify the strategy to store session attributes with. The default is
  472 | 	 * {@link org.springframework.web.bind.support.DefaultSessionAttributeStore},
  473 | 	 * storing session attributes in the HttpSession with the same attribute
  474 | 	 * name as in the model.
  475 | 	 */
  476 | 	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
  477 | 		this.sessionAttributeStore = sessionAttributeStore;
  478 | 	}
  479 | 
  480 | 	/**
  481 | 	 * Cache content produced by {@code @SessionAttributes} annotated handlers
  482 | 	 * for the given number of seconds.
  483 | 	 * <p>Possible values are:
  484 | 	 * <ul>
  485 | 	 * <li>-1: no generation of cache-related headers</li>
  486 | 	 * <li>0 (default value): "Cache-Control: no-store" will prevent caching</li>
  487 | 	 * <li>1 or higher: "Cache-Control: max-age=seconds" will ask to cache content;
  488 | 	 * not advised when dealing with session attributes</li>
  489 | 	 * </ul>
  490 | 	 * <p>In contrast to the "cacheSeconds" property which will apply to all general
  491 | 	 * handlers (but not to {@code @SessionAttributes} annotated handlers),
  492 | 	 * this setting will apply to {@code @SessionAttributes} handlers only.
  493 | 	 * @see #setCacheSeconds
  494 | 	 * @see org.springframework.web.bind.annotation.SessionAttributes
  495 | 	 */
  496 | 	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
  497 | 		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
  498 | 	}
  499 | 
  500 | 	/**
  501 | 	 * Set if controller execution should be synchronized on the session,
  502 | 	 * to serialize parallel invocations from the same client.
  503 | 	 * <p>More specifically, the execution of the {@code handleRequestInternal}
  504 | 	 * method will get synchronized if this flag is "true". The best available
  505 | 	 * session mutex will be used for the synchronization; ideally, this will
  506 | 	 * be a mutex exposed by HttpSessionMutexListener.
  507 | 	 * <p>The session mutex is guaranteed to be the same object during
  508 | 	 * the entire lifetime of the session, available under the key defined
  509 | 	 * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
  510 | 	 * safe reference to synchronize on for locking on the current session.
  511 | 	 * <p>In many cases, the HttpSession reference itself is a safe mutex
  512 | 	 * as well, since it will always be the same object reference for the
  513 | 	 * same active logical session. However, this is not guaranteed across
  514 | 	 * different servlet containers; the only 100% safe way is a session mutex.
  515 | 	 * @see org.springframework.web.util.HttpSessionMutexListener
  516 | 	 * @see org.springframework.web.util.WebUtils#getSessionMutex(jakarta.servlet.http.HttpSession)
  517 | 	 */
  518 | 	public void setSynchronizeOnSession(boolean synchronizeOnSession) {
  519 | 		this.synchronizeOnSession = synchronizeOnSession;
  520 | 	}
  521 | 
  522 | 	/**
  523 | 	 * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
  524 | 	 * (for example, for default attribute names).
  525 | 	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
  526 | 	 */
  527 | 	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
  528 | 		this.parameterNameDiscoverer = parameterNameDiscoverer;
  529 | 	}
  530 | 
  531 | 	/**
  532 | 	 * A {@link ConfigurableBeanFactory} is expected for resolving expressions
  533 | 	 * in method argument default values.
  534 | 	 */
  535 | 	@Override
  536 | 	public void setBeanFactory(BeanFactory beanFactory) {
  537 | 		if (beanFactory instanceof ConfigurableBeanFactory cbf) {
  538 | 			this.beanFactory = cbf;
  539 | 		}
  540 | 	}
  541 | 
  542 | 	/**
  543 | 	 * Return the owning factory of this bean instance, or {@code null} if none.
  544 | 	 */
  545 | 	protected @Nullable ConfigurableBeanFactory getBeanFactory() {
  546 | 		return this.beanFactory;
  547 | 	}
  548 | 
  549 | 
  550 | 	@Override
  551 | 	public void afterPropertiesSet() {
  552 | 		// Do this first, it may add ResponseBody advice beans
  553 | 		initControllerAdviceCache();
  554 | 		initMessageConverters();
  555 | 
  556 | 		if (this.argumentResolvers == null) {
  557 | 			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
  558 | 			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
  559 | 		}
  560 | 		if (this.initBinderArgumentResolvers == null) {
  561 | 			List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
  562 | 			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
  563 | 		}
  564 | 		if (this.returnValueHandlers == null) {
  565 | 			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
  566 | 			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
  567 | 		}
  568 | 		if (BEAN_VALIDATION_PRESENT) {
  569 | 			List<HandlerMethodArgumentResolver> resolvers = this.argumentResolvers.getResolvers();
  570 | 			this.methodValidator = HandlerMethodValidator.from(
  571 | 					this.webBindingInitializer, this.parameterNameDiscoverer,
  572 | 					methodParamPredicate(resolvers, ModelAttributeMethodProcessor.class),
  573 | 					methodParamPredicate(resolvers, RequestParamMethodArgumentResolver.class));
  574 | 		}
  575 | 	}
  576 | 
  577 | 	private void initMessageConverters() {
  578 | 		if (!this.messageConverters.isEmpty()) {
  579 | 			return;
  580 | 		}
  581 | 		this.messageConverters.add(new ByteArrayHttpMessageConverter());
  582 | 		this.messageConverters.add(new StringHttpMessageConverter());
  583 | 
  584 | 		this.messageConverters.add(new FormHttpMessageConverter());
  585 | 		this.messageConverters.add(new MultipartHttpMessageConverter(HttpMessageConverters.forServer()
  586 | 				.registerDefaults().build()));
  587 | 	}
  588 | 
  589 | 	private void initControllerAdviceCache() {
  590 | 		if (getApplicationContext() == null) {
  591 | 			return;
  592 | 		}
  593 | 
  594 | 		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
  595 | 
  596 | 		List<Object> requestResponseBodyAdviceBeans = new ArrayList<>();
  597 | 
  598 | 		for (ControllerAdviceBean adviceBean : adviceBeans) {
  599 | 			Class<?> beanType = adviceBean.getBeanType();
  600 | 			if (beanType == null) {
  601 | 				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
  602 | 			}
  603 | 			Set<Method> attrMethods = MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);
  604 | 			if (!attrMethods.isEmpty()) {
  605 | 				this.modelAttributeAdviceCache.put(adviceBean, attrMethods);
  606 | 			}
  607 | 			Set<Method> binderMethods = MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);
  608 | 			if (!binderMethods.isEmpty()) {
  609 | 				this.initBinderAdviceCache.put(adviceBean, binderMethods);
  610 | 			}
  611 | 			if (RequestBodyAdvice.class.isAssignableFrom(beanType) || ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
  612 | 				requestResponseBodyAdviceBeans.add(adviceBean);
  613 | 			}
  614 | 		}
  615 | 
  616 | 		if (!requestResponseBodyAdviceBeans.isEmpty()) {
  617 | 			this.requestResponseBodyAdvice.addAll(0, requestResponseBodyAdviceBeans);
  618 | 		}
  619 | 
  620 | 		if (logger.isDebugEnabled()) {
  621 | 			int modelSize = this.modelAttributeAdviceCache.size();
  622 | 			int binderSize = this.initBinderAdviceCache.size();
  623 | 			int reqCount = getBodyAdviceCount(RequestBodyAdvice.class);
  624 | 			int resCount = getBodyAdviceCount(ResponseBodyAdvice.class);
  625 | 			if (modelSize == 0 && binderSize == 0 && reqCount == 0 && resCount == 0) {
  626 | 				logger.debug("ControllerAdvice beans: none");
  627 | 			}
  628 | 			else {
  629 | 				logger.debug("ControllerAdvice beans: " + modelSize + " @ModelAttribute, " + binderSize +
  630 | 						" @InitBinder, " + reqCount + " RequestBodyAdvice, " + resCount + " ResponseBodyAdvice");
  631 | 			}
  632 | 		}
  633 | 	}
  634 | 
  635 | 	// Count all advice, including explicit registrations..
  636 | 
  637 | 	private int getBodyAdviceCount(Class<?> adviceType) {
  638 | 		List<Object> advice = this.requestResponseBodyAdvice;
  639 | 		return RequestBodyAdvice.class.isAssignableFrom(adviceType) ?
  640 | 				RequestResponseBodyAdviceChain.getAdviceByType(advice, RequestBodyAdvice.class).size() :
  641 | 				RequestResponseBodyAdviceChain.getAdviceByType(advice, ResponseBodyAdvice.class).size();
  642 | 	}
  643 | 
  644 | 	/**
  645 | 	 * Return the list of argument resolvers to use including built-in resolvers
  646 | 	 * and custom resolvers provided via {@link #setCustomArgumentResolvers}.
  647 | 	 */
  648 | 	private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
  649 | 		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(30);
  650 | 
  651 | 		// Annotation-based argument resolution
  652 | 		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
  653 | 		resolvers.add(new RequestParamMapMethodArgumentResolver());
  654 | 		resolvers.add(new PathVariableMethodArgumentResolver());
  655 | 		resolvers.add(new PathVariableMapMethodArgumentResolver());
  656 | 		resolvers.add(new MatrixVariableMethodArgumentResolver());
  657 | 		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
  658 | 		resolvers.add(new ServletModelAttributeMethodProcessor(false));
  659 | 		resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
  660 | 		resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));
  661 | 		resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
  662 | 		resolvers.add(new RequestHeaderMapMethodArgumentResolver());
  663 | 		resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
  664 | 		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
  665 | 		resolvers.add(new SessionAttributeMethodArgumentResolver());
  666 | 		resolvers.add(new RequestAttributeMethodArgumentResolver());
  667 | 
  668 | 		// Type-based argument resolution
  669 | 		resolvers.add(new ServletRequestMethodArgumentResolver());
  670 | 		resolvers.add(new ServletResponseMethodArgumentResolver());
  671 | 		resolvers.add(new HttpEntityMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));
  672 | 		resolvers.add(new RedirectAttributesMethodArgumentResolver());
  673 | 		resolvers.add(new ModelMethodProcessor());
  674 | 		resolvers.add(new MapMethodProcessor());
  675 | 		resolvers.add(new ErrorsMethodArgumentResolver());
  676 | 		resolvers.add(new SessionStatusMethodArgumentResolver());
  677 | 		resolvers.add(new UriComponentsBuilderMethodArgumentResolver());
  678 | 		resolvers.add(new ApiVersionMethodArgumentResolver());
  679 | 		if (KotlinDetector.isKotlinPresent()) {
  680 | 			resolvers.add(new ContinuationHandlerMethodArgumentResolver());
  681 | 		}
  682 | 
  683 | 		// Custom arguments
  684 | 		if (getCustomArgumentResolvers() != null) {
  685 | 			resolvers.addAll(getCustomArgumentResolvers());
  686 | 		}
  687 | 
  688 | 		// Catch-all
  689 | 		resolvers.add(new PrincipalMethodArgumentResolver());
  690 | 		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
  691 | 		resolvers.add(new ServletModelAttributeMethodProcessor(true));
  692 | 
  693 | 		return resolvers;
  694 | 	}
  695 | 
  696 | 	/**
  697 | 	 * Return the list of argument resolvers to use for {@code @InitBinder}
  698 | 	 * methods including built-in and custom resolvers.
  699 | 	 */
  700 | 	private List<HandlerMethodArgumentResolver> getDefaultInitBinderArgumentResolvers() {
  701 | 		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(20);
  702 | 
  703 | 		// Annotation-based argument resolution
  704 | 		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
  705 | 		resolvers.add(new RequestParamMapMethodArgumentResolver());
  706 | 		resolvers.add(new PathVariableMethodArgumentResolver());
  707 | 		resolvers.add(new PathVariableMapMethodArgumentResolver());
  708 | 		resolvers.add(new MatrixVariableMethodArgumentResolver());
  709 | 		resolvers.add(new MatrixVariableMapMethodArgumentResolver());
  710 | 		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
  711 | 		resolvers.add(new SessionAttributeMethodArgumentResolver());
  712 | 		resolvers.add(new RequestAttributeMethodArgumentResolver());
  713 | 
  714 | 		// Type-based argument resolution
  715 | 		resolvers.add(new ServletRequestMethodArgumentResolver());
  716 | 		resolvers.add(new ServletResponseMethodArgumentResolver());
  717 | 
  718 | 		// Custom arguments
  719 | 		if (getCustomArgumentResolvers() != null) {
  720 | 			resolvers.addAll(getCustomArgumentResolvers());
  721 | 		}
  722 | 
  723 | 		// Catch-all
  724 | 		resolvers.add(new PrincipalMethodArgumentResolver());
  725 | 		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
  726 | 
  727 | 		return resolvers;
  728 | 	}
  729 | 
  730 | 	/**
  731 | 	 * Return the list of return value handlers to use including built-in and
  732 | 	 * custom handlers provided via {@link #setReturnValueHandlers}.
  733 | 	 */
  734 | 	private List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
  735 | 		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(20);
  736 | 
  737 | 		ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler =
  738 | 				new ResponseBodyEmitterReturnValueHandler(getMessageConverters(),
  739 | 						this.reactiveAdapterRegistry, this.taskExecutor, this.contentNegotiationManager,
  740 | 						initViewResolvers(), initLocaleResolver());
  741 | 
  742 | 		HttpEntityMethodProcessor httpEntityMethodProcessor = new HttpEntityMethodProcessor(getMessageConverters(),
  743 | 				this.contentNegotiationManager, this.requestResponseBodyAdvice, this.errorResponseInterceptors);
  744 | 
  745 | 		// Single-purpose return value types
  746 | 		handlers.add(new ModelAndViewMethodReturnValueHandler());
  747 | 		handlers.add(new ModelMethodProcessor());
  748 | 		handlers.add(new ViewMethodReturnValueHandler());
  749 | 		handlers.add(responseBodyEmitterHandler);
  750 | 		handlers.add(new StreamingResponseBodyReturnValueHandler());
  751 | 		handlers.add(new ResponseEntityReturnValueHandler(httpEntityMethodProcessor, responseBodyEmitterHandler));
  752 | 		handlers.add(new HttpHeadersReturnValueHandler());
  753 | 		handlers.add(new CallableMethodReturnValueHandler());
  754 | 		handlers.add(new DeferredResultMethodReturnValueHandler());
  755 | 		handlers.add(new AsyncTaskMethodReturnValueHandler(this.beanFactory));
  756 | 
  757 | 		// Annotation-based return value types
  758 | 		handlers.add(new ServletModelAttributeMethodProcessor(false));
  759 | 		handlers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(),
  760 | 				this.contentNegotiationManager, this.requestResponseBodyAdvice, this.errorResponseInterceptors));
  761 | 
  762 | 		// Multi-purpose return value types
  763 | 		handlers.add(new ViewNameMethodReturnValueHandler());
  764 | 		handlers.add(new MapMethodProcessor());
  765 | 
  766 | 		// Custom return value types
  767 | 		if (getCustomReturnValueHandlers() != null) {
  768 | 			handlers.addAll(getCustomReturnValueHandlers());
  769 | 		}
  770 | 
  771 | 		// Catch-all
  772 | 		if (!CollectionUtils.isEmpty(getModelAndViewResolvers())) {
  773 | 			handlers.add(new ModelAndViewResolverMethodReturnValueHandler(getModelAndViewResolvers()));
  774 | 		}
  775 | 		else {
  776 | 			handlers.add(new ServletModelAttributeMethodProcessor(true));
  777 | 		}
  778 | 
  779 | 		return handlers;
  780 | 	}
  781 | 
  782 | 	private List<ViewResolver> initViewResolvers() {
  783 | 		if (getBeanFactory() instanceof ListableBeanFactory lbf) {
  784 | 			Map<String, ViewResolver> matchingBeans =
  785 | 					BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, ViewResolver.class, true, false);
  786 | 			if (!matchingBeans.isEmpty()) {
  787 | 				List<ViewResolver> viewResolvers = new ArrayList<>(matchingBeans.values());
  788 | 				AnnotationAwareOrderComparator.sort(viewResolvers);
  789 | 				return viewResolvers;
  790 | 			}
  791 | 		}
  792 | 		return Collections.emptyList();
  793 | 	}
  794 | 
  795 | 	private @Nullable LocaleResolver initLocaleResolver() {
  796 | 		if (getBeanFactory() != null) {
  797 | 			try {
  798 | 				return getBeanFactory().getBean(
  799 | 						DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
  800 | 			}
  801 | 			catch (NoSuchBeanDefinitionException ignored) {
  802 | 			}
  803 | 		}
  804 | 		return null;
  805 | 	}
  806 | 
  807 | 	private static Predicate<MethodParameter> methodParamPredicate(
  808 | 			List<HandlerMethodArgumentResolver> resolvers, Class<?> resolverType) {
  809 | 
  810 | 		return parameter -> {
  811 | 			for (HandlerMethodArgumentResolver resolver : resolvers) {
  812 | 				if (resolver.supportsParameter(parameter)) {
  813 | 					return resolverType.isInstance(resolver);
  814 | 				}
  815 | 			}
  816 | 			return false;
  817 | 		};
  818 | 	}
  819 | 
  820 | 
  821 | 	/**
  822 | 	 * Always return {@code true} since any method argument and return value
  823 | 	 * type will be processed in some way. A method argument not recognized
  824 | 	 * by any HandlerMethodArgumentResolver is interpreted as a request parameter
  825 | 	 * if it is a simple type, or as a model attribute otherwise. A return value
  826 | 	 * not recognized by any HandlerMethodReturnValueHandler will be interpreted
  827 | 	 * as a model attribute.
  828 | 	 */
  829 | 	@Override
  830 | 	protected boolean supportsInternal(HandlerMethod handlerMethod) {
  831 | 		return true;
  832 | 	}
  833 | 
  834 | 	@Override
  835 | 	protected @Nullable ModelAndView handleInternal(HttpServletRequest request,
  836 | 			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
  837 | 
  838 | 		ModelAndView mav;
  839 | 		checkRequest(request);
  840 | 
  841 | 		// Execute invokeHandlerMethod in synchronized block if required.
  842 | 		if (this.synchronizeOnSession) {
  843 | 			HttpSession session = request.getSession(false);
  844 | 			if (session != null) {
  845 | 				Object mutex = WebUtils.getSessionMutex(session);
  846 | 				synchronized (mutex) {
  847 | 					mav = invokeHandlerMethod(request, response, handlerMethod);
  848 | 				}
  849 | 			}
  850 | 			else {
  851 | 				// No HttpSession available -> no mutex necessary
  852 | 				mav = invokeHandlerMethod(request, response, handlerMethod);
  853 | 			}
  854 | 		}
  855 | 		else {
  856 | 			// No synchronization on session demanded at all...
  857 | 			mav = invokeHandlerMethod(request, response, handlerMethod);
  858 | 		}
  859 | 
  860 | 		if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
  861 | 			if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
  862 | 				applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
  863 | 			}
  864 | 			else {
  865 | 				prepareResponse(response);
  866 | 			}
  867 | 		}
  868 | 
  869 | 		return mav;
  870 | 	}
  871 | 
  872 | 
  873 | 	/**
  874 | 	 * Return the {@link SessionAttributesHandler} instance for the given handler type
  875 | 	 * (never {@code null}).
  876 | 	 */
  877 | 	private SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
  878 | 		return this.sessionAttributesHandlerCache.computeIfAbsent(
  879 | 				handlerMethod.getBeanType(),
  880 | 				type -> new SessionAttributesHandler(type, this.sessionAttributeStore));
  881 | 	}
  882 | 
  883 | 	/**
  884 | 	 * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView}
  885 | 	 * if view resolution is required.
  886 | 	 * @since 4.2
  887 | 	 * @see #createInvocableHandlerMethod(HandlerMethod)
  888 | 	 */
  889 | 	protected @Nullable ModelAndView invokeHandlerMethod(HttpServletRequest request,
  890 | 			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
  891 | 
  892 | 		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
  893 | 		AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
  894 | 		asyncWebRequest.setTimeout(this.asyncRequestTimeout);
  895 | 
  896 | 		asyncManager.setTaskExecutor(this.taskExecutor);
  897 | 		asyncManager.setAsyncWebRequest(asyncWebRequest);
  898 | 		asyncManager.registerCallableInterceptors(this.callableInterceptors);
  899 | 		asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);
  900 | 
  901 | 		// Obtain wrapped response to enforce lifecycle rule from Servlet spec, section 2.3.3.4
  902 | 		response = asyncWebRequest.getNativeResponse(HttpServletResponse.class);
  903 | 
  904 | 		ServletWebRequest webRequest = (asyncWebRequest instanceof ServletWebRequest servletWebRequest ?
  905 | 				servletWebRequest : new ServletWebRequest(request, response));
  906 | 
  907 | 		WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
  908 | 		ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);
  909 | 
  910 | 		ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
  911 | 		if (this.argumentResolvers != null) {
  912 | 			invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
  913 | 		}
  914 | 		if (this.returnValueHandlers != null) {
  915 | 			invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
  916 | 		}
  917 | 		invocableMethod.setDataBinderFactory(binderFactory);
  918 | 		invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
  919 | 		invocableMethod.setMethodValidator(this.methodValidator);
  920 | 
  921 | 		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
  922 | 		mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
  923 | 		modelFactory.initModel(webRequest, mavContainer, invocableMethod);
  924 | 
  925 | 		if (asyncManager.hasConcurrentResult()) {
  926 | 			Object result = asyncManager.getConcurrentResult();
  927 | 			Object[] resultContext = asyncManager.getConcurrentResultContext();
  928 | 			Assert.state(resultContext != null && resultContext.length > 0, "Missing result context");
  929 | 			mavContainer = (ModelAndViewContainer) resultContext[0];
  930 | 			asyncManager.clearConcurrentResult();
  931 | 			LogFormatUtils.traceDebug(logger, traceOn -> {
  932 | 				String formatted = LogFormatUtils.formatValue(result, !traceOn);
  933 | 				return "Resume with async result [" + formatted + "]";
  934 | 			});
  935 | 			invocableMethod = invocableMethod.wrapConcurrentResult(result);
  936 | 		}
  937 | 
  938 | 		invocableMethod.invokeAndHandle(webRequest, mavContainer);
  939 | 		if (asyncManager.isConcurrentHandlingStarted()) {
  940 | 			return null;
  941 | 		}
  942 | 
  943 | 		return getModelAndView(mavContainer, modelFactory, webRequest);
  944 | 	}
  945 | 
  946 | 	/**
  947 | 	 * Create a {@link ServletInvocableHandlerMethod} from the given {@link HandlerMethod} definition.
  948 | 	 * @param handlerMethod the {@link HandlerMethod} definition
  949 | 	 * @return the corresponding {@link ServletInvocableHandlerMethod} (or custom subclass thereof)
  950 | 	 * @since 4.2
  951 | 	 */
  952 | 	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
  953 | 		return new ServletInvocableHandlerMethod(handlerMethod);
  954 | 	}
  955 | 
  956 | 	private ModelFactory getModelFactory(HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
  957 | 		SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
  958 | 		Class<?> handlerType = handlerMethod.getBeanType();
  959 | 		Set<Method> methods = this.modelAttributeCache.get(handlerType);
  960 | 		if (methods == null) {
  961 | 			methods = MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
  962 | 			this.modelAttributeCache.put(handlerType, methods);
  963 | 		}
  964 | 		List<InvocableHandlerMethod> attrMethods = new ArrayList<>();
  965 | 		// Global methods first
  966 | 		this.modelAttributeAdviceCache.forEach((controllerAdviceBean, methodSet) -> {
  967 | 			if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
  968 | 				Object bean = controllerAdviceBean.resolveBean();
  969 | 				for (Method method : methodSet) {
  970 | 					attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
  971 | 				}
  972 | 			}
  973 | 		});
  974 | 		for (Method method : methods) {
  975 | 			Object bean = handlerMethod.getBean();
  976 | 			attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
  977 | 		}
  978 | 		return new ModelFactory(attrMethods, binderFactory, sessionAttrHandler);
  979 | 	}
  980 | 
  981 | 	private InvocableHandlerMethod createModelAttributeMethod(WebDataBinderFactory factory, Object bean, Method method) {
  982 | 		InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(bean, method);
  983 | 		if (this.argumentResolvers != null) {
  984 | 			attrMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
  985 | 		}
  986 | 		attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
  987 | 		attrMethod.setDataBinderFactory(factory);
  988 | 		return attrMethod;
  989 | 	}
  990 | 
  991 | 	private WebDataBinderFactory getDataBinderFactory(HandlerMethod handlerMethod) throws Exception {
  992 | 		Class<?> handlerType = handlerMethod.getBeanType();
  993 | 		Set<Method> methods = this.initBinderCache.get(handlerType);
  994 | 		if (methods == null) {
  995 | 			methods = MethodIntrospector.selectMethods(handlerType, INIT_BINDER_METHODS);
  996 | 			this.initBinderCache.put(handlerType, methods);
  997 | 		}
  998 | 		List<InvocableHandlerMethod> initBinderMethods = new ArrayList<>();
  999 | 		// Global methods first
 1000 | 		this.initBinderAdviceCache.forEach((controllerAdviceBean, methodSet) -> {
 1001 | 			if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
 1002 | 				Object bean = controllerAdviceBean.resolveBean();
 1003 | 				for (Method method : methodSet) {
 1004 | 					initBinderMethods.add(createInitBinderMethod(bean, method));
 1005 | 				}
 1006 | 			}
 1007 | 		});
 1008 | 		for (Method method : methods) {
 1009 | 			Object bean = handlerMethod.getBean();
 1010 | 			initBinderMethods.add(createInitBinderMethod(bean, method));
 1011 | 		}
 1012 | 		DefaultDataBinderFactory factory = createDataBinderFactory(initBinderMethods);
 1013 | 		factory.setMethodValidationApplicable(this.methodValidator != null && handlerMethod.shouldValidateArguments());
 1014 | 		return factory;
 1015 | 	}
 1016 | 
 1017 | 	private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
 1018 | 		InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
 1019 | 		if (this.initBinderArgumentResolvers != null) {
 1020 | 			binderMethod.setHandlerMethodArgumentResolvers(this.initBinderArgumentResolvers);
 1021 | 		}
 1022 | 		binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
 1023 | 		binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
 1024 | 		return binderMethod;
 1025 | 	}
 1026 | 
 1027 | 	/**
 1028 | 	 * Template method to create a new InitBinderDataBinderFactory instance.
 1029 | 	 * <p>The default implementation creates a ServletRequestDataBinderFactory.
 1030 | 	 * This can be overridden for custom ServletRequestDataBinder subclasses.
 1031 | 	 * @param binderMethods {@code @InitBinder} methods
 1032 | 	 * @return the InitBinderDataBinderFactory instance to use
 1033 | 	 * @throws Exception in case of invalid state or arguments
 1034 | 	 */
 1035 | 	protected InitBinderDataBinderFactory createDataBinderFactory(List<InvocableHandlerMethod> binderMethods)
 1036 | 			throws Exception {
 1037 | 
 1038 | 		return new ServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
 1039 | 	}
 1040 | 
 1041 | 	private @Nullable ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
 1042 | 			ModelFactory modelFactory, NativeWebRequest webRequest) throws Exception {
 1043 | 
 1044 | 		modelFactory.updateModel(webRequest, mavContainer);
 1045 | 		if (mavContainer.isRequestHandled()) {
 1046 | 			return null;
 1047 | 		}
 1048 | 		ModelMap model = mavContainer.getModel();
 1049 | 		ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, mavContainer.getStatus());
 1050 | 		if (!mavContainer.isViewReference()) {
 1051 | 			mav.setView((View) mavContainer.getView());
 1052 | 		}
 1053 | 		if (model instanceof RedirectAttributes redirectAttributes) {
 1054 | 			Map<String, ?> flashAttributes = redirectAttributes.getFlashAttributes();
 1055 | 			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
 1056 | 			if (request != null) {
 1057 | 				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
 1058 | 			}
 1059 | 		}
 1060 | 		return mav;
 1061 | 	}
 1062 | 
 1063 | 
 1064 | 	/**
 1065 | 	 * A default Spring MVC AsyncTaskExecutor that warns if used.
 1066 | 	 */
 1067 | 	@SuppressWarnings("serial")
 1068 | 	private class MvcSimpleAsyncTaskExecutor extends SimpleAsyncTaskExecutor {
 1069 | 
 1070 | 		private static boolean taskExecutorWarning = true;
 1071 | 
 1072 | 		MvcSimpleAsyncTaskExecutor() {
 1073 | 			super("MvcAsync");
 1074 | 		}
 1075 | 
 1076 | 		@Override
 1077 | 		public void execute(Runnable task) {
 1078 | 			if (taskExecutorWarning && logger.isWarnEnabled()) {
 1079 | 				synchronized (this) {
 1080 | 					if (taskExecutorWarning) {
 1081 | 						logger.warn("""
 1082 | 								!!!
 1083 | 								Performing asynchronous handling through the default Spring MVC SimpleAsyncTaskExecutor.
 1084 | 								This executor is not suitable for production use under load.
 1085 | 								Please, configure an AsyncTaskExecutor through the WebMvc config.
 1086 | 								-------------------------------
 1087 | 								!!!""");
 1088 | 						taskExecutorWarning = false;
 1089 | 					}
 1090 | 				}
 1091 | 			}
 1092 | 			super.execute(task);
 1093 | 		}
 1094 | 	}
 1095 | 
 1096 | }
```
