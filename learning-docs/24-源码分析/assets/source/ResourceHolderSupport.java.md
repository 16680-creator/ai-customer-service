<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/ResourceHolderSupport.java -->
<!-- 正文引用：[[源码:ResourceHolderSupport.java#L起-L止]] ；代码块头部：(src: ResourceHolderSupport.java#L起-L止) -->

# ResourceHolderSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/ResourceHolderSupport.java` |
| 行数 | 209 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:58.831Z |

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
   19 | import java.util.Date;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.transaction.TransactionTimedOutException;
   24 | 
   25 | /**
   26 |  * Convenient base class for resource holders.
   27 |  *
   28 |  * <p>Features rollback-only support for participating transactions.
   29 |  * Can expire after a certain number of seconds or milliseconds
   30 |  * in order to determine a transactional timeout.
   31 |  *
   32 |  * @author Juergen Hoeller
   33 |  * @since 02.02.2004
   34 |  * @see org.springframework.jdbc.datasource.DataSourceTransactionManager#doBegin
   35 |  * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
   36 |  */
   37 | public abstract class ResourceHolderSupport implements ResourceHolder {
   38 | 
   39 | 	private boolean synchronizedWithTransaction = false;
   40 | 
   41 | 	private boolean rollbackOnly = false;
   42 | 
   43 | 	private @Nullable Date deadline;
   44 | 
   45 | 	private int referenceCount = 0;
   46 | 
   47 | 	private boolean isVoid = false;
   48 | 
   49 | 
   50 | 	/**
   51 | 	 * Mark the resource as synchronized with a transaction.
   52 | 	 */
   53 | 	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
   54 | 		this.synchronizedWithTransaction = synchronizedWithTransaction;
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Return whether the resource is synchronized with a transaction.
   59 | 	 */
   60 | 	public boolean isSynchronizedWithTransaction() {
   61 | 		return this.synchronizedWithTransaction;
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Mark the resource transaction as rollback-only.
   66 | 	 */
   67 | 	public void setRollbackOnly() {
   68 | 		this.rollbackOnly = true;
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Reset the rollback-only status for this resource transaction.
   73 | 	 * <p>Only really intended to be called after custom rollback steps which
   74 | 	 * keep the original resource in action, for example, in case of a savepoint.
   75 | 	 * @since 5.0
   76 | 	 * @see org.springframework.transaction.SavepointManager#rollbackToSavepoint
   77 | 	 */
   78 | 	public void resetRollbackOnly() {
   79 | 		this.rollbackOnly = false;
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Return whether the resource transaction is marked as rollback-only.
   84 | 	 */
   85 | 	public boolean isRollbackOnly() {
   86 | 		return this.rollbackOnly;
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Set the timeout for this object in seconds.
   91 | 	 * @param seconds number of seconds until expiration
   92 | 	 */
   93 | 	public void setTimeoutInSeconds(int seconds) {
   94 | 		setTimeoutInMillis(seconds * 1000L);
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Set the timeout for this object in milliseconds.
   99 | 	 * @param millis number of milliseconds until expiration
  100 | 	 */
  101 | 	public void setTimeoutInMillis(long millis) {
  102 | 		this.deadline = new Date(System.currentTimeMillis() + millis);
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Return whether this object has an associated timeout.
  107 | 	 */
  108 | 	public boolean hasTimeout() {
  109 | 		return (this.deadline != null);
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return the expiration deadline of this object.
  114 | 	 * @return the deadline as Date object
  115 | 	 */
  116 | 	public @Nullable Date getDeadline() {
  117 | 		return this.deadline;
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Return the time to live for this object in seconds.
  122 | 	 * Rounds up eagerly, for example, 9.00001 still to 10.
  123 | 	 * @return number of seconds until expiration
  124 | 	 * @throws TransactionTimedOutException if the deadline has already been reached
  125 | 	 */
  126 | 	public int getTimeToLiveInSeconds() {
  127 | 		double diff = ((double) getTimeToLiveInMillis()) / 1000;
  128 | 		int secs = (int) Math.ceil(diff);
  129 | 		checkTransactionTimeout(secs <= 0);
  130 | 		return secs;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Return the time to live for this object in milliseconds.
  135 | 	 * @return number of milliseconds until expiration
  136 | 	 * @throws TransactionTimedOutException if the deadline has already been reached
  137 | 	 */
  138 | 	public long getTimeToLiveInMillis() throws TransactionTimedOutException{
  139 | 		if (this.deadline == null) {
  140 | 			throw new IllegalStateException("No timeout specified for this resource holder");
  141 | 		}
  142 | 		long timeToLive = this.deadline.getTime() - System.currentTimeMillis();
  143 | 		checkTransactionTimeout(timeToLive <= 0);
  144 | 		return timeToLive;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Set the transaction rollback-only if the deadline has been reached,
  149 | 	 * and throw a TransactionTimedOutException.
  150 | 	 */
  151 | 	private void checkTransactionTimeout(boolean deadlineReached) throws TransactionTimedOutException {
  152 | 		if (deadlineReached) {
  153 | 			setRollbackOnly();
  154 | 			throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
  155 | 		}
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Increase the reference count by one because the holder has been requested
  160 | 	 * (i.e. someone requested the resource held by it).
  161 | 	 */
  162 | 	public void requested() {
  163 | 		this.referenceCount++;
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Decrease the reference count by one because the holder has been released
  168 | 	 * (i.e. someone released the resource held by it).
  169 | 	 */
  170 | 	public void released() {
  171 | 		this.referenceCount--;
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Return whether there are still open references to this holder.
  176 | 	 */
  177 | 	public boolean isOpen() {
  178 | 		return (this.referenceCount > 0);
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Clear the transactional state of this resource holder.
  183 | 	 */
  184 | 	public void clear() {
  185 | 		this.synchronizedWithTransaction = false;
  186 | 		this.rollbackOnly = false;
  187 | 		this.deadline = null;
  188 | 	}
  189 | 
  190 | 	/**
  191 | 	 * Reset this resource holder - transactional state as well as reference count.
  192 | 	 */
  193 | 	@Override
  194 | 	public void reset() {
  195 | 		clear();
  196 | 		this.referenceCount = 0;
  197 | 	}
  198 | 
  199 | 	@Override
  200 | 	public void unbound() {
  201 | 		this.isVoid = true;
  202 | 	}
  203 | 
  204 | 	@Override
  205 | 	public boolean isVoid() {
  206 | 		return this.isVoid;
  207 | 	}
  208 | 
  209 | }
```
