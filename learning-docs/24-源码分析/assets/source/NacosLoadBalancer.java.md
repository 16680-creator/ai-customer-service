<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/loadbalancer/NacosLoadBalancer.java -->
<!-- 正文引用：[[源码:NacosLoadBalancer.java#L起-L止]] ；代码块头部：(src: NacosLoadBalancer.java#L起-L止) -->

# NacosLoadBalancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/loadbalancer/NacosLoadBalancer.java` |
| 行数 | 186 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.277Z |

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
   17 | package com.alibaba.cloud.nacos.loadbalancer;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.regex.Pattern;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import com.alibaba.cloud.commons.lang.StringUtils;
   26 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   27 | import com.alibaba.cloud.nacos.util.InetIPv6Utils;
   28 | import com.alibaba.nacos.common.utils.CollectionUtils;
   29 | import jakarta.annotation.PostConstruct;
   30 | import org.slf4j.Logger;
   31 | import org.slf4j.LoggerFactory;
   32 | import reactor.core.publisher.Mono;
   33 | 
   34 | import org.springframework.beans.factory.ObjectProvider;
   35 | import org.springframework.cloud.client.ServiceInstance;
   36 | import org.springframework.cloud.client.loadbalancer.DefaultResponse;
   37 | import org.springframework.cloud.client.loadbalancer.EmptyResponse;
   38 | import org.springframework.cloud.client.loadbalancer.Request;
   39 | import org.springframework.cloud.client.loadbalancer.Response;
   40 | import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
   41 | import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
   42 | import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
   43 | 
   44 | /**
   45 |  * see original.
   46 |  * {@link org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer}
   47 |  *
   48 |  * @author XuDaojie
   49 |  * @since 2021.1
   50 |  */
   51 | public class NacosLoadBalancer implements ReactorServiceInstanceLoadBalancer {
   52 | 
   53 | 	private static final Logger log = LoggerFactory.getLogger(NacosLoadBalancer.class);
   54 | 
   55 | 	private final String serviceId;
   56 | 
   57 | 	private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
   58 | 
   59 | 	private final NacosDiscoveryProperties nacosDiscoveryProperties;
   60 | 
   61 | 	private static final String IPV4_REGEX = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
   62 | 
   63 | 	private static final String IPV6_KEY = "IPv6";
   64 | 	/**
   65 | 	 * Storage local valid IPv6 address, it's a flag whether local machine support IPv6 address stack.
   66 | 	 */
   67 | 	public static String ipv6;
   68 | 
   69 | 	private final InetIPv6Utils inetIPv6Utils;
   70 | 
   71 | 	private final List<ServiceInstanceFilter> serviceInstanceFilters;
   72 | 
   73 | 	private final Map<String, LoadBalancerAlgorithm> loadBalancerAlgorithmMap;
   74 | 
   75 | 	@PostConstruct
   76 | 	public void init() {
   77 | 		String ip = nacosDiscoveryProperties.getIp();
   78 | 		if (StringUtils.isNotEmpty(ip)) {
   79 | 			ipv6 = Pattern.matches(IPV4_REGEX, ip) ? nacosDiscoveryProperties.getMetadata().get(IPV6_KEY) : ip;
   80 | 		}
   81 | 		else {
   82 | 			ipv6 = inetIPv6Utils.findIPv6Address();
   83 | 		}
   84 | 	}
   85 | 
   86 | 	private List<ServiceInstance> filterInstanceByIpType(List<ServiceInstance> instances) {
   87 | 		if (StringUtils.isNotEmpty(ipv6)) {
   88 | 			List<ServiceInstance> ipv6InstanceList = new ArrayList<>();
   89 | 			for (ServiceInstance instance : instances) {
   90 | 				if (Pattern.matches(IPV4_REGEX, instance.getHost())) {
   91 | 					if (StringUtils.isNotEmpty(instance.getMetadata().get(IPV6_KEY))) {
   92 | 						ipv6InstanceList.add(instance);
   93 | 					}
   94 | 				}
   95 | 				else {
   96 | 					ipv6InstanceList.add(instance);
   97 | 				}
   98 | 			}
   99 | 			// Provider has no IPv6, should use IPv4.
  100 | 			if (ipv6InstanceList.isEmpty()) {
  101 | 				return instances.stream()
  102 | 						.filter(instance -> Pattern.matches(IPV4_REGEX, instance.getHost()))
  103 | 						.collect(Collectors.toList());
  104 | 			}
  105 | 			else {
  106 | 				return ipv6InstanceList;
  107 | 			}
  108 | 		}
  109 | 		return instances.stream()
  110 | 				.filter(instance -> Pattern.matches(IPV4_REGEX, instance.getHost()))
  111 | 				.collect(Collectors.toList());
  112 | 	}
  113 | 
  114 | 	public NacosLoadBalancer(
  115 | 			ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
  116 | 			String serviceId, NacosDiscoveryProperties nacosDiscoveryProperties, InetIPv6Utils inetIPv6Utils,
  117 | 			List<ServiceInstanceFilter> serviceInstanceFilters,
  118 | 			Map<String, LoadBalancerAlgorithm> loadBalancerAlgorithmMap) {
  119 | 		this.serviceId = serviceId;
  120 | 		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
  121 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
  122 | 		this.inetIPv6Utils = inetIPv6Utils;
  123 | 		this.serviceInstanceFilters = serviceInstanceFilters;
  124 | 		this.loadBalancerAlgorithmMap = loadBalancerAlgorithmMap;
  125 | 	}
  126 | 
  127 | 	@Override
  128 | 	public Mono<Response<ServiceInstance>> choose(Request request) {
  129 | 		ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
  130 | 				.getIfAvailable(NoopServiceInstanceListSupplier::new);
  131 | 		return supplier.get(request).next().map(serviceInstances -> getInstanceResponse(request, serviceInstances));
  132 | 	}
  133 | 
  134 | 	private Response<ServiceInstance> getInstanceResponse(Request<?> request,
  135 | 			List<ServiceInstance> serviceInstances) {
  136 | 		if (serviceInstances.isEmpty()) {
  137 | 			log.warn("No servers available for service: {}", this.serviceId);
  138 | 			return new EmptyResponse();
  139 | 		}
  140 | 
  141 | 		try {
  142 | 			String clusterName = this.nacosDiscoveryProperties.getClusterName();
  143 | 
  144 | 			List<ServiceInstance> instancesToChoose = serviceInstances;
  145 | 			if (StringUtils.isNotBlank(clusterName)) {
  146 | 				List<ServiceInstance> sameClusterInstances = serviceInstances.stream()
  147 | 						.filter(serviceInstance -> {
  148 | 							String cluster = serviceInstance.getMetadata()
  149 | 									.get("nacos.cluster");
  150 | 							return StringUtils.equals(cluster, clusterName);
  151 | 						}).collect(Collectors.toList());
  152 | 				if (!CollectionUtils.isEmpty(sameClusterInstances)) {
  153 | 					instancesToChoose = sameClusterInstances;
  154 | 				}
  155 | 			}
  156 | 			else {
  157 | 				log.warn(
  158 | 						"A cross-cluster call occurs，name = {}, clusterName = {}, instance = {}",
  159 | 						serviceId, clusterName, serviceInstances);
  160 | 			}
  161 | 			instancesToChoose = this.filterInstanceByIpType(instancesToChoose);
  162 | 
  163 | 			// Filter the service list sequentially based on the order number
  164 | 			for (ServiceInstanceFilter filter : serviceInstanceFilters) {
  165 | 				instancesToChoose = filter.filterInstance(request, instancesToChoose);
  166 | 			}
  167 | 
  168 | 			ServiceInstance instance;
  169 | 			// Find the corresponding load balancing algorithm through the service ID and select the final service instance
  170 | 			if (loadBalancerAlgorithmMap.containsKey(serviceId)) {
  171 | 				instance = loadBalancerAlgorithmMap.get(serviceId).getInstance(request, instancesToChoose);
  172 | 			}
  173 | 			else {
  174 | 				instance = loadBalancerAlgorithmMap.get(LoadBalancerAlgorithm.DEFAULT_SERVICE_ID)
  175 | 						.getInstance(request, instancesToChoose);
  176 | 			}
  177 | 
  178 | 			return new DefaultResponse(instance);
  179 | 		}
  180 | 		catch (Exception e) {
  181 | 			log.warn("NacosLoadBalancer error", e);
  182 | 			return null;
  183 | 		}
  184 | 	}
  185 | 
  186 | }
```
