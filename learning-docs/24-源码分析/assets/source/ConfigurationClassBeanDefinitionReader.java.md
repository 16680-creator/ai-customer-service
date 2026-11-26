<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClassBeanDefinitionReader.java -->
<!-- 正文引用：[[源码:ConfigurationClassBeanDefinitionReader.java#L起-L止]] ；代码块头部：(src: ConfigurationClassBeanDefinitionReader.java#L起-L止) -->

# ConfigurationClassBeanDefinitionReader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClassBeanDefinitionReader.java` |
| 行数 | 539 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.107Z |

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
   19 | import java.lang.reflect.Constructor;
   20 | import java.lang.reflect.Method;
   21 | import java.util.HashMap;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.beans.BeanUtils;
   30 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   31 | import org.springframework.beans.factory.BeanRegistrar;
   32 | import org.springframework.beans.factory.ListableBeanFactory;
   33 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   34 | import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
   35 | import org.springframework.beans.factory.config.BeanDefinition;
   36 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   37 | import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
   38 | import org.springframework.beans.factory.parsing.SourceExtractor;
   39 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   40 | import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
   41 | import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
   42 | import org.springframework.beans.factory.support.BeanDefinitionReader;
   43 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   44 | import org.springframework.beans.factory.support.BeanNameGenerator;
   45 | import org.springframework.beans.factory.support.BeanRegistryAdapter;
   46 | import org.springframework.beans.factory.support.RootBeanDefinition;
   47 | import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
   48 | import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
   49 | import org.springframework.core.annotation.AnnotationAttributes;
   50 | import org.springframework.core.env.Environment;
   51 | import org.springframework.core.io.Resource;
   52 | import org.springframework.core.io.ResourceLoader;
   53 | import org.springframework.core.type.AnnotationMetadata;
   54 | import org.springframework.core.type.MethodMetadata;
   55 | import org.springframework.core.type.StandardAnnotationMetadata;
   56 | import org.springframework.core.type.StandardMethodMetadata;
   57 | import org.springframework.util.Assert;
   58 | import org.springframework.util.ClassUtils;
   59 | import org.springframework.util.MultiValueMap;
   60 | import org.springframework.util.StringUtils;
   61 | 
   62 | /**
   63 |  * Reads a given fully-populated set of ConfigurationClass instances, registering bean
   64 |  * definitions with the given {@link BeanDefinitionRegistry} based on its contents.
   65 |  *
   66 |  * <p>This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does
   67 |  * not implement/extend any of its artifacts as a set of configuration classes is not a
   68 |  * {@link Resource}.
   69 |  *
   70 |  * @author Chris Beams
   71 |  * @author Juergen Hoeller
   72 |  * @author Phillip Webb
   73 |  * @author Sam Brannen
   74 |  * @author Sebastien Deleuze
   75 |  * @since 3.0
   76 |  * @see ConfigurationClassParser
   77 |  */
   78 | class ConfigurationClassBeanDefinitionReader {
   79 | 
   80 | 	private static final Log logger = LogFactory.getLog(ConfigurationClassBeanDefinitionReader.class);
   81 | 
   82 | 	private static final ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
   83 | 
   84 | 	private final BeanDefinitionRegistry registry;
   85 | 
   86 | 	private final SourceExtractor sourceExtractor;
   87 | 
   88 | 	private final ResourceLoader resourceLoader;
   89 | 
   90 | 	private final Environment environment;
   91 | 
   92 | 	private final BeanNameGenerator importBeanNameGenerator;
   93 | 
   94 | 	private final ImportRegistry importRegistry;
   95 | 
   96 | 	private final ConditionEvaluator conditionEvaluator;
   97 | 
   98 | 
   99 | 	/**
  100 | 	 * Create a new {@link ConfigurationClassBeanDefinitionReader} instance
  101 | 	 * that will be used to populate the given {@link BeanDefinitionRegistry}.
  102 | 	 */
  103 | 	ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry, SourceExtractor sourceExtractor,
  104 | 			ResourceLoader resourceLoader, Environment environment, BeanNameGenerator importBeanNameGenerator,
  105 | 			ImportRegistry importRegistry) {
  106 | 
  107 | 		this.registry = registry;
  108 | 		this.sourceExtractor = sourceExtractor;
  109 | 		this.resourceLoader = resourceLoader;
  110 | 		this.environment = environment;
  111 | 		this.importBeanNameGenerator = importBeanNameGenerator;
  112 | 		this.importRegistry = importRegistry;
  113 | 		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
  114 | 	}
  115 | 
  116 | 
  117 | 	/**
  118 | 	 * Read {@code configurationModel}, registering bean definitions
  119 | 	 * with the registry based on its contents.
  120 | 	 */
  121 | 	public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
  122 | 		TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
  123 | 		for (ConfigurationClass configClass : configurationModel) {
  124 | 			loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
  125 | 		}
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Read a particular {@link ConfigurationClass}, registering bean definitions
  130 | 	 * for the class itself and all of its {@link Bean} methods.
  131 | 	 */
  132 | 	private void loadBeanDefinitionsForConfigurationClass(
  133 | 			ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
  134 | 
  135 | 		if (trackedConditionEvaluator.shouldSkip(configClass)) {
  136 | 			String beanName = configClass.getBeanName();
  137 | 			if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
  138 | 				this.registry.removeBeanDefinition(beanName);
  139 | 			}
  140 | 			this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
  141 | 			return;
  142 | 		}
  143 | 
  144 | 		if (configClass.isImported()) {
  145 | 			registerBeanDefinitionForImportedConfigurationClass(configClass);
  146 | 		}
  147 | 		for (BeanMethod beanMethod : configClass.getBeanMethods()) {
  148 | 			loadBeanDefinitionsForBeanMethod(beanMethod);
  149 | 		}
  150 | 
  151 | 		loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
  152 | 		loadBeanDefinitionsFromImportBeanDefinitionRegistrars(configClass.getImportBeanDefinitionRegistrars());
  153 | 		loadBeanDefinitionsFromBeanRegistrars(configClass.getBeanRegistrars());
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Register the {@link Configuration} class itself as a bean definition.
  158 | 	 */
  159 | 	private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
  160 | 		AnnotationMetadata metadata = configClass.getMetadata();
  161 | 		AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);
  162 | 
  163 | 		ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(configBeanDef);
  164 | 		configBeanDef.setScope(scopeMetadata.getScopeName());
  165 | 
  166 | 		String configBeanName;
  167 | 		try {
  168 | 			configBeanName = this.importBeanNameGenerator.generateBeanName(configBeanDef, this.registry);
  169 | 		}
  170 | 		catch (IllegalArgumentException ex) {
  171 | 			throw new IllegalStateException("Failed to generate bean name for imported class '" +
  172 | 					configClass.getMetadata().getClassName() + "'", ex);
  173 | 		}
  174 | 
  175 | 		AnnotationConfigUtils.processCommonDefinitionAnnotations(configBeanDef, metadata);
  176 | 		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(configBeanDef, configBeanName);
  177 | 		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
  178 | 		this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
  179 | 		configClass.setBeanName(configBeanName);
  180 | 
  181 | 		if (logger.isTraceEnabled()) {
  182 | 			logger.trace("Registered bean definition for imported class '" + configBeanName + "'");
  183 | 		}
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Read the given {@link BeanMethod}, registering bean definitions
  188 | 	 * with the BeanDefinitionRegistry based on its contents.
  189 | 	 */
  190 | 	private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
  191 | 		ConfigurationClass configClass = beanMethod.getConfigurationClass();
  192 | 		MethodMetadata metadata = beanMethod.getMetadata();
  193 | 		String methodName = metadata.getMethodName();
  194 | 
  195 | 		// Do we need to mark the bean as skipped by its condition?
  196 | 		if (this.conditionEvaluator.shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN)) {
  197 | 			configClass.skippedBeanMethods.add(methodName);
  198 | 			return;
  199 | 		}
  200 | 		if (configClass.skippedBeanMethods.contains(methodName)) {
  201 | 			return;
  202 | 		}
  203 | 
  204 | 		AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
  205 | 		Assert.state(bean != null, "No @Bean annotation attributes");
  206 | 
  207 | 		// Consider name and any aliases.
  208 | 		String[] explicitNames = bean.getStringArray("name");
  209 | 		String beanName = (explicitNames.length > 0 && StringUtils.hasText(explicitNames[0])) ? explicitNames[0] : null;
  210 | 		String localBeanName = defaultBeanName(beanName, methodName);
  211 | 		beanName = (this.importBeanNameGenerator instanceof ConfigurationBeanNameGenerator cbng ?
  212 | 				cbng.deriveBeanName(metadata, beanName) : localBeanName);
  213 | 		if (!localBeanName.equals(beanName) && !this.registry.containsBeanDefinition(localBeanName) &&
  214 | 				!this.registry.isAlias(localBeanName)) {
  215 | 			// Register original name as alias unless registered already.
  216 | 			this.registry.registerAlias(beanName, localBeanName);
  217 | 		}
  218 | 		if (explicitNames.length > 0) {
  219 | 			// Register aliases even when overridden below.
  220 | 			for (int i = 1; i < explicitNames.length; i++) {
  221 | 				this.registry.registerAlias(beanName, explicitNames[i]);
  222 | 			}
  223 | 		}
  224 | 
  225 | 		ConfigurationClassBeanDefinition beanDef =
  226 | 				new ConfigurationClassBeanDefinition(configClass, metadata, localBeanName);
  227 | 		beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));
  228 | 
  229 | 		// Has this effectively been overridden before (for example, via XML)?
  230 | 		if (isOverriddenByExistingDefinition(beanMethod, beanName, beanDef)) {
  231 | 			if (beanName.equals(beanMethod.getConfigurationClass().getBeanName())) {
  232 | 				throw new BeanDefinitionStoreException(beanMethod.getConfigurationClass().getResource().getDescription(),
  233 | 						beanName, "Bean name derived from @Bean method '" + beanMethod.getMetadata().getMethodName() +
  234 | 						"' clashes with bean name for containing configuration class; please make those names unique!");
  235 | 			}
  236 | 			return;
  237 | 		}
  238 | 
  239 | 		if (metadata.isStatic()) {
  240 | 			// static @Bean method
  241 | 			if (configClass.getMetadata() instanceof StandardAnnotationMetadata sam) {
  242 | 				beanDef.setBeanClass(sam.getIntrospectedClass());
  243 | 			}
  244 | 			else {
  245 | 				beanDef.setBeanClassName(configClass.getMetadata().getClassName());
  246 | 			}
  247 | 			beanDef.setUniqueFactoryMethodName(methodName);
  248 | 		}
  249 | 		else {
  250 | 			// instance @Bean method
  251 | 			beanDef.setFactoryBeanName(configClass.getBeanName());
  252 | 			beanDef.setUniqueFactoryMethodName(methodName);
  253 | 		}
  254 | 
  255 | 		if (metadata instanceof StandardMethodMetadata smm &&
  256 | 				configClass.getMetadata() instanceof StandardAnnotationMetadata sam) {
  257 | 			Method method = ClassUtils.getMostSpecificMethod(smm.getIntrospectedMethod(), sam.getIntrospectedClass());
  258 | 			if (method == smm.getIntrospectedMethod()) {
  259 | 				beanDef.setResolvedFactoryMethod(method);
  260 | 			}
  261 | 		}
  262 | 
  263 | 		beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
  264 | 		AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef, metadata);
  265 | 
  266 | 		boolean autowireCandidate = bean.getBoolean("autowireCandidate");
  267 | 		if (!autowireCandidate) {
  268 | 			beanDef.setAutowireCandidate(false);
  269 | 		}
  270 | 
  271 | 		boolean defaultCandidate = bean.getBoolean("defaultCandidate");
  272 | 		if (!defaultCandidate) {
  273 | 			beanDef.setDefaultCandidate(false);
  274 | 		}
  275 | 
  276 | 		Bean.Bootstrap instantiation = bean.getEnum("bootstrap");
  277 | 		if (instantiation == Bean.Bootstrap.BACKGROUND) {
  278 | 			beanDef.setBackgroundInit(true);
  279 | 		}
  280 | 
  281 | 		String initMethodName = bean.getString("initMethod");
  282 | 		if (StringUtils.hasText(initMethodName)) {
  283 | 			beanDef.setInitMethodName(initMethodName);
  284 | 		}
  285 | 
  286 | 		String destroyMethodName = bean.getString("destroyMethod");
  287 | 		beanDef.setDestroyMethodName(destroyMethodName);
  288 | 
  289 | 		// Consider scoping
  290 | 		ScopedProxyMode proxyMode = ScopedProxyMode.NO;
  291 | 		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(metadata, Scope.class);
  292 | 		if (attributes != null) {
  293 | 			beanDef.setScope(attributes.getString("value"));
  294 | 			proxyMode = attributes.getEnum("proxyMode");
  295 | 			if (proxyMode == ScopedProxyMode.DEFAULT) {
  296 | 				proxyMode = ScopedProxyMode.NO;
  297 | 			}
  298 | 		}
  299 | 
  300 | 		// Replace the original bean definition with the target one, if necessary
  301 | 		BeanDefinition beanDefToRegister = beanDef;
  302 | 		if (proxyMode != ScopedProxyMode.NO) {
  303 | 			BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
  304 | 					new BeanDefinitionHolder(beanDef, beanName), this.registry,
  305 | 					proxyMode == ScopedProxyMode.TARGET_CLASS);
  306 | 			beanDefToRegister = new ConfigurationClassBeanDefinition(
  307 | 					(RootBeanDefinition) proxyDef.getBeanDefinition(), configClass, metadata, localBeanName);
  308 | 		}
  309 | 
  310 | 		if (logger.isTraceEnabled()) {
  311 | 			logger.trace("Registering bean definition for @Bean method %s.%s() with bean name '%s'"
  312 | 					.formatted(configClass.getMetadata().getClassName(), methodName, beanName));
  313 | 		}
  314 | 		this.registry.registerBeanDefinition(beanName, beanDefToRegister);
  315 | 	}
  316 | 
  317 | 	private static String defaultBeanName(@Nullable String beanName, String methodName) {
  318 | 		return (beanName != null ? beanName : methodName);
  319 | 	}
  320 | 
  321 | 	@SuppressWarnings("NullAway") // Reflection
  322 | 	private boolean isOverriddenByExistingDefinition(
  323 | 			BeanMethod beanMethod, String beanName, ConfigurationClassBeanDefinition newBeanDef) {
  324 | 
  325 | 		if (!this.registry.containsBeanDefinition(beanName)) {
  326 | 			return false;
  327 | 		}
  328 | 		BeanDefinition existingBeanDef = this.registry.getBeanDefinition(beanName);
  329 | 		ConfigurationClass configClass = beanMethod.getConfigurationClass();
  330 | 
  331 | 		// If the bean method is an overloaded case on the same configuration class,
  332 | 		// preserve the existing bean definition and mark it as overloaded.
  333 | 		if (existingBeanDef instanceof ConfigurationClassBeanDefinition ccbd) {
  334 | 			if (!ccbd.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
  335 | 				return false;
  336 | 			}
  337 | 			if (ccbd.getFactoryMethodMetadata().getMethodName().equals(beanMethod.getMetadata().getMethodName())) {
  338 | 				ccbd.setNonUniqueFactoryMethodName(ccbd.getFactoryMethodMetadata().getMethodName());
  339 | 				return true;
  340 | 			}
  341 | 			Map<String, @Nullable Object> attributes =
  342 | 					configClass.getMetadata().getAnnotationAttributes(Configuration.class.getName());
  343 | 			if ((attributes != null && (Boolean) attributes.get("enforceUniqueMethods")) ||
  344 | 					!this.registry.isBeanDefinitionOverridable(beanName)) {
  345 | 				throw new BeanDefinitionOverrideException(beanName, newBeanDef, existingBeanDef,
  346 | 						"@Bean method override with same bean name but different method name: " + existingBeanDef);
  347 | 			}
  348 | 			return true;
  349 | 		}
  350 | 
  351 | 		// A bean definition resulting from a component scan can be silently overridden
  352 | 		// by an @Bean method - and as of 6.1, even when general overriding is disabled
  353 | 		// as long as the bean class is the same.
  354 | 		if (existingBeanDef instanceof ScannedGenericBeanDefinition scannedBeanDef) {
  355 | 			if (beanMethod.getMetadata().getReturnTypeName().equals(scannedBeanDef.getBeanClassName())) {
  356 | 				this.registry.removeBeanDefinition(beanName);
  357 | 			}
  358 | 			return false;
  359 | 		}
  360 | 
  361 | 		// Has the existing bean definition bean marked as a framework-generated bean?
  362 | 		// -> allow the current bean method to override it, since it is application-level
  363 | 		if (existingBeanDef.getRole() > BeanDefinition.ROLE_APPLICATION) {
  364 | 			return false;
  365 | 		}
  366 | 
  367 | 		// At this point, it's a top-level override (probably XML), just having been parsed
  368 | 		// before configuration class processing kicks in...
  369 | 		if (!this.registry.isBeanDefinitionOverridable(beanName)) {
  370 | 			throw new BeanDefinitionOverrideException(beanName,
  371 | 					new ConfigurationClassBeanDefinition(configClass, beanMethod.getMetadata(), beanName),
  372 | 					existingBeanDef,
  373 | 					"@Bean definition illegally overridden by existing bean definition: " + existingBeanDef);
  374 | 		}
  375 | 		if (logger.isDebugEnabled()) {
  376 | 			logger.debug(("Skipping bean definition for %s: a definition for bean '%s' already exists. " +
  377 | 					"This top-level bean definition is considered as an override.").formatted(beanMethod, beanName));
  378 | 		}
  379 | 		return true;
  380 | 	}
  381 | 
  382 | 	private void loadBeanDefinitionsFromImportedResources(
  383 | 			Map<String, Class<? extends BeanDefinitionReader>> importedResources) {
  384 | 
  385 | 		Map<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<>();
  386 | 
  387 | 		importedResources.forEach((resource, readerClass) -> {
  388 | 			// Default reader selection necessary?
  389 | 			if (BeanDefinitionReader.class == readerClass) {
  390 | 				if (StringUtils.endsWithIgnoreCase(resource, ".groovy")) {
  391 | 					// When clearly asking for Groovy, that's what they'll get...
  392 | 					readerClass = GroovyBeanDefinitionReader.class;
  393 | 				}
  394 | 				else {
  395 | 					// Primarily ".xml" files but for any other extension as well
  396 | 					readerClass = XmlBeanDefinitionReader.class;
  397 | 				}
  398 | 			}
  399 | 
  400 | 			BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
  401 | 			if (reader == null) {
  402 | 				try {
  403 | 					Constructor<? extends BeanDefinitionReader> constructor =
  404 | 							readerClass.getDeclaredConstructor(BeanDefinitionRegistry.class);
  405 | 					// Instantiate the specified BeanDefinitionReader
  406 | 					reader = BeanUtils.instantiateClass(constructor, this.registry);
  407 | 					// Delegate the current ResourceLoader and Environment to it if possible
  408 | 					if (reader instanceof AbstractBeanDefinitionReader abdr) {
  409 | 						abdr.setResourceLoader(this.resourceLoader);
  410 | 						abdr.setEnvironment(this.environment);
  411 | 					}
  412 | 					readerInstanceCache.put(readerClass, reader);
  413 | 				}
  414 | 				catch (Throwable ex) {
  415 | 					throw new IllegalStateException(
  416 | 							"Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]", ex);
  417 | 				}
  418 | 			}
  419 | 			reader.loadBeanDefinitions(resource);
  420 | 		});
  421 | 	}
  422 | 
  423 | 	private void loadBeanDefinitionsFromImportBeanDefinitionRegistrars(
  424 | 			Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
  425 | 
  426 | 		registrars.forEach((registrar, metadata) ->
  427 | 				registrar.registerBeanDefinitions(metadata, this.registry, this.importBeanNameGenerator));
  428 | 	}
  429 | 
  430 | 	private void loadBeanDefinitionsFromBeanRegistrars(MultiValueMap<String, BeanRegistrar> registrars) {
  431 | 		registrars.values().forEach(registrarList -> registrarList.forEach(registrar -> {
  432 | 			if (!(this.registry instanceof ListableBeanFactory beanFactory)) {
  433 | 				throw new IllegalStateException("Cannot support bean registrars since " +
  434 | 						this.registry.getClass().getName() + " does not implement ListableBeanFactory");
  435 | 			}
  436 | 			registrar.register(new BeanRegistryAdapter(
  437 | 					this.registry, beanFactory, this.environment, registrar.getClass()), this.environment);
  438 | 		}));
  439 | 	}
  440 | 
  441 | 
  442 | 	/**
  443 | 	 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition
  444 | 	 * was created from a configuration class as opposed to any other configuration source.
  445 | 	 * Used in bean overriding cases where it's necessary to determine whether the bean
  446 | 	 * definition was created externally.
  447 | 	 */
  448 | 	@SuppressWarnings("serial")
  449 | 	private static class ConfigurationClassBeanDefinition extends RootBeanDefinition implements AnnotatedBeanDefinition {
  450 | 
  451 | 		private final AnnotationMetadata annotationMetadata;
  452 | 
  453 | 		private final MethodMetadata factoryMethodMetadata;
  454 | 
  455 | 		private final String localBeanName;
  456 | 
  457 | 		public ConfigurationClassBeanDefinition(
  458 | 				ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String localBeanName) {
  459 | 
  460 | 			this.annotationMetadata = configClass.getMetadata();
  461 | 			this.factoryMethodMetadata = beanMethodMetadata;
  462 | 			this.localBeanName = localBeanName;
  463 | 			setResource(configClass.getResource());
  464 | 			setLenientConstructorResolution(false);
  465 | 		}
  466 | 
  467 | 		public ConfigurationClassBeanDefinition(RootBeanDefinition original,
  468 | 				ConfigurationClass configClass, MethodMetadata beanMethodMetadata, String localBeanName) {
  469 | 
  470 | 			super(original);
  471 | 			this.annotationMetadata = configClass.getMetadata();
  472 | 			this.factoryMethodMetadata = beanMethodMetadata;
  473 | 			this.localBeanName = localBeanName;
  474 | 		}
  475 | 
  476 | 		private ConfigurationClassBeanDefinition(ConfigurationClassBeanDefinition original) {
  477 | 			super(original);
  478 | 			this.annotationMetadata = original.annotationMetadata;
  479 | 			this.factoryMethodMetadata = original.factoryMethodMetadata;
  480 | 			this.localBeanName = original.localBeanName;
  481 | 		}
  482 | 
  483 | 		@Override
  484 | 		public AnnotationMetadata getMetadata() {
  485 | 			return this.annotationMetadata;
  486 | 		}
  487 | 
  488 | 		@Override
  489 | 		public MethodMetadata getFactoryMethodMetadata() {
  490 | 			return this.factoryMethodMetadata;
  491 | 		}
  492 | 
  493 | 		@Override
  494 | 		public boolean isFactoryMethod(Method candidate) {
  495 | 			return (super.isFactoryMethod(candidate) && BeanAnnotationHelper.isBeanAnnotated(candidate) &&
  496 | 					BeanAnnotationHelper.determineBeanNameFor(candidate).equals(this.localBeanName));
  497 | 		}
  498 | 
  499 | 		@Override
  500 | 		public ConfigurationClassBeanDefinition cloneBeanDefinition() {
  501 | 			return new ConfigurationClassBeanDefinition(this);
  502 | 		}
  503 | 	}
  504 | 
  505 | 
  506 | 	/**
  507 | 	 * Evaluate {@code @Conditional} annotations, tracking results and taking into
  508 | 	 * account 'imported by'.
  509 | 	 */
  510 | 	private class TrackedConditionEvaluator {
  511 | 
  512 | 		private final Map<ConfigurationClass, Boolean> skipped = new HashMap<>();
  513 | 
  514 | 		public boolean shouldSkip(ConfigurationClass configClass) {
  515 | 			Boolean skip = this.skipped.get(configClass);
  516 | 			if (skip == null) {
  517 | 				if (configClass.isImported()) {
  518 | 					boolean allSkipped = true;
  519 | 					for (ConfigurationClass importedBy : configClass.getImportedBy()) {
  520 | 						if (!shouldSkip(importedBy)) {
  521 | 							allSkipped = false;
  522 | 							break;
  523 | 						}
  524 | 					}
  525 | 					if (allSkipped) {
  526 | 						// The config classes that imported this one were all skipped, therefore we are skipped...
  527 | 						skip = true;
  528 | 					}
  529 | 				}
  530 | 				if (skip == null) {
  531 | 					skip = conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN);
  532 | 				}
  533 | 				this.skipped.put(configClass, skip);
  534 | 			}
  535 | 			return skip;
  536 | 		}
  537 | 	}
  538 | 
  539 | }
```
