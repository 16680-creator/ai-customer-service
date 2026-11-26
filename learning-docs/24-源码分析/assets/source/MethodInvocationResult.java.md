<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/MethodInvocationResult.java -->
<!-- 正文引用：[[源码:MethodInvocationResult.java#L起-L止]] ；代码块头部：(src: MethodInvocationResult.java#L起-L止) -->

# MethodInvocationResult.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/MethodInvocationResult.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.566Z |

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
   19 | import org.aopalliance.intercept.MethodInvocation;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * A context object that contains a {@link MethodInvocation} and the result of that
   26 |  * {@link MethodInvocation}.
   27 |  *
   28 |  * @author Josh Cummings
   29 |  * @since 5.6
   30 |  */
   31 | public class MethodInvocationResult {
   32 | 
   33 | 	private final MethodInvocation methodInvocation;
   34 | 
   35 | 	private final @Nullable Object result;
   36 | 
   37 | 	/**
   38 | 	 * Construct a {@link MethodInvocationResult} with the provided parameters.
   39 | 	 * @param methodInvocation the already-invoked {@link MethodInvocation}
   40 | 	 * @param result the value returned from the {@link MethodInvocation}
   41 | 	 */
   42 | 	public MethodInvocationResult(MethodInvocation methodInvocation, @Nullable Object result) {
   43 | 		Assert.notNull(methodInvocation, "methodInvocation cannot be null");
   44 | 		this.methodInvocation = methodInvocation;
   45 | 		this.result = result;
   46 | 	}
   47 | 
   48 | 	/**
   49 | 	 * Return the already-invoked {@link MethodInvocation}.
   50 | 	 * @return the already-invoked {@link MethodInvocation}
   51 | 	 */
   52 | 	public MethodInvocation getMethodInvocation() {
   53 | 		return this.methodInvocation;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Return the result of the already-invoked {@link MethodInvocation}.
   58 | 	 * @return the result
   59 | 	 */
   60 | 	public @Nullable Object getResult() {
   61 | 		return this.result;
   62 | 	}
   63 | 
   64 | }
```
