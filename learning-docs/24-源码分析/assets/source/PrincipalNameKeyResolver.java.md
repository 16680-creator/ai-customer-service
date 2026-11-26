<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ratelimit/PrincipalNameKeyResolver.java -->
<!-- 正文引用：[[源码:PrincipalNameKeyResolver.java#L起-L止]] ；代码块头部：(src: PrincipalNameKeyResolver.java#L起-L止) -->

# PrincipalNameKeyResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ratelimit/PrincipalNameKeyResolver.java` |
| 行数 | 35 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T08:49:13.968Z |

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
   19 | import reactor.core.publisher.Mono;
   20 | 
   21 | import org.springframework.web.server.ServerWebExchange;
   22 | 
   23 | public class PrincipalNameKeyResolver implements KeyResolver {
   24 | 
   25 | 	/**
   26 | 	 * {@link PrincipalNameKeyResolver} bean name.
   27 | 	 */
   28 | 	public static final String BEAN_NAME = "principalNameKeyResolver";
   29 | 
   30 | 	@Override
   31 | 	public Mono<String> resolve(ServerWebExchange exchange) {
   32 | 		return exchange.getPrincipal().flatMap(p -> Mono.justOrEmpty(p.getName()));
   33 | 	}
   34 | 
   35 | }
```
