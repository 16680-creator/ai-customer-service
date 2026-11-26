<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/SyncModernMcpToolCallbackProvider.java -->
<!-- 正文引用：[[源码:SyncModernMcpToolCallbackProvider.java#L起-L止]] ；代码块头部：(src: SyncModernMcpToolCallbackProvider.java#L起-L止) -->

# SyncModernMcpToolCallbackProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/SyncModernMcpToolCallbackProvider.java` |
| 行数 | 373 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:11.153Z |

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
   19 | import java.time.Duration;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | import java.util.function.LongSupplier;
   23 | 
   24 | import io.modelcontextprotocol.modern.McpSchema.ListToolsResult;
   25 | import io.modelcontextprotocol.modern.McpSchema.Tool;
   26 | import io.modelcontextprotocol.modern.client.McpSyncClient;
   27 | import io.modelcontextprotocol.modern.client.SubscriptionEvent;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
   33 | import org.springframework.ai.tool.ToolCallback;
   34 | import org.springframework.ai.tool.ToolCallbackProvider;
   35 | import org.springframework.ai.tool.support.ToolUtils;
   36 | import org.springframework.core.Ordered;
   37 | import org.springframework.util.Assert;
   38 | 
   39 | /**
   40 |  * Exposes the tools of modern MCP servers as Spring AI {@link ToolCallback}s.
   41 |  * <p>
   42 |  * Each call to {@link #getToolCallbacks()} asks every {@link SyncModernMcpConnection} for
   43 |  * the client that serves the current request, lists that client's tools, and returns
   44 |  * callbacks bound to that client. A {@code ChatClient} calls this method once for each
   45 |  * request, so register the provider with {@code defaultToolCallbacks(provider)} rather
   46 |  * than the callbacks it returns.
   47 |  * <p>
   48 |  * The server decides how long a tool listing may be kept and who may share it, with the
   49 |  * {@code ttlMs} and {@code cacheScope} of the listing:
   50 |  * <ul>
   51 |  * <li>A listing with a {@code ttlMs} of zero is never kept, and every call to
   52 |  * {@link #getToolCallbacks()} lists the tools again. Servers send zero unless configured
   53 |  * otherwise.</li>
   54 |  * <li>A {@code public} listing is the same for every caller. The provider keeps it for
   55 |  * its {@code ttlMs} and uses it for all requests, whichever client serves them.</li>
   56 |  * <li>A {@code private} listing may differ from caller to caller, so the provider does
   57 |  * not keep it. It is only kept by a client that outlives a request, as the client of a
   58 |  * {@link SyncModernMcpConnection#shared shared} connection does, or the client that a
   59 |  * {@link SyncModernMcpConnection#scoped scoped} connection keeps for a user. Use a shared
   60 |  * connection when the application calls the server under a single identity.</li>
   61 |  * </ul>
   62 |  * A tool call is never served from a kept listing: it always goes to the server, through
   63 |  * the client of the request that resolved the callback.
   64 |  * <p>
   65 |  * A kept listing is used until its {@code ttlMs} has passed. To drop it as soon as the
   66 |  * server reports that its tools changed, register the provider as a listener of a
   67 |  * {@link SyncModernMcpChangeSubscriber}, which keeps a listen stream open to the server.
   68 |  * <p>
   69 |  * A server that cannot be reached does not fail the call: its tools are left out and a
   70 |  * warning is logged, so the tools of the other connections stay usable. A server counts
   71 |  * as unreachable when the connection cannot be established, the exchange times out or
   72 |  * breaks, or the server answers with a {@code 5xx}, {@code 408} or {@code 429} status.
   73 |  * Every other failure is thrown, in particular an authorization failure, so that the
   74 |  * application can ask the user to log in again.
   75 |  * <p>
   76 |  * A server that could not be reached is not asked again until a cool-down time has passed
   77 |  * (30 seconds unless set otherwise). Otherwise a server that accepts connections but does
   78 |  * not answer would hold up every request for the whole timeout of the listing.
   79 |  *
   80 |  * @author Christian Tzolov
   81 |  * @since 2.1.0
   82 |  * @see SyncModernMcpConnection
   83 |  * @see SyncModernMcpToolCallback
   84 |  */
   85 | public final class SyncModernMcpToolCallbackProvider
   86 | 		implements ToolCallbackProvider, SyncModernMcpChangeListener, Ordered {
   87 | 
   88 | 	private static final Log logger = LogFactory.getLog(SyncModernMcpToolCallbackProvider.class);
   89 | 
   90 | 	private static final Duration DEFAULT_UNREACHABLE_COOL_DOWN = Duration.ofSeconds(30);
   91 | 
   92 | 	private final List<SyncModernMcpConnection> connections;
   93 | 
   94 | 	/** What is kept of the tool listings, and which servers are left alone for now. */
   95 | 	private final ModernToolListings<McpSyncClient> listings;
   96 | 
   97 | 	private final McpModernToolFilter toolFilter;
   98 | 
   99 | 	private final McpModernToolNamePrefixGenerator toolNamePrefixGenerator;
  100 | 
  101 | 	private final ToolContextToMcpMetaConverter toolContextToMcpMetaConverter;
  102 | 
  103 | 	private final @Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer;
  104 | 
  105 | 	private SyncModernMcpToolCallbackProvider(List<SyncModernMcpConnection> connections, McpModernToolFilter toolFilter,
  106 | 			McpModernToolNamePrefixGenerator toolNamePrefixGenerator,
  107 | 			ToolContextToMcpMetaConverter toolContextToMcpMetaConverter,
  108 | 			@Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer, Duration unreachableCoolDown,
  109 | 			LongSupplier nanoClock) {
  110 | 		Assert.notNull(connections, "connections must not be null");
  111 | 		Assert.notNull(toolFilter, "toolFilter must not be null");
  112 | 		Assert.notNull(toolNamePrefixGenerator, "toolNamePrefixGenerator must not be null");
  113 | 		Assert.notNull(toolContextToMcpMetaConverter, "toolContextToMcpMetaConverter must not be null");
  114 | 		this.connections = List.copyOf(connections);
  115 | 		this.toolFilter = toolFilter;
  116 | 		this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  117 | 		this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  118 | 		this.toolCallCustomizer = toolCallCustomizer;
  119 | 		this.listings = new ModernToolListings<>(logger, "SyncModernMcpClientScope", unreachableCoolDown, nanoClock);
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Creates a builder for a {@link SyncModernMcpToolCallbackProvider}.
  124 | 	 * @return the builder
  125 | 	 */
  126 | 	public static Builder builder() {
  127 | 		return new Builder();
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Returns a callback for each tool of every connection that the filter accepts. The
  132 | 	 * tools of a connection are listed unless a public listing of them is still kept. The
  133 | 	 * callbacks of a connection are bound to the client that connection returned for this
  134 | 	 * call. The tools of a connection whose server cannot be reached are left out, and
  135 | 	 * that server is not asked again until the cool-down time has passed.
  136 | 	 * @return the tool callbacks
  137 | 	 * @throws IllegalStateException if two tools end up with the same name
  138 | 	 */
  139 | 	@Override
  140 | 	public ToolCallback[] getToolCallbacks() {
  141 | 		List<ToolCallback> toolCallbacks = new ArrayList<>();
  142 | 		for (SyncModernMcpConnection connection : this.connections) {
  143 | 			McpModernConnectionInfo connectionInfo = connection.info();
  144 | 			if (this.listings.isCoolingDown(connection)) {
  145 | 				continue;
  146 | 			}
  147 | 			McpSyncClient mcpClient = connection.client();
  148 | 			List<Tool> tools = this.listings.tools(connection, connectionInfo, connection.clientPerRequest(), mcpClient,
  149 | 					SyncModernMcpToolCallbackProvider::list);
  150 | 			if (tools == null) {
  151 | 				// The server cannot be reached.
  152 | 				continue;
  153 | 			}
  154 | 			for (Tool tool : tools) {
  155 | 				if (this.toolFilter.test(connectionInfo, tool)) {
  156 | 					SyncModernMcpToolCallback.Builder toolCallback = SyncModernMcpToolCallback.builder()
  157 | 						.mcpClient(mcpClient)
  158 | 						.tool(tool)
  159 | 						.prefixedToolName(this.toolNamePrefixGenerator.prefixedToolName(connectionInfo, tool))
  160 | 						.toolContextToMcpMetaConverter(this.toolContextToMcpMetaConverter)
  161 | 						.connectionInfo(connectionInfo);
  162 | 					if (this.toolCallCustomizer != null) {
  163 | 						toolCallback.toolCallCustomizer(this.toolCallCustomizer);
  164 | 					}
  165 | 					toolCallbacks.add(toolCallback.build());
  166 | 				}
  167 | 			}
  168 | 		}
  169 | 		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
  170 | 		if (!duplicateToolNames.isEmpty()) {
  171 | 			throw new IllegalStateException(
  172 | 					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
  173 | 		}
  174 | 		return toolCallbacks.toArray(new ToolCallback[0]);
  175 | 	}
  176 | 
  177 | 	private static ListToolsResult list(McpSyncClient mcpClient, boolean fromServer) {
  178 | 		return fromServer ? mcpClient.listTools().skipCache().list() : mcpClient.listTools().list();
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * The provider hears about a change before the other listeners of a
  183 | 	 * {@link SyncModernMcpChangeSubscriber}, so that a listener that asks it for the
  184 | 	 * tools finds the kept listing dropped.
  185 | 	 * @return the highest precedence
  186 | 	 */
  187 | 	@Override
  188 | 	public int getOrder() {
  189 | 		return Ordered.HIGHEST_PRECEDENCE;
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Drops the kept tool listing of a connection, so that the next call to
  194 | 	 * {@link #getToolCallbacks()} lists its tools again. Use it when the tools of the
  195 | 	 * server are known to have changed before the {@code ttlMs} of the listing has
  196 | 	 * passed. A client that keeps a listing itself, as a shared client or a client that a
  197 | 	 * {@link SyncModernMcpClientScope} keeps for a user does, asks the server the next
  198 | 	 * time it lists the tools.
  199 | 	 * @param connectionName the name of the connection
  200 | 	 */
  201 | 	public void invalidateToolListing(String connectionName) {
  202 | 		Assert.notNull(connectionName, "connectionName must not be null");
  203 | 		this.listings.invalidate(connectionName,
  204 | 				this.connections.stream()
  205 | 					.filter(connection -> connection.info().name().equals(connectionName))
  206 | 					.toList());
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Ends the cool-down of the connection, if its server could not be reached before: a
  211 | 	 * listen stream that was established shows that it can be reached again.
  212 | 	 */
  213 | 	@Override
  214 | 	public void streamEstablished(McpModernConnectionInfo connectionInfo) {
  215 | 		for (SyncModernMcpConnection connection : this.connections) {
  216 | 			if (connection.info().name().equals(connectionInfo.name())) {
  217 | 				this.listings.serverReachable(connection, connection.info());
  218 | 			}
  219 | 		}
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Drops the kept tool listing of the connection when its server reports that its
  224 | 	 * tools changed.
  225 | 	 */
  226 | 	@Override
  227 | 	public void changed(McpModernConnectionInfo connectionInfo, SubscriptionEvent event) {
  228 | 		if (event instanceof SubscriptionEvent.ToolsListChanged) {
  229 | 			this.invalidateToolListing(connectionInfo.name());
  230 | 		}
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Drops the kept tool listing of the connection, because a change of its tools may
  235 | 	 * have gone unreported.
  236 | 	 */
  237 | 	@Override
  238 | 	public void changesMayBeMissed(McpModernConnectionInfo connectionInfo) {
  239 | 		this.invalidateToolListing(connectionInfo.name());
  240 | 	}
  241 | 
  242 | 	/**
  243 | 	 * Builder for {@link SyncModernMcpToolCallbackProvider}.
  244 | 	 */
  245 | 	public static final class Builder {
  246 | 
  247 | 		private final List<SyncModernMcpConnection> connections = new ArrayList<>();
  248 | 
  249 | 		private McpModernToolFilter toolFilter = McpModernToolFilter.acceptAll();
  250 | 
  251 | 		private McpModernToolNamePrefixGenerator toolNamePrefixGenerator = McpModernToolNamePrefixGenerator
  252 | 			.connectionPrefix();
  253 | 
  254 | 		private ToolContextToMcpMetaConverter toolContextToMcpMetaConverter = ToolContextToMcpMetaConverter
  255 | 			.defaultConverter();
  256 | 
  257 | 		private @Nullable SyncModernMcpToolCallCustomizer toolCallCustomizer;
  258 | 
  259 | 		private Duration unreachableCoolDown = DEFAULT_UNREACHABLE_COOL_DOWN;
  260 | 
  261 | 		private LongSupplier nanoClock = System::nanoTime;
  262 | 
  263 | 		private Builder() {
  264 | 		}
  265 | 
  266 | 		/**
  267 | 		 * Sets the connections whose tools are exposed, replacing those set before.
  268 | 		 * @param connections the connections
  269 | 		 * @return this builder
  270 | 		 */
  271 | 		public Builder connections(List<SyncModernMcpConnection> connections) {
  272 | 			Assert.notNull(connections, "connections must not be null");
  273 | 			this.connections.clear();
  274 | 			this.connections.addAll(connections);
  275 | 			return this;
  276 | 		}
  277 | 
  278 | 		/**
  279 | 		 * Adds a connection whose tools are exposed.
  280 | 		 * @param connection the connection
  281 | 		 * @return this builder
  282 | 		 */
  283 | 		public Builder connection(SyncModernMcpConnection connection) {
  284 | 			Assert.notNull(connection, "connection must not be null");
  285 | 			this.connections.add(connection);
  286 | 			return this;
  287 | 		}
  288 | 
  289 | 		/**
  290 | 		 * Sets the filter that decides which tools are exposed. Defaults to
  291 | 		 * {@link McpModernToolFilter#acceptAll()}.
  292 | 		 * @param toolFilter the tool filter
  293 | 		 * @return this builder
  294 | 		 */
  295 | 		public Builder toolFilter(McpModernToolFilter toolFilter) {
  296 | 			Assert.notNull(toolFilter, "toolFilter must not be null");
  297 | 			this.toolFilter = toolFilter;
  298 | 			return this;
  299 | 		}
  300 | 
  301 | 		/**
  302 | 		 * Sets the generator of the tool names presented to the model. Defaults to
  303 | 		 * {@link McpModernToolNamePrefixGenerator#connectionPrefix()}.
  304 | 		 * @param toolNamePrefixGenerator the tool name generator
  305 | 		 * @return this builder
  306 | 		 */
  307 | 		public Builder toolNamePrefixGenerator(McpModernToolNamePrefixGenerator toolNamePrefixGenerator) {
  308 | 			Assert.notNull(toolNamePrefixGenerator, "toolNamePrefixGenerator must not be null");
  309 | 			this.toolNamePrefixGenerator = toolNamePrefixGenerator;
  310 | 			return this;
  311 | 		}
  312 | 
  313 | 		/**
  314 | 		 * Sets the converter that turns {@code ToolContext} entries into the
  315 | 		 * {@code _meta} entries of a tool call.
  316 | 		 * @param toolContextToMcpMetaConverter the converter
  317 | 		 * @return this builder
  318 | 		 */
  319 | 		public Builder toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter toolContextToMcpMetaConverter) {
  320 | 			Assert.notNull(toolContextToMcpMetaConverter, "toolContextToMcpMetaConverter must not be null");
  321 | 			this.toolContextToMcpMetaConverter = toolContextToMcpMetaConverter;
  322 | 			return this;
  323 | 		}
  324 | 
  325 | 		/**
  326 | 		 * Sets the customizer that is applied to every tool call, with the
  327 | 		 * {@code ToolContext} of that call. Use it to set what depends on the call, such
  328 | 		 * as an elicitation handler that needs to know the user it has to ask.
  329 | 		 * @param toolCallCustomizer the customizer
  330 | 		 * @return this builder
  331 | 		 */
  332 | 		public Builder toolCallCustomizer(SyncModernMcpToolCallCustomizer toolCallCustomizer) {
  333 | 			Assert.notNull(toolCallCustomizer, "toolCallCustomizer must not be null");
  334 | 			this.toolCallCustomizer = toolCallCustomizer;
  335 | 			return this;
  336 | 		}
  337 | 
  338 | 		/**
  339 | 		 * Sets how long a server that could not be reached is left alone before it is
  340 | 		 * asked for its tools again. During that time its tools are left out without
  341 | 		 * contacting it. Defaults to 30 seconds; zero asks the server on every call.
  342 | 		 * @param unreachableCoolDown the cool-down time, not negative
  343 | 		 * @return this builder
  344 | 		 */
  345 | 		public Builder unreachableCoolDown(Duration unreachableCoolDown) {
  346 | 			Assert.notNull(unreachableCoolDown, "unreachableCoolDown must not be null");
  347 | 			Assert.isTrue(!unreachableCoolDown.isNegative(), "unreachableCoolDown must not be negative");
  348 | 			this.unreachableCoolDown = unreachableCoolDown;
  349 | 			return this;
  350 | 		}
  351 | 
  352 | 		/**
  353 | 		 * Sets the clock that kept listings and cool-downs are timed with, in
  354 | 		 * nanoseconds. For tests.
  355 | 		 */
  356 | 		Builder nanoClock(LongSupplier nanoClock) {
  357 | 			this.nanoClock = nanoClock;
  358 | 			return this;
  359 | 		}
  360 | 
  361 | 		/**
  362 | 		 * Builds the provider.
  363 | 		 * @return the provider
  364 | 		 */
  365 | 		public SyncModernMcpToolCallbackProvider build() {
  366 | 			return new SyncModernMcpToolCallbackProvider(this.connections, this.toolFilter,
  367 | 					this.toolNamePrefixGenerator, this.toolContextToMcpMetaConverter, this.toolCallCustomizer,
  368 | 					this.unreachableCoolDown, this.nanoClock);
  369 | 		}
  370 | 
  371 | 	}
  372 | 
  373 | }
```
