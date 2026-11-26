<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/NacosDiscoveryAutoConfiguration.java -->
<!-- 正文引用：[[源码:NacosDiscoveryAutoConfiguration.java#L起-L止]] ；代码块头部：(src: NacosDiscoveryAutoConfiguration.java#L起-L止) -->

# NacosDiscoveryAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/NacosDiscoveryAutoConfiguration.java` |
| 行数 | 50 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.446Z |

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
   17 | package com.alibaba.cloud.nacos.discovery;
   18 | 
   19 | import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
   20 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   21 | import com.alibaba.cloud.nacos.NacosServiceManager;
   22 | 
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   24 | import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
   25 | import org.springframework.context.annotation.Bean;
   26 | import org.springframework.context.annotation.Configuration;
   27 | 
   28 | /**
   29 |  * @author <a href="mailto:echooy.mxq@gmail.com">echooymxq</a>
   30 |  **/
   31 | @Configuration(proxyBeanMethods = false)
   32 | @ConditionalOnDiscoveryEnabled
   33 | @ConditionalOnNacosDiscoveryEnabled
   34 | public class NacosDiscoveryAutoConfiguration {
   35 | 
   36 | 	@Bean
   37 | 	@ConditionalOnMissingBean
   38 | 	public NacosDiscoveryProperties nacosProperties() {
   39 | 		return new NacosDiscoveryProperties();
   40 | 	}
   41 | 
   42 | 	@Bean
   43 | 	@ConditionalOnMissingBean
   44 | 	public NacosServiceDiscovery nacosServiceDiscovery(
   45 | 			NacosDiscoveryProperties discoveryProperties,
   46 | 			NacosServiceManager nacosServiceManager) {
   47 | 		return new NacosServiceDiscovery(discoveryProperties, nacosServiceManager);
   48 | 	}
   49 | 
   50 | }
```
