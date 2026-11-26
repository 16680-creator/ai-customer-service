<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesScanRegistrar.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesScanRegistrar.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesScanRegistrar.java#L起-L止) -->

# ConfigurationPropertiesScanRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesScanRegistrar.java` |
| 行数 | 115 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.789Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.LinkedHashSet;
    9 | import java.util.Set;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.beans.factory.BeanFactory;
   14 | import org.springframework.beans.factory.config.BeanDefinition;
   15 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   16 | import org.springframework.boot.context.TypeExcludeFilter;
   17 | import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
   18 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   19 | import org.springframework.core.annotation.AnnotationAttributes;
   20 | import org.springframework.core.annotation.MergedAnnotations;
   21 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   22 | import org.springframework.core.env.Environment;
   23 | import org.springframework.core.io.ResourceLoader;
   24 | import org.springframework.core.type.AnnotationMetadata;
   25 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   26 | import org.springframework.stereotype.Component;
   27 | import org.springframework.util.Assert;
   28 | import org.springframework.util.ClassUtils;
   29 | import org.springframework.util.StringUtils;
   30 | 
   31 | /**
   32 |  * {@link ImportBeanDefinitionRegistrar} for registering
   33 |  * {@link ConfigurationProperties @ConfigurationProperties} bean definitions through
   34 |  * scanning.
   35 |  *
   36 |  * @author Madhura Bhave
   37 |  * @author Phillip Webb
   38 |  */
   39 | class ConfigurationPropertiesScanRegistrar implements ImportBeanDefinitionRegistrar {
   40 | 
   41 | 	private final Environment environment;
   42 | 
   43 | 	private final @Nullable ResourceLoader resourceLoader;
   44 | 
   45 | 	ConfigurationPropertiesScanRegistrar(Environment environment, @Nullable ResourceLoader resourceLoader) {
   46 | 		this.environment = environment;
   47 | 		this.resourceLoader = resourceLoader;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
   52 | 		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
   53 | 		scan(registry, packagesToScan);
   54 | 	}
   55 | 
   56 | 	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
   57 | 		AnnotationAttributes attributes = AnnotationAttributes
   58 | 			.fromMap(metadata.getAnnotationAttributes(ConfigurationPropertiesScan.class.getName()));
   59 | 		Assert.state(attributes != null, "'attributes' must not be null");
   60 | 		String[] basePackages = attributes.getStringArray("basePackages");
   61 | 		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
   62 | 		Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
   63 | 		for (Class<?> basePackageClass : basePackageClasses) {
   64 | 			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
   65 | 		}
   66 | 		if (packagesToScan.isEmpty()) {
   67 | 			packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
   68 | 		}
   69 | 		packagesToScan.removeIf((candidate) -> !StringUtils.hasText(candidate));
   70 | 		return packagesToScan;
   71 | 	}
   72 | 
   73 | 	private void scan(BeanDefinitionRegistry registry, Set<String> packages) {
   74 | 		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
   75 | 		ClassPathScanningCandidateComponentProvider scanner = getScanner(registry);
   76 | 		for (String basePackage : packages) {
   77 | 			for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
   78 | 				String beanClassName = candidate.getBeanClassName();
   79 | 				Assert.state(beanClassName != null, "'beanClassName' must not be null");
   80 | 				register(registrar, beanClassName);
   81 | 			}
   82 | 		}
   83 | 	}
   84 | 
   85 | 	private ClassPathScanningCandidateComponentProvider getScanner(BeanDefinitionRegistry registry) {
   86 | 		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
   87 | 		scanner.setEnvironment(this.environment);
   88 | 		scanner.setResourceLoader(this.resourceLoader);
   89 | 		scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
   90 | 		TypeExcludeFilter typeExcludeFilter = new TypeExcludeFilter();
   91 | 		typeExcludeFilter.setBeanFactory((BeanFactory) registry);
   92 | 		scanner.addExcludeFilter(typeExcludeFilter);
   93 | 		return scanner;
   94 | 	}
   95 | 
   96 | 	private void register(ConfigurationPropertiesBeanRegistrar registrar, String className) throws LinkageError {
   97 | 		try {
   98 | 			register(registrar, ClassUtils.forName(className, null));
   99 | 		}
  100 | 		catch (ClassNotFoundException ex) {
  101 | 			// Ignore
  102 | 		}
  103 | 	}
  104 | 
  105 | 	private void register(ConfigurationPropertiesBeanRegistrar registrar, Class<?> type) {
  106 | 		if (!isComponent(type)) {
  107 | 			registrar.register(type);
  108 | 		}
  109 | 	}
  110 | 
  111 | 	private boolean isComponent(Class<?> type) {
  112 | 		return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).isPresent(Component.class);
  113 | 	}
  114 | 
  115 | }
```
