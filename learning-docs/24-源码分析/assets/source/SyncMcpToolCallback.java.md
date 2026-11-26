<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/SyncMcpToolCallback.java -->
<!-- 正文引用：[[源码:SyncMcpToolCallback.java#L起-L止]] ；代码块头部：(src: SyncMcpToolCallback.java#L起-L止) -->

# SyncMcpToolCallback.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/SyncMcpToolCallback.java` |
| 行数 | 244 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:09.865Z |

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
   17 | package org.springframework.ai.mcp;
   18 | 
   19 | import java.util.Map;
   20 | 
   21 | import io.modelcontextprotocol.client.McpSyncClient;
   22 | import io.modelcontextprotocol.spec.McpError;
   23 | import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
   24 | import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
   25 | import io.modelcontextprotocol.spec.McpSchema.Tool;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.ai.chat.model.ToolContext;
   31 | import org.springframework.ai.tool.ToolCallback;
   32 | import org.springframework.ai.tool.definition.ToolDefinition;
   33 | import org.springframework.ai.tool.execution.ToolExecutionException;
   34 | import org.springframework.ai.util.JsonHelper;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.util.StringUtils;
   37 | 
   38 | /**
   39 |  * Synchronous adapter bridging MCP tools to Spring AI's {@link ToolCallback} interface.
   40 |  * Handles tool execution and data conversion between MCP and Spring AI.
   41 |  *
   42 |  * @author Christian Tzolov
   43 |  * @author YunKui Lu
   44 |  * @author Ilayaperumal Gopinathan
   45 |  * @since 1.0.0
   46 |  */
   47 | public class SyncMcpToolCallback implements ToolCallback {
   48 | 
   49 | 	private static final JsonHelper jsonHelper = new JsonHelper();
   50 | 
   51 | 	private static final Log logger = LogFactory.getLog(SyncMcpToolCallback.class);
   52 | 
   53 | 	private final McpSyncClient mcpClient;
   54 | 
   55 | 	private final Tool tool;
   56 | 
   57 | 	private final String prefixedToolName;
   58 | 
   59 | 	private final ToolContextToMcpMetaConverter toolContextToMcpMetaConverter;
   60 | 
   61 | 	/**
   62 | 	 * Creates a callback with default settings.
   63 | 	 * @param mcpClient the MCP client for tool execution
   64 | 	 * @param tool the MCP tool to adapt
   65 | 	 * @deprecated use {@link #builder()} instead
   66 | 	 */
   67 | 	@Deprecated
   68 | 	public SyncMcpToolCallback(McpSyncClient mcpClient, Tool tool) {
   69 | 		this(mcpClient, tool, McpToolUtils.prefixedToolName(mcpClient.getClientInfo().name(),
   70 | 				mcpClient.getClientInfo().title(), tool.name()), ToolContextToMcpMetaConverter.defaultConverter());
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Creates a callback with full configuration.
   75 | 	 * @param mcpClient the MCP client for tool execution
   76 | 	 * @param tool the MCP tool to adapt
   77 | 	 * @param prefixedToolName the prefixed name for the tool
   78 | 	 * @param toolContextToMcpMetaConverter converter for tool context metadata
   79 | 	 */
   80 | 	private SyncMcpToolCallback(McpSyncClient mcpClient, Tool tool, String prefixedToolName,
   81 | 			ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
   82 | 		Assert.notNull(mcpClient, "MCP client must not be null");
   83 | 		Assert.notNull(tool, "MCP tool must not be null");
   84 | 		Assert.hasText(prefixedToolName, "Prefixed tool name must not be empty");
   85 | 		Assert.notNull(toolContextToMcpMetaConverter, "ToolContextToMcpMetaConverter must not be null");
   86 | 
   87 | 		this.mcpClient = mcpClient;
   88 | 		this.tool = tool;
   89 | 		this.prefixedToolName = prefixedToolName;
   90 | 		this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public ToolDefinition getToolDefinition() {
   95 | 		return McpToolUtils.createToolDefinition(this.prefixedToolName, this.tool);
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Returns the original MCP tool name without prefixing.
  100 | 	 * @return the original tool name
  101 | 	 */
  102 | 	public String getOriginalToolName() {
  103 | 		return this.tool.name();
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public String call(String toolCallInput) {
  108 | 		return this.call(toolCallInput, null);
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	public String call(String toolCallInput, @Nullable ToolContext toolContext) {
  113 | 
  114 | 		// Handle the possible null parameter situation in streaming mode.
  115 | 		if (!StringUtils.hasText(toolCallInput)) {
  116 | 			if (logger.isWarnEnabled()) {
  117 | 				logger.warn("Tool call arguments are null or empty for MCP tool: " + this.tool.name()
  118 | 						+ ". Using empty JSON object as default.");
  119 | 			}
  120 | 			toolCallInput = "{}";
  121 | 		}
  122 | 
  123 | 		Map<String, Object> arguments = jsonHelper.fromJsonToMap(toolCallInput);
  124 | 
  125 | 		CallToolResult response;
  126 | 		try {
  127 | 			var mcpMeta = toolContext != null ? this.toolContextToMcpMetaConverter.convert(toolContext) : null;
  128 | 
  129 | 			// Use the original tool name, not the prefixed one from getToolDefinition
  130 | 			var request = CallToolRequest.builder(this.tool.name()).arguments(arguments).meta(mcpMeta).build();
  131 | 
  132 | 			// Note that we use the original tool name here, not the adapted one from
  133 | 			// getToolDefinition
  134 | 			response = this.mcpClient.callTool(request);
  135 | 		}
  136 | 		catch (McpError ex) {
  137 | 			// A protocol-level error indicates the tool invocation itself failed and
  138 | 			// must not be conveyed to the model. Rethrow it as a hard failure, mirroring
  139 | 			// how @Tool treats checked exceptions and Errors. Since the tool calling
  140 | 			// manager only handles ToolExecutionException, this bubbles up and fails the
  141 | 			// model interaction.
  142 | 			logger.error("Protocol error while calling tool: ", ex);
  143 | 			throw ex;
  144 | 		}
  145 | 		catch (Exception ex) {
  146 | 			logger.error("Exception while tool calling: ", ex);
  147 | 			throw new ToolExecutionException(this.getToolDefinition(), ex);
  148 | 		}
  149 | 
  150 | 		if (response.isError() != null && response.isError()) {
  151 | 			if (logger.isErrorEnabled()) {
  152 | 				logger.error("Error calling tool: " + response.content());
  153 | 			}
  154 | 			throw new ToolExecutionException(this.getToolDefinition(),
  155 | 					new IllegalStateException("Error calling tool: " + response.content()));
  156 | 		}
  157 | 		return jsonHelper.toJson(response.content());
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Creates a builder for constructing {@code SyncMcpToolCallback} instances.
  162 | 	 * @return a new builder
  163 | 	 */
  164 | 	public static Builder builder() {
  165 | 		return new Builder();
  166 | 	}
  167 | 
  168 | 	/**
  169 | 	 * Builder for {@code SyncMcpToolCallback} instances.
  170 | 	 */
  171 | 	public static final class Builder {
  172 | 
  173 | 		private @Nullable McpSyncClient mcpClient;
  174 | 
  175 | 		private @Nullable Tool tool;
  176 | 
  177 | 		private @Nullable String prefixedToolName;
  178 | 
  179 | 		private ToolContextToMcpMetaConverter toolContextToMcpMetaConverter = ToolContextToMcpMetaConverter
  180 | 			.defaultConverter();
  181 | 
  182 | 		/**
  183 | 		 * Sets the MCP client for tool execution.
  184 | 		 * @param mcpClient the MCP client (required)
  185 | 		 * @return this builder
  186 | 		 */
  187 | 		public Builder mcpClient(McpSyncClient mcpClient) {
  188 | 			this.mcpClient = mcpClient;
  189 | 			return this;
  190 | 		}
  191 | 
  192 | 		/**
  193 | 		 * Sets the MCP tool to adapt.
  194 | 		 * @param tool the MCP tool (required)
  195 | 		 * @return this builder
  196 | 		 */
  197 | 		public Builder tool(Tool tool) {
  198 | 			this.tool = tool;
  199 | 			return this;
  200 | 		}
  201 | 
  202 | 		/**
  203 | 		 * Sets the prefixed tool name. If not specified, a default prefix is generated.
  204 | 		 * @param prefixedToolName the prefixed tool name
  205 | 		 * @return this builder
  206 | 		 */
  207 | 		public Builder prefixedToolName(String prefixedToolName) {
  208 | 			this.prefixedToolName = prefixedToolName;
  209 | 			return this;
  210 | 		}
  211 | 
  212 | 		/**
  213 | 		 * Sets the converter for tool context to MCP metadata transformation. Defaults to
  214 | 		 * {@link ToolContextToMcpMetaConverter#defaultConverter()}.
  215 | 		 * @param toolContextToMcpMetaConverter the converter
  216 | 		 * @return this builder
  217 | 		 */
  218 | 		public Builder toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
  219 | 			Assert.notNull(toolContextToMcpMetaConverter, "ToolContextToMcpMetaConverter must not be null");
  220 | 			this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  221 | 			return this;
  222 | 		}
  223 | 
  224 | 		/**
  225 | 		 * Builds a {@code SyncMcpToolCallback} with the configured parameters.
  226 | 		 * @return a new {@code SyncMcpToolCallback}
  227 | 		 * @throws IllegalArgumentException if required parameters are missing
  228 | 		 */
  229 | 		public SyncMcpToolCallback build() {
  230 | 			Assert.notNull(this.mcpClient, "MCP client must not be null");
  231 | 			Assert.notNull(this.tool, "MCP tool must not be null");
  232 | 
  233 | 			// Apply defaults if not specified
  234 | 			if (this.prefixedToolName == null) {
  235 | 				this.prefixedToolName = McpToolUtils.format(this.tool.name());
  236 | 			}
  237 | 
  238 | 			return new SyncMcpToolCallback(this.mcpClient, this.tool, this.prefixedToolName,
  239 | 					this.toolContextToMcpMetaConverter);
  240 | 		}
  241 | 
  242 | 	}
  243 | 
  244 | }
```
