<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/support/AbstractTestContextBootstrapper.java -->
<!-- 正文引用：[[源码:AbstractTestContextBootstrapper.java#L起-L止]] ；代码块头部：(src: AbstractTestContextBootstrapper.java#L起-L止) -->

# AbstractTestContextBootstrapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/support/AbstractTestContextBootstrapper.java` |
| 行数 | 623 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.453Z |

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
   17 | package org.springframework.test.context.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.Collection;
   22 | import java.util.Collections;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | 
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.beans.BeanInstantiationException;
   32 | import org.springframework.beans.BeanUtils;
   33 | import org.springframework.context.ApplicationContextInitializer;
   34 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   35 | import org.springframework.test.context.BootstrapContext;
   36 | import org.springframework.test.context.CacheAwareContextLoaderDelegate;
   37 | import org.springframework.test.context.ContextConfiguration;
   38 | import org.springframework.test.context.ContextConfigurationAttributes;
   39 | import org.springframework.test.context.ContextCustomizer;
   40 | import org.springframework.test.context.ContextCustomizerFactories;
   41 | import org.springframework.test.context.ContextCustomizerFactory;
   42 | import org.springframework.test.context.ContextHierarchy;
   43 | import org.springframework.test.context.ContextLoader;
   44 | import org.springframework.test.context.MergedContextConfiguration;
   45 | import org.springframework.test.context.SmartContextLoader;
   46 | import org.springframework.test.context.TestContext;
   47 | import org.springframework.test.context.TestContextAnnotationUtils;
   48 | import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
   49 | import org.springframework.test.context.TestContextBootstrapper;
   50 | import org.springframework.test.context.TestExecutionListener;
   51 | import org.springframework.test.context.TestExecutionListeners;
   52 | import org.springframework.test.context.TestExecutionListeners.MergeMode;
   53 | import org.springframework.test.context.util.TestContextSpringFactoriesUtils;
   54 | import org.springframework.util.Assert;
   55 | import org.springframework.util.ClassUtils;
   56 | import org.springframework.util.CollectionUtils;
   57 | import org.springframework.util.StringUtils;
   58 | 
   59 | /**
   60 |  * Abstract implementation of the {@link TestContextBootstrapper} interface which
   61 |  * provides most of the behavior required by a bootstrapper.
   62 |  *
   63 |  * <p>Concrete subclasses typically will only need to provide implementations for
   64 |  * the following methods:
   65 |  * <ul>
   66 |  * <li>{@link #getDefaultContextLoaderClass}
   67 |  * <li>{@link #processMergedContextConfiguration}
   68 |  * </ul>
   69 |  *
   70 |  * <p>To plug in custom
   71 |  * {@link org.springframework.test.context.cache.ContextCache ContextCache}
   72 |  * support, override {@link #getCacheAwareContextLoaderDelegate()}.
   73 |  *
   74 |  * @author Sam Brannen
   75 |  * @author Juergen Hoeller
   76 |  * @author Phillip Webb
   77 |  * @since 4.1
   78 |  */
   79 | public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {
   80 | 
   81 | 	private final Log logger = LogFactory.getLog(getClass());
   82 | 
   83 | 	private @Nullable BootstrapContext bootstrapContext;
   84 | 
   85 | 
   86 | 	@Override
   87 | 	public void setBootstrapContext(BootstrapContext bootstrapContext) {
   88 | 		this.bootstrapContext = bootstrapContext;
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public BootstrapContext getBootstrapContext() {
   93 | 		Assert.state(this.bootstrapContext != null, "No BootstrapContext set");
   94 | 		return this.bootstrapContext;
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Build a new {@link DefaultTestContext} using the {@linkplain Class test class}
   99 | 	 * in the {@link BootstrapContext} associated with this bootstrapper and
  100 | 	 * by delegating to {@link #buildMergedContextConfiguration()} and
  101 | 	 * {@link #getCacheAwareContextLoaderDelegate()}.
  102 | 	 * <p>Concrete subclasses may choose to override this method to return a
  103 | 	 * custom {@link TestContext} implementation.
  104 | 	 * @since 4.2
  105 | 	 */
  106 | 	@Override
  107 | 	public TestContext buildTestContext() {
  108 | 		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
  109 | 				getCacheAwareContextLoaderDelegate());
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	public final List<TestExecutionListener> getTestExecutionListeners() {
  114 | 		Class<?> clazz = getBootstrapContext().getTestClass();
  115 | 		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
  116 | 		List<TestExecutionListener> listeners = new ArrayList<>(8);
  117 | 		boolean usingDefaults = false;
  118 | 
  119 | 		AnnotationDescriptor<TestExecutionListeners> descriptor =
  120 | 				TestContextAnnotationUtils.findAnnotationDescriptor(clazz, annotationType);
  121 | 
  122 | 		// Use defaults?
  123 | 		if (descriptor == null) {
  124 | 			if (logger.isTraceEnabled()) {
  125 | 				logger.trace(String.format("@TestExecutionListeners is not present for class [%s]: using defaults.",
  126 | 						clazz.getName()));
  127 | 			}
  128 | 			usingDefaults = true;
  129 | 			listeners.addAll(getDefaultTestExecutionListeners());
  130 | 		}
  131 | 		else {
  132 | 			// Traverse the class hierarchy...
  133 | 			while (descriptor != null) {
  134 | 				Class<?> declaringClass = descriptor.getDeclaringClass();
  135 | 				TestExecutionListeners testExecutionListeners = descriptor.getAnnotation();
  136 | 				if (logger.isTraceEnabled()) {
  137 | 					logger.trace(String.format("Retrieved @TestExecutionListeners [%s] for declaring class [%s].",
  138 | 							testExecutionListeners, declaringClass.getName()));
  139 | 				}
  140 | 
  141 | 				boolean inheritListeners = testExecutionListeners.inheritListeners();
  142 | 				AnnotationDescriptor<TestExecutionListeners> parentDescriptor = descriptor.next();
  143 | 
  144 | 				// If there are no listeners to inherit, we might need to merge the
  145 | 				// locally declared listeners with the defaults.
  146 | 				if ((!inheritListeners || parentDescriptor == null) &&
  147 | 						testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS) {
  148 | 					if (logger.isTraceEnabled()) {
  149 | 						logger.trace(String.format("Merging default listeners with listeners configured via " +
  150 | 								"@TestExecutionListeners for class [%s].", descriptor.getRootDeclaringClass().getName()));
  151 | 					}
  152 | 					usingDefaults = true;
  153 | 					listeners.addAll(getDefaultTestExecutionListeners());
  154 | 				}
  155 | 
  156 | 				if (testExecutionListeners.listeners().length > 0) {
  157 | 					listeners.addAll(0, instantiateListeners(testExecutionListeners.listeners()));
  158 | 				}
  159 | 
  160 | 				descriptor = (inheritListeners ? parentDescriptor : null);
  161 | 			}
  162 | 		}
  163 | 
  164 | 		if (usingDefaults) {
  165 | 			// Remove possible duplicates if we loaded default listeners.
  166 | 			List<TestExecutionListener> uniqueListeners = new ArrayList<>(listeners.size());
  167 | 			listeners.forEach(listener -> {
  168 | 				Class<? extends TestExecutionListener> listenerClass = listener.getClass();
  169 | 				if (uniqueListeners.stream().map(Object::getClass).noneMatch(listenerClass::equals)) {
  170 | 					uniqueListeners.add(listener);
  171 | 				}
  172 | 			});
  173 | 			listeners = uniqueListeners;
  174 | 
  175 | 			// Sort by Ordered/@Order if we loaded default listeners.
  176 | 			AnnotationAwareOrderComparator.sort(listeners);
  177 | 		}
  178 | 
  179 | 		if (logger.isTraceEnabled()) {
  180 | 			logger.trace("Using TestExecutionListeners for test class [%s]: %s"
  181 | 					.formatted(clazz.getName(), listeners));
  182 | 		}
  183 | 		else if (logger.isDebugEnabled()) {
  184 | 			logger.debug("Using TestExecutionListeners for test class [%s]: %s"
  185 | 					.formatted(clazz.getSimpleName(), classSimpleNames(listeners)));
  186 | 		}
  187 | 		return listeners;
  188 | 	}
  189 | 
  190 | 	@SuppressWarnings("unchecked")
  191 | 	private List<TestExecutionListener> instantiateListeners(Class<? extends TestExecutionListener>... classes) {
  192 | 		return instantiateComponents(TestExecutionListener.class, classes);
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Get the default {@link TestExecutionListener TestExecutionListeners} for
  197 | 	 * this bootstrapper.
  198 | 	 * <p>The default implementation delegates to
  199 | 	 * {@link TestContextSpringFactoriesUtils#loadFactoryImplementations(Class)}.
  200 | 	 * <p>This method is invoked by {@link #getTestExecutionListeners()}.
  201 | 	 * @return an <em>unmodifiable</em> list of default {@code TestExecutionListener}
  202 | 	 * instances
  203 | 	 * @since 6.0
  204 | 	 */
  205 | 	protected List<TestExecutionListener> getDefaultTestExecutionListeners() {
  206 | 		return TestContextSpringFactoriesUtils.loadFactoryImplementations(TestExecutionListener.class);
  207 | 	}
  208 | 
  209 | 	@SuppressWarnings("unchecked")
  210 | 	@Override
  211 | 	public final MergedContextConfiguration buildMergedContextConfiguration() {
  212 | 		Class<?> testClass = getBootstrapContext().getTestClass();
  213 | 		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();
  214 | 
  215 | 		if (TestContextAnnotationUtils.findAnnotationDescriptorForTypes(
  216 | 				testClass, ContextConfiguration.class, ContextHierarchy.class) == null) {
  217 | 			return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
  218 | 		}
  219 | 
  220 | 		if (TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ContextHierarchy.class) != null) {
  221 | 			Map<String, List<ContextConfigurationAttributes>> hierarchyMap =
  222 | 					ContextLoaderUtils.buildContextHierarchyMap(testClass);
  223 | 			MergedContextConfiguration parentConfig = null;
  224 | 			MergedContextConfiguration mergedConfig = null;
  225 | 
  226 | 			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
  227 | 				List<ContextConfigurationAttributes> reversedList = new ArrayList<>(list);
  228 | 				Collections.reverse(reversedList);
  229 | 
  230 | 				// Don't use the supplied testClass; instead ensure that we are
  231 | 				// building the MCC for the actual test class that declared the
  232 | 				// configuration for the current level in the context hierarchy.
  233 | 				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
  234 | 				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();
  235 | 
  236 | 				mergedConfig = buildMergedContextConfiguration(
  237 | 						declaringClass, reversedList, parentConfig, cacheAwareContextLoaderDelegate, true);
  238 | 				parentConfig = mergedConfig;
  239 | 			}
  240 | 
  241 | 			// Return the last level in the context hierarchy
  242 | 			Assert.state(mergedConfig != null, "No merged context configuration");
  243 | 			return mergedConfig;
  244 | 		}
  245 | 		else {
  246 | 			return buildMergedContextConfiguration(testClass,
  247 | 					ContextLoaderUtils.resolveContextConfigurationAttributes(testClass),
  248 | 					null, cacheAwareContextLoaderDelegate, true);
  249 | 		}
  250 | 	}
  251 | 
  252 | 	private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
  253 | 			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
  254 | 
  255 | 		List<ContextConfigurationAttributes> defaultConfigAttributesList =
  256 | 				ContextLoaderUtils.resolveDefaultContextConfigurationAttributes(testClass);
  257 | 		MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(
  258 | 				testClass, defaultConfigAttributesList, null, cacheAwareContextLoaderDelegate, false);
  259 | 
  260 | 		if (logger.isTraceEnabled()) {
  261 | 			logger.trace(String.format(
  262 | 					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]: using %s",
  263 | 					testClass.getName(), mergedConfig.getContextLoader().getClass().getName()));
  264 | 		}
  265 | 		else if (logger.isDebugEnabled()) {
  266 | 			logger.debug(String.format(
  267 | 					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]: using %s",
  268 | 					testClass.getSimpleName(), mergedConfig.getContextLoader().getClass().getSimpleName()));
  269 | 		}
  270 | 
  271 | 		return mergedConfig;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Build the {@linkplain MergedContextConfiguration merged context configuration}
  276 | 	 * for the supplied {@link Class testClass}, context configuration attributes,
  277 | 	 * and parent context configuration.
  278 | 	 * @param testClass the test class for which the {@code MergedContextConfiguration}
  279 | 	 * should be built (must not be {@code null})
  280 | 	 * @param configAttributesList the list of context configuration attributes for the
  281 | 	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
  282 | 	 * traversing up the class hierarchy and enclosing class hierarchy); never
  283 | 	 * {@code null} or empty
  284 | 	 * @param parentConfig the merged context configuration for the parent application
  285 | 	 * context in a context hierarchy, or {@code null} if there is no parent
  286 | 	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
  287 | 	 * be passed to the {@code MergedContextConfiguration} constructor
  288 | 	 * @param requireLocationsClassesOrInitializers whether locations, classes, or
  289 | 	 * initializers are required; typically {@code true} but may be set to {@code false}
  290 | 	 * if the configured loader supports empty configuration
  291 | 	 * @return the merged context configuration
  292 | 	 * @see #resolveContextLoader
  293 | 	 * @see ContextLoaderUtils#resolveContextConfigurationAttributes
  294 | 	 * @see SmartContextLoader#processContextConfiguration
  295 | 	 * @see ContextLoader#processLocations
  296 | 	 * @see ActiveProfilesUtils#resolveActiveProfiles
  297 | 	 * @see ApplicationContextInitializerUtils#resolveInitializerClasses
  298 | 	 * @see MergedContextConfiguration
  299 | 	 */
  300 | 	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
  301 | 			List<ContextConfigurationAttributes> configAttributesList,
  302 | 			@Nullable MergedContextConfiguration parentConfig,
  303 | 			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
  304 | 			boolean requireLocationsClassesOrInitializers) {
  305 | 
  306 | 		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");
  307 | 
  308 | 		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
  309 | 		List<String> locations = new ArrayList<>();
  310 | 		List<Class<?>> classes = new ArrayList<>();
  311 | 		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
  312 | 			if (logger.isTraceEnabled()) {
  313 | 				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
  314 | 						configAttributes));
  315 | 			}
  316 | 			if (contextLoader instanceof SmartContextLoader smartContextLoader) {
  317 | 				smartContextLoader.processContextConfiguration(configAttributes);
  318 | 				if (configAttributes.getLocations().length > 0) {
  319 | 					locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
  320 | 				}
  321 | 				if (configAttributes.getClasses().length > 0) {
  322 | 					classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
  323 | 				}
  324 | 			}
  325 | 			else {
  326 | 				@SuppressWarnings("deprecation")
  327 | 				String[] processedLocations = contextLoader.processLocations(
  328 | 						configAttributes.getDeclaringClass(), configAttributes.getLocations());
  329 | 				if (processedLocations.length > 0) {
  330 | 					locations.addAll(0, Arrays.asList(processedLocations));
  331 | 				}
  332 | 				// Legacy ContextLoaders don't know how to process classes
  333 | 			}
  334 | 			if (!configAttributes.isInheritLocations()) {
  335 | 				break;
  336 | 			}
  337 | 		}
  338 | 
  339 | 		Set<Class<? extends ApplicationContextInitializer<?>>> initializers =
  340 | 				ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList);
  341 | 		Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
  342 | 				Collections.unmodifiableList(configAttributesList));
  343 | 
  344 | 		Assert.state(!(requireLocationsClassesOrInitializers &&
  345 | 				areAllEmpty(locations, classes, initializers, contextCustomizers)), () -> """
  346 | 					%s was unable to detect defaults, and no ApplicationContextInitializers \
  347 | 					or ContextCustomizers were declared for context configuration attributes %s\
  348 | 					""".formatted(contextLoader.getClass().getSimpleName(), configAttributesList));
  349 | 
  350 | 		MergedTestPropertySources mergedTestPropertySources =
  351 | 				TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
  352 | 		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
  353 | 				StringUtils.toStringArray(locations), ClassUtils.toClassArray(classes),
  354 | 				initializers, ActiveProfilesUtils.resolveActiveProfiles(testClass),
  355 | 				mergedTestPropertySources.getPropertySourceDescriptors(),
  356 | 				mergedTestPropertySources.getProperties(),
  357 | 				contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
  358 | 
  359 | 		return processMergedContextConfiguration(mergedConfig);
  360 | 	}
  361 | 
  362 | 	private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
  363 | 			List<ContextConfigurationAttributes> configAttributes) {
  364 | 
  365 | 		List<ContextCustomizerFactory> factories = getContextCustomizerFactories(testClass);
  366 | 		Set<ContextCustomizer> customizers = CollectionUtils.newLinkedHashSet(factories.size());
  367 | 		for (ContextCustomizerFactory factory : factories) {
  368 | 			ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
  369 | 			if (customizer != null) {
  370 | 				customizers.add(customizer);
  371 | 			}
  372 | 		}
  373 | 		if (logger.isTraceEnabled()) {
  374 | 			logger.trace("Using ContextCustomizers for test class [%s]: %s"
  375 | 					.formatted(testClass.getName(), customizers));
  376 | 		}
  377 | 		else if (logger.isDebugEnabled()) {
  378 | 			logger.debug("Using ContextCustomizers for test class [%s]: %s"
  379 | 					.formatted(testClass.getSimpleName(), classSimpleNames(customizers)));
  380 | 		}
  381 | 		return customizers;
  382 | 	}
  383 | 
  384 | 	private List<ContextCustomizerFactory> getContextCustomizerFactories(Class<?> testClass) {
  385 | 		AnnotationDescriptor<ContextCustomizerFactories> descriptor =
  386 | 				TestContextAnnotationUtils.findAnnotationDescriptor(testClass, ContextCustomizerFactories.class);
  387 | 		List<ContextCustomizerFactory> factories = new ArrayList<>();
  388 | 
  389 | 		if (descriptor == null) {
  390 | 			if (logger.isTraceEnabled()) {
  391 | 				logger.trace("@ContextCustomizerFactories is not present for class [%s]"
  392 | 						.formatted(testClass.getName()));
  393 | 			}
  394 | 			factories.addAll(getContextCustomizerFactories());
  395 | 		}
  396 | 		else {
  397 | 			// Traverse the class hierarchy...
  398 | 			while (descriptor != null) {
  399 | 				Class<?> declaringClass = descriptor.getDeclaringClass();
  400 | 				ContextCustomizerFactories annotation = descriptor.getAnnotation();
  401 | 				if (logger.isTraceEnabled()) {
  402 | 					logger.trace("Retrieved %s for declaring class [%s]."
  403 | 							.formatted(annotation, declaringClass.getName()));
  404 | 				}
  405 | 
  406 | 				boolean inheritFactories = annotation.inheritFactories();
  407 | 				AnnotationDescriptor<ContextCustomizerFactories> parentDescriptor = descriptor.next();
  408 | 				if (annotation.factories().length > 0) {
  409 | 					factories.addAll(0, instantiateCustomizerFactories(annotation.factories()));
  410 | 				}
  411 | 
  412 | 				// If there are no factories to inherit, we might need to merge the
  413 | 				// locally declared factories with the defaults.
  414 | 				if ((!inheritFactories || parentDescriptor == null) &&
  415 | 						annotation.mergeMode() == ContextCustomizerFactories.MergeMode.MERGE_WITH_DEFAULTS) {
  416 | 					if (logger.isTraceEnabled()) {
  417 | 						logger.trace(String.format("Merging default factories with factories configured via " +
  418 | 								"@ContextCustomizerFactories for class [%s].", descriptor.getRootDeclaringClass().getName()));
  419 | 					}
  420 | 					factories.addAll(0, getContextCustomizerFactories());
  421 | 				}
  422 | 
  423 | 				descriptor = (inheritFactories ? parentDescriptor : null);
  424 | 			}
  425 | 		}
  426 | 
  427 | 		// Remove possible duplicates.
  428 | 		List<ContextCustomizerFactory> uniqueFactories = new ArrayList<>(factories.size());
  429 | 		factories.forEach(factory -> {
  430 | 			Class<? extends ContextCustomizerFactory> factoryClass = factory.getClass();
  431 | 			if (uniqueFactories.stream().map(Object::getClass).noneMatch(factoryClass::equals)) {
  432 | 				uniqueFactories.add(factory);
  433 | 			}
  434 | 		});
  435 | 		factories = uniqueFactories;
  436 | 
  437 | 		if (logger.isTraceEnabled()) {
  438 | 			logger.trace("Using ContextCustomizerFactory implementations for test class [%s]: %s"
  439 | 					.formatted(testClass.getName(), factories));
  440 | 		}
  441 | 		else if (logger.isDebugEnabled()) {
  442 | 			logger.debug("Using ContextCustomizerFactory implementations for test class [%s]: %s"
  443 | 					.formatted(testClass.getSimpleName(), classSimpleNames(factories)));
  444 | 		}
  445 | 
  446 | 		return factories;
  447 | 	}
  448 | 
  449 | 	/**
  450 | 	 * Get the {@link ContextCustomizerFactory} instances for this bootstrapper.
  451 | 	 * <p>The default implementation delegates to
  452 | 	 * {@link TestContextSpringFactoriesUtils#loadFactoryImplementations(Class)}.
  453 | 	 * @since 4.3
  454 | 	 */
  455 | 	protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
  456 | 		return TestContextSpringFactoriesUtils.loadFactoryImplementations(ContextCustomizerFactory.class);
  457 | 	}
  458 | 
  459 | 	@SuppressWarnings("unchecked")
  460 | 	private List<ContextCustomizerFactory> instantiateCustomizerFactories(Class<? extends ContextCustomizerFactory>... classes) {
  461 | 		return instantiateComponents(ContextCustomizerFactory.class, classes);
  462 | 	}
  463 | 
  464 | 	@SuppressWarnings("unchecked")
  465 | 	private <T> List<T> instantiateComponents(Class<T> componentType, Class<? extends T>... classes) {
  466 | 		List<T> components = new ArrayList<>(classes.length);
  467 | 		for (Class<? extends T> clazz : classes) {
  468 | 			try {
  469 | 				components.add(BeanUtils.instantiateClass(clazz));
  470 | 			}
  471 | 			catch (BeanInstantiationException ex) {
  472 | 				Throwable cause = ex.getCause();
  473 | 				if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
  474 | 					if (logger.isDebugEnabled()) {
  475 | 						logger.debug("""
  476 | 								Skipping candidate %1$s [%2$s] due to a missing dependency. \
  477 | 								Specify custom %1$s classes or make the default %1$s classes \
  478 | 								and their required dependencies available. Offending class: [%3$s]"""
  479 | 									.formatted(componentType.getSimpleName(), clazz.getName(), cause.getMessage()));
  480 | 					}
  481 | 				}
  482 | 				else {
  483 | 					throw ex;
  484 | 				}
  485 | 			}
  486 | 		}
  487 | 		return components;
  488 | 	}
  489 | 
  490 | 	/**
  491 | 	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the
  492 | 	 * supplied list of {@link ContextConfigurationAttributes} and then instantiate
  493 | 	 * and return that {@code ContextLoader}.
  494 | 	 * <p>If the user has not explicitly declared which loader to use, the value
  495 | 	 * returned from {@link #getDefaultContextLoaderClass} will be used as the
  496 | 	 * default context loader class. For details on the class resolution process,
  497 | 	 * see {@link #resolveExplicitContextLoaderClass} and
  498 | 	 * {@link #getDefaultContextLoaderClass}.
  499 | 	 * @param testClass the test class for which the {@code ContextLoader} should be
  500 | 	 * resolved; must not be {@code null}
  501 | 	 * @param configAttributesList the list of configuration attributes to process; must
  502 | 	 * not be {@code null}; must be ordered <em>bottom-up</em>
  503 | 	 * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
  504 | 	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
  505 | 	 * (never {@code null})
  506 | 	 * @throws IllegalStateException if {@link #getDefaultContextLoaderClass(Class)}
  507 | 	 * returns {@code null}
  508 | 	 */
  509 | 	protected ContextLoader resolveContextLoader(Class<?> testClass,
  510 | 			List<ContextConfigurationAttributes> configAttributesList) {
  511 | 
  512 | 		Assert.notNull(testClass, "Class must not be null");
  513 | 		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");
  514 | 
  515 | 		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
  516 | 		if (contextLoaderClass == null) {
  517 | 			contextLoaderClass = getDefaultContextLoaderClass(testClass);
  518 | 		}
  519 | 		if (logger.isTraceEnabled()) {
  520 | 			logger.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
  521 | 					contextLoaderClass.getName(), testClass.getName()));
  522 | 		}
  523 | 		return BeanUtils.instantiateClass(contextLoaderClass, ContextLoader.class);
  524 | 	}
  525 | 
  526 | 	/**
  527 | 	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
  528 | 	 * list of {@link ContextConfigurationAttributes}.
  529 | 	 * <p>Beginning with the first level in the context configuration attributes hierarchy:
  530 | 	 * <ol>
  531 | 	 * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
  532 | 	 * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
  533 | 	 * configured with an explicit class, that class will be returned.</li>
  534 | 	 * <li>If an explicit {@code ContextLoader} class is not specified at the current
  535 | 	 * level in the hierarchy, traverse to the next level in the hierarchy and return to
  536 | 	 * step #1.</li>
  537 | 	 * </ol>
  538 | 	 * @param configAttributesList the list of configuration attributes to process;
  539 | 	 * must not be {@code null}; must be ordered <em>bottom-up</em>
  540 | 	 * (i.e., as if we were traversing up the class hierarchy and enclosing class hierarchy)
  541 | 	 * @return the {@code ContextLoader} class to use for the supplied configuration
  542 | 	 * attributes, or {@code null} if no explicit loader is found
  543 | 	 * @throws IllegalArgumentException if supplied configuration attributes are
  544 | 	 * {@code null} or <em>empty</em>
  545 | 	 */
  546 | 	protected @Nullable Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
  547 | 			List<ContextConfigurationAttributes> configAttributesList) {
  548 | 
  549 | 		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");
  550 | 
  551 | 		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
  552 | 			if (logger.isTraceEnabled()) {
  553 | 				logger.trace("Resolving ContextLoader for context configuration attributes " + configAttributes);
  554 | 			}
  555 | 			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
  556 | 			if (ContextLoader.class != contextLoaderClass) {
  557 | 				if (logger.isTraceEnabled()) {
  558 | 					logger.trace("Found explicit ContextLoader class [%s] for context configuration attributes %s"
  559 | 							.formatted(contextLoaderClass.getName(), configAttributes));
  560 | 				}
  561 | 				else if (logger.isDebugEnabled()) {
  562 | 					logger.debug("Found explicit ContextLoader class [%s] for test class [%s]"
  563 | 						.formatted(contextLoaderClass.getSimpleName(), configAttributes.getDeclaringClass().getSimpleName()));
  564 | 				}
  565 | 				return contextLoaderClass;
  566 | 			}
  567 | 		}
  568 | 		return null;
  569 | 	}
  570 | 
  571 | 	/**
  572 | 	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
  573 | 	 * interaction with the {@code ContextCache}.
  574 | 	 * <p>The default implementation delegates to
  575 | 	 * {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()} and
  576 | 	 * the default one will load {@link org.springframework.test.context.ApplicationContextFailureProcessor}
  577 | 	 * via the service loading mechanism.
  578 | 	 * <p>Concrete subclasses may choose to override this method to return a custom
  579 | 	 * {@code CacheAwareContextLoaderDelegate} implementation with custom
  580 | 	 * {@link org.springframework.test.context.cache.ContextCache ContextCache} support.
  581 | 	 * @return the context loader delegate (never {@code null})
  582 | 	 * @see org.springframework.test.context.ApplicationContextFailureProcessor
  583 | 	 */
  584 | 	protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
  585 | 		return getBootstrapContext().getCacheAwareContextLoaderDelegate();
  586 | 	}
  587 | 
  588 | 	/**
  589 | 	 * Determine the default {@link ContextLoader} {@linkplain Class class}
  590 | 	 * to use for the supplied test class.
  591 | 	 * <p>The class returned by this method will only be used if a {@code ContextLoader}
  592 | 	 * class has not been explicitly declared via {@link ContextConfiguration#loader}.
  593 | 	 * @param testClass the test class for which to retrieve the default
  594 | 	 * {@code ContextLoader} class
  595 | 	 * @return the default {@code ContextLoader} class for the supplied test class
  596 | 	 * (never {@code null})
  597 | 	 */
  598 | 	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);
  599 | 
  600 | 	/**
  601 | 	 * Process the supplied, newly instantiated {@link MergedContextConfiguration} instance.
  602 | 	 * <p>The returned {@link MergedContextConfiguration} instance may be a wrapper
  603 | 	 * around or a replacement for the original.
  604 | 	 * <p>The default implementation simply returns the supplied instance unmodified.
  605 | 	 * <p>Concrete subclasses may choose to return a specialized subclass of
  606 | 	 * {@link MergedContextConfiguration} based on properties in the supplied instance.
  607 | 	 * @param mergedConfig the {@code MergedContextConfiguration} to process; never {@code null}
  608 | 	 * @return a fully initialized {@code MergedContextConfiguration}; never {@code null}
  609 | 	 */
  610 | 	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
  611 | 		return mergedConfig;
  612 | 	}
  613 | 
  614 | 
  615 | 	private static List<String> classSimpleNames(Collection<?> components) {
  616 | 		return components.stream().map(Object::getClass).map(Class::getSimpleName).toList();
  617 | 	}
  618 | 
  619 | 	private static boolean areAllEmpty(Collection<?>... collections) {
  620 | 		return Arrays.stream(collections).allMatch(Collection::isEmpty);
  621 | 	}
  622 | 
  623 | }
```
