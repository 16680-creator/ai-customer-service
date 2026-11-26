<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/PlatformTransactionManager.java -->
<!-- 正文引用：[[源码:PlatformTransactionManager.java#L起-L止]] ；代码块头部：(src: PlatformTransactionManager.java#L起-L止) -->

# PlatformTransactionManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/PlatformTransactionManager.java` |
| 行数 | 120 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:59.332Z |

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
   22 |  * This is the central interface in Spring's imperative transaction infrastructure.
   23 |  * Applications can use this directly, but it is not primarily meant as an API:
   24 |  * Typically, applications will work with either TransactionTemplate or
   25 |  * declarative transaction demarcation through AOP.
   26 |  *
   27 |  * <p>For implementors, it is recommended to derive from the provided
   28 |  * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager}
   29 |  * class, which pre-implements the defined propagation behavior and takes care
   30 |  * of transaction synchronization handling. Subclasses have to implement
   31 |  * template methods for specific states of the underlying transaction,
   32 |  * for example: begin, suspend, resume, commit.
   33 |  *
   34 |  * <p>A classic implementation of this strategy interface is
   35 |  * {@link org.springframework.transaction.jta.JtaTransactionManager}. However,
   36 |  * in common single-resource scenarios, Spring's specific transaction managers
   37 |  * for example, JDBC, JPA, JMS are preferred choices.
   38 |  *
   39 |  * @author Rod Johnson
   40 |  * @author Juergen Hoeller
   41 |  * @since 16.05.2003
   42 |  * @see org.springframework.transaction.support.TransactionTemplate
   43 |  * @see org.springframework.transaction.interceptor.TransactionInterceptor
   44 |  * @see org.springframework.transaction.ReactiveTransactionManager
   45 |  * @see ConfigurableTransactionManager
   46 |  */
   47 | public interface PlatformTransactionManager extends TransactionManager {
   48 | 
   49 | 	/**
   50 | 	 * Return a currently active transaction or create a new one, according to
   51 | 	 * the specified propagation behavior.
   52 | 	 * <p>Note that parameters like isolation level or timeout will only be applied
   53 | 	 * to new transactions, and thus be ignored when participating in active ones.
   54 | 	 * <p>Furthermore, not all transaction definition settings will be supported
   55 | 	 * by every transaction manager: A proper transaction manager implementation
   56 | 	 * should throw an exception when unsupported settings are encountered.
   57 | 	 * <p>An exception to the above rule is the read-only flag, which should be
   58 | 	 * ignored if no explicit read-only mode is supported. Essentially, the
   59 | 	 * read-only flag is just a hint for potential optimization.
   60 | 	 * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   61 | 	 * describing propagation behavior, isolation level, timeout etc.
   62 | 	 * @return transaction status object representing the new or current transaction
   63 | 	 * @throws TransactionException in case of lookup, creation, or system errors
   64 | 	 * @throws IllegalTransactionStateException if the given transaction definition
   65 | 	 * cannot be executed (for example, if a currently active transaction is in
   66 | 	 * conflict with the specified propagation behavior)
   67 | 	 * @see TransactionDefinition#getPropagationBehavior
   68 | 	 * @see TransactionDefinition#getIsolationLevel
   69 | 	 * @see TransactionDefinition#getTimeout
   70 | 	 * @see TransactionDefinition#isReadOnly
   71 | 	 */
   72 | 	TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
   73 | 
   74 | 	/**
   75 | 	 * Commit the given transaction, with regard to its status. If the transaction
   76 | 	 * has been marked rollback-only programmatically, perform a rollback.
   77 | 	 * <p>If the transaction wasn't a new one, omit the commit for proper
   78 | 	 * participation in the surrounding transaction. If a previous transaction
   79 | 	 * has been suspended to be able to create a new one, resume the previous
   80 | 	 * transaction after committing the new one.
   81 | 	 * <p>Note that when the commit call completes, no matter if normally or
   82 | 	 * throwing an exception, the transaction must be fully completed and
   83 | 	 * cleaned up. No rollback call should be expected in such a case.
   84 | 	 * <p>Depending on the concrete transaction manager setup, {@code commit}
   85 | 	 * may propagate {@link org.springframework.dao.DataAccessException} as well,
   86 | 	 * either from before-commit flushes or from the actual commit step.
   87 | 	 * @param status object returned by the {@code getTransaction} method
   88 | 	 * @throws UnexpectedRollbackException in case of an unexpected rollback
   89 | 	 * that the transaction coordinator initiated
   90 | 	 * @throws HeuristicCompletionException in case of a transaction failure
   91 | 	 * caused by a heuristic decision on the side of the transaction coordinator
   92 | 	 * @throws TransactionSystemException in case of commit or system errors
   93 | 	 * (typically caused by fundamental resource failures)
   94 | 	 * @throws IllegalTransactionStateException if the given transaction
   95 | 	 * is already completed (that is, committed or rolled back)
   96 | 	 * @see TransactionStatus#setRollbackOnly
   97 | 	 */
   98 | 	void commit(TransactionStatus status) throws TransactionException;
   99 | 
  100 | 	/**
  101 | 	 * Perform a rollback of the given transaction.
  102 | 	 * <p>If the transaction wasn't a new one, just set it rollback-only for proper
  103 | 	 * participation in the surrounding transaction. If a previous transaction
  104 | 	 * has been suspended to be able to create a new one, resume the previous
  105 | 	 * transaction after rolling back the new one.
  106 | 	 * <p><b>Do not call rollback on a transaction if commit threw an exception.</b>
  107 | 	 * The transaction will already have been completed and cleaned up when commit
  108 | 	 * returns, even in case of a commit exception. Consequently, a rollback call
  109 | 	 * after commit failure will lead to an IllegalTransactionStateException.
  110 | 	 * <p>Depending on the concrete transaction manager setup, {@code rollback}
  111 | 	 * may propagate {@link org.springframework.dao.DataAccessException} as well.
  112 | 	 * @param status object returned by the {@code getTransaction} method
  113 | 	 * @throws TransactionSystemException in case of rollback or system errors
  114 | 	 * (typically caused by fundamental resource failures)
  115 | 	 * @throws IllegalTransactionStateException if the given transaction
  116 | 	 * is already completed (that is, committed or rolled back)
  117 | 	 */
  118 | 	void rollback(TransactionStatus status) throws TransactionException;
  119 | 
  120 | }
```
