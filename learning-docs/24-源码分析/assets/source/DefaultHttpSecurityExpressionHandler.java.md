<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/expression/DefaultHttpSecurityExpressionHandler.java -->
<!-- 正文引用：[[源码:DefaultHttpSecurityExpressionHandler.java#L起-L止]] ；代码块头部：(src: DefaultHttpSecurityExpressionHandler.java#L起-L止) -->

# DefaultHttpSecurityExpressionHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/expression/DefaultHttpSecurityExpressionHandler.java` |
| 行数 | 109 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:41.111Z |

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
   17 | package org.springframework.security.web.access.expression;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.expression.EvaluationContext;
   24 | import org.springframework.expression.spel.support.StandardEvaluationContext;
   25 | import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
   26 | import org.springframework.security.access.expression.SecurityExpressionHandler;
   27 | import org.springframework.security.access.expression.SecurityExpressionOperations;
   28 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   29 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   30 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   31 | import org.springframework.security.core.Authentication;
   32 | import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
   33 | import org.springframework.util.Assert;
   34 | 
   35 | /**
   36 |  * A {@link SecurityExpressionHandler} that uses a {@link RequestAuthorizationContext} to
   37 |  * create a {@link WebSecurityExpressionRoot}.
   38 |  *
   39 |  * @author Evgeniy Cheban
   40 |  * @author Steve Riesenberg
   41 |  * @since 5.8
   42 |  */
   43 | public class DefaultHttpSecurityExpressionHandler extends AbstractSecurityExpressionHandler<RequestAuthorizationContext>
   44 | 		implements SecurityExpressionHandler<RequestAuthorizationContext> {
   45 | 
   46 | 	private static final String DEFAULT_ROLE_PREFIX = "ROLE_";
   47 | 
   48 | 	private String defaultRolePrefix = DEFAULT_ROLE_PREFIX;
   49 | 
   50 | 	@Override
   51 | 	public EvaluationContext createEvaluationContext(Supplier<? extends @Nullable Authentication> authentication,
   52 | 			RequestAuthorizationContext context) {
   53 | 		WebSecurityExpressionRoot root = createSecurityExpressionRoot(authentication, context);
   54 | 		StandardEvaluationContext ctx = new StandardEvaluationContext(root);
   55 | 		ctx.setBeanResolver(getBeanResolver());
   56 | 		context.getVariables().forEach(ctx::setVariable);
   57 | 		return ctx;
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	protected SecurityExpressionOperations createSecurityExpressionRoot(@Nullable Authentication authentication,
   62 | 			RequestAuthorizationContext context) {
   63 | 		return createSecurityExpressionRoot(() -> authentication, context);
   64 | 	}
   65 | 
   66 | 	private WebSecurityExpressionRoot createSecurityExpressionRoot(
   67 | 			Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
   68 | 		WebSecurityExpressionRoot root = new WebSecurityExpressionRoot(authentication, context);
   69 | 		root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
   70 | 		root.setPermissionEvaluator(getPermissionEvaluator());
   71 | 		if (!DEFAULT_ROLE_PREFIX.equals(this.defaultRolePrefix)) {
   72 | 			// Ensure SecurityExpressionRoot can strip the custom role prefix
   73 | 			root.setDefaultRolePrefix(this.defaultRolePrefix);
   74 | 		}
   75 | 		return root;
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Sets the {@link AuthenticationTrustResolver} to be used. The default is
   80 | 	 * {@link AuthenticationTrustResolverImpl}.
   81 | 	 * @param trustResolver the {@link AuthenticationTrustResolver} to use
   82 | 	 * @deprecated Use
   83 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
   84 | 	 */
   85 | 	@Deprecated(since = "7.0")
   86 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
   87 | 		getDefaultAuthorizationManagerFactory().setTrustResolver(trustResolver);
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Sets the default prefix to be added to
   92 | 	 * {@link org.springframework.security.access.expression.SecurityExpressionRoot#hasAnyRole(String...)}
   93 | 	 * or
   94 | 	 * {@link org.springframework.security.access.expression.SecurityExpressionRoot#hasRole(String)}.
   95 | 	 * For example, if hasRole("ADMIN") or hasRole("ROLE_ADMIN") is passed in, then the
   96 | 	 * role ROLE_ADMIN will be used when the defaultRolePrefix is "ROLE_" (default).
   97 | 	 * @param defaultRolePrefix the default prefix to add to roles. The default is
   98 | 	 * "ROLE_".
   99 | 	 * @deprecated Use
  100 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  101 | 	 */
  102 | 	@Deprecated(since = "7.0")
  103 | 	public void setDefaultRolePrefix(String defaultRolePrefix) {
  104 | 		Assert.notNull(defaultRolePrefix, "defaultRolePrefix cannot be null");
  105 | 		getDefaultAuthorizationManagerFactory().setRolePrefix(defaultRolePrefix);
  106 | 		this.defaultRolePrefix = defaultRolePrefix;
  107 | 	}
  108 | 
  109 | }
```
