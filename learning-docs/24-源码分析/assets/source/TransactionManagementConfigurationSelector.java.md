<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/TransactionManagementConfigurationSelector.java -->
<!-- 正文引用：[[源码:TransactionManagementConfigurationSelector.java#L起-L止]] ；代码块头部：(src: TransactionManagementConfigurationSelector.java#L起-L止) -->

# TransactionManagementConfigurationSelector.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/TransactionManagementConfigurationSelector.java` |
| 行数 | 61 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:56.704Z |

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
   19 | import org.springframework.context.annotation.AdviceMode;
   20 | import org.springframework.context.annotation.AdviceModeImportSelector;
   21 | import org.springframework.context.annotation.AutoProxyRegistrar;
   22 | import org.springframework.transaction.config.TransactionManagementConfigUtils;
   23 | import org.springframework.util.ClassUtils;
   24 | 
   25 | /**
   26 |  * Selects which implementation of {@link AbstractTransactionManagementConfiguration}
   27 |  * should be used based on the value of {@link EnableTransactionManagement#mode} on the
   28 |  * importing {@code @Configuration} class.
   29 |  *
   30 |  * @author Chris Beams
   31 |  * @author Juergen Hoeller
   32 |  * @since 3.1
   33 |  * @see EnableTransactionManagement
   34 |  * @see ProxyTransactionManagementConfiguration
   35 |  * @see TransactionManagementConfigUtils#TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
   36 |  * @see TransactionManagementConfigUtils#JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME
   37 |  */
   38 | public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {
   39 | 
   40 | 	/**
   41 | 	 * Returns {@link ProxyTransactionManagementConfiguration} or
   42 | 	 * {@code AspectJ(Jta)TransactionManagementConfiguration} for {@code PROXY}
   43 | 	 * and {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()},
   44 | 	 * respectively.
   45 | 	 */
   46 | 	@Override
   47 | 	protected String[] selectImports(AdviceMode adviceMode) {
   48 | 		return switch (adviceMode) {
   49 | 			case PROXY -> new String[] {AutoProxyRegistrar.class.getName(),
   50 | 					ProxyTransactionManagementConfiguration.class.getName()};
   51 | 			case ASPECTJ -> new String[] {determineTransactionAspectClass()};
   52 | 		};
   53 | 	}
   54 | 
   55 | 	private String determineTransactionAspectClass() {
   56 | 		return (ClassUtils.isPresent("jakarta.transaction.Transactional", getClass().getClassLoader()) ?
   57 | 				TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME :
   58 | 				TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME);
   59 | 	}
   60 | 
   61 | }
```
