<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignAutoConfiguration.java -->
<!-- 正文引用：[[源码:FeignAutoConfiguration.java#L起-L止]] ；代码块头部：(src: FeignAutoConfiguration.java#L起-L止) -->

# FeignAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignAutoConfiguration.java` |
| 行数 | 314 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.967Z |

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
   19 | import java.lang.reflect.Method;
   20 | import java.net.http.HttpClient;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.Set;
   24 | 
   25 | import feign.Capability;
   26 | import feign.Client;
   27 | import feign.Feign;
   28 | import feign.ResponseInterceptor;
   29 | import feign.Target;
   30 | import feign.hc5.ApacheHttp5Client;
   31 | import feign.http2client.Http2Client;
   32 | import org.apache.commons.logging.Log;
   33 | import org.apache.commons.logging.LogFactory;
   34 | import tools.jackson.databind.JacksonModule;
   35 | 
   36 | import org.springframework.aot.hint.MemberCategory;
   37 | import org.springframework.aot.hint.RuntimeHints;
   38 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   39 | import org.springframework.aot.hint.TypeReference;
   40 | import org.springframework.beans.factory.annotation.Autowired;
   41 | import org.springframework.beans.factory.annotation.Value;
   42 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   43 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   44 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   45 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   46 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   47 | import org.springframework.cache.interceptor.CacheInterceptor;
   48 | import org.springframework.cloud.client.actuator.HasFeatures;
   49 | import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
   50 | import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
   51 | import org.springframework.cloud.openfeign.aot.FeignChildContextInitializer;
   52 | import org.springframework.cloud.openfeign.aot.FeignClientBeanFactoryInitializationAotProcessor;
   53 | import org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor;
   54 | import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
   55 | import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
   56 | import org.springframework.cloud.openfeign.support.PageJacksonModule;
   57 | import org.springframework.cloud.openfeign.support.SortJacksonModule;
   58 | import org.springframework.context.annotation.Bean;
   59 | import org.springframework.context.annotation.Conditional;
   60 | import org.springframework.context.annotation.Configuration;
   61 | import org.springframework.context.annotation.Import;
   62 | import org.springframework.context.support.GenericApplicationContext;
   63 | import org.springframework.data.domain.Page;
   64 | import org.springframework.data.domain.Sort;
   65 | import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
   66 | import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
   67 | import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
   68 | import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
   69 | import org.springframework.util.ClassUtils;
   70 | 
   71 | /**
   72 |  * @author Spencer Gibb
   73 |  * @author Julien Roy
   74 |  * @author Grzegorz Poznachowski
   75 |  * @author Nikita Konev
   76 |  * @author Tim Peeters
   77 |  * @author Olga Maciaszek-Sharma
   78 |  * @author Nguyen Ky Thanh
   79 |  * @author Andrii Bohutskyi
   80 |  * @author Kwangyong Kim
   81 |  * @author Sam Kruglov
   82 |  * @author Wojciech Mąka
   83 |  * @author Dangzhicairang(小水牛)
   84 |  * @author changjin wei(魏昌进)
   85 |  * @author jaehun lee
   86 |  */
   87 | @Configuration(proxyBeanMethods = false)
   88 | @ConditionalOnClass(Feign.class)
   89 | @EnableConfigurationProperties({ FeignClientProperties.class, FeignHttpClientProperties.class,
   90 | 		FeignEncoderProperties.class, FeignOAuth2Properties.class, FeignCircuitBreakerProperties.class })
   91 | public class FeignAutoConfiguration {
   92 | 
   93 | 	private static final Log LOG = LogFactory.getLog(FeignAutoConfiguration.class);
   94 | 
   95 | 	@Autowired(required = false)
   96 | 	private List<FeignClientSpecification> configurations = new ArrayList<>();
   97 | 
   98 | 	@Bean
   99 | 	public HasFeatures feignFeature() {
  100 | 		return HasFeatures.namedFeature("Feign", Feign.class);
  101 | 	}
  102 | 
  103 | 	@Bean
  104 | 	public FeignClientFactory feignContext() {
  105 | 		FeignClientFactory context = new FeignClientFactory();
  106 | 		context.setConfigurations(this.configurations);
  107 | 		return context;
  108 | 	}
  109 | 
  110 | 	@Bean
  111 | 	static FeignChildContextInitializer feignChildContextInitializer(GenericApplicationContext parentContext,
  112 | 			FeignClientFactory feignClientFactory) {
  113 | 		return new FeignChildContextInitializer(parentContext, feignClientFactory);
  114 | 	}
  115 | 
  116 | 	@Bean
  117 | 	static FeignClientBeanFactoryInitializationAotProcessor feignClientBeanFactoryInitializationCodeGenerator(
  118 | 			GenericApplicationContext applicationContext, FeignClientFactory feignClientFactory) {
  119 | 		return new FeignClientBeanFactoryInitializationAotProcessor(applicationContext, feignClientFactory);
  120 | 	}
  121 | 
  122 | 	@Bean
  123 | 	@ConditionalOnProperty(value = "spring.cloud.openfeign.cache.enabled", matchIfMissing = true)
  124 | 	@ConditionalOnBean(CacheInterceptor.class)
  125 | 	public Capability cachingCapability(CacheInterceptor cacheInterceptor) {
  126 | 		return new CachingCapability(cacheInterceptor);
  127 | 	}
  128 | 
  129 | 	@Configuration(proxyBeanMethods = false)
  130 | 	@ConditionalOnClass({ JacksonModule.class, Page.class, Sort.class })
  131 | 	@ConditionalOnProperty(value = "spring.cloud.openfeign.autoconfiguration.jackson.enabled", havingValue = "true",
  132 | 			matchIfMissing = true)
  133 | 	protected static class FeignJacksonConfiguration {
  134 | 
  135 | 		@Bean
  136 | 		@ConditionalOnMissingBean(PageJacksonModule.class)
  137 | 		public PageJacksonModule pageJacksonModule() {
  138 | 			return new PageJacksonModule();
  139 | 		}
  140 | 
  141 | 		@Bean
  142 | 		@ConditionalOnMissingBean(SortJacksonModule.class)
  143 | 		public SortJacksonModule sortModule() {
  144 | 			return new SortJacksonModule();
  145 | 		}
  146 | 
  147 | 	}
  148 | 
  149 | 	@Configuration(proxyBeanMethods = false)
  150 | 	@Conditional(FeignCircuitBreakerDisabledConditions.class)
  151 | 	protected static class DefaultFeignTargeterConfiguration {
  152 | 
  153 | 		@Bean
  154 | 		@ConditionalOnMissingBean
  155 | 		public Targeter feignTargeter() {
  156 | 			return new DefaultTargeter();
  157 | 		}
  158 | 
  159 | 	}
  160 | 
  161 | 	@Configuration(proxyBeanMethods = false)
  162 | 	@ConditionalOnClass(CircuitBreaker.class)
  163 | 	@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.enabled", havingValue = "true")
  164 | 	protected static class CircuitBreakerPresentFeignTargeterConfiguration {
  165 | 
  166 | 		@Bean
  167 | 		@ConditionalOnMissingBean(CircuitBreakerFactory.class)
  168 | 		public Targeter defaultFeignTargeter() {
  169 | 			return new DefaultTargeter();
  170 | 		}
  171 | 
  172 | 		@Bean
  173 | 		@ConditionalOnMissingBean(CircuitBreakerNameResolver.class)
  174 | 		@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled",
  175 | 				havingValue = "false")
  176 | 		public CircuitBreakerNameResolver circuitBreakerNameResolver() {
  177 | 			return new DefaultCircuitBreakerNameResolver();
  178 | 		}
  179 | 
  180 | 		@Bean
  181 | 		@ConditionalOnMissingBean(CircuitBreakerNameResolver.class)
  182 | 		@ConditionalOnProperty(value = "spring.cloud.openfeign.circuitbreaker.alphanumeric-ids.enabled",
  183 | 				havingValue = "true", matchIfMissing = true)
  184 | 		public CircuitBreakerNameResolver alphanumericCircuitBreakerNameResolver() {
  185 | 			return new AlphanumericCircuitBreakerNameResolver();
  186 | 		}
  187 | 
  188 | 		/**
  189 | 		 * @deprecated in favor of
  190 | 		 * {@link #circuitBreakerFeignTargeter(CircuitBreakerFactory, FeignCircuitBreakerProperties, CircuitBreakerNameResolver)}.
  191 | 		 */
  192 | 		@Deprecated
  193 | 		@SuppressWarnings("rawtypes")
  194 | 		public Targeter circuitBreakerFeignTargeter(CircuitBreakerFactory circuitBreakerFactory,
  195 | 				@Value("${spring.cloud.openfeign.circuitbreaker.group.enabled:false}") boolean circuitBreakerGroupEnabled,
  196 | 				CircuitBreakerNameResolver circuitBreakerNameResolver) {
  197 | 			return new FeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerGroupEnabled,
  198 | 					circuitBreakerNameResolver);
  199 | 		}
  200 | 
  201 | 		@SuppressWarnings("rawtypes")
  202 | 		@Bean
  203 | 		@ConditionalOnMissingBean
  204 | 		@ConditionalOnBean(CircuitBreakerFactory.class)
  205 | 		public Targeter circuitBreakerFeignTargeter(CircuitBreakerFactory circuitBreakerFactory,
  206 | 				FeignCircuitBreakerProperties circuitBreakerProperties,
  207 | 				CircuitBreakerNameResolver circuitBreakerNameResolver) {
  208 | 			return circuitBreakerFeignTargeter(circuitBreakerFactory, circuitBreakerProperties.getGroup().isEnabled(),
  209 | 					circuitBreakerNameResolver);
  210 | 		}
  211 | 
  212 | 		static class DefaultCircuitBreakerNameResolver implements CircuitBreakerNameResolver {
  213 | 
  214 | 			@Override
  215 | 			public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
  216 | 				return Feign.configKey(target.type(), method);
  217 | 			}
  218 | 
  219 | 		}
  220 | 
  221 | 		static class AlphanumericCircuitBreakerNameResolver extends DefaultCircuitBreakerNameResolver {
  222 | 
  223 | 			@Override
  224 | 			public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
  225 | 				return super.resolveCircuitBreakerName(feignClientName, target, method).replaceAll("[^a-zA-Z0-9]", "");
  226 | 			}
  227 | 
  228 | 		}
  229 | 
  230 | 	}
  231 | 
  232 | 	// the following configuration is for alternate feign clients if
  233 | 	// SC loadbalancer is not on the class path.
  234 | 	// see corresponding configurations in FeignLoadBalancerAutoConfiguration
  235 | 	// for load-balanced clients.
  236 | 	@Configuration(proxyBeanMethods = false)
  237 | 	@ConditionalOnClass(ApacheHttp5Client.class)
  238 | 	@ConditionalOnMissingBean(org.apache.hc.client5.http.impl.classic.CloseableHttpClient.class)
  239 | 	@ConditionalOnProperty(value = "spring.cloud.openfeign.httpclient.hc5.enabled", havingValue = "true",
  240 | 			matchIfMissing = true)
  241 | 	@Import(org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration.class)
  242 | 	protected static class HttpClient5FeignConfiguration {
  243 | 
  244 | 		@Bean
  245 | 		@ConditionalOnMissingBean(Client.class)
  246 | 		public Client feignClient(org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient5) {
  247 | 			return new ApacheHttp5Client(httpClient5);
  248 | 		}
  249 | 
  250 | 	}
  251 | 
  252 | 	@Configuration(proxyBeanMethods = false)
  253 | 	@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
  254 | 	@ConditionalOnProperty("spring.cloud.openfeign.oauth2.enabled")
  255 | 	protected static class Oauth2FeignConfiguration {
  256 | 
  257 | 		@Bean
  258 | 		@ConditionalOnBean({ OAuth2AuthorizedClientService.class, ClientRegistrationRepository.class })
  259 | 		@ConditionalOnMissingBean
  260 | 		OAuth2AuthorizedClientManager feignOAuth2AuthorizedClientManager(
  261 | 				ClientRegistrationRepository clientRegistrationRepository,
  262 | 				OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
  263 | 			return new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
  264 | 					oAuth2AuthorizedClientService);
  265 | 
  266 | 		}
  267 | 
  268 | 		@Bean
  269 | 		@ConditionalOnBean(OAuth2AuthorizedClientManager.class)
  270 | 		public OAuth2AccessTokenInterceptor defaultOAuth2AccessTokenInterceptor(FeignOAuth2Properties oAuth2Properties,
  271 | 				OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
  272 | 			return new OAuth2AccessTokenInterceptor(oAuth2Properties.getClientRegistrationId(),
  273 | 					oAuth2AuthorizedClientManager);
  274 | 		}
  275 | 
  276 | 	}
  277 | 
  278 | 	// the following configuration is for alternate feign clients if
  279 | 	// SC loadbalancer is not on the class path.
  280 | 	// see corresponding configurations in FeignLoadBalancerAutoConfiguration
  281 | 	// for load-balanced clients.
  282 | 	@Configuration(proxyBeanMethods = false)
  283 | 	@ConditionalOnClass({ Http2Client.class, HttpClient.class })
  284 | 	@ConditionalOnMissingBean(HttpClient.class)
  285 | 	@ConditionalOnProperty("spring.cloud.openfeign.http2client.enabled")
  286 | 	@Import(org.springframework.cloud.openfeign.clientconfig.Http2ClientFeignConfiguration.class)
  287 | 	protected static class Http2ClientFeignConfiguration {
  288 | 
  289 | 		@Bean
  290 | 		@ConditionalOnMissingBean(Client.class)
  291 | 		public Client feignClient(HttpClient httpClient) {
  292 | 			return new Http2Client(httpClient);
  293 | 		}
  294 | 
  295 | 	}
  296 | 
  297 | }
  298 | 
  299 | class FeignHints implements RuntimeHintsRegistrar {
  300 | 
  301 | 	@Override
  302 | 	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
  303 | 		if (!ClassUtils.isPresent("feign.Feign", classLoader)) {
  304 | 			return;
  305 | 		}
  306 | 		hints.reflection()
  307 | 			.registerTypes(
  308 | 					Set.of(TypeReference.of(FeignClientFactoryBean.class),
  309 | 							TypeReference.of(ResponseInterceptor.Chain.class), TypeReference.of(Capability.class)),
  310 | 					hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
  311 | 							MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.ACCESS_DECLARED_FIELDS));
  312 | 	}
  313 | 
  314 | }
```
