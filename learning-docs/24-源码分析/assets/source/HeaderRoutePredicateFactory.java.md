<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/HeaderRoutePredicateFactory.java -->
<!-- 正文引用：[[源码:HeaderRoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: HeaderRoutePredicateFactory.java#L起-L止) -->

# HeaderRoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/HeaderRoutePredicateFactory.java` |
| 行数 | 123 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.535Z |

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
   22 | import java.util.regex.Pattern;
   23 | 
   24 | import jakarta.validation.constraints.NotEmpty;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.util.StringUtils;
   28 | import org.springframework.web.server.ServerWebExchange;
   29 | 
   30 | /**
   31 |  * @author Spencer Gibb
   32 |  */
   33 | public class HeaderRoutePredicateFactory extends AbstractRoutePredicateFactory<HeaderRoutePredicateFactory.Config> {
   34 | 
   35 | 	/**
   36 | 	 * Header key.
   37 | 	 */
   38 | 	public static final String HEADER_KEY = "header";
   39 | 
   40 | 	/**
   41 | 	 * Regexp key.
   42 | 	 */
   43 | 	public static final String REGEXP_KEY = "regexp";
   44 | 
   45 | 	public HeaderRoutePredicateFactory() {
   46 | 		super(Config.class);
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public List<String> shortcutFieldOrder() {
   51 | 		return Arrays.asList(HEADER_KEY, REGEXP_KEY);
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public Predicate<ServerWebExchange> apply(Config config) {
   56 | 		Pattern pattern = (StringUtils.hasText(config.regexp)) ? Pattern.compile(config.regexp) : null;
   57 | 
   58 | 		return new GatewayPredicate() {
   59 | 			@Override
   60 | 			public boolean test(ServerWebExchange exchange) {
   61 | 				if (!StringUtils.hasText(config.header)) {
   62 | 					return false;
   63 | 				}
   64 | 				List<String> values = exchange.getRequest().getHeaders().getValuesAsList(config.header);
   65 | 				if (values.isEmpty()) {
   66 | 					return false;
   67 | 				}
   68 | 				// values is now guaranteed to not be empty
   69 | 				if (pattern != null) {
   70 | 					// check if a header value matches
   71 | 					for (int i = 0; i < values.size(); i++) {
   72 | 						String value = values.get(i);
   73 | 						if (pattern.asMatchPredicate().test(value)) {
   74 | 							return true;
   75 | 						}
   76 | 					}
   77 | 					return false;
   78 | 				}
   79 | 
   80 | 				// there is a value and since regexp is empty, we only check existence.
   81 | 				return true;
   82 | 			}
   83 | 
   84 | 			@Override
   85 | 			public Object getConfig() {
   86 | 				return config;
   87 | 			}
   88 | 
   89 | 			@Override
   90 | 			public String toString() {
   91 | 				return String.format("Header: %s regexp=%s", config.header, config.regexp);
   92 | 			}
   93 | 		};
   94 | 	}
   95 | 
   96 | 	public static class Config {
   97 | 
   98 | 		@NotEmpty
   99 | 		private @Nullable String header;
  100 | 
  101 | 		private @Nullable String regexp;
  102 | 
  103 | 		public @Nullable String getHeader() {
  104 | 			return header;
  105 | 		}
  106 | 
  107 | 		public Config setHeader(String header) {
  108 | 			this.header = header;
  109 | 			return this;
  110 | 		}
  111 | 
  112 | 		public @Nullable String getRegexp() {
  113 | 			return regexp;
  114 | 		}
  115 | 
  116 | 		public Config setRegexp(@Nullable String regexp) {
  117 | 			this.regexp = regexp;
  118 | 			return this;
  119 | 		}
  120 | 
  121 | 	}
  122 | 
  123 | }
```
