<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/config/BlockingLoadBalancerClientAutoConfiguration.java -->
<!-- 正文引用：[[源码:BlockingLoadBalancerClientAutoConfiguration.java#L起-L止]] ；代码块头部：(src: BlockingLoadBalancerClientAutoConfiguration.java#L起-L止) -->

# BlockingLoadBalancerClientAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/config/BlockingLoadBalancerClientAutoConfiguration.java` |
| 行数 | 95 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.872Z |

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
   17 | package org.springframework.cloud.loadbalancer.config;
   18 | 
   19 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   20 | import org.springframework.boot.autoconfigure.AutoConfigureBefore;
   21 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   22 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   25 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   26 | import org.springframework.cloud.client.ServiceInstance;
   27 | import org.springframework.cloud.client.loadbalancer.BlockingRestClassesPresentCondition;
   28 | import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
   29 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
   30 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
   31 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   32 | import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
   33 | import org.springframework.cloud.loadbalancer.blocking.XForwardedHeadersTransformer;
   34 | import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
   35 | import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryFactory;
   36 | import org.springframework.cloud.loadbalancer.core.LoadBalancerServiceInstanceCookieTransformer;
   37 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   38 | import org.springframework.context.annotation.Bean;
   39 | import org.springframework.context.annotation.Conditional;
   40 | import org.springframework.context.annotation.Configuration;
   41 | import org.springframework.retry.support.RetryTemplate;
   42 | 
   43 | /**
   44 |  * An autoconfiguration for {@link BlockingLoadBalancerClient}.
   45 |  *
   46 |  * @author Olga Maciaszek-Sharma
   47 |  * @author Gandhimathi Velusamy
   48 |  * @since 2.1.3
   49 |  */
   50 | @Configuration(proxyBeanMethods = false)
   51 | @LoadBalancerClients
   52 | @AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
   53 | @AutoConfigureBefore({ org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class })
   54 | @Conditional(BlockingRestClassesPresentCondition.class)
   55 | @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
   56 | public class BlockingLoadBalancerClientAutoConfiguration {
   57 | 
   58 | 	@Bean
   59 | 	@ConditionalOnBean(LoadBalancerClientFactory.class)
   60 | 	@ConditionalOnMissingBean
   61 | 	public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
   62 | 		return new BlockingLoadBalancerClient(loadBalancerClientFactory);
   63 | 	}
   64 | 
   65 | 	@Bean
   66 | 	@ConditionalOnBean(LoadBalancerClientFactory.class)
   67 | 	@ConditionalOnMissingBean(LoadBalancerServiceInstanceCookieTransformer.class)
   68 | 	public LoadBalancerServiceInstanceCookieTransformer loadBalancerServiceInstanceCookieTransformer(
   69 | 			LoadBalancerClientFactory loadBalancerClientFactory) {
   70 | 		return new LoadBalancerServiceInstanceCookieTransformer(loadBalancerClientFactory);
   71 | 	}
   72 | 
   73 | 	@Bean
   74 | 	@ConditionalOnMissingBean(XForwardedHeadersTransformer.class)
   75 | 	@ConditionalOnBean(LoadBalancerClientFactory.class)
   76 | 	public XForwardedHeadersTransformer xForwarderHeadersTransformer(
   77 | 			LoadBalancerClientFactory loadBalancerClientFactory) {
   78 | 		return new XForwardedHeadersTransformer(loadBalancerClientFactory);
   79 | 	}
   80 | 
   81 | 	@Configuration
   82 | 	@ConditionalOnClass(RetryTemplate.class)
   83 | 	@EnableConfigurationProperties(LoadBalancerClientsProperties.class)
   84 | 	protected static class BlockingLoadBalancerRetryConfig {
   85 | 
   86 | 		@Bean
   87 | 		@ConditionalOnMissingBean
   88 | 		LoadBalancedRetryFactory loadBalancedRetryFactory(
   89 | 				ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
   90 | 			return new BlockingLoadBalancedRetryFactory(loadBalancerFactory);
   91 | 		}
   92 | 
   93 | 	}
   94 | 
   95 | }
```
