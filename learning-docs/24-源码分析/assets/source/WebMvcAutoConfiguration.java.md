<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/WebMvcAutoConfiguration.java -->
<!-- 正文引用：[[源码:WebMvcAutoConfiguration.java#L起-L止]] ；代码块头部：(src: WebMvcAutoConfiguration.java#L起-L止) -->

# WebMvcAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/WebMvcAutoConfiguration.java` |
| 行数 | 704 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.548Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.List;
    9 | import java.util.Locale;
   10 | import java.util.Map;
   11 | import java.util.function.Consumer;
   12 | 
   13 | import jakarta.servlet.Servlet;
   14 | import jakarta.servlet.ServletContext;
   15 | import org.apache.commons.logging.Log;
   16 | import org.apache.commons.logging.LogFactory;
   17 | import org.jspecify.annotations.Nullable;
   18 | 
   19 | import org.springframework.aot.hint.RuntimeHints;
   20 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   21 | import org.springframework.aot.hint.TypeReference;
   22 | import org.springframework.beans.factory.BeanFactory;
   23 | import org.springframework.beans.factory.ListableBeanFactory;
   24 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   25 | import org.springframework.beans.factory.ObjectProvider;
   26 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   27 | import org.springframework.boot.autoconfigure.AutoConfigureOrder;
   28 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   29 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   30 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
   31 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   32 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   33 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
   34 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   35 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   36 | import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
   37 | import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
   38 | import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
   39 | import org.springframework.boot.autoconfigure.web.WebProperties;
   40 | import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
   41 | import org.springframework.boot.autoconfigure.web.WebProperties.Resources.Chain.Strategy;
   42 | import org.springframework.boot.autoconfigure.web.WebResourcesRuntimeHints;
   43 | import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
   44 | import org.springframework.boot.autoconfigure.web.format.WebConversionService;
   45 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   46 | import org.springframework.boot.context.properties.PropertyMapper;
   47 | import org.springframework.boot.convert.ApplicationConversionService;
   48 | import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
   49 | import org.springframework.boot.servlet.filter.OrderedFormContentFilter;
   50 | import org.springframework.boot.servlet.filter.OrderedHiddenHttpMethodFilter;
   51 | import org.springframework.boot.servlet.filter.OrderedRequestContextFilter;
   52 | import org.springframework.boot.validation.autoconfigure.ValidatorAdapter;
   53 | import org.springframework.boot.web.servlet.ServletRegistrationBean;
   54 | import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion;
   55 | import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Apiversion.Use;
   56 | import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties.Format;
   57 | import org.springframework.context.ApplicationContext;
   58 | import org.springframework.context.EmbeddedValueResolverAware;
   59 | import org.springframework.context.ResourceLoaderAware;
   60 | import org.springframework.context.annotation.Bean;
   61 | import org.springframework.context.annotation.Configuration;
   62 | import org.springframework.context.annotation.Import;
   63 | import org.springframework.context.annotation.ImportRuntimeHints;
   64 | import org.springframework.core.Ordered;
   65 | import org.springframework.core.annotation.Order;
   66 | import org.springframework.core.io.Resource;
   67 | import org.springframework.core.io.ResourceLoader;
   68 | import org.springframework.core.task.AsyncTaskExecutor;
   69 | import org.springframework.format.FormatterRegistry;
   70 | import org.springframework.format.support.FormattingConversionService;
   71 | import org.springframework.http.CacheControl;
   72 | import org.springframework.http.MediaType;
   73 | import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
   74 | import org.springframework.lang.Contract;
   75 | import org.springframework.util.Assert;
   76 | import org.springframework.util.ClassUtils;
   77 | import org.springframework.util.CollectionUtils;
   78 | import org.springframework.util.StringValueResolver;
   79 | import org.springframework.validation.DefaultMessageCodesResolver;
   80 | import org.springframework.validation.MessageCodesResolver;
   81 | import org.springframework.validation.Validator;
   82 | import org.springframework.web.accept.ApiVersionDeprecationHandler;
   83 | import org.springframework.web.accept.ApiVersionParser;
   84 | import org.springframework.web.accept.ApiVersionResolver;
   85 | import org.springframework.web.accept.ApiVersionStrategy;
   86 | import org.springframework.web.accept.ContentNegotiationManager;
   87 | import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
   88 | import org.springframework.web.context.ServletContextAware;
   89 | import org.springframework.web.context.request.RequestContextListener;
   90 | import org.springframework.web.context.support.ServletContextResource;
   91 | import org.springframework.web.filter.FormContentFilter;
   92 | import org.springframework.web.filter.HiddenHttpMethodFilter;
   93 | import org.springframework.web.filter.RequestContextFilter;
   94 | import org.springframework.web.servlet.DispatcherServlet;
   95 | import org.springframework.web.servlet.FlashMapManager;
   96 | import org.springframework.web.servlet.HandlerExceptionResolver;
   97 | import org.springframework.web.servlet.LocaleResolver;
   98 | import org.springframework.web.servlet.RequestToViewNameTranslator;
   99 | import org.springframework.web.servlet.View;
  100 | import org.springframework.web.servlet.ViewResolver;
  101 | import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
  102 | import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
  103 | import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
  104 | import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
  105 | import org.springframework.web.servlet.config.annotation.EnableWebMvc;
  106 | import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
  107 | import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
  108 | import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
  109 | import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
  110 | import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
  111 | import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
  112 | import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
  113 | import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
  114 | import org.springframework.web.servlet.i18n.FixedLocaleResolver;
  115 | import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
  116 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
  117 | import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
  118 | import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
  119 | import org.springframework.web.servlet.resource.EncodedResourceResolver;
  120 | import org.springframework.web.servlet.resource.ResourceResolver;
  121 | import org.springframework.web.servlet.resource.ResourceUrlProvider;
  122 | import org.springframework.web.servlet.resource.VersionResourceResolver;
  123 | import org.springframework.web.servlet.view.BeanNameViewResolver;
  124 | import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
  125 | import org.springframework.web.servlet.view.InternalResourceViewResolver;
  126 | 
  127 | /**
  128 |  * {@link EnableAutoConfiguration Auto-configuration} for {@link EnableWebMvc Web MVC}.
  129 |  *
  130 |  * @author Phillip Webb
  131 |  * @author Dave Syer
  132 |  * @author Andy Wilkinson
  133 |  * @author Sébastien Deleuze
  134 |  * @author Eddú Meléndez
  135 |  * @author Stephane Nicoll
  136 |  * @author Kristine Jetzke
  137 |  * @author Bruce Brouwer
  138 |  * @author Artsiom Yudovin
  139 |  * @author Scott Frederick
  140 |  * @since 4.0.0
  141 |  */
  142 | @AutoConfiguration(after = { DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class },
  143 | 		afterName = "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration")
  144 | @ConditionalOnWebApplication(type = Type.SERVLET)
  145 | @ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
  146 | @ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
  147 | @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
  148 | @ImportRuntimeHints(WebResourcesRuntimeHints.class)
  149 | public final class WebMvcAutoConfiguration {
  150 | 
  151 | 	/**
  152 | 	 * The default Spring MVC view prefix.
  153 | 	 */
  154 | 	public static final String DEFAULT_PREFIX = "";
  155 | 
  156 | 	/**
  157 | 	 * The default Spring MVC view suffix.
  158 | 	 */
  159 | 	public static final String DEFAULT_SUFFIX = "";
  160 | 
  161 | 	private static final String SERVLET_LOCATION = "/";
  162 | 
  163 | 	@Bean
  164 | 	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
  165 | 	@ConditionalOnBooleanProperty("spring.mvc.hiddenmethod.filter.enabled")
  166 | 	OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
  167 | 		return new OrderedHiddenHttpMethodFilter();
  168 | 	}
  169 | 
  170 | 	@Bean
  171 | 	@ConditionalOnMissingBean(FormContentFilter.class)
  172 | 	@ConditionalOnBooleanProperty(name = "spring.mvc.formcontent.filter.enabled", matchIfMissing = true)
  173 | 	OrderedFormContentFilter formContentFilter() {
  174 | 		return new OrderedFormContentFilter();
  175 | 	}
  176 | 
  177 | 	// Defined as a nested config to ensure WebMvcConfigurer is not read when not
  178 | 	// on the classpath
  179 | 	@Configuration(proxyBeanMethods = false)
  180 | 	@Import(EnableWebMvcConfiguration.class)
  181 | 	@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
  182 | 	@Order(0)
  183 | 	static class WebMvcAutoConfigurationAdapter implements WebMvcConfigurer, ServletContextAware {
  184 | 
  185 | 		private static final Log logger = LogFactory.getLog(WebMvcConfigurer.class);
  186 | 
  187 | 		private final Resources resourceProperties;
  188 | 
  189 | 		private final WebMvcProperties mvcProperties;
  190 | 
  191 | 		private final ListableBeanFactory beanFactory;
  192 | 
  193 | 		private final ObjectProvider<DispatcherServletPath> dispatcherServletPath;
  194 | 
  195 | 		private final ObjectProvider<ServletRegistrationBean<?>> servletRegistrations;
  196 | 
  197 | 		private final ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider;
  198 | 
  199 | 		private final @Nullable ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;
  200 | 
  201 | 		private @Nullable ServletContext servletContext;
  202 | 
  203 | 		private final ObjectProvider<ApiVersionResolver> apiVersionResolvers;
  204 | 
  205 | 		private final ObjectProvider<ApiVersionParser<?>> apiVersionParser;
  206 | 
  207 | 		private final ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler;
  208 | 
  209 | 		WebMvcAutoConfigurationAdapter(WebProperties webProperties, WebMvcProperties mvcProperties,
  210 | 				ListableBeanFactory beanFactory,
  211 | 				ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider,
  212 | 				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
  213 | 				ObjectProvider<DispatcherServletPath> dispatcherServletPath,
  214 | 				ObjectProvider<ServletRegistrationBean<?>> servletRegistrations,
  215 | 				ObjectProvider<ApiVersionResolver> apiVersionResolvers,
  216 | 				ObjectProvider<ApiVersionParser<?>> apiVersionParser,
  217 | 				ObjectProvider<ApiVersionDeprecationHandler> apiVersionDeprecationHandler) {
  218 | 			this.resourceProperties = webProperties.getResources();
  219 | 			this.mvcProperties = mvcProperties;
  220 | 			this.beanFactory = beanFactory;
  221 | 			this.httpMessageConvertersCustomizerProvider = httpMessageConvertersCustomizerProvider;
  222 | 			this.resourceHandlerRegistrationCustomizer = resourceHandlerRegistrationCustomizerProvider.getIfAvailable();
  223 | 			this.dispatcherServletPath = dispatcherServletPath;
  224 | 			this.servletRegistrations = servletRegistrations;
  225 | 			this.apiVersionResolvers = apiVersionResolvers;
  226 | 			this.apiVersionParser = apiVersionParser;
  227 | 			this.apiVersionDeprecationHandler = apiVersionDeprecationHandler;
  228 | 		}
  229 | 
  230 | 		@Override
  231 | 		public void setServletContext(ServletContext servletContext) {
  232 | 			this.servletContext = servletContext;
  233 | 		}
  234 | 
  235 | 		@Override
  236 | 		public void configureMessageConverters(ServerBuilder builder) {
  237 | 			this.httpMessageConvertersCustomizerProvider.orderedStream()
  238 | 				.forEach((customizer) -> customizer.customize(builder));
  239 | 		}
  240 | 
  241 | 		@Override
  242 | 		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
  243 | 			if (this.beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
  244 | 				Object taskExecutor = this.beanFactory
  245 | 					.getBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
  246 | 				if (taskExecutor instanceof AsyncTaskExecutor asyncTaskExecutor) {
  247 | 					configurer.setTaskExecutor(asyncTaskExecutor);
  248 | 				}
  249 | 			}
  250 | 			Duration timeout = this.mvcProperties.getAsync().getRequestTimeout();
  251 | 			if (timeout != null) {
  252 | 				configurer.setDefaultTimeout(timeout.toMillis());
  253 | 			}
  254 | 		}
  255 | 
  256 | 		@Override
  257 | 		public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
  258 | 			WebMvcProperties.Contentnegotiation contentnegotiation = this.mvcProperties.getContentnegotiation();
  259 | 			configurer.favorParameter(contentnegotiation.isFavorParameter());
  260 | 			if (contentnegotiation.getParameterName() != null) {
  261 | 				configurer.parameterName(contentnegotiation.getParameterName());
  262 | 			}
  263 | 			Map<String, MediaType> mediaTypes = contentnegotiation.getMediaTypes();
  264 | 			mediaTypes.forEach(configurer::mediaType);
  265 | 			List<MediaType> defaultContentTypes = contentnegotiation.getDefaultContentTypes();
  266 | 			if (!CollectionUtils.isEmpty(defaultContentTypes)) {
  267 | 				configurer.defaultContentType(defaultContentTypes.toArray(new MediaType[0]));
  268 | 			}
  269 | 		}
  270 | 
  271 | 		@Bean
  272 | 		@ConditionalOnMissingBean
  273 | 		InternalResourceViewResolver defaultViewResolver() {
  274 | 			InternalResourceViewResolver resolver = new InternalResourceViewResolver();
  275 | 			resolver.setPrefix(this.mvcProperties.getView().getPrefix());
  276 | 			resolver.setSuffix(this.mvcProperties.getView().getSuffix());
  277 | 			return resolver;
  278 | 		}
  279 | 
  280 | 		@Bean
  281 | 		@ConditionalOnBean(View.class)
  282 | 		@ConditionalOnMissingBean
  283 | 		BeanNameViewResolver beanNameViewResolver() {
  284 | 			BeanNameViewResolver resolver = new BeanNameViewResolver();
  285 | 			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
  286 | 			return resolver;
  287 | 		}
  288 | 
  289 | 		@Bean
  290 | 		@ConditionalOnBean(ViewResolver.class)
  291 | 		@ConditionalOnMissingBean(name = "viewResolver", value = ContentNegotiatingViewResolver.class)
  292 | 		ContentNegotiatingViewResolver viewResolver(BeanFactory beanFactory) {
  293 | 			ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
  294 | 			resolver.setContentNegotiationManager(beanFactory.getBean(ContentNegotiationManager.class));
  295 | 			// ContentNegotiatingViewResolver uses all the other view resolvers to locate
  296 | 			// a view so it should have a high precedence
  297 | 			resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
  298 | 			return resolver;
  299 | 		}
  300 | 
  301 | 		@Override
  302 | 		public @Nullable MessageCodesResolver getMessageCodesResolver() {
  303 | 			if (this.mvcProperties.getMessageCodesResolverFormat() != null) {
  304 | 				DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();
  305 | 				resolver.setMessageCodeFormatter(this.mvcProperties.getMessageCodesResolverFormat());
  306 | 				return resolver;
  307 | 			}
  308 | 			return null;
  309 | 		}
  310 | 
  311 | 		@Override
  312 | 		public void addFormatters(FormatterRegistry registry) {
  313 | 			ApplicationConversionService.addBeans(registry, this.beanFactory);
  314 | 		}
  315 | 
  316 | 		@Override
  317 | 		public void addResourceHandlers(ResourceHandlerRegistry registry) {
  318 | 			if (!this.resourceProperties.isAddMappings()) {
  319 | 				logger.debug("Default resource handling disabled");
  320 | 				return;
  321 | 			}
  322 | 			addResourceHandler(registry, this.mvcProperties.getWebjarsPathPattern(),
  323 | 					"classpath:/META-INF/resources/webjars/");
  324 | 			addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
  325 | 				registration.addResourceLocations(this.resourceProperties.getStaticLocations());
  326 | 				if (this.servletContext != null) {
  327 | 					ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
  328 | 					registration.addResourceLocations(resource);
  329 | 				}
  330 | 			});
  331 | 		}
  332 | 
  333 | 		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
  334 | 			addResourceHandler(registry, pattern, (registration) -> registration.addResourceLocations(locations));
  335 | 		}
  336 | 
  337 | 		private void addResourceHandler(ResourceHandlerRegistry registry, String pattern,
  338 | 				Consumer<ResourceHandlerRegistration> customizer) {
  339 | 			if (registry.hasMappingForPattern(pattern)) {
  340 | 				return;
  341 | 			}
  342 | 			ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
  343 | 			customizer.accept(registration);
  344 | 			Integer cachePeriod = getSeconds(this.resourceProperties.getCache().getPeriod());
  345 | 			if (cachePeriod != null) {
  346 | 				registration.setCachePeriod(cachePeriod);
  347 | 			}
  348 | 			CacheControl cacheControl = this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl();
  349 | 			if (cacheControl != null) {
  350 | 				registration.setCacheControl(cacheControl);
  351 | 			}
  352 | 			registration.setUseLastModified(this.resourceProperties.getCache().isUseLastModified());
  353 | 			customizeResourceHandlerRegistration(registration);
  354 | 		}
  355 | 
  356 | 		@Contract("!null -> !null")
  357 | 		private @Nullable Integer getSeconds(@Nullable Duration cachePeriod) {
  358 | 			return (cachePeriod != null) ? (int) cachePeriod.getSeconds() : null;
  359 | 		}
  360 | 
  361 | 		private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
  362 | 			if (this.resourceHandlerRegistrationCustomizer != null) {
  363 | 				this.resourceHandlerRegistrationCustomizer.customize(registration);
  364 | 			}
  365 | 		}
  366 | 
  367 | 		@Override
  368 | 		public void configureApiVersioning(ApiVersionConfigurer configurer) {
  369 | 			PropertyMapper map = PropertyMapper.get();
  370 | 			Apiversion properties = this.mvcProperties.getApiversion();
  371 | 			map.from(properties::getRequired).to(configurer::setVersionRequired);
  372 | 			map.from(properties::getDefaultVersion).to(configurer::setDefaultVersion);
  373 | 			map.from(properties::getSupported).to((supported) -> supported.forEach(configurer::addSupportedVersions));
  374 | 			map.from(properties::getDetectSupported).to(configurer::detectSupportedVersions);
  375 | 			configureApiVersioningUse(configurer, properties.getUse());
  376 | 			this.apiVersionResolvers.orderedStream().forEach(configurer::useVersionResolver);
  377 | 			this.apiVersionParser.ifAvailable(configurer::setVersionParser);
  378 | 			this.apiVersionDeprecationHandler.ifAvailable(configurer::setDeprecationHandler);
  379 | 		}
  380 | 
  381 | 		private void configureApiVersioningUse(ApiVersionConfigurer configurer, Use use) {
  382 | 			PropertyMapper map = PropertyMapper.get();
  383 | 			map.from(use::getHeader).whenHasText().to(configurer::useRequestHeader);
  384 | 			map.from(use::getQueryParameter).whenHasText().to(configurer::useQueryParam);
  385 | 			use.getMediaTypeParameter().forEach(configurer::useMediaTypeParameter);
  386 | 			map.from(use::getPathSegment).to(configurer::usePathSegment);
  387 | 		}
  388 | 
  389 | 		@Bean
  390 | 		@ConditionalOnMissingBean({ RequestContextListener.class, RequestContextFilter.class })
  391 | 		@ConditionalOnMissingFilterBean
  392 | 		static RequestContextFilter requestContextFilter() {
  393 | 			return new OrderedRequestContextFilter();
  394 | 		}
  395 | 
  396 | 	}
  397 | 
  398 | 	/**
  399 | 	 * Configuration equivalent to {@code @EnableWebMvc}.
  400 | 	 */
  401 | 	@Configuration(proxyBeanMethods = false)
  402 | 	@EnableConfigurationProperties(WebProperties.class)
  403 | 	@ImportRuntimeHints(MvcValidatorRuntimeHints.class)
  404 | 	static class EnableWebMvcConfiguration extends DelegatingWebMvcConfiguration
  405 | 			implements ResourceLoaderAware, EmbeddedValueResolverAware {
  406 | 
  407 | 		private final Resources resourceProperties;
  408 | 
  409 | 		private final WebMvcProperties mvcProperties;
  410 | 
  411 | 		private final WebProperties webProperties;
  412 | 
  413 | 		private final ListableBeanFactory beanFactory;
  414 | 
  415 | 		private final @Nullable WebMvcRegistrations mvcRegistrations;
  416 | 
  417 | 		private @Nullable StringValueResolver embeddedValueResolver;
  418 | 
  419 | 		@SuppressWarnings("NullAway.Init")
  420 | 		private ResourceLoader resourceLoader;
  421 | 
  422 | 		EnableWebMvcConfiguration(WebMvcProperties mvcProperties, WebProperties webProperties,
  423 | 				ObjectProvider<WebMvcRegistrations> mvcRegistrationsProvider,
  424 | 				ObjectProvider<ResourceHandlerRegistrationCustomizer> resourceHandlerRegistrationCustomizerProvider,
  425 | 				ListableBeanFactory beanFactory) {
  426 | 			this.resourceProperties = webProperties.getResources();
  427 | 			this.mvcProperties = mvcProperties;
  428 | 			this.webProperties = webProperties;
  429 | 			this.mvcRegistrations = mvcRegistrationsProvider.getIfUnique();
  430 | 			this.beanFactory = beanFactory;
  431 | 		}
  432 | 
  433 | 		@Override
  434 | 		protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
  435 | 			if (this.mvcRegistrations != null) {
  436 | 				RequestMappingHandlerAdapter adapter = this.mvcRegistrations.getRequestMappingHandlerAdapter();
  437 | 				if (adapter != null) {
  438 | 					return adapter;
  439 | 				}
  440 | 			}
  441 | 			return super.createRequestMappingHandlerAdapter();
  442 | 		}
  443 | 
  444 | 		@Bean
  445 | 		WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
  446 | 				FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
  447 | 			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
  448 | 					WelcomePageHandlerMapping::new);
  449 | 		}
  450 | 
  451 | 		@Bean
  452 | 		WelcomePageNotAcceptableHandlerMapping welcomePageNotAcceptableHandlerMapping(
  453 | 				ApplicationContext applicationContext, FormattingConversionService mvcConversionService,
  454 | 				ResourceUrlProvider mvcResourceUrlProvider) {
  455 | 			return createWelcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider,
  456 | 					WelcomePageNotAcceptableHandlerMapping::new);
  457 | 		}
  458 | 
  459 | 		private <T extends AbstractUrlHandlerMapping> T createWelcomePageHandlerMapping(
  460 | 				ApplicationContext applicationContext, FormattingConversionService mvcConversionService,
  461 | 				ResourceUrlProvider mvcResourceUrlProvider, WelcomePageHandlerMappingFactory<T> factory) {
  462 | 			TemplateAvailabilityProviders templateAvailabilityProviders = new TemplateAvailabilityProviders(
  463 | 					applicationContext);
  464 | 			String staticPathPattern = this.mvcProperties.getStaticPathPattern();
  465 | 			T handlerMapping = factory.create(templateAvailabilityProviders, applicationContext, getIndexHtmlResource(),
  466 | 					staticPathPattern);
  467 | 			handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
  468 | 			handlerMapping.setCorsConfigurations(getCorsConfigurations());
  469 | 			return handlerMapping;
  470 | 		}
  471 | 
  472 | 		@Override
  473 | 		@Bean
  474 | 		@ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  475 | 		public LocaleResolver localeResolver() {
  476 | 			Locale locale = this.webProperties.getLocale();
  477 | 			if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
  478 | 				Assert.state(locale != null, "'locale' must not be null");
  479 | 				return new FixedLocaleResolver(locale);
  480 | 			}
  481 | 			AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
  482 | 			localeResolver.setDefaultLocale(locale);
  483 | 			return localeResolver;
  484 | 		}
  485 | 
  486 | 		@Override
  487 | 		@Bean
  488 | 		@ConditionalOnMissingBean(name = DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)
  489 | 		public FlashMapManager flashMapManager() {
  490 | 			return super.flashMapManager();
  491 | 		}
  492 | 
  493 | 		@Override
  494 | 		@Bean
  495 | 		@ConditionalOnMissingBean(name = DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME)
  496 | 		public RequestToViewNameTranslator viewNameTranslator() {
  497 | 			return super.viewNameTranslator();
  498 | 		}
  499 | 
  500 | 		private @Nullable Resource getIndexHtmlResource() {
  501 | 			for (String location : this.resourceProperties.getStaticLocations()) {
  502 | 				Resource indexHtml = getIndexHtmlResource(location);
  503 | 				if (indexHtml != null) {
  504 | 					return indexHtml;
  505 | 				}
  506 | 			}
  507 | 			ServletContext servletContext = getServletContext();
  508 | 			if (servletContext != null) {
  509 | 				return getIndexHtmlResource(new ServletContextResource(servletContext, SERVLET_LOCATION));
  510 | 			}
  511 | 			return null;
  512 | 		}
  513 | 
  514 | 		private @Nullable Resource getIndexHtmlResource(String location) {
  515 | 			return getIndexHtmlResource(this.resourceLoader.getResource(location));
  516 | 		}
  517 | 
  518 | 		private @Nullable Resource getIndexHtmlResource(Resource location) {
  519 | 			try {
  520 | 				Resource resource = location.createRelative("index.html");
  521 | 				if (resource.exists() && (resource.getURL() != null)) {
  522 | 					return resource;
  523 | 				}
  524 | 			}
  525 | 			catch (Exception ex) {
  526 | 				// Ignore
  527 | 			}
  528 | 			return null;
  529 | 		}
  530 | 
  531 | 		@Bean
  532 | 		@Override
  533 | 		public FormattingConversionService mvcConversionService() {
  534 | 			Format format = this.mvcProperties.getFormat();
  535 | 			WebConversionService conversionService = new WebConversionService(this.embeddedValueResolver,
  536 | 					new DateTimeFormatters().dateFormat(format.getDate())
  537 | 						.timeFormat(format.getTime())
  538 | 						.dateTimeFormat(format.getDateTime()));
  539 | 			addFormatters(conversionService);
  540 | 			return conversionService;
  541 | 		}
  542 | 
  543 | 		@Override
  544 | 		public void setEmbeddedValueResolver(StringValueResolver resolver) {
  545 | 			this.embeddedValueResolver = resolver;
  546 | 		}
  547 | 
  548 | 		@Bean
  549 | 		@Override
  550 | 		public Validator mvcValidator() {
  551 | 			if (!ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())
  552 | 					|| !ClassUtils.isPresent("org.springframework.boot.validation.autoconfigure.ValidatorAdapter",
  553 | 							getClass().getClassLoader())) {
  554 | 				return super.mvcValidator();
  555 | 			}
  556 | 			ApplicationContext applicationContext = getApplicationContext();
  557 | 			Assert.state(applicationContext != null, "'applicationContext' must not be null");
  558 | 			return ValidatorAdapter.get(applicationContext, getValidator());
  559 | 		}
  560 | 
  561 | 		@Override
  562 | 		protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
  563 | 			if (this.mvcRegistrations != null) {
  564 | 				RequestMappingHandlerMapping mapping = this.mvcRegistrations.getRequestMappingHandlerMapping();
  565 | 				if (mapping != null) {
  566 | 					return mapping;
  567 | 				}
  568 | 			}
  569 | 			return super.createRequestMappingHandlerMapping();
  570 | 		}
  571 | 
  572 | 		@Override
  573 | 		protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
  574 | 				FormattingConversionService mvcConversionService, Validator mvcValidator) {
  575 | 			try {
  576 | 				return this.beanFactory.getBean(ConfigurableWebBindingInitializer.class);
  577 | 			}
  578 | 			catch (NoSuchBeanDefinitionException ex) {
  579 | 				return super.getConfigurableWebBindingInitializer(mvcConversionService, mvcValidator);
  580 | 			}
  581 | 		}
  582 | 
  583 | 		@Override
  584 | 		protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
  585 | 			if (this.mvcRegistrations != null) {
  586 | 				ExceptionHandlerExceptionResolver resolver = this.mvcRegistrations
  587 | 					.getExceptionHandlerExceptionResolver();
  588 | 				if (resolver != null) {
  589 | 					return resolver;
  590 | 				}
  591 | 			}
  592 | 			return super.createExceptionHandlerExceptionResolver();
  593 | 		}
  594 | 
  595 | 		@Override
  596 | 		protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
  597 | 			super.extendHandlerExceptionResolvers(exceptionResolvers);
  598 | 			if (this.mvcProperties.isLogResolvedException()) {
  599 | 				for (HandlerExceptionResolver resolver : exceptionResolvers) {
  600 | 					if (resolver instanceof AbstractHandlerExceptionResolver abstractResolver) {
  601 | 						abstractResolver.setWarnLogCategory(resolver.getClass().getName());
  602 | 					}
  603 | 				}
  604 | 			}
  605 | 		}
  606 | 
  607 | 		@Override
  608 | 		public void setResourceLoader(ResourceLoader resourceLoader) {
  609 | 			this.resourceLoader = resourceLoader;
  610 | 		}
  611 | 
  612 | 		@Override
  613 | 		@ConditionalOnMissingBean(name = "mvcApiVersionStrategy")
  614 | 		public @Nullable ApiVersionStrategy mvcApiVersionStrategy() {
  615 | 			return super.mvcApiVersionStrategy();
  616 | 		}
  617 | 
  618 | 	}
  619 | 
  620 | 	@Configuration(proxyBeanMethods = false)
  621 | 	@ConditionalOnEnabledResourceChain
  622 | 	static class ResourceChainCustomizerConfiguration {
  623 | 
  624 | 		@Bean
  625 | 		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
  626 | 				WebProperties webProperties) {
  627 | 			return new ResourceChainResourceHandlerRegistrationCustomizer(webProperties.getResources());
  628 | 		}
  629 | 
  630 | 	}
  631 | 
  632 | 	@FunctionalInterface
  633 | 	interface WelcomePageHandlerMappingFactory<T extends AbstractUrlHandlerMapping> {
  634 | 
  635 | 		T create(TemplateAvailabilityProviders templateAvailabilityProviders, ApplicationContext applicationContext,
  636 | 				@Nullable Resource indexHtmlResource, String staticPathPattern);
  637 | 
  638 | 	}
  639 | 
  640 | 	static class ResourceChainResourceHandlerRegistrationCustomizer implements ResourceHandlerRegistrationCustomizer {
  641 | 
  642 | 		private final Resources resourceProperties;
  643 | 
  644 | 		ResourceChainResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
  645 | 			this.resourceProperties = resourceProperties;
  646 | 		}
  647 | 
  648 | 		@Override
  649 | 		public void customize(ResourceHandlerRegistration registration) {
  650 | 			Resources.Chain properties = this.resourceProperties.getChain();
  651 | 			configureResourceChain(properties, registration.resourceChain(properties.isCache()));
  652 | 		}
  653 | 
  654 | 		private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
  655 | 			Strategy strategy = properties.getStrategy();
  656 | 			if (properties.isCompressed()) {
  657 | 				chain.addResolver(new EncodedResourceResolver());
  658 | 			}
  659 | 			if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
  660 | 				chain.addResolver(getVersionResourceResolver(strategy));
  661 | 			}
  662 | 		}
  663 | 
  664 | 		private ResourceResolver getVersionResourceResolver(Strategy properties) {
  665 | 			VersionResourceResolver resolver = new VersionResourceResolver();
  666 | 			if (properties.getFixed().isEnabled()) {
  667 | 				String version = properties.getFixed().getVersion();
  668 | 				String[] paths = properties.getFixed().getPaths();
  669 | 				Assert.state(version != null, "'version' must not be null");
  670 | 				resolver.addFixedVersionStrategy(version, paths);
  671 | 			}
  672 | 			if (properties.getContent().isEnabled()) {
  673 | 				String[] paths = properties.getContent().getPaths();
  674 | 				resolver.addContentVersionStrategy(paths);
  675 | 			}
  676 | 			return resolver;
  677 | 		}
  678 | 
  679 | 	}
  680 | 
  681 | 	@Configuration(proxyBeanMethods = false)
  682 | 	@ConditionalOnBooleanProperty("spring.mvc.problemdetails.enabled")
  683 | 	static class ProblemDetailsErrorHandlingConfiguration {
  684 | 
  685 | 		@Bean
  686 | 		@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
  687 | 		@Order(0)
  688 | 		ProblemDetailsExceptionHandler problemDetailsExceptionHandler() {
  689 | 			return new ProblemDetailsExceptionHandler();
  690 | 		}
  691 | 
  692 | 	}
  693 | 
  694 | 	static class MvcValidatorRuntimeHints implements RuntimeHintsRegistrar {
  695 | 
  696 | 		@Override
  697 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  698 | 			hints.reflection()
  699 | 				.registerType(TypeReference.of("org.springframework.boot.validation.autoconfigure.ValidatorAdapter"));
  700 | 		}
  701 | 
  702 | 	}
  703 | 
  704 | }
```
