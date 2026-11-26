<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ClassPathBeanDefinitionScanner.java -->
<!-- 正文引用：[[源码:ClassPathBeanDefinitionScanner.java#L起-L止]] ；代码块头部：(src: ClassPathBeanDefinitionScanner.java#L起-L止) -->

# ClassPathBeanDefinitionScanner.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ClassPathBeanDefinitionScanner.java` |
| 行数 | 395 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.514Z |

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
   19 | import java.util.LinkedHashSet;
   20 | import java.util.Set;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   25 | import org.springframework.beans.factory.config.BeanDefinition;
   26 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   27 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   28 | import org.springframework.beans.factory.support.BeanDefinitionDefaults;
   29 | import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
   30 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   31 | import org.springframework.beans.factory.support.BeanNameGenerator;
   32 | import org.springframework.core.env.Environment;
   33 | import org.springframework.core.env.EnvironmentCapable;
   34 | import org.springframework.core.env.StandardEnvironment;
   35 | import org.springframework.core.io.ResourceLoader;
   36 | import org.springframework.util.Assert;
   37 | import org.springframework.util.ObjectUtils;
   38 | import org.springframework.util.PatternMatchUtils;
   39 | 
   40 | /**
   41 |  * A bean definition scanner that detects bean candidates on the classpath,
   42 |  * registering corresponding bean definitions with a given registry ({@code BeanFactory}
   43 |  * or {@code ApplicationContext}).
   44 |  *
   45 |  * <p>Candidate classes are detected through configurable type filters. The
   46 |  * default filters include classes that are annotated or meta-annotated with Spring's
   47 |  * {@link org.springframework.stereotype.Component @Component} annotation, such as the
   48 |  * {@link org.springframework.stereotype.Repository @Repository},
   49 |  * {@link org.springframework.stereotype.Service @Service}, and
   50 |  * {@link org.springframework.stereotype.Controller @Controller} stereotypes.
   51 |  *
   52 |  * <p>Also supports JSR-330's {@link jakarta.inject.Named} annotations, if available.
   53 |  *
   54 |  * @author Mark Fisher
   55 |  * @author Juergen Hoeller
   56 |  * @author Chris Beams
   57 |  * @since 2.5
   58 |  * @see AnnotationConfigApplicationContext#scan
   59 |  * @see org.springframework.stereotype.Component
   60 |  * @see org.springframework.stereotype.Repository
   61 |  * @see org.springframework.stereotype.Service
   62 |  * @see org.springframework.stereotype.Controller
   63 |  */
   64 | public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {
   65 | 
   66 | 	private final BeanDefinitionRegistry registry;
   67 | 
   68 | 	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();
   69 | 
   70 | 	private String @Nullable [] autowireCandidatePatterns;
   71 | 
   72 | 	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
   73 | 
   74 | 	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
   75 | 
   76 | 	private boolean includeAnnotationConfig = true;
   77 | 
   78 | 
   79 | 	/**
   80 | 	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
   81 | 	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
   82 | 	 * of a {@code BeanDefinitionRegistry}
   83 | 	 */
   84 | 	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
   85 | 		this(registry, true);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory.
   90 | 	 * <p>If the passed-in bean factory does not only implement the
   91 | 	 * {@code BeanDefinitionRegistry} interface but also the {@code ResourceLoader}
   92 | 	 * interface, it will be used as default {@code ResourceLoader} as well. This will
   93 | 	 * usually be the case for {@link org.springframework.context.ApplicationContext}
   94 | 	 * implementations.
   95 | 	 * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
   96 | 	 * will be a {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
   97 | 	 * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
   98 | 	 * environment will be used by this reader.  Otherwise, the reader will initialize and
   99 | 	 * use a {@link org.springframework.core.env.StandardEnvironment}. All
  100 | 	 * {@code ApplicationContext} implementations are {@code EnvironmentCapable}, while
  101 | 	 * normal {@code BeanFactory} implementations are not.
  102 | 	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
  103 | 	 * of a {@code BeanDefinitionRegistry}
  104 | 	 * @param useDefaultFilters whether to include the default filters for the
  105 | 	 * {@link org.springframework.stereotype.Component @Component},
  106 | 	 * {@link org.springframework.stereotype.Repository @Repository},
  107 | 	 * {@link org.springframework.stereotype.Service @Service}, and
  108 | 	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
  109 | 	 * @see #setResourceLoader
  110 | 	 * @see #setEnvironment
  111 | 	 */
  112 | 	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
  113 | 		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
  118 | 	 * using the given {@link Environment} when evaluating bean definition profile metadata.
  119 | 	 * <p>If the passed-in bean factory does not only implement the {@code
  120 | 	 * BeanDefinitionRegistry} interface but also the {@link ResourceLoader} interface, it
  121 | 	 * will be used as default {@code ResourceLoader} as well. This will usually be the
  122 | 	 * case for {@link org.springframework.context.ApplicationContext} implementations.
  123 | 	 * <p>If given a plain {@code BeanDefinitionRegistry}, the default {@code ResourceLoader}
  124 | 	 * will be a {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
  125 | 	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
  126 | 	 * of a {@code BeanDefinitionRegistry}
  127 | 	 * @param useDefaultFilters whether to include the default filters for the
  128 | 	 * {@link org.springframework.stereotype.Component @Component},
  129 | 	 * {@link org.springframework.stereotype.Repository @Repository},
  130 | 	 * {@link org.springframework.stereotype.Service @Service}, and
  131 | 	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
  132 | 	 * @param environment the Spring {@link Environment} to use when evaluating bean
  133 | 	 * definition profile metadata
  134 | 	 * @since 3.1
  135 | 	 * @see #setResourceLoader
  136 | 	 */
  137 | 	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
  138 | 			Environment environment) {
  139 | 
  140 | 		this(registry, useDefaultFilters, environment,
  141 | 				(registry instanceof ResourceLoader resourceLoader ? resourceLoader : null));
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
  146 | 	 * using the given {@link Environment} when evaluating bean definition profile metadata.
  147 | 	 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
  148 | 	 * of a {@code BeanDefinitionRegistry}
  149 | 	 * @param useDefaultFilters whether to include the default filters for the
  150 | 	 * {@link org.springframework.stereotype.Component @Component},
  151 | 	 * {@link org.springframework.stereotype.Repository @Repository},
  152 | 	 * {@link org.springframework.stereotype.Service @Service}, and
  153 | 	 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
  154 | 	 * @param environment the Spring {@link Environment} to use when evaluating bean
  155 | 	 * definition profile metadata
  156 | 	 * @param resourceLoader the {@link ResourceLoader} to use
  157 | 	 * @since 4.3.6
  158 | 	 */
  159 | 	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
  160 | 			Environment environment, @Nullable ResourceLoader resourceLoader) {
  161 | 
  162 | 		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
  163 | 		this.registry = registry;
  164 | 
  165 | 		if (useDefaultFilters) {
  166 | 			registerDefaultFilters();
  167 | 		}
  168 | 		setEnvironment(environment);
  169 | 		setResourceLoader(resourceLoader);
  170 | 	}
  171 | 
  172 | 
  173 | 	/**
  174 | 	 * Return the BeanDefinitionRegistry that this scanner operates on.
  175 | 	 */
  176 | 	@Override
  177 | 	public final BeanDefinitionRegistry getRegistry() {
  178 | 		return this.registry;
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Set the defaults to use for detected beans.
  183 | 	 * @see BeanDefinitionDefaults
  184 | 	 */
  185 | 	public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
  186 | 		this.beanDefinitionDefaults =
  187 | 				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
  188 | 	}
  189 | 
  190 | 	/**
  191 | 	 * Return the defaults to use for detected beans (never {@code null}).
  192 | 	 * @since 4.1
  193 | 	 */
  194 | 	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
  195 | 		return this.beanDefinitionDefaults;
  196 | 	}
  197 | 
  198 | 	/**
  199 | 	 * Set the name-matching patterns for determining autowire candidates.
  200 | 	 * @param autowireCandidatePatterns the patterns to match against
  201 | 	 */
  202 | 	public void setAutowireCandidatePatterns(String @Nullable ... autowireCandidatePatterns) {
  203 | 		this.autowireCandidatePatterns = autowireCandidatePatterns;
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Set the {@link BeanNameGenerator} to use for detected bean classes.
  208 | 	 * <p>Default is an {@code AnnotationBeanNameGenerator}.
  209 | 	 * @see AnnotationBeanNameGenerator
  210 | 	 * @see FullyQualifiedAnnotationBeanNameGenerator
  211 | 	 * @see FullyQualifiedConfigurationBeanNameGenerator
  212 | 	 */
  213 | 	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
  214 | 		this.beanNameGenerator =
  215 | 				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
  216 | 	}
  217 | 
  218 | 	/**
  219 | 	 * Set the ScopeMetadataResolver to use for detected bean classes.
  220 | 	 * Note that this will override any custom "scopedProxyMode" setting.
  221 | 	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
  222 | 	 * @see #setScopedProxyMode
  223 | 	 */
  224 | 	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
  225 | 		this.scopeMetadataResolver =
  226 | 				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Specify the proxy behavior for non-singleton scoped beans.
  231 | 	 * Note that this will override any custom "scopeMetadataResolver" setting.
  232 | 	 * <p>The default is {@link ScopedProxyMode#NO}.
  233 | 	 * @see #setScopeMetadataResolver
  234 | 	 */
  235 | 	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
  236 | 		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Specify whether to register annotation config post-processors.
  241 | 	 * <p>The default is to register the post-processors. Turn this off
  242 | 	 * to be able to ignore the annotations or to process them differently.
  243 | 	 */
  244 | 	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
  245 | 		this.includeAnnotationConfig = includeAnnotationConfig;
  246 | 	}
  247 | 
  248 | 
  249 | 	/**
  250 | 	 * Perform a scan within the specified base packages.
  251 | 	 * @param basePackages the packages to check for annotated classes
  252 | 	 * @return number of beans registered
  253 | 	 */
  254 | 	public int scan(String... basePackages) {
  255 | 		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();
  256 | 
  257 | 		doScan(basePackages);
  258 | 
  259 | 		// Register annotation config processors, if necessary.
  260 | 		if (this.includeAnnotationConfig) {
  261 | 			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
  262 | 		}
  263 | 
  264 | 		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
  265 | 	}
  266 | 
  267 | 	/**
  268 | 	 * Perform a scan within the specified base packages,
  269 | 	 * returning the registered bean definitions.
  270 | 	 * <p>This method does <i>not</i> register an annotation config processor
  271 | 	 * but rather leaves this up to the caller.
  272 | 	 * @param basePackages the packages to check for annotated classes
  273 | 	 * @return set of beans registered if any for tooling registration purposes (never {@code null})
  274 | 	 */
  275 | 	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
  276 | 		Assert.notEmpty(basePackages, "At least one base package must be specified");
  277 | 		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
  278 | 		for (String basePackage : basePackages) {
  279 | 			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
  280 | 			for (BeanDefinition candidate : candidates) {
  281 | 				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
  282 | 				candidate.setScope(scopeMetadata.getScopeName());
  283 | 				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
  284 | 				if (candidate instanceof AbstractBeanDefinition abstractBeanDefinition) {
  285 | 					postProcessBeanDefinition(abstractBeanDefinition, beanName);
  286 | 				}
  287 | 				if (candidate instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
  288 | 					AnnotationConfigUtils.processCommonDefinitionAnnotations(annotatedBeanDefinition);
  289 | 				}
  290 | 				if (checkCandidate(beanName, candidate)) {
  291 | 					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
  292 | 					definitionHolder =
  293 | 							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
  294 | 					beanDefinitions.add(definitionHolder);
  295 | 					registerBeanDefinition(definitionHolder, this.registry);
  296 | 				}
  297 | 			}
  298 | 		}
  299 | 		return beanDefinitions;
  300 | 	}
  301 | 
  302 | 	/**
  303 | 	 * Apply further settings to the given bean definition,
  304 | 	 * beyond the contents retrieved from scanning the component class.
  305 | 	 * @param beanDefinition the scanned bean definition
  306 | 	 * @param beanName the generated bean name for the given bean
  307 | 	 */
  308 | 	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
  309 | 		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
  310 | 		if (this.autowireCandidatePatterns != null) {
  311 | 			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
  312 | 		}
  313 | 	}
  314 | 
  315 | 	/**
  316 | 	 * Register the specified bean with the given registry.
  317 | 	 * <p>Can be overridden in subclasses, for example, to adapt the registration
  318 | 	 * process or to register further bean definitions for each scanned bean.
  319 | 	 * @param definitionHolder the bean definition plus bean name for the bean
  320 | 	 * @param registry the BeanDefinitionRegistry to register the bean with
  321 | 	 */
  322 | 	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
  323 | 		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
  324 | 	}
  325 | 
  326 | 
  327 | 	/**
  328 | 	 * Check the given candidate's bean name, determining whether the corresponding
  329 | 	 * bean definition needs to be registered or conflicts with an existing definition.
  330 | 	 * @param beanName the suggested name for the bean
  331 | 	 * @param beanDefinition the corresponding bean definition
  332 | 	 * @return {@code true} if the bean can be registered as-is;
  333 | 	 * {@code false} if it should be skipped because there is an
  334 | 	 * existing, compatible bean definition for the specified name
  335 | 	 * @throws IllegalStateException if an existing, incompatible bean definition
  336 | 	 * has been found for the specified name
  337 | 	 */
  338 | 	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
  339 | 		if (!this.registry.containsBeanDefinition(beanName)) {
  340 | 			return true;
  341 | 		}
  342 | 
  343 | 		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
  344 | 		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
  345 | 		if (originatingDef != null) {
  346 | 			existingDef = originatingDef;
  347 | 		}
  348 | 
  349 | 		// Explicitly registered overriding bean?
  350 | 		if (!(existingDef instanceof ScannedGenericBeanDefinition) &&
  351 | 				(this.registry.isBeanDefinitionOverridable(beanName) || ObjectUtils.nullSafeEquals(
  352 | 						beanDefinition.getBeanClassName(), existingDef.getBeanClassName()))) {
  353 | 			return false;
  354 | 		}
  355 | 
  356 | 		// Scanned same file or equivalent class twice?
  357 | 		if (isCompatible(beanDefinition, existingDef)) {
  358 | 			return false;
  359 | 		}
  360 | 
  361 | 		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
  362 | 				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
  363 | 				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
  364 | 	}
  365 | 
  366 | 	/**
  367 | 	 * Determine whether the given new bean definition is compatible with
  368 | 	 * the given existing bean definition.
  369 | 	 * <p>The default implementation considers them as compatible when the existing
  370 | 	 * bean definition comes from the same source or from a non-scanning source.
  371 | 	 * @param newDef the new bean definition, originated from scanning
  372 | 	 * @param existingDef the existing bean definition, potentially an
  373 | 	 * explicitly defined one or a previously generated one from scanning
  374 | 	 * @return whether the definitions are considered as compatible, with the
  375 | 	 * new definition to be skipped in favor of the existing definition
  376 | 	 */
  377 | 	protected boolean isCompatible(BeanDefinition newDef, BeanDefinition existingDef) {
  378 | 		return ((newDef.getSource() != null && newDef.getSource().equals(existingDef.getSource())) ||
  379 | 				newDef.equals(existingDef));
  380 | 	}
  381 | 
  382 | 
  383 | 	/**
  384 | 	 * Get the Environment from the given registry if possible, otherwise return a new
  385 | 	 * StandardEnvironment.
  386 | 	 */
  387 | 	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
  388 | 		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
  389 | 		if (registry instanceof EnvironmentCapable environmentCapable) {
  390 | 			return environmentCapable.getEnvironment();
  391 | 		}
  392 | 		return new StandardEnvironment();
  393 | 	}
  394 | 
  395 | }
```
