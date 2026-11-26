<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/DefaultTransactionStatus.java -->
<!-- 正文引用：[[源码:DefaultTransactionStatus.java#L起-L止]] ；代码块头部：(src: DefaultTransactionStatus.java#L起-L止) -->

# DefaultTransactionStatus.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/DefaultTransactionStatus.java` |
| 行数 | 218 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:58.628Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.transaction.NestedTransactionNotSupportedException;
   22 | import org.springframework.transaction.SavepointManager;
   23 | import org.springframework.util.Assert;
   24 | 
   25 | /**
   26 |  * Default implementation of the {@link org.springframework.transaction.TransactionStatus}
   27 |  * interface, used by {@link AbstractPlatformTransactionManager}. Based on the concept
   28 |  * of an underlying "transaction object".
   29 |  *
   30 |  * <p>Holds all status information that {@link AbstractPlatformTransactionManager}
   31 |  * needs internally, including a generic transaction object determined by the
   32 |  * concrete transaction manager implementation.
   33 |  *
   34 |  * <p>Supports delegating savepoint-related methods to a transaction object
   35 |  * that implements the {@link SavepointManager} interface.
   36 |  *
   37 |  * <p><b>NOTE:</b> This is <i>not</i> intended for use with other PlatformTransactionManager
   38 |  * implementations, in particular not for mock transaction managers in testing environments.
   39 |  * Use the alternative {@link SimpleTransactionStatus} class or a mock for the plain
   40 |  * {@link org.springframework.transaction.TransactionStatus} interface instead.
   41 |  *
   42 |  * @author Juergen Hoeller
   43 |  * @since 19.01.2004
   44 |  * @see AbstractPlatformTransactionManager
   45 |  * @see org.springframework.transaction.SavepointManager
   46 |  * @see #getTransaction
   47 |  * @see #createSavepoint
   48 |  * @see #rollbackToSavepoint
   49 |  * @see #releaseSavepoint
   50 |  * @see SimpleTransactionStatus
   51 |  */
   52 | public class DefaultTransactionStatus extends AbstractTransactionStatus {
   53 | 
   54 | 	private final @Nullable String transactionName;
   55 | 
   56 | 	private final @Nullable Object transaction;
   57 | 
   58 | 	private final boolean newTransaction;
   59 | 
   60 | 	private final boolean newSynchronization;
   61 | 
   62 | 	private final boolean nested;
   63 | 
   64 | 	private final boolean readOnly;
   65 | 
   66 | 	private final boolean debug;
   67 | 
   68 | 	private final @Nullable Object suspendedResources;
   69 | 
   70 | 
   71 | 	/**
   72 | 	 * Create a new {@code DefaultTransactionStatus} instance.
   73 | 	 * @param transactionName the defined name of the transaction
   74 | 	 * @param transaction underlying transaction object that can hold state
   75 | 	 * for the internal transaction implementation
   76 | 	 * @param newTransaction if the transaction is new, otherwise participating
   77 | 	 * in an existing transaction
   78 | 	 * @param newSynchronization if a new transaction synchronization has been
   79 | 	 * opened for the given transaction
   80 | 	 * @param readOnly whether the transaction is marked as read-only
   81 | 	 * @param debug should debug logging be enabled for the handling of this transaction?
   82 | 	 * Caching it in here can prevent repeated calls to ask the logging system whether
   83 | 	 * debug logging should be enabled.
   84 | 	 * @param suspendedResources a holder for resources that have been suspended
   85 | 	 * for this transaction, if any
   86 | 	 * @since 6.1
   87 | 	 */
   88 | 	public DefaultTransactionStatus(
   89 | 			@Nullable String transactionName, @Nullable Object transaction, boolean newTransaction,
   90 | 			boolean newSynchronization, boolean nested, boolean readOnly, boolean debug,
   91 | 			@Nullable Object suspendedResources) {
   92 | 
   93 | 		this.transactionName = transactionName;
   94 | 		this.transaction = transaction;
   95 | 		this.newTransaction = newTransaction;
   96 | 		this.newSynchronization = newSynchronization;
   97 | 		this.nested = nested;
   98 | 		this.readOnly = readOnly;
   99 | 		this.debug = debug;
  100 | 		this.suspendedResources = suspendedResources;
  101 | 	}
  102 | 
  103 | 
  104 | 	@Override
  105 | 	public String getTransactionName() {
  106 | 		return (this.transactionName != null ? this.transactionName : "");
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Return the underlying transaction object.
  111 | 	 * @throws IllegalStateException if no transaction is active
  112 | 	 */
  113 | 	public Object getTransaction() {
  114 | 		Assert.state(this.transaction != null, "No transaction active");
  115 | 		return this.transaction;
  116 | 	}
  117 | 
  118 | 	@Override
  119 | 	public boolean hasTransaction() {
  120 | 		return (this.transaction != null);
  121 | 	}
  122 | 
  123 | 	@Override
  124 | 	public boolean isNewTransaction() {
  125 | 		return (hasTransaction() && this.newTransaction);
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Return if a new transaction synchronization has been opened for this transaction.
  130 | 	 */
  131 | 	public boolean isNewSynchronization() {
  132 | 		return this.newSynchronization;
  133 | 	}
  134 | 
  135 | 	@Override
  136 | 	public boolean isNested() {
  137 | 		return this.nested;
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	public boolean isReadOnly() {
  142 | 		return this.readOnly;
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Return whether the progress of this transaction is debugged. This is used by
  147 | 	 * {@link AbstractPlatformTransactionManager} as an optimization, to prevent repeated
  148 | 	 * calls to {@code logger.isDebugEnabled()}. Not really intended for client code.
  149 | 	 */
  150 | 	public boolean isDebug() {
  151 | 		return this.debug;
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Return the holder for resources that have been suspended for this transaction,
  156 | 	 * if any.
  157 | 	 */
  158 | 	public @Nullable Object getSuspendedResources() {
  159 | 		return this.suspendedResources;
  160 | 	}
  161 | 
  162 | 
  163 | 	//---------------------------------------------------------------------
  164 | 	// Enable functionality through underlying transaction object
  165 | 	//---------------------------------------------------------------------
  166 | 
  167 | 	/**
  168 | 	 * Determine the rollback-only flag via checking the transaction object, provided
  169 | 	 * that the latter implements the {@link SmartTransactionObject} interface.
  170 | 	 * <p>Will return {@code true} if the global transaction itself has been marked
  171 | 	 * rollback-only by the transaction coordinator, for example in case of a timeout.
  172 | 	 * @see SmartTransactionObject#isRollbackOnly()
  173 | 	 */
  174 | 	@Override
  175 | 	public boolean isGlobalRollbackOnly() {
  176 | 		return (this.transaction instanceof SmartTransactionObject smartTransactionObject &&
  177 | 				smartTransactionObject.isRollbackOnly());
  178 | 	}
  179 | 
  180 | 	/**
  181 | 	 * This implementation exposes the {@link SavepointManager} interface
  182 | 	 * of the underlying transaction object, if any.
  183 | 	 * @throws NestedTransactionNotSupportedException if savepoints are not supported
  184 | 	 * @see #isTransactionSavepointManager()
  185 | 	 */
  186 | 	@Override
  187 | 	protected SavepointManager getSavepointManager() {
  188 | 		Object transaction = this.transaction;
  189 | 		if (!(transaction instanceof SavepointManager savepointManager)) {
  190 | 			throw new NestedTransactionNotSupportedException(
  191 | 					"Transaction object [" + this.transaction + "] does not support savepoints");
  192 | 		}
  193 | 		return savepointManager;
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Return whether the underlying transaction implements the {@link SavepointManager}
  198 | 	 * interface and therefore supports savepoints.
  199 | 	 * @see #getTransaction()
  200 | 	 * @see #getSavepointManager()
  201 | 	 */
  202 | 	public boolean isTransactionSavepointManager() {
  203 | 		return (this.transaction instanceof SavepointManager);
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Delegate the flushing to the transaction object, provided that the latter
  208 | 	 * implements the {@link SmartTransactionObject} interface.
  209 | 	 * @see SmartTransactionObject#flush()
  210 | 	 */
  211 | 	@Override
  212 | 	public void flush() {
  213 | 		if (this.transaction instanceof SmartTransactionObject smartTransactionObject) {
  214 | 			smartTransactionObject.flush();
  215 | 		}
  216 | 	}
  217 | 
  218 | }
```
