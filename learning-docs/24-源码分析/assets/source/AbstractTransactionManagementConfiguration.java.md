<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/AbstractTransactionManagementConfiguration.java -->
<!-- 正文引用：[[源码:AbstractTransactionManagementConfiguration.java#L起-L止]] ；代码块头部：(src: AbstractTransactionManagementConfiguration.java#L起-L止) -->

# AbstractTransactionManagementConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/AbstractTransactionManagementConfiguration.java` |
| 行数 | 100 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:56.849Z |

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
   19 | import java.util.Collection;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.factory.annotation.Autowired;
   24 | import org.springframework.beans.factory.config.BeanDefinition;
   25 | import org.springframework.context.annotation.Bean;
   26 | import org.springframework.context.annotation.Configuration;
   27 | import org.springframework.context.annotation.ImportAware;
   28 | import org.springframework.context.annotation.Role;
   29 | import org.springframework.core.annotation.AnnotationAttributes;
   30 | import org.springframework.core.type.AnnotationMetadata;
   31 | import org.springframework.transaction.TransactionManager;
   32 | import org.springframework.transaction.config.TransactionManagementConfigUtils;
   33 | import org.springframework.transaction.event.TransactionalEventListenerFactory;
   34 | import org.springframework.transaction.interceptor.RollbackRuleAttribute;
   35 | import org.springframework.transaction.interceptor.TransactionAttributeSource;
   36 | import org.springframework.util.CollectionUtils;
   37 | 
   38 | /**
   39 |  * Abstract base {@code @Configuration} class providing common structure for enabling
   40 |  * Spring's annotation-driven transaction management capability.
   41 |  *
   42 |  * @author Chris Beams
   43 |  * @author Stephane Nicoll
   44 |  * @author Juergen Hoeller
   45 |  * @since 3.1
   46 |  * @see EnableTransactionManagement
   47 |  */
   48 | @Configuration
   49 | public abstract class AbstractTransactionManagementConfiguration implements ImportAware {
   50 | 
   51 | 	protected @Nullable AnnotationAttributes enableTx;
   52 | 
   53 | 	/**
   54 | 	 * Default transaction manager, as configured through a {@link TransactionManagementConfigurer}.
   55 | 	 */
   56 | 	protected @Nullable TransactionManager txManager;
   57 | 
   58 | 
   59 | 	@Override
   60 | 	public void setImportMetadata(AnnotationMetadata importMetadata) {
   61 | 		this.enableTx = AnnotationAttributes.fromMap(
   62 | 				importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName()));
   63 | 		if (this.enableTx == null) {
   64 | 			throw new IllegalArgumentException(
   65 | 					"@EnableTransactionManagement is not present on importing class " + importMetadata.getClassName());
   66 | 		}
   67 | 	}
   68 | 
   69 | 	@Autowired(required = false)
   70 | 	void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
   71 | 		if (CollectionUtils.isEmpty(configurers)) {
   72 | 			return;
   73 | 		}
   74 | 		if (configurers.size() > 1) {
   75 | 			throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
   76 | 		}
   77 | 		TransactionManagementConfigurer configurer = configurers.iterator().next();
   78 | 		this.txManager = configurer.annotationDrivenTransactionManager();
   79 | 	}
   80 | 
   81 | 
   82 | 	@Bean
   83 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   84 | 	public TransactionAttributeSource transactionAttributeSource() {
   85 | 		// Accept protected @Transactional methods on CGLIB proxies, as of 6.0
   86 | 		AnnotationTransactionAttributeSource tas = new AnnotationTransactionAttributeSource(false);
   87 | 		// Apply default rollback rule, as of 6.2
   88 | 		if (this.enableTx != null && this.enableTx.getEnum("rollbackOn") == RollbackOn.ALL_EXCEPTIONS) {
   89 | 			tas.addDefaultRollbackRule(RollbackRuleAttribute.ROLLBACK_ON_ALL_EXCEPTIONS);
   90 | 		}
   91 | 		return tas;
   92 | 	}
   93 | 
   94 | 	@Bean(name = TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)
   95 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   96 | 	public static TransactionalEventListenerFactory transactionalEventListenerFactory() {
   97 | 		return new RestrictedTransactionalEventListenerFactory();
   98 | 	}
   99 | 
  100 | }
```
