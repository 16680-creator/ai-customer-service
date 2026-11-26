<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/NacosServiceManager.java -->
<!-- 正文引用：[[源码:NacosServiceManager.java#L起-L止]] ；代码块头部：(src: NacosServiceManager.java#L起-L止) -->

# NacosServiceManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/NacosServiceManager.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.046Z |

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
   17 | package com.alibaba.cloud.nacos;
   18 | 
   19 | import java.util.Objects;
   20 | import java.util.Properties;
   21 | 
   22 | import com.alibaba.nacos.api.exception.NacosException;
   23 | import com.alibaba.nacos.api.naming.NamingMaintainService;
   24 | import com.alibaba.nacos.api.naming.NamingService;
   25 | import org.slf4j.Logger;
   26 | import org.slf4j.LoggerFactory;
   27 | 
   28 | import static com.alibaba.nacos.api.NacosFactory.createMaintainService;
   29 | import static com.alibaba.nacos.api.NacosFactory.createNamingService;
   30 | 
   31 | /**
   32 |  * @author yuhuangbin
   33 |  */
   34 | public class NacosServiceManager {
   35 | 
   36 | 	private static final Logger log = LoggerFactory.getLogger(NacosServiceManager.class);
   37 | 
   38 | 	private NacosDiscoveryProperties nacosDiscoveryProperties;
   39 | 
   40 | 	private volatile NamingService namingService;
   41 | 
   42 | 	private volatile NamingMaintainService namingMaintainService;
   43 | 
   44 | 	public NamingService getNamingService() {
   45 | 		if (Objects.isNull(this.namingService)) {
   46 | 			buildNamingService(nacosDiscoveryProperties.getNacosProperties());
   47 | 		}
   48 | 		return namingService;
   49 | 	}
   50 | 
   51 | 	@Deprecated
   52 | 	public NamingService getNamingService(Properties properties) {
   53 | 		if (Objects.isNull(this.namingService)) {
   54 | 			buildNamingService(properties);
   55 | 		}
   56 | 		return namingService;
   57 | 	}
   58 | 
   59 | 	public NamingMaintainService getNamingMaintainService(Properties properties) {
   60 | 		if (Objects.isNull(namingMaintainService)) {
   61 | 			buildNamingMaintainService(properties);
   62 | 		}
   63 | 		return namingMaintainService;
   64 | 	}
   65 | 
   66 | 	public boolean isNacosDiscoveryInfoChanged(
   67 | 			NacosDiscoveryProperties currentNacosDiscoveryPropertiesCache) {
   68 | 		if (Objects.isNull(this.nacosDiscoveryProperties)
   69 | 				|| this.nacosDiscoveryProperties.equals(currentNacosDiscoveryPropertiesCache)) {
   70 | 			return false;
   71 | 		}
   72 | 		return true;
   73 | 	}
   74 | 
   75 | 	private NamingMaintainService buildNamingMaintainService(Properties properties) {
   76 | 		if (Objects.isNull(namingMaintainService)) {
   77 | 			synchronized (NacosServiceManager.class) {
   78 | 				if (Objects.isNull(namingMaintainService)) {
   79 | 					namingMaintainService = createNamingMaintainService(properties);
   80 | 				}
   81 | 			}
   82 | 		}
   83 | 		return namingMaintainService;
   84 | 	}
   85 | 
   86 | 	private NamingService buildNamingService(Properties properties) {
   87 | 		if (Objects.isNull(namingService)) {
   88 | 			synchronized (NacosServiceManager.class) {
   89 | 				if (Objects.isNull(namingService)) {
   90 | 					namingService = createNewNamingService(properties);
   91 | 				}
   92 | 			}
   93 | 		}
   94 | 		return namingService;
   95 | 	}
   96 | 
   97 | 	private NamingService createNewNamingService(Properties properties) {
   98 | 		try {
   99 | 			return createNamingService(properties);
  100 | 		}
  101 | 		catch (NacosException e) {
  102 | 			throw new RuntimeException(e);
  103 | 		}
  104 | 	}
  105 | 
  106 | 	private NamingMaintainService createNamingMaintainService(Properties properties) {
  107 | 		try {
  108 | 			return createMaintainService(properties);
  109 | 		}
  110 | 		catch (NacosException e) {
  111 | 			throw new RuntimeException(e);
  112 | 		}
  113 | 	}
  114 | 
  115 | 	public void nacosServiceShutDown() throws NacosException {
  116 | 		if (Objects.nonNull(this.namingService)) {
  117 | 			this.namingService.shutDown();
  118 | 			this.namingService = null;
  119 | 		}
  120 | 		if (Objects.nonNull(this.namingMaintainService)) {
  121 | 			this.namingMaintainService.shutDown();
  122 | 			this.namingMaintainService = null;
  123 | 		}
  124 | 	}
  125 | 
  126 | 	public void setNacosDiscoveryProperties(NacosDiscoveryProperties nacosDiscoveryProperties) {
  127 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
  128 | 	}
  129 | }
```
