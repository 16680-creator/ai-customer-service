<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/annotation/InitDestroyAnnotationBeanPostProcessor.java -->
<!-- 正文引用：[[源码:InitDestroyAnnotationBeanPostProcessor.java#L起-L止]] ；代码块头部：(src: InitDestroyAnnotationBeanPostProcessor.java#L起-L止) -->

# InitDestroyAnnotationBeanPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/annotation/InitDestroyAnnotationBeanPostProcessor.java` |
| 行数 | 485 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:58.494Z |

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
   19 | import java.io.IOException;
   20 | import java.io.ObjectInputStream;
   21 | import java.io.Serializable;
   22 | import java.lang.annotation.Annotation;
   23 | import java.lang.reflect.InvocationTargetException;
   24 | import java.lang.reflect.Method;
   25 | import java.lang.reflect.Modifier;
   26 | import java.util.ArrayList;
   27 | import java.util.Collection;
   28 | import java.util.Collections;
   29 | import java.util.LinkedHashSet;
   30 | import java.util.List;
   31 | import java.util.Map;
   32 | import java.util.Set;
   33 | import java.util.concurrent.ConcurrentHashMap;
   34 | import java.util.stream.Stream;
   35 | 
   36 | import org.apache.commons.logging.Log;
   37 | import org.apache.commons.logging.LogFactory;
   38 | import org.jspecify.annotations.Nullable;
   39 | 
   40 | import org.springframework.beans.BeansException;
   41 | import org.springframework.beans.factory.BeanCreationException;
   42 | import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
   43 | import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
   44 | import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
   45 | import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
   46 | import org.springframework.beans.factory.support.RegisteredBean;
   47 | import org.springframework.beans.factory.support.RootBeanDefinition;
   48 | import org.springframework.core.Ordered;
   49 | import org.springframework.core.PriorityOrdered;
   50 | import org.springframework.core.annotation.AnnotationUtils;
   51 | import org.springframework.util.ClassUtils;
   52 | import org.springframework.util.CollectionUtils;
   53 | import org.springframework.util.ReflectionUtils;
   54 | 
   55 | /**
   56 |  * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
   57 |  * that invokes annotated init and destroy methods. Allows for an annotation
   58 |  * alternative to Spring's {@link org.springframework.beans.factory.InitializingBean}
   59 |  * and {@link org.springframework.beans.factory.DisposableBean} callback interfaces.
   60 |  *
   61 |  * <p>The actual annotation types that this post-processor checks for can be
   62 |  * configured through the {@link #setInitAnnotationType "initAnnotationType"}
   63 |  * and {@link #setDestroyAnnotationType "destroyAnnotationType"} properties.
   64 |  * Any custom annotation can be used, since there are no required annotation
   65 |  * attributes.
   66 |  *
   67 |  * <p>Init and destroy annotations may be applied to methods of any visibility:
   68 |  * public, package-protected, protected, or private. Multiple such methods
   69 |  * may be annotated, but it is recommended to only annotate one single
   70 |  * init method and destroy method, respectively.
   71 |  *
   72 |  * <p>Spring's {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}
   73 |  * supports the {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy}
   74 |  * annotations out of the box, as init annotation and destroy annotation, respectively.
   75 |  * Furthermore, it also supports the {@link jakarta.annotation.Resource} annotation
   76 |  * for annotation-driven injection of named beans.
   77 |  *
   78 |  * @author Juergen Hoeller
   79 |  * @author Stephane Nicoll
   80 |  * @author Phillip Webb
   81 |  * @author Sam Brannen
   82 |  * @since 2.5
   83 |  * @see #setInitAnnotationType
   84 |  * @see #setDestroyAnnotationType
   85 |  */
   86 | @SuppressWarnings("serial")
   87 | public class InitDestroyAnnotationBeanPostProcessor implements DestructionAwareBeanPostProcessor,
   88 | 		MergedBeanDefinitionPostProcessor, BeanRegistrationAotProcessor, PriorityOrdered, Serializable {
   89 | 
   90 | 	private final transient LifecycleMetadata emptyLifecycleMetadata =
   91 | 			new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList()) {
   92 | 				@Override
   93 | 				public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
   94 | 				}
   95 | 				@Override
   96 | 				public void invokeInitMethods(Object target, String beanName) {
   97 | 				}
   98 | 				@Override
   99 | 				public void invokeDestroyMethods(Object target, String beanName) {
  100 | 				}
  101 | 				@Override
  102 | 				public boolean hasDestroyMethods() {
  103 | 					return false;
  104 | 				}
  105 | 			};
  106 | 
  107 | 
  108 | 	protected transient Log logger = LogFactory.getLog(getClass());
  109 | 
  110 | 	private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);
  111 | 
  112 | 	private final Set<Class<? extends Annotation>> destroyAnnotationTypes = new LinkedHashSet<>(2);
  113 | 
  114 | 	private int order = Ordered.LOWEST_PRECEDENCE;
  115 | 
  116 | 	private final transient @Nullable Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);
  117 | 
  118 | 
  119 | 	/**
  120 | 	 * Specify the init annotation to check for, indicating initialization
  121 | 	 * methods to call after configuration of a bean.
  122 | 	 * <p>Any custom annotation can be used, since there are no required
  123 | 	 * annotation attributes. There is no default, although a typical choice
  124 | 	 * is the {@link jakarta.annotation.PostConstruct} annotation.
  125 | 	 * @see #addInitAnnotationType
  126 | 	 */
  127 | 	public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
  128 | 		this.initAnnotationTypes.clear();
  129 | 		this.initAnnotationTypes.add(initAnnotationType);
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Add an init annotation to check for, indicating initialization
  134 | 	 * methods to call after configuration of a bean.
  135 | 	 * @since 6.0.11
  136 | 	 * @see #setInitAnnotationType
  137 | 	 */
  138 | 	public void addInitAnnotationType(@Nullable Class<? extends Annotation> initAnnotationType) {
  139 | 		if (initAnnotationType != null) {
  140 | 			this.initAnnotationTypes.add(initAnnotationType);
  141 | 		}
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Specify the destroy annotation to check for, indicating destruction
  146 | 	 * methods to call when the context is shutting down.
  147 | 	 * <p>Any custom annotation can be used, since there are no required
  148 | 	 * annotation attributes. There is no default, although a typical choice
  149 | 	 * is the {@link jakarta.annotation.PreDestroy} annotation.
  150 | 	 * @see #addDestroyAnnotationType
  151 | 	 */
  152 | 	public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
  153 | 		this.destroyAnnotationTypes.clear();
  154 | 		this.destroyAnnotationTypes.add(destroyAnnotationType);
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Add a destroy annotation to check for, indicating destruction
  159 | 	 * methods to call when the context is shutting down.
  160 | 	 * @since 6.0.11
  161 | 	 * @see #setDestroyAnnotationType
  162 | 	 */
  163 | 	public void addDestroyAnnotationType(@Nullable Class<? extends Annotation> destroyAnnotationType) {
  164 | 		if (destroyAnnotationType != null) {
  165 | 			this.destroyAnnotationTypes.add(destroyAnnotationType);
  166 | 		}
  167 | 	}
  168 | 
  169 | 	public void setOrder(int order) {
  170 | 		this.order = order;
  171 | 	}
  172 | 
  173 | 	@Override
  174 | 	public int getOrder() {
  175 | 		return this.order;
  176 | 	}
  177 | 
  178 | 
  179 | 	@Override
  180 | 	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanClass, String beanName) {
  181 | 		findLifecycleMetadata(beanDefinition, beanClass);
  182 | 	}
  183 | 
  184 | 	@Override
  185 | 	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
  186 | 		RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
  187 | 		beanDefinition.resolveDestroyMethodIfNecessary();
  188 | 		LifecycleMetadata metadata = findLifecycleMetadata(beanDefinition, registeredBean.getBeanClass());
  189 | 		if (!CollectionUtils.isEmpty(metadata.initMethods)) {
  190 | 			String[] initMethodNames = safeMerge(beanDefinition.getInitMethodNames(), metadata.initMethods);
  191 | 			beanDefinition.setInitMethodNames(initMethodNames);
  192 | 		}
  193 | 		if (!CollectionUtils.isEmpty(metadata.destroyMethods)) {
  194 | 			String[] destroyMethodNames = safeMerge(beanDefinition.getDestroyMethodNames(), metadata.destroyMethods);
  195 | 			beanDefinition.setDestroyMethodNames(destroyMethodNames);
  196 | 		}
  197 | 		return null;
  198 | 	}
  199 | 
  200 | 	private LifecycleMetadata findLifecycleMetadata(RootBeanDefinition beanDefinition, Class<?> beanClass) {
  201 | 		LifecycleMetadata metadata = findLifecycleMetadata(beanClass);
  202 | 		metadata.checkInitDestroyMethods(beanDefinition);
  203 | 		return metadata;
  204 | 	}
  205 | 
  206 | 	private static String[] safeMerge(String @Nullable [] existingNames, Collection<LifecycleMethod> detectedMethods) {
  207 | 		Stream<String> detectedNames = detectedMethods.stream().map(LifecycleMethod::getIdentifier);
  208 | 		Stream<String> mergedNames = (existingNames != null ?
  209 | 				Stream.concat(detectedNames, Stream.of(existingNames)) : detectedNames);
  210 | 		return mergedNames.distinct().toArray(String[]::new);
  211 | 	}
  212 | 
  213 | 	@Override
  214 | 	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
  215 | 		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
  216 | 		try {
  217 | 			metadata.invokeInitMethods(bean, beanName);
  218 | 		}
  219 | 		catch (InvocationTargetException ex) {
  220 | 			throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
  221 | 		}
  222 | 		catch (Throwable ex) {
  223 | 			throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
  224 | 		}
  225 | 		return bean;
  226 | 	}
  227 | 
  228 | 	@Override
  229 | 	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
  230 | 		return bean;
  231 | 	}
  232 | 
  233 | 	@Override
  234 | 	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
  235 | 		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
  236 | 		try {
  237 | 			metadata.invokeDestroyMethods(bean, beanName);
  238 | 		}
  239 | 		catch (InvocationTargetException ex) {
  240 | 			String msg = "Destroy method on bean with name '" + beanName + "' threw an exception";
  241 | 			if (logger.isDebugEnabled()) {
  242 | 				logger.warn(msg, ex.getTargetException());
  243 | 			}
  244 | 			else if (logger.isWarnEnabled()) {
  245 | 				logger.warn(msg + ": " + ex.getTargetException());
  246 | 			}
  247 | 		}
  248 | 		catch (Throwable ex) {
  249 | 			if (logger.isWarnEnabled()) {
  250 | 				logger.warn("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
  251 | 			}
  252 | 		}
  253 | 	}
  254 | 
  255 | 	@Override
  256 | 	public boolean requiresDestruction(Object bean) {
  257 | 		return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
  258 | 	}
  259 | 
  260 | 
  261 | 	private LifecycleMetadata findLifecycleMetadata(Class<?> beanClass) {
  262 | 		if (this.lifecycleMetadataCache == null) {
  263 | 			// Happens after deserialization, during destruction...
  264 | 			return buildLifecycleMetadata(beanClass);
  265 | 		}
  266 | 		// Quick check on the concurrent map first, with minimal locking.
  267 | 		LifecycleMetadata metadata = this.lifecycleMetadataCache.get(beanClass);
  268 | 		if (metadata == null) {
  269 | 			synchronized (this.lifecycleMetadataCache) {
  270 | 				metadata = this.lifecycleMetadataCache.get(beanClass);
  271 | 				if (metadata == null) {
  272 | 					metadata = buildLifecycleMetadata(beanClass);
  273 | 					this.lifecycleMetadataCache.put(beanClass, metadata);
  274 | 				}
  275 | 				return metadata;
  276 | 			}
  277 | 		}
  278 | 		return metadata;
  279 | 	}
  280 | 
  281 | 	private LifecycleMetadata buildLifecycleMetadata(final Class<?> beanClass) {
  282 | 		if (!AnnotationUtils.isCandidateClass(beanClass, this.initAnnotationTypes) &&
  283 | 				!AnnotationUtils.isCandidateClass(beanClass, this.destroyAnnotationTypes)) {
  284 | 			return this.emptyLifecycleMetadata;
  285 | 		}
  286 | 
  287 | 		List<LifecycleMethod> initMethods = new ArrayList<>();
  288 | 		List<LifecycleMethod> destroyMethods = new ArrayList<>();
  289 | 		Class<?> currentClass = beanClass;
  290 | 
  291 | 		do {
  292 | 			final List<LifecycleMethod> currInitMethods = new ArrayList<>();
  293 | 			final List<LifecycleMethod> currDestroyMethods = new ArrayList<>();
  294 | 
  295 | 			ReflectionUtils.doWithLocalMethods(currentClass, method -> {
  296 | 				for (Class<? extends Annotation> initAnnotationType : this.initAnnotationTypes) {
  297 | 					if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
  298 | 						currInitMethods.add(new LifecycleMethod(method, beanClass));
  299 | 						if (logger.isTraceEnabled()) {
  300 | 							logger.trace("Found init method on class [" + beanClass.getName() + "]: " + method);
  301 | 						}
  302 | 					}
  303 | 				}
  304 | 				for (Class<? extends Annotation> destroyAnnotationType : this.destroyAnnotationTypes) {
  305 | 					if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
  306 | 						currDestroyMethods.add(new LifecycleMethod(method, beanClass));
  307 | 						if (logger.isTraceEnabled()) {
  308 | 							logger.trace("Found destroy method on class [" + beanClass.getName() + "]: " + method);
  309 | 						}
  310 | 					}
  311 | 				}
  312 | 			});
  313 | 
  314 | 			initMethods.addAll(0, currInitMethods);
  315 | 			destroyMethods.addAll(currDestroyMethods);
  316 | 			currentClass = currentClass.getSuperclass();
  317 | 		}
  318 | 		while (currentClass != null && currentClass != Object.class);
  319 | 
  320 | 		return (initMethods.isEmpty() && destroyMethods.isEmpty() ? this.emptyLifecycleMetadata :
  321 | 				new LifecycleMetadata(beanClass, initMethods, destroyMethods));
  322 | 	}
  323 | 
  324 | 
  325 | 	//---------------------------------------------------------------------
  326 | 	// Serialization support
  327 | 	//---------------------------------------------------------------------
  328 | 
  329 | 	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
  330 | 		// Rely on default serialization; just initialize state after deserialization.
  331 | 		ois.defaultReadObject();
  332 | 
  333 | 		// Initialize transient fields.
  334 | 		this.logger = LogFactory.getLog(getClass());
  335 | 	}
  336 | 
  337 | 
  338 | 	/**
  339 | 	 * Class representing information about annotated init and destroy methods.
  340 | 	 */
  341 | 	private class LifecycleMetadata {
  342 | 
  343 | 		private final Class<?> beanClass;
  344 | 
  345 | 		private final Collection<LifecycleMethod> initMethods;
  346 | 
  347 | 		private final Collection<LifecycleMethod> destroyMethods;
  348 | 
  349 | 		private volatile @Nullable Set<LifecycleMethod> checkedInitMethods;
  350 | 
  351 | 		private volatile @Nullable Set<LifecycleMethod> checkedDestroyMethods;
  352 | 
  353 | 		public LifecycleMetadata(Class<?> beanClass, Collection<LifecycleMethod> initMethods,
  354 | 				Collection<LifecycleMethod> destroyMethods) {
  355 | 
  356 | 			this.beanClass = beanClass;
  357 | 			this.initMethods = initMethods;
  358 | 			this.destroyMethods = destroyMethods;
  359 | 		}
  360 | 
  361 | 		public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
  362 | 			Set<LifecycleMethod> checkedInitMethods = CollectionUtils.newLinkedHashSet(this.initMethods.size());
  363 | 			for (LifecycleMethod lifecycleMethod : this.initMethods) {
  364 | 				String methodIdentifier = lifecycleMethod.getIdentifier();
  365 | 				if (!beanDefinition.isExternallyManagedInitMethod(methodIdentifier)) {
  366 | 					beanDefinition.registerExternallyManagedInitMethod(methodIdentifier);
  367 | 					checkedInitMethods.add(lifecycleMethod);
  368 | 					if (logger.isTraceEnabled()) {
  369 | 						logger.trace("Registered init method on class [" + this.beanClass.getName() + "]: " + methodIdentifier);
  370 | 					}
  371 | 				}
  372 | 			}
  373 | 			Set<LifecycleMethod> checkedDestroyMethods = CollectionUtils.newLinkedHashSet(this.destroyMethods.size());
  374 | 			for (LifecycleMethod lifecycleMethod : this.destroyMethods) {
  375 | 				String methodIdentifier = lifecycleMethod.getIdentifier();
  376 | 				if (!beanDefinition.isExternallyManagedDestroyMethod(methodIdentifier)) {
  377 | 					beanDefinition.registerExternallyManagedDestroyMethod(methodIdentifier);
  378 | 					checkedDestroyMethods.add(lifecycleMethod);
  379 | 					if (logger.isTraceEnabled()) {
  380 | 						logger.trace("Registered destroy method on class [" + this.beanClass.getName() + "]: " + methodIdentifier);
  381 | 					}
  382 | 				}
  383 | 			}
  384 | 			this.checkedInitMethods = checkedInitMethods;
  385 | 			this.checkedDestroyMethods = checkedDestroyMethods;
  386 | 		}
  387 | 
  388 | 		public void invokeInitMethods(Object target, String beanName) throws Throwable {
  389 | 			Collection<LifecycleMethod> checkedInitMethods = this.checkedInitMethods;
  390 | 			Collection<LifecycleMethod> initMethodsToIterate =
  391 | 					(checkedInitMethods != null ? checkedInitMethods : this.initMethods);
  392 | 			if (!initMethodsToIterate.isEmpty()) {
  393 | 				for (LifecycleMethod lifecycleMethod : initMethodsToIterate) {
  394 | 					if (logger.isTraceEnabled()) {
  395 | 						logger.trace("Invoking init method on bean '" + beanName + "': " + lifecycleMethod.getMethod());
  396 | 					}
  397 | 					lifecycleMethod.invoke(target);
  398 | 				}
  399 | 			}
  400 | 		}
  401 | 
  402 | 		public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
  403 | 			Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
  404 | 			Collection<LifecycleMethod> destroyMethodsToUse =
  405 | 					(checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
  406 | 			if (!destroyMethodsToUse.isEmpty()) {
  407 | 				for (LifecycleMethod lifecycleMethod : destroyMethodsToUse) {
  408 | 					if (logger.isTraceEnabled()) {
  409 | 						logger.trace("Invoking destroy method on bean '" + beanName + "': " + lifecycleMethod.getMethod());
  410 | 					}
  411 | 					lifecycleMethod.invoke(target);
  412 | 				}
  413 | 			}
  414 | 		}
  415 | 
  416 | 		public boolean hasDestroyMethods() {
  417 | 			Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
  418 | 			Collection<LifecycleMethod> destroyMethodsToUse =
  419 | 					(checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
  420 | 			return !destroyMethodsToUse.isEmpty();
  421 | 		}
  422 | 	}
  423 | 
  424 | 
  425 | 	/**
  426 | 	 * Class representing an annotated init or destroy method.
  427 | 	 */
  428 | 	private static class LifecycleMethod {
  429 | 
  430 | 		private final Method method;
  431 | 
  432 | 		private final String identifier;
  433 | 
  434 | 		public LifecycleMethod(Method method, Class<?> beanClass) {
  435 | 			if (method.getParameterCount() != 0) {
  436 | 				throw new IllegalStateException("Lifecycle annotation requires a no-arg method: " + method);
  437 | 			}
  438 | 			this.method = method;
  439 | 			this.identifier = (isPrivateOrNotVisible(method, beanClass) ?
  440 | 					ClassUtils.getQualifiedMethodName(method) : method.getName());
  441 | 		}
  442 | 
  443 | 		public Method getMethod() {
  444 | 			return this.method;
  445 | 		}
  446 | 
  447 | 		public String getIdentifier() {
  448 | 			return this.identifier;
  449 | 		}
  450 | 
  451 | 		public void invoke(Object target) throws Throwable {
  452 | 			ReflectionUtils.makeAccessible(this.method);
  453 | 			this.method.invoke(target);
  454 | 		}
  455 | 
  456 | 		@Override
  457 | 		public boolean equals(@Nullable Object other) {
  458 | 			return (this == other || (other instanceof LifecycleMethod that &&
  459 | 					this.identifier.equals(that.identifier)));
  460 | 		}
  461 | 
  462 | 		@Override
  463 | 		public int hashCode() {
  464 | 			return this.identifier.hashCode();
  465 | 		}
  466 | 
  467 | 		/**
  468 | 		 * Determine if the supplied lifecycle {@link Method} is private or not
  469 | 		 * visible to the supplied bean {@link Class}.
  470 | 		 * @since 6.0.11
  471 | 		 */
  472 | 		private static boolean isPrivateOrNotVisible(Method method, Class<?> beanClass) {
  473 | 			int modifiers = method.getModifiers();
  474 | 			if (Modifier.isPrivate(modifiers)) {
  475 | 				return true;
  476 | 			}
  477 | 			// Method is declared in a class that resides in a different package
  478 | 			// than the bean class and the method is neither public nor protected?
  479 | 			return (!method.getDeclaringClass().getPackageName().equals(beanClass.getPackageName()) &&
  480 | 					!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)));
  481 | 		}
  482 | 
  483 | 	}
  484 | 
  485 | }
```
