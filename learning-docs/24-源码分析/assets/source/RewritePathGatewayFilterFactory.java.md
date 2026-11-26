<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/RewritePathGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:RewritePathGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: RewritePathGatewayFilterFactory.java#L起-L止) -->

# RewritePathGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/RewritePathGatewayFilterFactory.java` |
| 行数 | 121 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:47.195Z |

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
   21 | import java.util.Objects;
   22 | import java.util.regex.Pattern;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | import reactor.core.publisher.Mono;
   26 | 
   27 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   28 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   29 | import org.springframework.http.server.reactive.ServerHttpRequest;
   30 | import org.springframework.util.Assert;
   31 | import org.springframework.web.server.ServerWebExchange;
   32 | 
   33 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   34 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   35 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;
   36 | 
   37 | /**
   38 |  * @author Spencer Gibb
   39 |  */
   40 | public class RewritePathGatewayFilterFactory
   41 | 		extends AbstractGatewayFilterFactory<RewritePathGatewayFilterFactory.Config> {
   42 | 
   43 | 	/**
   44 | 	 * Regexp key.
   45 | 	 */
   46 | 	public static final String REGEXP_KEY = "regexp";
   47 | 
   48 | 	/**
   49 | 	 * Replacement key.
   50 | 	 */
   51 | 	public static final String REPLACEMENT_KEY = "replacement";
   52 | 
   53 | 	public RewritePathGatewayFilterFactory() {
   54 | 		super(Config.class);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public List<String> shortcutFieldOrder() {
   59 | 		return Arrays.asList(REGEXP_KEY, REPLACEMENT_KEY);
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public GatewayFilter apply(Config config) {
   64 | 		String replacementValue = Objects.requireNonNull(config.replacement, "replacement must not be null");
   65 | 		String replacement = replacementValue.replace("$\\", "$");
   66 | 		String regexpValue = Objects.requireNonNull(config.regexp, "regexp must not be null");
   67 | 		Pattern pattern = Pattern.compile(regexpValue);
   68 | 		return new GatewayFilter() {
   69 | 			@Override
   70 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   71 | 				ServerHttpRequest req = exchange.getRequest();
   72 | 				addOriginalRequestUrl(exchange, req.getURI());
   73 | 				String path = req.getURI().getRawPath();
   74 | 				String newPath = pattern.matcher(path).replaceAll(replacement);
   75 | 
   76 | 				ServerHttpRequest request = req.mutate().path(newPath).build();
   77 | 
   78 | 				exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI());
   79 | 
   80 | 				return chain.filter(exchange.mutate().request(request).build());
   81 | 			}
   82 | 
   83 | 			@Override
   84 | 			public String toString() {
   85 | 				String regexp = config.getRegexp();
   86 | 				return filterToStringCreator(RewritePathGatewayFilterFactory.this)
   87 | 					.append(regexp != null ? regexp : "", replacement)
   88 | 					.toString();
   89 | 			}
   90 | 		};
   91 | 	}
   92 | 
   93 | 	public static class Config {
   94 | 
   95 | 		private @Nullable String regexp;
   96 | 
   97 | 		private @Nullable String replacement;
   98 | 
   99 | 		public @Nullable String getRegexp() {
  100 | 			return regexp;
  101 | 		}
  102 | 
  103 | 		public Config setRegexp(String regexp) {
  104 | 			Assert.hasText(regexp, "regexp must have a value");
  105 | 			this.regexp = regexp;
  106 | 			return this;
  107 | 		}
  108 | 
  109 | 		public @Nullable String getReplacement() {
  110 | 			return replacement;
  111 | 		}
  112 | 
  113 | 		public Config setReplacement(String replacement) {
  114 | 			Objects.requireNonNull(replacement, "replacement must not be null");
  115 | 			this.replacement = replacement;
  116 | 			return this;
  117 | 		}
  118 | 
  119 | 	}
  120 | 
  121 | }
```
