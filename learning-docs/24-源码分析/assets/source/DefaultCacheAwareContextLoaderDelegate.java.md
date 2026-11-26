<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/cache/DefaultCacheAwareContextLoaderDelegate.java -->
<!-- 正文引用：[[源码:DefaultCacheAwareContextLoaderDelegate.java#L起-L止]] ；代码块头部：(src: DefaultCacheAwareContextLoaderDelegate.java#L起-L止) -->

# DefaultCacheAwareContextLoaderDelegate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/cache/DefaultCacheAwareContextLoaderDelegate.java` |
| 行数 | 327 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.839Z |

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
   19 | import java.util.List;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aot.AotDetector;
   26 | import org.springframework.context.ApplicationContext;
   27 | import org.springframework.context.ApplicationContextInitializer;
   28 | import org.springframework.context.ConfigurableApplicationContext;
   29 | import org.springframework.context.support.GenericApplicationContext;
   30 | import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
   31 | import org.springframework.test.context.ApplicationContextFailureProcessor;
   32 | import org.springframework.test.context.CacheAwareContextLoaderDelegate;
   33 | import org.springframework.test.context.ContextLoadException;
   34 | import org.springframework.test.context.ContextLoader;
   35 | import org.springframework.test.context.MergedContextConfiguration;
   36 | import org.springframework.test.context.SmartContextLoader;
   37 | import org.springframework.test.context.aot.AotContextLoader;
   38 | import org.springframework.test.context.aot.AotTestContextInitializers;
   39 | import org.springframework.test.context.aot.TestContextAotException;
   40 | import org.springframework.test.context.util.TestContextSpringFactoriesUtils;
   41 | import org.springframework.util.Assert;
   42 | 
   43 | /**
   44 |  * Default implementation of the {@link CacheAwareContextLoaderDelegate} strategy.
   45 |  *
   46 |  * <p>To use a static {@link DefaultContextCache}, invoke the
   47 |  * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
   48 |  * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
   49 |  * and provide a custom {@link ContextCache} implementation.
   50 |  *
   51 |  * <p>As of Spring Framework 6.0, this class loads {@link ApplicationContextFailureProcessor}
   52 |  * implementations via the {@link org.springframework.core.io.support.SpringFactoriesLoader
   53 |  * SpringFactoriesLoader} mechanism and delegates to them in
   54 |  * {@link #loadContext(MergedContextConfiguration)} to process context load failures.
   55 |  *
   56 |  * <p>As of Spring Framework 6.1, this class supports the <em>failure threshold</em>
   57 |  * feature described in {@link CacheAwareContextLoaderDelegate#loadContext},
   58 |  * delegating to {@link ContextCacheUtils#retrieveContextFailureThreshold()} to
   59 |  * obtain the threshold value to use.
   60 |  *
   61 |  * <p>As of Spring Framework 7.0, this class provides support for
   62 |  * {@linkplain #registerContextUsage(MergedContextConfiguration, Class) registering} and
   63 |  * {@linkplain #unregisterContextUsage(MergedContextConfiguration, Class) unregistering}
   64 |  * context usage.
   65 |  *
   66 |  * @author Sam Brannen
   67 |  * @since 4.1
   68 |  */
   69 | public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {
   70 | 
   71 | 	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);
   72 | 
   73 | 	/**
   74 | 	 * Default static cache of Spring application contexts.
   75 | 	 */
   76 | 	static final ContextCache defaultContextCache = new DefaultContextCache();
   77 | 
   78 | 
   79 | 	private final List<ApplicationContextFailureProcessor> contextFailureProcessors = TestContextSpringFactoriesUtils
   80 | 			.loadFactoryImplementations(ApplicationContextFailureProcessor.class);
   81 | 
   82 | 	private final AotTestContextInitializers aotTestContextInitializers = new AotTestContextInitializers();
   83 | 
   84 | 	private final ContextCache contextCache;
   85 | 
   86 | 	/**
   87 | 	 * The configured failure threshold for errors encountered while attempting to
   88 | 	 * load an {@link ApplicationContext}.
   89 | 	 * @since 6.1
   90 | 	 */
   91 | 	private final int failureThreshold;
   92 | 
   93 | 
   94 | 	/**
   95 | 	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using a
   96 | 	 * static {@link DefaultContextCache}.
   97 | 	 * <p>The default cache is static so that each context can be cached and
   98 | 	 * reused for all subsequent tests that declare the same unique context
   99 | 	 * configuration within the same JVM process.
  100 | 	 * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
  101 | 	 */
  102 | 	public DefaultCacheAwareContextLoaderDelegate() {
  103 | 		this(defaultContextCache);
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
  108 | 	 * supplied {@link ContextCache} and the default or user-configured context
  109 | 	 * failure threshold.
  110 | 	 * @see #DefaultCacheAwareContextLoaderDelegate()
  111 | 	 * @see ContextCacheUtils#retrieveContextFailureThreshold()
  112 | 	 */
  113 | 	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
  114 | 		this(contextCache, ContextCacheUtils.retrieveContextFailureThreshold());
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
  119 | 	 * supplied {@link ContextCache} and context failure threshold.
  120 | 	 * @since 6.1
  121 | 	 */
  122 | 	private DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache, int failureThreshold) {
  123 | 		Assert.notNull(contextCache, "ContextCache must not be null");
  124 | 		Assert.isTrue(failureThreshold > 0, "'failureThreshold' must be positive");
  125 | 		this.contextCache = contextCache;
  126 | 		this.failureThreshold = failureThreshold;
  127 | 	}
  128 | 
  129 | 
  130 | 	@Override
  131 | 	public boolean isContextLoaded(MergedContextConfiguration mergedConfig) {
  132 | 		mergedConfig = replaceIfNecessary(mergedConfig);
  133 | 		synchronized (this.contextCache) {
  134 | 			return this.contextCache.contains(mergedConfig);
  135 | 		}
  136 | 	}
  137 | 
  138 | 	@Override
  139 | 	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
  140 | 		mergedConfig = replaceIfNecessary(mergedConfig);
  141 | 		synchronized (this.contextCache) {
  142 | 			try {
  143 | 				ApplicationContext context = this.contextCache.get(mergedConfig);
  144 | 				if (context != null) {
  145 | 					if (logger.isTraceEnabled()) {
  146 | 						logger.trace("Retrieved ApplicationContext [%s] from cache with key %s".formatted(
  147 | 								System.identityHashCode(context), mergedConfig));
  148 | 					}
  149 | 					return context;
  150 | 				}
  151 | 
  152 | 				int failureCount = this.contextCache.getFailureCount(mergedConfig);
  153 | 				if (failureCount >= this.failureThreshold) {
  154 | 					throw new IllegalStateException("""
  155 | 							ApplicationContext failure threshold (%d) exceeded: \
  156 | 							skipping repeated attempt to load context for %s"""
  157 | 								.formatted(this.failureThreshold, mergedConfig));
  158 | 				}
  159 | 
  160 | 				return this.contextCache.put(mergedConfig, key -> {
  161 | 					try {
  162 | 						ApplicationContext newContext;
  163 | 						if (key instanceof AotMergedContextConfiguration aotMergedConfig) {
  164 | 							newContext = loadContextInAotMode(aotMergedConfig);
  165 | 						}
  166 | 						else {
  167 | 							newContext = loadContextInternal(key);
  168 | 						}
  169 | 						if (logger.isTraceEnabled()) {
  170 | 							logger.trace("Storing ApplicationContext [%s] in cache under key %s".formatted(
  171 | 									System.identityHashCode(newContext), key));
  172 | 						}
  173 | 						return newContext;
  174 | 					}
  175 | 					catch (Exception ex) {
  176 | 						if (logger.isTraceEnabled()) {
  177 | 							logger.trace("Incrementing ApplicationContext failure count for " + key);
  178 | 						}
  179 | 						this.contextCache.incrementFailureCount(key);
  180 | 						Throwable cause = ex;
  181 | 						if (ex instanceof ContextLoadException cle) {
  182 | 							cause = cle.getCause();
  183 | 							for (ApplicationContextFailureProcessor contextFailureProcessor : this.contextFailureProcessors) {
  184 | 								try {
  185 | 									contextFailureProcessor.processLoadFailure(cle.getApplicationContext(), cause);
  186 | 								}
  187 | 								catch (Throwable throwable) {
  188 | 									if (logger.isDebugEnabled()) {
  189 | 										logger.debug("Ignoring exception thrown from ApplicationContextFailureProcessor [%s]: %s"
  190 | 												.formatted(contextFailureProcessor, throwable));
  191 | 									}
  192 | 								}
  193 | 							}
  194 | 						}
  195 | 						throw new IllegalStateException("Failed to load ApplicationContext for " + key, cause);
  196 | 					}
  197 | 				});
  198 | 			}
  199 | 			finally {
  200 | 				this.contextCache.logStatistics();
  201 | 			}
  202 | 		}
  203 | 	}
  204 | 
  205 | 	@Override
  206 | 	public void closeContext(MergedContextConfiguration mergedConfig, @Nullable HierarchyMode hierarchyMode) {
  207 | 		mergedConfig = replaceIfNecessary(mergedConfig);
  208 | 		synchronized (this.contextCache) {
  209 | 			this.contextCache.remove(mergedConfig, hierarchyMode);
  210 | 		}
  211 | 	}
  212 | 
  213 | 	@Override
  214 | 	public void registerContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
  215 | 		mergedConfig = replaceIfNecessary(mergedConfig);
  216 | 		synchronized (this.contextCache) {
  217 | 			this.contextCache.registerContextUsage(mergedConfig, testClass);
  218 | 		}
  219 | 	}
  220 | 
  221 | 	@Override
  222 | 	public void unregisterContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
  223 | 		mergedConfig = replaceIfNecessary(mergedConfig);
  224 | 		synchronized (this.contextCache) {
  225 | 			this.contextCache.unregisterContextUsage(mergedConfig, testClass);
  226 | 		}
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Get the {@link ContextCache} used by this context loader delegate.
  231 | 	 */
  232 | 	protected ContextCache getContextCache() {
  233 | 		return this.contextCache;
  234 | 	}
  235 | 
  236 | 	/**
  237 | 	 * Load the {@code ApplicationContext} for the supplied merged context configuration.
  238 | 	 * <p>Supports both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
  239 | 	 * @throws Exception if an error occurs while loading the application context
  240 | 	 */
  241 | 	@SuppressWarnings("deprecation")
  242 | 	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedConfig)
  243 | 			throws Exception {
  244 | 
  245 | 		ContextLoader contextLoader = getContextLoader(mergedConfig);
  246 | 		if (contextLoader instanceof SmartContextLoader smartContextLoader) {
  247 | 			return smartContextLoader.loadContext(mergedConfig);
  248 | 		}
  249 | 		else {
  250 | 			String[] locations = mergedConfig.getLocations();
  251 | 			Assert.notNull(locations, () -> """
  252 | 					Cannot load an ApplicationContext with a NULL 'locations' array. \
  253 | 					Consider annotating test class [%s] with @ContextConfiguration or \
  254 | 					@ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
  255 | 			return contextLoader.loadContext(locations);
  256 | 		}
  257 | 	}
  258 | 
  259 | 	protected ApplicationContext loadContextInAotMode(AotMergedContextConfiguration aotMergedConfig) throws Exception {
  260 | 		Class<?> testClass = aotMergedConfig.getTestClass();
  261 | 		ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer =
  262 | 				this.aotTestContextInitializers.getContextInitializer(testClass);
  263 | 		Assert.state(contextInitializer != null,
  264 | 				() -> "Failed to load AOT ApplicationContextInitializer for test class [%s]"
  265 | 						.formatted(testClass.getName()));
  266 | 		ContextLoader contextLoader = getContextLoader(aotMergedConfig);
  267 | 
  268 | 		if (logger.isTraceEnabled()) {
  269 | 			logger.trace("Loading ApplicationContext for AOT runtime for " + aotMergedConfig.getOriginal());
  270 | 		}
  271 | 		else if (logger.isDebugEnabled()) {
  272 | 			logger.debug("Loading ApplicationContext for AOT runtime for test class " +
  273 | 					aotMergedConfig.getTestClass().getName());
  274 | 		}
  275 | 
  276 | 		if (!((contextLoader instanceof AotContextLoader aotContextLoader) &&
  277 | 				(aotContextLoader.loadContextForAotRuntime(aotMergedConfig.getOriginal(), contextInitializer)
  278 | 						instanceof GenericApplicationContext gac))) {
  279 | 			throw new TestContextAotException("""
  280 | 					Cannot load ApplicationContext for AOT runtime for %s. The configured \
  281 | 					ContextLoader [%s] must be an AotContextLoader and must create a \
  282 | 					GenericApplicationContext."""
  283 | 						.formatted(aotMergedConfig.getOriginal(), contextLoader.getClass().getName()));
  284 | 		}
  285 | 		gac.registerShutdownHook();
  286 | 		return gac;
  287 | 	}
  288 | 
  289 | 	private ContextLoader getContextLoader(MergedContextConfiguration mergedConfig) {
  290 | 		ContextLoader contextLoader = mergedConfig.getContextLoader();
  291 | 		Assert.notNull(contextLoader, () -> """
  292 | 				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
  293 | 				Consider annotating test class [%s] with @ContextConfiguration or \
  294 | 				@ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
  295 | 		return contextLoader;
  296 | 	}
  297 | 
  298 | 	/**
  299 | 	 * If the test class associated with the supplied {@link MergedContextConfiguration}
  300 | 	 * has an AOT-optimized {@link ApplicationContext}, this method will create an
  301 | 	 * {@link AotMergedContextConfiguration} to replace the provided {@code MergedContextConfiguration}.
  302 | 	 * <p>Otherwise, this method simply returns the supplied {@code MergedContextConfiguration}
  303 | 	 * unmodified.
  304 | 	 * <p>This allows for transparent {@link org.springframework.test.context.cache.ContextCache ContextCache}
  305 | 	 * support for AOT-optimized application contexts.
  306 | 	 * @param mergedConfig the original {@code MergedContextConfiguration}
  307 | 	 * @return {@code AotMergedContextConfiguration} or the original {@code MergedContextConfiguration}
  308 | 	 * @throws IllegalStateException if running in AOT mode and the test class does not
  309 | 	 * have an AOT-optimized {@code ApplicationContext}
  310 | 	 * @since 6.0
  311 | 	 */
  312 | 	private MergedContextConfiguration replaceIfNecessary(MergedContextConfiguration mergedConfig) {
  313 | 		if (AotDetector.useGeneratedArtifacts()) {
  314 | 			Class<?> testClass = mergedConfig.getTestClass();
  315 | 			Class<? extends ApplicationContextInitializer<?>> contextInitializerClass =
  316 | 					this.aotTestContextInitializers.getContextInitializerClass(testClass);
  317 | 			Assert.state(contextInitializerClass != null, () -> """
  318 | 					Failed to load AOT ApplicationContextInitializer class for test class [%s]. \
  319 | 					This can occur if AOT processing has not taken place for the test suite. It \
  320 | 					can also occur if AOT processing failed for the test class, in which case you \
  321 | 					can consult the logs generated during AOT processing.""".formatted(testClass.getName()));
  322 | 			return new AotMergedContextConfiguration(testClass, contextInitializerClass, mergedConfig, this);
  323 | 		}
  324 | 		return mergedConfig;
  325 | 	}
  326 | 
  327 | }
```
