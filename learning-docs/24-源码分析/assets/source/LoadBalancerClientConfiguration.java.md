<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/annotation/LoadBalancerClientConfiguration.java -->
<!-- 正文引用：[[源码:LoadBalancerClientConfiguration.java#L起-L止]] ；代码块头部：(src: LoadBalancerClientConfiguration.java#L起-L止) -->

# LoadBalancerClientConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/annotation/LoadBalancerClientConfiguration.java` |
| 行数 | 473 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.725Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.loadbalancer.annotation;
   18 | 
   19 | import reactor.util.retry.RetrySpec;
   20 | 
   21 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   22 | import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   25 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   26 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   27 | import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
   28 | import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
   29 | import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
   30 | import org.springframework.cloud.client.ServiceInstance;
   31 | import org.springframework.cloud.client.discovery.DiscoveryClient;
   32 | import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
   33 | import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
   34 | import org.springframework.cloud.loadbalancer.core.RetryAwareServiceInstanceListSupplier;
   35 | import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
   36 | import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
   37 | import org.springframework.cloud.loadbalancer.core.XForwardedHeadersTransformer;
   38 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   39 | import org.springframework.cloud.loadbalancer.support.LoadBalancerEnvironmentPropertyUtils;
   40 | import org.springframework.context.ConfigurableApplicationContext;
   41 | import org.springframework.context.annotation.Bean;
   42 | import org.springframework.context.annotation.Condition;
   43 | import org.springframework.context.annotation.ConditionContext;
   44 | import org.springframework.context.annotation.Conditional;
   45 | import org.springframework.context.annotation.Configuration;
   46 | import org.springframework.context.annotation.Primary;
   47 | import org.springframework.core.annotation.Order;
   48 | import org.springframework.core.env.Environment;
   49 | import org.springframework.core.type.AnnotatedTypeMetadata;
   50 | import org.springframework.retry.support.RetryTemplate;
   51 | import org.springframework.web.accept.ApiVersionParser;
   52 | import org.springframework.web.accept.SemanticApiVersionParser;
   53 | import org.springframework.web.client.RestClient;
   54 | import org.springframework.web.client.RestTemplate;
   55 | import org.springframework.web.reactive.function.client.WebClient;
   56 | 
   57 | /**
   58 |  * @author Spencer Gibb
   59 |  * @author Olga Maciaszek-Sharma
   60 |  * @author Tim Ysewyn
   61 |  * @author BaoLin Zhu
   62 |  * @author changjin wei(魏昌进)
   63 |  * @author Zhuozhi Ji
   64 |  */
   65 | @Configuration(proxyBeanMethods = false)
   66 | @ConditionalOnDiscoveryEnabled
   67 | public class LoadBalancerClientConfiguration {
   68 | 
   69 | 	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;
   70 | 
   71 | 	@Bean
   72 | 	@ConditionalOnMissingBean
   73 | 	public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment environment,
   74 | 			LoadBalancerClientFactory loadBalancerClientFactory) {
   75 | 		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
   76 | 		return new RoundRobinLoadBalancer(
   77 | 				loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
   78 | 	}
   79 | 
   80 | 	@Bean
   81 | 	@ConditionalOnMissingBean
   82 | 	public ApiVersionParser<?> loadBalancerApiVersionParser() {
   83 | 		return new SemanticApiVersionParser();
   84 | 	}
   85 | 
   86 | 	@Configuration(proxyBeanMethods = false)
   87 | 	@ConditionalOnReactiveDiscoveryEnabled
   88 | 	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
   89 | 	public static class ReactiveSupportConfiguration {
   90 | 
   91 | 		@Bean
   92 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
   93 | 		@ConditionalOnMissingBean
   94 | 		@Conditional(DefaultConfigurationCondition.class)
   95 | 		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
   96 | 				ConfigurableApplicationContext context) {
   97 | 			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withCaching().build(context);
   98 | 		}
   99 | 
  100 | 		@Bean
  101 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  102 | 		@ConditionalOnMissingBean
  103 | 		@Conditional(ZonePreferenceConfigurationCondition.class)
  104 | 		public ServiceInstanceListSupplier zonePreferenceDiscoveryClientServiceInstanceListSupplier(
  105 | 				ConfigurableApplicationContext context) {
  106 | 			return ServiceInstanceListSupplier.builder()
  107 | 				.withDiscoveryClient()
  108 | 				.withCaching()
  109 | 				.withZonePreference()
  110 | 				.build(context);
  111 | 		}
  112 | 
  113 | 		@Bean
  114 | 		@ConditionalOnBean(LoadBalancerClientFactory.class)
  115 | 		@ConditionalOnMissingBean
  116 | 		public XForwardedHeadersTransformer xForwarderHeadersTransformer(LoadBalancerClientFactory clientFactory) {
  117 | 			return new XForwardedHeadersTransformer(clientFactory);
  118 | 		}
  119 | 
  120 | 		@Bean
  121 | 		@ConditionalOnBean({ ReactiveDiscoveryClient.class, WebClient.Builder.class })
  122 | 		@ConditionalOnMissingBean
  123 | 		@Conditional(HealthCheckConfigurationCondition.class)
  124 | 		public ServiceInstanceListSupplier healthCheckDiscoveryClientServiceInstanceListSupplier(
  125 | 				ConfigurableApplicationContext context) {
  126 | 			return ServiceInstanceListSupplier.builder().withDiscoveryClient().withHealthChecks().build(context);
  127 | 		}
  128 | 
  129 | 		@Bean
  130 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  131 | 		@ConditionalOnMissingBean
  132 | 		@Conditional(RequestBasedStickySessionConfigurationCondition.class)
  133 | 		public ServiceInstanceListSupplier requestBasedStickySessionDiscoveryClientServiceInstanceListSupplier(
  134 | 				ConfigurableApplicationContext context) {
  135 | 			return ServiceInstanceListSupplier.builder()
  136 | 				.withDiscoveryClient()
  137 | 				.withCaching()
  138 | 				.withRequestBasedStickySession()
  139 | 				.build(context);
  140 | 		}
  141 | 
  142 | 		@Bean
  143 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  144 | 		@ConditionalOnMissingBean
  145 | 		@Conditional(SameInstancePreferenceConfigurationCondition.class)
  146 | 		public ServiceInstanceListSupplier sameInstancePreferenceServiceInstanceListSupplier(
  147 | 				ConfigurableApplicationContext context) {
  148 | 			return ServiceInstanceListSupplier.builder()
  149 | 				.withDiscoveryClient()
  150 | 				.withCaching()
  151 | 				.withSameInstancePreference()
  152 | 				.build(context);
  153 | 		}
  154 | 
  155 | 		@Bean
  156 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  157 | 		@ConditionalOnMissingBean
  158 | 		@Conditional(WeightedConfigurationCondition.class)
  159 | 		public ServiceInstanceListSupplier weightedServiceInstanceListSupplier(ConfigurableApplicationContext context) {
  160 | 			return ServiceInstanceListSupplier.builder()
  161 | 				.withDiscoveryClient()
  162 | 				.withCaching()
  163 | 				.withWeighted()
  164 | 				.build(context);
  165 | 		}
  166 | 
  167 | 		@Bean
  168 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  169 | 		@ConditionalOnMissingBean
  170 | 		@Conditional(SubsetConfigurationCondition.class)
  171 | 		public ServiceInstanceListSupplier subsetServiceInstanceListSupplier(ConfigurableApplicationContext context) {
  172 | 			return ServiceInstanceListSupplier.builder()
  173 | 				.withDiscoveryClient()
  174 | 				.withCaching()
  175 | 				.withSubset()
  176 | 				.build(context);
  177 | 		}
  178 | 
  179 | 		@Bean
  180 | 		@ConditionalOnBean(ReactiveDiscoveryClient.class)
  181 | 		@ConditionalOnMissingBean
  182 | 		@Conditional(ApiVersionCondition.class)
  183 | 		public ServiceInstanceListSupplier reactiveApiVersionServiceInstanceListSupplier(
  184 | 				ConfigurableApplicationContext context) {
  185 | 			return ServiceInstanceListSupplier.builder()
  186 | 				.withDiscoveryClient()
  187 | 				.withCaching()
  188 | 				.withReactiveApiVersioning()
  189 | 				.build(context);
  190 | 		}
  191 | 
  192 | 	}
  193 | 
  194 | 	@Configuration(proxyBeanMethods = false)
  195 | 	@ConditionalOnBlockingDiscoveryEnabled
  196 | 	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
  197 | 	public static class BlockingSupportConfiguration {
  198 | 
  199 | 		@Bean
  200 | 		@ConditionalOnBean(DiscoveryClient.class)
  201 | 		@ConditionalOnMissingBean
  202 | 		@Conditional(DefaultConfigurationCondition.class)
  203 | 		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
  204 | 				ConfigurableApplicationContext context) {
  205 | 			return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching().build(context);
  206 | 		}
  207 | 
  208 | 		@Bean
  209 | 		@ConditionalOnBean(DiscoveryClient.class)
  210 | 		@ConditionalOnMissingBean
  211 | 		@Conditional(ZonePreferenceConfigurationCondition.class)
  212 | 		public ServiceInstanceListSupplier zonePreferenceDiscoveryClientServiceInstanceListSupplier(
  213 | 				ConfigurableApplicationContext context) {
  214 | 			return ServiceInstanceListSupplier.builder()
  215 | 				.withBlockingDiscoveryClient()
  216 | 				.withCaching()
  217 | 				.withZonePreference()
  218 | 				.build(context);
  219 | 		}
  220 | 
  221 | 		@Bean
  222 | 		@ConditionalOnBean({ DiscoveryClient.class, RestTemplate.class })
  223 | 		@ConditionalOnMissingBean
  224 | 		@Conditional(HealthCheckConfigurationCondition.class)
  225 | 		public ServiceInstanceListSupplier healthCheckDiscoveryClientServiceInstanceListSupplier(
  226 | 				ConfigurableApplicationContext context) {
  227 | 			return ServiceInstanceListSupplier.builder()
  228 | 				.withBlockingDiscoveryClient()
  229 | 				.withBlockingHealthChecks()
  230 | 				.build(context);
  231 | 		}
  232 | 
  233 | 		@Bean
  234 | 		@ConditionalOnBean({ DiscoveryClient.class, RestClient.class })
  235 | 		@ConditionalOnMissingBean
  236 | 		@Conditional(HealthCheckConfigurationCondition.class)
  237 | 		public ServiceInstanceListSupplier healthCheckRestClientDiscoveryClientServiceInstanceListSupplier(
  238 | 				ConfigurableApplicationContext context) {
  239 | 			return ServiceInstanceListSupplier.builder()
  240 | 				.withBlockingDiscoveryClient()
  241 | 				.withBlockingRestClientHealthChecks()
  242 | 				.build(context);
  243 | 		}
  244 | 
  245 | 		@Bean
  246 | 		@ConditionalOnBean(DiscoveryClient.class)
  247 | 		@ConditionalOnMissingBean
  248 | 		@Conditional(RequestBasedStickySessionConfigurationCondition.class)
  249 | 		public ServiceInstanceListSupplier requestBasedStickySessionDiscoveryClientServiceInstanceListSupplier(
  250 | 				ConfigurableApplicationContext context) {
  251 | 			return ServiceInstanceListSupplier.builder()
  252 | 				.withBlockingDiscoveryClient()
  253 | 				.withCaching()
  254 | 				.withRequestBasedStickySession()
  255 | 				.build(context);
  256 | 		}
  257 | 
  258 | 		@Bean
  259 | 		@ConditionalOnBean(DiscoveryClient.class)
  260 | 		@ConditionalOnMissingBean
  261 | 		@Conditional(SameInstancePreferenceConfigurationCondition.class)
  262 | 		public ServiceInstanceListSupplier sameInstancePreferenceServiceInstanceListSupplier(
  263 | 				ConfigurableApplicationContext context) {
  264 | 			return ServiceInstanceListSupplier.builder()
  265 | 				.withBlockingDiscoveryClient()
  266 | 				.withCaching()
  267 | 				.withSameInstancePreference()
  268 | 				.build(context);
  269 | 		}
  270 | 
  271 | 		@Bean
  272 | 		@ConditionalOnBean(DiscoveryClient.class)
  273 | 		@ConditionalOnMissingBean
  274 | 		@Conditional(WeightedConfigurationCondition.class)
  275 | 		public ServiceInstanceListSupplier weightedServiceInstanceListSupplier(ConfigurableApplicationContext context) {
  276 | 			return ServiceInstanceListSupplier.builder()
  277 | 				.withBlockingDiscoveryClient()
  278 | 				.withCaching()
  279 | 				.withWeighted()
  280 | 				.build(context);
  281 | 		}
  282 | 
  283 | 		@Bean
  284 | 		@ConditionalOnBean(DiscoveryClient.class)
  285 | 		@ConditionalOnMissingBean
  286 | 		@Conditional(SubsetConfigurationCondition.class)
  287 | 		public ServiceInstanceListSupplier subsetServiceInstanceListSupplier(ConfigurableApplicationContext context) {
  288 | 			return ServiceInstanceListSupplier.builder()
  289 | 				.withBlockingDiscoveryClient()
  290 | 				.withCaching()
  291 | 				.withSubset()
  292 | 				.build(context);
  293 | 		}
  294 | 
  295 | 		@Bean
  296 | 		@ConditionalOnBean(DiscoveryClient.class)
  297 | 		@ConditionalOnMissingBean
  298 | 		@Conditional(ApiVersionCondition.class)
  299 | 		public ServiceInstanceListSupplier blockingApiVersionServiceInstanceListSupplier(
  300 | 				ConfigurableApplicationContext context) {
  301 | 			return ServiceInstanceListSupplier.builder()
  302 | 				.withBlockingDiscoveryClient()
  303 | 				.withCaching()
  304 | 				.withBlockingApiVersioning()
  305 | 				.build(context);
  306 | 		}
  307 | 
  308 | 	}
  309 | 
  310 | 	@Configuration(proxyBeanMethods = false)
  311 | 	@ConditionalOnBlockingDiscoveryEnabled
  312 | 	@ConditionalOnClass(RetryTemplate.class)
  313 | 	@Conditional(BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition.class)
  314 | 	@AutoConfigureAfter(BlockingSupportConfiguration.class)
  315 | 	@ConditionalOnBean(ServiceInstanceListSupplier.class)
  316 | 	public static class BlockingRetryConfiguration {
  317 | 
  318 | 		@Bean
  319 | 		@ConditionalOnBean(DiscoveryClient.class)
  320 | 		@Primary
  321 | 		public ServiceInstanceListSupplier retryAwareDiscoveryClientServiceInstanceListSupplier(
  322 | 				ServiceInstanceListSupplier delegate) {
  323 | 			return new RetryAwareServiceInstanceListSupplier(delegate);
  324 | 		}
  325 | 
  326 | 	}
  327 | 
  328 | 	@Configuration(proxyBeanMethods = false)
  329 | 	@ConditionalOnReactiveDiscoveryEnabled
  330 | 	@Conditional(ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition.class)
  331 | 	@AutoConfigureAfter(ReactiveSupportConfiguration.class)
  332 | 	@ConditionalOnBean(ServiceInstanceListSupplier.class)
  333 | 	@ConditionalOnClass(RetrySpec.class)
  334 | 	public static class ReactiveRetryConfiguration {
  335 | 
  336 | 		@Bean
  337 | 		@ConditionalOnBean(DiscoveryClient.class)
  338 | 		@Primary
  339 | 		public ServiceInstanceListSupplier retryAwareDiscoveryClientServiceInstanceListSupplier(
  340 | 				ServiceInstanceListSupplier delegate) {
  341 | 			return new RetryAwareServiceInstanceListSupplier(delegate);
  342 | 		}
  343 | 
  344 | 	}
  345 | 
  346 | 	static final class BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition extends AllNestedConditions {
  347 | 
  348 | 		private BlockingOnAvoidPreviousInstanceAndRetryEnabledCondition() {
  349 | 			super(ConfigurationPhase.REGISTER_BEAN);
  350 | 		}
  351 | 
  352 | 		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
  353 | 				matchIfMissing = true)
  354 | 		static class LoadBalancerRetryEnabled {
  355 | 
  356 | 		}
  357 | 
  358 | 		@Conditional(AvoidPreviousInstanceEnabledCondition.class)
  359 | 		static class AvoidPreviousInstanceEnabled {
  360 | 
  361 | 		}
  362 | 
  363 | 	}
  364 | 
  365 | 	static final class ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition extends AllNestedConditions {
  366 | 
  367 | 		private ReactiveOnAvoidPreviousInstanceAndRetryEnabledCondition() {
  368 | 			super(ConfigurationPhase.REGISTER_BEAN);
  369 | 		}
  370 | 
  371 | 		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
  372 | 		static class LoadBalancerRetryEnabled {
  373 | 
  374 | 		}
  375 | 
  376 | 		@Conditional(AvoidPreviousInstanceEnabledCondition.class)
  377 | 		static class AvoidPreviousInstanceEnabled {
  378 | 
  379 | 		}
  380 | 
  381 | 	}
  382 | 
  383 | 	static class AvoidPreviousInstanceEnabledCondition implements Condition {
  384 | 
  385 | 		@Override
  386 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  387 | 			return LoadBalancerEnvironmentPropertyUtils.trueOrMissingForClientOrDefault(context.getEnvironment(),
  388 | 					"retry.avoid-previous-instance");
  389 | 		}
  390 | 
  391 | 	}
  392 | 
  393 | 	static class DefaultConfigurationCondition implements Condition {
  394 | 
  395 | 		@Override
  396 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  397 | 			return LoadBalancerEnvironmentPropertyUtils.equalToOrMissingForClientOrDefault(context.getEnvironment(),
  398 | 					"configurations", "default");
  399 | 		}
  400 | 
  401 | 	}
  402 | 
  403 | 	static class ZonePreferenceConfigurationCondition implements Condition {
  404 | 
  405 | 		@Override
  406 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  407 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  408 | 					"configurations", "zone-preference");
  409 | 		}
  410 | 
  411 | 	}
  412 | 
  413 | 	static class HealthCheckConfigurationCondition implements Condition {
  414 | 
  415 | 		@Override
  416 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  417 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  418 | 					"configurations", "health-check");
  419 | 		}
  420 | 
  421 | 	}
  422 | 
  423 | 	static class RequestBasedStickySessionConfigurationCondition implements Condition {
  424 | 
  425 | 		@Override
  426 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  427 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  428 | 					"configurations", "request-based-sticky-session");
  429 | 		}
  430 | 
  431 | 	}
  432 | 
  433 | 	static class SameInstancePreferenceConfigurationCondition implements Condition {
  434 | 
  435 | 		@Override
  436 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  437 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  438 | 					"configurations", "same-instance-preference");
  439 | 		}
  440 | 
  441 | 	}
  442 | 
  443 | 	static class WeightedConfigurationCondition implements Condition {
  444 | 
  445 | 		@Override
  446 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  447 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  448 | 					"configurations", "weighted");
  449 | 		}
  450 | 
  451 | 	}
  452 | 
  453 | 	static class SubsetConfigurationCondition implements Condition {
  454 | 
  455 | 		@Override
  456 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  457 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  458 | 					"configurations", "subset");
  459 | 		}
  460 | 
  461 | 	}
  462 | 
  463 | 	static class ApiVersionCondition implements Condition {
  464 | 
  465 | 		@Override
  466 | 		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
  467 | 			return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(context.getEnvironment(),
  468 | 					"configurations", "api-version");
  469 | 		}
  470 | 
  471 | 	}
  472 | 
  473 | }
```
