<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/McpToolNamePrefixGenerator.java -->
<!-- 正文引用：[[源码:McpToolNamePrefixGenerator.java#L起-L止]] ；代码块头部：(src: McpToolNamePrefixGenerator.java#L起-L止) -->

# McpToolNamePrefixGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/McpToolNamePrefixGenerator.java` |
| 行数 | 51 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.962Z |

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
   19 | import io.modelcontextprotocol.spec.McpSchema.Tool;
   20 | 
   21 | /**
   22 |  * Strategy interface for generating prefixed tool name based on MCP client/server and
   23 |  * Tool information.
   24 |  *
   25 |  * <p>
   26 |  * Implementations of this interface can define custom logic to create meaningful and
   27 |  * unique prefixes for tools, useful for avoiding name collisions in environments where
   28 |  * multiple MCP Servers provide tools.
   29 |  * </p>
   30 |  *
   31 |  * <p>
   32 |  * The prefix generation can take into account various aspects of the MCP client, server
   33 |  * and tool, such as client capabilities, client information, and server initialization
   34 |  * results, as well as specific attributes of the tool itself.
   35 |  * </p>
   36 |  *
   37 |  * @author Christian Tzolov
   38 |  */
   39 | public interface McpToolNamePrefixGenerator {
   40 | 
   41 | 	String prefixedToolName(McpConnectionInfo mcpConnectionInfo, Tool tool);
   42 | 
   43 | 	/**
   44 | 	 * Static factory method to create a no-op prefix generator that returns the tool name
   45 | 	 * @return a prefix generator that returns the tool name as-is
   46 | 	 */
   47 | 	static McpToolNamePrefixGenerator noPrefix() {
   48 | 		return (mcpConnectionInfo, tool) -> tool.name();
   49 | 	}
   50 | 
   51 | }
```
