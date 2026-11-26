<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/expression/AbstractSecurityExpressionHandler.java -->
<!-- 正文引用：[[源码:AbstractSecurityExpressionHandler.java#L起-L止]] ；代码块头部：(src: AbstractSecurityExpressionHandler.java#L起-L止) -->

# AbstractSecurityExpressionHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/expression/AbstractSecurityExpressionHandler.java` |
| 行数 | 186 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:47:23.153Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.context.ApplicationContext;
   22 | import org.springframework.context.ApplicationContextAware;
   23 | import org.springframework.context.expression.BeanFactoryResolver;
   24 | import org.springframework.expression.BeanResolver;
   25 | import org.springframework.expression.EvaluationContext;
   26 | import org.springframework.expression.ExpressionParser;
   27 | import org.springframework.expression.spel.standard.SpelExpressionParser;
   28 | import org.springframework.expression.spel.support.StandardEvaluationContext;
   29 | import org.springframework.security.access.PermissionEvaluator;
   30 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   31 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   32 | import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
   33 | import org.springframework.security.core.Authentication;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | /**
   37 |  * Base implementation of the facade which isolates Spring Security's requirements for
   38 |  * evaluating security expressions from the implementation of the underlying expression
   39 |  * objects.
   40 |  *
   41 |  * @author Luke Taylor
   42 |  * @author Evgeniy Cheban
   43 |  * @author Steve Riesenberg
   44 |  * @since 3.1
   45 |  */
   46 | public abstract class AbstractSecurityExpressionHandler<T>
   47 | 		implements SecurityExpressionHandler<T>, ApplicationContextAware {
   48 | 
   49 | 	private ExpressionParser expressionParser = new SpelExpressionParser();
   50 | 
   51 | 	private @Nullable BeanResolver beanResolver;
   52 | 
   53 | 	private @Nullable RoleHierarchy roleHierarchy;
   54 | 
   55 | 	private AuthorizationManagerFactory<T> authorizationManagerFactory = new DefaultAuthorizationManagerFactory<>();
   56 | 
   57 | 	private PermissionEvaluator permissionEvaluator = new DenyAllPermissionEvaluator();
   58 | 
   59 | 	@Override
   60 | 	public final ExpressionParser getExpressionParser() {
   61 | 		return this.expressionParser;
   62 | 	}
   63 | 
   64 | 	public final void setExpressionParser(ExpressionParser expressionParser) {
   65 | 		Assert.notNull(expressionParser, "expressionParser cannot be null");
   66 | 		this.expressionParser = expressionParser;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Invokes the internal template methods to create {@code StandardEvaluationContext}
   71 | 	 * and {@code SecurityExpressionRoot} objects.
   72 | 	 * @param authentication the current authentication object
   73 | 	 * @param invocation the invocation (filter, method, channel)
   74 | 	 * @return the context object for use in evaluating the expression, populated with a
   75 | 	 * suitable root object.
   76 | 	 */
   77 | 	@Override
   78 | 	public final EvaluationContext createEvaluationContext(@Nullable Authentication authentication, T invocation) {
   79 | 		SecurityExpressionOperations root = createSecurityExpressionRoot(authentication, invocation);
   80 | 		StandardEvaluationContext ctx = createEvaluationContextInternal(authentication, invocation);
   81 | 		ctx.setBeanResolver(this.beanResolver);
   82 | 		ctx.setRootObject(root);
   83 | 		return ctx;
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Override to create a custom instance of {@code StandardEvaluationContext}.
   88 | 	 * <p>
   89 | 	 * The returned object will have a {@code SecurityExpressionRootPropertyAccessor}
   90 | 	 * added, allowing beans in the {@code ApplicationContext} to be accessed via
   91 | 	 * expression properties.
   92 | 	 * @param authentication the current authentication object
   93 | 	 * @param invocation the invocation (filter, method, channel)
   94 | 	 * @return A {@code StandardEvaluationContext} or potentially a custom subclass if
   95 | 	 * overridden.
   96 | 	 */
   97 | 	protected StandardEvaluationContext createEvaluationContextInternal(@Nullable Authentication authentication,
   98 | 			T invocation) {
   99 | 		return new StandardEvaluationContext();
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Implement in order to create a root object of the correct type for the supported
  104 | 	 * invocation type.
  105 | 	 * @param authentication the current authentication object
  106 | 	 * @param invocation the invocation (filter, method, channel)
  107 | 	 * @return the object
  108 | 	 */
  109 | 	protected abstract SecurityExpressionOperations createSecurityExpressionRoot(
  110 | 			@Nullable Authentication authentication, T invocation);
  111 | 
  112 | 	/**
  113 | 	 * Sets the {@link AuthorizationManagerFactory} to be used. The default is
  114 | 	 * {@link DefaultAuthorizationManagerFactory}.
  115 | 	 * @param authorizationManagerFactory the {@link AuthorizationManagerFactory} to use.
  116 | 	 * Cannot be null.
  117 | 	 * @since 7.0
  118 | 	 */
  119 | 	public final void setAuthorizationManagerFactory(AuthorizationManagerFactory<T> authorizationManagerFactory) {
  120 | 		Assert.notNull(authorizationManagerFactory, "authorizationManagerFactory cannot be null");
  121 | 		this.authorizationManagerFactory = authorizationManagerFactory;
  122 | 	}
  123 | 
  124 | 	protected final AuthorizationManagerFactory<T> getAuthorizationManagerFactory() {
  125 | 		return this.authorizationManagerFactory;
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Allows accessing the {@link DefaultAuthorizationManagerFactory} for getting and
  130 | 	 * setting defaults. This method will be removed in Spring Security 8.
  131 | 	 * @return the {@link DefaultAuthorizationManagerFactory}
  132 | 	 * @throws IllegalStateException if a different {@link AuthorizationManagerFactory}
  133 | 	 * was already set
  134 | 	 * @deprecated Use
  135 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  136 | 	 */
  137 | 	@Deprecated(since = "7.0")
  138 | 	protected final DefaultAuthorizationManagerFactory<T> getDefaultAuthorizationManagerFactory() {
  139 | 		if (!(this.authorizationManagerFactory instanceof DefaultAuthorizationManagerFactory<T> defaultAuthorizationManagerFactory)) {
  140 | 			throw new IllegalStateException(
  141 | 					"authorizationManagerFactory must be an instance of DefaultAuthorizationManagerFactory");
  142 | 		}
  143 | 
  144 | 		return defaultAuthorizationManagerFactory;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Returns the {@link RoleHierarchy} to use.
  149 | 	 * @deprecated Use {@link #getDefaultAuthorizationManagerFactory()} instead
  150 | 	 */
  151 | 	@Deprecated(since = "7.0")
  152 | 	protected @Nullable RoleHierarchy getRoleHierarchy() {
  153 | 		return this.roleHierarchy;
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Sets the {@link RoleHierarchy} to use.
  158 | 	 * @deprecated Use
  159 | 	 * {@link #setAuthorizationManagerFactory(AuthorizationManagerFactory)} instead
  160 | 	 */
  161 | 	@Deprecated(since = "7.0")
  162 | 	public void setRoleHierarchy(@Nullable RoleHierarchy roleHierarchy) {
  163 | 		if (roleHierarchy != null) {
  164 | 			getDefaultAuthorizationManagerFactory().setRoleHierarchy(roleHierarchy);
  165 | 		}
  166 | 		this.roleHierarchy = roleHierarchy;
  167 | 	}
  168 | 
  169 | 	protected PermissionEvaluator getPermissionEvaluator() {
  170 | 		return this.permissionEvaluator;
  171 | 	}
  172 | 
  173 | 	public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
  174 | 		this.permissionEvaluator = permissionEvaluator;
  175 | 	}
  176 | 
  177 | 	protected @Nullable BeanResolver getBeanResolver() {
  178 | 		return this.beanResolver;
  179 | 	}
  180 | 
  181 | 	@Override
  182 | 	public void setApplicationContext(ApplicationContext applicationContext) {
  183 | 		this.beanResolver = new BeanFactoryResolver(applicationContext);
  184 | 	}
  185 | 
  186 | }
```
