<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/autoproxy/BeanFactoryAdvisorRetrievalHelper.java -->
<!-- 正文引用：[[源码:BeanFactoryAdvisorRetrievalHelper.java#L起-L止]] ；代码块头部：(src: BeanFactoryAdvisorRetrievalHelper.java#L起-L止) -->

# BeanFactoryAdvisorRetrievalHelper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/autoproxy/BeanFactoryAdvisorRetrievalHelper.java` |
| 行数 | 124 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:59.340Z |

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
   17 | package org.springframework.aop.framework.autoproxy;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.aop.Advisor;
   27 | import org.springframework.beans.factory.BeanCreationException;
   28 | import org.springframework.beans.factory.BeanCurrentlyInCreationException;
   29 | import org.springframework.beans.factory.BeanFactoryUtils;
   30 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Helper for retrieving standard Spring Advisors from a BeanFactory,
   35 |  * for use with auto-proxying.
   36 |  *
   37 |  * @author Juergen Hoeller
   38 |  * @since 2.0.2
   39 |  * @see AbstractAdvisorAutoProxyCreator
   40 |  */
   41 | public class BeanFactoryAdvisorRetrievalHelper {
   42 | 
   43 | 	private static final Log logger = LogFactory.getLog(BeanFactoryAdvisorRetrievalHelper.class);
   44 | 
   45 | 	private final ConfigurableListableBeanFactory beanFactory;
   46 | 
   47 | 	private volatile String @Nullable [] cachedAdvisorBeanNames;
   48 | 
   49 | 
   50 | 	/**
   51 | 	 * Create a new BeanFactoryAdvisorRetrievalHelper for the given BeanFactory.
   52 | 	 * @param beanFactory the ListableBeanFactory to scan
   53 | 	 */
   54 | 	public BeanFactoryAdvisorRetrievalHelper(ConfigurableListableBeanFactory beanFactory) {
   55 | 		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
   56 | 		this.beanFactory = beanFactory;
   57 | 	}
   58 | 
   59 | 
   60 | 	/**
   61 | 	 * Find all eligible Advisor beans in the current bean factory,
   62 | 	 * ignoring FactoryBeans and excluding beans that are currently in creation.
   63 | 	 * @return the list of {@link org.springframework.aop.Advisor} beans
   64 | 	 * @see #isEligibleBean
   65 | 	 */
   66 | 	public List<Advisor> findAdvisorBeans() {
   67 | 		// Determine list of advisor bean names, if not cached already.
   68 | 		String[] advisorNames = this.cachedAdvisorBeanNames;
   69 | 		if (advisorNames == null) {
   70 | 			// Do not initialize FactoryBeans here: We need to leave all regular beans
   71 | 			// uninitialized to let the auto-proxy creator apply to them!
   72 | 			advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
   73 | 					this.beanFactory, Advisor.class, true, false);
   74 | 			this.cachedAdvisorBeanNames = advisorNames;
   75 | 		}
   76 | 		if (advisorNames.length == 0) {
   77 | 			return new ArrayList<>();
   78 | 		}
   79 | 
   80 | 		List<Advisor> advisors = new ArrayList<>();
   81 | 		for (String name : advisorNames) {
   82 | 			if (isEligibleBean(name)) {
   83 | 				if (this.beanFactory.isCurrentlyInCreation(name)) {
   84 | 					if (logger.isTraceEnabled()) {
   85 | 						logger.trace("Skipping currently created advisor '" + name + "'");
   86 | 					}
   87 | 				}
   88 | 				else {
   89 | 					try {
   90 | 						advisors.add(this.beanFactory.getBean(name, Advisor.class));
   91 | 					}
   92 | 					catch (BeanCreationException ex) {
   93 | 						Throwable rootCause = ex.getMostSpecificCause();
   94 | 						if (rootCause instanceof BeanCurrentlyInCreationException bce) {
   95 | 							String bceBeanName = bce.getBeanName();
   96 | 							if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
   97 | 								if (logger.isTraceEnabled()) {
   98 | 									logger.trace("Skipping advisor '" + name +
   99 | 											"' with dependency on currently created bean: " + ex.getMessage());
  100 | 								}
  101 | 								// Ignore: indicates a reference back to the bean we're trying to advise.
  102 | 								// We want to find advisors other than the currently created bean itself.
  103 | 								continue;
  104 | 							}
  105 | 						}
  106 | 						throw ex;
  107 | 					}
  108 | 				}
  109 | 			}
  110 | 		}
  111 | 		return advisors;
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Determine whether the aspect bean with the given name is eligible.
  116 | 	 * <p>The default implementation always returns {@code true}.
  117 | 	 * @param beanName the name of the aspect bean
  118 | 	 * @return whether the bean is eligible
  119 | 	 */
  120 | 	protected boolean isEligibleBean(String beanName) {
  121 | 		return true;
  122 | 	}
  123 | 
  124 | }
```
