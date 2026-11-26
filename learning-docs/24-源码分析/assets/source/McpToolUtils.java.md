<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/McpToolUtils.java -->
<!-- 正文引用：[[源码:McpToolUtils.java#L起-L止]] ；代码块头部：(src: McpToolUtils.java#L起-L止) -->

# McpToolUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/McpToolUtils.java` |
| 行数 | 486 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:29:10.716Z |

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
   19 | import java.util.List;
   20 | import java.util.Locale;
   21 | import java.util.Map;
   22 | import java.util.Optional;
   23 | import java.util.function.BiFunction;
   24 | import java.util.stream.Stream;
   25 | 
   26 | import com.fasterxml.jackson.annotation.JsonAlias;
   27 | import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
   28 | import io.micrometer.common.util.StringUtils;
   29 | import io.modelcontextprotocol.client.McpAsyncClient;
   30 | import io.modelcontextprotocol.client.McpSyncClient;
   31 | import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
   32 | import io.modelcontextprotocol.server.McpServerFeatures;
   33 | import io.modelcontextprotocol.server.McpStatelessServerFeatures;
   34 | import io.modelcontextprotocol.server.McpSyncServerExchange;
   35 | import io.modelcontextprotocol.spec.McpSchema;
   36 | import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
   37 | import io.modelcontextprotocol.spec.McpSchema.Role;
   38 | import org.jspecify.annotations.Nullable;
   39 | import reactor.core.publisher.Mono;
   40 | import reactor.core.scheduler.Schedulers;
   41 | 
   42 | import org.springframework.ai.chat.model.ToolContext;
   43 | import org.springframework.ai.tool.ToolCallback;
   44 | import org.springframework.ai.tool.definition.DefaultToolDefinition;
   45 | import org.springframework.ai.tool.definition.ToolDefinition;
   46 | import org.springframework.ai.util.JacksonUtils;
   47 | import org.springframework.ai.util.JsonHelper;
   48 | import org.springframework.ai.util.json.schema.JsonSchemaUtils;
   49 | import org.springframework.util.CollectionUtils;
   50 | import org.springframework.util.MimeType;
   51 | 
   52 | /**
   53 |  * Utility class that provides helper methods for working with Model Context Protocol
   54 |  * (MCP) tools in a Spring AI environment. This class facilitates the integration between
   55 |  * Spring AI's tool callbacks and MCP's tool system.
   56 |  *
   57 |  * <p>
   58 |  * The MCP tool system enables servers to expose executable functionality to language
   59 |  * models, allowing them to interact with external systems, perform computations, and take
   60 |  * actions in the real world. Each tool is uniquely identified by a name and includes
   61 |  * metadata describing its schema.
   62 |  *
   63 |  * <p>
   64 |  * This helper class provides methods to:
   65 |  * <ul>
   66 |  * <li>Convert Spring AI's {@link ToolCallback} instances to MCP tool specification</li>
   67 |  * <li>Generate JSON schemas for tool input validation</li>
   68 |  * </ul>
   69 |  *
   70 |  * @author Christian Tzolov
   71 |  * @author Ilayaperumal Gopinathan
   72 |  */
   73 | public final class McpToolUtils {
   74 | 
   75 | 	private static final JsonHelper jsonHelper = new JsonHelper();
   76 | 
   77 | 	/**
   78 | 	 * The name of tool context key used to store the MCP exchange object.
   79 | 	 */
   80 | 	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
   81 | 
   82 | 	private McpToolUtils() {
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * @param prefix Client name, combination of client info name and the 'server'
   87 | 	 * connection name.
   88 | 	 * @param title Server connection name
   89 | 	 * @param toolName original MCP server tool name.
   90 | 	 * @return the prefix to use for the tool to avoid name collisions.
   91 | 	 */
   92 | 	public static String prefixedToolName(String prefix, @Nullable String title, String toolName) {
   93 | 
   94 | 		if (StringUtils.isEmpty(prefix) || StringUtils.isEmpty(toolName)) {
   95 | 			throw new IllegalArgumentException("Prefix or toolName cannot be null or empty");
   96 | 		}
   97 | 
   98 | 		String input = shorten(format(prefix));
   99 | 		if (!StringUtils.isEmpty(title)) {
  100 | 			input = input + "_" + format(title); // Do not shorten the title.
  101 | 		}
  102 | 
  103 | 		input = input + "_" + format(toolName);
  104 | 
  105 | 		// If the string is longer than 64 characters, keep the last 64 characters
  106 | 		if (input.length() > 64) {
  107 | 			input = input.substring(input.length() - 64);
  108 | 		}
  109 | 
  110 | 		return input;
  111 | 	}
  112 | 
  113 | 	public static String prefixedToolName(String prefix, String toolName) {
  114 | 		return prefixedToolName(prefix, null, toolName);
  115 | 	}
  116 | 
  117 | 	public static String format(String input) {
  118 | 		// Replace any character that isn't alphanumeric, underscore, or hyphen with
  119 | 		// concatenation. Support Han script + CJK blocks for complete Chinese character
  120 | 		// coverage
  121 | 		String formatted = input
  122 | 			.replaceAll("[^\\p{IsHan}\\p{InCJK_Unified_Ideographs}\\p{InCJK_Compatibility_Ideographs}a-zA-Z0-9_-]", "");
  123 | 
  124 | 		return formatted.replaceAll("-", "_");
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Shortens a string by taking the first letter of each word separated by underscores
  129 | 	 * @param input String in format "Word1_Word2_Word3_server"
  130 | 	 * @return Shortened string with first letters in lowercase "w_w_w_s"
  131 | 	 */
  132 | 	private static String shorten(String input) {
  133 | 		if (input == null || input.isEmpty()) {
  134 | 			return "";
  135 | 		}
  136 | 
  137 | 		return Stream.of(input.toLowerCase(Locale.ROOT).split("_"))
  138 | 			.filter(word -> !word.isEmpty())
  139 | 			.map(word -> String.valueOf(word.charAt(0)))
  140 | 			.collect(java.util.stream.Collectors.joining("_"));
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Converts a list of Spring AI tool callbacks to MCP synchronous tool specification.
  145 | 	 * <p>
  146 | 	 * This method processes multiple tool callbacks in bulk, converting each one to its
  147 | 	 * corresponding MCP tool specification while maintaining synchronous execution
  148 | 	 * semantics.
  149 | 	 * @param toolCallbacks the list of tool callbacks to convert
  150 | 	 * @return a list of MCP synchronous tool specification
  151 | 	 */
  152 | 	public static List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecification(
  153 | 			List<ToolCallback> toolCallbacks) {
  154 | 		return toolCallbacks.stream().map(McpToolUtils::toSyncToolSpecification).toList();
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Convenience method to convert a variable number of tool callbacks to MCP
  159 | 	 * synchronous tool specification.
  160 | 	 * <p>
  161 | 	 * This is a varargs wrapper around {@link #toSyncToolSpecification(List)} for easier
  162 | 	 * usage when working with individual callbacks.
  163 | 	 * @param toolCallbacks the tool callbacks to convert
  164 | 	 * @return a list of MCP synchronous tool specification
  165 | 	 */
  166 | 	public static List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(
  167 | 			ToolCallback... toolCallbacks) {
  168 | 		return toSyncToolSpecification(List.of(toolCallbacks));
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Converts a Spring AI ToolCallback to an MCP SyncToolSpecification. This enables
  173 | 	 * Spring AI functions to be exposed as MCP tools that can be discovered and invoked
  174 | 	 * by language models.
  175 | 	 *
  176 | 	 * <p>
  177 | 	 * The conversion process:
  178 | 	 * <ul>
  179 | 	 * <li>Creates an MCP Tool with the function's name and input schema</li>
  180 | 	 * <li>Wraps the function's execution in a SyncToolSpecification that handles the MCP
  181 | 	 * protocol</li>
  182 | 	 * <li>Provides error handling and result formatting according to MCP
  183 | 	 * specifications</li>
  184 | 	 * </ul>
  185 | 	 *
  186 | 	 * You can use the ToolCallback builder to create a new instance of ToolCallback using
  187 | 	 * either java.util.function.Function or Method reference.
  188 | 	 * @param toolCallback the Spring AI function callback to convert
  189 | 	 * @return an MCP SyncToolSpecification that wraps the function callback
  190 | 	 * @throws RuntimeException if there's an error during the function execution
  191 | 	 */
  192 | 	public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback) {
  193 | 		return toSyncToolSpecification(toolCallback, null);
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Converts a Spring AI ToolCallback to an MCP SyncToolSpecification. This enables
  198 | 	 * Spring AI functions to be exposed as MCP tools that can be discovered and invoked
  199 | 	 * by language models.
  200 | 	 * @param toolCallback the Spring AI function callback to convert
  201 | 	 * @param mimeType the MIME type of the output content
  202 | 	 * @return an MCP SyncToolSpecification that wraps the function callback
  203 | 	 * @throws RuntimeException if there's an error during the function execution
  204 | 	 */
  205 | 	public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback,
  206 | 			@Nullable MimeType mimeType) {
  207 | 
  208 | 		SharedSyncToolSpecification sharedSpec = toSharedSyncToolSpecification(toolCallback, mimeType);
  209 | 
  210 | 		return new McpServerFeatures.SyncToolSpecification(sharedSpec.tool(),
  211 | 				(exchange, request) -> sharedSpec.sharedHandler().apply(exchange, request));
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Converts a Spring AI ToolCallback to an MCP StatelessSyncToolSpecification. This
  216 | 	 * enables Spring AI functions to be exposed as MCP tools that can be discovered and
  217 | 	 * invoked by language models.
  218 | 	 *
  219 | 	 * You can use the ToolCallback builder to create a new instance of ToolCallback using
  220 | 	 * either java.util.function.Function or Method reference.
  221 | 	 * @param toolCallback the Spring AI function callback to convert
  222 | 	 * @param mimeType the MIME type of the output content
  223 | 	 * @return an MCP StatelessSyncToolSpecification that wraps the function callback
  224 | 	 * @throws RuntimeException if there's an error during the function execution
  225 | 	 */
  226 | 	public static McpStatelessServerFeatures.SyncToolSpecification toStatelessSyncToolSpecification(
  227 | 			ToolCallback toolCallback, @Nullable MimeType mimeType) {
  228 | 
  229 | 		var sharedSpec = toSharedSyncToolSpecification(toolCallback, mimeType);
  230 | 
  231 | 		return McpStatelessServerFeatures.SyncToolSpecification.builder()
  232 | 			.tool(sharedSpec.tool())
  233 | 			.callHandler((exchange, request) -> sharedSpec.sharedHandler().apply(exchange, request))
  234 | 			.build();
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Creates a Spring AI ToolDefinition from an MCP Tool.
  239 | 	 * @param prefixedToolName the prefixed name for the tool
  240 | 	 * @param tool the MCP tool
  241 | 	 * @return a ToolDefinition with normalized input schema
  242 | 	 */
  243 | 	public static ToolDefinition createToolDefinition(String prefixedToolName, McpSchema.Tool tool) {
  244 | 		return DefaultToolDefinition.builder()
  245 | 			.name(prefixedToolName)
  246 | 			.description(tool.description())
  247 | 			.inputSchema(JsonSchemaUtils.ensureValidInputSchema(jsonHelper.toJson(tool.inputSchema())))
  248 | 			.build();
  249 | 	}
  250 | 
  251 | 	private static SharedSyncToolSpecification toSharedSyncToolSpecification(ToolCallback toolCallback,
  252 | 			@Nullable MimeType mimeType) {
  253 | 
  254 | 		var tool = McpSchema.Tool
  255 | 			.builder(toolCallback.getToolDefinition().name(),
  256 | 					new JacksonMcpJsonMapper(JacksonUtils.getDefaultJsonMapper()),
  257 | 					toolCallback.getToolDefinition().inputSchema())
  258 | 			.description(toolCallback.getToolDefinition().description())
  259 | 			.build();
  260 | 
  261 | 		return new SharedSyncToolSpecification(tool, (exchangeOrContext, request) -> {
  262 | 			try {
  263 | 				String callResult = toolCallback.call(jsonHelper.toJson(request.arguments()),
  264 | 						new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchangeOrContext)));
  265 | 				if (mimeType != null && mimeType.toString().startsWith("image")) {
  266 | 					McpSchema.Annotations annotations = McpSchema.Annotations.builder()
  267 | 						.audience(List.of(Role.ASSISTANT))
  268 | 						.build();
  269 | 					return McpSchema.CallToolResult.builder()
  270 | 						.content(List.of(McpSchema.ImageContent.builder(callResult, mimeType.toString())
  271 | 							.annotations(annotations)
  272 | 							.build()))
  273 | 						.isError(false)
  274 | 						.build();
  275 | 				}
  276 | 				return McpSchema.CallToolResult.builder()
  277 | 					.content(List.of(McpSchema.TextContent.builder(callResult).build()))
  278 | 					.isError(false)
  279 | 					.build();
  280 | 			}
  281 | 			catch (Exception e) {
  282 | 				return McpSchema.CallToolResult.builder()
  283 | 					.content(List.of(McpSchema.TextContent.builder(e.getMessage()).build()))
  284 | 					.isError(true)
  285 | 					.build();
  286 | 			}
  287 | 		});
  288 | 	}
  289 | 
  290 | 	/**
  291 | 	 * Retrieves the MCP exchange object from the provided tool context if it exists.
  292 | 	 * @param toolContext the tool context from which to retrieve the MCP exchange
  293 | 	 * @return the MCP exchange object, or null if not present in the context
  294 | 	 */
  295 | 	public static Optional<McpSyncServerExchange> getMcpExchange(ToolContext toolContext) {
  296 | 		if (toolContext != null && toolContext.getContext().containsKey(TOOL_CONTEXT_MCP_EXCHANGE_KEY)) {
  297 | 			return Optional
  298 | 				.ofNullable((McpSyncServerExchange) toolContext.getContext().get(TOOL_CONTEXT_MCP_EXCHANGE_KEY));
  299 | 		}
  300 | 		return Optional.empty();
  301 | 	}
  302 | 
  303 | 	/**
  304 | 	 * Converts a list of Spring AI tool callbacks to MCP asynchronous tool specification.
  305 | 	 * <p>
  306 | 	 * This method processes multiple tool callbacks in bulk, converting each one to its
  307 | 	 * corresponding MCP tool specification while adding asynchronous execution
  308 | 	 * capabilities. The resulting specifications will execute their tools on a bounded
  309 | 	 * elastic scheduler.
  310 | 	 * @param toolCallbacks the list of tool callbacks to convert
  311 | 	 * @return a list of MCP asynchronous tool specifications
  312 | 	 */
  313 | 	public static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(
  314 | 			List<ToolCallback> toolCallbacks) {
  315 | 		return toolCallbacks.stream().map(McpToolUtils::toAsyncToolSpecification).toList();
  316 | 	}
  317 | 
  318 | 	/**
  319 | 	 * Convenience method to convert a variable number of tool callbacks to MCP
  320 | 	 * asynchronous tool specification.
  321 | 	 * <p>
  322 | 	 * This is a varargs wrapper around {@link #toAsyncToolSpecifications(List)} for
  323 | 	 * easier usage when working with individual callbacks.
  324 | 	 * @param toolCallbacks the tool callbacks to convert
  325 | 	 * @return a list of MCP asynchronous tool specifications
  326 | 	 * @see #toAsyncToolSpecifications(List)
  327 | 	 */
  328 | 	public static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(
  329 | 			ToolCallback... toolCallbacks) {
  330 | 		return toAsyncToolSpecifications(List.of(toolCallbacks));
  331 | 	}
  332 | 
  333 | 	/**
  334 | 	 * Converts a Spring AI tool callback to an MCP asynchronous tool specification.
  335 | 	 * <p>
  336 | 	 * This method enables Spring AI tools to be exposed as asynchronous MCP tools that
  337 | 	 * can be discovered and invoked by language models. The conversion process:
  338 | 	 * <ul>
  339 | 	 * <li>First converts the callback to a synchronous specification</li>
  340 | 	 * <li>Wraps the synchronous execution in a reactive Mono</li>
  341 | 	 * <li>Configures execution on a bounded elastic scheduler for non-blocking
  342 | 	 * operation</li>
  343 | 	 * </ul>
  344 | 	 * <p>
  345 | 	 * The resulting async specification will:
  346 | 	 * <ul>
  347 | 	 * <li>Execute the tool without blocking the calling thread</li>
  348 | 	 * <li>Handle errors and results asynchronously</li>
  349 | 	 * <li>Provide backpressure through Project Reactor</li>
  350 | 	 * </ul>
  351 | 	 * @param toolCallback the Spring AI tool callback to convert
  352 | 	 * @return an MCP asynchronous tool specification that wraps the tool callback
  353 | 	 * @see McpServerFeatures.AsyncToolSpecification
  354 | 	 * @see Mono
  355 | 	 * @see Schedulers#boundedElastic()
  356 | 	 */
  357 | 	public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(ToolCallback toolCallback) {
  358 | 		return toAsyncToolSpecification(toolCallback, null);
  359 | 	}
  360 | 
  361 | 	/**
  362 | 	 * Converts a Spring AI tool callback to an MCP asynchronous tool specification.
  363 | 	 * <p>
  364 | 	 * This method enables Spring AI tools to be exposed as asynchronous MCP tools that
  365 | 	 * can be discovered and invoked by language models. The conversion process:
  366 | 	 * <ul>
  367 | 	 * <li>First converts the callback to a synchronous specification</li>
  368 | 	 * <li>Wraps the synchronous execution in a reactive Mono</li>
  369 | 	 * <li>Configures execution on a bounded elastic scheduler for non-blocking
  370 | 	 * operation</li>
  371 | 	 * </ul>
  372 | 	 * <p>
  373 | 	 * The resulting async specification will:
  374 | 	 * <ul>
  375 | 	 * <li>Execute the tool without blocking the calling thread</li>
  376 | 	 * <li>Handle errors and results asynchronously</li>
  377 | 	 * <li>Provide backpressure through Project Reactor</li>
  378 | 	 * </ul>
  379 | 	 * @param toolCallback the Spring AI tool callback to convert
  380 | 	 * @param mimeType the MIME type of the output content
  381 | 	 * @return an MCP asynchronous tool specification that wraps the tool callback
  382 | 	 * @see McpServerFeatures.AsyncToolSpecification
  383 | 	 * @see Schedulers#boundedElastic()
  384 | 	 */
  385 | 	public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(ToolCallback toolCallback,
  386 | 			@Nullable MimeType mimeType) {
  387 | 
  388 | 		McpServerFeatures.SyncToolSpecification syncToolSpecification = toSyncToolSpecification(toolCallback, mimeType);
  389 | 
  390 | 		return McpServerFeatures.AsyncToolSpecification.builder()
  391 | 			.tool(syncToolSpecification.tool())
  392 | 			.callHandler((exchange, request) -> Mono
  393 | 				.fromCallable(
  394 | 						() -> syncToolSpecification.callHandler().apply(new McpSyncServerExchange(exchange), request))
  395 | 				.subscribeOn(Schedulers.boundedElastic()))
  396 | 			.build();
  397 | 	}
  398 | 
  399 | 	public static McpStatelessServerFeatures.AsyncToolSpecification toStatelessAsyncToolSpecification(
  400 | 			ToolCallback toolCallback, @Nullable MimeType mimeType) {
  401 | 
  402 | 		McpStatelessServerFeatures.SyncToolSpecification statelessSyncToolSpecification = toStatelessSyncToolSpecification(
  403 | 				toolCallback, mimeType);
  404 | 
  405 | 		return new McpStatelessServerFeatures.AsyncToolSpecification(statelessSyncToolSpecification.tool(),
  406 | 				(context, request) -> Mono
  407 | 					.fromCallable(() -> statelessSyncToolSpecification.callHandler().apply(context, request))
  408 | 					.subscribeOn(Schedulers.boundedElastic()));
  409 | 	}
  410 | 
  411 | 	/**
  412 | 	 * Convenience method to get tool callbacks from multiple synchronous MCP clients.
  413 | 	 * <p>
  414 | 	 * This is a varargs wrapper around {@link #getToolCallbacksFromSyncClients(List)} for
  415 | 	 * easier usage when working with individual clients.
  416 | 	 * @param mcpClients the synchronous MCP clients to get callbacks from
  417 | 	 * @return a list of tool callbacks from all provided clients
  418 | 	 * @see #getToolCallbacksFromSyncClients(List)
  419 | 	 */
  420 | 	public static List<ToolCallback> getToolCallbacksFromSyncClients(McpSyncClient... mcpClients) {
  421 | 		return getToolCallbacksFromSyncClients(List.of(mcpClients));
  422 | 	}
  423 | 
  424 | 	/**
  425 | 	 * Gets tool callbacks from a list of synchronous MCP clients.
  426 | 	 * <p>
  427 | 	 * This method:
  428 | 	 * <ol>
  429 | 	 * <li>Takes a list of synchronous MCP clients</li>
  430 | 	 * <li>Creates a provider for each client</li>
  431 | 	 * <li>Retrieves and combines all tool callbacks into a single list</li>
  432 | 	 * </ol>
  433 | 	 * @param mcpClients the list of synchronous MCP clients to get callbacks from
  434 | 	 * @return a list of tool callbacks from all provided clients
  435 | 	 */
  436 | 	public static List<ToolCallback> getToolCallbacksFromSyncClients(List<McpSyncClient> mcpClients) {
  437 | 
  438 | 		if (CollectionUtils.isEmpty(mcpClients)) {
  439 | 			return List.of();
  440 | 		}
  441 | 		return List.of((new SyncMcpToolCallbackProvider(mcpClients).getToolCallbacks()));
  442 | 	}
  443 | 
  444 | 	/**
  445 | 	 * Convenience method to get tool callbacks from multiple asynchronous MCP clients.
  446 | 	 * <p>
  447 | 	 * This is a varargs wrapper around {@link #getToolCallbacksFromAsyncClients(List)}
  448 | 	 * for easier usage when working with individual clients.
  449 | 	 * @param asyncMcpClients the asynchronous MCP clients to get callbacks from
  450 | 	 * @return a list of tool callbacks from all provided clients
  451 | 	 * @see #getToolCallbacksFromAsyncClients(List)
  452 | 	 */
  453 | 	public static List<ToolCallback> getToolCallbacksFromAsyncClients(McpAsyncClient... asyncMcpClients) {
  454 | 		return getToolCallbacksFromAsyncClients(List.of(asyncMcpClients));
  455 | 	}
  456 | 
  457 | 	/**
  458 | 	 * Gets tool callbacks from a list of asynchronous MCP clients.
  459 | 	 * <p>
  460 | 	 * This method:
  461 | 	 * <ol>
  462 | 	 * <li>Takes a list of asynchronous MCP clients</li>
  463 | 	 * <li>Creates a provider for each client</li>
  464 | 	 * <li>Retrieves and combines all tool callbacks into a single list</li>
  465 | 	 * </ol>
  466 | 	 * @param asyncMcpClients the list of asynchronous MCP clients to get callbacks from
  467 | 	 * @return a list of tool callbacks from all provided clients
  468 | 	 */
  469 | 	public static List<ToolCallback> getToolCallbacksFromAsyncClients(List<McpAsyncClient> asyncMcpClients) {
  470 | 
  471 | 		if (CollectionUtils.isEmpty(asyncMcpClients)) {
  472 | 			return List.of();
  473 | 		}
  474 | 		return List.of((AsyncMcpToolCallbackProvider.builder().mcpClients(asyncMcpClients).build().getToolCallbacks()));
  475 | 	}
  476 | 
  477 | 	@JsonIgnoreProperties(ignoreUnknown = true)
  478 | 	// @formatter:off
  479 | 	private record Base64Wrapper(@JsonAlias("mimetype") @Nullable MimeType mimeType, @JsonAlias({
  480 | 			"base64", "b64", "imageData" }) @Nullable String data) {
  481 | 	}
  482 | 
  483 | 	private record SharedSyncToolSpecification(McpSchema.Tool tool,
  484 | 											BiFunction<Object, CallToolRequest, McpSchema.CallToolResult> sharedHandler) {
  485 | 	}
  486 | }
```
