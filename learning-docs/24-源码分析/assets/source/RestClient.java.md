<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/client/RestClient.java -->
<!-- 正文引用：[[源码:RestClient.java#L起-L止]] ；代码块头部：(src: RestClient.java#L起-L止) -->

# RestClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/client/RestClient.java` |
| 行数 | 1166 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:37.789Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.client;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.OutputStream;
   21 | import java.net.URI;
   22 | import java.nio.charset.Charset;
   23 | import java.time.ZonedDateTime;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.function.BiPredicate;
   27 | import java.util.function.Consumer;
   28 | import java.util.function.Function;
   29 | import java.util.function.Predicate;
   30 | 
   31 | import io.micrometer.observation.ObservationRegistry;
   32 | import org.jspecify.annotations.NonNull;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.core.ParameterizedTypeReference;
   36 | import org.springframework.http.HttpHeaders;
   37 | import org.springframework.http.HttpMethod;
   38 | import org.springframework.http.HttpRequest;
   39 | import org.springframework.http.HttpStatusCode;
   40 | import org.springframework.http.MediaType;
   41 | import org.springframework.http.ResponseEntity;
   42 | import org.springframework.http.StreamingHttpOutputMessage;
   43 | import org.springframework.http.client.ClientHttpRequest;
   44 | import org.springframework.http.client.ClientHttpRequestFactory;
   45 | import org.springframework.http.client.ClientHttpRequestInitializer;
   46 | import org.springframework.http.client.ClientHttpRequestInterceptor;
   47 | import org.springframework.http.client.ClientHttpResponse;
   48 | import org.springframework.http.client.observation.ClientRequestObservationConvention;
   49 | import org.springframework.http.converter.HttpMessageConverter;
   50 | import org.springframework.http.converter.HttpMessageConverters;
   51 | import org.springframework.lang.CheckReturnValue;
   52 | import org.springframework.util.MultiValueMap;
   53 | import org.springframework.web.util.DefaultUriBuilderFactory;
   54 | import org.springframework.web.util.UriBuilder;
   55 | import org.springframework.web.util.UriBuilderFactory;
   56 | 
   57 | /**
   58 |  * Client to perform HTTP requests, exposing a fluent, synchronous API over
   59 |  * underlying HTTP client libraries such as the JDK {@code HttpClient}, Apache
   60 |  * HttpComponents, and others.
   61 |  *
   62 |  * <p>Use static factory methods {@link #create()}, {@link #create(String)},
   63 |  * or {@link RestClient#builder()} to prepare an instance. To use the same
   64 |  * configuration as a {@link RestTemplate}, use {@link #create(RestTemplate)} or
   65 |  * {@link #builder(RestTemplate)}.
   66 |  *
   67 |  * <p>For examples with a response body see:
   68 |  * <ul>
   69 |  * <li>{@link RequestHeadersSpec#retrieve() retrieve()}
   70 |  * <li>{@link RequestHeadersSpec#exchange(RequestHeadersSpec.ExchangeFunction) exchange(Function&lt;ClientHttpRequest, T&gt;)}
   71 |  * </ul>
   72 |  *
   73 |  * <p>For examples with a request body see:
   74 |  * <ul>
   75 |  * <li>{@link RequestBodySpec#body(Object) body(Object)}
   76 |  * <li>{@link RequestBodySpec#body(Object, ParameterizedTypeReference) body(Object, ParameterizedTypeReference)}
   77 |  * <li>{@link RequestBodySpec#body(StreamingHttpOutputMessage.Body) body(Consumer&lt;OutputStream&gt;)}
   78 |  * </ul>
   79 |  *
   80 |  * @author Arjen Poutsma
   81 |  * @author Sebastien Deleuze
   82 |  * @since 6.1
   83 |  */
   84 | public interface RestClient {
   85 | 
   86 | 	/**
   87 | 	 * Start building an HTTP GET request.
   88 | 	 * @return a spec for specifying the target URL
   89 | 	 */
   90 | 	RequestHeadersUriSpec<?> get();
   91 | 
   92 | 	/**
   93 | 	 * Start building an HTTP HEAD request.
   94 | 	 * @return a spec for specifying the target URL
   95 | 	 */
   96 | 	RequestHeadersUriSpec<?> head();
   97 | 
   98 | 	/**
   99 | 	 * Start building an HTTP POST request.
  100 | 	 * @return a spec for specifying the target URL
  101 | 	 */
  102 | 	RequestBodyUriSpec post();
  103 | 
  104 | 	/**
  105 | 	 * Start building an HTTP PUT request.
  106 | 	 * @return a spec for specifying the target URL
  107 | 	 */
  108 | 	RequestBodyUriSpec put();
  109 | 
  110 | 	/**
  111 | 	 * Start building an HTTP PATCH request.
  112 | 	 * @return a spec for specifying the target URL
  113 | 	 */
  114 | 	RequestBodyUriSpec patch();
  115 | 
  116 | 	/**
  117 | 	 * Start building an HTTP DELETE request.
  118 | 	 * @return a spec for specifying the target URL
  119 | 	 */
  120 | 	RequestHeadersUriSpec<?> delete();
  121 | 
  122 | 	/**
  123 | 	 * Start building an HTTP OPTIONS request.
  124 | 	 * @return a spec for specifying the target URL
  125 | 	 */
  126 | 	RequestHeadersUriSpec<?> options();
  127 | 
  128 | 	/**
  129 | 	 * Start building a request for the given {@code HttpMethod}.
  130 | 	 * @return a spec for specifying the target URL
  131 | 	 */
  132 | 	RequestBodyUriSpec method(HttpMethod method);
  133 | 
  134 | 
  135 | 	/**
  136 | 	 * Return a builder to create a new {@code RestClient} whose settings are
  137 | 	 * replicated from this {@code RestClient}.
  138 | 	 */
  139 | 	Builder mutate();
  140 | 
  141 | 
  142 | 	// Static factory methods
  143 | 
  144 | 	/**
  145 | 	 * Create a new {@code RestClient}.
  146 | 	 * @see #create(String)
  147 | 	 * @see #builder()
  148 | 	 */
  149 | 	static RestClient create() {
  150 | 		return new DefaultRestClientBuilder().build();
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Variant of {@link #create()} that accepts a default base URL. For more
  155 | 	 * details see {@link Builder#baseUrl(String) Builder.baseUrl(String)}.
  156 | 	 * @param baseUrl the base URI for all requests
  157 | 	 * @see #builder()
  158 | 	 */
  159 | 	static RestClient create(String baseUrl) {
  160 | 		return new DefaultRestClientBuilder().baseUrl(baseUrl).build();
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Variant of {@link #create()} that accepts a default base {@code URI}. For more
  165 | 	 * details see {@link Builder#baseUrl(URI) Builder.baseUrl(URI)}.
  166 | 	 * @param baseUrl the base URI for all requests
  167 | 	 * @since 6.2
  168 | 	 * @see #builder()
  169 | 	 */
  170 | 	static RestClient create(URI baseUrl) {
  171 | 		return new DefaultRestClientBuilder().baseUrl(baseUrl).build();
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Create a new {@code RestClient} based on the configuration of the given
  176 | 	 * {@code RestTemplate}.
  177 | 	 * <p>The returned builder is configured with the following attributes of
  178 | 	 * the template.
  179 | 	 * <ul>
  180 | 	 * <li>{@link RestTemplate#getRequestFactory() ClientHttpRequestFactory}</li>
  181 | 	 * <li>{@link RestTemplate#getMessageConverters() HttpMessageConverters}</li>
  182 | 	 * <li>{@link RestTemplate#getInterceptors() ClientHttpRequestInterceptors}</li>
  183 | 	 * <li>{@link RestTemplate#getClientHttpRequestInitializers() ClientHttpRequestInitializers}</li>
  184 | 	 * <li>{@link RestTemplate#getUriTemplateHandler() UriBuilderFactory}</li>
  185 | 	 * <li>{@linkplain RestTemplate#getErrorHandler() error handler}</li>
  186 | 	 * </ul>
  187 | 	 * @param restTemplate the rest template to base the returned client's
  188 | 	 * configuration on
  189 | 	 * @return a {@code RestClient} initialized with the {@code restTemplate}'s
  190 | 	 * configuration
  191 | 	 */
  192 | 	@SuppressWarnings("removal")
  193 | 	static RestClient create(RestTemplate restTemplate) {
  194 | 		return new DefaultRestClientBuilder(restTemplate).build();
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Obtain a {@code RestClient} builder.
  199 | 	 */
  200 | 	static RestClient.Builder builder() {
  201 | 		return new DefaultRestClientBuilder();
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Obtain a {@code RestClient} builder based on the configuration of the
  206 | 	 * given {@code RestTemplate}.
  207 | 	 * <p>The returned builder is configured with the following attributes of
  208 | 	 * the template.
  209 | 	 * <ul>
  210 | 	 * <li>{@link RestTemplate#getRequestFactory() ClientHttpRequestFactory}</li>
  211 | 	 * <li>{@link RestTemplate#getMessageConverters() HttpMessageConverters}</li>
  212 | 	 * <li>{@link RestTemplate#getInterceptors() ClientHttpRequestInterceptors}</li>
  213 | 	 * <li>{@link RestTemplate#getClientHttpRequestInitializers() ClientHttpRequestInitializers}</li>
  214 | 	 * <li>{@link RestTemplate#getUriTemplateHandler() UriBuilderFactory}</li>
  215 | 	 * <li>{@linkplain RestTemplate#getErrorHandler() error handler}</li>
  216 | 	 * </ul>
  217 | 	 * @param restTemplate the rest template to base the returned builder's
  218 | 	 * configuration on
  219 | 	 * @return a {@code RestClient} builder initialized with {@code restTemplate}'s
  220 | 	 * configuration
  221 | 	 */
  222 | 	@SuppressWarnings("removal")
  223 | 	static RestClient.Builder builder(RestTemplate restTemplate) {
  224 | 		return new DefaultRestClientBuilder(restTemplate);
  225 | 	}
  226 | 
  227 | 
  228 | 	/**
  229 | 	 * A mutable builder for creating a {@link RestClient}.
  230 | 	 */
  231 | 	interface Builder {
  232 | 
  233 | 		/**
  234 | 		 * Configure a base URL for requests. Effectively a shortcut for:
  235 | 		 * <pre class="code">
  236 | 		 * String baseUrl = "https://abc.go.com/v1";
  237 | 		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
  238 | 		 * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
  239 | 		 * </pre>
  240 | 		 * <p>The {@code DefaultUriBuilderFactory} is used to prepare the URL
  241 | 		 * for every request with the given base URL, unless the URL request
  242 | 		 * for a given URL is absolute in which case the base URL is ignored.
  243 | 		 * <p><strong>Note:</strong> this method is mutually exclusive with
  244 | 		 * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
  245 | 		 * {@code baseUrl} value provided here will be ignored.
  246 | 		 * @return this builder
  247 | 		 * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
  248 | 		 * @see #uriBuilderFactory(UriBuilderFactory)
  249 | 		 */
  250 | 		Builder baseUrl(String baseUrl);
  251 | 
  252 | 		/**
  253 | 		 * Configure a base {@code URI} for requests. Effectively a shortcut for:
  254 | 		 * <pre class="code">
  255 | 		 * URI baseUrl = URI.create("https://abc.go.com/v1");
  256 | 		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl.toString());
  257 | 		 * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
  258 | 		 * </pre>
  259 | 		 * <p>The {@code DefaultUriBuilderFactory} is used to prepare the URL
  260 | 		 * for every request with the given base URL, unless the URL request
  261 | 		 * for a given URL is absolute in which case the base URL is ignored.
  262 | 		 * <p><strong>Note:</strong> this method is mutually exclusive with
  263 | 		 * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
  264 | 		 * {@code baseUrl} value provided here will be ignored.
  265 | 		 * @return this builder
  266 | 		 * @since 6.2
  267 | 		 * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
  268 | 		 * @see #uriBuilderFactory(UriBuilderFactory)
  269 | 		 */
  270 | 		Builder baseUrl(URI baseUrl);
  271 | 
  272 | 		/**
  273 | 		 * Configure default URL variable values to use when expanding URI
  274 | 		 * templates with a {@link Map}. Effectively a shortcut for:
  275 | 		 * <pre class="code">
  276 | 		 * Map&lt;String, ?&gt; defaultVars = ...;
  277 | 		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
  278 | 		 * factory.setDefaultVariables(defaultVars);
  279 | 		 * RestClient client = RestClient.builder().uriBuilderFactory(factory).build();
  280 | 		 * </pre>
  281 | 		 * <p><strong>Note:</strong> this method is mutually exclusive with
  282 | 		 * {@link #uriBuilderFactory(UriBuilderFactory)}. If both are used, the
  283 | 		 * {@code defaultUriVariables} value provided here will be ignored.
  284 | 		 * @return this builder
  285 | 		 * @see DefaultUriBuilderFactory#setDefaultUriVariables(Map)
  286 | 		 * @see #uriBuilderFactory(UriBuilderFactory)
  287 | 		 */
  288 | 		Builder defaultUriVariables(Map<String, ?> defaultUriVariables);
  289 | 
  290 | 		/**
  291 | 		 * Provide a pre-configured {@link UriBuilderFactory} instance. This is
  292 | 		 * an alternative to, and effectively overrides the following shortcut
  293 | 		 * properties:
  294 | 		 * <ul>
  295 | 		 * <li>{@link #baseUrl(String)}
  296 | 		 * <li>{@link #defaultUriVariables(Map)}.
  297 | 		 * </ul>
  298 | 		 * @param uriBuilderFactory the URI builder factory to use
  299 | 		 * @return this builder
  300 | 		 * @see #baseUrl(String)
  301 | 		 * @see #defaultUriVariables(Map)
  302 | 		 */
  303 | 		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);
  304 | 
  305 | 		/**
  306 | 		 * Global option to specify a header to be added to every request,
  307 | 		 * if the request does not already contain such a header.
  308 | 		 * @param header the header name
  309 | 		 * @param values the header values
  310 | 		 * @return this builder
  311 | 		 */
  312 | 		Builder defaultHeader(String header, String... values);
  313 | 
  314 | 		/**
  315 | 		 * Provide a consumer to access to every {@linkplain #defaultHeader(String, String...)
  316 | 		 * default header} declared so far, with the possibility to add, replace, or remove.
  317 | 		 * @param headersConsumer the consumer
  318 | 		 * @return this builder
  319 | 		 */
  320 | 		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);
  321 | 
  322 | 		/**
  323 | 		 * Global option to specify a cookie to be added to every request,
  324 | 		 * if the request does not already contain such a cookie.
  325 | 		 * @param cookie the cookie name
  326 | 		 * @param values the cookie values
  327 | 		 * @since 6.2
  328 | 		 */
  329 | 		Builder defaultCookie(String cookie, String... values);
  330 | 
  331 | 		/**
  332 | 		 * Provides access to every {@link #defaultCookie(String, String...)}
  333 | 		 * declared so far with the possibility to add, replace, or remove.
  334 | 		 * @param cookiesConsumer a function that consumes the cookies map
  335 | 		 * @since 6.2
  336 | 		 */
  337 | 		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);
  338 | 
  339 | 		/**
  340 | 		 * Global option to specify an API version to be added to every request,
  341 | 		 * if not explicitly set.
  342 | 		 * @param version the version to use
  343 | 		 * @return this builder
  344 | 		 * @since 7.0
  345 | 		 */
  346 | 		Builder defaultApiVersion(Object version);
  347 | 
  348 | 		/**
  349 | 		 * Configure an {@link ApiVersionInserter} to abstract how an API version
  350 | 		 * specified via {@link RequestHeadersSpec#apiVersion(Object)}
  351 | 		 * is inserted into the request.
  352 | 		 * <p>{@code ApiVersionInserter} exposes shortcut methods for several
  353 | 		 * built-in inserter implementation types. See the class-level Javadoc
  354 | 		 * of {@link ApiVersionInserter} for a list of choices.
  355 | 		 * @param apiVersionInserter the inserter to use
  356 | 		 * @since 7.0
  357 | 		 */
  358 | 		Builder apiVersionInserter(@Nullable ApiVersionInserter apiVersionInserter);
  359 | 
  360 | 		/**
  361 | 		 * Provide a consumer to customize every request being built.
  362 | 		 * @param defaultRequest the consumer to use for modifying requests
  363 | 		 * @return this builder
  364 | 		 */
  365 | 		Builder defaultRequest(Consumer<RequestHeadersSpec<?>> defaultRequest);
  366 | 
  367 | 		/**
  368 | 		 * Register a default
  369 | 		 * {@linkplain ResponseSpec#onStatus(Predicate, ResponseSpec.ErrorHandler) status handler}
  370 | 		 * to apply to every response. Such default handlers are applied in the
  371 | 		 * order in which they are registered, and after any others that are
  372 | 		 * registered for a specific response.
  373 | 		 * @param statusPredicate to match responses with
  374 | 		 * @param errorHandler handler that typically, though not necessarily,
  375 | 		 * throws an exception
  376 | 		 * @return this builder
  377 | 		 */
  378 | 		Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate,
  379 | 						ResponseSpec.ErrorHandler errorHandler);
  380 | 
  381 | 		/**
  382 | 		 * Variant of {@link #defaultStatusHandler(Predicate, ResponseSpec.ErrorHandler)}
  383 | 		 * that allows use of {@code RestTemplate}'s {@code ResponseErrorHandler}
  384 | 		 * mechanism. This is provided mainly to assist {@code RestTemplate}
  385 | 		 * users to transition to {@link RestClient}. Internally, the given
  386 | 		 * handler is adapted to {@link RestClient.ResponseSpec}, which is the
  387 | 		 * preferred mechanism to use.
  388 | 		 * @param errorHandler the error handler to configure, internally adapted
  389 | 		 * and integrated into the {@link ResponseSpec.ErrorHandler} chain.
  390 | 		 * @return this builder
  391 | 		 * @deprecated as of 7.1 in favor of {@link #defaultStatusHandler(Predicate, ResponseSpec.ErrorHandler)}
  392 | 		 */
  393 | 		@Deprecated(since = "7.1", forRemoval = true)
  394 | 		@SuppressWarnings("removal")
  395 | 		Builder defaultStatusHandler(ResponseErrorHandler errorHandler);
  396 | 
  397 | 		/**
  398 | 		 * Add the given request interceptor to the end of the interceptor chain.
  399 | 		 * @param interceptor the interceptor to be added to the chain
  400 | 		 * @return this builder
  401 | 		 */
  402 | 		Builder requestInterceptor(ClientHttpRequestInterceptor interceptor);
  403 | 
  404 | 		/**
  405 | 		 * Manipulate the interceptors with the given consumer. The list provided to
  406 | 		 * the consumer is "live", so that the consumer can be used to remove
  407 | 		 * interceptors, change ordering, etc.
  408 | 		 * @param interceptorsConsumer a function that consumes the interceptors list
  409 | 		 * @return this builder
  410 | 		 */
  411 | 		Builder requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer);
  412 | 
  413 | 		/**
  414 | 		 * Enable buffering of request and response, aggregating all content before
  415 | 		 * it is sent, and making it possible to read the response body repeatedly.
  416 | 		 * @param predicate to determine whether to buffer for the given request
  417 | 		 * @return this builder
  418 | 		 * @since 7.0
  419 | 		 */
  420 | 		Builder bufferContent(BiPredicate<URI, HttpMethod> predicate);
  421 | 
  422 | 		/**
  423 | 		 * Add the given request initializer to the end of the initializer chain.
  424 | 		 * @param initializer the initializer to be added to the chain
  425 | 		 * @return this builder
  426 | 		 */
  427 | 		Builder requestInitializer(ClientHttpRequestInitializer initializer);
  428 | 
  429 | 		/**
  430 | 		 * Manipulate the initializers with the given consumer. The list provided to
  431 | 		 * the consumer is "live", so that the consumer can be used to remove
  432 | 		 * initializers, change ordering, etc.
  433 | 		 * @param initializersConsumer a function that consumes the initializers list
  434 | 		 * @return this builder
  435 | 		 */
  436 | 		Builder requestInitializers(Consumer<List<ClientHttpRequestInitializer>> initializersConsumer);
  437 | 
  438 | 		/**
  439 | 		 * Configure the {@link ClientHttpRequestFactory} to use. This is useful
  440 | 		 * for plugging in and/or customizing options of the underlying HTTP
  441 | 		 * client library (for example, SSL).
  442 | 		 * <p>If no request factory is specified, {@code RestClient} uses
  443 | 		 * {@linkplain org.springframework.http.client.HttpComponentsClientHttpRequestFactory Apache Http Client},
  444 | 		 * {@linkplain org.springframework.http.client.JettyClientHttpRequestFactory Jetty Http Client}
  445 | 		 * if available on the classpath, and defaults to the
  446 | 		 * {@linkplain org.springframework.http.client.JdkClientHttpRequestFactory JDK HttpClient}
  447 | 		 * if the {@code java.net.http} module is loaded, or to a
  448 | 		 * {@linkplain org.springframework.http.client.SimpleClientHttpRequestFactory simple default}
  449 | 		 * otherwise.
  450 | 		 * @param requestFactory the request factory to use
  451 | 		 * @return this builder
  452 | 		 */
  453 | 		Builder requestFactory(ClientHttpRequestFactory requestFactory);
  454 | 
  455 | 		/**
  456 | 		 * Configure the message converters for the {@code RestClient} to use.
  457 | 		 * Multiple consumers are composed together and applied to a single
  458 | 		 * {@link HttpMessageConverters.ClientBuilder} instance.
  459 | 		 * <p>As of 7.1, the builder is initialized with
  460 | 		 * {@link HttpMessageConverters.ClientBuilder#registerDefaults() default
  461 | 		 * converters}, unless converters were provided via the deprecated
  462 | 		 * {@link #messageConverters(Iterable)} methods. Use
  463 | 		 * {@link HttpMessageConverters.ClientBuilder#disableDefaults()} to only
  464 | 		 * use the converters configured explicitly.
  465 | 		 * @param configurer the configurer to apply on the
  466 | 		 * {@link HttpMessageConverters.ClientBuilder}
  467 | 		 * @return this builder
  468 | 		 * @since 7.0
  469 | 		 */
  470 | 		Builder configureMessageConverters(Consumer<HttpMessageConverters.ClientBuilder> configurer);
  471 | 
  472 | 		/**
  473 | 		 * Set the message converters to use.
  474 | 		 * <p><strong>Note:</strong> As of 7.0, the converters provided here
  475 | 		 * populate a {@link HttpMessageConverters.ClientBuilder} initially, and
  476 | 		 * after that the same builder is initialized further through the
  477 | 		 * configurers provided via {@link #configureMessageConverters(Consumer)}.
  478 | 		 * @param messageConverters the converters to use
  479 | 		 * @return this builder
  480 | 		 * @since 6.2
  481 | 		 * @deprecated since 7.0 in favor of {@link #configureMessageConverters(Consumer)}
  482 | 		 */
  483 | 		@Deprecated(since = "7.0", forRemoval = true)
  484 | 		Builder messageConverters(Iterable<HttpMessageConverter<?>> messageConverters);
  485 | 
  486 | 		/**
  487 | 		 * Customize the message converters to use, which is either the default
  488 | 		 * converters, or those provided via {@link #messageConverters(Iterable)}.
  489 | 		 * The consumer is applied immediately to the internal list
  490 | 		 * <p><strong>Note:</strong> As of 7.0, the list of converters customized
  491 | 		 * here is used to populate a {@link HttpMessageConverters.ClientBuilder}
  492 | 		 * initially, and after that the same builder is initialized further
  493 | 		 * through the configurers provided via {@link #configureMessageConverters(Consumer)}.
  494 | 		 * @param configurer the configurer to apply on the list of default
  495 | 		 * {@link HttpMessageConverter} pre-initialized
  496 | 		 * @return this builder
  497 | 		 * @deprecated since 7.0 in favor of {@link #configureMessageConverters(Consumer)}
  498 | 		 */
  499 | 		@Deprecated(since = "7.0", forRemoval = true)
  500 | 		Builder messageConverters(Consumer<List<HttpMessageConverter<?>>> configurer);
  501 | 
  502 | 		/**
  503 | 		 * Configure the {@link io.micrometer.observation.ObservationRegistry} to use
  504 | 		 * for recording HTTP client observations.
  505 | 		 * @param observationRegistry the observation registry to use
  506 | 		 * @return this builder
  507 | 		 */
  508 | 		Builder observationRegistry(ObservationRegistry observationRegistry);
  509 | 
  510 | 		/**
  511 | 		 * Configure the {@link io.micrometer.observation.ObservationConvention} to use
  512 | 		 * for collecting metadata for the request observation. Will use
  513 | 		 * {@link org.springframework.http.client.observation.DefaultClientRequestObservationConvention}
  514 | 		 * if none provided.
  515 | 		 * @param observationConvention the observation convention to use
  516 | 		 * @return this builder
  517 | 		 */
  518 | 		Builder observationConvention(ClientRequestObservationConvention observationConvention);
  519 | 
  520 | 		/**
  521 | 		 * Apply the given {@code Consumer} to this builder instance.
  522 | 		 * <p>This can be useful for applying pre-packaged customizations.
  523 | 		 * @param builderConsumer the consumer to apply
  524 | 		 * @return this builder
  525 | 		 */
  526 | 		Builder apply(Consumer<Builder> builderConsumer);
  527 | 
  528 | 		/**
  529 | 		 * Clone this {@code RestClient.Builder}.
  530 | 		 */
  531 | 		Builder clone();
  532 | 
  533 | 		/**
  534 | 		 * Build the {@link RestClient} instance.
  535 | 		 */
  536 | 		RestClient build();
  537 | 	}
  538 | 
  539 | 
  540 | 	/**
  541 | 	 * Contract for specifying the URI for a request.
  542 | 	 *
  543 | 	 * @param <S> a self reference to the spec type
  544 | 	 */
  545 | 	interface UriSpec<S extends RequestHeadersSpec<?>> {
  546 | 
  547 | 		/**
  548 | 		 * Specify the URI using a fully constructed {@link URI}.
  549 | 		 * <p>If the given URI is absolute, it is used as given. If it is
  550 | 		 * a relative URI, the {@link UriBuilderFactory} configured for
  551 | 		 * the client (for example, with a base URI) will be used to
  552 | 		 * {@linkplain URI#resolve(URI) resolve} the given URI against.
  553 | 		 */
  554 | 		S uri(URI uri);
  555 | 
  556 | 		/**
  557 | 		 * Specify the URI for the request using a URI template and URI variables.
  558 | 		 * <p>If a {@link UriBuilderFactory} was configured for the client (for example,
  559 | 		 * with a base URI) it will be used to expand the URI template.
  560 | 		 */
  561 | 		S uri(String uri, @Nullable Object... uriVariables);
  562 | 
  563 | 		/**
  564 | 		 * Specify the URI for the request using a URI template and URI variables.
  565 | 		 * <p>If a {@link UriBuilderFactory} was configured for the client (for example,
  566 | 		 * with a base URI) it will be used to expand the URI template.
  567 | 		 */
  568 | 		S uri(String uri, Map<String, ? extends @Nullable Object> uriVariables);
  569 | 
  570 | 		/**
  571 | 		 * Specify the URI starting with a URI template and finishing off with a
  572 | 		 * {@link UriBuilder} created from the template.
  573 | 		 */
  574 | 		S uri(String uri, Function<UriBuilder, URI> uriFunction);
  575 | 
  576 | 		/**
  577 | 		 * Specify the URI by through a {@link UriBuilder}.
  578 | 		 * @see #uri(String, Function)
  579 | 		 */
  580 | 		S uri(Function<UriBuilder, URI> uriFunction);
  581 | 	}
  582 | 
  583 | 
  584 | 	/**
  585 | 	 * Contract for specifying request headers leading up to the exchange.
  586 | 	 *
  587 | 	 * @param <S> a self reference to the spec type
  588 | 	 */
  589 | 	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {
  590 | 
  591 | 		/**
  592 | 		 * Set the list of acceptable {@linkplain MediaType media types}, as
  593 | 		 * specified by the {@code Accept} header.
  594 | 		 * @param acceptableMediaTypes the acceptable media types
  595 | 		 * @return this builder
  596 | 		 */
  597 | 		S accept(MediaType... acceptableMediaTypes);
  598 | 
  599 | 		/**
  600 | 		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
  601 | 		 * by the {@code Accept-Charset} header.
  602 | 		 * @param acceptableCharsets the acceptable charsets
  603 | 		 * @return this builder
  604 | 		 */
  605 | 		S acceptCharset(Charset... acceptableCharsets);
  606 | 
  607 | 		/**
  608 | 		 * Add a cookie with the given name and value.
  609 | 		 * @param name the cookie name
  610 | 		 * @param value the cookie value
  611 | 		 * @return this builder
  612 | 		 * @since 6.2
  613 | 		 */
  614 | 		S cookie(String name, String value);
  615 | 
  616 | 		/**
  617 | 		 * Provides access to every cookie declared so far with the possibility
  618 | 		 * to add, replace, or remove values.
  619 | 		 * @param cookiesConsumer the consumer to provide access to
  620 | 		 * @return this builder
  621 | 		 * @since 6.2
  622 | 		 */
  623 | 		S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);
  624 | 
  625 | 		/**
  626 | 		 * Set the value of the {@code If-Modified-Since} header.
  627 | 		 * @param ifModifiedSince the new value of the header
  628 | 		 * @return this builder
  629 | 		 */
  630 | 		S ifModifiedSince(ZonedDateTime ifModifiedSince);
  631 | 
  632 | 		/**
  633 | 		 * Set the values of the {@code If-None-Match} header.
  634 | 		 * @param ifNoneMatches the new value of the header
  635 | 		 * @return this builder
  636 | 		 */
  637 | 		S ifNoneMatch(String... ifNoneMatches);
  638 | 
  639 | 		/**
  640 | 		 * Add the given, single header value under the given name.
  641 | 		 * @param headerName  the header name
  642 | 		 * @param headerValues the header value(s)
  643 | 		 * @return this builder
  644 | 		 */
  645 | 		S header(String headerName, String... headerValues);
  646 | 
  647 | 		/**
  648 | 		 * Provides access to every header declared so far with the possibility
  649 | 		 * to add, replace, or remove values.
  650 | 		 * @param headersConsumer the consumer to provide access to
  651 | 		 * @return this builder
  652 | 		 */
  653 | 		S headers(Consumer<HttpHeaders> headersConsumer);
  654 | 
  655 | 		/**
  656 | 		 * Set an API version for the request. The version is inserted into the
  657 | 		 * request through the {@link Builder#apiVersionInserter(ApiVersionInserter)
  658 | 		 * configured} {@code ApiVersionInserter}.
  659 | 		 * <p>If no version is set, the
  660 | 		 * {@link Builder#defaultApiVersion(Object) defaultApiVersion} is used,
  661 | 		 * if configured.
  662 | 		 * <p>If {@code null} is passed, then an API version is not inserted
  663 | 		 * irrespective of default version settings.
  664 | 		 * @param version the API version for the request; this can be a String
  665 | 		 * or some Object that can be formatted the inserter, e.g. through an
  666 | 		 * {@link ApiVersionFormatter}.
  667 | 		 * @since 7.0
  668 | 		 */
  669 | 		S apiVersion(@Nullable Object version);
  670 | 
  671 | 		/**
  672 | 		 * Set the attribute with the given name to the given value.
  673 | 		 * @param name the name of the attribute to add
  674 | 		 * @param value the value of the attribute to add
  675 | 		 * @return this builder
  676 | 		 * @since 6.2
  677 | 		 */
  678 | 		S attribute(String name, Object value);
  679 | 
  680 | 		/**
  681 | 		 * Provides access to every attribute declared so far with the
  682 | 		 * possibility to add, replace, or remove values.
  683 | 		 * @param attributesConsumer the consumer to provide access to
  684 | 		 * @return this builder
  685 | 		 * @since 6.2
  686 | 		 */
  687 | 		S attributes(Consumer<Map<String, Object>> attributesConsumer);
  688 | 
  689 | 		/**
  690 | 		 * Callback for access to the {@link ClientHttpRequest} that in turn
  691 | 		 * provides access to the native request of the underlying HTTP library.
  692 | 		 * <p>This could be useful for setting advanced, per-request options that
  693 | 		 * are exposed by the underlying library.
  694 | 		 * @param requestConsumer a consumer to access the
  695 | 		 * {@code ClientHttpRequest} with
  696 | 		 * @return this builder
  697 | 		 */
  698 | 		S httpRequest(Consumer<ClientHttpRequest> requestConsumer);
  699 | 
  700 | 		/**
  701 | 		 * Enter the retrieve workflow and use the returned {@link ResponseSpec}
  702 | 		 * to select from a number of built-in options to extract the response.
  703 | 		 * For example:
  704 | 		 *
  705 | 		 * <pre class="code">
  706 | 		 * ResponseEntity&lt;Person&gt; entity = client.get()
  707 | 		 *     .uri("/persons/1")
  708 | 		 *     .accept(MediaType.APPLICATION_JSON)
  709 | 		 *     .retrieve()
  710 | 		 *     .toEntity(Person.class);
  711 | 		 * </pre>
  712 | 		 * <p>Or if interested only in the body:
  713 | 		 * <pre class="code">
  714 | 		 * Person person = client.get()
  715 | 		 *     .uri("/persons/1")
  716 | 		 *     .accept(MediaType.APPLICATION_JSON)
  717 | 		 *     .retrieve()
  718 | 		 *     .body(Person.class);
  719 | 		 * </pre>
  720 | 		 * Note that this method does not actually execute the request until you
  721 | 		 * call one of the returned {@link ResponseSpec}. Use the
  722 | 		 * {@link #exchange(ExchangeFunction)} variants if you need to separate
  723 | 		 * request execution from response extraction.
  724 | 		 * <p>By default, 4xx response code result in a
  725 | 		 * {@link HttpClientErrorException} and 5xx response codes in a
  726 | 		 * {@link HttpServerErrorException}. To customize error handling, use
  727 | 		 * {@link ResponseSpec#onStatus(Predicate, ResponseSpec.ErrorHandler) onStatus} handlers.
  728 | 		 * @return {@code ResponseSpec} to specify how to decode the body
  729 | 		 */
  730 | 		@CheckReturnValue
  731 | 		ResponseSpec retrieve();
  732 | 
  733 | 		/**
  734 | 		 * Exchange the {@link ClientHttpResponse} for a value of type {@code T}.
  735 | 		 * This can be useful for advanced scenarios, for example to decode the
  736 | 		 * response differently depending on the response status:
  737 | 		 * <pre class="code">
  738 | 		 * Person person = client.get()
  739 | 		 *     .uri("/people/1")
  740 | 		 *     .accept(MediaType.APPLICATION_JSON)
  741 | 		 *     .exchange((request, response) -&gt; {
  742 | 		 *         if (response.getStatusCode().equals(HttpStatus.OK)) {
  743 | 		 *             return deserialize(response.getBody());
  744 | 		 *         }
  745 | 		 *         else {
  746 | 		 *             throw new BusinessException();
  747 | 		 *         }
  748 | 		 *     });
  749 | 		 * </pre>
  750 | 		 * <p><strong>Note:</strong> The response is
  751 | 		 * {@linkplain ClientHttpResponse#close() closed} after the exchange
  752 | 		 * function has been invoked.
  753 | 		 * @param exchangeFunction the function to handle the response with
  754 | 		 * @param <T> the type the response will be transformed to
  755 | 		 * @return the value returned from the exchange function, potentially {@code null}
  756 | 		 * @see RequestHeadersSpec#exchangeForRequiredValue(RequiredValueExchangeFunction)
  757 | 		 */
  758 | 		default <T extends @Nullable Object> T exchange(ExchangeFunction<T> exchangeFunction) {
  759 | 			return exchange(exchangeFunction, true);
  760 | 		}
  761 | 
  762 | 		/**
  763 | 		 * Exchange the {@link ClientHttpResponse} for a value of type {@code T}.
  764 | 		 * This can be useful for advanced scenarios, for example to decode the
  765 | 		 * response differently depending on the response status:
  766 | 		 * <pre class="code">
  767 | 		 * Person person = client.get()
  768 | 		 *     .uri("/people/1")
  769 | 		 *     .accept(MediaType.APPLICATION_JSON)
  770 | 		 *     .exchange((request, response) -&gt; {
  771 | 		 *         if (response.getStatusCode().equals(HttpStatus.OK)) {
  772 | 		 *             return deserialize(response.getBody());
  773 | 		 *         }
  774 | 		 *         else {
  775 | 		 *             throw new BusinessException();
  776 | 		 *         }
  777 | 		 *     });
  778 | 		 * </pre>
  779 | 		 * <p><strong>Note:</strong> The response is
  780 | 		 * {@linkplain ClientHttpResponse#close() closed} after the exchange
  781 | 		 * function has been invoked.
  782 | 		 * @param exchangeFunction the function to handle the response with
  783 | 		 * @param <T> the type the response will be transformed to
  784 | 		 * @return the value returned from the exchange function, never {@code null}
  785 | 		 * @since 6.2.6
  786 | 		 */
  787 | 		default <T> T exchangeForRequiredValue(RequiredValueExchangeFunction<T> exchangeFunction) {
  788 | 			return exchangeForRequiredValue(exchangeFunction, true);
  789 | 		}
  790 | 
  791 | 		/**
  792 | 		 * Exchange the {@link ClientHttpResponse} for a value of type {@code T}.
  793 | 		 * This can be useful for advanced scenarios, for example to decode the
  794 | 		 * response differently depending on the response status:
  795 | 		 * <pre class="code">
  796 | 		 * Person person = client.get()
  797 | 		 *     .uri("/people/1")
  798 | 		 *     .accept(MediaType.APPLICATION_JSON)
  799 | 		 *     .exchange((request, response) -&gt; {
  800 | 		 *         if (response.getStatusCode().equals(HttpStatus.OK)) {
  801 | 		 *             return deserialize(response.getBody());
  802 | 		 *         }
  803 | 		 *         else {
  804 | 		 *             throw new BusinessException();
  805 | 		 *         }
  806 | 		 *     });
  807 | 		 * </pre>
  808 | 		 * <p><strong>Note:</strong> If {@code close} is {@code true},
  809 | 		 * then the response is {@linkplain ClientHttpResponse#close() closed}
  810 | 		 * after the exchange function has been invoked. When set to
  811 | 		 * {@code false}, the caller is responsible for closing the response.
  812 | 		 * @param exchangeFunction the function to handle the response with
  813 | 		 * @param close {@code true} to close the response after
  814 | 		 * {@code exchangeFunction} is invoked, {@code false} to keep it open
  815 | 		 * @param <T> the type the response will be transformed to
  816 | 		 * @return the value returned from the exchange function, potentially {@code null}
  817 | 		 * @see RequestHeadersSpec#exchangeForRequiredValue(RequiredValueExchangeFunction, boolean)
  818 | 		 */
  819 | 		<T extends @Nullable Object> T exchange(ExchangeFunction<T> exchangeFunction, boolean close);
  820 | 
  821 | 		/**
  822 | 		 * Exchange the {@link ClientHttpResponse} for a value of type {@code T}.
  823 | 		 * This can be useful for advanced scenarios, for example to decode the
  824 | 		 * response differently depending on the response status:
  825 | 		 * <pre class="code">
  826 | 		 * Person person = client.get()
  827 | 		 *     .uri("/people/1")
  828 | 		 *     .accept(MediaType.APPLICATION_JSON)
  829 | 		 *     .exchange((request, response) -&gt; {
  830 | 		 *         if (response.getStatusCode().equals(HttpStatus.OK)) {
  831 | 		 *             return deserialize(response.getBody());
  832 | 		 *         }
  833 | 		 *         else {
  834 | 		 *             throw new BusinessException();
  835 | 		 *         }
  836 | 		 *     });
  837 | 		 * </pre>
  838 | 		 * <p><strong>Note:</strong> If {@code close} is {@code true},
  839 | 		 * then the response is {@linkplain ClientHttpResponse#close() closed}
  840 | 		 * after the exchange function has been invoked. When set to
  841 | 		 * {@code false}, the caller is responsible for closing the response.
  842 | 		 * @param exchangeFunction the function to handle the response with
  843 | 		 * @param close {@code true} to close the response after
  844 | 		 * {@code exchangeFunction} is invoked, {@code false} to keep it open
  845 | 		 * @param <T> the type the response will be transformed to
  846 | 		 * @return the value returned from the exchange function, never {@code null}
  847 | 		 * @since 6.2.6
  848 | 		 */
  849 | 		<T> T exchangeForRequiredValue(RequiredValueExchangeFunction<T> exchangeFunction, boolean close);
  850 | 
  851 | 
  852 | 		/**
  853 | 		 * Defines the contract for {@link #exchange(ExchangeFunction)}.
  854 | 		 *
  855 | 		 * @param <T> the type the response will be transformed to
  856 | 		 */
  857 | 		@FunctionalInterface
  858 | 		interface ExchangeFunction<T extends @Nullable Object> {
  859 | 
  860 | 			/**
  861 | 			 * Exchange the given response into a value of type {@code T}.
  862 | 			 * @param clientRequest the request
  863 | 			 * @param clientResponse the response
  864 | 			 * @return the exchanged value, potentially {@code null}
  865 | 			 * @throws IOException in case of I/O errors
  866 | 			 */
  867 | 			T exchange(HttpRequest clientRequest, ConvertibleClientHttpResponse clientResponse) throws IOException;
  868 | 		}
  869 | 
  870 | 		/**
  871 | 		 * Variant of {@link ExchangeFunction} returning a non-null required value.
  872 | 		 *
  873 | 		 * @since 6.2.6
  874 | 		 * @param <T> the type the response will be transformed to
  875 | 		 */
  876 | 		@FunctionalInterface
  877 | 		interface RequiredValueExchangeFunction<T> extends ExchangeFunction<@NonNull T> {
  878 | 
  879 | 			/**
  880 | 			 * Exchange the given response into a value of type {@code T}.
  881 | 			 * @param clientRequest the request
  882 | 			 * @param clientResponse the response
  883 | 			 * @return the exchanged value, never {@code null}
  884 | 			 * @throws IOException in case of I/O errors
  885 | 			 */
  886 | 			@Override
  887 | 			T exchange(HttpRequest clientRequest, ConvertibleClientHttpResponse clientResponse) throws IOException;
  888 | 		}
  889 | 
  890 | 		/**
  891 | 		 * Extension of {@link ClientHttpResponse} that can convert the body.
  892 | 		 */
  893 | 		interface ConvertibleClientHttpResponse extends ClientHttpResponse {
  894 | 
  895 | 			/**
  896 | 			 * Extract the response body as an object of the given type.
  897 | 			 * @param bodyType the type of return value
  898 | 			 * @param <T> the body type
  899 | 			 * @return the body, or {@code null} if no response body was available
  900 | 			 */
  901 | 			<T> @Nullable T bodyTo(Class<T> bodyType);
  902 | 
  903 | 			/**
  904 | 			 * Extract the response body as an object of the given type.
  905 | 			 * @param bodyType the type of return value
  906 | 			 * @param <T> the body type
  907 | 			 * @return the body, or {@code null} if no response body was available
  908 | 			 */
  909 | 			<T> @Nullable T bodyTo(ParameterizedTypeReference<T> bodyType);
  910 | 
  911 | 			/**
  912 | 			 * Create a {@link RestClientResponseException} of the appropriate
  913 | 			 * subtype depending on the response status code. The exception contains
  914 | 			 * the status, headers, and body of the response.
  915 | 			 * @throws IOException in case of a response failure (e.g. to obtain the status)
  916 | 			 * @since 7.0
  917 | 			 */
  918 | 			RestClientResponseException createException() throws IOException;
  919 | 		}
  920 | 	}
  921 | 
  922 | 
  923 | 	/**
  924 | 	 * Contract for specifying request headers and body leading up to the exchange.
  925 | 	 */
  926 | 	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {
  927 | 
  928 | 		/**
  929 | 		 * Set the length of the body in bytes, as specified by the
  930 | 		 * {@code Content-Length} header.
  931 | 		 * @param contentLength the content length
  932 | 		 * @return this builder
  933 | 		 * @see HttpHeaders#setContentLength(long)
  934 | 		 */
  935 | 		RequestBodySpec contentLength(long contentLength);
  936 | 
  937 | 		/**
  938 | 		 * Set the {@linkplain MediaType media type} of the body, as specified
  939 | 		 * by the {@code Content-Type} header.
  940 | 		 * @param contentType the content type
  941 | 		 * @return this builder
  942 | 		 * @see HttpHeaders#setContentType(MediaType)
  943 | 		 */
  944 | 		RequestBodySpec contentType(MediaType contentType);
  945 | 
  946 | 		/**
  947 | 		 * Set the body of the request to the given {@code Object}.
  948 | 		 * For example:
  949 | 		 * <pre class="code">
  950 | 		 * Person person = ... ;
  951 | 		 * ResponseEntity&lt;Void&gt; response = client.post()
  952 | 		 *     .uri("/persons/{id}", id)
  953 | 		 *     .contentType(MediaType.APPLICATION_JSON)
  954 | 		 *     .body(person)
  955 | 		 *     .retrieve()
  956 | 		 *     .toBodilessEntity();
  957 | 		 * </pre>
  958 | 		 * @param body the body of the request
  959 | 		 * @return this builder
  960 | 		 */
  961 | 		RequestBodySpec body(Object body);
  962 | 
  963 | 		/**
  964 | 		 * Set the body of the request to the given {@code Object}.
  965 | 		 * The parameter {@code bodyType} is used to capture the generic type.
  966 | 		 * @param body the body of the request
  967 | 		 * @param bodyType the type of the body, used to capture the generic type
  968 | 		 * @return this builder
  969 | 		 */
  970 | 		<T> RequestBodySpec body(T body, ParameterizedTypeReference<T> bodyType);
  971 | 
  972 | 		/**
  973 | 		 * Set the body of the request to the given function that writes to
  974 | 		 * an {@link OutputStream}.
  975 | 		 * @param body a function that takes an {@code OutputStream} and can
  976 | 		 * throw an {@code IOException}
  977 | 		 * @return this builder
  978 | 		 */
  979 | 		RequestBodySpec body(StreamingHttpOutputMessage.Body body);
  980 | 
  981 | 		/**
  982 | 		 * Set the hint with the given name to the given value for
  983 | 		 * {@link org.springframework.http.converter.SmartHttpMessageConverter}s
  984 | 		 * supporting them.
  985 | 		 * @param key the key of the hint to add
  986 | 		 * @param value the value of the hint to add
  987 | 		 * @return this builder
  988 | 		 * @since 7.0
  989 | 		 */
  990 | 		RequestBodySpec hint(String key, Object value);
  991 | 	}
  992 | 
  993 | 
  994 | 	/**
  995 | 	 * Contract for specifying response operations following the exchange.
  996 | 	 */
  997 | 	interface ResponseSpec {
  998 | 
  999 | 		/**
 1000 | 		 * Provide a function to map specific error status codes to an error handler.
 1001 | 		 * <p>By default, if there are no matching status handlers, responses with
 1002 | 		 * status codes &gt;= 400 will throw a {@link RestClientResponseException}.
 1003 | 		 * <p>Note that {@link IOException IOExceptions},
 1004 | 		 * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
 1005 | 		 * {@link org.springframework.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
 1006 | 		 * thrown from {@code errorHandler} will be wrapped in a
 1007 | 		 * {@link RestClientException}.
 1008 | 		 * @param statusPredicate to match responses with
 1009 | 		 * @param errorHandler handler that typically, though not necessarily,
 1010 | 		 * throws an exception
 1011 | 		 * @return this builder
 1012 | 		 */
 1013 | 		ResponseSpec onStatus(Predicate<HttpStatusCode> statusPredicate,
 1014 | 				ErrorHandler errorHandler);
 1015 | 
 1016 | 		/**
 1017 | 		 * Provide a function to map specific error status codes to an error handler.
 1018 | 		 * <p>By default, if there are no matching status handlers, responses with
 1019 | 		 * status codes &gt;= 400 will throw a {@link RestClientResponseException}.
 1020 | 		 * <p>Note that {@link IOException IOExceptions},
 1021 | 		 * {@link java.io.UncheckedIOException UncheckedIOExceptions}, and
 1022 | 		 * {@link org.springframework.http.converter.HttpMessageNotReadableException HttpMessageNotReadableExceptions}
 1023 | 		 * thrown from {@code errorHandler} will be wrapped in a
 1024 | 		 * {@link RestClientException}.
 1025 | 		 * @param errorHandler the error handler
 1026 | 		 * @return this builder
 1027 | 		 * @deprecated as of 7.1 in favor of {@link #onStatus(Predicate, ErrorHandler)}
 1028 | 		 */
 1029 | 		@Deprecated(since = "7.1", forRemoval = true)
 1030 | 		@SuppressWarnings("removal")
 1031 | 		ResponseSpec onStatus(ResponseErrorHandler errorHandler);
 1032 | 
 1033 | 		/**
 1034 | 		 * Extract the body as an object of the given type.
 1035 | 		 * @param bodyType the type of return value
 1036 | 		 * @param <T> the body type
 1037 | 		 * @return the body, or {@code null} if no response body was available
 1038 | 		 * @throws RestClientResponseException by default when receiving a
 1039 | 		 * response with a status code of 4xx or 5xx. Use
 1040 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1041 | 		 * handling.
 1042 | 		 * @see #requiredBody(Class)
 1043 | 		 */
 1044 | 		<T> @Nullable T body(Class<T> bodyType);
 1045 | 
 1046 | 		/**
 1047 | 		 * Extract the body as an object of the given type.
 1048 | 		 * @param bodyType the type of return value
 1049 | 		 * @param <T> the body type
 1050 | 		 * @return the body
 1051 | 		 * @throws IllegalStateException if no response body was available
 1052 | 		 * @throws RestClientResponseException by default when receiving a
 1053 | 		 * response with a status code of 4xx or 5xx. Use
 1054 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1055 | 		 * handling.
 1056 | 		 * @since 7.0.4
 1057 | 		 */
 1058 | 		<T> T requiredBody(Class<T> bodyType);
 1059 | 
 1060 | 		/**
 1061 | 		 * Extract the body as an object of the given type.
 1062 | 		 * @param bodyType the type of return value
 1063 | 		 * @param <T> the body type
 1064 | 		 * @return the body, or {@code null} if no response body was available
 1065 | 		 * @throws RestClientResponseException by default when receiving a
 1066 | 		 * response with a status code of 4xx or 5xx. Use
 1067 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1068 | 		 * handling.
 1069 | 		 * @see #requiredBody(ParameterizedTypeReference)
 1070 | 		 */
 1071 | 		<T> @Nullable T body(ParameterizedTypeReference<T> bodyType);
 1072 | 
 1073 | 		/**
 1074 | 		 * Extract the body as an object of the given type.
 1075 | 		 * @param bodyType the type of return value
 1076 | 		 * @param <T> the body type
 1077 | 		 * @return the body
 1078 | 		 * @throws IllegalStateException if no response body was available
 1079 | 		 * @throws RestClientResponseException by default when receiving a
 1080 | 		 * response with a status code of 4xx or 5xx. Use
 1081 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1082 | 		 * handling.
 1083 | 		 * @since 7.0.4
 1084 | 		 */
 1085 | 		<T> T requiredBody(ParameterizedTypeReference<T> bodyType);
 1086 | 
 1087 | 		/**
 1088 | 		 * Return a {@code ResponseEntity} with the body decoded to an Object of
 1089 | 		 * the given type.
 1090 | 		 * @param bodyType the expected response body type
 1091 | 		 * @param <T> response body type
 1092 | 		 * @return the {@code ResponseEntity} with the decoded body
 1093 | 		 * @throws RestClientResponseException by default when receiving a
 1094 | 		 * response with a status code of 4xx or 5xx. Use
 1095 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1096 | 		 * handling.
 1097 | 		 */
 1098 | 		<T> ResponseEntity<T> toEntity(Class<T> bodyType);
 1099 | 
 1100 | 		/**
 1101 | 		 * Return a {@code ResponseEntity} with the body decoded to an Object of
 1102 | 		 * the given type.
 1103 | 		 * @param bodyType the expected response body type
 1104 | 		 * @param <T> response body type
 1105 | 		 * @return the {@code ResponseEntity} with the decoded body
 1106 | 		 * @throws RestClientResponseException by default when receiving a
 1107 | 		 * response with a status code of 4xx or 5xx. Use
 1108 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1109 | 		 * handling.
 1110 | 		 */
 1111 | 		<T> ResponseEntity<T> toEntity(ParameterizedTypeReference<T> bodyType);
 1112 | 
 1113 | 		/**
 1114 | 		 * Return a {@code ResponseEntity} without a body.
 1115 | 		 * @return the {@code ResponseEntity}
 1116 | 		 * @throws RestClientResponseException by default when receiving a
 1117 | 		 * response with a status code of 4xx or 5xx. Use
 1118 | 		 * {@link #onStatus(Predicate, ErrorHandler)} to customize error response
 1119 | 		 * handling.
 1120 | 		 */
 1121 | 		ResponseEntity<Void> toBodilessEntity();
 1122 | 
 1123 | 		/**
 1124 | 		 * Set the hint with the given name to the given value for
 1125 | 		 * {@link org.springframework.http.converter.SmartHttpMessageConverter}s
 1126 | 		 * supporting them.
 1127 | 		 * @param key the key of the hint to add
 1128 | 		 * @param value the value of the hint to add
 1129 | 		 * @return this builder
 1130 | 		 * @since 7.0
 1131 | 		 */
 1132 | 		ResponseSpec hint(String key, Object value);
 1133 | 
 1134 | 
 1135 | 		/**
 1136 | 		 * Used in {@link #onStatus(Predicate, ErrorHandler)}.
 1137 | 		 */
 1138 | 		@FunctionalInterface
 1139 | 		interface ErrorHandler {
 1140 | 
 1141 | 			/**
 1142 | 			 * Handle the error in the given response.
 1143 | 			 * @param response the response with the error
 1144 | 			 * @throws IOException in case of I/O errors
 1145 | 			 */
 1146 | 			void handle(HttpRequest request, ClientHttpResponse response) throws IOException;
 1147 | 		}
 1148 | 	}
 1149 | 
 1150 | 
 1151 | 	/**
 1152 | 	 * Contract for specifying request headers and URI for a request.
 1153 | 	 *
 1154 | 	 * @param <S> a self reference to the spec type
 1155 | 	 */
 1156 | 	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>> extends UriSpec<S>, RequestHeadersSpec<S> {
 1157 | 	}
 1158 | 
 1159 | 
 1160 | 	/**
 1161 | 	 * Contract for specifying request headers, body and URI for a request.
 1162 | 	 */
 1163 | 	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
 1164 | 	}
 1165 | 
 1166 | }
```
