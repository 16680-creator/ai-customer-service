<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/support/DefaultTestContextBootstrapper.java -->
<!-- 正文引用：[[源码:DefaultTestContextBootstrapper.java#L起-L止]] ；代码块头部：(src: DefaultTestContextBootstrapper.java#L起-L止) -->

# DefaultTestContextBootstrapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/support/DefaultTestContextBootstrapper.java` |
| 行数 | 40 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.931Z |

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
   17 | package org.springframework.test.context.support;
   18 | 
   19 | import org.springframework.test.context.ContextLoader;
   20 | import org.springframework.test.context.TestContextBootstrapper;
   21 | 
   22 | /**
   23 |  * Default implementation of the {@link TestContextBootstrapper} SPI.
   24 |  *
   25 |  * <p>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
   26 |  *
   27 |  * @author Sam Brannen
   28 |  * @since 4.1
   29 |  */
   30 | public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {
   31 | 
   32 | 	/**
   33 | 	 * Returns {@link DelegatingSmartContextLoader}.
   34 | 	 */
   35 | 	@Override
   36 | 	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
   37 | 		return DelegatingSmartContextLoader.class;
   38 | 	}
   39 | 
   40 | }
```
