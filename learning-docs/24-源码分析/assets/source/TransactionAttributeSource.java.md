<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/TransactionAttributeSource.java -->
<!-- 正文引用：[[源码:TransactionAttributeSource.java#L起-L止]] ；代码块头部：(src: TransactionAttributeSource.java#L起-L止) -->

# TransactionAttributeSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/TransactionAttributeSource.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.013Z |

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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | /**
   24 |  * Strategy interface used by {@link TransactionInterceptor} for metadata retrieval.
   25 |  *
   26 |  * <p>Implementations know how to source transaction attributes, whether from configuration,
   27 |  * metadata attributes at source level (such as annotations), or anywhere else.
   28 |  *
   29 |  * @author Rod Johnson
   30 |  * @author Juergen Hoeller
   31 |  * @since 15.04.2003
   32 |  * @see TransactionInterceptor#setTransactionAttributeSource
   33 |  * @see TransactionProxyFactoryBean#setTransactionAttributeSource
   34 |  * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
   35 |  */
   36 | public interface TransactionAttributeSource {
   37 | 
   38 | 	/**
   39 | 	 * Determine whether the given class is a candidate for transaction attributes
   40 | 	 * in the metadata format of this {@code TransactionAttributeSource}.
   41 | 	 * <p>If this method returns {@code false}, the methods on the given class
   42 | 	 * will not get traversed for {@link #getTransactionAttribute} introspection.
   43 | 	 * Returning {@code false} is therefore an optimization for non-affected
   44 | 	 * classes, whereas {@code true} simply means that the class needs to get
   45 | 	 * fully introspected for each method on the given class individually.
   46 | 	 * @param targetClass the class to introspect
   47 | 	 * @return {@code false} if the class is known to have no transaction
   48 | 	 * attributes at class or method level; {@code true} otherwise. The default
   49 | 	 * implementation returns {@code true}, leading to regular introspection.
   50 | 	 * @since 5.2
   51 | 	 * @see #hasTransactionAttribute
   52 | 	 */
   53 | 	default boolean isCandidateClass(Class<?> targetClass) {
   54 | 		return true;
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Determine whether there is a transaction attribute for the given method.
   59 | 	 * @param method the method to introspect
   60 | 	 * @param targetClass the target class (can be {@code null},
   61 | 	 * in which case the declaring class of the method must be used)
   62 | 	 * @since 6.2
   63 | 	 * @see #getTransactionAttribute
   64 | 	 */
   65 | 	default boolean hasTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
   66 | 		return (getTransactionAttribute(method, targetClass) != null);
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Return the transaction attribute for the given method,
   71 | 	 * or {@code null} if the method is non-transactional.
   72 | 	 * @param method the method to introspect
   73 | 	 * @param targetClass the target class (can be {@code null},
   74 | 	 * in which case the declaring class of the method must be used)
   75 | 	 * @return the matching transaction attribute, or {@code null} if none found
   76 | 	 */
   77 | 	@Nullable TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass);
   78 | 
   79 | }
```
