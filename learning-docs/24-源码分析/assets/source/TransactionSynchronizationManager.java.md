<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/TransactionSynchronizationManager.java -->
<!-- 正文引用：[[源码:TransactionSynchronizationManager.java#L起-L止]] ；代码块头部：(src: TransactionSynchronizationManager.java#L起-L止) -->

# TransactionSynchronizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/TransactionSynchronizationManager.java` |
| 行数 | 521 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:14.728Z |

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
   17 | package org.springframework.transaction.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.HashMap;
   22 | import java.util.LinkedHashSet;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.core.NamedThreadLocal;
   30 | import org.springframework.core.OrderComparator;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Central delegate that manages resources and transaction synchronizations per thread.
   35 |  * To be used by resource management code but not by typical application code.
   36 |  *
   37 |  * <p>Supports one resource per key without overwriting, that is, a resource needs
   38 |  * to be removed before a new one can be set for the same key.
   39 |  * Supports a list of transaction synchronizations if synchronization is active.
   40 |  *
   41 |  * <p>Resource management code should check for thread-bound resources, for example, JDBC
   42 |  * Connections or Hibernate Sessions, via {@code getResource}. Such code is
   43 |  * normally not supposed to bind resources to threads, as this is the responsibility
   44 |  * of transaction managers. A further option is to lazily bind on first use if
   45 |  * transaction synchronization is active, for performing transactions that span
   46 |  * an arbitrary number of resources.
   47 |  *
   48 |  * <p>Transaction synchronization must be activated and deactivated by a transaction
   49 |  * manager via {@link #initSynchronization()} and {@link #clearSynchronization()}.
   50 |  * This is automatically supported by {@link AbstractPlatformTransactionManager},
   51 |  * and thus by all standard Spring transaction managers, such as
   52 |  * {@link org.springframework.transaction.jta.JtaTransactionManager} and
   53 |  * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
   54 |  *
   55 |  * <p>Resource management code should only register synchronizations when this
   56 |  * manager is active, which can be checked via {@link #isSynchronizationActive};
   57 |  * it should perform immediate resource cleanup else. If transaction synchronization
   58 |  * isn't active, there is either no current transaction, or the transaction manager
   59 |  * doesn't support transaction synchronization.
   60 |  *
   61 |  * <p>Synchronization is for example used to always return the same resources
   62 |  * within a JTA transaction, for example, a JDBC Connection or a Hibernate Session for
   63 |  * any given DataSource or SessionFactory, respectively.
   64 |  *
   65 |  * @author Juergen Hoeller
   66 |  * @since 02.06.2003
   67 |  * @see #isSynchronizationActive
   68 |  * @see #registerSynchronization
   69 |  * @see TransactionSynchronization
   70 |  * @see AbstractPlatformTransactionManager#setTransactionSynchronization
   71 |  * @see org.springframework.transaction.jta.JtaTransactionManager
   72 |  * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
   73 |  * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
   74 |  */
   75 | public abstract class TransactionSynchronizationManager {
   76 | 
   77 | 	private static final ThreadLocal<Map<Object, Object>> resources =
   78 | 			new NamedThreadLocal<>("Transactional resources");
   79 | 
   80 | 	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
   81 | 			new NamedThreadLocal<>("Transaction synchronizations");
   82 | 
   83 | 	private static final ThreadLocal<String> currentTransactionName =
   84 | 			new NamedThreadLocal<>("Current transaction name");
   85 | 
   86 | 	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
   87 | 			new NamedThreadLocal<>("Current transaction read-only status");
   88 | 
   89 | 	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
   90 | 			new NamedThreadLocal<>("Current transaction isolation level");
   91 | 
   92 | 	private static final ThreadLocal<Boolean> actualTransactionActive =
   93 | 			new NamedThreadLocal<>("Actual transaction active");
   94 | 
   95 | 
   96 | 	//-------------------------------------------------------------------------
   97 | 	// Management of transaction-associated resource handles
   98 | 	//-------------------------------------------------------------------------
   99 | 
  100 | 	/**
  101 | 	 * Return all resources that are bound to the current thread.
  102 | 	 * <p>Mainly for debugging purposes. Resource managers should always invoke
  103 | 	 * {@code hasResource} for a specific resource key that they are interested in.
  104 | 	 * @return a Map with resource keys (usually the resource factory) and resource
  105 | 	 * values (usually the active resource object), or an empty Map if there are
  106 | 	 * currently no resources bound
  107 | 	 * @see #hasResource
  108 | 	 */
  109 | 	public static Map<Object, Object> getResourceMap() {
  110 | 		Map<Object, Object> map = resources.get();
  111 | 		return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Check if there is a resource for the given key bound to the current thread.
  116 | 	 * @param key the key to check (usually the resource factory)
  117 | 	 * @return if there is a value bound to the current thread
  118 | 	 * @see ResourceTransactionManager#getResourceFactory()
  119 | 	 */
  120 | 	public static boolean hasResource(Object key) {
  121 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  122 | 		Object value = doGetResource(actualKey);
  123 | 		return (value != null);
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * Retrieve a resource for the given key that is bound to the current thread.
  128 | 	 * @param key the key to check (usually the resource factory)
  129 | 	 * @return a value bound to the current thread (usually the active
  130 | 	 * resource object), or {@code null} if none
  131 | 	 * @see ResourceTransactionManager#getResourceFactory()
  132 | 	 */
  133 | 	public static @Nullable Object getResource(Object key) {
  134 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  135 | 		return doGetResource(actualKey);
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Actually check the value of the resource that is bound for the given key.
  140 | 	 */
  141 | 	private static @Nullable Object doGetResource(Object actualKey) {
  142 | 		Map<Object, Object> map = resources.get();
  143 | 		if (map == null) {
  144 | 			return null;
  145 | 		}
  146 | 		Object value = map.get(actualKey);
  147 | 		// Transparently remove ResourceHolder that was marked as void...
  148 | 		if (value instanceof ResourceHolder resourceHolder && resourceHolder.isVoid()) {
  149 | 			map.remove(actualKey);
  150 | 			// Remove entire ThreadLocal if empty...
  151 | 			if (map.isEmpty()) {
  152 | 				resources.remove();
  153 | 			}
  154 | 			value = null;
  155 | 		}
  156 | 		return value;
  157 | 	}
  158 | 
  159 | 	/**
  160 | 	 * Bind the given resource for the given key to the current thread.
  161 | 	 * <p><b>Note: Any bound resource needs to get explicitly unbound through
  162 | 	 * {@link #unbindResource}. For automatic unbinding after transaction
  163 | 	 * completion, use {@link #bindSynchronizedResource} instead.</b>
  164 | 	 * @param key the key to bind the value to (usually the resource factory)
  165 | 	 * @param value the value to bind (usually the active resource object)
  166 | 	 * @throws IllegalStateException if there is already a value bound to the thread
  167 | 	 * @see ResourceTransactionManager#getResourceFactory()
  168 | 	 * @see #bindSynchronizedResource
  169 | 	 */
  170 | 	public static void bindResource(Object key, Object value) throws IllegalStateException {
  171 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  172 | 		Object oldValue = doBindResource(actualKey, value);
  173 | 		if (oldValue != null) {
  174 | 			throw new IllegalStateException(
  175 | 					"Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
  176 | 		}
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * Bind the given resource for the given key to the current thread,
  181 | 	 * synchronizing it with the current transaction for automatic unbinding
  182 | 	 * after transaction completion.
  183 | 	 * <p>This is effectively a programmatic way to register a transaction-scoped
  184 | 	 * resource, similar to the BeanFactory-driven {@link SimpleTransactionScope}.
  185 | 	 * <p>An existing value bound for the given key will be preserved and re-bound
  186 | 	 * after transaction completion, restoring the state before this bind call.
  187 | 	 * @param key the key to bind the value to (usually the resource factory)
  188 | 	 * @param value the value to bind (usually the active resource object)
  189 | 	 * @throws IllegalStateException if transaction synchronization is not active
  190 | 	 * @since 7.0
  191 | 	 * @see #bindResource
  192 | 	 * @see #registerSynchronization
  193 | 	 */
  194 | 	public static void bindSynchronizedResource(Object key, Object value) throws IllegalStateException {
  195 | 		Set<TransactionSynchronization> synchs = synchronizations.get();
  196 | 		if (synchs == null) {
  197 | 			throw new IllegalStateException("Transaction synchronization is not active");
  198 | 		}
  199 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  200 | 		Object oldValue = doBindResource(actualKey, value);
  201 | 		synchs.add(new TransactionSynchronization() {
  202 | 			@Override
  203 | 			public void suspend() {
  204 | 				doUnbindResource(actualKey);
  205 | 			}
  206 | 			@Override
  207 | 			public void resume() {
  208 | 				Object existingValue = doBindResource(actualKey, value);
  209 | 				if (existingValue != null) {
  210 | 					throw new IllegalStateException(
  211 | 							"Unexpected value [" + existingValue + "] for key [" + actualKey + "] bound on resume");
  212 | 				}
  213 | 			}
  214 | 			@Override
  215 | 			public void afterCompletion(int status) {
  216 | 				doUnbindResource(actualKey);
  217 | 				if (oldValue != null) {
  218 | 					doBindResource(actualKey, oldValue);
  219 | 				}
  220 | 			}
  221 | 		});
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Actually bind the given resource for the given key to the current thread.
  226 | 	 */
  227 | 	private static @Nullable Object doBindResource(Object actualKey, Object value) {
  228 | 		Assert.notNull(value, "Value must not be null");
  229 | 		Map<Object, Object> map = resources.get();
  230 | 		// set ThreadLocal Map if none found
  231 | 		if (map == null) {
  232 | 			map = new HashMap<>();
  233 | 			resources.set(map);
  234 | 		}
  235 | 		Object oldValue = map.put(actualKey, value);
  236 | 		// Transparently suppress a ResourceHolder that was marked as void...
  237 | 		if (oldValue instanceof ResourceHolder resourceHolder && resourceHolder.isVoid()) {
  238 | 			oldValue = null;
  239 | 		}
  240 | 		return oldValue;
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Unbind a resource for the given key from the current thread.
  245 | 	 * <p>This explicit step is only necessary with {@link #bindResource}.
  246 | 	 * For automatic unbinding, consider {@link #bindSynchronizedResource}.
  247 | 	 * @param key the key to unbind (usually the resource factory)
  248 | 	 * @return the previously bound value (usually the active resource object)
  249 | 	 * @throws IllegalStateException if there is no value bound to the thread
  250 | 	 * @see ResourceTransactionManager#getResourceFactory()
  251 | 	 * @see #bindResource
  252 | 	 * @see #unbindResourceIfPossible
  253 | 	 */
  254 | 	public static Object unbindResource(Object key) throws IllegalStateException {
  255 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  256 | 		Object value = doUnbindResource(actualKey);
  257 | 		if (value == null) {
  258 | 			throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
  259 | 		}
  260 | 		return value;
  261 | 	}
  262 | 
  263 | 	/**
  264 | 	 * Unbind a resource for the given key from the current thread.
  265 | 	 * <p>This explicit step is only necessary with {@link #bindResource}.
  266 | 	 * For automatic unbinding, consider {@link #bindSynchronizedResource}.
  267 | 	 * @param key the key to unbind (usually the resource factory)
  268 | 	 * @return the previously bound value, or {@code null} if none bound
  269 | 	 * @see #bindResource
  270 | 	 * @see #unbindResource
  271 | 	 */
  272 | 	public static @Nullable Object unbindResourceIfPossible(Object key) {
  273 | 		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
  274 | 		return doUnbindResource(actualKey);
  275 | 	}
  276 | 
  277 | 	/**
  278 | 	 * Actually remove the value of the resource that is bound for the given key.
  279 | 	 */
  280 | 	private static @Nullable Object doUnbindResource(Object actualKey) {
  281 | 		Map<Object, Object> map = resources.get();
  282 | 		if (map == null) {
  283 | 			return null;
  284 | 		}
  285 | 		Object value = map.remove(actualKey);
  286 | 		// Remove entire ThreadLocal if empty...
  287 | 		if (map.isEmpty()) {
  288 | 			resources.remove();
  289 | 		}
  290 | 		// Transparently suppress a ResourceHolder that was marked as void...
  291 | 		if (value instanceof ResourceHolder resourceHolder && resourceHolder.isVoid()) {
  292 | 			value = null;
  293 | 		}
  294 | 		return value;
  295 | 	}
  296 | 
  297 | 
  298 | 	//-------------------------------------------------------------------------
  299 | 	// Management of transaction synchronizations
  300 | 	//-------------------------------------------------------------------------
  301 | 
  302 | 	/**
  303 | 	 * Return if transaction synchronization is active for the current thread.
  304 | 	 * Can be called before register to avoid unnecessary instance creation.
  305 | 	 * @see #registerSynchronization
  306 | 	 */
  307 | 	public static boolean isSynchronizationActive() {
  308 | 		return (synchronizations.get() != null);
  309 | 	}
  310 | 
  311 | 	/**
  312 | 	 * Activate transaction synchronization for the current thread.
  313 | 	 * Called by a transaction manager on transaction begin.
  314 | 	 * @throws IllegalStateException if synchronization is already active
  315 | 	 */
  316 | 	public static void initSynchronization() throws IllegalStateException {
  317 | 		if (isSynchronizationActive()) {
  318 | 			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
  319 | 		}
  320 | 		synchronizations.set(new LinkedHashSet<>());
  321 | 	}
  322 | 
  323 | 	/**
  324 | 	 * Register a new transaction synchronization for the current thread.
  325 | 	 * Typically called by resource management code.
  326 | 	 * <p>Note that synchronizations can implement the
  327 | 	 * {@link org.springframework.core.Ordered} interface.
  328 | 	 * They will be executed in an order according to their order value (if any).
  329 | 	 * @param synchronization the synchronization object to register
  330 | 	 * @throws IllegalStateException if transaction synchronization is not active
  331 | 	 * @see org.springframework.core.Ordered
  332 | 	 */
  333 | 	public static void registerSynchronization(TransactionSynchronization synchronization)
  334 | 			throws IllegalStateException {
  335 | 
  336 | 		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
  337 | 		Set<TransactionSynchronization> synchs = synchronizations.get();
  338 | 		if (synchs == null) {
  339 | 			throw new IllegalStateException("Transaction synchronization is not active");
  340 | 		}
  341 | 		synchs.add(synchronization);
  342 | 	}
  343 | 
  344 | 	/**
  345 | 	 * Return an unmodifiable snapshot list of all registered synchronizations
  346 | 	 * for the current thread.
  347 | 	 * @return unmodifiable List of TransactionSynchronization instances
  348 | 	 * @throws IllegalStateException if synchronization is not active
  349 | 	 * @see TransactionSynchronization
  350 | 	 */
  351 | 	public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
  352 | 		Set<TransactionSynchronization> synchs = synchronizations.get();
  353 | 		if (synchs == null) {
  354 | 			throw new IllegalStateException("Transaction synchronization is not active");
  355 | 		}
  356 | 		// Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
  357 | 		// while iterating and invoking synchronization callbacks that in turn
  358 | 		// might register further synchronizations.
  359 | 		if (synchs.isEmpty()) {
  360 | 			return Collections.emptyList();
  361 | 		}
  362 | 		else if (synchs.size() == 1) {
  363 | 			return Collections.singletonList(synchs.iterator().next());
  364 | 		}
  365 | 		else {
  366 | 			// Sort lazily here, not in registerSynchronization.
  367 | 			List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
  368 | 			OrderComparator.sort(sortedSynchs);
  369 | 			return Collections.unmodifiableList(sortedSynchs);
  370 | 		}
  371 | 	}
  372 | 
  373 | 	/**
  374 | 	 * Deactivate transaction synchronization for the current thread.
  375 | 	 * Called by the transaction manager on transaction cleanup.
  376 | 	 * @throws IllegalStateException if synchronization is not active
  377 | 	 */
  378 | 	public static void clearSynchronization() throws IllegalStateException {
  379 | 		if (!isSynchronizationActive()) {
  380 | 			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
  381 | 		}
  382 | 		synchronizations.remove();
  383 | 	}
  384 | 
  385 | 
  386 | 	//-------------------------------------------------------------------------
  387 | 	// Exposure of transaction characteristics
  388 | 	//-------------------------------------------------------------------------
  389 | 
  390 | 	/**
  391 | 	 * Expose the name of the current transaction, if any.
  392 | 	 * Called by the transaction manager on transaction begin and on cleanup.
  393 | 	 * @param name the name of the transaction, or {@code null} to reset it
  394 | 	 * @see org.springframework.transaction.TransactionDefinition#getName()
  395 | 	 */
  396 | 	public static void setCurrentTransactionName(@Nullable String name) {
  397 | 		currentTransactionName.set(name);
  398 | 	}
  399 | 
  400 | 	/**
  401 | 	 * Return the name of the current transaction, or {@code null} if none set.
  402 | 	 * To be called by resource management code for optimizations per use case,
  403 | 	 * for example to optimize fetch strategies for specific named transactions.
  404 | 	 * @see org.springframework.transaction.TransactionDefinition#getName()
  405 | 	 */
  406 | 	public static @Nullable String getCurrentTransactionName() {
  407 | 		return currentTransactionName.get();
  408 | 	}
  409 | 
  410 | 	/**
  411 | 	 * Expose a read-only flag for the current transaction.
  412 | 	 * Called by the transaction manager on transaction begin and on cleanup.
  413 | 	 * @param readOnly {@code true} to mark the current transaction
  414 | 	 * as read-only; {@code false} to reset such a read-only marker
  415 | 	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
  416 | 	 */
  417 | 	public static void setCurrentTransactionReadOnly(boolean readOnly) {
  418 | 		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
  419 | 	}
  420 | 
  421 | 	/**
  422 | 	 * Return whether the current transaction is marked as read-only.
  423 | 	 * To be called by resource management code when preparing a newly
  424 | 	 * created resource (for example, a Hibernate Session).
  425 | 	 * <p>Note that transaction synchronizations receive the read-only flag
  426 | 	 * as argument for the {@code beforeCommit} callback, to be able
  427 | 	 * to suppress change detection on commit. The present method is meant
  428 | 	 * to be used for earlier read-only checks, for example to set the
  429 | 	 * flush mode of a Hibernate Session to "FlushMode.MANUAL" upfront.
  430 | 	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
  431 | 	 * @see TransactionSynchronization#beforeCommit(boolean)
  432 | 	 */
  433 | 	public static boolean isCurrentTransactionReadOnly() {
  434 | 		return (currentTransactionReadOnly.get() != null);
  435 | 	}
  436 | 
  437 | 	/**
  438 | 	 * Expose an isolation level for the current transaction.
  439 | 	 * Called by the transaction manager on transaction begin and on cleanup.
  440 | 	 * @param isolationLevel the isolation level to expose, according to the
  441 | 	 * JDBC Connection constants (equivalent to the corresponding Spring
  442 | 	 * TransactionDefinition constants), or {@code null} to reset it
  443 | 	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
  444 | 	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
  445 | 	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
  446 | 	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
  447 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
  448 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
  449 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
  450 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
  451 | 	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
  452 | 	 */
  453 | 	public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
  454 | 		currentTransactionIsolationLevel.set(isolationLevel);
  455 | 	}
  456 | 
  457 | 	/**
  458 | 	 * Return the isolation level for the current transaction, if any.
  459 | 	 * To be called by resource management code when preparing a newly
  460 | 	 * created resource (for example, a JDBC Connection).
  461 | 	 * @return the currently exposed isolation level, according to the
  462 | 	 * JDBC Connection constants (equivalent to the corresponding Spring
  463 | 	 * TransactionDefinition constants), or {@code null} if none
  464 | 	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
  465 | 	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
  466 | 	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
  467 | 	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
  468 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
  469 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
  470 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
  471 | 	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
  472 | 	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
  473 | 	 */
  474 | 	public static @Nullable Integer getCurrentTransactionIsolationLevel() {
  475 | 		return currentTransactionIsolationLevel.get();
  476 | 	}
  477 | 
  478 | 	/**
  479 | 	 * Expose whether there currently is an actual transaction active.
  480 | 	 * Called by the transaction manager on transaction begin and on cleanup.
  481 | 	 * @param active {@code true} to mark the current thread as being associated
  482 | 	 * with an actual transaction; {@code false} to reset that marker
  483 | 	 */
  484 | 	public static void setActualTransactionActive(boolean active) {
  485 | 		actualTransactionActive.set(active ? Boolean.TRUE : null);
  486 | 	}
  487 | 
  488 | 	/**
  489 | 	 * Return whether there currently is an actual transaction active.
  490 | 	 * This indicates whether the current thread is associated with an actual
  491 | 	 * transaction rather than just with active transaction synchronization.
  492 | 	 * <p>To be called by resource management code that wants to differentiate
  493 | 	 * between active transaction synchronization (with or without a backing
  494 | 	 * resource transaction; also on PROPAGATION_SUPPORTS) and an actual
  495 | 	 * transaction being active (with a backing resource transaction;
  496 | 	 * on PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW, etc).
  497 | 	 * @see #isSynchronizationActive()
  498 | 	 */
  499 | 	public static boolean isActualTransactionActive() {
  500 | 		return (actualTransactionActive.get() != null);
  501 | 	}
  502 | 
  503 | 
  504 | 	/**
  505 | 	 * Clear the entire transaction synchronization state for the current thread:
  506 | 	 * registered synchronizations as well as the various transaction characteristics.
  507 | 	 * @see #clearSynchronization()
  508 | 	 * @see #setCurrentTransactionName
  509 | 	 * @see #setCurrentTransactionReadOnly
  510 | 	 * @see #setCurrentTransactionIsolationLevel
  511 | 	 * @see #setActualTransactionActive
  512 | 	 */
  513 | 	public static void clear() {
  514 | 		synchronizations.remove();
  515 | 		currentTransactionName.remove();
  516 | 		currentTransactionReadOnly.remove();
  517 | 		currentTransactionIsolationLevel.remove();
  518 | 		actualTransactionActive.remove();
  519 | 	}
  520 | 
  521 | }
```
