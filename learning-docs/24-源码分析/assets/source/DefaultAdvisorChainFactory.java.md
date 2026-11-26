<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/DefaultAdvisorChainFactory.java -->
<!-- 正文引用：[[源码:DefaultAdvisorChainFactory.java#L起-L止]] ；代码块头部：(src: DefaultAdvisorChainFactory.java#L起-L止) -->

# DefaultAdvisorChainFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/DefaultAdvisorChainFactory.java` |
| 行数 | 126 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.825Z |

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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.Method;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.List;
   24 | 
   25 | import org.aopalliance.intercept.Interceptor;
   26 | import org.aopalliance.intercept.MethodInterceptor;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.aop.Advisor;
   30 | import org.springframework.aop.IntroductionAdvisor;
   31 | import org.springframework.aop.IntroductionAwareMethodMatcher;
   32 | import org.springframework.aop.MethodMatcher;
   33 | import org.springframework.aop.PointcutAdvisor;
   34 | import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
   35 | import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
   36 | 
   37 | /**
   38 |  * A simple but definitive way of working out an advice chain for a Method,
   39 |  * given an {@link Advised} object. Always rebuilds each advice chain;
   40 |  * caching can be provided by subclasses.
   41 |  *
   42 |  * @author Juergen Hoeller
   43 |  * @author Rod Johnson
   44 |  * @author Adrian Colyer
   45 |  * @since 2.0.3
   46 |  */
   47 | @SuppressWarnings("serial")
   48 | public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {
   49 | 
   50 | 	/**
   51 | 	 * Singleton instance of this class.
   52 | 	 * @since 6.0.10
   53 | 	 */
   54 | 	public static final DefaultAdvisorChainFactory INSTANCE = new DefaultAdvisorChainFactory();
   55 | 
   56 | 
   57 | 	@Override
   58 | 	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
   59 | 			Advised config, Method method, @Nullable Class<?> targetClass) {
   60 | 
   61 | 		// This is somewhat tricky... We have to process introductions first,
   62 | 		// but we need to preserve order in the ultimate list.
   63 | 		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
   64 | 		Advisor[] advisors = config.getAdvisors();
   65 | 		List<Object> interceptorList = new ArrayList<>(advisors.length);
   66 | 		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
   67 | 		Boolean hasIntroductions = null;
   68 | 
   69 | 		for (Advisor advisor : advisors) {
   70 | 			if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
   71 | 				// Add it conditionally.
   72 | 				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
   73 | 					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
   74 | 					boolean match;
   75 | 					if (mm instanceof IntroductionAwareMethodMatcher iamm) {
   76 | 						if (hasIntroductions == null) {
   77 | 							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
   78 | 						}
   79 | 						match = iamm.matches(method, actualClass, hasIntroductions);
   80 | 					}
   81 | 					else {
   82 | 						match = mm.matches(method, actualClass);
   83 | 					}
   84 | 					if (match) {
   85 | 						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
   86 | 						if (mm.isRuntime()) {
   87 | 							// Creating a new object instance in the getInterceptors() method
   88 | 							// isn't a problem as we normally cache created chains.
   89 | 							for (MethodInterceptor interceptor : interceptors) {
   90 | 								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
   91 | 							}
   92 | 						}
   93 | 						else {
   94 | 							interceptorList.addAll(Arrays.asList(interceptors));
   95 | 						}
   96 | 					}
   97 | 				}
   98 | 			}
   99 | 			else if (advisor instanceof IntroductionAdvisor ia) {
  100 | 				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
  101 | 					Interceptor[] interceptors = registry.getInterceptors(advisor);
  102 | 					interceptorList.addAll(Arrays.asList(interceptors));
  103 | 				}
  104 | 			}
  105 | 			else {
  106 | 				Interceptor[] interceptors = registry.getInterceptors(advisor);
  107 | 				interceptorList.addAll(Arrays.asList(interceptors));
  108 | 			}
  109 | 		}
  110 | 
  111 | 		return interceptorList;
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Determine whether the Advisors contain matching introductions.
  116 | 	 */
  117 | 	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
  118 | 		for (Advisor advisor : advisors) {
  119 | 			if (advisor instanceof IntroductionAdvisor ia && ia.getClassFilter().matches(actualClass)) {
  120 | 				return true;
  121 | 			}
  122 | 		}
  123 | 		return false;
  124 | 	}
  125 | 
  126 | }
```
