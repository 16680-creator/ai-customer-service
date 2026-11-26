<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/MethodExpressionAuthorizationManager.java -->
<!-- 正文引用：[[源码:MethodExpressionAuthorizationManager.java#L起-L止]] ；代码块头部：(src: MethodExpressionAuthorizationManager.java#L起-L止) -->

# MethodExpressionAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/MethodExpressionAuthorizationManager.java` |
| 行数 | 90 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.685Z |

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
   24 | import org.springframework.expression.EvaluationContext;
   25 | import org.springframework.expression.Expression;
   26 | import org.springframework.security.access.expression.ExpressionUtils;
   27 | import org.springframework.security.access.expression.SecurityExpressionHandler;
   28 | import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
   29 | import org.springframework.security.authorization.AuthorizationManager;
   30 | import org.springframework.security.authorization.AuthorizationResult;
   31 | import org.springframework.security.authorization.ExpressionAuthorizationDecision;
   32 | import org.springframework.security.core.Authentication;
   33 | import org.springframework.util.Assert;
   34 | 
   35 | /**
   36 |  * An expression-based {@link AuthorizationManager} that determines the access by
   37 |  * evaluating the provided expression against the {@link MethodInvocation}.
   38 |  *
   39 |  * @author Josh Cummings
   40 |  * @since 5.8
   41 |  */
   42 | public final class MethodExpressionAuthorizationManager implements AuthorizationManager<MethodInvocation> {
   43 | 
   44 | 	private SecurityExpressionHandler<MethodInvocation> expressionHandler = new DefaultMethodSecurityExpressionHandler();
   45 | 
   46 | 	private Expression expression;
   47 | 
   48 | 	/**
   49 | 	 * Creates an instance.
   50 | 	 * @param expressionString the raw expression string to parse
   51 | 	 */
   52 | 	public MethodExpressionAuthorizationManager(String expressionString) {
   53 | 		Assert.hasText(expressionString, "expressionString cannot be empty");
   54 | 		this.expression = this.expressionHandler.getExpressionParser().parseExpression(expressionString);
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Sets the {@link SecurityExpressionHandler} to be used. The default is
   59 | 	 * {@link DefaultMethodSecurityExpressionHandler}.
   60 | 	 * @param expressionHandler the {@link SecurityExpressionHandler} to use
   61 | 	 */
   62 | 	public void setExpressionHandler(SecurityExpressionHandler<MethodInvocation> expressionHandler) {
   63 | 		Assert.notNull(expressionHandler, "expressionHandler cannot be null");
   64 | 		this.expressionHandler = expressionHandler;
   65 | 		this.expression = expressionHandler.getExpressionParser()
   66 | 			.parseExpression(this.expression.getExpressionString());
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Determines the access by evaluating the provided expression.
   71 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   72 | 	 * @param context the {@link MethodInvocation} to check
   73 | 	 * @return an {@link ExpressionAuthorizationDecision} based on the evaluated
   74 | 	 * expression
   75 | 	 */
   76 | 	@Override
   77 | 	@SuppressWarnings("NullAway") // FIXME: Dataflow analysis limitation
   78 | 	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   79 | 			MethodInvocation context) {
   80 | 		EvaluationContext ctx = this.expressionHandler.createEvaluationContext(authentication, context);
   81 | 		boolean granted = ExpressionUtils.evaluateAsBoolean(this.expression, ctx);
   82 | 		return new ExpressionAuthorizationDecision(granted, this.expression);
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public String toString() {
   87 | 		return "WebExpressionAuthorizationManager[expression='" + this.expression + "']";
   88 | 	}
   89 | 
   90 | }
```
