<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/support/GenericApplicationContext.java -->
<!-- 正文引用：[[源码:GenericApplicationContext.java#L起-L止]] ；代码块头部：(src: GenericApplicationContext.java#L起-L止) -->

# GenericApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/support/GenericApplicationContext.java` |
| 行数 | 690 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:02.084Z |

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
   20 | import java.lang.reflect.Constructor;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.concurrent.atomic.AtomicBoolean;
   24 | import java.util.function.Supplier;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.aot.hint.RuntimeHints;
   29 | import org.springframework.aot.hint.support.ClassHintUtils;
   30 | import org.springframework.beans.BeanUtils;
   31 | import org.springframework.beans.BeansException;
   32 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   33 | import org.springframework.beans.factory.BeanRegistrar;
   34 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   35 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   36 | import org.springframework.beans.factory.config.BeanDefinition;
   37 | import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
   38 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   39 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   40 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   41 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   42 | import org.springframework.beans.factory.support.BeanRegistryAdapter;
   43 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   44 | import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
   45 | import org.springframework.beans.factory.support.RootBeanDefinition;
   46 | import org.springframework.context.ApplicationContext;
   47 | import org.springframework.core.Ordered;
   48 | import org.springframework.core.PriorityOrdered;
   49 | import org.springframework.core.io.ProtocolResolver;
   50 | import org.springframework.core.io.Resource;
   51 | import org.springframework.core.io.ResourceLoader;
   52 | import org.springframework.core.io.support.ResourcePatternResolver;
   53 | import org.springframework.core.metrics.ApplicationStartup;
   54 | import org.springframework.util.Assert;
   55 | 
   56 | /**
   57 |  * Generic ApplicationContext implementation that holds a single internal
   58 |  * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
   59 |  * instance and does not assume a specific bean definition format. Implements
   60 |  * the {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
   61 |  * interface in order to allow for applying any bean definition readers to it.
   62 |  *
   63 |  * <p>Typical usage is to register a variety of bean definitions via the
   64 |  * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
   65 |  * interface and then call {@link #refresh()} to initialize those beans
   66 |  * with application context semantics (handling
   67 |  * {@link org.springframework.context.ApplicationContextAware}, auto-detecting
   68 |  * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
   69 |  * etc).
   70 |  *
   71 |  * <p>In contrast to other ApplicationContext implementations that create a new
   72 |  * internal BeanFactory instance for each refresh, the internal BeanFactory of
   73 |  * this context is available right from the start, to be able to register bean
   74 |  * definitions on it. {@link #refresh()} may only be called once.
   75 |  *
   76 |  * <p>This ApplicationContext implementation is suitable for Ahead of Time
   77 |  * processing, using {@link #refreshForAotProcessing} as an alternative to the
   78 |  * regular {@link #refresh()}.
   79 |  *
   80 |  * <p>Usage example:
   81 |  *
   82 |  * <pre class="code">
   83 |  * GenericApplicationContext ctx = new GenericApplicationContext();
   84 |  * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
   85 |  * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
   86 |  * ctx.refresh();
   87 |  *
   88 |  * MyBean myBean = (MyBean) ctx.getBean("myBean");
   89 |  * ...</pre>
   90 |  *
   91 |  * For the typical case of XML bean definitions, you may also use
   92 |  * {@link ClassPathXmlApplicationContext} or {@link FileSystemXmlApplicationContext},
   93 |  * which are easier to set up - but less flexible, since you can just use standard
   94 |  * resource locations for XML bean definitions, rather than mixing arbitrary bean
   95 |  * definition formats. For a custom application context implementation supposed to
   96 |  * read a specific bean definition format in a refreshable manner, consider
   97 |  * deriving from the {@link AbstractRefreshableApplicationContext} base class.
   98 |  *
   99 |  * @author Juergen Hoeller
  100 |  * @author Chris Beams
  101 |  * @author Stephane Nicoll
  102 |  * @author Sam Brannen
  103 |  * @since 1.1.2
  104 |  * @see #registerBeanDefinition
  105 |  * @see #refresh()
  106 |  * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
  107 |  */
  108 | public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {
  109 | 
  110 | 	private static final String DEFERRED_REGISTRY_POST_PROCESSOR_BEAN_NAME =
  111 | 			GenericApplicationContext.class.getName() + ".deferredRegistryPostProcessor";
  112 | 
  113 | 
  114 | 	private final DefaultListableBeanFactory beanFactory;
  115 | 
  116 | 	private @Nullable ResourceLoader resourceLoader;
  117 | 
  118 | 	private boolean customClassLoader = false;
  119 | 
  120 | 	private final AtomicBoolean refreshed = new AtomicBoolean();
  121 | 
  122 | 
  123 | 	/**
  124 | 	 * Create a new GenericApplicationContext.
  125 | 	 * @see #registerBeanDefinition
  126 | 	 * @see #refresh
  127 | 	 */
  128 | 	public GenericApplicationContext() {
  129 | 		this.beanFactory = new DefaultListableBeanFactory();
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Create a new GenericApplicationContext with the given DefaultListableBeanFactory.
  134 | 	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
  135 | 	 * @see #registerBeanDefinition
  136 | 	 * @see #refresh
  137 | 	 */
  138 | 	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
  139 | 		Assert.notNull(beanFactory, "BeanFactory must not be null");
  140 | 		this.beanFactory = beanFactory;
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Create a new GenericApplicationContext with the given parent.
  145 | 	 * @param parent the parent application context
  146 | 	 * @see #registerBeanDefinition
  147 | 	 * @see #refresh
  148 | 	 */
  149 | 	public GenericApplicationContext(@Nullable ApplicationContext parent) {
  150 | 		this();
  151 | 		setParent(parent);
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Create a new GenericApplicationContext with the given DefaultListableBeanFactory.
  156 | 	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
  157 | 	 * @param parent the parent application context
  158 | 	 * @see #registerBeanDefinition
  159 | 	 * @see #refresh
  160 | 	 */
  161 | 	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
  162 | 		this(beanFactory);
  163 | 		setParent(parent);
  164 | 	}
  165 | 
  166 | 
  167 | 	/**
  168 | 	 * Set the parent of this application context, also setting
  169 | 	 * the parent of the internal BeanFactory accordingly.
  170 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
  171 | 	 */
  172 | 	@Override
  173 | 	public void setParent(@Nullable ApplicationContext parent) {
  174 | 		super.setParent(parent);
  175 | 		this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
  176 | 	}
  177 | 
  178 | 	@Override
  179 | 	public void setApplicationStartup(ApplicationStartup applicationStartup) {
  180 | 		super.setApplicationStartup(applicationStartup);
  181 | 		this.beanFactory.setApplicationStartup(applicationStartup);
  182 | 	}
  183 | 
  184 | 	/**
  185 | 	 * Set whether it should be allowed to override bean definitions by registering
  186 | 	 * a different definition with the same name, automatically replacing the former.
  187 | 	 * If not, an exception will be thrown. Default is "true".
  188 | 	 * @since 3.0
  189 | 	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
  190 | 	 */
  191 | 	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
  192 | 		this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Set whether to allow circular references between beans - and automatically
  197 | 	 * try to resolve them.
  198 | 	 * <p>Default is "true". Turn this off to throw an exception when encountering
  199 | 	 * a circular reference, disallowing them completely.
  200 | 	 * @since 3.0
  201 | 	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
  202 | 	 */
  203 | 	public void setAllowCircularReferences(boolean allowCircularReferences) {
  204 | 		this.beanFactory.setAllowCircularReferences(allowCircularReferences);
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Set a ResourceLoader to use for this context. If set, the context will
  209 | 	 * delegate all {@code getResource} calls to the given ResourceLoader.
  210 | 	 * If not set, default resource loading will apply.
  211 | 	 * <p>The main reason to specify a custom ResourceLoader is to resolve
  212 | 	 * resource paths (without URL prefix) in a specific fashion.
  213 | 	 * The default behavior is to resolve such paths as class path locations.
  214 | 	 * To resolve resource paths as file system locations, specify a
  215 | 	 * FileSystemResourceLoader here.
  216 | 	 * <p>You can also pass in a full ResourcePatternResolver, which will
  217 | 	 * be autodetected by the context and used for {@code getResources}
  218 | 	 * calls as well. Else, default resource pattern matching will apply.
  219 | 	 * @see #getResource
  220 | 	 * @see org.springframework.core.io.DefaultResourceLoader
  221 | 	 * @see org.springframework.core.io.FileSystemResourceLoader
  222 | 	 * @see org.springframework.core.io.support.ResourcePatternResolver
  223 | 	 * @see #getResources
  224 | 	 */
  225 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  226 | 		this.resourceLoader = resourceLoader;
  227 | 	}
  228 | 
  229 | 
  230 | 	//---------------------------------------------------------------------
  231 | 	// ResourceLoader / ResourcePatternResolver override if necessary
  232 | 	//---------------------------------------------------------------------
  233 | 
  234 | 	/**
  235 | 	 * This implementation delegates to this context's {@code ResourceLoader} if set,
  236 | 	 * falling back to the default superclass behavior otherwise.
  237 | 	 * <p>As of Spring Framework 5.3.22, this method also honors registered
  238 | 	 * {@linkplain #getProtocolResolvers() protocol resolvers} when a custom
  239 | 	 * {@code ResourceLoader} has been set.
  240 | 	 * @see #setResourceLoader(ResourceLoader)
  241 | 	 * @see #addProtocolResolver(ProtocolResolver)
  242 | 	 */
  243 | 	@Override
  244 | 	public Resource getResource(String location) {
  245 | 		if (this.resourceLoader != null) {
  246 | 			for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
  247 | 				Resource resource = protocolResolver.resolve(location, this);
  248 | 				if (resource != null) {
  249 | 					return resource;
  250 | 				}
  251 | 			}
  252 | 			return this.resourceLoader.getResource(location);
  253 | 		}
  254 | 		return super.getResource(location);
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * This implementation delegates to this context's ResourceLoader if it
  259 | 	 * implements the ResourcePatternResolver interface, falling back to the
  260 | 	 * default superclass behavior otherwise.
  261 | 	 * @see #setResourceLoader
  262 | 	 */
  263 | 	@Override
  264 | 	public Resource[] getResources(String locationPattern) throws IOException {
  265 | 		if (this.resourceLoader instanceof ResourcePatternResolver resourcePatternResolver) {
  266 | 			return resourcePatternResolver.getResources(locationPattern);
  267 | 		}
  268 | 		return super.getResources(locationPattern);
  269 | 	}
  270 | 
  271 | 	@Override
  272 | 	public void setClassLoader(@Nullable ClassLoader classLoader) {
  273 | 		super.setClassLoader(classLoader);
  274 | 		this.customClassLoader = true;
  275 | 	}
  276 | 
  277 | 	@Override
  278 | 	public @Nullable ClassLoader getClassLoader() {
  279 | 		if (this.resourceLoader != null && !this.customClassLoader) {
  280 | 			return this.resourceLoader.getClassLoader();
  281 | 		}
  282 | 		return super.getClassLoader();
  283 | 	}
  284 | 
  285 | 
  286 | 	//---------------------------------------------------------------------
  287 | 	// Implementations of AbstractApplicationContext's template methods
  288 | 	//---------------------------------------------------------------------
  289 | 
  290 | 	/**
  291 | 	 * Do nothing: We hold a single internal BeanFactory and rely on callers
  292 | 	 * to register beans through our public methods (or the BeanFactory's).
  293 | 	 * @see #registerBeanDefinition
  294 | 	 */
  295 | 	@Override
  296 | 	protected final void refreshBeanFactory() throws IllegalStateException {
  297 | 		if (!this.refreshed.compareAndSet(false, true)) {
  298 | 			throw new IllegalStateException(
  299 | 					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
  300 | 		}
  301 | 		this.beanFactory.setSerializationId(getId());
  302 | 	}
  303 | 
  304 | 	@Override
  305 | 	protected void cancelRefresh(Throwable ex) {
  306 | 		this.beanFactory.setSerializationId(null);
  307 | 		super.cancelRefresh(ex);
  308 | 	}
  309 | 
  310 | 	/**
  311 | 	 * Not much to do: We hold a single internal BeanFactory that will never
  312 | 	 * get released.
  313 | 	 */
  314 | 	@Override
  315 | 	protected final void closeBeanFactory() {
  316 | 		this.beanFactory.setSerializationId(null);
  317 | 	}
  318 | 
  319 | 	/**
  320 | 	 * Return the single internal BeanFactory held by this context
  321 | 	 * (as ConfigurableListableBeanFactory).
  322 | 	 */
  323 | 	@Override
  324 | 	public final ConfigurableListableBeanFactory getBeanFactory() {
  325 | 		return this.beanFactory;
  326 | 	}
  327 | 
  328 | 	/**
  329 | 	 * Return the underlying bean factory of this context,
  330 | 	 * available for registering bean definitions.
  331 | 	 * <p><b>NOTE:</b> You need to call {@link #refresh()} to initialize the
  332 | 	 * bean factory and its contained beans with application context semantics
  333 | 	 * (autodetecting BeanFactoryPostProcessors, etc).
  334 | 	 * @return the internal bean factory (as DefaultListableBeanFactory)
  335 | 	 */
  336 | 	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
  337 | 		return this.beanFactory;
  338 | 	}
  339 | 
  340 | 	@Override
  341 | 	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
  342 | 		assertBeanFactoryActive();
  343 | 		return this.beanFactory;
  344 | 	}
  345 | 
  346 | 
  347 | 	//---------------------------------------------------------------------
  348 | 	// Implementation of BeanDefinitionRegistry
  349 | 	//---------------------------------------------------------------------
  350 | 
  351 | 	@Override
  352 | 	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
  353 | 			throws BeanDefinitionStoreException {
  354 | 
  355 | 		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
  356 | 	}
  357 | 
  358 | 	@Override
  359 | 	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
  360 | 		this.beanFactory.removeBeanDefinition(beanName);
  361 | 	}
  362 | 
  363 | 	@Override
  364 | 	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
  365 | 		return this.beanFactory.getBeanDefinition(beanName);
  366 | 	}
  367 | 
  368 | 	@Override
  369 | 	public boolean isBeanDefinitionOverridable(String beanName) {
  370 | 		return this.beanFactory.isBeanDefinitionOverridable(beanName);
  371 | 	}
  372 | 
  373 | 	@Override
  374 | 	public boolean isBeanNameInUse(String beanName) {
  375 | 		return this.beanFactory.isBeanNameInUse(beanName);
  376 | 	}
  377 | 
  378 | 	@Override
  379 | 	public void registerAlias(String beanName, String alias) {
  380 | 		this.beanFactory.registerAlias(beanName, alias);
  381 | 	}
  382 | 
  383 | 	@Override
  384 | 	public void removeAlias(String alias) {
  385 | 		this.beanFactory.removeAlias(alias);
  386 | 	}
  387 | 
  388 | 	@Override
  389 | 	public boolean isAlias(String beanName) {
  390 | 		return this.beanFactory.isAlias(beanName);
  391 | 	}
  392 | 
  393 | 
  394 | 	//---------------------------------------------------------------------
  395 | 	// AOT processing
  396 | 	//---------------------------------------------------------------------
  397 | 
  398 | 	/**
  399 | 	 * Load or refresh the persistent representation of the configuration up to
  400 | 	 * a point where the underlying bean factory is ready to create bean
  401 | 	 * instances.
  402 | 	 * <p>This variant of {@link #refresh()} is used by Ahead of Time (AOT)
  403 | 	 * processing that optimizes the application context, typically at build time.
  404 | 	 * <p>In this mode, only {@link BeanDefinitionRegistryPostProcessor} and
  405 | 	 * {@link MergedBeanDefinitionPostProcessor} are invoked.
  406 | 	 * @param runtimeHints the runtime hints
  407 | 	 * @throws BeansException if the bean factory could not be initialized
  408 | 	 * @throws IllegalStateException if already initialized and multiple refresh
  409 | 	 * attempts are not supported
  410 | 	 * @since 6.0
  411 | 	 */
  412 | 	public void refreshForAotProcessing(RuntimeHints runtimeHints) {
  413 | 		if (logger.isDebugEnabled()) {
  414 | 			logger.debug("Preparing bean factory for AOT processing");
  415 | 		}
  416 | 		prepareRefresh();
  417 | 		obtainFreshBeanFactory();
  418 | 		prepareBeanFactory(this.beanFactory);
  419 | 		postProcessBeanFactory(this.beanFactory);
  420 | 		invokeBeanFactoryPostProcessors(this.beanFactory);
  421 | 		this.beanFactory.freezeConfiguration();
  422 | 		PostProcessorRegistrationDelegate.invokeMergedBeanDefinitionPostProcessors(this.beanFactory);
  423 | 		preDetermineBeanTypes(runtimeHints);
  424 | 	}
  425 | 
  426 | 	/**
  427 | 	 * Pre-determine bean types in order to trigger early proxy class creation.
  428 | 	 * @see org.springframework.beans.factory.BeanFactory#getType
  429 | 	 * @see SmartInstantiationAwareBeanPostProcessor#determineBeanType
  430 | 	 */
  431 | 	private void preDetermineBeanTypes(RuntimeHints runtimeHints) {
  432 | 		List<String> singletons = new ArrayList<>();
  433 | 		List<String> lazyBeans = new ArrayList<>();
  434 | 
  435 | 		// First round: pre-registered singleton instances, if any.
  436 | 		for (String beanName : this.beanFactory.getSingletonNames()) {
  437 | 			Class<?> beanType = this.beanFactory.getType(beanName);
  438 | 			if (beanType != null) {
  439 | 				ClassHintUtils.registerProxyIfNecessary(beanType, runtimeHints);
  440 | 			}
  441 | 			singletons.add(beanName);
  442 | 		}
  443 | 
  444 | 		List<SmartInstantiationAwareBeanPostProcessor> bpps =
  445 | 				PostProcessorRegistrationDelegate.loadBeanPostProcessors(
  446 | 						this.beanFactory, SmartInstantiationAwareBeanPostProcessor.class);
  447 | 
  448 | 		// Second round: non-lazy singleton beans in definition order,
  449 | 		// matching preInstantiateSingletons.
  450 | 		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
  451 | 			if (!singletons.contains(beanName)) {
  452 | 				BeanDefinition bd = getBeanDefinition(beanName);
  453 | 				if (bd.isSingleton() && !bd.isLazyInit()) {
  454 | 					preDetermineBeanType(beanName, bpps, runtimeHints);
  455 | 				}
  456 | 				else {
  457 | 					lazyBeans.add(beanName);
  458 | 				}
  459 | 			}
  460 | 		}
  461 | 
  462 | 		// Third round: lazy singleton beans and scoped beans.
  463 | 		for (String beanName : lazyBeans) {
  464 | 			preDetermineBeanType(beanName, bpps, runtimeHints);
  465 | 		}
  466 | 	}
  467 | 
  468 | 	private void preDetermineBeanType(String beanName, List<SmartInstantiationAwareBeanPostProcessor> bpps,
  469 | 			RuntimeHints runtimeHints) {
  470 | 
  471 | 		Class<?> beanType = this.beanFactory.getType(beanName);
  472 | 		if (beanType != null) {
  473 | 			ClassHintUtils.registerProxyIfNecessary(beanType, runtimeHints);
  474 | 			for (SmartInstantiationAwareBeanPostProcessor bpp : bpps) {
  475 | 				Class<?> newBeanType = bpp.determineBeanType(beanType, beanName);
  476 | 				if (newBeanType != beanType) {
  477 | 					ClassHintUtils.registerProxyIfNecessary(newBeanType, runtimeHints);
  478 | 					beanType = newBeanType;
  479 | 				}
  480 | 			}
  481 | 		}
  482 | 	}
  483 | 
  484 | 
  485 | 	//---------------------------------------------------------------------
  486 | 	// Convenient methods for registering individual beans
  487 | 	//---------------------------------------------------------------------
  488 | 
  489 | 	/**
  490 | 	 * Register a bean from the given bean class, optionally providing explicit
  491 | 	 * constructor arguments for consideration in the autowiring process.
  492 | 	 * @param beanClass the class of the bean
  493 | 	 * @param constructorArgs custom argument values to be fed into Spring's
  494 | 	 * constructor resolution algorithm, resolving either all arguments or just
  495 | 	 * specific ones, with the rest to be resolved through regular autowiring
  496 | 	 * (may be {@code null} or empty)
  497 | 	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
  498 | 	 */
  499 | 	public <T> void registerBean(Class<T> beanClass, @Nullable Object... constructorArgs) {
  500 | 		registerBean(null, beanClass, constructorArgs);
  501 | 	}
  502 | 
  503 | 	/**
  504 | 	 * Register a bean from the given bean class, optionally providing explicit
  505 | 	 * constructor arguments for consideration in the autowiring process.
  506 | 	 * @param beanName the name of the bean (may be {@code null})
  507 | 	 * @param beanClass the class of the bean
  508 | 	 * @param constructorArgs custom argument values to be fed into Spring's
  509 | 	 * constructor resolution algorithm, resolving either all arguments or just
  510 | 	 * specific ones, with the rest to be resolved through regular autowiring
  511 | 	 * (may be {@code null} or empty)
  512 | 	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
  513 | 	 */
  514 | 	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, @Nullable Object... constructorArgs) {
  515 | 		registerBean(beanName, beanClass, (Supplier<T>) null,
  516 | 				bd -> {
  517 | 					for (Object arg : constructorArgs) {
  518 | 						bd.getConstructorArgumentValues().addGenericArgumentValue(arg);
  519 | 					}
  520 | 				});
  521 | 	}
  522 | 
  523 | 	/**
  524 | 	 * Register a bean from the given bean class, optionally customizing its
  525 | 	 * bean definition metadata (typically declared as a lambda expression).
  526 | 	 * @param beanClass the class of the bean (resolving a public constructor
  527 | 	 * to be autowired, possibly simply the default constructor)
  528 | 	 * @param customizers one or more callbacks for customizing the factory's
  529 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  530 | 	 * @since 5.0
  531 | 	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
  532 | 	 */
  533 | 	public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
  534 | 		registerBean(null, beanClass, null, customizers);
  535 | 	}
  536 | 
  537 | 	/**
  538 | 	 * Register a bean from the given bean class, optionally customizing its
  539 | 	 * bean definition metadata (typically declared as a lambda expression).
  540 | 	 * @param beanName the name of the bean (may be {@code null})
  541 | 	 * @param beanClass the class of the bean (resolving a public constructor
  542 | 	 * to be autowired, possibly simply the default constructor)
  543 | 	 * @param customizers one or more callbacks for customizing the factory's
  544 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  545 | 	 * @since 5.0
  546 | 	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
  547 | 	 */
  548 | 	public final <T> void registerBean(
  549 | 			@Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
  550 | 
  551 | 		registerBean(beanName, beanClass, null, customizers);
  552 | 	}
  553 | 
  554 | 	/**
  555 | 	 * Register a bean from the given bean class, using the given supplier for
  556 | 	 * obtaining a new instance (typically declared as a lambda expression or
  557 | 	 * method reference), optionally customizing its bean definition metadata
  558 | 	 * (again typically declared as a lambda expression).
  559 | 	 * @param beanClass the class of the bean
  560 | 	 * @param supplier a callback for creating an instance of the bean
  561 | 	 * @param customizers one or more callbacks for customizing the factory's
  562 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  563 | 	 * @since 5.0
  564 | 	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
  565 | 	 */
  566 | 	public final <T> void registerBean(
  567 | 			Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
  568 | 
  569 | 		registerBean(null, beanClass, supplier, customizers);
  570 | 	}
  571 | 
  572 | 	/**
  573 | 	 * Register a bean from the given bean class, using the given supplier for
  574 | 	 * obtaining a new instance (typically declared as a lambda expression or
  575 | 	 * method reference), optionally customizing its bean definition metadata
  576 | 	 * (again typically declared as a lambda expression).
  577 | 	 * <p>This method can be overridden to adapt the registration mechanism for
  578 | 	 * all {@code registerBean} methods (since they all delegate to this one).
  579 | 	 * @param beanName the name of the bean (may be {@code null})
  580 | 	 * @param beanClass the class of the bean
  581 | 	 * @param supplier a callback for creating an instance of the bean (in case
  582 | 	 * of {@code null}, resolving a public constructor to be autowired instead)
  583 | 	 * @param customizers one or more callbacks for customizing the factory's
  584 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  585 | 	 * @since 5.0
  586 | 	 */
  587 | 	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
  588 | 			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
  589 | 
  590 | 		ClassDerivedBeanDefinition beanDefinition = new ClassDerivedBeanDefinition(beanClass);
  591 | 		if (supplier != null) {
  592 | 			beanDefinition.setInstanceSupplier(supplier);
  593 | 		}
  594 | 		for (BeanDefinitionCustomizer customizer : customizers) {
  595 | 			customizer.customize(beanDefinition);
  596 | 		}
  597 | 
  598 | 		String nameToUse = (beanName != null ? beanName : beanClass.getName());
  599 | 		registerBeanDefinition(nameToUse, beanDefinition);
  600 | 	}
  601 | 
  602 | 	/**
  603 | 	 * Invoke the given registrars for registering their beans with this
  604 | 	 * application context.
  605 | 	 * <p>This can be used to apply encapsulated pieces of programmatic
  606 | 	 * bean registration to this application context without relying on
  607 | 	 * individual calls to its context-level {@code registerBean} methods.
  608 | 	 * @param registrars one or more {@link BeanRegistrar} instances
  609 | 	 * @since 7.0
  610 | 	 */
  611 | 	public void register(BeanRegistrar... registrars) {
  612 | 		for (BeanRegistrar registrar : registrars) {
  613 | 			DeferredRegistryPostProcessor pp = (DeferredRegistryPostProcessor)
  614 | 					this.beanFactory.getSingleton(DEFERRED_REGISTRY_POST_PROCESSOR_BEAN_NAME);
  615 | 			if (pp == null) {
  616 | 				pp = new DeferredRegistryPostProcessor();
  617 | 				this.beanFactory.registerSingleton(DEFERRED_REGISTRY_POST_PROCESSOR_BEAN_NAME, pp);
  618 | 			}
  619 | 			pp.addRegistrar(registrar);
  620 | 		}
  621 | 	}
  622 | 
  623 | 
  624 | 	/**
  625 | 	 * {@link RootBeanDefinition} subclass for {@code #registerBean} based
  626 | 	 * registrations with flexible autowiring for public constructors.
  627 | 	 */
  628 | 	@SuppressWarnings("serial")
  629 | 	private static class ClassDerivedBeanDefinition extends RootBeanDefinition {
  630 | 
  631 | 		public ClassDerivedBeanDefinition(Class<?> beanClass) {
  632 | 			super(beanClass);
  633 | 		}
  634 | 
  635 | 		public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
  636 | 			super(original);
  637 | 		}
  638 | 
  639 | 		@Override
  640 | 		public Constructor<?> @Nullable [] getPreferredConstructors() {
  641 | 			Constructor<?>[] fromAttribute = super.getPreferredConstructors();
  642 | 			if (fromAttribute != null) {
  643 | 				return fromAttribute;
  644 | 			}
  645 | 			Class<?> clazz = getBeanClass();
  646 | 			Constructor<?> primaryCtor = BeanUtils.findPrimaryConstructor(clazz);
  647 | 			if (primaryCtor != null) {
  648 | 				return new Constructor<?>[] {primaryCtor};
  649 | 			}
  650 | 			Constructor<?>[] publicCtors = clazz.getConstructors();
  651 | 			if (publicCtors.length > 0) {
  652 | 				return publicCtors;
  653 | 			}
  654 | 			return null;
  655 | 		}
  656 | 
  657 | 		@Override
  658 | 		public RootBeanDefinition cloneBeanDefinition() {
  659 | 			return new ClassDerivedBeanDefinition(this);
  660 | 		}
  661 | 	}
  662 | 
  663 | 
  664 | 	/**
  665 | 	 * Internal post-processor for invoking DeferredBeanRegistrars at the end
  666 | 	 * of the BeanDefinitionRegistryPostProcessor PriorityOrdered phase,
  667 | 	 * right before a potential ConfigurationClassPostProcessor.
  668 | 	 */
  669 | 	private class DeferredRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {
  670 | 
  671 | 		private final List<BeanRegistrar> registrars = new ArrayList<>();
  672 | 
  673 | 		public void addRegistrar(BeanRegistrar registrar) {
  674 | 			this.registrars.add(registrar);
  675 | 		}
  676 | 
  677 | 		@Override
  678 | 		public int getOrder() {
  679 | 			return Ordered.LOWEST_PRECEDENCE - 1;  // within PriorityOrdered, 1 before ConfigurationClassPostProcessor
  680 | 		}
  681 | 
  682 | 		@Override
  683 | 		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
  684 | 			for (BeanRegistrar registrar : this.registrars) {
  685 | 				new BeanRegistryAdapter(beanFactory, getEnvironment(), registrar.getClass()).register(registrar);
  686 | 			}
  687 | 		}
  688 | 	}
  689 | 
  690 | }
```
