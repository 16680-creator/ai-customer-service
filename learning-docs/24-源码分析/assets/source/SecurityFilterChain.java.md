<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/SecurityFilterChain.java -->
<!-- 正文引用：[[源码:SecurityFilterChain.java#L起-L止]] ；代码块头部：(src: SecurityFilterChain.java#L起-L止) -->

# SecurityFilterChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/SecurityFilterChain.java` |
| 行数 | 39 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.160Z |

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
   17 | package org.springframework.security.web;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import jakarta.servlet.Filter;
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | 
   24 | /**
   25 |  * Defines a filter chain which is capable of being matched against an
   26 |  * {@code HttpServletRequest}. in order to decide whether it applies to that request.
   27 |  * <p>
   28 |  * Used to configure a {@code FilterChainProxy}.
   29 |  *
   30 |  * @author Luke Taylor
   31 |  * @since 3.1
   32 |  */
   33 | public interface SecurityFilterChain {
   34 | 
   35 | 	boolean matches(HttpServletRequest request);
   36 | 
   37 | 	List<Filter> getFilters();
   38 | 
   39 | }
```
