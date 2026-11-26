<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/EnableConfigurationPropertiesRegistrar.java -->
<!-- 正文引用：[[源码:EnableConfigurationPropertiesRegistrar.java#L起-L止]] ；代码块头部：(src: EnableConfigurationPropertiesRegistrar.java#L起-L止) -->

# EnableConfigurationPropertiesRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/EnableConfigurationPropertiesRegistrar.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.673Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.Set;
    9 | import java.util.stream.Collectors;
   10 | 
   11 | import org.springframework.beans.factory.config.BeanDefinition;
   12 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   13 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   14 | import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
   15 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   16 | import org.springframework.core.Conventions;
   17 | import org.springframework.core.annotation.MergedAnnotation;
   18 | import org.springframework.core.type.AnnotationMetadata;
   19 | 
   20 | /**
   21 |  * {@link ImportBeanDefinitionRegistrar} for
   22 |  * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @author Andy Wilkinson
   26 |  */
   27 | class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {
   28 | 
   29 | 	private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions
   30 | 		.getQualifiedAttributeName(EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");
   31 | 
   32 | 	@Override
   33 | 	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
   34 | 		registerInfrastructureBeans(registry);
   35 | 		registerMethodValidationExcludeFilter(registry);
   36 | 		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
   37 | 		getTypes(metadata).forEach(beanRegistrar::register);
   38 | 	}
   39 | 
   40 | 	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
   41 | 		return metadata.getAnnotations()
   42 | 			.stream(EnableConfigurationProperties.class)
   43 | 			.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
   44 | 			.filter((type) -> void.class != type)
   45 | 			.collect(Collectors.toSet());
   46 | 	}
   47 | 
   48 | 	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
   49 | 		ConfigurationPropertiesBindingPostProcessor.register(registry);
   50 | 		BoundConfigurationProperties.register(registry);
   51 | 	}
   52 | 
   53 | 	static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
   54 | 		if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
   55 | 			BeanDefinition definition = BeanDefinitionBuilder
   56 | 				.rootBeanDefinition(MethodValidationExcludeFilter.class, "byAnnotation")
   57 | 				.addConstructorArgValue(ConfigurationProperties.class)
   58 | 				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
   59 | 				.getBeanDefinition();
   60 | 			registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
   61 | 		}
   62 | 	}
   63 | 
   64 | }
```
