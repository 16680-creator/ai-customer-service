<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/aot/AotApplicationContextInitializer.java -->
<!-- 正文引用：[[源码:AotApplicationContextInitializer.java#L起-L止]] ；代码块头部：(src: AotApplicationContextInitializer.java#L起-L止) -->

# AotApplicationContextInitializer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/aot/AotApplicationContextInitializer.java` |
| 行数 | 90 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:27.679Z |

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
   19 | import org.apache.commons.logging.Log;
   20 | import org.apache.commons.logging.LogFactory;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.BeanInstantiationException;
   24 | import org.springframework.beans.BeanUtils;
   25 | import org.springframework.context.ApplicationContextInitializer;
   26 | import org.springframework.context.ConfigurableApplicationContext;
   27 | import org.springframework.core.log.LogMessage;
   28 | import org.springframework.util.Assert;
   29 | import org.springframework.util.ClassUtils;
   30 | 
   31 | /**
   32 |  * Specialized {@link ApplicationContextInitializer} used to initialize a
   33 |  * {@link ConfigurableApplicationContext} using artifacts that were generated
   34 |  * ahead-of-time.
   35 |  * <p>
   36 |  * Instances of this initializer are usually created using
   37 |  * {@link #forInitializerClasses(String...)}, passing in the names of code
   38 |  * generated initializer classes.
   39 |  *
   40 |  * @author Stephane Nicoll
   41 |  * @author Phillip Webb
   42 |  * @since 6.0
   43 |  * @param <C> the application context type
   44 |  */
   45 | @FunctionalInterface
   46 | public interface AotApplicationContextInitializer<C extends ConfigurableApplicationContext>
   47 | 		extends ApplicationContextInitializer<C> {
   48 | 
   49 | 	/**
   50 | 	 * Factory method to create a new {@link AotApplicationContextInitializer}
   51 | 	 * instance that delegates to other initializers loaded from the given set
   52 | 	 * of class names.
   53 | 	 * @param <C> the application context type
   54 | 	 * @param initializerClassNames the class names of the initializers to load
   55 | 	 * @return a new {@link AotApplicationContextInitializer} instance
   56 | 	 */
   57 | 	static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> forInitializerClasses(
   58 | 			String... initializerClassNames) {
   59 | 
   60 | 		Assert.noNullElements(initializerClassNames, "'initializerClassNames' must not contain null elements");
   61 | 		return applicationContext -> initialize(applicationContext, initializerClassNames);
   62 | 	}
   63 | 
   64 | 	private static <C extends ConfigurableApplicationContext> void initialize(
   65 | 			C applicationContext, String... initializerClassNames) {
   66 | 
   67 | 		Log logger = LogFactory.getLog(AotApplicationContextInitializer.class);
   68 | 		ClassLoader classLoader = applicationContext.getClassLoader();
   69 | 		logger.debug("Initializing ApplicationContext with AOT");
   70 | 		for (String initializerClassName : initializerClassNames) {
   71 | 			logger.trace(LogMessage.format("Applying %s", initializerClassName));
   72 | 			instantiateInitializer(initializerClassName, classLoader).initialize(applicationContext);
   73 | 		}
   74 | 	}
   75 | 
   76 | 	@SuppressWarnings("unchecked")
   77 | 	static <C extends ConfigurableApplicationContext> ApplicationContextInitializer<C> instantiateInitializer(
   78 | 			String initializerClassName, @Nullable ClassLoader classLoader) {
   79 | 		try {
   80 | 			Class<?> initializerClass = ClassUtils.resolveClassName(initializerClassName, classLoader);
   81 | 			Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
   82 | 			return (ApplicationContextInitializer<C>) BeanUtils.instantiateClass(initializerClass);
   83 | 		}
   84 | 		catch (BeanInstantiationException ex) {
   85 | 			throw new IllegalArgumentException(
   86 | 					"Failed to instantiate ApplicationContextInitializer: " + initializerClassName, ex);
   87 | 		}
   88 | 	}
   89 | 
   90 | }
```
