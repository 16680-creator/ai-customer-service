<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/expression/WebExpressionAuthorizationManager.java -->
<!-- 正文引用：[[源码:WebExpressionAuthorizationManager.java#L起-L止]] ；代码块头部：(src: WebExpressionAuthorizationManager.java#L起-L止) -->

# WebExpressionAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/expression/WebExpressionAuthorizationManager.java` |
| 行数 | 174 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.647Z |

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
   23 | import org.springframework.beans.BeansException;
   24 | import org.springframework.context.ApplicationContext;
   25 | import org.springframework.context.ApplicationContextAware;
   26 | import org.springframework.expression.EvaluationContext;
   27 | import org.springframework.expression.Expression;
   28 | import org.springframework.security.access.expression.ExpressionUtils;
   29 | import org.springframework.security.access.expression.SecurityExpressionHandler;
   30 | import org.springframework.security.authorization.AuthorizationManager;
   31 | import org.springframework.security.authorization.AuthorizationResult;
   32 | import org.springframework.security.authorization.ExpressionAuthorizationDecision;
   33 | import org.springframework.security.core.Authentication;
   34 | import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
   35 | import org.springframework.util.Assert;
   36 | 
   37 | /**
   38 |  * An expression-based {@link AuthorizationManager} that determines the access by
   39 |  * evaluating the provided expression.
   40 |  *
   41 |  * @author Evgeniy Cheban
   42 |  * @since 5.8
   43 |  */
   44 | public final class WebExpressionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
   45 | 
   46 | 	private SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler = new DefaultHttpSecurityExpressionHandler();
   47 | 
   48 | 	private Expression expression;
   49 | 
   50 | 	/**
   51 | 	 * Creates an instance.
   52 | 	 * @param expressionString the raw expression string to parse
   53 | 	 */
   54 | 	public WebExpressionAuthorizationManager(String expressionString) {
   55 | 		Assert.hasText(expressionString, "expressionString cannot be empty");
   56 | 		this.expression = this.expressionHandler.getExpressionParser().parseExpression(expressionString);
   57 | 	}
   58 | 
   59 | 	private WebExpressionAuthorizationManager(String expressionString,
   60 | 			SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler) {
   61 | 		Assert.hasText(expressionString, "expressionString cannot be empty");
   62 | 		this.expressionHandler = expressionHandler;
   63 | 		this.expression = expressionHandler.getExpressionParser().parseExpression(expressionString);
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * Sets the {@link SecurityExpressionHandler} to be used. The default is
   68 | 	 * {@link DefaultHttpSecurityExpressionHandler}.
   69 | 	 * @param expressionHandler the {@link SecurityExpressionHandler} to use
   70 | 	 * @deprecated Please use {@link #withDefaults()} or {@link #withExpressionHandler}
   71 | 	 */
   72 | 	@Deprecated
   73 | 	public void setExpressionHandler(SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler) {
   74 | 		Assert.notNull(expressionHandler, "expressionHandler cannot be null");
   75 | 		this.expressionHandler = expressionHandler;
   76 | 		this.expression = expressionHandler.getExpressionParser()
   77 | 			.parseExpression(this.expression.getExpressionString());
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Determines the access by evaluating the provided expression.
   82 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
   83 | 	 * @param context the {@link RequestAuthorizationContext} to check
   84 | 	 * @return an {@link ExpressionAuthorizationDecision} based on the evaluated
   85 | 	 * expression
   86 | 	 */
   87 | 	@Override
   88 | 	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   89 | 			RequestAuthorizationContext context) {
   90 | 		EvaluationContext ctx = this.expressionHandler.createEvaluationContext(authentication, context);
   91 | 		boolean granted = ExpressionUtils.evaluateAsBoolean(this.expression, ctx);
   92 | 		return new ExpressionAuthorizationDecision(granted, this.expression);
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public String toString() {
   97 | 		return "WebExpressionAuthorizationManager[expression='" + this.expression + "']";
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Use a {@link DefaultHttpSecurityExpressionHandler} to create
  102 | 	 * {@link WebExpressionAuthorizationManager} instances.
  103 | 	 *
  104 | 	 * <p>
  105 | 	 * Note that publishing the {@link Builder} as a bean will allow the default
  106 | 	 * expression handler to be configured with a bean provider so that expressions can
  107 | 	 * reference beans
  108 | 	 * @return a {@link Builder} for constructing
  109 | 	 * {@link WebExpressionAuthorizationManager} instances
  110 | 	 * @since 7.0
  111 | 	 */
  112 | 	public static Builder withDefaults() {
  113 | 		return new Builder();
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Use this {@link SecurityExpressionHandler} to create
  118 | 	 * {@link WebExpressionAuthorizationManager} instances.
  119 | 	 * @param expressionHandler
  120 | 	 * @return a {@link Builder} for constructing
  121 | 	 * {@link WebExpressionAuthorizationManager} instances
  122 | 	 * @since 7.0
  123 | 	 */
  124 | 	public static Builder withExpressionHandler(
  125 | 			SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler) {
  126 | 		return new Builder(expressionHandler);
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * A {@link Builder} for constructing {@link WebExpressionAuthorizationManager}
  131 | 	 * instances.
  132 | 	 *
  133 | 	 * <p>
  134 | 	 * May be reused to create multiple instances.
  135 | 	 *
  136 | 	 * @author Josh Cummings
  137 | 	 * @since 7.0
  138 | 	 */
  139 | 	public static final class Builder implements ApplicationContextAware {
  140 | 
  141 | 		private final SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler;
  142 | 
  143 | 		private final boolean defaultExpressionHandler;
  144 | 
  145 | 		private Builder() {
  146 | 			this.expressionHandler = new DefaultHttpSecurityExpressionHandler();
  147 | 			this.defaultExpressionHandler = true;
  148 | 		}
  149 | 
  150 | 		private Builder(SecurityExpressionHandler<RequestAuthorizationContext> expressionHandler) {
  151 | 			this.expressionHandler = expressionHandler;
  152 | 			this.defaultExpressionHandler = false;
  153 | 		}
  154 | 
  155 | 		/**
  156 | 		 * Create a {@link WebExpressionAuthorizationManager} using this
  157 | 		 * {@code expression}.
  158 | 		 * @param expression the expression to evaluate
  159 | 		 * @return the resulting {@link AuthorizationManager}
  160 | 		 */
  161 | 		public WebExpressionAuthorizationManager expression(String expression) {
  162 | 			return new WebExpressionAuthorizationManager(expression, this.expressionHandler);
  163 | 		}
  164 | 
  165 | 		@Override
  166 | 		public void setApplicationContext(ApplicationContext context) throws BeansException {
  167 | 			if (this.defaultExpressionHandler) {
  168 | 				((DefaultHttpSecurityExpressionHandler) this.expressionHandler).setApplicationContext(context);
  169 | 			}
  170 | 		}
  171 | 
  172 | 	}
  173 | 
  174 | }
```
