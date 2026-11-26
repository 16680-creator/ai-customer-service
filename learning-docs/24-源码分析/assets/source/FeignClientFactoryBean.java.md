<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientFactoryBean.java -->
<!-- 正文引用：[[源码:FeignClientFactoryBean.java#L起-L止]] ；代码块头部：(src: FeignClientFactoryBean.java#L起-L止) -->

# FeignClientFactoryBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientFactoryBean.java` |
| 行数 | 780 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:24.088Z |

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
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Objects;
   26 | import java.util.Set;
   27 | import java.util.WeakHashMap;
   28 | import java.util.concurrent.ConcurrentHashMap;
   29 | import java.util.concurrent.TimeUnit;
   30 | 
   31 | import feign.Capability;
   32 | import feign.Client;
   33 | import feign.Contract;
   34 | import feign.ExceptionPropagationPolicy;
   35 | import feign.Feign;
   36 | import feign.Logger;
   37 | import feign.QueryMapEncoder;
   38 | import feign.Request;
   39 | import feign.RequestInterceptor;
   40 | import feign.ResponseInterceptor;
   41 | import feign.Retryer;
   42 | import feign.Target.HardCodedTarget;
   43 | import feign.codec.Decoder;
   44 | import feign.codec.Encoder;
   45 | import feign.codec.ErrorDecoder;
   46 | import org.apache.commons.logging.Log;
   47 | import org.apache.commons.logging.LogFactory;
   48 | 
   49 | import org.springframework.beans.BeanUtils;
   50 | import org.springframework.beans.BeansException;
   51 | import org.springframework.beans.factory.BeanFactory;
   52 | import org.springframework.beans.factory.BeanFactoryAware;
   53 | import org.springframework.beans.factory.FactoryBean;
   54 | import org.springframework.beans.factory.InitializingBean;
   55 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   56 | import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
   57 | import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
   58 | import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
   59 | import org.springframework.cloud.openfeign.support.NestedQueryMapCapability;
   60 | import org.springframework.context.ApplicationContext;
   61 | import org.springframework.context.ApplicationContextAware;
   62 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   63 | import org.springframework.util.Assert;
   64 | import org.springframework.util.StringUtils;
   65 | 
   66 | /**
   67 |  * @author Spencer Gibb
   68 |  * @author Venil Noronha
   69 |  * @author Eko Kurniawan Khannedy
   70 |  * @author Gregor Zurowski
   71 |  * @author Matt King
   72 |  * @author Olga Maciaszek-Sharma
   73 |  * @author Ilia Ilinykh
   74 |  * @author Marcin Grzejszczak
   75 |  * @author Jonatan Ivanov
   76 |  * @author Sam Kruglov
   77 |  * @author Jasbir Singh
   78 |  * @author Hyeonmin Park
   79 |  * @author Felix Dittrich
   80 |  * @author Dominique Villard
   81 |  * @author Can Bezmen
   82 |  * @author Sharang Gupta
   83 |  */
   84 | public class FeignClientFactoryBean
   85 | 		implements FactoryBean<Object>, InitializingBean, ApplicationContextAware, BeanFactoryAware {
   86 | 
   87 | 	/***********************************
   88 | 	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some
   89 | 	 * lifecycle race condition.
   90 | 	 ***********************************/
   91 | 
   92 | 	private static final Log LOG = LogFactory.getLog(FeignClientFactoryBean.class);
   93 | 
   94 | 	/**
   95 | 	 * Context ids whose {@link Client} bean has already been resolved, tracked per
   96 | 	 * application context so that a second application context in the same JVM does not
   97 | 	 * look like repeated initialisation.
   98 | 	 */
   99 | 	private static final Map<Object, Set<String>> resolvedContextIds = Collections.synchronizedMap(new WeakHashMap<>());
  100 | 
  101 | 	private Class<?> type;
  102 | 
  103 | 	private String name;
  104 | 
  105 | 	private String url;
  106 | 
  107 | 	private String contextId;
  108 | 
  109 | 	private String path;
  110 | 
  111 | 	private boolean dismiss404;
  112 | 
  113 | 	private boolean inheritParentContext = true;
  114 | 
  115 | 	private ApplicationContext applicationContext;
  116 | 
  117 | 	private BeanFactory beanFactory;
  118 | 
  119 | 	private Class<?> fallback = void.class;
  120 | 
  121 | 	private Class<?> fallbackFactory = void.class;
  122 | 
  123 | 	private int readTimeoutMillis = new Request.Options().readTimeoutMillis();
  124 | 
  125 | 	private int connectTimeoutMillis = new Request.Options().connectTimeoutMillis();
  126 | 
  127 | 	private boolean followRedirects = new Request.Options().isFollowRedirects();
  128 | 
  129 | 	private boolean refreshableClient = false;
  130 | 
  131 | 	private final List<FeignBuilderCustomizer> additionalCustomizers = new ArrayList<>();
  132 | 
  133 | 	private String[] qualifiers = new String[] {};
  134 | 
  135 | 	// For AOT testing
  136 | 	public FeignClientFactoryBean() {
  137 | 		if (LOG.isDebugEnabled()) {
  138 | 			LOG.debug("Creating a FeignClientFactoryBean.");
  139 | 		}
  140 | 	}
  141 | 
  142 | 	@Override
  143 | 	public void afterPropertiesSet() {
  144 | 		Assert.hasText(contextId, "Context id must be set");
  145 | 		Assert.hasText(name, "Name must be set");
  146 | 	}
  147 | 
  148 | 	protected Feign.Builder feign(FeignClientFactory context) {
  149 | 		FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
  150 | 		Logger logger = loggerFactory.create(type);
  151 | 
  152 | 		// @formatter:off
  153 | 		Feign.Builder builder = get(context, Feign.Builder.class)
  154 | 				// required values
  155 | 				.logger(logger)
  156 | 				.encoder(get(context, Encoder.class))
  157 | 				.decoder(get(context, Decoder.class))
  158 | 				.contract(get(context, Contract.class));
  159 | 		// @formatter:on
  160 | 
  161 | 		configureFeign(context, builder);
  162 | 
  163 | 		return builder;
  164 | 	}
  165 | 
  166 | 	private void applyBuildCustomizers(FeignClientFactory context, Feign.Builder builder) {
  167 | 		Map<String, FeignBuilderCustomizer> customizerMap = context.getInstances(contextId,
  168 | 				FeignBuilderCustomizer.class);
  169 | 
  170 | 		if (customizerMap != null) {
  171 | 			customizerMap.values()
  172 | 				.stream()
  173 | 				.sorted(AnnotationAwareOrderComparator.INSTANCE)
  174 | 				.forEach(feignBuilderCustomizer -> feignBuilderCustomizer.customize(builder));
  175 | 		}
  176 | 		additionalCustomizers.forEach(customizer -> customizer.customize(builder));
  177 | 	}
  178 | 
  179 | 	protected void configureFeign(FeignClientFactory context, Feign.Builder builder) {
  180 | 		FeignClientProperties properties = beanFactory != null ? beanFactory.getBean(FeignClientProperties.class)
  181 | 				: applicationContext.getBean(FeignClientProperties.class);
  182 | 
  183 | 		FeignClientConfigurer feignClientConfigurer = getOptional(context, FeignClientConfigurer.class);
  184 | 		setInheritParentContext(feignClientConfigurer.inheritParentConfiguration());
  185 | 
  186 | 		if (properties != null && inheritParentContext) {
  187 | 			if (properties.isDefaultToProperties()) {
  188 | 				configureUsingConfiguration(context, builder);
  189 | 				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()),
  190 | 						properties.getConfig().get(contextId), builder);
  191 | 			}
  192 | 			else {
  193 | 				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()),
  194 | 						properties.getConfig().get(contextId), builder);
  195 | 				configureUsingConfiguration(context, builder);
  196 | 			}
  197 | 			configureDefaultRequestElements(properties.getConfig().get(properties.getDefaultConfig()),
  198 | 					properties.getConfig().get(contextId), builder);
  199 | 		}
  200 | 		else {
  201 | 			configureUsingConfiguration(context, builder);
  202 | 		}
  203 | 	}
  204 | 
  205 | 	protected void configureUsingConfiguration(FeignClientFactory context, Feign.Builder builder) {
  206 | 		Logger.Level level = getInheritedAwareOptional(context, Logger.Level.class);
  207 | 		if (level != null) {
  208 | 			builder.logLevel(level);
  209 | 		}
  210 | 		Retryer retryer = getInheritedAwareOptional(context, Retryer.class);
  211 | 		if (retryer != null) {
  212 | 			builder.retryer(retryer);
  213 | 		}
  214 | 		ErrorDecoder errorDecoder = getInheritedAwareOptional(context, ErrorDecoder.class);
  215 | 		if (errorDecoder != null) {
  216 | 			builder.errorDecoder(errorDecoder);
  217 | 		}
  218 | 		else {
  219 | 			FeignErrorDecoderFactory errorDecoderFactory = getOptional(context, FeignErrorDecoderFactory.class);
  220 | 			if (errorDecoderFactory != null) {
  221 | 				ErrorDecoder factoryErrorDecoder = errorDecoderFactory.create(type);
  222 | 				builder.errorDecoder(factoryErrorDecoder);
  223 | 			}
  224 | 		}
  225 | 		Request.Options options = getInheritedAwareOptional(context, Request.Options.class);
  226 | 		if (options == null) {
  227 | 			options = getOptionsByName(context, contextId);
  228 | 		}
  229 | 
  230 | 		if (options != null) {
  231 | 			builder.options(options);
  232 | 			readTimeoutMillis = options.readTimeoutMillis();
  233 | 			connectTimeoutMillis = options.connectTimeoutMillis();
  234 | 			followRedirects = options.isFollowRedirects();
  235 | 		}
  236 | 		Map<String, RequestInterceptor> requestInterceptors = getInheritedAwareInstances(context,
  237 | 				RequestInterceptor.class);
  238 | 		if (requestInterceptors != null) {
  239 | 			List<RequestInterceptor> interceptors = new ArrayList<>(requestInterceptors.values());
  240 | 			AnnotationAwareOrderComparator.sort(interceptors);
  241 | 			builder.requestInterceptors(interceptors);
  242 | 		}
  243 | 		ResponseInterceptor responseInterceptor = getInheritedAwareOptional(context, ResponseInterceptor.class);
  244 | 		if (responseInterceptor != null) {
  245 | 			builder.responseInterceptor(responseInterceptor);
  246 | 		}
  247 | 		QueryMapEncoder queryMapEncoder = getInheritedAwareOptional(context, QueryMapEncoder.class);
  248 | 		if (queryMapEncoder != null) {
  249 | 			builder.queryMapEncoder(queryMapEncoder);
  250 | 		}
  251 | 		if (dismiss404) {
  252 | 			builder.dismiss404();
  253 | 		}
  254 | 		ExceptionPropagationPolicy exceptionPropagationPolicy = getInheritedAwareOptional(context,
  255 | 				ExceptionPropagationPolicy.class);
  256 | 		if (exceptionPropagationPolicy != null) {
  257 | 			builder.exceptionPropagationPolicy(exceptionPropagationPolicy);
  258 | 		}
  259 | 
  260 | 		Map<String, Capability> capabilities = getInheritedAwareInstances(context, Capability.class);
  261 | 		if (capabilities != null) {
  262 | 			capabilities.values()
  263 | 				.stream()
  264 | 				.sorted(AnnotationAwareOrderComparator.INSTANCE)
  265 | 				.forEach(builder::addCapability);
  266 | 		}
  267 | 	}
  268 | 
  269 | 	protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration baseConfig,
  270 | 			FeignClientProperties.FeignClientConfiguration finalConfig, Feign.Builder builder) {
  271 | 		configureUsingProperties(baseConfig, builder);
  272 | 		configureUsingProperties(finalConfig, builder);
  273 | 		Boolean dismiss404 = finalConfig != null && finalConfig.getDismiss404() != null ? finalConfig.getDismiss404()
  274 | 				: (baseConfig != null && baseConfig.getDismiss404() != null ? baseConfig.getDismiss404() : null);
  275 | 		if (dismiss404 != null) {
  276 | 			if (dismiss404) {
  277 | 				builder.dismiss404();
  278 | 			}
  279 | 		}
  280 | 		Boolean nestedQueryMapEnabled = finalConfig != null && finalConfig.getNestedQueryMapEnabled() != null
  281 | 				? finalConfig.getNestedQueryMapEnabled()
  282 | 				: (baseConfig != null ? baseConfig.getNestedQueryMapEnabled() : null);
  283 | 		if (Boolean.TRUE.equals(nestedQueryMapEnabled)) {
  284 | 			builder.addCapability(new NestedQueryMapCapability());
  285 | 		}
  286 | 	}
  287 | 
  288 | 	protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config,
  289 | 			Feign.Builder builder) {
  290 | 		if (config == null) {
  291 | 			return;
  292 | 		}
  293 | 
  294 | 		if (config.getLoggerLevel() != null) {
  295 | 			builder.logLevel(config.getLoggerLevel());
  296 | 		}
  297 | 
  298 | 		if (!refreshableClient) {
  299 | 			connectTimeoutMillis = config.getConnectTimeout() != null ? config.getConnectTimeout()
  300 | 					: connectTimeoutMillis;
  301 | 			readTimeoutMillis = config.getReadTimeout() != null ? config.getReadTimeout() : readTimeoutMillis;
  302 | 			followRedirects = config.isFollowRedirects() != null ? config.isFollowRedirects() : followRedirects;
  303 | 
  304 | 			builder.options(new Request.Options(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis,
  305 | 					TimeUnit.MILLISECONDS, followRedirects));
  306 | 		}
  307 | 
  308 | 		if (config.getRetryer() != null) {
  309 | 			Retryer retryer = getOrInstantiate(config.getRetryer());
  310 | 			builder.retryer(retryer);
  311 | 		}
  312 | 
  313 | 		if (config.getErrorDecoder() != null) {
  314 | 			ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
  315 | 			builder.errorDecoder(errorDecoder);
  316 | 		}
  317 | 
  318 | 		if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
  319 | 			// this will add request interceptor to builder, not replace existing
  320 | 			for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
  321 | 				RequestInterceptor interceptor = getOrInstantiate(bean);
  322 | 				builder.requestInterceptor(interceptor);
  323 | 			}
  324 | 		}
  325 | 
  326 | 		if (config.getResponseInterceptor() != null) {
  327 | 			builder.responseInterceptor(getOrInstantiate(config.getResponseInterceptor()));
  328 | 		}
  329 | 
  330 | 		if (Objects.nonNull(config.getEncoder())) {
  331 | 			builder.encoder(getOrInstantiate(config.getEncoder()));
  332 | 		}
  333 | 
  334 | 		if (Objects.nonNull(config.getDecoder())) {
  335 | 			builder.decoder(getOrInstantiate(config.getDecoder()));
  336 | 		}
  337 | 
  338 | 		if (Objects.nonNull(config.getContract())) {
  339 | 			builder.contract(getOrInstantiate(config.getContract()));
  340 | 		}
  341 | 
  342 | 		if (Objects.nonNull(config.getExceptionPropagationPolicy())) {
  343 | 			builder.exceptionPropagationPolicy(config.getExceptionPropagationPolicy());
  344 | 		}
  345 | 
  346 | 		if (config.getCapabilities() != null) {
  347 | 			config.getCapabilities().stream().map(this::getOrInstantiate).forEach(builder::addCapability);
  348 | 		}
  349 | 
  350 | 		if (config.getQueryMapEncoder() != null) {
  351 | 			builder.queryMapEncoder(getOrInstantiate(config.getQueryMapEncoder()));
  352 | 		}
  353 | 	}
  354 | 
  355 | 	protected void configureDefaultRequestElements(FeignClientProperties.FeignClientConfiguration defaultConfig,
  356 | 			FeignClientProperties.FeignClientConfiguration clientConfig, Feign.Builder builder) {
  357 | 		Map<String, Collection<String>> defaultRequestHeaders = new HashMap<>();
  358 | 		if (defaultConfig != null) {
  359 | 			defaultConfig.getDefaultRequestHeaders()
  360 | 				.forEach((k, v) -> defaultRequestHeaders.put(k, new ArrayList<>(v)));
  361 | 		}
  362 | 		if (clientConfig != null) {
  363 | 			clientConfig.getDefaultRequestHeaders().forEach((k, v) -> defaultRequestHeaders.put(k, new ArrayList<>(v)));
  364 | 		}
  365 | 		if (!defaultRequestHeaders.isEmpty()) {
  366 | 			addDefaultRequestHeaders(defaultRequestHeaders, builder);
  367 | 		}
  368 | 
  369 | 		Map<String, Collection<String>> defaultQueryParameters = new HashMap<>();
  370 | 		if (defaultConfig != null) {
  371 | 			defaultConfig.getDefaultQueryParameters()
  372 | 				.forEach((k, v) -> defaultQueryParameters.put(k, new ArrayList<>(v)));
  373 | 		}
  374 | 		if (clientConfig != null) {
  375 | 			clientConfig.getDefaultQueryParameters()
  376 | 				.forEach((k, v) -> defaultQueryParameters.put(k, new ArrayList<>(v)));
  377 | 		}
  378 | 		if (!defaultQueryParameters.isEmpty()) {
  379 | 			addDefaultQueryParams(defaultQueryParameters, builder);
  380 | 		}
  381 | 
  382 | 	}
  383 | 
  384 | 	private void addDefaultQueryParams(Map<String, Collection<String>> defaultQueryParameters, Feign.Builder builder) {
  385 | 		builder.requestInterceptor(requestTemplate -> {
  386 | 			Map<String, Collection<String>> queries = requestTemplate.queries();
  387 | 			defaultQueryParameters.keySet().forEach(key -> {
  388 | 				if (!queries.containsKey(key)) {
  389 | 					requestTemplate.query(key, defaultQueryParameters.get(key));
  390 | 				}
  391 | 			});
  392 | 		});
  393 | 	}
  394 | 
  395 | 	private void addDefaultRequestHeaders(Map<String, Collection<String>> defaultRequestHeaders,
  396 | 			Feign.Builder builder) {
  397 | 		builder.requestInterceptor(requestTemplate -> {
  398 | 			Map<String, Collection<String>> headers = requestTemplate.headers();
  399 | 			defaultRequestHeaders.keySet().forEach(key -> {
  400 | 				if (!headers.containsKey(key)) {
  401 | 					requestTemplate.header(key, defaultRequestHeaders.get(key));
  402 | 				}
  403 | 			});
  404 | 		});
  405 | 	}
  406 | 
  407 | 	private <T> T getOrInstantiate(Class<T> tClass) {
  408 | 		try {
  409 | 			return beanFactory != null ? beanFactory.getBean(tClass) : applicationContext.getBean(tClass);
  410 | 		}
  411 | 		catch (NoSuchBeanDefinitionException e) {
  412 | 			return BeanUtils.instantiateClass(tClass);
  413 | 		}
  414 | 	}
  415 | 
  416 | 	protected <T> T get(FeignClientFactory context, Class<T> type) {
  417 | 		T instance = context.getInstance(contextId, type);
  418 | 		if (instance == null) {
  419 | 			throw new IllegalStateException("No bean found of type " + type + " for " + contextId);
  420 | 		}
  421 | 		return instance;
  422 | 	}
  423 | 
  424 | 	protected <T> T getOptional(FeignClientFactory context, Class<T> type) {
  425 | 		return context.getInstance(contextId, type);
  426 | 	}
  427 | 
  428 | 	protected <T> T getInheritedAwareOptional(FeignClientFactory context, Class<T> type) {
  429 | 		if (inheritParentContext) {
  430 | 			return getOptional(context, type);
  431 | 		}
  432 | 		else {
  433 | 			return context.getInstanceWithoutAncestors(contextId, type);
  434 | 		}
  435 | 	}
  436 | 
  437 | 	protected <T> Map<String, T> getInheritedAwareInstances(FeignClientFactory context, Class<T> type) {
  438 | 		if (inheritParentContext) {
  439 | 			return context.getInstances(contextId, type);
  440 | 		}
  441 | 		else {
  442 | 			return context.getInstancesWithoutAncestors(contextId, type);
  443 | 		}
  444 | 	}
  445 | 
  446 | 	protected <T> T loadBalance(Feign.Builder builder, FeignClientFactory context, HardCodedTarget<T> target) {
  447 | 		Client client = getOptional(context, Client.class);
  448 | 		if (client != null) {
  449 | 			builder.client(client);
  450 | 			applyBuildCustomizers(context, builder);
  451 | 			Targeter targeter = get(context, Targeter.class);
  452 | 			return targeter.target(this, builder, context, target);
  453 | 		}
  454 | 
  455 | 		throw new IllegalStateException(
  456 | 				"No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-loadbalancer?");
  457 | 	}
  458 | 
  459 | 	/**
  460 | 	 * Meant to get Options bean from context with bean name.
  461 | 	 * @param context context of Feign client
  462 | 	 * @param contextId name of feign client
  463 | 	 * @return returns Options found in context
  464 | 	 */
  465 | 	protected Request.Options getOptionsByName(FeignClientFactory context, String contextId) {
  466 | 		if (refreshableClient) {
  467 | 			return context.getInstance(contextId, Request.Options.class.getCanonicalName() + "-" + contextId,
  468 | 					Request.Options.class);
  469 | 		}
  470 | 		return null;
  471 | 	}
  472 | 
  473 | 	@Override
  474 | 	public Object getObject() {
  475 | 		return getTarget();
  476 | 	}
  477 | 
  478 | 	/**
  479 | 	 * @param <T> the target type of the Feign client
  480 | 	 * @return a {@link Feign} client created with the specified data and the context
  481 | 	 * information
  482 | 	 */
  483 | 	@SuppressWarnings("unchecked")
  484 | 	<T> T getTarget() {
  485 | 		FeignClientFactory feignClientFactory = beanFactory != null ? beanFactory.getBean(FeignClientFactory.class)
  486 | 				: applicationContext.getBean(FeignClientFactory.class);
  487 | 		Feign.Builder builder = feign(feignClientFactory);
  488 | 		if (!StringUtils.hasText(url) && !isUrlAvailableInConfig(contextId)) {
  489 | 
  490 | 			if (LOG.isInfoEnabled()) {
  491 | 				LOG.info("For '" + name + "' URL not provided. Will try picking an instance via load-balancing.");
  492 | 			}
  493 | 			if (!name.startsWith("http://") && !name.startsWith("https://")) {
  494 | 				url = "http://" + name;
  495 | 			}
  496 | 			else {
  497 | 				url = name;
  498 | 			}
  499 | 			url += cleanPath();
  500 | 			return (T) loadBalance(builder, feignClientFactory, new HardCodedTarget<>(type, name, url));
  501 | 		}
  502 | 		if (StringUtils.hasText(url) && !url.startsWith("http://") && !url.startsWith("https://")) {
  503 | 			url = "http://" + url;
  504 | 		}
  505 | 		// NOTE: The Client bean should ideally be configured as a singleton.
  506 | 		// Repeated resolution of Client instances may lead to resource exhaustion
  507 | 		// (e.g., connection pools, threads) if not properly managed.
  508 | 		Client client = getOptional(feignClientFactory, Client.class);
  509 | 		if (client != null) {
  510 | 			warnIfAlreadyResolved();
  511 | 			if (client instanceof FeignBlockingLoadBalancerClient) {
  512 | 				// not load balancing because we have a url,
  513 | 				// but Spring Cloud LoadBalancer is on the classpath, so unwrap
  514 | 				client = ((FeignBlockingLoadBalancerClient) client).getDelegate();
  515 | 			}
  516 | 			if (client instanceof RetryableFeignBlockingLoadBalancerClient) {
  517 | 				// not load balancing because we have a url,
  518 | 				// but Spring Cloud LoadBalancer is on the classpath, so unwrap
  519 | 				client = ((RetryableFeignBlockingLoadBalancerClient) client).getDelegate();
  520 | 			}
  521 | 			builder.client(client);
  522 | 		}
  523 | 
  524 | 		applyBuildCustomizers(feignClientFactory, builder);
  525 | 
  526 | 		Targeter targeter = get(feignClientFactory, Targeter.class);
  527 | 		return targeter.target(this, builder, feignClientFactory, resolveTarget(feignClientFactory, contextId, url));
  528 | 	}
  529 | 
  530 | 	private void warnIfAlreadyResolved() {
  531 | 		Object applicationKey = beanFactory != null ? beanFactory : applicationContext;
  532 | 		if (applicationKey == null) {
  533 | 			return;
  534 | 		}
  535 | 		Set<String> resolved = resolvedContextIds.computeIfAbsent(applicationKey,
  536 | 				key -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
  537 | 		if (!resolved.add(contextId)) {
  538 | 			if (LOG.isWarnEnabled()) {
  539 | 				LOG.warn("FeignClient with contextId '" + contextId + "' is being initialized more than once. "
  540 | 						+ "Ensure the Client bean is Singleton scoped " + "to avoid connection pool exhaustion.");
  541 | 			}
  542 | 		}
  543 | 	}
  544 | 
  545 | 	private String cleanPath() {
  546 | 		if (path == null) {
  547 | 			return "";
  548 | 		}
  549 | 		String path = this.path.trim();
  550 | 		if (StringUtils.hasLength(path)) {
  551 | 			if (!path.startsWith("/")) {
  552 | 				path = "/" + path;
  553 | 			}
  554 | 			if (path.endsWith("/")) {
  555 | 				path = path.substring(0, path.length() - 1);
  556 | 			}
  557 | 		}
  558 | 		return path;
  559 | 	}
  560 | 
  561 | 	@SuppressWarnings({ "unchecked", "rawtypes" })
  562 | 	private <T> HardCodedTarget<T> resolveTarget(FeignClientFactory context, String contextId, String url) {
  563 | 		if (StringUtils.hasText(url)) {
  564 | 			return new HardCodedTarget(type, name, url + cleanPath());
  565 | 		}
  566 | 
  567 | 		if (refreshableClient) {
  568 | 			RefreshableUrl refreshableUrl = context.getInstance(contextId,
  569 | 					RefreshableUrl.class.getCanonicalName() + "-" + contextId, RefreshableUrl.class);
  570 | 			if (Objects.nonNull(refreshableUrl) && StringUtils.hasText(refreshableUrl.getUrl())) {
  571 | 				return new RefreshableHardCodedTarget<>(type, name, refreshableUrl, cleanPath());
  572 | 			}
  573 | 		}
  574 | 		FeignClientProperties.FeignClientConfiguration config = findConfigByKey(contextId);
  575 | 		if (Objects.isNull(config) || !StringUtils.hasText(config.getUrl())) {
  576 | 			throw new IllegalStateException(
  577 | 					"Provide Feign client URL either in @FeignClient() or in config properties.");
  578 | 		}
  579 | 
  580 | 		return new PropertyBasedTarget(type, name, config, cleanPath());
  581 | 	}
  582 | 
  583 | 	private boolean isUrlAvailableInConfig(String contextId) {
  584 | 		FeignClientProperties.FeignClientConfiguration config = findConfigByKey(contextId);
  585 | 		return Objects.nonNull(config) && StringUtils.hasText(config.getUrl());
  586 | 	}
  587 | 
  588 | 	private FeignClientProperties.FeignClientConfiguration findConfigByKey(String configKey) {
  589 | 		FeignClientProperties properties = beanFactory != null ? beanFactory.getBean(FeignClientProperties.class)
  590 | 				: applicationContext.getBean(FeignClientProperties.class);
  591 | 		return properties.getConfig().get(configKey);
  592 | 	}
  593 | 
  594 | 	@Override
  595 | 	public Class<?> getObjectType() {
  596 | 		return type;
  597 | 	}
  598 | 
  599 | 	@Override
  600 | 	public boolean isSingleton() {
  601 | 		return true;
  602 | 	}
  603 | 
  604 | 	public Class<?> getType() {
  605 | 		return type;
  606 | 	}
  607 | 
  608 | 	public void setType(Class<?> type) {
  609 | 		this.type = type;
  610 | 	}
  611 | 
  612 | 	public String getName() {
  613 | 		return name;
  614 | 	}
  615 | 
  616 | 	public void setName(String name) {
  617 | 		this.name = name;
  618 | 	}
  619 | 
  620 | 	public String getContextId() {
  621 | 		return contextId;
  622 | 	}
  623 | 
  624 | 	public void setContextId(String contextId) {
  625 | 		this.contextId = contextId;
  626 | 	}
  627 | 
  628 | 	public String getUrl() {
  629 | 		return url;
  630 | 	}
  631 | 
  632 | 	public void setUrl(String url) {
  633 | 		this.url = url;
  634 | 	}
  635 | 
  636 | 	public String getPath() {
  637 | 		return path;
  638 | 	}
  639 | 
  640 | 	public void setPath(String path) {
  641 | 		this.path = path;
  642 | 	}
  643 | 
  644 | 	public boolean isDismiss404() {
  645 | 		return dismiss404;
  646 | 	}
  647 | 
  648 | 	public void setDismiss404(boolean dismiss404) {
  649 | 		this.dismiss404 = dismiss404;
  650 | 	}
  651 | 
  652 | 	public boolean isInheritParentContext() {
  653 | 		return inheritParentContext;
  654 | 	}
  655 | 
  656 | 	public void setInheritParentContext(boolean inheritParentContext) {
  657 | 		this.inheritParentContext = inheritParentContext;
  658 | 	}
  659 | 
  660 | 	public void addCustomizer(FeignBuilderCustomizer customizer) {
  661 | 		additionalCustomizers.add(customizer);
  662 | 	}
  663 | 
  664 | 	public ApplicationContext getApplicationContext() {
  665 | 		return applicationContext;
  666 | 	}
  667 | 
  668 | 	@Override
  669 | 	public void setApplicationContext(ApplicationContext context) throws BeansException {
  670 | 		applicationContext = context;
  671 | 		beanFactory = context;
  672 | 	}
  673 | 
  674 | 	public Class<?> getFallback() {
  675 | 		return fallback;
  676 | 	}
  677 | 
  678 | 	public void setFallback(Class<?> fallback) {
  679 | 		this.fallback = fallback;
  680 | 	}
  681 | 
  682 | 	public Class<?> getFallbackFactory() {
  683 | 		return fallbackFactory;
  684 | 	}
  685 | 
  686 | 	public void setFallbackFactory(Class<?> fallbackFactory) {
  687 | 		this.fallbackFactory = fallbackFactory;
  688 | 	}
  689 | 
  690 | 	public void setRefreshableClient(boolean refreshableClient) {
  691 | 		this.refreshableClient = refreshableClient;
  692 | 	}
  693 | 
  694 | 	public String[] getQualifiers() {
  695 | 		return qualifiers;
  696 | 	}
  697 | 
  698 | 	public void setQualifiers(String[] qualifiers) {
  699 | 		this.qualifiers = qualifiers;
  700 | 	}
  701 | 
  702 | 	@Override
  703 | 	public boolean equals(Object o) {
  704 | 		if (this == o) {
  705 | 			return true;
  706 | 		}
  707 | 		if (o == null || getClass() != o.getClass()) {
  708 | 			return false;
  709 | 		}
  710 | 		FeignClientFactoryBean that = (FeignClientFactoryBean) o;
  711 | 		return Objects.equals(applicationContext, that.applicationContext)
  712 | 				&& Objects.equals(beanFactory, that.beanFactory) && dismiss404 == that.dismiss404
  713 | 				&& inheritParentContext == that.inheritParentContext && Objects.equals(fallback, that.fallback)
  714 | 				&& Objects.equals(fallbackFactory, that.fallbackFactory) && Objects.equals(name, that.name)
  715 | 				&& Objects.equals(path, that.path) && Objects.equals(type, that.type) && Objects.equals(url, that.url)
  716 | 				&& Objects.equals(connectTimeoutMillis, that.connectTimeoutMillis)
  717 | 				&& Objects.equals(readTimeoutMillis, that.readTimeoutMillis)
  718 | 				&& Objects.equals(followRedirects, that.followRedirects)
  719 | 				&& Objects.equals(refreshableClient, that.refreshableClient);
  720 | 	}
  721 | 
  722 | 	@Override
  723 | 	public int hashCode() {
  724 | 		return Objects.hash(applicationContext, beanFactory, dismiss404, inheritParentContext, fallback,
  725 | 				fallbackFactory, name, path, type, url, readTimeoutMillis, connectTimeoutMillis, followRedirects,
  726 | 				refreshableClient);
  727 | 	}
  728 | 
  729 | 	@Override
  730 | 	public String toString() {
  731 | 		return new StringBuilder("FeignClientFactoryBean{").append("type=")
  732 | 			.append(type)
  733 | 			.append(", ")
  734 | 			.append("name='")
  735 | 			.append(name)
  736 | 			.append("', ")
  737 | 			.append("url='")
  738 | 			.append(url)
  739 | 			.append("', ")
  740 | 			.append("path='")
  741 | 			.append(path)
  742 | 			.append("', ")
  743 | 			.append("dismiss404=")
  744 | 			.append(dismiss404)
  745 | 			.append(", ")
  746 | 			.append("inheritParentContext=")
  747 | 			.append(inheritParentContext)
  748 | 			.append(", ")
  749 | 			.append("applicationContext=")
  750 | 			.append(applicationContext)
  751 | 			.append(", ")
  752 | 			.append("beanFactory=")
  753 | 			.append(beanFactory)
  754 | 			.append(", ")
  755 | 			.append("fallback=")
  756 | 			.append(fallback)
  757 | 			.append(", ")
  758 | 			.append("fallbackFactory=")
  759 | 			.append(fallbackFactory)
  760 | 			.append("}")
  761 | 			.append("connectTimeoutMillis=")
  762 | 			.append(connectTimeoutMillis)
  763 | 			.append("}")
  764 | 			.append("readTimeoutMillis=")
  765 | 			.append(readTimeoutMillis)
  766 | 			.append("}")
  767 | 			.append("followRedirects=")
  768 | 			.append(followRedirects)
  769 | 			.append("refreshableClient=")
  770 | 			.append(refreshableClient)
  771 | 			.append("}")
  772 | 			.toString();
  773 | 	}
  774 | 
  775 | 	@Override
  776 | 	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
  777 | 		this.beanFactory = beanFactory;
  778 | 	}
  779 | 
  780 | }
```
