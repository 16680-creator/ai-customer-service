<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/SyncModernMcpConnection.java -->
<!-- 正文引用：[[源码:SyncModernMcpConnection.java#L起-L止]] ；代码块头部：(src: SyncModernMcpConnection.java#L起-L止) -->

# SyncModernMcpConnection.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/SyncModernMcpConnection.java` |
| 行数 | 194 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:23.005Z |

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
   19 | import java.util.Collections;
   20 | import java.util.Set;
   21 | import java.util.WeakHashMap;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import io.modelcontextprotocol.modern.client.McpSyncClient;
   25 | 
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * A connection to one modern MCP server, and the rule that decides which
   30 |  * {@link McpSyncClient} serves a request.
   31 |  * <p>
   32 |  * A modern client is cheap to create and does no I/O when built, so an application may
   33 |  * hold one client for all requests or create a client for each request:
   34 |  * <ul>
   35 |  * <li>{@link #perRequest} returns a new client each time. A client keeps nothing from one
   36 |  * request to the next, so a tool listing the server marks as {@code private} is never
   37 |  * handed to another caller.</li>
   38 |  * <li>{@link #shared} always returns the same client. That client keeps the server's tool
   39 |  * listing for as long as the server allows, even a {@code private} one. It fits an
   40 |  * application that calls the server under a single identity.</li>
   41 |  * <li>{@link #scoped} leaves the choice to a {@link SyncModernMcpClientScope}, for
   42 |  * example one that keeps a client for each user.</li>
   43 |  * </ul>
   44 |  * In all cases {@link SyncModernMcpToolCallbackProvider} keeps a listing the server marks
   45 |  * as {@code public} for as long as the server allows.
   46 |  * <p>
   47 |  * {@link SyncModernMcpToolCallbackProvider} asks for the client once for each call to
   48 |  * {@code getToolCallbacks()} and binds the returned tool callbacks to it. Listing and
   49 |  * calling a tool therefore use the same client.
   50 |  *
   51 |  * @author Christian Tzolov
   52 |  * @since 2.1.0
   53 |  */
   54 | public interface SyncModernMcpConnection {
   55 | 
   56 | 	/**
   57 | 	 * Describes the connection.
   58 | 	 * @return the connection info
   59 | 	 */
   60 | 	McpModernConnectionInfo info();
   61 | 
   62 | 	/**
   63 | 	 * Returns the client that serves the current request.
   64 | 	 * @return the client
   65 | 	 */
   66 | 	McpSyncClient client();
   67 | 
   68 | 	/**
   69 | 	 * Returns a client for what the application does for itself, outside of any request:
   70 | 	 * a listen stream, or listing the tools, prompts or resources after the server
   71 | 	 * reported a change. The caller keeps the client for as long as it needs it. By
   72 | 	 * default this is {@link #client()}.
   73 | 	 * @return the client
   74 | 	 */
   75 | 	default McpSyncClient applicationClient() {
   76 | 		return this.client();
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Whether a client of this connection serves one request only, so that nothing it
   81 | 	 * keeps, such as a tool listing that the server marks as {@code private}, lasts
   82 | 	 * beyond that request. {@link SyncModernMcpToolCallbackProvider} uses the answer to
   83 | 	 * tell when such a listing could be kept with another kind of connection.
   84 | 	 * @return {@code true} if {@link #client()} returns a new client each time,
   85 | 	 * {@code false} by default
   86 | 	 */
   87 | 	default boolean clientPerRequest() {
   88 | 		return false;
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * A connection that serves every request with the given client.
   93 | 	 * @param info the connection info
   94 | 	 * @param client the shared client
   95 | 	 * @return the connection
   96 | 	 */
   97 | 	static SyncModernMcpConnection shared(McpModernConnectionInfo info, McpSyncClient client) {
   98 | 		Assert.notNull(client, "client must not be null");
   99 | 		return connection(info, () -> client, () -> client, false);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * A connection that asks the given factory for a client each time {@link #client()}
  104 | 	 * is called, to give each request its own client. A factory is expected to build a
  105 | 	 * new client on each call, and the connection {@link #clientPerRequest() says so}. To
  106 | 	 * keep clients, for example one for each user, use {@link #scoped}.
  107 | 	 * @param info the connection info
  108 | 	 * @param clientFactory creates the client for a request
  109 | 	 * @return the connection
  110 | 	 */
  111 | 	static SyncModernMcpConnection perRequest(McpModernConnectionInfo info, Supplier<McpSyncClient> clientFactory) {
  112 | 		Supplier<McpSyncClient> newClient = notNull(clientFactory);
  113 | 		return connection(info, newClient, newClient, true);
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * A connection that asks the given scope for the client of each request. The scope
  118 | 	 * gets the factory to build a client with, and it may keep the clients it builds, for
  119 | 	 * example one for each user. A scope may serve several connections, and it must
  120 | 	 * return a client that the factory of this connection built: a client of another
  121 | 	 * connection, which would send the requests to the server of that connection, is
  122 | 	 * rejected. {@link #applicationClient()} does not ask the scope: it returns a new
  123 | 	 * client from the factory, so that the scope is never asked for a client when there
  124 | 	 * is no request.
  125 | 	 * @param info the connection info
  126 | 	 * @param clientFactory builds a new client for the connection on each call
  127 | 	 * @param clientScope selects the client for a request
  128 | 	 * @return the connection
  129 | 	 */
  130 | 	static SyncModernMcpConnection scoped(McpModernConnectionInfo info, Supplier<McpSyncClient> clientFactory,
  131 | 			SyncModernMcpClientScope clientScope) {
  132 | 		Assert.notNull(info, "info must not be null");
  133 | 		Assert.notNull(clientScope, "clientScope must not be null");
  134 | 		Supplier<McpSyncClient> factory = notNull(clientFactory);
  135 | 		// The clients of this connection, held weakly: a scope decides how long it keeps
  136 | 		// a client.
  137 | 		Set<McpSyncClient> built = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
  138 | 		Supplier<McpSyncClient> newClient = () -> {
  139 | 			McpSyncClient client = factory.get();
  140 | 			built.add(client);
  141 | 			return client;
  142 | 		};
  143 | 		return connection(info, () -> {
  144 | 			McpSyncClient client = clientScope.client(info, newClient);
  145 | 			Assert.state(client != null, "clientScope must not return null");
  146 | 			Assert.state(built.contains(client),
  147 | 					() -> "The SyncModernMcpClientScope returned a client that was not built for the MCP connection '"
  148 | 							+ info.name() + "'. A scope that keeps clients must keep them for each connection, for "
  149 | 							+ "example under a key that contains the name of the connection.");
  150 | 			return client;
  151 | 		}, newClient, false);
  152 | 	}
  153 | 
  154 | 	private static SyncModernMcpConnection connection(McpModernConnectionInfo info, Supplier<McpSyncClient> client,
  155 | 			Supplier<McpSyncClient> applicationClient, boolean clientPerRequest) {
  156 | 		Assert.notNull(info, "info must not be null");
  157 | 		return new SyncModernMcpConnection() {
  158 | 
  159 | 			@Override
  160 | 			public McpModernConnectionInfo info() {
  161 | 				return info;
  162 | 			}
  163 | 
  164 | 			@Override
  165 | 			public McpSyncClient client() {
  166 | 				return client.get();
  167 | 			}
  168 | 
  169 | 			@Override
  170 | 			public McpSyncClient applicationClient() {
  171 | 				return applicationClient.get();
  172 | 			}
  173 | 
  174 | 			@Override
  175 | 			public boolean clientPerRequest() {
  176 | 				return clientPerRequest;
  177 | 			}
  178 | 
  179 | 		};
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * A factory that fails if the given one returns no client.
  184 | 	 */
  185 | 	private static Supplier<McpSyncClient> notNull(Supplier<McpSyncClient> clientFactory) {
  186 | 		Assert.notNull(clientFactory, "clientFactory must not be null");
  187 | 		return () -> {
  188 | 			McpSyncClient client = clientFactory.get();
  189 | 			Assert.state(client != null, "clientFactory must not return null");
  190 | 			return client;
  191 | 		};
  192 | 	}
  193 | 
  194 | }
```
