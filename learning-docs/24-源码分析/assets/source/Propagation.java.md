<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/Propagation.java -->
<!-- 正文引用：[[源码:Propagation.java#L起-L止]] ；代码块头部：(src: Propagation.java#L起-L止) -->

# Propagation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/Propagation.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.639Z |

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
   17 | package org.springframework.transaction.annotation;
   18 | 
   19 | import org.springframework.transaction.TransactionDefinition;
   20 | 
   21 | /**
   22 |  * Enumeration that represents transaction propagation behaviors for use
   23 |  * with the {@link Transactional} annotation, corresponding to the
   24 |  * {@link TransactionDefinition} interface.
   25 |  *
   26 |  * @author Colin Sampaleanu
   27 |  * @author Juergen Hoeller
   28 |  * @since 1.2
   29 |  */
   30 | public enum Propagation {
   31 | 
   32 | 	/**
   33 | 	 * Support a current transaction, create a new one if none exists.
   34 | 	 * Analogous to EJB transaction attribute of the same name.
   35 | 	 * <p>This is the default setting of a transaction annotation.
   36 | 	 */
   37 | 	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
   38 | 
   39 | 	/**
   40 | 	 * Support a current transaction, execute non-transactionally if none exists.
   41 | 	 * Analogous to EJB transaction attribute of the same name.
   42 | 	 * <p>Note: For transaction managers with transaction synchronization,
   43 | 	 * {@code SUPPORTS} is slightly different from no transaction at all,
   44 | 	 * as it defines a transaction scope that synchronization will apply for.
   45 | 	 * As a consequence, the same resources (JDBC Connection, Hibernate Session, etc)
   46 | 	 * will be shared for the entire specified scope. Note that this depends on
   47 | 	 * the actual synchronization configuration of the transaction manager.
   48 | 	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
   49 | 	 */
   50 | 	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),
   51 | 
   52 | 	/**
   53 | 	 * Support a current transaction, throw an exception if none exists.
   54 | 	 * Analogous to EJB transaction attribute of the same name.
   55 | 	 */
   56 | 	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),
   57 | 
   58 | 	/**
   59 | 	 * Create a new transaction, and suspend the current transaction if one exists.
   60 | 	 * Analogous to the EJB transaction attribute of the same name.
   61 | 	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
   62 | 	 * on all transaction managers. This in particular applies to
   63 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
   64 | 	 * which requires the {@code jakarta.transaction.TransactionManager} to be
   65 | 	 * made available to it (which is server-specific in standard Jakarta EE).
   66 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
   67 | 	 */
   68 | 	REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
   69 | 
   70 | 	/**
   71 | 	 * Execute non-transactionally, suspend the current transaction if one exists.
   72 | 	 * Analogous to EJB transaction attribute of the same name.
   73 | 	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
   74 | 	 * on all transaction managers. This in particular applies to
   75 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
   76 | 	 * which requires the {@code jakarta.transaction.TransactionManager} to be
   77 | 	 * made available to it (which is server-specific in standard Jakarta EE).
   78 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
   79 | 	 */
   80 | 	NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),
   81 | 
   82 | 	/**
   83 | 	 * Execute non-transactionally, throw an exception if a transaction exists.
   84 | 	 * Analogous to EJB transaction attribute of the same name.
   85 | 	 */
   86 | 	NEVER(TransactionDefinition.PROPAGATION_NEVER),
   87 | 
   88 | 	/**
   89 | 	 * Execute within a nested transaction if a current transaction exists,
   90 | 	 * behave like {@code REQUIRED} otherwise. There is no analogous feature in EJB.
   91 | 	 * <p>Note: Actual creation of a nested transaction will only work on specific
   92 | 	 * transaction managers. Out of the box, this only applies to the JDBC
   93 | 	 * DataSourceTransactionManager. Some JTA providers might support nested
   94 | 	 * transactions as well.
   95 | 	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
   96 | 	 */
   97 | 	NESTED(TransactionDefinition.PROPAGATION_NESTED);
   98 | 
   99 | 
  100 | 	private final int value;
  101 | 
  102 | 
  103 | 	Propagation(int value) {
  104 | 		this.value = value;
  105 | 	}
  106 | 
  107 | 	public int value() {
  108 | 		return this.value;
  109 | 	}
  110 | 
  111 | }
```
