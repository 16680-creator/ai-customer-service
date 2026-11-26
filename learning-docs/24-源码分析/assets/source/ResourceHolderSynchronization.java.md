<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/ResourceHolderSynchronization.java -->
<!-- 正文引用：[[源码:ResourceHolderSynchronization.java#L起-L止]] ；代码块头部：(src: ResourceHolderSynchronization.java#L起-L止) -->

# ResourceHolderSynchronization.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/ResourceHolderSynchronization.java` |
| 行数 | 185 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:15.310Z |

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
   19 | /**
   20 |  * {@link TransactionSynchronization} implementation that manages a
   21 |  * {@link ResourceHolder} bound through {@link TransactionSynchronizationManager}.
   22 |  *
   23 |  * @author Juergen Hoeller
   24 |  * @since 2.5.5
   25 |  * @param <H> the resource holder type
   26 |  * @param <K> the resource key type
   27 |  */
   28 | public abstract class ResourceHolderSynchronization<H extends ResourceHolder, K>
   29 | 		implements TransactionSynchronization {
   30 | 
   31 | 	private final H resourceHolder;
   32 | 
   33 | 	private final K resourceKey;
   34 | 
   35 | 	private volatile boolean holderActive = true;
   36 | 
   37 | 
   38 | 	/**
   39 | 	 * Create a new ResourceHolderSynchronization for the given holder.
   40 | 	 * @param resourceHolder the ResourceHolder to manage
   41 | 	 * @param resourceKey the key to bind the ResourceHolder for
   42 | 	 * @see TransactionSynchronizationManager#bindResource
   43 | 	 */
   44 | 	public ResourceHolderSynchronization(H resourceHolder, K resourceKey) {
   45 | 		this.resourceHolder = resourceHolder;
   46 | 		this.resourceKey = resourceKey;
   47 | 	}
   48 | 
   49 | 
   50 | 	@Override
   51 | 	public void suspend() {
   52 | 		if (this.holderActive) {
   53 | 			TransactionSynchronizationManager.unbindResource(this.resourceKey);
   54 | 		}
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public void resume() {
   59 | 		if (this.holderActive) {
   60 | 			TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
   61 | 		}
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	public void flush() {
   66 | 		flushResource(this.resourceHolder);
   67 | 	}
   68 | 
   69 | 	@Override
   70 | 	public void beforeCommit(boolean readOnly) {
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public void beforeCompletion() {
   75 | 		if (shouldUnbindAtCompletion()) {
   76 | 			TransactionSynchronizationManager.unbindResource(this.resourceKey);
   77 | 			this.holderActive = false;
   78 | 			if (shouldReleaseBeforeCompletion()) {
   79 | 				releaseResource(this.resourceHolder, this.resourceKey);
   80 | 			}
   81 | 		}
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public void afterCommit() {
   86 | 		if (!shouldReleaseBeforeCompletion()) {
   87 | 			processResourceAfterCommit(this.resourceHolder);
   88 | 		}
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public void afterCompletion(int status) {
   93 | 		if (shouldUnbindAtCompletion()) {
   94 | 			boolean releaseNecessary = false;
   95 | 			if (this.holderActive) {
   96 | 				// The thread-bound resource holder might not be available anymore,
   97 | 				// since afterCompletion might get called from a different thread.
   98 | 				this.holderActive = false;
   99 | 				TransactionSynchronizationManager.unbindResourceIfPossible(this.resourceKey);
  100 | 				this.resourceHolder.unbound();
  101 | 				releaseNecessary = true;
  102 | 			}
  103 | 			else {
  104 | 				releaseNecessary = shouldReleaseAfterCompletion(this.resourceHolder);
  105 | 			}
  106 | 			if (releaseNecessary) {
  107 | 				releaseResource(this.resourceHolder, this.resourceKey);
  108 | 			}
  109 | 		}
  110 | 		else {
  111 | 			// Probably a pre-bound resource...
  112 | 			cleanupResource(this.resourceHolder, this.resourceKey, (status == STATUS_COMMITTED));
  113 | 		}
  114 | 		this.resourceHolder.reset();
  115 | 	}
  116 | 
  117 | 
  118 | 	/**
  119 | 	 * Return whether this holder should be unbound at completion
  120 | 	 * (or should rather be left bound to the thread after the transaction).
  121 | 	 * <p>The default implementation returns {@code true}.
  122 | 	 */
  123 | 	protected boolean shouldUnbindAtCompletion() {
  124 | 		return true;
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Return whether this holder's resource should be released before
  129 | 	 * transaction completion ({@code true}) or rather after
  130 | 	 * transaction completion ({@code false}).
  131 | 	 * <p>Note that resources will only be released when they are
  132 | 	 * unbound from the thread ({@link #shouldUnbindAtCompletion()}).
  133 | 	 * <p>The default implementation returns {@code true}.
  134 | 	 * @see #releaseResource
  135 | 	 */
  136 | 	protected boolean shouldReleaseBeforeCompletion() {
  137 | 		return true;
  138 | 	}
  139 | 
  140 | 	/**
  141 | 	 * Return whether this holder's resource should be released after
  142 | 	 * transaction completion ({@code true}).
  143 | 	 * <p>The default implementation returns {@code !shouldReleaseBeforeCompletion()},
  144 | 	 * releasing after completion if no attempt was made before completion.
  145 | 	 * @see #releaseResource
  146 | 	 */
  147 | 	protected boolean shouldReleaseAfterCompletion(H resourceHolder) {
  148 | 		return !shouldReleaseBeforeCompletion();
  149 | 	}
  150 | 
  151 | 	/**
  152 | 	 * Flush callback for the given resource holder.
  153 | 	 * @param resourceHolder the resource holder to flush
  154 | 	 */
  155 | 	protected void flushResource(H resourceHolder) {
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * After-commit callback for the given resource holder.
  160 | 	 * Only called when the resource hasn't been released yet
  161 | 	 * ({@link #shouldReleaseBeforeCompletion()}).
  162 | 	 * @param resourceHolder the resource holder to process
  163 | 	 */
  164 | 	protected void processResourceAfterCommit(H resourceHolder) {
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Release the given resource (after it has been unbound from the thread).
  169 | 	 * @param resourceHolder the resource holder to process
  170 | 	 * @param resourceKey the key that the ResourceHolder was bound for
  171 | 	 */
  172 | 	protected void releaseResource(H resourceHolder, K resourceKey) {
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Perform a cleanup on the given resource (which is left bound to the thread).
  177 | 	 * @param resourceHolder the resource holder to process
  178 | 	 * @param resourceKey the key that the ResourceHolder was bound for
  179 | 	 * @param committed whether the transaction has committed ({@code true})
  180 | 	 * or rolled back ({@code false})
  181 | 	 */
  182 | 	protected void cleanupResource(H resourceHolder, K resourceKey, boolean committed) {
  183 | 	}
  184 | 
  185 | }
```
