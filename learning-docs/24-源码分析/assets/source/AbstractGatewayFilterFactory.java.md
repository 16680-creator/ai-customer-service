<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/AbstractGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:AbstractGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: AbstractGatewayFilterFactory.java#L起-L止) -->

# AbstractGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/AbstractGatewayFilterFactory.java` |
| 行数 | 75 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:26.484Z |

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
   17 | package org.springframework.cloud.gateway.filter.factory;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
   22 | import org.springframework.cloud.gateway.support.AbstractConfigurable;
   23 | import org.springframework.context.ApplicationEventPublisher;
   24 | import org.springframework.context.ApplicationEventPublisherAware;
   25 | 
   26 | /**
   27 |  * This class is BETA and may be subject to change in a future release.
   28 |  *
   29 |  * @param <C> {@link AbstractConfigurable} subtype
   30 |  */
   31 | public abstract class AbstractGatewayFilterFactory<C> extends AbstractConfigurable<C>
   32 | 		implements GatewayFilterFactory<C>, ApplicationEventPublisherAware {
   33 | 
   34 | 	private @Nullable ApplicationEventPublisher publisher;
   35 | 
   36 | 	@SuppressWarnings("unchecked")
   37 | 	public AbstractGatewayFilterFactory() {
   38 | 		super((Class<C>) Object.class);
   39 | 	}
   40 | 
   41 | 	public AbstractGatewayFilterFactory(Class<C> configClass) {
   42 | 		super(configClass);
   43 | 	}
   44 | 
   45 | 	protected @Nullable ApplicationEventPublisher getPublisher() {
   46 | 		return this.publisher;
   47 | 	}
   48 | 
   49 | 	protected void enableBodyCaching(@Nullable String routeId) {
   50 | 		if (routeId != null && getPublisher() != null) {
   51 | 			// send an event to enable caching
   52 | 			getPublisher().publishEvent(new EnableBodyCachingEvent(this, routeId));
   53 | 		}
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
   58 | 		this.publisher = publisher;
   59 | 	}
   60 | 
   61 | 	public static class NameConfig {
   62 | 
   63 | 		private @Nullable String name;
   64 | 
   65 | 		public @Nullable String getName() {
   66 | 			return name;
   67 | 		}
   68 | 
   69 | 		public void setName(String name) {
   70 | 			this.name = name;
   71 | 		}
   72 | 
   73 | 	}
   74 | 
   75 | }
```
