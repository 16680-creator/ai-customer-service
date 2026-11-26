<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/AspectJAfterReturningAdvice.java -->
<!-- 正文引用：[[源码:AspectJAfterReturningAdvice.java#L起-L止]] ；代码块头部：(src: AspectJAfterReturningAdvice.java#L起-L止) -->

# AspectJAfterReturningAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/AspectJAfterReturningAdvice.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:00.120Z |

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
   21 | import java.lang.reflect.Type;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aop.AfterAdvice;
   26 | import org.springframework.aop.AfterReturningAdvice;
   27 | import org.springframework.util.ClassUtils;
   28 | import org.springframework.util.TypeUtils;
   29 | 
   30 | /**
   31 |  * Spring AOP advice wrapping an AspectJ after-returning advice method.
   32 |  *
   33 |  * @author Rod Johnson
   34 |  * @author Juergen Hoeller
   35 |  * @author Ramnivas Laddad
   36 |  * @since 2.0
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
   40 | 		implements AfterReturningAdvice, AfterAdvice, Serializable {
   41 | 
   42 | 	public AspectJAfterReturningAdvice(
   43 | 			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
   44 | 
   45 | 		super(aspectJBeforeAdviceMethod, pointcut, aif);
   46 | 	}
   47 | 
   48 | 
   49 | 	@Override
   50 | 	public boolean isBeforeAdvice() {
   51 | 		return false;
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public boolean isAfterAdvice() {
   56 | 		return true;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public void setReturningName(String name) {
   61 | 		setReturningNameNoCheck(name);
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	public void afterReturning(@Nullable Object returnValue, Method method, @Nullable Object[] args, @Nullable Object target) throws Throwable {
   66 | 		if (shouldInvokeOnReturnValueOf(method, returnValue)) {
   67 | 			invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
   68 | 		}
   69 | 	}
   70 | 
   71 | 
   72 | 	/**
   73 | 	 * Following AspectJ semantics, if a returning clause was specified, then the
   74 | 	 * advice is only invoked if the returned value is an instance of the given
   75 | 	 * returning type and generic type parameters, if any, match the assignment
   76 | 	 * rules. If the returning type is Object, the advice is *always* invoked.
   77 | 	 * @param returnValue the return value of the target method
   78 | 	 * @return whether to invoke the advice method for the given return value
   79 | 	 */
   80 | 	private boolean shouldInvokeOnReturnValueOf(Method method, @Nullable Object returnValue) {
   81 | 		Class<?> type = getDiscoveredReturningType();
   82 | 		Type genericType = getDiscoveredReturningGenericType();
   83 | 		// If we aren't dealing with a raw type, check if generic parameters are assignable.
   84 | 		return (matchesReturnValue(type, method, returnValue) &&
   85 | 				(genericType == null || genericType == type ||
   86 | 						TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Following AspectJ semantics, if a return value is null (or return type is void),
   91 | 	 * then the return type of target method should be used to determine whether advice
   92 | 	 * is invoked or not. Also, even if the return type is void, if the type of argument
   93 | 	 * declared in the advice method is Object, then the advice must still get invoked.
   94 | 	 * @param type the type of argument declared in advice method
   95 | 	 * @param method the advice method
   96 | 	 * @param returnValue the return value of the target method
   97 | 	 * @return whether to invoke the advice method for the given return value and type
   98 | 	 */
   99 | 	private boolean matchesReturnValue(Class<?> type, Method method, @Nullable Object returnValue) {
  100 | 		if (returnValue != null) {
  101 | 			return ClassUtils.isAssignableValue(type, returnValue);
  102 | 		}
  103 | 		else if (Object.class == type && void.class == method.getReturnType()) {
  104 | 			return true;
  105 | 		}
  106 | 		else {
  107 | 			return ClassUtils.isAssignable(type, method.getReturnType());
  108 | 		}
  109 | 	}
  110 | 
  111 | }
```
