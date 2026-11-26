<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/bean/override/BeanOverrideContextCustomizer.java -->
<!-- 正文引用：[[源码:BeanOverrideContextCustomizer.java#L起-L止]] ；代码块头部：(src: BeanOverrideContextCustomizer.java#L起-L止) -->

# BeanOverrideContextCustomizer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/bean/override/BeanOverrideContextCustomizer.java` |
| 行数 | 90 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:34.809Z |

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
   17 | package org.springframework.test.context.bean.override;
   18 | 
   19 | import java.util.Set;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   24 | import org.springframework.context.ConfigurableApplicationContext;
   25 | import org.springframework.test.context.ContextCustomizer;
   26 | import org.springframework.test.context.MergedContextConfiguration;
   27 | 
   28 | /**
   29 |  * {@link ContextCustomizer} implementation that registers the necessary
   30 |  * infrastructure to support {@linkplain BeanOverride Bean Overrides}.
   31 |  *
   32 |  * @author Simon Baslé
   33 |  * @author Stephane Nicoll
   34 |  * @author Sam Brannen
   35 |  * @since 6.2
   36 |  */
   37 | class BeanOverrideContextCustomizer implements ContextCustomizer {
   38 | 
   39 | 	private static final String INFRASTRUCTURE_BEAN_NAME =
   40 | 			"org.springframework.test.context.bean.override.internalBeanOverridePostProcessor";
   41 | 
   42 | 	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME =
   43 | 			"org.springframework.test.context.bean.override.internalWrapEarlyBeanPostProcessor";
   44 | 
   45 | 
   46 | 	private final Set<BeanOverrideHandler> handlers;
   47 | 
   48 | 	BeanOverrideContextCustomizer(Set<BeanOverrideHandler> handlers) {
   49 | 		this.handlers = handlers;
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
   54 | 		ConfigurableBeanFactory beanFactory = context.getBeanFactory();
   55 | 		// Since all three Bean Override infrastructure beans are never injected as
   56 | 		// dependencies into other beans within the ApplicationContext, it is sufficient
   57 | 		// to register them as manual singleton instances. In addition, registration of
   58 | 		// the BeanOverrideBeanFactoryPostProcessor as a singleton is a requirement for
   59 | 		// AOT processing, since a bean definition cannot be generated for the
   60 | 		// Set<BeanOverrideHandler> argument that it accepts in its constructor.
   61 | 		BeanOverrideRegistry beanOverrideRegistry = new BeanOverrideRegistry(beanFactory);
   62 | 		beanFactory.registerSingleton(BeanOverrideRegistry.BEAN_NAME, beanOverrideRegistry);
   63 | 		beanFactory.registerSingleton(INFRASTRUCTURE_BEAN_NAME,
   64 | 				new BeanOverrideBeanFactoryPostProcessor(this.handlers, beanOverrideRegistry));
   65 | 		beanFactory.registerSingleton(EARLY_INFRASTRUCTURE_BEAN_NAME,
   66 | 				new WrapEarlyBeanPostProcessor(beanOverrideRegistry));
   67 | 	}
   68 | 
   69 | 	Set<BeanOverrideHandler> getBeanOverrideHandlers() {
   70 | 		return this.handlers;
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public boolean equals(@Nullable Object other) {
   75 | 		if (other == this) {
   76 | 			return true;
   77 | 		}
   78 | 		if (other == null || other.getClass() != getClass()) {
   79 | 			return false;
   80 | 		}
   81 | 		BeanOverrideContextCustomizer that = (BeanOverrideContextCustomizer) other;
   82 | 		return this.handlers.equals(that.handlers);
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public int hashCode() {
   87 | 		return this.handlers.hashCode();
   88 | 	}
   89 | 
   90 | }
```
