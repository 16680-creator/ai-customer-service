<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/GatewayFilterFactory.java -->
<!-- 正文引用：[[源码:GatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: GatewayFilterFactory.java#L起-L止) -->

# GatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/GatewayFilterFactory.java` |
| 行数 | 82 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:26.607Z |

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
   19 | import java.util.function.Consumer;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   24 | import org.springframework.cloud.gateway.support.Configurable;
   25 | import org.springframework.cloud.gateway.support.HasRouteId;
   26 | import org.springframework.cloud.gateway.support.NameUtils;
   27 | import org.springframework.cloud.gateway.support.ShortcutConfigurable;
   28 | 
   29 | /**
   30 |  * @author Spencer Gibb
   31 |  */
   32 | @FunctionalInterface
   33 | public interface GatewayFilterFactory<C> extends ShortcutConfigurable, Configurable<C> {
   34 | 
   35 | 	/**
   36 | 	 * Name key.
   37 | 	 */
   38 | 	String NAME_KEY = "name";
   39 | 
   40 | 	/**
   41 | 	 * Value key.
   42 | 	 */
   43 | 	String VALUE_KEY = "value";
   44 | 
   45 | 	// useful for javadsl
   46 | 	default GatewayFilter apply(@Nullable String routeId, Consumer<C> consumer) {
   47 | 		C config = newConfig();
   48 | 		consumer.accept(config);
   49 | 		return apply(routeId, config);
   50 | 	}
   51 | 
   52 | 	default GatewayFilter apply(Consumer<C> consumer) {
   53 | 		C config = newConfig();
   54 | 		consumer.accept(config);
   55 | 		return apply(config);
   56 | 	}
   57 | 
   58 | 	default Class<C> getConfigClass() {
   59 | 		throw new UnsupportedOperationException("getConfigClass() not implemented");
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	default C newConfig() {
   64 | 		throw new UnsupportedOperationException("newConfig() not implemented");
   65 | 	}
   66 | 
   67 | 	GatewayFilter apply(C config);
   68 | 
   69 | 	default GatewayFilter apply(@Nullable String routeId, C config) {
   70 | 		if (config instanceof HasRouteId) {
   71 | 			HasRouteId hasRouteId = (HasRouteId) config;
   72 | 			hasRouteId.setRouteId(routeId);
   73 | 		}
   74 | 		return apply(config);
   75 | 	}
   76 | 
   77 | 	default String name() {
   78 | 		// TODO: deal with proxys
   79 | 		return NameUtils.normalizeFilterFactoryName(getClass());
   80 | 	}
   81 | 
   82 | }
```
