<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/annotation/AnnotationAwareAspectJAutoProxyCreator.java -->
<!-- 正文引用：[[源码:AnnotationAwareAspectJAutoProxyCreator.java#L起-L止]] ；代码块头部：(src: AnnotationAwareAspectJAutoProxyCreator.java#L起-L止) -->

# AnnotationAwareAspectJAutoProxyCreator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/annotation/AnnotationAwareAspectJAutoProxyCreator.java` |
| 行数 | 151 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:01.333Z |

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
   17 | package org.springframework.aop.aspectj.annotation;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.regex.Pattern;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aop.Advisor;
   26 | import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
   27 | import org.springframework.beans.factory.ListableBeanFactory;
   28 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * {@link AspectJAwareAdvisorAutoProxyCreator} subclass that processes all AspectJ
   33 |  * annotation aspects in the current application context, as well as Spring Advisors.
   34 |  *
   35 |  * <p>Any AspectJ annotated classes will automatically be recognized, and their
   36 |  * advice applied if Spring AOP's proxy-based model is capable of applying it.
   37 |  * This covers method execution joinpoints.
   38 |  *
   39 |  * <p>If the &lt;aop:include&gt; element is used, only @AspectJ beans with names matched by
   40 |  * an include pattern will be considered as defining aspects to use for Spring auto-proxying.
   41 |  *
   42 |  * <p>Processing of Spring Advisors follows the rules established in
   43 |  * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}.
   44 |  *
   45 |  * @author Rod Johnson
   46 |  * @author Juergen Hoeller
   47 |  * @since 2.0
   48 |  * @see org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory
   49 |  */
   50 | @SuppressWarnings("serial")
   51 | public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {
   52 | 
   53 | 	private @Nullable List<Pattern> includePatterns;
   54 | 
   55 | 	private @Nullable AspectJAdvisorFactory aspectJAdvisorFactory;
   56 | 
   57 | 	private @Nullable BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;
   58 | 
   59 | 
   60 | 	/**
   61 | 	 * Set a list of regex patterns, matching eligible @AspectJ bean names.
   62 | 	 * <p>Default is to consider all @AspectJ beans as eligible.
   63 | 	 */
   64 | 	public void setIncludePatterns(List<String> patterns) {
   65 | 		this.includePatterns = new ArrayList<>(patterns.size());
   66 | 		for (String patternText : patterns) {
   67 | 			this.includePatterns.add(Pattern.compile(patternText));
   68 | 		}
   69 | 	}
   70 | 
   71 | 	public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
   72 | 		Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
   73 | 		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
   78 | 		super.initBeanFactory(beanFactory);
   79 | 		if (this.aspectJAdvisorFactory == null) {
   80 | 			this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
   81 | 		}
   82 | 		this.aspectJAdvisorsBuilder =
   83 | 				new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
   84 | 	}
   85 | 
   86 | 
   87 | 	@Override
   88 | 	protected List<Advisor> findCandidateAdvisors() {
   89 | 		// Add all the Spring advisors found according to superclass rules.
   90 | 		List<Advisor> advisors = super.findCandidateAdvisors();
   91 | 		// Build Advisors for all AspectJ aspects in the bean factory.
   92 | 		if (this.aspectJAdvisorsBuilder != null) {
   93 | 			advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
   94 | 		}
   95 | 		return advisors;
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	protected boolean isInfrastructureClass(Class<?> beanClass) {
  100 | 		// Previously we setProxyTargetClass(true) in the constructor, but that has too
  101 | 		// broad an impact. Instead we now override isInfrastructureClass to avoid proxying
  102 | 		// aspects. I'm not entirely happy with that as there is no good reason not
  103 | 		// to advise aspects, except that it causes advice invocation to go through a
  104 | 		// proxy, and if the aspect implements, for example, the Ordered interface it will be
  105 | 		// proxied by that interface and fail at runtime as the advice method is not
  106 | 		// defined on the interface. We could potentially relax the restriction about
  107 | 		// not advising aspects in the future.
  108 | 		return (super.isInfrastructureClass(beanClass) ||
  109 | 				(this.aspectJAdvisorFactory != null && this.aspectJAdvisorFactory.isAspect(beanClass)));
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Check whether the given aspect bean is eligible for auto-proxying.
  114 | 	 * <p>If no &lt;aop:include&gt; elements were used then "includePatterns" will be
  115 | 	 * {@code null} and all beans are included. If "includePatterns" is non-null,
  116 | 	 * then one of the patterns must match.
  117 | 	 */
  118 | 	protected boolean isEligibleAspectBean(String beanName) {
  119 | 		if (this.includePatterns == null) {
  120 | 			return true;
  121 | 		}
  122 | 		else {
  123 | 			for (Pattern pattern : this.includePatterns) {
  124 | 				if (pattern.matcher(beanName).matches()) {
  125 | 					return true;
  126 | 				}
  127 | 			}
  128 | 			return false;
  129 | 		}
  130 | 	}
  131 | 
  132 | 
  133 | 	/**
  134 | 	 * Subclass of BeanFactoryAspectJAdvisorsBuilderAdapter that delegates to
  135 | 	 * surrounding AnnotationAwareAspectJAutoProxyCreator facilities.
  136 | 	 */
  137 | 	private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {
  138 | 
  139 | 		public BeanFactoryAspectJAdvisorsBuilderAdapter(
  140 | 				ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
  141 | 
  142 | 			super(beanFactory, advisorFactory);
  143 | 		}
  144 | 
  145 | 		@Override
  146 | 		protected boolean isEligibleBean(String beanName) {
  147 | 			return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
  148 | 		}
  149 | 	}
  150 | 
  151 | }
```
