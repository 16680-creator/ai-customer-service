<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/AdvisedSupport.java -->
<!-- 正文引用：[[源码:AdvisedSupport.java#L起-L止]] ；代码块头部：(src: AdvisedSupport.java#L起-L止) -->

# AdvisedSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/AdvisedSupport.java` |
| 行数 | 747 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.135Z |

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
   21 | import java.lang.reflect.Method;
   22 | import java.util.ArrayList;
   23 | import java.util.Arrays;
   24 | import java.util.Collection;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.concurrent.ConcurrentHashMap;
   28 | 
   29 | import org.aopalliance.aop.Advice;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.aop.Advisor;
   33 | import org.springframework.aop.DynamicIntroductionAdvice;
   34 | import org.springframework.aop.IntroductionAdvisor;
   35 | import org.springframework.aop.IntroductionInfo;
   36 | import org.springframework.aop.Pointcut;
   37 | import org.springframework.aop.PointcutAdvisor;
   38 | import org.springframework.aop.SpringProxy;
   39 | import org.springframework.aop.TargetSource;
   40 | import org.springframework.aop.support.DefaultIntroductionAdvisor;
   41 | import org.springframework.aop.support.DefaultPointcutAdvisor;
   42 | import org.springframework.aop.target.EmptyTargetSource;
   43 | import org.springframework.aop.target.SingletonTargetSource;
   44 | import org.springframework.util.Assert;
   45 | import org.springframework.util.ClassUtils;
   46 | import org.springframework.util.CollectionUtils;
   47 | import org.springframework.util.ObjectUtils;
   48 | 
   49 | /**
   50 |  * Base class for AOP proxy configuration managers.
   51 |  *
   52 |  * <p>These are not themselves AOP proxies, but subclasses of this class are
   53 |  * normally factories from which AOP proxy instances are obtained directly.
   54 |  *
   55 |  * <p>This class frees subclasses of the housekeeping of Advices
   56 |  * and Advisors, but doesn't actually implement proxy creation
   57 |  * methods, which are provided by subclasses.
   58 |  *
   59 |  * <p>This class is serializable; subclasses need not be.
   60 |  *
   61 |  * <p>This class is used to hold snapshots of proxies.
   62 |  *
   63 |  * @author Rod Johnson
   64 |  * @author Juergen Hoeller
   65 |  * @author Sam Brannen
   66 |  * @see org.springframework.aop.framework.AopProxy
   67 |  */
   68 | public class AdvisedSupport extends ProxyConfig implements Advised {
   69 | 
   70 | 	/** use serialVersionUID from Spring 2.0 for interoperability. */
   71 | 	private static final long serialVersionUID = 2651364800145442165L;
   72 | 
   73 | 	private static final Advisor[] EMPTY_ADVISOR_ARRAY = new Advisor[0];
   74 | 
   75 | 
   76 | 	/**
   77 | 	 * Canonical TargetSource when there's no target, and behavior is
   78 | 	 * supplied by the advisors.
   79 | 	 */
   80 | 	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;
   81 | 
   82 | 
   83 | 	/** Package-protected to allow direct access for efficiency. */
   84 | 	@SuppressWarnings("serial")
   85 | 	TargetSource targetSource = EMPTY_TARGET_SOURCE;
   86 | 
   87 | 	/** Whether the Advisors are already filtered for the specific target class. */
   88 | 	private boolean preFiltered = false;
   89 | 
   90 | 	/** The AdvisorChainFactory to use. */
   91 | 	@SuppressWarnings("serial")
   92 | 	private AdvisorChainFactory advisorChainFactory = DefaultAdvisorChainFactory.INSTANCE;
   93 | 
   94 | 	/**
   95 | 	 * Interfaces to be implemented by the proxy. Held in List to keep the order
   96 | 	 * of registration, to create JDK proxy with specified order of interfaces.
   97 | 	 */
   98 | 	@SuppressWarnings("serial")
   99 | 	private List<Class<?>> interfaces = new ArrayList<>();
  100 | 
  101 | 	/**
  102 | 	 * List of Advisors. If an Advice is added, it will be wrapped
  103 | 	 * in an Advisor before being added to this List.
  104 | 	 */
  105 | 	@SuppressWarnings("serial")
  106 | 	private List<Advisor> advisors = new ArrayList<>();
  107 | 
  108 | 	/**
  109 | 	 * List of minimal {@link AdvisorKeyEntry} instances,
  110 | 	 * to be assigned to the {@link #advisors} field on reduction.
  111 | 	 * @since 6.0.10
  112 | 	 * @see #reduceToAdvisorKey
  113 | 	 */
  114 | 	@SuppressWarnings("serial")
  115 | 	private List<Advisor> advisorKey = this.advisors;
  116 | 
  117 | 	/** Cache with Method as key and advisor chain List as value. */
  118 | 	private transient @Nullable Map<MethodCacheKey, List<Object>> methodCache;
  119 | 
  120 | 	/** Cache with shared interceptors which are not method-specific. */
  121 | 	private transient volatile @Nullable List<Object> cachedInterceptors;
  122 | 
  123 | 	/**
  124 | 	 * Optional field for {@link AopProxy} implementations to store metadata in.
  125 | 	 * Used by {@link JdkDynamicAopProxy}.
  126 | 	 * @since 6.1.3
  127 | 	 * @see JdkDynamicAopProxy#JdkDynamicAopProxy(AdvisedSupport)
  128 | 	 */
  129 | 	transient volatile @Nullable Object proxyMetadataCache;
  130 | 
  131 | 
  132 | 	/**
  133 | 	 * No-arg constructor for use as a JavaBean.
  134 | 	 */
  135 | 	public AdvisedSupport() {
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Create an {@code AdvisedSupport} instance with the given parameters.
  140 | 	 * @param interfaces the proxied interfaces
  141 | 	 */
  142 | 	public AdvisedSupport(Class<?>... interfaces) {
  143 | 		setInterfaces(interfaces);
  144 | 	}
  145 | 
  146 | 
  147 | 	/**
  148 | 	 * Set the given object as target.
  149 | 	 * <p>Will create a SingletonTargetSource for the object.
  150 | 	 * @see #setTargetSource
  151 | 	 * @see org.springframework.aop.target.SingletonTargetSource
  152 | 	 */
  153 | 	public void setTarget(Object target) {
  154 | 		setTargetSource(new SingletonTargetSource(target));
  155 | 	}
  156 | 
  157 | 	@Override
  158 | 	public void setTargetSource(@Nullable TargetSource targetSource) {
  159 | 		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
  160 | 	}
  161 | 
  162 | 	@Override
  163 | 	public TargetSource getTargetSource() {
  164 | 		return this.targetSource;
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Set a target class to be proxied, indicating that the proxy
  169 | 	 * should be castable to the given class.
  170 | 	 * <p>Internally, an {@link org.springframework.aop.target.EmptyTargetSource}
  171 | 	 * for the given target class will be used. The kind of proxy needed
  172 | 	 * will be determined on actual creation of the proxy.
  173 | 	 * <p>This is a replacement for setting a "targetSource" or "target",
  174 | 	 * for the case where we want a proxy based on a target class
  175 | 	 * (which can be an interface or a concrete class) without having
  176 | 	 * a fully capable TargetSource available.
  177 | 	 * @see #setTargetSource
  178 | 	 * @see #setTarget
  179 | 	 */
  180 | 	public void setTargetClass(@Nullable Class<?> targetClass) {
  181 | 		this.targetSource = EmptyTargetSource.forClass(targetClass);
  182 | 	}
  183 | 
  184 | 	@Override
  185 | 	public @Nullable Class<?> getTargetClass() {
  186 | 		return this.targetSource.getTargetClass();
  187 | 	}
  188 | 
  189 | 	@Override
  190 | 	public void setPreFiltered(boolean preFiltered) {
  191 | 		this.preFiltered = preFiltered;
  192 | 	}
  193 | 
  194 | 	@Override
  195 | 	public boolean isPreFiltered() {
  196 | 		return this.preFiltered;
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * Set the advisor chain factory to use.
  201 | 	 * <p>Default is a {@link DefaultAdvisorChainFactory}.
  202 | 	 */
  203 | 	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
  204 | 		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
  205 | 		this.advisorChainFactory = advisorChainFactory;
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Return the advisor chain factory to use (never {@code null}).
  210 | 	 */
  211 | 	public AdvisorChainFactory getAdvisorChainFactory() {
  212 | 		return this.advisorChainFactory;
  213 | 	}
  214 | 
  215 | 
  216 | 	/**
  217 | 	 * Set the interfaces to be proxied.
  218 | 	 */
  219 | 	public void setInterfaces(Class<?>... interfaces) {
  220 | 		Assert.notNull(interfaces, "Interfaces must not be null");
  221 | 		this.interfaces.clear();
  222 | 		for (Class<?> ifc : interfaces) {
  223 | 			addInterface(ifc);
  224 | 		}
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Add a new proxied interface.
  229 | 	 * @param ifc the additional interface to proxy
  230 | 	 */
  231 | 	public void addInterface(Class<?> ifc) {
  232 | 		Assert.notNull(ifc, "Interface must not be null");
  233 | 		if (!ifc.isInterface()) {
  234 | 			throw new IllegalArgumentException("[" + ifc.getName() + "] is not an interface");
  235 | 		}
  236 | 		if (!this.interfaces.contains(ifc)) {
  237 | 			this.interfaces.add(ifc);
  238 | 			adviceChanged();
  239 | 		}
  240 | 	}
  241 | 
  242 | 	/**
  243 | 	 * Remove a proxied interface.
  244 | 	 * <p>Does nothing if the given interface isn't proxied.
  245 | 	 * @param ifc the interface to remove from the proxy
  246 | 	 * @return {@code true} if the interface was removed; {@code false}
  247 | 	 * if the interface was not found and hence could not be removed
  248 | 	 */
  249 | 	public boolean removeInterface(Class<?> ifc) {
  250 | 		return this.interfaces.remove(ifc);
  251 | 	}
  252 | 
  253 | 	@Override
  254 | 	public Class<?>[] getProxiedInterfaces() {
  255 | 		return ClassUtils.toClassArray(this.interfaces);
  256 | 	}
  257 | 
  258 | 	@Override
  259 | 	public boolean isInterfaceProxied(Class<?> ifc) {
  260 | 		for (Class<?> proxyIntf : this.interfaces) {
  261 | 			if (ifc.isAssignableFrom(proxyIntf)) {
  262 | 				return true;
  263 | 			}
  264 | 		}
  265 | 		return false;
  266 | 	}
  267 | 
  268 | 	boolean hasUserSuppliedInterfaces() {
  269 | 		for (Class<?> ifc : this.interfaces) {
  270 | 			if (!SpringProxy.class.isAssignableFrom(ifc) && !isAdvisorIntroducedInterface(ifc)) {
  271 | 				return true;
  272 | 			}
  273 | 		}
  274 | 		return false;
  275 | 	}
  276 | 
  277 | 	private boolean isAdvisorIntroducedInterface(Class<?> ifc) {
  278 | 		for (Advisor advisor : this.advisors) {
  279 | 			if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
  280 | 				for (Class<?> introducedInterface : introductionAdvisor.getInterfaces()) {
  281 | 					if (introducedInterface == ifc) {
  282 | 						return true;
  283 | 					}
  284 | 				}
  285 | 			}
  286 | 		}
  287 | 		return false;
  288 | 	}
  289 | 
  290 | 
  291 | 	@Override
  292 | 	public final Advisor[] getAdvisors() {
  293 | 		return this.advisors.toArray(EMPTY_ADVISOR_ARRAY);
  294 | 	}
  295 | 
  296 | 	@Override
  297 | 	public int getAdvisorCount() {
  298 | 		return this.advisors.size();
  299 | 	}
  300 | 
  301 | 	@Override
  302 | 	public void addAdvisor(Advisor advisor) {
  303 | 		int pos = this.advisors.size();
  304 | 		addAdvisor(pos, advisor);
  305 | 	}
  306 | 
  307 | 	@Override
  308 | 	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
  309 | 		if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
  310 | 			validateIntroductionAdvisor(introductionAdvisor);
  311 | 		}
  312 | 		addAdvisorInternal(pos, advisor);
  313 | 	}
  314 | 
  315 | 	@Override
  316 | 	public boolean removeAdvisor(Advisor advisor) {
  317 | 		int index = indexOf(advisor);
  318 | 		if (index == -1) {
  319 | 			return false;
  320 | 		}
  321 | 		else {
  322 | 			removeAdvisor(index);
  323 | 			return true;
  324 | 		}
  325 | 	}
  326 | 
  327 | 	@Override
  328 | 	public void removeAdvisor(int index) throws AopConfigException {
  329 | 		if (isFrozen()) {
  330 | 			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
  331 | 		}
  332 | 		if (index < 0 || index > this.advisors.size() - 1) {
  333 | 			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
  334 | 					"This configuration only has " + this.advisors.size() + " advisors.");
  335 | 		}
  336 | 
  337 | 		Advisor advisor = this.advisors.remove(index);
  338 | 		if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
  339 | 			// We need to remove introduction interfaces.
  340 | 			for (Class<?> ifc : introductionAdvisor.getInterfaces()) {
  341 | 				removeInterface(ifc);
  342 | 			}
  343 | 		}
  344 | 
  345 | 		adviceChanged();
  346 | 	}
  347 | 
  348 | 	@Override
  349 | 	public int indexOf(Advisor advisor) {
  350 | 		Assert.notNull(advisor, "Advisor must not be null");
  351 | 		return this.advisors.indexOf(advisor);
  352 | 	}
  353 | 
  354 | 	@Override
  355 | 	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
  356 | 		Assert.notNull(a, "Advisor a must not be null");
  357 | 		Assert.notNull(b, "Advisor b must not be null");
  358 | 		int index = indexOf(a);
  359 | 		if (index == -1) {
  360 | 			return false;
  361 | 		}
  362 | 		removeAdvisor(index);
  363 | 		addAdvisor(index, b);
  364 | 		return true;
  365 | 	}
  366 | 
  367 | 	/**
  368 | 	 * Add all the given advisors to this proxy configuration.
  369 | 	 * @param advisors the advisors to register
  370 | 	 */
  371 | 	public void addAdvisors(Advisor... advisors) {
  372 | 		addAdvisors(Arrays.asList(advisors));
  373 | 	}
  374 | 
  375 | 	/**
  376 | 	 * Add all the given advisors to this proxy configuration.
  377 | 	 * @param advisors the advisors to register
  378 | 	 */
  379 | 	public void addAdvisors(Collection<Advisor> advisors) {
  380 | 		if (isFrozen()) {
  381 | 			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
  382 | 		}
  383 | 		if (!CollectionUtils.isEmpty(advisors)) {
  384 | 			for (Advisor advisor : advisors) {
  385 | 				if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
  386 | 					validateIntroductionAdvisor(introductionAdvisor);
  387 | 				}
  388 | 				Assert.notNull(advisor, "Advisor must not be null");
  389 | 				this.advisors.add(advisor);
  390 | 			}
  391 | 			adviceChanged();
  392 | 		}
  393 | 	}
  394 | 
  395 | 	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
  396 | 		advisor.validateInterfaces();
  397 | 		// If the advisor passed validation, we can make the change.
  398 | 		for (Class<?> ifc : advisor.getInterfaces()) {
  399 | 			addInterface(ifc);
  400 | 		}
  401 | 	}
  402 | 
  403 | 	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
  404 | 		Assert.notNull(advisor, "Advisor must not be null");
  405 | 		if (isFrozen()) {
  406 | 			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
  407 | 		}
  408 | 		if (pos > this.advisors.size()) {
  409 | 			throw new IllegalArgumentException(
  410 | 					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
  411 | 		}
  412 | 		this.advisors.add(pos, advisor);
  413 | 		adviceChanged();
  414 | 	}
  415 | 
  416 | 	/**
  417 | 	 * Allows uncontrolled access to the {@link List} of {@link Advisor Advisors}.
  418 | 	 * <p>Use with care, and remember to {@link #adviceChanged() fire advice changed events}
  419 | 	 * when making any modifications.
  420 | 	 */
  421 | 	protected final List<Advisor> getAdvisorsInternal() {
  422 | 		return this.advisors;
  423 | 	}
  424 | 
  425 | 	@Override
  426 | 	public void addAdvice(Advice advice) throws AopConfigException {
  427 | 		int pos = this.advisors.size();
  428 | 		addAdvice(pos, advice);
  429 | 	}
  430 | 
  431 | 	/**
  432 | 	 * Cannot add introductions this way unless the advice implements IntroductionInfo.
  433 | 	 */
  434 | 	@Override
  435 | 	public void addAdvice(int pos, Advice advice) throws AopConfigException {
  436 | 		Assert.notNull(advice, "Advice must not be null");
  437 | 		if (advice instanceof IntroductionInfo introductionInfo) {
  438 | 			// We don't need an IntroductionAdvisor for this kind of introduction:
  439 | 			// It's fully self-describing.
  440 | 			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, introductionInfo));
  441 | 		}
  442 | 		else if (advice instanceof DynamicIntroductionAdvice) {
  443 | 			// We need an IntroductionAdvisor for this kind of introduction.
  444 | 			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
  445 | 		}
  446 | 		else {
  447 | 			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
  448 | 		}
  449 | 	}
  450 | 
  451 | 	@Override
  452 | 	public boolean removeAdvice(Advice advice) throws AopConfigException {
  453 | 		int index = indexOf(advice);
  454 | 		if (index == -1) {
  455 | 			return false;
  456 | 		}
  457 | 		else {
  458 | 			removeAdvisor(index);
  459 | 			return true;
  460 | 		}
  461 | 	}
  462 | 
  463 | 	@Override
  464 | 	public int indexOf(Advice advice) {
  465 | 		Assert.notNull(advice, "Advice must not be null");
  466 | 		for (int i = 0; i < this.advisors.size(); i++) {
  467 | 			Advisor advisor = this.advisors.get(i);
  468 | 			if (advisor.getAdvice() == advice) {
  469 | 				return i;
  470 | 			}
  471 | 		}
  472 | 		return -1;
  473 | 	}
  474 | 
  475 | 	/**
  476 | 	 * Is the given advice included in any advisor within this proxy configuration?
  477 | 	 * @param advice the advice to check inclusion of
  478 | 	 * @return whether this advice instance is included
  479 | 	 */
  480 | 	public boolean adviceIncluded(@Nullable Advice advice) {
  481 | 		if (advice != null) {
  482 | 			for (Advisor advisor : this.advisors) {
  483 | 				if (advisor.getAdvice() == advice) {
  484 | 					return true;
  485 | 				}
  486 | 			}
  487 | 		}
  488 | 		return false;
  489 | 	}
  490 | 
  491 | 	/**
  492 | 	 * Count advices of the given class.
  493 | 	 * @param adviceClass the advice class to check
  494 | 	 * @return the count of the interceptors of this class or subclasses
  495 | 	 */
  496 | 	public int countAdvicesOfType(@Nullable Class<?> adviceClass) {
  497 | 		int count = 0;
  498 | 		if (adviceClass != null) {
  499 | 			for (Advisor advisor : this.advisors) {
  500 | 				if (adviceClass.isInstance(advisor.getAdvice())) {
  501 | 					count++;
  502 | 				}
  503 | 			}
  504 | 		}
  505 | 		return count;
  506 | 	}
  507 | 
  508 | 
  509 | 	/**
  510 | 	 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
  511 | 	 * for the given method, based on this configuration.
  512 | 	 * @param method the proxied method
  513 | 	 * @param targetClass the target class
  514 | 	 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
  515 | 	 */
  516 | 	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
  517 | 		List<Object> cachedInterceptors;
  518 | 		if (this.methodCache != null) {
  519 | 			// Method-specific cache for method-specific pointcuts
  520 | 			MethodCacheKey cacheKey = new MethodCacheKey(method);
  521 | 			cachedInterceptors = this.methodCache.get(cacheKey);
  522 | 			if (cachedInterceptors == null) {
  523 | 				cachedInterceptors = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
  524 | 						this, method, targetClass);
  525 | 				this.methodCache.put(cacheKey, cachedInterceptors);
  526 | 			}
  527 | 		}
  528 | 		else {
  529 | 			// Shared cache since there are no method-specific advisors (see below).
  530 | 			cachedInterceptors = this.cachedInterceptors;
  531 | 			if (cachedInterceptors == null) {
  532 | 				cachedInterceptors = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
  533 | 						this, method, targetClass);
  534 | 				this.cachedInterceptors = cachedInterceptors;
  535 | 			}
  536 | 		}
  537 | 		return cachedInterceptors;
  538 | 	}
  539 | 
  540 | 	/**
  541 | 	 * Invoked when advice has changed.
  542 | 	 */
  543 | 	protected void adviceChanged() {
  544 | 		this.methodCache = null;
  545 | 		this.cachedInterceptors = null;
  546 | 		this.proxyMetadataCache = null;
  547 | 
  548 | 		// Initialize method cache if necessary; otherwise,
  549 | 		// cachedInterceptors is going to be shared (see above).
  550 | 		for (Advisor advisor : this.advisors) {
  551 | 			if (advisor instanceof PointcutAdvisor) {
  552 | 				this.methodCache = new ConcurrentHashMap<>();
  553 | 				break;
  554 | 			}
  555 | 		}
  556 | 	}
  557 | 
  558 | 	/**
  559 | 	 * Call this method on a new instance created by the no-arg constructor
  560 | 	 * to create an independent copy of the configuration from the given object.
  561 | 	 * @param other the AdvisedSupport object to copy configuration from
  562 | 	 */
  563 | 	protected void copyConfigurationFrom(AdvisedSupport other) {
  564 | 		copyConfigurationFrom(other, other.targetSource, new ArrayList<>(other.advisors));
  565 | 	}
  566 | 
  567 | 	/**
  568 | 	 * Copy the AOP configuration from the given {@link AdvisedSupport} object,
  569 | 	 * but allow substitution of a fresh {@link TargetSource} and a given interceptor chain.
  570 | 	 * @param other the {@code AdvisedSupport} object to take proxy configuration from
  571 | 	 * @param targetSource the new TargetSource
  572 | 	 * @param advisors the Advisors for the chain
  573 | 	 */
  574 | 	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
  575 | 		copyFrom(other);
  576 | 		this.targetSource = targetSource;
  577 | 		this.advisorChainFactory = other.advisorChainFactory;
  578 | 		this.interfaces = new ArrayList<>(other.interfaces);
  579 | 		for (Advisor advisor : advisors) {
  580 | 			if (advisor instanceof IntroductionAdvisor introductionAdvisor) {
  581 | 				validateIntroductionAdvisor(introductionAdvisor);
  582 | 			}
  583 | 			Assert.notNull(advisor, "Advisor must not be null");
  584 | 			this.advisors.add(advisor);
  585 | 		}
  586 | 		adviceChanged();
  587 | 	}
  588 | 
  589 | 	/**
  590 | 	 * Build a configuration-only copy of this {@link AdvisedSupport},
  591 | 	 * replacing the {@link TargetSource}.
  592 | 	 */
  593 | 	AdvisedSupport getConfigurationOnlyCopy() {
  594 | 		AdvisedSupport copy = new AdvisedSupport();
  595 | 		copy.copyFrom(this);
  596 | 		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
  597 | 		copy.preFiltered = this.preFiltered;
  598 | 		copy.advisorChainFactory = this.advisorChainFactory;
  599 | 		copy.interfaces = new ArrayList<>(this.interfaces);
  600 | 		copy.advisors = new ArrayList<>(this.advisors);
  601 | 		copy.advisorKey = new ArrayList<>(this.advisors.size());
  602 | 		for (Advisor advisor : this.advisors) {
  603 | 			copy.advisorKey.add(new AdvisorKeyEntry(advisor));
  604 | 		}
  605 | 		copy.methodCache = this.methodCache;
  606 | 		copy.cachedInterceptors = this.cachedInterceptors;
  607 | 		copy.proxyMetadataCache = this.proxyMetadataCache;
  608 | 		return copy;
  609 | 	}
  610 | 
  611 | 	void reduceToAdvisorKey() {
  612 | 		this.advisors = this.advisorKey;
  613 | 		this.methodCache = null;
  614 | 		this.cachedInterceptors = null;
  615 | 		this.proxyMetadataCache = null;
  616 | 	}
  617 | 
  618 | 	Object getAdvisorKey() {
  619 | 		return this.advisorKey;
  620 | 	}
  621 | 
  622 | 
  623 | 	@Override
  624 | 	public String toProxyConfigString() {
  625 | 		return toString();
  626 | 	}
  627 | 
  628 | 	/**
  629 | 	 * For debugging/diagnostic use.
  630 | 	 */
  631 | 	@Override
  632 | 	public String toString() {
  633 | 		StringBuilder sb = new StringBuilder(getClass().getName());
  634 | 		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
  635 | 		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
  636 | 		sb.append(this.advisors.size()).append(" advisors ");
  637 | 		sb.append(this.advisors).append("; ");
  638 | 		sb.append("targetSource [").append(this.targetSource).append("]; ");
  639 | 		sb.append(super.toString());
  640 | 		return sb.toString();
  641 | 	}
  642 | 
  643 | 
  644 | 	//---------------------------------------------------------------------
  645 | 	// Serialization support
  646 | 	//---------------------------------------------------------------------
  647 | 
  648 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
  649 | 		// Rely on default serialization; just initialize state after deserialization.
  650 | 		ois.defaultReadObject();
  651 | 
  652 | 		// Initialize method cache if necessary.
  653 | 		adviceChanged();
  654 | 	}
  655 | 
  656 | 
  657 | 	/**
  658 | 	 * Simple wrapper class around a Method. Used as the key when
  659 | 	 * caching methods, for efficient equals and hashCode comparisons.
  660 | 	 */
  661 | 	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {
  662 | 
  663 | 		private final Method method;
  664 | 
  665 | 		private final int hashCode;
  666 | 
  667 | 		public MethodCacheKey(Method method) {
  668 | 			this.method = method;
  669 | 			this.hashCode = method.hashCode();
  670 | 		}
  671 | 
  672 | 		@Override
  673 | 		public boolean equals(@Nullable Object other) {
  674 | 			return (this == other || (other instanceof MethodCacheKey that &&
  675 | 					(this.method == that.method || this.method.equals(that.method))));
  676 | 		}
  677 | 
  678 | 		@Override
  679 | 		public int hashCode() {
  680 | 			return this.hashCode;
  681 | 		}
  682 | 
  683 | 		@Override
  684 | 		public String toString() {
  685 | 			return this.method.toString();
  686 | 		}
  687 | 
  688 | 		@Override
  689 | 		public int compareTo(MethodCacheKey other) {
  690 | 			int result = this.method.getName().compareTo(other.method.getName());
  691 | 			if (result == 0) {
  692 | 				result = this.method.toString().compareTo(other.method.toString());
  693 | 			}
  694 | 			return result;
  695 | 		}
  696 | 	}
  697 | 
  698 | 
  699 | 	/**
  700 | 	 * Stub for an {@link Advisor} instance that is just needed for key purposes,
  701 | 	 * allowing for efficient equals and hashCode comparisons against the
  702 | 	 * advice class and the pointcut.
  703 | 	 * @since 6.0.10
  704 | 	 * @see #getConfigurationOnlyCopy()
  705 | 	 * @see #getAdvisorKey()
  706 | 	 */
  707 | 	private static final class AdvisorKeyEntry implements Advisor {
  708 | 
  709 | 		private final Class<?> adviceType;
  710 | 
  711 | 		private final @Nullable String classFilterKey;
  712 | 
  713 | 		private final @Nullable String methodMatcherKey;
  714 | 
  715 | 		public AdvisorKeyEntry(Advisor advisor) {
  716 | 			this.adviceType = advisor.getAdvice().getClass();
  717 | 			if (advisor instanceof PointcutAdvisor pointcutAdvisor) {
  718 | 				Pointcut pointcut = pointcutAdvisor.getPointcut();
  719 | 				this.classFilterKey = pointcut.getClassFilter().toString();
  720 | 				this.methodMatcherKey = pointcut.getMethodMatcher().toString();
  721 | 			}
  722 | 			else {
  723 | 				this.classFilterKey = null;
  724 | 				this.methodMatcherKey = null;
  725 | 			}
  726 | 		}
  727 | 
  728 | 		@Override
  729 | 		public Advice getAdvice() {
  730 | 			throw new UnsupportedOperationException();
  731 | 		}
  732 | 
  733 | 		@Override
  734 | 		public boolean equals(@Nullable Object other) {
  735 | 			return (this == other || (other instanceof AdvisorKeyEntry that &&
  736 | 					this.adviceType == that.adviceType &&
  737 | 					ObjectUtils.nullSafeEquals(this.classFilterKey, that.classFilterKey) &&
  738 | 					ObjectUtils.nullSafeEquals(this.methodMatcherKey, that.methodMatcherKey)));
  739 | 		}
  740 | 
  741 | 		@Override
  742 | 		public int hashCode() {
  743 | 			return this.adviceType.hashCode();
  744 | 		}
  745 | 	}
  746 | 
  747 | }
```
