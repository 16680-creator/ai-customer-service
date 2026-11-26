<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/SyncModernMcpClientScope.java -->
<!-- 正文引用：[[源码:SyncModernMcpClientScope.java#L起-L止]] ；代码块头部：(src: SyncModernMcpClientScope.java#L起-L止) -->

# SyncModernMcpClientScope.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/SyncModernMcpClientScope.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.913Z |

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
   19 | import java.util.function.Supplier;
   20 | 
   21 | import io.modelcontextprotocol.modern.client.McpSyncClient;
   22 | 
   23 | /**
   24 |  * Decides which {@link McpSyncClient} of a connection serves a request, for an
   25 |  * application that wants neither a new client for each request nor one client for all of
   26 |  * them. A typical scope keeps one client for each user, so that the tool listing a server
   27 |  * marks as {@code private} is kept for that user for as long as the server allows:
   28 |  *
   29 |  * <pre class="code">
   30 |  * Map&lt;String, McpSyncClient&gt; clients = new ConcurrentHashMap&lt;&gt;();
   31 |  * SyncModernMcpClientScope perUser = (connection, clientFactory) -&gt; clients
   32 |  *     .computeIfAbsent(connection.name() + "/" + currentUserName(), key -&gt; clientFactory.get());
   33 |  * </pre>
   34 |  *
   35 |  * The scope is called once for each call to
   36 |  * {@link SyncModernMcpToolCallbackProvider#getToolCallbacks()}, on the thread that
   37 |  * resolves the tools, and the returned tool callbacks are bound to the client it returns.
   38 |  * It is not called for what the application does for itself, such as a listen stream: see
   39 |  * {@link SyncModernMcpConnection#applicationClient()}.
   40 |  * <p>
   41 |  * A scope that keeps clients decides how many it keeps and for how long. A client holds
   42 |  * no resource that must be released, so a client that is no longer needed is simply
   43 |  * dropped. A client that outlives a request must not hold a credential that expires: its
   44 |  * {@code transportContextProvider} should read the current credential of its user.
   45 |  *
   46 |  * @author Christian Tzolov
   47 |  * @since 2.1.0
   48 |  * @see SyncModernMcpConnection#scoped
   49 |  */
   50 | @FunctionalInterface
   51 | public interface SyncModernMcpClientScope {
   52 | 
   53 | 	/**
   54 | 	 * Returns the client that serves the current request of the given connection.
   55 | 	 * @param connectionInfo the connection the request goes to
   56 | 	 * @param clientFactory builds a new client for the connection on each call,
   57 | 	 * configured as every client of that connection is
   58 | 	 * @return the client, either one that the scope kept or a new one from the factory
   59 | 	 */
   60 | 	McpSyncClient client(McpModernConnectionInfo connectionInfo, Supplier<McpSyncClient> clientFactory);
   61 | 
   62 | }
```
