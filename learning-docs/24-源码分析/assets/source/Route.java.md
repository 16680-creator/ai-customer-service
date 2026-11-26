<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/route/Route.java -->
<!-- 正文引用：[[源码:Route.java#L起-L止]] ；代码块头部：(src: Route.java#L起-L止) -->

# Route.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/route/Route.java` |
| 行数 | 342 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:45.375Z |

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
   19 | import java.net.URI;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collection;
   23 | import java.util.Collections;
   24 | import java.util.HashMap;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.function.Predicate;
   29 | 
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   33 | import org.springframework.cloud.gateway.handler.AsyncPredicate;
   34 | import org.springframework.cloud.gateway.route.builder.Buildable;
   35 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   36 | import org.springframework.core.Ordered;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.web.server.ServerWebExchange;
   39 | import org.springframework.web.util.UriComponentsBuilder;
   40 | 
   41 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.toAsyncPredicate;
   42 | 
   43 | /**
   44 |  * @author Spencer Gibb
   45 |  */
   46 | public class Route implements Ordered {
   47 | 
   48 | 	private final String id;
   49 | 
   50 | 	private final URI uri;
   51 | 
   52 | 	private final int order;
   53 | 
   54 | 	private final AsyncPredicate<ServerWebExchange> predicate;
   55 | 
   56 | 	private final List<GatewayFilter> gatewayFilters;
   57 | 
   58 | 	private final Map<String, Object> metadata;
   59 | 
   60 | 	private Route(String id, URI uri, int order, AsyncPredicate<ServerWebExchange> predicate,
   61 | 			List<GatewayFilter> gatewayFilters, Map<String, Object> metadata) {
   62 | 		this.id = id;
   63 | 		this.uri = uri;
   64 | 		this.order = order;
   65 | 		this.predicate = predicate;
   66 | 		this.gatewayFilters = gatewayFilters;
   67 | 		this.metadata = metadata;
   68 | 	}
   69 | 
   70 | 	public static Builder builder() {
   71 | 		return new Builder();
   72 | 	}
   73 | 
   74 | 	public static Builder builder(RouteDefinition routeDefinition) {
   75 | 		// @formatter:off
   76 | 		Builder builder = new Builder().order(routeDefinition.getOrder())
   77 | 				.metadata(routeDefinition.getMetadata());
   78 | 		if (routeDefinition.getUri() != null) {
   79 | 			builder.uri(routeDefinition.getUri());
   80 | 		}
   81 | 		if (routeDefinition.getId() != null) {
   82 | 			builder.id(routeDefinition.getId());
   83 | 		}
   84 | 		return builder;
   85 | 		// @formatter:on
   86 | 	}
   87 | 
   88 | 	public static AsyncBuilder async() {
   89 | 		return new AsyncBuilder();
   90 | 	}
   91 | 
   92 | 	public static AsyncBuilder async(RouteDefinition routeDefinition) {
   93 | 		// @formatter:off
   94 | 		AsyncBuilder asyncBuilder = new AsyncBuilder()
   95 | 				.order(routeDefinition.getOrder())
   96 | 				.metadata(routeDefinition.getMetadata());
   97 | 		if (routeDefinition.getUri() != null) {
   98 | 			asyncBuilder.uri(routeDefinition.getUri());
   99 | 		}
  100 | 		if (routeDefinition.getId() != null) {
  101 | 			asyncBuilder.id(routeDefinition.getId());
  102 | 		}
  103 | 		return asyncBuilder;
  104 | 		// @formatter:on
  105 | 	}
  106 | 
  107 | 	public String getId() {
  108 | 		return this.id;
  109 | 	}
  110 | 
  111 | 	public URI getUri() {
  112 | 		return this.uri;
  113 | 	}
  114 | 
  115 | 	public int getOrder() {
  116 | 		return order;
  117 | 	}
  118 | 
  119 | 	public AsyncPredicate<ServerWebExchange> getPredicate() {
  120 | 		return this.predicate;
  121 | 	}
  122 | 
  123 | 	public List<GatewayFilter> getFilters() {
  124 | 		return Collections.unmodifiableList(this.gatewayFilters);
  125 | 	}
  126 | 
  127 | 	public Map<String, Object> getMetadata() {
  128 | 		return Collections.unmodifiableMap(metadata);
  129 | 	}
  130 | 
  131 | 	@Override
  132 | 	public boolean equals(Object o) {
  133 | 		if (this == o) {
  134 | 			return true;
  135 | 		}
  136 | 		if (o == null || getClass() != o.getClass()) {
  137 | 			return false;
  138 | 		}
  139 | 		Route route = (Route) o;
  140 | 		return this.order == route.order && Objects.equals(this.id, route.id) && Objects.equals(this.uri, route.uri)
  141 | 				&& Objects.equals(this.predicate, route.predicate)
  142 | 				&& Objects.equals(this.gatewayFilters, route.gatewayFilters)
  143 | 				&& Objects.equals(this.metadata, route.metadata);
  144 | 	}
  145 | 
  146 | 	@Override
  147 | 	public int hashCode() {
  148 | 		return Objects.hash(this.id, this.uri, this.order, this.predicate, this.gatewayFilters, this.metadata);
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	public String toString() {
  153 | 		final StringBuffer sb = new StringBuffer("Route{");
  154 | 		sb.append("id='").append(id).append('\'');
  155 | 		sb.append(", uri=").append(uri);
  156 | 		sb.append(", order=").append(order);
  157 | 		sb.append(", predicate=").append(predicate);
  158 | 		sb.append(", gatewayFilters=").append(gatewayFilters);
  159 | 		sb.append(", metadata=").append(metadata);
  160 | 		sb.append('}');
  161 | 		return sb.toString();
  162 | 	}
  163 | 
  164 | 	public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> implements Buildable<Route> {
  165 | 
  166 | 		protected @Nullable String id;
  167 | 
  168 | 		protected @Nullable URI uri;
  169 | 
  170 | 		protected int order = 0;
  171 | 
  172 | 		protected List<GatewayFilter> gatewayFilters = new ArrayList<>();
  173 | 
  174 | 		protected Map<String, Object> metadata = new HashMap<>();
  175 | 
  176 | 		protected AbstractBuilder() {
  177 | 		}
  178 | 
  179 | 		protected abstract B getThis();
  180 | 
  181 | 		public B id(String id) {
  182 | 			this.id = id;
  183 | 			return getThis();
  184 | 		}
  185 | 
  186 | 		public @Nullable String getId() {
  187 | 			return id;
  188 | 		}
  189 | 
  190 | 		public B order(int order) {
  191 | 			this.order = order;
  192 | 			return getThis();
  193 | 		}
  194 | 
  195 | 		public B uri(String uri) {
  196 | 			return uri(URI.create(uri));
  197 | 		}
  198 | 
  199 | 		public B uri(URI uri) {
  200 | 			this.uri = uri;
  201 | 			String scheme = this.uri.getScheme();
  202 | 			Assert.hasText(scheme, "The parameter [" + this.uri + "] format is incorrect, scheme can not be empty");
  203 | 			if (scheme.equalsIgnoreCase("localhost")) {
  204 | 				// common error
  205 | 				// TODO: find a general way to detect without breaking existing behavior
  206 | 				throw new IllegalArgumentException(
  207 | 						"The parameter [" + this.uri + "] format is incorrect, scheme can not be localhost");
  208 | 			}
  209 | 			if (this.uri.getPort() < 0 && scheme.startsWith("http")) {
  210 | 				// default known http ports
  211 | 				int port = this.uri.getScheme().equals("https") ? 443 : 80;
  212 | 				this.uri = UriComponentsBuilder.fromUri(this.uri).port(port).build(false).toUri();
  213 | 			}
  214 | 			return getThis();
  215 | 		}
  216 | 
  217 | 		public B replaceMetadata(Map<String, Object> metadata) {
  218 | 			this.metadata = metadata;
  219 | 			return getThis();
  220 | 		}
  221 | 
  222 | 		public B metadata(Map<String, Object> metadata) {
  223 | 			this.metadata.putAll(metadata);
  224 | 			return getThis();
  225 | 		}
  226 | 
  227 | 		public B metadata(@Nullable String key, @Nullable Object value) {
  228 | 			this.metadata.put(key, value);
  229 | 			return getThis();
  230 | 		}
  231 | 
  232 | 		public abstract AsyncPredicate<ServerWebExchange> getPredicate();
  233 | 
  234 | 		public B replaceFilters(List<GatewayFilter> gatewayFilters) {
  235 | 			this.gatewayFilters = gatewayFilters;
  236 | 			return getThis();
  237 | 		}
  238 | 
  239 | 		public B filter(GatewayFilter gatewayFilter) {
  240 | 			this.gatewayFilters.add(gatewayFilter);
  241 | 			return getThis();
  242 | 		}
  243 | 
  244 | 		public B filters(Collection<GatewayFilter> gatewayFilters) {
  245 | 			this.gatewayFilters.addAll(gatewayFilters);
  246 | 			return getThis();
  247 | 		}
  248 | 
  249 | 		public B filters(GatewayFilter... gatewayFilters) {
  250 | 			return filters(Arrays.asList(gatewayFilters));
  251 | 		}
  252 | 
  253 | 		public Route build() {
  254 | 			Objects.requireNonNull(this.id, "id can not be null");
  255 | 			Objects.requireNonNull(this.uri, "uri can not be null");
  256 | 			AsyncPredicate<ServerWebExchange> predicate = getPredicate();
  257 | 			Objects.requireNonNull(predicate, "predicate can not be null");
  258 | 
  259 | 			return new Route(this.id, this.uri, this.order, predicate, this.gatewayFilters, this.metadata);
  260 | 		}
  261 | 
  262 | 	}
  263 | 
  264 | 	public static class AsyncBuilder extends AbstractBuilder<AsyncBuilder> {
  265 | 
  266 | 		protected @Nullable AsyncPredicate<ServerWebExchange> predicate;
  267 | 
  268 | 		@Override
  269 | 		protected AsyncBuilder getThis() {
  270 | 			return this;
  271 | 		}
  272 | 
  273 | 		@Override
  274 | 		public AsyncPredicate<ServerWebExchange> getPredicate() {
  275 | 			Objects.requireNonNull(this.predicate, "predicate can not be null");
  276 | 			return this.predicate;
  277 | 		}
  278 | 
  279 | 		public AsyncBuilder predicate(Predicate<ServerWebExchange> predicate) {
  280 | 			return asyncPredicate(toAsyncPredicate(predicate));
  281 | 		}
  282 | 
  283 | 		public AsyncBuilder asyncPredicate(AsyncPredicate<ServerWebExchange> predicate) {
  284 | 			this.predicate = predicate;
  285 | 			return this;
  286 | 		}
  287 | 
  288 | 		public AsyncBuilder and(AsyncPredicate<ServerWebExchange> predicate) {
  289 | 			Objects.requireNonNull(this.predicate, "can not call and() on null predicate");
  290 | 			this.predicate = this.predicate.and(predicate);
  291 | 			return this;
  292 | 		}
  293 | 
  294 | 		public AsyncBuilder or(AsyncPredicate<ServerWebExchange> predicate) {
  295 | 			Objects.requireNonNull(this.predicate, "can not call or() on null predicate");
  296 | 			this.predicate = this.predicate.or(predicate);
  297 | 			return this;
  298 | 		}
  299 | 
  300 | 		public AsyncBuilder negate() {
  301 | 			Objects.requireNonNull(this.predicate, "can not call negate() on null predicate");
  302 | 			this.predicate = this.predicate.negate();
  303 | 			return this;
  304 | 		}
  305 | 
  306 | 	}
  307 | 
  308 | 	public static class Builder extends AbstractBuilder<Builder> {
  309 | 
  310 | 		protected @Nullable Predicate<ServerWebExchange> predicate;
  311 | 
  312 | 		@Override
  313 | 		protected Builder getThis() {
  314 | 			return this;
  315 | 		}
  316 | 
  317 | 		@Override
  318 | 		public AsyncPredicate<ServerWebExchange> getPredicate() {
  319 | 			return ServerWebExchangeUtils.toAsyncPredicate(this.predicate);
  320 | 		}
  321 | 
  322 | 		public Builder and(Predicate<ServerWebExchange> predicate) {
  323 | 			Objects.requireNonNull(this.predicate, "can not call and() on null predicate");
  324 | 			this.predicate = this.predicate.and(predicate);
  325 | 			return this;
  326 | 		}
  327 | 
  328 | 		public Builder or(Predicate<ServerWebExchange> predicate) {
  329 | 			Objects.requireNonNull(this.predicate, "can not call or() on null predicate");
  330 | 			this.predicate = this.predicate.or(predicate);
  331 | 			return this;
  332 | 		}
  333 | 
  334 | 		public Builder negate() {
  335 | 			Objects.requireNonNull(this.predicate, "can not call negate() on null predicate");
  336 | 			this.predicate = this.predicate.negate();
  337 | 			return this;
  338 | 		}
  339 | 
  340 | 	}
  341 | 
  342 | }
```
