<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/adapter/MethodBeforeAdviceInterceptor.java -->
<!-- 正文引用：[[源码:MethodBeforeAdviceInterceptor.java#L起-L止]] ；代码块头部：(src: MethodBeforeAdviceInterceptor.java#L起-L止) -->

# MethodBeforeAdviceInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/adapter/MethodBeforeAdviceInterceptor.java` |
| 行数 | 60 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:58.253Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.aop.framework.adapter;
   18 | 
   19 | import java.io.Serializable;
   20 | 
   21 | import org.aopalliance.intercept.MethodInterceptor;
   22 | import org.aopalliance.intercept.MethodInvocation;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aop.BeforeAdvice;
   26 | import org.springframework.aop.MethodBeforeAdvice;
   27 | import org.springframework.util.Assert;
   28 | 
   29 | /**
   30 |  * Interceptor to wrap a {@link MethodBeforeAdvice}.
   31 |  * <p>Used internally by the AOP framework; application developers should not
   32 |  * need to use this class directly.
   33 |  *
   34 |  * @author Rod Johnson
   35 |  * @see AfterReturningAdviceInterceptor
   36 |  * @see ThrowsAdviceInterceptor
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {
   40 | 
   41 | 	private final MethodBeforeAdvice advice;
   42 | 
   43 | 
   44 | 	/**
   45 | 	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
   46 | 	 * @param advice the MethodBeforeAdvice to wrap
   47 | 	 */
   48 | 	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
   49 | 		Assert.notNull(advice, "Advice must not be null");
   50 | 		this.advice = advice;
   51 | 	}
   52 | 
   53 | 
   54 | 	@Override
   55 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
   56 | 		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
   57 | 		return mi.proceed();
   58 | 	}
   59 | 
   60 | }
```
