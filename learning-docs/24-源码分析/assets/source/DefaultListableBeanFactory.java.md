<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/support/DefaultListableBeanFactory.java -->
<!-- 正文引用：[[源码:DefaultListableBeanFactory.java#L起-L止]] ；代码块头部：(src: DefaultListableBeanFactory.java#L起-L止) -->

# DefaultListableBeanFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/support/DefaultListableBeanFactory.java` |
| 行数 | 2812 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.265Z |

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
   17 | package org.springframework.beans.factory.support;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.NotSerializableException;
   21 | import java.io.ObjectInputStream;
   22 | import java.io.ObjectStreamException;
   23 | import java.io.Serial;
   24 | import java.io.Serializable;
   25 | import java.lang.annotation.Annotation;
   26 | import java.lang.ref.Reference;
   27 | import java.lang.ref.WeakReference;
   28 | import java.lang.reflect.Method;
   29 | import java.util.ArrayList;
   30 | import java.util.Arrays;
   31 | import java.util.Collection;
   32 | import java.util.Collections;
   33 | import java.util.Comparator;
   34 | import java.util.IdentityHashMap;
   35 | import java.util.Iterator;
   36 | import java.util.LinkedHashSet;
   37 | import java.util.List;
   38 | import java.util.Map;
   39 | import java.util.Optional;
   40 | import java.util.Set;
   41 | import java.util.concurrent.CompletableFuture;
   42 | import java.util.concurrent.CompletionException;
   43 | import java.util.concurrent.ConcurrentHashMap;
   44 | import java.util.concurrent.Executor;
   45 | import java.util.function.Consumer;
   46 | import java.util.function.Predicate;
   47 | import java.util.function.Supplier;
   48 | import java.util.stream.Stream;
   49 | 
   50 | import jakarta.inject.Provider;
   51 | import org.jspecify.annotations.Nullable;
   52 | 
   53 | import org.springframework.beans.BeansException;
   54 | import org.springframework.beans.TypeConverter;
   55 | import org.springframework.beans.factory.BeanCreationException;
   56 | import org.springframework.beans.factory.BeanCurrentlyInCreationException;
   57 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   58 | import org.springframework.beans.factory.BeanFactory;
   59 | import org.springframework.beans.factory.BeanFactoryAware;
   60 | import org.springframework.beans.factory.BeanFactoryUtils;
   61 | import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
   62 | import org.springframework.beans.factory.CannotLoadBeanClassException;
   63 | import org.springframework.beans.factory.FactoryBean;
   64 | import org.springframework.beans.factory.InjectionPoint;
   65 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   66 | import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
   67 | import org.springframework.beans.factory.ObjectFactory;
   68 | import org.springframework.beans.factory.ObjectProvider;
   69 | import org.springframework.beans.factory.SmartFactoryBean;
   70 | import org.springframework.beans.factory.SmartInitializingSingleton;
   71 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   72 | import org.springframework.beans.factory.config.BeanDefinition;
   73 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   74 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   75 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   76 | import org.springframework.beans.factory.config.DependencyDescriptor;
   77 | import org.springframework.beans.factory.config.NamedBeanHolder;
   78 | import org.springframework.core.NamedThreadLocal;
   79 | import org.springframework.core.OrderComparator;
   80 | import org.springframework.core.Ordered;
   81 | import org.springframework.core.ParameterizedTypeReference;
   82 | import org.springframework.core.ResolvableType;
   83 | import org.springframework.core.SpringProperties;
   84 | import org.springframework.core.annotation.MergedAnnotation;
   85 | import org.springframework.core.annotation.MergedAnnotations;
   86 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   87 | import org.springframework.core.log.LogMessage;
   88 | import org.springframework.core.metrics.StartupStep;
   89 | import org.springframework.lang.Contract;
   90 | import org.springframework.util.Assert;
   91 | import org.springframework.util.ClassUtils;
   92 | import org.springframework.util.CollectionUtils;
   93 | import org.springframework.util.CompositeIterator;
   94 | import org.springframework.util.ConcurrentReferenceHashMap;
   95 | import org.springframework.util.ObjectUtils;
   96 | import org.springframework.util.ReflectionUtils;
   97 | import org.springframework.util.StringUtils;
   98 | 
   99 | /**
  100 |  * Spring's default implementation of the {@link ConfigurableListableBeanFactory}
  101 |  * and {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
  102 |  * based on bean definition metadata, extensible through post-processors.
  103 |  *
  104 |  * <p>Typical usage is registering all bean definitions first (possibly read
  105 |  * from a bean definition file), before accessing beans. Bean lookup by name
  106 |  * is therefore an inexpensive operation in a local bean definition table,
  107 |  * operating on pre-resolved bean definition metadata objects.
  108 |  *
  109 |  * <p>Note that readers for specific bean definition formats are typically
  110 |  * implemented separately rather than as bean factory subclasses: see for example
  111 |  * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
  112 |  *
  113 |  * <p>For an alternative implementation of the
  114 |  * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
  115 |  * have a look at {@link StaticListableBeanFactory}, which manages existing
  116 |  * bean instances rather than creating new ones based on bean definitions.
  117 |  *
  118 |  * @author Rod Johnson
  119 |  * @author Juergen Hoeller
  120 |  * @author Sam Brannen
  121 |  * @author Costin Leau
  122 |  * @author Chris Beams
  123 |  * @author Phillip Webb
  124 |  * @author Stephane Nicoll
  125 |  * @author Sebastien Deleuze
  126 |  * @since 16 April 2001
  127 |  * @see #registerBeanDefinition
  128 |  * @see #addBeanPostProcessor
  129 |  * @see #getBean
  130 |  * @see #resolveDependency
  131 |  */
  132 | @SuppressWarnings("serial")
  133 | public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
  134 | 		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
  135 | 
  136 | 	/**
  137 | 	 * System property that instructs Spring to enforce strict locking during bean creation,
  138 | 	 * rather than the mix of strict and lenient locking that 6.2 applies by default. Setting
  139 | 	 * this flag to "true" restores 6.1.x style locking in the entire pre-instantiation phase.
  140 | 	 * <p>By default, the factory infers strict locking from the encountered thread names:
  141 | 	 * If additional threads have names that match the thread prefix of the main bootstrap thread,
  142 | 	 * they are considered external (multiple external bootstrap threads calling into the factory)
  143 | 	 * and therefore have strict locking applied to them. This inference can be turned off through
  144 | 	 * explicitly setting this flag to "false" rather than leaving it unspecified.
  145 | 	 * @since 6.2.6
  146 | 	 * @see #preInstantiateSingletons()
  147 | 	 */
  148 | 	public static final String STRICT_LOCKING_PROPERTY_NAME = "spring.locking.strict";
  149 | 
  150 | 	private static @Nullable Class<?> jakartaInjectProviderClass;
  151 | 
  152 | 	static {
  153 | 		try {
  154 | 			jakartaInjectProviderClass =
  155 | 					ClassUtils.forName("jakarta.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
  156 | 		}
  157 | 		catch (ClassNotFoundException ex) {
  158 | 			// JSR-330 API not available - Provider interface simply not supported then.
  159 | 			jakartaInjectProviderClass = null;
  160 | 		}
  161 | 	}
  162 | 
  163 | 
  164 | 	/** Map from serialized id to factory instance. */
  165 | 	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
  166 | 			new ConcurrentHashMap<>(8);
  167 | 
  168 | 	/** Whether strict locking is enforced or relaxed in this factory. */
  169 | 	private final @Nullable Boolean strictLocking = SpringProperties.checkFlag(STRICT_LOCKING_PROPERTY_NAME);
  170 | 
  171 | 	/** Optional id for this factory, for serialization purposes. */
  172 | 	private @Nullable String serializationId;
  173 | 
  174 | 	/** Whether to allow re-registration of a different definition with the same name. */
  175 | 	private @Nullable Boolean allowBeanDefinitionOverriding;
  176 | 
  177 | 	/** Whether to allow eager class loading even for lazy-init beans. */
  178 | 	private boolean allowEagerClassLoading = true;
  179 | 
  180 | 	private @Nullable Executor bootstrapExecutor;
  181 | 
  182 | 	/** Optional OrderComparator for dependency Lists and arrays. */
  183 | 	private @Nullable Comparator<Object> dependencyComparator;
  184 | 
  185 | 	/** Resolver to use for checking if a bean definition is an autowire candidate. */
  186 | 	private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;
  187 | 
  188 | 	/** Map from dependency type to corresponding autowired value. */
  189 | 	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);
  190 | 
  191 | 	/** Map of bean definition objects, keyed by bean name. */
  192 | 	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
  193 | 
  194 | 	/** Map from bean name to merged BeanDefinitionHolder. */
  195 | 	private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);
  196 | 
  197 | 	/** Map of bean definition names with a primary marker plus corresponding type. */
  198 | 	private final Map<String, Class<?>> primaryBeanNamesWithType = new ConcurrentHashMap<>(16);
  199 | 
  200 | 	/** Cache of singleton and non-singleton bean names, keyed by dependency type. */
  201 | 	private final Map<Object, String[]> allBeanNamesByType = new ConcurrentReferenceHashMap<>(64);
  202 | 
  203 | 	/** Cache of singleton-only bean names, keyed by dependency type. */
  204 | 	private final Map<Object, String[]> singletonBeanNamesByType = new ConcurrentReferenceHashMap<>(64);
  205 | 
  206 | 	/** List of bean definition names, in registration order. */
  207 | 	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
  208 | 
  209 | 	/** List of names of manually registered singletons, in registration order. */
  210 | 	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);
  211 | 
  212 | 	/** Cached array of bean definition names in case of frozen configuration. */
  213 | 	private volatile String @Nullable [] frozenBeanDefinitionNames;
  214 | 
  215 | 	/** Whether bean definition metadata may be cached for all beans. */
  216 | 	private volatile boolean configurationFrozen;
  217 | 
  218 | 	/** Name prefix of main thread: only set during pre-instantiation phase. */
  219 | 	private volatile @Nullable String mainThreadPrefix;
  220 | 
  221 | 	private final NamedThreadLocal<PreInstantiation> preInstantiationThread =
  222 | 			new NamedThreadLocal<>("Pre-instantiation thread marker");
  223 | 
  224 | 
  225 | 	/**
  226 | 	 * Create a new DefaultListableBeanFactory.
  227 | 	 */
  228 | 	public DefaultListableBeanFactory() {
  229 | 	}
  230 | 
  231 | 	/**
  232 | 	 * Create a new DefaultListableBeanFactory with the given parent.
  233 | 	 * @param parentBeanFactory the parent BeanFactory
  234 | 	 */
  235 | 	public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
  236 | 		super(parentBeanFactory);
  237 | 	}
  238 | 
  239 | 
  240 | 	/**
  241 | 	 * Specify an id for serialization purposes, allowing this BeanFactory to be
  242 | 	 * deserialized from this id back into the BeanFactory object, if needed.
  243 | 	 */
  244 | 	public void setSerializationId(@Nullable String serializationId) {
  245 | 		if (serializationId != null) {
  246 | 			serializableFactories.put(serializationId, new WeakReference<>(this));
  247 | 		}
  248 | 		else if (this.serializationId != null) {
  249 | 			serializableFactories.remove(this.serializationId);
  250 | 		}
  251 | 		this.serializationId = serializationId;
  252 | 	}
  253 | 
  254 | 	/**
  255 | 	 * Return an id for serialization purposes, if specified, allowing this BeanFactory
  256 | 	 * to be deserialized from this id back into the BeanFactory object, if needed.
  257 | 	 * @since 4.1.2
  258 | 	 */
  259 | 	public @Nullable String getSerializationId() {
  260 | 		return this.serializationId;
  261 | 	}
  262 | 
  263 | 	/**
  264 | 	 * Set whether it should be allowed to override bean definitions by registering
  265 | 	 * a different definition with the same name, automatically replacing the former.
  266 | 	 * If not, an exception will be thrown. This also applies to overriding aliases.
  267 | 	 * <p>Default is "true".
  268 | 	 * @see #registerBeanDefinition
  269 | 	 */
  270 | 	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
  271 | 		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Return whether it should be allowed to override bean definitions by registering
  276 | 	 * a different definition with the same name, automatically replacing the former.
  277 | 	 * @since 4.1.2
  278 | 	 */
  279 | 	public boolean isAllowBeanDefinitionOverriding() {
  280 | 		return !Boolean.FALSE.equals(this.allowBeanDefinitionOverriding);
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * Set whether the factory is allowed to eagerly load bean classes
  285 | 	 * even for bean definitions that are marked as "lazy-init".
  286 | 	 * <p>Default is "true". Turn this flag off to suppress class loading
  287 | 	 * for lazy-init beans unless such a bean is explicitly requested.
  288 | 	 * In particular, by-type lookups will then simply ignore bean definitions
  289 | 	 * without resolved class name, instead of loading the bean classes on
  290 | 	 * demand just to perform a type check.
  291 | 	 * @see AbstractBeanDefinition#setLazyInit
  292 | 	 */
  293 | 	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
  294 | 		this.allowEagerClassLoading = allowEagerClassLoading;
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Return whether the factory is allowed to eagerly load bean classes
  299 | 	 * even for bean definitions that are marked as "lazy-init".
  300 | 	 * @since 4.1.2
  301 | 	 */
  302 | 	public boolean isAllowEagerClassLoading() {
  303 | 		return this.allowEagerClassLoading;
  304 | 	}
  305 | 
  306 | 	@Override
  307 | 	public void setBootstrapExecutor(@Nullable Executor bootstrapExecutor) {
  308 | 		this.bootstrapExecutor = bootstrapExecutor;
  309 | 	}
  310 | 
  311 | 	@Override
  312 | 	public @Nullable Executor getBootstrapExecutor() {
  313 | 		return this.bootstrapExecutor;
  314 | 	}
  315 | 
  316 | 	/**
  317 | 	 * Set a {@link java.util.Comparator} for dependency Lists and arrays.
  318 | 	 * @since 4.0
  319 | 	 * @see org.springframework.core.OrderComparator
  320 | 	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
  321 | 	 */
  322 | 	public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
  323 | 		this.dependencyComparator = dependencyComparator;
  324 | 	}
  325 | 
  326 | 	/**
  327 | 	 * Return the dependency comparator for this BeanFactory (may be {@code null}).
  328 | 	 * @since 4.0
  329 | 	 */
  330 | 	public @Nullable Comparator<Object> getDependencyComparator() {
  331 | 		return this.dependencyComparator;
  332 | 	}
  333 | 
  334 | 	/**
  335 | 	 * Set a custom autowire candidate resolver for this BeanFactory to use
  336 | 	 * when deciding whether a bean definition should be considered as a
  337 | 	 * candidate for autowiring.
  338 | 	 */
  339 | 	public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
  340 | 		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
  341 | 		if (autowireCandidateResolver instanceof BeanFactoryAware beanFactoryAware) {
  342 | 			beanFactoryAware.setBeanFactory(this);
  343 | 		}
  344 | 		this.autowireCandidateResolver = autowireCandidateResolver;
  345 | 	}
  346 | 
  347 | 	/**
  348 | 	 * Return the autowire candidate resolver for this BeanFactory (never {@code null}).
  349 | 	 */
  350 | 	public AutowireCandidateResolver getAutowireCandidateResolver() {
  351 | 		return this.autowireCandidateResolver;
  352 | 	}
  353 | 
  354 | 
  355 | 	@Override
  356 | 	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
  357 | 		super.copyConfigurationFrom(otherFactory);
  358 | 		if (otherFactory instanceof DefaultListableBeanFactory otherListableFactory) {
  359 | 			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
  360 | 			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
  361 | 			this.bootstrapExecutor = otherListableFactory.bootstrapExecutor;
  362 | 			this.dependencyComparator = otherListableFactory.dependencyComparator;
  363 | 			// A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware
  364 | 			setAutowireCandidateResolver(otherListableFactory.getAutowireCandidateResolver().cloneIfNecessary());
  365 | 			// Make resolvable dependencies (for example, ResourceLoader) available here as well
  366 | 			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
  367 | 		}
  368 | 	}
  369 | 
  370 | 
  371 | 	//---------------------------------------------------------------------
  372 | 	// Implementation of remaining BeanFactory methods
  373 | 	//---------------------------------------------------------------------
  374 | 
  375 | 	@Override
  376 | 	public <T> T getBean(Class<T> requiredType) throws BeansException {
  377 | 		return getBean(requiredType, (Object[]) null);
  378 | 	}
  379 | 
  380 | 	@SuppressWarnings("unchecked")
  381 | 	@Override
  382 | 	public <T> T getBean(Class<T> requiredType, @Nullable Object @Nullable ... args) throws BeansException {
  383 | 		Assert.notNull(requiredType, "Required type must not be null");
  384 | 		Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
  385 | 		if (resolved == null) {
  386 | 			throw new NoSuchBeanDefinitionException(requiredType);
  387 | 		}
  388 | 		return (T) resolved;
  389 | 	}
  390 | 
  391 | 	@Override
  392 | 	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
  393 | 		Assert.notNull(requiredType, "Required type must not be null");
  394 | 		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
  395 | 	}
  396 | 
  397 | 	@Override
  398 | 	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
  399 | 		return getBeanProvider(requiredType, true);
  400 | 	}
  401 | 
  402 | 	@Override
  403 | 	public <T> ObjectProvider<T> getBeanProvider(ParameterizedTypeReference<T> requiredType) {
  404 | 		return getBeanProvider(ResolvableType.forType(requiredType), true);
  405 | 	}
  406 | 
  407 | 
  408 | 	//---------------------------------------------------------------------
  409 | 	// Implementation of ListableBeanFactory interface
  410 | 	//---------------------------------------------------------------------
  411 | 
  412 | 	@Override
  413 | 	public boolean containsBeanDefinition(String beanName) {
  414 | 		Assert.notNull(beanName, "Bean name must not be null");
  415 | 		return this.beanDefinitionMap.containsKey(beanName);
  416 | 	}
  417 | 
  418 | 	@Override
  419 | 	public int getBeanDefinitionCount() {
  420 | 		return this.beanDefinitionMap.size();
  421 | 	}
  422 | 
  423 | 	@Override
  424 | 	public String[] getBeanDefinitionNames() {
  425 | 		String[] frozenNames = this.frozenBeanDefinitionNames;
  426 | 		if (frozenNames != null) {
  427 | 			return frozenNames.clone();
  428 | 		}
  429 | 		else {
  430 | 			return StringUtils.toStringArray(this.beanDefinitionNames);
  431 | 		}
  432 | 	}
  433 | 
  434 | 	@Override
  435 | 	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
  436 | 		Assert.notNull(requiredType, "Required type must not be null");
  437 | 		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
  438 | 	}
  439 | 
  440 | 	@Override
  441 | 	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
  442 | 		return new BeanObjectProvider<>() {
  443 | 			@Override
  444 | 			public T getObject() throws BeansException {
  445 | 				T resolved = resolveBean(requiredType, null, false);
  446 | 				if (resolved == null) {
  447 | 					throw new NoSuchBeanDefinitionException(requiredType);
  448 | 				}
  449 | 				return resolved;
  450 | 			}
  451 | 			@Override
  452 | 			public T getObject(@Nullable Object... args) throws BeansException {
  453 | 				T resolved = resolveBean(requiredType, args, false);
  454 | 				if (resolved == null) {
  455 | 					throw new NoSuchBeanDefinitionException(requiredType);
  456 | 				}
  457 | 				return resolved;
  458 | 			}
  459 | 			@Override
  460 | 			public @Nullable T getIfAvailable() throws BeansException {
  461 | 				try {
  462 | 					return resolveBean(requiredType, null, false);
  463 | 				}
  464 | 				catch (ScopeNotActiveException ex) {
  465 | 					// Ignore resolved bean in non-active scope
  466 | 					return null;
  467 | 				}
  468 | 			}
  469 | 			@Override
  470 | 			public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
  471 | 				T dependency = getIfAvailable();
  472 | 				if (dependency != null) {
  473 | 					try {
  474 | 						dependencyConsumer.accept(dependency);
  475 | 					}
  476 | 					catch (ScopeNotActiveException ex) {
  477 | 						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
  478 | 					}
  479 | 				}
  480 | 			}
  481 | 			@Override
  482 | 			public @Nullable T getIfUnique() throws BeansException {
  483 | 				try {
  484 | 					return resolveBean(requiredType, null, true);
  485 | 				}
  486 | 				catch (ScopeNotActiveException ex) {
  487 | 					// Ignore resolved bean in non-active scope
  488 | 					return null;
  489 | 				}
  490 | 			}
  491 | 			@Override
  492 | 			public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
  493 | 				T dependency = getIfUnique();
  494 | 				if (dependency != null) {
  495 | 					try {
  496 | 						dependencyConsumer.accept(dependency);
  497 | 					}
  498 | 					catch (ScopeNotActiveException ex) {
  499 | 						// Ignore resolved bean in non-active scope, even on scoped proxy invocation
  500 | 					}
  501 | 				}
  502 | 			}
  503 | 			@SuppressWarnings("unchecked")
  504 | 			@Override
  505 | 			public Stream<T> stream() {
  506 | 				return Arrays.stream(beanNamesForStream(requiredType, true, allowEagerInit))
  507 | 						.map(name -> (T) resolveBean(name, requiredType))
  508 | 						.filter(bean -> !(bean instanceof NullBean));
  509 | 			}
  510 | 			@SuppressWarnings("unchecked")
  511 | 			@Override
  512 | 			public Stream<T> orderedStream() {
  513 | 				String[] beanNames = beanNamesForStream(requiredType, true, allowEagerInit);
  514 | 				if (beanNames.length == 0) {
  515 | 					return Stream.empty();
  516 | 				}
  517 | 				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
  518 | 				for (String beanName : beanNames) {
  519 | 					Object beanInstance = resolveBean(beanName, requiredType);
  520 | 					if (!(beanInstance instanceof NullBean)) {
  521 | 						matchingBeans.put(beanName, (T) beanInstance);
  522 | 					}
  523 | 				}
  524 | 				Stream<T> stream = matchingBeans.values().stream();
  525 | 				return stream.sorted(adaptOrderComparator(matchingBeans));
  526 | 			}
  527 | 			@SuppressWarnings("unchecked")
  528 | 			@Override
  529 | 			public Stream<T> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
  530 | 				return Arrays.stream(beanNamesForStream(requiredType, includeNonSingletons, allowEagerInit))
  531 | 						.filter(name -> customFilter.test(getType(name)))
  532 | 						.map(name -> (T) resolveBean(name, requiredType))
  533 | 						.filter(bean -> !(bean instanceof NullBean));
  534 | 			}
  535 | 			@SuppressWarnings("unchecked")
  536 | 			@Override
  537 | 			public Stream<T> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
  538 | 				String[] beanNames = beanNamesForStream(requiredType, includeNonSingletons, allowEagerInit);
  539 | 				if (beanNames.length == 0) {
  540 | 					return Stream.empty();
  541 | 				}
  542 | 				Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
  543 | 				for (String beanName : beanNames) {
  544 | 					if (customFilter.test(getType(beanName))) {
  545 | 						Object beanInstance = resolveBean(beanName, requiredType);
  546 | 						if (!(beanInstance instanceof NullBean)) {
  547 | 							matchingBeans.put(beanName, (T) beanInstance);
  548 | 						}
  549 | 					}
  550 | 				}
  551 | 				return matchingBeans.values().stream().sorted(adaptOrderComparator(matchingBeans));
  552 | 			}
  553 | 		};
  554 | 	}
  555 | 
  556 | 	private <T> @Nullable T resolveBean(ResolvableType requiredType, @Nullable Object @Nullable [] args, boolean nonUniqueAsNull) {
  557 | 		NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
  558 | 		if (namedBean != null) {
  559 | 			return namedBean.getBeanInstance();
  560 | 		}
  561 | 		BeanFactory parent = getParentBeanFactory();
  562 | 		if (parent instanceof DefaultListableBeanFactory dlbf) {
  563 | 			return dlbf.resolveBean(requiredType, args, nonUniqueAsNull);
  564 | 		}
  565 | 		else if (parent != null) {
  566 | 			ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
  567 | 			if (args != null) {
  568 | 				return parentProvider.getObject(args);
  569 | 			}
  570 | 			else {
  571 | 				return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
  572 | 			}
  573 | 		}
  574 | 		return null;
  575 | 	}
  576 | 
  577 | 	private String[] beanNamesForStream(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
  578 | 		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, includeNonSingletons, allowEagerInit);
  579 | 	}
  580 | 
  581 | 	@Override
  582 | 	public String[] getBeanNamesForType(ResolvableType type) {
  583 | 		return getBeanNamesForType(type, true, true);
  584 | 	}
  585 | 
  586 | 	@Override
  587 | 	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
  588 | 		Class<?> resolved = type.resolve();
  589 | 		if (resolved != null && !type.hasGenerics()) {
  590 | 			return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
  591 | 		}
  592 | 
  593 | 		// ResolvableType with actual generics to be matched...
  594 | 		if (!isConfigurationFrozen() || !allowEagerInit) {
  595 | 			return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  596 | 		}
  597 | 		Map<Object, String[]> cache = (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
  598 | 		return cache.computeIfAbsent(type, key -> doGetBeanNamesForType(type, includeNonSingletons, true));
  599 | 	}
  600 | 
  601 | 	@Override
  602 | 	public String[] getBeanNamesForType(@Nullable Class<?> type) {
  603 | 		return getBeanNamesForType(type, true, true);
  604 | 	}
  605 | 
  606 | 	@Override
  607 | 	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
  608 | 		if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
  609 | 			return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
  610 | 		}
  611 | 		Map<Object, String[]> cache = (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
  612 | 		return cache.computeIfAbsent(type, key ->
  613 | 				doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true));
  614 | 	}
  615 | 
  616 | 	private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
  617 | 		List<String> result = new ArrayList<>();
  618 | 
  619 | 		// Check all bean definitions.
  620 | 		for (String beanName : this.beanDefinitionNames) {
  621 | 			// Only consider bean as eligible if the bean name is not defined as alias for some other bean.
  622 | 			if (!isAlias(beanName)) {
  623 | 				try {
  624 | 					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  625 | 					// Only check bean definition if it is complete.
  626 | 					if (!mbd.isAbstract() && (allowEagerInit ||
  627 | 							(mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
  628 | 									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
  629 | 						boolean isFactoryBean = isFactoryBean(beanName, mbd);
  630 | 						BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
  631 | 						boolean matchFound = false;
  632 | 						boolean allowFactoryBeanInit = (allowEagerInit || containsSingleton(beanName));
  633 | 						boolean isNonLazyDecorated = (dbd != null && !mbd.isLazyInit());
  634 | 						if (!isFactoryBean) {
  635 | 							if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
  636 | 								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
  637 | 							}
  638 | 						}
  639 | 						else {
  640 | 							if (includeNonSingletons || isNonLazyDecorated) {
  641 | 								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
  642 | 							}
  643 | 							else if (allowFactoryBeanInit) {
  644 | 								// Type check before singleton check, avoiding FactoryBean instantiation
  645 | 								// for early FactoryBean.isSingleton() calls on non-matching beans.
  646 | 								matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit) &&
  647 | 										isSingleton(beanName, mbd, dbd);
  648 | 							}
  649 | 							if (!matchFound) {
  650 | 								// In case of FactoryBean, try to match FactoryBean instance itself next.
  651 | 								beanName = FACTORY_BEAN_PREFIX + beanName;
  652 | 								if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
  653 | 									matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
  654 | 								}
  655 | 							}
  656 | 						}
  657 | 						if (matchFound) {
  658 | 							result.add(beanName);
  659 | 						}
  660 | 					}
  661 | 				}
  662 | 				catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
  663 | 					if (allowEagerInit) {
  664 | 						throw ex;
  665 | 					}
  666 | 					// Probably a placeholder: let's ignore it for type matching purposes.
  667 | 					LogMessage message = (ex instanceof CannotLoadBeanClassException ?
  668 | 							LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
  669 | 							LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName));
  670 | 					logger.trace(message, ex);
  671 | 					// Register exception, in case the bean was accidentally unresolvable.
  672 | 					onSuppressedException(ex);
  673 | 				}
  674 | 				catch (NoSuchBeanDefinitionException ex) {
  675 | 					// Bean definition got removed while we were iterating -> ignore.
  676 | 				}
  677 | 			}
  678 | 		}
  679 | 
  680 | 		// Check manually registered singletons too.
  681 | 		for (String beanName : this.manualSingletonNames) {
  682 | 			try {
  683 | 				// In case of FactoryBean, match object created by FactoryBean.
  684 | 				if (isFactoryBean(beanName)) {
  685 | 					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
  686 | 						result.add(beanName);
  687 | 						// Match found for this bean: do not match FactoryBean itself anymore.
  688 | 						continue;
  689 | 					}
  690 | 					// In case of FactoryBean, try to match FactoryBean itself next.
  691 | 					beanName = FACTORY_BEAN_PREFIX + beanName;
  692 | 				}
  693 | 				// Match raw bean instance (might be raw FactoryBean).
  694 | 				if (isTypeMatch(beanName, type)) {
  695 | 					result.add(beanName);
  696 | 				}
  697 | 			}
  698 | 			catch (NoSuchBeanDefinitionException ex) {
  699 | 				// Shouldn't happen - probably a result of circular reference resolution...
  700 | 				logger.trace(LogMessage.format(
  701 | 						"Failed to check manually registered singleton with name '%s'", beanName), ex);
  702 | 			}
  703 | 		}
  704 | 
  705 | 		return StringUtils.toStringArray(result);
  706 | 	}
  707 | 
  708 | 	private boolean isSingleton(String beanName, RootBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
  709 | 		return (dbd != null ? mbd.isSingleton() : isSingleton(beanName));
  710 | 	}
  711 | 
  712 | 	/**
  713 | 	 * Check whether the specified bean would need to be eagerly initialized
  714 | 	 * in order to determine its type.
  715 | 	 * @param factoryBeanName a factory-bean reference that the bean definition
  716 | 	 * defines a factory method for
  717 | 	 * @return whether eager initialization is necessary
  718 | 	 */
  719 | 	private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
  720 | 		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
  721 | 	}
  722 | 
  723 | 	@Override
  724 | 	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
  725 | 		return getBeansOfType(type, true, true);
  726 | 	}
  727 | 
  728 | 	@Override
  729 | 	@SuppressWarnings("unchecked")
  730 | 	public <T> Map<String, T> getBeansOfType(
  731 | 			@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
  732 | 
  733 | 		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
  734 | 		Map<String, T> result = CollectionUtils.newLinkedHashMap(beanNames.length);
  735 | 		for (String beanName : beanNames) {
  736 | 			try {
  737 | 				Object beanInstance = (type != null ? getBean(beanName, type) : getBean(beanName));
  738 | 				if (!(beanInstance instanceof NullBean)) {
  739 | 					result.put(beanName, (T) beanInstance);
  740 | 				}
  741 | 			}
  742 | 			catch (BeanNotOfRequiredTypeException ex) {
  743 | 				// Ignore - probably a NullBean
  744 | 			}
  745 | 			catch (BeanCreationException ex) {
  746 | 				Throwable rootCause = ex.getMostSpecificCause();
  747 | 				if (rootCause instanceof BeanCurrentlyInCreationException bce) {
  748 | 					String exBeanName = bce.getBeanName();
  749 | 					if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
  750 | 						if (logger.isTraceEnabled()) {
  751 | 							logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " +
  752 | 									ex.getMessage());
  753 | 						}
  754 | 						onSuppressedException(ex);
  755 | 						// Ignore: indicates a circular reference when autowiring constructors.
  756 | 						// We want to find matches other than the currently created bean itself.
  757 | 						continue;
  758 | 					}
  759 | 				}
  760 | 				throw ex;
  761 | 			}
  762 | 		}
  763 | 		return result;
  764 | 	}
  765 | 
  766 | 	@Override
  767 | 	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
  768 | 		List<String> result = new ArrayList<>();
  769 | 		for (String beanName : this.beanDefinitionNames) {
  770 | 			BeanDefinition bd = this.beanDefinitionMap.get(beanName);
  771 | 			if (bd != null && !bd.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
  772 | 				result.add(beanName);
  773 | 			}
  774 | 		}
  775 | 		for (String beanName : this.manualSingletonNames) {
  776 | 			if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
  777 | 				result.add(beanName);
  778 | 			}
  779 | 		}
  780 | 		return StringUtils.toStringArray(result);
  781 | 	}
  782 | 
  783 | 	@Override
  784 | 	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
  785 | 		String[] beanNames = getBeanNamesForAnnotation(annotationType);
  786 | 		Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.length);
  787 | 		for (String beanName : beanNames) {
  788 | 			Object beanInstance = getBean(beanName);
  789 | 			if (!(beanInstance instanceof NullBean)) {
  790 | 				result.put(beanName, beanInstance);
  791 | 			}
  792 | 		}
  793 | 		return result;
  794 | 	}
  795 | 
  796 | 	@Override
  797 | 	public <A extends Annotation> @Nullable A findAnnotationOnBean(String beanName, Class<A> annotationType)
  798 | 			throws NoSuchBeanDefinitionException {
  799 | 
  800 | 		return findAnnotationOnBean(beanName, annotationType, true);
  801 | 	}
  802 | 
  803 | 	@Override
  804 | 	public <A extends Annotation> @Nullable A findAnnotationOnBean(
  805 | 			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
  806 | 			throws NoSuchBeanDefinitionException {
  807 | 
  808 | 		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
  809 | 		if (beanType != null) {
  810 | 			MergedAnnotation<A> annotation =
  811 | 					MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
  812 | 			if (annotation.isPresent()) {
  813 | 				return annotation.synthesize();
  814 | 			}
  815 | 		}
  816 | 		if (containsBeanDefinition(beanName)) {
  817 | 			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
  818 | 			// Check raw bean class, for example, in case of a proxy.
  819 | 			if (bd.hasBeanClass() && bd.getFactoryMethodName() == null) {
  820 | 				Class<?> beanClass = bd.getBeanClass();
  821 | 				if (beanClass != beanType) {
  822 | 					MergedAnnotation<A> annotation =
  823 | 							MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
  824 | 					if (annotation.isPresent()) {
  825 | 						return annotation.synthesize();
  826 | 					}
  827 | 				}
  828 | 			}
  829 | 			// Check annotations declared on factory method, if any.
  830 | 			Method factoryMethod = bd.getResolvedFactoryMethod();
  831 | 			if (factoryMethod != null) {
  832 | 				MergedAnnotation<A> annotation =
  833 | 						MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
  834 | 				if (annotation.isPresent()) {
  835 | 					return annotation.synthesize();
  836 | 				}
  837 | 			}
  838 | 		}
  839 | 		return null;
  840 | 	}
  841 | 
  842 | 	@Override
  843 | 	public <A extends Annotation> Set<A> findAllAnnotationsOnBean(
  844 | 			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
  845 | 			throws NoSuchBeanDefinitionException {
  846 | 
  847 | 		Set<A> annotations = new LinkedHashSet<>();
  848 | 		Class<?> beanType = getType(beanName, allowFactoryBeanInit);
  849 | 		if (beanType != null) {
  850 | 			MergedAnnotations.from(beanType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
  851 | 					.stream(annotationType)
  852 | 					.filter(MergedAnnotation::isPresent)
  853 | 					.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
  854 | 		}
  855 | 		if (containsBeanDefinition(beanName)) {
  856 | 			RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
  857 | 			// Check raw bean class, for example, in case of a proxy.
  858 | 			if (bd.hasBeanClass() && bd.getFactoryMethodName() == null) {
  859 | 				Class<?> beanClass = bd.getBeanClass();
  860 | 				if (beanClass != beanType) {
  861 | 					MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
  862 | 							.stream(annotationType)
  863 | 							.filter(MergedAnnotation::isPresent)
  864 | 							.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
  865 | 				}
  866 | 			}
  867 | 			// Check annotations declared on factory method, if any.
  868 | 			Method factoryMethod = bd.getResolvedFactoryMethod();
  869 | 			if (factoryMethod != null) {
  870 | 				MergedAnnotations.from(factoryMethod, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
  871 | 						.stream(annotationType)
  872 | 						.filter(MergedAnnotation::isPresent)
  873 | 						.forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
  874 | 			}
  875 | 		}
  876 | 		return annotations;
  877 | 	}
  878 | 
  879 | 
  880 | 	//---------------------------------------------------------------------
  881 | 	// Implementation of ConfigurableListableBeanFactory interface
  882 | 	//---------------------------------------------------------------------
  883 | 
  884 | 	@Override
  885 | 	public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
  886 | 		Assert.notNull(dependencyType, "Dependency type must not be null");
  887 | 		if (autowiredValue != null) {
  888 | 			if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
  889 | 				throw new IllegalArgumentException("Value [" + autowiredValue +
  890 | 						"] does not implement specified dependency type [" + dependencyType.getName() + "]");
  891 | 			}
  892 | 			this.resolvableDependencies.put(dependencyType, autowiredValue);
  893 | 		}
  894 | 	}
  895 | 
  896 | 	@Override
  897 | 	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
  898 | 			throws NoSuchBeanDefinitionException {
  899 | 
  900 | 		return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
  901 | 	}
  902 | 
  903 | 	/**
  904 | 	 * Determine whether the specified bean definition qualifies as an autowire candidate,
  905 | 	 * to be injected into other beans which declare a dependency of matching type.
  906 | 	 * @param beanName the name of the bean definition to check
  907 | 	 * @param descriptor the descriptor of the dependency to resolve
  908 | 	 * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
  909 | 	 * @return whether the bean should be considered as autowire candidate
  910 | 	 */
  911 | 	protected boolean isAutowireCandidate(
  912 | 			String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
  913 | 			throws NoSuchBeanDefinitionException {
  914 | 
  915 | 		String bdName = transformedBeanName(beanName);
  916 | 		if (containsBeanDefinition(bdName)) {
  917 | 			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
  918 | 		}
  919 | 		else if (containsSingleton(beanName)) {
  920 | 			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
  921 | 		}
  922 | 
  923 | 		BeanFactory parent = getParentBeanFactory();
  924 | 		if (parent instanceof DefaultListableBeanFactory dlbf) {
  925 | 			// No bean definition found in this factory -> delegate to parent.
  926 | 			return dlbf.isAutowireCandidate(beanName, descriptor, resolver);
  927 | 		}
  928 | 		else if (parent instanceof ConfigurableListableBeanFactory clbf) {
  929 | 			// If no DefaultListableBeanFactory, can't pass the resolver along.
  930 | 			return clbf.isAutowireCandidate(beanName, descriptor);
  931 | 		}
  932 | 		else {
  933 | 			return true;
  934 | 		}
  935 | 	}
  936 | 
  937 | 	/**
  938 | 	 * Determine whether the specified bean definition qualifies as an autowire candidate,
  939 | 	 * to be injected into other beans which declare a dependency of matching type.
  940 | 	 * @param beanName the name of the bean definition to check
  941 | 	 * @param mbd the merged bean definition to check
  942 | 	 * @param descriptor the descriptor of the dependency to resolve
  943 | 	 * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
  944 | 	 * @return whether the bean should be considered as autowire candidate
  945 | 	 */
  946 | 	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
  947 | 			DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {
  948 | 
  949 | 		String bdName = transformedBeanName(beanName);
  950 | 		resolveBeanClass(mbd, bdName);
  951 | 		if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
  952 | 			new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
  953 | 		}
  954 | 		BeanDefinitionHolder holder = (beanName.equals(bdName) ?
  955 | 				this.mergedBeanDefinitionHolders.computeIfAbsent(beanName,
  956 | 						key -> new BeanDefinitionHolder(mbd, beanName, getAliases(bdName))) :
  957 | 				new BeanDefinitionHolder(mbd, beanName, getAliases(bdName)));
  958 | 		return resolver.isAutowireCandidate(holder, descriptor);
  959 | 	}
  960 | 
  961 | 	@Override
  962 | 	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
  963 | 		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
  964 | 		if (bd == null) {
  965 | 			if (logger.isTraceEnabled()) {
  966 | 				logger.trace("No bean named '" + beanName + "' found in " + this);
  967 | 			}
  968 | 			throw new NoSuchBeanDefinitionException(beanName);
  969 | 		}
  970 | 		return bd;
  971 | 	}
  972 | 
  973 | 	@Override
  974 | 	public Iterator<String> getBeanNamesIterator() {
  975 | 		CompositeIterator<String> iterator = new CompositeIterator<>();
  976 | 		iterator.add(this.beanDefinitionNames.iterator());
  977 | 		iterator.add(this.manualSingletonNames.iterator());
  978 | 		return iterator;
  979 | 	}
  980 | 
  981 | 	@Override
  982 | 	protected void clearMergedBeanDefinition(String beanName) {
  983 | 		super.clearMergedBeanDefinition(beanName);
  984 | 		this.mergedBeanDefinitionHolders.remove(beanName);
  985 | 	}
  986 | 
  987 | 	@Override
  988 | 	public void clearMetadataCache() {
  989 | 		super.clearMetadataCache();
  990 | 		this.mergedBeanDefinitionHolders.clear();
  991 | 		clearByTypeCache();
  992 | 	}
  993 | 
  994 | 	@Override
  995 | 	public void freezeConfiguration() {
  996 | 		clearMetadataCache();
  997 | 		this.configurationFrozen = true;
  998 | 		this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
  999 | 	}
 1000 | 
 1001 | 	@Override
 1002 | 	public boolean isConfigurationFrozen() {
 1003 | 		return this.configurationFrozen;
 1004 | 	}
 1005 | 
 1006 | 	/**
 1007 | 	 * Considers all beans as eligible for metadata caching
 1008 | 	 * if the factory's configuration has been marked as frozen.
 1009 | 	 * @see #freezeConfiguration()
 1010 | 	 */
 1011 | 	@Override
 1012 | 	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
 1013 | 		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
 1014 | 	}
 1015 | 
 1016 | 	@Override
 1017 | 	protected @Nullable Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd)
 1018 | 			throws Exception {
 1019 | 
 1020 | 		if (supplier instanceof InstanceSupplier<?> instanceSupplier) {
 1021 | 			return instanceSupplier.get(RegisteredBean.of(this, beanName, mbd));
 1022 | 		}
 1023 | 		return super.obtainInstanceFromSupplier(supplier, beanName, mbd);
 1024 | 	}
 1025 | 
 1026 | 	@Override
 1027 | 	protected void cacheMergedBeanDefinition(RootBeanDefinition mbd, String beanName) {
 1028 | 		super.cacheMergedBeanDefinition(mbd, beanName);
 1029 | 		if (mbd.isPrimary()) {
 1030 | 			this.primaryBeanNamesWithType.put(beanName, Void.class);
 1031 | 		}
 1032 | 	}
 1033 | 
 1034 | 	@Override
 1035 | 	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object @Nullable [] args) {
 1036 | 		super.checkMergedBeanDefinition(mbd, beanName, args);
 1037 | 
 1038 | 		if (mbd.isBackgroundInit()) {
 1039 | 			if (this.preInstantiationThread.get() == PreInstantiation.MAIN && getBootstrapExecutor() != null) {
 1040 | 				throw new BeanCurrentlyInCreationException(beanName, "Bean marked for background " +
 1041 | 						"initialization but requested in mainline thread - declare ObjectProvider " +
 1042 | 						"or lazy injection point in dependent mainline beans");
 1043 | 			}
 1044 | 		}
 1045 | 		else {
 1046 | 			// Bean intended to be initialized in main bootstrap thread.
 1047 | 			if (this.preInstantiationThread.get() == PreInstantiation.BACKGROUND) {
 1048 | 				throw new BeanCurrentlyInCreationException(beanName, "Bean marked for mainline initialization " +
 1049 | 						"but requested in background thread - enforce early instantiation in mainline thread " +
 1050 | 						"through depends-on '" + beanName + "' declaration for dependent background beans");
 1051 | 			}
 1052 | 		}
 1053 | 	}
 1054 | 
 1055 | 	@Override
 1056 | 	protected @Nullable Boolean isCurrentThreadAllowedToHoldSingletonLock() {
 1057 | 		String mainThreadPrefix = this.mainThreadPrefix;
 1058 | 		if (mainThreadPrefix != null) {
 1059 | 			// We only differentiate in the preInstantiateSingletons phase, using
 1060 | 			// the volatile mainThreadPrefix field as an indicator for that phase.
 1061 | 
 1062 | 			PreInstantiation preInstantiation = this.preInstantiationThread.get();
 1063 | 			if (preInstantiation != null) {
 1064 | 				// A Spring-managed bootstrap thread:
 1065 | 				// MAIN is allowed to lock (true) or even forced to lock (null),
 1066 | 				// BACKGROUND is never allowed to lock (false).
 1067 | 				return switch (preInstantiation) {
 1068 | 					case MAIN -> (Boolean.TRUE.equals(this.strictLocking) ? null : true);
 1069 | 					case BACKGROUND -> false;
 1070 | 				};
 1071 | 			}
 1072 | 
 1073 | 			// Not a Spring-managed bootstrap thread...
 1074 | 			if (Boolean.FALSE.equals(this.strictLocking)) {
 1075 | 				// Explicitly configured to use lenient locking wherever possible.
 1076 | 				return true;
 1077 | 			}
 1078 | 			else if (this.strictLocking == null) {
 1079 | 				// No explicit locking configuration -> infer appropriate locking.
 1080 | 				if (!getThreadNamePrefix().equals(mainThreadPrefix)) {
 1081 | 					// An unmanaged thread (assumed to be application-internal) with lenient locking,
 1082 | 					// and not part of the same thread pool that provided the main bootstrap thread
 1083 | 					// (excluding scenarios where we are hit by multiple external bootstrap threads).
 1084 | 					return true;
 1085 | 				}
 1086 | 			}
 1087 | 		}
 1088 | 
 1089 | 		// Traditional behavior: forced to always hold a full lock.
 1090 | 		return null;
 1091 | 	}
 1092 | 
 1093 | 	@Override
 1094 | 	public void prepareSingletonBootstrap() {
 1095 | 		this.mainThreadPrefix = getThreadNamePrefix();
 1096 | 	}
 1097 | 
 1098 | 	@Override
 1099 | 	public void preInstantiateSingletons() throws BeansException {
 1100 | 		if (logger.isTraceEnabled()) {
 1101 | 			logger.trace("Pre-instantiating singletons in " + this);
 1102 | 		}
 1103 | 
 1104 | 		// Iterate over a copy to allow for init methods which in turn register new bean definitions.
 1105 | 		// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
 1106 | 		List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
 1107 | 
 1108 | 		// Trigger initialization of all non-lazy singleton beans...
 1109 | 		this.preInstantiationThread.set(PreInstantiation.MAIN);
 1110 | 		if (this.mainThreadPrefix == null) {
 1111 | 			this.mainThreadPrefix = getThreadNamePrefix();
 1112 | 		}
 1113 | 		try {
 1114 | 			List<CompletableFuture<?>> futures = new ArrayList<>();
 1115 | 			for (String beanName : beanNames) {
 1116 | 				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
 1117 | 				if (!mbd.isAbstract() && mbd.isSingleton()) {
 1118 | 					CompletableFuture<?> future = preInstantiateSingleton(beanName, mbd);
 1119 | 					if (future != null) {
 1120 | 						futures.add(future);
 1121 | 					}
 1122 | 				}
 1123 | 			}
 1124 | 			if (!futures.isEmpty()) {
 1125 | 				try {
 1126 | 					CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
 1127 | 				}
 1128 | 				catch (CompletionException ex) {
 1129 | 					ReflectionUtils.rethrowRuntimeException(ex.getCause());
 1130 | 				}
 1131 | 			}
 1132 | 		}
 1133 | 		finally {
 1134 | 			this.mainThreadPrefix = null;
 1135 | 			this.preInstantiationThread.remove();
 1136 | 		}
 1137 | 
 1138 | 		// Trigger post-initialization callback for all applicable beans...
 1139 | 		for (String beanName : beanNames) {
 1140 | 			Object singletonInstance = getSingleton(beanName, false);
 1141 | 			if (singletonInstance instanceof SmartInitializingSingleton smartSingleton) {
 1142 | 				StartupStep smartInitialize = getApplicationStartup().start("spring.beans.smart-initialize")
 1143 | 						.tag("beanName", beanName);
 1144 | 				smartSingleton.afterSingletonsInstantiated();
 1145 | 				smartInitialize.end();
 1146 | 			}
 1147 | 		}
 1148 | 	}
 1149 | 
 1150 | 	private @Nullable CompletableFuture<?> preInstantiateSingleton(String beanName, RootBeanDefinition mbd) {
 1151 | 		if (mbd.isBackgroundInit()) {
 1152 | 			Executor executor = getBootstrapExecutor();
 1153 | 			if (executor != null) {
 1154 | 				// Force initialization of depends-on beans in mainline thread.
 1155 | 				String[] dependsOn = mbd.getDependsOn();
 1156 | 				if (dependsOn != null) {
 1157 | 					for (String dep : dependsOn) {
 1158 | 						getBean(dep);
 1159 | 					}
 1160 | 				}
 1161 | 				// Force initialization of factory reference in mainline thread.
 1162 | 				String factoryBeanName = mbd.getFactoryBeanName();
 1163 | 				if (factoryBeanName != null) {
 1164 | 					getBean(factoryBeanName);
 1165 | 				}
 1166 | 				// Instantiate current bean in background thread.
 1167 | 				CompletableFuture<?> future = CompletableFuture.runAsync(
 1168 | 						() -> instantiateSingletonInBackgroundThread(beanName), executor);
 1169 | 				addSingletonFactory(beanName, () -> {
 1170 | 					try {
 1171 | 						future.join();
 1172 | 					}
 1173 | 					catch (CompletionException ex) {
 1174 | 						ReflectionUtils.rethrowRuntimeException(ex.getCause());
 1175 | 					}
 1176 | 					return future;  // not to be exposed, just to lead to ClassCastException in case of mismatch
 1177 | 				});
 1178 | 				return (!mbd.isLazyInit() ? future : null);
 1179 | 			}
 1180 | 			else if (logger.isInfoEnabled()) {
 1181 | 				logger.info("Bean '" + beanName + "' marked for background initialization " +
 1182 | 						"without bootstrap executor configured - falling back to mainline initialization");
 1183 | 			}
 1184 | 		}
 1185 | 
 1186 | 		if (!mbd.isLazyInit()) {
 1187 | 			try {
 1188 | 				instantiateSingleton(beanName);
 1189 | 			}
 1190 | 			catch (BeanCurrentlyInCreationException ex) {
 1191 | 				logger.info("Bean '" + beanName + "' marked for pre-instantiation (not lazy-init) " +
 1192 | 						"but currently initialized by other thread - skipping it in mainline thread");
 1193 | 			}
 1194 | 		}
 1195 | 		return null;
 1196 | 	}
 1197 | 
 1198 | 	private void instantiateSingletonInBackgroundThread(String beanName) {
 1199 | 		this.preInstantiationThread.set(PreInstantiation.BACKGROUND);
 1200 | 		try {
 1201 | 			instantiateSingleton(beanName);
 1202 | 		}
 1203 | 		catch (RuntimeException | Error ex) {
 1204 | 			if (logger.isWarnEnabled()) {
 1205 | 				logger.warn("Failed to instantiate singleton bean '" + beanName + "' in background thread", ex);
 1206 | 			}
 1207 | 			throw ex;
 1208 | 		}
 1209 | 		finally {
 1210 | 			this.preInstantiationThread.remove();
 1211 | 		}
 1212 | 	}
 1213 | 
 1214 | 	private void instantiateSingleton(String beanName) {
 1215 | 		if (isFactoryBean(beanName)) {
 1216 | 			Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
 1217 | 			if (bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isEagerInit()) {
 1218 | 				getBean(beanName);
 1219 | 			}
 1220 | 		}
 1221 | 		else {
 1222 | 			getBean(beanName);
 1223 | 		}
 1224 | 	}
 1225 | 
 1226 | 	private Object resolveBean(String beanName, ResolvableType requiredType) {
 1227 | 		try {
 1228 | 			// Need to provide required type for SmartFactoryBean
 1229 | 			return getBean(beanName, requiredType.toClass());
 1230 | 		}
 1231 | 		catch (BeanNotOfRequiredTypeException ex) {
 1232 | 			// Probably a null bean...
 1233 | 			return getBean(beanName);
 1234 | 		}
 1235 | 	}
 1236 | 
 1237 | 	private static String getThreadNamePrefix() {
 1238 | 		String name = Thread.currentThread().getName();
 1239 | 		int numberSeparator = name.lastIndexOf('-');
 1240 | 		return (numberSeparator >= 0 ? name.substring(0, numberSeparator) : name);
 1241 | 	}
 1242 | 
 1243 | 
 1244 | 	//---------------------------------------------------------------------
 1245 | 	// Implementation of BeanDefinitionRegistry interface
 1246 | 	//---------------------------------------------------------------------
 1247 | 
 1248 | 	@Override
 1249 | 	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
 1250 | 			throws BeanDefinitionStoreException {
 1251 | 
 1252 | 		Assert.hasText(beanName, "Bean name must not be empty");
 1253 | 		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
 1254 | 
 1255 | 		if (beanDefinition instanceof AbstractBeanDefinition abd) {
 1256 | 			try {
 1257 | 				abd.validate();
 1258 | 			}
 1259 | 			catch (BeanDefinitionValidationException ex) {
 1260 | 				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
 1261 | 						"Validation of bean definition failed", ex);
 1262 | 			}
 1263 | 		}
 1264 | 
 1265 | 		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
 1266 | 		if (existingDefinition != null) {
 1267 | 			if (!isBeanDefinitionOverridable(beanName)) {
 1268 | 				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
 1269 | 			}
 1270 | 			else {
 1271 | 				logBeanDefinitionOverriding(beanName, beanDefinition, existingDefinition);
 1272 | 			}
 1273 | 			this.beanDefinitionMap.put(beanName, beanDefinition);
 1274 | 		}
 1275 | 		else {
 1276 | 			if (isAlias(beanName)) {
 1277 | 				String aliasedName = canonicalName(beanName);
 1278 | 				if (!isBeanDefinitionOverridable(aliasedName)) {
 1279 | 					if (containsBeanDefinition(aliasedName)) {  // alias for existing bean definition
 1280 | 						throw new BeanDefinitionOverrideException(
 1281 | 								beanName, beanDefinition, getBeanDefinition(aliasedName));
 1282 | 					}
 1283 | 					else {  // alias pointing to non-existing bean definition
 1284 | 						throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
 1285 | 								"Cannot register bean definition for bean '" + beanName +
 1286 | 								"' since there is already an alias for bean '" + aliasedName + "' bound.");
 1287 | 					}
 1288 | 				}
 1289 | 				else {
 1290 | 					if (logger.isInfoEnabled()) {
 1291 | 						logger.info("Removing alias '" + beanName + "' for bean '" + aliasedName +
 1292 | 								"' due to registration of bean definition for bean '" + beanName + "': [" +
 1293 | 								beanDefinition + "]");
 1294 | 					}
 1295 | 					removeAlias(beanName);
 1296 | 				}
 1297 | 			}
 1298 | 			if (hasBeanCreationStarted()) {
 1299 | 				// Cannot modify startup-time collection elements anymore (for stable iteration)
 1300 | 				synchronized (this.beanDefinitionMap) {
 1301 | 					this.beanDefinitionMap.put(beanName, beanDefinition);
 1302 | 					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
 1303 | 					updatedDefinitions.addAll(this.beanDefinitionNames);
 1304 | 					updatedDefinitions.add(beanName);
 1305 | 					this.beanDefinitionNames = updatedDefinitions;
 1306 | 					removeManualSingletonName(beanName);
 1307 | 				}
 1308 | 			}
 1309 | 			else {
 1310 | 				// Still in startup registration phase
 1311 | 				this.beanDefinitionMap.put(beanName, beanDefinition);
 1312 | 				this.beanDefinitionNames.add(beanName);
 1313 | 				removeManualSingletonName(beanName);
 1314 | 			}
 1315 | 			this.frozenBeanDefinitionNames = null;
 1316 | 		}
 1317 | 
 1318 | 		if (existingDefinition != null || containsSingleton(beanName)) {
 1319 | 			resetBeanDefinition(beanName);
 1320 | 		}
 1321 | 		else if (isConfigurationFrozen()) {
 1322 | 			clearByTypeCache();
 1323 | 		}
 1324 | 
 1325 | 		// Cache a primary marker for the given bean.
 1326 | 		if (beanDefinition.isPrimary()) {
 1327 | 			this.primaryBeanNamesWithType.put(beanName, Void.class);
 1328 | 		}
 1329 | 	}
 1330 | 
 1331 | 	private void logBeanDefinitionOverriding(String beanName, BeanDefinition beanDefinition,
 1332 | 			BeanDefinition existingDefinition) {
 1333 | 
 1334 | 		boolean explicitBeanOverride = (this.allowBeanDefinitionOverriding != null);
 1335 | 		if (existingDefinition.getRole() < beanDefinition.getRole()) {
 1336 | 			// for example, was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
 1337 | 			if (logger.isInfoEnabled()) {
 1338 | 				logger.info("Overriding user-defined bean definition for bean '" + beanName +
 1339 | 						"' with a framework-generated bean definition: replacing [" +
 1340 | 						existingDefinition + "] with [" + beanDefinition + "]");
 1341 | 			}
 1342 | 		}
 1343 | 		else if (!beanDefinition.equals(existingDefinition)) {
 1344 | 			if (explicitBeanOverride && logger.isInfoEnabled()) {
 1345 | 				logger.info("Overriding bean definition for bean '" + beanName +
 1346 | 						"' with a different definition: replacing [" + existingDefinition +
 1347 | 						"] with [" + beanDefinition + "]");
 1348 | 			}
 1349 | 			if (logger.isDebugEnabled()) {
 1350 | 				logger.debug("Overriding bean definition for bean '" + beanName +
 1351 | 						"' with a different definition: replacing [" + existingDefinition +
 1352 | 						"] with [" + beanDefinition + "]");
 1353 | 			}
 1354 | 		}
 1355 | 		else {
 1356 | 			if (explicitBeanOverride && logger.isInfoEnabled()) {
 1357 | 				logger.info("Overriding bean definition for bean '" + beanName +
 1358 | 						"' with an equivalent definition: replacing [" + existingDefinition +
 1359 | 						"] with [" + beanDefinition + "]");
 1360 | 			}
 1361 | 			if (logger.isTraceEnabled()) {
 1362 | 				logger.trace("Overriding bean definition for bean '" + beanName +
 1363 | 						"' with an equivalent definition: replacing [" + existingDefinition +
 1364 | 						"] with [" + beanDefinition + "]");
 1365 | 			}
 1366 | 		}
 1367 | 	}
 1368 | 
 1369 | 	@Override
 1370 | 	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
 1371 | 		Assert.hasText(beanName, "'beanName' must not be empty");
 1372 | 
 1373 | 		BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
 1374 | 		if (bd == null) {
 1375 | 			if (logger.isTraceEnabled()) {
 1376 | 				logger.trace("No bean named '" + beanName + "' found in " + this);
 1377 | 			}
 1378 | 			throw new NoSuchBeanDefinitionException(beanName);
 1379 | 		}
 1380 | 
 1381 | 		if (hasBeanCreationStarted()) {
 1382 | 			// Cannot modify startup-time collection elements anymore (for stable iteration)
 1383 | 			synchronized (this.beanDefinitionMap) {
 1384 | 				List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
 1385 | 				updatedDefinitions.remove(beanName);
 1386 | 				this.beanDefinitionNames = updatedDefinitions;
 1387 | 			}
 1388 | 		}
 1389 | 		else {
 1390 | 			// Still in startup registration phase
 1391 | 			this.beanDefinitionNames.remove(beanName);
 1392 | 		}
 1393 | 		this.frozenBeanDefinitionNames = null;
 1394 | 
 1395 | 		resetBeanDefinition(beanName);
 1396 | 	}
 1397 | 
 1398 | 	/**
 1399 | 	 * Reset all bean definition caches for the given bean,
 1400 | 	 * including the caches of beans that are derived from it.
 1401 | 	 * <p>Called after an existing bean definition has been replaced or removed,
 1402 | 	 * triggering {@link #clearMergedBeanDefinition}, {@link #destroySingleton}
 1403 | 	 * and {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition} on the
 1404 | 	 * given bean and on all bean definitions that have the given bean as parent.
 1405 | 	 * @param beanName the name of the bean to reset
 1406 | 	 * @see #registerBeanDefinition
 1407 | 	 * @see #removeBeanDefinition
 1408 | 	 */
 1409 | 	protected void resetBeanDefinition(String beanName) {
 1410 | 		// Remove the merged bean definition for the given bean, if already created.
 1411 | 		clearMergedBeanDefinition(beanName);
 1412 | 
 1413 | 		// Remove corresponding bean from singleton cache, if any. Shouldn't usually
 1414 | 		// be necessary, rather just meant for overriding a context's default beans
 1415 | 		// (for example, the default StaticMessageSource in a StaticApplicationContext).
 1416 | 		destroySingleton(beanName);
 1417 | 
 1418 | 		// Remove a cached primary marker for the given bean.
 1419 | 		this.primaryBeanNamesWithType.remove(beanName);
 1420 | 
 1421 | 		// Notify all post-processors that the specified bean definition has been reset.
 1422 | 		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
 1423 | 			processor.resetBeanDefinition(beanName);
 1424 | 		}
 1425 | 
 1426 | 		// Reset all bean definitions that have the given bean as parent (recursively).
 1427 | 		for (String bdName : this.beanDefinitionNames) {
 1428 | 			if (!beanName.equals(bdName)) {
 1429 | 				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
 1430 | 				// Ensure bd is non-null due to potential concurrent modification of beanDefinitionMap.
 1431 | 				if (bd != null && beanName.equals(bd.getParentName())) {
 1432 | 					resetBeanDefinition(bdName);
 1433 | 				}
 1434 | 			}
 1435 | 		}
 1436 | 	}
 1437 | 
 1438 | 	/**
 1439 | 	 * This implementation returns {@code true} if bean definition overriding
 1440 | 	 * is generally allowed.
 1441 | 	 * @see #setAllowBeanDefinitionOverriding
 1442 | 	 */
 1443 | 	@Override
 1444 | 	public boolean isBeanDefinitionOverridable(String beanName) {
 1445 | 		return isAllowBeanDefinitionOverriding();
 1446 | 	}
 1447 | 
 1448 | 	/**
 1449 | 	 * Only allows alias overriding if bean definition overriding is allowed.
 1450 | 	 * @see #setAllowBeanDefinitionOverriding
 1451 | 	 */
 1452 | 	@Override
 1453 | 	protected boolean allowAliasOverriding() {
 1454 | 		return isAllowBeanDefinitionOverriding();
 1455 | 	}
 1456 | 
 1457 | 	/**
 1458 | 	 * Also checks for an alias overriding a bean definition of the same name.
 1459 | 	 */
 1460 | 	@Override
 1461 | 	protected void checkForAliasCircle(String name, String alias) {
 1462 | 		super.checkForAliasCircle(name, alias);
 1463 | 		if (!isBeanDefinitionOverridable(alias) && containsBeanDefinition(alias)) {
 1464 | 			throw new IllegalStateException("Cannot register alias '" + alias +
 1465 | 					"' for name '" + name + "': Alias would override bean definition '" + alias + "'");
 1466 | 		}
 1467 | 	}
 1468 | 
 1469 | 	@Override
 1470 | 	protected void addSingleton(String beanName, Object singletonObject) {
 1471 | 		super.addSingleton(beanName, singletonObject);
 1472 | 
 1473 | 		Predicate<Object> filter = (beanType -> beanType != Object.class &&
 1474 | 				((beanType instanceof Class<?> clazz && clazz.isInstance(singletonObject)) ||
 1475 | 						(beanType instanceof ResolvableType resolvable && resolvable.isInstance(singletonObject))));
 1476 | 		this.allBeanNamesByType.keySet().removeIf(filter);
 1477 | 		this.singletonBeanNamesByType.keySet().removeIf(filter);
 1478 | 
 1479 | 		if (this.primaryBeanNamesWithType.containsKey(beanName) && singletonObject.getClass() != NullBean.class) {
 1480 | 			Class<?> beanType = (singletonObject instanceof FactoryBean<?> fb ?
 1481 | 					getTypeForFactoryBean(fb) : singletonObject.getClass());
 1482 | 			if (beanType != null) {
 1483 | 				this.primaryBeanNamesWithType.put(beanName, beanType);
 1484 | 			}
 1485 | 		}
 1486 | 	}
 1487 | 
 1488 | 	@Override
 1489 | 	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
 1490 | 		super.registerSingleton(beanName, singletonObject);
 1491 | 
 1492 | 		updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
 1493 | 		this.allBeanNamesByType.remove(Object.class);
 1494 | 		this.singletonBeanNamesByType.remove(Object.class);
 1495 | 	}
 1496 | 
 1497 | 	@Override
 1498 | 	public void destroySingletons() {
 1499 | 		super.destroySingletons();
 1500 | 		updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
 1501 | 		clearByTypeCache();
 1502 | 	}
 1503 | 
 1504 | 	@Override
 1505 | 	public void destroySingleton(String beanName) {
 1506 | 		super.destroySingleton(beanName);
 1507 | 		removeManualSingletonName(beanName);
 1508 | 		clearByTypeCache();
 1509 | 	}
 1510 | 
 1511 | 	private void removeManualSingletonName(String beanName) {
 1512 | 		updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
 1513 | 	}
 1514 | 
 1515 | 	/**
 1516 | 	 * Update the factory's internal set of manual singleton names.
 1517 | 	 * @param action the modification action
 1518 | 	 * @param condition a precondition for the modification action
 1519 | 	 * (if this condition does not apply, the action can be skipped)
 1520 | 	 */
 1521 | 	private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
 1522 | 		if (hasBeanCreationStarted()) {
 1523 | 			// Cannot modify startup-time collection elements anymore (for stable iteration)
 1524 | 			synchronized (this.beanDefinitionMap) {
 1525 | 				if (condition.test(this.manualSingletonNames)) {
 1526 | 					Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
 1527 | 					action.accept(updatedSingletons);
 1528 | 					this.manualSingletonNames = updatedSingletons;
 1529 | 				}
 1530 | 			}
 1531 | 		}
 1532 | 		else {
 1533 | 			// Still in startup registration phase
 1534 | 			if (condition.test(this.manualSingletonNames)) {
 1535 | 				action.accept(this.manualSingletonNames);
 1536 | 			}
 1537 | 		}
 1538 | 	}
 1539 | 
 1540 | 	/**
 1541 | 	 * Remove any assumptions about by-type mappings.
 1542 | 	 */
 1543 | 	private void clearByTypeCache() {
 1544 | 		this.allBeanNamesByType.clear();
 1545 | 		this.singletonBeanNamesByType.clear();
 1546 | 	}
 1547 | 
 1548 | 
 1549 | 	//---------------------------------------------------------------------
 1550 | 	// Dependency resolution functionality
 1551 | 	//---------------------------------------------------------------------
 1552 | 
 1553 | 	@Override
 1554 | 	public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
 1555 | 		Assert.notNull(requiredType, "Required type must not be null");
 1556 | 		NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);
 1557 | 		if (namedBean != null) {
 1558 | 			return namedBean;
 1559 | 		}
 1560 | 		BeanFactory parent = getParentBeanFactory();
 1561 | 		if (parent instanceof AutowireCapableBeanFactory acbf) {
 1562 | 			return acbf.resolveNamedBean(requiredType);
 1563 | 		}
 1564 | 		throw new NoSuchBeanDefinitionException(requiredType);
 1565 | 	}
 1566 | 
 1567 | 	@SuppressWarnings("unchecked")
 1568 | 	private <T> @Nullable NamedBeanHolder<T> resolveNamedBean(
 1569 | 			ResolvableType requiredType, @Nullable Object @Nullable [] args, boolean nonUniqueAsNull) throws BeansException {
 1570 | 
 1571 | 		Assert.notNull(requiredType, "Required type must not be null");
 1572 | 		String[] candidateNames = getBeanNamesForType(requiredType);
 1573 | 
 1574 | 		if (candidateNames.length > 1) {
 1575 | 			List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
 1576 | 			for (String beanName : candidateNames) {
 1577 | 				if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
 1578 | 					autowireCandidates.add(beanName);
 1579 | 				}
 1580 | 			}
 1581 | 			if (!autowireCandidates.isEmpty()) {
 1582 | 				candidateNames = StringUtils.toStringArray(autowireCandidates);
 1583 | 			}
 1584 | 		}
 1585 | 
 1586 | 		if (candidateNames.length == 1) {
 1587 | 			return resolveNamedBean(candidateNames[0], requiredType, args);
 1588 | 		}
 1589 | 		else if (candidateNames.length > 1) {
 1590 | 			Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(candidateNames.length);
 1591 | 			for (String beanName : candidateNames) {
 1592 | 				if (containsSingleton(beanName) && args == null) {
 1593 | 					Object beanInstance = resolveBean(beanName, requiredType);
 1594 | 					candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
 1595 | 				}
 1596 | 				else {
 1597 | 					candidates.put(beanName, getType(beanName));
 1598 | 				}
 1599 | 			}
 1600 | 			String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
 1601 | 			if (candidateName == null) {
 1602 | 				candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
 1603 | 			}
 1604 | 			if (candidateName == null) {
 1605 | 				candidateName = determineDefaultCandidate(candidates);
 1606 | 			}
 1607 | 			if (candidateName != null) {
 1608 | 				Object beanInstance = candidates.get(candidateName);
 1609 | 				if (beanInstance == null) {
 1610 | 					return null;
 1611 | 				}
 1612 | 				if (beanInstance instanceof Class) {
 1613 | 					return resolveNamedBean(candidateName, requiredType, args);
 1614 | 				}
 1615 | 				return new NamedBeanHolder<>(candidateName, (T) beanInstance);
 1616 | 			}
 1617 | 			if (!nonUniqueAsNull) {
 1618 | 				throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
 1619 | 			}
 1620 | 		}
 1621 | 
 1622 | 		return null;
 1623 | 	}
 1624 | 
 1625 | 	private <T> @Nullable NamedBeanHolder<T> resolveNamedBean(
 1626 | 			String beanName, ResolvableType requiredType, @Nullable Object @Nullable [] args) throws BeansException {
 1627 | 
 1628 | 		Object bean = (args != null ? getBean(beanName, args) : resolveBean(beanName, requiredType));
 1629 | 		if (bean instanceof NullBean) {
 1630 | 			return null;
 1631 | 		}
 1632 | 		return new NamedBeanHolder<>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
 1633 | 	}
 1634 | 
 1635 | 	@Override
 1636 | 	public @Nullable Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
 1637 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
 1638 | 
 1639 | 		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
 1640 | 		if (Optional.class == descriptor.getDependencyType()) {
 1641 | 			return createOptionalDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
 1642 | 		}
 1643 | 		else if (ObjectFactory.class == descriptor.getDependencyType() ||
 1644 | 				ObjectProvider.class == descriptor.getDependencyType()) {
 1645 | 			return new DependencyObjectProvider(descriptor, requestingBeanName);
 1646 | 		}
 1647 | 		else if (jakartaInjectProviderClass == descriptor.getDependencyType()) {
 1648 | 			return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
 1649 | 		}
 1650 | 		else if (descriptor.supportsLazyResolution()) {
 1651 | 			Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
 1652 | 					descriptor, requestingBeanName);
 1653 | 			if (result != null) {
 1654 | 				return result;
 1655 | 			}
 1656 | 		}
 1657 | 		return doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
 1658 | 	}
 1659 | 
 1660 | 	@SuppressWarnings("NullAway")  // Dataflow analysis limitation
 1661 | 	public @Nullable Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
 1662 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
 1663 | 
 1664 | 		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
 1665 | 		try {
 1666 | 			// Step 1: pre-resolved shortcut for single bean match, for example, from @Autowired
 1667 | 			Object shortcut = descriptor.resolveShortcut(this);
 1668 | 			if (shortcut != null) {
 1669 | 				return shortcut;
 1670 | 			}
 1671 | 
 1672 | 			Class<?> type = descriptor.getDependencyType();
 1673 | 
 1674 | 			// Step 2: pre-defined value or expression, for example, from @Value
 1675 | 			Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
 1676 | 			if (value != null) {
 1677 | 				if (value instanceof String strValue) {
 1678 | 					String resolvedValue = resolveEmbeddedValue(strValue);
 1679 | 					BeanDefinition bd = (beanName != null && containsBean(beanName) ?
 1680 | 							getMergedBeanDefinition(beanName) : null);
 1681 | 					value = evaluateBeanDefinitionString(resolvedValue, bd);
 1682 | 				}
 1683 | 				TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
 1684 | 				try {
 1685 | 					return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
 1686 | 				}
 1687 | 				catch (UnsupportedOperationException ex) {
 1688 | 					// A custom TypeConverter which does not support TypeDescriptor resolution...
 1689 | 					return (descriptor.getField() != null ?
 1690 | 							converter.convertIfNecessary(value, type, descriptor.getField()) :
 1691 | 							converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
 1692 | 				}
 1693 | 			}
 1694 | 
 1695 | 			// Step 3: shortcut for declared dependency name or qualifier-suggested name matching target bean name
 1696 | 			if (descriptor.usesStandardBeanLookup()) {
 1697 | 				String dependencyName = descriptor.getDependencyName();
 1698 | 				if (dependencyName == null || !containsBean(dependencyName)) {
 1699 | 					String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
 1700 | 					dependencyName = (suggestedName != null && containsBean(suggestedName) ? suggestedName : null);
 1701 | 				}
 1702 | 				if (dependencyName != null) {
 1703 | 					dependencyName = canonicalName(dependencyName);  // dependency name can be alias of target name
 1704 | 					if (isTypeMatch(dependencyName, type) && isAutowireCandidate(dependencyName, descriptor) &&
 1705 | 							!isFallback(dependencyName) && !hasPrimaryConflict(dependencyName, type) &&
 1706 | 							!isSelfReference(beanName, dependencyName)) {
 1707 | 						if (autowiredBeanNames != null) {
 1708 | 							autowiredBeanNames.add(dependencyName);
 1709 | 						}
 1710 | 						Object dependencyBean = resolveBean(dependencyName, descriptor.getResolvableType());
 1711 | 						return resolveInstance(dependencyBean, descriptor, type, dependencyName);
 1712 | 					}
 1713 | 				}
 1714 | 			}
 1715 | 
 1716 | 			// Step 4a: multiple beans as stream / array / standard collection / plain map
 1717 | 			Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
 1718 | 			if (multipleBeans != null) {
 1719 | 				return multipleBeans;
 1720 | 			}
 1721 | 			// Step 4b: direct bean matches, possibly direct beans of type Collection / Map
 1722 | 			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
 1723 | 			if (matchingBeans.isEmpty()) {
 1724 | 				// Step 4c (fallback): custom Collection / Map declarations for collecting multiple beans
 1725 | 				multipleBeans = resolveMultipleBeansFallback(descriptor, beanName, autowiredBeanNames, typeConverter);
 1726 | 				if (multipleBeans != null) {
 1727 | 					return multipleBeans;
 1728 | 				}
 1729 | 				// Raise exception if nothing found for required injection point
 1730 | 				if (isRequired(descriptor)) {
 1731 | 					raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
 1732 | 				}
 1733 | 				return null;
 1734 | 			}
 1735 | 
 1736 | 			String autowiredBeanName;
 1737 | 			Object instanceCandidate;
 1738 | 
 1739 | 			// Step 5: determine single candidate
 1740 | 			if (matchingBeans.size() > 1) {
 1741 | 				autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
 1742 | 				if (autowiredBeanName == null) {
 1743 | 					if (isRequired(descriptor) || !indicatesArrayCollectionOrMap(type)) {
 1744 | 						// Raise exception if no clear match found for required injection point
 1745 | 						return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
 1746 | 					}
 1747 | 					else {
 1748 | 						// In case of an optional Collection/Map, silently ignore a non-unique case:
 1749 | 						// possibly it was meant to be an empty collection of multiple regular beans
 1750 | 						// (before 4.3 in particular when we didn't even look for collection beans).
 1751 | 						return null;
 1752 | 					}
 1753 | 				}
 1754 | 				instanceCandidate = matchingBeans.get(autowiredBeanName);
 1755 | 			}
 1756 | 			else {
 1757 | 				// We have exactly one match.
 1758 | 				Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
 1759 | 				autowiredBeanName = entry.getKey();
 1760 | 				instanceCandidate = entry.getValue();
 1761 | 			}
 1762 | 
 1763 | 			// Step 6: validate single result
 1764 | 			if (autowiredBeanNames != null) {
 1765 | 				autowiredBeanNames.add(autowiredBeanName);
 1766 | 			}
 1767 | 			if (instanceCandidate instanceof Class) {
 1768 | 				instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
 1769 | 			}
 1770 | 			return resolveInstance(instanceCandidate, descriptor, type, autowiredBeanName);
 1771 | 		}
 1772 | 		finally {
 1773 | 			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
 1774 | 		}
 1775 | 	}
 1776 | 
 1777 | 	private @Nullable Object resolveInstance(Object candidate, DependencyDescriptor descriptor, Class<?> type, String name) {
 1778 | 		Object result = candidate;
 1779 | 		if (result instanceof NullBean) {
 1780 | 			// Raise exception if null encountered for required injection point
 1781 | 			if (isRequired(descriptor)) {
 1782 | 				raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
 1783 | 			}
 1784 | 			result = null;
 1785 | 		}
 1786 | 		if (!ClassUtils.isAssignableValue(type, result)) {
 1787 | 			throw new BeanNotOfRequiredTypeException(name, type, candidate.getClass());
 1788 | 		}
 1789 | 		return result;
 1790 | 	}
 1791 | 
 1792 | 	private @Nullable Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
 1793 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
 1794 | 
 1795 | 		Class<?> type = descriptor.getDependencyType();
 1796 | 
 1797 | 		if (descriptor instanceof StreamDependencyDescriptor streamDependencyDescriptor) {
 1798 | 			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
 1799 | 			if (autowiredBeanNames != null) {
 1800 | 				autowiredBeanNames.addAll(matchingBeans.keySet());
 1801 | 			}
 1802 | 			Stream<Object> stream = matchingBeans.keySet().stream()
 1803 | 					.map(name -> descriptor.resolveCandidate(name, type, this))
 1804 | 					.filter(bean -> !(bean instanceof NullBean));
 1805 | 			if (streamDependencyDescriptor.isOrdered()) {
 1806 | 				stream = stream.sorted(adaptOrderComparator(matchingBeans));
 1807 | 			}
 1808 | 			return stream;
 1809 | 		}
 1810 | 		else if (type.isArray()) {
 1811 | 			Class<?> componentType = type.componentType();
 1812 | 			ResolvableType resolvableType = descriptor.getResolvableType();
 1813 | 			Class<?> resolvedArrayType = resolvableType.resolve(type);
 1814 | 			if (resolvedArrayType != type) {
 1815 | 				componentType = resolvableType.getComponentType().resolve();
 1816 | 			}
 1817 | 			if (componentType == null) {
 1818 | 				return null;
 1819 | 			}
 1820 | 			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
 1821 | 					new MultiElementDescriptor(descriptor));
 1822 | 			if (matchingBeans.isEmpty()) {
 1823 | 				return null;
 1824 | 			}
 1825 | 			if (autowiredBeanNames != null) {
 1826 | 				autowiredBeanNames.addAll(matchingBeans.keySet());
 1827 | 			}
 1828 | 			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
 1829 | 			Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
 1830 | 			if (result instanceof Object[] array && array.length > 1) {
 1831 | 				Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
 1832 | 				if (comparator != null) {
 1833 | 					Arrays.sort(array, comparator);
 1834 | 				}
 1835 | 			}
 1836 | 			return result;
 1837 | 		}
 1838 | 		else if (Collection.class == type || Set.class == type || List.class == type) {
 1839 | 			return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
 1840 | 		}
 1841 | 		else if (Map.class == type) {
 1842 | 			return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
 1843 | 		}
 1844 | 		return null;
 1845 | 	}
 1846 | 
 1847 | 
 1848 | 	private @Nullable Object resolveMultipleBeansFallback(DependencyDescriptor descriptor, @Nullable String beanName,
 1849 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
 1850 | 
 1851 | 		Class<?> type = descriptor.getDependencyType();
 1852 | 
 1853 | 		if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
 1854 | 			return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
 1855 | 		}
 1856 | 		else if (Map.class.isAssignableFrom(type) && type.isInterface()) {
 1857 | 			return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
 1858 | 		}
 1859 | 		return null;
 1860 | 	}
 1861 | 
 1862 | 	private @Nullable Object resolveMultipleBeanCollection(DependencyDescriptor descriptor, @Nullable String beanName,
 1863 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
 1864 | 
 1865 | 		Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
 1866 | 		if (elementType == null) {
 1867 | 			return null;
 1868 | 		}
 1869 | 		Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
 1870 | 				new MultiElementDescriptor(descriptor));
 1871 | 		if (matchingBeans.isEmpty()) {
 1872 | 			return null;
 1873 | 		}
 1874 | 		if (autowiredBeanNames != null) {
 1875 | 			autowiredBeanNames.addAll(matchingBeans.keySet());
 1876 | 		}
 1877 | 		TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
 1878 | 		Object result = converter.convertIfNecessary(matchingBeans.values(), descriptor.getDependencyType());
 1879 | 		if (result instanceof List<?> list && list.size() > 1) {
 1880 | 			Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
 1881 | 			if (comparator != null) {
 1882 | 				list.sort(comparator);
 1883 | 			}
 1884 | 		}
 1885 | 		return result;
 1886 | 	}
 1887 | 
 1888 | 	private @Nullable Object resolveMultipleBeanMap(DependencyDescriptor descriptor, @Nullable String beanName,
 1889 | 			@Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
 1890 | 
 1891 | 		ResolvableType mapType = descriptor.getResolvableType().asMap();
 1892 | 		Class<?> keyType = mapType.resolveGeneric(0);
 1893 | 		if (String.class != keyType) {
 1894 | 			return null;
 1895 | 		}
 1896 | 		Class<?> valueType = mapType.resolveGeneric(1);
 1897 | 		if (valueType == null) {
 1898 | 			return null;
 1899 | 		}
 1900 | 		Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
 1901 | 				new MultiElementDescriptor(descriptor));
 1902 | 		if (matchingBeans.isEmpty()) {
 1903 | 			return null;
 1904 | 		}
 1905 | 		if (autowiredBeanNames != null) {
 1906 | 			autowiredBeanNames.addAll(matchingBeans.keySet());
 1907 | 		}
 1908 | 		TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
 1909 | 		return converter.convertIfNecessary(matchingBeans, descriptor.getDependencyType());
 1910 | 	}
 1911 | 
 1912 | 	private boolean indicatesArrayCollectionOrMap(Class<?> type) {
 1913 | 		return (type.isArray() || (type.isInterface() &&
 1914 | 				(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
 1915 | 	}
 1916 | 
 1917 | 	private boolean isRequired(DependencyDescriptor descriptor) {
 1918 | 		return getAutowireCandidateResolver().isRequired(descriptor);
 1919 | 	}
 1920 | 
 1921 | 	private @Nullable Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
 1922 | 		Comparator<Object> comparator = getDependencyComparator();
 1923 | 		if (comparator instanceof OrderComparator orderComparator) {
 1924 | 			return orderComparator.withSourceProvider(
 1925 | 					createFactoryAwareOrderSourceProvider(matchingBeans));
 1926 | 		}
 1927 | 		else {
 1928 | 			return comparator;
 1929 | 		}
 1930 | 	}
 1931 | 
 1932 | 	private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
 1933 | 		Comparator<Object> dependencyComparator = getDependencyComparator();
 1934 | 		OrderComparator comparator = (dependencyComparator instanceof OrderComparator orderComparator ?
 1935 | 				orderComparator : OrderComparator.INSTANCE);
 1936 | 		return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
 1937 | 	}
 1938 | 
 1939 | 	private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
 1940 | 		IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
 1941 | 		beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
 1942 | 		return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
 1943 | 	}
 1944 | 
 1945 | 	/**
 1946 | 	 * Find bean instances that match the required type.
 1947 | 	 * Called during autowiring for the specified bean.
 1948 | 	 * @param beanName the name of the bean that is about to be wired
 1949 | 	 * @param requiredType the actual type of bean to look for
 1950 | 	 * (may be an array component type or collection element type)
 1951 | 	 * @param descriptor the descriptor of the dependency to resolve
 1952 | 	 * @return a Map of candidate names and candidate instances that match
 1953 | 	 * the required type (never {@code null})
 1954 | 	 * @throws BeansException in case of errors
 1955 | 	 * @see #autowireByType
 1956 | 	 * @see #autowireConstructor
 1957 | 	 */
 1958 | 	protected Map<String, Object> findAutowireCandidates(
 1959 | 			@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
 1960 | 
 1961 | 		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
 1962 | 				this, requiredType, true, descriptor.isEager());
 1963 | 		Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.length);
 1964 | 		for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
 1965 | 			Class<?> autowiringType = classObjectEntry.getKey();
 1966 | 			if (autowiringType.isAssignableFrom(requiredType)) {
 1967 | 				Object autowiringValue = classObjectEntry.getValue();
 1968 | 				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
 1969 | 				if (requiredType.isInstance(autowiringValue)) {
 1970 | 					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
 1971 | 					break;
 1972 | 				}
 1973 | 			}
 1974 | 		}
 1975 | 		for (String candidate : candidateNames) {
 1976 | 			if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
 1977 | 				addCandidateEntry(result, candidate, descriptor, requiredType);
 1978 | 			}
 1979 | 		}
 1980 | 		if (result.isEmpty()) {
 1981 | 			boolean multiple = indicatesArrayCollectionOrMap(requiredType);
 1982 | 			// Consider fallback matches if the first pass failed to find anything...
 1983 | 			DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
 1984 | 			for (String candidate : candidateNames) {
 1985 | 				if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
 1986 | 						(!multiple || matchesBeanName(candidate, descriptor.getDependencyName()) ||
 1987 | 								getAutowireCandidateResolver().hasQualifier(descriptor))) {
 1988 | 					addCandidateEntry(result, candidate, descriptor, requiredType);
 1989 | 				}
 1990 | 			}
 1991 | 			if (result.isEmpty() && !multiple) {
 1992 | 				// Consider self references as a final pass...
 1993 | 				// but in the case of a dependency collection, not the very same bean itself.
 1994 | 				for (String candidate : candidateNames) {
 1995 | 					if (isSelfReference(beanName, candidate) &&
 1996 | 							(!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
 1997 | 							isAutowireCandidate(candidate, fallbackDescriptor)) {
 1998 | 						addCandidateEntry(result, candidate, descriptor, requiredType);
 1999 | 					}
 2000 | 				}
 2001 | 			}
 2002 | 		}
 2003 | 		return result;
 2004 | 	}
 2005 | 
 2006 | 	/**
 2007 | 	 * Add an entry to the candidate map: a bean instance if available or just the resolved
 2008 | 	 * type, preventing early bean initialization ahead of primary candidate selection.
 2009 | 	 */
 2010 | 	private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
 2011 | 			DependencyDescriptor descriptor, Class<?> requiredType) {
 2012 | 
 2013 | 		if (descriptor instanceof MultiElementDescriptor) {
 2014 | 			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
 2015 | 			if (!(beanInstance instanceof NullBean)) {
 2016 | 				candidates.put(candidateName, beanInstance);
 2017 | 			}
 2018 | 		}
 2019 | 		else if (containsSingleton(candidateName) ||
 2020 | 				(descriptor instanceof StreamDependencyDescriptor streamDescriptor && streamDescriptor.isOrdered())) {
 2021 | 			Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
 2022 | 			candidates.put(candidateName, beanInstance);
 2023 | 		}
 2024 | 		else {
 2025 | 			candidates.put(candidateName, getType(candidateName));
 2026 | 		}
 2027 | 	}
 2028 | 
 2029 | 	/**
 2030 | 	 * Determine the autowire candidate in the given set of beans.
 2031 | 	 * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
 2032 | 	 * @param candidates a Map of candidate names and candidate instances
 2033 | 	 * that match the required type, as returned by {@link #findAutowireCandidates}
 2034 | 	 * @param descriptor the target dependency to match against
 2035 | 	 * @return the name of the autowire candidate, or {@code null} if none found
 2036 | 	 */
 2037 | 	protected @Nullable String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
 2038 | 		Class<?> requiredType = descriptor.getDependencyType();
 2039 | 		// Step 1: check primary candidate
 2040 | 		String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
 2041 | 		if (primaryCandidate != null) {
 2042 | 			return primaryCandidate;
 2043 | 		}
 2044 | 		// Step 2a: match bean name against declared dependency name
 2045 | 		String dependencyName = descriptor.getDependencyName();
 2046 | 		if (dependencyName != null) {
 2047 | 			for (String beanName : candidates.keySet()) {
 2048 | 				if (matchesBeanName(beanName, dependencyName)) {
 2049 | 					return beanName;
 2050 | 				}
 2051 | 			}
 2052 | 		}
 2053 | 		// Step 2b: match bean name against qualifier-suggested name
 2054 | 		String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
 2055 | 		if (suggestedName != null) {
 2056 | 			for (String beanName : candidates.keySet()) {
 2057 | 				if (matchesBeanName(beanName, suggestedName)) {
 2058 | 					return beanName;
 2059 | 				}
 2060 | 			}
 2061 | 		}
 2062 | 		// Step 3: check highest priority candidate
 2063 | 		String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
 2064 | 		if (priorityCandidate != null) {
 2065 | 			return priorityCandidate;
 2066 | 		}
 2067 | 		// Step 4: pick unique default-candidate
 2068 | 		String defaultCandidate = determineDefaultCandidate(candidates);
 2069 | 		if (defaultCandidate != null) {
 2070 | 			return defaultCandidate;
 2071 | 		}
 2072 | 		// Step 5: pick directly registered dependency
 2073 | 		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
 2074 | 			String candidateName = entry.getKey();
 2075 | 			Object beanInstance = entry.getValue();
 2076 | 			if (beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) {
 2077 | 				return candidateName;
 2078 | 			}
 2079 | 		}
 2080 | 		return null;
 2081 | 	}
 2082 | 
 2083 | 	/**
 2084 | 	 * Determine the primary candidate in the given set of beans.
 2085 | 	 * @param candidates a Map of candidate names and candidate instances
 2086 | 	 * (or candidate classes if not created yet) that match the required type
 2087 | 	 * @param requiredType the target dependency type to match against
 2088 | 	 * @return the name of the primary candidate, or {@code null} if none found
 2089 | 	 * @see #isPrimary(String, Object)
 2090 | 	 */
 2091 | 	protected @Nullable String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
 2092 | 		String primaryBeanName = null;
 2093 | 		// First pass: identify unique primary candidate
 2094 | 		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
 2095 | 			String candidateBeanName = entry.getKey();
 2096 | 			Object beanInstance = entry.getValue();
 2097 | 			if (isPrimary(candidateBeanName, beanInstance)) {
 2098 | 				if (primaryBeanName != null) {
 2099 | 					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
 2100 | 					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
 2101 | 					if (candidateLocal == primaryLocal) {
 2102 | 						String message = "more than one 'primary' bean found among candidates: " + candidates.keySet();
 2103 | 						logger.trace(message);
 2104 | 						throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(), message);
 2105 | 					}
 2106 | 					else if (candidateLocal) {
 2107 | 						primaryBeanName = candidateBeanName;
 2108 | 					}
 2109 | 				}
 2110 | 				else {
 2111 | 					primaryBeanName = candidateBeanName;
 2112 | 				}
 2113 | 			}
 2114 | 		}
 2115 | 		// Second pass: identify unique non-fallback candidate
 2116 | 		if (primaryBeanName == null) {
 2117 | 			for (String candidateBeanName : candidates.keySet()) {
 2118 | 				if (!isFallback(candidateBeanName)) {
 2119 | 					if (primaryBeanName != null) {
 2120 | 						return null;
 2121 | 					}
 2122 | 					primaryBeanName = candidateBeanName;
 2123 | 				}
 2124 | 			}
 2125 | 		}
 2126 | 		return primaryBeanName;
 2127 | 	}
 2128 | 
 2129 | 	/**
 2130 | 	 * Determine the candidate with the highest priority in the given set of beans.
 2131 | 	 * <p>Based on {@code @jakarta.annotation.Priority}. As defined by the related
 2132 | 	 * {@link org.springframework.core.Ordered} interface, the lowest value has
 2133 | 	 * the highest priority.
 2134 | 	 * @param candidates a Map of candidate names and candidate instances
 2135 | 	 * (or candidate classes if not created yet) that match the required type
 2136 | 	 * @param requiredType the target dependency type to match against
 2137 | 	 * @return the name of the candidate with the highest priority,
 2138 | 	 * or {@code null} if none found
 2139 | 	 * @throws NoUniqueBeanDefinitionException if multiple beans are detected with
 2140 | 	 * the same highest priority value
 2141 | 	 * @see #getPriority(Object)
 2142 | 	 */
 2143 | 	protected @Nullable String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
 2144 | 		String highestPriorityBeanName = null;
 2145 | 		Integer highestPriority = null;
 2146 | 		boolean highestPriorityConflictDetected = false;
 2147 | 		for (Map.Entry<String, Object> entry : candidates.entrySet()) {
 2148 | 			String candidateBeanName = entry.getKey();
 2149 | 			Object beanInstance = entry.getValue();
 2150 | 			if (beanInstance != null) {
 2151 | 				Integer candidatePriority = getPriority(beanInstance);
 2152 | 				if (candidatePriority != null) {
 2153 | 					if (highestPriority != null) {
 2154 | 						if (candidatePriority.equals(highestPriority)) {
 2155 | 							highestPriorityConflictDetected = true;
 2156 | 						}
 2157 | 						else if (candidatePriority < highestPriority) {
 2158 | 							highestPriorityBeanName = candidateBeanName;
 2159 | 							highestPriority = candidatePriority;
 2160 | 							highestPriorityConflictDetected = false;
 2161 | 						}
 2162 | 					}
 2163 | 					else {
 2164 | 						highestPriorityBeanName = candidateBeanName;
 2165 | 						highestPriority = candidatePriority;
 2166 | 					}
 2167 | 				}
 2168 | 			}
 2169 | 		}
 2170 | 
 2171 | 		if (highestPriorityConflictDetected) {
 2172 | 			throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
 2173 | 					"Multiple beans found with the same highest priority (" + highestPriority +
 2174 | 					") among candidates: " + candidates.keySet());
 2175 | 
 2176 | 		}
 2177 | 		return highestPriorityBeanName;
 2178 | 	}
 2179 | 
 2180 | 	/**
 2181 | 	 * Return whether the bean definition for the given bean name has been
 2182 | 	 * marked as a primary bean.
 2183 | 	 * @param beanName the name of the bean
 2184 | 	 * @param beanInstance the corresponding bean instance (can be {@code null})
 2185 | 	 * @return whether the given bean qualifies as primary
 2186 | 	 */
 2187 | 	protected boolean isPrimary(String beanName, Object beanInstance) {
 2188 | 		String transformedBeanName = transformedBeanName(beanName);
 2189 | 		if (containsBeanDefinition(transformedBeanName)) {
 2190 | 			return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
 2191 | 		}
 2192 | 		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
 2193 | 				parent.isPrimary(transformedBeanName, beanInstance));
 2194 | 	}
 2195 | 
 2196 | 	/**
 2197 | 	 * Return whether the bean definition for the given bean name has been
 2198 | 	 * marked as a fallback bean.
 2199 | 	 * @param beanName the name of the bean
 2200 | 	 * @since 6.2
 2201 | 	 */
 2202 | 	private boolean isFallback(String beanName) {
 2203 | 		String transformedBeanName = transformedBeanName(beanName);
 2204 | 		if (containsBeanDefinition(transformedBeanName)) {
 2205 | 			return getMergedLocalBeanDefinition(transformedBeanName).isFallback();
 2206 | 		}
 2207 | 		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
 2208 | 				parent.isFallback(transformedBeanName));
 2209 | 	}
 2210 | 
 2211 | 	/**
 2212 | 	 * Return the priority assigned for the given bean instance by
 2213 | 	 * the {@code jakarta.annotation.Priority} annotation.
 2214 | 	 * <p>The default implementation delegates to the specified
 2215 | 	 * {@link #setDependencyComparator dependency comparator}, checking its
 2216 | 	 * {@link OrderComparator#getPriority method} if it is an extension of
 2217 | 	 * Spring's common {@link OrderComparator} - typically, an
 2218 | 	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}.
 2219 | 	 * If no such comparator is present, this implementation returns {@code null}.
 2220 | 	 * @param beanInstance the bean instance to check (can be {@code null})
 2221 | 	 * @return the priority assigned to that bean or {@code null} if none is set
 2222 | 	 */
 2223 | 	protected @Nullable Integer getPriority(Object beanInstance) {
 2224 | 		Comparator<Object> comparator = getDependencyComparator();
 2225 | 		if (comparator instanceof OrderComparator orderComparator) {
 2226 | 			return orderComparator.getPriority(beanInstance);
 2227 | 		}
 2228 | 		return null;
 2229 | 	}
 2230 | 
 2231 | 	/**
 2232 | 	 * Return a unique "default-candidate" among remaining non-default candidates.
 2233 | 	 * @param candidates a Map of candidate names and candidate instances
 2234 | 	 * (or candidate classes if not created yet) that match the required type
 2235 | 	 * @return the name of the default candidate, or {@code null} if none found
 2236 | 	 * @since 6.2.4
 2237 | 	 * @see AbstractBeanDefinition#isDefaultCandidate()
 2238 | 	 */
 2239 | 	@Nullable
 2240 | 	private String determineDefaultCandidate(Map<String, Object> candidates) {
 2241 | 		String defaultBeanName = null;
 2242 | 		for (String candidateBeanName : candidates.keySet()) {
 2243 | 			if (AutowireUtils.isDefaultCandidate(this, candidateBeanName)) {
 2244 | 				if (defaultBeanName != null) {
 2245 | 					return null;
 2246 | 				}
 2247 | 				defaultBeanName = candidateBeanName;
 2248 | 			}
 2249 | 		}
 2250 | 		return defaultBeanName;
 2251 | 	}
 2252 | 
 2253 | 	/**
 2254 | 	 * Determine whether the given dependency name matches the bean name or the aliases
 2255 | 	 * stored in this bean definition.
 2256 | 	 */
 2257 | 	protected boolean matchesBeanName(String beanName, @Nullable String dependencyName) {
 2258 | 		return (dependencyName != null &&
 2259 | 				(dependencyName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), dependencyName)));
 2260 | 	}
 2261 | 
 2262 | 	/**
 2263 | 	 * Determine whether the given beanName/candidateName pair indicates a self reference,
 2264 | 	 * i.e. whether the candidate points back to the original bean or to a factory method
 2265 | 	 * on the original bean.
 2266 | 	 */
 2267 | 	@Contract("null, _ -> false; _, null -> false;")
 2268 | 	private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
 2269 | 		return (beanName != null && candidateName != null &&
 2270 | 				(beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
 2271 | 						beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
 2272 | 	}
 2273 | 
 2274 | 	/**
 2275 | 	 * Determine whether there is a primary bean registered for the given dependency type,
 2276 | 	 * not matching the given bean name.
 2277 | 	 */
 2278 | 	private boolean hasPrimaryConflict(String beanName, Class<?> dependencyType) {
 2279 | 		for (Map.Entry<String, Class<?>> candidate : this.primaryBeanNamesWithType.entrySet()) {
 2280 | 			String candidateName = candidate.getKey();
 2281 | 			Class<?> candidateType = candidate.getValue();
 2282 | 			if (!candidateName.equals(beanName) && (candidateType != Void.class ?
 2283 | 					dependencyType.isAssignableFrom(candidateType) :  // cached singleton class for primary bean
 2284 | 					isTypeMatch(candidateName, dependencyType))) {  // not instantiated yet or not a singleton
 2285 | 				return true;
 2286 | 			}
 2287 | 		}
 2288 | 		return (getParentBeanFactory() instanceof DefaultListableBeanFactory parent &&
 2289 | 				parent.hasPrimaryConflict(beanName, dependencyType));
 2290 | 	}
 2291 | 
 2292 | 	/**
 2293 | 	 * Raise a NoSuchBeanDefinitionException or BeanNotOfRequiredTypeException
 2294 | 	 * for an unresolvable dependency.
 2295 | 	 */
 2296 | 	private void raiseNoMatchingBeanFound(
 2297 | 			Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {
 2298 | 
 2299 | 		checkBeanNotOfRequiredType(type, descriptor);
 2300 | 
 2301 | 		throw new NoSuchBeanDefinitionException(resolvableType,
 2302 | 				"expected at least 1 bean which qualifies as autowire candidate. " +
 2303 | 				"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
 2304 | 	}
 2305 | 
 2306 | 	/**
 2307 | 	 * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
 2308 | 	 * i.e. if the target type of the bean would match but an exposed proxy doesn't.
 2309 | 	 */
 2310 | 	private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
 2311 | 		for (String beanName : this.beanDefinitionNames) {
 2312 | 			try {
 2313 | 				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
 2314 | 				Class<?> targetType = mbd.getTargetType();
 2315 | 				if (targetType != null && type.isAssignableFrom(targetType) &&
 2316 | 						isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
 2317 | 					// Probably a proxy interfering with target type match -> throw meaningful exception.
 2318 | 					Object beanInstance = getSingleton(beanName, false);
 2319 | 					Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
 2320 | 							beanInstance.getClass() : predictBeanType(beanName, mbd));
 2321 | 					if (beanType != null && !type.isAssignableFrom(beanType)) {
 2322 | 						throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
 2323 | 					}
 2324 | 				}
 2325 | 			}
 2326 | 			catch (NoSuchBeanDefinitionException ex) {
 2327 | 				// Bean definition got removed while we were iterating -> ignore.
 2328 | 			}
 2329 | 		}
 2330 | 
 2331 | 		if (getParentBeanFactory() instanceof DefaultListableBeanFactory parent) {
 2332 | 			parent.checkBeanNotOfRequiredType(type, descriptor);
 2333 | 		}
 2334 | 	}
 2335 | 
 2336 | 	/**
 2337 | 	 * Create an {@link Optional} wrapper for the specified dependency.
 2338 | 	 */
 2339 | 	private Optional<?> createOptionalDependency(DependencyDescriptor descriptor, @Nullable String beanName,
 2340 | 			@Nullable Set<String> autowiredBeanNames, @Nullable Object @Nullable [] args) {
 2341 | 
 2342 | 		DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
 2343 | 			@Override
 2344 | 			public boolean isRequired() {
 2345 | 				return false;
 2346 | 			}
 2347 | 			@Override
 2348 | 			public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
 2349 | 				return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
 2350 | 						super.resolveCandidate(beanName, requiredType, beanFactory));
 2351 | 			}
 2352 | 			@Override
 2353 | 			public boolean usesStandardBeanLookup() {
 2354 | 				return ObjectUtils.isEmpty(args);
 2355 | 			}
 2356 | 		};
 2357 | 		Object result = doResolveDependency(descriptorToUse, beanName, autowiredBeanNames, null);
 2358 | 		return (result instanceof Optional<?> optional ? optional : Optional.ofNullable(result));
 2359 | 	}
 2360 | 
 2361 | 	/**
 2362 | 	 * Public method to determine the applicable order value for a given bean.
 2363 | 	 * <p>This variant implicitly obtains a corresponding bean instance from this factory.
 2364 | 	 * @param beanName the name of the bean
 2365 | 	 * @return the corresponding order value (default is {@link Ordered#LOWEST_PRECEDENCE})
 2366 | 	 * @since 7.0
 2367 | 	 * @see #getOrder(String, Object)
 2368 | 	 */
 2369 | 	public int getOrder(String beanName) {
 2370 | 		return getOrder(beanName, getBean(beanName));
 2371 | 	}
 2372 | 
 2373 | 	/**
 2374 | 	 * Public method to determine the applicable order value for a given bean.
 2375 | 	 * @param beanName the name of the bean
 2376 | 	 * @param beanInstance the bean instance to check
 2377 | 	 * @return the corresponding order value (default is {@link Ordered#LOWEST_PRECEDENCE})
 2378 | 	 * @since 7.0
 2379 | 	 * @see #getOrder(String)
 2380 | 	 */
 2381 | 	public int getOrder(String beanName, Object beanInstance) {
 2382 | 		OrderComparator comparator = (getDependencyComparator() instanceof OrderComparator orderComparator ?
 2383 | 				orderComparator : OrderComparator.INSTANCE);
 2384 | 		return comparator.getOrder(beanInstance,
 2385 | 				new FactoryAwareOrderSourceProvider(Collections.singletonMap(beanInstance, beanName)));
 2386 | 	}
 2387 | 
 2388 | 
 2389 | 	@Override
 2390 | 	public String toString() {
 2391 | 		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
 2392 | 		sb.append(": defining beans [");
 2393 | 		sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
 2394 | 		sb.append("]; ");
 2395 | 		BeanFactory parent = getParentBeanFactory();
 2396 | 		if (parent == null) {
 2397 | 			sb.append("root of factory hierarchy");
 2398 | 		}
 2399 | 		else {
 2400 | 			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
 2401 | 		}
 2402 | 		return sb.toString();
 2403 | 	}
 2404 | 
 2405 | 
 2406 | 	//---------------------------------------------------------------------
 2407 | 	// Serialization support
 2408 | 	//---------------------------------------------------------------------
 2409 | 
 2410 | 	@Serial
 2411 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
 2412 | 		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
 2413 | 				"just a SerializedBeanFactoryReference is");
 2414 | 	}
 2415 | 
 2416 | 	@Serial
 2417 | 	protected Object writeReplace() throws ObjectStreamException {
 2418 | 		if (this.serializationId != null) {
 2419 | 			return new SerializedBeanFactoryReference(this.serializationId);
 2420 | 		}
 2421 | 		else {
 2422 | 			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
 2423 | 		}
 2424 | 	}
 2425 | 
 2426 | 
 2427 | 	/**
 2428 | 	 * Minimal id reference to the factory.
 2429 | 	 * Resolved to the actual factory instance on deserialization.
 2430 | 	 */
 2431 | 	private static class SerializedBeanFactoryReference implements Serializable {
 2432 | 
 2433 | 		private final String id;
 2434 | 
 2435 | 		public SerializedBeanFactoryReference(String id) {
 2436 | 			this.id = id;
 2437 | 		}
 2438 | 
 2439 | 		private Object readResolve() {
 2440 | 			Reference<?> ref = serializableFactories.get(this.id);
 2441 | 			if (ref != null) {
 2442 | 				Object result = ref.get();
 2443 | 				if (result != null) {
 2444 | 					return result;
 2445 | 				}
 2446 | 			}
 2447 | 			// Lenient fallback: dummy factory in case of original factory not found...
 2448 | 			DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
 2449 | 			dummyFactory.serializationId = this.id;
 2450 | 			return dummyFactory;
 2451 | 		}
 2452 | 	}
 2453 | 
 2454 | 
 2455 | 	/**
 2456 | 	 * A dependency descriptor marker for nested elements.
 2457 | 	 */
 2458 | 	private static class NestedDependencyDescriptor extends DependencyDescriptor {
 2459 | 
 2460 | 		public NestedDependencyDescriptor(DependencyDescriptor original) {
 2461 | 			super(original);
 2462 | 			increaseNestingLevel();
 2463 | 		}
 2464 | 
 2465 | 		@Override
 2466 | 		public boolean usesStandardBeanLookup() {
 2467 | 			return true;
 2468 | 		}
 2469 | 	}
 2470 | 
 2471 | 
 2472 | 	/**
 2473 | 	 * A dependency descriptor for a multi-element declaration with nested elements.
 2474 | 	 */
 2475 | 	private static class MultiElementDescriptor extends NestedDependencyDescriptor {
 2476 | 
 2477 | 		public MultiElementDescriptor(DependencyDescriptor original) {
 2478 | 			super(original);
 2479 | 		}
 2480 | 	}
 2481 | 
 2482 | 
 2483 | 	/**
 2484 | 	 * A dependency descriptor marker for stream access to multiple elements.
 2485 | 	 */
 2486 | 	private static class StreamDependencyDescriptor extends DependencyDescriptor {
 2487 | 
 2488 | 		private final boolean ordered;
 2489 | 
 2490 | 		public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
 2491 | 			super(original);
 2492 | 			this.ordered = ordered;
 2493 | 		}
 2494 | 
 2495 | 		public boolean isOrdered() {
 2496 | 			return this.ordered;
 2497 | 		}
 2498 | 	}
 2499 | 
 2500 | 
 2501 | 	private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
 2502 | 	}
 2503 | 
 2504 | 
 2505 | 	/**
 2506 | 	 * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
 2507 | 	 */
 2508 | 	private class DependencyObjectProvider implements BeanObjectProvider<Object> {
 2509 | 
 2510 | 		private static final Object NOT_CACHEABLE = new Object();
 2511 | 
 2512 | 		private static final Object NULL_VALUE = new Object();
 2513 | 
 2514 | 		private final DependencyDescriptor descriptor;
 2515 | 
 2516 | 		private final boolean optional;
 2517 | 
 2518 | 		private final @Nullable String beanName;
 2519 | 
 2520 | 		private transient volatile @Nullable Object cachedValue;
 2521 | 
 2522 | 		public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
 2523 | 			this.descriptor = new NestedDependencyDescriptor(descriptor);
 2524 | 			this.optional = (this.descriptor.getDependencyType() == Optional.class);
 2525 | 			this.beanName = beanName;
 2526 | 		}
 2527 | 
 2528 | 		@Override
 2529 | 		public Object getObject() throws BeansException {
 2530 | 			Object result = getValue();
 2531 | 			if (result == null) {
 2532 | 				throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
 2533 | 			}
 2534 | 			return result;
 2535 | 		}
 2536 | 
 2537 | 		@Override
 2538 | 		public Object getObject(final @Nullable Object... args) throws BeansException {
 2539 | 			if (this.optional) {
 2540 | 				return createOptionalDependency(this.descriptor, this.beanName, null, args);
 2541 | 			}
 2542 | 			else {
 2543 | 				DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
 2544 | 					@Override
 2545 | 					public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
 2546 | 						return beanFactory.getBean(beanName, args);
 2547 | 					}
 2548 | 				};
 2549 | 				Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
 2550 | 				if (result == null) {
 2551 | 					throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
 2552 | 				}
 2553 | 				return result;
 2554 | 			}
 2555 | 		}
 2556 | 
 2557 | 		@Override
 2558 | 		public @Nullable Object getIfAvailable() throws BeansException {
 2559 | 			try {
 2560 | 				if (this.optional) {
 2561 | 					return createOptionalDependency(this.descriptor, this.beanName, null, null);
 2562 | 				}
 2563 | 				else {
 2564 | 					DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
 2565 | 						@Override
 2566 | 						public boolean isRequired() {
 2567 | 							return false;
 2568 | 						}
 2569 | 						@Override
 2570 | 						public boolean usesStandardBeanLookup() {
 2571 | 							return true;
 2572 | 						}
 2573 | 					};
 2574 | 					return doResolveDependency(descriptorToUse, this.beanName, null, null);
 2575 | 				}
 2576 | 			}
 2577 | 			catch (ScopeNotActiveException ex) {
 2578 | 				// Ignore resolved bean in non-active scope
 2579 | 				return null;
 2580 | 			}
 2581 | 		}
 2582 | 
 2583 | 		@Override
 2584 | 		public void ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
 2585 | 			Object dependency = getIfAvailable();
 2586 | 			if (dependency != null) {
 2587 | 				try {
 2588 | 					dependencyConsumer.accept(dependency);
 2589 | 				}
 2590 | 				catch (ScopeNotActiveException ex) {
 2591 | 					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
 2592 | 				}
 2593 | 			}
 2594 | 		}
 2595 | 
 2596 | 		@Override
 2597 | 		public @Nullable Object getIfUnique() throws BeansException {
 2598 | 			DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
 2599 | 				@Override
 2600 | 				public boolean isRequired() {
 2601 | 					return false;
 2602 | 				}
 2603 | 				@Override
 2604 | 				public boolean usesStandardBeanLookup() {
 2605 | 					return true;
 2606 | 				}
 2607 | 				@Override
 2608 | 				public @Nullable Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
 2609 | 					return null;
 2610 | 				}
 2611 | 			};
 2612 | 			try {
 2613 | 				if (this.optional) {
 2614 | 					return createOptionalDependency(descriptorToUse, this.beanName, null, null);
 2615 | 				}
 2616 | 				else {
 2617 | 					return doResolveDependency(descriptorToUse, this.beanName, null, null);
 2618 | 				}
 2619 | 			}
 2620 | 			catch (ScopeNotActiveException ex) {
 2621 | 				// Ignore resolved bean in non-active scope
 2622 | 				return null;
 2623 | 			}
 2624 | 		}
 2625 | 
 2626 | 		@Override
 2627 | 		public void ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
 2628 | 			Object dependency = getIfUnique();
 2629 | 			if (dependency != null) {
 2630 | 				try {
 2631 | 					dependencyConsumer.accept(dependency);
 2632 | 				}
 2633 | 				catch (ScopeNotActiveException ex) {
 2634 | 					// Ignore resolved bean in non-active scope, even on scoped proxy invocation
 2635 | 				}
 2636 | 			}
 2637 | 		}
 2638 | 
 2639 | 		protected @Nullable Object getValue() throws BeansException {
 2640 | 			Object value = this.cachedValue;
 2641 | 			if (value == null) {
 2642 | 				if (isConfigurationFrozen()) {
 2643 | 					Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
 2644 | 					value = resolveValue(autowiredBeanNames);
 2645 | 					boolean cacheable = false;
 2646 | 					if (!autowiredBeanNames.isEmpty()) {
 2647 | 						cacheable = true;
 2648 | 						for (String autowiredBeanName : autowiredBeanNames) {
 2649 | 							if (!containsBean(autowiredBeanName) || !isSingleton(autowiredBeanName)) {
 2650 | 								cacheable = false;
 2651 | 							}
 2652 | 						}
 2653 | 					}
 2654 | 					this.cachedValue = (cacheable ? (value != null ? value : NULL_VALUE) : NOT_CACHEABLE);
 2655 | 					return value;
 2656 | 				}
 2657 | 			}
 2658 | 			else if (value == NULL_VALUE) {
 2659 | 				return null;
 2660 | 			}
 2661 | 			else if (value != NOT_CACHEABLE) {
 2662 | 				return value;
 2663 | 			}
 2664 | 
 2665 | 			// Not cacheable -> fresh resolution.
 2666 | 			return resolveValue(null);
 2667 | 		}
 2668 | 
 2669 | 		private @Nullable Object resolveValue(@Nullable Set<String> autowiredBeanNames) {
 2670 | 			if (this.optional) {
 2671 | 				return createOptionalDependency(this.descriptor, this.beanName, autowiredBeanNames, null);
 2672 | 			}
 2673 | 			else {
 2674 | 				return doResolveDependency(this.descriptor, this.beanName, autowiredBeanNames, null);
 2675 | 			}
 2676 | 		}
 2677 | 
 2678 | 		@Override
 2679 | 		public Stream<Object> stream() {
 2680 | 			return resolveStream(false);
 2681 | 		}
 2682 | 
 2683 | 		@Override
 2684 | 		public Stream<Object> orderedStream() {
 2685 | 			return resolveStream(true);
 2686 | 		}
 2687 | 
 2688 | 		@SuppressWarnings({"rawtypes", "unchecked"})
 2689 | 		private Stream<Object> resolveStream(boolean ordered) {
 2690 | 			DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
 2691 | 			Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
 2692 | 			return (result instanceof Stream stream ? stream : Stream.of(result));
 2693 | 		}
 2694 | 
 2695 | 		@Override
 2696 | 		public Stream<Object> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
 2697 | 			ResolvableType type = this.descriptor.getResolvableType();
 2698 | 			return Arrays.stream(beanNamesForStream(type, includeNonSingletons, true))
 2699 | 					.filter(name -> AutowireUtils.isAutowireCandidate(DefaultListableBeanFactory.this, name))
 2700 | 					.filter(name -> customFilter.test(getType(name)))
 2701 | 					.map(name -> resolveBean(name, type))
 2702 | 					.filter(bean -> !(bean instanceof NullBean));
 2703 | 		}
 2704 | 
 2705 | 		@Override
 2706 | 		public Stream<Object> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
 2707 | 			ResolvableType type = this.descriptor.getResolvableType();
 2708 | 			String[] beanNames = beanNamesForStream(type, includeNonSingletons, true);
 2709 | 			if (beanNames.length == 0) {
 2710 | 				return Stream.empty();
 2711 | 			}
 2712 | 			Map<String, Object> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.length);
 2713 | 			for (String beanName : beanNames) {
 2714 | 				if (AutowireUtils.isAutowireCandidate(DefaultListableBeanFactory.this, beanName) &&
 2715 | 						customFilter.test(getType(beanName))) {
 2716 | 					Object beanInstance = resolveBean(beanName, type);
 2717 | 					if (!(beanInstance instanceof NullBean)) {
 2718 | 						matchingBeans.put(beanName, beanInstance);
 2719 | 					}
 2720 | 				}
 2721 | 			}
 2722 | 			return matchingBeans.values().stream().sorted(adaptOrderComparator(matchingBeans));
 2723 | 		}
 2724 | 	}
 2725 | 
 2726 | 
 2727 | 	/**
 2728 | 	 * Separate inner class for avoiding a hard dependency on the {@code jakarta.inject} API.
 2729 | 	 * Actual {@code jakarta.inject.Provider} implementation is nested here in order to make it
 2730 | 	 * invisible for Graal's introspection of DefaultListableBeanFactory's nested classes.
 2731 | 	 */
 2732 | 	private class Jsr330Factory implements Serializable {
 2733 | 
 2734 | 		public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
 2735 | 			return new Jsr330Provider(descriptor, beanName);
 2736 | 		}
 2737 | 
 2738 | 		private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {
 2739 | 
 2740 | 			public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
 2741 | 				super(descriptor, beanName);
 2742 | 			}
 2743 | 
 2744 | 			@Override
 2745 | 			public @Nullable Object get() throws BeansException {
 2746 | 				return getValue();
 2747 | 			}
 2748 | 		}
 2749 | 	}
 2750 | 
 2751 | 
 2752 | 	/**
 2753 | 	 * An {@link org.springframework.core.OrderComparator.OrderSourceProvider} implementation
 2754 | 	 * that is aware of the bean metadata of the instances to sort.
 2755 | 	 * <p>Lookup for the method factory of an instance to sort, if any, and let the
 2756 | 	 * comparator retrieve the {@link org.springframework.core.annotation.Order}
 2757 | 	 * value defined on it.
 2758 | 	 * <p>As of 6.1.2, this class takes the {@link AbstractBeanDefinition#ORDER_ATTRIBUTE}
 2759 | 	 * attribute into account.
 2760 | 	 */
 2761 | 	private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {
 2762 | 
 2763 | 		private final Map<Object, String> instancesToBeanNames;
 2764 | 
 2765 | 		public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
 2766 | 			this.instancesToBeanNames = instancesToBeanNames;
 2767 | 		}
 2768 | 
 2769 | 		@Override
 2770 | 		public @Nullable Object getOrderSource(Object obj) {
 2771 | 			String beanName = this.instancesToBeanNames.get(obj);
 2772 | 			if (beanName == null) {
 2773 | 				return null;
 2774 | 			}
 2775 | 			try {
 2776 | 				BeanDefinition beanDefinition = getMergedBeanDefinition(beanName);
 2777 | 				List<Object> sources = new ArrayList<>(3);
 2778 | 				Object orderAttribute = beanDefinition.getAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE);
 2779 | 				if (orderAttribute != null) {
 2780 | 					if (orderAttribute instanceof Integer order) {
 2781 | 						sources.add((Ordered) () -> order);
 2782 | 					}
 2783 | 					else {
 2784 | 						throw new IllegalStateException("Invalid value type for attribute '" +
 2785 | 								AbstractBeanDefinition.ORDER_ATTRIBUTE + "': " + orderAttribute.getClass().getName());
 2786 | 					}
 2787 | 				}
 2788 | 				if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
 2789 | 					Method factoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
 2790 | 					if (factoryMethod != null) {
 2791 | 						sources.add(factoryMethod);
 2792 | 					}
 2793 | 					Class<?> targetType = rootBeanDefinition.getTargetType();
 2794 | 					if (targetType != null && targetType != obj.getClass()) {
 2795 | 						sources.add(targetType);
 2796 | 					}
 2797 | 				}
 2798 | 				return sources.toArray();
 2799 | 			}
 2800 | 			catch (NoSuchBeanDefinitionException ex) {
 2801 | 				return null;
 2802 | 			}
 2803 | 		}
 2804 | 	}
 2805 | 
 2806 | 
 2807 | 	private enum PreInstantiation {
 2808 | 
 2809 | 		MAIN, BACKGROUND
 2810 | 	}
 2811 | 
 2812 | }
```
