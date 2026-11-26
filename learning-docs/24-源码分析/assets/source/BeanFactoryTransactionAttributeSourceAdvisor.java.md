<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/BeanFactoryTransactionAttributeSourceAdvisor.java -->
<!-- 正文引用：[[源码:BeanFactoryTransactionAttributeSourceAdvisor.java#L起-L止]] ；代码块头部：(src: BeanFactoryTransactionAttributeSourceAdvisor.java#L起-L止) -->

# BeanFactoryTransactionAttributeSourceAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/BeanFactoryTransactionAttributeSourceAdvisor.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.481Z |

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
   19 | import org.springframework.aop.ClassFilter;
   20 | import org.springframework.aop.Pointcut;
   21 | import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
   22 | 
   23 | /**
   24 |  * Advisor driven by a {@link TransactionAttributeSource}, used to include
   25 |  * a transaction advice bean for methods that are transactional.
   26 |  *
   27 |  * @author Juergen Hoeller
   28 |  * @since 2.5.5
   29 |  * @see #setAdviceBeanName
   30 |  * @see TransactionInterceptor
   31 |  * @see TransactionAttributeSourceAdvisor
   32 |  */
   33 | @SuppressWarnings("serial")
   34 | public class BeanFactoryTransactionAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {
   35 | 
   36 | 	private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut();
   37 | 
   38 | 
   39 | 	/**
   40 | 	 * Set the transaction attribute source which is used to find transaction
   41 | 	 * attributes. This should usually be identical to the source reference
   42 | 	 * set on the transaction interceptor itself.
   43 | 	 * @see TransactionInterceptor#setTransactionAttributeSource
   44 | 	 */
   45 | 	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
   46 | 		this.pointcut.setTransactionAttributeSource(transactionAttributeSource);
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Set the {@link ClassFilter} to use for this pointcut.
   51 | 	 * Default is {@link ClassFilter#TRUE}.
   52 | 	 */
   53 | 	public void setClassFilter(ClassFilter classFilter) {
   54 | 		this.pointcut.setClassFilter(classFilter);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public Pointcut getPointcut() {
   59 | 		return this.pointcut;
   60 | 	}
   61 | 
   62 | }
```
