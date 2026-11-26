<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/expression/method/MethodSecurityExpressionHandler.java -->
<!-- 正文引用：[[源码:MethodSecurityExpressionHandler.java#L起-L止]] ；代码块头部：(src: MethodSecurityExpressionHandler.java#L起-L止) -->

# MethodSecurityExpressionHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/expression/method/MethodSecurityExpressionHandler.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.741Z |

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
   19 | import org.aopalliance.intercept.MethodInvocation;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.expression.EvaluationContext;
   23 | import org.springframework.expression.Expression;
   24 | import org.springframework.security.access.expression.SecurityExpressionHandler;
   25 | 
   26 | /**
   27 |  * Extended expression-handler facade which adds methods which are specific to securing
   28 |  * method invocations.
   29 |  *
   30 |  * @author Luke Taylor
   31 |  * @since 3.0
   32 |  */
   33 | public interface MethodSecurityExpressionHandler extends SecurityExpressionHandler<MethodInvocation> {
   34 | 
   35 | 	/**
   36 | 	 * Filters a target collection or array. Only applies to method invocations.
   37 | 	 * @param filterTarget the array or collection to be filtered.
   38 | 	 * @param filterExpression the expression which should be used as the filter
   39 | 	 * condition. If it returns false on evaluation, the object will be removed from the
   40 | 	 * returned collection
   41 | 	 * @param ctx the current evaluation context (as created through a call to
   42 | 	 * {@link #createEvaluationContext(org.springframework.security.core.Authentication, Object)}
   43 | 	 * @return the filtered collection or array
   44 | 	 */
   45 | 	Object filter(@Nullable Object filterTarget, Expression filterExpression, EvaluationContext ctx);
   46 | 
   47 | 	/**
   48 | 	 * Used to inform the expression system of the return object for the given evaluation
   49 | 	 * context. Only applies to method invocations.
   50 | 	 * @param returnObject the return object value
   51 | 	 * @param ctx the context within which the object should be set (as created through a
   52 | 	 * call to
   53 | 	 * {@link #createEvaluationContext(org.springframework.security.core.Authentication, Object)}
   54 | 	 */
   55 | 	void setReturnObject(@Nullable Object returnObject, EvaluationContext ctx);
   56 | 
   57 | }
```
