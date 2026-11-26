<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthorizationManager.java -->
<!-- 正文引用：[[源码:AuthorizationManager.java#L起-L止]] ；代码块头部：(src: AuthorizationManager.java#L起-L止) -->

# AuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthorizationManager.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.057Z |

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
   17 | package org.springframework.security.authorization;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.security.access.AccessDeniedException;
   24 | import org.springframework.security.core.Authentication;
   25 | 
   26 | /**
   27 |  * An Authorization manager which can determine if an {@link Authentication} has access to
   28 |  * a specific object.
   29 |  *
   30 |  * @param <T> the type of object that the authorization check is being done on.
   31 |  * @author Evgeniy Cheban
   32 |  */
   33 | @FunctionalInterface
   34 | public interface AuthorizationManager<T extends @Nullable Object> {
   35 | 
   36 | 	/**
   37 | 	 * Determines if access should be granted for a specific authentication and object.
   38 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   39 | 	 * @param object the {@link T} object to check
   40 | 	 * @throws AccessDeniedException if access is not granted
   41 | 	 */
   42 | 	default void verify(Supplier<? extends @Nullable Authentication> authentication, T object) {
   43 | 		AuthorizationResult result = authorize(authentication, object);
   44 | 		if (result != null && !result.isGranted()) {
   45 | 			throw new AuthorizationDeniedException("Access Denied", result);
   46 | 		}
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Determines if access is granted for a specific authentication and object.
   51 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to
   52 | 	 * authorize
   53 | 	 * @param object the {@link T} object to authorize
   54 | 	 * @return an {@link AuthorizationResult}
   55 | 	 * @since 6.4
   56 | 	 */
   57 | 	@Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, T object);
   58 | 
   59 | }
```
