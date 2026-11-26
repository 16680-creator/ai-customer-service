<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/route/CachingRouteLocator.java -->
<!-- 正文引用：[[源码:CachingRouteLocator.java#L起-L止]] ；代码块头部：(src: CachingRouteLocator.java#L起-L止) -->

# CachingRouteLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/route/CachingRouteLocator.java` |
| 行数 | 143 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:46.727Z |

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
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.concurrent.ConcurrentHashMap;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | import reactor.core.publisher.Flux;
   29 | import reactor.core.publisher.Mono;
   30 | import reactor.core.publisher.Signal;
   31 | 
   32 | import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
   33 | import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
   34 | import org.springframework.cloud.gateway.support.CacheFlux;
   35 | import org.springframework.context.ApplicationEventPublisher;
   36 | import org.springframework.context.ApplicationEventPublisherAware;
   37 | import org.springframework.context.ApplicationListener;
   38 | import org.springframework.core.Ordered;
   39 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   40 | 
   41 | /**
   42 |  * @author Spencer Gibb
   43 |  */
   44 | public class CachingRouteLocator
   45 | 		implements Ordered, RouteLocator, ApplicationListener<RefreshRoutesEvent>, ApplicationEventPublisherAware {
   46 | 
   47 | 	private static final Log log = LogFactory.getLog(CachingRouteLocator.class);
   48 | 
   49 | 	private static final String CACHE_KEY = "routes";
   50 | 
   51 | 	private final RouteLocator delegate;
   52 | 
   53 | 	private final Flux<Route> routes;
   54 | 
   55 | 	private final Map<String, List> cache = new ConcurrentHashMap<>();
   56 | 
   57 | 	private @Nullable ApplicationEventPublisher applicationEventPublisher;
   58 | 
   59 | 	public CachingRouteLocator(RouteLocator delegate) {
   60 | 		this.delegate = delegate;
   61 | 		routes = CacheFlux.lookup(cache, CACHE_KEY, Route.class).onCacheMissResume(this::fetch);
   62 | 	}
   63 | 
   64 | 	private Flux<Route> fetch() {
   65 | 		return this.delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE);
   66 | 	}
   67 | 
   68 | 	private Flux<Route> fetch(Map<String, Object> metadata) {
   69 | 		return this.delegate.getRoutesByMetadata(metadata).sort(AnnotationAwareOrderComparator.INSTANCE);
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public Flux<Route> getRoutes() {
   74 | 		return this.routes;
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Clears the routes cache.
   79 | 	 * @return routes flux
   80 | 	 */
   81 | 	public Flux<Route> refresh() {
   82 | 		this.cache.remove(CACHE_KEY);
   83 | 		return this.routes;
   84 | 	}
   85 | 
   86 | 	@Override
   87 | 	public void onApplicationEvent(RefreshRoutesEvent event) {
   88 | 		try {
   89 | 			if (this.cache.containsKey(CACHE_KEY) && event.isScoped()) {
   90 | 				final Mono<List<Route>> scopedRoutes = fetch(event.getMetadata()).collect(Collectors.toList())
   91 | 					.onErrorResume(s -> Mono.just(List.of()));
   92 | 
   93 | 				scopedRoutes.subscribe(scopedRoutesList -> {
   94 | 					updateCache(Flux.concat(Flux.fromIterable(scopedRoutesList), getNonScopedRoutes(event))
   95 | 						.sort(AnnotationAwareOrderComparator.INSTANCE));
   96 | 				}, this::handleRefreshError);
   97 | 			}
   98 | 			else {
   99 | 				final Mono<List<Route>> allRoutes = fetch().collect(Collectors.toList());
  100 | 				allRoutes.subscribe(list -> updateCache(Flux.fromIterable(list)), this::handleRefreshError);
  101 | 			}
  102 | 		}
  103 | 		catch (Throwable e) {
  104 | 			handleRefreshError(e);
  105 | 		}
  106 | 	}
  107 | 
  108 | 	private synchronized void updateCache(Flux<Route> routes) {
  109 | 		routes.materialize()
  110 | 			.collect(Collectors.toList())
  111 | 			.subscribe(this::publishRefreshEvent, this::handleRefreshError);
  112 | 	}
  113 | 
  114 | 	private void publishRefreshEvent(List<Signal<Route>> signals) {
  115 | 		cache.put(CACHE_KEY, signals);
  116 | 		Objects.requireNonNull(applicationEventPublisher, "ApplicationEventPublisher is required");
  117 | 		applicationEventPublisher.publishEvent(new RefreshRoutesResultEvent(this));
  118 | 	}
  119 | 
  120 | 	private Flux<Route> getNonScopedRoutes(RefreshRoutesEvent scopedEvent) {
  121 | 		return this.getRoutes()
  122 | 			.filter(route -> !RouteLocator.matchMetadata(route.getMetadata(), scopedEvent.getMetadata()));
  123 | 	}
  124 | 
  125 | 	private void handleRefreshError(Throwable throwable) {
  126 | 		if (log.isErrorEnabled()) {
  127 | 			log.error("Refresh routes error !!!", throwable);
  128 | 		}
  129 | 		Objects.requireNonNull(applicationEventPublisher, "ApplicationEventPublisher is required");
  130 | 		applicationEventPublisher.publishEvent(new RefreshRoutesResultEvent(this, throwable));
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public int getOrder() {
  135 | 		return 0;
  136 | 	}
  137 | 
  138 | 	@Override
  139 | 	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
  140 | 		this.applicationEventPublisher = applicationEventPublisher;
  141 | 	}
  142 | 
  143 | }
```
