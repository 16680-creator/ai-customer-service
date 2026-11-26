<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/discovery/event/InstanceRegisteredEvent.java -->
<!-- 正文引用：[[源码:InstanceRegisteredEvent.java#L起-L止]] ；代码块头部：(src: InstanceRegisteredEvent.java#L起-L止) -->

# InstanceRegisteredEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/discovery/event/InstanceRegisteredEvent.java` |
| 行数 | 55 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.529Z |

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
   20 | import org.springframework.core.ResolvableType;
   21 | import org.springframework.core.ResolvableTypeProvider;
   22 | 
   23 | /**
   24 |  * Event to be published after the local service instance registers itself with a
   25 |  * discovery service.
   26 |  *
   27 |  * @param <T> - type of configuration
   28 |  * @author Spencer Gibb
   29 |  * @author Yanming Zhou
   30 |  */
   31 | @SuppressWarnings("serial")
   32 | public class InstanceRegisteredEvent<T> extends ApplicationEvent implements ResolvableTypeProvider {
   33 | 
   34 | 	private T config;
   35 | 
   36 | 	/**
   37 | 	 * Creates a new {@link InstanceRegisteredEvent} instance.
   38 | 	 * @param source The component that published the event (never {@code null}).
   39 | 	 * @param config The configuration of the instance.
   40 | 	 */
   41 | 	public InstanceRegisteredEvent(Object source, T config) {
   42 | 		super(source);
   43 | 		this.config = config;
   44 | 	}
   45 | 
   46 | 	public T getConfig() {
   47 | 		return this.config;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public ResolvableType getResolvableType() {
   52 | 		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(this.config));
   53 | 	}
   54 | 
   55 | }
```
