<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/SpringApplication.java -->
<!-- 正文引用：[[源码:SpringApplication.java#L起-L止]] ；代码块头部：(src: SpringApplication.java#L起-L止) -->

# SpringApplication.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/SpringApplication.java` |
| 行数 | 1874 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T13:48:36.506Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.lang.StackWalker.StackFrame;
    8 | import java.lang.management.ManagementFactory;
    9 | import java.lang.reflect.Method;
   10 | import java.time.Duration;
   11 | import java.util.ArrayList;
   12 | import java.util.Arrays;
   13 | import java.util.Collection;
   14 | import java.util.Collections;
   15 | import java.util.Comparator;
   16 | import java.util.HashMap;
   17 | import java.util.IdentityHashMap;
   18 | import java.util.LinkedHashSet;
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.Optional;
   23 | import java.util.Properties;
   24 | import java.util.Set;
   25 | import java.util.concurrent.atomic.AtomicBoolean;
   26 | import java.util.concurrent.atomic.AtomicReference;
   27 | import java.util.stream.Stream;
   28 | 
   29 | import org.apache.commons.logging.Log;
   30 | import org.apache.commons.logging.LogFactory;
   31 | import org.crac.management.CRaCMXBean;
   32 | import org.jspecify.annotations.Nullable;
   33 | 
   34 | import org.springframework.aot.AotDetector;
   35 | import org.springframework.beans.BeansException;
   36 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   37 | import org.springframework.beans.factory.config.BeanDefinition;
   38 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   39 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   40 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   41 | import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
   42 | import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
   43 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   44 | import org.springframework.beans.factory.support.BeanNameGenerator;
   45 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   46 | import org.springframework.beans.factory.support.RootBeanDefinition;
   47 | import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
   48 | import org.springframework.boot.Banner.Mode;
   49 | import org.springframework.boot.bootstrap.BootstrapRegistry;
   50 | import org.springframework.boot.bootstrap.BootstrapRegistryInitializer;
   51 | import org.springframework.boot.bootstrap.DefaultBootstrapContext;
   52 | import org.springframework.boot.context.properties.bind.Bindable;
   53 | import org.springframework.boot.context.properties.bind.Binder;
   54 | import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
   55 | import org.springframework.boot.convert.ApplicationConversionService;
   56 | import org.springframework.boot.env.DefaultPropertiesPropertySource;
   57 | import org.springframework.boot.system.JavaVersion;
   58 | import org.springframework.context.ApplicationContext;
   59 | import org.springframework.context.ApplicationContextInitializer;
   60 | import org.springframework.context.ApplicationListener;
   61 | import org.springframework.context.ConfigurableApplicationContext;
   62 | import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
   63 | import org.springframework.context.annotation.AnnotationConfigUtils;
   64 | import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
   65 | import org.springframework.context.annotation.ConfigurationClassPostProcessor;
   66 | import org.springframework.context.aot.AotApplicationContextInitializer;
   67 | import org.springframework.context.event.ApplicationContextEvent;
   68 | import org.springframework.context.event.ContextClosedEvent;
   69 | import org.springframework.context.event.ContextRefreshedEvent;
   70 | import org.springframework.context.support.AbstractApplicationContext;
   71 | import org.springframework.context.support.GenericApplicationContext;
   72 | import org.springframework.core.GenericTypeResolver;
   73 | import org.springframework.core.NativeDetector;
   74 | import org.springframework.core.OrderComparator;
   75 | import org.springframework.core.OrderComparator.OrderSourceProvider;
   76 | import org.springframework.core.Ordered;
   77 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   78 | import org.springframework.core.annotation.Order;
   79 | import org.springframework.core.env.CommandLinePropertySource;
   80 | import org.springframework.core.env.CompositePropertySource;
   81 | import org.springframework.core.env.ConfigurableEnvironment;
   82 | import org.springframework.core.env.Environment;
   83 | import org.springframework.core.env.MutablePropertySources;
   84 | import org.springframework.core.env.PropertySource;
   85 | import org.springframework.core.env.SimpleCommandLinePropertySource;
   86 | import org.springframework.core.io.DefaultResourceLoader;
   87 | import org.springframework.core.io.ResourceLoader;
   88 | import org.springframework.core.io.support.SpringFactoriesLoader;
   89 | import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
   90 | import org.springframework.core.metrics.ApplicationStartup;
   91 | import org.springframework.util.Assert;
   92 | import org.springframework.util.ClassUtils;
   93 | import org.springframework.util.CollectionUtils;
   94 | import org.springframework.util.ObjectUtils;
   95 | import org.springframework.util.StringUtils;
   96 | import org.springframework.util.function.ThrowingConsumer;
   97 | import org.springframework.util.function.ThrowingSupplier;
   98 | 
   99 | /**
  100 |  * Class that can be used to bootstrap and launch a Spring application from a Java main
  101 |  * method. By default class will perform the following steps to bootstrap your
  102 |  * application:
  103 |  *
  104 |  * <ul>
  105 |  * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
  106 |  * classpath)</li>
  107 |  * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
  108 |  * Spring properties</li>
  109 |  * <li>Refresh the application context, loading all singleton beans</li>
  110 |  * <li>Trigger any {@link CommandLineRunner} beans</li>
  111 |  * </ul>
  112 |  *
  113 |  * In most circumstances the static {@link #run(Class, String[])} method can be called
  114 |  * directly from your {@literal main} method to bootstrap your application:
  115 |  *
  116 |  * <pre class="code">
  117 |  * &#064;Configuration
  118 |  * &#064;EnableAutoConfiguration
  119 |  * public class MyApplication {
  120 |  *
  121 |  *   // ... Bean definitions
  122 |  *
  123 |  *   public static void main(String[] args) {
  124 |  *     SpringApplication.run(MyApplication.class, args);
  125 |  *   }
  126 |  * }
  127 |  * </pre>
  128 |  *
  129 |  * <p>
  130 |  * For more advanced configuration a {@link SpringApplication} instance can be created and
  131 |  * customized before being run:
  132 |  *
  133 |  * <pre class="code">
  134 |  * public static void main(String[] args) {
  135 |  *   SpringApplication application = new SpringApplication(MyApplication.class);
  136 |  *   // ... customize application settings here
  137 |  *   application.run(args)
  138 |  * }
  139 |  * </pre>
  140 |  *
  141 |  * {@link SpringApplication}s can read beans from a variety of different sources. It is
  142 |  * generally recommended that a single {@code @Configuration} class is used to bootstrap
  143 |  * your application, however, you may also set {@link #getSources() sources} from:
  144 |  * <ul>
  145 |  * <li>The fully qualified class name to be loaded by
  146 |  * {@link AnnotatedBeanDefinitionReader}</li>
  147 |  * <li>The location of an XML resource to be loaded by {@link XmlBeanDefinitionReader}, or
  148 |  * a groovy script to be loaded by {@link GroovyBeanDefinitionReader}</li>
  149 |  * <li>The name of a package to be scanned by {@link ClassPathBeanDefinitionScanner}</li>
  150 |  * </ul>
  151 |  *
  152 |  * Configuration properties are also bound to the {@link SpringApplication}. This makes it
  153 |  * possible to set {@link SpringApplication} properties dynamically, like additional
  154 |  * sources ("spring.main.sources" - a CSV list) the flag to indicate a web environment
  155 |  * ("spring.main.web-application-type=none") or the flag to switch off the banner
  156 |  * ("spring.main.banner-mode=off").
  157 |  *
  158 |  * @author Phillip Webb
  159 |  * @author Dave Syer
  160 |  * @author Andy Wilkinson
  161 |  * @author Christian Dupuis
  162 |  * @author Stephane Nicoll
  163 |  * @author Jeremy Rickard
  164 |  * @author Craig Burke
  165 |  * @author Michael Simons
  166 |  * @author Madhura Bhave
  167 |  * @author Brian Clozel
  168 |  * @author Ethan Rubinson
  169 |  * @author Chris Bono
  170 |  * @author Moritz Halbritter
  171 |  * @author Tadaya Tsuyukubo
  172 |  * @author Lasse Wulff
  173 |  * @author Yanming Zhou
  174 |  * @since 1.0.0
  175 |  * @see #run(Class, String[])
  176 |  * @see #run(Class[], String[])
  177 |  * @see #SpringApplication(Class...)
  178 |  */
  179 | public class SpringApplication {
  180 | 
  181 | 	/**
  182 | 	 * Default banner location.
  183 | 	 */
  184 | 	public static final String BANNER_LOCATION_PROPERTY_VALUE = SpringApplicationBannerPrinter.DEFAULT_BANNER_LOCATION;
  185 | 
  186 | 	/**
  187 | 	 * Banner location property key.
  188 | 	 */
  189 | 	public static final String BANNER_LOCATION_PROPERTY = SpringApplicationBannerPrinter.BANNER_LOCATION_PROPERTY;
  190 | 
  191 | 	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";
  192 | 
  193 | 	private static final Log logger = LogFactory.getLog(SpringApplication.class);
  194 | 
  195 | 	static final SpringApplicationShutdownHook shutdownHook = new SpringApplicationShutdownHook();
  196 | 
  197 | 	private static final ThreadLocal<SpringApplicationHook> applicationHook = new ThreadLocal<>();
  198 | 
  199 | 	private final Set<Class<?>> primarySources;
  200 | 
  201 | 	private @Nullable Class<?> mainApplicationClass;
  202 | 
  203 | 	private boolean addCommandLineProperties = true;
  204 | 
  205 | 	private boolean addConversionService = true;
  206 | 
  207 | 	private @Nullable Banner banner;
  208 | 
  209 | 	private @Nullable ResourceLoader resourceLoader;
  210 | 
  211 | 	private @Nullable BeanNameGenerator beanNameGenerator;
  212 | 
  213 | 	private @Nullable ConfigurableEnvironment environment;
  214 | 
  215 | 	private boolean headless = true;
  216 | 
  217 | 	private List<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
  218 | 
  219 | 	private List<ApplicationListener<?>> listeners = new ArrayList<>();
  220 | 
  221 | 	private @Nullable Map<String, Object> defaultProperties;
  222 | 
  223 | 	private final List<BootstrapRegistryInitializer> bootstrapRegistryInitializers;
  224 | 
  225 | 	private Set<String> additionalProfiles = Collections.emptySet();
  226 | 
  227 | 	private boolean isCustomEnvironment;
  228 | 
  229 | 	private @Nullable String environmentPrefix;
  230 | 
  231 | 	private ApplicationContextFactory applicationContextFactory = ApplicationContextFactory.DEFAULT;
  232 | 
  233 | 	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
  234 | 
  235 | 	final ApplicationProperties properties = new ApplicationProperties();
  236 | 
  237 | 	/**
  238 | 	 * Create a new {@link SpringApplication} instance. The application context will load
  239 | 	 * beans from the specified primary sources (see {@link SpringApplication class-level}
  240 | 	 * documentation for details). The instance can be customized before calling
  241 | 	 * {@link #run(String...)}.
  242 | 	 * @param primarySources the primary bean sources
  243 | 	 * @see #run(Class, String[])
  244 | 	 * @see #SpringApplication(ResourceLoader, Class...)
  245 | 	 * @see #setSources(Set)
  246 | 	 */
  247 | 	public SpringApplication(Class<?>... primarySources) {
  248 | 		this(null, primarySources);
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * Create a new {@link SpringApplication} instance. The application context will load
  253 | 	 * beans from the specified primary sources (see {@link SpringApplication class-level}
  254 | 	 * documentation for details). The instance can be customized before calling
  255 | 	 * {@link #run(String...)}.
  256 | 	 * @param resourceLoader the resource loader to use
  257 | 	 * @param primarySources the primary bean sources
  258 | 	 * @see #run(Class, String[])
  259 | 	 * @see #setSources(Set)
  260 | 	 */
  261 | 	@SuppressWarnings({ "unchecked", "rawtypes" })
  262 | 	public SpringApplication(@Nullable ResourceLoader resourceLoader, Class<?>... primarySources) {
  263 | 		this.resourceLoader = resourceLoader;
  264 | 		Assert.notNull(primarySources, "'primarySources' must not be null");
  265 | 		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
  266 | 		this.properties.setWebApplicationType(WebApplicationType.deduce());
  267 | 		this.bootstrapRegistryInitializers = new ArrayList<>(
  268 | 				getSpringFactoriesInstances(BootstrapRegistryInitializer.class));
  269 | 		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
  270 | 		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
  271 | 		this.mainApplicationClass = deduceMainApplicationClass();
  272 | 	}
  273 | 
  274 | 	private @Nullable Class<?> deduceMainApplicationClass() {
  275 | 		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
  276 | 			.walk(this::findMainClass)
  277 | 			.orElse(null);
  278 | 	}
  279 | 
  280 | 	private Optional<Class<?>> findMainClass(Stream<StackFrame> stack) {
  281 | 		return stack.filter((frame) -> Objects.equals(frame.getMethodName(), "main"))
  282 | 			.findFirst()
  283 | 			.map(StackWalker.StackFrame::getDeclaringClass);
  284 | 	}
  285 | 
  286 | 	/**
  287 | 	 * Run the Spring application, creating and refreshing a new
  288 | 	 * {@link ApplicationContext}.
  289 | 	 * @param args the application arguments (usually passed from a Java main method)
  290 | 	 * @return a running {@link ApplicationContext}
  291 | 	 */
  292 | 	public ConfigurableApplicationContext run(String... args) {
  293 | 		Startup startup = Startup.create();
  294 | 		if (this.properties.isRegisterShutdownHook()) {
  295 | 			SpringApplication.shutdownHook.enableShutdownHookAddition();
  296 | 		}
  297 | 		DefaultBootstrapContext bootstrapContext = createBootstrapContext();
  298 | 		ConfigurableApplicationContext context = null;
  299 | 		configureHeadlessProperty();
  300 | 		SpringApplicationRunListeners listeners = getRunListeners(args);
  301 | 		listeners.starting(bootstrapContext, this.mainApplicationClass);
  302 | 		try {
  303 | 			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
  304 | 			ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
  305 | 			Banner printedBanner = printBanner(environment);
  306 | 			context = createApplicationContext();
  307 | 			context.setApplicationStartup(this.applicationStartup);
  308 | 			prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
  309 | 			refreshContext(context);
  310 | 			afterRefresh(context, applicationArguments);
  311 | 			Duration timeTakenToStarted = startup.started();
  312 | 			if (this.properties.isLogStartupInfo()) {
  313 | 				new StartupInfoLogger(this.mainApplicationClass, environment).logStarted(getApplicationLog(), startup);
  314 | 			}
  315 | 			listeners.started(context, timeTakenToStarted);
  316 | 			callRunners(context, applicationArguments);
  317 | 		}
  318 | 		catch (Throwable ex) {
  319 | 			throw handleRunFailure(context, ex, listeners);
  320 | 		}
  321 | 		try {
  322 | 			listeners.ready(context, startup.ready());
  323 | 		}
  324 | 		catch (Throwable ex) {
  325 | 			throw handleRunFailure(context, ex, null);
  326 | 		}
  327 | 		return context;
  328 | 	}
  329 | 
  330 | 	private DefaultBootstrapContext createBootstrapContext() {
  331 | 		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
  332 | 		this.bootstrapRegistryInitializers.forEach((initializer) -> initializer.initialize(bootstrapContext));
  333 | 		return bootstrapContext;
  334 | 	}
  335 | 
  336 | 	private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
  337 | 			DefaultBootstrapContext bootstrapContext, ApplicationArguments applicationArguments) {
  338 | 		// Create and configure the environment
  339 | 		ConfigurableEnvironment environment = getOrCreateEnvironment();
  340 | 		configureEnvironment(environment, applicationArguments.getSourceArgs());
  341 | 		ConfigurationPropertySources.attach(environment);
  342 | 		listeners.environmentPrepared(bootstrapContext, environment);
  343 | 		ApplicationInfoPropertySource.moveToEnd(environment);
  344 | 		DefaultPropertiesPropertySource.moveToEnd(environment);
  345 | 		Assert.state(!environment.containsProperty("spring.main.environment-prefix"),
  346 | 				"Environment prefix cannot be set via properties.");
  347 | 		bindToSpringApplication(environment);
  348 | 		if (!this.isCustomEnvironment) {
  349 | 			EnvironmentConverter environmentConverter = new EnvironmentConverter(getClassLoader());
  350 | 			environment = environmentConverter.convertEnvironmentIfNecessary(environment, deduceEnvironmentClass());
  351 | 		}
  352 | 		ConfigurationPropertySources.attach(environment);
  353 | 		return environment;
  354 | 	}
  355 | 
  356 | 	private Class<? extends ConfigurableEnvironment> deduceEnvironmentClass() {
  357 | 		WebApplicationType webApplicationType = this.properties.getWebApplicationType();
  358 | 		Class<? extends ConfigurableEnvironment> environmentType = this.applicationContextFactory
  359 | 			.getEnvironmentType(webApplicationType);
  360 | 		if (environmentType == null && this.applicationContextFactory != ApplicationContextFactory.DEFAULT) {
  361 | 			environmentType = ApplicationContextFactory.DEFAULT.getEnvironmentType(webApplicationType);
  362 | 		}
  363 | 		return (environmentType != null) ? environmentType : ApplicationEnvironment.class;
  364 | 	}
  365 | 
  366 | 	private void prepareContext(DefaultBootstrapContext bootstrapContext, ConfigurableApplicationContext context,
  367 | 			ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
  368 | 			ApplicationArguments applicationArguments, @Nullable Banner printedBanner) {
  369 | 		context.setEnvironment(environment);
  370 | 		postProcessApplicationContext(context);
  371 | 		addAotGeneratedInitializerIfNecessary(this.initializers);
  372 | 		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
  373 | 		if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
  374 | 			autowireCapableBeanFactory.setAllowCircularReferences(this.properties.isAllowCircularReferences());
  375 | 			if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
  376 | 				listableBeanFactory.setAllowBeanDefinitionOverriding(this.properties.isAllowBeanDefinitionOverriding());
  377 | 			}
  378 | 		}
  379 | 		applyInitializers(context);
  380 | 		listeners.contextPrepared(context);
  381 | 		bootstrapContext.close(context);
  382 | 		if (this.properties.isLogStartupInfo()) {
  383 | 			logStartupInfo(context);
  384 | 			logStartupProfileInfo(context);
  385 | 		}
  386 | 		// Add boot specific singleton beans
  387 | 		beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
  388 | 		if (printedBanner != null) {
  389 | 			beanFactory.registerSingleton("springBootBanner", printedBanner);
  390 | 		}
  391 | 		if (this.properties.isLazyInitialization()) {
  392 | 			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
  393 | 		}
  394 | 		if (this.properties.isKeepAlive()) {
  395 | 			context.addApplicationListener(new KeepAlive(context));
  396 | 		}
  397 | 		context.addBeanFactoryPostProcessor(new PropertySourceOrderingBeanFactoryPostProcessor(context));
  398 | 		if (!AotDetector.useGeneratedArtifacts()) {
  399 | 			// Load the sources
  400 | 			Set<Object> sources = getAllSources();
  401 | 			Assert.state(!ObjectUtils.isEmpty(sources), "No sources defined");
  402 | 			load(context, sources.toArray(new Object[0]));
  403 | 		}
  404 | 		listeners.contextLoaded(context);
  405 | 	}
  406 | 
  407 | 	private void addAotGeneratedInitializerIfNecessary(List<ApplicationContextInitializer<?>> initializers) {
  408 | 		if (AotDetector.useGeneratedArtifacts()) {
  409 | 			List<ApplicationContextInitializer<?>> aotInitializers = new ArrayList<>(
  410 | 					initializers.stream().filter(AotApplicationContextInitializer.class::isInstance).toList());
  411 | 			if (aotInitializers.isEmpty()) {
  412 | 				Assert.state(this.mainApplicationClass != null, "No application main class found");
  413 | 				String initializerClassName = this.mainApplicationClass.getName() + "__ApplicationContextInitializer";
  414 | 				if (!ClassUtils.isPresent(initializerClassName, getClassLoader())) {
  415 | 					throw new AotInitializerNotFoundException(this.mainApplicationClass, initializerClassName);
  416 | 				}
  417 | 				aotInitializers.add(AotApplicationContextInitializer.forInitializerClasses(initializerClassName));
  418 | 			}
  419 | 			initializers.removeAll(aotInitializers);
  420 | 			initializers.addAll(0, aotInitializers);
  421 | 		}
  422 | 		if (NativeDetector.inNativeImage()) {
  423 | 			NativeImageRequirementsException.throwIfNotMet();
  424 | 		}
  425 | 	}
  426 | 
  427 | 	private void refreshContext(ConfigurableApplicationContext context) {
  428 | 		if (this.properties.isRegisterShutdownHook()) {
  429 | 			shutdownHook.registerApplicationContext(context);
  430 | 		}
  431 | 		refresh(context);
  432 | 	}
  433 | 
  434 | 	private void configureHeadlessProperty() {
  435 | 		System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS,
  436 | 				System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
  437 | 	}
  438 | 
  439 | 	private SpringApplicationRunListeners getRunListeners(String[] args) {
  440 | 		ArgumentResolver argumentResolver = ArgumentResolver.of(SpringApplication.class, this);
  441 | 		argumentResolver = argumentResolver.and(String[].class, args);
  442 | 		List<SpringApplicationRunListener> listeners = getSpringFactoriesInstances(SpringApplicationRunListener.class,
  443 | 				argumentResolver);
  444 | 		SpringApplicationHook hook = applicationHook.get();
  445 | 		SpringApplicationRunListener hookListener = (hook != null) ? hook.getRunListener(this) : null;
  446 | 		if (hookListener != null) {
  447 | 			listeners = new ArrayList<>(listeners);
  448 | 			listeners.add(hookListener);
  449 | 		}
  450 | 		return new SpringApplicationRunListeners(logger, listeners, this.applicationStartup);
  451 | 	}
  452 | 
  453 | 	private <T> List<T> getSpringFactoriesInstances(Class<T> type) {
  454 | 		return getSpringFactoriesInstances(type, null);
  455 | 	}
  456 | 
  457 | 	private <T> List<T> getSpringFactoriesInstances(Class<T> type, @Nullable ArgumentResolver argumentResolver) {
  458 | 		return SpringFactoriesLoader.forDefaultResourceLocation(getClassLoader()).load(type, argumentResolver);
  459 | 	}
  460 | 
  461 | 	private ConfigurableEnvironment getOrCreateEnvironment() {
  462 | 		if (this.environment != null) {
  463 | 			return this.environment;
  464 | 		}
  465 | 		WebApplicationType webApplicationType = this.properties.getWebApplicationType();
  466 | 		ConfigurableEnvironment environment = this.applicationContextFactory.createEnvironment(webApplicationType);
  467 | 		if (environment == null && this.applicationContextFactory != ApplicationContextFactory.DEFAULT) {
  468 | 			environment = ApplicationContextFactory.DEFAULT.createEnvironment(webApplicationType);
  469 | 		}
  470 | 		return (environment != null) ? environment : new ApplicationEnvironment();
  471 | 	}
  472 | 
  473 | 	/**
  474 | 	 * Template method delegating to
  475 | 	 * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
  476 | 	 * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
  477 | 	 * Override this method for complete control over Environment customization, or one of
  478 | 	 * the above for fine-grained control over property sources or profiles, respectively.
  479 | 	 * @param environment this application's environment
  480 | 	 * @param args arguments passed to the {@code run} method
  481 | 	 * @see #configureProfiles(ConfigurableEnvironment, String[])
  482 | 	 * @see #configurePropertySources(ConfigurableEnvironment, String[])
  483 | 	 */
  484 | 	protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
  485 | 		if (this.addConversionService) {
  486 | 			environment.setConversionService(new ApplicationConversionService());
  487 | 		}
  488 | 		configurePropertySources(environment, args);
  489 | 		configureProfiles(environment, args);
  490 | 	}
  491 | 
  492 | 	/**
  493 | 	 * Add, remove or re-order any {@link PropertySource}s in this application's
  494 | 	 * environment.
  495 | 	 * @param environment this application's environment
  496 | 	 * @param args arguments passed to the {@code run} method
  497 | 	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
  498 | 	 */
  499 | 	protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
  500 | 		MutablePropertySources sources = environment.getPropertySources();
  501 | 		if (!CollectionUtils.isEmpty(this.defaultProperties)) {
  502 | 			DefaultPropertiesPropertySource.addOrMerge(this.defaultProperties, sources);
  503 | 		}
  504 | 		if (this.addCommandLineProperties && args.length > 0) {
  505 | 			String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
  506 | 			PropertySource<?> source = sources.get(name);
  507 | 			if (source != null) {
  508 | 				CompositePropertySource composite = new CompositePropertySource(name);
  509 | 				composite
  510 | 					.addPropertySource(new SimpleCommandLinePropertySource("springApplicationCommandLineArgs", args));
  511 | 				composite.addPropertySource(source);
  512 | 				sources.replace(name, composite);
  513 | 			}
  514 | 			else {
  515 | 				sources.addFirst(new SimpleCommandLinePropertySource(args));
  516 | 			}
  517 | 		}
  518 | 		environment.getPropertySources().addLast(new ApplicationInfoPropertySource(this.mainApplicationClass));
  519 | 	}
  520 | 
  521 | 	/**
  522 | 	 * Configure which profiles are active (or active by default) for this application
  523 | 	 * environment. Additional profiles may be activated during configuration file
  524 | 	 * processing through the {@code spring.profiles.active} property.
  525 | 	 * @param environment this application's environment
  526 | 	 * @param args arguments passed to the {@code run} method
  527 | 	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
  528 | 	 */
  529 | 	protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
  530 | 	}
  531 | 
  532 | 	/**
  533 | 	 * Bind the environment to the {@link ApplicationProperties}.
  534 | 	 * @param environment the environment to bind
  535 | 	 */
  536 | 	protected void bindToSpringApplication(ConfigurableEnvironment environment) {
  537 | 		try {
  538 | 			Binder.get(environment).bind("spring.main", Bindable.ofInstance(this.properties));
  539 | 		}
  540 | 		catch (Exception ex) {
  541 | 			throw new IllegalStateException("Cannot bind to SpringApplication", ex);
  542 | 		}
  543 | 	}
  544 | 
  545 | 	private @Nullable Banner printBanner(ConfigurableEnvironment environment) {
  546 | 		if (this.properties.getBannerMode(environment) == Banner.Mode.OFF) {
  547 | 			return null;
  548 | 		}
  549 | 		ResourceLoader resourceLoader = (this.resourceLoader != null) ? this.resourceLoader
  550 | 				: new DefaultResourceLoader(null);
  551 | 		SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(resourceLoader, this.banner);
  552 | 		if (this.properties.getBannerMode(environment) == Mode.LOG) {
  553 | 			return bannerPrinter.print(environment, this.mainApplicationClass, logger);
  554 | 		}
  555 | 		return bannerPrinter.print(environment, this.mainApplicationClass, System.out);
  556 | 	}
  557 | 
  558 | 	/**
  559 | 	 * Strategy method used to create the {@link ApplicationContext}. By default this
  560 | 	 * method will respect any explicitly set application context class or factory before
  561 | 	 * falling back to a suitable default.
  562 | 	 * @return the application context (not yet refreshed)
  563 | 	 * @see #setApplicationContextFactory(ApplicationContextFactory)
  564 | 	 */
  565 | 	protected ConfigurableApplicationContext createApplicationContext() {
  566 | 		ConfigurableApplicationContext context = this.applicationContextFactory
  567 | 			.create(this.properties.getWebApplicationType());
  568 | 		Assert.state(context != null, "ApplicationContextFactory created null context");
  569 | 		return context;
  570 | 	}
  571 | 
  572 | 	/**
  573 | 	 * Apply any relevant post-processing to the {@link ApplicationContext}. Subclasses
  574 | 	 * can apply additional processing as required.
  575 | 	 * @param context the application context
  576 | 	 */
  577 | 	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
  578 | 		if (this.beanNameGenerator != null) {
  579 | 			context.getBeanFactory()
  580 | 				.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, this.beanNameGenerator);
  581 | 		}
  582 | 		if (this.resourceLoader != null) {
  583 | 			if (context instanceof GenericApplicationContext genericApplicationContext) {
  584 | 				genericApplicationContext.setResourceLoader(this.resourceLoader);
  585 | 			}
  586 | 			if (context instanceof DefaultResourceLoader defaultResourceLoader) {
  587 | 				defaultResourceLoader.setClassLoader(this.resourceLoader.getClassLoader());
  588 | 			}
  589 | 		}
  590 | 		if (this.addConversionService) {
  591 | 			context.getBeanFactory().setConversionService(context.getEnvironment().getConversionService());
  592 | 		}
  593 | 	}
  594 | 
  595 | 	/**
  596 | 	 * Apply any {@link ApplicationContextInitializer}s to the context before it is
  597 | 	 * refreshed.
  598 | 	 * @param context the configured ApplicationContext (not refreshed yet)
  599 | 	 * @see ConfigurableApplicationContext#refresh()
  600 | 	 */
  601 | 	@SuppressWarnings({ "rawtypes", "unchecked" })
  602 | 	protected void applyInitializers(ConfigurableApplicationContext context) {
  603 | 		for (ApplicationContextInitializer initializer : getInitializers()) {
  604 | 			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
  605 | 					ApplicationContextInitializer.class);
  606 | 			Assert.state(requiredType != null,
  607 | 					() -> "No generic type found for initializr of type " + initializer.getClass());
  608 | 			Assert.state(requiredType.isInstance(context), "Unable to call initializer");
  609 | 			initializer.initialize(context);
  610 | 		}
  611 | 	}
  612 | 
  613 | 	/**
  614 | 	 * Called to log startup information, subclasses may override to add additional
  615 | 	 * logging.
  616 | 	 * @param context the application context
  617 | 	 * @since 3.4.0
  618 | 	 */
  619 | 	protected void logStartupInfo(ConfigurableApplicationContext context) {
  620 | 		boolean isRoot = context.getParent() == null;
  621 | 		if (isRoot) {
  622 | 			new StartupInfoLogger(this.mainApplicationClass, context.getEnvironment()).logStarting(getApplicationLog());
  623 | 		}
  624 | 	}
  625 | 
  626 | 	/**
  627 | 	 * Called to log active profile information.
  628 | 	 * @param context the application context
  629 | 	 */
  630 | 	protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
  631 | 		Log log = getApplicationLog();
  632 | 		if (log.isInfoEnabled()) {
  633 | 			List<String> activeProfiles = quoteProfiles(context.getEnvironment().getActiveProfiles());
  634 | 			if (ObjectUtils.isEmpty(activeProfiles)) {
  635 | 				List<String> defaultProfiles = quoteProfiles(context.getEnvironment().getDefaultProfiles());
  636 | 				String message = String.format("%s default %s: ", defaultProfiles.size(),
  637 | 						(defaultProfiles.size() <= 1) ? "profile" : "profiles");
  638 | 				log.info("No active profile set, falling back to " + message
  639 | 						+ StringUtils.collectionToDelimitedString(defaultProfiles, ", "));
  640 | 			}
  641 | 			else {
  642 | 				String message = (activeProfiles.size() == 1) ? "1 profile is active: "
  643 | 						: activeProfiles.size() + " profiles are active: ";
  644 | 				log.info("The following " + message + StringUtils.collectionToDelimitedString(activeProfiles, ", "));
  645 | 			}
  646 | 		}
  647 | 	}
  648 | 
  649 | 	private List<String> quoteProfiles(String[] profiles) {
  650 | 		return Arrays.stream(profiles).map((profile) -> "\"" + profile + "\"").toList();
  651 | 	}
  652 | 
  653 | 	/**
  654 | 	 * Returns the {@link Log} for the application. By default will be deduced.
  655 | 	 * @return the application log
  656 | 	 */
  657 | 	protected Log getApplicationLog() {
  658 | 		if (this.mainApplicationClass == null) {
  659 | 			return logger;
  660 | 		}
  661 | 		return LogFactory.getLog(this.mainApplicationClass);
  662 | 	}
  663 | 
  664 | 	/**
  665 | 	 * Load beans into the application context.
  666 | 	 * @param context the context to load beans into
  667 | 	 * @param sources the sources to load
  668 | 	 */
  669 | 	protected void load(ApplicationContext context, Object[] sources) {
  670 | 		if (logger.isDebugEnabled()) {
  671 | 			logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
  672 | 		}
  673 | 		BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
  674 | 		if (this.beanNameGenerator != null) {
  675 | 			loader.setBeanNameGenerator(this.beanNameGenerator);
  676 | 		}
  677 | 		if (this.resourceLoader != null) {
  678 | 			loader.setResourceLoader(this.resourceLoader);
  679 | 		}
  680 | 		if (this.environment != null) {
  681 | 			loader.setEnvironment(this.environment);
  682 | 		}
  683 | 		loader.load();
  684 | 	}
  685 | 
  686 | 	/**
  687 | 	 * The ResourceLoader that will be used in the ApplicationContext.
  688 | 	 * @return the resourceLoader the resource loader that will be used in the
  689 | 	 * ApplicationContext (or {@code null} if the default)
  690 | 	 */
  691 | 	public @Nullable ResourceLoader getResourceLoader() {
  692 | 		return this.resourceLoader;
  693 | 	}
  694 | 
  695 | 	/**
  696 | 	 * Either the ClassLoader that will be used in the ApplicationContext (if
  697 | 	 * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set), or the context
  698 | 	 * class loader (if not null), or the loader of the Spring {@link ClassUtils} class.
  699 | 	 * @return a ClassLoader (never null)
  700 | 	 */
  701 | 	public ClassLoader getClassLoader() {
  702 | 		if (this.resourceLoader != null) {
  703 | 			ClassLoader classLoader = this.resourceLoader.getClassLoader();
  704 | 			Assert.state(classLoader != null, "No classloader found");
  705 | 			return classLoader;
  706 | 		}
  707 | 		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
  708 | 		Assert.state(classLoader != null, "No classloader found");
  709 | 		return classLoader;
  710 | 	}
  711 | 
  712 | 	/**
  713 | 	 * Get the bean definition registry.
  714 | 	 * @param context the application context
  715 | 	 * @return the BeanDefinitionRegistry if it can be determined
  716 | 	 */
  717 | 	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
  718 | 		if (context instanceof BeanDefinitionRegistry registry) {
  719 | 			return registry;
  720 | 		}
  721 | 		if (context instanceof AbstractApplicationContext abstractApplicationContext) {
  722 | 			return (BeanDefinitionRegistry) abstractApplicationContext.getBeanFactory();
  723 | 		}
  724 | 		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
  725 | 	}
  726 | 
  727 | 	/**
  728 | 	 * Factory method used to create the {@link BeanDefinitionLoader}.
  729 | 	 * @param registry the bean definition registry
  730 | 	 * @param sources the sources to load
  731 | 	 * @return the {@link BeanDefinitionLoader} that will be used to load beans
  732 | 	 */
  733 | 	protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
  734 | 		return new BeanDefinitionLoader(registry, sources);
  735 | 	}
  736 | 
  737 | 	/**
  738 | 	 * Refresh the underlying {@link ApplicationContext}.
  739 | 	 * @param applicationContext the application context to refresh
  740 | 	 */
  741 | 	protected void refresh(ConfigurableApplicationContext applicationContext) {
  742 | 		applicationContext.refresh();
  743 | 	}
  744 | 
  745 | 	/**
  746 | 	 * Called after the context has been refreshed.
  747 | 	 * @param context the application context
  748 | 	 * @param args the application arguments
  749 | 	 */
  750 | 	protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
  751 | 	}
  752 | 
  753 | 	private void callRunners(ConfigurableApplicationContext context, ApplicationArguments args) {
  754 | 		if (!context.isRunning()) {
  755 | 			return;
  756 | 		}
  757 | 		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
  758 | 		String[] beanNames = beanFactory.getBeanNamesForType(Runner.class);
  759 | 		Map<Runner, String> instancesToBeanNames = new IdentityHashMap<>();
  760 | 		for (String beanName : beanNames) {
  761 | 			instancesToBeanNames.put(beanFactory.getBean(beanName, Runner.class), beanName);
  762 | 		}
  763 | 		Comparator<Object> comparator = getOrderComparator(beanFactory)
  764 | 			.withSourceProvider(new FactoryAwareOrderSourceProvider(beanFactory, instancesToBeanNames));
  765 | 		instancesToBeanNames.keySet().stream().sorted(comparator).forEach((runner) -> callRunner(runner, args));
  766 | 	}
  767 | 
  768 | 	private OrderComparator getOrderComparator(ConfigurableListableBeanFactory beanFactory) {
  769 | 		Comparator<?> dependencyComparator = (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory)
  770 | 				? defaultListableBeanFactory.getDependencyComparator() : null;
  771 | 		return (dependencyComparator instanceof OrderComparator orderComparator) ? orderComparator
  772 | 				: AnnotationAwareOrderComparator.INSTANCE;
  773 | 	}
  774 | 
  775 | 	private void callRunner(Runner runner, ApplicationArguments args) {
  776 | 		if (runner instanceof ApplicationRunner) {
  777 | 			callRunner(ApplicationRunner.class, runner, (applicationRunner) -> applicationRunner.run(args));
  778 | 		}
  779 | 		if (runner instanceof CommandLineRunner) {
  780 | 			callRunner(CommandLineRunner.class, runner,
  781 | 					(commandLineRunner) -> commandLineRunner.run(args.getSourceArgs()));
  782 | 		}
  783 | 	}
  784 | 
  785 | 	@SuppressWarnings("unchecked")
  786 | 	private <R extends Runner> void callRunner(Class<R> type, Runner runner, ThrowingConsumer<R> call) {
  787 | 		call.throwing(
  788 | 				(message, ex) -> new IllegalStateException("Failed to execute " + ClassUtils.getShortName(type), ex))
  789 | 			.accept((R) runner);
  790 | 	}
  791 | 
  792 | 	private RuntimeException handleRunFailure(@Nullable ConfigurableApplicationContext context, Throwable exception,
  793 | 			@Nullable SpringApplicationRunListeners listeners) {
  794 | 		if (exception instanceof AbandonedRunException abandonedRunException) {
  795 | 			return abandonedRunException;
  796 | 		}
  797 | 		try {
  798 | 			try {
  799 | 				handleExitCode(context, exception);
  800 | 				if (listeners != null) {
  801 | 					listeners.failed(context, exception);
  802 | 				}
  803 | 			}
  804 | 			finally {
  805 | 				reportFailure(getExceptionReporters(context), exception);
  806 | 				if (context != null) {
  807 | 					context.close();
  808 | 					shutdownHook.deregisterFailedApplicationContext(context);
  809 | 				}
  810 | 			}
  811 | 		}
  812 | 		catch (Exception ex) {
  813 | 			logger.warn("Unable to close ApplicationContext", ex);
  814 | 		}
  815 | 		return (exception instanceof RuntimeException runtimeException) ? runtimeException
  816 | 				: new IllegalStateException(exception);
  817 | 	}
  818 | 
  819 | 	private Collection<SpringBootExceptionReporter> getExceptionReporters(
  820 | 			@Nullable ConfigurableApplicationContext context) {
  821 | 		try {
  822 | 			ArgumentResolver argumentResolver = (context != null)
  823 | 					? ArgumentResolver.of(ConfigurableApplicationContext.class, context) : ArgumentResolver.none();
  824 | 			return getSpringFactoriesInstances(SpringBootExceptionReporter.class, argumentResolver);
  825 | 		}
  826 | 		catch (Throwable ex) {
  827 | 			return Collections.emptyList();
  828 | 		}
  829 | 	}
  830 | 
  831 | 	private void reportFailure(Collection<SpringBootExceptionReporter> exceptionReporters, Throwable failure) {
  832 | 		try {
  833 | 			for (SpringBootExceptionReporter reporter : exceptionReporters) {
  834 | 				if (reporter.reportException(failure)) {
  835 | 					registerLoggedException(failure);
  836 | 					return;
  837 | 				}
  838 | 			}
  839 | 		}
  840 | 		catch (Throwable ex) {
  841 | 			// Continue with normal handling of the original failure
  842 | 		}
  843 | 		if (logger.isErrorEnabled()) {
  844 | 			if (NativeDetector.inNativeImage()) {
  845 | 				// Depending on how early the failure was, logging may not work in a
  846 | 				// native image so we output the stack trace directly to System.out
  847 | 				// instead.
  848 | 				System.out.println("Application run failed");
  849 | 				failure.printStackTrace(System.out);
  850 | 			}
  851 | 			else {
  852 | 				logger.error("Application run failed", failure);
  853 | 			}
  854 | 			registerLoggedException(failure);
  855 | 		}
  856 | 	}
  857 | 
  858 | 	/**
  859 | 	 * Register that the given exception has been logged. By default, if the running in
  860 | 	 * the main thread, this method will suppress additional printing of the stacktrace.
  861 | 	 * @param exception the exception that was logged
  862 | 	 */
  863 | 	protected void registerLoggedException(Throwable exception) {
  864 | 		SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
  865 | 		if (handler != null) {
  866 | 			handler.registerLoggedException(exception);
  867 | 		}
  868 | 	}
  869 | 
  870 | 	private void handleExitCode(@Nullable ConfigurableApplicationContext context, Throwable exception) {
  871 | 		int exitCode = getExitCodeFromException(context, exception);
  872 | 		if (exitCode != 0) {
  873 | 			if (context != null) {
  874 | 				context.publishEvent(new ExitCodeEvent(context, exitCode));
  875 | 			}
  876 | 			SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
  877 | 			if (handler != null) {
  878 | 				handler.registerExitCode(exitCode);
  879 | 			}
  880 | 		}
  881 | 	}
  882 | 
  883 | 	private int getExitCodeFromException(@Nullable ConfigurableApplicationContext context, Throwable exception) {
  884 | 		int exitCode = getExitCodeFromMappedException(context, exception);
  885 | 		if (exitCode == 0) {
  886 | 			exitCode = getExitCodeFromExitCodeGeneratorException(exception);
  887 | 		}
  888 | 		return exitCode;
  889 | 	}
  890 | 
  891 | 	private int getExitCodeFromMappedException(@Nullable ConfigurableApplicationContext context, Throwable exception) {
  892 | 		if (context == null || !context.isActive()) {
  893 | 			return 0;
  894 | 		}
  895 | 		ExitCodeGenerators generators = new ExitCodeGenerators();
  896 | 		Collection<ExitCodeExceptionMapper> beans = context.getBeansOfType(ExitCodeExceptionMapper.class).values();
  897 | 		generators.addAll(exception, beans);
  898 | 		return generators.getExitCode();
  899 | 	}
  900 | 
  901 | 	private int getExitCodeFromExitCodeGeneratorException(@Nullable Throwable exception) {
  902 | 		if (exception == null) {
  903 | 			return 0;
  904 | 		}
  905 | 		if (exception instanceof ExitCodeGenerator generator) {
  906 | 			return generator.getExitCode();
  907 | 		}
  908 | 		return getExitCodeFromExitCodeGeneratorException(exception.getCause());
  909 | 	}
  910 | 
  911 | 	@Nullable SpringBootExceptionHandler getSpringBootExceptionHandler() {
  912 | 		if (isMainThread(Thread.currentThread())) {
  913 | 			return SpringBootExceptionHandler.forCurrentThread();
  914 | 		}
  915 | 		return null;
  916 | 	}
  917 | 
  918 | 	private boolean isMainThread(Thread currentThread) {
  919 | 		return ("main".equals(currentThread.getName()) || "restartedMain".equals(currentThread.getName()))
  920 | 				&& "main".equals(currentThread.getThreadGroup().getName());
  921 | 	}
  922 | 
  923 | 	/**
  924 | 	 * Returns the main application class that has been deduced or explicitly configured.
  925 | 	 * @return the main application class or {@code null}
  926 | 	 */
  927 | 	public @Nullable Class<?> getMainApplicationClass() {
  928 | 		return this.mainApplicationClass;
  929 | 	}
  930 | 
  931 | 	/**
  932 | 	 * Set a specific main application class that will be used as a log source and to
  933 | 	 * obtain version information. By default the main application class will be deduced.
  934 | 	 * Can be set to {@code null} if there is no explicit application class.
  935 | 	 * @param mainApplicationClass the mainApplicationClass to set or {@code null}
  936 | 	 */
  937 | 	public void setMainApplicationClass(@Nullable Class<?> mainApplicationClass) {
  938 | 		this.mainApplicationClass = mainApplicationClass;
  939 | 	}
  940 | 
  941 | 	/**
  942 | 	 * Returns the type of web application that is being run.
  943 | 	 * @return the type of web application
  944 | 	 * @since 2.0.0
  945 | 	 */
  946 | 	public @Nullable WebApplicationType getWebApplicationType() {
  947 | 		return this.properties.getWebApplicationType();
  948 | 	}
  949 | 
  950 | 	/**
  951 | 	 * Sets the type of web application to be run. If not explicitly set the type of web
  952 | 	 * application will be deduced based on the classpath.
  953 | 	 * @param webApplicationType the web application type
  954 | 	 * @since 2.0.0
  955 | 	 */
  956 | 	public void setWebApplicationType(WebApplicationType webApplicationType) {
  957 | 		Assert.notNull(webApplicationType, "'webApplicationType' must not be null");
  958 | 		this.properties.setWebApplicationType(webApplicationType);
  959 | 	}
  960 | 
  961 | 	/**
  962 | 	 * Sets if bean definition overriding, by registering a definition with the same name
  963 | 	 * as an existing definition, should be allowed. Defaults to {@code false}.
  964 | 	 * @param allowBeanDefinitionOverriding if overriding is allowed
  965 | 	 * @since 2.1.0
  966 | 	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding(boolean)
  967 | 	 */
  968 | 	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
  969 | 		this.properties.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
  970 | 	}
  971 | 
  972 | 	/**
  973 | 	 * Sets whether to allow circular references between beans and automatically try to
  974 | 	 * resolve them. Defaults to {@code false}.
  975 | 	 * @param allowCircularReferences if circular references are allowed
  976 | 	 * @since 2.6.0
  977 | 	 * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
  978 | 	 */
  979 | 	public void setAllowCircularReferences(boolean allowCircularReferences) {
  980 | 		this.properties.setAllowCircularReferences(allowCircularReferences);
  981 | 	}
  982 | 
  983 | 	/**
  984 | 	 * Sets if beans should be initialized lazily. Defaults to {@code false}.
  985 | 	 * @param lazyInitialization if initialization should be lazy
  986 | 	 * @since 2.2
  987 | 	 * @see BeanDefinition#setLazyInit(boolean)
  988 | 	 */
  989 | 	public void setLazyInitialization(boolean lazyInitialization) {
  990 | 		this.properties.setLazyInitialization(lazyInitialization);
  991 | 	}
  992 | 
  993 | 	/**
  994 | 	 * Sets if the application is headless and should not instantiate AWT. Defaults to
  995 | 	 * {@code true} to prevent java icons appearing.
  996 | 	 * @param headless if the application is headless
  997 | 	 */
  998 | 	public void setHeadless(boolean headless) {
  999 | 		this.headless = headless;
 1000 | 	}
 1001 | 
 1002 | 	/**
 1003 | 	 * Sets if the created {@link ApplicationContext} should have a shutdown hook
 1004 | 	 * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
 1005 | 	 * gracefully.
 1006 | 	 * @param registerShutdownHook if the shutdown hook should be registered
 1007 | 	 * @see #getShutdownHandlers()
 1008 | 	 */
 1009 | 	public void setRegisterShutdownHook(boolean registerShutdownHook) {
 1010 | 		this.properties.setRegisterShutdownHook(registerShutdownHook);
 1011 | 	}
 1012 | 
 1013 | 	/**
 1014 | 	 * Sets the {@link Banner} instance which will be used to print the banner when no
 1015 | 	 * static banner file is provided.
 1016 | 	 * @param banner the Banner instance to use
 1017 | 	 */
 1018 | 	public void setBanner(Banner banner) {
 1019 | 		this.banner = banner;
 1020 | 	}
 1021 | 
 1022 | 	/**
 1023 | 	 * Sets the mode used to display the banner when the application runs. Defaults to
 1024 | 	 * {@code Banner.Mode.CONSOLE}.
 1025 | 	 * @param bannerMode the mode used to display the banner
 1026 | 	 */
 1027 | 	public void setBannerMode(Banner.Mode bannerMode) {
 1028 | 		this.properties.setBannerMode(bannerMode);
 1029 | 	}
 1030 | 
 1031 | 	/**
 1032 | 	 * Sets if the application information should be logged when the application starts.
 1033 | 	 * Defaults to {@code true}.
 1034 | 	 * @param logStartupInfo if startup info should be logged.
 1035 | 	 */
 1036 | 	public void setLogStartupInfo(boolean logStartupInfo) {
 1037 | 		this.properties.setLogStartupInfo(logStartupInfo);
 1038 | 	}
 1039 | 
 1040 | 	/**
 1041 | 	 * Sets if a {@link CommandLinePropertySource} should be added to the application
 1042 | 	 * context in order to expose arguments. Defaults to {@code true}.
 1043 | 	 * @param addCommandLineProperties if command line arguments should be exposed
 1044 | 	 */
 1045 | 	public void setAddCommandLineProperties(boolean addCommandLineProperties) {
 1046 | 		this.addCommandLineProperties = addCommandLineProperties;
 1047 | 	}
 1048 | 
 1049 | 	/**
 1050 | 	 * Sets if the {@link ApplicationConversionService} should be added to the application
 1051 | 	 * context's {@link Environment}.
 1052 | 	 * @param addConversionService if the application conversion service should be added
 1053 | 	 * @since 2.1.0
 1054 | 	 */
 1055 | 	public void setAddConversionService(boolean addConversionService) {
 1056 | 		this.addConversionService = addConversionService;
 1057 | 	}
 1058 | 
 1059 | 	/**
 1060 | 	 * Adds {@link BootstrapRegistryInitializer} instances that can be used to initialize
 1061 | 	 * the {@link BootstrapRegistry}.
 1062 | 	 * @param bootstrapRegistryInitializer the bootstrap registry initializer to add
 1063 | 	 * @since 2.4.5
 1064 | 	 */
 1065 | 	public void addBootstrapRegistryInitializer(BootstrapRegistryInitializer bootstrapRegistryInitializer) {
 1066 | 		Assert.notNull(bootstrapRegistryInitializer, "'bootstrapRegistryInitializer' must not be null");
 1067 | 		this.bootstrapRegistryInitializers.addAll(Arrays.asList(bootstrapRegistryInitializer));
 1068 | 	}
 1069 | 
 1070 | 	/**
 1071 | 	 * Set default environment properties which will be used in addition to those in the
 1072 | 	 * existing {@link Environment}.
 1073 | 	 * @param defaultProperties the additional properties to set
 1074 | 	 */
 1075 | 	public void setDefaultProperties(Map<String, Object> defaultProperties) {
 1076 | 		this.defaultProperties = defaultProperties;
 1077 | 	}
 1078 | 
 1079 | 	/**
 1080 | 	 * Convenient alternative to {@link #setDefaultProperties(Map)}.
 1081 | 	 * @param defaultProperties some {@link Properties}
 1082 | 	 */
 1083 | 	public void setDefaultProperties(Properties defaultProperties) {
 1084 | 		this.defaultProperties = new HashMap<>();
 1085 | 		for (Object key : Collections.list(defaultProperties.propertyNames())) {
 1086 | 			this.defaultProperties.put((String) key, defaultProperties.get(key));
 1087 | 		}
 1088 | 	}
 1089 | 
 1090 | 	/**
 1091 | 	 * Set additional profile values to use (on top of those set in system or command line
 1092 | 	 * properties).
 1093 | 	 * @param profiles the additional profiles to set
 1094 | 	 */
 1095 | 	public void setAdditionalProfiles(String... profiles) {
 1096 | 		this.additionalProfiles = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(profiles)));
 1097 | 	}
 1098 | 
 1099 | 	/**
 1100 | 	 * Return an immutable set of any additional profiles in use.
 1101 | 	 * @return the additional profiles
 1102 | 	 */
 1103 | 	public Set<String> getAdditionalProfiles() {
 1104 | 		return this.additionalProfiles;
 1105 | 	}
 1106 | 
 1107 | 	/**
 1108 | 	 * Sets the bean name generator that should be used when generating bean names.
 1109 | 	 * @param beanNameGenerator the bean name generator
 1110 | 	 */
 1111 | 	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
 1112 | 		this.beanNameGenerator = beanNameGenerator;
 1113 | 	}
 1114 | 
 1115 | 	/**
 1116 | 	 * Sets the underlying environment that should be used with the created application
 1117 | 	 * context.
 1118 | 	 * @param environment the environment
 1119 | 	 */
 1120 | 	public void setEnvironment(@Nullable ConfigurableEnvironment environment) {
 1121 | 		this.isCustomEnvironment = true;
 1122 | 		this.environment = environment;
 1123 | 	}
 1124 | 
 1125 | 	/**
 1126 | 	 * Add additional items to the primary sources that will be added to an
 1127 | 	 * ApplicationContext when {@link #run(String...)} is called.
 1128 | 	 * <p>
 1129 | 	 * The sources here are added to those that were set in the constructor. Most users
 1130 | 	 * should consider using {@link #getSources()}/{@link #setSources(Set)} rather than
 1131 | 	 * calling this method.
 1132 | 	 * @param additionalPrimarySources the additional primary sources to add
 1133 | 	 * @see #SpringApplication(Class...)
 1134 | 	 * @see #getSources()
 1135 | 	 * @see #setSources(Set)
 1136 | 	 * @see #getAllSources()
 1137 | 	 */
 1138 | 	public void addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
 1139 | 		this.primarySources.addAll(additionalPrimarySources);
 1140 | 	}
 1141 | 
 1142 | 	/**
 1143 | 	 * Returns a mutable set of the sources that will be added to an ApplicationContext
 1144 | 	 * when {@link #run(String...)} is called.
 1145 | 	 * <p>
 1146 | 	 * Sources set here will be used in addition to any primary sources set in the
 1147 | 	 * constructor.
 1148 | 	 * @return the application sources.
 1149 | 	 * @see #SpringApplication(Class...)
 1150 | 	 * @see #getAllSources()
 1151 | 	 */
 1152 | 	public Set<String> getSources() {
 1153 | 		return this.properties.getSources();
 1154 | 	}
 1155 | 
 1156 | 	/**
 1157 | 	 * Set additional sources that will be used to create an ApplicationContext. A source
 1158 | 	 * can be: a class name, package name, or an XML resource location.
 1159 | 	 * <p>
 1160 | 	 * Sources set here will be used in addition to any primary sources set in the
 1161 | 	 * constructor.
 1162 | 	 * @param sources the application sources to set
 1163 | 	 * @see #SpringApplication(Class...)
 1164 | 	 * @see #getAllSources()
 1165 | 	 */
 1166 | 	public void setSources(Set<String> sources) {
 1167 | 		Assert.notNull(sources, "'sources' must not be null");
 1168 | 		this.properties.setSources(sources);
 1169 | 	}
 1170 | 
 1171 | 	/**
 1172 | 	 * Return an immutable set of all the sources that will be added to an
 1173 | 	 * ApplicationContext when {@link #run(String...)} is called. This method combines any
 1174 | 	 * primary sources specified in the constructor with any additional ones that have
 1175 | 	 * been {@link #setSources(Set) explicitly set}.
 1176 | 	 * @return an immutable set of all sources
 1177 | 	 */
 1178 | 	public Set<Object> getAllSources() {
 1179 | 		Set<Object> allSources = new LinkedHashSet<>();
 1180 | 		if (!CollectionUtils.isEmpty(this.primarySources)) {
 1181 | 			allSources.addAll(this.primarySources);
 1182 | 		}
 1183 | 		if (!CollectionUtils.isEmpty(this.properties.getSources())) {
 1184 | 			allSources.addAll(this.properties.getSources());
 1185 | 		}
 1186 | 		return Collections.unmodifiableSet(allSources);
 1187 | 	}
 1188 | 
 1189 | 	/**
 1190 | 	 * Sets the {@link ResourceLoader} that should be used when loading resources.
 1191 | 	 * @param resourceLoader the resource loader
 1192 | 	 */
 1193 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
 1194 | 		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
 1195 | 		this.resourceLoader = resourceLoader;
 1196 | 	}
 1197 | 
 1198 | 	/**
 1199 | 	 * Return a prefix that should be applied when obtaining configuration properties from
 1200 | 	 * the system environment.
 1201 | 	 * @return the environment property prefix
 1202 | 	 * @since 2.5.0
 1203 | 	 */
 1204 | 	public @Nullable String getEnvironmentPrefix() {
 1205 | 		return this.environmentPrefix;
 1206 | 	}
 1207 | 
 1208 | 	/**
 1209 | 	 * Set the prefix that should be applied when obtaining configuration properties from
 1210 | 	 * the system environment.
 1211 | 	 * @param environmentPrefix the environment property prefix to set
 1212 | 	 * @since 2.5.0
 1213 | 	 */
 1214 | 	public void setEnvironmentPrefix(String environmentPrefix) {
 1215 | 		this.environmentPrefix = environmentPrefix;
 1216 | 	}
 1217 | 
 1218 | 	/**
 1219 | 	 * Sets the factory that will be called to create the application context. If not set,
 1220 | 	 * defaults to a factory that will create a context that is appropriate for the
 1221 | 	 * application's type (a reactive web application, a servlet web application, or a
 1222 | 	 * non-web application).
 1223 | 	 * @param applicationContextFactory the factory for the context
 1224 | 	 * @since 2.4.0
 1225 | 	 */
 1226 | 	public void setApplicationContextFactory(@Nullable ApplicationContextFactory applicationContextFactory) {
 1227 | 		this.applicationContextFactory = (applicationContextFactory != null) ? applicationContextFactory
 1228 | 				: ApplicationContextFactory.DEFAULT;
 1229 | 	}
 1230 | 
 1231 | 	/**
 1232 | 	 * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
 1233 | 	 * {@link ApplicationContext}.
 1234 | 	 * @param initializers the initializers to set
 1235 | 	 */
 1236 | 	public void setInitializers(Collection<? extends ApplicationContextInitializer<?>> initializers) {
 1237 | 		this.initializers = new ArrayList<>(initializers);
 1238 | 	}
 1239 | 
 1240 | 	/**
 1241 | 	 * Add {@link ApplicationContextInitializer}s to be applied to the Spring
 1242 | 	 * {@link ApplicationContext}.
 1243 | 	 * @param initializers the initializers to add
 1244 | 	 */
 1245 | 	public void addInitializers(ApplicationContextInitializer<?>... initializers) {
 1246 | 		this.initializers.addAll(Arrays.asList(initializers));
 1247 | 	}
 1248 | 
 1249 | 	/**
 1250 | 	 * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
 1251 | 	 * will be applied to the Spring {@link ApplicationContext}.
 1252 | 	 * @return the initializers
 1253 | 	 */
 1254 | 	public Set<ApplicationContextInitializer<?>> getInitializers() {
 1255 | 		return asUnmodifiableOrderedSet(this.initializers);
 1256 | 	}
 1257 | 
 1258 | 	/**
 1259 | 	 * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
 1260 | 	 * and registered with the {@link ApplicationContext}.
 1261 | 	 * @param listeners the listeners to set
 1262 | 	 */
 1263 | 	public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
 1264 | 		this.listeners = new ArrayList<>(listeners);
 1265 | 	}
 1266 | 
 1267 | 	/**
 1268 | 	 * Add {@link ApplicationListener}s to be applied to the SpringApplication and
 1269 | 	 * registered with the {@link ApplicationContext}.
 1270 | 	 * @param listeners the listeners to add
 1271 | 	 */
 1272 | 	public void addListeners(ApplicationListener<?>... listeners) {
 1273 | 		this.listeners.addAll(Arrays.asList(listeners));
 1274 | 	}
 1275 | 
 1276 | 	/**
 1277 | 	 * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
 1278 | 	 * applied to the SpringApplication and registered with the {@link ApplicationContext}
 1279 | 	 * .
 1280 | 	 * @return the listeners
 1281 | 	 */
 1282 | 	public Set<ApplicationListener<?>> getListeners() {
 1283 | 		return asUnmodifiableOrderedSet(this.listeners);
 1284 | 	}
 1285 | 
 1286 | 	/**
 1287 | 	 * Set the {@link ApplicationStartup} to use for collecting startup metrics.
 1288 | 	 * @param applicationStartup the application startup to use
 1289 | 	 * @since 2.4.0
 1290 | 	 */
 1291 | 	public void setApplicationStartup(ApplicationStartup applicationStartup) {
 1292 | 		this.applicationStartup = (applicationStartup != null) ? applicationStartup : ApplicationStartup.DEFAULT;
 1293 | 	}
 1294 | 
 1295 | 	/**
 1296 | 	 * Returns the {@link ApplicationStartup} used for collecting startup metrics.
 1297 | 	 * @return the application startup
 1298 | 	 * @since 2.4.0
 1299 | 	 */
 1300 | 	public ApplicationStartup getApplicationStartup() {
 1301 | 		return this.applicationStartup;
 1302 | 	}
 1303 | 
 1304 | 	/**
 1305 | 	 * Whether to keep the application alive even if there are no more non-daemon threads.
 1306 | 	 * @return whether to keep the application alive even if there are no more non-daemon
 1307 | 	 * threads
 1308 | 	 * @since 3.2.0
 1309 | 	 */
 1310 | 	public boolean isKeepAlive() {
 1311 | 		return this.properties.isKeepAlive();
 1312 | 	}
 1313 | 
 1314 | 	/**
 1315 | 	 * Set whether to keep the application alive even if there are no more non-daemon
 1316 | 	 * threads.
 1317 | 	 * @param keepAlive whether to keep the application alive even if there are no more
 1318 | 	 * non-daemon threads
 1319 | 	 * @since 3.2.0
 1320 | 	 */
 1321 | 	public void setKeepAlive(boolean keepAlive) {
 1322 | 		this.properties.setKeepAlive(keepAlive);
 1323 | 	}
 1324 | 
 1325 | 	/**
 1326 | 	 * Return a {@link SpringApplicationShutdownHandlers} instance that can be used to add
 1327 | 	 * or remove handlers that perform actions before the JVM is shutdown.
 1328 | 	 * @return a {@link SpringApplicationShutdownHandlers} instance
 1329 | 	 * @since 2.5.1
 1330 | 	 */
 1331 | 	public static SpringApplicationShutdownHandlers getShutdownHandlers() {
 1332 | 		return shutdownHook.getHandlers();
 1333 | 	}
 1334 | 
 1335 | 	/**
 1336 | 	 * Static helper that can be used to run a {@link SpringApplication} from the
 1337 | 	 * specified source using default settings.
 1338 | 	 * @param primarySource the primary source to load
 1339 | 	 * @param args the application arguments (usually passed from a Java main method)
 1340 | 	 * @return the running {@link ApplicationContext}
 1341 | 	 */
 1342 | 	public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
 1343 | 		return run(new Class<?>[] { primarySource }, args);
 1344 | 	}
 1345 | 
 1346 | 	/**
 1347 | 	 * Static helper that can be used to run a {@link SpringApplication} from the
 1348 | 	 * specified sources using default settings and user supplied arguments.
 1349 | 	 * @param primarySources the primary sources to load
 1350 | 	 * @param args the application arguments (usually passed from a Java main method)
 1351 | 	 * @return the running {@link ApplicationContext}
 1352 | 	 */
 1353 | 	public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
 1354 | 		return new SpringApplication(primarySources).run(args);
 1355 | 	}
 1356 | 
 1357 | 	/**
 1358 | 	 * A basic main that can be used to launch an application. This method is useful when
 1359 | 	 * application sources are defined through a {@literal --spring.main.sources} command
 1360 | 	 * line argument.
 1361 | 	 * <p>
 1362 | 	 * Most developers will want to define their own main method and call the
 1363 | 	 * {@link #run(Class, String...) run} method instead.
 1364 | 	 * @param args command line arguments
 1365 | 	 * @throws Exception if the application cannot be started
 1366 | 	 * @see SpringApplication#run(Class[], String[])
 1367 | 	 * @see SpringApplication#run(Class, String...)
 1368 | 	 */
 1369 | 	public static void main(String[] args) throws Exception {
 1370 | 		SpringApplication.run(new Class<?>[0], args);
 1371 | 	}
 1372 | 
 1373 | 	/**
 1374 | 	 * Static helper that can be used to exit a {@link SpringApplication} and obtain a
 1375 | 	 * code indicating success (0) or otherwise. Does not throw exceptions but should
 1376 | 	 * print stack traces of any encountered. Applies the specified
 1377 | 	 * {@link ExitCodeGenerator ExitCodeGenerators} in addition to any Spring beans that
 1378 | 	 * implement {@link ExitCodeGenerator}. When multiple generators are available, the
 1379 | 	 * first non-zero exit code is used. Generators are ordered based on their
 1380 | 	 * {@link Ordered} implementation and {@link Order @Order} annotation.
 1381 | 	 * @param context the context to close if possible
 1382 | 	 * @param exitCodeGenerators exit code generators
 1383 | 	 * @return the outcome (0 if successful)
 1384 | 	 */
 1385 | 	public static int exit(ApplicationContext context, ExitCodeGenerator... exitCodeGenerators) {
 1386 | 		Assert.notNull(context, "'context' must not be null");
 1387 | 		int exitCode = 0;
 1388 | 		try {
 1389 | 			try {
 1390 | 				ExitCodeGenerators generators = new ExitCodeGenerators();
 1391 | 				Collection<ExitCodeGenerator> beans = context.getBeansOfType(ExitCodeGenerator.class).values();
 1392 | 				generators.addAll(exitCodeGenerators);
 1393 | 				generators.addAll(beans);
 1394 | 				exitCode = generators.getExitCode();
 1395 | 				if (exitCode != 0) {
 1396 | 					context.publishEvent(new ExitCodeEvent(context, exitCode));
 1397 | 				}
 1398 | 			}
 1399 | 			finally {
 1400 | 				close(context);
 1401 | 			}
 1402 | 		}
 1403 | 		catch (Exception ex) {
 1404 | 			ex.printStackTrace();
 1405 | 			exitCode = (exitCode != 0) ? exitCode : 1;
 1406 | 		}
 1407 | 		return exitCode;
 1408 | 	}
 1409 | 
 1410 | 	/**
 1411 | 	 * Create an application from an existing {@code main} method that can run with
 1412 | 	 * additional {@code @Configuration} or bean classes. This method can be helpful when
 1413 | 	 * writing a test harness that needs to start an application with additional
 1414 | 	 * configuration.
 1415 | 	 * @param main the main method entry point that runs the {@link SpringApplication}
 1416 | 	 * @return a {@link SpringApplication.Augmented} instance that can be used to add
 1417 | 	 * configuration and run the application
 1418 | 	 * @since 3.1.0
 1419 | 	 * @see #withHook(SpringApplicationHook, Runnable)
 1420 | 	 */
 1421 | 	public static SpringApplication.Augmented from(ThrowingConsumer<String[]> main) {
 1422 | 		Assert.notNull(main, "'main' must not be null");
 1423 | 		return new Augmented(main, Collections.emptySet(), Collections.emptySet());
 1424 | 	}
 1425 | 
 1426 | 	/**
 1427 | 	 * Perform the given action with the given {@link SpringApplicationHook} attached if
 1428 | 	 * the action triggers an {@link SpringApplication#run(String...) application run}.
 1429 | 	 * @param hook the hook to apply
 1430 | 	 * @param action the action to run
 1431 | 	 * @since 3.0.0
 1432 | 	 * @see #withHook(SpringApplicationHook, ThrowingSupplier)
 1433 | 	 */
 1434 | 	public static void withHook(SpringApplicationHook hook, Runnable action) {
 1435 | 		withHook(hook, () -> {
 1436 | 			action.run();
 1437 | 			return Void.class;
 1438 | 		});
 1439 | 	}
 1440 | 
 1441 | 	/**
 1442 | 	 * Perform the given action with the given {@link SpringApplicationHook} attached if
 1443 | 	 * the action triggers an {@link SpringApplication#run(String...) application run}.
 1444 | 	 * @param <T> the result type
 1445 | 	 * @param hook the hook to apply
 1446 | 	 * @param action the action to run
 1447 | 	 * @return the result of the action
 1448 | 	 * @since 3.0.0
 1449 | 	 * @see #withHook(SpringApplicationHook, Runnable)
 1450 | 	 */
 1451 | 	public static <T> T withHook(SpringApplicationHook hook, ThrowingSupplier<T> action) {
 1452 | 		applicationHook.set(hook);
 1453 | 		try {
 1454 | 			return action.get();
 1455 | 		}
 1456 | 		finally {
 1457 | 			applicationHook.remove();
 1458 | 		}
 1459 | 	}
 1460 | 
 1461 | 	private static void close(ApplicationContext context) {
 1462 | 		if (context instanceof ConfigurableApplicationContext closable) {
 1463 | 			closable.close();
 1464 | 		}
 1465 | 	}
 1466 | 
 1467 | 	private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
 1468 | 		List<E> list = new ArrayList<>(elements);
 1469 | 		list.sort(AnnotationAwareOrderComparator.INSTANCE);
 1470 | 		return new LinkedHashSet<>(list);
 1471 | 	}
 1472 | 
 1473 | 	/**
 1474 | 	 * Used to configure and run an augmented {@link SpringApplication} where additional
 1475 | 	 * configuration should be applied.
 1476 | 	 *
 1477 | 	 * @since 3.1.0
 1478 | 	 */
 1479 | 	public static class Augmented {
 1480 | 
 1481 | 		private final ThrowingConsumer<String[]> main;
 1482 | 
 1483 | 		private final Set<Class<?>> sources;
 1484 | 
 1485 | 		private final Set<String> additionalProfiles;
 1486 | 
 1487 | 		Augmented(ThrowingConsumer<String[]> main, Set<Class<?>> sources, Set<String> additionalProfiles) {
 1488 | 			this.main = main;
 1489 | 			this.sources = Set.copyOf(sources);
 1490 | 			this.additionalProfiles = additionalProfiles;
 1491 | 		}
 1492 | 
 1493 | 		/**
 1494 | 		 * Return a new {@link SpringApplication.Augmented} instance with additional
 1495 | 		 * sources that should be applied when the application runs.
 1496 | 		 * @param sources the sources that should be applied
 1497 | 		 * @return a new {@link SpringApplication.Augmented} instance
 1498 | 		 */
 1499 | 		public Augmented with(Class<?>... sources) {
 1500 | 			LinkedHashSet<Class<?>> merged = new LinkedHashSet<>(this.sources);
 1501 | 			merged.addAll(Arrays.asList(sources));
 1502 | 			return new Augmented(this.main, merged, this.additionalProfiles);
 1503 | 		}
 1504 | 
 1505 | 		/**
 1506 | 		 * Return a new {@link SpringApplication.Augmented} instance with additional
 1507 | 		 * profiles that should be applied when the application runs.
 1508 | 		 * @param profiles the profiles that should be applied
 1509 | 		 * @return a new {@link SpringApplication.Augmented} instance
 1510 | 		 * @since 3.4.0
 1511 | 		 */
 1512 | 		public Augmented withAdditionalProfiles(String... profiles) {
 1513 | 			Set<String> merged = new LinkedHashSet<>(this.additionalProfiles);
 1514 | 			merged.addAll(Arrays.asList(profiles));
 1515 | 			return new Augmented(this.main, this.sources, merged);
 1516 | 		}
 1517 | 
 1518 | 		/**
 1519 | 		 * Run the application using the given args.
 1520 | 		 * @param args the main method args
 1521 | 		 * @return the running {@link ApplicationContext}
 1522 | 		 */
 1523 | 		public SpringApplication.Running run(String... args) {
 1524 | 			RunListener runListener = new RunListener();
 1525 | 			SpringApplicationHook hook = new SingleUseSpringApplicationHook((springApplication) -> {
 1526 | 				springApplication.addPrimarySources(this.sources);
 1527 | 				springApplication.setAdditionalProfiles(this.additionalProfiles.toArray(String[]::new));
 1528 | 				return runListener;
 1529 | 			});
 1530 | 			withHook(hook, () -> this.main.accept(args));
 1531 | 			return runListener;
 1532 | 		}
 1533 | 
 1534 | 		/**
 1535 | 		 * {@link SpringApplicationRunListener} to capture {@link Running} application
 1536 | 		 * details.
 1537 | 		 */
 1538 | 		private static final class RunListener implements SpringApplicationRunListener, Running {
 1539 | 
 1540 | 			private final List<ConfigurableApplicationContext> contexts = Collections
 1541 | 				.synchronizedList(new ArrayList<>());
 1542 | 
 1543 | 			@Override
 1544 | 			public void contextLoaded(ConfigurableApplicationContext context) {
 1545 | 				this.contexts.add(context);
 1546 | 			}
 1547 | 
 1548 | 			@Override
 1549 | 			public ConfigurableApplicationContext getApplicationContext() {
 1550 | 				List<ConfigurableApplicationContext> rootContexts = this.contexts.stream()
 1551 | 					.filter((context) -> context.getParent() == null)
 1552 | 					.toList();
 1553 | 				Assert.state(!rootContexts.isEmpty(), "No root application context located");
 1554 | 				Assert.state(rootContexts.size() == 1, "No unique root application context located");
 1555 | 				return rootContexts.get(0);
 1556 | 			}
 1557 | 
 1558 | 		}
 1559 | 
 1560 | 	}
 1561 | 
 1562 | 	/**
 1563 | 	 * Provides access to details of a {@link SpringApplication} run using
 1564 | 	 * {@link Augmented#run(String...)}.
 1565 | 	 *
 1566 | 	 * @since 3.1.0
 1567 | 	 */
 1568 | 	public interface Running {
 1569 | 
 1570 | 		/**
 1571 | 		 * Return the root {@link ConfigurableApplicationContext} of the running
 1572 | 		 * application.
 1573 | 		 * @return the root application context
 1574 | 		 */
 1575 | 		ConfigurableApplicationContext getApplicationContext();
 1576 | 
 1577 | 	}
 1578 | 
 1579 | 	/**
 1580 | 	 * {@link BeanFactoryPostProcessor} to re-order our property sources below any
 1581 | 	 * {@code @PropertySource} items added by the {@link ConfigurationClassPostProcessor}.
 1582 | 	 */
 1583 | 	private static class PropertySourceOrderingBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {
 1584 | 
 1585 | 		private final ConfigurableApplicationContext context;
 1586 | 
 1587 | 		PropertySourceOrderingBeanFactoryPostProcessor(ConfigurableApplicationContext context) {
 1588 | 			this.context = context;
 1589 | 		}
 1590 | 
 1591 | 		@Override
 1592 | 		public int getOrder() {
 1593 | 			return Ordered.HIGHEST_PRECEDENCE;
 1594 | 		}
 1595 | 
 1596 | 		@Override
 1597 | 		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
 1598 | 			DefaultPropertiesPropertySource.moveToEnd(this.context.getEnvironment());
 1599 | 		}
 1600 | 
 1601 | 	}
 1602 | 
 1603 | 	/**
 1604 | 	 * Exception that can be thrown to silently exit a running {@link SpringApplication}
 1605 | 	 * without handling run failures.
 1606 | 	 *
 1607 | 	 * @since 3.0.0
 1608 | 	 */
 1609 | 	public static class AbandonedRunException extends RuntimeException {
 1610 | 
 1611 | 		private final @Nullable ConfigurableApplicationContext applicationContext;
 1612 | 
 1613 | 		/**
 1614 | 		 * Create a new {@link AbandonedRunException} instance.
 1615 | 		 */
 1616 | 		public AbandonedRunException() {
 1617 | 			this(null);
 1618 | 		}
 1619 | 
 1620 | 		/**
 1621 | 		 * Create a new {@link AbandonedRunException} instance with the given application
 1622 | 		 * context.
 1623 | 		 * @param applicationContext the application context that was available when the
 1624 | 		 * run was abandoned
 1625 | 		 */
 1626 | 		public AbandonedRunException(@Nullable ConfigurableApplicationContext applicationContext) {
 1627 | 			this.applicationContext = applicationContext;
 1628 | 		}
 1629 | 
 1630 | 		/**
 1631 | 		 * Return the application context that was available when the run was abandoned or
 1632 | 		 * {@code null} if no context was available.
 1633 | 		 * @return the application context
 1634 | 		 */
 1635 | 		public @Nullable ConfigurableApplicationContext getApplicationContext() {
 1636 | 			return this.applicationContext;
 1637 | 		}
 1638 | 
 1639 | 	}
 1640 | 
 1641 | 	/**
 1642 | 	 * Exception which is thrown if GraalVM's native-image requirements aren't met.
 1643 | 	 */
 1644 | 	static final class NativeImageRequirementsException extends RuntimeException {
 1645 | 
 1646 | 		private static final JavaVersion MINIMUM_REQUIRED_JAVA_VERSION = JavaVersion.TWENTY_FIVE;
 1647 | 
 1648 | 		private static final JavaVersion CURRENT_JAVA_VERSION = JavaVersion.getJavaVersion();
 1649 | 
 1650 | 		NativeImageRequirementsException(String message) {
 1651 | 			super(message);
 1652 | 		}
 1653 | 
 1654 | 		static void throwIfNotMet() {
 1655 | 			if (CURRENT_JAVA_VERSION.isOlderThan(MINIMUM_REQUIRED_JAVA_VERSION)) {
 1656 | 				throw new NativeImageRequirementsException("Native Image requirements not met. "
 1657 | 						+ "Native Image must support at least Java %s but Java %s was detected"
 1658 | 							.formatted(MINIMUM_REQUIRED_JAVA_VERSION, CURRENT_JAVA_VERSION));
 1659 | 			}
 1660 | 		}
 1661 | 
 1662 | 	}
 1663 | 
 1664 | 	/**
 1665 | 	 * {@link SpringApplicationHook} decorator that ensures the hook is only used once.
 1666 | 	 */
 1667 | 	private static final class SingleUseSpringApplicationHook implements SpringApplicationHook {
 1668 | 
 1669 | 		private final AtomicBoolean used = new AtomicBoolean();
 1670 | 
 1671 | 		private final SpringApplicationHook delegate;
 1672 | 
 1673 | 		private SingleUseSpringApplicationHook(SpringApplicationHook delegate) {
 1674 | 			this.delegate = delegate;
 1675 | 		}
 1676 | 
 1677 | 		@Override
 1678 | 		public @Nullable SpringApplicationRunListener getRunListener(SpringApplication springApplication) {
 1679 | 			return this.used.compareAndSet(false, true) ? this.delegate.getRunListener(springApplication) : null;
 1680 | 		}
 1681 | 
 1682 | 	}
 1683 | 
 1684 | 	/**
 1685 | 	 * Starts a non-daemon thread to keep the JVM alive on {@link ContextRefreshedEvent}.
 1686 | 	 * Stops the thread on {@link ContextClosedEvent}.
 1687 | 	 */
 1688 | 	private static final class KeepAlive implements ApplicationListener<ApplicationContextEvent> {
 1689 | 
 1690 | 		private final ApplicationContext applicationContext;
 1691 | 
 1692 | 		private final AtomicReference<@Nullable Thread> thread = new AtomicReference<>();
 1693 | 
 1694 | 		KeepAlive(ApplicationContext applicationContext) {
 1695 | 			this.applicationContext = applicationContext;
 1696 | 		}
 1697 | 
 1698 | 		@Override
 1699 | 		public void onApplicationEvent(ApplicationContextEvent event) {
 1700 | 			if (event.getApplicationContext() != this.applicationContext) {
 1701 | 				return;
 1702 | 			}
 1703 | 			if (event instanceof ContextRefreshedEvent) {
 1704 | 				startKeepAliveThread();
 1705 | 			}
 1706 | 			else if (event instanceof ContextClosedEvent) {
 1707 | 				stopKeepAliveThread();
 1708 | 			}
 1709 | 		}
 1710 | 
 1711 | 		private void startKeepAliveThread() {
 1712 | 			Thread thread = new Thread(() -> {
 1713 | 				while (true) {
 1714 | 					try {
 1715 | 						Thread.sleep(Long.MAX_VALUE);
 1716 | 					}
 1717 | 					catch (InterruptedException ex) {
 1718 | 						break;
 1719 | 					}
 1720 | 				}
 1721 | 			});
 1722 | 			if (this.thread.compareAndSet(null, thread)) {
 1723 | 				thread.setDaemon(false);
 1724 | 				thread.setName("keep-alive");
 1725 | 				thread.start();
 1726 | 			}
 1727 | 		}
 1728 | 
 1729 | 		private void stopKeepAliveThread() {
 1730 | 			Thread thread = this.thread.getAndSet(null);
 1731 | 			if (thread == null) {
 1732 | 				return;
 1733 | 			}
 1734 | 			thread.interrupt();
 1735 | 		}
 1736 | 
 1737 | 	}
 1738 | 
 1739 | 	/**
 1740 | 	 * Strategy used to handle startup concerns.
 1741 | 	 */
 1742 | 	abstract static class Startup {
 1743 | 
 1744 | 		private @Nullable Duration timeTakenToStarted;
 1745 | 
 1746 | 		protected abstract long startTime();
 1747 | 
 1748 | 		protected abstract @Nullable Long processUptime();
 1749 | 
 1750 | 		protected abstract String action();
 1751 | 
 1752 | 		final Duration started() {
 1753 | 			long now = System.currentTimeMillis();
 1754 | 			this.timeTakenToStarted = Duration.ofMillis(now - startTime());
 1755 | 			return this.timeTakenToStarted;
 1756 | 		}
 1757 | 
 1758 | 		Duration timeTakenToStarted() {
 1759 | 			Assert.state(this.timeTakenToStarted != null,
 1760 | 					"timeTakenToStarted is not set. Make sure to call started() before this method");
 1761 | 			return this.timeTakenToStarted;
 1762 | 		}
 1763 | 
 1764 | 		private Duration ready() {
 1765 | 			long now = System.currentTimeMillis();
 1766 | 			return Duration.ofMillis(now - startTime());
 1767 | 		}
 1768 | 
 1769 | 		static Startup create() {
 1770 | 			ClassLoader classLoader = Startup.class.getClassLoader();
 1771 | 			return (ClassUtils.isPresent("jdk.crac.management.CRaCMXBean", classLoader)
 1772 | 					&& ClassUtils.isPresent("org.crac.management.CRaCMXBean", classLoader))
 1773 | 							? new CoordinatedRestoreAtCheckpointStartup() : new StandardStartup();
 1774 | 		}
 1775 | 
 1776 | 	}
 1777 | 
 1778 | 	/**
 1779 | 	 * Standard {@link Startup} implementation.
 1780 | 	 */
 1781 | 	private static final class StandardStartup extends Startup {
 1782 | 
 1783 | 		private final Long startTime = System.currentTimeMillis();
 1784 | 
 1785 | 		@Override
 1786 | 		protected long startTime() {
 1787 | 			return this.startTime;
 1788 | 		}
 1789 | 
 1790 | 		@Override
 1791 | 		protected @Nullable Long processUptime() {
 1792 | 			try {
 1793 | 				return ManagementFactory.getRuntimeMXBean().getUptime();
 1794 | 			}
 1795 | 			catch (Throwable ex) {
 1796 | 				return null;
 1797 | 			}
 1798 | 		}
 1799 | 
 1800 | 		@Override
 1801 | 		protected String action() {
 1802 | 			return "Started";
 1803 | 		}
 1804 | 
 1805 | 	}
 1806 | 
 1807 | 	/**
 1808 | 	 * Coordinated-Restore-At-Checkpoint {@link Startup} implementation.
 1809 | 	 */
 1810 | 	private static final class CoordinatedRestoreAtCheckpointStartup extends Startup {
 1811 | 
 1812 | 		private final StandardStartup fallback = new StandardStartup();
 1813 | 
 1814 | 		@Override
 1815 | 		protected @Nullable Long processUptime() {
 1816 | 			Long uptime = CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore();
 1817 | 			return (uptime >= 0) ? uptime : this.fallback.processUptime();
 1818 | 		}
 1819 | 
 1820 | 		@Override
 1821 | 		protected String action() {
 1822 | 			return (restoreTime() >= 0) ? "Restored" : this.fallback.action();
 1823 | 		}
 1824 | 
 1825 | 		private long restoreTime() {
 1826 | 			return CRaCMXBean.getCRaCMXBean().getRestoreTime();
 1827 | 		}
 1828 | 
 1829 | 		@Override
 1830 | 		protected long startTime() {
 1831 | 			long restoreTime = restoreTime();
 1832 | 			return (restoreTime >= 0) ? restoreTime : this.fallback.startTime();
 1833 | 		}
 1834 | 
 1835 | 	}
 1836 | 
 1837 | 	/**
 1838 | 	 * {@link OrderSourceProvider} used to obtain factory method and target type order
 1839 | 	 * sources. Based on internal {@link DefaultListableBeanFactory} code.
 1840 | 	 */
 1841 | 	private static class FactoryAwareOrderSourceProvider implements OrderSourceProvider {
 1842 | 
 1843 | 		private final ConfigurableBeanFactory beanFactory;
 1844 | 
 1845 | 		private final Map<?, String> instancesToBeanNames;
 1846 | 
 1847 | 		FactoryAwareOrderSourceProvider(ConfigurableBeanFactory beanFactory, Map<?, String> instancesToBeanNames) {
 1848 | 			this.beanFactory = beanFactory;
 1849 | 			this.instancesToBeanNames = instancesToBeanNames;
 1850 | 		}
 1851 | 
 1852 | 		@Override
 1853 | 		public @Nullable Object getOrderSource(Object obj) {
 1854 | 			String beanName = this.instancesToBeanNames.get(obj);
 1855 | 			return (beanName != null) ? getOrderSource(beanName, obj.getClass()) : null;
 1856 | 		}
 1857 | 
 1858 | 		private @Nullable Object getOrderSource(String beanName, Class<?> instanceType) {
 1859 | 			try {
 1860 | 				RootBeanDefinition beanDefinition = (RootBeanDefinition) this.beanFactory
 1861 | 					.getMergedBeanDefinition(beanName);
 1862 | 				Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
 1863 | 				Class<?> targetType = beanDefinition.getTargetType();
 1864 | 				targetType = (targetType != instanceType) ? targetType : null;
 1865 | 				return Stream.of(factoryMethod, targetType).filter(Objects::nonNull).toArray();
 1866 | 			}
 1867 | 			catch (NoSuchBeanDefinitionException ex) {
 1868 | 				return null;
 1869 | 			}
 1870 | 		}
 1871 | 
 1872 | 	}
 1873 | 
 1874 | }
```
