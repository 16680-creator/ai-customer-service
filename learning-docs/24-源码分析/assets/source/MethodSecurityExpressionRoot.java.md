<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/expression/method/MethodSecurityExpressionRoot.java -->
<!-- 正文引用：[[源码:MethodSecurityExpressionRoot.java#L起-L止]] ；代码块头部：(src: MethodSecurityExpressionRoot.java#L起-L止) -->

# MethodSecurityExpressionRoot.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/expression/method/MethodSecurityExpressionRoot.java` |
| 行数 | 84 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:47:23.020Z |

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
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.aopalliance.intercept.MethodInvocation;
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.access.expression.SecurityExpressionRoot;
   25 | import org.springframework.security.core.Authentication;
   26 | 
   27 | /**
   28 |  * Extended expression root object which contains extra method-specific functionality.
   29 |  *
   30 |  * @author Luke Taylor
   31 |  * @author Evgeniy Cheban
   32 |  * @author Steve Riesenberg
   33 |  * @since 3.0
   34 |  */
   35 | class MethodSecurityExpressionRoot extends SecurityExpressionRoot<MethodInvocation>
   36 | 		implements MethodSecurityExpressionOperations {
   37 | 
   38 | 	private @Nullable Object filterObject;
   39 | 
   40 | 	private @Nullable Object returnObject;
   41 | 
   42 | 	private @Nullable Object target;
   43 | 
   44 | 	MethodSecurityExpressionRoot(Supplier<? extends @Nullable Authentication> authentication,
   45 | 			MethodInvocation methodInvocation) {
   46 | 		super(authentication, methodInvocation);
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public void setFilterObject(Object filterObject) {
   51 | 		this.filterObject = filterObject;
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public @Nullable Object getFilterObject() {
   56 | 		return this.filterObject;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public void setReturnObject(@Nullable Object returnObject) {
   61 | 		this.returnObject = returnObject;
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	public @Nullable Object getReturnObject() {
   66 | 		return this.returnObject;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Sets the "this" property for use in expressions. Typically this will be the "this"
   71 | 	 * property of the {@code JoinPoint} representing the method invocation which is being
   72 | 	 * protected.
   73 | 	 * @param target the target object on which the method in is being invoked.
   74 | 	 */
   75 | 	void setThis(@Nullable Object target) {
   76 | 		this.target = target;
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public @Nullable Object getThis() {
   81 | 		return this.target;
   82 | 	}
   83 | 
   84 | }
```
