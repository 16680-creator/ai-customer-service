<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/TransactionStatus.java -->
<!-- 正文引用：[[源码:TransactionStatus.java#L起-L止]] ；代码块头部：(src: TransactionStatus.java#L起-L止) -->

# TransactionStatus.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/TransactionStatus.java` |
| 行数 | 71 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:59.495Z |

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
   19 | import java.io.Flushable;
   20 | 
   21 | /**
   22 |  * Representation of an ongoing {@link PlatformTransactionManager} transaction.
   23 |  * Extends the common {@link TransactionExecution} interface.
   24 |  *
   25 |  * <p>Transactional code can use this to retrieve status information,
   26 |  * and to programmatically request a rollback (instead of throwing
   27 |  * an exception that causes an implicit rollback).
   28 |  *
   29 |  * <p>Includes the {@link SavepointManager} interface to provide access
   30 |  * to savepoint management facilities. Note that savepoint management
   31 |  * is only available if supported by the underlying transaction manager.
   32 |  *
   33 |  * @author Juergen Hoeller
   34 |  * @since 27.03.2003
   35 |  * @see #setRollbackOnly()
   36 |  * @see PlatformTransactionManager#getTransaction
   37 |  * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
   38 |  * @see org.springframework.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
   39 |  */
   40 | public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {
   41 | 
   42 | 	/**
   43 | 	 * Return whether this transaction internally carries a savepoint,
   44 | 	 * that is, has been created as nested transaction based on a savepoint.
   45 | 	 * <p>This method is mainly here for diagnostic purposes, alongside
   46 | 	 * {@link #isNewTransaction()}. For programmatic handling of custom
   47 | 	 * savepoints, use the operations provided by {@link SavepointManager}.
   48 | 	 * <p>The default implementation returns {@code false}.
   49 | 	 * @see #isNewTransaction()
   50 | 	 * @see #createSavepoint()
   51 | 	 * @see #rollbackToSavepoint(Object)
   52 | 	 * @see #releaseSavepoint(Object)
   53 | 	 */
   54 | 	default boolean hasSavepoint() {
   55 | 		return false;
   56 | 	}
   57 | 
   58 | 	/**
   59 | 	 * Flush the underlying session to the datastore, if applicable:
   60 | 	 * for example, all affected Hibernate/JPA sessions.
   61 | 	 * <p>This is effectively just a hint and may be a no-op if the underlying
   62 | 	 * transaction manager does not have a flush concept. A flush signal may
   63 | 	 * get applied to the primary resource or to transaction synchronizations,
   64 | 	 * depending on the underlying resource.
   65 | 	 * <p>The default implementation is empty, considering flush as a no-op.
   66 | 	 */
   67 | 	@Override
   68 | 	default void flush() {
   69 | 	}
   70 | 
   71 | }
```
