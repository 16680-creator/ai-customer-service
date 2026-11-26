<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/config/LoadBalancerAutoConfiguration.java -->
<!-- 正文引用：[[源码:LoadBalancerAutoConfiguration.java#L起-L止]] ；代码块头部：(src: LoadBalancerAutoConfiguration.java#L起-L止) -->

# LoadBalancerAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/config/LoadBalancerAutoConfiguration.java` |
| 行数 | 80 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:03.802Z |

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
   19 | import java.util.Collections;
   20 | import java.util.List;
   21 | 
   22 | import org.springframework.beans.factory.ObjectProvider;
   23 | import org.springframework.boot.autoconfigure.AutoConfigureBefore;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   25 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   26 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   27 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
   28 | import org.springframework.cloud.client.loadbalancer.LoadBalancerEagerLoadProperties;
   29 | import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerBeanPostProcessorAutoConfiguration;
   30 | import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerClientAutoConfiguration;
   31 | import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
   32 | import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
   33 | import org.springframework.cloud.loadbalancer.aot.LoadBalancerChildContextInitializer;
   34 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   35 | import org.springframework.cloud.loadbalancer.support.LoadBalancerEagerContextInitializer;
   36 | import org.springframework.context.ApplicationContext;
   37 | import org.springframework.context.annotation.Bean;
   38 | import org.springframework.context.annotation.Configuration;
   39 | import org.springframework.core.env.Environment;
   40 | 
   41 | /**
   42 |  * @author Spencer Gibb
   43 |  * @author Olga Maciaszek-Sharma
   44 |  */
   45 | @Configuration(proxyBeanMethods = false)
   46 | @LoadBalancerClients
   47 | @EnableConfigurationProperties({ LoadBalancerClientsProperties.class, LoadBalancerEagerLoadProperties.class })
   48 | @AutoConfigureBefore({ ReactorLoadBalancerClientAutoConfiguration.class,
   49 | 		LoadBalancerBeanPostProcessorAutoConfiguration.class })
   50 | @ConditionalOnProperty(value = "spring.cloud.loadbalancer.enabled", havingValue = "true", matchIfMissing = true)
   51 | public class LoadBalancerAutoConfiguration {
   52 | 
   53 | 	@Bean
   54 | 	@ConditionalOnMissingBean
   55 | 	public LoadBalancerZoneConfig zoneConfig(Environment environment) {
   56 | 		return new LoadBalancerZoneConfig(environment.getProperty("spring.cloud.loadbalancer.zone"));
   57 | 	}
   58 | 
   59 | 	@ConditionalOnMissingBean
   60 | 	@Bean
   61 | 	public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties,
   62 | 			ObjectProvider<List<LoadBalancerClientSpecification>> configurations) {
   63 | 		LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory(properties);
   64 | 		clientFactory.setConfigurations(configurations.getIfAvailable(Collections::emptyList));
   65 | 		return clientFactory;
   66 | 	}
   67 | 
   68 | 	@Bean
   69 | 	public LoadBalancerEagerContextInitializer loadBalancerEagerContextInitializer(
   70 | 			LoadBalancerClientFactory clientFactory, LoadBalancerEagerLoadProperties properties) {
   71 | 		return new LoadBalancerEagerContextInitializer(clientFactory, properties.getClients());
   72 | 	}
   73 | 
   74 | 	@Bean
   75 | 	static LoadBalancerChildContextInitializer loadBalancerChildContextInitializer(
   76 | 			LoadBalancerClientFactory loadBalancerClientFactory, ApplicationContext parentContext) {
   77 | 		return new LoadBalancerChildContextInitializer(loadBalancerClientFactory, parentContext);
   78 | 	}
   79 | 
   80 | }
```
