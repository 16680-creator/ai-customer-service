<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/OnBeanCondition.java -->
<!-- 正文引用：[[源码:OnBeanCondition.java#L起-L止]] ；代码块头部：(src: OnBeanCondition.java#L起-L止) -->

# OnBeanCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/OnBeanCondition.java` |
| 行数 | 924 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:47.660Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | import java.lang.reflect.Method;
    9 | import java.util.ArrayList;
   10 | import java.util.Arrays;
   11 | import java.util.Collection;
   12 | import java.util.Collections;
   13 | import java.util.HashMap;
   14 | import java.util.HashSet;
   15 | import java.util.LinkedHashMap;
   16 | import java.util.LinkedHashSet;
   17 | import java.util.List;
   18 | import java.util.Locale;
   19 | import java.util.Map;
   20 | import java.util.Map.Entry;
   21 | import java.util.Set;
   22 | import java.util.function.BiPredicate;
   23 | import java.util.function.Predicate;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.aop.scope.ScopedProxyUtils;
   28 | import org.springframework.beans.factory.BeanFactory;
   29 | import org.springframework.beans.factory.BeanFactoryUtils;
   30 | import org.springframework.beans.factory.HierarchicalBeanFactory;
   31 | import org.springframework.beans.factory.ListableBeanFactory;
   32 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   33 | import org.springframework.beans.factory.config.BeanDefinition;
   34 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   35 | import org.springframework.beans.factory.config.SingletonBeanRegistry;
   36 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   37 | import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
   38 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
   39 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
   40 | import org.springframework.context.annotation.Bean;
   41 | import org.springframework.context.annotation.Condition;
   42 | import org.springframework.context.annotation.ConditionContext;
   43 | import org.springframework.context.annotation.ConfigurationCondition;
   44 | import org.springframework.core.Ordered;
   45 | import org.springframework.core.ResolvableType;
   46 | import org.springframework.core.annotation.MergedAnnotation;
   47 | import org.springframework.core.annotation.MergedAnnotation.Adapt;
   48 | import org.springframework.core.annotation.MergedAnnotationCollectors;
   49 | import org.springframework.core.annotation.MergedAnnotationPredicates;
   50 | import org.springframework.core.annotation.MergedAnnotations;
   51 | import org.springframework.core.annotation.Order;
   52 | import org.springframework.core.type.AnnotatedTypeMetadata;
   53 | import org.springframework.core.type.MethodMetadata;
   54 | import org.springframework.lang.Contract;
   55 | import org.springframework.util.Assert;
   56 | import org.springframework.util.ClassUtils;
   57 | import org.springframework.util.CollectionUtils;
   58 | import org.springframework.util.MultiValueMap;
   59 | import org.springframework.util.ObjectUtils;
   60 | import org.springframework.util.ReflectionUtils;
   61 | import org.springframework.util.StringUtils;
   62 | 
   63 | /**
   64 |  * {@link Condition} that checks for the presence or absence of specific beans.
   65 |  *
   66 |  * @author Phillip Webb
   67 |  * @author Dave Syer
   68 |  * @author Jakub Kubrynski
   69 |  * @author Stephane Nicoll
   70 |  * @author Andy Wilkinson
   71 |  * @author Uladzislau Seuruk
   72 |  * @see ConditionalOnBean
   73 |  * @see ConditionalOnMissingBean
   74 |  * @see ConditionalOnSingleCandidate
   75 |  */
   76 | @Order(Ordered.LOWEST_PRECEDENCE)
   77 | class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {
   78 | 
   79 | 	@Override
   80 | 	public ConfigurationPhase getConfigurationPhase() {
   81 | 		return ConfigurationPhase.REGISTER_BEAN;
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	protected final @Nullable ConditionOutcome[] getOutcomes(@Nullable String[] autoConfigurationClasses,
   86 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   87 | 		@Nullable ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
   88 | 		for (int i = 0; i < outcomes.length; i++) {
   89 | 			String autoConfigurationClass = autoConfigurationClasses[i];
   90 | 			if (autoConfigurationClass != null) {
   91 | 				Set<String> onBeanTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
   92 | 				outcomes[i] = getOutcome(onBeanTypes, ConditionalOnBean.class);
   93 | 				if (outcomes[i] == null) {
   94 | 					Set<String> onSingleCandidateTypes = autoConfigurationMetadata.getSet(autoConfigurationClass,
   95 | 							"ConditionalOnSingleCandidate");
   96 | 					outcomes[i] = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
   97 | 				}
   98 | 			}
   99 | 		}
  100 | 		return outcomes;
  101 | 	}
  102 | 
  103 | 	private @Nullable ConditionOutcome getOutcome(@Nullable Set<String> requiredBeanTypes,
  104 | 			Class<? extends Annotation> annotation) {
  105 | 		List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
  106 | 		if (!missing.isEmpty()) {
  107 | 			ConditionMessage message = ConditionMessage.forCondition(annotation)
  108 | 				.didNotFind("required type", "required types")
  109 | 				.items(Style.QUOTE, missing);
  110 | 			return ConditionOutcome.noMatch(message);
  111 | 		}
  112 | 		return null;
  113 | 	}
  114 | 
  115 | 	@Override
  116 | 	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
  117 | 		ConditionOutcome matchOutcome = ConditionOutcome.match();
  118 | 		MergedAnnotations annotations = metadata.getAnnotations();
  119 | 		if (annotations.isPresent(ConditionalOnBean.class)) {
  120 | 			Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
  121 | 			matchOutcome = evaluateConditionalOnBean(spec, matchOutcome.getConditionMessage());
  122 | 			if (!matchOutcome.isMatch()) {
  123 | 				return matchOutcome;
  124 | 			}
  125 | 		}
  126 | 		if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
  127 | 			Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata,
  128 | 					metadata.getAnnotations());
  129 | 			matchOutcome = evaluateConditionalOnSingleCandidate(spec, matchOutcome.getConditionMessage());
  130 | 			if (!matchOutcome.isMatch()) {
  131 | 				return matchOutcome;
  132 | 			}
  133 | 		}
  134 | 		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
  135 | 			Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
  136 | 					ConditionalOnMissingBean.class);
  137 | 			matchOutcome = evaluateConditionalOnMissingBean(spec, matchOutcome.getConditionMessage());
  138 | 			if (!matchOutcome.isMatch()) {
  139 | 				return matchOutcome;
  140 | 			}
  141 | 		}
  142 | 		return matchOutcome;
  143 | 	}
  144 | 
  145 | 	private ConditionOutcome evaluateConditionalOnBean(Spec<ConditionalOnBean> spec, ConditionMessage matchMessage) {
  146 | 		MatchResult matchResult = getMatchingBeans(spec);
  147 | 		if (!matchResult.isAllMatched()) {
  148 | 			String reason = createOnBeanNoMatchReason(matchResult);
  149 | 			return ConditionOutcome.noMatch(spec.message().because(reason));
  150 | 		}
  151 | 		return ConditionOutcome.match(spec.message(matchMessage)
  152 | 			.found("bean", "beans")
  153 | 			.items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
  154 | 	}
  155 | 
  156 | 	private ConditionOutcome evaluateConditionalOnSingleCandidate(Spec<ConditionalOnSingleCandidate> spec,
  157 | 			ConditionMessage matchMessage) {
  158 | 		MatchResult matchResult = getMatchingBeans(spec);
  159 | 		if (!matchResult.isAllMatched()) {
  160 | 			return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
  161 | 		}
  162 | 		Set<String> allBeans = matchResult.getNamesOfAllMatches();
  163 | 		if (allBeans.size() == 1) {
  164 | 			return ConditionOutcome
  165 | 				.match(spec.message(matchMessage).found("a single bean").items(Style.QUOTE, allBeans));
  166 | 		}
  167 | 		ConfigurableListableBeanFactory beanFactory = spec.context.getBeanFactory();
  168 | 		Assert.state(beanFactory != null, "'beanFactory' must not be null");
  169 | 		Map<String, @Nullable BeanDefinition> beanDefinitions = getBeanDefinitions(beanFactory, allBeans,
  170 | 				spec.getStrategy() == SearchStrategy.ALL);
  171 | 		List<String> primaryBeans = getPrimaryBeans(beanDefinitions);
  172 | 		if (primaryBeans.size() == 1) {
  173 | 			return ConditionOutcome.match(spec.message(matchMessage)
  174 | 				.found("a single primary bean '" + primaryBeans.get(0) + "' from beans")
  175 | 				.items(Style.QUOTE, allBeans));
  176 | 		}
  177 | 		if (primaryBeans.size() > 1) {
  178 | 			return ConditionOutcome
  179 | 				.noMatch(spec.message().found("multiple primary beans").items(Style.QUOTE, primaryBeans));
  180 | 		}
  181 | 		List<String> nonFallbackBeans = getNonFallbackBeans(beanDefinitions);
  182 | 		if (nonFallbackBeans.size() == 1) {
  183 | 			return ConditionOutcome.match(spec.message(matchMessage)
  184 | 				.found("a single non-fallback bean '" + nonFallbackBeans.get(0) + "' from beans")
  185 | 				.items(Style.QUOTE, allBeans));
  186 | 		}
  187 | 		return ConditionOutcome.noMatch(spec.message().found("multiple beans").items(Style.QUOTE, allBeans));
  188 | 	}
  189 | 
  190 | 	private ConditionOutcome evaluateConditionalOnMissingBean(Spec<ConditionalOnMissingBean> spec,
  191 | 			ConditionMessage matchMessage) {
  192 | 		MatchResult matchResult = getMatchingBeans(spec);
  193 | 		if (matchResult.isAnyMatched()) {
  194 | 			String reason = createOnMissingBeanNoMatchReason(matchResult);
  195 | 			return ConditionOutcome.noMatch(spec.message().because(reason));
  196 | 		}
  197 | 		return ConditionOutcome.match(spec.message(matchMessage).didNotFind("any beans").atAll());
  198 | 	}
  199 | 
  200 | 	protected final MatchResult getMatchingBeans(Spec<?> spec) {
  201 | 		ConfigurableListableBeanFactory beanFactory = getSearchBeanFactory(spec);
  202 | 		ClassLoader classLoader = spec.getContext().getClassLoader();
  203 | 		boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
  204 | 		Set<BeanType> parameterizedContainers = spec.getParameterizedContainers();
  205 | 		MatchResult result = new MatchResult();
  206 | 		Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(beanFactory, considerHierarchy,
  207 | 				spec.getIgnoredTypes(), parameterizedContainers);
  208 | 		for (BeanType type : spec.getTypes()) {
  209 | 			Map<String, @Nullable BeanDefinition> typeMatchedDefinitions = getBeanDefinitionsForType(beanFactory,
  210 | 					considerHierarchy, type, parameterizedContainers);
  211 | 			Set<String> typeMatchedNames = matchedNamesFrom(typeMatchedDefinitions,
  212 | 					(name, definition) -> !ScopedProxyUtils.isScopedTarget(name)
  213 | 							&& isCandidate(beanFactory, name, definition, beansIgnoredByType));
  214 | 			if (typeMatchedNames.isEmpty()) {
  215 | 				result.recordUnmatchedType(type);
  216 | 			}
  217 | 			else {
  218 | 				result.recordMatchedType(type, typeMatchedNames);
  219 | 			}
  220 | 		}
  221 | 		for (String annotation : spec.getAnnotations()) {
  222 | 			Map<String, @Nullable BeanDefinition> annotationMatchedDefinitions = getBeanDefinitionsForAnnotation(
  223 | 					classLoader, beanFactory, annotation, considerHierarchy);
  224 | 			Set<String> annotationMatchedNames = matchedNamesFrom(annotationMatchedDefinitions,
  225 | 					(name, definition) -> isCandidate(beanFactory, name, definition, beansIgnoredByType));
  226 | 			if (annotationMatchedNames.isEmpty()) {
  227 | 				result.recordUnmatchedAnnotation(annotation);
  228 | 			}
  229 | 			else {
  230 | 				result.recordMatchedAnnotation(annotation, annotationMatchedNames);
  231 | 
  232 | 			}
  233 | 		}
  234 | 		for (String beanName : spec.getNames()) {
  235 | 			if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
  236 | 				result.recordMatchedName(beanName);
  237 | 			}
  238 | 			else {
  239 | 				result.recordUnmatchedName(beanName);
  240 | 			}
  241 | 		}
  242 | 		return result;
  243 | 	}
  244 | 
  245 | 	private ConfigurableListableBeanFactory getSearchBeanFactory(Spec<?> spec) {
  246 | 		ConfigurableListableBeanFactory beanFactory = spec.getContext().getBeanFactory();
  247 | 		Assert.state(beanFactory != null, "'beanFactory' must not be null'");
  248 | 		if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
  249 | 			BeanFactory parent = beanFactory.getParentBeanFactory();
  250 | 			Assert.state(parent instanceof ConfigurableListableBeanFactory,
  251 | 					"Unable to use SearchStrategy.ANCESTORS without ConfigurableListableBeanFactory");
  252 | 			beanFactory = (ConfigurableListableBeanFactory) parent;
  253 | 		}
  254 | 		return beanFactory;
  255 | 	}
  256 | 
  257 | 	private Set<String> matchedNamesFrom(Map<String, @Nullable BeanDefinition> namedDefinitions,
  258 | 			BiPredicate<String, BeanDefinition> filter) {
  259 | 		Set<String> matchedNames = new LinkedHashSet<>(namedDefinitions.size());
  260 | 		for (Entry<String, BeanDefinition> namedDefinition : namedDefinitions.entrySet()) {
  261 | 			if (filter.test(namedDefinition.getKey(), namedDefinition.getValue())) {
  262 | 				matchedNames.add(namedDefinition.getKey());
  263 | 			}
  264 | 		}
  265 | 		return matchedNames;
  266 | 	}
  267 | 
  268 | 	private boolean isCandidate(ConfigurableListableBeanFactory beanFactory, String name,
  269 | 			@Nullable BeanDefinition definition, Set<String> ignoredBeans) {
  270 | 		if (ignoredBeans.contains(name)) {
  271 | 			return false;
  272 | 		}
  273 | 		if (definition == null || (definition.isAutowireCandidate() && isDefaultCandidate(definition))) {
  274 | 			return true;
  275 | 		}
  276 | 		if (ScopedProxyUtils.isScopedTarget(name)) {
  277 | 			try {
  278 | 				BeanDefinition originalDefinition = beanFactory
  279 | 					.getBeanDefinition(ScopedProxyUtils.getOriginalBeanName(name));
  280 | 				if (originalDefinition.isAutowireCandidate() && isDefaultCandidate(originalDefinition)) {
  281 | 					return true;
  282 | 				}
  283 | 			}
  284 | 			catch (NoSuchBeanDefinitionException ex) {
  285 | 			}
  286 | 		}
  287 | 		return false;
  288 | 	}
  289 | 
  290 | 	private boolean isDefaultCandidate(BeanDefinition definition) {
  291 | 		if (definition instanceof AbstractBeanDefinition abstractBeanDefinition) {
  292 | 			return abstractBeanDefinition.isDefaultCandidate();
  293 | 		}
  294 | 		return true;
  295 | 	}
  296 | 
  297 | 	private Set<String> getNamesOfBeansIgnoredByType(ListableBeanFactory beanFactory, boolean considerHierarchy,
  298 | 			Set<BeanType> ignoredTypes, Set<BeanType> parameterizedContainers) {
  299 | 		Set<String> result = null;
  300 | 		for (BeanType ignoredType : ignoredTypes) {
  301 | 			Collection<String> ignoredNames = getBeanDefinitionsForType(beanFactory, considerHierarchy, ignoredType,
  302 | 					parameterizedContainers)
  303 | 				.keySet();
  304 | 			result = addAll(result, ignoredNames);
  305 | 		}
  306 | 		return (result != null) ? result : Collections.emptySet();
  307 | 	}
  308 | 
  309 | 	private Map<String, @Nullable BeanDefinition> getBeanDefinitionsForType(ListableBeanFactory beanFactory,
  310 | 			boolean considerHierarchy, BeanType type, Set<BeanType> parameterizedContainers) {
  311 | 		Map<String, @Nullable BeanDefinition> result = collectBeanDefinitionsForType(beanFactory, considerHierarchy,
  312 | 				type, parameterizedContainers, null);
  313 | 		return (result != null) ? result : Collections.<String, @Nullable BeanDefinition>emptyMap();
  314 | 	}
  315 | 
  316 | 	private @Nullable Map<String, @Nullable BeanDefinition> collectBeanDefinitionsForType(
  317 | 			ListableBeanFactory beanFactory, boolean considerHierarchy, BeanType type,
  318 | 			Set<BeanType> parameterizedContainers, @Nullable Map<String, @Nullable BeanDefinition> result) {
  319 | 		if (ResolvableType.NONE.equals(type.resolvableType())) {
  320 | 			return result;
  321 | 		}
  322 | 		result = putAll(result, beanFactory.getBeanNamesForType(type.resolvableType(), true, false), beanFactory);
  323 | 		for (BeanType parameterizedContainer : parameterizedContainers) {
  324 | 			Class<?> resolved = parameterizedContainer.resolvableType().resolve();
  325 | 			Assert.state(resolved != null, "'resolved' must not be null");
  326 | 			ResolvableType generic = ResolvableType.forClassWithGenerics(resolved, type.resolvableType());
  327 | 			result = putAll(result, beanFactory.getBeanNamesForType(generic, true, false), beanFactory);
  328 | 		}
  329 | 		if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
  330 | 			BeanFactory parent = hierarchicalBeanFactory.getParentBeanFactory();
  331 | 			if (parent instanceof ListableBeanFactory listableBeanFactory) {
  332 | 				result = collectBeanDefinitionsForType(listableBeanFactory, considerHierarchy, type,
  333 | 						parameterizedContainers, result);
  334 | 			}
  335 | 		}
  336 | 		return result;
  337 | 	}
  338 | 
  339 | 	private Map<String, @Nullable BeanDefinition> getBeanDefinitionsForAnnotation(@Nullable ClassLoader classLoader,
  340 | 			ConfigurableListableBeanFactory beanFactory, String type, boolean considerHierarchy) throws LinkageError {
  341 | 		Map<String, @Nullable BeanDefinition> result = null;
  342 | 		try {
  343 | 			result = collectBeanDefinitionsForAnnotation(beanFactory, resolveAnnotationType(classLoader, type),
  344 | 					considerHierarchy, result);
  345 | 		}
  346 | 		catch (ClassNotFoundException ex) {
  347 | 			// Continue
  348 | 		}
  349 | 		return (result != null) ? result : Collections.<String, @Nullable BeanDefinition>emptyMap();
  350 | 	}
  351 | 
  352 | 	@SuppressWarnings("unchecked")
  353 | 	private Class<? extends Annotation> resolveAnnotationType(@Nullable ClassLoader classLoader, String type)
  354 | 			throws ClassNotFoundException {
  355 | 		return (Class<? extends Annotation>) resolve(type, classLoader);
  356 | 	}
  357 | 
  358 | 	private @Nullable Map<String, @Nullable BeanDefinition> collectBeanDefinitionsForAnnotation(
  359 | 			ListableBeanFactory beanFactory, Class<? extends Annotation> annotationType, boolean considerHierarchy,
  360 | 			@Nullable Map<String, @Nullable BeanDefinition> result) {
  361 | 		result = putAll(result, getBeanNamesForAnnotation(beanFactory, annotationType), beanFactory);
  362 | 		if (considerHierarchy) {
  363 | 			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
  364 | 			if (parent instanceof ListableBeanFactory listableBeanFactory) {
  365 | 				result = collectBeanDefinitionsForAnnotation(listableBeanFactory, annotationType, considerHierarchy,
  366 | 						result);
  367 | 			}
  368 | 		}
  369 | 		return result;
  370 | 	}
  371 | 
  372 | 	private String[] getBeanNamesForAnnotation(ListableBeanFactory beanFactory,
  373 | 			Class<? extends Annotation> annotationType) {
  374 | 		Set<String> foundBeanNames = new LinkedHashSet<>();
  375 | 		for (String beanName : beanFactory.getBeanDefinitionNames()) {
  376 | 			if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
  377 | 				BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(beanName);
  378 | 				if (beanDefinition != null && beanDefinition.isAbstract()) {
  379 | 					continue;
  380 | 				}
  381 | 			}
  382 | 			if (beanFactory.findAnnotationOnBean(beanName, annotationType, false) != null) {
  383 | 				foundBeanNames.add(beanName);
  384 | 			}
  385 | 		}
  386 | 		if (beanFactory instanceof SingletonBeanRegistry singletonBeanRegistry) {
  387 | 			for (String beanName : singletonBeanRegistry.getSingletonNames()) {
  388 | 				if (beanFactory.findAnnotationOnBean(beanName, annotationType) != null) {
  389 | 					foundBeanNames.add(beanName);
  390 | 				}
  391 | 			}
  392 | 		}
  393 | 		return foundBeanNames.toArray(String[]::new);
  394 | 	}
  395 | 
  396 | 	private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName,
  397 | 			boolean considerHierarchy) {
  398 | 		if (considerHierarchy) {
  399 | 			return beanFactory.containsBean(beanName);
  400 | 		}
  401 | 		return beanFactory.containsLocalBean(beanName);
  402 | 	}
  403 | 
  404 | 	private String createOnBeanNoMatchReason(MatchResult matchResult) {
  405 | 		StringBuilder reason = new StringBuilder();
  406 | 		appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
  407 | 		appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
  408 | 		appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
  409 | 		return reason.toString();
  410 | 	}
  411 | 
  412 | 	private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
  413 | 		if (!unmatched.isEmpty()) {
  414 | 			if (!reason.isEmpty()) {
  415 | 				reason.append(" and ");
  416 | 			}
  417 | 			reason.append("did not find any beans ");
  418 | 			reason.append(description);
  419 | 			reason.append(" ");
  420 | 			reason.append(StringUtils.collectionToDelimitedString(unmatched, ", "));
  421 | 		}
  422 | 	}
  423 | 
  424 | 	private String createOnMissingBeanNoMatchReason(MatchResult matchResult) {
  425 | 		StringBuilder reason = new StringBuilder();
  426 | 		appendMessageForMatches(reason, matchResult.getMatchedAnnotations(), "annotated with");
  427 | 		appendMessageForMatches(reason, matchResult.getMatchedTypes(), "of type");
  428 | 		if (!matchResult.getMatchedNames().isEmpty()) {
  429 | 			if (!reason.isEmpty()) {
  430 | 				reason.append(" and ");
  431 | 			}
  432 | 			reason.append("found beans named ");
  433 | 			reason.append(StringUtils.collectionToDelimitedString(matchResult.getMatchedNames(), ", "));
  434 | 		}
  435 | 		return reason.toString();
  436 | 	}
  437 | 
  438 | 	private void appendMessageForMatches(StringBuilder reason, Map<String, Collection<String>> matches,
  439 | 			String description) {
  440 | 		if (!matches.isEmpty()) {
  441 | 			matches.forEach((key, value) -> {
  442 | 				if (!reason.isEmpty()) {
  443 | 					reason.append(" and ");
  444 | 				}
  445 | 				reason.append("found beans ");
  446 | 				reason.append(description);
  447 | 				reason.append(" '");
  448 | 				reason.append(key);
  449 | 				reason.append("' ");
  450 | 				reason.append(StringUtils.collectionToDelimitedString(value, ", "));
  451 | 			});
  452 | 		}
  453 | 	}
  454 | 
  455 | 	private Map<String, @Nullable BeanDefinition> getBeanDefinitions(ConfigurableListableBeanFactory beanFactory,
  456 | 			Set<String> beanNames, boolean considerHierarchy) {
  457 | 		Map<String, @Nullable BeanDefinition> definitions = new HashMap<>(beanNames.size());
  458 | 		for (String beanName : beanNames) {
  459 | 			BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
  460 | 			definitions.put(beanName, beanDefinition);
  461 | 		}
  462 | 		return definitions;
  463 | 	}
  464 | 
  465 | 	private List<String> getPrimaryBeans(Map<String, @Nullable BeanDefinition> beanDefinitions) {
  466 | 		return getMatchingBeans(beanDefinitions, this::isPrimary);
  467 | 	}
  468 | 
  469 | 	private boolean isPrimary(@Nullable BeanDefinition beanDefinition) {
  470 | 		return (beanDefinition != null) && beanDefinition.isPrimary();
  471 | 	}
  472 | 
  473 | 	private List<String> getNonFallbackBeans(Map<String, @Nullable BeanDefinition> beanDefinitions) {
  474 | 		return getMatchingBeans(beanDefinitions, this::isNotFallback);
  475 | 	}
  476 | 
  477 | 	private boolean isNotFallback(@Nullable BeanDefinition beanDefinition) {
  478 | 		return (beanDefinition == null) || !beanDefinition.isFallback();
  479 | 	}
  480 | 
  481 | 	private List<String> getMatchingBeans(Map<String, @Nullable BeanDefinition> beanDefinitions,
  482 | 			Predicate<@Nullable BeanDefinition> test) {
  483 | 		List<String> matches = new ArrayList<>();
  484 | 		for (Entry<String, @Nullable BeanDefinition> namedBeanDefinition : beanDefinitions.entrySet()) {
  485 | 			if (test.test(namedBeanDefinition.getValue())) {
  486 | 				matches.add(namedBeanDefinition.getKey());
  487 | 			}
  488 | 		}
  489 | 		return matches;
  490 | 	}
  491 | 
  492 | 	private @Nullable BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName,
  493 | 			boolean considerHierarchy) {
  494 | 		if (beanFactory.containsBeanDefinition(beanName)) {
  495 | 			return beanFactory.getBeanDefinition(beanName);
  496 | 		}
  497 | 		if (considerHierarchy
  498 | 				&& beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory listableBeanFactory) {
  499 | 			return findBeanDefinition(listableBeanFactory, beanName, considerHierarchy);
  500 | 		}
  501 | 		return null;
  502 | 	}
  503 | 
  504 | 	private static @Nullable Set<String> addAll(@Nullable Set<String> result, @Nullable Collection<String> additional) {
  505 | 		if (CollectionUtils.isEmpty(additional)) {
  506 | 			return result;
  507 | 		}
  508 | 		result = (result != null) ? result : new LinkedHashSet<>();
  509 | 		result.addAll(additional);
  510 | 		return result;
  511 | 	}
  512 | 
  513 | 	private static @Nullable Map<String, @Nullable BeanDefinition> putAll(
  514 | 			@Nullable Map<String, @Nullable BeanDefinition> result, String[] beanNames,
  515 | 			ListableBeanFactory beanFactory) {
  516 | 		if (ObjectUtils.isEmpty(beanNames)) {
  517 | 			return result;
  518 | 		}
  519 | 		if (result == null) {
  520 | 			result = new LinkedHashMap<>();
  521 | 		}
  522 | 		for (String beanName : beanNames) {
  523 | 			if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
  524 | 				result.put(beanName, getBeanDefinition(beanName, clbf));
  525 | 			}
  526 | 			else {
  527 | 				result.put(beanName, null);
  528 | 			}
  529 | 		}
  530 | 		return result;
  531 | 	}
  532 | 
  533 | 	private static @Nullable BeanDefinition getBeanDefinition(String beanName,
  534 | 			ConfigurableListableBeanFactory beanFactory) {
  535 | 		try {
  536 | 			return beanFactory.getBeanDefinition(beanName);
  537 | 		}
  538 | 		catch (NoSuchBeanDefinitionException ex) {
  539 | 			if (BeanFactoryUtils.isFactoryDereference(beanName)) {
  540 | 				return getBeanDefinition(BeanFactoryUtils.transformedBeanName(beanName), beanFactory);
  541 | 			}
  542 | 		}
  543 | 		return null;
  544 | 	}
  545 | 
  546 | 	/**
  547 | 	 * A search specification extracted from the underlying annotation.
  548 | 	 */
  549 | 	private static class Spec<A extends Annotation> {
  550 | 
  551 | 		private final ConditionContext context;
  552 | 
  553 | 		private final Class<? extends Annotation> annotationType;
  554 | 
  555 | 		private final Set<String> names;
  556 | 
  557 | 		private final Set<BeanType> types;
  558 | 
  559 | 		private final Set<String> annotations;
  560 | 
  561 | 		private final Set<BeanType> ignoredTypes;
  562 | 
  563 | 		private final Set<BeanType> parameterizedContainers;
  564 | 
  565 | 		private final @Nullable SearchStrategy strategy;
  566 | 
  567 | 		Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations,
  568 | 				Class<A> annotationType) {
  569 | 			MultiValueMap<String, @Nullable Object> attributes = annotations.stream(annotationType)
  570 | 				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
  571 | 				.collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
  572 | 			MergedAnnotation<A> annotation = annotations.get(annotationType);
  573 | 			this.context = context;
  574 | 			this.annotationType = annotationType;
  575 | 			this.names = extract(attributes, "name");
  576 | 			this.annotations = extract(attributes, "annotation");
  577 | 			this.ignoredTypes = resolveWhenPossible(extract(attributes, "ignored", "ignoredType"));
  578 | 			this.parameterizedContainers = resolveWhenPossible(extract(attributes, "parameterizedContainer"));
  579 | 			this.strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
  580 | 			Set<BeanType> types = resolveWhenPossible(extractTypes(attributes));
  581 | 			BeanTypeDeductionException deductionException = null;
  582 | 			if (types.isEmpty() && this.names.isEmpty() && this.annotations.isEmpty()) {
  583 | 				try {
  584 | 					types = deducedBeanType(context, metadata);
  585 | 				}
  586 | 				catch (BeanTypeDeductionException ex) {
  587 | 					deductionException = ex;
  588 | 				}
  589 | 			}
  590 | 			this.types = types;
  591 | 			validate(deductionException);
  592 | 		}
  593 | 
  594 | 		protected Set<String> extractTypes(@Nullable MultiValueMap<String, @Nullable Object> attributes) {
  595 | 			return extract(attributes, "value", "type");
  596 | 		}
  597 | 
  598 | 		private Set<String> extract(@Nullable MultiValueMap<String, @Nullable Object> attributes,
  599 | 				String... attributeNames) {
  600 | 			if (CollectionUtils.isEmpty(attributes)) {
  601 | 				return Collections.emptySet();
  602 | 			}
  603 | 			Set<String> result = new LinkedHashSet<>();
  604 | 			for (String attributeName : attributeNames) {
  605 | 				List<@Nullable Object> values = attributes.getOrDefault(attributeName, Collections.emptyList());
  606 | 				for (Object value : values) {
  607 | 					if (value instanceof String[] stringArray) {
  608 | 						merge(result, stringArray);
  609 | 					}
  610 | 					else if (value instanceof String string) {
  611 | 						merge(result, string);
  612 | 					}
  613 | 				}
  614 | 			}
  615 | 			return result.isEmpty() ? Collections.emptySet() : result;
  616 | 		}
  617 | 
  618 | 		private void merge(Set<String> result, String... additional) {
  619 | 			Collections.addAll(result, additional);
  620 | 		}
  621 | 
  622 | 		private Set<BeanType> resolveWhenPossible(Set<String> classNames) {
  623 | 			if (classNames.isEmpty()) {
  624 | 				return Collections.emptySet();
  625 | 			}
  626 | 			Set<BeanType> resolved = new LinkedHashSet<>(classNames.size());
  627 | 			for (String className : classNames) {
  628 | 				try {
  629 | 					Class<?> type = resolve(className, this.context.getClassLoader());
  630 | 					resolved.add(new BeanType(className, ResolvableType.forRawClass(type)));
  631 | 				}
  632 | 				catch (ClassNotFoundException | NoClassDefFoundError ex) {
  633 | 					resolved.add(new BeanType(className, ResolvableType.NONE));
  634 | 				}
  635 | 			}
  636 | 			return resolved;
  637 | 		}
  638 | 
  639 | 		protected void validate(@Nullable BeanTypeDeductionException ex) {
  640 | 			if (!hasAtLeastOneElement(getTypes(), getNames(), getAnnotations())) {
  641 | 				String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
  642 | 				if (ex == null) {
  643 | 					throw new IllegalStateException(message);
  644 | 				}
  645 | 				throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
  646 | 			}
  647 | 		}
  648 | 
  649 | 		private boolean hasAtLeastOneElement(Set<?>... sets) {
  650 | 			for (Set<?> set : sets) {
  651 | 				if (!set.isEmpty()) {
  652 | 					return true;
  653 | 				}
  654 | 			}
  655 | 			return false;
  656 | 		}
  657 | 
  658 | 		protected final String getAnnotationName() {
  659 | 			return "@" + ClassUtils.getShortName(this.annotationType);
  660 | 		}
  661 | 
  662 | 		private Set<BeanType> deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
  663 | 			if (metadata instanceof MethodMetadata methodMetadata && metadata.isAnnotated(Bean.class.getName())) {
  664 | 				return deducedBeanTypeForBeanMethod(context, methodMetadata);
  665 | 			}
  666 | 			return Collections.emptySet();
  667 | 		}
  668 | 
  669 | 		private Set<BeanType> deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
  670 | 			try {
  671 | 				return Set.of(getReturnType(context, metadata));
  672 | 			}
  673 | 			catch (Throwable ex) {
  674 | 				throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
  675 | 			}
  676 | 		}
  677 | 
  678 | 		private BeanType getReturnType(ConditionContext context, MethodMetadata metadata)
  679 | 				throws ClassNotFoundException, LinkageError {
  680 | 			// Safe to load at this point since we are in the REGISTER_BEAN phase
  681 | 			ClassLoader classLoader = context.getClassLoader();
  682 | 			ResolvableType returnType = getMethodReturnType(metadata, classLoader);
  683 | 			if (isParameterizedContainer(returnType.resolve())) {
  684 | 				returnType = returnType.getGeneric();
  685 | 			}
  686 | 			return new BeanType(returnType.toString(), returnType);
  687 | 		}
  688 | 
  689 | 		private boolean isParameterizedContainer(@Nullable Class<?> type) {
  690 | 			return (type != null) && this.parameterizedContainers.stream()
  691 | 				.map((beanType) -> beanType.resolvableType().resolve(type))
  692 | 				.anyMatch((container) -> container != null && container.isAssignableFrom(type));
  693 | 		}
  694 | 
  695 | 		private ResolvableType getMethodReturnType(MethodMetadata metadata, @Nullable ClassLoader classLoader)
  696 | 				throws ClassNotFoundException, LinkageError {
  697 | 			Class<?> declaringClass = resolve(metadata.getDeclaringClassName(), classLoader);
  698 | 			Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
  699 | 			return ResolvableType.forMethodReturnType(beanMethod);
  700 | 		}
  701 | 
  702 | 		private Method findBeanMethod(Class<?> declaringClass, String methodName) {
  703 | 			Method method = ReflectionUtils.findMethod(declaringClass, methodName);
  704 | 			if (isBeanMethod(method)) {
  705 | 				return method;
  706 | 			}
  707 | 			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(declaringClass);
  708 | 			for (Method candidate : candidates) {
  709 | 				if (candidate.getName().equals(methodName) && isBeanMethod(candidate)) {
  710 | 					return candidate;
  711 | 				}
  712 | 			}
  713 | 			throw new IllegalStateException("Unable to find bean method " + methodName);
  714 | 		}
  715 | 
  716 | 		@Contract("null -> false")
  717 | 		private boolean isBeanMethod(@Nullable Method method) {
  718 | 			return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
  719 | 				.isPresent(Bean.class);
  720 | 		}
  721 | 
  722 | 		private SearchStrategy getStrategy() {
  723 | 			return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
  724 | 		}
  725 | 
  726 | 		Set<BeanType> getTypes() {
  727 | 			return this.types;
  728 | 		}
  729 | 
  730 | 		private ConditionContext getContext() {
  731 | 			return this.context;
  732 | 		}
  733 | 
  734 | 		private Set<String> getNames() {
  735 | 			return this.names;
  736 | 		}
  737 | 
  738 | 		private Set<String> getAnnotations() {
  739 | 			return this.annotations;
  740 | 		}
  741 | 
  742 | 		private Set<BeanType> getIgnoredTypes() {
  743 | 			return this.ignoredTypes;
  744 | 		}
  745 | 
  746 | 		private Set<BeanType> getParameterizedContainers() {
  747 | 			return this.parameterizedContainers;
  748 | 		}
  749 | 
  750 | 		private Builder message() {
  751 | 			return ConditionMessage.forCondition(this.annotationType, this);
  752 | 		}
  753 | 
  754 | 		private Builder message(ConditionMessage message) {
  755 | 			return message.andCondition(this.annotationType, this);
  756 | 		}
  757 | 
  758 | 		@Override
  759 | 		public String toString() {
  760 | 			boolean hasNames = !this.names.isEmpty();
  761 | 			boolean hasTypes = !this.types.isEmpty();
  762 | 			boolean hasIgnoredTypes = !this.ignoredTypes.isEmpty();
  763 | 			StringBuilder string = new StringBuilder();
  764 | 			string.append("(");
  765 | 			if (hasNames) {
  766 | 				string.append("names: ");
  767 | 				string.append(StringUtils.collectionToCommaDelimitedString(this.names));
  768 | 				string.append(hasTypes ? " " : "; ");
  769 | 			}
  770 | 			if (hasTypes) {
  771 | 				string.append("types: ");
  772 | 				string.append(StringUtils.collectionToCommaDelimitedString(this.types));
  773 | 				string.append(hasIgnoredTypes ? " " : "; ");
  774 | 			}
  775 | 			if (hasIgnoredTypes) {
  776 | 				string.append("ignored: ");
  777 | 				string.append(StringUtils.collectionToCommaDelimitedString(this.ignoredTypes));
  778 | 				string.append("; ");
  779 | 			}
  780 | 			if (this.strategy != null) {
  781 | 				string.append("SearchStrategy: ");
  782 | 				string.append(this.strategy.toString().toLowerCase(Locale.ENGLISH));
  783 | 			}
  784 | 			string.append(")");
  785 | 			return string.toString();
  786 | 		}
  787 | 
  788 | 	}
  789 | 
  790 | 	/**
  791 | 	 * Specialized {@link Spec specification} for
  792 | 	 * {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
  793 | 	 */
  794 | 	private static class SingleCandidateSpec extends Spec<ConditionalOnSingleCandidate> {
  795 | 
  796 | 		private static final Collection<String> FILTERED_TYPES = Arrays.asList("", Object.class.getName());
  797 | 
  798 | 		SingleCandidateSpec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
  799 | 			super(context, metadata, annotations, ConditionalOnSingleCandidate.class);
  800 | 		}
  801 | 
  802 | 		@Override
  803 | 		protected Set<String> extractTypes(@Nullable MultiValueMap<String, @Nullable Object> attributes) {
  804 | 			Set<String> types = super.extractTypes(attributes);
  805 | 			types.removeAll(FILTERED_TYPES);
  806 | 			return types;
  807 | 		}
  808 | 
  809 | 		@Override
  810 | 		protected void validate(@Nullable BeanTypeDeductionException ex) {
  811 | 			Assert.isTrue(getTypes().size() == 1,
  812 | 					() -> getAnnotationName() + " annotations must specify only one type (got "
  813 | 							+ StringUtils.collectionToCommaDelimitedString(getTypes()) + ")");
  814 | 		}
  815 | 
  816 | 	}
  817 | 
  818 | 	/**
  819 | 	 * Results collected during the condition evaluation.
  820 | 	 */
  821 | 	private static final class MatchResult {
  822 | 
  823 | 		private final Map<String, Collection<String>> matchedAnnotations = new HashMap<>();
  824 | 
  825 | 		private final List<String> matchedNames = new ArrayList<>();
  826 | 
  827 | 		private final Map<String, Collection<String>> matchedTypes = new HashMap<>();
  828 | 
  829 | 		private final List<String> unmatchedAnnotations = new ArrayList<>();
  830 | 
  831 | 		private final List<String> unmatchedNames = new ArrayList<>();
  832 | 
  833 | 		private final List<String> unmatchedTypes = new ArrayList<>();
  834 | 
  835 | 		private final Set<String> namesOfAllMatches = new HashSet<>();
  836 | 
  837 | 		private void recordMatchedName(String name) {
  838 | 			this.matchedNames.add(name);
  839 | 			this.namesOfAllMatches.add(name);
  840 | 		}
  841 | 
  842 | 		private void recordUnmatchedName(String name) {
  843 | 			this.unmatchedNames.add(name);
  844 | 		}
  845 | 
  846 | 		private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
  847 | 			this.matchedAnnotations.put(annotation, matchingNames);
  848 | 			this.namesOfAllMatches.addAll(matchingNames);
  849 | 		}
  850 | 
  851 | 		private void recordUnmatchedAnnotation(String annotation) {
  852 | 			this.unmatchedAnnotations.add(annotation);
  853 | 		}
  854 | 
  855 | 		private void recordMatchedType(BeanType type, Collection<String> matchingNames) {
  856 | 			this.matchedTypes.put(type.name.toString(), matchingNames);
  857 | 			this.namesOfAllMatches.addAll(matchingNames);
  858 | 		}
  859 | 
  860 | 		private void recordUnmatchedType(BeanType type) {
  861 | 			this.unmatchedTypes.add(type.toString());
  862 | 		}
  863 | 
  864 | 		boolean isAllMatched() {
  865 | 			return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty()
  866 | 					&& this.unmatchedTypes.isEmpty();
  867 | 		}
  868 | 
  869 | 		boolean isAnyMatched() {
  870 | 			return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty())
  871 | 					|| (!this.matchedTypes.isEmpty());
  872 | 		}
  873 | 
  874 | 		Map<String, Collection<String>> getMatchedAnnotations() {
  875 | 			return this.matchedAnnotations;
  876 | 		}
  877 | 
  878 | 		List<String> getMatchedNames() {
  879 | 			return this.matchedNames;
  880 | 		}
  881 | 
  882 | 		Map<String, Collection<String>> getMatchedTypes() {
  883 | 			return this.matchedTypes;
  884 | 		}
  885 | 
  886 | 		List<String> getUnmatchedAnnotations() {
  887 | 			return this.unmatchedAnnotations;
  888 | 		}
  889 | 
  890 | 		List<String> getUnmatchedNames() {
  891 | 			return this.unmatchedNames;
  892 | 		}
  893 | 
  894 | 		List<String> getUnmatchedTypes() {
  895 | 			return this.unmatchedTypes;
  896 | 		}
  897 | 
  898 | 		Set<String> getNamesOfAllMatches() {
  899 | 			return this.namesOfAllMatches;
  900 | 		}
  901 | 
  902 | 	}
  903 | 
  904 | 	/**
  905 | 	 * Exception thrown when the bean type cannot be deduced.
  906 | 	 */
  907 | 	static final class BeanTypeDeductionException extends RuntimeException {
  908 | 
  909 | 		private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
  910 | 			super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
  911 | 		}
  912 | 
  913 | 	}
  914 | 
  915 | 	record BeanType(String name, ResolvableType resolvableType) {
  916 | 
  917 | 		@Override
  918 | 		public String toString() {
  919 | 			return ResolvableType.NONE.equals(this.resolvableType) ? this.name : this.resolvableType.toString();
  920 | 		}
  921 | 
  922 | 	}
  923 | 
  924 | }
```
