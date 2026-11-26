<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/support/ToolUtils.java -->
<!-- 正文引用：[[源码:ToolUtils.java#L起-L止]] ；代码块头部：(src: ToolUtils.java#L起-L止) -->

# ToolUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/support/ToolUtils.java` |
| 行数 | 150 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.797Z |

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
   17 | package org.springframework.ai.tool.support;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.regex.Pattern;
   24 | import java.util.stream.Collectors;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | 
   29 | import org.springframework.ai.tool.ToolCallback;
   30 | import org.springframework.ai.tool.annotation.Tool;
   31 | import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
   32 | import org.springframework.ai.tool.execution.ToolCallResultConverter;
   33 | import org.springframework.ai.util.ParsingUtils;
   34 | import org.springframework.core.annotation.AnnotatedElementUtils;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.util.StringUtils;
   37 | 
   38 | /**
   39 |  * Miscellaneous tool utility methods. Mainly for internal use within the framework.
   40 |  *
   41 |  * @author Thomas Vitale
   42 |  */
   43 | public final class ToolUtils {
   44 | 
   45 | 	private static final Log logger = LogFactory.getLog(ToolUtils.class);
   46 | 
   47 | 	/**
   48 | 	 * Regular expression pattern for recommended tool names. Tool names should contain
   49 | 	 * only alphanumeric characters, underscores, hyphens, and dots for maximum
   50 | 	 * compatibility across different LLMs.
   51 | 	 */
   52 | 	private static final Pattern RECOMMENDED_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\.-]+$");
   53 | 
   54 | 	private ToolUtils() {
   55 | 	}
   56 | 
   57 | 	public static String getToolName(Method method) {
   58 | 		Assert.notNull(method, "method cannot be null");
   59 | 		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
   60 | 		String toolName;
   61 | 		if (tool == null) {
   62 | 			toolName = method.getName();
   63 | 		}
   64 | 		else {
   65 | 			toolName = StringUtils.hasText(tool.name()) ? tool.name() : method.getName();
   66 | 		}
   67 | 		validateToolName(toolName);
   68 | 		return toolName;
   69 | 	}
   70 | 
   71 | 	public static String getToolDescriptionFromName(String toolName) {
   72 | 		Assert.hasText(toolName, "toolName cannot be null or empty");
   73 | 		return ParsingUtils.reConcatenateCamelCase(toolName, " ");
   74 | 	}
   75 | 
   76 | 	public static String getToolDescription(Method method) {
   77 | 		Assert.notNull(method, "method cannot be null");
   78 | 		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
   79 | 		if (tool == null) {
   80 | 			return ParsingUtils.reConcatenateCamelCase(method.getName(), " ");
   81 | 		}
   82 | 		return StringUtils.hasText(tool.description()) ? tool.description() : method.getName();
   83 | 	}
   84 | 
   85 | 	public static boolean getToolReturnDirect(Method method) {
   86 | 		Assert.notNull(method, "method cannot be null");
   87 | 		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
   88 | 		return tool != null && tool.returnDirect();
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Whether calls of the {@code @Tool} method need a person's confirmation before they
   93 | 	 * run.
   94 | 	 * @param method the tool method
   95 | 	 * @return {@code true} when the method's {@code @Tool} sets
   96 | 	 * {@code requiresConfirmation}
   97 | 	 * @since 2.1.0
   98 | 	 */
   99 | 	public static boolean getToolRequiresConfirmation(Method method) {
  100 | 		Assert.notNull(method, "method cannot be null");
  101 | 		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
  102 | 		return tool != null && tool.requiresConfirmation();
  103 | 	}
  104 | 
  105 | 	public static ToolCallResultConverter getToolCallResultConverter(Method method) {
  106 | 		Assert.notNull(method, "method cannot be null");
  107 | 		var tool = AnnotatedElementUtils.findMergedAnnotation(method, Tool.class);
  108 | 		if (tool == null) {
  109 | 			return new DefaultToolCallResultConverter();
  110 | 		}
  111 | 		var type = tool.resultConverter();
  112 | 		try {
  113 | 			return type.getDeclaredConstructor().newInstance();
  114 | 		}
  115 | 		catch (Exception e) {
  116 | 			throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + type, e);
  117 | 		}
  118 | 	}
  119 | 
  120 | 	public static List<String> getDuplicateToolNames(List<ToolCallback> toolCallbacks) {
  121 | 		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
  122 | 		return toolCallbacks.stream()
  123 | 			.collect(Collectors.groupingBy(toolCallback -> toolCallback.getToolDefinition().name(),
  124 | 					Collectors.counting()))
  125 | 			.entrySet()
  126 | 			.stream()
  127 | 			.filter(entry -> entry.getValue() > 1)
  128 | 			.map(Map.Entry::getKey)
  129 | 			.toList();
  130 | 	}
  131 | 
  132 | 	public static List<String> getDuplicateToolNames(ToolCallback... toolCallbacks) {
  133 | 		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
  134 | 		return getDuplicateToolNames(Arrays.asList(toolCallbacks));
  135 | 	}
  136 | 
  137 | 	/**
  138 | 	 * Validates that a tool name follows recommended naming conventions. Logs a warning
  139 | 	 * if the tool name contains characters that may not be compatible with some LLMs.
  140 | 	 * @param toolName the tool name to validate
  141 | 	 */
  142 | 	private static void validateToolName(String toolName) {
  143 | 		Assert.hasText(toolName, "Tool name cannot be null or empty");
  144 | 		if (logger.isWarnEnabled() && !RECOMMENDED_NAME_PATTERN.matcher(toolName).matches()) {
  145 | 			logger.warn("Tool name '" + toolName + "' may not be compatible with some LLMs (e.g., OpenAI). "
  146 | 					+ "Consider using only alphanumeric characters, underscores, hyphens, and dots.");
  147 | 		}
  148 | 	}
  149 | 
  150 | }
```
