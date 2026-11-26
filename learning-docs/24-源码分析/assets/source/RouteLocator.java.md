<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/route/RouteLocator.java -->
<!-- 正文引用：[[源码:RouteLocator.java#L起-L止]] ；代码块头部：(src: RouteLocator.java#L起-L止) -->

# RouteLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/route/RouteLocator.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:45.829Z |

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
   17 | package org.springframework.cloud.gateway.route;
   18 | 
   19 | import java.util.Map;
   20 | 
   21 | import reactor.core.publisher.Flux;
   22 | 
   23 | import org.springframework.util.CollectionUtils;
   24 | 
   25 | /**
   26 |  * @author Spencer Gibb
   27 |  */
   28 | // TODO: rename to Routes?
   29 | public interface RouteLocator {
   30 | 
   31 | 	Flux<Route> getRoutes();
   32 | 
   33 | 	/**
   34 | 	 * Gets routes whose {@link Route#getId()} matches with any of the ids passed by
   35 | 	 * parameters. If an ID cannot be found, it will not return a route for that ID.
   36 | 	 */
   37 | 	default Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
   38 | 		return getRoutes().filter(route -> matchMetadata(route.getMetadata(), metadata));
   39 | 	}
   40 | 
   41 | 	static boolean matchMetadata(Map<String, Object> toCheck, Map<String, Object> expectedMetadata) {
   42 | 		if (CollectionUtils.isEmpty(expectedMetadata)) {
   43 | 			return true;
   44 | 		}
   45 | 		else {
   46 | 			return toCheck != null && expectedMetadata.entrySet()
   47 | 				.stream()
   48 | 				.allMatch(keyValue -> toCheck.containsKey(keyValue.getKey())
   49 | 						&& toCheck.get(keyValue.getKey()).equals(keyValue.getValue()));
   50 | 		}
   51 | 	}
   52 | 
   53 | }
```
