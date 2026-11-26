<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java -->
<!-- 正文引用：[[源码:DefaultSingletonBeanRegistry.java#L起-L止]] ；代码块头部：(src: DefaultSingletonBeanRegistry.java#L起-L止) -->

# DefaultSingletonBeanRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/support/DefaultSingletonBeanRegistry.java` |
| 行数 | 838 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.053Z |

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
   19 | import java.util.Collections;
   20 | import java.util.HashMap;
   21 | import java.util.HashSet;
   22 | import java.util.Iterator;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.Map;
   26 | import java.util.Set;
   27 | import java.util.concurrent.ConcurrentHashMap;
   28 | import java.util.concurrent.locks.Condition;
   29 | import java.util.concurrent.locks.Lock;
   30 | import java.util.concurrent.locks.ReentrantLock;
   31 | import java.util.function.Consumer;
   32 | 
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.beans.factory.BeanCreationException;
   36 | import org.springframework.beans.factory.BeanCreationNotAllowedException;
   37 | import org.springframework.beans.factory.BeanCurrentlyInCreationException;
   38 | import org.springframework.beans.factory.DisposableBean;
   39 | import org.springframework.beans.factory.ObjectFactory;
   40 | import org.springframework.beans.factory.config.SingletonBeanRegistry;
   41 | import org.springframework.core.SimpleAliasRegistry;
   42 | import org.springframework.util.Assert;
   43 | import org.springframework.util.StringUtils;
   44 | 
   45 | /**
   46 |  * Generic registry for shared bean instances, implementing the
   47 |  * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
   48 |  * Allows for registering singleton instances that should be shared
   49 |  * for all callers of the registry, to be obtained via bean name.
   50 |  *
   51 |  * <p>Also supports registration of
   52 |  * {@link org.springframework.beans.factory.DisposableBean} instances,
   53 |  * (which might or might not correspond to registered singletons),
   54 |  * to be destroyed on shutdown of the registry. Dependencies between
   55 |  * beans can be registered to enforce an appropriate shutdown order.
   56 |  *
   57 |  * <p>This class mainly serves as base class for
   58 |  * {@link org.springframework.beans.factory.BeanFactory} implementations,
   59 |  * factoring out the common management of singleton bean instances. Note that
   60 |  * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
   61 |  * interface extends the {@link SingletonBeanRegistry} interface.
   62 |  *
   63 |  * <p>Note that this class assumes neither a bean definition concept
   64 |  * nor a specific creation process for bean instances, in contrast to
   65 |  * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
   66 |  * (which inherit from it). Can alternatively also be used as a nested
   67 |  * helper to delegate to.
   68 |  *
   69 |  * @author Juergen Hoeller
   70 |  * @since 2.0
   71 |  * @see #registerSingleton
   72 |  * @see #registerDisposableBean
   73 |  * @see org.springframework.beans.factory.DisposableBean
   74 |  * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
   75 |  */
   76 | public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
   77 | 
   78 | 	/** Maximum number of suppressed exceptions to preserve. */
   79 | 	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;
   80 | 
   81 | 
   82 | 	/** Common lock for singleton creation. */
   83 | 	final Lock singletonLock = new ReentrantLock();
   84 | 
   85 | 	/** Cache of singleton objects: bean name to bean instance. */
   86 | 	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
   87 | 
   88 | 	/** Creation-time registry of singleton factories: bean name to ObjectFactory. */
   89 | 	private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
   90 | 
   91 | 	/** Custom callbacks for singleton creation/registration. */
   92 | 	private final Map<String, Consumer<Object>> singletonCallbacks = new ConcurrentHashMap<>(16);
   93 | 
   94 | 	/** Cache of early singleton objects: bean name to bean instance. */
   95 | 	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
   96 | 
   97 | 	/** Set of registered singletons, containing the bean names in registration order. */
   98 | 	private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));
   99 | 
  100 | 	/** Names of beans that are currently in creation. */
  101 | 	private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);
  102 | 
  103 | 	/** Names of beans currently excluded from in creation checks. */
  104 | 	private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);
  105 | 
  106 | 	/** Specific lock for lenient creation tracking. */
  107 | 	private final Lock lenientCreationLock = new ReentrantLock();
  108 | 
  109 | 	/** Specific lock condition for lenient creation tracking. */
  110 | 	private final Condition lenientCreationFinished = this.lenientCreationLock.newCondition();
  111 | 
  112 | 	/** Names of beans that are currently in lenient creation. */
  113 | 	private final Set<String> singletonsInLenientCreation = new HashSet<>();
  114 | 
  115 | 	/** Map from one creation thread waiting on a lenient creation thread. */
  116 | 	private final Map<Thread, Thread> lenientWaitingThreads = new HashMap<>();
  117 | 
  118 | 	/** Map from bean name to actual creation thread for currently created beans. */
  119 | 	private final Map<String, Thread> currentCreationThreads = new ConcurrentHashMap<>();
  120 | 
  121 | 	/** Flag that indicates whether we're currently within destroySingletons. */
  122 | 	private volatile boolean singletonsCurrentlyInDestruction = false;
  123 | 
  124 | 	/** Collection of suppressed Exceptions, available for associating related causes. */
  125 | 	private @Nullable Set<Exception> suppressedExceptions;
  126 | 
  127 | 	/** Disposable bean instances: bean name to disposable instance. */
  128 | 	private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();
  129 | 
  130 | 	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
  131 | 	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);
  132 | 
  133 | 	/** Map between dependent bean names: bean name to Set of dependent bean names. */
  134 | 	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);
  135 | 
  136 | 	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
  137 | 	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);
  138 | 
  139 | 
  140 | 	@Override
  141 | 	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
  142 | 		Assert.notNull(beanName, "Bean name must not be null");
  143 | 		Assert.notNull(singletonObject, "Singleton object must not be null");
  144 | 		this.singletonLock.lock();
  145 | 		try {
  146 | 			addSingleton(beanName, singletonObject);
  147 | 		}
  148 | 		finally {
  149 | 			this.singletonLock.unlock();
  150 | 		}
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Add the given singleton object to the singleton registry.
  155 | 	 * <p>To be called for exposure of freshly registered/created singletons.
  156 | 	 * @param beanName the name of the bean
  157 | 	 * @param singletonObject the singleton object
  158 | 	 */
  159 | 	protected void addSingleton(String beanName, Object singletonObject) {
  160 | 		Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
  161 | 		if (oldObject != null) {
  162 | 			throw new IllegalStateException("Could not register object [" + singletonObject +
  163 | 					"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
  164 | 		}
  165 | 		this.singletonFactories.remove(beanName);
  166 | 		this.earlySingletonObjects.remove(beanName);
  167 | 		this.registeredSingletons.add(beanName);
  168 | 
  169 | 		Consumer<Object> callback = this.singletonCallbacks.get(beanName);
  170 | 		if (callback != null) {
  171 | 			callback.accept(singletonObject);
  172 | 		}
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Add the given singleton factory for building the specified singleton
  177 | 	 * if necessary.
  178 | 	 * <p>To be called for early exposure purposes, for example, to be able to
  179 | 	 * resolve circular references.
  180 | 	 * @param beanName the name of the bean
  181 | 	 * @param singletonFactory the factory for the singleton object
  182 | 	 */
  183 | 	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
  184 | 		Assert.notNull(singletonFactory, "Singleton factory must not be null");
  185 | 		this.singletonFactories.put(beanName, singletonFactory);
  186 | 		this.earlySingletonObjects.remove(beanName);
  187 | 		this.registeredSingletons.add(beanName);
  188 | 	}
  189 | 
  190 | 	@Override
  191 | 	public void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer) {
  192 | 		this.singletonCallbacks.put(beanName, singletonConsumer);
  193 | 	}
  194 | 
  195 | 	@Override
  196 | 	public @Nullable Object getSingleton(String beanName) {
  197 | 		return getSingleton(beanName, true);
  198 | 	}
  199 | 
  200 | 	/**
  201 | 	 * Return the (raw) singleton object registered under the given name.
  202 | 	 * <p>Checks already instantiated singletons and also allows for an early
  203 | 	 * reference to a currently created singleton (resolving a circular reference).
  204 | 	 * @param beanName the name of the bean to look for
  205 | 	 * @param allowEarlyReference whether early references should be created or not
  206 | 	 * @return the registered singleton object, or {@code null} if none found
  207 | 	 */
  208 | 	protected @Nullable Object getSingleton(String beanName, boolean allowEarlyReference) {
  209 | 		// Quick check for existing instance without full singleton lock.
  210 | 		Object singletonObject = this.singletonObjects.get(beanName);
  211 | 		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
  212 | 			singletonObject = this.earlySingletonObjects.get(beanName);
  213 | 			if (singletonObject == null && allowEarlyReference) {
  214 | 				if (!this.singletonLock.tryLock()) {
  215 | 					// Avoid early singleton inference outside of original creation thread.
  216 | 					return null;
  217 | 				}
  218 | 				try {
  219 | 					// Consistent creation of early reference within full singleton lock.
  220 | 					singletonObject = this.singletonObjects.get(beanName);
  221 | 					if (singletonObject == null) {
  222 | 						singletonObject = this.earlySingletonObjects.get(beanName);
  223 | 						if (singletonObject == null) {
  224 | 							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
  225 | 							if (singletonFactory != null) {
  226 | 								singletonObject = singletonFactory.getObject();
  227 | 								// Singleton could have been added or removed in the meantime.
  228 | 								if (this.singletonFactories.remove(beanName) != null) {
  229 | 									this.earlySingletonObjects.put(beanName, singletonObject);
  230 | 								}
  231 | 								else {
  232 | 									singletonObject = this.singletonObjects.get(beanName);
  233 | 								}
  234 | 							}
  235 | 						}
  236 | 					}
  237 | 				}
  238 | 				finally {
  239 | 					this.singletonLock.unlock();
  240 | 				}
  241 | 			}
  242 | 		}
  243 | 		return singletonObject;
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * Return the (raw) singleton object registered under the given name,
  248 | 	 * creating and registering a new one if none registered yet.
  249 | 	 * @param beanName the name of the bean
  250 | 	 * @param singletonFactory the ObjectFactory to lazily create the singleton
  251 | 	 * with, if necessary
  252 | 	 * @return the registered singleton object
  253 | 	 */
  254 | 	@SuppressWarnings("NullAway") // Dataflow analysis limitation
  255 | 	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
  256 | 		Assert.notNull(beanName, "Bean name must not be null");
  257 | 
  258 | 		Thread currentThread = Thread.currentThread();
  259 | 		Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock();
  260 | 		boolean acquireLock = !Boolean.FALSE.equals(lockFlag);
  261 | 		boolean locked = (acquireLock && this.singletonLock.tryLock());
  262 | 
  263 | 		try {
  264 | 			Object singletonObject = this.singletonObjects.get(beanName);
  265 | 			if (singletonObject == null) {
  266 | 				if (acquireLock && !locked) {
  267 | 					if (Boolean.TRUE.equals(lockFlag)) {
  268 | 						// Another thread is busy in a singleton factory callback, potentially blocked.
  269 | 						// Fallback as of 6.2: process given singleton bean outside of singleton lock.
  270 | 						// Thread-safe exposure is still guaranteed, there is just a risk of collisions
  271 | 						// when triggering creation of other beans as dependencies of the current bean.
  272 | 						this.lenientCreationLock.lock();
  273 | 						try {
  274 | 							if (logger.isInfoEnabled()) {
  275 | 								Set<String> lockedBeans = new HashSet<>(this.singletonsCurrentlyInCreation);
  276 | 								lockedBeans.removeAll(this.singletonsInLenientCreation);
  277 | 								logger.info("Obtaining singleton bean '" + beanName + "' in thread \"" +
  278 | 										currentThread.getName() + "\" while other thread holds singleton " +
  279 | 										"lock for other beans " + lockedBeans);
  280 | 							}
  281 | 							this.singletonsInLenientCreation.add(beanName);
  282 | 						}
  283 | 						finally {
  284 | 							this.lenientCreationLock.unlock();
  285 | 						}
  286 | 					}
  287 | 					else {
  288 | 						// No specific locking indication (outside a coordinated bootstrap) and
  289 | 						// singleton lock currently held by some other creation method -> wait.
  290 | 						this.singletonLock.lock();
  291 | 						locked = true;
  292 | 						// Singleton object might have possibly appeared in the meantime.
  293 | 						singletonObject = this.singletonObjects.get(beanName);
  294 | 						if (singletonObject != null) {
  295 | 							return singletonObject;
  296 | 						}
  297 | 					}
  298 | 				}
  299 | 
  300 | 				if (this.singletonsCurrentlyInDestruction) {
  301 | 					throw new BeanCreationNotAllowedException(beanName,
  302 | 							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
  303 | 							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
  304 | 				}
  305 | 				if (logger.isDebugEnabled()) {
  306 | 					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
  307 | 				}
  308 | 
  309 | 				try {
  310 | 					beforeSingletonCreation(beanName);
  311 | 				}
  312 | 				catch (BeanCurrentlyInCreationException ex) {
  313 | 					this.lenientCreationLock.lock();
  314 | 					try {
  315 | 						while ((singletonObject = this.singletonObjects.get(beanName)) == null) {
  316 | 							Thread otherThread = this.currentCreationThreads.get(beanName);
  317 | 							if (otherThread != null && (otherThread == currentThread ||
  318 | 									checkDependentWaitingThreads(otherThread, currentThread))) {
  319 | 								throw ex;
  320 | 							}
  321 | 							if (!this.singletonsInLenientCreation.contains(beanName)) {
  322 | 								break;
  323 | 							}
  324 | 							if (otherThread != null) {
  325 | 								this.lenientWaitingThreads.put(currentThread, otherThread);
  326 | 							}
  327 | 							try {
  328 | 								this.lenientCreationFinished.await();
  329 | 							}
  330 | 							catch (InterruptedException ie) {
  331 | 								currentThread.interrupt();
  332 | 							}
  333 | 							finally {
  334 | 								if (otherThread != null) {
  335 | 									this.lenientWaitingThreads.remove(currentThread);
  336 | 								}
  337 | 							}
  338 | 						}
  339 | 					}
  340 | 					finally {
  341 | 						this.lenientCreationLock.unlock();
  342 | 					}
  343 | 					if (singletonObject != null) {
  344 | 						return singletonObject;
  345 | 					}
  346 | 					if (locked) {
  347 | 						throw ex;
  348 | 					}
  349 | 					// Try late locking for waiting on specific bean to be finished.
  350 | 					this.singletonLock.lock();
  351 | 					locked = true;
  352 | 					// Lock-created singleton object should have appeared in the meantime.
  353 | 					singletonObject = this.singletonObjects.get(beanName);
  354 | 					if (singletonObject != null) {
  355 | 						return singletonObject;
  356 | 					}
  357 | 					beforeSingletonCreation(beanName);
  358 | 				}
  359 | 
  360 | 				boolean newSingleton = false;
  361 | 				boolean recordSuppressedExceptions = (locked && this.suppressedExceptions == null);
  362 | 				if (recordSuppressedExceptions) {
  363 | 					this.suppressedExceptions = new LinkedHashSet<>();
  364 | 				}
  365 | 				try {
  366 | 					// Leniently created singleton object could have appeared in the meantime.
  367 | 					singletonObject = this.singletonObjects.get(beanName);
  368 | 					if (singletonObject == null) {
  369 | 						this.currentCreationThreads.put(beanName, currentThread);
  370 | 						try {
  371 | 							singletonObject = singletonFactory.getObject();
  372 | 						}
  373 | 						finally {
  374 | 							this.currentCreationThreads.remove(beanName);
  375 | 						}
  376 | 						newSingleton = true;
  377 | 					}
  378 | 				}
  379 | 				catch (IllegalStateException ex) {
  380 | 					// Has the singleton object implicitly appeared in the meantime ->
  381 | 					// if yes, proceed with it since the exception indicates that state.
  382 | 					singletonObject = this.singletonObjects.get(beanName);
  383 | 					if (singletonObject == null) {
  384 | 						throw ex;
  385 | 					}
  386 | 				}
  387 | 				catch (BeanCreationException ex) {
  388 | 					if (recordSuppressedExceptions) {
  389 | 						for (Exception suppressedException : this.suppressedExceptions) {
  390 | 							ex.addRelatedCause(suppressedException);
  391 | 						}
  392 | 					}
  393 | 					throw ex;
  394 | 				}
  395 | 				finally {
  396 | 					if (recordSuppressedExceptions) {
  397 | 						this.suppressedExceptions = null;
  398 | 					}
  399 | 					afterSingletonCreation(beanName);
  400 | 				}
  401 | 
  402 | 				if (newSingleton) {
  403 | 					try {
  404 | 						addSingleton(beanName, singletonObject);
  405 | 					}
  406 | 					catch (IllegalStateException ex) {
  407 | 						// Leniently accept same instance if implicitly appeared.
  408 | 						Object object = this.singletonObjects.get(beanName);
  409 | 						if (singletonObject != object) {
  410 | 							throw ex;
  411 | 						}
  412 | 					}
  413 | 				}
  414 | 			}
  415 | 			return singletonObject;
  416 | 		}
  417 | 		finally {
  418 | 			if (locked) {
  419 | 				this.singletonLock.unlock();
  420 | 			}
  421 | 			this.lenientCreationLock.lock();
  422 | 			try {
  423 | 				this.singletonsInLenientCreation.remove(beanName);
  424 | 				this.lenientWaitingThreads.entrySet().removeIf(
  425 | 						entry -> entry.getValue() == currentThread);
  426 | 				this.lenientCreationFinished.signalAll();
  427 | 			}
  428 | 			finally {
  429 | 				this.lenientCreationLock.unlock();
  430 | 			}
  431 | 		}
  432 | 	}
  433 | 
  434 | 	private boolean checkDependentWaitingThreads(Thread waitingThread, Thread candidateThread) {
  435 | 		Thread threadToCheck = waitingThread;
  436 | 		while ((threadToCheck = this.lenientWaitingThreads.get(threadToCheck)) != null) {
  437 | 			if (threadToCheck == candidateThread) {
  438 | 				return true;
  439 | 			}
  440 | 		}
  441 | 		return false;
  442 | 	}
  443 | 
  444 | 	/**
  445 | 	 * Determine whether the current thread is allowed to hold the singleton lock.
  446 | 	 * <p>By default, all threads are forced to hold a full lock through {@code null}.
  447 | 	 * {@link DefaultListableBeanFactory} overrides this to specifically handle its
  448 | 	 * threads during the pre-instantiation phase: {@code true} for the main thread,
  449 | 	 * {@code false} for managed background threads, and configuration-dependent
  450 | 	 * behavior for unmanaged threads.
  451 | 	 * @return {@code true} if the current thread is explicitly allowed to hold the
  452 | 	 * lock but also accepts lenient fallback behavior, {@code false} if it is
  453 | 	 * explicitly not allowed to hold the lock and therefore forced to use lenient
  454 | 	 * fallback behavior, or {@code null} if there is no specific indication
  455 | 	 * (traditional behavior: forced to always hold a full lock)
  456 | 	 * @since 6.2
  457 | 	 */
  458 | 	protected @Nullable Boolean isCurrentThreadAllowedToHoldSingletonLock() {
  459 | 		return null;
  460 | 	}
  461 | 
  462 | 	/**
  463 | 	 * Register an exception that happened to get suppressed during the creation of a
  464 | 	 * singleton bean instance, for example, a temporary circular reference resolution problem.
  465 | 	 * <p>The default implementation preserves any given exception in this registry's
  466 | 	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
  467 | 	 * them as related causes to an eventual top-level {@link BeanCreationException}.
  468 | 	 * @param ex the Exception to register
  469 | 	 * @see BeanCreationException#getRelatedCauses()
  470 | 	 */
  471 | 	protected void onSuppressedException(Exception ex) {
  472 | 		if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
  473 | 			this.suppressedExceptions.add(ex);
  474 | 		}
  475 | 	}
  476 | 
  477 | 	/**
  478 | 	 * Remove the bean with the given name from the singleton registry, either on
  479 | 	 * regular destruction or on cleanup after early exposure when creation failed.
  480 | 	 * @param beanName the name of the bean
  481 | 	 */
  482 | 	protected void removeSingleton(String beanName) {
  483 | 		this.singletonObjects.remove(beanName);
  484 | 		this.singletonFactories.remove(beanName);
  485 | 		this.earlySingletonObjects.remove(beanName);
  486 | 		this.registeredSingletons.remove(beanName);
  487 | 	}
  488 | 
  489 | 	@Override
  490 | 	public boolean containsSingleton(String beanName) {
  491 | 		return this.singletonObjects.containsKey(beanName);
  492 | 	}
  493 | 
  494 | 	@Override
  495 | 	public String[] getSingletonNames() {
  496 | 		return StringUtils.toStringArray(this.registeredSingletons);
  497 | 	}
  498 | 
  499 | 	@Override
  500 | 	public int getSingletonCount() {
  501 | 		return this.registeredSingletons.size();
  502 | 	}
  503 | 
  504 | 
  505 | 	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
  506 | 		Assert.notNull(beanName, "Bean name must not be null");
  507 | 		if (!inCreation) {
  508 | 			this.inCreationCheckExclusions.add(beanName);
  509 | 		}
  510 | 		else {
  511 | 			this.inCreationCheckExclusions.remove(beanName);
  512 | 		}
  513 | 	}
  514 | 
  515 | 	public boolean isCurrentlyInCreation(String beanName) {
  516 | 		Assert.notNull(beanName, "Bean name must not be null");
  517 | 		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
  518 | 	}
  519 | 
  520 | 	protected boolean isActuallyInCreation(String beanName) {
  521 | 		return isSingletonCurrentlyInCreation(beanName);
  522 | 	}
  523 | 
  524 | 	/**
  525 | 	 * Return whether the specified singleton bean is currently in creation
  526 | 	 * (within the entire factory).
  527 | 	 * @param beanName the name of the bean
  528 | 	 */
  529 | 	public boolean isSingletonCurrentlyInCreation(@Nullable String beanName) {
  530 | 		return this.singletonsCurrentlyInCreation.contains(beanName);
  531 | 	}
  532 | 
  533 | 	/**
  534 | 	 * Callback before singleton creation.
  535 | 	 * <p>The default implementation registers the singleton as currently in creation.
  536 | 	 * @param beanName the name of the singleton about to be created
  537 | 	 * @see #isSingletonCurrentlyInCreation
  538 | 	 */
  539 | 	protected void beforeSingletonCreation(String beanName) {
  540 | 		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
  541 | 			throw new BeanCurrentlyInCreationException(beanName);
  542 | 		}
  543 | 	}
  544 | 
  545 | 	/**
  546 | 	 * Callback after singleton creation.
  547 | 	 * <p>The default implementation marks the singleton as not in creation anymore.
  548 | 	 * @param beanName the name of the singleton that has been created
  549 | 	 * @see #isSingletonCurrentlyInCreation
  550 | 	 */
  551 | 	protected void afterSingletonCreation(String beanName) {
  552 | 		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
  553 | 			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
  554 | 		}
  555 | 	}
  556 | 
  557 | 
  558 | 	/**
  559 | 	 * Add the given bean to the list of disposable beans in this registry.
  560 | 	 * <p>Disposable beans usually correspond to registered singletons,
  561 | 	 * matching the bean name but potentially being a different instance
  562 | 	 * (for example, a DisposableBean adapter for a singleton that does not
  563 | 	 * naturally implement Spring's DisposableBean interface).
  564 | 	 * @param beanName the name of the bean
  565 | 	 * @param bean the bean instance
  566 | 	 */
  567 | 	public void registerDisposableBean(String beanName, DisposableBean bean) {
  568 | 		synchronized (this.disposableBeans) {
  569 | 			this.disposableBeans.put(beanName, bean);
  570 | 		}
  571 | 	}
  572 | 
  573 | 	/**
  574 | 	 * Register a containment relationship between two beans,
  575 | 	 * for example, between an inner bean and its containing outer bean.
  576 | 	 * <p>Also registers the containing bean as dependent on the contained bean
  577 | 	 * in terms of destruction order.
  578 | 	 * @param containedBeanName the name of the contained (inner) bean
  579 | 	 * @param containingBeanName the name of the containing (outer) bean
  580 | 	 * @see #registerDependentBean
  581 | 	 */
  582 | 	public void registerContainedBean(String containedBeanName, String containingBeanName) {
  583 | 		synchronized (this.containedBeanMap) {
  584 | 			Set<String> containedBeans =
  585 | 					this.containedBeanMap.computeIfAbsent(containingBeanName, key -> new LinkedHashSet<>(8));
  586 | 			if (!containedBeans.add(containedBeanName)) {
  587 | 				return;
  588 | 			}
  589 | 		}
  590 | 		registerDependentBean(containedBeanName, containingBeanName);
  591 | 	}
  592 | 
  593 | 	/**
  594 | 	 * Register a dependent bean for the given bean,
  595 | 	 * to be destroyed before the given bean is destroyed.
  596 | 	 * @param beanName the name of the bean
  597 | 	 * @param dependentBeanName the name of the dependent bean
  598 | 	 */
  599 | 	public void registerDependentBean(String beanName, String dependentBeanName) {
  600 | 		String canonicalName = canonicalName(beanName);
  601 | 
  602 | 		synchronized (this.dependentBeanMap) {
  603 | 			Set<String> dependentBeans =
  604 | 					this.dependentBeanMap.computeIfAbsent(canonicalName, key -> new LinkedHashSet<>(8));
  605 | 			if (!dependentBeans.add(dependentBeanName)) {
  606 | 				return;
  607 | 			}
  608 | 		}
  609 | 
  610 | 		synchronized (this.dependenciesForBeanMap) {
  611 | 			Set<String> dependenciesForBean =
  612 | 					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, key -> new LinkedHashSet<>(8));
  613 | 			dependenciesForBean.add(canonicalName);
  614 | 		}
  615 | 	}
  616 | 
  617 | 	/**
  618 | 	 * Determine whether the specified dependent bean has been registered as
  619 | 	 * dependent on the given bean or on any of its transitive dependencies.
  620 | 	 * @param beanName the name of the bean to check
  621 | 	 * @param dependentBeanName the name of the dependent bean
  622 | 	 * @since 4.0
  623 | 	 */
  624 | 	protected boolean isDependent(String beanName, String dependentBeanName) {
  625 | 		synchronized (this.dependentBeanMap) {
  626 | 			return isDependent(beanName, dependentBeanName, null);
  627 | 		}
  628 | 	}
  629 | 
  630 | 	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
  631 | 		if (alreadySeen != null && alreadySeen.contains(beanName)) {
  632 | 			return false;
  633 | 		}
  634 | 		String canonicalName = canonicalName(beanName);
  635 | 		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
  636 | 		if (dependentBeans == null || dependentBeans.isEmpty()) {
  637 | 			return false;
  638 | 		}
  639 | 		if (dependentBeans.contains(dependentBeanName)) {
  640 | 			return true;
  641 | 		}
  642 | 		if (alreadySeen == null) {
  643 | 			alreadySeen = new HashSet<>();
  644 | 		}
  645 | 		alreadySeen.add(beanName);
  646 | 		for (String transitiveDependency : dependentBeans) {
  647 | 			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
  648 | 				return true;
  649 | 			}
  650 | 		}
  651 | 		return false;
  652 | 	}
  653 | 
  654 | 	/**
  655 | 	 * Determine whether a dependent bean has been registered for the given name.
  656 | 	 * @param beanName the name of the bean to check
  657 | 	 */
  658 | 	protected boolean hasDependentBean(String beanName) {
  659 | 		return this.dependentBeanMap.containsKey(beanName);
  660 | 	}
  661 | 
  662 | 	/**
  663 | 	 * Return the names of all beans which depend on the specified bean, if any.
  664 | 	 * @param beanName the name of the bean
  665 | 	 * @return the array of dependent bean names, or an empty array if none
  666 | 	 */
  667 | 	public String[] getDependentBeans(String beanName) {
  668 | 		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
  669 | 		if (dependentBeans == null) {
  670 | 			return new String[0];
  671 | 		}
  672 | 		synchronized (this.dependentBeanMap) {
  673 | 			return StringUtils.toStringArray(dependentBeans);
  674 | 		}
  675 | 	}
  676 | 
  677 | 	/**
  678 | 	 * Return the names of all beans that the specified bean depends on, if any.
  679 | 	 * @param beanName the name of the bean
  680 | 	 * @return the array of names of beans which the bean depends on,
  681 | 	 * or an empty array if none
  682 | 	 */
  683 | 	public String[] getDependenciesForBean(String beanName) {
  684 | 		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
  685 | 		if (dependenciesForBean == null) {
  686 | 			return new String[0];
  687 | 		}
  688 | 		synchronized (this.dependenciesForBeanMap) {
  689 | 			return StringUtils.toStringArray(dependenciesForBean);
  690 | 		}
  691 | 	}
  692 | 
  693 | 	public void destroySingletons() {
  694 | 		if (logger.isTraceEnabled()) {
  695 | 			logger.trace("Destroying singletons in " + this);
  696 | 		}
  697 | 		this.singletonsCurrentlyInDestruction = true;
  698 | 
  699 | 		String[] disposableBeanNames;
  700 | 		synchronized (this.disposableBeans) {
  701 | 			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
  702 | 		}
  703 | 		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
  704 | 			destroySingleton(disposableBeanNames[i]);
  705 | 		}
  706 | 
  707 | 		this.containedBeanMap.clear();
  708 | 		this.dependentBeanMap.clear();
  709 | 		this.dependenciesForBeanMap.clear();
  710 | 
  711 | 		this.singletonLock.lock();
  712 | 		try {
  713 | 			clearSingletonCache();
  714 | 		}
  715 | 		finally {
  716 | 			this.singletonLock.unlock();
  717 | 		}
  718 | 	}
  719 | 
  720 | 	/**
  721 | 	 * Clear all cached singleton instances in this registry.
  722 | 	 * @since 4.3.15
  723 | 	 */
  724 | 	protected void clearSingletonCache() {
  725 | 		this.singletonObjects.clear();
  726 | 		this.singletonFactories.clear();
  727 | 		this.earlySingletonObjects.clear();
  728 | 		this.registeredSingletons.clear();
  729 | 		this.singletonsCurrentlyInDestruction = false;
  730 | 	}
  731 | 
  732 | 	/**
  733 | 	 * Destroy the given bean. Delegates to {@code destroyBean}
  734 | 	 * if a corresponding disposable bean instance is found.
  735 | 	 * @param beanName the name of the bean
  736 | 	 * @see #destroyBean
  737 | 	 */
  738 | 	public void destroySingleton(String beanName) {
  739 | 		// Destroy the corresponding DisposableBean instance.
  740 | 		// This also triggers the destruction of dependent beans.
  741 | 		DisposableBean disposableBean;
  742 | 		synchronized (this.disposableBeans) {
  743 | 			disposableBean = this.disposableBeans.remove(beanName);
  744 | 		}
  745 | 		destroyBean(beanName, disposableBean);
  746 | 
  747 | 		// destroySingletons() removes all singleton instances at the end,
  748 | 		// leniently tolerating late retrieval during the shutdown phase.
  749 | 		if (!this.singletonsCurrentlyInDestruction) {
  750 | 			// For an individual destruction, remove the registered instance now.
  751 | 			// As of 6.2, this happens after the current bean's destruction step,
  752 | 			// allowing for late bean retrieval by on-demand suppliers etc.
  753 | 			if (this.currentCreationThreads.get(beanName) == Thread.currentThread()) {
  754 | 				// Local remove after failed creation step -> without singleton lock
  755 | 				// since bean creation may have happened leniently without any lock.
  756 | 				removeSingleton(beanName);
  757 | 			}
  758 | 			else {
  759 | 				this.singletonLock.lock();
  760 | 				try {
  761 | 					removeSingleton(beanName);
  762 | 				}
  763 | 				finally {
  764 | 					this.singletonLock.unlock();
  765 | 				}
  766 | 			}
  767 | 		}
  768 | 	}
  769 | 
  770 | 	/**
  771 | 	 * Destroy the given bean. Must destroy beans that depend on the given
  772 | 	 * bean before the bean itself. Should not throw any exceptions.
  773 | 	 * @param beanName the name of the bean
  774 | 	 * @param bean the bean instance to destroy
  775 | 	 */
  776 | 	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
  777 | 		// Trigger destruction of dependent beans first...
  778 | 		Set<String> dependentBeanNames;
  779 | 		synchronized (this.dependentBeanMap) {
  780 | 			// Within full synchronization in order to guarantee a disconnected Set
  781 | 			dependentBeanNames = this.dependentBeanMap.remove(beanName);
  782 | 		}
  783 | 		if (dependentBeanNames != null) {
  784 | 			if (logger.isTraceEnabled()) {
  785 | 				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependentBeanNames);
  786 | 			}
  787 | 			for (String dependentBeanName : dependentBeanNames) {
  788 | 				destroySingleton(dependentBeanName);
  789 | 			}
  790 | 		}
  791 | 
  792 | 		// Actually destroy the bean now...
  793 | 		if (bean != null) {
  794 | 			try {
  795 | 				bean.destroy();
  796 | 			}
  797 | 			catch (Throwable ex) {
  798 | 				if (logger.isWarnEnabled()) {
  799 | 					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
  800 | 				}
  801 | 			}
  802 | 		}
  803 | 
  804 | 		// Trigger destruction of contained beans...
  805 | 		Set<String> containedBeans;
  806 | 		synchronized (this.containedBeanMap) {
  807 | 			// Within full synchronization in order to guarantee a disconnected Set
  808 | 			containedBeans = this.containedBeanMap.remove(beanName);
  809 | 		}
  810 | 		if (containedBeans != null) {
  811 | 			for (String containedBeanName : containedBeans) {
  812 | 				destroySingleton(containedBeanName);
  813 | 			}
  814 | 		}
  815 | 
  816 | 		// Remove destroyed bean from other beans' dependencies.
  817 | 		synchronized (this.dependentBeanMap) {
  818 | 			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
  819 | 				Map.Entry<String, Set<String>> entry = it.next();
  820 | 				Set<String> dependenciesToClean = entry.getValue();
  821 | 				dependenciesToClean.remove(beanName);
  822 | 				if (dependenciesToClean.isEmpty()) {
  823 | 					it.remove();
  824 | 				}
  825 | 			}
  826 | 		}
  827 | 
  828 | 		// Remove destroyed bean's prepared dependency information.
  829 | 		this.dependenciesForBeanMap.remove(beanName);
  830 | 	}
  831 | 
  832 | 	@Deprecated(since = "6.2")
  833 | 	@Override
  834 | 	public final Object getSingletonMutex() {
  835 | 		return new Object();
  836 | 	}
  837 | 
  838 | }
```
