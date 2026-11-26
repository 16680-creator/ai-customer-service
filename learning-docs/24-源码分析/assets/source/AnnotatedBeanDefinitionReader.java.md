<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/AnnotatedBeanDefinitionReader.java -->
<!-- 正文引用：[[源码:AnnotatedBeanDefinitionReader.java#L起-L止]] ；代码块头部：(src: AnnotatedBeanDefinitionReader.java#L起-L止) -->

# AnnotatedBeanDefinitionReader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/AnnotatedBeanDefinitionReader.java` |
| 行数 | 308 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:02.310Z |

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
   19 | import java.lang.annotation.Annotation;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
   25 | import org.springframework.beans.factory.config.BeanDefinition;
   26 | import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
   27 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   28 | import org.springframework.beans.factory.support.AutowireCandidateQualifier;
   29 | import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
   30 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   31 | import org.springframework.beans.factory.support.BeanNameGenerator;
   32 | import org.springframework.core.env.Environment;
   33 | import org.springframework.core.env.EnvironmentCapable;
   34 | import org.springframework.core.env.StandardEnvironment;
   35 | import org.springframework.util.Assert;
   36 | 
   37 | /**
   38 |  * Convenient adapter for programmatic registration of bean classes.
   39 |  *
   40 |  * <p>This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying
   41 |  * the same resolution of annotations but for explicitly registered classes only.
   42 |  *
   43 |  * @author Juergen Hoeller
   44 |  * @author Chris Beams
   45 |  * @author Sam Brannen
   46 |  * @author Phillip Webb
   47 |  * @author Yanming Zhou
   48 |  * @since 3.0
   49 |  * @see AnnotationConfigApplicationContext#register
   50 |  */
   51 | public class AnnotatedBeanDefinitionReader {
   52 | 
   53 | 	private final BeanDefinitionRegistry registry;
   54 | 
   55 | 	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
   56 | 
   57 | 	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
   58 | 
   59 | 	private ConditionEvaluator conditionEvaluator;
   60 | 
   61 | 
   62 | 	/**
   63 | 	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
   64 | 	 * <p>If the registry is {@link EnvironmentCapable}, for example, is an {@code ApplicationContext},
   65 | 	 * the {@link Environment} will be inherited, otherwise a new
   66 | 	 * {@link StandardEnvironment} will be created and used.
   67 | 	 * @param registry the {@code BeanFactory} to load bean definitions into,
   68 | 	 * in the form of a {@code BeanDefinitionRegistry}
   69 | 	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
   70 | 	 * @see #setEnvironment(Environment)
   71 | 	 */
   72 | 	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
   73 | 		this(registry, getOrCreateEnvironment(registry));
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry,
   78 | 	 * using the given {@link Environment}.
   79 | 	 * @param registry the {@code BeanFactory} to load bean definitions into,
   80 | 	 * in the form of a {@code BeanDefinitionRegistry}
   81 | 	 * @param environment the {@code Environment} to use when evaluating bean definition
   82 | 	 * profiles.
   83 | 	 * @since 3.1
   84 | 	 */
   85 | 	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
   86 | 		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
   87 | 		Assert.notNull(environment, "Environment must not be null");
   88 | 		this.registry = registry;
   89 | 		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
   90 | 		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
   91 | 	}
   92 | 
   93 | 
   94 | 	/**
   95 | 	 * Get the BeanDefinitionRegistry that this reader operates on.
   96 | 	 */
   97 | 	public final BeanDefinitionRegistry getRegistry() {
   98 | 		return this.registry;
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Set the {@code Environment} to use when evaluating whether
  103 | 	 * {@link Conditional @Conditional}-annotated component classes should be registered.
  104 | 	 * <p>The default is a {@link StandardEnvironment}.
  105 | 	 */
  106 | 	public void setEnvironment(Environment environment) {
  107 | 		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Set the {@code BeanNameGenerator} to use for detected bean classes.
  112 | 	 * <p>The default is an {@link AnnotationBeanNameGenerator}.
  113 | 	 * @see FullyQualifiedAnnotationBeanNameGenerator
  114 | 	 * @see FullyQualifiedConfigurationBeanNameGenerator
  115 | 	 */
  116 | 	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
  117 | 		this.beanNameGenerator =
  118 | 				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
  119 | 	}
  120 | 
  121 | 	/**
  122 | 	 * Set the {@code ScopeMetadataResolver} to use for registered component classes.
  123 | 	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
  124 | 	 */
  125 | 	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
  126 | 		this.scopeMetadataResolver =
  127 | 				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
  128 | 	}
  129 | 
  130 | 
  131 | 	/**
  132 | 	 * Register one or more component classes to be processed.
  133 | 	 * <p>Calls to {@code register} are idempotent; adding the same
  134 | 	 * component class more than once has no additional effect.
  135 | 	 * @param componentClasses one or more component classes,
  136 | 	 * for example, {@link Configuration @Configuration} classes
  137 | 	 */
  138 | 	public void register(Class<?>... componentClasses) {
  139 | 		for (Class<?> componentClass : componentClasses) {
  140 | 			registerBean(componentClass);
  141 | 		}
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Register a bean from the given bean class, deriving its metadata from
  146 | 	 * class-declared annotations.
  147 | 	 * @param beanClass the class of the bean
  148 | 	 */
  149 | 	public void registerBean(Class<?> beanClass) {
  150 | 		doRegisterBean(beanClass, null, null, null, null);
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Register a bean from the given bean class, deriving its metadata from
  155 | 	 * class-declared annotations.
  156 | 	 * @param beanClass the class of the bean
  157 | 	 * @param name an explicit name for the bean
  158 | 	 * (or {@code null} for generating a default bean name)
  159 | 	 * @since 5.2
  160 | 	 */
  161 | 	public void registerBean(Class<?> beanClass, @Nullable String name) {
  162 | 		doRegisterBean(beanClass, name, null, null, null);
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * Register a bean from the given bean class, deriving its metadata from
  167 | 	 * class-declared annotations.
  168 | 	 * @param beanClass the class of the bean
  169 | 	 * @param qualifiers specific qualifier annotations to consider,
  170 | 	 * in addition to qualifiers at the bean class level
  171 | 	 */
  172 | 	@SuppressWarnings("unchecked")
  173 | 	public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
  174 | 		doRegisterBean(beanClass, null, qualifiers, null, null);
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Register a bean from the given bean class, deriving its metadata from
  179 | 	 * class-declared annotations.
  180 | 	 * @param beanClass the class of the bean
  181 | 	 * @param name an explicit name for the bean
  182 | 	 * (or {@code null} for generating a default bean name)
  183 | 	 * @param qualifiers specific qualifier annotations to consider,
  184 | 	 * in addition to qualifiers at the bean class level
  185 | 	 */
  186 | 	@SuppressWarnings("unchecked")
  187 | 	public void registerBean(Class<?> beanClass, @Nullable String name,
  188 | 			Class<? extends Annotation>... qualifiers) {
  189 | 
  190 | 		doRegisterBean(beanClass, name, qualifiers, null, null);
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Register a bean from the given bean class, deriving its metadata from
  195 | 	 * class-declared annotations, using the given supplier for obtaining a new
  196 | 	 * instance (possibly declared as a lambda expression or method reference).
  197 | 	 * @param beanClass the class of the bean
  198 | 	 * @param supplier a callback for creating an instance of the bean
  199 | 	 * (may be {@code null})
  200 | 	 * @since 5.0
  201 | 	 */
  202 | 	public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
  203 | 		doRegisterBean(beanClass, null, null, supplier, null);
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Register a bean from the given bean class, deriving its metadata from
  208 | 	 * class-declared annotations, using the given supplier for obtaining a new
  209 | 	 * instance (possibly declared as a lambda expression or method reference).
  210 | 	 * @param beanClass the class of the bean
  211 | 	 * @param name an explicit name for the bean
  212 | 	 * (or {@code null} for generating a default bean name)
  213 | 	 * @param supplier a callback for creating an instance of the bean
  214 | 	 * (may be {@code null})
  215 | 	 * @since 5.0
  216 | 	 */
  217 | 	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
  218 | 		doRegisterBean(beanClass, name, null, supplier, null);
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Register a bean from the given bean class, deriving its metadata from
  223 | 	 * class-declared annotations.
  224 | 	 * @param beanClass the class of the bean
  225 | 	 * @param name an explicit name for the bean
  226 | 	 * (or {@code null} for generating a default bean name)
  227 | 	 * @param supplier a callback for creating an instance of the bean
  228 | 	 * (may be {@code null})
  229 | 	 * @param customizers one or more callbacks for customizing the factory's
  230 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  231 | 	 * @since 5.2
  232 | 	 */
  233 | 	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
  234 | 			BeanDefinitionCustomizer... customizers) {
  235 | 
  236 | 		doRegisterBean(beanClass, name, null, supplier, customizers);
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Register a bean from the given bean class, deriving its metadata from
  241 | 	 * class-declared annotations.
  242 | 	 * @param beanClass the class of the bean
  243 | 	 * @param name an explicit name for the bean
  244 | 	 * @param qualifiers specific qualifier annotations to consider, if any,
  245 | 	 * in addition to qualifiers at the bean class level
  246 | 	 * @param supplier a callback for creating an instance of the bean
  247 | 	 * (may be {@code null})
  248 | 	 * @param customizers one or more callbacks for customizing the factory's
  249 | 	 * {@link BeanDefinition}, for example, setting a lazy-init or primary flag
  250 | 	 * @since 5.0
  251 | 	 */
  252 | 	private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
  253 | 			Class<? extends Annotation> @Nullable [] qualifiers, @Nullable Supplier<T> supplier,
  254 | 			BeanDefinitionCustomizer @Nullable [] customizers) {
  255 | 
  256 | 		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
  257 | 		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
  258 | 			return;
  259 | 		}
  260 | 
  261 | 		abd.setAttribute(ConfigurationClassUtils.CANDIDATE_ATTRIBUTE, Boolean.TRUE);
  262 | 		abd.setInstanceSupplier(supplier);
  263 | 		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
  264 | 		abd.setScope(scopeMetadata.getScopeName());
  265 | 		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
  266 | 
  267 | 		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
  268 | 		if (qualifiers != null) {
  269 | 			for (Class<? extends Annotation> qualifier : qualifiers) {
  270 | 				if (Primary.class == qualifier) {
  271 | 					abd.setPrimary(true);
  272 | 				}
  273 | 				else if (Fallback.class == qualifier) {
  274 | 					abd.setFallback(true);
  275 | 				}
  276 | 				else if (Lazy.class == qualifier) {
  277 | 					abd.setLazyInit(true);
  278 | 				}
  279 | 				else {
  280 | 					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
  281 | 				}
  282 | 			}
  283 | 		}
  284 | 		if (customizers != null) {
  285 | 			for (BeanDefinitionCustomizer customizer : customizers) {
  286 | 				customizer.customize(abd);
  287 | 			}
  288 | 		}
  289 | 
  290 | 		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
  291 | 		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
  292 | 		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
  293 | 	}
  294 | 
  295 | 
  296 | 	/**
  297 | 	 * Get the Environment from the given registry if possible, otherwise return a new
  298 | 	 * StandardEnvironment.
  299 | 	 */
  300 | 	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
  301 | 		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
  302 | 		if (registry instanceof EnvironmentCapable environmentCapable) {
  303 | 			return environmentCapable.getEnvironment();
  304 | 		}
  305 | 		return new StandardEnvironment();
  306 | 	}
  307 | 
  308 | }
```
