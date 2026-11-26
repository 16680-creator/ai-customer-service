<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/DefaultMcpToolNamePrefixGenerator.java -->
<!-- 正文引用：[[源码:DefaultMcpToolNamePrefixGenerator.java#L起-L止]] ；代码块头部：(src: DefaultMcpToolNamePrefixGenerator.java#L起-L止) -->

# DefaultMcpToolNamePrefixGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/DefaultMcpToolNamePrefixGenerator.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.040Z |

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
   20 | import java.util.Set;
   21 | import java.util.concurrent.ConcurrentHashMap;
   22 | import java.util.concurrent.atomic.AtomicInteger;
   23 | 
   24 | import io.modelcontextprotocol.spec.McpSchema;
   25 | import io.modelcontextprotocol.spec.McpSchema.Implementation;
   26 | import io.modelcontextprotocol.spec.McpSchema.Tool;
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | /**
   32 |  * Default implementation of {@link McpToolNamePrefixGenerator} that ensures unique tool
   33 |  * names for all client/server connections.
   34 |  *
   35 |  * <p>
   36 |  * This implementation ensures that tool names are unique across different MCP clients and
   37 |  * servers by tracking existing connections and appending a counter to duplicate tool
   38 |  * names.
   39 |  *
   40 |  * <p>
   41 |  * For each unique combination of (client, server, tool), e.g. each connection, the tool
   42 |  * name is generated only once. If a tool name has already been used, a prefix with a
   43 |  * counter is added to make it unique (e.g., "alt_1_toolName", "alt_2_toolName", etc.).
   44 |  *
   45 |  * <p>
   46 |  * This implementation is thread-safe.
   47 |  *
   48 |  * @author Christian Tzolov
   49 |  */
   50 | public class DefaultMcpToolNamePrefixGenerator implements McpToolNamePrefixGenerator {
   51 | 
   52 | 	private static final Log logger = LogFactory.getLog(DefaultMcpToolNamePrefixGenerator.class);
   53 | 
   54 | 	// Idempotency tracking. For a given combination of (client, server, tool) we will
   55 | 	// generate a unique tool name only once and return the same name on later calls.
   56 | 	private final Map<ConnectionId, String> existingConnections = new ConcurrentHashMap<>();
   57 | 
   58 | 	private final Set<String> allUsedToolNames = ConcurrentHashMap.newKeySet();
   59 | 
   60 | 	private final AtomicInteger counter = new AtomicInteger(1);
   61 | 
   62 | 	@Override
   63 | 	public String prefixedToolName(McpConnectionInfo mcpConnectionInfo, McpSchema.Tool tool) {
   64 | 		var serverInfo = mcpConnectionInfo.initializeResult() != null
   65 | 				? mcpConnectionInfo.initializeResult().serverInfo() : null;
   66 | 		ConnectionId connectionId = new ConnectionId(mcpConnectionInfo.clientInfo(), serverInfo, tool);
   67 | 
   68 | 		return this.existingConnections.computeIfAbsent(connectionId, id -> uniqueToolName(tool));
   69 | 	}
   70 | 
   71 | 	private String uniqueToolName(McpSchema.Tool tool) {
   72 | 		String uniqueToolName = McpToolUtils.format(tool.name());
   73 | 		if (!this.allUsedToolNames.add(uniqueToolName)) {
   74 | 			uniqueToolName = "alt_" + this.counter.getAndIncrement() + "_" + uniqueToolName;
   75 | 			this.allUsedToolNames.add(uniqueToolName);
   76 | 			if (logger.isWarnEnabled()) {
   77 | 				logger.warn("Tool name '" + tool.name() + "' already exists. Using unique tool name '" + uniqueToolName
   78 | 						+ "'");
   79 | 			}
   80 | 		}
   81 | 		return uniqueToolName;
   82 | 	}
   83 | 
   84 | 	private record ConnectionId(@Nullable Implementation clientInfo, @Nullable Implementation serverInfo, Tool tool) {
   85 | 	}
   86 | 
   87 | }
```
