<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/RoutePredicateFactory.java -->
<!-- 正文引用：[[源码:RoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: RoutePredicateFactory.java#L起-L止) -->

# RoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/RoutePredicateFactory.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:26.680Z |

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
   19 | import java.util.function.Consumer;
   20 | import java.util.function.Predicate;
   21 | 
   22 | import org.springframework.cloud.gateway.handler.AsyncPredicate;
   23 | import org.springframework.cloud.gateway.support.Configurable;
   24 | import org.springframework.cloud.gateway.support.NameUtils;
   25 | import org.springframework.cloud.gateway.support.ShortcutConfigurable;
   26 | import org.springframework.web.server.ServerWebExchange;
   27 | 
   28 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;
   29 | 
   30 | /**
   31 |  * @author Spencer Gibb
   32 |  */
   33 | @FunctionalInterface
   34 | public interface RoutePredicateFactory<C> extends ShortcutConfigurable, Configurable<C> {
   35 | 
   36 | 	/**
   37 | 	 * Pattern key.
   38 | 	 */
   39 | 	String PATTERN_KEY = "pattern";
   40 | 
   41 | 	// useful for javadsl
   42 | 	default Predicate<ServerWebExchange> apply(Consumer<C> consumer) {
   43 | 		C config = newConfig();
   44 | 		consumer.accept(config);
   45 | 		beforeApply(config);
   46 | 		return apply(config);
   47 | 	}
   48 | 
   49 | 	default AsyncPredicate<ServerWebExchange> applyAsync(Consumer<C> consumer) {
   50 | 		C config = newConfig();
   51 | 		consumer.accept(config);
   52 | 		beforeApply(config);
   53 | 		return applyAsync(config);
   54 | 	}
   55 | 
   56 | 	default Class<C> getConfigClass() {
   57 | 		throw new UnsupportedOperationException("getConfigClass() not implemented");
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	default C newConfig() {
   62 | 		throw new UnsupportedOperationException("newConfig() not implemented");
   63 | 	}
   64 | 
   65 | 	default void beforeApply(C config) {
   66 | 	}
   67 | 
   68 | 	Predicate<ServerWebExchange> apply(C config);
   69 | 
   70 | 	default AsyncPredicate<ServerWebExchange> applyAsync(C config) {
   71 | 		return toAsyncPredicate(apply(config));
   72 | 	}
   73 | 
   74 | 	default String name() {
   75 | 		return NameUtils.normalizeRoutePredicateName(getClass());
   76 | 	}
   77 | 
   78 | }
```
