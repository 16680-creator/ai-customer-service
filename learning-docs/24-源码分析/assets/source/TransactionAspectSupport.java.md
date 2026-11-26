<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/TransactionAspectSupport.java -->
<!-- 正文引用：[[源码:TransactionAspectSupport.java#L起-L止]] ；代码块头部：(src: TransactionAspectSupport.java#L起-L止) -->

# TransactionAspectSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/TransactionAspectSupport.java` |
| 行数 | 1139 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:12.865Z |

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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.Properties;
   23 | import java.util.concurrent.ExecutionException;
   24 | import java.util.concurrent.Future;
   25 | 
   26 | import io.vavr.control.Try;
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | import reactor.core.publisher.Flux;
   31 | import reactor.core.publisher.Mono;
   32 | 
   33 | import org.springframework.beans.factory.BeanFactory;
   34 | import org.springframework.beans.factory.BeanFactoryAware;
   35 | import org.springframework.beans.factory.InitializingBean;
   36 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   37 | import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
   38 | import org.springframework.core.KotlinDetector;
   39 | import org.springframework.core.MethodParameter;
   40 | import org.springframework.core.NamedThreadLocal;
   41 | import org.springframework.core.ReactiveAdapter;
   42 | import org.springframework.core.ReactiveAdapterRegistry;
   43 | import org.springframework.transaction.NoTransactionException;
   44 | import org.springframework.transaction.PlatformTransactionManager;
   45 | import org.springframework.transaction.ReactiveTransaction;
   46 | import org.springframework.transaction.ReactiveTransactionManager;
   47 | import org.springframework.transaction.TransactionExecution;
   48 | import org.springframework.transaction.TransactionManager;
   49 | import org.springframework.transaction.TransactionStatus;
   50 | import org.springframework.transaction.TransactionSystemException;
   51 | import org.springframework.transaction.reactive.TransactionContextManager;
   52 | import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
   53 | import org.springframework.util.Assert;
   54 | import org.springframework.util.ClassUtils;
   55 | import org.springframework.util.ConcurrentReferenceHashMap;
   56 | import org.springframework.util.StringUtils;
   57 | 
   58 | /**
   59 |  * Base class for transactional aspects, such as the {@link TransactionInterceptor}
   60 |  * or an AspectJ aspect.
   61 |  *
   62 |  * <p>This enables the underlying Spring transaction infrastructure to be used easily
   63 |  * to implement an aspect for any aspect system.
   64 |  *
   65 |  * <p>Subclasses are responsible for calling methods in this class in the correct order.
   66 |  *
   67 |  * <p>If no transaction name has been specified in the {@link TransactionAttribute},
   68 |  * the exposed name will be the {@code fully-qualified class name + "." + method name}
   69 |  * (by default).
   70 |  *
   71 |  * <p>Uses the <b>Strategy</b> design pattern. A {@link PlatformTransactionManager} or
   72 |  * {@link ReactiveTransactionManager} implementation will perform the actual transaction
   73 |  * management, and a {@link TransactionAttributeSource} (for example, annotation-based) is used
   74 |  * for determining transaction definitions for a particular class or method.
   75 |  *
   76 |  * <p>A transaction aspect is serializable if its {@code TransactionManager} and
   77 |  * {@code TransactionAttributeSource} are serializable.
   78 |  *
   79 |  * @author Rod Johnson
   80 |  * @author Juergen Hoeller
   81 |  * @author Stéphane Nicoll
   82 |  * @author Sam Brannen
   83 |  * @author Mark Paluch
   84 |  * @author Sebastien Deleuze
   85 |  * @author Enric Sala
   86 |  * @since 1.1
   87 |  * @see PlatformTransactionManager
   88 |  * @see ReactiveTransactionManager
   89 |  * @see #setTransactionManager
   90 |  * @see #setTransactionAttributes
   91 |  * @see #setTransactionAttributeSource
   92 |  */
   93 | public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {
   94 | 
   95 | 	// NOTE: This class must not implement Serializable because it serves as base
   96 | 	// class for AspectJ aspects (which are not allowed to implement Serializable)!
   97 | 
   98 | 
   99 | 	/**
  100 | 	 * Key to use to store the default transaction manager.
  101 | 	 */
  102 | 	private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();
  103 | 
  104 | 	private static final String COROUTINES_FLOW_CLASS_NAME = "kotlinx.coroutines.flow.Flow";
  105 | 
  106 | 	/**
  107 | 	 * Reactive Streams API present on the classpath?
  108 | 	 */
  109 | 	private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
  110 | 			"org.reactivestreams.Publisher", TransactionAspectSupport.class.getClassLoader());
  111 | 
  112 | 	/**
  113 | 	 * Vavr library present on the classpath?
  114 | 	 */
  115 | 	private static final boolean VAVR_PRESENT = ClassUtils.isPresent(
  116 | 			"io.vavr.control.Try", TransactionAspectSupport.class.getClassLoader());
  117 | 
  118 | 	/**
  119 | 	 * Holder to support the {@code currentTransactionStatus()} method,
  120 | 	 * and to support communication between different cooperating advices
  121 | 	 * (for example, before and after advice) if the aspect involves more than a
  122 | 	 * single method (as will be the case for around advice).
  123 | 	 */
  124 | 	private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
  125 | 			new NamedThreadLocal<>("Current aspect-driven transaction");
  126 | 
  127 | 
  128 | 	/**
  129 | 	 * Subclasses can use this to return the current TransactionInfo.
  130 | 	 * Only subclasses that cannot handle all operations in one method,
  131 | 	 * such as an AspectJ aspect involving distinct before and after advice,
  132 | 	 * need to use this mechanism to get at the current TransactionInfo.
  133 | 	 * An around advice such as an AOP Alliance MethodInterceptor can hold a
  134 | 	 * reference to the TransactionInfo throughout the aspect method.
  135 | 	 * <p>A TransactionInfo will be returned even if no transaction was created.
  136 | 	 * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
  137 | 	 * <p>To find out about specific transaction characteristics, consider using
  138 | 	 * TransactionSynchronizationManager's {@code isSynchronizationActive()}
  139 | 	 * and/or {@code isActualTransactionActive()} methods.
  140 | 	 * @return the TransactionInfo bound to this thread, or {@code null} if none
  141 | 	 * @see TransactionInfo#hasTransaction()
  142 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isSynchronizationActive()
  143 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
  144 | 	 */
  145 | 	protected static @Nullable TransactionInfo currentTransactionInfo() throws NoTransactionException {
  146 | 		return transactionInfoHolder.get();
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Return the transaction status of the current method invocation.
  151 | 	 * Mainly intended for code that wants to set the current transaction
  152 | 	 * rollback-only but not throw an application exception.
  153 | 	 * <p>This exposes the locally declared transaction boundary with its declared name
  154 | 	 * and characteristics, as managed by the aspect. At runtime, the local boundary may
  155 | 	 * participate in an outer transaction: If you need transaction metadata from such
  156 | 	 * an outer transaction (the actual resource transaction) instead, consider using
  157 | 	 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}.
  158 | 	 * @throws NoTransactionException if the transaction info cannot be found,
  159 | 	 * because the method was invoked outside an AOP invocation context
  160 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionName()
  161 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
  162 | 	 */
  163 | 	public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
  164 | 		TransactionInfo info = currentTransactionInfo();
  165 | 		if (info == null || info.transactionStatus == null) {
  166 | 			throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
  167 | 		}
  168 | 		return info.transactionStatus;
  169 | 	}
  170 | 
  171 | 
  172 | 	protected final Log logger = LogFactory.getLog(getClass());
  173 | 
  174 | 	private final @Nullable ReactiveAdapterRegistry reactiveAdapterRegistry;
  175 | 
  176 | 	private @Nullable String transactionManagerBeanName;
  177 | 
  178 | 	private @Nullable TransactionManager transactionManager;
  179 | 
  180 | 	private @Nullable TransactionAttributeSource transactionAttributeSource;
  181 | 
  182 | 	private @Nullable BeanFactory beanFactory;
  183 | 
  184 | 	private final Map<Object, TransactionManager> transactionManagerCache =
  185 | 			new ConcurrentReferenceHashMap<>(4);
  186 | 
  187 | 	private final Map<Method, ReactiveTransactionSupport> transactionSupportCache =
  188 | 			new ConcurrentReferenceHashMap<>(1024);
  189 | 
  190 | 
  191 | 	protected TransactionAspectSupport() {
  192 | 		if (REACTIVE_STREAMS_PRESENT) {
  193 | 			this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
  194 | 		}
  195 | 		else {
  196 | 			this.reactiveAdapterRegistry = null;
  197 | 		}
  198 | 	}
  199 | 
  200 | 
  201 | 	/**
  202 | 	 * Specify the name of the default transaction manager bean.
  203 | 	 * <p>This can either point to a traditional {@link PlatformTransactionManager} or a
  204 | 	 * {@link ReactiveTransactionManager} for reactive transaction management.
  205 | 	 */
  206 | 	public void setTransactionManagerBeanName(@Nullable String transactionManagerBeanName) {
  207 | 		this.transactionManagerBeanName = transactionManagerBeanName;
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * Return the name of the default transaction manager bean.
  212 | 	 */
  213 | 	protected final @Nullable String getTransactionManagerBeanName() {
  214 | 		return this.transactionManagerBeanName;
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Specify the <em>default</em> transaction manager to use to drive transactions.
  219 | 	 * <p>This can either be a traditional {@link PlatformTransactionManager} or a
  220 | 	 * {@link ReactiveTransactionManager} for reactive transaction management.
  221 | 	 * <p>The default transaction manager will be used if a <em>qualifier</em>
  222 | 	 * has not been declared for a given transaction or if an explicit name for the
  223 | 	 * default transaction manager bean has not been specified.
  224 | 	 * @see #setTransactionManagerBeanName
  225 | 	 */
  226 | 	public void setTransactionManager(@Nullable TransactionManager transactionManager) {
  227 | 		this.transactionManager = transactionManager;
  228 | 	}
  229 | 
  230 | 	/**
  231 | 	 * Return the default transaction manager, or {@code null} if unknown.
  232 | 	 * <p>This can either be a traditional {@link PlatformTransactionManager} or a
  233 | 	 * {@link ReactiveTransactionManager} for reactive transaction management.
  234 | 	 */
  235 | 	public @Nullable TransactionManager getTransactionManager() {
  236 | 		return this.transactionManager;
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Set properties with method names as keys and transaction attribute
  241 | 	 * descriptors (parsed via TransactionAttributeEditor) as values:
  242 | 	 * for example, key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
  243 | 	 * <p>Note: Method names are always applied to the target class,
  244 | 	 * no matter if defined in an interface or the class itself.
  245 | 	 * <p>Internally, a NameMatchTransactionAttributeSource will be
  246 | 	 * created from the given properties.
  247 | 	 * @see #setTransactionAttributeSource
  248 | 	 * @see TransactionAttributeEditor
  249 | 	 * @see NameMatchTransactionAttributeSource
  250 | 	 */
  251 | 	public void setTransactionAttributes(Properties transactionAttributes) {
  252 | 		NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
  253 | 		tas.setProperties(transactionAttributes);
  254 | 		this.transactionAttributeSource = tas;
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * Set multiple transaction attribute sources which are used to find transaction
  259 | 	 * attributes. Will build a CompositeTransactionAttributeSource for the given sources.
  260 | 	 * @see CompositeTransactionAttributeSource
  261 | 	 * @see MethodMapTransactionAttributeSource
  262 | 	 * @see NameMatchTransactionAttributeSource
  263 | 	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
  264 | 	 */
  265 | 	public void setTransactionAttributeSources(TransactionAttributeSource... transactionAttributeSources) {
  266 | 		this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Set the transaction attribute source which is used to find transaction
  271 | 	 * attributes. If specifying a String property value, a PropertyEditor
  272 | 	 * will create a MethodMapTransactionAttributeSource from the value.
  273 | 	 * @see TransactionAttributeSourceEditor
  274 | 	 * @see MethodMapTransactionAttributeSource
  275 | 	 * @see NameMatchTransactionAttributeSource
  276 | 	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
  277 | 	 */
  278 | 	public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
  279 | 		this.transactionAttributeSource = transactionAttributeSource;
  280 | 	}
  281 | 
  282 | 	/**
  283 | 	 * Return the transaction attribute source.
  284 | 	 */
  285 | 	public @Nullable TransactionAttributeSource getTransactionAttributeSource() {
  286 | 		return this.transactionAttributeSource;
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Set the BeanFactory to use for retrieving {@code TransactionManager} beans.
  291 | 	 */
  292 | 	@Override
  293 | 	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
  294 | 		this.beanFactory = beanFactory;
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Return the BeanFactory to use for retrieving {@code TransactionManager} beans.
  299 | 	 */
  300 | 	protected final @Nullable BeanFactory getBeanFactory() {
  301 | 		return this.beanFactory;
  302 | 	}
  303 | 
  304 | 	/**
  305 | 	 * Check that required properties were set.
  306 | 	 */
  307 | 	@Override
  308 | 	public void afterPropertiesSet() {
  309 | 		if (getTransactionManager() == null && this.beanFactory == null) {
  310 | 			throw new IllegalStateException(
  311 | 					"Set the 'transactionManager' property or make sure to run within a BeanFactory " +
  312 | 					"containing a TransactionManager bean!");
  313 | 		}
  314 | 		if (getTransactionAttributeSource() == null) {
  315 | 			throw new IllegalStateException(
  316 | 					"Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
  317 | 					"If there are no transactional methods, then don't use a transaction aspect.");
  318 | 		}
  319 | 	}
  320 | 
  321 | 
  322 | 	/**
  323 | 	 * General delegate for around-advice-based subclasses, delegating to several other template
  324 | 	 * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
  325 | 	 * as well as regular {@link PlatformTransactionManager} implementations and
  326 | 	 * {@link ReactiveTransactionManager} implementations for reactive return types.
  327 | 	 * @param method the Method being invoked
  328 | 	 * @param targetClass the target class that we're invoking the method on
  329 | 	 * @param invocation the callback to use for proceeding with the target invocation
  330 | 	 * @return the return value of the method, if any
  331 | 	 * @throws Throwable propagated from the target invocation
  332 | 	 */
  333 | 	protected @Nullable Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
  334 | 			final InvocationCallback invocation) throws Throwable {
  335 | 
  336 | 		// If the transaction attribute is null, the method is non-transactional.
  337 | 		TransactionAttributeSource tas = getTransactionAttributeSource();
  338 | 		final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
  339 | 		final TransactionManager tm = determineTransactionManager(txAttr, targetClass);
  340 | 
  341 | 		if (this.reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager rtm) {
  342 | 			boolean isSuspendingFunction = KotlinDetector.isSuspendingFunction(method);
  343 | 			boolean hasSuspendingFlowReturnType = isSuspendingFunction &&
  344 | 					COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName());
  345 | 
  346 | 			ReactiveTransactionSupport txSupport = this.transactionSupportCache.computeIfAbsent(method, key -> {
  347 | 				Class<?> reactiveType =
  348 | 						(isSuspendingFunction ? (hasSuspendingFlowReturnType ? Flux.class : Mono.class) : method.getReturnType());
  349 | 				ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(reactiveType);
  350 | 				if (adapter == null) {
  351 | 					throw new IllegalStateException("Cannot apply reactive transaction to non-reactive return type [" +
  352 | 							method.getReturnType() + "] with specified transaction manager: " + tm);
  353 | 				}
  354 | 				return new ReactiveTransactionSupport(adapter);
  355 | 			});
  356 | 
  357 | 			return txSupport.invokeWithinTransaction(method, targetClass, invocation, txAttr, rtm);
  358 | 		}
  359 | 
  360 | 		PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
  361 | 		final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);
  362 | 
  363 | 		if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager cpptm)) {
  364 | 			// Standard transaction demarcation with getTransaction and commit/rollback calls.
  365 | 			TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);
  366 | 
  367 | 			Object retVal;
  368 | 			try {
  369 | 				// This is an around advice: Invoke the next interceptor in the chain.
  370 | 				// This will normally result in a target object being invoked.
  371 | 				retVal = invocation.proceedWithInvocation();
  372 | 			}
  373 | 			catch (Throwable ex) {
  374 | 				// target invocation exception
  375 | 				completeTransactionAfterThrowing(txInfo, invocation, ex);
  376 | 				throw ex;
  377 | 			}
  378 | 			finally {
  379 | 				cleanupTransactionInfo(txInfo);
  380 | 			}
  381 | 
  382 | 			if (retVal != null && txAttr != null) {
  383 | 				TransactionStatus status = txInfo.getTransactionStatus();
  384 | 				if (status != null) {
  385 | 					if (retVal instanceof Future<?> future && future.isDone()) {
  386 | 						try {
  387 | 							future.get();
  388 | 						}
  389 | 						catch (ExecutionException ex) {
  390 | 							Throwable cause = ex.getCause();
  391 | 							Assert.state(cause != null, "Cause must not be null");
  392 | 							if (txAttr.rollbackOn(cause)) {
  393 | 								invocation.onRollback(cause, status);
  394 | 								status.setRollbackOnly();
  395 | 							}
  396 | 						}
  397 | 						catch (InterruptedException ex) {
  398 | 							Thread.currentThread().interrupt();
  399 | 						}
  400 | 					}
  401 | 					else if (VAVR_PRESENT && VavrDelegate.isVavrTry(retVal)) {
  402 | 						// Set rollback-only in case of Vavr failure matching our rollback rules...
  403 | 						retVal = VavrDelegate.evaluateTryFailure(invocation, retVal, txAttr, status);
  404 | 					}
  405 | 				}
  406 | 			}
  407 | 
  408 | 			commitTransactionAfterReturning(txInfo);
  409 | 			return retVal;
  410 | 		}
  411 | 
  412 | 		else {
  413 | 			Object result;
  414 | 			final ThrowableHolder throwableHolder = new ThrowableHolder();
  415 | 
  416 | 			// It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
  417 | 			try {
  418 | 				result = cpptm.<@Nullable Object> execute(txAttr, status -> {
  419 | 					TransactionInfo txInfo = prepareTransactionInfo(ptm, txAttr, joinpointIdentification, status);
  420 | 					try {
  421 | 						Object retVal = invocation.proceedWithInvocation();
  422 | 						if (retVal != null && VAVR_PRESENT && VavrDelegate.isVavrTry(retVal)) {
  423 | 							// Set rollback-only in case of Vavr failure matching our rollback rules...
  424 | 							retVal = VavrDelegate.evaluateTryFailure(invocation, retVal, txAttr, status);
  425 | 						}
  426 | 						return retVal;
  427 | 					}
  428 | 					catch (Throwable ex) {
  429 | 						if (txAttr.rollbackOn(ex)) {
  430 | 							invocation.onRollback(ex, status);
  431 | 							// A RuntimeException: will lead to a rollback.
  432 | 							if (ex instanceof RuntimeException runtimeException) {
  433 | 								throw runtimeException;
  434 | 							}
  435 | 							else {
  436 | 								throw new ThrowableHolderException(ex);
  437 | 							}
  438 | 						}
  439 | 						else {
  440 | 							// A normal return value: will lead to a commit.
  441 | 							throwableHolder.throwable = ex;
  442 | 							return null;
  443 | 						}
  444 | 					}
  445 | 					finally {
  446 | 						cleanupTransactionInfo(txInfo);
  447 | 					}
  448 | 				});
  449 | 			}
  450 | 			catch (ThrowableHolderException ex) {
  451 | 				throw ex.getCause();
  452 | 			}
  453 | 			catch (TransactionSystemException ex2) {
  454 | 				if (throwableHolder.throwable != null) {
  455 | 					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
  456 | 					ex2.initApplicationException(throwableHolder.throwable);
  457 | 				}
  458 | 				throw ex2;
  459 | 			}
  460 | 			catch (Throwable ex2) {
  461 | 				if (throwableHolder.throwable != null) {
  462 | 					logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
  463 | 				}
  464 | 				throw ex2;
  465 | 			}
  466 | 
  467 | 			// Check result state: It might indicate a Throwable to rethrow.
  468 | 			if (throwableHolder.throwable != null) {
  469 | 				throw throwableHolder.throwable;
  470 | 			}
  471 | 			return result;
  472 | 		}
  473 | 	}
  474 | 
  475 | 	/**
  476 | 	 * Clear the transaction manager cache.
  477 | 	 */
  478 | 	protected void clearTransactionManagerCache() {
  479 | 		this.transactionManagerCache.clear();
  480 | 		this.beanFactory = null;
  481 | 	}
  482 | 
  483 | 	/**
  484 | 	 * Determine the specific transaction manager to use for the given transaction.
  485 | 	 * @param txAttr the current transaction attribute
  486 | 	 * @param targetClass the target class that the attribute has been declared on
  487 | 	 * @since 6.2
  488 | 	 */
  489 | 	protected @Nullable TransactionManager determineTransactionManager(
  490 | 			@Nullable TransactionAttribute txAttr, @Nullable Class<?> targetClass) {
  491 | 
  492 | 		TransactionManager tm = determineTransactionManager(txAttr);
  493 | 		if (tm != null) {
  494 | 			return tm;
  495 | 		}
  496 | 
  497 | 		// Do not attempt to lookup tx manager if no tx attributes are set
  498 | 		if (txAttr == null || this.beanFactory == null) {
  499 | 			return getTransactionManager();
  500 | 		}
  501 | 
  502 | 		String qualifier = txAttr.getQualifier();
  503 | 		if (StringUtils.hasText(qualifier)) {
  504 | 			return determineQualifiedTransactionManager(this.beanFactory, qualifier);
  505 | 		}
  506 | 		else if (targetClass != null) {
  507 | 			// Consider type-level qualifier annotations for transaction manager selection
  508 | 			String typeQualifier = BeanFactoryAnnotationUtils.getQualifierValue(targetClass);
  509 | 			if (StringUtils.hasText(typeQualifier)) {
  510 | 				try {
  511 | 					return determineQualifiedTransactionManager(this.beanFactory, typeQualifier);
  512 | 				}
  513 | 				catch (NoSuchBeanDefinitionException ex) {
  514 | 					// Consider type qualifier as optional, proceed with regular resolution below.
  515 | 				}
  516 | 			}
  517 | 		}
  518 | 
  519 | 		if (StringUtils.hasText(this.transactionManagerBeanName)) {
  520 | 			return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
  521 | 		}
  522 | 		else {
  523 | 			TransactionManager defaultTransactionManager = getTransactionManager();
  524 | 			if (defaultTransactionManager == null) {
  525 | 				defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
  526 | 				if (defaultTransactionManager == null) {
  527 | 					defaultTransactionManager = this.beanFactory.getBean(TransactionManager.class);
  528 | 					this.transactionManagerCache.putIfAbsent(
  529 | 							DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
  530 | 				}
  531 | 			}
  532 | 			return defaultTransactionManager;
  533 | 		}
  534 | 	}
  535 | 
  536 | 	/**
  537 | 	 * Determine the specific transaction manager to use for the given transaction.
  538 | 	 * @deprecated in favor of {@link #determineTransactionManager(TransactionAttribute, Class)}
  539 | 	 */
  540 | 	@Deprecated(since = "6.2")
  541 | 	protected @Nullable TransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
  542 | 		return null;
  543 | 	}
  544 | 
  545 | 	private TransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
  546 | 		TransactionManager txManager = this.transactionManagerCache.get(qualifier);
  547 | 		if (txManager == null) {
  548 | 			txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
  549 | 					beanFactory, TransactionManager.class, qualifier);
  550 | 			this.transactionManagerCache.putIfAbsent(qualifier, txManager);
  551 | 		}
  552 | 		return txManager;
  553 | 	}
  554 | 
  555 | 	private @Nullable PlatformTransactionManager asPlatformTransactionManager(@Nullable Object transactionManager) {
  556 | 		if (transactionManager == null) {
  557 | 			return null;
  558 | 		}
  559 | 		if (transactionManager instanceof PlatformTransactionManager ptm) {
  560 | 			return ptm;
  561 | 		}
  562 | 		else {
  563 | 			throw new IllegalStateException(
  564 | 					"Specified transaction manager is not a PlatformTransactionManager: " + transactionManager);
  565 | 		}
  566 | 	}
  567 | 
  568 | 	private String methodIdentification(Method method, @Nullable Class<?> targetClass,
  569 | 			@Nullable TransactionAttribute txAttr) {
  570 | 
  571 | 		String methodIdentification = methodIdentification(method, targetClass);
  572 | 		if (methodIdentification == null) {
  573 | 			if (txAttr instanceof DefaultTransactionAttribute dta) {
  574 | 				methodIdentification = dta.getDescriptor();
  575 | 			}
  576 | 			if (methodIdentification == null) {
  577 | 				methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
  578 | 			}
  579 | 		}
  580 | 		return methodIdentification;
  581 | 	}
  582 | 
  583 | 	/**
  584 | 	 * Convenience method to return a String representation of this Method
  585 | 	 * for use in logging. Can be overridden in subclasses to provide a
  586 | 	 * different identifier for the given method.
  587 | 	 * <p>The default implementation returns {@code null}, indicating the
  588 | 	 * use of {@link DefaultTransactionAttribute#getDescriptor()} instead,
  589 | 	 * ending up as {@link ClassUtils#getQualifiedMethodName(Method, Class)}.
  590 | 	 * @param method the method we're interested in
  591 | 	 * @param targetClass the class that the method is being invoked on
  592 | 	 * @return a String representation identifying this method
  593 | 	 * @see org.springframework.util.ClassUtils#getQualifiedMethodName
  594 | 	 */
  595 | 	protected @Nullable String methodIdentification(Method method, @Nullable Class<?> targetClass) {
  596 | 		return null;
  597 | 	}
  598 | 
  599 | 	/**
  600 | 	 * Create a transaction if necessary based on the given TransactionAttribute.
  601 | 	 * <p>Allows callers to perform custom TransactionAttribute lookups through
  602 | 	 * the TransactionAttributeSource.
  603 | 	 * @param txAttr the TransactionAttribute (may be {@code null})
  604 | 	 * @param joinpointIdentification the fully qualified method name
  605 | 	 * (used for monitoring and logging purposes)
  606 | 	 * @return a TransactionInfo object, whether a transaction was created.
  607 | 	 * The {@code hasTransaction()} method on TransactionInfo can be used to
  608 | 	 * tell if there was a transaction created.
  609 | 	 * @see #getTransactionAttributeSource()
  610 | 	 */
  611 | 	@SuppressWarnings("serial")
  612 | 	protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
  613 | 			@Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
  614 | 
  615 | 		// If no name specified, apply method identification as transaction name.
  616 | 		if (txAttr != null && txAttr.getName() == null) {
  617 | 			txAttr = new DelegatingTransactionAttribute(txAttr) {
  618 | 				@Override
  619 | 				public String getName() {
  620 | 					return joinpointIdentification;
  621 | 				}
  622 | 			};
  623 | 		}
  624 | 
  625 | 		TransactionStatus status = null;
  626 | 		if (txAttr != null) {
  627 | 			if (tm != null) {
  628 | 				status = tm.getTransaction(txAttr);
  629 | 			}
  630 | 			else {
  631 | 				if (logger.isDebugEnabled()) {
  632 | 					logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
  633 | 							"] because no transaction manager has been configured");
  634 | 				}
  635 | 			}
  636 | 		}
  637 | 		return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
  638 | 	}
  639 | 
  640 | 	/**
  641 | 	 * Prepare a TransactionInfo for the given attribute and status object.
  642 | 	 * @param txAttr the TransactionAttribute (may be {@code null})
  643 | 	 * @param joinpointIdentification the fully qualified method name
  644 | 	 * (used for monitoring and logging purposes)
  645 | 	 * @param status the TransactionStatus for the current transaction
  646 | 	 * @return the prepared TransactionInfo object
  647 | 	 */
  648 | 	protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm,
  649 | 			@Nullable TransactionAttribute txAttr, String joinpointIdentification,
  650 | 			@Nullable TransactionStatus status) {
  651 | 
  652 | 		TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
  653 | 		if (txAttr != null) {
  654 | 			// We need a transaction for this method...
  655 | 			if (logger.isTraceEnabled()) {
  656 | 				logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
  657 | 			}
  658 | 			// The transaction manager will flag an error if an incompatible tx already exists.
  659 | 			txInfo.newTransactionStatus(status);
  660 | 		}
  661 | 		else {
  662 | 			// The TransactionInfo.hasTransaction() method will return false. We created it only
  663 | 			// to preserve the integrity of the ThreadLocal stack maintained in this class.
  664 | 			if (logger.isTraceEnabled()) {
  665 | 				logger.trace("No need to create transaction for [" + joinpointIdentification +
  666 | 						"]: This method is not transactional.");
  667 | 			}
  668 | 		}
  669 | 
  670 | 		// We always bind the TransactionInfo to the thread, even if we didn't create
  671 | 		// a new transaction here. This guarantees that the TransactionInfo stack
  672 | 		// will be managed correctly even if no transaction was created by this aspect.
  673 | 		txInfo.bindToThread();
  674 | 		return txInfo;
  675 | 	}
  676 | 
  677 | 	/**
  678 | 	 * Execute after successful completion of call, but not after an exception was handled.
  679 | 	 * Do nothing if we didn't create a transaction.
  680 | 	 * @param txInfo information about the current transaction
  681 | 	 */
  682 | 	protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
  683 | 		if (txInfo != null && txInfo.getTransactionStatus() != null) {
  684 | 			if (logger.isTraceEnabled()) {
  685 | 				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
  686 | 			}
  687 | 			txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
  688 | 		}
  689 | 	}
  690 | 
  691 | 	/**
  692 | 	 * Handle a throwable, completing the transaction.
  693 | 	 * We may commit or roll back, depending on the configuration.
  694 | 	 * @param txInfo information about the current transaction
  695 | 	 * @param ex throwable encountered
  696 | 	 */
  697 | 	protected void completeTransactionAfterThrowing(
  698 | 			@Nullable TransactionInfo txInfo, InvocationCallback invocation, Throwable ex) {
  699 | 
  700 | 		if (txInfo != null && txInfo.getTransactionStatus() != null) {
  701 | 			if (logger.isTraceEnabled()) {
  702 | 				logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
  703 | 						"] after exception: " + ex);
  704 | 			}
  705 | 			if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
  706 | 				invocation.onRollback(ex, txInfo.getTransactionStatus());
  707 | 				try {
  708 | 					txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
  709 | 				}
  710 | 				catch (TransactionSystemException ex2) {
  711 | 					logger.error("Application exception overridden by rollback exception", ex);
  712 | 					ex2.initApplicationException(ex);
  713 | 					throw ex2;
  714 | 				}
  715 | 				catch (RuntimeException | Error ex2) {
  716 | 					logger.error("Application exception overridden by rollback exception", ex);
  717 | 					throw ex2;
  718 | 				}
  719 | 			}
  720 | 			else {
  721 | 				// We don't roll back on this exception.
  722 | 				// Will still roll back if TransactionStatus.isRollbackOnly() is true.
  723 | 				try {
  724 | 					txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
  725 | 				}
  726 | 				catch (TransactionSystemException ex2) {
  727 | 					logger.error("Application exception overridden by commit exception", ex);
  728 | 					ex2.initApplicationException(ex);
  729 | 					throw ex2;
  730 | 				}
  731 | 				catch (RuntimeException | Error ex2) {
  732 | 					logger.error("Application exception overridden by commit exception", ex);
  733 | 					throw ex2;
  734 | 				}
  735 | 			}
  736 | 		}
  737 | 	}
  738 | 
  739 | 	/**
  740 | 	 * Reset the TransactionInfo ThreadLocal.
  741 | 	 * <p>Call this in all cases: exception or normal return!
  742 | 	 * @param txInfo information about the current transaction (may be {@code null})
  743 | 	 */
  744 | 	protected void cleanupTransactionInfo(@Nullable TransactionInfo txInfo) {
  745 | 		if (txInfo != null) {
  746 | 			txInfo.restoreThreadLocalStatus();
  747 | 		}
  748 | 	}
  749 | 
  750 | 
  751 | 	/**
  752 | 	 * Opaque object used to hold transaction information. Subclasses
  753 | 	 * must pass it back to methods on this class, but not see its internals.
  754 | 	 */
  755 | 	protected static final class TransactionInfo {
  756 | 
  757 | 		private final @Nullable PlatformTransactionManager transactionManager;
  758 | 
  759 | 		private final @Nullable TransactionAttribute transactionAttribute;
  760 | 
  761 | 		private final String joinpointIdentification;
  762 | 
  763 | 		private @Nullable TransactionStatus transactionStatus;
  764 | 
  765 | 		private @Nullable TransactionInfo oldTransactionInfo;
  766 | 
  767 | 		public TransactionInfo(@Nullable PlatformTransactionManager transactionManager,
  768 | 				@Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {
  769 | 
  770 | 			this.transactionManager = transactionManager;
  771 | 			this.transactionAttribute = transactionAttribute;
  772 | 			this.joinpointIdentification = joinpointIdentification;
  773 | 		}
  774 | 
  775 | 		public PlatformTransactionManager getTransactionManager() {
  776 | 			Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
  777 | 			return this.transactionManager;
  778 | 		}
  779 | 
  780 | 		public @Nullable TransactionAttribute getTransactionAttribute() {
  781 | 			return this.transactionAttribute;
  782 | 		}
  783 | 
  784 | 		/**
  785 | 		 * Return a String representation of this joinpoint (usually a Method call)
  786 | 		 * for use in logging.
  787 | 		 */
  788 | 		public String getJoinpointIdentification() {
  789 | 			return this.joinpointIdentification;
  790 | 		}
  791 | 
  792 | 		public void newTransactionStatus(@Nullable TransactionStatus status) {
  793 | 			this.transactionStatus = status;
  794 | 		}
  795 | 
  796 | 		public @Nullable TransactionStatus getTransactionStatus() {
  797 | 			return this.transactionStatus;
  798 | 		}
  799 | 
  800 | 		/**
  801 | 		 * Return whether a transaction was created by this aspect,
  802 | 		 * or whether we just have a placeholder to keep ThreadLocal stack integrity.
  803 | 		 */
  804 | 		public boolean hasTransaction() {
  805 | 			return (this.transactionStatus != null);
  806 | 		}
  807 | 
  808 | 		private void bindToThread() {
  809 | 			// Expose current TransactionStatus, preserving any existing TransactionStatus
  810 | 			// for restoration after this transaction is complete.
  811 | 			this.oldTransactionInfo = transactionInfoHolder.get();
  812 | 			transactionInfoHolder.set(this);
  813 | 		}
  814 | 
  815 | 		private void restoreThreadLocalStatus() {
  816 | 			// Use stack to restore old transaction TransactionInfo.
  817 | 			// Will be null if none was set.
  818 | 			transactionInfoHolder.set(this.oldTransactionInfo);
  819 | 		}
  820 | 
  821 | 		@Override
  822 | 		public String toString() {
  823 | 			return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
  824 | 		}
  825 | 	}
  826 | 
  827 | 
  828 | 	/**
  829 | 	 * Simple callback interface for proceeding with the target invocation.
  830 | 	 * Concrete interceptors/aspects adapt this to their invocation mechanism.
  831 | 	 */
  832 | 	@FunctionalInterface
  833 | 	protected interface InvocationCallback {
  834 | 
  835 | 		/**
  836 | 		 * Invocation adaptation method.
  837 | 		 * @see org.aopalliance.intercept.MethodInvocation#proceed()
  838 | 		 */
  839 | 		@Nullable Object proceedWithInvocation() throws Throwable;
  840 | 
  841 | 		/**
  842 | 		 * Callback method for rollback-triggering exceptions.
  843 | 		 * @param failure the application exception that triggered the rollback
  844 | 		 * @param execution the current transaction status
  845 | 		 * @since 7.0.3
  846 | 		 * @see TransactionAttribute#rollbackOn(Throwable)
  847 | 		 */
  848 | 		default void onRollback(Throwable failure, TransactionExecution execution) {
  849 | 		}
  850 | 	}
  851 | 
  852 | 
  853 | 	/**
  854 | 	 * Internal holder class for a Throwable in a callback transaction model.
  855 | 	 */
  856 | 	private static class ThrowableHolder {
  857 | 
  858 | 		public @Nullable Throwable throwable;
  859 | 	}
  860 | 
  861 | 
  862 | 	/**
  863 | 	 * Internal holder class for a Throwable, used as a RuntimeException to be
  864 | 	 * thrown from a TransactionCallback (and subsequently unwrapped again).
  865 | 	 */
  866 | 	@SuppressWarnings("serial")
  867 | 	private static class ThrowableHolderException extends RuntimeException {
  868 | 
  869 | 		public ThrowableHolderException(Throwable throwable) {
  870 | 			super(throwable);
  871 | 		}
  872 | 
  873 | 		@Override
  874 | 		public String toString() {
  875 | 			Throwable cause = getCause();
  876 | 			Assert.state(cause != null, "Cause must not be null");
  877 | 			return cause.toString();
  878 | 		}
  879 | 	}
  880 | 
  881 | 
  882 | 	/**
  883 | 	 * Inner class to avoid a hard dependency on the Vavr library at runtime.
  884 | 	 */
  885 | 	private static class VavrDelegate {
  886 | 
  887 | 		public static boolean isVavrTry(Object retVal) {
  888 | 			return (retVal instanceof Try);
  889 | 		}
  890 | 
  891 | 		public static Object evaluateTryFailure(
  892 | 				InvocationCallback invocation, Object retVal, TransactionAttribute txAttr, TransactionStatus status) {
  893 | 
  894 | 			return ((Try<?>) retVal).onFailure(ex -> {
  895 | 				if (txAttr.rollbackOn(ex)) {
  896 | 					invocation.onRollback(ex, status);
  897 | 					status.setRollbackOnly();
  898 | 				}
  899 | 			});
  900 | 		}
  901 | 	}
  902 | 
  903 | 
  904 | 	/**
  905 | 	 * Delegate for Reactor-based management of transactional methods with a
  906 | 	 * reactive return type.
  907 | 	 */
  908 | 	private class ReactiveTransactionSupport {
  909 | 
  910 | 		private final ReactiveAdapter adapter;
  911 | 
  912 | 		public ReactiveTransactionSupport(ReactiveAdapter adapter) {
  913 | 			this.adapter = adapter;
  914 | 		}
  915 | 
  916 | 		public Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
  917 | 				InvocationCallback invocation, @Nullable TransactionAttribute txAttr, ReactiveTransactionManager rtm) {
  918 | 
  919 | 			String joinpointIdentification = methodIdentification(method, targetClass, txAttr);
  920 | 
  921 | 			// For Mono and suspending functions not returning kotlinx.coroutines.flow.Flow
  922 | 			if (Mono.class.isAssignableFrom(method.getReturnType()) || (KotlinDetector.isSuspendingFunction(method) &&
  923 | 					!COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName()))) {
  924 | 
  925 | 				return TransactionContextManager.currentContext().flatMap(context ->
  926 | 							Mono.<Object, ReactiveTransactionInfo>usingWhen(
  927 | 								createTransactionIfNecessary(rtm, txAttr, joinpointIdentification),
  928 | 								tx -> {
  929 | 									try {
  930 | 										return (Mono<?>) Objects.requireNonNull(invocation.proceedWithInvocation());
  931 | 									}
  932 | 									catch (Throwable ex) {
  933 | 										return Mono.error(ex);
  934 | 									}
  935 | 								},
  936 | 								this::commitTransactionAfterReturning,
  937 | 								(txInfo, ex) -> completeTransactionAfterThrowing(txInfo, invocation, ex),
  938 | 								this::rollbackTransactionOnCancel)
  939 | 							.onErrorMap(this::unwrapIfResourceCleanupFailure))
  940 | 						.contextWrite(TransactionContextManager.getOrCreateContext())
  941 | 						.contextWrite(TransactionContextManager.getOrCreateContextHolder());
  942 | 			}
  943 | 
  944 | 			// Any other reactive type, typically a Flux
  945 | 			return this.adapter.fromPublisher(TransactionContextManager.currentContext().flatMapMany(context ->
  946 | 						Flux.usingWhen(
  947 | 							createTransactionIfNecessary(rtm, txAttr, joinpointIdentification),
  948 | 							tx -> {
  949 | 								try {
  950 | 									return this.adapter.toPublisher(invocation.proceedWithInvocation());
  951 | 								}
  952 | 								catch (Throwable ex) {
  953 | 									return Mono.error(ex);
  954 | 								}
  955 | 							},
  956 | 							this::commitTransactionAfterReturning,
  957 | 							(txInfo, ex) -> completeTransactionAfterThrowing(txInfo, invocation, ex),
  958 | 							this::rollbackTransactionOnCancel)
  959 | 						.onErrorMap(this::unwrapIfResourceCleanupFailure))
  960 | 					.contextWrite(TransactionContextManager.getOrCreateContext())
  961 | 					.contextWrite(TransactionContextManager.getOrCreateContextHolder()));
  962 | 		}
  963 | 
  964 | 		@SuppressWarnings("serial")
  965 | 		private Mono<ReactiveTransactionInfo> createTransactionIfNecessary(ReactiveTransactionManager tm,
  966 | 				@Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
  967 | 
  968 | 			// If no name specified, apply method identification as transaction name.
  969 | 			if (txAttr != null && txAttr.getName() == null) {
  970 | 				txAttr = new DelegatingTransactionAttribute(txAttr) {
  971 | 					@Override
  972 | 					public String getName() {
  973 | 						return joinpointIdentification;
  974 | 					}
  975 | 				};
  976 | 			}
  977 | 
  978 | 			final TransactionAttribute attrToUse = txAttr;
  979 | 			Mono<ReactiveTransaction> tx = (attrToUse != null ? tm.getReactiveTransaction(attrToUse) : Mono.empty());
  980 | 			return tx.map(it -> prepareTransactionInfo(tm, attrToUse, joinpointIdentification, it)).switchIfEmpty(
  981 | 					Mono.defer(() -> Mono.just(prepareTransactionInfo(tm, attrToUse, joinpointIdentification, null))));
  982 | 		}
  983 | 
  984 | 		private ReactiveTransactionInfo prepareTransactionInfo(@Nullable ReactiveTransactionManager tm,
  985 | 				@Nullable TransactionAttribute txAttr, String joinpointIdentification,
  986 | 				@Nullable ReactiveTransaction transaction) {
  987 | 
  988 | 			ReactiveTransactionInfo txInfo = new ReactiveTransactionInfo(tm, txAttr, joinpointIdentification);
  989 | 			if (txAttr != null) {
  990 | 				// We need a transaction for this method...
  991 | 				if (logger.isTraceEnabled()) {
  992 | 					logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
  993 | 				}
  994 | 				// The transaction manager will flag an error if an incompatible tx already exists.
  995 | 				txInfo.newReactiveTransaction(transaction);
  996 | 			}
  997 | 			else {
  998 | 				// The TransactionInfo.hasTransaction() method will return false. We created it only
  999 | 				// to preserve the integrity of the ThreadLocal stack maintained in this class.
 1000 | 				if (logger.isTraceEnabled()) {
 1001 | 					logger.trace("Don't need to create transaction for [" + joinpointIdentification +
 1002 | 							"]: This method isn't transactional.");
 1003 | 				}
 1004 | 			}
 1005 | 
 1006 | 			return txInfo;
 1007 | 		}
 1008 | 
 1009 | 		private Mono<Void> commitTransactionAfterReturning(@Nullable ReactiveTransactionInfo txInfo) {
 1010 | 			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
 1011 | 				if (logger.isTraceEnabled()) {
 1012 | 					logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
 1013 | 				}
 1014 | 				return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction());
 1015 | 			}
 1016 | 			return Mono.empty();
 1017 | 		}
 1018 | 
 1019 | 		private Mono<Void> rollbackTransactionOnCancel(@Nullable ReactiveTransactionInfo txInfo) {
 1020 | 			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
 1021 | 				if (logger.isTraceEnabled()) {
 1022 | 					logger.trace("Rolling back transaction for [" + txInfo.getJoinpointIdentification() + "] after cancellation");
 1023 | 				}
 1024 | 				return txInfo.getTransactionManager().rollback(txInfo.getReactiveTransaction());
 1025 | 			}
 1026 | 			return Mono.empty();
 1027 | 		}
 1028 | 
 1029 | 		private Mono<Void> completeTransactionAfterThrowing(
 1030 | 				@Nullable ReactiveTransactionInfo txInfo, InvocationCallback invocation, Throwable ex) {
 1031 | 
 1032 | 			if (txInfo != null && txInfo.getReactiveTransaction() != null) {
 1033 | 				if (logger.isTraceEnabled()) {
 1034 | 					logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
 1035 | 							"] after exception: " + ex);
 1036 | 				}
 1037 | 				if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
 1038 | 					invocation.onRollback(ex, txInfo.getReactiveTransaction());
 1039 | 					return txInfo.getTransactionManager().rollback(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
 1040 | 								logger.error("Application exception overridden by rollback exception", ex);
 1041 | 								if (ex2 instanceof TransactionSystemException systemException) {
 1042 | 									systemException.initApplicationException(ex);
 1043 | 								}
 1044 | 								else {
 1045 | 									ex2.addSuppressed(ex);
 1046 | 								}
 1047 | 								return ex2;
 1048 | 							}
 1049 | 					);
 1050 | 				}
 1051 | 				else {
 1052 | 					// We don't roll back on this exception.
 1053 | 					// Will still roll back if TransactionStatus.isRollbackOnly() is true.
 1054 | 					return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
 1055 | 								logger.error("Application exception overridden by commit exception", ex);
 1056 | 								if (ex2 instanceof TransactionSystemException systemException) {
 1057 | 									systemException.initApplicationException(ex);
 1058 | 								}
 1059 | 								else {
 1060 | 									ex2.addSuppressed(ex);
 1061 | 								}
 1062 | 								return ex2;
 1063 | 							}
 1064 | 					);
 1065 | 				}
 1066 | 			}
 1067 | 			return Mono.empty();
 1068 | 		}
 1069 | 
 1070 | 		/**
 1071 | 		 * Unwrap the cause of a throwable, if produced by a failure
 1072 | 		 * during the async resource cleanup in {@link Flux#usingWhen}.
 1073 | 		 * @param ex the throwable to try to unwrap
 1074 | 		 */
 1075 | 		private Throwable unwrapIfResourceCleanupFailure(Throwable ex) {
 1076 | 			if (ex instanceof RuntimeException && ex.getCause() != null) {
 1077 | 				String msg = ex.getMessage();
 1078 | 				if (msg != null && msg.startsWith("Async resource cleanup failed")) {
 1079 | 					return ex.getCause();
 1080 | 				}
 1081 | 			}
 1082 | 			return ex;
 1083 | 		}
 1084 | 	}
 1085 | 
 1086 | 
 1087 | 	/**
 1088 | 	 * Opaque object used to hold transaction information for reactive methods.
 1089 | 	 */
 1090 | 	private static final class ReactiveTransactionInfo {
 1091 | 
 1092 | 		private final @Nullable ReactiveTransactionManager transactionManager;
 1093 | 
 1094 | 		private final @Nullable TransactionAttribute transactionAttribute;
 1095 | 
 1096 | 		private final String joinpointIdentification;
 1097 | 
 1098 | 		private @Nullable ReactiveTransaction reactiveTransaction;
 1099 | 
 1100 | 		public ReactiveTransactionInfo(@Nullable ReactiveTransactionManager transactionManager,
 1101 | 				@Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {
 1102 | 
 1103 | 			this.transactionManager = transactionManager;
 1104 | 			this.transactionAttribute = transactionAttribute;
 1105 | 			this.joinpointIdentification = joinpointIdentification;
 1106 | 		}
 1107 | 
 1108 | 		public ReactiveTransactionManager getTransactionManager() {
 1109 | 			Assert.state(this.transactionManager != null, "No ReactiveTransactionManager set");
 1110 | 			return this.transactionManager;
 1111 | 		}
 1112 | 
 1113 | 		public @Nullable TransactionAttribute getTransactionAttribute() {
 1114 | 			return this.transactionAttribute;
 1115 | 		}
 1116 | 
 1117 | 		/**
 1118 | 		 * Return a String representation of this joinpoint (usually a Method call)
 1119 | 		 * for use in logging.
 1120 | 		 */
 1121 | 		public String getJoinpointIdentification() {
 1122 | 			return this.joinpointIdentification;
 1123 | 		}
 1124 | 
 1125 | 		public void newReactiveTransaction(@Nullable ReactiveTransaction transaction) {
 1126 | 			this.reactiveTransaction = transaction;
 1127 | 		}
 1128 | 
 1129 | 		public @Nullable ReactiveTransaction getReactiveTransaction() {
 1130 | 			return this.reactiveTransaction;
 1131 | 		}
 1132 | 
 1133 | 		@Override
 1134 | 		public String toString() {
 1135 | 			return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
 1136 | 		}
 1137 | 	}
 1138 | 
 1139 | }
```
