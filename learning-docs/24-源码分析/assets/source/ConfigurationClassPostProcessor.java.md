<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClassPostProcessor.java -->
<!-- 正文引用：[[源码:ConfigurationClassPostProcessor.java#L起-L止]] ；代码块头部：(src: ConfigurationClassPostProcessor.java#L起-L止) -->

# ConfigurationClassPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClassPostProcessor.java` |
| 行数 | 1101 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.813Z |

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
   20 | import java.io.UncheckedIOException;
   21 | import java.lang.reflect.Constructor;
   22 | import java.lang.reflect.Executable;
   23 | import java.lang.reflect.Method;
   24 | import java.util.ArrayList;
   25 | import java.util.Arrays;
   26 | import java.util.Collections;
   27 | import java.util.HashMap;
   28 | import java.util.HashSet;
   29 | import java.util.LinkedHashMap;
   30 | import java.util.LinkedHashSet;
   31 | import java.util.List;
   32 | import java.util.Map;
   33 | import java.util.Set;
   34 | import java.util.function.Function;
   35 | import java.util.function.Predicate;
   36 | import java.util.function.Supplier;
   37 | 
   38 | import javax.lang.model.element.Modifier;
   39 | 
   40 | import org.apache.commons.logging.Log;
   41 | import org.apache.commons.logging.LogFactory;
   42 | import org.jspecify.annotations.Nullable;
   43 | 
   44 | import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
   45 | import org.springframework.aot.generate.AccessControl;
   46 | import org.springframework.aot.generate.GeneratedClass;
   47 | import org.springframework.aot.generate.GeneratedMethod;
   48 | import org.springframework.aot.generate.GeneratedMethods;
   49 | import org.springframework.aot.generate.GenerationContext;
   50 | import org.springframework.aot.generate.MethodReference;
   51 | import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
   52 | import org.springframework.aot.hint.ExecutableMode;
   53 | import org.springframework.aot.hint.MemberCategory;
   54 | import org.springframework.aot.hint.ReflectionHints;
   55 | import org.springframework.aot.hint.ResourceHints;
   56 | import org.springframework.aot.hint.RuntimeHints;
   57 | import org.springframework.aot.hint.TypeReference;
   58 | import org.springframework.beans.BeanUtils;
   59 | import org.springframework.beans.PropertyValues;
   60 | import org.springframework.beans.factory.BeanClassLoaderAware;
   61 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   62 | import org.springframework.beans.factory.BeanFactory;
   63 | import org.springframework.beans.factory.BeanRegistrar;
   64 | import org.springframework.beans.factory.ListableBeanFactory;
   65 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   66 | import org.springframework.beans.factory.aot.AotServices;
   67 | import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
   68 | import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
   69 | import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
   70 | import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
   71 | import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
   72 | import org.springframework.beans.factory.aot.BeanRegistrationCode;
   73 | import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
   74 | import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
   75 | import org.springframework.beans.factory.aot.InstanceSupplierCodeGenerator;
   76 | import org.springframework.beans.factory.config.BeanDefinition;
   77 | import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
   78 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   79 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   80 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   81 | import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
   82 | import org.springframework.beans.factory.config.SingletonBeanRegistry;
   83 | import org.springframework.beans.factory.parsing.FailFastProblemReporter;
   84 | import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
   85 | import org.springframework.beans.factory.parsing.ProblemReporter;
   86 | import org.springframework.beans.factory.parsing.SourceExtractor;
   87 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   88 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   89 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   90 | import org.springframework.beans.factory.support.BeanNameGenerator;
   91 | import org.springframework.beans.factory.support.BeanRegistryAdapter;
   92 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   93 | import org.springframework.beans.factory.support.RegisteredBean;
   94 | import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
   95 | import org.springframework.beans.factory.support.RootBeanDefinition;
   96 | import org.springframework.context.ApplicationStartupAware;
   97 | import org.springframework.context.EnvironmentAware;
   98 | import org.springframework.context.ResourceLoaderAware;
   99 | import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
  100 | import org.springframework.core.Ordered;
  101 | import org.springframework.core.PriorityOrdered;
  102 | import org.springframework.core.env.ConfigurableEnvironment;
  103 | import org.springframework.core.env.Environment;
  104 | import org.springframework.core.env.StandardEnvironment;
  105 | import org.springframework.core.io.ClassPathResource;
  106 | import org.springframework.core.io.DefaultResourceLoader;
  107 | import org.springframework.core.io.Resource;
  108 | import org.springframework.core.io.ResourceLoader;
  109 | import org.springframework.core.io.support.PropertySourceDescriptor;
  110 | import org.springframework.core.io.support.PropertySourceProcessor;
  111 | import org.springframework.core.io.support.ResourcePatternResolver;
  112 | import org.springframework.core.metrics.ApplicationStartup;
  113 | import org.springframework.core.metrics.StartupStep;
  114 | import org.springframework.core.type.AnnotationMetadata;
  115 | import org.springframework.core.type.MethodMetadata;
  116 | import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
  117 | import org.springframework.core.type.classreading.MetadataReaderFactory;
  118 | import org.springframework.javapoet.ClassName;
  119 | import org.springframework.javapoet.CodeBlock;
  120 | import org.springframework.javapoet.CodeBlock.Builder;
  121 | import org.springframework.javapoet.MethodSpec;
  122 | import org.springframework.javapoet.NameAllocator;
  123 | import org.springframework.javapoet.ParameterizedTypeName;
  124 | import org.springframework.util.Assert;
  125 | import org.springframework.util.ClassUtils;
  126 | import org.springframework.util.CollectionUtils;
  127 | import org.springframework.util.LinkedMultiValueMap;
  128 | import org.springframework.util.MultiValueMap;
  129 | import org.springframework.util.ObjectUtils;
  130 | import org.springframework.util.ReflectionUtils;
  131 | import org.springframework.util.StringUtils;
  132 | 
  133 | /**
  134 |  * {@link BeanFactoryPostProcessor} used for bootstrapping processing of
  135 |  * {@link Configuration @Configuration} classes.
  136 |  *
  137 |  * <p>Registered by default when using {@code <context:annotation-config/>} or
  138 |  * {@code <context:component-scan/>}. Otherwise, may be declared manually as
  139 |  * with any other {@link BeanFactoryPostProcessor}.
  140 |  *
  141 |  * <p>This post processor is priority-ordered as it is important that any
  142 |  * {@link Bean @Bean} methods declared in {@code @Configuration} classes have
  143 |  * their corresponding bean definitions registered before any other
  144 |  * {@code BeanFactoryPostProcessor} executes.
  145 |  *
  146 |  * @author Chris Beams
  147 |  * @author Juergen Hoeller
  148 |  * @author Phillip Webb
  149 |  * @author Sam Brannen
  150 |  * @since 3.0
  151 |  */
  152 | public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
  153 | 		BeanRegistrationAotProcessor, BeanFactoryInitializationAotProcessor, PriorityOrdered,
  154 | 		ResourceLoaderAware, ApplicationStartupAware, BeanClassLoaderAware, EnvironmentAware {
  155 | 
  156 | 	/**
  157 | 	 * A {@code BeanNameGenerator} using fully qualified class names as default bean names.
  158 | 	 * <p>This default for configuration-level import purposes may be overridden through
  159 | 	 * {@link #setBeanNameGenerator}. Note that the default for component scanning purposes
  160 | 	 * is a plain {@link AnnotationBeanNameGenerator#INSTANCE}, unless overridden through
  161 | 	 * {@link #setBeanNameGenerator} with a unified user-level bean name generator.
  162 | 	 * @since 5.2
  163 | 	 * @see #setBeanNameGenerator
  164 | 	 */
  165 | 	public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
  166 | 			FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;
  167 | 
  168 | 	private static final String IMPORT_REGISTRY_BEAN_NAME =
  169 | 			ConfigurationClassPostProcessor.class.getName() + ".importRegistry";
  170 | 
  171 | 
  172 | 	private final Log logger = LogFactory.getLog(getClass());
  173 | 
  174 | 	private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();
  175 | 
  176 | 	private ProblemReporter problemReporter = new FailFastProblemReporter();
  177 | 
  178 | 	private @Nullable Environment environment;
  179 | 
  180 | 	private ResourceLoader resourceLoader = new DefaultResourceLoader();
  181 | 
  182 | 	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
  183 | 
  184 | 	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
  185 | 
  186 | 	private boolean setMetadataReaderFactoryCalled = false;
  187 | 
  188 | 	private final Set<Integer> registriesPostProcessed = new HashSet<>();
  189 | 
  190 | 	private final Set<Integer> factoriesPostProcessed = new HashSet<>();
  191 | 
  192 | 	private @Nullable ConfigurationClassBeanDefinitionReader reader;
  193 | 
  194 | 	private boolean localBeanNameGeneratorSet = false;
  195 | 
  196 | 	/* Using short class names as default bean names by default. */
  197 | 	private BeanNameGenerator componentScanBeanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
  198 | 
  199 | 	/* Using fully qualified class names as default bean names by default. */
  200 | 	private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;
  201 | 
  202 | 	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;
  203 | 
  204 | 	private List<PropertySourceDescriptor> propertySourceDescriptors = Collections.emptyList();
  205 | 
  206 | 	private final MultiValueMap<String, BeanRegistrar> beanRegistrars = new LinkedMultiValueMap<>();
  207 | 
  208 | 
  209 | 	@Override
  210 | 	public int getOrder() {
  211 | 		return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Set the {@link SourceExtractor} to use for generated bean definitions
  216 | 	 * that correspond to {@link Bean} factory methods.
  217 | 	 */
  218 | 	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
  219 | 		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Set the {@link ProblemReporter} to use.
  224 | 	 * <p>Used to register any problems detected with {@link Configuration} or {@link Bean}
  225 | 	 * declarations. For instance, a @Bean method marked as {@code final} is illegal
  226 | 	 * and would be reported as a problem. Defaults to {@link FailFastProblemReporter}.
  227 | 	 */
  228 | 	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
  229 | 		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Set the {@link MetadataReaderFactory} to use.
  234 | 	 * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
  235 | 	 * {@linkplain #setBeanClassLoader bean class loader}.
  236 | 	 */
  237 | 	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
  238 | 		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
  239 | 		this.metadataReaderFactory = metadataReaderFactory;
  240 | 		this.setMetadataReaderFactoryCalled = true;
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Set the {@link BeanNameGenerator} to be used when triggering component scanning
  245 | 	 * from {@link Configuration @Configuration} classes and when registering
  246 | 	 * {@link Import @Import}'ed configuration classes.
  247 | 	 * <p>The default is a standard {@link AnnotationBeanNameGenerator} for scanned
  248 | 	 * components (compatible with the default in {@link ClassPathBeanDefinitionScanner})
  249 | 	 * and a variant thereof for imported configuration classes (using unique fully-qualified
  250 | 	 * class names instead of standard component overriding).
  251 | 	 * <p>If the supplied bean name generator is a {@link ConfigurationBeanNameGenerator}
  252 | 	 * (such as {@link FullyQualifiedConfigurationBeanNameGenerator}), it also affects the
  253 | 	 * default names for {@link Bean @Bean} methods in configuration classes.
  254 | 	 * <p>This setter is typically only appropriate when configuring the post-processor as a
  255 | 	 * standalone bean definition in XML, for example, not using the dedicated {@code AnnotationConfig*}
  256 | 	 * application contexts or the {@code <context:annotation-config>} element. Any bean name
  257 | 	 * generator specified against the application context will take precedence over any set here.
  258 | 	 * @since 3.1.1
  259 | 	 * @see AnnotationConfigApplicationContext#setBeanNameGenerator(BeanNameGenerator)
  260 | 	 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
  261 | 	 * @see AnnotationBeanNameGenerator
  262 | 	 * @see FullyQualifiedAnnotationBeanNameGenerator
  263 | 	 * @see FullyQualifiedConfigurationBeanNameGenerator
  264 | 	 */
  265 | 	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
  266 | 		Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
  267 | 		this.localBeanNameGeneratorSet = true;
  268 | 		this.componentScanBeanNameGenerator = beanNameGenerator;
  269 | 		this.importBeanNameGenerator = beanNameGenerator;
  270 | 	}
  271 | 
  272 | 	@Override
  273 | 	public void setEnvironment(Environment environment) {
  274 | 		Assert.notNull(environment, "Environment must not be null");
  275 | 		this.environment = environment;
  276 | 	}
  277 | 
  278 | 	@Override
  279 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  280 | 		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
  281 | 		this.resourceLoader = resourceLoader;
  282 | 		if (!this.setMetadataReaderFactoryCalled) {
  283 | 			this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
  284 | 		}
  285 | 	}
  286 | 
  287 | 	@Override
  288 | 	public void setBeanClassLoader(ClassLoader beanClassLoader) {
  289 | 		this.beanClassLoader = beanClassLoader;
  290 | 		if (!this.setMetadataReaderFactoryCalled) {
  291 | 			this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
  292 | 		}
  293 | 	}
  294 | 
  295 | 	@Override
  296 | 	public void setApplicationStartup(ApplicationStartup applicationStartup) {
  297 | 		this.applicationStartup = applicationStartup;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Derive further bean definitions from the configuration classes in the registry.
  302 | 	 */
  303 | 	@Override
  304 | 	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
  305 | 		int registryId = System.identityHashCode(registry);
  306 | 		if (this.registriesPostProcessed.contains(registryId)) {
  307 | 			throw new IllegalStateException(
  308 | 					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
  309 | 		}
  310 | 		if (this.factoriesPostProcessed.contains(registryId)) {
  311 | 			throw new IllegalStateException(
  312 | 					"postProcessBeanFactory already called on this post-processor against " + registry);
  313 | 		}
  314 | 		this.registriesPostProcessed.add(registryId);
  315 | 
  316 | 		processConfigBeanDefinitions(registry);
  317 | 	}
  318 | 
  319 | 	/**
  320 | 	 * Prepare the Configuration classes for servicing bean requests at runtime
  321 | 	 * by replacing them with CGLIB-enhanced subclasses.
  322 | 	 */
  323 | 	@Override
  324 | 	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  325 | 		int factoryId = System.identityHashCode(beanFactory);
  326 | 		if (this.factoriesPostProcessed.contains(factoryId)) {
  327 | 			throw new IllegalStateException(
  328 | 					"postProcessBeanFactory already called on this post-processor against " + beanFactory);
  329 | 		}
  330 | 		this.factoriesPostProcessed.add(factoryId);
  331 | 		if (!this.registriesPostProcessed.contains(factoryId)) {
  332 | 			// BeanDefinitionRegistryPostProcessor hook apparently not supported...
  333 | 			// Simply call processConfigurationClasses lazily at this point then.
  334 | 			processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
  335 | 		}
  336 | 
  337 | 		enhanceConfigurationClasses(beanFactory);
  338 | 		beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
  339 | 	}
  340 | 
  341 | 	@Override
  342 | 	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
  343 | 		Object configClassAttr = registeredBean.getMergedBeanDefinition()
  344 | 				.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
  345 | 		if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
  346 | 			return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
  347 | 					new ConfigurationClassProxyBeanRegistrationCodeFragments(codeFragments, registeredBean));
  348 | 		}
  349 | 		return null;
  350 | 	}
  351 | 
  352 | 	@Override
  353 | 	public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
  354 | 		boolean hasPropertySourceDescriptors = !CollectionUtils.isEmpty(this.propertySourceDescriptors);
  355 | 		boolean hasImportRegistry = beanFactory.containsBean(IMPORT_REGISTRY_BEAN_NAME);
  356 | 		boolean hasBeanRegistrars = !this.beanRegistrars.isEmpty();
  357 | 		if (hasPropertySourceDescriptors || hasImportRegistry || hasBeanRegistrars) {
  358 | 			return (generationContext, code) -> {
  359 | 				if (hasPropertySourceDescriptors) {
  360 | 					new PropertySourcesAotContribution(this.propertySourceDescriptors, this::resolvePropertySourceLocation)
  361 | 							.applyTo(generationContext, code);
  362 | 				}
  363 | 				if (hasImportRegistry) {
  364 | 					new ImportAwareAotContribution(beanFactory).applyTo(generationContext, code);
  365 | 				}
  366 | 				if (hasBeanRegistrars) {
  367 | 					new BeanRegistrarAotContribution(this.beanRegistrars, beanFactory).applyTo(generationContext, code);
  368 | 				}
  369 | 			};
  370 | 		}
  371 | 		return null;
  372 | 	}
  373 | 
  374 | 	private @Nullable Resource resolvePropertySourceLocation(String location) {
  375 | 		try {
  376 | 			String resolvedLocation = (this.environment != null ?
  377 | 					this.environment.resolveRequiredPlaceholders(location) : location);
  378 | 			return this.resourceLoader.getResource(resolvedLocation);
  379 | 		}
  380 | 		catch (Exception ex) {
  381 | 			return null;
  382 | 		}
  383 | 	}
  384 | 
  385 | 	/**
  386 | 	 * Build and validate a configuration model based on the registry of
  387 | 	 * {@link Configuration} classes.
  388 | 	 */
  389 | 	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
  390 | 		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
  391 | 		String[] candidateNames = registry.getBeanDefinitionNames();
  392 | 
  393 | 		for (String beanName : candidateNames) {
  394 | 			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
  395 | 			if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
  396 | 				if (logger.isDebugEnabled()) {
  397 | 					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
  398 | 				}
  399 | 			}
  400 | 			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
  401 | 				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
  402 | 			}
  403 | 		}
  404 | 
  405 | 		// Return immediately if no @Configuration classes were found
  406 | 		if (configCandidates.isEmpty()) {
  407 | 			return;
  408 | 		}
  409 | 
  410 | 		// Sort by previously determined @Order value, if applicable
  411 | 		configCandidates.sort((bd1, bd2) -> {
  412 | 			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
  413 | 			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
  414 | 			return Integer.compare(i1, i2);
  415 | 		});
  416 | 
  417 | 		// Detect any custom bean name generation strategy supplied through the enclosing application context
  418 | 		SingletonBeanRegistry singletonRegistry = null;
  419 | 		if (registry instanceof SingletonBeanRegistry sbr) {
  420 | 			singletonRegistry = sbr;
  421 | 			BeanNameGenerator configurationGenerator = (BeanNameGenerator) singletonRegistry.getSingleton(
  422 | 					AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
  423 | 			if (configurationGenerator != null) {
  424 | 				if (this.localBeanNameGeneratorSet) {
  425 | 					if (configurationGenerator instanceof ConfigurationBeanNameGenerator &
  426 | 							configurationGenerator != this.importBeanNameGenerator) {
  427 | 						throw new IllegalStateException("Context-level ConfigurationBeanNameGenerator [" +
  428 | 								configurationGenerator + "] must not be overridden with processor-level generator [" +
  429 | 								this.importBeanNameGenerator + "]");
  430 | 					}
  431 | 				}
  432 | 				else {
  433 | 					this.componentScanBeanNameGenerator = configurationGenerator;
  434 | 					this.importBeanNameGenerator = configurationGenerator;
  435 | 				}
  436 | 			}
  437 | 		}
  438 | 
  439 | 		if (this.environment == null) {
  440 | 			this.environment = new StandardEnvironment();
  441 | 		}
  442 | 
  443 | 		// Parse each @Configuration class
  444 | 		ConfigurationClassParser parser = new ConfigurationClassParser(
  445 | 				this.metadataReaderFactory, this.problemReporter, this.environment,
  446 | 				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
  447 | 
  448 | 		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
  449 | 		Set<ConfigurationClass> alreadyParsed = CollectionUtils.newHashSet(configCandidates.size());
  450 | 		do {
  451 | 			StartupStep processConfig = this.applicationStartup.start("spring.context.config-classes.parse");
  452 | 			parser.parse(candidates);
  453 | 			parser.validate();
  454 | 
  455 | 			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
  456 | 			configClasses.removeAll(alreadyParsed);
  457 | 
  458 | 			// Read the model and create bean definitions based on its content
  459 | 			if (this.reader == null) {
  460 | 				this.reader = new ConfigurationClassBeanDefinitionReader(
  461 | 						registry, this.sourceExtractor, this.resourceLoader, this.environment,
  462 | 						this.importBeanNameGenerator, parser.getImportRegistry());
  463 | 			}
  464 | 			this.reader.loadBeanDefinitions(configClasses);
  465 | 			for (ConfigurationClass configClass : configClasses) {
  466 | 				this.beanRegistrars.addAll(configClass.getBeanRegistrars());
  467 | 			}
  468 | 			alreadyParsed.addAll(configClasses);
  469 | 			processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();
  470 | 
  471 | 			candidates.clear();
  472 | 			if (registry.getBeanDefinitionCount() > candidateNames.length) {
  473 | 				String[] newCandidateNames = registry.getBeanDefinitionNames();
  474 | 				Set<String> oldCandidateNames = Set.of(candidateNames);
  475 | 				Set<String> alreadyParsedClasses = CollectionUtils.newHashSet(alreadyParsed.size());
  476 | 				for (ConfigurationClass configurationClass : alreadyParsed) {
  477 | 					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
  478 | 				}
  479 | 				for (String candidateName : newCandidateNames) {
  480 | 					if (!oldCandidateNames.contains(candidateName)) {
  481 | 						BeanDefinition bd = registry.getBeanDefinition(candidateName);
  482 | 						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
  483 | 								!alreadyParsedClasses.contains(bd.getBeanClassName())) {
  484 | 							candidates.add(new BeanDefinitionHolder(bd, candidateName));
  485 | 						}
  486 | 					}
  487 | 				}
  488 | 				candidateNames = newCandidateNames;
  489 | 			}
  490 | 		}
  491 | 		while (!candidates.isEmpty());
  492 | 
  493 | 		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
  494 | 		if (singletonRegistry != null && !singletonRegistry.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
  495 | 			singletonRegistry.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
  496 | 		}
  497 | 
  498 | 		// Store the PropertySourceDescriptors to contribute them Ahead-of-time if necessary
  499 | 		this.propertySourceDescriptors = parser.getPropertySourceDescriptors();
  500 | 
  501 | 		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory cachingMetadataReaderFactory) {
  502 | 			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
  503 | 			// for a shared cache since it'll be cleared by the ApplicationContext.
  504 | 			cachingMetadataReaderFactory.clearCache();
  505 | 		}
  506 | 	}
  507 | 
  508 | 	/**
  509 | 	 * Post-processes a BeanFactory in search of Configuration class BeanDefinitions;
  510 | 	 * any candidates are then enhanced by a {@link ConfigurationClassEnhancer}.
  511 | 	 * Candidate status is determined by BeanDefinition attribute metadata.
  512 | 	 * @see ConfigurationClassEnhancer
  513 | 	 */
  514 | 	public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
  515 | 		StartupStep enhanceConfigClasses = this.applicationStartup.start("spring.context.config-classes.enhance");
  516 | 		Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
  517 | 		for (String beanName : beanFactory.getBeanDefinitionNames()) {
  518 | 			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
  519 | 			Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
  520 | 			AnnotationMetadata annotationMetadata = null;
  521 | 			MethodMetadata methodMetadata = null;
  522 | 			if (beanDef instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
  523 | 				annotationMetadata = annotatedBeanDefinition.getMetadata();
  524 | 				methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
  525 | 			}
  526 | 			if ((configClassAttr != null || methodMetadata != null) &&
  527 | 					(beanDef instanceof AbstractBeanDefinition abd) && !abd.hasBeanClass()) {
  528 | 				// Configuration class (full or lite) or a configuration-derived @Bean method
  529 | 				// -> eagerly resolve bean class at this point, unless it's a 'lite' configuration
  530 | 				// or component class without @Bean methods.
  531 | 				boolean liteConfigurationCandidateWithoutBeanMethods =
  532 | 						(ConfigurationClassUtils.CONFIGURATION_CLASS_LITE.equals(configClassAttr) &&
  533 | 							annotationMetadata != null && !ConfigurationClassUtils.hasBeanMethods(annotationMetadata));
  534 | 				if (!liteConfigurationCandidateWithoutBeanMethods) {
  535 | 					try {
  536 | 						abd.resolveBeanClass(this.beanClassLoader);
  537 | 					}
  538 | 					catch (Throwable ex) {
  539 | 						throw new IllegalStateException(
  540 | 								"Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
  541 | 					}
  542 | 				}
  543 | 			}
  544 | 			if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
  545 | 				if (!(beanDef instanceof AbstractBeanDefinition abd)) {
  546 | 					throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
  547 | 							beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
  548 | 				}
  549 | 				else if (beanFactory.containsSingleton(beanName)) {
  550 | 					if (logger.isWarnEnabled()) {
  551 | 						logger.warn("Cannot enhance @Configuration bean definition '" + beanName +
  552 | 								"' since its singleton instance has been created too early. The typical cause " +
  553 | 								"is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
  554 | 								"return type: Consider declaring such methods as 'static' and/or marking the " +
  555 | 								"containing configuration class as 'proxyBeanMethods=false'.");
  556 | 					}
  557 | 				}
  558 | 				else {
  559 | 					configBeanDefs.put(beanName, abd);
  560 | 				}
  561 | 			}
  562 | 		}
  563 | 		if (configBeanDefs.isEmpty()) {
  564 | 			// nothing to enhance -> return immediately
  565 | 			enhanceConfigClasses.end();
  566 | 			return;
  567 | 		}
  568 | 
  569 | 		ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
  570 | 		for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
  571 | 			AbstractBeanDefinition beanDef = entry.getValue();
  572 | 			// If a @Configuration class gets proxied, always proxy the target class
  573 | 			beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
  574 | 			// Set enhanced subclass of the user-specified bean class
  575 | 			Class<?> configClass = beanDef.getBeanClass();
  576 | 			Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
  577 | 			if (configClass != enhancedClass) {
  578 | 				if (logger.isTraceEnabled()) {
  579 | 					logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
  580 | 							"enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
  581 | 				}
  582 | 				beanDef.setBeanClass(enhancedClass);
  583 | 			}
  584 | 		}
  585 | 		enhanceConfigClasses.tag("classCount", () -> String.valueOf(configBeanDefs.keySet().size())).end();
  586 | 	}
  587 | 
  588 | 
  589 | 	private static class ImportAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
  590 | 
  591 | 		private final BeanFactory beanFactory;
  592 | 
  593 | 		public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
  594 | 			this.beanFactory = beanFactory;
  595 | 		}
  596 | 
  597 | 		@Override
  598 | 		public @Nullable PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
  599 | 			// Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
  600 | 			// postProcessProperties method attempts to autowire other configuration beans.
  601 | 			if (bean instanceof EnhancedConfiguration enhancedConfiguration) {
  602 | 				enhancedConfiguration.setBeanFactory(this.beanFactory);
  603 | 			}
  604 | 			return pvs;
  605 | 		}
  606 | 
  607 | 		@Override
  608 | 		public Object postProcessBeforeInitialization(Object bean, String beanName) {
  609 | 			if (bean instanceof ImportAware importAware) {
  610 | 				ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
  611 | 				AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
  612 | 				if (importingClass != null) {
  613 | 					importAware.setImportMetadata(importingClass);
  614 | 				}
  615 | 			}
  616 | 			return bean;
  617 | 		}
  618 | 	}
  619 | 
  620 | 
  621 | 	private static class ImportAwareAotContribution implements BeanFactoryInitializationAotContribution {
  622 | 
  623 | 		private static final String BEAN_FACTORY_VARIABLE = BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE;
  624 | 
  625 | 		private static final ParameterizedTypeName STRING_STRING_MAP =
  626 | 				ParameterizedTypeName.get(Map.class, String.class, String.class);
  627 | 
  628 | 		private static final String MAPPINGS_VARIABLE = "mappings";
  629 | 
  630 | 		private static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";
  631 | 
  632 | 		private static final String BEAN_NAME = "org.springframework.context.annotation.internalImportAwareAotProcessor";
  633 | 
  634 | 		private final ConfigurableListableBeanFactory beanFactory;
  635 | 
  636 | 		public ImportAwareAotContribution(ConfigurableListableBeanFactory beanFactory) {
  637 | 			this.beanFactory = beanFactory;
  638 | 		}
  639 | 
  640 | 		@Override
  641 | 		public void applyTo(GenerationContext generationContext,
  642 | 				BeanFactoryInitializationCode beanFactoryInitializationCode) {
  643 | 
  644 | 			Map<String, String> mappings = buildImportAwareMappings();
  645 | 			if (!mappings.isEmpty()) {
  646 | 				GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add(
  647 | 						"addImportAwareBeanPostProcessors", method -> generateAddPostProcessorMethod(method, mappings));
  648 | 				beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
  649 | 				ResourceHints hints = generationContext.getRuntimeHints().resources();
  650 | 				mappings.forEach((target, from) -> hints.registerType(TypeReference.of(from)));
  651 | 			}
  652 | 		}
  653 | 
  654 | 		private void generateAddPostProcessorMethod(MethodSpec.Builder method, Map<String, String> mappings) {
  655 | 			method.addJavadoc("Add ImportAwareBeanPostProcessor to support ImportAware beans.");
  656 | 			method.addModifiers(Modifier.PRIVATE);
  657 | 			method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE);
  658 | 			method.addCode(generateAddPostProcessorCode(mappings));
  659 | 		}
  660 | 
  661 | 		private CodeBlock generateAddPostProcessorCode(Map<String, String> mappings) {
  662 | 			CodeBlock.Builder code = CodeBlock.builder();
  663 | 			code.addStatement("$T $L = new $T<>()", STRING_STRING_MAP,
  664 | 					MAPPINGS_VARIABLE, HashMap.class);
  665 | 			mappings.forEach((type, from) -> code.addStatement("$L.put($S, $S)",
  666 | 					MAPPINGS_VARIABLE, type, from));
  667 | 			code.addStatement("$T $L = new $T($T.class)", RootBeanDefinition.class,
  668 | 					BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, ImportAwareAotBeanPostProcessor.class);
  669 | 			code.addStatement("$L.setRole($T.ROLE_INFRASTRUCTURE)",
  670 | 					BEAN_DEFINITION_VARIABLE, BeanDefinition.class);
  671 | 			code.addStatement("$L.setInstanceSupplier(() -> new $T($L))",
  672 | 					BEAN_DEFINITION_VARIABLE, ImportAwareAotBeanPostProcessor.class, MAPPINGS_VARIABLE);
  673 | 			code.addStatement("$L.registerBeanDefinition($S, $L)",
  674 | 					BEAN_FACTORY_VARIABLE, BEAN_NAME, BEAN_DEFINITION_VARIABLE);
  675 | 			return code.build();
  676 | 		}
  677 | 
  678 | 		private Map<String, String> buildImportAwareMappings() {
  679 | 			ImportRegistry importRegistry = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
  680 | 			Map<String, String> mappings = new LinkedHashMap<>();
  681 | 			for (String name : this.beanFactory.getBeanDefinitionNames()) {
  682 | 				Class<?> beanType = this.beanFactory.getType(name);
  683 | 				if (beanType != null && ImportAware.class.isAssignableFrom(beanType)) {
  684 | 					String target = ClassUtils.getUserClass(beanType).getName();
  685 | 					AnnotationMetadata from = importRegistry.getImportingClassFor(target);
  686 | 					if (from != null) {
  687 | 						mappings.put(target, from.getClassName());
  688 | 					}
  689 | 				}
  690 | 			}
  691 | 			return mappings;
  692 | 		}
  693 | 	}
  694 | 
  695 | 
  696 | 	private static class PropertySourcesAotContribution implements BeanFactoryInitializationAotContribution {
  697 | 
  698 | 		private static final String ENVIRONMENT_VARIABLE = "environment";
  699 | 
  700 | 		private static final String RESOURCE_LOADER_VARIABLE = "resourceLoader";
  701 | 
  702 | 		private final Log logger = LogFactory.getLog(getClass());
  703 | 
  704 | 		private final List<PropertySourceDescriptor> descriptors;
  705 | 
  706 | 		private final Function<String, @Nullable Resource> resourceResolver;
  707 | 
  708 | 		PropertySourcesAotContribution(List<PropertySourceDescriptor> descriptors, Function<String, @Nullable Resource> resourceResolver) {
  709 | 			this.descriptors = descriptors;
  710 | 			this.resourceResolver = resourceResolver;
  711 | 		}
  712 | 
  713 | 		@Override
  714 | 		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
  715 | 			registerRuntimeHints(generationContext.getRuntimeHints());
  716 | 			GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods()
  717 | 					.add("processPropertySources", this::generateAddPropertySourceProcessorMethod);
  718 | 			beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
  719 | 		}
  720 | 
  721 | 		private void registerRuntimeHints(RuntimeHints hints) {
  722 | 			for (PropertySourceDescriptor descriptor : this.descriptors) {
  723 | 				Class<?> factoryClass = descriptor.propertySourceFactory();
  724 | 				if (factoryClass != null) {
  725 | 					hints.reflection().registerType(factoryClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  726 | 				}
  727 | 				for (String location : descriptor.locations()) {
  728 | 					if (location.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) ||
  729 | 							(location.startsWith(ResourcePatternResolver.CLASSPATH_URL_PREFIX) &&
  730 | 									(location.contains("*") || location.contains("?")))) {
  731 | 
  732 | 						if (logger.isWarnEnabled()) {
  733 | 							logger.warn("""
  734 | 									Runtime hint registration is not supported for the 'classpath*:' \
  735 | 									prefix or wildcards in @PropertySource locations. Please manually \
  736 | 									register a resource hint for each property source location represented \
  737 | 									by '%s'.""".formatted(location));
  738 | 						}
  739 | 					}
  740 | 					else {
  741 | 						Resource resource = this.resourceResolver.apply(location);
  742 | 						if (resource instanceof ClassPathResource classPathResource && classPathResource.exists()) {
  743 | 							hints.resources().registerPattern(classPathResource.getPath());
  744 | 						}
  745 | 					}
  746 | 				}
  747 | 			}
  748 | 		}
  749 | 
  750 | 		private void generateAddPropertySourceProcessorMethod(MethodSpec.Builder method) {
  751 | 			method.addJavadoc("Apply known @PropertySources to the environment.");
  752 | 			method.addModifiers(Modifier.PRIVATE);
  753 | 			method.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_VARIABLE);
  754 | 			method.addParameter(ResourceLoader.class, RESOURCE_LOADER_VARIABLE);
  755 | 			method.addCode(generateAddPropertySourceProcessorCode());
  756 | 		}
  757 | 
  758 | 		private CodeBlock generateAddPropertySourceProcessorCode() {
  759 | 			Builder code = CodeBlock.builder();
  760 | 			String processorVariable = "processor";
  761 | 			code.addStatement("$T $L = new $T($L, $L)", PropertySourceProcessor.class,
  762 | 					processorVariable, PropertySourceProcessor.class, ENVIRONMENT_VARIABLE,
  763 | 					RESOURCE_LOADER_VARIABLE);
  764 | 			code.beginControlFlow("try");
  765 | 			for (PropertySourceDescriptor descriptor : this.descriptors) {
  766 | 				code.addStatement("$L.processPropertySource($L)", processorVariable,
  767 | 						generatePropertySourceDescriptorCode(descriptor));
  768 | 			}
  769 | 			code.nextControlFlow("catch ($T ex)", IOException.class);
  770 | 			code.addStatement("throw new $T(ex)", UncheckedIOException.class);
  771 | 			code.endControlFlow();
  772 | 			return code.build();
  773 | 		}
  774 | 
  775 | 		private CodeBlock generatePropertySourceDescriptorCode(PropertySourceDescriptor descriptor) {
  776 | 			CodeBlock.Builder code = CodeBlock.builder();
  777 | 			code.add("new $T(", PropertySourceDescriptor.class);
  778 | 			CodeBlock values = descriptor.locations().stream()
  779 | 					.map(value -> CodeBlock.of("$S", value)).collect(CodeBlock.joining(", "));
  780 | 			if (descriptor.name() == null && descriptor.propertySourceFactory() == null &&
  781 | 					descriptor.encoding() == null && !descriptor.ignoreResourceNotFound()) {
  782 | 				code.add("$L)", values);
  783 | 			}
  784 | 			else {
  785 | 				List<CodeBlock> arguments = new ArrayList<>();
  786 | 				arguments.add(CodeBlock.of("$T.of($L)", List.class, values));
  787 | 				arguments.add(CodeBlock.of("$L", descriptor.ignoreResourceNotFound()));
  788 | 				arguments.add(handleNull(descriptor.name(), () -> CodeBlock.of("$S", descriptor.name())));
  789 | 				arguments.add(handleNull(descriptor.propertySourceFactory(),
  790 | 						() -> CodeBlock.of("$T.class", descriptor.propertySourceFactory())));
  791 | 				arguments.add(handleNull(descriptor.encoding(),
  792 | 						() -> CodeBlock.of("$S", descriptor.encoding())));
  793 | 				code.add(CodeBlock.join(arguments, ", "));
  794 | 				code.add(")");
  795 | 			}
  796 | 			return code.build();
  797 | 		}
  798 | 
  799 | 		private CodeBlock handleNull(@Nullable Object value, Supplier<CodeBlock> nonNull) {
  800 | 			return (value == null ? CodeBlock.of("null") : nonNull.get());
  801 | 		}
  802 | 	}
  803 | 
  804 | 
  805 | 	private static class ConfigurationClassProxyBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {
  806 | 
  807 | 		private final RegisteredBean registeredBean;
  808 | 
  809 | 		private final Class<?> proxyClass;
  810 | 
  811 | 		public ConfigurationClassProxyBeanRegistrationCodeFragments(
  812 | 				BeanRegistrationCodeFragments codeFragments, RegisteredBean registeredBean) {
  813 | 
  814 | 			super(codeFragments);
  815 | 			this.registeredBean = registeredBean;
  816 | 			this.proxyClass = registeredBean.getBeanType().toClass();
  817 | 		}
  818 | 
  819 | 		@Override
  820 | 		public CodeBlock generateSetBeanDefinitionPropertiesCode(
  821 | 				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
  822 | 				RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {
  823 | 
  824 | 			CodeBlock.Builder code = CodeBlock.builder();
  825 | 			code.add(super.generateSetBeanDefinitionPropertiesCode(generationContext,
  826 | 					beanRegistrationCode, beanDefinition, attributeFilter));
  827 | 			code.addStatement("$T.initializeConfigurationClass($T.class)",
  828 | 					ConfigurationClassUtils.class, ClassUtils.getUserClass(this.proxyClass));
  829 | 			return code.build();
  830 | 		}
  831 | 
  832 | 		@Override
  833 | 		public CodeBlock generateInstanceSupplierCode(
  834 | 				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
  835 | 				boolean allowDirectSupplierShortcut) {
  836 | 
  837 | 			InstantiationDescriptor instantiationDescriptor = proxyInstantiationDescriptor(
  838 | 					generationContext.getRuntimeHints(), this.registeredBean.resolveInstantiationDescriptor());
  839 | 
  840 | 			return new InstanceSupplierCodeGenerator(generationContext,
  841 | 					beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
  842 | 					.generateCode(this.registeredBean, instantiationDescriptor);
  843 | 		}
  844 | 
  845 | 		private InstantiationDescriptor proxyInstantiationDescriptor(
  846 | 				RuntimeHints runtimeHints, InstantiationDescriptor instantiationDescriptor) {
  847 | 
  848 | 			Executable userExecutable = instantiationDescriptor.executable();
  849 | 			if (userExecutable instanceof Constructor<?> userConstructor) {
  850 | 				try {
  851 | 					runtimeHints.reflection().registerType(userConstructor.getDeclaringClass());
  852 | 					Constructor<?> constructor = this.proxyClass.getConstructor(userExecutable.getParameterTypes());
  853 | 					return new InstantiationDescriptor(constructor);
  854 | 				}
  855 | 				catch (NoSuchMethodException ex) {
  856 | 					throw new IllegalStateException("No matching constructor found on proxy " + this.proxyClass, ex);
  857 | 				}
  858 | 			}
  859 | 			return instantiationDescriptor;
  860 | 		}
  861 | 	}
  862 | 
  863 | 
  864 | 	private static class BeanRegistrarAotContribution implements BeanFactoryInitializationAotContribution {
  865 | 
  866 | 		private static final String CUSTOMIZER_MAP_VARIABLE = "customizers";
  867 | 
  868 | 		private static final String ENVIRONMENT_VARIABLE = "environment";
  869 | 
  870 | 		private final MultiValueMap<String, BeanRegistrar> beanRegistrars;
  871 | 
  872 | 		private final ConfigurableListableBeanFactory beanFactory;
  873 | 
  874 | 		private final AotServices<BeanRegistrationAotProcessor> aotProcessors;
  875 | 
  876 | 		public BeanRegistrarAotContribution(MultiValueMap<String, BeanRegistrar> beanRegistrars, ConfigurableListableBeanFactory beanFactory) {
  877 | 			this.beanRegistrars = beanRegistrars;
  878 | 			this.beanFactory = beanFactory;
  879 | 			this.aotProcessors = AotServices.factoriesAndBeans(this.beanFactory).load(BeanRegistrationAotProcessor.class);
  880 | 		}
  881 | 
  882 | 		@Override
  883 | 		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
  884 | 			GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add(
  885 | 					"applyBeanRegistrars", builder -> this.generateApplyBeanRegistrarsMethod(builder,
  886 | 							generationContext, beanFactoryInitializationCode.getClassName()));
  887 | 			beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
  888 | 		}
  889 | 
  890 | 		private void generateApplyBeanRegistrarsMethod(MethodSpec.Builder method, GenerationContext generationContext,
  891 | 				ClassName className) {
  892 | 
  893 | 			ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
  894 | 			method.addJavadoc("Apply bean registrars.");
  895 | 			method.addModifiers(Modifier.PRIVATE);
  896 | 			method.addParameter(ListableBeanFactory.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE);
  897 | 			method.addParameter(Environment.class, ENVIRONMENT_VARIABLE);
  898 | 			method.addCode(generateCustomizerMap());
  899 | 
  900 | 			for (String name : this.beanFactory.getBeanDefinitionNames()) {
  901 | 				BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition(name);
  902 | 				if (beanDefinition.getSource() instanceof Class<?> sourceClass &&
  903 | 						BeanRegistrar.class.isAssignableFrom(sourceClass)) {
  904 | 
  905 | 					for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
  906 | 						BeanRegistrationAotContribution contribution =
  907 | 								aotProcessor.processAheadOfTime(RegisteredBean.of(this.beanFactory, name));
  908 | 						if (contribution != null) {
  909 | 							contribution.applyTo(generationContext,
  910 | 									new UnsupportedBeanRegistrationCode(name, aotProcessor.getClass()));
  911 | 						}
  912 | 					}
  913 | 					if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
  914 | 						if (rootBeanDefinition.getPreferredConstructors() != null) {
  915 | 							for (Constructor<?> constructor : rootBeanDefinition.getPreferredConstructors()) {
  916 | 								reflectionHints.registerConstructor(constructor, ExecutableMode.INVOKE);
  917 | 							}
  918 | 						}
  919 | 						if (!ObjectUtils.isEmpty(rootBeanDefinition.getInitMethodNames())) {
  920 | 							method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
  921 | 									rootBeanDefinition.getInitMethodNames(), "setInitMethodNames", reflectionHints));
  922 | 						}
  923 | 						if (!ObjectUtils.isEmpty(rootBeanDefinition.getDestroyMethodNames())) {
  924 | 							method.addCode(generateInitDestroyMethods(name, rootBeanDefinition,
  925 | 									rootBeanDefinition.getDestroyMethodNames(), "setDestroyMethodNames", reflectionHints));
  926 | 						}
  927 | 						checkUnsupportedFeatures(rootBeanDefinition);
  928 | 					}
  929 | 				}
  930 | 			}
  931 | 			method.addCode(generateRegisterCode(className, generationContext));
  932 | 		}
  933 | 
  934 | 		private void checkUnsupportedFeatures(AbstractBeanDefinition beanDefinition) {
  935 | 			if (!ObjectUtils.isEmpty(beanDefinition.getFactoryBeanName())) {
  936 | 				throw new UnsupportedOperationException("AOT post processing of the factory bean name is not supported yet with BeanRegistrar");
  937 | 			}
  938 | 			if (beanDefinition.hasConstructorArgumentValues()) {
  939 | 				throw new UnsupportedOperationException("AOT post processing of argument values is not supported yet with BeanRegistrar");
  940 | 			}
  941 | 			if (!beanDefinition.getQualifiers().isEmpty()) {
  942 | 				throw new UnsupportedOperationException("AOT post processing of qualifiers is not supported yet with BeanRegistrar");
  943 | 			}
  944 | 		}
  945 | 
  946 | 		private CodeBlock generateCustomizerMap() {
  947 | 			Builder code = CodeBlock.builder();
  948 | 			code.addStatement("$T<$T, $T> $L = new $T<>()", MultiValueMap.class, String.class, BeanDefinitionCustomizer.class,
  949 | 					CUSTOMIZER_MAP_VARIABLE, LinkedMultiValueMap.class);
  950 | 			return code.build();
  951 | 		}
  952 | 
  953 | 		private CodeBlock generateRegisterCode(ClassName className, GenerationContext generationContext) {
  954 | 			Builder code = CodeBlock.builder();
  955 | 			Builder metadataReaderFactoryCode = null;
  956 | 			NameAllocator nameAllocator = new NameAllocator();
  957 | 			for (Map.Entry<String, List<BeanRegistrar>> beanRegistrarEntry : this.beanRegistrars.entrySet()) {
  958 | 				for (BeanRegistrar beanRegistrar : beanRegistrarEntry.getValue()) {
  959 | 					String beanRegistrarName = nameAllocator.newName(StringUtils.uncapitalize(beanRegistrar.getClass().getSimpleName()));
  960 | 					Constructor<?> constructor = BeanUtils.getResolvableConstructor(beanRegistrar.getClass());
  961 | 					boolean visible = isVisible(constructor, className);
  962 | 					if (visible) {
  963 | 						code.addStatement("$T $L = new $T()", beanRegistrar.getClass(), beanRegistrarName, beanRegistrar.getClass());
  964 | 					}
  965 | 					else {
  966 | 						try {
  967 | 							Class<?> configClass = ClassUtils.forName(beanRegistrarEntry.getKey(), beanRegistrar.getClass().getClassLoader());
  968 | 							GeneratedClass generatedClass = generationContext.getGeneratedClasses()
  969 | 									.getOrAddForFeatureComponent("BeanRegistrars", configClass, type ->
  970 | 											type.addJavadoc("Bean registrars for {@link $T}.", configClass)
  971 | 													.addModifiers(Modifier.PUBLIC));
  972 | 							GeneratedMethod generatedMethod = generatedClass.getMethods().add(
  973 | 									"get" + beanRegistrar.getClass().getSimpleName(),
  974 | 									method -> method
  975 | 											.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
  976 | 											.returns(BeanRegistrar.class)
  977 | 											.addStatement("return new $T()", beanRegistrar.getClass()));
  978 | 							code.addStatement("$T $L = $L", BeanRegistrar.class, beanRegistrarName,
  979 | 									generatedMethod.toMethodReference().toInvokeCodeBlock(ArgumentCodeGenerator.none()));
  980 | 						}
  981 | 						catch (ClassNotFoundException ex) {
  982 | 							throw new IllegalStateException(ex);
  983 | 						}
  984 | 					}
  985 | 					if (beanRegistrar instanceof ImportAware) {
  986 | 						if (metadataReaderFactoryCode == null) {
  987 | 							metadataReaderFactoryCode = CodeBlock.builder();
  988 | 							metadataReaderFactoryCode.addStatement("$T metadataReaderFactory = new $T()",
  989 | 									MetadataReaderFactory.class, CachingMetadataReaderFactory.class);
  990 | 						}
  991 | 						CodeBlock setImportMetadataCode;
  992 | 						if (visible) {
  993 | 							setImportMetadataCode = CodeBlock.builder()
  994 | 									.addStatement("$L.setImportMetadata(metadataReaderFactory.getMetadataReader($S).getAnnotationMetadata())",
  995 | 									beanRegistrarName, beanRegistrarEntry.getKey()).build();
  996 | 						}
  997 | 						else {
  998 | 							setImportMetadataCode = CodeBlock.builder()
  999 | 									.addStatement("(($T)$L).setImportMetadata(metadataReaderFactory.getMetadataReader($S).getAnnotationMetadata())",
 1000 | 									ImportAware.class, beanRegistrarName, beanRegistrarEntry.getKey()).build();
 1001 | 						}
 1002 | 						code.beginControlFlow("try")
 1003 | 								.add(setImportMetadataCode)
 1004 | 								.nextControlFlow("catch ($T ex)", IOException.class)
 1005 | 								.addStatement("throw new $T(\"Failed to read metadata for '$L'\", ex)",
 1006 | 										IllegalStateException.class, beanRegistrarEntry.getKey())
 1007 | 								.endControlFlow();
 1008 | 						generationContext.getRuntimeHints().resources().registerType(TypeReference.of(beanRegistrarEntry.getKey()));
 1009 | 					}
 1010 | 					code.addStatement("$L.register(new $T(($T)$L, $L, $L, $L.getClass(), $L), $L)", beanRegistrarName,
 1011 | 							BeanRegistryAdapter.class, BeanDefinitionRegistry.class, BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE,
 1012 | 							BeanFactoryInitializationCode.BEAN_FACTORY_VARIABLE, ENVIRONMENT_VARIABLE, beanRegistrarName,
 1013 | 							CUSTOMIZER_MAP_VARIABLE, ENVIRONMENT_VARIABLE);
 1014 | 				}
 1015 | 			}
 1016 | 			return (metadataReaderFactoryCode == null ? code.build() : metadataReaderFactoryCode.add(code.build()).build());
 1017 | 		}
 1018 | 
 1019 | 		private boolean isVisible(Constructor<?> ctor, ClassName className) {
 1020 | 			AccessControl classAccessControl = AccessControl.forClass(ctor.getDeclaringClass());
 1021 | 			AccessControl memberAccessControl = AccessControl.forMember(ctor);
 1022 | 			AccessControl.Visibility visibility = AccessControl.lowest(classAccessControl, memberAccessControl).getVisibility();
 1023 | 			return (visibility == AccessControl.Visibility.PUBLIC || (visibility != AccessControl.Visibility.PRIVATE &&
 1024 | 					ctor.getDeclaringClass().getPackageName().equals(className.packageName())));
 1025 | 		}
 1026 | 
 1027 | 		private CodeBlock generateInitDestroyMethods(String beanName, AbstractBeanDefinition beanDefinition,
 1028 | 				String[] methodNames, String method, ReflectionHints reflectionHints) {
 1029 | 
 1030 | 			Builder code = CodeBlock.builder();
 1031 | 			// For Publisher-based destroy methods
 1032 | 			reflectionHints.registerType(TypeReference.of("org.reactivestreams.Publisher"));
 1033 | 			Class<?> beanType = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
 1034 | 			Arrays.stream(methodNames).forEach(methodName -> addInitDestroyHint(beanType, methodName, reflectionHints));
 1035 | 			CodeBlock arguments = Arrays.stream(methodNames)
 1036 | 					.map(name -> CodeBlock.of("$S", name))
 1037 | 					.collect(CodeBlock.joining(", "));
 1038 | 
 1039 | 			code.addStatement("$L.add($S, $L -> (($T)$L).$L($L))", CUSTOMIZER_MAP_VARIABLE, beanName, "bd",
 1040 | 					AbstractBeanDefinition.class, "bd", method, arguments);
 1041 | 			return code.build();
 1042 | 		}
 1043 | 
 1044 | 		// Inspired from BeanDefinitionPropertiesCodeGenerator#addInitDestroyHint
 1045 | 		private static void addInitDestroyHint(Class<?> beanUserClass, String methodName, ReflectionHints reflectionHints) {
 1046 | 			Class<?> methodDeclaringClass = beanUserClass;
 1047 | 
 1048 | 			// Parse fully-qualified method name if necessary.
 1049 | 			int indexOfDot = methodName.lastIndexOf('.');
 1050 | 			if (indexOfDot > 0) {
 1051 | 				String className = methodName.substring(0, indexOfDot);
 1052 | 				methodName = methodName.substring(indexOfDot + 1);
 1053 | 				if (!beanUserClass.getName().equals(className)) {
 1054 | 					try {
 1055 | 						methodDeclaringClass = ClassUtils.forName(className, beanUserClass.getClassLoader());
 1056 | 					}
 1057 | 					catch (Throwable ex) {
 1058 | 						throw new IllegalStateException("Failed to load Class [" + className +
 1059 | 								"] from ClassLoader [" + beanUserClass.getClassLoader() + "]", ex);
 1060 | 					}
 1061 | 				}
 1062 | 			}
 1063 | 
 1064 | 			Method method = ReflectionUtils.findMethod(methodDeclaringClass, methodName);
 1065 | 			if (method != null) {
 1066 | 				reflectionHints.registerMethod(method, ExecutableMode.INVOKE);
 1067 | 				Method publiclyAccessibleMethod = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, beanUserClass);
 1068 | 				if (!publiclyAccessibleMethod.equals(method)) {
 1069 | 					reflectionHints.registerMethod(publiclyAccessibleMethod, ExecutableMode.INVOKE);
 1070 | 				}
 1071 | 			}
 1072 | 		}
 1073 | 
 1074 | 
 1075 | 		static class UnsupportedBeanRegistrationCode implements BeanRegistrationCode {
 1076 | 
 1077 | 			private final String message;
 1078 | 
 1079 | 			public UnsupportedBeanRegistrationCode(String beanName, Class<?> aotProcessorClass) {
 1080 | 				this.message = "Code generation attempted for bean " + beanName + " by the AOT Processor " +
 1081 | 						aotProcessorClass + " is not supported with BeanRegistrar yet";
 1082 | 			}
 1083 | 
 1084 | 			@Override
 1085 | 			public ClassName getClassName() {
 1086 | 				throw new UnsupportedOperationException(this.message);
 1087 | 			}
 1088 | 
 1089 | 			@Override
 1090 | 			public GeneratedMethods getMethods() {
 1091 | 				throw new UnsupportedOperationException(this.message);
 1092 | 			}
 1093 | 
 1094 | 			@Override
 1095 | 			public void addInstancePostProcessor(MethodReference methodReference) {
 1096 | 				throw new UnsupportedOperationException(this.message);
 1097 | 			}
 1098 | 		}
 1099 | 	}
 1100 | 
 1101 | }
```
