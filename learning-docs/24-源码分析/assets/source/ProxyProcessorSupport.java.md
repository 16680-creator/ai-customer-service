<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ProxyProcessorSupport.java -->
<!-- 正文引用：[[源码:ProxyProcessorSupport.java#L起-L止]] ；代码块头部：(src: ProxyProcessorSupport.java#L起-L止) -->

# ProxyProcessorSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ProxyProcessorSupport.java` |
| 行数 | 145 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:57.652Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.beans.factory.BeanClassLoaderAware;
   22 | import org.springframework.core.Ordered;
   23 | import org.springframework.util.ClassUtils;
   24 | 
   25 | /**
   26 |  * Base class with common functionality for proxy processors, in particular
   27 |  * ClassLoader management and the {@link #evaluateProxyInterfaces} algorithm.
   28 |  *
   29 |  * @author Juergen Hoeller
   30 |  * @since 4.1
   31 |  * @see AbstractAdvisingBeanPostProcessor
   32 |  * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
   33 |  */
   34 | @SuppressWarnings("serial")
   35 | public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {
   36 | 
   37 | 	/**
   38 | 	 * This should run after all other processors, so that it can just add
   39 | 	 * an advisor to existing proxies rather than double-proxy.
   40 | 	 */
   41 | 	private int order = Ordered.LOWEST_PRECEDENCE;
   42 | 
   43 | 	private @Nullable ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();
   44 | 
   45 | 	private boolean classLoaderConfigured = false;
   46 | 
   47 | 
   48 | 	/**
   49 | 	 * Set the ordering which will apply to this processor's implementation
   50 | 	 * of {@link Ordered}, used when applying multiple processors.
   51 | 	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   52 | 	 * @param order the ordering value
   53 | 	 */
   54 | 	public void setOrder(int order) {
   55 | 		this.order = order;
   56 | 	}
   57 | 
   58 | 	@Override
   59 | 	public int getOrder() {
   60 | 		return this.order;
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Set the ClassLoader to generate the proxy class in.
   65 | 	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the containing
   66 | 	 * {@link org.springframework.beans.factory.BeanFactory} for loading all bean classes.
   67 | 	 * This can be overridden here for specific proxies.
   68 | 	 */
   69 | 	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
   70 | 		this.proxyClassLoader = classLoader;
   71 | 		this.classLoaderConfigured = (classLoader != null);
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * Return the configured proxy ClassLoader for this processor.
   76 | 	 */
   77 | 	protected @Nullable ClassLoader getProxyClassLoader() {
   78 | 		return this.proxyClassLoader;
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public void setBeanClassLoader(ClassLoader classLoader) {
   83 | 		if (!this.classLoaderConfigured) {
   84 | 			this.proxyClassLoader = classLoader;
   85 | 		}
   86 | 	}
   87 | 
   88 | 
   89 | 	/**
   90 | 	 * Check the interfaces on the given bean class and apply them to the {@link ProxyFactory},
   91 | 	 * if appropriate.
   92 | 	 * <p>Calls {@link #isConfigurationCallbackInterface} and {@link #isInternalLanguageInterface}
   93 | 	 * to filter for reasonable proxy interfaces, falling back to a target-class proxy otherwise.
   94 | 	 * @param beanClass the class of the bean
   95 | 	 * @param proxyFactory the ProxyFactory for the bean
   96 | 	 */
   97 | 	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
   98 | 		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
   99 | 		boolean hasReasonableProxyInterface = false;
  100 | 		for (Class<?> ifc : targetInterfaces) {
  101 | 			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
  102 | 					ifc.getMethods().length > 0) {
  103 | 				hasReasonableProxyInterface = true;
  104 | 				break;
  105 | 			}
  106 | 		}
  107 | 		if (hasReasonableProxyInterface) {
  108 | 			// Must allow for introductions; can't just set interfaces to the target's interfaces only.
  109 | 			for (Class<?> ifc : targetInterfaces) {
  110 | 				proxyFactory.addInterface(ifc);
  111 | 			}
  112 | 		}
  113 | 		else {
  114 | 			proxyFactory.setProxyTargetClass(true);
  115 | 		}
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Determine whether the given interface is just a container callback and
  120 | 	 * therefore not to be considered as a reasonable proxy interface.
  121 | 	 * <p>If no reasonable proxy interface is found for a given bean, it will get
  122 | 	 * proxied with its full target class, assuming that as the user's intention.
  123 | 	 * @param ifc the interface to check
  124 | 	 * @return whether the given interface is just a container callback
  125 | 	 */
  126 | 	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
  127 | 		return AopProxyUtils.isConfigurationCallbackInterface(ifc);
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Determine whether the given interface is a well-known internal language interface
  132 | 	 * and therefore not to be considered as a reasonable proxy interface.
  133 | 	 * <p>If no reasonable proxy interface is found for a given bean, it will get
  134 | 	 * proxied with its full target class, assuming that as the user's intention.
  135 | 	 * @param ifc the interface to check
  136 | 	 * @return whether the given interface is an internal language interface
  137 | 	 */
  138 | 	protected boolean isInternalLanguageInterface(Class<?> ifc) {
  139 | 		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
  140 | 				ifc.getName().endsWith(".cglib.proxy.Factory") ||
  141 | 				ifc.getName().endsWith(".bytebuddy.MockAccess") ||  // Mockito < 5.16.1
  142 | 				ifc.getName().endsWith(".bytebuddy.access.MockAccess"));  // Mockito >= 5.16.1
  143 | 	}
  144 | 
  145 | }
```
