<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/SyncModernMcpToolCallback.java -->
<!-- 正文引用：[[源码:SyncModernMcpToolCallback.java#L起-L止]] ；代码块头部：(src: SyncModernMcpToolCallback.java#L起-L止) -->

# SyncModernMcpToolCallback.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/SyncModernMcpToolCallback.java` |
| 行数 | 264 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.357Z |

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
   19 | import java.util.Map;
   20 | 
   21 | import io.modelcontextprotocol.modern.McpException;
   22 | import io.modelcontextprotocol.modern.McpSchema.CallToolResult;
   23 | import io.modelcontextprotocol.modern.McpSchema.Tool;
   24 | import io.modelcontextprotocol.modern.client.McpSyncClient;
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.ai.chat.model.ToolContext;
   30 | import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
   31 | import org.springframework.ai.tool.ToolCallback;
   32 | import org.springframework.ai.tool.definition.ToolDefinition;
   33 | import org.springframework.ai.tool.execution.ToolExecutionException;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | /**
   37 |  * Adapts a tool of a modern MCP server to Spring AI's {@link ToolCallback}.
   38 |  * <p>
   39 |  * The callback is bound to the {@link McpSyncClient} it was created with and to the
   40 |  * {@link Tool} definition that client listed. It never selects a client itself, so it
   41 |  * calls the tool with the same client, and on behalf of the same caller, that listed it.
   42 |  * Passing the listed definition also lets the client add the {@code Mcp-Param-*} headers
   43 |  * without looking the tool up again.
   44 |  * <p>
   45 |  * A callback is only as long-lived as its client. Register the
   46 |  * {@link SyncModernMcpToolCallbackProvider} with the {@code ChatClient}, not the
   47 |  * callbacks it returns, so that each request gets callbacks for its own client.
   48 |  * <p>
   49 |  * A call fails in one of two ways, as a call of a legacy tool does. A protocol error that
   50 |  * the server answers with ({@code McpException}) is thrown as it is and fails the model
   51 |  * interaction. Every other failure is thrown as a {@link ToolExecutionException}, which
   52 |  * the application's tool error handling decides about: a tool result that is marked as an
   53 |  * error, and a failure in the client or on the way to the server, such as a timeout
   54 |  * ({@code McpTimeoutException}), a lost response or an HTTP error status. After a timeout
   55 |  * or a lost response it is unknown whether the server ran the tool.
   56 |  *
   57 |  * @author Christian Tzolov
   58 |  * @since 2.1.0
   59 |  * @see SyncModernMcpToolCallbackProvider
   60 |  */
   61 | public final class SyncModernMcpToolCallback implements ToolCallback {
   62 | 
   63 | 	private static final Log logger = LogFactory.getLog(SyncModernMcpToolCallback.class);
   64 | 
   65 | 	private final McpSyncClient.ToolSpec toolSpec;
   66 | 
   67 | 	private final Tool tool;
   68 | 
   69 | 	/** Built once: it is asked for several times for each request. */
   70 | 	private final ToolDefinition toolDefinition;
   71 | 
   72 | 	private final ToolContextToMcpMetaConverter toolContextToMcpMetaConverter;
   73 | 
   74 | 	private final @Nullable McpModernConnectionInfo connectionInfo;
   75 | 
   76 | 	private final @Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer;
   77 | 
   78 | 	private SyncModernMcpToolCallback(McpSyncClient mcpClient, Tool tool, String prefixedToolName,
   79 | 			ToolContextToMcpMetaConverter toolContextToMcpMetaConverter,
   80 | 			@Nullable McpModernConnectionInfo connectionInfo,
   81 | 			@Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer) {
   82 | 		Assert.notNull(mcpClient, "mcpClient must not be null");
   83 | 		Assert.notNull(tool, "tool must not be null");
   84 | 		Assert.hasText(prefixedToolName, "prefixedToolName must not be empty");
   85 | 		Assert.notNull(toolContextToMcpMetaConverter, "toolContextToMcpMetaConverter must not be null");
   86 | 		Assert.isTrue(toolCallCustomizer == null || connectionInfo != null,
   87 | 				"connectionInfo must not be null when a toolCallCustomizer is set");
   88 | 
   89 | 		// Request specs are immutable: the prepared spec is shared by every call, and
   90 | 		// each call derives its own spec with the arguments and the _meta entries.
   91 | 		this.toolSpec = mcpClient.tool(tool);
   92 | 		this.tool = tool;
   93 | 		this.toolDefinition = ModernToolCalls.toolDefinition(prefixedToolName, tool);
   94 | 		this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
   95 | 		this.connectionInfo = connectionInfo;
   96 | 		this.toolCallCustomizer = toolCallCustomizer;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Creates a builder for a {@link SyncModernMcpToolCallback}.
  101 | 	 * @return the builder
  102 | 	 */
  103 | 	public static Builder builder() {
  104 | 		return new Builder();
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public ToolDefinition getToolDefinition() {
  109 | 		return this.toolDefinition;
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Returns the name of the tool on the MCP server, without the prefix.
  114 | 	 * @return the original tool name
  115 | 	 */
  116 | 	public String getOriginalToolName() {
  117 | 		return this.tool.name();
  118 | 	}
  119 | 
  120 | 	@Override
  121 | 	public String call(String toolCallInput) {
  122 | 		return this.call(toolCallInput, null);
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	public String call(String toolCallInput, @Nullable ToolContext toolContext) {
  127 | 
  128 | 		Map<String, Object> arguments = ModernToolCalls.arguments(toolCallInput);
  129 | 
  130 | 		CallToolResult result;
  131 | 		try {
  132 | 			McpSyncClient.ToolSpec callSpec = this.toolSpec.arguments(arguments)
  133 | 				.meta(ModernToolCalls.requestMeta(this.toolContextToMcpMetaConverter, toolContext));
  134 | 			if (this.toolCallCustomizer != null && this.connectionInfo != null) {
  135 | 				callSpec = this.toolCallCustomizer.customize(this.connectionInfo, callSpec, toolContext);
  136 | 				Assert.state(callSpec != null, "toolCallCustomizer must not return null");
  137 | 			}
  138 | 			result = callSpec.call();
  139 | 		}
  140 | 		catch (McpException ex) {
  141 | 			// A protocol-level error of the server indicates the tool invocation itself
  142 | 			// failed and must not be conveyed to the model. Rethrow it as a hard failure,
  143 | 			// as the legacy callbacks do with their protocol errors. Since the tool
  144 | 			// calling manager only handles ToolExecutionException, this bubbles up and
  145 | 			// fails the model interaction.
  146 | 			logger.error("Protocol error while calling tool: " + this.tool.name(), ex);
  147 | 			throw ex;
  148 | 		}
  149 | 		catch (Exception ex) {
  150 | 			logger.error("Exception while calling tool: " + this.tool.name(), ex);
  151 | 			throw new ToolExecutionException(this.getToolDefinition(), ex);
  152 | 		}
  153 | 
  154 | 		if (Boolean.TRUE.equals(result.isError())) {
  155 | 			throw new ToolExecutionException(this.getToolDefinition(),
  156 | 					new IllegalStateException("Error calling tool: " + result.content()));
  157 | 		}
  158 | 
  159 | 		return ModernToolCalls.toModelResponse(result);
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * Builder for {@link SyncModernMcpToolCallback}.
  164 | 	 */
  165 | 	public static final class Builder {
  166 | 
  167 | 		private @Nullable McpSyncClient mcpClient;
  168 | 
  169 | 		private @Nullable Tool tool;
  170 | 
  171 | 		private @Nullable String prefixedToolName;
  172 | 
  173 | 		private ToolContextToMcpMetaConverter toolContextToMcpMetaConverter = ToolContextToMcpMetaConverter
  174 | 			.defaultConverter();
  175 | 
  176 | 		private @Nullable McpModernConnectionInfo connectionInfo;
  177 | 
  178 | 		private @Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer;
  179 | 
  180 | 		private Builder() {
  181 | 		}
  182 | 
  183 | 		/**
  184 | 		 * Sets the client that calls the tool.
  185 | 		 * @param mcpClient the client the tool was listed with
  186 | 		 * @return this builder
  187 | 		 */
  188 | 		public Builder mcpClient(McpSyncClient mcpClient) {
  189 | 			this.mcpClient = mcpClient;
  190 | 			return this;
  191 | 		}
  192 | 
  193 | 		/**
  194 | 		 * Sets the tool to adapt.
  195 | 		 * @param tool the tool, as listed by the server
  196 | 		 * @return this builder
  197 | 		 */
  198 | 		public Builder tool(Tool tool) {
  199 | 			this.tool = tool;
  200 | 			return this;
  201 | 		}
  202 | 
  203 | 		/**
  204 | 		 * Sets the name under which the model sees the tool. Defaults to the tool's own
  205 | 		 * name.
  206 | 		 * @param prefixedToolName the tool name presented to the model
  207 | 		 * @return this builder
  208 | 		 */
  209 | 		public Builder prefixedToolName(String prefixedToolName) {
  210 | 			this.prefixedToolName = prefixedToolName;
  211 | 			return this;
  212 | 		}
  213 | 
  214 | 		/**
  215 | 		 * Sets the converter that turns {@link ToolContext} entries into the request's
  216 | 		 * {@code _meta} entries.
  217 | 		 * @param toolContextToMcpMetaConverter the converter
  218 | 		 * @return this builder
  219 | 		 */
  220 | 		public Builder toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
  221 | 			Assert.notNull(toolContextToMcpMetaConverter, "toolContextToMcpMetaConverter must not be null");
  222 | 			this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  223 | 			return this;
  224 | 		}
  225 | 
  226 | 		/**
  227 | 		 * Sets the connection the tool belongs to. Required when a
  228 | 		 * {@link #toolCallCustomizer} is set, which receives it.
  229 | 		 * @param connectionInfo the connection of the tool
  230 | 		 * @return this builder
  231 | 		 */
  232 | 		public Builder connectionInfo(McpModernConnectionInfo connectionInfo) {
  233 | 			Assert.notNull(connectionInfo, "connectionInfo must not be null");
  234 | 			this.connectionInfo = connectionInfo;
  235 | 			return this;
  236 | 		}
  237 | 
  238 | 		/**
  239 | 		 * Sets the customizer that is applied to the request spec of every call, with the
  240 | 		 * {@link ToolContext} of that call.
  241 | 		 * @param toolCallCustomizer the customizer
  242 | 		 * @return this builder
  243 | 		 */
  244 | 		public Builder toolCallCustomizer(SyncModernMcpToolCallCustomizer toolCallCustomizer) {
  245 | 			Assert.notNull(toolCallCustomizer, "toolCallCustomizer must not be null");
  246 | 			this.toolCallCustomizer = toolCallCustomizer;
  247 | 			return this;
  248 | 		}
  249 | 
  250 | 		/**
  251 | 		 * Builds the callback.
  252 | 		 * @return the callback
  253 | 		 */
  254 | 		public SyncModernMcpToolCallback build() {
  255 | 			Assert.notNull(this.mcpClient, "mcpClient must not be null");
  256 | 			Assert.notNull(this.tool, "tool must not be null");
  257 | 			String name = this.prefixedToolName != null ? this.prefixedToolName : this.tool.name();
  258 | 			return new SyncModernMcpToolCallback(this.mcpClient, this.tool, name, this.toolContextToMcpMetaConverter,
  259 | 					this.connectionInfo, this.toolCallCustomizer);
  260 | 		}
  261 | 
  262 | 	}
  263 | 
  264 | }
```
