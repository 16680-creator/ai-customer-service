<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/AbstractAspectJAdvice.java -->
<!-- 正文引用：[[源码:AbstractAspectJAdvice.java#L起-L止]] ；代码块头部：(src: AbstractAspectJAdvice.java#L起-L止) -->

# AbstractAspectJAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/AbstractAspectJAdvice.java` |
| 行数 | 741 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:59.553Z |

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
   17 | package org.springframework.aop.aspectj;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.ObjectInputStream;
   21 | import java.io.Serializable;
   22 | import java.lang.reflect.InvocationTargetException;
   23 | import java.lang.reflect.Method;
   24 | import java.lang.reflect.Type;
   25 | import java.util.HashMap;
   26 | import java.util.Map;
   27 | 
   28 | import org.aopalliance.aop.Advice;
   29 | import org.aopalliance.intercept.MethodInvocation;
   30 | import org.aspectj.lang.JoinPoint;
   31 | import org.aspectj.lang.ProceedingJoinPoint;
   32 | import org.aspectj.weaver.tools.JoinPointMatch;
   33 | import org.aspectj.weaver.tools.PointcutParameter;
   34 | import org.jspecify.annotations.Nullable;
   35 | 
   36 | import org.springframework.aop.AopInvocationException;
   37 | import org.springframework.aop.MethodMatcher;
   38 | import org.springframework.aop.Pointcut;
   39 | import org.springframework.aop.ProxyMethodInvocation;
   40 | import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
   41 | import org.springframework.aop.support.ComposablePointcut;
   42 | import org.springframework.aop.support.MethodMatchers;
   43 | import org.springframework.aop.support.StaticMethodMatcher;
   44 | import org.springframework.core.DefaultParameterNameDiscoverer;
   45 | import org.springframework.core.ParameterNameDiscoverer;
   46 | import org.springframework.lang.Contract;
   47 | import org.springframework.util.Assert;
   48 | import org.springframework.util.ClassUtils;
   49 | import org.springframework.util.CollectionUtils;
   50 | import org.springframework.util.ReflectionUtils;
   51 | import org.springframework.util.StringUtils;
   52 | 
   53 | /**
   54 |  * Base class for AOP Alliance {@link org.aopalliance.aop.Advice} classes
   55 |  * wrapping an AspectJ aspect or an AspectJ-annotated advice method.
   56 |  *
   57 |  * @author Rod Johnson
   58 |  * @author Adrian Colyer
   59 |  * @author Juergen Hoeller
   60 |  * @author Ramnivas Laddad
   61 |  * @since 2.0
   62 |  */
   63 | @SuppressWarnings("serial")
   64 | public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation, Serializable {
   65 | 
   66 | 	/**
   67 | 	 * Key used in ReflectiveMethodInvocation userAttributes map for the current joinpoint.
   68 | 	 */
   69 | 	protected static final String JOIN_POINT_KEY = JoinPoint.class.getName();
   70 | 
   71 | 
   72 | 	/**
   73 | 	 * Lazily instantiate joinpoint for the current invocation.
   74 | 	 * Requires MethodInvocation to be bound with ExposeInvocationInterceptor.
   75 | 	 * <p>Do not use if access is available to the current ReflectiveMethodInvocation
   76 | 	 * (in an around advice).
   77 | 	 * @return current AspectJ joinpoint, or through an exception if we're not in a
   78 | 	 * Spring AOP invocation.
   79 | 	 */
   80 | 	public static JoinPoint currentJoinPoint() {
   81 | 		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
   82 | 		if (!(mi instanceof ProxyMethodInvocation pmi)) {
   83 | 			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
   84 | 		}
   85 | 		JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
   86 | 		if (jp == null) {
   87 | 			jp = new MethodInvocationProceedingJoinPoint(pmi);
   88 | 			pmi.setUserAttribute(JOIN_POINT_KEY, jp);
   89 | 		}
   90 | 		return jp;
   91 | 	}
   92 | 
   93 | 
   94 | 	private final Class<?> declaringClass;
   95 | 
   96 | 	private final String methodName;
   97 | 
   98 | 	private final Class<?>[] parameterTypes;
   99 | 
  100 | 	protected transient Method aspectJAdviceMethod;
  101 | 
  102 | 	private final AspectJExpressionPointcut pointcut;
  103 | 
  104 | 	private final AspectInstanceFactory aspectInstanceFactory;
  105 | 
  106 | 	/**
  107 | 	 * The name of the aspect (ref bean) in which this advice was defined
  108 | 	 * (used when determining advice precedence so that we can determine
  109 | 	 * whether two pieces of advice come from the same aspect).
  110 | 	 */
  111 | 	private String aspectName = "";
  112 | 
  113 | 	/**
  114 | 	 * The order of declaration of this advice within the aspect.
  115 | 	 */
  116 | 	private int declarationOrder;
  117 | 
  118 | 	/**
  119 | 	 * This will be non-null if the creator of this advice object knows the argument names
  120 | 	 * and sets them explicitly.
  121 | 	 */
  122 | 	private @Nullable String @Nullable [] argumentNames;
  123 | 
  124 | 	/** Non-null if after throwing advice binds the thrown value. */
  125 | 	private @Nullable String throwingName;
  126 | 
  127 | 	/** Non-null if after returning advice binds the return value. */
  128 | 	private @Nullable String returningName;
  129 | 
  130 | 	private Class<?> discoveredReturningType = Object.class;
  131 | 
  132 | 	private Class<?> discoveredThrowingType = Object.class;
  133 | 
  134 | 	/**
  135 | 	 * Index for thisJoinPoint argument (currently only
  136 | 	 * supported at index 0 if present at all).
  137 | 	 */
  138 | 	private int joinPointArgumentIndex = -1;
  139 | 
  140 | 	/**
  141 | 	 * Index for thisJoinPointStaticPart argument (currently only
  142 | 	 * supported at index 0 if present at all).
  143 | 	 */
  144 | 	private int joinPointStaticPartArgumentIndex = -1;
  145 | 
  146 | 	private @Nullable Map<String, Integer> argumentBindings;
  147 | 
  148 | 	private boolean argumentsIntrospected = false;
  149 | 
  150 | 	private @Nullable Type discoveredReturningGenericType;
  151 | 	// Note: Unlike return type, no such generic information is needed for the throwing type,
  152 | 	// since Java doesn't allow exception types to be parameterized.
  153 | 
  154 | 
  155 | 	/**
  156 | 	 * Create a new AbstractAspectJAdvice for the given advice method.
  157 | 	 * @param aspectJAdviceMethod the AspectJ-style advice method
  158 | 	 * @param pointcut the AspectJ expression pointcut
  159 | 	 * @param aspectInstanceFactory the factory for aspect instances
  160 | 	 */
  161 | 	public AbstractAspectJAdvice(
  162 | 			Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
  163 | 
  164 | 		Assert.notNull(aspectJAdviceMethod, "Advice method must not be null");
  165 | 		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
  166 | 		this.methodName = aspectJAdviceMethod.getName();
  167 | 		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
  168 | 		this.aspectJAdviceMethod = aspectJAdviceMethod;
  169 | 		this.pointcut = pointcut;
  170 | 		this.aspectInstanceFactory = aspectInstanceFactory;
  171 | 	}
  172 | 
  173 | 
  174 | 	/**
  175 | 	 * Return the AspectJ-style advice method.
  176 | 	 */
  177 | 	public final Method getAspectJAdviceMethod() {
  178 | 		return this.aspectJAdviceMethod;
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Return the AspectJ expression pointcut.
  183 | 	 */
  184 | 	public final AspectJExpressionPointcut getPointcut() {
  185 | 		calculateArgumentBindings();
  186 | 		return this.pointcut;
  187 | 	}
  188 | 
  189 | 	/**
  190 | 	 * Build a 'safe' pointcut that excludes the AspectJ advice method itself.
  191 | 	 * @return a composable pointcut that builds on the original AspectJ expression pointcut
  192 | 	 * @see #getPointcut()
  193 | 	 */
  194 | 	public final Pointcut buildSafePointcut() {
  195 | 		Pointcut pc = getPointcut();
  196 | 		MethodMatcher safeMethodMatcher = MethodMatchers.intersection(
  197 | 				new AdviceExcludingMethodMatcher(this.aspectJAdviceMethod), pc.getMethodMatcher());
  198 | 		return new ComposablePointcut(pc.getClassFilter(), safeMethodMatcher);
  199 | 	}
  200 | 
  201 | 	/**
  202 | 	 * Return the factory for aspect instances.
  203 | 	 */
  204 | 	public final AspectInstanceFactory getAspectInstanceFactory() {
  205 | 		return this.aspectInstanceFactory;
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Return the ClassLoader for aspect instances.
  210 | 	 */
  211 | 	public final @Nullable ClassLoader getAspectClassLoader() {
  212 | 		return this.aspectInstanceFactory.getAspectClassLoader();
  213 | 	}
  214 | 
  215 | 	@Override
  216 | 	public int getOrder() {
  217 | 		return this.aspectInstanceFactory.getOrder();
  218 | 	}
  219 | 
  220 | 
  221 | 	/**
  222 | 	 * Set the name of the aspect (bean) in which the advice was declared.
  223 | 	 */
  224 | 	public void setAspectName(String name) {
  225 | 		this.aspectName = name;
  226 | 	}
  227 | 
  228 | 	@Override
  229 | 	public String getAspectName() {
  230 | 		return this.aspectName;
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Set the declaration order of this advice within the aspect.
  235 | 	 */
  236 | 	public void setDeclarationOrder(int order) {
  237 | 		this.declarationOrder = order;
  238 | 	}
  239 | 
  240 | 	@Override
  241 | 	public int getDeclarationOrder() {
  242 | 		return this.declarationOrder;
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * Set by the creator of this advice object if the argument names are known.
  247 | 	 * <p>This could be for example because they have been explicitly specified in XML
  248 | 	 * or in an advice annotation.
  249 | 	 * @param argumentNames comma delimited list of argument names
  250 | 	 */
  251 | 	public void setArgumentNames(String argumentNames) {
  252 | 		String[] tokens = StringUtils.commaDelimitedListToStringArray(argumentNames);
  253 | 		setArgumentNamesFromStringArray(tokens);
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Set by the creator of this advice object if the argument names are known.
  258 | 	 * <p>This could be for example because they have been explicitly specified in XML
  259 | 	 * or in an advice annotation.
  260 | 	 * @param argumentNames list of argument names
  261 | 	 */
  262 | 	public void setArgumentNamesFromStringArray(@Nullable String... argumentNames) {
  263 | 		this.argumentNames = new String[argumentNames.length];
  264 | 		for (int i = 0; i < argumentNames.length; i++) {
  265 | 			String argumentName = argumentNames[i];
  266 | 			this.argumentNames[i] = argumentName != null ? argumentName.strip() : null;
  267 | 			if (!isVariableName(this.argumentNames[i])) {
  268 | 				throw new IllegalArgumentException(
  269 | 						"'argumentNames' property of AbstractAspectJAdvice contains an argument name '" +
  270 | 						this.argumentNames[i] + "' that is not a valid Java identifier");
  271 | 			}
  272 | 		}
  273 | 		if (this.aspectJAdviceMethod.getParameterCount() == this.argumentNames.length + 1) {
  274 | 			// May need to add implicit join point arg name...
  275 | 			for (int i = 0; i < this.aspectJAdviceMethod.getParameterCount(); i++) {
  276 | 				Class<?> argType = this.aspectJAdviceMethod.getParameterTypes()[i];
  277 | 				if (argType == JoinPoint.class ||
  278 | 						argType == ProceedingJoinPoint.class ||
  279 | 						argType == JoinPoint.StaticPart.class) {
  280 | 					@Nullable String[] oldNames = this.argumentNames;
  281 | 				this.argumentNames = new String[oldNames.length + 1];
  282 | 				System.arraycopy(oldNames, 0, this.argumentNames, 0, i);
  283 | 					this.argumentNames[i] = "THIS_JOIN_POINT";
  284 | 					System.arraycopy(oldNames, i, this.argumentNames, i + 1, oldNames.length - i);
  285 | 					break;
  286 | 				}
  287 | 			}
  288 | 		}
  289 | 	}
  290 | 
  291 | 	public void setReturningName(String name) {
  292 | 		throw new UnsupportedOperationException("Only afterReturning advice can be used to bind a return value");
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * We need to hold the returning name at this level for argument binding calculations,
  297 | 	 * this method allows the afterReturning advice subclass to set the name.
  298 | 	 */
  299 | 	protected void setReturningNameNoCheck(String name) {
  300 | 		// name could be a variable or a type...
  301 | 		if (isVariableName(name)) {
  302 | 			this.returningName = name;
  303 | 		}
  304 | 		else {
  305 | 			// assume a type
  306 | 			try {
  307 | 				this.discoveredReturningType = ClassUtils.forName(name, getAspectClassLoader());
  308 | 			}
  309 | 			catch (Throwable ex) {
  310 | 				throw new IllegalArgumentException("Returning name '" + name +
  311 | 						"' is neither a valid argument name nor the fully-qualified " +
  312 | 						"name of a Java type on the classpath. Root cause: " + ex);
  313 | 			}
  314 | 		}
  315 | 	}
  316 | 
  317 | 	protected Class<?> getDiscoveredReturningType() {
  318 | 		return this.discoveredReturningType;
  319 | 	}
  320 | 
  321 | 	protected @Nullable Type getDiscoveredReturningGenericType() {
  322 | 		return this.discoveredReturningGenericType;
  323 | 	}
  324 | 
  325 | 	public void setThrowingName(String name) {
  326 | 		throw new UnsupportedOperationException("Only afterThrowing advice can be used to bind a thrown exception");
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * We need to hold the throwing name at this level for argument binding calculations,
  331 | 	 * this method allows the afterThrowing advice subclass to set the name.
  332 | 	 */
  333 | 	protected void setThrowingNameNoCheck(String name) {
  334 | 		// name could be a variable or a type...
  335 | 		if (isVariableName(name)) {
  336 | 			this.throwingName = name;
  337 | 		}
  338 | 		else {
  339 | 			// assume a type
  340 | 			try {
  341 | 				this.discoveredThrowingType = ClassUtils.forName(name, getAspectClassLoader());
  342 | 			}
  343 | 			catch (Throwable ex) {
  344 | 				throw new IllegalArgumentException("Throwing name '" + name +
  345 | 						"' is neither a valid argument name nor the fully-qualified " +
  346 | 						"name of a Java type on the classpath. Root cause: " + ex);
  347 | 			}
  348 | 		}
  349 | 	}
  350 | 
  351 | 	protected Class<?> getDiscoveredThrowingType() {
  352 | 		return this.discoveredThrowingType;
  353 | 	}
  354 | 
  355 | 	@Contract("null -> false")
  356 | 	private static boolean isVariableName(@Nullable String name) {
  357 | 		return AspectJProxyUtils.isVariableName(name);
  358 | 	}
  359 | 
  360 | 
  361 | 	/**
  362 | 	 * Do as much work as we can as part of the set-up so that argument binding
  363 | 	 * on subsequent advice invocations can be as fast as possible.
  364 | 	 * <p>If the first argument is of type JoinPoint or ProceedingJoinPoint then we
  365 | 	 * pass a JoinPoint in that position (ProceedingJoinPoint for around advice).
  366 | 	 * <p>If the first argument is of type {@code JoinPoint.StaticPart}
  367 | 	 * then we pass a {@code JoinPoint.StaticPart} in that position.
  368 | 	 * <p>Remaining arguments have to be bound by pointcut evaluation at
  369 | 	 * a given join point. We will get back a map from argument name to
  370 | 	 * value. We need to calculate which advice parameter needs to be bound
  371 | 	 * to which argument name. There are multiple strategies for determining
  372 | 	 * this binding, which are arranged in a ChainOfResponsibility.
  373 | 	 */
  374 | 	public final void calculateArgumentBindings() {
  375 | 		// The simple case... nothing to bind.
  376 | 		if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
  377 | 			return;
  378 | 		}
  379 | 
  380 | 		int numUnboundArgs = this.parameterTypes.length;
  381 | 		Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
  382 | 		if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0]) ||
  383 | 				maybeBindJoinPointStaticPart(parameterTypes[0])) {
  384 | 			numUnboundArgs--;
  385 | 		}
  386 | 
  387 | 		if (numUnboundArgs > 0) {
  388 | 			// need to bind arguments by name as returned from the pointcut match
  389 | 			bindArgumentsByName(numUnboundArgs);
  390 | 		}
  391 | 
  392 | 		this.argumentsIntrospected = true;
  393 | 	}
  394 | 
  395 | 	private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
  396 | 		if (JoinPoint.class == candidateParameterType) {
  397 | 			this.joinPointArgumentIndex = 0;
  398 | 			return true;
  399 | 		}
  400 | 		else {
  401 | 			return false;
  402 | 		}
  403 | 	}
  404 | 
  405 | 	private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
  406 | 		if (ProceedingJoinPoint.class == candidateParameterType) {
  407 | 			if (!supportsProceedingJoinPoint()) {
  408 | 				throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");
  409 | 			}
  410 | 			this.joinPointArgumentIndex = 0;
  411 | 			return true;
  412 | 		}
  413 | 		else {
  414 | 			return false;
  415 | 		}
  416 | 	}
  417 | 
  418 | 	protected boolean supportsProceedingJoinPoint() {
  419 | 		return false;
  420 | 	}
  421 | 
  422 | 	private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType) {
  423 | 		if (JoinPoint.StaticPart.class == candidateParameterType) {
  424 | 			this.joinPointStaticPartArgumentIndex = 0;
  425 | 			return true;
  426 | 		}
  427 | 		else {
  428 | 			return false;
  429 | 		}
  430 | 	}
  431 | 
  432 | 	private void bindArgumentsByName(int numArgumentsExpectingToBind) {
  433 | 		if (this.argumentNames == null) {
  434 | 			this.argumentNames = createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);
  435 | 		}
  436 | 		if (this.argumentNames != null) {
  437 | 			// We have been able to determine the arg names.
  438 | 			bindExplicitArguments(numArgumentsExpectingToBind);
  439 | 		}
  440 | 		else {
  441 | 			throw new IllegalStateException("Advice method [" + this.aspectJAdviceMethod.getName() + "] " +
  442 | 					"requires " + numArgumentsExpectingToBind + " arguments to be bound by name, but " +
  443 | 					"the argument names were not specified and could not be discovered.");
  444 | 		}
  445 | 	}
  446 | 
  447 | 	/**
  448 | 	 * Create a ParameterNameDiscoverer to be used for argument binding.
  449 | 	 * <p>The default implementation creates a {@link DefaultParameterNameDiscoverer}
  450 | 	 * and adds a specifically configured {@link AspectJAdviceParameterNameDiscoverer}.
  451 | 	 */
  452 | 	protected ParameterNameDiscoverer createParameterNameDiscoverer() {
  453 | 		// We need to discover them, or if that fails, guess,
  454 | 		// and if we can't guess with 100% accuracy, fail.
  455 | 		DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
  456 | 		AspectJAdviceParameterNameDiscoverer adviceParameterNameDiscoverer =
  457 | 				new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
  458 | 		adviceParameterNameDiscoverer.setReturningName(this.returningName);
  459 | 		adviceParameterNameDiscoverer.setThrowingName(this.throwingName);
  460 | 		// Last in chain, so if we're called and we fail, that's bad...
  461 | 		adviceParameterNameDiscoverer.setRaiseExceptions(true);
  462 | 		discoverer.addDiscoverer(adviceParameterNameDiscoverer);
  463 | 		return discoverer;
  464 | 	}
  465 | 
  466 | 	@SuppressWarnings("NullAway") // Dataflow analysis limitation
  467 | 	private void bindExplicitArguments(int numArgumentsLeftToBind) {
  468 | 		Assert.state(this.argumentNames != null, "No argument names available");
  469 | 		this.argumentBindings = new HashMap<>();
  470 | 
  471 | 		int numExpectedArgumentNames = this.aspectJAdviceMethod.getParameterCount();
  472 | 		if (this.argumentNames.length != numExpectedArgumentNames) {
  473 | 			throw new IllegalStateException("Expecting to find " + numExpectedArgumentNames +
  474 | 					" arguments to bind by name in advice, but actually found " +
  475 | 					this.argumentNames.length + " arguments.");
  476 | 		}
  477 | 
  478 | 		// So we match in number...
  479 | 		int argumentIndexOffset = this.parameterTypes.length - numArgumentsLeftToBind;
  480 | 		for (int i = argumentIndexOffset; i < this.argumentNames.length; i++) {
  481 | 			this.argumentBindings.put(this.argumentNames[i], i);
  482 | 		}
  483 | 
  484 | 		// Check that returning and throwing were in the argument names list if
  485 | 		// specified, and find the discovered argument types.
  486 | 		if (this.returningName != null) {
  487 | 			if (!this.argumentBindings.containsKey(this.returningName)) {
  488 | 				throw new IllegalStateException("Returning argument name '" + this.returningName +
  489 | 						"' was not bound in advice arguments");
  490 | 			}
  491 | 			else {
  492 | 				Integer index = this.argumentBindings.get(this.returningName);
  493 | 				this.discoveredReturningType = this.aspectJAdviceMethod.getParameterTypes()[index];
  494 | 				this.discoveredReturningGenericType = this.aspectJAdviceMethod.getGenericParameterTypes()[index];
  495 | 			}
  496 | 		}
  497 | 		if (this.throwingName != null) {
  498 | 			if (!this.argumentBindings.containsKey(this.throwingName)) {
  499 | 				throw new IllegalStateException("Throwing argument name '" + this.throwingName +
  500 | 						"' was not bound in advice arguments");
  501 | 			}
  502 | 			else {
  503 | 				Integer index = this.argumentBindings.get(this.throwingName);
  504 | 				this.discoveredThrowingType = this.aspectJAdviceMethod.getParameterTypes()[index];
  505 | 			}
  506 | 		}
  507 | 
  508 | 		// configure the pointcut expression accordingly.
  509 | 		configurePointcutParameters(this.argumentNames, argumentIndexOffset);
  510 | 	}
  511 | 
  512 | 	/**
  513 | 	 * All parameters from argumentIndexOffset onwards are candidates for
  514 | 	 * pointcut parameters - but returning and throwing vars are handled differently
  515 | 	 * and must be removed from the list if present.
  516 | 	 */
  517 | 	private void configurePointcutParameters(String[] argumentNames, int argumentIndexOffset) {
  518 | 		int numParametersToRemove = argumentIndexOffset;
  519 | 		if (this.returningName != null) {
  520 | 			numParametersToRemove++;
  521 | 		}
  522 | 		if (this.throwingName != null) {
  523 | 			numParametersToRemove++;
  524 | 		}
  525 | 		String[] pointcutParameterNames = new String[argumentNames.length - numParametersToRemove];
  526 | 		Class<?>[] pointcutParameterTypes = new Class<?>[pointcutParameterNames.length];
  527 | 		Class<?>[] methodParameterTypes = this.aspectJAdviceMethod.getParameterTypes();
  528 | 
  529 | 		int index = 0;
  530 | 		for (int i = 0; i < argumentNames.length; i++) {
  531 | 			if (i < argumentIndexOffset) {
  532 | 				continue;
  533 | 			}
  534 | 			if (argumentNames[i].equals(this.returningName) ||
  535 | 				argumentNames[i].equals(this.throwingName)) {
  536 | 				continue;
  537 | 			}
  538 | 			pointcutParameterNames[index] = argumentNames[i];
  539 | 			pointcutParameterTypes[index] = methodParameterTypes[i];
  540 | 			index++;
  541 | 		}
  542 | 
  543 | 		this.pointcut.setParameterNames(pointcutParameterNames);
  544 | 		this.pointcut.setParameterTypes(pointcutParameterTypes);
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Take the arguments at the method execution join point and output a set of arguments
  549 | 	 * to the advice method.
  550 | 	 * @param jp the current JoinPoint
  551 | 	 * @param jpMatch the join point match that matched this execution join point
  552 | 	 * @param returnValue the return value from the method execution (may be null)
  553 | 	 * @param ex the exception thrown by the method execution (may be null)
  554 | 	 * @return the empty array if there are no arguments
  555 | 	 */
  556 | 	protected @Nullable Object[] argBinding(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
  557 | 			@Nullable Object returnValue, @Nullable Throwable ex) {
  558 | 
  559 | 		calculateArgumentBindings();
  560 | 
  561 | 		// AMC start
  562 | 		@Nullable Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
  563 | 		int numBound = 0;
  564 | 
  565 | 		if (this.joinPointArgumentIndex != -1) {
  566 | 			adviceInvocationArgs[this.joinPointArgumentIndex] = jp;
  567 | 			numBound++;
  568 | 		}
  569 | 		else if (this.joinPointStaticPartArgumentIndex != -1) {
  570 | 			adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
  571 | 			numBound++;
  572 | 		}
  573 | 
  574 | 		if (!CollectionUtils.isEmpty(this.argumentBindings)) {
  575 | 			// binding from pointcut match
  576 | 			if (jpMatch != null) {
  577 | 				PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
  578 | 				for (PointcutParameter parameter : parameterBindings) {
  579 | 					String name = parameter.getName();
  580 | 					Integer index = this.argumentBindings.get(name);
  581 | 					Assert.state(index != null, "Index must not be null");
  582 | 					adviceInvocationArgs[index] = parameter.getBinding();
  583 | 					numBound++;
  584 | 				}
  585 | 			}
  586 | 			// binding from returning clause
  587 | 			if (this.returningName != null) {
  588 | 				Integer index = this.argumentBindings.get(this.returningName);
  589 | 				Assert.state(index != null, "Index must not be null");
  590 | 				adviceInvocationArgs[index] = returnValue;
  591 | 				numBound++;
  592 | 			}
  593 | 			// binding from thrown exception
  594 | 			if (this.throwingName != null) {
  595 | 				Integer index = this.argumentBindings.get(this.throwingName);
  596 | 				Assert.state(index != null, "Index must not be null");
  597 | 				adviceInvocationArgs[index] = ex;
  598 | 				numBound++;
  599 | 			}
  600 | 		}
  601 | 
  602 | 		if (numBound != this.parameterTypes.length) {
  603 | 			throw new IllegalStateException("Required to bind " + this.parameterTypes.length +
  604 | 					" arguments, but only bound " + numBound + " (JoinPointMatch " +
  605 | 					(jpMatch == null ? "was NOT" : "WAS") + " bound in invocation)");
  606 | 		}
  607 | 
  608 | 		return adviceInvocationArgs;
  609 | 	}
  610 | 
  611 | 
  612 | 	/**
  613 | 	 * Invoke the advice method.
  614 | 	 * @param jpMatch the JoinPointMatch that matched this execution join point
  615 | 	 * @param returnValue the return value from the method execution (may be null)
  616 | 	 * @param ex the exception thrown by the method execution (may be null)
  617 | 	 * @return the invocation result
  618 | 	 * @throws Throwable in case of invocation failure
  619 | 	 */
  620 | 	protected @Nullable Object invokeAdviceMethod(@Nullable JoinPointMatch jpMatch,
  621 | 			@Nullable Object returnValue, @Nullable Throwable ex) throws Throwable {
  622 | 
  623 | 		return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
  624 | 	}
  625 | 
  626 | 	// As above, but in this case we are given the join point.
  627 | 	protected @Nullable Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
  628 | 			@Nullable Object returnValue, @Nullable Throwable t) throws Throwable {
  629 | 
  630 | 		return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
  631 | 	}
  632 | 
  633 | 	protected @Nullable Object invokeAdviceMethodWithGivenArgs(@Nullable Object[] args) throws Throwable {
  634 | 		@Nullable Object[] actualArgs = args;
  635 | 		if (this.aspectJAdviceMethod.getParameterCount() == 0) {
  636 | 			actualArgs = null;
  637 | 		}
  638 | 		Object aspectInstance = this.aspectInstanceFactory.getAspectInstance();
  639 | 		if (aspectInstance.equals(null)) {
  640 | 			// Possibly a NullBean -> simply proceed if necessary.
  641 | 			if (getJoinPoint() instanceof ProceedingJoinPoint pjp) {
  642 | 				return pjp.proceed();
  643 | 			}
  644 | 			return null;
  645 | 		}
  646 | 		try {
  647 | 			ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
  648 | 			return this.aspectJAdviceMethod.invoke(aspectInstance, actualArgs);
  649 | 		}
  650 | 		catch (IllegalArgumentException ex) {
  651 | 			throw new AopInvocationException("Mismatch on arguments to advice method [" +
  652 | 					this.aspectJAdviceMethod + "]; pointcut expression [" +
  653 | 					this.pointcut.getPointcutExpression() + "]", ex);
  654 | 		}
  655 | 		catch (InvocationTargetException ex) {
  656 | 			throw ex.getTargetException();
  657 | 		}
  658 | 	}
  659 | 
  660 | 	/**
  661 | 	 * Overridden in around advice to return proceeding join point.
  662 | 	 */
  663 | 	protected JoinPoint getJoinPoint() {
  664 | 		return currentJoinPoint();
  665 | 	}
  666 | 
  667 | 	/**
  668 | 	 * Get the current join point match at the join point we are being dispatched on.
  669 | 	 */
  670 | 	protected @Nullable JoinPointMatch getJoinPointMatch() {
  671 | 		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
  672 | 		if (!(mi instanceof ProxyMethodInvocation pmi)) {
  673 | 			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
  674 | 		}
  675 | 		return getJoinPointMatch(pmi);
  676 | 	}
  677 | 
  678 | 	// Note: We can't use JoinPointMatch.getClass().getName() as the key, since
  679 | 	// Spring AOP does all the matching at a join point, and then all the invocations.
  680 | 	// Under this scenario, if we just use JoinPointMatch as the key, then
  681 | 	// 'last man wins' which is not what we want at all.
  682 | 	// Using the expression is guaranteed to be safe, since 2 identical expressions
  683 | 	// are guaranteed to bind in exactly the same way.
  684 | 	protected @Nullable JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi) {
  685 | 		String expression = this.pointcut.getExpression();
  686 | 		return (expression != null ? (JoinPointMatch) pmi.getUserAttribute(expression) : null);
  687 | 	}
  688 | 
  689 | 
  690 | 	@Override
  691 | 	public String toString() {
  692 | 		return getClass().getName() + ": advice method [" + this.aspectJAdviceMethod + "]; " +
  693 | 				"aspect name '" + this.aspectName + "'";
  694 | 	}
  695 | 
  696 | 	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
  697 | 		inputStream.defaultReadObject();
  698 | 		try {
  699 | 			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
  700 | 		}
  701 | 		catch (NoSuchMethodException ex) {
  702 | 			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
  703 | 		}
  704 | 	}
  705 | 
  706 | 
  707 | 	/**
  708 | 	 * MethodMatcher that excludes the specified advice method.
  709 | 	 * @see AbstractAspectJAdvice#buildSafePointcut()
  710 | 	 */
  711 | 	private static class AdviceExcludingMethodMatcher extends StaticMethodMatcher {
  712 | 
  713 | 		private final Method adviceMethod;
  714 | 
  715 | 		public AdviceExcludingMethodMatcher(Method adviceMethod) {
  716 | 			this.adviceMethod = adviceMethod;
  717 | 		}
  718 | 
  719 | 		@Override
  720 | 		public boolean matches(Method method, Class<?> targetClass) {
  721 | 			return !this.adviceMethod.equals(method);
  722 | 		}
  723 | 
  724 | 		@Override
  725 | 		public boolean equals(@Nullable Object other) {
  726 | 			return (this == other || (other instanceof AdviceExcludingMethodMatcher that &&
  727 | 					this.adviceMethod.equals(that.adviceMethod)));
  728 | 		}
  729 | 
  730 | 		@Override
  731 | 		public int hashCode() {
  732 | 			return this.adviceMethod.hashCode();
  733 | 		}
  734 | 
  735 | 		@Override
  736 | 		public String toString() {
  737 | 			return getClass().getName() + ": " + this.adviceMethod;
  738 | 		}
  739 | 	}
  740 | 
  741 | }
```
