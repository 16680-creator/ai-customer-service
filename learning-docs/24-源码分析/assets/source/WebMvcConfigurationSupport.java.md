<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/config/annotation/WebMvcConfigurationSupport.java -->
<!-- 正文引用：[[源码:WebMvcConfigurationSupport.java#L起-L止]] ；代码块头部：(src: WebMvcConfigurationSupport.java#L起-L止) -->

# WebMvcConfigurationSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/config/annotation/WebMvcConfigurationSupport.java` |
| 行数 | 1227 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.398Z |

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
   17 | package org.springframework.web.servlet.config.annotation;
   18 | 
   19 | import java.nio.charset.StandardCharsets;
   20 | import java.util.ArrayList;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Locale;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | import java.util.stream.Collectors;
   27 | 
   28 | import jakarta.servlet.ServletContext;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.beans.factory.BeanFactoryUtils;
   32 | import org.springframework.beans.factory.BeanInitializationException;
   33 | import org.springframework.beans.factory.annotation.Qualifier;
   34 | import org.springframework.context.ApplicationContext;
   35 | import org.springframework.context.ApplicationContextAware;
   36 | import org.springframework.context.annotation.Bean;
   37 | import org.springframework.context.annotation.Configuration;
   38 | import org.springframework.context.annotation.Lazy;
   39 | import org.springframework.core.KotlinDetector;
   40 | import org.springframework.core.convert.converter.Converter;
   41 | import org.springframework.format.Formatter;
   42 | import org.springframework.format.FormatterRegistry;
   43 | import org.springframework.format.support.DefaultFormattingConversionService;
   44 | import org.springframework.format.support.FormattingConversionService;
   45 | import org.springframework.http.MediaType;
   46 | import org.springframework.http.converter.HttpMessageConverter;
   47 | import org.springframework.http.converter.HttpMessageConverters;
   48 | import org.springframework.util.AntPathMatcher;
   49 | import org.springframework.util.Assert;
   50 | import org.springframework.util.ClassUtils;
   51 | import org.springframework.util.PathMatcher;
   52 | import org.springframework.validation.Errors;
   53 | import org.springframework.validation.MessageCodesResolver;
   54 | import org.springframework.validation.Validator;
   55 | import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
   56 | import org.springframework.web.ErrorResponse;
   57 | import org.springframework.web.HttpRequestHandler;
   58 | import org.springframework.web.accept.ApiVersionStrategy;
   59 | import org.springframework.web.accept.ContentNegotiationManager;
   60 | import org.springframework.web.bind.WebDataBinder;
   61 | import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
   62 | import org.springframework.web.context.ServletContextAware;
   63 | import org.springframework.web.cors.CorsConfiguration;
   64 | import org.springframework.web.cors.PreFlightRequestHandler;
   65 | import org.springframework.web.method.support.CompositeUriComponentsContributor;
   66 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   67 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   68 | import org.springframework.web.servlet.FlashMapManager;
   69 | import org.springframework.web.servlet.HandlerAdapter;
   70 | import org.springframework.web.servlet.HandlerExceptionResolver;
   71 | import org.springframework.web.servlet.HandlerMapping;
   72 | import org.springframework.web.servlet.LocaleResolver;
   73 | import org.springframework.web.servlet.RequestToViewNameTranslator;
   74 | import org.springframework.web.servlet.ViewResolver;
   75 | import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
   76 | import org.springframework.web.servlet.function.support.RouterFunctionMapping;
   77 | import org.springframework.web.servlet.handler.AbstractHandlerMapping;
   78 | import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
   79 | import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
   80 | import org.springframework.web.servlet.handler.DefaultPreFlightRequestHandler;
   81 | import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
   82 | import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
   83 | import org.springframework.web.servlet.mvc.Controller;
   84 | import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
   85 | import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
   86 | import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
   87 | import org.springframework.web.servlet.mvc.method.annotation.ContextClassRequestBodyAdvice;
   88 | import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
   89 | import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
   90 | import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
   91 | import org.springframework.web.servlet.mvc.method.annotation.KotlinRequestBodyAdvice;
   92 | import org.springframework.web.servlet.mvc.method.annotation.KotlinResponseBodyAdvice;
   93 | import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
   94 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
   95 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
   96 | import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
   97 | import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
   98 | import org.springframework.web.servlet.resource.ResourceUrlProvider;
   99 | import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
  100 | import org.springframework.web.servlet.support.SessionFlashMapManager;
  101 | import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
  102 | import org.springframework.web.servlet.view.InternalResourceViewResolver;
  103 | import org.springframework.web.servlet.view.ViewResolverComposite;
  104 | import org.springframework.web.util.UrlPathHelper;
  105 | import org.springframework.web.util.pattern.PathPatternParser;
  106 | 
  107 | /**
  108 |  * This is the main class providing the configuration behind the MVC Java config.
  109 |  * It is typically imported by adding {@link EnableWebMvc @EnableWebMvc} to an
  110 |  * application {@link Configuration @Configuration} class. An alternative more
  111 |  * advanced option is to extend directly from this class and override methods as
  112 |  * necessary, remembering to add {@link Configuration @Configuration} to the
  113 |  * subclass and {@link Bean @Bean} to overridden {@link Bean @Bean} methods.
  114 |  * For more details see the javadoc of {@link EnableWebMvc @EnableWebMvc}.
  115 |  *
  116 |  * <p>This class registers the following {@link HandlerMapping HandlerMappings}:</p>
  117 |  * <ul>
  118 |  * <li>{@link RouterFunctionMapping}
  119 |  * ordered at -1 to map {@linkplain org.springframework.web.servlet.function.RouterFunction router functions}.
  120 |  * <li>{@link RequestMappingHandlerMapping}
  121 |  * ordered at 0 for mapping requests to annotated controller methods.
  122 |  * <li>{@link HandlerMapping}
  123 |  * ordered at 1 to map URL paths directly to view names.
  124 |  * <li>{@link BeanNameUrlHandlerMapping}
  125 |  * ordered at 2 to map URL paths to controller bean names.
  126 |  * <li>{@link HandlerMapping}
  127 |  * ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
  128 |  * <li>{@link HandlerMapping}
  129 |  * ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
  130 |  * </ul>
  131 |  *
  132 |  * <p>Registers these {@link HandlerAdapter HandlerAdapters}:
  133 |  * <ul>
  134 |  * <li>{@link RequestMappingHandlerAdapter}
  135 |  * for processing requests with annotated controller methods.
  136 |  * <li>{@link HttpRequestHandlerAdapter}
  137 |  * for processing requests with {@link HttpRequestHandler HttpRequestHandlers}.
  138 |  * <li>{@link SimpleControllerHandlerAdapter}
  139 |  * for processing requests with interface-based {@link Controller Controllers}.
  140 |  * <li>{@link HandlerFunctionAdapter}
  141 |  * for processing requests with {@linkplain org.springframework.web.servlet.function.RouterFunction router functions}.
  142 |  * </ul>
  143 |  *
  144 |  * <p>Registers a {@link HandlerExceptionResolverComposite} with this chain of
  145 |  * exception resolvers:
  146 |  * <ul>
  147 |  * <li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through
  148 |  * {@link org.springframework.web.bind.annotation.ExceptionHandler} methods.
  149 |  * <li>{@link ResponseStatusExceptionResolver} for exceptions annotated with
  150 |  * {@link org.springframework.web.bind.annotation.ResponseStatus}.
  151 |  * <li>{@link DefaultHandlerExceptionResolver} for resolving known Spring
  152 |  * exception types
  153 |  * </ul>
  154 |  *
  155 |  * <p>Registers an {@link AntPathMatcher} and a {@link UrlPathHelper}
  156 |  * to be used by:
  157 |  * <ul>
  158 |  * <li>the {@link RequestMappingHandlerMapping},
  159 |  * <li>the {@link HandlerMapping} for ViewControllers
  160 |  * <li>and the {@link HandlerMapping} for serving resources
  161 |  * </ul>
  162 |  * Note that those beans can be configured with a {@link PathMatchConfigurer}.
  163 |  *
  164 |  * <p>Both the {@link RequestMappingHandlerAdapter} and the
  165 |  * {@link ExceptionHandlerExceptionResolver} are configured with default
  166 |  * instances of the following by default:
  167 |  * <ul>
  168 |  * <li>a {@link ContentNegotiationManager}
  169 |  * <li>a {@link DefaultFormattingConversionService}
  170 |  * <li>an {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}
  171 |  * if a JSR-303 implementation is available on the classpath
  172 |  * <li>a range of {@link HttpMessageConverter HttpMessageConverters} depending on the third-party
  173 |  * libraries available on the classpath.
  174 |  * </ul>
  175 |  *
  176 |  * @author Rossen Stoyanchev
  177 |  * @author Brian Clozel
  178 |  * @author Sebastien Deleuze
  179 |  * @author Hyoungjune Kim
  180 |  * @since 3.1
  181 |  * @see EnableWebMvc
  182 |  * @see WebMvcConfigurer
  183 |  */
  184 | public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {
  185 | 
  186 | 	private static final boolean JACKSON_PRESENT;
  187 | 
  188 | 	private static final boolean JACKSON_2_PRESENT;
  189 | 
  190 | 	private static final boolean KOTLIN_REFLECT_PRESENT;
  191 | 
  192 | 	private static final boolean KOTLIN_SERIALIZATION_PRESENT;
  193 | 
  194 | 
  195 | 	static {
  196 | 		ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
  197 | 		JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
  198 | 		JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
  199 | 				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
  200 | 		KOTLIN_REFLECT_PRESENT = KotlinDetector.isKotlinReflectPresent();
  201 | 		KOTLIN_SERIALIZATION_PRESENT = ClassUtils.isPresent("kotlinx.serialization.Serializable", classLoader);
  202 | 	}
  203 | 
  204 | 
  205 | 	private @Nullable ApplicationContext applicationContext;
  206 | 
  207 | 	private @Nullable ServletContext servletContext;
  208 | 
  209 | 	private @Nullable List<Object> interceptors;
  210 | 
  211 | 	private @Nullable PathMatchConfigurer pathMatchConfigurer;
  212 | 
  213 | 	private @Nullable ContentNegotiationManager contentNegotiationManager;
  214 | 
  215 | 	private @Nullable ApiVersionStrategy apiVersionStrategy;
  216 | 
  217 | 	private @Nullable List<HandlerMethodArgumentResolver> argumentResolvers;
  218 | 
  219 | 	private @Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers;
  220 | 
  221 | 	private @Nullable List<HttpMessageConverter<?>> messageConverters;
  222 | 
  223 | 	private @Nullable List<ErrorResponse.Interceptor> errorResponseInterceptors;
  224 | 
  225 | 	private @Nullable Map<String, CorsConfiguration> corsConfigurations;
  226 | 
  227 | 	private @Nullable AsyncSupportConfigurer asyncSupportConfigurer;
  228 | 
  229 | 	private @Nullable ProblemDetailsConfigurer problemDetailsConfigurer;
  230 | 
  231 | 
  232 | 	/**
  233 | 	 * Set the Spring {@link ApplicationContext}, for example, for resource loading.
  234 | 	 */
  235 | 	@Override
  236 | 	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
  237 | 		this.applicationContext = applicationContext;
  238 | 	}
  239 | 
  240 | 	/**
  241 | 	 * Return the associated Spring {@link ApplicationContext}.
  242 | 	 * @since 4.2
  243 | 	 */
  244 | 	public final @Nullable ApplicationContext getApplicationContext() {
  245 | 		return this.applicationContext;
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Set the {@link jakarta.servlet.ServletContext}, for example, for resource handling,
  250 | 	 * looking up file extensions, etc.
  251 | 	 */
  252 | 	@Override
  253 | 	public void setServletContext(@Nullable ServletContext servletContext) {
  254 | 		this.servletContext = servletContext;
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * Return the associated {@link jakarta.servlet.ServletContext}.
  259 | 	 * @since 4.2
  260 | 	 */
  261 | 	public final @Nullable ServletContext getServletContext() {
  262 | 		return this.servletContext;
  263 | 	}
  264 | 
  265 | 
  266 | 	/**
  267 | 	 * Return a {@link RequestMappingHandlerMapping} ordered at 0 for mapping
  268 | 	 * requests to annotated controllers.
  269 | 	 */
  270 | 	@Bean
  271 | 	public RequestMappingHandlerMapping requestMappingHandlerMapping(
  272 | 			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
  273 | 			@Qualifier("mvcApiVersionStrategy") @Nullable ApiVersionStrategy apiVersionStrategy,
  274 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  275 | 			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
  276 | 
  277 | 		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
  278 | 		mapping.setOrder(0);
  279 | 		mapping.setContentNegotiationManager(contentNegotiationManager);
  280 | 		mapping.setApiVersionStrategy(apiVersionStrategy);
  281 | 
  282 | 		initHandlerMapping(mapping, conversionService, resourceUrlProvider);
  283 | 
  284 | 		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
  285 | 		if (pathConfig.getPathPrefixes() != null) {
  286 | 			mapping.setPathPrefixes(pathConfig.getPathPrefixes());
  287 | 		}
  288 | 
  289 | 		return mapping;
  290 | 	}
  291 | 
  292 | 	/**
  293 | 	 * Protected method for plugging in a custom subclass of
  294 | 	 * {@link RequestMappingHandlerMapping}.
  295 | 	 * @since 4.0
  296 | 	 */
  297 | 	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
  298 | 		return new RequestMappingHandlerMapping();
  299 | 	}
  300 | 
  301 | 	/**
  302 | 	 * Provide access to the shared handler interceptors used to configure
  303 | 	 * {@link HandlerMapping} instances with.
  304 | 	 * <p>This method cannot be overridden; use {@link #addInterceptors} instead.
  305 | 	 */
  306 | 	protected final Object[] getInterceptors(
  307 | 			FormattingConversionService mvcConversionService,
  308 | 			ResourceUrlProvider mvcResourceUrlProvider) {
  309 | 
  310 | 		if (this.interceptors == null) {
  311 | 			InterceptorRegistry registry = new InterceptorRegistry();
  312 | 			addInterceptors(registry);
  313 | 			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));
  314 | 			registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));
  315 | 			this.interceptors = registry.getInterceptors();
  316 | 		}
  317 | 		return this.interceptors.toArray();
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Override this method to add Spring MVC interceptors for
  322 | 	 * pre- and post-processing of controller invocation.
  323 | 	 * @see InterceptorRegistry
  324 | 	 */
  325 | 	protected void addInterceptors(InterceptorRegistry registry) {
  326 | 	}
  327 | 
  328 | 	/**
  329 | 	 * Callback for building the {@link PathMatchConfigurer}.
  330 | 	 * Delegates to {@link #configurePathMatch}.
  331 | 	 * @since 4.1
  332 | 	 */
  333 | 	protected PathMatchConfigurer getPathMatchConfigurer() {
  334 | 		if (this.pathMatchConfigurer == null) {
  335 | 			this.pathMatchConfigurer = new PathMatchConfigurer();
  336 | 			configurePathMatch(this.pathMatchConfigurer);
  337 | 		}
  338 | 		return this.pathMatchConfigurer;
  339 | 	}
  340 | 
  341 | 	/**
  342 | 	 * Override this method to configure path matching options.
  343 | 	 * @since 4.0.3
  344 | 	 * @see PathMatchConfigurer
  345 | 	 */
  346 | 	protected void configurePathMatch(PathMatchConfigurer configurer) {
  347 | 	}
  348 | 
  349 | 	/**
  350 | 	 * Return a global {@link PathPatternParser} instance to use for parsing
  351 | 	 * patterns to match to the {@link org.springframework.http.server.RequestPath}.
  352 | 	 * The returned instance can be configured using
  353 | 	 * {@link #configurePathMatch(PathMatchConfigurer)}.
  354 | 	 * @since 5.3.4
  355 | 	 */
  356 | 	@Bean
  357 | 	public PathPatternParser mvcPatternParser() {
  358 | 		return getPathMatchConfigurer().getPatternParserOrDefault();
  359 | 	}
  360 | 
  361 | 	/**
  362 | 	 * Return a global {@link UrlPathHelper} instance which is used to resolve
  363 | 	 * the request mapping path for an application. The instance can be
  364 | 	 * configured via {@link #configurePathMatch(PathMatchConfigurer)}.
  365 | 	 * <p><b>Note:</b> This is only used when parsed patterns are not
  366 | 	 * {@link PathMatchConfigurer#setPatternParser enabled}.
  367 | 	 * @since 4.1
  368 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  369 | 	 * for use at runtime in web modules in favor of parsed patterns with
  370 | 	 * {@link PathPatternParser}.
  371 | 	 */
  372 | 	@SuppressWarnings("removal")
  373 | 	@Deprecated(since = "7.0", forRemoval = true)
  374 | 	@Bean
  375 | 	public UrlPathHelper mvcUrlPathHelper() {
  376 | 		return getPathMatchConfigurer().getUrlPathHelperOrDefault();
  377 | 	}
  378 | 
  379 | 	/**
  380 | 	 * Return a global {@link PathMatcher} instance which is used for URL path
  381 | 	 * matching with String patterns. The returned instance can be configured
  382 | 	 * using {@link #configurePathMatch(PathMatchConfigurer)}.
  383 | 	 * <p><b>Note:</b> This is only used when parsed patterns are not
  384 | 	 * {@link PathMatchConfigurer#setPatternParser enabled}.
  385 | 	 * @since 4.1
  386 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  387 | 	 * for use at runtime in web modules in favor of parsed patterns with
  388 | 	 * {@link PathPatternParser}.
  389 | 	 */
  390 | 	@SuppressWarnings("removal")
  391 | 	@Deprecated(since = "7.0", forRemoval = true)
  392 | 	@Bean
  393 | 	public PathMatcher mvcPathMatcher() {
  394 | 		return getPathMatchConfigurer().getPathMatcherOrDefault();
  395 | 	}
  396 | 
  397 | 	/**
  398 | 	 * Return a {@link ContentNegotiationManager} instance to use to determine
  399 | 	 * requested {@linkplain MediaType media types} in a given request.
  400 | 	 */
  401 | 	@Bean
  402 | 	public ContentNegotiationManager mvcContentNegotiationManager() {
  403 | 		if (this.contentNegotiationManager == null) {
  404 | 			ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer();
  405 | 			configurer.mediaTypes(getDefaultMediaTypes());
  406 | 			configureContentNegotiation(configurer);
  407 | 			this.contentNegotiationManager = configurer.buildContentNegotiationManager();
  408 | 		}
  409 | 		return this.contentNegotiationManager;
  410 | 	}
  411 | 
  412 | 	protected Map<String, MediaType> getDefaultMediaTypes() {
  413 | 		Map<String, MediaType> map = new HashMap<>(4);
  414 | 		List<HttpMessageConverter<?>> messageConverters = getMessageConverters();
  415 | 		Set<MediaType> supportedMediaTypes = messageConverters.stream()
  416 | 				.flatMap(converter -> converter.getSupportedMediaTypes().stream())
  417 | 				.collect(Collectors.toSet());
  418 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_ATOM_XML)) {
  419 | 			map.put("atom", MediaType.APPLICATION_ATOM_XML);
  420 | 		}
  421 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_RSS_XML)) {
  422 | 			map.put("rss", MediaType.APPLICATION_RSS_XML);
  423 | 		}
  424 | 		MediaType xmlUtf8MediaType = new MediaType("application", "xml", StandardCharsets.UTF_8);
  425 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_XML) ||
  426 | 				supportedMediaTypes.contains(xmlUtf8MediaType)) {
  427 | 			map.put("xml", MediaType.APPLICATION_XML);
  428 | 		}
  429 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
  430 | 			map.put("json", MediaType.APPLICATION_JSON);
  431 | 		}
  432 | 		MediaType smileMediaType = new MediaType("application", "x-jackson-smile");
  433 | 		if (supportedMediaTypes.contains(smileMediaType)) {
  434 | 			map.put("smile", smileMediaType);
  435 | 		}
  436 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_CBOR)) {
  437 | 			map.put("cbor", MediaType.APPLICATION_CBOR);
  438 | 		}
  439 | 		if (supportedMediaTypes.contains(MediaType.APPLICATION_ATOM_XML)) {
  440 | 			map.put("yaml", MediaType.APPLICATION_YAML);
  441 | 		}
  442 | 		return map;
  443 | 	}
  444 | 
  445 | 	/**
  446 | 	 * Override this method to configure content negotiation.
  447 | 	 * @see DefaultServletHandlerConfigurer
  448 | 	 */
  449 | 	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
  450 | 	}
  451 | 
  452 | 	/**
  453 | 	 * Return the central strategy to manage API versioning with, or {@code null}
  454 | 	 * if the application does not use versioning.
  455 | 	 * @since 7.0
  456 | 	 */
  457 | 	@Bean
  458 | 	public @Nullable ApiVersionStrategy mvcApiVersionStrategy() {
  459 | 		if (this.apiVersionStrategy == null) {
  460 | 			ApiVersionConfigurer configurer = new ApiVersionConfigurer();
  461 | 			configureApiVersioning(configurer);
  462 | 			ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
  463 | 			if (strategy != null) {
  464 | 				this.apiVersionStrategy = strategy;
  465 | 			}
  466 | 		}
  467 | 		return this.apiVersionStrategy;
  468 | 	}
  469 | 
  470 | 	/**
  471 | 	 * Override this method to configure API versioning.
  472 | 	 * @since 7.0
  473 | 	 */
  474 | 	protected void configureApiVersioning(ApiVersionConfigurer configurer) {
  475 | 	}
  476 | 
  477 | 	/**
  478 | 	 * Return a handler mapping ordered at 1 to map URL paths directly to
  479 | 	 * view names. To configure view controllers, override
  480 | 	 * {@link #addViewControllers}.
  481 | 	 */
  482 | 	@Bean
  483 | 	public @Nullable HandlerMapping viewControllerHandlerMapping(
  484 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  485 | 			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
  486 | 
  487 | 		ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
  488 | 		addViewControllers(registry);
  489 | 
  490 | 		AbstractHandlerMapping mapping = registry.buildHandlerMapping();
  491 | 		initHandlerMapping(mapping, conversionService, resourceUrlProvider);
  492 | 		return mapping;
  493 | 	}
  494 | 
  495 | 	@SuppressWarnings("removal")
  496 | 	private void initHandlerMapping(
  497 | 			@Nullable AbstractHandlerMapping mapping, FormattingConversionService conversionService,
  498 | 			ResourceUrlProvider resourceUrlProvider) {
  499 | 
  500 | 		if (mapping == null) {
  501 | 			return;
  502 | 		}
  503 | 		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
  504 | 		if (pathConfig.preferPathMatcher()) {
  505 | 			mapping.setPatternParser(null);
  506 | 			mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());
  507 | 			mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());
  508 | 		}
  509 | 		else if (pathConfig.getPatternParser() != null) {
  510 | 			mapping.setPatternParser(pathConfig.getPatternParser());
  511 | 		}
  512 | 		// else: AbstractHandlerMapping defaults to PathPatternParser
  513 | 
  514 | 		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
  515 | 		mapping.setCorsConfigurations(getCorsConfigurations());
  516 | 	}
  517 | 
  518 | 	/**
  519 | 	 * Override this method to add view controllers.
  520 | 	 * @see ViewControllerRegistry
  521 | 	 */
  522 | 	protected void addViewControllers(ViewControllerRegistry registry) {
  523 | 	}
  524 | 
  525 | 	/**
  526 | 	 * Return a {@link BeanNameUrlHandlerMapping} ordered at 2 to map URL
  527 | 	 * paths to controller bean names.
  528 | 	 */
  529 | 	@Bean
  530 | 	public BeanNameUrlHandlerMapping beanNameHandlerMapping(
  531 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  532 | 			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
  533 | 
  534 | 		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
  535 | 		mapping.setOrder(2);
  536 | 		initHandlerMapping(mapping, conversionService, resourceUrlProvider);
  537 | 		return mapping;
  538 | 	}
  539 | 
  540 | 	/**
  541 | 	 * Return a {@link RouterFunctionMapping} ordered at -1 to map
  542 | 	 * {@linkplain org.springframework.web.servlet.function.RouterFunction router functions}.
  543 | 	 * Consider overriding one of these other more fine-grained methods:
  544 | 	 * <ul>
  545 | 	 * <li>{@link #addInterceptors} for adding handler interceptors.
  546 | 	 * <li>{@link #addCorsMappings} to configure cross origin requests processing.
  547 | 	 * <li>{@link #configureMessageConverters} for adding custom message converters.
  548 | 	 * <li>{@link #configurePathMatch(PathMatchConfigurer)} for customizing the {@link PathPatternParser}.
  549 | 	 * </ul>
  550 | 	 * @since 5.2
  551 | 	 */
  552 | 	@Bean
  553 | 	public RouterFunctionMapping routerFunctionMapping(
  554 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  555 | 			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider,
  556 | 			@Qualifier("mvcApiVersionStrategy") @Nullable ApiVersionStrategy versionStrategy) {
  557 | 
  558 | 		RouterFunctionMapping mapping = new RouterFunctionMapping();
  559 | 		mapping.setOrder(-1);  // go before RequestMappingHandlerMapping
  560 | 		mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
  561 | 		mapping.setCorsConfigurations(getCorsConfigurations());
  562 | 		mapping.setMessageConverters(getMessageConverters());
  563 | 		mapping.setApiVersionStrategy(versionStrategy);
  564 | 
  565 | 		PathPatternParser patternParser = getPathMatchConfigurer().getPatternParser();
  566 | 		if (patternParser != null) {
  567 | 			mapping.setPatternParser(patternParser);
  568 | 		}
  569 | 
  570 | 		return mapping;
  571 | 	}
  572 | 
  573 | 	/**
  574 | 	 * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
  575 | 	 * resource handlers. To configure resource handling, override
  576 | 	 * {@link #addResourceHandlers}.
  577 | 	 */
  578 | 	@SuppressWarnings("removal")
  579 | 	@Bean
  580 | 	public @Nullable HandlerMapping resourceHandlerMapping(
  581 | 			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
  582 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  583 | 			@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
  584 | 
  585 | 		Assert.state(this.applicationContext != null, "No ApplicationContext set");
  586 | 		Assert.state(this.servletContext != null, "No ServletContext set");
  587 | 
  588 | 		PathMatchConfigurer pathConfig = getPathMatchConfigurer();
  589 | 
  590 | 		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext,
  591 | 				this.servletContext, contentNegotiationManager, pathConfig.getUrlPathHelper());
  592 | 		addResourceHandlers(registry);
  593 | 
  594 | 		AbstractHandlerMapping mapping = registry.getHandlerMapping();
  595 | 		initHandlerMapping(mapping, conversionService, resourceUrlProvider);
  596 | 		return mapping;
  597 | 	}
  598 | 
  599 | 	/**
  600 | 	 * Override this method to add resource handlers for serving static resources.
  601 | 	 * @see ResourceHandlerRegistry
  602 | 	 */
  603 | 	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
  604 | 	}
  605 | 
  606 | 	/**
  607 | 	 * A {@link ResourceUrlProvider} bean for use with the MVC dispatcher.
  608 | 	 * @since 4.1
  609 | 	 */
  610 | 	@SuppressWarnings("removal")
  611 | 	@Bean
  612 | 	public ResourceUrlProvider mvcResourceUrlProvider() {
  613 | 		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
  614 | 		urlProvider.setUrlPathHelper(getPathMatchConfigurer().getUrlPathHelperOrDefault());
  615 | 		urlProvider.setPathMatcher(getPathMatchConfigurer().getPathMatcherOrDefault());
  616 | 		return urlProvider;
  617 | 	}
  618 | 
  619 | 	/**
  620 | 	 * Return a handler mapping ordered at Integer.MAX_VALUE with a mapped
  621 | 	 * default servlet handler. To configure "default" Servlet handling,
  622 | 	 * override {@link #configureDefaultServletHandling}.
  623 | 	 */
  624 | 	@Bean
  625 | 	public @Nullable HandlerMapping defaultServletHandlerMapping() {
  626 | 		Assert.state(this.servletContext != null, "No ServletContext set");
  627 | 		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(this.servletContext);
  628 | 		configureDefaultServletHandling(configurer);
  629 | 		return configurer.buildHandlerMapping();
  630 | 	}
  631 | 
  632 | 	/**
  633 | 	 * Override this method to configure "default" Servlet handling.
  634 | 	 * @see DefaultServletHandlerConfigurer
  635 | 	 */
  636 | 	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
  637 | 	}
  638 | 
  639 | 	/**
  640 | 	 * Returns a {@link RequestMappingHandlerAdapter} for processing requests
  641 | 	 * through annotated controller methods. Consider overriding one of these
  642 | 	 * other more fine-grained methods:
  643 | 	 * <ul>
  644 | 	 * <li>{@link #addArgumentResolvers} for adding custom argument resolvers.
  645 | 	 * <li>{@link #addReturnValueHandlers} for adding custom return value handlers.
  646 | 	 * <li>{@link #configureMessageConverters} for adding custom message converters.
  647 | 	 * </ul>
  648 | 	 */
  649 | 	@Bean
  650 | 	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
  651 | 			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
  652 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  653 | 			@Qualifier("mvcValidator") Validator validator) {
  654 | 
  655 | 		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
  656 | 		adapter.setContentNegotiationManager(contentNegotiationManager);
  657 | 		adapter.setMessageConverters(getMessageConverters());
  658 | 		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
  659 | 		adapter.setCustomArgumentResolvers(getArgumentResolvers());
  660 | 		adapter.setCustomReturnValueHandlers(getReturnValueHandlers());
  661 | 		adapter.setErrorResponseInterceptors(getErrorResponseInterceptors());
  662 | 
  663 | 		if (JACKSON_PRESENT || JACKSON_2_PRESENT || KOTLIN_SERIALIZATION_PRESENT) {
  664 | 			List<RequestBodyAdvice> requestBodyAdvices = new ArrayList<>(2);
  665 | 			List<ResponseBodyAdvice<?>> responseBodyAdvices = new ArrayList<>(2);
  666 | 			if (JACKSON_PRESENT || JACKSON_2_PRESENT) {
  667 | 				requestBodyAdvices.add(new JsonViewRequestBodyAdvice());
  668 | 				requestBodyAdvices.add(new ContextClassRequestBodyAdvice());
  669 | 				responseBodyAdvices.add(new JsonViewResponseBodyAdvice());
  670 | 			}
  671 | 			if (KOTLIN_REFLECT_PRESENT && KOTLIN_SERIALIZATION_PRESENT) {
  672 | 				requestBodyAdvices.add(new KotlinRequestBodyAdvice());
  673 | 				responseBodyAdvices.add(new KotlinResponseBodyAdvice());
  674 | 			}
  675 | 			adapter.setRequestBodyAdvice(requestBodyAdvices);
  676 | 			adapter.setResponseBodyAdvice(responseBodyAdvices);
  677 | 		}
  678 | 
  679 | 		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
  680 | 		if (configurer.getTaskExecutor() != null) {
  681 | 			adapter.setTaskExecutor(configurer.getTaskExecutor());
  682 | 		}
  683 | 		if (configurer.getTimeout() != null) {
  684 | 			adapter.setAsyncRequestTimeout(configurer.getTimeout());
  685 | 		}
  686 | 		adapter.setCallableInterceptors(configurer.getCallableInterceptors());
  687 | 		adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());
  688 | 
  689 | 		return adapter;
  690 | 	}
  691 | 
  692 | 	/**
  693 | 	 * Protected method for plugging in a custom subclass of
  694 | 	 * {@link RequestMappingHandlerAdapter}.
  695 | 	 * @since 4.3
  696 | 	 */
  697 | 	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
  698 | 		return new RequestMappingHandlerAdapter();
  699 | 	}
  700 | 
  701 | 	/**
  702 | 	 * Returns a {@link HandlerFunctionAdapter} for processing requests through
  703 | 	 * {@linkplain org.springframework.web.servlet.function.HandlerFunction handler functions}.
  704 | 	 * @since 5.2
  705 | 	 */
  706 | 	@Bean
  707 | 	public HandlerFunctionAdapter handlerFunctionAdapter() {
  708 | 		HandlerFunctionAdapter adapter = new HandlerFunctionAdapter();
  709 | 
  710 | 		AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();
  711 | 		if (configurer.getTimeout() != null) {
  712 | 			adapter.setAsyncRequestTimeout(configurer.getTimeout());
  713 | 		}
  714 | 		return adapter;
  715 | 	}
  716 | 
  717 | 	/**
  718 | 	 * Return the {@link ConfigurableWebBindingInitializer} to use for
  719 | 	 * initializing all {@link WebDataBinder} instances.
  720 | 	 */
  721 | 	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
  722 | 			FormattingConversionService mvcConversionService, Validator mvcValidator) {
  723 | 
  724 | 		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
  725 | 		initializer.setConversionService(mvcConversionService);
  726 | 		initializer.setValidator(mvcValidator);
  727 | 		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
  728 | 		if (messageCodesResolver != null) {
  729 | 			initializer.setMessageCodesResolver(messageCodesResolver);
  730 | 		}
  731 | 		return initializer;
  732 | 	}
  733 | 
  734 | 	/**
  735 | 	 * Override this method to provide a custom {@link MessageCodesResolver}.
  736 | 	 */
  737 | 	protected @Nullable MessageCodesResolver getMessageCodesResolver() {
  738 | 		return null;
  739 | 	}
  740 | 
  741 | 	/**
  742 | 	 * Return a {@link FormattingConversionService} for use with annotated controllers.
  743 | 	 * <p>See {@link #addFormatters} as an alternative to overriding this method.
  744 | 	 */
  745 | 	@Bean
  746 | 	public FormattingConversionService mvcConversionService() {
  747 | 		FormattingConversionService conversionService = new DefaultFormattingConversionService();
  748 | 		addFormatters(conversionService);
  749 | 		return conversionService;
  750 | 	}
  751 | 
  752 | 	/**
  753 | 	 * Override this method to add custom {@link Converter} and/or {@link Formatter}
  754 | 	 * delegates to the common {@link FormattingConversionService}.
  755 | 	 * @see #mvcConversionService()
  756 | 	 */
  757 | 	protected void addFormatters(FormatterRegistry registry) {
  758 | 	}
  759 | 
  760 | 	/**
  761 | 	 * Return a global {@link Validator} instance for example for validating
  762 | 	 * {@code @ModelAttribute} and {@code @RequestBody} method arguments.
  763 | 	 * Delegates to {@link #getValidator()} first and if that returns {@code null}
  764 | 	 * checks the classpath for the presence of a JSR-303 implementations
  765 | 	 * before creating a {@code OptionalValidatorFactoryBean}.If a JSR-303
  766 | 	 * implementation is not available, a no-op {@link Validator} is returned.
  767 | 	 */
  768 | 	@Bean
  769 | 	public Validator mvcValidator() {
  770 | 		Validator validator = getValidator();
  771 | 		if (validator == null) {
  772 | 			if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
  773 | 				try {
  774 | 					validator = new OptionalValidatorFactoryBean();
  775 | 				}
  776 | 				catch (Throwable ex) {
  777 | 					throw new BeanInitializationException("Failed to create default validator", ex);
  778 | 				}
  779 | 			}
  780 | 			else {
  781 | 				validator = new NoOpValidator();
  782 | 			}
  783 | 		}
  784 | 		return validator;
  785 | 	}
  786 | 
  787 | 	/**
  788 | 	 * Override this method to provide a custom {@link Validator}.
  789 | 	 */
  790 | 	protected @Nullable Validator getValidator() {
  791 | 		return null;
  792 | 	}
  793 | 
  794 | 	/**
  795 | 	 * Provide access to the shared custom argument resolvers used by the
  796 | 	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
  797 | 	 * <p>This method cannot be overridden; use {@link #addArgumentResolvers} instead.
  798 | 	 * @since 4.3
  799 | 	 */
  800 | 	protected final List<HandlerMethodArgumentResolver> getArgumentResolvers() {
  801 | 		if (this.argumentResolvers == null) {
  802 | 			this.argumentResolvers = new ArrayList<>();
  803 | 			addArgumentResolvers(this.argumentResolvers);
  804 | 		}
  805 | 		return this.argumentResolvers;
  806 | 	}
  807 | 
  808 | 	/**
  809 | 	 * Add custom {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
  810 | 	 * to use in addition to the ones registered by default.
  811 | 	 * <p>Custom argument resolvers are invoked before built-in resolvers except for
  812 | 	 * those that rely on the presence of annotations (for example, {@code @RequestParameter},
  813 | 	 * {@code @PathVariable}, etc). The latter can be customized by configuring the
  814 | 	 * {@link RequestMappingHandlerAdapter} directly.
  815 | 	 * @param argumentResolvers the list of custom converters (initially an empty list)
  816 | 	 */
  817 | 	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
  818 | 	}
  819 | 
  820 | 	/**
  821 | 	 * Provide access to the shared return value handlers used by the
  822 | 	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
  823 | 	 * <p>This method cannot be overridden; use {@link #addReturnValueHandlers} instead.
  824 | 	 * @since 4.3
  825 | 	 */
  826 | 	protected final List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
  827 | 		if (this.returnValueHandlers == null) {
  828 | 			this.returnValueHandlers = new ArrayList<>();
  829 | 			addReturnValueHandlers(this.returnValueHandlers);
  830 | 		}
  831 | 		return this.returnValueHandlers;
  832 | 	}
  833 | 
  834 | 	/**
  835 | 	 * Add custom {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}
  836 | 	 * in addition to the ones registered by default.
  837 | 	 * <p>Custom return value handlers are invoked before built-in ones except for
  838 | 	 * those that rely on the presence of annotations (for example, {@code @ResponseBody},
  839 | 	 * {@code @ModelAttribute}, etc). The latter can be customized by configuring the
  840 | 	 * {@link RequestMappingHandlerAdapter} directly.
  841 | 	 * @param returnValueHandlers the list of custom handlers (initially an empty list)
  842 | 	 */
  843 | 	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  844 | 	}
  845 | 
  846 | 	/**
  847 | 	 * Provides access to the shared {@link HttpMessageConverter HttpMessageConverters}
  848 | 	 * used by the {@link RequestMappingHandlerAdapter} and the
  849 | 	 * {@link ExceptionHandlerExceptionResolver}.
  850 | 	 * <p>This method cannot be overridden; use {@link #configureMessageConverters} instead.
  851 | 	 * Also see {@link #addDefaultHttpMessageConverters} for adding default message converters.
  852 | 	 */
  853 | 	protected final List<HttpMessageConverter<?>> getMessageConverters() {
  854 | 		if (this.messageConverters == null) {
  855 | 			this.messageConverters = new ArrayList<>();
  856 | 			configureMessageConverters(this.messageConverters);
  857 | 			if (this.messageConverters.isEmpty()) {
  858 | 				addDefaultHttpMessageConverters(this.messageConverters);
  859 | 			}
  860 | 			extendMessageConverters(this.messageConverters);
  861 | 		}
  862 | 		return this.messageConverters;
  863 | 	}
  864 | 
  865 | 
  866 | 	/**
  867 | 	 * Override this method to create a custom {@link HttpMessageConverters}.
  868 | 	 * Converters will be used with the
  869 | 	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
  870 | 	 * <p>By default, this will create an instance with default message converters registered,
  871 | 	 * if present on the classpath.
  872 | 	 * @since 7.0
  873 | 	 */
  874 | 	protected HttpMessageConverters createMessageConverters() {
  875 | 		HttpMessageConverters.ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
  876 | 		configureMessageConverters(serverBuilder);
  877 | 		return serverBuilder.build();
  878 | 	}
  879 | 
  880 | 	/**
  881 | 	 * Override this method to configure the message converters on the given
  882 | 	 * {@link HttpMessageConverters.Builder builder}.
  883 | 	 * @param builder the {@code HttpMessageConverters} builder to configure
  884 | 	 * @since 7.0
  885 | 	 */
  886 | 	protected void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
  887 | 	}
  888 | 
  889 | 	/**
  890 | 	 * Override this method to add custom {@link HttpMessageConverter message converters}
  891 | 	 * to use with the {@link RequestMappingHandlerAdapter} and the
  892 | 	 * {@link ExceptionHandlerExceptionResolver}.
  893 | 	 * <p>Adding converters to the list turns off the default converters that would
  894 | 	 * otherwise be registered by default. Also see {@link #addDefaultHttpMessageConverters}
  895 | 	 * for adding default message converters.
  896 | 	 * @param converters a list to add message converters to (initially an empty list)
  897 | 	 * @deprecated since 7.0 in favor of {@link #configureMessageConverters(HttpMessageConverters.ServerBuilder)}
  898 | 	 */
  899 | 	@Deprecated(since = "7.0", forRemoval = true)
  900 | 	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
  901 | 	}
  902 | 
  903 | 	/**
  904 | 	 * Override this method to extend or modify the list of converters after it has
  905 | 	 * been configured. This may be useful for example to allow default converters
  906 | 	 * to be registered and then insert a custom converter through this method.
  907 | 	 * @param converters the list of configured converters to extend
  908 | 	 * @since 4.1.3
  909 | 	 * @deprecated since 7.0 in favor of {@link #configureMessageConverters(HttpMessageConverters.ServerBuilder)}
  910 | 	 */
  911 | 	@Deprecated(since = "7.0", forRemoval = true)
  912 | 	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  913 | 	}
  914 | 
  915 | 	/**
  916 | 	 * Adds a set of default HttpMessageConverter instances to the given list.
  917 | 	 * Subclasses can call this method from {@link #configureMessageConverters}.
  918 | 	 * @param messageConverters the list to add the default message converters to
  919 | 	 * @deprecated since 7.0 in favor of {@link #createMessageConverters()}
  920 | 	 */
  921 | 	@Deprecated(since = "7.0", forRemoval = true)
  922 | 	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
  923 | 		HttpMessageConverters converters = createMessageConverters();
  924 | 		converters.forEach(messageConverters::add);
  925 | 	}
  926 | 
  927 | 	/**
  928 | 	 * Callback for building the {@link AsyncSupportConfigurer}.
  929 | 	 * Delegates to {@link #configureAsyncSupport(AsyncSupportConfigurer)}.
  930 | 	 * @since 5.3.2
  931 | 	 */
  932 | 	protected AsyncSupportConfigurer getAsyncSupportConfigurer() {
  933 | 		if (this.asyncSupportConfigurer == null) {
  934 | 			this.asyncSupportConfigurer = new AsyncSupportConfigurer();
  935 | 			configureAsyncSupport(this.asyncSupportConfigurer);
  936 | 		}
  937 | 		return this.asyncSupportConfigurer;
  938 | 	}
  939 | 
  940 | 	/**
  941 | 	 * Override this method to configure asynchronous request processing options.
  942 | 	 * @see AsyncSupportConfigurer
  943 | 	 */
  944 | 	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
  945 | 	}
  946 | 
  947 | 	/**
  948 | 	 * Return an instance of {@link CompositeUriComponentsContributor} for use with
  949 | 	 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}.
  950 | 	 * @since 4.0
  951 | 	 */
  952 | 	@Bean
  953 | 	public CompositeUriComponentsContributor mvcUriComponentsContributor(
  954 | 			@Qualifier("mvcConversionService") FormattingConversionService conversionService,
  955 | 			@Qualifier("requestMappingHandlerAdapter") RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
  956 | 		return new CompositeUriComponentsContributor(
  957 | 				requestMappingHandlerAdapter.getArgumentResolvers(), conversionService);
  958 | 	}
  959 | 
  960 | 	/**
  961 | 	 * Returns a {@link HttpRequestHandlerAdapter} for processing requests
  962 | 	 * with {@link HttpRequestHandler HttpRequestHandlers}.
  963 | 	 */
  964 | 	@Bean
  965 | 	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
  966 | 		return new HttpRequestHandlerAdapter();
  967 | 	}
  968 | 
  969 | 	/**
  970 | 	 * Returns a {@link SimpleControllerHandlerAdapter} for processing requests
  971 | 	 * with interface-based controllers.
  972 | 	 */
  973 | 	@Bean
  974 | 	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
  975 | 		return new SimpleControllerHandlerAdapter();
  976 | 	}
  977 | 
  978 | 	/**
  979 | 	 * Returns a {@link HandlerExceptionResolverComposite} containing a list of exception
  980 | 	 * resolvers obtained either through {@link #configureHandlerExceptionResolvers} or
  981 | 	 * through {@link #addDefaultHandlerExceptionResolvers}.
  982 | 	 * <p><strong>Note:</strong> This method cannot be made final due to CGLIB constraints.
  983 | 	 * Rather than overriding it, consider overriding {@link #configureHandlerExceptionResolvers}
  984 | 	 * which allows for providing a list of resolvers.
  985 | 	 */
  986 | 	@Bean
  987 | 	public HandlerExceptionResolver handlerExceptionResolver(
  988 | 			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
  989 | 		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<>();
  990 | 		configureHandlerExceptionResolvers(exceptionResolvers);
  991 | 		if (exceptionResolvers.isEmpty()) {
  992 | 			addDefaultHandlerExceptionResolvers(exceptionResolvers, contentNegotiationManager);
  993 | 		}
  994 | 		extendHandlerExceptionResolvers(exceptionResolvers);
  995 | 		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
  996 | 		composite.setOrder(0);
  997 | 		composite.setExceptionResolvers(exceptionResolvers);
  998 | 		return composite;
  999 | 	}
 1000 | 
 1001 | 	/**
 1002 | 	 * Override this method to configure the list of
 1003 | 	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} to use.
 1004 | 	 * <p>Adding resolvers to the list turns off the default resolvers that would otherwise
 1005 | 	 * be registered by default. Also see {@link #addDefaultHandlerExceptionResolvers}
 1006 | 	 * that can be used to add the default exception resolvers.
 1007 | 	 * @param exceptionResolvers a list to add exception resolvers to (initially an empty list)
 1008 | 	 */
 1009 | 	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
 1010 | 	}
 1011 | 
 1012 | 	/**
 1013 | 	 * Override this method to extend or modify the list of
 1014 | 	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} after it has been configured.
 1015 | 	 * <p>This may be useful for example to allow default resolvers to be registered
 1016 | 	 * and then insert a custom one through this method.
 1017 | 	 * @param exceptionResolvers the list of configured resolvers to extend.
 1018 | 	 * @since 4.3
 1019 | 	 */
 1020 | 	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
 1021 | 	}
 1022 | 
 1023 | 	/**
 1024 | 	 * A method available to subclasses for adding default
 1025 | 	 * {@link HandlerExceptionResolver HandlerExceptionResolvers}.
 1026 | 	 * <p>Adds the following exception resolvers:
 1027 | 	 * <ul>
 1028 | 	 * <li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through
 1029 | 	 * {@link org.springframework.web.bind.annotation.ExceptionHandler} methods.
 1030 | 	 * <li>{@link ResponseStatusExceptionResolver} for exceptions annotated with
 1031 | 	 * {@link org.springframework.web.bind.annotation.ResponseStatus}.
 1032 | 	 * <li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
 1033 | 	 * </ul>
 1034 | 	 */
 1035 | 	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers,
 1036 | 			ContentNegotiationManager mvcContentNegotiationManager) {
 1037 | 
 1038 | 		ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();
 1039 | 		exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager);
 1040 | 		exceptionHandlerResolver.setMessageConverters(getMessageConverters());
 1041 | 		exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());
 1042 | 		exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());
 1043 | 		exceptionHandlerResolver.setErrorResponseInterceptors(getErrorResponseInterceptors());
 1044 | 		ProblemDetailsConfigurer problemDetailsConfigurer = getProblemDetailsConfigurer();
 1045 | 		exceptionHandlerResolver.setRenderUnhandledExceptionsAsProblemDetails(
 1046 | 				problemDetailsConfigurer.isRenderUnhandledExceptions());
 1047 | 		exceptionHandlerResolver.setErrorResponseViewResolver(problemDetailsConfigurer.getViewResolver());
 1048 | 		if (JACKSON_PRESENT || JACKSON_2_PRESENT || KOTLIN_SERIALIZATION_PRESENT) {
 1049 | 			List<ResponseBodyAdvice<?>> responseBodyAdvices = new ArrayList<>(2);
 1050 | 			if (JACKSON_PRESENT || JACKSON_2_PRESENT) {
 1051 | 				responseBodyAdvices.add(new JsonViewResponseBodyAdvice());
 1052 | 			}
 1053 | 			if (KOTLIN_SERIALIZATION_PRESENT) {
 1054 | 				responseBodyAdvices.add(new KotlinResponseBodyAdvice());
 1055 | 			}
 1056 | 			exceptionHandlerResolver.setResponseBodyAdvice(responseBodyAdvices);
 1057 | 		}
 1058 | 		if (this.applicationContext != null) {
 1059 | 			exceptionHandlerResolver.setApplicationContext(this.applicationContext);
 1060 | 		}
 1061 | 		exceptionHandlerResolver.afterPropertiesSet();
 1062 | 		exceptionResolvers.add(exceptionHandlerResolver);
 1063 | 
 1064 | 		ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();
 1065 | 		responseStatusResolver.setMessageSource(this.applicationContext);
 1066 | 		exceptionResolvers.add(responseStatusResolver);
 1067 | 
 1068 | 		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
 1069 | 	}
 1070 | 
 1071 | 	/**
 1072 | 	 * Protected method for plugging in a custom subclass of
 1073 | 	 * {@link ExceptionHandlerExceptionResolver}.
 1074 | 	 * @since 4.3
 1075 | 	 */
 1076 | 	protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
 1077 | 		return new ExceptionHandlerExceptionResolver();
 1078 | 	}
 1079 | 
 1080 | 	/**
 1081 | 	 * Provide access to the list of {@link ErrorResponse.Interceptor}'s to apply
 1082 | 	 * when rendering error responses.
 1083 | 	 * <p>This method cannot be overridden; use {@link #configureErrorResponseInterceptors(List)} instead.
 1084 | 	 * @since 6.2
 1085 | 	 */
 1086 | 	protected final List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
 1087 | 		if (this.errorResponseInterceptors == null) {
 1088 | 			this.errorResponseInterceptors = new ArrayList<>();
 1089 | 			configureErrorResponseInterceptors(this.errorResponseInterceptors);
 1090 | 		}
 1091 | 		return this.errorResponseInterceptors;
 1092 | 	}
 1093 | 
 1094 | 	/**
 1095 | 	 * Returns the {@link ProblemDetailsConfigurer}, as prepared by
 1096 | 	 * {@link #customizeProblemDetails(ProblemDetailsConfigurer)}.
 1097 | 	 * @since 7.1
 1098 | 	 */
 1099 | 	protected final ProblemDetailsConfigurer getProblemDetailsConfigurer() {
 1100 | 		if (this.problemDetailsConfigurer == null) {
 1101 | 			this.problemDetailsConfigurer = new ProblemDetailsConfigurer();
 1102 | 			customizeProblemDetails(this.problemDetailsConfigurer);
 1103 | 		}
 1104 | 		return this.problemDetailsConfigurer;
 1105 | 	}
 1106 | 
 1107 | 	/**
 1108 | 	 * Override this method to configure how RFC 9457 problem details are
 1109 | 	 * produced and rendered by the default {@code ExceptionHandlerExceptionResolver}.
 1110 | 	 * @since 7.1
 1111 | 	 * @see ProblemDetailsConfigurer
 1112 | 	 */
 1113 | 	protected void customizeProblemDetails(ProblemDetailsConfigurer configurer) {
 1114 | 	}
 1115 | 
 1116 | 	/**
 1117 | 	 * Override this method for control over the {@link ErrorResponse.Interceptor}'s
 1118 | 	 * to apply when rendering error responses.
 1119 | 	 * @param interceptors the list to add handlers to
 1120 | 	 * @since 6.2
 1121 | 	 */
 1122 | 	protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
 1123 | 	}
 1124 | 
 1125 | 	/**
 1126 | 	 * Register a {@link ViewResolverComposite} that contains a chain of view resolvers
 1127 | 	 * to use for view resolution.
 1128 | 	 * By default, this resolver is ordered at 0 unless content negotiation view
 1129 | 	 * resolution is used in which case the order is raised to
 1130 | 	 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE
 1131 | 	 * Ordered.HIGHEST_PRECEDENCE}.
 1132 | 	 * <p>If no other resolvers are configured,
 1133 | 	 * {@link ViewResolverComposite#resolveViewName(String, Locale)} returns null in order
 1134 | 	 * to allow other potential {@link ViewResolver} beans to resolve views.
 1135 | 	 * @since 4.1
 1136 | 	 */
 1137 | 	@Bean
 1138 | 	public ViewResolver mvcViewResolver(
 1139 | 			@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
 1140 | 		ViewResolverRegistry registry =
 1141 | 				new ViewResolverRegistry(contentNegotiationManager, this.applicationContext);
 1142 | 		configureViewResolvers(registry);
 1143 | 
 1144 | 		if (registry.getViewResolvers().isEmpty() && this.applicationContext != null) {
 1145 | 			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
 1146 | 					this.applicationContext, ViewResolver.class, true, false);
 1147 | 			if (names.length == 1) {
 1148 | 				registry.getViewResolvers().add(new InternalResourceViewResolver());
 1149 | 			}
 1150 | 		}
 1151 | 
 1152 | 		ViewResolverComposite composite = new ViewResolverComposite();
 1153 | 		composite.setOrder(registry.getOrder());
 1154 | 		composite.setViewResolvers(registry.getViewResolvers());
 1155 | 		if (this.applicationContext != null) {
 1156 | 			composite.setApplicationContext(this.applicationContext);
 1157 | 		}
 1158 | 		if (this.servletContext != null) {
 1159 | 			composite.setServletContext(this.servletContext);
 1160 | 		}
 1161 | 		return composite;
 1162 | 	}
 1163 | 
 1164 | 	/**
 1165 | 	 * Override this method to configure view resolution.
 1166 | 	 * @see ViewResolverRegistry
 1167 | 	 */
 1168 | 	protected void configureViewResolvers(ViewResolverRegistry registry) {
 1169 | 	}
 1170 | 
 1171 | 	/**
 1172 | 	 * Return the registered {@link CorsConfiguration} objects,
 1173 | 	 * keyed by path pattern.
 1174 | 	 * @since 4.2
 1175 | 	 */
 1176 | 	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
 1177 | 		if (this.corsConfigurations == null) {
 1178 | 			CorsRegistry registry = new CorsRegistry();
 1179 | 			addCorsMappings(registry);
 1180 | 			this.corsConfigurations = registry.getCorsConfigurations();
 1181 | 		}
 1182 | 		return this.corsConfigurations;
 1183 | 	}
 1184 | 
 1185 | 	/**
 1186 | 	 * Override this method to configure cross-origin requests processing.
 1187 | 	 * @since 4.2
 1188 | 	 * @see CorsRegistry
 1189 | 	 */
 1190 | 	protected void addCorsMappings(CorsRegistry registry) {
 1191 | 	}
 1192 | 
 1193 | 	@Bean
 1194 | 	@Lazy
 1195 | 	public PreFlightRequestHandler mvcPreFlightRequestHandler() {
 1196 | 		return new DefaultPreFlightRequestHandler();
 1197 | 	}
 1198 | 
 1199 | 	@Bean
 1200 | 	public LocaleResolver localeResolver() {
 1201 | 		return new AcceptHeaderLocaleResolver();
 1202 | 	}
 1203 | 
 1204 | 	@Bean
 1205 | 	public FlashMapManager flashMapManager() {
 1206 | 		return new SessionFlashMapManager();
 1207 | 	}
 1208 | 
 1209 | 	@Bean
 1210 | 	public RequestToViewNameTranslator viewNameTranslator() {
 1211 | 		return new DefaultRequestToViewNameTranslator();
 1212 | 	}
 1213 | 
 1214 | 
 1215 | 	private static final class NoOpValidator implements Validator {
 1216 | 
 1217 | 		@Override
 1218 | 		public boolean supports(Class<?> clazz) {
 1219 | 			return false;
 1220 | 		}
 1221 | 
 1222 | 		@Override
 1223 | 		public void validate(@Nullable Object target, Errors errors) {
 1224 | 		}
 1225 | 	}
 1226 | 
 1227 | }
```
