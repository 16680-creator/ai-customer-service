<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/cache/DefaultContextCache.java -->
<!-- 正文引用：[[源码:DefaultContextCache.java#L起-L止]] ；代码块头部：(src: DefaultContextCache.java#L起-L止) -->

# DefaultContextCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/cache/DefaultContextCache.java` |
| 行数 | 470 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:56.564Z |

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
   17 | package org.springframework.test.context.cache;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.HashSet;
   22 | import java.util.Iterator;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | import java.util.concurrent.ConcurrentHashMap;
   29 | import java.util.concurrent.atomic.AtomicInteger;
   30 | 
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.context.ApplicationContext;
   36 | import org.springframework.context.ConfigurableApplicationContext;
   37 | import org.springframework.core.style.ToStringCreator;
   38 | import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
   39 | import org.springframework.test.context.MergedContextConfiguration;
   40 | import org.springframework.util.Assert;
   41 | 
   42 | /**
   43 |  * Default implementation of the {@link ContextCache} API.
   44 |  *
   45 |  * <p>Uses a synchronized {@link Map} configured with a maximum size
   46 |  * and a <em>least recently used</em> (LRU) eviction policy to cache
   47 |  * {@link ApplicationContext} instances.
   48 |  *
   49 |  * <p>The maximum size may be supplied as a {@linkplain #DefaultContextCache(int)
   50 |  * constructor argument} or set via a system property or Spring property named
   51 |  * {@value ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME}.
   52 |  *
   53 |  * <p>The {@link PauseMode} may be supplied as a {@linkplain #DefaultContextCache(int, PauseMode)
   54 |  * constructor argument} or set via a system property or Spring property named
   55 |  * {@value ContextCache#CONTEXT_CACHE_PAUSE_PROPERTY_NAME}.
   56 |  *
   57 |  * @author Sam Brannen
   58 |  * @author Juergen Hoeller
   59 |  * @since 2.5
   60 |  * @see ContextCacheUtils#retrieveMaxCacheSize()
   61 |  * @see ContextCacheUtils#retrievePauseMode()
   62 |  */
   63 | public class DefaultContextCache implements ContextCache {
   64 | 
   65 | 	private static final Log statsLogger = LogFactory.getLog(CONTEXT_CACHE_LOGGING_CATEGORY);
   66 | 
   67 | 
   68 | 	/**
   69 | 	 * Map of context keys to Spring {@code ApplicationContext} instances.
   70 | 	 */
   71 | 	private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
   72 | 			Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true));
   73 | 
   74 | 	/**
   75 | 	 * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
   76 | 	 * of context hierarchies. This information is used for determining which subtrees
   77 | 	 * need to be recursively removed and closed when removing a context that is a parent
   78 | 	 * of other contexts.
   79 | 	 */
   80 | 	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
   81 | 			new ConcurrentHashMap<>(32);
   82 | 
   83 | 	/**
   84 | 	 * Map of context keys to active test classes (i.e., test classes that are actively
   85 | 	 * using the corresponding {@link ApplicationContext}).
   86 | 	 * @since 7.0
   87 | 	 */
   88 | 	private final Map<MergedContextConfiguration, Set<Class<?>>> contextUsageMap = new ConcurrentHashMap<>(32);
   89 | 
   90 | 	/**
   91 | 	 * Set of keys for contexts that are currently unused and are therefore
   92 | 	 * candidates for pausing on context switch.
   93 | 	 * @since 7.0.3
   94 | 	 */
   95 | 	private final Set<MergedContextConfiguration> unusedContexts = new LinkedHashSet<>(4);
   96 | 
   97 | 	/**
   98 | 	 * Map of context keys to context load failure counts.
   99 | 	 * @since 6.1
  100 | 	 */
  101 | 	private final Map<MergedContextConfiguration, Integer> failureCounts = new ConcurrentHashMap<>(32);
  102 | 
  103 | 	private final AtomicInteger totalFailureCount = new AtomicInteger();
  104 | 
  105 | 	private final int maxSize;
  106 | 
  107 | 	private final PauseMode pauseMode;
  108 | 
  109 | 	private final AtomicInteger hitCount = new AtomicInteger();
  110 | 
  111 | 	private final AtomicInteger missCount = new AtomicInteger();
  112 | 
  113 | 
  114 | 	/**
  115 | 	 * Create a new {@code DefaultContextCache} using the maximum cache size
  116 | 	 * obtained via {@link ContextCacheUtils#retrieveMaxCacheSize()} and the
  117 | 	 * {@link PauseMode} obtained via {@link ContextCacheUtils#retrievePauseMode()}.
  118 | 	 * @since 4.3
  119 | 	 * @see #DefaultContextCache(int)
  120 | 	 * @see #DefaultContextCache(int, PauseMode)
  121 | 	 * @see ContextCacheUtils#retrieveMaxCacheSize()
  122 | 	 * @see ContextCacheUtils#retrievePauseMode()
  123 | 	 */
  124 | 	public DefaultContextCache() {
  125 | 		this(ContextCacheUtils.retrieveMaxCacheSize());
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Create a new {@code DefaultContextCache} using the supplied maximum
  130 | 	 * cache size and the {@link PauseMode} obtained via
  131 | 	 * {@link ContextCacheUtils#retrievePauseMode()}.
  132 | 	 * @param maxSize the maximum cache size
  133 | 	 * @throws IllegalArgumentException if the supplied {@code maxSize} value
  134 | 	 * is not positive
  135 | 	 * @since 4.3
  136 | 	 * @see #DefaultContextCache()
  137 | 	 * @see #DefaultContextCache(int, PauseMode)
  138 | 	 * @see ContextCacheUtils#retrievePauseMode()
  139 | 	 */
  140 | 	public DefaultContextCache(int maxSize) {
  141 | 		this(maxSize, ContextCacheUtils.retrievePauseMode());
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Create a new {@code DefaultContextCache} using the supplied maximum
  146 | 	 * cache size and {@link PauseMode}.
  147 | 	 * @param maxSize the maximum cache size
  148 | 	 * @param pauseMode the {@code PauseMode} to use
  149 | 	 * @throws IllegalArgumentException if the supplied {@code maxSize} value
  150 | 	 * is not positive or if the supplied {@code PauseMode} is {@code null}
  151 | 	 * @since 7.0.3
  152 | 	 * @see #DefaultContextCache()
  153 | 	 */
  154 | 	public DefaultContextCache(int maxSize, PauseMode pauseMode) {
  155 | 		Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
  156 | 		Assert.notNull(pauseMode, "'pauseMode' must not be null");
  157 | 		this.maxSize = maxSize;
  158 | 		this.pauseMode = pauseMode;
  159 | 	}
  160 | 
  161 | 
  162 | 	@Override
  163 | 	public boolean contains(MergedContextConfiguration key) {
  164 | 		Assert.notNull(key, "Key must not be null");
  165 | 		return this.contextMap.containsKey(key);
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public @Nullable ApplicationContext get(MergedContextConfiguration key) {
  170 | 		Assert.notNull(key, "Key must not be null");
  171 | 		ApplicationContext context = this.contextMap.get(key);
  172 | 		if (context == null) {
  173 | 			this.missCount.incrementAndGet();
  174 | 		}
  175 | 		else {
  176 | 			this.hitCount.incrementAndGet();
  177 | 			pauseOnContextSwitchIfNecessary(key);
  178 | 			restartContextIfNecessary(context);
  179 | 		}
  180 | 		return context;
  181 | 	}
  182 | 
  183 | 	private void restartContextIfNecessary(ApplicationContext context) {
  184 | 		if (this.pauseMode != PauseMode.NEVER) {
  185 | 			// Recurse up the context hierarchy first.
  186 | 			ApplicationContext parent = context.getParent();
  187 | 			if (parent != null) {
  188 | 				restartContextIfNecessary(parent);
  189 | 			}
  190 | 			if (context instanceof ConfigurableApplicationContext cac && !cac.isRunning()) {
  191 | 				cac.restart();
  192 | 			}
  193 | 		}
  194 | 	}
  195 | 
  196 | 	@Override
  197 | 	@Deprecated(since = "7.0")
  198 | 	public void put(MergedContextConfiguration key, ApplicationContext context) {
  199 | 		Assert.notNull(key, "Key must not be null");
  200 | 		Assert.notNull(context, "ApplicationContext must not be null");
  201 | 
  202 | 		evictLruContextIfNecessary();
  203 | 		pauseOnContextSwitchIfNecessary(key);
  204 | 		putInternal(key, context);
  205 | 	}
  206 | 
  207 | 	@Override
  208 | 	public ApplicationContext put(MergedContextConfiguration key, LoadFunction loadFunction) {
  209 | 		Assert.notNull(key, "Key must not be null");
  210 | 		Assert.notNull(loadFunction, "LoadFunction must not be null");
  211 | 
  212 | 		evictLruContextIfNecessary();
  213 | 		pauseOnContextSwitchIfNecessary(key);
  214 | 		ApplicationContext context = loadFunction.loadContext(key);
  215 | 		Assert.state(context != null, "LoadFunction must return a non-null ApplicationContext");
  216 | 		putInternal(key, context);
  217 | 		return context;
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * Evict the least recently used (LRU) context if necessary.
  222 | 	 * @since 7.0
  223 | 	 */
  224 | 	private void evictLruContextIfNecessary() {
  225 | 		if (this.contextMap.size() >= this.maxSize) {
  226 | 			Iterator<MergedContextConfiguration> iterator = this.contextMap.keySet().iterator();
  227 | 			Assert.state(iterator.hasNext(), "Failed to retrieve LRU context");
  228 | 			// The least recently used (LRU) key is the first/head in a LinkedHashMap
  229 | 			// configured for access-order iteration order.
  230 | 			MergedContextConfiguration lruKey = iterator.next();
  231 | 			remove(lruKey, HierarchyMode.CURRENT_LEVEL);
  232 | 		}
  233 | 	}
  234 | 
  235 | 	private void putInternal(MergedContextConfiguration key, ApplicationContext context) {
  236 | 		this.contextMap.put(key, context);
  237 | 
  238 | 		// Update context hierarchy map.
  239 | 		MergedContextConfiguration child = key;
  240 | 		MergedContextConfiguration parent = child.getParent();
  241 | 		while (parent != null) {
  242 | 			Set<MergedContextConfiguration> set = this.hierarchyMap.computeIfAbsent(parent, k -> new HashSet<>());
  243 | 			set.add(child);
  244 | 			child = parent;
  245 | 			parent = child.getParent();
  246 | 		}
  247 | 	}
  248 | 
  249 | 	@Override
  250 | 	public void registerContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
  251 | 		// Recurse up the context hierarchy first.
  252 | 		MergedContextConfiguration parent = mergedConfig.getParent();
  253 | 		if (parent != null) {
  254 | 			registerContextUsage(parent, testClass);
  255 | 		}
  256 | 		getActiveTestClasses(mergedConfig).add(testClass);
  257 | 	}
  258 | 
  259 | 	@Override
  260 | 	public void unregisterContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
  261 | 		ApplicationContext context = this.contextMap.get(mergedConfig);
  262 | 		Assert.state(context != null, "ApplicationContext must not be null for: " + mergedConfig);
  263 | 
  264 | 		Set<Class<?>> activeTestClasses = getActiveTestClasses(mergedConfig);
  265 | 		activeTestClasses.remove(testClass);
  266 | 		if (activeTestClasses.isEmpty()) {
  267 | 			switch (this.pauseMode) {
  268 | 				case ALWAYS -> pauseIfNecessary(context);
  269 | 				case ON_CONTEXT_SWITCH -> this.unusedContexts.add(mergedConfig);
  270 | 			}
  271 | 			this.contextUsageMap.remove(mergedConfig);
  272 | 		}
  273 | 
  274 | 		// Recurse up the context hierarchy last.
  275 | 		MergedContextConfiguration parent = mergedConfig.getParent();
  276 | 		if (parent != null) {
  277 | 			unregisterContextUsage(parent, testClass);
  278 | 		}
  279 | 	}
  280 | 
  281 | 	private Set<Class<?>> getActiveTestClasses(MergedContextConfiguration mergedConfig) {
  282 | 		return this.contextUsageMap.computeIfAbsent(mergedConfig, key -> new HashSet<>());
  283 | 	}
  284 | 
  285 | 	private boolean pauseOnContextSwitch() {
  286 | 		return (this.pauseMode == PauseMode.ON_CONTEXT_SWITCH);
  287 | 	}
  288 | 
  289 | 	private void pauseOnContextSwitchIfNecessary(MergedContextConfiguration activeContextKey) {
  290 | 		if (pauseOnContextSwitch()) {
  291 | 			removeFromUnusedContexts(activeContextKey);
  292 | 			for (MergedContextConfiguration unusedContextKey : this.unusedContexts) {
  293 | 				pauseIfNecessary(this.contextMap.get(unusedContextKey));
  294 | 			}
  295 | 			this.unusedContexts.clear();
  296 | 		}
  297 | 	}
  298 | 
  299 | 	/**
  300 | 	 * Remove the supplied key and any keys for parent contexts from the unused
  301 | 	 * contexts set. This effectively stops tracking the context (or context
  302 | 	 * hierarchy) as unused.
  303 | 	 */
  304 | 	private void removeFromUnusedContexts(MergedContextConfiguration key) {
  305 | 		do {
  306 | 			this.unusedContexts.remove(key);
  307 | 			key = key.getParent();
  308 | 		} while (key != null);
  309 | 	}
  310 | 
  311 | 	private static void pauseIfNecessary(@Nullable ApplicationContext context) {
  312 | 		if (context instanceof ConfigurableApplicationContext cac && cac.isRunning()) {
  313 | 			cac.pause();
  314 | 		}
  315 | 	}
  316 | 
  317 | 	@Override
  318 | 	public void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode) {
  319 | 		Assert.notNull(key, "Key must not be null");
  320 | 
  321 | 		// startKey is the level at which to begin clearing the cache,
  322 | 		// depending on the configured hierarchy mode.
  323 | 		MergedContextConfiguration startKey = key;
  324 | 		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
  325 | 			MergedContextConfiguration parent = startKey.getParent();
  326 | 			while (parent != null) {
  327 | 				startKey = parent;
  328 | 				parent = startKey.getParent();
  329 | 			}
  330 | 		}
  331 | 
  332 | 		List<MergedContextConfiguration> removedContexts = new ArrayList<>();
  333 | 		remove(removedContexts, startKey);
  334 | 
  335 | 		// Remove all remaining references to any removed contexts from the
  336 | 		// hierarchy map.
  337 | 		for (MergedContextConfiguration currentKey : removedContexts) {
  338 | 			for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
  339 | 				children.remove(currentKey);
  340 | 			}
  341 | 		}
  342 | 
  343 | 		// Remove empty entries from the hierarchy map.
  344 | 		this.hierarchyMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  345 | 	}
  346 | 
  347 | 	private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
  348 | 		Assert.notNull(key, "Key must not be null");
  349 | 
  350 | 		Set<MergedContextConfiguration> children = this.hierarchyMap.get(key);
  351 | 		if (children != null) {
  352 | 			for (MergedContextConfiguration child : children) {
  353 | 				// Recurse through lower levels
  354 | 				remove(removedContexts, child);
  355 | 			}
  356 | 			// Remove the set of children for the current context from the hierarchy map.
  357 | 			this.hierarchyMap.remove(key);
  358 | 		}
  359 | 
  360 | 		// Physically remove and close leaf nodes first (i.e., on the way back up the
  361 | 		// stack as opposed to prior to the recursive call).
  362 | 		ApplicationContext context = this.contextMap.remove(key);
  363 | 		this.contextUsageMap.remove(key);
  364 | 		if (pauseOnContextSwitch()) {
  365 | 			this.unusedContexts.remove(key);
  366 | 		}
  367 | 		if (context instanceof ConfigurableApplicationContext cac) {
  368 | 			cac.close();
  369 | 		}
  370 | 		removedContexts.add(key);
  371 | 	}
  372 | 
  373 | 	@Override
  374 | 	public int getFailureCount(MergedContextConfiguration key) {
  375 | 		return this.failureCounts.getOrDefault(key, 0);
  376 | 	}
  377 | 
  378 | 	@Override
  379 | 	public void incrementFailureCount(MergedContextConfiguration key) {
  380 | 		this.totalFailureCount.incrementAndGet();
  381 | 		this.failureCounts.merge(key, 1, Integer::sum);
  382 | 	}
  383 | 
  384 | 	@Override
  385 | 	public int size() {
  386 | 		return this.contextMap.size();
  387 | 	}
  388 | 
  389 | 	/**
  390 | 	 * Get the maximum size of this cache.
  391 | 	 */
  392 | 	public int getMaxSize() {
  393 | 		return this.maxSize;
  394 | 	}
  395 | 
  396 | 	@Override
  397 | 	public int getContextUsageCount() {
  398 | 		return this.contextUsageMap.size();
  399 | 	}
  400 | 
  401 | 	@Override
  402 | 	public int getParentContextCount() {
  403 | 		return this.hierarchyMap.size();
  404 | 	}
  405 | 
  406 | 	@Override
  407 | 	public int getHitCount() {
  408 | 		return this.hitCount.get();
  409 | 	}
  410 | 
  411 | 	@Override
  412 | 	public int getMissCount() {
  413 | 		return this.missCount.get();
  414 | 	}
  415 | 
  416 | 	@Override
  417 | 	public void reset() {
  418 | 		synchronized (this.contextMap) {
  419 | 			clear();
  420 | 			clearStatistics();
  421 | 			this.totalFailureCount.set(0);
  422 | 			this.failureCounts.clear();
  423 | 		}
  424 | 	}
  425 | 
  426 | 	@Override
  427 | 	public void clear() {
  428 | 		synchronized (this.contextMap) {
  429 | 			for (MergedContextConfiguration key : new ArrayList<>(this.contextMap.keySet())) {
  430 | 				remove(key, HierarchyMode.CURRENT_LEVEL);
  431 | 			}
  432 | 		}
  433 | 	}
  434 | 
  435 | 	@Override
  436 | 	public void clearStatistics() {
  437 | 		synchronized (this.contextMap) {
  438 | 			this.hitCount.set(0);
  439 | 			this.missCount.set(0);
  440 | 		}
  441 | 	}
  442 | 
  443 | 	@Override
  444 | 	public void logStatistics() {
  445 | 		if (statsLogger.isDebugEnabled()) {
  446 | 			statsLogger.debug("Spring test ApplicationContext cache statistics: " + this);
  447 | 		}
  448 | 	}
  449 | 
  450 | 	/**
  451 | 	 * Generate a text string containing the implementation type of this
  452 | 	 * cache and its statistics.
  453 | 	 * <p>The string returned by this method contains all information
  454 | 	 * required for compliance with the contract for {@link #logStatistics()}.
  455 | 	 * @return a string representation of this cache, including statistics
  456 | 	 */
  457 | 	@Override
  458 | 	public String toString() {
  459 | 		return new ToStringCreator(this)
  460 | 				.append("size", size())
  461 | 				.append("maxSize", getMaxSize())
  462 | 				.append("contextUsageCount", getContextUsageCount())
  463 | 				.append("parentContextCount", getParentContextCount())
  464 | 				.append("hitCount", getHitCount())
  465 | 				.append("missCount", getMissCount())
  466 | 				.append("failureCount", this.totalFailureCount)
  467 | 				.toString();
  468 | 	}
  469 | 
  470 | }
```
