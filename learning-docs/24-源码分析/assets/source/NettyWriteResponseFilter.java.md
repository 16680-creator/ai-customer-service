<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/NettyWriteResponseFilter.java -->
<!-- 正文引用：[[源码:NettyWriteResponseFilter.java#L起-L止]] ；代码块头部：(src: NettyWriteResponseFilter.java#L起-L止) -->

# NettyWriteResponseFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/NettyWriteResponseFilter.java` |
| 行数 | 178 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:50:10.939Z |

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
   19 | import java.util.List;
   20 | 
   21 | import io.netty.buffer.ByteBuf;
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import org.jspecify.annotations.Nullable;
   25 | import reactor.core.publisher.Flux;
   26 | import reactor.core.publisher.Mono;
   27 | import reactor.core.publisher.SignalType;
   28 | import reactor.netty.Connection;
   29 | import reactor.netty.http.client.HttpClientResponse;
   30 | 
   31 | import org.springframework.beans.factory.ObjectProvider;
   32 | import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
   33 | import org.springframework.cloud.gateway.filter.headers.TrailerHeadersFilter;
   34 | import org.springframework.core.Ordered;
   35 | import org.springframework.core.io.buffer.DataBuffer;
   36 | import org.springframework.core.io.buffer.DataBufferFactory;
   37 | import org.springframework.core.io.buffer.DefaultDataBufferFactory;
   38 | import org.springframework.core.io.buffer.NettyDataBufferFactory;
   39 | import org.springframework.http.MediaType;
   40 | import org.springframework.http.server.reactive.ServerHttpResponse;
   41 | import org.springframework.web.server.ServerWebExchange;
   42 | 
   43 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
   44 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR;
   45 | 
   46 | /**
   47 |  * @author Spencer Gibb
   48 |  * @author Seungbin Ko
   49 |  */
   50 | public class NettyWriteResponseFilter implements GlobalFilter, Ordered {
   51 | 
   52 | 	/**
   53 | 	 * Order for write response filter.
   54 | 	 */
   55 | 	public static final int WRITE_RESPONSE_FILTER_ORDER = -1;
   56 | 
   57 | 	private static final Log log = LogFactory.getLog(NettyWriteResponseFilter.class);
   58 | 
   59 | 	private final List<MediaType> streamingMediaTypes;
   60 | 
   61 | 	private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;
   62 | 
   63 | 	// do not use this headersFilters directly, use getHeadersFilters() instead.
   64 | 	private volatile @Nullable List<HttpHeadersFilter> headersFilters;
   65 | 
   66 | 	public NettyWriteResponseFilter(List<MediaType> streamingMediaTypes,
   67 | 			ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider) {
   68 | 		this.streamingMediaTypes = streamingMediaTypes;
   69 | 		this.headersFiltersProvider = headersFiltersProvider;
   70 | 	}
   71 | 
   72 | 	public @Nullable List<HttpHeadersFilter> getHeadersFilters() {
   73 | 		if (headersFilters == null) {
   74 | 			headersFilters = headersFiltersProvider == null ? List.of() : headersFiltersProvider.getIfAvailable();
   75 | 		}
   76 | 		return headersFilters;
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public int getOrder() {
   81 | 		return WRITE_RESPONSE_FILTER_ORDER;
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   86 | 		// NOTICE: nothing in "pre" filter stage as CLIENT_RESPONSE_CONN_ATTR is not added
   87 | 		// until the NettyRoutingFilter is run
   88 | 		// @formatter:off
   89 | 		return chain.filter(exchange)
   90 | 				.then(Mono.defer(() -> {
   91 | 					Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
   92 | 
   93 | 					if (connection == null) {
   94 | 						return Mono.empty();
   95 | 					}
   96 | 					ServerHttpResponse response = exchange.getResponse();
   97 | 					if (response.isCommitted()) {
   98 | 						if (log.isDebugEnabled()) {
   99 | 							log.debug("NettyWriteResponseFilter response already committed, discarding inbound: "
  100 | 									+ connection.channel().id().asShortText() + ", outbound: "
  101 | 									+ exchange.getLogPrefix());
  102 | 						}
  103 | 						cleanup(exchange);
  104 | 						return Mono.empty();
  105 | 					}
  106 | 					if (log.isTraceEnabled()) {
  107 | 						log.trace("NettyWriteResponseFilter start inbound: "
  108 | 								+ connection.channel().id().asShortText() + ", outbound: "
  109 | 								+ exchange.getLogPrefix());
  110 | 					}
  111 | 
  112 | 					// TODO: needed?
  113 | 					final Flux<DataBuffer> body = connection
  114 | 							.inbound()
  115 | 							.receive()
  116 | 							.retain()
  117 | 							.map(byteBuf -> wrap(byteBuf, response));
  118 | 
  119 | 					MediaType contentType = null;
  120 | 					try {
  121 | 						contentType = response.getHeaders().getContentType();
  122 | 					}
  123 | 					catch (Exception e) {
  124 | 						if (log.isTraceEnabled()) {
  125 | 							log.trace("invalid media type", e);
  126 | 						}
  127 | 					}
  128 | 
  129 | 					HttpClientResponse httpClientResponse = exchange.getAttribute(CLIENT_RESPONSE_ATTR);
  130 | 					Mono<Void> write = (isStreamingMediaType(contentType)
  131 | 							? response.writeAndFlushWith(body.map(Flux::just))
  132 | 							: response.writeWith(body));
  133 | 					return write.then(TrailerHeadersFilter.filter(getHeadersFilters(), exchange, httpClientResponse)).then();
  134 | 				}))
  135 | 				.doFinally(signalType -> {
  136 | 					if (signalType == SignalType.CANCEL || signalType == SignalType.ON_ERROR) {
  137 | 						cleanup(exchange);
  138 | 					}
  139 | 				});
  140 | 		// @formatter:on
  141 | 	}
  142 | 
  143 | 	protected DataBuffer wrap(ByteBuf byteBuf, ServerHttpResponse response) {
  144 | 		DataBufferFactory bufferFactory = response.bufferFactory();
  145 | 		if (bufferFactory instanceof NettyDataBufferFactory) {
  146 | 			NettyDataBufferFactory factory = (NettyDataBufferFactory) bufferFactory;
  147 | 			return factory.wrap(byteBuf);
  148 | 		}
  149 | 		// MockServerHttpResponse creates these
  150 | 		else if (bufferFactory instanceof DefaultDataBufferFactory) {
  151 | 			DataBuffer buffer = ((DefaultDataBufferFactory) bufferFactory).allocateBuffer(byteBuf.readableBytes());
  152 | 			buffer.write(byteBuf.nioBuffer());
  153 | 			byteBuf.release();
  154 | 			return buffer;
  155 | 		}
  156 | 		throw new IllegalArgumentException("Unknown DataBufferFactory type " + bufferFactory.getClass());
  157 | 	}
  158 | 
  159 | 	private void cleanup(ServerWebExchange exchange) {
  160 | 		Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
  161 | 		if (connection != null) {
  162 | 			connection.dispose();
  163 | 		}
  164 | 	}
  165 | 
  166 | 	// TODO: use framework if possible
  167 | 	private boolean isStreamingMediaType(@Nullable MediaType contentType) {
  168 | 		if (contentType != null) {
  169 | 			for (int i = 0; i < streamingMediaTypes.size(); i++) {
  170 | 				if (streamingMediaTypes.get(i).isCompatibleWith(contentType)) {
  171 | 					return true;
  172 | 				}
  173 | 			}
  174 | 		}
  175 | 		return false;
  176 | 	}
  177 | 
  178 | }
```
