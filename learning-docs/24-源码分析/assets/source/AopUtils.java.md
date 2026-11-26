<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/support/AopUtils.java -->
<!-- 正文引用：[[源码:AopUtils.java#L起-L止]] ；代码块头部：(src: AopUtils.java#L起-L止) -->

# AopUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/support/AopUtils.java` |
| 行数 | 389 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:01.715Z |

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
   17 | package org.springframework.aop.support;
   18 | 
   19 | import java.lang.reflect.InaccessibleObjectException;
   20 | import java.lang.reflect.InvocationTargetException;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Modifier;
   23 | import java.lang.reflect.Proxy;
   24 | import java.util.ArrayList;
   25 | import java.util.LinkedHashSet;
   26 | import java.util.List;
   27 | import java.util.Set;
   28 | 
   29 | import kotlin.coroutines.Continuation;
   30 | import kotlin.coroutines.CoroutineContext;
   31 | import kotlinx.coroutines.Job;
   32 | import org.jspecify.annotations.Nullable;
   33 | 
   34 | import org.springframework.aop.Advisor;
   35 | import org.springframework.aop.AopInvocationException;
   36 | import org.springframework.aop.IntroductionAdvisor;
   37 | import org.springframework.aop.IntroductionAwareMethodMatcher;
   38 | import org.springframework.aop.MethodMatcher;
   39 | import org.springframework.aop.Pointcut;
   40 | import org.springframework.aop.PointcutAdvisor;
   41 | import org.springframework.aop.SpringProxy;
   42 | import org.springframework.aop.TargetClassAware;
   43 | import org.springframework.core.BridgeMethodResolver;
   44 | import org.springframework.core.CoroutinesUtils;
   45 | import org.springframework.core.KotlinDetector;
   46 | import org.springframework.core.MethodIntrospector;
   47 | import org.springframework.lang.Contract;
   48 | import org.springframework.util.Assert;
   49 | import org.springframework.util.ClassUtils;
   50 | import org.springframework.util.ReflectionUtils;
   51 | 
   52 | /**
   53 |  * Utility methods for AOP support code.
   54 |  *
   55 |  * <p>Mainly for internal use within Spring's AOP support.
   56 |  *
   57 |  * <p>See {@link org.springframework.aop.framework.AopProxyUtils} for a
   58 |  * collection of framework-specific AOP utility methods which depend
   59 |  * on internals of Spring's AOP framework implementation.
   60 |  *
   61 |  * @author Rod Johnson
   62 |  * @author Juergen Hoeller
   63 |  * @author Rob Harrop
   64 |  * @author Sebastien Deleuze
   65 |  * @see org.springframework.aop.framework.AopProxyUtils
   66 |  */
   67 | public abstract class AopUtils {
   68 | 
   69 | 	private static final boolean COROUTINES_REACTOR_PRESENT = ClassUtils.isPresent(
   70 | 			"kotlinx.coroutines.reactor.MonoKt", AopUtils.class.getClassLoader());
   71 | 
   72 | 
   73 | 	/**
   74 | 	 * Check whether the given object is a JDK dynamic proxy or a CGLIB proxy.
   75 | 	 * <p>This method additionally checks if the given object is an instance
   76 | 	 * of {@link SpringProxy}.
   77 | 	 * @param object the object to check
   78 | 	 * @see #isJdkDynamicProxy
   79 | 	 * @see #isCglibProxy
   80 | 	 */
   81 | 	@Contract("null -> false")
   82 | 	public static boolean isAopProxy(@Nullable Object object) {
   83 | 		return (object instanceof SpringProxy && (Proxy.isProxyClass(object.getClass()) ||
   84 | 				object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)));
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Check whether the given object is a JDK dynamic proxy.
   89 | 	 * <p>This method goes beyond the implementation of
   90 | 	 * {@link Proxy#isProxyClass(Class)} by additionally checking if the
   91 | 	 * given object is an instance of {@link SpringProxy}.
   92 | 	 * @param object the object to check
   93 | 	 * @see java.lang.reflect.Proxy#isProxyClass
   94 | 	 */
   95 | 	@Contract("null -> false")
   96 | 	public static boolean isJdkDynamicProxy(@Nullable Object object) {
   97 | 		return (object instanceof SpringProxy && Proxy.isProxyClass(object.getClass()));
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Check whether the given object is a CGLIB proxy.
  102 | 	 * <p>This method goes beyond the implementation of
  103 | 	 * {@link ClassUtils#isCglibProxy(Object)} by additionally checking if
  104 | 	 * the given object is an instance of {@link SpringProxy}.
  105 | 	 * @param object the object to check
  106 | 	 * @see ClassUtils#isCglibProxy(Object)
  107 | 	 */
  108 | 	@Contract("null -> false")
  109 | 	public static boolean isCglibProxy(@Nullable Object object) {
  110 | 		return (object instanceof SpringProxy &&
  111 | 				object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR));
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Determine the target class of the given bean instance which might be an AOP proxy.
  116 | 	 * <p>Returns the target class for an AOP proxy or the plain class otherwise.
  117 | 	 * @param candidate the instance to check (might be an AOP proxy)
  118 | 	 * @return the target class (or the plain class of the given object as fallback;
  119 | 	 * never {@code null})
  120 | 	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
  121 | 	 * @see org.springframework.aop.framework.AopProxyUtils#ultimateTargetClass(Object)
  122 | 	 */
  123 | 	public static Class<?> getTargetClass(Object candidate) {
  124 | 		Assert.notNull(candidate, "Candidate object must not be null");
  125 | 		Class<?> result = null;
  126 | 		if (candidate instanceof TargetClassAware targetClassAware) {
  127 | 			result = targetClassAware.getTargetClass();
  128 | 		}
  129 | 		if (result == null) {
  130 | 			result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
  131 | 		}
  132 | 		return result;
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * Select an invocable method on the target type: either the given method itself
  137 | 	 * if actually exposed on the target type, or otherwise a corresponding method
  138 | 	 * on one of the target type's interfaces or on the target type itself.
  139 | 	 * @param method the method to check
  140 | 	 * @param targetType the target type to search methods on (typically an AOP proxy)
  141 | 	 * @return a corresponding invocable method on the target type
  142 | 	 * @throws IllegalStateException if the given method is not invocable on the given
  143 | 	 * target type (typically due to a proxy mismatch)
  144 | 	 * @since 4.3
  145 | 	 * @see MethodIntrospector#selectInvocableMethod(Method, Class)
  146 | 	 */
  147 | 	public static Method selectInvocableMethod(Method method, @Nullable Class<?> targetType) {
  148 | 		if (targetType == null) {
  149 | 			return method;
  150 | 		}
  151 | 		Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
  152 | 		if (Modifier.isPrivate(methodToUse.getModifiers()) && !Modifier.isStatic(methodToUse.getModifiers()) &&
  153 | 				SpringProxy.class.isAssignableFrom(targetType)) {
  154 | 			throw new IllegalStateException(String.format(
  155 | 					"Need to invoke method '%s' found on proxy for target class '%s' but cannot " +
  156 | 					"be delegated to target bean. Switch its visibility to package or protected.",
  157 | 					method.getName(), method.getDeclaringClass().getSimpleName()));
  158 | 		}
  159 | 		return methodToUse;
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * Determine whether the given method is an "equals" method.
  164 | 	 * @see java.lang.Object#equals
  165 | 	 */
  166 | 	public static boolean isEqualsMethod(@Nullable Method method) {
  167 | 		return ReflectionUtils.isEqualsMethod(method);
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Determine whether the given method is a "hashCode" method.
  172 | 	 * @see java.lang.Object#hashCode
  173 | 	 */
  174 | 	public static boolean isHashCodeMethod(@Nullable Method method) {
  175 | 		return ReflectionUtils.isHashCodeMethod(method);
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Determine whether the given method is a "toString" method.
  180 | 	 * @see java.lang.Object#toString()
  181 | 	 */
  182 | 	public static boolean isToStringMethod(@Nullable Method method) {
  183 | 		return ReflectionUtils.isToStringMethod(method);
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Determine whether the given method is a "finalize" method.
  188 | 	 * @see java.lang.Object#finalize()
  189 | 	 */
  190 | 	public static boolean isFinalizeMethod(@Nullable Method method) {
  191 | 		return (method != null && method.getName().equals("finalize") &&
  192 | 				method.getParameterCount() == 0);
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Given a method, which may come from an interface, and a target class used
  197 | 	 * in the current AOP invocation, find the corresponding target method if there
  198 | 	 * is one. For example, the method may be {@code IFoo.bar()} and the target class
  199 | 	 * may be {@code DefaultFoo}. In this case, the method may be
  200 | 	 * {@code DefaultFoo.bar()}. This enables attributes on that method to be found.
  201 | 	 * <p><b>NOTE:</b> In contrast to {@link org.springframework.util.ClassUtils#getMostSpecificMethod},
  202 | 	 * this method resolves bridge methods in order to retrieve attributes from
  203 | 	 * the <i>original</i> method definition.
  204 | 	 * @param method the method to be invoked, which may come from an interface
  205 | 	 * @param targetClass the target class for the current invocation
  206 | 	 * (can be {@code null} or may not even implement the method)
  207 | 	 * @return the specific target method, or the original method if the
  208 | 	 * {@code targetClass} does not implement it
  209 | 	 * @see org.springframework.util.ClassUtils#getMostSpecificMethod
  210 | 	 * @see org.springframework.core.BridgeMethodResolver#getMostSpecificMethod
  211 | 	 */
  212 | 	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
  213 | 		Class<?> specificTargetClass = (targetClass != null ? ClassUtils.getUserClass(targetClass) : null);
  214 | 		return BridgeMethodResolver.getMostSpecificMethod(method, specificTargetClass);
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Can the given pointcut apply at all on the given class?
  219 | 	 * <p>This is an important test as it can be used to optimize
  220 | 	 * out a pointcut for a class.
  221 | 	 * @param pc the static or dynamic pointcut to check
  222 | 	 * @param targetClass the class to test
  223 | 	 * @return whether the pointcut can apply on any method
  224 | 	 */
  225 | 	public static boolean canApply(Pointcut pc, Class<?> targetClass) {
  226 | 		return canApply(pc, targetClass, false);
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Can the given pointcut apply at all on the given class?
  231 | 	 * <p>This is an important test as it can be used to optimize
  232 | 	 * out a pointcut for a class.
  233 | 	 * @param pc the static or dynamic pointcut to check
  234 | 	 * @param targetClass the class to test
  235 | 	 * @param hasIntroductions whether the advisor chain
  236 | 	 * for this bean includes any introductions
  237 | 	 * @return whether the pointcut can apply on any method
  238 | 	 */
  239 | 	public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
  240 | 		Assert.notNull(pc, "Pointcut must not be null");
  241 | 		if (!pc.getClassFilter().matches(targetClass)) {
  242 | 			return false;
  243 | 		}
  244 | 
  245 | 		MethodMatcher methodMatcher = pc.getMethodMatcher();
  246 | 		if (methodMatcher == MethodMatcher.TRUE) {
  247 | 			// No need to iterate the methods if we're matching any method anyway...
  248 | 			return true;
  249 | 		}
  250 | 
  251 | 		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
  252 | 		if (methodMatcher instanceof IntroductionAwareMethodMatcher iamm) {
  253 | 			introductionAwareMethodMatcher = iamm;
  254 | 		}
  255 | 
  256 | 		Set<Class<?>> classes = new LinkedHashSet<>();
  257 | 		if (!Proxy.isProxyClass(targetClass)) {
  258 | 			classes.add(ClassUtils.getUserClass(targetClass));
  259 | 		}
  260 | 		classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
  261 | 
  262 | 		for (Class<?> clazz : classes) {
  263 | 			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
  264 | 			for (Method method : methods) {
  265 | 				if (introductionAwareMethodMatcher != null ?
  266 | 						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
  267 | 						methodMatcher.matches(method, targetClass)) {
  268 | 					return true;
  269 | 				}
  270 | 			}
  271 | 		}
  272 | 
  273 | 		return false;
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * Can the given advisor apply at all on the given class?
  278 | 	 * This is an important test as it can be used to optimize
  279 | 	 * out an advisor for a class.
  280 | 	 * @param advisor the advisor to check
  281 | 	 * @param targetClass class we're testing
  282 | 	 * @return whether the pointcut can apply on any method
  283 | 	 */
  284 | 	public static boolean canApply(Advisor advisor, Class<?> targetClass) {
  285 | 		return canApply(advisor, targetClass, false);
  286 | 	}
  287 | 
  288 | 	/**
  289 | 	 * Can the given advisor apply at all on the given class?
  290 | 	 * <p>This is an important test as it can be used to optimize out an advisor for a class.
  291 | 	 * This version also takes into account introductions (for IntroductionAwareMethodMatchers).
  292 | 	 * @param advisor the advisor to check
  293 | 	 * @param targetClass class we're testing
  294 | 	 * @param hasIntroductions whether the advisor chain for this bean includes
  295 | 	 * any introductions
  296 | 	 * @return whether the pointcut can apply on any method
  297 | 	 */
  298 | 	public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
  299 | 		if (advisor instanceof IntroductionAdvisor ia) {
  300 | 			return ia.getClassFilter().matches(targetClass);
  301 | 		}
  302 | 		else if (advisor instanceof PointcutAdvisor pca) {
  303 | 			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
  304 | 		}
  305 | 		else {
  306 | 			// It doesn't have a pointcut so we assume it applies.
  307 | 			return true;
  308 | 		}
  309 | 	}
  310 | 
  311 | 	/**
  312 | 	 * Determine the sublist of the {@code candidateAdvisors} list
  313 | 	 * that is applicable to the given class.
  314 | 	 * @param candidateAdvisors the Advisors to evaluate
  315 | 	 * @param clazz the target class
  316 | 	 * @return sublist of Advisors that can apply to an object of the given class
  317 | 	 * (may be the incoming List as-is)
  318 | 	 */
  319 | 	public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
  320 | 		if (candidateAdvisors.isEmpty()) {
  321 | 			return candidateAdvisors;
  322 | 		}
  323 | 		List<Advisor> eligibleAdvisors = new ArrayList<>();
  324 | 		for (Advisor candidate : candidateAdvisors) {
  325 | 			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
  326 | 				eligibleAdvisors.add(candidate);
  327 | 			}
  328 | 		}
  329 | 		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
  330 | 		for (Advisor candidate : candidateAdvisors) {
  331 | 			if (candidate instanceof IntroductionAdvisor) {
  332 | 				// already processed
  333 | 				continue;
  334 | 			}
  335 | 			if (canApply(candidate, clazz, hasIntroductions)) {
  336 | 				eligibleAdvisors.add(candidate);
  337 | 			}
  338 | 		}
  339 | 		return eligibleAdvisors;
  340 | 	}
  341 | 
  342 | 	/**
  343 | 	 * Invoke the given target via reflection, as part of an AOP method invocation.
  344 | 	 * @param target the target object
  345 | 	 * @param method the method to invoke
  346 | 	 * @param args the arguments for the method
  347 | 	 * @return the invocation result, if any
  348 | 	 * @throws Throwable if thrown by the target method
  349 | 	 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
  350 | 	 */
  351 | 	public static @Nullable Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, @Nullable Object[] args)
  352 | 			throws Throwable {
  353 | 
  354 | 		// Use reflection to invoke the method.
  355 | 		try {
  356 | 			Method originalMethod = BridgeMethodResolver.findBridgedMethod(method);
  357 | 			ReflectionUtils.makeAccessible(originalMethod);
  358 | 			return (COROUTINES_REACTOR_PRESENT && KotlinDetector.isSuspendingFunction(originalMethod) ?
  359 | 					KotlinDelegate.invokeSuspendingFunction(originalMethod, target, args) : originalMethod.invoke(target, args));
  360 | 		}
  361 | 		catch (InvocationTargetException ex) {
  362 | 			// Invoked method threw a checked exception.
  363 | 			// We must rethrow it. The client won't see the interceptor.
  364 | 			throw ex.getTargetException();
  365 | 		}
  366 | 		catch (IllegalArgumentException ex) {
  367 | 			throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
  368 | 					method + "] on target [" + target + "]", ex);
  369 | 		}
  370 | 		catch (IllegalAccessException | InaccessibleObjectException ex) {
  371 | 			throw new AopInvocationException("Could not access method [" + method + "]", ex);
  372 | 		}
  373 | 	}
  374 | 
  375 | 
  376 | 	/**
  377 | 	 * Inner class to avoid a hard dependency on Kotlin at runtime.
  378 | 	 */
  379 | 	private static class KotlinDelegate {
  380 | 
  381 | 		public static Object invokeSuspendingFunction(Method method, @Nullable Object target, @Nullable Object... args) {
  382 | 			Continuation<?> continuation = (Continuation<?>) args[args.length -1];
  383 | 			Assert.state(continuation != null, "No Continuation available");
  384 | 			CoroutineContext context = continuation.getContext().minusKey(Job.Key);
  385 | 			return CoroutinesUtils.invokeSuspendingFunction(context, method, target, args);
  386 | 		}
  387 | 	}
  388 | 
  389 | }
```
