<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/AutoConfigurationImportSelector.java -->
<!-- 正文引用：[[源码:AutoConfigurationImportSelector.java#L起-L止]] ；代码块头部：(src: AutoConfigurationImportSelector.java#L起-L止) -->

# AutoConfigurationImportSelector.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/AutoConfigurationImportSelector.java` |
| 行数 | 561 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:06.317Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Arrays;
    9 | import java.util.Collection;
   10 | import java.util.Collections;
   11 | import java.util.HashSet;
   12 | import java.util.LinkedHashMap;
   13 | import java.util.LinkedHashSet;
   14 | import java.util.List;
   15 | import java.util.Map;
   16 | import java.util.Set;
   17 | import java.util.concurrent.TimeUnit;
   18 | import java.util.function.Predicate;
   19 | import java.util.stream.Collectors;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.beans.BeansException;
   26 | import org.springframework.beans.factory.Aware;
   27 | import org.springframework.beans.factory.BeanClassLoaderAware;
   28 | import org.springframework.beans.factory.BeanFactory;
   29 | import org.springframework.beans.factory.BeanFactoryAware;
   30 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   31 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   32 | import org.springframework.boot.context.annotation.ImportCandidates;
   33 | import org.springframework.boot.context.properties.bind.Binder;
   34 | import org.springframework.context.EnvironmentAware;
   35 | import org.springframework.context.ResourceLoaderAware;
   36 | import org.springframework.context.annotation.Configuration;
   37 | import org.springframework.context.annotation.DeferredImportSelector;
   38 | import org.springframework.core.Ordered;
   39 | import org.springframework.core.annotation.AnnotationAttributes;
   40 | import org.springframework.core.env.ConfigurableEnvironment;
   41 | import org.springframework.core.env.Environment;
   42 | import org.springframework.core.io.ResourceLoader;
   43 | import org.springframework.core.io.support.SpringFactoriesLoader;
   44 | import org.springframework.core.type.AnnotationMetadata;
   45 | import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
   46 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   47 | import org.springframework.util.Assert;
   48 | import org.springframework.util.ClassUtils;
   49 | import org.springframework.util.CollectionUtils;
   50 | import org.springframework.util.StringUtils;
   51 | 
   52 | /**
   53 |  * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
   54 |  * auto-configuration}. This class can also be subclassed if a custom variant of
   55 |  * {@link EnableAutoConfiguration @EnableAutoConfiguration} is needed.
   56 |  *
   57 |  * @author Phillip Webb
   58 |  * @author Andy Wilkinson
   59 |  * @author Stephane Nicoll
   60 |  * @author Madhura Bhave
   61 |  * @author Moritz Halbritter
   62 |  * @author Scott Frederick
   63 |  * @since 1.3.0
   64 |  * @see EnableAutoConfiguration
   65 |  */
   66 | public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware,
   67 | 		ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {
   68 | 
   69 | 	static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;
   70 | 
   71 | 	private static final AutoConfigurationEntry EMPTY_ENTRY = new AutoConfigurationEntry();
   72 | 
   73 | 	private static final String[] NO_IMPORTS = {};
   74 | 
   75 | 	private static final Log logger = LogFactory.getLog(AutoConfigurationImportSelector.class);
   76 | 
   77 | 	private static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";
   78 | 
   79 | 	private final Class<?> autoConfigurationAnnotation;
   80 | 
   81 | 	@SuppressWarnings("NullAway.Init")
   82 | 	private ConfigurableListableBeanFactory beanFactory;
   83 | 
   84 | 	@SuppressWarnings("NullAway.Init")
   85 | 	private Environment environment;
   86 | 
   87 | 	@SuppressWarnings("NullAway.Init")
   88 | 	private ClassLoader beanClassLoader;
   89 | 
   90 | 	@SuppressWarnings("NullAway.Init")
   91 | 	private ResourceLoader resourceLoader;
   92 | 
   93 | 	private volatile @Nullable ConfigurationClassFilter configurationClassFilter;
   94 | 
   95 | 	private volatile @Nullable AutoConfigurationReplacements autoConfigurationReplacements;
   96 | 
   97 | 	public AutoConfigurationImportSelector() {
   98 | 		this(null);
   99 | 	}
  100 | 
  101 | 	AutoConfigurationImportSelector(@Nullable Class<?> autoConfigurationAnnotation) {
  102 | 		this.autoConfigurationAnnotation = (autoConfigurationAnnotation != null) ? autoConfigurationAnnotation
  103 | 				: AutoConfiguration.class;
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public String[] selectImports(AnnotationMetadata annotationMetadata) {
  108 | 		if (!isEnabled(annotationMetadata)) {
  109 | 			return NO_IMPORTS;
  110 | 		}
  111 | 		AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
  112 | 		return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
  113 | 	}
  114 | 
  115 | 	@Override
  116 | 	public Predicate<String> getExclusionFilter() {
  117 | 		return this::shouldExclude;
  118 | 	}
  119 | 
  120 | 	private boolean shouldExclude(String configurationClassName) {
  121 | 		return getConfigurationClassFilter().filter(Collections.singletonList(configurationClassName)).isEmpty();
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
  126 | 	 * of the importing {@link Configuration @Configuration} class.
  127 | 	 * @param annotationMetadata the annotation metadata of the configuration class
  128 | 	 * @return the auto-configurations that should be imported
  129 | 	 */
  130 | 	protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
  131 | 		if (!isEnabled(annotationMetadata)) {
  132 | 			return EMPTY_ENTRY;
  133 | 		}
  134 | 		AnnotationAttributes attributes = getAttributes(annotationMetadata);
  135 | 		List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
  136 | 		configurations = removeDuplicates(configurations);
  137 | 		Set<String> exclusions = getExclusions(annotationMetadata, attributes);
  138 | 		checkExcludedClasses(configurations, exclusions);
  139 | 		configurations.removeAll(exclusions);
  140 | 		configurations = getConfigurationClassFilter().filter(configurations);
  141 | 		fireAutoConfigurationImportEvents(configurations, exclusions);
  142 | 		return new AutoConfigurationEntry(configurations, exclusions);
  143 | 	}
  144 | 
  145 | 	@Override
  146 | 	public Class<? extends Group> getImportGroup() {
  147 | 		return AutoConfigurationGroup.class;
  148 | 	}
  149 | 
  150 | 	protected boolean isEnabled(AnnotationMetadata metadata) {
  151 | 		if (getClass() == AutoConfigurationImportSelector.class) {
  152 | 			return getEnvironment().getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true);
  153 | 		}
  154 | 		return true;
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Return the appropriate {@link AnnotationAttributes} from the
  159 | 	 * {@link AnnotationMetadata}. By default this method will return attributes for
  160 | 	 * {@link #getAnnotationClass()}.
  161 | 	 * @param metadata the annotation metadata
  162 | 	 * @return annotation attributes
  163 | 	 */
  164 | 	protected @Nullable AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
  165 | 		String name = getAnnotationClass().getName();
  166 | 		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(name, true));
  167 | 		Assert.state(attributes != null, () -> "No auto-configuration attributes found. Is " + metadata.getClassName()
  168 | 				+ " annotated with " + ClassUtils.getShortName(name) + "?");
  169 | 		return attributes;
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Return the source annotation class used by the selector.
  174 | 	 * @return the annotation class
  175 | 	 */
  176 | 	protected Class<?> getAnnotationClass() {
  177 | 		return EnableAutoConfiguration.class;
  178 | 	}
  179 | 
  180 | 	/**
  181 | 	 * Return the auto-configuration class names that should be considered. By default,
  182 | 	 * this method will load candidates using {@link ImportCandidates}.
  183 | 	 * @param metadata the source metadata
  184 | 	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
  185 | 	 * attributes}
  186 | 	 * @return a list of candidate configurations
  187 | 	 */
  188 | 	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
  189 | 			@Nullable AnnotationAttributes attributes) {
  190 | 		ImportCandidates importCandidates = ImportCandidates.load(this.autoConfigurationAnnotation,
  191 | 				getBeanClassLoader());
  192 | 		List<String> configurations = importCandidates.getCandidates();
  193 | 		Assert.state(!CollectionUtils.isEmpty(configurations),
  194 | 				() -> "No auto configuration classes found in " + "META-INF/spring/"
  195 | 						+ this.autoConfigurationAnnotation.getName() + ".imports. If you "
  196 | 						+ "are using a custom packaging, make sure that file is correct.");
  197 | 		return configurations;
  198 | 	}
  199 | 
  200 | 	private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
  201 | 		List<String> invalidExcludes = new ArrayList<>(exclusions.size());
  202 | 		ClassLoader classLoader = (this.beanClassLoader != null) ? this.beanClassLoader : getClass().getClassLoader();
  203 | 		for (String exclusion : exclusions) {
  204 | 			if (ClassUtils.isPresent(exclusion, classLoader) && !configurations.contains(exclusion)) {
  205 | 				invalidExcludes.add(exclusion);
  206 | 			}
  207 | 		}
  208 | 		if (!invalidExcludes.isEmpty()) {
  209 | 			handleInvalidExcludes(invalidExcludes);
  210 | 		}
  211 | 	}
  212 | 
  213 | 	/**
  214 | 	 * Handle any invalid excludes that have been specified.
  215 | 	 * @param invalidExcludes the list of invalid excludes (will always have at least one
  216 | 	 * element)
  217 | 	 */
  218 | 	protected void handleInvalidExcludes(List<String> invalidExcludes) {
  219 | 		StringBuilder message = new StringBuilder();
  220 | 		for (String exclude : invalidExcludes) {
  221 | 			message.append("\t- ").append(exclude).append(String.format("%n"));
  222 | 		}
  223 | 		throw new IllegalStateException(String.format(
  224 | 				"The following classes could not be excluded because they are not auto-configuration classes:%n%s",
  225 | 				message));
  226 | 	}
  227 | 
  228 | 	/**
  229 | 	 * Return any exclusions that limit the candidate configurations.
  230 | 	 * @param metadata the source metadata
  231 | 	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
  232 | 	 * attributes}
  233 | 	 * @return exclusions or an empty set
  234 | 	 */
  235 | 	protected Set<String> getExclusions(AnnotationMetadata metadata, @Nullable AnnotationAttributes attributes) {
  236 | 		Set<String> excluded = new LinkedHashSet<>();
  237 | 		if (attributes != null) {
  238 | 			excluded.addAll(asList(attributes, "exclude"));
  239 | 			excluded.addAll(asList(attributes, "excludeName"));
  240 | 		}
  241 | 		excluded.addAll(getExcludeAutoConfigurationsProperty());
  242 | 		return getAutoConfigurationReplacements().replaceAll(excluded);
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * Returns the auto-configurations excluded by the
  247 | 	 * {@code spring.autoconfigure.exclude} property.
  248 | 	 * @return excluded auto-configurations
  249 | 	 * @since 2.3.2
  250 | 	 */
  251 | 	protected List<String> getExcludeAutoConfigurationsProperty() {
  252 | 		Environment environment = getEnvironment();
  253 | 		if (environment == null) {
  254 | 			return Collections.emptyList();
  255 | 		}
  256 | 		if (environment instanceof ConfigurableEnvironment) {
  257 | 			Binder binder = Binder.get(environment);
  258 | 			return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class)
  259 | 				.map(Arrays::asList)
  260 | 				.orElse(Collections.emptyList());
  261 | 		}
  262 | 		String[] excludes = environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
  263 | 		return (excludes != null) ? Arrays.asList(excludes) : Collections.emptyList();
  264 | 	}
  265 | 
  266 | 	protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
  267 | 		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, this.beanClassLoader);
  268 | 	}
  269 | 
  270 | 	private ConfigurationClassFilter getConfigurationClassFilter() {
  271 | 		ConfigurationClassFilter configurationClassFilter = this.configurationClassFilter;
  272 | 		if (configurationClassFilter == null) {
  273 | 			List<AutoConfigurationImportFilter> filters = getAutoConfigurationImportFilters();
  274 | 			for (AutoConfigurationImportFilter filter : filters) {
  275 | 				invokeAwareMethods(filter);
  276 | 			}
  277 | 			configurationClassFilter = new ConfigurationClassFilter(this.beanClassLoader, filters);
  278 | 			this.configurationClassFilter = configurationClassFilter;
  279 | 		}
  280 | 		return configurationClassFilter;
  281 | 	}
  282 | 
  283 | 	private AutoConfigurationReplacements getAutoConfigurationReplacements() {
  284 | 		AutoConfigurationReplacements autoConfigurationReplacements = this.autoConfigurationReplacements;
  285 | 		if (autoConfigurationReplacements == null) {
  286 | 			autoConfigurationReplacements = AutoConfigurationReplacements.load(this.autoConfigurationAnnotation,
  287 | 					this.beanClassLoader);
  288 | 			this.autoConfigurationReplacements = autoConfigurationReplacements;
  289 | 		}
  290 | 		return autoConfigurationReplacements;
  291 | 	}
  292 | 
  293 | 	protected final <T> List<T> removeDuplicates(List<T> list) {
  294 | 		return new ArrayList<>(new LinkedHashSet<>(list));
  295 | 	}
  296 | 
  297 | 	protected final List<String> asList(AnnotationAttributes attributes, String name) {
  298 | 		String[] value = attributes.getStringArray(name);
  299 | 		return Arrays.asList(value);
  300 | 	}
  301 | 
  302 | 	private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
  303 | 		List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
  304 | 		if (!listeners.isEmpty()) {
  305 | 			AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
  306 | 			for (AutoConfigurationImportListener listener : listeners) {
  307 | 				invokeAwareMethods(listener);
  308 | 				listener.onAutoConfigurationImportEvent(event);
  309 | 			}
  310 | 		}
  311 | 	}
  312 | 
  313 | 	protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
  314 | 		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportListener.class, this.beanClassLoader);
  315 | 	}
  316 | 
  317 | 	private void invokeAwareMethods(Object instance) {
  318 | 		if (instance instanceof Aware) {
  319 | 			if (instance instanceof BeanClassLoaderAware beanClassLoaderAwareInstance) {
  320 | 				beanClassLoaderAwareInstance.setBeanClassLoader(this.beanClassLoader);
  321 | 			}
  322 | 			if (instance instanceof BeanFactoryAware beanFactoryAwareInstance) {
  323 | 				beanFactoryAwareInstance.setBeanFactory(this.beanFactory);
  324 | 			}
  325 | 			if (instance instanceof EnvironmentAware environmentAwareInstance) {
  326 | 				environmentAwareInstance.setEnvironment(this.environment);
  327 | 			}
  328 | 			if (instance instanceof ResourceLoaderAware resourceLoaderAwareInstance) {
  329 | 				resourceLoaderAwareInstance.setResourceLoader(this.resourceLoader);
  330 | 			}
  331 | 		}
  332 | 	}
  333 | 
  334 | 	@Override
  335 | 	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
  336 | 		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
  337 | 		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
  338 | 	}
  339 | 
  340 | 	protected final ConfigurableListableBeanFactory getBeanFactory() {
  341 | 		return this.beanFactory;
  342 | 	}
  343 | 
  344 | 	@Override
  345 | 	public void setBeanClassLoader(ClassLoader classLoader) {
  346 | 		this.beanClassLoader = classLoader;
  347 | 	}
  348 | 
  349 | 	protected ClassLoader getBeanClassLoader() {
  350 | 		return this.beanClassLoader;
  351 | 	}
  352 | 
  353 | 	@Override
  354 | 	public void setEnvironment(Environment environment) {
  355 | 		this.environment = environment;
  356 | 	}
  357 | 
  358 | 	protected final Environment getEnvironment() {
  359 | 		return this.environment;
  360 | 	}
  361 | 
  362 | 	@Override
  363 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  364 | 		this.resourceLoader = resourceLoader;
  365 | 	}
  366 | 
  367 | 	protected final ResourceLoader getResourceLoader() {
  368 | 		return this.resourceLoader;
  369 | 	}
  370 | 
  371 | 	@Override
  372 | 	public int getOrder() {
  373 | 		return ORDER;
  374 | 	}
  375 | 
  376 | 	private static class ConfigurationClassFilter {
  377 | 
  378 | 		private final AutoConfigurationMetadata autoConfigurationMetadata;
  379 | 
  380 | 		private final List<AutoConfigurationImportFilter> filters;
  381 | 
  382 | 		ConfigurationClassFilter(ClassLoader classLoader, List<AutoConfigurationImportFilter> filters) {
  383 | 			this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(classLoader);
  384 | 			this.filters = filters;
  385 | 		}
  386 | 
  387 | 		List<String> filter(List<String> configurations) {
  388 | 			long startTime = System.nanoTime();
  389 | 			@Nullable String[] candidates = StringUtils.toStringArray(configurations);
  390 | 			boolean skipped = false;
  391 | 			for (AutoConfigurationImportFilter filter : this.filters) {
  392 | 				boolean[] match = filter.match(candidates, this.autoConfigurationMetadata);
  393 | 				for (int i = 0; i < match.length; i++) {
  394 | 					if (!match[i]) {
  395 | 						candidates[i] = null;
  396 | 						skipped = true;
  397 | 					}
  398 | 				}
  399 | 			}
  400 | 			if (!skipped) {
  401 | 				return configurations;
  402 | 			}
  403 | 			List<String> result = new ArrayList<>(candidates.length);
  404 | 			for (String candidate : candidates) {
  405 | 				if (candidate != null) {
  406 | 					result.add(candidate);
  407 | 				}
  408 | 			}
  409 | 			if (logger.isTraceEnabled()) {
  410 | 				int numberFiltered = configurations.size() - result.size();
  411 | 				logger.trace("Filtered " + numberFiltered + " auto configuration class in "
  412 | 						+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
  413 | 			}
  414 | 			return result;
  415 | 		}
  416 | 
  417 | 	}
  418 | 
  419 | 	private static final class AutoConfigurationGroup
  420 | 			implements DeferredImportSelector.Group, BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware {
  421 | 
  422 | 		private final Map<String, AnnotationMetadata> entries = new LinkedHashMap<>();
  423 | 
  424 | 		private final List<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();
  425 | 
  426 | 		@SuppressWarnings("NullAway.Init")
  427 | 		private ClassLoader beanClassLoader;
  428 | 
  429 | 		@SuppressWarnings("NullAway.Init")
  430 | 		private BeanFactory beanFactory;
  431 | 
  432 | 		@SuppressWarnings("NullAway.Init")
  433 | 		private ResourceLoader resourceLoader;
  434 | 
  435 | 		private @Nullable AutoConfigurationMetadata autoConfigurationMetadata;
  436 | 
  437 | 		private @Nullable AutoConfigurationReplacements autoConfigurationReplacements;
  438 | 
  439 | 		@Override
  440 | 		public void setBeanClassLoader(ClassLoader classLoader) {
  441 | 			this.beanClassLoader = classLoader;
  442 | 		}
  443 | 
  444 | 		@Override
  445 | 		public void setBeanFactory(BeanFactory beanFactory) {
  446 | 			this.beanFactory = beanFactory;
  447 | 		}
  448 | 
  449 | 		@Override
  450 | 		public void setResourceLoader(ResourceLoader resourceLoader) {
  451 | 			this.resourceLoader = resourceLoader;
  452 | 		}
  453 | 
  454 | 		@Override
  455 | 		public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
  456 | 			Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
  457 | 					() -> String.format("Only %s implementations are supported, got %s",
  458 | 							AutoConfigurationImportSelector.class.getSimpleName(),
  459 | 							deferredImportSelector.getClass().getName()));
  460 | 			AutoConfigurationImportSelector autoConfigurationImportSelector = (AutoConfigurationImportSelector) deferredImportSelector;
  461 | 			AutoConfigurationReplacements autoConfigurationReplacements = autoConfigurationImportSelector
  462 | 				.getAutoConfigurationReplacements();
  463 | 			Assert.state(
  464 | 					this.autoConfigurationReplacements == null
  465 | 							|| this.autoConfigurationReplacements.equals(autoConfigurationReplacements),
  466 | 					"Auto-configuration replacements must be the same for each call to process");
  467 | 			this.autoConfigurationReplacements = autoConfigurationReplacements;
  468 | 			AutoConfigurationEntry autoConfigurationEntry = autoConfigurationImportSelector
  469 | 				.getAutoConfigurationEntry(annotationMetadata);
  470 | 			this.autoConfigurationEntries.add(autoConfigurationEntry);
  471 | 			for (String importClassName : autoConfigurationEntry.getConfigurations()) {
  472 | 				this.entries.putIfAbsent(importClassName, annotationMetadata);
  473 | 			}
  474 | 		}
  475 | 
  476 | 		@Override
  477 | 		public Iterable<Entry> selectImports() {
  478 | 			if (this.autoConfigurationEntries.isEmpty()) {
  479 | 				return Collections.emptyList();
  480 | 			}
  481 | 			Set<String> allExclusions = this.autoConfigurationEntries.stream()
  482 | 				.map(AutoConfigurationEntry::getExclusions)
  483 | 				.flatMap(Collection::stream)
  484 | 				.collect(Collectors.toSet());
  485 | 			Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
  486 | 				.map(AutoConfigurationEntry::getConfigurations)
  487 | 				.flatMap(Collection::stream)
  488 | 				.collect(Collectors.toCollection(LinkedHashSet::new));
  489 | 			processedConfigurations.removeAll(allExclusions);
  490 | 			return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
  491 | 				.map(this::getEntry)
  492 | 				.toList();
  493 | 		}
  494 | 
  495 | 		private Entry getEntry(String importClassName) {
  496 | 			AnnotationMetadata metadata = this.entries.get(importClassName);
  497 | 			Assert.state(metadata != null, "'metadata' must not be null");
  498 | 			return new Entry(metadata, importClassName);
  499 | 		}
  500 | 
  501 | 		private AutoConfigurationMetadata getAutoConfigurationMetadata() {
  502 | 			if (this.autoConfigurationMetadata == null) {
  503 | 				this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
  504 | 			}
  505 | 			return this.autoConfigurationMetadata;
  506 | 		}
  507 | 
  508 | 		private List<String> sortAutoConfigurations(Set<String> configurations,
  509 | 				AutoConfigurationMetadata autoConfigurationMetadata) {
  510 | 			Assert.state(this.autoConfigurationReplacements != null,
  511 | 					"'autoConfigurationReplacements' must not be null");
  512 | 			return new AutoConfigurationSorter(getMetadataReaderFactory(), autoConfigurationMetadata,
  513 | 					this.autoConfigurationReplacements::replace)
  514 | 				.getInPriorityOrder(configurations);
  515 | 		}
  516 | 
  517 | 		private MetadataReaderFactory getMetadataReaderFactory() {
  518 | 			try {
  519 | 				return this.beanFactory.getBean(SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
  520 | 						MetadataReaderFactory.class);
  521 | 			}
  522 | 			catch (NoSuchBeanDefinitionException ex) {
  523 | 				return new CachingMetadataReaderFactory(this.resourceLoader);
  524 | 			}
  525 | 		}
  526 | 
  527 | 	}
  528 | 
  529 | 	protected static class AutoConfigurationEntry {
  530 | 
  531 | 		private final List<String> configurations;
  532 | 
  533 | 		private final Set<String> exclusions;
  534 | 
  535 | 		private AutoConfigurationEntry() {
  536 | 			this.configurations = Collections.emptyList();
  537 | 			this.exclusions = Collections.emptySet();
  538 | 		}
  539 | 
  540 | 		/**
  541 | 		 * Create an entry with the configurations that were contributed and their
  542 | 		 * exclusions.
  543 | 		 * @param configurations the configurations that should be imported
  544 | 		 * @param exclusions the exclusions that were applied to the original list
  545 | 		 */
  546 | 		AutoConfigurationEntry(Collection<String> configurations, Collection<String> exclusions) {
  547 | 			this.configurations = new ArrayList<>(configurations);
  548 | 			this.exclusions = new HashSet<>(exclusions);
  549 | 		}
  550 | 
  551 | 		public List<String> getConfigurations() {
  552 | 			return this.configurations;
  553 | 		}
  554 | 
  555 | 		public Set<String> getExclusions() {
  556 | 			return this.exclusions;
  557 | 		}
  558 | 
  559 | 	}
  560 | 
  561 | }
```
