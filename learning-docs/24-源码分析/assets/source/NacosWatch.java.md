<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/NacosWatch.java -->
<!-- 正文引用：[[源码:NacosWatch.java#L起-L止]] ；代码块头部：(src: NacosWatch.java#L起-L止) -->

# NacosWatch.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/NacosWatch.java` |
| 行数 | 152 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.118Z |

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
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Optional;
   23 | import java.util.concurrent.ConcurrentHashMap;
   24 | import java.util.concurrent.atomic.AtomicBoolean;
   25 | 
   26 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   27 | import com.alibaba.cloud.nacos.NacosServiceManager;
   28 | import com.alibaba.nacos.api.naming.NamingService;
   29 | import com.alibaba.nacos.api.naming.listener.Event;
   30 | import com.alibaba.nacos.api.naming.listener.EventListener;
   31 | import com.alibaba.nacos.api.naming.listener.NamingEvent;
   32 | import com.alibaba.nacos.api.naming.pojo.Instance;
   33 | import org.slf4j.Logger;
   34 | import org.slf4j.LoggerFactory;
   35 | 
   36 | import org.springframework.beans.factory.DisposableBean;
   37 | import org.springframework.context.SmartLifecycle;
   38 | 
   39 | /**
   40 |  * @author xiaojing
   41 |  * @author yuhuangbin
   42 |  * @author pengfei.lu
   43 |  * @author ruansheng
   44 |  */
   45 | public class NacosWatch implements SmartLifecycle, DisposableBean {
   46 | 
   47 | 	private static final Logger log = LoggerFactory.getLogger(NacosWatch.class);
   48 | 
   49 | 	private final Map<String, EventListener> listenerMap = new ConcurrentHashMap<>(16);
   50 | 
   51 | 	private final AtomicBoolean running = new AtomicBoolean(false);
   52 | 
   53 | 	private final NacosServiceManager nacosServiceManager;
   54 | 
   55 | 	private final NacosDiscoveryProperties properties;
   56 | 
   57 | 	public NacosWatch(NacosServiceManager nacosServiceManager,
   58 | 			NacosDiscoveryProperties properties) {
   59 | 		this.nacosServiceManager = nacosServiceManager;
   60 | 		this.properties = properties;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public boolean isAutoStartup() {
   65 | 		return true;
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public void stop(Runnable callback) {
   70 | 		this.stop();
   71 | 		callback.run();
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public void start() {
   76 | 		if (this.running.compareAndSet(false, true)) {
   77 | 			EventListener eventListener = listenerMap.computeIfAbsent(buildKey(),
   78 | 					event -> new EventListener() {
   79 | 						@Override
   80 | 						public void onEvent(Event event) {
   81 | 							if (event instanceof NamingEvent namingEvent) {
   82 | 								List<Instance> instances = namingEvent.getInstances();
   83 | 								Optional<Instance> instanceOptional = selectCurrentInstance(
   84 | 										instances);
   85 | 								instanceOptional.ifPresent(currentInstance -> {
   86 | 									resetIfNeeded(currentInstance);
   87 | 								});
   88 | 							}
   89 | 						}
   90 | 					});
   91 | 
   92 | 			NamingService namingService = nacosServiceManager.getNamingService();
   93 | 			try {
   94 | 				namingService.subscribe(properties.getService(), properties.getGroup(),
   95 | 						Arrays.asList(properties.getClusterName()), eventListener);
   96 | 			}
   97 | 			catch (Exception e) {
   98 | 				log.error("namingService subscribe failed, properties:{}", properties, e);
   99 | 			}
  100 | 
  101 | 		}
  102 | 	}
  103 | 
  104 | 	private String buildKey() {
  105 | 		return String.join(":", properties.getService(), properties.getGroup());
  106 | 	}
  107 | 
  108 | 	private void resetIfNeeded(Instance instance) {
  109 | 		if (!properties.getMetadata().equals(instance.getMetadata())) {
  110 | 			properties.setMetadata(instance.getMetadata());
  111 | 		}
  112 | 	}
  113 | 
  114 | 	private Optional<Instance> selectCurrentInstance(List<Instance> instances) {
  115 | 		return instances.stream()
  116 | 				.filter(instance -> properties.getIp().equals(instance.getIp())
  117 | 						&& properties.getPort() == instance.getPort())
  118 | 				.findFirst();
  119 | 	}
  120 | 
  121 | 	@Override
  122 | 	public void stop() {
  123 | 		if (this.running.compareAndSet(true, false)) {
  124 | 
  125 | 			EventListener eventListener = listenerMap.get(buildKey());
  126 | 			try {
  127 | 				NamingService namingService = nacosServiceManager.getNamingService();
  128 | 				namingService.unsubscribe(properties.getService(), properties.getGroup(),
  129 | 						Arrays.asList(properties.getClusterName()), eventListener);
  130 | 			}
  131 | 			catch (Exception e) {
  132 | 				log.error("namingService unsubscribe failed, properties:{}", properties,
  133 | 						e);
  134 | 			}
  135 | 		}
  136 | 	}
  137 | 
  138 | 	@Override
  139 | 	public boolean isRunning() {
  140 | 		return this.running.get();
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public int getPhase() {
  145 | 		return 0;
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	public void destroy() {
  150 | 		this.stop();
  151 | 	}
  152 | }
```
