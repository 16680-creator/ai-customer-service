<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/route/builder/RouteLocatorBuilder.java -->
<!-- 正文引用：[[源码:RouteLocatorBuilder.java#L起-L止]] ；代码块头部：(src: RouteLocatorBuilder.java#L起-L止) -->

# RouteLocatorBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/route/builder/RouteLocatorBuilder.java` |
| 行数 | 130 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:48.760Z |

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
   17 | package org.springframework.cloud.gateway.route.builder;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.UUID;
   22 | import java.util.function.Function;
   23 | 
   24 | import reactor.core.publisher.Flux;
   25 | 
   26 | import org.springframework.cloud.gateway.route.Route;
   27 | import org.springframework.cloud.gateway.route.RouteLocator;
   28 | import org.springframework.context.ConfigurableApplicationContext;
   29 | 
   30 | /**
   31 |  * Used to build a {@link RouteLocator}.
   32 |  */
   33 | public class RouteLocatorBuilder {
   34 | 
   35 | 	private ConfigurableApplicationContext context;
   36 | 
   37 | 	public RouteLocatorBuilder(ConfigurableApplicationContext context) {
   38 | 		this.context = context;
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * Creates a new {@link Builder}.
   43 | 	 * @return a new {@link Builder}.
   44 | 	 */
   45 | 	public Builder routes() {
   46 | 		return new Builder(context);
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * A class that can be used to construct routes and return a {@link RouteLocator}.
   51 | 	 */
   52 | 	public static class Builder {
   53 | 
   54 | 		private List<Buildable<Route>> routes = new ArrayList<>();
   55 | 
   56 | 		private ConfigurableApplicationContext context;
   57 | 
   58 | 		public Builder(ConfigurableApplicationContext context) {
   59 | 			this.context = context;
   60 | 		}
   61 | 
   62 | 		/**
   63 | 		 * Creates a new {@link Route}.
   64 | 		 * @param id the unique id for the route
   65 | 		 * @param fn a function which takes in a {@link PredicateSpec} and returns a
   66 | 		 * {@link Route.AsyncBuilder}
   67 | 		 * @return a {@link Builder}
   68 | 		 */
   69 | 		public Builder route(String id, Function<PredicateSpec, Buildable<Route>> fn) {
   70 | 			Buildable<Route> routeBuilder = fn.apply(new RouteSpec(this).id(id));
   71 | 			add(routeBuilder);
   72 | 			return this;
   73 | 		}
   74 | 
   75 | 		/**
   76 | 		 * Creates a new {@link Route}.
   77 | 		 * @param fn a function which takes in a {@link PredicateSpec} and returns a
   78 | 		 * {@link Route.AsyncBuilder}
   79 | 		 * @return a {@link Builder}
   80 | 		 */
   81 | 		public Builder route(Function<PredicateSpec, Buildable<Route>> fn) {
   82 | 			Buildable<Route> routeBuilder = fn.apply(new RouteSpec(this).randomId());
   83 | 			add(routeBuilder);
   84 | 			return this;
   85 | 		}
   86 | 
   87 | 		/**
   88 | 		 * Builds and returns a {@link RouteLocator}.
   89 | 		 * @return a {@link RouteLocator}
   90 | 		 */
   91 | 		public RouteLocator build() {
   92 | 			return () -> Flux.fromIterable(this.routes).map(routeBuilder -> routeBuilder.build());
   93 | 		}
   94 | 
   95 | 		ConfigurableApplicationContext getContext() {
   96 | 			return context;
   97 | 		}
   98 | 
   99 | 		void add(Buildable<Route> route) {
  100 | 			routes.add(route);
  101 | 		}
  102 | 
  103 | 	}
  104 | 
  105 | 	public static class RouteSpec {
  106 | 
  107 | 		private final Route.AsyncBuilder routeBuilder = Route.async();
  108 | 
  109 | 		private final Builder builder;
  110 | 
  111 | 		RouteSpec(Builder builder) {
  112 | 			this.builder = builder;
  113 | 		}
  114 | 
  115 | 		public PredicateSpec id(String id) {
  116 | 			this.routeBuilder.id(id);
  117 | 			return predicateBuilder();
  118 | 		}
  119 | 
  120 | 		public PredicateSpec randomId() {
  121 | 			return id(UUID.randomUUID().toString());
  122 | 		}
  123 | 
  124 | 		private PredicateSpec predicateBuilder() {
  125 | 			return new PredicateSpec(this.routeBuilder, this.builder);
  126 | 		}
  127 | 
  128 | 	}
  129 | 
  130 | }
```
