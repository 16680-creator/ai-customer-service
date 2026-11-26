<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/properties/ConfigurationPropertiesRebinder.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesRebinder.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesRebinder.java#L起-L止) -->

# ConfigurationPropertiesRebinder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/properties/ConfigurationPropertiesRebinder.java` |
| 行数 | 636 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:37.527Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.context.properties;
   18 | 
   19 | import java.beans.PropertyDescriptor;
   20 | import java.lang.reflect.Constructor;
   21 | import java.lang.reflect.Modifier;
   22 | import java.util.Collection;
   23 | import java.util.Collections;
   24 | import java.util.HashSet;
   25 | import java.util.IdentityHashMap;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.Set;
   29 | import java.util.concurrent.ConcurrentHashMap;
   30 | import java.util.concurrent.ConcurrentMap;
   31 | import java.util.concurrent.locks.Lock;
   32 | import java.util.concurrent.locks.ReentrantLock;
   33 | 
   34 | import org.apache.commons.logging.Log;
   35 | import org.apache.commons.logging.LogFactory;
   36 | 
   37 | import org.springframework.aop.framework.Advised;
   38 | import org.springframework.aop.framework.AopProxyUtils;
   39 | import org.springframework.aop.scope.ScopedProxyUtils;
   40 | import org.springframework.aop.support.AopUtils;
   41 | import org.springframework.aop.target.SingletonTargetSource;
   42 | import org.springframework.beans.BeanUtils;
   43 | import org.springframework.beans.BeanWrapper;
   44 | import org.springframework.beans.BeanWrapperImpl;
   45 | import org.springframework.beans.BeansException;
   46 | import org.springframework.beans.factory.BeanFactoryUtils;
   47 | import org.springframework.boot.context.properties.ConfigurationProperties;
   48 | import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
   49 | import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration.RefreshProperties;
   50 | import org.springframework.cloud.context.config.annotation.RefreshScope;
   51 | import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
   52 | import org.springframework.cloud.util.ProxyUtils;
   53 | import org.springframework.context.ApplicationContext;
   54 | import org.springframework.context.ApplicationContextAware;
   55 | import org.springframework.context.ApplicationListener;
   56 | import org.springframework.core.env.Environment;
   57 | import org.springframework.jmx.export.annotation.ManagedAttribute;
   58 | import org.springframework.jmx.export.annotation.ManagedOperation;
   59 | import org.springframework.jmx.export.annotation.ManagedResource;
   60 | import org.springframework.stereotype.Component;
   61 | import org.springframework.util.ObjectUtils;
   62 | import org.springframework.util.StringUtils;
   63 | 
   64 | /**
   65 |  * Listens for {@link EnvironmentChangeEvent} and rebinds beans that were bound to the
   66 |  * {@link Environment} using {@link ConfigurationProperties
   67 |  * <code>@ConfigurationProperties</code>}. When these beans are re-bound and
   68 |  * re-initialized, the changes are available immediately to any component that is using
   69 |  * the <code>@ConfigurationProperties</code> bean.
   70 |  *
   71 |  * <p>
   72 |  * Rebinding a given bean is serialized against other concurrent rebinds of that same
   73 |  * bean, so two overlapping refreshes (for example a manual {@link #rebind(String)} call
   74 |  * racing with an {@link EnvironmentChangeEvent}-triggered {@link #rebind()}) cannot
   75 |  * interleave their destroy/reset/re-initialize steps.
   76 |  *
   77 |  * <p>
   78 |  * To revert properties that were removed from the {@link Environment} to their defaults
   79 |  * without exposing those defaults to other threads, a new instance of the bean's class is
   80 |  * bound first and its state is then copied onto the live bean. A concurrent reader
   81 |  * therefore never sees a bound property reset to its class-level default (for example a
   82 |  * {@code null} nested object or a {@code 0}), and getter-only collections and maps are
   83 |  * updated in place rather than being cleared and refilled. The bean is still mutated in
   84 |  * place, field by field, so a reader can observe a mix of old and new values while a
   85 |  * rebind is in progress. Beans that need a consistent view across a refresh should use
   86 |  * {@link RefreshScope} instead, which serializes reads against refreshes via a per-bean
   87 |  * {@link java.util.concurrent.locks.ReadWriteLock}.
   88 |  *
   89 |  * <p>
   90 |  * Known limitation: a collection property that has a value in the {@link Environment} can
   91 |  * be briefly empty during a rebind. The bean is initialized again after the new state has
   92 |  * been copied onto it, and Spring Boot's binder updates an existing, modifiable
   93 |  * collection by clearing it and then adding the bound elements. That happens inside
   94 |  * Spring Boot's binding and cannot be avoided here. Maps are not affected.
   95 |  *
   96 |  * @author Dave Syer
   97 |  * @author Yanming Zhou
   98 |  * @see RefreshScope for a deeper and optionally more focused refresh of bean components.
   99 |  *
  100 |  */
  101 | @Component
  102 | @ManagedResource
  103 | public class ConfigurationPropertiesRebinder
  104 | 		implements ApplicationContextAware, ApplicationListener<EnvironmentChangeEvent> {
  105 | 
  106 | 	private static final Log logger = LogFactory.getLog(ConfigurationPropertiesRebinder.class);
  107 | 
  108 | 	private ConfigurationPropertiesBeans beans;
  109 | 
  110 | 	private ApplicationContext applicationContext;
  111 | 
  112 | 	private Map<String, Exception> errors = new ConcurrentHashMap<>();
  113 | 
  114 | 	private final ConcurrentMap<String, Lock> rebindLocks = new ConcurrentHashMap<>();
  115 | 
  116 | 	private final Set<String> neverResetNestedTypes;
  117 | 
  118 | 	private final Set<String> loggedResetFailures = ConcurrentHashMap.newKeySet();
  119 | 
  120 | 	/**
  121 | 	 * Safety net against unbounded recursion in {@link #resetProperties}. Cyclic graphs
  122 | 	 * are already broken by the identity-based {@code visited} set, and graphs built from
  123 | 	 * unstable (newly-allocated-per-call) instances are broken by
  124 | 	 * {@link #isStableInstance(BeanWrapper, String, Object)}; this limit only guards
  125 | 	 * against a gap in either of those checks, or a legitimately very deep object graph.
  126 | 	 */
  127 | 	private static final int MAX_RESET_DEPTH = 25;
  128 | 
  129 | 	public ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
  130 | 		this(beans, Collections.emptySet());
  131 | 	}
  132 | 
  133 | 	public ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans, RefreshProperties refreshProperties) {
  134 | 		this(beans, refreshProperties.getNeverResetNestedTypes());
  135 | 	}
  136 | 
  137 | 	private ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans, Set<String> neverResetNestedTypes) {
  138 | 		this.beans = beans;
  139 | 		this.neverResetNestedTypes = neverResetNestedTypes;
  140 | 	}
  141 | 
  142 | 	@Override
  143 | 	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
  144 | 		this.applicationContext = applicationContext;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * A map of bean name to errors when instantiating the bean.
  149 | 	 * @return The errors accumulated since the latest destroy.
  150 | 	 */
  151 | 	public Map<String, Exception> getErrors() {
  152 | 		return this.errors;
  153 | 	}
  154 | 
  155 | 	@ManagedOperation
  156 | 	public void rebind() {
  157 | 		this.errors.clear();
  158 | 		for (String name : this.beans.getBeanNames()) {
  159 | 			rebind(name);
  160 | 		}
  161 | 	}
  162 | 
  163 | 	@ManagedOperation
  164 | 	public boolean rebind(String name) {
  165 | 		if (!this.beans.getBeanNames().contains(name)) {
  166 | 			return false;
  167 | 		}
  168 | 		ApplicationContext appContext = this.applicationContext;
  169 | 		while (appContext != null) {
  170 | 			if (appContext.containsLocalBean(name)) {
  171 | 				return rebind(name, appContext);
  172 | 			}
  173 | 			else {
  174 | 				appContext = appContext.getParent();
  175 | 			}
  176 | 		}
  177 | 		return false;
  178 | 	}
  179 | 
  180 | 	/**
  181 | 	 * WARNING: This method rebinds beans from any context in the hierarchy using the main
  182 | 	 * application context.
  183 | 	 * @param type bean type to rebind.
  184 | 	 * @return true, if successful.
  185 | 	 */
  186 | 	public boolean rebind(Class type) {
  187 | 		String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.applicationContext, type);
  188 | 		if (beanNamesForType.length > 0) {
  189 | 			String name = beanNamesForType[0];
  190 | 			if (ScopedProxyUtils.isScopedTarget(name)) {
  191 | 				name = ScopedProxyUtils.getOriginalBeanName(name);
  192 | 			}
  193 | 			return rebind(name, this.applicationContext);
  194 | 		}
  195 | 		return false;
  196 | 	}
  197 | 
  198 | 	private boolean rebind(String name, ApplicationContext appContext) {
  199 | 		// Serialize concurrent rebinds of the *same* bean (for example a manual
  200 | 		// rebind(name) racing with an EnvironmentChangeEvent-triggered rebind()) so
  201 | 		// their destroy/reset/re-initialize steps cannot interleave on the live bean.
  202 | 		// This does not protect concurrent readers of the bean; see the class Javadoc.
  203 | 		Lock lock = this.rebindLocks.computeIfAbsent(name, key -> new ReentrantLock());
  204 | 		lock.lock();
  205 | 		try {
  206 | 			try {
  207 | 				Object bean = appContext.getBean(name);
  208 | 				if (bean != null && !AopUtils.isAopProxy(bean)) {
  209 | 					// A BeanPostProcessor may have replaced the bean with a wrapper after
  210 | 					// the properties were bound (for example Boot's lazy connection fetch
  211 | 					// wraps the DataSource). Rebind what the properties were bound to,
  212 | 					// not
  213 | 					// the wrapper, which could not be bound to the bean's declared type.
  214 | 					Object bound = this.beans.getInstance(name);
  215 | 					if (bound != null) {
  216 | 						bean = bound;
  217 | 					}
  218 | 				}
  219 | 				if (bean != null) {
  220 | 					// The class behind every proxy layer, not just the outermost one
  221 | 					Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
  222 | 					// TODO: determine a more general approach to fix this.
  223 | 					// see
  224 | 					// https://github.com/spring-cloud/spring-cloud-commons/issues/571
  225 | 					if (getNeverRefreshable().contains(targetClass.getName()) || getNeverRefreshable().contains(name)) {
  226 | 						return false; // ignore
  227 | 					}
  228 | 					if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised) {
  229 | 						Object target = ProxyUtils.getTargetObject(bean);
  230 | 						if (target != bean && !targetClass.isInterface()
  231 | 								&& !Modifier.isAbstract(targetClass.getModifiers())) {
  232 | 							Object freshBean = appContext.getAutowireCapableBeanFactory().createBean(targetClass);
  233 | 							Object freshTarget = AopUtils.isAopProxy(freshBean) ? ProxyUtils.getTargetObject(freshBean)
  234 | 									: freshBean;
  235 | 							// Keep the advice of every proxy layer
  236 | 							innermostAdvised(advised).setTargetSource(new SingletonTargetSource(freshTarget));
  237 | 							appContext.getAutowireCapableBeanFactory().destroyBean(target);
  238 | 						}
  239 | 						else {
  240 | 							Object boundDefaults = bindFreshInstance(target, name, appContext);
  241 | 							appContext.getAutowireCapableBeanFactory().destroyBean(target);
  242 | 							resetBeanToDefaults(target, boundDefaults);
  243 | 							appContext.getAutowireCapableBeanFactory().autowireBean(target);
  244 | 							appContext.getAutowireCapableBeanFactory().initializeBean(target, name);
  245 | 						}
  246 | 					}
  247 | 					else {
  248 | 						Object boundDefaults = bindFreshInstance(bean, name, appContext);
  249 | 						appContext.getAutowireCapableBeanFactory().destroyBean(bean);
  250 | 						resetBeanToDefaults(bean, boundDefaults);
  251 | 						appContext.getAutowireCapableBeanFactory().autowireBean(bean);
  252 | 						appContext.getAutowireCapableBeanFactory().initializeBean(bean, name);
  253 | 					}
  254 | 					return true;
  255 | 				}
  256 | 			}
  257 | 			catch (RuntimeException e) {
  258 | 				this.errors.put(name, e);
  259 | 				throw e;
  260 | 			}
  261 | 			catch (Exception e) {
  262 | 				this.errors.put(name, e);
  263 | 				throw new IllegalStateException("Cannot rebind to " + name, e);
  264 | 			}
  265 | 			return false;
  266 | 		}
  267 | 		finally {
  268 | 			lock.unlock();
  269 | 		}
  270 | 	}
  271 | 
  272 | 	/**
  273 | 	 * Find the innermost proxy of a, possibly nested, chain of AOP proxies: the one whose
  274 | 	 * target is not itself a proxy.
  275 | 	 */
  276 | 	private Advised innermostAdvised(Advised advised) throws Exception {
  277 | 		Advised current = advised;
  278 | 		Object next = current.getTargetSource().getTarget();
  279 | 		while (AopUtils.isAopProxy(next) && next instanceof Advised nested) {
  280 | 			current = nested;
  281 | 			next = current.getTargetSource().getTarget();
  282 | 		}
  283 | 		return current;
  284 | 	}
  285 | 
  286 | 	/**
  287 | 	 * Create a new instance of the bean's class and bind it from the {@link Environment},
  288 | 	 * <em>without touching the live bean</em>. The result is what the live bean should
  289 | 	 * look like after rebinding: removed properties carry their class-level defaults and
  290 | 	 * everything else carries its freshly bound value. Applying it to the live bean (see
  291 | 	 * {@link #resetBeanToDefaults}) therefore never exposes the unbound defaults to
  292 | 	 * concurrent readers (gh-1727), and a binding failure here leaves the live bean as it
  293 | 	 * was.
  294 | 	 * @return the bound instance, or {@code null} if one cannot be produced, in which
  295 | 	 * case the live bean is simply re-bound in place
  296 | 	 */
  297 | 	private Object bindFreshInstance(Object bean, String name, ApplicationContext appContext) {
  298 | 		Class<?> targetClass = AopUtils.getTargetClass(bean);
  299 | 		if (!hasDefaultConstructor(targetClass)) {
  300 | 			// Beans whose no-arg constructor (if any) is not their only constructor
  301 | 			// (for example constructor-bound beans, beans with required dependencies, or
  302 | 			// beans with an extra no-arg constructor kept only for a framework's benefit)
  303 | 			// cannot be instantiated to obtain trustworthy defaults, so the reset is
  304 | 			// skipped. The bean is still re-bound from the Environment afterwards; only
  305 | 			// reverting removed properties to their defaults is skipped.
  306 | 			if (logger.isDebugEnabled()) {
  307 | 				logger.debug("No default constructor for " + targetClass.getName()
  308 | 						+ "; skipping property reset before rebinding");
  309 | 			}
  310 | 			return null;
  311 | 		}
  312 | 		ConfigurationPropertiesBindingPostProcessor binder;
  313 | 		try {
  314 | 			binder = appContext.getBean(ConfigurationPropertiesBindingPostProcessor.class);
  315 | 		}
  316 | 		catch (BeansException ex) {
  317 | 			if (logger.isDebugEnabled()) {
  318 | 				logger.debug("No ConfigurationPropertiesBindingPostProcessor available; skipping property reset of "
  319 | 						+ targetClass.getName());
  320 | 			}
  321 | 			return null;
  322 | 		}
  323 | 		Object freshInstance;
  324 | 		try {
  325 | 			freshInstance = BeanUtils.instantiateClass(targetClass);
  326 | 		}
  327 | 		catch (Exception ex) {
  328 | 			logger.warn("Cannot create default instance of " + targetClass.getName()
  329 | 					+ " for reset; skipping property reset", ex);
  330 | 			return null;
  331 | 		}
  332 | 		// Deliberately autowire the throw-away instance, even though it is only used as a
  333 | 		// source of values. The fresh instance is later copied onto the live bean
  334 | 		// property by property (see resetProperties), and anything that is injected
  335 | 		// through a writable property (for example an @Autowired setter) would be null
  336 | 		// on an un-autowired instance. Copying that null would wipe the live bean's
  337 | 		// collaborator and leave it null until the live bean is autowired again, which
  338 | 		// reopens the very window (gh-1727) this method exists to close. With the
  339 | 		// instance autowired, the copy writes the same collaborator that is already
  340 | 		// there, which resetProperties skips as "nothing to change". The cost is one
  341 | 		// extra round of dependency injection per rebind; only autowireBean is called,
  342 | 		// so injection happens but no initialization callbacks (afterPropertiesSet,
  343 | 		// @PostConstruct, init methods) run on the instance.
  344 | 		appContext.getAutowireCapableBeanFactory().autowireBean(freshInstance);
  345 | 		// Only the binding post-processor runs, so no other post-processor or lifecycle
  346 | 		// callback sees the throw-away instance. A binding failure propagates before the
  347 | 		// live bean has been touched.
  348 | 		binder.postProcessBeforeInitialization(freshInstance, name);
  349 | 		return freshInstance;
  350 | 	}
  351 | 
  352 | 	/**
  353 | 	 * Copy the state of a freshly bound instance onto the live bean, so that removed
  354 | 	 * properties do not retain stale values after rebinding.
  355 | 	 */
  356 | 	private void resetBeanToDefaults(Object bean, Object boundDefaults) {
  357 | 		if (boundDefaults != null) {
  358 | 			resetProperties(bean, boundDefaults, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
  359 | 		}
  360 | 	}
  361 | 
  362 | 	/**
  363 | 	 * Whether the given type's <em>only</em> declared constructor is a no-argument one,
  364 | 	 * which is what {@link BeanUtils#instantiateClass(Class)} needs to build a defaults
  365 | 	 * template that is actually representative of the bean's defaults.
  366 | 	 * <p>
  367 | 	 * A no-arg constructor that sits alongside another, parameterized constructor is
  368 | 	 * deliberately not enough here, regardless of its visibility: some beans declare one
  369 | 	 * purely for a framework's benefit (for example Jackson deserialization) next to
  370 | 	 * another constructor that takes collaborators and computes the bean's real defaults
  371 | 	 * (for example from the current host). Instantiating the type through the no-arg
  372 | 	 * constructor then produces a template with the wrong defaults - typically
  373 | 	 * {@code null} or zero-valued fields - for anything the other constructor would have
  374 | 	 * computed, and resetting the bean to that template before rebinding wipes out values
  375 | 	 * no property source ever carried (see gh-1733). Only a type whose sole constructor
  376 | 	 * takes no arguments can be assumed to have one intended, complete way of producing a
  377 | 	 * default instance.
  378 | 	 */
  379 | 	private boolean hasDefaultConstructor(Class<?> type) {
  380 | 		Constructor<?>[] constructors = type.getDeclaredConstructors();
  381 | 		return constructors.length == 1 && constructors[0].getParameterCount() == 0;
  382 | 	}
  383 | 
  384 | 	private void resetProperties(Object bean, Object defaults, Set<Object> visited, int depth) {
  385 | 		// Guard against cyclic object graphs and pathologically deep ones so that
  386 | 		// recursion
  387 | 		// always terminates; see MAX_RESET_DEPTH.
  388 | 		if (bean == null || depth > MAX_RESET_DEPTH || !visited.add(bean)) {
  389 | 			return;
  390 | 		}
  391 | 		BeanWrapper target = new BeanWrapperImpl(bean);
  392 | 		BeanWrapper defaultsWrapper = new BeanWrapperImpl(defaults);
  393 | 		for (PropertyDescriptor pd : target.getPropertyDescriptors()) {
  394 | 			String propertyName = pd.getName();
  395 | 			if ("class".equals(propertyName)) {
  396 | 				continue;
  397 | 			}
  398 | 			try {
  399 | 				// A property whose declared type the user excluded is left alone whatever
  400 | 				// its accessors look like: a read-only property is not descended into and
  401 | 				// a writable one is not overwritten with the default instance's value.
  402 | 				if (isNeverReset(pd.getPropertyType())) {
  403 | 					continue;
  404 | 				}
  405 | 				if (target.isWritableProperty(propertyName) && defaultsWrapper.isReadableProperty(propertyName)) {
  406 | 					if (target.isReadableProperty(propertyName)
  407 | 							&& isNeverReset(target.getPropertyValue(propertyName))) {
  408 | 						continue;
  409 | 					}
  410 | 					Object defaultValue = defaultsWrapper.getPropertyValue(propertyName);
  411 | 					Object currentValue = target.isReadableProperty(propertyName)
  412 | 							? target.getPropertyValue(propertyName) : null;
  413 | 					// Skip the setter call entirely when there's nothing to change: this
  414 | 					// avoids needlessly invoking setters that don't tolerate being called
  415 | 					// again with the same value, and setters that reject null are only
  416 | 					// ever
  417 | 					// invoked when the property actually needs to become null.
  418 | 					if (!ObjectUtils.nullSafeEquals(currentValue, defaultValue)) {
  419 | 						try {
  420 | 							target.setPropertyValue(propertyName, defaultValue);
  421 | 						}
  422 | 						catch (Exception ex) {
  423 | 							warnCannotReset(bean, propertyName, ex);
  424 | 						}
  425 | 					}
  426 | 				}
  427 | 				else if (target.isReadableProperty(propertyName) && defaultsWrapper.isReadableProperty(propertyName)) {
  428 | 					Object value = target.getPropertyValue(propertyName);
  429 | 					Object defaultValue = defaultsWrapper.getPropertyValue(propertyName);
  430 | 					if (value instanceof Collection collection) {
  431 | 						try {
  432 | 							mergeCollection(collection, defaultValue instanceof Collection c ? c : null);
  433 | 						}
  434 | 						catch (UnsupportedOperationException ex) {
  435 | 							warnCannotReset(bean, propertyName, ex);
  436 | 						}
  437 | 					}
  438 | 					else if (value instanceof Map map) {
  439 | 						try {
  440 | 							mergeMap(map, defaultValue instanceof Map m ? m : null);
  441 | 						}
  442 | 						catch (UnsupportedOperationException ex) {
  443 | 							warnCannotReset(bean, propertyName, ex);
  444 | 						}
  445 | 					}
  446 | 					else if (value != null && defaultValue != null && isResettableNestedType(value.getClass())
  447 | 							&& isStableInstance(target, propertyName, value)) {
  448 | 						resetProperties(value, defaultValue, visited, depth + 1);
  449 | 					}
  450 | 				}
  451 | 			}
  452 | 			catch (Exception ex) {
  453 | 				if (logger.isDebugEnabled()) {
  454 | 					logger.debug("Failed to reset property '" + propertyName + "' on "
  455 | 							+ AopUtils.getTargetClass(bean).getName(), ex);
  456 | 				}
  457 | 			}
  458 | 		}
  459 | 	}
  460 | 
  461 | 	/**
  462 | 	 * Make a getter-only collection hold the same elements as {@code source} without ever
  463 | 	 * passing through an empty state, which a concurrent reader would otherwise observe
  464 | 	 * (gh-1727). The new elements are added before the stale ones are removed, so a
  465 | 	 * reader sees the old elements, then old and new, then the new ones.
  466 | 	 */
  467 | 	@SuppressWarnings({ "unchecked", "rawtypes" })
  468 | 	private void mergeCollection(Collection target, Collection source) {
  469 | 		if (source == null || source.isEmpty()) {
  470 | 			target.clear();
  471 | 			return;
  472 | 		}
  473 | 		if (target == source || (target instanceof List || target instanceof Set) && target.equals(source)) {
  474 | 			return;
  475 | 		}
  476 | 		if (target instanceof List list) {
  477 | 			int stale = list.size();
  478 | 			list.addAll(source);
  479 | 			list.subList(0, stale).clear();
  480 | 		}
  481 | 		else if (target instanceof Set) {
  482 | 			target.addAll(source);
  483 | 			target.retainAll(source);
  484 | 		}
  485 | 		else {
  486 | 			// No way to tell old from new elements in other collection types (they may
  487 | 			// hold duplicates), so fall back to replacing the contents.
  488 | 			target.clear();
  489 | 			target.addAll(source);
  490 | 		}
  491 | 	}
  492 | 
  493 | 	/**
  494 | 	 * Make a getter-only map hold the same entries as {@code source} without ever passing
  495 | 	 * through an empty state: new and changed entries are put before removed keys are
  496 | 	 * dropped.
  497 | 	 */
  498 | 	@SuppressWarnings({ "unchecked", "rawtypes" })
  499 | 	private void mergeMap(Map target, Map source) {
  500 | 		if (source == null || source.isEmpty()) {
  501 | 			target.clear();
  502 | 			return;
  503 | 		}
  504 | 		if (target == source || target.equals(source)) {
  505 | 			return;
  506 | 		}
  507 | 		target.putAll(source);
  508 | 		target.keySet().retainAll(source.keySet());
  509 | 	}
  510 | 
  511 | 	/**
  512 | 	 * Whether re-reading the given read-only property returns the very same instance. A
  513 | 	 * getter that instead returns a new object on every call cannot be holding any state
  514 | 	 * that a reset would need to affect, so recursing into such a value would only ever
  515 | 	 * walk a throw-away object graph - one that, for record-style/fluent accessors, may
  516 | 	 * not even terminate (see gh-1750).
  517 | 	 */
  518 | 	private boolean isStableInstance(BeanWrapper target, String propertyName, Object value) {
  519 | 		try {
  520 | 			return target.getPropertyValue(propertyName) == value;
  521 | 		}
  522 | 		catch (Exception ex) {
  523 | 			return false;
  524 | 		}
  525 | 	}
  526 | 
  527 | 	/**
  528 | 	 * Log, at most once per bean type and property, that a property could not be reset to
  529 | 	 * its default value and may therefore retain a stale value after this and future
  530 | 	 * refreshes. The full exception is only logged at DEBUG, on every occurrence, to
  531 | 	 * avoid spamming the log at the default level while still making the failure
  532 | 	 * discoverable.
  533 | 	 */
  534 | 	private void warnCannotReset(Object bean, String propertyName, Exception ex) {
  535 | 		String targetClassName = AopUtils.getTargetClass(bean).getName();
  536 | 		if (this.loggedResetFailures.add(targetClassName + "#" + propertyName)) {
  537 | 			logger.warn("Cannot reset property '" + propertyName + "' on " + targetClassName
  538 | 					+ " to its default value; it may retain its previous value after this and future refreshes. "
  539 | 					+ "Enable DEBUG logging for " + ConfigurationPropertiesRebinder.class.getName()
  540 | 					+ " for the full exception.");
  541 | 		}
  542 | 		if (logger.isDebugEnabled()) {
  543 | 			logger.debug("Failed to reset property '" + propertyName + "' on " + targetClassName, ex);
  544 | 		}
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Determine whether a nested property value should be recursively reset. Only
  549 | 	 * user-defined types are descended into. Recursing into JDK or standard API types
  550 | 	 * (for example {@link javax.net.ssl.SSLContext}) is both unnecessary and unsafe:
  551 | 	 * their object graphs may be cyclic, which previously led to a
  552 | 	 * {@link StackOverflowError} (see gh-1698), and they may expose internal collections
  553 | 	 * or maps that must not be cleared. Additional types can be excluded via the
  554 | 	 * {@code spring.cloud.refresh.never-reset-nested-types} property.
  555 | 	 */
  556 | 	boolean isResettableNestedType(Class<?> type) {
  557 | 		if (type.isArray() || BeanUtils.isSimpleValueType(type) || isJdkClass(type) || isStandardApiClass(type)) {
  558 | 			return false;
  559 | 		}
  560 | 		return !isNeverReset(type);
  561 | 	}
  562 | 
  563 | 	/**
  564 | 	 * Whether the given value was excluded through the
  565 | 	 * {@code spring.cloud.refresh.never-reset-nested-types} property.
  566 | 	 */
  567 | 	private boolean isNeverReset(Object value) {
  568 | 		return value != null && isNeverReset(value.getClass());
  569 | 	}
  570 | 
  571 | 	/**
  572 | 	 * Whether the given type was excluded through the
  573 | 	 * {@code spring.cloud.refresh.never-reset-nested-types} property. The exclusion is
  574 | 	 * matched against a property's declared type as well as the runtime class of its
  575 | 	 * value, so that a type the user asked us to leave alone is neither descended into
  576 | 	 * nor overwritten with the default instance's value, whether or not the value happens
  577 | 	 * to be an instance of a subtype whose own name does not carry the excluded prefix.
  578 | 	 */
  579 | 	private boolean isNeverReset(Class<?> type) {
  580 | 		if (type == null) {
  581 | 			return false;
  582 | 		}
  583 | 		String typeName = type.getName();
  584 | 		for (String excluded : this.neverResetNestedTypes) {
  585 | 			if (typeName.startsWith(excluded)) {
  586 | 				return true;
  587 | 			}
  588 | 		}
  589 | 		return false;
  590 | 	}
  591 | 
  592 | 	/**
  593 | 	 * Whether the given type is provided by the JDK itself. JDK types are loaded either
  594 | 	 * by the bootstrap class loader (for example everything in {@code java.base}, which
  595 | 	 * reports a {@code null} class loader) or by the platform class loader (the other
  596 | 	 * platform modules); application and library types are loaded by the application
  597 | 	 * class loader.
  598 | 	 */
  599 | 	private boolean isJdkClass(Class<?> type) {
  600 | 		ClassLoader classLoader = type.getClassLoader();
  601 | 		return classLoader == null || classLoader == ClassLoader.getPlatformClassLoader();
  602 | 	}
  603 | 
  604 | 	/**
  605 | 	 * Whether the given type belongs to a standard API namespace (Jakarta EE or the
  606 | 	 * legacy {@code javax} packages). These namespaces are reserved for specifications
  607 | 	 * and, unlike the JDK modules, are loaded by the application class loader, so they
  608 | 	 * are not detected by {@link #isJdkClass}.
  609 | 	 */
  610 | 	private boolean isStandardApiClass(Class<?> type) {
  611 | 		String packageName = type.getPackageName();
  612 | 		return packageName.startsWith("jakarta.") || packageName.startsWith("javax.");
  613 | 	}
  614 | 
  615 | 	@ManagedAttribute
  616 | 	public Set<String> getNeverRefreshable() {
  617 | 		String neverRefresh = this.applicationContext.getEnvironment()
  618 | 			.getProperty("spring.cloud.refresh.never-refreshable", "com.zaxxer.hikari.HikariDataSource");
  619 | 		return StringUtils.commaDelimitedListToSet(neverRefresh);
  620 | 	}
  621 | 
  622 | 	@ManagedAttribute
  623 | 	public Set<String> getBeanNames() {
  624 | 		return new HashSet<>(this.beans.getBeanNames());
  625 | 	}
  626 | 
  627 | 	@Override
  628 | 	public void onApplicationEvent(EnvironmentChangeEvent event) {
  629 | 		if (this.applicationContext.equals(event.getSource())
  630 | 				// Backwards compatible
  631 | 				|| event.getKeys().equals(event.getSource())) {
  632 | 			rebind();
  633 | 		}
  634 | 	}
  635 | 
  636 | }
```
