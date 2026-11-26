<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/method/MethodToolCallback.java -->
<!-- 正文引用：[[源码:MethodToolCallback.java#L起-L止]] ；代码块头部：(src: MethodToolCallback.java#L起-L止) -->

# MethodToolCallback.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/method/MethodToolCallback.java` |
| 行数 | 265 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.338Z |

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
   19 | import java.lang.reflect.InvocationTargetException;
   20 | import java.lang.reflect.Method;
   21 | import java.lang.reflect.Modifier;
   22 | import java.lang.reflect.Type;
   23 | import java.util.Map;
   24 | import java.util.stream.Stream;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | import tools.jackson.core.JacksonException;
   30 | 
   31 | import org.springframework.ai.chat.model.ToolContext;
   32 | import org.springframework.ai.tool.ToolCallback;
   33 | import org.springframework.ai.tool.definition.ToolDefinition;
   34 | import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
   35 | import org.springframework.ai.tool.execution.ToolCallResultConverter;
   36 | import org.springframework.ai.tool.execution.ToolExecutionException;
   37 | import org.springframework.ai.tool.metadata.ToolMetadata;
   38 | import org.springframework.ai.util.JsonHelper;
   39 | import org.springframework.core.ParameterizedTypeReference;
   40 | import org.springframework.util.Assert;
   41 | import org.springframework.util.ClassUtils;
   42 | import org.springframework.util.CollectionUtils;
   43 | 
   44 | /**
   45 |  * A {@link ToolCallback} implementation to invoke methods as tools.
   46 |  *
   47 |  * @author Thomas Vitale
   48 |  * @since 1.0.0
   49 |  */
   50 | public final class MethodToolCallback implements ToolCallback {
   51 | 
   52 | 	private static final JsonHelper jsonHelper = new JsonHelper();
   53 | 
   54 | 	private static final Log logger = LogFactory.getLog(MethodToolCallback.class);
   55 | 
   56 | 	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();
   57 | 
   58 | 	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();
   59 | 
   60 | 	private final ToolDefinition toolDefinition;
   61 | 
   62 | 	private final ToolMetadata toolMetadata;
   63 | 
   64 | 	private final Method toolMethod;
   65 | 
   66 | 	private final @Nullable Object toolObject;
   67 | 
   68 | 	private final ToolCallResultConverter toolCallResultConverter;
   69 | 
   70 | 	public MethodToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod,
   71 | 			@Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
   72 | 		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
   73 | 		Assert.notNull(toolMethod, "toolMethod cannot be null");
   74 | 		Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
   75 | 				"toolObject cannot be null for non-static methods");
   76 | 		this.toolDefinition = toolDefinition;
   77 | 		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
   78 | 		this.toolMethod = toolMethod;
   79 | 		this.toolObject = toolObject;
   80 | 		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
   81 | 				: DEFAULT_RESULT_CONVERTER;
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public ToolDefinition getToolDefinition() {
   86 | 		return this.toolDefinition;
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	public ToolMetadata getToolMetadata() {
   91 | 		return this.toolMetadata;
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	public String call(String toolInput) {
   96 | 		return call(toolInput, null);
   97 | 	}
   98 | 
   99 | 	@Override
  100 | 	public String call(String toolInput, @Nullable ToolContext toolContext) {
  101 | 		Assert.hasText(toolInput, "toolInput cannot be null or empty");
  102 | 
  103 | 		if (logger.isDebugEnabled()) {
  104 | 			logger.debug("Starting execution of tool: " + this.toolDefinition.name());
  105 | 		}
  106 | 
  107 | 		this.validateToolContextSupport(toolContext);
  108 | 
  109 | 		Map<String, Object> toolArguments = this.extractToolArguments(toolInput);
  110 | 		Assert.state(toolArguments != null, "toolArguments must not be null");
  111 | 
  112 | 		Object[] methodArguments = this.buildMethodArguments(toolArguments, toolContext);
  113 | 
  114 | 		Object result = this.callMethod(methodArguments);
  115 | 
  116 | 		if (logger.isDebugEnabled()) {
  117 | 			logger.debug("Successful execution of tool: " + this.toolDefinition.name());
  118 | 		}
  119 | 
  120 | 		Type returnType = this.toolMethod.getGenericReturnType();
  121 | 
  122 | 		return this.toolCallResultConverter.convert(result, returnType);
  123 | 	}
  124 | 
  125 | 	private void validateToolContextSupport(@Nullable ToolContext toolContext) {
  126 | 		var isNonEmptyToolContextProvided = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
  127 | 		var isToolContextAcceptedByMethod = Stream.of(this.toolMethod.getParameterTypes())
  128 | 			.anyMatch(type -> ClassUtils.isAssignable(ToolContext.class, type));
  129 | 		if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
  130 | 			throw new IllegalArgumentException("ToolContext is required by the method as an argument");
  131 | 		}
  132 | 	}
  133 | 
  134 | 	private @Nullable Map<String, Object> extractToolArguments(String toolInput) {
  135 | 		try {
  136 | 			return jsonHelper.fromJson(toolInput, new ParameterizedTypeReference<>() {
  137 | 			});
  138 | 		}
  139 | 		catch (Exception ex) {
  140 | 			logger.warn("Conversion from JSON failed", ex);
  141 | 			Throwable cause = (ex.getCause() instanceof JacksonException) ? ex.getCause() : ex;
  142 | 			throw new ToolExecutionException(this.getToolDefinition(), cause);
  143 | 		}
  144 | 	}
  145 | 
  146 | 	// Based on the implementation in MethodToolCallback.
  147 | 	@SuppressWarnings("null")
  148 | 	private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
  149 | 		return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
  150 | 			if (ClassUtils.isAssignable(ToolContext.class, parameter.getType())) {
  151 | 				return toolContext;
  152 | 			}
  153 | 			Object rawArgument = toolInputArguments.get(parameter.getName());
  154 | 			return buildTypedArgument(rawArgument, parameter.getParameterizedType());
  155 | 		}).toArray();
  156 | 	}
  157 | 
  158 | 	private @Nullable Object buildTypedArgument(@Nullable Object value, Type type) {
  159 | 		if (value == null) {
  160 | 			return null;
  161 | 		}
  162 | 		try {
  163 | 			if (type instanceof Class<?>) {
  164 | 				return jsonHelper.convertToTypedObject(value, (Class<?>) type);
  165 | 			}
  166 | 
  167 | 			// For generic types, use the fromJson method that accepts Type
  168 | 			String json = jsonHelper.toJson(value, true);
  169 | 			return jsonHelper.fromJson(json, type);
  170 | 		}
  171 | 		catch (Exception ex) {
  172 | 			logger.warn("Conversion from JSON failed", ex);
  173 | 			Throwable cause = (ex.getCause() instanceof JacksonException) ? ex.getCause() : ex;
  174 | 			throw new ToolExecutionException(this.getToolDefinition(), cause);
  175 | 		}
  176 | 	}
  177 | 
  178 | 	@SuppressWarnings("NullAway") // ex.getCause() is guaranteed to be non-null
  179 | 	private @Nullable Object callMethod(Object[] methodArguments) {
  180 | 		if (isObjectNotPublic() || isMethodNotPublic()) {
  181 | 			this.toolMethod.setAccessible(true);
  182 | 		}
  183 | 
  184 | 		Object result;
  185 | 		try {
  186 | 			result = this.toolMethod.invoke(this.toolObject, methodArguments);
  187 | 		}
  188 | 		catch (IllegalAccessException ex) {
  189 | 			throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
  190 | 		}
  191 | 		catch (InvocationTargetException ex) {
  192 | 			throw new ToolExecutionException(this.toolDefinition, ex.getCause());
  193 | 		}
  194 | 		return result;
  195 | 	}
  196 | 
  197 | 	private boolean isObjectNotPublic() {
  198 | 		return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
  199 | 	}
  200 | 
  201 | 	private boolean isMethodNotPublic() {
  202 | 		return !Modifier.isPublic(this.toolMethod.getModifiers());
  203 | 	}
  204 | 
  205 | 	@Override
  206 | 	public String toString() {
  207 | 		return "MethodToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
  208 | 				+ '}';
  209 | 	}
  210 | 
  211 | 	public static Builder builder() {
  212 | 		return new Builder();
  213 | 	}
  214 | 
  215 | 	public static final class Builder {
  216 | 
  217 | 		private @Nullable ToolDefinition toolDefinition;
  218 | 
  219 | 		private @Nullable ToolMetadata toolMetadata;
  220 | 
  221 | 		private @Nullable Method toolMethod;
  222 | 
  223 | 		private @Nullable Object toolObject;
  224 | 
  225 | 		private @Nullable ToolCallResultConverter toolCallResultConverter;
  226 | 
  227 | 		private Builder() {
  228 | 		}
  229 | 
  230 | 		public Builder toolDefinition(ToolDefinition toolDefinition) {
  231 | 			this.toolDefinition = toolDefinition;
  232 | 			return this;
  233 | 		}
  234 | 
  235 | 		public Builder toolMetadata(ToolMetadata toolMetadata) {
  236 | 			this.toolMetadata = toolMetadata;
  237 | 			return this;
  238 | 		}
  239 | 
  240 | 		public Builder toolMethod(Method toolMethod) {
  241 | 			this.toolMethod = toolMethod;
  242 | 			return this;
  243 | 		}
  244 | 
  245 | 		public Builder toolObject(Object toolObject) {
  246 | 			this.toolObject = toolObject;
  247 | 			return this;
  248 | 		}
  249 | 
  250 | 		public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
  251 | 			this.toolCallResultConverter = toolCallResultConverter;
  252 | 			return this;
  253 | 		}
  254 | 
  255 | 		@SuppressWarnings("null")
  256 | 		public MethodToolCallback build() {
  257 | 			Assert.state(this.toolDefinition != null, "ToolDefinition is required");
  258 | 			Assert.state(this.toolMethod != null, "ToolMethod is required");
  259 | 			return new MethodToolCallback(this.toolDefinition, this.toolMetadata, this.toolMethod, this.toolObject,
  260 | 					this.toolCallResultConverter);
  261 | 		}
  262 | 
  263 | 	}
  264 | 
  265 | }
```
