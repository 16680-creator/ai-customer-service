<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/prepost/PreAuthorize.java -->
<!-- 正文引用：[[源码:PreAuthorize.java#L起-L止]] ；代码块头部：(src: PreAuthorize.java#L起-L止) -->

# PreAuthorize.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/prepost/PreAuthorize.java` |
| 行数 | 46 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.797Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.access.prepost;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | /**
   27 |  * Annotation for specifying a method access-control expression which will be evaluated to
   28 |  * decide whether a method invocation is allowed or not.
   29 |  *
   30 |  * @author Luke Taylor
   31 |  * @since 3.0
   32 |  */
   33 | @Target({ ElementType.METHOD, ElementType.TYPE })
   34 | @Retention(RetentionPolicy.RUNTIME)
   35 | @Inherited
   36 | @Documented
   37 | public @interface PreAuthorize {
   38 | 
   39 | 	/**
   40 | 	 * Returns the Spring-EL expression to be evaluated.
   41 | 	 * @return the Spring-EL expression to be evaluated before invoking the protected
   42 | 	 * method
   43 | 	 */
   44 | 	String value();
   45 | 
   46 | }
```
