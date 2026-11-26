<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/method/MethodToolCallbackProvider.java -->
<!-- 正文引用：[[源码:MethodToolCallbackProvider.java#L起-L止]] ；代码块头部：(src: MethodToolCallbackProvider.java#L起-L止) -->

# MethodToolCallbackProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/method/MethodToolCallbackProvider.java` |
| 行数 | 162 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.411Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
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
   17 | package org.springframework.ai.tool.method;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.List;
   23 | import java.util.Objects;
   24 | import java.util.function.Consumer;
   25 | import java.util.function.Function;
   26 | import java.util.function.Supplier;
   27 | import java.util.stream.Collectors;
   28 | import java.util.stream.Stream;
   29 | 
   30 | import org.apache.commons.logging.Log;
   31 | import org.apache.commons.logging.LogFactory;
   32 | 
   33 | import org.springframework.ai.tool.ToolCallback;
   34 | import org.springframework.ai.tool.ToolCallbackProvider;
   35 | import org.springframework.ai.tool.annotation.Tool;
   36 | import org.springframework.ai.tool.metadata.ToolMetadata;
   37 | import org.springframework.ai.tool.support.ToolDefinitions;
   38 | import org.springframework.ai.tool.support.ToolUtils;
   39 | import org.springframework.aop.support.AopUtils;
   40 | import org.springframework.core.annotation.AnnotationUtils;
   41 | import org.springframework.util.Assert;
   42 | import org.springframework.util.ClassUtils;
   43 | import org.springframework.util.ReflectionUtils;
   44 | 
   45 | /**
   46 |  * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from
   47 |  * {@link Tool}-annotated methods.
   48 |  *
   49 |  * @author Thomas Vitale
   50 |  * @author Christian Tzolov
   51 |  * @since 1.0.0
   52 |  */
   53 | public final class MethodToolCallbackProvider implements ToolCallbackProvider {
   54 | 
   55 | 	private static final Log logger = LogFactory.getLog(MethodToolCallbackProvider.class);
   56 | 
   57 | 	private final List<Object> toolObjects;
   58 | 
   59 | 	private MethodToolCallbackProvider(List<Object> toolObjects) {
   60 | 		Assert.notNull(toolObjects, "toolObjects cannot be null");
   61 | 		Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
   62 | 		assertToolAnnotatedMethodsPresent(toolObjects);
   63 | 		this.toolObjects = toolObjects;
   64 | 		validateToolCallbacks(getToolCallbacks());
   65 | 	}
   66 | 
   67 | 	private void assertToolAnnotatedMethodsPresent(List<Object> toolObjects) {
   68 | 
   69 | 		for (Object toolObject : toolObjects) {
   70 | 			List<Method> toolMethods = Stream
   71 | 				.of(ReflectionUtils.getDeclaredMethods(
   72 | 						AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
   73 | 				.filter(this::isToolAnnotatedMethod)
   74 | 				.filter(toolMethod -> !isFunctionalType(toolMethod))
   75 | 				.toList();
   76 | 
   77 | 			if (toolMethods.isEmpty()) {
   78 | 				throw new IllegalArgumentException("No @Tool annotated methods found in " + toolObject + ". "
   79 | 						+ "Did you mean to pass a ToolCallback or ToolCallbackProvider? If so, use"
   80 | 						+ " .tools(toolCallback) or .toolCallbacks(toolCallback) instead.");
   81 | 			}
   82 | 		}
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public ToolCallback[] getToolCallbacks() {
   87 | 		var toolCallbacks = this.toolObjects.stream()
   88 | 			.map(toolObject -> Stream
   89 | 				.of(ReflectionUtils.getDeclaredMethods(
   90 | 						AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
   91 | 				.filter(this::isToolAnnotatedMethod)
   92 | 				.filter(toolMethod -> !isFunctionalType(toolMethod))
   93 | 				.filter(ReflectionUtils.USER_DECLARED_METHODS::matches)
   94 | 				.map(toolMethod -> MethodToolCallback.builder()
   95 | 					.toolDefinition(ToolDefinitions.from(toolMethod))
   96 | 					.toolMetadata(ToolMetadata.from(toolMethod))
   97 | 					.toolMethod(toolMethod)
   98 | 					.toolObject(toolObject)
   99 | 					.toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod))
  100 | 					.build())
  101 | 				.toArray(ToolCallback[]::new))
  102 | 			.flatMap(Stream::of)
  103 | 			.toArray(ToolCallback[]::new);
  104 | 
  105 | 		validateToolCallbacks(toolCallbacks);
  106 | 
  107 | 		return toolCallbacks;
  108 | 	}
  109 | 
  110 | 	private boolean isFunctionalType(Method toolMethod) {
  111 | 		var isFunction = ClassUtils.isAssignable(Function.class, toolMethod.getReturnType())
  112 | 				|| ClassUtils.isAssignable(Supplier.class, toolMethod.getReturnType())
  113 | 				|| ClassUtils.isAssignable(Consumer.class, toolMethod.getReturnType());
  114 | 
  115 | 		if (isFunction) {
  116 | 			if (logger.isWarnEnabled()) {
  117 | 				logger.warn("Method " + toolMethod.getName() + "is annotated with @Tool but returns a functional type. "
  118 | 						+ "This is not supported and the method will be ignored.");
  119 | 			}
  120 | 		}
  121 | 
  122 | 		return isFunction;
  123 | 	}
  124 | 
  125 | 	private boolean isToolAnnotatedMethod(Method method) {
  126 | 		Tool annotation = AnnotationUtils.findAnnotation(method, Tool.class);
  127 | 		return Objects.nonNull(annotation);
  128 | 	}
  129 | 
  130 | 	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
  131 | 		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
  132 | 		if (!duplicateToolNames.isEmpty()) {
  133 | 			throw new IllegalArgumentException("Multiple tools with the same name (%s) found in sources: %s".formatted(
  134 | 					String.join(", ", duplicateToolNames),
  135 | 					this.toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
  136 | 		}
  137 | 	}
  138 | 
  139 | 	public static Builder builder() {
  140 | 		return new Builder();
  141 | 	}
  142 | 
  143 | 	public static final class Builder {
  144 | 
  145 | 		private List<Object> toolObjects = new ArrayList<>();
  146 | 
  147 | 		private Builder() {
  148 | 		}
  149 | 
  150 | 		public Builder toolObjects(Object... toolObjects) {
  151 | 			Assert.notNull(toolObjects, "toolObjects cannot be null");
  152 | 			this.toolObjects = Arrays.asList(toolObjects);
  153 | 			return this;
  154 | 		}
  155 | 
  156 | 		public MethodToolCallbackProvider build() {
  157 | 			return new MethodToolCallbackProvider(this.toolObjects);
  158 | 		}
  159 | 
  160 | 	}
  161 | 
  162 | }
```
