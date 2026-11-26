<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/bootstrap/config/PropertySourceLocator.java -->
<!-- 正文引用：[[源码:PropertySourceLocator.java#L起-L止]] ；代码块头部：(src: PropertySourceLocator.java#L起-L止) -->

# PropertySourceLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/bootstrap/config/PropertySourceLocator.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:38.228Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.bootstrap.config;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.List;
   23 | 
   24 | import org.springframework.core.env.CompositePropertySource;
   25 | import org.springframework.core.env.Environment;
   26 | import org.springframework.core.env.PropertySource;
   27 | 
   28 | /**
   29 |  * Strategy for locating (possibly remote) property sources for the Environment.
   30 |  * Implementations should not fail unless they intend to prevent the application from
   31 |  * starting.
   32 |  *
   33 |  * @author Dave Syer
   34 |  *
   35 |  */
   36 | public interface PropertySourceLocator {
   37 | 
   38 | 	/**
   39 | 	 * @param environment The current Environment.
   40 | 	 * @return A PropertySource, or null if there is none.
   41 | 	 * @throws IllegalStateException if there is a fail-fast condition.
   42 | 	 */
   43 | 	PropertySource<?> locate(Environment environment);
   44 | 
   45 | 	default Collection<PropertySource<?>> locateCollection(Environment environment) {
   46 | 		return locateCollection(this, environment);
   47 | 	}
   48 | 
   49 | 	static Collection<PropertySource<?>> locateCollection(PropertySourceLocator locator, Environment environment) {
   50 | 		PropertySource<?> propertySource = locator.locate(environment);
   51 | 		if (propertySource == null) {
   52 | 			return Collections.emptyList();
   53 | 		}
   54 | 		if (propertySource instanceof CompositePropertySource compositePropertySource) {
   55 | 			Collection<PropertySource<?>> sources = compositePropertySource.getPropertySources();
   56 | 			List<PropertySource<?>> filteredSources = new ArrayList<>();
   57 | 			for (PropertySource<?> p : sources) {
   58 | 				if (p != null) {
   59 | 					filteredSources.add(p);
   60 | 				}
   61 | 			}
   62 | 			return filteredSources;
   63 | 		}
   64 | 		else {
   65 | 			return List.of(propertySource);
   66 | 		}
   67 | 	}
   68 | 
   69 | }
```
