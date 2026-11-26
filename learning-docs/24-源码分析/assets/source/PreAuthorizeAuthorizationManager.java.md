<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/PreAuthorizeAuthorizationManager.java -->
<!-- 正文引用：[[源码:PreAuthorizeAuthorizationManager.java#L起-L止]] ；代码块头部：(src: PreAuthorizeAuthorizationManager.java#L起-L止) -->

# PreAuthorizeAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/PreAuthorizeAuthorizationManager.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.296Z |

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
   27 | import org.springframework.security.access.prepost.PreAuthorize;
   28 | import org.springframework.security.authorization.AuthorizationDecision;
   29 | import org.springframework.security.authorization.AuthorizationManager;
   30 | import org.springframework.security.authorization.AuthorizationResult;
   31 | import org.springframework.security.core.Authentication;
   32 | import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
   33 | 
   34 | /**
   35 |  * An {@link AuthorizationManager} which can determine if an {@link Authentication} may
   36 |  * invoke the {@link MethodInvocation} by evaluating an expression from the
   37 |  * {@link PreAuthorize} annotation.
   38 |  *
   39 |  * @author Evgeniy Cheban
   40 |  * @since 5.6
   41 |  */
   42 | public final class PreAuthorizeAuthorizationManager
   43 | 		implements AuthorizationManager<MethodInvocation>, MethodAuthorizationDeniedHandler {
   44 | 
   45 | 	private PreAuthorizeExpressionAttributeRegistry registry = new PreAuthorizeExpressionAttributeRegistry();
   46 | 
   47 | 	/**
   48 | 	 * Sets the {@link MethodSecurityExpressionHandler}.
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
   67 | 	public void setApplicationContext(ApplicationContext context) {
   68 | 		this.registry.setApplicationContext(context);
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Determine if an {@link Authentication} has access to a method by evaluating an
   73 | 	 * expression from the {@link PreAuthorize} annotation that the
   74 | 	 * {@link MethodInvocation} specifies.
   75 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   76 | 	 * @param mi the {@link MethodInvocation} to check
   77 | 	 * @return an {@link AuthorizationDecision} or {@code null} if the
   78 | 	 * {@link PreAuthorize} annotation is not present
   79 | 	 */
   80 | 	@Override
   81 | 	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   82 | 			MethodInvocation mi) {
   83 | 		ExpressionAttribute attribute = this.registry.getAttribute(mi);
   84 | 		if (attribute == null) {
   85 | 			return null;
   86 | 		}
   87 | 		EvaluationContext ctx = this.registry.getExpressionHandler().createEvaluationContext(authentication, mi);
   88 | 		return ExpressionUtils.evaluate(attribute.getExpression(), ctx, authentication, mi);
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public @Nullable Object handleDeniedInvocation(MethodInvocation methodInvocation,
   93 | 			AuthorizationResult authorizationResult) {
   94 | 		ExpressionAttribute attribute = this.registry.getAttribute(methodInvocation);
   95 | 		PreAuthorizeExpressionAttribute preAuthorizeAttribute = (PreAuthorizeExpressionAttribute) attribute;
   96 | 		return preAuthorizeAttribute.getHandler().handleDeniedInvocation(methodInvocation, authorizationResult);
   97 | 	}
   98 | 
   99 | }
```
