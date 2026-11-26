<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/event/TransactionalEventListener.java -->
<!-- 正文引用：[[源码:TransactionalEventListener.java#L起-L止]] ；代码块头部：(src: TransactionalEventListener.java#L起-L止) -->

# TransactionalEventListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/event/TransactionalEventListener.java` |
| 行数 | 122 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.757Z |

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
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.context.event.EventListener;
   26 | import org.springframework.core.annotation.AliasFor;
   27 | 
   28 | /**
   29 |  * An {@link EventListener} that is invoked according to a {@link TransactionPhase}.
   30 |  * This is an annotation-based equivalent of {@link TransactionalApplicationListener}.
   31 |  *
   32 |  * <p>If the event is not published within an active transaction, the event is discarded
   33 |  * unless the {@link #fallbackExecution} flag is explicitly set. If a transaction is
   34 |  * running, the event is handled according to its {@code TransactionPhase}.
   35 |  *
   36 |  * <p>Adding {@link org.springframework.core.annotation.Order @Order} to your annotated
   37 |  * method allows you to prioritize that listener amongst other listeners running before
   38 |  * or after transaction completion.
   39 |  *
   40 |  * <p>As of 6.1, transactional event listeners can work with thread-bound transactions managed
   41 |  * by a {@link org.springframework.transaction.PlatformTransactionManager} as well as reactive
   42 |  * transactions managed by a {@link org.springframework.transaction.ReactiveTransactionManager}.
   43 |  * For the former, listeners are guaranteed to see the current thread-bound transaction.
   44 |  * Since the latter uses the Reactor context instead of thread-local variables, the transaction
   45 |  * context needs to be included in the published event instance as the event source:
   46 |  * see {@link org.springframework.transaction.reactive.TransactionalEventPublisher}.
   47 |  *
   48 |  * <p><strong>WARNING:</strong> if the {@code TransactionPhase} is set to
   49 |  * {@link TransactionPhase#AFTER_COMMIT AFTER_COMMIT} (the default),
   50 |  * {@link TransactionPhase#AFTER_ROLLBACK AFTER_ROLLBACK}, or
   51 |  * {@link TransactionPhase#AFTER_COMPLETION AFTER_COMPLETION}, the transaction will
   52 |  * have been committed or rolled back already, but the transactional resources might
   53 |  * still be active and accessible. As a consequence, any data access code triggered
   54 |  * at this point will still "participate" in the original transaction, but changes
   55 |  * will not be committed to the transactional resource. See
   56 |  * {@link org.springframework.transaction.support.TransactionSynchronization#afterCompletion(int)
   57 |  * TransactionSynchronization.afterCompletion(int)} for details.
   58 |  *
   59 |  * @author Stephane Nicoll
   60 |  * @author Sam Brannen
   61 |  * @author Oliver Drotbohm
   62 |  * @since 4.2
   63 |  * @see TransactionalApplicationListener
   64 |  * @see TransactionalApplicationListenerMethodAdapter
   65 |  */
   66 | @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
   67 | @Retention(RetentionPolicy.RUNTIME)
   68 | @Documented
   69 | @EventListener
   70 | public @interface TransactionalEventListener {
   71 | 
   72 | 	/**
   73 | 	 * Phase to bind the handling of an event to.
   74 | 	 * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
   75 | 	 * <p>If no transaction is in progress, the event is not processed at
   76 | 	 * all unless {@link #fallbackExecution} has been enabled explicitly.
   77 | 	 */
   78 | 	TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;
   79 | 
   80 | 	/**
   81 | 	 * Alias for {@link #classes}.
   82 | 	 */
   83 | 	@AliasFor(annotation = EventListener.class, attribute = "classes")
   84 | 	Class<?>[] value() default {};
   85 | 
   86 | 	/**
   87 | 	 * The event classes that this listener handles.
   88 | 	 * <p>If this attribute is specified with a single value, the annotated
   89 | 	 * method may optionally accept a single parameter. However, if this
   90 | 	 * attribute is specified with multiple values, the annotated method
   91 | 	 * must <em>not</em> declare any parameters.
   92 | 	 */
   93 | 	@AliasFor(annotation = EventListener.class, attribute = "classes")
   94 | 	Class<?>[] classes() default {};
   95 | 
   96 | 	/**
   97 | 	 * Spring Expression Language (SpEL) attribute used for making the event
   98 | 	 * handling conditional.
   99 | 	 * <p>The default is {@code ""}, meaning the event is always handled.
  100 | 	 * @see EventListener#condition
  101 | 	 */
  102 | 	@AliasFor(annotation = EventListener.class, attribute = "condition")
  103 | 	String condition() default "";
  104 | 
  105 | 	/**
  106 | 	 * Whether the event should be handled if no transaction is running.
  107 | 	 * @see EventListener#defaultExecution()
  108 | 	 */
  109 | 	@AliasFor(annotation = EventListener.class, attribute = "defaultExecution")
  110 | 	boolean fallbackExecution() default false;
  111 | 
  112 | 	/**
  113 | 	 * An optional identifier for the listener, defaulting to the fully-qualified
  114 | 	 * signature of the declaring method (for example, "mypackage.MyClass.myMethod()").
  115 | 	 * @since 5.3
  116 | 	 * @see EventListener#id
  117 | 	 * @see TransactionalApplicationListener#getListenerId()
  118 | 	 */
  119 | 	@AliasFor(annotation = EventListener.class, attribute = "id")
  120 | 	String id() default "";
  121 | 
  122 | }
```
