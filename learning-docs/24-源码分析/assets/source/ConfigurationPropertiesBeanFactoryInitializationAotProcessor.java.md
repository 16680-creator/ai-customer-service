<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesBeanFactoryInitializationAotProcessor.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesBeanFactoryInitializationAotProcessor.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesBeanFactoryInitializationAotProcessor.java#L起-L止) -->

# ConfigurationPropertiesBeanFactoryInitializationAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesBeanFactoryInitializationAotProcessor.java` |
| 行数 | 73 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:26:42.663Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.List;
    9 | 
   10 | import org.jspecify.annotations.Nullable;
   11 | 
   12 | import org.springframework.aot.generate.GenerationContext;
   13 | import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
   14 | import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
   15 | import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
   16 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   17 | import org.springframework.boot.context.properties.bind.BindMethod;
   18 | import org.springframework.boot.context.properties.bind.Bindable;
   19 | import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
   20 | import org.springframework.util.ClassUtils;
   21 | 
   22 | /**
   23 |  * {@link BeanFactoryInitializationAotProcessor} that contributes runtime hints for
   24 |  * configuration properties-annotated beans.
   25 |  *
   26 |  * @author Stephane Nicoll
   27 |  * @author Christoph Strobl
   28 |  * @author Sebastien Deleuze
   29 |  * @author Andy Wilkinson
   30 |  */
   31 | class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {
   32 | 
   33 | 	@Override
   34 | 	public @Nullable ConfigurationPropertiesReflectionHintsContribution processAheadOfTime(
   35 | 			ConfigurableListableBeanFactory beanFactory) {
   36 | 		String[] beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
   37 | 		List<Bindable<?>> bindables = new ArrayList<>();
   38 | 		for (String beanName : beanNames) {
   39 | 			Class<?> beanType = beanFactory.getType(beanName, false);
   40 | 			if (beanType != null) {
   41 | 				BindMethod bindMethod = beanFactory.containsBeanDefinition(beanName)
   42 | 						? (BindMethod) beanFactory.getBeanDefinition(beanName).getAttribute(BindMethod.class.getName())
   43 | 						: null;
   44 | 				bindables.add(Bindable.of(ClassUtils.getUserClass(beanType))
   45 | 					.withBindMethod((bindMethod != null) ? bindMethod : BindMethod.JAVA_BEAN));
   46 | 			}
   47 | 		}
   48 | 		return (!bindables.isEmpty()) ? new ConfigurationPropertiesReflectionHintsContribution(bindables) : null;
   49 | 	}
   50 | 
   51 | 	static final class ConfigurationPropertiesReflectionHintsContribution
   52 | 			implements BeanFactoryInitializationAotContribution {
   53 | 
   54 | 		private final List<Bindable<?>> bindables;
   55 | 
   56 | 		private ConfigurationPropertiesReflectionHintsContribution(List<Bindable<?>> bindables) {
   57 | 			this.bindables = bindables;
   58 | 		}
   59 | 
   60 | 		@Override
   61 | 		public void applyTo(GenerationContext generationContext,
   62 | 				BeanFactoryInitializationCode beanFactoryInitializationCode) {
   63 | 			BindableRuntimeHintsRegistrar.forBindables(this.bindables)
   64 | 				.registerHints(generationContext.getRuntimeHints());
   65 | 		}
   66 | 
   67 | 		Iterable<Bindable<?>> getBindables() {
   68 | 			return this.bindables;
   69 | 		}
   70 | 
   71 | 	}
   72 | 
   73 | }
```
