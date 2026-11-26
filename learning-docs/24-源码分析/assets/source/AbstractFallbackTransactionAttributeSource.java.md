<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java -->
<!-- 正文引用：[[源码:AbstractFallbackTransactionAttributeSource.java#L起-L止]] ；代码块头部：(src: AbstractFallbackTransactionAttributeSource.java#L起-L止) -->

# AbstractFallbackTransactionAttributeSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java` |
| 行数 | 228 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.250Z |

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
   20 | import java.lang.reflect.Modifier;
   21 | import java.util.Map;
   22 | import java.util.concurrent.ConcurrentHashMap;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.aop.support.AopUtils;
   29 | import org.springframework.beans.factory.BeanFactoryAware;
   30 | import org.springframework.context.EmbeddedValueResolverAware;
   31 | import org.springframework.core.MethodClassKey;
   32 | import org.springframework.util.ClassUtils;
   33 | import org.springframework.util.ReflectionUtils;
   34 | import org.springframework.util.StringValueResolver;
   35 | 
   36 | /**
   37 |  * Abstract implementation of {@link TransactionAttributeSource} that caches
   38 |  * attributes for methods and implements a fallback policy: 1. specific target
   39 |  * method; 2. target class; 3. declaring method; 4. declaring class/interface.
   40 |  *
   41 |  * <p>Defaults to using the target class's transaction attribute if none is
   42 |  * associated with the target method. Any transaction attribute associated with
   43 |  * the target method completely overrides a class transaction attribute.
   44 |  * If none found on the target class, the interface that the invoked method
   45 |  * has been called through (in case of a JDK proxy) will be checked.
   46 |  *
   47 |  * @author Rod Johnson
   48 |  * @author Juergen Hoeller
   49 |  * @since 1.1
   50 |  */
   51 | public abstract class AbstractFallbackTransactionAttributeSource
   52 | 		implements TransactionAttributeSource, EmbeddedValueResolverAware {
   53 | 
   54 | 	/**
   55 | 	 * Canonical value held in cache to indicate no transaction attribute was
   56 | 	 * found for this method, and we don't need to look again.
   57 | 	 */
   58 | 	@SuppressWarnings("serial")
   59 | 	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
   60 | 		@Override
   61 | 		public String toString() {
   62 | 			return "null";
   63 | 		}
   64 | 	};
   65 | 
   66 | 
   67 | 	/**
   68 | 	 * Logger available to subclasses.
   69 | 	 * <p>As this base class is not marked Serializable, the logger will be recreated
   70 | 	 * after serialization - provided that the concrete subclass is Serializable.
   71 | 	 */
   72 | 	protected final Log logger = LogFactory.getLog(getClass());
   73 | 
   74 | 	private transient @Nullable StringValueResolver embeddedValueResolver;
   75 | 
   76 | 	/**
   77 | 	 * Cache of TransactionAttributes, keyed by method on a specific target class.
   78 | 	 * <p>As this base class is not marked Serializable, the cache will be recreated
   79 | 	 * after serialization - provided that the concrete subclass is Serializable.
   80 | 	 */
   81 | 	private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);
   82 | 
   83 | 
   84 | 	@Override
   85 | 	public void setEmbeddedValueResolver(StringValueResolver resolver) {
   86 | 		this.embeddedValueResolver = resolver;
   87 | 	}
   88 | 
   89 | 
   90 | 	@Override
   91 | 	public boolean hasTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
   92 | 		return (getTransactionAttribute(method, targetClass, false) != null);
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public @Nullable TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
   97 | 		return getTransactionAttribute(method, targetClass, true);
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Determine the transaction attribute for this method invocation.
  102 | 	 * <p>Defaults to the class's transaction attribute if no method attribute is found.
  103 | 	 * @param method the method for the current invocation (never {@code null})
  104 | 	 * @param targetClass the target class for this invocation (can be {@code null})
  105 | 	 * @param cacheNull whether {@code null} results should be cached as well
  106 | 	 * @return a TransactionAttribute for this method, or {@code null} if the method
  107 | 	 * is not transactional
  108 | 	 */
  109 | 	private @Nullable TransactionAttribute getTransactionAttribute(
  110 | 			Method method, @Nullable Class<?> targetClass, boolean cacheNull) {
  111 | 
  112 | 		if (ReflectionUtils.isObjectMethod(method)) {
  113 | 			return null;
  114 | 		}
  115 | 
  116 | 		Object cacheKey = getCacheKey(method, targetClass);
  117 | 		TransactionAttribute cached = this.attributeCache.get(cacheKey);
  118 | 
  119 | 		if (cached != null) {
  120 | 			return (cached != NULL_TRANSACTION_ATTRIBUTE ? cached : null);
  121 | 		}
  122 | 		else {
  123 | 			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
  124 | 			if (txAttr != null) {
  125 | 				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
  126 | 				if (txAttr instanceof DefaultTransactionAttribute dta) {
  127 | 					dta.setDescriptor(methodIdentification);
  128 | 					dta.resolveAttributeStrings(this.embeddedValueResolver);
  129 | 				}
  130 | 				if (logger.isTraceEnabled()) {
  131 | 					logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
  132 | 				}
  133 | 				this.attributeCache.put(cacheKey, txAttr);
  134 | 			}
  135 | 			else if (cacheNull) {
  136 | 				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
  137 | 			}
  138 | 			return txAttr;
  139 | 		}
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Determine a cache key for the given method and target class.
  144 | 	 * <p>Must not produce same key for overloaded methods.
  145 | 	 * Must produce same key for different instances of the same method.
  146 | 	 * @param method the method (never {@code null})
  147 | 	 * @param targetClass the target class (may be {@code null})
  148 | 	 * @return the cache key (never {@code null})
  149 | 	 */
  150 | 	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
  151 | 		return new MethodClassKey(method, targetClass);
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Same signature as {@link #getTransactionAttribute}, but doesn't cache the result.
  156 | 	 * {@link #getTransactionAttribute} is effectively a caching decorator for this method.
  157 | 	 * <p>As of 4.1.8, this method can be overridden.
  158 | 	 * @since 4.1.8
  159 | 	 * @see #getTransactionAttribute
  160 | 	 */
  161 | 	protected @Nullable TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
  162 | 		// Don't allow non-public methods, as configured.
  163 | 		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
  164 | 			return null;
  165 | 		}
  166 | 		// Skip setBeanFactory method on BeanFactoryAware.
  167 | 		if (method.getDeclaringClass() == BeanFactoryAware.class) {
  168 | 			return null;
  169 | 		}
  170 | 
  171 | 		// The method may be on an interface, but we need attributes from the target class.
  172 | 		// If the target class is null, the method will be unchanged.
  173 | 		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
  174 | 
  175 | 		// First try is the method in the target class.
  176 | 		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
  177 | 		if (txAttr != null) {
  178 | 			return txAttr;
  179 | 		}
  180 | 
  181 | 		// Second try is the transaction attribute on the target class.
  182 | 		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
  183 | 		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
  184 | 			return txAttr;
  185 | 		}
  186 | 
  187 | 		if (specificMethod != method) {
  188 | 			// Fallback is to look at the original method.
  189 | 			txAttr = findTransactionAttribute(method);
  190 | 			if (txAttr != null) {
  191 | 				return txAttr;
  192 | 			}
  193 | 			// Last fallback is the class of the original method.
  194 | 			txAttr = findTransactionAttribute(method.getDeclaringClass());
  195 | 			if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
  196 | 				return txAttr;
  197 | 			}
  198 | 		}
  199 | 
  200 | 		return null;
  201 | 	}
  202 | 
  203 | 
  204 | 	/**
  205 | 	 * Subclasses need to implement this to return the transaction attribute for the
  206 | 	 * given class, if any.
  207 | 	 * @param clazz the class to retrieve the attribute for
  208 | 	 * @return all transaction attribute associated with this class, or {@code null} if none
  209 | 	 */
  210 | 	protected abstract @Nullable TransactionAttribute findTransactionAttribute(Class<?> clazz);
  211 | 
  212 | 	/**
  213 | 	 * Subclasses need to implement this to return the transaction attribute for the
  214 | 	 * given method, if any.
  215 | 	 * @param method the method to retrieve the attribute for
  216 | 	 * @return all transaction attribute associated with this method, or {@code null} if none
  217 | 	 */
  218 | 	protected abstract @Nullable TransactionAttribute findTransactionAttribute(Method method);
  219 | 
  220 | 	/**
  221 | 	 * Should only public methods be allowed to have transactional semantics?
  222 | 	 * <p>The default implementation returns {@code false}.
  223 | 	 */
  224 | 	protected boolean allowPublicMethodsOnly() {
  225 | 		return false;
  226 | 	}
  227 | 
  228 | }
```
