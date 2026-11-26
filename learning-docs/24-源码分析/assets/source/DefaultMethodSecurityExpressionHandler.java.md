<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/expression/method/DefaultMethodSecurityExpressionHandler.java -->
<!-- 正文引用：[[源码:DefaultMethodSecurityExpressionHandler.java#L起-L止]] ；代码块头部：(src: DefaultMethodSecurityExpressionHandler.java#L起-L止) -->

# DefaultMethodSecurityExpressionHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/expression/method/DefaultMethodSecurityExpressionHandler.java` |
| 行数 | 328 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:47:22.891Z |

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
   17 | package org.springframework.security.access.expression.method;
   18 | 
   19 | import java.lang.reflect.Array;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collection;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.function.Supplier;
   27 | import java.util.stream.Stream;
   28 | 
   29 | import org.aopalliance.intercept.MethodInvocation;
   30 | import org.apache.commons.logging.Log;
   31 | import org.apache.commons.logging.LogFactory;
   32 | import org.jspecify.annotations.Nullable;
   33 | 
   34 | import org.springframework.core.ParameterNameDiscoverer;
   35 | import org.springframework.core.log.LogMessage;
   36 | import org.springframework.expression.EvaluationContext;
   37 | import org.springframework.expression.Expression;
   38 | import org.springframework.expression.TypedValue;
   39 | import org.springframework.expression.spel.support.StandardEvaluationContext;
   40 | import org.springframework.security.access.PermissionCacheOptimizer;
   41 | import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
   42 | import org.springframework.security.access.expression.ExpressionUtils;
   43 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   44 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   45 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   46 | import org.springframework.security.core.Authentication;
   47 | import org.springframework.security.core.parameters.DefaultSecurityParameterNameDiscoverer;
   48 | import org.springframework.util.Assert;
   49 | 
   50 | /**
   51 |  * The standard implementation of {@code MethodSecurityExpressionHandler}.
   52 |  * <p>
   53 |  * A single instance should usually be shared amongst the beans that require expression
   54 |  * support.
   55 |  *
   56 |  * @author Luke Taylor
   57 |  * @author Evgeniy Cheban
   58 |  * @author Blagoja Stamatovski
   59 |  * @author Steve Riesenberg
   60 |  * @since 3.0
   61 |  */
   62 | public class DefaultMethodSecurityExpressionHandler extends AbstractSecurityExpressionHandler<MethodInvocation>
   63 | 		implements MethodSecurityExpressionHandler {
   64 | 
   65 | 	private static final String DEFAULT_ROLE_PREFIX = "ROLE_";
   66 | 
   67 | 	protected final Log logger = LogFactory.getLog(getClass());
   68 | 
   69 | 	private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
   70 | 
   71 | 	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultSecurityParameterNameDiscoverer();
   72 | 
   73 | 	private @Nullable PermissionCacheOptimizer permissionCacheOptimizer = null;
   74 | 
   75 | 	private String defaultRolePrefix = DEFAULT_ROLE_PREFIX;
   76 | 
   77 | 	public DefaultMethodSecurityExpressionHandler() {
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Uses a {@link MethodSecurityEvaluationContext} as the <tt>EvaluationContext</tt>
   82 | 	 * implementation.
   83 | 	 */
   84 | 	@Override
   85 | 	public StandardEvaluationContext createEvaluationContextInternal(@Nullable Authentication auth,
   86 | 			MethodInvocation mi) {
   87 | 		return new MethodSecurityEvaluationContext(auth, mi, getParameterNameDiscoverer());
   88 | 	}
   89 | 
   90 | 	@Override
   91 | 	public EvaluationContext createEvaluationContext(Supplier<? extends @Nullable Authentication> authentication,
   92 | 			MethodInvocation mi) {
   93 | 		MethodSecurityExpressionOperations root = createSecurityExpressionRoot(authentication, mi);
   94 | 		MethodSecurityEvaluationContext ctx = new MethodSecurityEvaluationContext(root, mi,
   95 | 				getParameterNameDiscoverer());
   96 | 		ctx.setBeanResolver(getBeanResolver());
   97 | 		return ctx;
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Creates the root object for expression evaluation.
  102 | 	 */
  103 | 	@Override
  104 | 	protected MethodSecurityExpressionOperations createSecurityExpressionRoot(@Nullable Authentication authentication,
  105 | 			MethodInvocation invocation) {
  106 | 		return createSecurityExpressionRoot(() -> authentication, invocation);
  107 | 	}
  108 | 
  109 | 	private MethodSecurityExpressionOperations createSecurityExpressionRoot(
  110 | 			Supplier<? extends @Nullable Authentication> authentication, MethodInvocation invocation) {
  111 | 		MethodSecurityExpressionRoot root = new MethodSecurityExpressionRoot(authentication, invocation);
  112 | 		root.setThis(invocation.getThis());
  113 | 		root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
  114 | 		root.setPermissionEvaluator(getPermissionEvaluator());
  115 | 		if (!DEFAULT_ROLE_PREFIX.equals(this.defaultRolePrefix)) {
  116 | 			// Ensure SecurityExpressionRoot can strip the custom role prefix
  117 | 			root.setDefaultRolePrefix(getDefaultRolePrefix());
  118 | 		}
  119 | 		return root;
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Filters the {@code filterTarget} object (which must be either a {@link Collection},
  124 | 	 * {@code Array}, {@link Map} or {@link Stream}), by evaluating the supplied
  125 | 	 * expression.
  126 | 	 * <p>
  127 | 	 * Returns new instances of the same type as the supplied {@code filterTarget} object
  128 | 	 * @return The filtered {@link Collection}, {@code Array}, {@link Map} or
  129 | 	 * {@link Stream}
  130 | 	 */
  131 | 	@Override
  132 | 	public Object filter(@Nullable Object filterTarget, Expression filterExpression, EvaluationContext ctx) {
  133 | 		MethodSecurityExpressionOperations rootObject = (MethodSecurityExpressionOperations) ctx.getRootObject()
  134 | 			.getValue();
  135 | 		Assert.notNull(rootObject, "rootObject cannot be null");
  136 | 		this.logger.debug(LogMessage.format("Filtering with expression: %s", filterExpression.getExpressionString()));
  137 | 		if (filterTarget instanceof Collection) {
  138 | 			return filterCollection((Collection<?>) filterTarget, filterExpression, ctx, rootObject);
  139 | 		}
  140 | 		if (filterTarget != null && filterTarget.getClass().isArray()) {
  141 | 			return filterArray((Object[]) filterTarget, filterExpression, ctx, rootObject);
  142 | 		}
  143 | 		if (filterTarget instanceof Map) {
  144 | 			return filterMap((Map<?, ?>) filterTarget, filterExpression, ctx, rootObject);
  145 | 		}
  146 | 		if (filterTarget instanceof Stream) {
  147 | 			return filterStream((Stream<?>) filterTarget, filterExpression, ctx, rootObject);
  148 | 		}
  149 | 		throw new IllegalArgumentException(
  150 | 				"Filter target must be a collection, array, map or stream type, but was " + filterTarget);
  151 | 	}
  152 | 
  153 | 	private <T> Object filterCollection(Collection<T> filterTarget, Expression filterExpression, EvaluationContext ctx,
  154 | 			MethodSecurityExpressionOperations rootObject) {
  155 | 		this.logger.debug(LogMessage.format("Filtering collection with %s elements", filterTarget.size()));
  156 | 		List<T> retain = new ArrayList<>(filterTarget.size());
  157 | 		if (this.permissionCacheOptimizer != null) {
  158 | 			this.permissionCacheOptimizer.cachePermissionsFor(rootObject.getAuthentication(), filterTarget);
  159 | 		}
  160 | 		for (T filterObject : filterTarget) {
  161 | 			rootObject.setFilterObject(filterObject);
  162 | 			if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
  163 | 				retain.add(filterObject);
  164 | 			}
  165 | 		}
  166 | 		this.logger.debug(LogMessage.format("Retaining elements: %s", retain));
  167 | 		try {
  168 | 			filterTarget.clear();
  169 | 			filterTarget.addAll(retain);
  170 | 			return filterTarget;
  171 | 		}
  172 | 		catch (UnsupportedOperationException readonly) {
  173 | 			this.logger.trace(LogMessage.format(
  174 | 					"Collection threw exception: %s. Will return a new instance instead of mutating its state.",
  175 | 					readonly.getMessage()));
  176 | 			return retain;
  177 | 		}
  178 | 	}
  179 | 
  180 | 	private Object filterArray(Object[] filterTarget, Expression filterExpression, EvaluationContext ctx,
  181 | 			MethodSecurityExpressionOperations rootObject) {
  182 | 		List<Object> retain = new ArrayList<>(filterTarget.length);
  183 | 		this.logger.debug(LogMessage.format("Filtering array with %s elements", filterTarget.length));
  184 | 		if (this.permissionCacheOptimizer != null) {
  185 | 			this.permissionCacheOptimizer.cachePermissionsFor(rootObject.getAuthentication(),
  186 | 					Arrays.asList(filterTarget));
  187 | 		}
  188 | 		for (Object filterObject : filterTarget) {
  189 | 			rootObject.setFilterObject(filterObject);
  190 | 			if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
  191 | 				retain.add(filterObject);
  192 | 			}
  193 | 		}
  194 | 		this.logger.debug(LogMessage.format("Retaining elements: %s", retain));
  195 | 		Object[] filtered = (Object[]) Array.newInstance(filterTarget.getClass().getComponentType(), retain.size());
  196 | 		for (int i = 0; i < retain.size(); i++) {
  197 | 			filtered[i] = retain.get(i);
  198 | 		}
  199 | 		return filtered;
  200 | 	}
  201 | 
  202 | 	private <K, V> Object filterMap(Map<K, V> filterTarget, Expression filterExpression, EvaluationContext ctx,
  203 | 			MethodSecurityExpressionOperations rootObject) {
  204 | 		Map<K, V> retain = new LinkedHashMap<>(filterTarget.size());
  205 | 		this.logger.debug(LogMessage.format("Filtering map with %s elements", filterTarget.size()));
  206 | 		for (Map.Entry<K, V> filterObject : filterTarget.entrySet()) {
  207 | 			rootObject.setFilterObject(filterObject);
  208 | 			if (ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)) {
  209 | 				retain.put(filterObject.getKey(), filterObject.getValue());
  210 | 			}
  211 | 		}
  212 | 		this.logger.debug(LogMessage.format("Retaining elements: %s", retain));
  213 | 		try {
  214 | 			filterTarget.clear();
  215 | 			filterTarget.putAll(retain);
  216 | 			return filterTarget;
  217 | 		}
  218 | 		catch (UnsupportedOperationException readonly) {
  219 | 			this.logger.trace(LogMessage.format(
  220 | 					"Map threw exception: %s. Will return a new instance instead of mutating its state.",
  221 | 					readonly.getMessage()));
  222 | 			return retain;
  223 | 		}
  224 | 	}
  225 | 
  226 | 	private Object filterStream(final Stream<?> filterTarget, Expression filterExpression, EvaluationContext ctx,
  227 | 			MethodSecurityExpressionOperations rootObject) {
  228 | 		return filterTarget.filter((filterObject) -> {
  229 | 			rootObject.setFilterObject(filterObject);
  230 | 			return ExpressionUtils.evaluateAsBoolean(filterExpression, ctx);
  231 | 		}).onClose(filterTarget::close);
  232 | 	}
  233 | 
  234 | 	/**
  235 | 	 * Sets the {@link AuthenticationTrustResolver} to be used. The default is
  236 | 	 * {@link AuthenticationTrustResolverImpl}.
  237 | 	 * @param trustResolver the {@link AuthenticationTrustResolver} to use. Cannot be
  238 | 	 * null.
  239 | 	 * @deprecated Use
  240 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  241 | 	 */
  242 | 	@Deprecated(since = "7.0")
  243 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
  244 | 		Assert.notNull(trustResolver, "trustResolver cannot be null");
  245 | 		getDefaultAuthorizationManagerFactory().setTrustResolver(trustResolver);
  246 | 		this.trustResolver = trustResolver;
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Returns the current {@link AuthenticationTrustResolver}.
  251 | 	 * @return The current {@link AuthenticationTrustResolver}
  252 | 	 * @deprecated Use
  253 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  254 | 	 */
  255 | 	@Deprecated(since = "7.0")
  256 | 	protected AuthenticationTrustResolver getTrustResolver() {
  257 | 		return this.trustResolver;
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * Sets the {@link ParameterNameDiscoverer} to use. The default is
  262 | 	 * {@link DefaultSecurityParameterNameDiscoverer}.
  263 | 	 * @param parameterNameDiscoverer
  264 | 	 */
  265 | 	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
  266 | 		this.parameterNameDiscoverer = parameterNameDiscoverer;
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Returns the current {@link ParameterNameDiscoverer}.
  271 | 	 * @return The current {@link ParameterNameDiscoverer}
  272 | 	 */
  273 | 	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
  274 | 		return this.parameterNameDiscoverer;
  275 | 	}
  276 | 
  277 | 	public void setPermissionCacheOptimizer(PermissionCacheOptimizer permissionCacheOptimizer) {
  278 | 		this.permissionCacheOptimizer = permissionCacheOptimizer;
  279 | 	}
  280 | 
  281 | 	@Override
  282 | 	public void setReturnObject(@Nullable Object returnObject, EvaluationContext ctx) {
  283 | 		TypedValue rootObject = ctx.getRootObject();
  284 | 		Assert.notNull(rootObject, "rootObject cannot be null");
  285 | 		MethodSecurityExpressionOperations methodOperations = (MethodSecurityExpressionOperations) rootObject
  286 | 			.getValue();
  287 | 		Assert.notNull(methodOperations, "MethodSecurityExpressionOperations cannot be null");
  288 | 		methodOperations.setReturnObject(returnObject);
  289 | 	}
  290 | 
  291 | 	/**
  292 | 	 * <p>
  293 | 	 * Sets the default prefix to be added to
  294 | 	 * {@link org.springframework.security.access.expression.SecurityExpressionRoot#hasAnyRole(String...)}
  295 | 	 * or
  296 | 	 * {@link org.springframework.security.access.expression.SecurityExpressionRoot#hasRole(String)}.
  297 | 	 * For example, if hasRole("ADMIN") or hasRole("ROLE_ADMIN") is passed in, then the
  298 | 	 * role ROLE_ADMIN will be used when the defaultRolePrefix is "ROLE_" (default).
  299 | 	 * </p>
  300 | 	 *
  301 | 	 * <p>
  302 | 	 * If null or empty, then no default role prefix is used.
  303 | 	 * </p>
  304 | 	 * @param defaultRolePrefix the default prefix to add to roles. Default "ROLE_".
  305 | 	 * @deprecated Use
  306 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  307 | 	 */
  308 | 	@Deprecated(since = "7.0")
  309 | 	public void setDefaultRolePrefix(@Nullable String defaultRolePrefix) {
  310 | 		if (defaultRolePrefix == null) {
  311 | 			defaultRolePrefix = "";
  312 | 		}
  313 | 		getDefaultAuthorizationManagerFactory().setRolePrefix(defaultRolePrefix);
  314 | 		this.defaultRolePrefix = defaultRolePrefix;
  315 | 	}
  316 | 
  317 | 	/**
  318 | 	 * Returns the default role prefix.
  319 | 	 * @return The default role prefix
  320 | 	 * @deprecated Use
  321 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  322 | 	 */
  323 | 	@Deprecated(since = "7.0")
  324 | 	protected String getDefaultRolePrefix() {
  325 | 		return this.defaultRolePrefix;
  326 | 	}
  327 | 
  328 | }
```
