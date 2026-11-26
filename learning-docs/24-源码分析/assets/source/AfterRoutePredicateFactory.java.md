<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/AfterRoutePredicateFactory.java -->
<!-- 正文引用：[[源码:AfterRoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: AfterRoutePredicateFactory.java#L起-L止) -->

# AfterRoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/AfterRoutePredicateFactory.java` |
| 行数 | 84 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:49:46.932Z |

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
   19 | import java.time.ZonedDateTime;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | import java.util.function.Predicate;
   23 | 
   24 | import jakarta.validation.constraints.NotNull;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.web.server.ServerWebExchange;
   28 | 
   29 | /**
   30 |  * @author Spencer Gibb
   31 |  */
   32 | public class AfterRoutePredicateFactory extends AbstractRoutePredicateFactory<AfterRoutePredicateFactory.Config> {
   33 | 
   34 | 	/**
   35 | 	 * DateTime key.
   36 | 	 */
   37 | 	public static final String DATETIME_KEY = "datetime";
   38 | 
   39 | 	public AfterRoutePredicateFactory() {
   40 | 		super(Config.class);
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public List<String> shortcutFieldOrder() {
   45 | 		return Collections.singletonList(DATETIME_KEY);
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public Predicate<ServerWebExchange> apply(Config config) {
   50 | 		return new GatewayPredicate() {
   51 | 			@Override
   52 | 			public boolean test(ServerWebExchange serverWebExchange) {
   53 | 				final ZonedDateTime now = ZonedDateTime.now();
   54 | 				return now.isAfter(config.getDatetime());
   55 | 			}
   56 | 
   57 | 			@Override
   58 | 			public Object getConfig() {
   59 | 				return config;
   60 | 			}
   61 | 
   62 | 			@Override
   63 | 			public String toString() {
   64 | 				return String.format("After: %s", config.getDatetime());
   65 | 			}
   66 | 		};
   67 | 	}
   68 | 
   69 | 	public static class Config {
   70 | 
   71 | 		@NotNull
   72 | 		private @Nullable ZonedDateTime datetime;
   73 | 
   74 | 		public @Nullable ZonedDateTime getDatetime() {
   75 | 			return datetime;
   76 | 		}
   77 | 
   78 | 		public void setDatetime(ZonedDateTime datetime) {
   79 | 			this.datetime = datetime;
   80 | 		}
   81 | 
   82 | 	}
   83 | 
   84 | }
```
