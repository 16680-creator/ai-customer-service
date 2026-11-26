<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/AuthorizationManagerBeforeMethodInterceptor.java -->
<!-- 正文引用：[[源码:AuthorizationManagerBeforeMethodInterceptor.java#L起-L止]] ；代码块头部：(src: AuthorizationManagerBeforeMethodInterceptor.java#L起-L止) -->

# AuthorizationManagerBeforeMethodInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/AuthorizationManagerBeforeMethodInterceptor.java` |
| 行数 | 304 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.236Z |

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
   19 | import java.util.function.Supplier;
   20 | 
   21 | import jakarta.annotation.security.DenyAll;
   22 | import jakarta.annotation.security.PermitAll;
   23 | import jakarta.annotation.security.RolesAllowed;
   24 | import org.aopalliance.aop.Advice;
   25 | import org.aopalliance.intercept.MethodInterceptor;
   26 | import org.aopalliance.intercept.MethodInvocation;
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.aop.Pointcut;
   32 | import org.springframework.core.log.LogMessage;
   33 | import org.springframework.security.access.AccessDeniedException;
   34 | import org.springframework.security.access.annotation.Secured;
   35 | import org.springframework.security.access.prepost.PreAuthorize;
   36 | import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
   37 | import org.springframework.security.authorization.AuthorizationDeniedException;
   38 | import org.springframework.security.authorization.AuthorizationEventPublisher;
   39 | import org.springframework.security.authorization.AuthorizationManager;
   40 | import org.springframework.security.authorization.AuthorizationResult;
   41 | import org.springframework.security.core.Authentication;
   42 | import org.springframework.security.core.context.SecurityContextHolder;
   43 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   44 | import org.springframework.util.Assert;
   45 | 
   46 | /**
   47 |  * A {@link MethodInterceptor} which uses a {@link AuthorizationManager} to determine if
   48 |  * an {@link Authentication} may invoke the given {@link MethodInvocation}.
   49 |  *
   50 |  * @author Evgeniy Cheban
   51 |  * @author Josh Cummings
   52 |  * @since 5.6
   53 |  */
   54 | public final class AuthorizationManagerBeforeMethodInterceptor implements AuthorizationAdvisor {
   55 | 
   56 | 	private Supplier<SecurityContextHolderStrategy> securityContextHolderStrategy = SecurityContextHolder::getContextHolderStrategy;
   57 | 
   58 | 	private final Log logger = LogFactory.getLog(this.getClass());
   59 | 
   60 | 	private final Pointcut pointcut;
   61 | 
   62 | 	private final AuthorizationManager<MethodInvocation> authorizationManager;
   63 | 
   64 | 	private final MethodAuthorizationDeniedHandler defaultHandler = new ThrowingMethodAuthorizationDeniedHandler();
   65 | 
   66 | 	private int order = AuthorizationInterceptorsOrder.FIRST.getOrder();
   67 | 
   68 | 	private AuthorizationEventPublisher eventPublisher = new NoOpAuthorizationEventPublisher();
   69 | 
   70 | 	/**
   71 | 	 * Creates an instance.
   72 | 	 * @param pointcut the {@link Pointcut} to use
   73 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
   74 | 	 */
   75 | 	public AuthorizationManagerBeforeMethodInterceptor(Pointcut pointcut,
   76 | 			AuthorizationManager<MethodInvocation> authorizationManager) {
   77 | 		Assert.notNull(pointcut, "pointcut cannot be null");
   78 | 		Assert.notNull(authorizationManager, "authorizationManager cannot be null");
   79 | 		this.pointcut = pointcut;
   80 | 		this.authorizationManager = authorizationManager;
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Creates an interceptor for the {@link PreAuthorize} annotation.
   85 | 	 * @return the interceptor
   86 | 	 */
   87 | 	public static AuthorizationManagerBeforeMethodInterceptor preAuthorize() {
   88 | 		return preAuthorize(new PreAuthorizeAuthorizationManager());
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Creates an interceptor for the {@link PreAuthorize} annotation.
   93 | 	 * @param authorizationManager the {@link PreAuthorizeAuthorizationManager} to use
   94 | 	 * @return the interceptor
   95 | 	 */
   96 | 	public static AuthorizationManagerBeforeMethodInterceptor preAuthorize(
   97 | 			PreAuthorizeAuthorizationManager authorizationManager) {
   98 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
   99 | 				AuthorizationMethodPointcuts.forAnnotations(PreAuthorize.class), authorizationManager);
  100 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder());
  101 | 		return interceptor;
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Creates an interceptor for the {@link PreAuthorize} annotation.
  106 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
  107 | 	 * @return the interceptor
  108 | 	 * @since 6.0
  109 | 	 */
  110 | 	public static AuthorizationManagerBeforeMethodInterceptor preAuthorize(
  111 | 			AuthorizationManager<MethodInvocation> authorizationManager) {
  112 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
  113 | 				AuthorizationMethodPointcuts.forAnnotations(PreAuthorize.class), authorizationManager);
  114 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder());
  115 | 		return interceptor;
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Creates an interceptor for the {@link Secured} annotation.
  120 | 	 * @return the interceptor
  121 | 	 */
  122 | 	public static AuthorizationManagerBeforeMethodInterceptor secured() {
  123 | 		return secured(new SecuredAuthorizationManager());
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * Creates an interceptor for the {@link Secured} annotation.
  128 | 	 * @param authorizationManager the {@link SecuredAuthorizationManager} to use
  129 | 	 * @return the interceptor
  130 | 	 */
  131 | 	public static AuthorizationManagerBeforeMethodInterceptor secured(
  132 | 			SecuredAuthorizationManager authorizationManager) {
  133 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
  134 | 				AuthorizationMethodPointcuts.forAnnotations(Secured.class), authorizationManager);
  135 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.SECURED.getOrder());
  136 | 		return interceptor;
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Creates an interceptor for the {@link Secured} annotation.
  141 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
  142 | 	 * @return the interceptor
  143 | 	 * @since 6.0
  144 | 	 */
  145 | 	public static AuthorizationManagerBeforeMethodInterceptor secured(
  146 | 			AuthorizationManager<MethodInvocation> authorizationManager) {
  147 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
  148 | 				AuthorizationMethodPointcuts.forAnnotations(Secured.class), authorizationManager);
  149 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.SECURED.getOrder());
  150 | 		return interceptor;
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Creates an interceptor for the JSR-250 annotations.
  155 | 	 * @return the interceptor
  156 | 	 */
  157 | 	public static AuthorizationManagerBeforeMethodInterceptor jsr250() {
  158 | 		return jsr250(new Jsr250AuthorizationManager());
  159 | 	}
  160 | 
  161 | 	/**
  162 | 	 * Creates an interceptor for the JSR-250 annotations.
  163 | 	 * @param authorizationManager the {@link Jsr250AuthorizationManager} to use
  164 | 	 * @return the interceptor
  165 | 	 */
  166 | 	public static AuthorizationManagerBeforeMethodInterceptor jsr250(Jsr250AuthorizationManager authorizationManager) {
  167 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
  168 | 				AuthorizationMethodPointcuts.forAnnotations(RolesAllowed.class, DenyAll.class, PermitAll.class),
  169 | 				authorizationManager);
  170 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.JSR250.getOrder());
  171 | 		return interceptor;
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Creates an interceptor for the JSR-250 annotations.
  176 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
  177 | 	 * @return the interceptor
  178 | 	 * @since 6.0
  179 | 	 */
  180 | 	public static AuthorizationManagerBeforeMethodInterceptor jsr250(
  181 | 			AuthorizationManager<MethodInvocation> authorizationManager) {
  182 | 		AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
  183 | 				AuthorizationMethodPointcuts.forAnnotations(RolesAllowed.class, DenyAll.class, PermitAll.class),
  184 | 				authorizationManager);
  185 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.JSR250.getOrder());
  186 | 		return interceptor;
  187 | 	}
  188 | 
  189 | 	/**
  190 | 	 * Determine if an {@link Authentication} has access to the {@link MethodInvocation}
  191 | 	 * using the configured {@link AuthorizationManager}.
  192 | 	 * @param mi the {@link MethodInvocation} to check
  193 | 	 * @throws AccessDeniedException if access is not granted
  194 | 	 */
  195 | 	@Override
  196 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
  197 | 		return attemptAuthorization(mi);
  198 | 	}
  199 | 
  200 | 	@Override
  201 | 	public int getOrder() {
  202 | 		return this.order;
  203 | 	}
  204 | 
  205 | 	public void setOrder(int order) {
  206 | 		this.order = order;
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Use this {@link AuthorizationEventPublisher} to publish the
  211 | 	 * {@link AuthorizationManager} result.
  212 | 	 * @param eventPublisher
  213 | 	 * @since 5.7
  214 | 	 */
  215 | 	public void setAuthorizationEventPublisher(AuthorizationEventPublisher eventPublisher) {
  216 | 		Assert.notNull(eventPublisher, "eventPublisher cannot be null");
  217 | 		this.eventPublisher = eventPublisher;
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * {@inheritDoc}
  222 | 	 */
  223 | 	@Override
  224 | 	public Pointcut getPointcut() {
  225 | 		return this.pointcut;
  226 | 	}
  227 | 
  228 | 	@Override
  229 | 	public Advice getAdvice() {
  230 | 		return this;
  231 | 	}
  232 | 
  233 | 	@Override
  234 | 	public boolean isPerInstance() {
  235 | 		return true;
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  240 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  241 | 	 *
  242 | 	 * @since 5.8
  243 | 	 */
  244 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  245 | 		this.securityContextHolderStrategy = () -> securityContextHolderStrategy;
  246 | 	}
  247 | 
  248 | 	private @Nullable Object attemptAuthorization(MethodInvocation mi) throws Throwable {
  249 | 		this.logger.debug(LogMessage.of(() -> "Authorizing method invocation " + mi));
  250 | 		AuthorizationResult result;
  251 | 		try {
  252 | 			result = this.authorizationManager.authorize(this::getAuthentication, mi);
  253 | 		}
  254 | 		catch (AuthorizationDeniedException denied) {
  255 | 			return handle(mi, denied);
  256 | 		}
  257 | 		if (result != null) {
  258 | 			this.eventPublisher.publishAuthorizationEvent(this::getAuthentication, mi, result);
  259 | 		}
  260 | 		if (result != null && !result.isGranted()) {
  261 | 			this.logger.debug(LogMessage.of(() -> "Failed to authorize " + mi + " with authorization manager "
  262 | 					+ this.authorizationManager + " and result " + result));
  263 | 			return handle(mi, result);
  264 | 		}
  265 | 		this.logger.debug(LogMessage.of(() -> "Authorized method invocation " + mi));
  266 | 		return proceed(mi);
  267 | 	}
  268 | 
  269 | 	private @Nullable Object proceed(MethodInvocation mi) throws Throwable {
  270 | 		try {
  271 | 			return mi.proceed();
  272 | 		}
  273 | 		catch (AuthorizationDeniedException ex) {
  274 | 			if (this.authorizationManager instanceof MethodAuthorizationDeniedHandler handler) {
  275 | 				return handler.handleDeniedInvocation(mi, ex);
  276 | 			}
  277 | 			return this.defaultHandler.handleDeniedInvocation(mi, ex);
  278 | 		}
  279 | 	}
  280 | 
  281 | 	private @Nullable Object handle(MethodInvocation mi, AuthorizationDeniedException denied) {
  282 | 		if (this.authorizationManager instanceof MethodAuthorizationDeniedHandler handler) {
  283 | 			return handler.handleDeniedInvocation(mi, denied);
  284 | 		}
  285 | 		return this.defaultHandler.handleDeniedInvocation(mi, denied);
  286 | 	}
  287 | 
  288 | 	private @Nullable Object handle(MethodInvocation mi, AuthorizationResult result) {
  289 | 		if (this.authorizationManager instanceof MethodAuthorizationDeniedHandler handler) {
  290 | 			return handler.handleDeniedInvocation(mi, result);
  291 | 		}
  292 | 		return this.defaultHandler.handleDeniedInvocation(mi, result);
  293 | 	}
  294 | 
  295 | 	private Authentication getAuthentication() {
  296 | 		Authentication authentication = this.securityContextHolderStrategy.get().getContext().getAuthentication();
  297 | 		if (authentication == null) {
  298 | 			throw new AuthenticationCredentialsNotFoundException(
  299 | 					"An Authentication object was not found in the SecurityContext");
  300 | 		}
  301 | 		return authentication;
  302 | 	}
  303 | 
  304 | }
```
