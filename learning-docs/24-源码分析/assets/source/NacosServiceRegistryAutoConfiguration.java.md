<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/registry/NacosServiceRegistryAutoConfiguration.java -->
<!-- 正文引用：[[源码:NacosServiceRegistryAutoConfiguration.java#L起-L止]] ；代码块头部：(src: NacosServiceRegistryAutoConfiguration.java#L起-L止) -->

# NacosServiceRegistryAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/registry/NacosServiceRegistryAutoConfiguration.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.647Z |

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
   17 | package com.alibaba.cloud.nacos.registry;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
   22 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   23 | import com.alibaba.cloud.nacos.NacosServiceManager;
   24 | import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
   25 | 
   26 | import org.springframework.beans.factory.ObjectProvider;
   27 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   28 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   29 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   30 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   31 | import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
   32 | import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration;
   33 | import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
   34 | import org.springframework.context.ApplicationContext;
   35 | import org.springframework.context.annotation.Bean;
   36 | import org.springframework.context.annotation.Configuration;
   37 | 
   38 | /**
   39 |  * @author xiaojing
   40 |  * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
   41 |  */
   42 | @Configuration(proxyBeanMethods = false)
   43 | @EnableConfigurationProperties
   44 | @ConditionalOnNacosDiscoveryEnabled
   45 | @ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled",
   46 | 		matchIfMissing = true)
   47 | @AutoConfigureAfter({ AutoServiceRegistrationConfiguration.class,
   48 | 		AutoServiceRegistrationAutoConfiguration.class,
   49 | 		NacosDiscoveryAutoConfiguration.class })
   50 | public class NacosServiceRegistryAutoConfiguration {
   51 | 
   52 | 	@Bean
   53 | 	public NacosServiceRegistry nacosServiceRegistry(
   54 | 			NacosServiceManager nacosServiceManager,
   55 | 			NacosDiscoveryProperties nacosDiscoveryProperties) {
   56 | 		return new NacosServiceRegistry(nacosServiceManager, nacosDiscoveryProperties);
   57 | 	}
   58 | 
   59 | 	@Bean
   60 | 	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
   61 | 	public NacosRegistration nacosRegistration(
   62 | 			ObjectProvider<List<NacosRegistrationCustomizer>> registrationCustomizers,
   63 | 			NacosDiscoveryProperties nacosDiscoveryProperties,
   64 | 			ApplicationContext context) {
   65 | 		return new NacosRegistration(registrationCustomizers.getIfAvailable(),
   66 | 				nacosDiscoveryProperties, context);
   67 | 	}
   68 | 
   69 | 	@Bean
   70 | 	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
   71 | 	public NacosAutoServiceRegistration nacosAutoServiceRegistration(
   72 | 			ApplicationContext context,
   73 | 			NacosServiceRegistry registry,
   74 | 			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
   75 | 			NacosRegistration registration) {
   76 | 		return new NacosAutoServiceRegistration(context, registry,
   77 | 				autoServiceRegistrationProperties, registration);
   78 | 	}
   79 | 
   80 | 	@Bean
   81 | 	@ConditionalOnBean(NacosAutoServiceRegistration.class)
   82 | 	public NacosGracefulShutdownDelegate nacosGracefulShutdownDelegate(
   83 | 			NacosAutoServiceRegistration nacosAutoServiceRegistration,
   84 | 			NacosDiscoveryProperties nacosDiscoveryProperties) {
   85 | 		return new NacosGracefulShutdownDelegate(nacosAutoServiceRegistration, nacosDiscoveryProperties);
   86 | 	}
   87 | }
```
