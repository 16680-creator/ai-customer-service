<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/McpSchemaConverter.java -->
<!-- 正文引用：[[源码:McpSchemaConverter.java#L起-L止]] ；代码块头部：(src: McpSchemaConverter.java#L起-L止) -->

# McpSchemaConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/McpSchemaConverter.java` |
| 行数 | 162 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.621Z |

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
   17 | package org.springframework.ai.mcp.modern;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import io.modelcontextprotocol.json.McpJsonDefaults;
   23 | import io.modelcontextprotocol.json.McpJsonMapper;
   24 | import io.modelcontextprotocol.modern.McpException;
   25 | import io.modelcontextprotocol.modern.McpSchema;
   26 | import io.modelcontextprotocol.spec.McpError;
   27 | 
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * Converts between the legacy ({@code io.modelcontextprotocol.spec.McpSchema}) and the
   32 |  * modern ({@code io.modelcontextprotocol.modern.McpSchema}) schema types.
   33 |  * <p>
   34 |  * Both type hierarchies serialize to the same JSON shapes, so the conversion is a
   35 |  * mapper-based round trip. Modern-only fields such as {@code resultType}, {@code ttlMs}
   36 |  * and {@code cacheScope} take their default values. This is what allows annotated methods
   37 |  * and {@code ToolCallback}s written against the legacy types to be served by a 2026-07-28
   38 |  * server without changes.
   39 |  *
   40 |  * @author Christian Tzolov
   41 |  * @since 2.1.0
   42 |  */
   43 | public final class McpSchemaConverter {
   44 | 
   45 | 	private McpSchemaConverter() {
   46 | 	}
   47 | 
   48 | 	private static McpJsonMapper mapper() {
   49 | 		return McpJsonDefaults.getMapper();
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Converts any JSON-compatible value to the given target type using the default MCP
   54 | 	 * JSON mapper.
   55 | 	 * @param source the value to convert
   56 | 	 * @param targetType the type to convert to
   57 | 	 * @param <T> the target type
   58 | 	 * @return the converted value
   59 | 	 */
   60 | 	public static <T> T convert(Object source, Class<T> targetType) {
   61 | 		Assert.notNull(source, "source must not be null");
   62 | 		Assert.notNull(targetType, "targetType must not be null");
   63 | 		return mapper().convertValue(source, targetType);
   64 | 	}
   65 | 
   66 | 	// ---------------------------------------------------------------------------------
   67 | 	// Legacy -> modern results and definitions
   68 | 	// ---------------------------------------------------------------------------------
   69 | 
   70 | 	public static McpSchema.CallToolResult toModern(io.modelcontextprotocol.spec.McpSchema.CallToolResult result) {
   71 | 		return convert(result, McpSchema.CallToolResult.class);
   72 | 	}
   73 | 
   74 | 	public static McpSchema.ReadResourceResult toModern(
   75 | 			io.modelcontextprotocol.spec.McpSchema.ReadResourceResult result) {
   76 | 		return convert(result, McpSchema.ReadResourceResult.class);
   77 | 	}
   78 | 
   79 | 	public static McpSchema.GetPromptResult toModern(io.modelcontextprotocol.spec.McpSchema.GetPromptResult result) {
   80 | 		return convert(result, McpSchema.GetPromptResult.class);
   81 | 	}
   82 | 
   83 | 	public static McpSchema.CompleteResult toModern(io.modelcontextprotocol.spec.McpSchema.CompleteResult result) {
   84 | 		return convert(result, McpSchema.CompleteResult.class);
   85 | 	}
   86 | 
   87 | 	public static McpSchema.Tool toModern(io.modelcontextprotocol.spec.McpSchema.Tool tool) {
   88 | 		return convert(tool, McpSchema.Tool.class);
   89 | 	}
   90 | 
   91 | 	public static McpSchema.Resource toModern(io.modelcontextprotocol.spec.McpSchema.Resource resource) {
   92 | 		return convert(resource, McpSchema.Resource.class);
   93 | 	}
   94 | 
   95 | 	public static McpSchema.ResourceTemplate toModern(
   96 | 			io.modelcontextprotocol.spec.McpSchema.ResourceTemplate resourceTemplate) {
   97 | 		return convert(resourceTemplate, McpSchema.ResourceTemplate.class);
   98 | 	}
   99 | 
  100 | 	public static McpSchema.Prompt toModern(io.modelcontextprotocol.spec.McpSchema.Prompt prompt) {
  101 | 		return convert(prompt, McpSchema.Prompt.class);
  102 | 	}
  103 | 
  104 | 	public static McpSchema.CompleteReference toModern(
  105 | 			io.modelcontextprotocol.spec.McpSchema.CompleteReference reference) {
  106 | 		if (reference instanceof io.modelcontextprotocol.spec.McpSchema.PromptReference promptReference) {
  107 | 			return McpSchema.PromptReference.builder(promptReference.name()).title(promptReference.title()).build();
  108 | 		}
  109 | 		if (reference instanceof io.modelcontextprotocol.spec.McpSchema.ResourceReference resourceReference) {
  110 | 			return new McpSchema.ResourceReference(resourceReference.uri());
  111 | 		}
  112 | 		throw new IllegalArgumentException("Unsupported completion reference: " + reference);
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Converts a legacy protocol error into its modern counterpart so that it is answered
  117 | 	 * with the same JSON-RPC error code and data.
  118 | 	 * @param error the legacy error
  119 | 	 * @return the modern exception
  120 | 	 */
  121 | 	public static McpException toModern(McpError error) {
  122 | 		var jsonRpcError = error.getJsonRpcError();
  123 | 		if (jsonRpcError == null) {
  124 | 			return new McpException(McpSchema.ErrorCodes.INTERNAL_ERROR,
  125 | 					error.getMessage() != null ? error.getMessage() : "Internal error");
  126 | 		}
  127 | 		return new McpException(jsonRpcError.code(), jsonRpcError.message(), jsonRpcError.data());
  128 | 	}
  129 | 
  130 | 	// ---------------------------------------------------------------------------------
  131 | 	// Modern -> legacy requests
  132 | 	// ---------------------------------------------------------------------------------
  133 | 
  134 | 	/**
  135 | 	 * Builds the legacy request a handler expects from a modern {@code tools/call}
  136 | 	 * request. The MRTR fields ({@code inputResponses}, {@code requestState}) have no
  137 | 	 * legacy counterpart and are dropped; handlers that need them take the modern
  138 | 	 * request.
  139 | 	 */
  140 | 	public static io.modelcontextprotocol.spec.McpSchema.CallToolRequest toLegacy(McpSchema.CallToolRequest request) {
  141 | 		return new io.modelcontextprotocol.spec.McpSchema.CallToolRequest(request.name(),
  142 | 				request.arguments() != null ? request.arguments() : Map.of(), request.meta());
  143 | 	}
  144 | 
  145 | 	public static io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest toLegacy(
  146 | 			McpSchema.ReadResourceRequest request) {
  147 | 		return new io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest(request.uri(), request.meta());
  148 | 	}
  149 | 
  150 | 	public static io.modelcontextprotocol.spec.McpSchema.GetPromptRequest toLegacy(McpSchema.GetPromptRequest request) {
  151 | 		Map<String, Object> arguments = new HashMap<>();
  152 | 		if (request.arguments() != null) {
  153 | 			arguments.putAll(request.arguments());
  154 | 		}
  155 | 		return new io.modelcontextprotocol.spec.McpSchema.GetPromptRequest(request.name(), arguments, request.meta());
  156 | 	}
  157 | 
  158 | 	public static io.modelcontextprotocol.spec.McpSchema.CompleteRequest toLegacy(McpSchema.CompleteRequest request) {
  159 | 		return convert(request, io.modelcontextprotocol.spec.McpSchema.CompleteRequest.class);
  160 | 	}
  161 | 
  162 | }
```
