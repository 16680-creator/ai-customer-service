<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/TransactionTemplate.java -->
<!-- 正文引用：[[源码:TransactionTemplate.java#L起-L止]] ；代码块头部：(src: TransactionTemplate.java#L起-L止) -->

# TransactionTemplate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/TransactionTemplate.java` |
| 行数 | 185 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:58.158Z |

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
   19 | import java.lang.reflect.UndeclaredThrowableException;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.beans.factory.InitializingBean;
   26 | import org.springframework.transaction.PlatformTransactionManager;
   27 | import org.springframework.transaction.TransactionDefinition;
   28 | import org.springframework.transaction.TransactionException;
   29 | import org.springframework.transaction.TransactionStatus;
   30 | import org.springframework.transaction.TransactionSystemException;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Template class that simplifies programmatic transaction demarcation and
   35 |  * transaction exception handling.
   36 |  *
   37 |  * <p>The central method is {@link #execute}, supporting transactional code that
   38 |  * implements the {@link TransactionCallback} interface. This template handles
   39 |  * the transaction lifecycle and possible exceptions such that neither the
   40 |  * TransactionCallback implementation nor the calling code needs to explicitly
   41 |  * handle transactions.
   42 |  *
   43 |  * <p>Typical usage: Allows for writing low-level data access objects that use
   44 |  * resources such as JDBC DataSources but are not transaction-aware themselves.
   45 |  * Instead, they can implicitly participate in transactions handled by higher-level
   46 |  * application services utilizing this class, making calls to the low-level
   47 |  * services via an inner-class callback object.
   48 |  *
   49 |  * <p>Can be used within a service implementation via direct instantiation with
   50 |  * a transaction manager reference, or get prepared in an application context
   51 |  * and passed to services as bean reference. Note: The transaction manager should
   52 |  * always be configured as bean in the application context: in the first case given
   53 |  * to the service directly, in the second case given to the prepared template.
   54 |  *
   55 |  * <p>Supports setting the propagation behavior and the isolation level by name,
   56 |  * for convenient configuration in context definitions.
   57 |  *
   58 |  * @author Juergen Hoeller
   59 |  * @since 17.03.2003
   60 |  * @see #execute
   61 |  * @see #setTransactionManager
   62 |  * @see org.springframework.transaction.PlatformTransactionManager
   63 |  */
   64 | @SuppressWarnings("serial")
   65 | public class TransactionTemplate extends DefaultTransactionDefinition
   66 | 		implements TransactionOperations, InitializingBean {
   67 | 
   68 | 	/** Logger available to subclasses. */
   69 | 	protected final Log logger = LogFactory.getLog(getClass());
   70 | 
   71 | 	private @Nullable PlatformTransactionManager transactionManager;
   72 | 
   73 | 
   74 | 	/**
   75 | 	 * Construct a new TransactionTemplate for bean usage.
   76 | 	 * <p>Note: The PlatformTransactionManager needs to be set before
   77 | 	 * any {@code execute} calls.
   78 | 	 * @see #setTransactionManager
   79 | 	 */
   80 | 	public TransactionTemplate() {
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Construct a new TransactionTemplate using the given transaction manager.
   85 | 	 * @param transactionManager the transaction management strategy to be used
   86 | 	 */
   87 | 	public TransactionTemplate(PlatformTransactionManager transactionManager) {
   88 | 		this.transactionManager = transactionManager;
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Construct a new TransactionTemplate using the given transaction manager,
   93 | 	 * taking its default settings from the given transaction definition.
   94 | 	 * @param transactionManager the transaction management strategy to be used
   95 | 	 * @param transactionDefinition the transaction definition to copy the
   96 | 	 * default settings from. Local properties can still be set to change values.
   97 | 	 */
   98 | 	public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
   99 | 		super(transactionDefinition);
  100 | 		this.transactionManager = transactionManager;
  101 | 	}
  102 | 
  103 | 
  104 | 	/**
  105 | 	 * Set the transaction management strategy to be used.
  106 | 	 */
  107 | 	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
  108 | 		this.transactionManager = transactionManager;
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Return the transaction management strategy to be used.
  113 | 	 */
  114 | 	public @Nullable PlatformTransactionManager getTransactionManager() {
  115 | 		return this.transactionManager;
  116 | 	}
  117 | 
  118 | 	@Override
  119 | 	public void afterPropertiesSet() {
  120 | 		if (this.transactionManager == null) {
  121 | 			throw new IllegalArgumentException("Property 'transactionManager' is required");
  122 | 		}
  123 | 	}
  124 | 
  125 | 
  126 | 	@Override
  127 | 	public <T extends @Nullable Object> T execute(TransactionCallback<T> action) throws TransactionException {
  128 | 		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
  129 | 
  130 | 		if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager cpptm) {
  131 | 			return cpptm.execute(this, action);
  132 | 		}
  133 | 		else {
  134 | 			TransactionStatus status = this.transactionManager.getTransaction(this);
  135 | 			T result;
  136 | 			try {
  137 | 				result = action.doInTransaction(status);
  138 | 			}
  139 | 			catch (RuntimeException | Error ex) {
  140 | 				// Transactional code threw application exception -> rollback
  141 | 				rollbackOnException(status, ex);
  142 | 				throw ex;
  143 | 			}
  144 | 			catch (Throwable ex) {
  145 | 				// Transactional code threw unexpected exception -> rollback
  146 | 				rollbackOnException(status, ex);
  147 | 				throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
  148 | 			}
  149 | 			this.transactionManager.commit(status);
  150 | 			return result;
  151 | 		}
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Perform a rollback, handling rollback exceptions properly.
  156 | 	 * @param status object representing the transaction
  157 | 	 * @param ex the thrown application exception or error
  158 | 	 * @throws TransactionException in case of a rollback error
  159 | 	 */
  160 | 	private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
  161 | 		Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
  162 | 
  163 | 		logger.debug("Initiating transaction rollback on application exception", ex);
  164 | 		try {
  165 | 			this.transactionManager.rollback(status);
  166 | 		}
  167 | 		catch (TransactionSystemException ex2) {
  168 | 			logger.error("Application exception overridden by rollback exception", ex);
  169 | 			ex2.initApplicationException(ex);
  170 | 			throw ex2;
  171 | 		}
  172 | 		catch (RuntimeException | Error ex2) {
  173 | 			logger.error("Application exception overridden by rollback exception", ex);
  174 | 			throw ex2;
  175 | 		}
  176 | 	}
  177 | 
  178 | 
  179 | 	@Override
  180 | 	public boolean equals(@Nullable Object other) {
  181 | 		return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate template) ||
  182 | 				getTransactionManager() == template.getTransactionManager())));
  183 | 	}
  184 | 
  185 | }
```
