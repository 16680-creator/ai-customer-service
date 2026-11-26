<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/TransactionAttributeSourcePointcut.java -->
<!-- 正文引用：[[源码:TransactionAttributeSourcePointcut.java#L起-L止]] ；代码块头部：(src: TransactionAttributeSourcePointcut.java#L起-L止) -->

# TransactionAttributeSourcePointcut.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/TransactionAttributeSourcePointcut.java` |
| 行数 | 113 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.380Z |

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
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.Method;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.aop.ClassFilter;
   25 | import org.springframework.aop.support.StaticMethodMatcherPointcut;
   26 | import org.springframework.dao.support.PersistenceExceptionTranslator;
   27 | import org.springframework.transaction.TransactionManager;
   28 | import org.springframework.util.ObjectUtils;
   29 | 
   30 | /**
   31 |  * Internal class that implements a {@code Pointcut} that matches if the underlying
   32 |  * {@link TransactionAttributeSource} has an attribute for a given method.
   33 |  *
   34 |  * @author Juergen Hoeller
   35 |  * @author Sam Brannen
   36 |  * @since 2.5.5
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | final class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {
   40 | 
   41 | 	private @Nullable TransactionAttributeSource transactionAttributeSource;
   42 | 
   43 | 
   44 | 	public TransactionAttributeSourcePointcut() {
   45 | 		setClassFilter(new TransactionAttributeSourceClassFilter());
   46 | 	}
   47 | 
   48 | 
   49 | 	public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
   50 | 		this.transactionAttributeSource = transactionAttributeSource;
   51 | 	}
   52 | 
   53 | 	@Override
   54 | 	public boolean matches(Method method, Class<?> targetClass) {
   55 | 		return (this.transactionAttributeSource == null ||
   56 | 				this.transactionAttributeSource.hasTransactionAttribute(method, targetClass));
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public boolean equals(@Nullable Object other) {
   61 | 		return (this == other || (other instanceof TransactionAttributeSourcePointcut that &&
   62 | 				ObjectUtils.nullSafeEquals(this.transactionAttributeSource, that.transactionAttributeSource)));
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public int hashCode() {
   67 | 		return TransactionAttributeSourcePointcut.class.hashCode();
   68 | 	}
   69 | 
   70 | 	@Override
   71 | 	public String toString() {
   72 | 		return getClass().getName() + ": " + this.transactionAttributeSource;
   73 | 	}
   74 | 
   75 | 
   76 | 	/**
   77 | 	 * {@link ClassFilter} that delegates to {@link TransactionAttributeSource#isCandidateClass}
   78 | 	 * for filtering classes whose methods are not worth searching to begin with.
   79 | 	 */
   80 | 	private final class TransactionAttributeSourceClassFilter implements ClassFilter {
   81 | 
   82 | 		@Override
   83 | 		public boolean matches(Class<?> clazz) {
   84 | 			if (TransactionalProxy.class.isAssignableFrom(clazz) ||
   85 | 					TransactionManager.class.isAssignableFrom(clazz) ||
   86 | 					PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
   87 | 				return false;
   88 | 			}
   89 | 			return (transactionAttributeSource == null || transactionAttributeSource.isCandidateClass(clazz));
   90 | 		}
   91 | 
   92 | 		private @Nullable TransactionAttributeSource getTransactionAttributeSource() {
   93 | 			return transactionAttributeSource;
   94 | 		}
   95 | 
   96 | 		@Override
   97 | 		public boolean equals(@Nullable Object other) {
   98 | 			return (this == other || (other instanceof TransactionAttributeSourceClassFilter that &&
   99 | 					ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), that.getTransactionAttributeSource())));
  100 | 		}
  101 | 
  102 | 		@Override
  103 | 		public int hashCode() {
  104 | 			return TransactionAttributeSourceClassFilter.class.hashCode();
  105 | 		}
  106 | 
  107 | 		@Override
  108 | 		public String toString() {
  109 | 			return TransactionAttributeSourceClassFilter.class.getName() + ": " + getTransactionAttributeSource();
  110 | 		}
  111 | 	}
  112 | 
  113 | }
```
