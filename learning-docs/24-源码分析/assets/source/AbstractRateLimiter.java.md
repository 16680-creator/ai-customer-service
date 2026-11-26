<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ratelimit/AbstractRateLimiter.java -->
<!-- 正文引用：[[源码:AbstractRateLimiter.java#L起-L止]] ；代码块头部：(src: AbstractRateLimiter.java#L起-L止) -->

# AbstractRateLimiter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ratelimit/AbstractRateLimiter.java` |
| 行数 | 83 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.127Z |

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
   17 | package org.springframework.cloud.gateway.filter.ratelimit;
   18 | 
   19 | import java.util.Map;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.cloud.gateway.event.FilterArgsEvent;
   24 | import org.springframework.cloud.gateway.support.AbstractStatefulConfigurable;
   25 | import org.springframework.cloud.gateway.support.ConfigurationService;
   26 | import org.springframework.context.ApplicationListener;
   27 | import org.springframework.core.style.ToStringCreator;
   28 | 
   29 | public abstract class AbstractRateLimiter<C> extends AbstractStatefulConfigurable<C>
   30 | 		implements RateLimiter<C>, ApplicationListener<FilterArgsEvent> {
   31 | 
   32 | 	private String configurationPropertyName;
   33 | 
   34 | 	private @Nullable ConfigurationService configurationService;
   35 | 
   36 | 	protected AbstractRateLimiter(Class<C> configClass, String configurationPropertyName,
   37 | 			@Nullable ConfigurationService configurationService) {
   38 | 		super(configClass);
   39 | 		this.configurationPropertyName = configurationPropertyName;
   40 | 		this.configurationService = configurationService;
   41 | 	}
   42 | 
   43 | 	protected String getConfigurationPropertyName() {
   44 | 		return configurationPropertyName;
   45 | 	}
   46 | 
   47 | 	protected void setConfigurationService(ConfigurationService configurationService) {
   48 | 		this.configurationService = configurationService;
   49 | 	}
   50 | 
   51 | 	@Override
   52 | 	public void onApplicationEvent(FilterArgsEvent event) {
   53 | 		Map<String, Object> args = event.getArgs();
   54 | 
   55 | 		if (args.isEmpty() || !hasRelevantKey(args)) {
   56 | 			return;
   57 | 		}
   58 | 
   59 | 		String routeId = event.getRouteId();
   60 | 
   61 | 		C routeConfig = newConfig();
   62 | 		if (this.configurationService != null) {
   63 | 			this.configurationService.with(routeConfig)
   64 | 				.name(this.configurationPropertyName)
   65 | 				.normalizedProperties(args)
   66 | 				.bind();
   67 | 		}
   68 | 		getConfig().put(routeId, routeConfig);
   69 | 	}
   70 | 
   71 | 	private boolean hasRelevantKey(Map<String, Object> args) {
   72 | 		return args.keySet().stream().anyMatch(key -> key.startsWith(configurationPropertyName + "."));
   73 | 	}
   74 | 
   75 | 	@Override
   76 | 	public String toString() {
   77 | 		return new ToStringCreator(this).append("configurationPropertyName", configurationPropertyName)
   78 | 			.append("config", getConfig())
   79 | 			.append("configClass", getConfigClass())
   80 | 			.toString();
   81 | 	}
   82 | 
   83 | }
```
