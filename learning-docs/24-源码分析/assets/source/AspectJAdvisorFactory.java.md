<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/annotation/AspectJAdvisorFactory.java -->
<!-- 正文引用：[[源码:AspectJAdvisorFactory.java#L起-L止]] ；代码块头部：(src: AspectJAdvisorFactory.java#L起-L止) -->

# AspectJAdvisorFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/annotation/AspectJAdvisorFactory.java` |
| 行数 | 105 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:01.161Z |

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
   17 | package org.springframework.aop.aspectj.annotation;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.List;
   21 | 
   22 | import org.aopalliance.aop.Advice;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aop.Advisor;
   26 | import org.springframework.aop.aspectj.AspectJExpressionPointcut;
   27 | import org.springframework.aop.framework.AopConfigException;
   28 | 
   29 | /**
   30 |  * Interface for factories that can create Spring AOP Advisors from classes
   31 |  * annotated with AspectJ annotation syntax.
   32 |  *
   33 |  * @author Rod Johnson
   34 |  * @author Juergen Hoeller
   35 |  * @since 2.0
   36 |  * @see AspectMetadata
   37 |  * @see org.aspectj.lang.reflect.AjTypeSystem
   38 |  */
   39 | public interface AspectJAdvisorFactory {
   40 | 
   41 | 	/**
   42 | 	 * Determine whether the given class is an aspect, as reported
   43 | 	 * by AspectJ's {@link org.aspectj.lang.reflect.AjTypeSystem}.
   44 | 	 * <p>Will simply return {@code false} if the supposed aspect is
   45 | 	 * invalid (such as an extension of a concrete aspect class).
   46 | 	 * Will return true for some aspects that Spring AOP cannot process,
   47 | 	 * such as those with unsupported instantiation models.
   48 | 	 * Use the {@link #validate} method to handle these cases if necessary.
   49 | 	 * @param clazz the supposed annotation-style AspectJ class
   50 | 	 * @return whether this class is recognized by AspectJ as an aspect class
   51 | 	 */
   52 | 	boolean isAspect(Class<?> clazz);
   53 | 
   54 | 	/**
   55 | 	 * Is the given class a valid AspectJ aspect class?
   56 | 	 * @param aspectClass the supposed AspectJ annotation-style class to validate
   57 | 	 * @throws AopConfigException if the class is an invalid aspect
   58 | 	 * (which can never be legal)
   59 | 	 * @throws NotAnAtAspectException if the class is not an aspect at all
   60 | 	 * (which may or may not be legal, depending on the context)
   61 | 	 */
   62 | 	void validate(Class<?> aspectClass) throws AopConfigException;
   63 | 
   64 | 	/**
   65 | 	 * Build Spring AOP Advisors for all annotated At-AspectJ methods
   66 | 	 * on the specified aspect instance.
   67 | 	 * @param aspectInstanceFactory the aspect instance factory
   68 | 	 * (not the aspect instance itself in order to avoid eager instantiation)
   69 | 	 * @return a list of advisors for this class
   70 | 	 */
   71 | 	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);
   72 | 
   73 | 	/**
   74 | 	 * Build a Spring AOP Advisor for the given AspectJ advice method.
   75 | 	 * @param candidateAdviceMethod the candidate advice method
   76 | 	 * @param aspectInstanceFactory the aspect instance factory
   77 | 	 * @param declarationOrder the declaration order within the aspect
   78 | 	 * @param aspectName the name of the aspect
   79 | 	 * @return {@code null} if the method is not an AspectJ advice method
   80 | 	 * or if it is a pointcut that will be used by other advice but will not
   81 | 	 * create a Spring advice in its own right
   82 | 	 */
   83 | 	@Nullable Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
   84 | 			int declarationOrder, String aspectName);
   85 | 
   86 | 	/**
   87 | 	 * Build a Spring AOP Advice for the given AspectJ advice method.
   88 | 	 * @param candidateAdviceMethod the candidate advice method
   89 | 	 * @param expressionPointcut the AspectJ expression pointcut
   90 | 	 * @param aspectInstanceFactory the aspect instance factory
   91 | 	 * @param declarationOrder the declaration order within the aspect
   92 | 	 * @param aspectName the name of the aspect
   93 | 	 * @return {@code null} if the method is not an AspectJ advice method
   94 | 	 * or if it is a pointcut that will be used by other advice but will not
   95 | 	 * create a Spring advice in its own right
   96 | 	 * @see org.springframework.aop.aspectj.AspectJAroundAdvice
   97 | 	 * @see org.springframework.aop.aspectj.AspectJMethodBeforeAdvice
   98 | 	 * @see org.springframework.aop.aspectj.AspectJAfterAdvice
   99 | 	 * @see org.springframework.aop.aspectj.AspectJAfterReturningAdvice
  100 | 	 * @see org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
  101 | 	 */
  102 | 	@Nullable Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
  103 | 			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);
  104 | 
  105 | }
```
