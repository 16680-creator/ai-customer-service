<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/ThrowingMethodAuthorizationDeniedHandler.java -->
<!-- 正文引用：[[源码:ThrowingMethodAuthorizationDeniedHandler.java#L起-L止]] ；代码块头部：(src: ThrowingMethodAuthorizationDeniedHandler.java#L起-L止) -->

# ThrowingMethodAuthorizationDeniedHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/ThrowingMethodAuthorizationDeniedHandler.java` |
| 行数 | 50 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:49:26.504Z |

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
   19 | import org.aopalliance.intercept.MethodInvocation;
   20 | 
   21 | import org.springframework.security.authorization.AuthorizationDeniedException;
   22 | import org.springframework.security.authorization.AuthorizationResult;
   23 | 
   24 | /**
   25 |  * An implementation of {@link MethodAuthorizationDeniedHandler} that throws
   26 |  * {@link AuthorizationDeniedException}.
   27 |  *
   28 |  * @author Marcus da Coregio
   29 |  * @since 6.3
   30 |  */
   31 | public final class ThrowingMethodAuthorizationDeniedHandler implements MethodAuthorizationDeniedHandler {
   32 | 
   33 | 	@Override
   34 | 	public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
   35 | 		if (authorizationResult instanceof AuthorizationDeniedException denied) {
   36 | 			throw denied;
   37 | 		}
   38 | 		throw new AuthorizationDeniedException("Access Denied", authorizationResult);
   39 | 	}
   40 | 
   41 | 	@Override
   42 | 	public Object handleDeniedInvocationResult(MethodInvocationResult methodInvocationResult,
   43 | 			AuthorizationResult authorizationResult) {
   44 | 		if (authorizationResult instanceof AuthorizationDeniedException denied) {
   45 | 			throw denied;
   46 | 		}
   47 | 		throw new AuthorizationDeniedException("Access Denied", authorizationResult);
   48 | 	}
   49 | 
   50 | }
```
