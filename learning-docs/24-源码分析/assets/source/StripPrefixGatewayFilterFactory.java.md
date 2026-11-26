<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/StripPrefixGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:StripPrefixGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: StripPrefixGatewayFilterFactory.java#L起-L止) -->

# StripPrefixGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/StripPrefixGatewayFilterFactory.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:47.013Z |

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
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | 
   22 | import reactor.core.publisher.Mono;
   23 | 
   24 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   25 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   26 | import org.springframework.http.server.reactive.ServerHttpRequest;
   27 | import org.springframework.util.StringUtils;
   28 | import org.springframework.web.server.ServerWebExchange;
   29 | 
   30 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   31 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   32 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;
   33 | 
   34 | /**
   35 |  * This filter removes the first part of the path, known as the prefix, from the request
   36 |  * before sending it downstream.
   37 |  *
   38 |  * @author Ryan Baxter
   39 |  */
   40 | public class StripPrefixGatewayFilterFactory
   41 | 		extends AbstractGatewayFilterFactory<StripPrefixGatewayFilterFactory.Config> {
   42 | 
   43 | 	/**
   44 | 	 * Parts key.
   45 | 	 */
   46 | 	public static final String PARTS_KEY = "parts";
   47 | 
   48 | 	public StripPrefixGatewayFilterFactory() {
   49 | 		super(Config.class);
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public List<String> shortcutFieldOrder() {
   54 | 		return Arrays.asList(PARTS_KEY);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public GatewayFilter apply(Config config) {
   59 | 		return new GatewayFilter() {
   60 | 			@Override
   61 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   62 | 				ServerHttpRequest request = exchange.getRequest();
   63 | 				addOriginalRequestUrl(exchange, request.getURI());
   64 | 				String path = request.getURI().getRawPath();
   65 | 				String[] originalParts = StringUtils.tokenizeToStringArray(path, "/");
   66 | 
   67 | 				// all new paths start with /
   68 | 				StringBuilder newPath = new StringBuilder("/");
   69 | 				for (int i = 0; i < originalParts.length; i++) {
   70 | 					if (i >= config.getParts()) {
   71 | 						// only append slash if this is the second part or greater
   72 | 						if (newPath.length() > 1) {
   73 | 							newPath.append('/');
   74 | 						}
   75 | 						newPath.append(originalParts[i]);
   76 | 					}
   77 | 				}
   78 | 				if (newPath.length() > 1 && path.endsWith("/")) {
   79 | 					newPath.append('/');
   80 | 				}
   81 | 
   82 | 				ServerHttpRequest newRequest = request.mutate().path(newPath.toString()).build();
   83 | 
   84 | 				exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());
   85 | 
   86 | 				return chain.filter(exchange.mutate().request(newRequest).build());
   87 | 			}
   88 | 
   89 | 			@Override
   90 | 			public String toString() {
   91 | 				return filterToStringCreator(StripPrefixGatewayFilterFactory.this).append("parts", config.getParts())
   92 | 					.toString();
   93 | 			}
   94 | 		};
   95 | 	}
   96 | 
   97 | 	public static class Config {
   98 | 
   99 | 		private int parts = 1;
  100 | 
  101 | 		public int getParts() {
  102 | 			return parts;
  103 | 		}
  104 | 
  105 | 		public void setParts(int parts) {
  106 | 			this.parts = parts;
  107 | 		}
  108 | 
  109 | 	}
  110 | 
  111 | }
```
