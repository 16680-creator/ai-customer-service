<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/SyncMcpToolCallbackProvider.java -->
<!-- 正文引用：[[源码:SyncMcpToolCallbackProvider.java#L起-L止]] ；代码块头部：(src: SyncMcpToolCallbackProvider.java#L起-L止) -->

# SyncMcpToolCallbackProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/SyncMcpToolCallbackProvider.java` |
| 行数 | 357 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:09.475Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.concurrent.locks.Lock;
   22 | import java.util.concurrent.locks.ReentrantLock;
   23 | 
   24 | import io.modelcontextprotocol.client.McpSyncClient;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.ai.tool.ToolCallback;
   28 | import org.springframework.ai.tool.ToolCallbackProvider;
   29 | import org.springframework.ai.tool.support.ToolUtils;
   30 | import org.springframework.context.ApplicationListener;
   31 | import org.springframework.util.Assert;
   32 | import org.springframework.util.CollectionUtils;
   33 | 
   34 | /**
   35 |  * Provides Spring AI tool callbacks by discovering tools from MCP servers.
   36 |  * <p>
   37 |  * Automatically discovers and exposes tools from multiple MCP servers as Spring AI
   38 |  * {@link ToolCallback} instances.
   39 |  *
   40 |  * @author Christian Tzolov
   41 |  * @author YunKui Lu
   42 |  * @since 1.0.0
   43 |  */
   44 | public class SyncMcpToolCallbackProvider implements ToolCallbackProvider, ApplicationListener<McpToolsChangedEvent> {
   45 | 
   46 | 	private final List<McpSyncClient> mcpClients;
   47 | 
   48 | 	private final McpToolFilter toolFilter;
   49 | 
   50 | 	private final McpToolNamePrefixGenerator toolNamePrefixGenerator;
   51 | 
   52 | 	private final ToolContextToMcpMetaConverter toolContextToMcpMetaConverter;
   53 | 
   54 | 	private final @Nullable ToolCallbackProvider modernToolCallbackProvider;
   55 | 
   56 | 	private volatile boolean invalidateCache = true;
   57 | 
   58 | 	private volatile List<ToolCallback> cachedToolCallbacks = List.of();
   59 | 
   60 | 	private final Lock lock = new ReentrantLock();
   61 | 
   62 | 	/**
   63 | 	 * Creates a provider with MCP clients and tool filter.
   64 | 	 * @param mcpClients MCP clients for tool discovery
   65 | 	 * @param toolFilter filter for discovered tools
   66 | 	 * @deprecated use {@link #builder()} instead
   67 | 	 */
   68 | 	@Deprecated
   69 | 	public SyncMcpToolCallbackProvider(McpToolFilter toolFilter, List<McpSyncClient> mcpClients) {
   70 | 		this(toolFilter, McpToolNamePrefixGenerator.noPrefix(), mcpClients,
   71 | 				ToolContextToMcpMetaConverter.defaultConverter());
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * Creates a provider with all configuration options.
   76 | 	 * @param mcpClients MCP clients for tool discovery
   77 | 	 * @param toolNamePrefixGenerator generates prefixes for tool names
   78 | 	 * @param toolFilter filter for discovered tools
   79 | 	 * @param toolContextToMcpMetaConverter converts tool context to MCP metadata
   80 | 	 */
   81 | 	private SyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpToolNamePrefixGenerator toolNamePrefixGenerator,
   82 | 			List<McpSyncClient> mcpClients, ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
   83 | 		this(toolFilter, toolNamePrefixGenerator, mcpClients, toolContextToMcpMetaConverter, null);
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Creates a provider with all configuration options.
   88 | 	 * @param mcpClients MCP clients for tool discovery
   89 | 	 * @param toolNamePrefixGenerator generates prefixes for tool names
   90 | 	 * @param toolFilter filter for discovered tools
   91 | 	 * @param toolContextToMcpMetaConverter converts tool context to MCP metadata
   92 | 	 * @param modernToolCallbackProvider provider of the tools of modern MCP connections,
   93 | 	 * if any
   94 | 	 */
   95 | 	private SyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpToolNamePrefixGenerator toolNamePrefixGenerator,
   96 | 			List<McpSyncClient> mcpClients, ToolContextToMcpMetaConverter toolContextToMcpMetaConverter,
   97 | 			@Nullable ToolCallbackProvider modernToolCallbackProvider) {
   98 | 		Assert.notNull(mcpClients, "MCP clients must not be null");
   99 | 		Assert.notNull(toolFilter, "Tool filter must not be null");
  100 | 		Assert.notNull(toolNamePrefixGenerator, "Tool name prefix generator must not be null");
  101 | 		Assert.notNull(toolContextToMcpMetaConverter, "Tool context to MCP meta converter must not be null");
  102 | 		this.mcpClients = mcpClients;
  103 | 		this.toolFilter = toolFilter;
  104 | 		this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  105 | 		this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  106 | 		this.modernToolCallbackProvider = modernToolCallbackProvider;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Creates a provider with MCP clients using default filter.
  111 | 	 * @param mcpClients MCP clients for tool discovery
  112 | 	 * @deprecated use {@link #builder()} instead
  113 | 	 */
  114 | 	@Deprecated
  115 | 	public SyncMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
  116 | 		this((mcpClient, tool) -> true, mcpClients);
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Creates a provider with MCP clients, filter, and prefix generator.
  121 | 	 * @param mcpClients MCP clients for tool discovery
  122 | 	 * @param toolNamePrefixGenerator generates prefixes for tool names
  123 | 	 * @param toolFilter filter for discovered tools
  124 | 	 * @deprecated use {@link #builder()} instead
  125 | 	 */
  126 | 	@Deprecated
  127 | 	public SyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpToolNamePrefixGenerator toolNamePrefixGenerator,
  128 | 			McpSyncClient... mcpClients) {
  129 | 		this(toolFilter, toolNamePrefixGenerator, List.of(mcpClients),
  130 | 				ToolContextToMcpMetaConverter.defaultConverter());
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Creates a provider with MCP clients using default filter.
  135 | 	 * @param mcpClients MCP clients for tool discovery
  136 | 	 * @deprecated use {@link #builder()} instead
  137 | 	 */
  138 | 	@Deprecated
  139 | 	public SyncMcpToolCallbackProvider(McpSyncClient... mcpClients) {
  140 | 		this(List.of(mcpClients));
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public ToolCallback[] getToolCallbacks() {
  145 | 
  146 | 		if (this.invalidateCache) {
  147 | 			this.lock.lock();
  148 | 			try {
  149 | 				if (this.invalidateCache) {
  150 | 					this.cachedToolCallbacks = this.mcpClients.stream()
  151 | 						.flatMap(mcpClient -> mcpClient.listTools()
  152 | 							.tools()
  153 | 							.stream()
  154 | 							.filter(tool -> this.toolFilter.test(connectionInfo(mcpClient), tool))
  155 | 							.<ToolCallback>map(tool -> SyncMcpToolCallback.builder()
  156 | 								.mcpClient(mcpClient)
  157 | 								.tool(tool)
  158 | 								.prefixedToolName(
  159 | 										this.toolNamePrefixGenerator.prefixedToolName(connectionInfo(mcpClient), tool))
  160 | 								.toolContextToMcpMetaConverter(this.toolContextToMcpMetaConverter)
  161 | 								.build()))
  162 | 						.toList();
  163 | 
  164 | 					this.validateToolCallbacks(this.cachedToolCallbacks);
  165 | 					this.invalidateCache = false;
  166 | 				}
  167 | 			}
  168 | 			finally {
  169 | 				this.lock.unlock();
  170 | 			}
  171 | 		}
  172 | 
  173 | 		List<ToolCallback> toolCallbacks = this.cachedToolCallbacks;
  174 | 
  175 | 		if (this.modernToolCallbackProvider != null) {
  176 | 			// Modern callbacks are bound to the client that serves the current request,
  177 | 			// so they are resolved on every call and are not part of the cache.
  178 | 			toolCallbacks = new ArrayList<>(toolCallbacks);
  179 | 			toolCallbacks.addAll(List.of(this.modernToolCallbackProvider.getToolCallbacks()));
  180 | 			this.validateToolCallbacks(toolCallbacks);
  181 | 		}
  182 | 
  183 | 		return toolCallbacks.toArray(new ToolCallback[0]);
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Invalidates the cached tool callbacks, forcing re-discovery on next request.
  188 | 	 */
  189 | 	public void invalidateCache() {
  190 | 		this.invalidateCache = true;
  191 | 	}
  192 | 
  193 | 	@Override
  194 | 	public void onApplicationEvent(McpToolsChangedEvent event) {
  195 | 		this.invalidateCache();
  196 | 	}
  197 | 
  198 | 	private static McpConnectionInfo connectionInfo(McpSyncClient mcpClient) {
  199 | 		return McpConnectionInfo.builder()
  200 | 			.clientCapabilities(mcpClient.getClientCapabilities())
  201 | 			.clientInfo(mcpClient.getClientInfo())
  202 | 			.initializeResult(mcpClient.getCurrentInitializationResult())
  203 | 			.build();
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Validates tool callbacks for duplicate names.
  208 | 	 * @param toolCallbacks callbacks to validate
  209 | 	 * @throws IllegalStateException if duplicate names exist
  210 | 	 */
  211 | 	private void validateToolCallbacks(List<ToolCallback> toolCallbacks) {
  212 | 		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
  213 | 		if (!duplicateToolNames.isEmpty()) {
  214 | 			throw new IllegalStateException(
  215 | 					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
  216 | 		}
  217 | 	}
  218 | 
  219 | 	/**
  220 | 	 * Creates tool callbacks from multiple MCP clients.
  221 | 	 * <p>
  222 | 	 * Discovers and consolidates tools from all provided clients into a single list,
  223 | 	 * ensuring no naming conflicts.
  224 | 	 * @param mcpClients MCP clients to discover tools from
  225 | 	 * @return consolidated list of tool callbacks
  226 | 	 */
  227 | 	public static List<ToolCallback> syncToolCallbacks(List<McpSyncClient> mcpClients) {
  228 | 
  229 | 		if (CollectionUtils.isEmpty(mcpClients)) {
  230 | 			return List.of();
  231 | 		}
  232 | 		return List.of((new SyncMcpToolCallbackProvider(mcpClients).getToolCallbacks()));
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * Creates a builder for constructing provider instances.
  237 | 	 * @return new builder
  238 | 	 */
  239 | 	public static Builder builder() {
  240 | 		return new Builder();
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Builder for {@code SyncMcpToolCallbackProvider}.
  245 | 	 */
  246 | 	public static final class Builder {
  247 | 
  248 | 		private List<McpSyncClient> mcpClients = new ArrayList<>();
  249 | 
  250 | 		private McpToolFilter toolFilter = (mcpClient, tool) -> true;
  251 | 
  252 | 		private McpToolNamePrefixGenerator toolNamePrefixGenerator = new DefaultMcpToolNamePrefixGenerator();
  253 | 
  254 | 		private ToolContextToMcpMetaConverter toolContextToMcpMetaConverter = ToolContextToMcpMetaConverter
  255 | 			.defaultConverter();
  256 | 
  257 | 		private @Nullable ToolCallbackProvider modernToolCallbackProvider;
  258 | 
  259 | 		/**
  260 | 		 * Sets MCP clients for tool discovery (replaces existing).
  261 | 		 * @param mcpClients list of MCP clients
  262 | 		 * @return this builder
  263 | 		 */
  264 | 		public Builder mcpClients(List<McpSyncClient> mcpClients) {
  265 | 			Assert.notNull(mcpClients, "MCP clients list must not be null");
  266 | 			this.mcpClients = new ArrayList<>(mcpClients);
  267 | 			return this;
  268 | 		}
  269 | 
  270 | 		/**
  271 | 		 * Sets MCP clients for tool discovery (replaces existing).
  272 | 		 * @param mcpClients MCP clients array
  273 | 		 * @return this builder
  274 | 		 */
  275 | 		public Builder mcpClients(McpSyncClient... mcpClients) {
  276 | 			Assert.notNull(mcpClients, "MCP clients array must not be null");
  277 | 			this.mcpClients = new java.util.ArrayList<>(List.of(mcpClients));
  278 | 			return this;
  279 | 		}
  280 | 
  281 | 		/**
  282 | 		 * Adds an MCP client to the existing list.
  283 | 		 * @param mcpClient MCP client to add
  284 | 		 * @return this builder
  285 | 		 */
  286 | 		public Builder addMcpClient(McpSyncClient mcpClient) {
  287 | 			Assert.notNull(mcpClient, "MCP client must not be null");
  288 | 			this.mcpClients.add(mcpClient);
  289 | 			return this;
  290 | 		}
  291 | 
  292 | 		/**
  293 | 		 * Sets tool filter. Defaults to accepting all tools.
  294 | 		 * @param toolFilter filter for discovered tools
  295 | 		 * @return this builder
  296 | 		 */
  297 | 		public Builder toolFilter(McpToolFilter toolFilter) {
  298 | 			Assert.notNull(toolFilter, "Tool filter must not be null");
  299 | 			this.toolFilter = toolFilter;
  300 | 			return this;
  301 | 		}
  302 | 
  303 | 		/**
  304 | 		 * Sets tool name prefix generator.
  305 | 		 * @param toolNamePrefixGenerator generates prefixes for tool names
  306 | 		 * @return this builder
  307 | 		 */
  308 | 		public Builder toolNamePrefixGenerator(McpToolNamePrefixGenerator toolNamePrefixGenerator) {
  309 | 			Assert.notNull(toolNamePrefixGenerator, "Tool name prefix generator must not be null");
  310 | 			this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  311 | 			return this;
  312 | 		}
  313 | 
  314 | 		/**
  315 | 		 * Sets tool context to MCP metadata converter. Defaults to
  316 | 		 * {@link ToolContextToMcpMetaConverter#defaultConverter()}.
  317 | 		 * @param toolContextToMcpMetaConverter converts tool context to MCP metadata
  318 | 		 * @return this builder
  319 | 		 */
  320 | 		public Builder toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
  321 | 			Assert.notNull(toolContextToMcpMetaConverter, "Tool context to MCP meta converter must not be null");
  322 | 			this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  323 | 			return this;
  324 | 		}
  325 | 
  326 | 		/**
  327 | 		 * Sets the provider of the tools of the MCP connections that speak the 2026-07-28
  328 | 		 * ("modern") protocol revision, typically a
  329 | 		 * {@code SyncModernMcpToolCallbackProvider}. Its callbacks are returned together
  330 | 		 * with those of the MCP clients, so that an application that talks to legacy and
  331 | 		 * modern servers registers a single provider. They are resolved on every call to
  332 | 		 * {@link SyncMcpToolCallbackProvider#getToolCallbacks()} and are neither cached
  333 | 		 * nor affected by {@link SyncMcpToolCallbackProvider#invalidateCache()}.
  334 | 		 * @param modernToolCallbackProvider provider of the modern connections' tools
  335 | 		 * @return this builder
  336 | 		 * @since 2.1.0
  337 | 		 */
  338 | 		public Builder modernToolCallbackProvider(ToolCallbackProvider modernToolCallbackProvider) {
  339 | 			Assert.notNull(modernToolCallbackProvider, "Modern tool callback provider must not be null");
  340 | 			this.modernToolCallbackProvider = modernToolCallbackProvider;
  341 | 			return this;
  342 | 		}
  343 | 
  344 | 		/**
  345 | 		 * Builds the provider with configured parameters.
  346 | 		 * @return configured {@code SyncMcpToolCallbackProvider}
  347 | 		 */
  348 | 		public SyncMcpToolCallbackProvider build() {
  349 | 			// Assert.notEmpty(this.mcpClients, "At least one MCP client must be
  350 | 			// provided");
  351 | 			return new SyncMcpToolCallbackProvider(this.toolFilter, this.toolNamePrefixGenerator, this.mcpClients,
  352 | 					this.toolContextToMcpMetaConverter, this.modernToolCallbackProvider);
  353 | 		}
  354 | 
  355 | 	}
  356 | 
  357 | }
```
