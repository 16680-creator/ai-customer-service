<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/AddResponseHeaderGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:AddResponseHeaderGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: AddResponseHeaderGatewayFilterFactory.java#L起-L止) -->

# AddResponseHeaderGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/AddResponseHeaderGatewayFilterFactory.java` |
| 行数 | 133 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:48.492Z |

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
   22 | 
   23 | import reactor.core.publisher.Mono;
   24 | 
   25 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   26 | import org.springframework.cloud.gateway.filter.GatewayFilterChain;
   27 | import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
   28 | import org.springframework.core.style.ToStringCreator;
   29 | import org.springframework.http.HttpHeaders;
   30 | import org.springframework.util.StringUtils;
   31 | import org.springframework.web.server.ServerWebExchange;
   32 | 
   33 | import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
   34 | 
   35 | /**
   36 |  * @author Spencer Gibb
   37 |  */
   38 | public class AddResponseHeaderGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {
   39 | 
   40 | 	private static final String OVERRIDE_KEY = "override";
   41 | 
   42 | 	@Override
   43 | 	public Class getConfigClass() {
   44 | 		return Config.class;
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public NameValueConfig newConfig() {
   49 | 		return new Config();
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public List<String> shortcutFieldOrder() {
   54 | 		return Arrays.asList(GatewayFilter.NAME_KEY, GatewayFilter.VALUE_KEY, OVERRIDE_KEY);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public GatewayFilter apply(NameValueConfig config) {
   59 | 		return new GatewayFilter() {
   60 | 			@Override
   61 | 			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   62 | 				return chain.filter(exchange).then(Mono.fromRunnable(() -> addHeader(exchange, config)));
   63 | 			}
   64 | 
   65 | 			@Override
   66 | 			public String toString() {
   67 | 				String name = config.getName();
   68 | 				String value = config.getValue();
   69 | 				if (config instanceof Config) {
   70 | 					return filterToStringCreator(AddResponseHeaderGatewayFilterFactory.this)
   71 | 						.append(GatewayFilter.NAME_KEY, name != null ? name : "")
   72 | 						.append(GatewayFilter.VALUE_KEY, value != null ? value : "")
   73 | 						.append(OVERRIDE_KEY, ((Config) config).isOverride())
   74 | 						.toString();
   75 | 				}
   76 | 				return filterToStringCreator(AddResponseHeaderGatewayFilterFactory.this)
   77 | 					.append(name != null ? name : "", value != null ? value : "")
   78 | 					.toString();
   79 | 			}
   80 | 		};
   81 | 	}
   82 | 
   83 | 	void addHeader(ServerWebExchange exchange, NameValueConfig config) {
   84 | 		// if response has been commited, no more response headers will bee added.
   85 | 		if (!exchange.getResponse().isCommitted()) {
   86 | 			String name = Objects.requireNonNull(config.getName(), "name must not be null");
   87 | 			String rawValue = Objects.requireNonNull(config.getValue(), "value must not be null");
   88 | 			final String value = ServerWebExchangeUtils.expand(exchange, rawValue);
   89 | 			HttpHeaders headers = exchange.getResponse().getHeaders();
   90 | 
   91 | 			boolean override = true; // default is true
   92 | 			if (config instanceof Config) {
   93 | 				override = ((Config) config).isOverride();
   94 | 			}
   95 | 
   96 | 			if (override) {
   97 | 				headers.add(name, value);
   98 | 			}
   99 | 			else {
  100 | 				boolean headerIsMissingOrBlank = headers.getOrEmpty(name)
  101 | 					.stream()
  102 | 					.allMatch(h -> !StringUtils.hasText(h));
  103 | 				if (headerIsMissingOrBlank) {
  104 | 					headers.add(name, value);
  105 | 				}
  106 | 			}
  107 | 		}
  108 | 	}
  109 | 
  110 | 	public static class Config extends AbstractNameValueGatewayFilterFactory.NameValueConfig {
  111 | 
  112 | 		private boolean override = true;
  113 | 
  114 | 		public boolean isOverride() {
  115 | 			return override;
  116 | 		}
  117 | 
  118 | 		public Config setOverride(boolean override) {
  119 | 			this.override = override;
  120 | 			return this;
  121 | 		}
  122 | 
  123 | 		@Override
  124 | 		public String toString() {
  125 | 			return new ToStringCreator(this).append(NAME_KEY, name)
  126 | 				.append(VALUE_KEY, value)
  127 | 				.append(OVERRIDE_KEY, override)
  128 | 				.toString();
  129 | 		}
  130 | 
  131 | 	}
  132 | 
  133 | }
```
