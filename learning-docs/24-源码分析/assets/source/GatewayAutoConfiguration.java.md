<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/config/GatewayAutoConfiguration.java -->
<!-- 正文引用：[[源码:GatewayAutoConfiguration.java#L起-L止]] ；代码块头部：(src: GatewayAutoConfiguration.java#L起-L止) -->

# GatewayAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/config/GatewayAutoConfiguration.java` |
| 行数 | 1039 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:50.152Z |

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
   17 | package org.springframework.cloud.gateway.config;
   18 | 
   19 | import java.io.IOException;
   20 | import java.net.InetSocketAddress;
   21 | import java.security.KeyStore;
   22 | import java.security.KeyStoreException;
   23 | import java.security.NoSuchAlgorithmException;
   24 | import java.security.cert.CertificateException;
   25 | import java.util.List;
   26 | import java.util.Objects;
   27 | import java.util.Set;
   28 | import java.util.function.Supplier;
   29 | 
   30 | import javax.net.ssl.TrustManagerFactory;
   31 | 
   32 | import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
   33 | import org.apache.commons.logging.Log;
   34 | import org.apache.commons.logging.LogFactory;
   35 | import org.jspecify.annotations.Nullable;
   36 | import reactor.core.publisher.Flux;
   37 | import reactor.netty.http.client.HttpClient;
   38 | import reactor.netty.http.client.WebsocketClientSpec;
   39 | import reactor.netty.http.server.WebsocketServerSpec;
   40 | 
   41 | import org.springframework.aot.hint.MemberCategory;
   42 | import org.springframework.aot.hint.RuntimeHints;
   43 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   44 | import org.springframework.aot.hint.TypeReference;
   45 | import org.springframework.beans.factory.BeanFactory;
   46 | import org.springframework.beans.factory.ObjectProvider;
   47 | import org.springframework.beans.factory.annotation.Qualifier;
   48 | import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
   49 | import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
   50 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   51 | import org.springframework.boot.autoconfigure.AutoConfigureBefore;
   52 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   53 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   54 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   55 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   56 | import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
   57 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   58 | import org.springframework.boot.context.properties.PropertyMapper;
   59 | import org.springframework.boot.http.codec.CodecCustomizer;
   60 | import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
   61 | import org.springframework.boot.reactor.netty.NettyServerCustomizer;
   62 | import org.springframework.boot.reactor.netty.autoconfigure.NettyReactiveWebServerFactoryCustomizer;
   63 | import org.springframework.boot.reactor.netty.autoconfigure.NettyServerProperties;
   64 | import org.springframework.boot.security.autoconfigure.SecurityProperties;
   65 | import org.springframework.boot.ssl.SslBundles;
   66 | import org.springframework.boot.web.server.autoconfigure.ServerProperties;
   67 | import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
   68 | import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
   69 | import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
   70 | import org.springframework.boot.webflux.autoconfigure.WebFluxProperties.Apiversion;
   71 | import org.springframework.cloud.gateway.actuate.GatewayControllerEndpoint;
   72 | import org.springframework.cloud.gateway.actuate.GatewayLegacyControllerEndpoint;
   73 | import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledFilter;
   74 | import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledGlobalFilter;
   75 | import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledPredicate;
   76 | import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
   77 | import org.springframework.cloud.gateway.filter.FilterDefinition;
   78 | import org.springframework.cloud.gateway.filter.ForwardPathFilter;
   79 | import org.springframework.cloud.gateway.filter.ForwardRoutingFilter;
   80 | import org.springframework.cloud.gateway.filter.GlobalFilter;
   81 | import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
   82 | import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
   83 | import org.springframework.cloud.gateway.filter.RemoveCachedBodyFilter;
   84 | import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
   85 | import org.springframework.cloud.gateway.filter.WebsocketRoutingFilter;
   86 | import org.springframework.cloud.gateway.filter.WeightCalculatorWebFilter;
   87 | import org.springframework.cloud.gateway.filter.cors.CorsGatewayFilterApplicationListener;
   88 | import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
   89 | import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
   90 | import org.springframework.cloud.gateway.filter.factory.AddRequestHeadersIfNotPresentGatewayFilterFactory;
   91 | import org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory;
   92 | import org.springframework.cloud.gateway.filter.factory.AddResponseHeaderGatewayFilterFactory;
   93 | import org.springframework.cloud.gateway.filter.factory.CacheRequestBodyGatewayFilterFactory;
   94 | import org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory;
   95 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   96 | import org.springframework.cloud.gateway.filter.factory.JsonToGrpcGatewayFilterFactory;
   97 | import org.springframework.cloud.gateway.filter.factory.MapRequestHeaderGatewayFilterFactory;
   98 | import org.springframework.cloud.gateway.filter.factory.PrefixPathGatewayFilterFactory;
   99 | import org.springframework.cloud.gateway.filter.factory.PreserveHostHeaderGatewayFilterFactory;
  100 | import org.springframework.cloud.gateway.filter.factory.RedirectToGatewayFilterFactory;
  101 | import org.springframework.cloud.gateway.filter.factory.RemoveJsonAttributesResponseBodyGatewayFilterFactory;
  102 | import org.springframework.cloud.gateway.filter.factory.RemoveRequestHeaderGatewayFilterFactory;
  103 | import org.springframework.cloud.gateway.filter.factory.RemoveRequestParameterGatewayFilterFactory;
  104 | import org.springframework.cloud.gateway.filter.factory.RemoveResponseHeaderGatewayFilterFactory;
  105 | import org.springframework.cloud.gateway.filter.factory.RequestHeaderSizeGatewayFilterFactory;
  106 | import org.springframework.cloud.gateway.filter.factory.RequestHeaderToRequestUriGatewayFilterFactory;
  107 | import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
  108 | import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory;
  109 | import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
  110 | import org.springframework.cloud.gateway.filter.factory.RewriteLocationResponseHeaderGatewayFilterFactory;
  111 | import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
  112 | import org.springframework.cloud.gateway.filter.factory.RewriteRequestParameterGatewayFilterFactory;
  113 | import org.springframework.cloud.gateway.filter.factory.RewriteResponseHeaderGatewayFilterFactory;
  114 | import org.springframework.cloud.gateway.filter.factory.SaveSessionGatewayFilterFactory;
  115 | import org.springframework.cloud.gateway.filter.factory.SecureHeadersGatewayFilterFactory;
  116 | import org.springframework.cloud.gateway.filter.factory.SecureHeadersProperties;
  117 | import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory;
  118 | import org.springframework.cloud.gateway.filter.factory.SetRequestHeaderGatewayFilterFactory;
  119 | import org.springframework.cloud.gateway.filter.factory.SetRequestHostHeaderGatewayFilterFactory;
  120 | import org.springframework.cloud.gateway.filter.factory.SetRequestUriGatewayFilterFactory;
  121 | import org.springframework.cloud.gateway.filter.factory.SetResponseHeaderGatewayFilterFactory;
  122 | import org.springframework.cloud.gateway.filter.factory.SetStatusGatewayFilterFactory;
  123 | import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
  124 | import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory;
  125 | import org.springframework.cloud.gateway.filter.factory.rewrite.GzipMessageBodyResolver;
  126 | import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
  127 | import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyEncoder;
  128 | import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
  129 | import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
  130 | import org.springframework.cloud.gateway.filter.headers.ForwardedHeadersFilter;
  131 | import org.springframework.cloud.gateway.filter.headers.GRPCRequestHeadersFilter;
  132 | import org.springframework.cloud.gateway.filter.headers.GRPCResponseHeadersFilter;
  133 | import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
  134 | import org.springframework.cloud.gateway.filter.headers.RemoveForwardedHeadersFilter;
  135 | import org.springframework.cloud.gateway.filter.headers.RemoveHopByHopHeadersFilter;
  136 | import org.springframework.cloud.gateway.filter.headers.RemoveXForwardedHeadersFilter;
  137 | import org.springframework.cloud.gateway.filter.headers.TransferEncodingNormalizationHeadersFilter;
  138 | import org.springframework.cloud.gateway.filter.headers.TrustedProxies;
  139 | import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
  140 | import org.springframework.cloud.gateway.filter.ratelimit.Bucket4jRateLimiter;
  141 | import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
  142 | import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver;
  143 | import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
  144 | import org.springframework.cloud.gateway.handler.FilteringWebHandler;
  145 | import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
  146 | import org.springframework.cloud.gateway.handler.predicate.AfterRoutePredicateFactory;
  147 | import org.springframework.cloud.gateway.handler.predicate.BeforeRoutePredicateFactory;
  148 | import org.springframework.cloud.gateway.handler.predicate.BetweenRoutePredicateFactory;
  149 | import org.springframework.cloud.gateway.handler.predicate.CloudFoundryRouteServiceRoutePredicateFactory;
  150 | import org.springframework.cloud.gateway.handler.predicate.CookieRoutePredicateFactory;
  151 | import org.springframework.cloud.gateway.handler.predicate.HeaderRoutePredicateFactory;
  152 | import org.springframework.cloud.gateway.handler.predicate.HostRoutePredicateFactory;
  153 | import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
  154 | import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
  155 | import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
  156 | import org.springframework.cloud.gateway.handler.predicate.QueryRoutePredicateFactory;
  157 | import org.springframework.cloud.gateway.handler.predicate.ReadBodyRoutePredicateFactory;
  158 | import org.springframework.cloud.gateway.handler.predicate.RemoteAddrRoutePredicateFactory;
  159 | import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
  160 | import org.springframework.cloud.gateway.handler.predicate.VersionRoutePredicateFactory;
  161 | import org.springframework.cloud.gateway.handler.predicate.WeightRoutePredicateFactory;
  162 | import org.springframework.cloud.gateway.handler.predicate.XForwardedRemoteAddrRoutePredicateFactory;
  163 | import org.springframework.cloud.gateway.route.CachingRouteLocator;
  164 | import org.springframework.cloud.gateway.route.CompositeRouteDefinitionLocator;
  165 | import org.springframework.cloud.gateway.route.CompositeRouteLocator;
  166 | import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
  167 | import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
  168 | import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
  169 | import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
  170 | import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
  171 | import org.springframework.cloud.gateway.route.RouteLocator;
  172 | import org.springframework.cloud.gateway.route.RouteRefreshListener;
  173 | import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
  174 | import org.springframework.cloud.gateway.support.ConfigurationService;
  175 | import org.springframework.cloud.gateway.support.StringToZonedDateTimeConverter;
  176 | import org.springframework.cloud.gateway.support.config.KeyValueConverter;
  177 | import org.springframework.context.ApplicationEventPublisher;
  178 | import org.springframework.context.ConfigurableApplicationContext;
  179 | import org.springframework.context.annotation.Bean;
  180 | import org.springframework.context.annotation.Conditional;
  181 | import org.springframework.context.annotation.Configuration;
  182 | import org.springframework.context.annotation.DependsOn;
  183 | import org.springframework.context.annotation.Primary;
  184 | import org.springframework.core.convert.ConversionService;
  185 | import org.springframework.core.env.Environment;
  186 | import org.springframework.core.io.ResourceLoader;
  187 | import org.springframework.http.codec.ServerCodecConfigurer;
  188 | import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
  189 | import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
  190 | import org.springframework.security.web.server.SecurityWebFilterChain;
  191 | import org.springframework.util.ClassUtils;
  192 | import org.springframework.util.CollectionUtils;
  193 | import org.springframework.util.StringUtils;
  194 | import org.springframework.validation.Validator;
  195 | import org.springframework.web.reactive.DispatcherHandler;
  196 | import org.springframework.web.reactive.accept.ApiVersionStrategy;
  197 | import org.springframework.web.reactive.config.ApiVersionConfigurer;
  198 | import org.springframework.web.reactive.config.WebFluxConfigurer;
  199 | import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
  200 | import org.springframework.web.reactive.socket.client.WebSocketClient;
  201 | import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
  202 | import org.springframework.web.reactive.socket.server.WebSocketService;
  203 | import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
  204 | import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
  205 | 
  206 | /**
  207 |  * @author Spencer Gibb
  208 |  * @author Ziemowit Stolarczyk
  209 |  * @author Mete Alpaslan Katırcıoğlu
  210 |  * @author Alberto C. Ríos
  211 |  * @author Olga Maciaszek-Sharma
  212 |  * @author FuYiNan Guo
  213 |  */
  214 | @Configuration(proxyBeanMethods = false)
  215 | @ConditionalOnProperty(name = "spring.cloud.gateway.server.webflux.enabled", matchIfMissing = true)
  216 | @EnableConfigurationProperties
  217 | @AutoConfigureBefore({ HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class })
  218 | @AutoConfigureAfter({ GatewayReactiveLoadBalancerClientAutoConfiguration.class,
  219 | 		GatewayClassPathWarningAutoConfiguration.class })
  220 | @ConditionalOnClass(DispatcherHandler.class)
  221 | public class GatewayAutoConfiguration {
  222 | 
  223 | 	@Bean
  224 | 	public StringToZonedDateTimeConverter stringToZonedDateTimeConverter() {
  225 | 		return new StringToZonedDateTimeConverter();
  226 | 	}
  227 | 
  228 | 	@Bean
  229 | 	public KeyValueConverter keyValueConverter() {
  230 | 		return new KeyValueConverter();
  231 | 	}
  232 | 
  233 | 	@Bean
  234 | 	public RouteLocatorBuilder routeLocatorBuilder(ConfigurableApplicationContext context) {
  235 | 		return new RouteLocatorBuilder(context);
  236 | 	}
  237 | 
  238 | 	@Bean
  239 | 	@ConditionalOnMissingBean
  240 | 	public PropertiesRouteDefinitionLocator propertiesRouteDefinitionLocator(GatewayProperties properties) {
  241 | 		return new PropertiesRouteDefinitionLocator(properties);
  242 | 	}
  243 | 
  244 | 	@Bean
  245 | 	@ConditionalOnMissingBean(RouteDefinitionRepository.class)
  246 | 	public InMemoryRouteDefinitionRepository inMemoryRouteDefinitionRepository() {
  247 | 		return new InMemoryRouteDefinitionRepository();
  248 | 	}
  249 | 
  250 | 	@Bean
  251 | 	@Primary
  252 | 	public RouteDefinitionLocator routeDefinitionLocator(List<RouteDefinitionLocator> routeDefinitionLocators) {
  253 | 		return new CompositeRouteDefinitionLocator(Flux.fromIterable(routeDefinitionLocators));
  254 | 	}
  255 | 
  256 | 	@Bean
  257 | 	public ConfigurationService gatewayConfigurationService(BeanFactory beanFactory,
  258 | 			@Qualifier("webFluxConversionService") ObjectProvider<ConversionService> conversionService,
  259 | 			ObjectProvider<Validator> validator) {
  260 | 		return new ConfigurationService(beanFactory, conversionService, validator);
  261 | 	}
  262 | 
  263 | 	@Bean
  264 | 	public RouteLocator routeDefinitionRouteLocator(GatewayProperties properties,
  265 | 			List<GatewayFilterFactory> gatewayFilters, List<RoutePredicateFactory> predicates,
  266 | 			RouteDefinitionLocator routeDefinitionLocator, ConfigurationService configurationService) {
  267 | 		return new RouteDefinitionRouteLocator(routeDefinitionLocator, predicates, gatewayFilters, properties,
  268 | 				configurationService);
  269 | 	}
  270 | 
  271 | 	@Bean
  272 | 	@Primary
  273 | 	@ConditionalOnMissingBean(name = "cachedCompositeRouteLocator")
  274 | 	// TODO: property to disable composite?
  275 | 	public RouteLocator cachedCompositeRouteLocator(List<RouteLocator> routeLocators) {
  276 | 		return new CachingRouteLocator(new CompositeRouteLocator(Flux.fromIterable(routeLocators)));
  277 | 	}
  278 | 
  279 | 	@Bean
  280 | 	@ConditionalOnClass(name = "org.springframework.cloud.client.discovery.event.HeartbeatMonitor")
  281 | 	@ConditionalOnProperty(prefix = GatewayProperties.PREFIX, name = "route-refresh-listener.enabled",
  282 | 			matchIfMissing = true)
  283 | 	public RouteRefreshListener routeRefreshListener(ApplicationEventPublisher publisher) {
  284 | 		return new RouteRefreshListener(publisher);
  285 | 	}
  286 | 
  287 | 	@Bean
  288 | 	@ConditionalOnMissingBean
  289 | 	public FilteringWebHandler filteringWebHandler(List<GlobalFilter> globalFilters, GatewayProperties properties) {
  290 | 		return new FilteringWebHandler(globalFilters, properties.isRouteFilterCacheEnabled());
  291 | 	}
  292 | 
  293 | 	@Bean
  294 | 	public GlobalCorsProperties globalCorsProperties() {
  295 | 		return new GlobalCorsProperties();
  296 | 	}
  297 | 
  298 | 	@Bean
  299 | 	@ConditionalOnProperty(name = "spring.cloud.gateway.server.webflux.globalcors.enabled", matchIfMissing = true)
  300 | 	public CorsGatewayFilterApplicationListener corsGatewayFilterApplicationListener(
  301 | 			GlobalCorsProperties globalCorsProperties, RoutePredicateHandlerMapping routePredicateHandlerMapping,
  302 | 			RouteLocator routeLocator) {
  303 | 		return new CorsGatewayFilterApplicationListener(globalCorsProperties, routePredicateHandlerMapping,
  304 | 				routeLocator);
  305 | 	}
  306 | 
  307 | 	@Bean
  308 | 	@ConditionalOnMissingBean
  309 | 	public RoutePredicateHandlerMapping routePredicateHandlerMapping(FilteringWebHandler webHandler,
  310 | 			RouteLocator routeLocator, GlobalCorsProperties globalCorsProperties, Environment environment,
  311 | 			ObjectProvider<ApiVersionStrategy> apiVersionStrategy) {
  312 | 		RoutePredicateHandlerMapping handlerMapping = new RoutePredicateHandlerMapping(webHandler, routeLocator,
  313 | 				globalCorsProperties, environment);
  314 | 		apiVersionStrategy.ifAvailable(handlerMapping::setApiVersionStrategy);
  315 | 		return handlerMapping;
  316 | 	}
  317 | 
  318 | 	// ConfigurationProperty beans
  319 | 
  320 | 	@Bean
  321 | 	public GatewayProperties gatewayProperties() {
  322 | 		return new GatewayProperties();
  323 | 	}
  324 | 
  325 | 	@Bean
  326 | 	public WebsocketProperties websocketProperties() {
  327 | 		return new WebsocketProperties();
  328 | 	}
  329 | 
  330 | 	@Bean
  331 | 	public SecureHeadersProperties secureHeadersProperties() {
  332 | 		return new SecureHeadersProperties();
  333 | 	}
  334 | 
  335 | 	@Bean
  336 | 	public VersionProperties versionProperties() {
  337 | 		return new VersionProperties();
  338 | 	}
  339 | 
  340 | 	@Bean
  341 | 	@Conditional(TrustedProxies.ForwardedTrustedProxiesCondition.class)
  342 | 	public ForwardedHeadersFilter forwardedHeadersFilter(Environment env, ServerProperties serverProperties,
  343 | 			GatewayProperties properties) {
  344 | 		boolean forwardedByEnabled = env.getProperty("spring.cloud.gateway.server.webflux.forwarded.by.enabled",
  345 | 				Boolean.class, false);
  346 | 		ForwardedHeadersFilter forwardedHeadersFilter = new ForwardedHeadersFilter(properties.getTrustedProxies());
  347 | 		forwardedHeadersFilter.setForwardedByEnabled(forwardedByEnabled);
  348 | 		forwardedHeadersFilter.setServerPort(serverProperties.getPort());
  349 | 		return forwardedHeadersFilter;
  350 | 	}
  351 | 
  352 | 	@Bean
  353 | 	@Conditional(TrustedProxies.NotForwardedTrustedProxiesCondition.class)
  354 | 	public RemoveForwardedHeadersFilter removeForwardedHeadersFilter() {
  355 | 		return new RemoveForwardedHeadersFilter();
  356 | 	}
  357 | 
  358 | 	// HttpHeaderFilter beans
  359 | 
  360 | 	@Bean
  361 | 	public RemoveHopByHopHeadersFilter removeHopByHopHeadersFilter() {
  362 | 		return new RemoveHopByHopHeadersFilter();
  363 | 	}
  364 | 
  365 | 	@Bean
  366 | 	@Conditional(TrustedProxies.XForwardedTrustedProxiesCondition.class)
  367 | 	public XForwardedHeadersFilter xForwardedHeadersFilter(GatewayProperties properties) {
  368 | 		return new XForwardedHeadersFilter(properties.getTrustedProxies());
  369 | 	}
  370 | 
  371 | 	@Bean
  372 | 	@Conditional(TrustedProxies.NotXForwardedTrustedProxiesCondition.class)
  373 | 	public RemoveXForwardedHeadersFilter removeXForwardedHeadersFilter() {
  374 | 		return new RemoveXForwardedHeadersFilter();
  375 | 	}
  376 | 
  377 | 	@Bean
  378 | 	@ConditionalOnProperty(name = "server.http2.enabled", matchIfMissing = true)
  379 | 	public GRPCRequestHeadersFilter gRPCRequestHeadersFilter() {
  380 | 		return new GRPCRequestHeadersFilter();
  381 | 	}
  382 | 
  383 | 	@Bean
  384 | 	@ConditionalOnProperty(name = "server.http2.enabled", matchIfMissing = true)
  385 | 	public GRPCResponseHeadersFilter gRPCResponseHeadersFilter() {
  386 | 		return new GRPCResponseHeadersFilter();
  387 | 	}
  388 | 
  389 | 	@Bean
  390 | 	@ConditionalOnEnabledFilter
  391 | 	@ConditionalOnProperty(name = "server.http2.enabled", matchIfMissing = true)
  392 | 	@ConditionalOnClass(name = "io.grpc.Channel")
  393 | 	public JsonToGrpcGatewayFilterFactory jsonToGRPCFilterFactory(GrpcSslConfigurer gRPCSSLContext,
  394 | 			ResourceLoader resourceLoader) {
  395 | 		return new JsonToGrpcGatewayFilterFactory(gRPCSSLContext, resourceLoader);
  396 | 	}
  397 | 
  398 | 	@Bean
  399 | 	@ConditionalOnEnabledFilter(JsonToGrpcGatewayFilterFactory.class)
  400 | 	@ConditionalOnMissingBean(GrpcSslConfigurer.class)
  401 | 	@ConditionalOnClass(name = "io.grpc.Channel")
  402 | 	public GrpcSslConfigurer grpcSslConfigurer(HttpClientProperties properties, SslBundles bundles)
  403 | 			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
  404 | 		TrustManagerFactory trustManagerFactory = TrustManagerFactory
  405 | 			.getInstance(TrustManagerFactory.getDefaultAlgorithm());
  406 | 		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
  407 | 		keyStore.load(null);
  408 | 		trustManagerFactory.init(keyStore);
  409 | 
  410 | 		return new GrpcSslConfigurer(properties.getSsl(), bundles);
  411 | 	}
  412 | 
  413 | 	@Bean
  414 | 	public TransferEncodingNormalizationHeadersFilter transferEncodingNormalizationHeadersFilter() {
  415 | 		return new TransferEncodingNormalizationHeadersFilter();
  416 | 	}
  417 | 
  418 | 	// GlobalFilter beans
  419 | 
  420 | 	@Bean
  421 | 	@ConditionalOnEnabledGlobalFilter
  422 | 	public AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter() {
  423 | 		return new AdaptCachedBodyGlobalFilter();
  424 | 	}
  425 | 
  426 | 	@Bean
  427 | 	@ConditionalOnEnabledGlobalFilter
  428 | 	public RemoveCachedBodyFilter removeCachedBodyFilter() {
  429 | 		return new RemoveCachedBodyFilter();
  430 | 	}
  431 | 
  432 | 	@Bean
  433 | 	@ConditionalOnEnabledGlobalFilter
  434 | 	public RouteToRequestUrlFilter routeToRequestUrlFilter() {
  435 | 		return new RouteToRequestUrlFilter();
  436 | 	}
  437 | 
  438 | 	@Bean
  439 | 	@ConditionalOnEnabledGlobalFilter
  440 | 	public ForwardRoutingFilter forwardRoutingFilter(ObjectProvider<DispatcherHandler> dispatcherHandler) {
  441 | 		return new ForwardRoutingFilter(dispatcherHandler);
  442 | 	}
  443 | 
  444 | 	@Bean
  445 | 	@ConditionalOnEnabledGlobalFilter
  446 | 	public ForwardPathFilter forwardPathFilter() {
  447 | 		return new ForwardPathFilter();
  448 | 	}
  449 | 
  450 | 	@Bean
  451 | 	@ConditionalOnEnabledGlobalFilter(WebsocketRoutingFilter.class)
  452 | 	public WebSocketService webSocketService(RequestUpgradeStrategy requestUpgradeStrategy) {
  453 | 		return new HandshakeWebSocketService(requestUpgradeStrategy);
  454 | 	}
  455 | 
  456 | 	@Bean
  457 | 	@ConditionalOnEnabledGlobalFilter
  458 | 	public WebsocketRoutingFilter websocketRoutingFilter(WebSocketClient webSocketClient,
  459 | 			WebSocketService webSocketService, ObjectProvider<List<HttpHeadersFilter>> headersFilters,
  460 | 			WebsocketProperties websocketProperties) {
  461 | 		WebsocketRoutingFilter filter = new WebsocketRoutingFilter(webSocketClient, webSocketService, headersFilters);
  462 | 		filter.setAllowedOrigins(websocketProperties.getAllowedOrigins());
  463 | 		filter.setAllowedOriginPatterns(websocketProperties.getAllowedOriginPatterns());
  464 | 		return filter;
  465 | 	}
  466 | 
  467 | 	@Bean
  468 | 	@ConditionalOnEnabledPredicate(WeightRoutePredicateFactory.class)
  469 | 	public WeightCalculatorWebFilter weightCalculatorWebFilter(ConfigurationService configurationService,
  470 | 			ObjectProvider<RouteLocator> routeLocator) {
  471 | 		return new WeightCalculatorWebFilter(routeLocator, configurationService);
  472 | 	}
  473 | 
  474 | 	// Predicate Factory beans
  475 | 
  476 | 	@Bean
  477 | 	@ConditionalOnEnabledPredicate
  478 | 	public AfterRoutePredicateFactory afterRoutePredicateFactory() {
  479 | 		return new AfterRoutePredicateFactory();
  480 | 	}
  481 | 
  482 | 	@Bean
  483 | 	@ConditionalOnEnabledPredicate
  484 | 	public BeforeRoutePredicateFactory beforeRoutePredicateFactory() {
  485 | 		return new BeforeRoutePredicateFactory();
  486 | 	}
  487 | 
  488 | 	@Bean
  489 | 	@ConditionalOnEnabledPredicate
  490 | 	public BetweenRoutePredicateFactory betweenRoutePredicateFactory() {
  491 | 		return new BetweenRoutePredicateFactory();
  492 | 	}
  493 | 
  494 | 	@Bean
  495 | 	@ConditionalOnEnabledPredicate
  496 | 	public CookieRoutePredicateFactory cookieRoutePredicateFactory() {
  497 | 		return new CookieRoutePredicateFactory();
  498 | 	}
  499 | 
  500 | 	@Bean
  501 | 	@ConditionalOnEnabledPredicate
  502 | 	public HeaderRoutePredicateFactory headerRoutePredicateFactory() {
  503 | 		return new HeaderRoutePredicateFactory();
  504 | 	}
  505 | 
  506 | 	@Bean
  507 | 	@ConditionalOnEnabledPredicate
  508 | 	public HostRoutePredicateFactory hostRoutePredicateFactory(Environment env) {
  509 | 		boolean includePort = env.getProperty("spring.cloud.gateway.server.webflux.predicate.host.include-port",
  510 | 				Boolean.class, true);
  511 | 		return new HostRoutePredicateFactory(includePort);
  512 | 	}
  513 | 
  514 | 	@Bean
  515 | 	@ConditionalOnEnabledPredicate
  516 | 	public MethodRoutePredicateFactory methodRoutePredicateFactory() {
  517 | 		return new MethodRoutePredicateFactory();
  518 | 	}
  519 | 
  520 | 	@Bean
  521 | 	@ConditionalOnEnabledPredicate
  522 | 	public PathRoutePredicateFactory pathRoutePredicateFactory(WebFluxProperties webFluxProperties) {
  523 | 		return new PathRoutePredicateFactory(webFluxProperties);
  524 | 	}
  525 | 
  526 | 	@Bean
  527 | 	@ConditionalOnEnabledPredicate
  528 | 	public QueryRoutePredicateFactory queryRoutePredicateFactory() {
  529 | 		return new QueryRoutePredicateFactory();
  530 | 	}
  531 | 
  532 | 	@Bean
  533 | 	@ConditionalOnEnabledPredicate
  534 | 	public ReadBodyRoutePredicateFactory readBodyPredicateFactory(ServerCodecConfigurer codecConfigurer) {
  535 | 		return new ReadBodyRoutePredicateFactory(codecConfigurer.getReaders());
  536 | 	}
  537 | 
  538 | 	@Bean
  539 | 	@ConditionalOnEnabledPredicate
  540 | 	public RemoteAddrRoutePredicateFactory remoteAddrRoutePredicateFactory() {
  541 | 		return new RemoteAddrRoutePredicateFactory();
  542 | 	}
  543 | 
  544 | 	@Bean
  545 | 	@ConditionalOnEnabledPredicate
  546 | 	public VersionRoutePredicateFactory versionRoutePredicateFactory(
  547 | 			@Qualifier("webFluxApiVersionStrategy") @Nullable ApiVersionStrategy apiVersionStrategy) {
  548 | 		return new VersionRoutePredicateFactory(apiVersionStrategy);
  549 | 	}
  550 | 
  551 | 	@Bean
  552 | 	@ConditionalOnEnabledPredicate
  553 | 	public XForwardedRemoteAddrRoutePredicateFactory xForwardedRemoteAddrRoutePredicateFactory() {
  554 | 		return new XForwardedRemoteAddrRoutePredicateFactory();
  555 | 	}
  556 | 
  557 | 	@Bean
  558 | 	@DependsOn("weightCalculatorWebFilter")
  559 | 	@ConditionalOnEnabledPredicate
  560 | 	public WeightRoutePredicateFactory weightRoutePredicateFactory() {
  561 | 		return new WeightRoutePredicateFactory();
  562 | 	}
  563 | 
  564 | 	@Bean
  565 | 	@ConditionalOnEnabledPredicate
  566 | 	public CloudFoundryRouteServiceRoutePredicateFactory cloudFoundryRouteServiceRoutePredicateFactory() {
  567 | 		return new CloudFoundryRouteServiceRoutePredicateFactory();
  568 | 	}
  569 | 
  570 | 	// GatewayFilter Factory beans
  571 | 
  572 | 	@Bean
  573 | 	@ConditionalOnEnabledFilter
  574 | 	public AddRequestHeaderGatewayFilterFactory addRequestHeaderGatewayFilterFactory() {
  575 | 		return new AddRequestHeaderGatewayFilterFactory();
  576 | 	}
  577 | 
  578 | 	@Bean
  579 | 	@ConditionalOnEnabledFilter
  580 | 	public AddRequestHeadersIfNotPresentGatewayFilterFactory addRequestHeadersIfNotPresentGatewayFilterFactory() {
  581 | 		return new AddRequestHeadersIfNotPresentGatewayFilterFactory();
  582 | 	}
  583 | 
  584 | 	@Bean
  585 | 	@ConditionalOnEnabledFilter
  586 | 	public MapRequestHeaderGatewayFilterFactory mapRequestHeaderGatewayFilterFactory() {
  587 | 		return new MapRequestHeaderGatewayFilterFactory();
  588 | 	}
  589 | 
  590 | 	@Bean
  591 | 	@ConditionalOnEnabledFilter
  592 | 	public AddRequestParameterGatewayFilterFactory addRequestParameterGatewayFilterFactory() {
  593 | 		return new AddRequestParameterGatewayFilterFactory();
  594 | 	}
  595 | 
  596 | 	@Bean
  597 | 	@ConditionalOnEnabledFilter
  598 | 	public AddResponseHeaderGatewayFilterFactory addResponseHeaderGatewayFilterFactory() {
  599 | 		return new AddResponseHeaderGatewayFilterFactory();
  600 | 	}
  601 | 
  602 | 	@Bean
  603 | 	@ConditionalOnEnabledFilter
  604 | 	public ModifyRequestBodyGatewayFilterFactory modifyRequestBodyGatewayFilterFactory(
  605 | 			ServerCodecConfigurer codecConfigurer, ObjectProvider<CodecCustomizer> codecCustomizers) {
  606 | 		return new ModifyRequestBodyGatewayFilterFactory(codecConfigurer.getReaders(),
  607 | 				codecCustomizers.orderedStream().toList());
  608 | 	}
  609 | 
  610 | 	@Bean
  611 | 	@ConditionalOnEnabledFilter
  612 | 	public DedupeResponseHeaderGatewayFilterFactory dedupeResponseHeaderGatewayFilterFactory() {
  613 | 		return new DedupeResponseHeaderGatewayFilterFactory();
  614 | 	}
  615 | 
  616 | 	@Bean
  617 | 	@ConditionalOnEnabledFilter
  618 | 	public ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory(
  619 | 			ServerCodecConfigurer codecConfigurer, Set<MessageBodyDecoder> bodyDecoders,
  620 | 			Set<MessageBodyEncoder> bodyEncoders, ObjectProvider<CodecCustomizer> codecCustomizers) {
  621 | 		return new ModifyResponseBodyGatewayFilterFactory(codecConfigurer.getReaders(), bodyDecoders, bodyEncoders,
  622 | 				codecCustomizers.orderedStream().toList());
  623 | 	}
  624 | 
  625 | 	@Bean
  626 | 	@ConditionalOnEnabledFilter
  627 | 	public CacheRequestBodyGatewayFilterFactory cacheRequestBodyGatewayFilterFactory(
  628 | 			ServerCodecConfigurer codecConfigurer) {
  629 | 		return new CacheRequestBodyGatewayFilterFactory(codecConfigurer.getReaders());
  630 | 	}
  631 | 
  632 | 	@Bean
  633 | 	@ConditionalOnEnabledFilter
  634 | 	public PrefixPathGatewayFilterFactory prefixPathGatewayFilterFactory() {
  635 | 		return new PrefixPathGatewayFilterFactory();
  636 | 	}
  637 | 
  638 | 	@Bean
  639 | 	@ConditionalOnEnabledFilter
  640 | 	public PreserveHostHeaderGatewayFilterFactory preserveHostHeaderGatewayFilterFactory() {
  641 | 		return new PreserveHostHeaderGatewayFilterFactory();
  642 | 	}
  643 | 
  644 | 	@Bean
  645 | 	@ConditionalOnEnabledFilter
  646 | 	public RedirectToGatewayFilterFactory redirectToGatewayFilterFactory() {
  647 | 		return new RedirectToGatewayFilterFactory();
  648 | 	}
  649 | 
  650 | 	@Bean
  651 | 	@ConditionalOnEnabledFilter
  652 | 	public RemoveJsonAttributesResponseBodyGatewayFilterFactory removeJsonAttributesResponseBodyGatewayFilterFactory(
  653 | 			ServerCodecConfigurer codecConfigurer, Set<MessageBodyDecoder> bodyDecoders,
  654 | 			Set<MessageBodyEncoder> bodyEncoders, ObjectProvider<CodecCustomizer> codecCustomizers) {
  655 | 		return new RemoveJsonAttributesResponseBodyGatewayFilterFactory(new ModifyResponseBodyGatewayFilterFactory(
  656 | 				codecConfigurer.getReaders(), bodyDecoders, bodyEncoders, codecCustomizers.orderedStream().toList()));
  657 | 	}
  658 | 
  659 | 	@Bean
  660 | 	@ConditionalOnEnabledFilter
  661 | 	public RemoveRequestHeaderGatewayFilterFactory removeRequestHeaderGatewayFilterFactory() {
  662 | 		return new RemoveRequestHeaderGatewayFilterFactory();
  663 | 	}
  664 | 
  665 | 	@Bean
  666 | 	@ConditionalOnEnabledFilter
  667 | 	public RemoveRequestParameterGatewayFilterFactory removeRequestParameterGatewayFilterFactory() {
  668 | 		return new RemoveRequestParameterGatewayFilterFactory();
  669 | 	}
  670 | 
  671 | 	@Bean
  672 | 	@ConditionalOnEnabledFilter
  673 | 	public RemoveResponseHeaderGatewayFilterFactory removeResponseHeaderGatewayFilterFactory() {
  674 | 		return new RemoveResponseHeaderGatewayFilterFactory();
  675 | 	}
  676 | 
  677 | 	@Bean(name = PrincipalNameKeyResolver.BEAN_NAME)
  678 | 	@ConditionalOnBean(RateLimiter.class)
  679 | 	@ConditionalOnMissingBean(KeyResolver.class)
  680 | 	@ConditionalOnEnabledFilter(RequestRateLimiterGatewayFilterFactory.class)
  681 | 	public PrincipalNameKeyResolver principalNameKeyResolver() {
  682 | 		return new PrincipalNameKeyResolver();
  683 | 	}
  684 | 
  685 | 	@Bean
  686 | 	@ConditionalOnBean({ RateLimiter.class, KeyResolver.class })
  687 | 	@ConditionalOnEnabledFilter
  688 | 	public RequestRateLimiterGatewayFilterFactory requestRateLimiterGatewayFilterFactory(RateLimiter rateLimiter,
  689 | 			KeyResolver resolver) {
  690 | 		return new RequestRateLimiterGatewayFilterFactory(rateLimiter, resolver);
  691 | 	}
  692 | 
  693 | 	@Bean
  694 | 	@ConditionalOnEnabledFilter
  695 | 	public RewritePathGatewayFilterFactory rewritePathGatewayFilterFactory() {
  696 | 		return new RewritePathGatewayFilterFactory();
  697 | 	}
  698 | 
  699 | 	@Bean
  700 | 	@ConditionalOnEnabledFilter
  701 | 	public RetryGatewayFilterFactory retryGatewayFilterFactory() {
  702 | 		return new RetryGatewayFilterFactory();
  703 | 	}
  704 | 
  705 | 	@Bean
  706 | 	@ConditionalOnEnabledFilter
  707 | 	public SetPathGatewayFilterFactory setPathGatewayFilterFactory() {
  708 | 		return new SetPathGatewayFilterFactory();
  709 | 	}
  710 | 
  711 | 	@Bean
  712 | 	@ConditionalOnEnabledFilter
  713 | 	public SecureHeadersGatewayFilterFactory secureHeadersGatewayFilterFactory(SecureHeadersProperties properties) {
  714 | 		return new SecureHeadersGatewayFilterFactory(properties);
  715 | 	}
  716 | 
  717 | 	@Bean
  718 | 	@ConditionalOnEnabledFilter
  719 | 	public SetRequestHeaderGatewayFilterFactory setRequestHeaderGatewayFilterFactory() {
  720 | 		return new SetRequestHeaderGatewayFilterFactory();
  721 | 	}
  722 | 
  723 | 	@Bean
  724 | 	@ConditionalOnEnabledFilter
  725 | 	public SetRequestHostHeaderGatewayFilterFactory setRequestHostHeaderGatewayFilterFactory() {
  726 | 		return new SetRequestHostHeaderGatewayFilterFactory();
  727 | 	}
  728 | 
  729 | 	@Bean
  730 | 	@ConditionalOnEnabledFilter
  731 | 	public SetResponseHeaderGatewayFilterFactory setResponseHeaderGatewayFilterFactory() {
  732 | 		return new SetResponseHeaderGatewayFilterFactory();
  733 | 	}
  734 | 
  735 | 	@Bean
  736 | 	@ConditionalOnEnabledFilter
  737 | 	public RewriteResponseHeaderGatewayFilterFactory rewriteResponseHeaderGatewayFilterFactory() {
  738 | 		return new RewriteResponseHeaderGatewayFilterFactory();
  739 | 	}
  740 | 
  741 | 	@Bean
  742 | 	@ConditionalOnEnabledFilter
  743 | 	public RewriteLocationResponseHeaderGatewayFilterFactory rewriteLocationResponseHeaderGatewayFilterFactory() {
  744 | 		return new RewriteLocationResponseHeaderGatewayFilterFactory();
  745 | 	}
  746 | 
  747 | 	@Bean
  748 | 	@ConditionalOnEnabledFilter
  749 | 	public SetStatusGatewayFilterFactory setStatusGatewayFilterFactory() {
  750 | 		return new SetStatusGatewayFilterFactory();
  751 | 	}
  752 | 
  753 | 	@Bean
  754 | 	@ConditionalOnEnabledFilter
  755 | 	public SaveSessionGatewayFilterFactory saveSessionGatewayFilterFactory() {
  756 | 		return new SaveSessionGatewayFilterFactory();
  757 | 	}
  758 | 
  759 | 	@Bean
  760 | 	@ConditionalOnEnabledFilter
  761 | 	public StripPrefixGatewayFilterFactory stripPrefixGatewayFilterFactory() {
  762 | 		return new StripPrefixGatewayFilterFactory();
  763 | 	}
  764 | 
  765 | 	@Bean
  766 | 	@ConditionalOnEnabledFilter
  767 | 	public RequestHeaderToRequestUriGatewayFilterFactory requestHeaderToRequestUriGatewayFilterFactory() {
  768 | 		return new RequestHeaderToRequestUriGatewayFilterFactory();
  769 | 	}
  770 | 
  771 | 	@Bean
  772 | 	@ConditionalOnEnabledFilter
  773 | 	public SetRequestUriGatewayFilterFactory setRequestUriGatewayFilterFactory() {
  774 | 		return new SetRequestUriGatewayFilterFactory();
  775 | 	}
  776 | 
  777 | 	@Bean
  778 | 	@ConditionalOnEnabledFilter
  779 | 	public RequestSizeGatewayFilterFactory requestSizeGatewayFilterFactory() {
  780 | 		return new RequestSizeGatewayFilterFactory();
  781 | 	}
  782 | 
  783 | 	@Bean
  784 | 	@ConditionalOnEnabledFilter
  785 | 	public RequestHeaderSizeGatewayFilterFactory requestHeaderSizeGatewayFilterFactory() {
  786 | 		return new RequestHeaderSizeGatewayFilterFactory();
  787 | 	}
  788 | 
  789 | 	@Bean
  790 | 	@ConditionalOnEnabledFilter
  791 | 	public RewriteRequestParameterGatewayFilterFactory rewriteRequestParameterGatewayFilterFactory() {
  792 | 		return new RewriteRequestParameterGatewayFilterFactory();
  793 | 	}
  794 | 
  795 | 	@Bean
  796 | 	public GzipMessageBodyResolver gzipMessageBodyResolver() {
  797 | 		return new GzipMessageBodyResolver();
  798 | 	}
  799 | 
  800 | 	@Bean
  801 | 	static ConfigurableHintsRegistrationProcessor configurableHintsRegistrationProcessor() {
  802 | 		return new ConfigurableHintsRegistrationProcessor();
  803 | 	}
  804 | 
  805 | 	@Configuration(proxyBeanMethods = false)
  806 | 	static class GatewayWebFluxApiVersionConfig implements WebFluxConfigurer {
  807 | 
  808 | 		final Apiversion versionProperties;
  809 | 
  810 | 		GatewayWebFluxApiVersionConfig(ObjectProvider<WebFluxProperties> webFluxProperties) {
  811 | 			this.versionProperties = webFluxProperties.getIfAvailable(WebFluxProperties::new).getApiversion();
  812 | 		}
  813 | 
  814 | 		@Override
  815 | 		public void configureApiVersioning(ApiVersionConfigurer configurer) {
  816 | 			Boolean required = versionProperties.getRequired();
  817 | 			Boolean detectSupported = versionProperties.getDetectSupported();
  818 | 
  819 | 			Apiversion.Use use = versionProperties.getUse();
  820 | 			// only set defaults is one or more use options is set
  821 | 			if (StringUtils.hasText(use.getHeader()) || StringUtils.hasText(use.getQueryParameter())
  822 | 					|| use.getPathSegment() != null || !CollectionUtils.isEmpty(use.getMediaTypeParameter())) {
  823 | 				if (required == null) {
  824 | 					configurer.setVersionRequired(false);
  825 | 				}
  826 | 				if (detectSupported == null) {
  827 | 					configurer.detectSupportedVersions(true);
  828 | 				}
  829 | 				configurer.setSupportedVersionPredicate(comparable -> true);
  830 | 			}
  831 | 		}
  832 | 
  833 | 	}
  834 | 
  835 | 	@Configuration(proxyBeanMethods = false)
  836 | 	@ConditionalOnClass(AsyncProxyManager.class)
  837 | 	protected static class Bucket4jConfiguration {
  838 | 
  839 | 		@Bean
  840 | 		@ConditionalOnBean(AsyncProxyManager.class)
  841 | 		@ConditionalOnEnabledFilter(RequestRateLimiterGatewayFilterFactory.class)
  842 | 		public Bucket4jRateLimiter bucket4jRateLimiter(AsyncProxyManager<String> proxyManager,
  843 | 				ConfigurationService configurationService) {
  844 | 			return new Bucket4jRateLimiter(proxyManager, configurationService);
  845 | 		}
  846 | 
  847 | 	}
  848 | 
  849 | 	@Configuration(proxyBeanMethods = false)
  850 | 	@ConditionalOnClass({ HttpClient.class, NettyServerProperties.class })
  851 | 	protected static class NettyConfiguration {
  852 | 
  853 | 		protected final Log logger = LogFactory.getLog(getClass());
  854 | 
  855 | 		@Bean
  856 | 		@ConditionalOnProperty(name = HttpServerProperties.PREFIX + ".wiretap")
  857 | 		public NettyReactiveWebServerFactoryCustomizer nettyServerWiretapCustomizer(Environment environment,
  858 | 				ServerProperties serverProperties, NettyServerProperties nettyServerProperties) {
  859 | 			return new NettyReactiveWebServerFactoryCustomizer(environment, serverProperties, nettyServerProperties) {
  860 | 				@Override
  861 | 				public void customize(NettyReactiveWebServerFactory factory) {
  862 | 					factory.addServerCustomizers(httpServer -> httpServer.wiretap(true));
  863 | 					super.customize(factory);
  864 | 				}
  865 | 			};
  866 | 		}
  867 | 
  868 | 		@Bean
  869 | 		@Conditional(TrustedProxies.NettyServerCustomizerEnabledCondition.class)
  870 | 		public NettyServerCustomizer gatewayNettyServerCustomizer(GatewayProperties gatewayProperties) {
  871 | 			TrustedProxies trustedProxies = TrustedProxies.from(gatewayProperties.getTrustedProxies());
  872 | 
  873 | 			return httpServer -> httpServer.forwarded((connectionInfo, httpRequest) -> {
  874 | 				InetSocketAddress remoteAddress = connectionInfo.getRemoteAddress();
  875 | 				if (remoteAddress != null && trustedProxies.isTrusted(remoteAddress.getHostString())) {
  876 | 					// update remote address
  877 | 					return DefaultNettyHttpForwardedHeaderHandler.INSTANCE.apply(connectionInfo, httpRequest);
  878 | 				}
  879 | 				return connectionInfo;
  880 | 			});
  881 | 		}
  882 | 
  883 | 		@Bean
  884 | 		public HttpClientSslConfigurer httpClientSslConfigurer(ServerProperties serverProperties,
  885 | 				HttpClientProperties httpClientProperties, SslBundles bundles) {
  886 | 			return new HttpClientSslConfigurer(httpClientProperties.getSsl(), serverProperties, bundles) {
  887 | 			};
  888 | 		}
  889 | 
  890 | 		@Bean
  891 | 		@ConditionalOnMissingBean({ HttpClient.class, HttpClientFactory.class })
  892 | 		public HttpClientFactory gatewayHttpClientFactory(HttpClientProperties properties,
  893 | 				ServerProperties serverProperties, List<HttpClientCustomizer> customizers,
  894 | 				HttpClientSslConfigurer sslConfigurer) {
  895 | 			return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers);
  896 | 		}
  897 | 
  898 | 		@Bean
  899 | 		public HttpClientProperties httpClientProperties() {
  900 | 			return new HttpClientProperties();
  901 | 		}
  902 | 
  903 | 		@Bean
  904 | 		public HttpServerProperties httpServerProperties() {
  905 | 			return new HttpServerProperties();
  906 | 		}
  907 | 
  908 | 		@Bean
  909 | 		@ConditionalOnEnabledGlobalFilter
  910 | 		public NettyRoutingFilter routingFilter(HttpClient httpClient,
  911 | 				ObjectProvider<List<HttpHeadersFilter>> headersFilters, HttpClientProperties properties) {
  912 | 			return new NettyRoutingFilter(httpClient, headersFilters, properties);
  913 | 		}
  914 | 
  915 | 		@Bean
  916 | 		@ConditionalOnEnabledGlobalFilter(NettyRoutingFilter.class)
  917 | 		public NettyWriteResponseFilter nettyWriteResponseFilter(GatewayProperties properties,
  918 | 				ObjectProvider<List<HttpHeadersFilter>> headersFilters) {
  919 | 			return new NettyWriteResponseFilter(properties.getStreamingMediaTypes(), headersFilters);
  920 | 		}
  921 | 
  922 | 		@Bean
  923 | 		@ConditionalOnEnabledGlobalFilter(WebsocketRoutingFilter.class)
  924 | 		public ReactorNettyWebSocketClient reactorNettyWebSocketClient(HttpClientProperties properties,
  925 | 				HttpClient httpClient) {
  926 | 			Supplier<WebsocketClientSpec.Builder> builderSupplier = () -> {
  927 | 				WebsocketClientSpec.Builder builder = WebsocketClientSpec.builder()
  928 | 					.handlePing(properties.getWebsocket().isProxyPing());
  929 | 				if (properties.getWebsocket().getMaxFramePayloadLength() != null) {
  930 | 					builder.maxFramePayloadLength(properties.getWebsocket().getMaxFramePayloadLength());
  931 | 				}
  932 | 				return builder;
  933 | 			};
  934 | 			return new ReactorNettyWebSocketClient(httpClient, builderSupplier);
  935 | 		}
  936 | 
  937 | 		@Bean
  938 | 		@ConditionalOnEnabledGlobalFilter(WebsocketRoutingFilter.class)
  939 | 		public ReactorNettyRequestUpgradeStrategy reactorNettyRequestUpgradeStrategy(
  940 | 				HttpClientProperties httpClientProperties) {
  941 | 
  942 | 			Supplier<WebsocketServerSpec.Builder> builderSupplier = () -> {
  943 | 				WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder();
  944 | 				HttpClientProperties.Websocket websocket = httpClientProperties.getWebsocket();
  945 | 				PropertyMapper map = PropertyMapper.get();
  946 | 				map.from(websocket::getMaxFramePayloadLength).when(Objects::nonNull).to(builder::maxFramePayloadLength);
  947 | 				map.from(websocket::isProxyPing).to(builder::handlePing);
  948 | 				return builder;
  949 | 			};
  950 | 
  951 | 			return new ReactorNettyRequestUpgradeStrategy(builderSupplier);
  952 | 		}
  953 | 
  954 | 	}
  955 | 
  956 | 	@Configuration(proxyBeanMethods = false)
  957 | 	@ConditionalOnClass(WebEndpointProperties.class)
  958 | 	protected static class GatewayActuatorConfiguration {
  959 | 
  960 | 		@Bean
  961 | 		@ConditionalOnProperty(name = "spring.cloud.gateway.server.webflux.actuator.verbose.enabled",
  962 | 				matchIfMissing = true)
  963 | 		@ConditionalOnAvailableEndpoint
  964 | 		public GatewayControllerEndpoint gatewayControllerEndpoint(List<GlobalFilter> globalFilters,
  965 | 				List<GatewayFilterFactory> gatewayFilters, List<RoutePredicateFactory> routePredicates,
  966 | 				RouteDefinitionWriter routeDefinitionWriter, RouteLocator routeLocator,
  967 | 				RouteDefinitionLocator routeDefinitionLocator, WebEndpointProperties webEndpointProperties) {
  968 | 			return new GatewayControllerEndpoint(globalFilters, gatewayFilters, routePredicates, routeDefinitionWriter,
  969 | 					routeLocator, routeDefinitionLocator, webEndpointProperties);
  970 | 		}
  971 | 
  972 | 		@Bean
  973 | 		@Conditional(OnVerboseDisabledCondition.class)
  974 | 		@ConditionalOnAvailableEndpoint
  975 | 		public GatewayLegacyControllerEndpoint gatewayLegacyControllerEndpoint(
  976 | 				RouteDefinitionLocator routeDefinitionLocator, List<GlobalFilter> globalFilters,
  977 | 				List<GatewayFilterFactory> gatewayFilters, List<RoutePredicateFactory> routePredicates,
  978 | 				RouteDefinitionWriter routeDefinitionWriter, RouteLocator routeLocator,
  979 | 				WebEndpointProperties webEndpointProperties) {
  980 | 			return new GatewayLegacyControllerEndpoint(routeDefinitionLocator, globalFilters, gatewayFilters,
  981 | 					routePredicates, routeDefinitionWriter, routeLocator, webEndpointProperties);
  982 | 		}
  983 | 
  984 | 	}
  985 | 
  986 | 	private static class OnVerboseDisabledCondition extends NoneNestedConditions {
  987 | 
  988 | 		OnVerboseDisabledCondition() {
  989 | 			super(ConfigurationPhase.REGISTER_BEAN);
  990 | 		}
  991 | 
  992 | 		@ConditionalOnProperty(name = "spring.cloud.gateway.server.webflux.actuator.verbose.enabled",
  993 | 				matchIfMissing = true)
  994 | 		static class VerboseDisabled {
  995 | 
  996 | 		}
  997 | 
  998 | 	}
  999 | 
 1000 | 	@Configuration(proxyBeanMethods = false)
 1001 | 	@ConditionalOnProperty(name = "spring.cloud.gateway.server.webflux.enabled", matchIfMissing = true)
 1002 | 	@ConditionalOnClass({ OAuth2AuthorizedClient.class, SecurityWebFilterChain.class, SecurityProperties.class })
 1003 | 	@ConditionalOnEnabledFilter(TokenRelayGatewayFilterFactory.class)
 1004 | 	protected static class TokenRelayConfiguration {
 1005 | 
 1006 | 		@Bean
 1007 | 		public TokenRelayGatewayFilterFactory tokenRelayGatewayFilterFactory(
 1008 | 				ObjectProvider<ReactiveOAuth2AuthorizedClientManager> clientManager) {
 1009 | 			return new TokenRelayGatewayFilterFactory(clientManager);
 1010 | 		}
 1011 | 
 1012 | 	}
 1013 | 
 1014 | }
 1015 | 
 1016 | class GatewayHints implements RuntimeHintsRegistrar {
 1017 | 
 1018 | 	@Override
 1019 | 	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
 1020 | 		if (!ClassUtils.isPresent("org.springframework.cloud.gateway.route.RouteLocator", classLoader)) {
 1021 | 			return;
 1022 | 		}
 1023 | 		hints.reflection()
 1024 | 			.registerType(TypeReference.of(FilterDefinition.class),
 1025 | 					hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
 1026 | 							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
 1027 | 			.registerType(TypeReference.of(PredicateDefinition.class),
 1028 | 					hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
 1029 | 							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
 1030 | 			.registerType(TypeReference.of(AbstractNameValueGatewayFilterFactory.NameValueConfig.class),
 1031 | 					hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
 1032 | 							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
 1033 | 			.registerType(TypeReference
 1034 | 				.of("org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator$DelegatingServiceInstance"),
 1035 | 					hint -> hint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
 1036 | 							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
 1037 | 	}
 1038 | 
 1039 | }
```
