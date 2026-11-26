<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/PreFilterAuthorizationMethodInterceptor.java -->
<!-- 正文引用：[[源码:PreFilterAuthorizationMethodInterceptor.java#L起-L止]] ；代码块头部：(src: PreFilterAuthorizationMethodInterceptor.java#L起-L止) -->

# PreFilterAuthorizationMethodInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/PreFilterAuthorizationMethodInterceptor.java` |
| 行数 | 171 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.435Z |

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
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.aop.Pointcut;
   27 | import org.springframework.expression.EvaluationContext;
   28 | import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
   29 | import org.springframework.security.access.prepost.PreFilter;
   30 | import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
   31 | import org.springframework.security.core.Authentication;
   32 | import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
   33 | import org.springframework.security.core.context.SecurityContextHolder;
   34 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.util.StringUtils;
   37 | 
   38 | /**
   39 |  * A {@link MethodInterceptor} which filters a method argument by evaluating an expression
   40 |  * from the {@link PreFilter} annotation.
   41 |  *
   42 |  * @author Evgeniy Cheban
   43 |  * @author Josh Cummings
   44 |  * @since 5.6
   45 |  */
   46 | public final class PreFilterAuthorizationMethodInterceptor implements AuthorizationAdvisor {
   47 | 
   48 | 	private Supplier<SecurityContextHolderStrategy> securityContextHolderStrategy = SecurityContextHolder::getContextHolderStrategy;
   49 | 
   50 | 	private PreFilterExpressionAttributeRegistry registry = new PreFilterExpressionAttributeRegistry();
   51 | 
   52 | 	private int order = AuthorizationInterceptorsOrder.PRE_FILTER.getOrder();
   53 | 
   54 | 	private final Pointcut pointcut;
   55 | 
   56 | 	/**
   57 | 	 * Creates a {@link PreFilterAuthorizationMethodInterceptor} using the provided
   58 | 	 * parameters.
   59 | 	 */
   60 | 	public PreFilterAuthorizationMethodInterceptor() {
   61 | 		this.pointcut = AuthorizationMethodPointcuts.forAnnotations(PreFilter.class);
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Use this {@link MethodSecurityExpressionHandler}.
   66 | 	 * @param expressionHandler the {@link MethodSecurityExpressionHandler} to use
   67 | 	 */
   68 | 	public void setExpressionHandler(MethodSecurityExpressionHandler expressionHandler) {
   69 | 		this.registry.setExpressionHandler(expressionHandler);
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Configure pre/post-authorization template resolution
   74 | 	 * <p>
   75 | 	 * By default, this value is <code>null</code>, which indicates that templates should
   76 | 	 * not be resolved.
   77 | 	 * @param defaults - whether to resolve pre/post-authorization templates parameters
   78 | 	 * @since 6.4
   79 | 	 */
   80 | 	public void setTemplateDefaults(AnnotationTemplateExpressionDefaults defaults) {
   81 | 		this.registry.setTemplateDefaults(defaults);
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * {@inheritDoc}
   86 | 	 */
   87 | 	@Override
   88 | 	public int getOrder() {
   89 | 		return this.order;
   90 | 	}
   91 | 
   92 | 	public void setOrder(int order) {
   93 | 		this.order = order;
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * {@inheritDoc}
   98 | 	 */
   99 | 	@Override
  100 | 	public Pointcut getPointcut() {
  101 | 		return this.pointcut;
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	public Advice getAdvice() {
  106 | 		return this;
  107 | 	}
  108 | 
  109 | 	@Override
  110 | 	public boolean isPerInstance() {
  111 | 		return true;
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  116 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  117 | 	 *
  118 | 	 * @since 5.8
  119 | 	 */
  120 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy strategy) {
  121 | 		this.securityContextHolderStrategy = () -> strategy;
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * Filter the method argument specified in the {@link PreFilter} annotation that
  126 | 	 * {@link MethodInvocation} specifies.
  127 | 	 * @param mi the {@link MethodInvocation} to check
  128 | 	 */
  129 | 	@Override
  130 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
  131 | 		PreFilterExpressionAttributeRegistry.PreFilterExpressionAttribute attribute = this.registry.getAttribute(mi);
  132 | 		if (attribute == null) {
  133 | 			return mi.proceed();
  134 | 		}
  135 | 		MethodSecurityExpressionHandler expressionHandler = this.registry.getExpressionHandler();
  136 | 		EvaluationContext ctx = expressionHandler.createEvaluationContext(this::getAuthentication, mi);
  137 | 		Object filterTarget = findFilterTarget(attribute.getFilterTarget(), ctx, mi);
  138 | 		expressionHandler.filter(filterTarget, attribute.getExpression(), ctx);
  139 | 		return mi.proceed();
  140 | 	}
  141 | 
  142 | 	private Object findFilterTarget(String filterTargetName, EvaluationContext ctx, MethodInvocation methodInvocation) {
  143 | 		Object filterTarget;
  144 | 		if (StringUtils.hasText(filterTargetName)) {
  145 | 			filterTarget = ctx.lookupVariable(filterTargetName);
  146 | 			Assert.notNull(filterTarget, () -> "Filter target was null, or no argument with name '" + filterTargetName
  147 | 					+ "' found in method.");
  148 | 		}
  149 | 		else {
  150 | 			@Nullable Object[] arguments = methodInvocation.getArguments();
  151 | 			Assert.state(arguments.length == 1,
  152 | 					"Unable to determine the method argument for filtering. Specify the filter target.");
  153 | 			filterTarget = arguments[0];
  154 | 			Assert.notNull(filterTarget,
  155 | 					"Filter target was null. Make sure you passing the correct value in the method argument.");
  156 | 		}
  157 | 		Assert.state(!filterTarget.getClass().isArray(),
  158 | 				"Pre-filtering on array types is not supported. Using a Collection will solve this problem.");
  159 | 		return filterTarget;
  160 | 	}
  161 | 
  162 | 	private Authentication getAuthentication() {
  163 | 		Authentication authentication = this.securityContextHolderStrategy.get().getContext().getAuthentication();
  164 | 		if (authentication == null) {
  165 | 			throw new AuthenticationCredentialsNotFoundException(
  166 | 					"An Authentication object was not found in the SecurityContext");
  167 | 		}
  168 | 		return authentication;
  169 | 	}
  170 | 
  171 | }
```
