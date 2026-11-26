<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/event/TransactionalApplicationListenerMethodAdapter.java -->
<!-- 正文引用：[[源码:TransactionalApplicationListenerMethodAdapter.java#L起-L止]] ；代码块头部：(src: TransactionalApplicationListenerMethodAdapter.java#L起-L止) -->

# TransactionalApplicationListenerMethodAdapter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/event/TransactionalApplicationListenerMethodAdapter.java` |
| 行数 | 105 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.868Z |

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
   17 | package org.springframework.transaction.event;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.List;
   21 | import java.util.concurrent.CopyOnWriteArrayList;
   22 | 
   23 | import org.springframework.context.ApplicationEvent;
   24 | import org.springframework.context.event.ApplicationListenerMethodAdapter;
   25 | import org.springframework.context.event.EventListener;
   26 | import org.springframework.context.event.GenericApplicationListener;
   27 | import org.springframework.core.annotation.AnnotatedElementUtils;
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * {@link GenericApplicationListener} adapter that delegates the processing of
   32 |  * an event to a {@link TransactionalEventListener} annotated method. Supports
   33 |  * the exact same features as any regular {@link EventListener} annotated method
   34 |  * but is aware of the transactional context of the event publisher.
   35 |  *
   36 |  * <p>Processing of {@link TransactionalEventListener} is enabled automatically
   37 |  * when Spring's transaction management is enabled. For other cases, registering
   38 |  * a bean of type {@link TransactionalEventListenerFactory} is required.
   39 |  *
   40 |  * @author Stephane Nicoll
   41 |  * @author Juergen Hoeller
   42 |  * @since 5.3
   43 |  * @see TransactionalEventListener
   44 |  * @see TransactionalApplicationListener
   45 |  * @see TransactionalApplicationListenerAdapter
   46 |  */
   47 | public class TransactionalApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter
   48 | 		implements TransactionalApplicationListener<ApplicationEvent> {
   49 | 
   50 | 	private final TransactionPhase transactionPhase;
   51 | 
   52 | 	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();
   53 | 
   54 | 
   55 | 	/**
   56 | 	 * Construct a new TransactionalApplicationListenerMethodAdapter.
   57 | 	 * @param beanName the name of the bean to invoke the listener method on
   58 | 	 * @param targetClass the target class that the method is declared on
   59 | 	 * @param method the listener method to invoke
   60 | 	 */
   61 | 	public TransactionalApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
   62 | 		super(beanName, targetClass, method);
   63 | 		TransactionalEventListener eventAnn =
   64 | 				AnnotatedElementUtils.findMergedAnnotation(getTargetMethod(), TransactionalEventListener.class);
   65 | 		if (eventAnn == null) {
   66 | 			throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
   67 | 		}
   68 | 		this.transactionPhase = eventAnn.phase();
   69 | 	}
   70 | 
   71 | 
   72 | 	@Override
   73 | 	public TransactionPhase getTransactionPhase() {
   74 | 		return this.transactionPhase;
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	public void addCallback(SynchronizationCallback callback) {
   79 | 		Assert.notNull(callback, "SynchronizationCallback must not be null");
   80 | 		this.callbacks.add(callback);
   81 | 	}
   82 | 
   83 | 
   84 | 	@Override
   85 | 	public void onApplicationEvent(ApplicationEvent event) {
   86 | 		if (TransactionalApplicationListenerSynchronization.register(event, this, this.callbacks)) {
   87 | 			if (logger.isDebugEnabled()) {
   88 | 				logger.debug("Registered transaction synchronization for " + event);
   89 | 			}
   90 | 		}
   91 | 		else if (isDefaultExecution()) {
   92 | 			if (getTransactionPhase() == TransactionPhase.AFTER_ROLLBACK && logger.isWarnEnabled()) {
   93 | 				logger.warn("Processing " + event + " as a fallback execution on AFTER_ROLLBACK phase");
   94 | 			}
   95 | 			processEvent(event);
   96 | 		}
   97 | 		else {
   98 | 			// No transactional event execution at all
   99 | 			if (logger.isDebugEnabled()) {
  100 | 				logger.debug("No transaction is active - skipping " + event);
  101 | 			}
  102 | 		}
  103 | 	}
  104 | 
  105 | }
```
