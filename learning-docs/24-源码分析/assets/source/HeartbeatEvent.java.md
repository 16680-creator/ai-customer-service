<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/discovery/event/HeartbeatEvent.java -->
<!-- 正文引用：[[源码:HeartbeatEvent.java#L起-L止]] ；代码块头部：(src: HeartbeatEvent.java#L起-L止) -->

# HeartbeatEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/discovery/event/HeartbeatEvent.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.430Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.client.discovery.event;
   18 | 
   19 | import org.springframework.context.ApplicationEvent;
   20 | 
   21 | /**
   22 |  * An event that a DiscoveryClient implementation can broadcast if it supports heartbeats
   23 |  * from the discovery server. Provides listeners with a basic indication of a state change
   24 |  * in the service catalog.
   25 |  *
   26 |  * @author Spencer Gibb
   27 |  * @author Dave Syer
   28 |  */
   29 | @SuppressWarnings("serial")
   30 | public class HeartbeatEvent extends ApplicationEvent {
   31 | 
   32 | 	private final Object state;
   33 | 
   34 | 	/**
   35 | 	 * Creates a new event with a source (for example, a discovery client) and a value.
   36 | 	 * Neither parameter should be relied on to have specific content or format.
   37 | 	 * @param source The source of the event.
   38 | 	 * @param state The value indicating state of the catalog.
   39 | 	 */
   40 | 	public HeartbeatEvent(Object source, Object state) {
   41 | 		super(source);
   42 | 		this.state = state;
   43 | 	}
   44 | 
   45 | 	/**
   46 | 	 * A value representing the state of the service catalog. The only requirement is that
   47 | 	 * it changes when the catalog is updated; it can be as simple as a version counter or
   48 | 	 * a hash. Implementations can provide information to help users visualize what is
   49 | 	 * going on in the catalog, but users should not rely on the content (since the
   50 | 	 * implementation of the underlying discovery might change).
   51 | 	 * @return A value representing state of the service catalog.
   52 | 	 */
   53 | 	public Object getValue() {
   54 | 		return this.state;
   55 | 	}
   56 | 
   57 | }
```
