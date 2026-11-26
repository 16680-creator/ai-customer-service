<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/support/ApplicationContextAwareProcessor.java -->
<!-- 正文引用：[[源码:ApplicationContextAwareProcessor.java#L起-L止]] ；代码块头部：(src: ApplicationContextAwareProcessor.java#L起-L止) -->

# ApplicationContextAwareProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/support/ApplicationContextAwareProcessor.java` |
| 行数 | 114 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:03.640Z |

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
   17 | package org.springframework.context.support;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.beans.BeansException;
   22 | import org.springframework.beans.factory.Aware;
   23 | import org.springframework.beans.factory.config.BeanPostProcessor;
   24 | import org.springframework.beans.factory.config.EmbeddedValueResolver;
   25 | import org.springframework.context.ApplicationContextAware;
   26 | import org.springframework.context.ApplicationEventPublisherAware;
   27 | import org.springframework.context.ApplicationStartupAware;
   28 | import org.springframework.context.ConfigurableApplicationContext;
   29 | import org.springframework.context.EmbeddedValueResolverAware;
   30 | import org.springframework.context.EnvironmentAware;
   31 | import org.springframework.context.MessageSourceAware;
   32 | import org.springframework.context.ResourceLoaderAware;
   33 | import org.springframework.util.StringValueResolver;
   34 | 
   35 | /**
   36 |  * {@link BeanPostProcessor} implementation that supplies the
   37 |  * {@link org.springframework.context.ApplicationContext ApplicationContext},
   38 |  * {@link org.springframework.core.env.Environment Environment},
   39 |  * {@link StringValueResolver}, or
   40 |  * {@link org.springframework.core.metrics.ApplicationStartup ApplicationStartup}
   41 |  * for the {@code ApplicationContext} to beans that implement the {@link EnvironmentAware},
   42 |  * {@link EmbeddedValueResolverAware}, {@link ResourceLoaderAware},
   43 |  * {@link ApplicationEventPublisherAware}, {@link MessageSourceAware},
   44 |  * {@link ApplicationStartupAware}, and/or {@link ApplicationContextAware} interfaces.
   45 |  *
   46 |  * <p>Implemented interfaces are satisfied in the order in which they are
   47 |  * mentioned above.
   48 |  *
   49 |  * <p>Application contexts will automatically register this with their
   50 |  * underlying bean factory. Applications do not use this directly.
   51 |  *
   52 |  * @author Juergen Hoeller
   53 |  * @author Costin Leau
   54 |  * @author Chris Beams
   55 |  * @author Sam Brannen
   56 |  * @since 10.10.2003
   57 |  * @see org.springframework.context.EnvironmentAware
   58 |  * @see org.springframework.context.EmbeddedValueResolverAware
   59 |  * @see org.springframework.context.ResourceLoaderAware
   60 |  * @see org.springframework.context.ApplicationEventPublisherAware
   61 |  * @see org.springframework.context.MessageSourceAware
   62 |  * @see org.springframework.context.ApplicationStartupAware
   63 |  * @see org.springframework.context.ApplicationContextAware
   64 |  * @see org.springframework.context.support.AbstractApplicationContext#refresh()
   65 |  */
   66 | class ApplicationContextAwareProcessor implements BeanPostProcessor {
   67 | 
   68 | 	private final ConfigurableApplicationContext applicationContext;
   69 | 
   70 | 	private final StringValueResolver embeddedValueResolver;
   71 | 
   72 | 
   73 | 	/**
   74 | 	 * Create a new ApplicationContextAwareProcessor for the given context.
   75 | 	 */
   76 | 	public ApplicationContextAwareProcessor(ConfigurableApplicationContext applicationContext) {
   77 | 		this.applicationContext = applicationContext;
   78 | 		this.embeddedValueResolver = new EmbeddedValueResolver(applicationContext.getBeanFactory());
   79 | 	}
   80 | 
   81 | 
   82 | 	@Override
   83 | 	public @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
   84 | 		if (bean instanceof Aware) {
   85 | 			invokeAwareInterfaces(bean);
   86 | 		}
   87 | 		return bean;
   88 | 	}
   89 | 
   90 | 	private void invokeAwareInterfaces(Object bean) {
   91 | 		if (bean instanceof EnvironmentAware environmentAware) {
   92 | 			environmentAware.setEnvironment(this.applicationContext.getEnvironment());
   93 | 		}
   94 | 		if (bean instanceof EmbeddedValueResolverAware embeddedValueResolverAware) {
   95 | 			embeddedValueResolverAware.setEmbeddedValueResolver(this.embeddedValueResolver);
   96 | 		}
   97 | 		if (bean instanceof ResourceLoaderAware resourceLoaderAware) {
   98 | 			resourceLoaderAware.setResourceLoader(this.applicationContext);
   99 | 		}
  100 | 		if (bean instanceof ApplicationEventPublisherAware applicationEventPublisherAware) {
  101 | 			applicationEventPublisherAware.setApplicationEventPublisher(this.applicationContext);
  102 | 		}
  103 | 		if (bean instanceof MessageSourceAware messageSourceAware) {
  104 | 			messageSourceAware.setMessageSource(this.applicationContext);
  105 | 		}
  106 | 		if (bean instanceof ApplicationStartupAware applicationStartupAware) {
  107 | 			applicationStartupAware.setApplicationStartup(this.applicationContext.getApplicationStartup());
  108 | 		}
  109 | 		if (bean instanceof ApplicationContextAware applicationContextAware) {
  110 | 			applicationContextAware.setApplicationContext(this.applicationContext);
  111 | 		}
  112 | 	}
  113 | 
  114 | }
```
