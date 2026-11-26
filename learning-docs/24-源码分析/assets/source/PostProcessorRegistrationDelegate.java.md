<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/support/PostProcessorRegistrationDelegate.java -->
<!-- 正文引用：[[源码:PostProcessorRegistrationDelegate.java#L起-L止]] ；代码块头部：(src: PostProcessorRegistrationDelegate.java#L起-L止) -->

# PostProcessorRegistrationDelegate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/support/PostProcessorRegistrationDelegate.java` |
| 行数 | 543 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:02.193Z |

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
   17 | package org.springframework.context.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Comparator;
   22 | import java.util.HashSet;
   23 | import java.util.LinkedHashSet;
   24 | import java.util.List;
   25 | import java.util.Set;
   26 | import java.util.function.BiConsumer;
   27 | 
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.beans.PropertyValue;
   33 | import org.springframework.beans.factory.config.BeanDefinition;
   34 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   35 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   36 | import org.springframework.beans.factory.config.BeanPostProcessor;
   37 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   38 | import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
   39 | import org.springframework.beans.factory.config.TypedStringValue;
   40 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   41 | import org.springframework.beans.factory.support.AbstractBeanFactory;
   42 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   43 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   44 | import org.springframework.beans.factory.support.BeanDefinitionValueResolver;
   45 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   46 | import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
   47 | import org.springframework.beans.factory.support.RootBeanDefinition;
   48 | import org.springframework.core.OrderComparator;
   49 | import org.springframework.core.Ordered;
   50 | import org.springframework.core.PriorityOrdered;
   51 | import org.springframework.core.metrics.ApplicationStartup;
   52 | import org.springframework.core.metrics.StartupStep;
   53 | 
   54 | /**
   55 |  * Delegate for AbstractApplicationContext's post-processor handling.
   56 |  *
   57 |  * @author Juergen Hoeller
   58 |  * @author Sam Brannen
   59 |  * @author Stephane Nicoll
   60 |  * @since 4.0
   61 |  */
   62 | final class PostProcessorRegistrationDelegate {
   63 | 
   64 | 	private PostProcessorRegistrationDelegate() {
   65 | 	}
   66 | 
   67 | 
   68 | 	public static void invokeBeanFactoryPostProcessors(
   69 | 			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
   70 | 
   71 | 		// WARNING: Although it may appear that the body of this method can be easily
   72 | 		// refactored to avoid the use of multiple loops and multiple lists, the use
   73 | 		// of multiple lists and multiple passes over the names of processors is
   74 | 		// intentional. We must ensure that we honor the contracts for PriorityOrdered
   75 | 		// and Ordered processors. Specifically, we must NOT cause processors to be
   76 | 		// instantiated (via getBean() invocations) or registered in the ApplicationContext
   77 | 		// in the wrong order.
   78 | 		//
   79 | 		// Before submitting a pull request (PR) to change this method, please review the
   80 | 		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
   81 | 		// to ensure that your proposal does not result in a breaking change:
   82 | 		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22
   83 | 
   84 | 		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
   85 | 		Set<String> processedBeans = new HashSet<>();
   86 | 
   87 | 		if (beanFactory instanceof BeanDefinitionRegistry registry) {
   88 | 			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
   89 | 			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
   90 | 
   91 | 			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
   92 | 				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor registryProcessor) {
   93 | 					registryProcessor.postProcessBeanDefinitionRegistry(registry);
   94 | 					registryProcessors.add(registryProcessor);
   95 | 				}
   96 | 				else {
   97 | 					regularPostProcessors.add(postProcessor);
   98 | 				}
   99 | 			}
  100 | 
  101 | 			// Do not initialize FactoryBeans here: We need to leave all regular beans
  102 | 			// uninitialized to let the bean factory post-processors apply to them!
  103 | 			// Separate between BeanDefinitionRegistryPostProcessors that implement
  104 | 			// PriorityOrdered, Ordered, and the rest.
  105 | 			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
  106 | 
  107 | 			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
  108 | 			String[] postProcessorNames =
  109 | 					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
  110 | 			for (String ppName : postProcessorNames) {
  111 | 				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
  112 | 					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
  113 | 					processedBeans.add(ppName);
  114 | 				}
  115 | 			}
  116 | 			sortPostProcessors(currentRegistryProcessors, beanFactory);
  117 | 			registryProcessors.addAll(currentRegistryProcessors);
  118 | 			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
  119 | 			currentRegistryProcessors.clear();
  120 | 
  121 | 			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
  122 | 			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
  123 | 			for (String ppName : postProcessorNames) {
  124 | 				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
  125 | 					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
  126 | 					processedBeans.add(ppName);
  127 | 				}
  128 | 			}
  129 | 			sortPostProcessors(currentRegistryProcessors, beanFactory);
  130 | 			registryProcessors.addAll(currentRegistryProcessors);
  131 | 			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
  132 | 			currentRegistryProcessors.clear();
  133 | 
  134 | 			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
  135 | 			boolean reiterate = true;
  136 | 			while (reiterate) {
  137 | 				reiterate = false;
  138 | 				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
  139 | 				for (String ppName : postProcessorNames) {
  140 | 					if (!processedBeans.contains(ppName)) {
  141 | 						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
  142 | 						processedBeans.add(ppName);
  143 | 						reiterate = true;
  144 | 					}
  145 | 				}
  146 | 				sortPostProcessors(currentRegistryProcessors, beanFactory);
  147 | 				registryProcessors.addAll(currentRegistryProcessors);
  148 | 				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
  149 | 				currentRegistryProcessors.clear();
  150 | 			}
  151 | 
  152 | 			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
  153 | 			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
  154 | 			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
  155 | 		}
  156 | 
  157 | 		else {
  158 | 			// Invoke factory processors registered with the context instance.
  159 | 			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
  160 | 		}
  161 | 
  162 | 		// Do not initialize FactoryBeans here: We need to leave all regular beans
  163 | 		// uninitialized to let the bean factory post-processors apply to them!
  164 | 		String[] postProcessorNames =
  165 | 				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
  166 | 
  167 | 		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
  168 | 		// Ordered, and the rest.
  169 | 		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
  170 | 		List<String> orderedPostProcessorNames = new ArrayList<>();
  171 | 		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
  172 | 		for (String ppName : postProcessorNames) {
  173 | 			if (processedBeans.contains(ppName)) {
  174 | 				// skip - already processed in first phase above
  175 | 			}
  176 | 			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
  177 | 				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
  178 | 			}
  179 | 			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
  180 | 				orderedPostProcessorNames.add(ppName);
  181 | 			}
  182 | 			else {
  183 | 				nonOrderedPostProcessorNames.add(ppName);
  184 | 			}
  185 | 		}
  186 | 
  187 | 		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
  188 | 		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
  189 | 		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);
  190 | 
  191 | 		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
  192 | 		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
  193 | 		for (String postProcessorName : orderedPostProcessorNames) {
  194 | 			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
  195 | 		}
  196 | 		sortPostProcessors(orderedPostProcessors, beanFactory);
  197 | 		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);
  198 | 
  199 | 		// Finally, invoke all other BeanFactoryPostProcessors.
  200 | 		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
  201 | 		for (String postProcessorName : nonOrderedPostProcessorNames) {
  202 | 			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
  203 | 		}
  204 | 		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
  205 | 
  206 | 		// Clear cached merged bean definitions since the post-processors might have
  207 | 		// modified the original metadata, for example, replacing placeholders in values...
  208 | 		beanFactory.clearMetadataCache();
  209 | 	}
  210 | 
  211 | 	public static void registerBeanPostProcessors(
  212 | 			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
  213 | 
  214 | 		// WARNING: Although it may appear that the body of this method can be easily
  215 | 		// refactored to avoid the use of multiple loops and multiple lists, the use
  216 | 		// of multiple lists and multiple passes over the names of processors is
  217 | 		// intentional. We must ensure that we honor the contracts for PriorityOrdered
  218 | 		// and Ordered processors. Specifically, we must NOT cause processors to be
  219 | 		// instantiated (via getBean() invocations) or registered in the ApplicationContext
  220 | 		// in the wrong order.
  221 | 		//
  222 | 		// Before submitting a pull request (PR) to change this method, please review the
  223 | 		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
  224 | 		// to ensure that your proposal does not result in a breaking change:
  225 | 		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22
  226 | 
  227 | 		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
  228 | 
  229 | 		// Register BeanPostProcessorChecker that logs a warn message when
  230 | 		// a bean is created during BeanPostProcessor instantiation, i.e. when
  231 | 		// a bean is not eligible for getting processed by all BeanPostProcessors.
  232 | 		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
  233 | 		beanFactory.addBeanPostProcessor(
  234 | 				new BeanPostProcessorChecker(beanFactory, postProcessorNames, beanProcessorTargetCount));
  235 | 
  236 | 		// Separate between BeanPostProcessors that implement PriorityOrdered,
  237 | 		// Ordered, and the rest.
  238 | 		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
  239 | 		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
  240 | 		List<String> orderedPostProcessorNames = new ArrayList<>();
  241 | 		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
  242 | 		for (String ppName : postProcessorNames) {
  243 | 			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
  244 | 				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
  245 | 				priorityOrderedPostProcessors.add(pp);
  246 | 				if (pp instanceof MergedBeanDefinitionPostProcessor) {
  247 | 					internalPostProcessors.add(pp);
  248 | 				}
  249 | 			}
  250 | 			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
  251 | 				orderedPostProcessorNames.add(ppName);
  252 | 			}
  253 | 			else {
  254 | 				nonOrderedPostProcessorNames.add(ppName);
  255 | 			}
  256 | 		}
  257 | 
  258 | 		// First, register the BeanPostProcessors that implement PriorityOrdered.
  259 | 		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
  260 | 		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
  261 | 
  262 | 		// Next, register the BeanPostProcessors that implement Ordered.
  263 | 		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
  264 | 		for (String ppName : orderedPostProcessorNames) {
  265 | 			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
  266 | 			orderedPostProcessors.add(pp);
  267 | 			if (pp instanceof MergedBeanDefinitionPostProcessor) {
  268 | 				internalPostProcessors.add(pp);
  269 | 			}
  270 | 		}
  271 | 		sortPostProcessors(orderedPostProcessors, beanFactory);
  272 | 		registerBeanPostProcessors(beanFactory, orderedPostProcessors);
  273 | 
  274 | 		// Now, register all regular BeanPostProcessors.
  275 | 		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
  276 | 		for (String ppName : nonOrderedPostProcessorNames) {
  277 | 			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
  278 | 			nonOrderedPostProcessors.add(pp);
  279 | 			if (pp instanceof MergedBeanDefinitionPostProcessor) {
  280 | 				internalPostProcessors.add(pp);
  281 | 			}
  282 | 		}
  283 | 		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);
  284 | 
  285 | 		// Finally, re-register all internal BeanPostProcessors.
  286 | 		sortPostProcessors(internalPostProcessors, beanFactory);
  287 | 		registerBeanPostProcessors(beanFactory, internalPostProcessors);
  288 | 
  289 | 		// Re-register post-processor for detecting inner beans as ApplicationListeners,
  290 | 		// moving it to the end of the processor chain (for picking up proxies etc).
  291 | 		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
  292 | 	}
  293 | 
  294 | 	/**
  295 | 	 * Load and sort the post-processors of the specified type.
  296 | 	 * @param beanFactory the bean factory to use
  297 | 	 * @param beanPostProcessorType the post-processor type
  298 | 	 * @param <T> the post-processor type
  299 | 	 * @return a list of sorted post-processors for the specified type
  300 | 	 */
  301 | 	static <T extends BeanPostProcessor> List<T> loadBeanPostProcessors(
  302 | 			ConfigurableListableBeanFactory beanFactory, Class<T> beanPostProcessorType) {
  303 | 
  304 | 		String[] postProcessorNames = beanFactory.getBeanNamesForType(beanPostProcessorType, true, false);
  305 | 		List<T> postProcessors = new ArrayList<>();
  306 | 		for (String ppName : postProcessorNames) {
  307 | 			postProcessors.add(beanFactory.getBean(ppName, beanPostProcessorType));
  308 | 		}
  309 | 		sortPostProcessors(postProcessors, beanFactory);
  310 | 		return postProcessors;
  311 | 
  312 | 	}
  313 | 
  314 | 	/**
  315 | 	 * Selectively invoke {@link MergedBeanDefinitionPostProcessor} instances
  316 | 	 * registered in the specified bean factory, resolving bean definitions and
  317 | 	 * any attributes if necessary as well as any inner bean definitions that
  318 | 	 * they may contain.
  319 | 	 * @param beanFactory the bean factory to use
  320 | 	 */
  321 | 	static void invokeMergedBeanDefinitionPostProcessors(DefaultListableBeanFactory beanFactory) {
  322 | 		new MergedBeanDefinitionPostProcessorInvoker(beanFactory).invokeMergedBeanDefinitionPostProcessors();
  323 | 	}
  324 | 
  325 | 	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
  326 | 		// Nothing to sort?
  327 | 		if (postProcessors.size() <= 1) {
  328 | 			return;
  329 | 		}
  330 | 		Comparator<Object> comparatorToUse = null;
  331 | 		if (beanFactory instanceof DefaultListableBeanFactory dlbf) {
  332 | 			comparatorToUse = dlbf.getDependencyComparator();
  333 | 		}
  334 | 		if (comparatorToUse == null) {
  335 | 			comparatorToUse = OrderComparator.INSTANCE;
  336 | 		}
  337 | 		postProcessors.sort(comparatorToUse);
  338 | 	}
  339 | 
  340 | 	/**
  341 | 	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
  342 | 	 */
  343 | 	private static void invokeBeanDefinitionRegistryPostProcessors(
  344 | 			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {
  345 | 
  346 | 		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
  347 | 			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
  348 | 					.tag("postProcessor", postProcessor::toString);
  349 | 			postProcessor.postProcessBeanDefinitionRegistry(registry);
  350 | 			postProcessBeanDefRegistry.end();
  351 | 		}
  352 | 	}
  353 | 
  354 | 	/**
  355 | 	 * Invoke the given BeanFactoryPostProcessor beans.
  356 | 	 */
  357 | 	private static void invokeBeanFactoryPostProcessors(
  358 | 			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {
  359 | 
  360 | 		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
  361 | 			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
  362 | 					.tag("postProcessor", postProcessor::toString);
  363 | 			postProcessor.postProcessBeanFactory(beanFactory);
  364 | 			postProcessBeanFactory.end();
  365 | 		}
  366 | 	}
  367 | 
  368 | 	/**
  369 | 	 * Register the given BeanPostProcessor beans.
  370 | 	 */
  371 | 	private static void registerBeanPostProcessors(
  372 | 			ConfigurableListableBeanFactory beanFactory, List<? extends BeanPostProcessor> postProcessors) {
  373 | 
  374 | 		if (beanFactory instanceof AbstractBeanFactory abstractBeanFactory) {
  375 | 			// Bulk addition is more efficient against our CopyOnWriteArrayList there
  376 | 			abstractBeanFactory.addBeanPostProcessors(postProcessors);
  377 | 		}
  378 | 		else {
  379 | 			for (BeanPostProcessor postProcessor : postProcessors) {
  380 | 				beanFactory.addBeanPostProcessor(postProcessor);
  381 | 			}
  382 | 		}
  383 | 	}
  384 | 
  385 | 
  386 | 	/**
  387 | 	 * BeanPostProcessor that logs a warn message when a bean is created during
  388 | 	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
  389 | 	 * getting processed by all BeanPostProcessors.
  390 | 	 */
  391 | 	private static final class BeanPostProcessorChecker implements BeanPostProcessor {
  392 | 
  393 | 		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);
  394 | 
  395 | 		private final ConfigurableListableBeanFactory beanFactory;
  396 | 
  397 | 		private final String[] postProcessorNames;
  398 | 
  399 | 		private final int beanPostProcessorTargetCount;
  400 | 
  401 | 		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory,
  402 | 				String[] postProcessorNames, int beanPostProcessorTargetCount) {
  403 | 
  404 | 			this.beanFactory = beanFactory;
  405 | 			this.postProcessorNames = postProcessorNames;
  406 | 			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
  407 | 		}
  408 | 
  409 | 		@Override
  410 | 		public Object postProcessBeforeInitialization(Object bean, String beanName) {
  411 | 			return bean;
  412 | 		}
  413 | 
  414 | 		@Override
  415 | 		public Object postProcessAfterInitialization(Object bean, String beanName) {
  416 | 			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
  417 | 					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
  418 | 				if (logger.isWarnEnabled()) {
  419 | 					Set<String> bppsInCreation = new LinkedHashSet<>(2);
  420 | 					for (String bppName : this.postProcessorNames) {
  421 | 						if (this.beanFactory.isCurrentlyInCreation(bppName)) {
  422 | 							bppsInCreation.add(bppName);
  423 | 						}
  424 | 					}
  425 | 					if (bppsInCreation.size() == 1) {
  426 | 						String bppName = bppsInCreation.iterator().next();
  427 | 						if (this.beanFactory.containsBeanDefinition(bppName) &&
  428 | 								beanName.equals(this.beanFactory.getBeanDefinition(bppName).getFactoryBeanName())) {
  429 | 							logger.warn("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
  430 | 									"] is not eligible for getting processed by all BeanPostProcessors " +
  431 | 									"(for example: not eligible for auto-proxying). The currently created " +
  432 | 									"BeanPostProcessor " + bppsInCreation + " is declared through a non-static " +
  433 | 									"factory method on that class; consider declaring it as static instead.");
  434 | 							return bean;
  435 | 						}
  436 | 					}
  437 | 					logger.warn("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
  438 | 							"] is not eligible for getting processed by all BeanPostProcessors " +
  439 | 							"(for example: not eligible for auto-proxying). Is this bean getting eagerly " +
  440 | 							"injected/applied to a currently created BeanPostProcessor " + bppsInCreation + "? " +
  441 | 							"Check the corresponding BeanPostProcessor declaration and its dependencies/advisors. " +
  442 | 							"If this bean does not have to be post-processed, declare it with ROLE_INFRASTRUCTURE.");
  443 | 				}
  444 | 			}
  445 | 			return bean;
  446 | 		}
  447 | 
  448 | 		private boolean isInfrastructureBean(@Nullable String beanName) {
  449 | 			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
  450 | 				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
  451 | 				return (bd.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE);
  452 | 			}
  453 | 			return false;
  454 | 		}
  455 | 	}
  456 | 
  457 | 
  458 | 	private static final class MergedBeanDefinitionPostProcessorInvoker {
  459 | 
  460 | 		private final DefaultListableBeanFactory beanFactory;
  461 | 
  462 | 		private MergedBeanDefinitionPostProcessorInvoker(DefaultListableBeanFactory beanFactory) {
  463 | 			this.beanFactory = beanFactory;
  464 | 		}
  465 | 
  466 | 		private void invokeMergedBeanDefinitionPostProcessors() {
  467 | 			List<MergedBeanDefinitionPostProcessor> postProcessors = PostProcessorRegistrationDelegate.loadBeanPostProcessors(
  468 | 					this.beanFactory, MergedBeanDefinitionPostProcessor.class);
  469 | 			for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
  470 | 				RootBeanDefinition bd = (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(beanName);
  471 | 				Class<?> beanType = resolveBeanType(bd);
  472 | 				postProcessRootBeanDefinition(postProcessors, beanName, beanType, bd);
  473 | 				bd.markAsPostProcessed();
  474 | 			}
  475 | 			registerBeanPostProcessors(this.beanFactory, postProcessors);
  476 | 		}
  477 | 
  478 | 		private void postProcessRootBeanDefinition(List<MergedBeanDefinitionPostProcessor> postProcessors,
  479 | 				String beanName, Class<?> beanType, RootBeanDefinition bd) {
  480 | 
  481 | 			BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this.beanFactory, beanName, bd);
  482 | 			postProcessors.forEach(postProcessor -> postProcessor.postProcessMergedBeanDefinition(bd, beanType, beanName));
  483 | 			for (PropertyValue propertyValue : bd.getPropertyValues().getPropertyValueList()) {
  484 | 				postProcessValue(postProcessors, valueResolver, propertyValue.getValue());
  485 | 			}
  486 | 			for (ValueHolder valueHolder : bd.getConstructorArgumentValues().getIndexedArgumentValues().values()) {
  487 | 				postProcessValue(postProcessors, valueResolver, valueHolder.getValue());
  488 | 			}
  489 | 			for (ValueHolder valueHolder : bd.getConstructorArgumentValues().getGenericArgumentValues()) {
  490 | 				postProcessValue(postProcessors, valueResolver, valueHolder.getValue());
  491 | 			}
  492 | 		}
  493 | 
  494 | 		private void postProcessValue(List<MergedBeanDefinitionPostProcessor> postProcessors,
  495 | 				BeanDefinitionValueResolver valueResolver, @Nullable Object value) {
  496 | 			if (value instanceof BeanDefinitionHolder bdh &&
  497 | 					bdh.getBeanDefinition() instanceof AbstractBeanDefinition innerBd) {
  498 | 
  499 | 				Class<?> innerBeanType = resolveBeanType(innerBd);
  500 | 				resolveInnerBeanDefinition(valueResolver, innerBd, (innerBeanName, innerBeanDefinition)
  501 | 						-> postProcessRootBeanDefinition(postProcessors, innerBeanName, innerBeanType, innerBeanDefinition));
  502 | 			}
  503 | 			else if (value instanceof AbstractBeanDefinition innerBd) {
  504 | 				Class<?> innerBeanType = resolveBeanType(innerBd);
  505 | 				resolveInnerBeanDefinition(valueResolver, innerBd, (innerBeanName, innerBeanDefinition)
  506 | 						-> postProcessRootBeanDefinition(postProcessors, innerBeanName, innerBeanType, innerBeanDefinition));
  507 | 			}
  508 | 			else if (value instanceof TypedStringValue typedStringValue) {
  509 | 				resolveTypeStringValue(typedStringValue);
  510 | 			}
  511 | 		}
  512 | 
  513 | 		private void resolveInnerBeanDefinition(BeanDefinitionValueResolver valueResolver, BeanDefinition innerBeanDefinition,
  514 | 				BiConsumer<String, RootBeanDefinition> resolver) {
  515 | 
  516 | 			valueResolver.resolveInnerBean(null, innerBeanDefinition, (name, rbd) -> {
  517 | 				resolver.accept(name, rbd);
  518 | 				return Void.class;
  519 | 			});
  520 | 		}
  521 | 
  522 | 		private void resolveTypeStringValue(TypedStringValue typedStringValue) {
  523 | 			try {
  524 | 				typedStringValue.resolveTargetType(this.beanFactory.getBeanClassLoader());
  525 | 			}
  526 | 			catch (ClassNotFoundException ignored) {
  527 | 			}
  528 | 		}
  529 | 
  530 | 		private Class<?> resolveBeanType(AbstractBeanDefinition bd) {
  531 | 			if (!bd.hasBeanClass()) {
  532 | 				try {
  533 | 					bd.resolveBeanClass(this.beanFactory.getBeanClassLoader());
  534 | 				}
  535 | 				catch (ClassNotFoundException ex) {
  536 | 					// ignore
  537 | 				}
  538 | 			}
  539 | 			return bd.getResolvableType().toClass();
  540 | 		}
  541 | 	}
  542 | 
  543 | }
```
