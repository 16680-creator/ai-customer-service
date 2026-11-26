<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/AspectJExpressionPointcut.java -->
<!-- 正文引用：[[源码:AspectJExpressionPointcut.java#L起-L止]] ；代码块头部：(src: AspectJExpressionPointcut.java#L起-L止) -->

# AspectJExpressionPointcut.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/AspectJExpressionPointcut.java` |
| 行数 | 731 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:59.736Z |

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
   19 | import java.lang.reflect.Field;
   20 | import java.lang.reflect.Method;
   21 | import java.lang.reflect.Proxy;
   22 | import java.util.Arrays;
   23 | import java.util.Set;
   24 | 
   25 | import org.aopalliance.intercept.MethodInvocation;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.aspectj.weaver.patterns.NamePattern;
   29 | import org.aspectj.weaver.reflect.ReflectionWorld.ReflectionWorldException;
   30 | import org.aspectj.weaver.reflect.ShadowMatchImpl;
   31 | import org.aspectj.weaver.tools.ContextBasedMatcher;
   32 | import org.aspectj.weaver.tools.FuzzyBoolean;
   33 | import org.aspectj.weaver.tools.JoinPointMatch;
   34 | import org.aspectj.weaver.tools.MatchingContext;
   35 | import org.aspectj.weaver.tools.PointcutDesignatorHandler;
   36 | import org.aspectj.weaver.tools.PointcutExpression;
   37 | import org.aspectj.weaver.tools.PointcutParameter;
   38 | import org.aspectj.weaver.tools.PointcutParser;
   39 | import org.aspectj.weaver.tools.PointcutPrimitive;
   40 | import org.aspectj.weaver.tools.ShadowMatch;
   41 | import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;
   42 | import org.jspecify.annotations.Nullable;
   43 | 
   44 | import org.springframework.aop.ClassFilter;
   45 | import org.springframework.aop.IntroductionAwareMethodMatcher;
   46 | import org.springframework.aop.MethodMatcher;
   47 | import org.springframework.aop.ProxyMethodInvocation;
   48 | import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
   49 | import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
   50 | import org.springframework.aop.support.AbstractExpressionPointcut;
   51 | import org.springframework.aop.support.AopUtils;
   52 | import org.springframework.beans.factory.BeanFactory;
   53 | import org.springframework.beans.factory.BeanFactoryAware;
   54 | import org.springframework.beans.factory.BeanFactoryUtils;
   55 | import org.springframework.beans.factory.FactoryBean;
   56 | import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
   57 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   58 | import org.springframework.util.Assert;
   59 | import org.springframework.util.ClassUtils;
   60 | import org.springframework.util.ObjectUtils;
   61 | import org.springframework.util.StringUtils;
   62 | 
   63 | /**
   64 |  * Spring {@link org.springframework.aop.Pointcut} implementation
   65 |  * that uses the AspectJ weaver to evaluate a pointcut expression.
   66 |  *
   67 |  * <p>The pointcut expression value is an AspectJ expression. This can
   68 |  * reference other pointcuts and use composition and other operations.
   69 |  *
   70 |  * <p>Naturally, as this is to be processed by Spring AOP's proxy-based model,
   71 |  * only method execution pointcuts are supported.
   72 |  *
   73 |  * @author Rob Harrop
   74 |  * @author Adrian Colyer
   75 |  * @author Rod Johnson
   76 |  * @author Juergen Hoeller
   77 |  * @author Ramnivas Laddad
   78 |  * @author Dave Syer
   79 |  * @author Yanming Zhou
   80 |  * @since 2.0
   81 |  */
   82 | @SuppressWarnings("serial")
   83 | public class AspectJExpressionPointcut extends AbstractExpressionPointcut
   84 | 		implements ClassFilter, IntroductionAwareMethodMatcher, BeanFactoryAware {
   85 | 
   86 | 	private static final String AJC_MAGIC = "ajc$";
   87 | 
   88 | 	private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = Set.of(
   89 | 			PointcutPrimitive.EXECUTION,
   90 | 			PointcutPrimitive.ARGS,
   91 | 			PointcutPrimitive.REFERENCE,
   92 | 			PointcutPrimitive.THIS,
   93 | 			PointcutPrimitive.TARGET,
   94 | 			PointcutPrimitive.WITHIN,
   95 | 			PointcutPrimitive.AT_ANNOTATION,
   96 | 			PointcutPrimitive.AT_WITHIN,
   97 | 			PointcutPrimitive.AT_ARGS,
   98 | 			PointcutPrimitive.AT_TARGET);
   99 | 
  100 | 	private static final Log logger = LogFactory.getLog(AspectJExpressionPointcut.class);
  101 | 
  102 | 	private @Nullable Class<?> pointcutDeclarationScope;
  103 | 
  104 | 	private boolean aspectCompiledByAjc;
  105 | 
  106 | 	private String[] pointcutParameterNames = new String[0];
  107 | 
  108 | 	private Class<?>[] pointcutParameterTypes = new Class<?>[0];
  109 | 
  110 | 	private @Nullable BeanFactory beanFactory;
  111 | 
  112 | 	private transient volatile @Nullable ClassLoader pointcutClassLoader;
  113 | 
  114 | 	private transient volatile @Nullable PointcutExpression pointcutExpression;
  115 | 
  116 | 	private transient volatile boolean pointcutParsingFailed;
  117 | 
  118 | 
  119 | 	/**
  120 | 	 * Create a new default AspectJExpressionPointcut.
  121 | 	 */
  122 | 	public AspectJExpressionPointcut() {
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create a new AspectJExpressionPointcut with the given settings.
  127 | 	 * @param declarationScope the declaration scope for the pointcut
  128 | 	 * @param paramNames the parameter names for the pointcut
  129 | 	 * @param paramTypes the parameter types for the pointcut
  130 | 	 */
  131 | 	public AspectJExpressionPointcut(Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
  132 | 		setPointcutDeclarationScope(declarationScope);
  133 | 		if (paramNames.length != paramTypes.length) {
  134 | 			throw new IllegalStateException(
  135 | 					"Number of pointcut parameter names must match number of pointcut parameter types");
  136 | 		}
  137 | 		this.pointcutParameterNames = paramNames;
  138 | 		this.pointcutParameterTypes = paramTypes;
  139 | 	}
  140 | 
  141 | 
  142 | 	/**
  143 | 	 * Set the declaration scope for the pointcut.
  144 | 	 */
  145 | 	public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
  146 | 		this.pointcutDeclarationScope = pointcutDeclarationScope;
  147 | 		this.aspectCompiledByAjc = compiledByAjc(pointcutDeclarationScope);
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Set the parameter names for the pointcut.
  152 | 	 */
  153 | 	public void setParameterNames(String... names) {
  154 | 		this.pointcutParameterNames = names;
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Set the parameter types for the pointcut.
  159 | 	 */
  160 | 	public void setParameterTypes(Class<?>... types) {
  161 | 		this.pointcutParameterTypes = types;
  162 | 	}
  163 | 
  164 | 	@Override
  165 | 	public void setBeanFactory(BeanFactory beanFactory) {
  166 | 		this.beanFactory = beanFactory;
  167 | 	}
  168 | 
  169 | 
  170 | 	@Override
  171 | 	public ClassFilter getClassFilter() {
  172 | 		checkExpression();
  173 | 		return this;
  174 | 	}
  175 | 
  176 | 	@Override
  177 | 	public MethodMatcher getMethodMatcher() {
  178 | 		checkExpression();
  179 | 		return this;
  180 | 	}
  181 | 
  182 | 
  183 | 	/**
  184 | 	 * Check whether this pointcut is ready to match.
  185 | 	 */
  186 | 	private void checkExpression() {
  187 | 		if (getExpression() == null) {
  188 | 			throw new IllegalStateException("Must set property 'expression' before attempting to match");
  189 | 		}
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Lazily build the underlying AspectJ pointcut expression.
  194 | 	 */
  195 | 	private PointcutExpression obtainPointcutExpression() {
  196 | 		PointcutExpression pointcutExpression = this.pointcutExpression;
  197 | 		if (pointcutExpression == null) {
  198 | 			ClassLoader pointcutClassLoader = determinePointcutClassLoader();
  199 | 			pointcutExpression = buildPointcutExpression(pointcutClassLoader);
  200 | 			this.pointcutClassLoader = pointcutClassLoader;
  201 | 			this.pointcutExpression = pointcutExpression;
  202 | 		}
  203 | 		return pointcutExpression;
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Determine the ClassLoader to use for pointcut evaluation.
  208 | 	 */
  209 | 	private @Nullable ClassLoader determinePointcutClassLoader() {
  210 | 		if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
  211 | 			return cbf.getBeanClassLoader();
  212 | 		}
  213 | 		if (this.pointcutDeclarationScope != null) {
  214 | 			return this.pointcutDeclarationScope.getClassLoader();
  215 | 		}
  216 | 		return ClassUtils.getDefaultClassLoader();
  217 | 	}
  218 | 
  219 | 	/**
  220 | 	 * Build the underlying AspectJ pointcut expression.
  221 | 	 */
  222 | 	private PointcutExpression buildPointcutExpression(@Nullable ClassLoader classLoader) {
  223 | 		PointcutParser parser = initializePointcutParser(classLoader);
  224 | 		PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
  225 | 		for (int i = 0; i < pointcutParameters.length; i++) {
  226 | 			pointcutParameters[i] = parser.createPointcutParameter(
  227 | 					this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
  228 | 		}
  229 | 		return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()),
  230 | 				this.pointcutDeclarationScope, pointcutParameters);
  231 | 	}
  232 | 
  233 | 	private String resolveExpression() {
  234 | 		String expression = getExpression();
  235 | 		Assert.state(expression != null, "No expression set");
  236 | 		return expression;
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Initialize the underlying AspectJ pointcut parser.
  241 | 	 */
  242 | 	private PointcutParser initializePointcutParser(@Nullable ClassLoader classLoader) {
  243 | 		PointcutParser parser = PointcutParser
  244 | 				.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
  245 | 						SUPPORTED_PRIMITIVES, classLoader);
  246 | 		parser.registerPointcutDesignatorHandler(new BeanPointcutDesignatorHandler());
  247 | 		return parser;
  248 | 	}
  249 | 
  250 | 
  251 | 	/**
  252 | 	 * If a pointcut expression has been specified in XML, the user cannot
  253 | 	 * write "and" as "&&" (though {@code &amp;&amp;} will work).
  254 | 	 * <p>We also allow "and" between two pointcut sub-expressions.
  255 | 	 * <p>This method converts back to {@code &&} for the AspectJ pointcut parser.
  256 | 	 */
  257 | 	private String replaceBooleanOperators(String pcExpr) {
  258 | 		String result = StringUtils.replace(pcExpr, " and ", " && ");
  259 | 		result = StringUtils.replace(result, " or ", " || ");
  260 | 		result = StringUtils.replace(result, " not ", " ! ");
  261 | 		return result;
  262 | 	}
  263 | 
  264 | 
  265 | 	/**
  266 | 	 * Return the underlying AspectJ pointcut expression.
  267 | 	 */
  268 | 	public PointcutExpression getPointcutExpression() {
  269 | 		return obtainPointcutExpression();
  270 | 	}
  271 | 
  272 | 	@Override
  273 | 	public boolean matches(Class<?> targetClass) {
  274 | 		if (this.pointcutParsingFailed) {
  275 | 			// Pointcut parsing failed before below -> avoid trying again.
  276 | 			return false;
  277 | 		}
  278 | 		if (this.aspectCompiledByAjc && compiledByAjc(targetClass)) {
  279 | 			// ajc-compiled aspect class for ajc-compiled target class -> already weaved.
  280 | 			return false;
  281 | 		}
  282 | 
  283 | 		try {
  284 | 			try {
  285 | 				return obtainPointcutExpression().couldMatchJoinPointsInType(targetClass);
  286 | 			}
  287 | 			catch (ReflectionWorldException ex) {
  288 | 				logger.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
  289 | 				// Actually this is still a "maybe" - treat the pointcut as dynamic if we don't know enough yet
  290 | 				PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
  291 | 				if (fallbackExpression != null) {
  292 | 					return fallbackExpression.couldMatchJoinPointsInType(targetClass);
  293 | 				}
  294 | 			}
  295 | 		}
  296 | 		catch (IllegalArgumentException | IllegalStateException | UnsupportedPointcutPrimitiveException ex) {
  297 | 			this.pointcutParsingFailed = true;
  298 | 			if (logger.isDebugEnabled()) {
  299 | 				logger.debug("Pointcut parser rejected expression [" + getExpression() + "]: " + ex);
  300 | 			}
  301 | 		}
  302 | 		catch (Throwable ex) {
  303 | 			logger.debug("PointcutExpression matching rejected target class", ex);
  304 | 		}
  305 | 		return false;
  306 | 	}
  307 | 
  308 | 	@Override
  309 | 	public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
  310 | 		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);
  311 | 
  312 | 		// Special handling for this, target, @this, @target, @annotation
  313 | 		// in Spring - we can optimize since we know we have exactly this class,
  314 | 		// and there will never be matching subclass at runtime.
  315 | 		if (shadowMatch.alwaysMatches()) {
  316 | 			return true;
  317 | 		}
  318 | 		else if (shadowMatch.neverMatches()) {
  319 | 			return false;
  320 | 		}
  321 | 		else {
  322 | 			// the maybe case
  323 | 			if (hasIntroductions) {
  324 | 				return true;
  325 | 			}
  326 | 			// A match test returned maybe - if there are any subtype sensitive variables
  327 | 			// involved in the test (this, target, at_this, at_target, at_annotation) then
  328 | 			// we say this is not a match as in Spring there will never be a different
  329 | 			// runtime subtype.
  330 | 			RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
  331 | 			return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
  332 | 		}
  333 | 	}
  334 | 
  335 | 	@Override
  336 | 	public boolean matches(Method method, Class<?> targetClass) {
  337 | 		return matches(method, targetClass, false);
  338 | 	}
  339 | 
  340 | 	@Override
  341 | 	public boolean isRuntime() {
  342 | 		return obtainPointcutExpression().mayNeedDynamicTest();
  343 | 	}
  344 | 
  345 | 	@Override
  346 | 	public boolean matches(Method method, Class<?> targetClass, @Nullable Object... args) {
  347 | 		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);
  348 | 
  349 | 		// Bind Spring AOP proxy to AspectJ "this" and Spring AOP target to AspectJ target,
  350 | 		// consistent with return of MethodInvocationProceedingJoinPoint
  351 | 		ProxyMethodInvocation pmi = null;
  352 | 		Object targetObject = null;
  353 | 		Object thisObject = null;
  354 | 		try {
  355 | 			MethodInvocation curr = ExposeInvocationInterceptor.currentInvocation();
  356 | 			if (curr.getMethod() == method) {
  357 | 				targetObject = curr.getThis();
  358 | 				if (!(curr instanceof ProxyMethodInvocation currPmi)) {
  359 | 					throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + curr);
  360 | 				}
  361 | 				pmi = currPmi;
  362 | 				thisObject = pmi.getProxy();
  363 | 			}
  364 | 		}
  365 | 		catch (IllegalStateException ex) {
  366 | 			// No current invocation...
  367 | 			if (logger.isDebugEnabled()) {
  368 | 				logger.debug("Could not access current invocation - matching with limited context: " + ex);
  369 | 			}
  370 | 		}
  371 | 
  372 | 		try {
  373 | 			JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, args);
  374 | 
  375 | 			/*
  376 | 			 * Do a final check to see if any this(TYPE) kind of residue match. For
  377 | 			 * this purpose, we use the original method's (proxy method's) shadow to
  378 | 			 * ensure that 'this' is correctly checked against. Without this check,
  379 | 			 * we get incorrect match on this(TYPE) where TYPE matches the target
  380 | 			 * type but not 'this' (as would be the case of JDK dynamic proxies).
  381 | 			 * <p>See SPR-2979 for the original bug.
  382 | 			 */
  383 | 			if (pmi != null && thisObject != null) {  // there is a current invocation
  384 | 				RuntimeTestWalker originalMethodResidueTest = getRuntimeTestWalker(getShadowMatch(method, method));
  385 | 				if (!originalMethodResidueTest.testThisInstanceOfResidue(thisObject.getClass())) {
  386 | 					return false;
  387 | 				}
  388 | 				if (joinPointMatch.matches()) {
  389 | 					bindParameters(pmi, joinPointMatch);
  390 | 				}
  391 | 			}
  392 | 
  393 | 			return joinPointMatch.matches();
  394 | 		}
  395 | 		catch (Throwable ex) {
  396 | 			if (logger.isDebugEnabled()) {
  397 | 				logger.debug("Failed to evaluate join point for arguments " + Arrays.toString(args) +
  398 | 						" - falling back to non-match", ex);
  399 | 			}
  400 | 			return false;
  401 | 		}
  402 | 	}
  403 | 
  404 | 	protected @Nullable String getCurrentProxiedBeanName() {
  405 | 		return ProxyCreationContext.getCurrentProxiedBeanName();
  406 | 	}
  407 | 
  408 | 
  409 | 	/**
  410 | 	 * Get a new pointcut expression based on a target class's loader rather than the default.
  411 | 	 */
  412 | 	private @Nullable PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
  413 | 		try {
  414 | 			ClassLoader classLoader = targetClass.getClassLoader();
  415 | 			if (classLoader != null && classLoader != this.pointcutClassLoader) {
  416 | 				return buildPointcutExpression(classLoader);
  417 | 			}
  418 | 		}
  419 | 		catch (Throwable ex) {
  420 | 			logger.debug("Failed to create fallback PointcutExpression", ex);
  421 | 		}
  422 | 		return null;
  423 | 	}
  424 | 
  425 | 	private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
  426 | 		if (shadowMatch instanceof DefensiveShadowMatch defensiveShadowMatch) {
  427 | 			return new RuntimeTestWalker(defensiveShadowMatch.primary);
  428 | 		}
  429 | 		return new RuntimeTestWalker(shadowMatch);
  430 | 	}
  431 | 
  432 | 	private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
  433 | 		// Note: Can't use JoinPointMatch.getClass().getName() as the key, since
  434 | 		// Spring AOP does all the matching at a join point, and then all the invocations
  435 | 		// under this scenario, if we just use JoinPointMatch as the key, then
  436 | 		// 'last man wins' which is not what we want at all.
  437 | 		// Using the expression is guaranteed to be safe, since 2 identical expressions
  438 | 		// are guaranteed to bind in exactly the same way.
  439 | 		invocation.setUserAttribute(resolveExpression(), jpm);
  440 | 	}
  441 | 
  442 | 	private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
  443 | 		Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
  444 | 		if (targetMethod.getDeclaringClass().isInterface() && targetMethod.getDeclaringClass() != targetClass &&
  445 | 				obtainPointcutExpression().getPointcutExpression().contains("." + targetMethod.getName() + "(")) {
  446 | 			// Try to build the most specific interface possible for inherited methods to be
  447 | 			// considered for sub-interface matches as well, in particular for proxy classes.
  448 | 			// Note: AspectJ is only going to take Method.getDeclaringClass() into account.
  449 | 			Set<Class<?>> ifcs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
  450 | 			if (ifcs.size() > 1) {
  451 | 				try {
  452 | 					Class<?> compositeInterface = ClassUtils.createCompositeInterface(
  453 | 							ClassUtils.toClassArray(ifcs), targetClass.getClassLoader());
  454 | 					targetMethod = ClassUtils.getMostSpecificMethod(targetMethod, compositeInterface);
  455 | 				}
  456 | 				catch (IllegalArgumentException ex) {
  457 | 					// Implemented interfaces probably expose conflicting method signatures...
  458 | 					// Proceed with original target method.
  459 | 				}
  460 | 			}
  461 | 		}
  462 | 		return getShadowMatch(targetMethod, method);
  463 | 	}
  464 | 
  465 | 	private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
  466 | 		ShadowMatchKey key = new ShadowMatchKey(this, targetMethod);
  467 | 		ShadowMatch shadowMatch = ShadowMatchUtils.getShadowMatch(key);
  468 | 		if (shadowMatch == null) {
  469 | 			PointcutExpression pointcutExpression = obtainPointcutExpression();
  470 | 			synchronized (pointcutExpression) {
  471 | 				shadowMatch = ShadowMatchUtils.getShadowMatch(key);
  472 | 				if (shadowMatch != null) {
  473 | 					return shadowMatch;
  474 | 				}
  475 | 				PointcutExpression fallbackExpression = null;
  476 | 				Method methodToMatch = targetMethod;
  477 | 				try {
  478 | 					try {
  479 | 						shadowMatch = pointcutExpression.matchesMethodExecution(methodToMatch);
  480 | 					}
  481 | 					catch (ReflectionWorldException ex) {
  482 | 						// Failed to introspect target method, probably because it has been loaded
  483 | 						// in a special ClassLoader. Let's try the declaring ClassLoader instead...
  484 | 						try {
  485 | 							fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
  486 | 							if (fallbackExpression != null) {
  487 | 								shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
  488 | 							}
  489 | 						}
  490 | 						catch (ReflectionWorldException ex2) {
  491 | 							fallbackExpression = null;
  492 | 						}
  493 | 					}
  494 | 					if (targetMethod != originalMethod && (shadowMatch == null ||
  495 | 							(Proxy.isProxyClass(targetMethod.getDeclaringClass()) &&
  496 | 									(shadowMatch.neverMatches() || containsAnnotationPointcut())))) {
  497 | 						// Fall back to the plain original method in case of no resolvable match or a
  498 | 						// negative match on a proxy class (which doesn't carry any annotations on its
  499 | 						// redeclared methods), as well as for annotation pointcuts.
  500 | 						methodToMatch = originalMethod;
  501 | 						try {
  502 | 							shadowMatch = pointcutExpression.matchesMethodExecution(methodToMatch);
  503 | 						}
  504 | 						catch (ReflectionWorldException ex) {
  505 | 							// Could neither introspect the target class nor the proxy class ->
  506 | 							// let's try the original method's declaring class before we give up...
  507 | 							try {
  508 | 								fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
  509 | 								if (fallbackExpression != null) {
  510 | 									shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
  511 | 								}
  512 | 							}
  513 | 							catch (ReflectionWorldException ex2) {
  514 | 								fallbackExpression = null;
  515 | 							}
  516 | 						}
  517 | 					}
  518 | 				}
  519 | 				catch (Throwable ex) {
  520 | 					// Possibly AspectJ 1.8.10 encountering an invalid signature
  521 | 					logger.debug("PointcutExpression matching rejected target method", ex);
  522 | 					fallbackExpression = null;
  523 | 				}
  524 | 				if (shadowMatch == null) {
  525 | 					shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
  526 | 				}
  527 | 				else if (shadowMatch.maybeMatches() && fallbackExpression != null) {
  528 | 					shadowMatch = new DefensiveShadowMatch(shadowMatch,
  529 | 							fallbackExpression.matchesMethodExecution(methodToMatch));
  530 | 				}
  531 | 				shadowMatch = ShadowMatchUtils.setShadowMatch(key, shadowMatch);
  532 | 			}
  533 | 		}
  534 | 		return shadowMatch;
  535 | 	}
  536 | 
  537 | 	private boolean containsAnnotationPointcut() {
  538 | 		return resolveExpression().contains("@annotation");
  539 | 	}
  540 | 
  541 | 	private static boolean compiledByAjc(Class<?> clazz) {
  542 | 		for (Field field : clazz.getDeclaredFields()) {
  543 | 			if (field.getName().startsWith(AJC_MAGIC)) {
  544 | 				return true;
  545 | 			}
  546 | 		}
  547 | 		Class<?> superclass = clazz.getSuperclass();
  548 | 		return (superclass != null && compiledByAjc(superclass));
  549 | 	}
  550 | 
  551 | 
  552 | 	@Override
  553 | 	public boolean equals(@Nullable Object other) {
  554 | 		return (this == other || (other instanceof AspectJExpressionPointcut that &&
  555 | 				ObjectUtils.nullSafeEquals(getExpression(), that.getExpression()) &&
  556 | 				ObjectUtils.nullSafeEquals(this.pointcutDeclarationScope, that.pointcutDeclarationScope) &&
  557 | 				ObjectUtils.nullSafeEquals(this.pointcutParameterNames, that.pointcutParameterNames) &&
  558 | 				ObjectUtils.nullSafeEquals(this.pointcutParameterTypes, that.pointcutParameterTypes)));
  559 | 	}
  560 | 
  561 | 	@Override
  562 | 	public int hashCode() {
  563 | 		return ObjectUtils.nullSafeHash(getExpression(), this.pointcutDeclarationScope,
  564 | 				this.pointcutParameterNames, this.pointcutParameterTypes);
  565 | 	}
  566 | 
  567 | 	@Override
  568 | 	public String toString() {
  569 | 		StringBuilder sb = new StringBuilder("AspectJExpressionPointcut: (");
  570 | 		for (int i = 0; i < this.pointcutParameterTypes.length; i++) {
  571 | 			sb.append(this.pointcutParameterTypes[i].getName());
  572 | 			sb.append(' ');
  573 | 			sb.append(this.pointcutParameterNames[i]);
  574 | 			if ((i+1) < this.pointcutParameterTypes.length) {
  575 | 				sb.append(", ");
  576 | 			}
  577 | 		}
  578 | 		sb.append(") ");
  579 | 		if (getExpression() != null) {
  580 | 			sb.append(getExpression());
  581 | 		}
  582 | 		else {
  583 | 			sb.append("<pointcut expression not set>");
  584 | 		}
  585 | 		return sb.toString();
  586 | 	}
  587 | 
  588 | 
  589 | 	/**
  590 | 	 * Handler for the Spring-specific {@code bean()} pointcut designator
  591 | 	 * extension to AspectJ.
  592 | 	 * <p>This handler must be added to each pointcut object that needs to
  593 | 	 * handle the {@code bean()} PCD. Matching context is obtained
  594 | 	 * automatically by examining a thread local variable and therefore a matching
  595 | 	 * context need not be set on the pointcut.
  596 | 	 */
  597 | 	private class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {
  598 | 
  599 | 		private static final String BEAN_DESIGNATOR_NAME = "bean";
  600 | 
  601 | 		@Override
  602 | 		public String getDesignatorName() {
  603 | 			return BEAN_DESIGNATOR_NAME;
  604 | 		}
  605 | 
  606 | 		@Override
  607 | 		public ContextBasedMatcher parse(String expression) {
  608 | 			return new BeanContextMatcher(expression);
  609 | 		}
  610 | 	}
  611 | 
  612 | 
  613 | 	/**
  614 | 	 * Matcher class for the BeanNamePointcutDesignatorHandler.
  615 | 	 * <p>Dynamic match tests for this matcher always return true,
  616 | 	 * since the matching decision is made at the proxy creation time.
  617 | 	 * For static match tests, this matcher abstains to allow the overall
  618 | 	 * pointcut to match even when negation is used with the bean() pointcut.
  619 | 	 */
  620 | 	private class BeanContextMatcher implements ContextBasedMatcher {
  621 | 
  622 | 		private final NamePattern expressionPattern;
  623 | 
  624 | 		public BeanContextMatcher(String expression) {
  625 | 			this.expressionPattern = new NamePattern(expression);
  626 | 		}
  627 | 
  628 | 		@Override
  629 | 		@SuppressWarnings("rawtypes")
  630 | 		@Deprecated(since = "4.0") // deprecated by AspectJ
  631 | 		public boolean couldMatchJoinPointsInType(Class someClass) {
  632 | 			return (contextMatch(someClass) == FuzzyBoolean.YES);
  633 | 		}
  634 | 
  635 | 		@Override
  636 | 		@SuppressWarnings("rawtypes")
  637 | 		@Deprecated(since = "4.0") // deprecated by AspectJ
  638 | 		public boolean couldMatchJoinPointsInType(Class someClass, MatchingContext context) {
  639 | 			return (contextMatch(someClass) == FuzzyBoolean.YES);
  640 | 		}
  641 | 
  642 | 		@Override
  643 | 		public boolean matchesDynamically(MatchingContext context) {
  644 | 			return true;
  645 | 		}
  646 | 
  647 | 		@Override
  648 | 		public FuzzyBoolean matchesStatically(MatchingContext context) {
  649 | 			return contextMatch(null);
  650 | 		}
  651 | 
  652 | 		@Override
  653 | 		public boolean mayNeedDynamicTest() {
  654 | 			return false;
  655 | 		}
  656 | 
  657 | 		private FuzzyBoolean contextMatch(@Nullable Class<?> targetType) {
  658 | 			String advisedBeanName = getCurrentProxiedBeanName();
  659 | 			if (advisedBeanName == null) {  // no proxy creation in progress
  660 | 				// abstain; can't return YES, since that will make pointcut with negation fail
  661 | 				return FuzzyBoolean.MAYBE;
  662 | 			}
  663 | 			if (BeanFactoryUtils.isGeneratedBeanName(advisedBeanName)) {
  664 | 				return FuzzyBoolean.NO;
  665 | 			}
  666 | 			if (targetType != null) {
  667 | 				boolean isFactory = FactoryBean.class.isAssignableFrom(targetType);
  668 | 				return FuzzyBoolean.fromBoolean(
  669 | 						matchesBean(isFactory ? BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName : advisedBeanName));
  670 | 			}
  671 | 			else {
  672 | 				return FuzzyBoolean.fromBoolean(matchesBean(advisedBeanName) ||
  673 | 						matchesBean(BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName));
  674 | 			}
  675 | 		}
  676 | 
  677 | 		private boolean matchesBean(String advisedBeanName) {
  678 | 			return BeanFactoryAnnotationUtils.isQualifierMatch(
  679 | 					this.expressionPattern::matches, advisedBeanName, beanFactory);
  680 | 		}
  681 | 	}
  682 | 
  683 | 
  684 | 	private static class DefensiveShadowMatch implements ShadowMatch {
  685 | 
  686 | 		private final ShadowMatch primary;
  687 | 
  688 | 		private final ShadowMatch other;
  689 | 
  690 | 		public DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) {
  691 | 			this.primary = primary;
  692 | 			this.other = other;
  693 | 		}
  694 | 
  695 | 		@Override
  696 | 		public boolean alwaysMatches() {
  697 | 			return this.primary.alwaysMatches();
  698 | 		}
  699 | 
  700 | 		@Override
  701 | 		public boolean maybeMatches() {
  702 | 			return this.primary.maybeMatches();
  703 | 		}
  704 | 
  705 | 		@Override
  706 | 		public boolean neverMatches() {
  707 | 			return this.primary.neverMatches();
  708 | 		}
  709 | 
  710 | 		@Override
  711 | 		public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
  712 | 			try {
  713 | 				return this.primary.matchesJoinPoint(thisObject, targetObject, args);
  714 | 			}
  715 | 			catch (ReflectionWorldException ex) {
  716 | 				return this.other.matchesJoinPoint(thisObject, targetObject, args);
  717 | 			}
  718 | 		}
  719 | 
  720 | 		@Override
  721 | 		public void setMatchingContext(MatchingContext aMatchContext) {
  722 | 			this.primary.setMatchingContext(aMatchContext);
  723 | 			this.other.setMatchingContext(aMatchContext);
  724 | 		}
  725 | 	}
  726 | 
  727 | 
  728 | 	private record ShadowMatchKey(AspectJExpressionPointcut expression, Method method) {
  729 | 	}
  730 | 
  731 | }
```
