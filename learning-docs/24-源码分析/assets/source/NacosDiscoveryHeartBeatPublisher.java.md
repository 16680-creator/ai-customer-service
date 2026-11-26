<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/discovery/NacosDiscoveryHeartBeatPublisher.java -->
<!-- 正文引用：[[源码:NacosDiscoveryHeartBeatPublisher.java#L起-L止]] ；代码块头部：(src: NacosDiscoveryHeartBeatPublisher.java#L起-L止) -->

# NacosDiscoveryHeartBeatPublisher.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/discovery/NacosDiscoveryHeartBeatPublisher.java` |
| 行数 | 106 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:22.577Z |

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
   19 | import java.time.Duration;
   20 | import java.util.concurrent.ScheduledFuture;
   21 | import java.util.concurrent.atomic.AtomicBoolean;
   22 | import java.util.concurrent.atomic.AtomicLong;
   23 | 
   24 | import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
   25 | import org.slf4j.Logger;
   26 | import org.slf4j.LoggerFactory;
   27 | 
   28 | import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
   29 | import org.springframework.context.ApplicationEventPublisher;
   30 | import org.springframework.context.ApplicationEventPublisherAware;
   31 | import org.springframework.context.SmartLifecycle;
   32 | import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
   33 | 
   34 | 
   35 | /**
   36 |  * @author yuhuangbin
   37 |  * @author ruansheng
   38 |  */
   39 | public class NacosDiscoveryHeartBeatPublisher implements ApplicationEventPublisherAware, SmartLifecycle {
   40 | 
   41 | 	private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryHeartBeatPublisher.class);
   42 | 
   43 | 	private final NacosDiscoveryProperties nacosDiscoveryProperties;
   44 | 
   45 | 	private final ThreadPoolTaskScheduler taskScheduler;
   46 | 	private final AtomicLong nacosHeartBeatIndex = new AtomicLong(0);
   47 | 	private final AtomicBoolean running = new AtomicBoolean(false);
   48 | 	private ApplicationEventPublisher publisher;
   49 | 	private ScheduledFuture<?> heartBeatFuture;
   50 | 
   51 | 	public NacosDiscoveryHeartBeatPublisher(NacosDiscoveryProperties nacosDiscoveryProperties) {
   52 | 		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
   53 | 		this.taskScheduler = getTaskScheduler();
   54 | 	}
   55 | 
   56 | 	private static ThreadPoolTaskScheduler getTaskScheduler() {
   57 | 		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
   58 | 		taskScheduler.setBeanName("HeartBeat-Task-Scheduler");
   59 | 		taskScheduler.initialize();
   60 | 		return taskScheduler;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public void start() {
   65 | 		if (this.running.compareAndSet(false, true)) {
   66 | 			log.info("Start nacos heartBeat task scheduler.");
   67 | 			this.heartBeatFuture = this.taskScheduler.scheduleWithFixedDelay(
   68 | 					this::publishHeartBeat, Duration.ofMillis(this.nacosDiscoveryProperties.getWatchDelay()));
   69 | 		}
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public void stop() {
   74 | 		if (this.running.compareAndSet(true, false)) {
   75 | 			if (this.heartBeatFuture != null) {
   76 | 				// shutdown current user-thread,
   77 | 				// then the other daemon-threads will terminate automatic.
   78 | 				this.taskScheduler.shutdown();
   79 | 				this.heartBeatFuture.cancel(true);
   80 | 			}
   81 | 		}
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public boolean isAutoStartup() {
   86 | 		return true;
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	public boolean isRunning() {
   91 | 		return this.running.get();
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
   96 | 		this.publisher = applicationEventPublisher;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * nacos doesn't support watch now , publish an event every 30 seconds.
  101 | 	 */
  102 | 	public void publishHeartBeat() {
  103 | 		HeartbeatEvent event = new HeartbeatEvent(this, nacosHeartBeatIndex.getAndIncrement());
  104 | 		this.publisher.publishEvent(event);
  105 | 	}
  106 | }
```
