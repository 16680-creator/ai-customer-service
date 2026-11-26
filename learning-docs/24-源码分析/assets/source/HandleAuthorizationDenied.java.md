<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/HandleAuthorizationDenied.java -->
<!-- 正文引用：[[源码:HandleAuthorizationDenied.java#L起-L止]] ；代码块头部：(src: HandleAuthorizationDenied.java#L起-L止) -->

# HandleAuthorizationDenied.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/HandleAuthorizationDenied.java` |
| 行数 | 50 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.864Z |

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
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | /**
   27 |  * Annotation for specifying handling behavior when an authorization denied happens in
   28 |  * method security or an
   29 |  * {@link org.springframework.security.authorization.AuthorizationDeniedException} is
   30 |  * thrown during method invocation.
   31 |  *
   32 |  * @author Marcus da Coregio
   33 |  * @since 6.3
   34 |  * @see AuthorizationManagerAfterMethodInterceptor
   35 |  * @see AuthorizationManagerBeforeMethodInterceptor
   36 |  */
   37 | @Target({ ElementType.METHOD, ElementType.TYPE })
   38 | @Retention(RetentionPolicy.RUNTIME)
   39 | @Inherited
   40 | @Documented
   41 | public @interface HandleAuthorizationDenied {
   42 | 
   43 | 	/**
   44 | 	 * The {@link MethodAuthorizationDeniedHandler} used to handle denied authorization
   45 | 	 * results.
   46 | 	 * @return
   47 | 	 */
   48 | 	Class<? extends MethodAuthorizationDeniedHandler> handlerClass() default ThrowingMethodAuthorizationDeniedHandler.class;
   49 | 
   50 | }
```
