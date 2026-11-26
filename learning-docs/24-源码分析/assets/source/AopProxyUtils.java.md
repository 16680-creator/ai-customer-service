<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/AopProxyUtils.java -->
<!-- 正文引用：[[源码:AopProxyUtils.java#L起-L止]] ；代码块头部：(src: AopProxyUtils.java#L起-L止) -->

# AopProxyUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/AopProxyUtils.java` |
| 行数 | 320 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.496Z |

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
   19 | import java.io.Closeable;
   20 | import java.lang.reflect.Array;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Proxy;
   23 | import java.util.ArrayList;
   24 | import java.util.Arrays;
   25 | import java.util.List;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.aop.SpringProxy;
   30 | import org.springframework.aop.TargetClassAware;
   31 | import org.springframework.aop.TargetSource;
   32 | import org.springframework.aop.support.AopUtils;
   33 | import org.springframework.aop.target.SingletonTargetSource;
   34 | import org.springframework.beans.factory.Aware;
   35 | import org.springframework.beans.factory.DisposableBean;
   36 | import org.springframework.beans.factory.InitializingBean;
   37 | import org.springframework.core.DecoratingProxy;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.ClassUtils;
   40 | import org.springframework.util.ObjectUtils;
   41 | 
   42 | /**
   43 |  * Utility methods for AOP proxy factories.
   44 |  *
   45 |  * <p>Mainly for internal use within the AOP framework.
   46 |  *
   47 |  * <p>See {@link org.springframework.aop.support.AopUtils} for a collection of
   48 |  * generic AOP utility methods which do not depend on AOP framework internals.
   49 |  *
   50 |  * @author Rod Johnson
   51 |  * @author Juergen Hoeller
   52 |  * @author Sam Brannen
   53 |  * @see org.springframework.aop.support.AopUtils
   54 |  */
   55 | public abstract class AopProxyUtils {
   56 | 
   57 | 	/**
   58 | 	 * Obtain the singleton target object behind the given proxy, if any.
   59 | 	 * @param candidate the (potential) proxy to check
   60 | 	 * @return the singleton target object managed in a {@link SingletonTargetSource},
   61 | 	 * or {@code null} in any other case (not a proxy, not an existing singleton target)
   62 | 	 * @since 4.3.8
   63 | 	 * @see Advised#getTargetSource()
   64 | 	 * @see SingletonTargetSource#getTarget()
   65 | 	 */
   66 | 	public static @Nullable Object getSingletonTarget(Object candidate) {
   67 | 		if (candidate instanceof Advised advised) {
   68 | 			TargetSource targetSource = advised.getTargetSource();
   69 | 			if (targetSource instanceof SingletonTargetSource singleTargetSource) {
   70 | 				return singleTargetSource.getTarget();
   71 | 			}
   72 | 		}
   73 | 		return null;
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Obtain the ultimate singleton target object behind the given proxy,
   78 | 	 * even for a nested proxy scenario where the immediate singleton target
   79 | 	 * is yet another proxy.
   80 | 	 * @param candidate the (potential) proxy to check
   81 | 	 * @return the singleton target object managed in a {@link SingletonTargetSource},
   82 | 	 * or the original candidate if not a proxy or not an existing singleton target
   83 | 	 * @since 7.0.10
   84 | 	 * @see Advised#getTargetSource()
   85 | 	 * @see SingletonTargetSource#getTarget()
   86 | 	 */
   87 | 	public static Object ultimateSingletonTarget(Object candidate) {
   88 | 		Object current = candidate;
   89 | 		while (current instanceof Advised advised) {
   90 | 			TargetSource targetSource = advised.getTargetSource();
   91 | 			if (!(targetSource instanceof SingletonTargetSource singleTargetSource)) {
   92 | 				break;
   93 | 			}
   94 | 			current = singleTargetSource.getTarget();
   95 | 		}
   96 | 		return current;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Determine the ultimate target class of the given bean instance, traversing
  101 | 	 * not only a top-level proxy but any number of nested proxies as well &mdash;
  102 | 	 * as long as possible without side effects, that is, just for singleton targets.
  103 | 	 * @param candidate the instance to check (might be an AOP proxy)
  104 | 	 * @return the ultimate target class (or the plain class of the given
  105 | 	 * object as fallback; never {@code null})
  106 | 	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
  107 | 	 * @see Advised#getTargetSource()
  108 | 	 */
  109 | 	public static Class<?> ultimateTargetClass(Object candidate) {
  110 | 		Assert.notNull(candidate, "Candidate object must not be null");
  111 | 		Object current = candidate;
  112 | 		Class<?> result = null;
  113 | 		while (current instanceof TargetClassAware targetClassAware) {
  114 | 			result = targetClassAware.getTargetClass();
  115 | 			current = getSingletonTarget(current);
  116 | 		}
  117 | 		if (result == null) {
  118 | 			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
  119 | 		}
  120 | 		return result;
  121 | 	}
  122 | 
  123 | 	/**
  124 | 	 * Complete the set of interfaces that are typically required in a JDK dynamic
  125 | 	 * proxy generated by Spring AOP.
  126 | 	 * <p>Specifically, {@link SpringProxy}, {@link Advised}, and {@link DecoratingProxy}
  127 | 	 * will be appended to the set of user-specified interfaces.
  128 | 	 * <p>This method can be useful when registering
  129 | 	 * {@linkplain org.springframework.aot.hint.ProxyHints proxy hints} for Spring's
  130 | 	 * AOT support, as demonstrated in the following example which uses this method
  131 | 	 * via a {@code static} import.
  132 | 	 * <pre class="code">
  133 | 	 * RuntimeHints hints = ...
  134 | 	 * hints.proxies().registerJdkProxy(completeJdkProxyInterfaces(MyInterface.class));
  135 | 	 * </pre>
  136 | 	 * @param userInterfaces the set of user-specified interfaces implemented by
  137 | 	 * the component to be proxied
  138 | 	 * @return the complete set of interfaces that the proxy should implement
  139 | 	 * @throws IllegalArgumentException if a supplied {@code Class} is {@code null},
  140 | 	 * is not an {@linkplain Class#isInterface() interface}, or is a
  141 | 	 * {@linkplain Class#isSealed() sealed} interface
  142 | 	 * @since 6.0
  143 | 	 * @see SpringProxy
  144 | 	 * @see Advised
  145 | 	 * @see DecoratingProxy
  146 | 	 * @see org.springframework.aot.hint.RuntimeHints#proxies()
  147 | 	 * @see org.springframework.aot.hint.ProxyHints#registerJdkProxy(Class...)
  148 | 	 */
  149 | 	public static Class<?>[] completeJdkProxyInterfaces(Class<?>... userInterfaces) {
  150 | 		List<Class<?>> completedInterfaces = new ArrayList<>(userInterfaces.length + 3);
  151 | 		for (Class<?> ifc : userInterfaces) {
  152 | 			Assert.notNull(ifc, "'userInterfaces' must not contain null values");
  153 | 			Assert.isTrue(ifc.isInterface() && !ifc.isSealed(),
  154 | 					() -> ifc.getName() + " must be a non-sealed interface");
  155 | 			completedInterfaces.add(ifc);
  156 | 		}
  157 | 		completedInterfaces.add(SpringProxy.class);
  158 | 		completedInterfaces.add(Advised.class);
  159 | 		completedInterfaces.add(DecoratingProxy.class);
  160 | 		return completedInterfaces.toArray(Class<?>[]::new);
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
  165 | 	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
  166 | 	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
  167 | 	 * {@link org.springframework.aop.SpringProxy} marker interface.
  168 | 	 * @param advised the proxy config
  169 | 	 * @return the complete set of interfaces to proxy
  170 | 	 * @see SpringProxy
  171 | 	 * @see Advised
  172 | 	 */
  173 | 	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
  174 | 		return completeProxiedInterfaces(advised, false);
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
  179 | 	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
  180 | 	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
  181 | 	 * {@link org.springframework.aop.SpringProxy} marker interface.
  182 | 	 * @param advised the proxy config
  183 | 	 * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface
  184 | 	 * @return the complete set of interfaces to proxy
  185 | 	 * @since 4.3
  186 | 	 * @see SpringProxy
  187 | 	 * @see Advised
  188 | 	 * @see DecoratingProxy
  189 | 	 */
  190 | 	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
  191 | 		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
  192 | 		if (specifiedInterfaces.length == 0) {
  193 | 			// No user-specified interfaces: check whether target class is an interface.
  194 | 			Class<?> targetClass = advised.getTargetClass();
  195 | 			if (targetClass != null) {
  196 | 				if (targetClass.isInterface()) {
  197 | 					advised.setInterfaces(targetClass);
  198 | 				}
  199 | 				else if (Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
  200 | 					advised.setInterfaces(targetClass.getInterfaces());
  201 | 				}
  202 | 				specifiedInterfaces = advised.getProxiedInterfaces();
  203 | 			}
  204 | 		}
  205 | 		List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
  206 | 		for (Class<?> ifc : specifiedInterfaces) {
  207 | 			// Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
  208 | 			if (!ifc.isSealed()) {
  209 | 				proxiedInterfaces.add(ifc);
  210 | 			}
  211 | 		}
  212 | 		if (!advised.isInterfaceProxied(SpringProxy.class)) {
  213 | 			proxiedInterfaces.add(SpringProxy.class);
  214 | 		}
  215 | 		if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
  216 | 			proxiedInterfaces.add(Advised.class);
  217 | 		}
  218 | 		if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
  219 | 			proxiedInterfaces.add(DecoratingProxy.class);
  220 | 		}
  221 | 		return ClassUtils.toClassArray(proxiedInterfaces);
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Extract the user-specified interfaces that the given proxy implements,
  226 | 	 * i.e. all non-Advised interfaces that the proxy implements.
  227 | 	 * @param proxy the proxy to analyze (usually a JDK dynamic proxy)
  228 | 	 * @return all user-specified interfaces that the proxy implements,
  229 | 	 * in the original order (never {@code null} or empty)
  230 | 	 * @see Advised
  231 | 	 */
  232 | 	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
  233 | 		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
  234 | 		int nonUserIfcCount = 0;
  235 | 		if (proxy instanceof SpringProxy) {
  236 | 			nonUserIfcCount++;
  237 | 		}
  238 | 		if (proxy instanceof Advised) {
  239 | 			nonUserIfcCount++;
  240 | 		}
  241 | 		if (proxy instanceof DecoratingProxy) {
  242 | 			nonUserIfcCount++;
  243 | 		}
  244 | 		Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
  245 | 		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
  246 | 		return userInterfaces;
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Check equality of the proxies behind the given AdvisedSupport objects.
  251 | 	 * Not the same as equality of the AdvisedSupport objects:
  252 | 	 * rather, equality of interfaces, advisors and target sources.
  253 | 	 */
  254 | 	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
  255 | 		return (a == b ||
  256 | 				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
  261 | 	 */
  262 | 	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
  263 | 		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
  264 | 	}
  265 | 
  266 | 	/**
  267 | 	 * Check equality of the advisors behind the given AdvisedSupport objects.
  268 | 	 */
  269 | 	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
  270 | 		return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
  271 | 	}
  272 | 
  273 | 
  274 | 	/**
  275 | 	 * Adapt the given arguments to the target signature in the given method,
  276 | 	 * if necessary: in particular, if a given vararg argument array does not
  277 | 	 * match the array type of the declared vararg parameter in the method.
  278 | 	 * @param method the target method
  279 | 	 * @param arguments the given arguments
  280 | 	 * @return a cloned argument array, or the original if no adaptation is needed
  281 | 	 * @since 4.2.3
  282 | 	 */
  283 | 	static @Nullable Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
  284 | 		if (ObjectUtils.isEmpty(arguments)) {
  285 | 			return new Object[0];
  286 | 		}
  287 | 		if (method.isVarArgs() && (method.getParameterCount() == arguments.length)) {
  288 | 			Class<?>[] paramTypes = method.getParameterTypes();
  289 | 			int varargIndex = paramTypes.length - 1;
  290 | 			Class<?> varargType = paramTypes[varargIndex];
  291 | 			if (varargType.isArray()) {
  292 | 				Object varargArray = arguments[varargIndex];
  293 | 				if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
  294 | 					Object[] newArguments = new Object[arguments.length];
  295 | 					System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
  296 | 					Class<?> targetElementType = varargType.componentType();
  297 | 					int varargLength = Array.getLength(varargArray);
  298 | 					Object newVarargArray = Array.newInstance(targetElementType, varargLength);
  299 | 					System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
  300 | 					newArguments[varargIndex] = newVarargArray;
  301 | 					return newArguments;
  302 | 				}
  303 | 			}
  304 | 		}
  305 | 		return arguments;
  306 | 	}
  307 | 
  308 | 	/**
  309 | 	 * Determine whether the given interface is a Spring configuration callback
  310 | 	 * interface (i.e. {@link InitializingBean}, {@link DisposableBean},
  311 | 	 * {@link Closeable}/{@link AutoCloseable}, or an {@link Aware} sub-interface).
  312 | 	 * @since 7.1
  313 | 	 */
  314 | 	static boolean isConfigurationCallbackInterface(Class<?> ifc) {
  315 | 		return (InitializingBean.class == ifc || DisposableBean.class == ifc ||
  316 | 				Closeable.class == ifc || AutoCloseable.class == ifc ||
  317 | 				ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
  318 | 	}
  319 | 
  320 | }
```
