<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/McpModernConnectionInfo.java -->
<!-- 正文引用：[[源码:McpModernConnectionInfo.java#L起-L止]] ；代码块头部：(src: McpModernConnectionInfo.java#L起-L止) -->

# McpModernConnectionInfo.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/McpModernConnectionInfo.java` |
| 行数 | 43 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.447Z |

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
   19 | import io.modelcontextprotocol.modern.McpSchema.Implementation;
   20 | 
   21 | import org.springframework.util.Assert;
   22 | 
   23 | /**
   24 |  * Describes a connection to a modern MCP server, for {@link McpModernToolFilter} and
   25 |  * {@link McpModernToolNamePrefixGenerator}.
   26 |  * <p>
   27 |  * The modern protocol has no initialization handshake, so the server's info and
   28 |  * capabilities are not part of this record. Use the client's {@code discover()} method
   29 |  * when they are needed.
   30 |  *
   31 |  * @param name the name of the connection, as configured by the application
   32 |  * @param clientInfo the name and version the client sends to the server
   33 |  * @author Christian Tzolov
   34 |  * @since 2.1.0
   35 |  */
   36 | public record McpModernConnectionInfo(String name, Implementation clientInfo) {
   37 | 
   38 | 	public McpModernConnectionInfo {
   39 | 		Assert.hasText(name, "name must not be empty");
   40 | 		Assert.notNull(clientInfo, "clientInfo must not be null");
   41 | 	}
   42 | 
   43 | }
```
