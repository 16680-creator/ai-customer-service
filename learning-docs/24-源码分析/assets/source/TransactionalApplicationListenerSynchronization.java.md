<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/event/TransactionalApplicationListenerSynchronization.java -->
<!-- 正文引用：[[源码:TransactionalApplicationListenerSynchronization.java#L起-L止]] ；代码块头部：(src: TransactionalApplicationListenerSynchronization.java#L起-L止) -->

# TransactionalApplicationListenerSynchronization.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/event/TransactionalApplicationListenerSynchronization.java` |
| 行数 | 164 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:09:42.820Z |

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
   20 | 
   21 | import reactor.core.publisher.Mono;
   22 | 
   23 | import org.springframework.context.ApplicationEvent;
   24 | import org.springframework.core.Ordered;
   25 | import org.springframework.transaction.reactive.TransactionContext;
   26 | 
   27 | /**
   28 |  * {@code TransactionSynchronization} implementations for event processing with a
   29 |  * {@link TransactionalApplicationListener}.
   30 |  *
   31 |  * @author Juergen Hoeller
   32 |  * @since 5.3
   33 |  * @param <E> the specific {@code ApplicationEvent} subclass to listen to
   34 |  */
   35 | abstract class TransactionalApplicationListenerSynchronization<E extends ApplicationEvent> implements Ordered {
   36 | 
   37 | 	private final E event;
   38 | 
   39 | 	private final TransactionalApplicationListener<E> listener;
   40 | 
   41 | 	private final List<TransactionalApplicationListener.SynchronizationCallback> callbacks;
   42 | 
   43 | 
   44 | 	public TransactionalApplicationListenerSynchronization(E event, TransactionalApplicationListener<E> listener,
   45 | 			List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {
   46 | 
   47 | 		this.event = event;
   48 | 		this.listener = listener;
   49 | 		this.callbacks = callbacks;
   50 | 	}
   51 | 
   52 | 
   53 | 	@Override
   54 | 	public int getOrder() {
   55 | 		return this.listener.getOrder();
   56 | 	}
   57 | 
   58 | 	public TransactionPhase getTransactionPhase() {
   59 | 		return this.listener.getTransactionPhase();
   60 | 	}
   61 | 
   62 | 	public void processEventWithCallbacks() {
   63 | 		this.callbacks.forEach(callback -> callback.preProcessEvent(this.event));
   64 | 		try {
   65 | 			this.listener.processEvent(this.event);
   66 | 		}
   67 | 		catch (RuntimeException | Error ex) {
   68 | 			this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, ex));
   69 | 			throw ex;
   70 | 		}
   71 | 		this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, null));
   72 | 	}
   73 | 
   74 | 
   75 | 	public static <E extends ApplicationEvent> boolean register(
   76 | 			E event, TransactionalApplicationListener<E> listener,
   77 | 			List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {
   78 | 
   79 | 		if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive() &&
   80 | 				org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
   81 | 			org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
   82 | 					new PlatformSynchronization<>(event, listener, callbacks));
   83 | 			return true;
   84 | 		}
   85 | 		else if (event.getSource() instanceof TransactionContext txContext) {
   86 | 			org.springframework.transaction.reactive.TransactionSynchronizationManager rtsm =
   87 | 					new org.springframework.transaction.reactive.TransactionSynchronizationManager(txContext);
   88 | 			if (rtsm.isSynchronizationActive() && rtsm.isActualTransactionActive()) {
   89 | 				rtsm.registerSynchronization(new ReactiveSynchronization<>(event, listener, callbacks));
   90 | 				return true;
   91 | 			}
   92 | 		}
   93 | 		return false;
   94 | 	}
   95 | 
   96 | 
   97 | 	private static class PlatformSynchronization<AE extends ApplicationEvent>
   98 | 			extends TransactionalApplicationListenerSynchronization<AE>
   99 | 			implements org.springframework.transaction.support.TransactionSynchronization {
  100 | 
  101 | 		public PlatformSynchronization(AE event, TransactionalApplicationListener<AE> listener,
  102 | 				List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {
  103 | 
  104 | 			super(event, listener, callbacks);
  105 | 		}
  106 | 
  107 | 		@Override
  108 | 		public void beforeCommit(boolean readOnly) {
  109 | 			if (getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
  110 | 				processEventWithCallbacks();
  111 | 			}
  112 | 		}
  113 | 
  114 | 		@Override
  115 | 		public void afterCompletion(int status) {
  116 | 			TransactionPhase phase = getTransactionPhase();
  117 | 			if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
  118 | 				processEventWithCallbacks();
  119 | 			}
  120 | 			else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
  121 | 				processEventWithCallbacks();
  122 | 			}
  123 | 			else if (phase == TransactionPhase.AFTER_COMPLETION) {
  124 | 				processEventWithCallbacks();
  125 | 			}
  126 | 		}
  127 | 	}
  128 | 
  129 | 
  130 | 	private static class ReactiveSynchronization<AE extends ApplicationEvent>
  131 | 			extends TransactionalApplicationListenerSynchronization<AE>
  132 | 			implements org.springframework.transaction.reactive.TransactionSynchronization {
  133 | 
  134 | 		public ReactiveSynchronization(AE event, TransactionalApplicationListener<AE> listener,
  135 | 				List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {
  136 | 
  137 | 			super(event, listener, callbacks);
  138 | 		}
  139 | 
  140 | 		@Override
  141 | 		public Mono<Void> beforeCommit(boolean readOnly) {
  142 | 			if (getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
  143 | 				return Mono.fromRunnable(this::processEventWithCallbacks);
  144 | 			}
  145 | 			return Mono.empty();
  146 | 		}
  147 | 
  148 | 		@Override
  149 | 		public Mono<Void> afterCompletion(int status) {
  150 | 			TransactionPhase phase = getTransactionPhase();
  151 | 			if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
  152 | 				return Mono.fromRunnable(this::processEventWithCallbacks);
  153 | 			}
  154 | 			else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
  155 | 				return Mono.fromRunnable(this::processEventWithCallbacks);
  156 | 			}
  157 | 			else if (phase == TransactionPhase.AFTER_COMPLETION) {
  158 | 				return Mono.fromRunnable(this::processEventWithCallbacks);
  159 | 			}
  160 | 			return Mono.empty();
  161 | 		}
  162 | 	}
  163 | 
  164 | }
```
