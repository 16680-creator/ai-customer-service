<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/TransactionInterceptor.java -->
<!-- 正文引用：[[源码:TransactionInterceptor.java#L起-L止]] ；代码块头部：(src: TransactionInterceptor.java#L起-L止) -->

# TransactionInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/TransactionInterceptor.java` |
| 行数 | 182 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:12.495Z |

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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.ObjectInputStream;
   21 | import java.io.ObjectOutputStream;
   22 | import java.io.Serializable;
   23 | import java.util.Properties;
   24 | 
   25 | import org.aopalliance.intercept.MethodInterceptor;
   26 | import org.aopalliance.intercept.MethodInvocation;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.aop.support.AopUtils;
   30 | import org.springframework.beans.factory.BeanFactory;
   31 | import org.springframework.context.ApplicationEventPublisher;
   32 | import org.springframework.context.ApplicationEventPublisherAware;
   33 | import org.springframework.transaction.PlatformTransactionManager;
   34 | import org.springframework.transaction.TransactionExecution;
   35 | import org.springframework.transaction.TransactionManager;
   36 | 
   37 | /**
   38 |  * AOP Alliance MethodInterceptor for declarative transaction
   39 |  * management using the common Spring transaction infrastructure
   40 |  * ({@link org.springframework.transaction.PlatformTransactionManager}/
   41 |  * {@link org.springframework.transaction.ReactiveTransactionManager}).
   42 |  *
   43 |  * <p>Derives from the {@link TransactionAspectSupport} class which
   44 |  * contains the integration with Spring's underlying transaction API.
   45 |  * TransactionInterceptor simply calls the relevant superclass methods
   46 |  * such as {@link #invokeWithinTransaction} in the correct order.
   47 |  *
   48 |  * <p>TransactionInterceptors are thread-safe.
   49 |  *
   50 |  * @author Rod Johnson
   51 |  * @author Juergen Hoeller
   52 |  * @author Sebastien Deleuze
   53 |  * @see TransactionProxyFactoryBean
   54 |  * @see org.springframework.aop.framework.ProxyFactoryBean
   55 |  * @see org.springframework.aop.framework.ProxyFactory
   56 |  */
   57 | @SuppressWarnings("serial")
   58 | public class TransactionInterceptor extends TransactionAspectSupport
   59 | 		implements MethodInterceptor, ApplicationEventPublisherAware, Serializable {
   60 | 
   61 | 	private @Nullable ApplicationEventPublisher applicationEventPublisher;
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Create a new TransactionInterceptor.
   66 | 	 * <p>Transaction manager and transaction attributes still need to be set.
   67 | 	 * @see #setTransactionManager
   68 | 	 * @see #setTransactionAttributes(java.util.Properties)
   69 | 	 * @see #setTransactionAttributeSource(TransactionAttributeSource)
   70 | 	 */
   71 | 	public TransactionInterceptor() {
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * Create a new TransactionInterceptor.
   76 | 	 * @param tm the default transaction manager to perform the actual transaction management
   77 | 	 * @param tas the attribute source to be used to find transaction attributes
   78 | 	 * @since 5.2.5
   79 | 	 * @see #setTransactionManager
   80 | 	 * @see #setTransactionAttributeSource
   81 | 	 */
   82 | 	public TransactionInterceptor(TransactionManager tm, TransactionAttributeSource tas) {
   83 | 		setTransactionManager(tm);
   84 | 		setTransactionAttributeSource(tas);
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Create a new TransactionInterceptor.
   89 | 	 * @param ptm the default transaction manager to perform the actual transaction management
   90 | 	 * @param tas the attribute source to be used to find transaction attributes
   91 | 	 * @see #setTransactionManager
   92 | 	 * @see #setTransactionAttributeSource
   93 | 	 * @deprecated in favor of
   94 | 	 * {@link #TransactionInterceptor(TransactionManager, TransactionAttributeSource)}
   95 | 	 */
   96 | 	@Deprecated(since = "5.2.5")
   97 | 	public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
   98 | 		setTransactionManager(ptm);
   99 | 		setTransactionAttributeSource(tas);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Create a new TransactionInterceptor.
  104 | 	 * @param ptm the default transaction manager to perform the actual transaction management
  105 | 	 * @param attributes the transaction attributes in properties format
  106 | 	 * @see #setTransactionManager
  107 | 	 * @see #setTransactionAttributes(java.util.Properties)
  108 | 	 * @deprecated in favor of {@link #setTransactionAttributes(Properties)}
  109 | 	 */
  110 | 	@Deprecated(since = "5.2.5")
  111 | 	public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
  112 | 		setTransactionManager(ptm);
  113 | 		setTransactionAttributes(attributes);
  114 | 	}
  115 | 
  116 | 
  117 | 	@Override
  118 | 	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
  119 | 		this.applicationEventPublisher = applicationEventPublisher;
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
  124 | 		// Work out the target class: may be {@code null}.
  125 | 		// The TransactionAttributeSource should be passed the target class
  126 | 		// as well as the method, which may be from an interface.
  127 | 		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
  128 | 
  129 | 		// Adapt to TransactionAspectSupport's invokeWithinTransaction...
  130 | 		return invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
  131 | 			@Override
  132 | 			public @Nullable Object proceedWithInvocation() throws Throwable {
  133 | 				return invocation.proceed();
  134 | 			}
  135 | 			@Override
  136 | 			public void onRollback(Throwable failure, TransactionExecution execution) {
  137 | 				MethodRollbackEvent event = new MethodRollbackEvent(invocation, failure, execution);
  138 | 				logger.trace(event, failure);
  139 | 				if (applicationEventPublisher != null) {
  140 | 					try {
  141 | 						applicationEventPublisher.publishEvent(event);
  142 | 					}
  143 | 					catch (Throwable ex) {
  144 | 						if (logger.isWarnEnabled()) {
  145 | 							logger.warn("Failed to publish " + event, ex);
  146 | 						}
  147 | 					}
  148 | 				}
  149 | 			}
  150 | 		});
  151 | 	}
  152 | 
  153 | 
  154 | 	//---------------------------------------------------------------------
  155 | 	// Serialization support
  156 | 	//---------------------------------------------------------------------
  157 | 
  158 | 	private void writeObject(ObjectOutputStream oos) throws IOException {
  159 | 		// Rely on default serialization, although this class itself doesn't carry state anyway...
  160 | 		oos.defaultWriteObject();
  161 | 
  162 | 		// Deserialize superclass fields.
  163 | 		oos.writeObject(getTransactionManagerBeanName());
  164 | 		oos.writeObject(getTransactionManager());
  165 | 		oos.writeObject(getTransactionAttributeSource());
  166 | 		oos.writeObject(getBeanFactory());
  167 | 	}
  168 | 
  169 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
  170 | 		// Rely on default serialization, although this class itself doesn't carry state anyway...
  171 | 		ois.defaultReadObject();
  172 | 
  173 | 		// Serialize all relevant superclass fields.
  174 | 		// Superclass can't implement Serializable because it also serves as base class
  175 | 		// for AspectJ aspects (which are not allowed to implement Serializable)!
  176 | 		setTransactionManagerBeanName((String) ois.readObject());
  177 | 		setTransactionManager((PlatformTransactionManager) ois.readObject());
  178 | 		setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
  179 | 		setBeanFactory((BeanFactory) ois.readObject());
  180 | 	}
  181 | 
  182 | }
```
