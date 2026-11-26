<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConditionEvaluator.java -->
<!-- 正文引用：[[源码:ConditionEvaluator.java#L起-L止]] ；代码块头部：(src: ConditionEvaluator.java#L起-L止) -->

# ConditionEvaluator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConditionEvaluator.java` |
| 行数 | 234 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.679Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.beans.BeanUtils;
   26 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   27 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   28 | import org.springframework.context.ConfigurableApplicationContext;
   29 | import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
   30 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   31 | import org.springframework.core.env.Environment;
   32 | import org.springframework.core.env.EnvironmentCapable;
   33 | import org.springframework.core.env.StandardEnvironment;
   34 | import org.springframework.core.io.DefaultResourceLoader;
   35 | import org.springframework.core.io.ResourceLoader;
   36 | import org.springframework.core.type.AnnotatedTypeMetadata;
   37 | import org.springframework.core.type.AnnotationMetadata;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.ClassUtils;
   40 | import org.springframework.util.MultiValueMap;
   41 | 
   42 | /**
   43 |  * Internal class used to evaluate {@link Conditional} annotations.
   44 |  *
   45 |  * @author Phillip Webb
   46 |  * @author Juergen Hoeller
   47 |  * @since 4.0
   48 |  */
   49 | class ConditionEvaluator {
   50 | 
   51 | 	private final ConditionContextImpl context;
   52 | 
   53 | 
   54 | 	/**
   55 | 	 * Create a new {@link ConditionEvaluator} instance.
   56 | 	 */
   57 | 	public ConditionEvaluator(@Nullable BeanDefinitionRegistry registry,
   58 | 			@Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {
   59 | 
   60 | 		this.context = new ConditionContextImpl(registry, environment, resourceLoader);
   61 | 	}
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Determine if an item should be skipped based on {@code @Conditional} annotations.
   66 | 	 * The {@link ConfigurationPhase} will be deduced from the type of item (i.e. a
   67 | 	 * {@code @Configuration} class will be {@link ConfigurationPhase#PARSE_CONFIGURATION})
   68 | 	 * @param metadata the meta data
   69 | 	 * @return if the item should be skipped
   70 | 	 */
   71 | 	public boolean shouldSkip(AnnotatedTypeMetadata metadata) {
   72 | 		return shouldSkip(metadata, null);
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Determine if an item should be skipped based on {@code @Conditional} annotations.
   77 | 	 * @param metadata the meta data
   78 | 	 * @param phase the phase of the call
   79 | 	 * @return if the item should be skipped
   80 | 	 */
   81 | 	public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
   82 | 		if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {
   83 | 			return false;
   84 | 		}
   85 | 
   86 | 		if (phase == null) {
   87 | 			if (metadata instanceof AnnotationMetadata annotationMetadata &&
   88 | 					ConfigurationClassUtils.isConfigurationCandidate(annotationMetadata)) {
   89 | 				return shouldSkip(metadata, ConfigurationPhase.PARSE_CONFIGURATION);
   90 | 			}
   91 | 			return shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN);
   92 | 		}
   93 | 
   94 | 		List<Condition> conditions = collectConditions(metadata);
   95 | 		for (Condition condition : conditions) {
   96 | 			ConfigurationPhase requiredPhase = null;
   97 | 			if (condition instanceof ConfigurationCondition configurationCondition) {
   98 | 				requiredPhase = configurationCondition.getConfigurationPhase();
   99 | 			}
  100 | 			if ((requiredPhase == null || requiredPhase == phase) && !condition.matches(this.context, metadata)) {
  101 | 				return true;
  102 | 			}
  103 | 		}
  104 | 
  105 | 		return false;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Return the {@linkplain Condition conditions} that should be applied when
  110 | 	 * considering the given annotated type.
  111 | 	 * @param metadata the metadata of the annotated type
  112 | 	 * @return the ordered list of conditions for that type
  113 | 	 */
  114 | 	List<Condition> collectConditions(@Nullable AnnotatedTypeMetadata metadata) {
  115 | 		if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {
  116 | 			return Collections.emptyList();
  117 | 		}
  118 | 
  119 | 		List<Condition> conditions = new ArrayList<>();
  120 | 		for (String[] conditionClasses : getConditionClasses(metadata)) {
  121 | 			for (String conditionClass : conditionClasses) {
  122 | 				Condition condition = getCondition(conditionClass, this.context.getClassLoader());
  123 | 				conditions.add(condition);
  124 | 			}
  125 | 		}
  126 | 		AnnotationAwareOrderComparator.sort(conditions);
  127 | 		return conditions;
  128 | 	}
  129 | 
  130 | 	@SuppressWarnings("unchecked")
  131 | 	private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
  132 | 		MultiValueMap<String, @Nullable Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(), true);
  133 | 		Object values = (attributes != null ? attributes.get("value") : null);
  134 | 		return (List<String[]>) (values != null ? values : Collections.emptyList());
  135 | 	}
  136 | 
  137 | 	private Condition getCondition(String conditionClassName, @Nullable ClassLoader classloader) {
  138 | 		Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName, classloader);
  139 | 		return (Condition) BeanUtils.instantiateClass(conditionClass);
  140 | 	}
  141 | 
  142 | 
  143 | 	/**
  144 | 	 * Implementation of a {@link ConditionContext}.
  145 | 	 */
  146 | 	private static class ConditionContextImpl implements ConditionContext {
  147 | 
  148 | 		private final @Nullable BeanDefinitionRegistry registry;
  149 | 
  150 | 		private final @Nullable ConfigurableListableBeanFactory beanFactory;
  151 | 
  152 | 		private final Environment environment;
  153 | 
  154 | 		private final ResourceLoader resourceLoader;
  155 | 
  156 | 		private final @Nullable ClassLoader classLoader;
  157 | 
  158 | 		public ConditionContextImpl(@Nullable BeanDefinitionRegistry registry,
  159 | 				@Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {
  160 | 
  161 | 			this.registry = registry;
  162 | 			this.beanFactory = deduceBeanFactory(registry);
  163 | 			this.environment = (environment != null ? environment : deduceEnvironment(registry));
  164 | 			this.resourceLoader = (resourceLoader != null ? resourceLoader : deduceResourceLoader(registry));
  165 | 			this.classLoader = deduceClassLoader(resourceLoader, this.beanFactory);
  166 | 		}
  167 | 
  168 | 		private static @Nullable ConfigurableListableBeanFactory deduceBeanFactory(@Nullable BeanDefinitionRegistry source) {
  169 | 			if (source instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
  170 | 				return configurableListableBeanFactory;
  171 | 			}
  172 | 			if (source instanceof ConfigurableApplicationContext configurableApplicationContext) {
  173 | 				return configurableApplicationContext.getBeanFactory();
  174 | 			}
  175 | 			return null;
  176 | 		}
  177 | 
  178 | 		private static Environment deduceEnvironment(@Nullable BeanDefinitionRegistry source) {
  179 | 			if (source instanceof EnvironmentCapable environmentCapable) {
  180 | 				return environmentCapable.getEnvironment();
  181 | 			}
  182 | 			return new StandardEnvironment();
  183 | 		}
  184 | 
  185 | 		private static ResourceLoader deduceResourceLoader(@Nullable BeanDefinitionRegistry source) {
  186 | 			if (source instanceof ResourceLoader resourceLoader) {
  187 | 				return resourceLoader;
  188 | 			}
  189 | 			return new DefaultResourceLoader();
  190 | 		}
  191 | 
  192 | 		private static @Nullable ClassLoader deduceClassLoader(@Nullable ResourceLoader resourceLoader,
  193 | 				@Nullable ConfigurableListableBeanFactory beanFactory) {
  194 | 
  195 | 			if (resourceLoader != null) {
  196 | 				ClassLoader classLoader = resourceLoader.getClassLoader();
  197 | 				if (classLoader != null) {
  198 | 					return classLoader;
  199 | 				}
  200 | 			}
  201 | 			if (beanFactory != null) {
  202 | 				return beanFactory.getBeanClassLoader();
  203 | 			}
  204 | 			return ClassUtils.getDefaultClassLoader();
  205 | 		}
  206 | 
  207 | 		@Override
  208 | 		public BeanDefinitionRegistry getRegistry() {
  209 | 			Assert.state(this.registry != null, "No BeanDefinitionRegistry available");
  210 | 			return this.registry;
  211 | 		}
  212 | 
  213 | 		@Override
  214 | 		public @Nullable ConfigurableListableBeanFactory getBeanFactory() {
  215 | 			return this.beanFactory;
  216 | 		}
  217 | 
  218 | 		@Override
  219 | 		public Environment getEnvironment() {
  220 | 			return this.environment;
  221 | 		}
  222 | 
  223 | 		@Override
  224 | 		public ResourceLoader getResourceLoader() {
  225 | 			return this.resourceLoader;
  226 | 		}
  227 | 
  228 | 		@Override
  229 | 		public @Nullable ClassLoader getClassLoader() {
  230 | 			return this.classLoader;
  231 | 		}
  232 | 	}
  233 | 
  234 | }
```
