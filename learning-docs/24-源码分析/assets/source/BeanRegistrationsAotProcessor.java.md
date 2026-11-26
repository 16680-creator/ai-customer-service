<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/aot/BeanRegistrationsAotProcessor.java -->
<!-- 正文引用：[[源码:BeanRegistrationsAotProcessor.java#L起-L止]] ；代码块头部：(src: BeanRegistrationsAotProcessor.java#L起-L止) -->

# BeanRegistrationsAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/aot/BeanRegistrationsAotProcessor.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.179Z |

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
   17 | package org.springframework.beans.factory.aot;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
   25 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   26 | import org.springframework.beans.factory.support.RegisteredBean;
   27 | 
   28 | /**
   29 |  * {@link BeanFactoryInitializationAotProcessor} that contributes code to
   30 |  * register beans.
   31 |  *
   32 |  * @author Phillip Webb
   33 |  * @author Sebastien Deleuze
   34 |  * @author Stephane Nicoll
   35 |  * @author Brian Clozel
   36 |  * @since 6.0
   37 |  */
   38 | class BeanRegistrationsAotProcessor implements BeanFactoryInitializationAotProcessor {
   39 | 
   40 | 	@Override
   41 | 	public @Nullable BeanRegistrationsAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
   42 | 		BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory =
   43 | 				new BeanDefinitionMethodGeneratorFactory(beanFactory);
   44 | 		List<Registration> registrations = new ArrayList<>();
   45 | 
   46 | 		for (String beanName : beanFactory.getBeanDefinitionNames()) {
   47 | 			RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
   48 | 			BeanDefinitionMethodGenerator beanDefinitionMethodGenerator =
   49 | 					beanDefinitionMethodGeneratorFactory.getBeanDefinitionMethodGenerator(registeredBean);
   50 | 			if (beanDefinitionMethodGenerator != null) {
   51 | 				registrations.add(new Registration(registeredBean, beanDefinitionMethodGenerator,
   52 | 						beanFactory.getAliases(beanName)));
   53 | 			}
   54 | 		}
   55 | 
   56 | 		if (registrations.isEmpty()) {
   57 | 			return null;
   58 | 		}
   59 | 		return new BeanRegistrationsAotContribution(registrations);
   60 | 	}
   61 | 
   62 | }
```
