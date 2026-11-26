<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/AuthorizationAdvisor.java -->
<!-- 正文引用：[[源码:AuthorizationAdvisor.java#L起-L止]] ；代码块头部：(src: AuthorizationAdvisor.java#L起-L止) -->

# AuthorizationAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/AuthorizationAdvisor.java` |
| 行数 | 37 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.380Z |

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
   17 | package org.springframework.security.authorization.method;
   18 | 
   19 | import org.aopalliance.intercept.MethodInterceptor;
   20 | 
   21 | import org.springframework.aop.PointcutAdvisor;
   22 | import org.springframework.aop.framework.AopInfrastructureBean;
   23 | import org.springframework.core.Ordered;
   24 | 
   25 | /**
   26 |  * An interface that indicates method security advice.
   27 |  *
   28 |  * @author Josh Cummings
   29 |  * @since 6.3
   30 |  * @see AuthorizationManagerBeforeMethodInterceptor
   31 |  * @see AuthorizationManagerAfterMethodInterceptor
   32 |  * @see PreFilterAuthorizationMethodInterceptor
   33 |  * @see PostFilterAuthorizationMethodInterceptor
   34 |  */
   35 | public interface AuthorizationAdvisor extends Ordered, MethodInterceptor, PointcutAdvisor, AopInfrastructureBean {
   36 | 
   37 | }
```
