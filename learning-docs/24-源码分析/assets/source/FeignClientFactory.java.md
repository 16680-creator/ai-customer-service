<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientFactory.java -->
<!-- 正文引用：[[源码:FeignClientFactory.java#L起-L止]] ；代码块头部：(src: FeignClientFactory.java#L起-L止) -->

# FeignClientFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientFactory.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:24.466Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.beans.BeansException;
   25 | import org.springframework.beans.factory.BeanFactoryUtils;
   26 | import org.springframework.cloud.context.named.NamedContextFactory;
   27 | import org.springframework.context.ApplicationContextInitializer;
   28 | import org.springframework.context.support.GenericApplicationContext;
   29 | 
   30 | /**
   31 |  * A factory that creates instances of feign classes. It creates a Spring
   32 |  * ApplicationContext per client name, and extracts the beans that it needs from there.
   33 |  *
   34 |  * @author Spencer Gibb
   35 |  * @author Dave Syer
   36 |  * @author Matt King
   37 |  * @author Jasbir Singh
   38 |  * @author Olga Maciaszek-Sharma
   39 |  */
   40 | public class FeignClientFactory extends NamedContextFactory<FeignClientSpecification> {
   41 | 
   42 | 	public FeignClientFactory() {
   43 | 		this(new HashMap<>());
   44 | 	}
   45 | 
   46 | 	public FeignClientFactory(
   47 | 			Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
   48 | 		super(FeignClientsConfiguration.class, "spring.cloud.openfeign", "spring.cloud.openfeign.client.name",
   49 | 				applicationContextInitializers);
   50 | 	}
   51 | 
   52 | 	public <T> @Nullable T getInstanceWithoutAncestors(String name, Class<T> type) {
   53 | 		try {
   54 | 			return BeanFactoryUtils.beanOfType(getContext(name), type);
   55 | 		}
   56 | 		catch (BeansException ex) {
   57 | 			return null;
   58 | 		}
   59 | 	}
   60 | 
   61 | 	@Nullable public <T> Map<String, T> getInstancesWithoutAncestors(String name, Class<T> type) {
   62 | 		return getContext(name).getBeansOfType(type);
   63 | 	}
   64 | 
   65 | 	public <T> T getInstance(String contextName, String beanName, Class<T> type) {
   66 | 		return getContext(contextName).getBean(beanName, type);
   67 | 	}
   68 | 
   69 | 	@SuppressWarnings("unchecked")
   70 | 	public FeignClientFactory withApplicationContextInitializers(Map<String, Object> applicationContextInitializers) {
   71 | 		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
   72 | 		applicationContextInitializers.keySet()
   73 | 			.forEach(contextId -> convertedInitializers.put(contextId,
   74 | 					(ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers
   75 | 						.get(contextId)));
   76 | 		return new FeignClientFactory(convertedInitializers);
   77 | 	}
   78 | 
   79 | }
```
