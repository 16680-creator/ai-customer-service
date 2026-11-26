<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClassUtils.java -->
<!-- 正文引用：[[源码:ConfigurationClassUtils.java#L起-L止]] ；代码块头部：(src: ConfigurationClassUtils.java#L起-L止) -->

# ConfigurationClassUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClassUtils.java` |
| 行数 | 228 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.868Z |

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
   20 | import java.util.Map;
   21 | import java.util.Set;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.aop.framework.AopInfrastructureBean;
   28 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   29 | import org.springframework.beans.factory.config.BeanDefinition;
   30 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   31 | import org.springframework.beans.factory.config.BeanPostProcessor;
   32 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   33 | import org.springframework.cglib.proxy.Enhancer;
   34 | import org.springframework.context.event.EventListenerFactory;
   35 | import org.springframework.core.Conventions;
   36 | import org.springframework.core.Ordered;
   37 | import org.springframework.core.annotation.AnnotationUtils;
   38 | import org.springframework.core.annotation.Order;
   39 | import org.springframework.core.type.AnnotationMetadata;
   40 | import org.springframework.core.type.classreading.MetadataReader;
   41 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   42 | import org.springframework.stereotype.Component;
   43 | 
   44 | /**
   45 |  * Utilities for identifying and configuring {@link Configuration} classes.
   46 |  *
   47 |  * @author Chris Beams
   48 |  * @author Juergen Hoeller
   49 |  * @author Sam Brannen
   50 |  * @author Stephane Nicoll
   51 |  * @since 6.0
   52 |  */
   53 | public abstract class ConfigurationClassUtils {
   54 | 
   55 | 	static final String CONFIGURATION_CLASS_FULL = "full";
   56 | 
   57 | 	static final String CONFIGURATION_CLASS_LITE = "lite";
   58 | 
   59 | 	/**
   60 | 	 * When set to {@link Boolean#TRUE}, this attribute signals that the bean class
   61 | 	 * for the given {@link BeanDefinition} should be considered as a candidate
   62 | 	 * configuration class in 'lite' mode by default.
   63 | 	 * <p>For example, a class registered directly with an {@code ApplicationContext}
   64 | 	 * should always be considered a configuration class candidate.
   65 | 	 * @since 6.0.10
   66 | 	 */
   67 | 	static final String CANDIDATE_ATTRIBUTE =
   68 | 			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "candidate");
   69 | 
   70 | 	static final String CONFIGURATION_CLASS_ATTRIBUTE =
   71 | 			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");
   72 | 
   73 | 	static final String ORDER_ATTRIBUTE =
   74 | 			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");
   75 | 
   76 | 
   77 | 	private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);
   78 | 
   79 | 	private static final Set<String> candidateIndicators = Set.of(
   80 | 			Component.class.getName(),
   81 | 			ComponentScan.class.getName(),
   82 | 			Import.class.getName(),
   83 | 			ImportResource.class.getName());
   84 | 
   85 | 
   86 | 	/**
   87 | 	 * Initialize a configuration class proxy for the specified class.
   88 | 	 * @param userClass the configuration class to initialize
   89 | 	 */
   90 | 	@SuppressWarnings("unused") // Used by AOT-optimized generated code
   91 | 	public static Class<?> initializeConfigurationClass(Class<?> userClass) {
   92 | 		Class<?> configurationClass = new ConfigurationClassEnhancer().enhance(userClass, null);
   93 | 		Enhancer.registerStaticCallbacks(configurationClass, ConfigurationClassEnhancer.CALLBACKS);
   94 | 		return configurationClass;
   95 | 	}
   96 | 
   97 | 
   98 | 	/**
   99 | 	 * Check whether the given bean definition is a candidate for a configuration class
  100 | 	 * (or a nested component class declared within a configuration/component class,
  101 | 	 * to be auto-registered as well), and mark it accordingly.
  102 | 	 * @param beanDef the bean definition to check
  103 | 	 * @param metadataReaderFactory the current factory in use by the caller
  104 | 	 * @return whether the candidate qualifies as (any kind of) configuration class
  105 | 	 */
  106 | 	static boolean checkConfigurationClassCandidate(
  107 | 			BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
  108 | 
  109 | 		String className = beanDef.getBeanClassName();
  110 | 		if (className == null || beanDef.getFactoryMethodName() != null) {
  111 | 			return false;
  112 | 		}
  113 | 
  114 | 		AnnotationMetadata metadata;
  115 | 		if (beanDef instanceof AnnotatedBeanDefinition annotatedBd &&
  116 | 				className.equals(annotatedBd.getMetadata().getClassName())) {
  117 | 			// Can reuse the pre-parsed metadata from the given BeanDefinition...
  118 | 			metadata = annotatedBd.getMetadata();
  119 | 		}
  120 | 		else if (beanDef instanceof AbstractBeanDefinition abstractBd && abstractBd.hasBeanClass()) {
  121 | 			// Check already loaded Class if present...
  122 | 			// since we possibly can't even load the class file for this Class.
  123 | 			Class<?> beanClass = abstractBd.getBeanClass();
  124 | 			if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass) ||
  125 | 					BeanPostProcessor.class.isAssignableFrom(beanClass) ||
  126 | 					AopInfrastructureBean.class.isAssignableFrom(beanClass) ||
  127 | 					EventListenerFactory.class.isAssignableFrom(beanClass)) {
  128 | 				return false;
  129 | 			}
  130 | 			metadata = AnnotationMetadata.introspect(beanClass);
  131 | 		}
  132 | 		else {
  133 | 			try {
  134 | 				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
  135 | 				metadata = metadataReader.getAnnotationMetadata();
  136 | 			}
  137 | 			catch (IOException ex) {
  138 | 				if (logger.isDebugEnabled()) {
  139 | 					logger.debug("Could not find class file for introspecting configuration annotations: " +
  140 | 							className, ex);
  141 | 				}
  142 | 				return false;
  143 | 			}
  144 | 		}
  145 | 
  146 | 		Map<String, @Nullable Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
  147 | 		if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
  148 | 			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
  149 | 		}
  150 | 		else if (config != null || Boolean.TRUE.equals(beanDef.getAttribute(CANDIDATE_ATTRIBUTE)) ||
  151 | 				isConfigurationCandidate(metadata)) {
  152 | 			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
  153 | 		}
  154 | 		else {
  155 | 			return false;
  156 | 		}
  157 | 
  158 | 		// It's a full or lite configuration candidate... Let's determine the order value, if any.
  159 | 		Integer order = getOrder(metadata);
  160 | 		if (order != null) {
  161 | 			beanDef.setAttribute(ORDER_ATTRIBUTE, order);
  162 | 		}
  163 | 
  164 | 		return true;
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Check the given metadata for a configuration class candidate
  169 | 	 * (or nested component class declared within a configuration/component class).
  170 | 	 * @param metadata the metadata of the annotated class
  171 | 	 * @return {@code true} if the given class is to be registered for
  172 | 	 * configuration class processing; {@code false} otherwise
  173 | 	 */
  174 | 	static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
  175 | 		// Do not consider an interface or an annotation...
  176 | 		if (metadata.isInterface()) {
  177 | 			return false;
  178 | 		}
  179 | 
  180 | 		// Any of the typical annotations found?
  181 | 		for (String indicator : candidateIndicators) {
  182 | 			if (metadata.isAnnotated(indicator)) {
  183 | 				return true;
  184 | 			}
  185 | 		}
  186 | 
  187 | 		// Finally, let's look for @Bean methods...
  188 | 		return hasBeanMethods(metadata);
  189 | 	}
  190 | 
  191 | 	static boolean hasBeanMethods(AnnotationMetadata metadata) {
  192 | 		try {
  193 | 			return metadata.hasAnnotatedMethods(Bean.class.getName());
  194 | 		}
  195 | 		catch (Throwable ex) {
  196 | 			if (logger.isDebugEnabled()) {
  197 | 				logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
  198 | 			}
  199 | 			return false;
  200 | 		}
  201 | 	}
  202 | 
  203 | 	/**
  204 | 	 * Determine the order for the given configuration class metadata.
  205 | 	 * @param metadata the metadata of the annotated class
  206 | 	 * @return the {@code @Order} annotation value on the configuration class,
  207 | 	 * or {@code Ordered.LOWEST_PRECEDENCE} if none declared
  208 | 	 * @since 5.0
  209 | 	 */
  210 | 	public static @Nullable Integer getOrder(AnnotationMetadata metadata) {
  211 | 		Map<String, @Nullable Object> orderAttributes = metadata.getAnnotationAttributes(Order.class.getName());
  212 | 		return (orderAttributes != null ? ((Integer) orderAttributes.get(AnnotationUtils.VALUE)) : null);
  213 | 	}
  214 | 
  215 | 	/**
  216 | 	 * Determine the order for the given configuration class bean definition,
  217 | 	 * as set by {@link #checkConfigurationClassCandidate}.
  218 | 	 * @param beanDef the bean definition to check
  219 | 	 * @return the {@link Order @Order} annotation value on the configuration class,
  220 | 	 * or {@link Ordered#LOWEST_PRECEDENCE} if none declared
  221 | 	 * @since 4.2
  222 | 	 */
  223 | 	public static int getOrder(BeanDefinition beanDef) {
  224 | 		Integer order = (Integer) beanDef.getAttribute(ORDER_ATTRIBUTE);
  225 | 		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
  226 | 	}
  227 | 
  228 | }
```
