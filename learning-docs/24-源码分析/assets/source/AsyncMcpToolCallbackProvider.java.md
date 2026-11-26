<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/AsyncMcpToolCallbackProvider.java -->
<!-- 正文引用：[[源码:AsyncMcpToolCallbackProvider.java#L起-L止]] ；代码块头部：(src: AsyncMcpToolCallbackProvider.java#L起-L止) -->

# AsyncMcpToolCallbackProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/AsyncMcpToolCallbackProvider.java` |
| 行数 | 360 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:10.322Z |

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
   24 | import io.modelcontextprotocol.client.McpAsyncClient;
   25 | import io.modelcontextprotocol.util.Assert;
   26 | import org.jspecify.annotations.Nullable;
   27 | import reactor.core.publisher.Flux;
   28 | 
   29 | import org.springframework.ai.tool.ToolCallback;
   30 | import org.springframework.ai.tool.ToolCallbackProvider;
   31 | import org.springframework.ai.tool.support.ToolUtils;
   32 | import org.springframework.context.ApplicationListener;
   33 | import org.springframework.util.CollectionUtils;
   34 | 
   35 | /**
   36 |  * Provides MCP tools asynchronously from multiple MCP servers as Spring AI tool
   37 |  * callbacks.
   38 |  * <p>
   39 |  * Discovers and exposes tools from configured MCP servers, enabling their use within
   40 |  * Spring AI applications. Supports filtering and custom naming strategies for tools.
   41 |  *
   42 |  * @author Christian Tzolov
   43 |  * @author YunKui Lu
   44 |  * @since 1.0.0
   45 |  */
   46 | public class AsyncMcpToolCallbackProvider implements ToolCallbackProvider, ApplicationListener<McpToolsChangedEvent> {
   47 | 
   48 | 	private final McpToolFilter toolFilter;
   49 | 
   50 | 	private final List<McpAsyncClient> mcpClients;
   51 | 
   52 | 	private final McpToolNamePrefixGenerator toolNamePrefixGenerator;
   53 | 
   54 | 	private final ToolContextToMcpMetaConverter toolContextToMcpMetaConverter;
   55 | 
   56 | 	private final @Nullable ToolCallbackProvider modernToolCallbackProvider;
   57 | 
   58 | 	private volatile boolean invalidateCache = true;
   59 | 
   60 | 	private volatile List<ToolCallback> cachedToolCallbacks = List.of();
   61 | 
   62 | 	private final Lock lock = new ReentrantLock();
   63 | 
   64 | 	/**
   65 | 	 * Creates a provider with tool filtering.
   66 | 	 * @param toolFilter filter to apply to discovered tools
   67 | 	 * @param mcpClients MCP clients for tool discovery
   68 | 	 * @deprecated use {@link #builder()} instead
   69 | 	 */
   70 | 	@Deprecated
   71 | 	public AsyncMcpToolCallbackProvider(McpToolFilter toolFilter, List<McpAsyncClient> mcpClients) {
   72 | 		this(toolFilter, McpToolNamePrefixGenerator.noPrefix(), ToolContextToMcpMetaConverter.defaultConverter(),
   73 | 				mcpClients);
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Creates a provider with full configuration.
   78 | 	 * @param toolFilter filter for discovered tools
   79 | 	 * @param toolNamePrefixGenerator generates prefixes for tool names
   80 | 	 * @param toolContextToMcpMetaConverter converts tool context to MCP metadata
   81 | 	 * @param mcpClients MCP clients for tool discovery
   82 | 	 */
   83 | 	private AsyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpToolNamePrefixGenerator toolNamePrefixGenerator,
   84 | 			ToolContextToMcpMetaConverter toolContextToMcpMetaConverter, List<McpAsyncClient> mcpClients) {
   85 | 		this(toolFilter, toolNamePrefixGenerator, toolContextToMcpMetaConverter, mcpClients, null);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Creates a provider with full configuration.
   90 | 	 * @param toolFilter filter for discovered tools
   91 | 	 * @param toolNamePrefixGenerator generates prefixes for tool names
   92 | 	 * @param toolContextToMcpMetaConverter converts tool context to MCP metadata
   93 | 	 * @param mcpClients MCP clients for tool discovery
   94 | 	 * @param modernToolCallbackProvider provider of the tools of modern MCP connections,
   95 | 	 * if any
   96 | 	 */
   97 | 	private AsyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpToolNamePrefixGenerator toolNamePrefixGenerator,
   98 | 			ToolContextToMcpMetaConverter toolContextToMcpMetaConverter, List<McpAsyncClient> mcpClients,
   99 | 			@Nullable ToolCallbackProvider modernToolCallbackProvider) {
  100 | 		Assert.notNull(mcpClients, "MCP clients must not be null");
  101 | 		Assert.notNull(toolFilter, "Tool filter must not be null");
  102 | 		Assert.notNull(toolNamePrefixGenerator, "Tool name prefix generator must not be null");
  103 | 		Assert.notNull(toolContextToMcpMetaConverter, "Tool context to MCP meta converter must not be null");
  104 | 		this.toolFilter = toolFilter;
  105 | 		this.mcpClients = mcpClients;
  106 | 		this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  107 | 		this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  108 | 		this.modernToolCallbackProvider = modernToolCallbackProvider;
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Creates a provider with default configuration.
  113 | 	 * @param mcpClients MCP clients for tool discovery
  114 | 	 * @throws IllegalArgumentException if mcpClients is null
  115 | 	 * @deprecated use {@link #builder()} instead
  116 | 	 */
  117 | 	@Deprecated
  118 | 	public AsyncMcpToolCallbackProvider(List<McpAsyncClient> mcpClients) {
  119 | 		this((mcpClient, tool) -> true, mcpClients);
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Creates a provider with tool filtering.
  124 | 	 * @param toolFilter filter for discovered tools
  125 | 	 * @param mcpClients MCP clients for tool discovery
  126 | 	 * @deprecated use {@link #builder()} instead
  127 | 	 */
  128 | 	@Deprecated
  129 | 	public AsyncMcpToolCallbackProvider(McpToolFilter toolFilter, McpAsyncClient... mcpClients) {
  130 | 		this(toolFilter, List.of(mcpClients));
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Creates a provider with default configuration.
  135 | 	 * @param mcpClients MCP clients for tool discovery
  136 | 	 * @deprecated use {@link #builder()} instead
  137 | 	 */
  138 | 	@Deprecated
  139 | 	public AsyncMcpToolCallbackProvider(McpAsyncClient... mcpClients) {
  140 | 		this(List.of(mcpClients));
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Discovers and returns all available tools from configured MCP servers.
  145 | 	 * <p>
  146 | 	 * Retrieves tools asynchronously from each server, creates callbacks, and validates
  147 | 	 * uniqueness. Blocks until all tools are discovered.
  148 | 	 * @return array of tool callbacks for discovered tools
  149 | 	 * @throws IllegalStateException if duplicate tool names exist
  150 | 	 */
  151 | 	@Override
  152 | 	public ToolCallback[] getToolCallbacks() {
  153 | 
  154 | 		if (this.invalidateCache) {
  155 | 			this.lock.lock();
  156 | 			try {
  157 | 				if (this.invalidateCache) {
  158 | 					List<ToolCallback> toolCallbackList = new ArrayList<>();
  159 | 
  160 | 					for (McpAsyncClient mcpClient : this.mcpClients) {
  161 | 
  162 | 						ToolCallback[] toolCallbacks = mcpClient.listTools()
  163 | 							.map(response -> response.tools()
  164 | 								.stream()
  165 | 								.filter(tool -> this.toolFilter.test(connectionInfo(mcpClient), tool))
  166 | 								.<ToolCallback>map(tool -> AsyncMcpToolCallback.builder()
  167 | 									.mcpClient(mcpClient)
  168 | 									.tool(tool)
  169 | 									.prefixedToolName(this.toolNamePrefixGenerator
  170 | 										.prefixedToolName(connectionInfo(mcpClient), tool))
  171 | 									.toolContextToMcpMetaConverter(this.toolContextToMcpMetaConverter)
  172 | 									.build())
  173 | 								.toArray(ToolCallback[]::new))
  174 | 							.block();
  175 | 
  176 | 						toolCallbackList.addAll(List.of(toolCallbacks));
  177 | 					}
  178 | 
  179 | 					this.cachedToolCallbacks = toolCallbackList;
  180 | 
  181 | 					this.validateToolCallbacks(this.cachedToolCallbacks);
  182 | 
  183 | 					this.invalidateCache = false;
  184 | 				}
  185 | 			}
  186 | 			finally {
  187 | 				this.lock.unlock();
  188 | 			}
  189 | 		}
  190 | 
  191 | 		List<ToolCallback> toolCallbacks = this.cachedToolCallbacks;
  192 | 
  193 | 		if (this.modernToolCallbackProvider != null) {
  194 | 			// Modern callbacks are bound to the client that serves the current request,
  195 | 			// so they are resolved on every call and are not part of the cache.
  196 | 			toolCallbacks = new ArrayList<>(toolCallbacks);
  197 | 			toolCallbacks.addAll(List.of(this.modernToolCallbackProvider.getToolCallbacks()));
  198 | 			this.validateToolCallbacks(toolCallbacks);
  199 | 		}
  200 | 
  201 | 		return toolCallbacks.toArray(new ToolCallback[0]);
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Invalidates the cached tool callbacks, forcing re-discovery on next request.
  206 | 	 */
  207 | 	public void invalidateCache() {
  208 | 		this.invalidateCache = true;
  209 | 	}
  210 | 
  211 | 	@Override
  212 | 	public void onApplicationEvent(McpToolsChangedEvent event) {
  213 | 		this.invalidateCache();
  214 | 	}
  215 | 
  216 | 	private static McpConnectionInfo connectionInfo(McpAsyncClient mcpClient) {
  217 | 		return McpConnectionInfo.builder()
  218 | 			.clientCapabilities(mcpClient.getClientCapabilities())
  219 | 			.clientInfo(mcpClient.getClientInfo())
  220 | 			.initializeResult(mcpClient.getCurrentInitializationResult())
  221 | 			.build();
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Validates tool name uniqueness.
  226 | 	 * @param toolCallbacks callbacks to validate
  227 | 	 * @throws IllegalStateException if duplicate names found
  228 | 	 */
  229 | 	private void validateToolCallbacks(List<ToolCallback> toolCallbacks) {
  230 | 		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
  231 | 		if (!duplicateToolNames.isEmpty()) {
  232 | 			throw new IllegalStateException(
  233 | 					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
  234 | 		}
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Creates a reactive stream of tool callbacks from multiple MCP clients.
  239 | 	 * <p>
  240 | 	 * Provides fully reactive tool discovery suitable for non-blocking applications.
  241 | 	 * Combines tools from all clients into a single stream with name conflict validation.
  242 | 	 * @param mcpClients MCP clients for tool discovery
  243 | 	 * @return Flux of tool callbacks from all clients
  244 | 	 */
  245 | 	public static Flux<ToolCallback> asyncToolCallbacks(List<McpAsyncClient> mcpClients) {
  246 | 		if (CollectionUtils.isEmpty(mcpClients)) {
  247 | 			return Flux.empty();
  248 | 		}
  249 | 
  250 | 		return Flux.fromArray(new AsyncMcpToolCallbackProvider(mcpClients).getToolCallbacks());
  251 | 	}
  252 | 
  253 | 	/**
  254 | 	 * Creates a builder for constructing provider instances.
  255 | 	 * @return new builder
  256 | 	 */
  257 | 	public static Builder builder() {
  258 | 		return new Builder();
  259 | 	}
  260 | 
  261 | 	/**
  262 | 	 * Builder for {@code AsyncMcpToolCallbackProvider} configuration.
  263 | 	 */
  264 | 	public final static class Builder {
  265 | 
  266 | 		private McpToolFilter toolFilter = (mcpClient, tool) -> true;
  267 | 
  268 | 		private List<McpAsyncClient> mcpClients = List.of();
  269 | 
  270 | 		private McpToolNamePrefixGenerator toolNamePrefixGenerator = new DefaultMcpToolNamePrefixGenerator();
  271 | 
  272 | 		private ToolContextToMcpMetaConverter toolContextToMcpMetaConverter = ToolContextToMcpMetaConverter
  273 | 			.defaultConverter();
  274 | 
  275 | 		private @Nullable ToolCallbackProvider modernToolCallbackProvider;
  276 | 
  277 | 		private Builder() {
  278 | 		}
  279 | 
  280 | 		/**
  281 | 		 * Sets tool filter.
  282 | 		 * @param toolFilter filter for discovered tools
  283 | 		 * @return this builder
  284 | 		 */
  285 | 		public Builder toolFilter(McpToolFilter toolFilter) {
  286 | 			Assert.notNull(toolFilter, "Tool filter must not be null");
  287 | 			this.toolFilter = toolFilter;
  288 | 			return this;
  289 | 		}
  290 | 
  291 | 		/**
  292 | 		 * Sets MCP clients.
  293 | 		 * @param mcpClients list of MCP clients
  294 | 		 * @return this builder
  295 | 		 */
  296 | 		public Builder mcpClients(List<McpAsyncClient> mcpClients) {
  297 | 			Assert.notNull(mcpClients, "MCP clients list must not be null");
  298 | 			this.mcpClients = mcpClients;
  299 | 			return this;
  300 | 		}
  301 | 
  302 | 		/**
  303 | 		 * Sets MCP clients.
  304 | 		 * @param mcpClients MCP clients as varargs
  305 | 		 * @return this builder
  306 | 		 */
  307 | 		public Builder mcpClients(McpAsyncClient... mcpClients) {
  308 | 			Assert.notNull(mcpClients, "MCP clients must not be null");
  309 | 			this.mcpClients = List.of(mcpClients);
  310 | 			return this;
  311 | 		}
  312 | 
  313 | 		/**
  314 | 		 * Sets tool name prefix generator.
  315 | 		 * @param toolNamePrefixGenerator generator for tool name prefixes
  316 | 		 * @return this builder
  317 | 		 */
  318 | 		public Builder toolNamePrefixGenerator(McpToolNamePrefixGenerator toolNamePrefixGenerator) {
  319 | 			Assert.notNull(toolNamePrefixGenerator, "Tool name prefix generator must not be null");
  320 | 			this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  321 | 			return this;
  322 | 		}
  323 | 
  324 | 		/**
  325 | 		 * Sets tool context to MCP metadata converter.
  326 | 		 * @param toolContextToMcpMetaConverter converter for tool context
  327 | 		 * @return this builder
  328 | 		 */
  329 | 		public Builder toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
  330 | 			Assert.notNull(toolContextToMcpMetaConverter, "Tool context to MCP meta converter must not be null");
  331 | 			this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  332 | 			return this;
  333 | 		}
  334 | 
  335 | 		/**
  336 | 		 * Sets the provider of the tools of the MCP connections that speak the 2026-07-28
  337 | 		 * ("modern") protocol revision, typically an
  338 | 		 * {@code AsyncModernMcpToolCallbackProvider}. Its callbacks are returned together
  339 | 		 * with those of the MCP clients, so that an application that talks to legacy and
  340 | 		 * modern servers registers a single provider. They are resolved on every call to
  341 | 		 * {@link AsyncMcpToolCallbackProvider#getToolCallbacks()} and are neither cached
  342 | 		 * nor affected by {@link AsyncMcpToolCallbackProvider#invalidateCache()}.
  343 | 		 * @param modernToolCallbackProvider provider of the modern connections' tools
  344 | 		 * @return this builder
  345 | 		 * @since 2.1.0
  346 | 		 */
  347 | 		public Builder modernToolCallbackProvider(ToolCallbackProvider modernToolCallbackProvider) {
  348 | 			Assert.notNull(modernToolCallbackProvider, "Modern tool callback provider must not be null");
  349 | 			this.modernToolCallbackProvider = modernToolCallbackProvider;
  350 | 			return this;
  351 | 		}
  352 | 
  353 | 		public AsyncMcpToolCallbackProvider build() {
  354 | 			return new AsyncMcpToolCallbackProvider(this.toolFilter, this.toolNamePrefixGenerator,
  355 | 					this.toolContextToMcpMetaConverter, this.mcpClients, this.modernToolCallbackProvider);
  356 | 		}
  357 | 
  358 | 	}
  359 | 
  360 | }
```
