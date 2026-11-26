<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClassParser.java -->
<!-- 正文引用：[[源码:ConfigurationClassParser.java#L起-L止]] ；代码块头部：(src: ConfigurationClassParser.java#L起-L止) -->

# ConfigurationClassParser.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClassParser.java` |
| 行数 | 1179 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.551Z |

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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import java.io.IOException;
   20 | import java.lang.annotation.Annotation;
   21 | import java.util.ArrayDeque;
   22 | import java.util.ArrayList;
   23 | import java.util.Collection;
   24 | import java.util.Collections;
   25 | import java.util.Comparator;
   26 | import java.util.Deque;
   27 | import java.util.HashMap;
   28 | import java.util.HashSet;
   29 | import java.util.Iterator;
   30 | import java.util.LinkedHashMap;
   31 | import java.util.LinkedHashSet;
   32 | import java.util.List;
   33 | import java.util.Map;
   34 | import java.util.Set;
   35 | import java.util.StringJoiner;
   36 | import java.util.function.Predicate;
   37 | 
   38 | import org.apache.commons.logging.Log;
   39 | import org.apache.commons.logging.LogFactory;
   40 | import org.jspecify.annotations.Nullable;
   41 | 
   42 | import org.springframework.beans.BeanUtils;
   43 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   44 | import org.springframework.beans.factory.BeanRegistrar;
   45 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   46 | import org.springframework.beans.factory.config.BeanDefinition;
   47 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   48 | import org.springframework.beans.factory.parsing.Location;
   49 | import org.springframework.beans.factory.parsing.Problem;
   50 | import org.springframework.beans.factory.parsing.ProblemReporter;
   51 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   52 | import org.springframework.beans.factory.support.BeanDefinitionReader;
   53 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   54 | import org.springframework.beans.factory.support.BeanNameGenerator;
   55 | import org.springframework.context.ApplicationContextException;
   56 | import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
   57 | import org.springframework.context.annotation.DeferredImportSelector.Group;
   58 | import org.springframework.core.OrderComparator;
   59 | import org.springframework.core.Ordered;
   60 | import org.springframework.core.annotation.AnnotationAttributes;
   61 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   62 | import org.springframework.core.annotation.AnnotationUtils;
   63 | import org.springframework.core.annotation.MergedAnnotation;
   64 | import org.springframework.core.env.ConfigurableEnvironment;
   65 | import org.springframework.core.env.Environment;
   66 | import org.springframework.core.io.ResourceLoader;
   67 | import org.springframework.core.io.support.PropertySourceDescriptor;
   68 | import org.springframework.core.io.support.PropertySourceProcessor;
   69 | import org.springframework.core.type.AnnotationMetadata;
   70 | import org.springframework.core.type.MethodMetadata;
   71 | import org.springframework.core.type.StandardAnnotationMetadata;
   72 | import org.springframework.core.type.classreading.MetadataReader;
   73 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   74 | import org.springframework.core.type.filter.AssignableTypeFilter;
   75 | import org.springframework.stereotype.Component;
   76 | import org.springframework.util.Assert;
   77 | import org.springframework.util.ClassUtils;
   78 | import org.springframework.util.CollectionUtils;
   79 | import org.springframework.util.LinkedMultiValueMap;
   80 | import org.springframework.util.MultiValueMap;
   81 | import org.springframework.util.StringUtils;
   82 | 
   83 | /**
   84 |  * Parses a {@link Configuration} class definition, populating a collection of
   85 |  * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
   86 |  * any number of ConfigurationClass objects because one Configuration class may import
   87 |  * another using the {@link Import} annotation).
   88 |  *
   89 |  * <p>This class helps separate the concern of parsing the structure of a Configuration class
   90 |  * from the concern of registering BeanDefinition objects based on the content of that model
   91 |  * (except {@code @ComponentScan} annotations which need to be registered immediately).
   92 |  *
   93 |  * <p>This ASM-based implementation avoids reflection and eager class loading in order to
   94 |  * interoperate effectively with lazy class loading in a Spring ApplicationContext.
   95 |  *
   96 |  * @author Chris Beams
   97 |  * @author Juergen Hoeller
   98 |  * @author Phillip Webb
   99 |  * @author Sam Brannen
  100 |  * @author Stephane Nicoll
  101 |  * @author Daeho Kwon
  102 |  * @since 3.0
  103 |  * @see ConfigurationClassBeanDefinitionReader
  104 |  */
  105 | class ConfigurationClassParser {
  106 | 
  107 | 	private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
  108 | 			(className.startsWith("java.lang.annotation.") || className.startsWith("org.springframework.stereotype."));
  109 | 
  110 | 	private static final Predicate<Condition> REGISTER_BEAN_CONDITION_FILTER = condition ->
  111 | 			(condition instanceof ConfigurationCondition configurationCondition &&
  112 | 				ConfigurationPhase.REGISTER_BEAN.equals(configurationCondition.getConfigurationPhase()));
  113 | 
  114 | 	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
  115 | 			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());
  116 | 
  117 | 
  118 | 	private final Log logger = LogFactory.getLog(getClass());
  119 | 
  120 | 	private final MetadataReaderFactory metadataReaderFactory;
  121 | 
  122 | 	private final ProblemReporter problemReporter;
  123 | 
  124 | 	private final Environment environment;
  125 | 
  126 | 	private final ResourceLoader resourceLoader;
  127 | 
  128 | 	private final @Nullable PropertySourceRegistry propertySourceRegistry;
  129 | 
  130 | 	private final BeanDefinitionRegistry registry;
  131 | 
  132 | 	private final ComponentScanAnnotationParser componentScanParser;
  133 | 
  134 | 	private final ConditionEvaluator conditionEvaluator;
  135 | 
  136 | 	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
  137 | 
  138 | 	private final MultiValueMap<String, ConfigurationClass> knownSuperclasses = new LinkedMultiValueMap<>();
  139 | 
  140 | 	private final ImportStack importStack = new ImportStack();
  141 | 
  142 | 	private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();
  143 | 
  144 | 	private final SourceClass objectSourceClass = new SourceClass(Object.class);
  145 | 
  146 | 
  147 | 	/**
  148 | 	 * Create a new {@link ConfigurationClassParser} instance that will be used
  149 | 	 * to populate the set of configuration classes.
  150 | 	 */
  151 | 	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
  152 | 			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
  153 | 			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {
  154 | 
  155 | 		this.metadataReaderFactory = metadataReaderFactory;
  156 | 		this.problemReporter = problemReporter;
  157 | 		this.environment = environment;
  158 | 		this.resourceLoader = resourceLoader;
  159 | 		this.propertySourceRegistry = (this.environment instanceof ConfigurableEnvironment ce ?
  160 | 				new PropertySourceRegistry(new PropertySourceProcessor(ce, this.resourceLoader)) : null);
  161 | 		this.registry = registry;
  162 | 		this.componentScanParser = new ComponentScanAnnotationParser(
  163 | 				environment, resourceLoader, componentScanBeanNameGenerator, registry);
  164 | 		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
  165 | 	}
  166 | 
  167 | 
  168 | 	public void parse(Set<BeanDefinitionHolder> configCandidates) {
  169 | 		for (BeanDefinitionHolder holder : configCandidates) {
  170 | 			BeanDefinition bd = holder.getBeanDefinition();
  171 | 			try {
  172 | 				ConfigurationClass configClass;
  173 | 				if (bd instanceof AnnotatedBeanDefinition annotatedBeanDef) {
  174 | 					configClass = parse(annotatedBeanDef, holder.getBeanName());
  175 | 				}
  176 | 				else if (bd instanceof AbstractBeanDefinition abstractBeanDef && abstractBeanDef.hasBeanClass()) {
  177 | 					configClass = parse(abstractBeanDef.getBeanClass(), holder.getBeanName());
  178 | 				}
  179 | 				else {
  180 | 					configClass = parse(bd.getBeanClassName(), holder.getBeanName());
  181 | 				}
  182 | 
  183 | 				// Downgrade to lite (no enhancement) in case of no instance-level @Bean methods.
  184 | 				if (!configClass.getMetadata().isAbstract() && !configClass.hasNonStaticBeanMethods() &&
  185 | 						ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(
  186 | 								bd.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE))) {
  187 | 					bd.setAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE,
  188 | 							ConfigurationClassUtils.CONFIGURATION_CLASS_LITE);
  189 | 				}
  190 | 			}
  191 | 			catch (BeanDefinitionStoreException ex) {
  192 | 				throw ex;
  193 | 			}
  194 | 			catch (Throwable ex) {
  195 | 				throw new BeanDefinitionStoreException(
  196 | 						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
  197 | 			}
  198 | 		}
  199 | 
  200 | 		this.deferredImportSelectorHandler.process();
  201 | 	}
  202 | 
  203 | 	private ConfigurationClass parse(AnnotatedBeanDefinition beanDef, String beanName) {
  204 | 		ConfigurationClass configClass = new ConfigurationClass(
  205 | 				beanDef.getMetadata(), beanName, (beanDef instanceof ScannedGenericBeanDefinition));
  206 | 		processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
  207 | 		return configClass;
  208 | 	}
  209 | 
  210 | 	private ConfigurationClass parse(Class<?> clazz, String beanName) {
  211 | 		ConfigurationClass configClass = new ConfigurationClass(clazz, beanName);
  212 | 		processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
  213 | 		return configClass;
  214 | 	}
  215 | 
  216 | 	final ConfigurationClass parse(@Nullable String className, String beanName) throws IOException {
  217 | 		Assert.notNull(className, "No bean class name for configuration class bean definition");
  218 | 		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
  219 | 		ConfigurationClass configClass = new ConfigurationClass(reader, beanName);
  220 | 		processConfigurationClass(configClass, DEFAULT_EXCLUSION_FILTER);
  221 | 		return configClass;
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Validate each {@link ConfigurationClass} object.
  226 | 	 * @see ConfigurationClass#validate
  227 | 	 */
  228 | 	void validate() {
  229 | 		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
  230 | 			configClass.validate(this.problemReporter);
  231 | 		}
  232 | 	}
  233 | 
  234 | 	Set<ConfigurationClass> getConfigurationClasses() {
  235 | 		return this.configurationClasses.keySet();
  236 | 	}
  237 | 
  238 | 	List<PropertySourceDescriptor> getPropertySourceDescriptors() {
  239 | 		return (this.propertySourceRegistry != null ? this.propertySourceRegistry.getDescriptors() :
  240 | 				Collections.emptyList());
  241 | 	}
  242 | 
  243 | 	ImportRegistry getImportRegistry() {
  244 | 		return this.importStack;
  245 | 	}
  246 | 
  247 | 
  248 | 	protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) {
  249 | 		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
  250 | 			return;
  251 | 		}
  252 | 
  253 | 		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
  254 | 		if (existingClass != null) {
  255 | 			if (configClass.isImported()) {
  256 | 				if (existingClass.isImported()) {
  257 | 					existingClass.mergeImportedBy(configClass);
  258 | 				}
  259 | 				// Otherwise ignore new imported config class; existing non-imported class overrides it.
  260 | 				return;
  261 | 			}
  262 | 			else if (configClass.isScanned()) {
  263 | 				if (existingClass.isImported()) {
  264 | 					String beanName = configClass.getBeanName();
  265 | 					if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
  266 | 						this.registry.removeBeanDefinition(beanName);
  267 | 					}
  268 | 				}
  269 | 				// An implicitly scanned bean definition should not override an explicit import.
  270 | 				return;
  271 | 			}
  272 | 			else {
  273 | 				// Explicit bean definition found, probably replacing an import.
  274 | 				// Let's remove the old one and go with the new one.
  275 | 				this.configurationClasses.remove(configClass);
  276 | 				removeKnownSuperclass(configClass.getMetadata().getClassName(), false);
  277 | 			}
  278 | 		}
  279 | 
  280 | 		// Recursively process the configuration class and its superclass hierarchy.
  281 | 		SourceClass sourceClass = null;
  282 | 		try {
  283 | 			sourceClass = asSourceClass(configClass, filter);
  284 | 			do {
  285 | 				sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
  286 | 			}
  287 | 			while (sourceClass != null);
  288 | 		}
  289 | 		catch (IOException ex) {
  290 | 			throw new BeanDefinitionStoreException(
  291 | 					"I/O failure while processing configuration class [" + sourceClass + "]", ex);
  292 | 		}
  293 | 
  294 | 		this.configurationClasses.put(configClass, configClass);
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Apply processing and build a complete {@link ConfigurationClass} by reading the
  299 | 	 * annotations, members and methods from the source class. This method can be called
  300 | 	 * multiple times as relevant sources are discovered.
  301 | 	 * @param configClass the configuration class being build
  302 | 	 * @param sourceClass a source class
  303 | 	 * @return the superclass, or {@code null} if none found or previously processed
  304 | 	 */
  305 | 	protected final @Nullable SourceClass doProcessConfigurationClass(
  306 | 			ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
  307 | 			throws IOException {
  308 | 
  309 | 		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
  310 | 			// Recursively process any member (nested) classes first
  311 | 			processMemberClasses(configClass, sourceClass, filter);
  312 | 		}
  313 | 
  314 | 		// Process any @PropertySource annotations
  315 | 		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
  316 | 				sourceClass.getMetadata(), org.springframework.context.annotation.PropertySource.class,
  317 | 				PropertySources.class, true)) {
  318 | 			if (this.propertySourceRegistry != null) {
  319 | 				this.propertySourceRegistry.processPropertySource(propertySource);
  320 | 			}
  321 | 			else {
  322 | 				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
  323 | 						"]. Reason: Environment must implement ConfigurableEnvironment");
  324 | 			}
  325 | 		}
  326 | 
  327 | 		// Search for locally declared @ComponentScan annotations first.
  328 | 		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
  329 | 				sourceClass.getMetadata(), ComponentScan.class, ComponentScans.class,
  330 | 				MergedAnnotation::isDirectlyPresent);
  331 | 
  332 | 		// Fall back to searching for @ComponentScan meta-annotations (which indirectly
  333 | 		// includes locally declared composed annotations).
  334 | 		if (componentScans.isEmpty()) {
  335 | 			componentScans = AnnotationConfigUtils.attributesForRepeatable(sourceClass.getMetadata(),
  336 | 					ComponentScan.class, ComponentScans.class, MergedAnnotation::isMetaPresent);
  337 | 		}
  338 | 
  339 | 		if (!componentScans.isEmpty()) {
  340 | 			List<Condition> registerBeanConditions = collectRegisterBeanConditions(configClass);
  341 | 			if (!registerBeanConditions.isEmpty()) {
  342 | 				throw new ApplicationContextException(
  343 | 						"Component scan for configuration class [%s] could not be used with conditions in REGISTER_BEAN phase: %s"
  344 | 								.formatted(configClass.getMetadata().getClassName(), registerBeanConditions));
  345 | 			}
  346 | 			for (AnnotationAttributes componentScan : componentScans) {
  347 | 				// The config class is annotated with @ComponentScan -> perform the scan immediately
  348 | 				Set<BeanDefinitionHolder> scannedBeanDefinitions =
  349 | 						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
  350 | 				// Check the set of scanned definitions for any further config classes and parse recursively if needed
  351 | 				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
  352 | 					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
  353 | 					if (bdCand == null) {
  354 | 						bdCand = holder.getBeanDefinition();
  355 | 					}
  356 | 					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
  357 | 						parse(bdCand.getBeanClassName(), holder.getBeanName());
  358 | 					}
  359 | 				}
  360 | 			}
  361 | 		}
  362 | 
  363 | 		// Process any @Import annotations
  364 | 		processImports(configClass, sourceClass, getImports(sourceClass), filter, true);
  365 | 
  366 | 		// Process any @ImportResource annotations
  367 | 		AnnotationAttributes importResource =
  368 | 				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
  369 | 		if (importResource != null) {
  370 | 			String[] resources = importResource.getStringArray("locations");
  371 | 			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
  372 | 			for (String resource : resources) {
  373 | 				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
  374 | 				configClass.addImportedResource(resolvedResource, readerClass);
  375 | 			}
  376 | 		}
  377 | 
  378 | 		// Process individual @Bean methods
  379 | 		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
  380 | 		for (MethodMetadata methodMetadata : beanMethods) {
  381 | 			if (methodMetadata.isAnnotated("kotlin.jvm.JvmStatic") && !methodMetadata.isStatic()) {
  382 | 				continue;
  383 | 			}
  384 | 			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
  385 | 		}
  386 | 
  387 | 		// Process default methods on interfaces
  388 | 		processInterfaces(configClass, sourceClass);
  389 | 
  390 | 		// Process superclass, if any
  391 | 		if (sourceClass.getMetadata().hasSuperClass()) {
  392 | 			String superclass = sourceClass.getMetadata().getSuperClassName();
  393 | 			if (superclass != null && !superclass.startsWith("java")) {
  394 | 				boolean superclassKnown = this.knownSuperclasses.containsKey(superclass);
  395 | 				this.knownSuperclasses.add(superclass, configClass);
  396 | 				if (!superclassKnown) {
  397 | 					// Superclass found, return its annotation metadata and recurse
  398 | 					return sourceClass.getSuperClass();
  399 | 				}
  400 | 			}
  401 | 		}
  402 | 
  403 | 		// No superclass -> processing is complete
  404 | 		return null;
  405 | 	}
  406 | 
  407 | 	/**
  408 | 	 * Register member (nested) classes that happen to be configuration classes themselves.
  409 | 	 */
  410 | 	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass,
  411 | 			Predicate<String> filter) throws IOException {
  412 | 
  413 | 		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
  414 | 		if (!memberClasses.isEmpty()) {
  415 | 			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
  416 | 			for (SourceClass memberClass : memberClasses) {
  417 | 				if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
  418 | 						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
  419 | 					candidates.add(memberClass);
  420 | 				}
  421 | 			}
  422 | 			OrderComparator.sort(candidates);
  423 | 			for (SourceClass candidate : candidates) {
  424 | 				if (this.importStack.contains(configClass)) {
  425 | 					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
  426 | 				}
  427 | 				else {
  428 | 					this.importStack.push(configClass);
  429 | 					try {
  430 | 						processConfigurationClass(candidate.asConfigClass(configClass), filter);
  431 | 					}
  432 | 					finally {
  433 | 						this.importStack.pop();
  434 | 					}
  435 | 				}
  436 | 			}
  437 | 		}
  438 | 	}
  439 | 
  440 | 	/**
  441 | 	 * Register default methods on interfaces implemented by the configuration class.
  442 | 	 */
  443 | 	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
  444 | 		for (SourceClass ifc : sourceClass.getInterfaces()) {
  445 | 			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
  446 | 			for (MethodMetadata methodMetadata : beanMethods) {
  447 | 				if (!methodMetadata.isAbstract()) {
  448 | 					// A default method or other concrete method on a Java interface...
  449 | 					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
  450 | 				}
  451 | 			}
  452 | 			processInterfaces(configClass, ifc);
  453 | 		}
  454 | 	}
  455 | 
  456 | 	/**
  457 | 	 * Retrieve the metadata for all <code>@Bean</code> methods.
  458 | 	 */
  459 | 	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
  460 | 		AnnotationMetadata original = sourceClass.getMetadata();
  461 | 		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
  462 | 		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
  463 | 			// Try reading the class file via ASM for deterministic declaration order...
  464 | 			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
  465 | 			// order, even between different runs of the same application on the same JVM.
  466 | 			try {
  467 | 				AnnotationMetadata asm =
  468 | 						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
  469 | 				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
  470 | 				if (asmMethods.size() >= beanMethods.size()) {
  471 | 					Set<MethodMetadata> candidateMethods = new LinkedHashSet<>(beanMethods);
  472 | 					Set<MethodMetadata> selectedMethods = CollectionUtils.newLinkedHashSet(asmMethods.size());
  473 | 					for (MethodMetadata asmMethod : asmMethods) {
  474 | 						for (Iterator<MethodMetadata> it = candidateMethods.iterator(); it.hasNext();) {
  475 | 							MethodMetadata beanMethod = it.next();
  476 | 							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
  477 | 								selectedMethods.add(beanMethod);
  478 | 								it.remove();
  479 | 								break;
  480 | 							}
  481 | 						}
  482 | 					}
  483 | 					if (selectedMethods.size() == beanMethods.size()) {
  484 | 						// All reflection-detected methods found in ASM method set -> proceed
  485 | 						beanMethods = selectedMethods;
  486 | 					}
  487 | 				}
  488 | 			}
  489 | 			catch (IOException ex) {
  490 | 				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
  491 | 				// No worries, let's continue with the reflection metadata we started with...
  492 | 			}
  493 | 		}
  494 | 		return beanMethods;
  495 | 	}
  496 | 
  497 | 	/**
  498 | 	 * Remove known superclasses for the given removed class, potentially replacing
  499 | 	 * the superclass exposure on a different config class with the same superclass.
  500 | 	 */
  501 | 	private void removeKnownSuperclass(String removedClass, boolean replace) {
  502 | 		String replacedSuperclass = null;
  503 | 		ConfigurationClass replacingClass = null;
  504 | 
  505 | 		Iterator<Map.Entry<String, List<ConfigurationClass>>> it = this.knownSuperclasses.entrySet().iterator();
  506 | 		while (it.hasNext()) {
  507 | 			Map.Entry<String, List<ConfigurationClass>> entry = it.next();
  508 | 			if (entry.getValue().removeIf(configClass -> configClass.getMetadata().getClassName().equals(removedClass))) {
  509 | 				if (entry.getValue().isEmpty()) {
  510 | 					it.remove();
  511 | 				}
  512 | 				else if (replace && replacingClass == null) {
  513 | 					replacedSuperclass = entry.getKey();
  514 | 					replacingClass = entry.getValue().get(0);
  515 | 				}
  516 | 			}
  517 | 		}
  518 | 
  519 | 		if (replacingClass != null) {
  520 | 			try {
  521 | 				SourceClass sourceClass = asSourceClass(replacingClass, DEFAULT_EXCLUSION_FILTER).getSuperClass();
  522 | 				while (!sourceClass.getMetadata().getClassName().equals(replacedSuperclass) &&
  523 | 						sourceClass.getMetadata().getSuperClassName() != null) {
  524 | 					sourceClass = sourceClass.getSuperClass();
  525 | 				}
  526 | 				do {
  527 | 					sourceClass = doProcessConfigurationClass(replacingClass, sourceClass, DEFAULT_EXCLUSION_FILTER);
  528 | 				}
  529 | 				while (sourceClass != null);
  530 | 			}
  531 | 			catch (IOException ex) {
  532 | 				throw new BeanDefinitionStoreException(
  533 | 						"I/O failure while removing configuration class [" + removedClass + "]", ex);
  534 | 			}
  535 | 		}
  536 | 	}
  537 | 
  538 | 	/**
  539 | 	 * Returns {@code @Import} classes, considering all meta-annotations.
  540 | 	 */
  541 | 	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
  542 | 		Set<SourceClass> imports = new LinkedHashSet<>();
  543 | 		collectImports(sourceClass, imports, new HashSet<>());
  544 | 		return imports;
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Recursively collect all declared {@code @Import} values. Unlike most
  549 | 	 * meta-annotations it is valid to have several {@code @Import}s declared with
  550 | 	 * different values; the usual process of returning values from the first
  551 | 	 * meta-annotation on a class is not sufficient.
  552 | 	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
  553 | 	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
  554 | 	 * annotation.
  555 | 	 * <p>As of Spring Framework 7.0, {@code @Import} annotations declared on interfaces
  556 | 	 * implemented by the configuration class are also considered. This allows imports to
  557 | 	 * be triggered indirectly via marker interfaces or shared base interfaces.
  558 | 	 * @param sourceClass the class to search
  559 | 	 * @param imports the imports collected so far
  560 | 	 * @param visited used to track visited classes and interfaces to prevent infinite
  561 | 	 * recursion
  562 | 	 * @throws IOException if there is any problem reading metadata from the named class
  563 | 	 */
  564 | 	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
  565 | 			throws IOException {
  566 | 
  567 | 		if (visited.add(sourceClass)) {
  568 | 			for (SourceClass ifc : sourceClass.getInterfaces()) {
  569 | 				collectImports(ifc, imports, visited);
  570 | 			}
  571 | 			for (SourceClass annotation : sourceClass.getAnnotations()) {
  572 | 				String annName = annotation.getMetadata().getClassName();
  573 | 				if (!annName.equals(Import.class.getName())) {
  574 | 					collectImports(annotation, imports, visited);
  575 | 				}
  576 | 			}
  577 | 			imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
  578 | 		}
  579 | 	}
  580 | 
  581 | 	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
  582 | 			Collection<SourceClass> importCandidates, Predicate<String> filter, boolean checkForCircularImports) {
  583 | 
  584 | 		if (importCandidates.isEmpty()) {
  585 | 			return;
  586 | 		}
  587 | 
  588 | 		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
  589 | 			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
  590 | 		}
  591 | 		else {
  592 | 			this.importStack.push(configClass);
  593 | 			try {
  594 | 				for (SourceClass candidate : importCandidates) {
  595 | 					if (candidate.isAssignable(ImportSelector.class)) {
  596 | 						// Candidate class is an ImportSelector -> delegate to it to determine imports
  597 | 						Class<?> candidateClass = candidate.loadClass();
  598 | 						ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
  599 | 								this.environment, this.resourceLoader, this.registry);
  600 | 						Predicate<String> selectorFilter = selector.getExclusionFilter();
  601 | 						if (selectorFilter != null) {
  602 | 							filter = filter.or(selectorFilter);
  603 | 						}
  604 | 						if (selector instanceof DeferredImportSelector deferredImportSelector) {
  605 | 							this.deferredImportSelectorHandler.handle(configClass, deferredImportSelector);
  606 | 						}
  607 | 						else {
  608 | 							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
  609 | 							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, filter);
  610 | 							processImports(configClass, currentSourceClass, importSourceClasses, filter, false);
  611 | 						}
  612 | 					}
  613 | 					else if (candidate.isAssignable(BeanRegistrar.class)) {
  614 | 						Class<?> candidateClass = candidate.loadClass();
  615 | 						BeanRegistrar registrar = (BeanRegistrar) BeanUtils.instantiateClass(candidateClass);
  616 | 						AnnotationMetadata metadata = currentSourceClass.getMetadata();
  617 | 						if (registrar instanceof ImportAware importAware) {
  618 | 							importAware.setImportMetadata(metadata);
  619 | 						}
  620 | 						configClass.addBeanRegistrar(metadata.getClassName(), registrar);
  621 | 					}
  622 | 					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
  623 | 						// Candidate class is an ImportBeanDefinitionRegistrar ->
  624 | 						// delegate to it to register additional bean definitions
  625 | 						Class<?> candidateClass = candidate.loadClass();
  626 | 						ImportBeanDefinitionRegistrar registrar =
  627 | 								ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
  628 | 										this.environment, this.resourceLoader, this.registry);
  629 | 						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
  630 | 					}
  631 | 					else {
  632 | 						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
  633 | 						// process it as an @Configuration class
  634 | 						this.importStack.registerImport(
  635 | 								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
  636 | 						processConfigurationClass(candidate.asConfigClass(configClass), filter);
  637 | 					}
  638 | 				}
  639 | 			}
  640 | 			catch (BeanDefinitionStoreException ex) {
  641 | 				throw ex;
  642 | 			}
  643 | 			catch (Throwable ex) {
  644 | 				throw new BeanDefinitionStoreException(
  645 | 						"Failed to process import candidates for configuration class [" +
  646 | 						configClass.getMetadata().getClassName() + "]: " + ex.getMessage(), ex);
  647 | 			}
  648 | 			finally {
  649 | 				this.importStack.pop();
  650 | 			}
  651 | 		}
  652 | 	}
  653 | 
  654 | 	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
  655 | 		if (this.importStack.contains(configClass)) {
  656 | 			String configClassName = configClass.getMetadata().getClassName();
  657 | 			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
  658 | 			while (importingClass != null) {
  659 | 				if (configClassName.equals(importingClass.getClassName())) {
  660 | 					return true;
  661 | 				}
  662 | 				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
  663 | 			}
  664 | 		}
  665 | 		return false;
  666 | 	}
  667 | 
  668 | 	/**
  669 | 	 * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
  670 | 	 */
  671 | 	private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) throws IOException {
  672 | 		AnnotationMetadata metadata = configurationClass.getMetadata();
  673 | 		if (metadata instanceof StandardAnnotationMetadata standardAnnotationMetadata) {
  674 | 			return asSourceClass(standardAnnotationMetadata.getIntrospectedClass(), filter);
  675 | 		}
  676 | 		return asSourceClass(metadata.getClassName(), filter);
  677 | 	}
  678 | 
  679 | 	/**
  680 | 	 * Factory method to obtain a {@link SourceClass} from a {@link Class}.
  681 | 	 */
  682 | 	SourceClass asSourceClass(@Nullable Class<?> classType, Predicate<String> filter) throws IOException {
  683 | 		if (classType == null || filter.test(classType.getName())) {
  684 | 			return this.objectSourceClass;
  685 | 		}
  686 | 		try {
  687 | 			// Sanity test that we can reflectively read annotations,
  688 | 			// including Class attributes; if not -> fall back to ASM
  689 | 			for (Annotation ann : classType.getDeclaredAnnotations()) {
  690 | 				AnnotationUtils.validateAnnotation(ann);
  691 | 			}
  692 | 			return new SourceClass(classType);
  693 | 		}
  694 | 		catch (Throwable ex) {
  695 | 			// Enforce ASM via class name resolution
  696 | 			return asSourceClass(classType.getName(), filter);
  697 | 		}
  698 | 	}
  699 | 
  700 | 	/**
  701 | 	 * Factory method to obtain a {@link SourceClass} collection from class names.
  702 | 	 */
  703 | 	private Collection<SourceClass> asSourceClasses(String[] classNames, Predicate<String> filter) throws IOException {
  704 | 		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
  705 | 		for (String className : classNames) {
  706 | 			SourceClass sourceClass = asSourceClass(className, filter);
  707 | 			if (this.objectSourceClass != sourceClass) {
  708 | 				annotatedClasses.add(sourceClass);
  709 | 			}
  710 | 		}
  711 | 		return annotatedClasses;
  712 | 	}
  713 | 
  714 | 	/**
  715 | 	 * Factory method to obtain a {@link SourceClass} from a class name.
  716 | 	 */
  717 | 	SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) throws IOException {
  718 | 		if (className == null || filter.test(className)) {
  719 | 			return this.objectSourceClass;
  720 | 		}
  721 | 		if (className.startsWith("java")) {
  722 | 			// Never use ASM for core java types
  723 | 			try {
  724 | 				return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
  725 | 			}
  726 | 			catch (ClassNotFoundException ex) {
  727 | 				throw new IOException("Failed to load class [" + className + "]", ex);
  728 | 			}
  729 | 		}
  730 | 		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
  731 | 	}
  732 | 
  733 | 	private List<Condition> collectRegisterBeanConditions(ConfigurationClass configurationClass) {
  734 | 		AnnotationMetadata metadata = configurationClass.getMetadata();
  735 | 		List<Condition> allConditions = new ArrayList<>(this.conditionEvaluator.collectConditions(metadata));
  736 | 		ConfigurationClass enclosingConfigurationClass = getEnclosingConfigurationClass(configurationClass);
  737 | 		if (enclosingConfigurationClass != null) {
  738 | 			allConditions.addAll(this.conditionEvaluator.collectConditions(enclosingConfigurationClass.getMetadata()));
  739 | 		}
  740 | 		return allConditions.stream().filter(REGISTER_BEAN_CONDITION_FILTER).toList();
  741 | 	}
  742 | 
  743 | 	private @Nullable ConfigurationClass getEnclosingConfigurationClass(ConfigurationClass configurationClass) {
  744 | 		String enclosingClassName = configurationClass.getMetadata().getEnclosingClassName();
  745 | 		if (enclosingClassName != null) {
  746 | 			return configurationClass.getImportedBy().stream()
  747 | 					.filter(candidate -> enclosingClassName.equals(candidate.getMetadata().getClassName()))
  748 | 					.findFirst().orElse(null);
  749 | 		}
  750 | 		return null;
  751 | 	}
  752 | 
  753 | 
  754 | 	@SuppressWarnings("serial")
  755 | 	private class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {
  756 | 
  757 | 		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();
  758 | 
  759 | 		void registerImport(AnnotationMetadata importingClass, String importedClass) {
  760 | 			this.imports.add(importedClass, importingClass);
  761 | 		}
  762 | 
  763 | 		@Override
  764 | 		public @Nullable AnnotationMetadata getImportingClassFor(String importedClass) {
  765 | 			return CollectionUtils.lastElement(this.imports.get(importedClass));
  766 | 		}
  767 | 
  768 | 		@Override
  769 | 		public void removeImportingClass(String importingClass) {
  770 | 			for (List<AnnotationMetadata> list : this.imports.values()) {
  771 | 				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext();) {
  772 | 					if (iterator.next().getClassName().equals(importingClass)) {
  773 | 						iterator.remove();
  774 | 						break;
  775 | 					}
  776 | 				}
  777 | 			}
  778 | 			removeKnownSuperclass(importingClass, true);
  779 | 		}
  780 | 
  781 | 		/**
  782 | 		 * Given a stack containing (in order)
  783 | 		 * <ul>
  784 | 		 * <li>com.acme.Foo</li>
  785 | 		 * <li>com.acme.Bar</li>
  786 | 		 * <li>com.acme.Baz</li>
  787 | 		 * </ul>
  788 | 		 * return "[Foo->Bar->Baz]".
  789 | 		 */
  790 | 		@Override
  791 | 		public String toString() {
  792 | 			StringJoiner joiner = new StringJoiner("->", "[", "]");
  793 | 			for (ConfigurationClass configurationClass : this) {
  794 | 				joiner.add(configurationClass.getSimpleName());
  795 | 			}
  796 | 			return joiner.toString();
  797 | 		}
  798 | 	}
  799 | 
  800 | 
  801 | 	private class DeferredImportSelectorHandler {
  802 | 
  803 | 		private @Nullable List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();
  804 | 
  805 | 		/**
  806 | 		 * Handle the specified {@link DeferredImportSelector}. If deferred import
  807 | 		 * selectors are being collected, this registers this instance to the list. If
  808 | 		 * they are being processed, the {@link DeferredImportSelector} is also processed
  809 | 		 * immediately according to its {@link DeferredImportSelector.Group}.
  810 | 		 * @param configClass the source configuration class
  811 | 		 * @param importSelector the selector to handle
  812 | 		 */
  813 | 		void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
  814 | 			DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
  815 | 			if (this.deferredImportSelectors == null) {
  816 | 				DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
  817 | 				handler.register(holder);
  818 | 				handler.processGroupImports();
  819 | 			}
  820 | 			else {
  821 | 				this.deferredImportSelectors.add(holder);
  822 | 			}
  823 | 		}
  824 | 
  825 | 		void process() {
  826 | 			List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
  827 | 			this.deferredImportSelectors = null;
  828 | 			try {
  829 | 				if (deferredImports != null) {
  830 | 					DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
  831 | 					deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
  832 | 					deferredImports.forEach(handler::register);
  833 | 					handler.processGroupImports();
  834 | 				}
  835 | 			}
  836 | 			finally {
  837 | 				this.deferredImportSelectors = new ArrayList<>();
  838 | 			}
  839 | 		}
  840 | 	}
  841 | 
  842 | 
  843 | 	private class DeferredImportSelectorGroupingHandler {
  844 | 
  845 | 		private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();
  846 | 
  847 | 		private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();
  848 | 
  849 | 		void register(DeferredImportSelectorHolder deferredImport) {
  850 | 			Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
  851 | 			DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(
  852 | 					(group != null ? group : deferredImport),
  853 | 					key -> new DeferredImportSelectorGrouping(createGroup(group)));
  854 | 			grouping.add(deferredImport);
  855 | 			this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
  856 | 					deferredImport.getConfigurationClass());
  857 | 		}
  858 | 
  859 | 		void processGroupImports() {
  860 | 			for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
  861 | 				Predicate<String> filter = grouping.getCandidateFilter();
  862 | 				grouping.getImports().forEach(entry -> {
  863 | 					ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());
  864 | 					Assert.state(configurationClass != null, "ConfigurationClass must not be null");
  865 | 					try {
  866 | 						processImports(configurationClass, asSourceClass(configurationClass, filter),
  867 | 								Collections.singleton(asSourceClass(entry.getImportClassName(), filter)),
  868 | 								filter, false);
  869 | 					}
  870 | 					catch (BeanDefinitionStoreException ex) {
  871 | 						throw ex;
  872 | 					}
  873 | 					catch (Throwable ex) {
  874 | 						throw new BeanDefinitionStoreException(
  875 | 								"Failed to process import candidates for configuration class [" +
  876 | 										configurationClass.getMetadata().getClassName() + "]", ex);
  877 | 					}
  878 | 				});
  879 | 			}
  880 | 		}
  881 | 
  882 | 		private Group createGroup(@Nullable Class<? extends Group> type) {
  883 | 			Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
  884 | 			return ParserStrategyUtils.instantiateClass(effectiveType, Group.class,
  885 | 					ConfigurationClassParser.this.environment,
  886 | 					ConfigurationClassParser.this.resourceLoader,
  887 | 					ConfigurationClassParser.this.registry);
  888 | 		}
  889 | 	}
  890 | 
  891 | 
  892 | 	private static class DeferredImportSelectorHolder {
  893 | 
  894 | 		private final ConfigurationClass configurationClass;
  895 | 
  896 | 		private final DeferredImportSelector importSelector;
  897 | 
  898 | 		DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
  899 | 			this.configurationClass = configClass;
  900 | 			this.importSelector = selector;
  901 | 		}
  902 | 
  903 | 		ConfigurationClass getConfigurationClass() {
  904 | 			return this.configurationClass;
  905 | 		}
  906 | 
  907 | 		DeferredImportSelector getImportSelector() {
  908 | 			return this.importSelector;
  909 | 		}
  910 | 	}
  911 | 
  912 | 
  913 | 	private static class DeferredImportSelectorGrouping {
  914 | 
  915 | 		private final DeferredImportSelector.Group group;
  916 | 
  917 | 		private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();
  918 | 
  919 | 		DeferredImportSelectorGrouping(Group group) {
  920 | 			this.group = group;
  921 | 		}
  922 | 
  923 | 		void add(DeferredImportSelectorHolder deferredImport) {
  924 | 			this.deferredImports.add(deferredImport);
  925 | 		}
  926 | 
  927 | 		/**
  928 | 		 * Return the imports defined by the group.
  929 | 		 * @return each import with its associated configuration class
  930 | 		 */
  931 | 		Iterable<Group.Entry> getImports() {
  932 | 			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
  933 | 				this.group.process(deferredImport.getConfigurationClass().getMetadata(),
  934 | 						deferredImport.getImportSelector());
  935 | 			}
  936 | 			return this.group.selectImports();
  937 | 		}
  938 | 
  939 | 		Predicate<String> getCandidateFilter() {
  940 | 			Predicate<String> mergedFilter = DEFAULT_EXCLUSION_FILTER;
  941 | 			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
  942 | 				Predicate<String> selectorFilter = deferredImport.getImportSelector().getExclusionFilter();
  943 | 				if (selectorFilter != null) {
  944 | 					mergedFilter = mergedFilter.or(selectorFilter);
  945 | 				}
  946 | 			}
  947 | 			return mergedFilter;
  948 | 		}
  949 | 	}
  950 | 
  951 | 
  952 | 	private static class DefaultDeferredImportSelectorGroup implements Group {
  953 | 
  954 | 		private final List<Entry> imports = new ArrayList<>();
  955 | 
  956 | 		@Override
  957 | 		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
  958 | 			for (String importClassName : selector.selectImports(metadata)) {
  959 | 				this.imports.add(new Entry(metadata, importClassName));
  960 | 			}
  961 | 		}
  962 | 
  963 | 		@Override
  964 | 		public Iterable<Entry> selectImports() {
  965 | 			return this.imports;
  966 | 		}
  967 | 	}
  968 | 
  969 | 
  970 | 	/**
  971 | 	 * Simple wrapper that allows annotated source classes to be dealt with
  972 | 	 * in a uniform manner, regardless of how they are loaded.
  973 | 	 */
  974 | 	private class SourceClass implements Ordered {
  975 | 
  976 | 		private final Object source;  // Class or MetadataReader
  977 | 
  978 | 		private final AnnotationMetadata metadata;
  979 | 
  980 | 		public SourceClass(Object source) {
  981 | 			this.source = source;
  982 | 			if (source instanceof Class<?> sourceClass) {
  983 | 				this.metadata = AnnotationMetadata.introspect(sourceClass);
  984 | 			}
  985 | 			else {
  986 | 				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
  987 | 			}
  988 | 		}
  989 | 
  990 | 		public final AnnotationMetadata getMetadata() {
  991 | 			return this.metadata;
  992 | 		}
  993 | 
  994 | 		@Override
  995 | 		public int getOrder() {
  996 | 			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
  997 | 			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
  998 | 		}
  999 | 
 1000 | 		public Class<?> loadClass() throws ClassNotFoundException {
 1001 | 			if (this.source instanceof Class<?> sourceClass) {
 1002 | 				return sourceClass;
 1003 | 			}
 1004 | 			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
 1005 | 			return ClassUtils.forName(className, resourceLoader.getClassLoader());
 1006 | 		}
 1007 | 
 1008 | 		public boolean isAssignable(Class<?> clazz) throws IOException {
 1009 | 			if (this.source instanceof Class<?> sourceClass) {
 1010 | 				return clazz.isAssignableFrom(sourceClass);
 1011 | 			}
 1012 | 			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
 1013 | 		}
 1014 | 
 1015 | 		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
 1016 | 			if (this.source instanceof Class<?> sourceClass) {
 1017 | 				return new ConfigurationClass(sourceClass, importedBy);
 1018 | 			}
 1019 | 			return new ConfigurationClass((MetadataReader) this.source, importedBy);
 1020 | 		}
 1021 | 
 1022 | 		public Collection<SourceClass> getMemberClasses() throws IOException {
 1023 | 			Object sourceToProcess = this.source;
 1024 | 			if (sourceToProcess instanceof Class<?> sourceClass) {
 1025 | 				try {
 1026 | 					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
 1027 | 					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
 1028 | 					for (Class<?> declaredClass : declaredClasses) {
 1029 | 						members.add(asSourceClass(declaredClass, DEFAULT_EXCLUSION_FILTER));
 1030 | 					}
 1031 | 					return members;
 1032 | 				}
 1033 | 				catch (NoClassDefFoundError err) {
 1034 | 					// getDeclaredClasses() failed because of non-resolvable dependencies
 1035 | 					// -> fall back to ASM below
 1036 | 					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
 1037 | 				}
 1038 | 			}
 1039 | 
 1040 | 			// ASM-based resolution - safe for non-resolvable classes as well
 1041 | 			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
 1042 | 			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
 1043 | 			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
 1044 | 			for (String memberClassName : memberClassNames) {
 1045 | 				try {
 1046 | 					members.add(asSourceClass(memberClassName, DEFAULT_EXCLUSION_FILTER));
 1047 | 				}
 1048 | 				catch (IOException ex) {
 1049 | 					// Let's skip it if it's not resolvable - we're just looking for candidates
 1050 | 					if (logger.isDebugEnabled()) {
 1051 | 						logger.debug("Failed to resolve member class [" + memberClassName +
 1052 | 								"] - not considering it as a configuration class candidate");
 1053 | 					}
 1054 | 				}
 1055 | 			}
 1056 | 			return members;
 1057 | 		}
 1058 | 
 1059 | 		public SourceClass getSuperClass() throws IOException {
 1060 | 			if (this.source instanceof Class<?> sourceClass) {
 1061 | 				return asSourceClass(sourceClass.getSuperclass(), DEFAULT_EXCLUSION_FILTER);
 1062 | 			}
 1063 | 			return asSourceClass(
 1064 | 					((MetadataReader) this.source).getClassMetadata().getSuperClassName(), DEFAULT_EXCLUSION_FILTER);
 1065 | 		}
 1066 | 
 1067 | 		public Set<SourceClass> getInterfaces() throws IOException {
 1068 | 			Set<SourceClass> result = new LinkedHashSet<>();
 1069 | 			if (this.source instanceof Class<?> sourceClass) {
 1070 | 				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
 1071 | 					result.add(asSourceClass(ifcClass, DEFAULT_EXCLUSION_FILTER));
 1072 | 				}
 1073 | 			}
 1074 | 			else {
 1075 | 				for (String className : this.metadata.getInterfaceNames()) {
 1076 | 					result.add(asSourceClass(className, DEFAULT_EXCLUSION_FILTER));
 1077 | 				}
 1078 | 			}
 1079 | 			return result;
 1080 | 		}
 1081 | 
 1082 | 		public Set<SourceClass> getAnnotations() {
 1083 | 			Set<SourceClass> result = new LinkedHashSet<>();
 1084 | 			if (this.source instanceof Class<?> sourceClass) {
 1085 | 				for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
 1086 | 					Class<?> annType = ann.annotationType();
 1087 | 					if (!annType.getName().startsWith("java")) {
 1088 | 						try {
 1089 | 							result.add(asSourceClass(annType, DEFAULT_EXCLUSION_FILTER));
 1090 | 						}
 1091 | 						catch (Throwable ex) {
 1092 | 							// An annotation not present on the classpath is being ignored
 1093 | 							// by the JVM's class loading -> ignore here as well.
 1094 | 						}
 1095 | 					}
 1096 | 				}
 1097 | 			}
 1098 | 			else {
 1099 | 				for (String className : this.metadata.getAnnotationTypes()) {
 1100 | 					if (!className.startsWith("java")) {
 1101 | 						try {
 1102 | 							result.add(getRelated(className));
 1103 | 						}
 1104 | 						catch (Throwable ex) {
 1105 | 							// An annotation not present on the classpath is being ignored
 1106 | 							// by the JVM's class loading -> ignore here as well.
 1107 | 						}
 1108 | 					}
 1109 | 				}
 1110 | 			}
 1111 | 			return result;
 1112 | 		}
 1113 | 
 1114 | 		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
 1115 | 			Map<String, @Nullable Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
 1116 | 			if (annotationAttributes == null) {
 1117 | 				return Collections.emptySet();
 1118 | 			}
 1119 | 			String[] classNames = (String[]) annotationAttributes.get(attribute);
 1120 | 			if (classNames == null) {
 1121 | 				return Collections.emptySet();
 1122 | 			}
 1123 | 			Set<SourceClass> result = CollectionUtils.newLinkedHashSet(classNames.length);
 1124 | 			for (String className : classNames) {
 1125 | 				result.add(getRelated(className));
 1126 | 			}
 1127 | 			return result;
 1128 | 		}
 1129 | 
 1130 | 		private SourceClass getRelated(String className) throws IOException {
 1131 | 			if (this.source instanceof Class<?> sourceClass) {
 1132 | 				try {
 1133 | 					Class<?> clazz = ClassUtils.forName(className, sourceClass.getClassLoader());
 1134 | 					return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
 1135 | 				}
 1136 | 				catch (ClassNotFoundException ex) {
 1137 | 					// Ignore -> fall back to ASM next, except for core java types.
 1138 | 					if (className.startsWith("java")) {
 1139 | 						throw new IOException("Failed to load class [" + className + "]", ex);
 1140 | 					}
 1141 | 					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
 1142 | 				}
 1143 | 			}
 1144 | 			return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
 1145 | 		}
 1146 | 
 1147 | 		@Override
 1148 | 		public boolean equals(@Nullable Object other) {
 1149 | 			return (this == other || (other instanceof SourceClass that &&
 1150 | 					this.metadata.getClassName().equals(that.metadata.getClassName())));
 1151 | 		}
 1152 | 
 1153 | 		@Override
 1154 | 		public int hashCode() {
 1155 | 			return this.metadata.getClassName().hashCode();
 1156 | 		}
 1157 | 
 1158 | 		@Override
 1159 | 		public String toString() {
 1160 | 			return this.metadata.getClassName();
 1161 | 		}
 1162 | 	}
 1163 | 
 1164 | 
 1165 | 	/**
 1166 | 	 * {@link Problem} registered upon detection of a circular {@link Import}.
 1167 | 	 */
 1168 | 	private static class CircularImportProblem extends Problem {
 1169 | 
 1170 | 		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
 1171 | 			super(String.format("A circular @Import has been detected: " +
 1172 | 					"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
 1173 | 					"already present in the current import stack %s", importStack.element().getSimpleName(),
 1174 | 					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
 1175 | 					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
 1176 | 		}
 1177 | 	}
 1178 | 
 1179 | }
```
