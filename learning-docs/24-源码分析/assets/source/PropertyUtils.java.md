<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/util/PropertyUtils.java -->
<!-- 正文引用：[[源码:PropertyUtils.java#L起-L止]] ；代码块头部：(src: PropertyUtils.java#L起-L止) -->

# PropertyUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/util/PropertyUtils.java` |
| 行数 | 56 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:53.775Z |

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
   17 | package org.springframework.cloud.util;
   18 | 
   19 | import org.springframework.core.env.Environment;
   20 | import org.springframework.util.ClassUtils;
   21 | 
   22 | public abstract class PropertyUtils {
   23 | 
   24 | 	/**
   25 | 	 * Property name for checking if bootstrap is enabled.
   26 | 	 */
   27 | 	public static final String BOOTSTRAP_ENABLED_PROPERTY = "spring.cloud.bootstrap.enabled";
   28 | 
   29 | 	/**
   30 | 	 * Property name for spring boot legacy processing.
   31 | 	 */
   32 | 	public static final String USE_LEGACY_PROCESSING_PROPERTY = "spring.config.use-legacy-processing";
   33 | 
   34 | 	/**
   35 | 	 * Property name for bootstrap marker class name.
   36 | 	 */
   37 | 	public static final String MARKER_CLASS = "org.springframework.cloud.bootstrap.marker.Marker";
   38 | 
   39 | 	/**
   40 | 	 * Boolean if bootstrap marker class exists.
   41 | 	 */
   42 | 	public static final boolean MARKER_CLASS_EXISTS = ClassUtils.isPresent(MARKER_CLASS, null);
   43 | 
   44 | 	private PropertyUtils() {
   45 | 		throw new UnsupportedOperationException("unable to instantiate utils class");
   46 | 	}
   47 | 
   48 | 	public static boolean bootstrapEnabled(Environment environment) {
   49 | 		return environment.getProperty(BOOTSTRAP_ENABLED_PROPERTY, Boolean.class, false) || MARKER_CLASS_EXISTS;
   50 | 	}
   51 | 
   52 | 	public static boolean useLegacyProcessing(Environment environment) {
   53 | 		return environment.getProperty(USE_LEGACY_PROCESSING_PROPERTY, Boolean.class, false);
   54 | 	}
   55 | 
   56 | }
```
