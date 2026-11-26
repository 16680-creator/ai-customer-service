<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/autoproxy/AbstractAdvisorAutoProxyCreator.java -->
<!-- 正文引用：[[源码:AbstractAdvisorAutoProxyCreator.java#L起-L止]] ；代码块头部：(src: AbstractAdvisorAutoProxyCreator.java#L起-L止) -->

# AbstractAdvisorAutoProxyCreator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/autoproxy/AbstractAdvisorAutoProxyCreator.java` |
| 行数 | 203 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:59.139Z |

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
   19 | import java.util.List;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.aop.Advisor;
   24 | import org.springframework.aop.TargetSource;
   25 | import org.springframework.aop.framework.AopConfigException;
   26 | import org.springframework.aop.support.AopUtils;
   27 | import org.springframework.beans.factory.BeanCreationException;
   28 | import org.springframework.beans.factory.BeanFactory;
   29 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   30 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Generic auto proxy creator that builds AOP proxies for specific beans
   35 |  * based on detected Advisors for each bean.
   36 |  *
   37 |  * <p>Subclasses may override the {@link #findCandidateAdvisors()} method to
   38 |  * return a custom list of Advisors applying to any object. Subclasses can
   39 |  * also override the inherited {@link #shouldSkip} method to exclude certain
   40 |  * objects from auto-proxying.
   41 |  *
   42 |  * <p>Advisors or advices requiring ordering should be annotated with
   43 |  * {@link org.springframework.core.annotation.Order @Order} or implement the
   44 |  * {@link org.springframework.core.Ordered} interface. This class sorts
   45 |  * advisors using the {@link AnnotationAwareOrderComparator}. Advisors that are
   46 |  * not annotated with {@code @Order} or don't implement the {@code Ordered}
   47 |  * interface will be considered as unordered; they will appear at the end of the
   48 |  * advisor chain in an undefined order.
   49 |  *
   50 |  * @author Rod Johnson
   51 |  * @author Juergen Hoeller
   52 |  * @see #findCandidateAdvisors
   53 |  */
   54 | @SuppressWarnings("serial")
   55 | public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {
   56 | 
   57 | 	private @Nullable BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;
   58 | 
   59 | 
   60 | 	@Override
   61 | 	public void setBeanFactory(BeanFactory beanFactory) {
   62 | 		super.setBeanFactory(beanFactory);
   63 | 		if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
   64 | 			throw new IllegalArgumentException(
   65 | 					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
   66 | 		}
   67 | 		initBeanFactory(clbf);
   68 | 	}
   69 | 
   70 | 	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
   71 | 		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
   72 | 	}
   73 | 
   74 | 
   75 | 	@Override
   76 | 	protected Object @Nullable [] getAdvicesAndAdvisorsForBean(
   77 | 			Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
   78 | 
   79 | 		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
   80 | 		if (advisors.isEmpty()) {
   81 | 			return DO_NOT_PROXY;
   82 | 		}
   83 | 		return advisors.toArray();
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Find all eligible Advisors for auto-proxying this class.
   88 | 	 * @param beanClass the clazz to find advisors for
   89 | 	 * @param beanName the name of the currently proxied bean
   90 | 	 * @return the empty List, not {@code null},
   91 | 	 * if there are no pointcuts or interceptors
   92 | 	 * @see #findCandidateAdvisors
   93 | 	 * @see #sortAdvisors
   94 | 	 * @see #extendAdvisors
   95 | 	 */
   96 | 	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
   97 | 		List<Advisor> candidateAdvisors = findCandidateAdvisors();
   98 | 		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
   99 | 		extendAdvisors(eligibleAdvisors);
  100 | 		if (!eligibleAdvisors.isEmpty()) {
  101 | 			try {
  102 | 				eligibleAdvisors = sortAdvisors(eligibleAdvisors);
  103 | 			}
  104 | 			catch (BeanCreationException ex) {
  105 | 				throw new AopConfigException("Advisor sorting failed with unexpected bean creation, probably due " +
  106 | 						"to custom use of the Ordered interface. Consider using the @Order annotation instead.", ex);
  107 | 			}
  108 | 		}
  109 | 		return eligibleAdvisors;
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Find all candidate Advisors to use in auto-proxying.
  114 | 	 * @return the List of candidate Advisors
  115 | 	 */
  116 | 	protected List<Advisor> findCandidateAdvisors() {
  117 | 		Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
  118 | 		return this.advisorRetrievalHelper.findAdvisorBeans();
  119 | 	}
  120 | 
  121 | 	/**
  122 | 	 * Search the given candidate Advisors to find all Advisors that
  123 | 	 * can apply to the specified bean.
  124 | 	 * @param candidateAdvisors the candidate Advisors
  125 | 	 * @param beanClass the target's bean class
  126 | 	 * @param beanName the target's bean name
  127 | 	 * @return the List of applicable Advisors
  128 | 	 * @see ProxyCreationContext#getCurrentProxiedBeanName()
  129 | 	 */
  130 | 	protected List<Advisor> findAdvisorsThatCanApply(
  131 | 			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
  132 | 
  133 | 		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
  134 | 		try {
  135 | 			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
  136 | 		}
  137 | 		finally {
  138 | 			ProxyCreationContext.setCurrentProxiedBeanName(null);
  139 | 		}
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Return whether the Advisor bean with the given name is eligible
  144 | 	 * for proxying in the first place.
  145 | 	 * @param beanName the name of the Advisor bean
  146 | 	 * @return whether the bean is eligible
  147 | 	 */
  148 | 	protected boolean isEligibleAdvisorBean(String beanName) {
  149 | 		return true;
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Sort advisors based on ordering. Subclasses may choose to override this
  154 | 	 * method to customize the sorting strategy.
  155 | 	 * @param advisors the source List of Advisors
  156 | 	 * @return the sorted List of Advisors
  157 | 	 * @see org.springframework.core.Ordered
  158 | 	 * @see org.springframework.core.annotation.Order
  159 | 	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
  160 | 	 */
  161 | 	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
  162 | 		AnnotationAwareOrderComparator.sort(advisors);
  163 | 		return advisors;
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Extension hook that subclasses can override to register additional Advisors,
  168 | 	 * given the sorted Advisors obtained to date.
  169 | 	 * <p>The default implementation is empty.
  170 | 	 * <p>Typically used to add Advisors that expose contextual information
  171 | 	 * required by some of the later advisors.
  172 | 	 * @param candidateAdvisors the Advisors that have already been identified as
  173 | 	 * applying to a given bean
  174 | 	 */
  175 | 	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * This auto-proxy creator always returns pre-filtered Advisors.
  180 | 	 */
  181 | 	@Override
  182 | 	protected boolean advisorsPreFiltered() {
  183 | 		return true;
  184 | 	}
  185 | 
  186 | 
  187 | 	/**
  188 | 	 * Subclass of BeanFactoryAdvisorRetrievalHelper that delegates to
  189 | 	 * surrounding AbstractAdvisorAutoProxyCreator facilities.
  190 | 	 */
  191 | 	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {
  192 | 
  193 | 		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
  194 | 			super(beanFactory);
  195 | 		}
  196 | 
  197 | 		@Override
  198 | 		protected boolean isEligibleBean(String beanName) {
  199 | 			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
  200 | 		}
  201 | 	}
  202 | 
  203 | }
```
