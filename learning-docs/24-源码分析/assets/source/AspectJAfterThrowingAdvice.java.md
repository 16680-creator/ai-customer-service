<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/AspectJAfterThrowingAdvice.java -->
<!-- 正文引用：[[源码:AspectJAfterThrowingAdvice.java#L起-L止]] ；代码块头部：(src: AspectJAfterThrowingAdvice.java#L起-L止) -->

# AspectJAfterThrowingAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/AspectJAfterThrowingAdvice.java` |
| 行数 | 81 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:00.303Z |

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
   17 | package org.springframework.aop.aspectj;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.Method;
   21 | 
   22 | import org.aopalliance.intercept.MethodInterceptor;
   23 | import org.aopalliance.intercept.MethodInvocation;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.aop.AfterAdvice;
   27 | 
   28 | /**
   29 |  * Spring AOP advice wrapping an AspectJ after-throwing advice method.
   30 |  *
   31 |  * @author Rod Johnson
   32 |  * @since 2.0
   33 |  */
   34 | @SuppressWarnings("serial")
   35 | public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
   36 | 		implements MethodInterceptor, AfterAdvice, Serializable {
   37 | 
   38 | 	public AspectJAfterThrowingAdvice(
   39 | 			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
   40 | 
   41 | 		super(aspectJBeforeAdviceMethod, pointcut, aif);
   42 | 	}
   43 | 
   44 | 
   45 | 	@Override
   46 | 	public boolean isBeforeAdvice() {
   47 | 		return false;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public boolean isAfterAdvice() {
   52 | 		return true;
   53 | 	}
   54 | 
   55 | 	@Override
   56 | 	public void setThrowingName(String name) {
   57 | 		setThrowingNameNoCheck(name);
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
   62 | 		try {
   63 | 			return mi.proceed();
   64 | 		}
   65 | 		catch (Throwable ex) {
   66 | 			if (shouldInvokeOnThrowing(ex)) {
   67 | 				invokeAdviceMethod(getJoinPointMatch(), null, ex);
   68 | 			}
   69 | 			throw ex;
   70 | 		}
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * In AspectJ semantics, after throwing advice that specifies a throwing clause
   75 | 	 * is only invoked if the thrown exception is a subtype of the given throwing type.
   76 | 	 */
   77 | 	private boolean shouldInvokeOnThrowing(Throwable ex) {
   78 | 		return getDiscoveredThrowingType().isInstance(ex);
   79 | 	}
   80 | 
   81 | }
```
