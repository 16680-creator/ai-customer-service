<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/reactive/ReactorLoadBalancerClientAutoConfiguration.java -->
<!-- 正文引用：[[源码:ReactorLoadBalancerClientAutoConfiguration.java#L起-L止]] ；代码块头部：(src: ReactorLoadBalancerClientAutoConfiguration.java#L起-L止) -->

# ReactorLoadBalancerClientAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/reactive/ReactorLoadBalancerClientAutoConfiguration.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.621Z |

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
   17 | package org.springframework.cloud.client.loadbalancer.reactive;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.List;
   21 | 
   22 | import org.springframework.beans.factory.ObjectProvider;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   25 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   26 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   27 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   28 | import org.springframework.cloud.client.ServiceInstance;
   29 | import org.springframework.cloud.client.loadbalancer.LoadBalanced;
   30 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
   31 | import org.springframework.context.annotation.Bean;
   32 | import org.springframework.context.annotation.Configuration;
   33 | import org.springframework.web.reactive.function.client.WebClient;
   34 | 
   35 | /**
   36 |  * An auto-configuration that allows the use of a {@link LoadBalanced}
   37 |  * {@link WebClient.Builder} with {@link ReactorLoadBalancerExchangeFilterFunction} and
   38 |  * {@link ReactiveLoadBalancer} used under the hood.
   39 |  *
   40 |  * @author Olga Maciaszek-Sharma
   41 |  * @since 2.2.0
   42 |  */
   43 | @Configuration(proxyBeanMethods = false)
   44 | @ConditionalOnClass(WebClient.class)
   45 | @ConditionalOnBean(ReactiveLoadBalancer.Factory.class)
   46 | @EnableConfigurationProperties(LoadBalancerClientsProperties.class)
   47 | public class ReactorLoadBalancerClientAutoConfiguration {
   48 | 
   49 | 	@ConditionalOnMissingBean
   50 | 	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "false",
   51 | 			matchIfMissing = true)
   52 | 	@Bean
   53 | 	public ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(
   54 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
   55 | 			ObjectProvider<List<LoadBalancerClientRequestTransformer>> transformers) {
   56 | 		return new ReactorLoadBalancerExchangeFilterFunction(loadBalancerFactory,
   57 | 				transformers.getIfAvailable(Collections::emptyList));
   58 | 	}
   59 | 
   60 | 	@ConditionalOnMissingBean
   61 | 	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
   62 | 	@Bean
   63 | 	public RetryableLoadBalancerExchangeFilterFunction retryableLoadBalancerExchangeFilterFunction(
   64 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
   65 | 			LoadBalancerRetryPolicy.Factory retryPolicyFactory,
   66 | 			ObjectProvider<List<LoadBalancerClientRequestTransformer>> transformers) {
   67 | 		return new RetryableLoadBalancerExchangeFilterFunction(retryPolicyFactory, loadBalancerFactory,
   68 | 				transformers.getIfAvailable(Collections::emptyList));
   69 | 	}
   70 | 
   71 | 	@ConditionalOnMissingBean
   72 | 	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true")
   73 | 	@Bean
   74 | 	public LoadBalancerRetryPolicy.Factory loadBalancerRetryPolicy(
   75 | 			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
   76 | 		return new RetryableExchangeFilterFunctionLoadBalancerRetryPolicy.Factory(loadBalancerFactory);
   77 | 	}
   78 | 
   79 | }
```
