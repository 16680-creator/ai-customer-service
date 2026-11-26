<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/AspectJAroundAdvice.java -->
<!-- 正文引用：[[源码:AspectJAroundAdvice.java#L起-L止]] ；代码块头部：(src: AspectJAroundAdvice.java#L起-L止) -->

# AspectJAroundAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/AspectJAroundAdvice.java` |
| 行数 | 84 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:00.499Z |

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
   24 | import org.aspectj.lang.ProceedingJoinPoint;
   25 | import org.aspectj.weaver.tools.JoinPointMatch;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.aop.ProxyMethodInvocation;
   29 | 
   30 | /**
   31 |  * Spring AOP around advice (MethodInterceptor) that wraps
   32 |  * an AspectJ advice method. Exposes ProceedingJoinPoint.
   33 |  *
   34 |  * @author Rod Johnson
   35 |  * @author Juergen Hoeller
   36 |  * @since 2.0
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor, Serializable {
   40 | 
   41 | 	public AspectJAroundAdvice(
   42 | 			Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
   43 | 
   44 | 		super(aspectJAroundAdviceMethod, pointcut, aif);
   45 | 	}
   46 | 
   47 | 
   48 | 	@Override
   49 | 	public boolean isBeforeAdvice() {
   50 | 		return false;
   51 | 	}
   52 | 
   53 | 	@Override
   54 | 	public boolean isAfterAdvice() {
   55 | 		return false;
   56 | 	}
   57 | 
   58 | 	@Override
   59 | 	protected boolean supportsProceedingJoinPoint() {
   60 | 		return true;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
   65 | 		if (!(mi instanceof ProxyMethodInvocation pmi)) {
   66 | 			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
   67 | 		}
   68 | 		ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
   69 | 		JoinPointMatch jpm = getJoinPointMatch(pmi);
   70 | 		return invokeAdviceMethod(pjp, jpm, null, null);
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Return the ProceedingJoinPoint for the current invocation,
   75 | 	 * instantiating it lazily if it hasn't been bound to the thread already.
   76 | 	 * @param rmi the current Spring AOP ReflectiveMethodInvocation,
   77 | 	 * which we'll use for attribute binding
   78 | 	 * @return the ProceedingJoinPoint to make available to advice methods
   79 | 	 */
   80 | 	protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
   81 | 		return new MethodInvocationProceedingJoinPoint(rmi);
   82 | 	}
   83 | 
   84 | }
```
