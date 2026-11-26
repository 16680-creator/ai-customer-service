<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthorityAuthorizationManager.java -->
<!-- 正文引用：[[源码:AuthorityAuthorizationManager.java#L起-L止]] ；代码块头部：(src: AuthorityAuthorizationManager.java#L起-L止) -->

# AuthorityAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthorityAuthorizationManager.java` |
| 行数 | 152 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.117Z |

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
   19 | import java.util.Set;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
   25 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   26 | import org.springframework.security.core.Authentication;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * An {@link AuthorizationManager} that determines if the current user is authorized by
   31 |  * evaluating if the {@link Authentication} contains a specified authority.
   32 |  *
   33 |  * @param <T> the type of object being authorized.
   34 |  * @author Evgeniy Cheban
   35 |  * @since 5.5
   36 |  * @see AllAuthoritiesAuthorizationManager
   37 |  */
   38 | public final class AuthorityAuthorizationManager<T> implements AuthorizationManager<T> {
   39 | 
   40 | 	private static final String ROLE_PREFIX = "ROLE_";
   41 | 
   42 | 	private final AuthoritiesAuthorizationManager delegate = new AuthoritiesAuthorizationManager();
   43 | 
   44 | 	private final Set<String> authorities;
   45 | 
   46 | 	private AuthorityAuthorizationManager(String... authorities) {
   47 | 		this.authorities = Set.of(authorities);
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * Sets the {@link RoleHierarchy} to be used. Default is {@link NullRoleHierarchy}.
   52 | 	 * Cannot be null.
   53 | 	 * @param roleHierarchy the {@link RoleHierarchy} to use
   54 | 	 * @since 5.8
   55 | 	 */
   56 | 	public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
   57 | 		this.delegate.setRoleHierarchy(roleHierarchy);
   58 | 	}
   59 | 
   60 | 	/**
   61 | 	 * Creates an instance of {@link AuthorityAuthorizationManager} with the provided
   62 | 	 * authority.
   63 | 	 * @param role the authority to check for prefixed with "ROLE_". Role should not start
   64 | 	 * with "ROLE_" since it is automatically prepended already.
   65 | 	 * @param <T> the type of object being authorized
   66 | 	 * @return the new instance
   67 | 	 */
   68 | 	public static <T> AuthorityAuthorizationManager<T> hasRole(String role) {
   69 | 		Assert.notNull(role, "role cannot be null");
   70 | 		Assert.isTrue(!role.startsWith(ROLE_PREFIX), () -> role + " should not start with " + ROLE_PREFIX + " since "
   71 | 				+ ROLE_PREFIX + " is automatically prepended when using hasRole. Consider using hasAuthority instead.");
   72 | 		return hasAuthority(ROLE_PREFIX + role);
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Creates an instance of {@link AuthorityAuthorizationManager} with the provided
   77 | 	 * authority.
   78 | 	 * @param authority the authority to check for
   79 | 	 * @param <T> the type of object being authorized
   80 | 	 * @return the new instance
   81 | 	 */
   82 | 	public static <T> AuthorityAuthorizationManager<T> hasAuthority(String authority) {
   83 | 		Assert.notNull(authority, "authority cannot be null");
   84 | 		return new AuthorityAuthorizationManager<>(authority);
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Creates an instance of {@link AuthorityAuthorizationManager} with the provided
   89 | 	 * authorities.
   90 | 	 * @param roles the authorities to check for prefixed with "ROLE_". Each role should
   91 | 	 * not start with "ROLE_" since it is automatically prepended already.
   92 | 	 * @param <T> the type of object being authorized
   93 | 	 * @return the new instance
   94 | 	 */
   95 | 	public static <T> AuthorityAuthorizationManager<T> hasAnyRole(String... roles) {
   96 | 		return hasAnyRole(ROLE_PREFIX, roles);
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Creates an instance of {@link AuthorityAuthorizationManager} with the provided
  101 | 	 * authorities.
  102 | 	 * @param rolePrefix the role prefix for <code>roles</code>
  103 | 	 * @param roles the authorities to check for prefixed with <code>rolePrefix</code>
  104 | 	 * @param <T> the type of object being authorized
  105 | 	 * @return the new instance
  106 | 	 */
  107 | 	public static <T> AuthorityAuthorizationManager<T> hasAnyRole(String rolePrefix, String[] roles) {
  108 | 		Assert.notNull(rolePrefix, "rolePrefix cannot be null");
  109 | 		Assert.notEmpty(roles, "roles cannot be empty");
  110 | 		Assert.noNullElements(roles, "roles cannot contain null values");
  111 | 		return hasAnyAuthority(toNamedRolesArray(rolePrefix, roles));
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Creates an instance of {@link AuthorityAuthorizationManager} with the provided
  116 | 	 * authorities.
  117 | 	 * @param authorities the authorities to check for
  118 | 	 * @param <T> the type of object being authorized
  119 | 	 * @return the new instance
  120 | 	 */
  121 | 	public static <T> AuthorityAuthorizationManager<T> hasAnyAuthority(String... authorities) {
  122 | 		Assert.notEmpty(authorities, "authorities cannot be empty");
  123 | 		Assert.noNullElements(authorities, "authorities cannot contain null values");
  124 | 		return new AuthorityAuthorizationManager<>(authorities);
  125 | 	}
  126 | 
  127 | 	private static String[] toNamedRolesArray(String rolePrefix, String[] roles) {
  128 | 		String[] result = new String[roles.length];
  129 | 		for (int i = 0; i < roles.length; i++) {
  130 | 			String role = roles[i];
  131 | 			Assert.isTrue(rolePrefix.isEmpty() || !role.startsWith(rolePrefix), () -> role + " should not start with "
  132 | 					+ rolePrefix + " since " + rolePrefix
  133 | 					+ " is automatically prepended when using hasAnyRole. Consider using hasAnyAuthority instead.");
  134 | 			result[i] = rolePrefix + role;
  135 | 		}
  136 | 		return result;
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * {@inheritDoc}
  141 | 	 */
  142 | 	@Override
  143 | 	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, T object) {
  144 | 		return this.delegate.authorize(authentication, this.authorities);
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public String toString() {
  149 | 		return "AuthorityAuthorizationManager[authorities=" + this.authorities + "]";
  150 | 	}
  151 | 
  152 | }
```
