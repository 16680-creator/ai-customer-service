<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/JdkDynamicAopProxy.java -->
<!-- 正文引用：[[源码:JdkDynamicAopProxy.java#L起-L止]] ；代码块头部：(src: JdkDynamicAopProxy.java#L起-L止) -->

# JdkDynamicAopProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/JdkDynamicAopProxy.java` |
| 行数 | 355 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:57.306Z |

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
   19 | import java.io.IOException;
   20 | import java.io.ObjectInputStream;
   21 | import java.io.Serializable;
   22 | import java.lang.reflect.InvocationHandler;
   23 | import java.lang.reflect.Method;
   24 | import java.lang.reflect.Proxy;
   25 | import java.util.List;
   26 | 
   27 | import org.aopalliance.intercept.MethodInvocation;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.aop.AopInvocationException;
   33 | import org.springframework.aop.RawTargetAccess;
   34 | import org.springframework.aop.TargetSource;
   35 | import org.springframework.aop.support.AopUtils;
   36 | import org.springframework.core.DecoratingProxy;
   37 | import org.springframework.core.KotlinDetector;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.ClassUtils;
   40 | 
   41 | /**
   42 |  * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
   43 |  * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
   44 |  *
   45 |  * <p>Creates a dynamic proxy, implementing the interfaces exposed by
   46 |  * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
   47 |  * defined in classes, rather than interfaces.
   48 |  *
   49 |  * <p>Objects of this type should be obtained through proxy factories,
   50 |  * configured by an {@link AdvisedSupport} class. This class is internal
   51 |  * to Spring's AOP framework and need not be used directly by client code.
   52 |  *
   53 |  * <p>Proxies created using this class will be thread-safe if the
   54 |  * underlying (target) class is thread-safe.
   55 |  *
   56 |  * <p>Proxies are serializable so long as all Advisors (including Advices
   57 |  * and Pointcuts) and the TargetSource are serializable.
   58 |  *
   59 |  * @author Rod Johnson
   60 |  * @author Juergen Hoeller
   61 |  * @author Rob Harrop
   62 |  * @author Dave Syer
   63 |  * @author Sergey Tsypanov
   64 |  * @author Sebastien Deleuze
   65 |  * @see java.lang.reflect.Proxy
   66 |  * @see AdvisedSupport
   67 |  * @see ProxyFactory
   68 |  */
   69 | final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {
   70 | 
   71 | 	/** use serialVersionUID from Spring 1.2 for interoperability. */
   72 | 	private static final long serialVersionUID = 5531744639992436476L;
   73 | 
   74 | 
   75 | 	private static final boolean COROUTINES_REACTOR_PRESENT = ClassUtils.isPresent(
   76 | 			"kotlinx.coroutines.reactor.MonoKt", JdkDynamicAopProxy.class.getClassLoader());
   77 | 
   78 | 	/** We use a static Log to avoid serialization issues. */
   79 | 	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);
   80 | 
   81 | 	/** Config used to configure this proxy. */
   82 | 	private final AdvisedSupport advised;
   83 | 
   84 | 	/** Cached in {@link AdvisedSupport#proxyMetadataCache}. */
   85 | 	private transient ProxiedInterfacesCache cache;
   86 | 
   87 | 
   88 | 	/**
   89 | 	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
   90 | 	 * @param config the AOP configuration as AdvisedSupport object
   91 | 	 * @throws AopConfigException if the config is invalid. We try to throw an informative
   92 | 	 * exception in this case, rather than let a mysterious failure happen later.
   93 | 	 */
   94 | 	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
   95 | 		Assert.notNull(config, "AdvisedSupport must not be null");
   96 | 		this.advised = config;
   97 | 
   98 | 		// Initialize ProxiedInterfacesCache if not cached already
   99 | 		ProxiedInterfacesCache cache;
  100 | 		if (config.proxyMetadataCache instanceof ProxiedInterfacesCache proxiedInterfacesCache) {
  101 | 			cache = proxiedInterfacesCache;
  102 | 		}
  103 | 		else {
  104 | 			cache = new ProxiedInterfacesCache(config);
  105 | 			config.proxyMetadataCache = cache;
  106 | 		}
  107 | 		this.cache = cache;
  108 | 	}
  109 | 
  110 | 
  111 | 	@Override
  112 | 	public Object getProxy() {
  113 | 		return getProxy(ClassUtils.getDefaultClassLoader());
  114 | 	}
  115 | 
  116 | 	@Override
  117 | 	public Object getProxy(@Nullable ClassLoader classLoader) {
  118 | 		if (logger.isTraceEnabled()) {
  119 | 			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
  120 | 		}
  121 | 		return Proxy.newProxyInstance(determineClassLoader(classLoader), this.cache.proxiedInterfaces, this);
  122 | 	}
  123 | 
  124 | 	@SuppressWarnings("deprecation")
  125 | 	@Override
  126 | 	public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
  127 | 		return Proxy.getProxyClass(determineClassLoader(classLoader), this.cache.proxiedInterfaces);
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Determine whether the JDK bootstrap or platform loader has been suggested ->
  132 | 	 * use higher-level loader which can see Spring infrastructure classes instead.
  133 | 	 */
  134 | 	private ClassLoader determineClassLoader(@Nullable ClassLoader classLoader) {
  135 | 		if (classLoader == null) {
  136 | 			// JDK bootstrap loader -> use spring-aop ClassLoader instead.
  137 | 			return getClass().getClassLoader();
  138 | 		}
  139 | 		if (classLoader.getParent() == null) {
  140 | 			// Potentially the JDK platform loader on JDK 9+
  141 | 			ClassLoader aopClassLoader = getClass().getClassLoader();
  142 | 			ClassLoader aopParent = aopClassLoader.getParent();
  143 | 			while (aopParent != null) {
  144 | 				if (classLoader == aopParent) {
  145 | 					// Suggested ClassLoader is ancestor of spring-aop ClassLoader
  146 | 					// -> use spring-aop ClassLoader itself instead.
  147 | 					return aopClassLoader;
  148 | 				}
  149 | 				aopParent = aopParent.getParent();
  150 | 			}
  151 | 		}
  152 | 		// Regular case: use suggested ClassLoader as-is.
  153 | 		return classLoader;
  154 | 	}
  155 | 
  156 | 
  157 | 	/**
  158 | 	 * Implementation of {@code InvocationHandler.invoke}.
  159 | 	 * <p>Callers will see exactly the exception thrown by the target,
  160 | 	 * unless a hook method throws an exception.
  161 | 	 */
  162 | 	@Override
  163 | 	public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  164 | 		Object oldProxy = null;
  165 | 		boolean setProxyContext = false;
  166 | 
  167 | 		TargetSource targetSource = this.advised.targetSource;
  168 | 		Object target = null;
  169 | 
  170 | 		try {
  171 | 			if (!this.cache.equalsDefined && AopUtils.isEqualsMethod(method)) {
  172 | 				// The target does not implement the equals(Object) method itself.
  173 | 				return equals(args[0]);
  174 | 			}
  175 | 			else if (!this.cache.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
  176 | 				// The target does not implement the hashCode() method itself.
  177 | 				return hashCode();
  178 | 			}
  179 | 			else if (method.getDeclaringClass() == DecoratingProxy.class) {
  180 | 				// There is only getDecoratedClass() declared -> dispatch to proxy config.
  181 | 				return AopProxyUtils.ultimateTargetClass(this.advised);
  182 | 			}
  183 | 			else if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() &&
  184 | 					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
  185 | 				// Service invocations on ProxyConfig with the proxy config...
  186 | 				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
  187 | 			}
  188 | 
  189 | 			Object retVal;
  190 | 
  191 | 			if (this.advised.isExposeProxy()) {
  192 | 				// Make invocation available if necessary.
  193 | 				oldProxy = AopContext.setCurrentProxy(proxy);
  194 | 				setProxyContext = true;
  195 | 			}
  196 | 
  197 | 			// Get as late as possible to minimize the time we "own" the target,
  198 | 			// in case it comes from a pool.
  199 | 			target = targetSource.getTarget();
  200 | 			Class<?> targetClass = (target != null ? target.getClass() : null);
  201 | 
  202 | 			// Get the interception chain for this method.
  203 | 			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
  204 | 
  205 | 			// Check whether we have any advice. If we don't, we can fall back on direct
  206 | 			// reflective invocation of the target, and avoid creating a MethodInvocation.
  207 | 			if (chain.isEmpty()) {
  208 | 				// We can skip creating a MethodInvocation: just invoke the target directly
  209 | 				// Note that the final invoker must be an InvokerInterceptor so we know it does
  210 | 				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
  211 | 				@Nullable Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
  212 | 				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
  213 | 			}
  214 | 			else {
  215 | 				// We need to create a method invocation...
  216 | 				MethodInvocation invocation =
  217 | 						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
  218 | 				// Proceed to the joinpoint through the interceptor chain.
  219 | 				retVal = invocation.proceed();
  220 | 			}
  221 | 
  222 | 			// Massage return value if necessary.
  223 | 			Class<?> returnType = method.getReturnType();
  224 | 			if (retVal != null && retVal == target &&
  225 | 					returnType != Object.class && returnType.isInstance(proxy) &&
  226 | 					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
  227 | 				// Special case: it returned "this" and the return type of the method
  228 | 				// is type-compatible. Note that we can't help if the target sets
  229 | 				// a reference to itself in another returned object.
  230 | 				retVal = proxy;
  231 | 			}
  232 | 			else if (retVal == null && returnType != void.class && returnType.isPrimitive()) {
  233 | 				throw new AopInvocationException(
  234 | 						"Null return value from advice does not match primitive return type for: " + method);
  235 | 			}
  236 | 			if (COROUTINES_REACTOR_PRESENT && KotlinDetector.isSuspendingFunction(method)) {
  237 | 				return CoroutinesUtils.adaptReturnValue(method, retVal, args[args.length - 1]);
  238 | 			}
  239 | 			return retVal;
  240 | 		}
  241 | 		finally {
  242 | 			if (target != null && !targetSource.isStatic()) {
  243 | 				// Must have come from TargetSource.
  244 | 				targetSource.releaseTarget(target);
  245 | 			}
  246 | 			if (setProxyContext) {
  247 | 				// Restore old proxy.
  248 | 				AopContext.setCurrentProxy(oldProxy);
  249 | 			}
  250 | 		}
  251 | 	}
  252 | 
  253 | 
  254 | 	/**
  255 | 	 * Equality means interfaces, advisors and TargetSource are equal.
  256 | 	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
  257 | 	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
  258 | 	 */
  259 | 	@Override
  260 | 	public boolean equals(@Nullable Object other) {
  261 | 		if (other == this) {
  262 | 			return true;
  263 | 		}
  264 | 		if (other == null) {
  265 | 			return false;
  266 | 		}
  267 | 
  268 | 		JdkDynamicAopProxy otherProxy;
  269 | 		if (other instanceof JdkDynamicAopProxy jdkDynamicAopProxy) {
  270 | 			otherProxy = jdkDynamicAopProxy;
  271 | 		}
  272 | 		else if (Proxy.isProxyClass(other.getClass())) {
  273 | 			InvocationHandler ih = Proxy.getInvocationHandler(other);
  274 | 			if (!(ih instanceof JdkDynamicAopProxy jdkDynamicAopProxy)) {
  275 | 				return false;
  276 | 			}
  277 | 			otherProxy = jdkDynamicAopProxy;
  278 | 		}
  279 | 		else {
  280 | 			// Not a valid comparison...
  281 | 			return false;
  282 | 		}
  283 | 
  284 | 		// If we get here, otherProxy is the other AopProxy.
  285 | 		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
  286 | 	}
  287 | 
  288 | 	/**
  289 | 	 * Proxy uses the hash code of the TargetSource.
  290 | 	 */
  291 | 	@Override
  292 | 	public int hashCode() {
  293 | 		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
  294 | 	}
  295 | 
  296 | 
  297 | 	//---------------------------------------------------------------------
  298 | 	// Serialization support
  299 | 	//---------------------------------------------------------------------
  300 | 
  301 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
  302 | 		// Rely on default serialization; just initialize state after deserialization.
  303 | 		ois.defaultReadObject();
  304 | 
  305 | 		// Initialize transient fields.
  306 | 		this.cache = new ProxiedInterfacesCache(this.advised);
  307 | 	}
  308 | 
  309 | 
  310 | 	/**
  311 | 	 * Holder for the complete proxied interfaces and derived metadata,
  312 | 	 * to be cached in {@link AdvisedSupport#proxyMetadataCache}.
  313 | 	 * @since 6.1.3
  314 | 	 */
  315 | 	private static final class ProxiedInterfacesCache {
  316 | 
  317 | 		final Class<?>[] proxiedInterfaces;
  318 | 
  319 | 		final boolean equalsDefined;
  320 | 
  321 | 		final boolean hashCodeDefined;
  322 | 
  323 | 		ProxiedInterfacesCache(AdvisedSupport config) {
  324 | 			this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(config, true);
  325 | 
  326 | 			// Find any {@link #equals} or {@link #hashCode} method that may be defined
  327 | 			// on the supplied set of interfaces.
  328 | 			boolean equalsDefined = false;
  329 | 			boolean hashCodeDefined = false;
  330 | 			for (Class<?> proxiedInterface : this.proxiedInterfaces) {
  331 | 				Method[] methods = proxiedInterface.getDeclaredMethods();
  332 | 				for (Method method : methods) {
  333 | 					if (AopUtils.isEqualsMethod(method)) {
  334 | 						equalsDefined = true;
  335 | 						if (hashCodeDefined) {
  336 | 							break;
  337 | 						}
  338 | 					}
  339 | 					if (AopUtils.isHashCodeMethod(method)) {
  340 | 						hashCodeDefined = true;
  341 | 						if (equalsDefined) {
  342 | 							break;
  343 | 						}
  344 | 					}
  345 | 				}
  346 | 				if (equalsDefined && hashCodeDefined) {
  347 | 					break;
  348 | 				}
  349 | 			}
  350 | 			this.equalsDefined = equalsDefined;
  351 | 			this.hashCodeDefined = hashCodeDefined;
  352 | 		}
  353 | 	}
  354 | 
  355 | }
```
