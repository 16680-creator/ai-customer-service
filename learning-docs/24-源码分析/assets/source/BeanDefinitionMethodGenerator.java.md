<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/aot/BeanDefinitionMethodGenerator.java -->
<!-- 正文引用：[[源码:BeanDefinitionMethodGenerator.java#L起-L止]] ；代码块头部：(src: BeanDefinitionMethodGenerator.java#L起-L止) -->

# BeanDefinitionMethodGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/aot/BeanDefinitionMethodGenerator.java` |
| 行数 | 205 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.326Z |

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
   17 | package org.springframework.beans.factory.aot;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import javax.lang.model.element.Modifier;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aot.generate.GeneratedClass;
   26 | import org.springframework.aot.generate.GeneratedMethod;
   27 | import org.springframework.aot.generate.GeneratedMethods;
   28 | import org.springframework.aot.generate.GenerationContext;
   29 | import org.springframework.aot.generate.MethodReference;
   30 | import org.springframework.beans.factory.config.BeanDefinition;
   31 | import org.springframework.beans.factory.support.RegisteredBean;
   32 | import org.springframework.javapoet.ClassName;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * Generates a method that returns a {@link BeanDefinition} to be registered.
   37 |  *
   38 |  * @author Phillip Webb
   39 |  * @author Stephane Nicoll
   40 |  * @author Sebastien Deleuze
   41 |  * @since 6.0
   42 |  * @see BeanDefinitionMethodGeneratorFactory
   43 |  */
   44 | class BeanDefinitionMethodGenerator {
   45 | 
   46 | 	private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;
   47 | 
   48 | 	private final RegisteredBean registeredBean;
   49 | 
   50 | 	private final @Nullable String currentPropertyName;
   51 | 
   52 | 	private final List<BeanRegistrationAotContribution> aotContributions;
   53 | 
   54 | 
   55 | 	/**
   56 | 	 * Create a new {@link BeanDefinitionMethodGenerator} instance.
   57 | 	 * @param methodGeneratorFactory the method generator factory
   58 | 	 * @param registeredBean the registered bean
   59 | 	 * @param currentPropertyName the current property name
   60 | 	 * @param aotContributions the AOT contributions
   61 | 	 * @throws IllegalArgumentException if the bean definition defines an instance supplier since this can't be supported for code generation
   62 | 	 */
   63 | 	BeanDefinitionMethodGenerator(
   64 | 			BeanDefinitionMethodGeneratorFactory methodGeneratorFactory,
   65 | 			RegisteredBean registeredBean, @Nullable String currentPropertyName,
   66 | 			List<BeanRegistrationAotContribution> aotContributions) {
   67 | 
   68 | 		this.methodGeneratorFactory = methodGeneratorFactory;
   69 | 		this.registeredBean = registeredBean;
   70 | 		this.currentPropertyName = currentPropertyName;
   71 | 		this.aotContributions = aotContributions;
   72 | 	}
   73 | 
   74 | 
   75 | 	/**
   76 | 	 * Generate the method that returns the {@link BeanDefinition} to be registered.
   77 | 	 * @param generationContext the generation context
   78 | 	 * @param beanRegistrationsCode the bean registrations code
   79 | 	 * @return a reference to the generated method.
   80 | 	 */
   81 | 	MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
   82 | 			BeanRegistrationsCode beanRegistrationsCode) {
   83 | 
   84 | 		BeanRegistrationCodeFragments codeFragments = getCodeFragments(generationContext, beanRegistrationsCode);
   85 | 		ClassName target = codeFragments.getTarget(this.registeredBean);
   86 | 		if (isWritablePackageName(target)) {
   87 | 			GeneratedClass generatedClass = lookupGeneratedClass(generationContext, target);
   88 | 			GeneratedMethods generatedMethods = generatedClass.getMethods().withPrefix(getName());
   89 | 			GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
   90 | 					generatedClass.getName(), generatedMethods, codeFragments, Modifier.PUBLIC);
   91 | 			return generatedMethod.toMethodReference();
   92 | 		}
   93 | 		GeneratedMethods generatedMethods = beanRegistrationsCode.getMethods().withPrefix(getName());
   94 | 		GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
   95 | 				beanRegistrationsCode.getClassName(), generatedMethods, codeFragments, Modifier.PRIVATE);
   96 | 		return generatedMethod.toMethodReference();
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Specify if the {@link ClassName} belongs to a writable package.
  101 | 	 * @param target the target to check
  102 | 	 * @return {@code true} if generated code in that package is allowed
  103 | 	 */
  104 | 	private boolean isWritablePackageName(ClassName target) {
  105 | 		String packageName = target.packageName();
  106 | 		return (!packageName.startsWith("java.") && !packageName.startsWith("javax."));
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Return the {@link GeneratedClass} to use for the specified {@code target}.
  111 | 	 * <p>If the target class is an inner class, a corresponding inner class in
  112 | 	 * the original structure is created.
  113 | 	 * @param generationContext the generation context to use
  114 | 	 * @param target the chosen target class name for the bean definition
  115 | 	 * @return the generated class to use
  116 | 	 */
  117 | 	private static GeneratedClass lookupGeneratedClass(GenerationContext generationContext, ClassName target) {
  118 | 		ClassName topLevelClassName = target.topLevelClassName();
  119 | 		GeneratedClass generatedClass = generationContext.getGeneratedClasses()
  120 | 				.getOrAddForFeatureComponent("BeanDefinitions", topLevelClassName, type -> {
  121 | 					type.addJavadoc("Bean definitions for {@link $T}.", topLevelClassName);
  122 | 					type.addModifiers(Modifier.PUBLIC);
  123 | 				});
  124 | 
  125 | 		List<String> names = target.simpleNames();
  126 | 		if (names.size() == 1) {
  127 | 			return generatedClass;
  128 | 		}
  129 | 
  130 | 		List<String> namesToProcess = names.subList(1, names.size());
  131 | 		ClassName currentTargetClassName = topLevelClassName;
  132 | 		GeneratedClass tmp = generatedClass;
  133 | 		for (String nameToProcess : namesToProcess) {
  134 | 			currentTargetClassName = currentTargetClassName.nestedClass(nameToProcess);
  135 | 			tmp = createInnerClass(tmp, nameToProcess, currentTargetClassName);
  136 | 		}
  137 | 		return tmp;
  138 | 	}
  139 | 
  140 | 	private static GeneratedClass createInnerClass(GeneratedClass generatedClass, String name, ClassName target) {
  141 | 		return generatedClass.getOrAdd(name, type -> {
  142 | 			type.addJavadoc("Bean definitions for {@link $T}.", target);
  143 | 			type.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
  144 | 		});
  145 | 	}
  146 | 
  147 | 	private BeanRegistrationCodeFragments getCodeFragments(GenerationContext generationContext,
  148 | 			BeanRegistrationsCode beanRegistrationsCode) {
  149 | 
  150 | 		BeanRegistrationCodeFragments codeFragments = new DefaultBeanRegistrationCodeFragments(
  151 | 				beanRegistrationsCode, this.registeredBean, this.methodGeneratorFactory);
  152 | 		for (BeanRegistrationAotContribution aotContribution : this.aotContributions) {
  153 | 			codeFragments = aotContribution.customizeBeanRegistrationCodeFragments(generationContext, codeFragments);
  154 | 		}
  155 | 		return codeFragments;
  156 | 	}
  157 | 
  158 | 	private GeneratedMethod generateBeanDefinitionMethod(GenerationContext generationContext,
  159 | 			ClassName className, GeneratedMethods generatedMethods,
  160 | 			BeanRegistrationCodeFragments codeFragments, Modifier modifier) {
  161 | 
  162 | 		BeanRegistrationCodeGenerator codeGenerator = new BeanRegistrationCodeGenerator(
  163 | 				className, generatedMethods, this.registeredBean, codeFragments);
  164 | 
  165 | 		this.aotContributions.forEach(aotContribution -> aotContribution.applyTo(generationContext, codeGenerator));
  166 | 
  167 | 		CodeWarnings codeWarnings = new CodeWarnings();
  168 | 		codeWarnings.detectDeprecation(this.registeredBean.getBeanType());
  169 | 		return generatedMethods.add("getBeanDefinition", method -> {
  170 | 			method.addJavadoc("Get the $L definition for '$L'.",
  171 | 					(this.registeredBean.isInnerBean() ? "inner-bean" : "bean"),
  172 | 					getName());
  173 | 			method.addModifiers(modifier, Modifier.STATIC);
  174 | 			codeWarnings.suppress(method);
  175 | 			method.returns(BeanDefinition.class);
  176 | 			method.addCode(codeGenerator.generateCode(generationContext));
  177 | 		});
  178 | 	}
  179 | 
  180 | 	private String getName() {
  181 | 		if (this.currentPropertyName != null) {
  182 | 			return this.currentPropertyName;
  183 | 		}
  184 | 		if (!this.registeredBean.isGeneratedBeanName()) {
  185 | 			return getSimpleBeanName(this.registeredBean.getBeanName());
  186 | 		}
  187 | 		RegisteredBean nonGeneratedParent = this.registeredBean;
  188 | 		while (nonGeneratedParent != null && nonGeneratedParent.isGeneratedBeanName()) {
  189 | 			nonGeneratedParent = nonGeneratedParent.getParent();
  190 | 		}
  191 | 		if (nonGeneratedParent != null) {
  192 | 			return getSimpleBeanName(nonGeneratedParent.getBeanName()) + "InnerBean";
  193 | 		}
  194 | 		return "innerBean";
  195 | 	}
  196 | 
  197 | 	private String getSimpleBeanName(String beanName) {
  198 | 		int lastDot = beanName.lastIndexOf('.');
  199 | 		beanName = (lastDot != -1 ? beanName.substring(lastDot + 1) : beanName);
  200 | 		int lastDollar = beanName.lastIndexOf('$');
  201 | 		beanName = (lastDollar != -1 ? beanName.substring(lastDollar + 1) : beanName);
  202 | 		return StringUtils.uncapitalize(beanName);
  203 | 	}
  204 | 
  205 | }
```
