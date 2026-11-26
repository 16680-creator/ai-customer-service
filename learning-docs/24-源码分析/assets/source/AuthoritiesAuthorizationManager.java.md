<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthoritiesAuthorizationManager.java -->
<!-- 正文引用：[[源码:AuthoritiesAuthorizationManager.java#L起-L止]] ；代码块头部：(src: AuthoritiesAuthorizationManager.java#L起-L止) -->

# AuthoritiesAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthoritiesAuthorizationManager.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:06.801Z |

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
   19 | import java.util.Collection;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
   25 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   26 | import org.springframework.security.core.Authentication;
   27 | import org.springframework.security.core.GrantedAuthority;
   28 | import org.springframework.security.core.authority.AuthorityUtils;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * An {@link AuthorizationManager} that determines if the current user is authorized by
   33 |  * evaluating if the {@link Authentication} contains any of the specified authorities.
   34 |  *
   35 |  * @author Evgeniy Cheban
   36 |  * @since 6.1
   37 |  */
   38 | public final class AuthoritiesAuthorizationManager implements AuthorizationManager<Collection<String>> {
   39 | 
   40 | 	private RoleHierarchy roleHierarchy = new NullRoleHierarchy();
   41 | 
   42 | 	/**
   43 | 	 * Sets the {@link RoleHierarchy} to be used. Default is {@link NullRoleHierarchy}.
   44 | 	 * Cannot be null.
   45 | 	 * @param roleHierarchy the {@link RoleHierarchy} to use
   46 | 	 */
   47 | 	public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
   48 | 		Assert.notNull(roleHierarchy, "roleHierarchy cannot be null");
   49 | 		this.roleHierarchy = roleHierarchy;
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Determines if the current user is authorized by evaluating if the
   54 | 	 * {@link Authentication} contains any of specified authorities.
   55 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   56 | 	 * @param authorities the collection of authority strings to check
   57 | 	 * @return an {@link AuthorityAuthorizationDecision}
   58 | 	 */
   59 | 	@Override
   60 | 	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   61 | 			Collection<String> authorities) {
   62 | 		boolean granted = isGranted(authentication.get(), authorities);
   63 | 		return new AuthorityAuthorizationDecision(granted, AuthorityUtils.createAuthorityList(authorities));
   64 | 	}
   65 | 
   66 | 	private boolean isGranted(Authentication authentication, Collection<String> authorities) {
   67 | 		return authentication != null && isAuthorized(authentication, authorities);
   68 | 	}
   69 | 
   70 | 	private boolean isAuthorized(Authentication authentication, Collection<String> authorities) {
   71 | 		for (GrantedAuthority grantedAuthority : getGrantedAuthorities(authentication)) {
   72 | 			String authority = grantedAuthority.getAuthority();
   73 | 			if (authority == null) {
   74 | 				continue;
   75 | 			}
   76 | 			if (authorities.contains(authority)) {
   77 | 				return true;
   78 | 			}
   79 | 		}
   80 | 		return false;
   81 | 	}
   82 | 
   83 | 	private Collection<? extends GrantedAuthority> getGrantedAuthorities(Authentication authentication) {
   84 | 		return this.roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities());
   85 | 	}
   86 | 
   87 | }
```
