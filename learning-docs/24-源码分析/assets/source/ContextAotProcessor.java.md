<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/aot/ContextAotProcessor.java -->
<!-- 正文引用：[[源码:ContextAotProcessor.java#L起-L止]] ；代码块头部：(src: ContextAotProcessor.java#L起-L止) -->

# ContextAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/aot/ContextAotProcessor.java` |
| 行数 | 176 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:27.804Z |

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
   19 | import java.io.IOException;
   20 | import java.nio.file.Files;
   21 | import java.nio.file.Path;
   22 | import java.util.ArrayList;
   23 | import java.util.Collections;
   24 | import java.util.List;
   25 | 
   26 | import org.springframework.aot.generate.DefaultGenerationContext;
   27 | import org.springframework.aot.generate.FileSystemGeneratedFiles;
   28 | import org.springframework.aot.generate.NameGenerator;
   29 | import org.springframework.aot.hint.ExecutableMode;
   30 | import org.springframework.aot.hint.ReflectionHints;
   31 | import org.springframework.aot.hint.TypeReference;
   32 | import org.springframework.context.support.GenericApplicationContext;
   33 | import org.springframework.javapoet.ClassName;
   34 | import org.springframework.util.CollectionUtils;
   35 | 
   36 | /**
   37 |  * Filesystem-based ahead-of-time (AOT) processing base implementation.
   38 |  *
   39 |  * <p>Concrete implementations are typically used to kick off optimization of an
   40 |  * application in a build tool.
   41 |  *
   42 |  * @author Stephane Nicoll
   43 |  * @author Andy Wilkinson
   44 |  * @author Phillip Webb
   45 |  * @author Sam Brannen
   46 |  * @since 6.0
   47 |  * @see org.springframework.test.context.aot.TestAotProcessor
   48 |  */
   49 | public abstract class ContextAotProcessor extends AbstractAotProcessor<ClassName> {
   50 | 
   51 | 	private final Class<?> applicationClass;
   52 | 
   53 | 
   54 | 	/**
   55 | 	 * Create a new processor for the specified application entry point and
   56 | 	 * common settings.
   57 | 	 * @param applicationClass the application entry point (class with a {@code main()} method)
   58 | 	 * @param settings the settings to apply
   59 | 	 */
   60 | 	protected ContextAotProcessor(Class<?> applicationClass, Settings settings) {
   61 | 		super(settings);
   62 | 		this.applicationClass = applicationClass;
   63 | 	}
   64 | 
   65 | 
   66 | 	/**
   67 | 	 * Get the application entry point (typically a class with a {@code main()} method).
   68 | 	 */
   69 | 	protected Class<?> getApplicationClass() {
   70 | 		return this.applicationClass;
   71 | 	}
   72 | 
   73 | 
   74 | 	/**
   75 | 	 * Invoke the processing by clearing output directories first, followed by
   76 | 	 * {@link #performAotProcessing(GenericApplicationContext)}.
   77 | 	 * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
   78 | 	 * entry point
   79 | 	 */
   80 | 	@Override
   81 | 	protected ClassName doProcess() {
   82 | 		deleteExistingOutput();
   83 | 		try (GenericApplicationContext applicationContext = prepareApplicationContext(getApplicationClass())) {
   84 | 			return performAotProcessing(applicationContext);
   85 | 		}
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Prepare the {@link GenericApplicationContext} for the specified
   90 | 	 * application entry point to be used against an {@link ApplicationContextAotGenerator}.
   91 | 	 * @return a non-refreshed {@link GenericApplicationContext}
   92 | 	 */
   93 | 	protected abstract GenericApplicationContext prepareApplicationContext(Class<?> applicationClass);
   94 | 
   95 | 	/**
   96 | 	 * Perform ahead-of-time processing of the specified context.
   97 | 	 * <p>Code, resources, and generated classes are stored in the configured
   98 | 	 * output directories. In addition, run-time hints are registered for the
   99 | 	 * application and its entry point.
  100 | 	 * @param applicationContext the context to process
  101 | 	 */
  102 | 	protected ClassName performAotProcessing(GenericApplicationContext applicationContext) {
  103 | 		FileSystemGeneratedFiles generatedFiles = createFileSystemGeneratedFiles();
  104 | 		DefaultGenerationContext generationContext = new DefaultGenerationContext(
  105 | 				createNameGenerator(), generatedFiles);
  106 | 		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
  107 | 		ClassName generatedInitializerClassName = generator.processAheadOfTime(applicationContext, generationContext);
  108 | 		registerEntryPointHint(generationContext, generatedInitializerClassName);
  109 | 		generationContext.writeGeneratedContent();
  110 | 		writeHints(generationContext.getRuntimeHints());
  111 | 		writeNativeImageProperties(getDefaultNativeImageArguments(getApplicationClass().getName()));
  112 | 		return generatedInitializerClassName;
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Callback to customize the {@link NameGenerator}.
  117 | 	 * <p>By default, a standard {@link NameGenerator} using the configured
  118 | 	 * {@linkplain #getApplicationClass() application entry point} as the default
  119 | 	 * target is used.
  120 | 	 * @return the name generator
  121 | 	 * @since 7.1
  122 | 	 */
  123 | 	protected NameGenerator createNameGenerator() {
  124 | 		return new NameGenerator(ClassName.get(getApplicationClass()));
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Return the native image arguments to use.
  129 | 	 * <p>By default, the main class to use, as well as standard application flags
  130 | 	 * are added.
  131 | 	 * <p>If the returned list is empty, no {@code native-image.properties} is
  132 | 	 * contributed.
  133 | 	 * @param applicationClassName the fully qualified class name of the application
  134 | 	 * entry point
  135 | 	 * @return the native image options to contribute
  136 | 	 */
  137 | 	protected List<String> getDefaultNativeImageArguments(String applicationClassName) {
  138 | 		List<String> args = new ArrayList<>();
  139 | 		args.add("-H:Class=" + applicationClassName);
  140 | 		args.add("--no-fallback");
  141 | 		return args;
  142 | 	}
  143 | 
  144 | 	private void registerEntryPointHint(DefaultGenerationContext generationContext,
  145 | 			ClassName generatedInitializerClassName) {
  146 | 
  147 | 		TypeReference generatedType = TypeReference.of(generatedInitializerClassName.canonicalName());
  148 | 		TypeReference applicationType = TypeReference.of(getApplicationClass());
  149 | 		ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
  150 | 		reflection.registerType(applicationType);
  151 | 		reflection.registerType(generatedType, typeHint -> typeHint.onReachableType(applicationType)
  152 | 				.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
  153 | 	}
  154 | 
  155 | 	private void writeNativeImageProperties(List<String> args) {
  156 | 		if (CollectionUtils.isEmpty(args)) {
  157 | 			return;
  158 | 		}
  159 | 		StringBuilder sb = new StringBuilder();
  160 | 		sb.append("Args = ");
  161 | 		sb.append(String.join(String.format(" \\%n"), args));
  162 | 		Path file = getSettings().getResourceOutput().resolve("META-INF/native-image/" +
  163 | 				getSettings().getGroupId() + "/" + getSettings().getArtifactId() + "/native-image.properties");
  164 | 		try {
  165 | 			if (!Files.exists(file)) {
  166 | 				Files.createDirectories(file.getParent());
  167 | 				Files.createFile(file);
  168 | 			}
  169 | 			Files.writeString(file, sb.toString());
  170 | 		}
  171 | 		catch (IOException ex) {
  172 | 			throw new IllegalStateException("Failed to write native-image.properties", ex);
  173 | 		}
  174 | 	}
  175 | 
  176 | }
```
