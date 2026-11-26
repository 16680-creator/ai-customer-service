<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/AbstractHandlerMethodMapping.java -->
<!-- 正文引用：[[源码:AbstractHandlerMethodMapping.java#L起-L止]] ；代码块头部：(src: AbstractHandlerMethodMapping.java#L起-L止) -->

# AbstractHandlerMethodMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/AbstractHandlerMethodMapping.java` |
| 行数 | 817 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:10.146Z |

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
   17 | package org.springframework.web.servlet.handler;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collection;
   23 | import java.util.Collections;
   24 | import java.util.Comparator;
   25 | import java.util.HashMap;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.Set;
   29 | import java.util.concurrent.ConcurrentHashMap;
   30 | import java.util.concurrent.locks.ReentrantReadWriteLock;
   31 | import java.util.function.Function;
   32 | import java.util.stream.Collectors;
   33 | 
   34 | import jakarta.servlet.ServletException;
   35 | import jakarta.servlet.http.HttpServletRequest;
   36 | import org.jspecify.annotations.Nullable;
   37 | 
   38 | import org.springframework.aop.support.AopUtils;
   39 | import org.springframework.beans.factory.BeanFactoryUtils;
   40 | import org.springframework.beans.factory.InitializingBean;
   41 | import org.springframework.core.MethodIntrospector;
   42 | import org.springframework.util.Assert;
   43 | import org.springframework.util.ClassUtils;
   44 | import org.springframework.util.LinkedMultiValueMap;
   45 | import org.springframework.util.MultiValueMap;
   46 | import org.springframework.util.StringUtils;
   47 | import org.springframework.web.cors.CorsConfiguration;
   48 | import org.springframework.web.cors.CorsUtils;
   49 | import org.springframework.web.method.HandlerMethod;
   50 | import org.springframework.web.servlet.HandlerMapping;
   51 | import org.springframework.web.util.pattern.PathPatternParser;
   52 | 
   53 | /**
   54 |  * Abstract base class for {@link HandlerMapping} implementations that define
   55 |  * a mapping between a request and a {@link HandlerMethod}.
   56 |  *
   57 |  * <p>For each registered handler method, a unique mapping is maintained with
   58 |  * subclasses defining the details of the mapping type {@code <T>}.
   59 |  *
   60 |  * @author Arjen Poutsma
   61 |  * @author Rossen Stoyanchev
   62 |  * @author Juergen Hoeller
   63 |  * @author Sam Brannen
   64 |  * @since 3.1
   65 |  * @param <T> the mapping for a {@link HandlerMethod} containing the conditions
   66 |  * needed to match the handler method to an incoming request.
   67 |  */
   68 | public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
   69 | 
   70 | 	/**
   71 | 	 * Bean name prefix for target beans behind scoped proxies. Used to exclude those
   72 | 	 * targets from handler method detection, in favor of the corresponding proxies.
   73 | 	 * <p>We're not checking the autowire-candidate status here, which is how the
   74 | 	 * proxy target filtering problem is being handled at the autowiring level,
   75 | 	 * since autowire-candidate may have been turned to {@code false} for other
   76 | 	 * reasons, while still expecting the bean to be eligible for handler methods.
   77 | 	 * <p>Originally defined in {@link org.springframework.aop.scope.ScopedProxyUtils}
   78 | 	 * but duplicated here to avoid a hard dependency on the spring-aop module.
   79 | 	 */
   80 | 	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";
   81 | 
   82 | 	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
   83 | 			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));
   84 | 
   85 | 	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();
   86 | 
   87 | 	static {
   88 | 		ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
   89 | 		ALLOW_CORS_CONFIG.addAllowedMethod("*");
   90 | 		ALLOW_CORS_CONFIG.addAllowedHeader("*");
   91 | 		ALLOW_CORS_CONFIG.setAllowCredentials(true);
   92 | 	}
   93 | 
   94 | 
   95 | 	private boolean detectHandlerMethodsInAncestorContexts = false;
   96 | 
   97 | 	private @Nullable HandlerMethodMappingNamingStrategy<T> namingStrategy;
   98 | 
   99 | 	private final MappingRegistry mappingRegistry = new MappingRegistry();
  100 | 
  101 | 
  102 | 	@Override
  103 | 	public void setPatternParser(@Nullable PathPatternParser patternParser) {
  104 | 		Assert.state(this.mappingRegistry.getRegistrations().isEmpty(),
  105 | 				"PathPatternParser must be set before the initialization of " +
  106 | 						"request mappings through InitializingBean#afterPropertiesSet.");
  107 | 		super.setPatternParser(patternParser);
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Whether to detect handler methods in beans in ancestor ApplicationContexts.
  112 | 	 * <p>Default is "false": Only beans in the current ApplicationContext are
  113 | 	 * considered, i.e. only in the context that this HandlerMapping itself
  114 | 	 * is defined in (typically the current DispatcherServlet's context).
  115 | 	 * <p>Switch this flag on to detect handler beans in ancestor contexts
  116 | 	 * (typically the Spring root WebApplicationContext) as well.
  117 | 	 * @see #getCandidateBeanNames()
  118 | 	 */
  119 | 	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
  120 | 		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
  121 | 	}
  122 | 
  123 | 	/**
  124 | 	 * Configure the naming strategy to use for assigning a default name to every
  125 | 	 * mapped handler method.
  126 | 	 * <p>The default naming strategy is based on the capital letters of the
  127 | 	 * class name followed by "#" and then the method name, for example, "TC#getFoo"
  128 | 	 * for a class named TestController with method getFoo.
  129 | 	 */
  130 | 	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
  131 | 		this.namingStrategy = namingStrategy;
  132 | 	}
  133 | 
  134 | 	/**
  135 | 	 * Return the configured naming strategy or {@code null}.
  136 | 	 */
  137 | 	public @Nullable HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
  138 | 		return this.namingStrategy;
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Return a (read-only) map with all mappings and HandlerMethod's.
  143 | 	 */
  144 | 	public Map<T, HandlerMethod> getHandlerMethods() {
  145 | 		this.mappingRegistry.acquireReadLock();
  146 | 		try {
  147 | 			return this.mappingRegistry.getRegistrations().entrySet().stream()
  148 | 					.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod));
  149 | 		}
  150 | 		finally {
  151 | 			this.mappingRegistry.releaseReadLock();
  152 | 		}
  153 | 	}
  154 | 
  155 | 	/**
  156 | 	 * Return the handler methods for the given mapping name.
  157 | 	 * @param mappingName the mapping name
  158 | 	 * @return a list of matching HandlerMethod's or {@code null}; the returned
  159 | 	 * list will never be modified and is safe to iterate.
  160 | 	 * @see #setHandlerMethodMappingNamingStrategy
  161 | 	 */
  162 | 	public @Nullable List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
  163 | 		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Return the internal mapping registry. Provided for testing purposes.
  168 | 	 */
  169 | 	MappingRegistry getMappingRegistry() {
  170 | 		return this.mappingRegistry;
  171 | 	}
  172 | 
  173 | 	/**
  174 | 	 * Register the given mapping.
  175 | 	 * <p>This method may be invoked at runtime after initialization has completed.
  176 | 	 * @param mapping the mapping for the handler method
  177 | 	 * @param handler the handler
  178 | 	 * @param method the method
  179 | 	 */
  180 | 	public void registerMapping(T mapping, Object handler, Method method) {
  181 | 		if (logger.isTraceEnabled()) {
  182 | 			logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
  183 | 		}
  184 | 		this.mappingRegistry.register(mapping, handler, method);
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Un-register the given mapping.
  189 | 	 * <p>This method may be invoked at runtime after initialization has completed.
  190 | 	 * @param mapping the mapping to unregister
  191 | 	 */
  192 | 	public void unregisterMapping(T mapping) {
  193 | 		if (logger.isTraceEnabled()) {
  194 | 			logger.trace("Unregister mapping \"" + mapping + "\"");
  195 | 		}
  196 | 		this.mappingRegistry.unregister(mapping);
  197 | 	}
  198 | 
  199 | 
  200 | 	// Handler method detection
  201 | 
  202 | 	/**
  203 | 	 * Detects handler methods at initialization.
  204 | 	 * @see #initHandlerMethods
  205 | 	 */
  206 | 	@Override
  207 | 	public void afterPropertiesSet() {
  208 | 		initHandlerMethods();
  209 | 	}
  210 | 
  211 | 	/**
  212 | 	 * Scan beans in the ApplicationContext, detect and register handler methods.
  213 | 	 * @see #getCandidateBeanNames()
  214 | 	 * @see #processCandidateBean
  215 | 	 * @see #handlerMethodsInitialized
  216 | 	 */
  217 | 	protected void initHandlerMethods() {
  218 | 		for (String beanName : getCandidateBeanNames()) {
  219 | 			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
  220 | 				processCandidateBean(beanName);
  221 | 			}
  222 | 		}
  223 | 		handlerMethodsInitialized(getHandlerMethods());
  224 | 	}
  225 | 
  226 | 	/**
  227 | 	 * Determine the names of candidate beans in the application context.
  228 | 	 * @since 5.1
  229 | 	 * @see #setDetectHandlerMethodsInAncestorContexts
  230 | 	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
  231 | 	 */
  232 | 	protected String[] getCandidateBeanNames() {
  233 | 		return (this.detectHandlerMethodsInAncestorContexts ?
  234 | 				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
  235 | 				obtainApplicationContext().getBeanNamesForType(Object.class));
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Determine the type of the specified candidate bean and call
  240 | 	 * {@link #detectHandlerMethods} if identified as a handler type.
  241 | 	 * <p>This implementation avoids bean creation through checking
  242 | 	 * {@link org.springframework.beans.factory.BeanFactory#getType}
  243 | 	 * and calling {@link #detectHandlerMethods} with the bean name.
  244 | 	 * @param beanName the name of the candidate bean
  245 | 	 * @since 5.1
  246 | 	 * @see #isHandler
  247 | 	 * @see #detectHandlerMethods
  248 | 	 */
  249 | 	protected void processCandidateBean(String beanName) {
  250 | 		Class<?> beanType = null;
  251 | 		try {
  252 | 			beanType = obtainApplicationContext().getType(beanName);
  253 | 		}
  254 | 		catch (Throwable ex) {
  255 | 			// An unresolvable bean type, probably from a lazy bean - let's ignore it.
  256 | 			if (logger.isTraceEnabled()) {
  257 | 				logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
  258 | 			}
  259 | 		}
  260 | 		if (beanType != null && isHandler(beanType)) {
  261 | 			detectHandlerMethods(beanName);
  262 | 		}
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Look for handler methods in the specified handler bean.
  267 | 	 * @param handler either a bean name or an actual handler instance
  268 | 	 * @see #getMappingForMethod
  269 | 	 */
  270 | 	protected void detectHandlerMethods(Object handler) {
  271 | 		Class<?> handlerType = (handler instanceof String beanName ?
  272 | 				obtainApplicationContext().getType(beanName) : handler.getClass());
  273 | 
  274 | 		if (handlerType != null) {
  275 | 			Class<?> userType = ClassUtils.getUserClass(handlerType);
  276 | 			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
  277 | 					(MethodIntrospector.MetadataLookup<T>) method -> {
  278 | 						try {
  279 | 							return getMappingForMethod(method, userType);
  280 | 						}
  281 | 						catch (Throwable ex) {
  282 | 							throw new IllegalStateException("Invalid mapping on handler class [" +
  283 | 									userType.getName() + "]: " + method, ex);
  284 | 						}
  285 | 					});
  286 | 			if (logger.isTraceEnabled()) {
  287 | 				logger.trace(formatMappings(userType, methods));
  288 | 			}
  289 | 			else if (mappingsLogger.isDebugEnabled()) {
  290 | 				mappingsLogger.debug(formatMappings(userType, methods));
  291 | 			}
  292 | 			methods.forEach((method, mapping) -> {
  293 | 				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
  294 | 				registerHandlerMethod(handler, invocableMethod, mapping);
  295 | 			});
  296 | 		}
  297 | 	}
  298 | 
  299 | 	private String formatMappings(Class<?> userType, Map<Method, T> methods) {
  300 | 		String packageName = ClassUtils.getPackageName(userType);
  301 | 		String formattedType = (StringUtils.hasText(packageName) ?
  302 | 				Arrays.stream(packageName.split("\\."))
  303 | 						.map(packageSegment -> packageSegment.substring(0, 1))
  304 | 						.collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
  305 | 				userType.getSimpleName());
  306 | 		Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
  307 | 				.map(Class::getSimpleName)
  308 | 				.collect(Collectors.joining(",", "(", ")"));
  309 | 		return methods.entrySet().stream()
  310 | 				.map(e -> {
  311 | 					Method method = e.getKey();
  312 | 					return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
  313 | 				})
  314 | 				.collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
  315 | 	}
  316 | 
  317 | 	/**
  318 | 	 * Register a handler method and its unique mapping. Invoked at startup for
  319 | 	 * each detected handler method.
  320 | 	 * @param handler the bean name of the handler or the handler instance
  321 | 	 * @param method the method to register
  322 | 	 * @param mapping the mapping conditions associated with the handler method
  323 | 	 * @throws IllegalStateException if another method was already registered
  324 | 	 * under the same mapping
  325 | 	 */
  326 | 	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
  327 | 		this.mappingRegistry.register(mapping, handler, method);
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Create the HandlerMethod instance.
  332 | 	 * @param handler either a bean name or an actual handler instance
  333 | 	 * @param method the target method
  334 | 	 * @return the created HandlerMethod
  335 | 	 */
  336 | 	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
  337 | 		if (handler instanceof String beanName) {
  338 | 			return new HandlerMethod(beanName,
  339 | 					obtainApplicationContext().getAutowireCapableBeanFactory(),
  340 | 					obtainApplicationContext(),
  341 | 					method);
  342 | 		}
  343 | 		return new HandlerMethod(handler, method);
  344 | 	}
  345 | 
  346 | 	/**
  347 | 	 * Extract and return the CORS configuration for the mapping.
  348 | 	 */
  349 | 	protected @Nullable CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
  350 | 		return null;
  351 | 	}
  352 | 
  353 | 	/**
  354 | 	 * Invoked after all handler methods have been detected.
  355 | 	 * @param handlerMethods a read-only map with handler methods and mappings.
  356 | 	 */
  357 | 	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
  358 | 		// Total includes detected mappings + explicit registrations via registerMapping
  359 | 		int total = handlerMethods.size();
  360 | 		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0) ) {
  361 | 			logger.debug(total + " mappings in " + formatMappingName());
  362 | 		}
  363 | 	}
  364 | 
  365 | 
  366 | 	// Handler method lookup
  367 | 
  368 | 	/**
  369 | 	 * Look up a handler method for the given request.
  370 | 	 */
  371 | 	@Override
  372 | 	protected @Nullable HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
  373 | 		String lookupPath = initLookupPath(request);
  374 | 		this.mappingRegistry.acquireReadLock();
  375 | 		try {
  376 | 			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
  377 | 			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
  378 | 		}
  379 | 		finally {
  380 | 			this.mappingRegistry.releaseReadLock();
  381 | 		}
  382 | 	}
  383 | 
  384 | 	/**
  385 | 	 * Look up the best-matching handler method for the current request.
  386 | 	 * If multiple matches are found, the best match is selected.
  387 | 	 * @param lookupPath mapping lookup path within the current servlet mapping
  388 | 	 * @param request the current request
  389 | 	 * @return the best-matching handler method, or {@code null} if no match
  390 | 	 * @see #handleMatch(Object, String, HttpServletRequest)
  391 | 	 * @see #handleNoMatch(Set, String, HttpServletRequest)
  392 | 	 */
  393 | 	protected @Nullable HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
  394 | 		List<Match> matches = new ArrayList<>();
  395 | 		List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
  396 | 		if (directPathMatches != null) {
  397 | 			addMatchingMappings(directPathMatches, matches, request);
  398 | 		}
  399 | 		if (matches.isEmpty()) {
  400 | 			addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
  401 | 		}
  402 | 		if (!matches.isEmpty()) {
  403 | 			Match bestMatch = matches.get(0);
  404 | 			if (matches.size() > 1) {
  405 | 				Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
  406 | 				matches.sort(comparator);
  407 | 				bestMatch = matches.get(0);
  408 | 				if (logger.isTraceEnabled()) {
  409 | 					logger.trace(matches.size() + " matching mappings: " + matches);
  410 | 				}
  411 | 				if (CorsUtils.isPreFlightRequest(request)) {
  412 | 					if (matches.stream().allMatch(Match::hasCorsConfig)) {
  413 | 						return PREFLIGHT_AMBIGUOUS_MATCH;
  414 | 					}
  415 | 				}
  416 | 				else {
  417 | 					Match secondBestMatch = matches.get(1);
  418 | 					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
  419 | 						Method m1 = bestMatch.getHandlerMethod().getMethod();
  420 | 						Method m2 = secondBestMatch.getHandlerMethod().getMethod();
  421 | 						String uri = request.getRequestURI();
  422 | 						throw new IllegalStateException(
  423 | 								"Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
  424 | 					}
  425 | 				}
  426 | 			}
  427 | 			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.getHandlerMethod());
  428 | 			handleMatch(bestMatch.mapping, lookupPath, request);
  429 | 			return bestMatch.getHandlerMethod();
  430 | 		}
  431 | 		else {
  432 | 			return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), lookupPath, request);
  433 | 		}
  434 | 	}
  435 | 
  436 | 	@SuppressWarnings("NullAway") // Dataflow analysis limitation
  437 | 	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
  438 | 		for (T mapping : mappings) {
  439 | 			T match = getMatchingMapping(mapping, request);
  440 | 			if (match != null) {
  441 | 				matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
  442 | 			}
  443 | 		}
  444 | 	}
  445 | 
  446 | 	/**
  447 | 	 * Invoked when a matching mapping is found.
  448 | 	 * @param mapping the matching mapping
  449 | 	 * @param lookupPath mapping lookup path within the current servlet mapping
  450 | 	 * @param request the current request
  451 | 	 */
  452 | 	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
  453 | 		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
  454 | 	}
  455 | 
  456 | 	/**
  457 | 	 * Invoked when no matching mapping is not found.
  458 | 	 * @param mappings all registered mappings
  459 | 	 * @param lookupPath mapping lookup path within the current servlet mapping
  460 | 	 * @param request the current request
  461 | 	 * @throws ServletException in case of errors
  462 | 	 */
  463 | 	protected @Nullable HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
  464 | 			throws Exception {
  465 | 
  466 | 		return null;
  467 | 	}
  468 | 
  469 | 	@Override
  470 | 	protected boolean hasCorsConfigurationSource(Object handler) {
  471 | 		return super.hasCorsConfigurationSource(handler) ||
  472 | 				(handler instanceof HandlerMethod handlerMethod &&
  473 | 						this.mappingRegistry.getCorsConfiguration(handlerMethod) != null);
  474 | 	}
  475 | 
  476 | 	@Override
  477 | 	protected @Nullable CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
  478 | 		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
  479 | 		if (handler instanceof HandlerMethod handlerMethod) {
  480 | 			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
  481 | 				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
  482 | 			}
  483 | 			else {
  484 | 				CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
  485 | 				corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
  486 | 			}
  487 | 		}
  488 | 		return corsConfig;
  489 | 	}
  490 | 
  491 | 
  492 | 	// Abstract template methods
  493 | 
  494 | 	/**
  495 | 	 * Whether the given type is a handler with handler methods.
  496 | 	 * @param beanType the type of the bean being checked
  497 | 	 * @return "true" if this a handler type, "false" otherwise.
  498 | 	 */
  499 | 	protected abstract boolean isHandler(Class<?> beanType);
  500 | 
  501 | 	/**
  502 | 	 * Provide the mapping for a handler method. A method for which no
  503 | 	 * mapping can be provided is not a handler method.
  504 | 	 * @param method the method to provide a mapping for
  505 | 	 * @param handlerType the handler type, possibly a subtype of the method's
  506 | 	 * declaring class
  507 | 	 * @return the mapping, or {@code null} if the method is not mapped
  508 | 	 */
  509 | 	protected abstract @Nullable T getMappingForMethod(Method method, Class<?> handlerType);
  510 | 
  511 | 	/**
  512 | 	 * Return the request mapping paths that are not patterns.
  513 | 	 * @since 5.3
  514 | 	 */
  515 | 	protected Set<String> getDirectPaths(T mapping) {
  516 | 		return Collections.emptySet();
  517 | 	}
  518 | 
  519 | 	/**
  520 | 	 * Check if a mapping matches the current request and return a (potentially
  521 | 	 * new) mapping with conditions relevant to the current request.
  522 | 	 * @param mapping the mapping to get a match for
  523 | 	 * @param request the current HTTP servlet request
  524 | 	 * @return the match, or {@code null} if the mapping doesn't match
  525 | 	 */
  526 | 	protected abstract @Nullable T getMatchingMapping(T mapping, HttpServletRequest request);
  527 | 
  528 | 	/**
  529 | 	 * Return a comparator for sorting matching mappings.
  530 | 	 * The returned comparator should sort 'better' matches higher.
  531 | 	 * @param request the current request
  532 | 	 * @return the comparator (never {@code null})
  533 | 	 */
  534 | 	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);
  535 | 
  536 | 
  537 | 	/**
  538 | 	 * A registry that maintains all mappings to handler methods, exposing methods
  539 | 	 * to perform lookups and providing concurrent access.
  540 | 	 *
  541 | 	 * <p>Package-private for testing purposes.
  542 | 	 */
  543 | 	class MappingRegistry {
  544 | 
  545 | 		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();
  546 | 
  547 | 		private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();
  548 | 
  549 | 		private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();
  550 | 
  551 | 		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();
  552 | 
  553 | 		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  554 | 
  555 | 		/**
  556 | 		 * Return all registrations.
  557 | 		 * @since 5.3
  558 | 		 */
  559 | 		public Map<T, MappingRegistration<T>> getRegistrations() {
  560 | 			return this.registry;
  561 | 		}
  562 | 
  563 | 		/**
  564 | 		 * Return matches for the given URL path. Not thread-safe.
  565 | 		 * @see #acquireReadLock()
  566 | 		 */
  567 | 		public @Nullable List<T> getMappingsByDirectPath(String urlPath) {
  568 | 			return this.pathLookup.get(urlPath);
  569 | 		}
  570 | 
  571 | 		/**
  572 | 		 * Return handler methods by mapping name. Thread-safe for concurrent use.
  573 | 		 */
  574 | 		public @Nullable List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
  575 | 			return this.nameLookup.get(mappingName);
  576 | 		}
  577 | 
  578 | 		/**
  579 | 		 * Return CORS configuration. Thread-safe for concurrent use.
  580 | 		 */
  581 | 		public @Nullable CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
  582 | 			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
  583 | 			return this.corsLookup.get(original != null ? original : handlerMethod);
  584 | 		}
  585 | 
  586 | 		/**
  587 | 		 * Acquire the read lock when using getMappings and getMappingsByUrl.
  588 | 		 */
  589 | 		public void acquireReadLock() {
  590 | 			this.readWriteLock.readLock().lock();
  591 | 		}
  592 | 
  593 | 		/**
  594 | 		 * Release the read lock after using getMappings and getMappingsByUrl.
  595 | 		 */
  596 | 		public void releaseReadLock() {
  597 | 			this.readWriteLock.readLock().unlock();
  598 | 		}
  599 | 
  600 | 		public void register(T mapping, Object handler, Method method) {
  601 | 			this.readWriteLock.writeLock().lock();
  602 | 			try {
  603 | 				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
  604 | 				validateMethodMapping(handlerMethod, mapping);
  605 | 
  606 | 				Set<String> directPaths = getDirectPaths(mapping);
  607 | 				for (String path : directPaths) {
  608 | 					this.pathLookup.add(path, mapping);
  609 | 				}
  610 | 
  611 | 				String name = null;
  612 | 				HandlerMethodMappingNamingStrategy<T> namingStrategy = getNamingStrategy();
  613 | 				if (namingStrategy != null) {
  614 | 					name = namingStrategy.getName(handlerMethod, mapping);
  615 | 					addMappingName(name, handlerMethod);
  616 | 				}
  617 | 
  618 | 				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
  619 | 				if (corsConfig != null) {
  620 | 					corsConfig.validateAllowCredentials();
  621 | 					corsConfig.validateAllowPrivateNetwork();
  622 | 					this.corsLookup.put(handlerMethod, corsConfig);
  623 | 				}
  624 | 
  625 | 				// Init validation flags
  626 | 				// We do this strictly after using the original instance in the CORS lookups
  627 | 				handlerMethod = handlerMethod.createWithValidateFlags();
  628 | 
  629 | 				this.registry.put(mapping,
  630 | 						new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));
  631 | 			}
  632 | 			finally {
  633 | 				this.readWriteLock.writeLock().unlock();
  634 | 			}
  635 | 		}
  636 | 
  637 | 		private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
  638 | 			MappingRegistration<T> registration = this.registry.get(mapping);
  639 | 			HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
  640 | 			if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
  641 | 				throw new IllegalStateException(
  642 | 						"Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
  643 | 						handlerMethod + "\nto " + mapping + ": There is already '" +
  644 | 						existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
  645 | 			}
  646 | 		}
  647 | 
  648 | 		private void addMappingName(String name, HandlerMethod handlerMethod) {
  649 | 			List<HandlerMethod> oldList = this.nameLookup.get(name);
  650 | 			if (oldList == null) {
  651 | 				oldList = Collections.emptyList();
  652 | 			}
  653 | 
  654 | 			for (HandlerMethod current : oldList) {
  655 | 				if (handlerMethod.equals(current)) {
  656 | 					return;
  657 | 				}
  658 | 			}
  659 | 
  660 | 			List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
  661 | 			newList.addAll(oldList);
  662 | 			newList.add(handlerMethod);
  663 | 			this.nameLookup.put(name, newList);
  664 | 		}
  665 | 
  666 | 		public void unregister(T mapping) {
  667 | 			this.readWriteLock.writeLock().lock();
  668 | 			try {
  669 | 				MappingRegistration<T> registration = this.registry.remove(mapping);
  670 | 				if (registration == null) {
  671 | 					return;
  672 | 				}
  673 | 
  674 | 				for (String path : registration.getDirectPaths()) {
  675 | 					List<T> mappings = this.pathLookup.get(path);
  676 | 					if (mappings != null) {
  677 | 						mappings.remove(registration.getMapping());
  678 | 						if (mappings.isEmpty()) {
  679 | 							this.pathLookup.remove(path);
  680 | 						}
  681 | 					}
  682 | 				}
  683 | 
  684 | 				removeMappingName(registration);
  685 | 
  686 | 				this.corsLookup.remove(registration.getHandlerMethod());
  687 | 			}
  688 | 			finally {
  689 | 				this.readWriteLock.writeLock().unlock();
  690 | 			}
  691 | 		}
  692 | 
  693 | 		private void removeMappingName(MappingRegistration<T> definition) {
  694 | 			String name = definition.getMappingName();
  695 | 			if (name == null) {
  696 | 				return;
  697 | 			}
  698 | 			HandlerMethod handlerMethod = definition.getHandlerMethod();
  699 | 			List<HandlerMethod> oldList = this.nameLookup.get(name);
  700 | 			if (oldList == null) {
  701 | 				return;
  702 | 			}
  703 | 			if (oldList.size() <= 1) {
  704 | 				this.nameLookup.remove(name);
  705 | 				return;
  706 | 			}
  707 | 			List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
  708 | 			for (HandlerMethod current : oldList) {
  709 | 				if (!current.equals(handlerMethod)) {
  710 | 					newList.add(current);
  711 | 				}
  712 | 			}
  713 | 			this.nameLookup.put(name, newList);
  714 | 		}
  715 | 	}
  716 | 
  717 | 
  718 | 	static class MappingRegistration<T> {
  719 | 
  720 | 		private final T mapping;
  721 | 
  722 | 		private final HandlerMethod handlerMethod;
  723 | 
  724 | 		private final Set<String> directPaths;
  725 | 
  726 | 		private final @Nullable String mappingName;
  727 | 
  728 | 		private final boolean corsConfig;
  729 | 
  730 | 		public MappingRegistration(T mapping, HandlerMethod handlerMethod,
  731 | 				@Nullable Set<String> directPaths, @Nullable String mappingName, boolean corsConfig) {
  732 | 
  733 | 			Assert.notNull(mapping, "Mapping must not be null");
  734 | 			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
  735 | 			this.mapping = mapping;
  736 | 			this.handlerMethod = handlerMethod;
  737 | 			this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
  738 | 			this.mappingName = mappingName;
  739 | 			this.corsConfig = corsConfig;
  740 | 		}
  741 | 
  742 | 		public T getMapping() {
  743 | 			return this.mapping;
  744 | 		}
  745 | 
  746 | 		public HandlerMethod getHandlerMethod() {
  747 | 			return this.handlerMethod;
  748 | 		}
  749 | 
  750 | 		public Set<String> getDirectPaths() {
  751 | 			return this.directPaths;
  752 | 		}
  753 | 
  754 | 		public @Nullable String getMappingName() {
  755 | 			return this.mappingName;
  756 | 		}
  757 | 
  758 | 		public boolean hasCorsConfig() {
  759 | 			return this.corsConfig;
  760 | 		}
  761 | 	}
  762 | 
  763 | 
  764 | 	/**
  765 | 	 * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
  766 | 	 * comparing the best match with a comparator in the context of the current request.
  767 | 	 */
  768 | 	private class Match {
  769 | 
  770 | 		private final T mapping;
  771 | 
  772 | 		private final MappingRegistration<T> registration;
  773 | 
  774 | 		public Match(T mapping, MappingRegistration<T> registration) {
  775 | 			this.mapping = mapping;
  776 | 			this.registration = registration;
  777 | 		}
  778 | 
  779 | 		public HandlerMethod getHandlerMethod() {
  780 | 			return this.registration.getHandlerMethod();
  781 | 		}
  782 | 
  783 | 		public boolean hasCorsConfig() {
  784 | 			return this.registration.hasCorsConfig();
  785 | 		}
  786 | 
  787 | 		@Override
  788 | 		public String toString() {
  789 | 			return this.mapping.toString();
  790 | 		}
  791 | 	}
  792 | 
  793 | 
  794 | 	private class MatchComparator implements Comparator<Match> {
  795 | 
  796 | 		private final Comparator<T> comparator;
  797 | 
  798 | 		public MatchComparator(Comparator<T> comparator) {
  799 | 			this.comparator = comparator;
  800 | 		}
  801 | 
  802 | 		@Override
  803 | 		public int compare(Match match1, Match match2) {
  804 | 			return this.comparator.compare(match1.mapping, match2.mapping);
  805 | 		}
  806 | 	}
  807 | 
  808 | 
  809 | 	private static class EmptyHandler {
  810 | 
  811 | 		@SuppressWarnings("unused")
  812 | 		public void handle() {
  813 | 			throw new UnsupportedOperationException("Not implemented");
  814 | 		}
  815 | 	}
  816 | 
  817 | }
```
