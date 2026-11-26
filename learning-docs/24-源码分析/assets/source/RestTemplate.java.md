<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/client/RestTemplate.java -->
<!-- 正文引用：[[源码:RestTemplate.java#L起-L止]] ；代码块头部：(src: RestTemplate.java#L起-L止) -->

# RestTemplate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/client/RestTemplate.java` |
| 行数 | 1059 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:28.798Z |

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
   20 | import java.lang.reflect.ParameterizedType;
   21 | import java.lang.reflect.Type;
   22 | import java.net.URI;
   23 | import java.util.ArrayList;
   24 | import java.util.Collections;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | import java.util.stream.Collectors;
   29 | import java.util.stream.Stream;
   30 | 
   31 | import io.micrometer.observation.Observation;
   32 | import io.micrometer.observation.ObservationConvention;
   33 | import io.micrometer.observation.ObservationRegistry;
   34 | import org.jspecify.annotations.Nullable;
   35 | 
   36 | import org.springframework.core.ParameterizedTypeReference;
   37 | import org.springframework.core.ResolvableType;
   38 | import org.springframework.http.HttpEntity;
   39 | import org.springframework.http.HttpHeaders;
   40 | import org.springframework.http.HttpMethod;
   41 | import org.springframework.http.HttpStatusCode;
   42 | import org.springframework.http.MediaType;
   43 | import org.springframework.http.RequestEntity;
   44 | import org.springframework.http.ResponseEntity;
   45 | import org.springframework.http.client.ClientHttpRequest;
   46 | import org.springframework.http.client.ClientHttpRequestFactory;
   47 | import org.springframework.http.client.ClientHttpResponse;
   48 | import org.springframework.http.client.observation.ClientHttpObservationDocumentation;
   49 | import org.springframework.http.client.observation.ClientRequestObservationContext;
   50 | import org.springframework.http.client.observation.ClientRequestObservationConvention;
   51 | import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
   52 | import org.springframework.http.client.support.InterceptingHttpAccessor;
   53 | import org.springframework.http.converter.GenericHttpMessageConverter;
   54 | import org.springframework.http.converter.HttpMessageConverter;
   55 | import org.springframework.http.converter.HttpMessageConverters;
   56 | import org.springframework.http.converter.SmartHttpMessageConverter;
   57 | import org.springframework.util.Assert;
   58 | import org.springframework.util.MimeTypeUtils;
   59 | import org.springframework.web.util.DefaultUriBuilderFactory;
   60 | import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;
   61 | import org.springframework.web.util.UriTemplateHandler;
   62 | 
   63 | /**
   64 |  * Synchronous client to perform HTTP requests, exposing a simple, template
   65 |  * method API over underlying HTTP client libraries such as the JDK
   66 |  * {@code HttpURLConnection}, Apache HttpComponents, and others. RestTemplate
   67 |  * offers templates for common scenarios by HTTP method, in addition to the
   68 |  * generalized {@code exchange} and {@code execute} methods that support
   69 |  * less frequent cases.
   70 |  *
   71 |  * <p>RestTemplate is typically used as a shared component. However, its
   72 |  * configuration does not support concurrent modification, and as such its
   73 |  * configuration is typically prepared on startup. If necessary, you can create
   74 |  * multiple, differently configured RestTemplate instances on startup. Such
   75 |  * instances may use the same underlying {@link ClientHttpRequestFactory}
   76 |  * if they need to share HTTP client resources.
   77 |  *
   78 |  * <p><strong>NOTE:</strong> As of 6.1, {@link RestClient} offers a more modern
   79 |  * API for synchronous HTTP access. For asynchronous and streaming scenarios,
   80 |  * consider the reactive
   81 |  * {@link org.springframework.web.reactive.function.client.WebClient}.
   82 |  *
   83 |  * <p>{@code RestTemplate} and {@code RestClient} share the same infrastructure
   84 |  * (i.e. {@linkplain ClientHttpRequestFactory request factories}, request
   85 |  * {@linkplain org.springframework.http.client.ClientHttpRequestInterceptor interceptors} and
   86 |  * {@linkplain org.springframework.http.client.ClientHttpRequestInitializer initializers},
   87 |  * {@linkplain HttpMessageConverter message converters},
   88 |  * etc.), so any improvements made therein are shared as well.
   89 |  * However, {@code RestClient} is the focus for new higher-level features.
   90 |  *
   91 |  * @author Arjen Poutsma
   92 |  * @author Brian Clozel
   93 |  * @author Roy Clarkson
   94 |  * @author Juergen Hoeller
   95 |  * @author Sam Brannen
   96 |  * @author Sebastien Deleuze
   97 |  * @author Hyoungjune Kim
   98 |  * @since 3.0
   99 |  * @see HttpMessageConverter
  100 |  * @see RequestCallback
  101 |  * @see ResponseExtractor
  102 |  * @see ResponseErrorHandler
  103 |  * @deprecated as of 7.1, in favor of {@link RestClient}. For removal in 8.0.
  104 |  */
  105 | @Deprecated(since = "7.1", forRemoval = true)
  106 | @SuppressWarnings("removal")
  107 | public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {
  108 | 
  109 | 	private static final ClientRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultClientRequestObservationConvention();
  110 | 
  111 | 
  112 | 	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
  113 | 
  114 | 	private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
  115 | 
  116 | 	private UriTemplateHandler uriTemplateHandler;
  117 | 
  118 | 	private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();
  119 | 
  120 | 	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
  121 | 
  122 | 	private @Nullable ClientRequestObservationConvention observationConvention;
  123 | 
  124 | 
  125 | 	/**
  126 | 	 * Create a new instance with default settings.
  127 | 	 * Default {@link HttpMessageConverter HttpMessageConverters} are initialized.
  128 | 	 */
  129 | 	public RestTemplate() {
  130 | 		this(HttpMessageConverters.forClient().registerDefaults().build());
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Create a new instance with the given {@link ClientHttpRequestFactory}.
  135 | 	 * @param requestFactory the HTTP request factory to use
  136 | 	 * @see org.springframework.http.client.SimpleClientHttpRequestFactory
  137 | 	 * @see org.springframework.http.client.HttpComponentsClientHttpRequestFactory
  138 | 	 */
  139 | 	public RestTemplate(ClientHttpRequestFactory requestFactory) {
  140 | 		this();
  141 | 		setRequestFactory(requestFactory);
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Create a new instance with the given message converters.
  146 | 	 * @param messageConverters the list of converters to use
  147 | 	 * @since 3.2.7
  148 | 	 */
  149 | 	public RestTemplate(Iterable<HttpMessageConverter<?>> messageConverters) {
  150 | 		validateConverters(messageConverters);
  151 | 		messageConverters.forEach(this.messageConverters::add);
  152 | 		this.uriTemplateHandler = initUriTemplateHandler();
  153 | 		updateErrorHandlerConverters();
  154 | 	}
  155 | 
  156 | 
  157 | 	private void updateErrorHandlerConverters() {
  158 | 		if (this.errorHandler instanceof DefaultResponseErrorHandler handler) {
  159 | 			handler.setMessageConverters(this.messageConverters);
  160 | 		}
  161 | 	}
  162 | 
  163 | 	private static DefaultUriBuilderFactory initUriTemplateHandler() {
  164 | 		DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
  165 | 		uriFactory.setEncodingMode(EncodingMode.URI_COMPONENT);  // for backwards compatibility..
  166 | 		return uriFactory;
  167 | 	}
  168 | 
  169 | 
  170 | 	/**
  171 | 	 * Set the message body converters to use.
  172 | 	 * <p>These converters are used to convert from and to HTTP requests and responses.
  173 | 	 */
  174 | 	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
  175 | 		validateConverters(messageConverters);
  176 | 		// Take getMessageConverters() List as-is when passed in here
  177 | 		if (this.messageConverters != messageConverters) {
  178 | 			this.messageConverters.clear();
  179 | 			this.messageConverters.addAll(messageConverters);
  180 | 		}
  181 | 		updateErrorHandlerConverters();
  182 | 	}
  183 | 
  184 | 	private void validateConverters(Iterable<HttpMessageConverter<?>> messageConverters) {
  185 | 		Assert.notNull(messageConverters, "At least one HttpMessageConverter is required");
  186 | 		Assert.isTrue(messageConverters.iterator().hasNext(), "At least one HttpMessageConverter is required");
  187 | 		messageConverters.forEach(converter ->
  188 | 				Assert.notNull(converter, "The HttpMessageConverter list must not contain null elements"));
  189 | 	}
  190 | 
  191 | 	/**
  192 | 	 * Return the list of message body converters.
  193 | 	 * <p>The returned {@link List} is active and may get appended to.
  194 | 	 */
  195 | 	public List<HttpMessageConverter<?>> getMessageConverters() {
  196 | 		return this.messageConverters;
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * Set the error handler.
  201 | 	 * <p>By default, RestTemplate uses a {@link DefaultResponseErrorHandler}.
  202 | 	 */
  203 | 	public void setErrorHandler(ResponseErrorHandler errorHandler) {
  204 | 		Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");
  205 | 		this.errorHandler = errorHandler;
  206 | 		updateErrorHandlerConverters();
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Return the error handler.
  211 | 	 */
  212 | 	public ResponseErrorHandler getErrorHandler() {
  213 | 		return this.errorHandler;
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Configure default URI variable values. This is a shortcut for:
  218 | 	 * <pre class="code">
  219 | 	 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
  220 | 	 * handler.setDefaultUriVariables(...);
  221 | 	 *
  222 | 	 * RestTemplate restTemplate = new RestTemplate();
  223 | 	 * restTemplate.setUriTemplateHandler(handler);
  224 | 	 * </pre>
  225 | 	 * @param uriVars the default URI variable values
  226 | 	 * @since 4.3
  227 | 	 */
  228 | 	public void setDefaultUriVariables(Map<String, ? extends @Nullable Object> uriVars) {
  229 | 		if (this.uriTemplateHandler instanceof DefaultUriBuilderFactory factory) {
  230 | 			factory.setDefaultUriVariables(uriVars);
  231 | 		}
  232 | 		else {
  233 | 			throw new IllegalArgumentException(
  234 | 					"This property is not supported with the configured UriTemplateHandler.");
  235 | 		}
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Configure a strategy for expanding URI templates.
  240 | 	 * <p>By default, {@link DefaultUriBuilderFactory} is used and for
  241 | 	 * backwards compatibility, the encoding mode is set to
  242 | 	 * {@link EncodingMode#URI_COMPONENT URI_COMPONENT}. As of 5.0.8, prefer
  243 | 	 * using {@link EncodingMode#TEMPLATE_AND_VALUES TEMPLATE_AND_VALUES}.
  244 | 	 * @param handler the URI template handler to use
  245 | 	 */
  246 | 	public void setUriTemplateHandler(UriTemplateHandler handler) {
  247 | 		Assert.notNull(handler, "UriTemplateHandler must not be null");
  248 | 		this.uriTemplateHandler = handler;
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * Return the configured URI template handler.
  253 | 	 */
  254 | 	public UriTemplateHandler getUriTemplateHandler() {
  255 | 		return this.uriTemplateHandler;
  256 | 	}
  257 | 
  258 | 	/**
  259 | 	 * Configure an {@link ObservationRegistry} for collecting spans and metrics
  260 | 	 * for request execution. By default, {@link Observation observations} are no-ops.
  261 | 	 * @param observationRegistry the observation registry to use
  262 | 	 * @since 6.0
  263 | 	 */
  264 | 	public void setObservationRegistry(ObservationRegistry observationRegistry) {
  265 | 		Assert.notNull(observationRegistry, "observationRegistry must not be null");
  266 | 		this.observationRegistry = observationRegistry;
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Return the configured {@link ObservationRegistry}.
  271 | 	 * @since 6.1
  272 | 	 */
  273 | 	public ObservationRegistry getObservationRegistry() {
  274 | 		return this.observationRegistry;
  275 | 	}
  276 | 
  277 | 	/**
  278 | 	 * Configure an {@link ObservationConvention} that sets the name of the
  279 | 	 * {@link Observation observation} as well as its {@link io.micrometer.common.KeyValues}
  280 | 	 * extracted from the {@link ClientRequestObservationContext}.
  281 | 	 * If none set, the {@link DefaultClientRequestObservationConvention default convention} will be used.
  282 | 	 * @param observationConvention the observation convention to use
  283 | 	 * @since 6.0
  284 | 	 * @see #setObservationRegistry(ObservationRegistry)
  285 | 	 */
  286 | 	public void setObservationConvention(ClientRequestObservationConvention observationConvention) {
  287 | 		Assert.notNull(observationConvention, "observationConvention must not be null");
  288 | 		this.observationConvention = observationConvention;
  289 | 	}
  290 | 
  291 | 	/**
  292 | 	 * Return the configured {@link ClientRequestObservationConvention}, or {@code null} if not set.
  293 | 	 * @since 6.1
  294 | 	 */
  295 | 	public @Nullable ClientRequestObservationConvention getObservationConvention() {
  296 | 		return this.observationConvention;
  297 | 	}
  298 | 
  299 | 
  300 | 	// GET
  301 | 
  302 | 	@Override
  303 | 	public <T> @Nullable T getForObject(String url, Class<T> responseType, @Nullable Object... uriVariables) throws RestClientException {
  304 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  305 | 		HttpMessageConverterExtractor<T> responseExtractor =
  306 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  307 | 		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
  308 | 	}
  309 | 
  310 | 	@Override
  311 | 	public <T> @Nullable T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
  312 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  313 | 		HttpMessageConverterExtractor<T> responseExtractor =
  314 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  315 | 		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
  316 | 	}
  317 | 
  318 | 	@Override
  319 | 	public <T> @Nullable T getForObject(URI url, Class<T> responseType) throws RestClientException {
  320 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  321 | 		HttpMessageConverterExtractor<T> responseExtractor =
  322 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  323 | 		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
  324 | 	}
  325 | 
  326 | 	@Override
  327 | 	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, @Nullable Object... uriVariables)
  328 | 			throws RestClientException {
  329 | 
  330 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  331 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  332 | 		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
  333 | 	}
  334 | 
  335 | 	@Override
  336 | 	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ? extends @Nullable Object> uriVariables)
  337 | 			throws RestClientException {
  338 | 
  339 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  340 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  341 | 		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
  342 | 	}
  343 | 
  344 | 	@Override
  345 | 	public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
  346 | 		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
  347 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  348 | 		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor));
  349 | 	}
  350 | 
  351 | 
  352 | 	// HEAD
  353 | 
  354 | 	@Override
  355 | 	public HttpHeaders headForHeaders(String url, @Nullable Object... uriVariables) throws RestClientException {
  356 | 		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
  357 | 	}
  358 | 
  359 | 	@Override
  360 | 	public HttpHeaders headForHeaders(String url, Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  361 | 		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
  362 | 	}
  363 | 
  364 | 	@Override
  365 | 	public HttpHeaders headForHeaders(URI url) throws RestClientException {
  366 | 		return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor()));
  367 | 	}
  368 | 
  369 | 
  370 | 	// POST
  371 | 
  372 | 	@Override
  373 | 	public @Nullable URI postForLocation(String url, @Nullable Object request, @Nullable Object... uriVariables)
  374 | 			throws RestClientException {
  375 | 
  376 | 		RequestCallback requestCallback = httpEntityCallback(request);
  377 | 		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
  378 | 		return (headers != null ? headers.getLocation() : null);
  379 | 	}
  380 | 
  381 | 	@Override
  382 | 	public @Nullable URI postForLocation(String url, @Nullable Object request, Map<String, ? extends @Nullable Object> uriVariables)
  383 | 			throws RestClientException {
  384 | 
  385 | 		RequestCallback requestCallback = httpEntityCallback(request);
  386 | 		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
  387 | 		return (headers != null ? headers.getLocation() : null);
  388 | 	}
  389 | 
  390 | 	@Override
  391 | 	public @Nullable URI postForLocation(URI url, @Nullable Object request) throws RestClientException {
  392 | 		RequestCallback requestCallback = httpEntityCallback(request);
  393 | 		HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor());
  394 | 		return (headers != null ? headers.getLocation() : null);
  395 | 	}
  396 | 
  397 | 	@Override
  398 | 	public <T> @Nullable T postForObject(String url, @Nullable Object request, Class<T> responseType,
  399 | 			@Nullable Object... uriVariables) throws RestClientException {
  400 | 
  401 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  402 | 		HttpMessageConverterExtractor<T> responseExtractor =
  403 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  404 | 		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
  405 | 	}
  406 | 
  407 | 	@Override
  408 | 	public <T> @Nullable T postForObject(String url, @Nullable Object request, Class<T> responseType,
  409 | 			Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  410 | 
  411 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  412 | 		HttpMessageConverterExtractor<T> responseExtractor =
  413 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  414 | 		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
  415 | 	}
  416 | 
  417 | 	@Override
  418 | 	public <T> @Nullable T postForObject(URI url, @Nullable Object request, Class<T> responseType)
  419 | 			throws RestClientException {
  420 | 
  421 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  422 | 		HttpMessageConverterExtractor<T> responseExtractor =
  423 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
  424 | 		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
  425 | 	}
  426 | 
  427 | 	@Override
  428 | 	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
  429 | 			Class<T> responseType, @Nullable Object... uriVariables) throws RestClientException {
  430 | 
  431 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  432 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  433 | 		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
  434 | 	}
  435 | 
  436 | 	@Override
  437 | 	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request,
  438 | 			Class<T> responseType, Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  439 | 
  440 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  441 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  442 | 		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
  443 | 	}
  444 | 
  445 | 	@Override
  446 | 	public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType)
  447 | 			throws RestClientException {
  448 | 
  449 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  450 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  451 | 		return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor));
  452 | 	}
  453 | 
  454 | 
  455 | 	// PUT
  456 | 
  457 | 	@Override
  458 | 	public void put(String url, @Nullable Object request, @Nullable Object... uriVariables)
  459 | 			throws RestClientException {
  460 | 
  461 | 		RequestCallback requestCallback = httpEntityCallback(request);
  462 | 		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
  463 | 	}
  464 | 
  465 | 	@Override
  466 | 	public void put(String url, @Nullable Object request, Map<String, ? extends @Nullable Object> uriVariables)
  467 | 			throws RestClientException {
  468 | 
  469 | 		RequestCallback requestCallback = httpEntityCallback(request);
  470 | 		execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
  471 | 	}
  472 | 
  473 | 	@Override
  474 | 	public void put(URI url, @Nullable Object request) throws RestClientException {
  475 | 		RequestCallback requestCallback = httpEntityCallback(request);
  476 | 		execute(url, HttpMethod.PUT, requestCallback, null);
  477 | 	}
  478 | 
  479 | 
  480 | 	// PATCH
  481 | 
  482 | 	@Override
  483 | 	public <T> @Nullable T patchForObject(String url, @Nullable Object request, Class<T> responseType,
  484 | 			@Nullable Object... uriVariables) throws RestClientException {
  485 | 
  486 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  487 | 		HttpMessageConverterExtractor<T> responseExtractor =
  488 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  489 | 		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
  490 | 	}
  491 | 
  492 | 	@Override
  493 | 	public <T> @Nullable T patchForObject(String url, @Nullable Object request, Class<T> responseType,
  494 | 			Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  495 | 
  496 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  497 | 		HttpMessageConverterExtractor<T> responseExtractor =
  498 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
  499 | 		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
  500 | 	}
  501 | 
  502 | 	@Override
  503 | 	public <T> @Nullable T patchForObject(URI url, @Nullable Object request, Class<T> responseType)
  504 | 			throws RestClientException {
  505 | 
  506 | 		RequestCallback requestCallback = httpEntityCallback(request, responseType);
  507 | 		HttpMessageConverterExtractor<T> responseExtractor =
  508 | 				new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
  509 | 		return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor);
  510 | 	}
  511 | 
  512 | 
  513 | 	// DELETE
  514 | 
  515 | 	@Override
  516 | 	public void delete(String url, @Nullable Object... uriVariables) throws RestClientException {
  517 | 		execute(url, HttpMethod.DELETE, null, null, uriVariables);
  518 | 	}
  519 | 
  520 | 	@Override
  521 | 	public void delete(String url, Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  522 | 		execute(url, HttpMethod.DELETE, null, null, uriVariables);
  523 | 	}
  524 | 
  525 | 	@Override
  526 | 	public void delete(URI url) throws RestClientException {
  527 | 		execute(url, HttpMethod.DELETE, null, null);
  528 | 	}
  529 | 
  530 | 
  531 | 	// OPTIONS
  532 | 
  533 | 	@Override
  534 | 	public Set<HttpMethod> optionsForAllow(String url, @Nullable Object... uriVariables) throws RestClientException {
  535 | 		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
  536 | 		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
  537 | 		return (headers != null ? headers.getAllow() : Collections.emptySet());
  538 | 	}
  539 | 
  540 | 	@Override
  541 | 	public Set<HttpMethod> optionsForAllow(String url, Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  542 | 		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
  543 | 		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
  544 | 		return (headers != null ? headers.getAllow() : Collections.emptySet());
  545 | 	}
  546 | 
  547 | 	@Override
  548 | 	public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
  549 | 		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
  550 | 		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
  551 | 		return (headers != null ? headers.getAllow() : Collections.emptySet());
  552 | 	}
  553 | 
  554 | 
  555 | 	// exchange
  556 | 
  557 | 	@Override
  558 | 	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
  559 | 			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, @Nullable Object... uriVariables)
  560 | 			throws RestClientException {
  561 | 
  562 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
  563 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  564 | 		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  565 | 	}
  566 | 
  567 | 	@Override
  568 | 	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
  569 | 			@Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ? extends @Nullable Object> uriVariables)
  570 | 			throws RestClientException {
  571 | 
  572 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
  573 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  574 | 		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  575 | 	}
  576 | 
  577 | 	@Override
  578 | 	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
  579 | 			Class<T> responseType) throws RestClientException {
  580 | 
  581 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
  582 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  583 | 		return nonNull(execute(url, method, requestCallback, responseExtractor));
  584 | 	}
  585 | 
  586 | 	@Override
  587 | 	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
  588 | 			ParameterizedTypeReference<T> responseType, @Nullable Object... uriVariables) throws RestClientException {
  589 | 
  590 | 		Type type = responseType.getType();
  591 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
  592 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
  593 | 		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  594 | 	}
  595 | 
  596 | 	@Override
  597 | 	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
  598 | 			ParameterizedTypeReference<T> responseType, Map<String, ? extends @Nullable Object> uriVariables) throws RestClientException {
  599 | 
  600 | 		Type type = responseType.getType();
  601 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
  602 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
  603 | 		return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
  604 | 	}
  605 | 
  606 | 	@Override
  607 | 	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
  608 | 			ParameterizedTypeReference<T> responseType) throws RestClientException {
  609 | 
  610 | 		Type type = responseType.getType();
  611 | 		RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
  612 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
  613 | 		return nonNull(execute(url, method, requestCallback, responseExtractor));
  614 | 	}
  615 | 
  616 | 	@Override
  617 | 	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, Class<T> responseType)
  618 | 			throws RestClientException {
  619 | 
  620 | 		RequestCallback requestCallback = httpEntityCallback(entity, responseType);
  621 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
  622 | 		return nonNull(doExecute(resolveUrl(entity), resolveUriTemplate(entity), entity.getMethod(), requestCallback, responseExtractor));
  623 | 	}
  624 | 
  625 | 	@Override
  626 | 	public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, ParameterizedTypeReference<T> responseType)
  627 | 			throws RestClientException {
  628 | 
  629 | 		Type type = responseType.getType();
  630 | 		RequestCallback requestCallback = httpEntityCallback(entity, type);
  631 | 		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
  632 | 		return nonNull(doExecute(resolveUrl(entity), resolveUriTemplate(entity), entity.getMethod(), requestCallback, responseExtractor));
  633 | 	}
  634 | 
  635 | 	private URI resolveUrl(RequestEntity<?> entity) {
  636 | 		if (entity instanceof RequestEntity.UriTemplateRequestEntity<?> ext) {
  637 | 			if (ext.getVars() != null) {
  638 | 				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVars());
  639 | 			}
  640 | 			else if (ext.getVarsMap() != null) {
  641 | 				return this.uriTemplateHandler.expand(ext.getUriTemplate(), ext.getVarsMap());
  642 | 			}
  643 | 			else {
  644 | 				throw new IllegalStateException("No variables specified for URI template: " + ext.getUriTemplate());
  645 | 			}
  646 | 		}
  647 | 		else {
  648 | 			return entity.getUrl();
  649 | 		}
  650 | 	}
  651 | 
  652 | 	private @Nullable String resolveUriTemplate(RequestEntity<?> entity) {
  653 | 		if (entity instanceof RequestEntity.UriTemplateRequestEntity<?> templated) {
  654 | 			return templated.getUriTemplate();
  655 | 		}
  656 | 		else {
  657 | 			return null;
  658 | 		}
  659 | 	}
  660 | 
  661 | 
  662 | 	// General execution
  663 | 
  664 | 	/**
  665 | 	 * {@inheritDoc}
  666 | 	 * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
  667 | 	 * but not both, consider using:
  668 | 	 * <ul>
  669 | 	 * <li>{@link #acceptHeaderRequestCallback(Class)}
  670 | 	 * <li>{@link #httpEntityCallback(Object)}
  671 | 	 * <li>{@link #httpEntityCallback(Object, Type)}
  672 | 	 * <li>{@link #responseEntityExtractor(Type)}
  673 | 	 * </ul>
  674 | 	 */
  675 | 	@Override
  676 | 	public <T> @Nullable T execute(String uriTemplate, HttpMethod method, @Nullable RequestCallback requestCallback,
  677 | 			@Nullable ResponseExtractor<T> responseExtractor, @Nullable Object... uriVariables) throws RestClientException {
  678 | 
  679 | 		URI url = getUriTemplateHandler().expand(uriTemplate, uriVariables);
  680 | 		return doExecute(url, uriTemplate, method, requestCallback, responseExtractor);
  681 | 	}
  682 | 
  683 | 	/**
  684 | 	 * {@inheritDoc}
  685 | 	 * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
  686 | 	 * but not both, consider using:
  687 | 	 * <ul>
  688 | 	 * <li>{@link #acceptHeaderRequestCallback(Class)}
  689 | 	 * <li>{@link #httpEntityCallback(Object)}
  690 | 	 * <li>{@link #httpEntityCallback(Object, Type)}
  691 | 	 * <li>{@link #responseEntityExtractor(Type)}
  692 | 	 * </ul>
  693 | 	 */
  694 | 	@Override
  695 | 	public <T> @Nullable T execute(String uriTemplate, HttpMethod method, @Nullable RequestCallback requestCallback,
  696 | 			@Nullable ResponseExtractor<T> responseExtractor, Map<String, ? extends @Nullable Object> uriVariables)
  697 | 			throws RestClientException {
  698 | 
  699 | 		URI url = getUriTemplateHandler().expand(uriTemplate, uriVariables);
  700 | 		return doExecute(url, uriTemplate, method, requestCallback, responseExtractor);
  701 | 	}
  702 | 
  703 | 	/**
  704 | 	 * {@inheritDoc}
  705 | 	 * <p>To provide a {@code RequestCallback} or {@code ResponseExtractor} only,
  706 | 	 * but not both, consider using:
  707 | 	 * <ul>
  708 | 	 * <li>{@link #acceptHeaderRequestCallback(Class)}
  709 | 	 * <li>{@link #httpEntityCallback(Object)}
  710 | 	 * <li>{@link #httpEntityCallback(Object, Type)}
  711 | 	 * <li>{@link #responseEntityExtractor(Type)}
  712 | 	 * </ul>
  713 | 	 */
  714 | 	@Override
  715 | 	public <T> @Nullable T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback,
  716 | 			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
  717 | 
  718 | 		return doExecute(url, null, method, requestCallback, responseExtractor);
  719 | 	}
  720 | 
  721 | 	/**
  722 | 	 * Execute the given method on the provided URI.
  723 | 	 * <p>The {@link ClientHttpRequest} is processed using the {@link RequestCallback};
  724 | 	 * the response with the {@link ResponseExtractor}.
  725 | 	 * @param url the fully-expanded URL to connect to
  726 | 	 * @param uriTemplate the URI template that was used for creating the expanded URL
  727 | 	 * @param method the HTTP method to execute (GET, POST, etc.)
  728 | 	 * @param requestCallback object that prepares the request (can be {@code null})
  729 | 	 * @param responseExtractor object that extracts the return value from the response (can be {@code null})
  730 | 	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
  731 | 	 * @since 6.0
  732 | 	 */
  733 | 	@SuppressWarnings("try")
  734 | 	protected <T> @Nullable T doExecute(URI url, @Nullable String uriTemplate, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
  735 | 			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
  736 | 
  737 | 		Assert.notNull(url, "url is required");
  738 | 		Assert.notNull(method, "HttpMethod is required");
  739 | 		ClientHttpRequest request;
  740 | 		try {
  741 | 			request = createRequest(url, method);
  742 | 		}
  743 | 		catch (IOException ex) {
  744 | 			throw createResourceAccessException(url, method, ex);
  745 | 		}
  746 | 
  747 | 		ClientRequestObservationContext observationContext = new ClientRequestObservationContext(request);
  748 | 		observationContext.setUriTemplate(uriTemplate);
  749 | 		Observation observation = ClientHttpObservationDocumentation.HTTP_CLIENT_EXCHANGES.observation(
  750 | 				this.observationConvention, DEFAULT_OBSERVATION_CONVENTION,
  751 | 				() -> observationContext, this.observationRegistry).start();
  752 | 		ClientHttpResponse response = null;
  753 | 		try (Observation.Scope scope = observation.openScope()){
  754 | 			if (requestCallback != null) {
  755 | 				requestCallback.doWithRequest(request);
  756 | 			}
  757 | 			response = request.execute();
  758 | 			observationContext.setResponse(response);
  759 | 			handleResponse(url, method, response);
  760 | 			return (responseExtractor != null ? responseExtractor.extractData(response) : null);
  761 | 		}
  762 | 		catch (IOException ex) {
  763 | 			ResourceAccessException accessEx = createResourceAccessException(url, method, ex);
  764 | 			observation.error(accessEx);
  765 | 			throw accessEx;
  766 | 		}
  767 | 		catch (Throwable ex) {
  768 | 			observation.error(ex);
  769 | 			throw ex;
  770 | 		}
  771 | 		finally {
  772 | 			if (response != null) {
  773 | 				response.close();
  774 | 			}
  775 | 			observation.stop();
  776 | 		}
  777 | 	}
  778 | 
  779 | 	private static ResourceAccessException createResourceAccessException(URI url, HttpMethod method, IOException ex) {
  780 | 		String resource = url.toString();
  781 | 		resource = (url.getRawQuery() != null ? resource.substring(0, resource.indexOf('?')) : resource);
  782 | 		return new ResourceAccessException("I/O error on " + method.name() +
  783 | 				" request for \"" + resource + "\": " + ex.getMessage(), ex);
  784 | 	}
  785 | 
  786 | 	/**
  787 | 	 * Handle the given response, performing appropriate logging and
  788 | 	 * invoking the {@link ResponseErrorHandler} if necessary.
  789 | 	 * <p>Can be overridden in subclasses.
  790 | 	 * @param url the fully-expanded URL to connect to
  791 | 	 * @param method the HTTP method to execute (GET, POST, etc.)
  792 | 	 * @param response the resulting {@link ClientHttpResponse}
  793 | 	 * @throws IOException if propagated from {@link ResponseErrorHandler}
  794 | 	 * @since 4.1.6
  795 | 	 * @see #setErrorHandler
  796 | 	 */
  797 | 	protected void handleResponse(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
  798 | 		ResponseErrorHandler errorHandler = getErrorHandler();
  799 | 		boolean hasError = errorHandler.hasError(response);
  800 | 		if (logger.isDebugEnabled()) {
  801 | 			try {
  802 | 				HttpStatusCode statusCode = response.getStatusCode();
  803 | 				logger.debug("Response " + statusCode);
  804 | 			}
  805 | 			catch (IOException ex) {
  806 | 				logger.debug("Failed to obtain response status code", ex);
  807 | 			}
  808 | 		}
  809 | 		if (hasError) {
  810 | 			errorHandler.handleError(url, method, response);
  811 | 		}
  812 | 	}
  813 | 
  814 | 	/**
  815 | 	 * Return a {@code RequestCallback} that sets the request {@code Accept}
  816 | 	 * header based on the given response type, cross-checked against the
  817 | 	 * configured message converters.
  818 | 	 */
  819 | 	public <T> RequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
  820 | 		return new AcceptHeaderRequestCallback(responseType);
  821 | 	}
  822 | 
  823 | 	/**
  824 | 	 * Return a {@code RequestCallback} implementation that writes the given
  825 | 	 * object to the request stream.
  826 | 	 */
  827 | 	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody) {
  828 | 		return new HttpEntityRequestCallback(requestBody);
  829 | 	}
  830 | 
  831 | 	/**
  832 | 	 * Return a {@code RequestCallback} implementation that:
  833 | 	 * <ol>
  834 | 	 * <li>Sets the request {@code Accept} header based on the given response
  835 | 	 * type, cross-checked against the configured message converters.
  836 | 	 * <li>Writes the given object to the request stream.
  837 | 	 * </ol>
  838 | 	 */
  839 | 	public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
  840 | 		return new HttpEntityRequestCallback(requestBody, responseType);
  841 | 	}
  842 | 
  843 | 	/**
  844 | 	 * Return a {@code ResponseExtractor} that prepares a {@link ResponseEntity}.
  845 | 	 */
  846 | 	public <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
  847 | 		return new ResponseEntityResponseExtractor<>(responseType);
  848 | 	}
  849 | 
  850 | 	/**
  851 | 	 * Return a response extractor for {@link HttpHeaders}.
  852 | 	 */
  853 | 	protected ResponseExtractor<HttpHeaders> headersExtractor() {
  854 | 		return this.headersExtractor;
  855 | 	}
  856 | 
  857 | 	private static <T> T nonNull(@Nullable T result) {
  858 | 		Assert.state(result != null, "No result");
  859 | 		return result;
  860 | 	}
  861 | 
  862 | 
  863 | 	/**
  864 | 	 * Request callback implementation that prepares the request's accept headers.
  865 | 	 */
  866 | 	private class AcceptHeaderRequestCallback implements RequestCallback {
  867 | 
  868 | 		private final @Nullable Type responseType;
  869 | 
  870 | 		public AcceptHeaderRequestCallback(@Nullable Type responseType) {
  871 | 			this.responseType = responseType;
  872 | 		}
  873 | 
  874 | 		@Override
  875 | 		public void doWithRequest(ClientHttpRequest request) throws IOException {
  876 | 			if (this.responseType != null) {
  877 | 				List<MediaType> allSupportedMediaTypes = getMessageConverters().stream()
  878 | 						.filter(converter -> canReadResponse(this.responseType, converter))
  879 | 						.flatMap((HttpMessageConverter<?> converter) -> getSupportedMediaTypes(this.responseType, converter))
  880 | 						.distinct()
  881 | 						.collect(Collectors.toList());
  882 | 				MimeTypeUtils.sortBySpecificity(allSupportedMediaTypes);
  883 | 				if (logger.isDebugEnabled()) {
  884 | 					logger.debug("Accept=" + allSupportedMediaTypes);
  885 | 				}
  886 | 				request.getHeaders().setAccept(allSupportedMediaTypes);
  887 | 			}
  888 | 		}
  889 | 
  890 | 		private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
  891 | 			if (converter instanceof GenericHttpMessageConverter<?> genericConverter) {
  892 | 				return genericConverter.canRead(responseType, null, null);
  893 | 			}
  894 | 			else if (converter instanceof SmartHttpMessageConverter<?> smartConverter) {
  895 | 				return smartConverter.canRead(ResolvableType.forType(responseType), null);
  896 | 			}
  897 | 			else if (responseType instanceof Class<?> responseClass) {
  898 | 				return converter.canRead(responseClass, null);
  899 | 			}
  900 | 			return false;
  901 | 		}
  902 | 
  903 | 		private Stream<MediaType> getSupportedMediaTypes(Type type, HttpMessageConverter<?> converter) {
  904 | 			Type rawType = (type instanceof ParameterizedType parameterizedType ? parameterizedType.getRawType() : type);
  905 | 			Class<?> clazz = (rawType instanceof Class<?> rawClass ? rawClass : null);
  906 | 			return (clazz != null ? converter.getSupportedMediaTypes(clazz) : converter.getSupportedMediaTypes())
  907 | 					.stream()
  908 | 					.map(mediaType -> {
  909 | 						if (mediaType.getCharset() != null) {
  910 | 							return new MediaType(mediaType.getType(), mediaType.getSubtype());
  911 | 						}
  912 | 						return mediaType;
  913 | 					});
  914 | 		}
  915 | 	}
  916 | 
  917 | 
  918 | 	/**
  919 | 	 * Request callback implementation that writes the given object to the request stream.
  920 | 	 */
  921 | 	private class HttpEntityRequestCallback extends AcceptHeaderRequestCallback {
  922 | 
  923 | 		private final HttpEntity<?> requestEntity;
  924 | 
  925 | 		public HttpEntityRequestCallback(@Nullable Object requestBody) {
  926 | 			this(requestBody, null);
  927 | 		}
  928 | 
  929 | 		public HttpEntityRequestCallback(@Nullable Object requestBody, @Nullable Type responseType) {
  930 | 			super(responseType);
  931 | 			if (requestBody instanceof HttpEntity<?> httpEntity) {
  932 | 				this.requestEntity = httpEntity;
  933 | 			}
  934 | 			else if (requestBody != null) {
  935 | 				this.requestEntity = new HttpEntity<>(requestBody);
  936 | 			}
  937 | 			else {
  938 | 				this.requestEntity = HttpEntity.EMPTY;
  939 | 			}
  940 | 		}
  941 | 
  942 | 		@Override
  943 | 		@SuppressWarnings({"rawtypes", "unchecked"})
  944 | 		public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
  945 | 			super.doWithRequest(httpRequest);
  946 | 			Object requestBody = this.requestEntity.getBody();
  947 | 			if (requestBody == null) {
  948 | 				HttpHeaders httpHeaders = httpRequest.getHeaders();
  949 | 				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
  950 | 				if (!requestHeaders.isEmpty()) {
  951 | 					requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
  952 | 				}
  953 | 				if (httpHeaders.getContentLength() < 0) {
  954 | 					httpHeaders.setContentLength(0L);
  955 | 				}
  956 | 			}
  957 | 			else {
  958 | 				Class<?> requestBodyClass = requestBody.getClass();
  959 | 				// The following pattern variable cannot be named "requestEntity" due to lacking
  960 | 				// support in Checkstyle: https://github.com/checkstyle/checkstyle/issues/10969
  961 | 				Type requestBodyType = (this.requestEntity instanceof RequestEntity<?> _requestEntity ?
  962 | 						_requestEntity.getType() : requestBodyClass);
  963 | 				HttpHeaders httpHeaders = httpRequest.getHeaders();
  964 | 				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
  965 | 				MediaType requestContentType = requestHeaders.getContentType();
  966 | 				for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
  967 | 					if (messageConverter instanceof GenericHttpMessageConverter genericConverter) {
  968 | 						if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
  969 | 							if (!requestHeaders.isEmpty()) {
  970 | 								requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
  971 | 							}
  972 | 							logBody(requestBody, requestContentType, genericConverter);
  973 | 							genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
  974 | 							return;
  975 | 						}
  976 | 					}
  977 | 					else if (messageConverter instanceof SmartHttpMessageConverter smartConverter) {
  978 | 						ResolvableType resolvableType = ResolvableType.forType(requestBodyType);
  979 | 						if (smartConverter.canWrite(resolvableType, requestBodyClass, requestContentType)) {
  980 | 							if (!requestHeaders.isEmpty()) {
  981 | 								requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
  982 | 							}
  983 | 							logBody(requestBody, requestContentType, smartConverter);
  984 | 							smartConverter.write(requestBody, resolvableType, requestContentType, httpRequest, null);
  985 | 							return;
  986 | 						}
  987 | 					}
  988 | 					else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
  989 | 						if (!requestHeaders.isEmpty()) {
  990 | 							requestHeaders.forEach((key, values) -> httpHeaders.put(key, new ArrayList<>(values)));
  991 | 						}
  992 | 						logBody(requestBody, requestContentType, messageConverter);
  993 | 						((HttpMessageConverter<Object>) messageConverter).write(
  994 | 								requestBody, requestContentType, httpRequest);
  995 | 						return;
  996 | 					}
  997 | 				}
  998 | 				String message = "No HttpMessageConverter for " + requestBodyClass.getName();
  999 | 				if (requestContentType != null) {
 1000 | 					message += " and content type \"" + requestContentType + "\"";
 1001 | 				}
 1002 | 				throw new RestClientException(message);
 1003 | 			}
 1004 | 		}
 1005 | 
 1006 | 		private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
 1007 | 			if (logger.isDebugEnabled()) {
 1008 | 				if (mediaType != null) {
 1009 | 					logger.debug("Writing [" + body + "] as \"" + mediaType + "\"");
 1010 | 				}
 1011 | 				else {
 1012 | 					logger.debug("Writing [" + body + "] with " + converter.getClass().getName());
 1013 | 				}
 1014 | 			}
 1015 | 		}
 1016 | 	}
 1017 | 
 1018 | 
 1019 | 	/**
 1020 | 	 * Response extractor for {@link HttpEntity}.
 1021 | 	 */
 1022 | 	private class ResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {
 1023 | 
 1024 | 		private final @Nullable HttpMessageConverterExtractor<T> delegate;
 1025 | 
 1026 | 		public ResponseEntityResponseExtractor(@Nullable Type responseType) {
 1027 | 			if (responseType != null && Void.class != responseType) {
 1028 | 				this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
 1029 | 			}
 1030 | 			else {
 1031 | 				this.delegate = null;
 1032 | 			}
 1033 | 		}
 1034 | 
 1035 | 		@Override
 1036 | 		public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
 1037 | 			if (this.delegate != null) {
 1038 | 				T body = this.delegate.extractData(response);
 1039 | 				return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(body);
 1040 | 			}
 1041 | 			else {
 1042 | 				return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).build();
 1043 | 			}
 1044 | 		}
 1045 | 	}
 1046 | 
 1047 | 
 1048 | 	/**
 1049 | 	 * Response extractor that extracts the response {@link HttpHeaders}.
 1050 | 	 */
 1051 | 	private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {
 1052 | 
 1053 | 		@Override
 1054 | 		public HttpHeaders extractData(ClientHttpResponse response) {
 1055 | 			return response.getHeaders();
 1056 | 		}
 1057 | 	}
 1058 | 
 1059 | }
```
