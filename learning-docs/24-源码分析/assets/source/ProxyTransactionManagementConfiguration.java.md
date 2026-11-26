<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/ProxyTransactionManagementConfiguration.java -->
<!-- 正文引用：[[源码:ProxyTransactionManagementConfiguration.java#L起-L止]] ；代码块头部：(src: ProxyTransactionManagementConfiguration.java#L起-L止) -->

# ProxyTransactionManagementConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/ProxyTransactionManagementConfiguration.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:56.390Z |

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
   17 | package org.springframework.transaction.annotation;
   18 | 
   19 | import org.springframework.beans.factory.config.BeanDefinition;
   20 | import org.springframework.context.annotation.Bean;
   21 | import org.springframework.context.annotation.Configuration;
   22 | import org.springframework.context.annotation.ImportRuntimeHints;
   23 | import org.springframework.context.annotation.Role;
   24 | import org.springframework.transaction.config.TransactionManagementConfigUtils;
   25 | import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
   26 | import org.springframework.transaction.interceptor.TransactionAttributeSource;
   27 | import org.springframework.transaction.interceptor.TransactionInterceptor;
   28 | 
   29 | /**
   30 |  * {@code @Configuration} class that registers the Spring infrastructure beans
   31 |  * necessary to enable proxy-based annotation-driven transaction management.
   32 |  *
   33 |  * @author Chris Beams
   34 |  * @author Sebastien Deleuze
   35 |  * @since 3.1
   36 |  * @see EnableTransactionManagement
   37 |  * @see TransactionManagementConfigurationSelector
   38 |  */
   39 | @Configuration(proxyBeanMethods = false)
   40 | @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   41 | @ImportRuntimeHints(TransactionRuntimeHints.class)
   42 | public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {
   43 | 
   44 | 	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
   45 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   46 | 	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
   47 | 			TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {
   48 | 
   49 | 		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
   50 | 		advisor.setTransactionAttributeSource(transactionAttributeSource);
   51 | 		advisor.setAdvice(transactionInterceptor);
   52 | 		if (this.enableTx != null) {
   53 | 			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
   54 | 		}
   55 | 		return advisor;
   56 | 	}
   57 | 
   58 | 	@Bean
   59 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   60 | 	public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
   61 | 		TransactionInterceptor interceptor = new TransactionInterceptor();
   62 | 		interceptor.setTransactionAttributeSource(transactionAttributeSource);
   63 | 		if (this.txManager != null) {
   64 | 			interceptor.setTransactionManager(this.txManager);
   65 | 		}
   66 | 		return interceptor;
   67 | 	}
   68 | 
   69 | }
```
