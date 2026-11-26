<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java -->
<!-- 正文引用：[[源码:AbstractAutowireCapableBeanFactory.java#L起-L止]] ；代码块头部：(src: AbstractAutowireCapableBeanFactory.java#L起-L止) -->

# AbstractAutowireCapableBeanFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory.java` |
| 行数 | 2073 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:48:29.651Z |

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
   19 | import java.beans.PropertyDescriptor;
   20 | import java.lang.reflect.Constructor;
   21 | import java.lang.reflect.InvocationTargetException;
   22 | import java.lang.reflect.Method;
   23 | import java.lang.reflect.Modifier;
   24 | import java.util.ArrayList;
   25 | import java.util.Arrays;
   26 | import java.util.Collection;
   27 | import java.util.HashSet;
   28 | import java.util.LinkedHashSet;
   29 | import java.util.List;
   30 | import java.util.Map;
   31 | import java.util.Set;
   32 | import java.util.TreeSet;
   33 | import java.util.concurrent.ConcurrentHashMap;
   34 | import java.util.function.Supplier;
   35 | 
   36 | import org.apache.commons.logging.Log;
   37 | import org.jspecify.annotations.Nullable;
   38 | 
   39 | import org.springframework.beans.BeanUtils;
   40 | import org.springframework.beans.BeanWrapper;
   41 | import org.springframework.beans.BeanWrapperImpl;
   42 | import org.springframework.beans.BeansException;
   43 | import org.springframework.beans.InvalidPropertyException;
   44 | import org.springframework.beans.MutablePropertyValues;
   45 | import org.springframework.beans.PropertyAccessor;
   46 | import org.springframework.beans.PropertyValue;
   47 | import org.springframework.beans.PropertyValues;
   48 | import org.springframework.beans.TypeConverter;
   49 | import org.springframework.beans.factory.Aware;
   50 | import org.springframework.beans.factory.BeanClassLoaderAware;
   51 | import org.springframework.beans.factory.BeanCreationException;
   52 | import org.springframework.beans.factory.BeanCurrentlyInCreationException;
   53 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   54 | import org.springframework.beans.factory.BeanFactory;
   55 | import org.springframework.beans.factory.BeanFactoryAware;
   56 | import org.springframework.beans.factory.BeanNameAware;
   57 | import org.springframework.beans.factory.FactoryBean;
   58 | import org.springframework.beans.factory.InitializingBean;
   59 | import org.springframework.beans.factory.InjectionPoint;
   60 | import org.springframework.beans.factory.UnsatisfiedDependencyException;
   61 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   62 | import org.springframework.beans.factory.config.AutowiredPropertyMarker;
   63 | import org.springframework.beans.factory.config.BeanDefinition;
   64 | import org.springframework.beans.factory.config.BeanPostProcessor;
   65 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   66 | import org.springframework.beans.factory.config.ConstructorArgumentValues;
   67 | import org.springframework.beans.factory.config.DependencyDescriptor;
   68 | import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
   69 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   70 | import org.springframework.beans.factory.config.TypedStringValue;
   71 | import org.springframework.core.DefaultParameterNameDiscoverer;
   72 | import org.springframework.core.MethodParameter;
   73 | import org.springframework.core.NamedThreadLocal;
   74 | import org.springframework.core.ParameterNameDiscoverer;
   75 | import org.springframework.core.PriorityOrdered;
   76 | import org.springframework.core.ResolvableType;
   77 | import org.springframework.util.ClassUtils;
   78 | import org.springframework.util.CollectionUtils;
   79 | import org.springframework.util.ObjectUtils;
   80 | import org.springframework.util.ReflectionUtils;
   81 | import org.springframework.util.ReflectionUtils.MethodCallback;
   82 | import org.springframework.util.StringUtils;
   83 | import org.springframework.util.function.ThrowingSupplier;
   84 | 
   85 | /**
   86 |  * Abstract bean factory superclass that implements default bean creation,
   87 |  * with the full capabilities specified by the {@link RootBeanDefinition} class.
   88 |  * Implements the {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
   89 |  * interface in addition to AbstractBeanFactory's {@link #createBean} method.
   90 |  *
   91 |  * <p>Provides bean creation (with constructor resolution), property population,
   92 |  * wiring (including autowiring), and initialization. Handles runtime bean
   93 |  * references, resolves managed collections, calls initialization methods, etc.
   94 |  * Supports autowiring constructors, properties by name, and properties by type.
   95 |  *
   96 |  * <p>The main template method to be implemented by subclasses is
   97 |  * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)}, used for
   98 |  * autowiring. In case of a {@link org.springframework.beans.factory.ListableBeanFactory}
   99 |  * which is capable of searching its bean definitions, matching beans will typically be
  100 |  * implemented through such a search. Otherwise, simplified matching can be implemented.
  101 |  *
  102 |  * <p>Note that this class does <i>not</i> assume or implement bean definition
  103 |  * registry capabilities. See {@link DefaultListableBeanFactory} for an implementation
  104 |  * of the {@link org.springframework.beans.factory.ListableBeanFactory} and
  105 |  * {@link BeanDefinitionRegistry} interfaces, which represent the API and SPI
  106 |  * view of such a factory, respectively.
  107 |  *
  108 |  * @author Rod Johnson
  109 |  * @author Juergen Hoeller
  110 |  * @author Rob Harrop
  111 |  * @author Mark Fisher
  112 |  * @author Costin Leau
  113 |  * @author Chris Beams
  114 |  * @author Sam Brannen
  115 |  * @author Phillip Webb
  116 |  * @since 13.02.2004
  117 |  * @see RootBeanDefinition
  118 |  * @see DefaultListableBeanFactory
  119 |  * @see BeanDefinitionRegistry
  120 |  */
  121 | public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
  122 | 		implements AutowireCapableBeanFactory {
  123 | 
  124 | 	/** Strategy for creating bean instances. */
  125 | 	private InstantiationStrategy instantiationStrategy;
  126 | 
  127 | 	/** Resolver strategy for method parameter names. */
  128 | 	private @Nullable ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
  129 | 
  130 | 	/** Whether to automatically try to resolve circular references between beans. */
  131 | 	private boolean allowCircularReferences = true;
  132 | 
  133 | 	/**
  134 | 	 * Whether to resort to injecting a raw bean instance in case of circular reference,
  135 | 	 * even if the injected bean eventually got wrapped.
  136 | 	 */
  137 | 	private boolean allowRawInjectionDespiteWrapping = false;
  138 | 
  139 | 	/**
  140 | 	 * Dependency types to ignore on dependency check and autowire, as Set of
  141 | 	 * Class objects: for example, String. Default is none.
  142 | 	 */
  143 | 	private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();
  144 | 
  145 | 	/**
  146 | 	 * Dependency interfaces to ignore on dependency check and autowire, as a Set
  147 | 	 * of Class objects.
  148 | 	 * <p>By default, the {@code BeanNameAware}, {@code BeanFactoryAware}, and
  149 | 	 * {@code BeanClassLoaderAware} interfaces are ignored.
  150 | 	 */
  151 | 	private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();
  152 | 
  153 | 	/**
  154 | 	 * The name of the currently created bean, for implicit dependency registration
  155 | 	 * on getBean etc invocations triggered from a user-specified Supplier callback.
  156 | 	 */
  157 | 	private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");
  158 | 
  159 | 	/** Cache of unfinished FactoryBean instances: FactoryBean name to BeanWrapper. */
  160 | 	private final Map<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();
  161 | 
  162 | 	/** Cache of candidate factory methods per factory class. */
  163 | 	private final Map<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();
  164 | 
  165 | 	/** Cache of filtered PropertyDescriptors: bean Class to PropertyDescriptor array. */
  166 | 	private final Map<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache = new ConcurrentHashMap<>();
  167 | 
  168 | 
  169 | 	/**
  170 | 	 * Create a new AbstractAutowireCapableBeanFactory.
  171 | 	 */
  172 | 	public AbstractAutowireCapableBeanFactory() {
  173 | 		ignoreDependencyInterface(BeanNameAware.class);
  174 | 		ignoreDependencyInterface(BeanFactoryAware.class);
  175 | 		ignoreDependencyInterface(BeanClassLoaderAware.class);
  176 | 		this.instantiationStrategy = new CglibSubclassingInstantiationStrategy();
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * Create a new AbstractAutowireCapableBeanFactory with the given parent.
  181 | 	 * @param parentBeanFactory parent bean factory, or {@code null} if none
  182 | 	 */
  183 | 	public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
  184 | 		this();
  185 | 		setParentBeanFactory(parentBeanFactory);
  186 | 	}
  187 | 
  188 | 
  189 | 	/**
  190 | 	 * Set the instantiation strategy to use for creating bean instances.
  191 | 	 * Default is CglibSubclassingInstantiationStrategy.
  192 | 	 * @see CglibSubclassingInstantiationStrategy
  193 | 	 */
  194 | 	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
  195 | 		this.instantiationStrategy = instantiationStrategy;
  196 | 	}
  197 | 
  198 | 	/**
  199 | 	 * Return the instantiation strategy to use for creating bean instances.
  200 | 	 */
  201 | 	public InstantiationStrategy getInstantiationStrategy() {
  202 | 		return this.instantiationStrategy;
  203 | 	}
  204 | 
  205 | 	/**
  206 | 	 * Set the ParameterNameDiscoverer to use for resolving method parameter
  207 | 	 * names if needed (for example, for constructor names).
  208 | 	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
  209 | 	 */
  210 | 	public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
  211 | 		this.parameterNameDiscoverer = parameterNameDiscoverer;
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Return the ParameterNameDiscoverer to use for resolving method parameter
  216 | 	 * names if needed.
  217 | 	 */
  218 | 	public @Nullable ParameterNameDiscoverer getParameterNameDiscoverer() {
  219 | 		return this.parameterNameDiscoverer;
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Set whether to allow circular references between beans - and automatically
  224 | 	 * try to resolve them.
  225 | 	 * <p>Note that circular reference resolution means that one of the involved beans
  226 | 	 * will receive a reference to another bean that is not fully initialized yet.
  227 | 	 * This can lead to subtle and not-so-subtle side effects on initialization;
  228 | 	 * it does work fine for many scenarios, though.
  229 | 	 * <p>Default is "true". Turn this off to throw an exception when encountering
  230 | 	 * a circular reference, disallowing them completely.
  231 | 	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
  232 | 	 * between your beans. Refactor your application logic to have the two beans
  233 | 	 * involved delegate to a third bean that encapsulates their common logic.
  234 | 	 */
  235 | 	public void setAllowCircularReferences(boolean allowCircularReferences) {
  236 | 		this.allowCircularReferences = allowCircularReferences;
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Return whether to allow circular references between beans.
  241 | 	 * @since 5.3.10
  242 | 	 * @see #setAllowCircularReferences
  243 | 	 */
  244 | 	public boolean isAllowCircularReferences() {
  245 | 		return this.allowCircularReferences;
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Set whether to allow the raw injection of a bean instance into some other
  250 | 	 * bean's property, despite the injected bean eventually getting wrapped
  251 | 	 * (for example, through AOP auto-proxying).
  252 | 	 * <p>This will only be used as a last resort in case of a circular reference
  253 | 	 * that cannot be resolved otherwise: essentially, preferring a raw instance
  254 | 	 * getting injected over a failure of the entire bean wiring process.
  255 | 	 * <p>Default is "false". Turn this on to allow for non-wrapped
  256 | 	 * raw beans injected into some of your references.
  257 | 	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
  258 | 	 * between your beans, in particular with auto-proxying involved.
  259 | 	 * @see #setAllowCircularReferences
  260 | 	 */
  261 | 	public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
  262 | 		this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Return whether to allow the raw injection of a bean instance.
  267 | 	 * @since 5.3.10
  268 | 	 * @see #setAllowRawInjectionDespiteWrapping
  269 | 	 */
  270 | 	public boolean isAllowRawInjectionDespiteWrapping() {
  271 | 		return this.allowRawInjectionDespiteWrapping;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Ignore the given dependency type for autowiring:
  276 | 	 * for example, String. Default is none.
  277 | 	 */
  278 | 	public void ignoreDependencyType(Class<?> type) {
  279 | 		this.ignoredDependencyTypes.add(type);
  280 | 	}
  281 | 
  282 | 	/**
  283 | 	 * Ignore the given dependency interface for autowiring.
  284 | 	 * <p>This will typically be used by application contexts to register
  285 | 	 * dependencies that are resolved in other ways, like {@code BeanFactory}
  286 | 	 * through {@code BeanFactoryAware} or {@code ApplicationContext} through
  287 | 	 * {@code ApplicationContextAware}.
  288 | 	 * <p>By default, the {@code BeanNameAware}, {@code BeanFactoryAware}, and
  289 | 	 * {@code BeanClassLoaderAware} interfaces are ignored.
  290 | 	 * For further types to ignore, invoke this method for each type.
  291 | 	 * @see org.springframework.beans.factory.BeanNameAware
  292 | 	 * @see org.springframework.beans.factory.BeanFactoryAware
  293 | 	 * @see org.springframework.beans.factory.BeanClassLoaderAware
  294 | 	 * @see org.springframework.context.ApplicationContextAware
  295 | 	 */
  296 | 	public void ignoreDependencyInterface(Class<?> ifc) {
  297 | 		this.ignoredDependencyInterfaces.add(ifc);
  298 | 	}
  299 | 
  300 | 	@Override
  301 | 	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
  302 | 		super.copyConfigurationFrom(otherFactory);
  303 | 		if (otherFactory instanceof AbstractAutowireCapableBeanFactory otherAutowireFactory) {
  304 | 			this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
  305 | 			this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
  306 | 			this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
  307 | 			this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
  308 | 		}
  309 | 	}
  310 | 
  311 | 
  312 | 	//-------------------------------------------------------------------------
  313 | 	// Typical methods for creating and populating external bean instances
  314 | 	//-------------------------------------------------------------------------
  315 | 
  316 | 	@Override
  317 | 	@SuppressWarnings("unchecked")
  318 | 	public <T> T createBean(Class<T> beanClass) throws BeansException {
  319 | 		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
  320 | 		RootBeanDefinition bd = new CreateFromClassBeanDefinition(beanClass);
  321 | 		bd.setScope(SCOPE_PROTOTYPE);
  322 | 		bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
  323 | 		return (T) createBean(beanClass.getName(), bd, null);
  324 | 	}
  325 | 
  326 | 	@Override
  327 | 	public void autowireBean(Object existingBean) {
  328 | 		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
  329 | 		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
  330 | 		bd.setScope(SCOPE_PROTOTYPE);
  331 | 		bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
  332 | 		BeanWrapper bw = new BeanWrapperImpl(existingBean);
  333 | 		initBeanWrapper(bw);
  334 | 		populateBean(bd.getBeanClass().getName(), bd, bw);
  335 | 	}
  336 | 
  337 | 	@Override
  338 | 	public Object configureBean(Object existingBean, String beanName) throws BeansException {
  339 | 		markBeanAsCreated(beanName);
  340 | 		BeanDefinition mbd = getMergedBeanDefinition(beanName);
  341 | 		RootBeanDefinition bd = null;
  342 | 		if (mbd instanceof RootBeanDefinition rbd) {
  343 | 			bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
  344 | 		}
  345 | 		if (bd == null) {
  346 | 			bd = new RootBeanDefinition(mbd);
  347 | 		}
  348 | 		if (!bd.isPrototype()) {
  349 | 			bd.setScope(SCOPE_PROTOTYPE);
  350 | 			bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
  351 | 		}
  352 | 		BeanWrapper bw = new BeanWrapperImpl(existingBean);
  353 | 		initBeanWrapper(bw);
  354 | 		populateBean(beanName, bd, bw);
  355 | 		return initializeBean(beanName, existingBean, bd);
  356 | 	}
  357 | 
  358 | 
  359 | 	//-------------------------------------------------------------------------
  360 | 	// Specialized methods for fine-grained control over the bean lifecycle
  361 | 	//-------------------------------------------------------------------------
  362 | 
  363 | 	@Deprecated(since = "6.1")
  364 | 	@Override
  365 | 	public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
  366 | 		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
  367 | 		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
  368 | 		bd.setScope(SCOPE_PROTOTYPE);
  369 | 		return createBean(beanClass.getName(), bd, null);
  370 | 	}
  371 | 
  372 | 	@Override
  373 | 	public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
  374 | 		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
  375 | 		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
  376 | 		bd.setScope(SCOPE_PROTOTYPE);
  377 | 		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
  378 | 			return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
  379 | 		}
  380 | 		else {
  381 | 			Object bean = getInstantiationStrategy().instantiate(bd, null, this);
  382 | 			populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
  383 | 			return bean;
  384 | 		}
  385 | 	}
  386 | 
  387 | 	@Override
  388 | 	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
  389 | 			throws BeansException {
  390 | 
  391 | 		if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
  392 | 			throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
  393 | 		}
  394 | 		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
  395 | 		RootBeanDefinition bd =
  396 | 				new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
  397 | 		bd.setScope(SCOPE_PROTOTYPE);
  398 | 		BeanWrapper bw = new BeanWrapperImpl(existingBean);
  399 | 		initBeanWrapper(bw);
  400 | 		populateBean(bd.getBeanClass().getName(), bd, bw);
  401 | 	}
  402 | 
  403 | 	@Override
  404 | 	public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
  405 | 		markBeanAsCreated(beanName);
  406 | 		BeanDefinition bd = getMergedBeanDefinition(beanName);
  407 | 		BeanWrapper bw = new BeanWrapperImpl(existingBean);
  408 | 		initBeanWrapper(bw);
  409 | 		applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
  410 | 	}
  411 | 
  412 | 	@Override
  413 | 	public Object initializeBean(Object existingBean, String beanName) {
  414 | 		return initializeBean(beanName, existingBean, null);
  415 | 	}
  416 | 
  417 | 	@Deprecated(since = "6.1")
  418 | 	@Override
  419 | 	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
  420 | 			throws BeansException {
  421 | 
  422 | 		Object result = existingBean;
  423 | 		for (BeanPostProcessor processor : getBeanPostProcessors()) {
  424 | 			Object current = processor.postProcessBeforeInitialization(result, beanName);
  425 | 			if (current == null) {
  426 | 				return result;
  427 | 			}
  428 | 			result = current;
  429 | 		}
  430 | 		return result;
  431 | 	}
  432 | 
  433 | 	@Deprecated(since = "6.1")
  434 | 	@Override
  435 | 	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
  436 | 			throws BeansException {
  437 | 
  438 | 		Object result = existingBean;
  439 | 		for (BeanPostProcessor processor : getBeanPostProcessors()) {
  440 | 			Object current = processor.postProcessAfterInitialization(result, beanName);
  441 | 			if (current == null) {
  442 | 				return result;
  443 | 			}
  444 | 			result = current;
  445 | 		}
  446 | 		return result;
  447 | 	}
  448 | 
  449 | 	@Override
  450 | 	public void destroyBean(Object existingBean) {
  451 | 		new DisposableBeanAdapter(existingBean, getBeanPostProcessorCache().destructionAware).destroy();
  452 | 	}
  453 | 
  454 | 
  455 | 	//-------------------------------------------------------------------------
  456 | 	// Delegate methods for resolving injection points
  457 | 	//-------------------------------------------------------------------------
  458 | 
  459 | 	@Override
  460 | 	public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
  461 | 		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
  462 | 		try {
  463 | 			return getBean(name, descriptor.getDependencyType());
  464 | 		}
  465 | 		finally {
  466 | 			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
  467 | 		}
  468 | 	}
  469 | 
  470 | 	@Override
  471 | 	public @Nullable Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
  472 | 		return resolveDependency(descriptor, requestingBeanName, null, null);
  473 | 	}
  474 | 
  475 | 
  476 | 	//---------------------------------------------------------------------
  477 | 	// Implementation of relevant AbstractBeanFactory template methods
  478 | 	//---------------------------------------------------------------------
  479 | 
  480 | 	/**
  481 | 	 * Central method of this class: creates a bean instance,
  482 | 	 * populates the bean instance, applies post-processors, etc.
  483 | 	 * @see #doCreateBean
  484 | 	 */
  485 | 	@Override
  486 | 	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args)
  487 | 			throws BeanCreationException {
  488 | 
  489 | 		if (logger.isTraceEnabled()) {
  490 | 			logger.trace("Creating instance of bean '" + beanName + "'");
  491 | 		}
  492 | 		RootBeanDefinition mbdToUse = mbd;
  493 | 
  494 | 		// Make sure bean class is actually resolved at this point, and
  495 | 		// clone the bean definition in case of a dynamically resolved Class
  496 | 		// which cannot be stored in the shared merged bean definition.
  497 | 		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
  498 | 		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
  499 | 			mbdToUse = new RootBeanDefinition(mbd);
  500 | 			mbdToUse.setBeanClass(resolvedClass);
  501 | 			try {
  502 | 				mbdToUse.prepareMethodOverrides();
  503 | 			}
  504 | 			catch (BeanDefinitionValidationException ex) {
  505 | 				throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
  506 | 						beanName, "Validation of method overrides failed", ex);
  507 | 			}
  508 | 		}
  509 | 
  510 | 		try {
  511 | 			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
  512 | 			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
  513 | 			if (bean != null) {
  514 | 				return bean;
  515 | 			}
  516 | 		}
  517 | 		catch (Throwable ex) {
  518 | 			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
  519 | 					"BeanPostProcessor before instantiation of bean failed", ex);
  520 | 		}
  521 | 
  522 | 		try {
  523 | 			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
  524 | 			if (logger.isTraceEnabled()) {
  525 | 				logger.trace("Finished creating instance of bean '" + beanName + "'");
  526 | 			}
  527 | 			return beanInstance;
  528 | 		}
  529 | 		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
  530 | 			// A previously detected exception with proper bean creation context already,
  531 | 			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
  532 | 			throw ex;
  533 | 		}
  534 | 		catch (Throwable ex) {
  535 | 			throw new BeanCreationException(
  536 | 					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
  537 | 		}
  538 | 	}
  539 | 
  540 | 	/**
  541 | 	 * Actually create the specified bean. Pre-creation processing has already happened
  542 | 	 * at this point, for example, checking {@code postProcessBeforeInstantiation} callbacks.
  543 | 	 * <p>Differentiates between default bean instantiation, use of a
  544 | 	 * factory method, and autowiring a constructor.
  545 | 	 * @param beanName the name of the bean
  546 | 	 * @param mbd the merged bean definition for the bean
  547 | 	 * @param args explicit arguments to use for constructor or factory method invocation
  548 | 	 * @return a new instance of the bean
  549 | 	 * @throws BeanCreationException if the bean could not be created
  550 | 	 * @see #instantiateBean
  551 | 	 * @see #instantiateUsingFactoryMethod
  552 | 	 * @see #autowireConstructor
  553 | 	 */
  554 | 	protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args)
  555 | 			throws BeanCreationException {
  556 | 
  557 | 		// Instantiate the bean.
  558 | 		BeanWrapper instanceWrapper = null;
  559 | 		if (mbd.isSingleton()) {
  560 | 			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
  561 | 		}
  562 | 		if (instanceWrapper == null) {
  563 | 			instanceWrapper = createBeanInstance(beanName, mbd, args);
  564 | 		}
  565 | 		Object bean = instanceWrapper.getWrappedInstance();
  566 | 		Class<?> beanType = instanceWrapper.getWrappedClass();
  567 | 		if (beanType != NullBean.class) {
  568 | 			mbd.resolvedTargetType = beanType;
  569 | 		}
  570 | 
  571 | 		// Allow post-processors to modify the merged bean definition.
  572 | 		synchronized (mbd.postProcessingLock) {
  573 | 			if (!mbd.postProcessed) {
  574 | 				try {
  575 | 					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
  576 | 				}
  577 | 				catch (Throwable ex) {
  578 | 					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
  579 | 							"Post-processing of merged bean definition failed", ex);
  580 | 				}
  581 | 				mbd.markAsPostProcessed();
  582 | 			}
  583 | 		}
  584 | 
  585 | 		// Eagerly cache singletons to be able to resolve circular references
  586 | 		// even when triggered by lifecycle interfaces like BeanFactoryAware.
  587 | 		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
  588 | 				isSingletonCurrentlyInCreation(beanName));
  589 | 		if (earlySingletonExposure) {
  590 | 			if (logger.isTraceEnabled()) {
  591 | 				logger.trace("Eagerly caching bean '" + beanName +
  592 | 						"' to allow for resolving potential circular references");
  593 | 			}
  594 | 			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
  595 | 		}
  596 | 
  597 | 		// Initialize the bean instance.
  598 | 		Object exposedObject = bean;
  599 | 		try {
  600 | 			populateBean(beanName, mbd, instanceWrapper);
  601 | 			exposedObject = initializeBean(beanName, exposedObject, mbd);
  602 | 		}
  603 | 		catch (Throwable ex) {
  604 | 			if (ex instanceof BeanCreationException bce && beanName.equals(bce.getBeanName())) {
  605 | 				throw bce;
  606 | 			}
  607 | 			throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
  608 | 		}
  609 | 
  610 | 		if (earlySingletonExposure) {
  611 | 			Object earlySingletonReference = getSingleton(beanName, false);
  612 | 			if (earlySingletonReference != null) {
  613 | 				if (exposedObject == bean) {
  614 | 					exposedObject = earlySingletonReference;
  615 | 				}
  616 | 				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
  617 | 					String[] dependentBeans = getDependentBeans(beanName);
  618 | 					Set<String> actualDependentBeans = CollectionUtils.newLinkedHashSet(dependentBeans.length);
  619 | 					for (String dependentBean : dependentBeans) {
  620 | 						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
  621 | 							actualDependentBeans.add(dependentBean);
  622 | 						}
  623 | 					}
  624 | 					if (!actualDependentBeans.isEmpty()) {
  625 | 						throw new BeanCurrentlyInCreationException(beanName,
  626 | 								"Bean with name '" + beanName + "' has been injected into other beans [" +
  627 | 								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
  628 | 								"] in its raw version as part of a circular reference, but has eventually been " +
  629 | 								"wrapped. This means that said other beans do not use the final version of the " +
  630 | 								"bean. This is often the result of over-eager type matching - consider using " +
  631 | 								"'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
  632 | 					}
  633 | 				}
  634 | 			}
  635 | 		}
  636 | 
  637 | 		// Register bean as disposable.
  638 | 		try {
  639 | 			registerDisposableBeanIfNecessary(beanName, bean, mbd);
  640 | 		}
  641 | 		catch (BeanDefinitionValidationException ex) {
  642 | 			throw new BeanCreationException(
  643 | 					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
  644 | 		}
  645 | 
  646 | 		return exposedObject;
  647 | 	}
  648 | 
  649 | 	@Override
  650 | 	protected @Nullable Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
  651 | 		Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);
  652 | 		// Apply SmartInstantiationAwareBeanPostProcessors to predict the
  653 | 		// eventual type after a before-instantiation shortcut.
  654 | 		if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
  655 | 			boolean matchingOnlyFactoryBean = (typesToMatch.length == 1 && typesToMatch[0] == FactoryBean.class);
  656 | 			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
  657 | 				Class<?> predicted = bp.predictBeanType(targetType, beanName);
  658 | 				if (predicted != null &&
  659 | 						(!matchingOnlyFactoryBean || FactoryBean.class.isAssignableFrom(predicted))) {
  660 | 					return predicted;
  661 | 				}
  662 | 			}
  663 | 		}
  664 | 		return targetType;
  665 | 	}
  666 | 
  667 | 	/**
  668 | 	 * Determine the target type for the given bean definition.
  669 | 	 * @param beanName the name of the bean (for error handling purposes)
  670 | 	 * @param mbd the merged bean definition for the bean
  671 | 	 * @param typesToMatch the types to match in case of internal type matching purposes
  672 | 	 * (also signals that the returned {@code Class} will never be exposed to application code)
  673 | 	 * @return the type for the bean if determinable, or {@code null} otherwise
  674 | 	 */
  675 | 	protected @Nullable Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
  676 | 		Class<?> targetType = mbd.getTargetType();
  677 | 		if (targetType == null) {
  678 | 			if (mbd.getFactoryMethodName() != null) {
  679 | 				targetType = getTypeForFactoryMethod(beanName, mbd, typesToMatch);
  680 | 			}
  681 | 			else {
  682 | 				targetType = resolveBeanClass(mbd, beanName, typesToMatch);
  683 | 				if (mbd.hasBeanClass()) {
  684 | 					targetType = getInstantiationStrategy().getActualBeanClass(mbd, beanName, this);
  685 | 				}
  686 | 			}
  687 | 			if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
  688 | 				mbd.resolvedTargetType = targetType;
  689 | 			}
  690 | 		}
  691 | 		return targetType;
  692 | 	}
  693 | 
  694 | 	/**
  695 | 	 * Determine the target type for the given bean definition which is based on
  696 | 	 * a factory method. Only called if there is no singleton instance registered
  697 | 	 * for the target bean already.
  698 | 	 * <p>This implementation determines the type matching {@link #createBean}'s
  699 | 	 * different creation strategies. As far as possible, we'll perform static
  700 | 	 * type checking to avoid creation of the target bean.
  701 | 	 * @param beanName the name of the bean (for error handling purposes)
  702 | 	 * @param mbd the merged bean definition for the bean
  703 | 	 * @param typesToMatch the types to match in case of internal type matching purposes
  704 | 	 * (also signals that the returned {@code Class} will never be exposed to application code)
  705 | 	 * @return the type for the bean if determinable, or {@code null} otherwise
  706 | 	 * @see #createBean
  707 | 	 */
  708 | 	protected @Nullable Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
  709 | 		ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
  710 | 		if (cachedReturnType != null) {
  711 | 			return cachedReturnType.resolve();
  712 | 		}
  713 | 
  714 | 		Class<?> commonType = null;
  715 | 		Method uniqueCandidate = mbd.factoryMethodToIntrospect;
  716 | 
  717 | 		if (uniqueCandidate == null) {
  718 | 			Class<?> factoryClass;
  719 | 			boolean isStatic = true;
  720 | 
  721 | 			String factoryBeanName = mbd.getFactoryBeanName();
  722 | 			if (factoryBeanName != null) {
  723 | 				if (factoryBeanName.equals(beanName)) {
  724 | 					throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
  725 | 							"factory-bean reference points back to the same bean definition");
  726 | 				}
  727 | 				// Check declared factory method return type on factory class.
  728 | 				factoryClass = getType(factoryBeanName);
  729 | 				isStatic = false;
  730 | 			}
  731 | 			else {
  732 | 				// Check declared factory method return type on bean class.
  733 | 				factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
  734 | 			}
  735 | 
  736 | 			if (factoryClass == null) {
  737 | 				return null;
  738 | 			}
  739 | 			factoryClass = ClassUtils.getUserClass(factoryClass);
  740 | 
  741 | 			// If all factory methods have the same return type, return that type.
  742 | 			// Can't clearly figure out exact method due to type converting / autowiring!
  743 | 			int minNrOfArgs =
  744 | 					(mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
  745 | 			Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
  746 | 					clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));
  747 | 
  748 | 			for (Method candidate : candidates) {
  749 | 				if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
  750 | 						candidate.getParameterCount() >= minNrOfArgs) {
  751 | 					// Declared type variables to inspect?
  752 | 					if (candidate.getTypeParameters().length > 0) {
  753 | 						try {
  754 | 							// Fully resolve parameter names and argument values.
  755 | 							ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
  756 | 							Class<?>[] paramTypes = candidate.getParameterTypes();
  757 | 							@Nullable String[] paramNames = null;
  758 | 							if (cav.containsNamedArgument()) {
  759 | 								ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
  760 | 								if (pnd != null) {
  761 | 									paramNames = pnd.getParameterNames(candidate);
  762 | 								}
  763 | 							}
  764 | 							Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = CollectionUtils.newHashSet(paramTypes.length);
  765 | 							@Nullable Object[] args = new Object[paramTypes.length];
  766 | 							for (int i = 0; i < args.length; i++) {
  767 | 								ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
  768 | 										i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
  769 | 								if (valueHolder == null) {
  770 | 									valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
  771 | 								}
  772 | 								if (valueHolder != null) {
  773 | 									args[i] = valueHolder.getValue();
  774 | 									usedValueHolders.add(valueHolder);
  775 | 								}
  776 | 							}
  777 | 							Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
  778 | 									candidate, args, getBeanClassLoader());
  779 | 							uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ?
  780 | 									candidate : null);
  781 | 							commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
  782 | 							if (commonType == null) {
  783 | 								// Ambiguous return types found: return null to indicate "not determinable".
  784 | 								return null;
  785 | 							}
  786 | 						}
  787 | 						catch (Throwable ex) {
  788 | 							if (logger.isDebugEnabled()) {
  789 | 								logger.debug("Failed to resolve generic return type for factory method: " + ex);
  790 | 							}
  791 | 						}
  792 | 					}
  793 | 					else {
  794 | 						uniqueCandidate = (commonType == null ? candidate : null);
  795 | 						commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
  796 | 						if (commonType == null) {
  797 | 							// Ambiguous return types found: return null to indicate "not determinable".
  798 | 							return null;
  799 | 						}
  800 | 					}
  801 | 				}
  802 | 			}
  803 | 
  804 | 			mbd.factoryMethodToIntrospect = uniqueCandidate;
  805 | 			if (commonType == null) {
  806 | 				return null;
  807 | 			}
  808 | 		}
  809 | 
  810 | 		// Common return type found: all factory methods return same type. For a non-parameterized
  811 | 		// unique candidate, cache the full type declaration context of the target factory method.
  812 | 		try {
  813 | 			cachedReturnType = (uniqueCandidate != null ?
  814 | 					ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
  815 | 			mbd.factoryMethodReturnType = cachedReturnType;
  816 | 			return cachedReturnType.resolve();
  817 | 		}
  818 | 		catch (LinkageError err) {
  819 | 			// For example, a NoClassDefFoundError for a generic method return type
  820 | 			if (logger.isDebugEnabled()) {
  821 | 				logger.debug("Failed to resolve type for factory method of bean '" + beanName + "': " +
  822 | 						(uniqueCandidate != null ? uniqueCandidate : commonType), err);
  823 | 			}
  824 | 			return null;
  825 | 		}
  826 | 	}
  827 | 
  828 | 	/**
  829 | 	 * This implementation attempts to query the FactoryBean's generic parameter metadata
  830 | 	 * if present to determine the object type. If not present, i.e. the FactoryBean is
  831 | 	 * declared as a raw type, it checks the FactoryBean's {@code getObjectType} method
  832 | 	 * on a plain instance of the FactoryBean, without bean properties applied yet.
  833 | 	 * If this doesn't return a type yet and {@code allowInit} is {@code true}, full
  834 | 	 * creation of the FactoryBean is attempted as fallback (through delegation to the
  835 | 	 * superclass implementation).
  836 | 	 * <p>The shortcut check for a FactoryBean is only applied in case of a singleton
  837 | 	 * FactoryBean. If the FactoryBean instance itself is not kept as singleton,
  838 | 	 * it will be fully created to check the type of its exposed object.
  839 | 	 */
  840 | 	@Override
  841 | 	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
  842 | 		ResolvableType result;
  843 | 
  844 | 		// Check if the bean definition itself has defined the type with an attribute
  845 | 		try {
  846 | 			result = getTypeForFactoryBeanFromAttributes(mbd);
  847 | 			if (result != ResolvableType.NONE) {
  848 | 				return result;
  849 | 			}
  850 | 		}
  851 | 		catch (IllegalArgumentException ex) {
  852 | 			throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
  853 | 					String.valueOf(ex.getMessage()));
  854 | 		}
  855 | 
  856 | 		// For instance supplied beans, try the target type and bean class immediately
  857 | 		if (mbd.getInstanceSupplier() != null) {
  858 | 			result = getFactoryBeanGeneric(mbd.targetType);
  859 | 			if (result.resolve() != null) {
  860 | 				return result;
  861 | 			}
  862 | 			result = getFactoryBeanGeneric(mbd.hasBeanClass() ? ResolvableType.forClass(mbd.getBeanClass()) : null);
  863 | 			if (result.resolve() != null) {
  864 | 				return result;
  865 | 			}
  866 | 		}
  867 | 
  868 | 		// Consider factory methods
  869 | 		String factoryBeanName = mbd.getFactoryBeanName();
  870 | 		String factoryMethodName = mbd.getFactoryMethodName();
  871 | 
  872 | 		// Scan the factory bean methods
  873 | 		if (factoryBeanName != null) {
  874 | 			if (factoryMethodName != null) {
  875 | 				// Try to obtain the FactoryBean's object type from its factory method
  876 | 				// declaration without instantiating the containing bean at all.
  877 | 				BeanDefinition factoryBeanDefinition = getBeanDefinition(factoryBeanName);
  878 | 				Class<?> factoryBeanClass;
  879 | 				if (factoryBeanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition &&
  880 | 						abstractBeanDefinition.hasBeanClass()) {
  881 | 					factoryBeanClass = abstractBeanDefinition.getBeanClass();
  882 | 				}
  883 | 				else {
  884 | 					RootBeanDefinition fbmbd = getMergedBeanDefinition(factoryBeanName, factoryBeanDefinition);
  885 | 					factoryBeanClass = determineTargetType(factoryBeanName, fbmbd);
  886 | 				}
  887 | 				if (factoryBeanClass != null) {
  888 | 					result = getTypeForFactoryBeanFromMethod(factoryBeanClass, factoryMethodName);
  889 | 					if (result.resolve() != null) {
  890 | 						return result;
  891 | 					}
  892 | 				}
  893 | 			}
  894 | 			// If not resolvable above and the referenced factory bean doesn't exist yet,
  895 | 			// exit here - we don't want to force the creation of another bean just to
  896 | 			// obtain a FactoryBean's object type...
  897 | 			if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
  898 | 				return ResolvableType.NONE;
  899 | 			}
  900 | 		}
  901 | 
  902 | 		// If we're allowed, we can create the factory bean and call getObjectType() early
  903 | 		if (allowInit) {
  904 | 			FactoryBean<?> factoryBean = (mbd.isSingleton() ?
  905 | 					getSingletonFactoryBeanForTypeCheck(beanName, mbd) :
  906 | 					getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));
  907 | 			if (factoryBean != null) {
  908 | 				// Try to obtain the FactoryBean's object type from this early stage of the instance.
  909 | 				Class<?> type = getTypeForFactoryBean(factoryBean);
  910 | 				if (type != null) {
  911 | 					return ResolvableType.forClass(type);
  912 | 				}
  913 | 				// No type found for shortcut FactoryBean instance:
  914 | 				// fall back to full creation of the FactoryBean instance.
  915 | 				return super.getTypeForFactoryBean(beanName, mbd, true);
  916 | 			}
  917 | 		}
  918 | 
  919 | 		if (factoryBeanName == null && mbd.hasBeanClass() && factoryMethodName != null) {
  920 | 			// No early bean instantiation possible: determine FactoryBean's type from
  921 | 			// static factory method signature or from class inheritance hierarchy...
  922 | 			return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
  923 | 		}
  924 | 
  925 | 		// For regular beans, try the target type and bean class as fallback
  926 | 		if (mbd.getInstanceSupplier() == null) {
  927 | 			result = getFactoryBeanGeneric(mbd.targetType);
  928 | 			if (result.resolve() != null) {
  929 | 				return result;
  930 | 			}
  931 | 			result = getFactoryBeanGeneric(mbd.hasBeanClass() ? ResolvableType.forClass(mbd.getBeanClass()) : null);
  932 | 			if (result.resolve() != null) {
  933 | 				return result;
  934 | 			}
  935 | 		}
  936 | 
  937 | 		// FactoryBean type not resolvable
  938 | 		return ResolvableType.NONE;
  939 | 	}
  940 | 
  941 | 	/**
  942 | 	 * Introspect the factory method signatures on the given bean class,
  943 | 	 * trying to find a common {@code FactoryBean} object type declared there.
  944 | 	 * @param beanClass the bean class to find the factory method on
  945 | 	 * @param factoryMethodName the name of the factory method
  946 | 	 * @return the common {@code FactoryBean} object type, or {@code null} if none
  947 | 	 */
  948 | 	private ResolvableType getTypeForFactoryBeanFromMethod(Class<?> beanClass, String factoryMethodName) {
  949 | 		// CGLIB subclass methods hide generic parameters; look at the original user class.
  950 | 		Class<?> factoryBeanClass = ClassUtils.getUserClass(beanClass);
  951 | 		FactoryBeanMethodTypeFinder finder = new FactoryBeanMethodTypeFinder(factoryMethodName);
  952 | 		ReflectionUtils.doWithMethods(factoryBeanClass, finder, ReflectionUtils.USER_DECLARED_METHODS);
  953 | 		return finder.getResult();
  954 | 	}
  955 | 
  956 | 	/**
  957 | 	 * Obtain a reference for early access to the specified bean,
  958 | 	 * typically for the purpose of resolving a circular reference.
  959 | 	 * @param beanName the name of the bean (for error handling purposes)
  960 | 	 * @param mbd the merged bean definition for the bean
  961 | 	 * @param bean the raw bean instance
  962 | 	 * @return the object to expose as bean reference
  963 | 	 */
  964 | 	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
  965 | 		Object exposedObject = bean;
  966 | 		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
  967 | 			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
  968 | 				exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
  969 | 			}
  970 | 		}
  971 | 		return exposedObject;
  972 | 	}
  973 | 
  974 | 
  975 | 	//---------------------------------------------------------------------
  976 | 	// Implementation methods
  977 | 	//---------------------------------------------------------------------
  978 | 
  979 | 	/**
  980 | 	 * Obtain a "shortcut" singleton FactoryBean instance to use for a
  981 | 	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
  982 | 	 * @param beanName the name of the bean
  983 | 	 * @param mbd the bean definition for the bean
  984 | 	 * @return the FactoryBean instance, or {@code null} to indicate
  985 | 	 * that we couldn't obtain a shortcut FactoryBean instance
  986 | 	 */
  987 | 	private @Nullable FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
  988 | 		Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock();
  989 | 		if (lockFlag == null) {
  990 | 			this.singletonLock.lock();
  991 | 		}
  992 | 		else {
  993 | 			boolean locked = (lockFlag && this.singletonLock.tryLock());
  994 | 			if (!locked) {
  995 | 				// Avoid shortcut FactoryBean instance but allow for subsequent type-based resolution.
  996 | 				resolveBeanClass(mbd, beanName);
  997 | 				return null;
  998 | 			}
  999 | 		}
 1000 | 
 1001 | 		try {
 1002 | 			BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
 1003 | 			if (bw != null) {
 1004 | 				return (FactoryBean<?>) bw.getWrappedInstance();
 1005 | 			}
 1006 | 			Object beanInstance = getSingleton(beanName, false);
 1007 | 			if (beanInstance instanceof FactoryBean<?> factoryBean) {
 1008 | 				return factoryBean;
 1009 | 			}
 1010 | 			if (isSingletonCurrentlyInCreation(beanName) ||
 1011 | 					(mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
 1012 | 				return null;
 1013 | 			}
 1014 | 
 1015 | 			Object instance;
 1016 | 			try {
 1017 | 				// Mark this bean as currently in creation, even if just partially.
 1018 | 				beforeSingletonCreation(beanName);
 1019 | 				// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
 1020 | 				instance = resolveBeforeInstantiation(beanName, mbd);
 1021 | 				if (instance == null) {
 1022 | 					bw = createBeanInstance(beanName, mbd, null);
 1023 | 					instance = bw.getWrappedInstance();
 1024 | 					this.factoryBeanInstanceCache.put(beanName, bw);
 1025 | 				}
 1026 | 			}
 1027 | 			catch (UnsatisfiedDependencyException ex) {
 1028 | 				// Don't swallow, probably misconfiguration...
 1029 | 				throw ex;
 1030 | 			}
 1031 | 			catch (BeanCreationException ex) {
 1032 | 				// Don't swallow a linkage error since it contains a full stacktrace on
 1033 | 				// first occurrence... and just a plain NoClassDefFoundError afterwards.
 1034 | 				if (ex.contains(LinkageError.class)) {
 1035 | 					throw ex;
 1036 | 				}
 1037 | 				// Instantiation failure, maybe too early...
 1038 | 				if (logger.isDebugEnabled()) {
 1039 | 					logger.debug("Bean creation exception on singleton FactoryBean type check: " + ex);
 1040 | 				}
 1041 | 				onSuppressedException(ex);
 1042 | 				return null;
 1043 | 			}
 1044 | 			finally {
 1045 | 				// Finished partial creation of this bean.
 1046 | 				afterSingletonCreation(beanName);
 1047 | 			}
 1048 | 
 1049 | 			return getFactoryBean(beanName, instance);
 1050 | 		}
 1051 | 		finally {
 1052 | 			this.singletonLock.unlock();
 1053 | 		}
 1054 | 	}
 1055 | 
 1056 | 	/**
 1057 | 	 * Obtain a "shortcut" non-singleton FactoryBean instance to use for a
 1058 | 	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
 1059 | 	 * @param beanName the name of the bean
 1060 | 	 * @param mbd the bean definition for the bean
 1061 | 	 * @return the FactoryBean instance, or {@code null} to indicate
 1062 | 	 * that we couldn't obtain a shortcut FactoryBean instance
 1063 | 	 */
 1064 | 	private @Nullable FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
 1065 | 		if (isPrototypeCurrentlyInCreation(beanName)) {
 1066 | 			return null;
 1067 | 		}
 1068 | 
 1069 | 		Object instance;
 1070 | 		try {
 1071 | 			// Mark this bean as currently in creation, even if just partially.
 1072 | 			beforePrototypeCreation(beanName);
 1073 | 			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
 1074 | 			instance = resolveBeforeInstantiation(beanName, mbd);
 1075 | 			if (instance == null) {
 1076 | 				BeanWrapper bw = createBeanInstance(beanName, mbd, null);
 1077 | 				instance = bw.getWrappedInstance();
 1078 | 			}
 1079 | 		}
 1080 | 		catch (UnsatisfiedDependencyException ex) {
 1081 | 			// Don't swallow, probably misconfiguration...
 1082 | 			throw ex;
 1083 | 		}
 1084 | 		catch (BeanCreationException ex) {
 1085 | 			// Instantiation failure, maybe too early...
 1086 | 			if (logger.isDebugEnabled()) {
 1087 | 				logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
 1088 | 			}
 1089 | 			onSuppressedException(ex);
 1090 | 			return null;
 1091 | 		}
 1092 | 		finally {
 1093 | 			// Finished partial creation of this bean.
 1094 | 			afterPrototypeCreation(beanName);
 1095 | 		}
 1096 | 
 1097 | 		return getFactoryBean(beanName, instance);
 1098 | 	}
 1099 | 
 1100 | 	/**
 1101 | 	 * Apply MergedBeanDefinitionPostProcessors to the specified bean definition,
 1102 | 	 * invoking their {@code postProcessMergedBeanDefinition} methods.
 1103 | 	 * @param mbd the merged bean definition for the bean
 1104 | 	 * @param beanType the actual type of the managed bean instance
 1105 | 	 * @param beanName the name of the bean
 1106 | 	 * @see MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
 1107 | 	 */
 1108 | 	protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
 1109 | 		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
 1110 | 			processor.postProcessMergedBeanDefinition(mbd, beanType, beanName);
 1111 | 		}
 1112 | 	}
 1113 | 
 1114 | 	/**
 1115 | 	 * Apply before-instantiation post-processors, resolving whether there is a
 1116 | 	 * before-instantiation shortcut for the specified bean.
 1117 | 	 * @param beanName the name of the bean
 1118 | 	 * @param mbd the bean definition for the bean
 1119 | 	 * @return the shortcut-determined bean instance, or {@code null} if none
 1120 | 	 */
 1121 | 	@SuppressWarnings("deprecation")
 1122 | 	protected @Nullable Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
 1123 | 		Object bean = null;
 1124 | 		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
 1125 | 			// Make sure bean class is actually resolved at this point.
 1126 | 			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
 1127 | 				Class<?> targetType = determineTargetType(beanName, mbd);
 1128 | 				if (targetType != null) {
 1129 | 					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
 1130 | 					if (bean != null) {
 1131 | 						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
 1132 | 					}
 1133 | 				}
 1134 | 			}
 1135 | 			mbd.beforeInstantiationResolved = (bean != null);
 1136 | 		}
 1137 | 		return bean;
 1138 | 	}
 1139 | 
 1140 | 	/**
 1141 | 	 * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
 1142 | 	 * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
 1143 | 	 * <p>Any returned object will be used as the bean instead of actually instantiating
 1144 | 	 * the target bean. A {@code null} return value from the post-processor will
 1145 | 	 * result in the target bean being instantiated.
 1146 | 	 * @param beanClass the class of the bean to be instantiated
 1147 | 	 * @param beanName the name of the bean
 1148 | 	 * @return the bean object to use instead of a default instance of the target bean, or {@code null}
 1149 | 	 * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
 1150 | 	 */
 1151 | 	protected @Nullable Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
 1152 | 		for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
 1153 | 			Object result = bp.postProcessBeforeInstantiation(beanClass, beanName);
 1154 | 			if (result != null) {
 1155 | 				return result;
 1156 | 			}
 1157 | 		}
 1158 | 		return null;
 1159 | 	}
 1160 | 
 1161 | 	/**
 1162 | 	 * Create a new instance for the specified bean, using an appropriate instantiation strategy:
 1163 | 	 * factory method, constructor autowiring, or simple instantiation.
 1164 | 	 * @param beanName the name of the bean
 1165 | 	 * @param mbd the bean definition for the bean
 1166 | 	 * @param args explicit arguments to use for constructor or factory method invocation
 1167 | 	 * @return a BeanWrapper for the new instance
 1168 | 	 * @see #obtainFromSupplier
 1169 | 	 * @see #instantiateUsingFactoryMethod
 1170 | 	 * @see #autowireConstructor
 1171 | 	 * @see #instantiateBean
 1172 | 	 */
 1173 | 	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args) {
 1174 | 		// Make sure bean class is actually resolved at this point.
 1175 | 		Class<?> beanClass = resolveBeanClass(mbd, beanName);
 1176 | 
 1177 | 		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
 1178 | 			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
 1179 | 					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
 1180 | 		}
 1181 | 
 1182 | 		if (args == null) {
 1183 | 			Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
 1184 | 			if (instanceSupplier != null) {
 1185 | 				return obtainFromSupplier(instanceSupplier, beanName, mbd);
 1186 | 			}
 1187 | 		}
 1188 | 
 1189 | 		if (mbd.getFactoryMethodName() != null) {
 1190 | 			return instantiateUsingFactoryMethod(beanName, mbd, args);
 1191 | 		}
 1192 | 
 1193 | 		// Shortcut when re-creating the same bean...
 1194 | 		boolean resolved = false;
 1195 | 		boolean autowireNecessary = false;
 1196 | 		if (args == null) {
 1197 | 			synchronized (mbd.constructorArgumentLock) {
 1198 | 				if (mbd.resolvedConstructorOrFactoryMethod != null) {
 1199 | 					resolved = true;
 1200 | 					autowireNecessary = mbd.constructorArgumentsResolved;
 1201 | 				}
 1202 | 			}
 1203 | 		}
 1204 | 		if (resolved) {
 1205 | 			if (autowireNecessary) {
 1206 | 				return autowireConstructor(beanName, mbd, null, null);
 1207 | 			}
 1208 | 			else {
 1209 | 				return instantiateBean(beanName, mbd);
 1210 | 			}
 1211 | 		}
 1212 | 
 1213 | 		// Candidate constructors for autowiring?
 1214 | 		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
 1215 | 		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
 1216 | 				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
 1217 | 			return autowireConstructor(beanName, mbd, ctors, args);
 1218 | 		}
 1219 | 
 1220 | 		// Preferred constructors for default construction?
 1221 | 		ctors = mbd.getPreferredConstructors();
 1222 | 		if (ctors != null) {
 1223 | 			return autowireConstructor(beanName, mbd, ctors, null);
 1224 | 		}
 1225 | 
 1226 | 		// No special handling: simply use no-arg constructor.
 1227 | 		return instantiateBean(beanName, mbd);
 1228 | 	}
 1229 | 
 1230 | 	/**
 1231 | 	 * Obtain a bean instance from the given supplier.
 1232 | 	 * @param supplier the configured supplier
 1233 | 	 * @param beanName the corresponding bean name
 1234 | 	 * @return a BeanWrapper for the new instance
 1235 | 	 */
 1236 | 	private BeanWrapper obtainFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd) {
 1237 | 		String outerBean = this.currentlyCreatedBean.get();
 1238 | 		this.currentlyCreatedBean.set(beanName);
 1239 | 		Object instance;
 1240 | 
 1241 | 		try {
 1242 | 			instance = obtainInstanceFromSupplier(supplier, beanName, mbd);
 1243 | 		}
 1244 | 		catch (Throwable ex) {
 1245 | 			if (ex instanceof BeanCreationException bce && beanName.equals(bce.getBeanName())) {
 1246 | 				throw bce;
 1247 | 			}
 1248 | 			throw new BeanCreationException(beanName, "Instantiation of supplied bean failed", ex);
 1249 | 		}
 1250 | 		finally {
 1251 | 			if (outerBean != null) {
 1252 | 				this.currentlyCreatedBean.set(outerBean);
 1253 | 			}
 1254 | 			else {
 1255 | 				this.currentlyCreatedBean.remove();
 1256 | 			}
 1257 | 		}
 1258 | 
 1259 | 		if (instance == null) {
 1260 | 			instance = new NullBean();
 1261 | 		}
 1262 | 		BeanWrapper bw = new BeanWrapperImpl(instance);
 1263 | 		initBeanWrapper(bw);
 1264 | 		return bw;
 1265 | 	}
 1266 | 
 1267 | 	/**
 1268 | 	 * Obtain a bean instance from the given supplier.
 1269 | 	 * @param supplier the configured supplier
 1270 | 	 * @param beanName the corresponding bean name
 1271 | 	 * @param mbd the bean definition for the bean
 1272 | 	 * @return the bean instance (possibly {@code null})
 1273 | 	 * @since 6.0.7
 1274 | 	 */
 1275 | 	protected @Nullable Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd)
 1276 | 			throws Exception {
 1277 | 
 1278 | 		if (supplier instanceof ThrowingSupplier<?> throwingSupplier) {
 1279 | 			return throwingSupplier.getWithException();
 1280 | 		}
 1281 | 		return supplier.get();
 1282 | 	}
 1283 | 
 1284 | 	/**
 1285 | 	 * Overridden in order to implicitly register the currently created bean as
 1286 | 	 * dependent on further beans getting programmatically retrieved during a
 1287 | 	 * {@link Supplier} callback.
 1288 | 	 * @since 5.0
 1289 | 	 * @see #obtainFromSupplier
 1290 | 	 */
 1291 | 	@Override
 1292 | 	protected Object getObjectForBeanInstance(Object beanInstance, @Nullable Class<?> requiredType,
 1293 | 			String name, String beanName, @Nullable RootBeanDefinition mbd) {
 1294 | 
 1295 | 		String currentlyCreatedBean = this.currentlyCreatedBean.get();
 1296 | 		if (currentlyCreatedBean != null) {
 1297 | 			registerDependentBean(beanName, currentlyCreatedBean);
 1298 | 		}
 1299 | 
 1300 | 		return super.getObjectForBeanInstance(beanInstance, requiredType, name, beanName, mbd);
 1301 | 	}
 1302 | 
 1303 | 	/**
 1304 | 	 * Determine candidate constructors to use for the given bean, checking all registered
 1305 | 	 * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
 1306 | 	 * @param beanClass the raw class of the bean
 1307 | 	 * @param beanName the name of the bean
 1308 | 	 * @return the candidate constructors, or {@code null} if none specified
 1309 | 	 * @throws org.springframework.beans.BeansException in case of errors
 1310 | 	 * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
 1311 | 	 */
 1312 | 	protected Constructor<?> @Nullable [] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
 1313 | 			throws BeansException {
 1314 | 
 1315 | 		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
 1316 | 			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
 1317 | 				Constructor<?>[] ctors = bp.determineCandidateConstructors(beanClass, beanName);
 1318 | 				if (ctors != null) {
 1319 | 					return ctors;
 1320 | 				}
 1321 | 			}
 1322 | 		}
 1323 | 		return null;
 1324 | 	}
 1325 | 
 1326 | 	/**
 1327 | 	 * Instantiate the given bean using its default constructor.
 1328 | 	 * @param beanName the name of the bean
 1329 | 	 * @param mbd the bean definition for the bean
 1330 | 	 * @return a BeanWrapper for the new instance
 1331 | 	 */
 1332 | 	protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
 1333 | 		try {
 1334 | 			Object beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
 1335 | 			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
 1336 | 			initBeanWrapper(bw);
 1337 | 			return bw;
 1338 | 		}
 1339 | 		catch (Throwable ex) {
 1340 | 			throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
 1341 | 		}
 1342 | 	}
 1343 | 
 1344 | 	/**
 1345 | 	 * Instantiate the bean using a named factory method. The method may be static, if the
 1346 | 	 * mbd parameter specifies a class, rather than a factoryBean, or an instance variable
 1347 | 	 * on a factory object itself configured using Dependency Injection.
 1348 | 	 * @param beanName the name of the bean
 1349 | 	 * @param mbd the bean definition for the bean
 1350 | 	 * @param explicitArgs argument values passed in programmatically via the getBean method,
 1351 | 	 * or {@code null} if none (implying the use of constructor argument values from bean definition)
 1352 | 	 * @return a BeanWrapper for the new instance
 1353 | 	 * @see #getBean(String, Object[])
 1354 | 	 */
 1355 | 	protected BeanWrapper instantiateUsingFactoryMethod(
 1356 | 			String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] explicitArgs) {
 1357 | 
 1358 | 		return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
 1359 | 	}
 1360 | 
 1361 | 	/**
 1362 | 	 * "autowire constructor" (with constructor arguments by type) behavior.
 1363 | 	 * Also applied if explicit constructor argument values are specified,
 1364 | 	 * matching all remaining arguments with beans from the bean factory.
 1365 | 	 * <p>This corresponds to constructor injection: In this mode, a Spring
 1366 | 	 * bean factory is able to host components that expect constructor-based
 1367 | 	 * dependency resolution.
 1368 | 	 * @param beanName the name of the bean
 1369 | 	 * @param mbd the bean definition for the bean
 1370 | 	 * @param ctors the chosen candidate constructors
 1371 | 	 * @param explicitArgs argument values passed in programmatically via the getBean method,
 1372 | 	 * or {@code null} if none (implying the use of constructor argument values from bean definition)
 1373 | 	 * @return a BeanWrapper for the new instance
 1374 | 	 */
 1375 | 	protected BeanWrapper autowireConstructor(
 1376 | 			String beanName, RootBeanDefinition mbd, Constructor<?> @Nullable [] ctors, @Nullable Object @Nullable [] explicitArgs) {
 1377 | 
 1378 | 		return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
 1379 | 	}
 1380 | 
 1381 | 	/**
 1382 | 	 * Populate the bean instance in the given BeanWrapper with the property values
 1383 | 	 * from the bean definition.
 1384 | 	 * @param beanName the name of the bean
 1385 | 	 * @param mbd the bean definition for the bean
 1386 | 	 * @param bw the BeanWrapper with bean instance
 1387 | 	 */
 1388 | 	protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
 1389 | 		if (bw == null) {
 1390 | 			if (mbd.hasPropertyValues()) {
 1391 | 				throw new BeanCreationException(
 1392 | 						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
 1393 | 			}
 1394 | 			else {
 1395 | 				// Skip property population phase for null instance.
 1396 | 				return;
 1397 | 			}
 1398 | 		}
 1399 | 
 1400 | 		if (bw.getWrappedClass().isRecord()) {
 1401 | 			if (mbd.hasPropertyValues()) {
 1402 | 				throw new BeanCreationException(
 1403 | 						mbd.getResourceDescription(), beanName, "Cannot apply property values to a record");
 1404 | 			}
 1405 | 			else {
 1406 | 				// Skip property population phase for records since they are immutable.
 1407 | 				return;
 1408 | 			}
 1409 | 		}
 1410 | 
 1411 | 		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
 1412 | 		// state of the bean before properties are set. This can be used, for example,
 1413 | 		// to support styles of field injection.
 1414 | 		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
 1415 | 			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
 1416 | 				if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
 1417 | 					return;
 1418 | 				}
 1419 | 			}
 1420 | 		}
 1421 | 
 1422 | 		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
 1423 | 
 1424 | 		int resolvedAutowireMode = mbd.getResolvedAutowireMode();
 1425 | 		if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
 1426 | 			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
 1427 | 			// Add property values based on autowire by name if applicable.
 1428 | 			if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
 1429 | 				autowireByName(beanName, mbd, bw, newPvs);
 1430 | 			}
 1431 | 			// Add property values based on autowire by type if applicable.
 1432 | 			if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
 1433 | 				autowireByType(beanName, mbd, bw, newPvs);
 1434 | 			}
 1435 | 			pvs = newPvs;
 1436 | 		}
 1437 | 		if (hasInstantiationAwareBeanPostProcessors()) {
 1438 | 			if (pvs == null) {
 1439 | 				pvs = mbd.getPropertyValues();
 1440 | 			}
 1441 | 			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
 1442 | 				PropertyValues pvsToUse = bp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
 1443 | 				if (pvsToUse == null) {
 1444 | 					return;
 1445 | 				}
 1446 | 				pvs = pvsToUse;
 1447 | 			}
 1448 | 		}
 1449 | 
 1450 | 		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
 1451 | 		if (needsDepCheck) {
 1452 | 			PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
 1453 | 			checkDependencies(beanName, mbd, filteredPds, pvs);
 1454 | 		}
 1455 | 
 1456 | 		if (pvs != null) {
 1457 | 			applyPropertyValues(beanName, mbd, bw, pvs);
 1458 | 		}
 1459 | 	}
 1460 | 
 1461 | 	/**
 1462 | 	 * Fill in any missing property values with references to
 1463 | 	 * other beans in this factory if autowire is set to "byName".
 1464 | 	 * @param beanName the name of the bean we're wiring up.
 1465 | 	 * Useful for debugging messages; not used functionally.
 1466 | 	 * @param mbd bean definition to update through autowiring
 1467 | 	 * @param bw the BeanWrapper from which we can obtain information about the bean
 1468 | 	 * @param pvs the PropertyValues to register wired objects with
 1469 | 	 */
 1470 | 	protected void autowireByName(
 1471 | 			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
 1472 | 
 1473 | 		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
 1474 | 		for (String propertyName : propertyNames) {
 1475 | 			if (containsBean(propertyName)) {
 1476 | 				Object bean = getBean(propertyName);
 1477 | 				pvs.add(propertyName, bean);
 1478 | 				registerDependentBean(propertyName, beanName);
 1479 | 				if (logger.isTraceEnabled()) {
 1480 | 					logger.trace("Added autowiring by name from bean name '" + beanName +
 1481 | 							"' via property '" + propertyName + "' to bean named '" + propertyName + "'");
 1482 | 				}
 1483 | 			}
 1484 | 			else {
 1485 | 				if (logger.isTraceEnabled()) {
 1486 | 					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
 1487 | 							"' by name: no matching bean found");
 1488 | 				}
 1489 | 			}
 1490 | 		}
 1491 | 	}
 1492 | 
 1493 | 	/**
 1494 | 	 * Abstract method defining "autowire by type" (bean properties by type) behavior.
 1495 | 	 * <p>This is like PicoContainer default, in which there must be exactly one bean
 1496 | 	 * of the property type in the bean factory. This makes bean factories simple to
 1497 | 	 * configure for small namespaces, but doesn't work as well as standard Spring
 1498 | 	 * behavior for bigger applications.
 1499 | 	 * @param beanName the name of the bean to autowire by type
 1500 | 	 * @param mbd the merged bean definition to update through autowiring
 1501 | 	 * @param bw the BeanWrapper from which we can obtain information about the bean
 1502 | 	 * @param pvs the PropertyValues to register wired objects with
 1503 | 	 */
 1504 | 	protected void autowireByType(
 1505 | 			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
 1506 | 
 1507 | 		TypeConverter converter = getCustomTypeConverter();
 1508 | 		if (converter == null) {
 1509 | 			converter = bw;
 1510 | 		}
 1511 | 
 1512 | 		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
 1513 | 		Set<String> autowiredBeanNames = new LinkedHashSet<>(propertyNames.length * 2);
 1514 | 		for (String propertyName : propertyNames) {
 1515 | 			try {
 1516 | 				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
 1517 | 				// Don't try autowiring by type for type Object: never makes sense,
 1518 | 				// even if it technically is an unsatisfied, non-simple property.
 1519 | 				if (Object.class != pd.getPropertyType()) {
 1520 | 					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
 1521 | 					// Do not allow eager init for type matching in case of a prioritized post-processor.
 1522 | 					boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
 1523 | 					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
 1524 | 					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
 1525 | 					if (autowiredArgument != null) {
 1526 | 						pvs.add(propertyName, autowiredArgument);
 1527 | 					}
 1528 | 					for (String autowiredBeanName : autowiredBeanNames) {
 1529 | 						if (containsBean(autowiredBeanName)) {
 1530 | 							registerDependentBean(autowiredBeanName, beanName);
 1531 | 						}
 1532 | 						if (logger.isTraceEnabled()) {
 1533 | 							logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
 1534 | 									propertyName + "' to bean named '" + autowiredBeanName + "'");
 1535 | 						}
 1536 | 					}
 1537 | 					autowiredBeanNames.clear();
 1538 | 				}
 1539 | 			}
 1540 | 			catch (BeansException ex) {
 1541 | 				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
 1542 | 			}
 1543 | 		}
 1544 | 	}
 1545 | 
 1546 | 
 1547 | 	/**
 1548 | 	 * Return an array of non-simple bean properties that are unsatisfied.
 1549 | 	 * These are probably unsatisfied references to other beans in the
 1550 | 	 * factory. Does not include simple properties like primitives or Strings.
 1551 | 	 * @param mbd the merged bean definition the bean was created with
 1552 | 	 * @param bw the BeanWrapper the bean was created with
 1553 | 	 * @return an array of bean property names
 1554 | 	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
 1555 | 	 */
 1556 | 	protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
 1557 | 		Set<String> result = new TreeSet<>();
 1558 | 		PropertyValues pvs = mbd.getPropertyValues();
 1559 | 		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
 1560 | 		for (PropertyDescriptor pd : pds) {
 1561 | 			if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
 1562 | 					!BeanUtils.isSimpleProperty(pd.getPropertyType())) {
 1563 | 				result.add(pd.getName());
 1564 | 			}
 1565 | 		}
 1566 | 		return StringUtils.toStringArray(result);
 1567 | 	}
 1568 | 
 1569 | 	/**
 1570 | 	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
 1571 | 	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
 1572 | 	 * @param bw the BeanWrapper the bean was created with
 1573 | 	 * @param cache whether to cache filtered PropertyDescriptors for the given bean Class
 1574 | 	 * @return the filtered PropertyDescriptors
 1575 | 	 * @see #isExcludedFromDependencyCheck
 1576 | 	 * @see #filterPropertyDescriptorsForDependencyCheck(org.springframework.beans.BeanWrapper)
 1577 | 	 */
 1578 | 	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
 1579 | 		PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
 1580 | 		if (filtered == null) {
 1581 | 			filtered = filterPropertyDescriptorsForDependencyCheck(bw);
 1582 | 			if (cache) {
 1583 | 				PropertyDescriptor[] existing =
 1584 | 						this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
 1585 | 				if (existing != null) {
 1586 | 					filtered = existing;
 1587 | 				}
 1588 | 			}
 1589 | 		}
 1590 | 		return filtered;
 1591 | 	}
 1592 | 
 1593 | 	/**
 1594 | 	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
 1595 | 	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
 1596 | 	 * @param bw the BeanWrapper the bean was created with
 1597 | 	 * @return the filtered PropertyDescriptors
 1598 | 	 * @see #isExcludedFromDependencyCheck
 1599 | 	 */
 1600 | 	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
 1601 | 		List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
 1602 | 		pds.removeIf(this::isExcludedFromDependencyCheck);
 1603 | 		return pds.toArray(new PropertyDescriptor[0]);
 1604 | 	}
 1605 | 
 1606 | 	/**
 1607 | 	 * Determine whether the given bean property is excluded from dependency checks.
 1608 | 	 * <p>This implementation excludes properties defined by CGLIB and
 1609 | 	 * properties whose type matches an ignored dependency type or which
 1610 | 	 * are defined by an ignored dependency interface.
 1611 | 	 * @param pd the PropertyDescriptor of the bean property
 1612 | 	 * @return whether the bean property is excluded
 1613 | 	 * @see #ignoreDependencyType(Class)
 1614 | 	 * @see #ignoreDependencyInterface(Class)
 1615 | 	 */
 1616 | 	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
 1617 | 		return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
 1618 | 				this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
 1619 | 				AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
 1620 | 	}
 1621 | 
 1622 | 	/**
 1623 | 	 * Perform a dependency check that all properties exposed have been set,
 1624 | 	 * if desired. Dependency checks can be objects (collaborating beans),
 1625 | 	 * simple (primitives and String), or all (both).
 1626 | 	 * @param beanName the name of the bean
 1627 | 	 * @param mbd the merged bean definition the bean was created with
 1628 | 	 * @param pds the relevant property descriptors for the target bean
 1629 | 	 * @param pvs the property values to be applied to the bean
 1630 | 	 * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
 1631 | 	 */
 1632 | 	protected void checkDependencies(
 1633 | 			String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, @Nullable PropertyValues pvs)
 1634 | 			throws UnsatisfiedDependencyException {
 1635 | 
 1636 | 		int dependencyCheck = mbd.getDependencyCheck();
 1637 | 		for (PropertyDescriptor pd : pds) {
 1638 | 			if (pd.getWriteMethod() != null && (pvs == null || !pvs.contains(pd.getName()))) {
 1639 | 				boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
 1640 | 				boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL) ||
 1641 | 						(isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
 1642 | 						(!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
 1643 | 				if (unsatisfied) {
 1644 | 					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
 1645 | 							"Set this property value or disable dependency checking for this bean.");
 1646 | 				}
 1647 | 			}
 1648 | 		}
 1649 | 	}
 1650 | 
 1651 | 	/**
 1652 | 	 * Apply the given property values, resolving any runtime references
 1653 | 	 * to other beans in this bean factory. Must use deep copy, so we
 1654 | 	 * don't permanently modify this property.
 1655 | 	 * @param beanName the bean name passed for better exception information
 1656 | 	 * @param mbd the merged bean definition
 1657 | 	 * @param bw the BeanWrapper wrapping the target object
 1658 | 	 * @param pvs the new property values
 1659 | 	 */
 1660 | 	protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
 1661 | 		if (pvs.isEmpty()) {
 1662 | 			return;
 1663 | 		}
 1664 | 
 1665 | 		MutablePropertyValues mpvs = null;
 1666 | 		List<PropertyValue> original;
 1667 | 
 1668 | 		if (pvs instanceof MutablePropertyValues _mpvs) {
 1669 | 			mpvs = _mpvs;
 1670 | 			if (mpvs.isConverted()) {
 1671 | 				// Shortcut: use the pre-converted values as-is.
 1672 | 				try {
 1673 | 					bw.setPropertyValues(mpvs);
 1674 | 					return;
 1675 | 				}
 1676 | 				catch (BeansException ex) {
 1677 | 					throw new BeanCreationException(
 1678 | 							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
 1679 | 				}
 1680 | 			}
 1681 | 			original = mpvs.getPropertyValueList();
 1682 | 		}
 1683 | 		else {
 1684 | 			original = Arrays.asList(pvs.getPropertyValues());
 1685 | 		}
 1686 | 
 1687 | 		TypeConverter converter = getCustomTypeConverter();
 1688 | 		if (converter == null) {
 1689 | 			converter = bw;
 1690 | 		}
 1691 | 		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);
 1692 | 
 1693 | 		// Create a deep copy, resolving any references for values.
 1694 | 		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
 1695 | 		boolean resolveNecessary = false;
 1696 | 		for (PropertyValue pv : original) {
 1697 | 			if (pv.isConverted()) {
 1698 | 				deepCopy.add(pv);
 1699 | 			}
 1700 | 			else {
 1701 | 				String propertyName = pv.getName();
 1702 | 				Object originalValue = pv.getValue();
 1703 | 				if (originalValue == AutowiredPropertyMarker.INSTANCE) {
 1704 | 					Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
 1705 | 					if (writeMethod == null) {
 1706 | 						throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
 1707 | 					}
 1708 | 					originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
 1709 | 				}
 1710 | 				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
 1711 | 				Object convertedValue = resolvedValue;
 1712 | 				boolean convertible = isConvertibleProperty(propertyName, bw);
 1713 | 				if (convertible) {
 1714 | 					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
 1715 | 				}
 1716 | 				// Possibly store converted value in merged bean definition,
 1717 | 				// in order to avoid re-conversion for every created bean instance.
 1718 | 				if (resolvedValue == originalValue) {
 1719 | 					if (convertible) {
 1720 | 						pv.setConvertedValue(convertedValue);
 1721 | 					}
 1722 | 					deepCopy.add(pv);
 1723 | 				}
 1724 | 				else if (convertible && originalValue instanceof TypedStringValue typedStringValue &&
 1725 | 						!typedStringValue.isDynamic() &&
 1726 | 						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
 1727 | 					pv.setConvertedValue(convertedValue);
 1728 | 					deepCopy.add(pv);
 1729 | 				}
 1730 | 				else {
 1731 | 					resolveNecessary = true;
 1732 | 					deepCopy.add(new PropertyValue(pv, convertedValue));
 1733 | 				}
 1734 | 			}
 1735 | 		}
 1736 | 		if (mpvs != null && !resolveNecessary) {
 1737 | 			mpvs.setConverted();
 1738 | 		}
 1739 | 
 1740 | 		// Set our (possibly massaged) deep copy.
 1741 | 		try {
 1742 | 			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
 1743 | 		}
 1744 | 		catch (BeansException ex) {
 1745 | 			throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
 1746 | 		}
 1747 | 	}
 1748 | 
 1749 | 	/**
 1750 | 	 * Determine whether the factory should cache a converted value for the given property.
 1751 | 	 */
 1752 | 	private boolean isConvertibleProperty(String propertyName, BeanWrapper bw) {
 1753 | 		try {
 1754 | 			return !isNestedOrIndexedProperty(propertyName) &&
 1755 | 					BeanUtils.hasUniqueWriteMethod(bw.getPropertyDescriptor(propertyName));
 1756 | 		}
 1757 | 		catch (InvalidPropertyException ex) {
 1758 | 			return false;
 1759 | 		}
 1760 | 	}
 1761 | 
 1762 | 	/**
 1763 | 	 * Check whether the given property path indicates an indexed or nested property.
 1764 | 	 */
 1765 | 	private static boolean isNestedOrIndexedProperty(String propertyName) {
 1766 | 		for (int i = 0; i < propertyName.length(); i++) {
 1767 | 			char ch = propertyName.charAt(i);
 1768 | 			if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR || ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
 1769 | 				return true;
 1770 | 			}
 1771 | 		}
 1772 | 		return false;
 1773 | 	}
 1774 | 
 1775 | 	/**
 1776 | 	 * Convert the given value for the specified target property.
 1777 | 	 */
 1778 | 	private @Nullable Object convertForProperty(
 1779 | 			@Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {
 1780 | 
 1781 | 		if (converter instanceof BeanWrapperImpl beanWrapper) {
 1782 | 			return beanWrapper.convertForProperty(value, propertyName);
 1783 | 		}
 1784 | 		else {
 1785 | 			PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
 1786 | 			MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
 1787 | 			return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
 1788 | 		}
 1789 | 	}
 1790 | 
 1791 | 
 1792 | 	/**
 1793 | 	 * Initialize the given bean instance, applying factory callbacks
 1794 | 	 * as well as init methods and bean post processors.
 1795 | 	 * <p>Called from {@link #createBean} for traditionally defined beans,
 1796 | 	 * and from {@link #initializeBean} for existing bean instances.
 1797 | 	 * @param beanName the bean name in the factory (for debugging purposes)
 1798 | 	 * @param bean the new bean instance we may need to initialize
 1799 | 	 * @param mbd the bean definition that the bean was created with
 1800 | 	 * (can also be {@code null}, if given an existing bean instance)
 1801 | 	 * @return the initialized bean instance (potentially wrapped)
 1802 | 	 * @see BeanNameAware
 1803 | 	 * @see BeanClassLoaderAware
 1804 | 	 * @see BeanFactoryAware
 1805 | 	 * @see #applyBeanPostProcessorsBeforeInitialization
 1806 | 	 * @see #invokeInitMethods
 1807 | 	 * @see #applyBeanPostProcessorsAfterInitialization
 1808 | 	 */
 1809 | 	@SuppressWarnings("deprecation")
 1810 | 	protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
 1811 | 		// Skip initialization of a NullBean
 1812 | 		if (bean.getClass() == NullBean.class) {
 1813 | 			return bean;
 1814 | 		}
 1815 | 
 1816 | 		invokeAwareMethods(beanName, bean);
 1817 | 
 1818 | 		Object wrappedBean = bean;
 1819 | 		if (mbd == null || !mbd.isSynthetic()) {
 1820 | 			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
 1821 | 		}
 1822 | 
 1823 | 		try {
 1824 | 			invokeInitMethods(beanName, wrappedBean, mbd);
 1825 | 		}
 1826 | 		catch (Throwable ex) {
 1827 | 			throw new BeanCreationException(
 1828 | 					(mbd != null ? mbd.getResourceDescription() : null), beanName, ex.getMessage(), ex);
 1829 | 		}
 1830 | 		if (mbd == null || !mbd.isSynthetic()) {
 1831 | 			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
 1832 | 		}
 1833 | 
 1834 | 		return wrappedBean;
 1835 | 	}
 1836 | 
 1837 | 	private void invokeAwareMethods(String beanName, Object bean) {
 1838 | 		if (bean instanceof Aware) {
 1839 | 			if (bean instanceof BeanNameAware beanNameAware) {
 1840 | 				beanNameAware.setBeanName(beanName);
 1841 | 			}
 1842 | 			if (bean instanceof BeanClassLoaderAware beanClassLoaderAware) {
 1843 | 				ClassLoader bcl = getBeanClassLoader();
 1844 | 				if (bcl != null) {
 1845 | 					beanClassLoaderAware.setBeanClassLoader(bcl);
 1846 | 				}
 1847 | 			}
 1848 | 			if (bean instanceof BeanFactoryAware beanFactoryAware) {
 1849 | 				beanFactoryAware.setBeanFactory(AbstractAutowireCapableBeanFactory.this);
 1850 | 			}
 1851 | 		}
 1852 | 	}
 1853 | 
 1854 | 	/**
 1855 | 	 * Give a bean a chance to initialize itself after all its properties are set,
 1856 | 	 * and a chance to know about its owning bean factory (this object).
 1857 | 	 * <p>This means checking whether the bean implements {@link InitializingBean}
 1858 | 	 * or defines any custom init methods, and invoking the necessary callback(s)
 1859 | 	 * if it does.
 1860 | 	 * @param beanName the bean name in the factory (for debugging purposes)
 1861 | 	 * @param bean the new bean instance we may need to initialize
 1862 | 	 * @param mbd the merged bean definition that the bean was created with
 1863 | 	 * (can also be {@code null}, if given an existing bean instance)
 1864 | 	 * @throws Throwable if thrown by init methods or by the invocation process
 1865 | 	 * @see #invokeCustomInitMethod
 1866 | 	 */
 1867 | 	protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
 1868 | 			throws Throwable {
 1869 | 
 1870 | 		boolean isInitializingBean = (bean instanceof InitializingBean);
 1871 | 		if (isInitializingBean && (mbd == null || !mbd.hasAnyExternallyManagedInitMethod("afterPropertiesSet"))) {
 1872 | 			if (logger.isTraceEnabled()) {
 1873 | 				logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
 1874 | 			}
 1875 | 			((InitializingBean) bean).afterPropertiesSet();
 1876 | 		}
 1877 | 
 1878 | 		if (mbd != null && bean.getClass() != NullBean.class) {
 1879 | 			String[] initMethodNames = mbd.getInitMethodNames();
 1880 | 			if (initMethodNames != null) {
 1881 | 				for (String initMethodName : initMethodNames) {
 1882 | 					if (StringUtils.hasLength(initMethodName) &&
 1883 | 							!(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
 1884 | 							!mbd.hasAnyExternallyManagedInitMethod(initMethodName)) {
 1885 | 						invokeCustomInitMethod(beanName, bean, mbd, initMethodName);
 1886 | 					}
 1887 | 				}
 1888 | 			}
 1889 | 		}
 1890 | 	}
 1891 | 
 1892 | 	/**
 1893 | 	 * Invoke the specified custom init method on the given bean.
 1894 | 	 * <p>Called by {@link #invokeInitMethods(String, Object, RootBeanDefinition)}.
 1895 | 	 * <p>Can be overridden in subclasses for custom resolution of init methods
 1896 | 	 * with arguments.
 1897 | 	 * @see #invokeInitMethods
 1898 | 	 */
 1899 | 	protected void invokeCustomInitMethod(String beanName, Object bean, RootBeanDefinition mbd, String initMethodName)
 1900 | 			throws Throwable {
 1901 | 
 1902 | 		Class<?> beanClass = bean.getClass();
 1903 | 		MethodDescriptor descriptor = MethodDescriptor.create(beanName, beanClass, initMethodName);
 1904 | 		String methodName = descriptor.methodName();
 1905 | 
 1906 | 		Method initMethod = (mbd.isNonPublicAccessAllowed() ?
 1907 | 				BeanUtils.findMethod(descriptor.declaringClass(), methodName) :
 1908 | 				ClassUtils.getMethodIfAvailable(beanClass, methodName));
 1909 | 
 1910 | 		if (initMethod == null) {
 1911 | 			if (mbd.isEnforceInitMethod()) {
 1912 | 				throw new BeanDefinitionValidationException("Could not find an init method named '" +
 1913 | 						methodName + "' on bean with name '" + beanName + "'");
 1914 | 			}
 1915 | 			else {
 1916 | 				if (logger.isTraceEnabled()) {
 1917 | 					logger.trace("No default init method named '" + methodName +
 1918 | 							"' found on bean with name '" + beanName + "'");
 1919 | 				}
 1920 | 				// Ignore non-existent default lifecycle methods.
 1921 | 				return;
 1922 | 			}
 1923 | 		}
 1924 | 
 1925 | 		if (logger.isTraceEnabled()) {
 1926 | 			logger.trace("Invoking init method '" + methodName + "' on bean with name '" + beanName + "'");
 1927 | 		}
 1928 | 		Method methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(initMethod, beanClass);
 1929 | 
 1930 | 		try {
 1931 | 			ReflectionUtils.makeAccessible(methodToInvoke);
 1932 | 			methodToInvoke.invoke(bean);
 1933 | 		}
 1934 | 		catch (InvocationTargetException ex) {
 1935 | 			throw ex.getTargetException();
 1936 | 		}
 1937 | 	}
 1938 | 
 1939 | 
 1940 | 	/**
 1941 | 	 * Applies the {@code postProcessAfterInitialization} callback of all
 1942 | 	 * registered BeanPostProcessors, giving them a chance to post-process the
 1943 | 	 * object obtained from FactoryBeans (for example, to auto-proxy them).
 1944 | 	 * @see #applyBeanPostProcessorsAfterInitialization
 1945 | 	 */
 1946 | 	@SuppressWarnings("deprecation")
 1947 | 	@Override
 1948 | 	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
 1949 | 		return applyBeanPostProcessorsAfterInitialization(object, beanName);
 1950 | 	}
 1951 | 
 1952 | 	/**
 1953 | 	 * Overridden to clear FactoryBean instance cache as well.
 1954 | 	 */
 1955 | 	@Override
 1956 | 	protected void removeSingleton(String beanName) {
 1957 | 		super.removeSingleton(beanName);
 1958 | 		this.factoryBeanInstanceCache.remove(beanName);
 1959 | 	}
 1960 | 
 1961 | 	/**
 1962 | 	 * Overridden to clear FactoryBean instance cache as well.
 1963 | 	 */
 1964 | 	@Override
 1965 | 	protected void clearSingletonCache() {
 1966 | 		super.clearSingletonCache();
 1967 | 		this.factoryBeanInstanceCache.clear();
 1968 | 	}
 1969 | 
 1970 | 	/**
 1971 | 	 * Expose the logger to collaborating delegates.
 1972 | 	 * @since 5.0.7
 1973 | 	 */
 1974 | 	Log getLogger() {
 1975 | 		return logger;
 1976 | 	}
 1977 | 
 1978 | 
 1979 | 	/**
 1980 | 	 * {@link RootBeanDefinition} subclass for {@code #createBean} calls with
 1981 | 	 * flexible selection of a Kotlin primary / single public / single non-public
 1982 | 	 * constructor candidate in addition to the default constructor.
 1983 | 	 * @see BeanUtils#getResolvableConstructor(Class)
 1984 | 	 */
 1985 | 	@SuppressWarnings("serial")
 1986 | 	private static class CreateFromClassBeanDefinition extends RootBeanDefinition {
 1987 | 
 1988 | 		public CreateFromClassBeanDefinition(Class<?> beanClass) {
 1989 | 			super(beanClass);
 1990 | 		}
 1991 | 
 1992 | 		public CreateFromClassBeanDefinition(CreateFromClassBeanDefinition original) {
 1993 | 			super(original);
 1994 | 		}
 1995 | 
 1996 | 		@Override
 1997 | 		public Constructor<?> @Nullable [] getPreferredConstructors() {
 1998 | 			Constructor<?>[] fromAttribute = super.getPreferredConstructors();
 1999 | 			if (fromAttribute != null) {
 2000 | 				return fromAttribute;
 2001 | 			}
 2002 | 			return ConstructorResolver.determinePreferredConstructors(getBeanClass());
 2003 | 		}
 2004 | 
 2005 | 		@Override
 2006 | 		public RootBeanDefinition cloneBeanDefinition() {
 2007 | 			return new CreateFromClassBeanDefinition(this);
 2008 | 		}
 2009 | 	}
 2010 | 
 2011 | 
 2012 | 	/**
 2013 | 	 * Special DependencyDescriptor variant for Spring's good old autowire="byType" mode.
 2014 | 	 * Always optional; never considering the parameter name for choosing a primary candidate.
 2015 | 	 */
 2016 | 	@SuppressWarnings("serial")
 2017 | 	private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {
 2018 | 
 2019 | 		public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
 2020 | 			super(methodParameter, false, eager);
 2021 | 		}
 2022 | 
 2023 | 		@Override
 2024 | 		public @Nullable String getDependencyName() {
 2025 | 			return null;
 2026 | 		}
 2027 | 	}
 2028 | 
 2029 | 
 2030 | 	/**
 2031 | 	 * {@link MethodCallback} used to find {@link FactoryBean} type information.
 2032 | 	 */
 2033 | 	private static class FactoryBeanMethodTypeFinder implements MethodCallback {
 2034 | 
 2035 | 		private final String factoryMethodName;
 2036 | 
 2037 | 		private ResolvableType result = ResolvableType.NONE;
 2038 | 
 2039 | 		FactoryBeanMethodTypeFinder(String factoryMethodName) {
 2040 | 			this.factoryMethodName = factoryMethodName;
 2041 | 		}
 2042 | 
 2043 | 		@Override
 2044 | 		public void doWith(Method method) throws IllegalArgumentException {
 2045 | 			if (isFactoryBeanMethod(method)) {
 2046 | 				ResolvableType returnType = ResolvableType.forMethodReturnType(method);
 2047 | 				ResolvableType candidate = returnType.as(FactoryBean.class).getGeneric();
 2048 | 				if (this.result == ResolvableType.NONE) {
 2049 | 					this.result = candidate;
 2050 | 				}
 2051 | 				else {
 2052 | 					Class<?> resolvedResult = this.result.resolve();
 2053 | 					Class<?> commonAncestor = ClassUtils.determineCommonAncestor(candidate.resolve(), resolvedResult);
 2054 | 					if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
 2055 | 						this.result = ResolvableType.forClass(commonAncestor);
 2056 | 					}
 2057 | 				}
 2058 | 			}
 2059 | 		}
 2060 | 
 2061 | 		private boolean isFactoryBeanMethod(Method method) {
 2062 | 			return (method.getName().equals(this.factoryMethodName) &&
 2063 | 					FactoryBean.class.isAssignableFrom(method.getReturnType()));
 2064 | 		}
 2065 | 
 2066 | 		ResolvableType getResult() {
 2067 | 			Class<?> resolved = this.result.resolve();
 2068 | 			boolean foundResult = resolved != null && resolved != Object.class;
 2069 | 			return (foundResult ? this.result : ResolvableType.NONE);
 2070 | 		}
 2071 | 	}
 2072 | 
 2073 | }
```
