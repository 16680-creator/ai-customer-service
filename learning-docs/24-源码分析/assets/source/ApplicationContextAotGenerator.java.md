<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/aot/ApplicationContextAotGenerator.java -->
<!-- 正文引用：[[源码:ApplicationContextAotGenerator.java#L起-L止]] ；代码块头部：(src: ApplicationContextAotGenerator.java#L起-L止) -->

# ApplicationContextAotGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/aot/ApplicationContextAotGenerator.java` |
| 行数 | 76 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:27.565Z |

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
   17 | package org.springframework.context.aot;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.springframework.aot.generate.GenerationContext;
   22 | import org.springframework.beans.factory.BeanFactory;
   23 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   24 | import org.springframework.cglib.core.ReflectUtils;
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.context.ApplicationContextInitializer;
   27 | import org.springframework.context.support.GenericApplicationContext;
   28 | import org.springframework.javapoet.ClassName;
   29 | 
   30 | /**
   31 |  * Process an {@link ApplicationContext} and its {@link BeanFactory} to generate
   32 |  * code that represents the state of the bean factory, as well as the necessary
   33 |  * hints that can be used at runtime in a constrained environment.
   34 |  *
   35 |  * @author Stephane Nicoll
   36 |  * @author Phillip Webb
   37 |  * @since 6.0
   38 |  */
   39 | public class ApplicationContextAotGenerator {
   40 | 
   41 | 	/**
   42 | 	 * Process the specified {@link GenericApplicationContext} ahead-of-time using
   43 | 	 * the specified {@link GenerationContext}.
   44 | 	 * <p>Return the {@link ClassName} of the {@link ApplicationContextInitializer}
   45 | 	 * to use to restore an optimized state of the application context.
   46 | 	 * @param applicationContext the non-refreshed application context to process
   47 | 	 * @param generationContext the generation context to use
   48 | 	 * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
   49 | 	 * entry point
   50 | 	 */
   51 | 	public ClassName processAheadOfTime(GenericApplicationContext applicationContext,
   52 | 			GenerationContext generationContext) {
   53 | 
   54 | 		return withCglibClassHandler(new CglibClassHandler(generationContext), () -> {
   55 | 			applicationContext.refreshForAotProcessing(generationContext.getRuntimeHints());
   56 | 			ApplicationContextInitializationCodeGenerator codeGenerator =
   57 | 					new ApplicationContextInitializationCodeGenerator(applicationContext, generationContext);
   58 | 			DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
   59 | 			new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext, codeGenerator);
   60 | 			return codeGenerator.getClassName();
   61 | 		});
   62 | 	}
   63 | 
   64 | 	private <T> T withCglibClassHandler(CglibClassHandler cglibClassHandler, Supplier<T> task) {
   65 | 		try {
   66 | 			ReflectUtils.setLoadedClassHandler(cglibClassHandler::handleLoadedClass);
   67 | 			ReflectUtils.setGeneratedClassHandler(cglibClassHandler::handleGeneratedClass);
   68 | 			return task.get();
   69 | 		}
   70 | 		finally {
   71 | 			ReflectUtils.setLoadedClassHandler(null);
   72 | 			ReflectUtils.setGeneratedClassHandler(null);
   73 | 		}
   74 | 	}
   75 | 
   76 | }
```
