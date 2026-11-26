<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/adapter/DefaultAdvisorAdapterRegistry.java -->
<!-- 正文引用：[[源码:DefaultAdvisorAdapterRegistry.java#L起-L止]] ；代码块头部：(src: DefaultAdvisorAdapterRegistry.java#L起-L止) -->

# DefaultAdvisorAdapterRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/adapter/DefaultAdvisorAdapterRegistry.java` |
| 行数 | 103 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:58.032Z |

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
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | 
   23 | import org.aopalliance.aop.Advice;
   24 | import org.aopalliance.intercept.MethodInterceptor;
   25 | 
   26 | import org.springframework.aop.Advisor;
   27 | import org.springframework.aop.support.DefaultPointcutAdvisor;
   28 | 
   29 | /**
   30 |  * Default implementation of the {@link AdvisorAdapterRegistry} interface.
   31 |  * Supports {@link org.aopalliance.intercept.MethodInterceptor},
   32 |  * {@link org.springframework.aop.MethodBeforeAdvice},
   33 |  * {@link org.springframework.aop.AfterReturningAdvice},
   34 |  * {@link org.springframework.aop.ThrowsAdvice}.
   35 |  *
   36 |  * @author Rod Johnson
   37 |  * @author Rob Harrop
   38 |  * @author Juergen Hoeller
   39 |  */
   40 | @SuppressWarnings("serial")
   41 | public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {
   42 | 
   43 | 	private static final MethodInterceptor[] EMPTY_METHOD_INTERCEPTOR_ARRAY = new MethodInterceptor[0];
   44 | 
   45 | 
   46 | 	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);
   47 | 
   48 | 
   49 | 	/**
   50 | 	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
   51 | 	 */
   52 | 	public DefaultAdvisorAdapterRegistry() {
   53 | 		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
   54 | 		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
   55 | 		registerAdvisorAdapter(new ThrowsAdviceAdapter());
   56 | 	}
   57 | 
   58 | 
   59 | 	@Override
   60 | 	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
   61 | 		if (adviceObject instanceof Advisor advisor) {
   62 | 			return advisor;
   63 | 		}
   64 | 		if (!(adviceObject instanceof Advice advice)) {
   65 | 			throw new UnknownAdviceTypeException(adviceObject);
   66 | 		}
   67 | 		if (advice instanceof MethodInterceptor) {
   68 | 			// So well-known it doesn't even need an adapter.
   69 | 			return new DefaultPointcutAdvisor(advice);
   70 | 		}
   71 | 		for (AdvisorAdapter adapter : this.adapters) {
   72 | 			// Check that it is supported.
   73 | 			if (adapter.supportsAdvice(advice)) {
   74 | 				return new DefaultPointcutAdvisor(advice);
   75 | 			}
   76 | 		}
   77 | 		throw new UnknownAdviceTypeException(advice);
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
   82 | 		List<MethodInterceptor> interceptors = new ArrayList<>(3);
   83 | 		Advice advice = advisor.getAdvice();
   84 | 		if (advice instanceof MethodInterceptor methodInterceptor) {
   85 | 			interceptors.add(methodInterceptor);
   86 | 		}
   87 | 		for (AdvisorAdapter adapter : this.adapters) {
   88 | 			if (adapter.supportsAdvice(advice)) {
   89 | 				interceptors.add(adapter.getInterceptor(advisor));
   90 | 			}
   91 | 		}
   92 | 		if (interceptors.isEmpty()) {
   93 | 			throw new UnknownAdviceTypeException(advisor.getAdvice());
   94 | 		}
   95 | 		return interceptors.toArray(EMPTY_METHOD_INTERCEPTOR_ARRAY);
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
  100 | 		this.adapters.add(adapter);
  101 | 	}
  102 | 
  103 | }
```
