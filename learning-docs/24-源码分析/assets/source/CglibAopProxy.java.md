<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/CglibAopProxy.java -->
<!-- 正文引用：[[源码:CglibAopProxy.java#L起-L止]] ；代码块头部：(src: CglibAopProxy.java#L起-L止) -->

# CglibAopProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/CglibAopProxy.java` |
| 行数 | 929 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.651Z |

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
   21 | import java.lang.reflect.Modifier;
   22 | import java.lang.reflect.UndeclaredThrowableException;
   23 | import java.util.ArrayList;
   24 | import java.util.Collections;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | import java.util.WeakHashMap;
   29 | 
   30 | import org.aopalliance.intercept.MethodInvocation;
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.aop.AopInvocationException;
   36 | import org.springframework.aop.RawTargetAccess;
   37 | import org.springframework.aop.TargetSource;
   38 | import org.springframework.aop.support.AopUtils;
   39 | import org.springframework.aot.AotDetector;
   40 | import org.springframework.cglib.core.ClassLoaderAwareGeneratorStrategy;
   41 | import org.springframework.cglib.core.CodeGenerationException;
   42 | import org.springframework.cglib.core.GeneratorStrategy;
   43 | import org.springframework.cglib.core.SpringNamingPolicy;
   44 | import org.springframework.cglib.proxy.Callback;
   45 | import org.springframework.cglib.proxy.CallbackFilter;
   46 | import org.springframework.cglib.proxy.Dispatcher;
   47 | import org.springframework.cglib.proxy.Enhancer;
   48 | import org.springframework.cglib.proxy.Factory;
   49 | import org.springframework.cglib.proxy.MethodInterceptor;
   50 | import org.springframework.cglib.proxy.MethodProxy;
   51 | import org.springframework.cglib.proxy.NoOp;
   52 | import org.springframework.cglib.transform.impl.UndeclaredThrowableStrategy;
   53 | import org.springframework.core.KotlinDetector;
   54 | import org.springframework.core.SmartClassLoader;
   55 | import org.springframework.util.Assert;
   56 | import org.springframework.util.ClassUtils;
   57 | import org.springframework.util.CollectionUtils;
   58 | import org.springframework.util.ObjectUtils;
   59 | 
   60 | /**
   61 |  * CGLIB-based {@link AopProxy} implementation for the Spring AOP framework.
   62 |  *
   63 |  * <p>Objects of this type should be obtained through proxy factories,
   64 |  * configured by an {@link AdvisedSupport} object. This class is internal
   65 |  * to Spring's AOP framework and need not be used directly by client code.
   66 |  *
   67 |  * <p>{@link DefaultAopProxyFactory} will automatically create CGLIB-based
   68 |  * proxies if necessary, for example in case of proxying a target class
   69 |  * (see the {@link DefaultAopProxyFactory attendant javadoc} for details).
   70 |  *
   71 |  * <p>Proxies created using this class are thread-safe if the underlying
   72 |  * (target) class is thread-safe.
   73 |  *
   74 |  * @author Rod Johnson
   75 |  * @author Rob Harrop
   76 |  * @author Juergen Hoeller
   77 |  * @author Ramnivas Laddad
   78 |  * @author Chris Beams
   79 |  * @author Dave Syer
   80 |  * @author Sebastien Deleuze
   81 |  * @see org.springframework.cglib.proxy.Enhancer
   82 |  * @see AdvisedSupport#setProxyTargetClass
   83 |  * @see DefaultAopProxyFactory
   84 |  */
   85 | @SuppressWarnings("serial")
   86 | class CglibAopProxy implements AopProxy, Serializable {
   87 | 
   88 | 	// Constants for CGLIB callback array indices
   89 | 	private static final int AOP_PROXY = 0;
   90 | 	private static final int INVOKE_TARGET = 1;
   91 | 	private static final int NO_OVERRIDE = 2;
   92 | 	private static final int DISPATCH_TARGET = 3;
   93 | 	private static final int DISPATCH_ADVISED = 4;
   94 | 	private static final int INVOKE_EQUALS = 5;
   95 | 	private static final int INVOKE_HASHCODE = 6;
   96 | 
   97 | 
   98 | 	private static final boolean COROUTINES_REACTOR_PRESENT = ClassUtils.isPresent(
   99 | 			"kotlinx.coroutines.reactor.MonoKt", CglibAopProxy.class.getClassLoader());
  100 | 
  101 | 	private static final GeneratorStrategy undeclaredThrowableStrategy =
  102 | 			new UndeclaredThrowableStrategy(UndeclaredThrowableException.class);
  103 | 
  104 | 	/** Logger available to subclasses; static to optimize serialization. */
  105 | 	protected static final Log logger = LogFactory.getLog(CglibAopProxy.class);
  106 | 
  107 | 	/** Keeps track of the Classes that we have validated for final methods. */
  108 | 	private static final Map<Class<?>, Boolean> validatedClasses = new WeakHashMap<>();
  109 | 
  110 | 
  111 | 	/** The configuration used to configure this proxy. */
  112 | 	protected final AdvisedSupport advised;
  113 | 
  114 | 	protected Object @Nullable [] constructorArgs;
  115 | 
  116 | 	protected Class<?> @Nullable [] constructorArgTypes;
  117 | 
  118 | 	/** Dispatcher used for methods on Advised. */
  119 | 	private final transient AdvisedDispatcher advisedDispatcher;
  120 | 
  121 | 	private transient Map<Method, Integer> fixedInterceptorMap = Collections.emptyMap();
  122 | 
  123 | 	private transient int fixedInterceptorOffset;
  124 | 
  125 | 
  126 | 	/**
  127 | 	 * Create a new CglibAopProxy for the given AOP configuration.
  128 | 	 * @param config the AOP configuration as AdvisedSupport object
  129 | 	 * @throws AopConfigException if the config is invalid. We try to throw an informative
  130 | 	 * exception in this case, rather than let a mysterious failure happen later.
  131 | 	 */
  132 | 	public CglibAopProxy(AdvisedSupport config) throws AopConfigException {
  133 | 		Assert.notNull(config, "AdvisedSupport must not be null");
  134 | 		this.advised = config;
  135 | 		this.advisedDispatcher = new AdvisedDispatcher(this.advised);
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Set constructor arguments to use for creating the proxy.
  140 | 	 * @param constructorArgs the constructor argument values
  141 | 	 * @param constructorArgTypes the constructor argument types
  142 | 	 */
  143 | 	public void setConstructorArguments(Object @Nullable [] constructorArgs, Class<?> @Nullable [] constructorArgTypes) {
  144 | 		if (constructorArgs == null || constructorArgTypes == null) {
  145 | 			throw new IllegalArgumentException("Both 'constructorArgs' and 'constructorArgTypes' need to be specified");
  146 | 		}
  147 | 		if (constructorArgs.length != constructorArgTypes.length) {
  148 | 			throw new IllegalArgumentException("Number of 'constructorArgs' (" + constructorArgs.length +
  149 | 					") must match number of 'constructorArgTypes' (" + constructorArgTypes.length + ")");
  150 | 		}
  151 | 		this.constructorArgs = constructorArgs;
  152 | 		this.constructorArgTypes = constructorArgTypes;
  153 | 	}
  154 | 
  155 | 
  156 | 	@Override
  157 | 	public Object getProxy() {
  158 | 		return buildProxy(null, false);
  159 | 	}
  160 | 
  161 | 	@Override
  162 | 	public Object getProxy(@Nullable ClassLoader classLoader) {
  163 | 		return buildProxy(classLoader, false);
  164 | 	}
  165 | 
  166 | 	@Override
  167 | 	public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
  168 | 		return (Class<?>) buildProxy(classLoader, true);
  169 | 	}
  170 | 
  171 | 	private Object buildProxy(@Nullable ClassLoader classLoader, boolean classOnly) {
  172 | 		if (logger.isTraceEnabled()) {
  173 | 			logger.trace("Creating CGLIB proxy: " + this.advised.getTargetSource());
  174 | 		}
  175 | 
  176 | 		try {
  177 | 			Class<?> rootClass = this.advised.getTargetClass();
  178 | 			Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");
  179 | 
  180 | 			Class<?> proxySuperClass = rootClass;
  181 | 			if (rootClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
  182 | 				proxySuperClass = rootClass.getSuperclass();
  183 | 				Class<?>[] additionalInterfaces = rootClass.getInterfaces();
  184 | 				for (Class<?> additionalInterface : additionalInterfaces) {
  185 | 					this.advised.addInterface(additionalInterface);
  186 | 				}
  187 | 			}
  188 | 
  189 | 			// Validate the class, writing log messages as necessary.
  190 | 			validateClassIfNecessary(proxySuperClass, classLoader);
  191 | 
  192 | 			// Configure CGLIB Enhancer...
  193 | 			Enhancer enhancer = createEnhancer();
  194 | 			if (classLoader != null) {
  195 | 				enhancer.setClassLoader(classLoader);
  196 | 				if (classLoader instanceof SmartClassLoader smartClassLoader &&
  197 | 						smartClassLoader.isClassReloadable(proxySuperClass)) {
  198 | 					enhancer.setUseCache(false);
  199 | 				}
  200 | 			}
  201 | 			enhancer.setSuperclass(proxySuperClass);
  202 | 			enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(this.advised));
  203 | 			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
  204 | 			enhancer.setAttemptLoad(enhancer.getUseCache() && AotDetector.useGeneratedArtifacts());
  205 | 			enhancer.setStrategy(KotlinDetector.isKotlinType(proxySuperClass) ?
  206 | 					new ClassLoaderAwareGeneratorStrategy(classLoader) :
  207 | 					new ClassLoaderAwareGeneratorStrategy(classLoader, undeclaredThrowableStrategy)
  208 | 			);
  209 | 
  210 | 			Callback[] callbacks = getCallbacks(rootClass);
  211 | 			Class<?>[] types = new Class<?>[callbacks.length];
  212 | 			for (int x = 0; x < types.length; x++) {
  213 | 				types[x] = callbacks[x].getClass();
  214 | 			}
  215 | 			// fixedInterceptorMap only populated at this point, after getCallbacks call above
  216 | 			ProxyCallbackFilter filter = new ProxyCallbackFilter(
  217 | 					this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset);
  218 | 			enhancer.setCallbackFilter(filter);
  219 | 			enhancer.setCallbackTypes(types);
  220 | 
  221 | 			// Generate the proxy class and create a proxy instance.
  222 | 			// ProxyCallbackFilter has method introspection capability with Advisor access.
  223 | 			try {
  224 | 				return (classOnly ? createProxyClass(enhancer) : createProxyClassAndInstance(enhancer, callbacks));
  225 | 			}
  226 | 			finally {
  227 | 				// Reduce ProxyCallbackFilter to key-only state for its class cache role
  228 | 				// in the CGLIB$CALLBACK_FILTER field, not leaking any Advisor state...
  229 | 				filter.advised.reduceToAdvisorKey();
  230 | 			}
  231 | 		}
  232 | 		catch (CodeGenerationException | IllegalArgumentException ex) {
  233 | 			throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() +
  234 | 					": Common causes of this problem include using a final class or a non-visible class",
  235 | 					ex);
  236 | 		}
  237 | 		catch (Throwable ex) {
  238 | 			// TargetSource.getTarget() failed
  239 | 			throw new AopConfigException("Unexpected AOP exception", ex);
  240 | 		}
  241 | 	}
  242 | 
  243 | 	protected Class<?> createProxyClass(Enhancer enhancer) {
  244 | 		enhancer.setInterceptDuringConstruction(false);
  245 | 		return enhancer.createClass();
  246 | 	}
  247 | 
  248 | 	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
  249 | 		enhancer.setInterceptDuringConstruction(false);
  250 | 		enhancer.setCallbacks(callbacks);
  251 | 		return (this.constructorArgs != null && this.constructorArgTypes != null ?
  252 | 				enhancer.create(this.constructorArgTypes, this.constructorArgs) :
  253 | 				enhancer.create());
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Creates the CGLIB {@link Enhancer}. Subclasses may wish to override this to return a custom
  258 | 	 * {@link Enhancer} implementation.
  259 | 	 */
  260 | 	protected Enhancer createEnhancer() {
  261 | 		return new Enhancer();
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * Checks to see whether the supplied {@code Class} has already been validated and
  266 | 	 * validates it if not.
  267 | 	 */
  268 | 	private void validateClassIfNecessary(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader) {
  269 | 		if (!this.advised.isOptimize() && logger.isInfoEnabled()) {
  270 | 			synchronized (validatedClasses) {
  271 | 				validatedClasses.computeIfAbsent(proxySuperClass, clazz -> {
  272 | 					doValidateClass(clazz, proxyClassLoader, ClassUtils.getAllInterfacesForClassAsSet(clazz));
  273 | 					return Boolean.TRUE;
  274 | 				});
  275 | 			}
  276 | 		}
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Checks for final methods on the given {@code Class}, as well as package-visible
  281 | 	 * methods across ClassLoaders, and writes warnings to the log for each one found.
  282 | 	 */
  283 | 	private void doValidateClass(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader, Set<Class<?>> ifcs) {
  284 | 		if (proxySuperClass != Object.class) {
  285 | 			Method[] methods = proxySuperClass.getDeclaredMethods();
  286 | 			for (Method method : methods) {
  287 | 				int mod = method.getModifiers();
  288 | 				if (!Modifier.isStatic(mod) && !Modifier.isPrivate(mod)) {
  289 | 					if (Modifier.isFinal(mod)) {
  290 | 						if (logger.isWarnEnabled() && Modifier.isPublic(mod)) {
  291 | 							if (implementsInterface(method, ifcs)) {
  292 | 								// Final methods inherited from configuration callback interfaces are
  293 | 								// typically driven by the container itself rather than by user code, so
  294 | 								// logging a warning about CGLIB being unable to advise them is misleading noise.
  295 | 								if (!implementsOnlyConfigurationCallbackInterfaces(method, ifcs)) {
  296 | 									logger.warn("Unable to proxy interface-implementing method [" + method + "] because " +
  297 | 											"it is marked as final, consider using interface-based JDK proxies instead.");
  298 | 								}
  299 | 							}
  300 | 							else {
  301 | 								logger.warn("Public final method [" + method + "] cannot get proxied via CGLIB, " +
  302 | 										"consider removing the final marker or using interface-based JDK proxies.");
  303 | 							}
  304 | 						}
  305 | 						if (logger.isDebugEnabled()) {
  306 | 							logger.debug("Final method [" + method + "] cannot get proxied via CGLIB: " +
  307 | 									"Calls to this method will NOT be routed to the target instance and " +
  308 | 									"might lead to NPEs against uninitialized fields in the proxy instance.");
  309 | 						}
  310 | 					}
  311 | 					else if (logger.isDebugEnabled() && !Modifier.isPublic(mod) && !Modifier.isProtected(mod) &&
  312 | 							proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
  313 | 						logger.debug("Method [" + method + "] is package-visible across different ClassLoaders " +
  314 | 								"and cannot get proxied via CGLIB: Declare this method as public or protected " +
  315 | 								"if you need to support invocations through the proxy.");
  316 | 					}
  317 | 				}
  318 | 			}
  319 | 			doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader, ifcs);
  320 | 		}
  321 | 	}
  322 | 
  323 | 	private Callback[] getCallbacks(Class<?> rootClass) throws Exception {
  324 | 		// Parameters used for optimization choices...
  325 | 		boolean isStatic = this.advised.getTargetSource().isStatic();
  326 | 		boolean isFrozen = this.advised.isFrozen();
  327 | 		boolean exposeProxy = this.advised.isExposeProxy();
  328 | 
  329 | 		// Choose an "aop" interceptor (used for AOP calls).
  330 | 		Callback aopInterceptor = new DynamicAdvisedInterceptor(this.advised);
  331 | 
  332 | 		// Choose a "straight to target" interceptor. (used for calls that are
  333 | 		// unadvised but can return this). May be required to expose the proxy.
  334 | 		Callback targetInterceptor;
  335 | 		if (exposeProxy) {
  336 | 			targetInterceptor = (isStatic ?
  337 | 					new StaticUnadvisedExposedInterceptor(this.advised.getTargetSource().getTarget()) :
  338 | 					new DynamicUnadvisedExposedInterceptor(this.advised.getTargetSource()));
  339 | 		}
  340 | 		else {
  341 | 			targetInterceptor = (isStatic ?
  342 | 					new StaticUnadvisedInterceptor(this.advised.getTargetSource().getTarget()) :
  343 | 					new DynamicUnadvisedInterceptor(this.advised.getTargetSource()));
  344 | 		}
  345 | 
  346 | 		// Choose a "direct to target" dispatcher (used for
  347 | 		// unadvised calls to static targets that cannot return this).
  348 | 		Callback targetDispatcher = (isStatic ?
  349 | 				new StaticDispatcher(this.advised.getTargetSource().getTarget()) : new SerializableNoOp());
  350 | 
  351 | 		Callback[] mainCallbacks = new Callback[] {
  352 | 				aopInterceptor,  // for normal advice
  353 | 				targetInterceptor,  // invoke target without considering advice, if optimized
  354 | 				new SerializableNoOp(),  // no override for methods mapped to this
  355 | 				targetDispatcher, this.advisedDispatcher,
  356 | 				new EqualsInterceptor(this.advised),
  357 | 				new HashCodeInterceptor(this.advised)
  358 | 		};
  359 | 
  360 | 		// If the target is a static one and the advice chain is frozen,
  361 | 		// then we can make some optimizations by sending the AOP calls
  362 | 		// direct to the target using the fixed chain for that method.
  363 | 		if (isStatic && isFrozen) {
  364 | 			Method[] methods = rootClass.getMethods();
  365 | 			int methodsCount = methods.length;
  366 | 			List<Callback> fixedCallbacks = new ArrayList<>(methodsCount);
  367 | 			this.fixedInterceptorMap = CollectionUtils.newHashMap(methodsCount);
  368 | 
  369 | 			int advicedMethodCount = methodsCount;
  370 | 			for (int x = 0; x < methodsCount; x++) {
  371 | 				Method method = methods[x];
  372 | 				//do not create advices for non-overridden methods of java.lang.Object
  373 | 				if (method.getDeclaringClass() == Object.class) {
  374 | 					advicedMethodCount--;
  375 | 					continue;
  376 | 				}
  377 | 				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, rootClass);
  378 | 				fixedCallbacks.add(new FixedChainStaticTargetInterceptor(
  379 | 						chain, this.advised.getTargetSource().getTarget(), this.advised.getTargetClass()));
  380 | 				this.fixedInterceptorMap.put(method, x - (methodsCount - advicedMethodCount) );
  381 | 			}
  382 | 
  383 | 			// Now copy both the callbacks from mainCallbacks
  384 | 			// and fixedCallbacks into the callbacks array.
  385 | 			Callback[] callbacks = new Callback[mainCallbacks.length + advicedMethodCount];
  386 | 			System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
  387 | 			System.arraycopy(fixedCallbacks.toArray(Callback[]::new), 0, callbacks,
  388 | 					mainCallbacks.length, advicedMethodCount);
  389 | 			this.fixedInterceptorOffset = mainCallbacks.length;
  390 | 			return callbacks;
  391 | 		}
  392 | 		return mainCallbacks;
  393 | 	}
  394 | 
  395 | 
  396 | 	@Override
  397 | 	public boolean equals(@Nullable Object other) {
  398 | 		return (this == other || (other instanceof CglibAopProxy that &&
  399 | 				AopProxyUtils.equalsInProxy(this.advised, that.advised)));
  400 | 	}
  401 | 
  402 | 	@Override
  403 | 	public int hashCode() {
  404 | 		return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
  405 | 	}
  406 | 
  407 | 
  408 | 	/**
  409 | 	 * Check whether the given method is declared on any of the given interfaces.
  410 | 	 */
  411 | 	private static boolean implementsInterface(Method method, Set<Class<?>> ifcs) {
  412 | 		for (Class<?> ifc : ifcs) {
  413 | 			if (ClassUtils.hasMethod(ifc, method)) {
  414 | 				return true;
  415 | 			}
  416 | 		}
  417 | 		return false;
  418 | 	}
  419 | 
  420 | 	/**
  421 | 	 * Check whether every interface that declares the given method is a
  422 | 	 * configuration callback interface.
  423 | 	 * @since 7.1
  424 | 	 * @see AopProxyUtils#isConfigurationCallbackInterface(Class)
  425 | 	 */
  426 | 	static boolean implementsOnlyConfigurationCallbackInterfaces(Method method, Set<Class<?>> ifcs) {
  427 | 		boolean matched = false;
  428 | 		for (Class<?> ifc : ifcs) {
  429 | 			if (ClassUtils.hasMethod(ifc, method)) {
  430 | 				if (!AopProxyUtils.isConfigurationCallbackInterface(ifc)) {
  431 | 					return false;
  432 | 				}
  433 | 				matched = true;
  434 | 			}
  435 | 		}
  436 | 		return matched;
  437 | 	}
  438 | 
  439 | 	/**
  440 | 	 * Process a return value. Wraps a return of {@code this} if necessary to be the
  441 | 	 * {@code proxy} and also verifies that {@code null} is not returned as a primitive.
  442 | 	 * Also takes care of the conversion from {@code Mono} to Kotlin Coroutines if needed.
  443 | 	 */
  444 | 	private static @Nullable Object processReturnType(
  445 | 			Object proxy, @Nullable Object target, Method method, Object[] arguments, @Nullable Object returnValue) {
  446 | 
  447 | 		// Massage return value if necessary
  448 | 		if (returnValue != null && returnValue == target &&
  449 | 				!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
  450 | 			// Special case: it returned "this". Note that we can't help
  451 | 			// if the target sets a reference to itself in another returned object.
  452 | 			returnValue = proxy;
  453 | 		}
  454 | 		Class<?> returnType = method.getReturnType();
  455 | 		if (returnValue == null && returnType != void.class && returnType.isPrimitive()) {
  456 | 			throw new AopInvocationException(
  457 | 					"Null return value from advice does not match primitive return type for: " + method);
  458 | 		}
  459 | 		if (COROUTINES_REACTOR_PRESENT && KotlinDetector.isSuspendingFunction(method)) {
  460 | 			return CoroutinesUtils.adaptReturnValue(method, returnValue, arguments[arguments.length - 1]);
  461 | 		}
  462 | 		return returnValue;
  463 | 	}
  464 | 
  465 | 
  466 | 	/**
  467 | 	 * Serializable replacement for CGLIB's NoOp interface.
  468 | 	 * Public to allow use elsewhere in the framework.
  469 | 	 */
  470 | 	public static class SerializableNoOp implements NoOp, Serializable {
  471 | 	}
  472 | 
  473 | 
  474 | 	/**
  475 | 	 * Method interceptor used for static targets with no advice chain. The call is
  476 | 	 * passed directly back to the target. Used when the proxy needs to be exposed
  477 | 	 * and it can't be determined that the method won't return {@code this}.
  478 | 	 */
  479 | 	private static class StaticUnadvisedInterceptor implements MethodInterceptor, Serializable {
  480 | 
  481 | 		private final @Nullable Object target;
  482 | 
  483 | 		public StaticUnadvisedInterceptor(@Nullable Object target) {
  484 | 			this.target = target;
  485 | 		}
  486 | 
  487 | 		@Override
  488 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  489 | 			Object retVal = AopUtils.invokeJoinpointUsingReflection(this.target, method, args);
  490 | 			return processReturnType(proxy, this.target, method, args, retVal);
  491 | 		}
  492 | 	}
  493 | 
  494 | 
  495 | 	/**
  496 | 	 * Method interceptor used for static targets with no advice chain, when the
  497 | 	 * proxy is to be exposed.
  498 | 	 */
  499 | 	private static class StaticUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {
  500 | 
  501 | 		private final @Nullable Object target;
  502 | 
  503 | 		public StaticUnadvisedExposedInterceptor(@Nullable Object target) {
  504 | 			this.target = target;
  505 | 		}
  506 | 
  507 | 		@Override
  508 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  509 | 			Object oldProxy = null;
  510 | 			try {
  511 | 				oldProxy = AopContext.setCurrentProxy(proxy);
  512 | 				Object retVal = AopUtils.invokeJoinpointUsingReflection(this.target, method, args);
  513 | 				return processReturnType(proxy, this.target, method, args, retVal);
  514 | 			}
  515 | 			finally {
  516 | 				AopContext.setCurrentProxy(oldProxy);
  517 | 			}
  518 | 		}
  519 | 	}
  520 | 
  521 | 
  522 | 	/**
  523 | 	 * Interceptor used to invoke a dynamic target without creating a method
  524 | 	 * invocation or evaluating an advice chain. (We know there was no advice
  525 | 	 * for this method.)
  526 | 	 */
  527 | 	private static class DynamicUnadvisedInterceptor implements MethodInterceptor, Serializable {
  528 | 
  529 | 		private final TargetSource targetSource;
  530 | 
  531 | 		public DynamicUnadvisedInterceptor(TargetSource targetSource) {
  532 | 			this.targetSource = targetSource;
  533 | 		}
  534 | 
  535 | 		@Override
  536 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  537 | 			Object target = this.targetSource.getTarget();
  538 | 			try {
  539 | 				Object retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
  540 | 				return processReturnType(proxy, target, method, args, retVal);
  541 | 			}
  542 | 			finally {
  543 | 				if (target != null) {
  544 | 					this.targetSource.releaseTarget(target);
  545 | 				}
  546 | 			}
  547 | 		}
  548 | 	}
  549 | 
  550 | 
  551 | 	/**
  552 | 	 * Interceptor for unadvised dynamic targets when the proxy needs exposing.
  553 | 	 */
  554 | 	private static class DynamicUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {
  555 | 
  556 | 		private final TargetSource targetSource;
  557 | 
  558 | 		public DynamicUnadvisedExposedInterceptor(TargetSource targetSource) {
  559 | 			this.targetSource = targetSource;
  560 | 		}
  561 | 
  562 | 		@Override
  563 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  564 | 			Object oldProxy = null;
  565 | 			Object target = this.targetSource.getTarget();
  566 | 			try {
  567 | 				oldProxy = AopContext.setCurrentProxy(proxy);
  568 | 				Object retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
  569 | 				return processReturnType(proxy, target, method, args, retVal);
  570 | 			}
  571 | 			finally {
  572 | 				AopContext.setCurrentProxy(oldProxy);
  573 | 				if (target != null) {
  574 | 					this.targetSource.releaseTarget(target);
  575 | 				}
  576 | 			}
  577 | 		}
  578 | 	}
  579 | 
  580 | 
  581 | 	/**
  582 | 	 * Dispatcher for a static target. Dispatcher is much faster than
  583 | 	 * interceptor. This will be used whenever it can be determined that a
  584 | 	 * method definitely does not return "this"
  585 | 	 */
  586 | 	private static class StaticDispatcher implements Dispatcher, Serializable {
  587 | 
  588 | 		private final @Nullable Object target;
  589 | 
  590 | 		public StaticDispatcher(@Nullable Object target) {
  591 | 			this.target = target;
  592 | 		}
  593 | 
  594 | 		@Override
  595 | 		public @Nullable Object loadObject() {
  596 | 			return this.target;
  597 | 		}
  598 | 	}
  599 | 
  600 | 
  601 | 	/**
  602 | 	 * Dispatcher for any methods declared on the Advised class.
  603 | 	 */
  604 | 	private static class AdvisedDispatcher implements Dispatcher, Serializable {
  605 | 
  606 | 		private final AdvisedSupport advised;
  607 | 
  608 | 		public AdvisedDispatcher(AdvisedSupport advised) {
  609 | 			this.advised = advised;
  610 | 		}
  611 | 
  612 | 		@Override
  613 | 		public Object loadObject() {
  614 | 			return this.advised;
  615 | 		}
  616 | 	}
  617 | 
  618 | 
  619 | 	/**
  620 | 	 * Dispatcher for the {@code equals} method.
  621 | 	 * Ensures that the method call is always handled by this class.
  622 | 	 */
  623 | 	private static class EqualsInterceptor implements MethodInterceptor, Serializable {
  624 | 
  625 | 		private final AdvisedSupport advised;
  626 | 
  627 | 		public EqualsInterceptor(AdvisedSupport advised) {
  628 | 			this.advised = advised;
  629 | 		}
  630 | 
  631 | 		@Override
  632 | 		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
  633 | 			Object other = args[0];
  634 | 			if (proxy == other) {
  635 | 				return true;
  636 | 			}
  637 | 			if (other instanceof Factory factory) {
  638 | 				Callback callback = factory.getCallback(INVOKE_EQUALS);
  639 | 				return (callback instanceof EqualsInterceptor that &&
  640 | 						AopProxyUtils.equalsInProxy(this.advised, that.advised));
  641 | 			}
  642 | 			return false;
  643 | 		}
  644 | 	}
  645 | 
  646 | 
  647 | 	/**
  648 | 	 * Dispatcher for the {@code hashCode} method.
  649 | 	 * Ensures that the method call is always handled by this class.
  650 | 	 */
  651 | 	private static class HashCodeInterceptor implements MethodInterceptor, Serializable {
  652 | 
  653 | 		private final AdvisedSupport advised;
  654 | 
  655 | 		public HashCodeInterceptor(AdvisedSupport advised) {
  656 | 			this.advised = advised;
  657 | 		}
  658 | 
  659 | 		@Override
  660 | 		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
  661 | 			return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
  662 | 		}
  663 | 	}
  664 | 
  665 | 
  666 | 	/**
  667 | 	 * Interceptor used specifically for advised methods on a frozen, static proxy.
  668 | 	 */
  669 | 	private static class FixedChainStaticTargetInterceptor implements MethodInterceptor, Serializable {
  670 | 
  671 | 		private final List<Object> adviceChain;
  672 | 
  673 | 		private final @Nullable Object target;
  674 | 
  675 | 		private final @Nullable Class<?> targetClass;
  676 | 
  677 | 		public FixedChainStaticTargetInterceptor(
  678 | 				List<Object> adviceChain, @Nullable Object target, @Nullable Class<?> targetClass) {
  679 | 
  680 | 			this.adviceChain = adviceChain;
  681 | 			this.target = target;
  682 | 			this.targetClass = targetClass;
  683 | 		}
  684 | 
  685 | 		@Override
  686 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  687 | 			MethodInvocation invocation = new ReflectiveMethodInvocation(
  688 | 					proxy, this.target, method, args, this.targetClass, this.adviceChain);
  689 | 			// If we get here, we need to create a MethodInvocation.
  690 | 			Object retVal = invocation.proceed();
  691 | 			retVal = processReturnType(proxy, this.target, method, args, retVal);
  692 | 			return retVal;
  693 | 		}
  694 | 	}
  695 | 
  696 | 
  697 | 	/**
  698 | 	 * General purpose AOP callback. Used when the target is dynamic or when the
  699 | 	 * proxy is not frozen.
  700 | 	 */
  701 | 	private static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {
  702 | 
  703 | 		private final AdvisedSupport advised;
  704 | 
  705 | 		public DynamicAdvisedInterceptor(AdvisedSupport advised) {
  706 | 			this.advised = advised;
  707 | 		}
  708 | 
  709 | 		@Override
  710 | 		public @Nullable Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  711 | 			Object oldProxy = null;
  712 | 			boolean setProxyContext = false;
  713 | 			Object target = null;
  714 | 			TargetSource targetSource = this.advised.getTargetSource();
  715 | 			try {
  716 | 				if (this.advised.isExposeProxy()) {
  717 | 					// Make invocation available if necessary.
  718 | 					oldProxy = AopContext.setCurrentProxy(proxy);
  719 | 					setProxyContext = true;
  720 | 				}
  721 | 				// Get as late as possible to minimize the time we "own" the target, in case it comes from a pool...
  722 | 				target = targetSource.getTarget();
  723 | 				Class<?> targetClass = (target != null ? target.getClass() : null);
  724 | 				List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
  725 | 				Object retVal;
  726 | 				// Check whether we only have one InvokerInterceptor: that is,
  727 | 				// no real advice, but just reflective invocation of the target.
  728 | 				if (chain.isEmpty()) {
  729 | 					// We can skip creating a MethodInvocation: just invoke the target directly.
  730 | 					// Note that the final invoker must be an InvokerInterceptor, so we know
  731 | 					// it does nothing but a reflective operation on the target, and no hot
  732 | 					// swapping or fancy proxying.
  733 | 					@Nullable Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
  734 | 					retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
  735 | 				}
  736 | 				else {
  737 | 					// We need to create a method invocation...
  738 | 					retVal = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain).proceed();
  739 | 				}
  740 | 				return processReturnType(proxy, target, method, args, retVal);
  741 | 			}
  742 | 			finally {
  743 | 				if (target != null && !targetSource.isStatic()) {
  744 | 					targetSource.releaseTarget(target);
  745 | 				}
  746 | 				if (setProxyContext) {
  747 | 					// Restore old proxy.
  748 | 					AopContext.setCurrentProxy(oldProxy);
  749 | 				}
  750 | 			}
  751 | 		}
  752 | 
  753 | 		@Override
  754 | 		public boolean equals(@Nullable Object other) {
  755 | 			return (this == other ||
  756 | 					(other instanceof DynamicAdvisedInterceptor dynamicAdvisedInterceptor &&
  757 | 							this.advised.equals(dynamicAdvisedInterceptor.advised)));
  758 | 		}
  759 | 
  760 | 		/**
  761 | 		 * CGLIB uses this to drive proxy creation.
  762 | 		 */
  763 | 		@Override
  764 | 		public int hashCode() {
  765 | 			return this.advised.hashCode();
  766 | 		}
  767 | 	}
  768 | 
  769 | 
  770 | 	/**
  771 | 	 * CallbackFilter to assign Callbacks to methods.
  772 | 	 */
  773 | 	private static class ProxyCallbackFilter implements CallbackFilter {
  774 | 
  775 | 		final AdvisedSupport advised;
  776 | 
  777 | 		private final Map<Method, Integer> fixedInterceptorMap;
  778 | 
  779 | 		private final int fixedInterceptorOffset;
  780 | 
  781 | 		public ProxyCallbackFilter(
  782 | 				AdvisedSupport advised, Map<Method, Integer> fixedInterceptorMap, int fixedInterceptorOffset) {
  783 | 
  784 | 			this.advised = advised;
  785 | 			this.fixedInterceptorMap = fixedInterceptorMap;
  786 | 			this.fixedInterceptorOffset = fixedInterceptorOffset;
  787 | 		}
  788 | 
  789 | 		/**
  790 | 		 * Implementation of CallbackFilter.accept() to return the index of the
  791 | 		 * callback we need.
  792 | 		 * <p>The callbacks for each proxy are built up of a set of fixed callbacks
  793 | 		 * for general use and then a set of callbacks that are specific to a method
  794 | 		 * for use on static targets with a fixed advice chain.
  795 | 		 * <p>The callback used is determined thus:
  796 | 		 * <dl>
  797 | 		 * <dt>For exposed proxies</dt>
  798 | 		 * <dd>Exposing the proxy requires code to execute before and after the
  799 | 		 * method/chain invocation. This means we must use
  800 | 		 * DynamicAdvisedInterceptor, since all other interceptors can avoid the
  801 | 		 * need for a try/catch block</dd>
  802 | 		 * <dt>For Object.finalize():</dt>
  803 | 		 * <dd>No override for this method is used.</dd>
  804 | 		 * <dt>For equals():</dt>
  805 | 		 * <dd>The EqualsInterceptor is used to redirect equals() calls to a
  806 | 		 * special handler to this proxy.</dd>
  807 | 		 * <dt>For methods on the Advised class:</dt>
  808 | 		 * <dd>the AdvisedDispatcher is used to dispatch the call directly to
  809 | 		 * the target</dd>
  810 | 		 * <dt>For advised methods:</dt>
  811 | 		 * <dd>If the target is static and the advice chain is frozen then a
  812 | 		 * FixedChainStaticTargetInterceptor specific to the method is used to
  813 | 		 * invoke the advice chain. Otherwise a DynamicAdvisedInterceptor is
  814 | 		 * used.</dd>
  815 | 		 * <dt>For non-advised methods:</dt>
  816 | 		 * <dd>Where it can be determined that the method will not return {@code this}
  817 | 		 * or when {@code ProxyFactory.getExposeProxy()} returns {@code false},
  818 | 		 * then a Dispatcher is used. For static targets, the StaticDispatcher is used;
  819 | 		 * and for dynamic targets, a DynamicUnadvisedInterceptor is used.
  820 | 		 * If it possible for the method to return {@code this} then a
  821 | 		 * StaticUnadvisedInterceptor is used for static targets - the
  822 | 		 * DynamicUnadvisedInterceptor already considers this.</dd>
  823 | 		 * </dl>
  824 | 		 */
  825 | 		@Override
  826 | 		public int accept(Method method) {
  827 | 			if (AopUtils.isFinalizeMethod(method)) {
  828 | 				logger.trace("Found finalize() method - using NO_OVERRIDE");
  829 | 				return NO_OVERRIDE;
  830 | 			}
  831 | 			if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() &&
  832 | 					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
  833 | 				if (logger.isTraceEnabled()) {
  834 | 					logger.trace("Method is declared on Advised interface: " + method);
  835 | 				}
  836 | 				return DISPATCH_ADVISED;
  837 | 			}
  838 | 			// We must always proxy equals, to direct calls to this.
  839 | 			if (AopUtils.isEqualsMethod(method)) {
  840 | 				if (logger.isTraceEnabled()) {
  841 | 					logger.trace("Found 'equals' method: " + method);
  842 | 				}
  843 | 				return INVOKE_EQUALS;
  844 | 			}
  845 | 			// We must always calculate hashCode based on the proxy.
  846 | 			if (AopUtils.isHashCodeMethod(method)) {
  847 | 				if (logger.isTraceEnabled()) {
  848 | 					logger.trace("Found 'hashCode' method: " + method);
  849 | 				}
  850 | 				return INVOKE_HASHCODE;
  851 | 			}
  852 | 			Class<?> targetClass = this.advised.getTargetClass();
  853 | 			// Proxy is not yet available, but that shouldn't matter.
  854 | 			List<?> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
  855 | 			boolean haveAdvice = !chain.isEmpty();
  856 | 			boolean isStatic = this.advised.getTargetSource().isStatic();
  857 | 			boolean isFrozen = this.advised.isFrozen();
  858 | 			boolean exposeProxy = this.advised.isExposeProxy();
  859 | 			if (haveAdvice || !isFrozen) {
  860 | 				// If exposing the proxy, then AOP_PROXY must be used.
  861 | 				if (exposeProxy) {
  862 | 					if (logger.isTraceEnabled()) {
  863 | 						logger.trace("Must expose proxy on advised method: " + method);
  864 | 					}
  865 | 					return AOP_PROXY;
  866 | 				}
  867 | 				// Check to see if we have fixed interceptor to serve this method.
  868 | 				// Else use the AOP_PROXY.
  869 | 				if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(method)) {
  870 | 					if (logger.isTraceEnabled()) {
  871 | 						logger.trace("Method has advice and optimizations are enabled: " + method);
  872 | 					}
  873 | 					// We know that we are optimizing so we can use the FixedStaticChainInterceptors.
  874 | 					int index = this.fixedInterceptorMap.get(method);
  875 | 					return (index + this.fixedInterceptorOffset);
  876 | 				}
  877 | 				else {
  878 | 					if (logger.isTraceEnabled()) {
  879 | 						logger.trace("Unable to apply any optimizations to advised method: " + method);
  880 | 					}
  881 | 					return AOP_PROXY;
  882 | 				}
  883 | 			}
  884 | 			else {
  885 | 				// See if the return type of the method is outside the class hierarchy of the target type.
  886 | 				// If so we know it never needs to have return type massage and can use a dispatcher.
  887 | 				// If the proxy is being exposed, then must use the interceptor the correct one is already
  888 | 				// configured. If the target is not static, then we cannot use a dispatcher because the
  889 | 				// target needs to be explicitly released after the invocation.
  890 | 				if (exposeProxy || !isStatic) {
  891 | 					return INVOKE_TARGET;
  892 | 				}
  893 | 				Class<?> returnType = method.getReturnType();
  894 | 				if (targetClass != null && returnType.isAssignableFrom(targetClass)) {
  895 | 					if (logger.isTraceEnabled()) {
  896 | 						logger.trace("Method return type is assignable from target type and " +
  897 | 								"may therefore return 'this' - using INVOKE_TARGET: " + method);
  898 | 					}
  899 | 					return INVOKE_TARGET;
  900 | 				}
  901 | 				else {
  902 | 					if (logger.isTraceEnabled()) {
  903 | 						logger.trace("Method return type ensures 'this' cannot be returned - " +
  904 | 								"using DISPATCH_TARGET: " + method);
  905 | 					}
  906 | 					return DISPATCH_TARGET;
  907 | 				}
  908 | 			}
  909 | 		}
  910 | 
  911 | 		@Override
  912 | 		public boolean equals(@Nullable Object other) {
  913 | 			return (this == other || (other instanceof ProxyCallbackFilter that &&
  914 | 					this.advised.getAdvisorKey().equals(that.advised.getAdvisorKey()) &&
  915 | 					AopProxyUtils.equalsProxiedInterfaces(this.advised, that.advised) &&
  916 | 					ObjectUtils.nullSafeEquals(this.advised.getTargetClass(), that.advised.getTargetClass()) &&
  917 | 					this.advised.getTargetSource().isStatic() == that.advised.getTargetSource().isStatic() &&
  918 | 					this.advised.isFrozen() == that.advised.isFrozen() &&
  919 | 					this.advised.isExposeProxy() == that.advised.isExposeProxy() &&
  920 | 					this.advised.isOpaque() == that.advised.isOpaque()));
  921 | 		}
  922 | 
  923 | 		@Override
  924 | 		public int hashCode() {
  925 | 			return this.advised.getAdvisorKey().hashCode();
  926 | 		}
  927 | 	}
  928 | 
  929 | }
```
