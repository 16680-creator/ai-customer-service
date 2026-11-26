<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/support/ServerWebExchangeUtils.java -->
<!-- 正文引用：[[源码:ServerWebExchangeUtils.java#L起-L止]] ；代码块头部：(src: ServerWebExchangeUtils.java#L起-L止) -->

# ServerWebExchangeUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/support/ServerWebExchangeUtils.java` |
| 行数 | 496 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:50.773Z |

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
   17 | package org.springframework.cloud.gateway.support;
   18 | 
   19 | import java.net.URI;
   20 | import java.nio.charset.StandardCharsets;
   21 | import java.util.Collections;
   22 | import java.util.HashMap;
   23 | import java.util.LinkedHashSet;
   24 | import java.util.List;
   25 | import java.util.Locale;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.Set;
   29 | import java.util.function.Function;
   30 | import java.util.function.Predicate;
   31 | 
   32 | import io.netty.buffer.Unpooled;
   33 | import org.apache.commons.logging.Log;
   34 | import org.apache.commons.logging.LogFactory;
   35 | import reactor.core.publisher.Flux;
   36 | import reactor.core.publisher.Mono;
   37 | import reactor.netty.Connection;
   38 | 
   39 | import org.springframework.cloud.client.loadbalancer.Response;
   40 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   41 | import org.springframework.cloud.gateway.handler.AsyncPredicate;
   42 | import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
   43 | import org.springframework.core.io.buffer.DataBuffer;
   44 | import org.springframework.core.io.buffer.DataBufferFactory;
   45 | import org.springframework.core.io.buffer.DataBufferUtils;
   46 | import org.springframework.core.io.buffer.DefaultDataBuffer;
   47 | import org.springframework.core.io.buffer.NettyDataBuffer;
   48 | import org.springframework.core.io.buffer.PooledDataBuffer;
   49 | import org.springframework.http.HttpStatus;
   50 | import org.springframework.http.server.reactive.AbstractServerHttpResponse;
   51 | import org.springframework.http.server.reactive.ServerHttpRequest;
   52 | import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
   53 | import org.springframework.http.server.reactive.ServerHttpResponse;
   54 | import org.springframework.util.CollectionUtils;
   55 | import org.springframework.util.LinkedMultiValueMap;
   56 | import org.springframework.util.MultiValueMap;
   57 | import org.springframework.web.reactive.DispatcherHandler;
   58 | import org.springframework.web.server.ServerWebExchange;
   59 | import org.springframework.web.util.UriComponentsBuilder;
   60 | import org.springframework.web.util.UriUtils;
   61 | 
   62 | /**
   63 |  * @author Spencer Gibb
   64 |  */
   65 | public final class ServerWebExchangeUtils {
   66 | 
   67 | 	private static final Log log = LogFactory.getLog(ServerWebExchangeUtils.class);
   68 | 
   69 | 	/**
   70 | 	 * Preserve-Host header attribute name.
   71 | 	 */
   72 | 	public static final String PRESERVE_HOST_HEADER_ATTRIBUTE = qualify("preserveHostHeader");
   73 | 
   74 | 	/**
   75 | 	 * URI template variables attribute name.
   76 | 	 */
   77 | 	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = qualify("uriTemplateVariables");
   78 | 
   79 | 	/**
   80 | 	 * Client response attribute name.
   81 | 	 */
   82 | 	public static final String CLIENT_RESPONSE_ATTR = qualify("gatewayClientResponse");
   83 | 
   84 | 	/**
   85 | 	 * Client response connection attribute name.
   86 | 	 */
   87 | 	public static final String CLIENT_RESPONSE_CONN_ATTR = qualify("gatewayClientResponseConnection");
   88 | 
   89 | 	/**
   90 | 	 * Client response header names attribute name.
   91 | 	 */
   92 | 	public static final String CLIENT_RESPONSE_HEADER_NAMES = qualify("gatewayClientResponseHeaderNames");
   93 | 
   94 | 	/**
   95 | 	 * Gateway route attribute name.
   96 | 	 */
   97 | 	public static final String GATEWAY_ROUTE_ATTR = qualify("gatewayRoute");
   98 | 
   99 | 	/**
  100 | 	 * Original Reactor Context corresponding to the processed request.
  101 | 	 */
  102 | 	public static final String GATEWAY_REACTOR_CONTEXT_ATTR = qualify("gatewayReactorContext");
  103 | 
  104 | 	/**
  105 | 	 * Gateway request URL attribute name.
  106 | 	 */
  107 | 	public static final String GATEWAY_REQUEST_URL_ATTR = qualify("gatewayRequestUrl");
  108 | 
  109 | 	/**
  110 | 	 * Gateway original request URL attribute name.
  111 | 	 */
  112 | 	public static final String GATEWAY_ORIGINAL_REQUEST_URL_ATTR = qualify("gatewayOriginalRequestUrl");
  113 | 
  114 | 	/**
  115 | 	 * Gateway handler mapper attribute name.
  116 | 	 */
  117 | 	public static final String GATEWAY_HANDLER_MAPPER_ATTR = qualify("gatewayHandlerMapper");
  118 | 
  119 | 	/**
  120 | 	 * Gateway scheme prefix attribute name.
  121 | 	 */
  122 | 	public static final String GATEWAY_SCHEME_PREFIX_ATTR = qualify("gatewaySchemePrefix");
  123 | 
  124 | 	/**
  125 | 	 * Gateway predicate route attribute name.
  126 | 	 */
  127 | 	public static final String GATEWAY_PREDICATE_ROUTE_ATTR = qualify("gatewayPredicateRouteAttr");
  128 | 
  129 | 	/**
  130 | 	 * Gateway predicate matched path attribute name.
  131 | 	 */
  132 | 	public static final String GATEWAY_PREDICATE_MATCHED_PATH_ATTR = qualify("gatewayPredicateMatchedPathAttr");
  133 | 
  134 | 	/**
  135 | 	 * Gateway predicate matched path route id attribute name.
  136 | 	 */
  137 | 	public static final String GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR = qualify(
  138 | 			"gatewayPredicateMatchedPathRouteIdAttr");
  139 | 
  140 | 	/**
  141 | 	 * Gateway predicate path container attribute name.
  142 | 	 */
  143 | 	public static final String GATEWAY_PREDICATE_PATH_CONTAINER_ATTR = qualify("gatewayPredicatePathContainer");
  144 | 
  145 | 	/**
  146 | 	 * Weight attribute name.
  147 | 	 */
  148 | 	public static final String WEIGHT_ATTR = qualify("routeWeight");
  149 | 
  150 | 	/**
  151 | 	 * Original response Content-Type attribute name.
  152 | 	 */
  153 | 	public static final String ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR = "original_response_content_type";
  154 | 
  155 | 	/**
  156 | 	 * CircuitBreaker execution exception attribute name.
  157 | 	 */
  158 | 	public static final String CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR = qualify("circuitBreakerExecutionException");
  159 | 
  160 | 	/**
  161 | 	 * Used when a routing filter has been successfully called. Allows users to write
  162 | 	 * custom routing filters that disable built in routing filters.
  163 | 	 */
  164 | 	public static final String GATEWAY_ALREADY_ROUTED_ATTR = qualify("gatewayAlreadyRouted");
  165 | 
  166 | 	/**
  167 | 	 * Gateway already prefixed attribute name.
  168 | 	 */
  169 | 	public static final String GATEWAY_ALREADY_PREFIXED_ATTR = qualify("gatewayAlreadyPrefixed");
  170 | 
  171 | 	/**
  172 | 	 * Cached ServerHttpRequestDecorator attribute name. Used when
  173 | 	 * {@link #cacheRequestBodyAndRequest(ServerWebExchange, Function)} is called.
  174 | 	 */
  175 | 	public static final String CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR = "cachedServerHttpRequestDecorator";
  176 | 
  177 | 	/**
  178 | 	 * Cached request body key. Used when
  179 | 	 * {@link #cacheRequestBodyAndRequest(ServerWebExchange, Function)} or
  180 | 	 * {@link #cacheRequestBody(ServerWebExchange, Function)} are called.
  181 | 	 */
  182 | 	public static final String CACHED_REQUEST_BODY_ATTR = "cachedRequestBody";
  183 | 
  184 | 	/**
  185 | 	 * Gateway LoadBalancer {@link Response} attribute name.
  186 | 	 */
  187 | 	public static final String GATEWAY_LOADBALANCER_RESPONSE_ATTR = qualify("gatewayLoadBalancerResponse");
  188 | 
  189 | 	/**
  190 | 	 * Gateway Client {@code Observation} attribute name.
  191 | 	 */
  192 | 	public static final String GATEWAY_OBSERVATION_ATTR = qualify("gateway.observation");
  193 | 
  194 | 	private static final byte[] EMPTY_BYTES = {};
  195 | 
  196 | 	private ServerWebExchangeUtils() {
  197 | 		throw new AssertionError("Must not instantiate utility class.");
  198 | 	}
  199 | 
  200 | 	private static String qualify(String attr) {
  201 | 		return ServerWebExchangeUtils.class.getName() + "." + attr;
  202 | 	}
  203 | 
  204 | 	public static void setAlreadyRouted(ServerWebExchange exchange) {
  205 | 		exchange.getAttributes().put(GATEWAY_ALREADY_ROUTED_ATTR, true);
  206 | 	}
  207 | 
  208 | 	public static void removeAlreadyRouted(ServerWebExchange exchange) {
  209 | 		exchange.getAttributes().remove(GATEWAY_ALREADY_ROUTED_ATTR);
  210 | 	}
  211 | 
  212 | 	public static boolean isAlreadyRouted(ServerWebExchange exchange) {
  213 | 		return exchange.getAttributeOrDefault(GATEWAY_ALREADY_ROUTED_ATTR, false);
  214 | 	}
  215 | 
  216 | 	public static boolean setResponseStatus(ServerWebExchange exchange, HttpStatus httpStatus) {
  217 | 		boolean response = exchange.getResponse().setStatusCode(httpStatus);
  218 | 		if (!response && log.isWarnEnabled()) {
  219 | 			log.warn("Unable to set status code to " + httpStatus + ". Response already committed.");
  220 | 		}
  221 | 		return response;
  222 | 	}
  223 | 
  224 | 	public static void reset(ServerWebExchange exchange) {
  225 | 		// TODO: what else to do to reset exchange?
  226 | 		// Dispose and clean up client connection to prevent connection leaks
  227 | 		Connection conn = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
  228 | 		if (conn != null) {
  229 | 			conn.dispose();
  230 | 			exchange.getAttributes().remove(CLIENT_RESPONSE_CONN_ATTR);
  231 | 		}
  232 | 
  233 | 		// Clean up client response
  234 | 		exchange.getAttributes().remove(CLIENT_RESPONSE_ATTR);
  235 | 
  236 | 		// Remove added response headers
  237 | 		Set<String> addedHeaders = exchange.getAttributeOrDefault(CLIENT_RESPONSE_HEADER_NAMES, Collections.emptySet());
  238 | 		addedHeaders.forEach(header -> exchange.getResponse().getHeaders().remove(header));
  239 | 		removeAlreadyRouted(exchange);
  240 | 	}
  241 | 
  242 | 	public static boolean setResponseStatus(ServerWebExchange exchange, HttpStatusHolder statusHolder) {
  243 | 		if (exchange.getResponse().isCommitted()) {
  244 | 			return false;
  245 | 		}
  246 | 		if (log.isDebugEnabled()) {
  247 | 			log.debug("Setting response status to " + statusHolder);
  248 | 		}
  249 | 		if (statusHolder.getHttpStatus() != null) {
  250 | 			return setResponseStatus(exchange, statusHolder.getHttpStatus());
  251 | 		}
  252 | 		if (statusHolder.getStatus() != null && exchange.getResponse() instanceof AbstractServerHttpResponse) { // non-standard
  253 | 			((AbstractServerHttpResponse) exchange.getResponse()).setRawStatusCode(statusHolder.getStatus());
  254 | 			return true;
  255 | 		}
  256 | 		return false;
  257 | 	}
  258 | 
  259 | 	public static boolean containsEncodedParts(URI uri) {
  260 | 		boolean encoded = (uri.getRawQuery() != null && uri.getRawQuery().contains("%"))
  261 | 				|| (uri.getRawPath() != null && uri.getRawPath().contains("%"));
  262 | 
  263 | 		// Verify if it is really fully encoded. Treat partial encoded as unencoded.
  264 | 		if (encoded) {
  265 | 			try {
  266 | 				UriComponentsBuilder.fromUri(uri).build(true);
  267 | 				return true;
  268 | 			}
  269 | 			catch (IllegalArgumentException ignored) {
  270 | 				if (log.isTraceEnabled()) {
  271 | 					log.trace("Error in containsEncodedParts", ignored);
  272 | 				}
  273 | 			}
  274 | 
  275 | 			return false;
  276 | 		}
  277 | 
  278 | 		return encoded;
  279 | 	}
  280 | 
  281 | 	public static MultiValueMap<String, String> encodeQueryParams(MultiValueMap<String, String> params) {
  282 | 		MultiValueMap<String, String> encodedQueryParams = new LinkedMultiValueMap<>(params.size());
  283 | 		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
  284 | 			for (String value : entry.getValue()) {
  285 | 				encodedQueryParams.add(UriUtils.encode(entry.getKey(), StandardCharsets.UTF_8),
  286 | 						UriUtils.encode(value, StandardCharsets.UTF_8));
  287 | 			}
  288 | 		}
  289 | 		return CollectionUtils.unmodifiableMultiValueMap(encodedQueryParams);
  290 | 	}
  291 | 
  292 | 	public static HttpStatus parse(String statusString) {
  293 | 		HttpStatus httpStatus;
  294 | 
  295 | 		try {
  296 | 			int status = Integer.parseInt(statusString);
  297 | 			httpStatus = HttpStatus.resolve(status);
  298 | 		}
  299 | 		catch (NumberFormatException e) {
  300 | 			// try the enum string
  301 | 			httpStatus = HttpStatus.valueOf(statusString.toUpperCase(Locale.ROOT));
  302 | 		}
  303 | 		return httpStatus;
  304 | 	}
  305 | 
  306 | 	public static void addOriginalRequestUrl(ServerWebExchange exchange, URI url) {
  307 | 		exchange.getAttributes().computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, s -> new LinkedHashSet<>());
  308 | 		LinkedHashSet<URI> uris = exchange.getRequiredAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
  309 | 		uris.add(url);
  310 | 	}
  311 | 
  312 | 	public static AsyncPredicate<ServerWebExchange> toAsyncPredicate(Predicate<? super ServerWebExchange> predicate) {
  313 | 		Objects.requireNonNull(predicate, "predicate must not be null");
  314 | 		return AsyncPredicate.from(predicate);
  315 | 	}
  316 | 
  317 | 	public static String expand(ServerWebExchange exchange, String template) {
  318 | 		Objects.requireNonNull(exchange, "exchange may not be null");
  319 | 		Objects.requireNonNull(template, "template may not be null");
  320 | 
  321 | 		if (template.indexOf('{') == -1) { // short circuit
  322 | 			return template;
  323 | 		}
  324 | 
  325 | 		Map<String, String> variables = getUriTemplateVariables(exchange);
  326 | 		return UriComponentsBuilder.fromPath(template).build().expand(variables).getPath();
  327 | 	}
  328 | 
  329 | 	@SuppressWarnings("unchecked")
  330 | 	public static void putUriTemplateVariables(ServerWebExchange exchange, Map<String, String> uriVariables) {
  331 | 		if (exchange.getAttributes().containsKey(URI_TEMPLATE_VARIABLES_ATTRIBUTE)) {
  332 | 			Map<String, Object> existingVariables = (Map<String, Object>) exchange.getAttributes()
  333 | 				.get(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  334 | 			HashMap<String, Object> newVariables = new HashMap<>();
  335 | 			newVariables.putAll(existingVariables);
  336 | 			newVariables.putAll(uriVariables);
  337 | 			exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, newVariables);
  338 | 		}
  339 | 		else {
  340 | 			exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
  341 | 		}
  342 | 	}
  343 | 
  344 | 	public static Map<String, String> getUriTemplateVariables(ServerWebExchange exchange) {
  345 | 		return exchange.getAttributeOrDefault(URI_TEMPLATE_VARIABLES_ATTRIBUTE, new HashMap<>());
  346 | 	}
  347 | 
  348 | 	/**
  349 | 	 * Caches the request body and the created {@link ServerHttpRequestDecorator} in
  350 | 	 * ServerWebExchange attributes. Those attributes are
  351 | 	 * {@link #CACHED_REQUEST_BODY_ATTR} and
  352 | 	 * {@link #CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR} respectively. This method is
  353 | 	 * useful when the {@link ServerWebExchange} can not be modified, such as a
  354 | 	 * {@link RoutePredicateFactory}.
  355 | 	 * @param exchange the available ServerWebExchange.
  356 | 	 * @param function a function that accepts the created ServerHttpRequestDecorator.
  357 | 	 * @param <T> generic type for the return {@link Mono}.
  358 | 	 * @return Mono of type T created by the function parameter.
  359 | 	 */
  360 | 	public static <T> Mono<T> cacheRequestBodyAndRequest(ServerWebExchange exchange,
  361 | 			Function<ServerHttpRequest, Mono<T>> function) {
  362 | 		return cacheRequestBody(exchange, true, function);
  363 | 	}
  364 | 
  365 | 	/**
  366 | 	 * Caches the request body in a ServerWebExchange attributes. The attribute is
  367 | 	 * {@link #CACHED_REQUEST_BODY_ATTR}. This method is useful when the
  368 | 	 * {@link ServerWebExchange} can be mutated, such as a {@link GatewayFilterFactory}.
  369 | 	 * @param exchange the available ServerWebExchange.
  370 | 	 * @param function a function that accepts the created ServerHttpRequestDecorator.
  371 | 	 * @param <T> generic type for the return {@link Mono}.
  372 | 	 * @return Mono of type T created by the function parameter.
  373 | 	 */
  374 | 	public static <T> Mono<T> cacheRequestBody(ServerWebExchange exchange,
  375 | 			Function<ServerHttpRequest, Mono<T>> function) {
  376 | 		return cacheRequestBody(exchange, false, function);
  377 | 	}
  378 | 
  379 | 	/**
  380 | 	 * Caches the request body in a ServerWebExchange attribute. The attribute is
  381 | 	 * {@link #CACHED_REQUEST_BODY_ATTR}. If this method is called from a location that
  382 | 	 * can not mutate the ServerWebExchange (such as a Predicate), setting
  383 | 	 * cacheDecoratedRequest to true will put a {@link ServerHttpRequestDecorator} in an
  384 | 	 * attribute {@link #CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR} for adaptation later.
  385 | 	 * @param exchange the available ServerWebExchange.
  386 | 	 * @param cacheDecoratedRequest if true, the ServerHttpRequestDecorator will be
  387 | 	 * cached.
  388 | 	 * @param function a function that accepts a ServerHttpRequest. It can be the created
  389 | 	 * ServerHttpRequestDecorator or the original if there is no body.
  390 | 	 * @param <T> generic type for the return {@link Mono}.
  391 | 	 * @return Mono of type T created by the function parameter.
  392 | 	 */
  393 | 	private static <T> Mono<T> cacheRequestBody(ServerWebExchange exchange, boolean cacheDecoratedRequest,
  394 | 			Function<ServerHttpRequest, Mono<T>> function) {
  395 | 		// don't cache if body is already cached
  396 | 		Object cachedDataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
  397 | 		if (cachedDataBuffer instanceof DataBuffer) {
  398 | 			if (log.isTraceEnabled()) {
  399 | 				log.trace("body already in exchange attribute, short circuiting");
  400 | 			}
  401 | 			return Mono.just(exchange.getRequest()).flatMap(function);
  402 | 		}
  403 | 		ServerHttpResponse response = exchange.getResponse();
  404 | 		DataBufferFactory factory = response.bufferFactory();
  405 | 		// Join all the DataBuffers so we have a single DataBuffer for the body
  406 | 		return DataBufferUtils.join(exchange.getRequest().getBody())
  407 | 			.defaultIfEmpty(factory.wrap(EMPTY_BYTES))
  408 | 			.map(dataBuffer -> decorate(exchange, dataBuffer, cacheDecoratedRequest))
  409 | 			.switchIfEmpty(Mono.just(exchange.getRequest()))
  410 | 			.flatMap(function);
  411 | 	}
  412 | 
  413 | 	/**
  414 | 	 * clear the request body in a ServerWebExchange attribute. The attribute is
  415 | 	 * {@link #CACHED_REQUEST_BODY_ATTR}.
  416 | 	 * @param exchange the available ServerWebExchange.
  417 | 	 */
  418 | 	public static void clearCachedRequestBody(ServerWebExchange exchange) {
  419 | 		Object attribute = exchange.getAttributes().remove(CACHED_REQUEST_BODY_ATTR);
  420 | 		if (attribute != null && attribute instanceof PooledDataBuffer) {
  421 | 			PooledDataBuffer dataBuffer = (PooledDataBuffer) attribute;
  422 | 			if (dataBuffer.isAllocated()) {
  423 | 				if (log.isTraceEnabled()) {
  424 | 					log.trace("releasing cached body in exchange attribute");
  425 | 				}
  426 | 				// ensure proper release
  427 | 				while (!dataBuffer.release()) {
  428 | 					// release() counts down until zero, will never be infinite loop
  429 | 				}
  430 | 			}
  431 | 		}
  432 | 	}
  433 | 
  434 | 	private static ServerHttpRequest decorate(ServerWebExchange exchange, DataBuffer dataBuffer,
  435 | 			boolean cacheDecoratedRequest) {
  436 | 		if (dataBuffer.readableByteCount() > 0) {
  437 | 			if (log.isTraceEnabled()) {
  438 | 				log.trace("retaining body in exchange attribute");
  439 | 			}
  440 | 
  441 | 			Object cachedDataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
  442 | 			// don't cache if body is already cached
  443 | 			if (!(cachedDataBuffer instanceof DataBuffer)) {
  444 | 				exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR, dataBuffer);
  445 | 			}
  446 | 		}
  447 | 
  448 | 		ServerHttpRequest decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
  449 | 			@Override
  450 | 			public Flux<DataBuffer> getBody() {
  451 | 				return Mono.fromSupplier(() -> {
  452 | 					if (exchange.getAttribute(CACHED_REQUEST_BODY_ATTR) == null) {
  453 | 						// probably == downstream closed or no body
  454 | 						return null;
  455 | 					}
  456 | 					if (dataBuffer instanceof NettyDataBuffer) {
  457 | 						NettyDataBuffer pdb = (NettyDataBuffer) dataBuffer;
  458 | 						return pdb.factory().wrap(pdb.getNativeBuffer().retainedSlice());
  459 | 					}
  460 | 					else if (dataBuffer instanceof DefaultDataBuffer) {
  461 | 						DefaultDataBuffer ddf = (DefaultDataBuffer) dataBuffer;
  462 | 						return ddf.factory().wrap(Unpooled.wrappedBuffer(ddf.getNativeBuffer()).nioBuffer());
  463 | 					}
  464 | 					else {
  465 | 						throw new IllegalArgumentException(
  466 | 								"Unable to handle DataBuffer of type " + dataBuffer.getClass());
  467 | 					}
  468 | 				}).flux();
  469 | 			}
  470 | 		};
  471 | 		if (cacheDecoratedRequest) {
  472 | 			exchange.getAttributes().put(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR, decorator);
  473 | 		}
  474 | 		return decorator;
  475 | 	}
  476 | 
  477 | 	/**
  478 | 	 * One place to handle forwarding using DispatcherHandler. Allows for common code to
  479 | 	 * be reused.
  480 | 	 * @param handler The DispatcherHandler.
  481 | 	 * @param exchange The ServerWebExchange.
  482 | 	 * @return value from handler.
  483 | 	 */
  484 | 	public static Mono<Void> handle(DispatcherHandler handler, ServerWebExchange exchange) {
  485 | 		// remove attributes that may disrupt the forwarded request
  486 | 		exchange.getAttributes().remove(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR);
  487 | 
  488 | 		// CORS check is applied to the original request, but should not be applied to
  489 | 		// internally forwarded requests.
  490 | 		// See https://github.com/spring-cloud/spring-cloud-gateway/issues/3350.
  491 | 		exchange = exchange.mutate().request(request -> request.headers(headers -> headers.setOrigin(null))).build();
  492 | 
  493 | 		return handler.handle(exchange);
  494 | 	}
  495 | 
  496 | }
```
