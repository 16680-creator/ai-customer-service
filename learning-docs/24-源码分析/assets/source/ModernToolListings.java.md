<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/modern/client/ModernToolListings.java -->
<!-- 正文引用：[[源码:ModernToolListings.java#L起-L止]] ；代码块头部：(src: ModernToolListings.java#L起-L止) -->

# ModernToolListings.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/modern/client/ModernToolListings.java` |
| 行数 | 331 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.823Z |

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
   19 | import java.io.IOException;
   20 | import java.time.Duration;
   21 | import java.util.Collections;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Set;
   25 | import java.util.WeakHashMap;
   26 | import java.util.concurrent.ConcurrentHashMap;
   27 | import java.util.concurrent.TimeUnit;
   28 | import java.util.concurrent.atomic.AtomicLong;
   29 | import java.util.function.LongSupplier;
   30 | 
   31 | import io.modelcontextprotocol.modern.McpSchema.CacheScope;
   32 | import io.modelcontextprotocol.modern.McpSchema.ListToolsResult;
   33 | import io.modelcontextprotocol.modern.McpSchema.Tool;
   34 | import io.modelcontextprotocol.modern.client.McpResponseLostException;
   35 | import io.modelcontextprotocol.modern.client.McpTimeoutException;
   36 | import io.modelcontextprotocol.modern.client.transport.McpHttpStatusException;
   37 | import org.apache.commons.logging.Log;
   38 | import org.jspecify.annotations.Nullable;
   39 | import reactor.core.Exceptions;
   40 | import reactor.core.scheduler.Schedulers;
   41 | 
   42 | import org.springframework.util.Assert;
   43 | 
   44 | /**
   45 |  * The tool listings of the modern connections of a tool callback provider: which listing
   46 |  * may be used again, which must come from the server, and which server is left alone
   47 |  * because it could not be reached. The synchronous and the asynchronous provider share
   48 |  * it, and differ only in how a client lists its tools.
   49 |  *
   50 |  * @param <C> the type of the client that lists the tools
   51 |  * @author Christian Tzolov
   52 |  */
   53 | final class ModernToolListings<C> {
   54 | 
   55 | 	/** The statuses below 500 that also say that a server cannot serve for the moment. */
   56 | 	private static final Set<Integer> UNAVAILABLE_HTTP_STATUSES = Set.of(408, 429);
   57 | 
   58 | 	private final Log logger;
   59 | 
   60 | 	/** The type that selects the clients of a scoped connection, for the hint. */
   61 | 	private final String clientScopeTypeName;
   62 | 
   63 | 	/**
   64 | 	 * The connections whose server was unreachable at the last listing, with the time of
   65 | 	 * that listing. A server is not asked again during the cool-down, and an outage is
   66 | 	 * logged as a warning once and not for every request.
   67 | 	 */
   68 | 	private final Map<Object, Long> unreachableConnections = new ConcurrentHashMap<>();
   69 | 
   70 | 	private final long unreachableCoolDownNanos;
   71 | 
   72 | 	/**
   73 | 	 * The public tool listings that may still be used, by connection.
   74 | 	 */
   75 | 	private final Map<Object, KeptListing> keptListings = new ConcurrentHashMap<>();
   76 | 
   77 | 	/**
   78 | 	 * Counts the dropped listings of each connection, by its name, so that a listing that
   79 | 	 * was under way when the server reported a change is not kept, and so that each
   80 | 	 * client knows whether the listing it keeps itself is still the latest.
   81 | 	 */
   82 | 	private final Map<String, AtomicLong> invalidations = new ConcurrentHashMap<>();
   83 | 
   84 | 	/**
   85 | 	 * The count of dropped listings of its connection at which each client last listed
   86 | 	 * the tools. A client that listed them before the latest drop may answer from a copy
   87 | 	 * that it keeps itself, as a shared client or the client that a scope keeps for a
   88 | 	 * user does, so its next listing must come from the server. A connection can have
   89 | 	 * many clients, and each of them has to find that out for itself. The clients are
   90 | 	 * held weakly: one for a single request is gone with that request.
   91 | 	 */
   92 | 	private final Map<C, Long> listedAtInvalidations = Collections.synchronizedMap(new WeakHashMap<>());
   93 | 
   94 | 	/**
   95 | 	 * The connections that were told that a private listing could be kept with another
   96 | 	 * kind of connection.
   97 | 	 */
   98 | 	private final Set<String> privateListingHints = ConcurrentHashMap.newKeySet();
   99 | 
  100 | 	private final LongSupplier nanoClock;
  101 | 
  102 | 	ModernToolListings(Log logger, String clientScopeTypeName, Duration unreachableCoolDown, LongSupplier nanoClock) {
  103 | 		Assert.notNull(unreachableCoolDown, "unreachableCoolDown must not be null");
  104 | 		Assert.isTrue(!unreachableCoolDown.isNegative(), "unreachableCoolDown must not be negative");
  105 | 		Assert.notNull(nanoClock, "nanoClock must not be null");
  106 | 		this.logger = logger;
  107 | 		this.clientScopeTypeName = clientScopeTypeName;
  108 | 		this.unreachableCoolDownNanos = unreachableCoolDown.toNanos();
  109 | 		this.nanoClock = nanoClock;
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Whether the server of the connection could not be reached a short time ago, in
  114 | 	 * which case it is not asked now.
  115 | 	 * @param connection the connection
  116 | 	 * @return {@code true} if the tools of the connection are to be left out
  117 | 	 */
  118 | 	boolean isCoolingDown(Object connection) {
  119 | 		Long unreachableAtNanos = this.unreachableConnections.get(connection);
  120 | 		return unreachableAtNanos != null
  121 | 				&& this.nanoClock.getAsLong() - unreachableAtNanos < this.unreachableCoolDownNanos;
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * The tools of a connection for the given client, from the kept listing if there is
  126 | 	 * one and from the server otherwise. A server that cannot be reached is remembered,
  127 | 	 * and its tools are left out.
  128 | 	 * @param connection the connection, which is the key of what is kept for it
  129 | 	 * @param connectionInfo describes the connection
  130 | 	 * @param clientPerRequest whether the client serves this request only
  131 | 	 * @param client the client of the request
  132 | 	 * @param lister lists the tools with a client
  133 | 	 * @return the tools, or {@code null} if the server cannot be reached
  134 | 	 * @throws RuntimeException the failure of a listing that does not mean that the
  135 | 	 * server cannot be reached
  136 | 	 */
  137 | 	@Nullable List<Tool> tools(Object connection, McpModernConnectionInfo connectionInfo, boolean clientPerRequest, C client,
  138 | 			Lister<C> lister) {
  139 | 		List<Tool> tools;
  140 | 		try {
  141 | 			tools = this.list(connection, connectionInfo, clientPerRequest, client, lister);
  142 | 		}
  143 | 		catch (RuntimeException ex) {
  144 | 			if (!isServerUnreachable(ex)) {
  145 | 				throw ex;
  146 | 			}
  147 | 			this.serverUnreachable(connection, connectionInfo, ex);
  148 | 			return null;
  149 | 		}
  150 | 		this.serverReachable(connection, connectionInfo);
  151 | 		return tools;
  152 | 	}
  153 | 
  154 | 	private List<Tool> list(Object connection, McpModernConnectionInfo connectionInfo, boolean clientPerRequest,
  155 | 			C client, Lister<C> lister) {
  156 | 		long now = this.nanoClock.getAsLong();
  157 | 		AtomicLong invalidations = this.invalidations.computeIfAbsent(connectionInfo.name(), name -> new AtomicLong());
  158 | 		KeptListing kept = this.keptListings.get(connection);
  159 | 		boolean fromServer = false;
  160 | 		if (kept != null) {
  161 | 			if (kept.isFresh(now)) {
  162 | 				return kept.tools();
  163 | 			}
  164 | 			// A client that keeps this listing as well has a copy that is as old as the
  165 | 			// one that has just expired here. So the expiry counts as a dropped listing,
  166 | 			// which makes every such client ask the server.
  167 | 			if (this.keptListings.remove(connection, kept)) {
  168 | 				invalidations.incrementAndGet();
  169 | 			}
  170 | 			fromServer = true;
  171 | 		}
  172 | 		// The count is read before the listing: a drop during the listing changes it,
  173 | 		// and the listing is then not kept.
  174 | 		long invalidationsBefore = invalidations.get();
  175 | 		// A listing that was dropped must not be answered from the client's own copy.
  176 | 		// A client for one request has no copy and is not remembered. One that is not
  177 | 		// known here may have listed the tools elsewhere, so it asks the server once a
  178 | 		// listing was dropped.
  179 | 		boolean remembered = !clientPerRequest;
  180 | 		Long listedAt = remembered ? this.listedAtInvalidations.get(client) : null;
  181 | 		fromServer = fromServer || (listedAt != null ? listedAt < invalidationsBefore : invalidationsBefore > 0);
  182 | 		boolean nonBlockingThread = Schedulers.isInNonBlockingThread();
  183 | 		if (nonBlockingThread && (fromServer || clientPerRequest)) {
  184 | 			// The listing certainly asks the server and blocks, which Reactor refuses on
  185 | 			// such a thread with a message that does not say what to do. A client that
  186 | 			// outlives the request may answer from the listing it keeps, without
  187 | 			// blocking,
  188 | 			// so it is asked.
  189 | 			throw blockingListingRefused(connectionInfo, null);
  190 | 		}
  191 | 		ListToolsResult listing;
  192 | 		try {
  193 | 			listing = lister.list(client, fromServer);
  194 | 		}
  195 | 		catch (IllegalStateException ex) {
  196 | 			if (nonBlockingThread && isBlockingRefused(ex)) {
  197 | 				throw blockingListingRefused(connectionInfo, ex);
  198 | 			}
  199 | 			throw ex;
  200 | 		}
  201 | 		if (remembered) {
  202 | 			// Not reached if the listing fails: the client then asks the server again.
  203 | 			this.listedAtInvalidations.put(client, invalidationsBefore);
  204 | 		}
  205 | 		if (listing.ttlMs() > 0 && listing.cacheScope() != CacheScope.PUBLIC && clientPerRequest
  206 | 				&& this.privateListingHints.add(connectionInfo.name())) {
  207 | 			// Said once for a connection: the server allows a listing to be kept that
  208 | 			// this kind of connection cannot keep.
  209 | 			logger.info("The MCP server of the connection '" + connectionInfo.name()
  210 | 					+ "' allows its tool listing to be kept for " + listing.ttlMs() + " ms, but marks it as "
  211 | 					+ "private, so only the client that listed it may keep it. The connection creates a client for "
  212 | 					+ "each request, so its tools are listed for each request. If the application calls the server "
  213 | 					+ "under a single identity, use a shared connection (client-scope=SHARED). To keep the listing "
  214 | 					+ "for each user, use a scoped connection (client-scope=CUSTOM with a " + this.clientScopeTypeName
  215 | 					+ ").");
  216 | 		}
  217 | 		// Only a public listing may be handed to other callers. A private one is left to
  218 | 		// the client, which keeps it for as long as that client lives.
  219 | 		if (listing.ttlMs() > 0 && listing.cacheScope() == CacheScope.PUBLIC
  220 | 				&& invalidations.get() == invalidationsBefore) {
  221 | 			KeptListing keptListing = new KeptListing(listing.tools(), now,
  222 | 					TimeUnit.MILLISECONDS.toNanos(listing.ttlMs()));
  223 | 			this.keptListings.put(connection, keptListing);
  224 | 			if (invalidations.get() != invalidationsBefore) {
  225 | 				// The tools changed while the listing was being kept.
  226 | 				this.keptListings.remove(connection, keptListing);
  227 | 			}
  228 | 		}
  229 | 		return listing.tools();
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Drops what is kept of the tools of the connections with the given name. A client
  234 | 	 * that keeps a listing itself asks the server the next time it lists the tools.
  235 | 	 * @param connectionName the name of the connections
  236 | 	 * @param connections the connections with that name
  237 | 	 */
  238 | 	void invalidate(String connectionName, Iterable<?> connections) {
  239 | 		// The count also tells each client that the listing it keeps itself is dropped.
  240 | 		this.invalidations.computeIfAbsent(connectionName, name -> new AtomicLong()).incrementAndGet();
  241 | 		connections.forEach(this.keptListings::remove);
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * Ends the cool-down of the connection, if its server could not be reached before.
  246 | 	 * @param connection the connection
  247 | 	 * @param connectionInfo describes the connection
  248 | 	 */
  249 | 	void serverReachable(Object connection, McpModernConnectionInfo connectionInfo) {
  250 | 		if (this.unreachableConnections.remove(connection) != null) {
  251 | 			logger.info("The MCP server of the connection '" + connectionInfo.name() + "' can be reached again");
  252 | 		}
  253 | 	}
  254 | 
  255 | 	private void serverUnreachable(Object connection, McpModernConnectionInfo connectionInfo, RuntimeException ex) {
  256 | 		String message = "The MCP server of the connection '" + connectionInfo.name()
  257 | 				+ "' cannot be reached; its tools are left out until it can be reached again";
  258 | 		// The time is taken after the failed listing, which may have waited for a
  259 | 		// timeout.
  260 | 		if (this.unreachableConnections.put(connection, this.nanoClock.getAsLong()) == null) {
  261 | 			logger.warn(message, ex);
  262 | 		}
  263 | 		else if (logger.isDebugEnabled()) {
  264 | 			logger.debug(message + ": " + Exceptions.unwrap(ex));
  265 | 		}
  266 | 	}
  267 | 
  268 | 	private static IllegalStateException blockingListingRefused(McpModernConnectionInfo connectionInfo,
  269 | 			@Nullable Throwable cause) {
  270 | 		return new IllegalStateException("The tools of the MCP connection '" + connectionInfo.name()
  271 | 				+ "' are listed with a blocking call, which is not possible on the non-blocking thread '"
  272 | 				+ Thread.currentThread().getName() + "'. Resolve the tools on a thread that may block: start the "
  273 | 				+ "ChatClient call on one, for example with subscribeOn(Schedulers.boundedElastic()).", cause);
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * Whether Reactor refused to block on the current thread.
  278 | 	 */
  279 | 	private static boolean isBlockingRefused(IllegalStateException ex) {
  280 | 		String message = ex.getMessage();
  281 | 		return message != null && message.contains("are blocking, which is not supported in thread");
  282 | 	}
  283 | 
  284 | 	/**
  285 | 	 * Whether the failure of a listing means that the server cannot be reached, as
  286 | 	 * opposed to a server that answered with an error or a caller that is not authorized.
  287 | 	 */
  288 | 	private static boolean isServerUnreachable(RuntimeException ex) {
  289 | 		// A blocking call wraps checked exceptions, such as a refused connection.
  290 | 		Throwable failure = Exceptions.unwrap(ex);
  291 | 		if (failure instanceof McpHttpStatusException statusException) {
  292 | 			// A failure of the server, or one that asks to come back later. A status
  293 | 			// that the caller must act on, such as 401, 403 or 404, is not among them.
  294 | 			int status = statusException.statusCode();
  295 | 			return status >= 500 || UNAVAILABLE_HTTP_STATUSES.contains(status);
  296 | 		}
  297 | 		return failure instanceof IOException || failure instanceof McpTimeoutException
  298 | 				|| failure instanceof McpResponseLostException;
  299 | 	}
  300 | 
  301 | 	/**
  302 | 	 * Lists the tools of a server with a client.
  303 | 	 *
  304 | 	 * @param <C> the type of the client
  305 | 	 */
  306 | 	@FunctionalInterface
  307 | 	interface Lister<C> {
  308 | 
  309 | 		/**
  310 | 		 * Lists the tools.
  311 | 		 * @param client the client to list them with
  312 | 		 * @param fromServer whether the listing must come from the server and not from a
  313 | 		 * copy that the client keeps
  314 | 		 * @return the listing
  315 | 		 */
  316 | 		ListToolsResult list(C client, boolean fromServer);
  317 | 
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * A public tool listing and the time it may be used for.
  322 | 	 */
  323 | 	private record KeptListing(List<Tool> tools, long keptAtNanos, long ttlNanos) {
  324 | 
  325 | 		boolean isFresh(long nowNanos) {
  326 | 			return nowNanos - this.keptAtNanos < this.ttlNanos;
  327 | 		}
  328 | 
  329 | 	}
  330 | 
  331 | }
```
