<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/AbstractAdvisingBeanPostProcessor.java -->
<!-- 正文引用：[[源码:AbstractAdvisingBeanPostProcessor.java#L起-L止]] ；代码块头部：(src: AbstractAdvisingBeanPostProcessor.java#L起-L止) -->

# AbstractAdvisingBeanPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/AbstractAdvisingBeanPostProcessor.java` |
| 行数 | 210 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:55.948Z |

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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import java.util.Map;
   20 | import java.util.concurrent.ConcurrentHashMap;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.aop.Advisor;
   25 | import org.springframework.aop.support.AopUtils;
   26 | import org.springframework.beans.factory.config.BeanPostProcessor;
   27 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   28 | import org.springframework.core.SmartClassLoader;
   29 | 
   30 | /**
   31 |  * Base class for {@link BeanPostProcessor} implementations that apply a
   32 |  * Spring AOP {@link Advisor} to specific beans.
   33 |  *
   34 |  * @author Juergen Hoeller
   35 |  * @since 3.2
   36 |  */
   37 | @SuppressWarnings("serial")
   38 | public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport
   39 | 		implements SmartInstantiationAwareBeanPostProcessor {
   40 | 
   41 | 	protected @Nullable Advisor advisor;
   42 | 
   43 | 	protected boolean beforeExistingAdvisors = false;
   44 | 
   45 | 	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);
   46 | 
   47 | 
   48 | 	/**
   49 | 	 * Set whether this post-processor's advisor is supposed to apply before
   50 | 	 * existing advisors when encountering a pre-advised object.
   51 | 	 * <p>Default is "false", applying the advisor after existing advisors, i.e.
   52 | 	 * as close as possible to the target method. Switch this to "true" in order
   53 | 	 * for this post-processor's advisor to wrap existing advisors as well.
   54 | 	 * <p>Note: Check the concrete post-processor's javadoc whether it possibly
   55 | 	 * changes this flag by default, depending on the nature of its advisor.
   56 | 	 */
   57 | 	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
   58 | 		this.beforeExistingAdvisors = beforeExistingAdvisors;
   59 | 	}
   60 | 
   61 | 
   62 | 	@Override
   63 | 	public Class<?> determineBeanType(Class<?> beanClass, String beanName) {
   64 | 		if (this.advisor != null && isEligible(beanClass)) {
   65 | 			ProxyFactory proxyFactory = new ProxyFactory();
   66 | 			proxyFactory.copyFrom(this);
   67 | 			proxyFactory.setTargetClass(beanClass);
   68 | 
   69 | 			if (!proxyFactory.isProxyTargetClass()) {
   70 | 				evaluateProxyInterfaces(beanClass, proxyFactory);
   71 | 			}
   72 | 			proxyFactory.addAdvisor(this.advisor);
   73 | 			customizeProxyFactory(proxyFactory);
   74 | 
   75 | 			// Use original ClassLoader if bean class not locally loaded in overriding class loader
   76 | 			ClassLoader classLoader = getProxyClassLoader();
   77 | 			if (classLoader instanceof SmartClassLoader smartClassLoader &&
   78 | 					classLoader != beanClass.getClassLoader()) {
   79 | 				classLoader = smartClassLoader.getOriginalClassLoader();
   80 | 			}
   81 | 			return proxyFactory.getProxyClass(classLoader);
   82 | 		}
   83 | 
   84 | 		return beanClass;
   85 | 	}
   86 | 
   87 | 	@Override
   88 | 	public Object postProcessAfterInitialization(Object bean, String beanName) {
   89 | 		if (this.advisor == null || bean instanceof AopInfrastructureBean) {
   90 | 			// Ignore AOP infrastructure such as scoped proxies.
   91 | 			return bean;
   92 | 		}
   93 | 
   94 | 		if (bean instanceof Advised advised) {
   95 | 			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
   96 | 				// Add our local Advisor to the existing proxy's Advisor chain.
   97 | 				if (this.beforeExistingAdvisors) {
   98 | 					advised.addAdvisor(0, this.advisor);
   99 | 				}
  100 | 				else if (advised.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE &&
  101 | 						advised.getAdvisorCount() > 0) {
  102 | 					// No target, leave last Advisor in place and add new Advisor right before.
  103 | 					advised.addAdvisor(advised.getAdvisorCount() - 1, this.advisor);
  104 | 					return bean;
  105 | 				}
  106 | 				else {
  107 | 					advised.addAdvisor(this.advisor);
  108 | 				}
  109 | 				return bean;
  110 | 			}
  111 | 		}
  112 | 
  113 | 		if (isEligible(bean, beanName)) {
  114 | 			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
  115 | 			if (!proxyFactory.isProxyTargetClass() && !proxyFactory.hasUserSuppliedInterfaces()) {
  116 | 				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
  117 | 			}
  118 | 			proxyFactory.addAdvisor(this.advisor);
  119 | 			customizeProxyFactory(proxyFactory);
  120 | 			proxyFactory.setFrozen(isFrozen());
  121 | 			proxyFactory.setPreFiltered(true);
  122 | 
  123 | 			// Use original ClassLoader if bean class not locally loaded in overriding class loader
  124 | 			ClassLoader classLoader = getProxyClassLoader();
  125 | 			if (classLoader instanceof SmartClassLoader smartClassLoader &&
  126 | 					classLoader != bean.getClass().getClassLoader()) {
  127 | 				classLoader = smartClassLoader.getOriginalClassLoader();
  128 | 			}
  129 | 			return proxyFactory.getProxy(classLoader);
  130 | 		}
  131 | 
  132 | 		// No proxy needed.
  133 | 		return bean;
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Check whether the given bean is eligible for advising with this
  138 | 	 * post-processor's {@link Advisor}.
  139 | 	 * <p>Delegates to {@link #isEligible(Class)} for target class checking.
  140 | 	 * Can be overridden, for example, to specifically exclude certain beans by name.
  141 | 	 * <p>Note: Only called for regular bean instances but not for existing
  142 | 	 * proxy instances which implement {@link Advised} and allow for adding
  143 | 	 * the local {@link Advisor} to the existing proxy's {@link Advisor} chain.
  144 | 	 * For the latter, {@link #isEligible(Class)} is being called directly,
  145 | 	 * with the actual target class behind the existing proxy (as determined
  146 | 	 * by {@link AopUtils#getTargetClass(Object)}).
  147 | 	 * @param bean the bean instance
  148 | 	 * @param beanName the name of the bean
  149 | 	 * @see #isEligible(Class)
  150 | 	 */
  151 | 	protected boolean isEligible(Object bean, String beanName) {
  152 | 		return isEligible(bean.getClass());
  153 | 	}
  154 | 
  155 | 	/**
  156 | 	 * Check whether the given class is eligible for advising with this
  157 | 	 * post-processor's {@link Advisor}.
  158 | 	 * <p>Implements caching of {@code canApply} results per bean target class.
  159 | 	 * @param targetClass the class to check against
  160 | 	 * @see AopUtils#canApply(Advisor, Class)
  161 | 	 */
  162 | 	protected boolean isEligible(Class<?> targetClass) {
  163 | 		Boolean eligible = this.eligibleBeans.get(targetClass);
  164 | 		if (eligible != null) {
  165 | 			return eligible;
  166 | 		}
  167 | 		if (this.advisor == null) {
  168 | 			return false;
  169 | 		}
  170 | 		eligible = AopUtils.canApply(this.advisor, targetClass);
  171 | 		this.eligibleBeans.put(targetClass, eligible);
  172 | 		return eligible;
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Prepare a {@link ProxyFactory} for the given bean.
  177 | 	 * <p>Subclasses may customize the handling of the target instance and in
  178 | 	 * particular the exposure of the target class. The default introspection
  179 | 	 * of interfaces for non-target-class proxies and the configured advisor
  180 | 	 * will be applied afterwards; {@link #customizeProxyFactory} allows for
  181 | 	 * late customizations of those parts right before proxy creation.
  182 | 	 * @param bean the bean instance to create a proxy for
  183 | 	 * @param beanName the corresponding bean name
  184 | 	 * @return the ProxyFactory, initialized with this processor's
  185 | 	 * {@link ProxyConfig} settings and the specified bean
  186 | 	 * @since 4.2.3
  187 | 	 * @see #customizeProxyFactory
  188 | 	 */
  189 | 	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
  190 | 		ProxyFactory proxyFactory = new ProxyFactory();
  191 | 		proxyFactory.copyFrom(this);
  192 | 		proxyFactory.setFrozen(false);
  193 | 		proxyFactory.setTarget(bean);
  194 | 		return proxyFactory;
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Subclasses may choose to implement this: for example,
  199 | 	 * to change the interfaces exposed.
  200 | 	 * <p>The default implementation is empty.
  201 | 	 * @param proxyFactory the ProxyFactory that is already configured with
  202 | 	 * target, advisor and interfaces and will be used to create the proxy
  203 | 	 * immediately after this method returns
  204 | 	 * @since 4.2.3
  205 | 	 * @see #prepareProxyFactory
  206 | 	 */
  207 | 	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
  208 | 	}
  209 | 
  210 | }
```
