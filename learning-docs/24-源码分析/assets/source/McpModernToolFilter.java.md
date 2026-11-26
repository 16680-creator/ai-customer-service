<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/McpModernToolFilter.java -->
<!-- 正文引用：[[源码:McpModernToolFilter.java#L起-L止]] ；代码块头部：(src: McpModernToolFilter.java#L起-L止) -->

# McpModernToolFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/McpModernToolFilter.java` |
| 行数 | 42 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.532Z |

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
   19 | import java.util.function.BiPredicate;
   20 | 
   21 | import io.modelcontextprotocol.modern.McpSchema.Tool;
   22 | 
   23 | /**
   24 |  * Decides which tools of a modern MCP server {@link SyncModernMcpToolCallbackProvider}
   25 |  * and {@link AsyncModernMcpToolCallbackProvider} expose. A tool is exposed when the
   26 |  * predicate returns {@code true} for the connection and the tool.
   27 |  *
   28 |  * @author Christian Tzolov
   29 |  * @since 2.1.0
   30 |  */
   31 | @FunctionalInterface
   32 | public interface McpModernToolFilter extends BiPredicate<McpModernConnectionInfo, Tool> {
   33 | 
   34 | 	/**
   35 | 	 * A filter that exposes every tool.
   36 | 	 * @return the filter
   37 | 	 */
   38 | 	static McpModernToolFilter acceptAll() {
   39 | 		return (connectionInfo, tool) -> true;
   40 | 	}
   41 | 
   42 | }
```
