<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/NacosServiceDiscovery.java -->
<!-- 正文引用：[[源码:NacosServiceDiscovery.java#L起-L止]] ；代码块头部：(src: NacosServiceDiscovery.java#L起-L止) -->

# NacosServiceDiscovery.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/NacosServiceDiscovery.java` |
| 行数 | 120 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.789Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.HashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | 
   24 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   25 | import com.alibaba.cloud.nacos.NacosServiceInstance;
   26 | import com.alibaba.cloud.nacos.NacosServiceManager;
   27 | import com.alibaba.nacos.api.exception.NacosException;
   28 | import com.alibaba.nacos.api.naming.NamingService;
   29 | import com.alibaba.nacos.api.naming.pojo.Instance;
   30 | import com.alibaba.nacos.api.naming.pojo.ListView;
   31 | 
   32 | import org.springframework.cloud.client.ServiceInstance;
   33 | 
   34 | /**
   35 |  * @author <a href="mailto:echooy.mxq@gmail.com">echooymxq</a>
   36 |  * @author changjin wei(魏昌进)
   37 |  **/
   38 | public class NacosServiceDiscovery {
   39 | 
   40 | 	private NacosDiscoveryProperties discoveryProperties;
   41 | 
   42 | 	private NacosServiceManager nacosServiceManager;
   43 | 
   44 | 	public NacosServiceDiscovery(NacosDiscoveryProperties discoveryProperties,
   45 | 			NacosServiceManager nacosServiceManager) {
   46 | 		this.discoveryProperties = discoveryProperties;
   47 | 		this.nacosServiceManager = nacosServiceManager;
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * Return all instances for the given service.
   52 | 	 * @param serviceId id of service
   53 | 	 * @return list of instances
   54 | 	 * @throws NacosException nacosException
   55 | 	 */
   56 | 	public List<ServiceInstance> getInstances(String serviceId) throws NacosException {
   57 | 		String group = discoveryProperties.getGroup();
   58 | 		List<Instance> instances = namingService().selectInstances(serviceId, group,
   59 | 				true);
   60 | 		return hostToServiceInstanceList(instances, serviceId);
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Return the names of all services.
   65 | 	 * @return list of service names
   66 | 	 * @throws NacosException nacosException
   67 | 	 */
   68 | 	public List<String> getServices() throws NacosException {
   69 | 		String group = discoveryProperties.getGroup();
   70 | 		ListView<String> services = namingService().getServicesOfServer(1,
   71 | 				Integer.MAX_VALUE, group);
   72 | 		return services.getData();
   73 | 	}
   74 | 
   75 | 	public static List<ServiceInstance> hostToServiceInstanceList(
   76 | 			List<Instance> instances, String serviceId) {
   77 | 		List<ServiceInstance> result = new ArrayList<>(instances.size());
   78 | 		for (Instance instance : instances) {
   79 | 			ServiceInstance serviceInstance = hostToServiceInstance(instance, serviceId);
   80 | 			if (serviceInstance != null) {
   81 | 				result.add(serviceInstance);
   82 | 			}
   83 | 		}
   84 | 		return result;
   85 | 	}
   86 | 
   87 | 	public static ServiceInstance hostToServiceInstance(Instance instance,
   88 | 			String serviceId) {
   89 | 		if (instance == null || !instance.isEnabled() || !instance.isHealthy()) {
   90 | 			return null;
   91 | 		}
   92 | 		NacosServiceInstance nacosServiceInstance = new NacosServiceInstance();
   93 | 		nacosServiceInstance.setHost(instance.getIp());
   94 | 		nacosServiceInstance.setPort(instance.getPort());
   95 | 		nacosServiceInstance.setServiceId(serviceId);
   96 | 		nacosServiceInstance.setInstanceId(instance.getInstanceId());
   97 | 
   98 | 		Map<String, String> metadata = new HashMap<>();
   99 | 		metadata.put("nacos.instanceId", instance.getInstanceId());
  100 | 		metadata.put("nacos.weight", instance.getWeight() + "");
  101 | 		metadata.put("nacos.healthy", instance.isHealthy() + "");
  102 | 		metadata.put("nacos.cluster", instance.getClusterName() + "");
  103 | 		if (instance.getMetadata() != null) {
  104 | 			metadata.putAll(instance.getMetadata());
  105 | 		}
  106 | 		metadata.put("nacos.ephemeral", String.valueOf(instance.isEphemeral()));
  107 | 		nacosServiceInstance.setMetadata(metadata);
  108 | 
  109 | 		if (metadata.containsKey("secure")) {
  110 | 			boolean secure = Boolean.parseBoolean(metadata.get("secure"));
  111 | 			nacosServiceInstance.setSecure(secure);
  112 | 		}
  113 | 		return nacosServiceInstance;
  114 | 	}
  115 | 
  116 | 	private NamingService namingService() {
  117 | 		return nacosServiceManager.getNamingService();
  118 | 	}
  119 | 
  120 | }
```
