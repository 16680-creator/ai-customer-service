<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/config/GatewayProperties.java -->
<!-- 正文引用：[[源码:GatewayProperties.java#L起-L止]] ；代码块头部：(src: GatewayProperties.java#L起-L止) -->

# GatewayProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/config/GatewayProperties.java` |
| 行数 | 145 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:49.962Z |

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
   17 | package org.springframework.cloud.gateway.config;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | 
   23 | import jakarta.validation.Valid;
   24 | import jakarta.validation.constraints.NotNull;
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | 
   28 | import org.springframework.boot.context.properties.ConfigurationProperties;
   29 | import org.springframework.cloud.gateway.filter.FilterDefinition;
   30 | import org.springframework.cloud.gateway.route.RouteDefinition;
   31 | import org.springframework.core.style.ToStringCreator;
   32 | import org.springframework.http.MediaType;
   33 | import org.springframework.validation.annotation.Validated;
   34 | 
   35 | /**
   36 |  * @author Spencer Gibb
   37 |  */
   38 | @ConfigurationProperties(GatewayProperties.PREFIX)
   39 | @Validated
   40 | public class GatewayProperties {
   41 | 
   42 | 	/**
   43 | 	 * Properties prefix.
   44 | 	 */
   45 | 	public static final String PREFIX = "spring.cloud.gateway.server.webflux";
   46 | 
   47 | 	private final Log logger = LogFactory.getLog(getClass());
   48 | 
   49 | 	/**
   50 | 	 * List of Routes.
   51 | 	 */
   52 | 	@NotNull
   53 | 	@Valid
   54 | 	private List<RouteDefinition> routes = new ArrayList<>();
   55 | 
   56 | 	/**
   57 | 	 * List of filter definitions that are applied to every route.
   58 | 	 */
   59 | 	private List<FilterDefinition> defaultFilters = new ArrayList<>();
   60 | 
   61 | 	private List<MediaType> streamingMediaTypes = Arrays.asList(MediaType.TEXT_EVENT_STREAM,
   62 | 			new MediaType("application", "stream+json"), new MediaType("application", "grpc"),
   63 | 			new MediaType("application", "grpc+protobuf"), new MediaType("application", "grpc+json"));
   64 | 
   65 | 	/**
   66 | 	 * Option to fail on route definition errors, defaults to true. Otherwise, a warning
   67 | 	 * is logged.
   68 | 	 */
   69 | 	private boolean failOnRouteDefinitionError = true;
   70 | 
   71 | 	/**
   72 | 	 * Enables the route filter cache, defaults to false.
   73 | 	 */
   74 | 	private boolean routeFilterCacheEnabled = false;
   75 | 
   76 | 	/**
   77 | 	 * Regular expression defining proxies that are trusted when they appear in a
   78 | 	 * Forwarded or X-Forwarded header.
   79 | 	 */
   80 | 	private String trustedProxies;
   81 | 
   82 | 	public boolean isRouteFilterCacheEnabled() {
   83 | 		return routeFilterCacheEnabled;
   84 | 	}
   85 | 
   86 | 	public void setRouteFilterCacheEnabled(boolean routeFilterCacheEnabled) {
   87 | 		this.routeFilterCacheEnabled = routeFilterCacheEnabled;
   88 | 	}
   89 | 
   90 | 	public List<RouteDefinition> getRoutes() {
   91 | 		return routes;
   92 | 	}
   93 | 
   94 | 	public void setRoutes(List<RouteDefinition> routes) {
   95 | 		this.routes = routes;
   96 | 		if (routes != null && routes.size() > 0 && logger.isDebugEnabled()) {
   97 | 			logger.debug("Routes supplied from Gateway Properties: " + routes);
   98 | 		}
   99 | 	}
  100 | 
  101 | 	public List<FilterDefinition> getDefaultFilters() {
  102 | 		return defaultFilters;
  103 | 	}
  104 | 
  105 | 	public void setDefaultFilters(List<FilterDefinition> defaultFilters) {
  106 | 		this.defaultFilters = defaultFilters;
  107 | 	}
  108 | 
  109 | 	public List<MediaType> getStreamingMediaTypes() {
  110 | 		return streamingMediaTypes;
  111 | 	}
  112 | 
  113 | 	public void setStreamingMediaTypes(List<MediaType> streamingMediaTypes) {
  114 | 		this.streamingMediaTypes = streamingMediaTypes;
  115 | 	}
  116 | 
  117 | 	public boolean isFailOnRouteDefinitionError() {
  118 | 		return failOnRouteDefinitionError;
  119 | 	}
  120 | 
  121 | 	public void setFailOnRouteDefinitionError(boolean failOnRouteDefinitionError) {
  122 | 		this.failOnRouteDefinitionError = failOnRouteDefinitionError;
  123 | 	}
  124 | 
  125 | 	public String getTrustedProxies() {
  126 | 		return trustedProxies;
  127 | 	}
  128 | 
  129 | 	public void setTrustedProxies(String trustedProxies) {
  130 | 		this.trustedProxies = trustedProxies;
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public String toString() {
  135 | 		return new ToStringCreator(this).append("routes", routes)
  136 | 			.append("defaultFilters", defaultFilters)
  137 | 			.append("streamingMediaTypes", streamingMediaTypes)
  138 | 			.append("failOnRouteDefinitionError", failOnRouteDefinitionError)
  139 | 			.append("routeFilterCacheEnabled", routeFilterCacheEnabled)
  140 | 			.append("trustedProxies", trustedProxies)
  141 | 			.toString();
  142 | 
  143 | 	}
  144 | 
  145 | }
```
