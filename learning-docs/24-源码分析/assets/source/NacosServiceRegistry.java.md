<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/registry/NacosServiceRegistry.java -->
<!-- 正文引用：[[源码:NacosServiceRegistry.java#L起-L止]] ；代码块头部：(src: NacosServiceRegistry.java#L起-L止) -->

# NacosServiceRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/registry/NacosServiceRegistry.java` |
| 行数 | 186 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.491Z |

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
   21 | import com.alibaba.cloud.commons.lang.StringUtils;
   22 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   23 | import com.alibaba.cloud.nacos.NacosServiceManager;
   24 | import com.alibaba.nacos.api.exception.NacosException;
   25 | import com.alibaba.nacos.api.naming.NamingService;
   26 | import com.alibaba.nacos.api.naming.pojo.Instance;
   27 | import org.slf4j.Logger;
   28 | import org.slf4j.LoggerFactory;
   29 | 
   30 | import org.springframework.cloud.client.serviceregistry.Registration;
   31 | import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
   32 | 
   33 | import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;
   34 | 
   35 | /**
   36 |  * @author xiaojing
   37 |  * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
   38 |  * @author <a href="mailto:78552423@qq.com">eshun</a>
   39 |  * @author JAY
   40 |  */
   41 | public class NacosServiceRegistry implements ServiceRegistry<Registration> {
   42 | 
   43 | 	private static final String STATUS_UP = "UP";
   44 | 
   45 | 	private static final String STATUS_DOWN = "DOWN";
   46 | 
   47 | 	private static final Logger log = LoggerFactory.getLogger(NacosServiceRegistry.class);
   48 | 
   49 | 	private final NacosDiscoveryProperties nacosDiscoveryProperties;
   50 | 
   51 | 	private final NacosServiceManager nacosServiceManager;
   52 | 
   53 | 	public NacosServiceRegistry(NacosServiceManager nacosServiceManager,
   54 | 			NacosDiscoveryProperties nacosDiscoveryProperties) {
   55 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
   56 | 		this.nacosServiceManager = nacosServiceManager;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public void register(Registration registration) {
   61 | 
   62 | 		if (StringUtils.isEmpty(registration.getServiceId())) {
   63 | 			log.warn("No service to register for nacos client...");
   64 | 			return;
   65 | 		}
   66 | 
   67 | 		NamingService namingService = namingService();
   68 | 		String serviceId = registration.getServiceId();
   69 | 		String group = nacosDiscoveryProperties.getGroup();
   70 | 
   71 | 		Instance instance = getNacosInstanceFromRegistration(registration);
   72 | 
   73 | 		try {
   74 | 			namingService.registerInstance(serviceId, group, instance);
   75 | 			log.info("nacos registry, {} {} {}:{} register finished", group, serviceId,
   76 | 					instance.getIp(), instance.getPort());
   77 | 		}
   78 | 		catch (Exception e) {
   79 | 			if (nacosDiscoveryProperties.isFailFast()) {
   80 | 				log.error("nacos registry, {} register failed...{},", serviceId,
   81 | 						registration.toString(), e);
   82 | 				rethrowRuntimeException(e);
   83 | 			}
   84 | 			else {
   85 | 				log.warn("Failfast is false. {} register failed...{},", serviceId,
   86 | 						registration.toString(), e);
   87 | 			}
   88 | 		}
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public void deregister(Registration registration) {
   93 | 
   94 | 		log.info("De-registering from Nacos Server now...");
   95 | 
   96 | 		if (StringUtils.isEmpty(registration.getServiceId())) {
   97 | 			log.warn("No dom to de-register for nacos client...");
   98 | 			return;
   99 | 		}
  100 | 
  101 | 		NamingService namingService = namingService();
  102 | 		String serviceId = registration.getServiceId();
  103 | 		String group = nacosDiscoveryProperties.getGroup();
  104 | 
  105 | 		try {
  106 | 			namingService.deregisterInstance(serviceId, group, registration.getHost(),
  107 | 					registration.getPort(), nacosDiscoveryProperties.getClusterName());
  108 | 		}
  109 | 		catch (Exception e) {
  110 | 			log.error("ERR_NACOS_DEREGISTER, de-register failed...{},",
  111 | 					registration.toString(), e);
  112 | 		}
  113 | 
  114 | 		log.info("De-registration finished.");
  115 | 	}
  116 | 
  117 | 	@Override
  118 | 	public void close() {
  119 | 		try {
  120 | 			nacosServiceManager.nacosServiceShutDown();
  121 | 		}
  122 | 		catch (NacosException e) {
  123 | 			log.error("Nacos namingService shutDown failed", e);
  124 | 		}
  125 | 	}
  126 | 
  127 | 	@Override
  128 | 	public void setStatus(Registration registration, String status) {
  129 | 
  130 | 		if (!STATUS_UP.equalsIgnoreCase(status)
  131 | 				&& !STATUS_DOWN.equalsIgnoreCase(status)) {
  132 | 			log.warn("can't support status {},please choose UP or DOWN", status);
  133 | 			return;
  134 | 		}
  135 | 
  136 | 		try {
  137 | 			if (STATUS_DOWN.equalsIgnoreCase(status)) {
  138 | 				deregister(registration);
  139 | 			}
  140 | 			else {
  141 | 				register(registration);
  142 | 			}
  143 | 		}
  144 | 		catch (Exception e) {
  145 | 			throw new RuntimeException("update nacos instance status fail", e);
  146 | 		}
  147 | 	}
  148 | 
  149 | 	@Override
  150 | 	public Object getStatus(Registration registration) {
  151 | 
  152 | 		String serviceName = registration.getServiceId();
  153 | 		String group = nacosDiscoveryProperties.getGroup();
  154 | 		try {
  155 | 			List<Instance> instances = namingService().getAllInstances(serviceName,
  156 | 					group);
  157 | 			for (Instance instance : instances) {
  158 | 				if (instance.getIp().equalsIgnoreCase(nacosDiscoveryProperties.getIp())
  159 | 						&& instance.getPort() == nacosDiscoveryProperties.getPort()) {
  160 | 					return instance.isEnabled() ? STATUS_UP : STATUS_DOWN;
  161 | 				}
  162 | 			}
  163 | 		}
  164 | 		catch (Exception e) {
  165 | 			log.error("get all instance of {} error,", serviceName, e);
  166 | 		}
  167 | 		return null;
  168 | 	}
  169 | 
  170 | 	private Instance getNacosInstanceFromRegistration(Registration registration) {
  171 | 		Instance instance = new Instance();
  172 | 		instance.setIp(registration.getHost());
  173 | 		instance.setPort(registration.getPort());
  174 | 		instance.setWeight(nacosDiscoveryProperties.getWeight());
  175 | 		instance.setClusterName(nacosDiscoveryProperties.getClusterName());
  176 | 		instance.setEnabled(nacosDiscoveryProperties.isInstanceEnabled());
  177 | 		instance.setMetadata(registration.getMetadata());
  178 | 		instance.setEphemeral(nacosDiscoveryProperties.isEphemeral());
  179 | 		return instance;
  180 | 	}
  181 | 
  182 | 	private NamingService namingService() {
  183 | 		return nacosServiceManager.getNamingService();
  184 | 	}
  185 | 
  186 | }
```
