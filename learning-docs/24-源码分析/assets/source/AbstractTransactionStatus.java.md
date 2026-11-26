<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/AbstractTransactionStatus.java -->
<!-- 正文引用：[[源码:AbstractTransactionStatus.java#L起-L止]] ；代码块头部：(src: AbstractTransactionStatus.java#L起-L止) -->

# AbstractTransactionStatus.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/AbstractTransactionStatus.java` |
| 行数 | 231 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:58.327Z |

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
   23 | import org.springframework.transaction.TransactionException;
   24 | import org.springframework.transaction.TransactionStatus;
   25 | import org.springframework.transaction.TransactionUsageException;
   26 | 
   27 | /**
   28 |  * Abstract base implementation of the
   29 |  * {@link org.springframework.transaction.TransactionStatus} interface.
   30 |  *
   31 |  * <p>Pre-implements the handling of local rollback-only and completed flags, and
   32 |  * delegation to an underlying {@link org.springframework.transaction.SavepointManager}.
   33 |  * Also offers the option of a holding a savepoint within the transaction.
   34 |  *
   35 |  * <p>Does not assume any specific internal transaction handling, such as an
   36 |  * underlying transaction object, and no transaction synchronization mechanism.
   37 |  *
   38 |  * @author Juergen Hoeller
   39 |  * @since 1.2.3
   40 |  * @see #setRollbackOnly()
   41 |  * @see #isRollbackOnly()
   42 |  * @see #setCompleted()
   43 |  * @see #isCompleted()
   44 |  * @see #getSavepointManager()
   45 |  * @see SimpleTransactionStatus
   46 |  * @see DefaultTransactionStatus
   47 |  */
   48 | public abstract class AbstractTransactionStatus implements TransactionStatus {
   49 | 
   50 | 	private boolean rollbackOnly = false;
   51 | 
   52 | 	private boolean completed = false;
   53 | 
   54 | 	private @Nullable Object savepoint;
   55 | 
   56 | 
   57 | 	//---------------------------------------------------------------------
   58 | 	// Implementation of TransactionExecution
   59 | 	//---------------------------------------------------------------------
   60 | 
   61 | 	@Override
   62 | 	public void setRollbackOnly() {
   63 | 		if (this.completed) {
   64 | 			throw new IllegalStateException("Transaction completed");
   65 | 		}
   66 | 		this.rollbackOnly = true;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Determine the rollback-only flag via checking both the local rollback-only flag
   71 | 	 * of this TransactionStatus and the global rollback-only flag of the underlying
   72 | 	 * transaction, if any.
   73 | 	 * @see #isLocalRollbackOnly()
   74 | 	 * @see #isGlobalRollbackOnly()
   75 | 	 */
   76 | 	@Override
   77 | 	public boolean isRollbackOnly() {
   78 | 		return (isLocalRollbackOnly() || isGlobalRollbackOnly());
   79 | 	}
   80 | 
   81 | 	/**
   82 | 	 * Determine the rollback-only flag via checking this TransactionStatus.
   83 | 	 * <p>Will only return "true" if the application called {@code setRollbackOnly}
   84 | 	 * on this TransactionStatus object.
   85 | 	 */
   86 | 	public boolean isLocalRollbackOnly() {
   87 | 		return this.rollbackOnly;
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Template method for determining the global rollback-only flag of the
   92 | 	 * underlying transaction, if any.
   93 | 	 * <p>This implementation always returns {@code false}.
   94 | 	 */
   95 | 	public boolean isGlobalRollbackOnly() {
   96 | 		return false;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Mark this transaction as completed, that is, committed or rolled back.
  101 | 	 */
  102 | 	public void setCompleted() {
  103 | 		this.completed = true;
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public boolean isCompleted() {
  108 | 		return this.completed;
  109 | 	}
  110 | 
  111 | 
  112 | 	//---------------------------------------------------------------------
  113 | 	// Handling of current savepoint state
  114 | 	//---------------------------------------------------------------------
  115 | 
  116 | 	@Override
  117 | 	public boolean hasSavepoint() {
  118 | 		return (this.savepoint != null);
  119 | 	}
  120 | 
  121 | 	/**
  122 | 	 * Set a savepoint for this transaction. Useful for PROPAGATION_NESTED.
  123 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NESTED
  124 | 	 */
  125 | 	protected void setSavepoint(@Nullable Object savepoint) {
  126 | 		this.savepoint = savepoint;
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Get the savepoint for this transaction, if any.
  131 | 	 */
  132 | 	protected @Nullable Object getSavepoint() {
  133 | 		return this.savepoint;
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Create a savepoint and hold it for the transaction.
  138 | 	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
  139 | 	 * if the underlying transaction does not support savepoints
  140 | 	 * @see SavepointManager#createSavepoint
  141 | 	 */
  142 | 	public void createAndHoldSavepoint() throws TransactionException {
  143 | 		Object savepoint = getSavepointManager().createSavepoint();
  144 | 		TransactionSynchronizationUtils.triggerSavepoint(savepoint);
  145 | 		setSavepoint(savepoint);
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Roll back to the savepoint that is held for the transaction
  150 | 	 * and release the savepoint right afterwards.
  151 | 	 * @see SavepointManager#rollbackToSavepoint
  152 | 	 * @see SavepointManager#releaseSavepoint
  153 | 	 */
  154 | 	public void rollbackToHeldSavepoint() throws TransactionException {
  155 | 		Object savepoint = getSavepoint();
  156 | 		if (savepoint == null) {
  157 | 			throw new TransactionUsageException(
  158 | 					"Cannot roll back to savepoint - no savepoint associated with current transaction");
  159 | 		}
  160 | 		TransactionSynchronizationUtils.triggerSavepointRollback(savepoint);
  161 | 		getSavepointManager().rollbackToSavepoint(savepoint);
  162 | 		getSavepointManager().releaseSavepoint(savepoint);
  163 | 		setSavepoint(null);
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Release the savepoint that is held for the transaction.
  168 | 	 * @see SavepointManager#releaseSavepoint
  169 | 	 */
  170 | 	public void releaseHeldSavepoint() throws TransactionException {
  171 | 		Object savepoint = getSavepoint();
  172 | 		if (savepoint == null) {
  173 | 			throw new TransactionUsageException(
  174 | 					"Cannot release savepoint - no savepoint associated with current transaction");
  175 | 		}
  176 | 		getSavepointManager().releaseSavepoint(savepoint);
  177 | 		setSavepoint(null);
  178 | 	}
  179 | 
  180 | 
  181 | 	//---------------------------------------------------------------------
  182 | 	// Implementation of SavepointManager
  183 | 	//---------------------------------------------------------------------
  184 | 
  185 | 	/**
  186 | 	 * This implementation delegates to a SavepointManager for the
  187 | 	 * underlying transaction, if possible.
  188 | 	 * @see #getSavepointManager()
  189 | 	 * @see SavepointManager#createSavepoint()
  190 | 	 */
  191 | 	@Override
  192 | 	public Object createSavepoint() throws TransactionException {
  193 | 		Object savepoint = getSavepointManager().createSavepoint();
  194 | 		TransactionSynchronizationUtils.triggerSavepoint(savepoint);
  195 | 		return savepoint;
  196 | 	}
  197 | 
  198 | 	/**
  199 | 	 * This implementation delegates to a SavepointManager for the
  200 | 	 * underlying transaction, if possible.
  201 | 	 * @see #getSavepointManager()
  202 | 	 * @see SavepointManager#rollbackToSavepoint(Object)
  203 | 	 */
  204 | 	@Override
  205 | 	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
  206 | 		TransactionSynchronizationUtils.triggerSavepointRollback(savepoint);
  207 | 		getSavepointManager().rollbackToSavepoint(savepoint);
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * This implementation delegates to a SavepointManager for the
  212 | 	 * underlying transaction, if possible.
  213 | 	 * @see #getSavepointManager()
  214 | 	 * @see SavepointManager#releaseSavepoint(Object)
  215 | 	 */
  216 | 	@Override
  217 | 	public void releaseSavepoint(Object savepoint) throws TransactionException {
  218 | 		getSavepointManager().releaseSavepoint(savepoint);
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Return a SavepointManager for the underlying transaction, if possible.
  223 | 	 * <p>Default implementation always throws a NestedTransactionNotSupportedException.
  224 | 	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
  225 | 	 * if the underlying transaction does not support savepoints
  226 | 	 */
  227 | 	protected SavepointManager getSavepointManager() {
  228 | 		throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
  229 | 	}
  230 | 
  231 | }
```
