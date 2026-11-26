<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/annotation/AutowiredAnnotationBeanPostProcessor.java -->
<!-- 正文引用：[[源码:AutowiredAnnotationBeanPostProcessor.java#L起-L止]] ；代码块头部：(src: AutowiredAnnotationBeanPostProcessor.java#L起-L止) -->

# AutowiredAnnotationBeanPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/annotation/AutowiredAnnotationBeanPostProcessor.java` |
| 行数 | 1089 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:19.284Z |

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
   17 | package org.springframework.beans.factory.annotation;
   18 | 
   19 | import java.beans.PropertyDescriptor;
   20 | import java.io.IOException;
   21 | import java.lang.annotation.Annotation;
   22 | import java.lang.reflect.AccessibleObject;
   23 | import java.lang.reflect.Constructor;
   24 | import java.lang.reflect.Field;
   25 | import java.lang.reflect.InvocationTargetException;
   26 | import java.lang.reflect.Member;
   27 | import java.lang.reflect.Method;
   28 | import java.lang.reflect.Modifier;
   29 | import java.util.ArrayList;
   30 | import java.util.Arrays;
   31 | import java.util.Collection;
   32 | import java.util.Iterator;
   33 | import java.util.LinkedHashSet;
   34 | import java.util.List;
   35 | import java.util.Map;
   36 | import java.util.Optional;
   37 | import java.util.Set;
   38 | import java.util.concurrent.ConcurrentHashMap;
   39 | 
   40 | import org.apache.commons.logging.Log;
   41 | import org.apache.commons.logging.LogFactory;
   42 | import org.jspecify.annotations.Nullable;
   43 | 
   44 | import org.springframework.aot.generate.AccessControl;
   45 | import org.springframework.aot.generate.GeneratedClass;
   46 | import org.springframework.aot.generate.GeneratedMethod;
   47 | import org.springframework.aot.generate.GenerationContext;
   48 | import org.springframework.aot.hint.ExecutableMode;
   49 | import org.springframework.aot.hint.RuntimeHints;
   50 | import org.springframework.aot.hint.support.ClassHintUtils;
   51 | import org.springframework.beans.BeanUtils;
   52 | import org.springframework.beans.BeansException;
   53 | import org.springframework.beans.PropertyValues;
   54 | import org.springframework.beans.TypeConverter;
   55 | import org.springframework.beans.factory.BeanCreationException;
   56 | import org.springframework.beans.factory.BeanFactory;
   57 | import org.springframework.beans.factory.BeanFactoryAware;
   58 | import org.springframework.beans.factory.InjectionPoint;
   59 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   60 | import org.springframework.beans.factory.UnsatisfiedDependencyException;
   61 | import org.springframework.beans.factory.aot.AutowiredArgumentsCodeGenerator;
   62 | import org.springframework.beans.factory.aot.AutowiredFieldValueResolver;
   63 | import org.springframework.beans.factory.aot.AutowiredMethodArgumentsResolver;
   64 | import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
   65 | import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
   66 | import org.springframework.beans.factory.aot.BeanRegistrationCode;
   67 | import org.springframework.beans.factory.aot.CodeWarnings;
   68 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   69 | import org.springframework.beans.factory.config.DependencyDescriptor;
   70 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   71 | import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
   72 | import org.springframework.beans.factory.support.AutowireCandidateResolver;
   73 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   74 | import org.springframework.beans.factory.support.LookupOverride;
   75 | import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
   76 | import org.springframework.beans.factory.support.RegisteredBean;
   77 | import org.springframework.beans.factory.support.RootBeanDefinition;
   78 | import org.springframework.core.BridgeMethodResolver;
   79 | import org.springframework.core.MethodParameter;
   80 | import org.springframework.core.Ordered;
   81 | import org.springframework.core.PriorityOrdered;
   82 | import org.springframework.core.annotation.AnnotationUtils;
   83 | import org.springframework.core.annotation.MergedAnnotation;
   84 | import org.springframework.core.annotation.MergedAnnotations;
   85 | import org.springframework.core.type.AnnotationMetadata;
   86 | import org.springframework.core.type.MethodMetadata;
   87 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   88 | import org.springframework.javapoet.ClassName;
   89 | import org.springframework.javapoet.CodeBlock;
   90 | import org.springframework.util.Assert;
   91 | import org.springframework.util.ClassUtils;
   92 | import org.springframework.util.CollectionUtils;
   93 | import org.springframework.util.ObjectUtils;
   94 | import org.springframework.util.ReflectionUtils;
   95 | import org.springframework.util.StringUtils;
   96 | 
   97 | /**
   98 |  * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}
   99 |  * implementation that autowires annotated fields, setter methods, and arbitrary
  100 |  * config methods. Such members to be injected are detected through annotations:
  101 |  * by default, Spring's {@link Autowired @Autowired} and {@link Value @Value}
  102 |  * annotations.
  103 |  *
  104 |  * <p>Also supports the common {@link jakarta.inject.Inject @Inject} annotation,
  105 |  * if available, as a direct alternative to Spring's own {@code @Autowired}.
  106 |  *
  107 |  * <h3>Autowired Constructors</h3>
  108 |  * <p>Only one constructor of any given bean class may declare this annotation with
  109 |  * the 'required' attribute set to {@code true}, indicating <i>the</i> constructor
  110 |  * to autowire when used as a Spring bean. Furthermore, if the 'required' attribute
  111 |  * is set to {@code true}, only a single constructor may be annotated with
  112 |  * {@code @Autowired}. If multiple <i>non-required</i> constructors declare the
  113 |  * annotation, they will be considered as candidates for autowiring. The constructor
  114 |  * with the greatest number of dependencies that can be satisfied by matching beans
  115 |  * in the Spring container will be chosen. If none of the candidates can be satisfied,
  116 |  * then a primary/default constructor (if present) will be used. If a class only
  117 |  * declares a single constructor to begin with, it will always be used, even if not
  118 |  * annotated. An annotated constructor does not have to be public.
  119 |  *
  120 |  * <h3>Autowired Fields</h3>
  121 |  * <p>Fields are injected right after construction of a bean, before any
  122 |  * config methods are invoked. Such a config field does not have to be public.
  123 |  *
  124 |  * <h3>Autowired Methods</h3>
  125 |  * <p>Config methods may have an arbitrary name and any number of arguments; each of
  126 |  * those arguments will be autowired with a matching bean in the Spring container.
  127 |  * Bean property setter methods are effectively just a special case of such a
  128 |  * general config method. Config methods do not have to be public.
  129 |  *
  130 |  * <h3>Annotation Config vs. XML Config</h3>
  131 |  * <p>A default {@code AutowiredAnnotationBeanPostProcessor} will be registered
  132 |  * by the "context:annotation-config" and "context:component-scan" XML tags.
  133 |  * Remove or turn off the default annotation configuration there if you intend
  134 |  * to specify a custom {@code AutowiredAnnotationBeanPostProcessor} bean definition.
  135 |  *
  136 |  * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
  137 |  * thus the latter configuration will override the former for properties wired through
  138 |  * both approaches.
  139 |  *
  140 |  * <h3>{@literal @}Lookup Methods</h3>
  141 |  * <p>In addition to regular injection points as discussed above, this post-processor
  142 |  * also handles Spring's {@link Lookup @Lookup} annotation which identifies lookup
  143 |  * methods to be replaced by the container at runtime. This is essentially a type-safe
  144 |  * version of {@code getBean(Class, args)} and {@code getBean(String, args)}.
  145 |  * See {@link Lookup @Lookup's javadoc} for details.
  146 |  *
  147 |  * @author Juergen Hoeller
  148 |  * @author Mark Fisher
  149 |  * @author Stephane Nicoll
  150 |  * @author Sebastien Deleuze
  151 |  * @author Sam Brannen
  152 |  * @author Phillip Webb
  153 |  * @since 2.5
  154 |  * @see #setAutowiredAnnotationType
  155 |  * @see Autowired
  156 |  * @see Value
  157 |  */
  158 | public class AutowiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
  159 | 		MergedBeanDefinitionPostProcessor, BeanRegistrationAotProcessor, PriorityOrdered, BeanFactoryAware {
  160 | 
  161 | 	private static final Constructor<?>[] EMPTY_CONSTRUCTOR_ARRAY = new Constructor<?>[0];
  162 | 
  163 | 
  164 | 	protected final Log logger = LogFactory.getLog(getClass());
  165 | 
  166 | 	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = CollectionUtils.newLinkedHashSet(4);
  167 | 
  168 | 	private String requiredParameterName = "required";
  169 | 
  170 | 	private boolean requiredParameterValue = true;
  171 | 
  172 | 	private int order = Ordered.LOWEST_PRECEDENCE - 2;
  173 | 
  174 | 	private @Nullable ConfigurableListableBeanFactory beanFactory;
  175 | 
  176 | 	private @Nullable MetadataReaderFactory metadataReaderFactory;
  177 | 
  178 | 	private final Set<String> lookupMethodsChecked = ConcurrentHashMap.newKeySet(256);
  179 | 
  180 | 	private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);
  181 | 
  182 | 	private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
  183 | 
  184 | 
  185 | 	/**
  186 | 	 * Create a new {@code AutowiredAnnotationBeanPostProcessor} for Spring's
  187 | 	 * standard {@link Autowired @Autowired} and {@link Value @Value} annotations.
  188 | 	 * <p>Also supports the common {@link jakarta.inject.Inject @Inject} annotation
  189 | 	 * if available.
  190 | 	 */
  191 | 	@SuppressWarnings("unchecked")
  192 | 	public AutowiredAnnotationBeanPostProcessor() {
  193 | 		this.autowiredAnnotationTypes.add(Autowired.class);
  194 | 		this.autowiredAnnotationTypes.add(Value.class);
  195 | 
  196 | 		ClassLoader classLoader = AutowiredAnnotationBeanPostProcessor.class.getClassLoader();
  197 | 		try {
  198 | 			this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
  199 | 					ClassUtils.forName("jakarta.inject.Inject", classLoader));
  200 | 			logger.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
  201 | 		}
  202 | 		catch (ClassNotFoundException ex) {
  203 | 			// jakarta.inject API not available - simply skip.
  204 | 		}
  205 | 	}
  206 | 
  207 | 
  208 | 	/**
  209 | 	 * Set the 'autowired' annotation type, to be used on constructors, fields,
  210 | 	 * setter methods, and arbitrary config methods.
  211 | 	 * <p>The default autowired annotation types are the Spring-provided
  212 | 	 * {@link Autowired @Autowired} and {@link Value @Value} annotations as well
  213 | 	 * as the common {@code @Inject} annotation, if available.
  214 | 	 * <p>This setter property exists so that developers can provide their own
  215 | 	 * (non-Spring-specific) annotation type to indicate that a member is supposed
  216 | 	 * to be autowired.
  217 | 	 */
  218 | 	public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
  219 | 		Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
  220 | 		this.autowiredAnnotationTypes.clear();
  221 | 		this.autowiredAnnotationTypes.add(autowiredAnnotationType);
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Set the 'autowired' annotation types, to be used on constructors, fields,
  226 | 	 * setter methods, and arbitrary config methods.
  227 | 	 * <p>The default autowired annotation types are the Spring-provided
  228 | 	 * {@link Autowired @Autowired} and {@link Value @Value} annotations as well
  229 | 	 * as the common {@code @Inject} annotation, if available.
  230 | 	 * <p>This setter property exists so that developers can provide their own
  231 | 	 * (non-Spring-specific) annotation types to indicate that a member is supposed
  232 | 	 * to be autowired.
  233 | 	 */
  234 | 	public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
  235 | 		Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
  236 | 		this.autowiredAnnotationTypes.clear();
  237 | 		this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
  238 | 	}
  239 | 
  240 | 	/**
  241 | 	 * Set the name of an attribute of the annotation that specifies whether it is required.
  242 | 	 * @see #setRequiredParameterValue(boolean)
  243 | 	 */
  244 | 	public void setRequiredParameterName(String requiredParameterName) {
  245 | 		this.requiredParameterName = requiredParameterName;
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Set the boolean value that marks a dependency as required.
  250 | 	 * <p>For example if using 'required=true' (the default), this value should be
  251 | 	 * {@code true}; but if using 'optional=false', this value should be {@code false}.
  252 | 	 * @see #setRequiredParameterName(String)
  253 | 	 */
  254 | 	public void setRequiredParameterValue(boolean requiredParameterValue) {
  255 | 		this.requiredParameterValue = requiredParameterValue;
  256 | 	}
  257 | 
  258 | 	public void setOrder(int order) {
  259 | 		this.order = order;
  260 | 	}
  261 | 
  262 | 	@Override
  263 | 	public int getOrder() {
  264 | 		return this.order;
  265 | 	}
  266 | 
  267 | 	@Override
  268 | 	public void setBeanFactory(BeanFactory beanFactory) {
  269 | 		if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
  270 | 			throw new IllegalArgumentException(
  271 | 					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
  272 | 		}
  273 | 		this.beanFactory = clbf;
  274 | 		this.metadataReaderFactory = MetadataReaderFactory.create(clbf.getBeanClassLoader());
  275 | 	}
  276 | 
  277 | 
  278 | 	@Override
  279 | 	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
  280 | 		// Register externally managed config members on bean definition.
  281 | 		findInjectionMetadata(beanName, beanType, beanDefinition);
  282 | 
  283 | 		// Use opportunity to clear caches which are not needed after singleton instantiation.
  284 | 		// The injectionMetadataCache itself is left intact since it cannot be reliably
  285 | 		// reconstructed in terms of externally managed config members otherwise.
  286 | 		if (beanDefinition.isSingleton()) {
  287 | 			this.candidateConstructorsCache.remove(beanType);
  288 | 			// With actual lookup overrides, keep it intact along with bean definition.
  289 | 			if (!beanDefinition.hasMethodOverrides()) {
  290 | 				this.lookupMethodsChecked.remove(beanName);
  291 | 			}
  292 | 		}
  293 | 	}
  294 | 
  295 | 	@Override
  296 | 	public void resetBeanDefinition(String beanName) {
  297 | 		this.lookupMethodsChecked.remove(beanName);
  298 | 		this.injectionMetadataCache.remove(beanName);
  299 | 	}
  300 | 
  301 | 	@Override
  302 | 	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
  303 | 		Class<?> beanClass = registeredBean.getBeanClass();
  304 | 		String beanName = registeredBean.getBeanName();
  305 | 		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
  306 | 		InjectionMetadata metadata = findInjectionMetadata(beanName, beanClass, beanDefinition);
  307 | 		Collection<AutowiredElement> autowiredElements = getAutowiredElements(metadata,
  308 | 				beanDefinition.getPropertyValues());
  309 | 		if (!ObjectUtils.isEmpty(autowiredElements)) {
  310 | 			return new AotContribution(beanClass, autowiredElements, getAutowireCandidateResolver());
  311 | 		}
  312 | 		return null;
  313 | 	}
  314 | 
  315 | 
  316 | 	@SuppressWarnings({ "rawtypes", "unchecked" })
  317 | 	private Collection<AutowiredElement> getAutowiredElements(InjectionMetadata metadata, PropertyValues propertyValues) {
  318 | 		return (Collection) metadata.getInjectedElements(propertyValues);
  319 | 	}
  320 | 
  321 | 	private @Nullable AutowireCandidateResolver getAutowireCandidateResolver() {
  322 | 		if (this.beanFactory instanceof DefaultListableBeanFactory lbf) {
  323 | 			return lbf.getAutowireCandidateResolver();
  324 | 		}
  325 | 		return null;
  326 | 	}
  327 | 
  328 | 	private InjectionMetadata findInjectionMetadata(String beanName, Class<?> beanType, RootBeanDefinition beanDefinition) {
  329 | 		InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
  330 | 		metadata.checkConfigMembers(beanDefinition);
  331 | 		return metadata;
  332 | 	}
  333 | 
  334 | 	@Override
  335 | 	public Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeanCreationException {
  336 | 		checkLookupMethods(beanClass, beanName);
  337 | 
  338 | 		// Pick up subclass with fresh lookup method override from above
  339 | 		if (this.beanFactory instanceof AbstractAutowireCapableBeanFactory aacBeanFactory) {
  340 | 			RootBeanDefinition mbd = (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(beanName);
  341 | 			if (mbd.getFactoryMethodName() == null && mbd.hasBeanClass()) {
  342 | 				return aacBeanFactory.getInstantiationStrategy().getActualBeanClass(mbd, beanName, aacBeanFactory);
  343 | 			}
  344 | 		}
  345 | 		return beanClass;
  346 | 	}
  347 | 
  348 | 	@Override
  349 | 	public Constructor<?> @Nullable [] determineCandidateConstructors(Class<?> beanClass, final String beanName)
  350 | 			throws BeanCreationException {
  351 | 
  352 | 		checkLookupMethods(beanClass, beanName);
  353 | 
  354 | 		// Quick check on the concurrent map first, with minimal locking.
  355 | 		Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
  356 | 		if (candidateConstructors == null) {
  357 | 			// Fully synchronized resolution now...
  358 | 			synchronized (this.candidateConstructorsCache) {
  359 | 				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
  360 | 				if (candidateConstructors == null) {
  361 | 					Constructor<?>[] rawCandidates;
  362 | 					try {
  363 | 						rawCandidates = beanClass.getDeclaredConstructors();
  364 | 					}
  365 | 					catch (Throwable ex) {
  366 | 						throw new BeanCreationException(beanName,
  367 | 								"Resolution of declared constructors on bean Class [" + beanClass.getName() +
  368 | 								"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
  369 | 					}
  370 | 					List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
  371 | 					Constructor<?> requiredConstructor = null;
  372 | 					Constructor<?> defaultConstructor = null;
  373 | 					Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
  374 | 					int nonSyntheticConstructors = 0;
  375 | 					for (Constructor<?> candidate : rawCandidates) {
  376 | 						if (!candidate.isSynthetic()) {
  377 | 							nonSyntheticConstructors++;
  378 | 						}
  379 | 						else if (primaryConstructor != null) {
  380 | 							continue;
  381 | 						}
  382 | 						MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
  383 | 						if (ann == null) {
  384 | 							Class<?> userClass = ClassUtils.getUserClass(beanClass);
  385 | 							if (userClass != beanClass) {
  386 | 								try {
  387 | 									Constructor<?> superCtor =
  388 | 											userClass.getDeclaredConstructor(candidate.getParameterTypes());
  389 | 									ann = findAutowiredAnnotation(superCtor);
  390 | 								}
  391 | 								catch (NoSuchMethodException ex) {
  392 | 									// Simply proceed, no equivalent superclass constructor found...
  393 | 								}
  394 | 							}
  395 | 						}
  396 | 						if (ann != null) {
  397 | 							if (requiredConstructor != null) {
  398 | 								throw new BeanCreationException(beanName,
  399 | 										"Invalid autowire-marked constructor: " + candidate +
  400 | 										". Found constructor with 'required' Autowired annotation already: " +
  401 | 										requiredConstructor);
  402 | 							}
  403 | 							boolean required = determineRequiredStatus(ann);
  404 | 							if (required) {
  405 | 								if (!candidates.isEmpty()) {
  406 | 									throw new BeanCreationException(beanName,
  407 | 											"Invalid autowire-marked constructors: " + candidates +
  408 | 											". Found constructor with 'required' Autowired annotation: " +
  409 | 											candidate);
  410 | 								}
  411 | 								requiredConstructor = candidate;
  412 | 							}
  413 | 							candidates.add(candidate);
  414 | 						}
  415 | 						else if (candidate.getParameterCount() == 0) {
  416 | 							defaultConstructor = candidate;
  417 | 						}
  418 | 					}
  419 | 					if (!candidates.isEmpty()) {
  420 | 						// Add default constructor to list of optional constructors, as fallback.
  421 | 						if (requiredConstructor == null) {
  422 | 							if (defaultConstructor != null) {
  423 | 								candidates.add(defaultConstructor);
  424 | 							}
  425 | 							else if (candidates.size() == 1 && logger.isInfoEnabled()) {
  426 | 								logger.info("Inconsistent constructor declaration on bean with name '" + beanName +
  427 | 										"': single autowire-marked constructor flagged as optional - " +
  428 | 										"this constructor is effectively required since there is no " +
  429 | 										"default constructor to fall back to: " + candidates.get(0));
  430 | 							}
  431 | 						}
  432 | 						candidateConstructors = candidates.toArray(EMPTY_CONSTRUCTOR_ARRAY);
  433 | 					}
  434 | 					else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
  435 | 						candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
  436 | 					}
  437 | 					else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
  438 | 							defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
  439 | 						candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
  440 | 					}
  441 | 					else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
  442 | 						candidateConstructors = new Constructor<?>[] {primaryConstructor};
  443 | 					}
  444 | 					else {
  445 | 						candidateConstructors = EMPTY_CONSTRUCTOR_ARRAY;
  446 | 					}
  447 | 					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
  448 | 				}
  449 | 			}
  450 | 		}
  451 | 		return (candidateConstructors.length > 0 ? candidateConstructors : null);
  452 | 	}
  453 | 
  454 | 	private void checkLookupMethods(Class<?> beanClass, final String beanName) throws BeanCreationException {
  455 | 		if (!this.lookupMethodsChecked.contains(beanName)) {
  456 | 			if (AnnotationUtils.isCandidateClass(beanClass, Lookup.class)) {
  457 | 				try {
  458 | 					Class<?> targetClass = beanClass;
  459 | 					do {
  460 | 						ReflectionUtils.doWithLocalMethods(targetClass, method -> {
  461 | 							Lookup lookup = method.getAnnotation(Lookup.class);
  462 | 							if (lookup != null) {
  463 | 								Assert.state(this.beanFactory != null, "No BeanFactory available");
  464 | 								LookupOverride override = new LookupOverride(method, lookup.value());
  465 | 								try {
  466 | 									RootBeanDefinition mbd = (RootBeanDefinition)
  467 | 											this.beanFactory.getMergedBeanDefinition(beanName);
  468 | 									mbd.getMethodOverrides().addOverride(override);
  469 | 								}
  470 | 								catch (NoSuchBeanDefinitionException ex) {
  471 | 									throw new BeanCreationException(beanName,
  472 | 											"Cannot apply @Lookup to beans without corresponding bean definition");
  473 | 								}
  474 | 							}
  475 | 						});
  476 | 						targetClass = targetClass.getSuperclass();
  477 | 					}
  478 | 					while (targetClass != null && targetClass != Object.class);
  479 | 
  480 | 				}
  481 | 				catch (IllegalStateException ex) {
  482 | 					throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
  483 | 				}
  484 | 			}
  485 | 			this.lookupMethodsChecked.add(beanName);
  486 | 		}
  487 | 	}
  488 | 
  489 | 	@Override
  490 | 	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
  491 | 		InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
  492 | 		try {
  493 | 			metadata.inject(bean, beanName, pvs);
  494 | 		}
  495 | 		catch (BeanCreationException ex) {
  496 | 			throw ex;
  497 | 		}
  498 | 		catch (Throwable ex) {
  499 | 			throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
  500 | 		}
  501 | 		return pvs;
  502 | 	}
  503 | 
  504 | 	/**
  505 | 	 * <em>Native</em> processing method for direct calls with an arbitrary target
  506 | 	 * instance, resolving all of its fields and methods which are annotated with
  507 | 	 * one of the configured 'autowired' annotation types.
  508 | 	 * @param bean the target instance to process
  509 | 	 * @throws BeanCreationException if autowiring failed
  510 | 	 * @see #setAutowiredAnnotationTypes(Set)
  511 | 	 */
  512 | 	public void processInjection(Object bean) throws BeanCreationException {
  513 | 		Class<?> clazz = bean.getClass();
  514 | 		InjectionMetadata metadata = findAutowiringMetadata(clazz.getName(), clazz, null);
  515 | 		try {
  516 | 			metadata.inject(bean, null, null);
  517 | 		}
  518 | 		catch (BeanCreationException ex) {
  519 | 			throw ex;
  520 | 		}
  521 | 		catch (Throwable ex) {
  522 | 			throw new BeanCreationException(
  523 | 					"Injection of autowired dependencies failed for class [" + clazz + "]", ex);
  524 | 		}
  525 | 	}
  526 | 
  527 | 	private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
  528 | 		// Fall back to class name as cache key, for backwards compatibility with custom callers.
  529 | 		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
  530 | 		// Quick check on the concurrent map first, with minimal locking.
  531 | 		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
  532 | 		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
  533 | 			synchronized (this.injectionMetadataCache) {
  534 | 				metadata = this.injectionMetadataCache.get(cacheKey);
  535 | 				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
  536 | 					if (metadata != null) {
  537 | 						metadata.clear(pvs);
  538 | 					}
  539 | 					metadata = buildAutowiringMetadata(clazz);
  540 | 					this.injectionMetadataCache.put(cacheKey, metadata);
  541 | 				}
  542 | 			}
  543 | 		}
  544 | 		return metadata;
  545 | 	}
  546 | 
  547 | 	private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
  548 | 		if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
  549 | 			return InjectionMetadata.EMPTY;
  550 | 		}
  551 | 
  552 | 		final List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
  553 | 		Class<?> targetClass = ClassUtils.getUserClass(clazz);
  554 | 
  555 | 		do {
  556 | 			final List<InjectionMetadata.InjectedElement> fieldElements = new ArrayList<>();
  557 | 			ReflectionUtils.doWithLocalFields(targetClass, field -> {
  558 | 				MergedAnnotation<?> ann = findAutowiredAnnotation(field);
  559 | 				if (ann != null) {
  560 | 					if (Modifier.isStatic(field.getModifiers())) {
  561 | 						if (logger.isInfoEnabled()) {
  562 | 							logger.info("Autowired annotation is not supported on static fields: " + field);
  563 | 						}
  564 | 						return;
  565 | 					}
  566 | 					boolean required = determineRequiredStatus(ann);
  567 | 					fieldElements.add(new AutowiredFieldElement(field, required));
  568 | 				}
  569 | 			});
  570 | 
  571 | 			final List<InjectionMetadata.InjectedElement> methodElements = new ArrayList<>();
  572 | 			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
  573 | 				if (method.isBridge()) {
  574 | 					return;
  575 | 				}
  576 | 				MergedAnnotation<?> ann = findAutowiredAnnotation(method);
  577 | 				if (ann != null && method.equals(BridgeMethodResolver.getMostSpecificMethod(method, clazz))) {
  578 | 					if (Modifier.isStatic(method.getModifiers())) {
  579 | 						if (logger.isInfoEnabled()) {
  580 | 							logger.info("Autowired annotation is not supported on static methods: " + method);
  581 | 						}
  582 | 						return;
  583 | 					}
  584 | 					if (method.getParameterCount() == 0) {
  585 | 						if (method.getDeclaringClass().isRecord()) {
  586 | 							// Annotations on the compact constructor arguments made available on accessors, ignoring.
  587 | 							return;
  588 | 						}
  589 | 						if (logger.isInfoEnabled()) {
  590 | 							logger.info("Autowired annotation should only be used on methods with parameters: " +
  591 | 									method);
  592 | 						}
  593 | 					}
  594 | 					boolean required = determineRequiredStatus(ann);
  595 | 					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method, clazz);
  596 | 					methodElements.add(new AutowiredMethodElement(method, required, pd));
  597 | 				}
  598 | 			});
  599 | 
  600 | 			elements.addAll(0, sortMethodElements(methodElements, targetClass));
  601 | 			elements.addAll(0, fieldElements);
  602 | 			targetClass = targetClass.getSuperclass();
  603 | 		}
  604 | 		while (targetClass != null && targetClass != Object.class);
  605 | 
  606 | 		return InjectionMetadata.forElements(elements, clazz);
  607 | 	}
  608 | 
  609 | 	private @Nullable MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
  610 | 		MergedAnnotations annotations = MergedAnnotations.from(ao);
  611 | 		for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
  612 | 			MergedAnnotation<?> annotation = annotations.get(type);
  613 | 			if (annotation.isPresent()) {
  614 | 				return annotation;
  615 | 			}
  616 | 		}
  617 | 		return null;
  618 | 	}
  619 | 
  620 | 	/**
  621 | 	 * Determine if the annotated field or method requires its dependency.
  622 | 	 * <p>A 'required' dependency means that autowiring should fail when no beans
  623 | 	 * are found. Otherwise, the autowiring process will simply bypass the field
  624 | 	 * or method when no beans are found.
  625 | 	 * @param ann a {@link MergedAnnotation} representing the Autowired annotation
  626 | 	 * @return whether the annotation indicates that a dependency is required
  627 | 	 */
  628 | 	protected boolean determineRequiredStatus(MergedAnnotation<?> ann) {
  629 | 		Optional<Boolean> requiredAttribute = ann.getValue(this.requiredParameterName, Boolean.class);
  630 | 		return (requiredAttribute.isEmpty() || this.requiredParameterValue == requiredAttribute.get());
  631 | 	}
  632 | 
  633 | 	/**
  634 | 	 * Sort the method elements via ASM for deterministic declaration order if possible.
  635 | 	 */
  636 | 	private List<InjectionMetadata.InjectedElement> sortMethodElements(
  637 | 			List<InjectionMetadata.InjectedElement> methodElements, Class<?> targetClass) {
  638 | 
  639 | 		if (this.metadataReaderFactory != null && methodElements.size() > 1) {
  640 | 			// Try reading the class file via ASM for deterministic declaration order...
  641 | 			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
  642 | 			// order, even between different runs of the same application on the same JVM.
  643 | 			try {
  644 | 				AnnotationMetadata asm =
  645 | 						this.metadataReaderFactory.getMetadataReader(targetClass.getName()).getAnnotationMetadata();
  646 | 				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Autowired.class.getName());
  647 | 				if (asmMethods.size() >= methodElements.size()) {
  648 | 					List<InjectionMetadata.InjectedElement> candidateMethods = new ArrayList<>(methodElements);
  649 | 					List<InjectionMetadata.InjectedElement> selectedMethods = new ArrayList<>(asmMethods.size());
  650 | 					for (MethodMetadata asmMethod : asmMethods) {
  651 | 						for (Iterator<InjectionMetadata.InjectedElement> it = candidateMethods.iterator(); it.hasNext();) {
  652 | 							InjectionMetadata.InjectedElement element = it.next();
  653 | 							if (element.getMember().getName().equals(asmMethod.getMethodName())) {
  654 | 								selectedMethods.add(element);
  655 | 								it.remove();
  656 | 								break;
  657 | 							}
  658 | 						}
  659 | 					}
  660 | 					if (selectedMethods.size() == methodElements.size()) {
  661 | 						// All reflection-detected methods found in ASM method set -> proceed
  662 | 						return selectedMethods;
  663 | 					}
  664 | 				}
  665 | 			}
  666 | 			catch (IOException ex) {
  667 | 				logger.debug("Failed to read class file via ASM for determining @Autowired method order", ex);
  668 | 				// No worries, let's continue with the reflection metadata we started with...
  669 | 			}
  670 | 		}
  671 | 		return methodElements;
  672 | 	}
  673 | 
  674 | 	/**
  675 | 	 * Register the specified bean as dependent on the autowired beans.
  676 | 	 */
  677 | 	private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
  678 | 		if (beanName != null) {
  679 | 			for (String autowiredBeanName : autowiredBeanNames) {
  680 | 				if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
  681 | 					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
  682 | 				}
  683 | 				if (logger.isTraceEnabled()) {
  684 | 					logger.trace("Autowiring by type from bean name '" + beanName +
  685 | 							"' to bean named '" + autowiredBeanName + "'");
  686 | 				}
  687 | 			}
  688 | 		}
  689 | 	}
  690 | 
  691 | 	/**
  692 | 	 * Resolve the specified cached method argument or field value.
  693 | 	 */
  694 | 	private @Nullable Object resolveCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
  695 | 		if (cachedArgument instanceof DependencyDescriptor descriptor) {
  696 | 			Assert.state(this.beanFactory != null, "No BeanFactory available");
  697 | 			return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
  698 | 		}
  699 | 		else {
  700 | 			return cachedArgument;
  701 | 		}
  702 | 	}
  703 | 
  704 | 
  705 | 	/**
  706 | 	 * Base class representing injection information.
  707 | 	 */
  708 | 	private abstract static class AutowiredElement extends InjectionMetadata.InjectedElement {
  709 | 
  710 | 		protected final boolean required;
  711 | 
  712 | 		protected AutowiredElement(Member member, @Nullable PropertyDescriptor pd, boolean required) {
  713 | 			super(member, pd);
  714 | 			this.required = required;
  715 | 		}
  716 | 	}
  717 | 
  718 | 
  719 | 	/**
  720 | 	 * Class representing injection information about an annotated field.
  721 | 	 */
  722 | 	private class AutowiredFieldElement extends AutowiredElement {
  723 | 
  724 | 		private volatile boolean cached;
  725 | 
  726 | 		private volatile @Nullable Object cachedFieldValue;
  727 | 
  728 | 		public AutowiredFieldElement(Field field, boolean required) {
  729 | 			super(field, null, required);
  730 | 		}
  731 | 
  732 | 		@Override
  733 | 		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
  734 | 			Field field = (Field) this.member;
  735 | 			Object value;
  736 | 			if (this.cached) {
  737 | 				try {
  738 | 					value = resolveCachedArgument(beanName, this.cachedFieldValue);
  739 | 				}
  740 | 				catch (BeansException ex) {
  741 | 					// Unexpected target bean mismatch for cached argument -> re-resolve
  742 | 					this.cached = false;
  743 | 					logger.debug("Failed to resolve cached argument", ex);
  744 | 					value = resolveFieldValue(field, bean, beanName);
  745 | 				}
  746 | 			}
  747 | 			else {
  748 | 				value = resolveFieldValue(field, bean, beanName);
  749 | 			}
  750 | 			if (value != null) {
  751 | 				ReflectionUtils.makeAccessible(field);
  752 | 				field.set(bean, value);
  753 | 			}
  754 | 		}
  755 | 
  756 | 		private @Nullable Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
  757 | 			DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
  758 | 			desc.setContainingClass(bean.getClass());
  759 | 			Set<String> autowiredBeanNames = new LinkedHashSet<>(2);
  760 | 			Assert.state(beanFactory != null, "No BeanFactory available");
  761 | 			TypeConverter typeConverter = beanFactory.getTypeConverter();
  762 | 			Object value;
  763 | 			try {
  764 | 				value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
  765 | 			}
  766 | 			catch (BeansException ex) {
  767 | 				throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
  768 | 			}
  769 | 			synchronized (this) {
  770 | 				if (!this.cached) {
  771 | 					if (value != null || this.required) {
  772 | 						Object cachedFieldValue = desc;
  773 | 						registerDependentBeans(beanName, autowiredBeanNames);
  774 | 						if (value != null && autowiredBeanNames.size() == 1) {
  775 | 							String autowiredBeanName = autowiredBeanNames.iterator().next();
  776 | 							if (beanFactory.containsBean(autowiredBeanName) &&
  777 | 									beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
  778 | 								cachedFieldValue = new ShortcutDependencyDescriptor(desc, autowiredBeanName);
  779 | 							}
  780 | 						}
  781 | 						this.cachedFieldValue = cachedFieldValue;
  782 | 						this.cached = true;
  783 | 					}
  784 | 					else {
  785 | 						this.cachedFieldValue = null;
  786 | 						// cached flag remains false
  787 | 					}
  788 | 				}
  789 | 			}
  790 | 			return value;
  791 | 		}
  792 | 	}
  793 | 
  794 | 
  795 | 	/**
  796 | 	 * Class representing injection information about an annotated method.
  797 | 	 */
  798 | 	private class AutowiredMethodElement extends AutowiredElement {
  799 | 
  800 | 		private volatile boolean cached;
  801 | 
  802 | 		private volatile Object @Nullable [] cachedMethodArguments;
  803 | 
  804 | 		public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
  805 | 			super(method, pd, required);
  806 | 		}
  807 | 
  808 | 		@Override
  809 | 		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
  810 | 			if (!shouldInject(pvs)) {
  811 | 				return;
  812 | 			}
  813 | 			Method method = (Method) this.member;
  814 | 			@Nullable Object[] arguments;
  815 | 			if (this.cached) {
  816 | 				try {
  817 | 					arguments = resolveCachedArguments(beanName, this.cachedMethodArguments);
  818 | 				}
  819 | 				catch (BeansException ex) {
  820 | 					// Unexpected target bean mismatch for cached argument -> re-resolve
  821 | 					this.cached = false;
  822 | 					logger.debug("Failed to resolve cached argument", ex);
  823 | 					arguments = resolveMethodArguments(method, bean, beanName);
  824 | 				}
  825 | 			}
  826 | 			else {
  827 | 				arguments = resolveMethodArguments(method, bean, beanName);
  828 | 			}
  829 | 			if (arguments != null) {
  830 | 				try {
  831 | 					ReflectionUtils.makeAccessible(method);
  832 | 					method.invoke(bean, arguments);
  833 | 				}
  834 | 				catch (InvocationTargetException ex) {
  835 | 					throw ex.getTargetException();
  836 | 				}
  837 | 			}
  838 | 		}
  839 | 
  840 | 		private @Nullable Object @Nullable [] resolveCachedArguments(@Nullable String beanName, Object @Nullable [] cachedMethodArguments) {
  841 | 			if (cachedMethodArguments == null) {
  842 | 				return null;
  843 | 			}
  844 | 			@Nullable Object[] arguments = new Object[cachedMethodArguments.length];
  845 | 			for (int i = 0; i < arguments.length; i++) {
  846 | 				arguments[i] = resolveCachedArgument(beanName, cachedMethodArguments[i]);
  847 | 			}
  848 | 			return arguments;
  849 | 		}
  850 | 
  851 | 		private @Nullable Object @Nullable [] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
  852 | 			int argumentCount = method.getParameterCount();
  853 | 			@Nullable Object[] arguments = new Object[argumentCount];
  854 | 			DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
  855 | 			Set<String> autowiredBeanNames = CollectionUtils.newLinkedHashSet(argumentCount);
  856 | 			Assert.state(beanFactory != null, "No BeanFactory available");
  857 | 			TypeConverter typeConverter = beanFactory.getTypeConverter();
  858 | 			for (int i = 0; i < arguments.length; i++) {
  859 | 				MethodParameter methodParam = new MethodParameter(method, i);
  860 | 				DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
  861 | 				currDesc.setContainingClass(bean.getClass());
  862 | 				descriptors[i] = currDesc;
  863 | 				try {
  864 | 					Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeanNames, typeConverter);
  865 | 					if (arg == null && !this.required && !methodParam.isOptional()) {
  866 | 						arguments = null;
  867 | 						break;
  868 | 					}
  869 | 					arguments[i] = arg;
  870 | 				}
  871 | 				catch (BeansException ex) {
  872 | 					throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
  873 | 				}
  874 | 			}
  875 | 			synchronized (this) {
  876 | 				if (!this.cached) {
  877 | 					if (arguments != null) {
  878 | 						DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, argumentCount);
  879 | 						registerDependentBeans(beanName, autowiredBeanNames);
  880 | 						if (autowiredBeanNames.size() == argumentCount) {
  881 | 							Iterator<String> it = autowiredBeanNames.iterator();
  882 | 							Class<?>[] paramTypes = method.getParameterTypes();
  883 | 							for (int i = 0; i < paramTypes.length; i++) {
  884 | 								String autowiredBeanName = it.next();
  885 | 								if (arguments[i] != null && beanFactory.containsBean(autowiredBeanName) &&
  886 | 										beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
  887 | 									cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
  888 | 											descriptors[i], autowiredBeanName);
  889 | 								}
  890 | 							}
  891 | 						}
  892 | 						this.cachedMethodArguments = cachedMethodArguments;
  893 | 						this.cached = true;
  894 | 					}
  895 | 					else {
  896 | 						this.cachedMethodArguments = null;
  897 | 						// cached flag remains false
  898 | 					}
  899 | 				}
  900 | 			}
  901 | 			return arguments;
  902 | 		}
  903 | 	}
  904 | 
  905 | 
  906 | 	/**
  907 | 	 * DependencyDescriptor variant with a pre-resolved target bean name.
  908 | 	 */
  909 | 	@SuppressWarnings("serial")
  910 | 	private static class ShortcutDependencyDescriptor extends DependencyDescriptor {
  911 | 
  912 | 		private final String shortcut;
  913 | 
  914 | 		public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut) {
  915 | 			super(original);
  916 | 			this.shortcut = shortcut;
  917 | 		}
  918 | 
  919 | 		@Override
  920 | 		public Object resolveShortcut(BeanFactory beanFactory) {
  921 | 			return beanFactory.getBean(this.shortcut, getDependencyType());
  922 | 		}
  923 | 	}
  924 | 
  925 | 
  926 | 	/**
  927 | 	 * {@link BeanRegistrationAotContribution} to autowire fields and methods.
  928 | 	 */
  929 | 	private static class AotContribution implements BeanRegistrationAotContribution {
  930 | 
  931 | 		private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";
  932 | 
  933 | 		private static final String INSTANCE_PARAMETER = "instance";
  934 | 
  935 | 		private final Class<?> target;
  936 | 
  937 | 		private final Collection<AutowiredElement> autowiredElements;
  938 | 
  939 | 		private final @Nullable AutowireCandidateResolver candidateResolver;
  940 | 
  941 | 		AotContribution(Class<?> target, Collection<AutowiredElement> autowiredElements,
  942 | 				@Nullable AutowireCandidateResolver candidateResolver) {
  943 | 
  944 | 			this.target = target;
  945 | 			this.autowiredElements = autowiredElements;
  946 | 			this.candidateResolver = candidateResolver;
  947 | 		}
  948 | 
  949 | 		@Override
  950 | 		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
  951 | 			GeneratedClass generatedClass = generationContext.getGeneratedClasses()
  952 | 					.addForFeatureComponent("Autowiring", this.target, type -> {
  953 | 						type.addJavadoc("Autowiring for {@link $T}.", this.target);
  954 | 						type.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
  955 | 					});
  956 | 			GeneratedMethod generateMethod = generatedClass.getMethods().add("apply", method -> {
  957 | 				method.addJavadoc("Apply the autowiring.");
  958 | 				method.addModifiers(javax.lang.model.element.Modifier.PUBLIC,
  959 | 						javax.lang.model.element.Modifier.STATIC);
  960 | 				method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
  961 | 				method.addParameter(this.target, INSTANCE_PARAMETER);
  962 | 				method.returns(this.target);
  963 | 				CodeWarnings codeWarnings = new CodeWarnings();
  964 | 				codeWarnings.detectDeprecation(this.target);
  965 | 				method.addCode(generateMethodCode(codeWarnings,
  966 | 						generatedClass.getName(), generationContext.getRuntimeHints()));
  967 | 				codeWarnings.suppress(method);
  968 | 			});
  969 | 			beanRegistrationCode.addInstancePostProcessor(generateMethod.toMethodReference());
  970 | 
  971 | 			if (this.candidateResolver != null) {
  972 | 				registerHints(generationContext.getRuntimeHints());
  973 | 			}
  974 | 		}
  975 | 
  976 | 		private CodeBlock generateMethodCode(CodeWarnings codeWarnings,
  977 | 				ClassName targetClassName, RuntimeHints hints) {
  978 | 
  979 | 			CodeBlock.Builder code = CodeBlock.builder();
  980 | 			for (AutowiredElement autowiredElement : this.autowiredElements) {
  981 | 				code.addStatement(generateMethodStatementForElement(
  982 | 						codeWarnings, targetClassName, autowiredElement, hints));
  983 | 			}
  984 | 			code.addStatement("return $L", INSTANCE_PARAMETER);
  985 | 			return code.build();
  986 | 		}
  987 | 
  988 | 		private CodeBlock generateMethodStatementForElement(CodeWarnings codeWarnings,
  989 | 				ClassName targetClassName, AutowiredElement autowiredElement, RuntimeHints hints) {
  990 | 
  991 | 			Member member = autowiredElement.getMember();
  992 | 			boolean required = autowiredElement.required;
  993 | 			if (member instanceof Field field) {
  994 | 				return generateMethodStatementForField(
  995 | 						codeWarnings, targetClassName, field, required, hints);
  996 | 			}
  997 | 			if (member instanceof Method method) {
  998 | 				return generateMethodStatementForMethod(
  999 | 						codeWarnings, targetClassName, method, required, hints);
 1000 | 			}
 1001 | 			throw new IllegalStateException(
 1002 | 					"Unsupported member type " + member.getClass().getName());
 1003 | 		}
 1004 | 
 1005 | 		private CodeBlock generateMethodStatementForField(CodeWarnings codeWarnings,
 1006 | 				ClassName targetClassName, Field field, boolean required, RuntimeHints hints) {
 1007 | 
 1008 | 			hints.reflection().registerField(field);
 1009 | 			CodeBlock resolver = CodeBlock.of("$T.$L($S)",
 1010 | 					AutowiredFieldValueResolver.class,
 1011 | 					(!required ? "forField" : "forRequiredField"), field.getName());
 1012 | 			AccessControl accessControl = AccessControl.forMember(field);
 1013 | 			if (!accessControl.isAccessibleFrom(targetClassName)) {
 1014 | 				return CodeBlock.of("$L.resolveAndSet($L, $L)", resolver,
 1015 | 						REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
 1016 | 			}
 1017 | 			else {
 1018 | 				codeWarnings.detectDeprecation(field);
 1019 | 				return CodeBlock.of("$L.$L = $L.resolve($L)", INSTANCE_PARAMETER,
 1020 | 						field.getName(), resolver, REGISTERED_BEAN_PARAMETER);
 1021 | 			}
 1022 | 		}
 1023 | 
 1024 | 		private CodeBlock generateMethodStatementForMethod(CodeWarnings codeWarnings,
 1025 | 				ClassName targetClassName, Method method, boolean required, RuntimeHints hints) {
 1026 | 
 1027 | 			CodeBlock.Builder code = CodeBlock.builder();
 1028 | 			code.add("$T.$L", AutowiredMethodArgumentsResolver.class,
 1029 | 					(!required ? "forMethod" : "forRequiredMethod"));
 1030 | 			code.add("($S", method.getName());
 1031 | 			if (method.getParameterCount() > 0) {
 1032 | 				codeWarnings.detectDeprecation(method.getParameterTypes());
 1033 | 				code.add(", $L", generateParameterTypesCode(method.getParameterTypes()));
 1034 | 			}
 1035 | 			code.add(")");
 1036 | 			AccessControl accessControl = AccessControl.forMember(method);
 1037 | 			if (!accessControl.isAccessibleFrom(targetClassName)) {
 1038 | 				hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
 1039 | 				code.add(".resolveAndInvoke($L, $L)", REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
 1040 | 			}
 1041 | 			else {
 1042 | 				codeWarnings.detectDeprecation(method);
 1043 | 				hints.reflection().registerType(method.getDeclaringClass());
 1044 | 				CodeBlock arguments = new AutowiredArgumentsCodeGenerator(this.target,
 1045 | 						method).generateCode(method.getParameterTypes());
 1046 | 				CodeBlock injectionCode = CodeBlock.of("args -> $L.$L($L)",
 1047 | 						INSTANCE_PARAMETER, method.getName(), arguments);
 1048 | 				code.add(".resolve($L, $L)", REGISTERED_BEAN_PARAMETER, injectionCode);
 1049 | 			}
 1050 | 			return code.build();
 1051 | 		}
 1052 | 
 1053 | 		private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes) {
 1054 | 			return CodeBlock.join(Arrays.stream(parameterTypes)
 1055 | 					.map(parameterType -> CodeBlock.of("$T.class", parameterType))
 1056 | 					.toList(), ", ");
 1057 | 		}
 1058 | 
 1059 | 		private void registerHints(RuntimeHints runtimeHints) {
 1060 | 			this.autowiredElements.forEach(autowiredElement -> {
 1061 | 				boolean required = autowiredElement.required;
 1062 | 				Member member = autowiredElement.getMember();
 1063 | 				if (member instanceof Field field) {
 1064 | 					DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(field, required);
 1065 | 					registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
 1066 | 				}
 1067 | 				if (member instanceof Method method) {
 1068 | 					Class<?>[] parameterTypes = method.getParameterTypes();
 1069 | 					for (int i = 0; i < parameterTypes.length; i++) {
 1070 | 						MethodParameter methodParam = new MethodParameter(method, i);
 1071 | 						DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(methodParam, required);
 1072 | 						registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
 1073 | 					}
 1074 | 				}
 1075 | 			});
 1076 | 		}
 1077 | 
 1078 | 		private void registerProxyIfNecessary(RuntimeHints runtimeHints, DependencyDescriptor dependencyDescriptor) {
 1079 | 			if (this.candidateResolver != null) {
 1080 | 				Class<?> proxyClass =
 1081 | 						this.candidateResolver.getLazyResolutionProxyClass(dependencyDescriptor, null);
 1082 | 				if (proxyClass != null) {
 1083 | 					ClassHintUtils.registerProxyIfNecessary(proxyClass, runtimeHints);
 1084 | 				}
 1085 | 			}
 1086 | 		}
 1087 | 	}
 1088 | 
 1089 | }
```
