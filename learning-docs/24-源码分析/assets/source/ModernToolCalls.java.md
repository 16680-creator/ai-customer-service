<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/ModernToolCalls.java -->
<!-- 正文引用：[[源码:ModernToolCalls.java#L起-L止]] ；代码块头部：(src: ModernToolCalls.java#L起-L止) -->

# ModernToolCalls.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/ModernToolCalls.java` |
| 行数 | 117 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.714Z |

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
   17 | package org.springframework.ai.mcp.modern.client;
   18 | 
   19 | import java.util.LinkedHashMap;
   20 | import java.util.Map;
   21 | 
   22 | import io.modelcontextprotocol.modern.McpSchema.CallToolResult;
   23 | import io.modelcontextprotocol.modern.McpSchema.MetaKeys;
   24 | import io.modelcontextprotocol.modern.McpSchema.Tool;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.chat.model.ToolContext;
   28 | import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
   29 | import org.springframework.ai.tool.definition.DefaultToolDefinition;
   30 | import org.springframework.ai.tool.definition.ToolDefinition;
   31 | import org.springframework.ai.util.JsonHelper;
   32 | import org.springframework.ai.util.json.schema.JsonSchemaUtils;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * What the synchronous and the asynchronous tool callback of a modern connection have in
   37 |  * common: how a tool is presented to the model, what goes into a request, and how a
   38 |  * result is returned to the model.
   39 |  *
   40 |  * @author Christian Tzolov
   41 |  */
   42 | final class ModernToolCalls {
   43 | 
   44 | 	private static final JsonHelper jsonHelper = new JsonHelper();
   45 | 
   46 | 	/**
   47 | 	 * Prefix of the {@code _meta} keys the modern client sets itself and rejects when the
   48 | 	 * application sets them.
   49 | 	 */
   50 | 	private static final String RESERVED_META_PREFIX = "io.modelcontextprotocol/";
   51 | 
   52 | 	private ModernToolCalls() {
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * The definition of a tool as the model sees it.
   57 | 	 * @param prefixedToolName the name of the tool for the model
   58 | 	 * @param tool the tool, as listed by the server
   59 | 	 * @return the tool definition
   60 | 	 */
   61 | 	static ToolDefinition toolDefinition(String prefixedToolName, Tool tool) {
   62 | 		return DefaultToolDefinition.builder()
   63 | 			.name(prefixedToolName)
   64 | 			.description(tool.description())
   65 | 			.inputSchema(JsonSchemaUtils.ensureValidInputSchema(jsonHelper.toJson(tool.inputSchema())))
   66 | 			.build();
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * The arguments of a tool call. A streaming model may send no arguments at all.
   71 | 	 * @param toolCallInput the arguments as the model sent them
   72 | 	 * @return the arguments
   73 | 	 */
   74 | 	static Map<String, Object> arguments(String toolCallInput) {
   75 | 		return jsonHelper.fromJsonToMap(StringUtils.hasText(toolCallInput) ? toolCallInput : "{}");
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * The {@code _meta} entries of a request: the {@link ToolContext} entries without the
   80 | 	 * keys the modern client sets itself.
   81 | 	 * @param toolContextToMcpMetaConverter turns the tool context into {@code _meta}
   82 | 	 * entries
   83 | 	 * @param toolContext the tool context of the call, if any
   84 | 	 * @return the {@code _meta} entries
   85 | 	 */
   86 | 	static Map<String, Object> requestMeta(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter,
   87 | 			@Nullable ToolContext toolContext) {
   88 | 		if (toolContext == null) {
   89 | 			return Map.of();
   90 | 		}
   91 | 		Map<String, Object> meta = new LinkedHashMap<>();
   92 | 		toolContextToMcpMetaConverter.convert(toolContext).forEach((key, value) -> {
   93 | 			if (!MetaKeys.PROGRESS_TOKEN.equals(key) && !key.startsWith(RESERVED_META_PREFIX)) {
   94 | 				meta.put(key, value);
   95 | 			}
   96 | 		});
   97 | 		return meta;
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * The text returned to the model. A result without structured content is returned as
  102 | 	 * the JSON of its content blocks, like the legacy callbacks do. A result with
  103 | 	 * structured content is returned as a JSON object that holds both.
  104 | 	 * @param result the result of the tool call
  105 | 	 * @return the text for the model
  106 | 	 */
  107 | 	static String toModelResponse(CallToolResult result) {
  108 | 		if (result.structuredContent() == null) {
  109 | 			return jsonHelper.toJson(result.content());
  110 | 		}
  111 | 		Map<String, Object> response = new LinkedHashMap<>();
  112 | 		response.put("content", result.content());
  113 | 		response.put("structuredContent", result.structuredContent());
  114 | 		return jsonHelper.toJson(response);
  115 | 	}
  116 | 
  117 | }
```
