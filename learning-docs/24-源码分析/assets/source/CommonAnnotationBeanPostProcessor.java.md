<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/CommonAnnotationBeanPostProcessor.java -->
<!-- 正文引用：[[源码:CommonAnnotationBeanPostProcessor.java#L起-L止]] ；代码块头部：(src: CommonAnnotationBeanPostProcessor.java#L起-L止) -->

# CommonAnnotationBeanPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/CommonAnnotationBeanPostProcessor.java` |
| 行数 | 878 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:03.467Z |

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
   19 | import java.beans.PropertyDescriptor;
   20 | import java.io.Serializable;
   21 | import java.lang.annotation.Annotation;
   22 | import java.lang.reflect.AnnotatedElement;
   23 | import java.lang.reflect.Field;
   24 | import java.lang.reflect.Member;
   25 | import java.lang.reflect.Method;
   26 | import java.lang.reflect.Modifier;
   27 | import java.util.ArrayList;
   28 | import java.util.Collection;
   29 | import java.util.Collections;
   30 | import java.util.HashSet;
   31 | import java.util.LinkedHashSet;
   32 | import java.util.List;
   33 | import java.util.Map;
   34 | import java.util.Set;
   35 | import java.util.concurrent.ConcurrentHashMap;
   36 | 
   37 | import org.jspecify.annotations.Nullable;
   38 | 
   39 | import org.springframework.aop.TargetSource;
   40 | import org.springframework.aop.framework.ProxyFactory;
   41 | import org.springframework.aot.generate.AccessControl;
   42 | import org.springframework.aot.generate.GeneratedClass;
   43 | import org.springframework.aot.generate.GeneratedMethod;
   44 | import org.springframework.aot.generate.GenerationContext;
   45 | import org.springframework.aot.hint.ExecutableMode;
   46 | import org.springframework.aot.hint.RuntimeHints;
   47 | import org.springframework.aot.hint.support.ClassHintUtils;
   48 | import org.springframework.beans.BeanUtils;
   49 | import org.springframework.beans.PropertyValues;
   50 | import org.springframework.beans.factory.BeanCreationException;
   51 | import org.springframework.beans.factory.BeanFactory;
   52 | import org.springframework.beans.factory.BeanFactoryAware;
   53 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   54 | import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
   55 | import org.springframework.beans.factory.annotation.InjectionMetadata;
   56 | import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
   57 | import org.springframework.beans.factory.aot.BeanRegistrationCode;
   58 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   59 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   60 | import org.springframework.beans.factory.config.DependencyDescriptor;
   61 | import org.springframework.beans.factory.config.EmbeddedValueResolver;
   62 | import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
   63 | import org.springframework.beans.factory.support.AutowireCandidateResolver;
   64 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   65 | import org.springframework.beans.factory.support.RegisteredBean;
   66 | import org.springframework.beans.factory.support.RootBeanDefinition;
   67 | import org.springframework.core.BridgeMethodResolver;
   68 | import org.springframework.core.Ordered;
   69 | import org.springframework.core.annotation.AnnotationUtils;
   70 | import org.springframework.javapoet.ClassName;
   71 | import org.springframework.javapoet.CodeBlock;
   72 | import org.springframework.jndi.support.SimpleJndiBeanFactory;
   73 | import org.springframework.util.Assert;
   74 | import org.springframework.util.ClassUtils;
   75 | import org.springframework.util.CollectionUtils;
   76 | import org.springframework.util.ObjectUtils;
   77 | import org.springframework.util.ReflectionUtils;
   78 | import org.springframework.util.StringUtils;
   79 | import org.springframework.util.StringValueResolver;
   80 | 
   81 | /**
   82 |  * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
   83 |  * that supports common Java annotations out of the box, in particular the common
   84 |  * annotations in the {@code jakarta.annotation} package. These common Java
   85 |  * annotations are supported in many Jakarta EE technologies (for example, JSF and JAX-RS).
   86 |  *
   87 |  * <p>This post-processor includes support for the {@link jakarta.annotation.PostConstruct}
   88 |  * and {@link jakarta.annotation.PreDestroy} annotations - as init annotation
   89 |  * and destroy annotation, respectively - through inheriting from
   90 |  * {@link InitDestroyAnnotationBeanPostProcessor} with pre-configured annotation types.
   91 |  *
   92 |  * <p>The central element is the {@link jakarta.annotation.Resource} annotation
   93 |  * for annotation-driven injection of named beans, by default from the containing
   94 |  * Spring BeanFactory, with only {@code mappedName} references resolved in JNDI.
   95 |  * The {@link #setAlwaysUseJndiLookup "alwaysUseJndiLookup" flag} enforces JNDI lookups
   96 |  * equivalent to standard Jakarta EE resource injection for {@code name} references
   97 |  * and default names as well. The target beans can be simple POJOs, with no special
   98 |  * requirements other than the type having to match.
   99 |  *
  100 |  * <p>This post-processor also supports the EJB {@link jakarta.ejb.EJB} annotation,
  101 |  * analogous to {@link jakarta.annotation.Resource}, with the capability to
  102 |  * specify both a local bean name and a global JNDI name for fallback retrieval.
  103 |  * The target beans can be plain POJOs as well as EJB Session Beans in this case.
  104 |  *
  105 |  * <p>For default usage, resolving resource names as Spring bean names,
  106 |  * simply define the following in your application context:
  107 |  *
  108 |  * <pre class="code">
  109 |  * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"/&gt;</pre>
  110 |  *
  111 |  * For direct JNDI access, resolving resource names as JNDI resource references
  112 |  * within the Jakarta EE application's "java:comp/env/" namespace, use the following:
  113 |  *
  114 |  * <pre class="code">
  115 |  * &lt;bean class="org.springframework.context.annotation.CommonAnnotationBeanPostProcessor"&gt;
  116 |  *   &lt;property name="alwaysUseJndiLookup" value="true"/&gt;
  117 |  * &lt;/bean&gt;</pre>
  118 |  *
  119 |  * {@code mappedName} references will always be resolved in JNDI,
  120 |  * allowing for global JNDI names (including "java:" prefix) as well. The
  121 |  * "alwaysUseJndiLookup" flag just affects {@code name} references and
  122 |  * default names (inferred from the field name / property name).
  123 |  *
  124 |  * <p><b>NOTE:</b> A default CommonAnnotationBeanPostProcessor will be registered
  125 |  * by the "context:annotation-config" and "context:component-scan" XML tags.
  126 |  * Remove or turn off the default annotation configuration there if you intend
  127 |  * to specify a custom CommonAnnotationBeanPostProcessor bean definition!
  128 |  * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
  129 |  * thus the latter configuration will override the former for properties wired through
  130 |  * both approaches.
  131 |  *
  132 |  * @author Juergen Hoeller
  133 |  * @author Sam Brannen
  134 |  * @since 2.5
  135 |  * @see #setAlwaysUseJndiLookup
  136 |  * @see #setResourceFactory
  137 |  * @see org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor
  138 |  * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
  139 |  */
  140 | @SuppressWarnings("serial")
  141 | public class CommonAnnotationBeanPostProcessor extends InitDestroyAnnotationBeanPostProcessor
  142 | 		implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, Serializable {
  143 | 
  144 | 	// Defensive reference to JNDI API for JDK 9+ (optional java.naming module)
  145 | 	private static final boolean JNDI_PRESENT = ClassUtils.isPresent(
  146 | 			"javax.naming.InitialContext", CommonAnnotationBeanPostProcessor.class.getClassLoader());
  147 | 
  148 | 	private static final @Nullable Class<? extends Annotation> JAKARTA_RESOURCE_TYPE;
  149 | 
  150 | 	private static final @Nullable Class<? extends Annotation> EJB_ANNOTATION_TYPE;
  151 | 
  152 | 	private static final Set<Class<? extends Annotation>> resourceAnnotationTypes = CollectionUtils.newLinkedHashSet(2);
  153 | 
  154 | 	static {
  155 | 		JAKARTA_RESOURCE_TYPE = AnnotationUtils.loadAnnotationType("jakarta.annotation.Resource");
  156 | 		if (JAKARTA_RESOURCE_TYPE != null) {
  157 | 			resourceAnnotationTypes.add(JAKARTA_RESOURCE_TYPE);
  158 | 		}
  159 | 
  160 | 		EJB_ANNOTATION_TYPE = AnnotationUtils.loadAnnotationType("jakarta.ejb.EJB");
  161 | 		if (EJB_ANNOTATION_TYPE != null) {
  162 | 			resourceAnnotationTypes.add(EJB_ANNOTATION_TYPE);
  163 | 		}
  164 | 	}
  165 | 
  166 | 
  167 | 	private final Set<String> ignoredResourceTypes = new HashSet<>(1);
  168 | 
  169 | 	private boolean fallbackToDefaultTypeMatch = true;
  170 | 
  171 | 	private boolean alwaysUseJndiLookup = false;
  172 | 
  173 | 	private transient @Nullable BeanFactory jndiFactory;
  174 | 
  175 | 	private transient @Nullable BeanFactory resourceFactory;
  176 | 
  177 | 	private transient @Nullable BeanFactory beanFactory;
  178 | 
  179 | 	private transient @Nullable StringValueResolver embeddedValueResolver;
  180 | 
  181 | 	private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
  182 | 
  183 | 
  184 | 	/**
  185 | 	 * Create a new CommonAnnotationBeanPostProcessor,
  186 | 	 * with the init and destroy annotation types set to
  187 | 	 * {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy},
  188 | 	 * respectively.
  189 | 	 */
  190 | 	public CommonAnnotationBeanPostProcessor() {
  191 | 		setOrder(Ordered.LOWEST_PRECEDENCE - 3);
  192 | 
  193 | 		// Jakarta EE 9 set of annotations in jakarta.annotation package
  194 | 		addInitAnnotationType(AnnotationUtils.loadAnnotationType("jakarta.annotation.PostConstruct"));
  195 | 		addDestroyAnnotationType(AnnotationUtils.loadAnnotationType("jakarta.annotation.PreDestroy"));
  196 | 
  197 | 		// java.naming module present on JDK 9+?
  198 | 		if (JNDI_PRESENT) {
  199 | 			this.jndiFactory = new SimpleJndiBeanFactory();
  200 | 		}
  201 | 	}
  202 | 
  203 | 
  204 | 	/**
  205 | 	 * Ignore the given resource type when resolving {@code @Resource} annotations.
  206 | 	 * @param resourceType the resource type to ignore
  207 | 	 */
  208 | 	public void ignoreResourceType(String resourceType) {
  209 | 		Assert.notNull(resourceType, "Ignored resource type must not be null");
  210 | 		this.ignoredResourceTypes.add(resourceType);
  211 | 	}
  212 | 
  213 | 	/**
  214 | 	 * Set whether to allow a fallback to a type match if no explicit name has been
  215 | 	 * specified. The default name (i.e. the field name or bean property name) will
  216 | 	 * still be checked first; if a bean of that name exists, it will be taken.
  217 | 	 * However, if no bean of that name exists, a by-type resolution of the
  218 | 	 * dependency will be attempted if this flag is "true".
  219 | 	 * <p>Default is "true". Switch this flag to "false" in order to enforce a
  220 | 	 * by-name lookup in all cases, throwing an exception in case of no name match.
  221 | 	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#resolveDependency
  222 | 	 */
  223 | 	public void setFallbackToDefaultTypeMatch(boolean fallbackToDefaultTypeMatch) {
  224 | 		this.fallbackToDefaultTypeMatch = fallbackToDefaultTypeMatch;
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Set whether to always use JNDI lookups equivalent to standard Jakarta EE resource
  229 | 	 * injection, <b>even for {@code name} attributes and default names</b>.
  230 | 	 * <p>Default is "false": Resource names are used for Spring bean lookups in the
  231 | 	 * containing BeanFactory; only {@code mappedName} attributes point directly
  232 | 	 * into JNDI. Switch this flag to "true" for enforcing Jakarta EE style JNDI lookups
  233 | 	 * in any case, even for {@code name} attributes and default names.
  234 | 	 * @see #setJndiFactory
  235 | 	 * @see #setResourceFactory
  236 | 	 */
  237 | 	public void setAlwaysUseJndiLookup(boolean alwaysUseJndiLookup) {
  238 | 		this.alwaysUseJndiLookup = alwaysUseJndiLookup;
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * Specify the factory for objects to be injected into {@code @Resource} /
  243 | 	 * {@code @EJB} annotated fields and setter methods,
  244 | 	 * <b>for {@code mappedName} attributes that point directly into JNDI</b>.
  245 | 	 * This factory will also be used if "alwaysUseJndiLookup" is set to "true" in order
  246 | 	 * to enforce JNDI lookups even for {@code name} attributes and default names.
  247 | 	 * <p>The default is a {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
  248 | 	 * for JNDI lookup behavior equivalent to standard Jakarta EE resource injection.
  249 | 	 * @see #setResourceFactory
  250 | 	 * @see #setAlwaysUseJndiLookup
  251 | 	 */
  252 | 	public void setJndiFactory(BeanFactory jndiFactory) {
  253 | 		Assert.notNull(jndiFactory, "BeanFactory must not be null");
  254 | 		this.jndiFactory = jndiFactory;
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * Specify the factory for objects to be injected into {@code @Resource} /
  259 | 	 * {@code @EJB} annotated fields and setter methods,
  260 | 	 * <b>for {@code name} attributes and default names</b>.
  261 | 	 * <p>The default is the BeanFactory that this post-processor is defined in,
  262 | 	 * if any, looking up resource names as Spring bean names. Specify the resource
  263 | 	 * factory explicitly for programmatic usage of this post-processor.
  264 | 	 * <p>Specifying Spring's {@link org.springframework.jndi.support.SimpleJndiBeanFactory}
  265 | 	 * leads to JNDI lookup behavior equivalent to standard Jakarta EE resource injection,
  266 | 	 * even for {@code name} attributes and default names. This is the same behavior
  267 | 	 * that the "alwaysUseJndiLookup" flag enables.
  268 | 	 * @see #setAlwaysUseJndiLookup
  269 | 	 */
  270 | 	public void setResourceFactory(BeanFactory resourceFactory) {
  271 | 		Assert.notNull(resourceFactory, "BeanFactory must not be null");
  272 | 		this.resourceFactory = resourceFactory;
  273 | 	}
  274 | 
  275 | 	@Override
  276 | 	public void setBeanFactory(BeanFactory beanFactory) {
  277 | 		Assert.notNull(beanFactory, "BeanFactory must not be null");
  278 | 		this.beanFactory = beanFactory;
  279 | 		if (this.resourceFactory == null) {
  280 | 			this.resourceFactory = beanFactory;
  281 | 		}
  282 | 		if (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory) {
  283 | 			this.embeddedValueResolver = new EmbeddedValueResolver(configurableBeanFactory);
  284 | 		}
  285 | 	}
  286 | 
  287 | 
  288 | 	@Override
  289 | 	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
  290 | 		super.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
  291 | 		InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
  292 | 		metadata.checkConfigMembers(beanDefinition);
  293 | 	}
  294 | 
  295 | 	@Override
  296 | 	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
  297 | 		BeanRegistrationAotContribution parentAotContribution = super.processAheadOfTime(registeredBean);
  298 | 		Class<?> beanClass = registeredBean.getBeanClass();
  299 | 		String beanName = registeredBean.getBeanName();
  300 | 		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
  301 | 		InjectionMetadata metadata = findResourceMetadata(beanName, beanClass,
  302 | 				beanDefinition.getPropertyValues());
  303 | 		Collection<LookupElement> injectedElements = getInjectedElements(metadata,
  304 | 				beanDefinition.getPropertyValues());
  305 | 		if (!ObjectUtils.isEmpty(injectedElements)) {
  306 | 			AotContribution aotContribution = new AotContribution(beanClass, injectedElements,
  307 | 					getAutowireCandidateResolver(registeredBean));
  308 | 			return BeanRegistrationAotContribution.concat(parentAotContribution, aotContribution);
  309 | 		}
  310 | 		return parentAotContribution;
  311 | 	}
  312 | 
  313 | 	private @Nullable AutowireCandidateResolver getAutowireCandidateResolver(RegisteredBean registeredBean) {
  314 | 		if (registeredBean.getBeanFactory() instanceof DefaultListableBeanFactory lbf) {
  315 | 			return lbf.getAutowireCandidateResolver();
  316 | 		}
  317 | 		return null;
  318 | 	}
  319 | 
  320 | 	@SuppressWarnings({ "rawtypes", "unchecked" })
  321 | 	private Collection<LookupElement> getInjectedElements(InjectionMetadata metadata, PropertyValues propertyValues) {
  322 | 		return (Collection) metadata.getInjectedElements(propertyValues);
  323 | 	}
  324 | 
  325 | 	@Override
  326 | 	public void resetBeanDefinition(String beanName) {
  327 | 		this.injectionMetadataCache.remove(beanName);
  328 | 	}
  329 | 
  330 | 	@Override
  331 | 	public @Nullable Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
  332 | 		return null;
  333 | 	}
  334 | 
  335 | 	@Override
  336 | 	public boolean postProcessAfterInstantiation(Object bean, String beanName) {
  337 | 		return true;
  338 | 	}
  339 | 
  340 | 	@Override
  341 | 	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
  342 | 		InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
  343 | 		try {
  344 | 			metadata.inject(bean, beanName, pvs);
  345 | 		}
  346 | 		catch (Throwable ex) {
  347 | 			throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
  348 | 		}
  349 | 		return pvs;
  350 | 	}
  351 | 
  352 | 	/**
  353 | 	 * <em>Native</em> processing method for direct calls with an arbitrary target
  354 | 	 * instance, resolving all of its fields and methods which are annotated with
  355 | 	 * one of the supported 'resource' annotation types.
  356 | 	 * @param bean the target instance to process
  357 | 	 * @throws BeanCreationException if resource injection failed
  358 | 	 * @since 6.1.3
  359 | 	 */
  360 | 	public void processInjection(Object bean) throws BeanCreationException {
  361 | 		Class<?> clazz = bean.getClass();
  362 | 		InjectionMetadata metadata = findResourceMetadata(clazz.getName(), clazz, null);
  363 | 		try {
  364 | 			metadata.inject(bean, null, null);
  365 | 		}
  366 | 		catch (BeanCreationException ex) {
  367 | 			throw ex;
  368 | 		}
  369 | 		catch (Throwable ex) {
  370 | 			throw new BeanCreationException(
  371 | 					"Injection of resource dependencies failed for class [" + clazz + "]", ex);
  372 | 		}
  373 | 	}
  374 | 
  375 | 
  376 | 	private InjectionMetadata findResourceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
  377 | 		// Fall back to class name as cache key, for backwards compatibility with custom callers.
  378 | 		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
  379 | 		// Quick check on the concurrent map first, with minimal locking.
  380 | 		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
  381 | 		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
  382 | 			synchronized (this.injectionMetadataCache) {
  383 | 				metadata = this.injectionMetadataCache.get(cacheKey);
  384 | 				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
  385 | 					if (metadata != null) {
  386 | 						metadata.clear(pvs);
  387 | 					}
  388 | 					metadata = buildResourceMetadata(clazz);
  389 | 					this.injectionMetadataCache.put(cacheKey, metadata);
  390 | 				}
  391 | 			}
  392 | 		}
  393 | 		return metadata;
  394 | 	}
  395 | 
  396 | 	private InjectionMetadata buildResourceMetadata(Class<?> clazz) {
  397 | 		if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
  398 | 			return InjectionMetadata.EMPTY;
  399 | 		}
  400 | 
  401 | 		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
  402 | 		Class<?> targetClass = ClassUtils.getUserClass(clazz);
  403 | 
  404 | 		do {
  405 | 			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
  406 | 
  407 | 			ReflectionUtils.doWithLocalFields(targetClass, field -> {
  408 | 				if (EJB_ANNOTATION_TYPE != null && field.isAnnotationPresent(EJB_ANNOTATION_TYPE)) {
  409 | 					if (Modifier.isStatic(field.getModifiers())) {
  410 | 						throw new IllegalStateException("@EJB annotation is not supported on static fields");
  411 | 					}
  412 | 					currElements.add(new EjbRefElement(field, field, null));
  413 | 				}
  414 | 				else if (JAKARTA_RESOURCE_TYPE != null && field.isAnnotationPresent(JAKARTA_RESOURCE_TYPE)) {
  415 | 					if (Modifier.isStatic(field.getModifiers())) {
  416 | 						throw new IllegalStateException("@Resource annotation is not supported on static fields");
  417 | 					}
  418 | 					if (!this.ignoredResourceTypes.contains(field.getType().getName())) {
  419 | 						currElements.add(new ResourceElement(field, field, null));
  420 | 					}
  421 | 				}
  422 | 			});
  423 | 
  424 | 			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
  425 | 				if (method.isBridge()) {
  426 | 					return;
  427 | 				}
  428 | 				if (EJB_ANNOTATION_TYPE != null && method.isAnnotationPresent(EJB_ANNOTATION_TYPE)) {
  429 | 					if (method.equals(BridgeMethodResolver.getMostSpecificMethod(method, clazz))) {
  430 | 						if (Modifier.isStatic(method.getModifiers())) {
  431 | 							throw new IllegalStateException("@EJB annotation is not supported on static methods");
  432 | 						}
  433 | 						if (method.getParameterCount() != 1) {
  434 | 							throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
  435 | 						}
  436 | 						PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method, clazz);
  437 | 						currElements.add(new EjbRefElement(method, method, pd));
  438 | 					}
  439 | 				}
  440 | 				else if (JAKARTA_RESOURCE_TYPE != null && method.isAnnotationPresent(JAKARTA_RESOURCE_TYPE)) {
  441 | 					if (method.equals(BridgeMethodResolver.getMostSpecificMethod(method, clazz))) {
  442 | 						if (Modifier.isStatic(method.getModifiers())) {
  443 | 							throw new IllegalStateException("@Resource annotation is not supported on static methods");
  444 | 						}
  445 | 						Class<?>[] paramTypes = method.getParameterTypes();
  446 | 						if (paramTypes.length != 1) {
  447 | 							throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
  448 | 						}
  449 | 						if (!this.ignoredResourceTypes.contains(paramTypes[0].getName())) {
  450 | 							PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method, clazz);
  451 | 							currElements.add(new ResourceElement(method, method, pd));
  452 | 						}
  453 | 					}
  454 | 				}
  455 | 			});
  456 | 
  457 | 			elements.addAll(0, currElements);
  458 | 			targetClass = targetClass.getSuperclass();
  459 | 		}
  460 | 		while (targetClass != null && targetClass != Object.class);
  461 | 
  462 | 		return InjectionMetadata.forElements(elements, clazz);
  463 | 	}
  464 | 
  465 | 	/**
  466 | 	 * Obtain a lazily resolving resource proxy for the given name and type,
  467 | 	 * delegating to {@link #getResource} on demand once a method call comes in.
  468 | 	 * @param element the descriptor for the annotated field/method
  469 | 	 * @param requestingBeanName the name of the requesting bean
  470 | 	 * @return the resource object (never {@code null})
  471 | 	 * @since 4.2
  472 | 	 * @see #getResource
  473 | 	 * @see Lazy
  474 | 	 */
  475 | 	protected Object buildLazyResourceProxy(LookupElement element, @Nullable String requestingBeanName) {
  476 | 		TargetSource ts = new TargetSource() {
  477 | 			@Override
  478 | 			public Class<?> getTargetClass() {
  479 | 				return element.lookupType;
  480 | 			}
  481 | 			@Override
  482 | 			public Object getTarget() {
  483 | 				return getResource(element, requestingBeanName);
  484 | 			}
  485 | 		};
  486 | 
  487 | 		ProxyFactory pf = new ProxyFactory();
  488 | 		pf.setTargetSource(ts);
  489 | 		if (element.lookupType.isInterface()) {
  490 | 			pf.addInterface(element.lookupType);
  491 | 		}
  492 | 		ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory ?
  493 | 				configurableBeanFactory.getBeanClassLoader() : null);
  494 | 		return pf.getProxy(classLoader);
  495 | 	}
  496 | 
  497 | 	/**
  498 | 	 * Obtain the resource object for the given name and type.
  499 | 	 * @param element the descriptor for the annotated field/method
  500 | 	 * @param requestingBeanName the name of the requesting bean
  501 | 	 * @return the resource object (never {@code null})
  502 | 	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
  503 | 	 */
  504 | 	protected Object getResource(LookupElement element, @Nullable String requestingBeanName)
  505 | 			throws NoSuchBeanDefinitionException {
  506 | 
  507 | 		// JNDI lookup to perform?
  508 | 		String jndiName = null;
  509 | 		if (StringUtils.hasLength(element.mappedName)) {
  510 | 			jndiName = element.mappedName;
  511 | 		}
  512 | 		else if (this.alwaysUseJndiLookup) {
  513 | 			jndiName = element.name;
  514 | 		}
  515 | 		if (jndiName != null) {
  516 | 			if (this.jndiFactory == null) {
  517 | 				throw new NoSuchBeanDefinitionException(element.lookupType,
  518 | 						"No JNDI factory configured - specify the 'jndiFactory' property");
  519 | 			}
  520 | 			return this.jndiFactory.getBean(jndiName, element.lookupType);
  521 | 		}
  522 | 
  523 | 		// Regular resource autowiring
  524 | 		if (this.resourceFactory == null) {
  525 | 			throw new NoSuchBeanDefinitionException(element.lookupType,
  526 | 					"No resource factory configured - specify the 'resourceFactory' property");
  527 | 		}
  528 | 		return autowireResource(this.resourceFactory, element, requestingBeanName);
  529 | 	}
  530 | 
  531 | 	/**
  532 | 	 * Obtain a resource object for the given name and type through autowiring
  533 | 	 * based on the given factory.
  534 | 	 * @param factory the factory to autowire against
  535 | 	 * @param element the descriptor for the annotated field/method
  536 | 	 * @param requestingBeanName the name of the requesting bean
  537 | 	 * @return the resource object (never {@code null})
  538 | 	 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
  539 | 	 */
  540 | 	protected Object autowireResource(BeanFactory factory, LookupElement element, @Nullable String requestingBeanName)
  541 | 			throws NoSuchBeanDefinitionException {
  542 | 
  543 | 		Object resource;
  544 | 		Set<String> autowiredBeanNames;
  545 | 		String name = element.name;
  546 | 
  547 | 		if (factory instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
  548 | 			if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
  549 | 				autowiredBeanNames = new LinkedHashSet<>();
  550 | 				resource = autowireCapableBeanFactory.resolveDependency(
  551 | 						element.getDependencyDescriptor(), requestingBeanName, autowiredBeanNames, null);
  552 | 				if (resource == null) {
  553 | 					throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
  554 | 				}
  555 | 			}
  556 | 			else {
  557 | 				resource = autowireCapableBeanFactory.resolveBeanByName(name, element.getDependencyDescriptor());
  558 | 				autowiredBeanNames = Collections.singleton(name);
  559 | 			}
  560 | 		}
  561 | 		else {
  562 | 			resource = factory.getBean(name, element.lookupType);
  563 | 			autowiredBeanNames = Collections.singleton(name);
  564 | 		}
  565 | 
  566 | 		if (factory instanceof ConfigurableBeanFactory configurableBeanFactory) {
  567 | 			for (String autowiredBeanName : autowiredBeanNames) {
  568 | 				if (requestingBeanName != null && configurableBeanFactory.containsBean(autowiredBeanName)) {
  569 | 					configurableBeanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
  570 | 				}
  571 | 			}
  572 | 		}
  573 | 
  574 | 		return resource;
  575 | 	}
  576 | 
  577 | 
  578 | 	/**
  579 | 	 * Class representing generic injection information about an annotated field
  580 | 	 * or setter method, supporting @Resource and related annotations.
  581 | 	 */
  582 | 	protected abstract static class LookupElement extends InjectionMetadata.InjectedElement {
  583 | 
  584 | 		protected String name = "";
  585 | 
  586 | 		protected boolean isDefaultName = false;
  587 | 
  588 | 		protected Class<?> lookupType = Object.class;
  589 | 
  590 | 		protected @Nullable String mappedName;
  591 | 
  592 | 		public LookupElement(Member member, @Nullable PropertyDescriptor pd) {
  593 | 			super(member, pd);
  594 | 		}
  595 | 
  596 | 		/**
  597 | 		 * Return the resource name for the lookup.
  598 | 		 */
  599 | 		public final String getName() {
  600 | 			return this.name;
  601 | 		}
  602 | 
  603 | 		/**
  604 | 		 * Return the desired type for the lookup.
  605 | 		 */
  606 | 		public final Class<?> getLookupType() {
  607 | 			return this.lookupType;
  608 | 		}
  609 | 
  610 | 		/**
  611 | 		 * Build a DependencyDescriptor for the underlying field/method.
  612 | 		 */
  613 | 		public final DependencyDescriptor getDependencyDescriptor() {
  614 | 			if (this.isField) {
  615 | 				return new ResourceElementResolver.LookupDependencyDescriptor(
  616 | 						(Field) this.member, this.lookupType, isLazyLookup());
  617 | 			}
  618 | 			else {
  619 | 				return new ResourceElementResolver.LookupDependencyDescriptor(
  620 | 						(Method) this.member, this.lookupType, isLazyLookup());
  621 | 			}
  622 | 		}
  623 | 
  624 | 		/**
  625 | 		 * Determine whether this dependency is marked for lazy lookup.
  626 | 		 * The default is {@code false}.
  627 | 		 * @since 6.1.2
  628 | 		 */
  629 | 		boolean isLazyLookup() {
  630 | 			return false;
  631 | 		}
  632 | 	}
  633 | 
  634 | 
  635 | 	/**
  636 | 	 * Class representing injection information about an annotated field
  637 | 	 * or setter method, supporting the @Resource annotation.
  638 | 	 */
  639 | 	private class ResourceElement extends LookupElement {
  640 | 
  641 | 		private final boolean lazyLookup;
  642 | 
  643 | 		public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
  644 | 			super(member, pd);
  645 | 			jakarta.annotation.Resource resource = ae.getAnnotation(jakarta.annotation.Resource.class);
  646 | 			String resourceName = resource.name();
  647 | 			Class<?> resourceType = resource.type();
  648 | 			this.isDefaultName = !StringUtils.hasLength(resourceName);
  649 | 			if (this.isDefaultName) {
  650 | 				resourceName = this.member.getName();
  651 | 				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
  652 | 					resourceName = StringUtils.uncapitalizeAsProperty(resourceName.substring(3));
  653 | 				}
  654 | 			}
  655 | 			else if (embeddedValueResolver != null) {
  656 | 				resourceName = embeddedValueResolver.resolveStringValue(resourceName);
  657 | 			}
  658 | 			if (Object.class != resourceType) {
  659 | 				checkResourceType(resourceType);
  660 | 			}
  661 | 			else {
  662 | 				// No resource type specified... check field/method.
  663 | 				resourceType = getResourceType();
  664 | 			}
  665 | 			this.name = (resourceName != null ? resourceName : "");
  666 | 			this.lookupType = resourceType;
  667 | 			String lookupValue = resource.lookup();
  668 | 			this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
  669 | 			Lazy lazy = ae.getAnnotation(Lazy.class);
  670 | 			this.lazyLookup = (lazy != null && lazy.value());
  671 | 		}
  672 | 
  673 | 		@Override
  674 | 		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
  675 | 			return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
  676 | 					getResource(this, requestingBeanName));
  677 | 		}
  678 | 
  679 | 		@Override
  680 | 		boolean isLazyLookup() {
  681 | 			return this.lazyLookup;
  682 | 		}
  683 | 	}
  684 | 
  685 | 
  686 | 	/**
  687 | 	 * Class representing injection information about an annotated field
  688 | 	 * or setter method, supporting the @EJB annotation.
  689 | 	 */
  690 | 	private class EjbRefElement extends LookupElement {
  691 | 
  692 | 		private final String beanName;
  693 | 
  694 | 		public EjbRefElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
  695 | 			super(member, pd);
  696 | 			jakarta.ejb.EJB resource = ae.getAnnotation(jakarta.ejb.EJB.class);
  697 | 			String resourceBeanName = resource.beanName();
  698 | 			String resourceName = resource.name();
  699 | 			this.isDefaultName = !StringUtils.hasLength(resourceName);
  700 | 			if (this.isDefaultName) {
  701 | 				resourceName = this.member.getName();
  702 | 				if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
  703 | 					resourceName = StringUtils.uncapitalizeAsProperty(resourceName.substring(3));
  704 | 				}
  705 | 			}
  706 | 			Class<?> resourceType = resource.beanInterface();
  707 | 			if (Object.class != resourceType) {
  708 | 				checkResourceType(resourceType);
  709 | 			}
  710 | 			else {
  711 | 				// No resource type specified... check field/method.
  712 | 				resourceType = getResourceType();
  713 | 			}
  714 | 			this.beanName = resourceBeanName;
  715 | 			this.name = resourceName;
  716 | 			this.lookupType = resourceType;
  717 | 			this.mappedName = resource.mappedName();
  718 | 		}
  719 | 
  720 | 		@Override
  721 | 		protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
  722 | 			if (StringUtils.hasLength(this.beanName)) {
  723 | 				if (beanFactory != null && beanFactory.containsBean(this.beanName)) {
  724 | 					// Local match found for explicitly specified local bean name.
  725 | 					Object bean = beanFactory.getBean(this.beanName, this.lookupType);
  726 | 					if (requestingBeanName != null && beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory) {
  727 | 						configurableBeanFactory.registerDependentBean(this.beanName, requestingBeanName);
  728 | 					}
  729 | 					return bean;
  730 | 				}
  731 | 				else if (this.isDefaultName && !StringUtils.hasLength(this.mappedName)) {
  732 | 					throw new NoSuchBeanDefinitionException(this.beanName,
  733 | 							"Cannot resolve 'beanName' in local BeanFactory. Consider specifying a general 'name' value instead.");
  734 | 				}
  735 | 			}
  736 | 			// JNDI name lookup - may still go to a local BeanFactory.
  737 | 			return getResource(this, requestingBeanName);
  738 | 		}
  739 | 	}
  740 | 
  741 | 
  742 | 	/**
  743 | 	 * {@link BeanRegistrationAotContribution} to inject resources on fields and methods.
  744 | 	 */
  745 | 	private static class AotContribution implements BeanRegistrationAotContribution {
  746 | 
  747 | 		private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";
  748 | 
  749 | 		private static final String INSTANCE_PARAMETER = "instance";
  750 | 
  751 | 		private final Class<?> target;
  752 | 
  753 | 		private final Collection<LookupElement> lookupElements;
  754 | 
  755 | 		private final @Nullable AutowireCandidateResolver candidateResolver;
  756 | 
  757 | 		AotContribution(Class<?> target, Collection<LookupElement> lookupElements,
  758 | 				@Nullable AutowireCandidateResolver candidateResolver) {
  759 | 
  760 | 			this.target = target;
  761 | 			this.lookupElements = lookupElements;
  762 | 			this.candidateResolver = candidateResolver;
  763 | 		}
  764 | 
  765 | 		@Override
  766 | 		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
  767 | 			GeneratedClass generatedClass = generationContext.getGeneratedClasses()
  768 | 					.addForFeatureComponent("ResourceAutowiring", this.target, type -> {
  769 | 						type.addJavadoc("Resource autowiring for {@link $T}.", this.target);
  770 | 						type.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
  771 | 					});
  772 | 			GeneratedMethod generateMethod = generatedClass.getMethods().add("apply", method -> {
  773 | 				method.addJavadoc("Apply resource autowiring.");
  774 | 				method.addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC);
  775 | 				method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
  776 | 				method.addParameter(this.target, INSTANCE_PARAMETER);
  777 | 				method.returns(this.target);
  778 | 				method.addCode(generateMethodCode(generatedClass.getName(), generationContext.getRuntimeHints()));
  779 | 			});
  780 | 			beanRegistrationCode.addInstancePostProcessor(generateMethod.toMethodReference());
  781 | 			registerHints(generationContext.getRuntimeHints());
  782 | 		}
  783 | 
  784 | 		private CodeBlock generateMethodCode(ClassName targetClassName, RuntimeHints hints) {
  785 | 			CodeBlock.Builder code = CodeBlock.builder();
  786 | 			for (LookupElement lookupElement : this.lookupElements) {
  787 | 				code.addStatement(generateMethodStatementForElement(
  788 | 						targetClassName, lookupElement, hints));
  789 | 			}
  790 | 			code.addStatement("return $L", INSTANCE_PARAMETER);
  791 | 			return code.build();
  792 | 		}
  793 | 
  794 | 		private CodeBlock generateMethodStatementForElement(ClassName targetClassName,
  795 | 				LookupElement lookupElement, RuntimeHints hints) {
  796 | 
  797 | 			Member member = lookupElement.getMember();
  798 | 			if (member instanceof Field field) {
  799 | 				return generateMethodStatementForField(
  800 | 						targetClassName, field, lookupElement, hints);
  801 | 			}
  802 | 			if (member instanceof Method method) {
  803 | 				return generateMethodStatementForMethod(
  804 | 						targetClassName, method, lookupElement, hints);
  805 | 			}
  806 | 			throw new IllegalStateException(
  807 | 					"Unsupported member type " + member.getClass().getName());
  808 | 		}
  809 | 
  810 | 		private CodeBlock generateMethodStatementForField(ClassName targetClassName,
  811 | 				Field field, LookupElement lookupElement, RuntimeHints hints) {
  812 | 
  813 | 			hints.reflection().registerField(field);
  814 | 			CodeBlock resolver = generateFieldResolverCode(field, lookupElement);
  815 | 			AccessControl accessControl = AccessControl.forMember(field);
  816 | 			if (!accessControl.isAccessibleFrom(targetClassName)) {
  817 | 				return CodeBlock.of("$L.resolveAndSet($L, $L)", resolver,
  818 | 						REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
  819 | 			}
  820 | 			return CodeBlock.of("$L.$L = $L.resolve($L)", INSTANCE_PARAMETER,
  821 | 					field.getName(), resolver, REGISTERED_BEAN_PARAMETER);
  822 | 		}
  823 | 
  824 | 		private CodeBlock generateFieldResolverCode(Field field, LookupElement lookupElement) {
  825 | 			if (lookupElement.isDefaultName) {
  826 | 				return CodeBlock.of("$T.$L($S)", ResourceElementResolver.class,
  827 | 						"forField", field.getName());
  828 | 			}
  829 | 			else {
  830 | 				return CodeBlock.of("$T.$L($S, $S)", ResourceElementResolver.class,
  831 | 						"forField", field.getName(), lookupElement.getName());
  832 | 			}
  833 | 		}
  834 | 
  835 | 		private CodeBlock generateMethodStatementForMethod(ClassName targetClassName,
  836 | 				Method method, LookupElement lookupElement, RuntimeHints hints) {
  837 | 
  838 | 			CodeBlock resolver = generateMethodResolverCode(method, lookupElement);
  839 | 			AccessControl accessControl = AccessControl.forMember(method);
  840 | 			if (!accessControl.isAccessibleFrom(targetClassName)) {
  841 | 				hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
  842 | 				return CodeBlock.of("$L.resolveAndSet($L, $L)", resolver,
  843 | 						REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
  844 | 			}
  845 | 			hints.reflection().registerType(method.getDeclaringClass());
  846 | 			return CodeBlock.of("$L.$L($L.resolve($L))", INSTANCE_PARAMETER,
  847 | 					method.getName(), resolver, REGISTERED_BEAN_PARAMETER);
  848 | 
  849 | 		}
  850 | 
  851 | 		private CodeBlock generateMethodResolverCode(Method method, LookupElement lookupElement) {
  852 | 			if (lookupElement.isDefaultName) {
  853 | 				return CodeBlock.of("$T.$L($S, $T.class)", ResourceElementResolver.class,
  854 | 						"forMethod", method.getName(), lookupElement.getLookupType());
  855 | 			}
  856 | 			else {
  857 | 				return CodeBlock.of("$T.$L($S, $T.class, $S)", ResourceElementResolver.class,
  858 | 						"forMethod", method.getName(), lookupElement.getLookupType(), lookupElement.getName());
  859 | 			}
  860 | 		}
  861 | 
  862 | 		private void registerHints(RuntimeHints runtimeHints) {
  863 | 			this.lookupElements.forEach(lookupElement ->
  864 | 					registerProxyIfNecessary(runtimeHints, lookupElement.getDependencyDescriptor()));
  865 | 		}
  866 | 
  867 | 		private void registerProxyIfNecessary(RuntimeHints runtimeHints, DependencyDescriptor dependencyDescriptor) {
  868 | 			if (this.candidateResolver != null) {
  869 | 				Class<?> proxyClass =
  870 | 						this.candidateResolver.getLazyResolutionProxyClass(dependencyDescriptor, null);
  871 | 				if (proxyClass != null) {
  872 | 					ClassHintUtils.registerProxyIfNecessary(proxyClass, runtimeHints);
  873 | 				}
  874 | 			}
  875 | 		}
  876 | 	}
  877 | 
  878 | }
```
