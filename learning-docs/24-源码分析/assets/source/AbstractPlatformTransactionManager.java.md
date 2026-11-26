<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/AbstractPlatformTransactionManager.java -->
<!-- 正文引用：[[源码:AbstractPlatformTransactionManager.java#L起-L止]] ；代码块头部：(src: AbstractPlatformTransactionManager.java#L起-L止) -->

# AbstractPlatformTransactionManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/AbstractPlatformTransactionManager.java` |
| 行数 | 1363 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:14.466Z |

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
   19 | import java.io.IOException;
   20 | import java.io.ObjectInputStream;
   21 | import java.io.Serializable;
   22 | import java.util.ArrayList;
   23 | import java.util.Collection;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | 
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.transaction.ConfigurableTransactionManager;
   32 | import org.springframework.transaction.IllegalTransactionStateException;
   33 | import org.springframework.transaction.InvalidTimeoutException;
   34 | import org.springframework.transaction.NestedTransactionNotSupportedException;
   35 | import org.springframework.transaction.PlatformTransactionManager;
   36 | import org.springframework.transaction.TransactionDefinition;
   37 | import org.springframework.transaction.TransactionException;
   38 | import org.springframework.transaction.TransactionExecutionListener;
   39 | import org.springframework.transaction.TransactionStatus;
   40 | import org.springframework.transaction.TransactionSuspensionNotSupportedException;
   41 | import org.springframework.transaction.UnexpectedRollbackException;
   42 | import org.springframework.util.Assert;
   43 | 
   44 | /**
   45 |  * Abstract base class that implements Spring's standard transaction workflow,
   46 |  * serving as basis for concrete platform transaction managers like
   47 |  * {@link org.springframework.transaction.jta.JtaTransactionManager}.
   48 |  *
   49 |  * <p>This base class provides the following workflow handling:
   50 |  * <ul>
   51 |  * <li>determines if there is an existing transaction;
   52 |  * <li>applies the appropriate propagation behavior;
   53 |  * <li>suspends and resumes transactions if necessary;
   54 |  * <li>checks the rollback-only flag on commit;
   55 |  * <li>applies the appropriate modification on rollback
   56 |  * (actual rollback or setting rollback-only);
   57 |  * <li>triggers registered synchronization callbacks
   58 |  * (if transaction synchronization is active).
   59 |  * </ul>
   60 |  *
   61 |  * <p>Subclasses have to implement specific template methods for specific
   62 |  * states of a transaction, for example: begin, suspend, resume, commit, rollback.
   63 |  * The most important of them are abstract and must be provided by a concrete
   64 |  * implementation; for the rest, defaults are provided, so overriding is optional.
   65 |  *
   66 |  * <p>Transaction synchronization is a generic mechanism for registering callbacks
   67 |  * that get invoked at transaction completion time. This is mainly used internally
   68 |  * by the data access support classes for JDBC, Hibernate, JPA, etc when running
   69 |  * within a JTA transaction: They register resources that are opened within the
   70 |  * transaction for closing at transaction completion time, allowing, for example, for reuse
   71 |  * of the same Hibernate Session within the transaction. The same mechanism can
   72 |  * also be leveraged for custom synchronization needs in an application.
   73 |  *
   74 |  * <p>The state of this class is serializable, to allow for serializing the
   75 |  * transaction strategy along with proxies that carry a transaction interceptor.
   76 |  * It is up to subclasses if they wish to make their state to be serializable too.
   77 |  * They should implement the {@code java.io.Serializable} marker interface in
   78 |  * that case, and potentially a private {@code readObject()} method (according
   79 |  * to Java serialization rules) if they need to restore any transient state.
   80 |  *
   81 |  * @author Juergen Hoeller
   82 |  * @author Sam Brannen
   83 |  * @since 28.03.2003
   84 |  * @see #setTransactionSynchronization
   85 |  * @see TransactionSynchronizationManager
   86 |  * @see org.springframework.transaction.jta.JtaTransactionManager
   87 |  */
   88 | @SuppressWarnings("serial")
   89 | public abstract class AbstractPlatformTransactionManager
   90 | 		implements PlatformTransactionManager, ConfigurableTransactionManager, Serializable {
   91 | 
   92 | 	/**
   93 | 	 * Always activate transaction synchronization, even for "empty" transactions
   94 | 	 * that result from PROPAGATION_SUPPORTS with no existing backend transaction.
   95 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
   96 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NOT_SUPPORTED
   97 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NEVER
   98 | 	 */
   99 | 	public static final int SYNCHRONIZATION_ALWAYS = 0;
  100 | 
  101 | 	/**
  102 | 	 * Activate transaction synchronization only for actual transactions,
  103 | 	 * that is, not for empty ones that result from PROPAGATION_SUPPORTS with
  104 | 	 * no existing backend transaction.
  105 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED
  106 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_MANDATORY
  107 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRES_NEW
  108 | 	 */
  109 | 	public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;
  110 | 
  111 | 	/**
  112 | 	 * Never active transaction synchronization, not even for actual transactions.
  113 | 	 */
  114 | 	public static final int SYNCHRONIZATION_NEVER = 2;
  115 | 
  116 | 
  117 | 	/**
  118 | 	 * Map of constant names to constant values for the transaction synchronization
  119 | 	 * constants defined in this class.
  120 | 	 */
  121 | 	static final Map<String, Integer> constants = Map.of(
  122 | 			"SYNCHRONIZATION_ALWAYS", SYNCHRONIZATION_ALWAYS,
  123 | 			"SYNCHRONIZATION_ON_ACTUAL_TRANSACTION", SYNCHRONIZATION_ON_ACTUAL_TRANSACTION,
  124 | 			"SYNCHRONIZATION_NEVER", SYNCHRONIZATION_NEVER
  125 | 		);
  126 | 
  127 | 
  128 | 	protected transient Log logger = LogFactory.getLog(getClass());
  129 | 
  130 | 	private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;
  131 | 
  132 | 	private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;
  133 | 
  134 | 	private boolean nestedTransactionAllowed = false;
  135 | 
  136 | 	private boolean validateExistingTransaction = false;
  137 | 
  138 | 	private boolean globalRollbackOnParticipationFailure = true;
  139 | 
  140 | 	private boolean failEarlyOnGlobalRollbackOnly = false;
  141 | 
  142 | 	private boolean rollbackOnCommitFailure = false;
  143 | 
  144 | 	private Collection<TransactionExecutionListener> transactionExecutionListeners = new ArrayList<>();
  145 | 
  146 | 
  147 | 	/**
  148 | 	 * Set the transaction synchronization by the name of the corresponding constant
  149 | 	 * in this class &mdash; for example, {@code "SYNCHRONIZATION_ALWAYS"}.
  150 | 	 * @param constantName name of the constant
  151 | 	 * @see #SYNCHRONIZATION_ALWAYS
  152 | 	 * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
  153 | 	 * @see #SYNCHRONIZATION_NEVER
  154 | 	 * @see #setTransactionSynchronization(int)
  155 | 	 */
  156 | 	public final void setTransactionSynchronizationName(String constantName) {
  157 | 		Assert.hasText(constantName, "'constantName' must not be null or blank");
  158 | 		Integer transactionSynchronization = constants.get(constantName);
  159 | 		Assert.notNull(transactionSynchronization, "Only transaction synchronization constants allowed");
  160 | 		this.transactionSynchronization = transactionSynchronization;
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Set when this transaction manager should activate the thread-bound
  165 | 	 * transaction synchronization support. Default is "always".
  166 | 	 * <p>Note that transaction synchronization isn't supported for
  167 | 	 * multiple concurrent transactions by different transaction managers.
  168 | 	 * Only one transaction manager is allowed to activate it at any time.
  169 | 	 * @see #SYNCHRONIZATION_ALWAYS
  170 | 	 * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
  171 | 	 * @see #SYNCHRONIZATION_NEVER
  172 | 	 * @see TransactionSynchronizationManager
  173 | 	 * @see TransactionSynchronization
  174 | 	 */
  175 | 	public final void setTransactionSynchronization(int transactionSynchronization) {
  176 | 		this.transactionSynchronization = transactionSynchronization;
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * Return if this transaction manager should activate the thread-bound
  181 | 	 * transaction synchronization support.
  182 | 	 */
  183 | 	public final int getTransactionSynchronization() {
  184 | 		return this.transactionSynchronization;
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Specify the default timeout that this transaction manager should apply
  189 | 	 * if there is no timeout specified at the transaction level, in seconds.
  190 | 	 * <p>Default is the underlying transaction infrastructure's default timeout,
  191 | 	 * for example, typically 30 seconds in case of a JTA provider, indicated by the
  192 | 	 * {@code TransactionDefinition.TIMEOUT_DEFAULT} value.
  193 | 	 * @see org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT
  194 | 	 */
  195 | 	public final void setDefaultTimeout(int defaultTimeout) {
  196 | 		if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
  197 | 			throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
  198 | 		}
  199 | 		this.defaultTimeout = defaultTimeout;
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Return the default timeout that this transaction manager should apply
  204 | 	 * if there is no timeout specified at the transaction level, in seconds.
  205 | 	 * <p>Returns {@code TransactionDefinition.TIMEOUT_DEFAULT} to indicate
  206 | 	 * the underlying transaction infrastructure's default timeout.
  207 | 	 */
  208 | 	public final int getDefaultTimeout() {
  209 | 		return this.defaultTimeout;
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * Set whether nested transactions are allowed. Default is "false".
  214 | 	 * <p>Typically initialized with an appropriate default by the
  215 | 	 * concrete transaction manager subclass.
  216 | 	 */
  217 | 	public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
  218 | 		this.nestedTransactionAllowed = nestedTransactionAllowed;
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Return whether nested transactions are allowed.
  223 | 	 */
  224 | 	public final boolean isNestedTransactionAllowed() {
  225 | 		return this.nestedTransactionAllowed;
  226 | 	}
  227 | 
  228 | 	/**
  229 | 	 * Set whether existing transactions should be validated before participating
  230 | 	 * in them.
  231 | 	 * <p>When participating in an existing transaction (for example, with
  232 | 	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
  233 | 	 * transaction), this outer transaction's characteristics will apply even
  234 | 	 * to the inner transaction scope. Validation will detect incompatible
  235 | 	 * isolation level and read-only settings on the inner transaction definition
  236 | 	 * and reject participation accordingly through throwing a corresponding exception.
  237 | 	 * <p>Default is "false", leniently ignoring inner transaction settings,
  238 | 	 * simply overriding them with the outer transaction's characteristics.
  239 | 	 * Switch this flag to "true" in order to enforce strict validation.
  240 | 	 * @since 2.5.1
  241 | 	 */
  242 | 	public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
  243 | 		this.validateExistingTransaction = validateExistingTransaction;
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * Return whether existing transactions should be validated before participating
  248 | 	 * in them.
  249 | 	 * @since 2.5.1
  250 | 	 */
  251 | 	public final boolean isValidateExistingTransaction() {
  252 | 		return this.validateExistingTransaction;
  253 | 	}
  254 | 
  255 | 	/**
  256 | 	 * Set whether to globally mark an existing transaction as rollback-only
  257 | 	 * after a participating transaction failed.
  258 | 	 * <p>Default is "true": If a participating transaction (for example, with
  259 | 	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
  260 | 	 * transaction) fails, the transaction will be globally marked as rollback-only.
  261 | 	 * The only possible outcome of such a transaction is a rollback: The
  262 | 	 * transaction originator <i>cannot</i> make the transaction commit anymore.
  263 | 	 * <p>Switch this to "false" to let the transaction originator make the rollback
  264 | 	 * decision. If a participating transaction fails with an exception, the caller
  265 | 	 * can still decide to continue with a different path within the transaction.
  266 | 	 * However, note that this will only work as long as all participating resources
  267 | 	 * are capable of continuing towards a transaction commit even after a data access
  268 | 	 * failure: This is generally not the case for a Hibernate Session, for example;
  269 | 	 * neither is it for a sequence of JDBC insert/update/delete operations.
  270 | 	 * <p><b>Note:</b>This flag only applies to an explicit rollback attempt for a
  271 | 	 * subtransaction, typically caused by an exception thrown by a data access operation
  272 | 	 * (where TransactionInterceptor will trigger a {@code PlatformTransactionManager.rollback()}
  273 | 	 * call according to a rollback rule). If the flag is off, the caller can handle the exception
  274 | 	 * and decide on a rollback, independent of the rollback rules of the subtransaction.
  275 | 	 * This flag does, however, <i>not</i> apply to explicit {@code setRollbackOnly}
  276 | 	 * calls on a {@code TransactionStatus}, which will always cause an eventual
  277 | 	 * global rollback (as it might not throw an exception after the rollback-only call).
  278 | 	 * <p>The recommended solution for handling failure of a subtransaction
  279 | 	 * is a "nested transaction", where the global transaction can be rolled
  280 | 	 * back to a savepoint taken at the beginning of the subtransaction.
  281 | 	 * PROPAGATION_NESTED provides exactly those semantics; however, it will
  282 | 	 * only work when nested transaction support is available. This is the case
  283 | 	 * with DataSourceTransactionManager, but not with JtaTransactionManager.
  284 | 	 * @see #setNestedTransactionAllowed
  285 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager
  286 | 	 */
  287 | 	public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
  288 | 		this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
  289 | 	}
  290 | 
  291 | 	/**
  292 | 	 * Return whether to globally mark an existing transaction as rollback-only
  293 | 	 * after a participating transaction failed.
  294 | 	 */
  295 | 	public final boolean isGlobalRollbackOnParticipationFailure() {
  296 | 		return this.globalRollbackOnParticipationFailure;
  297 | 	}
  298 | 
  299 | 	/**
  300 | 	 * Set whether to fail early in case of the transaction being globally marked
  301 | 	 * as rollback-only.
  302 | 	 * <p>Default is "false", only causing an UnexpectedRollbackException at the
  303 | 	 * outermost transaction boundary. Switch this flag on to cause an
  304 | 	 * UnexpectedRollbackException as early as the global rollback-only marker
  305 | 	 * has been first detected, even from within an inner transaction boundary.
  306 | 	 * <p>Note that the fail-early behavior for global
  307 | 	 * rollback-only markers has been unified: All transaction managers will by
  308 | 	 * default only cause UnexpectedRollbackException at the outermost transaction
  309 | 	 * boundary. This allows, for example, to continue unit tests even after an
  310 | 	 * operation failed and the transaction will never be completed. All transaction
  311 | 	 * managers will only fail earlier if this flag has explicitly been set to "true".
  312 | 	 * @since 2.0
  313 | 	 * @see org.springframework.transaction.UnexpectedRollbackException
  314 | 	 */
  315 | 	public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
  316 | 		this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
  317 | 	}
  318 | 
  319 | 	/**
  320 | 	 * Return whether to fail early in case of the transaction being globally marked
  321 | 	 * as rollback-only.
  322 | 	 * @since 2.0
  323 | 	 */
  324 | 	public final boolean isFailEarlyOnGlobalRollbackOnly() {
  325 | 		return this.failEarlyOnGlobalRollbackOnly;
  326 | 	}
  327 | 
  328 | 	/**
  329 | 	 * Set whether {@code doRollback} should be performed on failure of the
  330 | 	 * {@code doCommit} call. Typically not necessary and thus to be avoided,
  331 | 	 * as it can potentially override the commit exception with a subsequent
  332 | 	 * rollback exception.
  333 | 	 * <p>Default is "false".
  334 | 	 * @see #doCommit
  335 | 	 * @see #doRollback
  336 | 	 */
  337 | 	public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
  338 | 		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
  339 | 	}
  340 | 
  341 | 	/**
  342 | 	 * Return whether {@code doRollback} should be performed on failure of the
  343 | 	 * {@code doCommit} call.
  344 | 	 */
  345 | 	public final boolean isRollbackOnCommitFailure() {
  346 | 		return this.rollbackOnCommitFailure;
  347 | 	}
  348 | 
  349 | 	@Override
  350 | 	public final void setTransactionExecutionListeners(Collection<TransactionExecutionListener> listeners) {
  351 | 		this.transactionExecutionListeners = listeners;
  352 | 	}
  353 | 
  354 | 	@Override
  355 | 	public final Collection<TransactionExecutionListener> getTransactionExecutionListeners() {
  356 | 		return this.transactionExecutionListeners;
  357 | 	}
  358 | 
  359 | 
  360 | 	//---------------------------------------------------------------------
  361 | 	// Implementation of PlatformTransactionManager
  362 | 	//---------------------------------------------------------------------
  363 | 
  364 | 	/**
  365 | 	 * This implementation handles propagation behavior. Delegates to
  366 | 	 * {@code doGetTransaction}, {@code isExistingTransaction}
  367 | 	 * and {@code doBegin}.
  368 | 	 * @see #doGetTransaction
  369 | 	 * @see #isExistingTransaction
  370 | 	 * @see #doBegin
  371 | 	 */
  372 | 	@Override
  373 | 	public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
  374 | 			throws TransactionException {
  375 | 
  376 | 		// Use defaults if no transaction definition given.
  377 | 		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
  378 | 
  379 | 		Object transaction = doGetTransaction();
  380 | 		boolean debugEnabled = logger.isDebugEnabled();
  381 | 
  382 | 		if (isExistingTransaction(transaction)) {
  383 | 			// Existing transaction found -> check propagation behavior to find out how to behave.
  384 | 			return handleExistingTransaction(def, transaction, debugEnabled);
  385 | 		}
  386 | 
  387 | 		// Check definition settings for new transaction.
  388 | 		if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
  389 | 			throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
  390 | 		}
  391 | 
  392 | 		// No existing transaction found -> check propagation behavior to find out how to proceed.
  393 | 		if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
  394 | 			throw new IllegalTransactionStateException(
  395 | 					"No existing transaction found for transaction marked with propagation 'mandatory'");
  396 | 		}
  397 | 		else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
  398 | 				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
  399 | 				def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
  400 | 			SuspendedResourcesHolder suspendedResources = suspend(null);
  401 | 			if (debugEnabled) {
  402 | 				logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
  403 | 			}
  404 | 			try {
  405 | 				return startTransaction(def, transaction, false, debugEnabled, suspendedResources);
  406 | 			}
  407 | 			catch (RuntimeException | Error ex) {
  408 | 				resume(null, suspendedResources);
  409 | 				throw ex;
  410 | 			}
  411 | 		}
  412 | 		else {
  413 | 			// Create "empty" transaction: no actual transaction, but potentially synchronization.
  414 | 			if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
  415 | 				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
  416 | 						"isolation level will effectively be ignored: " + def);
  417 | 			}
  418 | 			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
  419 | 			return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
  420 | 		}
  421 | 	}
  422 | 
  423 | 	/**
  424 | 	 * Create a TransactionStatus for an existing transaction.
  425 | 	 */
  426 | 	private TransactionStatus handleExistingTransaction(
  427 | 			TransactionDefinition definition, Object transaction, boolean debugEnabled)
  428 | 			throws TransactionException {
  429 | 
  430 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
  431 | 			throw new IllegalTransactionStateException(
  432 | 					"Existing transaction found for transaction marked with propagation 'never'");
  433 | 		}
  434 | 
  435 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
  436 | 			if (debugEnabled) {
  437 | 				logger.debug("Suspending current transaction");
  438 | 			}
  439 | 			Object suspendedResources = suspend(transaction);
  440 | 			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
  441 | 			return prepareTransactionStatus(
  442 | 					definition, null, false, newSynchronization, debugEnabled, suspendedResources);
  443 | 		}
  444 | 
  445 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
  446 | 			if (debugEnabled) {
  447 | 				logger.debug("Suspending current transaction, creating new transaction with name [" +
  448 | 						definition.getName() + "]");
  449 | 			}
  450 | 			SuspendedResourcesHolder suspendedResources = suspend(transaction);
  451 | 			try {
  452 | 				return startTransaction(definition, transaction, false, debugEnabled, suspendedResources);
  453 | 			}
  454 | 			catch (RuntimeException | Error beginEx) {
  455 | 				resumeAfterBeginException(transaction, suspendedResources, beginEx);
  456 | 				throw beginEx;
  457 | 			}
  458 | 		}
  459 | 
  460 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
  461 | 			if (!isNestedTransactionAllowed()) {
  462 | 				throw new NestedTransactionNotSupportedException(
  463 | 						"Transaction manager does not allow nested transactions by default - " +
  464 | 						"specify 'nestedTransactionAllowed' property with value 'true'");
  465 | 			}
  466 | 			if (debugEnabled) {
  467 | 				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
  468 | 			}
  469 | 			if (useSavepointForNestedTransaction()) {
  470 | 				// Create savepoint within existing Spring-managed transaction,
  471 | 				// through the SavepointManager API implemented by TransactionStatus.
  472 | 				// Usually uses JDBC savepoints. Never activates Spring synchronization.
  473 | 				DefaultTransactionStatus status = newTransactionStatus(
  474 | 						definition, transaction, false, false, true, debugEnabled, null);
  475 | 				this.transactionExecutionListeners.forEach(listener -> listener.beforeBegin(status));
  476 | 				try {
  477 | 					status.createAndHoldSavepoint();
  478 | 				}
  479 | 				catch (RuntimeException | Error ex) {
  480 | 					this.transactionExecutionListeners.forEach(listener -> listener.afterBegin(status, ex));
  481 | 					throw ex;
  482 | 				}
  483 | 				this.transactionExecutionListeners.forEach(listener -> listener.afterBegin(status, null));
  484 | 				return status;
  485 | 			}
  486 | 			else {
  487 | 				// Nested transaction through nested begin and commit/rollback calls.
  488 | 				// Usually only for JTA: Spring synchronization might get activated here
  489 | 				// in case of a pre-existing JTA transaction.
  490 | 				return startTransaction(definition, transaction, true, debugEnabled, null);
  491 | 			}
  492 | 		}
  493 | 
  494 | 		// PROPAGATION_REQUIRED, PROPAGATION_SUPPORTS, PROPAGATION_MANDATORY:
  495 | 		// regular participation in existing transaction.
  496 | 		if (debugEnabled) {
  497 | 			logger.debug("Participating in existing transaction");
  498 | 		}
  499 | 		if (isValidateExistingTransaction()) {
  500 | 			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
  501 | 				Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
  502 | 				if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
  503 | 					throw new IllegalTransactionStateException("Participating transaction with definition [" +
  504 | 							definition + "] specifies isolation level which is incompatible with existing transaction: " +
  505 | 							(currentIsolationLevel != null ?
  506 | 									DefaultTransactionDefinition.getIsolationLevelName(currentIsolationLevel) :
  507 | 									"(unknown)"));
  508 | 				}
  509 | 			}
  510 | 			if (!definition.isReadOnly()) {
  511 | 				if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
  512 | 					throw new IllegalTransactionStateException("Participating transaction with definition [" +
  513 | 							definition + "] is not marked as read-only but existing transaction is");
  514 | 				}
  515 | 			}
  516 | 		}
  517 | 		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
  518 | 		return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
  519 | 	}
  520 | 
  521 | 	/**
  522 | 	 * Start a new transaction.
  523 | 	 */
  524 | 	private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction,
  525 | 			boolean nested, boolean debugEnabled, @Nullable SuspendedResourcesHolder suspendedResources) {
  526 | 
  527 | 		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
  528 | 		DefaultTransactionStatus status = newTransactionStatus(
  529 | 				definition, transaction, true, newSynchronization, nested, debugEnabled, suspendedResources);
  530 | 		this.transactionExecutionListeners.forEach(listener -> listener.beforeBegin(status));
  531 | 		try {
  532 | 			doBegin(transaction, definition);
  533 | 		}
  534 | 		catch (RuntimeException | Error ex) {
  535 | 			this.transactionExecutionListeners.forEach(listener -> listener.afterBegin(status, ex));
  536 | 			throw ex;
  537 | 		}
  538 | 		prepareSynchronization(status, definition);
  539 | 		this.transactionExecutionListeners.forEach(listener -> listener.afterBegin(status, null));
  540 | 		return status;
  541 | 	}
  542 | 
  543 | 	/**
  544 | 	 * Create a new TransactionStatus for the given arguments,
  545 | 	 * also initializing transaction synchronization as appropriate.
  546 | 	 * @see #newTransactionStatus
  547 | 	 * @see #prepareTransactionStatus
  548 | 	 */
  549 | 	private DefaultTransactionStatus prepareTransactionStatus(
  550 | 			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
  551 | 			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
  552 | 
  553 | 		DefaultTransactionStatus status = newTransactionStatus(
  554 | 				definition, transaction, newTransaction, newSynchronization, false, debug, suspendedResources);
  555 | 		prepareSynchronization(status, definition);
  556 | 		return status;
  557 | 	}
  558 | 
  559 | 	/**
  560 | 	 * Create a TransactionStatus instance for the given arguments.
  561 | 	 */
  562 | 	private DefaultTransactionStatus newTransactionStatus(
  563 | 			TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
  564 | 			boolean newSynchronization, boolean nested, boolean debug, @Nullable Object suspendedResources) {
  565 | 
  566 | 		boolean actualNewSynchronization = newSynchronization &&
  567 | 				!TransactionSynchronizationManager.isSynchronizationActive();
  568 | 		return new DefaultTransactionStatus(definition.getName(), transaction, newTransaction,
  569 | 				actualNewSynchronization, nested, definition.isReadOnly(), debug, suspendedResources);
  570 | 	}
  571 | 
  572 | 	/**
  573 | 	 * Initialize transaction synchronization as appropriate.
  574 | 	 */
  575 | 	protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
  576 | 		if (status.isNewSynchronization()) {
  577 | 			TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
  578 | 			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
  579 | 					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
  580 | 							definition.getIsolationLevel() : null);
  581 | 			TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
  582 | 			TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
  583 | 			TransactionSynchronizationManager.initSynchronization();
  584 | 		}
  585 | 	}
  586 | 
  587 | 	/**
  588 | 	 * Determine the actual timeout to use for the given definition.
  589 | 	 * Will fall back to this manager's default timeout if the
  590 | 	 * transaction definition doesn't specify a non-default value.
  591 | 	 * @param definition the transaction definition
  592 | 	 * @return the actual timeout to use
  593 | 	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
  594 | 	 * @see #setDefaultTimeout
  595 | 	 */
  596 | 	protected int determineTimeout(TransactionDefinition definition) {
  597 | 		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
  598 | 			return definition.getTimeout();
  599 | 		}
  600 | 		return getDefaultTimeout();
  601 | 	}
  602 | 
  603 | 
  604 | 	/**
  605 | 	 * Suspend the given transaction. Suspends transaction synchronization first,
  606 | 	 * then delegates to the {@code doSuspend} template method.
  607 | 	 * @param transaction the current transaction object
  608 | 	 * (or {@code null} to just suspend active synchronizations, if any)
  609 | 	 * @return an object that holds suspended resources
  610 | 	 * (or {@code null} if neither transaction nor synchronization active)
  611 | 	 * @see #doSuspend
  612 | 	 * @see #resume
  613 | 	 */
  614 | 	protected final @Nullable SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {
  615 | 		if (TransactionSynchronizationManager.isSynchronizationActive()) {
  616 | 			List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
  617 | 			try {
  618 | 				Object suspendedResources = null;
  619 | 				if (transaction != null) {
  620 | 					suspendedResources = doSuspend(transaction);
  621 | 				}
  622 | 				String name = TransactionSynchronizationManager.getCurrentTransactionName();
  623 | 				TransactionSynchronizationManager.setCurrentTransactionName(null);
  624 | 				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
  625 | 				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
  626 | 				Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
  627 | 				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
  628 | 				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
  629 | 				TransactionSynchronizationManager.setActualTransactionActive(false);
  630 | 				return new SuspendedResourcesHolder(
  631 | 						suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
  632 | 			}
  633 | 			catch (RuntimeException | Error ex) {
  634 | 				// doSuspend failed - original transaction is still active...
  635 | 				doResumeSynchronization(suspendedSynchronizations);
  636 | 				throw ex;
  637 | 			}
  638 | 		}
  639 | 		else if (transaction != null) {
  640 | 			// Transaction active but no synchronization active.
  641 | 			Object suspendedResources = doSuspend(transaction);
  642 | 			return new SuspendedResourcesHolder(suspendedResources);
  643 | 		}
  644 | 		else {
  645 | 			// Neither transaction nor synchronization active.
  646 | 			return null;
  647 | 		}
  648 | 	}
  649 | 
  650 | 	/**
  651 | 	 * Resume the given transaction. Delegates to the {@code doResume}
  652 | 	 * template method first, then resuming transaction synchronization.
  653 | 	 * @param transaction the current transaction object
  654 | 	 * @param resourcesHolder the object that holds suspended resources,
  655 | 	 * as returned by {@code suspend} (or {@code null} to just
  656 | 	 * resume synchronizations, if any)
  657 | 	 * @see #doResume
  658 | 	 * @see #suspend
  659 | 	 */
  660 | 	protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
  661 | 			throws TransactionException {
  662 | 
  663 | 		if (resourcesHolder != null) {
  664 | 			Object suspendedResources = resourcesHolder.suspendedResources;
  665 | 			if (suspendedResources != null) {
  666 | 				doResume(transaction, suspendedResources);
  667 | 			}
  668 | 			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
  669 | 			if (suspendedSynchronizations != null) {
  670 | 				TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
  671 | 				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
  672 | 				TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
  673 | 				TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
  674 | 				doResumeSynchronization(suspendedSynchronizations);
  675 | 			}
  676 | 		}
  677 | 	}
  678 | 
  679 | 	/**
  680 | 	 * Resume outer transaction after inner transaction begin failed.
  681 | 	 */
  682 | 	private void resumeAfterBeginException(
  683 | 			Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {
  684 | 
  685 | 		try {
  686 | 			resume(transaction, suspendedResources);
  687 | 		}
  688 | 		catch (RuntimeException | Error resumeEx) {
  689 | 			String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
  690 | 			logger.error(exMessage, beginEx);
  691 | 			throw resumeEx;
  692 | 		}
  693 | 	}
  694 | 
  695 | 	/**
  696 | 	 * Suspend all current synchronizations and deactivate transaction
  697 | 	 * synchronization for the current thread.
  698 | 	 * @return the List of suspended TransactionSynchronization objects
  699 | 	 */
  700 | 	private List<TransactionSynchronization> doSuspendSynchronization() {
  701 | 		List<TransactionSynchronization> suspendedSynchronizations =
  702 | 				TransactionSynchronizationManager.getSynchronizations();
  703 | 		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
  704 | 			synchronization.suspend();
  705 | 		}
  706 | 		TransactionSynchronizationManager.clearSynchronization();
  707 | 		return suspendedSynchronizations;
  708 | 	}
  709 | 
  710 | 	/**
  711 | 	 * Reactivate transaction synchronization for the current thread
  712 | 	 * and resume all given synchronizations.
  713 | 	 * @param suspendedSynchronizations a List of TransactionSynchronization objects
  714 | 	 */
  715 | 	private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
  716 | 		TransactionSynchronizationManager.initSynchronization();
  717 | 		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
  718 | 			synchronization.resume();
  719 | 			TransactionSynchronizationManager.registerSynchronization(synchronization);
  720 | 		}
  721 | 	}
  722 | 
  723 | 
  724 | 	/**
  725 | 	 * This implementation of commit handles participating in existing
  726 | 	 * transactions and programmatic rollback requests.
  727 | 	 * Delegates to {@code isRollbackOnly}, {@code doCommit}
  728 | 	 * and {@code rollback}.
  729 | 	 * @see org.springframework.transaction.TransactionStatus#isRollbackOnly()
  730 | 	 * @see #doCommit
  731 | 	 * @see #rollback
  732 | 	 */
  733 | 	@Override
  734 | 	public final void commit(TransactionStatus status) throws TransactionException {
  735 | 		if (status.isCompleted()) {
  736 | 			throw new IllegalTransactionStateException(
  737 | 					"Transaction is already completed - do not call commit or rollback more than once per transaction");
  738 | 		}
  739 | 
  740 | 		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
  741 | 		if (defStatus.isLocalRollbackOnly()) {
  742 | 			if (defStatus.isDebug()) {
  743 | 				logger.debug("Transactional code has requested rollback");
  744 | 			}
  745 | 			processRollback(defStatus, false);
  746 | 			return;
  747 | 		}
  748 | 
  749 | 		if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
  750 | 			if (defStatus.isDebug()) {
  751 | 				logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
  752 | 			}
  753 | 			processRollback(defStatus, true);
  754 | 			return;
  755 | 		}
  756 | 
  757 | 		processCommit(defStatus);
  758 | 	}
  759 | 
  760 | 	/**
  761 | 	 * Process an actual commit.
  762 | 	 * Rollback-only flags have already been checked and applied.
  763 | 	 * @param status object representing the transaction
  764 | 	 * @throws TransactionException in case of commit failure
  765 | 	 */
  766 | 	private void processCommit(DefaultTransactionStatus status) throws TransactionException {
  767 | 		try {
  768 | 			boolean beforeCompletionInvoked = false;
  769 | 			boolean commitListenerInvoked = false;
  770 | 
  771 | 			try {
  772 | 				boolean unexpectedRollback = false;
  773 | 				prepareForCommit(status);
  774 | 				triggerBeforeCommit(status);
  775 | 				triggerBeforeCompletion(status);
  776 | 				beforeCompletionInvoked = true;
  777 | 
  778 | 				if (status.hasSavepoint()) {
  779 | 					if (status.isDebug()) {
  780 | 						logger.debug("Releasing transaction savepoint");
  781 | 					}
  782 | 					unexpectedRollback = status.isGlobalRollbackOnly();
  783 | 					this.transactionExecutionListeners.forEach(listener -> listener.beforeCommit(status));
  784 | 					commitListenerInvoked = true;
  785 | 					status.releaseHeldSavepoint();
  786 | 				}
  787 | 				else if (status.isNewTransaction()) {
  788 | 					if (status.isDebug()) {
  789 | 						logger.debug("Initiating transaction commit");
  790 | 					}
  791 | 					unexpectedRollback = status.isGlobalRollbackOnly();
  792 | 					this.transactionExecutionListeners.forEach(listener -> listener.beforeCommit(status));
  793 | 					commitListenerInvoked = true;
  794 | 					doCommit(status);
  795 | 				}
  796 | 				else if (isFailEarlyOnGlobalRollbackOnly()) {
  797 | 					unexpectedRollback = status.isGlobalRollbackOnly();
  798 | 				}
  799 | 
  800 | 				// Throw UnexpectedRollbackException if we have a global rollback-only
  801 | 				// marker but still didn't get a corresponding exception from commit.
  802 | 				if (unexpectedRollback) {
  803 | 					throw new UnexpectedRollbackException(
  804 | 							"Transaction silently rolled back because it has been marked as rollback-only");
  805 | 				}
  806 | 			}
  807 | 			catch (UnexpectedRollbackException ex) {
  808 | 				triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
  809 | 				this.transactionExecutionListeners.forEach(listener -> listener.afterRollback(status, null));
  810 | 				throw ex;
  811 | 			}
  812 | 			catch (TransactionException ex) {
  813 | 				if (isRollbackOnCommitFailure()) {
  814 | 					doRollbackOnCommitException(status, ex);
  815 | 				}
  816 | 				else {
  817 | 					triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
  818 | 					if (commitListenerInvoked) {
  819 | 						this.transactionExecutionListeners.forEach(listener -> listener.afterCommit(status, ex));
  820 | 					}
  821 | 				}
  822 | 				throw ex;
  823 | 			}
  824 | 			catch (RuntimeException | Error ex) {
  825 | 				if (!beforeCompletionInvoked) {
  826 | 					triggerBeforeCompletion(status);
  827 | 				}
  828 | 				doRollbackOnCommitException(status, ex);
  829 | 				throw ex;
  830 | 			}
  831 | 
  832 | 			// Trigger afterCommit callbacks, with an exception thrown there
  833 | 			// propagated to callers but the transaction still considered as committed.
  834 | 			try {
  835 | 				triggerAfterCommit(status);
  836 | 			}
  837 | 			finally {
  838 | 				triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
  839 | 				if (commitListenerInvoked) {
  840 | 					this.transactionExecutionListeners.forEach(listener -> listener.afterCommit(status, null));
  841 | 				}
  842 | 			}
  843 | 
  844 | 		}
  845 | 		finally {
  846 | 			cleanupAfterCompletion(status);
  847 | 		}
  848 | 	}
  849 | 
  850 | 	/**
  851 | 	 * This implementation of rollback handles participating in existing
  852 | 	 * transactions. Delegates to {@code doRollback} and
  853 | 	 * {@code doSetRollbackOnly}.
  854 | 	 * @see #doRollback
  855 | 	 * @see #doSetRollbackOnly
  856 | 	 */
  857 | 	@Override
  858 | 	public final void rollback(TransactionStatus status) throws TransactionException {
  859 | 		if (status.isCompleted()) {
  860 | 			throw new IllegalTransactionStateException(
  861 | 					"Transaction is already completed - do not call commit or rollback more than once per transaction");
  862 | 		}
  863 | 
  864 | 		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
  865 | 		processRollback(defStatus, false);
  866 | 	}
  867 | 
  868 | 	/**
  869 | 	 * Process an actual rollback.
  870 | 	 * The completed flag has already been checked.
  871 | 	 * @param status object representing the transaction
  872 | 	 * @throws TransactionException in case of rollback failure
  873 | 	 */
  874 | 	private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
  875 | 		try {
  876 | 			boolean unexpectedRollback = unexpected;
  877 | 			boolean rollbackListenerInvoked = false;
  878 | 
  879 | 			try {
  880 | 				triggerBeforeCompletion(status);
  881 | 
  882 | 				if (status.hasSavepoint()) {
  883 | 					if (status.isDebug()) {
  884 | 						logger.debug("Rolling back transaction to savepoint");
  885 | 					}
  886 | 					this.transactionExecutionListeners.forEach(listener -> listener.beforeRollback(status));
  887 | 					rollbackListenerInvoked = true;
  888 | 					status.rollbackToHeldSavepoint();
  889 | 				}
  890 | 				else if (status.isNewTransaction()) {
  891 | 					if (status.isDebug()) {
  892 | 						logger.debug("Initiating transaction rollback");
  893 | 					}
  894 | 					this.transactionExecutionListeners.forEach(listener -> listener.beforeRollback(status));
  895 | 					rollbackListenerInvoked = true;
  896 | 					doRollback(status);
  897 | 				}
  898 | 				else {
  899 | 					// Participating in larger transaction
  900 | 					if (status.hasTransaction()) {
  901 | 						if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
  902 | 							if (status.isDebug()) {
  903 | 								logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
  904 | 							}
  905 | 							doSetRollbackOnly(status);
  906 | 						}
  907 | 						else {
  908 | 							if (status.isDebug()) {
  909 | 								logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
  910 | 							}
  911 | 						}
  912 | 					}
  913 | 					else {
  914 | 						logger.debug("Should roll back transaction but cannot - no transaction available");
  915 | 					}
  916 | 					// Unexpected rollback only matters here if we're asked to fail early
  917 | 					if (!isFailEarlyOnGlobalRollbackOnly()) {
  918 | 						unexpectedRollback = false;
  919 | 					}
  920 | 				}
  921 | 			}
  922 | 			catch (RuntimeException | Error ex) {
  923 | 				triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
  924 | 				if (rollbackListenerInvoked) {
  925 | 					this.transactionExecutionListeners.forEach(listener -> listener.afterRollback(status, ex));
  926 | 				}
  927 | 				throw ex;
  928 | 			}
  929 | 
  930 | 			triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
  931 | 			if (rollbackListenerInvoked) {
  932 | 				this.transactionExecutionListeners.forEach(listener -> listener.afterRollback(status, null));
  933 | 			}
  934 | 
  935 | 			// Raise UnexpectedRollbackException if we had a global rollback-only marker
  936 | 			if (unexpectedRollback) {
  937 | 				throw new UnexpectedRollbackException(
  938 | 						"Transaction rolled back because it has been marked as rollback-only");
  939 | 			}
  940 | 		}
  941 | 		finally {
  942 | 			cleanupAfterCompletion(status);
  943 | 		}
  944 | 	}
  945 | 
  946 | 	/**
  947 | 	 * Invoke {@code doRollback}, handling rollback exceptions properly.
  948 | 	 * @param status object representing the transaction
  949 | 	 * @param ex the thrown application exception or error
  950 | 	 * @throws TransactionException in case of rollback failure
  951 | 	 * @see #doRollback
  952 | 	 */
  953 | 	private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
  954 | 		try {
  955 | 			if (status.isNewTransaction()) {
  956 | 				if (status.isDebug()) {
  957 | 					logger.debug("Initiating transaction rollback after commit exception", ex);
  958 | 				}
  959 | 				doRollback(status);
  960 | 			}
  961 | 			else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
  962 | 				if (status.isDebug()) {
  963 | 					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
  964 | 				}
  965 | 				doSetRollbackOnly(status);
  966 | 			}
  967 | 		}
  968 | 		catch (RuntimeException | Error rbex) {
  969 | 			logger.error("Commit exception overridden by rollback exception", ex);
  970 | 			triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
  971 | 			this.transactionExecutionListeners.forEach(listener -> listener.afterRollback(status, rbex));
  972 | 			throw rbex;
  973 | 		}
  974 | 		triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
  975 | 		this.transactionExecutionListeners.forEach(listener -> listener.afterRollback(status, null));
  976 | 	}
  977 | 
  978 | 
  979 | 	/**
  980 | 	 * Trigger {@code beforeCommit} callbacks.
  981 | 	 * @param status object representing the transaction
  982 | 	 */
  983 | 	protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
  984 | 		if (status.isNewSynchronization()) {
  985 | 			TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
  986 | 		}
  987 | 	}
  988 | 
  989 | 	/**
  990 | 	 * Trigger {@code beforeCompletion} callbacks.
  991 | 	 * @param status object representing the transaction
  992 | 	 */
  993 | 	protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
  994 | 		if (status.isNewSynchronization()) {
  995 | 			TransactionSynchronizationUtils.triggerBeforeCompletion();
  996 | 		}
  997 | 	}
  998 | 
  999 | 	/**
 1000 | 	 * Trigger {@code afterCommit} callbacks.
 1001 | 	 * @param status object representing the transaction
 1002 | 	 */
 1003 | 	private void triggerAfterCommit(DefaultTransactionStatus status) {
 1004 | 		if (status.isNewSynchronization()) {
 1005 | 			TransactionSynchronizationUtils.triggerAfterCommit();
 1006 | 		}
 1007 | 	}
 1008 | 
 1009 | 	/**
 1010 | 	 * Trigger {@code afterCompletion} callbacks.
 1011 | 	 * @param status object representing the transaction
 1012 | 	 * @param completionStatus completion status according to TransactionSynchronization constants
 1013 | 	 */
 1014 | 	private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
 1015 | 		if (status.isNewSynchronization()) {
 1016 | 			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
 1017 | 			TransactionSynchronizationManager.clearSynchronization();
 1018 | 			if (!status.hasTransaction() || status.isNewTransaction()) {
 1019 | 				// No transaction or new transaction for the current scope ->
 1020 | 				// invoke the afterCompletion callbacks immediately
 1021 | 				invokeAfterCompletion(synchronizations, completionStatus);
 1022 | 			}
 1023 | 			else if (!synchronizations.isEmpty()) {
 1024 | 				// Existing transaction that we participate in, controlled outside
 1025 | 				// the scope of this Spring transaction manager -> try to register
 1026 | 				// an afterCompletion callback with the existing (JTA) transaction.
 1027 | 				registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
 1028 | 			}
 1029 | 		}
 1030 | 	}
 1031 | 
 1032 | 	/**
 1033 | 	 * Actually invoke the {@code afterCompletion} methods of the
 1034 | 	 * given Spring TransactionSynchronization objects.
 1035 | 	 * <p>To be called by this abstract manager itself, or by special implementations
 1036 | 	 * of the {@code registerAfterCompletionWithExistingTransaction} callback.
 1037 | 	 * @param synchronizations a List of TransactionSynchronization objects
 1038 | 	 * @param completionStatus the completion status according to the
 1039 | 	 * constants in the TransactionSynchronization interface
 1040 | 	 * @see #registerAfterCompletionWithExistingTransaction(Object, java.util.List)
 1041 | 	 * @see TransactionSynchronization#STATUS_COMMITTED
 1042 | 	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
 1043 | 	 * @see TransactionSynchronization#STATUS_UNKNOWN
 1044 | 	 */
 1045 | 	protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
 1046 | 		TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
 1047 | 	}
 1048 | 
 1049 | 	/**
 1050 | 	 * Clean up after completion, clearing synchronization if necessary,
 1051 | 	 * and invoking doCleanupAfterCompletion.
 1052 | 	 * @param status object representing the transaction
 1053 | 	 * @see #doCleanupAfterCompletion
 1054 | 	 */
 1055 | 	private void cleanupAfterCompletion(DefaultTransactionStatus status) {
 1056 | 		status.setCompleted();
 1057 | 		if (status.isNewSynchronization()) {
 1058 | 			TransactionSynchronizationManager.clear();
 1059 | 		}
 1060 | 		if (status.isNewTransaction()) {
 1061 | 			doCleanupAfterCompletion(status.getTransaction());
 1062 | 		}
 1063 | 		if (status.getSuspendedResources() != null) {
 1064 | 			if (status.isDebug()) {
 1065 | 				logger.debug("Resuming suspended transaction after completion of inner transaction");
 1066 | 			}
 1067 | 			Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
 1068 | 			resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
 1069 | 		}
 1070 | 	}
 1071 | 
 1072 | 
 1073 | 	//---------------------------------------------------------------------
 1074 | 	// Template methods to be implemented in subclasses
 1075 | 	//---------------------------------------------------------------------
 1076 | 
 1077 | 	/**
 1078 | 	 * Return a transaction object for the current transaction state.
 1079 | 	 * <p>The returned object will usually be specific to the concrete transaction
 1080 | 	 * manager implementation, carrying corresponding transaction state in a
 1081 | 	 * modifiable fashion. This object will be passed into the other template
 1082 | 	 * methods (for example, doBegin and doCommit), either directly or as part of a
 1083 | 	 * DefaultTransactionStatus instance.
 1084 | 	 * <p>The returned object should contain information about any existing
 1085 | 	 * transaction, that is, a transaction that has already started before the
 1086 | 	 * current {@code getTransaction} call on the transaction manager.
 1087 | 	 * Consequently, a {@code doGetTransaction} implementation will usually
 1088 | 	 * look for an existing transaction and store corresponding state in the
 1089 | 	 * returned transaction object.
 1090 | 	 * @return the current transaction object
 1091 | 	 * @throws org.springframework.transaction.CannotCreateTransactionException
 1092 | 	 * if transaction support is not available
 1093 | 	 * @throws TransactionException in case of lookup or system errors
 1094 | 	 * @see #doBegin
 1095 | 	 * @see #doCommit
 1096 | 	 * @see #doRollback
 1097 | 	 * @see DefaultTransactionStatus#getTransaction
 1098 | 	 */
 1099 | 	protected abstract Object doGetTransaction() throws TransactionException;
 1100 | 
 1101 | 	/**
 1102 | 	 * Check if the given transaction object indicates an existing transaction
 1103 | 	 * (that is, a transaction which has already started).
 1104 | 	 * <p>The result will be evaluated according to the specified propagation
 1105 | 	 * behavior for the new transaction. An existing transaction might get
 1106 | 	 * suspended (in case of PROPAGATION_REQUIRES_NEW), or the new transaction
 1107 | 	 * might participate in the existing one (in case of PROPAGATION_REQUIRED).
 1108 | 	 * <p>The default implementation returns {@code false}, assuming that
 1109 | 	 * participating in existing transactions is generally not supported.
 1110 | 	 * Subclasses are of course encouraged to provide such support.
 1111 | 	 * @param transaction the transaction object returned by doGetTransaction
 1112 | 	 * @return if there is an existing transaction
 1113 | 	 * @throws TransactionException in case of system errors
 1114 | 	 * @see #doGetTransaction
 1115 | 	 */
 1116 | 	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
 1117 | 		return false;
 1118 | 	}
 1119 | 
 1120 | 	/**
 1121 | 	 * Return whether to use a savepoint for a nested transaction.
 1122 | 	 * <p>Default is {@code true}, which causes delegation to DefaultTransactionStatus
 1123 | 	 * for creating and holding a savepoint. If the transaction object does not implement
 1124 | 	 * the SavepointManager interface, a NestedTransactionNotSupportedException will be
 1125 | 	 * thrown. Else, the SavepointManager will be asked to create a new savepoint to
 1126 | 	 * demarcate the start of the nested transaction.
 1127 | 	 * <p>Subclasses can override this to return {@code false}, causing a further
 1128 | 	 * call to {@code doBegin} - within the context of an already existing transaction.
 1129 | 	 * The {@code doBegin} implementation needs to handle this accordingly in such
 1130 | 	 * a scenario. This is appropriate for JTA, for example.
 1131 | 	 * @see DefaultTransactionStatus#createAndHoldSavepoint
 1132 | 	 * @see DefaultTransactionStatus#rollbackToHeldSavepoint
 1133 | 	 * @see DefaultTransactionStatus#releaseHeldSavepoint
 1134 | 	 * @see #doBegin
 1135 | 	 */
 1136 | 	protected boolean useSavepointForNestedTransaction() {
 1137 | 		return true;
 1138 | 	}
 1139 | 
 1140 | 	/**
 1141 | 	 * Begin a new transaction with semantics according to the given transaction
 1142 | 	 * definition. Does not have to care about applying the propagation behavior,
 1143 | 	 * as this has already been handled by this abstract manager.
 1144 | 	 * <p>This method gets called when the transaction manager has decided to actually
 1145 | 	 * start a new transaction. Either there wasn't any transaction before, or the
 1146 | 	 * previous transaction has been suspended.
 1147 | 	 * <p>A special scenario is a nested transaction without savepoint: If
 1148 | 	 * {@code useSavepointForNestedTransaction()} returns "false", this method
 1149 | 	 * will be called to start a nested transaction when necessary. In such a context,
 1150 | 	 * there will be an active transaction: The implementation of this method has
 1151 | 	 * to detect this and start an appropriate nested transaction.
 1152 | 	 * @param transaction the transaction object returned by {@code doGetTransaction}
 1153 | 	 * @param definition a TransactionDefinition instance, describing propagation
 1154 | 	 * behavior, isolation level, read-only flag, timeout, and transaction name
 1155 | 	 * @throws TransactionException in case of creation or system errors
 1156 | 	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
 1157 | 	 * if the underlying transaction does not support nesting
 1158 | 	 */
 1159 | 	protected abstract void doBegin(Object transaction, TransactionDefinition definition)
 1160 | 			throws TransactionException;
 1161 | 
 1162 | 	/**
 1163 | 	 * Suspend the resources of the current transaction.
 1164 | 	 * Transaction synchronization will already have been suspended.
 1165 | 	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
 1166 | 	 * assuming that transaction suspension is generally not supported.
 1167 | 	 * @param transaction the transaction object returned by {@code doGetTransaction}
 1168 | 	 * @return an object that holds suspended resources
 1169 | 	 * (will be kept unexamined for passing it into doResume)
 1170 | 	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
 1171 | 	 * if suspending is not supported by the transaction manager implementation
 1172 | 	 * @throws TransactionException in case of system errors
 1173 | 	 * @see #doResume
 1174 | 	 */
 1175 | 	protected Object doSuspend(Object transaction) throws TransactionException {
 1176 | 		throw new TransactionSuspensionNotSupportedException(
 1177 | 				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
 1178 | 	}
 1179 | 
 1180 | 	/**
 1181 | 	 * Resume the resources of the current transaction.
 1182 | 	 * Transaction synchronization will be resumed afterwards.
 1183 | 	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
 1184 | 	 * assuming that transaction suspension is generally not supported.
 1185 | 	 * @param transaction the transaction object returned by {@code doGetTransaction}
 1186 | 	 * @param suspendedResources the object that holds suspended resources,
 1187 | 	 * as returned by doSuspend
 1188 | 	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
 1189 | 	 * if resuming is not supported by the transaction manager implementation
 1190 | 	 * @throws TransactionException in case of system errors
 1191 | 	 * @see #doSuspend
 1192 | 	 */
 1193 | 	protected void doResume(@Nullable Object transaction, Object suspendedResources) throws TransactionException {
 1194 | 		throw new TransactionSuspensionNotSupportedException(
 1195 | 				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
 1196 | 	}
 1197 | 
 1198 | 	/**
 1199 | 	 * Return whether to call {@code doCommit} on a transaction that has been
 1200 | 	 * marked as rollback-only in a global fashion.
 1201 | 	 * <p>Does not apply if an application locally sets the transaction to rollback-only
 1202 | 	 * via the TransactionStatus, but only to the transaction itself being marked as
 1203 | 	 * rollback-only by the transaction coordinator.
 1204 | 	 * <p>Default is "false": Local transaction strategies usually don't hold the rollback-only
 1205 | 	 * marker in the transaction itself, therefore they can't handle rollback-only transactions
 1206 | 	 * as part of transaction commit. Hence, AbstractPlatformTransactionManager will trigger
 1207 | 	 * a rollback in that case, throwing an UnexpectedRollbackException afterwards.
 1208 | 	 * <p>Override this to return "true" if the concrete transaction manager expects a
 1209 | 	 * {@code doCommit} call even for a rollback-only transaction, allowing for
 1210 | 	 * special handling there. This will, for example, be the case for JTA, where
 1211 | 	 * {@code UserTransaction.commit} will check the read-only flag itself and
 1212 | 	 * throw a corresponding RollbackException, which might include the specific reason
 1213 | 	 * (such as a transaction timeout).
 1214 | 	 * <p>If this method returns "true" but the {@code doCommit} implementation does not
 1215 | 	 * throw an exception, this transaction manager will throw an UnexpectedRollbackException
 1216 | 	 * itself. This should not be the typical case; it is mainly checked to cover misbehaving
 1217 | 	 * JTA providers that silently roll back even when the rollback has not been requested
 1218 | 	 * by the calling code.
 1219 | 	 * @see #doCommit
 1220 | 	 * @see DefaultTransactionStatus#isGlobalRollbackOnly()
 1221 | 	 * @see DefaultTransactionStatus#isLocalRollbackOnly()
 1222 | 	 * @see org.springframework.transaction.TransactionStatus#setRollbackOnly()
 1223 | 	 * @see org.springframework.transaction.UnexpectedRollbackException
 1224 | 	 * @see jakarta.transaction.UserTransaction#commit()
 1225 | 	 * @see jakarta.transaction.RollbackException
 1226 | 	 */
 1227 | 	protected boolean shouldCommitOnGlobalRollbackOnly() {
 1228 | 		return false;
 1229 | 	}
 1230 | 
 1231 | 	/**
 1232 | 	 * Make preparations for commit, to be performed before the
 1233 | 	 * {@code beforeCommit} synchronization callbacks occur.
 1234 | 	 * <p>Note that exceptions will get propagated to the commit caller
 1235 | 	 * and cause a rollback of the transaction.
 1236 | 	 * @param status the status representation of the transaction
 1237 | 	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
 1238 | 	 * (note: do not throw TransactionException subclasses here!)
 1239 | 	 */
 1240 | 	protected void prepareForCommit(DefaultTransactionStatus status) {
 1241 | 	}
 1242 | 
 1243 | 	/**
 1244 | 	 * Perform an actual commit of the given transaction.
 1245 | 	 * <p>An implementation does not need to check the "new transaction" flag
 1246 | 	 * or the rollback-only flag; this will already have been handled before.
 1247 | 	 * Usually, a straight commit will be performed on the transaction object
 1248 | 	 * contained in the passed-in status.
 1249 | 	 * @param status the status representation of the transaction
 1250 | 	 * @throws TransactionException in case of commit or system errors
 1251 | 	 * @see DefaultTransactionStatus#getTransaction
 1252 | 	 */
 1253 | 	protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;
 1254 | 
 1255 | 	/**
 1256 | 	 * Perform an actual rollback of the given transaction.
 1257 | 	 * <p>An implementation does not need to check the "new transaction" flag;
 1258 | 	 * this will already have been handled before. Usually, a straight rollback
 1259 | 	 * will be performed on the transaction object contained in the passed-in status.
 1260 | 	 * @param status the status representation of the transaction
 1261 | 	 * @throws TransactionException in case of system errors
 1262 | 	 * @see DefaultTransactionStatus#getTransaction
 1263 | 	 */
 1264 | 	protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;
 1265 | 
 1266 | 	/**
 1267 | 	 * Set the given transaction rollback-only. Only called on rollback
 1268 | 	 * if the current transaction participates in an existing one.
 1269 | 	 * <p>The default implementation throws an IllegalTransactionStateException,
 1270 | 	 * assuming that participating in existing transactions is generally not
 1271 | 	 * supported. Subclasses are of course encouraged to provide such support.
 1272 | 	 * @param status the status representation of the transaction
 1273 | 	 * @throws TransactionException in case of system errors
 1274 | 	 */
 1275 | 	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
 1276 | 		throw new IllegalTransactionStateException(
 1277 | 				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
 1278 | 				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
 1279 | 	}
 1280 | 
 1281 | 	/**
 1282 | 	 * Register the given list of transaction synchronizations with the existing transaction.
 1283 | 	 * <p>Invoked when the control of the Spring transaction manager and thus all Spring
 1284 | 	 * transaction synchronizations end, without the transaction being completed yet. This
 1285 | 	 * is for example the case when participating in an existing JTA or EJB CMT transaction.
 1286 | 	 * <p>The default implementation simply invokes the {@code afterCompletion} methods
 1287 | 	 * immediately, passing in "STATUS_UNKNOWN". This is the best we can do if there's no
 1288 | 	 * chance to determine the actual outcome of the outer transaction.
 1289 | 	 * @param transaction the transaction object returned by {@code doGetTransaction}
 1290 | 	 * @param synchronizations a List of TransactionSynchronization objects
 1291 | 	 * @throws TransactionException in case of system errors
 1292 | 	 * @see #invokeAfterCompletion(java.util.List, int)
 1293 | 	 * @see TransactionSynchronization#afterCompletion(int)
 1294 | 	 * @see TransactionSynchronization#STATUS_UNKNOWN
 1295 | 	 */
 1296 | 	protected void registerAfterCompletionWithExistingTransaction(
 1297 | 			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {
 1298 | 
 1299 | 		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
 1300 | 				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
 1301 | 		invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
 1302 | 	}
 1303 | 
 1304 | 	/**
 1305 | 	 * Cleanup resources after transaction completion.
 1306 | 	 * <p>Called after {@code doCommit} and {@code doRollback} execution,
 1307 | 	 * on any outcome. The default implementation does nothing.
 1308 | 	 * <p>Should not throw any exceptions but just issue warnings on errors.
 1309 | 	 * @param transaction the transaction object returned by {@code doGetTransaction}
 1310 | 	 */
 1311 | 	protected void doCleanupAfterCompletion(Object transaction) {
 1312 | 	}
 1313 | 
 1314 | 
 1315 | 	//---------------------------------------------------------------------
 1316 | 	// Serialization support
 1317 | 	//---------------------------------------------------------------------
 1318 | 
 1319 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
 1320 | 		// Rely on default serialization; just initialize state after deserialization.
 1321 | 		ois.defaultReadObject();
 1322 | 
 1323 | 		// Initialize transient fields.
 1324 | 		this.logger = LogFactory.getLog(getClass());
 1325 | 	}
 1326 | 
 1327 | 
 1328 | 	/**
 1329 | 	 * Holder for suspended resources.
 1330 | 	 * Used internally by {@code suspend} and {@code resume}.
 1331 | 	 */
 1332 | 	protected static final class SuspendedResourcesHolder {
 1333 | 
 1334 | 		private final @Nullable Object suspendedResources;
 1335 | 
 1336 | 		private @Nullable List<TransactionSynchronization> suspendedSynchronizations;
 1337 | 
 1338 | 		private @Nullable String name;
 1339 | 
 1340 | 		private boolean readOnly;
 1341 | 
 1342 | 		private @Nullable Integer isolationLevel;
 1343 | 
 1344 | 		private boolean wasActive;
 1345 | 
 1346 | 		private SuspendedResourcesHolder(Object suspendedResources) {
 1347 | 			this.suspendedResources = suspendedResources;
 1348 | 		}
 1349 | 
 1350 | 		private SuspendedResourcesHolder(
 1351 | 				@Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
 1352 | 				@Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {
 1353 | 
 1354 | 			this.suspendedResources = suspendedResources;
 1355 | 			this.suspendedSynchronizations = suspendedSynchronizations;
 1356 | 			this.name = name;
 1357 | 			this.readOnly = readOnly;
 1358 | 			this.isolationLevel = isolationLevel;
 1359 | 			this.wasActive = wasActive;
 1360 | 		}
 1361 | 	}
 1362 | 
 1363 | }
```
