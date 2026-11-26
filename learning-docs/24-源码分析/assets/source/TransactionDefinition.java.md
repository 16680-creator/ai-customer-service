<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/TransactionDefinition.java -->
<!-- 正文引用：[[源码:TransactionDefinition.java#L起-L止]] ；代码块头部：(src: TransactionDefinition.java#L起-L止) -->

# TransactionDefinition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/TransactionDefinition.java` |
| 行数 | 293 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:59.186Z |

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
   17 | package org.springframework.transaction;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Interface that defines Spring-compliant transaction properties.
   23 |  * Based on the propagation behavior definitions analogous to EJB CMT attributes.
   24 |  *
   25 |  * <p>Note that isolation level and timeout settings will not get applied unless
   26 |  * an actual new transaction gets started. As only {@link #PROPAGATION_REQUIRED},
   27 |  * {@link #PROPAGATION_REQUIRES_NEW}, and {@link #PROPAGATION_NESTED} can cause
   28 |  * that, it usually doesn't make sense to specify those settings in other cases.
   29 |  * Furthermore, be aware that not all transaction managers will support those
   30 |  * advanced features and thus might throw corresponding exceptions when given
   31 |  * non-default values.
   32 |  *
   33 |  * <p>The {@linkplain #isReadOnly() read-only flag} applies to any transaction context,
   34 |  * whether backed by an actual resource transaction or operating non-transactionally
   35 |  * at the resource level. In the latter case, the flag will only apply to managed
   36 |  * resources within the application, such as a Hibernate {@code Session}.
   37 |  *
   38 |  * @author Juergen Hoeller
   39 |  * @since 08.05.2003
   40 |  * @see PlatformTransactionManager#getTransaction(TransactionDefinition)
   41 |  * @see org.springframework.transaction.support.DefaultTransactionDefinition
   42 |  * @see org.springframework.transaction.interceptor.TransactionAttribute
   43 |  */
   44 | public interface TransactionDefinition {
   45 | 
   46 | 	/**
   47 | 	 * Support a current transaction; create a new one if none exists.
   48 | 	 * Analogous to the EJB transaction attribute of the same name.
   49 | 	 * <p>This is typically the default setting of a transaction definition
   50 | 	 * and typically defines a transaction synchronization scope.
   51 | 	 */
   52 | 	int PROPAGATION_REQUIRED = 0;
   53 | 
   54 | 	/**
   55 | 	 * Support a current transaction; execute non-transactionally if none exists.
   56 | 	 * Analogous to the EJB transaction attribute of the same name.
   57 | 	 * <p><b>NOTE:</b> For transaction managers with transaction synchronization,
   58 | 	 * {@code PROPAGATION_SUPPORTS} is slightly different from no transaction
   59 | 	 * at all, as it defines a transaction scope that synchronization might apply to.
   60 | 	 * As a consequence, the same resources (a JDBC {@code Connection}, a
   61 | 	 * Hibernate {@code Session}, etc) will be shared for the entire specified
   62 | 	 * scope. Note that the exact behavior depends on the actual synchronization
   63 | 	 * configuration of the transaction manager.
   64 | 	 * <p>In general, use {@code PROPAGATION_SUPPORTS} with care. In particular, do
   65 | 	 * not rely on {@code PROPAGATION_REQUIRED} or {@code PROPAGATION_REQUIRES_NEW}
   66 | 	 * <i>within</i> a {@code PROPAGATION_SUPPORTS} scope (which may lead to
   67 | 	 * synchronization conflicts at runtime). If such nesting is unavoidable, make sure
   68 | 	 * to configure your transaction manager appropriately (typically switching to
   69 | 	 * "synchronization on actual transaction").
   70 | 	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
   71 | 	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
   72 | 	 */
   73 | 	int PROPAGATION_SUPPORTS = 1;
   74 | 
   75 | 	/**
   76 | 	 * Support a current transaction; throw an exception if no current transaction
   77 | 	 * exists. Analogous to the EJB transaction attribute of the same name.
   78 | 	 * <p>Note that transaction synchronization within a {@code PROPAGATION_MANDATORY}
   79 | 	 * scope will always be driven by the surrounding transaction.
   80 | 	 */
   81 | 	int PROPAGATION_MANDATORY = 2;
   82 | 
   83 | 	/**
   84 | 	 * Create a new transaction, suspending the current transaction if one exists.
   85 | 	 * Analogous to the EJB transaction attribute of the same name.
   86 | 	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
   87 | 	 * on all transaction managers. This in particular applies to
   88 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
   89 | 	 * which requires the {@code jakarta.transaction.TransactionManager} to be
   90 | 	 * made available to it (which is server-specific in standard Jakarta EE).
   91 | 	 * <p>A {@code PROPAGATION_REQUIRES_NEW} scope always defines its own
   92 | 	 * transaction synchronizations. Existing synchronizations will be suspended
   93 | 	 * and resumed appropriately.
   94 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
   95 | 	 */
   96 | 	int PROPAGATION_REQUIRES_NEW = 3;
   97 | 
   98 | 	/**
   99 | 	 * Do not support a current transaction; rather always execute non-transactionally.
  100 | 	 * Analogous to the EJB transaction attribute of the same name.
  101 | 	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
  102 | 	 * on all transaction managers. This in particular applies to
  103 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
  104 | 	 * which requires the {@code jakarta.transaction.TransactionManager} to be
  105 | 	 * made available to it (which is server-specific in standard Jakarta EE).
  106 | 	 * <p>Note that transaction synchronization is <i>not</i> available within a
  107 | 	 * {@code PROPAGATION_NOT_SUPPORTED} scope. Existing synchronizations
  108 | 	 * will be suspended and resumed appropriately.
  109 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
  110 | 	 */
  111 | 	int PROPAGATION_NOT_SUPPORTED = 4;
  112 | 
  113 | 	/**
  114 | 	 * Do not support a current transaction; throw an exception if a current transaction
  115 | 	 * exists. Analogous to the EJB transaction attribute of the same name.
  116 | 	 * <p>Note that transaction synchronization is <i>not</i> available within a
  117 | 	 * {@code PROPAGATION_NEVER} scope.
  118 | 	 */
  119 | 	int PROPAGATION_NEVER = 5;
  120 | 
  121 | 	/**
  122 | 	 * Execute within a nested transaction if a current transaction exists,
  123 | 	 * behaving like {@link #PROPAGATION_REQUIRED} otherwise. There is no
  124 | 	 * analogous feature in EJB.
  125 | 	 * <p><b>NOTE:</b> Actual creation of a nested transaction will only work on
  126 | 	 * specific transaction managers. Out of the box, this only applies to the JDBC
  127 | 	 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
  128 | 	 * when working on a JDBC 3.0+ driver. Some JTA providers might support
  129 | 	 * nested transactions as well.
  130 | 	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
  131 | 	 */
  132 | 	int PROPAGATION_NESTED = 6;
  133 | 
  134 | 
  135 | 	/**
  136 | 	 * Use the default isolation level of the underlying datastore.
  137 | 	 * <p>All other levels correspond to the JDBC isolation levels.
  138 | 	 * @see java.sql.Connection
  139 | 	 */
  140 | 	int ISOLATION_DEFAULT = -1;
  141 | 
  142 | 	/**
  143 | 	 * Indicates that dirty reads, non-repeatable reads, and phantom reads
  144 | 	 * can occur.
  145 | 	 * <p>This level allows a row changed by one transaction to be read by another
  146 | 	 * transaction before any changes in that row have been committed (a "dirty read").
  147 | 	 * If any of the changes are rolled back, the second transaction will have
  148 | 	 * retrieved an invalid row.
  149 | 	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
  150 | 	 */
  151 | 	int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
  152 | 
  153 | 	/**
  154 | 	 * Indicates that dirty reads are prevented; non-repeatable reads and
  155 | 	 * phantom reads can occur.
  156 | 	 * <p>This level only prohibits a transaction from reading a row with uncommitted
  157 | 	 * changes in it.
  158 | 	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
  159 | 	 */
  160 | 	int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;
  161 | 
  162 | 	/**
  163 | 	 * Indicates that dirty reads and non-repeatable reads are prevented;
  164 | 	 * phantom reads can occur.
  165 | 	 * <p>This level prohibits a transaction from reading a row with uncommitted changes
  166 | 	 * in it, and it also prohibits the situation where one transaction reads a row,
  167 | 	 * a second transaction alters the row, and the first transaction re-reads the row,
  168 | 	 * getting different values the second time (a "non-repeatable read").
  169 | 	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
  170 | 	 */
  171 | 	int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;
  172 | 
  173 | 	/**
  174 | 	 * Indicates that dirty reads, non-repeatable reads, and phantom reads
  175 | 	 * are prevented.
  176 | 	 * <p>This level includes the prohibitions in {@link #ISOLATION_REPEATABLE_READ}
  177 | 	 * and further prohibits the situation where one transaction reads all rows that
  178 | 	 * satisfy a {@code WHERE} condition, a second transaction inserts a row
  179 | 	 * that satisfies that {@code WHERE} condition, and the first transaction
  180 | 	 * re-reads for the same condition, retrieving the additional "phantom" row
  181 | 	 * in the second read.
  182 | 	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
  183 | 	 */
  184 | 	int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;
  185 | 
  186 | 
  187 | 	/**
  188 | 	 * Use the default timeout of the underlying transaction system,
  189 | 	 * or none if timeouts are not supported.
  190 | 	 */
  191 | 	int TIMEOUT_DEFAULT = -1;
  192 | 
  193 | 
  194 | 	/**
  195 | 	 * Return the propagation behavior.
  196 | 	 * <p>Must return one of the {@code PROPAGATION_XXX} constants
  197 | 	 * defined on {@link TransactionDefinition this interface}.
  198 | 	 * <p>The default is {@link #PROPAGATION_REQUIRED}.
  199 | 	 * @return the propagation behavior
  200 | 	 * @see #PROPAGATION_REQUIRED
  201 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
  202 | 	 */
  203 | 	default int getPropagationBehavior() {
  204 | 		return PROPAGATION_REQUIRED;
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Return the isolation level.
  209 | 	 * <p>Must return one of the {@code ISOLATION_XXX} constants defined on
  210 | 	 * {@link TransactionDefinition this interface}. Those constants are designed
  211 | 	 * to match the values of the same constants on {@link java.sql.Connection}.
  212 | 	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
  213 | 	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
  214 | 	 * transactions. Consider switching the "validateExistingTransaction" flag to
  215 | 	 * "true" on your transaction manager if you'd like isolation level declarations
  216 | 	 * to get rejected when participating in an existing transaction with a different
  217 | 	 * isolation level.
  218 | 	 * <p>The default is {@link #ISOLATION_DEFAULT}. Note that a transaction manager
  219 | 	 * that does not support custom isolation levels will throw an exception when
  220 | 	 * given any other level than {@link #ISOLATION_DEFAULT}.
  221 | 	 * @return the isolation level
  222 | 	 * @see #ISOLATION_DEFAULT
  223 | 	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
  224 | 	 */
  225 | 	default int getIsolationLevel() {
  226 | 		return ISOLATION_DEFAULT;
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Return the transaction timeout.
  231 | 	 * <p>Must return a number of seconds, or {@link #TIMEOUT_DEFAULT}.
  232 | 	 * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
  233 | 	 * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
  234 | 	 * transactions.
  235 | 	 * <p>Note that a transaction manager that does not support timeouts will throw
  236 | 	 * an exception when given any other timeout than {@link #TIMEOUT_DEFAULT}.
  237 | 	 * <p>The default is {@link #TIMEOUT_DEFAULT}.
  238 | 	 * @return the transaction timeout
  239 | 	 */
  240 | 	default int getTimeout() {
  241 | 		return TIMEOUT_DEFAULT;
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * Return whether to optimize as a read-only transaction.
  246 | 	 * <p>The read-only flag applies to any transaction context, whether backed
  247 | 	 * by an actual resource transaction ({@link #PROPAGATION_REQUIRED}/
  248 | 	 * {@link #PROPAGATION_REQUIRES_NEW}) or operating non-transactionally at
  249 | 	 * the resource level ({@link #PROPAGATION_SUPPORTS}). In the latter case,
  250 | 	 * the flag will only apply to managed resources within the application,
  251 | 	 * such as a Hibernate {@code Session}.
  252 | 	 * <p>This just serves as a hint for the actual transaction subsystem;
  253 | 	 * it will <i>not necessarily</i> cause failure of write access attempts.
  254 | 	 * A transaction manager which cannot interpret the read-only hint will
  255 | 	 * <i>not</i> throw an exception when asked for a read-only transaction.
  256 | 	 * @return {@code true} if the transaction is to be optimized as read-only
  257 | 	 * ({@code false} by default)
  258 | 	 * @see org.springframework.transaction.support.TransactionSynchronization#beforeCommit(boolean)
  259 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
  260 | 	 */
  261 | 	default boolean isReadOnly() {
  262 | 		return false;
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Return the name of this transaction. Can be {@code null}.
  267 | 	 * <p>This will be used as the transaction name to be shown in a
  268 | 	 * transaction monitor, if applicable.
  269 | 	 * <p>In case of Spring's declarative transactions, the exposed name will be
  270 | 	 * the {@code fully-qualified class name + "." + method name} (by default).
  271 | 	 * @return the name of this transaction ({@code null} by default}
  272 | 	 * @see org.springframework.transaction.interceptor.TransactionAspectSupport
  273 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionName()
  274 | 	 */
  275 | 	default @Nullable String getName() {
  276 | 		return null;
  277 | 	}
  278 | 
  279 | 
  280 | 	// Static builder methods
  281 | 
  282 | 	/**
  283 | 	 * Return an unmodifiable {@code TransactionDefinition} with defaults.
  284 | 	 * <p>For customization purposes, use the modifiable
  285 | 	 * {@link org.springframework.transaction.support.DefaultTransactionDefinition}
  286 | 	 * instead.
  287 | 	 * @since 5.2
  288 | 	 */
  289 | 	static TransactionDefinition withDefaults() {
  290 | 		return StaticTransactionDefinition.INSTANCE;
  291 | 	}
  292 | 
  293 | }
```
