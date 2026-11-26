<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/AspectJAutoProxyRegistrar.java -->
<!-- 正文引用：[[源码:AspectJAutoProxyRegistrar.java#L起-L止]] ；代码块头部：(src: AspectJAutoProxyRegistrar.java#L起-L止) -->

# AspectJAutoProxyRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/AspectJAutoProxyRegistrar.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:02.235Z |

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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import org.springframework.aop.config.AopConfigUtils;
   20 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   21 | import org.springframework.core.annotation.AnnotationAttributes;
   22 | import org.springframework.core.type.AnnotationMetadata;
   23 | 
   24 | /**
   25 |  * Registers an {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
   26 |  * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
   27 |  * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
   28 |  *
   29 |  * @author Chris Beams
   30 |  * @author Juergen Hoeller
   31 |  * @since 3.1
   32 |  * @see EnableAspectJAutoProxy
   33 |  */
   34 | class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
   35 | 
   36 | 	/**
   37 | 	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
   38 | 	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
   39 | 	 * {@code @Configuration} class.
   40 | 	 */
   41 | 	@Override
   42 | 	public void registerBeanDefinitions(
   43 | 			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
   44 | 
   45 | 		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
   46 | 
   47 | 		AnnotationAttributes enableAspectJAutoProxy =
   48 | 				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
   49 | 		if (enableAspectJAutoProxy != null) {
   50 | 			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
   51 | 				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
   52 | 			}
   53 | 			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
   54 | 				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
   55 | 			}
   56 | 		}
   57 | 	}
   58 | 
   59 | }
```
