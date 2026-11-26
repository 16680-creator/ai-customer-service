<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/SecuredAuthorizationManager.java -->
<!-- 正文引用：[[源码:SecuredAuthorizationManager.java#L起-L止]] ；代码块头部：(src: SecuredAuthorizationManager.java#L起-L止) -->

# SecuredAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/SecuredAuthorizationManager.java` |
| 行数 | 96 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:07.146Z |

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
   19 | import java.lang.reflect.Method;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | import java.util.concurrent.ConcurrentHashMap;
   25 | import java.util.function.Supplier;
   26 | 
   27 | import org.aopalliance.intercept.MethodInvocation;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.core.MethodClassKey;
   31 | import org.springframework.security.access.annotation.Secured;
   32 | import org.springframework.security.authorization.AuthoritiesAuthorizationManager;
   33 | import org.springframework.security.authorization.AuthorizationManager;
   34 | import org.springframework.security.authorization.AuthorizationResult;
   35 | import org.springframework.security.core.Authentication;
   36 | import org.springframework.security.core.annotation.SecurityAnnotationScanner;
   37 | import org.springframework.security.core.annotation.SecurityAnnotationScanners;
   38 | import org.springframework.util.Assert;
   39 | 
   40 | /**
   41 |  * An {@link AuthorizationManager} which can determine if an {@link Authentication} may
   42 |  * invoke the {@link MethodInvocation} by evaluating if the {@link Authentication}
   43 |  * contains a specified authority from the Spring Security's {@link Secured} annotation.
   44 |  *
   45 |  * @author Evgeniy Cheban
   46 |  * @author DingHao
   47 |  * @since 5.6
   48 |  */
   49 | public final class SecuredAuthorizationManager implements AuthorizationManager<MethodInvocation> {
   50 | 
   51 | 	private AuthorizationManager<Collection<String>> authoritiesAuthorizationManager = new AuthoritiesAuthorizationManager();
   52 | 
   53 | 	private final Map<MethodClassKey, Set<String>> cachedAuthorities = new ConcurrentHashMap<>();
   54 | 
   55 | 	private final SecurityAnnotationScanner<Secured> scanner = SecurityAnnotationScanners.requireUnique(Secured.class);
   56 | 
   57 | 	/**
   58 | 	 * Sets an {@link AuthorizationManager} that accepts a collection of authority
   59 | 	 * strings.
   60 | 	 * @param authoritiesAuthorizationManager the {@link AuthorizationManager} that
   61 | 	 * accepts a collection of authority strings to use
   62 | 	 * @since 6.1
   63 | 	 */
   64 | 	public void setAuthoritiesAuthorizationManager(
   65 | 			AuthorizationManager<Collection<String>> authoritiesAuthorizationManager) {
   66 | 		Assert.notNull(authoritiesAuthorizationManager, "authoritiesAuthorizationManager cannot be null");
   67 | 		this.authoritiesAuthorizationManager = authoritiesAuthorizationManager;
   68 | 	}
   69 | 
   70 | 	@Override
   71 | 	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   72 | 			MethodInvocation mi) {
   73 | 		Set<String> authorities = getAuthorities(mi);
   74 | 		return authorities.isEmpty() ? null
   75 | 				: this.authoritiesAuthorizationManager.authorize(authentication, authorities);
   76 | 	}
   77 | 
   78 | 	private Set<String> getAuthorities(MethodInvocation methodInvocation) {
   79 | 		Method method = methodInvocation.getMethod();
   80 | 		Object target = methodInvocation.getThis();
   81 | 		Class<?> targetClass = (target != null) ? target.getClass() : null;
   82 | 		MethodClassKey cacheKey = new MethodClassKey(method, targetClass);
   83 | 		return this.cachedAuthorities.computeIfAbsent(cacheKey, (k) -> resolveAuthorities(method, targetClass));
   84 | 	}
   85 | 
   86 | 	private Set<String> resolveAuthorities(Method method, @Nullable Class<?> targetClass) {
   87 | 		Secured secured = findSecuredAnnotation(method, targetClass);
   88 | 		return (secured != null) ? Set.of(secured.value()) : Collections.emptySet();
   89 | 	}
   90 | 
   91 | 	private @Nullable Secured findSecuredAnnotation(Method method, @Nullable Class<?> targetClass) {
   92 | 		Class<?> targetClassToUse = (targetClass != null) ? targetClass : method.getDeclaringClass();
   93 | 		return this.scanner.scan(method, targetClassToUse);
   94 | 	}
   95 | 
   96 | }
```
