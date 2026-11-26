<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/NettyRoutingFilter.java -->
<!-- 正文引用：[[源码:NettyRoutingFilter.java#L起-L止]] ；代码块头部：(src: NettyRoutingFilter.java#L起-L止) -->

# NettyRoutingFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/NettyRoutingFilter.java` |
| 行数 | 319 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:46.579Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.gateway.filter;
   18 | 
   19 | import java.net.URI;
   20 | import java.time.Duration;
   21 | import java.util.List;
   22 | import java.util.Objects;
   23 | 
   24 | import io.netty.buffer.ByteBuf;
   25 | import io.netty.buffer.Unpooled;
   26 | import io.netty.channel.ChannelOption;
   27 | import io.netty.handler.codec.http.DefaultHttpHeaders;
   28 | import io.netty.handler.codec.http.HttpMethod;
   29 | import org.apache.commons.logging.Log;
   30 | import org.apache.commons.logging.LogFactory;
   31 | import org.jspecify.annotations.Nullable;
   32 | import reactor.core.publisher.Flux;
   33 | import reactor.core.publisher.Mono;
   34 | import reactor.netty.Connection;
   35 | import reactor.netty.http.client.HttpClient;
   36 | import reactor.netty.http.client.HttpClientResponse;
   37 | 
   38 | import org.springframework.beans.factory.ObjectProvider;
   39 | import org.springframework.cloud.gateway.config.HttpClientProperties;
   40 | import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
   41 | import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter.Type;
   42 | import org.springframework.cloud.gateway.route.Route;
   43 | import org.springframework.cloud.gateway.support.TimeoutException;
   44 | import org.springframework.core.Ordered;
   45 | import org.springframework.core.io.buffer.DataBuffer;
   46 | import org.springframework.core.io.buffer.DefaultDataBuffer;
   47 | import org.springframework.core.io.buffer.NettyDataBuffer;
   48 | import org.springframework.http.HttpHeaders;
   49 | import org.springframework.http.HttpStatus;
   50 | import org.springframework.http.server.reactive.AbstractServerHttpResponse;
   51 | import org.springframework.http.server.reactive.ServerHttpRequest;
   52 | import org.springframework.http.server.reactive.ServerHttpResponse;
   53 | import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
   54 | import org.springframework.util.StringUtils;
   55 | import org.springframework.web.server.ResponseStatusException;
   56 | import org.springframework.web.server.ServerWebExchange;
   57 | 
   58 | import static org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter.filterRequest;
   59 | import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
   60 | import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;
   61 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
   62 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;
   63 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_HEADER_NAMES;
   64 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   65 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
   66 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;
   67 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE;
   68 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;
   69 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;
   70 | 
   71 | /**
   72 |  * @author Spencer Gibb
   73 |  * @author Biju Kunjummen
   74 |  * @author Seungbin Ko
   75 |  */
   76 | public class NettyRoutingFilter implements GlobalFilter, Ordered {
   77 | 
   78 | 	/**
   79 | 	 * The order of the NettyRoutingFilter. See {@link Ordered#LOWEST_PRECEDENCE}.
   80 | 	 */
   81 | 	public static final int ORDER = Ordered.LOWEST_PRECEDENCE;
   82 | 
   83 | 	private static final Log log = LogFactory.getLog(NettyRoutingFilter.class);
   84 | 
   85 | 	private final HttpClient httpClient;
   86 | 
   87 | 	private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;
   88 | 
   89 | 	private final HttpClientProperties properties;
   90 | 
   91 | 	// do not use this headersFilters directly, use getHeadersFilters() instead.
   92 | 	private volatile @Nullable List<HttpHeadersFilter> headersFilters;
   93 | 
   94 | 	public NettyRoutingFilter(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
   95 | 			HttpClientProperties properties) {
   96 | 		this.httpClient = httpClient;
   97 | 		this.headersFiltersProvider = headersFiltersProvider;
   98 | 		this.properties = properties;
   99 | 	}
  100 | 
  101 | 	public @Nullable List<HttpHeadersFilter> getHeadersFilters() {
  102 | 		if (headersFilters == null) {
  103 | 			headersFilters = headersFiltersProvider.getIfAvailable();
  104 | 		}
  105 | 		return headersFilters;
  106 | 	}
  107 | 
  108 | 	@Override
  109 | 	public int getOrder() {
  110 | 		return ORDER;
  111 | 	}
  112 | 
  113 | 	@Override
  114 | 	@SuppressWarnings("Duplicates")
  115 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  116 | 		URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
  117 | 
  118 | 		String scheme = requestUrl.getScheme();
  119 | 		if (isAlreadyRouted(exchange) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
  120 | 			return chain.filter(exchange);
  121 | 		}
  122 | 		setAlreadyRouted(exchange);
  123 | 
  124 | 		ServerHttpRequest request = exchange.getRequest();
  125 | 
  126 | 		final HttpMethod method = HttpMethod.valueOf(request.getMethod().name());
  127 | 		final String url = requestUrl.toASCIIString();
  128 | 
  129 | 		HttpHeaders filtered = filterRequest(getHeadersFilters(), exchange);
  130 | 
  131 | 		final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
  132 | 		filtered.forEach(httpHeaders::set);
  133 | 
  134 | 		boolean preserveHost = exchange.getAttributeOrDefault(PRESERVE_HOST_HEADER_ATTRIBUTE, false);
  135 | 		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
  136 | 		Objects.requireNonNull(route, "route must not be null");
  137 | 		Flux<HttpClientResponse> responseFlux = getHttpClientMono(route, exchange)
  138 | 			.flatMapMany(httpClient -> httpClient.headers(headers -> {
  139 | 				headers.add(httpHeaders);
  140 | 				// Will either be set below, or later by Netty
  141 | 				headers.remove(HttpHeaders.HOST);
  142 | 				if (preserveHost) {
  143 | 					String host = request.getHeaders().getFirst(HttpHeaders.HOST);
  144 | 					headers.add(HttpHeaders.HOST, host);
  145 | 				}
  146 | 			}).request(method).uri(url).send((req, nettyOutbound) -> {
  147 | 				if (log.isTraceEnabled()) {
  148 | 					nettyOutbound.withConnection(connection -> log.trace("outbound route: "
  149 | 							+ connection.channel().id().asShortText() + ", inbound: " + exchange.getLogPrefix()));
  150 | 				}
  151 | 				return nettyOutbound.send(request.getBody().map(this::getByteBuf));
  152 | 			}).responseConnection((res, connection) -> {
  153 | 
  154 | 				// Defer committing the response until all route filters have run
  155 | 				// Put client response as ServerWebExchange attribute and write
  156 | 				// response later NettyWriteResponseFilter
  157 | 				exchange.getAttributes().put(CLIENT_RESPONSE_ATTR, res);
  158 | 				exchange.getAttributes().put(CLIENT_RESPONSE_CONN_ATTR, connection);
  159 | 
  160 | 				ServerHttpResponse response = exchange.getResponse();
  161 | 				// put headers and status so filters can modify the response
  162 | 				HttpHeaders headers = new HttpHeaders();
  163 | 
  164 | 				res.responseHeaders().forEach(entry -> headers.add(entry.getKey(), entry.getValue()));
  165 | 
  166 | 				String contentTypeValue = headers.getFirst(HttpHeaders.CONTENT_TYPE);
  167 | 				if (StringUtils.hasLength(contentTypeValue)) {
  168 | 					exchange.getAttributes().put(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR, contentTypeValue);
  169 | 				}
  170 | 
  171 | 				setResponseStatus(res, response);
  172 | 
  173 | 				// make sure headers filters run after setting status so it is
  174 | 				// available in response
  175 | 				HttpHeaders filteredResponseHeaders = HttpHeadersFilter.filter(getHeadersFilters(), headers, exchange,
  176 | 						Type.RESPONSE);
  177 | 
  178 | 				if (!filteredResponseHeaders.containsHeader(HttpHeaders.TRANSFER_ENCODING)
  179 | 						&& filteredResponseHeaders.containsHeader(HttpHeaders.CONTENT_LENGTH)) {
  180 | 					// It is not valid to have both the transfer-encoding header and
  181 | 					// the content-length header.
  182 | 					// Remove the transfer-encoding header in the response if the
  183 | 					// content-length header is present.
  184 | 					response.getHeaders().remove(HttpHeaders.TRANSFER_ENCODING);
  185 | 				}
  186 | 
  187 | 				exchange.getAttributes().put(CLIENT_RESPONSE_HEADER_NAMES, filteredResponseHeaders.headerNames());
  188 | 
  189 | 				response.getHeaders().addAll(filteredResponseHeaders);
  190 | 
  191 | 				return Mono.just(res);
  192 | 			})
  193 | 				// A cancelled exchange discards this response before anything subscribes
  194 | 				// to it, and the cleanup in NettyWriteResponseFilter has already run
  195 | 				// without finding a connection to dispose. Dispose it here instead.
  196 | 				//
  197 | 				// Must stay on the inner publisher: doOnDiscard writes a context entry
  198 | 				// that only upstream operators see. Downstream of the then() below it
  199 | 				// would also fire for successful exchanges, closing live connections.
  200 | 				.doOnDiscard(Connection.class, Connection::dispose));
  201 | 
  202 | 		Duration responseTimeout = getResponseTimeout(route);
  203 | 		if (responseTimeout != null) {
  204 | 			responseFlux = responseFlux
  205 | 				.timeout(responseTimeout,
  206 | 						Mono.defer(() -> Mono
  207 | 							.error(new TimeoutException("Response took longer than timeout: " + responseTimeout))))
  208 | 				.onErrorMap(TimeoutException.class,
  209 | 						th -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, th.getMessage(), th));
  210 | 		}
  211 | 
  212 | 		return responseFlux.then(chain.filter(exchange));
  213 | 	}
  214 | 
  215 | 	protected ByteBuf getByteBuf(DataBuffer dataBuffer) {
  216 | 		if (dataBuffer instanceof NettyDataBuffer) {
  217 | 			NettyDataBuffer buffer = (NettyDataBuffer) dataBuffer;
  218 | 			return buffer.getNativeBuffer();
  219 | 		}
  220 | 		// MockServerHttpResponse creates these
  221 | 		else if (dataBuffer instanceof DefaultDataBuffer) {
  222 | 			DefaultDataBuffer buffer = (DefaultDataBuffer) dataBuffer;
  223 | 			return Unpooled.wrappedBuffer(buffer.getNativeBuffer());
  224 | 		}
  225 | 		throw new IllegalArgumentException("Unable to handle DataBuffer of type " + dataBuffer.getClass());
  226 | 	}
  227 | 
  228 | 	private void setResponseStatus(HttpClientResponse clientResponse, ServerHttpResponse response) {
  229 | 		HttpStatus status = HttpStatus.resolve(clientResponse.status().code());
  230 | 		if (status != null) {
  231 | 			response.setStatusCode(status);
  232 | 		}
  233 | 		else {
  234 | 			while (response instanceof ServerHttpResponseDecorator) {
  235 | 				response = ((ServerHttpResponseDecorator) response).getDelegate();
  236 | 			}
  237 | 			if (response instanceof AbstractServerHttpResponse) {
  238 | 				((AbstractServerHttpResponse) response).setRawStatusCode(clientResponse.status().code());
  239 | 			}
  240 | 			else {
  241 | 				// TODO: log warning here, not throw error?
  242 | 				throw new IllegalStateException("Unable to set status code " + clientResponse.status().code()
  243 | 						+ " on response of type " + response.getClass().getName());
  244 | 			}
  245 | 		}
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Creates a new HttpClient with per route timeout configuration. Sub-classes that
  250 | 	 * override, should call super.httpClient() if they want to honor the per route
  251 | 	 * timeout configuration.
  252 | 	 * @param route the current route.
  253 | 	 * @param exchange the current ServerWebExchange.
  254 | 	 * @return the configured HttpClient.
  255 | 	 */
  256 | 	protected Mono<HttpClient> getHttpClientMono(Route route, ServerWebExchange exchange) {
  257 | 		return Mono.just(getHttpClient(route, exchange));
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * Creates a new HttpClient with per route timeout configuration. Sub-classes that
  262 | 	 * override, should call super.getHttpClient() if they want to honor the per route
  263 | 	 * timeout configuration.
  264 | 	 * @param route the current route.
  265 | 	 * @param exchange the current ServerWebExchange.
  266 | 	 * @return the configured HttpClient.
  267 | 	 */
  268 | 	protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
  269 | 		Object connectTimeoutAttr = route.getMetadata().get(CONNECT_TIMEOUT_ATTR) != null
  270 | 				? route.getMetadata().get(CONNECT_TIMEOUT_ATTR) : properties.getConnectTimeout();
  271 | 		if (connectTimeoutAttr != null) {
  272 | 			Integer connectTimeout = java.util.Objects.requireNonNull(getInteger(connectTimeoutAttr),
  273 | 					"connectTimeout must not be null");
  274 | 			return this.httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
  275 | 		}
  276 | 		return httpClient;
  277 | 	}
  278 | 
  279 | 	static @Nullable Integer getInteger(Object connectTimeoutAttr) {
  280 | 		Integer connectTimeout;
  281 | 		if (connectTimeoutAttr instanceof Integer) {
  282 | 			connectTimeout = (Integer) connectTimeoutAttr;
  283 | 		}
  284 | 		else {
  285 | 			connectTimeout = Integer.parseInt(connectTimeoutAttr.toString());
  286 | 		}
  287 | 		return connectTimeout;
  288 | 	}
  289 | 
  290 | 	private @Nullable Duration getResponseTimeout(Route route) {
  291 | 		try {
  292 | 			if (route.getMetadata().containsKey(RESPONSE_TIMEOUT_ATTR)) {
  293 | 				Long routeResponseTimeout = getLong(route.getMetadata().get(RESPONSE_TIMEOUT_ATTR));
  294 | 				if (routeResponseTimeout != null && routeResponseTimeout >= 0) {
  295 | 					return Duration.ofMillis(routeResponseTimeout);
  296 | 				}
  297 | 				else {
  298 | 					return null;
  299 | 				}
  300 | 			}
  301 | 		}
  302 | 		catch (NumberFormatException e) {
  303 | 			// ignore number format and use global default
  304 | 		}
  305 | 		return properties.getResponseTimeout();
  306 | 	}
  307 | 
  308 | 	static @Nullable Long getLong(Object responseTimeoutAttr) {
  309 | 		Long responseTimeout = null;
  310 | 		if (responseTimeoutAttr instanceof Number) {
  311 | 			responseTimeout = ((Number) responseTimeoutAttr).longValue();
  312 | 		}
  313 | 		else if (responseTimeoutAttr != null) {
  314 | 			responseTimeout = Long.parseLong(responseTimeoutAttr.toString());
  315 | 		}
  316 | 		return responseTimeout;
  317 | 	}
  318 | 
  319 | }
```
