<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/PostAuthorizeAuthorizationManager.java -->
<!-- 正文引用：[[源码:PostAuthorizeAuthorizationManager.java#L起-L止]] ；代码块头部：(src: PostAuthorizeAuthorizationManager.java#L起-L止) -->

# PostAuthorizeAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/PostAuthorizeAuthorizationManager.java` |
| 行数 | 118 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.628Z |

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
   21 | import org.aopalliance.intercept.MethodInvocation;
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.context.ApplicationContext;
   25 | import org.springframework.expression.EvaluationContext;
   26 | import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
   27 | import org.springframework.security.access.prepost.PostAuthorize;
   28 | import org.springframework.security.authorization.AuthorizationDecision;
   29 | import org.springframework.security.authorization.AuthorizationManager;
   30 | import org.springframework.security.authorization.AuthorizationResult;
   31 | import org.springframework.security.core.Authentication;
   32 | import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
   33 | 
   34 | /**
   35 |  * An {@link AuthorizationManager} which can determine if an {@link Authentication} may
   36 |  * return the result from an invoked {@link MethodInvocation} by evaluating an expression
   37 |  * from the {@link PostAuthorize} annotation.
   38 |  *
   39 |  * @author Evgeniy Cheban
   40 |  * @since 5.6
   41 |  */
   42 | public final class PostAuthorizeAuthorizationManager
   43 | 		implements AuthorizationManager<MethodInvocationResult>, MethodAuthorizationDeniedHandler {
   44 | 
   45 | 	private PostAuthorizeExpressionAttributeRegistry registry = new PostAuthorizeExpressionAttributeRegistry();
   46 | 
   47 | 	/**
   48 | 	 * Use this the {@link MethodSecurityExpressionHandler}.
   49 | 	 * @param expressionHandler the {@link MethodSecurityExpressionHandler} to use
   50 | 	 */
   51 | 	public void setExpressionHandler(MethodSecurityExpressionHandler expressionHandler) {
   52 | 		this.registry.setExpressionHandler(expressionHandler);
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * Configure pre/post-authorization template resolution
   57 | 	 * <p>
   58 | 	 * By default, this value is <code>null</code>, which indicates that templates should
   59 | 	 * not be resolved.
   60 | 	 * @param defaults - whether to resolve pre/post-authorization templates parameters
   61 | 	 * @since 6.4
   62 | 	 */
   63 | 	public void setTemplateDefaults(AnnotationTemplateExpressionDefaults defaults) {
   64 | 		this.registry.setTemplateDefaults(defaults);
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Invokes
   69 | 	 * {@link PostAuthorizeExpressionAttributeRegistry#setApplicationContext(ApplicationContext)}
   70 | 	 * with the provided {@link ApplicationContext}.
   71 | 	 * @param context the {@link ApplicationContext}
   72 | 	 * @since 6.3
   73 | 	 * @see PreAuthorizeExpressionAttributeRegistry#setApplicationContext(ApplicationContext)
   74 | 	 */
   75 | 	public void setApplicationContext(ApplicationContext context) {
   76 | 		this.registry.setApplicationContext(context);
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Determine if an {@link Authentication} has access to the returned object by
   81 | 	 * evaluating the {@link PostAuthorize} annotation that the {@link MethodInvocation}
   82 | 	 * specifies.
   83 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   84 | 	 * @param mi the {@link MethodInvocationResult} to check
   85 | 	 * @return an {@link AuthorizationDecision} or {@code null} if the
   86 | 	 * {@link PostAuthorize} annotation is not present
   87 | 	 */
   88 | 	@Override
   89 | 	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   90 | 			MethodInvocationResult mi) {
   91 | 		ExpressionAttribute attribute = this.registry.getAttribute(mi.getMethodInvocation());
   92 | 		if (attribute == null) {
   93 | 			return null;
   94 | 		}
   95 | 		MethodSecurityExpressionHandler expressionHandler = this.registry.getExpressionHandler();
   96 | 		EvaluationContext ctx = expressionHandler.createEvaluationContext(authentication, mi.getMethodInvocation());
   97 | 		expressionHandler.setReturnObject(mi.getResult(), ctx);
   98 | 		return ExpressionUtils.evaluate(attribute.getExpression(), ctx, authentication, mi);
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public @Nullable Object handleDeniedInvocation(MethodInvocation methodInvocation,
  103 | 			AuthorizationResult authorizationResult) {
  104 | 		ExpressionAttribute attribute = this.registry.getAttribute(methodInvocation);
  105 | 		PostAuthorizeExpressionAttribute postAuthorizeAttribute = (PostAuthorizeExpressionAttribute) attribute;
  106 | 		return postAuthorizeAttribute.getHandler().handleDeniedInvocation(methodInvocation, authorizationResult);
  107 | 	}
  108 | 
  109 | 	@Override
  110 | 	public @Nullable Object handleDeniedInvocationResult(MethodInvocationResult methodInvocationResult,
  111 | 			AuthorizationResult authorizationResult) {
  112 | 		ExpressionAttribute attribute = this.registry.getAttribute(methodInvocationResult.getMethodInvocation());
  113 | 		PostAuthorizeExpressionAttribute postAuthorizeAttribute = (PostAuthorizeExpressionAttribute) attribute;
  114 | 		return postAuthorizeAttribute.getHandler()
  115 | 			.handleDeniedInvocationResult(methodInvocationResult, authorizationResult);
  116 | 	}
  117 | 
  118 | }
```
