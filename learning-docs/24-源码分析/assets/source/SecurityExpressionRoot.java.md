<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/expression/SecurityExpressionRoot.java -->
<!-- 正文引用：[[源码:SecurityExpressionRoot.java#L起-L止]] ；代码块头部：(src: SecurityExpressionRoot.java#L起-L止) -->

# SecurityExpressionRoot.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/expression/SecurityExpressionRoot.java` |
| 行数 | 311 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.628Z |

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
   17 | package org.springframework.security.access.expression;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.access.PermissionEvaluator;
   25 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   26 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   27 | import org.springframework.security.authorization.AuthorizationManager;
   28 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   29 | import org.springframework.security.authorization.AuthorizationResult;
   30 | import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
   31 | import org.springframework.security.core.Authentication;
   32 | import org.springframework.util.Assert;
   33 | import org.springframework.util.function.SingletonSupplier;
   34 | 
   35 | /**
   36 |  * Base root object for use in Spring Security expression evaluations.
   37 |  *
   38 |  * @author Luke Taylor
   39 |  * @author Evgeniy Cheban
   40 |  * @author Steve Riesenberg
   41 |  * @author Ngoc Nhan
   42 |  * @since 3.0
   43 |  */
   44 | public abstract class SecurityExpressionRoot<T extends @Nullable Object> implements SecurityExpressionOperations {
   45 | 
   46 | 	private final Supplier<Authentication> authentication;
   47 | 
   48 | 	private String defaultRolePrefix = "ROLE_";
   49 | 
   50 | 	private final T object;
   51 | 
   52 | 	private AuthorizationManagerFactory<T> authorizationManagerFactory = new DefaultAuthorizationManagerFactory<>();
   53 | 
   54 | 	/**
   55 | 	 * Allows "permitAll" expression.
   56 | 	 */
   57 | 	public final boolean permitAll = true;
   58 | 
   59 | 	/**
   60 | 	 * Allows "denyAll" expression.
   61 | 	 */
   62 | 	public final boolean denyAll = false;
   63 | 
   64 | 	private PermissionEvaluator permissionEvaluator = new DenyAllPermissionEvaluator();
   65 | 
   66 | 	public final String read = "read";
   67 | 
   68 | 	public final String write = "write";
   69 | 
   70 | 	public final String create = "create";
   71 | 
   72 | 	public final String delete = "delete";
   73 | 
   74 | 	public final String admin = "administration";
   75 | 
   76 | 	/**
   77 | 	 * Creates a new instance.
   78 | 	 * @param authentication the {@link Authentication} to use. Cannot be null.
   79 | 	 * @deprecated Use {@link #SecurityExpressionRoot(Supplier, Object)} instead
   80 | 	 */
   81 | 	@Deprecated(since = "7.0")
   82 | 	@SuppressWarnings("NullAway")
   83 | 	public SecurityExpressionRoot(@Nullable Authentication authentication) {
   84 | 		this(() -> authentication, null);
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Creates a new instance that uses lazy initialization of the {@link Authentication}
   89 | 	 * object.
   90 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to use.
   91 | 	 * Cannot be null.
   92 | 	 * @since 5.8
   93 | 	 * @deprecated Use {@link #SecurityExpressionRoot(Supplier, Object)} instead
   94 | 	 */
   95 | 	@Deprecated(since = "7.0")
   96 | 	@SuppressWarnings("NullAway")
   97 | 	public SecurityExpressionRoot(Supplier<@Nullable Authentication> authentication) {
   98 | 		this(authentication, null);
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Creates a new instance that uses lazy initialization of the {@link Authentication}
  103 | 	 * object.
  104 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to use.
  105 | 	 * Cannot be null.
  106 | 	 * @param object the object being authorized
  107 | 	 * @since 7.0
  108 | 	 */
  109 | 	public SecurityExpressionRoot(Supplier<? extends @Nullable Authentication> authentication, T object) {
  110 | 		this.authentication = SingletonSupplier.of(() -> {
  111 | 			Authentication value = authentication.get();
  112 | 			Assert.notNull(value, "Authentication object cannot be null");
  113 | 			return value;
  114 | 		});
  115 | 		this.object = object;
  116 | 	}
  117 | 
  118 | 	@Override
  119 | 	public final boolean hasAuthority(String authority) {
  120 | 		return isGranted(this.authorizationManagerFactory.hasAuthority(authority));
  121 | 	}
  122 | 
  123 | 	@Override
  124 | 	public final boolean hasAnyAuthority(String... authorities) {
  125 | 		return isGranted(this.authorizationManagerFactory.hasAnyAuthority(authorities));
  126 | 	}
  127 | 
  128 | 	public final boolean hasAllAuthorities(String... authorities) {
  129 | 		AuthorizationManager<T> manager = this.authorizationManagerFactory.hasAllAuthorities(authorities);
  130 | 		return isGranted(manager);
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public final boolean hasRole(String role) {
  135 | 		if (this.authorizationManagerFactory instanceof DefaultAuthorizationManagerFactory<T>) {
  136 | 			// To provide passivity for old behavior where hasRole('ROLE_A') is allowed,
  137 | 			// we strip the role prefix when found.
  138 | 			// TODO: Remove in favor of fixing inconsistent behavior?
  139 | 			String rolePrefix = this.defaultRolePrefix;
  140 | 			if (role.startsWith(rolePrefix)) {
  141 | 				role = role.substring(rolePrefix.length());
  142 | 			}
  143 | 		}
  144 | 		return isGranted(this.authorizationManagerFactory.hasRole(role));
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public final boolean hasAnyRole(String... roles) {
  149 | 		if (this.authorizationManagerFactory instanceof DefaultAuthorizationManagerFactory<T>) {
  150 | 			// To provide passivity for old behavior where hasRole('ROLE_A') is allowed,
  151 | 			// we strip the role prefix when found.
  152 | 			// TODO: Remove in favor of fixing inconsistent behavior?
  153 | 			String rolePrefix = this.defaultRolePrefix;
  154 | 			for (int index = 0; index < roles.length; index++) {
  155 | 				String role = roles[index];
  156 | 				if (role.startsWith(rolePrefix)) {
  157 | 					roles[index] = role.substring(rolePrefix.length());
  158 | 				}
  159 | 			}
  160 | 		}
  161 | 		return isGranted(this.authorizationManagerFactory.hasAnyRole(roles));
  162 | 	}
  163 | 
  164 | 	public final boolean hasAllRoles(String... roles) {
  165 | 		AuthorizationManager<T> manager = this.authorizationManagerFactory.hasAllRoles(roles);
  166 | 		return isGranted(manager);
  167 | 	}
  168 | 
  169 | 	@Override
  170 | 	public final Authentication getAuthentication() {
  171 | 		return this.authentication.get();
  172 | 	}
  173 | 
  174 | 	@Override
  175 | 	public final boolean permitAll() {
  176 | 		return isGranted(this.authorizationManagerFactory.permitAll());
  177 | 	}
  178 | 
  179 | 	@Override
  180 | 	public final boolean denyAll() {
  181 | 		return isGranted(this.authorizationManagerFactory.denyAll());
  182 | 	}
  183 | 
  184 | 	@Override
  185 | 	public final boolean isAnonymous() {
  186 | 		return isGranted(this.authorizationManagerFactory.anonymous());
  187 | 	}
  188 | 
  189 | 	@Override
  190 | 	public final boolean isAuthenticated() {
  191 | 		return isGranted(this.authorizationManagerFactory.authenticated());
  192 | 	}
  193 | 
  194 | 	@Override
  195 | 	public final boolean isRememberMe() {
  196 | 		return isGranted(this.authorizationManagerFactory.rememberMe());
  197 | 	}
  198 | 
  199 | 	@Override
  200 | 	public final boolean isFullyAuthenticated() {
  201 | 		return isGranted(this.authorizationManagerFactory.fullyAuthenticated());
  202 | 	}
  203 | 
  204 | 	private boolean isGranted(AuthorizationManager<T> authorizationManager) {
  205 | 		AuthorizationResult authorizationResult = authorizationManager.authorize(this.authentication, this.object);
  206 | 		return (authorizationResult != null && authorizationResult.isGranted());
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Convenience method to access {@link Authentication#getPrincipal()} from
  211 | 	 * {@link #getAuthentication()}.
  212 | 	 * @return the {@code Principal} being authenticated or the authenticated principal
  213 | 	 * after authentication.
  214 | 	 */
  215 | 	public @Nullable Object getPrincipal() {
  216 | 		return getAuthentication().getPrincipal();
  217 | 	}
  218 | 
  219 | 	/**
  220 | 	 * Sets the {@link AuthenticationTrustResolver} to use.
  221 | 	 * @deprecated Use
  222 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  223 | 	 */
  224 | 	@Deprecated(since = "7.0")
  225 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
  226 | 		getDefaultAuthorizationManagerFactory().setTrustResolver(trustResolver);
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Sets the {@link RoleHierarchy} to use.
  231 | 	 * @deprecated Use
  232 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  233 | 	 */
  234 | 	@Deprecated(since = "7.0")
  235 | 	public void setRoleHierarchy(@Nullable RoleHierarchy roleHierarchy) {
  236 | 		if (roleHierarchy != null) {
  237 | 			getDefaultAuthorizationManagerFactory().setRoleHierarchy(roleHierarchy);
  238 | 		}
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * <p>
  243 | 	 * Sets the default prefix to be added to {@link #hasAnyRole(String...)} or
  244 | 	 * {@link #hasRole(String)}. For example, if hasRole("ADMIN") or hasRole("ROLE_ADMIN")
  245 | 	 * is passed in, then the role ROLE_ADMIN will be used when the defaultRolePrefix is
  246 | 	 * "ROLE_" (default).
  247 | 	 * </p>
  248 | 	 *
  249 | 	 * <p>
  250 | 	 * If null or empty, then no default role prefix is used.
  251 | 	 * </p>
  252 | 	 * @param defaultRolePrefix the default prefix to add to roles. Default "ROLE_".
  253 | 	 * @deprecated Use
  254 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  255 | 	 */
  256 | 	@Deprecated(since = "7.0")
  257 | 	public void setDefaultRolePrefix(@Nullable String defaultRolePrefix) {
  258 | 		if (defaultRolePrefix == null) {
  259 | 			defaultRolePrefix = "";
  260 | 		}
  261 | 		getDefaultAuthorizationManagerFactory().setRolePrefix(defaultRolePrefix);
  262 | 		this.defaultRolePrefix = defaultRolePrefix;
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Sets the {@link AuthorizationManagerFactory} to use for creating instances of
  267 | 	 * {@link AuthorizationManager}.
  268 | 	 * @param authorizationManagerFactory the {@link AuthorizationManagerFactory} to use
  269 | 	 * @since 7.0
  270 | 	 */
  271 | 	public void setAuthorizationManagerFactory(AuthorizationManagerFactory<T> authorizationManagerFactory) {
  272 | 		Assert.notNull(authorizationManagerFactory, "authorizationManagerFactory cannot be null");
  273 | 		this.authorizationManagerFactory = authorizationManagerFactory;
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * Allows accessing the {@link DefaultAuthorizationManagerFactory} for getting and
  278 | 	 * setting defaults. This method will be removed in Spring Security 8.
  279 | 	 * @return the {@link DefaultAuthorizationManagerFactory}
  280 | 	 * @throws IllegalStateException if a different {@link AuthorizationManagerFactory}
  281 | 	 * was already set
  282 | 	 * @deprecated Use
  283 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  284 | 	 */
  285 | 	@Deprecated(since = "7.0", forRemoval = true)
  286 | 	private DefaultAuthorizationManagerFactory<T> getDefaultAuthorizationManagerFactory() {
  287 | 		if (!(this.authorizationManagerFactory instanceof DefaultAuthorizationManagerFactory<T> defaultAuthorizationManagerFactory)) {
  288 | 			throw new IllegalStateException(
  289 | 					"authorizationManagerFactory must be an instance of DefaultAuthorizationManagerFactory");
  290 | 		}
  291 | 
  292 | 		return defaultAuthorizationManagerFactory;
  293 | 	}
  294 | 
  295 | 	@Override
  296 | 	public boolean hasPermission(Object target, Object permission) {
  297 | 		return this.permissionEvaluator.hasPermission(getAuthentication(), target, permission);
  298 | 	}
  299 | 
  300 | 	@Override
  301 | 	public boolean hasPermission(Object targetId, String targetType, Object permission) {
  302 | 		return this.permissionEvaluator.hasPermission(getAuthentication(), (Serializable) targetId, targetType,
  303 | 				permission);
  304 | 	}
  305 | 
  306 | 	public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
  307 | 		Assert.notNull(permissionEvaluator, "permissionEvaluator cannot be null");
  308 | 		this.permissionEvaluator = permissionEvaluator;
  309 | 	}
  310 | 
  311 | }
```
