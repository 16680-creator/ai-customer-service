<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/PathRoutePredicateFactory.java -->
<!-- 正文引用：[[源码:PathRoutePredicateFactory.java#L起-L止]] ；代码块头部：(src: PathRoutePredicateFactory.java#L起-L止) -->

# PathRoutePredicateFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/PathRoutePredicateFactory.java` |
| 行数 | 187 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:49.493Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | import java.util.function.Predicate;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | 
   27 | import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
   28 | import org.springframework.core.style.ToStringCreator;
   29 | import org.springframework.http.server.PathContainer;
   30 | import org.springframework.util.StringUtils;
   31 | import org.springframework.web.server.ServerWebExchange;
   32 | import org.springframework.web.util.pattern.PathPattern;
   33 | import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;
   34 | import org.springframework.web.util.pattern.PathPatternParser;
   35 | 
   36 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ATTR;
   37 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR;
   38 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR;
   39 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR;
   40 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.putUriTemplateVariables;
   41 | import static org.springframework.http.server.PathContainer.parsePath;
   42 | 
   43 | /**
   44 |  * @author Spencer Gibb
   45 |  * @author Dhawal Kapil
   46 |  * @author FuYiNan Guo
   47 |  */
   48 | public class PathRoutePredicateFactory extends AbstractRoutePredicateFactory<PathRoutePredicateFactory.Config> {
   49 | 
   50 | 	private static final Log log = LogFactory.getLog(PathRoutePredicateFactory.class);
   51 | 
   52 | 	private static final String MATCH_TRAILING_SLASH = "matchTrailingSlash";
   53 | 
   54 | 	private PathPatternParser pathPatternParser = new PathPatternParser();
   55 | 
   56 | 	private final WebFluxProperties webFluxProperties;
   57 | 
   58 | 	public PathRoutePredicateFactory(WebFluxProperties webFluxProperties) {
   59 | 		super(Config.class);
   60 | 		this.webFluxProperties = webFluxProperties;
   61 | 	}
   62 | 
   63 | 	private static void traceMatch(String prefix, Object desired, Object actual, boolean match) {
   64 | 		if (log.isTraceEnabled()) {
   65 | 			String message = String.format("%s \"%s\" %s against value \"%s\"", prefix, desired,
   66 | 					match ? "matches" : "does not match", actual);
   67 | 			log.trace(message);
   68 | 		}
   69 | 	}
   70 | 
   71 | 	public void setPathPatternParser(PathPatternParser pathPatternParser) {
   72 | 		this.pathPatternParser = pathPatternParser;
   73 | 	}
   74 | 
   75 | 	@Override
   76 | 	public List<String> shortcutFieldOrder() {
   77 | 		return Arrays.asList("patterns", MATCH_TRAILING_SLASH);
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public ShortcutType shortcutType() {
   82 | 		return ShortcutType.GATHER_LIST_TAIL_FLAG;
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public Predicate<ServerWebExchange> apply(Config config) {
   87 | 		final ArrayList<PathPattern> pathPatterns = new ArrayList<>();
   88 | 		synchronized (this.pathPatternParser) {
   89 | 			// FIXME: 5.0.0 setMatchOptionalTrailingSeparator missing
   90 | 			// pathPatternParser.setMatchOptionalTrailingSeparator(config.isMatchTrailingSlash());
   91 | 			config.getPatterns().forEach(pattern -> {
   92 | 				String basePath = webFluxProperties.getBasePath();
   93 | 				boolean basePathIsNotBlank = StringUtils.hasText(basePath);
   94 | 				String pathPatternStr = pattern;
   95 | 				if (basePathIsNotBlank) {
   96 | 					if (pattern.length() > 1 && !pattern.startsWith("/")) {
   97 | 						basePath += ("/");
   98 | 					}
   99 | 					pathPatternStr = basePath + pattern;
  100 | 				}
  101 | 				PathPattern pathPattern = this.pathPatternParser.parse(pathPatternStr);
  102 | 				pathPatterns.add(pathPattern);
  103 | 			});
  104 | 		}
  105 | 		return new GatewayPredicate() {
  106 | 			@Override
  107 | 			public boolean test(ServerWebExchange exchange) {
  108 | 				PathContainer path = (PathContainer) exchange.getAttributes()
  109 | 					.computeIfAbsent(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR,
  110 | 							s -> parsePath(exchange.getRequest().getURI().getRawPath()));
  111 | 
  112 | 				PathPattern match = null;
  113 | 				for (int i = 0; i < pathPatterns.size(); i++) {
  114 | 					PathPattern pathPattern = pathPatterns.get(i);
  115 | 					if (pathPattern.matches(path)) {
  116 | 						match = pathPattern;
  117 | 						break;
  118 | 					}
  119 | 				}
  120 | 
  121 | 				if (match != null) {
  122 | 					traceMatch("Pattern", match.getPatternString(), path, true);
  123 | 					PathMatchInfo pathMatchInfo = match.matchAndExtract(path);
  124 | 					if (pathMatchInfo != null) {
  125 | 						putUriTemplateVariables(exchange, pathMatchInfo.getUriVariables());
  126 | 					}
  127 | 					exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ATTR, match.getPatternString());
  128 | 					String routeId = (String) exchange.getAttributes().get(GATEWAY_PREDICATE_ROUTE_ATTR);
  129 | 					if (routeId != null) {
  130 | 						// populated in RoutePredicateHandlerMapping
  131 | 						exchange.getAttributes().put(GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR, routeId);
  132 | 					}
  133 | 					return true;
  134 | 				}
  135 | 				else {
  136 | 					traceMatch("Pattern", config.getPatterns(), path, false);
  137 | 					return false;
  138 | 				}
  139 | 			}
  140 | 
  141 | 			@Override
  142 | 			public Object getConfig() {
  143 | 				return config;
  144 | 			}
  145 | 
  146 | 			@Override
  147 | 			public String toString() {
  148 | 				return String.format("Paths: %s, match trailing slash: %b", config.getPatterns(),
  149 | 						config.isMatchTrailingSlash());
  150 | 			}
  151 | 		};
  152 | 	}
  153 | 
  154 | 	public static class Config {
  155 | 
  156 | 		private List<String> patterns = new ArrayList<>();
  157 | 
  158 | 		private boolean matchTrailingSlash = true;
  159 | 
  160 | 		public List<String> getPatterns() {
  161 | 			return patterns;
  162 | 		}
  163 | 
  164 | 		public Config setPatterns(List<String> patterns) {
  165 | 			this.patterns = patterns;
  166 | 			return this;
  167 | 		}
  168 | 
  169 | 		public boolean isMatchTrailingSlash() {
  170 | 			return matchTrailingSlash;
  171 | 		}
  172 | 
  173 | 		public Config setMatchTrailingSlash(boolean matchTrailingSlash) {
  174 | 			this.matchTrailingSlash = matchTrailingSlash;
  175 | 			return this;
  176 | 		}
  177 | 
  178 | 		@Override
  179 | 		public String toString() {
  180 | 			return new ToStringCreator(this).append("patterns", patterns)
  181 | 				.append(MATCH_TRAILING_SLASH, matchTrailingSlash)
  182 | 				.toString();
  183 | 		}
  184 | 
  185 | 	}
  186 | 
  187 | }
```
