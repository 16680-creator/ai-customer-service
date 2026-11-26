<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesBeanRegistrationAotProcessor.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesBeanRegistrationAotProcessor.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesBeanRegistrationAotProcessor.java#L起-L止) -->

# ConfigurationPropertiesBeanRegistrationAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesBeanRegistrationAotProcessor.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:26:42.560Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.function.Predicate;
    8 | 
    9 | import javax.lang.model.element.Modifier;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.aot.generate.GeneratedMethod;
   14 | import org.springframework.aot.generate.GenerationContext;
   15 | import org.springframework.beans.factory.BeanFactory;
   16 | import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
   17 | import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
   18 | import org.springframework.beans.factory.aot.BeanRegistrationCode;
   19 | import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
   20 | import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
   21 | import org.springframework.beans.factory.config.BeanDefinition;
   22 | import org.springframework.beans.factory.support.InstanceSupplier;
   23 | import org.springframework.beans.factory.support.RegisteredBean;
   24 | import org.springframework.beans.factory.support.RootBeanDefinition;
   25 | import org.springframework.boot.context.properties.bind.BindMethod;
   26 | import org.springframework.javapoet.ClassName;
   27 | import org.springframework.javapoet.CodeBlock;
   28 | 
   29 | /**
   30 |  * {@link BeanRegistrationAotProcessor} for immutable configuration properties.
   31 |  *
   32 |  * @author Stephane Nicoll
   33 |  * @see ConstructorBound
   34 |  */
   35 | class ConfigurationPropertiesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {
   36 | 
   37 | 	@Override
   38 | 	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
   39 | 		if (!isImmutableConfigurationPropertiesBeanDefinition(registeredBean.getMergedBeanDefinition())) {
   40 | 			return null;
   41 | 		}
   42 | 		return BeanRegistrationAotContribution.withCustomCodeFragments(
   43 | 				(codeFragments) -> new ConfigurationPropertiesBeanRegistrationCodeFragments(codeFragments,
   44 | 						registeredBean));
   45 | 
   46 | 	}
   47 | 
   48 | 	private boolean isImmutableConfigurationPropertiesBeanDefinition(BeanDefinition beanDefinition) {
   49 | 		return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(beanDefinition));
   50 | 	}
   51 | 
   52 | 	private static class ConfigurationPropertiesBeanRegistrationCodeFragments
   53 | 			extends BeanRegistrationCodeFragmentsDecorator {
   54 | 
   55 | 		private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";
   56 | 
   57 | 		private final RegisteredBean registeredBean;
   58 | 
   59 | 		ConfigurationPropertiesBeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments,
   60 | 				RegisteredBean registeredBean) {
   61 | 			super(codeFragments);
   62 | 			this.registeredBean = registeredBean;
   63 | 		}
   64 | 
   65 | 		@Override
   66 | 		public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
   67 | 				BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
   68 | 				Predicate<String> attributeFilter) {
   69 | 			return super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode,
   70 | 					beanDefinition, attributeFilter.or(BindMethodAttribute.NAME::equals));
   71 | 		}
   72 | 
   73 | 		@Override
   74 | 		public ClassName getTarget(RegisteredBean registeredBean) {
   75 | 			return ClassName.get(this.registeredBean.getBeanClass());
   76 | 		}
   77 | 
   78 | 		@Override
   79 | 		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
   80 | 				BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
   81 | 			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
   82 | 				Class<?> beanClass = this.registeredBean.getBeanClass();
   83 | 				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
   84 | 					.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
   85 | 					.returns(beanClass)
   86 | 					.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME)
   87 | 					.addStatement("$T beanFactory = registeredBean.getBeanFactory()", BeanFactory.class)
   88 | 					.addStatement("$T beanName = registeredBean.getBeanName()", String.class)
   89 | 					.addStatement("$T<?> beanClass = registeredBean.getBeanClass()", Class.class)
   90 | 					.addStatement("return ($T) $T.from(beanFactory, beanName, beanClass)", beanClass,
   91 | 							ConstructorBound.class);
   92 | 			});
   93 | 			return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, beanRegistrationCode.getClassName(),
   94 | 					generatedMethod.getName());
   95 | 		}
   96 | 
   97 | 	}
   98 | 
   99 | }
```
