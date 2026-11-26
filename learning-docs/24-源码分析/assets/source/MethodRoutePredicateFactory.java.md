<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/MethodRoutePredicateFactory.java -->
<!-- 正文引用：[[源码:MethodRoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: MethodRoutePredicateFactory.java#L起-L止) -->

# MethodRoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/MethodRoutePredicateFactory.java` |
| 行数 | 83 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.417Z |

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
   17 | package org.springframework.cloud.gateway.handler.predicate;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | import java.util.function.Predicate;
   22 | 
   23 | import org.springframework.http.HttpMethod;
   24 | import org.springframework.web.server.ServerWebExchange;
   25 | 
   26 | import static java.util.Arrays.stream;
   27 | 
   28 | /**
   29 |  * @author Spencer Gibb
   30 |  * @author Dennis Menge
   31 |  */
   32 | public class MethodRoutePredicateFactory extends AbstractRoutePredicateFactory<MethodRoutePredicateFactory.Config> {
   33 | 
   34 | 	/**
   35 | 	 * Methods key.
   36 | 	 */
   37 | 	public static final String METHODS_KEY = "methods";
   38 | 
   39 | 	public MethodRoutePredicateFactory() {
   40 | 		super(Config.class);
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public List<String> shortcutFieldOrder() {
   45 | 		return List.of(METHODS_KEY);
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public ShortcutType shortcutType() {
   50 | 		return ShortcutType.GATHER_LIST;
   51 | 	}
   52 | 
   53 | 	@Override
   54 | 	public Predicate<ServerWebExchange> apply(Config config) {
   55 | 		return new GatewayPredicate() {
   56 | 			@Override
   57 | 			public boolean test(ServerWebExchange exchange) {
   58 | 				HttpMethod requestMethod = exchange.getRequest().getMethod();
   59 | 				return stream(config.getMethods()).anyMatch(httpMethod -> httpMethod == requestMethod);
   60 | 			}
   61 | 
   62 | 			@Override
   63 | 			public String toString() {
   64 | 				return String.format("Methods: %s", Arrays.toString(config.getMethods()));
   65 | 			}
   66 | 		};
   67 | 	}
   68 | 
   69 | 	public static class Config {
   70 | 
   71 | 		private HttpMethod[] methods = new HttpMethod[0];
   72 | 
   73 | 		public HttpMethod[] getMethods() {
   74 | 			return methods;
   75 | 		}
   76 | 
   77 | 		public void setMethods(HttpMethod... methods) {
   78 | 			this.methods = methods;
   79 | 		}
   80 | 
   81 | 	}
   82 | 
   83 | }
```
