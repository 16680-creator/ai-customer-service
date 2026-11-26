<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/TransactionSynchronization.java -->
<!-- 正文引用：[[源码:TransactionSynchronization.java#L起-L止]] ；代码块头部：(src: TransactionSynchronization.java#L起-L止) -->

# TransactionSynchronization.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/TransactionSynchronization.java` |
| 行数 | 189 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:15.006Z |

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
   19 | import java.io.Flushable;
   20 | 
   21 | import org.springframework.core.Ordered;
   22 | 
   23 | /**
   24 |  * Interface for transaction synchronization callbacks.
   25 |  * Supported by AbstractPlatformTransactionManager.
   26 |  *
   27 |  * <p>TransactionSynchronization implementations can implement the Ordered interface
   28 |  * to influence their execution order. A synchronization that does not implement the
   29 |  * Ordered interface is appended to the end of the synchronization chain.
   30 |  *
   31 |  * <p>System synchronizations performed by Spring itself use specific order values,
   32 |  * allowing for fine-grained interaction with their execution order (if necessary).
   33 |  *
   34 |  * <p>Implements the {@link Ordered} interface to enable the execution order of
   35 |  * synchronizations to be controlled declaratively. The default {@link #getOrder()
   36 |  * order} is {@link Ordered#LOWEST_PRECEDENCE}, indicating late execution; return
   37 |  * a lower value for earlier execution.
   38 |  *
   39 |  * @author Juergen Hoeller
   40 |  * @since 02.06.2003
   41 |  * @see TransactionSynchronizationManager
   42 |  * @see AbstractPlatformTransactionManager
   43 |  * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
   44 |  */
   45 | public interface TransactionSynchronization extends Ordered, Flushable {
   46 | 
   47 | 	/** Completion status in case of proper commit. */
   48 | 	int STATUS_COMMITTED = 0;
   49 | 
   50 | 	/** Completion status in case of proper rollback. */
   51 | 	int STATUS_ROLLED_BACK = 1;
   52 | 
   53 | 	/** Completion status in case of heuristic mixed completion or system errors. */
   54 | 	int STATUS_UNKNOWN = 2;
   55 | 
   56 | 
   57 | 	/**
   58 | 	 * Return the execution order for this transaction synchronization.
   59 | 	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
   60 | 	 */
   61 | 	@Override
   62 | 	default int getOrder() {
   63 | 		return Ordered.LOWEST_PRECEDENCE;
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * Suspend this synchronization.
   68 | 	 * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
   69 | 	 * @see TransactionSynchronizationManager#unbindResource
   70 | 	 */
   71 | 	default void suspend() {
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * Resume this synchronization.
   76 | 	 * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
   77 | 	 * @see TransactionSynchronizationManager#bindResource
   78 | 	 */
   79 | 	default void resume() {
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Flush the underlying session to the datastore, if applicable:
   84 | 	 * for example, a Hibernate/JPA session.
   85 | 	 * @see org.springframework.transaction.TransactionStatus#flush()
   86 | 	 */
   87 | 	@Override
   88 | 	default void flush() {
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Invoked on creation of a new savepoint, either when a nested transaction
   93 | 	 * is started against an existing transaction or on a programmatic savepoint
   94 | 	 * via {@link org.springframework.transaction.TransactionStatus}.
   95 | 	 * <p>This synchronization callback is invoked right <i>after</i> the creation
   96 | 	 * of the resource savepoint, with the given savepoint object already active.
   97 | 	 * @param savepoint the associated savepoint object (primarily as a key for
   98 | 	 * identifying the savepoint but also castable to the resource savepoint type)
   99 | 	 * @since 6.2
  100 | 	 * @see org.springframework.transaction.SavepointManager#createSavepoint
  101 | 	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NESTED
  102 | 	 */
  103 | 	default void savepoint(Object savepoint) {
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Invoked in case of a rollback to the previously created savepoint.
  108 | 	 * <p>This synchronization callback is invoked right <i>before</i> the rollback
  109 | 	 * of the resource savepoint, with the given savepoint object still active.
  110 | 	 * @param savepoint the associated savepoint object (primarily as a key for
  111 | 	 * identifying the savepoint but also castable to the resource savepoint type)
  112 | 	 * @since 6.2
  113 | 	 * @see #savepoint
  114 | 	 * @see org.springframework.transaction.SavepointManager#rollbackToSavepoint
  115 | 	 */
  116 | 	default void savepointRollback(Object savepoint) {
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Invoked before transaction commit (before "beforeCompletion").
  121 | 	 * Can, for example, flush transactional O/R Mapping sessions to the database.
  122 | 	 * <p>This callback does <i>not</i> mean that the transaction will actually be committed.
  123 | 	 * A rollback decision can still occur after this method has been called. This callback
  124 | 	 * is rather meant to perform work that's only relevant if a commit still has a chance
  125 | 	 * to happen, such as flushing SQL statements to the database.
  126 | 	 * <p>Note that exceptions will get propagated to the commit caller and cause a
  127 | 	 * rollback of the transaction.
  128 | 	 * @param readOnly whether the transaction is defined as read-only transaction
  129 | 	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
  130 | 	 * (note: do not throw TransactionException subclasses here!)
  131 | 	 * @see #beforeCompletion
  132 | 	 */
  133 | 	default void beforeCommit(boolean readOnly) {
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Invoked before transaction commit/rollback.
  138 | 	 * Can perform resource cleanup <i>before</i> transaction completion.
  139 | 	 * <p>This method will be invoked after {@code beforeCommit}, even when
  140 | 	 * {@code beforeCommit} threw an exception. This callback allows for
  141 | 	 * closing resources before transaction completion, for any outcome.
  142 | 	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
  143 | 	 * (note: do not throw TransactionException subclasses here!)
  144 | 	 * @see #beforeCommit
  145 | 	 * @see #afterCompletion
  146 | 	 */
  147 | 	default void beforeCompletion() {
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Invoked after transaction commit. Can perform further operations right
  152 | 	 * <i>after</i> the main transaction has <i>successfully</i> committed.
  153 | 	 * <p>Can, for example, commit further operations that are supposed to follow on a successful
  154 | 	 * commit of the main transaction, like confirmation messages or emails.
  155 | 	 * <p><b>NOTE:</b> The transaction will have been committed already, but the
  156 | 	 * transactional resources might still be active and accessible. As a consequence,
  157 | 	 * any data access code triggered at this point will still "participate" in the
  158 | 	 * original transaction, allowing to perform some cleanup (with no commit following
  159 | 	 * anymore!), unless it explicitly declares that it needs to run in a separate
  160 | 	 * transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW} for any
  161 | 	 * transactional operation that is called from here.</b>
  162 | 	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
  163 | 	 * (note: do not throw TransactionException subclasses here!)
  164 | 	 */
  165 | 	default void afterCommit() {
  166 | 	}
  167 | 
  168 | 	/**
  169 | 	 * Invoked after transaction commit/rollback.
  170 | 	 * Can perform resource cleanup <i>after</i> transaction completion.
  171 | 	 * <p><b>NOTE:</b> The transaction will have been committed or rolled back already,
  172 | 	 * but the transactional resources might still be active and accessible. As a
  173 | 	 * consequence, any data access code triggered at this point will still "participate"
  174 | 	 * in the original transaction, allowing to perform some cleanup (with no commit
  175 | 	 * following anymore!), unless it explicitly declares that it needs to run in a
  176 | 	 * separate transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW}
  177 | 	 * for any transactional operation that is called from here.</b>
  178 | 	 * @param status completion status according to the {@code STATUS_*} constants
  179 | 	 * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
  180 | 	 * (note: do not throw TransactionException subclasses here!)
  181 | 	 * @see #STATUS_COMMITTED
  182 | 	 * @see #STATUS_ROLLED_BACK
  183 | 	 * @see #STATUS_UNKNOWN
  184 | 	 * @see #beforeCompletion
  185 | 	 */
  186 | 	default void afterCompletion(int status) {
  187 | 	}
  188 | 
  189 | }
```
