<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/cache/ContextCache.java -->
<!-- 正文引用：[[源码:ContextCache.java#L起-L止]] ；代码块头部：(src: ContextCache.java#L起-L止) -->

# ContextCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/cache/ContextCache.java` |
| 行数 | 424 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:56.414Z |

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
   19 | import java.util.Locale;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
   27 | import org.springframework.test.context.MergedContextConfiguration;
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * {@code ContextCache} defines the SPI for caching Spring
   32 |  * {@link ApplicationContext ApplicationContexts} within the
   33 |  * <em>Spring TestContext Framework</em>.
   34 |  *
   35 |  * <p>A {@code ContextCache} maintains a cache of {@code ApplicationContexts}
   36 |  * keyed by {@link MergedContextConfiguration} instances, potentially configured
   37 |  * with a {@linkplain ContextCacheUtils#retrieveMaxCacheSize maximum size},
   38 |  * {@linkplain ContextCacheUtils#retrievePauseMode() pause mode}, and custom
   39 |  * eviction policy.
   40 |  *
   41 |  * <p>As of Spring Framework 6.1, this SPI includes optional support for
   42 |  * {@linkplain #getFailureCount(MergedContextConfiguration) tracking} and
   43 |  * {@linkplain #incrementFailureCount(MergedContextConfiguration) incrementing}
   44 |  * failure counts. As of Spring Framework 7.0, this SPI includes optional support for
   45 |  * {@linkplain #registerContextUsage(MergedContextConfiguration, Class) registering} and
   46 |  * {@linkplain #unregisterContextUsage(MergedContextConfiguration, Class) unregistering}
   47 |  * context usage.
   48 |  *
   49 |  * <h3>Rationale</h3>
   50 |  * <p>Context caching can have significant performance benefits if context
   51 |  * initialization is complex. Although the initialization of a Spring context
   52 |  * itself is typically very quick, some beans in a context &mdash; for example,
   53 |  * an embedded database or a {@code LocalContainerEntityManagerFactoryBean} for
   54 |  * working with JPA &mdash; may take several seconds to initialize. Hence, it
   55 |  * often makes sense to perform that initialization only once per test suite or
   56 |  * JVM process.
   57 |  *
   58 |  * @author Sam Brannen
   59 |  * @author Juergen Hoeller
   60 |  * @since 4.2
   61 |  * @see ContextCacheUtils#retrieveMaxCacheSize()
   62 |  * @see ContextCacheUtils#retrievePauseMode()
   63 |  */
   64 | public interface ContextCache {
   65 | 
   66 | 	/**
   67 | 	 * The name of the logging category used for reporting {@code ContextCache}
   68 | 	 * statistics.
   69 | 	 */
   70 | 	String CONTEXT_CACHE_LOGGING_CATEGORY = "org.springframework.test.context.cache";
   71 | 
   72 | 	/**
   73 | 	 * The default maximum size of the context cache: {@value}.
   74 | 	 * @since 4.3
   75 | 	 * @see #MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
   76 | 	 */
   77 | 	int DEFAULT_MAX_CONTEXT_CACHE_SIZE = 32;
   78 | 
   79 | 	/**
   80 | 	 * System property used to configure the maximum size of the {@link ContextCache}
   81 | 	 * as a positive integer: {@value}.
   82 | 	 * <p>May alternatively be configured via the
   83 | 	 * {@link org.springframework.core.SpringProperties} mechanism.
   84 | 	 * <p>Note that implementations of {@code ContextCache} are not required to
   85 | 	 * actually support a maximum cache size. Consult the documentation of the
   86 | 	 * corresponding implementation for details.
   87 | 	 * @since 4.3
   88 | 	 * @see #DEFAULT_MAX_CONTEXT_CACHE_SIZE
   89 | 	 */
   90 | 	String MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME = "spring.test.context.cache.maxSize";
   91 | 
   92 | 	/**
   93 | 	 * System property used to configure whether inactive application contexts
   94 | 	 * stored in the {@link ContextCache} should be paused: {@value}.
   95 | 	 * <p>Defaults to {@code on_context_switch}. Can be set to {@code always} or
   96 | 	 * {@code never} to disable pausing of inactive application contexts &mdash;
   97 | 	 * for example:
   98 | 	 * <p>{@code -Dspring.test.context.cache.pause=never}
   99 | 	 * <p>May alternatively be configured via the
  100 | 	 * {@link org.springframework.core.SpringProperties} mechanism.
  101 | 	 * <p>Note that implementations of {@code ContextCache} are not required to
  102 | 	 * support context pausing. Consult the documentation of the corresponding
  103 | 	 * implementation for details.
  104 | 	 * @since 7.0.3
  105 | 	 * @see PauseMode
  106 | 	 * @see org.springframework.context.ConfigurableApplicationContext#pause()
  107 | 	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
  108 | 	 */
  109 | 	String CONTEXT_CACHE_PAUSE_PROPERTY_NAME = "spring.test.context.cache.pause";
  110 | 
  111 | 
  112 | 	/**
  113 | 	 * Determine whether there is a cached context for the given key.
  114 | 	 * @param key the context key; never {@code null}
  115 | 	 * @return {@code true} if the cache contains a context with the given key
  116 | 	 */
  117 | 	boolean contains(MergedContextConfiguration key);
  118 | 
  119 | 	/**
  120 | 	 * Obtain a cached {@link ApplicationContext} for the given key.
  121 | 	 * <p>If the cached application context was previously
  122 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused},
  123 | 	 * it must be
  124 | 	 * {@linkplain org.springframework.context.support.AbstractApplicationContext#restart()
  125 | 	 * restarted}. This applies to parent contexts as well.
  126 | 	 * <p>In addition, the {@linkplain #getHitCount() hit} and
  127 | 	 * {@linkplain #getMissCount() miss} counts must be updated accordingly.
  128 | 	 * @param key the context key; never {@code null}
  129 | 	 * @return the corresponding {@code ApplicationContext} instance, or {@code null}
  130 | 	 * if not found in the cache
  131 | 	 * @see #put(MergedContextConfiguration, LoadFunction)
  132 | 	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
  133 | 	 * @see #remove(MergedContextConfiguration, HierarchyMode)
  134 | 	 */
  135 | 	@Nullable ApplicationContext get(MergedContextConfiguration key);
  136 | 
  137 | 	/**
  138 | 	 * Explicitly add an {@link ApplicationContext} to the cache under the given
  139 | 	 * key, potentially honoring a custom eviction policy.
  140 | 	 * @param key the context key; never {@code null}
  141 | 	 * @param context the {@code ApplicationContext}; never {@code null}
  142 | 	 * @see #get(MergedContextConfiguration)
  143 | 	 * @see #put(MergedContextConfiguration, LoadFunction)
  144 | 	 * @deprecated since Spring Framework 7.0 in favor of
  145 | 	 * {@link #put(MergedContextConfiguration, LoadFunction)}
  146 | 	 */
  147 | 	@Deprecated(since = "7.0")
  148 | 	void put(MergedContextConfiguration key, ApplicationContext context);
  149 | 
  150 | 	/**
  151 | 	 * Explicitly add an {@link ApplicationContext} to the cache under the given
  152 | 	 * key, potentially honoring a custom eviction policy.
  153 | 	 * <p>The supplied {@link LoadFunction} will be invoked to load the
  154 | 	 * {@code ApplicationContext}.
  155 | 	 * <p>Concrete implementations which honor a custom eviction policy must
  156 | 	 * override this method to ensure that an evicted context is removed from the
  157 | 	 * cache and closed before a new context is loaded via the supplied
  158 | 	 * {@code LoadFunction}.
  159 | 	 * @param key the context key; never {@code null}
  160 | 	 * @param loadFunction a function which loads the context for the supplied key;
  161 | 	 * never {@code null}
  162 | 	 * @return the {@code ApplicationContext}; never {@code null}
  163 | 	 * @since 7.0
  164 | 	 * @see #get(MergedContextConfiguration)
  165 | 	 * @see #put(MergedContextConfiguration, ApplicationContext)
  166 | 	 */
  167 | 	default ApplicationContext put(MergedContextConfiguration key, LoadFunction loadFunction) {
  168 | 		Assert.notNull(key, "Key must not be null");
  169 | 		Assert.notNull(loadFunction, "LoadFunction must not be null");
  170 | 
  171 | 		ApplicationContext applicationContext = loadFunction.loadContext(key);
  172 | 		Assert.state(applicationContext != null, "LoadFunction must return a non-null ApplicationContext");
  173 | 		put(key, applicationContext);
  174 | 		return applicationContext;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Remove the context with the given key from the cache and explicitly
  179 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#close() close}
  180 | 	 * it if it is an instance of {@code ConfigurableApplicationContext}.
  181 | 	 * <p>Generally speaking, this method should be called to properly evict
  182 | 	 * a context from the cache (for example, due to a custom eviction policy) or if
  183 | 	 * the state of a singleton bean has been modified, potentially affecting
  184 | 	 * future interaction with the context.
  185 | 	 * <p>In addition, the semantics of the supplied {@code HierarchyMode} must
  186 | 	 * be honored. See the Javadoc for {@link HierarchyMode} for details.
  187 | 	 * @param key the context key; never {@code null}
  188 | 	 * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
  189 | 	 * is not part of a hierarchy
  190 | 	 */
  191 | 	void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode);
  192 | 
  193 | 	/**
  194 | 	 * Get the failure count for the given key.
  195 | 	 * <p>A <em>failure</em> is any attempt to load the {@link ApplicationContext}
  196 | 	 * for the given key that results in an exception.
  197 | 	 * <p>The default implementation of this method always returns {@code 0}.
  198 | 	 * Concrete implementations are therefore highly encouraged to override this
  199 | 	 * method and {@link #incrementFailureCount(MergedContextConfiguration)} with
  200 | 	 * appropriate behavior. Note that the standard {@code ContextContext}
  201 | 	 * implementation in Spring overrides these methods appropriately.
  202 | 	 * @param key the context key; never {@code null}
  203 | 	 * @since 6.1
  204 | 	 * @see #incrementFailureCount(MergedContextConfiguration)
  205 | 	 */
  206 | 	default int getFailureCount(MergedContextConfiguration key) {
  207 | 		return 0;
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * Increment the failure count for the given key.
  212 | 	 * <p>The default implementation of this method does nothing. Concrete
  213 | 	 * implementations are therefore highly encouraged to override this
  214 | 	 * method and {@link #getFailureCount(MergedContextConfiguration)} with
  215 | 	 * appropriate behavior. Note that the standard {@code ContextContext}
  216 | 	 * implementation in Spring overrides these methods appropriately.
  217 | 	 * @param key the context key; never {@code null}
  218 | 	 * @since 6.1
  219 | 	 * @see #getFailureCount(MergedContextConfiguration)
  220 | 	 */
  221 | 	default void incrementFailureCount(MergedContextConfiguration key) {
  222 | 		/* no-op */
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * Register usage of the {@link ApplicationContext} for the supplied
  227 | 	 * {@link MergedContextConfiguration} and any of its parents.
  228 | 	 * <p>The default implementation of this method does nothing. Concrete
  229 | 	 * implementations are therefore highly encouraged to override this
  230 | 	 * method, {@link #unregisterContextUsage(MergedContextConfiguration, Class)},
  231 | 	 * and {@link #getContextUsageCount()} with appropriate behavior. Note that
  232 | 	 * the standard {@code ContextContext} implementation in Spring overrides
  233 | 	 * these methods appropriately.
  234 | 	 * @param key the context key; never {@code null}
  235 | 	 * @param testClass the test class that is using the application context(s)
  236 | 	 * @since 7.0
  237 | 	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
  238 | 	 * @see #getContextUsageCount()
  239 | 	 */
  240 | 	default void registerContextUsage(MergedContextConfiguration key, Class<?> testClass) {
  241 | 		/* no-op */
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * Unregister usage of the {@link ApplicationContext} for the supplied
  246 | 	 * {@link MergedContextConfiguration} and any of its parents.
  247 | 	 * <p>If no other test classes are actively using the same application
  248 | 	 * context(s), the application context(s) should be
  249 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause()
  250 | 	 * paused} according to the configured {@link PauseMode}.
  251 | 	 * <p>The default implementation of this method does nothing. Concrete
  252 | 	 * implementations are therefore highly encouraged to override this
  253 | 	 * method, {@link #registerContextUsage(MergedContextConfiguration, Class)},
  254 | 	 * and {@link #getContextUsageCount()} with appropriate behavior. Note that
  255 | 	 * the standard {@code ContextContext} implementation in Spring overrides
  256 | 	 * these methods appropriately.
  257 | 	 * @param key the context key; never {@code null}
  258 | 	 * @param testClass the test class that is no longer using the application context(s)
  259 | 	 * @since 7.0
  260 | 	 * @see #registerContextUsage(MergedContextConfiguration, Class)
  261 | 	 * @see #getContextUsageCount()
  262 | 	 */
  263 | 	default void unregisterContextUsage(MergedContextConfiguration key, Class<?> testClass) {
  264 | 		/* no-op */
  265 | 	}
  266 | 
  267 | 	/**
  268 | 	 * Determine the number of contexts within the cache that are currently in use.
  269 | 	 * <p>The default implementation of this method always returns {@code 0}.
  270 | 	 * Concrete implementations are therefore highly encouraged to override this
  271 | 	 * method, {@link #registerContextUsage(MergedContextConfiguration, Class)},
  272 | 	 * and {@link #unregisterContextUsage(MergedContextConfiguration, Class)} with
  273 | 	 * appropriate behavior. Note that the standard {@code ContextContext}
  274 | 	 * implementation in Spring overrides these methods appropriately.
  275 | 	 * @since 7.0
  276 | 	 * @see #registerContextUsage(MergedContextConfiguration, Class)
  277 | 	 * @see #unregisterContextUsage(MergedContextConfiguration, Class)
  278 | 	 */
  279 | 	default int getContextUsageCount() {
  280 | 		return 0;
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * Determine the number of contexts currently stored in the cache.
  285 | 	 * <p>If the cache contains more than {@code Integer.MAX_VALUE} elements,
  286 | 	 * this method must return {@code Integer.MAX_VALUE}.
  287 | 	 */
  288 | 	int size();
  289 | 
  290 | 	/**
  291 | 	 * Determine the number of parent contexts currently tracked within the cache.
  292 | 	 */
  293 | 	int getParentContextCount();
  294 | 
  295 | 	/**
  296 | 	 * Get the overall hit count for this cache.
  297 | 	 * <p>A <em>hit</em> is any access to the cache that returns a non-null
  298 | 	 * context for the queried key.
  299 | 	 */
  300 | 	int getHitCount();
  301 | 
  302 | 	/**
  303 | 	 * Get the overall miss count for this cache.
  304 | 	 * <p>A <em>miss</em> is any access to the cache that returns a {@code null}
  305 | 	 * context for the queried key.
  306 | 	 */
  307 | 	int getMissCount();
  308 | 
  309 | 	/**
  310 | 	 * Reset all state maintained by this cache including statistics.
  311 | 	 * @see #clear()
  312 | 	 * @see #clearStatistics()
  313 | 	 */
  314 | 	void reset();
  315 | 
  316 | 	/**
  317 | 	 * Clear all contexts from the cache, explicitly
  318 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#close() closing}
  319 | 	 * each context that is an instance of {@code ConfigurableApplicationContext}
  320 | 	 * and clearing context hierarchy information as well.
  321 | 	 */
  322 | 	void clear();
  323 | 
  324 | 	/**
  325 | 	 * Clear {@linkplain #getHitCount() hit count} and {@linkplain #getMissCount()
  326 | 	 * miss count} statistics for the cache (i.e., reset counters to zero).
  327 | 	 */
  328 | 	void clearStatistics();
  329 | 
  330 | 	/**
  331 | 	 * Log the statistics for this {@code ContextCache} at {@code DEBUG} level
  332 | 	 * using the {@value #CONTEXT_CACHE_LOGGING_CATEGORY} logging category.
  333 | 	 * <p>The following information should be logged.
  334 | 	 * <ul>
  335 | 	 * <li>name of the concrete {@code ContextCache} implementation</li>
  336 | 	 * <li>{@linkplain #size}</li>
  337 | 	 * <li>{@linkplain #getContextUsageCount() context usage count}</li>
  338 | 	 * <li>{@linkplain #getParentContextCount() parent context count}</li>
  339 | 	 * <li>{@linkplain #getHitCount() hit count}</li>
  340 | 	 * <li>{@linkplain #getMissCount() miss count}</li>
  341 | 	 * <li>any other information useful for monitoring the state of this cache</li>
  342 | 	 * </ul>
  343 | 	 */
  344 | 	void logStatistics();
  345 | 
  346 | 
  347 | 	/**
  348 | 	 * Represents a function that loads an {@link ApplicationContext}.
  349 | 	 *
  350 | 	 * @since 7.0
  351 | 	 */
  352 | 	@FunctionalInterface
  353 | 	interface LoadFunction {
  354 | 
  355 | 		/**
  356 | 		 * Load a new {@link ApplicationContext} based on the supplied
  357 | 		 * {@link MergedContextConfiguration} and return the context in a fully
  358 | 		 * <em>refreshed</em> state.
  359 | 		 * @param mergedConfig the merged context configuration to use to load the
  360 | 		 * application context
  361 | 		 * @return a new application context; never {@code null}
  362 | 		 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
  363 | 		 */
  364 | 		ApplicationContext loadContext(MergedContextConfiguration mergedConfig);
  365 | 
  366 | 	}
  367 | 
  368 | 	/**
  369 | 	 * Enumeration of <em>modes</em> that dictate whether inactive application contexts
  370 | 	 * stored in the {@link ContextCache} should be
  371 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused}.
  372 | 	 *
  373 | 	 * @since 7.0.3
  374 | 	 * @see #ALWAYS
  375 | 	 * @see #ON_CONTEXT_SWITCH
  376 | 	 * @see #NEVER
  377 | 	 * @see ContextCache#CONTEXT_CACHE_PAUSE_PROPERTY_NAME
  378 | 	 */
  379 | 	enum PauseMode {
  380 | 
  381 | 		/**
  382 | 		 * Always pause inactive application contexts.
  383 | 		 */
  384 | 		ALWAYS,
  385 | 
  386 | 		/**
  387 | 		 * Only pause inactive application contexts if the next context
  388 | 		 * retrieved from the cache is a different context.
  389 | 		 */
  390 | 		ON_CONTEXT_SWITCH,
  391 | 
  392 | 		/**
  393 | 		 * Never pause inactive application contexts, effectively disabling the
  394 | 		 * pausing feature of the {@link ContextCache}.
  395 | 		 */
  396 | 		NEVER;
  397 | 
  398 | 
  399 | 		/**
  400 | 		 * Get the {@code PauseMode} enum constant with the supplied name,
  401 | 		 * {@linkplain String#strip() stripped} and ignoring case.
  402 | 		 * @param name the name of the enum constant to retrieve
  403 | 		 * @return the corresponding enum constant or {@code null} if not found
  404 | 		 * @see PauseMode#valueOf(String)
  405 | 		 */
  406 | 		public static @Nullable PauseMode from(@Nullable String name) {
  407 | 			if (name == null) {
  408 | 				return null;
  409 | 			}
  410 | 			try {
  411 | 				return PauseMode.valueOf(name.strip().toUpperCase(Locale.ROOT));
  412 | 			}
  413 | 			catch (IllegalArgumentException ex) {
  414 | 				Log logger = LogFactory.getLog(PauseMode.class);
  415 | 				if (logger.isDebugEnabled()) {
  416 | 					logger.debug("Failed to parse PauseMode from '%s': %s"
  417 | 							.formatted(name, ex.getMessage()));
  418 | 				}
  419 | 				return null;
  420 | 			}
  421 | 		}
  422 | 	}
  423 | 
  424 | }
```
