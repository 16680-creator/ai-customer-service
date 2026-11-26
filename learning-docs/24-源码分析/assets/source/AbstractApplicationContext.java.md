<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/support/AbstractApplicationContext.java -->
<!-- 正文引用：[[源码:AbstractApplicationContext.java#L起-L止]] ；代码块头部：(src: AbstractApplicationContext.java#L起-L止) -->

# AbstractApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/support/AbstractApplicationContext.java` |
| 行数 | 1673 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:48:31.947Z |

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
   17 | package org.springframework.context.support;
   18 | 
   19 | import java.io.IOException;
   20 | import java.lang.annotation.Annotation;
   21 | import java.util.ArrayList;
   22 | import java.util.Collection;
   23 | import java.util.Date;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Locale;
   27 | import java.util.Map;
   28 | import java.util.Set;
   29 | import java.util.concurrent.Executor;
   30 | import java.util.concurrent.TimeUnit;
   31 | import java.util.concurrent.atomic.AtomicBoolean;
   32 | import java.util.concurrent.locks.Lock;
   33 | import java.util.concurrent.locks.ReentrantLock;
   34 | 
   35 | import org.apache.commons.logging.Log;
   36 | import org.apache.commons.logging.LogFactory;
   37 | import org.jspecify.annotations.Nullable;
   38 | 
   39 | import org.springframework.beans.BeansException;
   40 | import org.springframework.beans.CachedIntrospectionResults;
   41 | import org.springframework.beans.factory.BeanFactory;
   42 | import org.springframework.beans.factory.BeanFactoryInitializer;
   43 | import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
   44 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   45 | import org.springframework.beans.factory.ObjectProvider;
   46 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   47 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   48 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   49 | import org.springframework.beans.support.ResourceEditorRegistrar;
   50 | import org.springframework.context.ApplicationContext;
   51 | import org.springframework.context.ApplicationContextAware;
   52 | import org.springframework.context.ApplicationEvent;
   53 | import org.springframework.context.ApplicationEventPublisher;
   54 | import org.springframework.context.ApplicationEventPublisherAware;
   55 | import org.springframework.context.ApplicationListener;
   56 | import org.springframework.context.ApplicationStartupAware;
   57 | import org.springframework.context.ConfigurableApplicationContext;
   58 | import org.springframework.context.EmbeddedValueResolverAware;
   59 | import org.springframework.context.EnvironmentAware;
   60 | import org.springframework.context.HierarchicalMessageSource;
   61 | import org.springframework.context.LifecycleProcessor;
   62 | import org.springframework.context.MessageSource;
   63 | import org.springframework.context.MessageSourceAware;
   64 | import org.springframework.context.MessageSourceResolvable;
   65 | import org.springframework.context.NoSuchMessageException;
   66 | import org.springframework.context.PayloadApplicationEvent;
   67 | import org.springframework.context.ResourceLoaderAware;
   68 | import org.springframework.context.event.ApplicationEventMulticaster;
   69 | import org.springframework.context.event.ContextClosedEvent;
   70 | import org.springframework.context.event.ContextPausedEvent;
   71 | import org.springframework.context.event.ContextRefreshedEvent;
   72 | import org.springframework.context.event.ContextRestartedEvent;
   73 | import org.springframework.context.event.ContextStartedEvent;
   74 | import org.springframework.context.event.ContextStoppedEvent;
   75 | import org.springframework.context.event.SimpleApplicationEventMulticaster;
   76 | import org.springframework.context.expression.StandardBeanExpressionResolver;
   77 | import org.springframework.context.weaving.LoadTimeWeaverAware;
   78 | import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
   79 | import org.springframework.core.NativeDetector;
   80 | import org.springframework.core.ParameterizedTypeReference;
   81 | import org.springframework.core.ResolvableType;
   82 | import org.springframework.core.annotation.AnnotationUtils;
   83 | import org.springframework.core.convert.ConversionService;
   84 | import org.springframework.core.env.ConfigurableEnvironment;
   85 | import org.springframework.core.env.Environment;
   86 | import org.springframework.core.env.StandardEnvironment;
   87 | import org.springframework.core.io.DefaultResourceLoader;
   88 | import org.springframework.core.io.Resource;
   89 | import org.springframework.core.io.ResourceLoader;
   90 | import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
   91 | import org.springframework.core.io.support.ResourcePatternResolver;
   92 | import org.springframework.core.metrics.ApplicationStartup;
   93 | import org.springframework.core.metrics.StartupStep;
   94 | import org.springframework.util.Assert;
   95 | import org.springframework.util.CollectionUtils;
   96 | import org.springframework.util.ObjectUtils;
   97 | import org.springframework.util.ReflectionUtils;
   98 | 
   99 | /**
  100 |  * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
  101 |  * interface. Doesn't mandate the type of storage used for configuration; simply
  102 |  * implements common context functionality. Uses the Template Method design pattern,
  103 |  * requiring concrete subclasses to implement abstract methods.
  104 |  *
  105 |  * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
  106 |  * to detect special beans defined in its internal bean factory:
  107 |  * Therefore, this class automatically registers
  108 |  * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
  109 |  * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
  110 |  * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
  111 |  * which are defined as beans in the context.
  112 |  *
  113 |  * <p>A {@link org.springframework.context.MessageSource} may also be supplied
  114 |  * as a bean in the context, with the name "messageSource"; otherwise, message
  115 |  * resolution is delegated to the parent context. Furthermore, a multicaster
  116 |  * for application events can be supplied as an "applicationEventMulticaster" bean
  117 |  * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
  118 |  * in the context; otherwise, a default multicaster of type
  119 |  * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
  120 |  *
  121 |  * <p>Implements resource loading by extending
  122 |  * {@link org.springframework.core.io.DefaultResourceLoader}.
  123 |  * Consequently treats non-URL resource paths as class path resources
  124 |  * (supporting full class path resource names that include the package path,
  125 |  * for example, "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
  126 |  * method is overridden in a subclass.
  127 |  *
  128 |  * @author Rod Johnson
  129 |  * @author Juergen Hoeller
  130 |  * @author Mark Fisher
  131 |  * @author Stephane Nicoll
  132 |  * @author Sam Brannen
  133 |  * @author Sebastien Deleuze
  134 |  * @author Brian Clozel
  135 |  * @author Yanming Zhou
  136 |  * @since January 21, 2001
  137 |  * @see #refreshBeanFactory
  138 |  * @see #getBeanFactory
  139 |  * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
  140 |  * @see org.springframework.beans.factory.config.BeanPostProcessor
  141 |  * @see org.springframework.context.event.ApplicationEventMulticaster
  142 |  * @see org.springframework.context.ApplicationListener
  143 |  * @see org.springframework.context.MessageSource
  144 |  */
  145 | public abstract class AbstractApplicationContext extends DefaultResourceLoader
  146 | 		implements ConfigurableApplicationContext {
  147 | 
  148 | 	/**
  149 | 	 * The name of the {@link MessageSource} bean in the context.
  150 | 	 * If none is supplied, message resolution is delegated to the parent.
  151 | 	 * @see org.springframework.context.MessageSource
  152 | 	 * @see org.springframework.context.support.ResourceBundleMessageSource
  153 | 	 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
  154 | 	 * @see #getMessage(MessageSourceResolvable, Locale)
  155 | 	 */
  156 | 	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";
  157 | 
  158 | 	/**
  159 | 	 * The name of the {@link ApplicationEventMulticaster} bean in the context.
  160 | 	 * If none is supplied, a {@link SimpleApplicationEventMulticaster} is used.
  161 | 	 * @see org.springframework.context.event.ApplicationEventMulticaster
  162 | 	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
  163 | 	 * @see #publishEvent(ApplicationEvent)
  164 | 	 * @see #addApplicationListener(ApplicationListener)
  165 | 	 */
  166 | 	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";
  167 | 
  168 | 	/**
  169 | 	 * The name of the {@link LifecycleProcessor} bean in the context.
  170 | 	 * If none is supplied, a {@link DefaultLifecycleProcessor} is used.
  171 | 	 * @since 3.0
  172 | 	 * @see org.springframework.context.LifecycleProcessor
  173 | 	 * @see org.springframework.context.support.DefaultLifecycleProcessor
  174 | 	 * @see #start()
  175 | 	 * @see #stop()
  176 | 	 */
  177 | 	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";
  178 | 
  179 | 
  180 | 	static {
  181 | 		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
  182 | 		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
  183 | 		ContextClosedEvent.class.getName();
  184 | 	}
  185 | 
  186 | 
  187 | 	/** Logger used by this class. Available to subclasses. */
  188 | 	protected final Log logger = LogFactory.getLog(getClass());
  189 | 
  190 | 	/** Unique id for this context, if any. */
  191 | 	private String id = ObjectUtils.identityToString(this);
  192 | 
  193 | 	/** Display name. */
  194 | 	private String displayName = ObjectUtils.identityToString(this);
  195 | 
  196 | 	/** Parent context. */
  197 | 	private @Nullable ApplicationContext parent;
  198 | 
  199 | 	/** Environment used by this context. */
  200 | 	private @Nullable ConfigurableEnvironment environment;
  201 | 
  202 | 	/** BeanFactoryPostProcessors to apply on refresh. */
  203 | 	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
  204 | 
  205 | 	/** System time in milliseconds when this context started. */
  206 | 	private long startupDate;
  207 | 
  208 | 	/** Flag that indicates whether this context is currently active. */
  209 | 	private final AtomicBoolean active = new AtomicBoolean();
  210 | 
  211 | 	/** Flag that indicates whether this context has been closed already. */
  212 | 	private final AtomicBoolean closed = new AtomicBoolean();
  213 | 
  214 | 	/** Synchronization lock for "refresh" and "close". */
  215 | 	private final Lock startupShutdownLock = new ReentrantLock();
  216 | 
  217 | 	/** Currently active startup/shutdown thread. */
  218 | 	private volatile @Nullable Thread startupShutdownThread;
  219 | 
  220 | 	/** Reference to the JVM shutdown hook, if registered. */
  221 | 	private @Nullable Thread shutdownHook;
  222 | 
  223 | 	/** ResourcePatternResolver used by this context. */
  224 | 	private final ResourcePatternResolver resourcePatternResolver;
  225 | 
  226 | 	/** LifecycleProcessor for managing the lifecycle of beans within this context. */
  227 | 	private @Nullable LifecycleProcessor lifecycleProcessor;
  228 | 
  229 | 	/** MessageSource we delegate our implementation of this interface to. */
  230 | 	private @Nullable MessageSource messageSource;
  231 | 
  232 | 	/** Helper class used in event publishing. */
  233 | 	private @Nullable ApplicationEventMulticaster applicationEventMulticaster;
  234 | 
  235 | 	/** Application startup metrics. */
  236 | 	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
  237 | 
  238 | 	/** Statically specified listeners. */
  239 | 	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
  240 | 
  241 | 	/** Local listeners registered before refresh. */
  242 | 	private @Nullable Set<ApplicationListener<?>> earlyApplicationListeners;
  243 | 
  244 | 	/** ApplicationEvents published before the multicaster setup. */
  245 | 	private @Nullable Set<ApplicationEvent> earlyApplicationEvents;
  246 | 
  247 | 
  248 | 	/**
  249 | 	 * Create a new AbstractApplicationContext with no parent.
  250 | 	 */
  251 | 	public AbstractApplicationContext() {
  252 | 		this.resourcePatternResolver = getResourcePatternResolver();
  253 | 	}
  254 | 
  255 | 	/**
  256 | 	 * Create a new AbstractApplicationContext with the given parent context.
  257 | 	 * @param parent the parent context
  258 | 	 */
  259 | 	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
  260 | 		this();
  261 | 		setParent(parent);
  262 | 	}
  263 | 
  264 | 
  265 | 	//---------------------------------------------------------------------
  266 | 	// Implementation of ApplicationContext interface
  267 | 	//---------------------------------------------------------------------
  268 | 
  269 | 	/**
  270 | 	 * Set the unique id of this application context.
  271 | 	 * <p>Default is the object id of the context instance, or the name
  272 | 	 * of the context bean if the context is itself defined as a bean.
  273 | 	 * @param id the unique id of the context
  274 | 	 */
  275 | 	@Override
  276 | 	public void setId(String id) {
  277 | 		this.id = id;
  278 | 	}
  279 | 
  280 | 	@Override
  281 | 	public String getId() {
  282 | 		return this.id;
  283 | 	}
  284 | 
  285 | 	@Override
  286 | 	public String getApplicationName() {
  287 | 		return "";
  288 | 	}
  289 | 
  290 | 	/**
  291 | 	 * Set a friendly name for this context.
  292 | 	 * Typically done during initialization of concrete context implementations.
  293 | 	 * <p>Default is the object id of the context instance.
  294 | 	 */
  295 | 	public void setDisplayName(String displayName) {
  296 | 		Assert.hasLength(displayName, "Display name must not be empty");
  297 | 		this.displayName = displayName;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Return a friendly name for this context.
  302 | 	 * @return a display name for this context (never {@code null})
  303 | 	 */
  304 | 	@Override
  305 | 	public String getDisplayName() {
  306 | 		return this.displayName;
  307 | 	}
  308 | 
  309 | 	/**
  310 | 	 * Return the parent context, or {@code null} if there is no parent
  311 | 	 * (that is, this context is the root of the context hierarchy).
  312 | 	 */
  313 | 	@Override
  314 | 	public @Nullable ApplicationContext getParent() {
  315 | 		return this.parent;
  316 | 	}
  317 | 
  318 | 	/**
  319 | 	 * Set the {@code Environment} for this application context.
  320 | 	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
  321 | 	 * default with this method is one option but configuration through {@link
  322 | 	 * #getEnvironment()} should also be considered. In either case, such modifications
  323 | 	 * should be performed <em>before</em> {@link #refresh()}.
  324 | 	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
  325 | 	 */
  326 | 	@Override
  327 | 	public void setEnvironment(ConfigurableEnvironment environment) {
  328 | 		this.environment = environment;
  329 | 	}
  330 | 
  331 | 	/**
  332 | 	 * Return the {@code Environment} for this application context in configurable
  333 | 	 * form, allowing for further customization.
  334 | 	 * <p>If none specified, a default environment will be initialized via
  335 | 	 * {@link #createEnvironment()}.
  336 | 	 */
  337 | 	@Override
  338 | 	public ConfigurableEnvironment getEnvironment() {
  339 | 		if (this.environment == null) {
  340 | 			this.environment = createEnvironment();
  341 | 		}
  342 | 		return this.environment;
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Create and return a new {@link StandardEnvironment}.
  347 | 	 * <p>Subclasses may override this method in order to supply
  348 | 	 * a custom {@link ConfigurableEnvironment} implementation.
  349 | 	 */
  350 | 	protected ConfigurableEnvironment createEnvironment() {
  351 | 		return new StandardEnvironment();
  352 | 	}
  353 | 
  354 | 	/**
  355 | 	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
  356 | 	 * if already available.
  357 | 	 * @see #getBeanFactory()
  358 | 	 */
  359 | 	@Override
  360 | 	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
  361 | 		return getBeanFactory();
  362 | 	}
  363 | 
  364 | 	/**
  365 | 	 * Return the timestamp (ms) when this context was first loaded.
  366 | 	 */
  367 | 	@Override
  368 | 	public long getStartupDate() {
  369 | 		return this.startupDate;
  370 | 	}
  371 | 
  372 | 	/**
  373 | 	 * Publish the given event to all listeners.
  374 | 	 * <p>Note: Listeners get initialized after the MessageSource, to be able
  375 | 	 * to access it within listener implementations. Thus, MessageSource
  376 | 	 * implementations cannot publish events.
  377 | 	 * @param event the event to publish (may be application-specific or a
  378 | 	 * standard framework event)
  379 | 	 */
  380 | 	@Override
  381 | 	public void publishEvent(ApplicationEvent event) {
  382 | 		publishEvent(event, null);
  383 | 	}
  384 | 
  385 | 	/**
  386 | 	 * Publish the given event to all listeners.
  387 | 	 * <p>Note: Listeners get initialized after the MessageSource, to be able
  388 | 	 * to access it within listener implementations. Thus, MessageSource
  389 | 	 * implementations cannot publish events.
  390 | 	 * @param event the event to publish (may be an {@link ApplicationEvent}
  391 | 	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
  392 | 	 */
  393 | 	@Override
  394 | 	public void publishEvent(Object event) {
  395 | 		publishEvent(event, null);
  396 | 	}
  397 | 
  398 | 	/**
  399 | 	 * Publish the given event to all listeners.
  400 | 	 * <p>This is the internal delegate that all other {@code publishEvent}
  401 | 	 * methods refer to. It is not meant to be called directly but rather serves
  402 | 	 * as a propagation mechanism between application contexts in a hierarchy,
  403 | 	 * potentially overridden in subclasses for a custom propagation arrangement.
  404 | 	 * @param event the event to publish (may be an {@link ApplicationEvent}
  405 | 	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
  406 | 	 * @param typeHint the resolved event type, if known.
  407 | 	 * The implementation of this method also tolerates a payload type hint for
  408 | 	 * a payload object to be turned into a {@link PayloadApplicationEvent}.
  409 | 	 * However, the recommended way is to construct an actual event object via
  410 | 	 * {@link PayloadApplicationEvent#PayloadApplicationEvent(Object, Object, ResolvableType)}
  411 | 	 * instead for such scenarios.
  412 | 	 * @since 4.2
  413 | 	 * @see ApplicationEventMulticaster#multicastEvent(ApplicationEvent, ResolvableType)
  414 | 	 */
  415 | 	protected void publishEvent(Object event, @Nullable ResolvableType typeHint) {
  416 | 		Assert.notNull(event, "Event must not be null");
  417 | 		ResolvableType eventType = null;
  418 | 
  419 | 		// Decorate event as an ApplicationEvent if necessary
  420 | 		ApplicationEvent applicationEvent;
  421 | 		if (event instanceof ApplicationEvent applEvent) {
  422 | 			applicationEvent = applEvent;
  423 | 			eventType = typeHint;
  424 | 		}
  425 | 		else {
  426 | 			ResolvableType payloadType = null;
  427 | 			if (typeHint != null && ApplicationEvent.class.isAssignableFrom(typeHint.toClass())) {
  428 | 				eventType = typeHint;
  429 | 			}
  430 | 			else {
  431 | 				payloadType = typeHint;
  432 | 			}
  433 | 			applicationEvent = new PayloadApplicationEvent<>(this, event, payloadType);
  434 | 		}
  435 | 
  436 | 		// Determine event type only once (for multicast and parent publish)
  437 | 		if (eventType == null) {
  438 | 			eventType = ResolvableType.forInstance(applicationEvent);
  439 | 			if (typeHint == null) {
  440 | 				typeHint = eventType;
  441 | 			}
  442 | 		}
  443 | 
  444 | 		// Multicast right now if possible - or lazily once the multicaster is initialized
  445 | 		if (this.earlyApplicationEvents != null) {
  446 | 			this.earlyApplicationEvents.add(applicationEvent);
  447 | 		}
  448 | 		else if (this.applicationEventMulticaster != null) {
  449 | 			this.applicationEventMulticaster.multicastEvent(applicationEvent, eventType);
  450 | 		}
  451 | 
  452 | 		// Publish event via parent context as well...
  453 | 		if (this.parent != null) {
  454 | 			if (this.parent instanceof AbstractApplicationContext abstractApplicationContext) {
  455 | 				abstractApplicationContext.publishEvent(event, typeHint);
  456 | 			}
  457 | 			else {
  458 | 				this.parent.publishEvent(event);
  459 | 			}
  460 | 		}
  461 | 	}
  462 | 
  463 | 	/**
  464 | 	 * Return the internal ApplicationEventMulticaster used by the context.
  465 | 	 * @return the internal ApplicationEventMulticaster (never {@code null})
  466 | 	 * @throws IllegalStateException if the context has not been initialized yet
  467 | 	 */
  468 | 	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
  469 | 		if (this.applicationEventMulticaster == null) {
  470 | 			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
  471 | 					"call 'refresh' before multicasting events via the context: " + this);
  472 | 		}
  473 | 		return this.applicationEventMulticaster;
  474 | 	}
  475 | 
  476 | 	@Override
  477 | 	public void setApplicationStartup(ApplicationStartup applicationStartup) {
  478 | 		Assert.notNull(applicationStartup, "ApplicationStartup must not be null");
  479 | 		this.applicationStartup = applicationStartup;
  480 | 	}
  481 | 
  482 | 	@Override
  483 | 	public ApplicationStartup getApplicationStartup() {
  484 | 		return this.applicationStartup;
  485 | 	}
  486 | 
  487 | 	/**
  488 | 	 * Return the internal LifecycleProcessor used by the context.
  489 | 	 * @return the internal LifecycleProcessor (never {@code null})
  490 | 	 * @throws IllegalStateException if the context has not been initialized yet
  491 | 	 */
  492 | 	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
  493 | 		if (this.lifecycleProcessor == null) {
  494 | 			throw new IllegalStateException("LifecycleProcessor not initialized - " +
  495 | 					"call 'refresh' before invoking lifecycle methods via the context: " + this);
  496 | 		}
  497 | 		return this.lifecycleProcessor;
  498 | 	}
  499 | 
  500 | 	/**
  501 | 	 * Return the ResourcePatternResolver to use for resolving location patterns
  502 | 	 * into Resource instances. Default is a
  503 | 	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
  504 | 	 * supporting Ant-style location patterns.
  505 | 	 * <p>Can be overridden in subclasses, for extended resolution strategies,
  506 | 	 * for example in a web environment.
  507 | 	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
  508 | 	 * Call the context's {@code getResources} method instead, which
  509 | 	 * will delegate to the ResourcePatternResolver.
  510 | 	 * @return the ResourcePatternResolver for this context
  511 | 	 * @see #getResources
  512 | 	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
  513 | 	 */
  514 | 	protected ResourcePatternResolver getResourcePatternResolver() {
  515 | 		return new PathMatchingResourcePatternResolver(this);
  516 | 	}
  517 | 
  518 | 
  519 | 	//---------------------------------------------------------------------
  520 | 	// Implementation of ConfigurableApplicationContext interface
  521 | 	//---------------------------------------------------------------------
  522 | 
  523 | 	/**
  524 | 	 * Set the parent of this application context.
  525 | 	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
  526 | 	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
  527 | 	 * this (child) application context environment if the parent is non-{@code null} and
  528 | 	 * its environment is an instance of {@link ConfigurableEnvironment}.
  529 | 	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
  530 | 	 */
  531 | 	@Override
  532 | 	public void setParent(@Nullable ApplicationContext parent) {
  533 | 		this.parent = parent;
  534 | 		if (parent != null) {
  535 | 			Environment parentEnvironment = parent.getEnvironment();
  536 | 			if (parentEnvironment instanceof ConfigurableEnvironment configurableEnvironment) {
  537 | 				getEnvironment().merge(configurableEnvironment);
  538 | 			}
  539 | 		}
  540 | 	}
  541 | 
  542 | 	@Override
  543 | 	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
  544 | 		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
  545 | 		this.beanFactoryPostProcessors.add(postProcessor);
  546 | 	}
  547 | 
  548 | 	/**
  549 | 	 * Return the list of BeanFactoryPostProcessors that will get applied
  550 | 	 * to the internal BeanFactory.
  551 | 	 */
  552 | 	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
  553 | 		return this.beanFactoryPostProcessors;
  554 | 	}
  555 | 
  556 | 	@Override
  557 | 	public void addApplicationListener(ApplicationListener<?> listener) {
  558 | 		Assert.notNull(listener, "ApplicationListener must not be null");
  559 | 		if (this.applicationEventMulticaster != null) {
  560 | 			this.applicationEventMulticaster.addApplicationListener(listener);
  561 | 		}
  562 | 		this.applicationListeners.add(listener);
  563 | 	}
  564 | 
  565 | 	@Override
  566 | 	public void removeApplicationListener(ApplicationListener<?> listener) {
  567 | 		Assert.notNull(listener, "ApplicationListener must not be null");
  568 | 		if (this.applicationEventMulticaster != null) {
  569 | 			this.applicationEventMulticaster.removeApplicationListener(listener);
  570 | 		}
  571 | 		this.applicationListeners.remove(listener);
  572 | 	}
  573 | 
  574 | 	/**
  575 | 	 * Return the list of statically specified ApplicationListeners.
  576 | 	 */
  577 | 	public Collection<ApplicationListener<?>> getApplicationListeners() {
  578 | 		return this.applicationListeners;
  579 | 	}
  580 | 
  581 | 	@Override
  582 | 	public void refresh() throws BeansException, IllegalStateException {
  583 | 		this.startupShutdownLock.lock();
  584 | 		try {
  585 | 			this.startupShutdownThread = Thread.currentThread();
  586 | 
  587 | 			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");
  588 | 
  589 | 			// Prepare this context for refreshing.
  590 | 			prepareRefresh();
  591 | 
  592 | 			// Tell the subclass to refresh the internal bean factory.
  593 | 			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
  594 | 
  595 | 			// Prepare the bean factory for use in this context.
  596 | 			prepareBeanFactory(beanFactory);
  597 | 
  598 | 			try {
  599 | 				// Allows post-processing of the bean factory in context subclasses.
  600 | 				postProcessBeanFactory(beanFactory);
  601 | 
  602 | 				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
  603 | 				// Invoke factory processors registered as beans in the context.
  604 | 				invokeBeanFactoryPostProcessors(beanFactory);
  605 | 				// Register bean processors that intercept bean creation.
  606 | 				registerBeanPostProcessors(beanFactory);
  607 | 				beanPostProcess.end();
  608 | 
  609 | 				// Initialize message source for this context.
  610 | 				initMessageSource();
  611 | 
  612 | 				// Initialize event multicaster for this context.
  613 | 				initApplicationEventMulticaster();
  614 | 
  615 | 				// Initialize other special beans in specific context subclasses.
  616 | 				onRefresh();
  617 | 
  618 | 				// Check for listener beans and register them.
  619 | 				registerListeners();
  620 | 
  621 | 				// Instantiate all remaining (non-lazy-init) singletons.
  622 | 				finishBeanFactoryInitialization(beanFactory);
  623 | 
  624 | 				// Last step: publish corresponding event.
  625 | 				finishRefresh();
  626 | 			}
  627 | 
  628 | 			catch (RuntimeException | Error ex) {
  629 | 				if (logger.isWarnEnabled()) {
  630 | 					logger.warn("Exception encountered during context initialization - " +
  631 | 							"cancelling refresh attempt: " + ex);
  632 | 				}
  633 | 
  634 | 				// Stop already started Lifecycle beans to avoid dangling resources.
  635 | 				if (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning()) {
  636 | 					try {
  637 | 						this.lifecycleProcessor.stop();
  638 | 					}
  639 | 					catch (Throwable ex2) {
  640 | 						logger.warn("Exception thrown from LifecycleProcessor on cancelled refresh", ex2);
  641 | 					}
  642 | 				}
  643 | 
  644 | 				// Destroy already created singletons to avoid dangling resources.
  645 | 				destroyBeans();
  646 | 
  647 | 				// Reset 'active' flag.
  648 | 				cancelRefresh(ex);
  649 | 
  650 | 				// Propagate exception to caller.
  651 | 				throw ex;
  652 | 			}
  653 | 
  654 | 			finally {
  655 | 				contextRefresh.end();
  656 | 			}
  657 | 		}
  658 | 		finally {
  659 | 			this.startupShutdownThread = null;
  660 | 			this.startupShutdownLock.unlock();
  661 | 		}
  662 | 	}
  663 | 
  664 | 	/**
  665 | 	 * Prepare this context for refreshing, setting its startup date and
  666 | 	 * active flag as well as performing any initialization of property sources.
  667 | 	 */
  668 | 	protected void prepareRefresh() {
  669 | 		// Switch to active.
  670 | 		this.startupDate = System.currentTimeMillis();
  671 | 		this.closed.set(false);
  672 | 		this.active.set(true);
  673 | 
  674 | 		if (logger.isDebugEnabled()) {
  675 | 			if (logger.isTraceEnabled()) {
  676 | 				logger.trace("Refreshing " + this);
  677 | 			}
  678 | 			else {
  679 | 				logger.debug("Refreshing " + getDisplayName());
  680 | 			}
  681 | 		}
  682 | 
  683 | 		// Initialize any placeholder property sources in the context environment.
  684 | 		initPropertySources();
  685 | 
  686 | 		// Validate that all properties marked as required are resolvable:
  687 | 		// see ConfigurablePropertyResolver#setRequiredProperties
  688 | 		getEnvironment().validateRequiredProperties();
  689 | 
  690 | 		// Store pre-refresh ApplicationListeners...
  691 | 		if (this.earlyApplicationListeners == null) {
  692 | 			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
  693 | 		}
  694 | 		else {
  695 | 			// Reset local application listeners to pre-refresh state.
  696 | 			this.applicationListeners.clear();
  697 | 			this.applicationListeners.addAll(this.earlyApplicationListeners);
  698 | 		}
  699 | 
  700 | 		// Allow for the collection of early ApplicationEvents,
  701 | 		// to be published once the multicaster is available...
  702 | 		this.earlyApplicationEvents = new LinkedHashSet<>();
  703 | 	}
  704 | 
  705 | 	/**
  706 | 	 * <p>Replace any stub property sources with actual instances.
  707 | 	 * @see org.springframework.core.env.PropertySource.StubPropertySource
  708 | 	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
  709 | 	 */
  710 | 	protected void initPropertySources() {
  711 | 		// For subclasses: do nothing by default.
  712 | 	}
  713 | 
  714 | 	/**
  715 | 	 * Tell the subclass to refresh the internal bean factory.
  716 | 	 * @return the fresh BeanFactory instance
  717 | 	 * @see #refreshBeanFactory()
  718 | 	 * @see #getBeanFactory()
  719 | 	 */
  720 | 	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
  721 | 		refreshBeanFactory();
  722 | 		return getBeanFactory();
  723 | 	}
  724 | 
  725 | 	/**
  726 | 	 * Configure the factory's standard context characteristics,
  727 | 	 * such as the context's ClassLoader and post-processors.
  728 | 	 * @param beanFactory the BeanFactory to configure
  729 | 	 */
  730 | 	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  731 | 		// Tell the internal bean factory to use the context's class loader etc.
  732 | 		beanFactory.setBeanClassLoader(getClassLoader());
  733 | 		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
  734 | 		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));
  735 | 
  736 | 		// Configure the bean factory with context callbacks.
  737 | 		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
  738 | 		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
  739 | 		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
  740 | 		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
  741 | 		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
  742 | 		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
  743 | 		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
  744 | 		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);
  745 | 
  746 | 		// BeanFactory interface not registered as resolvable type in a plain factory.
  747 | 		// MessageSource registered (and found for autowiring) as a bean.
  748 | 		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
  749 | 		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
  750 | 		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
  751 | 		beanFactory.registerResolvableDependency(ApplicationContext.class, this);
  752 | 
  753 | 		// Register early post-processor for detecting inner beans as ApplicationListeners.
  754 | 		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
  755 | 
  756 | 		// Detect a LoadTimeWeaver and prepare for weaving, if found.
  757 | 		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
  758 | 			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
  759 | 			// Set a temporary ClassLoader for type matching.
  760 | 			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
  761 | 		}
  762 | 
  763 | 		// Register default environment beans.
  764 | 		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
  765 | 			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
  766 | 		}
  767 | 		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
  768 | 			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
  769 | 		}
  770 | 		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
  771 | 			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
  772 | 		}
  773 | 		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
  774 | 			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
  775 | 		}
  776 | 	}
  777 | 
  778 | 	/**
  779 | 	 * Modify the application context's internal bean factory after its standard
  780 | 	 * initialization. The initial definition resources will have been loaded but no
  781 | 	 * post-processors will have run and no derived bean definitions will have been
  782 | 	 * registered, and most importantly, no beans will have been instantiated yet.
  783 | 	 * <p>This template method allows for registering special BeanPostProcessors
  784 | 	 * etc in certain AbstractApplicationContext subclasses.
  785 | 	 * @param beanFactory the bean factory used by the application context
  786 | 	 */
  787 | 	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  788 | 	}
  789 | 
  790 | 	/**
  791 | 	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
  792 | 	 * respecting explicit order if given.
  793 | 	 * <p>Must be called before singleton instantiation.
  794 | 	 */
  795 | 	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
  796 | 		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
  797 | 
  798 | 		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
  799 | 		// (for example, through a @Bean method registered by ConfigurationClassPostProcessor)
  800 | 		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null &&
  801 | 				beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
  802 | 			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
  803 | 			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
  804 | 		}
  805 | 	}
  806 | 
  807 | 	/**
  808 | 	 * Instantiate and register all BeanPostProcessor beans,
  809 | 	 * respecting explicit order if given.
  810 | 	 * <p>Must be called before any instantiation of application beans.
  811 | 	 */
  812 | 	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
  813 | 		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
  814 | 	}
  815 | 
  816 | 	/**
  817 | 	 * Initialize the {@link MessageSource}.
  818 | 	 * <p>Uses parent's {@code MessageSource} if none defined in this context.
  819 | 	 * @see #MESSAGE_SOURCE_BEAN_NAME
  820 | 	 */
  821 | 	protected void initMessageSource() {
  822 | 		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
  823 | 		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
  824 | 			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
  825 | 			// Make MessageSource aware of parent MessageSource.
  826 | 			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource hms &&
  827 | 					hms.getParentMessageSource() == null) {
  828 | 				// Only set parent context as parent MessageSource if no parent MessageSource
  829 | 				// registered already.
  830 | 				hms.setParentMessageSource(getInternalParentMessageSource());
  831 | 			}
  832 | 			if (logger.isTraceEnabled()) {
  833 | 				logger.trace("Using MessageSource [" + this.messageSource + "]");
  834 | 			}
  835 | 		}
  836 | 		else {
  837 | 			// Use empty MessageSource to be able to accept getMessage calls.
  838 | 			DelegatingMessageSource dms = new DelegatingMessageSource();
  839 | 			dms.setParentMessageSource(getInternalParentMessageSource());
  840 | 			this.messageSource = dms;
  841 | 			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
  842 | 			if (logger.isTraceEnabled()) {
  843 | 				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
  844 | 			}
  845 | 		}
  846 | 	}
  847 | 
  848 | 	/**
  849 | 	 * Initialize the {@link ApplicationEventMulticaster}.
  850 | 	 * <p>Uses {@link SimpleApplicationEventMulticaster} if none defined in the context.
  851 | 	 * @see #APPLICATION_EVENT_MULTICASTER_BEAN_NAME
  852 | 	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
  853 | 	 */
  854 | 	protected void initApplicationEventMulticaster() {
  855 | 		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
  856 | 		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
  857 | 			this.applicationEventMulticaster =
  858 | 					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
  859 | 			if (logger.isTraceEnabled()) {
  860 | 				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
  861 | 			}
  862 | 		}
  863 | 		else {
  864 | 			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
  865 | 			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
  866 | 			if (logger.isTraceEnabled()) {
  867 | 				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
  868 | 						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
  869 | 			}
  870 | 		}
  871 | 	}
  872 | 
  873 | 	/**
  874 | 	 * Initialize the {@link LifecycleProcessor}.
  875 | 	 * <p>Uses {@link DefaultLifecycleProcessor} if none defined in the context.
  876 | 	 * @since 3.0
  877 | 	 * @see #LIFECYCLE_PROCESSOR_BEAN_NAME
  878 | 	 * @see org.springframework.context.support.DefaultLifecycleProcessor
  879 | 	 */
  880 | 	protected void initLifecycleProcessor() {
  881 | 		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
  882 | 		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
  883 | 			this.lifecycleProcessor = beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
  884 | 			if (logger.isTraceEnabled()) {
  885 | 				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
  886 | 			}
  887 | 		}
  888 | 		else {
  889 | 			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
  890 | 			defaultProcessor.setBeanFactory(beanFactory);
  891 | 			this.lifecycleProcessor = defaultProcessor;
  892 | 			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
  893 | 			if (logger.isTraceEnabled()) {
  894 | 				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
  895 | 						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
  896 | 			}
  897 | 		}
  898 | 	}
  899 | 
  900 | 	/**
  901 | 	 * Template method which can be overridden to add context-specific refresh work.
  902 | 	 * Called on initialization of special beans, before instantiation of singletons.
  903 | 	 * <p>This implementation is empty.
  904 | 	 * @throws BeansException in case of errors
  905 | 	 * @see #refresh()
  906 | 	 */
  907 | 	protected void onRefresh() throws BeansException {
  908 | 		// For subclasses: do nothing by default.
  909 | 	}
  910 | 
  911 | 	/**
  912 | 	 * Add beans that implement ApplicationListener as listeners.
  913 | 	 * Doesn't affect other listeners, which can be added without being beans.
  914 | 	 */
  915 | 	protected void registerListeners() {
  916 | 		// Register statically specified listeners first.
  917 | 		for (ApplicationListener<?> listener : getApplicationListeners()) {
  918 | 			getApplicationEventMulticaster().addApplicationListener(listener);
  919 | 		}
  920 | 
  921 | 		// Do not initialize FactoryBeans here: We need to leave all regular beans
  922 | 		// uninitialized to let post-processors apply to them!
  923 | 		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
  924 | 		for (String listenerBeanName : listenerBeanNames) {
  925 | 			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
  926 | 		}
  927 | 
  928 | 		// Publish early application events now that we finally have a multicaster...
  929 | 		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
  930 | 		this.earlyApplicationEvents = null;
  931 | 		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
  932 | 			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
  933 | 				getApplicationEventMulticaster().multicastEvent(earlyEvent);
  934 | 			}
  935 | 		}
  936 | 	}
  937 | 
  938 | 	/**
  939 | 	 * Finish the initialization of this context's bean factory,
  940 | 	 * initializing all remaining singleton beans.
  941 | 	 */
  942 | 	@SuppressWarnings("unchecked")
  943 | 	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
  944 | 		// Mark current thread for singleton instantiation with applied bootstrap locking.
  945 | 		beanFactory.prepareSingletonBootstrap();
  946 | 
  947 | 		// Initialize bootstrap executor for this context.
  948 | 		if (beanFactory.containsBean(BOOTSTRAP_EXECUTOR_BEAN_NAME) &&
  949 | 				beanFactory.isTypeMatch(BOOTSTRAP_EXECUTOR_BEAN_NAME, Executor.class)) {
  950 | 			beanFactory.setBootstrapExecutor(
  951 | 					beanFactory.getBean(BOOTSTRAP_EXECUTOR_BEAN_NAME, Executor.class));
  952 | 		}
  953 | 
  954 | 		// Initialize conversion service for this context.
  955 | 		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
  956 | 				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
  957 | 			beanFactory.setConversionService(
  958 | 					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
  959 | 		}
  960 | 
  961 | 		// Register a default embedded value resolver if no BeanFactoryPostProcessor
  962 | 		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
  963 | 		// at this point, primarily for resolution in annotation attribute values.
  964 | 		if (!beanFactory.hasEmbeddedValueResolver()) {
  965 | 			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
  966 | 		}
  967 | 
  968 | 		// Call BeanFactoryInitializer beans early to allow for initializing specific other beans early.
  969 | 		String[] initializerNames = beanFactory.getBeanNamesForType(BeanFactoryInitializer.class, false, false);
  970 | 		for (String initializerName : initializerNames) {
  971 | 			beanFactory.getBean(initializerName, BeanFactoryInitializer.class).initialize(beanFactory);
  972 | 		}
  973 | 
  974 | 		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
  975 | 		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
  976 | 		for (String weaverAwareName : weaverAwareNames) {
  977 | 			try {
  978 | 				beanFactory.getBean(weaverAwareName, LoadTimeWeaverAware.class);
  979 | 			}
  980 | 			catch (BeanNotOfRequiredTypeException ex) {
  981 | 				if (logger.isDebugEnabled()) {
  982 | 					logger.debug("Failed to initialize LoadTimeWeaverAware bean '" + weaverAwareName +
  983 | 							"' due to unexpected type mismatch: " + ex.getMessage());
  984 | 				}
  985 | 			}
  986 | 		}
  987 | 
  988 | 		// Stop using the temporary ClassLoader for type matching.
  989 | 		beanFactory.setTempClassLoader(null);
  990 | 
  991 | 		// Allow for caching all bean definition metadata, not expecting further changes.
  992 | 		beanFactory.freezeConfiguration();
  993 | 
  994 | 		// Instantiate all remaining (non-lazy-init) singletons.
  995 | 		beanFactory.preInstantiateSingletons();
  996 | 	}
  997 | 
  998 | 	/**
  999 | 	 * Finish the refresh of this context, invoking the LifecycleProcessor's
 1000 | 	 * onRefresh() method and publishing the
 1001 | 	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
 1002 | 	 */
 1003 | 	protected void finishRefresh() {
 1004 | 		// Reset common introspection caches in Spring's core infrastructure.
 1005 | 		resetCommonCaches();
 1006 | 
 1007 | 		// Clear context-level resource caches (such as ASM metadata from scanning).
 1008 | 		clearResourceCaches();
 1009 | 
 1010 | 		// Initialize lifecycle processor for this context.
 1011 | 		initLifecycleProcessor();
 1012 | 
 1013 | 		// Propagate refresh to lifecycle processor first.
 1014 | 		getLifecycleProcessor().onRefresh();
 1015 | 
 1016 | 		// Publish the final event.
 1017 | 		publishEvent(new ContextRefreshedEvent(this));
 1018 | 	}
 1019 | 
 1020 | 	/**
 1021 | 	 * Cancel this context's refresh attempt, resetting the {@code active} flag
 1022 | 	 * after an exception got thrown.
 1023 | 	 * @param ex the exception that led to the cancellation
 1024 | 	 */
 1025 | 	protected void cancelRefresh(Throwable ex) {
 1026 | 		this.active.set(false);
 1027 | 
 1028 | 		// Reset common introspection caches in Spring's core infrastructure.
 1029 | 		resetCommonCaches();
 1030 | 	}
 1031 | 
 1032 | 	/**
 1033 | 	 * Reset Spring's common reflection metadata caches, in particular the
 1034 | 	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
 1035 | 	 * and {@link CachedIntrospectionResults} caches.
 1036 | 	 * @since 4.2
 1037 | 	 * @see ReflectionUtils#clearCache()
 1038 | 	 * @see AnnotationUtils#clearCache()
 1039 | 	 * @see ResolvableType#clearCache()
 1040 | 	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
 1041 | 	 */
 1042 | 	protected void resetCommonCaches() {
 1043 | 		ReflectionUtils.clearCache();
 1044 | 		AnnotationUtils.clearCache();
 1045 | 		ResolvableType.clearCache();
 1046 | 		CachedIntrospectionResults.clearClassLoader(getClassLoader());
 1047 | 	}
 1048 | 
 1049 | 	@Override
 1050 | 	public void clearResourceCaches() {
 1051 | 		super.clearResourceCaches();
 1052 | 		if (this.resourcePatternResolver instanceof PathMatchingResourcePatternResolver pmrpr) {
 1053 | 			pmrpr.clearCache();
 1054 | 		}
 1055 | 	}
 1056 | 
 1057 | 
 1058 | 	/**
 1059 | 	 * Register a shutdown hook {@linkplain Thread#getName() named}
 1060 | 	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
 1061 | 	 * context on JVM shutdown unless it has already been closed at that time.
 1062 | 	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
 1063 | 	 * @see Runtime#addShutdownHook
 1064 | 	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
 1065 | 	 * @see #close()
 1066 | 	 * @see #doClose()
 1067 | 	 */
 1068 | 	@Override
 1069 | 	public void registerShutdownHook() {
 1070 | 		if (this.shutdownHook == null) {
 1071 | 			// No shutdown hook registered yet.
 1072 | 			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
 1073 | 				@Override
 1074 | 				public void run() {
 1075 | 					if (!tryLockForShutdown()) {
 1076 | 						return;
 1077 | 					}
 1078 | 					try {
 1079 | 						doClose();
 1080 | 					}
 1081 | 					finally {
 1082 | 						startupShutdownLock.unlock();
 1083 | 					}
 1084 | 				}
 1085 | 			};
 1086 | 			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
 1087 | 		}
 1088 | 	}
 1089 | 
 1090 | 	/**
 1091 | 	 * Try to acquire the common startup/shutdown lock, backing out if
 1092 | 	 * the main startup/shutdown thread is stuck or on interruption.
 1093 | 	 * @see #isStartupShutdownThreadStuck()
 1094 | 	 */
 1095 | 	private boolean tryLockForShutdown() {
 1096 | 		try {
 1097 | 			while (!this.startupShutdownLock.tryLock(100, TimeUnit.MILLISECONDS)) {
 1098 | 				if (!this.active.get() || this.closed.get()) {
 1099 | 					return false;
 1100 | 				}
 1101 | 				if (isStartupShutdownThreadStuck()) {
 1102 | 					this.active.set(false);
 1103 | 					return false;
 1104 | 				}
 1105 | 			}
 1106 | 			return true;
 1107 | 		}
 1108 | 		catch (InterruptedException ex) {
 1109 | 			Thread.currentThread().interrupt();
 1110 | 			return false;
 1111 | 		}
 1112 | 	}
 1113 | 
 1114 | 	/**
 1115 | 	 * Determine whether an active startup/shutdown thread is currently stuck,
 1116 | 	 * for example, through a {@code System.exit} call in a user component.
 1117 | 	 */
 1118 | 	private boolean isStartupShutdownThreadStuck() {
 1119 | 		Thread activeThread = this.startupShutdownThread;
 1120 | 		if (activeThread != null && activeThread.getState() == Thread.State.WAITING) {
 1121 | 			// Indefinitely waiting: might be Thread.join or the like, or System.exit
 1122 | 			activeThread.interrupt();
 1123 | 			try {
 1124 | 				// Leave just a little bit of time for the interruption to show effect
 1125 | 				Thread.sleep(10);
 1126 | 			}
 1127 | 			catch (InterruptedException ex) {
 1128 | 				Thread.currentThread().interrupt();
 1129 | 			}
 1130 | 			if (activeThread.getState() == Thread.State.WAITING) {
 1131 | 				// Interrupted but still waiting: very likely a System.exit call
 1132 | 				return true;
 1133 | 			}
 1134 | 		}
 1135 | 		return false;
 1136 | 	}
 1137 | 
 1138 | 	/**
 1139 | 	 * Close this application context, destroying all beans in its bean factory.
 1140 | 	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
 1141 | 	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
 1142 | 	 * @see #doClose()
 1143 | 	 * @see #registerShutdownHook()
 1144 | 	 */
 1145 | 	@Override
 1146 | 	public void close() {
 1147 | 		if (!tryLockForShutdown()) {
 1148 | 			return;
 1149 | 		}
 1150 | 
 1151 | 		try {
 1152 | 			this.startupShutdownThread = Thread.currentThread();
 1153 | 
 1154 | 			doClose();
 1155 | 
 1156 | 			// If we registered a JVM shutdown hook, we don't need it anymore now:
 1157 | 			// We've already explicitly closed the context.
 1158 | 			if (this.shutdownHook != null) {
 1159 | 				try {
 1160 | 					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
 1161 | 				}
 1162 | 				catch (IllegalStateException ex) {
 1163 | 					// ignore - VM is already shutting down
 1164 | 				}
 1165 | 			}
 1166 | 		}
 1167 | 		finally {
 1168 | 			this.startupShutdownThread = null;
 1169 | 			this.startupShutdownLock.unlock();
 1170 | 		}
 1171 | 	}
 1172 | 
 1173 | 	/**
 1174 | 	 * Actually performs context closing: publishes a ContextClosedEvent and
 1175 | 	 * destroys the singletons in the bean factory of this application context.
 1176 | 	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
 1177 | 	 * @see org.springframework.context.event.ContextClosedEvent
 1178 | 	 * @see #destroyBeans()
 1179 | 	 * @see #close()
 1180 | 	 * @see #registerShutdownHook()
 1181 | 	 */
 1182 | 	protected void doClose() {
 1183 | 		// Check whether an actual close attempt is necessary...
 1184 | 		if (this.active.get() && this.closed.compareAndSet(false, true)) {
 1185 | 			if (logger.isDebugEnabled()) {
 1186 | 				logger.debug("Closing " + this);
 1187 | 			}
 1188 | 
 1189 | 			try {
 1190 | 				// Publish shutdown event.
 1191 | 				publishEvent(new ContextClosedEvent(this));
 1192 | 			}
 1193 | 			catch (Throwable ex) {
 1194 | 				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
 1195 | 			}
 1196 | 
 1197 | 			// Stop all Lifecycle beans, to avoid delays during individual destruction.
 1198 | 			if (this.lifecycleProcessor != null) {
 1199 | 				try {
 1200 | 					this.lifecycleProcessor.onClose();
 1201 | 				}
 1202 | 				catch (Throwable ex) {
 1203 | 					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
 1204 | 				}
 1205 | 			}
 1206 | 
 1207 | 			// Destroy all cached singletons in the context's BeanFactory.
 1208 | 			destroyBeans();
 1209 | 
 1210 | 			// Close the state of this context itself.
 1211 | 			closeBeanFactory();
 1212 | 
 1213 | 			// Let subclasses do some final clean-up if they wish...
 1214 | 			onClose();
 1215 | 
 1216 | 			// Reset common introspection caches to avoid class reference leaks.
 1217 | 			resetCommonCaches();
 1218 | 
 1219 | 			// Reset local application listeners to pre-refresh state.
 1220 | 			if (this.earlyApplicationListeners != null) {
 1221 | 				this.applicationListeners.clear();
 1222 | 				this.applicationListeners.addAll(this.earlyApplicationListeners);
 1223 | 			}
 1224 | 
 1225 | 			// Reset internal delegates.
 1226 | 			this.applicationEventMulticaster = null;
 1227 | 			this.messageSource = null;
 1228 | 			this.lifecycleProcessor = null;
 1229 | 
 1230 | 			// Switch to inactive.
 1231 | 			this.active.set(false);
 1232 | 		}
 1233 | 	}
 1234 | 
 1235 | 	/**
 1236 | 	 * Template method for destroying all beans that this context manages.
 1237 | 	 * The default implementation destroy all cached singletons in this context,
 1238 | 	 * invoking {@code DisposableBean.destroy()} and/or the specified
 1239 | 	 * "destroy-method".
 1240 | 	 * <p>Can be overridden to add context-specific bean destruction steps
 1241 | 	 * right before or right after standard singleton destruction,
 1242 | 	 * while the context's BeanFactory is still active.
 1243 | 	 * @see #getBeanFactory()
 1244 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
 1245 | 	 */
 1246 | 	protected void destroyBeans() {
 1247 | 		getBeanFactory().destroySingletons();
 1248 | 	}
 1249 | 
 1250 | 	/**
 1251 | 	 * Template method which can be overridden to add context-specific shutdown work.
 1252 | 	 * The default implementation is empty.
 1253 | 	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
 1254 | 	 * this context's BeanFactory has been closed. If custom shutdown logic
 1255 | 	 * needs to execute while the BeanFactory is still active, override
 1256 | 	 * the {@link #destroyBeans()} method instead.
 1257 | 	 */
 1258 | 	protected void onClose() {
 1259 | 		// For subclasses: do nothing by default.
 1260 | 	}
 1261 | 
 1262 | 	@Override
 1263 | 	public boolean isClosed() {
 1264 | 		return this.closed.get();
 1265 | 	}
 1266 | 
 1267 | 	@Override
 1268 | 	public boolean isActive() {
 1269 | 		return this.active.get();
 1270 | 	}
 1271 | 
 1272 | 	/**
 1273 | 	 * Assert that this context's BeanFactory is currently active,
 1274 | 	 * throwing an {@link IllegalStateException} if it isn't.
 1275 | 	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
 1276 | 	 * on an active context, i.e. in particular all bean accessor methods.
 1277 | 	 * <p>The default implementation checks the {@link #isActive() 'active'} status
 1278 | 	 * of this context overall. May be overridden for more specific checks, or for a
 1279 | 	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
 1280 | 	 */
 1281 | 	protected void assertBeanFactoryActive() {
 1282 | 		if (!this.active.get()) {
 1283 | 			if (this.closed.get()) {
 1284 | 				throw new IllegalStateException(getDisplayName() + " has been closed already");
 1285 | 			}
 1286 | 			else {
 1287 | 				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
 1288 | 			}
 1289 | 		}
 1290 | 	}
 1291 | 
 1292 | 
 1293 | 	//---------------------------------------------------------------------
 1294 | 	// Implementation of BeanFactory interface
 1295 | 	//---------------------------------------------------------------------
 1296 | 
 1297 | 	@Override
 1298 | 	public Object getBean(String name) throws BeansException {
 1299 | 		assertBeanFactoryActive();
 1300 | 		return getBeanFactory().getBean(name);
 1301 | 	}
 1302 | 
 1303 | 	@Override
 1304 | 	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
 1305 | 		assertBeanFactoryActive();
 1306 | 		return getBeanFactory().getBean(name, requiredType);
 1307 | 	}
 1308 | 
 1309 | 	@Override
 1310 | 	public <T> T getBean(String name, ParameterizedTypeReference<T> typeReference) throws BeansException {
 1311 | 		assertBeanFactoryActive();
 1312 | 		return getBeanFactory().getBean(name, typeReference);
 1313 | 	}
 1314 | 
 1315 | 	@Override
 1316 | 	public Object getBean(String name, @Nullable Object @Nullable ... args) throws BeansException {
 1317 | 		assertBeanFactoryActive();
 1318 | 		return getBeanFactory().getBean(name, args);
 1319 | 	}
 1320 | 
 1321 | 	@Override
 1322 | 	public <T> T getBean(Class<T> requiredType) throws BeansException {
 1323 | 		assertBeanFactoryActive();
 1324 | 		return getBeanFactory().getBean(requiredType);
 1325 | 	}
 1326 | 
 1327 | 	@Override
 1328 | 	public <T> T getBean(Class<T> requiredType, @Nullable Object @Nullable ... args) throws BeansException {
 1329 | 		assertBeanFactoryActive();
 1330 | 		return getBeanFactory().getBean(requiredType, args);
 1331 | 	}
 1332 | 
 1333 | 	@Override
 1334 | 	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
 1335 | 		assertBeanFactoryActive();
 1336 | 		return getBeanFactory().getBeanProvider(requiredType);
 1337 | 	}
 1338 | 
 1339 | 	@Override
 1340 | 	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
 1341 | 		assertBeanFactoryActive();
 1342 | 		return getBeanFactory().getBeanProvider(requiredType);
 1343 | 	}
 1344 | 
 1345 | 	@Override
 1346 | 	public <T> ObjectProvider<T> getBeanProvider(ParameterizedTypeReference<T> requiredType) {
 1347 | 		assertBeanFactoryActive();
 1348 | 		return getBeanFactory().getBeanProvider(requiredType);
 1349 | 	}
 1350 | 
 1351 | 	@Override
 1352 | 	public boolean containsBean(String name) {
 1353 | 		return getBeanFactory().containsBean(name);
 1354 | 	}
 1355 | 
 1356 | 	@Override
 1357 | 	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
 1358 | 		assertBeanFactoryActive();
 1359 | 		return getBeanFactory().isSingleton(name);
 1360 | 	}
 1361 | 
 1362 | 	@Override
 1363 | 	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
 1364 | 		assertBeanFactoryActive();
 1365 | 		return getBeanFactory().isPrototype(name);
 1366 | 	}
 1367 | 
 1368 | 	@Override
 1369 | 	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
 1370 | 		assertBeanFactoryActive();
 1371 | 		return getBeanFactory().isTypeMatch(name, typeToMatch);
 1372 | 	}
 1373 | 
 1374 | 	@Override
 1375 | 	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
 1376 | 		assertBeanFactoryActive();
 1377 | 		return getBeanFactory().isTypeMatch(name, typeToMatch);
 1378 | 	}
 1379 | 
 1380 | 	@Override
 1381 | 	public @Nullable Class<?> getType(String name) throws NoSuchBeanDefinitionException {
 1382 | 		assertBeanFactoryActive();
 1383 | 		return getBeanFactory().getType(name);
 1384 | 	}
 1385 | 
 1386 | 	@Override
 1387 | 	public @Nullable Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
 1388 | 		assertBeanFactoryActive();
 1389 | 		return getBeanFactory().getType(name, allowFactoryBeanInit);
 1390 | 	}
 1391 | 
 1392 | 	@Override
 1393 | 	public String[] getAliases(String name) {
 1394 | 		return getBeanFactory().getAliases(name);
 1395 | 	}
 1396 | 
 1397 | 
 1398 | 	//---------------------------------------------------------------------
 1399 | 	// Implementation of ListableBeanFactory interface
 1400 | 	//---------------------------------------------------------------------
 1401 | 
 1402 | 	@Override
 1403 | 	public boolean containsBeanDefinition(String beanName) {
 1404 | 		return getBeanFactory().containsBeanDefinition(beanName);
 1405 | 	}
 1406 | 
 1407 | 	@Override
 1408 | 	public int getBeanDefinitionCount() {
 1409 | 		return getBeanFactory().getBeanDefinitionCount();
 1410 | 	}
 1411 | 
 1412 | 	@Override
 1413 | 	public String[] getBeanDefinitionNames() {
 1414 | 		return getBeanFactory().getBeanDefinitionNames();
 1415 | 	}
 1416 | 
 1417 | 	@Override
 1418 | 	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
 1419 | 		assertBeanFactoryActive();
 1420 | 		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
 1421 | 	}
 1422 | 
 1423 | 	@Override
 1424 | 	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
 1425 | 		assertBeanFactoryActive();
 1426 | 		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
 1427 | 	}
 1428 | 
 1429 | 	@Override
 1430 | 	public String[] getBeanNamesForType(ResolvableType type) {
 1431 | 		assertBeanFactoryActive();
 1432 | 		return getBeanFactory().getBeanNamesForType(type);
 1433 | 	}
 1434 | 
 1435 | 	@Override
 1436 | 	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
 1437 | 		assertBeanFactoryActive();
 1438 | 		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
 1439 | 	}
 1440 | 
 1441 | 	@Override
 1442 | 	public String[] getBeanNamesForType(@Nullable Class<?> type) {
 1443 | 		assertBeanFactoryActive();
 1444 | 		return getBeanFactory().getBeanNamesForType(type);
 1445 | 	}
 1446 | 
 1447 | 	@Override
 1448 | 	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
 1449 | 		assertBeanFactoryActive();
 1450 | 		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
 1451 | 	}
 1452 | 
 1453 | 	@Override
 1454 | 	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
 1455 | 		assertBeanFactoryActive();
 1456 | 		return getBeanFactory().getBeansOfType(type);
 1457 | 	}
 1458 | 
 1459 | 	@Override
 1460 | 	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
 1461 | 			throws BeansException {
 1462 | 
 1463 | 		assertBeanFactoryActive();
 1464 | 		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
 1465 | 	}
 1466 | 
 1467 | 	@Override
 1468 | 	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
 1469 | 		assertBeanFactoryActive();
 1470 | 		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
 1471 | 	}
 1472 | 
 1473 | 	@Override
 1474 | 	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
 1475 | 			throws BeansException {
 1476 | 
 1477 | 		assertBeanFactoryActive();
 1478 | 		return getBeanFactory().getBeansWithAnnotation(annotationType);
 1479 | 	}
 1480 | 
 1481 | 	@Override
 1482 | 	public <A extends Annotation> @Nullable A findAnnotationOnBean(String beanName, Class<A> annotationType)
 1483 | 			throws NoSuchBeanDefinitionException {
 1484 | 
 1485 | 		assertBeanFactoryActive();
 1486 | 		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
 1487 | 	}
 1488 | 
 1489 | 	@Override
 1490 | 	public <A extends Annotation> @Nullable A findAnnotationOnBean(
 1491 | 			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
 1492 | 			throws NoSuchBeanDefinitionException {
 1493 | 
 1494 | 		assertBeanFactoryActive();
 1495 | 		return getBeanFactory().findAnnotationOnBean(beanName, annotationType, allowFactoryBeanInit);
 1496 | 	}
 1497 | 
 1498 | 	@Override
 1499 | 	public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
 1500 | 			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
 1501 | 			throws NoSuchBeanDefinitionException {
 1502 | 
 1503 | 		assertBeanFactoryActive();
 1504 | 		return getBeanFactory().findAllAnnotationsOnBean(beanName, annotationType, allowFactoryBeanInit);
 1505 | 	}
 1506 | 
 1507 | 
 1508 | 	//---------------------------------------------------------------------
 1509 | 	// Implementation of HierarchicalBeanFactory interface
 1510 | 	//---------------------------------------------------------------------
 1511 | 
 1512 | 	@Override
 1513 | 	public @Nullable BeanFactory getParentBeanFactory() {
 1514 | 		return getParent();
 1515 | 	}
 1516 | 
 1517 | 	@Override
 1518 | 	public boolean containsLocalBean(String name) {
 1519 | 		return getBeanFactory().containsLocalBean(name);
 1520 | 	}
 1521 | 
 1522 | 	/**
 1523 | 	 * Return the internal bean factory of the parent context if it implements
 1524 | 	 * ConfigurableApplicationContext; else, return the parent context itself.
 1525 | 	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
 1526 | 	 */
 1527 | 	protected @Nullable BeanFactory getInternalParentBeanFactory() {
 1528 | 		return (getParent() instanceof ConfigurableApplicationContext cac ?
 1529 | 				cac.getBeanFactory() : getParent());
 1530 | 	}
 1531 | 
 1532 | 
 1533 | 	//---------------------------------------------------------------------
 1534 | 	// Implementation of MessageSource interface
 1535 | 	//---------------------------------------------------------------------
 1536 | 
 1537 | 	@Override
 1538 | 	public @Nullable String getMessage(String code, Object @Nullable [] args, @Nullable String defaultMessage, @Nullable Locale locale) {
 1539 | 		return getMessageSource().getMessage(code, args, defaultMessage, locale);
 1540 | 	}
 1541 | 
 1542 | 	@Override
 1543 | 	public String getMessage(String code, Object @Nullable [] args, @Nullable Locale locale) throws NoSuchMessageException {
 1544 | 		return getMessageSource().getMessage(code, args, locale);
 1545 | 	}
 1546 | 
 1547 | 	@Override
 1548 | 	public String getMessage(MessageSourceResolvable resolvable, @Nullable Locale locale) throws NoSuchMessageException {
 1549 | 		return getMessageSource().getMessage(resolvable, locale);
 1550 | 	}
 1551 | 
 1552 | 	/**
 1553 | 	 * Return the internal MessageSource used by the context.
 1554 | 	 * @return the internal MessageSource (never {@code null})
 1555 | 	 * @throws IllegalStateException if the context has not been initialized yet
 1556 | 	 */
 1557 | 	private MessageSource getMessageSource() throws IllegalStateException {
 1558 | 		if (this.messageSource == null) {
 1559 | 			throw new IllegalStateException("MessageSource not initialized - " +
 1560 | 					"call 'refresh' before accessing messages via the context: " + this);
 1561 | 		}
 1562 | 		return this.messageSource;
 1563 | 	}
 1564 | 
 1565 | 	/**
 1566 | 	 * Return the internal message source of the parent context if it is an
 1567 | 	 * AbstractApplicationContext too; else, return the parent context itself.
 1568 | 	 */
 1569 | 	protected @Nullable MessageSource getInternalParentMessageSource() {
 1570 | 		return (getParent() instanceof AbstractApplicationContext abstractApplicationContext ?
 1571 | 				abstractApplicationContext.messageSource : getParent());
 1572 | 	}
 1573 | 
 1574 | 
 1575 | 	//---------------------------------------------------------------------
 1576 | 	// Implementation of ResourcePatternResolver interface
 1577 | 	//---------------------------------------------------------------------
 1578 | 
 1579 | 	@Override
 1580 | 	public Resource[] getResources(String locationPattern) throws IOException {
 1581 | 		return this.resourcePatternResolver.getResources(locationPattern);
 1582 | 	}
 1583 | 
 1584 | 
 1585 | 	//---------------------------------------------------------------------
 1586 | 	// Implementation of Lifecycle interface
 1587 | 	//---------------------------------------------------------------------
 1588 | 
 1589 | 	@Override
 1590 | 	public void start() {
 1591 | 		getLifecycleProcessor().start();
 1592 | 		publishEvent(new ContextStartedEvent(this));
 1593 | 	}
 1594 | 
 1595 | 	@Override
 1596 | 	public void stop() {
 1597 | 		getLifecycleProcessor().stop();
 1598 | 		publishEvent(new ContextStoppedEvent(this));
 1599 | 	}
 1600 | 
 1601 | 	@Override
 1602 | 	public void restart() {
 1603 | 		getLifecycleProcessor().onRestart();
 1604 | 		publishEvent(new ContextRestartedEvent(this));
 1605 | 	}
 1606 | 
 1607 | 	@Override
 1608 | 	public void pause() {
 1609 | 		getLifecycleProcessor().onPause();
 1610 | 		publishEvent(new ContextPausedEvent(this));
 1611 | 	}
 1612 | 
 1613 | 	@Override
 1614 | 	public boolean isRunning() {
 1615 | 		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
 1616 | 	}
 1617 | 
 1618 | 
 1619 | 	//---------------------------------------------------------------------
 1620 | 	// Abstract methods that must be implemented by subclasses
 1621 | 	//---------------------------------------------------------------------
 1622 | 
 1623 | 	/**
 1624 | 	 * Subclasses must implement this method to perform the actual configuration load.
 1625 | 	 * The method is invoked by {@link #refresh()} before any other initialization work.
 1626 | 	 * <p>A subclass will either create a new bean factory and hold a reference to it,
 1627 | 	 * or return a single BeanFactory instance that it holds. In the latter case, it will
 1628 | 	 * usually throw an IllegalStateException if refreshing the context more than once.
 1629 | 	 * @throws BeansException if initialization of the bean factory failed
 1630 | 	 * @throws IllegalStateException if already initialized and multiple refresh
 1631 | 	 * attempts are not supported
 1632 | 	 */
 1633 | 	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;
 1634 | 
 1635 | 	/**
 1636 | 	 * Subclasses must implement this method to release their internal bean factory.
 1637 | 	 * This method gets invoked by {@link #close()} after all other shutdown work.
 1638 | 	 * <p>Should never throw an exception but rather log shutdown failures.
 1639 | 	 */
 1640 | 	protected abstract void closeBeanFactory();
 1641 | 
 1642 | 	/**
 1643 | 	 * Subclasses must return their internal bean factory here. They should implement the
 1644 | 	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
 1645 | 	 * <p>Note: Subclasses should check whether the context is still active before
 1646 | 	 * returning the internal bean factory. The internal factory should generally be
 1647 | 	 * considered unavailable once the context has been closed.
 1648 | 	 * @return this application context's internal bean factory (never {@code null})
 1649 | 	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
 1650 | 	 * (usually if {@link #refresh()} has never been called) or if the context has been
 1651 | 	 * closed already
 1652 | 	 * @see #refreshBeanFactory()
 1653 | 	 * @see #closeBeanFactory()
 1654 | 	 */
 1655 | 	@Override
 1656 | 	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;
 1657 | 
 1658 | 
 1659 | 	/**
 1660 | 	 * Return information about this context.
 1661 | 	 */
 1662 | 	@Override
 1663 | 	public String toString() {
 1664 | 		StringBuilder sb = new StringBuilder(getDisplayName());
 1665 | 		sb.append(", started on ").append(new Date(getStartupDate()));
 1666 | 		ApplicationContext parent = getParent();
 1667 | 		if (parent != null) {
 1668 | 			sb.append(", parent: ").append(parent.getDisplayName());
 1669 | 		}
 1670 | 		return sb.toString();
 1671 | 	}
 1672 | 
 1673 | }
```
