<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/event/TransactionalApplicationListenerAdapter.java -->
<!-- 正文引用：[[源码:TransactionalApplicationListenerAdapter.java#L起-L止]] ；代码块头部：(src: TransactionalApplicationListenerAdapter.java#L起-L止) -->

# TransactionalApplicationListenerAdapter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/event/TransactionalApplicationListenerAdapter.java` |
| 行数 | 133 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:09:43.318Z |

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
   19 | import java.util.List;
   20 | import java.util.concurrent.CopyOnWriteArrayList;
   21 | 
   22 | import org.springframework.context.ApplicationEvent;
   23 | import org.springframework.context.ApplicationListener;
   24 | import org.springframework.core.Ordered;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * {@link TransactionalApplicationListener} adapter that delegates the processing of
   29 |  * an event to a target {@link ApplicationListener} instance. Supports the exact
   30 |  * same features as any regular {@link ApplicationListener} but is aware of the
   31 |  * transactional context of the event publisher.
   32 |  *
   33 |  * <p>For simple {@link org.springframework.context.PayloadApplicationEvent} handling,
   34 |  * consider the {@link TransactionalApplicationListener#forPayload} factory methods
   35 |  * as a convenient alternative to custom usage of this adapter class.
   36 |  *
   37 |  * @author Juergen Hoeller
   38 |  * @since 5.3
   39 |  * @param <E> the specific {@code ApplicationEvent} subclass to listen to
   40 |  * @see TransactionalApplicationListener
   41 |  * @see TransactionalEventListener
   42 |  * @see TransactionalApplicationListenerMethodAdapter
   43 |  */
   44 | public class TransactionalApplicationListenerAdapter<E extends ApplicationEvent>
   45 | 		implements TransactionalApplicationListener<E>, Ordered {
   46 | 
   47 | 	private final ApplicationListener<E> targetListener;
   48 | 
   49 | 	private int order = Ordered.LOWEST_PRECEDENCE;
   50 | 
   51 | 	private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;
   52 | 
   53 | 	private String listenerId = "";
   54 | 
   55 | 	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();
   56 | 
   57 | 
   58 | 	/**
   59 | 	 * Construct a new TransactionalApplicationListenerAdapter.
   60 | 	 * @param targetListener the actual listener to invoke in the specified transaction phase
   61 | 	 * @see #setTransactionPhase
   62 | 	 * @see TransactionalApplicationListener#forPayload
   63 | 	 */
   64 | 	public TransactionalApplicationListenerAdapter(ApplicationListener<E> targetListener) {
   65 | 		this.targetListener = targetListener;
   66 | 	}
   67 | 
   68 | 
   69 | 	/**
   70 | 	 * Specify the synchronization order for the listener.
   71 | 	 */
   72 | 	public void setOrder(int order) {
   73 | 		this.order = order;
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Return the synchronization order for the listener.
   78 | 	 */
   79 | 	@Override
   80 | 	public int getOrder() {
   81 | 		return this.order;
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * Specify the transaction phase to invoke the listener in.
   86 | 	 * <p>The default is {@link TransactionPhase#AFTER_COMMIT}.
   87 | 	 */
   88 | 	public void setTransactionPhase(TransactionPhase transactionPhase) {
   89 | 		this.transactionPhase = transactionPhase;
   90 | 	}
   91 | 
   92 | 	/**
   93 | 	 * Return the transaction phase to invoke the listener in.
   94 | 	 */
   95 | 	@Override
   96 | 	public TransactionPhase getTransactionPhase() {
   97 | 		return this.transactionPhase;
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Specify an id to identify the listener with.
  102 | 	 * <p>The default is an empty String.
  103 | 	 */
  104 | 	public void setListenerId(String listenerId) {
  105 | 		this.listenerId = listenerId;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Return an id to identify the listener with.
  110 | 	 */
  111 | 	@Override
  112 | 	public String getListenerId() {
  113 | 		return this.listenerId;
  114 | 	}
  115 | 
  116 | 	@Override
  117 | 	public void addCallback(SynchronizationCallback callback) {
  118 | 		Assert.notNull(callback, "SynchronizationCallback must not be null");
  119 | 		this.callbacks.add(callback);
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public void processEvent(E event) {
  124 | 		this.targetListener.onApplicationEvent(event);
  125 | 	}
  126 | 
  127 | 
  128 | 	@Override
  129 | 	public void onApplicationEvent(E event) {
  130 | 		TransactionalApplicationListenerSynchronization.register(event, this, this.callbacks);
  131 | 	}
  132 | 
  133 | }
```
