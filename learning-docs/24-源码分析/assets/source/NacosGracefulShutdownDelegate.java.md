<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/registry/NacosGracefulShutdownDelegate.java -->
<!-- 正文引用：[[源码:NacosGracefulShutdownDelegate.java#L起-L止]] ；代码块头部：(src: NacosGracefulShutdownDelegate.java#L起-L止) -->

# NacosGracefulShutdownDelegate.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/registry/NacosGracefulShutdownDelegate.java` |
| 行数 | 88 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.353Z |

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
   19 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   20 | import com.alibaba.nacos.common.utils.ThreadUtils;
   21 | import org.slf4j.Logger;
   22 | import org.slf4j.LoggerFactory;
   23 | 
   24 | import org.springframework.beans.BeansException;
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.context.ApplicationContextAware;
   27 | import org.springframework.context.ApplicationListener;
   28 | import org.springframework.context.event.ContextClosedEvent;
   29 | import org.springframework.lang.NonNull;
   30 | 
   31 | /**
   32 |  * @author <a href="mailto:uuuyuqi@gmail.com">uuuyuqi</a>
   33 |  */
   34 | public class NacosGracefulShutdownDelegate implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware {
   35 | 
   36 | 	private static final Logger log = LoggerFactory.getLogger(NacosGracefulShutdownDelegate.class);
   37 | 
   38 | 	private final NacosAutoServiceRegistration autoServiceRegistration;
   39 | 
   40 | 	private final NacosDiscoveryProperties nacosDiscoveryProperties;
   41 | 
   42 | 	private ApplicationContext applicationContext;
   43 | 
   44 | 	public NacosGracefulShutdownDelegate(NacosAutoServiceRegistration autoServiceRegistration,
   45 | 		NacosDiscoveryProperties nacosDiscoveryProperties) {
   46 | 		this.autoServiceRegistration = autoServiceRegistration;
   47 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
   52 | 		this.applicationContext = applicationContext;
   53 | 	}
   54 | 
   55 | 	@Override
   56 | 	public void onApplicationEvent(@NonNull ContextClosedEvent event) {
   57 | 		// should NOT be executed if ContextClosedEvent published by sub contexts
   58 | 		if (!applicationContext.equals(event.getApplicationContext())) {
   59 | 			log.debug("Nacos client graceful shutdown will NOT be executed "
   60 | 					+ "for Spring context source: {}", event.getApplicationContext());
   61 | 			return;
   62 | 		}
   63 | 
   64 | 		doGracefulShutdown();
   65 | 	}
   66 | 
   67 | 	protected void doGracefulShutdown() {
   68 | 		try {
   69 | 			autoServiceRegistration.stop();
   70 | 			Integer gracefulShutdownWaitTime = this.nacosDiscoveryProperties.getGracefulShutdownWaitTime();
   71 | 			if (gracefulShutdownWaitTime != null && gracefulShutdownWaitTime > 0) {
   72 | 				ThreadUtils.sleep(gracefulShutdownWaitTime);
   73 | 			}
   74 | 
   75 | 			log.info("Nacos client graceful shutdown has been executed successfully. " +
   76 | 					"Graceful shutdown wait time is {}", gracefulShutdownWaitTime);
   77 | 		}
   78 | 		catch (Throwable t) {
   79 | 			log.error("Error occurred while performing Nacos client graceful shutdown", t);
   80 | 		}
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public boolean supportsAsyncExecution() {
   85 | 		// need wait for graceful shutdown
   86 | 		return false;
   87 | 	}
   88 | }
```
