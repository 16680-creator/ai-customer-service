<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/config/BeanPostProcessor.java -->
<!-- 正文引用：[[源码:BeanPostProcessor.java#L起-L止]] ；代码块头部：(src: BeanPostProcessor.java#L起-L止) -->

# BeanPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/config/BeanPostProcessor.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:34:22.045Z |

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
   17 | package org.springframework.beans.factory.config;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.beans.BeansException;
   22 | 
   23 | /**
   24 |  * Factory hook that allows for custom modification of new bean instances &mdash;
   25 |  * for example, checking for marker interfaces or wrapping beans with proxies.
   26 |  *
   27 |  * <p>Typically, post-processors that populate beans via marker interfaces
   28 |  * or the like will implement {@link #postProcessBeforeInitialization},
   29 |  * while post-processors that wrap beans with proxies will normally
   30 |  * implement {@link #postProcessAfterInitialization}.
   31 |  *
   32 |  * <h3>Registration</h3>
   33 |  * <p>An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
   34 |  * in its bean definitions and apply those post-processors to any beans subsequently
   35 |  * created. A plain {@code BeanFactory} allows for programmatic registration of
   36 |  * post-processors, applying them to all beans created through the bean factory.
   37 |  *
   38 |  * <p>When registering a {@code BeanPostProcessor} via an {@code @Bean} method in
   39 |  * a {@code @Configuration} class, use a {@code static} method with ideally no
   40 |  * dependencies in order to avoid eager initialization that can make other beans
   41 |  * ineligible for full post-processing. See the "BeanPostProcessor-returning
   42 |  * {@code @Bean} methods" section in
   43 |  * {@link org.springframework.context.annotation.Bean @Bean}'s javadoc for details
   44 |  * and an example.
   45 |  *
   46 |  * <h3>Ordering</h3>
   47 |  * <p>{@code BeanPostProcessor} beans that are autodetected in an
   48 |  * {@code ApplicationContext} will be ordered according to
   49 |  * {@link org.springframework.core.PriorityOrdered} and
   50 |  * {@link org.springframework.core.Ordered} semantics. In contrast,
   51 |  * {@code BeanPostProcessor} beans that are registered programmatically with a
   52 |  * {@code BeanFactory} will be applied in the order of registration; any ordering
   53 |  * semantics expressed through implementing the
   54 |  * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
   55 |  * programmatically registered post-processors. Furthermore, the
   56 |  * {@link org.springframework.core.annotation.Order @Order} annotation is not
   57 |  * taken into account for {@code BeanPostProcessor} beans.
   58 |  *
   59 |  * @author Juergen Hoeller
   60 |  * @author Sam Brannen
   61 |  * @since 10.10.2003
   62 |  * @see InstantiationAwareBeanPostProcessor
   63 |  * @see DestructionAwareBeanPostProcessor
   64 |  * @see ConfigurableBeanFactory#addBeanPostProcessor
   65 |  * @see BeanFactoryPostProcessor
   66 |  */
   67 | public interface BeanPostProcessor {
   68 | 
   69 | 	/**
   70 | 	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>before</i> any bean
   71 | 	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
   72 | 	 * or a custom init-method). The bean will already be populated with property values.
   73 | 	 * The returned bean instance may be a wrapper around the original.
   74 | 	 * <p>The default implementation returns the given {@code bean} as-is.
   75 | 	 * @param bean the new bean instance
   76 | 	 * @param beanName the name of the bean
   77 | 	 * @return the bean instance to use, either the original or a wrapped one;
   78 | 	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
   79 | 	 * @throws org.springframework.beans.BeansException in case of errors
   80 | 	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
   81 | 	 */
   82 | 	default @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
   83 | 		return bean;
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Apply this {@code BeanPostProcessor} to the given new bean instance <i>after</i> any bean
   88 | 	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
   89 | 	 * or a custom init-method). The bean will already be populated with property values.
   90 | 	 * The returned bean instance may be a wrapper around the original.
   91 | 	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
   92 | 	 * instance and the objects created by the FactoryBean. The
   93 | 	 * post-processor can decide whether to apply to either the FactoryBean or created
   94 | 	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
   95 | 	 * <p>This callback will also be invoked after a short-circuiting triggered by a
   96 | 	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
   97 | 	 * in contrast to all other {@code BeanPostProcessor} callbacks.
   98 | 	 * <p>The default implementation returns the given {@code bean} as-is.
   99 | 	 * @param bean the new bean instance
  100 | 	 * @param beanName the name of the bean
  101 | 	 * @return the bean instance to use, either the original or a wrapped one;
  102 | 	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
  103 | 	 * @throws org.springframework.beans.BeansException in case of errors
  104 | 	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
  105 | 	 * @see org.springframework.beans.factory.FactoryBean
  106 | 	 */
  107 | 	default @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
  108 | 		return bean;
  109 | 	}
  110 | 
  111 | }
```
