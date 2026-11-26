<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/AuthorizationManagerAfterMethodInterceptor.java -->
<!-- 正文引用：[[源码:AuthorizationManagerAfterMethodInterceptor.java#L起-L止]] ；代码块头部：(src: AuthorizationManagerAfterMethodInterceptor.java#L起-L止) -->

# AuthorizationManagerAfterMethodInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/AuthorizationManagerAfterMethodInterceptor.java` |
| 行数 | 215 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:47:23.446Z |

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
   21 | import org.aopalliance.aop.Advice;
   22 | import org.aopalliance.intercept.MethodInterceptor;
   23 | import org.aopalliance.intercept.MethodInvocation;
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.aop.Pointcut;
   29 | import org.springframework.core.log.LogMessage;
   30 | import org.springframework.security.access.AccessDeniedException;
   31 | import org.springframework.security.access.prepost.PostAuthorize;
   32 | import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
   33 | import org.springframework.security.authorization.AuthorizationDeniedException;
   34 | import org.springframework.security.authorization.AuthorizationEventPublisher;
   35 | import org.springframework.security.authorization.AuthorizationManager;
   36 | import org.springframework.security.authorization.AuthorizationResult;
   37 | import org.springframework.security.core.Authentication;
   38 | import org.springframework.security.core.context.SecurityContextHolder;
   39 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   40 | import org.springframework.util.Assert;
   41 | 
   42 | /**
   43 |  * A {@link MethodInterceptor} which can determine if an {@link Authentication} has access
   44 |  * to the result of an {@link MethodInvocation} using an {@link AuthorizationManager}.
   45 |  *
   46 |  * @author Evgeniy Cheban
   47 |  * @author Josh Cummings
   48 |  * @since 5.6
   49 |  */
   50 | public final class AuthorizationManagerAfterMethodInterceptor implements AuthorizationAdvisor {
   51 | 
   52 | 	private Supplier<SecurityContextHolderStrategy> securityContextHolderStrategy = SecurityContextHolder::getContextHolderStrategy;
   53 | 
   54 | 	private final Log logger = LogFactory.getLog(this.getClass());
   55 | 
   56 | 	private final Pointcut pointcut;
   57 | 
   58 | 	private final AuthorizationManager<MethodInvocationResult> authorizationManager;
   59 | 
   60 | 	private final MethodAuthorizationDeniedHandler defaultHandler = new ThrowingMethodAuthorizationDeniedHandler();
   61 | 
   62 | 	private int order;
   63 | 
   64 | 	private AuthorizationEventPublisher eventPublisher = new NoOpAuthorizationEventPublisher();
   65 | 
   66 | 	/**
   67 | 	 * Creates an instance.
   68 | 	 * @param pointcut the {@link Pointcut} to use
   69 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
   70 | 	 */
   71 | 	public AuthorizationManagerAfterMethodInterceptor(Pointcut pointcut,
   72 | 			AuthorizationManager<MethodInvocationResult> authorizationManager) {
   73 | 		Assert.notNull(pointcut, "pointcut cannot be null");
   74 | 		Assert.notNull(authorizationManager, "authorizationManager cannot be null");
   75 | 		this.pointcut = pointcut;
   76 | 		this.authorizationManager = authorizationManager;
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Creates an interceptor for the {@link PostAuthorize} annotation.
   81 | 	 * @return the interceptor
   82 | 	 */
   83 | 	public static AuthorizationManagerAfterMethodInterceptor postAuthorize() {
   84 | 		return postAuthorize(new PostAuthorizeAuthorizationManager());
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Creates an interceptor for the {@link PostAuthorize} annotation.
   89 | 	 * @param authorizationManager the {@link PostAuthorizeAuthorizationManager} to use
   90 | 	 * @return the interceptor
   91 | 	 */
   92 | 	public static AuthorizationManagerAfterMethodInterceptor postAuthorize(
   93 | 			PostAuthorizeAuthorizationManager authorizationManager) {
   94 | 		AuthorizationManagerAfterMethodInterceptor interceptor = new AuthorizationManagerAfterMethodInterceptor(
   95 | 				AuthorizationMethodPointcuts.forAnnotations(PostAuthorize.class), authorizationManager);
   96 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.POST_AUTHORIZE.getOrder());
   97 | 		return interceptor;
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Creates an interceptor for the {@link PostAuthorize} annotation.
  102 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
  103 | 	 * @return the interceptor
  104 | 	 * @since 6.0
  105 | 	 */
  106 | 	public static AuthorizationManagerAfterMethodInterceptor postAuthorize(
  107 | 			AuthorizationManager<MethodInvocationResult> authorizationManager) {
  108 | 		AuthorizationManagerAfterMethodInterceptor interceptor = new AuthorizationManagerAfterMethodInterceptor(
  109 | 				AuthorizationMethodPointcuts.forAnnotations(PostAuthorize.class), authorizationManager);
  110 | 		interceptor.setOrder(AuthorizationInterceptorsOrder.POST_AUTHORIZE.getOrder());
  111 | 		return interceptor;
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Determine if an {@link Authentication} has access to the {@link MethodInvocation}
  116 | 	 * using the {@link AuthorizationManager}.
  117 | 	 * @param mi the {@link MethodInvocation} to check
  118 | 	 * @throws AccessDeniedException if access is not granted
  119 | 	 */
  120 | 	@Override
  121 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
  122 | 		Object result;
  123 | 		try {
  124 | 			result = mi.proceed();
  125 | 		}
  126 | 		catch (AuthorizationDeniedException ex) {
  127 | 			if (this.authorizationManager instanceof MethodAuthorizationDeniedHandler handler) {
  128 | 				return handler.handleDeniedInvocation(mi, ex);
  129 | 			}
  130 | 			return this.defaultHandler.handleDeniedInvocation(mi, ex);
  131 | 		}
  132 | 		return attemptAuthorization(mi, result);
  133 | 	}
  134 | 
  135 | 	@Override
  136 | 	public int getOrder() {
  137 | 		return this.order;
  138 | 	}
  139 | 
  140 | 	public void setOrder(int order) {
  141 | 		this.order = order;
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Use this {@link AuthorizationEventPublisher} to publish the
  146 | 	 * {@link AuthorizationManager} result.
  147 | 	 * @param eventPublisher
  148 | 	 * @since 5.7
  149 | 	 */
  150 | 	public void setAuthorizationEventPublisher(AuthorizationEventPublisher eventPublisher) {
  151 | 		Assert.notNull(eventPublisher, "eventPublisher cannot be null");
  152 | 		this.eventPublisher = eventPublisher;
  153 | 	}
  154 | 
  155 | 	/**
  156 | 	 * {@inheritDoc}
  157 | 	 */
  158 | 	@Override
  159 | 	public Pointcut getPointcut() {
  160 | 		return this.pointcut;
  161 | 	}
  162 | 
  163 | 	@Override
  164 | 	public Advice getAdvice() {
  165 | 		return this;
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public boolean isPerInstance() {
  170 | 		return true;
  171 | 	}
  172 | 
  173 | 	/**
  174 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  175 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  176 | 	 *
  177 | 	 * @since 5.8
  178 | 	 */
  179 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy strategy) {
  180 | 		this.securityContextHolderStrategy = () -> strategy;
  181 | 	}
  182 | 
  183 | 	private @Nullable Object attemptAuthorization(MethodInvocation mi, @Nullable Object result) {
  184 | 		this.logger.debug(LogMessage.of(() -> "Authorizing method invocation " + mi));
  185 | 		MethodInvocationResult object = new MethodInvocationResult(mi, result);
  186 | 		AuthorizationResult authorizationResult = this.authorizationManager.authorize(this::getAuthentication, object);
  187 | 		if (authorizationResult != null) {
  188 | 			this.eventPublisher.publishAuthorizationEvent(this::getAuthentication, object, authorizationResult);
  189 | 		}
  190 | 		if (authorizationResult != null && !authorizationResult.isGranted()) {
  191 | 			this.logger.debug(LogMessage.of(() -> "Failed to authorize " + mi + " with authorization manager "
  192 | 					+ this.authorizationManager + " and authorizationResult " + authorizationResult));
  193 | 			return handlePostInvocationDenied(object, authorizationResult);
  194 | 		}
  195 | 		this.logger.debug(LogMessage.of(() -> "Authorized method invocation " + mi));
  196 | 		return result;
  197 | 	}
  198 | 
  199 | 	private @Nullable Object handlePostInvocationDenied(MethodInvocationResult mi, AuthorizationResult result) {
  200 | 		if (this.authorizationManager instanceof MethodAuthorizationDeniedHandler deniedHandler) {
  201 | 			return deniedHandler.handleDeniedInvocationResult(mi, result);
  202 | 		}
  203 | 		return this.defaultHandler.handleDeniedInvocationResult(mi, result);
  204 | 	}
  205 | 
  206 | 	private Authentication getAuthentication() {
  207 | 		Authentication authentication = this.securityContextHolderStrategy.get().getContext().getAuthentication();
  208 | 		if (authentication == null) {
  209 | 			throw new AuthenticationCredentialsNotFoundException(
  210 | 					"An Authentication object was not found in the SecurityContext");
  211 | 		}
  212 | 		return authentication;
  213 | 	}
  214 | 
  215 | }
```
