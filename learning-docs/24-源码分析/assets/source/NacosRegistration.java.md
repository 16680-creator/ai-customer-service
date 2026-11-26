<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/registry/NacosRegistration.java -->
<!-- 正文引用：[[源码:NacosRegistration.java#L起-L止]] ；代码块头部：(src: NacosRegistration.java#L起-L止) -->

# NacosRegistration.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/registry/NacosRegistration.java` |
| 行数 | 184 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.567Z |

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
   19 | import java.net.URI;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | 
   23 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   24 | import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
   25 | import jakarta.annotation.PostConstruct;
   26 | 
   27 | import org.springframework.beans.BeanUtils;
   28 | import org.springframework.cloud.client.ServiceInstance;
   29 | import org.springframework.cloud.client.discovery.ManagementServerPortUtils;
   30 | import org.springframework.cloud.client.serviceregistry.Registration;
   31 | import org.springframework.context.ApplicationContext;
   32 | import org.springframework.core.env.Environment;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * @author xiaojing
   37 |  * @author changjin wei(魏昌进)
   38 |  */
   39 | public class NacosRegistration implements Registration {
   40 | 
   41 | 	/**
   42 | 	 * The metadata key of management port.
   43 | 	 */
   44 | 	public static final String MANAGEMENT_PORT = "management.port";
   45 | 
   46 | 	/**
   47 | 	 * The metadata key of management context-path.
   48 | 	 */
   49 | 	public static final String MANAGEMENT_CONTEXT_PATH = "management.context-path";
   50 | 
   51 | 	/**
   52 | 	 * The metadata key of management address.
   53 | 	 */
   54 | 	public static final String MANAGEMENT_ADDRESS = "management.address";
   55 | 
   56 | 	/**
   57 | 	 * The metadata key of management endpoints web base path.
   58 | 	 */
   59 | 	public static final String MANAGEMENT_ENDPOINT_BASE_PATH = "management.endpoints.web.base-path";
   60 | 
   61 | 	private List<NacosRegistrationCustomizer> registrationCustomizers;
   62 | 
   63 | 	private NacosDiscoveryProperties nacosDiscoveryProperties;
   64 | 
   65 | 	private ApplicationContext context;
   66 | 
   67 | 	public NacosRegistration(List<NacosRegistrationCustomizer> registrationCustomizers,
   68 | 			NacosDiscoveryProperties nacosDiscoveryProperties,
   69 | 			ApplicationContext context) {
   70 | 		this.registrationCustomizers = registrationCustomizers;
   71 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
   72 | 		this.context = context;
   73 | 	}
   74 | 
   75 | 	@PostConstruct
   76 | 	public void init() {
   77 | 
   78 | 		Map<String, String> metadata = nacosDiscoveryProperties.getMetadata();
   79 | 		Environment env = context.getEnvironment();
   80 | 
   81 | 		String endpointBasePath = env.getProperty(MANAGEMENT_ENDPOINT_BASE_PATH);
   82 | 		if (StringUtils.hasLength(endpointBasePath)) {
   83 | 			metadata.put(MANAGEMENT_ENDPOINT_BASE_PATH, endpointBasePath);
   84 | 		}
   85 | 
   86 | 		Integer managementPort = ManagementServerPortUtils.getPort(context);
   87 | 		if (null != managementPort) {
   88 | 			metadata.put(MANAGEMENT_PORT, managementPort.toString());
   89 | 			String contextPath = env
   90 | 					.getProperty("management.server.servlet.context-path");
   91 | 			String address = env.getProperty("management.server.address");
   92 | 			if (StringUtils.hasLength(contextPath)) {
   93 | 				metadata.put(MANAGEMENT_CONTEXT_PATH, contextPath);
   94 | 			}
   95 | 			if (StringUtils.hasLength(address)) {
   96 | 				metadata.put(MANAGEMENT_ADDRESS, address);
   97 | 			}
   98 | 		}
   99 | 
  100 | 		if (null != nacosDiscoveryProperties.getHeartBeatInterval()) {
  101 | 			metadata.put(PreservedMetadataKeys.HEART_BEAT_INTERVAL,
  102 | 					nacosDiscoveryProperties.getHeartBeatInterval().toString());
  103 | 		}
  104 | 		if (null != nacosDiscoveryProperties.getHeartBeatTimeout()) {
  105 | 			metadata.put(PreservedMetadataKeys.HEART_BEAT_TIMEOUT,
  106 | 					nacosDiscoveryProperties.getHeartBeatTimeout().toString());
  107 | 		}
  108 | 		if (null != nacosDiscoveryProperties.getIpDeleteTimeout()) {
  109 | 			metadata.put(PreservedMetadataKeys.IP_DELETE_TIMEOUT,
  110 | 					nacosDiscoveryProperties.getIpDeleteTimeout().toString());
  111 | 		}
  112 | 		customize(registrationCustomizers);
  113 | 	}
  114 | 
  115 | 	protected void customize(
  116 | 			List<NacosRegistrationCustomizer> registrationCustomizers) {
  117 | 		if (registrationCustomizers != null) {
  118 | 			for (NacosRegistrationCustomizer customizer : registrationCustomizers) {
  119 | 				customizer.customize(this);
  120 | 			}
  121 | 		}
  122 | 	}
  123 | 
  124 | 	@Override
  125 | 	public String getServiceId() {
  126 | 		return nacosDiscoveryProperties.getService();
  127 | 	}
  128 | 
  129 | 	@Override
  130 | 	public String getHost() {
  131 | 		return nacosDiscoveryProperties.getIp();
  132 | 	}
  133 | 
  134 | 	@Override
  135 | 	public int getPort() {
  136 | 		return nacosDiscoveryProperties.getPort();
  137 | 	}
  138 | 
  139 | 	public void setPort(int port) {
  140 | 		this.nacosDiscoveryProperties.setPort(port);
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public boolean isSecure() {
  145 | 		return nacosDiscoveryProperties.isSecure();
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	public URI getUri() {
  150 | 		return ServiceInstance.createUri(this);
  151 | 	}
  152 | 
  153 | 	@Override
  154 | 	public Map<String, String> getMetadata() {
  155 | 		return nacosDiscoveryProperties.getMetadata();
  156 | 	}
  157 | 
  158 | 	public boolean isRegisterEnabled() {
  159 | 		return nacosDiscoveryProperties.isRegisterEnabled();
  160 | 	}
  161 | 
  162 | 	public String getCluster() {
  163 | 		return nacosDiscoveryProperties.getClusterName();
  164 | 	}
  165 | 
  166 | 	public float getRegisterWeight() {
  167 | 		return nacosDiscoveryProperties.getWeight();
  168 | 	}
  169 | 
  170 | 	public NacosDiscoveryProperties getNacosDiscoveryProperties() {
  171 | 		return nacosDiscoveryProperties;
  172 | 	}
  173 | 
  174 | 	@Override
  175 | 	public String toString() {
  176 | 		NacosDiscoveryProperties safeProp = new NacosDiscoveryProperties();
  177 | 		BeanUtils.copyProperties(nacosDiscoveryProperties, safeProp);
  178 | 		safeProp.setUsername("******");
  179 | 		safeProp.setPassword("******");
  180 | 		return "NacosRegistration{" + "nacosDiscoveryProperties="
  181 | 				+ safeProp + '}';
  182 | 	}
  183 | 
  184 | }
```
