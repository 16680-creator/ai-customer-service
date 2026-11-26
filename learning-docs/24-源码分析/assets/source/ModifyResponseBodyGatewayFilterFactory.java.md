<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/rewrite/ModifyResponseBodyGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:ModifyResponseBodyGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: ModifyResponseBodyGatewayFilterFactory.java#L起-L止) -->

# ModifyResponseBodyGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/rewrite/ModifyResponseBodyGatewayFilterFactory.java` |
| 行数 | 331 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:50:11.034Z |

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
   17 | package org.springframework.cloud.gateway.filter.factory.rewrite;
   18 | 
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.Set;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | import org.reactivestreams.Publisher;
   27 | import reactor.core.publisher.Flux;
   28 | import reactor.core.publisher.Mono;
   29 | import reactor.core.scheduler.Schedulers;
   30 | 
   31 | import org.springframework.boot.http.codec.CodecCustomizer;
   32 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   33 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   34 | import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
   35 | import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
   36 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   37 | import org.springframework.cloud.gateway.support.BodyInserterContext;
   38 | import org.springframework.core.Ordered;
   39 | import org.springframework.core.io.buffer.DataBuffer;
   40 | import org.springframework.core.io.buffer.DataBufferFactory;
   41 | import org.springframework.core.io.buffer.DataBufferUtils;
   42 | import org.springframework.http.HttpHeaders;
   43 | import org.springframework.http.codec.HttpMessageReader;
   44 | import org.springframework.http.server.reactive.ServerHttpResponse;
   45 | import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
   46 | import org.springframework.util.StringUtils;
   47 | import org.springframework.web.reactive.function.BodyInserter;
   48 | import org.springframework.web.reactive.function.BodyInserters;
   49 | import org.springframework.web.reactive.function.client.ClientResponse;
   50 | import org.springframework.web.reactive.function.client.ExchangeStrategies;
   51 | import org.springframework.web.server.ServerWebExchange;
   52 | 
   53 | import static java.util.function.Function.identity;
   54 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   55 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;
   56 | 
   57 | /**
   58 |  * GatewayFilter that modifies the response body.
   59 |  * <p>
   60 |  * When the response has no body, the {@link RewriteFunction} is invoked with {@code null}
   61 |  * for the body parameter. Implementations (including Kotlin with a nullable parameter)
   62 |  * must handle this case.
   63 |  */
   64 | public class ModifyResponseBodyGatewayFilterFactory
   65 | 		extends AbstractGatewayFilterFactory<ModifyResponseBodyGatewayFilterFactory.Config> {
   66 | 
   67 | 	private final Map<String, MessageBodyDecoder> messageBodyDecoders;
   68 | 
   69 | 	private final Map<String, MessageBodyEncoder> messageBodyEncoders;
   70 | 
   71 | 	private final List<HttpMessageReader<?>> messageReaders;
   72 | 
   73 | 	private final ExchangeStrategies exchangeStrategies;
   74 | 
   75 | 	public ModifyResponseBodyGatewayFilterFactory(List<HttpMessageReader<?>> messageReaders,
   76 | 			Set<MessageBodyDecoder> messageBodyDecoders, Set<MessageBodyEncoder> messageBodyEncoders) {
   77 | 		this(messageReaders, messageBodyDecoders, messageBodyEncoders, List.of());
   78 | 	}
   79 | 
   80 | 	public ModifyResponseBodyGatewayFilterFactory(List<HttpMessageReader<?>> messageReaders,
   81 | 			Set<MessageBodyDecoder> messageBodyDecoders, Set<MessageBodyEncoder> messageBodyEncoders,
   82 | 			List<CodecCustomizer> codecCustomizers) {
   83 | 		super(Config.class);
   84 | 		this.messageReaders = messageReaders;
   85 | 		this.messageBodyDecoders = messageBodyDecoders.stream()
   86 | 			.collect(Collectors.toMap(MessageBodyDecoder::encodingType, identity()));
   87 | 		this.messageBodyEncoders = messageBodyEncoders.stream()
   88 | 			.collect(Collectors.toMap(MessageBodyEncoder::encodingType, identity()));
   89 | 		this.exchangeStrategies = BodyInserterContext.buildExchangeStrategies(codecCustomizers);
   90 | 	}
   91 | 
   92 | 	@Override
   93 | 	public GatewayFilter apply(Config config) {
   94 | 		ModifyResponseGatewayFilter gatewayFilter = new ModifyResponseGatewayFilter(config);
   95 | 		gatewayFilter.setFactory(this);
   96 | 		return gatewayFilter;
   97 | 	}
   98 | 
   99 | 	public static class Config {
  100 | 
  101 | 		private @Nullable Class inClass;
  102 | 
  103 | 		private @Nullable Class outClass;
  104 | 
  105 | 		private @Nullable Map<String, Object> inHints;
  106 | 
  107 | 		private @Nullable Map<String, Object> outHints;
  108 | 
  109 | 		private @Nullable String newContentType;
  110 | 
  111 | 		private @Nullable RewriteFunction rewriteFunction;
  112 | 
  113 | 		public @Nullable Class getInClass() {
  114 | 			return inClass;
  115 | 		}
  116 | 
  117 | 		public Config setInClass(Class inClass) {
  118 | 			this.inClass = inClass;
  119 | 			return this;
  120 | 		}
  121 | 
  122 | 		public @Nullable Class getOutClass() {
  123 | 			return outClass;
  124 | 		}
  125 | 
  126 | 		public Config setOutClass(Class outClass) {
  127 | 			this.outClass = outClass;
  128 | 			return this;
  129 | 		}
  130 | 
  131 | 		public @Nullable Map<String, Object> getInHints() {
  132 | 			return inHints;
  133 | 		}
  134 | 
  135 | 		public Config setInHints(Map<String, Object> inHints) {
  136 | 			this.inHints = inHints;
  137 | 			return this;
  138 | 		}
  139 | 
  140 | 		public @Nullable Map<String, Object> getOutHints() {
  141 | 			return outHints;
  142 | 		}
  143 | 
  144 | 		public Config setOutHints(Map<String, Object> outHints) {
  145 | 			this.outHints = outHints;
  146 | 			return this;
  147 | 		}
  148 | 
  149 | 		public @Nullable String getNewContentType() {
  150 | 			return newContentType;
  151 | 		}
  152 | 
  153 | 		public Config setNewContentType(@Nullable String newContentType) {
  154 | 			this.newContentType = newContentType;
  155 | 			return this;
  156 | 		}
  157 | 
  158 | 		public @Nullable RewriteFunction getRewriteFunction() {
  159 | 			return rewriteFunction;
  160 | 		}
  161 | 
  162 | 		public Config setRewriteFunction(RewriteFunction rewriteFunction) {
  163 | 			this.rewriteFunction = rewriteFunction;
  164 | 			return this;
  165 | 		}
  166 | 
  167 | 		public <T, R> Config setRewriteFunction(Class<T> inClass, Class<R> outClass,
  168 | 				RewriteFunction<T, R> rewriteFunction) {
  169 | 			setInClass(inClass);
  170 | 			setOutClass(outClass);
  171 | 			setRewriteFunction(rewriteFunction);
  172 | 			return this;
  173 | 		}
  174 | 
  175 | 	}
  176 | 
  177 | 	public class ModifyResponseGatewayFilter implements GatewayFilter, Ordered {
  178 | 
  179 | 		private final Config config;
  180 | 
  181 | 		private @Nullable GatewayFilterFactory<Config> gatewayFilterFactory;
  182 | 
  183 | 		public ModifyResponseGatewayFilter(Config config) {
  184 | 			this.config = config;
  185 | 		}
  186 | 
  187 | 		@Override
  188 | 		public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
  189 | 			return chain.filter(exchange.mutate().response(new ModifiedServerHttpResponse(exchange, config)).build());
  190 | 		}
  191 | 
  192 | 		@Override
  193 | 		public int getOrder() {
  194 | 			return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
  195 | 		}
  196 | 
  197 | 		@Override
  198 | 		public String toString() {
  199 | 			Object obj = (this.gatewayFilterFactory != null) ? this.gatewayFilterFactory : this;
  200 | 			return filterToStringCreator(obj).append("New content type", config.getNewContentType())
  201 | 				.append("In class", config.getInClass())
  202 | 				.append("Out class", config.getOutClass())
  203 | 				.toString();
  204 | 		}
  205 | 
  206 | 		public void setFactory(GatewayFilterFactory<Config> gatewayFilterFactory) {
  207 | 			this.gatewayFilterFactory = gatewayFilterFactory;
  208 | 		}
  209 | 
  210 | 	}
  211 | 
  212 | 	protected class ModifiedServerHttpResponse extends ServerHttpResponseDecorator {
  213 | 
  214 | 		private final ServerWebExchange exchange;
  215 | 
  216 | 		private final Config config;
  217 | 
  218 | 		public ModifiedServerHttpResponse(ServerWebExchange exchange, Config config) {
  219 | 			super(exchange.getResponse());
  220 | 			this.exchange = exchange;
  221 | 			this.config = config;
  222 | 		}
  223 | 
  224 | 		@SuppressWarnings("unchecked")
  225 | 		@Override
  226 | 		public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
  227 | 
  228 | 			Class inClass = Objects.requireNonNull(config.getInClass(), "inClass must not be null");
  229 | 			Class outClass = Objects.requireNonNull(config.getOutClass(), "outClass must not be null");
  230 | 			RewriteFunction rewriteFunction = Objects.requireNonNull(config.getRewriteFunction(),
  231 | 					"rewriteFunction must not be null");
  232 | 
  233 | 			String originalResponseContentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
  234 | 			HttpHeaders httpHeaders = new HttpHeaders();
  235 | 			// explicitly add it in this way instead of
  236 | 			// 'httpHeaders.setContentType(originalResponseContentType)'
  237 | 			// this will prevent exception in case of using non-standard media
  238 | 			// types like "Content-Type: image"
  239 | 			httpHeaders.add(HttpHeaders.CONTENT_TYPE, originalResponseContentType);
  240 | 
  241 | 			ClientResponse clientResponse = prepareClientResponse(body, httpHeaders);
  242 | 
  243 | 			// TODO: flux or mono
  244 | 			Mono modifiedBody = extractBody(exchange, clientResponse, inClass)
  245 | 				.flatMap(originalBody -> rewriteFunction.apply(exchange, originalBody))
  246 | 				.switchIfEmpty(Mono.defer(() -> (Mono) rewriteFunction.apply(exchange, null)));
  247 | 
  248 | 			BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, outClass);
  249 | 			CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
  250 | 					exchange.getResponse().getHeaders());
  251 | 			return bodyInserter.insert(outputMessage, new BodyInserterContext(exchangeStrategies))
  252 | 				.then(Mono.defer(() -> {
  253 | 					Mono<DataBuffer> messageBody = writeBody(getDelegate(), outputMessage, outClass);
  254 | 					HttpHeaders headers = getDelegate().getHeaders();
  255 | 					if (!headers.containsHeader(HttpHeaders.TRANSFER_ENCODING)
  256 | 							|| headers.containsHeader(HttpHeaders.CONTENT_LENGTH)) {
  257 | 						messageBody = messageBody.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
  258 | 					}
  259 | 
  260 | 					if (StringUtils.hasText(config.newContentType)) {
  261 | 						headers.set(HttpHeaders.CONTENT_TYPE, config.newContentType);
  262 | 					}
  263 | 
  264 | 					// TODO: fail if isStreamingMediaType?
  265 | 					return getDelegate().writeWith(messageBody);
  266 | 				}));
  267 | 		}
  268 | 
  269 | 		@Override
  270 | 		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
  271 | 			return writeWith(Flux.from(body).flatMapSequential(p -> p));
  272 | 		}
  273 | 
  274 | 		private ClientResponse prepareClientResponse(Publisher<? extends DataBuffer> body, HttpHeaders httpHeaders) {
  275 | 			ClientResponse.Builder builder;
  276 | 			builder = ClientResponse.create(
  277 | 					Objects.requireNonNull(exchange.getResponse().getStatusCode(), "Status code must not be null"),
  278 | 					messageReaders);
  279 | 			return builder.headers(headers -> headers.putAll(httpHeaders)).body(Flux.from(body)).build();
  280 | 		}
  281 | 
  282 | 		private <T> Mono<T> extractBody(ServerWebExchange exchange, ClientResponse clientResponse, Class<T> inClass) {
  283 | 			// if inClass is byte[] then just return body, otherwise check if
  284 | 			// decoding required
  285 | 			if (byte[].class.isAssignableFrom(inClass)) {
  286 | 				return clientResponse.bodyToMono(inClass);
  287 | 			}
  288 | 
  289 | 			List<String> encodingHeaders = exchange.getResponse().getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
  290 | 			for (String encoding : encodingHeaders) {
  291 | 				MessageBodyDecoder decoder = messageBodyDecoders.get(encoding);
  292 | 				if (decoder != null) {
  293 | 					return clientResponse.bodyToMono(byte[].class)
  294 | 						.publishOn(Schedulers.parallel())
  295 | 						.map(decoder::decode)
  296 | 						.map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
  297 | 						.map(buffer -> prepareClientResponse(Mono.just(buffer), exchange.getResponse().getHeaders()))
  298 | 						.flatMap(response -> response.bodyToMono(inClass));
  299 | 				}
  300 | 			}
  301 | 
  302 | 			return clientResponse.bodyToMono(inClass);
  303 | 		}
  304 | 
  305 | 		private Mono<DataBuffer> writeBody(ServerHttpResponse httpResponse, CachedBodyOutputMessage message,
  306 | 				Class<?> outClass) {
  307 | 			Mono<DataBuffer> response = DataBufferUtils.join(message.getBody());
  308 | 			if (byte[].class.isAssignableFrom(outClass)) {
  309 | 				return response;
  310 | 			}
  311 | 
  312 | 			List<String> encodingHeaders = httpResponse.getHeaders().getOrEmpty(HttpHeaders.CONTENT_ENCODING);
  313 | 			for (String encoding : encodingHeaders) {
  314 | 				MessageBodyEncoder encoder = messageBodyEncoders.get(encoding);
  315 | 				if (encoder != null) {
  316 | 					DataBufferFactory dataBufferFactory = httpResponse.bufferFactory();
  317 | 					response = response.publishOn(Schedulers.parallel()).map(buffer -> {
  318 | 						byte[] encodedResponse = encoder.encode(buffer);
  319 | 						DataBufferUtils.release(buffer);
  320 | 						return encodedResponse;
  321 | 					}).map(dataBufferFactory::wrap);
  322 | 					break;
  323 | 				}
  324 | 			}
  325 | 
  326 | 			return response;
  327 | 		}
  328 | 
  329 | 	}
  330 | 
  331 | }
```
