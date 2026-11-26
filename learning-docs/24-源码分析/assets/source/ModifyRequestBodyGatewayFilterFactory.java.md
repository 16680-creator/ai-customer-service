<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/rewrite/ModifyRequestBodyGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:ModifyRequestBodyGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: ModifyRequestBodyGatewayFilterFactory.java#L起-L止) -->

# ModifyRequestBodyGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/rewrite/ModifyRequestBodyGatewayFilterFactory.java` |
| 行数 | 237 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:48.243Z |

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
   20 | import java.util.Objects;
   21 | import java.util.function.Function;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | import reactor.core.publisher.Flux;
   25 | import reactor.core.publisher.Mono;
   26 | 
   27 | import org.springframework.boot.http.codec.CodecCustomizer;
   28 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   29 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   30 | import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
   31 | import org.springframework.cloud.gateway.support.BodyInserterContext;
   32 | import org.springframework.core.ParameterizedTypeReference;
   33 | import org.springframework.core.io.buffer.DataBuffer;
   34 | import org.springframework.core.io.buffer.DataBufferUtils;
   35 | import org.springframework.http.HttpHeaders;
   36 | import org.springframework.http.codec.HttpMessageReader;
   37 | import org.springframework.http.server.reactive.ServerHttpRequest;
   38 | import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
   39 | import org.springframework.web.reactive.function.BodyInserter;
   40 | import org.springframework.web.reactive.function.BodyInserters;
   41 | import org.springframework.web.reactive.function.client.ExchangeStrategies;
   42 | import org.springframework.web.reactive.function.server.HandlerStrategies;
   43 | import org.springframework.web.reactive.function.server.ServerRequest;
   44 | import org.springframework.web.server.ServerWebExchange;
   45 | 
   46 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   47 | 
   48 | /**
   49 |  * GatewayFilter that modifies the request body.
   50 |  * <p>
   51 |  * When the request has no body, the {@link RewriteFunction} is invoked with {@code null}
   52 |  * for the body parameter. Implementations (including Kotlin with a nullable parameter)
   53 |  * must handle this case.
   54 |  */
   55 | public class ModifyRequestBodyGatewayFilterFactory
   56 | 		extends AbstractGatewayFilterFactory<ModifyRequestBodyGatewayFilterFactory.Config> {
   57 | 
   58 | 	private final List<HttpMessageReader<?>> messageReaders;
   59 | 
   60 | 	private final ExchangeStrategies exchangeStrategies;
   61 | 
   62 | 	public ModifyRequestBodyGatewayFilterFactory() {
   63 | 		this(HandlerStrategies.withDefaults().messageReaders(), List.of());
   64 | 	}
   65 | 
   66 | 	public ModifyRequestBodyGatewayFilterFactory(List<HttpMessageReader<?>> messageReaders) {
   67 | 		this(messageReaders, List.of());
   68 | 	}
   69 | 
   70 | 	public ModifyRequestBodyGatewayFilterFactory(List<HttpMessageReader<?>> messageReaders,
   71 | 			List<CodecCustomizer> codecCustomizers) {
   72 | 		super(Config.class);
   73 | 		this.messageReaders = messageReaders;
   74 | 		this.exchangeStrategies = BodyInserterContext.buildExchangeStrategies(codecCustomizers);
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	@SuppressWarnings("unchecked")
   79 | 	public GatewayFilter apply(Config config) {
   80 | 		return new GatewayFilter() {
   81 | 			@Override
   82 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   83 | 				ParameterizedTypeReference inClass = Objects.requireNonNull(config.getInClass(),
   84 | 						"inClass must not be null");
   85 | 				RewriteFunction rewriteFunction = Objects.requireNonNull(config.getRewriteFunction(),
   86 | 						"rewriteFunction must not be null");
   87 | 				ParameterizedTypeReference outClass = Objects.requireNonNull(config.getOutClass(),
   88 | 						"outClass must not be null");
   89 | 				ServerRequest serverRequest = ServerRequest.create(exchange, messageReaders);
   90 | 
   91 | 				// TODO: flux or mono
   92 | 				Mono<?> modifiedBody = serverRequest.bodyToMono(inClass)
   93 | 					.flatMap(originalBody -> rewriteFunction.apply(exchange, originalBody))
   94 | 					.switchIfEmpty(Mono.defer(() -> (Mono) rewriteFunction.apply(exchange, null)));
   95 | 
   96 | 				BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, outClass);
   97 | 				HttpHeaders headers = new HttpHeaders();
   98 | 				headers.putAll(exchange.getRequest().getHeaders());
   99 | 
  100 | 				// the new content type will be computed by bodyInserter
  101 | 				// and then set in the request decorator
  102 | 				headers.remove(HttpHeaders.CONTENT_LENGTH);
  103 | 
  104 | 				// if the body is changing content types, set it here, to the bodyInserter
  105 | 				// will know about it
  106 | 				if (config.getContentType() != null) {
  107 | 					headers.set(HttpHeaders.CONTENT_TYPE, config.getContentType());
  108 | 				}
  109 | 				CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
  110 | 				return bodyInserter.insert(outputMessage, new BodyInserterContext(exchangeStrategies))
  111 | 					// .log("modify_request", Level.INFO)
  112 | 					.then(Mono.defer(() -> {
  113 | 						ServerHttpRequest decorator = decorate(exchange, headers, outputMessage);
  114 | 						return chain.filter(exchange.mutate().request(decorator).build());
  115 | 					}))
  116 | 					.onErrorResume(
  117 | 							(Function<Throwable, Mono<Void>>) throwable -> release(exchange, outputMessage, throwable));
  118 | 			}
  119 | 
  120 | 			@Override
  121 | 			public String toString() {
  122 | 				return filterToStringCreator(ModifyRequestBodyGatewayFilterFactory.this)
  123 | 					.append("Content type", config.getContentType())
  124 | 					.append("In class", config.getInClass())
  125 | 					.append("Out class", config.getOutClass())
  126 | 					.toString();
  127 | 			}
  128 | 		};
  129 | 	}
  130 | 
  131 | 	protected Mono<Void> release(ServerWebExchange exchange, CachedBodyOutputMessage outputMessage,
  132 | 			Throwable throwable) {
  133 | 		if (outputMessage.isCached()) {
  134 | 			return outputMessage.getBody().map(DataBufferUtils::release).then(Mono.error(throwable));
  135 | 		}
  136 | 		return Mono.error(throwable);
  137 | 	}
  138 | 
  139 | 	ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers,
  140 | 			CachedBodyOutputMessage outputMessage) {
  141 | 		return new ServerHttpRequestDecorator(exchange.getRequest()) {
  142 | 			@Override
  143 | 			public HttpHeaders getHeaders() {
  144 | 				long contentLength = headers.getContentLength();
  145 | 				HttpHeaders httpHeaders = new HttpHeaders();
  146 | 				httpHeaders.putAll(headers);
  147 | 				if (contentLength > 0) {
  148 | 					httpHeaders.setContentLength(contentLength);
  149 | 				}
  150 | 				else {
  151 | 					// TODO: this causes a 'HTTP/1.1 411 Length Required' // on
  152 | 					// httpbin.org
  153 | 					httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
  154 | 				}
  155 | 				return httpHeaders;
  156 | 			}
  157 | 
  158 | 			@Override
  159 | 			public Flux<DataBuffer> getBody() {
  160 | 				return outputMessage.getBody();
  161 | 			}
  162 | 		};
  163 | 	}
  164 | 
  165 | 	public static class Config {
  166 | 
  167 | 		private @Nullable ParameterizedTypeReference inClass;
  168 | 
  169 | 		private @Nullable ParameterizedTypeReference outClass;
  170 | 
  171 | 		private @Nullable String contentType;
  172 | 
  173 | 		private @Nullable RewriteFunction rewriteFunction;
  174 | 
  175 | 		public @Nullable ParameterizedTypeReference getInClass() {
  176 | 			return inClass;
  177 | 		}
  178 | 
  179 | 		public Config setInClass(Class inClass) {
  180 | 			return setInClass(ParameterizedTypeReference.forType(inClass));
  181 | 		}
  182 | 
  183 | 		public Config setInClass(ParameterizedTypeReference inTypeReference) {
  184 | 			this.inClass = inTypeReference;
  185 | 			return this;
  186 | 		}
  187 | 
  188 | 		public @Nullable ParameterizedTypeReference getOutClass() {
  189 | 			return outClass;
  190 | 		}
  191 | 
  192 | 		public Config setOutClass(Class outClass) {
  193 | 			return setOutClass(ParameterizedTypeReference.forType(outClass));
  194 | 		}
  195 | 
  196 | 		public Config setOutClass(ParameterizedTypeReference outClass) {
  197 | 			this.outClass = outClass;
  198 | 			return this;
  199 | 		}
  200 | 
  201 | 		public @Nullable RewriteFunction getRewriteFunction() {
  202 | 			return rewriteFunction;
  203 | 		}
  204 | 
  205 | 		public Config setRewriteFunction(RewriteFunction rewriteFunction) {
  206 | 			this.rewriteFunction = rewriteFunction;
  207 | 			return this;
  208 | 		}
  209 | 
  210 | 		public <T, R> Config setRewriteFunction(Class<T> inClass, Class<R> outClass,
  211 | 				RewriteFunction<T, R> rewriteFunction) {
  212 | 			setInClass(inClass);
  213 | 			setOutClass(outClass);
  214 | 			setRewriteFunction(rewriteFunction);
  215 | 			return this;
  216 | 		}
  217 | 
  218 | 		public <T, R> Config setRewriteFunction(ParameterizedTypeReference<T> inClass,
  219 | 				ParameterizedTypeReference<R> outClass, RewriteFunction<T, R> rewriteFunction) {
  220 | 			setInClass(inClass);
  221 | 			setOutClass(outClass);
  222 | 			setRewriteFunction(rewriteFunction);
  223 | 			return this;
  224 | 		}
  225 | 
  226 | 		public @Nullable String getContentType() {
  227 | 			return contentType;
  228 | 		}
  229 | 
  230 | 		public Config setContentType(@Nullable String contentType) {
  231 | 			this.contentType = contentType;
  232 | 			return this;
  233 | 		}
  234 | 
  235 | 	}
  236 | 
  237 | }
```
