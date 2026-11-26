<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/SpringBootContextLoader.java -->
<!-- 正文引用：[[源码:SpringBootContextLoader.java#L起-L止]] ；代码块头部：(src: SpringBootContextLoader.java#L起-L止) -->

# SpringBootContextLoader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/SpringBootContextLoader.java` |
| 行数 | 612 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.080Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context;
    6 | 
    7 | import java.lang.reflect.Method;
    8 | import java.lang.reflect.Modifier;
    9 | import java.util.ArrayList;
   10 | import java.util.Arrays;
   11 | import java.util.Collections;
   12 | import java.util.List;
   13 | import java.util.function.Consumer;
   14 | 
   15 | import org.jspecify.annotations.Nullable;
   16 | 
   17 | import org.springframework.aot.hint.ExecutableMode;
   18 | import org.springframework.aot.hint.RuntimeHints;
   19 | import org.springframework.beans.BeanUtils;
   20 | import org.springframework.boot.ApplicationContextFactory;
   21 | import org.springframework.boot.Banner;
   22 | import org.springframework.boot.SpringApplication;
   23 | import org.springframework.boot.SpringApplication.AbandonedRunException;
   24 | import org.springframework.boot.SpringApplicationHook;
   25 | import org.springframework.boot.SpringApplicationRunListener;
   26 | import org.springframework.boot.SpringBootConfiguration;
   27 | import org.springframework.boot.WebApplicationType;
   28 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   29 | import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
   30 | import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
   31 | import org.springframework.boot.test.mock.web.SpringBootMockServletContext;
   32 | import org.springframework.boot.test.util.TestPropertyValues;
   33 | import org.springframework.boot.test.util.TestPropertyValues.Type;
   34 | import org.springframework.boot.web.context.reactive.GenericReactiveWebApplicationContext;
   35 | import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer;
   36 | import org.springframework.context.ApplicationContext;
   37 | import org.springframework.context.ApplicationContextInitializer;
   38 | import org.springframework.context.ApplicationListener;
   39 | import org.springframework.context.ConfigurableApplicationContext;
   40 | import org.springframework.context.aot.AotApplicationContextInitializer;
   41 | import org.springframework.core.KotlinDetector;
   42 | import org.springframework.core.Ordered;
   43 | import org.springframework.core.PriorityOrdered;
   44 | import org.springframework.core.annotation.MergedAnnotations;
   45 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   46 | import org.springframework.core.annotation.Order;
   47 | import org.springframework.core.env.ConfigurableEnvironment;
   48 | import org.springframework.core.io.DefaultResourceLoader;
   49 | import org.springframework.core.io.ResourceLoader;
   50 | import org.springframework.test.context.ContextConfigurationAttributes;
   51 | import org.springframework.test.context.ContextCustomizer;
   52 | import org.springframework.test.context.ContextLoadException;
   53 | import org.springframework.test.context.ContextLoader;
   54 | import org.springframework.test.context.MergedContextConfiguration;
   55 | import org.springframework.test.context.SmartContextLoader;
   56 | import org.springframework.test.context.aot.AotContextLoader;
   57 | import org.springframework.test.context.support.AbstractContextLoader;
   58 | import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
   59 | import org.springframework.test.context.support.TestPropertySourceUtils;
   60 | import org.springframework.test.context.web.WebMergedContextConfiguration;
   61 | import org.springframework.util.Assert;
   62 | import org.springframework.util.ClassUtils;
   63 | import org.springframework.util.ObjectUtils;
   64 | import org.springframework.util.ReflectionUtils;
   65 | import org.springframework.util.StringUtils;
   66 | import org.springframework.util.function.ThrowingSupplier;
   67 | import org.springframework.web.context.ConfigurableWebApplicationContext;
   68 | import org.springframework.web.context.support.GenericWebApplicationContext;
   69 | 
   70 | /**
   71 |  * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
   72 |  * normally startup using {@link SpringApplication}). Although this loader can be used
   73 |  * directly, most test will instead want to use it with
   74 |  * {@link SpringBootTest @SpringBootTest}.
   75 |  * <p>
   76 |  * The loader supports both standard {@link MergedContextConfiguration} as well as
   77 |  * {@link WebMergedContextConfiguration}. If {@link WebMergedContextConfiguration} is used
   78 |  * the context will either use a mock servlet environment, or start the full embedded web
   79 |  * server.
   80 |  * <p>
   81 |  * If {@code @ActiveProfiles} are provided in the test class they will be used to create
   82 |  * the application context.
   83 |  *
   84 |  * @author Dave Syer
   85 |  * @author Phillip Webb
   86 |  * @author Andy Wilkinson
   87 |  * @author Stephane Nicoll
   88 |  * @author Madhura Bhave
   89 |  * @author Scott Frederick
   90 |  * @since 1.4.0
   91 |  * @see SpringBootTest
   92 |  */
   93 | public class SpringBootContextLoader extends AbstractContextLoader implements AotContextLoader {
   94 | 
   95 | 	private static final Consumer<SpringApplication> ALREADY_CONFIGURED = (springApplication) -> {
   96 | 	};
   97 | 
   98 | 	private static final Object NONE = new Object();
   99 | 
  100 | 	@Override
  101 | 	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
  102 | 		return loadContext(mergedConfig, Mode.STANDARD, null, null);
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
  107 | 			RuntimeHints runtimeHints) throws Exception {
  108 | 		return loadContext(mergedConfig, Mode.AOT_PROCESSING, null, runtimeHints);
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	public ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
  113 | 			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception {
  114 | 		return loadContext(mergedConfig, Mode.AOT_RUNTIME, initializer, null);
  115 | 	}
  116 | 
  117 | 	private ApplicationContext loadContext(MergedContextConfiguration mergedConfig, Mode mode,
  118 | 			@Nullable ApplicationContextInitializer<ConfigurableApplicationContext> initializer,
  119 | 			@Nullable RuntimeHints runtimeHints) throws Exception {
  120 | 		assertHasClassesOrLocations(mergedConfig);
  121 | 		SpringBootTestAnnotation annotation = SpringBootTestAnnotation.get(mergedConfig);
  122 | 		String[] args = annotation.getArgs();
  123 | 		UseMainMethod useMainMethod = annotation.getUseMainMethod();
  124 | 		Method mainMethod = getMainMethod(mergedConfig, useMainMethod);
  125 | 		if (mainMethod != null) {
  126 | 			if (runtimeHints != null) {
  127 | 				runtimeHints.reflection().registerMethod(mainMethod, ExecutableMode.INVOKE);
  128 | 			}
  129 | 			ContextLoaderHook hook = new ContextLoaderHook(mode, initializer,
  130 | 					(application) -> configure(mergedConfig, application));
  131 | 			return hook.runMain(() -> {
  132 | 				if (mainMethod.getParameterCount() == 0) {
  133 | 					ReflectionUtils.invokeMethod(mainMethod, null);
  134 | 				}
  135 | 				else {
  136 | 					ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { args });
  137 | 				}
  138 | 			});
  139 | 		}
  140 | 		SpringApplication application = getSpringApplication();
  141 | 		configure(mergedConfig, application);
  142 | 		ContextLoaderHook hook = new ContextLoaderHook(mode, initializer, ALREADY_CONFIGURED);
  143 | 		return hook.run(() -> application.run(args));
  144 | 	}
  145 | 
  146 | 	private void assertHasClassesOrLocations(MergedContextConfiguration mergedConfig) {
  147 | 		Assert.state(mergedConfig.hasResources(),
  148 | 				() -> "No configuration classes or locations found. Check your test's configuration.");
  149 | 	}
  150 | 
  151 | 	private @Nullable Method getMainMethod(MergedContextConfiguration mergedConfig, UseMainMethod useMainMethod) {
  152 | 		if (useMainMethod == UseMainMethod.NEVER) {
  153 | 			return null;
  154 | 		}
  155 | 		Assert.state(mergedConfig.getParent() == null,
  156 | 				() -> "UseMainMethod.%s cannot be used with @ContextHierarchy tests".formatted(useMainMethod));
  157 | 		Class<?> springBootConfiguration = Arrays.stream(mergedConfig.getClasses())
  158 | 			.filter(this::isSpringBootConfiguration)
  159 | 			.findFirst()
  160 | 			.orElse(null);
  161 | 		Assert.state(springBootConfiguration != null || useMainMethod == UseMainMethod.WHEN_AVAILABLE,
  162 | 				"Cannot use main method as no @SpringBootConfiguration-annotated class is available");
  163 | 		Method mainMethod = findMainMethod(springBootConfiguration);
  164 | 		Assert.state(mainMethod != null || useMainMethod == UseMainMethod.WHEN_AVAILABLE,
  165 | 				() -> "Main method not found on '%s'"
  166 | 					.formatted((springBootConfiguration != null) ? springBootConfiguration.getName() : null));
  167 | 		return mainMethod;
  168 | 	}
  169 | 
  170 | 	private static @Nullable Method findMainMethod(@Nullable Class<?> type) {
  171 | 		Method mainMethod = (type != null) ? findMainJavaMethod(type) : null;
  172 | 		if (mainMethod == null && KotlinDetector.isKotlinPresent()) {
  173 | 			try {
  174 | 				Assert.state(type != null, "'type' must not be null");
  175 | 				Class<?> kotlinClass = ClassUtils.forName(type.getName() + "Kt", type.getClassLoader());
  176 | 				mainMethod = ReflectionUtils.findMethod(kotlinClass, "main", String[].class);
  177 | 			}
  178 | 			catch (ClassNotFoundException ex) {
  179 | 				// Ignore
  180 | 			}
  181 | 		}
  182 | 		return mainMethod;
  183 | 	}
  184 | 
  185 | 	private static @Nullable Method findMainJavaMethod(Class<?> type) {
  186 | 		try {
  187 | 			Method method = getMainMethod(type);
  188 | 			if (Modifier.isStatic(method.getModifiers())) {
  189 | 				method.setAccessible(true);
  190 | 				return method;
  191 | 			}
  192 | 		}
  193 | 		catch (Exception ex) {
  194 | 			// Ignore
  195 | 		}
  196 | 		return null;
  197 | 	}
  198 | 
  199 | 	private static Method getMainMethod(Class<?> type) throws NoSuchMethodException {
  200 | 		try {
  201 | 			return type.getDeclaredMethod("main", String[].class);
  202 | 		}
  203 | 		catch (NoSuchMethodException ex) {
  204 | 			return type.getDeclaredMethod("main");
  205 | 		}
  206 | 
  207 | 	}
  208 | 
  209 | 	private boolean isSpringBootConfiguration(Class<?> candidate) {
  210 | 		return MergedAnnotations.from(candidate, SearchStrategy.TYPE_HIERARCHY)
  211 | 			.isPresent(SpringBootConfiguration.class);
  212 | 	}
  213 | 
  214 | 	private void configure(MergedContextConfiguration mergedConfig, SpringApplication application) {
  215 | 		application.setMainApplicationClass(mergedConfig.getTestClass());
  216 | 		application.addPrimarySources(Arrays.asList(mergedConfig.getClasses()));
  217 | 		application.getSources().addAll(Arrays.asList(mergedConfig.getLocations()));
  218 | 		List<ApplicationContextInitializer<?>> initializers = getInitializers(mergedConfig, application);
  219 | 		if (mergedConfig instanceof WebMergedContextConfiguration) {
  220 | 			application.setWebApplicationType(WebApplicationType.SERVLET);
  221 | 			if (!isEmbeddedWebEnvironment(mergedConfig)) {
  222 | 				new WebConfigurer().configure(mergedConfig, initializers);
  223 | 			}
  224 | 		}
  225 | 		else if (mergedConfig instanceof ReactiveWebMergedContextConfiguration) {
  226 | 			application.setWebApplicationType(WebApplicationType.REACTIVE);
  227 | 		}
  228 | 		else {
  229 | 			application.setWebApplicationType(WebApplicationType.NONE);
  230 | 		}
  231 | 		application.setApplicationContextFactory(getApplicationContextFactory(mergedConfig));
  232 | 		if (mergedConfig.getParent() != null) {
  233 | 			application.setBannerMode(Banner.Mode.OFF);
  234 | 		}
  235 | 		application.setInitializers(initializers);
  236 | 		ConfigurableEnvironment environment = getEnvironment();
  237 | 		if (environment != null) {
  238 | 			prepareEnvironment(mergedConfig, application, environment, false);
  239 | 			application.setEnvironment(environment);
  240 | 		}
  241 | 		else {
  242 | 			application.addListeners(new PrepareEnvironmentListener(mergedConfig));
  243 | 		}
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * Return the {@link ApplicationContextFactory} that should be used for the test. By
  248 | 	 * default this method will return a factory that will create an appropriate
  249 | 	 * {@link ApplicationContext} for the {@link WebApplicationType}.
  250 | 	 * @param mergedConfig the merged context configuration
  251 | 	 * @return the application context factory to use
  252 | 	 * @since 3.2.0
  253 | 	 */
  254 | 	protected ApplicationContextFactory getApplicationContextFactory(MergedContextConfiguration mergedConfig) {
  255 | 		return (webApplicationType) -> {
  256 | 			if (webApplicationType != WebApplicationType.NONE && !isEmbeddedWebEnvironment(mergedConfig)) {
  257 | 				if (webApplicationType == WebApplicationType.REACTIVE) {
  258 | 					return new GenericReactiveWebApplicationContext();
  259 | 				}
  260 | 				if (webApplicationType == WebApplicationType.SERVLET) {
  261 | 					return new GenericWebApplicationContext();
  262 | 				}
  263 | 			}
  264 | 			return ApplicationContextFactory.DEFAULT.create(webApplicationType);
  265 | 		};
  266 | 	}
  267 | 
  268 | 	private void prepareEnvironment(MergedContextConfiguration mergedConfig, SpringApplication application,
  269 | 			ConfigurableEnvironment environment, boolean applicationEnvironment) {
  270 | 		setActiveProfiles(environment, mergedConfig.getActiveProfiles(), applicationEnvironment);
  271 | 		ResourceLoader resourceLoader = (application.getResourceLoader() != null) ? application.getResourceLoader()
  272 | 				: new DefaultResourceLoader(null);
  273 | 		TestPropertySourceUtils.addPropertySourcesToEnvironment(environment, resourceLoader,
  274 | 				mergedConfig.getPropertySourceDescriptors());
  275 | 		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, getInlinedProperties(mergedConfig));
  276 | 	}
  277 | 
  278 | 	private void setActiveProfiles(ConfigurableEnvironment environment, String[] profiles,
  279 | 			boolean applicationEnvironment) {
  280 | 		if (ObjectUtils.isEmpty(profiles)) {
  281 | 			return;
  282 | 		}
  283 | 		if (!applicationEnvironment) {
  284 | 			environment.setActiveProfiles(profiles);
  285 | 		}
  286 | 		String[] pairs = new String[profiles.length];
  287 | 		for (int i = 0; i < profiles.length; i++) {
  288 | 			pairs[i] = "spring.profiles.active[" + i + "]=" + profiles[i];
  289 | 		}
  290 | 		TestPropertyValues.of(pairs).applyTo(environment, Type.MAP, "active-test-profiles");
  291 | 	}
  292 | 
  293 | 	/**
  294 | 	 * Builds new {@link org.springframework.boot.SpringApplication} instance. This method
  295 | 	 * is only called when a {@code main} method isn't being used to create the
  296 | 	 * {@link SpringApplication}.
  297 | 	 * @return a {@link SpringApplication} instance
  298 | 	 */
  299 | 	protected SpringApplication getSpringApplication() {
  300 | 		return new SpringApplication();
  301 | 	}
  302 | 
  303 | 	/**
  304 | 	 * Returns the {@link ConfigurableEnvironment} instance that should be applied to
  305 | 	 * {@link SpringApplication} or {@code null} to use the default. You can override this
  306 | 	 * method if you need a custom environment.
  307 | 	 * @return a {@link ConfigurableEnvironment} instance
  308 | 	 */
  309 | 	protected @Nullable ConfigurableEnvironment getEnvironment() {
  310 | 		return null;
  311 | 	}
  312 | 
  313 | 	protected String[] getInlinedProperties(MergedContextConfiguration mergedConfig) {
  314 | 		ArrayList<String> properties = new ArrayList<>();
  315 | 		// JMX bean names will clash if the same bean is used in multiple contexts
  316 | 		properties.add("spring.jmx.enabled=false");
  317 | 		properties.addAll(Arrays.asList(mergedConfig.getPropertySourceProperties()));
  318 | 		return StringUtils.toStringArray(properties);
  319 | 	}
  320 | 
  321 | 	/**
  322 | 	 * Return the {@link ApplicationContextInitializer initializers} that will be applied
  323 | 	 * to the context. By default this method will adapt {@link ContextCustomizer context
  324 | 	 * customizers}, add {@link SpringApplication#getInitializers() application
  325 | 	 * initializers} and add
  326 | 	 * {@link MergedContextConfiguration#getContextInitializerClasses() initializers
  327 | 	 * specified on the test}.
  328 | 	 * @param mergedConfig the source context configuration
  329 | 	 * @param application the application instance
  330 | 	 * @return the initializers to apply
  331 | 	 * @since 2.0.0
  332 | 	 */
  333 | 	protected List<ApplicationContextInitializer<?>> getInitializers(MergedContextConfiguration mergedConfig,
  334 | 			SpringApplication application) {
  335 | 		List<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
  336 | 		for (ContextCustomizer contextCustomizer : mergedConfig.getContextCustomizers()) {
  337 | 			initializers.add(new ContextCustomizerAdapter(contextCustomizer, mergedConfig));
  338 | 		}
  339 | 		initializers.addAll(application.getInitializers());
  340 | 		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : mergedConfig
  341 | 			.getContextInitializerClasses()) {
  342 | 			initializers.add(BeanUtils.instantiateClass(initializerClass));
  343 | 		}
  344 | 		if (mergedConfig.getParent() != null) {
  345 | 			ApplicationContext parentApplicationContext = mergedConfig.getParentApplicationContext();
  346 | 			initializers.add(new ParentContextApplicationContextInitializer(parentApplicationContext));
  347 | 		}
  348 | 		return initializers;
  349 | 	}
  350 | 
  351 | 	private boolean isEmbeddedWebEnvironment(MergedContextConfiguration mergedConfig) {
  352 | 		return SpringBootTestAnnotation.get(mergedConfig).getWebEnvironment().isEmbedded();
  353 | 	}
  354 | 
  355 | 	@Override
  356 | 	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
  357 | 		super.processContextConfiguration(configAttributes);
  358 | 		if (!configAttributes.hasResources()) {
  359 | 			Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(configAttributes.getDeclaringClass());
  360 | 			configAttributes.setClasses(defaultConfigClasses);
  361 | 		}
  362 | 	}
  363 | 
  364 | 	/**
  365 | 	 * Detect the default configuration classes for the supplied test class. By default
  366 | 	 * simply delegates to
  367 | 	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses}.
  368 | 	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
  369 | 	 * @return an array of default configuration classes, potentially empty but never
  370 | 	 * {@code null}
  371 | 	 * @see AnnotationConfigContextLoaderUtils
  372 | 	 */
  373 | 	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
  374 | 		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
  375 | 	}
  376 | 
  377 | 	@Override
  378 | 	protected String[] getResourceSuffixes() {
  379 | 		return new String[] { "-context.xml", "Context.groovy" };
  380 | 	}
  381 | 
  382 | 	@Override
  383 | 	protected String getResourceSuffix() {
  384 | 		throw new IllegalStateException();
  385 | 	}
  386 | 
  387 | 	/**
  388 | 	 * Modes that the {@link SpringBootContextLoader} can operate.
  389 | 	 */
  390 | 	private enum Mode {
  391 | 
  392 | 		/**
  393 | 		 * Load for regular usage.
  394 | 		 * @see SmartContextLoader#loadContext
  395 | 		 */
  396 | 		STANDARD,
  397 | 
  398 | 		/**
  399 | 		 * Load for AOT processing.
  400 | 		 * @see AotContextLoader#loadContextForAotProcessing
  401 | 		 */
  402 | 		AOT_PROCESSING,
  403 | 
  404 | 		/**
  405 | 		 * Load for AOT runtime.
  406 | 		 * @see AotContextLoader#loadContextForAotRuntime
  407 | 		 */
  408 | 		AOT_RUNTIME
  409 | 
  410 | 	}
  411 | 
  412 | 	/**
  413 | 	 * Inner class to configure {@link WebMergedContextConfiguration}.
  414 | 	 */
  415 | 	private static final class WebConfigurer {
  416 | 
  417 | 		void configure(MergedContextConfiguration mergedConfig, List<ApplicationContextInitializer<?>> initializers) {
  418 | 			WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) mergedConfig;
  419 | 			addMockServletContext(initializers, webMergedConfig);
  420 | 		}
  421 | 
  422 | 		private void addMockServletContext(List<ApplicationContextInitializer<?>> initializers,
  423 | 				WebMergedContextConfiguration webMergedConfig) {
  424 | 			SpringBootMockServletContext servletContext = new SpringBootMockServletContext(
  425 | 					webMergedConfig.getResourceBasePath());
  426 | 			initializers.add(0, new DefensiveWebApplicationContextInitializer(
  427 | 					new ServletContextApplicationContextInitializer(servletContext, true)));
  428 | 		}
  429 | 
  430 | 		/**
  431 | 		 * Decorator for {@link ServletContextApplicationContextInitializer} that prevents
  432 | 		 * a failure when the context type is not as was predicted when the initializer
  433 | 		 * was registered. This can occur when spring.main.web-application-type is set to
  434 | 		 * something other than servlet.
  435 | 		 */
  436 | 		private static final class DefensiveWebApplicationContextInitializer
  437 | 				implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  438 | 
  439 | 			private final ServletContextApplicationContextInitializer delegate;
  440 | 
  441 | 			private DefensiveWebApplicationContextInitializer(ServletContextApplicationContextInitializer delegate) {
  442 | 				this.delegate = delegate;
  443 | 			}
  444 | 
  445 | 			@Override
  446 | 			public void initialize(ConfigurableApplicationContext applicationContext) {
  447 | 				if (applicationContext instanceof ConfigurableWebApplicationContext webApplicationContext) {
  448 | 					this.delegate.initialize(webApplicationContext);
  449 | 				}
  450 | 			}
  451 | 
  452 | 		}
  453 | 
  454 | 	}
  455 | 
  456 | 	/**
  457 | 	 * Adapts a {@link ContextCustomizer} to a {@link ApplicationContextInitializer} so
  458 | 	 * that it can be triggered through {@link SpringApplication}.
  459 | 	 */
  460 | 	private static class ContextCustomizerAdapter
  461 | 			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  462 | 
  463 | 		private final ContextCustomizer contextCustomizer;
  464 | 
  465 | 		private final MergedContextConfiguration mergedConfig;
  466 | 
  467 | 		ContextCustomizerAdapter(ContextCustomizer contextCustomizer, MergedContextConfiguration mergedConfig) {
  468 | 			this.contextCustomizer = contextCustomizer;
  469 | 			this.mergedConfig = mergedConfig;
  470 | 		}
  471 | 
  472 | 		@Override
  473 | 		public void initialize(ConfigurableApplicationContext applicationContext) {
  474 | 			this.contextCustomizer.customizeContext(applicationContext, this.mergedConfig);
  475 | 		}
  476 | 
  477 | 	}
  478 | 
  479 | 	/**
  480 | 	 * {@link ApplicationContextInitializer} used to set the parent context.
  481 | 	 */
  482 | 	@Order(Ordered.HIGHEST_PRECEDENCE)
  483 | 	private static class ParentContextApplicationContextInitializer
  484 | 			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  485 | 
  486 | 		private final @Nullable ApplicationContext parent;
  487 | 
  488 | 		ParentContextApplicationContextInitializer(@Nullable ApplicationContext parent) {
  489 | 			this.parent = parent;
  490 | 		}
  491 | 
  492 | 		@Override
  493 | 		public void initialize(ConfigurableApplicationContext applicationContext) {
  494 | 			applicationContext.setParent(this.parent);
  495 | 		}
  496 | 
  497 | 	}
  498 | 
  499 | 	/**
  500 | 	 * {@link ApplicationListener} used to prepare the application created environment.
  501 | 	 */
  502 | 	private class PrepareEnvironmentListener
  503 | 			implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, PriorityOrdered {
  504 | 
  505 | 		private final MergedContextConfiguration mergedConfig;
  506 | 
  507 | 		PrepareEnvironmentListener(MergedContextConfiguration mergedConfig) {
  508 | 			this.mergedConfig = mergedConfig;
  509 | 		}
  510 | 
  511 | 		@Override
  512 | 		public int getOrder() {
  513 | 			return Ordered.HIGHEST_PRECEDENCE;
  514 | 		}
  515 | 
  516 | 		@Override
  517 | 		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
  518 | 			prepareEnvironment(this.mergedConfig, event.getSpringApplication(), event.getEnvironment(), true);
  519 | 		}
  520 | 
  521 | 	}
  522 | 
  523 | 	/**
  524 | 	 * {@link SpringApplicationHook} used to capture {@link ApplicationContext} instances
  525 | 	 * and to trigger early exit for the {@link Mode#AOT_PROCESSING} mode.
  526 | 	 */
  527 | 	private static class ContextLoaderHook implements SpringApplicationHook {
  528 | 
  529 | 		private final Mode mode;
  530 | 
  531 | 		private final @Nullable ApplicationContextInitializer<ConfigurableApplicationContext> initializer;
  532 | 
  533 | 		private final Consumer<SpringApplication> configurer;
  534 | 
  535 | 		private final List<ApplicationContext> contexts = Collections.synchronizedList(new ArrayList<>());
  536 | 
  537 | 		private final List<ApplicationContext> failedContexts = Collections.synchronizedList(new ArrayList<>());
  538 | 
  539 | 		ContextLoaderHook(Mode mode,
  540 | 				@Nullable ApplicationContextInitializer<ConfigurableApplicationContext> initializer,
  541 | 				Consumer<SpringApplication> configurer) {
  542 | 			this.mode = mode;
  543 | 			this.initializer = initializer;
  544 | 			this.configurer = configurer;
  545 | 		}
  546 | 
  547 | 		@Override
  548 | 		public SpringApplicationRunListener getRunListener(SpringApplication application) {
  549 | 			return new SpringApplicationRunListener() {
  550 | 
  551 | 				@Override
  552 | 				public void starting(ConfigurableBootstrapContext bootstrapContext) {
  553 | 					ContextLoaderHook.this.configurer.accept(application);
  554 | 					if (ContextLoaderHook.this.mode == Mode.AOT_RUNTIME) {
  555 | 						Assert.state(ContextLoaderHook.this.initializer != null, "'initializer' must not be null");
  556 | 						application.addInitializers(
  557 | 								(AotApplicationContextInitializer<?>) ContextLoaderHook.this.initializer::initialize);
  558 | 					}
  559 | 				}
  560 | 
  561 | 				@Override
  562 | 				public void contextLoaded(ConfigurableApplicationContext context) {
  563 | 					ContextLoaderHook.this.contexts.add(context);
  564 | 					if (ContextLoaderHook.this.mode == Mode.AOT_PROCESSING) {
  565 | 						throw new AbandonedRunException(context);
  566 | 					}
  567 | 				}
  568 | 
  569 | 				@Override
  570 | 				public void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
  571 | 					if (context != null) {
  572 | 						ContextLoaderHook.this.failedContexts.add(context);
  573 | 					}
  574 | 				}
  575 | 
  576 | 			};
  577 | 		}
  578 | 
  579 | 		private ApplicationContext runMain(Runnable action) throws Exception {
  580 | 			return run(() -> {
  581 | 				action.run();
  582 | 				return NONE;
  583 | 			});
  584 | 		}
  585 | 
  586 | 		private ApplicationContext run(ThrowingSupplier<?> action) throws Exception {
  587 | 			try {
  588 | 				Object result = SpringApplication.withHook(this, action);
  589 | 				if (result instanceof ApplicationContext context) {
  590 | 					return context;
  591 | 				}
  592 | 			}
  593 | 			catch (AbandonedRunException ex) {
  594 | 				// Ignore
  595 | 			}
  596 | 			catch (Exception ex) {
  597 | 				if (this.failedContexts.size() == 1) {
  598 | 					throw new ContextLoadException(this.failedContexts.get(0), ex);
  599 | 				}
  600 | 				throw ex;
  601 | 			}
  602 | 			List<ApplicationContext> rootContexts = this.contexts.stream()
  603 | 				.filter((context) -> context.getParent() == null)
  604 | 				.toList();
  605 | 			Assert.state(!rootContexts.isEmpty(), "No root application context located");
  606 | 			Assert.state(rootContexts.size() == 1, "No unique root application context located");
  607 | 			return rootContexts.get(0);
  608 | 		}
  609 | 
  610 | 	}
  611 | 
  612 | }
```
