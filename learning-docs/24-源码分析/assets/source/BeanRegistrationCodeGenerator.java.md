<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/aot/BeanRegistrationCodeGenerator.java -->
<!-- 正文引用：[[源码:BeanRegistrationCodeGenerator.java#L起-L止]] ；代码块头部：(src: BeanRegistrationCodeGenerator.java#L起-L止) -->

# BeanRegistrationCodeGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/aot/BeanRegistrationCodeGenerator.java` |
| 行数 | 93 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.005Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.function.Predicate;
   22 | 
   23 | import org.springframework.aot.generate.GeneratedMethods;
   24 | import org.springframework.aot.generate.GenerationContext;
   25 | import org.springframework.aot.generate.MethodReference;
   26 | import org.springframework.beans.factory.support.RegisteredBean;
   27 | import org.springframework.javapoet.ClassName;
   28 | import org.springframework.javapoet.CodeBlock;
   29 | import org.springframework.util.Assert;
   30 | 
   31 | /**
   32 |  * {@link BeanRegistrationCode} implementation with code generation support.
   33 |  *
   34 |  * @author Phillip Webb
   35 |  * @since 6.0
   36 |  */
   37 | class BeanRegistrationCodeGenerator implements BeanRegistrationCode {
   38 | 
   39 | 	private static final Predicate<String> REJECT_ALL_ATTRIBUTES_FILTER = attribute -> false;
   40 | 
   41 | 	private final ClassName className;
   42 | 
   43 | 	private final GeneratedMethods generatedMethods;
   44 | 
   45 | 	private final List<MethodReference> instancePostProcessors = new ArrayList<>();
   46 | 
   47 | 	private final RegisteredBean registeredBean;
   48 | 
   49 | 	private final BeanRegistrationCodeFragments codeFragments;
   50 | 
   51 | 
   52 | 	BeanRegistrationCodeGenerator(ClassName className, GeneratedMethods generatedMethods,
   53 | 			RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments) {
   54 | 
   55 | 		this.className = className;
   56 | 		this.generatedMethods = generatedMethods;
   57 | 		this.registeredBean = registeredBean;
   58 | 		this.codeFragments = codeFragments;
   59 | 	}
   60 | 
   61 | 
   62 | 	@Override
   63 | 	public ClassName getClassName() {
   64 | 		return this.className;
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	public GeneratedMethods getMethods() {
   69 | 		return this.generatedMethods;
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public void addInstancePostProcessor(MethodReference methodReference) {
   74 | 		Assert.notNull(methodReference, "'methodReference' must not be null");
   75 | 		this.instancePostProcessors.add(methodReference);
   76 | 	}
   77 | 
   78 | 	CodeBlock generateCode(GenerationContext generationContext) {
   79 | 		CodeBlock.Builder code = CodeBlock.builder();
   80 | 		code.add(this.codeFragments.generateNewBeanDefinitionCode(generationContext,
   81 | 				this.registeredBean.getBeanType(), this));
   82 | 		code.add(this.codeFragments.generateSetBeanDefinitionPropertiesCode(
   83 | 				generationContext, this, this.registeredBean.getMergedBeanDefinition(),
   84 | 				REJECT_ALL_ATTRIBUTES_FILTER));
   85 | 		CodeBlock instanceSupplierCode = this.codeFragments.generateInstanceSupplierCode(
   86 | 				generationContext, this, this.instancePostProcessors.isEmpty());
   87 | 		code.add(this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext,
   88 | 				this, instanceSupplierCode, this.instancePostProcessors));
   89 | 		code.add(this.codeFragments.generateReturnCode(generationContext, this));
   90 | 		return code.build();
   91 | 	}
   92 | 
   93 | }
```
