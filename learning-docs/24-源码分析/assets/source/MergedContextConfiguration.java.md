<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/MergedContextConfiguration.java -->
<!-- 正文引用：[[源码:MergedContextConfiguration.java#L起-L止]] ；代码块头部：(src: MergedContextConfiguration.java#L起-L止) -->

# MergedContextConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/MergedContextConfiguration.java` |
| 行数 | 621 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:11:37.481Z |

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
   17 | package org.springframework.test.context;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.util.Arrays;
   21 | import java.util.Collections;
   22 | import java.util.LinkedHashSet;
   23 | import java.util.List;
   24 | import java.util.Set;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.context.ApplicationContext;
   29 | import org.springframework.context.ApplicationContextInitializer;
   30 | import org.springframework.core.io.support.PropertySourceDescriptor;
   31 | import org.springframework.core.style.DefaultToStringStyler;
   32 | import org.springframework.core.style.SimpleValueStyler;
   33 | import org.springframework.core.style.ToStringCreator;
   34 | import org.springframework.util.Assert;
   35 | import org.springframework.util.ObjectUtils;
   36 | import org.springframework.util.StringUtils;
   37 | 
   38 | /**
   39 |  * {@code MergedContextConfiguration} encapsulates the <em>merged</em> context
   40 |  * configuration declared on a test class and all of its superclasses and
   41 |  * enclosing classes via {@link ContextConfiguration @ContextConfiguration},
   42 |  * {@link ActiveProfiles @ActiveProfiles}, and
   43 |  * {@link TestPropertySource @TestPropertySource}.
   44 |  *
   45 |  * <p>Merged context resource locations, annotated classes, active profiles,
   46 |  * property resource locations, and in-lined properties represent all declared
   47 |  * values in the test class hierarchy and enclosing class hierarchy taking into
   48 |  * consideration the semantics of the {@link ContextConfiguration#inheritLocations},
   49 |  * {@link ActiveProfiles#inheritProfiles},
   50 |  * {@link TestPropertySource#inheritLocations}, and
   51 |  * {@link TestPropertySource#inheritProperties} flags.
   52 |  *
   53 |  * <p>A {@link SmartContextLoader} uses {@code MergedContextConfiguration}
   54 |  * to load an {@link org.springframework.context.ApplicationContext ApplicationContext}.
   55 |  *
   56 |  * <p>{@code MergedContextConfiguration} is also used by the
   57 |  * {@link org.springframework.test.context.cache.ContextCache ContextCache}
   58 |  * as the key for caching an
   59 |  * {@link org.springframework.context.ApplicationContext ApplicationContext}
   60 |  * that was loaded using properties of this {@code MergedContextConfiguration}.
   61 |  *
   62 |  * @author Sam Brannen
   63 |  * @author Phillip Webb
   64 |  * @since 3.1
   65 |  * @see ContextConfiguration
   66 |  * @see ContextHierarchy
   67 |  * @see ActiveProfiles
   68 |  * @see TestPropertySource
   69 |  * @see ContextConfigurationAttributes
   70 |  * @see SmartContextLoader#loadContext(MergedContextConfiguration)
   71 |  */
   72 | public class MergedContextConfiguration implements Serializable {
   73 | 
   74 | 	private static final long serialVersionUID = -3290560718464957422L;
   75 | 
   76 | 	private static final String[] EMPTY_STRING_ARRAY = new String[0];
   77 | 
   78 | 	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
   79 | 
   80 | 	private static final Set<Class<? extends ApplicationContextInitializer<?>>> EMPTY_INITIALIZER_CLASSES =
   81 | 			Collections.emptySet();
   82 | 
   83 | 	private static final Set<ContextCustomizer> EMPTY_CONTEXT_CUSTOMIZERS = Collections.emptySet();
   84 | 
   85 | 
   86 | 	private final Class<?> testClass;
   87 | 
   88 | 	private final String[] locations;
   89 | 
   90 | 	private final Class<?>[] classes;
   91 | 
   92 | 	@SuppressWarnings("serial")
   93 | 	private final Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses;
   94 | 
   95 | 	private final String[] activeProfiles;
   96 | 
   97 | 	@SuppressWarnings("serial")
   98 | 	private final List<PropertySourceDescriptor> propertySourceDescriptors;
   99 | 
  100 | 	private final String[] propertySourceLocations;
  101 | 
  102 | 	private final String[] propertySourceProperties;
  103 | 
  104 | 	@SuppressWarnings("serial")
  105 | 	private final Set<ContextCustomizer> contextCustomizers;
  106 | 
  107 | 	@SuppressWarnings("serial")
  108 | 	private final ContextLoader contextLoader;
  109 | 
  110 | 	@SuppressWarnings("serial")
  111 | 	private final @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;
  112 | 
  113 | 	private final @Nullable MergedContextConfiguration parent;
  114 | 
  115 | 
  116 | 	/**
  117 | 	 * Create a new {@code MergedContextConfiguration} instance for the
  118 | 	 * supplied parameters.
  119 | 	 * @param testClass the test class for which the configuration was merged
  120 | 	 * @param locations the merged context resource locations
  121 | 	 * @param classes the merged annotated classes
  122 | 	 * @param activeProfiles the merged active bean definition profiles
  123 | 	 * @param contextLoader the resolved {@code ContextLoader}
  124 | 	 */
  125 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  126 | 			String @Nullable [] activeProfiles, ContextLoader contextLoader) {
  127 | 
  128 | 		this(testClass, locations, classes, null, activeProfiles, contextLoader);
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Create a new {@code MergedContextConfiguration} instance for the
  133 | 	 * supplied parameters.
  134 | 	 * @param testClass the test class for which the configuration was merged
  135 | 	 * @param locations the merged context resource locations
  136 | 	 * @param classes the merged annotated classes
  137 | 	 * @param contextInitializerClasses the merged context initializer classes
  138 | 	 * @param activeProfiles the merged active bean definition profiles
  139 | 	 * @param contextLoader the resolved {@code ContextLoader}
  140 | 	 */
  141 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  142 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
  143 | 			String @Nullable [] activeProfiles, ContextLoader contextLoader) {
  144 | 
  145 | 		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, contextLoader, null, null);
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Create a new {@code MergedContextConfiguration} instance for the
  150 | 	 * supplied parameters.
  151 | 	 * @param testClass the test class for which the configuration was merged
  152 | 	 * @param locations the merged context resource locations
  153 | 	 * @param classes the merged annotated classes
  154 | 	 * @param contextInitializerClasses the merged context initializer classes
  155 | 	 * @param activeProfiles the merged active bean definition profiles
  156 | 	 * @param contextLoader the resolved {@code ContextLoader}
  157 | 	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
  158 | 	 * delegate with which to retrieve the parent {@code ApplicationContext}
  159 | 	 * @param parent the parent configuration or {@code null} if there is no parent
  160 | 	 * @since 3.2.2
  161 | 	 */
  162 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  163 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
  164 | 			String @Nullable [] activeProfiles, ContextLoader contextLoader,
  165 | 			@Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
  166 | 			@Nullable MergedContextConfiguration parent) {
  167 | 
  168 | 		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, null, null,
  169 | 				contextLoader, cacheAwareContextLoaderDelegate, parent);
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Create a new {@code MergedContextConfiguration} instance by copying
  174 | 	 * all fields from the supplied {@code MergedContextConfiguration}.
  175 | 	 * @since 4.1
  176 | 	 */
  177 | 	public MergedContextConfiguration(MergedContextConfiguration mergedConfig) {
  178 | 		this(mergedConfig.testClass, mergedConfig.locations, mergedConfig.classes,
  179 | 				mergedConfig.contextInitializerClasses, mergedConfig.activeProfiles,
  180 | 				mergedConfig.propertySourceDescriptors, mergedConfig.propertySourceProperties,
  181 | 				mergedConfig.contextCustomizers, mergedConfig.contextLoader,
  182 | 				mergedConfig.cacheAwareContextLoaderDelegate, mergedConfig.parent);
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Create a new {@code MergedContextConfiguration} instance for the
  187 | 	 * supplied parameters.
  188 | 	 * <p>If a {@code null} value is supplied for {@code locations},
  189 | 	 * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
  190 | 	 * or {@code propertySourceProperties} an empty array will be stored instead.
  191 | 	 * If a {@code null} value is supplied for the
  192 | 	 * {@code contextInitializerClasses} an empty set will be stored instead.
  193 | 	 * Furthermore, duplicate active profiles will be removed while preserving
  194 | 	 * registration order.
  195 | 	 * @param testClass the test class for which the configuration was merged
  196 | 	 * @param locations the merged context resource locations
  197 | 	 * @param classes the merged annotated classes
  198 | 	 * @param contextInitializerClasses the merged context initializer classes
  199 | 	 * @param activeProfiles the merged active bean definition profiles
  200 | 	 * @param propertySourceLocations the merged {@code PropertySource} locations
  201 | 	 * @param propertySourceProperties the merged {@code PropertySource} properties
  202 | 	 * @param contextLoader the resolved {@code ContextLoader}
  203 | 	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
  204 | 	 * delegate with which to retrieve the parent {@code ApplicationContext}
  205 | 	 * @param parent the parent configuration or {@code null} if there is no parent
  206 | 	 * @since 4.1
  207 | 	 * @deprecated since 6.1 in favor of
  208 | 	 * {@link #MergedContextConfiguration(Class, String[], Class[], Set, String[], List, String[], Set, ContextLoader, CacheAwareContextLoaderDelegate, MergedContextConfiguration)}
  209 | 	 */
  210 | 	@Deprecated(since = "6.1")
  211 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  212 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
  213 | 			String @Nullable [] activeProfiles, String @Nullable [] propertySourceLocations,
  214 | 			String @Nullable [] propertySourceProperties, ContextLoader contextLoader,
  215 | 			@Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
  216 | 			@Nullable MergedContextConfiguration parent) {
  217 | 
  218 | 		this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
  219 | 				propertySourceLocations, propertySourceProperties,
  220 | 				EMPTY_CONTEXT_CUSTOMIZERS, contextLoader,
  221 | 				cacheAwareContextLoaderDelegate, parent);
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Create a new {@code MergedContextConfiguration} instance for the
  226 | 	 * supplied parameters.
  227 | 	 * <p>If a {@code null} value is supplied for {@code locations},
  228 | 	 * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
  229 | 	 * or {@code propertySourceProperties} an empty array will be stored instead.
  230 | 	 * If a {@code null} value is supplied for {@code contextInitializerClasses}
  231 | 	 * or {@code contextCustomizers}, an empty set will be stored instead.
  232 | 	 * Furthermore, duplicate active profiles will be removed while preserving
  233 | 	 * registration order.
  234 | 	 * @param testClass the test class for which the configuration was merged
  235 | 	 * @param locations the merged context resource locations
  236 | 	 * @param classes the merged annotated classes
  237 | 	 * @param contextInitializerClasses the merged context initializer classes
  238 | 	 * @param activeProfiles the merged active bean definition profiles
  239 | 	 * @param propertySourceLocations the merged {@code PropertySource} locations
  240 | 	 * @param propertySourceProperties the merged {@code PropertySource} properties
  241 | 	 * @param contextCustomizers the context customizers
  242 | 	 * @param contextLoader the resolved {@code ContextLoader}
  243 | 	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
  244 | 	 * delegate with which to retrieve the parent {@code ApplicationContext}
  245 | 	 * @param parent the parent configuration or {@code null} if there is no parent
  246 | 	 * @since 4.3
  247 | 	 * @deprecated since 6.1 in favor of
  248 | 	 * {@link #MergedContextConfiguration(Class, String[], Class[], Set, String[], List, String[], Set, ContextLoader, CacheAwareContextLoaderDelegate, MergedContextConfiguration)}
  249 | 	 */
  250 | 	@Deprecated(since = "6.1")
  251 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  252 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
  253 | 			String @Nullable [] activeProfiles, String @Nullable [] propertySourceLocations,
  254 | 			String @Nullable [] propertySourceProperties, @Nullable Set<ContextCustomizer> contextCustomizers,
  255 | 			ContextLoader contextLoader, @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
  256 | 			@Nullable MergedContextConfiguration parent) {
  257 | 
  258 | 		this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
  259 | 			List.of(new PropertySourceDescriptor(processStrings(propertySourceLocations))),
  260 | 			propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate,
  261 | 			parent);
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * Create a new {@code MergedContextConfiguration} instance for the supplied
  266 | 	 * parameters.
  267 | 	 * <p>If a {@code null} value is supplied for {@code locations}, {@code classes},
  268 | 	 * {@code activeProfiles}, or {@code propertySourceProperties} an empty array
  269 | 	 * will be stored instead. If a {@code null} value is supplied for
  270 | 	 * {@code contextInitializerClasses} or {@code contextCustomizers}, an empty
  271 | 	 * set will be stored instead. Furthermore, duplicate active profiles
  272 | 	 * will be removed while preserving registration order.
  273 | 	 * @param testClass the test class for which the configuration was merged
  274 | 	 * @param locations the merged context resource locations
  275 | 	 * @param classes the merged annotated classes
  276 | 	 * @param contextInitializerClasses the merged context initializer classes
  277 | 	 * @param activeProfiles the merged active bean definition profiles
  278 | 	 * @param propertySourceDescriptors the merged property source descriptors
  279 | 	 * @param propertySourceProperties the merged inlined properties
  280 | 	 * @param contextCustomizers the context customizers
  281 | 	 * @param contextLoader the resolved {@code ContextLoader}
  282 | 	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
  283 | 	 * delegate with which to retrieve the parent {@code ApplicationContext}
  284 | 	 * @param parent the parent configuration or {@code null} if there is no parent
  285 | 	 * @since 6.1
  286 | 	 */
  287 | 	public MergedContextConfiguration(Class<?> testClass, String @Nullable [] locations, Class<?> @Nullable [] classes,
  288 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses,
  289 | 			String @Nullable [] activeProfiles, List<PropertySourceDescriptor> propertySourceDescriptors,
  290 | 			String @Nullable [] propertySourceProperties, @Nullable Set<ContextCustomizer> contextCustomizers,
  291 | 			ContextLoader contextLoader, @Nullable CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
  292 | 			@Nullable MergedContextConfiguration parent) {
  293 | 
  294 | 		this.testClass = testClass;
  295 | 		this.locations = processStrings(locations);
  296 | 		this.classes = processClasses(classes);
  297 | 		this.contextInitializerClasses = processContextInitializerClasses(contextInitializerClasses);
  298 | 		this.activeProfiles = processActiveProfiles(activeProfiles);
  299 | 		this.propertySourceDescriptors = Collections.unmodifiableList(propertySourceDescriptors);
  300 | 		this.propertySourceLocations = this.propertySourceDescriptors.stream()
  301 | 				.map(PropertySourceDescriptor::locations)
  302 | 				.flatMap(List::stream)
  303 | 				.toArray(String[]::new);
  304 | 		this.propertySourceProperties = processStrings(propertySourceProperties);
  305 | 		this.contextCustomizers = processContextCustomizers(contextCustomizers);
  306 | 		this.contextLoader = contextLoader;
  307 | 		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
  308 | 		this.parent = parent;
  309 | 	}
  310 | 
  311 | 
  312 | 	/**
  313 | 	 * Get the {@linkplain Class test class} associated with this
  314 | 	 * {@code MergedContextConfiguration}.
  315 | 	 */
  316 | 	public Class<?> getTestClass() {
  317 | 		return this.testClass;
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Get the merged resource locations for {@code ApplicationContext}
  322 | 	 * configuration files for the {@linkplain #getTestClass() test class}.
  323 | 	 * <p>Context resource locations typically represent XML configuration
  324 | 	 * files or Groovy scripts.
  325 | 	 */
  326 | 	public String[] getLocations() {
  327 | 		return this.locations;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Get the merged annotated classes for the {@linkplain #getTestClass() test class}.
  332 | 	 */
  333 | 	public Class<?>[] getClasses() {
  334 | 		return this.classes;
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Determine if this {@code MergedContextConfiguration} instance has
  339 | 	 * path-based context resource locations.
  340 | 	 * @return {@code true} if the {@link #getLocations() locations} array is not empty
  341 | 	 * @since 4.0.4
  342 | 	 * @see #hasResources()
  343 | 	 * @see #hasClasses()
  344 | 	 */
  345 | 	public boolean hasLocations() {
  346 | 		return !ObjectUtils.isEmpty(getLocations());
  347 | 	}
  348 | 
  349 | 	/**
  350 | 	 * Determine if this {@code MergedContextConfiguration} instance has
  351 | 	 * class-based resources.
  352 | 	 * @return {@code true} if the {@link #getClasses() classes} array is not empty
  353 | 	 * @since 4.0.4
  354 | 	 * @see #hasResources()
  355 | 	 * @see #hasLocations()
  356 | 	 */
  357 | 	public boolean hasClasses() {
  358 | 		return !ObjectUtils.isEmpty(getClasses());
  359 | 	}
  360 | 
  361 | 	/**
  362 | 	 * Determine if this {@code MergedContextConfiguration} instance has
  363 | 	 * either path-based context resource locations or class-based resources.
  364 | 	 * @return {@code true} if either the {@link #getLocations() locations}
  365 | 	 * or the {@link #getClasses() classes} array is not empty
  366 | 	 * @since 4.0.4
  367 | 	 * @see #hasLocations()
  368 | 	 * @see #hasClasses()
  369 | 	 */
  370 | 	public boolean hasResources() {
  371 | 		return (hasLocations() || hasClasses());
  372 | 	}
  373 | 
  374 | 	/**
  375 | 	 * Get the merged {@code ApplicationContextInitializer} classes for the
  376 | 	 * {@linkplain #getTestClass() test class}.
  377 | 	 */
  378 | 	public Set<Class<? extends ApplicationContextInitializer<?>>> getContextInitializerClasses() {
  379 | 		return this.contextInitializerClasses;
  380 | 	}
  381 | 
  382 | 	/**
  383 | 	 * Get the merged active bean definition profiles for the
  384 | 	 * {@linkplain #getTestClass() test class}.
  385 | 	 * @see ActiveProfiles
  386 | 	 */
  387 | 	public String[] getActiveProfiles() {
  388 | 		return this.activeProfiles;
  389 | 	}
  390 | 
  391 | 	/**
  392 | 	 * Get the merged descriptors for resource locations for test {@code PropertySources}
  393 | 	 * for the {@linkplain #getTestClass() test class}.
  394 | 	 * <p>Properties will be loaded into the {@code Environment}'s set of
  395 | 	 * {@code PropertySources}.
  396 | 	 * @since 6.1
  397 | 	 * @see TestPropertySource#locations
  398 | 	 * @see TestPropertySource#encoding
  399 | 	 * @see TestPropertySource#factory
  400 | 	 */
  401 | 	public List<PropertySourceDescriptor> getPropertySourceDescriptors() {
  402 | 		return this.propertySourceDescriptors;
  403 | 	}
  404 | 
  405 | 	/**
  406 | 	 * Get the merged resource locations of properties files for the
  407 | 	 * {@linkplain #getTestClass() test class}.
  408 | 	 * @see TestPropertySource#locations
  409 | 	 * @see java.util.Properties
  410 | 	 * @deprecated since 6.1 in favor of {@link #getPropertySourceDescriptors()}
  411 | 	 */
  412 | 	@Deprecated(since = "6.1")
  413 | 	public String[] getPropertySourceLocations() {
  414 | 		return this.propertySourceLocations;
  415 | 	}
  416 | 
  417 | 	/**
  418 | 	 * Get the merged inlined properties for the {@linkplain #getTestClass() test class}.
  419 | 	 * <p>Properties will be loaded into the {@code Environment}'s set of
  420 | 	 * {@code PropertySources}.
  421 | 	 * @see TestPropertySource#properties
  422 | 	 * @see java.util.Properties
  423 | 	 */
  424 | 	public String[] getPropertySourceProperties() {
  425 | 		return this.propertySourceProperties;
  426 | 	}
  427 | 
  428 | 	/**
  429 | 	 * Get the merged {@link ContextCustomizer ContextCustomizers} that will be applied
  430 | 	 * when the application context is loaded.
  431 | 	 */
  432 | 	public Set<ContextCustomizer> getContextCustomizers() {
  433 | 		return this.contextCustomizers;
  434 | 	}
  435 | 
  436 | 	/**
  437 | 	 * Get the resolved {@link ContextLoader} for the {@linkplain #getTestClass() test class}.
  438 | 	 */
  439 | 	public ContextLoader getContextLoader() {
  440 | 		return this.contextLoader;
  441 | 	}
  442 | 
  443 | 	/**
  444 | 	 * Get the {@link MergedContextConfiguration} for the parent application context
  445 | 	 * in a context hierarchy.
  446 | 	 * @return the parent configuration or {@code null} if there is no parent
  447 | 	 * @since 3.2.2
  448 | 	 * @see #getParentApplicationContext()
  449 | 	 */
  450 | 	public @Nullable MergedContextConfiguration getParent() {
  451 | 		return this.parent;
  452 | 	}
  453 | 
  454 | 	/**
  455 | 	 * Get the parent {@link ApplicationContext} for the context defined by this
  456 | 	 * {@code MergedContextConfiguration} from the context cache.
  457 | 	 * <p>If the parent context has not yet been loaded, it will be loaded, stored
  458 | 	 * in the cache, and then returned.
  459 | 	 * @return the parent {@code ApplicationContext} or {@code null} if there is no parent
  460 | 	 * @since 3.2.2
  461 | 	 * @see #getParent()
  462 | 	 */
  463 | 	public @Nullable ApplicationContext getParentApplicationContext() {
  464 | 		if (this.parent == null) {
  465 | 			return null;
  466 | 		}
  467 | 		Assert.state(this.cacheAwareContextLoaderDelegate != null,
  468 | 				"Cannot retrieve a parent application context without access to the CacheAwareContextLoaderDelegate");
  469 | 		return this.cacheAwareContextLoaderDelegate.loadContext(this.parent);
  470 | 	}
  471 | 
  472 | 
  473 | 	/**
  474 | 	 * Determine if the supplied object is equal to this {@code MergedContextConfiguration}
  475 | 	 * instance by comparing both objects' {@linkplain #getLocations() locations},
  476 | 	 * {@linkplain #getClasses() annotated classes},
  477 | 	 * {@linkplain #getContextInitializerClasses() context initializer classes},
  478 | 	 * {@linkplain #getActiveProfiles() active profiles},
  479 | 	 * {@linkplain #getPropertySourceDescriptors() property source descriptors},
  480 | 	 * {@linkplain #getPropertySourceProperties() property source properties},
  481 | 	 * {@linkplain #getContextCustomizers() context customizers},
  482 | 	 * {@linkplain #getParent() parents}, and the fully qualified names of their
  483 | 	 * {@link #getContextLoader() ContextLoaders}.
  484 | 	 */
  485 | 	@Override
  486 | 	public boolean equals(@Nullable Object other) {
  487 | 		if (this == other) {
  488 | 			return true;
  489 | 		}
  490 | 		if (other == null || other.getClass() != getClass()) {
  491 | 			return false;
  492 | 		}
  493 | 
  494 | 		MergedContextConfiguration otherConfig = (MergedContextConfiguration) other;
  495 | 		if (!Arrays.equals(this.locations, otherConfig.locations)) {
  496 | 			return false;
  497 | 		}
  498 | 		if (!Arrays.equals(this.classes, otherConfig.classes)) {
  499 | 			return false;
  500 | 		}
  501 | 		if (!this.contextInitializerClasses.equals(otherConfig.contextInitializerClasses)) {
  502 | 			return false;
  503 | 		}
  504 | 		if (!Arrays.equals(this.activeProfiles, otherConfig.activeProfiles)) {
  505 | 			return false;
  506 | 		}
  507 | 		if (!this.propertySourceDescriptors.equals(otherConfig.propertySourceDescriptors)) {
  508 | 			return false;
  509 | 		}
  510 | 		if (!Arrays.equals(this.propertySourceProperties, otherConfig.propertySourceProperties)) {
  511 | 			return false;
  512 | 		}
  513 | 		if (!this.contextCustomizers.equals(otherConfig.contextCustomizers)) {
  514 | 			return false;
  515 | 		}
  516 | 
  517 | 		if (this.parent == null) {
  518 | 			if (otherConfig.parent != null) {
  519 | 				return false;
  520 | 			}
  521 | 		}
  522 | 		else if (!this.parent.equals(otherConfig.parent)) {
  523 | 			return false;
  524 | 		}
  525 | 
  526 | 		if (!nullSafeClassName(this.contextLoader).equals(nullSafeClassName(otherConfig.contextLoader))) {
  527 | 			return false;
  528 | 		}
  529 | 
  530 | 		return true;
  531 | 	}
  532 | 
  533 | 	/**
  534 | 	 * Generate a unique hash code for all properties of this
  535 | 	 * {@code MergedContextConfiguration} excluding the
  536 | 	 * {@linkplain #getTestClass() test class}.
  537 | 	 */
  538 | 	@Override
  539 | 	public int hashCode() {
  540 | 		int result = Arrays.hashCode(this.locations);
  541 | 		result = 31 * result + Arrays.hashCode(this.classes);
  542 | 		result = 31 * result + this.contextInitializerClasses.hashCode();
  543 | 		result = 31 * result + Arrays.hashCode(this.activeProfiles);
  544 | 		result = 31 * result + this.propertySourceDescriptors.hashCode();
  545 | 		result = 31 * result + Arrays.hashCode(this.propertySourceProperties);
  546 | 		result = 31 * result + this.contextCustomizers.hashCode();
  547 | 		result = 31 * result + (this.parent != null ? this.parent.hashCode() : 0);
  548 | 		result = 31 * result + nullSafeClassName(this.contextLoader).hashCode();
  549 | 		return result;
  550 | 	}
  551 | 
  552 | 	/**
  553 | 	 * Provide a String representation of the {@linkplain #getTestClass() test class},
  554 | 	 * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
  555 | 	 * {@linkplain #getContextInitializerClasses() context initializer classes},
  556 | 	 * {@linkplain #getActiveProfiles() active profiles},
  557 | 	 * {@linkplain #getPropertySourceDescriptors() property source descriptors},
  558 | 	 * {@linkplain #getPropertySourceProperties() property source properties},
  559 | 	 * {@linkplain #getContextCustomizers() context customizers},
  560 | 	 * the name of the {@link #getContextLoader() ContextLoader}, and the
  561 | 	 * {@linkplain #getParent() parent configuration}.
  562 | 	 */
  563 | 	@Override
  564 | 	public String toString() {
  565 | 		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
  566 | 				.append("testClass", this.testClass)
  567 | 				.append("locations", this.locations)
  568 | 				.append("classes", this.classes)
  569 | 				.append("contextInitializerClasses", this.contextInitializerClasses)
  570 | 				.append("activeProfiles", this.activeProfiles)
  571 | 				.append("propertySourceDescriptors", this.propertySourceDescriptors)
  572 | 				.append("propertySourceProperties", this.propertySourceProperties)
  573 | 				.append("contextCustomizers", this.contextCustomizers)
  574 | 				.append("contextLoader", (this.contextLoader != null ? this.contextLoader.getClass() : null))
  575 | 				.append("parent", this.parent)
  576 | 				.toString();
  577 | 	}
  578 | 
  579 | 
  580 | 	protected static String[] processStrings(String @Nullable [] array) {
  581 | 		return (array != null ? array : EMPTY_STRING_ARRAY);
  582 | 	}
  583 | 
  584 | 	private static Class<?>[] processClasses(Class<?> @Nullable [] classes) {
  585 | 		return (classes != null ? classes : EMPTY_CLASS_ARRAY);
  586 | 	}
  587 | 
  588 | 	private static Set<Class<? extends ApplicationContextInitializer<?>>> processContextInitializerClasses(
  589 | 			@Nullable Set<Class<? extends ApplicationContextInitializer<?>>> contextInitializerClasses) {
  590 | 
  591 | 		return (contextInitializerClasses != null ?
  592 | 				Collections.unmodifiableSet(contextInitializerClasses) : EMPTY_INITIALIZER_CLASSES);
  593 | 	}
  594 | 
  595 | 	private static Set<ContextCustomizer> processContextCustomizers(
  596 | 			@Nullable Set<ContextCustomizer> contextCustomizers) {
  597 | 
  598 | 		return (contextCustomizers != null ?
  599 | 				Collections.unmodifiableSet(contextCustomizers) : EMPTY_CONTEXT_CUSTOMIZERS);
  600 | 	}
  601 | 
  602 | 	private static String[] processActiveProfiles(String @Nullable [] activeProfiles) {
  603 | 		if (activeProfiles == null) {
  604 | 			return EMPTY_STRING_ARRAY;
  605 | 		}
  606 | 
  607 | 		// Active profiles must be unique
  608 | 		Set<String> profilesSet = new LinkedHashSet<>(Arrays.asList(activeProfiles));
  609 | 		return StringUtils.toStringArray(profilesSet);
  610 | 	}
  611 | 
  612 | 	/**
  613 | 	 * Generate a null-safe {@link String} representation of the supplied
  614 | 	 * {@link ContextLoader} based solely on the fully qualified name of the
  615 | 	 * loader or &quot;null&quot; if the supplied loader is {@code null}.
  616 | 	 */
  617 | 	protected static String nullSafeClassName(@Nullable ContextLoader contextLoader) {
  618 | 		return (contextLoader != null ? contextLoader.getClass().getName() : "null");
  619 | 	}
  620 | 
  621 | }
```
