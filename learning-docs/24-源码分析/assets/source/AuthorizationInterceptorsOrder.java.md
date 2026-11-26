<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/AuthorizationInterceptorsOrder.java -->
<!-- 正文引用：[[源码:AuthorizationInterceptorsOrder.java#L起-L止]] ；代码块头部：(src: AuthorizationInterceptorsOrder.java#L起-L止) -->

# AuthorizationInterceptorsOrder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/AuthorizationInterceptorsOrder.java` |
| 行数 | 73 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.537Z |

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
   19 | import org.springframework.aop.Advisor;
   20 | 
   21 | /**
   22 |  * Ordering of Spring Security's authorization {@link Advisor}s.
   23 |  *
   24 |  * @author Josh Cummings
   25 |  * @since 5.6
   26 |  * @see PreAuthorizeAuthorizationManager
   27 |  * @see PostAuthorizeAuthorizationManager
   28 |  * @see SecuredAuthorizationManager
   29 |  * @see Jsr250AuthorizationManager
   30 |  */
   31 | public enum AuthorizationInterceptorsOrder {
   32 | 
   33 | 	FIRST(Integer.MIN_VALUE),
   34 | 
   35 | 	/**
   36 | 	 * The order for the {@link PreFilterAuthorizationMethodInterceptor}.
   37 | 	 */
   38 | 	PRE_FILTER,
   39 | 
   40 | 	PRE_AUTHORIZE,
   41 | 
   42 | 	SECURED,
   43 | 
   44 | 	JSR250,
   45 | 
   46 | 	SECURE_RESULT(450),
   47 | 
   48 | 	POST_AUTHORIZE(500),
   49 | 
   50 | 	/**
   51 | 	 * The order for the {@link PostFilterAuthorizationMethodInterceptor}.
   52 | 	 */
   53 | 	POST_FILTER(600),
   54 | 
   55 | 	LAST(Integer.MAX_VALUE);
   56 | 
   57 | 	private static final int INTERVAL = 100;
   58 | 
   59 | 	private final int order;
   60 | 
   61 | 	AuthorizationInterceptorsOrder() {
   62 | 		this.order = ordinal() * INTERVAL;
   63 | 	}
   64 | 
   65 | 	AuthorizationInterceptorsOrder(int order) {
   66 | 		this.order = order;
   67 | 	}
   68 | 
   69 | 	public int getOrder() {
   70 | 		return this.order;
   71 | 	}
   72 | 
   73 | }
```
