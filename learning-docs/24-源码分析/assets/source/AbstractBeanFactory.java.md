<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/support/AbstractBeanFactory.java -->
<!-- 正文引用：[[源码:AbstractBeanFactory.java#L起-L止]] ；代码块头部：(src: AbstractBeanFactory.java#L起-L止) -->

# AbstractBeanFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/support/AbstractBeanFactory.java` |
| 行数 | 2143 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:48:29.769Z |

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
   19 | import java.beans.PropertyEditor;
   20 | import java.lang.reflect.Type;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.Collection;
   24 | import java.util.HashMap;
   25 | import java.util.LinkedHashMap;
   26 | import java.util.LinkedHashSet;
   27 | import java.util.List;
   28 | import java.util.Map;
   29 | import java.util.Set;
   30 | import java.util.concurrent.ConcurrentHashMap;
   31 | import java.util.concurrent.CopyOnWriteArrayList;
   32 | import java.util.function.Predicate;
   33 | import java.util.function.UnaryOperator;
   34 | 
   35 | import org.jspecify.annotations.Nullable;
   36 | 
   37 | import org.springframework.beans.BeanUtils;
   38 | import org.springframework.beans.BeanWrapper;
   39 | import org.springframework.beans.BeansException;
   40 | import org.springframework.beans.PropertyEditorRegistrar;
   41 | import org.springframework.beans.PropertyEditorRegistry;
   42 | import org.springframework.beans.PropertyEditorRegistrySupport;
   43 | import org.springframework.beans.SimpleTypeConverter;
   44 | import org.springframework.beans.TypeConverter;
   45 | import org.springframework.beans.TypeMismatchException;
   46 | import org.springframework.beans.factory.BeanCreationException;
   47 | import org.springframework.beans.factory.BeanCurrentlyInCreationException;
   48 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   49 | import org.springframework.beans.factory.BeanFactory;
   50 | import org.springframework.beans.factory.BeanFactoryUtils;
   51 | import org.springframework.beans.factory.BeanIsAbstractException;
   52 | import org.springframework.beans.factory.BeanIsNotAFactoryException;
   53 | import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
   54 | import org.springframework.beans.factory.CannotLoadBeanClassException;
   55 | import org.springframework.beans.factory.FactoryBean;
   56 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   57 | import org.springframework.beans.factory.SmartFactoryBean;
   58 | import org.springframework.beans.factory.config.BeanDefinition;
   59 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   60 | import org.springframework.beans.factory.config.BeanExpressionContext;
   61 | import org.springframework.beans.factory.config.BeanExpressionResolver;
   62 | import org.springframework.beans.factory.config.BeanPostProcessor;
   63 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   64 | import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
   65 | import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
   66 | import org.springframework.beans.factory.config.Scope;
   67 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   68 | import org.springframework.core.DecoratingClassLoader;
   69 | import org.springframework.core.NamedThreadLocal;
   70 | import org.springframework.core.ParameterizedTypeReference;
   71 | import org.springframework.core.ResolvableType;
   72 | import org.springframework.core.convert.ConversionService;
   73 | import org.springframework.core.log.LogMessage;
   74 | import org.springframework.core.metrics.ApplicationStartup;
   75 | import org.springframework.core.metrics.StartupStep;
   76 | import org.springframework.util.Assert;
   77 | import org.springframework.util.ClassUtils;
   78 | import org.springframework.util.CollectionUtils;
   79 | import org.springframework.util.ObjectUtils;
   80 | import org.springframework.util.StringUtils;
   81 | import org.springframework.util.StringValueResolver;
   82 | 
   83 | /**
   84 |  * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
   85 |  * implementations, providing the full capabilities of the
   86 |  * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
   87 |  * Does <i>not</i> assume a listable bean factory: can therefore also be used
   88 |  * as base class for bean factory implementations which obtain bean definitions
   89 |  * from some backend resource (where bean definition access is an expensive operation).
   90 |  *
   91 |  * <p>This class provides a singleton cache (through its base class
   92 |  * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
   93 |  * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
   94 |  * handling, aliases, bean definition merging for child bean definitions,
   95 |  * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
   96 |  * interface, custom destroy methods). Furthermore, it can manage a bean factory
   97 |  * hierarchy (delegating to the parent in case of an unknown bean), through implementing
   98 |  * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
   99 |  *
  100 |  * <p>The main template methods to be implemented by subclasses are
  101 |  * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
  102 |  * for a given bean name and creating a bean instance for a given bean definition,
  103 |  * respectively. Default implementations of those operations can be found in
  104 |  * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
  105 |  *
  106 |  * @author Rod Johnson
  107 |  * @author Juergen Hoeller
  108 |  * @author Costin Leau
  109 |  * @author Chris Beams
  110 |  * @author Phillip Webb
  111 |  * @author Sam Brannen
  112 |  * @since 15 April 2001
  113 |  * @see #getBeanDefinition
  114 |  * @see #createBean
  115 |  * @see AbstractAutowireCapableBeanFactory#createBean
  116 |  * @see DefaultListableBeanFactory#getBeanDefinition
  117 |  */
  118 | public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
  119 | 
  120 | 	/** Parent bean factory, for bean inheritance support. */
  121 | 	private @Nullable BeanFactory parentBeanFactory;
  122 | 
  123 | 	/** ClassLoader to resolve bean class names with, if necessary. */
  124 | 	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
  125 | 
  126 | 	/** ClassLoader to temporarily resolve bean class names with, if necessary. */
  127 | 	private @Nullable ClassLoader tempClassLoader;
  128 | 
  129 | 	/** Whether to cache bean metadata or rather reobtain it for every access. */
  130 | 	private boolean cacheBeanMetadata = true;
  131 | 
  132 | 	/** Resolution strategy for expressions in bean definition values. */
  133 | 	private @Nullable BeanExpressionResolver beanExpressionResolver;
  134 | 
  135 | 	/** Spring ConversionService to use instead of PropertyEditors. */
  136 | 	private @Nullable ConversionService conversionService;
  137 | 
  138 | 	/** Default PropertyEditorRegistrars to apply to the beans of this factory. */
  139 | 	private final Set<PropertyEditorRegistrar> defaultEditorRegistrars = new LinkedHashSet<>(4);
  140 | 
  141 | 	/** Custom PropertyEditorRegistrars to apply to the beans of this factory. */
  142 | 	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);
  143 | 
  144 | 	/** Custom PropertyEditors to apply to the beans of this factory. */
  145 | 	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);
  146 | 
  147 | 	/** A custom TypeConverter to use, overriding the default PropertyEditor mechanism. */
  148 | 	private @Nullable TypeConverter typeConverter;
  149 | 
  150 | 	/** String resolvers to apply, for example, to annotation attribute values. */
  151 | 	private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();
  152 | 
  153 | 	/** BeanPostProcessors to apply. */
  154 | 	private final List<BeanPostProcessor> beanPostProcessors = new BeanPostProcessorCacheAwareList();
  155 | 
  156 | 	/** Cache of pre-filtered post-processors. */
  157 | 	private @Nullable BeanPostProcessorCache beanPostProcessorCache;
  158 | 
  159 | 	/** Map from scope identifier String to corresponding Scope. */
  160 | 	private final Map<String, Scope> scopes = new LinkedHashMap<>(8);
  161 | 
  162 | 	/** Application startup metrics. */
  163 | 	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
  164 | 
  165 | 	/** Map from bean name to merged RootBeanDefinition. */
  166 | 	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);
  167 | 
  168 | 	/** Names of beans that have already been created at least once. */
  169 | 	private final Set<String> alreadyCreated = ConcurrentHashMap.newKeySet(256);
  170 | 
  171 | 	/** Names of beans that are currently in creation. */
  172 | 	private final ThreadLocal<Object> prototypesCurrentlyInCreation =
  173 | 			new NamedThreadLocal<>("Prototype beans currently in creation");
  174 | 
  175 | 
  176 | 	/**
  177 | 	 * Create a new AbstractBeanFactory.
  178 | 	 */
  179 | 	public AbstractBeanFactory() {
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * Create a new AbstractBeanFactory with the given parent.
  184 | 	 * @param parentBeanFactory parent bean factory, or {@code null} if none
  185 | 	 * @see #getBean
  186 | 	 */
  187 | 	public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
  188 | 		this.parentBeanFactory = parentBeanFactory;
  189 | 	}
  190 | 
  191 | 
  192 | 	//---------------------------------------------------------------------
  193 | 	// Implementation of BeanFactory interface
  194 | 	//---------------------------------------------------------------------
  195 | 
  196 | 	@Override
  197 | 	public Object getBean(String name) throws BeansException {
  198 | 		return doGetBean(name, null, null, false);
  199 | 	}
  200 | 
  201 | 	@Override
  202 | 	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
  203 | 		return doGetBean(name, requiredType, null, false);
  204 | 	}
  205 | 
  206 | 	@Override
  207 | 	@SuppressWarnings("unchecked")
  208 | 	public <T> T getBean(String name, ParameterizedTypeReference<T> typeReference) throws BeansException {
  209 | 		Object bean = getBean(name);
  210 | 		Type requiredType = typeReference.getType();
  211 | 		if (!isTypeMatch(name, ResolvableType.forType(requiredType), true)) {
  212 | 			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
  213 | 		}
  214 | 		return (T) bean;
  215 | 	}
  216 | 
  217 | 	@Override
  218 | 	public Object getBean(String name, @Nullable Object @Nullable ... args) throws BeansException {
  219 | 		return doGetBean(name, null, args, false);
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Return an instance, which may be shared or independent, of the specified bean.
  224 | 	 * @param name the name of the bean to retrieve
  225 | 	 * @param requiredType the required type of the bean to retrieve
  226 | 	 * @param args arguments to use when creating a bean instance using explicit arguments
  227 | 	 * (only applied when creating a new instance as opposed to retrieving an existing one)
  228 | 	 * @return an instance of the bean
  229 | 	 * @throws BeansException if the bean could not be created
  230 | 	 */
  231 | 	public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object @Nullable ... args)
  232 | 			throws BeansException {
  233 | 
  234 | 		return doGetBean(name, requiredType, args, false);
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Return an instance, which may be shared or independent, of the specified bean.
  239 | 	 * @param name the name of the bean to retrieve
  240 | 	 * @param requiredType the required type of the bean to retrieve
  241 | 	 * @param args arguments to use when creating a bean instance using explicit arguments
  242 | 	 * (only applied when creating a new instance as opposed to retrieving an existing one)
  243 | 	 * @param typeCheckOnly whether the instance is obtained for a type check,
  244 | 	 * not for actual use
  245 | 	 * @return an instance of the bean
  246 | 	 * @throws BeansException if the bean could not be created
  247 | 	 */
  248 | 	@SuppressWarnings("unchecked")
  249 | 	protected <T> T doGetBean(
  250 | 			String name, @Nullable Class<T> requiredType, @Nullable Object @Nullable [] args, boolean typeCheckOnly)
  251 | 			throws BeansException {
  252 | 
  253 | 		String beanName = transformedBeanName(name);
  254 | 		Object beanInstance;
  255 | 
  256 | 		// Eagerly check singleton cache for manually registered singletons.
  257 | 		Object sharedInstance = getSingleton(beanName);
  258 | 		if (sharedInstance != null && args == null) {
  259 | 			if (logger.isTraceEnabled()) {
  260 | 				if (isSingletonCurrentlyInCreation(beanName)) {
  261 | 					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
  262 | 							"' that is not fully initialized yet - a consequence of a circular reference");
  263 | 				}
  264 | 				else {
  265 | 					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
  266 | 				}
  267 | 			}
  268 | 			beanInstance = getObjectForBeanInstance(sharedInstance, requiredType, name, beanName, null);
  269 | 		}
  270 | 
  271 | 		else {
  272 | 			// Fail if we're already creating this bean instance:
  273 | 			// We're assumably within a circular reference.
  274 | 			if (isPrototypeCurrentlyInCreation(beanName)) {
  275 | 				throw new BeanCurrentlyInCreationException(beanName);
  276 | 			}
  277 | 
  278 | 			// Check if bean definition exists in this factory.
  279 | 			BeanFactory parentBeanFactory = getParentBeanFactory();
  280 | 			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
  281 | 				// Not found -> check parent.
  282 | 				String nameToLookup = originalBeanName(name);
  283 | 				if (parentBeanFactory instanceof AbstractBeanFactory abf) {
  284 | 					return abf.doGetBean(nameToLookup, requiredType, args, typeCheckOnly);
  285 | 				}
  286 | 				else if (args != null) {
  287 | 					// Delegation to parent with explicit args.
  288 | 					return (T) parentBeanFactory.getBean(nameToLookup, args);
  289 | 				}
  290 | 				else if (requiredType != null) {
  291 | 					// No args -> delegate to standard getBean method.
  292 | 					return parentBeanFactory.getBean(nameToLookup, requiredType);
  293 | 				}
  294 | 				else {
  295 | 					return (T) parentBeanFactory.getBean(nameToLookup);
  296 | 				}
  297 | 			}
  298 | 
  299 | 			if (!typeCheckOnly) {
  300 | 				markBeanAsCreated(beanName);
  301 | 			}
  302 | 
  303 | 			StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate")
  304 | 					.tag("beanName", name);
  305 | 			try {
  306 | 				if (requiredType != null) {
  307 | 					beanCreation.tag("beanType", requiredType::toString);
  308 | 				}
  309 | 				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  310 | 				checkMergedBeanDefinition(mbd, beanName, args);
  311 | 
  312 | 				// Guarantee initialization of beans that the current bean depends on.
  313 | 				String[] dependsOn = mbd.getDependsOn();
  314 | 				if (dependsOn != null) {
  315 | 					for (String dep : dependsOn) {
  316 | 						if (isDependent(beanName, dep)) {
  317 | 							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
  318 | 									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
  319 | 						}
  320 | 						registerDependentBean(dep, beanName);
  321 | 						try {
  322 | 							getBean(dep);
  323 | 						}
  324 | 						catch (NoSuchBeanDefinitionException ex) {
  325 | 							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
  326 | 									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
  327 | 						}
  328 | 						catch (BeanCreationException ex) {
  329 | 							if (requiredType != null) {
  330 | 								// Wrap exception with current bean metadata but only if specifically
  331 | 								// requested (indicated by required type), not for depends-on cascades.
  332 | 								throw new BeanCreationException(mbd.getResourceDescription(), beanName,
  333 | 										"Failed to initialize dependency '" + ex.getBeanName() + "' of " +
  334 | 												requiredType.getSimpleName() + " bean '" + beanName + "': " +
  335 | 												ex.getMessage(), ex);
  336 | 							}
  337 | 							throw ex;
  338 | 						}
  339 | 					}
  340 | 				}
  341 | 
  342 | 				// Create bean instance.
  343 | 				if (mbd.isSingleton()) {
  344 | 					sharedInstance = getSingleton(beanName, () -> {
  345 | 						try {
  346 | 							return createBean(beanName, mbd, args);
  347 | 						}
  348 | 						catch (BeansException ex) {
  349 | 							// Explicitly remove instance from singleton cache: It might have been put there
  350 | 							// eagerly by the creation process, to allow for circular reference resolution.
  351 | 							// Also remove any beans that received a temporary reference to the bean.
  352 | 							destroySingleton(beanName);
  353 | 							throw ex;
  354 | 						}
  355 | 					});
  356 | 					beanInstance = getObjectForBeanInstance(sharedInstance, requiredType, name, beanName, mbd);
  357 | 				}
  358 | 
  359 | 				else if (mbd.isPrototype()) {
  360 | 					// It's a prototype -> create a new instance.
  361 | 					Object prototypeInstance = null;
  362 | 					try {
  363 | 						beforePrototypeCreation(beanName);
  364 | 						prototypeInstance = createBean(beanName, mbd, args);
  365 | 					}
  366 | 					finally {
  367 | 						afterPrototypeCreation(beanName);
  368 | 					}
  369 | 					beanInstance = getObjectForBeanInstance(prototypeInstance, requiredType, name, beanName, mbd);
  370 | 				}
  371 | 
  372 | 				else {
  373 | 					String scopeName = mbd.getScope();
  374 | 					if (!StringUtils.hasLength(scopeName)) {
  375 | 						throw new IllegalStateException("No scope name defined for bean '" + beanName + "'");
  376 | 					}
  377 | 					Scope scope = this.scopes.get(scopeName);
  378 | 					if (scope == null) {
  379 | 						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
  380 | 					}
  381 | 					try {
  382 | 						Object scopedInstance = scope.get(beanName, () -> {
  383 | 							beforePrototypeCreation(beanName);
  384 | 							try {
  385 | 								return createBean(beanName, mbd, args);
  386 | 							}
  387 | 							finally {
  388 | 								afterPrototypeCreation(beanName);
  389 | 							}
  390 | 						});
  391 | 						beanInstance = getObjectForBeanInstance(scopedInstance, requiredType, name, beanName, mbd);
  392 | 					}
  393 | 					catch (IllegalStateException ex) {
  394 | 						throw new ScopeNotActiveException(beanName, scopeName, ex);
  395 | 					}
  396 | 				}
  397 | 			}
  398 | 			catch (BeansException ex) {
  399 | 				beanCreation.tag("exception", ex.getClass().toString());
  400 | 				beanCreation.tag("message", String.valueOf(ex.getMessage()));
  401 | 				cleanupAfterBeanCreationFailure(beanName);
  402 | 				throw ex;
  403 | 			}
  404 | 			finally {
  405 | 				beanCreation.end();
  406 | 				if (!isCacheBeanMetadata()) {
  407 | 					clearMergedBeanDefinition(beanName);
  408 | 				}
  409 | 			}
  410 | 		}
  411 | 
  412 | 		return adaptBeanInstance(name, beanInstance, requiredType);
  413 | 	}
  414 | 
  415 | 	@SuppressWarnings("unchecked")
  416 | 	<T> T adaptBeanInstance(String name, Object bean, @Nullable Class<?> requiredType) {
  417 | 		// Check if required type matches the type of the actual bean instance.
  418 | 		if (requiredType != null && !requiredType.isInstance(bean)) {
  419 | 			try {
  420 | 				Object convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
  421 | 				if (convertedBean == null) {
  422 | 					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
  423 | 				}
  424 | 				return (T) convertedBean;
  425 | 			}
  426 | 			catch (TypeMismatchException ex) {
  427 | 				if (logger.isTraceEnabled()) {
  428 | 					logger.trace("Failed to convert bean '" + name + "' to required type '" +
  429 | 							requiredType.getTypeName() + "'", ex);
  430 | 				}
  431 | 				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
  432 | 			}
  433 | 		}
  434 | 		return (T) bean;
  435 | 	}
  436 | 
  437 | 	@Override
  438 | 	public boolean containsBean(String name) {
  439 | 		String beanName = transformedBeanName(name);
  440 | 		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
  441 | 			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
  442 | 		}
  443 | 		// Not found -> check parent.
  444 | 		BeanFactory parentBeanFactory = getParentBeanFactory();
  445 | 		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
  446 | 	}
  447 | 
  448 | 	@Override
  449 | 	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
  450 | 		String beanName = transformedBeanName(name);
  451 | 
  452 | 		Object beanInstance = getSingleton(beanName, false);
  453 | 		if (beanInstance != null) {
  454 | 			if (beanInstance instanceof FactoryBean<?> factoryBean) {
  455 | 				return (BeanFactoryUtils.isFactoryDereference(name) || factoryBean.isSingleton());
  456 | 			}
  457 | 			else {
  458 | 				return !BeanFactoryUtils.isFactoryDereference(name);
  459 | 			}
  460 | 		}
  461 | 
  462 | 		// No singleton instance found -> check bean definition.
  463 | 		BeanFactory parentBeanFactory = getParentBeanFactory();
  464 | 		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
  465 | 			// No bean definition found in this factory -> delegate to parent.
  466 | 			return parentBeanFactory.isSingleton(originalBeanName(name));
  467 | 		}
  468 | 
  469 | 		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  470 | 
  471 | 		// In case of FactoryBean, return singleton status of created object if not a dereference.
  472 | 		if (mbd.isSingleton()) {
  473 | 			if (isFactoryBean(beanName, mbd)) {
  474 | 				if (BeanFactoryUtils.isFactoryDereference(name)) {
  475 | 					return true;
  476 | 				}
  477 | 				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
  478 | 				return factoryBean.isSingleton();
  479 | 			}
  480 | 			else {
  481 | 				return !BeanFactoryUtils.isFactoryDereference(name);
  482 | 			}
  483 | 		}
  484 | 		else {
  485 | 			return false;
  486 | 		}
  487 | 	}
  488 | 
  489 | 	@Override
  490 | 	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
  491 | 		String beanName = transformedBeanName(name);
  492 | 
  493 | 		BeanFactory parentBeanFactory = getParentBeanFactory();
  494 | 		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
  495 | 			// No bean definition found in this factory -> delegate to parent.
  496 | 			return parentBeanFactory.isPrototype(originalBeanName(name));
  497 | 		}
  498 | 
  499 | 		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  500 | 		if (mbd.isPrototype()) {
  501 | 			// In case of FactoryBean, return singleton status of created object if not a dereference.
  502 | 			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
  503 | 		}
  504 | 
  505 | 		// Singleton or scoped - not a prototype.
  506 | 		// However, FactoryBean may still produce a prototype object...
  507 | 		if (BeanFactoryUtils.isFactoryDereference(name)) {
  508 | 			return false;
  509 | 		}
  510 | 		if (isFactoryBean(beanName, mbd)) {
  511 | 			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
  512 | 			return ((fb instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isPrototype()) ||
  513 | 					!fb.isSingleton());
  514 | 		}
  515 | 		else {
  516 | 			return false;
  517 | 		}
  518 | 	}
  519 | 
  520 | 	@Override
  521 | 	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
  522 | 		return isTypeMatch(name, typeToMatch, true);
  523 | 	}
  524 | 
  525 | 	/**
  526 | 	 * Internal extended variant of {@link #isTypeMatch(String, ResolvableType)}
  527 | 	 * to check whether the bean with the given name matches the specified type. Allow
  528 | 	 * additional constraints to be applied to ensure that beans are not created early.
  529 | 	 * @param name the name of the bean to query
  530 | 	 * @param typeToMatch the type to match against (as a {@code ResolvableType})
  531 | 	 * @return {@code true} if the bean type matches, {@code false} if it
  532 | 	 * doesn't match or cannot be determined yet
  533 | 	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
  534 | 	 * @since 5.2
  535 | 	 * @see #getBean
  536 | 	 * @see #getType
  537 | 	 */
  538 | 	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
  539 | 			throws NoSuchBeanDefinitionException {
  540 | 
  541 | 		String beanName = transformedBeanName(name);
  542 | 		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);
  543 | 
  544 | 		// Check manually registered singletons.
  545 | 		Object beanInstance = getSingleton(beanName, false);
  546 | 		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
  547 | 
  548 | 			// Determine target for FactoryBean match if necessary.
  549 | 			if (beanInstance instanceof FactoryBean<?> factoryBean) {
  550 | 				if (!isFactoryDereference) {
  551 | 					Class<?> classToMatch = typeToMatch.resolve();
  552 | 					if (factoryBean instanceof SmartFactoryBean<?> smartFactoryBean &&
  553 | 							classToMatch != null && smartFactoryBean.supportsType(classToMatch)) {
  554 | 						return true;
  555 | 					}
  556 | 					Class<?> type = getTypeForFactoryBean(factoryBean);
  557 | 					if (type == null) {
  558 | 						return false;
  559 | 					}
  560 | 					if (typeToMatch.isAssignableFrom(type)) {
  561 | 						return true;
  562 | 					}
  563 | 					else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
  564 | 						RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  565 | 						ResolvableType targetType = mbd.targetType;
  566 | 						if (targetType == null) {
  567 | 							targetType = mbd.factoryMethodReturnType;
  568 | 						}
  569 | 						if (targetType == null) {
  570 | 							return false;
  571 | 						}
  572 | 						Class<?> targetClass = targetType.resolve();
  573 | 						if (targetClass != null && FactoryBean.class.isAssignableFrom(targetClass)) {
  574 | 							if (classToMatch != null && !FactoryBean.class.isAssignableFrom(classToMatch) &&
  575 | 									!classToMatch.isAssignableFrom(targetType.toClass())) {
  576 | 								return typeToMatch.isAssignableFrom(targetType.getGeneric());
  577 | 							}
  578 | 						}
  579 | 						else {
  580 | 							return typeToMatch.isAssignableFrom(targetType);
  581 | 						}
  582 | 					}
  583 | 					return false;
  584 | 				}
  585 | 			}
  586 | 			else if (isFactoryDereference) {
  587 | 				return false;
  588 | 			}
  589 | 
  590 | 			// Actual matching against bean instance...
  591 | 			if (typeToMatch.isInstance(beanInstance)) {
  592 | 				// Direct match for exposed instance?
  593 | 				return true;
  594 | 			}
  595 | 			else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
  596 | 				// Generics potentially only match on the target class, not on the proxy...
  597 | 				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  598 | 				Class<?> targetType = mbd.getTargetType();
  599 | 				if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
  600 | 					// Check raw class match as well, making sure it's exposed on the proxy.
  601 | 					Class<?> classToMatch = typeToMatch.resolve();
  602 | 					if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
  603 | 						return false;
  604 | 					}
  605 | 					if (typeToMatch.isAssignableFrom(targetType)) {
  606 | 						return true;
  607 | 					}
  608 | 				}
  609 | 				ResolvableType resolvableType = mbd.targetType;
  610 | 				if (resolvableType == null) {
  611 | 					resolvableType = mbd.factoryMethodReturnType;
  612 | 				}
  613 | 				return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
  614 | 			}
  615 | 			else {
  616 | 				return false;
  617 | 			}
  618 | 		}
  619 | 		else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
  620 | 			// null instance registered
  621 | 			return false;
  622 | 		}
  623 | 
  624 | 		// No singleton instance found -> check bean definition.
  625 | 		BeanFactory parentBeanFactory = getParentBeanFactory();
  626 | 		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
  627 | 			// No bean definition found in this factory -> delegate to parent.
  628 | 			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
  629 | 		}
  630 | 
  631 | 		// Retrieve corresponding bean definition.
  632 | 		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  633 | 		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
  634 | 
  635 | 		// Set up the types that we want to match against
  636 | 		Class<?> classToMatch = typeToMatch.resolve();
  637 | 		if (classToMatch == null) {
  638 | 			classToMatch = FactoryBean.class;
  639 | 		}
  640 | 		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
  641 | 				new Class<?>[] {classToMatch} : new Class<?>[] {FactoryBean.class, classToMatch});
  642 | 
  643 | 		// Attempt to predict the bean type
  644 | 		Class<?> predictedType = null;
  645 | 
  646 | 		// We're looking for a regular reference, but we're a factory bean that has
  647 | 		// a decorated bean definition. The target bean should be the same type
  648 | 		// as FactoryBean would ultimately return.
  649 | 		if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
  650 | 			// We should only attempt if the user explicitly set lazy-init to true
  651 | 			// and we know the merged bean definition is for a factory bean.
  652 | 			if (!mbd.isLazyInit() || allowFactoryBeanInit) {
  653 | 				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
  654 | 				Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
  655 | 				if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
  656 | 					predictedType = targetType;
  657 | 				}
  658 | 			}
  659 | 		}
  660 | 
  661 | 		// If we couldn't use the target type, try regular prediction.
  662 | 		if (predictedType == null) {
  663 | 			predictedType = predictBeanType(beanName, mbd, typesToMatch);
  664 | 			if (predictedType == null) {
  665 | 				return false;
  666 | 			}
  667 | 		}
  668 | 
  669 | 		// Attempt to get the actual ResolvableType for the bean.
  670 | 		ResolvableType beanType = null;
  671 | 
  672 | 		// If it's a FactoryBean, we want to look at what it creates, not the factory class.
  673 | 		if (FactoryBean.class.isAssignableFrom(predictedType)) {
  674 | 			if (beanInstance == null && !isFactoryDereference) {
  675 | 				beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
  676 | 				predictedType = beanType.resolve();
  677 | 				if (predictedType == null) {
  678 | 					return false;
  679 | 				}
  680 | 			}
  681 | 		}
  682 | 		else if (isFactoryDereference) {
  683 | 			// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
  684 | 			// type, but we nevertheless are being asked to dereference a FactoryBean...
  685 | 			// Let's check the original bean class and proceed with it if it is a FactoryBean.
  686 | 			predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
  687 | 			if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
  688 | 				return false;
  689 | 			}
  690 | 		}
  691 | 
  692 | 		// We don't have an exact type but if bean definition target type or the factory
  693 | 		// method return type matches the predicted type then we can use that.
  694 | 		if (beanType == null) {
  695 | 			ResolvableType definedType = mbd.targetType;
  696 | 			if (definedType == null) {
  697 | 				definedType = mbd.factoryMethodReturnType;
  698 | 			}
  699 | 			if (definedType != null && definedType.resolve() == predictedType) {
  700 | 				beanType = definedType;
  701 | 			}
  702 | 		}
  703 | 
  704 | 		// If we have a bean type use it so that generics are considered
  705 | 		if (beanType != null) {
  706 | 			return typeToMatch.isAssignableFrom(beanType);
  707 | 		}
  708 | 
  709 | 		// If we don't have a bean type, fallback to the predicted type
  710 | 		return typeToMatch.isAssignableFrom(predictedType);
  711 | 	}
  712 | 
  713 | 	@Override
  714 | 	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
  715 | 		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
  716 | 	}
  717 | 
  718 | 	@Override
  719 | 	public @Nullable Class<?> getType(String name) throws NoSuchBeanDefinitionException {
  720 | 		return getType(name, true);
  721 | 	}
  722 | 
  723 | 	@Override
  724 | 	public @Nullable Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
  725 | 		String beanName = transformedBeanName(name);
  726 | 
  727 | 		// Check manually registered singletons.
  728 | 		Object beanInstance = getSingleton(beanName, false);
  729 | 		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
  730 | 			if (beanInstance instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
  731 | 				return getTypeForFactoryBean(factoryBean);
  732 | 			}
  733 | 			else {
  734 | 				return beanInstance.getClass();
  735 | 			}
  736 | 		}
  737 | 
  738 | 		// No singleton instance found -> check bean definition.
  739 | 		BeanFactory parentBeanFactory = getParentBeanFactory();
  740 | 		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
  741 | 			// No bean definition found in this factory -> delegate to parent.
  742 | 			return parentBeanFactory.getType(originalBeanName(name));
  743 | 		}
  744 | 
  745 | 		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
  746 | 		Class<?> beanClass = predictBeanType(beanName, mbd);
  747 | 
  748 | 		if (beanClass != null) {
  749 | 			// Check bean class whether we're dealing with a FactoryBean.
  750 | 			if (FactoryBean.class.isAssignableFrom(beanClass)) {
  751 | 				if (!BeanFactoryUtils.isFactoryDereference(name)) {
  752 | 					// If it's a FactoryBean, we want to look at what it creates, not at the factory class.
  753 | 					beanClass = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit).resolve();
  754 | 				}
  755 | 			}
  756 | 			else if (BeanFactoryUtils.isFactoryDereference(name)) {
  757 | 				return null;
  758 | 			}
  759 | 		}
  760 | 
  761 | 		if (beanClass == null) {
  762 | 			// Check decorated bean definition, if any: We assume it'll be easier
  763 | 			// to determine the decorated bean's type than the proxy's type.
  764 | 			BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
  765 | 			if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
  766 | 				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
  767 | 				Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
  768 | 				if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
  769 | 					return targetClass;
  770 | 				}
  771 | 			}
  772 | 		}
  773 | 
  774 | 		return beanClass;
  775 | 	}
  776 | 
  777 | 	@Override
  778 | 	public String[] getAliases(String name) {
  779 | 		String beanName = transformedBeanName(name);
  780 | 		List<String> aliases = new ArrayList<>();
  781 | 		boolean hasFactoryPrefix = (!name.isEmpty() && name.charAt(0) == BeanFactory.FACTORY_BEAN_PREFIX_CHAR);
  782 | 		String fullBeanName = beanName;
  783 | 		if (hasFactoryPrefix) {
  784 | 			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
  785 | 		}
  786 | 		if (!fullBeanName.equals(name)) {
  787 | 			aliases.add(fullBeanName);
  788 | 		}
  789 | 		String[] retrievedAliases = super.getAliases(beanName);
  790 | 		String prefix = (hasFactoryPrefix ? FACTORY_BEAN_PREFIX : "");
  791 | 		for (String retrievedAlias : retrievedAliases) {
  792 | 			String alias = prefix + retrievedAlias;
  793 | 			if (!alias.equals(name)) {
  794 | 				aliases.add(alias);
  795 | 			}
  796 | 		}
  797 | 		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
  798 | 			BeanFactory parentBeanFactory = getParentBeanFactory();
  799 | 			if (parentBeanFactory != null) {
  800 | 				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
  801 | 			}
  802 | 		}
  803 | 		return StringUtils.toStringArray(aliases);
  804 | 	}
  805 | 
  806 | 
  807 | 	//---------------------------------------------------------------------
  808 | 	// Implementation of HierarchicalBeanFactory interface
  809 | 	//---------------------------------------------------------------------
  810 | 
  811 | 	@Override
  812 | 	public @Nullable BeanFactory getParentBeanFactory() {
  813 | 		return this.parentBeanFactory;
  814 | 	}
  815 | 
  816 | 	@Override
  817 | 	public boolean containsLocalBean(String name) {
  818 | 		String beanName = transformedBeanName(name);
  819 | 		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
  820 | 				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
  821 | 	}
  822 | 
  823 | 
  824 | 	//---------------------------------------------------------------------
  825 | 	// Implementation of ConfigurableBeanFactory interface
  826 | 	//---------------------------------------------------------------------
  827 | 
  828 | 	@Override
  829 | 	public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
  830 | 		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
  831 | 			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
  832 | 		}
  833 | 		if (this == parentBeanFactory) {
  834 | 			throw new IllegalStateException("Cannot set parent bean factory to self");
  835 | 		}
  836 | 		this.parentBeanFactory = parentBeanFactory;
  837 | 	}
  838 | 
  839 | 	@Override
  840 | 	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
  841 | 		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
  842 | 	}
  843 | 
  844 | 	@Override
  845 | 	public @Nullable ClassLoader getBeanClassLoader() {
  846 | 		return this.beanClassLoader;
  847 | 	}
  848 | 
  849 | 	@Override
  850 | 	public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
  851 | 		this.tempClassLoader = tempClassLoader;
  852 | 	}
  853 | 
  854 | 	@Override
  855 | 	public @Nullable ClassLoader getTempClassLoader() {
  856 | 		return this.tempClassLoader;
  857 | 	}
  858 | 
  859 | 	@Override
  860 | 	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
  861 | 		this.cacheBeanMetadata = cacheBeanMetadata;
  862 | 	}
  863 | 
  864 | 	@Override
  865 | 	public boolean isCacheBeanMetadata() {
  866 | 		return this.cacheBeanMetadata;
  867 | 	}
  868 | 
  869 | 	@Override
  870 | 	public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
  871 | 		this.beanExpressionResolver = resolver;
  872 | 	}
  873 | 
  874 | 	@Override
  875 | 	public @Nullable BeanExpressionResolver getBeanExpressionResolver() {
  876 | 		return this.beanExpressionResolver;
  877 | 	}
  878 | 
  879 | 	@Override
  880 | 	public void setConversionService(@Nullable ConversionService conversionService) {
  881 | 		this.conversionService = conversionService;
  882 | 	}
  883 | 
  884 | 	@Override
  885 | 	public @Nullable ConversionService getConversionService() {
  886 | 		return this.conversionService;
  887 | 	}
  888 | 
  889 | 	@Override
  890 | 	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
  891 | 		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
  892 | 		if (registrar.overridesDefaultEditors()) {
  893 | 			this.defaultEditorRegistrars.add(registrar);
  894 | 		}
  895 | 		else {
  896 | 			this.propertyEditorRegistrars.add(registrar);
  897 | 		}
  898 | 	}
  899 | 
  900 | 	/**
  901 | 	 * Return the set of PropertyEditorRegistrars.
  902 | 	 */
  903 | 	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
  904 | 		return this.propertyEditorRegistrars;
  905 | 	}
  906 | 
  907 | 	@Override
  908 | 	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
  909 | 		Assert.notNull(requiredType, "Required type must not be null");
  910 | 		Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
  911 | 		this.customEditors.put(requiredType, propertyEditorClass);
  912 | 	}
  913 | 
  914 | 	@Override
  915 | 	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
  916 | 		registerCustomEditors(registry);
  917 | 	}
  918 | 
  919 | 	/**
  920 | 	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
  921 | 	 */
  922 | 	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
  923 | 		return this.customEditors;
  924 | 	}
  925 | 
  926 | 	@Override
  927 | 	public void setTypeConverter(TypeConverter typeConverter) {
  928 | 		this.typeConverter = typeConverter;
  929 | 	}
  930 | 
  931 | 	/**
  932 | 	 * Return the custom TypeConverter to use, if any.
  933 | 	 * @return the custom TypeConverter, or {@code null} if none specified
  934 | 	 */
  935 | 	protected @Nullable TypeConverter getCustomTypeConverter() {
  936 | 		return this.typeConverter;
  937 | 	}
  938 | 
  939 | 	@Override
  940 | 	public TypeConverter getTypeConverter() {
  941 | 		TypeConverter customConverter = getCustomTypeConverter();
  942 | 		if (customConverter != null) {
  943 | 			return customConverter;
  944 | 		}
  945 | 		else {
  946 | 			// Build default TypeConverter, registering custom editors.
  947 | 			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
  948 | 			typeConverter.setConversionService(getConversionService());
  949 | 			registerCustomEditors(typeConverter);
  950 | 			return typeConverter;
  951 | 		}
  952 | 	}
  953 | 
  954 | 	@Override
  955 | 	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
  956 | 		Assert.notNull(valueResolver, "StringValueResolver must not be null");
  957 | 		this.embeddedValueResolvers.add(valueResolver);
  958 | 	}
  959 | 
  960 | 	@Override
  961 | 	public boolean hasEmbeddedValueResolver() {
  962 | 		return !this.embeddedValueResolvers.isEmpty();
  963 | 	}
  964 | 
  965 | 	@Override
  966 | 	public @Nullable String resolveEmbeddedValue(@Nullable String value) {
  967 | 		if (value == null) {
  968 | 			return null;
  969 | 		}
  970 | 		String result = value;
  971 | 		for (StringValueResolver resolver : this.embeddedValueResolvers) {
  972 | 			result = resolver.resolveStringValue(result);
  973 | 			if (result == null) {
  974 | 				return null;
  975 | 			}
  976 | 		}
  977 | 		return result;
  978 | 	}
  979 | 
  980 | 	@Override
  981 | 	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
  982 | 		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
  983 | 		synchronized (this.beanPostProcessors) {
  984 | 			// Remove from old position, if any
  985 | 			this.beanPostProcessors.remove(beanPostProcessor);
  986 | 			// Add to end of list
  987 | 			this.beanPostProcessors.add(beanPostProcessor);
  988 | 		}
  989 | 	}
  990 | 
  991 | 	/**
  992 | 	 * Add new BeanPostProcessors that will get applied to beans created
  993 | 	 * by this factory. To be invoked during factory configuration.
  994 | 	 * @since 5.3
  995 | 	 * @see #addBeanPostProcessor
  996 | 	 */
  997 | 	public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
  998 | 		synchronized (this.beanPostProcessors) {
  999 | 			// Remove from old position, if any
 1000 | 			this.beanPostProcessors.removeAll(beanPostProcessors);
 1001 | 			// Add to end of list
 1002 | 			this.beanPostProcessors.addAll(beanPostProcessors);
 1003 | 		}
 1004 | 	}
 1005 | 
 1006 | 	@Override
 1007 | 	public int getBeanPostProcessorCount() {
 1008 | 		return this.beanPostProcessors.size();
 1009 | 	}
 1010 | 
 1011 | 	/**
 1012 | 	 * Return the list of BeanPostProcessors that will get applied
 1013 | 	 * to beans created with this factory.
 1014 | 	 */
 1015 | 	public List<BeanPostProcessor> getBeanPostProcessors() {
 1016 | 		return this.beanPostProcessors;
 1017 | 	}
 1018 | 
 1019 | 	/**
 1020 | 	 * Return the internal cache of pre-filtered post-processors,
 1021 | 	 * freshly (re-)building it if necessary.
 1022 | 	 * @since 5.3
 1023 | 	 */
 1024 | 	BeanPostProcessorCache getBeanPostProcessorCache() {
 1025 | 		synchronized (this.beanPostProcessors) {
 1026 | 			BeanPostProcessorCache bppCache = this.beanPostProcessorCache;
 1027 | 			if (bppCache == null) {
 1028 | 				bppCache = new BeanPostProcessorCache();
 1029 | 				for (BeanPostProcessor bpp : this.beanPostProcessors) {
 1030 | 					if (bpp instanceof InstantiationAwareBeanPostProcessor instantiationAwareBpp) {
 1031 | 						bppCache.instantiationAware.add(instantiationAwareBpp);
 1032 | 						if (bpp instanceof SmartInstantiationAwareBeanPostProcessor smartInstantiationAwareBpp) {
 1033 | 							bppCache.smartInstantiationAware.add(smartInstantiationAwareBpp);
 1034 | 						}
 1035 | 					}
 1036 | 					if (bpp instanceof DestructionAwareBeanPostProcessor destructionAwareBpp) {
 1037 | 						bppCache.destructionAware.add(destructionAwareBpp);
 1038 | 					}
 1039 | 					if (bpp instanceof MergedBeanDefinitionPostProcessor mergedBeanDefBpp) {
 1040 | 						bppCache.mergedDefinition.add(mergedBeanDefBpp);
 1041 | 					}
 1042 | 				}
 1043 | 				this.beanPostProcessorCache = bppCache;
 1044 | 			}
 1045 | 			return bppCache;
 1046 | 		}
 1047 | 	}
 1048 | 
 1049 | 	private void resetBeanPostProcessorCache() {
 1050 | 		synchronized (this.beanPostProcessors) {
 1051 | 			this.beanPostProcessorCache = null;
 1052 | 		}
 1053 | 	}
 1054 | 
 1055 | 	/**
 1056 | 	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
 1057 | 	 * that will get applied to singleton beans on creation.
 1058 | 	 * @see #addBeanPostProcessor
 1059 | 	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
 1060 | 	 */
 1061 | 	protected boolean hasInstantiationAwareBeanPostProcessors() {
 1062 | 		return !getBeanPostProcessorCache().instantiationAware.isEmpty();
 1063 | 	}
 1064 | 
 1065 | 	/**
 1066 | 	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
 1067 | 	 * that will get applied to singleton beans on shutdown.
 1068 | 	 * @see #addBeanPostProcessor
 1069 | 	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 1070 | 	 */
 1071 | 	protected boolean hasDestructionAwareBeanPostProcessors() {
 1072 | 		return !getBeanPostProcessorCache().destructionAware.isEmpty();
 1073 | 	}
 1074 | 
 1075 | 	@Override
 1076 | 	public void registerScope(String scopeName, Scope scope) {
 1077 | 		Assert.notNull(scopeName, "Scope identifier must not be null");
 1078 | 		Assert.notNull(scope, "Scope must not be null");
 1079 | 		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
 1080 | 			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
 1081 | 		}
 1082 | 		Scope previous = this.scopes.put(scopeName, scope);
 1083 | 		if (previous != null && previous != scope) {
 1084 | 			if (logger.isDebugEnabled()) {
 1085 | 				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
 1086 | 			}
 1087 | 		}
 1088 | 		else {
 1089 | 			if (logger.isTraceEnabled()) {
 1090 | 				logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
 1091 | 			}
 1092 | 		}
 1093 | 	}
 1094 | 
 1095 | 	@Override
 1096 | 	public String[] getRegisteredScopeNames() {
 1097 | 		return StringUtils.toStringArray(this.scopes.keySet());
 1098 | 	}
 1099 | 
 1100 | 	@Override
 1101 | 	public @Nullable Scope getRegisteredScope(String scopeName) {
 1102 | 		Assert.notNull(scopeName, "Scope identifier must not be null");
 1103 | 		return this.scopes.get(scopeName);
 1104 | 	}
 1105 | 
 1106 | 	@Override
 1107 | 	public void setApplicationStartup(ApplicationStartup applicationStartup) {
 1108 | 		Assert.notNull(applicationStartup, "ApplicationStartup must not be null");
 1109 | 		this.applicationStartup = applicationStartup;
 1110 | 	}
 1111 | 
 1112 | 	@Override
 1113 | 	public ApplicationStartup getApplicationStartup() {
 1114 | 		return this.applicationStartup;
 1115 | 	}
 1116 | 
 1117 | 	@Override
 1118 | 	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
 1119 | 		Assert.notNull(otherFactory, "BeanFactory must not be null");
 1120 | 		setBeanClassLoader(otherFactory.getBeanClassLoader());
 1121 | 		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
 1122 | 		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
 1123 | 		setConversionService(otherFactory.getConversionService());
 1124 | 		if (otherFactory instanceof AbstractBeanFactory otherAbstractFactory) {
 1125 | 			this.defaultEditorRegistrars.addAll(otherAbstractFactory.defaultEditorRegistrars);
 1126 | 			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
 1127 | 			this.customEditors.putAll(otherAbstractFactory.customEditors);
 1128 | 			this.typeConverter = otherAbstractFactory.typeConverter;
 1129 | 			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
 1130 | 			this.scopes.putAll(otherAbstractFactory.scopes);
 1131 | 		}
 1132 | 		else {
 1133 | 			setTypeConverter(otherFactory.getTypeConverter());
 1134 | 			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
 1135 | 			for (String scopeName : otherScopeNames) {
 1136 | 				this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
 1137 | 			}
 1138 | 		}
 1139 | 	}
 1140 | 
 1141 | 	/**
 1142 | 	 * Return a 'merged' BeanDefinition for the given bean name,
 1143 | 	 * merging a child bean definition with its parent if necessary.
 1144 | 	 * <p>This {@code getMergedBeanDefinition} considers bean definition
 1145 | 	 * in ancestors as well.
 1146 | 	 * @param name the name of the bean to retrieve the merged definition for
 1147 | 	 * (may be an alias)
 1148 | 	 * @return a (potentially merged) RootBeanDefinition for the given bean
 1149 | 	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
 1150 | 	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
 1151 | 	 */
 1152 | 	@Override
 1153 | 	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
 1154 | 		String beanName = transformedBeanName(name);
 1155 | 		// Efficiently check whether bean definition exists in this factory.
 1156 | 		if (getParentBeanFactory() instanceof ConfigurableBeanFactory parent && !containsBeanDefinition(beanName)) {
 1157 | 			return parent.getMergedBeanDefinition(beanName);
 1158 | 		}
 1159 | 		// Resolve merged bean definition locally.
 1160 | 		return getMergedLocalBeanDefinition(beanName);
 1161 | 	}
 1162 | 
 1163 | 	@Override
 1164 | 	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
 1165 | 		String beanName = transformedBeanName(name);
 1166 | 		Object beanInstance = getSingleton(beanName, false);
 1167 | 		if (beanInstance != null) {
 1168 | 			return (beanInstance instanceof FactoryBean);
 1169 | 		}
 1170 | 		// No singleton instance found -> check bean definition.
 1171 | 		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory cbf) {
 1172 | 			// No bean definition found in this factory -> delegate to parent.
 1173 | 			return cbf.isFactoryBean(name);
 1174 | 		}
 1175 | 		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
 1176 | 	}
 1177 | 
 1178 | 	@Override
 1179 | 	public boolean isActuallyInCreation(String beanName) {
 1180 | 		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
 1181 | 	}
 1182 | 
 1183 | 	/**
 1184 | 	 * Return whether the specified prototype bean is currently in creation
 1185 | 	 * (within the current thread).
 1186 | 	 * @param beanName the name of the bean
 1187 | 	 */
 1188 | 	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
 1189 | 		Object curVal = this.prototypesCurrentlyInCreation.get();
 1190 | 		return (curVal != null &&
 1191 | 				(curVal.equals(beanName) || (curVal instanceof Set<?> set && set.contains(beanName))));
 1192 | 	}
 1193 | 
 1194 | 	/**
 1195 | 	 * Callback before prototype creation.
 1196 | 	 * <p>The default implementation registers the prototype as currently in creation.
 1197 | 	 * @param beanName the name of the prototype about to be created
 1198 | 	 * @see #isPrototypeCurrentlyInCreation
 1199 | 	 */
 1200 | 	@SuppressWarnings("unchecked")
 1201 | 	protected void beforePrototypeCreation(String beanName) {
 1202 | 		Object curVal = this.prototypesCurrentlyInCreation.get();
 1203 | 		if (curVal == null) {
 1204 | 			this.prototypesCurrentlyInCreation.set(beanName);
 1205 | 		}
 1206 | 		else if (curVal instanceof String strValue) {
 1207 | 			Set<String> beanNameSet = CollectionUtils.newHashSet(2);
 1208 | 			beanNameSet.add(strValue);
 1209 | 			beanNameSet.add(beanName);
 1210 | 			this.prototypesCurrentlyInCreation.set(beanNameSet);
 1211 | 		}
 1212 | 		else {
 1213 | 			Set<String> beanNameSet = (Set<String>) curVal;
 1214 | 			beanNameSet.add(beanName);
 1215 | 		}
 1216 | 	}
 1217 | 
 1218 | 	/**
 1219 | 	 * Callback after prototype creation.
 1220 | 	 * <p>The default implementation marks the prototype as not in creation anymore.
 1221 | 	 * @param beanName the name of the prototype that has been created
 1222 | 	 * @see #isPrototypeCurrentlyInCreation
 1223 | 	 */
 1224 | 	@SuppressWarnings("unchecked")
 1225 | 	protected void afterPrototypeCreation(String beanName) {
 1226 | 		Object curVal = this.prototypesCurrentlyInCreation.get();
 1227 | 		if (curVal instanceof String) {
 1228 | 			this.prototypesCurrentlyInCreation.remove();
 1229 | 		}
 1230 | 		else if (curVal instanceof Set<?> beanNameSet) {
 1231 | 			beanNameSet.remove(beanName);
 1232 | 			if (beanNameSet.isEmpty()) {
 1233 | 				this.prototypesCurrentlyInCreation.remove();
 1234 | 			}
 1235 | 		}
 1236 | 	}
 1237 | 
 1238 | 	@Override
 1239 | 	public void destroyBean(String beanName, Object beanInstance) {
 1240 | 		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
 1241 | 	}
 1242 | 
 1243 | 	/**
 1244 | 	 * Destroy the given bean instance (usually a prototype instance
 1245 | 	 * obtained from this factory) according to the given bean definition.
 1246 | 	 * @param beanName the name of the bean definition
 1247 | 	 * @param bean the bean instance to destroy
 1248 | 	 * @param mbd the merged bean definition
 1249 | 	 */
 1250 | 	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
 1251 | 		new DisposableBeanAdapter(
 1252 | 				bean, beanName, mbd, getBeanPostProcessorCache().destructionAware).destroy();
 1253 | 	}
 1254 | 
 1255 | 	@Override
 1256 | 	public void destroyScopedBean(String beanName) {
 1257 | 		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
 1258 | 		if (mbd.isSingleton() || mbd.isPrototype()) {
 1259 | 			throw new IllegalArgumentException(
 1260 | 					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
 1261 | 		}
 1262 | 		String scopeName = mbd.getScope();
 1263 | 		Scope scope = this.scopes.get(scopeName);
 1264 | 		if (scope == null) {
 1265 | 			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
 1266 | 		}
 1267 | 		Object bean = scope.remove(beanName);
 1268 | 		if (bean != null) {
 1269 | 			destroyBean(beanName, bean, mbd);
 1270 | 		}
 1271 | 	}
 1272 | 
 1273 | 
 1274 | 	//---------------------------------------------------------------------
 1275 | 	// Implementation methods
 1276 | 	//---------------------------------------------------------------------
 1277 | 
 1278 | 	/**
 1279 | 	 * Return the bean name, stripping out the factory dereference prefix if necessary,
 1280 | 	 * and resolving aliases to canonical names.
 1281 | 	 * @param name the user-specified name
 1282 | 	 * @return the transformed bean name
 1283 | 	 */
 1284 | 	protected String transformedBeanName(String name) {
 1285 | 		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
 1286 | 	}
 1287 | 
 1288 | 	/**
 1289 | 	 * Determine the original bean name, resolving locally defined aliases to canonical names.
 1290 | 	 * @param name the user-specified name
 1291 | 	 * @return the original bean name
 1292 | 	 */
 1293 | 	protected String originalBeanName(String name) {
 1294 | 		String beanName = transformedBeanName(name);
 1295 | 		if (!name.isEmpty() && name.charAt(0) == BeanFactory.FACTORY_BEAN_PREFIX_CHAR) {
 1296 | 			beanName = FACTORY_BEAN_PREFIX + beanName;
 1297 | 		}
 1298 | 		return beanName;
 1299 | 	}
 1300 | 
 1301 | 	/**
 1302 | 	 * Initialize the given BeanWrapper with the custom editors registered
 1303 | 	 * with this factory. To be called for BeanWrappers that will create
 1304 | 	 * and populate bean instances.
 1305 | 	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
 1306 | 	 * Can be overridden in subclasses.
 1307 | 	 * @param bw the BeanWrapper to initialize
 1308 | 	 */
 1309 | 	protected void initBeanWrapper(BeanWrapper bw) {
 1310 | 		bw.setConversionService(getConversionService());
 1311 | 		registerCustomEditors(bw);
 1312 | 	}
 1313 | 
 1314 | 	/**
 1315 | 	 * Initialize the given PropertyEditorRegistry with the custom editors
 1316 | 	 * that have been registered with this BeanFactory.
 1317 | 	 * <p>To be called for BeanWrappers that will create and populate bean
 1318 | 	 * instances, and for SimpleTypeConverter used for constructor argument
 1319 | 	 * and factory method type conversion.
 1320 | 	 * @param registry the PropertyEditorRegistry to initialize
 1321 | 	 */
 1322 | 	protected void registerCustomEditors(PropertyEditorRegistry registry) {
 1323 | 		if (registry instanceof PropertyEditorRegistrySupport registrySupport) {
 1324 | 			registrySupport.useConfigValueEditors();
 1325 | 			if (!this.defaultEditorRegistrars.isEmpty()) {
 1326 | 				// Optimization: lazy overriding of default editors only when needed
 1327 | 				registrySupport.setDefaultEditorRegistrar(new BeanFactoryDefaultEditorRegistrar());
 1328 | 			}
 1329 | 		}
 1330 | 		else if (!this.defaultEditorRegistrars.isEmpty()) {
 1331 | 			// Fallback: proactive overriding of default editors
 1332 | 			applyEditorRegistrars(registry, this.defaultEditorRegistrars);
 1333 | 		}
 1334 | 
 1335 | 		if (!this.propertyEditorRegistrars.isEmpty()) {
 1336 | 			applyEditorRegistrars(registry, this.propertyEditorRegistrars);
 1337 | 		}
 1338 | 		if (!this.customEditors.isEmpty()) {
 1339 | 			this.customEditors.forEach((requiredType, editorClass) ->
 1340 | 					registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
 1341 | 		}
 1342 | 	}
 1343 | 
 1344 | 	private void applyEditorRegistrars(PropertyEditorRegistry registry, Set<PropertyEditorRegistrar> registrars) {
 1345 | 		for (PropertyEditorRegistrar registrar : registrars) {
 1346 | 			try {
 1347 | 				registrar.registerCustomEditors(registry);
 1348 | 			}
 1349 | 			catch (BeanCreationException ex) {
 1350 | 				Throwable rootCause = ex.getMostSpecificCause();
 1351 | 				if (rootCause instanceof BeanCurrentlyInCreationException bce) {
 1352 | 					String bceBeanName = bce.getBeanName();
 1353 | 					if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
 1354 | 						if (logger.isDebugEnabled()) {
 1355 | 							logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
 1356 | 									"] failed because it tried to obtain currently created bean '" +
 1357 | 									ex.getBeanName() + "': " + ex.getMessage());
 1358 | 						}
 1359 | 						onSuppressedException(ex);
 1360 | 						return;
 1361 | 					}
 1362 | 				}
 1363 | 				throw ex;
 1364 | 			}
 1365 | 		}
 1366 | 	}
 1367 | 
 1368 | 	/**
 1369 | 	 * Return a merged RootBeanDefinition, traversing the parent bean definition
 1370 | 	 * if the specified bean corresponds to a child bean definition.
 1371 | 	 * @param beanName the name of the bean to retrieve the merged definition for
 1372 | 	 * @return a (potentially merged) RootBeanDefinition for the given bean
 1373 | 	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
 1374 | 	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
 1375 | 	 */
 1376 | 	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
 1377 | 		// Quick check on the concurrent map first, with minimal locking.
 1378 | 		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
 1379 | 		if (mbd != null && !mbd.stale) {
 1380 | 			return mbd;
 1381 | 		}
 1382 | 		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
 1383 | 	}
 1384 | 
 1385 | 	/**
 1386 | 	 * Return a RootBeanDefinition for the given top-level bean, by merging with
 1387 | 	 * the parent if the given bean's definition is a child bean definition.
 1388 | 	 * @param beanName the name of the bean definition
 1389 | 	 * @param bd the original bean definition (Root/ChildBeanDefinition)
 1390 | 	 * @return a (potentially merged) RootBeanDefinition for the given bean
 1391 | 	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
 1392 | 	 */
 1393 | 	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
 1394 | 			throws BeanDefinitionStoreException {
 1395 | 
 1396 | 		return getMergedBeanDefinition(beanName, bd, null);
 1397 | 	}
 1398 | 
 1399 | 	/**
 1400 | 	 * Return a RootBeanDefinition for the given bean, by merging with the
 1401 | 	 * parent if the given bean's definition is a child bean definition.
 1402 | 	 * @param beanName the name of the bean definition
 1403 | 	 * @param bd the original bean definition (Root/ChildBeanDefinition)
 1404 | 	 * @param containingBd the containing bean definition in case of inner bean,
 1405 | 	 * or {@code null} in case of a top-level bean
 1406 | 	 * @return a (potentially merged) RootBeanDefinition for the given bean
 1407 | 	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
 1408 | 	 */
 1409 | 	protected RootBeanDefinition getMergedBeanDefinition(
 1410 | 			String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
 1411 | 			throws BeanDefinitionStoreException {
 1412 | 
 1413 | 		synchronized (this.mergedBeanDefinitions) {
 1414 | 			RootBeanDefinition mbd = null;
 1415 | 			RootBeanDefinition previous = null;
 1416 | 
 1417 | 			// Check with full lock now in order to enforce the same merged instance.
 1418 | 			if (containingBd == null) {
 1419 | 				mbd = this.mergedBeanDefinitions.get(beanName);
 1420 | 			}
 1421 | 
 1422 | 			if (mbd == null || mbd.stale) {
 1423 | 				previous = mbd;
 1424 | 				if (bd.getParentName() == null) {
 1425 | 					// Use copy of given root bean definition.
 1426 | 					if (bd instanceof RootBeanDefinition rootBeanDef) {
 1427 | 						mbd = rootBeanDef.cloneBeanDefinition();
 1428 | 					}
 1429 | 					else {
 1430 | 						mbd = new RootBeanDefinition(bd);
 1431 | 					}
 1432 | 				}
 1433 | 				else {
 1434 | 					// Child bean definition: needs to be merged with parent.
 1435 | 					BeanDefinition pbd;
 1436 | 					try {
 1437 | 						String parentBeanName = transformedBeanName(bd.getParentName());
 1438 | 						if (!beanName.equals(parentBeanName)) {
 1439 | 							pbd = getMergedBeanDefinition(parentBeanName);
 1440 | 						}
 1441 | 						else {
 1442 | 							if (getParentBeanFactory() instanceof ConfigurableBeanFactory parent) {
 1443 | 								pbd = parent.getMergedBeanDefinition(parentBeanName);
 1444 | 							}
 1445 | 							else {
 1446 | 								throw new NoSuchBeanDefinitionException(parentBeanName,
 1447 | 										"Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
 1448 | 												"': cannot be resolved without a ConfigurableBeanFactory parent");
 1449 | 							}
 1450 | 						}
 1451 | 					}
 1452 | 					catch (NoSuchBeanDefinitionException ex) {
 1453 | 						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
 1454 | 								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
 1455 | 					}
 1456 | 					// Deep copy with overridden values.
 1457 | 					mbd = new RootBeanDefinition(pbd);
 1458 | 					mbd.overrideFrom(bd);
 1459 | 				}
 1460 | 
 1461 | 				// Set default singleton scope, if not configured before.
 1462 | 				if (!StringUtils.hasLength(mbd.getScope())) {
 1463 | 					mbd.setScope(SCOPE_SINGLETON);
 1464 | 				}
 1465 | 
 1466 | 				// A bean contained in a non-singleton bean cannot be a singleton itself.
 1467 | 				// Let's correct this on the fly here, since this might be the result of
 1468 | 				// parent-child merging for the outer bean, in which case the original inner bean
 1469 | 				// definition will not have inherited the merged outer bean's singleton status.
 1470 | 				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
 1471 | 					mbd.setScope(containingBd.getScope());
 1472 | 				}
 1473 | 
 1474 | 				// Cache the merged bean definition for the time being
 1475 | 				// (it might still get re-merged later on in order to pick up metadata changes)
 1476 | 				if (containingBd == null && (isCacheBeanMetadata() || isBeanEligibleForMetadataCaching(beanName))) {
 1477 | 					cacheMergedBeanDefinition(mbd, beanName);
 1478 | 				}
 1479 | 			}
 1480 | 			if (previous != null) {
 1481 | 				copyRelevantMergedBeanDefinitionCaches(previous, mbd);
 1482 | 			}
 1483 | 			return mbd;
 1484 | 		}
 1485 | 	}
 1486 | 
 1487 | 	private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
 1488 | 		if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
 1489 | 				ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
 1490 | 				ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
 1491 | 			ResolvableType targetType = mbd.targetType;
 1492 | 			ResolvableType previousTargetType = previous.targetType;
 1493 | 			if (targetType == null || targetType.equals(previousTargetType)) {
 1494 | 				mbd.targetType = previousTargetType;
 1495 | 				mbd.isFactoryBean = previous.isFactoryBean;
 1496 | 				mbd.resolvedTargetType = previous.resolvedTargetType;
 1497 | 				mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
 1498 | 				mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
 1499 | 			}
 1500 | 			if (previous.hasMethodOverrides()) {
 1501 | 				mbd.setMethodOverrides(new MethodOverrides(previous.getMethodOverrides()));
 1502 | 			}
 1503 | 		}
 1504 | 	}
 1505 | 
 1506 | 	/**
 1507 | 	 * Cache the given merged bean definition.
 1508 | 	 * <p>Subclasses can override this to derive additional cached state
 1509 | 	 * from the final post-processed bean definition.
 1510 | 	 * @param mbd the merged bean definition to cache
 1511 | 	 * @param beanName the name of the bean
 1512 | 	 * @since 6.2.6
 1513 | 	 */
 1514 | 	protected void cacheMergedBeanDefinition(RootBeanDefinition mbd, String beanName) {
 1515 | 		this.mergedBeanDefinitions.put(beanName, mbd);
 1516 | 	}
 1517 | 
 1518 | 	/**
 1519 | 	 * Check the given merged bean definition,
 1520 | 	 * potentially throwing validation exceptions.
 1521 | 	 * @param mbd the merged bean definition to check
 1522 | 	 * @param beanName the name of the bean
 1523 | 	 * @param args the arguments for bean creation, if any
 1524 | 	 */
 1525 | 	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object @Nullable [] args) {
 1526 | 		if (mbd.isAbstract()) {
 1527 | 			throw new BeanIsAbstractException(beanName);
 1528 | 		}
 1529 | 	}
 1530 | 
 1531 | 	/**
 1532 | 	 * Remove the merged bean definition for the specified bean,
 1533 | 	 * recreating it on next access.
 1534 | 	 * @param beanName the bean name to clear the merged definition for
 1535 | 	 */
 1536 | 	protected void clearMergedBeanDefinition(String beanName) {
 1537 | 		RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
 1538 | 		if (bd != null) {
 1539 | 			bd.stale = true;
 1540 | 		}
 1541 | 	}
 1542 | 
 1543 | 	/**
 1544 | 	 * Clear the merged bean definition cache, removing entries for beans
 1545 | 	 * which are not considered eligible for full metadata caching yet.
 1546 | 	 * <p>Typically triggered after changes to the original bean definitions,
 1547 | 	 * for example, after applying a {@code BeanFactoryPostProcessor}. Note that metadata
 1548 | 	 * for beans which have already been created at this point will be kept around.
 1549 | 	 * @since 4.2
 1550 | 	 */
 1551 | 	public void clearMetadataCache() {
 1552 | 		this.mergedBeanDefinitions.forEach((beanName, bd) -> {
 1553 | 			if (!isBeanEligibleForMetadataCaching(beanName)) {
 1554 | 				bd.stale = true;
 1555 | 			}
 1556 | 		});
 1557 | 	}
 1558 | 
 1559 | 	/**
 1560 | 	 * Resolve the bean class for the specified bean definition,
 1561 | 	 * resolving a bean class name into a Class reference (if necessary)
 1562 | 	 * and storing the resolved Class in the bean definition for further use.
 1563 | 	 * @param mbd the merged bean definition to determine the class for
 1564 | 	 * @param beanName the name of the bean (for error handling purposes)
 1565 | 	 * @param typesToMatch the types to match in case of internal type matching purposes
 1566 | 	 * (also signals that the returned {@code Class} will never be exposed to application code)
 1567 | 	 * @return the resolved bean class (or {@code null} if none)
 1568 | 	 * @throws CannotLoadBeanClassException if we failed to load the class
 1569 | 	 */
 1570 | 	protected @Nullable Class<?> resolveBeanClass(RootBeanDefinition mbd, String beanName, Class<?>... typesToMatch)
 1571 | 			throws CannotLoadBeanClassException {
 1572 | 
 1573 | 		try {
 1574 | 			if (mbd.hasBeanClass()) {
 1575 | 				return mbd.getBeanClass();
 1576 | 			}
 1577 | 			Class<?> beanClass = doResolveBeanClass(mbd, typesToMatch);
 1578 | 			if (mbd.hasBeanClass()) {
 1579 | 				mbd.prepareMethodOverrides();
 1580 | 			}
 1581 | 			return beanClass;
 1582 | 		}
 1583 | 		catch (ClassNotFoundException ex) {
 1584 | 			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
 1585 | 		}
 1586 | 		catch (LinkageError err) {
 1587 | 			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
 1588 | 		}
 1589 | 		catch (BeanDefinitionValidationException ex) {
 1590 | 			throw new BeanDefinitionStoreException(mbd.getResourceDescription(),
 1591 | 					beanName, "Validation of method overrides failed", ex);
 1592 | 		}
 1593 | 	}
 1594 | 
 1595 | 	private @Nullable Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
 1596 | 			throws ClassNotFoundException {
 1597 | 
 1598 | 		ClassLoader beanClassLoader = getBeanClassLoader();
 1599 | 		ClassLoader dynamicLoader = beanClassLoader;
 1600 | 		boolean freshResolve = false;
 1601 | 
 1602 | 		if (!ObjectUtils.isEmpty(typesToMatch)) {
 1603 | 			// When just doing type checks (i.e. not creating an actual instance yet),
 1604 | 			// use the specified temporary class loader (for example, in a weaving scenario).
 1605 | 			ClassLoader tempClassLoader = getTempClassLoader();
 1606 | 			if (tempClassLoader != null) {
 1607 | 				dynamicLoader = tempClassLoader;
 1608 | 				freshResolve = true;
 1609 | 				if (tempClassLoader instanceof DecoratingClassLoader dcl) {
 1610 | 					for (Class<?> typeToMatch : typesToMatch) {
 1611 | 						dcl.excludeClass(typeToMatch.getName());
 1612 | 					}
 1613 | 				}
 1614 | 			}
 1615 | 		}
 1616 | 
 1617 | 		String className = mbd.getBeanClassName();
 1618 | 		if (className != null) {
 1619 | 			Object evaluated = evaluateBeanDefinitionString(className, mbd);
 1620 | 			if (!className.equals(evaluated)) {
 1621 | 				// A dynamically resolved expression, supported as of 4.2...
 1622 | 				if (evaluated instanceof Class<?> clazz) {
 1623 | 					return clazz;
 1624 | 				}
 1625 | 				else if (evaluated instanceof String name) {
 1626 | 					className = name;
 1627 | 					freshResolve = true;
 1628 | 				}
 1629 | 				else {
 1630 | 					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
 1631 | 				}
 1632 | 			}
 1633 | 			if (freshResolve) {
 1634 | 				// When resolving against a temporary class loader, exit early in order
 1635 | 				// to avoid storing the resolved Class in the bean definition.
 1636 | 				if (dynamicLoader != null) {
 1637 | 					try {
 1638 | 						return dynamicLoader.loadClass(className);
 1639 | 					}
 1640 | 					catch (ClassNotFoundException ex) {
 1641 | 						if (logger.isTraceEnabled()) {
 1642 | 							logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
 1643 | 						}
 1644 | 					}
 1645 | 				}
 1646 | 				return ClassUtils.forName(className, dynamicLoader);
 1647 | 			}
 1648 | 		}
 1649 | 
 1650 | 		// Resolve regularly, caching the result in the BeanDefinition...
 1651 | 		return mbd.resolveBeanClass(beanClassLoader);
 1652 | 	}
 1653 | 
 1654 | 	/**
 1655 | 	 * Evaluate the given String as contained in a bean definition,
 1656 | 	 * potentially resolving it as an expression.
 1657 | 	 * @param value the value to check
 1658 | 	 * @param beanDefinition the bean definition that the value comes from
 1659 | 	 * @return the resolved value
 1660 | 	 * @see #setBeanExpressionResolver
 1661 | 	 */
 1662 | 	protected @Nullable Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
 1663 | 		if (this.beanExpressionResolver == null) {
 1664 | 			return value;
 1665 | 		}
 1666 | 
 1667 | 		Scope scope = null;
 1668 | 		if (beanDefinition != null) {
 1669 | 			String scopeName = beanDefinition.getScope();
 1670 | 			if (scopeName != null) {
 1671 | 				scope = getRegisteredScope(scopeName);
 1672 | 			}
 1673 | 		}
 1674 | 		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
 1675 | 	}
 1676 | 
 1677 | 
 1678 | 	/**
 1679 | 	 * Predict the eventual bean type (of the processed bean instance) for the
 1680 | 	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
 1681 | 	 * Does not need to handle FactoryBeans specifically, since it is only
 1682 | 	 * supposed to operate on the raw bean type.
 1683 | 	 * <p>This implementation is simplistic in that it is not able to
 1684 | 	 * handle factory methods and InstantiationAwareBeanPostProcessors.
 1685 | 	 * It only predicts the bean type correctly for a standard bean.
 1686 | 	 * To be overridden in subclasses, applying more sophisticated type detection.
 1687 | 	 * @param beanName the name of the bean
 1688 | 	 * @param mbd the merged bean definition to determine the type for
 1689 | 	 * @param typesToMatch the types to match in case of internal type matching purposes
 1690 | 	 * (also signals that the returned {@code Class} will never be exposed to application code)
 1691 | 	 * @return the type of the bean, or {@code null} if not predictable
 1692 | 	 */
 1693 | 	protected @Nullable Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
 1694 | 		Class<?> targetType = mbd.getTargetType();
 1695 | 		if (targetType != null) {
 1696 | 			return targetType;
 1697 | 		}
 1698 | 		if (mbd.getFactoryMethodName() != null) {
 1699 | 			return null;
 1700 | 		}
 1701 | 		return resolveBeanClass(mbd, beanName, typesToMatch);
 1702 | 	}
 1703 | 
 1704 | 	/**
 1705 | 	 * Check whether the given bean is defined as a {@link FactoryBean}.
 1706 | 	 * @param beanName the name of the bean
 1707 | 	 * @param mbd the corresponding bean definition
 1708 | 	 */
 1709 | 	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
 1710 | 		Boolean result = mbd.isFactoryBean;
 1711 | 		if (result == null) {
 1712 | 			Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
 1713 | 			result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
 1714 | 			mbd.isFactoryBean = result;
 1715 | 		}
 1716 | 		return result;
 1717 | 	}
 1718 | 
 1719 | 	/**
 1720 | 	 * Determine the bean type for the given FactoryBean definition, as far as possible.
 1721 | 	 * Only called if there is no singleton instance registered for the target bean
 1722 | 	 * already. The implementation is allowed to instantiate the target factory bean if
 1723 | 	 * {@code allowInit} is {@code true} and the type cannot be determined another way;
 1724 | 	 * otherwise it is restricted to introspecting signatures and related metadata.
 1725 | 	 * <p>If no {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} is set on the bean definition
 1726 | 	 * and {@code allowInit} is {@code true}, the default implementation will create
 1727 | 	 * the FactoryBean via {@code getBean} to call its {@code getObjectType} method.
 1728 | 	 * Subclasses are encouraged to optimize this, typically by inspecting the generic
 1729 | 	 * signature of the factory bean class or the factory method that creates it.
 1730 | 	 * If subclasses do instantiate the FactoryBean, they should consider trying the
 1731 | 	 * {@code getObjectType} method without fully populating the bean. If this fails,
 1732 | 	 * a full FactoryBean creation as performed by this implementation should be used
 1733 | 	 * as fallback.
 1734 | 	 * @param beanName the name of the bean
 1735 | 	 * @param mbd the merged bean definition for the bean
 1736 | 	 * @param allowInit if initialization of the FactoryBean is permitted if the type
 1737 | 	 * cannot be determined another way
 1738 | 	 * @return the type for the bean if determinable, otherwise {@code ResolvableType.NONE}
 1739 | 	 * @since 5.2
 1740 | 	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
 1741 | 	 * @see #getBean(String)
 1742 | 	 */
 1743 | 	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
 1744 | 		try {
 1745 | 			ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
 1746 | 			if (result != ResolvableType.NONE) {
 1747 | 				return result;
 1748 | 			}
 1749 | 		}
 1750 | 		catch (IllegalArgumentException ex) {
 1751 | 			throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
 1752 | 					String.valueOf(ex.getMessage()));
 1753 | 		}
 1754 | 
 1755 | 		if (allowInit && mbd.isSingleton()) {
 1756 | 			try {
 1757 | 				FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
 1758 | 				Class<?> objectType = getTypeForFactoryBean(factoryBean);
 1759 | 				return (objectType != null ? ResolvableType.forClass(objectType) : ResolvableType.NONE);
 1760 | 			}
 1761 | 			catch (BeanCreationException ex) {
 1762 | 				if (ex.contains(BeanCurrentlyInCreationException.class)) {
 1763 | 					logger.trace(LogMessage.format("Bean currently in creation on FactoryBean type check: %s", ex));
 1764 | 				}
 1765 | 				else if (mbd.isLazyInit()) {
 1766 | 					logger.trace(LogMessage.format("Bean creation exception on lazy FactoryBean type check: %s", ex));
 1767 | 				}
 1768 | 				else {
 1769 | 					logger.debug(LogMessage.format("Bean creation exception on eager FactoryBean type check: %s", ex));
 1770 | 				}
 1771 | 				onSuppressedException(ex);
 1772 | 			}
 1773 | 		}
 1774 | 
 1775 | 		// FactoryBean type not resolvable
 1776 | 		return ResolvableType.NONE;
 1777 | 	}
 1778 | 
 1779 | 	/**
 1780 | 	 * Mark the specified bean as already created (or about to be created).
 1781 | 	 * <p>This allows the bean factory to optimize its caching for repeated
 1782 | 	 * creation of the specified bean.
 1783 | 	 * @param beanName the name of the bean
 1784 | 	 */
 1785 | 	protected void markBeanAsCreated(String beanName) {
 1786 | 		if (!this.alreadyCreated.contains(beanName)) {
 1787 | 			synchronized (this.mergedBeanDefinitions) {
 1788 | 				if (!isBeanEligibleForMetadataCaching(beanName)) {
 1789 | 					// Let the bean definition get re-merged now that we're actually creating
 1790 | 					// the bean... just in case some of its metadata changed in the meantime.
 1791 | 					clearMergedBeanDefinition(beanName);
 1792 | 				}
 1793 | 				this.alreadyCreated.add(beanName);
 1794 | 			}
 1795 | 		}
 1796 | 	}
 1797 | 
 1798 | 	/**
 1799 | 	 * Perform appropriate cleanup of cached metadata after bean creation failed.
 1800 | 	 * @param beanName the name of the bean
 1801 | 	 */
 1802 | 	protected void cleanupAfterBeanCreationFailure(String beanName) {
 1803 | 		synchronized (this.mergedBeanDefinitions) {
 1804 | 			this.alreadyCreated.remove(beanName);
 1805 | 		}
 1806 | 	}
 1807 | 
 1808 | 	/**
 1809 | 	 * Determine whether the specified bean is eligible for having
 1810 | 	 * its bean definition metadata cached.
 1811 | 	 * @param beanName the name of the bean
 1812 | 	 * @return {@code true} if the bean's metadata may be cached
 1813 | 	 * at this point already
 1814 | 	 */
 1815 | 	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
 1816 | 		return this.alreadyCreated.contains(beanName);
 1817 | 	}
 1818 | 
 1819 | 	/**
 1820 | 	 * Remove the singleton instance (if any) for the given bean name,
 1821 | 	 * but only if it hasn't been used for other purposes than type checking.
 1822 | 	 * @param beanName the name of the bean
 1823 | 	 * @return {@code true} if actually removed, {@code false} otherwise
 1824 | 	 */
 1825 | 	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
 1826 | 		if (!this.alreadyCreated.contains(beanName)) {
 1827 | 			removeSingleton(beanName);
 1828 | 			return true;
 1829 | 		}
 1830 | 		else {
 1831 | 			return false;
 1832 | 		}
 1833 | 	}
 1834 | 
 1835 | 	/**
 1836 | 	 * Check whether this factory's bean creation phase already started,
 1837 | 	 * i.e. whether any bean has been marked as created in the meantime.
 1838 | 	 * @since 4.2.2
 1839 | 	 * @see #markBeanAsCreated
 1840 | 	 */
 1841 | 	protected boolean hasBeanCreationStarted() {
 1842 | 		return !this.alreadyCreated.isEmpty();
 1843 | 	}
 1844 | 
 1845 | 	/**
 1846 | 	 * Get the object for the given bean instance, either the bean
 1847 | 	 * instance itself or its created object in case of a FactoryBean.
 1848 | 	 * @param beanInstance the shared bean instance
 1849 | 	 * @param name the name that may include factory dereference prefix
 1850 | 	 * @param beanName the canonical bean name
 1851 | 	 * @param mbd the merged bean definition
 1852 | 	 * @return the object to expose for the bean
 1853 | 	 */
 1854 | 	protected Object getObjectForBeanInstance(Object beanInstance, @Nullable Class<?> requiredType,
 1855 | 			String name, String beanName, @Nullable RootBeanDefinition mbd) {
 1856 | 
 1857 | 		// Don't let calling code try to dereference the factory if the bean isn't a factory.
 1858 | 		if (BeanFactoryUtils.isFactoryDereference(name)) {
 1859 | 			if (beanInstance instanceof NullBean) {
 1860 | 				return beanInstance;
 1861 | 			}
 1862 | 			if (!(beanInstance instanceof FactoryBean)) {
 1863 | 				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
 1864 | 			}
 1865 | 			if (mbd != null) {
 1866 | 				mbd.isFactoryBean = true;
 1867 | 			}
 1868 | 			return beanInstance;
 1869 | 		}
 1870 | 
 1871 | 		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
 1872 | 		// If it's a FactoryBean, we use it to create a bean instance, unless the
 1873 | 		// caller actually wants a reference to the factory.
 1874 | 		if (!(beanInstance instanceof FactoryBean<?> factoryBean)) {
 1875 | 			return beanInstance;
 1876 | 		}
 1877 | 
 1878 | 		Object object = null;
 1879 | 		if (mbd != null) {
 1880 | 			mbd.isFactoryBean = true;
 1881 | 		}
 1882 | 		else {
 1883 | 			object = getCachedObjectForFactoryBean(beanName);
 1884 | 		}
 1885 | 		if (object == null) {
 1886 | 			// Return bean instance from factory.
 1887 | 			// Caches object obtained from FactoryBean if it is a singleton.
 1888 | 			if (mbd == null && containsBeanDefinition(beanName)) {
 1889 | 				mbd = getMergedLocalBeanDefinition(beanName);
 1890 | 			}
 1891 | 			boolean synthetic = (mbd != null && mbd.isSynthetic());
 1892 | 			object = getObjectFromFactoryBean(factoryBean, requiredType, beanName, !synthetic);
 1893 | 		}
 1894 | 		return object;
 1895 | 	}
 1896 | 
 1897 | 	/**
 1898 | 	 * Determine whether the given bean name is already in use within this factory,
 1899 | 	 * i.e. whether there is a local bean or alias registered under this name or
 1900 | 	 * an inner bean created with this name.
 1901 | 	 * @param beanName the name to check
 1902 | 	 */
 1903 | 	public boolean isBeanNameInUse(String beanName) {
 1904 | 		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
 1905 | 	}
 1906 | 
 1907 | 	/**
 1908 | 	 * Determine whether the given bean requires destruction on shutdown.
 1909 | 	 * <p>The default implementation checks the DisposableBean interface as well as
 1910 | 	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
 1911 | 	 * @param bean the bean instance to check
 1912 | 	 * @param mbd the corresponding bean definition
 1913 | 	 * @see org.springframework.beans.factory.DisposableBean
 1914 | 	 * @see AbstractBeanDefinition#getDestroyMethodName()
 1915 | 	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
 1916 | 	 */
 1917 | 	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
 1918 | 		return (bean.getClass() != NullBean.class && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) ||
 1919 | 				(hasDestructionAwareBeanPostProcessors() && DisposableBeanAdapter.hasApplicableProcessors(
 1920 | 						bean, getBeanPostProcessorCache().destructionAware))));
 1921 | 	}
 1922 | 
 1923 | 	/**
 1924 | 	 * Add the given bean to the list of disposable beans in this factory,
 1925 | 	 * registering its DisposableBean interface and/or the given destroy method
 1926 | 	 * to be called on factory shutdown (if applicable). Only applies to singletons.
 1927 | 	 * @param beanName the name of the bean
 1928 | 	 * @param bean the bean instance
 1929 | 	 * @param mbd the bean definition for the bean
 1930 | 	 * @see RootBeanDefinition#isSingleton
 1931 | 	 * @see RootBeanDefinition#getDependsOn
 1932 | 	 * @see #registerDisposableBean
 1933 | 	 * @see #registerDependentBean
 1934 | 	 */
 1935 | 	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
 1936 | 		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
 1937 | 			if (mbd.isSingleton()) {
 1938 | 				// Register a DisposableBean implementation that performs all destruction
 1939 | 				// work for the given bean: DestructionAwareBeanPostProcessors,
 1940 | 				// DisposableBean interface, custom destroy method.
 1941 | 				registerDisposableBean(beanName, new DisposableBeanAdapter(
 1942 | 						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware));
 1943 | 			}
 1944 | 			else {
 1945 | 				// A bean with a custom scope...
 1946 | 				Scope scope = this.scopes.get(mbd.getScope());
 1947 | 				if (scope == null) {
 1948 | 					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
 1949 | 				}
 1950 | 				scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(
 1951 | 						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware));
 1952 | 			}
 1953 | 		}
 1954 | 	}
 1955 | 
 1956 | 
 1957 | 	//---------------------------------------------------------------------
 1958 | 	// Abstract methods to be implemented by subclasses
 1959 | 	//---------------------------------------------------------------------
 1960 | 
 1961 | 	/**
 1962 | 	 * Check if this bean factory contains a bean definition with the given name.
 1963 | 	 * Does not consider any hierarchy this factory may participate in.
 1964 | 	 * Invoked by {@code containsBean} when no cached singleton instance is found.
 1965 | 	 * <p>Depending on the nature of the concrete bean factory implementation,
 1966 | 	 * this operation might be expensive (for example, because of directory lookups
 1967 | 	 * in external registries). However, for listable bean factories, this usually
 1968 | 	 * just amounts to a local hash lookup: The operation is therefore part of the
 1969 | 	 * public interface there. The same implementation can serve for both this
 1970 | 	 * template method and the public interface method in that case.
 1971 | 	 * @param beanName the name of the bean to look for
 1972 | 	 * @return if this bean factory contains a bean definition with the given name
 1973 | 	 * @see #containsBean
 1974 | 	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
 1975 | 	 */
 1976 | 	protected abstract boolean containsBeanDefinition(String beanName);
 1977 | 
 1978 | 	/**
 1979 | 	 * Return the bean definition for the given bean name.
 1980 | 	 * Subclasses should normally implement caching, as this method is invoked
 1981 | 	 * by this class every time bean definition metadata is needed.
 1982 | 	 * <p>Depending on the nature of the concrete bean factory implementation,
 1983 | 	 * this operation might be expensive (for example, because of directory lookups
 1984 | 	 * in external registries). However, for listable bean factories, this usually
 1985 | 	 * just amounts to a local hash lookup: The operation is therefore part of the
 1986 | 	 * public interface there. The same implementation can serve for both this
 1987 | 	 * template method and the public interface method in that case.
 1988 | 	 * @param beanName the name of the bean to find a definition for
 1989 | 	 * @return the BeanDefinition for this prototype name (never {@code null})
 1990 | 	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
 1991 | 	 * if the bean definition cannot be resolved
 1992 | 	 * @throws BeansException in case of errors
 1993 | 	 * @see RootBeanDefinition
 1994 | 	 * @see ChildBeanDefinition
 1995 | 	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
 1996 | 	 */
 1997 | 	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;
 1998 | 
 1999 | 	/**
 2000 | 	 * Create a bean instance for the given merged bean definition (and arguments).
 2001 | 	 * The bean definition will already have been merged with the parent definition
 2002 | 	 * in case of a child definition.
 2003 | 	 * <p>All bean retrieval methods delegate to this method for actual bean creation.
 2004 | 	 * @param beanName the name of the bean
 2005 | 	 * @param mbd the merged bean definition for the bean
 2006 | 	 * @param args explicit arguments to use for constructor or factory method invocation
 2007 | 	 * @return a new instance of the bean
 2008 | 	 * @throws BeanCreationException if the bean could not be created
 2009 | 	 */
 2010 | 	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args)
 2011 | 			throws BeanCreationException;
 2012 | 
 2013 | 
 2014 | 	/**
 2015 | 	 * CopyOnWriteArrayList which resets the beanPostProcessorCache field on modification.
 2016 | 	 *
 2017 | 	 * @since 5.3
 2018 | 	 */
 2019 | 	@SuppressWarnings("serial")
 2020 | 	private class BeanPostProcessorCacheAwareList extends CopyOnWriteArrayList<BeanPostProcessor> {
 2021 | 
 2022 | 		@Override
 2023 | 		public BeanPostProcessor set(int index, BeanPostProcessor element) {
 2024 | 			BeanPostProcessor result = super.set(index, element);
 2025 | 			resetBeanPostProcessorCache();
 2026 | 			return result;
 2027 | 		}
 2028 | 
 2029 | 		@Override
 2030 | 		public boolean add(BeanPostProcessor o) {
 2031 | 			boolean success = super.add(o);
 2032 | 			resetBeanPostProcessorCache();
 2033 | 			return success;
 2034 | 		}
 2035 | 
 2036 | 		@Override
 2037 | 		public void add(int index, BeanPostProcessor element) {
 2038 | 			super.add(index, element);
 2039 | 			resetBeanPostProcessorCache();
 2040 | 		}
 2041 | 
 2042 | 		@Override
 2043 | 		public BeanPostProcessor remove(int index) {
 2044 | 			BeanPostProcessor result = super.remove(index);
 2045 | 			resetBeanPostProcessorCache();
 2046 | 			return result;
 2047 | 		}
 2048 | 
 2049 | 		@Override
 2050 | 		public boolean remove(Object o) {
 2051 | 			boolean success = super.remove(o);
 2052 | 			if (success) {
 2053 | 				resetBeanPostProcessorCache();
 2054 | 			}
 2055 | 			return success;
 2056 | 		}
 2057 | 
 2058 | 		@Override
 2059 | 		public boolean removeAll(Collection<?> c) {
 2060 | 			boolean success = super.removeAll(c);
 2061 | 			if (success) {
 2062 | 				resetBeanPostProcessorCache();
 2063 | 			}
 2064 | 			return success;
 2065 | 		}
 2066 | 
 2067 | 		@Override
 2068 | 		public boolean retainAll(Collection<?> c) {
 2069 | 			boolean success = super.retainAll(c);
 2070 | 			if (success) {
 2071 | 				resetBeanPostProcessorCache();
 2072 | 			}
 2073 | 			return success;
 2074 | 		}
 2075 | 
 2076 | 		@Override
 2077 | 		public boolean addAll(Collection<? extends BeanPostProcessor> c) {
 2078 | 			boolean success = super.addAll(c);
 2079 | 			if (success) {
 2080 | 				resetBeanPostProcessorCache();
 2081 | 			}
 2082 | 			return success;
 2083 | 		}
 2084 | 
 2085 | 		@Override
 2086 | 		public boolean addAll(int index, Collection<? extends BeanPostProcessor> c) {
 2087 | 			boolean success = super.addAll(index, c);
 2088 | 			if (success) {
 2089 | 				resetBeanPostProcessorCache();
 2090 | 			}
 2091 | 			return success;
 2092 | 		}
 2093 | 
 2094 | 		@Override
 2095 | 		public boolean removeIf(Predicate<? super BeanPostProcessor> filter) {
 2096 | 			boolean success = super.removeIf(filter);
 2097 | 			if (success) {
 2098 | 				resetBeanPostProcessorCache();
 2099 | 			}
 2100 | 			return success;
 2101 | 		}
 2102 | 
 2103 | 		@Override
 2104 | 		public void replaceAll(UnaryOperator<BeanPostProcessor> operator) {
 2105 | 			super.replaceAll(operator);
 2106 | 			resetBeanPostProcessorCache();
 2107 | 		}
 2108 | 	}
 2109 | 
 2110 | 
 2111 | 	/**
 2112 | 	 * Internal cache of pre-filtered post-processors.
 2113 | 	 *
 2114 | 	 * @since 5.3
 2115 | 	 */
 2116 | 	static class BeanPostProcessorCache {
 2117 | 
 2118 | 		final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();
 2119 | 
 2120 | 		final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();
 2121 | 
 2122 | 		final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();
 2123 | 
 2124 | 		final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
 2125 | 	}
 2126 | 
 2127 | 
 2128 | 	/**
 2129 | 	 * {@link PropertyEditorRegistrar} that delegates to the bean factory's
 2130 | 	 * default registrars, adding exception handling for circular reference
 2131 | 	 * scenarios where an editor tries to refer back to the currently created bean.
 2132 | 	 *
 2133 | 	 * @since 6.2.3
 2134 | 	 */
 2135 | 	class BeanFactoryDefaultEditorRegistrar implements PropertyEditorRegistrar {
 2136 | 
 2137 | 		@Override
 2138 | 		public void registerCustomEditors(PropertyEditorRegistry registry) {
 2139 | 			applyEditorRegistrars(registry, defaultEditorRegistrars);
 2140 | 		}
 2141 | 	}
 2142 | 
 2143 | }
```
