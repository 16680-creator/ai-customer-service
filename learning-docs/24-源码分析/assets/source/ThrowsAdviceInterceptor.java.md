<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/adapter/ThrowsAdviceInterceptor.java -->
<!-- 正文引用：[[源码:ThrowsAdviceInterceptor.java#L起-L止]] ；代码块头部：(src: ThrowsAdviceInterceptor.java#L起-L止) -->

# ThrowsAdviceInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/adapter/ThrowsAdviceInterceptor.java` |
| 行数 | 184 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:58.629Z |

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
   19 | import java.lang.reflect.InvocationTargetException;
   20 | import java.lang.reflect.Method;
   21 | import java.util.HashMap;
   22 | import java.util.Map;
   23 | 
   24 | import org.aopalliance.intercept.MethodInterceptor;
   25 | import org.aopalliance.intercept.MethodInvocation;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.aop.AfterAdvice;
   31 | import org.springframework.aop.framework.AopConfigException;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * Interceptor to wrap an after-throwing advice.
   36 |  *
   37 |  * <p>The signatures on handler methods on the {@code ThrowsAdvice}
   38 |  * implementation method argument must be of the form:<br>
   39 |  *
   40 |  * {@code void afterThrowing([Method, args, target], ThrowableSubclass);}
   41 |  *
   42 |  * <p>Only the last argument is required.
   43 |  *
   44 |  * <p>Some examples of valid methods would be:
   45 |  *
   46 |  * <pre class="code">public void afterThrowing(Exception ex)</pre>
   47 |  * <pre class="code">public void afterThrowing(RemoteException)</pre>
   48 |  * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
   49 |  * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
   50 |  *
   51 |  * <p>This is a framework class that need not be used directly by Spring users.
   52 |  *
   53 |  * @author Rod Johnson
   54 |  * @author Juergen Hoeller
   55 |  * @see MethodBeforeAdviceInterceptor
   56 |  * @see AfterReturningAdviceInterceptor
   57 |  */
   58 | public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {
   59 | 
   60 | 	private static final String AFTER_THROWING = "afterThrowing";
   61 | 
   62 | 	private static final Log logger = LogFactory.getLog(ThrowsAdviceInterceptor.class);
   63 | 
   64 | 
   65 | 	private final Object throwsAdvice;
   66 | 
   67 | 	/** Methods on throws advice, keyed by exception class. */
   68 | 	private final Map<Class<?>, Method> exceptionHandlerMap = new HashMap<>();
   69 | 
   70 | 
   71 | 	/**
   72 | 	 * Create a new ThrowsAdviceInterceptor for the given ThrowsAdvice.
   73 | 	 * @param throwsAdvice the advice object that defines the exception handler methods
   74 | 	 * (usually a {@link org.springframework.aop.ThrowsAdvice} implementation)
   75 | 	 */
   76 | 	public ThrowsAdviceInterceptor(Object throwsAdvice) {
   77 | 		Assert.notNull(throwsAdvice, "Advice must not be null");
   78 | 		this.throwsAdvice = throwsAdvice;
   79 | 
   80 | 		Method[] methods = throwsAdvice.getClass().getMethods();
   81 | 		for (Method method : methods) {
   82 | 			if (method.getName().equals(AFTER_THROWING)) {
   83 | 				Class<?> throwableParam = null;
   84 | 				if (method.getParameterCount() == 1) {
   85 | 					// just a Throwable parameter
   86 | 					throwableParam = method.getParameterTypes()[0];
   87 | 					if (!Throwable.class.isAssignableFrom(throwableParam)) {
   88 | 						throw new AopConfigException("Invalid afterThrowing signature: " +
   89 | 								"single argument must be a Throwable subclass");
   90 | 					}
   91 | 				}
   92 | 				else if (method.getParameterCount() == 4) {
   93 | 					// Method, Object[], target, throwable
   94 | 					Class<?>[] paramTypes = method.getParameterTypes();
   95 | 					if (!Method.class.equals(paramTypes[0]) || !Object[].class.equals(paramTypes[1]) ||
   96 | 							Throwable.class.equals(paramTypes[2]) || !Throwable.class.isAssignableFrom(paramTypes[3])) {
   97 | 						throw new AopConfigException("Invalid afterThrowing signature: " +
   98 | 								"four arguments must be Method, Object[], target, throwable: " + method);
   99 | 					}
  100 | 					throwableParam = paramTypes[3];
  101 | 				}
  102 | 				if (throwableParam == null) {
  103 | 					throw new AopConfigException("Unsupported afterThrowing signature: single throwable argument " +
  104 | 							"or four arguments Method, Object[], target, throwable expected: " + method);
  105 | 				}
  106 | 				// An exception handler to register...
  107 | 				Method existingMethod = this.exceptionHandlerMap.put(throwableParam, method);
  108 | 				if (existingMethod != null) {
  109 | 					throw new AopConfigException("Only one afterThrowing method per specific Throwable subclass " +
  110 | 							"allowed: " + method + " / " + existingMethod);
  111 | 				}
  112 | 				if (logger.isDebugEnabled()) {
  113 | 					logger.debug("Found exception handler method on throws advice: " + method);
  114 | 				}
  115 | 			}
  116 | 		}
  117 | 
  118 | 		if (this.exceptionHandlerMap.isEmpty()) {
  119 | 			throw new AopConfigException(
  120 | 					"At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
  121 | 		}
  122 | 	}
  123 | 
  124 | 
  125 | 	/**
  126 | 	 * Return the number of handler methods in this advice.
  127 | 	 */
  128 | 	public int getHandlerMethodCount() {
  129 | 		return this.exceptionHandlerMap.size();
  130 | 	}
  131 | 
  132 | 
  133 | 	@Override
  134 | 	public @Nullable Object invoke(MethodInvocation mi) throws Throwable {
  135 | 		try {
  136 | 			return mi.proceed();
  137 | 		}
  138 | 		catch (Throwable ex) {
  139 | 			Method handlerMethod = getExceptionHandler(ex);
  140 | 			if (handlerMethod != null) {
  141 | 				invokeHandlerMethod(mi, ex, handlerMethod);
  142 | 			}
  143 | 			throw ex;
  144 | 		}
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Determine the exception handle method for the given exception.
  149 | 	 * @param exception the exception thrown
  150 | 	 * @return a handler for the given exception type, or {@code null} if none found
  151 | 	 */
  152 | 	private @Nullable Method getExceptionHandler(Throwable exception) {
  153 | 		Class<?> exceptionClass = exception.getClass();
  154 | 		if (logger.isTraceEnabled()) {
  155 | 			logger.trace("Trying to find handler for exception of type [" + exceptionClass.getName() + "]");
  156 | 		}
  157 | 		Method handler = this.exceptionHandlerMap.get(exceptionClass);
  158 | 		while (handler == null && exceptionClass != Throwable.class) {
  159 | 			exceptionClass = exceptionClass.getSuperclass();
  160 | 			handler = this.exceptionHandlerMap.get(exceptionClass);
  161 | 		}
  162 | 		if (handler != null && logger.isTraceEnabled()) {
  163 | 			logger.trace("Found handler for exception of type [" + exceptionClass.getName() + "]: " + handler);
  164 | 		}
  165 | 		return handler;
  166 | 	}
  167 | 
  168 | 	private void invokeHandlerMethod(MethodInvocation mi, Throwable ex, Method method) throws Throwable {
  169 | 		Object[] handlerArgs;
  170 | 		if (method.getParameterCount() == 1) {
  171 | 			handlerArgs = new Object[] {ex};
  172 | 		}
  173 | 		else {
  174 | 			handlerArgs = new Object[] {mi.getMethod(), mi.getArguments(), mi.getThis(), ex};
  175 | 		}
  176 | 		try {
  177 | 			method.invoke(this.throwsAdvice, handlerArgs);
  178 | 		}
  179 | 		catch (InvocationTargetException targetEx) {
  180 | 			throw targetEx.getTargetException();
  181 | 		}
  182 | 	}
  183 | 
  184 | }
```
